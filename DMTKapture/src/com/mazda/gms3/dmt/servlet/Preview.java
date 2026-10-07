package com.mazda.gms3.dmt.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.dmt.bean.UserAccessBean;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.preview.PreviewBuilder;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.PathUtil;
import com.mazda.gms3.dmt.utils.Utilities;

/**
 * CONTENT PREVIEW OF ONE JOB - opened from the History page in a new tab.
 *
 *   /preview?schedule=<id>[&name=<schedule name>]   the page
 *   /preview?schedule=<id>&data=Y                    the job's preview.json
 *   POST /preview schedule=<id>&action=publish       schedules the Publish Content job (JSON answer)
 *
 * The page reads ONLY the files the job wrote into its preview folder - no database, no Kapture.
 */
public class Preview extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Logger logger = LogManager.getLogger(Preview.class);

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			if (ApplicationProperties.getProperty("wsl.check").equals("TRUE")) {
				String wslId = request.getHeader("iv-user");
				if (null == wslId || "".equals(wslId)) {
					response.sendRedirect(request.getContextPath() + "/error");
					return;
				}
			}
			Object bean = request.getSession().getAttribute("userAccessBean");
			if (bean instanceof UserAccessBean && ((UserAccessBean) bean).isShowNoAccess()) {
				response.sendRedirect(request.getContextPath() + "/noaccess");
				return;
			}
			String scheduleId = request.getParameter("schedule");
			if (null == scheduleId || !scheduleId.trim().matches("[0-9]{1,18}")) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "schedule is missing or not a number");
				return;
			}
			scheduleId = scheduleId.trim();
			// the database decides: a deleted preview folder can come back on the S3 mount
			boolean available = !com.mazda.gms3.dmt.preview.PreviewDAO.jobsWithUnpublished(java.util.Collections.singletonList(scheduleId)).isEmpty()
					&& PreviewBuilder.hasPreview(scheduleId);

			if ("Y".equals(request.getParameter("data"))) {
				if (!available) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND, "no preview for this job");
					return;
				}
				sendPreviewFile(scheduleId, response);
				return;
			}
			request.setAttribute("previewScheduleId", scheduleId);
			request.setAttribute("previewScheduleName", null == request.getParameter("name") ? "" : request.getParameter("name"));
			request.setAttribute("previewAvailable", available);
			request.setAttribute("previewWebFolder", PreviewBuilder.jobWebFolder(scheduleId));
			RequestDispatcher rs = request.getRequestDispatcher("/jsps/preview.jsp");
			rs.forward(request, response);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(Preview.class.getName(), "doGet()", e);
			if (!response.isCommitted()) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}

	/**
	 * POST /preview  schedule=<id>&action=publish  - schedules the Publish Content job of the job and
	 * starts it. Answers JSON {ok, message, name}.
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		com.google.gson.JsonObject answer = new com.google.gson.JsonObject();
		try {
			String wslId = request.getHeader("iv-user");
			if (ApplicationProperties.getProperty("wsl.check").equals("TRUE") && (null == wslId || "".equals(wslId))) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			Object bean = request.getSession().getAttribute("userAccessBean");
			if (bean instanceof UserAccessBean && ((UserAccessBean) bean).isShowNoAccess()) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			String scheduleId = request.getParameter("schedule");
			if (!"publish".equals(request.getParameter("action")) || null == scheduleId || !scheduleId.trim().matches("[0-9]{1,18}")) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "schedule / action missing");
				return;
			}
			final String source = scheduleId.trim();
			final String user = null == wslId ? "" : wslId;
			long publishId;
			synchronized (com.mazda.gms3.dmt.publish.PublishDAO.SCHEDULE_LOCK) {
				com.mazda.gms3.dmt.publish.PublishDAO.Refusal refusal = com.mazda.gms3.dmt.publish.PublishDAO.refusal(source);
				if (null != refusal) {
					answer.addProperty("ok", false);
					answer.addProperty("message", refusal.message);
					// the items other jobs are processing - shown as a table
					com.google.gson.JsonArray busy = new com.google.gson.JsonArray();
					for (String[] b : refusal.busy) {
						com.google.gson.JsonObject o = new com.google.gson.JsonObject();
						o.addProperty("model", b[0]);
						o.addProperty("manualType", b[1]);
						o.addProperty("job", b[2]);
						o.addProperty("jobType", "PUBLISH".equals(b[3]) ? "Publish Content" : "Content conversion");
						busy.add(o);
					}
					answer.add("busy", busy);
					send(response, answer);
					return;
				}
				publishId = com.mazda.gms3.dmt.publish.PublishDAO.createSchedule(source, user,
						com.mazda.gms3.dmt.publish.PublishDAO.loadDocuments(source));
			}
			final String id = String.valueOf(publishId);
			String name = com.mazda.gms3.dmt.publish.PublishDAO.scheduleName(publishId);
			Thread th = new Thread(new Runnable() {
				public void run() {
					try {
						new com.mazda.gms3.dmt.publish.PublishContentImpl(id, source, user).run();
					} catch (Throwable e) {
						logger.info("publish :: job " + id + " ended :: " + e);
					}
				}
			}, com.mazda.gms3.dmt.publish.PublishDAO.marketOf(source).publishPrefix() + id);
			th.start();
			logger.info("doPost :: publish job " + name + " (schedule " + id + ") started for job " + source + " by " + user);
			answer.addProperty("ok", true);
			answer.addProperty("name", name);
			answer.addProperty("message", "Publish job " + name + " has been scheduled. Follow it on the History page.");
			send(response, answer);
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(Preview.class.getName(), "doPost()", e);
			answer.addProperty("ok", false);
			answer.addProperty("message", "The publish job could not be scheduled: " + e.getMessage());
			send(response, answer);
		}
	}

	private static void send(HttpServletResponse response, com.google.gson.JsonObject answer) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-store");
		response.getWriter().write(answer.toString());
	}

	private static void sendPreviewFile(String scheduleId, HttpServletResponse response) throws IOException {
		File f = PathUtil.file(PreviewBuilder.jobFolder(scheduleId), PreviewBuilder.PREVIEW_FILE);
		if (!f.isFile()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "no preview for this job");
			return;
		}
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-store");
		response.setContentLength((int) f.length());
		InputStream in = PathUtil.fileInputStream(f);
		try {
			OutputStream out = response.getOutputStream();
			byte[] buf = new byte[65536];
			int n;
			while ((n = in.read(buf)) > 0) {
				out.write(buf, 0, n);
			}
		} finally {
			in.close();
		}
	}
}
