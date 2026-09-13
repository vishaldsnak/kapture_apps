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

import com.mazda.gms3.mdm.bean.SIVinBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.FetchIMDataDAO;
import com.mazda.gms3.mdm.dao.FetchKaptureDataDAO;
import com.mazda.gms3.mdm.kapture.KaptureCategoryServiceImpl;
import com.mazda.gms3.mdm.kapture.KaptureContentServiceImpl;
import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.dao.SIVINBatchTransactionDAO;
import com.mazda.gms3.mdm.dao.SIVinDAO;
import com.mazda.gms3.mdm.im.impl.MNAOSIVINBatchProcessingImpl;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.CountryLocaleComparator;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.ManualLanguageComparator;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.utils.VDSComparator;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CarlineDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.IMCategoryDetails;
import com.mazda.gms3.mdm.vo.MNAOViewContentDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SIVINScheduleDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;
import com.mazda.gms3.mdm.vo.ScheduleConstants;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class MNAOSIVin
 */
public class MNAOSIVin extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	Logger logger = LogManager.getLogger(MNAOSIVin.class);
	String wslId=null;
	MessageProperties msgProps= null;
	String reportName=null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_MNAO_SIVIN_RANGE;
	
	private String catLoadLimit=ApplicationProperties.getProperty("mnao.cat.create.limit");
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MNAOSIVin() {
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
			SIVinBean sessionBean = getSessionBean(request);
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
			sessionBean.setFromYear(null);
			sessionBean.setToYear(null);
			sessionBean.setFromYearList(null);
			sessionBean.setToYearList(null);
//			sessionBean.setYearId(null);
//			sessionBean.setYearsList(null);
			sessionBean.setVdsId(null);
			sessionBean.setHiddenVDSId(null);
			sessionBean.setVdsList(null);
			sessionBean.setVinStartRange("000000");
			sessionBean.setVinEndRange("ZZZZZZ");
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
			sessionBean.setScheduleName(null);
			
			/*
			 * FRENCH CANADA TRANSLATION CONTROLS - hidden until en_CA is picked, and the radio
			 * defaults to Yes so an en_CA edit carries fr_CA with it unless the user says not to.
			 */
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/mnaosivin.jsp");
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
			SIVinBean sessionBean = getSessionBean(request);
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
			 * WHETHER THE FRENCH CANADA CONTROLS BELONG ON THIS SCREEN. Decided from the language
			 * that has just been read off the request, so it is right before any action runs and
			 * before the JSP asks for it.
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
					sessionBean.setFromYear(null);
					sessionBean.setToYear(null);
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					sessionBean.setTempVinsList(null);
					// set to default VALUE
					sessionBean.setTranslationsUpdate(ApplicationProperties.getProperty("value.esicategory.flag.yes"));

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
					 * call function to load year data
					 */
					getYearsData(sessionBean);
					/*
					 * call function to load VDS list
					 */
					getVDSList(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					sessionBean.setWmiId(null);
					sessionBean.setModelId(null);
					sessionBean.setCarlineCode(null);
					sessionBean.setCarlineNameEng(null);
					sessionBean.setFromYear(null);
					sessionBean.setToYear(null);
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					sessionBean.setTempVinsList(null);
					
					/*
					 * call function to load wmi list
					 */
					getWMIList(sessionBean);
					/*
					 * call function to load model list
					 */
					getModelList(sessionBean);
					/*
					 * call function to load year data
					 */
					getYearsData(sessionBean);
					/*
					 * call function to load VDS list
					 */
					getVDSList(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("WMI_SELECTION"))
				{
					sessionBean.setModelId(null);
					sessionBean.setCarlineCode(null);
					sessionBean.setCarlineNameEng(null);
					sessionBean.setFromYear(null);
					sessionBean.setToYear(null);
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					/*
					 * call function to load model list
					 */
					getModelList(sessionBean);
					/*
					 * call function to load year data
					 */
					getYearsData(sessionBean);
					/*
					 * call function to load VDS list
					 */
					getVDSList(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("MODEL_SELECTION"))
				{
					sessionBean.setFromYear(null);
					sessionBean.setToYear(null);
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					/*
					 * call function to load year data
					 */
					getYearsData(sessionBean);
					/*
					 * call function to load VDS list
					 */
					getVDSList(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("YEAR_SELCTION"))
				{
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					/*
					 * call function to load VDS list
					 */
					getVDSList(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("RESET"))
				{
					/*
					 * call function to reset the complete form
					 */
					resetForm(sessionBean, request);
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
					 * perform Remove VIN Operation for the en_CA document AND its fr_CA translation.
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
				
				else if(sessionBean.getActionClicked().equals("RESET_VIN"))
				{
					/*
					 * only clean the add vin block
					 */
					sessionBean.setWmiId(null);
					sessionBean.setModelId(null);
					sessionBean.setCarlineCode(null);
					sessionBean.setCarlineNameEng(null);
					sessionBean.setFromYear(null);
					sessionBean.setToYear(null);
					sessionBean.setVdsId(null);
					sessionBean.setHiddenVDSId(null);
					sessionBean.setVinStartRange("000000");
					sessionBean.setVinEndRange("ZZZZZZ");
					sessionBean.setTempVinsList(null);
					/*
					 * call getModels List function
					 */
					getModelList(sessionBean);
					/*
					 * call getYears Data
					 */
					getYearsData(sessionBean);
					/*
					 * call VDS List
					 */
					getVDSList(sessionBean);
				}
				
				else if(sessionBean.getActionClicked().equals("EXPORT"))
				{
					/*
					 * Call Export Function.
					 */
					exportMNAOSIVINDetails(sessionBean);
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/mnaosivin.jsp");
				rs.forward(request, response);
			}
		}
	}
	
	/**
	 * SHOW THE "UPDATE FRENCH CANADA TRANSLATIONS" CONTROLS? Only for en_CA.
	 *
	 * MME's equivalent tests for en_EU and then fans out to every translation the document has.
	 * MNAO has ONE translation target, fr_CA, so this gate is what keeps the whole feature off
	 * every other MNAO locale - en_US included.
	 */
	/**
	 * MAP THE SAME VIN RANGES ONTO THE fr_CA TRANSLATION, for the INLINE add path.
	 *
	 * WHY THIS EXISTS SEPARATELY FROM THE BATCH: the screen only schedules a job when the number
	 * of ranges exceeds sivin.category.load.limit. Anything smaller is mapped right here on the
	 * request thread, and that path never goes near MNAOSIVINBatchProcessingImpl - so wiring the
	 * flag into the schedule sites alone left every small en_CA add without its French update.
	 *
	 * CATEGORIES ARE PER LOCALE IN k_categories, so they have to exist under fr_CA before they can
	 * be mapped there. Any that were newly created for en_CA are created for fr_CA too, then the
	 * whole set is verified. If something is still missing the map is SKIPPED with a message
	 * rather than mapping a subset, because a partial translation is harder to spot than none.
	 *
	 * The en_CA document has already been updated by the caller and is never rolled back.
	 */
	/**
	 * REFRESH THE VIEW CONTENT ROWS FOR ONE LOCALE - gms3_vc_model_year_details and
	 * gms3_vc_vin_details, which the View Content popup reads.
	 *
	 * The selected locale has always had this done after a map or a delete. The fr_CA translation
	 * had not, so its View Content stayed at whatever it held before the translation was touched.
	 */
	private void refreshViewContent(String documentId, String locale)
	{
		try
		{
			if(null==documentId || "".equals(documentId.trim()) || null==locale || "".equals(locale.trim()))
			{
				return;
			}
			if(documentId.trim().startsWith("WD") || documentId.trim().startsWith("SM")
					|| documentId.trim().startsWith("SI") || documentId.trim().startsWith("AC"))
			{
				MNAOViewContentDetails viewContentDetails = FetchKaptureDataDAO.prepareViewContentData(documentId.trim(), locale);
				if(null!=viewContentDetails && null!=viewContentDetails.getDocumentId() && !"".equals(viewContentDetails.getDocumentId())
						&& null!=viewContentDetails.getLocale() && !"".equals(viewContentDetails.getLocale()))
				{
					boolean vcFlag = SIVinDAO.performViewContentOperation(viewContentDetails, null, "Y");
					if(vcFlag==false)
					{
						logger.info("refreshViewContent :: failed to save View Content details for {"+documentId+"} / {"+locale+"}.");
					}
					else
					{
						logger.info("refreshViewContent :: View Content refreshed for {"+documentId+"} / {"+locale+"}.");
					}
				}
				else
				{
					/*
					 * SAY SO. This used to be a silent no-op, which is why the fr_CA refresh looked
					 * like it was never called at all.
					 */
					logger.info("refreshViewContent :: no document found for {"+documentId+"} / {"+locale+"} - View Content NOT refreshed.");
				}
				viewContentDetails = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "refreshViewContent()", e);
		}
	}

	private void addVinsToFrenchTranslation(SIVinBean sessionBean, String documentId,
			String sourceLocale, String wslId, ArrayList<String> categoriesToBeAddedToDocument,
			ArrayList<IMCategoryDetails> newCategories)
	{
		try
		{
			if(sessionBean.isShowTranslationOperationsControl()==false)
			{
				// not en_CA - the feature does not apply
				return;
			}
			if(null==sessionBean.getTranslationsUpdate()
					|| !sessionBean.getTranslationsUpdate().equals(ApplicationProperties.getProperty("value.esicategory.flag.yes")))
			{
				logger.info("addVinsToFrenchTranslation :: {"+documentId+"} - Update French Canada Translations is No. Nothing to do.");
				return;
			}
			if(null==categoriesToBeAddedToDocument || categoriesToBeAddedToDocument.size()<=0)
			{
				return;
			}

			String frenchLocale = ApplicationProperties.getProperty("fr_ca");
			if(FetchKaptureDataDAO.hasTranslationForLocale(documentId, frenchLocale)==false)
			{
				logger.info("addVinsToFrenchTranslation :: {"+documentId+"} has no {"+frenchLocale+"} translation - skipped.");
				sessionBean.setInfoMessage("Document {"+documentId+"} has no "+frenchLocale+" translation, so only "+sourceLocale+" was updated.");
				return;
			}

			if(null!=newCategories && newCategories.size()>0)
			{
				KaptureCategoryServiceImpl.createCategories(newCategories, frenchLocale, wslId);
			}

			ArrayList<String> missing = KaptureCategoryServiceImpl.ensureCategoriesForLocale(
					categoriesToBeAddedToDocument, frenchLocale, sourceLocale);
			if(null!=missing && missing.size()>0)
			{
				logger.info("addVinsToFrenchTranslation :: {"+documentId+"} - "+missing.size()+" categor(y/ies) absent for {"+frenchLocale+"}. NOT mapping a subset.");
				sessionBean.setErrorMessage("The selected VIN range(s) could not be added to the "+frenchLocale+" translation because "+missing.size()+" categor(y/ies) do not exist for that locale. "+sourceLocale+" was updated.");
				return;
			}

			logger.info("addVinsToFrenchTranslation :: {"+documentId+"} - mapping "+categoriesToBeAddedToDocument.size()+" categor(y/ies) onto the {"+frenchLocale+"} translation.");
			SIVinDetails frDetails = KaptureContentServiceImpl.modifyContent(documentId, frenchLocale,
					wslId, categoriesToBeAddedToDocument, new SIVinDetails());
			if(null!=frDetails && null!=frDetails.getDocumentId() && !"".equals(frDetails.getDocumentId()))
			{
				logger.info("addVinsToFrenchTranslation :: {"+documentId+"} - {"+frenchLocale+"} translation updated.");
				// the translation's own View Content rows, same as the selected locale gets
				refreshViewContent(documentId, frenchLocale);
			}
			else
			{
				sessionBean.setErrorMessage("Failed to add the selected VIN range(s) to the "+frenchLocale+" translation of {"+documentId+"}.");
			}
			frDetails = null;
			frenchLocale = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "addVinsToFrenchTranslation()", e);
		}
	}

	private void identifyToShowTranslationOperationControls(SIVinBean sessionBean)
	{
		sessionBean.setShowTranslationOperationsControl(false);
		try
		{
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
				{
					ManualLanguageDetails lDetails = null;
					for(int r=0;r<sessionBean.getLanguageList().size();r++)
					{
						lDetails=  (ManualLanguageDetails)sessionBean.getLanguageList().get(r);
						if(String.valueOf(lDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
						{
							if(null!=lDetails.getManualLanguageName() && !"".equals(lDetails.getManualLanguageName()))
							{
								if(lDetails.getManualLanguageName().replace("-", "_").trim().toLowerCase().equals(ApplicationProperties.getProperty("en_ca").trim().toLowerCase()))
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "identifyToShowTranslationOperationControls()", e);
		}
	}

	private void getYesNoList(SIVinBean sessionBean)
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

	private SIVinBean getSessionBean(HttpServletRequest request) 
	{
		SIVinBean sessionBean = null;
		if (null != request.getSession().getAttribute("siVinBean") && !"".equals(request.getSession().getAttribute("siVinBean"))) 
		{
			sessionBean = (SIVinBean) request.getSession().getAttribute("siVinBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new SIVinBean();
			request.getSession().setAttribute("siVinBean", sessionBean);
		}
		return sessionBean;
	}

	
	
	private void getCountryLocaleList(SIVinBean sessionBean, HttpServletRequest request)
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
					 * ONLY MC COUNTRIES ALLOWED HERE
					 */
					ArrayList<CountryLocaleDetails> actList = new ArrayList<CountryLocaleDetails>();
					StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("mnao.countries.codes"),",");
					while(str.hasMoreTokens())
					{
						String token = str.nextToken();
						for(CountryLocaleDetails clD : list)
						{
							if(clD.getCountryLocaleDesc().trim().toLowerCase().equals(token.trim().toLowerCase()))
							{
								actList.add(clD);
								break;
							}
						}
						token = null;
					}
					
					if(null!=actList && actList.size()>0)
					{
						CountryLocaleComparator countryLocaleComparator = new CountryLocaleComparator();
						Collections.sort(actList,countryLocaleComparator);
						sessionBean.setCountryLocaleList(actList);
						countryLocaleComparator = null;
					}
					actList=  null;
					str = null;
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
						 * ONLY MC COUNTRIES ALLOWED HERE
						 */
						ArrayList<CountryLocaleDetails> actList = new ArrayList<CountryLocaleDetails>();
						StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("mnao.countries.codes"),",");
						while(str.hasMoreTokens())
						{
							String token = str.nextToken();
							for(CountryLocaleDetails clD : finalCountryList)
							{
								if(clD.getCountryLocaleDesc().trim().toLowerCase().equals(token.trim().toLowerCase()))
								{
									actList.add(clD);
									break;
								}
							}
							token = null;
						}
						
						if(null!=actList && actList.size()>0)
						{
							CountryLocaleComparator countryLocaleComparator = new CountryLocaleComparator();
							Collections.sort(actList,countryLocaleComparator);
							sessionBean.setCountryLocaleList(actList);
							countryLocaleComparator = null;
						}
						actList=  null;
						str = null;
					}
					finalCountryList=  null;
				}
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private void getLanguageList(SIVinBean sessionBean, HttpServletRequest request)
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
						 * ONLY MC LANGUAGES ALLOWED HERE
						 */
						ArrayList<ManualLanguageDetails> actList = new ArrayList<ManualLanguageDetails>();
						StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("mnao.countries.locales.codes"),",");
						while(str.hasMoreTokens())
						{
							String token = str.nextToken();
							token = token.replace("_", "-");
							for(ManualLanguageDetails mlD : list)
							{
								if(mlD.getManualLanguageName().trim().toLowerCase().equals(token.trim().toLowerCase()))
								{
									actList.add(mlD);
									break;
								}
							}
							token = null;
						}
						
						if(null!=actList && actList.size()>0)
						{
							ManualLanguageComparator manualLanguageComparator = new ManualLanguageComparator();
							Collections.sort(actList,manualLanguageComparator);
							sessionBean.setLanguageList(actList);
							manualLanguageComparator = null;
						}
						actList=  null;
						str = null;
					
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
							 * ONLY MC LANGUAGES ALLOWED HERE
							 */
							ArrayList<ManualLanguageDetails> actList = new ArrayList<ManualLanguageDetails>();
							StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("mnao.countries.locales.codes"),",");
							while(str.hasMoreTokens())
							{
								String token = str.nextToken();
								token = token.replace("_", "-");
								for(ManualLanguageDetails mlD : finalLocaleList)
								{
									if(mlD.getManualLanguageName().trim().toLowerCase().equals(token.trim().toLowerCase()))
									{
										actList.add(mlD);
										break;
									}
								}
								token = null;
							}
							
							if(null!=actList && actList.size()>0)
							{
								ManualLanguageComparator manualLanguageComparator = new ManualLanguageComparator();
								Collections.sort(actList,manualLanguageComparator);
								sessionBean.setLanguageList(actList);
								manualLanguageComparator = null;
							}
							actList=  null;
							str = null;
						}
						finalLocaleList=  null;
					}
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getLanguageList()", e);
		}
	}
	
	private void getWMIList(SIVinBean sessionBean)
	{
		try
		{
			sessionBean.setWmiList(new ArrayList<SelectItemDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<SelectItemDetails> list = new ArrayList<SelectItemDetails>();
				String languageId = identifyLanguageId(sessionBean);
				list = SIVinDAO.getWMIList(languageId);
				if(null!=list && list.size()>0)
				{
					sessionBean.setWmiList(list);
				}
				list = null;
				languageId = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getWMIList()", e);
		}
	}
	
	private void getModelList(SIVinBean sessionBean)
	{
		try
		{
			sessionBean.setModelsList(new ArrayList<CarlineDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()) &&
					 null!=sessionBean.getWmiId() && !"".equals(sessionBean.getWmiId()))
			{
				ArrayList<CarlineDetails> list = new ArrayList<CarlineDetails>();
				String languageId = identifyLanguageId(sessionBean);
				list = SIVinDAO.getModelsList(languageId, sessionBean.getWmiId());
				if(null!=list && list.size()>0)
				{
					sessionBean.setModelsList(list);
				}
				list = null;
				languageId = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getModelList()", e);
		}
	}
	
	private void getYearsData(SIVinBean sessionBean)
	{
		try
		{
			sessionBean.setFromYear(null);
			sessionBean.setToYear(null);
			sessionBean.setFromYearList(new ArrayList<SelectItemDetails>());
			sessionBean.setToYearList(new ArrayList<SelectItemDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()) &&
					 null!=sessionBean.getWmiId() && !"".equals(sessionBean.getWmiId()) && 
					 null!=sessionBean.getModelId() && !"".equals(sessionBean.getModelId()) && 
					 null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) && 
					 null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
			{
				String languageId = identifyLanguageId(sessionBean);
				ArrayList<SelectItemDetails> tempYearList = new ArrayList<SelectItemDetails>();
				tempYearList = SIVinDAO.getYearsList(languageId, sessionBean.getWmiId(), sessionBean.getCarlineNameEng(), sessionBean.getCarlineCode());
				if(null!=tempYearList && tempYearList.size()>0)
				{
					sessionBean.setFromYearList(tempYearList);
					sessionBean.setToYearList(tempYearList);
				}
				tempYearList = null;
				languageId = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getYearsData()", e);
		}
	}
	
	private void getVDSList(SIVinBean sessionBean)
	{
		try
		{
			sessionBean.setVdsList(new ArrayList<SelectItemDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()) &&
					null!=sessionBean.getWmiId() && !"".equals(sessionBean.getWmiId()) && 
					null!=sessionBean.getModelId() && !"".equals(sessionBean.getModelId()) && 
					null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) && 
					null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng())
					&& null!=sessionBean.getFromYear() && !"".equals(sessionBean.getFromYear()) &&
					null!=sessionBean.getToYear() && !"".equals(sessionBean.getToYear()))
			{
				if(validteYearRangeForVDS(sessionBean))
				{
					ArrayList<SelectItemDetails> tempVDSList = new ArrayList<SelectItemDetails>();
					String languageId = identifyLanguageId(sessionBean);
					ArrayList<SelectItemDetails> list = SIVinDAO.getVDSList(languageId, sessionBean.getWmiId(), sessionBean.getCarlineNameEng(), sessionBean.getCarlineCode(), sessionBean.getFromYear(), sessionBean.getToYear());
					if(null!=list && list.size()>0)
					{
						tempVDSList.addAll(list);
					}
					list = null;
					/*
					 * New change, check for all VDS, if the Year mapped with Them, exists with in the range selected 
					 * from the combos - then add
					 * else do not add.
					 */
					int startYear = 0;
					int endYear = 0;
					ArrayList<String> rangeYearList = new ArrayList<String>();
					if(null!=sessionBean.getFromYear() && !"".equals(sessionBean.getFromYear()))
					{
						startYear = new Integer(sessionBean.getFromYear()).intValue();
					}
					if(null!=sessionBean.getToYear() && !"".equals(sessionBean.getToYear()))
					{
						endYear = new Integer(sessionBean.getToYear()).intValue();
					}
					
					// GENERATE DEFAUTL VDS LIST
					if(startYear>0 && endYear>0)
					{
						if(startYear<=endYear)
						{
							for(int a=startYear;a<=endYear;a++)
							{
								/*
								 * a bit of change in log here
								 * since carline code is already identified by the model selected
								 * so only idenitfy the year chars for the year values
								 * and generate default VDS. No need of explicitly fetching Carline codes again 
								 */
								
//								list = SIVinDAO.getVDSListForDefaultVINs(sessionBean.getManualLanguageId(), sessionBean.getWmiId(), sessionBean.getModelId(), String.valueOf(a));
//								if(null!=list && list.size()>0)
//								{
//									tempVDSList.addAll(list);
//								}
//								list = null;
								
								String yearCodeForVds=SIVinDAO.getYearCodeFromVinXref(languageId, String.valueOf(a));
								if(null!=yearCodeForVds && !"".equals(yearCodeForVds))
								{
									String vdsCodeLabel=sessionBean.getCarlineCode().trim().toUpperCase()+"****"+yearCodeForVds.trim()+"#";
//									String vdsCode=carCode.trim()+"0000"+yearCodeForVds.trim()+"0";
									SelectItemDetails si = new SelectItemDetails();
									si.setLabel(vdsCodeLabel);
									si.setValue(vdsCodeLabel);
									si.setYearValue(String.valueOf(a));
									si.setVdsType("DEFAULT");
									
									/*
									 * before adding check here, if the DEFAULT VDS is already added in MASTER DATA
									 * THEN SET THEIR MASTER TYPE TO DEFAULT AND DO NOT ADD DEFAULT ONE AGAIN
									 * to Make the DEFAULT VDS Selection on Click on All in VDS List Box.
									 * 
									 * Change is done for Incident - INC0015972 - Date 14th July 2025
									 * Because when DEFAULT VDS ARE ADDED IN MASTER DATA, THEIR VDS TYPE IS BEING SET AS MASTER AND THEY DO NOT GET
									 * SELECTED WHEN CLICKED ON ALL IN VDS LIST BOX
									 */
									boolean addDefaultVDS = true;
									if(null!=tempVDSList && tempVDSList.size()>0)
									{
										SelectItemDetails existSi = null;
										for(int ty=0;ty<tempVDSList.size();ty++)
										{
											existSi = (SelectItemDetails)tempVDSList.get(ty);
											if(existSi.getValue().trim().toLowerCase().equals(vdsCodeLabel.trim().toLowerCase()))
											{
												// DEFAULT VDS IS ALREADY ADDED AS MASTER DATA - CHANGE ITS VDS TYPE TO DEFAULT
												addDefaultVDS = false;
												existSi.setVdsType("DEFAULT");
												break;
											}
											existSi = null;
										}
									}
									
									if(addDefaultVDS==true)
									{
										// add this to tempList
										tempVDSList.add(si);
									}
									si = null;
									vdsCodeLabel = null;
								}
								yearCodeForVds = null;
								// add this Year to rangeList
								rangeYearList.add(String.valueOf(a));
							}
						}
					}

					/*
					 * NOW Here, tempVDSList contains - 
					 * 
					 * CREATED ALL VDS For the Selected Model + CARLINE CODE + WMI CODE FALLING IN THE SELECTED YEAR RANGE
					 * DEFULT VDS for all the years mappedWith in the Year Range selected in From & To Combos.
					 * 
					 * ITERATE TEMP VDS LIST AND ADD TO FINAL VDS LIST (UNIQUE VALUES)
					 * A CASE MAY OCCUR WHERE A DEFUALT VDS WOULD HAVE BEEN CREATED IN MDM, SO ONLY UNIQUE ONE SHOULD BE ADDED
					 */
					if(null!=tempVDSList && tempVDSList.size()>0)
					{
						ArrayList<SelectItemDetails> finlList = new ArrayList<SelectItemDetails>();
						
						for(int t=0;t<tempVDSList.size();t++)
						{
							SelectItemDetails details = (SelectItemDetails)tempVDSList.get(t);
							boolean addToList = true;
							if(null!=finlList && finlList.size()>0)
							{
								for(int j=0;j<finlList.size();j++)
								{
									SelectItemDetails exist = (SelectItemDetails)finlList.get(j);
									if(details.getValue().trim().toLowerCase().equals(exist.getValue().trim().toLowerCase()))
									{
										addToList = false;
										break;
									}
								}
							}

							if(addToList == true)
							{
								if(null!=details.getYearValue() && !"".equals(details.getYearValue()))
								{
									// add to finlList
									finlList.add(details);
								}
							}
							details  =null;
						}
						
						if(null!=finlList && finlList.size()>0)
						{
							/*
							 *  ALL OPTION FOR VDS LIST -
							 */
							SelectItemDetails si = new SelectItemDetails();
							si.setLabel(msgProps.getProperty("label.all"));
							si.setValue(ApplicationProperties.getProperty("check.all.label"));
							sessionBean.getVdsList().add(si);
							si = null;
							// add Other Items
							// before adding sort the List , first all DEFAULT VDS then Masters
							Collections.sort(finlList, new VDSComparator());
							sessionBean.getVdsList().addAll(finlList);
						}
						finlList = null;
					}
					tempVDSList = null;
					rangeYearList = null;
					languageId = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getVDSList()", e);
		}
	}
		
	private boolean validteYearRangeForVDS(SIVinBean sessionBean)
	{
		int startYear = 0;
		int endYear = 0;
		if(null!=sessionBean.getFromYear() && !"".equals(sessionBean.getFromYear()))
		{
			startYear = new Integer(sessionBean.getFromYear()).intValue();
		}
		if(null!=sessionBean.getToYear() && !"".equals(sessionBean.getToYear()))
		{
			endYear = new Integer(sessionBean.getToYear()).intValue();
		}
		if(startYear>endYear)
		{
			// set errorMessage fromYear cannot be more than To year.
			String errorMessage=msgProps.addMessage("error.data.greater", msgProps.getProperty("label.year.to")+" "+msgProps.getProperty("label.year"), msgProps.getProperty("label.year.from")+" "+msgProps.getProperty("label.year"));
			sessionBean.setErrorMessage(errorMessage);
			errorMessage = null;
			return false;
		}
		return true;
	}
		
	private  void readParamsFromRequest(SIVinBean sessionBean, HttpServletRequest request)
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
			sessionBean.setVdsId(null);
			sessionBean.setHiddenVDSId(null);
			sessionBean.setVinStartRange(null);
			sessionBean.setVinEndRange(null);
			sessionBean.setActionClicked(null);
			sessionBean.setFromYear(null);
			sessionBean.setToYear(null);
			sessionBean.setSelectedPreviewRows(null);
			sessionBean.setSelectedRows(null);
			

			// Create FileItemFactory instance
			DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
			// By using fileItemFactory instance get the ServletFileUpload
			// object
			// as
			ServletFileUpload servletFileUpload = new ServletFileUpload(fileItemFactory);
			// Now get the list of all files by parsing the request
			@SuppressWarnings("unchecked")
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
						if (fieldName.equals("SIVIN_DataTabel_displayPageNo")) 
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


						if(fieldName.equals("SIVIN_DataTabel_displayPageLen"))
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
						if (fieldName.equals("SIVIN_Preview_DataTabel_displayPageNo")) 
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

						if (fieldName.equals("SIVIN_Preview_DataTabel_displayPageLen")) 
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

						if (fieldName.equals("SIVIN_ActionClicked")) 
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

						if (fieldName.equals("SIVIN_CountryLocale_Code")) 
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


						if(fieldName.equals("SIVIN_TranslationUpdate"))
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

						if (fieldName.equals("SIVIN_Lang_Code")) 
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

						if (fieldName.equals("SIVIN_SI_Number")) 
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

						if (fieldName.equals("SIVIN_WMICode")) 
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

						if (fieldName.equals("SIVIN_Model")) 
						{
							// set the value in sessionBean.setModelId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setModelId(value);
								/*
								 * ITERATE MODEL LIST AND IDENTIFY CARLINE ENG NAME & CARLINE CODE
								 */
								if(null!=sessionBean.getModelsList() && sessionBean.getModelsList().size()>0)
								{
									for(int a=0;a<sessionBean.getModelsList().size();a++)
									{
										CarlineDetails cDetails = (CarlineDetails)sessionBean.getModelsList().get(a);
										if(cDetails.getCarlineIdForCombo().trim().toLowerCase().equals(sessionBean.getModelId().trim().toLowerCase()))
										{
											sessionBean.setCarlineCode(cDetails.getCarlineCode());
											sessionBean.setCarlineNameEng(cDetails.getCarlineNameEng());
											break;
										}
									}
								}
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("SIVIN_Year_From")) 
						{
							// set the value in sessionBean.setFromYear
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setFromYear(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("SIVIN_Year_To")) 
						{
							// set the value in sessionBean.setToYear
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setToYear(value);
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("SIVIN_VDSSelectedValues"))
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

						if (fieldName.equals("SIVIN_VISStartRange")) 
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

						if (fieldName.equals("SIVIN_VISEndRange")) 
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
						
						if(fieldName.equals("SIVIN_Preview_SelectedRows"))
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
						
						if(fieldName.equals("SIVIN_SelectedRows"))
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "readParamsFromRequest()", e);
		}
	}
	
	private void resetForm(SIVinBean sessionBean, HttpServletRequest request)
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
		sessionBean.setModelsList(null);
		sessionBean.setFromYear(null);
		sessionBean.setToYear(null);
		sessionBean.setFromYearList(null);
		sessionBean.setToYearList(null);
		sessionBean.setVdsId(null);
		sessionBean.setHiddenVDSId(null);
		sessionBean.setVdsList(null);
		sessionBean.setVinStartRange("000000");
		sessionBean.setVinEndRange("ZZZZZZ");
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
	
	private void searchOperation(SIVinBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSiNumber() && !"".equals(sessionBean.getSiNumber()) && 
					null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()) && 
					null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
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
				logger.info("searchOperation :: Please select Country Locale, Manual Language & Enter SI Document id.");
				sessionBean.setErrorMessage(msgProps.getProperty("error.mandatory.fields"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(),"searchOperation()",e);
		}
	}

	private void getDocumentOperation(SIVinBean sessionBean, String documentId, String locale)
	{
		try
		{
			if(null!=documentId && !"".equals(documentId) && null!=locale && !"".equals(locale))
			{
				locale = locale.trim();
				locale = locale.replace("-", "_");
				/*
				 * Fetch Document Id details from the KAPTURE CMS database (was: IM Database)
				 */
				String languageId = identifyLanguageId(sessionBean);
				/*
				 * TWO CONNECTIONS ARE NEEDED NOW.
				 * The document and its categories come from kapture_cms_db (getCMSConnection), while
				 * the MDM masters cross-checked below - checkVININMDM, getMappedYearFromVinXref,
				 * getModel - live in kapture_dc (getConnection). Under Oracle OK_DC and OK_IM were
				 * two schemas on one connection; they are separate databases now, and on a developer
				 * workstation not even on the same server.
				 */
				Connection conn = null;
				Connection cmsConn = null;
				String connClosed="N";
				try
				{
					conn = DBConnectionHelper.getConnection();
					cmsConn = DBConnectionHelper.getCMSConnection();
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getDocumentOperation()", e);
				}


				/*
				 * The old code used this reference key to resolve an InfoManager object id for the VIN
				 * hierarchy. In Kapture the SAME value is the mastercategoryrefkey on
				 * k_article_category (MNAO -> VIN), so the property is passed straight through.
				 */
				String vinParentRefKey = ApplicationProperties.getProperty("MNAO.VIN.HIERARCHY.REFKEY");
				String labelLocale = FetchKaptureDataDAO.resolveLabelLocale(locale, "MNAO");
				SIVinDetails imDocumentDetails = FetchKaptureDataDAO.getDocumentsData(documentId, locale, vinParentRefKey,"MNAO", cmsConn, connClosed);
				if(null!=imDocumentDetails && null!=imDocumentDetails.getDocumentId() && !"".equals(imDocumentDetails.getDocumentId()))
				{
					/*
					 * CHECK IF DOCUMENT IS CHECKED OUT BY SOME ONE, SHOW ERROR MESSAGE,
					 * THE DOCUMENT IS CHECKED OUT. PLEASE CLEAR CHECK OUT ON {"++"} TO PROCEED FURTHER.
					 */
					if(imDocumentDetails.isCheckedOut()==true)
					{
						logger.info("getDocumentOperation :: Fetched Document from Kapture for id {"+documentId+"} And Locale :: > " + locale +" is Checked Out :: >" + imDocumentDetails.isCheckedOut());
						String message = msgProps.addMessage("error.document.checked.out", documentId);
						sessionBean.setErrorMessage(message);
						message= null;
					}
					else
					{
						if(null!=imDocumentDetails.getCategoryList() && imDocumentDetails.getCategoryList().size()>0)
						{
							ArrayList<IMCategoryDetails> vinHierarchy = null;
							String wmiRefKey=null;
							String wmiCode=null;
							String vdsCode=null;
							String visStartRange=null;
							String visEndRange = null;
							String vdsRefKey = null;
							String visRanges = null;
							SIVinDetails fieldDetails = null;
							String model=null;
							String yearCodeFromVDS=null;
							ArrayList<String> mappedYear  = null;
							for(int i=0;i<imDocumentDetails.getCategoryList().size();i++)
							{
								IMCategoryDetails catImpl = (IMCategoryDetails)imDocumentDetails.getCategoryList().get(i);
								/*
								 * WMI AND VDS USED TO BE CARVED OUT OF THE INFOMANAGER OBJECT ID, A DOTTED
								 * PATH VIN.WMI.VDS.VIS, AND ONLY A 4-PART ID - i.e. a mapping made at VIS
								 * RANGE level - was processed at all.
								 *
								 * Kapture has no object ids, so the same levels are read from the category's
								 * PARENT CHAIN in k_categories, walked root-first:
								 *   [0] VIN   [1] WMI   [2] VDS   [3] VIS range
								 * The size check below is the direct equivalent of the old tokens.length==4,
								 * so a document mapped only at VDS level is skipped exactly as it was before.
								 */
								try
								{
									vinHierarchy = FetchKaptureDataDAO.getCategoryHierarchy(catImpl.getCategoryRefKey(), locale, labelLocale, cmsConn);
									if(null!=vinHierarchy && vinHierarchy.size()==4)
									{
										// WMI REF KEY - level 1 of the chain
										wmiRefKey = vinHierarchy.get(1).getCategoryRefKey();
										if(null!=wmiRefKey && !"".equals(wmiRefKey))
										{
											if(wmiRefKey.trim().toLowerCase().equals("_"))
											{
												wmiCode = "-";
											}
											else
											{
												wmiCode = wmiRefKey;
											}
										}

										// VDS LABEL FOR THE LOCALE - level 2 of the chain. This is the NAME, which
										// is what getCategoryNameOnObjectId() used to return.
										vdsCode = vinHierarchy.get(2).getCategoryName();
									}
									else
									{
										logger.info("getDocumentOperation :: {"+catImpl.getCategoryRefKey()+"} is a "
												+ (null==vinHierarchy ? 0 : vinHierarchy.size()) + "-level category, not a 4-level"
												+ " VIN/WMI/VDS/VIS range - not shown on this screen. EXPECTED for a mapping"
												+ " made above VIS level; only VIS-range mappings are listed here.");
									}
								}
								catch(Exception e)
								{
									Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getDocumentOperation()", e);
								}

								if(null!=wmiCode && !"".equals(wmiCode) && null!=vdsCode && !"".equals(vdsCode))
								{
									// IDENTIFY YEAR + CARLINE CODE + CARLINE NAME + VIS RANGES
									vdsRefKey = Utilities.replaceRefKeys(wmiCode.trim().toUpperCase()+vdsCode.trim().toUpperCase());
									// replace it from CATEGORY REF KEY
									visRanges = catImpl.getCategoryRefKey().replace(vdsRefKey, "");
									if(null!=visRanges && !"".equals(visRanges))
									{
										if(visRanges.length()>=7)
										{
											visStartRange = visRanges.substring(0,6);
											visEndRange = visRanges.substring(6,visRanges.length());
										}
									}

									// NOW YEAR + CARLINE CODE + CARLINE NAME
									fieldDetails = new SIVinDetails();
									fieldDetails.setWmiCode(wmiCode);
									fieldDetails.setVdsCode(vdsCode);
									try
									{
										boolean valuesFound = false;
										fieldDetails = SIVinDAO.checkVININMDM(languageId, fieldDetails, conn, connClosed);
										if(null!=fieldDetails && null!=fieldDetails.getCarlineCode() && null!=fieldDetails.getModel() &&
										!"".equals(fieldDetails.getModel()) && null!=fieldDetails.getYear() && !"".equals(fieldDetails.getYear()))
										{
											valuesFound=true;
										}

										if(valuesFound==false)
										{
											// QUITE POSSIBLE WMI + VDS MAY NOT EXIST IN MDM - DEFAULT VDS CASE.
											if(vdsCode.length()>=8)
											{
												// GET CARLINE CODE FROM VDS
												fieldDetails.setCarlineCode(vdsCode.substring(0,2));
												// GET YEAR CODE FROM VDS
												yearCodeFromVDS = vdsCode.substring(6,7);
												if(null!=yearCodeFromVDS && !"".equals(yearCodeFromVDS))
												{
													mappedYear = SIVinDAO.getMappedYearFromVinXref(languageId,yearCodeFromVDS, conn, connClosed);
													if(null!=mappedYear && mappedYear.size()>0)
													{
														for(int r=0;r<mappedYear.size();r++)
														{
															// GET MODEL ON THE BASIS OF WMI CODE + CARLINE CODE + YEAR (CHECK WHICH LIES IN THE RANGE)
															model = SIVinDAO.getModel(languageId, wmiCode, mappedYear.get(r).toString(), fieldDetails.getCarlineCode(), conn, connClosed);
															if(null!=model && !"".equals(model))
															{
																fieldDetails.setModel(model);
																fieldDetails.setYear(mappedYear.get(r).toString());
															}
														}
													}

												}
											}
										}
									}
									catch(Exception e)
									{
										Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getDocumentOperation()", e);
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
									documentDetails.setCarlineCode(fieldDetails.getCarlineCode());
									documentDetails.setVdsCode(vdsCode);
									documentDetails.setVinStartRange(visStartRange);
									documentDetails.setVinEndRange(visEndRange);
									documentDetails.setModel(fieldDetails.getModel());
									documentDetails.setYear(fieldDetails.getYear());
									/*
									 * THE KAPTURE VERSION THIS ROW WAS READ AT. The update path has to send the
									 * version back to the API, and under InfoManager it was implicit in the
									 * content record - carry it explicitly now.
									 */
									documentDetails.setFetchedVersion(imDocumentDetails.getFetchedVersion());

									/*
									 * add DocumentDetails to sessionBean.DocumentsList
									 */
									if(null==sessionBean.getDocumentsList() || sessionBean.getDocumentsList().size()<=0)
									{
										sessionBean.setDocumentsList(new ArrayList<SIVinDetails>());
									}
									sessionBean.getDocumentsList().add(documentDetails);
									documentDetails = null;

									vinHierarchy = null;
									wmiRefKey=null;
									wmiCode=null;
									vdsCode=null;
									visStartRange=null;
									visEndRange = null;
									vdsRefKey = null;
									visRanges = null;
									fieldDetails = null;
									model=null;
									yearCodeFromVDS=null;
									mappedYear  = null;

								}
								vinHierarchy = null;
								wmiRefKey=null;
								wmiCode=null;
								vdsCode=null;
								visStartRange=null;
								visEndRange = null;
								vdsRefKey = null;
								visRanges = null;
								fieldDetails = null;
								model=null;
								yearCodeFromVDS=null;
								mappedYear  = null;
							}
						}
					}
				}
				else
				{
					logger.info("getDocumentOperation :: Failed to fetch Document from Kapture for id {"+documentId+"} And Locale :: > " + locale);
					sessionBean.setErrorMessage(msgProps.getProperty("error.fetch.document.im"));
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
					Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getDocumentOperation()", e);
				}
				conn = null;
				try
				{
					if(null!=cmsConn)
					{
						// close CMS connection object
						cmsConn.close();
					}
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getDocumentOperation()", e);
				}
				cmsConn = null;
				vinParentRefKey= null;
				labelLocale = null;
				imDocumentDetails=  null;
				languageId = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "getDocumentOperation()", e);
		}
	}

	private void addVinOperation(SIVinBean sessionBean, String wslId)
	{
		try
		{
			if(validate(sessionBean))
			{
				if(validateJobProcessing(sessionBean))
				{
					String locale = ManualLanguageDAO.getManualLanguageCode(sessionBean.getManualLanguageId().trim());
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
						
						
						String selectedVDSCodes[] = sessionBean.getVdsId();
						if(null!=selectedVDSCodes && selectedVDSCodes.length>0)
						{
							for(int a=0;a<selectedVDSCodes.length;a++)
							{
								String token = selectedVDSCodes[a];
								if(null!=token && !"".equals(token) && !ApplicationProperties.getProperty("check.all.label").equals(token))
								{
									String thirdLevel = sessionBean.getWmiId().trim().toUpperCase()+token.trim().toUpperCase()+sessionBean.getVinStartRange().trim().toUpperCase()+sessionBean.getVinEndRange().trim().toUpperCase();
									/*
									 * Proceed for adding VINs.
									 */
									SIVinDetails itemDetails = new SIVinDetails();
									itemDetails.setSrNo(sessionBean.getTempVinsList().size()+1);
									itemDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
									itemDetails.setManualLanguageId(sessionBean.getManualLanguageId());
									itemDetails.setWmiCode(sessionBean.getWmiId());
//									itemDetails.setModel(sessionBean.getModelId());
									itemDetails.setModel(sessionBean.getCarlineNameEng());
									itemDetails.setCarlineCode(sessionBean.getCarlineCode());
									
									/*
									 * identify YEAR VALUE FOR THE SELECTED VDS FROM VDS LIST
									 * AS WHILE POPULATING VDS - IT WAS ALREADY ADDED
									 */
									String selectedVDSYear=null;
									if(null!=sessionBean.getVdsList() && sessionBean.getVdsList().size()>0)
									{
										for(int y=0;y<sessionBean.getVdsList().size();y++)
										{
											SelectItemDetails si = (SelectItemDetails)sessionBean.getVdsList().get(y);
											if(si.getValue().trim().toLowerCase().equals(token.trim().toLowerCase()))
											{
												selectedVDSYear = si.getYearValue();
												break;
											}
										}
									}
									
									if(null!=selectedVDSYear && !"".equals(selectedVDSYear) && 
											null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
									{
										// set year in Item Details
										itemDetails.setYear(selectedVDSYear);
										// add Model Year Ref Key
										String modelYearRefKey = sessionBean.getCarlineNameEng();
										// remove extra hypens in name - e.g. CX-3 to CX3
										modelYearRefKey = modelYearRefKey.replace("-", "");
										modelYearRefKey = replaceRefKeyChars(modelYearRefKey);
										// add year
										modelYearRefKey=modelYearRefKey+"_"+selectedVDSYear.trim().toUpperCase();
										boolean add = true;
										if(null!=modelYearsList && modelYearsList.size()>0 )
										{
											for(int r=0;r<modelYearsList.size();r++)
											{
												if(String.valueOf(modelYearsList.get(r)).trim().toLowerCase().equals(modelYearRefKey.trim().toLowerCase()))
												{
													add = false;
													break;
												}
											}
										}
										
										if(add==true)
										{
											modelYearsList.add(modelYearRefKey);
										}
									}
									
									itemDetails.setVdsCode(token.trim());
									itemDetails.setVinStartRange(sessionBean.getVinStartRange());
									itemDetails.setVinEndRange(sessionBean.getVinEndRange());

									itemDetails.setFirstLevelName(sessionBean.getWmiId().trim().toUpperCase());
									itemDetails.setFirstLevelRefKey(Utilities.replaceRefKeys(sessionBean.getWmiId().trim().toUpperCase()));
									itemDetails.setSecondLevelName(token.trim().toUpperCase());
									itemDetails.setSeconddLevelRefKey(Utilities.replaceRefKeys(sessionBean.getWmiId().trim().toUpperCase()+token.trim().toUpperCase()));
									itemDetails.setThirdLevelName(thirdLevel);
									itemDetails.setThirdLevelRefKey(Utilities.replaceRefKeys(thirdLevel.trim().toUpperCase()));
									// for the new itemToBeAdded set the selectedFlag to true
									itemDetails.setSelected(true);

									// add to tempVINList
									tempVinDetailsList.add(itemDetails);
									itemDetails= null;
									thirdLevel = null;
									selectedVDSYear = null;
								}
								token = null;
							}
						}
						
						
						/*
						 * NOW FOR THE MODEL YEAR LIST, CHECK WHAT ALL EXISTS IN IM AND WHAT DOES NOT
						 * FOR THE ONES THAT DO NOT EXISTS IN IM - REMOVE IT FROM THE MODEL YEAR LIST AND
						 * AND ADD THEM TO DOES NOT EXIST LIST
						 */
						
						/*
						 * WHICH MODEL YEAR CATEGORIES ARE USABLE FOR THIS LOCALE?
						 *
						 * WAS: InfoManagerServiceImpl.getCategoryDetails(modelYearsList, wslId) - ask InfoManager which
						 * reference keys exist, and report every one that does not.
						 *
						 * NOW: kapture_cms_db.k_categories holds a row PER LOCALE, so a key that exists for another
						 * locale but not this one is NOT missing - it is created for this locale together with every
						 * level above it, through the category REST API - see
						 * KaptureCategoryServiceImpl). Only a key that exists NOWHERE is reported.
						 */
						ArrayList<String> modelYearsNotInKapture = new ArrayList<String>();
						if(null!=modelYearsList && modelYearsList.size()>0)
						{
							String categorySourceLocale = FetchKaptureDataDAO.resolveLabelLocale(locale, "MNAO");
							modelYearsNotInKapture = KaptureCategoryServiceImpl.ensureCategoriesForLocale(modelYearsList, locale, categorySourceLocale);
							/*
							 * THE SAME CHECK FOR fr_CA, and for the same reason it exists at all.
							 *
							 * ensureCategoriesForLocale CREATES the model-year category for the locale
							 * when it is missing there. k_categories holds a row PER LOCALE, so a model
							 * year present under en_CA can be absent under fr_CA - and then the VIN maps
							 * onto the translation with no MODEL_YEAR above it, which is why the Model
							 * column came back empty on the fr_CA document.
							 *
							 * Running it here, at Preview, means the user is told BEFORE anything is
							 * written. The two results are unioned: a model year unusable in either
							 * locale is unusable for the operation, because the point of Yes is that
							 * both documents end up carrying the same ranges.
							 */
							if(sessionBean.isShowTranslationOperationsControl()==true
									&& null!=sessionBean.getTranslationsUpdate()
									&& sessionBean.getTranslationsUpdate().equals(ApplicationProperties.getProperty("value.esicategory.flag.yes")))
							{
								String frenchLocale = ApplicationProperties.getProperty("fr_ca");
								ArrayList<String> missingInFrench = KaptureCategoryServiceImpl.ensureCategoriesForLocale(modelYearsList, frenchLocale, categorySourceLocale);
								if(null!=missingInFrench && missingInFrench.size()>0)
								{
									logger.info("mapVINOperation :: "+missingInFrench.size()+" model year(s) unusable for {"+frenchLocale+"}.");
									for(int m=0;m<missingInFrench.size();m++)
									{
										if(!modelYearsNotInKapture.contains(missingInFrench.get(m)))
										{
											modelYearsNotInKapture.add(missingInFrench.get(m));
										}
									}
								}
								missingInFrench = null;
								frenchLocale = null;
							}
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
								if(null!=sivinDetails.getModel() && !"".equals(sivinDetails.getModel()) && null!=sivinDetails.getYear() && !"".equals(sivinDetails.getYear()))
								{
									modelYearRefKey = sivinDetails.getModel();
									// remove extra hypens in name - e.g. CX-3 to CX3
									modelYearRefKey = modelYearRefKey.replace("-", "");
									modelYearRefKey = modelYearRefKey+"_"+sivinDetails.getYear();
									modelYearRefKey = replaceRefKeyChars(modelYearRefKey);
									if(null!=modelYearsList && modelYearsList.size()>0)
									{
										for(int a=0;a<modelYearsList.size();a++)
										{
											if(String.valueOf(modelYearsList.get(a)).trim().toLowerCase().equals(modelYearRefKey.trim().toLowerCase()))
											{
												// PROCEED FOR ADDING THE ITEM TO SESSION BEAN LIST, ELSE SKIP IT.
												if(alreadyExists(sivinDetails.getThirdLevelName(), sessionBean).equals("NEW"))
												{
													sivinDetails.setSrNo(sessionBean.getTempVinsList().size()+1);
													sessionBean.getTempVinsList().add(sivinDetails);
												}
												else
												{
													duplicateRefKeys = duplicateRefKeys+sivinDetails.getThirdLevelName()+",";
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "addVinOperation()", e);
		}
	}
	
	private String alreadyExists(String refKey, SIVinBean sessionBean)
	{	
		String status="NEW";
		if(null!=refKey && !"".equals(refKey) && null!=sessionBean.getTempVinsList() && sessionBean.getTempVinsList().size()>0)
		{
			refKey=  Utilities.replaceRefKeys(refKey);
			for(int i=0;i<sessionBean.getTempVinsList().size();i++)
			{
				SIVinDetails details = (SIVinDetails)sessionBean.getTempVinsList().get(i);
				if(details.getThirdLevelRefKey().trim().toLowerCase().equals(refKey.trim().toLowerCase()))
				{
					// already exists
					status = "ADDED";
					break;
				}
			}
		}
		return status;
	}
		
	private void setSelectedTempRows(SIVinBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "setSelectedTempRows()", e);
		}
	}
	
	private void mapVinOperation(SIVinBean sessionBean, String wslId, HttpServletRequest request)
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
							String locale = ManualLanguageDAO.getManualLanguageCode(sessionBean.getManualLanguageId().trim());
							if(null!=locale && !"".equals(locale))
							{
								locale = locale.trim();
								locale = locale.replace("-", "_");
								

								/*
								 * PROCEED FOR CREATING VINS AND MAPPING IT WITH DOCUMENTS.
								 * here, we have WMI CODE, VDS CODE MULTIPLE + VIN RANGE
								 */
								int totalCountForJob = 0;
								ArrayList<String> categoriesToBeAddedToDocument = new ArrayList<String>();
								/*
								 * ITERATE SELECTED CAT LIST AND ALL WMIS TO LIST
								 * 
								 * ADD ALL LEVELS TO THE LIST 
								 * 	1ST LEVEL (WMI)
								 * 		2ND LEVEL (VDS) 
								 * 			3RD LEVEL (VIS RANGE)
								 */
								ArrayList<IMCategoryDetails> catList = new ArrayList<IMCategoryDetails>();
								
								/*
								 * add all 1st Level Categories to the IM LIST
								 */
								for(int r=0;r<categoriesSelectedList.size();r++)
								{
									SIVinDetails selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
									if(null!=selItemDetails.getFirstLevelRefKey() && !"".equals(selItemDetails.getFirstLevelRefKey()))
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
												if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getFirstLevelRefKey().trim().toLowerCase()))
												{
													addToList = false;
													break;
												}
											}
										}
										
										if(addToList==true)
										{
											IMCategoryDetails details = new IMCategoryDetails();
											details.setCategoryName(selItemDetails.getFirstLevelName().trim().toUpperCase());
											details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getFirstLevelRefKey().trim().toUpperCase()));
											details.setParentRefKey(ApplicationProperties.getProperty("MNAO.VIN.HIERARCHY.REFKEY"));
											details.setLocale(locale);
											// set level as LEVEL 3
											details.setLevel(ScheduleConstants.LEVEL_3);
											// set itemDetails
											details.setItemDetails(new SIVinDetails());
											details.setItemDetails(selItemDetails);
											
											catList.add(details);
											details = null;
										}
									}
								}
								
								/*
								 * add all 4th Level Categories to the IM LIST
								 */
								for(int r=0;r<categoriesSelectedList.size();r++)
								{
									SIVinDetails selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
									if(null!=selItemDetails.getSeconddLevelRefKey() && !"".equals(selItemDetails.getSeconddLevelRefKey()))
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
												if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getSeconddLevelRefKey().trim().toLowerCase()))
												{
													addToList = false;
													break;
												}
											}
										}
										
										if(addToList==true)
										{
											IMCategoryDetails details = new IMCategoryDetails();
											details.setCategoryName(selItemDetails.getSecondLevelName().trim().toUpperCase());
											details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getSeconddLevelRefKey().trim().toUpperCase()));
											details.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getFirstLevelRefKey().trim().toUpperCase()));
											details.setLocale(locale);
											// set level as LEVEL 4
											details.setLevel(ScheduleConstants.LEVEL_4);
											// set itemDetails
											details.setItemDetails(new SIVinDetails());
											details.setItemDetails(selItemDetails);
											
											catList.add(details);
											details = null;
										}
									}
								}
								
								/*
								 * add all 3rd Level Categories to the IM LIST
								 */
								for(int r=0;r<categoriesSelectedList.size();r++)
								{
									SIVinDetails selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
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
											IMCategoryDetails details = new IMCategoryDetails();
											details.setCategoryName(selItemDetails.getThirdLevelName().trim().toUpperCase());
											details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
											details.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getSeconddLevelRefKey().trim().toUpperCase()));
											details.setLocale(locale);
											
											// set level as LEVEL 5
											details.setLevel(ScheduleConstants.LEVEL_5);
											// set itemDetails
											details.setItemDetails(new SIVinDetails());
											details.setItemDetails(selItemDetails);
											
											catList.add(details);
											
											/*
											 * add VIN Ranges to categoriesToBeAddedToDocument
											 */
											// increment totalCount for Job as well
											totalCountForJob++;
											categoriesToBeAddedToDocument.add(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
											details = null;
										}
									}
								}
								
