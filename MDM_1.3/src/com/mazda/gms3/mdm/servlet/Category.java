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
import org.apache.poi.ss.format.CellNumberFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mazda.gms3.mdm.bean.CategoryBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CategoryDAO;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.CountryLocaleComparator;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.ManualLanguageComparator;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CategoryDetails;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class Category
 */
public class Category extends HttpServlet {
	private static final long serialVersionUID = 1L;

	Logger logger = LogManager.getLogger(Category.class);

	MessageProperties msgProps= null;
	String wslId=null;
	String reportName=null;
	ArrayList<CategoryDetails> uniqueLevel1List = new ArrayList<CategoryDetails>();
	ArrayList<CategoryDetails> uniqueLevel2List = new ArrayList<CategoryDetails>();
	String moduleRefKey=AccessManagementInterface.REF_KEY_ESI_CATEGORY;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Category() {
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
			CategoryBean sessionBean = getSessionBean(request);
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
			sessionBean.setCategoryList(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setInfoMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);

			sessionBean.setReportViewPath(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setCategoryListToImport(null);

			sessionBean.setAllCategoryDataList(null);
			sessionBean.setShowButtons(false);
			sessionBean.setShowView(false);
			sessionBean.setShowAdd(false);

			clearSearchBlock(sessionBean);
			clearAddNewBlock(sessionBean);


			/*
			 * call function to load all the Country Locale Data
			 */
			getCountryLocaleList(sessionBean, request);

			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);

			/*
			 * 
			 * TO DO
			 * 
			 */
			// EXPLICITY SET sessionBean.setShowButtons(true);
			sessionBean.setShowButtons(true);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Category.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/category.jsp");
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
			CategoryBean sessionBean = getSessionBean(request);
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
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					sessionBean.setManualLanguageId(null);
					// clear search block
					clearSearchBlock(sessionBean);
					// clear add block
					clearAddNewBlock(sessionBean);

					/*
					 * call function to load all the Manual language data
					 */
					getLanguageList(sessionBean, request);
					/*
					 * call function to load categoryDetails
					 */
					getCategoryList(sessionBean);

				}
				else if(sessionBean.getActionClicked().equals("LANG_SELECTION"))
				{
					sessionBean.setShowUpdate(false);
					sessionBean.setSelectedRows(null);
					// clear search block
					clearSearchBlock(sessionBean);
					// clear add block
					clearAddNewBlock(sessionBean);

					/*
					 * call function to load categoryDetails
					 */
					getCategoryList(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("SAVE"))
				{
					/*
					 * Save Operation called
					 */
					saveCategoryDetails(sessionBean);
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("EDIT"))
				{
					/*
					 * Edit Operation called
					 */
					editCategoryDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("DELETE"))
				{
					/*
					 * Delete Operation called
					 */
					deleteCategoryDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE"))
				{
					/*
					 * Update Operation called
					 */
					updateCategoryDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
					sessionBean.setUpdatedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("ACTIVE"))
				{
					/*
					 * Active Operation called
					 */
					activeCategoryDetails(request, sessionBean);	
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("RESET"))
				{
					// reset some fields
					sessionBean.setCategoryList(null);
					sessionBean.setFlagList(null);
					sessionBean.setErrorMessage(null);
					sessionBean.setSuccessMessage(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setDisplayPageLength(null);
					sessionBean.setDisplayPageNo(null);
					clearAddNewBlock(sessionBean);
					clearSearchBlock(sessionBean);
					/*
					 * call getCategoryList
					 */
					getCategoryList(sessionBean);	
				}
				else if(sessionBean.getActionClicked().equals("EXPORT_CATEGORY"))
				{
					/*
					 * Export VIN Operation
					 */
					exportExportDetails(request, sessionBean);
					sessionBean.setSelectedRows(null);
				}
				else if(sessionBean.getActionClicked().equals("SEARCH_CATEGORY"))
				{
					/*
					 * Search Category Operation
					 */
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setUpdatedRows(null);

					searchCategory(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("CLEAR_SEARCH"))
				{
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setUpdatedRows(null);

					clearSearchBlock(sessionBean);
					clearAddNewBlock(sessionBean);
					/*
					 * getCategoryList Operation
					 */
					getCategoryList(sessionBean);
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
						 * call getCategoryList
						 */
						getCategoryList(sessionBean);
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
						 * call getCategoryList
						 */
						getCategoryList(sessionBean);
					}
				}
				else if(sessionBean.getActionClicked().equals("SEARCH_LEVEL1"))
				{
					/*
					 * call search operation
					 */
					searchLevel1Operation(sessionBean, "VIEW");
				}
				else if(sessionBean.getActionClicked().equals("ADD_LEVEL1"))
				{
					/*
					 * call search operation
					 */
					searchLevel1Operation(sessionBean, "ADDNEW");	
				}
			}

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Category.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/category.jsp");
				rs.forward(request, response);
			}
		}
	}

	private CategoryBean getSessionBean(HttpServletRequest request) 
	{
		CategoryBean sessionBean = null;
		if (null != request.getSession().getAttribute("categoryBean") && !"".equals(request.getSession().getAttribute("categoryBean"))) 
		{
			sessionBean = (CategoryBean) request.getSession().getAttribute("categoryBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new CategoryBean();
			request.getSession().setAttribute("categoryBean", sessionBean);
		}
		return sessionBean;
	}

	private  void clearSearchBlock(CategoryBean sessionBean)
	{
		sessionBean.setSearchCatLevel1Code(null);
		sessionBean.setSearchCatLevel2Code(null);
		sessionBean.setSearchCatLevel1List(null);
		sessionBean.setSearchCatLevel2List(null);
	}

	private  void clearAddNewBlock(CategoryBean sessionBean)
	{
		sessionBean.setAddCatLevel1Code(null);
		sessionBean.setAddCatLevel2Code(null);

		sessionBean.setNewCatLevel1Code(null);
		sessionBean.setNewCatLevel1Name(null);
		sessionBean.setNewCatLevel2Code(null);
		sessionBean.setNewCatLevel2Name(null);

		sessionBean.setAddCatLevel1List(null);
		sessionBean.setAddCatLevel2List(null);
	}

	private  void searchCategory(CategoryBean sessionBean)
	{
		try
		{
			sessionBean.setCategoryList(new ArrayList<CategoryDetails>());

			/*
			 * SEARCH FILTERS - 
			 * CAT LEVEL 1 + CAT LEVEL 2
			 * CAT LEVEL 2
			 * CAT LEVEL 2
			 */
			if(null!=sessionBean.getSearchCatLevel1Code() && !"".equals(sessionBean.getSearchCatLevel1Code()) 
					&& null!=sessionBean.getSearchCatLevel2Code() && !"".equals(sessionBean.getSearchCatLevel2Code())  )
			{
				// CAT LEVEL 1 + CAT LEVEL 2
				if(null!=sessionBean.getAllCategoryDataList() && sessionBean.getAllCategoryDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllCategoryDataList().size();a++)
					{
						CategoryDetails details = (CategoryDetails)sessionBean.getAllCategoryDataList().get(a);
						if(null!=details.getCategoryCode() 
								&& null!=details.getSubCategoryCode())
						{
							if(sessionBean.getSearchCatLevel1Code().trim().toLowerCase().equals(details.getCategoryCode().trim().toLowerCase()) 
									&& sessionBean.getSearchCatLevel2Code().trim().toLowerCase().equals(details.getSubCategoryCode().trim().toLowerCase()))
							{
								sessionBean.getCategoryList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchCatLevel1Code() && !"".equals(sessionBean.getSearchCatLevel1Code())) 
			{
				// CAT LEVEL 1 
				if(null!=sessionBean.getAllCategoryDataList() && sessionBean.getAllCategoryDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllCategoryDataList().size();a++)
					{
						CategoryDetails details = (CategoryDetails)sessionBean.getAllCategoryDataList().get(a);
						if(null!=details.getCategoryCode() )
						{
							if(sessionBean.getSearchCatLevel1Code().trim().toLowerCase().equals(details.getCategoryCode().trim().toLowerCase()) )
							{
								sessionBean.getCategoryList().add(details);
							}
						}
					}
				}
			}
			else if(null!=sessionBean.getSearchCatLevel2Code() && !"".equals(sessionBean.getSearchCatLevel2Code())  )
			{
				//  CAT LEVEL 2
				if(null!=sessionBean.getAllCategoryDataList() && sessionBean.getAllCategoryDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllCategoryDataList().size();a++)
					{
						CategoryDetails details = (CategoryDetails)sessionBean.getAllCategoryDataList().get(a);
						if(null!=details.getSubCategoryCode())
						{
							if(sessionBean.getSearchCatLevel2Code().trim().toLowerCase().equals(details.getSubCategoryCode().trim().toLowerCase()))
							{
								sessionBean.getCategoryList().add(details);
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Category.class.getName(), "searchCategory()", e);
		}
	}

	private void searchLevel1Operation(CategoryBean sessionBean, String operationType)
	{
		try
		{
			String level1IdToMatch="";
			if(operationType=="VIEW")
			{
				// set LEVEL 2 CODE AND LIST TO NULL
				sessionBean.setSearchCatLevel2Code(null);
				sessionBean.setSearchCatLevel2List(new ArrayList<CategoryDetails>());

				level1IdToMatch = sessionBean.getSearchCatLevel1Code();
			}
			else if(operationType.equals("ADDNEW"))
			{
				sessionBean.setAddCatLevel2Code(null);

				// set New Fields as Null
				sessionBean.setNewCatLevel2Code(null);
				sessionBean.setNewCatLevel2Name(null);
				sessionBean.setAddCatLevel2List(new ArrayList<CategoryDetails>());

				level1IdToMatch = sessionBean.getAddCatLevel1Code();
			}


			ArrayList<CategoryDetails> searchedList = new ArrayList<CategoryDetails>();
			/*
			 * FILTER WMI LIST ON THE BASIS OF SELECTED GROUP CODE
			 */
			if(null!=level1IdToMatch && !"".equals(level1IdToMatch) && !"ADDNEW".equals(level1IdToMatch))
			{
				if(null!=uniqueLevel2List && uniqueLevel2List.size()>0)
				{
					for(int a=0;a<uniqueLevel2List.size();a++)
					{
						CategoryDetails cDetails = (CategoryDetails) uniqueLevel2List.get(a);
						if(null!=cDetails.getCategoryCode())
						{
							if(cDetails.getCategoryCode().trim().toLowerCase().equals(level1IdToMatch.trim().toLowerCase()))
							{
								/*
								 * CHECK HERE WHETHER ALREADY ADDDED OR NOT
								 */
								boolean add=true;
								if(null!=searchedList && searchedList.size()>0)
								{
									for(int e=0;e<searchedList.size();e++)
									{
										CategoryDetails v = (CategoryDetails)searchedList.get(e);
										if(null!=v.getSubCategoryCode() && null!=cDetails.getSubCategoryCode())
										{
											if(v.getSubCategoryCode().trim().toLowerCase().equals(cDetails.getSubCategoryCode().trim().toLowerCase()))
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
									if(null!=cDetails.getSubCategoryCode() && !"".equals(cDetails.getSubCategoryCode()) 
											&& null!=cDetails.getSubCategoryNameEng() && !"".equals(cDetails.getSubCategoryNameEng()))
									{
										if(!"".equals(cDetails.getSubCategoryCode().trim()) && !"".equals(cDetails.getSubCategoryNameEng().trim()))
										{
											searchedList.add(cDetails);
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
					sessionBean.setSearchCatLevel2List(searchedList);
				}
			}
			else if(operationType.equals("ADDNEW"))
			{
				CategoryDetails cd = new CategoryDetails();
				cd.setSubCategoryCode("ADDNEW");
				cd.setSubCategoryNameEng("ADDNEW");
				sessionBean.getAddCatLevel2List().add(cd);
				cd = null;
				if(null!=searchedList && searchedList.size()>0)
				{
					sessionBean.getAddCatLevel2List().addAll(searchedList);
				}
			}
			searchedList = null;
			level1IdToMatch = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Category.class.getName(), "searchLevel1Operation()", e);
		}
	}

	private  void getCountryLocaleList_OLD(CategoryBean sessionBean, HttpServletRequest request)
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
					sessionBean.setCountryLocaleList(list);
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
			Utilities.printStackTraceToLogs(Category.class.getName(), "getCountryLocaleList()", e);
		}
	}

	private  void getLanguageList_OLD(CategoryBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(Category.class.getName(), "getLanguageList()", e);
		}
	}

	private  void getCountryLocaleList(CategoryBean sessionBean, HttpServletRequest request)
	{
		try
		{
			UserAccessBean userSessionBean = getUserSessionBean(request);
			sessionBean.setCountryLocaleList(new ArrayList<CountryLocaleDetails>());
			ArrayList<CountryLocaleDetails> tempList = new ArrayList<CountryLocaleDetails>();
			tempList = CountryLocaleDAO.getCountryLocaleDetailsListForCombo();
			ArrayList<CountryLocaleDetails> list = new ArrayList<CountryLocaleDetails>();
			if(null!=tempList && tempList.size()>0)
			{
				/*
				 * ITERATE LIST AND ALLOW ONLY WRITABLE COUNTRIES
				 */
				String nonWritableMMECountries=ApplicationProperties.getProperty("writable.esi.countries");
				String[] tokens = nonWritableMMECountries.split(",");
				if(null!=tokens && tokens.length>0)
				{
					for(int a=0;a<tokens.length;a++)
					{
						String ccCode = tokens[a];
						if(null!=tempList && tempList.size()>0)
						{
							for(int b=0;b<tempList.size();b++)
							{
								CountryLocaleDetails clDetails = (CountryLocaleDetails)tempList.get(b);
								if(clDetails.getCountryLocaleDesc().trim().toLowerCase().equals(ccCode.trim().toLowerCase()))
								{
									list.add(clDetails);
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
			tempList=  null;
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
			Utilities.printStackTraceToLogs(Category.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private  void getLanguageList(CategoryBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setLanguageList(new ArrayList<ManualLanguageDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()))
			{
				ArrayList<ManualLanguageDetails> tempList = new ArrayList<ManualLanguageDetails>();
				tempList = ManualLanguageDAO.getManualLanguageDetailsListForCombo(sessionBean.getCountryLocaleId());
				ArrayList<ManualLanguageDetails> list = new ArrayList<ManualLanguageDetails>();
				if(null!=tempList && tempList.size()>0)
				{
					/*
					 * ITERATE LIST AND ALLOW WRITABLE LANGUAGES
					 */
					String nonWritableMMELocales=ApplicationProperties.getProperty("writable.esi.locales");
					nonWritableMMELocales = nonWritableMMELocales.replace("_", "-");
					String[] tokens = nonWritableMMELocales.split(",");
					if(null!=tokens && tokens.length>0)
					{
						for(int a=0;a<tokens.length;a++)
						{
							String lcCode = tokens[a];
							if(null!=tempList && tempList.size()>0)
							{
								for(int b=0;b<tempList.size();b++)
								{
									ManualLanguageDetails mlDetails = (ManualLanguageDetails)tempList.get(b);
									if(mlDetails.getManualLanguageName().trim().toLowerCase().equals(lcCode.trim().toLowerCase()))
									{
										list.add(mlDetails);
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
				tempList=  null;
				
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
							
							// PROCEED FOR IDENTIFYING LANGUAGES FOR COMBO
							for(String contentLocale : userSessionBean.getUserLocalesList())
							{
								if(null!=contentLocale && !"".equals(contentLocale))
								{
									for(ManualLanguageDetails mldDetails : list)
									{
										boolean addToList = false;
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
			Utilities.printStackTraceToLogs(Category.class.getName(), "getLanguageList()", e);
		}
	}
	
	private  void getFlagList(CategoryBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());

		sessionBean.setFlagList(Utilities.prepareFlagsList(msgProps));
	}

	private  void getCategoryList(CategoryBean sessionBean)
	{
		try
		{
			sessionBean.setAllCategoryDataList(new ArrayList<CategoryDetails>());
			sessionBean.setCategoryList(new ArrayList<CategoryDetails>());

			clearSearchBlock(sessionBean);
			clearAddNewBlock(sessionBean);

			sessionBean.setSearchCatLevel1List(new ArrayList<CategoryDetails>());
			sessionBean.setSearchCatLevel2List(new ArrayList<CategoryDetails>());

			sessionBean.setAddCatLevel1List(new ArrayList<CategoryDetails>());
			sessionBean.setAddCatLevel2List(new ArrayList<CategoryDetails>());

			uniqueLevel1List = new ArrayList<CategoryDetails>();
			uniqueLevel2List = new ArrayList<CategoryDetails>();

			if(null!=sessionBean.getManualLanguageId() && !"".equals(sessionBean.getManualLanguageId()))
			{
				ArrayList<CategoryDetails> list = new ArrayList<CategoryDetails>();
				list = CategoryDAO.getCategoryDetailsList(String.valueOf(sessionBean.getManualLanguageId()));
				if(null!=list && list.size()>0)
				{
					for(int i=0;i<list.size();i++)
					{
						CategoryDetails cDetails  = (CategoryDetails)list.get(i);
						if(null!=cDetails.getFlag() && !"".equals(cDetails.getFlag()))
						{
							// set Label
							if(cDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
							{
								cDetails.setFlagLabel(msgProps.getProperty("flag.label.active"));
							}
							else if(cDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.sleep")))
							{
								cDetails.setFlagLabel(msgProps.getProperty("flag.label.sleep"));
							}
							else if(cDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
							{
								cDetails.setFlagLabel(msgProps.getProperty("flag.label.draft"));
							}
							else if(cDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
							{
								cDetails.setFlagLabel(msgProps.getProperty("flag.label.deprecated"));
							}
							else if(cDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.delete")))
							{
								cDetails.setFlagLabel(msgProps.getProperty("flag.label.delete"));
							}
						}
						cDetails = null;
					}
					sessionBean.setAllCategoryDataList(list);
					sessionBean.setCategoryList(list);
				}

				/*
				 * PREPARE - 
				 * 	 CAT LEVEL 1 & LEVEL 2 SEARCH / ADD NEW LIST
				 */
				if(null!=sessionBean.getAllCategoryDataList() && sessionBean.getAllCategoryDataList().size()>0)
				{
					for(int a=0;a<sessionBean.getAllCategoryDataList().size();a++)
					{
						CategoryDetails cDetails = (CategoryDetails)sessionBean.getAllCategoryDataList().get(a);
						boolean addLevel1 = true;

						if(null!=uniqueLevel1List && uniqueLevel1List.size()>0)
						{
							for(int r=0;r<uniqueLevel1List.size();r++)
							{
								CategoryDetails level1Details = (CategoryDetails)uniqueLevel1List.get(r);
								if(null!=level1Details.getCategoryCode() && !"".equals(level1Details.getCategoryCode()) 
										&& null!=cDetails.getCategoryCode() && !"".equals(cDetails.getCategoryCode()))
								{
									if(level1Details.getCategoryCode().trim().toLowerCase().
											equals(cDetails.getCategoryCode().trim().toLowerCase()))
									{
										addLevel1 = false;
										// exists
										break;
									}
								}
							}
						}

						if(addLevel1 == true)
						{
							if(null!=cDetails.getCategoryCode() && !"".equals(cDetails.getCategoryCode()) && 
									null!=cDetails.getCategoryNameEng() && !"".equals(cDetails.getCategoryNameEng()))
							{
								uniqueLevel1List.add(cDetails);
							}
						}
					}
				}

				list = null;

				CategoryDetails c = new CategoryDetails();
				c.setCategoryCode("ADDNEW");
				c.setCategoryNameEng("ADDNEW");
				sessionBean.getAddCatLevel1List().add(c);
				if(null!=uniqueLevel1List)
				{
					sessionBean.setSearchCatLevel1List(uniqueLevel1List);
					sessionBean.getAddCatLevel1List().addAll(uniqueLevel1List);
				}
				c= null;
				if(null!=sessionBean.getAllCategoryDataList() && sessionBean.getAllCategoryDataList().size()>0)
				{
					uniqueLevel2List = sessionBean.getAllCategoryDataList();
				}
				// DO NOT EMPTY UNIQUE LIST AS THEY WILL BE USED ON FILTERS
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Category.class.getName(), "getCategoryList()", e);
		}
	}


	private  void readParamsFromRequest(CategoryBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new CategoryDetails());
			sessionBean.setManualLanguageId(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setActionClicked(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setCountryLocaleId(null);
			sessionBean.setManualLanguageId(null);
			sessionBean.setCategoryListToImport(null);

			sessionBean.setSearchCatLevel1Code(null);
			sessionBean.setSearchCatLevel2Code(null);
			sessionBean.setAddCatLevel1Code(null);
			sessionBean.setAddCatLevel2Code(null);

			sessionBean.setNewCatLevel1Code(null);
			sessionBean.setNewCatLevel1Name(null);
			sessionBean.setNewCatLevel2Code(null);
			sessionBean.setNewCatLevel2Name(null);

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
						if (fieldName.equals("CAT_UpdatedRows")) 
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


						if(fieldName.equals("CAT_SelectedRows"))
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
						if (fieldName.equals("CAT_DataTabel_displayPageNo")) 
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

						if (fieldName.equals("CAT_DataTabel_displayPageLen")) 
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

						if (fieldName.equals("CAT_CountryLocale_Code")) 
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

						if (fieldName.equals("CAT_Lang_Code")) 
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


						if (fieldName.equals("CAT_SearchLevel1Code")) 
						{
							// set the value in sessionBean.setSearchCatLevel1Code
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchCatLevel1Code(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("CAT_SearchLevel2Code")) 
						{
							// set the value in sessionBean.setSearchCatLevel2Code
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setSearchCatLevel2Code(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("CAT_AddLevel1Code")) 
						{
							// set the value in sessionBean.setAddCatLevel1Code
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddCatLevel1Code(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("CAT_AddLevel2Code")) 
						{
							// set the value in sessionBean.setAddCatLevel2Code
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setAddCatLevel2Code(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("CAT_NewCatLevel1Code")) 
						{
							// set the value in sessionBean.setNewCatLevel1Code
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setNewCatLevel1Code(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("CAT_NewCatLevel1Name")) 
						{
							// set the value in sessionBean.setNewCatLevel1Name
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setNewCatLevel1Name(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("CAT_NewCatLevel2Code")) 
						{
							// set the value in sessionBean.setNewCatLevel2Code
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setNewCatLevel2Code(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("CAT_NewCatLevel2Name")) 
						{
							// set the value in sessionBean.setNewCatLevel1Code
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setNewCatLevel2Name(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("CAT_SubSubCategory_Code")) 
						{
							// set the value in sessionBean.getFieldDetails().setSubSubCategoryCode
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setSubSubCategoryCode(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("CAT_SubSubCategory_Name_En")) 
						{
							// set the value in sessionBean.getFieldDetails().setSubSubCategoryNameEng
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setSubSubCategoryNameEng(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("CAT_ActionClicked")) 
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
			Utilities.printStackTraceToLogs(Category.class.getName(), "readParamsFromRequest(", e);
		}
	}

	private  boolean validate(CategoryDetails fieldDetails, CategoryBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		boolean addMandatoryMessage = false;
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==sessionBean.getManualLanguageId() || "".equals(sessionBean.getManualLanguageId()) ||
				null==sessionBean.getAddCatLevel1Code() || "".equals(sessionBean.getAddCatLevel1Code()) || 
				null==sessionBean.getAddCatLevel2Code() || "".equals(sessionBean.getAddCatLevel2Code()) ||
				null==fieldDetails.getSubSubCategoryCode() || "".equals(fieldDetails.getSubSubCategoryCode()))
		{
			addMandatoryMessage = true;
		}

		if(null!=sessionBean.getAddCatLevel1Code() && !"".equals(sessionBean.getAddCatLevel1Code()))
		{
			if(sessionBean.getAddCatLevel1Code().equals("ADDNEW"))
			{
				if(null==sessionBean.getNewCatLevel1Code() || "".equals(sessionBean.getNewCatLevel1Code()) || 
						null==sessionBean.getNewCatLevel1Name() || "".equals(sessionBean.getNewCatLevel1Name()))
				{
					addMandatoryMessage= true;
				}
			}
		}

		if(null!=sessionBean.getAddCatLevel2Code() && !"".equals(sessionBean.getAddCatLevel2Code()))
		{
			if(sessionBean.getAddCatLevel2Code().equals("ADDNEW"))
			{
				if(null==sessionBean.getNewCatLevel2Code() || "".equals(sessionBean.getNewCatLevel2Code()) || 
						null==sessionBean.getNewCatLevel2Name() || "".equals(sessionBean.getNewCatLevel2Name()))
				{
					addMandatoryMessage= true;
				}
			}
		}

		if(addMandatoryMessage==true)
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}

		// SET FIELD VALUES
		if(null!=sessionBean.getAddCatLevel1Code() && !"".equals(sessionBean.getAddCatLevel1Code()))
		{
			if(sessionBean.getAddCatLevel1Code().equals("ADDNEW"))
			{
				if(null!=sessionBean.getNewCatLevel1Code() && !"".equals(sessionBean.getNewCatLevel1Code()))
				{
					fieldDetails.setCategoryCode(sessionBean.getNewCatLevel1Code());
				}
				if(null!=sessionBean.getNewCatLevel1Name() && !"".equals(sessionBean.getNewCatLevel1Name()))
				{
					fieldDetails.setCategoryNameEng(sessionBean.getNewCatLevel1Name());
				}
			}
			else
			{
				// use Combo Values
				fieldDetails.setCategoryCode(sessionBean.getAddCatLevel1Code());
				if(null!=sessionBean.getAddCatLevel1List() && sessionBean.getAddCatLevel1List().size()>0)
				{
					for(int a=0;a<sessionBean.getAddCatLevel1List().size();a++)
					{
						CategoryDetails cDetails = (CategoryDetails)sessionBean.getAddCatLevel1List().get(a);
						if(cDetails.getCategoryCode().trim().toLowerCase().equals(sessionBean.getAddCatLevel1Code().trim().toLowerCase()))
						{
							fieldDetails.setCategoryNameEng(cDetails.getCategoryNameEng());
							break;
						}
					}
				}
			}
		}

		if(null!=sessionBean.getAddCatLevel2Code() && !"".equals(sessionBean.getAddCatLevel2Code()))
		{
			if(sessionBean.getAddCatLevel2Code().equals("ADDNEW"))
			{
				if(null!=sessionBean.getNewCatLevel2Code() && !"".equals(sessionBean.getNewCatLevel2Code()))
				{
					fieldDetails.setSubCategoryCode(sessionBean.getNewCatLevel2Code());
				}
				if(null!=sessionBean.getNewCatLevel2Name() && !"".equals(sessionBean.getNewCatLevel2Name()))
				{
					fieldDetails.setSubCategoryNameEng(sessionBean.getNewCatLevel2Name());
				}
			}
			else
			{
				// use Combo Values
				fieldDetails.setSubCategoryCode(sessionBean.getAddCatLevel2Code());
				if(null!=sessionBean.getAddCatLevel2List() && sessionBean.getAddCatLevel2List().size()>0)
				{
					for(int a=0;a<sessionBean.getAddCatLevel2List().size();a++)
					{
						CategoryDetails cDetails = (CategoryDetails)sessionBean.getAddCatLevel2List().get(a);
						if(cDetails.getSubCategoryCode().trim().toLowerCase().equals(sessionBean.getAddCatLevel2Code().trim().toLowerCase()))
						{
							fieldDetails.setSubCategoryNameEng(cDetails.getSubCategoryNameEng());
							break;
						}
					}
				}
			}
		}


		if(null!=fieldDetails.getCategoryCode() && !"".equals(fieldDetails.getCategoryCode()))
		{
			if(fieldDetails.getCategoryCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.categorycode"),"20"));
			}
		}

		if(null!=fieldDetails.getCategoryNameEng() && !"".equals(fieldDetails.getCategoryNameEng()))
		{
			if(fieldDetails.getCategoryNameEng().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.categoryname.eng"),"200"));
			}
		}

		if(null!=fieldDetails.getSubCategoryCode() && !"".equals(fieldDetails.getSubCategoryCode()))
		{
			if(fieldDetails.getSubCategoryCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.subcategorycode"),"20"));
			}
		}

		if(null!=fieldDetails.getSubCategoryNameEng() && !"".equals(fieldDetails.getSubCategoryNameEng()))
		{
			if(fieldDetails.getSubCategoryNameEng().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.subcategoryname.eng"),"200"));
			}
		}

		if(null!=fieldDetails.getSubSubCategoryCode() && !"".equals(fieldDetails.getSubSubCategoryCode()))
		{
			if(fieldDetails.getSubSubCategoryCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.subsubcategorycode"),"20"));
			}
		}

		if(null!=fieldDetails.getSubSubCategoryNameEng() && !"".equals(fieldDetails.getSubSubCategoryNameEng()))
		{
			if(fieldDetails.getSubSubCategoryNameEng().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.subsubcategoryname.eng"),"200"));
			}
		}

		if(null!=fieldDetails.getCategoryCode() && !"".equals(fieldDetails.getCategoryCode()) && 
				null!=fieldDetails.getSubCategoryCode() && !"".equals(fieldDetails.getSubCategoryCode()) && 
				null!=fieldDetails.getSubSubCategoryCode() && !"".equals(fieldDetails.getSubSubCategoryCode()) 
				&& null!=sessionBean.getCategoryList() && sessionBean.getCategoryList().size()>0)
		{
			for(int a=0;a<sessionBean.getCategoryList().size();a++)
			{
				CategoryDetails cDetails = (CategoryDetails)sessionBean.getCategoryList().get(a);
				/*
				 * CHECK FOR THE MANDATORY FIELDS
				 */
				if(null!=cDetails.getCategoryCode() && !"".equals(cDetails.getCategoryCode()) 
						&& null!=cDetails.getSubCategoryCode() && !"".equals(cDetails.getSubCategoryCode())
						&& null!=cDetails.getSubSubCategoryCode() && !"".equals(cDetails.getSubSubCategoryCode()))
				{
					if(cDetails.getCategoryCode().trim().toLowerCase().equals(fieldDetails.getCategoryCode().trim().toLowerCase()) && 
							cDetails.getSubCategoryCode().trim().toLowerCase().equals(fieldDetails.getSubCategoryCode().trim().toLowerCase()) && 
							cDetails.getSubSubCategoryCode().trim().toLowerCase().equals(fieldDetails.getSubSubCategoryCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.categorydata")));
						break;
					}
				}
				cDetails=  null;
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

	private  boolean validateUpdate(CategoryDetails fieldDetails, CategoryBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getCategoryCode() || "".equals(fieldDetails.getCategoryCode()) || 
				null==fieldDetails.getCategoryNameEng() || "".equals(fieldDetails.getCategoryNameEng()) || 
				null==fieldDetails.getSubCategoryCode() || "".equals(fieldDetails.getSubCategoryCode()) || 
				null==fieldDetails.getSubCategoryNameEng() || "".equals(fieldDetails.getSubCategoryNameEng()) || 
				null==fieldDetails.getSubSubCategoryCode() || "".equals(fieldDetails.getSubSubCategoryCode()))
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}

		if(null!=fieldDetails.getCategoryCode() && !"".equals(fieldDetails.getCategoryCode()))
		{
			if(fieldDetails.getCategoryCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.categorycode")+",20,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}

		if(null!=fieldDetails.getCategoryNameEng() && !"".equals(fieldDetails.getCategoryNameEng()))
		{
			if(fieldDetails.getCategoryNameEng().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.categoryname.eng")+",200,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;

			}
		}

		if(null!=fieldDetails.getSubCategoryCode() && !"".equals(fieldDetails.getSubCategoryCode()))
		{
			if(fieldDetails.getSubCategoryCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.subcategorycode"),"20"));
			}
		}

		if(null!=fieldDetails.getSubCategoryNameEng() && !"".equals(fieldDetails.getSubCategoryNameEng()))
		{
			if(fieldDetails.getSubCategoryNameEng().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.subcategoryname.eng"),"200"));
			}
		}

		if(null!=fieldDetails.getSubSubCategoryCode() && !"".equals(fieldDetails.getSubSubCategoryCode()))
		{
			if(fieldDetails.getSubSubCategoryCode().length()>20)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.subsubcategorycode"),"20"));
			}
		}

		if(null!=fieldDetails.getSubSubCategoryNameEng() && !"".equals(fieldDetails.getSubSubCategoryNameEng()))
		{
			if(fieldDetails.getSubSubCategoryNameEng().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.subsubcategoryname.eng"),"200"));
			}
		}

		if(null!=fieldDetails.getCategoryCode() && !"".equals(fieldDetails.getCategoryCode()) && 
				null!=fieldDetails.getSubCategoryCode() && !"".equals(fieldDetails.getSubCategoryCode()) && 
				null!=fieldDetails.getSubSubCategoryCode() && !"".equals(fieldDetails.getSubSubCategoryCode()) 
				&& null!=sessionBean.getCategoryList() && sessionBean.getCategoryList().size()>0)
		{
			for(int a=0;a<sessionBean.getCategoryList().size();a++)
			{
				CategoryDetails cDetails = (CategoryDetails)sessionBean.getCategoryList().get(a);
				/*
				 * CHECL FOR THE MANDATORY FIELDS
				 */
				if(null!=cDetails.getCategoryCode() && !"".equals(cDetails.getCategoryCode()) 
						&& null!=cDetails.getSubCategoryCode() && !"".equals(cDetails.getSubCategoryCode())
						&& null!=cDetails.getSubSubCategoryCode() && !"".equals(cDetails.getSubSubCategoryCode()))
				{
					if(cDetails.getCategoryId() != fieldDetails.getCategoryId() && 
							cDetails.getCategoryCode().trim().toLowerCase().equals(fieldDetails.getCategoryCode().trim().toLowerCase()) &&
							cDetails.getSubCategoryCode().trim().toLowerCase().equals(fieldDetails.getSubCategoryCode().trim().toLowerCase()) && 
							cDetails.getSubSubCategoryCode().trim().toLowerCase().equals(fieldDetails.getSubSubCategoryCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.categorydata")+","+String.valueOf(fieldDetails.getSrNo());
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
						data = null;
						id = null;
						break;
					}
				}
				cDetails=  null;
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

	private  boolean validateFileUpload(CategoryBean sessionBean)
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

	private  void decideErrorDisplay(CategoryBean sessionBean, StringBuilder errorMessage, int errorCount)
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
			String eFName = ApplicationProperties.getProperty("EXPORT_DATA_CAT_NAME")+"_"+String.valueOf(currentTime)+ApplicationProperties.getProperty("EXPORT_ERROR_EXTENSION");
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
				Utilities.printStackTraceToLogs(Category.class.getName(), "validateExcelRowData()", e);
			} catch (IOException e) {
				e.printStackTrace();
				Utilities.printStackTraceToLogs(Category.class.getName(), "validateExcelRowData()", e);
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

	private  void saveCategoryDetails(CategoryBean sessionBean)
	{
		try
		{
			if(validate(sessionBean.getFieldDetails(), sessionBean))
			{
				/*
				 * call database function
				 * before that set flag as Active
				 */

				sessionBean.getFieldDetails().setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
				sessionBean.getFieldDetails().setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
				sessionBean.getFieldDetails().setFlag(ApplicationProperties.getProperty("flag.value.draft"));
				boolean bool = CategoryDAO.saveCategoryDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveCategoryDetails :: Category Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.category")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setCategoryList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					/*
					 * call getCategoryList
					 */
					getCategoryList(sessionBean);
				}
				else
				{
					logger.info("saveCategoryDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.category")));
				}
			}
			else
			{
				logger.info("saveCategoryDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Category.class.getName(), "saveCategoryDetails()", e);
		}
	}

	private  void editCategoryDetails(HttpServletRequest request, CategoryBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getCategoryList() && !"".equals(sessionBean.getCategoryList().size()>0))
			{
				for(int a=0;a<sessionBean.getCategoryList().size();a++)
				{
					CategoryDetails cDetails = (CategoryDetails)sessionBean.getCategoryList().get(a);
					cDetails.setEditableFlag(false);
				}
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getCategoryList() && !"".equals(sessionBean.getCategoryList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getCategoryList().size();a++)
							{
								CategoryDetails cDetails = (CategoryDetails)sessionBean.getCategoryList().get(a);
								if(rowId.equals(String.valueOf(cDetails.getCategoryId())))
								{
									logger.info("editCategoryDetails :: Making Row No {"+cDetails.getSrNo()+"} Editable.");
									cDetails.setEditableFlag(true);
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
				logger.info("editCategoryDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Category.class.getName(), "editCategoryDetails()", e);
		}
	}

	private  void deleteCategoryDetails(HttpServletRequest request, CategoryBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				String deleteIds=String.valueOf(sessionBean.getSelectedRows());
				if(null!=deleteIds && !"".equals(deleteIds))
				{
					if(deleteIds.endsWith(","))
					{
						deleteIds = deleteIds.substring(0,deleteIds.length()-1);
					}
					boolean bool = CategoryDAO.deleteCategoryDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.category")));
						/*
						 * call getCategoryList
						 */
						getCategoryList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.category")));
					}
				}
			}
			else
			{
				logger.info("deleteCategoryDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Category.class.getName(), "deleteCategoryDetails()", e);
		}
	}

	private  void activeCategoryDetails(HttpServletRequest request, CategoryBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				String activeIds=String.valueOf(sessionBean.getSelectedRows());
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
					CategoryDetails cDetails = null;
					Map<String,String> dataMap = null;
					if(null!=tok && tok.length>0)
					{
						for(int a=0;a<tok.length;a++)
						{
							if(null!=sessionBean.getCategoryList() && sessionBean.getCategoryList().size()>0)
							{
								cDetails  =null;
								for(int b=0;b<sessionBean.getCategoryList().size();b++)
								{
									cDetails=  (CategoryDetails)sessionBean.getCategoryList().get(b);
									if(String.valueOf(cDetails.getCategoryId()).equals(tok[a]))
									{
										dataMap= new HashMap<String, String>();
										dataMap.put("ID", tok[a]);
										dataMap.put("FLAG", cDetails.getOldFlag());
										activeIdsList.add(dataMap);
										dataMap=  null;
									}
									cDetails = null;
								}
							}
						}
					}
					tok = null;
					dataMap=  null;
					boolean bool = CategoryDAO.activeCategoryDetails(activeIdsList);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("active.success", msgProps.getProperty("label.category")));
						/*
						 * call getCategoryList
						 */
						getCategoryList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.active", msgProps.getProperty("label.category")));
					}
					activeIdsList = null;
				}
				activeIds  =null;
			}
			else
			{
				logger.info("activeCategoryDetails :: No Row selected for Active, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.active");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Category.class.getName(), "activeCategoryDetails()", e);
		}
	}

	private  void exportExportDetails(HttpServletRequest request, CategoryBean sessionBean)
	{
		sessionBean.setReportViewPath(null);
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				ArrayList<CategoryDetails> exportDataList = new ArrayList<CategoryDetails>();
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getCategoryList() && !"".equals(sessionBean.getCategoryList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getCategoryList().size();a++)
							{
								CategoryDetails cDetails = (CategoryDetails)sessionBean.getCategoryList().get(a);
								if(rowId.equals(String.valueOf(cDetails.getCategoryId())))
								{
									// add to export List
									exportDataList.add(cDetails);
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
					writeCVCExcel(exportDataList, sessionBean);
					/*
					 * PREARE CVC REORT PATH AND MAKE IT DOWNLOAD
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
					logger.info("exportExportDetails :: No Row selected for EXPORT, throwing message.");
					String errorMessage = msgProps.getProperty("error.select.onerow.export");
					sessionBean.setErrorMessage(errorMessage);
					errorMessage  =null;
				}
				exportDataList = null;
			}
			else
			{
				logger.info("exportExportDetails :: No Row selected for EXPORT, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.export");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Category.class.getName(), "exportExportDetails()", e);
		}
	}

	private  void updateCategoryDetails(HttpServletRequest request, CategoryBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getUpdatedRows() && !"".equals(sessionBean.getUpdatedRows()))
			{
				ArrayList<CategoryDetails> updateDataList = new ArrayList<CategoryDetails>();
				String updatedRows=(String)sessionBean.getUpdatedRows();
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getCategoryList() && sessionBean.getCategoryList().size()>0)
					{

						for(int i=0;i<sessionBean.getCategoryList().size();i++)
						{
							CategoryDetails cDetails = (CategoryDetails)sessionBean.getCategoryList().get(i);
							if(cDetails.isEditableFlag()==true)
							{
								// set fields empty 
								cDetails.setCategoryCode("");
								cDetails.setCategoryNameEng("");
								cDetails.setFlag("");
								cDetails.setSubCategoryCode("");
								cDetails.setSubCategoryNameEng("");
								cDetails.setSubSubCategoryCode("");
								cDetails.setSubSubCategoryNameEng("");

								/*
								 * fetch the values from request
								 * and set in LanguageList
								 */
								String catCodeId="CAT_CategoryList_Code_"+String.valueOf(cDetails.getCategoryId());
								String carNameEnId="CAT_CategoryList_Name_En_"+String.valueOf(cDetails.getCategoryId());

								String subCatCodeId="CAT_CategoryList_SubCatCode_"+String.valueOf(cDetails.getCategoryId());
								String subCarNameEnId="CAT_CategoryList_SubCatName_En_"+String.valueOf(cDetails.getCategoryId());

								String subSubCatCodeId="CAT_CategoryList_SubSubCatCode_"+String.valueOf(cDetails.getCategoryId());
								String subSubCarNameEnId="CAT_CategoryList_SubSubCatName_En_"+String.valueOf(cDetails.getCategoryId());


								String flag="CAT_LangList_Flag_"+String.valueOf(cDetails.getCategoryId());

								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										if(key.equals(catCodeId))
										{
											cDetails.setCategoryCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(carNameEnId))
										{
											cDetails.setCategoryNameEng(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(subCatCodeId))
										{
											cDetails.setSubCategoryCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(subCarNameEnId))
										{
											cDetails.setSubCategoryNameEng(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(subSubCatCodeId))
										{
											cDetails.setSubSubCategoryCode(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(subSubCarNameEnId))
										{
											cDetails.setSubSubCategoryNameEng(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));
										}
										else if(key.equals(flag))
										{
											cDetails.setFlag(token.substring(token.indexOf("<MDM_TS>")+8, token.length()));	
										}
										key = null;
										token = null;
									}
								}

								// set all request params ids to null
								carNameEnId = null;
								catCodeId = null;

								subCarNameEnId = null;
								subCatCodeId = null;

								subSubCarNameEnId = null;
								subSubCatCodeId = null;
								flag= null;
							}
						}

						for(int i=0;i<sessionBean.getCategoryList().size();i++)
						{
							CategoryDetails cDetails = (CategoryDetails)sessionBean.getCategoryList().get(i);
							if(cDetails.isEditableFlag()==true)
							{
								/*
								 * add to Update List
								 * Before adding validate data for each Row.
								 * validate cDetails Object
								 */
								if(validateUpdate(cDetails, sessionBean))
								{
									/*
									 * add data to updateList
									 */
									updateDataList.add(cDetails);
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
							logger.info("updateCategoryDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
							/*
							 * Iterate UpdateList and update each row one by one.
							 */
							String errorMessage="";
							String successMessage="";
							Connection conn = null;
							String closeConnection=null;
							try
							{
								conn = DBConnectionHelper.getConnection();
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(Category.class.getName(), "updateCategoryDetails()", e);
							}
							for(int a=0;a<updateDataList.size();a++)
							{
								CategoryDetails cDetails = (CategoryDetails)updateDataList.get(a);
								boolean updateFlag = CategoryDAO.updateCategoryDetails(cDetails, conn, closeConnection);
								if(updateFlag==true)
								{
									logger.info("updateCategoryDetails :: Category Details updated successfully for Row No :: > " + cDetails.getSrNo());
									successMessage = successMessage+String.valueOf(cDetails.getSrNo())+",";
								}
								else
								{
									logger.info("updateCategoryDetails :: Failed to Update Book Details for Row No :: >  "+ cDetails.getSrNo());
									errorMessage = errorMessage+String.valueOf(cDetails.getSrNo())+",";	
								}
								cDetails=  null;
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
								Utilities.printStackTraceToLogs(Category.class.getName(), "updateCategoryDetails()", e);
							}
							conn = null;
							
							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.category"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.category"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateCategoryDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setCategoryList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getCategoryList
								 */
								getCategoryList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateCategoryDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getCategoryList() && sessionBean.getCategoryList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										for(int a=0;a<sessionBean.getCategoryList().size();a++)
										{
											CategoryDetails cDetails = (CategoryDetails)sessionBean.getCategoryList().get(a);
											cDetails.setEditableFlag(false);
											for(int b=0;b<tokens.length;b++)
											{
												if(tokens[b].toString().equals(String.valueOf(cDetails.getSrNo())))
												{
													cDetails.setEditableFlag(true);
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
			Utilities.printStackTraceToLogs(Category.class.getName(), "updateCategoryDetails()", e);
		}
	}

	private  boolean validateExcelRowData(CategoryBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		int errorCount=0;
		if(null!=sessionBean.getCategoryListToImport() && sessionBean.getCategoryListToImport().size()>0)
		{
			for(int i=0;i<sessionBean.getCategoryListToImport().size();i++)
			{
				CategoryDetails fieldDetails = (CategoryDetails) sessionBean.getCategoryListToImport().get(i);
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


				if(null==fieldDetails.getCategoryCode() || "".equals(fieldDetails.getCategoryCode()) ||
						null==fieldDetails.getCategoryNameEng() || "".equals(fieldDetails.getCategoryNameEng()) ||
						null==fieldDetails.getSubCategoryCode() || "".equals(fieldDetails.getSubCategoryCode()) ||
						null==fieldDetails.getSubCategoryNameEng() || "".equals(fieldDetails.getSubCategoryNameEng()) ||
						null==fieldDetails.getSubSubCategoryCode() || "".equals(fieldDetails.getSubSubCategoryCode()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					/*
					 * INCOMPLETE DATA FOR VIN AT ROW NO . rowNo
					 */
					String data = msgProps.getProperty("label.category")+","+String.valueOf(rowNo);
					String[] id = data.split(",");
					errorMessage.append(msgProps.getMessage(id, "error.excel.improper.lines"));
					data = null;
					id = null;
					// increment errorCount by 1
					errorCount++;
				}

				if(null!=fieldDetails.getCategoryCode() && !"".equals(fieldDetails.getCategoryCode()))
				{
					if(fieldDetails.getCategoryCode().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.categorycode")+",20,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getCategoryNameEng() && !"".equals(fieldDetails.getCategoryNameEng()))
				{
					if(fieldDetails.getCategoryNameEng().length()>200)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.categoryname.eng")+",200,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getSubCategoryCode() && !"".equals(fieldDetails.getSubCategoryCode()))
				{
					if(fieldDetails.getSubCategoryCode().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.subcategorycode")+",20,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getSubCategoryNameEng() && !"".equals(fieldDetails.getSubCategoryNameEng()))
				{
					if(fieldDetails.getSubCategoryNameEng().length()>200)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.subcategoryname.eng")+",200,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getSubSubCategoryCode() && !"".equals(fieldDetails.getSubSubCategoryCode()))
				{
					if(fieldDetails.getSubSubCategoryCode().length()>20)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.subsubcategorycode")+",20,"+String.valueOf(rowNo);
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
						data = null;
						id = null;
						// increment errorCount by 1
						errorCount++;
					}
				}

				if(null!=fieldDetails.getSubSubCategoryNameEng() && !"".equals(fieldDetails.getSubSubCategoryNameEng()))
				{
					if(fieldDetails.getSubSubCategoryNameEng().length()>200)
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.subsubcategoryname.eng")+",200,"+String.valueOf(rowNo);
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
			errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.category")));
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

	private  void executeExcelOperation(CategoryBean sessionBean, byte[] data, String extension)
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

				String duplicateRowNo="";
				int errorCount=0;
				int duplicateRowsCount=0;
				int failureCount=0;
				ArrayList<CategoryDetails> listToSave = new ArrayList<CategoryDetails>();
				// ACTION=D rows are collected separately - they are a DELETE, not a save.
				ArrayList<CategoryDetails> listToDelete = new ArrayList<CategoryDetails>();
				for(int i=0;i<sessionBean.getCategoryListToImport().size();i++)
				{
					CategoryDetails fieldDetails = (CategoryDetails)sessionBean.getCategoryListToImport().get(i);
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
							CategoryDetails existingDetails = (CategoryDetails)listToSave.get(j);
							if(fieldDetails.getCategoryCode().equals(existingDetails.getCategoryCode()) 
									&& fieldDetails.getCategoryNameEng().equals(existingDetails.getCategoryNameEng())
									&& fieldDetails.getSubCategoryCode().equals(existingDetails.getSubCategoryCode())
									&& fieldDetails.getSubCategoryNameEng().equals(existingDetails.getSubCategoryNameEng())
									&& fieldDetails.getSubSubCategoryCode().equals(existingDetails.getSubSubCategoryCode()))
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
							errorMessage.append(msgProps.addMessage("error.excel.duplicate.lines", msgProps.getProperty("label.category"), String.valueOf(rows[a])));
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
					String closeConnection=null;
					Connection conn = null;
					try
					{
						conn = DBConnectionHelper.getConnection();
					}
					catch(Exception e)
					{
						Utilities.printStackTraceToLogs(Category.class.getName(), "executeExcelOperation()", e);
					}
					
					for(int i=0;i<listToSave.size();i++)
					{
						CategoryDetails fieldDetails = (CategoryDetails)listToSave.get(i);

						fieldDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
						fieldDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
						fieldDetails.setFlag(ApplicationProperties.getProperty("flag.value.draft"));
						boolean saveCategoryData = CategoryDAO.importCategoryDetails(fieldDetails, conn, closeConnection);

						if(saveCategoryData == true)
						{
							logger.info("executeExcelOperation() :: Category Data for Line No {"+(i+1+1)+"}. Saved Successfully.");
							if(null!=successLineNo && !"".equals(successLineNo))
							{
								successLineNo = successLineNo+",";
							}
							successLineNo = successLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						else
						{
							logger.info("executeExcelOperation() :: Failed to Import Category Data for Line No {"+(i+1+1)+"}.");
							if(null!=errorLineNo && !"".equals(errorLineNo))
							{
								errorLineNo = errorLineNo+",";
							}
							errorLineNo = errorLineNo+String.valueOf(fieldDetails.getSrNo());
						}
						fieldDetails= null;
					}
					
					closeConnection = null;
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
						Utilities.printStackTraceToLogs(Category.class.getName(), "executeExcelOperation()", e);
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
						successRows = null;

						//						successLineNo = "( "+successLineNo+" )";
						//						sessionBean.setSuccessMessage(msgProps.addMessage("import.success",msgProps.getProperty("label.category"), successLineNo ));
						sessionBean.setSelectedRows(null);
						sessionBean.setShowUpdate(false);
						/*
						 * call function to load updated category list
						 */
						getCategoryList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import", msgProps.getProperty("label.category"), String.valueOf(errorRows[a])));
							}
						}
						errorRows = null;
						//						errorLineNo= "( "+errorLineNo+ " )";
						//						sessionBean.setErrorMessage(msgProps.addMessage("error.import", msgProps.getProperty("label.category"), errorLineNo));
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
					errorMessage.append(msgProps.addMessage("error.no.data.found.excel.import", msgProps.getProperty("label.category")));
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
							CategoryDetails deleteDetails = (CategoryDetails)listToDelete.get(i);
							deleteDetails.setCountryLocaleId(new Long(sessionBean.getCountryLocaleId()).longValue());
							deleteDetails.setManualLanguageId(new Long(sessionBean.getManualLanguageId()).longValue());
							long existingId = 0;
							try
							{
								existingId = CategoryDAO.findExistingIdForImport(deleteDetails, deleteConn);
							}
							catch(Exception e)
							{
								Utilities.printStackTraceToLogs(Category.class.getName(), "executeExcelOperation()", e);
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
						Utilities.printStackTraceToLogs(Category.class.getName(), "executeExcelOperation()", e);
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
							Utilities.printStackTraceToLogs(Category.class.getName(), "executeExcelOperation()", e);
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
							deleted = CategoryDAO.deleteCategoryDetails(deleteIds);
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(Category.class.getName(), "executeExcelOperation()", e);
						}
						if(deleted==true)
						{
							deleteSuccessCount = deleteIdList.size();
							getCategoryList(sessionBean);
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
								errorMessage.append(msgProps.addMessage("error.import.delete", msgProps.getProperty("label.category"), String.valueOf(deleteErrorRows[a])));
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
			Utilities.printStackTraceToLogs(Category.class.getName(), "executeExcelOperation()", e);
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
			Utilities.printStackTraceToLogs(Category.class.getName(), "readCellValue()", e);
			// set cellValue to null
			cellValue = null;
		}
		return cellValue;
	}

	private  void writeCVCExcel(ArrayList<CategoryDetails> categoryList, CategoryBean sessionBean)
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
				// add CVC DATA NAME
				String name = "";
				/*
				 * add SELECTED COUNTRY LOCALE CODE, ADD MANUAL LANGUAGE CODE, 
				 * add Current Time Stamp in Format - DDMMYYYY HHMMSS
				 * 
				 * So the final Name will be - US_EN-US_CVC_DDMMYYYY_HHMMSS.XSLX
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
				name = name.trim()+ApplicationProperties.getProperty("EXPORT_DATA_CAT_NAME");
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

				Cell catCodeCell = headerRow.createCell(0);
				catCodeCell.setCellValue("CATEGORY LEVEL 1 CODE");
				Cell catNameEngCell = headerRow.createCell(1);
				catNameEngCell.setCellValue("CATEGORY LEVEL 1");
				Cell subCatCodeCell = headerRow.createCell(2);
				subCatCodeCell.setCellValue("CATEGORY LEVEL 2 CODE");
				Cell subCatNameEngCell = headerRow.createCell(3);
				subCatNameEngCell.setCellValue("CATEGORY LEVEL 2");
				Cell symCodeCell = headerRow.createCell(4);
				symCodeCell.setCellValue("CATEGORY LEVEL 3 CODE");
				Cell symNameEngCell = headerRow.createCell(5);
				symNameEngCell.setCellValue("CATEGORY LEVEL 3");


				int rowCount=0;
				if(null!=categoryList && categoryList.size()>0)
				{
					for(int i=0;i<categoryList.size();i++)
					{
						CategoryDetails cDetails = (CategoryDetails)categoryList.get(i);
						rowCount++;
						Row row = mySheet.createRow(rowCount);

						Cell cell0 = row.createCell(0);
						Cell cell1 = row.createCell(1);
						Cell cell2 = row.createCell(2);
						Cell cell3 = row.createCell(3);
						Cell cell4 = row.createCell(4);
						Cell cell5 = row.createCell(5);

						cell0.setCellValue("");
						cell1.setCellValue("");
						cell2.setCellValue("");
						cell3.setCellValue("");
						cell4.setCellValue("");
						cell5.setCellValue("");

						if(null!=cDetails.getCategoryCode() && !"".equals(cDetails.getCategoryCode()))
						{
							cell0.setCellValue(cDetails.getCategoryCode());
						}
						if(null!=cDetails.getCategoryNameEng() && !"".equals(cDetails.getCategoryNameEng()))
						{
							cell1.setCellValue(cDetails.getCategoryNameEng());
						}
						if(null!=cDetails.getSubCategoryCode() && !"".equals(cDetails.getSubCategoryCode()))
						{
							cell2.setCellValue(cDetails.getSubCategoryCode());
						}
						if(null!=cDetails.getSubCategoryNameEng() && !"".equals(cDetails.getSubCategoryNameEng()))
						{
							cell3.setCellValue(cDetails.getSubCategoryNameEng());
						}

						if(null!=cDetails.getSubSubCategoryCode() && !"".equals(cDetails.getSubSubCategoryCode()))
						{
							cell4.setCellValue(cDetails.getSubSubCategoryCode());
						}
						if(null!=cDetails.getSubSubCategoryNameEng() && !"".equals(cDetails.getSubSubCategoryNameEng()))
						{
							cell5.setCellValue(cDetails.getSubSubCategoryNameEng());
						}


						cell0 = null;
						cell1 = null;
						cell2 = null;
						cell3 = null;
						cell4 = null;
						cell5 = null;
						row = null;
						cDetails = null;
					}

					headerRow =  null;
					catCodeCell=null;
					catNameEngCell=null;
					subCatCodeCell=null;
					subCatNameEngCell=null;
					symCodeCell=null;
					symNameEngCell=null;

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
			Utilities.printStackTraceToLogs(Category.class.getName(), "writeCVCExcel()", e);
		}
	}

	private  void readExcelData(byte[] data, CategoryBean sessionBean, String extension)
	{
		sessionBean.setCategoryListToImport(new ArrayList<CategoryDetails>());
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
				 * RANK
				 * CATEGORY CODDE
				 * CATEGORY NAME
				 * SUB CATEGORY CODE
				 * SUB CATEGORY NAME
				 * SYMPTOM CODE
				 * SYMPTOM NAME
				 * SUB SYMPTOM CODE
				 * SUB SYMPTOM NAME
				 * CONDITION CODE
				 * CONDITION NAME
				 * 
				 */
				while(null!=rowIterator && rowIterator.hasNext())
				{
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						CategoryDetails cDetails = new CategoryDetails();
						Object dataCell = readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							cDetails.setCategoryCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							cDetails.setCategoryNameEng(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							cDetails.setSubCategoryCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = readCellValue(row.getCell(3));
						if(null!=dataCell && !"".equals(dataCell))
						{
							cDetails.setSubCategoryNameEng(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = readCellValue(row.getCell(4));
						if(null!=dataCell && !"".equals(dataCell))
						{
							cDetails.setSubSubCategoryCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						dataCell = readCellValue(row.getCell(5));
						if(null!=dataCell && !"".equals(dataCell))
						{
							cDetails.setSubSubCategoryNameEng(String.valueOf(dataCell).trim());
						}
						dataCell = null;

						/*
						 * NEW ACTION / MARKER COLUMN (last column of the import template).
						 * A/ADD (create), U/UPDATE (update), D/DELETE (soft delete).
						 * Blank or any non-delete value falls through to the existing
						 * create-or-update path, so an OLD template still imports.
						 */
						dataCell = readCellValue(row.getCell(6));
						if(null!=dataCell && !"".equals(dataCell))
						{
							cDetails.setImportAction(String.valueOf(dataCell).trim());
						}
						dataCell = null;


						/*
						 * add details to categoryList for import
						 */
						if(null==sessionBean.getCategoryListToImport() || sessionBean.getCategoryListToImport().size()<=0)
						{
							sessionBean.setCategoryListToImport(new ArrayList<CategoryDetails>());
						}

						sessionBean.getCategoryListToImport().add(cDetails);
						cDetails= null;
					}
					// INCREMENT ROW COUNT BY 1
					rowCount++;
					row = null;
				}
				sheet = null;
				workbook = null;
				xlsWorkBook  = null;
				xlsSheet  = null;
				rowIterator = null;
				is.close();
				is = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(Category.class.getName(), "readExcelData()", e);
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

	private void performAccessCheck(CategoryBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(Category.class.getName(), "performAccessCheck()", e);
		}
	}

}