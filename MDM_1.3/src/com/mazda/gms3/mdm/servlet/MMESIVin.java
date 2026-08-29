package com.mazda.gms3.mdm.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.apache.poi.ss.format.CellNumberFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mazda.gms3.mdm.bean.MMESIVinBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.FetchKaptureDataDAO;
import com.mazda.gms3.mdm.dao.MMESIVinDAO;
import java.sql.Connection;

import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.kapture.KaptureCategoryServiceImpl;
import com.mazda.gms3.mdm.kapture.KaptureContentServiceImpl;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.dao.SIVINBatchTransactionDAO;
import com.mazda.gms3.mdm.im.impl.MMESIVINBatchProcessingImpl;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.CountryLocaleComparator;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.ManualLanguageComparator;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.IMCategoryDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SIVINScheduleDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class MMESIVin
 */
public class MMESIVin extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static Logger logger = LogManager.getLogger(MMESIVin.class);
	static String wslId=null;
	static MessageProperties msgProps= null;
	String reportName=null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_MME_SIVIN_RANGE;
	private int maximumVISRangeLength=6;
	
	private int authTokenGenerationCount=0;
	private int authTokenUserGenerationCount=0;
	
	private String catLoadLimit=ApplicationProperties.getProperty("mme.cat.create.limit");
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MMESIVin() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
			
			/*
			 * CHECK HERE IF WSL ID IS FROM GMS3_INTERNAL_TEAM_USERS
			 * THEN SET WSL AS OKADMIN ELSE USE WSL AS IT IS
			 */
			if(null!=wslId && !"".equals(wslId))
			{
//				checkForWsl();
			}
			
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			/*
			 * Initialize bean
			 */
			MMESIVinBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setCountryLocaleList(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setLanguageList(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setActionClicked(null);
			sessionBean.setSiNumber(null);
			sessionBean.setWmiId(null);
			sessionBean.setWmiList(null);
			sessionBean.setModelId(null);
			sessionBean.setCarlineCode(null);
			sessionBean.setCarlineNameEng(null);
			sessionBean.setModelsList(null);
			sessionBean.setVdsId(null);
			sessionBean.setHiddenVDSId(null);
			sessionBean.setShowAddNewVDSField(false);
			sessionBean.setAddNewVDSId(null);
			sessionBean.setVdsList(null);
//			sessionBean.setVinStartRange("000000");
//			sessionBean.setVinEndRange("ZZZZZZ");
			
			maximumVISRangeLength = 6;
			setDefaultVINRanges(sessionBean);
			
			sessionBean.setSuccessMessage(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPreviewPageLength(null);
			sessionBean.setDisplayPreviewPageNo(null);
			sessionBean.setSelectedPreviewRows(null);
			sessionBean.setInfoMessage(null);
			sessionBean.setDocumentsList(null);
			sessionBean.setTempVinsList(null);
			sessionBean.setVinToImportList(null);
			sessionBean.setReportViewPath(null);
			
			sessionBean.setEngineTypeIds(null);
			sessionBean.setMissionTypeIds(null);
			sessionBean.setHiddenEngineTypeId(null);
			sessionBean.setHiddenMissionTypeId(null);
			sessionBean.setEngineTypeList(null);
			sessionBean.setMissionTypeList(null);
			
			sessionBean.setScheduleName(null);
			sessionBean.setShowTranslationOperationsControl(false);
			sessionBean.setYesNoList(null);
			sessionBean.setTranslationsUpdate(ApplicationProperties.getProperty("value.esicategory.flag.yes"));
			
			/*
			 * call function to load all the Country Locale Data
			 */
			getCountryLocaleList(sessionBean, request);
			
			/*
			 * call function to get YesNoList
			 */
			getYesNoList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/mmesivin.jsp");
				rs.forward(request, response);
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
			/*
			 * CHECK HERE IF WSL ID IS FROM GMS3_INTERNAL_TEAM_USERS
			 * THEN SET WSL AS OKADMIN ELSE USE WSL AS IT IS
			 */
			if(null!=wslId && !"".equals(wslId))
			{
//				checkForWsl();
			}
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			/*
			 * Initialize bean
			 */
			MMESIVinBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			sessionBean.setErrorMessage("");
			sessionBean.setSuccessMessage("");
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setDisplayPreviewPageLength(null);
			sessionBean.setDisplayPreviewPageNo(null);
			sessionBean.setInfoMessage("");
			sessionBean.setReportViewPath(null);
			sessionBean.setScheduleName(null);
			
			/*
			 * READ PARAMETERS FROM REQUEST
			 */
			readParamsFromRequest(sessionBean, request);
		
			/*
			 * call function to set Selected Rows
			 */
			setSelectedTempRows(sessionBean, request);
			
			/*
			 * call function to identify whether to show translation operations control
			 */
			identifyToShowTranslationOperationControls(sessionBean);
			
			if(null!=sessionBean.getActionClicked() && !"".equals(sessionBean.getActionClicked()))
			{
				if(sessionBean.getActionClicked().equals("COUNTRY_SELECTION"))
				{
					sessionBean.setManualLanguageId(null);
					sessionBean.setWmiId(null);
					sessionBean.setModelId(null);
					sessionBean.setCarlineCode(null);
					sessionBean.setCarlineNameEng(null);
					sessionBean.setCarlineNameReg(null);
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					sessionBean.setShowAddNewVDSField(false);
					sessionBean.setAddNewVDSId(null);
					sessionBean.setTempVinsList(null);
					sessionBean.setEngineTypeIds(null);
					sessionBean.setMissionTypeIds(null);
					sessionBean.setHiddenEngineTypeId(null);
					sessionBean.setHiddenMissionTypeId(null);
					
					/*
					 * call function to load all the Manual language data
					 */
					getLanguageList(sessionBean, request);
					/*
					 * call function to load wmi list
					 */
					getWMIList(sessionBean);
					/*
					 * call function to load model list
					 */
					getModelList(sessionBean);
					
					/*
					 * call function to identify max vis range length and set vis range accordingly
					 */
					setDefaultVINRanges(sessionBean);
					
					/*
					 * call function to load VDS list
					 */
					getVDSList(sessionBean);
					/*
					 * call function to load Engine Type list
					 */
					getEngineTypeList(sessionBean);
					/*
					 * call function to load Mission Type list
					 */
					getMissionTypeList(sessionBean);
					
				}
				
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					sessionBean.setWmiId(null);
					sessionBean.setModelId(null);
					sessionBean.setCarlineCode(null);
					sessionBean.setCarlineNameEng(null);
					sessionBean.setCarlineNameReg(null);
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					sessionBean.setShowAddNewVDSField(false);
					sessionBean.setAddNewVDSId(null);
					sessionBean.setTempVinsList(null);
					sessionBean.setEngineTypeIds(null);
					sessionBean.setMissionTypeIds(null);
					sessionBean.setHiddenEngineTypeId(null);
					sessionBean.setHiddenMissionTypeId(null);
					// set to default VALUE
					sessionBean.setTranslationsUpdate(ApplicationProperties.getProperty("value.esicategory.flag.yes"));
					
					/*
					 * call function to load wmi list
					 */
					getWMIList(sessionBean);
					/*
					 * call function to load model list
					 */
					getModelList(sessionBean);
					
					/*
					 * call function to identify max vis range length and set vis range accordingly
					 */
					setDefaultVINRanges(sessionBean);
					/*
					 * call function to load VDS list
					 */
					getVDSList(sessionBean);
					/*
					 * call function to load Engine Type list
					 */
					getEngineTypeList(sessionBean);
					/*
					 * call function to load Mission Type list
					 */
					getMissionTypeList(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("WMI_SELECTION"))
				{
					sessionBean.setModelId(null);
					sessionBean.setCarlineCode(null);
					sessionBean.setCarlineNameEng(null);
					sessionBean.setCarlineNameReg(null);
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					sessionBean.setShowAddNewVDSField(false);
					sessionBean.setAddNewVDSId(null);
					sessionBean.setEngineTypeIds(null);
					sessionBean.setMissionTypeIds(null);
					sessionBean.setHiddenEngineTypeId(null);
					sessionBean.setHiddenMissionTypeId(null);
					/*
					 * call function to load model list
					 */
					getModelList(sessionBean);
					/*
					 * call function to identify max vis range length and set vis range accordingly
					 */
					setDefaultVINRanges(sessionBean);
					/*
					 * call function to load VDS list
					 */
					getVDSList(sessionBean);
					/*
					 * call function to load Engine Type list
					 */
					getEngineTypeList(sessionBean);
					/*
					 * call function to load Mission Type list
					 */
					getMissionTypeList(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("MODEL_SELECTION"))
				{
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					sessionBean.setShowAddNewVDSField(false);
					sessionBean.setAddNewVDSId(null);
					sessionBean.setEngineTypeIds(null);
					sessionBean.setMissionTypeIds(null);
					sessionBean.setHiddenEngineTypeId(null);
					sessionBean.setHiddenMissionTypeId(null);
					/*
					 * call function to load VDS list
					 */
					getVDSList(sessionBean);
					/*
					 * call function to load Engine Type list
					 */
					getEngineTypeList(sessionBean);
					/*
					 * call function to load Mission Type list
					 */
					getMissionTypeList(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("ENGINETYPE_SELECTION"))
				{
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					sessionBean.setShowAddNewVDSField(false);
					sessionBean.setAddNewVDSId(null);
					sessionBean.setMissionTypeIds(null);
					sessionBean.setHiddenMissionTypeId(null);
					
					/*
					 * call function to load VDS list
					 */
					getVDSList(sessionBean);
					/*
					 * call function to load Mission Type list
					 */
					getMissionTypeList(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("MISSSIONTYPE_SELECTION"))
				{
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					sessionBean.setShowAddNewVDSField(false);
					sessionBean.setAddNewVDSId(null);
					
					/*
					 * call function to load VDS list
					 */
					getVDSList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("VDS_SELECTION"))
				{
					/*
					 * check if SEL VDS CONTAINS - ADDNEW
					 * SET SHOW ADD NEW VDS FIELD TO TRUE
					 */
					sessionBean.setShowAddNewVDSField(false);
					if(null!=sessionBean.getVdsId() && sessionBean.getVdsId().length>0)
					{
						for(int a=0;a<sessionBean.getVdsId().length;a++)
						{
							if(null!=sessionBean.getVdsId()[a] && sessionBean.getVdsId()[a].trim().toLowerCase().equals("addnew"))
							{
								// set show field flag to true
								sessionBean.setShowAddNewVDSField(true);
								break;
							}
						}
					}
				}
				
				
				else if(sessionBean.getActionClicked().equals("RESET"))
				{
					/*
					 * call function to reset the complete form
					 */
					resetForm(sessionBean, request);
				}
				
				
				else if(sessionBean.getActionClicked().equals("RESET_VIN"))
				{
					/*
					 * only clean the add vin block
					 */
					sessionBean.setWmiId(null);
					sessionBean.setModelId(null);
					sessionBean.setCarlineCode(null);
					sessionBean.setCarlineNameEng(null);
					sessionBean.setCarlineNameReg(null);
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					sessionBean.setShowAddNewVDSField(false);
					sessionBean.setAddNewVDSId(null);
//					sessionBean.setVinStartRange("000000");
//					sessionBean.setVinEndRange("ZZZZZZ");
					
					maximumVISRangeLength = 6;
					setDefaultVINRanges(sessionBean);
					
					sessionBean.setTempVinsList(null);
					sessionBean.setEngineTypeIds(null);
					sessionBean.setMissionTypeIds(null);
					sessionBean.setHiddenEngineTypeId(null);
					sessionBean.setHiddenMissionTypeId(null);
					/*
					 * call getModels List function
					 */
					getModelList(sessionBean);
					/*
					 * call VDS List
					 */
					getVDSList(sessionBean);
					/*
					 * call function to load Engine Type list
					 */
					getEngineTypeList(sessionBean);
					/*
					 * call function to load Mission Type list
					 */
					getMissionTypeList(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("ADD_VIN"))
				{
					/*
					 * perform Add VIN Operation for searched document.
					 */
					addVinOperation(sessionBean, wslId);
				}
				
				else if(sessionBean.getActionClicked().equals("PERMANENT_ADD_VIN"))
				{
					/*
					 * perform Permanent Add VIN Operation for searched document.
					 */
					mapVinOperation(sessionBean, wslId, request);
				}
				
				else if(sessionBean.getActionClicked().equals("SEARCH"))
				{
					/*
					 * perform document fetch operation.
					 */
					searchOperation(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("REMOVE_VIN"))
				{
					/*
					 * perform Remove VIN Operation for selected document.
					 */
					removeVIN(request, sessionBean, null);
				}
				
				else if(sessionBean.getActionClicked().equals("REMOVE_VIN_FROM_TRANSLATIONS_TOO"))
				{
					/*
					 * perform Remove VIN Operation for selected document along with its translations.
					 */
					removeVIN(request, sessionBean, "Yes");
				}
				
				else if(sessionBean.getActionClicked().equals("CLEAR_SEARCH"))
				{
					/*
					 * only clean the SI NUMBER & DOCUMENTS LIST
					 */
					sessionBean.setSiNumber(null);
					sessionBean.setDocumentsList(new ArrayList<SIVinDetails>());
					sessionBean.setSelectedRows(null);
				}
				
				else if(sessionBean.getActionClicked().equals("EXPORT"))
				{
					/*
					 * Call Export Function.
					 */
					exportMMESIVinDetails(sessionBean);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/mmesivin.jsp");
				rs.forward(request, response);
			}
		}
		
		if(null==wslId)
		{
			wslId ="";
		}
		logger.info("**************** MME SI VIN Range :: Total Auth Tokens Generated for Admin are :: > "+ authTokenGenerationCount);
		logger.info("**************** MME SI VIN Range :: Total Auth Tokens Generated for {"+wslId+"} are :: > "+ authTokenUserGenerationCount);
	}

	private MMESIVinBean getSessionBean(HttpServletRequest request) 
	{
		MMESIVinBean sessionBean = null;
		if (null != request.getSession().getAttribute("mmeSiVinBean") && !"".equals(request.getSession().getAttribute("mmeSiVinBean"))) 
		{
			sessionBean = (MMESIVinBean) request.getSession().getAttribute("mmeSiVinBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new MMESIVinBean();
			request.getSession().setAttribute("mmeSiVinBean", sessionBean);
		}
		return sessionBean;
	}

	private void getYesNoList(MMESIVinBean sessionBean)
	{
		sessionBean.setYesNoList(new ArrayList<SelectItemDetails>());
		SelectItemDetails si = new SelectItemDetails();
		si.setValue(ApplicationProperties.getProperty("value.esicategory.flag.yes"));
		si.setLabel(msgProps.getProperty("label.yes"));
		sessionBean.getYesNoList().add(si);
		si = null;
		
		si = new SelectItemDetails();
		si.setValue(ApplicationProperties.getProperty("value.esicategory.flag.no"));
		si.setLabel(msgProps.getProperty("label.no"));
		sessionBean.getYesNoList().add(si);
		si = null;
	}
	
	private void getCountryLocaleList(MMESIVinBean sessionBean, HttpServletRequest request)
	{
		try
		{

			UserAccessBean userSessionBean = getUserSessionBean(request);
			sessionBean.setCountryLocaleList(new ArrayList<CountryLocaleDetails>());
			ArrayList<CountryLocaleDetails> list = new ArrayList<CountryLocaleDetails>();
			list = CountryLocaleDAO.getCountryLocaleDetailsListForCombo();
			if(null!=list && list.size()>0)
			{
				if(userSessionBean.isSuperAdminUser()==true)
				{
					/*
					 * ALSO CHECK BEFORE SETTING FINAL COUNTRY LIST
					 * ONLY MME COUNTRIES ALLOWED HERE
					 */
					ArrayList<CountryLocaleDetails> actList = new ArrayList<CountryLocaleDetails>();
					String countriesToBeAvoided=ApplicationProperties.getProperty("mnao.countries.codes").trim();
					if(!countriesToBeAvoided.endsWith(","))
					{
						countriesToBeAvoided+=",";
					}
					countriesToBeAvoided+= ApplicationProperties.getProperty("mc.countries.codes").trim();
					
					String[] tokens=countriesToBeAvoided.split(",");
					for(CountryLocaleDetails clD : list)
					{
						boolean addToList = true;
						for(int r=0;r<tokens.length;r++)
						{
							if(clD.getCountryLocaleDesc().trim().toLowerCase().equals(tokens[r].trim().toLowerCase()))
							{
								// country to be avoided - do not add
								addToList=false;
								break;
							}
						}
						if(addToList==true)
						{
							actList.add(clD);
						}
					}
					
					
					if(null!=actList && actList.size()>0)
					{
						CountryLocaleComparator countryLocaleComparator = new CountryLocaleComparator();
						Collections.sort(actList,countryLocaleComparator);
						sessionBean.setCountryLocaleList(actList);
						countryLocaleComparator = null;
					}
					actList=  null;
					tokens = null;
					countriesToBeAvoided = null;
				}
				else if(userSessionBean.isSuperAdminUser()==false)
				{
					ArrayList<CountryLocaleDetails> finalCountryList = new ArrayList<CountryLocaleDetails>();
					// NOW CHECK FOR USER LOCALES
					if(null!=userSessionBean.getUserLocalesList() && userSessionBean.getUserLocalesList().size()>0)
					{
						for(String contentLocale : userSessionBean.getUserLocalesList())
						{
							if(contentLocale.lastIndexOf("-")!=-1)
							{
								String cCode = contentLocale.substring(contentLocale.lastIndexOf("-")+1, contentLocale.length());
								if(null!=cCode && !"".equals(cCode))
								{
									for(CountryLocaleDetails cldDetails : list)
									{
										boolean addToList = false;
										if(cCode.trim().toLowerCase().equals(cldDetails.getCountryLocaleDesc().trim().toLowerCase()))
										{
											// addToList
											addToList= true;
										}

										if(addToList==true)
										{
											boolean proceed = true;
											if(null!=finalCountryList && finalCountryList.size()>0)
											{
												for(CountryLocaleDetails existDetails : finalCountryList)
												{
													if(existDetails.getCountryLocaleId()==cldDetails.getCountryLocaleId())
													{
														// alreadyAdded - proceed - false
														proceed = false;
														break;
													}
												}
											}

											if(proceed==true)
											{
												finalCountryList.add(cldDetails);
											}
										}
										cldDetails = null;
									}
								}
								cCode = null;
							}
							contentLocale = null;
						}
					}

					if(null!=finalCountryList && finalCountryList.size()>0)
					{
						/*
						 * ALSO CHECK BEFORE SETTING FINAL COUNTRY LIST
						 * ONLY MME COUNTRIES ALLOWED HERE
						 */
						ArrayList<CountryLocaleDetails> actList = new ArrayList<CountryLocaleDetails>();
						String countriesToBeAvoided=ApplicationProperties.getProperty("mnao.countries.codes").trim();
						if(!countriesToBeAvoided.endsWith(","))
						{
							countriesToBeAvoided+=",";
						}
						countriesToBeAvoided+= ApplicationProperties.getProperty("mc.countries.codes").trim();
						
						String[] tokens=countriesToBeAvoided.split(",");
						for(CountryLocaleDetails clD : finalCountryList)
						{
							boolean addToList = true;
							for(int r=0;r<tokens.length;r++)
							{
								if(clD.getCountryLocaleDesc().trim().toLowerCase().equals(tokens[r].trim().toLowerCase()))
								{
									// country to be avoided - do not add
									addToList=false;
									break;
								}
							}
							if(addToList==true)
							{
								actList.add(clD);
							}
						}
						
						if(null!=actList && actList.size()>0)
						{
							CountryLocaleComparator countryLocaleComparator = new CountryLocaleComparator();
							Collections.sort(actList,countryLocaleComparator);
							sessionBean.setCountryLocaleList(actList);
							countryLocaleComparator = null;
						}
						actList=  null;
						tokens=null;
						countriesToBeAvoided = null;
					}
					finalCountryList=  null;
				}
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private void getLanguageList(MMESIVinBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setLanguageList(new ArrayList<ManualLanguageDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()))
			{
				ArrayList<ManualLanguageDetails> list = new ArrayList<ManualLanguageDetails>();
				list = ManualLanguageDAO.getManualLanguageDetailsListForCombo(sessionBean.getCountryLocaleId());
				if(null!=list && list.size()>0)
				{
					UserAccessBean userSessionBean = getUserSessionBean(request);
					if(userSessionBean.isSuperAdminUser()==true)
					{

						/*
						 * ALSO CHECK BEFORE SETTING FINAL LANGUAGE LIST
						 * ONLY MME LANGUAGES ALLOWED HERE
						 */
						ArrayList<ManualLanguageDetails> actList = new ArrayList<ManualLanguageDetails>();
						String languagesToBeSkipped=ApplicationProperties.getProperty("mnao.countries.locales.codes").trim();
						if(!languagesToBeSkipped.endsWith(","))
						{
							languagesToBeSkipped+=",";
						}
						languagesToBeSkipped+= ApplicationProperties.getProperty("mc.countries.locales.codes").trim();
						String[] tokens = languagesToBeSkipped.split(",");
						for(ManualLanguageDetails mlD : list)
						{
							boolean addToList = true;
							for(int r=0;r<tokens.length;r++)
							{
								String token = tokens[r];
								token = token.replace("_", "-");
								if(mlD.getManualLanguageName().trim().toLowerCase().equals(token.trim().toLowerCase()))
								{
									// do no add
									addToList = false;
									break;
								}
								token=null;
							}
							if(addToList==true)
							{
								actList.add(mlD);
							}
						}
						
						if(null!=actList && actList.size()>0)
						{
							ManualLanguageComparator manualLanguageComparator = new ManualLanguageComparator();
							Collections.sort(actList,manualLanguageComparator);
							sessionBean.setLanguageList(actList);
							manualLanguageComparator = null;
						}
						actList=  null;
						languagesToBeSkipped = null;
						tokens = null;
					
					}
					else if(userSessionBean.isSuperAdminUser()==false)
					{
						/*
						 * ITERATE LIST AND CHECK FOR THE LOCALE CODE WHETHER EXISTS IN USER'S DEFAULT LOCALE AND CONTENT LOCALE OR NOT
						 * IF EXISTS, THEN ONLY PROCEED. ELSE SKIP
						 */
						ArrayList<ManualLanguageDetails> finalLocaleList = new ArrayList<ManualLanguageDetails>();
						// CHECK WITH USER LOCALES
						if(null!=userSessionBean.getUserLocalesList() && userSessionBean.getUserLocalesList().size()>0)
						{
							for(String contentLocale : userSessionBean.getUserLocalesList())
							{
								if(null!=contentLocale && !"".equals(contentLocale))
								{
									for(ManualLanguageDetails mldDetails : list)
									{
										boolean addToList = false;
										if(contentLocale.trim().toLowerCase().equals(mldDetails.getManualLanguageName().trim().toLowerCase()))
										{
											// addToList
											addToList= true;
										}

										if(addToList==true)
										{
											boolean proceed = true;
											if(null!=finalLocaleList && finalLocaleList.size()>0)
											{
												for(ManualLanguageDetails existDetails : finalLocaleList)
												{
													if(existDetails.getManualLanguageId()==mldDetails.getManualLanguageId())
													{
														// alreadyAdded - proceed - false
														proceed = false;
														break;
													}
												}
											}

											if(proceed==true)
											{
												finalLocaleList.add(mldDetails);
											}
										}
										mldDetails = null;
									}
								}
								contentLocale = null;
							}
						}
						
						if(null!=finalLocaleList && finalLocaleList.size()>0)
						{
							/*
							 * ALSO CHECK BEFORE SETTING FINAL LANGUAGE LIST
							 * ONLY MME LANGUAGES ALLOWED HERE
							 */
							ArrayList<ManualLanguageDetails> actList = new ArrayList<ManualLanguageDetails>();
							String languagesToBeSkipped=ApplicationProperties.getProperty("mnao.countries.locales.codes").trim();
							if(!languagesToBeSkipped.endsWith(","))
							{
								languagesToBeSkipped+=",";
							}
							languagesToBeSkipped+= ApplicationProperties.getProperty("mc.countries.locales.codes").trim();
							String[] tokens = languagesToBeSkipped.split(",");
							for(ManualLanguageDetails mlD : finalLocaleList)
							{
								boolean addToList = true;
								for(int r=0;r<tokens.length;r++)
								{
									String token = tokens[r];
									token = token.replace("_", "-");
									if(mlD.getManualLanguageName().trim().toLowerCase().equals(token.trim().toLowerCase()))
									{
										// do no add
										addToList = false;
										break;
									}
									token=null;
								}
								if(addToList==true)
								{
									actList.add(mlD);
								}
							}
							if(null!=actList && actList.size()>0)
							{
								ManualLanguageComparator manualLanguageComparator = new ManualLanguageComparator();
								Collections.sort(actList,manualLanguageComparator);
								sessionBean.setLanguageList(actList);
								manualLanguageComparator = null;
							}
							actList=  null;
							languagesToBeSkipped = null;
							tokens = null; 
						}
						finalLocaleList=  null;
					}
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "getLanguageList()", e);
		}
	}
	
	private static void getWMIList(MMESIVinBean sessionBean)
	{
		try
		{
			sessionBean.setWmiList(new ArrayList<SelectItemDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId())
					&& null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<SelectItemDetails> list = new ArrayList<SelectItemDetails>();
//				list = MMESIVinDAO.getWMIList(sessionBean.getCountryLocaleId(), sessionBean.getManualLanguageId());
				list = MMESIVinDAO.getWMIList();
				if(null!=list && list.size()>0)
				{
					sessionBean.setWmiList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "getWMIList()", e);
		}
	}
	
	private static void getModelList(MMESIVinBean sessionBean)
	{
		try
		{
			sessionBean.setModelsList(new ArrayList<CarlineDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId())
					&& null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()) &&
					 null!=sessionBean.getWmiId() && !"".equals(sessionBean.getWmiId()))
			{
				ArrayList<CarlineDetails> list = new ArrayList<CarlineDetails>();
//				list = MMESIVinDAO.getModelsList(sessionBean.getCountryLocaleId(), sessionBean.getManualLanguageId(), sessionBean.getWmiId());
				list = MMESIVinDAO.getModelsList(sessionBean.getWmiId());
				if(null!=list && list.size()>0)
				{
					sessionBean.setModelsList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "getModelList()", e);
		}
	}
	
	private static void getVDSList(MMESIVinBean sessionBean)
	{
		try
		{
			sessionBean.setVdsList(new ArrayList<SelectItemDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId())
					&& null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()) &&
					 null!=sessionBean.getWmiId() && !"".equals(sessionBean.getWmiId()) && 
					 null!=sessionBean.getModelId() && !"".equals(sessionBean.getModelId()) && 
					 null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) && 
					 null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng())
					 && null!=sessionBean.getEngineTypeIds() && sessionBean.getEngineTypeIds().length>0 &&
					 null!=sessionBean.getMissionTypeIds() && sessionBean.getMissionTypeIds().length>0)
			{
				// NO CARLINE ENG NAME
				ArrayList<SelectItemDetails> list = MMESIVinDAO.getVDSList(sessionBean.getWmiId(), sessionBean.getCarlineCode(), sessionBean.getCarlineNameEng(), sessionBean.getEngineTypeIds(), sessionBean.getMissionTypeIds());
				/*
				 * add ADD NEW OPTION in VDS LIST
				 * DATE 1 JUNE 2019
				 */
				SelectItemDetails si  =new SelectItemDetails();
				si.setLabel("ADDNEW");
				si.setValue("ADDNEW");
				sessionBean.getVdsList().add(si);
				si = null;
				// add all other items
				if(null!=list && list.size()>0)
				{
					/*
					 * REMOVE ALL OPTION FROM VDS LIST -
					 * FEED BACK ON MME UAT 
					 * 17 JULY 2018
					 */
//					SelectItemDetails si = new SelectItemDetails();
//					si.setLabel(msgProps.getProperty("label.all"));
//					si.setValue(ApplicationProperties.getProperty("check.all.label"));
//					sessionBean.getVdsList().add(si);
//					si = null;
					// add Other Items
					sessionBean.getVdsList().addAll(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "getVDSList()", e);
		}
	}
	
	private static void getEngineTypeList(MMESIVinBean sessionBean)
	{
		try
		{
			sessionBean.setEngineTypeList(new ArrayList<SelectItemDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId())
					&& null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()) &&
					null!=sessionBean.getWmiId() && !"".equals(sessionBean.getWmiId()) && 
					null!=sessionBean.getModelId() && !"".equals(sessionBean.getModelId()) && 
					null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) && 
					null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
			{
				ArrayList<SelectItemDetails> list = MMESIVinDAO.getEngineTypeList(sessionBean.getWmiId(), sessionBean.getCarlineCode() , sessionBean.getCarlineNameEng());
				if(null!=list && list.size()>0)
				{
					SelectItemDetails si = new SelectItemDetails();
					si.setLabel(msgProps.getProperty("label.all"));
					si.setValue(ApplicationProperties.getProperty("check.all.label"));
					sessionBean.getEngineTypeList().add(si);
					si = null;
					sessionBean.getEngineTypeList().addAll(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "getEngineTypeList()", e);
		}
	}
	
	private static void getMissionTypeList(MMESIVinBean sessionBean)
	{
		try
		{
			sessionBean.setMissionTypeList(new ArrayList<SelectItemDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId())
					&& null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()) &&
					null!=sessionBean.getWmiId() && !"".equals(sessionBean.getWmiId()) && 
					null!=sessionBean.getModelId() && !"".equals(sessionBean.getModelId()) && 
							null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) && 
							null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng())  
					 && null!=sessionBean.getEngineTypeIds() && sessionBean.getEngineTypeIds().length>0)
			{
				ArrayList<SelectItemDetails> list = MMESIVinDAO.getMissionTypeList(sessionBean.getWmiId(), sessionBean.getCarlineCode(),sessionBean.getCarlineNameEng(), sessionBean.getEngineTypeIds());
				if(null!=list && list.size()>0)
				{
					SelectItemDetails si = new SelectItemDetails();
					si.setLabel(msgProps.getProperty("label.all"));
					si.setValue(ApplicationProperties.getProperty("check.all.label"));
					sessionBean.getMissionTypeList().add(si);
					si = null;
					sessionBean.getMissionTypeList().addAll(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "getMissionTypeList()", e);
		}
	}
		
		
	private  void readParamsFromRequest(MMESIVinBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setSiNumber(null);
			sessionBean.setWmiId(null);
			sessionBean.setModelId(null);
			sessionBean.setCarlineCode(null);
			sessionBean.setCarlineNameEng(null);
			sessionBean.setCarlineNameReg(null);
			sessionBean.setVdsId(null);
			sessionBean.setHiddenVDSId(null);
			sessionBean.setAddNewVDSId(null);
			sessionBean.setVinStartRange(null);
			sessionBean.setVinEndRange(null);
			sessionBean.setActionClicked(null);
			sessionBean.setSelectedPreviewRows(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setEngineTypeIds(null);
			sessionBean.setMissionTypeIds(null);
			sessionBean.setHiddenEngineTypeId(null);
			sessionBean.setHiddenMissionTypeId(null);
//			sessionBean.setTranslationsUpdate(null);
			
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
						if (fieldName.equals("MMESIVIN_DataTabel_displayPageNo")) 
						{
							// set the value in sessionBean.setDisplayPageNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDisplayPageNo(value);
							}
							// set value to null
							value = null;
						}


						if(fieldName.equals("MMESIVIN_DataTabel_displayPageLen"))
						{
							// set the value in sessionBean.setDisplayPageLength
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDisplayPageLength(value);
							}
							// set value to null
							value = null;
						}

						/*
						 * set displayPageNo and displayPageLenght
						 */
						if (fieldName.equals("MMESIVIN_Preview_DataTabel_displayPageNo")) 
						{
							// set the value in sessionBean.setDisplayPreviewPageNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDisplayPreviewPageNo(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("MMESIVIN_Preview_DataTabel_displayPageLen")) 
						{
							// set the value in sessionBean.displayPageLenght
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setDisplayPreviewPageLength(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("MMESIVIN_ActionClicked")) 
						{
							// set the value in sessionBean.setActionClicked
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setActionClicked(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("MMESIVIN_CountryLocale_Code")) 
						{
							// set the value in sessionBean.setCountryLocaleId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setCountryLocaleId(value);
							}
							// set value to null
							value = null;
						}


						if (fieldName.equals("MMESIVIN_Lang_Code")) 
						{
							// set the value in sessionBean.setManualLanguageId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setManualLanguageId(value);
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("MMESIVIN_TranslationUpdate"))
						{
							// set the value in sessionBean.setTranslationsUpdate
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setTranslationsUpdate(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("MMESIVIN_SI_Number")) 
						{
							// set the value in sessionBean.setSiNumber
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSiNumber(value.trim().toUpperCase());
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("MMESIVIN_WMICode")) 
						{
							// set the value in sessionBean.setWmiId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setWmiId(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("MMESIVIN_Model")) 
						{
							// set the value in sessionBean.setModelId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setModelId(value);
								if(null!=sessionBean.getModelsList() && sessionBean.getModelsList().size()>0)
								{
									for(int a=0;a<sessionBean.getModelsList().size();a++)
									{
										CarlineDetails cDetails = (CarlineDetails)sessionBean.getModelsList().get(a);
										if(cDetails.getCarlineIdForCombo().trim().toLowerCase().equals(sessionBean.getModelId().trim().toLowerCase()))
										{
											sessionBean.setCarlineCode(cDetails.getCarlineCode());
											sessionBean.setCarlineNameEng(cDetails.getCarlineNameEng());
											sessionBean.setCarlineNameReg(cDetails.getCarlineNameReg());
											break;
										}
									}
								}
							}
							// set value to null
							value = null;
						}

						if(fieldName.equals("MMESIVIN_EngineTypeSelectedValues"))
						{
							// set the value in sessionBean.setEngineTypeId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								String selEng = value;
								if(selEng.endsWith(","))
								{
									selEng = selEng.substring(0, selEng.length()-1);
								}
								// set in sessionBean.setHiddenEngineTypeId();
								sessionBean.setHiddenEngineTypeId(selEng);
								
								String[] tok = selEng.split(",");
								if(null!=tok && tok.length>0)
								{
									// set in sessionBean.setEngineTypeIds()
									sessionBean.setEngineTypeIds(tok);
								}
								tok = null;
								selEng = null;
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("MMESIVIN_MissionTypeSelectedValues"))
						{
							// set the value in sessionBean.setMissionTypeId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								String selMiss = value;
								if(selMiss.endsWith(","))
								{
									selMiss = selMiss.substring(0, selMiss.length()-1);
								}
								// set in sessionBean.setHiddenEngineTypeId();
								sessionBean.setHiddenMissionTypeId(selMiss);
								
								String[] tok = selMiss.split(",");
								if(null!=tok && tok.length>0)
								{
									// set in sessionBean.setMissionTypeIds()
									sessionBean.setMissionTypeIds(tok);
								}
								tok = null;
								selMiss = null;
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("MMESIVIN_VDSSelectedValues"))
						{
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								String selVDS = value;
								if(selVDS.endsWith(","))
								{
									selVDS = selVDS.substring(0, selVDS.length()-1);
								}
								// set in sessionBean.setHiddenVDSId();
								sessionBean.setHiddenVDSId(selVDS);
								
								String[] tok = selVDS.split(",");
								if(null!=tok && tok.length>0)
								{
									// set in sessionBean.setVdsId()
									sessionBean.setVdsId(tok);
								}
								tok = null;
								selVDS = null;
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("MMESIVIN_NEWVDSCode")) 
						{
							// set the value in sessionBean.setVinStartRange
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddNewVDSId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("MMESIVIN_VISStartRange")) 
						{
							// set the value in sessionBean.setVinStartRange
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setVinStartRange(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("MMESIVIN_VISEndRange")) 
						{
							// set the value in sessionBean.setVinEndRange
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setVinEndRange(value);
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("MMESIVIN_Preview_SelectedRows"))
						{
							// set the value in sessionBean.setSelectedPreviewRows
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSelectedPreviewRows(value);
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("MMESIVIN_SelectedRows"))
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
											if(validateJobProcessing(sessionBean))
											{
												/*
												 * call function to operate on uploaded excel
												 */
												executeExcelOperation(sessionBean, data, extension);
											}
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
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "readParamsFromRequest()", e);
		}
	}

	
	private void resetForm(MMESIVinBean sessionBean, HttpServletRequest request)
	{
		sessionBean.setCountryLocaleList(null);
		sessionBean.setCountryLocaleId(null);
		sessionBean.setLanguageList(null);
		sessionBean.setManualLanguageId(null);
		sessionBean.setActionClicked(null);
		sessionBean.setSiNumber(null);
		sessionBean.setWmiId(null);
		sessionBean.setWmiList(null);
		sessionBean.setModelId(null);
		sessionBean.setCarlineCode(null);
		sessionBean.setCarlineNameEng(null);
		sessionBean.setCarlineNameReg(null);
		sessionBean.setModelsList(null);
		sessionBean.setVdsId(null);
		sessionBean.setHiddenVDSId(null);
		sessionBean.setShowAddNewVDSField(false);
		sessionBean.setAddNewVDSId(null);
		sessionBean.setVdsList(null);
//		sessionBean.setVinStartRange("000000");
//		sessionBean.setVinEndRange("ZZZZZZ");
		
		maximumVISRangeLength = 6;
		setDefaultVINRanges(sessionBean);
		
		sessionBean.setSuccessMessage(null);
		sessionBean.setErrorMessage(null);
		sessionBean.setSelectedRows(null);
		sessionBean.setDisplayPageNo(null);
		sessionBean.setDisplayPageLength(null);
		sessionBean.setDisplayPreviewPageLength(null);
		sessionBean.setDisplayPreviewPageNo(null);
		sessionBean.setSelectedPreviewRows(null);
		
		sessionBean.setDocumentsList(null);
		sessionBean.setTempVinsList(null);
		sessionBean.setEngineTypeList(null);
		sessionBean.setMissionTypeList(null);
		sessionBean.setEngineTypeIds(null);
		sessionBean.setMissionTypeIds(null);
		sessionBean.setHiddenEngineTypeId(null);
		sessionBean.setHiddenMissionTypeId(null);
		
		sessionBean.setShowTranslationOperationsControl(false);
		sessionBean.setYesNoList(null);
		sessionBean.setTranslationsUpdate(ApplicationProperties.getProperty("value.esicategory.flag.yes"));
		
		/*
		 * call function to load all the Country Locale Data
		 */
		getCountryLocaleList(sessionBean, request);
		
		/*
		 * call function to get YesNoList
		 */
		getYesNoList(sessionBean);
	}
		
	private String alreadyExists(String refKey, MMESIVinBean sessionBean)
	{	
		String status="NEW";
		if(null!=refKey && !"".equals(refKey) && null!=sessionBean.getTempVinsList() && sessionBean.getTempVinsList().size()>0)
		{
			refKey=  Utilities.replaceRefKeys(refKey);
			for(int i=0;i<sessionBean.getTempVinsList().size();i++)
			{
				SIVinDetails details = (SIVinDetails)sessionBean.getTempVinsList().get(i);
				if(details.getFifthLevelRefKey().trim().toLowerCase().equals(refKey.trim().toLowerCase()))
				{
					// already exists
					status = "ADDED";
					break;
				}
			}
		}
		return status;
	}
	
	private static void setSelectedTempRows(MMESIVinBean sessionBean, HttpServletRequest request)
	{
		try
		{
			if(null!=sessionBean.getTempVinsList() && sessionBean.getTempVinsList().size()>0)
			{
				for(int j=0;j<sessionBean.getTempVinsList().size();j++)
				{
					SIVinDetails itemDetails = (SIVinDetails)sessionBean.getTempVinsList().get(j);
					itemDetails.setSelected(false);
				}
			}
			if(null!=sessionBean.getSelectedPreviewRows() && !"".equals(sessionBean.getSelectedPreviewRows()))
			{
				String selectedSrNo=(String)sessionBean.getSelectedPreviewRows();
				if(selectedSrNo.endsWith(","))
				{
					selectedSrNo = selectedSrNo.substring(0, selectedSrNo.length()-1);
				}
				
				String[] tokens = selectedSrNo.split(",");
				if(null!=tokens && tokens.length>0 && null!=sessionBean.getTempVinsList() && sessionBean.getTempVinsList().size()>0)
				{
					for(int i=0;i<tokens.length;i++)
					{
						for(int j=0;j<sessionBean.getTempVinsList().size();j++)
						{
							SIVinDetails itemDetails = (SIVinDetails)sessionBean.getTempVinsList().get(j);
							if(String.valueOf(itemDetails.getSrNo()).equals(String.valueOf(tokens[i])))
							{
								itemDetails.setSelected(true);
							}
						}
					}
				}
				selectedSrNo = null;
				tokens = null;
			}
			
//			sessionBean.setSelectedPreviewRows(null);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "setSelectedTempRows()", e);
		}
	}
	
	
	private String replaceRefKeyChars(String refKey)
	{
		refKey = refKey.trim().toUpperCase();
		refKey = refKey.replace("-", "_");
		refKey = refKey.replace("/", "_");
		refKey = refKey.replace(" ", "_");
		refKey = refKey.replace("&", "_");
		return refKey;
	}
	
	private static void checkForWsl()
	{
		try
		{
			String usersToCheck=ApplicationProperties.getProperty("GMS3_INTERNAL_TEAM_USERS");
			if(null!=usersToCheck && !"".equals(usersToCheck))
			{
				StringTokenizer str = new StringTokenizer(usersToCheck, ",");
				while(str.hasMoreTokens())
				{
					String token = str.nextToken();
					if(token.trim().toLowerCase().equals(wslId.trim().toLowerCase()))
					{
						// INTERNAL TEAM USERS FOUND - REPLACE WITH USERNAME
						wslId= ApplicationProperties.getProperty("USERNAME");
						break;
					}
					token = null;
				}
				str = null;
			}
			usersToCheck= null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "checkForWsl", e);
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
	
	private void performAccessCheck(MMESIVinBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "performAccessCheck()", e);
		}
	}

	private  boolean validateFileUpload(MMESIVinBean sessionBean)
	{

		StringBuilder errorMessage = new StringBuilder();
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()))
		{
			if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
			{
				errorMessage.append("<MSG_TOKEN>");
			}
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.specific", msgProps.getProperty("label.countrylocale")));
		}

		if(null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()))
		{
			if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
			{
				errorMessage.append("<MSG_TOKEN>");
			}
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.specific", msgProps.getProperty("label.language")));
		}
		
		if(null==sessionBean.getSiNumber() || "".equals(sessionBean.getSiNumber()))
		{
			if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
			{
				errorMessage.append("<MSG_TOKEN>");
			}
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.specific", msgProps.getProperty("label.sidocid")));
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

	private boolean validate(MMESIVinBean sessionBean)
	{
		boolean addMandatoryMessage=false;
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) || 
				null==sessionBean.getSiNumber() || "".equals(sessionBean.getSiNumber()) || null==sessionBean.getWmiId() 
				|| "".equals(sessionBean.getWmiId()) || null==sessionBean.getModelId() || "".equals(sessionBean.getModelId()) 
				|| null==sessionBean.getEngineTypeIds() || sessionBean.getEngineTypeIds().length<=0  
				|| null==sessionBean.getMissionTypeIds() || sessionBean.getMissionTypeIds().length<=0  
				|| null==sessionBean.getVdsId() || sessionBean.getVdsId().length<=0  || null==sessionBean.getVinStartRange() || 
				"".equals(sessionBean.getVinStartRange()) || null==sessionBean.getVinEndRange() || "".equals(sessionBean.getVinEndRange()))
			
		{
			addMandatoryMessage = true;
		}
		if(sessionBean.isShowAddNewVDSField()==true)
		{
			if(null==sessionBean.getAddNewVDSId() || "".equals(sessionBean.getAddNewVDSId()))
			{
				addMandatoryMessage = true;
			}
		}
		
		if(addMandatoryMessage==true)
		{
			sessionBean.setErrorMessage(msgProps.getProperty("error.mandatory.fields"));
			return false;
		}
		
		
		if(sessionBean.isShowAddNewVDSField()==true)
		{
			if(null!=sessionBean.getAddNewVDSId() && !"".equals(sessionBean.getAddNewVDSId()))
			{
				if(sessionBean.getAddNewVDSId().trim().length()>8)
				{
					sessionBean.setErrorMessage(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.vdscode"),"8"));
					return false;
				}
			}
		}
		
		if(null!=sessionBean.getVinStartRange() && !"".equals(sessionBean.getVinStartRange()))
		{
			if(sessionBean.getVinStartRange().trim().length()!= maximumVISRangeLength)
			{
				sessionBean.setErrorMessage(msgProps.addMessage("error.length.exact.characters", msgProps.getProperty("label.visstartrange"),String.valueOf(maximumVISRangeLength)));
				return false;
			}
		}
		
		if(null!=sessionBean.getVinEndRange() && !"".equals(sessionBean.getVinEndRange()))
		{
			if(sessionBean.getVinEndRange().trim().length()!= maximumVISRangeLength)
			{
				sessionBean.setErrorMessage(msgProps.addMessage("error.length.exact.characters", msgProps.getProperty("label.visendrange"), String.valueOf(maximumVISRangeLength)));
				return false;
			}
		}
		return true;
	}
	
	
	private void addVinOperation(MMESIVinBean sessionBean, String wslId)
	{
		try
		{
			if(validate(sessionBean))
			{
				if(validateJobProcessing(sessionBean))
				{
					/*
					 * Here Locale will always be en_UK
					 */
//					String locale = ManualLanguageDAO.getManualLanguageCode(sessionBean.getManualLanguageId().trim());
					String locale = ApplicationProperties.getProperty("en_uk");
					if(null!=locale && !"".equals(locale))
					{
						StringBuilder errorMessage = new StringBuilder();
						
						locale = locale.trim();
						locale = locale.replace("-", "_");
						
						/*
						 * Prepare, temporary VIN DISPLAY List for Preview
						 */
						if(null==sessionBean.getTempVinsList() || sessionBean.getTempVinsList().size()<=0)
						{
							sessionBean.setTempVinsList(new ArrayList<SIVinDetails>());
						}
						
						/*
						 * First before Adding, identify the UNIQUE MODEL YEAR
						 * AND FOR EACH CHECK, IF THEY EXIST IN IM, THEN ONLY ADDLOWED
						 * TO BE ADDED FURTHER. ELSE THROW A MESSAGE - THAT MODEL YEAR DOES NOT EXISTS IN IM
						 * THEIR VINS CANNOT BE MAPPED - DATE 28 NOVEMBER 2016
						 */
						
						ArrayList<SIVinDetails> tempVinDetailsList = new ArrayList<SIVinDetails>();
						ArrayList<String> modelYearsList = new ArrayList<String>();
						ArrayList<String> modelYearDoesNotExistsinIMList = new ArrayList<String>();
						
						String uniqueVDSCodes="";
						// remove duplicate VDS from the selected ones
						if(null!=sessionBean.getVdsId() && sessionBean.getVdsId().length>0)
						{
							/*
							 *  SPLIT EACH COMMA SEParated token and Add unique VDS Code to uniqueVDSCodes
							 */
							String data = null;
							for(int a=0;a<sessionBean.getVdsId().length;a++)
							{
								try
								{
									// add condition for REMOVING ADDNEW from the selected VDS Codes
									if(null!=sessionBean.getVdsId()[a] && !"ADDNEW".equals(sessionBean.getVdsId()[a]))
									{
										if(sessionBean.getVdsId()[a].indexOf("(")!=-1)
										{
											data = sessionBean.getVdsId()[a].substring(0,sessionBean.getVdsId()[a].indexOf("("));
											if(null!=data && !"".equals(data) && !"".equals(data.trim()))
											{
												if(!uniqueVDSCodes.contains(data.trim()+","))
												{
													uniqueVDSCodes+=data.trim()+",";
												}
											}
											data = null;
										}
									}
								}
								catch(Exception e)
								{
									Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "addVinOperation()", e);
								}
							}
							data = null;
						}
						
						if(null!=uniqueVDSCodes && !"".equals(uniqueVDSCodes))
						{
							if(uniqueVDSCodes.endsWith(","))
							{
								uniqueVDSCodes = uniqueVDSCodes.substring(0, uniqueVDSCodes.length()-1);
							}
						}
						
						
						/*
						 * DTAE 2 ND JUNE 2019
						 * ADD NEW VIN ENTERED MANUALLY IF SHOW ADD NEW VIN IS TRUE
						 */
						if(sessionBean.isShowAddNewVDSField()==true)
						{
							if(null!=sessionBean.getAddNewVDSId() && !"".equals(sessionBean.getAddNewVDSId()))
							{
								if(!uniqueVDSCodes.trim().toLowerCase().contains(sessionBean.getAddNewVDSId().trim().toLowerCase()))
								{
									if(!uniqueVDSCodes.endsWith(","))
									{
										uniqueVDSCodes+=",";
									}
									uniqueVDSCodes+=sessionBean.getAddNewVDSId();
								}
							}
						}
//						String selectedVDSCodes[] = sessionBean.getVdsId();
						String selectedVDSCodes[] = uniqueVDSCodes.split(",");
						if(null!=selectedVDSCodes && selectedVDSCodes.length>0)
						{
							for(int a=0;a<selectedVDSCodes.length;a++)
							{
								String token = selectedVDSCodes[a];
								if(null!=token && !"".equals(token) && !ApplicationProperties.getProperty("check.all.label").equals(token))
								{
									/*
									 * Proceed for adding VINs.
									 */
									SIVinDetails itemDetails = new SIVinDetails();
									itemDetails.setSrNo(sessionBean.getTempVinsList().size()+1);
									
									itemDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
									itemDetails.setManualLanguageId(sessionBean.getManualLanguageId());
									itemDetails.setWmiCode(sessionBean.getWmiId());
									
									if(null!=sessionBean.getModelsList() && sessionBean.getModelsList().size()>0)
									{
										for(CarlineDetails carD : sessionBean.getModelsList())
										{
											if(carD.getCarlineIdForCombo().trim().toLowerCase().equals(sessionBean.getModelId().trim().toLowerCase()))
											{
												itemDetails.setModel(carD.getCarlineNameEng());
												itemDetails.setModelRegionalName(carD.getCarlineNameReg());
												itemDetails.setCarlineCode(carD.getCarlineCode());
												String modelRefKey = itemDetails.getModel().trim();
												// replace - from the name if exists in the string for the following refKeys
//												if(itemDetails.getModel().trim().toLowerCase().equals("cx-3") 
//													|| itemDetails.getModel().trim().toLowerCase().equals("cx-5") || 
//													itemDetails.getModel().trim().toLowerCase().equals("cx-7") || 
//													itemDetails.getModel().trim().toLowerCase().equals("rx-7") || 
//													itemDetails.getModel().trim().toLowerCase().equals("rx-8"))
//												{
													modelRefKey = modelRefKey.replace("-", "");
//												}
												// add carlineCode
												modelRefKey = modelRefKey.trim()+"_"+itemDetails.getCarlineCode().trim();
												modelRefKey = Utilities.replaceRefKeys(modelRefKey.trim().toUpperCase());
												// PREPARE MODEL REF KEY AND ADD TO MODEL YEAR REF KEY LIST, AVOID DUPLICATION VALUES
												boolean addToList = true;
												if(null!=modelYearsList && modelYearsList.size()>0)
												{
													for(String existRef : modelYearsList)
													{
														if(existRef.trim().toLowerCase().equals(modelRefKey.trim().toLowerCase()))
														{
															addToList = false;
															break;
														}
													}
												}
												
												if(addToList==true)
												{
													modelYearsList.add(modelRefKey);
												}
												modelRefKey=  null;
												
												break;
											}
										}
									}
									
									
									itemDetails.setEngineTypeCode(sessionBean.getHiddenEngineTypeId());
									itemDetails.setMissionTypeCode(sessionBean.getHiddenMissionTypeId());
									itemDetails.setVdsCode(token.trim());
									
									/*
									 * 23 RD JUNE 2019 - SET CUSTOM VDS FLAG TO TRUE
									 * IF IT MATCHES WITH NEW VDS ADDED FIELD 
									 */
									if(sessionBean.isShowAddNewVDSField()==true)
									{
										if(null!=sessionBean.getAddNewVDSId() && !"".equals(sessionBean.getAddNewVDSId()))
										{
											if(sessionBean.getAddNewVDSId().trim().toLowerCase().equals(token.trim().toLowerCase()))
											{
												itemDetails.setCustomVDS(true);
											}
										}
									}
									
									itemDetails.setVinStartRange(sessionBean.getVinStartRange());
									itemDetails.setVinEndRange(sessionBean.getVinEndRange());

									// set Levels
									// Model
									if(null!=itemDetails.getModelRegionalName() && !"".equals(itemDetails.getModelRegionalName()))
									{
										itemDetails.setFirstLevelName(itemDetails.getModelRegionalName().trim()+" "+itemDetails.getCarlineCode().trim());
									}
									else
									{
										itemDetails.setFirstLevelName(itemDetails.getModel().trim()+" "+itemDetails.getCarlineCode().trim());
									}
									/*
									 * PREPARE FIRST LEVEL REF KEY
									 */
									String modelRefKey=itemDetails.getModel().trim();
//									if(itemDetails.getModel().trim().toLowerCase().equals("cx-3") 
//											|| itemDetails.getModel().trim().toLowerCase().equals("cx-5") || 
//											itemDetails.getModel().trim().toLowerCase().equals("cx-7") || 
//											itemDetails.getModel().trim().toLowerCase().equals("rx-7") || 
//											itemDetails.getModel().trim().toLowerCase().equals("rx-8"))
//									{
										modelRefKey = modelRefKey.replace("-", "");
//									}
									// add carlineCode
									modelRefKey = modelRefKey.trim()+"_"+itemDetails.getCarlineCode().trim();
									itemDetails.setFirstLevelRefKey(Utilities.replaceRefKeys(modelRefKey));
//									itemDetails.setFirstLevelRefKey(Utilities.replaceRefKeys(itemDetails.getModel().trim()+"_"+itemDetails.getCarlineCode().trim()));
									// WMI
									String wmiRefKeyToken="";
									if(sessionBean.getWmiId().trim().equals("-"))
									{
										wmiRefKeyToken="___";
									}
									else
									{
										wmiRefKeyToken=sessionBean.getWmiId().trim().toUpperCase();
									}
									itemDetails.setSecondLevelName(sessionBean.getWmiId().trim().toUpperCase());
									itemDetails.setSeconddLevelRefKey(Utilities.replaceRefKeys(modelRefKey.trim())+wmiRefKeyToken.trim());
									
									// VDS
									itemDetails.setThirdLevelName(token);
									itemDetails.setThirdLevelRefKey(Utilities.replaceRefKeys(itemDetails.getCarlineCode().trim().toUpperCase()+wmiRefKeyToken+token));
									// VIS START
									itemDetails.setFourthLevelName(sessionBean.getVinStartRange());
									itemDetails.setFourthLevelRefKey(Utilities.replaceRefKeys(itemDetails.getCarlineCode().trim().toUpperCase()+wmiRefKeyToken+token+sessionBean.getVinStartRange()));
									// VIS END
									if(sessionBean.getWmiId().trim().equals("-"))
									{
										itemDetails.setFifthLevelName(itemDetails.getModelRegionalName()+" "+itemDetails.getCarlineCode()+" "+token+"-"+sessionBean.getVinStartRange()+"-"+sessionBean.getVinEndRange());
									}
									else
									{
										itemDetails.setFifthLevelName(itemDetails.getModelRegionalName()+" "+itemDetails.getCarlineCode()+" "+itemDetails.getWmiCode()+"-"+token+"-"+sessionBean.getVinStartRange()+"-"+sessionBean.getVinEndRange());
									}
									itemDetails.setFifthLevelRefKey(Utilities.replaceRefKeys(itemDetails.getCarlineCode().trim().toUpperCase()+wmiRefKeyToken+token+sessionBean.getVinStartRange()+sessionBean.getVinEndRange()));
									// for the new itemToBeAdded set the selectedFlag to true
									itemDetails.setSelected(true);

									// add to tempVINList
									tempVinDetailsList.add(itemDetails);
									itemDetails= null;
									modelRefKey = null;
								}
								token = null;
							}
						}
						
						uniqueVDSCodes = null;
						/*
						 * NOW FOR THE MODEL YEAR LIST, CHECK WHAT ALL EXISTS IN IM AND WHAT DOES NOT
						 * FOR THE ONES THAT DO NOT EXISTS IN IM - REMOVE IT FROM THE MODEL YEAR LIST AND
						 * AND ADD THEM TO DOES NOT EXIST LIST
						 */
						
						/*
						 * WHICH MODEL YEAR CATEGORIES ARE USABLE FOR THIS LOCALE?
						 *
						 * WAS: getCategoryDetails(modelYearsList) - ask InfoManager which reference keys
						 * exist, and report every one that does not.
						 *
						 * NOW: kapture_cms_db.k_categories holds a row PER LOCALE, so a key that exists for
						 * another locale but not this one is NOT missing - it is created for this locale
						 * together with every level above it, through the category REST API. Only a key that
						 * exists NOWHERE is reported. See KaptureCategoryServiceImpl.
						 */
						ArrayList<String> modelYearsNotInKapture = new ArrayList<String>();
						if(null!=modelYearsList && modelYearsList.size()>0)
						{
							String categorySourceLocale = FetchKaptureDataDAO.resolveLabelLocale(locale, "MME");
							modelYearsNotInKapture = KaptureCategoryServiceImpl.ensureCategoriesForLocale(modelYearsList, locale, categorySourceLocale);
							categorySourceLocale = null;
						}
						
						if(null!=modelYearsNotInKapture && modelYearsNotInKapture.size()>0)
						{
							/*
							 * IDENTIFY THE ONES THAT ARE USABLE AND THE ONES THAT ARE NOT
							 */
							for(int a=0;a<modelYearsList.size();a++)
							{
								String refKey = modelYearsList.get(a);
								boolean found = true;
								for(int b=0;b<modelYearsNotInKapture.size();b++)
								{
									if(String.valueOf(modelYearsNotInKapture.get(b)).trim().toLowerCase().equals(refKey.trim().toLowerCase()))
									{
										found=false;
										break;
									}
								}
						
								if(found==false)
								{
									// add to
									modelYearDoesNotExistsinIMList.add(refKey);
									modelYearsList.remove(a);
									a--;
								}
							}
						}
						modelYearsNotInKapture = null;
						
						
						/*
						 * Now iterate TEMP VIN LIST & FOR EACH CHECK IF MODEL YEAR EXISTS IN EXISTING MODEL YEAR LIST
						 * THEN ONLY ADD THEM ELSE SKIP THEM.
						 */
						String duplicateRefKeys="";
						if(null!=tempVinDetailsList && tempVinDetailsList.size()>0)
						{
							for(int e=0;e<tempVinDetailsList.size();e++)
							{
								SIVinDetails sivinDetails = (SIVinDetails) tempVinDetailsList.get(e);
								String modelYearRefKey = "";
								if(null!=sivinDetails.getModel() && !"".equals(sivinDetails.getModel()) && null!=sivinDetails.getCarlineCode() && !"".equals(sivinDetails.getCarlineCode()))
								{
									modelYearRefKey = sivinDetails.getModel().trim().toUpperCase();
//									if(sivinDetails.getModel().trim().toLowerCase().equals("cx-3") || 
//											sivinDetails.getModel().trim().toLowerCase().equals("cx-5") || 
//											sivinDetails.getModel().trim().toLowerCase().equals("cx-7") || 
//											sivinDetails.getModel().trim().toLowerCase().equals("rx-7") || 
//											sivinDetails.getModel().trim().toLowerCase().equals("rx-8"))
//									{
										modelYearRefKey = modelYearRefKey.replace("-", "");
//									}
									// add Carline Code
									modelYearRefKey = modelYearRefKey.trim().toUpperCase()+"_"+sivinDetails.getCarlineCode().trim().toUpperCase();
									modelYearRefKey = replaceRefKeyChars(modelYearRefKey);
									if(null!=modelYearsList && modelYearsList.size()>0)
									{
										for(int a=0;a<modelYearsList.size();a++)
										{
											if(String.valueOf(modelYearsList.get(a)).trim().toLowerCase().equals(modelYearRefKey.trim().toLowerCase()))
											{
												// PROCEED FOR ADDING THE ITEM TO SESSION BEAN LIST, ELSE SKIP IT.
												if(alreadyExists(sivinDetails.getFifthLevelRefKey(), sessionBean).equals("NEW"))
												{
													sivinDetails.setSrNo(sessionBean.getTempVinsList().size()+1);
													sessionBean.getTempVinsList().add(sivinDetails);
												}
												else
												{
													duplicateRefKeys = duplicateRefKeys+sivinDetails.getFifthLevelName()+",";
												}
											}
										}
									}
								}
								modelYearRefKey = null;
								sivinDetails = null;
							}
						}
						
						
						if(null!=modelYearDoesNotExistsinIMList && modelYearDoesNotExistsinIMList.size()>0)
						{
							String modelsThatDoesNotExist ="";
							for(int e=0;e<modelYearDoesNotExistsinIMList.size();e++)
							{
								modelsThatDoesNotExist= modelsThatDoesNotExist+String.valueOf(modelYearDoesNotExistsinIMList.get(e))+",";
							}
							if(modelsThatDoesNotExist.endsWith(","))
							{
								modelsThatDoesNotExist = modelsThatDoesNotExist.substring(0, modelsThatDoesNotExist.length()-1);
							}
							
							if(null!=modelsThatDoesNotExist && !"".equals(modelsThatDoesNotExist))
							{
								modelsThatDoesNotExist ="["+modelsThatDoesNotExist+"]";
								errorMessage.append(msgProps.addMessage("error.model.year.doesnot.exist.im", modelsThatDoesNotExist));
							}
							modelsThatDoesNotExist =null;
						}
						
						
						if(null!=duplicateRefKeys && !"".equals(duplicateRefKeys))
						{
							if(duplicateRefKeys.endsWith(","))
							{
								duplicateRefKeys = duplicateRefKeys.substring(0, duplicateRefKeys.length()-1);
							}
							duplicateRefKeys = "{"+duplicateRefKeys+"}";
							logger.info("addVinOperation :: Following VINS are already Added :: > " + duplicateRefKeys);
							// set errorMessage
//							sessionBean.setErrorMessage(msgProps.addMessage("error.vin.already.added", duplicateRefKeys));
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							errorMessage.append(msgProps.getProperty("error.vin.already.added"));
						}
						duplicateRefKeys = null;
						
						if(null!=errorMessage && !"".equals(errorMessage.toString()))
						{
							sessionBean.setErrorMessage(errorMessage.toString());
						}
						errorMessage = null;
						
						tempVinDetailsList = null;
						modelYearDoesNotExistsinIMList = null;
						modelYearsList = null;
					}
					else
					{
						logger.info("addVinOperation :: Failed to fetch locale details for the selected Manual Language.");
						sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
					}
				}
			}
			
			if(null!=sessionBean.getTempVinsList() && sessionBean.getTempVinsList().size()>0)
			{
				String selectedRows="";
				for(int r=0;r<sessionBean.getTempVinsList().size();r++)
				{
					SIVinDetails details = (SIVinDetails)sessionBean.getTempVinsList().get(r);
					if(details.isSelected()==true)
					{
						selectedRows = selectedRows+String.valueOf(details.getSrNo())+".";
					}
				}
				if(null!=selectedRows && !"".equals(selectedRows))
				{
					if(selectedRows.endsWith(","))
					{
						selectedRows = selectedRows.substring(0, selectedRows.length()-1);
					}
					// set in sessionBean
					sessionBean.setSelectedPreviewRows(selectedRows);
				}
				selectedRows = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "addVinOperation()", e);
		}
	}

	/**
	 * Function will set Default VIN Ranges
	 * @param sessionBean
	 */
	private void setDefaultVINRanges(MMESIVinBean sessionBean)
	{
		try
		{
			sessionBean.setVinStartRange("");
			sessionBean.setVinEndRange("");
			
			String startRange="";
			String endRange="";
			if(maximumVISRangeLength>0)
			{
				for(int a=0;a<maximumVISRangeLength;a++)
				{
					startRange=startRange+"0";
					endRange=endRange+"Z";
				}
			}
			
			sessionBean.setVinStartRange(startRange);
			sessionBean.setVinEndRange(endRange);
			
			startRange = null;
			endRange = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "setDefaultVINRanges()", e);
		}
	}

	private void mapVinOperation(MMESIVinBean sessionBean, String wslId, HttpServletRequest request)
	{
		sessionBean.setScheduleName(null);
		try
		{
			if(validateJobProcessing(sessionBean))
			{
				if(null!=sessionBean.getSelectedPreviewRows() && !"".equals(sessionBean.getSelectedPreviewRows()))
				{
					String selectedSrNo=(String)sessionBean.getSelectedPreviewRows();
					if(selectedSrNo.endsWith(","))
					{
						selectedSrNo = selectedSrNo.substring(0, selectedSrNo.length()-1);
					}
					
					ArrayList<SIVinDetails> categoriesSelectedList = new ArrayList<SIVinDetails>();
					String[] tokens = selectedSrNo.split(",");
					if(null!=tokens && tokens.length>0 && null!=sessionBean.getTempVinsList() && sessionBean.getTempVinsList().size()>0)
					{
						for(int i=0;i<tokens.length;i++)
						{
							for(int j=0;j<sessionBean.getTempVinsList().size();j++)
							{
								SIVinDetails itemDetails = (SIVinDetails)sessionBean.getTempVinsList().get(j);
								if(String.valueOf(itemDetails.getSrNo()).equals(String.valueOf(tokens[i])))
								{
									categoriesSelectedList.add(itemDetails);
									break;
								}
							}
						}
						
						if(null!=categoriesSelectedList && categoriesSelectedList.size()>0)
						{
							// HERE LOCALE WILL ALWAYS BE EN_UK FOR FETCHING DATA FROM MDM - BUT FOR PROCESSING CATEGORIES IN IM - PROCESSING LOCALE
							String processingLocale = ManualLanguageDAO.getManualLanguageCode(sessionBean.getManualLanguageId().trim());
							/*
							 * HYPHEN TO UNDERSCORE, AS THE EXCEL PATH BELOW ALREADY DOES.
							 *
							 * gms3_mdm_manual_language stores the code hyphenated ("en-EU") and
							 * kapture_cms_db stores it with an underscore ("en_EU"), so the raw value
							 * matches NOTHING on the Kapture side: k_categories lookups miss, every
							 * category is treated as absent, and the create then fails because its
							 * parent "does not exist" either. Seen on SI1092 / en_EU, where the run
							 * logged: createCategory POST {ERJM3ER____R_} locale {en-EU} -> 404
							 * "Parent category not found for reference key: CX7_ERJM3".
							 */
							if(null!=processingLocale && !"".equals(processingLocale))
							{
								processingLocale = processingLocale.trim();
								processingLocale = processingLocale.replace("-", "_");
								/*
								 * PROCEED FOR CREATING VINS AND MAPPING IT WITH DOCUMENTS.
								 * here, we have WMI CODE, VDS CODE MULTIPLE + VIN RANGE
								 */

								ArrayList<String> categoriesToBeAddedToDocument = new ArrayList<String>();
								/*
								 * ITERATE SELECTED CAT LIST AND ALL WMIS TO LIST
								 * ONLY 3RD, 4TH & 5TH LEVEL NO NEED OF CHECKING FOR 1ST AND 2ND LEVEL
								 */
								ArrayList<IMCategoryDetails> catList = new ArrayList<IMCategoryDetails>();
								
								/*
								 * add all 3rd level Categories to the IM LIST
								 */
								SIVinDetails selItemDetails = null;
								IMCategoryDetails details = null;
								IMCategoryDetails added = null;
								for(int r=0;r<categoriesSelectedList.size();r++)
								{
									selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
									if(null!=selItemDetails.getThirdLevelRefKey() && !"".equals(selItemDetails.getThirdLevelRefKey()))
									{
										/*
										 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
										 */
										boolean addToList = true;
										if(null!=catList && catList.size()>0)
										{
											added = null;
											for(int e=0;e<catList.size();e++)
											{
												added = (IMCategoryDetails)catList.get(e);
												if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getThirdLevelRefKey().trim().toLowerCase()))
												{
													addToList = false;
													break;
												}
												added = null;
											}
										}
										
										if(addToList==true)
										{
											details = new IMCategoryDetails();
											details.setCategoryName(selItemDetails.getThirdLevelName().trim().toUpperCase());
											details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
											details.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getSeconddLevelRefKey().trim().toUpperCase()));
											// set Processing Locale - so that Label gets updated for it.
											details.setLocale(processingLocale);
											// set level as LEVEL 3
											details.setLevel(ScheduleConstants.LEVEL_3);
											// set itemDetails
											details.setItemDetails(new SIVinDetails());
											details.setItemDetails(selItemDetails);
											
											catList.add(details);
											details = null;
										}
									}
									selItemDetails  =null;
								}
								/*
								 * add all 4th Level Categories to the IM LIST
								 */
								for(int r=0;r<categoriesSelectedList.size();r++)
								{
									selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
									if(null!=selItemDetails.getFourthLevelRefKey() && !"".equals(selItemDetails.getFourthLevelRefKey()))
									{
										/*
										 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
										 */
										boolean addToList = true;
										if(null!=catList && catList.size()>0)
										{
											for(int e=0;e<catList.size();e++)
											{
												added = (IMCategoryDetails)catList.get(e);
												if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getFourthLevelRefKey().trim().toLowerCase()))
												{
													addToList = false;
													break;
												}
												added =  null;
											}
										}
										
										if(addToList==true)
										{
											details = new IMCategoryDetails();
											details.setCategoryName(selItemDetails.getFourthLevelName().trim().toUpperCase());
											details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getFourthLevelRefKey().trim().toUpperCase()));
											details.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
											// set Processing Locale - so that Label gets updated for it.
											details.setLocale(processingLocale);
											// set level as LEVEL 4
											details.setLevel(ScheduleConstants.LEVEL_4);
											// set itemDetails
											details.setItemDetails(new SIVinDetails());
											details.setItemDetails(selItemDetails);
											
											catList.add(details);
											details = null;
										}
									}
									selItemDetails  = null;
								}
								
								/*
								 * add all 5th Level Categories to the IM LIST
								 */
								for(int r=0;r<categoriesSelectedList.size();r++)
								{
									selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
									if(null!=selItemDetails.getFifthLevelRefKey() && !"".equals(selItemDetails.getFifthLevelRefKey()))
									{
										/*
										 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
										 */
										boolean addToList = true;
										if(null!=catList && catList.size()>0)
										{
											for(int e=0;e<catList.size();e++)
											{
												added = (IMCategoryDetails)catList.get(e);
												if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getFifthLevelRefKey().trim().toLowerCase()))
												{
													addToList = false;
													break;
												}
												added= null;
											}
										}
										
										if(addToList==true)
										{
											details = new IMCategoryDetails();
											details.setCategoryName(selItemDetails.getFifthLevelName().trim().toUpperCase());
											details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getFifthLevelRefKey().trim().toUpperCase()));
											details.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getFourthLevelRefKey().trim().toUpperCase()));
											// set Processing Locale - so that Label gets updated for it.
											details.setLocale(processingLocale);
											
											// set level as LEVEL 5
											details.setLevel(ScheduleConstants.LEVEL_5);
											// set itemDetails
											details.setItemDetails(new SIVinDetails());
											details.setItemDetails(selItemDetails);
											
											catList.add(details);
											
											/*
											 * add VIN Ranges to categoriesToBeAddedToDocument
											 */
											categoriesToBeAddedToDocument.add(Utilities.replaceRefKeys(selItemDetails.getFifthLevelRefKey().trim().toUpperCase()));
											details = null;
										}
									}
									selItemDetails =  null;
								}
								
								/*
								 * ALSO ITERATE AND ADD EACH MODEL YEAR TO DOCUMENT
								 * 
								 * NO NEED OF MAPPING MODEL HERE, SINCE THE VIN IS ALREADY INSIDE THE CARLINE HIERARCHY
								 */
								
								/*
								 * CHECK HERE IF CATLIST SIZE IS MORE THAN DEFINED LIMIT THEN DO NOT PROCEED FOR MAPPING IT RIGHT AWAY
								 * SCHEDULE A JOB AND PERFORM REST OF THE ACTIVITIES THERE
								 */
								
								boolean scheduleJob=true;
								if(null!=catLoadLimit && !"".equals(catLoadLimit))
								{
									// user categoriesToBeAddedToDocument because it contains only 5th levels
									if(null!=categoriesToBeAddedToDocument && categoriesToBeAddedToDocument.size()>0)
									{
										try
										{
											if(categoriesToBeAddedToDocument.size()<= new Integer(catLoadLimit).intValue())
											{
												/*
												 *  do not schedule Job as no of categories to be processed are less than 
												 *  or equals to defined limit.
												 */
												scheduleJob = false;
											}
										}
										catch(Exception e)
										{
											Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "mapVINOperatio()", e);
										}
									}
								}
								
								if(scheduleJob==false)
								{
									if(null!=catList && catList.size()>0)
									{
										/*
										 * call function to create NewCategories in IM.
										 */
										createCategory(catList, processingLocale, wslId);
									}

									/*
									 * HERE DOCUMENT LOCALE WILL THE ORIGINAL LOCALE SELCETED.
									 */
									SIVinDetails documentDetails = new SIVinDetails();
									documentDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
									documentDetails.setManualLanguageId(sessionBean.getManualLanguageId());
									//	documentDetails.setLocale(locale);
									documentDetails.setLocale(processingLocale);
									documentDetails.setWslId(wslId);
									if(null!=categoriesSelectedList && categoriesSelectedList.size()>0)
									{
										documentDetails.setItemsList(new ArrayList<SIVinDetails>());
										documentDetails.setItemsList(categoriesSelectedList);
									}
									/*
									 * NO TRANSLATION FAN-OUT ON THIS INLINE PATH, AND THAT IS DELIBERATE.
									 *
									 * Update Translations only takes effect through the SCHEDULED path
									 * (MMESIVINBatchProcessingImpl). A document here can carry many
									 * locales, and each one needs its category labels updated before it
									 * can be mapped - far too slow to run on a request thread, which
									 * would time the screen out. The batch is the right place for it.
									 *
									 * MNAO IS THE EXCEPTION AND DOES FAN OUT INLINE - see
									 * MNAOSIVin.addVinsToFrenchTranslation(). It has exactly ONE target,
									 * fr_CA, and only when the radio says Yes, so the cost is bounded.
									 * Do not copy that here without solving the timeout first.
									 */
									documentDetails = KaptureContentServiceImpl.modifyContent(sessionBean.getSiNumber().trim(), processingLocale, wslId, categoriesToBeAddedToDocument, documentDetails);
									if(null!=documentDetails  && null!=documentDetails.getDocumentId() && !"".equals(documentDetails.getDocumentId()))
									{

										sessionBean.setSuccessMessage(msgProps.addMessage("vin.mapping.success", sessionBean.getSiNumber()));
										sessionBean.setDocumentsList(new ArrayList<SIVinDetails>());
										sessionBean.setTempVinsList(null);
										/*
										 * call function to save the Data in database
										 */
										boolean bool = MMESIVinDAO.addVINDetails(documentDetails, null, "Y");
										if(bool==false)
										{
											sessionBean.setErrorMessage(msgProps.getProperty("error.failure.save.addvin.mapping.db"));
										}
										/*
										 * call function to reload the document details
										 */
										getDocumentOperation(sessionBean, sessionBean.getSiNumber().trim().toUpperCase(), processingLocale);
									}
									else
									{
										logger.info("mapVinOperation :: Either Document Details or New Document id is null after trying to update in IM.");

										if(null!=documentDetails && null!=documentDetails.getErrorCode() && documentDetails.getErrorCode().equals("7000"))
										{
											// APPEND ERROR - USER NOT AUTHROIZED 
											sessionBean.setErrorMessage(msgProps.getProperty("error.user.not.authorized.modify.content"));
										}
										else
										{
											// ADD GENERIC ERROR
											sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
										}
									}
									documentDetails = null;
								}
								else
								{
									if(null!=catList && catList.size()>0)
									{
										/*
										 * SCHEDULE A JOB, NAVIGATE USER TO HISTORY PAGE FOR VIEWING PROCESSING
										 */
										SIVINScheduleDetails schDetails = new SIVINScheduleDetails();
										schDetails.setScheduleName(ApplicationProperties.getProperty("mme.schedule.name.key"));
										schDetails.setLocale(processingLocale);
										schDetails.setDocumentId(sessionBean.getSiNumber().trim().toUpperCase());
										/*
										 * TOTAL IS THE NUMBER OF VIN RANGES, i.e. the LEAF categories - which is exactly what
										 * categoriesToBeAddedToDocument holds. It was catList.size()/3, on the assumption that a
										 * VIN row always contributes three levels. It does not: catList carries every level that
										 * has to be CREATED and PARENTS ARE SHARED between rows, so the ratio moves with the data.
										 * Six VIN ranges over two VDS codes produced fourteen categories, and 14/3 truncated to a
										 * reported total of 4 against 6 processed and 6 mapped (schedule 25382, SI1077 / ja_JP).
										 *
										 * MNAO counts the same thing with its own totalCountForJob counter, incremented where the
										 * leaf is added. Original line kept below for reference.
										 *
										 * schDetails.setTotalCount((catList.size() / 3));
										 */
										schDetails.setTotalCount(categoriesToBeAddedToDocument.size());
										schDetails.setCategoryList(catList);
										schDetails.setWslId(wslId);
										schDetails.setJobStatus(ScheduleConstants.STATUS_PENDING);
										// call function to schedule a Job in database
										long scheduleId = SIVINBatchTransactionDAO.createSchedule(schDetails);
										if(scheduleId>0)
										{
											logger.info("mapVINOperation :: Category Creation JOB Scheduled successfully. Navigate user to History Page Showing JOB Name.");
											// UPDATE SCHDULE NAME AND THREAD ID
											schDetails.setScheduleName(schDetails.getScheduleName()+String.valueOf(scheduleId));
											schDetails.setThreadId(schDetails.getScheduleName());
											
											// show success message - Job scheduled successfully
											sessionBean.setSuccessMessage(msgProps.addMessage("schedule.success", schDetails.getScheduleName()));
											// set scheduleName in sessionBean
											sessionBean.setScheduleName(schDetails.getScheduleName());
											/*
											 * START A PARALLEL THREAD.
											 */
											/*
											 * HERE DOCUMENT LOCALE WILL THE ORIGINAL LOCALE SELCETED.
											 * THIS IS REQUIRED FOR ADDING VIN DETAILS FOR THE PROCESSING DOCUMENT FROM JOB
											 */
											final SIVinDetails documentDetails = new SIVinDetails();
											String translationUpdate=null;
											if(sessionBean.isShowTranslationOperationsControl()==true)
											{
												// en_EU Locale - take the current selected Value
												translationUpdate = sessionBean.getTranslationsUpdate();
											}
											else
											{
												// always send this as No
												translationUpdate = ApplicationProperties.getProperty("value.esicategory.flag.no");
											}
											final String trnalsationUpdateFlag = translationUpdate;
											translationUpdate = null;
											documentDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
											documentDetails.setManualLanguageId(sessionBean.getManualLanguageId());
											//	documentDetails.setLocale(locale);
											documentDetails.setLocale(processingLocale);
											documentDetails.setWslId(wslId);
											if(null!=categoriesSelectedList && categoriesSelectedList.size()>0)
											{
												documentDetails.setItemsList(new ArrayList<SIVinDetails>());
												documentDetails.setItemsList(categoriesSelectedList);
											}
											
											final String schId=String.valueOf(scheduleId);
											final MMESIVINBatchProcessingImpl catProImpl = new MMESIVINBatchProcessingImpl();
											Runnable runn = new Runnable() 
											{
												@Override
												public void run() {
													
													synchronized (catProImpl) {
														try {
															catProImpl.startProcess(schId, documentDetails,"MME", trnalsationUpdateFlag);
														} catch (Exception e) {
															Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "run()", e);
														}
													}
												}
											};
											
											Thread th = new Thread(runn, schDetails.getThreadId());
											th.start();
										}
										else
										{
											logger.info("mapVINOperation :: Failed to create schedule for Category Creation. Throw Generic error");
											// ADD GENERIC ERROR
											sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
										}
										schDetails  = null;
									}
								}
								catList = null;
							}
							else
							{
								logger.info("mapVinOperation :: Failed to fetch locale details for the selected Manual Language.");
								sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
							}
							processingLocale = null;
						}
						else
						{
							logger.info("mapVinOperation :: Please select atleast one row for mapping VIN.");
							sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.add.vin"));
						}
					}
					categoriesSelectedList = null;
					tokens=  null;
					selectedSrNo = null;
				}
				else
				{
					logger.info("mapVinOperation :: Please select atleast one row for mapping VIN.");
					sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.add.vin"));
				}
				sessionBean.setSelectedPreviewRows(null);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "mapVinOperation()", e);
		}
	}
	
	private void searchOperation(MMESIVinBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSiNumber() && !"".equals(sessionBean.getSiNumber()) && 
					null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()) && 
					null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				/*
				 * THE THREE CHANNELS MME MAPS VIN RANGES ONTO, identified by document-id prefix:
				 *
				 *   SI - Service Information : same abbreviation in InfoManager and Kapture
				 *   ST - System Training     : was TRAINING / "TR" in InfoManager. KAPTURE RENAMED
				 *                              THE CHANNEL to System Training and its documents
				 *                              carry "ST", so the legacy "tr" test would reject
				 *                              every training document there is.
				 *   VI - Videos              : same abbreviation in both
				 *
				 * MNAO applies no prefix test at all and MC accepts SI only - deliberate, both
				 * left as they are.
				 */
				if(sessionBean.getSiNumber().trim().toLowerCase().startsWith("si") || 
						sessionBean.getSiNumber().trim().toLowerCase().startsWith("st") || 
						sessionBean.getSiNumber().trim().toLowerCase().startsWith("vi"))
				{
					sessionBean.setDocumentsList(new ArrayList<SIVinDetails>());
					String documentId=sessionBean.getSiNumber().trim();
					/*
					 * call function to get Locale Value
					 */
					String locale = ManualLanguageDAO.getManualLanguageCode(sessionBean.getManualLanguageId().trim());
					if(null!=locale && !"".equals(locale))
					{
						getDocumentOperation(sessionBean, documentId, locale);
					}
					else
					{
						logger.info("searchOperation :: Failed to fetch locale details for the selected Manual Language.");
						// Failed to fetch locale details for the selected Manual Language.
						sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
					}
					documentId  = null;
					locale=null;
				}
				else
				{
					logger.info("searchOperation :: Please Enter a valid SI / System Training / Video Document Id.");
					sessionBean.setErrorMessage(msgProps.getProperty("error.valid.mme.document"));
				}
			}
			else
			{
				logger.info("searchOperation :: Please select Country Locale, Manual Language & Enter SI Document id.");
				sessionBean.setErrorMessage(msgProps.getProperty("error.mandatory.fields"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(),"searchOperation()",e);
		}
	}

	private void getDocumentOperation(MMESIVinBean sessionBean, String documentId, String locale)
	{
		try
		{
			if(null!=documentId && !"".equals(documentId) && null!=locale && !"".equals(locale))
			{
				locale = locale.trim();
				locale = locale.replace("-", "_");
				
				Connection conn = null;
				String connClosed="N";
				try
				{
					conn = DBConnectionHelper.getConnection();
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(MMESIVin.class.getName(),"getDocumentOperation()",e);
				}
				
				/*
				 * Fetch Document Id details form IM Database
				 */
				String vinParentRefKey = ApplicationProperties.getProperty("MC.VIN.HIERARCHY.REFKEY");
				/*
				 * KAPTURE, NOT INFOMANAGER. vinParentRefKey is CARLINE for MC and MME - the
				 * mastercategoryrefkey the document's categories are filtered by, where MNAO uses VIN.
				 * A SEPARATE CONNECTION is needed because the document lives in kapture_cms_db while
				 * conn is the MDM schema.
				 */
				Connection cmsConn = DBConnectionHelper.getCMSConnection();
				String labelLocale = FetchKaptureDataDAO.resolveLabelLocale(locale, "MME");
				SIVinDetails imDocumentDetails = FetchKaptureDataDAO.getDocumentsData(documentId, locale, vinParentRefKey, "MME", cmsConn, "N");
				if(null!=imDocumentDetails && null!=imDocumentDetails.getDocumentId() && !"".equals(imDocumentDetails.getDocumentId()))
				{
					/*
					 * CHECK IF DOCUMENT IS CHECKED OUT BY SOME ONE, SHOW ERROR MESSAGE, 
					 * THE DOCUMENT IS CHECKED OUT IN INFOMANAGER. PLEASE CLEAR CHECK OUT ON {"++"} TO PROCEED FURTHER.
					 */
					if(imDocumentDetails.isCheckedOut()==true)
					{
						logger.info("getDocumentOperation :: Fetched Document from IM for id {"+documentId+"} And Locale :: > " + locale +" is Checked Out :: >" + imDocumentDetails.isCheckedOut());
						String message = msgProps.addMessage("error.document.checked.out", documentId);
						sessionBean.setErrorMessage(message);
						message= null;
					}
					else
					{
						if(null!=imDocumentDetails.getCategoryList() && imDocumentDetails.getCategoryList().size()>0)
						{
							String[] tokens=null;
							String modelObjectId=null;
							String modelRefKey = null;
							String wmiObjectId=null;
							String wmiRefKey=null;
							String wmiCode=null;
							String vdsObjectId=null;
							String visStartObjectId=null;
							String vdsCode=null;
							String vdsRefKey=null;
							String visStartRefKey=null;
							String visStart=null;
							String visEnd=null;
							String carlineCode=null;
							SIVinDetails fieldDetails = null;
							CarlineDetails carDetails = null;
							for(int i=0;i<imDocumentDetails.getCategoryList().size();i++)
							{
								IMCategoryDetails catImpl = (IMCategoryDetails)imDocumentDetails.getCategoryList().get(i);
								if(null!=catImpl && null!=catImpl.getCategoryRefKey() && !"".equals(catImpl.getCategoryRefKey()))
								{
									/*
									 * WMI / VDS / VIS USED TO BE CARVED OUT OF THE INFOMANAGER OBJECT ID, a dotted path
									 * CARLINE.MODEL.WMI.VDS.VISSTART.VISEND, and only a 6-part id - i.e. a mapping made
									 * at VIS RANGE level - was processed at all.
									 *
									 * Kapture has no object ids, so the same levels are read from the category's PARENT
									 * CHAIN in k_categories, walked root-first:
									 *   [0] CARLINE  [1] MODEL  [2] WMI  [3] VDS  [4] VIS START  [5] VIS END
									 * The size check below is the direct equivalent of the old tokens.length==6, so a
									 * mapping made above VIS level is skipped exactly as it was before.
									 *
									 * SIX LEVELS, NOT FOUR - MNAO hangs its ranges off VIN, MC and MME off CARLINE.
									 */
									ArrayList<IMCategoryDetails> carlineHierarchy = FetchKaptureDataDAO.getCategoryHierarchy(catImpl.getCategoryRefKey(), locale, labelLocale, cmsConn);
									if(null!=carlineHierarchy && carlineHierarchy.size()==6)
									{
										try
										{
											// ONE REF KEY PER LEVEL OF THE CHAIN
											modelRefKey = carlineHierarchy.get(1).getCategoryRefKey();
											wmiRefKey = carlineHierarchy.get(2).getCategoryRefKey();
											vdsRefKey = carlineHierarchy.get(3).getCategoryRefKey();
											visStartRefKey = carlineHierarchy.get(4).getCategoryRefKey();
											
											// IDENTIFY CARLINE CODE
											if(null!=modelRefKey && !"".equals(modelRefKey))
											{
												if(modelRefKey.lastIndexOf("_")!=-1)
												{
													carlineCode = modelRefKey.substring(modelRefKey.lastIndexOf("_")+1, modelRefKey.length());
												}
												
												// IDENTIFY WMI CODE
												if(null!=wmiRefKey && !"".equals(wmiRefKey))
												{
													wmiCode = wmiRefKey.replace(modelRefKey, "");
													if(null!=wmiCode && !"".equals(wmiCode))
													{
														wmiCode  = wmiCode.trim();
														if(wmiCode.trim().equals("___"))
														{
															wmiCode = "-";
														}
													}
												}
											}
											
											// IDENTIFY VDS CODE NAME - AS IT CANNOT BE IDENTIFED FROM REFKEY. THIS IS REQUIRED.
											// the category NAME for the locale - what getCategoryNameOnObjectId() used to return
											vdsCode = carlineHierarchy.get(3).getCategoryName();
											
											// IDENTIFY VIS START RANGE
											if(null!=vdsRefKey && !"".equals(vdsRefKey) && null!=visStartRefKey && !"".equals(visStartRefKey))
											{
												visStart = visStartRefKey.replace(vdsRefKey, "");
												if(null!=visStart && !"".equals(visStart))
												{
													visStart = visStart.trim();
												}
											}
											
											// IDENTIFY VIS END 
											if(null!=visStartRefKey && !"".equals(visStartRefKey))
											{
												visEnd = catImpl.getCategoryRefKey().replace(visStartRefKey, "");
												if(null!=visEnd && !"".equals(visEnd))
												{
													visEnd = visEnd.trim();
												}
											}
											
											/*
											 *  GET MODEL DETAILS - FIRST CHECK ON WMI CODE +VDS CODE IF FOUND USE IT
											 */
											boolean valuesFound = false;
											fieldDetails = new SIVinDetails();
											fieldDetails.setWmiCode(wmiCode);
											fieldDetails.setVdsCode(vdsCode);
											fieldDetails.setCarlineCode(carlineCode);
											fieldDetails = MMESIVinDAO.checkVININMDM(sessionBean.getManualLanguageId(), fieldDetails, conn, connClosed);
											if(null!=fieldDetails && null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
												&& null!=fieldDetails.getModel() && !"".equals(fieldDetails.getModel()))
											{
												valuesFound = true; 
											}
											
											if(valuesFound==false)
											{
												// SET IN FIELD DETAILS
												fieldDetails.setCarlineCode(carlineCode);
												// GET MODEL DETAILS ON THE BASIS OF CARLINE CODE & WMI CODE
												carDetails = MMESIVinDAO.getModel(carlineCode, wmiCode, conn, connClosed);
												if(null!=carDetails)
												{
													fieldDetails.setModel(carDetails.getCarlineNameEng());
													fieldDetails.setModelRegionalName(carDetails.getCarlineNameReg());
												}
											}
										
										}
										catch(Exception e)
										{
											Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "getDocumentOperation()", e);
										}
									}
									
									/*
									 * ADD ALL THE VALUES TO DOCUMENT DETAILS OBJECT
									 */
									SIVinDetails documentDetails = new SIVinDetails();
									documentDetails.setSrNo(sessionBean.getDocumentsList().size()+1);
									documentDetails.setDocumentId(imDocumentDetails.getDocumentId());
									documentDetails.setLocale(imDocumentDetails.getLocale());
									documentDetails.setReferenceKey(catImpl.getCategoryRefKey());
									documentDetails.setDocumentPublished(imDocumentDetails.isDocumentPublished());
									documentDetails.setWmiCode(wmiCode);
									documentDetails.setVdsCode(vdsCode);
									documentDetails.setVinStartRange(visStart);
									documentDetails.setVinEndRange(visEnd);

									if(null!=fieldDetails)
									{
										if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()))
										{
											documentDetails.setCarlineCode(fieldDetails.getCarlineCode());
										}
										if(null!=fieldDetails.getModel() && !"".equals(fieldDetails.getModel()))
										{
											documentDetails.setModel(fieldDetails.getModel());
										}
										if(null!=fieldDetails.getModelRegionalName() && !"".equals(fieldDetails.getModelRegionalName()))
										{
											documentDetails.setModelRegionalName(fieldDetails.getModelRegionalName());
										}
									}
									/*
									 * add DocumentDetails to sessionBean.DocumentsList
									 */
									if(null==sessionBean.getDocumentsList() || sessionBean.getDocumentsList().size()<=0)
									{
										sessionBean.setDocumentsList(new ArrayList<SIVinDetails>());
									}
									sessionBean.getDocumentsList().add(documentDetails);
									documentDetails = null;
									tokens=null;
									modelObjectId=null;
									modelRefKey = null;
									wmiObjectId=null;
									wmiRefKey=null;
									wmiCode=null;
									vdsObjectId=null;
									visStartObjectId=null;
									vdsCode=null;
									vdsRefKey=null;
									visStartRefKey=null;
									visStart=null;
									visEnd=null;
									carlineCode=null;
									fieldDetails = null;
									carDetails = null;
								}
								catImpl=null;
							}
							tokens=null;
							modelObjectId=null;
							modelRefKey = null;
							wmiObjectId=null;
							wmiRefKey=null;
							wmiCode=null;
							vdsObjectId=null;
							visStartObjectId=null;
							vdsCode=null;
							vdsRefKey=null;
							visStartRefKey=null;
							visStart=null;
							visEnd=null;
							carlineCode=null;
							fieldDetails = null;
							carDetails = null;
						}
					}
				}
				else
				{
					logger.info("getDocumentOperation :: Failed to fetch Document from IM for id {"+documentId+"} And Locale :: > " + locale);
					sessionBean.setErrorMessage(msgProps.getProperty("error.fetch.document.im"));
				}
				
				connClosed=null;
				try
				{
					if(null!=conn)
					{
						conn.close();
					}
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(MMESIVin.class.getName(),"getDocumentOperation()",e);
				}
				conn= null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "getDocumentOperation()", e);
		}
	}

	private void removeVIN(HttpServletRequest request, MMESIVinBean sessionBean,String removeFromTranslations)
	{
		try
		{
			if(validateJobProcessing(sessionBean))
			{
				if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
				{
					// IDENTIFY THE VINS TO BE REMOVED
					ArrayList<String> vinsToBeDeleted = new ArrayList<String>();
					ArrayList<SIVinDetails> itemsList = new ArrayList<SIVinDetails>();
					String documentId="";
					String locale="";
					if(null!=sessionBean.getDocumentsList() && sessionBean.getDocumentsList().size()>0)
					{
						for(int i=0;i<sessionBean.getDocumentsList().size();i++)
						{
							SIVinDetails vinDetails = (SIVinDetails)sessionBean.getDocumentsList().get(i);
							if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
							{
								String tokens[] = sessionBean.getSelectedRows().split(",");
								if(null!=tokens && tokens.length>0)
								{
									for(int j=0;j<tokens.length;j++)
									{
										if(String.valueOf(vinDetails.getSrNo()).equals(String.valueOf(tokens[j])))
										{
											/*
											 * PROCEED FOR DELETING VIN 
											 */
											if(null!=vinDetails.getReferenceKey() && !"".equals(vinDetails.getReferenceKey()))
											{
												documentId=vinDetails.getDocumentId();
												locale= vinDetails.getLocale();
												vinsToBeDeleted.add(vinDetails.getReferenceKey());
												
												/*
												 * Prepare ItemDetails
												 */
												SIVinDetails itemDetails = new SIVinDetails();
												itemDetails.setWmiCode(vinDetails.getWmiCode());
												itemDetails.setModel(vinDetails.getModel());
												itemDetails.setModelRegionalName(vinDetails.getModelRegionalName());
												itemDetails.setCarlineCode(vinDetails.getCarlineCode());
												itemDetails.setVdsCode(vinDetails.getVdsCode());
												itemDetails.setVinStartRange(vinDetails.getVinStartRange());
												itemDetails.setVinEndRange(vinDetails.getVinEndRange());
												
												if(null!=vinDetails.getModel() && !"".equals(vinDetails.getModel()) && 
														null!=vinDetails.getCarlineCode() && !"".equals(vinDetails.getCarlineCode()))
												{
													String modelRefKey=vinDetails.getModel();
//													if(vinDetails.getModel().trim().toLowerCase().equals("cx-3") || 
//															vinDetails.getModel().trim().toLowerCase().equals("cx-5") || 
//															vinDetails.getModel().trim().toLowerCase().equals("cx-7") || 
//															vinDetails.getModel().trim().toLowerCase().equals("rx-7") || 
//															vinDetails.getModel().trim().toLowerCase().equals("rx-8"))
//													{
														modelRefKey = modelRefKey.replace("-", "");
//													}
													// add carlineCode
													modelRefKey = modelRefKey.trim().toUpperCase()+"_"+vinDetails.getCarlineCode().trim().toUpperCase();
													itemDetails.setFirstLevelRefKey(Utilities.replaceRefKeys(modelRefKey.trim().toUpperCase()));
													
													if(null!=vinDetails.getWmiCode() && !"".equals(vinDetails.getWmiCode()))
													{
														String wmiPart="";
														if(vinDetails.getWmiCode().equals("-"))
														{
															wmiPart="___";
															
														}
														else
														{
															wmiPart = vinDetails.getWmiCode().trim().toUpperCase();
															itemDetails.setSeconddLevelRefKey(Utilities.replaceRefKeys(modelRefKey.trim().toUpperCase())+vinDetails.getWmiCode());
														}
														
														// 2ND LEVEL
														itemDetails.setSeconddLevelRefKey(Utilities.replaceRefKeys(modelRefKey.trim().toUpperCase())+wmiPart);
														
														if(null!=vinDetails.getVdsCode() && !"".equals(vinDetails.getVdsCode()))
														{
															// 3RD LEVEL
															itemDetails.setThirdLevelRefKey(Utilities.replaceRefKeys(vinDetails.getCarlineCode().trim().toUpperCase()+wmiPart+vinDetails.getVdsCode().trim().toUpperCase()));
															
															if(null!=vinDetails.getVinStartRange() && !"".equals(vinDetails.getVinStartRange()))
															{
																// 4TH LEVEL
																itemDetails.setFourthLevelRefKey(Utilities.replaceRefKeys(vinDetails.getCarlineCode().trim().toUpperCase()+wmiPart+vinDetails.getVdsCode().trim().toUpperCase()+vinDetails.getVinStartRange().trim().toUpperCase()));
															}
														}
														wmiPart = null;
													}
													modelRefKey = null;
												}
												// 5TH LEVEL
												itemDetails.setFifthLevelRefKey(vinDetails.getReferenceKey());
												itemsList.add(itemDetails);
												itemDetails=null;
											}
										}
									}
								}
								tokens=  null;
							}
							vinDetails= null;
						}
					}
					
					if(null!=vinsToBeDeleted && vinsToBeDeleted.size()>0 && 
							null!=documentId && !"".equals(documentId) && null!=locale && !"".equals(locale))
					{
						/*
						 * FETCH THE DOCUMENT FROM INFOMANAGER
						 * REMOVE THE IDENTIFIED VIN FROM THE DOCUMENT
						 * UPDATE THE DOCUMENT FROM INFO MANAGER
						 * 
						 * NOW, CHECK DELETE THE CATEGORIES FROM INFOMANAGER AS WELL
						 * 	CHECK THE VIN IS NOT MAPPED WITH ANY OTHER DOCUMENT - IF YES
						 * 			DELETE THE VIN
						 * 	NOW, CHECK FOR ITS PARENT, 
						 * 		IF DOESN'T HAVE ANY CHILD AND NOT MAPPED WITH ANY OTHER DOCUMENT
						 * 			DELETE THIS AS WELL
						 */
						
						SIVinDetails documentDetails = new SIVinDetails();
						documentDetails.setLocale(locale);
						documentDetails.setWslId(wslId);
						documentDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
						documentDetails.setManualLanguageId(sessionBean.getManualLanguageId());
						if(null!=itemsList && itemsList.size()>0)
						{
							documentDetails.setItemsList(itemsList);
						}
						itemsList = null;
						
						documentDetails = KaptureContentServiceImpl.deleteContent(documentId, locale, wslId, vinsToBeDeleted, documentDetails);
						if(null!=documentDetails && null!=documentDetails.getDocumentId() && !"".equals(documentDetails.getDocumentId()))
						{
							sessionBean.setSuccessMessage(msgProps.addMessage("vin.deleting.success", documentId));
							StringBuilder errorMessage = new StringBuilder();
							/*
							 * if deleteFromTranslations True
							 * 		get all Translations for the document and start deleting VIN from it as well.
							 */
							if(null!=removeFromTranslations && removeFromTranslations.equals("Yes"))
							{
								List<String> translationLocalesList = MMESIVinDAO.getDocumentTranslations(documentId);
								String errorTranslationLocales="";
								if(null!=translationLocalesList && translationLocalesList.size()>0)
								{
									SIVinDetails tempDocDetails = new SIVinDetails();
									for(int e=0;e<translationLocalesList.size();e++)
									{
										tempDocDetails=  new SIVinDetails();
										tempDocDetails = KaptureContentServiceImpl.deleteContent(documentId, String.valueOf(translationLocalesList.get(e)), wslId, vinsToBeDeleted, tempDocDetails);
										if(null!=tempDocDetails && null!=tempDocDetails.getDocumentId() && !"".equals(tempDocDetails.getDocumentId()))
										{
											// success case
										}
										else
										{
											errorTranslationLocales +=String.valueOf(translationLocalesList.get(e))+" ,";
										}
										tempDocDetails = null;
									}
								}
								translationLocalesList = null;
								
								if(null!=errorTranslationLocales && !"".equals(errorTranslationLocales))
								{
									if(errorTranslationLocales.endsWith(" ,"))
									{
										errorTranslationLocales = errorTranslationLocales.substring(0, errorTranslationLocales.length()-2);
									}
									errorMessage.append("Failed to remove selected VIN(s) from the following Translation Locales :: > "+ errorTranslationLocales.trim()+".");
								}
								errorTranslationLocales = null;
							}
							
							/*
							 * call function to save the details in database
							 */
							boolean  bool = MMESIVinDAO.deleteVINDetails(documentDetails);
							if(bool==false)
							{
								if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
								{
									errorMessage.append("<MSG_TOKEN>");
								}
								errorMessage.append(msgProps.getProperty("error.failure.save.deletevin.mapping.db"));
							}
							
							
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								sessionBean.setErrorMessage(errorMessage.toString());
							}
							errorMessage = null;
							/*
							 * CALL FUNCTION TO DELETE THE VINS AS CATEGORIES FROM DOCUMENT.
							 */
							
							sessionBean.setDocumentsList(new ArrayList<SIVinDetails>());
							/*
							 * call function to reload the document details
							 */
							getDocumentOperation(sessionBean, sessionBean.getSiNumber().trim().toUpperCase(), locale);
						}
						else
						{
							sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
						}
						documentDetails=null;
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.actual.delete"));
					}
					vinsToBeDeleted = null;
					documentId= null;
					locale= null;
				}
				else
				{
					sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.actual.delete"));
				}
				// empty selectedRows
				sessionBean.setSelectedRows(null);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "removeVIN()", e);
		}
	}
	
	private  void executeExcelOperation(MMESIVinBean sessionBean, byte[] data, String extension)
	{
		boolean scheduleJob=true;
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
				
				String duplicateRowNo="";
				int errorCount=0;
				int duplicateRowsCount=0;
				int failureCount=0;
				int successCount=0;
				ArrayList<SIVinDetails> listToSave = new ArrayList<SIVinDetails>();
				for(int i=0;i<sessionBean.getVinToImportList().size();i++)
				{
					SIVinDetails fieldDetails = (SIVinDetails)sessionBean.getVinToImportList().get(i);
					fieldDetails.setSrNo((i+1+1));
					boolean addToList = true;
					if(null!=listToSave && listToSave.size()>0)
					{
						for(int j=0;j<listToSave.size();j++)
						{
							SIVinDetails existingDetails = (SIVinDetails)listToSave.get(j);
							if(fieldDetails.getCarlineCode().equals(existingDetails.getCarlineCode()) && 
									fieldDetails.getWmiCode().equals(existingDetails.getWmiCode()) 
									&& fieldDetails.getVdsCode().equals(existingDetails.getVdsCode())
									&& fieldDetails.getVinStartRange().equals(existingDetails.getVinStartRange())
									&& fieldDetails.getVinEndRange().equals(existingDetails.getVinEndRange()))
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
						}
					}
					if(addToList==true)
					{
						listToSave.add(fieldDetails);
					}
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
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.vin"), String.valueOf(rows[a])));
						}
					}
					rows = null;
				}
				duplicateRowNo = null;


				if(null!=listToSave && listToSave.size()>0)
				{
					// HERE LOCALE WILL ALWAYS BE EN_UK FOR FETCHING DATA FROM MDM - BUT FOR PROCESSING CATEGORY IN IM USE PROCESSING LOCALE
					String processingLocale = ManualLanguageDAO.getManualLanguageCode(sessionBean.getManualLanguageId().trim());
					if(null!=processingLocale && !"".equals(processingLocale))
					{
						processingLocale = processingLocale.trim();
						processingLocale = processingLocale.replace("-", "_");
					}
					String locale = ApplicationProperties.getProperty("en_uk").trim();
					if(null!=locale && !"".equals(locale))
					{
						locale = locale.trim();
						locale = locale.replace("-", "_");
					}
					ArrayList<SIVinDetails> listToBeFinallyProcessed = new ArrayList<SIVinDetails>();
					ArrayList<String> modelYearsList = new ArrayList<String>();
					ArrayList<String> modelYearDoesNotExistsinIMList = new ArrayList<String>();
					String errorCarlineCodeNo="";
					String errorModelNo="";

					Connection conn = null;
					String connClosed="N";
					try
					{
						conn = DBConnectionHelper.getConnection();
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "executeExcelOperation()", e);
					}
					/*
					 * ITERATE AND CHECK FOR EACH ROW WHETHER MODEL EXISTS OR NOT
					 * IF EXISTS - THEN PROCEED FOR ADDING VIN TO THE DOCUMENT
					 * ELSE SKIP IT STATING MODEL DOESN'T EXISTS FOR THE ROW NO.
					 */
					for(int i=0;i<listToSave.size();i++)
					{
						SIVinDetails fieldDetails = (SIVinDetails)listToSave.get(i);
						/*
						 * CALL FUNCTION TO FETCH THE MODEL DETAILS ON THE BASIS OF WMI & VDS CODES
						 */
						try
						{
							// USE EN-UK LOCALE HERE
							fieldDetails = MMESIVinDAO.checkVININMDM(locale, fieldDetails, conn, connClosed); 
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "executeExcelOperation()", e);
						}
						
						if(null!=fieldDetails && null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
								&& null!=fieldDetails.getModel() && !"".equals(fieldDetails.getModel()))
						{
							// add Model to Model year List
							String modelRefKey = fieldDetails.getModel().trim();
//							if(carDetails.getCarlineNameEng().trim().toLowerCase().equals("cx-3") || 
//									carDetails.getCarlineNameEng().trim().toLowerCase().equals("cx-5") || 
//									carDetails.getCarlineNameEng().trim().toLowerCase().equals("cx-7") || 
//									carDetails.getCarlineNameEng().trim().toLowerCase().equals("rx-7") || 
//									carDetails.getCarlineNameEng().trim().toLowerCase().equals("rx-8"))
//							{
								modelRefKey=modelRefKey.replace("-", "");
//							}
							// add carlineCode
							modelRefKey=modelRefKey.trim()+"_"+fieldDetails.getCarlineCode().trim();
							modelRefKey = Utilities.replaceRefKeys(modelRefKey.trim().toUpperCase());
							
							boolean addToList = true;
							if(null!=modelYearsList && modelYearsList.size()>0)
							{
								for(String exitRef: modelYearsList)
								{
									if(exitRef.trim().toLowerCase().equals(modelRefKey.trim().toLowerCase()))
									{
										addToList=  false;
										break;
									}
								}
							}
							if(addToList==true)
							{
								modelYearsList.add(modelRefKey);
							}
							modelRefKey= null;
							
							// PROCEED FURTHER FOR MAPPING WITH DOCUMENT
							// set Levels
							// Model
							if(null!=fieldDetails.getModelRegionalName() && !"".equals(fieldDetails.getModelRegionalName()))
							{
								fieldDetails.setFirstLevelName(fieldDetails.getModelRegionalName().trim()+" "+fieldDetails.getCarlineCode().trim());
							}
							else
							{
								fieldDetails.setFirstLevelName(fieldDetails.getModel().trim()+" "+fieldDetails.getCarlineCode().trim());
							}
							String modelYearRefKey=fieldDetails.getModel().trim();
//							if(fieldDetails.getModel().trim().toLowerCase().equals("cx-3") || 
//									fieldDetails.getModel().trim().toLowerCase().equals("cx-5") || 
//									fieldDetails.getModel().trim().toLowerCase().equals("cx-7") || 
//									fieldDetails.getModel().trim().toLowerCase().equals("rx-7") || 
//									fieldDetails.getModel().trim().toLowerCase().equals("rx-8"))
//							{
								modelYearRefKey = modelYearRefKey.replace("-", "");
//							}
							// add carlineCode
							modelYearRefKey=modelYearRefKey.trim()+"_"+fieldDetails.getCarlineCode().trim();
							//fieldDetails.setFirstLevelRefKey(Utilities.replaceRefKeys(fieldDetails.getModel().trim()+"_"+fieldDetails.getCarlineCode().trim()));
							fieldDetails.setFirstLevelRefKey(Utilities.replaceRefKeys(modelYearRefKey.trim()));
							
							String wmiPart="";
							if(fieldDetails.getWmiCode().equals("-"))
							{
								wmiPart="___";
							}
							else
							{
								wmiPart=fieldDetails.getWmiCode().trim().toUpperCase();
							}
							
							// WMI
							fieldDetails.setSecondLevelName(fieldDetails.getWmiCode().trim().toUpperCase());
							//fieldDetails.setSeconddLevelRefKey(Utilities.replaceRefKeys(fieldDetails.getModel().trim()+"_"+fieldDetails.getCarlineCode().trim())+"___");
							fieldDetails.setSeconddLevelRefKey(Utilities.replaceRefKeys(modelYearRefKey.trim())+wmiPart);
							
							// VDS
							fieldDetails.setThirdLevelName(fieldDetails.getVdsCode().trim().toUpperCase());
							fieldDetails.setThirdLevelRefKey(Utilities.replaceRefKeys(fieldDetails.getCarlineCode().trim().toUpperCase()+wmiPart+fieldDetails.getVdsCode().trim().toUpperCase()));
							// VIS START
							fieldDetails.setFourthLevelName(fieldDetails.getVinStartRange().trim().toUpperCase());
							fieldDetails.setFourthLevelRefKey(Utilities.replaceRefKeys(fieldDetails.getCarlineCode().trim().toUpperCase()+wmiPart+fieldDetails.getVdsCode().trim().toUpperCase()+fieldDetails.getVinStartRange().trim().toUpperCase()));
							// VIS END
							if(fieldDetails.getWmiCode().equals("-"))
							{
								fieldDetails.setFifthLevelName(fieldDetails.getModelRegionalName()+" "+fieldDetails.getCarlineCode()+" "+fieldDetails.getVdsCode().trim().toUpperCase()+"-"+fieldDetails.getVinStartRange().trim().toUpperCase()+"-"+fieldDetails.getVinEndRange().trim().toUpperCase());
							}
							else
							{
								fieldDetails.setFifthLevelName(fieldDetails.getModelRegionalName()+" "+fieldDetails.getCarlineCode()+" "+fieldDetails.getWmiCode().trim().toUpperCase()+"-"+fieldDetails.getVdsCode().trim().toUpperCase()+"-"+fieldDetails.getVinStartRange().trim().toUpperCase()+"-"+fieldDetails.getVinEndRange().trim().toUpperCase());
							}
							fieldDetails.setFifthLevelRefKey(Utilities.replaceRefKeys(fieldDetails.getCarlineCode().trim().toUpperCase()+wmiPart+fieldDetails.getVdsCode().trim().toUpperCase()+fieldDetails.getVinStartRange().trim().toUpperCase()+fieldDetails.getVinEndRange().trim().toUpperCase()));
							
							listToBeFinallyProcessed.add(fieldDetails);
							modelYearRefKey = null;
							wmiPart = null;

						}
						else
						{
							// Failed to Locate MDOEL FOR ROW NOW
							if(null!=errorModelNo && !"".equals(errorModelNo))
							{
								errorModelNo = errorModelNo+",";	
							}
							errorModelNo = errorModelNo+String.valueOf(fieldDetails.getSrNo());
						}
						fieldDetails=  null;
					}
					
					connClosed = null;
					try
					{
						if(null!=conn)
						{
							// close connection object
							conn.close();
						}
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "executeExcelOperation()", e);
					}
					conn = null;
					if(null!=errorCarlineCodeNo && !"".equals(errorCarlineCodeNo))
					{
						if(errorCarlineCodeNo.endsWith(","))
						{
							errorCarlineCodeNo= errorCarlineCodeNo.substring(0, errorCarlineCodeNo.length()-1);
						}
						String[] errorRows = errorCarlineCodeNo.split(",");
						if(null!=errorRows && errorRows.length>0)
						{
							for(int a=0;a<errorRows.length;a++)
							{
								// increment errorCount by 1
								errorCount++;
								if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
								{
									errorMessage.append("<MSG_TOKEN>");
								}
								errorMessage.append(msgProps.addMessage("error.import.sivin.failed.carlinecode", String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
					}
					errorCarlineCodeNo = null;
					
					if(null!=errorModelNo && !"".equals(errorModelNo))
					{
						if(errorModelNo.endsWith(","))
						{
							errorModelNo= errorModelNo.substring(0, errorModelNo.length()-1);
						}
						String[] errorRows = errorModelNo.split(",");
						if(null!=errorRows && errorRows.length>0)
						{
							for(int a=0;a<errorRows.length;a++)
							{
								// increment errorCount by 1
								errorCount++;
								if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
								{
									errorMessage.append("<MSG_TOKEN>");
								}
								errorMessage.append(msgProps.addMessage("error.import.sivin.failed.model", String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
					}
					errorModelNo = null;
					
					/*
					 * CHECK IF MODEL YEAR LIST IS NOT NULL, IDENTIFY WHAT ALL EXISTS IN IM AND WHAT ALL DOESN'T
					 */
					/*
					 * WHICH MODEL YEAR CATEGORIES ARE USABLE FOR THIS LOCALE?
					 *
					 * WAS: getCategoryDetails(modelYearsList) - ask InfoManager which reference keys
					 * exist, and report every one that does not.
					 *
					 * NOW: kapture_cms_db.k_categories holds a row PER LOCALE, so a key that exists for
					 * another locale but not this one is NOT missing - it is created for this locale
					 * together with every level above it, through the category REST API. Only a key that
					 * exists NOWHERE is reported. See KaptureCategoryServiceImpl.
					 */
					ArrayList<String> modelYearsNotInKapture = new ArrayList<String>();
					if(null!=modelYearsList && modelYearsList.size()>0)
					{
						String categorySourceLocale = FetchKaptureDataDAO.resolveLabelLocale(locale, "MME");
						modelYearsNotInKapture = KaptureCategoryServiceImpl.ensureCategoriesForLocale(modelYearsList, locale, categorySourceLocale);
						categorySourceLocale = null;
					}
					
					if(null!=modelYearsNotInKapture && modelYearsNotInKapture.size()>0)
					{
						/*
						 * IDENTIFY THE ONES THAT ARE USABLE AND THE ONES THAT ARE NOT
						 */
						for(int a=0;a<modelYearsList.size();a++)
						{
							String refKey = modelYearsList.get(a);
							boolean found = true;
							for(int b=0;b<modelYearsNotInKapture.size();b++)
							{
								if(String.valueOf(modelYearsNotInKapture.get(b)).trim().toLowerCase().equals(refKey.trim().toLowerCase()))
								{
									found=false;
									break;
								}
							}
					
							if(found==false)
							{
								// add to
								modelYearDoesNotExistsinIMList.add(refKey);
							}
						}
					}
					modelYearsNotInKapture = null;
					
					ArrayList<SIVinDetails> listToBeMappedWithDocument = new ArrayList<SIVinDetails>();
					// ITERTAE LIST TO BE PROCESSED FINALLY AND IDENTIFY THE ROWS FOR WHOM MODEL DOES NOT EXISTS.
					if(null!=listToBeFinallyProcessed && listToBeFinallyProcessed.size()>0)
					{
						for(int i=0;i<listToBeFinallyProcessed.size();i++)
						{
							SIVinDetails fieldDetails = (SIVinDetails)listToBeFinallyProcessed.get(i);
							if(null!=fieldDetails.getModel() && !"".equals(fieldDetails.getModel()) && null!=fieldDetails.getCarlineCode() && 
									!"".equals(fieldDetails.getCarlineCode()))
							{
								String modelYearRefKey = fieldDetails.getModel().trim().toUpperCase();
//								if(fieldDetails.getModel().trim().toLowerCase().equals("cx-3") || 
//										fieldDetails.getModel().trim().toLowerCase().equals("cx-5") || 
//										fieldDetails.getModel().trim().toLowerCase().equals("cx-7") || 
//										fieldDetails.getModel().trim().toLowerCase().equals("rx-7") || 
//										fieldDetails.getModel().trim().toLowerCase().equals("rx-8"))
//								{
									modelYearRefKey=modelYearRefKey.replace("-", "");
//								}
								modelYearRefKey = modelYearRefKey.trim().toUpperCase()+"_"+fieldDetails.getCarlineCode().trim().toUpperCase();
								modelYearRefKey = replaceRefKeyChars(modelYearRefKey);
								boolean proceedFurther= true;
								// CHECK IF EXISTS IN MODEL YEAR THAT DOES NOT EXISTS IN IM
								if(null!=modelYearDoesNotExistsinIMList && modelYearDoesNotExistsinIMList.size()>0)
								{
									for(int a=0;a<modelYearDoesNotExistsinIMList.size();a++)
									{
										String failedRefKey = (String)modelYearDoesNotExistsinIMList.get(a);
										if(failedRefKey.trim().toLowerCase().equals(modelYearRefKey.trim().toLowerCase()))
										{
											proceedFurther = false;
											break;
										}
									}
								}
								
								if(proceedFurther==false)
								{
									// increment errorCount by 1
									errorCount++;
									if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
									{
										errorMessage.append("<MSG_TOKEN>");
									}
									errorMessage.append(msgProps.addMessage("error.import.sivin.failed.model.doesnot.exist.im", modelYearRefKey , String.valueOf(fieldDetails.getSrNo())));
								}
								else
								{
									listToBeMappedWithDocument.add(fieldDetails);
								}
								modelYearRefKey = null;
							}
						}
					}
			
					if(null!=listToBeMappedWithDocument && listToBeMappedWithDocument.size()>0)
					{
						/*
						 * ITERATE AND START SAVING EACH ROW
						 * PROCEED FOR CREATING VINS AND MAPPING IT WITH DOCUMENTS.
						 * here, we have WMI CODE, VDS CODE MULTIPLE + VIN RANGE
						 */
						ArrayList<String> categoriesToBeAddedToDocument = new ArrayList<String>();
						/*
						 * ITERATE SELECTED CAT LIST AND ALL WMIS TO LIST
						 */
						ArrayList<IMCategoryDetails> catList = new ArrayList<IMCategoryDetails>();
						
						/*
						 * add all 3rd Level Categories to the IM LIST
						 */
						for(int r=0;r<listToBeMappedWithDocument.size();r++)
						{
							SIVinDetails selItemDetails= (SIVinDetails)listToBeMappedWithDocument.get(r);
							if(null!=selItemDetails.getThirdLevelRefKey() && !"".equals(selItemDetails.getThirdLevelRefKey()))
							{
								/*
								 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
								 */
								boolean addToList = true;
								if(null!=catList && catList.size()>0)
								{
									for(int e=0;e<catList.size();e++)
									{
										IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
										if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getThirdLevelRefKey().trim().toLowerCase()))
										{
											addToList = false;
											break;
										}
									}
								}
								
								if(addToList==true)
								{
									IMCategoryDetails cDetails = new IMCategoryDetails();
									cDetails.setCategoryName(selItemDetails.getThirdLevelName().trim().toUpperCase());
									cDetails.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
									cDetails.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getSeconddLevelRefKey().trim().toUpperCase()));
									// USE PROCESSING LOCALE FOR UPDATING CATEGORIES IN IM
									cDetails.setLocale(processingLocale);
									// SET LEVEL AS LEVEL 3
									cDetails.setLevel(ScheduleConstants.LEVEL_3);
									// set itemDetails
									cDetails.setItemDetails(new SIVinDetails());
									cDetails.setItemDetails(selItemDetails);
									
									catList.add(cDetails);
									cDetails=  null;
								}
							}
						}
						
						
						/*
						 * add all 4th Level Categories to the IM LIST
						 */
						for(int r=0;r<listToBeMappedWithDocument.size();r++)
						{
							SIVinDetails selItemDetails= (SIVinDetails)listToBeMappedWithDocument.get(r);
							if(null!=selItemDetails.getFourthLevelRefKey() && !"".equals(selItemDetails.getFourthLevelRefKey()))
							{
								/*
								 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
								 */
								boolean addToList = true;
								if(null!=catList && catList.size()>0)
								{
									for(int e=0;e<catList.size();e++)
									{
										IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
										if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getFourthLevelRefKey().trim().toLowerCase()))
										{
											addToList = false;
											break;
										}
									}
								}
								
								if(addToList==true)
								{
									IMCategoryDetails cDetails = new IMCategoryDetails();
									cDetails.setCategoryName(selItemDetails.getFourthLevelName().trim().toUpperCase());
									cDetails.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getFourthLevelRefKey().trim().toUpperCase()));
									cDetails.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
									// USE PROCESSING LOCALE FOR UPDATING CATEGORIES IN IM
									cDetails.setLocale(processingLocale);
									// SET LEVEL AS LEVEL 4
									cDetails.setLevel(ScheduleConstants.LEVEL_4);
									// set itemDetails
									cDetails.setItemDetails(new SIVinDetails());
									cDetails.setItemDetails(selItemDetails);
									
									catList.add(cDetails);
									cDetails=  null;
								}
							}
						}
						
						/*
						 * add all 5th Level Categories to the IM LIST
						 */
						for(int r=0;r<listToBeMappedWithDocument.size();r++)
						{
							SIVinDetails selItemDetails= (SIVinDetails)listToBeMappedWithDocument.get(r);
							if(null!=selItemDetails.getFifthLevelRefKey() && !"".equals(selItemDetails.getFifthLevelRefKey()))
							{
								/*
								 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
								 */
								boolean addToList = true;
								if(null!=catList && catList.size()>0)
								{
									for(int e=0;e<catList.size();e++)
									{
										IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
										if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getFifthLevelRefKey().trim().toLowerCase()))
										{
											addToList = false;
											break;
										}
									}
								}
								
								if(addToList==true)
								{
									IMCategoryDetails cDetails = new IMCategoryDetails();
									cDetails.setCategoryName(selItemDetails.getFifthLevelName().trim().toUpperCase());
									cDetails.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getFifthLevelRefKey().trim().toUpperCase()));
									cDetails.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getFourthLevelRefKey().trim().toUpperCase()));
									cDetails.setLocale(processingLocale);
									// SET LEVEL AS LEVEL 5
									cDetails.setLevel(ScheduleConstants.LEVEL_5);
									// set itemDetails
									cDetails.setItemDetails(new SIVinDetails());
									cDetails.setItemDetails(selItemDetails);
									catList.add(cDetails);
									
									/*
									 * add VIN Ranges to categoriesToBeAddedToDocument
									 */
									categoriesToBeAddedToDocument.add(Utilities.replaceRefKeys(selItemDetails.getFifthLevelRefKey().trim().toUpperCase()));
									cDetails = null;
								}
							}
						}
						
						/*
						 * CHECK HERE IF CATLIST SIZE IS MORE THAN DEFINED LIMIT THEN DO NOT PROCEED FOR MAPPING IT RIGHT AWAY
						 * SCHEDULE A JOB AND PERFORM REST OF THE ACTIVITIES THERE
						 */
						
						
						if(null!=catLoadLimit && !"".equals(catLoadLimit))
						{
							// user categoriesToBeAddedToDocument because it contains only 5th levels
							if(null!=categoriesToBeAddedToDocument && categoriesToBeAddedToDocument.size()>0)
							{
								try
								{
									if(categoriesToBeAddedToDocument.size()<= new Integer(catLoadLimit).intValue())
									{
										/*
										 *  do not schedule Job as no of categories to be processed are less than 
										 *  or equals to defined limit.
										 */
										scheduleJob = false;
									}
								}
								catch(Exception e)
								{
									Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "executeExcelOperation()", e);
								}
							}
						}
						
						if(scheduleJob==false)
						{
							/*
							 * NO NEED OF MAPPING MODEL HERE, SINCE VIN IS A CHILD OF CARLINE IN MC
							 * ALSO ITERATE AND ADD EACH MODEL YEAR TO DOCUMENT
							 */
							if(null!=catList && catList.size()>0)
							{
								/*
								 * call function to create NewCategories in IM.
								 */
								createCategory(catList, processingLocale, wslId);
							}
							catList = null;
							
							SIVinDetails documentDetails = new SIVinDetails();
							documentDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
							documentDetails.setManualLanguageId(sessionBean.getManualLanguageId());
							documentDetails.setLocale(processingLocale);
							documentDetails.setWslId(wslId);
							if(null!=listToBeMappedWithDocument && listToBeMappedWithDocument.size()>0)
							{
								documentDetails.setItemsList(new ArrayList<SIVinDetails>());
								documentDetails.setItemsList(listToBeMappedWithDocument);
								successCount= listToBeMappedWithDocument.size();
							}
							/*
							 * NOW CALL FUNCTION TO ADD THE NEW CATEGORIES TO THE DOCUMENT
							 */
							documentDetails = KaptureContentServiceImpl.modifyContent(sessionBean.getSiNumber().trim(), processingLocale, wslId, categoriesToBeAddedToDocument, documentDetails);
							if(null!=documentDetails  && null!=documentDetails.getDocumentId() && !"".equals(documentDetails.getDocumentId()))
							{
								sessionBean.setSuccessMessage(msgProps.addMessage("import.vin.mapping.success", String.valueOf(successCount), sessionBean.getSiNumber()));
								sessionBean.setDocumentsList(new ArrayList<SIVinDetails>());
								sessionBean.setTempVinsList(null);
								/*
								 * call function to save the Data in database
								 */
								boolean bool = MMESIVinDAO.addVINDetails(documentDetails, null, "Y");
								if(bool==false)
								{
									// increment errorCount by 1
									errorCount++;
									if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
									{
										errorMessage.append("<MSG_TOKEN>");
									}
									errorMessage.append(msgProps.getProperty("error.failure.save.addvin.mapping.db"));
								}
								/*
								 * call function to reload the document details
								 */
								getDocumentOperation(sessionBean, sessionBean.getSiNumber().trim().toUpperCase(), processingLocale);
							}
							else
							{	
								logger.info("executeExcelOperation :: Either Document Details or New Document id is null Or User Not Authroized after trying to update in IM.");
//								sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
								// increment errorCount by 1
								errorCount++;
								if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
								{
									errorMessage.append("<MSG_TOKEN>");
								}
								if(null!=documentDetails && null!=documentDetails.getErrorCode() && documentDetails.getErrorCode().equals("7000"))
								{
									// APPEND ERROR - USER NOT AUTHROIZED 
									errorMessage.append(msgProps.getProperty("error.user.not.authorized.modify.content"));
								}
								else
								{
									// APPEND GENERIC ERROR
									errorMessage.append(msgProps.getProperty("error.generic"));
								}
							}
							documentDetails = null;
						}
						else
						{
							if(null!=catList && catList.size()>0)
							{
								/*
								 * SCHEDULE A JOB, NAVIGATE USER TO HISTORY PAGE FOR VIEWING PROCESSING
								 */
								SIVINScheduleDetails schDetails = new SIVINScheduleDetails();
								schDetails.setScheduleName(ApplicationProperties.getProperty("mme.schedule.name.key"));
								schDetails.setLocale(processingLocale);
								schDetails.setDocumentId(sessionBean.getSiNumber().trim().toUpperCase());
								/*
								 * TOTAL IS THE NUMBER OF VIN RANGES, i.e. the LEAF categories - which is exactly what
								 * categoriesToBeAddedToDocument holds. It was catList.size()/3, on the assumption that a
								 * VIN row always contributes three levels. It does not: catList carries every level that
								 * has to be CREATED and PARENTS ARE SHARED between rows, so the ratio moves with the data.
								 * Six VIN ranges over two VDS codes produced fourteen categories, and 14/3 truncated to a
								 * reported total of 4 against 6 processed and 6 mapped (schedule 25382, SI1077 / ja_JP).
								 *
								 * MNAO counts the same thing with its own totalCountForJob counter, incremented where the
								 * leaf is added. Original line kept below for reference.
								 *
								 * schDetails.setTotalCount((catList.size() / 3));
								 */
								schDetails.setTotalCount(categoriesToBeAddedToDocument.size());
								schDetails.setCategoryList(catList);
								schDetails.setWslId(wslId);
								schDetails.setJobStatus(ScheduleConstants.STATUS_PENDING);
								// call function to schedule a Job in database
								long scheduleId = SIVINBatchTransactionDAO.createSchedule(schDetails);
								if(scheduleId>0)
								{
									logger.info("executeExcelOperation :: Category Creation JOB Scheduled successfully. Navigate user to History Page Showing JOB Name.");
									// UPDATE SCHDULE NAME AND THREAD ID
									schDetails.setScheduleName(schDetails.getScheduleName()+String.valueOf(scheduleId));
									schDetails.setThreadId(schDetails.getScheduleName());
									
									// show success message - Job scheduled successfully
									sessionBean.setSuccessMessage(msgProps.addMessage("schedule.success", schDetails.getScheduleName()));
									// set scheduleName in sessionBean
									sessionBean.setScheduleName(schDetails.getScheduleName());
									/*
									 * START A PARALLEL THREAD.
									 */
									/*
									 * HERE DOCUMENT LOCALE WILL THE ORIGINAL LOCALE SELCETED.
									 * THIS IS REQUIRED FOR ADDING VIN DETAILS FOR THE PROCESSING DOCUMENT FROM JOB
									 */
									final SIVinDetails documentDetails = new SIVinDetails();
									documentDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
									documentDetails.setManualLanguageId(sessionBean.getManualLanguageId());
									//	documentDetails.setLocale(locale);
									documentDetails.setLocale(processingLocale);
									documentDetails.setWslId(wslId);
									if(null!=listToBeMappedWithDocument && listToBeMappedWithDocument.size()>0)
									{
										documentDetails.setItemsList(new ArrayList<SIVinDetails>());
										documentDetails.setItemsList(listToBeMappedWithDocument);
									}
									
									String translationUpdate=null;
									if(sessionBean.isShowTranslationOperationsControl()==true)
									{
										// en_EU Locale - take the current selected Value
										translationUpdate = sessionBean.getTranslationsUpdate();
									}
									else
									{
										// always send this as No
										translationUpdate = ApplicationProperties.getProperty("value.esicategory.flag.no");
									}
									final String trnalsationUpdateFlag = translationUpdate;
									translationUpdate = null;
									
									final String schId=String.valueOf(scheduleId);
									final MMESIVINBatchProcessingImpl catProImpl = new MMESIVINBatchProcessingImpl();
									Runnable runn = new Runnable() 
									{
										@Override
										public void run() {
											
											synchronized (catProImpl) {
												try {
													catProImpl.startProcess(schId, documentDetails,"MME",trnalsationUpdateFlag);
												} catch (Exception e) {
													Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "run()", e);
												}
											}
										}
									};
									
									Thread th = new Thread(runn, schDetails.getThreadId());
									th.start();
								}
								else
								{
									logger.info("executeExcelOperation :: Failed to create schedule for Category Creation. Throw Generic error");
									// ADD GENERIC ERROR
									sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
								}
								schDetails  = null;
							}
						}
						
						categoriesToBeAddedToDocument = null;
						catList = null;
					}
					listToBeMappedWithDocument = null;
					locale = null;
					processingLocale = null;
					listToBeFinallyProcessed = null;
					modelYearsList = null;
					modelYearDoesNotExistsinIMList = null;
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
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.vin")));
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
				
				if(scheduleJob==false)
				{
					// do this only when document is updated from template
					failureCount = sessionBean.getVinToImportList().size()-successCount;
					if(failureCount>0)
					{
						if(null!=mess && !"".equals(mess))
						{
							mess = mess+" ";
						}
						mess = mess+msgProps.addMessage("error.excel.failure.rows.count", String.valueOf(failureCount));
					}

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
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "executeExcelOperation()", e);
		}
	}

	/**
	 * Function will identify the Cell Value Type
	 * and accordingly will read their values and will return
	 * @param cell
	 * @return
	 */
	/**
	 * READS A FIXED-WIDTH CODE FROM A CELL, KEEPING ITS LEADING ZEROS.
	 *
	 * An MME VIS range is a 6-CHARACTER CODE, not a number - the import validates it with
	 * length()!=6. Excel does not know that: type 000000 into a cell and it stores the NUMBER 0,
	 * so readCellValue() hands back "0" and the range is neither what the user entered nor 6
	 * characters long. The same applies to any range with leading zeros - 000123 comes back "123".
	 *
	 * NOTE THIS IS NOT DONE FOR MC. There a VIS range is a FREE-LENGTH field with only a MAXIMUM
	 * (7, or 5 for Cosmo Sport) - the exact-length check is deliberately commented out there - so
	 * there is no width to pad to and padding would fabricate characters.
	 *
	 * Padding is applied ONLY to a NUMERIC cell. A cell the user formatted as TEXT already holds
	 * exactly what was typed, so "123" there is a genuine 3-character entry and must stay wrong,
	 * to be reported by the length check rather than silently corrected into "000123".
	 *
	 * @param width the exact number of characters the code must have (6 for an MME VIS range)
	 * @return the cell value left-padded with zeros, or null when the cell is empty
	 */
	private String readFixedWidthCode(Cell cell, int width)
	{
		Object value = readCellValue(cell);
		if(null==value)
		{
			return null;
		}
		String text = String.valueOf(value).trim();
		if("".equals(text) || text.length()>=width)
		{
			return text;
		}
		if(null==cell || cell.getCellType()!=Cell.CELL_TYPE_NUMERIC)
		{
			// a text cell holds what was typed - do not "correct" it
			return text;
		}
		for(int i=0;i<text.length();i++)
		{
			if(!Character.isDigit(text.charAt(i)))
			{
				// not a number Excel could have stripped zeros from
				return text;
			}
		}
		StringBuffer padded = new StringBuffer();
		for(int i=text.length();i<width;i++)
		{
			padded.append('0');
		}
		padded.append(text);
		logger.info("readFixedWidthCode :: Excel stored {"+text+"} as a number - restored to {"
				+padded.toString()+"}");
		return padded.toString();
	}

	private  Object readCellValue(Cell cell)
	{
		Object cellValue = null;
		try {
			if (null != cell) {
				/*
				 * check for Cell Type and format accordingly
				 */
				if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
					cellValue = cell.getNumericCellValue();

					/*
					 * Check here, if callValue is 0; THEN return "" as object
					 * else, check for decimal value in cell
					 * if decimal value is .0 or .00, then remove it
					 * if more than 0, then pass the value as it is
					 */

					String val = String.valueOf(cellValue);
					if(null!=val && !"".equals(val))
					{
						/*
						 *  check if val has any value in point to decimal and that value is 0 then remove the decimal Value
						 */
						if(val.lastIndexOf(".")!=-1)
						{
							String decimal = val.substring(val.lastIndexOf(".")+1,val.length());
							if(null!=decimal && !"".equals(decimal))
							{
								Double dec = new Double(decimal).doubleValue();
								if(dec==0)
								{
									// remove decimal from the val and value after decimal
									val = val.substring(0,val.lastIndexOf("."));
								}
								else
								{
									// GET THE NUMERIC VALUE IN THE FORMAT ########################## AND PASS IT
									CellNumberFormatter cn = new CellNumberFormatter("################################");
									val = cn.format(cell.getNumericCellValue());
									cn = null;
								}
								// set dec to null
								dec = null;
							}
							// set decimal to null
							decimal = null;
						}
					}

					// set cellValue as val
					cellValue = val;
					// set val to null
					val  = null;
				}
				else if(cell.getCellType() == Cell.CELL_TYPE_FORMULA)
				{
					cellValue = cell.getCellFormula();
					if(null!=cellValue)
					{
						// append a = with this formaula
						cellValue = "="+cellValue;
					}
				}
				else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
					cellValue = cell.getStringCellValue();
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "readCellValue()", e);
			// set cellValue to null
			cellValue = null;
		}
		return cellValue;
	}

	private void readExcelData(byte[] data, MMESIVinBean sessionBean, String extension)
	{
		sessionBean.setVinToImportList(new ArrayList<SIVinDetails>());
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
				 * WMI CODE
				 * VDS CODE
				 * VIS START RANGE
				 * VIS END RANGE
				 */
				
				while(null!=rowIterator && rowIterator.hasNext())
				{
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						SIVinDetails details = new SIVinDetails();
						Object dataCell = readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setCarlineCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setWmiCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setVdsCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * VIS RANGES ARE 6-CHARACTER CODES, NOT NUMBERS - read through
						 * readFixedWidthCode so Excel's "000000 is the number 0" does not turn a
						 * range into "0".
						 */
						String visCode = readFixedWidthCode(row.getCell(3), 6);
						if(null!=visCode && !"".equals(visCode))
						{
							details.setVinStartRange(visCode);
						}
						visCode = null;

						visCode = readFixedWidthCode(row.getCell(4), 6);
						if(null!=visCode && !"".equals(visCode))
						{
							details.setVinEndRange(visCode);
						}
						visCode = null;
						

						/*
						 * add details to categoryList for import
						 */
						if(null==sessionBean.getVinToImportList() || sessionBean.getVinToImportList().size()<=0)
						{
							sessionBean.setVinToImportList(new ArrayList<SIVinDetails>());
						}
						
						sessionBean.getVinToImportList().add(details);
						details= null;
					}
					// INCREMENT ROW COUNT BY 1
					rowCount++;
					row = null;
				}
				sheet = null;
				xlsSheet = null;
				xlsWorkBook = null;
				workbook = null;
				rowIterator=  null;
				is.close();
				is = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "readExcelData()", e);
		}
	}

	private  boolean validateExcelRowData(MMESIVinBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		if(null!=sessionBean.getVinToImportList() && sessionBean.getVinToImportList().size()>0)
		{
			for(int i=0;i<sessionBean.getVinToImportList().size();i++)
			{
				SIVinDetails fieldDetails = (SIVinDetails) sessionBean.getVinToImportList().get(i);
				// EXTRA 1 BECAUSE WHILE READING EXCEL, HEADER ROW WAS SKIPPED
				int rowNo = i+1+1;


				if(null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) || 
						null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) ||
						null==fieldDetails.getVdsCode() || "".equals(fieldDetails.getVdsCode()) ||
						null==fieldDetails.getVinStartRange() || "".equals(fieldDetails.getVinStartRange()) ||
						null==fieldDetails.getVinEndRange() || "".equals(fieldDetails.getVinEndRange()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					/*
					 * INCOMPLETE DATA FOR VIN AT ROW NO . rowNo
					 */
					String data = msgProps.getProperty("label.vin")+","+String.valueOf(rowNo);
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
					data = null;
					id = null;
					// increment errorCount by 1
					errorCount++;
				}

				if(null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()))
				{
					if(fieldDetails.getCarlineCode().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.carcode")+",20,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				if(null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()))
				{
					if(fieldDetails.getWmiCode().length()>5)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.wmicode")+",20,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()))
				{
					if(fieldDetails.getVdsCode().length()>8)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.vdscode")+",8,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}
				
				/*
				 * to fetch model for MME both CARLINE & WMI REQUIRED
				 */
				int visRangeChars=6;
				if(null!=fieldDetails.getVinStartRange() && !"".equals(fieldDetails.getVinStartRange()))
				{
					if(fieldDetails.getVinStartRange().length()!=visRangeChars)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.visstartrange")+","+visRangeChars+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.exact.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getVinEndRange() && !"".equals(fieldDetails.getVinEndRange()))
				{
					if(fieldDetails.getVinEndRange().length()!=visRangeChars)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.visendrange")+","+visRangeChars+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.exact.characters.for.row"));
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
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.vin")));
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

	private  void decideErrorDisplay(MMESIVinBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_MME_SIVIN_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "validateExcelRowData()", e);
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

	private  void exportMMESIVinDetails(MMESIVinBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<SIVinDetails> exportDataList = new ArrayList<SIVinDetails>();
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getDocumentsList() && !"".equals(sessionBean.getDocumentsList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getDocumentsList().size();a++)
							{
								SIVinDetails details = (SIVinDetails)sessionBean.getDocumentsList().get(a);
								if(rowId.equals(String.valueOf(details.getSrNo())))
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
					writeMMESIVinExcel(exportDataList, sessionBean);
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
					logger.info("exportMMESIVinDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportMMESIVinDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "exportMMESIVinDetails()", e);
		}
	}

	private void writeMMESIVinExcel(ArrayList<SIVinDetails> sivinList, MMESIVinBean sessionBean)
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
				 * add SELECTED COUNTRY LOCALE CODE, ADD MANUAL LANGUAGE CODE, ADD MODEL CODE
				 * add Current Time Stamp in Format - DDMMYYYY HHMMSS
				 * 
				 * So the final Name will be - US_EN-US_ND_VIN_DDMMYYYY_HHMMSS.XSLX
				 */
				
				String countryLocaleCode = "";
				if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()))
				{
					countryLocaleCode = CountryLocaleDAO.getCountryLocaleCode(sessionBean.getCountryLocaleId());
					if(null!=countryLocaleCode && !"".equals(countryLocaleCode))
					{
						name= countryLocaleCode.trim().toUpperCase();
					}
				}
				countryLocaleCode = null;
				String manualLanguageCode="";
				if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
				{
					manualLanguageCode = ManualLanguageDAO.getManualLanguageCode(sessionBean.getManualLanguageId());
					if(null!=manualLanguageCode && !"".equals(manualLanguageCode))
					{
						if(null!=name && !"".equals(name))
						{
							name = name.trim()+"_";
						}
						name = name.trim()+manualLanguageCode.trim().toUpperCase();
					}
				}
				manualLanguageCode = null;
				
				if(null!=name && !"".equals(name))
				{
					name = name.trim()+"_";
				}
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_MME_SIVIN_NAME");
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
				
				Cell carlineCodeCell = headerRow.createCell(0);
				carlineCodeCell.setCellValue("DOCUMENT ID");
				Cell carNameEngCell = headerRow.createCell(1);
				carNameEngCell.setCellValue("MODEL");
				Cell carNameRegCell = headerRow.createCell(2);
				carNameRegCell.setCellValue("CARLINE CODE");
				Cell wmiCodeCell = headerRow.createCell(3);
				wmiCodeCell.setCellValue("WMI CODE");
				Cell vdsCodeCell = headerRow.createCell(4);
				vdsCodeCell.setCellValue("VDS CODE");
				Cell visStartRangeCell = headerRow.createCell(5);
				visStartRangeCell.setCellValue("VIS START RANGE");
				Cell visEndRangeCell = headerRow.createCell(6);
				visEndRangeCell.setCellValue("VIS END RANGE");
				
				
				int rowCount=0;
				if(null!=sivinList && sivinList.size()>0)
				{
					for(int i=0;i<sivinList.size();i++)
					{
						SIVinDetails details = (SIVinDetails)sivinList.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);
						
						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						Cell cell3 = row.createCell(3);
						Cell cell4 = row.createCell(4);
						Cell cell5 = row.createCell(5);
						Cell cell6 = row.createCell(6);
						
						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						cell3.setCellValue("");
						cell4.setCellValue("");
						cell5.setCellValue("");
						cell6.setCellValue("");
						
						if(null!=details.getDocumentId() && !"".equals(details.getDocumentId()))
						{
							cell0.setCellValue(details.getDocumentId());
						}
						if(null!=details.getModelRegionalName() && !"".equals(details.getModelRegionalName()))
						{
							cell1.setCellValue(details.getModelRegionalName());
						}
						else if(null==details.getModelRegionalName() || "".equals(details.getModelRegionalName()))
						{
							if(null!=details.getModel() && !"".equals(details.getModel()))
							{
								cell1.setCellValue(details.getModel());
							}
						}
						if(null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()))
						{
							cell2.setCellValue(details.getCarlineCode());
						}
						if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
						{
							cell3.setCellValue(details.getWmiCode());
						}
						if(null!=details.getVdsCode() && !"".equals(details.getVdsCode()))
						{
							cell4.setCellValue(details.getVdsCode());
						}
						if(null!=details.getVinStartRange() && !"".equals(details.getVinStartRange()))
						{
							cell5.setCellValue(details.getVinStartRange());
						}
						if(null!=details.getVinEndRange() && !"".equals(details.getVinEndRange()))
						{
							cell6.setCellValue(details.getVinEndRange());
						}
						cell0 = null;
						cell1 = null;
						cell2 = null;
						cell3 = null;
						cell4 = null;
						cell5= null;
						cell6 = null;
						row = null;
						details = null;
					}
					
					headerRow =  null;
					carlineCodeCell = null;
					carNameEngCell = null;
					carNameRegCell = null;
					carlineCodeCell=null;
					wmiCodeCell= null;
					vdsCodeCell= null;
					visEndRangeCell = null;
					visStartRangeCell = null;
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
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "writeMMESIVinExcel()", e);
		}
	}
	
	
	/**
	 * Function to establish connection with IM for the logged in user
	 * @param client
	 * @param userId
	 * @return
	 */
	/**
	 * CREATE OR UPDATE THE CARLINE CATEGORIES FOR A MAPPING.
	 *
	 * WAS: createCategoryOperation() per category - getCategory() against InfoManager, then
	 * addCategory or updateCategory on the IQ client.
	 * NOW: one call into KaptureCategoryServiceImpl, which makes the same add-or-update decision
	 * from kapture_cms_db.k_categories and carries it out through the category REST API.
	 * kapture_cms_db grants SELECT only, so the write cannot be a direct INSERT.
	 *
	 * THE LIST IS ALREADY PARENT-BEFORE-CHILD - this screen builds it WMI, then VDS, then VIS
	 * range - which is the order the rows have to be written in, so it is passed through as-is.
	 */
	private void createCategory(ArrayList<IMCategoryDetails> categoryList, String locale, String wslId)
	{
		try
		{
			if(null!=categoryList && categoryList.size()>0)
			{
				KaptureCategoryServiceImpl.createCategories(categoryList, locale, wslId);
			}
		}
		catch (Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVin.class.getName(), "createCategory()", e);
		}
	}

	private boolean validateJobProcessing(MMESIVinBean sessionBean)
	{
		if(null!=wslId && !"".equals(wslId))
		{
			boolean checkForDocumentUse=true;
			// fetch pending / processing job count for the user for any Market
			int jobCount = SIVINBatchTransactionDAO.getPendingAndProcessingJobCount(wslId,"MME");
			if(jobCount > 0)
			{
				// set error Message - another Job is already in progress, please wait for it to finish
				sessionBean.setErrorMessage(msgProps.getProperty("error.schedule.job"));
				checkForDocumentUse = false;
				return false;
			}
			
			if(checkForDocumentUse==true)
			{
				if(null!=sessionBean.getSiNumber() && !"".equals(sessionBean.getSiNumber()) 
						&& null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
				{
					// check if there's any Pending / Processing Job for DOCUMENT + LOCALE BY ANY USER
					// If Yes = SHOW MESSAGE DOCUMENT FOR LOCALE IS IN USE BY ANOTHER USER. 
					// Please reach out to him for more details.
					String localeCode="";
					if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
					{
						ManualLanguageDetails mld = null;
						for(int a=0;a<sessionBean.getLanguageList().size();a++)
						{
							mld = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
							if(String.valueOf(mld.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
							{
								localeCode=  mld.getManualLanguageName();
								break;
							}
							mld = null;
						}
						mld = null;
					}
					
					Map<String, String> userMap = SIVINBatchTransactionDAO.checkDocumentInUse(sessionBean.getSiNumber(), localeCode);
					if(null!=userMap)
					{
						// e.g document Is in Use
						// error.document.inuse
						String data = sessionBean.getSiNumber()+","+localeCode+","+userMap.get("FIRST_NAME")+" "+userMap.get("LAST_NAME");
						String[] id = data.split(",");
						sessionBean.setErrorMessage(msgProps.getMessage(id, "error.document.inuse"));
						data = null;
						id = null;
						return false;
					}
					userMap=  null;
				}
			}
		}
		return true;
	}
	
	
	private void identifyToShowTranslationOperationControls(MMESIVinBean sessionBean)
	{
		sessionBean.setShowTranslationOperationsControl(false);
		try
		{
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
				{
					ManualLanguageDetails lDetails = null;;
					for(int r=0;r<sessionBean.getLanguageList().size();r++)
					{
						lDetails=  (ManualLanguageDetails)sessionBean.getLanguageList().get(r);
						if(String.valueOf(lDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
						{
							if(null!=lDetails.getManualLanguageName() && !"".equals(lDetails.getManualLanguageName()))
							{
								if(lDetails.getManualLanguageName().replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_eu").trim().toLowerCase()))
								{
									sessionBean.setShowTranslationOperationsControl(true);
									break;
								}
							}
						}
						lDetails  = null;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MMESIVinBean.class.getName(), "identifyToShowTranslationOperationControls()", e);
		}
	}
}