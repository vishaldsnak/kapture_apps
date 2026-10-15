package com.mazda.gms3.mdm.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mazda.gms3.mdm.bean.EngineBookBean;
import com.mazda.gms3.mdm.bean.SchMaintenanceVINMappingBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.dao.SchMaintenanceVINMappingDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.EngineBookDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SchVINMappingDetails;
import com.mazda.gms3.sst.utils.SSTUtils;

/**
 * Servlet implementation class SchMaintenanceVINMapping
 */
public class SchMaintenanceVINMapping extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger logger = LogManager.getLogger(SchMaintenanceVINMapping.class);
	static String wslId=null;
	static MessageProperties msgProps= null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_SCH_MAIN_VIN_MAPPING;
	String reportName=null;
	
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SchMaintenanceVINMapping() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		boolean useReqDis = true;
		/*
		 * Initialize bean
		 */
		SchMaintenanceVINMappingBean sessionBean = getSessionBean(request);
		try
		{
			if(ApplicationProperties.getProperty("wsl.check").equals("TRUE"))
			{
				// Get WSL ID From request Header and set in Variable
				wslId = request.getHeader("iv-user");
				if (null == wslId || "".equals(wslId)) {
					/*
					 * re direct to Error
					 */
					response.sendRedirect(request.getContextPath() + "/error");
					useReqDis = false;
				}
				// EXPLICITY MAKE WSLID TO LOWERCASE
				if(null!=wslId && !"".equals(wslId))
				{
					wslId = wslId.trim().toLowerCase();
				}
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			/*
			 * Perform Access check
			 */
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setActionClicked(null);
			sessionBean.setSchVinMappingList(null);
			sessionBean.setSchVinMappingListToImport(null);
			sessionBean.setInfoMessage(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setReportViewPath(null);
			
			/*
			 * CALL FUNTION TO GET SCH VIN MAPPING LIST
			 */
			getSchVinMappingList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SchMaintenanceVINMapping.class.getName(), "doGet()", e);
		}
		if(useReqDis==true)
		{
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS NULL
			 * REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if(null==userSessionBean.getTopMenuList() || userSessionBean.getTopMenuList().size()<=0 || 
					(null!=userSessionBean.getTopMenuList() && userSessionBean.getTopMenuList().size()==1))
			{
				response.sendRedirect(request.getContextPath()+"/mypage");
			}
			else
			{
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/schMaintenaceVinMapping.jsp");
				rs.forward(request, response);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		boolean useReqDis = true;
		/*
		 * Initialize bean
		 */
		SchMaintenanceVINMappingBean sessionBean = getSessionBean(request);
		try
		{
			if(ApplicationProperties.getProperty("wsl.check").equals("TRUE"))
			{
				// Get WSL ID From request Header and set in Variable
				wslId = request.getHeader("iv-user");
				if (null == wslId || "".equals(wslId)) {
					/*
					 * re direct to Error
					 */
					response.sendRedirect(request.getContextPath() + "/error");
					useReqDis = false;
				}
				// EXPLICITY MAKE WSLID TO LOWERCASE
				if(null!=wslId && !"".equals(wslId))
				{
					wslId = wslId.trim().toLowerCase();
				}
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			/*
			 * Perform Access Check
			 */
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setErrorMessage("");
			sessionBean.setSuccessMessage("");
			sessionBean.setInfoMessage(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setReportViewPath(null);
			
			/*
			 * call function to read parameters from request
			 */
			readParamsFromRequest(sessionBean, request);

			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("EXPORT"))
				{
					/*
					 * Export Operation called
					 */
					exportSchVINMappingDetails(request, sessionBean);
				}
			}
			
			// set actionClicked to null;
			sessionBean.setActionClicked(null);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SchMaintenanceVINMapping.class.getName(), "doPost()", e);
		}
		if(useReqDis==true)
		{
			/*
			 * ALSO CHECK LAST TIME - IF TOP MENU LIST IN USER SESSION BEAN IS NULL
			 * REDIRECT TO MY PAGE
			 */
			UserAccessBean userSessionBean = getUserSessionBean(request);
			if(null==userSessionBean.getTopMenuList() || userSessionBean.getTopMenuList().size()<=0 || 
					(null!=userSessionBean.getTopMenuList() && userSessionBean.getTopMenuList().size()==1))
			{
				response.sendRedirect(request.getContextPath()+"/mypage");
			}
			else
			{
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/schMaintenaceVinMapping.jsp");
				rs.forward(request, response);
			}
		}
	}
	
	private SchMaintenanceVINMappingBean getSessionBean(HttpServletRequest request) 
	{
		SchMaintenanceVINMappingBean sessionBean = null;
		if (null != request.getSession().getAttribute("schMaintenanceVINMappingBean") && !"".equals(request.getSession().getAttribute("schMaintenanceVINMappingBean"))) 
		{
			sessionBean = (SchMaintenanceVINMappingBean) request.getSession().getAttribute("schMaintenanceVINMappingBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new SchMaintenanceVINMappingBean();
			request.getSession().setAttribute("schMaintenanceVINMappingBean", sessionBean);
		}
		return sessionBean;
	}

	private UserAccessBean getUserSessionBean(HttpServletRequest request) 
	{
		UserAccessBean sessionBean = null;
		if (null != request.getSession().getAttribute("userAccessBean") && !"".equals(request.getSession().getAttribute("userAccessBean"))) 
		{
			sessionBean = (UserAccessBean) request.getSession().getAttribute("userAccessBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new UserAccessBean();
			request.getSession().setAttribute("userAccessBean", sessionBean);
		}
		return sessionBean;
	}
	
	private void performAccessCheck(SchMaintenanceVINMappingBean sessionBean, HttpServletRequest request)
	{
		/*
		 * Check User Has access to this Functionality or Not.
		 * Identify if User is Super Admin - then enable Read / Write Access on this page
		 * else - check if user has access to this functionality
		 * 		check for the accessType
		 * 			if READ ACCESS - SHOW READ CONTROLS
		 * 			if WRITE ACCESS - SHOW WRITE CONTROLS
		 * 			if READ & WRITE ACCESS - SHOW READ & WRITE CONTROLS
		 */
		try
		{
			UserAccessBean userSessionBean = getUserSessionBean(request);
			sessionBean.setShowReadControls(false);
			sessionBean.setShowWriteControls(false);
			if(userSessionBean.isSuperAdminUser()==true)
			{
				sessionBean.setShowReadControls(true);
				sessionBean.setShowWriteControls(true);
			}
			else if(userSessionBean.isSuperAdminUser()==false)
			{
				if(null!=userSessionBean.getUserAllModulesList() && userSessionBean.getUserAllModulesList().size()>0)
				{
					for(int a=0;a<userSessionBean.getUserAllModulesList().size();a++)
					{
						ModuleDetails modDetails = (ModuleDetails)userSessionBean.getUserAllModulesList().get(a);
						if(modDetails.getModuleRefkey().trim().toLowerCase().equals(moduleRefKey.trim().toLowerCase()))
						{
							// USER HAS ACCESS TO THIS SCREEN. CHECK FOR ACCESS TYPE
							if(modDetails.getAccessType()==AccessManagementInterface.ONLY_READ_ACCESS)
							{
								sessionBean.setShowReadControls(true);
							}
							else if(modDetails.getAccessType()==AccessManagementInterface.ONLY_WRITE_ACCESS)
							{
								sessionBean.setShowWriteControls(true);
							}
							else if(modDetails.getAccessType()==AccessManagementInterface.READ_AND_WRITE_ACCESS)
							{
								sessionBean.setShowReadControls(true);
								sessionBean.setShowWriteControls(true);
							}
							else
							{
								sessionBean.setShowReadControls(false);
								sessionBean.setShowWriteControls(false);
							}
							break;
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SchMaintenanceVINMapping.class.getName(), "performAccessCheck()", e);
		}
	}
	
	
	private static void getSchVinMappingList(SchMaintenanceVINMappingBean sessionBean)
	{
		try
		{
			sessionBean.setSchVinMappingList(new ArrayList<SchVINMappingDetails>());
			ArrayList<SchVINMappingDetails> list = new ArrayList<SchVINMappingDetails>();
			/*
			 * IDENTIFY & FETCH ON LOCALE CODE
			 */
			list = SchMaintenanceVINMappingDAO.getSchVinMappingDetails();
			if(null!=list && list.size()>0)
			{
				sessionBean.setSchVinMappingList(list);
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SchMaintenanceVINMapping.class.getName(), "getSchVinMappingList()", e);
		}
	}
	
	private void readParamsFromRequest(SchMaintenanceVINMappingBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setSchVinMappingListToImport(null);
			
			// Create FileItemFactory instance
			DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
			// By using fileItemFactory instance get the ServletFileUpload
			// object
			// as
			ServletFileUpload servletFileUpload = new ServletFileUpload(fileItemFactory);
			// Now get the list of all files by parsing the request
			List<FileItem> fileItems = servletFileUpload.parseRequest(request);
			// iterate fileItems and check for the Image File
			Iterator<FileItem> iterator = fileItems.iterator();
//			logger.info("readParamsFromRequest() :: iterating File Items");
			while (iterator.hasNext()) 
			{
				FileItem fileItem = iterator.next();
				if (fileItem.isFormField())
				{
//					logger.info("readParamsFromRequest() :: When Fields are Not Form Fields. Check each File Name and set the values accordingly in each attribute.");
					String fieldName = fileItem.getFieldName();
					if (null != fieldName && !"".equals(fieldName)) 
					{
						/*
						 * set displayPageNo and displayPageLenght
						 */
						if (fieldName.equals("SCHVIN_DataTabel_displayPageNo")) 
						{
							// set the value in sessionBean.displayPageNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDisplayPageNo(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SCHVIN_DataTabel_displayPageLen")) 
						{
							// set the value in sessionBean.displayPageLenght
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDisplayPageLength(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("SCHVIN_ActionClicked")) 
						{
							// set the value in sessionBean.actionClicked
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setActionClicked(value);
							}
							// set value to null
							value = null;
						}
					}
				}
				else if (!fileItem.isFormField()) 
				{
					if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked())
							&& sessionBean.getActionClicked().equals("FILE_UPLOAD"))
					{
						if(validateFileUpload(sessionBean))
						{
							String fileName = fileItem.getName();
							byte[] data = fileItem.get();
							if(null!=fileName && !"".equals(fileName) && null!=data)
							{
								/*
								 * check for CSV
								 */
								String extension="";
								if(fileName.lastIndexOf(".")!=-1)
								{
									extension = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
									if(null!=extension && !"".equals(extension))
									{
										if(extension.trim().toLowerCase().equals("xlsx") || extension.trim().toLowerCase().equals("xls"))
										{
											/*
											 * call function to operate on uploaded excel
											 */
											executeExcelOperation(sessionBean, data, extension);
										}
										else
										{
											logger.info("readParamsFromRequest() :: Extension is not EXCEL. Throw Message - Uploaded file not supported."); 
											sessionBean.setErrorMessage(msgProps.getProperty("error.valid.excel"));
										}
									}
									else
									{
										logger.info("readParamsFromRequest() :: File Name does not contain valid extension. Throw message - Please upload a valid Excel File.");
										sessionBean.setErrorMessage(msgProps.getProperty("error.valid.excel"));
									}
								}
								else
								{
									logger.info("readParamsFromRequest() :: File Name does not contain any extension. Throw message - Please upload a valid Excel File.");
									sessionBean.setErrorMessage(msgProps.getProperty("error.valid.excel"));
								}
								extension = null;
							}	
							else
							{
								logger.info("readParamsFromRequest() :: File Name / Size is null. Throw message - Please upload a valid Excel File.");
								sessionBean.setErrorMessage(msgProps.getProperty("error.valid.excel"));
							}
							fileName=  null;
							data = null;
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SchMaintenanceVINMapping.class.getName(), "readParamsFromRequest(", e);
		}
	}
	
	private boolean validateFileUpload(SchMaintenanceVINMappingBean sessionBean)
	{
		
//		StringBuilder errorMessage = new StringBuilder();
//		
//		
//		String messages=errorMessage.toString();
//		if(null!=messages && !"".equals(messages))
//		{
//			sessionBean.setErrorMessage(messages);
//			messages=  null;
//			errorMessage= null;
//			return false;
//		}
//		messages= null;
//		errorMessage = null;
		return true;
	}


	private void executeExcelOperation(SchMaintenanceVINMappingBean sessionBean, byte[] data, String extension)
	{
		try
		{
			/*
			 * proceed for uploading and parsing.
			 */
			readExcelData(data, sessionBean, extension);
			
			/*
			 * call function to validate ALL THE EXCEL ROWS
			 */
			if(validateExcelRowData(sessionBean))
			{
				StringBuilder errorMessage = new StringBuilder();
				int errorCount=0;
				String duplicateRowNo="";
				int duplicateRowsCount=0;
				int failureCount=0;
				List<SchVINMappingDetails> listToSave = new ArrayList<SchVINMappingDetails>();
				SchVINMappingDetails fieldDetails = new SchVINMappingDetails();
				SchVINMappingDetails existingDetails = new SchVINMappingDetails();
				for(int i=0;i<sessionBean.getSchVinMappingListToImport().size();i++)
				{
					fieldDetails = new SchVINMappingDetails();
					fieldDetails = (SchVINMappingDetails)sessionBean.getSchVinMappingListToImport().get(i);
					fieldDetails.setSrNo((i+1+1));
					boolean addToList = true;
					if(null!=listToSave && listToSave.size()>0)
					{
						for(int j=0;j<listToSave.size();j++)
						{
							existingDetails = new SchVINMappingDetails();
							existingDetails = (SchVINMappingDetails)listToSave.get(j);
							/*
							 * IDENTIY DUPLICATE ON THE BASIS OF 
							 *  ABBREVIATION CODE
							 */
							if(fieldDetails.getLocale().equals(existingDetails.getLocale()) && 
									fieldDetails.getCarlineCode().trim().toLowerCase().equals(existingDetails.getCarlineCode().trim().toLowerCase()) && 
									fieldDetails.getWmiCode().trim().toLowerCase().equals(existingDetails.getWmiCode().trim().toLowerCase()) && 
									fieldDetails.getVdsCode().trim().toLowerCase().equals(existingDetails.getVdsCode().trim().toLowerCase()) && 
									fieldDetails.getVisStartRange().trim().toLowerCase().equals(existingDetails.getVisStartRange().trim().toLowerCase()) && 
									fieldDetails.getVisEndRange().trim().toLowerCase().equals(existingDetails.getVisEndRange().trim().toLowerCase()) && 
									fieldDetails.getSourceFilePath().trim().toLowerCase().equals(existingDetails.getSourceFilePath().trim().toLowerCase()))
							{
								if(null!=duplicateRowNo && !"".equals(duplicateRowNo))
								{
									duplicateRowNo = duplicateRowNo+",";
								}
								duplicateRowNo = duplicateRowNo+String.valueOf(fieldDetails.getSrNo());
								// already Added = SKIP IT
								addToList = false;
								break;
							}
							existingDetails = null;
						}
					}
					if(addToList==true)
					{
						listToSave.add(fieldDetails);
					}
					fieldDetails = null;
				}
				
				if(null!=duplicateRowNo && !"".equals(duplicateRowNo))
				{
					if(duplicateRowNo.endsWith(","))
					{
						duplicateRowNo = duplicateRowNo.substring(0, duplicateRowNo.length()-1);
					}
					String[] rows = duplicateRowNo.split(",");
					if(null!=rows && rows.length>0)
					{
						duplicateRowsCount = rows.length;
						for(int a=0;a<rows.length;a++)
						{
							// increment errorCount by 1
							errorCount++;
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.sch.maintenance.vin.mapping"), String.valueOf(rows[a])));
						}
					}
					rows = null;
				}
				duplicateRowNo = null;
				
				
				if(null!=listToSave && listToSave.size()>0)
				{
					/*
					 * ITERATE AND START SAVING EACH ROW
					 */
					String successLineNo="";
					String errorLineNo="";
					int successCount=0;
					
					// Update countryId, localeId & Flag Status
					fieldDetails = new SchVINMappingDetails();
					/*
					 * Call Import Function
					 */
					listToSave = SchMaintenanceVINMappingDAO.importSchVinMappingDetails(listToSave);
					fieldDetails = new SchVINMappingDetails();
					for(int i=0;i<listToSave.size();i++)
					{
						fieldDetails = (SchVINMappingDetails)listToSave.get(i);
						if(fieldDetails.isSaveStatusWhileImport() == true)
						{
							logger.info("readParamsFromRequest() :: SCH Maintenance VIN Mapping Data for Line No {"+(i+1+1)+"}. Saved Successfully.");
							if(null!=successLineNo && !"".equals(successLineNo))
							{
								successLineNo = successLineNo+",";
							}
							successLineNo = successLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						else
						{
							logger.info("readParamsFromRequest() :: Failed to Import SCH Maintenance VIN Mapping Data for Line No {"+(i+1+1)+"}.");
							if(null!=errorLineNo && !"".equals(errorLineNo))
							{
								errorLineNo = errorLineNo+",";
							}
							if(null!=fieldDetails.getModelNotFoundMessage() && !"".equals(fieldDetails.getModelNotFoundMessage()) && 
									null!=fieldDetails.getDocumentNotFoundMessage() && !"".equals(fieldDetails.getDocumentNotFoundMessage()))
							{
								errorLineNo = errorLineNo+String.valueOf(fieldDetails.getSrNo()+" (Model Name not found for VIN in VIN Master & No Document found for Processing VIN)");
							}
							else if(null!=fieldDetails.getModelNotFoundMessage() && !"".equals(fieldDetails.getModelNotFoundMessage()))
							{
								errorLineNo = errorLineNo+String.valueOf(fieldDetails.getSrNo()+" (Model Name not found for VIN in VIN Master)");
							}
							else if(null!=fieldDetails.getDocumentNotFoundMessage() && !"".equals(fieldDetails.getDocumentNotFoundMessage()))
							{
								errorLineNo = errorLineNo+String.valueOf(fieldDetails.getSrNo()+" (No Document found for Processing VIN)");
							}
							else
							{
								errorLineNo = errorLineNo+String.valueOf(fieldDetails.getSrNo());
							}
						}
						fieldDetails= null;
					}

					if(null!=successLineNo && !"".equals(successLineNo))
					{
						if(successLineNo.endsWith(","))
						{
							successLineNo = successLineNo.substring(0, successLineNo.length()-1);
						}
						String[] successRows = successLineNo.split(",");
						if(null!=successRows && successRows.length>0)
						{
							successCount = successRows.length;
						}
						if(successCount>0)
						{
							sessionBean.setSuccessMessage(msgProps.addMessage("import.success.count.message", String.valueOf(successCount)));
						}
						successRows= null;
//						successLineNo = "( "+successLineNo+" )";
//						sessionBean.setSuccessMessage(msgProps.addMessage("import.success",msgProps.getProperty("label.displayorder"), successLineNo ));
						sessionBean.setSelectedRows(null);
						sessionBean.setShowUpdate(false);
						/*
						 * call function to load updated sch vin mapping list
						 */
						getSchVinMappingList(sessionBean);
					}
					
					if(null!=errorLineNo && !"".equals(errorLineNo))
					{
						if(errorLineNo.endsWith(","))
						{
							errorLineNo= errorLineNo.substring(0, errorLineNo.length()-1);
						}
						String[] errorRows = errorLineNo.split(",");
						if(null!=errorRows && errorRows.length>0)
						{
							failureCount = errorRows.length;
							for(int a=0;a<errorRows.length;a++)
							{
								// increment errorCount by 1
								errorCount++;
								if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
								{
									errorMessage.append("<MSG_TOKEN>");
								}
								errorMessage.append(msgProps.addMessage("error.import", msgProps.getProperty("label.sch.maintenance.vin.mapping"), String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
//						errorLineNo= "( "+errorLineNo+ " )";
//						sessionBean.setErrorMessage(msgProps.addMessage("error.import", msgProps.getProperty("label.sch.maintenance.vin.mapping"), errorLineNo));
					}
					
					successLineNo = null;
					errorLineNo = null;		
				}
				else
				{
					/*
					 * NO DATA FOUND TO IMPORT
					 */
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					// increment erroCount by 1
					errorCount++;
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.sch.maintenance.vin.mapping")));
				}
				listToSave = null;
				
				if(errorCount>0 )
				{
					decideErrorDisplay(sessionBean, errorMessage, errorCount);
				}
				
				
				/*
				 * also check if duplicateCount or failureCount is more than 0
				 * then set infoMessage
				 */
				String mess="";

				if(duplicateRowsCount>0)
				{
					mess = msgProps.addMessage("error.excel.duplicate.rows.count", String.valueOf(duplicateRowsCount));
				}
				
				if(failureCount>0)
				{
					if(null!=mess && !"".equals(mess))
					{
						mess = mess+" ";
					}
					mess = mess+msgProps.addMessage("error.excel.failure.rows.count", String.valueOf(failureCount));
				}
				
				if(null!=mess && !"".equals(mess))
				{
					sessionBean.setInfoMessage(mess);
				}
				mess = null;
			
				errorMessage = null;
				fieldDetails = null;
				existingDetails = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SchMaintenanceVINMapping.class.getName(), "executeExcelOperation()", e);
		}
	}

	private void readExcelData(byte[] data, SchMaintenanceVINMappingBean sessionBean,String extension)
	{
		sessionBean.setSchVinMappingListToImport(new ArrayList<SchVINMappingDetails>());
		try
		{
			if(null!=data)
			{
				InputStream is = new ByteArrayInputStream(data);
				XSSFWorkbook workbook  = null;
				XSSFSheet sheet = null;
				HSSFWorkbook xlsWorkBook = null;
				HSSFSheet xlsSheet = null;
				Iterator<Row> rowIterator = null;
				
				if(null!=extension && extension.equals("xlsx"))
				{
					//Create Workbook instance holding reference to .xlsx file
					workbook = new XSSFWorkbook(is);
					//Get first/desired sheet from the workbook
					sheet = workbook.getSheetAt(0);
					//Iterate through each rows one by one
					rowIterator = sheet.iterator();
				}
				else if(null!=extension && extension.equals("xls"))
				{
					// Create workbook instance holding reference to .xls file
					xlsWorkBook = new HSSFWorkbook(is);
					//Get first/desired sheet from the workbook
					xlsSheet=  xlsWorkBook.getSheetAt(0);
					//Iterate through each rows one by one
					rowIterator = xlsSheet.iterator();
				}
				long rowCount=0;
				/*
				 * ROW COLUMNS HAS TO BE IN FOLLOWING SEQUENCE
				 * LOCALE
				 * CARLINE CODE
				 * WMI CODE
				 * VDS CODE
				 * VIS START RANGE
				 * VIS END RANGE
				 */
				SchVINMappingDetails details = new SchVINMappingDetails();
				while(null!=rowIterator && rowIterator.hasNext())
				{
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						details=  new SchVINMappingDetails();
						Object dataCell = SSTUtils.readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setLocale(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setCarlineCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setWmiCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(3));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setVdsCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(4));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setVisStartRange(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(5));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setVisEndRange(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(6));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setSourceFilePath(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						/*
						 * add details to schVinList for import
						 */
						if(null==sessionBean.getSchVinMappingListToImport() || sessionBean.getSchVinMappingListToImport().size()<=0)
						{
							sessionBean.setSchVinMappingListToImport(new ArrayList<SchVINMappingDetails>());
						}

						sessionBean.getSchVinMappingListToImport().add(details);
						details= null;
					}
					// INCREMENT ROW COUNT BY 1
					rowCount++;
					row = null;
				}
				sheet = null;
				workbook = null;
				xlsWorkBook=  null;
				xlsSheet = null;
				rowIterator  = null;
				is.close();
				is = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SchMaintenanceVINMapping.class.getName(), "readExcelData()", e);
		}
	}

	private boolean validateExcelRowData(SchMaintenanceVINMappingBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		if(null!=sessionBean.getSchVinMappingListToImport() && sessionBean.getSchVinMappingListToImport().size()>0)
		{
			SchVINMappingDetails fieldDetails = new SchVINMappingDetails();
			for(int i=0;i<sessionBean.getSchVinMappingListToImport().size();i++)
			{
				fieldDetails = (SchVINMappingDetails) sessionBean.getSchVinMappingListToImport().get(i);
				// EXTRA 1 BECAUSE WHILE READING EXCEL, HEADER ROW WAS SKIPPED
				int rowNo = i+1+1;
				
				if(null==fieldDetails.getLocale() || "".equals(fieldDetails.getLocale()) || 
						null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) ||
						null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) || 
						null==fieldDetails.getVdsCode() || "".equals(fieldDetails.getVdsCode()) || 
						null==fieldDetails.getVisStartRange() || "".equals(fieldDetails.getVisStartRange()) || 
						null==fieldDetails.getVisEndRange() || "".equals(fieldDetails.getVisEndRange()) || 
						null==fieldDetails.getSourceFilePath() || "".equals(fieldDetails.getSourceFilePath()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					/*
					 * INCOMPLETE DATA FOR SCHVIN MAPPING AT ROW NO . rowNo
					 */
					String data = msgProps.getProperty("label.sch.maintenance.vin.mapping")+","+String.valueOf(rowNo);
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
					data = null;
					id = null;
					// increment errorCount by 1
					errorCount++;
				}
				fieldDetails = null;
			}
			fieldDetails = null;
		}
		else
		{
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.sch.maintenance.vin.mapping")));
			// increment errorCount by 1
			errorCount++;
		}
		/*
		 * HERE Check, if the Error Count is more than 10, then do not show error Messages on the screen.
		 * Instead, show a message, Multiple errors found while performing the transaction. Please Click <a>here</a> to view the details.
		 */
		if(errorCount>0)
		{
			decideErrorDisplay(sessionBean, errorMessage, errorCount);
			return false;
		}
		errorMessage = null;
		return true;
	}

	private void decideErrorDisplay(SchMaintenanceVINMappingBean sessionBean, StringBuilder errorMessage, int errorCount)
	{
		/*
		 * HERE Check, if the Error Count is more than 10, then do not show error Messages on the screen.
		 * Instead, show a message, Multiple errors found while performing the transaction. Please Click <a>here</a> to view the details.
		 */
		if(errorCount>10)
		{
			long currentTime = new Timestamp(new Date().getTime()).getTime();
			// WRITE ALL THE ERROR MESAGES TO A TEXT FILE  , NAME IT ON THE BASIS OF VIN_TIMESTAMP.TXT
			String eFPath = ApplicationProperties.getProperty("EXPORT_ERROR_PHYSICAL_PATH");
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_SCH_MAIN_VIN_MAPPING_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
			File errorFile = new File(eFPath+eFName);
			try {
				String data = errorMessage.toString();
				data = data.replace("<MSG_TOKEN>", "\n");
				
				FileOutputStream fos = new FileOutputStream(errorFile);
				fos.write(data.getBytes());
				fos.flush();
				fos.close();
				fos = null;
				data = null;
			} catch (FileNotFoundException e) {
				Utilities.printStackTraceToLogs(SchMaintenanceVINMapping.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				Utilities.printStackTraceToLogs(SchMaintenanceVINMapping.class.getName(), "validateExcelRowData()", e);
			}
			errorFile=null;
			String webPath = ApplicationProperties.getProperty("EXPORT_ERROR_WB_PATH")+eFName;
			
			// label.error.screen.help.text.start
			// label.here
			// label.view.the.details
			String message=msgProps.addMessage("error.import.invalid.custom.message", String.valueOf(errorCount));
			message = message + " "+msgProps.getProperty("label.error.screen.help.text.start");
			message = message+ " <a href=\""+webPath+"\" target=\"_blank\">" +msgProps.getProperty("label.here")+"</a>";
			message = message +" " +msgProps.getProperty("label.view.the.details");
			
			// set message in errorMessage
			sessionBean.setErrorMessage(message);
			message = null;
			eFPath = null;
			eFName=  null;
			webPath = null;
		}
		else
		{
			String messages=errorMessage.toString();
			if(null!=messages && !"".equals(messages))
			{
				sessionBean.setErrorMessage(messages);
				messages=  null;
				errorMessage= null;
			}
		}
	}
	
	private  void exportSchVINMappingDetails(HttpServletRequest request, SchMaintenanceVINMappingBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{

			if(null!=sessionBean.getSchVinMappingList() && !"".equals(sessionBean.getSchVinMappingList().size()>0))
			{
				/*
				 * CALL FUNCTION TO GENERATE EXCEL FOR THE SELECTED ROWS
				 */
				writeEngineBookExcel(sessionBean.getSchVinMappingList());
				/*
				 * PREARE VIN REORT PATH AND MAKE IT DOWNLOAD
				 */
				if(null!=reportName && !"".equals(reportName))
				{
					// set in SESSION BEAN
					String path = ApplicationProperties.getProperty("EXPORT_DATA_WB_PATH");
					if(!path.endsWith("/"))
					{
						path = path+"/";
					}
					path = path+reportName;
					sessionBean.setReportViewPath(path);
					path = null;
				}
				reportName = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SchMaintenanceVINMapping.class.getName(), "exportdetails()", e);
		}
	}

	private void writeEngineBookExcel(List<SchVINMappingDetails> schVinMappingList)
	{
		try
		{
			String path = ApplicationProperties.getProperty("EXPORT_DATA_PHYSICAL_PATH");
			if(null!=path && !"".equals(path))
			{
				if(!path.endsWith("/") && !path.endsWith("\\"))
				{
					path = path+"/";
				}
				// add VIN DATA NAME
				String name = "";
				/*
				 * add SELECTED COUNTRY LOCALE CODE, ADD MANUAL LANGUAGE CODE, ADD MODEL CODE
				 * add Current Time Stamp in Format - DDMMYYYY HHMMSS
				 * 
				 * So the final Name will be - US_EN-US_ND_VIN_DDMMYYYY_HHMMSS.XSLX
				 */
				name = ApplicationProperties.getProperty("EXPORT_DATA_SCH_MAIN_VIN_MAPPING_NAME");
				SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
				String displayValue = sdf.format(new Date());
				// ADD ZIP NAME.
				String zipName = name.trim()+"_"+displayValue+ApplicationProperties.getProperty("EXPORT_DATA_EXTENSION_ZIP");
				// ADD ZIP PATH
				String zipPath = path+zipName;
				name = name.trim()+"_"+displayValue+ApplicationProperties.getProperty("EXPORT_DATA_EXTENSION");
				path= path+name;
				
				// SET REPORT NAME TO ZIP FILE NAME INSTEAD OF EXCEL FILE
				reportName = zipName;
				name = null;
				zipName = null;
				File excelFile = new File(path);
				// Create the workbook instance for XLSX file, KEEP 100 ROWS IN MEMMORY AND RET ON DISK
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);
				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("EXPORTED DATA");
				Row headerRow = mySheet.createRow(0);
				
				Cell headerCell  =null;
				headerCell = headerRow.createCell(0);
				headerCell.setCellValue("LOCALE");
				headerCell = null;
				headerCell = headerRow.createCell(1);
				headerCell.setCellValue("MODEL");
				headerCell = null;
				headerCell = headerRow.createCell(2);
				headerCell.setCellValue("CARLINE CODE");
				headerCell = null;
				headerCell = headerRow.createCell(3);
				headerCell.setCellValue("WMI CODE");
				headerCell = null;
				headerCell = headerRow.createCell(4);
				headerCell.setCellValue("VDS CODE");
				headerCell = null;
				headerCell = headerRow.createCell(5);
				headerCell.setCellValue("VIS START RANGE");
				headerCell = null;
				headerCell = headerRow.createCell(6);
				headerCell.setCellValue("VIS END RANGE");
				headerCell = null;
				headerCell = headerRow.createCell(7);
				headerCell.setCellValue("DOCUMENT ID");
				headerCell = null;
				headerCell = headerRow.createCell(8);
				headerCell.setCellValue("DOCUMENT TITLE");
				headerCell = null;
				headerCell = headerRow.createCell(9);
				headerCell.setCellValue("SOURCE FILE NAME");
				headerCell = null;
				headerCell = headerRow.createCell(10);
				headerCell.setCellValue("SOURCE FILE PATH");
				headerCell = null;
				
				int rowCount=0;
				if(null!=schVinMappingList && schVinMappingList.size()>0)
				{
					SchVINMappingDetails details = null;
					for(int i=0;i<schVinMappingList.size();i++)
					{
						details = (SchVINMappingDetails)schVinMappingList.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);
						
						Cell dataCell = row.createCell(0);
						dataCell.setCellValue("");
						if(null!=details.getLocale() && !"".equals(details.getLocale()))
						{
							dataCell.setCellValue(details.getLocale().trim());
						}
						dataCell  = null;
						dataCell = row.createCell(1);
						dataCell.setCellValue("");
						if(null!=details.getModelName() && !"".equals(details.getModelName()))
						{
							dataCell.setCellValue(details.getModelName().trim());
						}
						dataCell = null;
						dataCell = row.createCell(2);
						dataCell.setCellValue("");
						if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
						{
							dataCell.setCellValue(details.getCarlineCode().trim());
						}
						dataCell = null;
						dataCell = row.createCell(3);
						dataCell.setCellValue("");
						if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
						{
							dataCell.setCellValue(details.getWmiCode().trim());
						}
						dataCell = null;
						dataCell = row.createCell(4);
						dataCell.setCellValue("");
						if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()))
						{
							dataCell.setCellValue(details.getVdsCode().trim());
						}
						dataCell = null;
						dataCell = row.createCell(5);
						dataCell.setCellValue("");
						if(null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()))
						{
							dataCell.setCellValue(details.getVisStartRange().trim());
						}
						dataCell = null;
						dataCell = row.createCell(6);
						dataCell.setCellValue("");
						if(null!=details.getVisEndRange() && !"".equals(details.getVisEndRange()))
						{
							dataCell.setCellValue(details.getVisEndRange().trim());
						}
						dataCell = null;
						dataCell = row.createCell(7);
						dataCell.setCellValue("");
						if(null!=details.getDocumentId() && !"".equals(details.getDocumentId()))
						{
							dataCell.setCellValue(details.getDocumentId().trim());
						}
						dataCell = null;
						dataCell = row.createCell(8);
						dataCell.setCellValue("");
						if(null!=details.getTitle() && !"".equals(details.getTitle()))
						{
							dataCell.setCellValue(details.getTitle().trim());
						}
						dataCell = null;
						dataCell = row.createCell(9);
						dataCell.setCellValue("");
						if(null!=details.getSourceFileName() && !"".equals(details.getSourceFileName()))
						{
							dataCell.setCellValue(details.getSourceFileName().trim());
						}
						dataCell = null;
						dataCell = row.createCell(10);
						dataCell.setCellValue("");
						if(null!=details.getSourceFilePath() && !"".equals(details.getSourceFilePath()))
						{
							dataCell.setCellValue(details.getSourceFilePath().trim());
						}
						dataCell = null;
						row = null;
						details = null;
					}
					
					headerRow =  null;
					headerCell = null;
					/*
					 * Before Writing check for size if equals to or more than 10 MB
					 * then generate a file with a extension to it.
					 */

					FileOutputStream os = new FileOutputStream(excelFile);
					myWorkBook.write(os);
					os.flush();
					os.close();

					// set mySheet to null
					mySheet = null;
					// set myWorkBook to null
					myWorkBook = null;
					// set path to null
					path = null;
					// set sdf to null
					sdf = null;
					/*
					 * CONVERT THIS FILE TO ZIP FILE.
					 */
					Utilities.createReportsZip(zipPath, excelFile);
					
					// set excelFile to null
					excelFile = null;
					// set zipPath to null
					zipPath=  null;
				}
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(SchMaintenanceVINMapping.class.getName(), "writeEngineBookExcel()", e);
		}
	}
	
}