//								for(int r=0;r<categoriesSelectedList.size();r++)
//								{
//									SIVinDetails selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
//									if(null!=selItemDetails.getFirstLevelRefKey() && !"".equals(selItemDetails.getFirstLevelRefKey()))
//									{
//										/*
//										 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
//										 */
//										boolean addToList = true;
//										if(null!=catList && catList.size()>0)
//										{
//											for(int e=0;e<catList.size();e++)
//											{
//												IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
//												if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getFirstLevelRefKey().trim().toLowerCase()))
//												{
//													addToList = false;
//													break;
//												}
//											}
//										}
//										
//										if(addToList==true)
//										{
//											IMCategoryDetails details = new IMCategoryDetails();
//											details.setCategoryName(selItemDetails.getFirstLevelName().trim().toUpperCase());
//											details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getFirstLevelRefKey().trim().toUpperCase()));
//											details.setParentRefKey(ApplicationProperties.getProperty("MNAO.VIN.HIERARCHY.REFKEY"));
//											details.setLocale(locale);
//
//											catList.add(details);
//											details = null;
//										}
//									}
//								}

								/*
								 * ITERATE SELECETED CAT LIST AND ADD ALL VDS TO LIST
								 */
//								for(int r=0;r<categoriesSelectedList.size();r++)
//								{
//									SIVinDetails selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
//									if(null!=selItemDetails.getSeconddLevelRefKey() && !"".equals(selItemDetails.getSeconddLevelRefKey()))
//									{
//										/*
//										 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
//										 */
//										boolean addToList = true;
//										if(null!=catList && catList.size()>0)
//										{
//											for(int e=0;e<catList.size();e++)
//											{
//												IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
//												if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getSeconddLevelRefKey().trim().toLowerCase()))
//												{
//													addToList = false;
//													break;
//												}
//											}
//										}
//										
//										if(addToList==true)
//										{
//											IMCategoryDetails details = new IMCategoryDetails();
//											details.setCategoryName(selItemDetails.getSecondLevelName().trim().toUpperCase());
//											details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getSeconddLevelRefKey().trim().toUpperCase()));
//											details.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getFirstLevelRefKey().trim().toUpperCase()));
//											details.setLocale(locale);
//
//											catList.add(details);
//											details = null;
//										}
//									}
//								}
								
								/*
								 * add all 3rd Level Categories to the IM LIST
								 */
