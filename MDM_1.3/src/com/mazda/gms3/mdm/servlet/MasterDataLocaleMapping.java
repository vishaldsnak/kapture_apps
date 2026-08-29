package com.mazda.gms3.mdm.servlet;

import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import java.sql.Connection;
import com.mazda.gms3.mdm.utils.ImportActionUtils;
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
import java.util.StringTokenizer;

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

import com.mazda.gms3.mdm.bean.MasterDataLocaleMappingBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.MasterDataLocaleMappingDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.MasterDataLocaleMappingConstants;
import com.mazda.gms3.mdm.vo.MasterDataLocaleMappingDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.sst.utils.SSTUtils;

/**
 * Servlet implementation class MasterDataLocaleMapping
 */
public class MasterDataLocaleMapping extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	private Logger logger = LogManager.getLogger(MasterDataLocaleMapping.class);
	String wslId=null;
	MessageProperties msgProps= null;
	String reportName=null;
	
	String moduleRefKey=AccessManagementInterface.REF_KEY_MASTERDATA_LOCALE_MAPPING;
	String selectedRowForMasterDataType=null;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MasterDataLocaleMapping() {
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
			 * Initialize bean
			 */
			MasterDataLocaleMappingBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setMasterDataTypesList(null);
			sessionBean.setMarketList(null);
			sessionBean.setMappingList(null);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setActionClicked(null);
			sessionBean.setMappingListToImport(new ArrayList<MasterDataLocaleMappingDetails>());
			sessionBean.setReportViewPath(null);
			sessionBean.setInfoMessage(null);
			sessionBean.setInfoMessage(null);
			
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
			/*
			 * call function to load masketList
			 */
			getMarketList(sessionBean);
			
			/*
			 * call function to load Mapping List
			 */
			getMappingList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/masterdatalocalemapping.jsp");
				rs.forward(request, response);
			}
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
			 * Initialize bean
			 */
			MasterDataLocaleMappingBean sessionBean = getSessionBean(request);
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
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
			
			/*
			 * call function to read parameters from request
			 */
			readParamsFromRequest(sessionBean, request);
			
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("MARKET_SELECTION"))
				{
					if(null!=sessionBean.getFieldDetails())
					{
						sessionBean.getFieldDetails().setMasterDataType(null);
					}
					sessionBean.setMasterDataTypesList(new ArrayList<SelectItemDetails>());
					if(null!=sessionBean.getFieldDetails() && null!=sessionBean.getFieldDetails().getMarket() && !"".equals(sessionBean.getFieldDetails().getMarket()))
					{
						sessionBean.setMasterDataTypesList(getMasterDataTypesList(sessionBean.getFieldDetails().getMarket()));
					}
				}
				else if(sessionBean.getActionClicked().equals("MARKET_DATATABLE_SELECTION"))
				{
					/*
					 * Call function to populate Master Data Type for the selectedRow
					 */
					populateMasterDataTypeForSpecificRow(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SAVE_MAPPING"))
				{
					/*
					 * Save Operation called
					 */
					saveMasterDataLocaleMappingDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EDIT_MAPPING"))
				{
					/*
					 * Edit Operation called
					 */
					editMasterDataLocaleMappingDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_MAPPING"))
				{
					/*
					 * Delete Operation called
					 */
					deleteMasterDataLocaleMappingDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("ACTIVE_MAPPING"))
				{
					/*
					 * Active Operation called
					 */
					activeMasterDataLocaleMappingDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE_MAPPING"))
				{
					/*
					 * Update Operation called
					 */
					updateMasterDataLocaleMappingDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("RESET_MAPPING"))
				{
					// reset some fields
					resetMasterDataLocaleMappingOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EXPORT_MAPPING"))
				{
					/*
					 * Export MasterDataLocaleMapping Operation
					 */
					exportMasterDataLocaleMappingDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/masterdatalocalemapping.jsp");
				rs.forward(request, response);
			}
		}
	}

	
	private MasterDataLocaleMappingBean getSessionBean(HttpServletRequest request) 
	{
		MasterDataLocaleMappingBean sessionBean = null;
		if (null != request.getSession().getAttribute("masterDataLocaleMappingBean") && !"".equals(request.getSession().getAttribute("masterDataLocaleMappingBean"))) 
		{
			sessionBean = (MasterDataLocaleMappingBean) request.getSession().getAttribute("masterDataLocaleMappingBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new MasterDataLocaleMappingBean();
			request.getSession().setAttribute("masterDataLocaleMappingBean", sessionBean);
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
	
	private void performAccessCheck(MasterDataLocaleMappingBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "performAccessCheck()", e);
		}
	}

	private void getFlagList(MasterDataLocaleMappingBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());
		SelectItemDetails si = new SelectItemDetails();
		si.setLabel(msgProps.getProperty("flag.label.active"));
		si.setValue(ApplicationProperties.getProperty("flag.value.active"));
		sessionBean.getFlagList().add(si);
		si = null;
		
		si = new SelectItemDetails();
		si.setLabel(msgProps.getProperty("flag.label.delete"));
		si.setValue(ApplicationProperties.getProperty("flag.value.delete"));
		sessionBean.getFlagList().add(si);
		si = null;
	}
	
	private ArrayList<SelectItemDetails> getMasterDataTypesList(String market)
	{
		ArrayList<SelectItemDetails> list = new ArrayList<SelectItemDetails>();
		try
		{
			if(null!=market && !"".equals(market))
			{
				SelectItemDetails si = new SelectItemDetails();
				String label="";
				String value = "";
				StringTokenizer strLabels = new StringTokenizer(MasterDataLocaleMappingConstants.ITEMS_LABELS,",");
				StringTokenizer strValues = new StringTokenizer(MasterDataLocaleMappingConstants.ITEMS_KEYS,",");
				while(strLabels.hasMoreTokens() && strValues.hasMoreTokens())
				{
					label  =strLabels.nextToken();
					value= strValues.nextToken();
					if(ApplicationProperties.getProperty("market.mnao").equals(market))
					{
						// DO NOT ADD CARLINE
						if(!value.equals(MasterDataLocaleMappingConstants.ITEM_KEY_CARLINE))
						{
							si = new SelectItemDetails();
							si.setLabel(label+" ("+value+")");
							si.setValue(value);
							list.add(si);
							si= null;
						}
					}
					else
					{
						// DO NOT ADD MODEL_YEAR & VIN_RANGE
						if(!value.equals(MasterDataLocaleMappingConstants.ITEM_KEY_MODEL_YEAR) && !value.equals(MasterDataLocaleMappingConstants.ITEM_KEY_VIN_RANGE))
						{
							si = new SelectItemDetails();
							si.setLabel(label+" ("+value+")");
							si.setValue(value);
							list.add(si);
							si= null;
						}
					}
					label = null;
					value=  null;
				}
				label=  null;
				value= null;
				si = null;
				strLabels=  null;
				strValues = null;
			
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "getMasterDataTypesList()", e);
		}
		return list;
	}
	
	private void getMarketList(MasterDataLocaleMappingBean sessionBean)
	{
		sessionBean.setMarketList(new ArrayList<SelectItemDetails>());
		try
		{
			SelectItemDetails si = new SelectItemDetails();
			si.setLabel(ApplicationProperties.getProperty("market.mnao"));
			si.setValue(ApplicationProperties.getProperty("market.mnao"));
			sessionBean.getMarketList().add(si);
			si = null;
			
			si = new SelectItemDetails();
			si.setLabel(ApplicationProperties.getProperty("market.mc"));
			si.setValue(ApplicationProperties.getProperty("market.mc"));
			sessionBean.getMarketList().add(si);
			si = null;
			
			si = new SelectItemDetails();
			si.setLabel(ApplicationProperties.getProperty("market.mme"));
			si.setValue(ApplicationProperties.getProperty("market.mme"));
			sessionBean.getMarketList().add(si);
			si = null;
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "getMasterDataTypesList()", e);
		}
	}
	
	private void getMappingList(MasterDataLocaleMappingBean sessionBean)
	{
		try
		{
			sessionBean.setMappingList(new ArrayList<MasterDataLocaleMappingDetails>());
			
			ArrayList<MasterDataLocaleMappingDetails> list = new ArrayList<MasterDataLocaleMappingDetails>();
			list = MasterDataLocaleMappingDAO.getMasterDataLocaleMappingDetailsList();
			if(null!=list && list.size()>0)
			{
				String[] tokValues = MasterDataLocaleMappingConstants.ITEMS_KEYS.split(",");
				String[] tokLabels = MasterDataLocaleMappingConstants.ITEMS_LABELS.split(",");
				for(int i=0;i<list.size();i++)
				{
					MasterDataLocaleMappingDetails details = (MasterDataLocaleMappingDetails)list.get(i);
					
					details.setMasterDataTypeList(new ArrayList<SelectItemDetails>());
					if(null!=details.getMarket() && !"".equals(details.getMarket()))
					{
						details.setMasterDataTypeList(getMasterDataTypesList(details.getMarket()));
					}
					
					if(null!=details.getFlag() && !"".equals(details.getFlag()))
					{
						// set Label
						if(details.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
						{
							details.setFlagLabel(msgProps.getProperty("flag.label.active"));
						}
						else if(details.getFlag().equals(ApplicationProperties.getProperty("flag.value.delete")))
						{
							details.setFlagLabel(msgProps.getProperty("flag.label.delete"));
						}
					}
					
					if(null!=details.getMasterDataType() && !"".equals(details.getMasterDataType()))
					{
						if(null!=tokValues && tokValues.length>0)
						{
							for(int e=0;e<tokValues.length;e++)
							{
								if(tokValues[e].equals(details.getMasterDataType()))
								{
									// get Label from tokLables at same Index
									details.setMasterDataTypeLabel(tokLabels[e]);
									break;
								}
							}
						}
					}
					details=  null;
				}
				tokValues=  null;
				tokLabels = null;
				// SET ALL LIST
				sessionBean.setMappingList(list);
			}
			list = null;
		
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "getMappingList()", e);
		}
	}
	
	private void readParamsFromRequest(MasterDataLocaleMappingBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new MasterDataLocaleMappingDetails());
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setMappingListToImport(null);
			selectedRowForMasterDataType = null;
			
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
						if (fieldName.equals("MDLM_UpdatedRows")) 
						{
							// set the value in sessionBean.displayPageNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setUpdatedRows(value);
							}
							// set value to null
							value = null;
						}
						
						
						if(fieldName.equals("MDLM_SelectedRows"))
						{
							// set the value in sessionBean.setSelectedRows
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSelectedRows(value);
							}
							// set value to null
							value = null;
						}
						
						/*
						 * set displayPageNo and displayPageLenght
						 */
						if (fieldName.equals("MDLM_DataTabel_displayPageNo")) 
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
						
						if (fieldName.equals("MDLM_DataTabel_displayPageLen")) 
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
						
						if (fieldName.equals("MDLM_SelectedRows")) 
						{
							// set the value in sessionBean.selectedRows
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSelectedRows(value);
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("MDLM_SelectedMappingIdForMasterDataType"))
						{
							// set the value in locale variable
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								selectedRowForMasterDataType = value;
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MDLM_Market_Code")) 
						{
							// set the value in sessionBean.getFieldDetails().setMarket
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setMarket(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MDLM_Locale")) 
						{
							// set the value in sessionBean.getFieldDetails().setLocale
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setLocale(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MDLM_MasterDataType")) 
						{
							// set the value in sessionBean.getFieldDetails().setMasterDataType
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setMasterDataType(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MDLM_AlternateLocale")) 
						{
							// set the value in sessionBean.getFieldDetails().setAlternateLocale
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setAlternateLocale(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MDLM_ActionClicked")) 
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
						String fileName = fileItem.getName();
						byte[] data = fileItem.get();
						if(null!=fileName && !"".equals(fileName) && null!=data)
						{
							/*
							 * check for EXCEL
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
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "readParamsFromRequest()", e);
		}
	}
	
	private void executeExcelOperation(MasterDataLocaleMappingBean sessionBean, byte[] data, String extension)
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
				ArrayList<MasterDataLocaleMappingDetails> listToSave = new ArrayList<MasterDataLocaleMappingDetails>();
				// ACTION=D rows are collected separately - they are a DELETE, not a save.
				ArrayList<MasterDataLocaleMappingDetails> listToDelete = new ArrayList<MasterDataLocaleMappingDetails>();
				MasterDataLocaleMappingDetails fieldDetails = new MasterDataLocaleMappingDetails();
				MasterDataLocaleMappingDetails existingDetails = new MasterDataLocaleMappingDetails();
				for(int i=0;i<sessionBean.getMappingListToImport().size();i++)
				{
					fieldDetails = new MasterDataLocaleMappingDetails();
					fieldDetails = (MasterDataLocaleMappingDetails)sessionBean.getMappingListToImport().get(i);
					fieldDetails.setSrNo((i+1+1));

					/*
					 * ACTION = D -> this row is a delete. Keep it out of listToSave so
					 * the existing create-or-update logic stays exactly as it was.
					 */
					if(ImportActionUtils.isDeleteAction(fieldDetails.getImportAction()))
					{
						listToDelete.add(fieldDetails);
						continue;
					}
					boolean addToList = true;
					if(null!=listToSave && listToSave.size()>0)
					{
						for(int j=0;j<listToSave.size();j++)
						{
							existingDetails = new MasterDataLocaleMappingDetails();
							existingDetails = (MasterDataLocaleMappingDetails)listToSave.get(j);
							/*
							 * IDENTIY DUPLICATE ON THE BASIS OF 
							 *  ABBREVIATION CODE
							 */
							if(fieldDetails.getMarket().equals(existingDetails.getMarket()) && 
									fieldDetails.getLocale().equals(existingDetails.getLocale()) && 
									fieldDetails.getAlternateLocale().equals(existingDetails.getAlternateLocale()) && 
									fieldDetails.getMasterDataType().equals(existingDetails.getMasterDataType()))
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
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.masterdatalocalemapping"), String.valueOf(rows[a])));
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
					fieldDetails = new MasterDataLocaleMappingDetails();
					ArrayList<MasterDataLocaleMappingDetails> tempList = listToSave;
					listToSave = new ArrayList<MasterDataLocaleMappingDetails>();
					for(int i=0;i<tempList.size();i++)
					{
						fieldDetails = new MasterDataLocaleMappingDetails();  
						fieldDetails = (MasterDataLocaleMappingDetails)tempList.get(i);
						listToSave.add(fieldDetails);
						fieldDetails = null;
					}
					
					listToSave = MasterDataLocaleMappingDAO.importMasterDataLocaleMappingDetails(listToSave);
					fieldDetails = new MasterDataLocaleMappingDetails();
					for(int i=0;i<listToSave.size();i++)
					{
						fieldDetails= new MasterDataLocaleMappingDetails();
						fieldDetails = (MasterDataLocaleMappingDetails)listToSave.get(i);
						if(fieldDetails.isSaveStatusWhileImport() == true)
						{
							logger.info("readParamsFromRequest() :: MasterDataLocaleMapping Data for Line No {"+(i+1+1)+"}. Saved Successfully.");
							if(null!=successLineNo && !"".equals(successLineNo))
							{
								successLineNo = successLineNo+",";
							}
							successLineNo = successLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						else
						{
							logger.info("readParamsFromRequest() :: Failed to Import MasterDataLocaleMapping Data for Line No {"+(i+1+1)+"}.");
							if(null!=errorLineNo && !"".equals(errorLineNo))
							{
								errorLineNo = errorLineNo+",";
							}
							errorLineNo = errorLineNo+String.valueOf(fieldDetails.getSrNo());
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
//						sessionBean.setSuccessMessage(msgProps.addMessage("import.success",msgProps.getProperty("label.masterdatalocalemapping"), successLineNo ));
						
						/*
						 * call function to load updated mapping list
						 */
						sessionBean.setSelectedRows(null);
						sessionBean.setShowUpdate(false);
						getMappingList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import", msgProps.getProperty("label.masterdatalocalemapping"), String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
//						errorLineNo= "( "+errorLineNo+ " )";
//						sessionBean.setErrorMessage(msgProps.addMessage("error.import", msgProps.getProperty("label.masterdatalocalemapping"), errorLineNo));
					}
					
					successLineNo = null;
					errorLineNo = null;		
				}
				// only complain about an empty file when there is nothing to delete either
				else if(null==listToDelete || listToDelete.size()<=0)
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
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.masterdatalocalemapping")));
				}
				/*
				 * ACTION = D ROWS - SOFT DELETE.
				 *
				 * The Excel carries no primary key, so each row is located with the SAME
				 * unique combination the create-or-update path uses, and the ids are handed
				 * to this screen's EXISTING delete method - so the import delete behaves
				 * identically to the on-screen delete (sync status, related tables, ...).
				 */
				if(null!=listToDelete && listToDelete.size()>0)
				{
					String deleteSuccessLineNo="";
					String deleteErrorLineNo="";
					java.util.List<Long> deleteIdList = new ArrayList<Long>();
					Connection deleteConn = null;
					try
					{
						deleteConn = DBConnectionHelper.getConnection();
						for(int i=0;i<listToDelete.size();i++)
						{
							MasterDataLocaleMappingDetails deleteDetails = (MasterDataLocaleMappingDetails)listToDelete.get(i);
							long existingId = 0;
							try
							{
								existingId = MasterDataLocaleMappingDAO.findExistingIdForImport(deleteDetails, deleteConn);
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "executeExcelOperation()", e);
							}
							if(existingId>0)
							{
								deleteIdList.add(new Long(existingId));
								if(null!=deleteSuccessLineNo && !"".equals(deleteSuccessLineNo))
								{
									deleteSuccessLineNo = deleteSuccessLineNo+",";
								}
								deleteSuccessLineNo = deleteSuccessLineNo+String.valueOf(deleteDetails.getSrNo());
							}
							else
							{
								/* NO ACTIVE ROW MATCHES - REPORT IT, never a silent no-op */
								if(null!=deleteErrorLineNo && !"".equals(deleteErrorLineNo))
								{
									deleteErrorLineNo = deleteErrorLineNo+",";
								}
								deleteErrorLineNo = deleteErrorLineNo+String.valueOf(deleteDetails.getSrNo());
							}
							deleteDetails = null;
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "executeExcelOperation()", e);
					}
					finally
					{
						try
						{
							if(null!=deleteConn)
							{
								deleteConn.close();
							}
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "executeExcelOperation()", e);
						}
						deleteConn = null;
					}
					int deleteSuccessCount=0;
					String deleteIds = ImportActionUtils.buildDeleteIds(deleteIdList);
					if(null!=deleteIds && !"".equals(deleteIds))
					{
						boolean deleted = false;
						try
						{
							deleted = MasterDataLocaleMappingDAO.deleteMasterDataLocaleMappingDetails(deleteIds);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "executeExcelOperation()", e);
						}
						if(deleted==true)
						{
							deleteSuccessCount = deleteIdList.size();
							getMappingList(sessionBean);
						}
						else
						{
							/* the delete itself failed - every row it covered is a failure */
							if(null!=deleteSuccessLineNo && !"".equals(deleteSuccessLineNo))
							{
								if(null!=deleteErrorLineNo && !"".equals(deleteErrorLineNo))
								{
									deleteErrorLineNo = deleteErrorLineNo+",";
								}
								deleteErrorLineNo = deleteErrorLineNo+deleteSuccessLineNo;
							}
							deleteSuccessLineNo = "";
						}
					}
					if(deleteSuccessCount>0)
					{
						/* successMessage is a single escaped c:out on the JSP - join with a SPACE */
						String existingMsg = sessionBean.getSuccessMessage();
						String deleteMsg = msgProps.addMessage("import.delete.success.count.message", String.valueOf(deleteSuccessCount));
						if(null!=existingMsg && !"".equals(existingMsg))
						{
							deleteMsg = existingMsg+" "+deleteMsg;
						}
						sessionBean.setSuccessMessage(deleteMsg);
					}
					if(null!=deleteErrorLineNo && !"".equals(deleteErrorLineNo))
					{
						String[] deleteErrorRows = deleteErrorLineNo.split(",");
						if(null!=deleteErrorRows && deleteErrorRows.length>0)
						{
							failureCount = failureCount+deleteErrorRows.length;
							for(int a=0;a<deleteErrorRows.length;a++)
							{
								errorCount++;
								if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
								{
									errorMessage.append("<MSG_TOKEN>");
								}
								errorMessage.append(msgProps.addMessage("error.import.delete", msgProps.getProperty("label.masterdatalocalemapping"), String.valueOf(deleteErrorRows[a])));
							}
						}
						deleteErrorRows = null;
					}
					deleteSuccessLineNo = null;
					deleteErrorLineNo = null;
					deleteIdList = null;
				}
				listToDelete = null;

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
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "executeExcelOperation()", e);
		}
	}

	private void readExcelData(byte[] data, MasterDataLocaleMappingBean sessionBean,String extension)
	{
		sessionBean.setMappingListToImport(new ArrayList<MasterDataLocaleMappingDetails>());
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
				 * MARKET
				 * LOCALE
				 * MASTER DATA TYPE
				 * ALTERNATE LOCALE
				 */
				while(null!=rowIterator && rowIterator.hasNext())
				{
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						MasterDataLocaleMappingDetails details = new MasterDataLocaleMappingDetails();
						Object dataCell = SSTUtils.readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setMarket(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setLocale(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setMasterDataType(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(3));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setAlternateLocale(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * NEW ACTION / MARKER COLUMN (last column of the import template).
						 * A/ADD (create), U/UPDATE (update), D/DELETE (soft delete).
						 * Blank or any non-delete value falls through to the existing
						 * create-or-update path, so an OLD template still imports.
						 */
						dataCell = SSTUtils.readCellValue(row.getCell(4));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setImportAction(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						/*
						 * add details to mappigList for import
						 */
						if(null==sessionBean.getMappingListToImport() || sessionBean.getMappingListToImport().size()<=0)
						{
							sessionBean.setMappingListToImport(new ArrayList<MasterDataLocaleMappingDetails>());
						}

						sessionBean.getMappingListToImport().add(details);
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
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "readExcelData()", e);
		}
	}

	private void writeMasterDataLocaleMappingExcel(ArrayList<MasterDataLocaleMappingDetails> list, MasterDataLocaleMappingBean sessionBean)
	{
		try
		{
			String path = ApplicationProperties.getProperty("EXPORT_DATA_PHYSICAL_PATH");
			if(null!=path && !"".equals(path))
			{
				if(!path.endsWith("\\"))
				{
					path = path+"\\";
				}
				// add VIN DATA NAME
				String name = "";
				/*
				 * add Current Time Stamp in Format - DDMMYYYY HHMMSS
				 * 
				 * So the final Name will be - EXPORT_DATA_MASTERDATALOCALEMAPPING_NAME.XSLX
				 */
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_MASTERDATALOCALEMAPPING_NAME");
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
				
				Cell modelTypeCell = headerRow.createCell(0);
				modelTypeCell.setCellValue("MARKET");
				Cell codeCell = headerRow.createCell(1);
				codeCell.setCellValue("LOCALE");
				Cell nameCell = headerRow.createCell(2);
				nameCell.setCellValue("MASTER DATA TYPE");
				Cell alternateLocaleCell = headerRow.createCell(3);
				alternateLocaleCell.setCellValue("ALTERNATE LOCALE");
				
				int rowCount=0;
				if(null!=list && list.size()>0)
				{
					MasterDataLocaleMappingDetails details = null;
					for(int i=0;i<list.size();i++)
					{
						details = (MasterDataLocaleMappingDetails)list.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);
						
						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						Cell cell3 = row.createCell(3);
						
						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						cell3.setCellValue("");
						
						if(null!=details.getMarket() && !"".equals(details.getMarket()))
						{
							cell0.setCellValue(details.getMarket());
						}
						if(null!=details.getLocale() && !"".equals(details.getLocale()))
						{
							cell1.setCellValue(details.getLocale());
						}
						if(null!=details.getMasterDataType() && !"".equals(details.getMasterDataType()))
						{
							cell2.setCellValue(details.getMasterDataType());
						}
						if(null!=details.getAlternateLocale() && !"".equals(details.getAlternateLocale()))
						{
							cell3.setCellValue(details.getAlternateLocale());
						}
						
						cell3 = null;
						cell2 = null;
						cell0 = null;
						cell1 = null;
						row = null;
						details = null;
					}
					
					headerRow =  null;
					codeCell = null;
					nameCell=  null;
					modelTypeCell= null;
					alternateLocaleCell=  null;
					
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
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "writeMasterDataLocaleMappingExcel()", e);
		}
	}

	private void decideErrorDisplay(MasterDataLocaleMappingBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_MASTERDATALOCALEMAPPING_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "validateExcelRowData()", e);
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

	private boolean validateExcelRowData(MasterDataLocaleMappingBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		ArrayList<SelectItemDetails> masterDataTypesList = new ArrayList<SelectItemDetails>();
 		if(null!=sessionBean.getMappingListToImport() && sessionBean.getMappingListToImport().size()>0)
		{
			for(int i=0;i<sessionBean.getMappingListToImport().size();i++)
			{
				MasterDataLocaleMappingDetails fieldDetails = (MasterDataLocaleMappingDetails) sessionBean.getMappingListToImport().get(i);
				// EXTRA 1 BECAUSE WHILE READING EXCEL, HEADER ROW WAS SKIPPED
				int rowNo = i+1+1;

				/*
				 * NEW ACTION / MARKER COLUMN - an unrecognised value is a HARD ERROR here,
				 * before any database work, so a mistyped marker can never be silently
				 * treated as create-or-update. Blank stays allowed.
				 */
				if(!ImportActionUtils.isValidAction(fieldDetails.getImportAction()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.excel.unknown.action", String.valueOf(rowNo)));
					errorCount++;
				}
				
				if(null==fieldDetails.getMarket() || "".equals(fieldDetails.getMarket()) ||  
						null==fieldDetails.getLocale() || "".equals(fieldDetails.getLocale()) || 
						null==fieldDetails.getAlternateLocale() || "".equals(fieldDetails.getAlternateLocale()) || 
						null==fieldDetails.getMasterDataType() || "".equals(fieldDetails.getMasterDataType()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					/*
					 * INCOMPLETE DATA FOR MASTER DATA MAPPING AT ROW NO . rowNo
					 */
					String data = msgProps.getProperty("label.masterdatalocalemapping")+","+String.valueOf(rowNo);
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
					data = null;
					id = null;
					// increment errorCount by 1
					errorCount++;
				}

				if(null!=fieldDetails.getMarket() && !"".equals(fieldDetails.getMarket()))
				{
					boolean matchFound = false;
					if(null!=sessionBean.getMarketList() && sessionBean.getMarketList().size()>0)
					{
						for(int a=0;a<sessionBean.getMarketList().size();a++)
						{
							SelectItemDetails si = (SelectItemDetails)sessionBean.getMarketList().get(a);
							if(fieldDetails.getMarket().trim().toLowerCase().equals(si.getValue().trim().toLowerCase()))
							{
								matchFound =true;
								fieldDetails.setMarket(si.getValue());
								break;
							}
							si  =null;
						}
					}
					
					if(matchFound==false)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						//error.excel.improper.lines
						String data = msgProps.getProperty("label.market")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getLocale() && !"".equals(fieldDetails.getLocale()))
				{
					if(fieldDetails.getLocale().length()>10)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.processinglocale")+",10,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getMarket() && !"".equals(fieldDetails.getMarket()) && 
						null!=fieldDetails.getMasterDataType() && !"".equals(fieldDetails.getMasterDataType()))
				{
					boolean matchFound = false;
					masterDataTypesList = getMasterDataTypesList(fieldDetails.getMarket());
					if(null!=masterDataTypesList && masterDataTypesList.size()>0)
					{
						for(int a=0;a<masterDataTypesList.size();a++)
						{
							SelectItemDetails si = (SelectItemDetails)masterDataTypesList.get(a);
							if(fieldDetails.getMasterDataType().trim().toLowerCase().equals(si.getValue().trim().toLowerCase()))
							{
								matchFound =true;
								fieldDetails.setMasterDataType(si.getValue());
								break;
							}
							si  =null;
						}
					}
					masterDataTypesList = null;
					if(matchFound==false)
					{

						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						//error.excel.improper.lines
						String data = msgProps.getProperty("label.masterdatatype")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					
					}
				}
				
				if(null!=fieldDetails.getAlternateLocale() && !"".equals(fieldDetails.getAlternateLocale()))
				{
					if(fieldDetails.getAlternateLocale().length()>10)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.alternatelocale")+",10,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
			}
		}
		else
		{
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.masterdatalocalemapping")));
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

	private boolean validate(MasterDataLocaleMappingDetails fieldDetails, MasterDataLocaleMappingBean sessionBean)
	{
		
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getMarket() || "".equals(fieldDetails.getMarket()) ||
				null==fieldDetails.getLocale() || "".equals(fieldDetails.getLocale()) || 
				null==fieldDetails.getAlternateLocale() || "".equals(fieldDetails.getAlternateLocale()) || 
				null==fieldDetails.getMasterDataType() || "".equals(fieldDetails.getMasterDataType()))
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}
		
		if(null!=fieldDetails.getLocale() && !"".equals(fieldDetails.getLocale()))
		{
			if(fieldDetails.getLocale().length()>10)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.processinglocale"),"10"));
			}
		}
		
		if(null!=fieldDetails.getAlternateLocale() && !"".equals(fieldDetails.getAlternateLocale()))
		{
			if(fieldDetails.getAlternateLocale().length()>10)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.alternatelocale"),"10"));
			}
		}
		
		if(null!=fieldDetails.getMarket() && !"".equals(fieldDetails.getMarket()) &&
				null!=fieldDetails.getLocale() && !"".equals(fieldDetails.getLocale()) && 
				null!=fieldDetails.getAlternateLocale() && !"".equals(fieldDetails.getAlternateLocale()) &&
				null!=fieldDetails.getMasterDataType() && !"".equals(fieldDetails.getMasterDataType()))
		{
			if(null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0)
			{
				for(int a=0;a<sessionBean.getMappingList().size();a++)
				{
					MasterDataLocaleMappingDetails details = (MasterDataLocaleMappingDetails)sessionBean.getMappingList().get(a);
					if(details.getLocale().trim().toLowerCase().equals(fieldDetails.getLocale().trim().toLowerCase()) && 
							details.getAlternateLocale().trim().toLowerCase().equals(fieldDetails.getAlternateLocale().trim().toLowerCase()) && 
							details.getMasterDataType().trim().toLowerCase().equals(fieldDetails.getMasterDataType().trim().toLowerCase()) &&  
							details.getMarket().trim().toLowerCase().equals(fieldDetails.getMarket().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.masterdatalocalemapping")));
						break;
					}
					details=  null;
				}
			}
		}
		
		String messages=errorMessage.toString();
		if(null!=messages && !"".equals(messages))
		{
			sessionBean.setErrorMessage(messages);
			messages=  null;
			errorMessage= null;
			return false;
		}
		messages= null;
		errorMessage = null;
		return true;
	}
	
	private boolean validateUpdate(MasterDataLocaleMappingDetails fieldDetails, MasterDataLocaleMappingBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getMarket() || "".equals(fieldDetails.getMarket()) ||
				null==fieldDetails.getLocale() || "".equals(fieldDetails.getLocale()) || 
						null==fieldDetails.getAlternateLocale() || "".equals(fieldDetails.getAlternateLocale()) || 
				null==fieldDetails.getMasterDataType() || "".equals(fieldDetails.getMasterDataType()))
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}

		if(null!=fieldDetails.getLocale() && !"".equals(fieldDetails.getLocale()))
		{
			if(fieldDetails.getLocale().length()>10)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.processinglocale")+",10,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getAlternateLocale() && !"".equals(fieldDetails.getAlternateLocale()))
		{
			if(fieldDetails.getAlternateLocale().length()>10)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.alternatelocale")+",10,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getMappingId() && fieldDetails.getMappingId()>0 && 
				null!=fieldDetails.getMarket() && !"".equals(fieldDetails.getMarket()) &&
				null!=fieldDetails.getLocale() && !"".equals(fieldDetails.getLocale()) && 
						null!=fieldDetails.getAlternateLocale() && !"".equals(fieldDetails.getAlternateLocale()) && 
				null!=fieldDetails.getMasterDataType() && !"".equals(fieldDetails.getMasterDataType()) &&
				null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0)
		{
			for(int a=0;a<sessionBean.getMappingList().size();a++)
			{
				MasterDataLocaleMappingDetails details = (MasterDataLocaleMappingDetails)sessionBean.getMappingList().get(a);
				if(fieldDetails.getMappingId()!=details.getMappingId()  && 
						details.getLocale().trim().toLowerCase().equals(fieldDetails.getLocale().trim().toLowerCase()) && 
						details.getAlternateLocale().trim().toLowerCase().equals(fieldDetails.getAlternateLocale().trim().toLowerCase()) 
						&& details.getMarket().trim().toLowerCase().equals(fieldDetails.getMarket().trim().toLowerCase())  
						&& details.getMasterDataType().trim().toLowerCase().equals(fieldDetails.getMasterDataType().trim().toLowerCase()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.masterdatalocalemapping")+","+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
					data = null;
					id = null;
					break;
				}
				details=  null;
			}
		}
		

		String messages=errorMessage.toString();
		if(null!=messages && !"".equals(messages))
		{
			sessionBean.setErrorMessage(messages);
			messages=  null;
			errorMessage= null;
			return false;
		}
		messages= null;
		errorMessage = null;
		return true;
	}
	
	
	private void saveMasterDataLocaleMappingDetails(MasterDataLocaleMappingBean sessionBean)
	{
		try
		{
			if(validate(sessionBean.getFieldDetails(), sessionBean))
			{
				/*
				 * call database function
				 * before that set flag as Active
				 */
				sessionBean.getFieldDetails().setFlag(ApplicationProperties.getProperty("flag.value.active"));
				boolean bool = MasterDataLocaleMappingDAO.saveMasterDataLocaleMappingDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveMasterDataLocaleMappingDetails :: MasterDataLocaleMapping Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.masterdatalocalemapping")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setMasterDataTypesList(null);
					sessionBean.setMappingList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					/*
					 * call getLanguageList
					 */
					getMappingList(sessionBean);
				}
				else
				{
					logger.info("saveMasterDataLocaleMappingDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.masterdatalocalemapping")));
				}
			}
			else
			{
				logger.info("saveMasterDataLocaleMappingDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "saveMasterDataLocaleMappingDetails()", e);
		}
	}
	
	private void editMasterDataLocaleMappingDetails(HttpServletRequest request, MasterDataLocaleMappingBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getMappingList() && !"".equals(sessionBean.getMappingList().size()>0))
			{
				for(int a=0;a<sessionBean.getMappingList().size();a++)
				{
					MasterDataLocaleMappingDetails details = (MasterDataLocaleMappingDetails)sessionBean.getMappingList().get(a);
					details.setEditableFlag(false);
				}
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getMappingList() && !"".equals(sessionBean.getMappingList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getMappingList().size();a++)
							{
								MasterDataLocaleMappingDetails details = (MasterDataLocaleMappingDetails)sessionBean.getMappingList().get(a);
								if(rowId.equals(String.valueOf(details.getMappingId())))
								{
									logger.info("editMasterDataLocaleMappingDetails :: Making Row No {"+details.getSrNo()+"} Editable.");
									details.setEditableFlag(true);
									break;
								}
							}
							rowId=  null;
						}
					}
					rows = null;
				}
				// show Update Button
				sessionBean.setShowUpdate(true);
			}
			else
			{
				logger.info("editMasterDataLocaleMappingDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "editMasterDataLocaleMappingDetails()", e);
		}
	}

	private void deleteMasterDataLocaleMappingDetails(HttpServletRequest request, MasterDataLocaleMappingBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				String deleteIds=sessionBean.getSelectedRows();
				if(null!=deleteIds && !"".equals(deleteIds))
				{
					if(deleteIds.endsWith(","))
					{
						deleteIds = deleteIds.substring(0,deleteIds.length()-1);
					}
					boolean bool = MasterDataLocaleMappingDAO.deleteMasterDataLocaleMappingDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.masterdatalocalemapping")));
						/*
						 * call getMappingList
						 */
						getMappingList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.masterdatalocalemapping")));
					}
				}
			}
			else
			{
				logger.info("deleteMasterDataLocaleMappingDetails :: No Row selected for Delete, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "deleteMasterDataLocaleMappingDetails()", e);
		}
	}

	private void activeMasterDataLocaleMappingDetails(HttpServletRequest request, MasterDataLocaleMappingBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				String activeIds=sessionBean.getSelectedRows();
				if(null!=activeIds && !"".equals(activeIds))
				{
					if(activeIds.endsWith(","))
					{
						activeIds = activeIds.substring(0,activeIds.length()-1);
					}
					boolean bool = MasterDataLocaleMappingDAO.activeMasterDataLocaleMappingDetails(activeIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("active.success", msgProps.getProperty("label.masterdatalocalemapping")));
						/*
						 * call getMissionBookList
						 */
						getMappingList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.active", msgProps.getProperty("label.masterdatalocalemapping")));
					}
				}
			}
			else
			{
				logger.info("activeMasterDataLocaleMappingDetails :: No Row selected for Active, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.active");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "activeMasterDataLocaleMappingDetails()", e);
		}
	}
	
	private void updateMasterDataLocaleMappingDetails(HttpServletRequest request, MasterDataLocaleMappingBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getUpdatedRows() && !"".equals(sessionBean.getUpdatedRows()))
			{
				ArrayList<MasterDataLocaleMappingDetails> updateDataList = new ArrayList<MasterDataLocaleMappingDetails>();
				String updatedRows=sessionBean.getUpdatedRows();
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0)
					{
						MasterDataLocaleMappingDetails details = new MasterDataLocaleMappingDetails();
						for(int i=0;i<sessionBean.getMappingList().size();i++)
						{
							details = (MasterDataLocaleMappingDetails)sessionBean.getMappingList().get(i);
							if(details.isEditableFlag()==true)
							{
								// set fields empty 
								details.setMarket("");
								details.setLocale("");
								details.setMasterDataType("");
								details.setAlternateLocale("");
								details.setFlag("");

								/*
								 * fetch the values from request
								 * and set in vinList
								 */
								String code="MDLM_List_Market"+String.valueOf(details.getMappingId());
								String name="MDLM_List_Locale"+String.valueOf(details.getMappingId());
								String modelType="MDLM_List_MasterDataType"+String.valueOf(details.getMappingId());
								String altLocale="MDLM_List_AlternateLocale"+String.valueOf(details.getMappingId());
								String flag="MDLM_List_Flag_"+String.valueOf(details.getMappingId());


								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										if(key.equals(code))
										{
											details.setMarket(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getMarket() && !"".equals(details.getMarket()))
											{
												details.setMarket(details.getMarket().trim());
											}
										}
										else if(key.equals(name))
										{
											details.setLocale(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getLocale() && !"".equals(details.getLocale()))
											{
												details.setLocale(details.getLocale().trim());
											}
										}
										else if(key.equals(modelType))
										{
											details.setMasterDataType(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getMasterDataType() && !"".equals(details.getMasterDataType()))
											{
												details.setMasterDataType(details.getMasterDataType().trim());
											}
										}
										else if(key.equals(altLocale))
										{
											details.setAlternateLocale(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
											if(null!=details.getAlternateLocale() && !"".equals(details.getAlternateLocale()))
											{
												details.setAlternateLocale(details.getAlternateLocale().trim());
											}
										}
										else if(key.equals(flag))
										{
											details.setFlag(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										key = null;
										token= null;
									}
								}

								// set all request params ids to null
								code = null;
								name=null;
								flag= null;
								modelType = null;
							}
							details = null;
						}

						details = new MasterDataLocaleMappingDetails();
						for(int i=0;i<sessionBean.getMappingList().size();i++)
						{
							details = (MasterDataLocaleMappingDetails)sessionBean.getMappingList().get(i);
							if(details.isEditableFlag()==true)
							{
								/*
								 * add to Update List
								 * Before adding validate data for each Row.
								 * validate details Object
								 */
								if(validateUpdate(details, sessionBean))
								{
									/*
									 * add data to updateList
									 */
									updateDataList.add(details);
								}
								else
								{
									// set updateList to null;
									updateDataList=  null;
									break;
								}
							}
							details = null;
						}

						if(null!=updateDataList && updateDataList.size()>0)
						{
							logger.info("updateMasterDataLocaleMappingDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
							/*
							 * Iterate UpdateList and update each row one by one.
							 */
							String errorMessage="";
							String successMessage="";
							
							updateDataList = MasterDataLocaleMappingDAO.updateMasterDataLocaleMappingDetails(updateDataList);
							
							for(int a=0;a<updateDataList.size();a++)
							{
								details = (MasterDataLocaleMappingDetails)updateDataList.get(a);
								if(details.isSaveStatusWhileImport()==true)
								{
									logger.info("updateMasterDataLocaleMappingDetails :: MasterDataLocaleMapping Details updated successfully for Row No :: > " + details.getSrNo());
									successMessage = successMessage+String.valueOf(details.getSrNo())+",";
								}
								else
								{
									logger.info("updateMasterDataLocaleMappingDetails :: Failed to Update MasterDataLocaleMapping Details for Row No :: >  "+ details.getSrNo());
									errorMessage = errorMessage+String.valueOf(details.getSrNo())+",";	
								}
								details=  null;
							}

							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.masterdatalocalemapping"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.masterdatalocalemapping"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateMasterDataLocaleMappingDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setMappingList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getMappingList
								 */
								getMappingList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateMasterDataLocaleMappingDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										details = new MasterDataLocaleMappingDetails();
										for(int a=0;a<sessionBean.getMappingList().size();a++)
										{
											details = (MasterDataLocaleMappingDetails)sessionBean.getMappingList().get(a);
											details.setEditableFlag(false);
											for(int b=0;b<tokens.length;b++)
											{
												if(tokens[b].toString().equals(String.valueOf(details.getSrNo())))
												{
													details.setEditableFlag(true);
													break;
												}
											}
										}
									}
									tokens= null;
								}
							}
							successMessage= null;
							errorMessage= null;
						}
						updateDataList= null;
					}
				}
				updatedRows = null;
				updatedRowsTokens = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "updateMasterDataLocaleMappingDetails()", e);
		}
	}
	
	private void exportMasterDataLocaleMappingDetails(HttpServletRequest request, MasterDataLocaleMappingBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<MasterDataLocaleMappingDetails> exportDataList = new ArrayList<MasterDataLocaleMappingDetails>();
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getMappingList() && !"".equals(sessionBean.getMappingList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getMappingList().size();a++)
							{
								MasterDataLocaleMappingDetails details = (MasterDataLocaleMappingDetails)sessionBean.getMappingList().get(a);
								if(rowId.equals(String.valueOf(details.getMappingId())))
								{
									// add to export List
									exportDataList.add(details);
									break;
								}
							}
							rowId=  null;
						}
					}
					rows = null;
				}
				
				if(null!=exportDataList && exportDataList.size()>0)
				{
					/*
					 * CALL FUNCTION TO GENERATE EXCEL FOR THE SELECTED ROWS
					 */
					writeMasterDataLocaleMappingExcel(exportDataList, sessionBean);
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
				else
				{
					logger.info("exportMasterDataLocaleMappingDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportMasterDataLocaleMappingDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "exportMasterDataLocaleMappingDetails()", e);
		}
	}
	
	/**
	 * Function will perform reset operation
	 * @param sessionBean
	 */
	private void resetMasterDataLocaleMappingOperation(MasterDataLocaleMappingBean sessionBean)
	{
		sessionBean.setMappingList(null);
		sessionBean.setFlagList(null);
		sessionBean.setErrorMessage(null);
		sessionBean.setSuccessMessage(null);
		sessionBean.setSelectedRows(null);
		sessionBean.setShowUpdate(false);
		sessionBean.setDisplayPageLength(null);
		sessionBean.setDisplayPageNo(null);
		sessionBean.setUpdatedRows(null);
		sessionBean.setFieldDetails(new MasterDataLocaleMappingDetails());
		sessionBean.setMasterDataTypesList(null);
		/*
		 * call getMappingList
		 */
		getMappingList(sessionBean);	
	}
	
	private void populateMasterDataTypeForSpecificRow(MasterDataLocaleMappingBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getMappingList() && sessionBean.getMappingList().size()>0 && null!=selectedRowForMasterDataType && !"".equals(selectedRowForMasterDataType))
			{
				MasterDataLocaleMappingDetails details = null;
				for(int a=0;a<sessionBean.getMappingList().size();a++)
				{
					details = (MasterDataLocaleMappingDetails)sessionBean.getMappingList().get(a);
					if(String.valueOf(details.getMappingId()).equals(selectedRowForMasterDataType))
					{
						details.setMasterDataType(null);
						details.setMasterDataTypeLabel(null);
						details.setMasterDataTypeList(new ArrayList<SelectItemDetails>());
						if(null!=details.getMarket() && !"".equals(details.getMarket()))
						{
							details.setMasterDataTypeList(getMasterDataTypesList(details.getMarket()));
						}
						break;
					}
					details = null;
				}
				details = null;
			}
		}
		catch(Exception  e)
		{
			Utilities.printStackTraceToLogs(MasterDataLocaleMapping.class.getName(), "populateMasterDataTypeForSpecificRow()", e);
		}
	}
}