package com.mazda.gms3.mdm.servlet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ImportTemplateUtils;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.ImportTemplateDetails;

/**
 * Servlet implementation class ImportTemplate
 *
 * Serves the blank import template of ANY screen - the screen is chosen with the "screen"
 * request parameter, whose value is a key registered in ImportTemplateUtils. One servlet
 * therefore covers every import screen; a new screen needs no change here.
 *
 * The workbook is generated in memory and streamed straight back, so nothing is written to
 * disk and there is no temporary file to clean up.
 */
public class ImportTemplate extends HttpServlet {
	private static final long serialVersionUID = 1L;

	static Logger logger = LogManager.getLogger(ImportTemplate.class);

	/** Request parameter carrying the screen key. */
	private static final String PARAM_SCREEN = "screen";

	/**
	 * Request parameter carrying the manual language currently selected on the screen.
	 * Only needed by the screens whose columns depend on the country / language (Carline);
	 * the others ignore it.
	 */
	private static final String PARAM_LANGUAGE = "ml";

	public ImportTemplate() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		OutputStream os = null;
		try {
			String screenKey = request.getParameter(PARAM_SCREEN);
			String manualLanguageId = request.getParameter(PARAM_LANGUAGE);
			ImportTemplateDetails template = ImportTemplateUtils.getTemplate(screenKey,
					manualLanguageId);

			if (null == template) {
				/*
				 * Distinguish the two reasons, otherwise a variant screen that was opened
				 * without a language selected looks like an unregistered screen.
				 */
				if (ImportTemplateUtils.isVariantScreen(screenKey)) {
					logger.info("doGet() :: Screen {" + screenKey + "} needs the country / language"
							+ " selection to decide the template layout. None supplied.");
					response.sendError(HttpServletResponse.SC_BAD_REQUEST,
							"Please select the Country Locale and Manual Language first.");
					return;
				}
				logger.info("doGet() :: No import template registered for screen :: >" + screenKey);
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			byte[] data = ImportTemplateUtils.buildTemplateWorkbook(template);
			String fileName = ImportTemplateUtils.buildFileName(template);

			/*
			 * Reset first - a filter has already stamped the response as text/html, and the
			 * download must carry the spreadsheet content type instead.
			 */
			response.reset();
			response.setContentType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
			response.setContentLength(data.length);
			// The template is generated per request - never let it be cached.
			response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
			response.setHeader("Pragma", "no-cache");
			response.setDateHeader("Expires", 0);

			os = response.getOutputStream();
			os.write(data);
			os.flush();

			logger.info("doGet() :: Import template served :: >" + fileName);
			data = null;
			fileName = null;
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(ImportTemplate.class.getName(), "doGet()", e);
			if (!response.isCommitted()) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		} finally {
			if (null != os) {
				try {
					os.close();
				} catch (Exception e) {
					// response stream already closed by the container
				}
			}
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
