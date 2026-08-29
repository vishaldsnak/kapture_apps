package com.mazda.gms3.mdm.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;

/**
 * Servlet implementation class SITranslationOtherOps
 */
public class SITranslationOtherOps extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = LogManager.getLogger(SITranslationOtherOps.class);
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SITranslationOtherOps() {
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
		boolean showError=false;
		try
		{
			if(null!=request.getParameter("SITU_OtherOps_ActionClicked") && !"".equals(request.getParameter("SITU_OtherOps_ActionClicked")))
			{
				logger.info("doPost :: Action Clicked :: >"+ request.getParameter("SITU_OtherOps_ActionClicked"));
				if(request.getParameter("SITU_OtherOps_ActionClicked").equals("DOWNLOAD_EXPORT_TEMPLATE"))
				{
					XSSFWorkbook workbook = new XSSFWorkbook();
					XSSFSheet sheet = workbook.createSheet("Details");
					XSSFRow row = sheet.createRow(0);
					XSSFCell cell = row.createCell(0);
					cell.setCellValue("Document Id");
					cell = null;
					row=  null;
					// write work book to response
					response.setCharacterEncoding("UTF-8");
					response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
					response.setHeader("Content-Disposition","attachment; filename=Translation_Export_Template.xlsx");
					workbook.write(response.getOutputStream());
					response.getOutputStream().flush();
					response.getOutputStream().close();
					logger.info("doPost :: > Setting Response Stream." );
					workbook.close();
					workbook=  null;
					sheet= null;
				}
					
				if(request.getParameter("SITU_OtherOps_ActionClicked").equals("VIEW_REPORT"))
				{
					String scheduleId="";
					String operationType="";
					if(null!=request.getParameter("SITU_OtherOps_SchId") && !"".equals(request.getParameter("SITU_OtherOps_SchId")))
					{
						scheduleId=  (String)request.getParameter("SITU_OtherOps_SchId");
					}
					if(null!=request.getParameter("SITU_OtherOps_OpType") && !"".equals(request.getParameter("SITU_OtherOps_OpType")))
					{
						operationType = (String)request.getParameter("SITU_OtherOps_OpType");
					}
					logger.info("doPost :: View Report :: Schedule Id >"+ scheduleId);
					logger.info("doPost :: View Report :: Operation Type >"+ operationType);
					if(null!=scheduleId && !"".equals(scheduleId) && null!=operationType && !"".equals(operationType))
					{
						/*
						 * READ REPORT FILE FROM THE EXPORT DIR PHYSICAL LOCATION 
						 * IF FOUND - THEN RENDER ON SCREEN.
						 * ELSE SHOW ERROR
						 */
						String path="";
						if(operationType.trim().equals(AccessManagementInterface.OPERATION_TYPE_EXPORT))
						{
							path = ApplicationProperties.getProperty("translation.update.export.physical.path");
						}
						else if(operationType.trim().equals(AccessManagementInterface.OPERATION_TYPE_IMPORT))
						{
							path = ApplicationProperties.getProperty("translation.update.import.physical.path");
						}
						
						if(!path.endsWith("/"))
						{
							path = path+"/";
						}
						path = path+scheduleId+"/"+scheduleId+"_"+operationType.trim().toUpperCase()+"_TRANSACTION_REPORT.xlsx";
						logger.info("doPost :: View Report :: > Path :: >"+ path);
						// read excel
						try
						{
							File file = new File(path);
							if(file.exists() && file.isFile())
							{
								logger.info("doPost :: View Report :: File Exists :: >");
								XSSFWorkbook workbook = new XSSFWorkbook(file);
								response.setCharacterEncoding("UTF-8");
								response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
								response.setHeader("Content-Disposition","attachment; filename="+file.getName());
								workbook.write(response.getOutputStream());
								response.getOutputStream().flush();
								response.getOutputStream().close();
								logger.info("doPost :: View Report :: Response Set.");
								
								workbook.close();
								workbook=  null;
							}
							else
							{
								showError=  true;
							}
							file= null;
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(SITranslationOtherOps.class.getName(), "doPost()", e);
							showError=true;
						}
						path = null;
					}
					else
					{
						showError = true;
					}
					scheduleId=  null;
					operationType=  null;
				}
				else if(request.getParameter("SITU_OtherOps_ActionClicked").equals("VIEW_XMLS"))
				{
					logger.info("doPost :: View XMLs Operation");
					// Operation Type will always be Export Only

					String scheduleId="";
					if(null!=request.getParameter("SITU_OtherOps_SchId") && !"".equals(request.getParameter("SITU_OtherOps_SchId")))
					{
						scheduleId=  (String)request.getParameter("SITU_OtherOps_SchId");
					}
					logger.info("doPost :: View XMLS :: Schedule Id :: >" + scheduleId);
					if(null!=scheduleId && !"".equals(scheduleId))
					{
						/*
						 * READ XML ZIP FILE FROM THE EXPORT DIR PHYSICAL LOCATION 
						 * IF FOUND - THEN RENDER ON SCREEN.
						 * ELSE SHOW ERROR
						 */
						String path=ApplicationProperties.getProperty("translation.update.export.physical.path");
						
						if(!path.endsWith("/"))
						{
							path = path+"/";
						}
						path = path+scheduleId+"/"+scheduleId+"_XMLS"+ApplicationProperties.getProperty("ZIP_SUFFIX");
						logger.info("doPost :: View XMLS :: Path :: >" + path);
						// read excel
						try
						{
							File file = new File(path);
							if(file.exists() && file.isFile())
							{
								logger.info("doPost :: View XMLS :: File Exists :: >");
								response.setContentType("application/zip");
								response.addHeader("Content-Disposition", "attachment; filename=" +file.getName());
								response.setContentLength((int) file.length());
								FileInputStream fileInputStream = new FileInputStream(file);
					            int bytes;
					            while ((bytes = fileInputStream.read()) != -1) {
					                response.getOutputStream().write(bytes);
					            }
					            response.getOutputStream().flush();
								response.getOutputStream().close();
								logger.info("doPost :: View XMLs :: Response Set.");
								
								fileInputStream.close();
								fileInputStream = null;
							}
							else
							{
								showError=  true;
							}
							file= null;
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(SITranslationOtherOps.class.getName(), "doPost()", e);
							showError=true;
						}
						path = null;
					}
					else
					{
						showError = true;
					}
					scheduleId=  null;
				}
			}
			else
			{
				showError = true;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SITranslationOtherOps.class.getName(), "doPost()", e);
			showError = true;
		}
		
		logger.info("----------- Show Error :: > "+ showError);
		if(showError==true)
		{
			// NO DOCUMENTS FOUND - REPORT SHOULD NOT GENERATE
			response.setCharacterEncoding("UTF-8");
			 //set the MIME type of the response, "text/html"
		    response.setContentType("text/html");

		    //use a PrintWriter send text data to the client who has requested the
		    // servlet
		    java.io.PrintWriter out = response.getWriter();

		    //Begin assembling the HTML content
		    out.println("<html><head>");
		    
		    Timestamp ts = new Timestamp(new Date().getTime());
		    out.println("<title>MDM</title></head><body><br/>");
		    
		    out.println("<p style=\"font-family: Times New Roman;\">Error: "+ ts.getTime()+" </p>");
		    ts=null;

//		    out.println("<table border=\"0\" width=\"100%\"><tr><td valign=\"center\">");
//		    out.println("No records found. </td></tr>");
//		    out.println("</table>");
		    out.println("</body></html>");
		}
		
	}

}