//								for(int r=0;r<categoriesSelectedList.size();r++)
//								{
//									SIVinDetails selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
//									if(null!=selItemDetails.getThirdLevelRefKey() && !"".equals(selItemDetails.getThirdLevelRefKey()))
//									{
//										/*
//										 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
//										 */
//										boolean addToList = true;
//										if(null!=catList && catList.size()>0)
//										{
//											for(int e=0;e<catList.size();e++)
//											{
//												IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
//												if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getThirdLevelRefKey().trim().toLowerCase()))
//												{
//													addToList = false;
//													break;
//												}
//											}
//										}
//										
//										if(addToList==true)
//										{
//											IMCategoryDetails details = new IMCategoryDetails();
//											details.setCategoryName(selItemDetails.getThirdLevelName().trim().toUpperCase());
//											details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
//											details.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getSeconddLevelRefKey().trim().toUpperCase()));
//											details.setLocale(locale);
//											
//											catList.add(details);
//											
//											/*
//											 * add VIN Ranges to categoriesToBeAddedToDocument
//											 */
//											categoriesToBeAddedToDocument.add(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
//										}
//									}
//								}
								
								/*
								 * ALSO ITERATE AND ADD EACH MODEL YEAR TO DOCUMENT
								 */
								for(int r=0;r<categoriesSelectedList.size();r++)
								{
									SIVinDetails selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
									if(null!=selItemDetails.getModel() && !"".equals(selItemDetails.getModel()) && 
											null!=selItemDetails.getYear() && !"".equals(selItemDetails.getYear()))
									{
										String refKey = selItemDetails.getModel().trim();
										// remove extra hypens from name.
										refKey = refKey.replace("-", "");
										refKey = refKey+"_"+selItemDetails.getYear().trim();
										refKey = replaceRefKeyChars(refKey);
										/*
										 * add Model Year Ref Key to categoriesToBeAddedToDocument
										 */
										categoriesToBeAddedToDocument.add(Utilities.replaceRefKeys(refKey.trim().toUpperCase()));
										refKey=  null;
									}
								}
								
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
											Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "mapVINOperatio()", e);
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
										KaptureCategoryServiceImpl.createCategories(catList, locale, wslId);
									}
									/*
									 * KEPT, NOT NULLED YET - the French Canada step below needs the same
									 * list to create these categories under fr_CA.
									 */
									ArrayList<IMCategoryDetails> frenchCatList = catList;
									catList = null;

									SIVinDetails documentDetails = new SIVinDetails();
									documentDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
									documentDetails.setManualLanguageId(sessionBean.getManualLanguageId());
									documentDetails.setLocale(locale);
									documentDetails.setWslId(wslId);
									if(null!=categoriesSelectedList && categoriesSelectedList.size()>0)
									{
										documentDetails.setItemsList(new ArrayList<SIVinDetails>());
										documentDetails.setItemsList(categoriesSelectedList);
									}

									/*
									 * NOW CALL FUNCTION TO ADD THE NEW CATEGORIES TO THE DOCUMENT
									 */
									documentDetails = KaptureContentServiceImpl.modifyContent(sessionBean.getSiNumber().trim(), locale, wslId, categoriesToBeAddedToDocument, documentDetails);
									if(null!=documentDetails  && null!=documentDetails.getDocumentId() && !"".equals(documentDetails.getDocumentId()))
									{

										sessionBean.setSuccessMessage(msgProps.addMessage("vin.mapping.success", sessionBean.getSiNumber()));

										/*
										 * UPDATE FRENCH CANADA TRANSLATIONS - en_CA only, and only when the
										 * radio says Yes. Does nothing otherwise.
										 */
										addVinsToFrenchTranslation(sessionBean, sessionBean.getSiNumber().trim(), locale, wslId, categoriesToBeAddedToDocument, frenchCatList);
										frenchCatList = null;
										sessionBean.setDocumentsList(new ArrayList<SIVinDetails>());
										sessionBean.setTempVinsList(null);
										/*
										 * call function to save the Data in database
										 */
										boolean bool = SIVinDAO.addVINDetails(documentDetails,null,"Y");
										if(bool==true)
										{
											if(sessionBean.getSiNumber().trim().startsWith("WD") || sessionBean.getSiNumber().trim().startsWith("SM") || 
													sessionBean.getSiNumber().trim().startsWith("SI") || sessionBean.getSiNumber().trim().startsWith("AC"))
											{
												MNAOViewContentDetails viewContentDetails = FetchKaptureDataDAO.prepareViewContentData(sessionBean.getSiNumber().trim(), locale);
												if(null!=viewContentDetails && null!=viewContentDetails.getDocumentId() && !"".equals(viewContentDetails.getDocumentId()) 
														&& null!=viewContentDetails.getLocale() && !"".equals(viewContentDetails.getLocale()))
												{
													// PROCEED FOR VIEW CONTENT OPERATION.
													boolean vcFlag = SIVinDAO.performViewContentOperation(viewContentDetails, null, "Y");
													if(vcFlag==false)
													{
														sessionBean.setErrorMessage("Failed to Save View Content Details for the document.");
													}
												}
												viewContentDetails = null;
											}
										}
										else if(bool==false)
										{
											sessionBean.setErrorMessage(msgProps.getProperty("error.failure.save.addvin.mapping.db"));
										}
										/*
										 * call function to reload the document details
										 */
										getDocumentOperation(sessionBean, sessionBean.getSiNumber().trim().toUpperCase(), locale);
									}
									else
									{
										logger.info("mapVinOperation :: Either Document Details or New Document id is null after trying to update in IM.");
										if(null!=documentDetails && null!=documentDetails.getErrorCode() && documentDetails.getErrorCode().equals("7000"))
										{
											// ADD ERROR - USER NOT AUTHROIZED 
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
									// SHCEDULE JOB
									if(null!=catList && catList.size()>0)
									{
										/*
										 * SCHEDULE A JOB, NAVIGATE USER TO HISTORY PAGE FOR VIEWING PROCESSING
										 */
										SIVINScheduleDetails schDetails = new SIVINScheduleDetails();
										schDetails.setScheduleName(ApplicationProperties.getProperty("mnao.schedule.name.key"));
										schDetails.setLocale(locale);
										schDetails.setDocumentId(sessionBean.getSiNumber().trim().toUpperCase());
										// divide by 3 as 3 levels 
//										schDetails.setTotalCount((catList.size() / 3));
										// only 5th level categories count here which are to be created. No Model Year Categories Count.
										schDetails.setTotalCount(totalCountForJob);
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
											/*
											 * UPDATE FRENCH CANADA TRANSLATIONS - the batch fans the
											 * added VINs out to fr_CA when this is Yes. It can only be
											 * Yes on en_CA; every other locale sends No.
											 */
											String translationUpdate=null;
											if(sessionBean.isShowTranslationOperationsControl()==true)
											{
												translationUpdate = sessionBean.getTranslationsUpdate();
											}
											else
											{
												translationUpdate = ApplicationProperties.getProperty("value.esicategory.flag.no");
											}
											final String trnalsationUpdateFlag = translationUpdate;
											translationUpdate = null;
											documentDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
											documentDetails.setManualLanguageId(sessionBean.getManualLanguageId());
											documentDetails.setLocale(locale);
											documentDetails.setWslId(wslId);
											if(null!=categoriesSelectedList && categoriesSelectedList.size()>0)
											{
												documentDetails.setItemsList(new ArrayList<SIVinDetails>());
												documentDetails.setItemsList(categoriesSelectedList);
											}
											
											final String schId=String.valueOf(scheduleId);
											final MNAOSIVINBatchProcessingImpl catProImpl = new MNAOSIVINBatchProcessingImpl();
											Runnable runn = new Runnable() 
											{
												@Override
												public void run() {
													
													synchronized (catProImpl) {
														try {
															catProImpl.startProcess(schId, documentDetails,"MNAO",trnalsationUpdateFlag);
														} catch (Exception e) {
															Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "run()", e);
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
							}
							else
							{
								logger.info("mapVinOperation :: Failed to fetch locale details for the selected Manual Language.");
								sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
							}
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "mapVinOperation()", e);
		}
	}

//	private void mapVinOperation_backup(SIVinBean sessionBean, String wslId, HttpServletRequest request)
//	{
//		try
//		{
//			if(null!=sessionBean.getSelectedPreviewRows() && !"".equals(sessionBean.getSelectedPreviewRows()))
//			{
//				String selectedSrNo=(String)sessionBean.getSelectedPreviewRows();
//				if(selectedSrNo.endsWith(","))
//				{
//					selectedSrNo = selectedSrNo.substring(0, selectedSrNo.length()-1);
//				}
//				
//				ArrayList<SIVinDetails> categoriesSelectedList = new ArrayList<SIVinDetails>();
//				String[] tokens = selectedSrNo.split(",");
//				if(null!=tokens && tokens.length>0 && null!=sessionBean.getTempVinsList() && sessionBean.getTempVinsList().size()>0)
//				{
//					for(int i=0;i<tokens.length;i++)
//					{
//						for(int j=0;j<sessionBean.getTempVinsList().size();j++)
//						{
//							SIVinDetails itemDetails = (SIVinDetails)sessionBean.getTempVinsList().get(j);
//							if(String.valueOf(itemDetails.getSrNo()).equals(String.valueOf(tokens[i])))
//							{
//								categoriesSelectedList.add(itemDetails);
//								break;
//							}
//						}
//					}
//					
//					if(null!=categoriesSelectedList && categoriesSelectedList.size()>0)
//					{
//						String locale = ManualLanguageDAO.getManualLanguageCode(sessionBean.getManualLanguageId().trim());
//						if(null!=locale && !"".equals(locale))
//						{
//							locale = locale.trim();
//							locale = locale.replace("-", "_");
//							
//
//							/*
//							 * PROCEED FOR CREATING VINS AND MAPPING IT WITH DOCUMENTS.
//							 * here, we have WMI CODE, VDS CODE MULTIPLE + VIN RANGE
//							 */
//
//							ArrayList<String> categoriesToBeAddedToDocument = new ArrayList<String>();
//							/*
//							 * ITERATE SELECTED CAT LIST AND ALL WMIS TO LIST
//							 */
//							ArrayList<IMCategoryDetails> catList = new ArrayList<IMCategoryDetails>();
//							
//							for(int r=0;r<categoriesSelectedList.size();r++)
//							{
//								SIVinDetails selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
//								if(null!=selItemDetails.getFirstLevelRefKey() && !"".equals(selItemDetails.getFirstLevelRefKey()))
//								{
//									/*
//									 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
//									 */
//									boolean addToList = true;
//									if(null!=catList && catList.size()>0)
//									{
//										for(int e=0;e<catList.size();e++)
//										{
//											IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
//											if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getFirstLevelRefKey().trim().toLowerCase()))
//											{
//												addToList = false;
//												break;
//											}
//										}
//									}
//									
//									if(addToList==true)
//									{
//										IMCategoryDetails details = new IMCategoryDetails();
//										details.setCategoryName(selItemDetails.getFirstLevelName().trim().toUpperCase());
//										details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getFirstLevelRefKey().trim().toUpperCase()));
//										details.setParentRefKey(ApplicationProperties.getProperty("MNAO.VIN.HIERARCHY.REFKEY"));
//										details.setLocale(locale);
//
//										catList.add(details);
//										details = null;
//									}
//								}
//							}
//
//							/*
//							 * ITERATE SELECETED CAT LIST AND ADD ALL VDS TO LIST
//							 */
//							for(int r=0;r<categoriesSelectedList.size();r++)
//							{
//								SIVinDetails selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
//								if(null!=selItemDetails.getSeconddLevelRefKey() && !"".equals(selItemDetails.getSeconddLevelRefKey()))
//								{
//									/*
//									 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
//									 */
//									boolean addToList = true;
//									if(null!=catList && catList.size()>0)
//									{
//										for(int e=0;e<catList.size();e++)
//										{
//											IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
//											if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getSeconddLevelRefKey().trim().toLowerCase()))
//											{
//												addToList = false;
//												break;
//											}
//										}
//									}
//									
//									if(addToList==true)
//									{
//										IMCategoryDetails details = new IMCategoryDetails();
//										details.setCategoryName(selItemDetails.getSecondLevelName().trim().toUpperCase());
//										details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getSeconddLevelRefKey().trim().toUpperCase()));
//										details.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getFirstLevelRefKey().trim().toUpperCase()));
//										details.setLocale(locale);
//
//										catList.add(details);
//										details = null;
//									}
//								}
//							}
//							
//							/*
//							 * add all 3rd Level Categories to the IM LIST
//							 */
//							for(int r=0;r<categoriesSelectedList.size();r++)
//							{
//								SIVinDetails selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
//								if(null!=selItemDetails.getThirdLevelRefKey() && !"".equals(selItemDetails.getThirdLevelRefKey()))
//								{
//									/*
//									 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
//									 */
//									boolean addToList = true;
//									if(null!=catList && catList.size()>0)
//									{
//										for(int e=0;e<catList.size();e++)
//										{
//											IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
//											if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getThirdLevelRefKey().trim().toLowerCase()))
//											{
//												addToList = false;
//												break;
//											}
//										}
//									}
//									
//									if(addToList==true)
//									{
//										IMCategoryDetails details = new IMCategoryDetails();
//										details.setCategoryName(selItemDetails.getThirdLevelName().trim().toUpperCase());
//										details.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
//										details.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getSeconddLevelRefKey().trim().toUpperCase()));
//										details.setLocale(locale);
//										
//										catList.add(details);
//										
//										/*
//										 * add VIN Ranges to categoriesToBeAddedToDocument
//										 */
//										categoriesToBeAddedToDocument.add(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
//									}
//								}
//							}
//							
//							
//							
//							/*
//							 * ALSO ITERATE AND ADD EACH MODEL YEAR TO DOCUMENT
//							 */
//							for(int r=0;r<categoriesSelectedList.size();r++)
//							{
//								SIVinDetails selItemDetails= (SIVinDetails)categoriesSelectedList.get(r);
//								if(null!=selItemDetails.getModel() && !"".equals(selItemDetails.getModel()) && null!=selItemDetails.getYear() && !"".equals(selItemDetails.getYear()))
//								{
//									String refKey = selItemDetails.getModel().trim();
//									// remove extra hypens from name.
//									refKey = refKey.replace("-", "");
//									refKey = refKey+"_"+selItemDetails.getYear().trim();
//									refKey = replaceRefKeyChars(refKey);
//									/*
//									 * add Model Year Ref Key to categoriesToBeAddedToDocument
//									 */
//									categoriesToBeAddedToDocument.add(Utilities.replaceRefKeys(refKey.trim().toUpperCase()));
//									refKey=  null;
//								}
//							}
//							
//							
//							if(null!=catList && catList.size()>0)
//							{
//								/*
//								 * call function to create NewCategories in IM.
//								 */
//								InfoManagerServiceImpl.createCategory(catList, wslId);
//							}
//							catList = null;
//							
//							SIVinDetails documentDetails = new SIVinDetails();
//							documentDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
//							documentDetails.setManualLanguageId(sessionBean.getManualLanguageId());
//							documentDetails.setLocale(locale);
//							documentDetails.setWslId(wslId);
//							if(null!=categoriesSelectedList && categoriesSelectedList.size()>0)
//							{
//								documentDetails.setItemsList(new ArrayList<SIVinDetails>());
//								documentDetails.setItemsList(categoriesSelectedList);
//							}
//							
//							/*
//							 * NOW CALL FUNCTION TO ADD THE NEW CATEGORIES TO THE DOCUMENT
//							 */
//							documentDetails = InfoManagerServiceImpl.modifyContent(sessionBean.getSiNumber().trim(), locale, wslId, categoriesToBeAddedToDocument, documentDetails);
//							if(null!=documentDetails  && null!=documentDetails.getDocumentId() && !"".equals(documentDetails.getDocumentId()))
//							{
//								
//								sessionBean.setSuccessMessage(msgProps.addMessage("vin.mapping.success", sessionBean.getSiNumber()));
//								sessionBean.setDocumentsList(new ArrayList<SIVinDetails>());
//								sessionBean.setTempVinsList(null);
//								/*
//								 * call function to save the Data in database
//								 */
//								boolean bool = SIVinDAO.addVINDetails(documentDetails);
//								if(bool==true)
//								{
//									if(sessionBean.getSiNumber().trim().startsWith("WD") || sessionBean.getSiNumber().trim().startsWith("SM") || 
//											sessionBean.getSiNumber().trim().startsWith("SI") || sessionBean.getSiNumber().trim().startsWith("AC"))
//									{
//										MNAOViewContentDetails viewContentDetails = FetchIMDataDAO.prepareViewContentData(sessionBean.getSiNumber().trim(), locale);
//										if(null!=viewContentDetails && null!=viewContentDetails.getDocumentId() && !"".equals(viewContentDetails.getDocumentId()) 
//												&& null!=viewContentDetails.getLocale() && !"".equals(viewContentDetails.getLocale()))
//										{
//											// PROCEED FOR VIEW CONTENT OPERATION.
//											SIVinDAO.performViewContentOperation(viewContentDetails);
//										}
//										viewContentDetails = null;
//									}
//								}
//								else if(bool==false)
//								{
//									sessionBean.setErrorMessage(msgProps.getProperty("error.failure.save.addvin.mapping.db"));
//								}
//								/*
//								 * call function to reload the document details
//								 */
//								getDocumentOperation(sessionBean, sessionBean.getSiNumber().trim().toUpperCase(), locale);
//							}
//							else
//							{
//								logger.info("mapVinOperation :: Either Document Details or New Document id is null after trying to update in IM.");
//								if(null!=documentDetails.getErrorCode() && documentDetails.getErrorCode().equals("7000"))
//								{
//									// ADD ERROR - USER NOT AUTHROIZED 
//									sessionBean.setErrorMessage(msgProps.getProperty("error.user.not.authorized.modify.content"));
//								}
//								else
//								{
//									// ADD GENERIC ERROR
//									sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
//								}
//							}
//							documentDetails = null;
//						}
//						else
//						{
//							logger.info("mapVinOperation :: Failed to fetch locale details for the selected Manual Language.");
//							sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
//						}
//					}
//					else
//					{
//						logger.info("mapVinOperation :: Please select atleast one row for mapping VIN.");
//						sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.add.vin"));
//					}
//				}
//				categoriesSelectedList = null;
//				tokens=  null;
//				selectedSrNo = null;
//			}
//			else
//			{
//				logger.info("mapVinOperation :: Please select atleast one row for mapping VIN.");
//				sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.add.vin"));
//			}
//			sessionBean.setSelectedPreviewRows(null);
//		}
//		catch(Exception e)
//		{
//			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "mapVinOperation()", e);
//		}
//	}

	
	private boolean validate(SIVinBean sessionBean)
	{
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) || 
				null==sessionBean.getSiNumber() || "".equals(sessionBean.getSiNumber()) || null==sessionBean.getWmiId() 
				|| "".equals(sessionBean.getWmiId()) || null==sessionBean.getModelId() || "".equals(sessionBean.getModelId()) 
				|| null==sessionBean.getCarlineNameEng() || "".equals(sessionBean.getCarlineNameEng()) || 
				null==sessionBean.getCarlineCode() || "".equals(sessionBean.getCarlineCode()) 
				|| null==sessionBean.getFromYear() || "".equals(sessionBean.getFromYear()) || null==sessionBean.getToYear() || 
				"".equals(sessionBean.getToYear()) 
				|| null==sessionBean.getVdsId() || sessionBean.getVdsId().length<=0  || null==sessionBean.getVinStartRange() || 
				"".equals(sessionBean.getVinStartRange()) || null==sessionBean.getVinEndRange() || "".equals(sessionBean.getVinEndRange()))

		{
			sessionBean.setErrorMessage(msgProps.getProperty("error.mandatory.fields"));
			return false;
		}
		
		if(null!=sessionBean.getVinStartRange() && !"".equals(sessionBean.getVinStartRange()))
		{
			if(sessionBean.getVinStartRange().trim().length()!=6)
			{
				sessionBean.setErrorMessage(msgProps.addMessage("error.length.exact.characters", msgProps.getProperty("label.visstartrange"),"6"));
				return false;
			}
		}
		
		if(null!=sessionBean.getVinEndRange() && !"".equals(sessionBean.getVinEndRange()))
		{
			if(sessionBean.getVinEndRange().trim().length()!=6)
			{
				sessionBean.setErrorMessage(msgProps.addMessage("error.length.exact.characters", msgProps.getProperty("label.visendrange"), "6"));
				return false;
			}
		}
			
		
		return true;
	}
		
	/**
	 * @param removeFromTranslations "Yes" to remove the same VINs from the document's FRENCH
	 *        CANADA translation as well. Only ever passed from the en_CA screen - see
	 *        identifyToShowTranslationOperationControls().
	 */
	private void removeVIN(HttpServletRequest request, SIVinBean sessionBean,
			String removeFromTranslations)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				/*
				 *  IDENTIFY THE VINS TO BE REMOVED
				 *  ALSO WHILE IDENTIFYING VINS TO BE REMOVED
				 *  CHECK MODEL & YEAR FOR EACH OF THE SELECTED VINS TO BE REMOVED
				 *  CHECK IF THEY ARE MAPPED WITH DOCUMENT AND NO OTHER VIN OF THAT MODEL/YEAR 
				 *  IS MAPPED REMOVED THE VIN.
				 */
				
				ArrayList<String> vinsToBeDeleted = new ArrayList<String>();
				ArrayList<SIVinDetails> itemsList = new ArrayList<SIVinDetails>();
				String documentId="";
				String locale="";
				
				ArrayList<SIVinDetails> remainingVinsList = new ArrayList<SIVinDetails>();
				
				if(null!=sessionBean.getDocumentsList() && sessionBean.getDocumentsList().size()>0)
				{
					/*
					 *  SET ALL DOCUMENTS LIST AS REMAINING VINS LIST
					 *  ITERATE SELECTED ROWS TOKEN AND START REMOVING ALL SELECTED ITEMS 
					 *  TO IDENTIFY THE EXACT REMAINING ITEMS LIST
					 */
					for(SIVinDetails det : sessionBean.getDocumentsList())
					{
						remainingVinsList.add(det);
						det = null;
					}
					
					// PREPARE REMAINING LIST - WILL BE USED FOR IDENITHING MODEL & MODEL_YEAR TO BE REMOVED
					
					if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
					{
						String tokens[] = sessionBean.getSelectedRows().split(",");
						if(null!=tokens && tokens.length>0)
						{
							for(int j=0;j<tokens.length;j++)
							{
								for(int i=0;i<remainingVinsList.size();i++)
								{
									SIVinDetails vinDetails = (SIVinDetails)remainingVinsList.get(i);
									if(String.valueOf(vinDetails.getSrNo()).equals(String.valueOf(tokens[j])))
									{
										// remove the current Index
										remainingVinsList.remove(i);
										i--;
										break;
									}
									vinDetails = null;
								}
							}
						}
						tokens = null;
					}
					
					/*
					 * PREPARE VIN LIST AND REMOVED ITEMS LIST
					 */
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
											itemDetails.setCarlineCode(vinDetails.getCarlineCode());
											itemDetails.setYear(vinDetails.getYear());
											itemDetails.setVdsCode(vinDetails.getVdsCode());
											itemDetails.setVinStartRange(vinDetails.getVinStartRange());
											itemDetails.setVinEndRange(vinDetails.getVinEndRange());
											
											itemDetails.setFirstLevelRefKey(vinDetails.getWmiCode().trim().toUpperCase());
											itemDetails.setSeconddLevelRefKey(vinDetails.getWmiCode().trim().toUpperCase()+vinDetails.getVdsCode().trim().toUpperCase());
											itemDetails.setThirdLevelRefKey(vinDetails.getReferenceKey());
											itemsList.add(itemDetails);
											itemDetails=null;
										}
										break;
									}
								}
							}
							tokens=  null;
						}
						vinDetails= null;
					}
				}
				
				/*
				 * NOW CHECK HERE, IF REMOVED ITEMS LIST IS NOT NULL
				 * AND REMAINING VIN LIST IS NOT NULL
				 * ITERATE BOTH LISTS AND TRY TO IDENITFY WHICH ALL MODEL & MODEL_YEAR NEEDS TO BE DELETED.
				 */
				
				if(null!=itemsList && itemsList.size()>0)
				{
					for(SIVinDetails itemDetails : itemsList)
					{
						if(null!=itemDetails.getModel() && !"".equals(itemDetails.getModel()) 
								&& null!=itemDetails.getYear() && !"".equals(itemDetails.getYear()))
						{
							boolean modelYearFound = false;
							if(null!=remainingVinsList && remainingVinsList.size()>0)
							{
								for(SIVinDetails remainingDetails : remainingVinsList)
								{
									if(null!=remainingDetails.getModel() && !"".equals(remainingDetails.getModel()) 
											&& null!=remainingDetails.getYear() && !"".equals(remainingDetails.getYear()))
									{
										if(remainingDetails.getModel().trim().equals(itemDetails.getModel().trim()) 
												&& remainingDetails.getYear().trim().equals(itemDetails.getYear().trim()))
										{
											// ANOTHER VIN OF THE SAME MODEL YEAR EXISTS. DO NOT DELETE IT
											modelYearFound = true;
											break;
										}
									}
									remainingDetails = null;
								}
							}
							
							if(modelYearFound==false)
							{
								// PREPARE MODEL_YEAR REF KEY AND ADD TO VIN DELETE LIST
								String modelYearRefKey = itemDetails.getModel().trim();
								// INCASE IF MODEL NAME IS CX-3, it has to be CX3
								modelYearRefKey = modelYearRefKey.replace("-", "");
								// ADD YEAR
								modelYearRefKey  = modelYearRefKey+"_"+itemDetails.getYear().trim();
								modelYearRefKey = replaceRefKeyChars(modelYearRefKey).toUpperCase();
								if(null==vinsToBeDeleted || vinsToBeDeleted.size()<=0)
								{
									vinsToBeDeleted = new ArrayList<String>();
								}
								
								boolean addToDeleteList = true;
								if(null!=vinsToBeDeleted && vinsToBeDeleted.size()>0)
								{
									for(String delRefkey : vinsToBeDeleted)
									{
										if(delRefkey.trim().toLowerCase().equals(modelYearRefKey.trim().toLowerCase()))
										{
											// ALREADY ADDED
											addToDeleteList = false;
											break;
										}
										delRefkey=  null;
									}
								}
								if(addToDeleteList==true)
								{
									vinsToBeDeleted.add(modelYearRefKey);
								}
								modelYearRefKey = null;
							}
							
							// NOW CHECK FOR MODEL
							boolean modelFound = false;
							if(null!=remainingVinsList && remainingVinsList.size()>0)
							{
								for(SIVinDetails remainingDetails : remainingVinsList)
								{
									if(null!=remainingDetails.getModel() && !"".equals(remainingDetails.getModel()) && 
											null!=remainingDetails.getYear() && !"".equals(remainingDetails.getYear()))
									{
										if(remainingDetails.getModel().trim().equals(itemDetails.getModel().trim()) 
												&& !remainingDetails.getYear().trim().equals(itemDetails.getYear().trim()))
										{
											// ANOTHER VIN OF THE SAME MODEL WITH DIFFERENT YEAR EXISTS. DO NOT DELETE IT
											modelFound = true;
											break;
										}
									}
									remainingDetails = null;
								}
							}
							
							if(modelFound==false)
							{
								// PREPARE MODEL REF KEY AND ADD TO VIN DELETE LIST
								String modelRefKey = itemDetails.getModel().trim();
								// INCASE IF MODEL NAME IS CX-3, it has to be CX3
								modelRefKey = modelRefKey.replace("-", "");
								modelRefKey = replaceRefKeyChars(modelRefKey).toUpperCase();
								if(null==vinsToBeDeleted || vinsToBeDeleted.size()<=0)
								{
									vinsToBeDeleted = new ArrayList<String>();
								}
								
								boolean addToDeleteList = true;
								if(null!=vinsToBeDeleted && vinsToBeDeleted.size()>0)
								{
									for(String delRefkey : vinsToBeDeleted)
									{
										if(delRefkey.trim().toLowerCase().equals(modelRefKey.trim().toLowerCase()))
										{
											// ALREADY ADDED
											addToDeleteList = false;
											break;
										}
										delRefkey=  null;
									}
								}
								if(addToDeleteList==true)
								{
									vinsToBeDeleted.add(modelRefKey);
								}
								modelRefKey = null;
							}
						}
						else if(null!=itemDetails.getModel() && !"".equals(itemDetails.getModel()))
						{
							// WHEN YEAR IS NULL - CHECK FOR MODEL
							// NOW CHECK FOR MODEL
							boolean modelFound = false;
							if(null!=remainingVinsList && remainingVinsList.size()>0)
							{
								for(SIVinDetails remainingDetails : remainingVinsList)
								{
									if(null!=remainingDetails.getModel() && !"".equals(remainingDetails.getModel()))
									{
										if(remainingDetails.getModel().trim().equals(itemDetails.getModel().trim()))
										{
											// ANOTHER VIN OF THE SAME MODEL EXISTS. DO NOT DELETE IT
											modelFound = true;
											break;
										}
									}
									remainingDetails = null;
								}
							}
							
							
							if(modelFound==false)
							{
								// PREPARE MODEL REF KEY AND ADD TO VIN DELETE LIST
								String modelRefKey = itemDetails.getModel().trim();
								// INCASE IF MODEL NAME IS CX-3, it has to be CX3
								modelRefKey = modelRefKey.replace("-", "");
								modelRefKey = replaceRefKeyChars(modelRefKey).toUpperCase();
								if(null==vinsToBeDeleted || vinsToBeDeleted.size()<=0)
								{
									vinsToBeDeleted = new ArrayList<String>();
								}
								
								boolean addToDeleteList = true;
								if(null!=vinsToBeDeleted && vinsToBeDeleted.size()>0)
								{
									for(String delRefkey : vinsToBeDeleted)
									{
										if(delRefkey.trim().toLowerCase().equals(modelRefKey.trim().toLowerCase()))
										{
											// ALREADY ADDED
											addToDeleteList = false;
											break;
										}
										delRefkey=  null;
									}
								}
								if(addToDeleteList==true)
								{
									vinsToBeDeleted.add(modelRefKey);
								}
								modelRefKey = null;
							}
						}
						itemDetails = null;
					}
				}
				
				/**
				 * PROCEED FOR DELETION.
				 */
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

						/*
						 * DELETE WITH FRENCH TRANSLATIONS. The en_CA document is done; now take the
						 * same VINs off its fr_CA translation.
						 *
						 * ONE TARGET, NOT A DISCOVERED SET. MME asks getDocumentTranslations() for
						 * every locale a document has; MNAO's rule is exactly fr_CA, so the locale is
						 * resolved straight from application.properties. deleteContent routes itself -
						 * fr_CA is a translation row, so it writes through save-translate-article.
						 *
						 * A MISSING fr_CA TRANSLATION IS NOT A FAILURE. The en_CA delete has already
						 * succeeded and is not undone; the user is told fr_CA was skipped.
						 */
						if(null!=removeFromTranslations && removeFromTranslations.equals("Yes"))
						{
							String frenchLocale = ApplicationProperties.getProperty("fr_ca");
							if(FetchKaptureDataDAO.hasTranslationForLocale(documentId, frenchLocale)==true)
							{
								SIVinDetails frDetails = new SIVinDetails();
								frDetails = KaptureContentServiceImpl.deleteContent(documentId, frenchLocale, wslId, vinsToBeDeleted, frDetails);
								if(null!=frDetails && null!=frDetails.getDocumentId() && !"".equals(frDetails.getDocumentId()))
								{
									logger.info("removeVIN :: {"+documentId+"} - selected VIN(s) also removed from the {"+frenchLocale+"} translation.");
									refreshViewContent(documentId, frenchLocale);
								}
								else
								{
									sessionBean.setErrorMessage("Failed to remove the selected VIN(s) from the "+frenchLocale+" translation of {"+documentId+"}.");
								}
								frDetails = null;
							}
							else
							{
								logger.info("removeVIN :: {"+documentId+"} has no {"+frenchLocale+"} translation - nothing to remove there.");
								sessionBean.setInfoMessage("Document {"+documentId+"} has no "+frenchLocale+" translation, so only "+locale+" was updated.");
							}
							frenchLocale = null;
						}
						
						/*
						 * call function to save the details in database
						 */
						boolean  bool = SIVinDAO.deleteVINDetails(documentDetails);
						if(bool==true)
						{
							if(sessionBean.getSiNumber().trim().startsWith("WD") || sessionBean.getSiNumber().trim().startsWith("SM") || 
									sessionBean.getSiNumber().trim().startsWith("SI") || sessionBean.getSiNumber().trim().startsWith("AC"))
							{
								MNAOViewContentDetails viewContentDetails = FetchKaptureDataDAO.prepareViewContentData(sessionBean.getSiNumber().trim(), locale);
								if(null!=viewContentDetails && null!=viewContentDetails.getDocumentId() && !"".equals(viewContentDetails.getDocumentId()) 
										&& null!=viewContentDetails.getLocale() && !"".equals(viewContentDetails.getLocale()))
								{
									// PROCEED FOR VIEW CONTENT OPERATION.
									boolean vcFlag = SIVinDAO.performViewContentOperation(viewContentDetails,null,"Y");
									if(vcFlag ==false)
									{
										sessionBean.setErrorMessage("Failed to Save View Content Details for the document.");
									}
								}
								viewContentDetails = null;
							}
						}
						else if(bool==false)
						{
							sessionBean.setErrorMessage(msgProps.getProperty("error.failure.save.deletevin.mapping.db"));
						}
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
				itemsList = null;
				remainingVinsList = null;
			}
			else
			{
				sessionBean.setErrorMessage(msgProps.getProperty("error.select.onerow.actual.delete"));
			}
			// empty selectedRows
			sessionBean.setSelectedRows(null);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "removeVIN()", e);
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
	
	private void checkForWsl()
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "checkForWsl", e);
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
	
	private void performAccessCheck(SIVinBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "performAccessCheck()", e);
		}
	}

	private  boolean validateFileUpload(SIVinBean sessionBean)
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
	private  void executeExcelOperation(SIVinBean sessionBean, byte[] data, String extension)
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
				int totalCountForJob = 0;
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
							if(fieldDetails.getWmiCode().equals(existingDetails.getWmiCode()) 
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
					String languageId = identifyLanguageId(sessionBean);
					String locale = ManualLanguageDAO.getManualLanguageCode(sessionBean.getManualLanguageId().trim());
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
					String errorYearNo="";
					
					/*
					 * ITERATE AND CHECK FOR EACH ROW WHETHER MODEL EXISTS OR NOT
					 * IF EXISTS - THEN PROCEED FOR ADDING VIN TO THE DOCUMENT
					 * ELSE SKIP IT STATING MODEL DOESN'T EXISTS FOR THE ROW NO.
					 */
					
					Connection conn = null;
					try
					{
						conn = DBConnectionHelper.getConnection();
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "executeExcelOperation()", e);
					}
					
					String connClosed="N";
					
					ArrayList<SIVinDetails> tempCheckList = listToSave;
					// empty listToSave
					listToSave = new ArrayList<SIVinDetails>();
					SIVinDetails svd = null;
					for(int i=0;i<tempCheckList.size();i++)
					{
						SIVinDetails fieldDetails = (SIVinDetails)tempCheckList.get(i);
						/*
						 * CALL FUNCTION TO FETCH THE MODEL & YEAR DETAILS ON THE BASIS OF WMI & VDS CODES
						 */
						try
						{
							fieldDetails = SIVinDAO.checkVININMDM(languageId, fieldDetails, conn, connClosed); 
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "executeExcelOperation()", e);
						}
						
						/*
						 * IF VDS NOT FOUND IN MDM - E.G. DEFAULT VIN SCENARIO
						 */
						boolean vdsFoundInMDM=false;
						if(null!=fieldDetails && null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
								&& null!=fieldDetails.getModel() && !"".equals(fieldDetails.getModel()))
						{
							vdsFoundInMDM = true;
							/*
							 * add the item to listToSave
							 */
							svd  =new SIVinDetails();
							svd.setSrNo(fieldDetails.getSrNo());
							svd.setWmiCode(fieldDetails.getWmiCode());
							svd.setVdsCode(fieldDetails.getVdsCode());
							svd.setVinStartRange(fieldDetails.getVinStartRange());
							svd.setVinEndRange(fieldDetails.getVinEndRange());
							// set Model
							svd.setModel(fieldDetails.getModel());
							// set carlineCode and Identified Year as well
							svd.setCarlineCode(fieldDetails.getCarlineCode());
							if(null!=fieldDetails.getYear() && !"".equals(fieldDetails.getYear()))
							{
								svd.setYear(fieldDetails.getYear());
							}
							// add to list
							listToSave.add(svd);
							svd = null;
						}
						
						if(vdsFoundInMDM==false && null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()))
						{
							logger.info("--- when default VIN ------");
							String vdsCode = fieldDetails.getVdsCode();
							// QUITE POSSIBLE WMI + VDS MAY NOT EXIST IN MDM - DEFAULT VDS CASE.
							if(vdsCode.length()>=8)
							{
								logger.info("---------------- 1 --------------");
								// GET CARLINE CODE FROM VDS
								fieldDetails.setCarlineCode(vdsCode.substring(0,2));
								// GET YEAR CODE FROM VDS
								String yearCodeFromVDS = vdsCode.substring(6,7);
								logger.info("---------------- yearCodeFromVDS -------------- > "+ yearCodeFromVDS);
								if(null!=yearCodeFromVDS && !"".equals(yearCodeFromVDS))
								{
									List<String> mappedYear = SIVinDAO.getMappedYearFromVinXref(languageId,yearCodeFromVDS, conn, connClosed);
									if(null!=mappedYear && mappedYear.size()>0)
									{
										logger.info("---------------- mappedYear :: >"+ mappedYear.size());
										boolean anyModelFoundForMappedYear=false;
										for(int r=0;r<mappedYear.size();r++)
										{
											logger.info("----------- proceed for checking year :: >"+ mappedYear.get(r).toString());
											// GET MODEL ON THE BASIS OF WMI CODE + CARLINE CODE + YEAR (CHECK WHICH LIES IN THE RANGE)
											String model = SIVinDAO.getModel(languageId, fieldDetails.getWmiCode()
													, mappedYear.get(r).toString(), fieldDetails.getCarlineCode(), conn, connClosed);
											logger.info("-------------- model :: >"+ model);
											if(null!=model && !"".equals(model))
											{
												anyModelFoundForMappedYear = true;
//												fieldDetails.setModel(model);
//												fieldDetails.setYear(mappedYear.get(r).toString());
												
												/*
												 * add the item to listToSave
												 */
												svd  =new SIVinDetails();
												svd.setSrNo(fieldDetails.getSrNo());
												svd.setWmiCode(fieldDetails.getWmiCode());
												svd.setVdsCode(fieldDetails.getVdsCode());
												svd.setVinStartRange(fieldDetails.getVinStartRange());
												svd.setVinEndRange(fieldDetails.getVinEndRange());
												svd.setCarlineCode(fieldDetails.getCarlineCode());
												svd.setModel(model);
												svd.setYear(mappedYear.get(r).toString());
												// add to list
												listToSave.add(svd);
												svd = null;
											}
											model = null;
										}
										
										if(anyModelFoundForMappedYear==false)
										{
											logger.info("----------- multiple mapped year found :: But no Model Found for any of them.");
											/*
											 * add the item to listToSave
											 */
											svd  =new SIVinDetails();
											svd.setSrNo(fieldDetails.getSrNo());
											svd.setWmiCode(fieldDetails.getWmiCode());
											svd.setVdsCode(fieldDetails.getVdsCode());
											svd.setVinStartRange(fieldDetails.getVinStartRange());
											svd.setVinEndRange(fieldDetails.getVinEndRange());
											svd.setCarlineCode(fieldDetails.getCarlineCode());
											// add to list
											listToSave.add(svd);
											svd = null;
										}
									}
									else
									{
										logger.info("------ mapped year not found issue :: >"+ fieldDetails.getVdsCode());
										/*
										 * add the item to listToSave
										 */
										svd  =new SIVinDetails();
										svd.setSrNo(fieldDetails.getSrNo());
										svd.setWmiCode(fieldDetails.getWmiCode());
										svd.setVdsCode(fieldDetails.getVdsCode());
										svd.setVinStartRange(fieldDetails.getVinStartRange());
										svd.setVinEndRange(fieldDetails.getVinEndRange());
										svd.setCarlineCode(fieldDetails.getCarlineCode());
										// add to list
										listToSave.add(svd);
										svd = null;
									}
									mappedYear = null;
								}
								else
								{
									logger.info("------ year code not found issue :: >"+ fieldDetails.getVdsCode());
									/*
									 * add the item to listToSave
									 */
									svd  =new SIVinDetails();
									svd.setSrNo(fieldDetails.getSrNo());
									svd.setWmiCode(fieldDetails.getWmiCode());
									svd.setVdsCode(fieldDetails.getVdsCode());
									svd.setVinStartRange(fieldDetails.getVinStartRange());
									svd.setVinEndRange(fieldDetails.getVinEndRange());
									svd.setCarlineCode(fieldDetails.getCarlineCode());
									// add to list
									listToSave.add(svd);
									svd = null;
								}
								yearCodeFromVDS = null;
							}
							else
							{
								logger.info("------ vds length issue :: >"+ fieldDetails.getVdsCode());
								/*
								 * add the item to listToSave
								 */
								svd  =new SIVinDetails();
								svd.setSrNo(fieldDetails.getSrNo());
								svd.setWmiCode(fieldDetails.getWmiCode());
								svd.setVdsCode(fieldDetails.getVdsCode());
								svd.setVinStartRange(fieldDetails.getVinStartRange());
								svd.setVinEndRange(fieldDetails.getVinEndRange());
								
								// add to list
								listToSave.add(svd);
								svd = null;
							}
							vdsCode=  null;
						}
						fieldDetails = null;
					}
					tempCheckList = null;
					/*
					 * Now Proceed with Existing Operation
					 */
					for(int i=0;i<listToSave.size();i++)
					{
						SIVinDetails fieldDetails = (SIVinDetails)listToSave.get(i);
						/*
						 * Now Proceed for Normal Flow
						 */
						if(null!=fieldDetails && null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) 
								&& null!=fieldDetails.getModel() && !"".equals(fieldDetails.getModel()))
						{
							if(null!=fieldDetails.getYear() && !"".equals(fieldDetails.getYear()))
							{
								/*
								 * EVERYTHING IDENTIFIED - MODEL, CARLINE CODE, YEAR - PROCEED FOR ADDING THIS VINS.
								 */
								String modelYearRefKey = fieldDetails.getModel().trim().toUpperCase();
								// replace extra hypens from the Name, e.g. CX-3 has to be CX3
								modelYearRefKey = modelYearRefKey.replace("-", "");
								modelYearRefKey = replaceRefKeyChars(modelYearRefKey);
								// add year
								modelYearRefKey=modelYearRefKey.trim().toUpperCase()+"_"+fieldDetails.getYear().trim().toUpperCase();
								boolean add = true;
								if(null!=modelYearsList && modelYearsList.size()>0 )
								{
									for(int r=0;r<modelYearsList.size();r++)
									{
										if(String.valueOf(modelYearsList.get(r)).trim().toLowerCase().equals(modelYearRefKey.trim().toLowerCase()))
										{
											add = false;
											break;
										}
									}
								}
								
								if(add==true)
								{
									modelYearsList.add(modelYearRefKey);
								}
								modelYearRefKey = null;
								
								// add VINS
								String thirdLevel = fieldDetails.getWmiCode().trim().toUpperCase()+fieldDetails.getVdsCode().trim().toUpperCase()+fieldDetails.getVinStartRange().trim().toUpperCase()+fieldDetails.getVinEndRange().trim().toUpperCase();
								// PROCEED FURTHER FOR MAPPING WITH DOCUMENT
								fieldDetails.setFirstLevelName(fieldDetails.getWmiCode().trim().toUpperCase());
								fieldDetails.setFirstLevelRefKey(Utilities.replaceRefKeys(fieldDetails.getWmiCode().trim().toUpperCase()));
								fieldDetails.setSecondLevelName(fieldDetails.getVdsCode().trim().toUpperCase());
								fieldDetails.setSeconddLevelRefKey(Utilities.replaceRefKeys(fieldDetails.getWmiCode().trim().toUpperCase()+fieldDetails.getVdsCode().trim().toUpperCase()));
								fieldDetails.setThirdLevelName(thirdLevel);
								fieldDetails.setThirdLevelRefKey(Utilities.replaceRefKeys(thirdLevel.trim().toUpperCase()));
								
								listToBeFinallyProcessed.add(fieldDetails);
								thirdLevel = null;
							}
							else
							{
								/*
								 *  FAILED TO IDENTIFY YEAR FOR VDS, 
								 *  EITHER THE IDENITIED VALUE OF YEAR FROM VDS AT 7 POSITION IS NULL
								 *  OR THE IDENTIFIED YEARS DOESN'T LIE IN THE YEAR RANGE LIST OF MODEL MAPPED WITH VDS 
								 */
								if(null!=errorYearNo && !"".equals(errorYearNo))
								{
									errorYearNo = errorYearNo+",";	
								}
								errorYearNo = errorYearNo+String.valueOf(fieldDetails.getSrNo());
							}
						}
						else
						{
							// MODEL COULD NOT BE IDENTIFIED FOR WMI + VDS - DOES NOT EXIST IN MDM
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
						Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "executeExcelOperation()", e);
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
					
					if(null!=errorYearNo && !"".equals(errorYearNo))
					{
						if(errorYearNo.endsWith(","))
						{
							errorYearNo= errorYearNo.substring(0, errorYearNo.length()-1);
						}
						String[] errorRows = errorYearNo.split(",");
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
								errorMessage.append(msgProps.addMessage("error.import.sivin.failed.yearcode", String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
					}
					errorYearNo = null;
					
					
					/*
					 * CHECK IF MODEL YEAR LIST IS NOT NULL, IDENTIFY WHAT ALL EXISTS IN IM AND WHAT ALL DOESN'T
					 */
					/*
					 * WHICH MODEL YEAR CATEGORIES ARE USABLE FOR THIS LOCALE?
					 *
					 * WAS: InfoManagerServiceImpl.getCategoryDetails(modelYearsList, wslId) - ask InfoManager which
					 * reference keys exist, and report every one that does not.
					 *
					 * NOW: kapture_cms_db.k_categories holds a row PER LOCALE, so a key that exists for another
					 * locale but not this one is NOT missing - it is created for this locale together with every
					 * level above it, through the category REST API - see
					 * KaptureCategoryServiceImpl). Only a key that exists NOWHERE is reported.
					 */
					ArrayList<String> modelYearsNotInKapture = new ArrayList<String>();
					if(null!=modelYearsList && modelYearsList.size()>0)
					{
						String categorySourceLocale = FetchKaptureDataDAO.resolveLabelLocale(locale, "MNAO");
						modelYearsNotInKapture = KaptureCategoryServiceImpl.ensureCategoriesForLocale(modelYearsList, locale, categorySourceLocale);
						/*
						 * THE SAME CHECK FOR fr_CA, and for the same reason it exists at all.
						 *
						 * ensureCategoriesForLocale CREATES the model-year category for the locale
						 * when it is missing there. k_categories holds a row PER LOCALE, so a model
						 * year present under en_CA can be absent under fr_CA - and then the VIN maps
						 * onto the translation with no MODEL_YEAR above it, which is why the Model
						 * column came back empty on the fr_CA document.
						 *
						 * Running it here, at Preview, means the user is told BEFORE anything is
						 * written. The two results are unioned: a model year unusable in either
						 * locale is unusable for the operation, because the point of Yes is that
						 * both documents end up carrying the same ranges.
						 */
						if(sessionBean.isShowTranslationOperationsControl()==true
								&& null!=sessionBean.getTranslationsUpdate()
								&& sessionBean.getTranslationsUpdate().equals(ApplicationProperties.getProperty("value.esicategory.flag.yes")))
						{
							String frenchLocale = ApplicationProperties.getProperty("fr_ca");
							ArrayList<String> missingInFrench = KaptureCategoryServiceImpl.ensureCategoriesForLocale(modelYearsList, frenchLocale, categorySourceLocale);
							if(null!=missingInFrench && missingInFrench.size()>0)
							{
								logger.info("mapVINOperation :: "+missingInFrench.size()+" model year(s) unusable for {"+frenchLocale+"}.");
								for(int m=0;m<missingInFrench.size();m++)
								{
									if(!modelYearsNotInKapture.contains(missingInFrench.get(m)))
									{
										modelYearsNotInKapture.add(missingInFrench.get(m));
									}
								}
							}
							missingInFrench = null;
							frenchLocale = null;
						}
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
							if(null!=fieldDetails.getModel() && !"".equals(fieldDetails.getModel()) && null!=fieldDetails.getYear() && 
									!"".equals(fieldDetails.getYear()))
							{
								String modelYearRefKey = fieldDetails.getModel().trim().toUpperCase();
								// REPLACE EXTRA HYPENS IN NAME - E.G. CX-3 HAS TO BE CX3
								modelYearRefKey = modelYearRefKey.replace("-", "");
								modelYearRefKey = modelYearRefKey.trim().toUpperCase()+"_"+fieldDetails.getYear().trim().toUpperCase();
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
									errorMessage.append(msgProps.addMessage("error.import.sivin.failed.model.year.doesnot.exist.im", modelYearRefKey , String.valueOf(fieldDetails.getSrNo())));
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
						 * add all 1st Level Categories to the IM LIST
						 */
						for(int r=0;r<listToBeMappedWithDocument.size();r++)
						{
							SIVinDetails selItemDetails= (SIVinDetails)listToBeMappedWithDocument.get(r);
							if(null!=selItemDetails.getFirstLevelRefKey() && !"".equals(selItemDetails.getFirstLevelRefKey()))
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
										if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getFirstLevelRefKey().trim().toLowerCase()))
										{
											addToList = false;
											break;
										}
									}
								}
								
								if(addToList==true)
								{
									IMCategoryDetails cDetails = new IMCategoryDetails();
									cDetails.setCategoryName(selItemDetails.getFirstLevelName().trim().toUpperCase());
									cDetails.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getFirstLevelRefKey().trim().toUpperCase()));
									cDetails.setParentRefKey(ApplicationProperties.getProperty("MNAO.VIN.HIERARCHY.REFKEY"));
									cDetails.setLocale(locale);
									// SET LEVEL AS LEVEL 3
									cDetails.setLevel(ScheduleConstants.LEVEL_3);
									// set itemDetails
									cDetails.setItemDetails(new SIVinDetails());
									cDetails.setItemDetails(selItemDetails);
									
									catList.add(cDetails);
									cDetails = null;
								}
							}
						}
