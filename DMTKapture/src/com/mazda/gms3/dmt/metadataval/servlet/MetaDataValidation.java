package com.mazda.gms3.dmt.metadataval.servlet;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.mazda.gms3.dmt.bean.UserAccessBean;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.metadataval.bean.MetaDataValidationBean;
import com.mazda.gms3.dmt.metadataval.dao.MetaDataValidationDAO;
import com.mazda.gms3.dmt.metadataval.impl.MetaDataValidationScheduleImpl;
import com.mazda.gms3.dmt.metadataval.utils.MetaDataScheduleConstants;
import com.mazda.gms3.dmt.metadataval.vo.MetaDataFileDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.FileReadWriteUtil;
import com.mazda.gms3.dmt.utils.MessageProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.SelectItemDetails;


/**
 * Servlet implementation class MetaDataValidation
 */
public class MetaDataValidation extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = LogManager.getLogger(MetaDataValidation.class);
	private MessageProperties msgProps= null;
	
	private String wslId=""; 
	private MetaDataValidationDAO metaDataValidationDAO=null;
	private String mnaoLocales=null;
	private String mcLocales=null;
	private String mmeLocales=null;
	
	private String deleteScheduleIds=null;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MetaDataValidation() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		boolean useReqDis = true;
		try
		{
			metaDataValidationDAO = new MetaDataValidationDAO();
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
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("DMT_LS_Locale"));
			
			/*
			 * CHECK WHETHER USER HAS ACCESS TO THE APPLICATION OR NOT
			 */
			UserAccessBean userBean = getUserSessionBean(request);
			if(null!=userBean && userBean.isShowNoAccess()==true)
			{
				/*
				 * NAVIGATE USER TO NO ACCESS PAGE, USER DOES NOT HAVE ACCESS TO DMT 
				 */
				response.sendRedirect(request.getContextPath()+"/noaccess");
				useReqDis = false;
			}
			
			MetaDataValidationBean sessionBean = getSessionBean(request);
			
			sessionBean.setCountryList(null);
			sessionBean.setLanguageList(null);
			sessionBean.setCountryId(null);
			sessionBean.setLanguageCode(null);
			sessionBean.setLanguageId(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setActionClicked(null);
			sessionBean.setMetaDataType(null);
			sessionBean.setMetaDataTypeList(null);
			sessionBean.setItemsList(null);
			sessionBean.setItemToBeDeleted(null);
			sessionBean.setScheduleList(null);
			sessionBean.setScheduleDisplayPageLength(null);
			sessionBean.setScheduleDisplayPageNo(null);
			sessionBean.setReloadJSP(false);
			
			/*
			 * getMarketSpecific Locales
			 */
			getMarketSpecificLocales();
			/*
			 * call function to get countryList
			 */
			getCountryList(sessionBean);
			
			/*
			 * call function to get scheduleList
			 */
			getScheduleList(sessionBean);
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "doGet()", e);
		}
		finally
		{
			metaDataValidationDAO = null;
		}
		
		if(useReqDis==true)
		{
			RequestDispatcher rs = request.getRequestDispatcher("/jsps/metadatavalidation.jsp");
			rs.forward(request, response);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		boolean useReqDis = true;
		try
		{
			metaDataValidationDAO = new MetaDataValidationDAO();
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
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("DMT_LS_Locale"));
			
			/*
			 * CHECK WHETHER USER HAS ACCESS TO THE APPLICATION OR NOT
			 */
			UserAccessBean userBean = getUserSessionBean(request);
			if(null!=userBean && userBean.isShowNoAccess()==true)
			{
				/*
				 * NAVIGATE USER TO NO ACCESS PAGE, USER DOES NOT HAVE ACCESS TO DMT 
				 */
				response.sendRedirect(request.getContextPath()+"/noaccess");
				useReqDis = false;
			}
			
			MetaDataValidationBean sessionBean = getSessionBean(request);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			
			
			/*
			 * call function to readParametersFromRequest
			 */
			readParamsFromRequest(sessionBean, request);
			
			/*
			 * getMarketSpecific Locales
			 */
			getMarketSpecificLocales();
			
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("COUNTRY_SELECTION"))
				{
					sessionBean.setLanguageCode(null);
					sessionBean.setLanguageId(null);
					sessionBean.setMetaDataType(null);
					/*
					 * call getLanguageList
					 */
					getLanguageList(sessionBean);
					/*
					 * call getMetaDataTypeList
					 */
					getMetaDataTypeList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					sessionBean.setMetaDataType(null);
					/*
					 * call getMetaDataTypeList
					 */
					getMetaDataTypeList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_ITEM"))
				{
					/*
					 * call function to delete item from list
					 */
					deleteItem(sessionBean);
					// empty sessionBean.getItemToBeDeleted()
					sessionBean.setItemToBeDeleted(null);
				}
				else if(sessionBean.getActionClicked().equals("RESET"))
				{
					/*
					 * call reset operation
					 */
					resetOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("VALIDATE"))
				{
					/*
					 * call validate operation
					 */
					validateOperation(sessionBean);
					/*
					 * CALL getScheduleList
					 */
					getScheduleList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_SCHEDULE"))
				{
					/*
					 * call delete schedule function
					 */
					deleteScheduleTransactions(request, sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("REFRESH"))
				{
					/*
					 * CALL getScheduleList
					 */
					getScheduleList(sessionBean);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "doPost()", e);
		}
		finally
		{
			metaDataValidationDAO = null;
		}
		
		if(useReqDis==true)
		{
			RequestDispatcher rs = request.getRequestDispatcher("/jsps/metadatavalidation.jsp");
			rs.forward(request, response);
		}
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
	
	
	private MetaDataValidationBean getSessionBean(HttpServletRequest request) 
	{
		MetaDataValidationBean sessionBean = null;
		if (null != request.getSession().getAttribute("metaDataValidationBean") && !"".equals(request.getSession().getAttribute("metaDataValidationBean"))) 
		{
			sessionBean = (MetaDataValidationBean) request.getSession().getAttribute("metaDataValidationBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new MetaDataValidationBean();
			request.getSession().setAttribute("metaDataValidationBean", sessionBean);
		}
		return sessionBean;
	}

	private void getCountryList(MetaDataValidationBean sessionBean)
	{
		sessionBean.setCountryList(new ArrayList<SelectItemDetails>());
		try
		{
			ArrayList<SelectItemDetails> list = metaDataValidationDAO.getCountryList();
			if(null!=list && list.size()>0)
			{
				sessionBean.setCountryList(list);
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "getCountryList()", e);
		}
	}
	
	private void getLanguageList(MetaDataValidationBean sessionBean)
	{
		sessionBean.setLanguageList(new ArrayList<SelectItemDetails>());
		try
		{
			if(null!=sessionBean.getCountryId() && !"".equals(sessionBean.getCountryId()))
			{
				ArrayList<SelectItemDetails> list = metaDataValidationDAO.getLanguageList(sessionBean.getCountryId());
				if(null!=list && list.size()>0)
				{
					sessionBean.setLanguageList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "getLanguageList()", e);
		}
	}
	
	private void getMetaDataTypeList(MetaDataValidationBean sessionBean)
	{
		sessionBean.setMetaDataTypeList(new ArrayList<SelectItemDetails>());
		try
		{
			if(null!=sessionBean.getLanguageCode() && !"".equals(sessionBean.getLanguageCode()))
			{
				String comboKeys="";
				String key = sessionBean.getLanguageCode().trim();
				key = key.replace("-", "_");
				if(mnaoLocales.trim().toLowerCase().indexOf(key.trim().toLowerCase()) > -1)
				{
					// MNAO LOCALES
					comboKeys = ApplicationProperties.getProperty("mnao.supported.filetypes");
				}
				else if(mcLocales.trim().toLowerCase().indexOf(key.trim().toLowerCase()) > -1)
				{
					// MC LOCALES
					comboKeys = ApplicationProperties.getProperty("mc.supported.filetypes"); 
				}
				else if(mmeLocales.trim().toLowerCase().indexOf(key.trim().toLowerCase()) > -1)
				{
					// MME LOCALES
					comboKeys = ApplicationProperties.getProperty("mme.supported.filetypes");
				}
				key=null;
				
				
				if(null!=comboKeys && !"".equals(comboKeys))
				{
					SelectItemDetails si = new SelectItemDetails();
					StringTokenizer str = new StringTokenizer(comboKeys,",");
					while(str.hasMoreTokens())
					{
						si = new SelectItemDetails();
						si.setValue(str.nextToken());
						si.setLabel(msgProps.getProperty(si.getValue()+".metadata.supported.filetypes.label"));
						sessionBean.getMetaDataTypeList().add(si);
						si=null;
					}
					si=null;
					str=null;
				}
				comboKeys=null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "getMetaDataTypeList()", e);
		}
	}
	
	
	private void readParamsFromRequest(MetaDataValidationBean sessionBean, HttpServletRequest request)
	{
		try
		{
			StringBuilder errorMessage=new StringBuilder();
			
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setScheduleDisplayPageLength(null);
			sessionBean.setScheduleDisplayPageNo(null);
			sessionBean.setActionClicked(null);
			sessionBean.setCountryId(null);
			sessionBean.setLanguageCode(null);
			sessionBean.setLanguageId(null);
			sessionBean.setMetaDataType(null);
			sessionBean.setItemToBeDeleted(null);
			
			deleteScheduleIds = null;
			
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
			while (iterator.hasNext()) 
			{
				FileItem fileItem = iterator.next();
				if (fileItem.isFormField())
				{
					String fieldName = fileItem.getFieldName();
					if (null != fieldName && !"".equals(fieldName)) 
					{
						/*
						 * set displayPageNo and displayPageLenght
						 */
						if (fieldName.equals("MDVAL_displayPageNo")) 
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
						
						if (fieldName.equals("MDVAL_displayPageLen")) 
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
						
						if (fieldName.equals("MDVAL_Schedule_displayPageNo")) 
						{
							// set the value in sessionBean.setScheduleDisplayPageNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setScheduleDisplayPageNo(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MDVAL_Schedule_displayPageLen")) 
						{
							// set the value in sessionBean.setScheduleDisplayPageLength
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setScheduleDisplayPageLength(value);
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("MDVAL_Sch_SelectedRows"))
						{
							// set the valUe in local variable
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								deleteScheduleIds=value;
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MDVAL_CountryLocale_Code")) 
						{
							// set the value in sessionBean.setCountryLocaleId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setCountryId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MDVAL_Lang_Code")) 
						{
							// set the value in sessionBean.setManualLanguageId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setLanguageId(value);
								if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
								{
									for(SelectItemDetails details : sessionBean.getLanguageList())
									{
										if(details.getValue().trim().toLowerCase().equals(sessionBean.getLanguageId().trim().toLowerCase()))
										{
											sessionBean.setLanguageCode(details.getCode());
										}
										details=null;
									}
								}
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MDVAL_MetaDataType")) 
						{
							// set the value in sessionBean.setMetaDataType
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setMetaDataType(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MDVAL_ActionClicked")) 
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
						
						if (fieldName.equals("MDVAL_ItemToBeDeleted")) 
						{
							// set the value in sessionBean.selectedRows
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setItemToBeDeleted(value);
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
								 * validate files
								 */
								String extension="";
								if(fileName.lastIndexOf(".")!=-1)
								{
									// do not remove (.) from extension
									extension = fileName.substring(fileName.lastIndexOf("."), fileName.length());
									if(null!=extension && !"".equals(extension))
									{
										if(validateSupportedFileType(sessionBean, extension, errorMessage, fileName))
										{
											boolean writeFlag=false;
											fileName = String.valueOf(new Date().getTime())+"_"+fileName;
											String tempPhysicalPath=ApplicationProperties.getProperty("metadata.validation.upload.physicalpath");
											try
											{
												tempPhysicalPath = tempPhysicalPath.replace("\\", "/");
												// write file to upload Location
												if(!tempPhysicalPath.endsWith("/"))
												{
													tempPhysicalPath+="/";
												}
												// add fileName
												tempPhysicalPath+= fileName;
												writeFlag=FileReadWriteUtil.writeFile(data, tempPhysicalPath);
											}
											catch(Exception e)
											{
												Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "readParamsFromRequest()", e);
											}
											
											if(writeFlag==true)
											{
												/*
												 * ADD THE FILE TO THE UPLOADED LIST
												 */
												if(null==sessionBean.getItemsList() || sessionBean.getItemsList().size()<=0)
												{
													sessionBean.setItemsList(new ArrayList<MetaDataFileDetails>());
												}
												MetaDataFileDetails fileDetails = new MetaDataFileDetails();
												fileDetails.setSrNo(sessionBean.getItemsList().size()+1);
												fileDetails.setFileName(fileName);
												fileDetails.setMetaDataType(sessionBean.getMetaDataType());
												fileDetails.setMetaDataTypeLabel(msgProps.getProperty(sessionBean.getMetaDataType()+".metadata.supported.filetypes.label"));
												fileDetails.setLocale(sessionBean.getLanguageCode());
												fileDetails.setFilePath(tempPhysicalPath);
												fileDetails.setWebFilePath(com.mazda.gms3.dmt.utils.OkAssetsWeb.url(ApplicationProperties.getProperty("metadata.validation.upload.relativepath")));
												// add fileName to relative Path
												if(!fileDetails.getWebFilePath().endsWith("/"))
												{
													fileDetails.setWebFilePath(fileDetails.getWebFilePath()+"/");
												}
												// add fileName
												fileDetails.setWebFilePath(fileDetails.getWebFilePath()+fileName);
												fileDetails.setFileData(data);
												sessionBean.getItemsList().add(fileDetails);
												fileDetails=null;
												tempPhysicalPath=null;
											}
											else
											{
												logger.info("readParamsFromRequest() :: Failed to Write File to Upload Location. Throw message - Failed to upload file. Please try after sometime.");
												if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
												{
													errorMessage.append("<MSG_TOKEN>");
												}
												errorMessage.append(msgProps.addMessage("error.fail.upload", fileName));
											}
										}
									}
									else
									{
										logger.info("readParamsFromRequest() :: File Name does not contain valid extension. Throw message - Please upload a valid File.");
										if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
										{
											errorMessage.append("<MSG_TOKEN>");
										}
										errorMessage.append(msgProps.addMessage("error.valid.file.invalid", fileName));
									}
								}
								else
								{
									logger.info("readParamsFromRequest() :: File Name does not contain any extension. Throw message - Please upload a valid File.");
									if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
									{
										errorMessage.append("<MSG_TOKEN>");
									}
									errorMessage.append(msgProps.addMessage("error.valid.file.invalid", fileName));
								}
								extension = null;
							}	
							else
							{
								logger.info("readParamsFromRequest() :: File Name / Size is null. Throw message - Please upload a valid File.");
								if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
								{
									errorMessage.append("<MSG_TOKEN>");
								}
								errorMessage.append(msgProps.getProperty("error.valid.file"));
							}
							fileName=  null;
							data = null;
						}
					}
				}
			}
			
			if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
			{
				sessionBean.setErrorMessage(errorMessage.toString());
			}
			errorMessage = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "readParamsFromRequest()", e);
		}
	}

	
	private boolean validateFileUpload(MetaDataValidationBean sessionBean)
	{
		try
		{
			if(null==sessionBean.getCountryId() || "".equals(sessionBean.getCountryId()) || 
					null==sessionBean.getLanguageId() || "".equals(sessionBean.getLanguageId()) || 
					null==sessionBean.getLanguageCode() || "".equals(sessionBean.getLanguageCode()) || 
					null==sessionBean.getMetaDataType() || "".equals(sessionBean.getMetaDataType()))
			{
				sessionBean.setErrorMessage(msgProps.getProperty("error.message.mandatory.fields"));
				return false;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(),"validateFileUpload()", e);
			return false;
		}
		return true;
	}
	
	private boolean validateSupportedFileType(MetaDataValidationBean sessionBean, String extension, StringBuilder errorMessage, String fileName)
	{
		try
		{
			if(null!=sessionBean.getLanguageCode() && !"".equals(sessionBean.getLanguageCode()))
			{
				String key = sessionBean.getLanguageCode().trim();
				key = key.replace("-", "_");
				if(mnaoLocales.trim().toLowerCase().indexOf(key.trim().toLowerCase()) > -1)
				{
					// MNAO MARKET
					if(sessionBean.getMetaDataType().equals("LEFTMENU"))
					{
						// extension must be a text file
						if(!extension.trim().toLowerCase().equals(ApplicationProperties.getProperty("extension.txt").trim().toLowerCase()))
						{
							logger.info("validateSupportedFileType() :: Extension is not TEXT. Throw Message - Uploaded file not supported."); 
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.valid.txt.file", fileName));
							return false;
						}
						
						// filename must be leftmenu.txt
						if(!fileName.trim().toLowerCase().startsWith("leftmenu"))
						{
							logger.info("validateSupportedFileType() :: File Name does not start with leftmenu. Throw Message - Uploaded file not supported."); 
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.valid.txt.file", fileName));
							return false;
						}
					}
					else if(sessionBean.getMetaDataType().equals("VIN"))
					{
						// extension must be a ENT FILE
						if(!extension.trim().toLowerCase().equals(ApplicationProperties.getProperty("extension.ent").trim().toLowerCase()))
						{
							logger.info("validateSupportedFileType() :: Extension is not ENT. Throw Message - Uploaded file not supported."); 
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.valid.ent.file", fileName));
							return false;
						}
						
						// filename must be vin.ent / vin1.ent
						if(!fileName.trim().toLowerCase().startsWith("vin"))
						{
							logger.info("validateSupportedFileType() :: File Name does not start with vin. Throw Message - Uploaded file not supported."); 
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.valid.ent.file", fileName));
							return false;
						}
					}
					else if(sessionBean.getMetaDataType().equals("VINATTRIBUTE"))
					{

						// extension must be a ENT FILE
						if(!extension.trim().toLowerCase().equals(ApplicationProperties.getProperty("extension.ent").trim().toLowerCase()))
						{
							logger.info("validateSupportedFileType() :: Extension is not ENT. Throw Message - Uploaded file not supported."); 
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.valid.ent.file", fileName));
							return false;
						}
						
						// filename must be vin_attribute.ent / vin_attribute1.ent
						if(!fileName.trim().toLowerCase().startsWith("vin_attribute"))
						{
							logger.info("validateSupportedFileType() :: File Name does not start with vin_attribute. Throw Message - Uploaded file not supported."); 
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.valid.ent.file", fileName));
							return false;
						}
					
					}
				}
				else if((mcLocales.trim().toLowerCase().indexOf(key.trim().toLowerCase()) > -1) || 
						(mmeLocales.trim().toLowerCase().indexOf(key.trim().toLowerCase()) > -1))
				{
					// MC OR MME MARKET
					// extension must be a text file
					if(!extension.trim().toLowerCase().equals(ApplicationProperties.getProperty("extension.txt").trim().toLowerCase()))
					{
						logger.info("validateSupportedFileType() :: Extension is not TEXT. Throw Message - Uploaded file not supported."); 
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.valid.txt.file", fileName));
						return false;
					}
					
//					if(sessionBean.getMetaDataType().equals("ESICATEGORY") || sessionBean.getMetaDataType().equals("ESICATEGORY_MANUALS") 
//							|| sessionBean.getMetaDataType().equals("ESICATEGORY_WD"))
					if(sessionBean.getMetaDataType().startsWith("ESICATEGORY"))
					{
						// filename must start with esi
						if(!fileName.trim().toLowerCase().startsWith("esi"))
						{
							logger.info("validateSupportedFileType() :: File Name does not start with esi. Throw Message - Uploaded file not supported."); 
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.valid.txt.file", fileName));
							return false;
						}
					}
					
					if(sessionBean.getMetaDataType().equals("VIN"))
					{
						// filename must start with VIN
						if(!fileName.trim().toLowerCase().startsWith("vin"))
						{
							logger.info("validateSupportedFileType() :: File Name does not start with vin. Throw Message - Uploaded file not supported."); 
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.valid.txt.file", fileName));
							return false;
						}
					}
					
					if(sessionBean.getMetaDataType().equals("DISPLAYORDER"))
					{
						// filename must start with VIN
						if(!fileName.trim().toLowerCase().startsWith("d"))
						{
							logger.info("validateSupportedFileType() :: File Name does not start with d. Throw Message - Uploaded file not supported."); 
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.valid.txt.file", fileName));
							return false;
						}
					}
					
					if(sessionBean.getMetaDataType().equals("CDPROCESSING"))
					{
						// filename must start with VIN
						if(!fileName.trim().toLowerCase().startsWith("cd"))
						{
							logger.info("validateSupportedFileType() :: File Name does not start with cd. Throw Message - Uploaded file not supported."); 
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.addMessage("error.valid.txt.file", fileName));
							return false;
						}
					}
				}
				else
				{
					logger.info("validateSupportedFileType() :: Failed to Identify Market Type for the Selected Language.");
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.valid.file.invalid", fileName));
					return false;
				}
				key=null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(),"validateSupportedFileType()", e);
			if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
			{
				errorMessage.append("<MSG_TOKEN>");
			}
			errorMessage.append(msgProps.addMessage("error.fail.upload",fileName));
			return false;
		}
		return true;
	}
	
	private void deleteItem(MetaDataValidationBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getItemToBeDeleted() && !"".equals(sessionBean.getItemToBeDeleted()))
			{
				if(null!=sessionBean.getItemsList() && sessionBean.getItemsList().size()>0)
				{
					for(int a=0;a<sessionBean.getItemsList().size();a++)
					{
						MetaDataFileDetails details = (MetaDataFileDetails)sessionBean.getItemsList().get(a);
						if(String.valueOf(details.getSrNo()).equals(sessionBean.getItemToBeDeleted()))
						{
							// also delete file from upload location
							FileReadWriteUtil.deleteFile(details.getFilePath());
							// remove item from List
							sessionBean.getItemsList().remove(a);
							a--;
							break;
						}
					}
				}
				
				// re-arrange srNo
				if(null!=sessionBean.getItemsList() && sessionBean.getItemsList().size()>0)
				{
					for(int a=0;a<sessionBean.getItemsList().size();a++)
					{
						MetaDataFileDetails details = (MetaDataFileDetails)sessionBean.getItemsList().get(a);
						details.setSrNo(a+1);
					}
				}
			}
			else
			{
				logger.info("deleteItem() :: Sr. No. for the item to be deleted is null. Failed to delete item.");
				sessionBean.setErrorMessage(msgProps.getProperty("error.message.faiure"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(),"deleteItem()", e);
			sessionBean.setErrorMessage(msgProps.getProperty("error.message.faiure"));
		}
	}
	
	private void resetOperation(MetaDataValidationBean sessionBean)
	{
		sessionBean.setActionClicked(null);
		sessionBean.setCountryId(null);
		sessionBean.setLanguageCode(null);
		sessionBean.setLanguageId(null);
		sessionBean.setMetaDataType(null);
		sessionBean.setItemToBeDeleted(null);
		sessionBean.setErrorMessage(null);
		sessionBean.setSuccessMessage(null);
		/*
		 * ITERATE ITEMS LIST AND DELETE ALL THE FILES FROM UPLOAD LOCATION
		 */
		if(null!=sessionBean.getItemsList() && sessionBean.getItemsList().size()>0)
		{
			for(MetaDataFileDetails d  : sessionBean.getItemsList())
			{
				try
				{
					if(null!=d.getFilePath() && !"".equals(d.getFilePath()))
					{
						FileReadWriteUtil.deleteFile(d.getFilePath());
					}
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "resetOperation()", e); 
				}
				d=null;
			}
		}
		
		sessionBean.setItemsList(null);
		sessionBean.setMetaDataTypeList(null);
		sessionBean.setLanguageList(null);
	}
	
	private void resetOperationAfterSave(MetaDataValidationBean sessionBean)
	{
		sessionBean.setActionClicked(null);
//		sessionBean.setCountryId(null);
//		sessionBean.setLanguageCode(null);
//		sessionBean.setLanguageId(null);
		sessionBean.setMetaDataType(null);
		sessionBean.setItemToBeDeleted(null);
		sessionBean.setErrorMessage(null);
		sessionBean.setSuccessMessage(null);
		sessionBean.setItemsList(null);
//		sessionBean.setMetaDataTypeList(null);
//		sessionBean.setLanguageList(null);
	}
	
	
	private void validateOperation(MetaDataValidationBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getItemsList() && sessionBean.getItemsList().size()>0)
			{
				ArrayList<MetaDataFileDetails> failureList = new ArrayList<MetaDataFileDetails>();
				StringBuilder errorMessage=new StringBuilder();
				/*
				 * FOR EACH ROW CREATE A NEW SCHEDULE ID AND EXEUCTE A PRALLEL THREAD FOR EVERY ONE TO FINISH 
				 */
				StringBuilder successFiles = new StringBuilder();
				for(MetaDataFileDetails details : sessionBean.getItemsList())
				{
					try
					{
						// set key Name
						details.setScheduleName(ApplicationProperties.getProperty("metadata.sch.key"));
						details.setWslId(wslId);
						details.setStartTime(new Timestamp(new Date().getTime()));
						details.setStatus(MetaDataScheduleConstants.STATUS_PENDING);
						details.setThreadId(ApplicationProperties.getProperty("metadata.sch.key"));
						details.setScheduleId(metaDataValidationDAO.createSchedule(details));
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "validateOperation()", e);
					}
					
					if(null!=details.getScheduleId() && details.getScheduleId()>0)
					{
						/*
						 * add file Name to successFiles
						 */
						if(null!=successFiles && null!=successFiles.toString() && !"".equals(successFiles.toString()))
						{
							successFiles.append(", ");
						}
						successFiles.append(details.getFileName());
						
						
						// update schedule name and thread id
						details.setScheduleName(details.getScheduleName()+String.valueOf(details.getScheduleId()));
						details.setThreadId(details.getThreadId()+String.valueOf(details.getScheduleId()));
						
						/*
						 * start execution thread for 
						 */
						String threadId = details.getThreadId();
						final String code = String.valueOf(details.getScheduleId());
						final MetaDataValidationScheduleImpl valSchImpl = new MetaDataValidationScheduleImpl();
						final String fileName=details.getFileName();
						final String metaDataType=details.getMetaDataType();
						final String locale = details.getLocale();
						Runnable runn = new Runnable() 
						{
							@Override
							public void run() {
								
								synchronized (valSchImpl) {
									try {
										valSchImpl.startValidation(code, fileName, metaDataType, locale);
									} catch (Exception e) {
										Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "run()", e);
									}
								}
							}
						};
						
						Thread th = new Thread(runn, threadId);
						th.start();
						threadId = null;
					}
					else
					{
						logger.info("validateOperation :: Failed to Create Schedule for Validation of {"+details.getMetaDataType()+"} for file :: > "+ details.getFileName()+ " of Locale :: > "+ details.getLocale());
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							// add token
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = details.getMetaDataTypeLabel()+","+details.getFileName()+","+details.getLocale();
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.message.fail.create.validation.schedule"));
						data = null;
						id = null;
						// add this to failure list
						failureList.add(details);
					}
					details = null;
				}
				
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					// set error Message
					sessionBean.setErrorMessage(errorMessage.toString());
				}
				errorMessage = null;
				
				if(null!=failureList && failureList.size()>0)
				{
					// reset itemsList and set failureList in it - show only failed items
					sessionBean.setItemsList(new ArrayList<MetaDataFileDetails>());
					for(int a=0;a<failureList.size();a++)
					{
						MetaDataFileDetails details = (MetaDataFileDetails)failureList.get(a);
						details.setSrNo(a+1);
						sessionBean.getItemsList().add(details);
						details=null;
					}
				}
				
				// check if failureList is null = all went success
				if(null==failureList || failureList.size()<=0)
				{
					/*
					 * call reset operation after save
					 */
					resetOperationAfterSave(sessionBean);
				}
				
				if(null!=successFiles && null!=successFiles.toString() && !"".equals(successFiles.toString()))
				{
					// set successMessage
					sessionBean.setSuccessMessage(msgProps.addMessage("conversion.schedule.success", msgProps.getProperty("label.metadata") +" "+msgProps.getProperty("label.validations")));
					if(null!=failureList && failureList.size()>0)
					{
						// some failure files exists - add specific success files Names
						sessionBean.setSuccessMessage(sessionBean.getSuccessMessage()+" for Files: "+ successFiles.toString());
					}
				}
				successFiles = null;
				failureList = null;
			}
			else
			{
				sessionBean.setErrorMessage(msgProps.getProperty("error.upload.atleast.one.item"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "validateOperation()", e); 
		}
	}
	
	private void getScheduleList(MetaDataValidationBean sessionBean)
	{
		sessionBean.setReloadJSP(false);
		sessionBean.setScheduleList(new ArrayList<MetaDataFileDetails>());
		try
		{
			ArrayList<MetaDataFileDetails> list = metaDataValidationDAO.getScheduleList();
			if(null!=list && list.size()>0)
			{
				MetaDataFileDetails details = null;
				for(int a=0;a<list.size();a++)
				{
					details=(MetaDataFileDetails)list.get(a);
					if(null!=details.getStatus() && (details.getStatus().equals(MetaDataScheduleConstants.STATUS_PENDING) || 
							details.getStatus().equals(MetaDataScheduleConstants.STATUS_PROCESSING)))
					{
						// reload page to get the current status of running schedules
						sessionBean.setReloadJSP(true);
						break;
					}
				}
				details=null;
				
				for(int a=0;a<list.size();a++)
				{
					details=(MetaDataFileDetails)list.get(a);
					if(null!=details.getMetaDataType() && !"".equals(details.getMetaDataType()))
					{
						details.setMetaDataTypeLabel(msgProps.getProperty(details.getMetaDataType()+".metadata.supported.filetypes.label"));
					}
				}
				details=null;
				
				sessionBean.setScheduleList(list);
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "getScheduleList()", e); 
		}
	}
	
	private void getMarketSpecificLocales()
	{
		try
		{
			if(null==mnaoLocales || "".equals(mnaoLocales))
			{
				mnaoLocales = Utilities.getMarketWiseLocalesList(ApplicationProperties.getProperty("market.mnao"));
			}
			if(null==mcLocales || "".equals(mcLocales))
			{
				mcLocales = Utilities.getMarketWiseLocalesList(ApplicationProperties.getProperty("market.mc"));
			}
			if(null==mmeLocales || "".equals(mmeLocales))
			{
				mmeLocales=  Utilities.getMarketWiseLocalesList(ApplicationProperties.getProperty("market.mme"));
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "getMarketSpecificLocales()", e);
		}
		
	}
	
	private void deleteScheduleTransactions(HttpServletRequest request, MetaDataValidationBean sessionBean)
	{
		try
		{
			if(null!=deleteScheduleIds && !"".equals(deleteScheduleIds))
			{
				String selRows = (String)deleteScheduleIds;
				if(null!=selRows && !"".equals(selRows))
				{
					if(selRows.endsWith(","))
					{
						selRows = selRows.substring(0,selRows.length()-1);
					}
					boolean bool = metaDataValidationDAO.deleteScheduleDetails(selRows);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.getProperty("metadata.schedule.delete.success"));
						/*
						 * call getScheduleList
						 */
						getScheduleList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.getProperty("error.message.metadata.schedule.delete"));
					}
				}
				else
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.delete"));
				}
				selRows=  null;
			}
			else
			{
				sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.delete"));
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MetaDataValidation.class.getName(), "deleteScheduleTransactions()", e);
		}
	}

}