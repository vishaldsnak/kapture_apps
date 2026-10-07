package com.mazda.gms3.dmt.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;

/**
 * Servlet implementation class FileUploadManager
 *
 * INTERNAL UTILITY SCREEN - accessed directly by URL, deliberately NOT added to the
 * application menu and deliberately NOT gated by the WSL (iv-user) header check that the
 * other DMT pages apply. No database operations are performed by this servlet.
 *
 * Physical folder and its web-relative counterpart are configured in application.properties:
 *   fileupload.physicalpath
 *   fileupload.relativepath
 *
 * Actions (all responses except the default page render are JSON):
 *   (no action)      -> forwards to /jsps/fileuploadmanager.jsp
 *   action=list      -> JSON array of files currently in the folder
 *   action=upload    -> multipart POST carrying exactly ONE file; returns JSON result
 *   action=delete    -> deletes one file by name; returns JSON result
 *
 * Uploads are handled ONE FILE PER REQUEST on purpose. The browser issues a separate
 * XMLHttpRequest per selected file, which is what allows a real per-file progress bar
 * via XMLHttpRequest.upload.onprogress. It also means a single huge file can never block
 * the whole batch, and a failure is isolated to its own file.
 *
 * The commons-fileupload STREAMING api (getItemIterator) is used rather than
 * parseRequest(), so the file is piped straight to disk. Nothing is buffered in memory
 * and nothing is staged into a temp file first, which is what makes "any size" safe.
 */