//						{
//							SIVinDetails selItemDetails= (SIVinDetails)listToBeMappedWithDocument.get(r);
//							if(null!=selItemDetails.getFirstLevelRefKey() && !"".equals(selItemDetails.getFirstLevelRefKey()))
//							{
//								/*
//								 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
//								 */
//								boolean addToList = true;
//								if(null!=catList && catList.size()>0)
//								{
//									for(int e=0;e<catList.size();e++)
//									{
//										IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
//										if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getFirstLevelRefKey().trim().toLowerCase()))
//										{
//											addToList = false;
//											break;
//										}
//									}
//								}
//								
//								if(addToList==true)
//								{
//									IMCategoryDetails cDetails = new IMCategoryDetails();
//									cDetails.setCategoryName(selItemDetails.getFirstLevelName().trim().toUpperCase());
//									cDetails.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getFirstLevelRefKey().trim().toUpperCase()));
//									cDetails.setParentRefKey("VIN");
//									cDetails.setLocale(locale);
//
//									catList.add(cDetails);
//									cDetails = null;
//								}
//							}
//						}
						
						/*
						 * ITERATE SELECETED CAT LIST AND ADD ALL VDS TO LIST
						 * add 2nd Level List
						 */
						for(int r=0;r<listToBeMappedWithDocument.size();r++)
						{

							SIVinDetails selItemDetails= (SIVinDetails)listToBeMappedWithDocument.get(r);
							if(null!=selItemDetails.getSeconddLevelRefKey() && !"".equals(selItemDetails.getSeconddLevelRefKey()))
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
										if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getSeconddLevelRefKey().trim().toLowerCase()))
										{
											addToList = false;
											break;
										}
									}
								}
								
								if(addToList==true)
								{
									IMCategoryDetails cDetails = new IMCategoryDetails();
									cDetails.setCategoryName(selItemDetails.getSecondLevelName().trim().toUpperCase());
									cDetails.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getSeconddLevelRefKey().trim().toUpperCase()));
									cDetails.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getFirstLevelRefKey().trim().toUpperCase()));
									cDetails.setLocale(locale);
									// SET LEVEL AS LEVEL 4
									cDetails.setLevel(ScheduleConstants.LEVEL_4);
									// set itemDetails
									cDetails.setItemDetails(new SIVinDetails());
									cDetails.setItemDetails(selItemDetails);
									
									catList.add(cDetails);
									cDetails = null;
								}
							}
						}
