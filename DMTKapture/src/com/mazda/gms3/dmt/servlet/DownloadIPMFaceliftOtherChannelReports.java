package com.mazda.gms3.dmt.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.bean.ScheduleBean;
import com.mazda.gms3.dmt.utils.Utilities;

/**
 * Servlet implementation class DownloadIPMFaceliftOtherChannelReports
 */
public class DownloadIPMFaceliftOtherChannelReports extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DownloadIPMFaceliftOtherChannelReports() {
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
		try
		{
			if(null!=request.getParameter("item") && !"".equals(request.getParameter("item")))
			{
				ScheduleBean sessionBean = getSessionBean(request);
				String srNo= (String)request.getParameter("item") ;
				
				if(null!=sessionBean.getAutomationItemsList() && sessionBean.getAutomationItemsList().size()>0)
				{
					ItemDetails itemDetails = null;
					for(int a=0;a<sessionBean.getAutomationItemsList().size();a++)
					{
						itemDetails = (ItemDetails)sessionBean.getAutomationItemsList().get(a);
						if(itemDetails.getSrNo()== new Integer(srNo).intValue())
						{
							if(null!=itemDetails.getImpactedDocumentsList() && itemDetails.getImpactedDocumentsList().size()>0)
							{
								printOtherChannelReports(itemDetails, response);
							}
							break;
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DownloadIPMFaceliftOtherChannelReports.class.getName(), "doPost()", e);
		}
	}

	private ScheduleBean getSessionBean(HttpServletRequest request) 
	{
		ScheduleBean sessionBean = null;
		if (null != request.getSession().getAttribute("scheduleBean") && 
				!"".equals(request.getSession().getAttribute("scheduleBean"))) 
		{
			sessionBean = (ScheduleBean) request.getSession().getAttribute("scheduleBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new ScheduleBean();
			request.getSession().setAttribute("scheduleBean", sessionBean);
		}
		return sessionBean;
	}
	
	@SuppressWarnings({ "unchecked", "resource" })
	private void printOtherChannelReports(ItemDetails itemDetails, HttpServletResponse response)
	{
		try
		{
			if(null!=itemDetails && null!=itemDetails.getImpactedDocumentsList() 
					&& itemDetails.getImpactedDocumentsList().size()>0)
			{
				/*
				 * Prepare File name
				 * ITEMCODE_LOCALE_CHANNEL_INITIALS_<MANUAL_TYPE_IF AVAILABLE>_MODEL.XLSX
				 */
				String channelInitials=itemDetails.getChannelRefKey();
				if(itemDetails.getChannelRefKey().equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE))
				{
					channelInitials="SI";
				}
				String fName = itemDetails.getLocale().trim()+"_"+channelInitials;
				if(null!=itemDetails.getDocumentTypeRefKey() && !"".equals(itemDetails.getDocumentTypeRefKey()))
				{
					// add manualType
					fName=fName+"_"+itemDetails.getDocumentTypeRefKey().trim();
				}
				if(null!=itemDetails.getCarlineInfo() && !"".equals(itemDetails.getCarlineInfo()))
				{
					// add Carline
					fName=fName+"_"+itemDetails.getCarlineInfo().trim();
				}
				fName = fName.replace("-", "_");
				fName = fName.trim().toUpperCase()+".xls";
				
				channelInitials = null;
				// Create the workbook instance for XLS file
				HSSFWorkbook myWorkBook = new HSSFWorkbook();

				// Create a new sheet
				HSSFSheet mySheet = myWorkBook.createSheet("Details");
				/*
				 * Add Header Row
				 */
				HSSFRow headerRow = mySheet.createRow(0);
				HSSFCell headerCell = null;
				
				String headers="LOCALE,CHANNEL,DOCUMENT_TYPE,MODEL,DOCUMENT_ID,OLD_VIN_REF_KEY,NEW_VIN_REF_KEY,"
						+ "OLD_CARLINE,OLD_WMI,OLD_VDS,OLD_VIS_START,OLD_VIS_END,NEW_CARLINE,NEW_WMI,NEW_VDS,NEW_VIS_START,NEW_VIS_END";
				String[] tokens=headers.split(",");
				if(null!=tokens && tokens.length>0)
				{
					for(int a=0;a<tokens.length;a++)
					{
						headerCell = headerRow.createCell(a);
						headerCell.setCellValue(tokens[a].replace("_", " "));
						headerCell  = null;
					}
				}
				tokens = null;
				headerCell=null;
				headerRow=null;
				headers=null;
				
				int rowCount = 0;
				/*
				 * GENERATE MULTIPLE ROWS SUCH THAT
				 * item data + Document Id + multiple Rows for Applicable VIN List
				 */
				String dataRow="";
				Map<String, Object> documentMap = null;
				ArrayList<ExcelRowDetails> appvinList = new ArrayList<ExcelRowDetails>();
				ExcelRowDetails vinDetails = new ExcelRowDetails();
				
				HSSFRow row=null;
				HSSFCell dataCell=null;
				
				for (int a = 0; a < itemDetails.getImpactedDocumentsList().size(); a++) {
					documentMap = (Map<String, Object>) itemDetails.getImpactedDocumentsList().get(a);

					if(null!=documentMap.get("VIN_LIST"))
					{
						appvinList = (ArrayList<ExcelRowDetails>)documentMap.get("VIN_LIST");
					}
					if(null!=appvinList && appvinList.size()>0)
					{
						vinDetails = new ExcelRowDetails();
						for(int b=0;b<appvinList.size();b++)
						{
							vinDetails = (ExcelRowDetails)appvinList.get(b);
							dataRow=itemDetails.getLocale()+"<TOK_SEPARATOR>";
							dataRow+=itemDetails.getChannelLabel()+"<TOK_SEPARATOR>"+itemDetails.getDocumentTypeLabel()+"<TOK_SEPARATOR>";
							dataRow+=itemDetails.getCarlineInfo()+"<TOK_SEPARATOR>"+documentMap.get("DOCUMENTID")+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldRefKey()+"<TOK_SEPARATOR>"+vinDetails.getNewRefKey()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldCarlineCode()+"<TOK_SEPARATOR>"+vinDetails.getOldWmiCode()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldVdsCode()+"<TOK_SEPARATOR>"+vinDetails.getOldVisStartRange()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getOldVisEndRange()+"<TOK_SEPARATOR>"+vinDetails.getNewCarlineCode()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getNewWmiCode()+"<TOK_SEPARATOR>"+vinDetails.getNewVdsCode()+"<TOK_SEPARATOR>";
							dataRow+=vinDetails.getNewVisStartRange()+"<TOK_SEPARATOR>"+vinDetails.getNewVisEndRange();
							
							// increment rowCount by 1
							rowCount++;
							// Create a new Row
							row = mySheet.createRow(rowCount);
							tokens = dataRow.split("<TOK_SEPARATOR>");
							if(null!=tokens && tokens.length>0)
							{
								for(int e=0;e<tokens.length;e++)
								{
									dataCell = row.createCell(e);
									dataCell.setCellValue("");
									if(null!=tokens[e] && !"".equals(tokens[e]) && !"null".equals(tokens[e].trim().toLowerCase()))
									{
										dataCell.setCellValue(tokens[e].trim());
									}
									dataCell =null;
								}
							}
							tokens = null;
							row=null;
							dataRow = null;
							dataCell = null;
							vinDetails = null;
						}
						vinDetails = null;
					}
					
					appvinList = null;
					documentMap = null;
				}

				headerRow = null;
				
				//response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
				response.setContentType("application/vnd.ms-excel");
				response.setHeader("Content-Disposition","attachment; filename="+fName+"");
				response.setCharacterEncoding("utf-8");
				myWorkBook.write(response.getOutputStream());
				response.getOutputStream().flush();
				response.getOutputStream().close();

				// set mySheet to null
				mySheet = null;
				// set myWorkBook to null
				myWorkBook = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(DownloadIPMFaceliftOtherChannelReports.class.getName(), "printOtherChannelReports()", e);
		}
	}

	
}