public class FileUploadManager extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private Logger logger = LogManager.getLogger(FileUploadManager.class);

	/** Guards the auto-rename + create sequence so two concurrent uploads cannot pick the same name. */
	private static final Object FILE_CREATE_LOCK = new Object();

	private static final int COPY_BUFFER_SIZE = 64 * 1024;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public FileUploadManager() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String action = null;
		try {
			/*
			 * NOTE - for a multipart upload the action cannot be read with getParameter(),
			 * the request body has not been parsed yet. The upload action is therefore
			 * detected from the content type / query string instead.
			 */
			if (ServletFileUpload.isMultipartContent(request)) {
				action = "upload";
			} else if (null != request.getParameter("action") && !"".equals(request.getParameter("action"))) {
				action = request.getParameter("action").trim();
			}

			if ("upload".equals(action)) {
				handleUpload(request, response);
			} else if ("delete".equals(action)) {
				handleDelete(request, response);
			} else if ("list".equals(action)) {
				handleList(request, response);
			} else {
				/*
				 * DEFAULT - render the page. No WSL / iv-user check here by design.
				 */
				request.setAttribute("uploadRelativePath", getRelativePath());
				request.setAttribute("uploadPhysicalPath", getPhysicalPathForDisplay());
				request.getRequestDispatcher("/jsps/fileuploadmanager.jsp").forward(request, response);
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileUploadManager.class.getName(), "doPost()", e);
			if (null != action && !"".equals(action)) {
				// AJAX caller - answer with JSON so the browser can show the reason
				try {
					writeJson(response, "{\"status\":\"ERROR\",\"message\":" + jsonString(cleanMessage(e)) + "}");
				} catch (Exception ignore) {
					// nothing further can be done for this request
				}
			}
		}
		action = null;
	}

	/* ------------------------------------------------------------------ */
	/* UPLOAD                                                              */
	/* ------------------------------------------------------------------ */

	/**
	 * Streams a single uploaded file to the configured folder.
	 * Responds with JSON: status, savedName, size, message.
	 */
	private void handleUpload(HttpServletRequest request, HttpServletResponse response) {
		String originalName = null;
		String savedName = null;
		long bytesWritten = 0;
		try {
			File targetDir = resolveTargetDirectory();
			if (null == targetDir) {
				writeJson(response, "{\"status\":\"ERROR\",\"message\":"
						+ jsonString("Upload folder is not available. Check fileupload.physicalpath in application.properties.") + "}");
				return;
			}

			ServletFileUpload upload = new ServletFileUpload();
			/*
			 * EXPLICITLY UNLIMITED - the requirement is "any size".
			 * -1 disables both the per-file and the whole-request ceiling in
			 * commons-fileupload. Note this does NOT lift the container's own limit;
			 * WebLogic's MaxPostSize must be raised separately (see README notes).
			 */
			upload.setFileSizeMax(-1L);
			upload.setSizeMax(-1L);
			upload.setHeaderEncoding("UTF-8");

			FileItemIterator iter = upload.getItemIterator(request);
			while (iter.hasNext()) {
				FileItemStream item = iter.next();
				if (item.isFormField()) {
					// no form fields are expected on the upload call - skip
					continue;
				}

				originalName = sanitiseFileName(item.getName());
				if (null == originalName || "".equals(originalName)) {
					writeJson(response, "{\"status\":\"ERROR\",\"message\":" + jsonString("File name is empty or invalid.") + "}");
					return;
				}

				File target = reserveTargetFile(targetDir, originalName);
				if (null == target) {
					writeJson(response, "{\"status\":\"ERROR\",\"originalName\":" + jsonString(originalName)
							+ ",\"message\":" + jsonString("Could not create the file on the server.") + "}");
					return;
				}
				savedName = target.getName();

				InputStream in = null;
				OutputStream out = null;
				boolean copied = false;
				try {
					in = item.openStream();
					out = new FileOutputStream(target);
					byte[] buffer = new byte[COPY_BUFFER_SIZE];
					int read = 0;
					while ((read = in.read(buffer)) != -1) {
						out.write(buffer, 0, read);
						bytesWritten = bytesWritten + read;
					}
					out.flush();
					buffer = null;
					copied = true;
				} finally {
					if (null != out) {
						try {
							out.close();
						} catch (Exception e) {
							logger.info("handleUpload :: failed to close output stream for :: > " + savedName);
						}
					}
					if (null != in) {
						try {
							in.close();
						} catch (Exception e) {
							logger.info("handleUpload :: failed to close input stream for :: > " + savedName);
						}
					}
					out = null;
					in = null;

					/*
					 * If the copy did not finish - browser aborted, network dropped, disk full -
					 * remove the partial file. Leaving a truncated file behind under a name that
					 * looks legitimate is worse than leaving nothing, because the next person to
					 * download it has no way of knowing it is incomplete.
					 */
					if (!copied) {
						try {
							boolean removed = target.delete();
							logger.info("handleUpload :: Upload of {" + originalName + "} did not complete after "
									+ bytesWritten + " bytes. Partial file cleanup delete() returned " + removed
									+ " for :: > " + target.getAbsolutePath());
						} catch (Exception e) {
							logger.info("handleUpload :: Failed to clean up partial file :: > " + target.getAbsolutePath());
						}
					}
				}

				logger.info("handleUpload :: Uploaded {" + originalName + "} saved as {" + savedName + "} size {" + bytesWritten + "} bytes.");

				// only the first file part is honoured - one file per request by design
				break;
			}

			if (null == savedName) {
				writeJson(response, "{\"status\":\"ERROR\",\"message\":" + jsonString("No file was received in the request.") + "}");
				return;
			}

			StringBuilder json = new StringBuilder();
			json.append("{\"status\":\"SUCCESS\"");
			json.append(",\"originalName\":").append(jsonString(originalName));
			json.append(",\"savedName\":").append(jsonString(savedName));
			json.append(",\"renamed\":").append(savedName.equals(originalName) ? "false" : "true");
			json.append(",\"size\":").append(bytesWritten);
			json.append(",\"message\":").append(jsonString(savedName.equals(originalName)
					? "Uploaded successfully."
					: "A file with that name already existed - saved as " + savedName + "."));
			json.append("}");
			writeJson(response, json.toString());
			json = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileUploadManager.class.getName(), "handleUpload()", e);
			try {
				writeJson(response, "{\"status\":\"ERROR\",\"originalName\":" + jsonString(originalName)
						+ ",\"message\":" + jsonString(cleanMessage(e)) + "}");
			} catch (Exception ignore) {
				// response already committed
			}
		}
		originalName = null;
		savedName = null;
	}

	/**
	 * Picks a free file name and atomically creates the placeholder so a concurrent
	 * upload cannot claim the same one.
	 *
	 * report.pdf -> report(1).pdf -> report(2).pdf ...
	 */
	private File reserveTargetFile(File targetDir, String fileName) {
		File reserved = null;
		try {
			String baseName = fileName;
			String extension = "";
			int dot = fileName.lastIndexOf('.');
			// a leading dot means a dotfile such as .gitignore - treat the whole thing as the base
			if (dot > 0) {
				baseName = fileName.substring(0, dot);
				extension = fileName.substring(dot);
			}

			synchronized (FILE_CREATE_LOCK) {
				File candidate = new File(targetDir, fileName);
				int counter = 0;
				/*
				 * createNewFile() is atomic - it returns false when the file already
				 * exists, so the loop settles on a name nobody else has taken.
				 */
				while (!candidate.createNewFile()) {
					counter++;
					candidate = new File(targetDir, baseName + "(" + counter + ")" + extension);
					if (counter > 10000) {
						logger.info("reserveTargetFile :: Gave up finding a free name for :: > " + fileName);
						return null;
					}
				}
				reserved = candidate;
				candidate = null;
			}
			baseName = null;
			extension = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileUploadManager.class.getName(), "reserveTargetFile()", e);
			reserved = null;
		}
		return reserved;
	}

	/* ------------------------------------------------------------------ */
	/* DELETE                                                              */
	/* ------------------------------------------------------------------ */

	private void handleDelete(HttpServletRequest request, HttpServletResponse response) {
		try {
			String fileName = request.getParameter("file");
			fileName = sanitiseFileName(fileName);
			if (null == fileName || "".equals(fileName)) {
				writeJson(response, "{\"status\":\"ERROR\",\"message\":" + jsonString("No file name supplied.") + "}");
				return;
			}

			File targetDir = resolveTargetDirectory();
			if (null == targetDir) {
				writeJson(response, "{\"status\":\"ERROR\",\"message\":" + jsonString("Upload folder is not available.") + "}");
				return;
			}

			File target = new File(targetDir, fileName);
			// containment check - the resolved file must really sit inside the upload folder
			if (!isInsideDirectory(targetDir, target)) {
				logger.info("handleDelete :: REJECTED - resolved path escapes the upload folder :: > " + fileName);
				writeJson(response, "{\"status\":\"ERROR\",\"message\":" + jsonString("Invalid file name.") + "}");
				return;
			}
			if (!target.exists() || !target.isFile()) {
				writeJson(response, "{\"status\":\"ERROR\",\"message\":" + jsonString("File no longer exists.") + "}");
				return;
			}

			boolean deleted = target.delete();
			if (deleted) {
				logger.info("handleDelete :: Deleted file :: > " + target.getAbsolutePath());
				writeJson(response, "{\"status\":\"SUCCESS\",\"message\":" + jsonString("Deleted " + fileName + ".") + "}");
			} else {
				logger.info("handleDelete :: Failed to delete file :: > " + target.getAbsolutePath());
				writeJson(response, "{\"status\":\"ERROR\",\"message\":"
						+ jsonString("Could not delete the file. It may be open or read-only.") + "}");
			}
			target = null;
			targetDir = null;
			fileName = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileUploadManager.class.getName(), "handleDelete()", e);
			try {
				writeJson(response, "{\"status\":\"ERROR\",\"message\":" + jsonString(cleanMessage(e)) + "}");
			} catch (Exception ignore) {
				// response already committed
			}
		}
	}

	/* ------------------------------------------------------------------ */
	/* LIST                                                                */
	/* ------------------------------------------------------------------ */

	private void handleList(HttpServletRequest request, HttpServletResponse response) {
		try {
			StringBuilder json = new StringBuilder();
			json.append("{\"status\":\"SUCCESS\",\"relativePath\":").append(jsonString(getRelativePath()));
			json.append(",\"files\":[");

			File targetDir = resolveTargetDirectory();
			if (null != targetDir) {
				List<File> files = listFilesNewestFirst(targetDir);
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
				for (int a = 0; a < files.size(); a++) {
					File f = files.get(a);
					if (a > 0) {
						json.append(",");
					}
					json.append("{\"name\":").append(jsonString(f.getName()));
					json.append(",\"size\":").append(f.length());
					json.append(",\"sizeLabel\":").append(jsonString(formatSize(f.length())));
					json.append(",\"modified\":").append(jsonString(sdf.format(new Date(f.lastModified()))));
					json.append(",\"url\":").append(jsonString(getRelativePath() + encodeUrlSegment(f.getName())));
					json.append("}");
					f = null;
				}
				sdf = null;
				files = null;
			}
			json.append("]}");
			writeJson(response, json.toString());
			json = null;
			targetDir = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileUploadManager.class.getName(), "handleList()", e);
			try {
				writeJson(response, "{\"status\":\"ERROR\",\"message\":" + jsonString(cleanMessage(e)) + ",\"files\":[]}");
			} catch (Exception ignore) {
				// response already committed
			}
		}
	}

	private List<File> listFilesNewestFirst(File targetDir) {
		List<File> result = new ArrayList<File>();
		try {
			File[] found = targetDir.listFiles();
			if (null != found && found.length > 0) {
				for (int a = 0; a < found.length; a++) {
					// only plain files are listed - sub folders are out of scope
					if (null != found[a] && found[a].isFile()) {
						result.add(found[a]);
					}
				}
				File[] asArray = result.toArray(new File[result.size()]);
				Arrays.sort(asArray, new Comparator<File>() {
					public int compare(File f1, File f2) {
						long diff = f2.lastModified() - f1.lastModified();
						if (diff > 0) {
							return 1;
						}
						if (diff < 0) {
							return -1;
						}
						return f1.getName().compareToIgnoreCase(f2.getName());
					}
				});
				result = new ArrayList<File>(Arrays.asList(asArray));
				asArray = null;
			}
			found = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileUploadManager.class.getName(), "listFilesNewestFirst()", e);
		}
		return result;
	}

	/* ------------------------------------------------------------------ */
	/* HELPERS                                                             */
	/* ------------------------------------------------------------------ */

	/**
	 * Resolves the configured physical folder, optionally creating it.
	 * Returns null when it cannot be used, so callers can report a clean error.
	 */
	private File resolveTargetDirectory() {
		File dir = null;
		try {
			String path = ApplicationProperties.getProperty("fileupload.physicalpath");
			if (null == path || "".equals(path.trim())) {
				logger.info("resolveTargetDirectory :: fileupload.physicalpath is not configured.");
				return null;
			}
			dir = new File(path.trim());
			if (!dir.exists()) {
				String createFlag = ApplicationProperties.getProperty("fileupload.createdir.ifmissing");
				if (null != createFlag && "TRUE".equalsIgnoreCase(createFlag.trim())) {
					boolean made = dir.mkdirs();
					logger.info("resolveTargetDirectory :: Folder missing, mkdirs() returned " + made + " for :: > " + dir.getAbsolutePath());
				}
			}
			if (!dir.exists() || !dir.isDirectory()) {
				logger.info("resolveTargetDirectory :: Upload folder is not reachable :: > " + dir.getAbsolutePath());
				dir = null;
			}
			path = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileUploadManager.class.getName(), "resolveTargetDirectory()", e);
			dir = null;
		}
		return dir;
	}

	private String getRelativePath() {
		String relative = ApplicationProperties.getProperty("fileupload.relativepath");
		if (null == relative || "".equals(relative.trim())) {
			return "";
		}
		relative = relative.trim();
		if (!relative.endsWith("/")) {
			relative = relative + "/";
		}
		// as the browser reaches it: under the OKAssets web context
		return com.mazda.gms3.dmt.utils.OkAssetsWeb.url(relative);
	}

	private String getPhysicalPathForDisplay() {
		String path = ApplicationProperties.getProperty("fileupload.physicalpath");
		if (null == path) {
			return "";
		}
		return path.trim();
	}

	/**
	 * Reduces whatever the browser sent to a bare file name.
	 * IE sends a full client path; every path separator is stripped, and anything that
	 * could walk the filesystem is refused.
	 */
	private String sanitiseFileName(String rawName) {
		String result = null;
		try {
			if (null == rawName || "".equals(rawName.trim())) {
				return null;
			}
			result = rawName.trim();

			// strip any directory component sent by the client (IE sends C:\path\file.txt)
			int slash = result.lastIndexOf('/');
			if (slash != -1) {
				result = result.substring(slash + 1);
			}
			slash = result.lastIndexOf('\\');
			if (slash != -1) {
				result = result.substring(slash + 1);
			}

			result = result.trim();

			// refuse traversal and control characters outright
			if ("".equals(result) || ".".equals(result) || "..".equals(result)) {
				return null;
			}
			// refuse null bytes and control characters (spaces in names are legal)
			for (int a = 0; a < result.length(); a++) {
				if (result.charAt(a) < 0x20) {
					return null;
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileUploadManager.class.getName(), "sanitiseFileName()", e);
			result = null;
		}
		return result;
	}

	/**
	 * Confirms the candidate really resolves inside the upload folder.
	 * Belt and braces on top of sanitiseFileName.
	 */
	private boolean isInsideDirectory(File parentDir, File candidate) {
		boolean inside = false;
		try {
			String parentPath = parentDir.getCanonicalPath();
			String childPath = candidate.getCanonicalPath();
			if (!parentPath.endsWith(File.separator)) {
				parentPath = parentPath + File.separator;
			}
			inside = childPath.startsWith(parentPath);
			parentPath = null;
			childPath = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileUploadManager.class.getName(), "isInsideDirectory()", e);
			inside = false;
		}
		return inside;
	}

	/** Percent-encodes a file name for use inside a URL, keeping it readable. */
	private String encodeUrlSegment(String name) {
		String encoded = name;
		try {
			encoded = java.net.URLEncoder.encode(name, "UTF-8");
			// URLEncoder targets form bodies - restore the characters that are legal in a path
			encoded = encoded.replace("+", "%20");
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileUploadManager.class.getName(), "encodeUrlSegment()", e);
			encoded = name;
		}
		return encoded;
	}

	private String formatSize(long bytes) {
		try {
			if (bytes < 1024L) {
				return bytes + " B";
			}
			if (bytes < 1024L * 1024L) {
				return (bytes / 1024L) + " KB";
			}
			if (bytes < 1024L * 1024L * 1024L) {
				return String.format("%.1f MB", new Object[] { new Double(bytes / (1024.0 * 1024.0)) });
			}
			return String.format("%.2f GB", new Object[] { new Double(bytes / (1024.0 * 1024.0 * 1024.0)) });
		} catch (Exception e) {
			return bytes + " B";
		}
	}

	private String cleanMessage(Exception e) {
		if (null == e) {
			return "Unexpected error.";
		}
		if (null != e.getMessage() && !"".equals(e.getMessage().trim())) {
			return e.getMessage().trim();
		}
		return e.getClass().getName();
	}

	private void writeJson(HttpServletResponse response, String json) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache, no-store");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		PrintWriter out = response.getWriter();
		out.write(json);
		out.flush();
		out = null;
	}

	/** Minimal JSON string encoder - avoids pulling in a JSON library for four fields. */
	private String jsonString(String value) {
		if (null == value) {
			return "\"\"";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("\"");
		for (int a = 0; a < value.length(); a++) {
			char c = value.charAt(a);
			switch (c) {
			case '"':
				sb.append("\\\"");
				break;
			case '\\':
				sb.append("\\\\");
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '/':
				sb.append("\\/");
				break;
			default:
				if (c < 0x20 || c > 0x7e) {
					// escape control chars and anything non-ASCII so the payload stays ASCII-safe
					sb.append(String.format("\\u%04x", new Object[] { new Integer(c) }));
				} else {
					sb.append(c);
				}
				break;
			}
		}
		sb.append("\"");
		return sb.toString();
	}
}