//						{
//							SIVinDetails selItemDetails= (SIVinDetails)listToBeMappedWithDocument.get(r);
//							if(null!=selItemDetails.getSeconddLevelRefKey() && !"".equals(selItemDetails.getSeconddLevelRefKey()))
//							{
//								/*
//								 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
//								 */
//								boolean addToList = true;
//								if(null!=catList && catList.size()>0)
//								{
//									for(int e=0;e<catList.size();e++)
//									{
//										IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
//										if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getSeconddLevelRefKey().trim().toLowerCase()))
//										{
//											addToList = false;
//											break;
//										}
//									}
//								}
//								
//								if(addToList==true)
//								{
//									IMCategoryDetails cDetails = new IMCategoryDetails();
//									cDetails.setCategoryName(selItemDetails.getSecondLevelName().trim().toUpperCase());
//									cDetails.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getSeconddLevelRefKey().trim().toUpperCase()));
//									cDetails.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getFirstLevelRefKey().trim().toUpperCase()));
//									cDetails.setLocale(locale);
//
//									catList.add(cDetails);
//									cDetails = null;
//								}
//							}
//						}
						
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
									cDetails.setLocale(locale);
									
									// SET LEVEL AS LEVEL 5
									cDetails.setLevel(ScheduleConstants.LEVEL_5);
									// set itemDetails
									cDetails.setItemDetails(new SIVinDetails());
									cDetails.setItemDetails(selItemDetails);
									catList.add(cDetails);
									
									/*
									 * add VIN Ranges to categoriesToBeAddedToDocument
									 */
									// increment totalCount for Job as well
									totalCountForJob++;
									categoriesToBeAddedToDocument.add(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
									cDetails = null;
								}
							}
						}
