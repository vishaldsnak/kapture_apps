package com.mazda.gms3.mdm.servlet;

import com.mazda.gms3.mdm.utils.ImportActionUtils;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.bean.VinBean;
import com.mazda.gms3.mdm.dao.CarlineDAO;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.dao.VinDAO;
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
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.VinDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;
import com.mazda.gms3.sst.utils.SSTUtils;

/**
 * Servlet implementation class Vin
 */
public class Vin extends HttpServlet {
	private static final long serialVersionUID = 1L;

	Logger logger = LogManager.getLogger(Vin.class);
	String wslId=null;
	MessageProperties msgProps= null;
	String reportName=null;

	ArrayList<VinDetails> uniqueDataList= new ArrayList<VinDetails>();
	String moduleRefKey=AccessManagementInterface.REF_KEY_VIN;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Vin() {
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
		VinBean sessionBean = getSessionBean(request);
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
			sessionBean.setCountryLocaleList(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setLanguageList(null);
			sessionBean.setCarlineList(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setCarlineId(null);
			sessionBean.setCarlineCode(null);
			sessionBean.setCarlineNameEng(null);
//			sessionBean.setCarlineNameReg(null);
			sessionBean.setVinList(null);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setActionClicked(null);
			sessionBean.setVinListToImport(new ArrayList<VinDetails>());
			sessionBean.setReportViewPath(null);
			sessionBean.setInfoMessage(null);

			sessionBean.setShowButtons(false);
			sessionBean.setShowView(false);
			sessionBean.setShowAdd(false);
			sessionBean.setAllVINDataList(null);

			/*
			 * call function to load all the Country Locale Data
			 */
			getCountryLocaleList(sessionBean, request);

			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);

			// EXPLICITY SET sessionBean.setShowButtons(true);
			sessionBean.setShowButtons(true);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/vin.jsp");
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
		/*
		 * Initialize bean
		 */
		VinBean sessionBean = getSessionBean(request);
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
			// perform Access Check
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
				if(sessionBean.getActionClicked().equals("COUNTRY_LOCALE_SELECTION"))
				{
					/*
					 * call function to load all the Manual language data
					 */
					sessionBean.setManualLanguageId(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					getLanguageList(sessionBean, request);
					sessionBean.setSelectedRows(null);

					// clear search block
					clearSearchBlock(sessionBean);
					// clear add block
					clearAddNewBlock(sessionBean);

					/*
					 * call function to load carDetails & vinDetails
					 */
					sessionBean.setCarlineId(null);
					sessionBean.setCarlineCode(null);
					sessionBean.setCarlineNameEng(null);
//					sessionBean.setCarlineNameReg(null);
					getCarlineList(sessionBean);
					getWmiList(sessionBean);
					getVinList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("LANGUAGE_SELECTION"))
				{
					/*
					 * call function to load carLineDetails
					 * and VIN Details List in case of language change
					 */
					getCarlineList(sessionBean);
					sessionBean.setCarlineId(null);
					sessionBean.setCarlineCode(null);
					sessionBean.setCarlineNameEng(null);
//					sessionBean.setCarlineNameReg(null);
					// clear search block
					clearSearchBlock(sessionBean);
					// clear add block
					clearAddNewBlock(sessionBean);

					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					getWmiList(sessionBean);
					getVinList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("CAR_SELECTION"))
				{
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					
					// clear search block
					clearSearchBlock(sessionBean);
					// clear add block
					clearAddNewBlock(sessionBean);
					
					/*
					 * call function to load wmiList
					 */
					getWmiList(sessionBean);
					/*
					 * call function to load vinDetailsList
					 */
					getVinList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SAVE_VIN"))
				{

					/*
					 * Save Operation called
					 */
					saveVinDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EDIT_VIN"))
				{
					/*
					 * Edit Operation called
					 */
					editVinDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_VIN"))
				{
					/*
					 * Delete Operation called
					 */
					deleteVinDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("ACTIVE_VIN"))
				{
					/*
					 * Active Operation called
					 */
					activeVinDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE_VIN"))
				{
					/*
					 * Update Operation called
					 */
					updateVinDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("RESET_VIN"))
				{
					// reset some fields
					sessionBean.setVinList(null);
					sessionBean.setFlagList(null);
					sessionBean.setErrorMessage(null);
					sessionBean.setSuccessMessage(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setDisplayPageLength(null);
					sessionBean.setDisplayPageNo(null);
					sessionBean.setUpdatedRows(null);
					clearAddNewBlock(sessionBean);
					clearSearchBlock(sessionBean);
					/*
					 * call getVinList
					 */
					getVinList(sessionBean);	
				}
				else if(sessionBean.getActionClicked().equals("EXPORT_VIN"))
				{
					/*
					 * Export VIN Operation
					 */
					exportVinDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("SEARCH_VIN"))
				{
					/*
					 * Search VIN Operation
					 */
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setUpdatedRows(null);

					searchVIN(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("CLEAR_SEARCH"))
				{
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setUpdatedRows(null);

					clearSearchBlock(sessionBean);
					clearAddNewBlock(sessionBean);
					/*
					 * getVIN List
					 */
					getVinList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("ADD_NEW_BUTTON"))
				{
					if(validateFileUpload(sessionBean))
					{
						sessionBean.setShowAdd(true);
						sessionBean.setShowView(false);
						sessionBean.setSelectedRows(null);
						sessionBean.setUpdatedRows(null);
						clearAddNewBlock(sessionBean);
						clearSearchBlock(sessionBean);
						/*
						 * call getCalineList
						 */
						getVinList(sessionBean);
					}
				}
				else if(sessionBean.getActionClicked().equals("VIEW_BUTTON"))
				{
					if(validateFileUpload(sessionBean))
					{
						sessionBean.setShowAdd(false);
						sessionBean.setShowView(true);
						sessionBean.setSelectedRows(null);
						sessionBean.setUpdatedRows(null);
						clearAddNewBlock(sessionBean);
						clearSearchBlock(sessionBean);
						/*
						 * call getCalineList
						 */
						getVinList(sessionBean);
					}
				}
				else if(sessionBean.getActionClicked().equals("SEARCH_GROUP"))
				{
					/*
					 * call search Operation
					 */
					searchGroupOperation(sessionBean, "VIEW");
				}
				else if(sessionBean.getActionClicked().equals("SEARCH_WMI"))
				{
					/*
					 * call search Operation
					 */
					searchWMIOperation(sessionBean, "VIEW");	
				}
				else if(sessionBean.getActionClicked().equals("SEARCH_VDS"))
				{
					/*
					 * call search Operation
					 */
					searchVDSOperation(sessionBean, "VIEW");
				}
				else if(sessionBean.getActionClicked().equals("SEARCH_VISSTART"))
				{
					/*
					 * call search Operation
					 */
					searchVISStartOperation(sessionBean, "VIEW");
				}
				
				else if(sessionBean.getActionClicked().equals("ADD_GROUP"))
				{
					/*
					 * call search Operation
					 */
					searchGroupOperation(sessionBean, "ADDNEW");
				}
				else if(sessionBean.getActionClicked().equals("ADD_WMI"))
				{
					/*
					 * call search Operation
					 */
					searchWMIOperation(sessionBean, "ADDNEW");	
				}
				else if(sessionBean.getActionClicked().equals("ADD_VDS"))
				{
					/*
					 * call search Operation
					 */
					searchVDSOperation(sessionBean, "ADDNEW");
				}
				else if(sessionBean.getActionClicked().equals("ADD_VISSTART"))
				{
					/*
					 * call search Operation
					 */
					searchVISStartOperation(sessionBean, "ADDNEW");
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/vin.jsp");
				rs.forward(request, response);
			}
		}
	}

	private VinBean getSessionBean(HttpServletRequest request) 
	{
		VinBean sessionBean = null;
		if (null != request.getSession().getAttribute("vinBean") && !"".equals(request.getSession().getAttribute("vinBean"))) 
		{
			sessionBean = (VinBean) request.getSession().getAttribute("vinBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new VinBean();
			request.getSession().setAttribute("vinBean", sessionBean);
		}
		return sessionBean;
	}


	private  void clearSearchBlock(VinBean sessionBean)
	{
		sessionBean.setSearchGroupId(null);
		sessionBean.setSearchWMICode(null);
		sessionBean.setSearchVDSCode(null);
		sessionBean.setSearchVISStartRange(null);
		sessionBean.setSearchVISEndRange(null);

		sessionBean.setSearchGroupList(null);
		sessionBean.setSearchVDSList(null);
		sessionBean.setSearchVISStartList(null);
		sessionBean.setSearchVISEndList(null);
	}

	private  void clearAddNewBlock(VinBean sessionBean)
	{
		sessionBean.setAddGroupId(null);
		sessionBean.setAddVDSCode(null);
		sessionBean.setAddVISStartRange(null);
		sessionBean.setAddVISEndRange(null);
		sessionBean.setAddWmiCode(null);

		// set New Fields as Null
		sessionBean.setNewGroupCode(null);
		sessionBean.setNewVDSCode(null);
		sessionBean.setNewVISEndRange(null);
		sessionBean.setNewVISStartRange(null);

		sessionBean.setAddGroupList(null);
		sessionBean.setAddVDSList(null);
		sessionBean.setAddVISEndList(null);
		sessionBean.setAddVISStartList(null);
	}


	private  void searchVIN(VinBean sessionBean)
	{
		try
		{
			sessionBean.setVinList(new ArrayList<VinDetails>());

			/*
			 * SEARCH FILTERS - 
			 * GROUP CODE + WMI CODE + VDS CODE + VIS START + VIS END
				GROUP CODE + WMI CODE + VDS CODE + VIS START
				GROUP CODE + WMI CODE + VDS CODE + VIS END
				GROUP CODE + WMI CODE + VIS START + VIS END
				GROUP CODE + VDS CODE + VIS START + VIS END
				WMI CODE + VDS CODE + VIS START + VIS END
				GROUP CODE + WMI CODE + VDS CODE
				GROUP CODE + WMI CODE + VIS START
				GROUP CODE + WMI CODE + VIS END
				GROUP CODE + VDS CODE + VIS START
				GROUP CODE + VDS CODE + VIS END
				WMI CODE + VDS CODE + VIS START
				WMI CODE + VDS CODE + VIS END
				VDS CODE + VIS START + VIS END
				GROUP CODE + WMI CODE
				GROUP CODE + VDS CODE
				GROUP CODE + VIS START
				GROUP CODE + VIS END
				WMI CODE + VDS CODE
				WMI CODE + VIS START
				WMI CODE + VIS END
				VDS CODE + VIS START
				VDS CODE + VIS END
				VIS START + VIS END
				GROUP CODE
				WMI CODE
				VDS CODE
				VIS START
				VIS END
			 */
			if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) 
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//GROUP CODE + WMI CODE + VDS CODE + VIS START + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getWmiCode()
								&& null!=details.getVdsCode() 
								&& null!=details.getVisStartRange() 
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) 
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) )
			{
				//GROUP CODE + WMI CODE + VDS CODE + VIS START
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getWmiCode()
								&& null!=details.getVdsCode() 
								&& null!=details.getVisStartRange() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) ) 
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//GROUP CODE + WMI CODE + VDS CODE + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getWmiCode()
								&& null!=details.getVdsCode() 
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) 
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//GROUP CODE + WMI CODE + VIS START + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getWmiCode()
								&& null!=details.getVisStartRange() 
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) 
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) 
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//GROUP CODE + VDS CODE + VIS START + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getVdsCode() 
								&& null!=details.getVisStartRange() 
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) 
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) 
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//WMI CODE + VDS CODE + VIS START + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getWmiCode()
								&& null!=details.getVdsCode() 
								&& null!=details.getVisStartRange() 
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) 
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) )
			{
				//GROUP CODE + WMI CODE + VDS CODE 
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getWmiCode()
								&& null!=details.getVdsCode() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) )
			{
				//GROUP CODE + WMI CODE + VIS START 
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getWmiCode()
								&& null!=details.getVisStartRange() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//GROUP CODE + WMI CODE + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getWmiCode()
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) )
			{
				//GROUP CODE + VDS CODE + VIS START 
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getVdsCode() 
								&& null!=details.getVisStartRange() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//GROUP CODE + VDS CODE  + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getVdsCode() 
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) )
			{
				// WMI CODE + VDS CODE + VIS START 
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getWmiCode()
								&& null!=details.getVdsCode() 
								&& null!=details.getVisStartRange() )
						{
							if(sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				// WMI CODE + VDS CODE + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getWmiCode()
								&& null!=details.getVdsCode() 
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) 
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//VDS CODE + VIS START + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getVdsCode() 
								&& null!=details.getVisStartRange() 
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) 
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  )
			{
				//GROUP CODE + WMI CODE 
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getWmiCode())
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) )
			{
				//GROUP CODE + VDS CODE 
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getVdsCode() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) )
			{
				//GROUP CODE + VIS START 
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getVisStartRange() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) 
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//GROUP CODE + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() 
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) )
			{
				//WMI CODE + VDS CODE
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getWmiCode()
								&& null!=details.getVdsCode() )
						{
							if(sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) )
			{
				// WMI CODE + VIS START
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getWmiCode()
								&& null!=details.getVisStartRange() )
						{
							if(sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//WMI CODE + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getWmiCode()
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}

			else if(null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) )
			{
				//VDS CODE + VIS START
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getVdsCode() 
								&& null!=details.getVisStartRange() )
						{
							if(sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) 
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//VDS CODE +  VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getVdsCode() 
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) 
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) 
					&& null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//VIS START + VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getVisStartRange() 
								&& null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) 
									&& sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}

			else if(null!=sessionBean.getSearchGroupId() && !"".equals(sessionBean.getSearchGroupId()) )
			{
				//GROUP CODE 
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getGroupCode() )
						{
							if(sessionBean.getSearchGroupId().trim().toLowerCase().equals(details.getGroupCode().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchWMICode() && !"".equals(sessionBean.getSearchWMICode())  )
			{
				// WMI CODE
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getWmiCode())
						{
							if(sessionBean.getSearchWMICode().trim().toLowerCase().equals(details.getWmiCode().trim().toLowerCase())  )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchVDSCode() && !"".equals(sessionBean.getSearchVDSCode()) )
			{
				// VDS CODE
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getVdsCode() )
						{
							if(sessionBean.getSearchVDSCode().trim().toLowerCase().equals(details.getVdsCode().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchVISStartRange() && !"".equals(sessionBean.getSearchVISStartRange()) )
			{
				//VIS START 
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getVisStartRange() )
						{
							if(sessionBean.getSearchVISStartRange().trim().toLowerCase().equals(details.getVisStartRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchVISEndRange() && !"".equals(sessionBean.getSearchVISEndRange()) )
			{
				//VIS END
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllVINDataList().size();a++)
					{
						VinDetails details = (VinDetails)sessionBean.getAllVINDataList().get(a);
						if(null!=details.getVisEndRange() )
						{
							if(sessionBean.getSearchVISEndRange().trim().toLowerCase().equals(details.getVisEndRange().trim().toLowerCase()) )
							{
								sessionBean.getVinList().add(details);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "searchVIN()", e);
		}
	}

	private void getCountryLocaleList(VinBean sessionBean, HttpServletRequest request)
	{
		try
		{
			UserAccessBean userSessionBean = getUserSessionBean(request);
			sessionBean.setCountryLocaleList(new ArrayList<CountryLocaleDetails>());
			ArrayList<CountryLocaleDetails> list = new ArrayList<CountryLocaleDetails>();
			list = CountryLocaleDAO.getCountryLocaleDetailsListForCombo();
			if(null!=list && list.size()>0)
			{
				/*
				 * ITERATE LIST AND REMOVE ALL NON-WRITABLE MME COUNTRIES
				 */
				String nonWritableMMECountries=ApplicationProperties.getProperty("mme.not.writable.countries");
				String[] tokens = nonWritableMMECountries.split(",");
				if(null!=tokens && tokens.length>0)
				{
					for(int a=0;a<tokens.length;a++)
					{
						String ccCode = tokens[a];
						if(null!=list && list.size()>0)
						{
							for(int b=0;b<list.size();b++)
							{
								CountryLocaleDetails clDetails = (CountryLocaleDetails)list.get(b);
								if(clDetails.getCountryLocaleDesc().trim().toLowerCase().equals(ccCode.trim().toLowerCase()))
								{
									list.remove(b);
									b--;
									break;
								}
							}
						}
						ccCode = null;
					}
				}
				tokens = null;
				nonWritableMMECountries = null;
			}
			// PROCEED NOW
			if(null!=list && list.size()>0)
			{
				if(userSessionBean.isSuperAdminUser()==true)
				{
					sessionBean.setCountryLocaleList(list);
				}
				else if(userSessionBean.isSuperAdminUser()==false)
				{
					ArrayList<CountryLocaleDetails> finalCountryList = new ArrayList<CountryLocaleDetails>();
					// NOW CHECK FOR USER LOCALES
					if(null!=userSessionBean.getUserLocalesList() && userSessionBean.getUserLocalesList().size()>0)
					{
						/*
						 * CHECK IF USER LOCALE CONTIANS ANY MME LOCALES AND DOES NOT CONTAIN EN-UK LOCALE
						 * THEN ALLOW EN-UK EXPLICITLY.
						 */
						String mmeNonWritableLoclaes=ApplicationProperties.getProperty("mme.not.writable.locales");
						String[] localeTokens = mmeNonWritableLoclaes.split(",");
						String enukLocale = ApplicationProperties.getProperty("en_uk");
						enukLocale = enukLocale.replace("_", "-");
						boolean containsMMELocale=false;
						boolean containsENUKLocale=false;
						for(String contentLocale : userSessionBean.getUserLocalesList())
						{
							for(int b=0;b<localeTokens.length;b++)
							{
								String loc = localeTokens[b];
								loc = loc.replace("_", "-");
								if(contentLocale.trim().toLowerCase().equals(loc.trim().toLowerCase()))
								{
									// USER CONTAINS MME LOCALE
									containsMMELocale=true;
									break;
								}
								loc = null;
							}
						}
						
						if(containsMMELocale==true)
						{
							// CHECK WHETHER USER CONTAIN ENUK LOCALE
							for(String contentLocale : userSessionBean.getUserLocalesList())
							{
								if(contentLocale.trim().toLowerCase().equals(enukLocale.trim().toLowerCase()))
								{
									// USER CONTAINS ENUK LOCALE
									containsENUKLocale=true;
									break;
								}
							}
						}
						
						// PROCEED FOR IDENTIFYING COUNTRIES FOR COMBO
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
										/*
										 * IF MME LOCALE IS TRUE AND EN UK LOCALE IS FALSE
										 * EXPLICITY ADD ENUK LOCALE AS WELL
										 */
										if(containsMMELocale==true && containsENUKLocale==false)
										{
											String ukCountry="";
											if(enukLocale.lastIndexOf("-")!=-1)
											{
												ukCountry = enukLocale.substring(enukLocale.lastIndexOf("-")+1, enukLocale.length());
											}
											if(ukCountry.trim().toLowerCase().equals(cldDetails.getCountryLocaleDesc().trim().toLowerCase()))
											{
												// addToList
												addToList= true;
											}
											ukCountry = null;
										}
										

										// NORMAL FLOW - REMAINS AS IT IS
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
						mmeNonWritableLoclaes = null;
						enukLocale = null;
						localeTokens = null;
					}

					if(null!=finalCountryList && finalCountryList.size()>0)
					{
						CountryLocaleComparator countryLocaleComparator = new CountryLocaleComparator();
						Collections.sort(finalCountryList,countryLocaleComparator);
						sessionBean.setCountryLocaleList(finalCountryList);
						countryLocaleComparator = null;
					}
					finalCountryList=  null;
				}
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "getCountryLocaleList()", e);
		}
	}

	private void getLanguageList(VinBean sessionBean, HttpServletRequest request)
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
					/*
					 * ITERATE LIST AND REMOVE ALL NON-WRITABLE MME LANGUAGES
					 */
					String nonWritableMMELocales=ApplicationProperties.getProperty("mme.not.writable.locales");
					String[] tokens = nonWritableMMELocales.split(",");
					if(null!=tokens && tokens.length>0)
					{
						for(int a=0;a<tokens.length;a++)
						{
							String lcCode = tokens[a];
							if(null!=list && list.size()>0)
							{
								for(int b=0;b<list.size();b++)
								{
									ManualLanguageDetails mlDetails = (ManualLanguageDetails)list.get(b);
									if(mlDetails.getManualLanguageName().trim().toLowerCase().equals(lcCode.trim().toLowerCase()))
									{
										list.remove(b);
										b--;
										break;
									}
								}
							}
							lcCode = null;
						}
					}
					tokens = null;
					nonWritableMMELocales = null;
				}
				
				if(null!=list && list.size()>0)
				{
					UserAccessBean userSessionBean = getUserSessionBean(request);
					if(userSessionBean.isSuperAdminUser()==true)
					{
						sessionBean.setLanguageList(list);
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
							/*
							 * CHECK HERE IF USER CONTAINS ANY MME LOCALE BUT DOES NOT CONTAIN
							 * ENUK LOCALE - ADD IT EXPLICITYLY 
							 */
							/*
							 * CHECK IF USER LOCALE CONTIANS ANY MME LOCALES AND DOES NOT CONTAIN EN-UK LOCALE
							 * THEN ALLOW EN-UK EXPLICITLY.
							 */
							String mmeNonWritableLoclaes=ApplicationProperties.getProperty("mme.not.writable.locales");
							String[] localeTokens = mmeNonWritableLoclaes.split(",");
							String enukLocale = ApplicationProperties.getProperty("en_uk");
							enukLocale = enukLocale.replace("_", "-");
							boolean containsMMELocale=false;
							boolean containsENUKLocale=false;
							for(String contentLocale : userSessionBean.getUserLocalesList())
							{
								for(int b=0;b<localeTokens.length;b++)
								{
									String loc = localeTokens[b];
									loc = loc.replace("_", "-");
									if(contentLocale.trim().toLowerCase().equals(loc.trim().toLowerCase()))
									{
										// USER CONTAINS MME LOCALE
										containsMMELocale=true;
										break;
									}
									loc = null;
								}
							}
							
							if(containsMMELocale==true)
							{
								// CHECK WHETHER USER CONTAIN ENUK LOCALE
								for(String contentLocale : userSessionBean.getUserLocalesList())
								{
									if(contentLocale.trim().toLowerCase().equals(enukLocale.trim().toLowerCase()))
									{
										// USER CONTAINS ENUK LOCALE
										containsENUKLocale=true;
										break;
									}
								}
							}
							
							
							// PROCEED FOR IDENTIFYING LANGUAGES FOR COMBO
							for(String contentLocale : userSessionBean.getUserLocalesList())
							{
								if(null!=contentLocale && !"".equals(contentLocale))
								{
									for(ManualLanguageDetails mldDetails : list)
									{
										boolean addToList = false;
										
										/*
										 * IF MME LOCALE IS TRUE AND EN UK LOCALE IS FALSE
										 * EXPLICITY ADD ENUK LOCALE AS WELL
										 */
										if(containsMMELocale==true && containsENUKLocale==false)
										{
											if(enukLocale.trim().toLowerCase().equals(mldDetails.getManualLanguageName().trim().toLowerCase()))
											{
												// addToList
												addToList= true;
											}
										}
										
										// NORMAL FLOW - REMAINS AS IT IS
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
							
							mmeNonWritableLoclaes = null;
							enukLocale = null;
							localeTokens = null;
						}
						
						if(null!=finalLocaleList && finalLocaleList.size()>0)
						{
							ManualLanguageComparator manualLanguageComparator = new ManualLanguageComparator();
							Collections.sort(finalLocaleList,manualLanguageComparator);
							sessionBean.setLanguageList(finalLocaleList);
							manualLanguageComparator = null;
						}
						finalLocaleList=  null;
					}
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "getLanguageList()", e);
		}
	}

	private  void getFlagList(VinBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());

		sessionBean.setFlagList(Utilities.prepareFlagsList(msgProps));
	}


	private  void getCarlineList(VinBean sessionBean)
	{
		try
		{
			sessionBean.setCarlineList(new ArrayList<CarlineDetails>());
			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<CarlineDetails> list = new ArrayList<CarlineDetails>();
				String languageId=identifyLanguageId(sessionBean);
				/*
				 * IDENTIFY & FETCH ON LOCALE CODE
				 */
				list = CarlineDAO.getCarlineDetailsListForComboFORVIN(languageId);
				if(null!=list && list.size()>0)
				{
					sessionBean.setCarlineList(list);
				}
				list = null;
				languageId=null;
			}

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "getCarlineList()", e);
		}
	}

	private void getWmiList(VinBean sessionBean)
	{
		try
		{
			sessionBean.setSearchWMIList(new ArrayList<SelectItemDetails>());
			sessionBean.setAddWMIList(new ArrayList<SelectItemDetails>());
			if((null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId())))
			{
				String languageId=identifyLanguageId(sessionBean);
				ArrayList<SelectItemDetails> list=  new ArrayList<SelectItemDetails>();
				list = CarlineDAO.getWMIList(sessionBean.getCarlineCode(), sessionBean.getCarlineNameEng(), languageId, null,null);
				if(null!=list && list.size()>0)
				{
					// SET IN SEARCH WMI LIST
					sessionBean.setSearchWMIList(list);
					// SET IN ADD WMI LIST - ONLY WHEN CARLINE ENG NAME AND CARLINE CODE IS NOT NULL
					if(null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) 
						&& null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
					{
						sessionBean.setAddWMIList(list);
					}
				}
				list = null;
				languageId = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "getWMIList()", e);
		}
	}
	

	private  void getVinList(VinBean sessionBean)
	{
		try
		{
			sessionBean.setAllVINDataList(new ArrayList<VinDetails>());
			sessionBean.setVinList(new ArrayList<VinDetails>());

			clearSearchBlock(sessionBean);
			clearAddNewBlock(sessionBean);

			// initialize All ArrayLists to Avoid Null Pointers
			sessionBean.setSearchGroupList(new ArrayList<VinDetails>());
			sessionBean.setSearchVDSList(new ArrayList<VinDetails>());
			sessionBean.setSearchVISStartList(new ArrayList<VinDetails>());
			sessionBean.setSearchVISEndList(new ArrayList<VinDetails>());

			sessionBean.setAddGroupList(new ArrayList<VinDetails>());
			sessionBean.setAddVDSList(new ArrayList<VinDetails>());
			sessionBean.setAddVISEndList(new ArrayList<VinDetails>());
			sessionBean.setAddVISStartList(new ArrayList<VinDetails>());

			uniqueDataList= new ArrayList<VinDetails>();

			if((null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId())))
			{
				String languageId=identifyLanguageId(sessionBean);
				ArrayList<VinDetails> list = new ArrayList<VinDetails>();
				list = VinDAO.getVinDetailsList(sessionBean.getCarlineNameEng(), sessionBean.getCarlineCode(), languageId);
				if(null!=list && list.size()>0)
				{
					for(int i=0;i<list.size();i++)
					{
						VinDetails vinDetails = (VinDetails)list.get(i);
						if(null!=vinDetails.getFlag() && !"".equals(vinDetails.getFlag()))
						{
							// set Label
							if(vinDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
							{
								vinDetails.setFlagLabel(msgProps.getProperty("flag.label.active"));
							}
							else if(vinDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.sleep")))
							{
								vinDetails.setFlagLabel(msgProps.getProperty("flag.label.sleep"));
							}
							else if(vinDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
							{
								vinDetails.setFlagLabel(msgProps.getProperty("flag.label.draft"));
							}
							else if(vinDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
							{
								vinDetails.setFlagLabel(msgProps.getProperty("flag.label.deprecated"));
							}
							else if(vinDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.delete")))
							{
								vinDetails.setFlagLabel(msgProps.getProperty("flag.label.delete"));
							}
						}
						vinDetails = null;
					}
					sessionBean.setAllVINDataList(list);
					sessionBean.setVinList(list);
				}
				list = null;
				languageId = null;
				
				// EXPLICITY SET ALL DATA LIST TO WMI , VDS, VIS START & END LIST
				if(null!=sessionBean.getAllVINDataList() && sessionBean.getAllVINDataList().size()>0)
				{
					uniqueDataList = sessionBean.getAllVINDataList();
				}
				
				// DO NOT SET THE UNIQUE LIST TO NULL, AS THEY WILL BE USED FOR FILTERING COMBOS
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "getVinList()", e);
		}
	}

	private  void readParamsFromRequest(VinBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new VinDetails());
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setCarlineId(null);
			sessionBean.setCarlineCode(null);
			sessionBean.setCarlineNameEng(null);
//			sessionBean.setCarlineNameReg(null);
			sessionBean.setVinListToImport(null);

			sessionBean.setSearchGroupId(null);
			sessionBean.setSearchWMICode(null);
			sessionBean.setSearchVDSCode(null);
			sessionBean.setSearchVISStartRange(null);
			sessionBean.setSearchVISEndRange(null);

			sessionBean.setAddGroupId(null);
			sessionBean.setAddVDSCode(null);
			sessionBean.setAddVISStartRange(null);
			sessionBean.setAddVISEndRange(null);
			sessionBean.setAddWmiCode(null);

			sessionBean.setNewGroupCode(null);
			sessionBean.setNewVDSCode(null);
			sessionBean.setNewVISStartRange(null);
			sessionBean.setNewVISEndRange(null);

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
						if (fieldName.equals("VIN_UpdatedRows")) 
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


						if(fieldName.equals("VIN_SelectedRows"))
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
						if (fieldName.equals("VIN_DataTabel_displayPageNo")) 
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

						if (fieldName.equals("VIN_DataTabel_displayPageLen")) 
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

						if (fieldName.equals("VIN_SelectedRows")) 
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

						if (fieldName.equals("VIN_CountryLocale_Code")) 
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

						if (fieldName.equals("VIN_Lang_Code")) 
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

						if (fieldName.equals("VIN_CarId")) 
						{
							// set the value in sessionBean.setCarlineId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value) 
									&& !"ALL".equals(value)) 
							{
								sessionBean.setCarlineId(value);
								if(null!=sessionBean.getCarlineId() && !"".equals(sessionBean.getCarlineId()) && !"ALL".equals(sessionBean.getCarlineId()))
								{
									if(null!=sessionBean.getCarlineList() && sessionBean.getCarlineList().size()>0)
									{
										for(int r=0;r<sessionBean.getCarlineList().size();r++)
										{
											CarlineDetails cDetails = (CarlineDetails)sessionBean.getCarlineList().get(r);
											if(null!=cDetails.getCarlineIdForCombo() && !"".equals(cDetails.getCarlineIdForCombo()))
											{
												if(cDetails.getCarlineIdForCombo().trim().toLowerCase().equals(sessionBean.getCarlineId().trim().toLowerCase()))
												{
													sessionBean.setCarlineCode(cDetails.getCarlineCode());
													sessionBean.setCarlineNameEng(cDetails.getCarlineNameEng());
//													sessionBean.setCarlineNameReg(cDetails.getCarlineNameReg());
													break;
												}
											}
										}
									}
								}
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_SearchWmiId")) 
						{
							// set the value in sessionBean.setSearchWMICode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchWMICode(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_SearchGroupId")) 
						{
							// set the value in sessionBean.setSearchGroupId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchGroupId(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_SearchVDSId")) 
						{
							// set the value in sessionBean.setSearchVDSCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchVDSCode(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_SearchVISStartId")) 
						{
							// set the value in sessionBean.setSearchVISStartRange
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchVISStartRange(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_SearchVISEndId")) 
						{
							// set the value in sessionBean.setSearchVISEndRange
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchVISEndRange(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_AddWmiId")) 
						{
							// set the value in sessionBean.setAddWmiCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddWmiCode(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_AddGroupId")) 
						{
							// set the value in sessionBean.setAddGroupId
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddGroupId(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_AddVDSId")) 
						{
							// set the value in sessionBean.setAddVDSCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddVDSCode(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_AddVISStartId")) 
						{
							// set the value in sessionBean.setAddVISStartRange
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddVISStartRange(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_AddVISEndId")) 
						{
							// set the value in sessionBean.setSearchVISEndRange
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddVISEndRange(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_NewVDSCode")) 
						{
							// set the value in sessionBean.setNewVDSCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setNewVDSCode(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_NewVISStartRange")) 
						{
							// set the value in sessionBean.setNewVISStartRange
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setNewVISStartRange(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_NewVISEndRange")) 
						{
							// set the value in sessionBean.setNewVISEndRange
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setNewVISEndRange(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_NewGroupCode")) 
						{
							// set the value in sessionBean.setNewGroupCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setNewGroupCode(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_Engine_Code")) 
						{
							// set the value in sessionBean.getFieldDetails().setEngineCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setEngineCode(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_Mission_Code")) 
						{
							// set the value in sessionBean.getFieldDetails().setMissionCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setMissionCode(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("VIN_ActionClicked")) 
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
			Utilities.printStackTraceToLogs(Vin.class.getName(), "readParamsFromRequest(", e);
		}
	}

	private  boolean validateFileUpload(VinBean sessionBean)
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

	private  boolean validate(VinDetails fieldDetails, VinBean sessionBean)
	{

		StringBuilder errorMessage = new StringBuilder();
		boolean addMandatoryMessage = false;
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) ||
				null==sessionBean.getCarlineId() || "".equals(sessionBean.getCarlineId()) || 
				null==sessionBean.getAddGroupId() || "".equals(sessionBean.getAddGroupId()) || 
				null==sessionBean.getAddWmiCode() || "".equals(sessionBean.getAddWmiCode()) || 
				null==sessionBean.getAddVDSCode() || "".equals(sessionBean.getAddVDSCode()) || 
				null==sessionBean.getAddVISStartRange() || "".equals(sessionBean.getAddVISStartRange()) || 
				null==sessionBean.getAddVISEndRange() || "".equals(sessionBean.getAddVISEndRange()) || 
				null==fieldDetails.getEngineCode() || "".equals(fieldDetails.getEngineCode()) || 
				null==fieldDetails.getMissionCode() || "".equals(fieldDetails.getMissionCode()))
		{
			addMandatoryMessage= true;
		}

		if(null!=sessionBean.getAddGroupId() && !"".equals(sessionBean.getAddGroupId()))
		{
			if(sessionBean.getAddGroupId().equals("ADDNEW"))
			{
				if(null==sessionBean.getNewGroupCode() || "".equals(sessionBean.getNewGroupCode()))
				{
					addMandatoryMessage=true;
				}
			}
		}

		

		if(null!=sessionBean.getAddVDSCode() && !"".equals(sessionBean.getAddVDSCode()))
		{
			if(sessionBean.getAddVDSCode().equals("ADDNEW"))
			{
				if(null==sessionBean.getNewVDSCode() || "".equals(sessionBean.getNewVDSCode()))
				{
					addMandatoryMessage=true;
				}
			}
		}

		if(null!=sessionBean.getAddVISStartRange() && !"".equals(sessionBean.getAddVISStartRange()))
		{
			if(sessionBean.getAddVISStartRange().equals("ADDNEW"))
			{
				if(null==sessionBean.getNewVISStartRange() || "".equals(sessionBean.getNewVISStartRange()))
				{
					addMandatoryMessage=true;
				}
			}
		}

		if(null!=sessionBean.getAddVISEndRange() && !"".equals(sessionBean.getAddVISEndRange()))
		{
			if(sessionBean.getAddVISEndRange().equals("ADDNEW"))
			{
				if(null==sessionBean.getNewVISEndRange() || "".equals(sessionBean.getNewVISEndRange()))
				{
					addMandatoryMessage=true;
				}
			}
		}


		if(addMandatoryMessage==true)
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}


		if(null!=sessionBean.getNewGroupCode() && !"".equals(sessionBean.getNewGroupCode()))
		{
			if(sessionBean.getNewGroupCode().length()>5)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.groupcode"),"20"));
			}
		}

		if(null!=sessionBean.getNewVDSCode() && !"".equals(sessionBean.getNewVDSCode()))
		{
			if(sessionBean.getNewVDSCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.vdscode"),"20"));
			}
		}

		if(null!=sessionBean.getNewVISStartRange() && !"".equals(sessionBean.getNewVISStartRange()))
		{
			if(sessionBean.getNewVISStartRange().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.visstartrange"),"20"));
			}
		}

		if(null!=sessionBean.getNewVISEndRange() && !"".equals(sessionBean.getNewVISEndRange()))
		{
			if(sessionBean.getNewVISEndRange().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.visendrange"),"20"));
			}
		}

		if(null!=fieldDetails.getEngineCode() && !"".equals(fieldDetails.getEngineCode()))
		{
			if(fieldDetails.getEngineCode().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.enginecode"),"200"));
			}
		}

		if(null!=fieldDetails.getMissionCode() && !"".equals(fieldDetails.getMissionCode()))
		{
			if(fieldDetails.getMissionCode().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.transmissioncode"),"200"));
			}
		}

		if(null!=sessionBean.getAddGroupId() && !"".equals(sessionBean.getAddGroupId()))
		{
			if(sessionBean.getAddGroupId().equals("ADDNEW"))
			{
				if(null!=sessionBean.getNewGroupCode() && !"".equals(sessionBean.getNewGroupCode()))
				{
					fieldDetails.setGroupCode(sessionBean.getNewGroupCode());
				}
			}
			else
			{
				fieldDetails.setGroupCode(sessionBean.getAddGroupId());
			}
		}

		// SET WMI CODE
		if(null!=sessionBean.getAddWmiCode() && !"".equals(sessionBean.getAddWmiCode()))
		{
			fieldDetails.setWmiCode(sessionBean.getAddWmiCode());
		}

		if(null!=sessionBean.getAddVDSCode() && !"".equals(sessionBean.getAddVDSCode()))
		{
			if(sessionBean.getAddVDSCode().equals("ADDNEW"))
			{
				if(null!=sessionBean.getNewVDSCode() && !"".equals(sessionBean.getNewVDSCode()))
				{
					fieldDetails.setVdsCode(sessionBean.getNewVDSCode());
				}
			}
			else
			{
				fieldDetails.setVdsCode(sessionBean.getAddVDSCode());
			}
		}

		if(null!=sessionBean.getAddVISStartRange() && !"".equals(sessionBean.getAddVISStartRange()))
		{
			if(sessionBean.getAddVISStartRange().equals("ADDNEW"))
			{
				if(null!=sessionBean.getNewVISStartRange() && !"".equals(sessionBean.getNewVISStartRange()))
				{
					fieldDetails.setVisStartRange(sessionBean.getNewVISStartRange());
				}
			}
			else
			{
				fieldDetails.setVisStartRange(sessionBean.getAddVISStartRange());
			}
		}

		if(null!=sessionBean.getAddVISEndRange() && !"".equals(sessionBean.getAddVISEndRange()))
		{
			if(sessionBean.getAddVISEndRange().equals("ADDNEW"))
			{
				if(null!=sessionBean.getNewVISEndRange() && !"".equals(sessionBean.getNewVISEndRange()))
				{
					fieldDetails.setVisEndRange(sessionBean.getNewVISEndRange());
				}
			}
			else
			{
				fieldDetails.setVisEndRange(sessionBean.getAddVISEndRange());
			}
		}
		
		if(null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) &&
				null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()) &&
				null!=fieldDetails.getVisStartRange() && !"".equals(fieldDetails.getVisStartRange()) &&
				null!=fieldDetails.getVisEndRange() && !"".equals(fieldDetails.getVisEndRange()) &&
				null!=fieldDetails.getGroupCode() && !"".equals(fieldDetails.getGroupCode()))
		{
			if(null!=sessionBean.getVinList() && sessionBean.getVinList().size()>0)
			{
				for(int a=0;a<sessionBean.getVinList().size();a++)
				{
					VinDetails vinDetails = (VinDetails)sessionBean.getVinList().get(a);
					if(vinDetails.getWmiCode().trim().toLowerCase().equals(fieldDetails.getWmiCode().trim().toLowerCase())
							&& vinDetails.getVdsCode().trim().toLowerCase().equals(fieldDetails.getVdsCode().trim().toLowerCase())
							&& vinDetails.getVisStartRange().trim().toLowerCase().equals(fieldDetails.getVisStartRange().trim().toLowerCase())
							&& vinDetails.getVisEndRange().trim().toLowerCase().equals(fieldDetails.getVisEndRange().trim().toLowerCase())
							&& vinDetails.getGroupCode().trim().toLowerCase().equals(fieldDetails.getGroupCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique.vin", msgProps.getProperty("label.vin")+" "+msgProps.getProperty("label.details")));
						break;
					}
					vinDetails=  null;
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

	private  boolean validateUpdate(VinDetails fieldDetails, VinBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) ||
				null==fieldDetails.getVdsCode() || "".equals(fieldDetails.getVdsCode()) ||
				null==fieldDetails.getVisStartRange() || "".equals(fieldDetails.getVisStartRange()) ||
				null==fieldDetails.getVisEndRange() || "".equals(fieldDetails.getVisEndRange()) ||
				null==fieldDetails.getGroupCode() || "".equals(fieldDetails.getGroupCode()) ||
				null==fieldDetails.getEngineCode() || "".equals(fieldDetails.getEngineCode()) ||
				null==fieldDetails.getMissionCode() || "".equals(fieldDetails.getMissionCode()))
		{
			if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
			{
				errorMessage.append("<MSG_TOKEN>");
			}
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}

		if(null!=fieldDetails.getGroupCode() && !"".equals(fieldDetails.getGroupCode()))
		{
			if(fieldDetails.getGroupCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.groupcode")+",20,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
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
				String data = msgProps.getProperty("label.vdscode")+",20,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}

		if(null!=fieldDetails.getVisStartRange() && !"".equals(fieldDetails.getVisStartRange()))
		{
			if(fieldDetails.getVisStartRange().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.visstartrange")+",20,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}

		if(null!=fieldDetails.getVisEndRange() && !"".equals(fieldDetails.getVisEndRange()))
		{
			if(fieldDetails.getVisEndRange().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.visendrange")+",20,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}

		if(null!=fieldDetails.getEngineCode() && !"".equals(fieldDetails.getEngineCode()))
		{
			if(fieldDetails.getEngineCode().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.enginecode")+",200,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}

		if(null!=fieldDetails.getMissionCode() && !"".equals(fieldDetails.getMissionCode()))
		{
			if(fieldDetails.getMissionCode().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.transmissioncode")+",200,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}

		/*
		 * check for carlineEngName & carlineCode
		 */
		if(null!=fieldDetails.getVinId() && fieldDetails.getVinId()>0 && 
				null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()) && 
				null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()) && 
				null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()) &&
				null!=fieldDetails.getVdsCode() && !"".equals(fieldDetails.getVdsCode()) &&
				null!=fieldDetails.getVisStartRange() && !"".equals(fieldDetails.getVisStartRange()) &&
				null!=fieldDetails.getVisEndRange() && !"".equals(fieldDetails.getVisEndRange()) &&
				null!=fieldDetails.getGroupCode() && !"".equals(fieldDetails.getGroupCode()) &&
				null!=sessionBean.getVinList() && sessionBean.getVinList().size()>0)
		{
			for(int a=0;a<sessionBean.getVinList().size();a++)
			{
				VinDetails vinDetails = (VinDetails)sessionBean.getVinList().get(a);
				//  check on carlineEngName, & carlineCode - as unique Entity is carlineCode + carlineName
				if(fieldDetails.getVinId()!=vinDetails.getVinId() 
						&& vinDetails.getCarlineCode().trim().toLowerCase().equals(fieldDetails.getCarlineCode().trim().toLowerCase()) 
						&& vinDetails.getCarlineNameEng().trim().toLowerCase().equals(fieldDetails.getCarlineNameEng().trim().toLowerCase()) 
						&& vinDetails.getWmiCode().trim().toLowerCase().equals(fieldDetails.getWmiCode().trim().toLowerCase())
						&& vinDetails.getVdsCode().trim().toLowerCase().equals(fieldDetails.getVdsCode().trim().toLowerCase())
						&& vinDetails.getVisStartRange().trim().toLowerCase().equals(fieldDetails.getVisStartRange().trim().toLowerCase())
						&& vinDetails.getVisEndRange().trim().toLowerCase().equals(fieldDetails.getVisEndRange().trim().toLowerCase())
						&& vinDetails.getGroupCode().trim().toLowerCase().equals(fieldDetails.getGroupCode().trim().toLowerCase()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					String data = msgProps.getProperty("label.vin")+" "+msgProps.getProperty("label.details") +","+String.valueOf(fieldDetails.getSrNo());
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.unique.update.vin"));
					data = null;
					id = null;
					break;
				}
				vinDetails=  null;
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
	

	private  boolean validateExcelRowData(VinBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		Connection conn = null;
		String connClosed="N";
		try
		{
			conn = DBConnectionHelper.getConnection();
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "validateExcelRowData()", e);
		}
		if(null!=sessionBean.getVinListToImport() && sessionBean.getVinListToImport().size()>0)
		{
			for(int i=0;i<sessionBean.getVinListToImport().size();i++)
			{
				VinDetails fieldDetails = (VinDetails) sessionBean.getVinListToImport().get(i);
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

				/*
				 * CHECK ON BOTH CARLINE NAME + CARLINE CODE
				 */
				if(null==fieldDetails.getCarlineCode() || "".equals(fieldDetails.getCarlineCode()) || 
						null==fieldDetails.getCarlineNameEng() || "".equals(fieldDetails.getCarlineNameEng()) || 
						null==fieldDetails.getWmiCode() || "".equals(fieldDetails.getWmiCode()) ||
						null==fieldDetails.getVdsCode() || "".equals(fieldDetails.getVdsCode()) ||
						null==fieldDetails.getVisStartRange() || "".equals(fieldDetails.getVisStartRange()) ||
						null==fieldDetails.getVisEndRange() || "".equals(fieldDetails.getVisEndRange()) ||
						null==fieldDetails.getGroupCode() || "".equals(fieldDetails.getGroupCode()) ||
						null==fieldDetails.getEngineCode() || "".equals(fieldDetails.getEngineCode()) ||
						null==fieldDetails.getMissionCode() || "".equals(fieldDetails.getMissionCode()))
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
				
				
				if(null!=fieldDetails.getCarlineNameEng() && !"".equals(fieldDetails.getCarlineNameEng()) && 
						null!=fieldDetails.getCarlineCode() && !"".equals(fieldDetails.getCarlineCode()))
				{
					String keyToMatch = fieldDetails.getCarlineNameEng().trim().toUpperCase()+"_"+fieldDetails.getCarlineCode().trim().toUpperCase()+"";
					boolean matchFound = false;
					/*
					 * CHECK WHETEHR THE CARLINE NAME IS VALID OR NOT
					 */
					if(null!=sessionBean.getCarlineList() && sessionBean.getCarlineList().size()>0)
					{
						for(int a=0;a<sessionBean.getCarlineList().size();a++)
						{
							CarlineDetails carDetails = (CarlineDetails)sessionBean.getCarlineList().get(a);
							if(null!=carDetails.getCarlineIdForCombo() && !"".equals(carDetails.getCarlineIdForCombo()))
							{
								if(carDetails.getCarlineIdForCombo().trim().toLowerCase().equals(keyToMatch.trim().toLowerCase()))
								{
									// setEnglish Name
									fieldDetails.setCarlineNameEng(carDetails.getCarlineNameEng());
									// set carlineCode
									fieldDetails.setCarlineCode(carDetails.getCarlineCode());
									matchFound = true;
									break;
								}
							}
						}
					}
					
					
					if(matchFound==false)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						//error.excel.improper.lines
						String data = msgProps.getProperty("label.model")+","+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
					keyToMatch = null;
					
					// CHECK FOR WMI CODE AS WELL
					if(null!=fieldDetails.getWmiCode() && !"".equals(fieldDetails.getWmiCode()))
					{
						/*
						 * CHECK WHETHER THE WMI CODE EXISTS FOR THE PROVIDED CARLINE ENG NAME & CARLINE CODE OR NOT
						 * DO THIS CHECK ONLY WHEN CARLINE ENG NAME & CARLINE CODE ARE NOT NULL.
						 */
						boolean wmiMatchFound = false;
						try
						{
							String languageId = identifyLanguageId(sessionBean);
							ArrayList<SelectItemDetails> list = CarlineDAO.getWMIList(fieldDetails.getCarlineCode(), fieldDetails.getCarlineNameEng(), languageId, conn, connClosed);
							if(null!=list && list.size()>0)
							{
								for(int a=0;a<list.size();a++)
								{
									SelectItemDetails si = (SelectItemDetails) list.get(a); 
									if(si.getValue().trim().toLowerCase().equals(fieldDetails.getWmiCode().trim().toLowerCase()))
									{
										// WMI IS APPLICABLE FOR IT
										wmiMatchFound = true;
										break;
									}
								}
							}
							languageId = null;
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(Vin.class.getName(), "validateExcelRowData()", e);
						}
						
						if(wmiMatchFound==false)
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							//error.excel.improper.lines
							String data = msgProps.getProperty("label.wmicode")+","+String.valueOf(rowNo);
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
							data = null;
							id = null;
							// increment errorCount by 1
							errorCount++;
						}
					}
				}

				
				
				
				if(null!=fieldDetails.getGroupCode() && !"".equals(fieldDetails.getGroupCode()))
				{
					if(fieldDetails.getGroupCode().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.groupcode")+",20,"+String.valueOf(rowNo);
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

				if(null!=fieldDetails.getVisStartRange() && !"".equals(fieldDetails.getVisStartRange()))
				{
					if(fieldDetails.getVisStartRange().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.visstartrange")+",20,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getVisEndRange() && !"".equals(fieldDetails.getVisEndRange()))
				{
					if(fieldDetails.getVisEndRange().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.visendrange")+",20,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getEngineCode() && !"".equals(fieldDetails.getEngineCode()))
				{
					if(fieldDetails.getEngineCode().length()>200)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.enginecode")+",200,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getMissionCode() && !"".equals(fieldDetails.getMissionCode()))
				{
					if(fieldDetails.getMissionCode().length()>200)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.transmissioncode")+",200,"+String.valueOf(rowNo);
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
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.vin")));
			// increment errorCount by 1
			errorCount++;
		}

		connClosed = null;
		if(null!=conn)
		{
			try
			{
				// close connection
				conn.close();
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(Vin.class.getName(), "validateExcelRowData()", e);
			}
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

	private  void decideErrorDisplay(VinBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_VIN_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(Vin.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				Utilities.printStackTraceToLogs(Vin.class.getName(), "validateExcelRowData()", e);
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


	private void searchGroupOperation(VinBean sessionBean, String operationType)
	{
		try
		{
			String wmiIdToMatch="";
			String groupIdToMatch="";
			if(operationType=="VIEW")
			{
				// set VDS, VIS START & VIS END SEARCH LIST TO NULL
				sessionBean.setSearchVDSCode(null);
				sessionBean.setSearchVISStartRange(null);
				sessionBean.setSearchVISEndRange(null);
				sessionBean.setSearchVDSList(new ArrayList<VinDetails>());
				sessionBean.setSearchVISStartList(new ArrayList<VinDetails>());
				sessionBean.setSearchVISEndList(new ArrayList<VinDetails>());
				
				wmiIdToMatch = sessionBean.getSearchWMICode();
				groupIdToMatch = sessionBean.getSearchGroupId();
			}
			else if(operationType.equals("ADDNEW"))
			{
				sessionBean.setAddVDSCode(null);
				sessionBean.setAddVISStartRange(null);
				sessionBean.setAddVISEndRange(null);

				// set New Fields as Null
				sessionBean.setNewVDSCode(null);
				sessionBean.setNewVISEndRange(null);
				sessionBean.setNewVISStartRange(null);

				sessionBean.setAddVDSList(new ArrayList<VinDetails>());
				sessionBean.setAddVISEndList(new ArrayList<VinDetails>());
				sessionBean.setAddVISStartList(new ArrayList<VinDetails>());
				
				wmiIdToMatch = sessionBean.getAddWmiCode();
				groupIdToMatch = sessionBean.getAddGroupId();
			}
			
			
			ArrayList<VinDetails> searchedList = new ArrayList<VinDetails>();
			/*
			 * FILTER WMI LIST ON THE BASIS OF SELECTED GROUP CODE
			 */
			if(null!=groupIdToMatch && !"".equals(groupIdToMatch) && !"ADDNEW".equals(groupIdToMatch))
			{
				if(null!=uniqueDataList && uniqueDataList.size()>0)
				{
					for(int a=0;a<uniqueDataList.size();a++)
					{
						VinDetails vDetails = (VinDetails) uniqueDataList.get(a);
						if(null!=vDetails.getGroupCode())
						{
							if(null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()) 
									&& null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()))
							{
								if(vDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getCarlineNameEng().trim().toLowerCase()) 
										&& vDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getCarlineCode().trim().toLowerCase()) &&
										vDetails.getWmiCode().trim().toLowerCase().equals(wmiIdToMatch.trim().toLowerCase()) && 
										vDetails.getGroupCode().trim().toLowerCase().equals(groupIdToMatch.trim().toLowerCase()))
								{
									/*
									 * CHECK HERE WHETHER ALREADY ADDDED OR NOT
									 */
									boolean add=true;
									if(null!=searchedList && searchedList.size()>0)
									{
										for(int e=0;e<searchedList.size();e++)
										{
											VinDetails v = (VinDetails)searchedList.get(e);
											if(null!=v.getVdsCode() && null!=vDetails.getVdsCode())
											{
												if(v.getVdsCode().trim().toLowerCase().equals(vDetails.getVdsCode().trim().toLowerCase()))
												{
													// already Exists
													add = false;
													break;
												}
												v = null;
											}
										}
									}
									if(add==true)
									{
										if(null!=vDetails.getVdsCode() && !"".equals(vDetails.getVdsCode()))
										{
											if(!"".equals(vDetails.getVdsCode().trim()))
											{
												searchedList.add(vDetails);
											}
										}
									}
								}
							}
							else
							{
								if(vDetails.getWmiCode().trim().toLowerCase().equals(wmiIdToMatch.trim().toLowerCase()) && 
										vDetails.getGroupCode().trim().toLowerCase().
										equals(groupIdToMatch.trim().toLowerCase()))
								{
									/*
									 * CHECK HERE WHETHER ALREADY ADDDED OR NOT
									 */
									boolean add=true;
									if(null!=searchedList && searchedList.size()>0)
									{
										for(int e=0;e<searchedList.size();e++)
										{
											VinDetails v = (VinDetails)searchedList.get(e);
											if(null!=v.getVdsCode() && null!=vDetails.getVdsCode())
											{
												if(v.getVdsCode().trim().toLowerCase().equals(vDetails.getVdsCode().trim().toLowerCase()))
												{
													// already Exists
													add = false;
													break;
												}
												v = null;
											}
										}
									}
									if(add==true)
									{
										if(null!=vDetails.getVdsCode() && !"".equals(vDetails.getVdsCode()))
										{
											if(!"".equals(vDetails.getVdsCode().trim()))
											{
												searchedList.add(vDetails);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			if(operationType.equals("VIEW"))
			{
				if(null!=searchedList && searchedList.size()>0)
				{
					sessionBean.setSearchVDSList(searchedList);
				}
			}
			else if(operationType.equals("ADDNEW"))
			{
				// MAKE SURE - CARLINE NAME & CODE IS NOT NULL - E.G. MODEL IS SELECTED
				if(null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()) 
						&& null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()))
				{
					VinDetails vd = new VinDetails();
					vd.setVdsCode("ADDNEW");
					sessionBean.getAddVDSList().add(vd);
					vd = null;
					if(null!=searchedList && searchedList.size()>0)
					{
						sessionBean.getAddVDSList().addAll(searchedList);
					}
				}
			}
			searchedList = null;
			groupIdToMatch = null;
			wmiIdToMatch = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "searchGroupOperation()", e);
		}
	}
	
	private void searchWMIOperation(VinBean sessionBean, String operationType)
	{
		try
		{
			String wmiIdToMatch="";
			if(operationType=="VIEW")
			{
				// set GROUP CODE, VIS START & VIS END SEARCH LIST TO NULL
				sessionBean.setSearchGroupId(null);
				sessionBean.setSearchVDSCode(null);
				sessionBean.setSearchVISStartRange(null);
				sessionBean.setSearchVISEndRange(null);
				sessionBean.setSearchGroupList(new ArrayList<VinDetails>());
				sessionBean.setSearchVDSList(new ArrayList<VinDetails>());
				sessionBean.setSearchVISStartList(new ArrayList<VinDetails>());
				sessionBean.setSearchVISEndList(new ArrayList<VinDetails>());
				
				wmiIdToMatch = sessionBean.getSearchWMICode();
			}
			else if(operationType.equals("ADDNEW"))
			{
				sessionBean.setAddGroupId(null);
				sessionBean.setAddVDSCode(null);
				sessionBean.setAddVISStartRange(null);
				sessionBean.setAddVISEndRange(null);

				// set New Fields as Null
				sessionBean.setNewGroupCode(null);
				sessionBean.setNewVDSCode(null);
				sessionBean.setNewVISEndRange(null);
				sessionBean.setNewVISStartRange(null);

				sessionBean.setAddGroupList(new ArrayList<VinDetails>());
				sessionBean.setAddVDSList(new ArrayList<VinDetails>());
				sessionBean.setAddVISEndList(new ArrayList<VinDetails>());
				sessionBean.setAddVISStartList(new ArrayList<VinDetails>());
				
				wmiIdToMatch = sessionBean.getAddWmiCode();
			}
			
			
			ArrayList<VinDetails> searchedList = new ArrayList<VinDetails>();
			/*
			 * FILTER GROUP LIST ON THE BASIS OF SELECTED WMI CODE
			 */
			if(null!=wmiIdToMatch && !"".equals(wmiIdToMatch) && !"ADDNEW".equals(wmiIdToMatch))
			{
				if(null!=uniqueDataList && uniqueDataList.size()>0)
				{
					for(int a=0;a<uniqueDataList.size();a++)
					{
						VinDetails vDetails = (VinDetails) uniqueDataList.get(a);
						if(null!=vDetails.getGroupCode() && null!=vDetails.getGroupCode())
						{
							if(null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) && 
								null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
							{
								if(vDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getCarlineNameEng().trim().toLowerCase()) 
										&& vDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getCarlineCode().trim().toLowerCase()) && 
										vDetails.getWmiCode().trim().toLowerCase().equals(wmiIdToMatch.trim().toLowerCase()))
								{
									/*
									 * CHECK HERE WHETHER ALREADY ADDDED OR NOT
									 */
									boolean add=true;
									if(null!=searchedList && searchedList.size()>0)
									{
										for(int e=0;e<searchedList.size();e++)
										{
											VinDetails v = (VinDetails)searchedList.get(e);
											if(null!=v.getGroupCode() && null!=vDetails.getGroupCode())
											{
												if(v.getGroupCode().trim().toLowerCase().equals(vDetails.getGroupCode().trim().toLowerCase()))
												{
													// already Exists
													add = false;
													break;
												}
												v = null;
											}
										}
									}
									if(add==true)
									{
										if(null!=vDetails.getGroupCode() && !"".equals(vDetails.getGroupCode()))
										{
											if(!"".equals(vDetails.getGroupCode().trim()))
											{
												searchedList.add(vDetails);
											}
										}
									}
								}
							}
							else
							{
								if(vDetails.getWmiCode().trim().toLowerCase().equals(wmiIdToMatch.trim().toLowerCase()))
								{
									/*
									 * CHECK HERE WHETHER ALREADY ADDDED OR NOT
									 */
									boolean add=true;
									if(null!=searchedList && searchedList.size()>0)
									{
										for(int e=0;e<searchedList.size();e++)
										{
											VinDetails v = (VinDetails)searchedList.get(e);
											if(null!=v.getGroupCode() && null!=vDetails.getGroupCode())
											{
												if(v.getGroupCode().trim().toLowerCase().equals(vDetails.getGroupCode().trim().toLowerCase()))
												{
													// already Exists
													add = false;
													break;
												}
												v = null;
											}
										}
									}
									if(add==true)
									{
										if(null!=vDetails.getGroupCode() && !"".equals(vDetails.getGroupCode()))
										{
											if(!"".equals(vDetails.getGroupCode().trim()))
											{
												searchedList.add(vDetails);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			if(operationType.equals("VIEW"))
			{
				if(null!=searchedList && searchedList.size()>0)
				{
					sessionBean.setSearchGroupList(searchedList);
				}
			}
			else if(operationType.equals("ADDNEW"))
			{
				// ONLY WHEN MODEL IS SELECTED
				if(null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) && 
						null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
				{
					VinDetails vd = new VinDetails();
					vd.setGroupCode("ADDNEW");
					sessionBean.getAddGroupList().add(vd);
					vd = null;
					if(null!=searchedList && searchedList.size()>0)
					{
						sessionBean.getAddGroupList().addAll(searchedList);
					}
				}
			}
			searchedList = null;
			wmiIdToMatch = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "searchWMIOperation()", e);
		}
	}
	
	private void searchVDSOperation(VinBean sessionBean, String operationType)
	{
		try
		{
			String groupIdToMatch="";
			String wmiIdToMatch="";
			String vdsIdToMatch="";
			if(operationType=="VIEW")
			{
				// set VIS START & VIS END SEARCH LIST TO NULL
				sessionBean.setSearchVISStartRange(null);
				sessionBean.setSearchVISEndRange(null);
				sessionBean.setSearchVISStartList(new ArrayList<VinDetails>());
				sessionBean.setSearchVISEndList(new ArrayList<VinDetails>());
				
				groupIdToMatch = sessionBean.getSearchGroupId();
				wmiIdToMatch = sessionBean.getSearchWMICode();
				vdsIdToMatch = sessionBean.getSearchVDSCode();
			}
			else if(operationType.equals("ADDNEW"))
			{
				sessionBean.setAddVISStartRange(null);
				sessionBean.setAddVISEndRange(null);

				// set New Fields as Null
				sessionBean.setNewVISEndRange(null);
				sessionBean.setNewVISStartRange(null);

				sessionBean.setAddVISEndList(new ArrayList<VinDetails>());
				sessionBean.setAddVISStartList(new ArrayList<VinDetails>());
				
				groupIdToMatch = sessionBean.getAddGroupId();
				wmiIdToMatch = sessionBean.getAddWmiCode();
				vdsIdToMatch = sessionBean.getAddVDSCode();
			}
			
			
			ArrayList<VinDetails> searchedList = new ArrayList<VinDetails>();
			/*
			 * FILTER VIS START LIST ON THE BASIS OF SELECTED GROUP, WMI CODE & VDS
			 */
			if(null!=groupIdToMatch && !"".equals(groupIdToMatch) && !"ADDNEW".equals(groupIdToMatch) && 
					null!=wmiIdToMatch && !"".equals(wmiIdToMatch) && !"ADDNEW".equals(wmiIdToMatch) && 
					null!=vdsIdToMatch && !"".equals(vdsIdToMatch) && !"ADDNEW".equals(vdsIdToMatch))
			{
				if(null!=uniqueDataList && uniqueDataList.size()>0)
				{
					for(int a=0;a<uniqueDataList.size();a++)
					{
						VinDetails vDetails = (VinDetails) uniqueDataList.get(a);
						if(null!=vDetails.getGroupCode() && null!=vDetails.getWmiCode() && null!=vDetails.getVdsCode())
						{
							if(null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) && 
								null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
							{
								if(vDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getCarlineNameEng().trim().toLowerCase()) 
										&& vDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getCarlineCode().trim().toLowerCase()) && 
										vDetails.getGroupCode().trim().toLowerCase().equals(groupIdToMatch.trim().toLowerCase()) && 
										vDetails.getWmiCode().trim().toLowerCase().equals(wmiIdToMatch.trim().toLowerCase()) && 
										vDetails.getVdsCode().trim().toLowerCase().equals(vdsIdToMatch.trim().toLowerCase()))
								{
									/*
									 * CHECK HERE WHETHER ALREADY ADDDED OR NOT
									 */
									boolean add=true;
									if(null!=searchedList && searchedList.size()>0)
									{
										for(int e=0;e<searchedList.size();e++)
										{
											VinDetails v = (VinDetails)searchedList.get(e);
											if(null!=v.getVisStartRange() && null!=vDetails.getVisStartRange())
											{
												if(v.getVisStartRange().trim().toLowerCase().equals(vDetails.getVisStartRange().trim().toLowerCase()))
												{
													// already Exists
													add = false;
													break;
												}
												v = null;
											}
										}
									}
									if(add==true)
									{
										if(null!=vDetails.getVisStartRange() && !"".equals(vDetails.getVisStartRange()))
										{
											if(!"".equals(vDetails.getVisStartRange().trim()))
											{
												searchedList.add(vDetails);
											}
										}
									}
								}
							}
							else
							{
								if(vDetails.getGroupCode().trim().toLowerCase().equals(groupIdToMatch.trim().toLowerCase()) && 
										vDetails.getWmiCode().trim().toLowerCase().equals(wmiIdToMatch.trim().toLowerCase()) && 
										vDetails.getVdsCode().trim().toLowerCase().equals(vdsIdToMatch.trim().toLowerCase()))
								{
									/*
									 * CHECK HERE WHETHER ALREADY ADDDED OR NOT
									 */
									boolean add=true;
									if(null!=searchedList && searchedList.size()>0)
									{
										for(int e=0;e<searchedList.size();e++)
										{
											VinDetails v = (VinDetails)searchedList.get(e);
											if(null!=v.getVisStartRange() && null!=vDetails.getVisStartRange())
											{
												if(v.getVisStartRange().trim().toLowerCase().equals(vDetails.getVisStartRange().trim().toLowerCase()))
												{
													// already Exists
													add = false;
													break;
												}
												v = null;
											}
										}
									}
									if(add==true)
									{
										if(null!=vDetails.getVisStartRange() && !"".equals(vDetails.getVisStartRange()))
										{
											if(!"".equals(vDetails.getVisStartRange().trim()))
											{
												searchedList.add(vDetails);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			if(operationType.equals("VIEW"))
			{
				if(null!=searchedList && searchedList.size()>0)
				{
					sessionBean.setSearchVISStartList(searchedList);
				}
			}
			else if(operationType.equals("ADDNEW"))
			{
				// ONLY WHEN MODEL IS SELECTED
				if(null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) && 
						null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
				{
					VinDetails vd = new VinDetails();
					vd.setVisStartRange("ADDNEW");
					sessionBean.getAddVISStartList().add(vd);
					vd = null;
					if(null!=searchedList && searchedList.size()>0)
					{
						sessionBean.getAddVISStartList().addAll(searchedList);
					}
				}
			}
			searchedList = null;
			wmiIdToMatch = null;
			groupIdToMatch= null;
			vdsIdToMatch=  null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "searchVDSOperation()", e);
		}
	}
	
	private void searchVISStartOperation(VinBean sessionBean, String operationType)
	{
		try
		{
			String groupIdToMatch="";
			String wmiIdToMatch="";
			String vdsIdToMatch="";
			String visStartToMatch="";
			if(operationType=="VIEW")
			{
				// set VIS END SEARCH LIST TO NULL
				sessionBean.setSearchVISEndRange(null);
				sessionBean.setSearchVISEndList(new ArrayList<VinDetails>());
				
				groupIdToMatch = sessionBean.getSearchGroupId();
				wmiIdToMatch = sessionBean.getSearchWMICode();
				vdsIdToMatch = sessionBean.getSearchVDSCode();
				visStartToMatch = sessionBean.getSearchVISStartRange();
			}
			else if(operationType.equals("ADDNEW"))
			{
				sessionBean.setAddVISEndRange(null);

				// set New Fields as Null
				sessionBean.setNewVISEndRange(null);

				sessionBean.setAddVISEndList(new ArrayList<VinDetails>());
				
				groupIdToMatch = sessionBean.getAddGroupId();
				wmiIdToMatch = sessionBean.getAddWmiCode();
				vdsIdToMatch = sessionBean.getAddVDSCode();
				visStartToMatch = sessionBean.getAddVISStartRange();
			}
			
			
			ArrayList<VinDetails> searchedList = new ArrayList<VinDetails>();
			/*
			 * FILTER VIS START LIST ON THE BASIS OF SELECTED GROUP, WMI CODE & VDS
			 */
			if(null!=groupIdToMatch && !"".equals(groupIdToMatch) && !"ADDNEW".equals(groupIdToMatch) && 
					null!=wmiIdToMatch && !"".equals(wmiIdToMatch) && !"ADDNEW".equals(wmiIdToMatch) && 
					null!=vdsIdToMatch && !"".equals(vdsIdToMatch) && !"ADDNEW".equals(vdsIdToMatch) && 
					null!=visStartToMatch && !"".equals(visStartToMatch) && !"ADDNEW".equals(visStartToMatch))
			{
				if(null!=uniqueDataList && uniqueDataList.size()>0)
				{
					for(int a=0;a<uniqueDataList.size();a++)
					{
						VinDetails vDetails = (VinDetails) uniqueDataList.get(a);
						if(null!=vDetails.getGroupCode() && null!=vDetails.getWmiCode() && null!=vDetails.getVdsCode() 
								&& null!=vDetails.getVisStartRange())
						{
							if(null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) && 
								null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
							{
								if(vDetails.getCarlineNameEng().trim().toLowerCase().equals(sessionBean.getCarlineNameEng().trim().toLowerCase()) 
										&& vDetails.getCarlineCode().trim().toLowerCase().equals(sessionBean.getCarlineCode().trim().toLowerCase()) && 
										vDetails.getGroupCode().trim().toLowerCase().equals(groupIdToMatch.trim().toLowerCase()) && 
										vDetails.getWmiCode().trim().toLowerCase().equals(wmiIdToMatch.trim().toLowerCase()) && 
										vDetails.getVdsCode().trim().toLowerCase().equals(vdsIdToMatch.trim().toLowerCase()) && 
										vDetails.getVisStartRange().trim().toLowerCase().equals(visStartToMatch.trim().toLowerCase()))
								{
									/*
									 * CHECK HERE WHETHER ALREADY ADDDED OR NOT
									 */
									boolean add=true;
									if(null!=searchedList && searchedList.size()>0)
									{
										for(int e=0;e<searchedList.size();e++)
										{
											VinDetails v = (VinDetails)searchedList.get(e);
											if(null!=v.getVisEndRange() && null!=vDetails.getVisEndRange())
											{
												if(v.getVisEndRange().trim().toLowerCase().equals(vDetails.getVisEndRange().trim().toLowerCase()))
												{
													// already Exists
													add = false;
													break;
												}
												v = null;
											}
										}
									}
									if(add==true)
									{
										if(null!=vDetails.getVisEndRange() && !"".equals(vDetails.getVisEndRange()))
										{
											if(!"".equals(vDetails.getVisEndRange().trim()))
											{
												searchedList.add(vDetails);
											}
										}
									}
								}
							}
							else
							{
								if(vDetails.getGroupCode().trim().toLowerCase().equals(groupIdToMatch.trim().toLowerCase()) && 
										vDetails.getWmiCode().trim().toLowerCase().equals(wmiIdToMatch.trim().toLowerCase()) && 
										vDetails.getVdsCode().trim().toLowerCase().equals(vdsIdToMatch.trim().toLowerCase()) &&  
										vDetails.getVisStartRange().trim().toLowerCase().equals(visStartToMatch.trim().toLowerCase()))
								{
									/*
									 * CHECK HERE WHETHER ALREADY ADDDED OR NOT
									 */
									boolean add=true;
									if(null!=searchedList && searchedList.size()>0)
									{
										for(int e=0;e<searchedList.size();e++)
										{
											VinDetails v = (VinDetails)searchedList.get(e);
											if(null!=v.getVisEndRange() && null!=vDetails.getVisEndRange())
											{
												if(v.getVisEndRange().trim().toLowerCase().equals(vDetails.getVisEndRange().trim().toLowerCase()))
												{
													// already Exists
													add = false;
													break;
												}
												v = null;
											}
										}
									}
									if(add==true)
									{
										if(null!=vDetails.getVisEndRange() && !"".equals(vDetails.getVisEndRange()))
										{
											if(!"".equals(vDetails.getVisEndRange().trim()))
											{
												searchedList.add(vDetails);
											}
										}
									}
								}
							}
						}
					}
				}
			}
			
			if(operationType.equals("VIEW"))
			{
				if(null!=searchedList && searchedList.size()>0)
				{
					sessionBean.setSearchVISEndList(searchedList);
				}
			}
			else if(operationType.equals("ADDNEW"))
			{
				// ONLY WHEN MODEL IS SELECTED
				if(null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()) && 
						null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
				{
					VinDetails vd = new VinDetails();
					vd.setVisEndRange("ADDNEW");
					sessionBean.getAddVISEndList().add(vd);
					vd = null;
					if(null!=searchedList && searchedList.size()>0)
					{
						sessionBean.getAddVISEndList().addAll(searchedList);
					}
				}
			}
			searchedList = null;
			wmiIdToMatch = null;
			groupIdToMatch= null;
			vdsIdToMatch=  null;
			visStartToMatch= null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "searchVISStartOperation()", e);
		}
	}
	

	private  void saveVinDetails(VinBean sessionBean)
	{
		try
		{
			if(validate(sessionBean.getFieldDetails(), sessionBean))
			{
				/*
				 * call database function
				 * before that set flag as Active
				 */

				// update Selected Country Locale Code & Manual Language Code
				if(null!=sessionBean.getCountryLocaleList() && sessionBean.getCountryLocaleList().size()>0)
				{
					for(int a=0;a<sessionBean.getCountryLocaleList().size();a++)
					{
						CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(a);
						if(String.valueOf(clDetails.getCountryLocaleId()).equals(sessionBean.getCountryLocaleId()))
						{
							// USE DESC FOR IDENTIFING CODE
							sessionBean.getFieldDetails().setCountryLocaleCode(clDetails.getCountryLocaleDesc());
							break;
						}
					}
				}
				if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
				{
					for(int a=0;a<sessionBean.getLanguageList().size();a++)
					{
						ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
						if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
						{
							// USE NAME FOR IDENTIFING CODE
							sessionBean.getFieldDetails().setManualLanguageCode(mlDetails.getManualLanguageName());
							break;
						}
					}
				}
				
				if(null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
				{
					sessionBean.getFieldDetails().setCarlineNameEng(sessionBean.getCarlineNameEng());
				}
//				if(null!=sessionBean.getCarlineNameReg() && !"".equals(sessionBean.getCarlineNameReg()))
//				{
//					sessionBean.getFieldDetails().setCarlineNameReg(sessionBean.getCarlineNameReg());
//				}
				if(null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()))
				{
					sessionBean.getFieldDetails().setCarlineCode(sessionBean.getCarlineCode());
				}
				
				/*
				 * IDENTIFY REGIONAL NAME ON THE BASIS OF 
				 * CARLINE ENG NAME + WMI CODE + CARLINE CODE
				 */
				try
				{
					String languageId = identifyLanguageId(sessionBean);
					sessionBean.setFieldDetails(CarlineDAO.getCarlineRegionalName(sessionBean.getFieldDetails().getCarlineCode(), sessionBean.getFieldDetails().getCarlineNameEng(), sessionBean.getFieldDetails().getWmiCode(), languageId, null, null, sessionBean.getFieldDetails()));
//					sessionBean.getFieldDetails().setCarlineNameReg(CarlineDAO.getCarlineRegionalName(sessionBean.getFieldDetails().getCarlineCode(), sessionBean.getFieldDetails().getCarlineNameEng(), sessionBean.getFieldDetails().getWmiCode(), languageId, null, null));
					languageId = null;
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(Vin.class.getName(), "saveVinDetails()", e);
				}

				sessionBean.getFieldDetails().setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
				sessionBean.getFieldDetails().setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
				sessionBean.getFieldDetails().setFlag(ApplicationProperties.getProperty("flag.value.draft"));
				boolean bool = VinDAO.saveVinDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveVinDetails :: Vin Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.vin")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setVinList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					/*
					 * call getLanguageList
					 */
					getVinList(sessionBean);
				}
				else
				{
					logger.info("saveVinDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.vin")));
				}
			}
			else
			{
				logger.info("saveVinDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "saveVinDetails()", e);
		}
	}

	private  void editVinDetails(HttpServletRequest request, VinBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getVinList() && !"".equals(sessionBean.getVinList().size()>0))
			{
				for(int a=0;a<sessionBean.getVinList().size();a++)
				{
					VinDetails vinDetails = (VinDetails)sessionBean.getVinList().get(a);
					vinDetails.setEditableFlag(false);
				}
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getVinList() && !"".equals(sessionBean.getVinList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getVinList().size();a++)
							{
								VinDetails vinDetails = (VinDetails)sessionBean.getVinList().get(a);
								if(rowId.equals(String.valueOf(vinDetails.getVinId())))
								{
									logger.info("editVinDetails :: Making Row No {"+vinDetails.getSrNo()+"} Editable.");
									vinDetails.setEditableFlag(true);
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
				logger.info("editVinDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "editVinDetails()", e);
		}
	}

	private  void deleteVinDetails(HttpServletRequest request, VinBean sessionBean)
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
					String[] tok = deleteIds.split(",");
					List<VinDetails> deleteIdsList = new ArrayList<VinDetails>();
					VinDetails vinDetails = null;
					if(null!=tok && tok.length>0)
					{
						for(int b=0;b<tok.length;b++)
						{
							if(null!=tok[b] && !"".equals(tok[b]) && !"null".equals(tok[b]))
							{
								if(null!=sessionBean.getVinList() && sessionBean.getVinList().size()>0)
								{
									vinDetails=  null;
									for(int a=0;a<sessionBean.getVinList().size();a++)
									{
										vinDetails=  (VinDetails)sessionBean.getVinList().get(a);
										if(String.valueOf(vinDetails.getVinId()).equals(tok[b]))
										{
											deleteIdsList.add(vinDetails);
											break;
										}
										vinDetails=  null;
									}
								}
							}
						}
					}
					vinDetails=  null;
					deleteIds = null;
					tok = null;
					
					
					boolean bool = VinDAO.deleteVinDetails(deleteIdsList);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.vin")));
						/*
						 * call getVinList
						 */
						getVinList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.vin")));
					}
					deleteIdsList = null;
				}
			}
			else
			{
				logger.info("deleteVinDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "deleteVinDetails()", e);
		}
	}

	private  void activeVinDetails(HttpServletRequest request, VinBean sessionBean)
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
					/*
					 * for each active id - fetch Old status from the carlineList
					 */
					List<Map<String, String>> activeIdsList = new ArrayList<Map<String, String>>();
					String[] tok = activeIds.split(",");
					Map<String,String> dataMap = null;
					VinDetails vinDetails = null;
					if(null!=tok && tok.length>0)
					{
						for(int a=0;a<tok.length;a++)
						{
							if(null!=sessionBean.getVinList() && sessionBean.getVinList().size()>0)
							{
								vinDetails  =null;
								for(int b=0;b<sessionBean.getVinList().size();b++)
								{
									vinDetails=  (VinDetails)sessionBean.getVinList().get(b);
									if(String.valueOf(vinDetails.getVinId()).equals(tok[a]))
									{
										dataMap= new HashMap<String, String>();
										dataMap.put("ID", tok[a]);
										dataMap.put("FLAG", vinDetails.getOldFlag());
										activeIdsList.add(dataMap);
										dataMap=  null;
									}
									vinDetails = null;
								}
							}
						}
					}
					tok = null;
					dataMap=  null;
					
					boolean bool = VinDAO.activeVinDetails(activeIdsList);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("active.success", msgProps.getProperty("label.vin")));
						/*
						 * call getMissionBookList
						 */
						getVinList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.active", msgProps.getProperty("label.vin")));
					}
					activeIds=  null;
					activeIdsList = null;
				}
			}
			else
			{
				logger.info("activeVinDetails :: No Row selected for Active, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.active");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "activeVinDetails()", e);
		}
	}

	private  void updateVinDetails(HttpServletRequest request, VinBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getUpdatedRows() && !"".equals(sessionBean.getUpdatedRows()))
			{
				ArrayList<VinDetails> updateDataList = new ArrayList<VinDetails>();
				String updatedRows=sessionBean.getUpdatedRows();
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getVinList() && sessionBean.getVinList().size()>0)
					{

						for(int i=0;i<sessionBean.getVinList().size();i++)
						{
							VinDetails vinDetails = (VinDetails)sessionBean.getVinList().get(i);
							if(vinDetails.isEditableFlag()==true)
							{
								// set fields empty 
								vinDetails.setWmiCode("");
								vinDetails.setVdsCode("");
								vinDetails.setVisStartRange("");
								vinDetails.setVisEndRange("");
								vinDetails.setGroupCode("");
								vinDetails.setEngineCode("");
								vinDetails.setMissionCode("");
								vinDetails.setFlag("");

								/*
								 * fetch the values from request
								 * and set in vinList
								 */
								String wmiId="VIN_VinList_Wmi_Code"+String.valueOf(vinDetails.getVinId());
								String vdsId="VIN_VinList_Vds_Code"+String.valueOf(vinDetails.getVinId());
								String visStartId="VIN_VinList_Vis_Start_Range"+String.valueOf(vinDetails.getVinId());
								String visEndId="VIN_VinList_Vis_End_Range"+String.valueOf(vinDetails.getVinId());
								String groupId="VIN_VinList_Group_Code"+String.valueOf(vinDetails.getVinId());
								String engineId="VIN_VinList_Engine_Code"+String.valueOf(vinDetails.getVinId());
								String missionId="VIN_VinList_Mission_Code"+String.valueOf(vinDetails.getVinId());
								String flag="VIN_LangList_Flag_"+String.valueOf(vinDetails.getVinId());


								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										if(key.equals(wmiId))
										{
											vinDetails.setWmiCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(vdsId))
										{
											vinDetails.setVdsCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(visStartId))
										{
											vinDetails.setVisStartRange(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										else if(key.equals(visEndId))
										{
											vinDetails.setVisEndRange(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										else if(key.equals(groupId))
										{
											vinDetails.setGroupCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										else if(key.equals(engineId))
										{
											vinDetails.setEngineCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										else if(key.equals(missionId))
										{
											vinDetails.setMissionCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										else if(key.equals(flag))
										{
											vinDetails.setFlag(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										key = null;
										token= null;
									}
								}

								// set all request params ids to null
								wmiId = null;
								vdsId=null;
								visStartId = null;
								visEndId = null;
								groupId = null;
								engineId=null;
								missionId = null;
								flag= null;
							}
						}

						for(int i=0;i<sessionBean.getVinList().size();i++)
						{
							VinDetails vinDetails = (VinDetails)sessionBean.getVinList().get(i);
							if(vinDetails.isEditableFlag()==true)
							{
								/*
								 * add to Update List
								 * Before adding validate data for each Row.
								 * validate vinDetails Object
								 */
								if(validateUpdate(vinDetails, sessionBean))
								{
									/*
									 * add data to updateList
									 */
									updateDataList.add(vinDetails);
								}
								else
								{
									// set updateList to null;
									updateDataList=  null;
									break;
								}
							}
						}

						if(null!=updateDataList && updateDataList.size()>0)
						{
							logger.info("updateVinDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
							/*
							 * Iterate UpdateList and update each row one by one.
							 */
							String errorMessage="";
							String successMessage="";
							String closeConnection="N";
							Connection conn = null;
							try
							{
								conn = DBConnectionHelper.getConnection();
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(Vin.class.getName(), "updateVinDetails()", e);
							}
							
							for(int a=0;a<updateDataList.size();a++)
							{
								VinDetails vinDetails = (VinDetails)updateDataList.get(a);
								boolean updateFlag = VinDAO.updateVinDetails(vinDetails, conn, closeConnection);
								if(updateFlag==true)
								{
									logger.info("updateVinDetails :: Vin Details updated successfully for Row No :: > " + vinDetails.getSrNo());
									successMessage = successMessage+String.valueOf(vinDetails.getSrNo())+",";
								}
								else
								{
									logger.info("updateVinDetails :: Failed to Update Vin Details for Row No :: >  "+ vinDetails.getSrNo());
									errorMessage = errorMessage+String.valueOf(vinDetails.getSrNo())+",";	
								}
								vinDetails=  null;
							}
							closeConnection = null;
							try
							{
								if(null!=conn)
								{
									conn.close();
								}
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(Vin.class.getName(), "updateVinDetails()", e);
							}
							conn = null;

							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.vin"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.vin"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateVinDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setVinList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getVinList
								 */
								getVinList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateVinDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getVinList() && sessionBean.getVinList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										for(int a=0;a<sessionBean.getVinList().size();a++)
										{
											VinDetails vinDetails = (VinDetails)sessionBean.getVinList().get(a);
											vinDetails.setEditableFlag(false);
											for(int b=0;b<tokens.length;b++)
											{
												if(tokens[b].toString().equals(String.valueOf(vinDetails.getSrNo())))
												{
													vinDetails.setEditableFlag(true);
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
			Utilities.printStackTraceToLogs(Vin.class.getName(), "updateVinDetails()", e);
		}
	}

	private  void exportVinDetails(HttpServletRequest request, VinBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<VinDetails> exportDataList = new ArrayList<VinDetails>();
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getVinList() && !"".equals(sessionBean.getVinList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getVinList().size();a++)
							{
								VinDetails vinDetails = (VinDetails)sessionBean.getVinList().get(a);
								if(rowId.equals(String.valueOf(vinDetails.getVinId())))
								{
									// add to export List
									exportDataList.add(vinDetails);
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
					writeVINExcel(exportDataList, sessionBean);
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
					logger.info("exportVinDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportVinDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "exportVinDetails()", e);
		}
	}

	private  void readExcelData(byte[] data, VinBean sessionBean, String extension)
	{
		sessionBean.setVinListToImport(new ArrayList<VinDetails>());
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
				 * MODEL NAME
				 * CARLINE CODE
				 * GROUP CODE
				 * WMI CODE
				 * VDS CODE
				 * VIS START RANGE
				 * VIS END RANGE
				 * ENGINE CODE
				 * MISSION CODE
				 * 
				 */
				while(null!=rowIterator && rowIterator.hasNext())
				{
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						VinDetails vinDetails = new VinDetails();
						Object dataCell = SSTUtils.readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							vinDetails.setCarlineNameEng(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							vinDetails.setCarlineCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						dataCell = SSTUtils.readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							vinDetails.setGroupCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(3));
						if(null!=dataCell && !"".equals(dataCell))
						{
							vinDetails.setWmiCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(4));
						if(null!=dataCell && !"".equals(dataCell))
						{
							vinDetails.setVdsCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell =SSTUtils. readCellValue(row.getCell(5));
						if(null!=dataCell && !"".equals(dataCell))
						{
							vinDetails.setVisStartRange(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(6));
						if(null!=dataCell && !"".equals(dataCell))
						{
							vinDetails.setVisEndRange(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(7));
						if(null!=dataCell && !"".equals(dataCell))
						{
							vinDetails.setEngineCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = SSTUtils.readCellValue(row.getCell(8));
						if(null!=dataCell && !"".equals(dataCell))
						{
							vinDetails.setMissionCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * NEW ACTION / MARKER COLUMN (last column of the import template).
						 * A/ADD (create), U/UPDATE (update), D/DELETE (soft delete).
						 * Blank or any non-delete value falls through to the existing
						 * create-or-update path, so an OLD template still imports.
						 */
						dataCell = SSTUtils.readCellValue(row.getCell(9));
						if(null!=dataCell && !"".equals(dataCell))
						{
							vinDetails.setImportAction(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * add details to vinList for import
						 */
						if(null==sessionBean.getVinListToImport() || sessionBean.getVinListToImport().size()<=0)
						{
							sessionBean.setVinListToImport(new ArrayList<VinDetails>());
						}

						sessionBean.getVinListToImport().add(vinDetails);
						vinDetails= null;
					}
					// INCREMENT ROW COUNT BY 1
					rowCount++;
					row = null;
				}
				sheet = null;
				workbook = null;
				xlsSheet = null;
				xlsWorkBook = null;
				rowIterator=  null;
				is.close();
				is = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "readExcelData()", e);
		}
	}

	private  void writeVINExcel(ArrayList<VinDetails> vinList, VinBean sessionBean)
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
				String modelName="";
				String carlineCode=null;
				if(null!=sessionBean.getCarlineId() && !"".equals(sessionBean.getCarlineId()) && !"ALL".equals(sessionBean.getCarlineId()))
				{
					if(null!=sessionBean.getCarlineNameEng() && !"".equals(sessionBean.getCarlineNameEng()))
					{
						modelName = sessionBean.getCarlineNameEng();
					}
					if(null!=sessionBean.getCarlineCode() && !"".equals(sessionBean.getCarlineCode()))
					{
						carlineCode=  sessionBean.getCarlineCode();
					}
					
					if(null!=modelName && !"".equals(modelName))
					{
						if(null!=name && !"".equals(name))
						{
							name = name.trim()+"_";
						}
						name = name.trim()+modelName.trim();
					}
					
					if(null!=carlineCode && !"".equals(carlineCode))
					{
						if(null!=name && !"".equals(name))
						{
							name = name.trim()+"_";
						}
						name = name.trim()+carlineCode.trim();
					}
				}
				else
				{
					// ADD - ALL TO NAME
					if(null!=name && !"".equals(name))
					{
						name = name.trim()+"_";
					}
					name = name.trim()+"ALL";
				}
				modelName = null;
				carlineCode = null;

				if(null!=name && !"".equals(name))
				{
					name = name.trim()+"_";
				}
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_VIN_NAME");
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

				Cell modelNameCell = headerRow.createCell(0);
				modelNameCell.setCellValue("MODEL");
				Cell carlineCodeCell = headerRow.createCell(1);
				carlineCodeCell.setCellValue("CARLINE CODE");
				Cell groupCodeCell = headerRow.createCell(2);
				groupCodeCell.setCellValue("GROUP");
				Cell wmiCodeCell = headerRow.createCell(3);
				wmiCodeCell.setCellValue("WMI");
				Cell vdsCell = headerRow.createCell(4);
				vdsCell.setCellValue("VDS");
				Cell visStartCell = headerRow.createCell(5);
				visStartCell.setCellValue("VIS START");
				Cell visEndCell = headerRow.createCell(6);
				visEndCell.setCellValue("VIS END");
				Cell engCell = headerRow.createCell(7);
				engCell.setCellValue("ENGINE CODE");
				Cell missionCell = headerRow.createCell(8);
				missionCell.setCellValue("TRANSMISSION_CODE");

				int rowCount=0;
				if(null!=vinList && vinList.size()>0)
				{
					for(int i=0;i<vinList.size();i++)
					{
						VinDetails vinDetails = (VinDetails)vinList.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);

						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						Cell cell3 = row.createCell(3);
						Cell cell4 = row.createCell(4);
						Cell cell5 = row.createCell(5);
						Cell cell6 = row.createCell(6);
						Cell cell7 = row.createCell(7);
						Cell cell8 = row.createCell(8);

						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						cell3.setCellValue("");
						cell4.setCellValue("");
						cell5.setCellValue("");
						cell6.setCellValue("");
						cell7.setCellValue("");
						cell8.setCellValue("");

						
						if(null!=vinDetails.getCarlineNameEng() && !"".equals(vinDetails.getCarlineNameEng()))
						{
							cell0.setCellValue(vinDetails.getCarlineNameEng().trim());
						}
						if(null!=vinDetails.getCarlineCode() && !"".equals(vinDetails.getCarlineCode()))
						{
							cell1.setCellValue(vinDetails.getCarlineCode().trim());
						}
						if(null!=vinDetails.getGroupCode() && !"".equals(vinDetails.getGroupCode()))
						{
							cell2.setCellValue(vinDetails.getGroupCode().trim());
						}
						if(null!=vinDetails.getWmiCode() && !"".equals(vinDetails.getWmiCode()))
						{
							cell3.setCellValue(vinDetails.getWmiCode().trim());
						}
						if(null!=vinDetails.getVdsCode() && !"".equals(vinDetails.getVdsCode()))
						{
							cell4.setCellValue(vinDetails.getVdsCode().trim());
						}
						if(null!=vinDetails.getVisStartRange() && !"".equals(vinDetails.getVisStartRange()))
						{
							cell5.setCellValue(vinDetails.getVisStartRange().trim());
						}
						if(null!=vinDetails.getVisEndRange() && !"".equals(vinDetails.getVisEndRange()))
						{
							cell6.setCellValue(vinDetails.getVisEndRange().trim());
						}
						if(null!=vinDetails.getEngineCode() && !"".equals(vinDetails.getEngineCode()))
						{
							cell7.setCellValue(vinDetails.getEngineCode().trim());
						}
						if(null!=vinDetails.getMissionCode() && !"".equals(vinDetails.getMissionCode()))
						{
							cell8.setCellValue(vinDetails.getMissionCode().trim());
						}

						cell0 = null;
						cell1 = null;
						cell2 = null;
						cell3 = null;
						cell4 = null;
						cell5 = null;
						cell6 = null;
						cell7 = null;
						cell8 = null;
						row = null;
						vinDetails = null;
					}

					headerRow =  null;
					groupCodeCell = null;
					wmiCodeCell = null;
					vdsCell = null;
					visEndCell = null;
					visStartCell=  null;
					engCell = null;
					missionCell = null;
					modelNameCell= null;
					carlineCodeCell = null;
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
			Utilities.printStackTraceToLogs(Vin.class.getName(), "writeVINExcel()", e);
		}
	}


	private  void executeExcelOperation(VinBean sessionBean, byte[] data, String extension)
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
				ArrayList<VinDetails> listToSave = new ArrayList<VinDetails>();
				// ACTION=D rows are collected separately - they are a DELETE, not a save.
				ArrayList<VinDetails> listToDelete = new ArrayList<VinDetails>();
				for(int i=0;i<sessionBean.getVinListToImport().size();i++)
				{
					VinDetails fieldDetails = (VinDetails)sessionBean.getVinListToImport().get(i);
					fieldDetails.setSrNo((i+1+1));

					/*
					 * ACTION = D -> this row is a delete. Keep it out of listToSave so the
					 * existing create-or-update logic stays exactly as it was.
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
							VinDetails existingDetails = (VinDetails)listToSave.get(j);
							if(fieldDetails.getCarlineNameEng().trim().toLowerCase().equals(existingDetails.getCarlineNameEng().trim().toLowerCase()) && 
								fieldDetails.getCarlineCode().trim().toLowerCase().equals(existingDetails.getCarlineCode().trim().toLowerCase()) && 
								fieldDetails.getWmiCode().trim().toLowerCase().equals(existingDetails.getWmiCode().trim().toLowerCase()) 
								&& fieldDetails.getVdsCode().trim().toLowerCase().equals(existingDetails.getVdsCode().trim().toLowerCase())
								&& fieldDetails.getVisStartRange().trim().toLowerCase().equals(existingDetails.getVisStartRange().trim().toLowerCase())
								&& fieldDetails.getVisEndRange().trim().toLowerCase().equals(existingDetails.getVisEndRange().trim().toLowerCase())
								&& fieldDetails.getGroupCode().trim().toLowerCase().equals(existingDetails.getGroupCode().trim().toLowerCase())
								&& fieldDetails.getEngineCode().trim().toLowerCase().equals(existingDetails.getEngineCode().trim().toLowerCase())
								&& fieldDetails.getMissionCode().trim().toLowerCase().equals(existingDetails.getMissionCode().trim().toLowerCase()))
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
					/*
					 * ITERATE AND START SAVING EACH ROW
					 */
					String countryLocaleCode="";
					String languageCode="";
					// update Selected Country Locale Code & Manual Language Code
					if(null!=sessionBean.getCountryLocaleList() && sessionBean.getCountryLocaleList().size()>0)
					{
						for(int a=0;a<sessionBean.getCountryLocaleList().size();a++)
						{
							CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(a);
							if(String.valueOf(clDetails.getCountryLocaleId()).equals(sessionBean.getCountryLocaleId()))
							{
								// USE DESC FOR IDENTIFING CODE
								countryLocaleCode=clDetails.getCountryLocaleDesc();
								break;
							}
						}
					}
					if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
					{
						for(int a=0;a<sessionBean.getLanguageList().size();a++)
						{
							ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
							if(String.valueOf(mlDetails.getManualLanguageId()).equals(sessionBean.getManualLanguageId()))
							{
								// USE NAME FOR IDENTIFING CODE
								languageCode=mlDetails.getManualLanguageName();
								break;
							}
						}
					}

					
					Connection conn = null;
					String connClosed="N";
					try
					{
						conn = DBConnectionHelper.getConnection();
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(Vin.class.getName(), "executeExcelOperation()", e);
					}
					
					String successLineNo="";
					String errorLineNo="";
					int successCount=0;
					
					for(int i=0;i<listToSave.size();i++)
					{
						VinDetails fieldDetails = (VinDetails)listToSave.get(i);

						fieldDetails.setCountryLocaleCode(countryLocaleCode);
						fieldDetails.setManualLanguageCode(languageCode);
						fieldDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
						fieldDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
						fieldDetails.setFlag(ApplicationProperties.getProperty("flag.value.draft"));
						try
						{
							/*
							 *  IDENTIFY REGIONAL NAME ON THE BASIS OF 
							 *  CARLINE ENG NAME + CARLINE CODE + WMI CODE
							 */
							String languageId = identifyLanguageId(sessionBean);
							fieldDetails = CarlineDAO.getCarlineRegionalName(fieldDetails.getCarlineCode(), fieldDetails.getCarlineNameEng(), fieldDetails.getWmiCode(), languageId, conn,connClosed, fieldDetails);
//							fieldDetails.setCarlineNameReg(CarlineDAO.getCarlineRegionalName(fieldDetails.getCarlineCode(), fieldDetails.getCarlineNameEng(), fieldDetails.getWmiCode(), languageId, conn,connClosed));
							languageId = null;
							
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(Vin.class.getName(), "executeExcelOperation()", e);
						}
						
						boolean saveVinData = VinDAO.importVINDetails(fieldDetails, conn, connClosed);
						
						if(saveVinData == true)
						{
							logger.info("readParamsFromRequest() :: VIN Data for Line No {"+(i+1+1)+"}. Saved Successfully.");
							if(null!=successLineNo && !"".equals(successLineNo))
							{
								successLineNo = successLineNo+",";
							}
							successLineNo = successLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						else
						{
							logger.info("readParamsFromRequest() :: Failed to Import VIN Data for Line No {"+(i+1+1)+"}.");
							if(null!=errorLineNo && !"".equals(errorLineNo))
							{
								errorLineNo = errorLineNo+",";
							}
							errorLineNo = errorLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						fieldDetails= null;
					}

					countryLocaleCode = null;
					languageCode= null;
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
						Utilities.printStackTraceToLogs(Vin.class.getName(), "executeExcelOperation()", e);
					}
					conn = null;

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
						//						sessionBean.setSuccessMessage(msgProps.addMessage("import.success",msgProps.getProperty("label.vin"), successLineNo ));
						sessionBean.setSelectedRows(null);
						sessionBean.setShowUpdate(false);
						/*
						 * call function to load updated vin list
						 */
						getVinList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import", msgProps.getProperty("label.vin"), String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
						//						errorLineNo= "( "+errorLineNo+ " )";
						//						sessionBean.setErrorMessage(msgProps.addMessage("error.import", msgProps.getProperty("label.vin"), errorLineNo));
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
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.vin")));
				}

				/*
				 * ACTION = D ROWS - SOFT DELETE.
				 *
				 * Each row is located with the SAME unique combination the create-or-update path
				 * uses, the resolved primary key is set back on the VO, and the VOs are handed to
				 * the screen's EXISTING deleteVinDetails() - which also clears the related
				 * gms3_mdm_vin_ml_mapping rows. Reusing it keeps both tables in step.
				 */
				if(null!=listToDelete && listToDelete.size()>0)
				{
					String deleteSuccessLineNo="";
					String deleteErrorLineNo="";
					ArrayList<VinDetails> resolvedDeleteList = new ArrayList<VinDetails>();
					Connection deleteConn = null;
					try
					{
						deleteConn = DBConnectionHelper.getConnection();
						for(int i=0;i<listToDelete.size();i++)
						{
							VinDetails deleteDetails = (VinDetails)listToDelete.get(i);
							deleteDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
							deleteDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
							long existingId = 0;
							try
							{
								existingId = VinDAO.findExistingIdForImport(deleteDetails, deleteConn);
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(Vin.class.getName(), "executeExcelOperation()", e);
							}
							if(existingId>0)
							{
								deleteDetails.setVinId(new Long(existingId));
								resolvedDeleteList.add(deleteDetails);
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
						Utilities.printStackTraceToLogs(Vin.class.getName(), "executeExcelOperation()", e);
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
							Utilities.printStackTraceToLogs(Vin.class.getName(), "executeExcelOperation()", e);
						}
						deleteConn = null;
					}
					int deleteSuccessCount=0;
					if(null!=resolvedDeleteList && resolvedDeleteList.size()>0)
					{
						boolean deleted = false;
						try
						{
							deleted = VinDAO.deleteVinDetails(resolvedDeleteList);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(Vin.class.getName(), "executeExcelOperation()", e);
						}
						if(deleted==true)
						{
							deleteSuccessCount = resolvedDeleteList.size();
							getVinList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import.delete", msgProps.getProperty("label.vin"), String.valueOf(deleteErrorRows[a])));
							}
						}
						deleteErrorRows = null;
					}
					deleteSuccessLineNo = null;
					deleteErrorLineNo = null;
					resolvedDeleteList = null;
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
			Utilities.printStackTraceToLogs(Vin.class.getName(), "executeExcelOperation()", e);
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
	
	private void performAccessCheck(VinBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(Vin.class.getName(), "performAccessCheck()", e);
		}
	}
	
	private String identifyLanguageId(VinBean sessionBean)
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
//						Utilities.printStackTraceToLogs(Vin.class.getName(), "identifyLanguageId()", e);
//					}
//				}
//				encaLocale = null;
//				enusLocale = null;
//			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Vin.class.getName(), "identifyLanguageId()", e);
		}
		return languageId;
	}

}