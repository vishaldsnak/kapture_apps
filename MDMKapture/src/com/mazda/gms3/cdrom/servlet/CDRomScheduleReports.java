package com.mazda.gms3.cdrom.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.mazda.gms3.cdrom.bean.CDRomScheduleItemDetails;
import com.mazda.gms3.cdrom.dao.CDRomDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.GenerateFinalResponse;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;

/**
 * Servlet implementation class ScheduleReports
 */
public class CDRomScheduleReports extends HttpServlet {
	private static final long serialVersionUID = 1L;

	static Logger logger = LogManager.getLogger(CDRomScheduleReports.class);

	static MessageProperties msgProps = null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public CDRomScheduleReports() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		msgProps = new MessageProperties(request.getSession().getAttribute(
				"MDM_LS_Locale"));
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			msgProps = new MessageProperties(request.getSession().getAttribute(
					"MDM_LS_Locale"));
			if (null != request.getParameter("scheduleId")
					&& !"".equals(request.getParameter("scheduleId"))) {
				downloadReport(request, response);
			}
			if (null != request.getParameter("MDM_HISTORY_VIEW_REPORTS") && !"".equals(request.getParameter("MDM_HISTORY_VIEW_REPORTS"))) 
			{
				performViewReportOperation(request, response);
			}