//						{
//							SIVinDetails selItemDetails= (SIVinDetails)listToBeMappedWithDocument.get(r);
//							if(null!=selItemDetails.getThirdLevelRefKey() && !"".equals(selItemDetails.getThirdLevelRefKey()))
//							{
//								/*
//								 * BEFORE ADDING CHECK, WHETHER ALREADY ADDED OR NOT.
//								 */
//								boolean addToList = true;
//								if(null!=catList && catList.size()>0)
//								{
//									for(int e=0;e<catList.size();e++)
//									{
//										IMCategoryDetails added = (IMCategoryDetails)catList.get(e);
//										if(added.getCategoryRefKey().trim().toLowerCase().equals(selItemDetails.getThirdLevelRefKey().trim().toLowerCase()))
//										{
//											addToList = false;
//											break;
//										}
//									}
//								}
//								
//								if(addToList==true)
//								{
//									IMCategoryDetails cDetails = new IMCategoryDetails();
//									cDetails.setCategoryName(selItemDetails.getThirdLevelName().trim().toUpperCase());
//									cDetails.setCategoryRefKey(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
//									cDetails.setParentRefKey(Utilities.replaceRefKeys(selItemDetails.getSeconddLevelRefKey().trim().toUpperCase()));
//									cDetails.setLocale(locale);
//									
//									catList.add(cDetails);
//									
//									/*
//									 * add VIN Ranges to categoriesToBeAddedToDocument
//									 */
//									categoriesToBeAddedToDocument.add(Utilities.replaceRefKeys(selItemDetails.getThirdLevelRefKey().trim().toUpperCase()));
//								}
//							}
//						}
						
						
						/*
						 * ALSO ITERATE AND ADD EACH MODEL YEAR TO DOCUMENT
						 */
						for(int r=0;r<listToBeMappedWithDocument.size();r++)
						{
							SIVinDetails selItemDetails= (SIVinDetails)listToBeMappedWithDocument.get(r);
							if(null!=selItemDetails.getModel() && !"".equals(selItemDetails.getModel()) && null!=selItemDetails.getYear() && !"".equals(selItemDetails.getYear()))
							{
								String refKey = selItemDetails.getModel().trim();
								// REMOVE HYPHENS FROM NAME, E.G CX-3 HAS TO BE CX3
								refKey = refKey.replace("-", "");
								refKey = refKey.trim()+"_"+selItemDetails.getYear().trim();
								refKey = replaceRefKeyChars(refKey);
								/*
								 * add Model Year Ref Key to categoriesToBeAddedToDocument
								 */
								categoriesToBeAddedToDocument.add(Utilities.replaceRefKeys(refKey.trim().toUpperCase()));
								refKey=  null;
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
									Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "executeExcelOperation()", e);
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
								KaptureCategoryServiceImpl.createCategories(catList, locale, wslId);
							}
							/*
							 * KEPT for the French Canada step - see the other mapping path.
							 */
							ArrayList<IMCategoryDetails> frenchCatList = catList;
							catList = null;


							SIVinDetails documentDetails = new SIVinDetails();
							documentDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
							documentDetails.setManualLanguageId(sessionBean.getManualLanguageId());
							documentDetails.setLocale(locale);
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
							documentDetails = KaptureContentServiceImpl.modifyContent(sessionBean.getSiNumber().trim(), locale, wslId, categoriesToBeAddedToDocument, documentDetails);
							if(null!=documentDetails  && null!=documentDetails.getDocumentId() && !"".equals(documentDetails.getDocumentId()))
							{
								sessionBean.setSuccessMessage(msgProps.addMessage("import.vin.mapping.success", String.valueOf(successCount), sessionBean.getSiNumber()));

								/*
								 * UPDATE FRENCH CANADA TRANSLATIONS - en_CA only, radio Yes only.
								 */
								addVinsToFrenchTranslation(sessionBean, sessionBean.getSiNumber().trim(), locale, wslId, categoriesToBeAddedToDocument, frenchCatList);
								frenchCatList = null;
								sessionBean.setDocumentsList(new ArrayList<SIVinDetails>());
								sessionBean.setTempVinsList(null);
								/*
								 * call function to save the Data in database
								 */
								boolean bool = SIVinDAO.addVINDetails(documentDetails,null,"Y");
								if(bool==true)
								{
									if(sessionBean.getSiNumber().trim().startsWith("WD") || sessionBean.getSiNumber().trim().startsWith("SM") || 
											sessionBean.getSiNumber().trim().startsWith("SI") || sessionBean.getSiNumber().trim().startsWith("AC"))
									{
										MNAOViewContentDetails viewContentDetails = FetchKaptureDataDAO.prepareViewContentData(sessionBean.getSiNumber().trim(), locale);
										if(null!=viewContentDetails && null!=viewContentDetails.getDocumentId() && !"".equals(viewContentDetails.getDocumentId()) 
												&& null!=viewContentDetails.getLocale() && !"".equals(viewContentDetails.getLocale()))
										{
											// PROCEED FOR VIEW CONTENT OPERATION.
											boolean vcFlag = SIVinDAO.performViewContentOperation(viewContentDetails, null, "Y");
											if(vcFlag==false)
											{
												sessionBean.setErrorMessage("Failed to Save View Content Details for the document.");
											}
										}
										viewContentDetails = null;
									}
								}
								else if(bool==false)
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
								getDocumentOperation(sessionBean, sessionBean.getSiNumber().trim().toUpperCase(), locale);
							}
							else
							{
								logger.info("executeExcelOperation :: Either Document Details or New Document id is null after trying to update in IM.");
								//							sessionBean.setErrorMessage(msgProps.getProperty("error.generic"));
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
								schDetails.setScheduleName(ApplicationProperties.getProperty("mnao.schedule.name.key"));
								schDetails.setLocale(locale);
								schDetails.setDocumentId(sessionBean.getSiNumber().trim().toUpperCase());
								// divide by 3 as 3 levels 
//								schDetails.setTotalCount((catList.size() / 3));
								// only 5th level categories count here which are to be created. No Model Year Categories Count.
								schDetails.setTotalCount(totalCountForJob);
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
									/*
									 * UPDATE FRENCH CANADA TRANSLATIONS - see the note on the other
									 * schedule site. Yes is only possible on en_CA.
									 */
									String translationUpdate=null;
									if(sessionBean.isShowTranslationOperationsControl()==true)
									{
										translationUpdate = sessionBean.getTranslationsUpdate();
									}
									else
									{
										translationUpdate = ApplicationProperties.getProperty("value.esicategory.flag.no");
									}
									final String trnalsationUpdateFlag = translationUpdate;
									translationUpdate = null;
									documentDetails.setCountryLocaleId(sessionBean.getCountryLocaleId());
									documentDetails.setManualLanguageId(sessionBean.getManualLanguageId());
									documentDetails.setLocale(locale);
									documentDetails.setWslId(wslId);
									if(null!=listToBeMappedWithDocument && listToBeMappedWithDocument.size()>0)
									{
										documentDetails.setItemsList(new ArrayList<SIVinDetails>());
										documentDetails.setItemsList(listToBeMappedWithDocument);
									}
									
									final String schId=String.valueOf(scheduleId);
									final MNAOSIVINBatchProcessingImpl catProImpl = new MNAOSIVINBatchProcessingImpl();
									Runnable runn = new Runnable() 
									{
										@Override
										public void run() {
											
											synchronized (catProImpl) {
												try {
													catProImpl.startProcess(schId, documentDetails,"MNAO",trnalsationUpdateFlag);
												} catch (Exception e) {
													Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "run()", e);
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
					listToBeFinallyProcessed = null;
					modelYearsList = null;
					modelYearDoesNotExistsinIMList = null;
					languageId = null;
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "executeExcelOperation()", e);
		}
	}

	
	
	/**
	 * Function will identify the Cell Value Type
	 * and accordingly will read their values and will return
	 * @param cell
	 * @return
	 */
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "readCellValue()", e);
			// set cellValue to null
			cellValue = null;
		}
		return cellValue;
	}

	/**
	 * READS A FIXED-WIDTH CODE FROM A CELL, KEEPING ITS LEADING ZEROS.
	 *
	 * A VIS range is a 6-CHARACTER CODE, not a number - "000000" to "ZZZZZZ". Excel does not know
	 * that: type 000000 into a cell and it stores the NUMBER 0, so readCellValue() hands back "0"
	 * and the range is neither the value the user entered nor 6 characters long. The same applies
	 * to any range with leading zeros - 000123 comes back as "123".
	 *
	 * Padding is applied ONLY to a NUMERIC cell. A cell the user formatted as TEXT already holds
	 * exactly what was typed, so "123" there is a genuine 3-character entry and must stay wrong,
	 * to be reported by the length check rather than silently corrected into "000123".
	 *
	 * @param width the exact number of characters the code must have (6 for a VIS range)
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

	private void readExcelData(byte[] data, SIVinBean sessionBean, String extension)
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
							details.setWmiCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = readCellValue(row.getCell(1));
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
						String visCode = readFixedWidthCode(row.getCell(2), 6);
						if(null!=visCode && !"".equals(visCode))
						{
							details.setVinStartRange(visCode);
						}
						visCode = null;

						visCode = readFixedWidthCode(row.getCell(3), 6);
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "readExcelData()", e);
		}
	}

	private  boolean validateExcelRowData(SIVinBean sessionBean)
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

				if(null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) ||
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
					if(fieldDetails.getVdsCode().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.vdscode")+",20,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getVinStartRange() && !"".equals(fieldDetails.getVinStartRange()))
				{
					if(fieldDetails.getVinStartRange().length()!=6)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.visstartrange")+",6,"+String.valueOf(rowNo);
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
					if(fieldDetails.getVinEndRange().length()!=6)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.visendrange")+",6,"+String.valueOf(rowNo);
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

	private  void decideErrorDisplay(SIVinBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_MNAOSIVIN_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "validateExcelRowData()", e);
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

	private  void exportMNAOSIVINDetails(SIVinBean sessionBean)
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
					writeMNAOSIVINExcel(exportDataList, sessionBean);
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
					logger.info("exportMNAOSIVINDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportMNAOSIVINDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "exportMNAOSIVINDetails()", e);
		}
	}

	private void writeMNAOSIVINExcel(ArrayList<SIVinDetails> sivinList, SIVinBean sessionBean)
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
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_MNAOSIVIN_NAME");
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
				@SuppressWarnings("resource")
				SXSSFWorkbook myWorkBook = new SXSSFWorkbook(100);
				// Create a new sheet
				Sheet mySheet = myWorkBook.createSheet("EXPORTED DATA");
				Row headerRow = mySheet.createRow(0);
				
				Cell carlineCodeCell = headerRow.createCell(0);
				carlineCodeCell.setCellValue("DOCUMENT ID");
				Cell wmiCodeCell = headerRow.createCell(1);
				wmiCodeCell.setCellValue("WMI CODE");
				Cell carNameEngCell = headerRow.createCell(2);
				carNameEngCell.setCellValue("MODEL");
				Cell carNameRegCell = headerRow.createCell(3);
				carNameRegCell.setCellValue("YEAR");
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
						if(null!=details.getWmiCode() && !"".equals(details.getWmiCode()))
						{
							cell1.setCellValue(details.getWmiCode());
						}
						if(null!=details.getModel() && !"".equals(details.getModel()))
						{
							cell2.setCellValue(details.getModel());
						}
						if(null!=details.getYear() && !"".equals(details.getYear()))
						{
							cell3.setCellValue(details.getYear());
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
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "writeMNAOSIVINExcel()", e);
		}
	}

	private String identifyLanguageId(SIVinBean sessionBean)
	{
		String languageId=null;
		try
		{
			// set Combo selectedValue
			languageId = sessionBean.getManualLanguageId();
			/*
			 * IDENTIFY HERE LANGUAGE ID
			 * 	IF EN-CA IS SELECTED - SHOW DATA FOR EN-US (DATE 27 JUNE 2018)
			 * ELSE SELECTED LOCALE
			 */