			if (null != request.getParameter("MDM_HISTORY_VIEW_ITEM_DETAILS") && !"".equals(request.getParameter("MDM_HISTORY_VIEW_ITEM_DETAILS"))) 
			{
				performViewItemDetailsOperation(request, response);
			}
		} catch (Exception e) {
			logger.info("doPost :: ################ Exception ################");
			Utilities.printStackTraceToLogs(
					CDRomScheduleReports.class.getName(), "doPost()", e);
			logger.info("doPost :: ################ Exception ################");
		}
	}

	private void downloadReport(HttpServletRequest request,
			HttpServletResponse response) {

		String scheduleCode = "";
		String type = "";
		if (null != request.getParameter("scheduleId")
				&& !"".equals(request.getParameter("scheduleId"))) {
			scheduleCode = (String) request.getParameter("scheduleId");
		}
		if (null != request.getParameter("type")
				&& !"".equals(request.getParameter("type"))) {
			type = (String) request.getParameter("type");
		}
		try {
			String zipFileName = CDRomDAO.getZipFileName(scheduleCode);
			String filename = "";
			if (type.equalsIgnoreCase("downloadContent")) {
				int index = zipFileName.lastIndexOf('/');
				filename = zipFileName.substring(index + 1);
			} else if (type.equalsIgnoreCase("downloadReport")) {
				int index = zipFileName.lastIndexOf('.');
				zipFileName = zipFileName.substring(0, index);
				zipFileName = zipFileName + "_Reports" + ".zip";
				index = zipFileName.lastIndexOf('/');
				filename = zipFileName.substring(index + 1);
			}
			OutputStream outStream = response.getOutputStream();
			response.setContentType("application/zip");
			response.setHeader("Content-Disposition", "attachment; filename="
					+ filename + ";");
			FileInputStream fileInputStream = new FileInputStream(zipFileName);

			int i;
			while ((i = fileInputStream.read()) != -1) {
				outStream.write(i);
			}
			outStream.close();
			fileInputStream.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * CALL FUNCTION TO GENERATE REPORTS
	 * 
	 * @param request
	 * @param response
	 */
	private static void performViewReportOperation(HttpServletRequest request,
			HttpServletResponse response) {
		String token = "";
		String responseString = "";
		try {
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("MDM_SCHEDULE_CODE")
					&& !"".equals(request.getParameter("MDM_SCHEDULE_CODE"))
					&& null != request.getParameter("MDM_SCHEDULE_STATUS")
					&& !"".equals(request.getParameter("MDM_SCHEDULE_STATUS"))) {
				String scheduleCode = (String) request.getParameter("MDM_SCHEDULE_CODE");
				String type = (String) request.getParameter("MDM_SCHEDULE_STATUS");
				String path = CDRomDAO.getZipFileName(scheduleCode);
				File filepath = new File(path);
				if (filepath.exists()) {
					/*
					 *  path will be //m1okfs10/okassets/library/MAZDA/GMS3_CUSTOM/CDROM/CD_ROM_1643165301255.zip
					 *  remove the okassets physical context and make it relative URL
					 */
					if(path.indexOf("/library")!=-1)
					{
						path = path.substring(path.indexOf("/library"),path.length());
					}
					String filename = "";
					if (type.equalsIgnoreCase("downloadContent")) {
						int index = path.lastIndexOf('/');
						filename = path.substring(index + 1);
					} else if (type.equalsIgnoreCase("downloadReport")) {
						int index = path.lastIndexOf('.');
						path = path.substring(0, index);
						path = path + "_Reports" + ".zip";
						index = path.lastIndexOf('/');
						filename = path.substring(index + 1);
					}
					/*
					 * now prepare the path for the reports directory
					 */
					if (StringUtils.isNotBlank(filename)) {
						token = "DATA";

						String data = "<table width=\\\"100%\\\" class=\\\"display historyTable\\\" cellpadding=\\\"0\\\" style=\\\"border-left: 1px solid #DADADA;\\\"cellspacing=\\\"0\\\">";
						data = data + "<tr>";
						data = data + "<td><strong>" + filename
								+ "</strong></td>";
						data = data + "<td>";
//						data = data + "<a href=\\\"#"
//								+ "\\\"  onclick=\\\"dmt_viewReports('"
//								+ scheduleCode + "','" + type
//								+ "')\\\"style=\\\"text-decoration:none;\\\">";
						data = data + "<a href=\\\""+path+""
								+ "\\\"  target=\\\"_blank\\\" style=\\\"text-decoration:none;\\\">";
						data = data + "<i class=\\\"downloadReports\\\"></i>";
						data = data + "</a>";
						data = data + "</td>";
						data = data + "</tr>";
						data = data + "</table>";
						responseString = data;
						data = null;
					}
				} else {
					token = "DATA";
					responseString = "<p>";
					responseString = responseString + "<strong>"
							+ msgProps.getProperty("label.no.reports.found")
							+ "</strong>";
					responseString = responseString + "</p>";
				}
			}
		} catch (Exception e) {
			logger.info("performViewReportOperation :: ################ Exception ################");
			Utilities.printStackTraceToLogs(
					CDRomScheduleReports.class.getName(),
					"performViewReportOperation()", e);
			logger.info("performViewReportOperation :: ################ Exception ################");
			token = "DATA";
			responseString = "<p>";
			responseString = responseString + "<strong>"
					+ msgProps.getProperty("label.no.reports.found")
					+ "</strong>";
			responseString = responseString + "</p>";
		}
		GenerateFinalResponse.generateFinalResponse(response, token,
				responseString);
		token = null;
		responseString = null;
	}

	private static void performViewItemDetailsOperation(
			HttpServletRequest request, HttpServletResponse response) {
		String token = "";
		String responseString = "";
		try {
			logger.info("performViewItemDetailsOperation :: Starting ############################");
			/*
			 * check for schedule code
			 */
			if (null != request.getParameter("MDM_SCHEDULE_CODE")
					&& !"".equals(request.getParameter("MDM_SCHEDULE_CODE"))) {
				String scheduleCode = (String) request
						.getParameter("MDM_SCHEDULE_CODE");
				logger.info("performViewItemDetailsOperation :: Schedule Code :: > "+ scheduleCode);
				/*
				 * now call function to fetch Item Details for the Schedule Id
				 */
				ArrayList<CDRomScheduleItemDetails> itemsList = CDRomDAO
						.getScheduleItemDetails(scheduleCode);
				
				if (null != itemsList && itemsList.size() > 0) {
					logger.info("performViewItemDetailsOperation :: ItemsList Size :: > "+ itemsList.size());
					String data = "";
					data = data
							+ "<table width=\\\"100%\\\" cellspacing=\\\"0\\\" cellpadding=\\\"0\\\" >";
					data = data + "<tr>";
					data = data + "<td>";
					data = data
							+ "<table class=\\\"historyTable\\\" cellspacing=\\\"0\\\" width=\\\"100%\\\" style=\\\"border-left:1px solid #DADADA;\\\">";
					data = data + "<thead>";
					data = data + "<tr>";
					data = data
							+ "<th scope=\\\"col\\\" width=\\\"3%\\\">#.</th>";
					// data=data+"<th scope=\\\"col\\\" width=\\\"6%\\\">"+msgProps.getProperty("label.market")+"</th>";
					data = data + "<th scope=\\\"col\\\" width=\\\"7%\\\">"
							+ msgProps.getProperty("label.locale") + "</th>";
					data = data + "<th scope=\\\"col\\\" width=\\\"7%\\\">"
							+ msgProps.getProperty("label.model") + "</th>";
					data = data + "<th scope=\\\"col\\\" width=\\\"7%\\\">"
							+ msgProps.getProperty("label.wmi") + "</th>";
					data = data + "<th scope=\\\"col\\\" width=\\\"7%\\\">"
							+ msgProps.getProperty("label.vds") + "</th>";
					data = data + "<th scope=\\\"col\\\" width=\\\"7%\\\">"
							+ msgProps.getProperty("label.vis") + "</th>";
					data = data + "<th scope=\\\"col\\\" width=\\\"10%\\\">"
							+ msgProps.getProperty("label.manualtype")
							+ "</th>";
					// data=data+"<th scope=\\\"col\\\" width=\\\"12%\\\">"+msgProps.getProperty("label.totaldocsforprocessing")+"</th>";
					data = data + "<th scope=\\\"col\\\" width=\\\"11%\\\">"
							+ msgProps.getProperty("label.documents") + " - ("
							+ msgProps.getProperty("label.total") + " / "
							+ msgProps.getProperty("label.processed") + " / "
							+ msgProps.getProperty("label.failed") + ")</th>";
					data = data + "<th scope=\\\"col\\\" width=\\\"10%\\\">"
							+ msgProps.getProperty("label.okassetscount")
							+ " - (" + msgProps.getProperty("label.total")
							+ " / " + msgProps.getProperty("label.processed")
							+ " / " + msgProps.getProperty("label.failed")
							+ ")</th>";
					data = data + "<th scope=\\\"col\\\" width=\\\"10%\\\">"
							+ msgProps.getProperty("label.innerlinkscount")
							+ " - (" + msgProps.getProperty("label.total")
							+ " / " + msgProps.getProperty("label.processed")
							+ " / " + msgProps.getProperty("label.failed")
							+ ")</th>";
					data = data + "<th scope=\\\"col\\\" width=\\\"10%\\\">"
							+ msgProps.getProperty("label.contentstatus")
							+ "</th>";

					data = data + "</tr>";
					data = data + "</thead>";
					data = data + "<tbody>";
					StringBuilder rowsData = new StringBuilder();
					for (int i = 0; i < itemsList.size(); i++) {
						CDRomScheduleItemDetails itemDetails = (CDRomScheduleItemDetails) itemsList
								.get(i);
						String status = itemDetails
								.getCurrentProcessingStatus();
						if (StringUtils.isBlank(status)) {
							status = "Pending";
						}
						rowsData.append("<tr>");
						rowsData.append("<td>" + itemDetails.getSrNo()
								+ "</td>");
						// rowsData.append("<td>"+itemDetails.getMarket()+"</td>");
						rowsData.append("<td>" + itemDetails.getLocale()
								+ "</td>");
						rowsData.append("<td>" + itemDetails.getCarlineCode()
								+ "</td>");
						rowsData.append("<td>" + itemDetails.getWmiCode()
								+ "</td>");
						rowsData.append("<td>" + itemDetails.getVdsCode()
								+ "</td>");
						rowsData.append("<td>" + itemDetails.getVinRange()
								+ "</td>");

						rowsData.append("<td>"
								+ itemDetails.getManualTypeName() + "</td>");
						rowsData.append("<td>"
								+ itemDetails.getTotalDocsForProcessing()
								+ " / " + itemDetails.getProcessedDocsCount()
								+ " / "
								+ itemDetails.getFailedProcessedDocsCount()
								+ "</td>");
						rowsData.append("<td>" + itemDetails.getOkAssetsCount()
								+ " / "
								+ itemDetails.getProcessedOkAssetsCount()
								+ " / " + itemDetails.getFailedOkAssetsCount()
								+ "</td>");
						rowsData.append("<td>"
								+ itemDetails.getTotalInnerLinksCount() + " / "
								+ itemDetails.getProcessedInnerLinksCount()
								+ " / "
								+ itemDetails.getFailedInnerLinksCount()
								+ "</td>");
						rowsData.append("<td>" + status + "</td>");
						rowsData.append("</tr>");
					}
					if (null != rowsData) {
						data = data + rowsData.toString();
					}
					data = data + "</tbody>";
					data = data + "</table>";
					data = data + "</td>";
					data = data + "</tr>";
					data = data + "</table>";

					token = "DATA";
					responseString = data;
					data = null;
					rowsData = null;
				} else {
					logger.info("performViewItemDetailsOperation :: ItemsList is 0 for Schedule Code :: > "+ scheduleCode);
					token = "DATA";
					responseString = "<p>";
					responseString = responseString + "<strong>"
							+ msgProps.getProperty("label.norecordsfound")
							+ "</strong>";
					responseString = responseString + "</p>";
				}
				scheduleCode = null;
			} else {
				token = "DATA";
				responseString = "<p>";
				responseString = responseString + "<strong>"
						+ msgProps.getProperty("label.norecordsfound")
						+ "</strong>";
				responseString = responseString + "</p>";
			}
		} catch (Exception e) {
			logger.info("performViewItemDetailsOperation :: ################ Exception ################");
			Utilities.printStackTraceToLogs(
					CDRomScheduleReports.class.getName(),
					"performViewItemDetailsOperation()", e);
			logger.info("performViewItemDetailsOperation :: ################ Exception ################");
			token = "DATA";
			responseString = "<p>";
			responseString = responseString + "<strong>"
					+ msgProps.getProperty("label.norecordsfound")
					+ "</strong>";
			responseString = responseString + "</p>";
		}
		GenerateFinalResponse.generateFinalResponse(response, token,
				responseString);
		token = null;
		responseString = null;
	}

}