//			if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
//			{
//				String encaLocale=ApplicationProperties.getProperty("en_ca");
//				String enusLocale =ApplicationProperties.getProperty("en_us");
//				encaLocale = encaLocale.replace("_", "-");
//				enusLocale = enusLocale.replace("_", "-");
//				boolean useENUS = false;
//				for(int r=0;r<sessionBean.getLanguageList().size();r++)
//				{
//					ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(r);
//					if(sessionBean.getManualLanguageId().trim().equals(String.valueOf(mlDetails.getManualLanguageId())))
//					{
//						if(mlDetails.getManualLanguageName().trim().toLowerCase().equals(encaLocale.trim().toLowerCase()))
//						{
//							// selectedLocale is en-CA - use en-US
//							useENUS = true;
//						}
//						break;
//					}
//					mlDetails= null;
//				}
//				
//				if(useENUS==true)
//				{
//					// identify Language Id for enUS
//					try
//					{
//						languageId=ManualLanguageDAO.getManualLanguageId(enusLocale);
//					}
//					catch(Exception e)
//					{
//						Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "identifyLanguageId()", e);
//					}
//				}
//				encaLocale = null;
//				enusLocale = null;
//			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(MNAOSIVin.class.getName(), "identifyLanguageId()", e);
		}
		return languageId;
	}
	
	private boolean validateJobProcessing(SIVinBean sessionBean)
	{
		if(null!=wslId && !"".equals(wslId))
		{
			boolean checkForDocumentUse=true;
			// fetch pending / processing job count for the user for any Market
			int jobCount = SIVINBatchTransactionDAO.getPendingAndProcessingJobCount(wslId,"MNAO");
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

}
