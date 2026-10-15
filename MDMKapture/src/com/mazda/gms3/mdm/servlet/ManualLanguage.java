package com.mazda.gms3.mdm.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.mazda.gms3.mdm.bean.ManualLanguageBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.CountryLocaleComparator;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ManualLanguageDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class ManualLanguage
 */
public class ManualLanguage extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	static Logger logger = LogManager.getLogger(ManualLanguage.class);
	static String wslId=null;
	static MessageProperties msgProps= null;
	String moduleRefKey=AccessManagementInterface.REF_KEY_MANUAL_LANGUAGE;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ManualLanguage() {
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
			ManualLanguageBean sessionBean = getSessionBean(request);
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
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			
			/*
			 * call function to load all the Country Locale Data
			 */
			getCountryLocaleList(sessionBean, request);
			
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualLanguage.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/manualLanguage.jsp");
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
			ManualLanguageBean sessionBean = getSessionBean(request);
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
			/*
			 * set displayPageNo and displayPageLenght
			 */
			if(null!=request.getParameter("ML_DataTabel_displayPageNo") && !"".equals(request.getParameter("ML_DataTabel_displayPageNo")))
			{
				sessionBean.setDisplayPageNo((String)request.getParameter("ML_DataTabel_displayPageNo"));
			}
			if(null!=request.getParameter("ML_DataTabel_displayPageLen") && !"".equals(request.getParameter("ML_DataTabel_displayPageLen")))
			{
				sessionBean.setDisplayPageLength((String)request.getParameter("ML_DataTabel_displayPageLen"));
			}
			
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
			
			/*
			 * call function to read parameters from request
			 */
			readParamsFromRequest(sessionBean, request);
			
			if(null!=request.getParameter("ML_CountrylocaleSelection") && !"".equals(request.getParameter("ML_CountrylocaleSelection")))
			{
				sessionBean.setShowUpdate(false);
				sessionBean.setSelectedRows(null);
				/*
				 * call function to load all the Manual language data
				 */
				getLanguageList(sessionBean);
			}
			
			if(null!=request.getParameter("ML_Entry") && !"".equals(request.getParameter("ML_Entry")))
			{
				/*
				 * Save Operation called
				 */
				saveLanguageDetails(sessionBean);
			}
			
			if(null!=request.getParameter("ML_EditAction") && !"".equals(request.getParameter("ML_EditAction")))
			{
				/*
				 * Edit Operation called
				 */
				editLanguageDetails(request, sessionBean);	
			}
			
			if(null!=request.getParameter("ML_DeleteAction") && !"".equals(request.getParameter("ML_DeleteAction")))
			{
				/*
				 * Delete Operation called
				 */
				deleteManualLanguageDetails(request, sessionBean);	
			}
			
			if(null!=request.getParameter("ML_PermanentDeleteAction") && !"".equals(request.getParameter("ML_PermanentDeleteAction")))
			{
				/*
				 * PERMANENT DELETE OPERATION CALLED
				 */
				permanentDeleteManualLanguageDetails(request, sessionBean);
			}
			
			if(null!=request.getParameter("ML_Update") && !"".equals(request.getParameter("ML_Update")))
			{
				/*
				 * Update Operation called
				 */
				updateLanguageDetails(request, sessionBean);
			}
			
			if(null!=request.getParameter("ML_ResetAction") && !"".equals(request.getParameter("ML_ResetAction")))
			{
				// reset fields
				sessionBean.setLanguageList(null);
				sessionBean.setFlagList(null);
				sessionBean.setErrorMessage(null);
				sessionBean.setSuccessMessage(null);
				sessionBean.setSelectedRows(null);
				sessionBean.setShowUpdate(false);
				sessionBean.setDisplayPageLength(null);
				sessionBean.setDisplayPageNo(null);
				/*
				 * call getLanguageList
				 */
				getLanguageList(sessionBean);	
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualLanguage.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/manualLanguage.jsp");
				rs.forward(request, response);
			}
		}
	}

	private ManualLanguageBean getSessionBean(HttpServletRequest request) 
	{
		ManualLanguageBean sessionBean = null;
		if (null != request.getSession().getAttribute("manualLanguageBean") && !"".equals(request.getSession().getAttribute("manualLanguageBean"))) 
		{
			sessionBean = (ManualLanguageBean) request.getSession().getAttribute("manualLanguageBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new ManualLanguageBean();
			request.getSession().setAttribute("manualLanguageBean", sessionBean);
		}
		return sessionBean;
	}
	
	private void getCountryLocaleList(ManualLanguageBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(ManualLanguage.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private static void getLanguageList(ManualLanguageBean sessionBean)
	{
		try
		{
			sessionBean.setLanguageList(new ArrayList<ManualLanguageDetails>());
			if(null!=sessionBean.getCountryLocaleId() && !"".equals(sessionBean.getCountryLocaleId()))
			{
				ArrayList<ManualLanguageDetails> list = new ArrayList<ManualLanguageDetails>();
				list = ManualLanguageDAO.getManualLanguageDetailsList(sessionBean.getCountryLocaleId());
				if(null!=list && list.size()>0)
				{
					for(int i=0;i<list.size();i++)
					{
						ManualLanguageDetails mlDetails = (ManualLanguageDetails)list.get(i);
						if(null!=mlDetails.getFlag() && !"".equals(mlDetails.getFlag()))
						{
							// set Label
							if(mlDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
							{
								mlDetails.setFlagLabel(msgProps.getProperty("flag.label.active"));
							}
							else if(mlDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.sleep")))
							{
								mlDetails.setFlagLabel(msgProps.getProperty("flag.label.sleep"));
							}
							else if(mlDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
							{
								mlDetails.setFlagLabel(msgProps.getProperty("flag.label.draft"));
							}
							else if(mlDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
							{
								mlDetails.setFlagLabel(msgProps.getProperty("flag.label.deprecated"));
							}
							else if(mlDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.delete")))
							{
								mlDetails.setFlagLabel(msgProps.getProperty("flag.label.delete"));
							}
						}
						mlDetails=  null;
					}
					sessionBean.setLanguageList(list);
				}
				list = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualLanguage.class.getName(), "getLanguageList()", e);
		}
	}
	
	private static void getFlagList(ManualLanguageBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());
		ArrayList<SelectItemDetails> flagList = Utilities.prepareFlagsList(msgProps);
		// here add Inactive as Well.
		SelectItemDetails si = new SelectItemDetails();
		si.setLabel(msgProps.getProperty("flag.label.delete"));
		si.setValue(ApplicationProperties.getProperty("flag.value.delete"));
		flagList.add(si);
		si = null;
		sessionBean.setFlagList(flagList);
		flagList=null;
	}

	private static void readParamsFromRequest(ManualLanguageBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new ManualLanguageDetails());
			sessionBean.setCountryLocaleId(null);
			if(null!=request.getParameter("ML_CountryLocale_Code") && !"".equals(request.getParameter("ML_CountryLocale_Code")))
			{
				sessionBean.setCountryLocaleId((String)request.getParameter("ML_CountryLocale_Code").trim());
			}
			if(null!=request.getParameter("ML_Lang_Code") && !"".equals(request.getParameter("ML_Lang_Code")))
			{
				sessionBean.getFieldDetails().setManualLanguageCode((String)request.getParameter("ML_Lang_Code").trim());
			}
			
			if(null!=request.getParameter("ML_Lang_Name") && !"".equals(request.getParameter("ML_Lang_Name")))
			{
				sessionBean.getFieldDetails().setManualLanguageName((String)request.getParameter("ML_Lang_Name").trim());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static boolean validate(ManualLanguageDetails fieldDetails, ManualLanguageBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==sessionBean.getCountryLocaleId() || "".equals(sessionBean.getCountryLocaleId()) || 
				null==fieldDetails.getManualLanguageCode() || "".equals(fieldDetails.getManualLanguageCode()) || 
				null==fieldDetails.getManualLanguageName() || "".equals(fieldDetails.getManualLanguageName()))  
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}
		
		if(null!=fieldDetails.getManualLanguageCode() && !"".equals(fieldDetails.getManualLanguageCode()))
		{
			if(fieldDetails.getManualLanguageCode().length()>10)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
//				errorMessage.append(msgProps.addMessage("error.length.exact.characters", msgProps.getProperty("label.languagecode"),"5"));
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.languagecode"),"10"));
			}
		}
		
		if(null!=fieldDetails.getManualLanguageName() && !"".equals(fieldDetails.getManualLanguageName()))
		{
			if(fieldDetails.getManualLanguageName().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.languagename"),"200"));
			}
		}
		
		
		if(null!=fieldDetails.getManualLanguageCode() && !"".equals(fieldDetails.getManualLanguageCode()))
		{
			if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
			{
				for(int a=0;a<sessionBean.getLanguageList().size();a++)
				{
					ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
					if(mlDetails.getManualLanguageCode().trim().toLowerCase().equals(fieldDetails.getManualLanguageCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.languagecode")));
						break;
					}
					mlDetails=  null;
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
	
	private static boolean validateUpdate(ManualLanguageDetails fieldDetails, ManualLanguageBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getManualLanguageCode() || "".equals(fieldDetails.getManualLanguageCode()) || 
				null==fieldDetails.getManualLanguageName() || "".equals(fieldDetails.getManualLanguageName())) 
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}
		
		if(null!=fieldDetails.getManualLanguageCode() && !"".equals(fieldDetails.getManualLanguageCode()))
		{
			if(fieldDetails.getManualLanguageCode().length()>10)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
//				String data = msgProps.getProperty("label.languagecode")+",5,"+String.valueOf(fieldDetails.getSrNo());
				String data = msgProps.getProperty("label.languagecode")+",10,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
//				errorMessage.append(msgProps.getMessage(id, "error.length.exact.characters.for.row"));
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}

		if(null!=fieldDetails.getManualLanguageName() && !"".equals(fieldDetails.getManualLanguageName()))
		{
			if(fieldDetails.getManualLanguageName().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.languagename")+",200,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getManualLanguageCode() && !"".equals(fieldDetails.getManualLanguageCode()))
		{
			if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
			{
				for(int a=0;a<sessionBean.getLanguageList().size();a++)
				{
					ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
					if(mlDetails.getManualLanguageId()!=fieldDetails.getManualLanguageId() && 
							mlDetails.getManualLanguageCode().trim().toLowerCase().equals(fieldDetails.getManualLanguageCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.languagecode")+","+String.valueOf(fieldDetails.getSrNo());
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
						data = null;
						id = null;
						break;
					}
					mlDetails=  null;
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
	
	private static void saveLanguageDetails(ManualLanguageBean sessionBean)
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
				sessionBean.getFieldDetails().setFlag(ApplicationProperties.getProperty("flag.value.draft"));
				boolean bool = ManualLanguageDAO.saveManualLanguageDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveLanguageDetails :: Manual Language Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.manuallanguage")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setLanguageList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					/*
					 * call getLanguageList
					 */
					getLanguageList(sessionBean);
				}
				else
				{
					logger.info("saveLanguageDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.manuallanguage")));
				}
			}
			else
			{
				logger.info("saveLanguageDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualLanguage.class.getName(), "saveLanguageDetails()", e);
		}
	}
	
	private static void editLanguageDetails(HttpServletRequest request, ManualLanguageBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			sessionBean.setSelectedRows(null);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getLanguageList() && !"".equals(sessionBean.getLanguageList().size()>0))
			{
				for(int a=0;a<sessionBean.getLanguageList().size();a++)
				{
					ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
					mlDetails.setEditableFlag(false);
				}
			}
			if(null!=request.getParameter("ML_SelectedRows") && !"".equals(request.getParameter("ML_SelectedRows")))
			{
				sessionBean.setSelectedRows(String.valueOf(request.getParameter("ML_SelectedRows")));
				
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getLanguageList() && !"".equals(sessionBean.getLanguageList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getLanguageList().size();a++)
							{
								ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
								if(rowId.equals(String.valueOf(mlDetails.getManualLanguageId())))
								{
									logger.info("editLanguageDetails :: Making Row No {"+mlDetails.getSrNo()+"} Editable.");
									mlDetails.setEditableFlag(true);
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
				logger.info("editLanguageDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualLanguage.class.getName(), "editLanguageDetails()", e);
		}
	}

	private static void deleteManualLanguageDetails(HttpServletRequest request, ManualLanguageBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("ML_SelectedRows") && !"".equals(request.getParameter("ML_SelectedRows")))
			{
				String deleteIds=String.valueOf(request.getParameter("ML_SelectedRows"));
				if(null!=deleteIds && !"".equals(deleteIds))
				{
					if(deleteIds.endsWith(","))
					{
						deleteIds = deleteIds.substring(0,deleteIds.length()-1);
					}
					boolean bool = ManualLanguageDAO.deleteManualLanguageDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.manuallanguage")));
						/*
						 * call getCarList
						 */
						getLanguageList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.manuallanguage")));
					}
				}
			}
			else
			{
				logger.info("deleteManualLanguageDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualLanguage.class.getName(), "deleteManualLanguageDetails()", e);
		}
	}

	private static void permanentDeleteManualLanguageDetails(HttpServletRequest request, ManualLanguageBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("ML_SelectedRows") && !"".equals(request.getParameter("ML_SelectedRows")))
			{
				String deleteIds=String.valueOf(request.getParameter("ML_SelectedRows"));
				if(null!=deleteIds && !"".equals(deleteIds))
				{
					if(deleteIds.endsWith(","))
					{
						deleteIds = deleteIds.substring(0,deleteIds.length()-1);
					}
					boolean bool = ManualLanguageDAO.permanentDeleteManualLanguageDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("permanent.delete.success", msgProps.getProperty("label.manuallanguage")));
						/*
						 * call getCarList
						 */
						getLanguageList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.permanent.delete", msgProps.getProperty("label.manuallanguage")));
					}
				}
			}
			else
			{
				logger.info("permanentDeleteManualLanguageDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.permanent.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(java.sql.SQLIntegrityConstraintViolationException e)
		{
			/*
			 * The selected Manual Language is still referenced by dependent data, so the DB
			 * refused the permanent delete (MySQL 1451). Show the specific "in use" message
			 * rather than the generic operation-failed one.
			 */
			logger.info("permanentDeleteManualLanguageDetails :: Manual Language is in use - cannot permanently delete.");
			sessionBean.setErrorMessage(msgProps.addMessage("error.message.permanent.delete.inuse", msgProps.getProperty("label.manuallanguage")));
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ManualLanguage.class.getName(), "permanentDeleteManualLanguageDetails()", e);
		}
	}

	
	private static void updateLanguageDetails(HttpServletRequest request, ManualLanguageBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("ML_UpdatedRows") && !"".equals(request.getParameter("ML_UpdatedRows")))
			{
				ArrayList<ManualLanguageDetails> updateDataList = new ArrayList<ManualLanguageDetails>();
				String updatedRows=(String)request.getParameter("ML_UpdatedRows");
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
					{

						for(int i=0;i<sessionBean.getLanguageList().size();i++)
						{
							ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(i);
							if(mlDetails.isEditableFlag()==true)
							{
								// set fields empty 
								mlDetails.setManualLanguageName("");
								mlDetails.setFlag("");
								mlDetails.setManualLanguageCode("");

								/*
								 * fetch the values from request
								 * and set in LanguageList
								 */
								String languageCodeId="ML_LangList_Code_"+String.valueOf(mlDetails.getManualLanguageId());
								String languageNameId="ML_LangList_Name_"+String.valueOf(mlDetails.getManualLanguageId());
								String flag="ML_LangList_Flag_"+String.valueOf(mlDetails.getManualLanguageId());


								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										String value=token.substring(token.indexOf("<MDM_TS>")+8, token.length());
										if(key.equals(languageCodeId))
										{
											mlDetails.setManualLanguageCode(value);
										}
										if(key.equals(languageNameId))
										{
											mlDetails.setManualLanguageName(value);
										}
										else if(key.equals(flag))
										{
											mlDetails.setFlag(value);	
										}
										key = null;
										value= null;
										token = null;
									}
								}

								// set all request params ids to null
								languageCodeId = null;
								languageNameId = null;
								flag= null;
							}
						}

						for(int i=0;i<sessionBean.getLanguageList().size();i++)
						{
							ManualLanguageDetails mlDetails = (ManualLanguageDetails)sessionBean.getLanguageList().get(i);
							if(mlDetails.isEditableFlag()==true)
							{
								/*
								 * add to Update List
								 * Before adding validate data for each Row.
								 * validate mlDetails Object
								 */
								if(validateUpdate(mlDetails, sessionBean))
								{
									/*
									 * add data to updateList
									 */
									updateDataList.add(mlDetails);
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
							logger.info("updateLanguageDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
							/*
							 * Iterate UpdateList and update each row one by one.
							 */
							String errorMessage="";
							String successMessage="";

							for(int a=0;a<updateDataList.size();a++)
							{
								ManualLanguageDetails mlDetails = (ManualLanguageDetails)updateDataList.get(a);
								boolean updateFlag = ManualLanguageDAO.updateManualLanguageDetails(mlDetails);
								if(updateFlag==true)
								{
									logger.info("updateLanguageDetails :: Manual Language Details updated successfully for Row No :: > " + mlDetails.getSrNo());
									successMessage = successMessage+String.valueOf(mlDetails.getSrNo())+",";
								}
								else
								{
									logger.info("updateLanguageDetails :: Failed to Update Manual Language Details for Row No :: >  "+ mlDetails.getSrNo());
									errorMessage = errorMessage+String.valueOf(mlDetails.getSrNo())+",";	
								}
								mlDetails=  null;
							}

							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.manuallanguage"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.manuallanguage"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateLanguageDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setLanguageList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getLanguageList
								 */
								getLanguageList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateLanguageDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getLanguageList() && sessionBean.getLanguageList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										for(int a=0;a<sessionBean.getLanguageList().size();a++)
										{
											ManualLanguageDetails mlD = (ManualLanguageDetails)sessionBean.getLanguageList().get(a);
											mlD.setEditableFlag(false);
											for(int b=0;b<tokens.length;b++)
											{
												if(tokens[b].toString().equals(String.valueOf(mlD.getSrNo())))
												{
													mlD.setEditableFlag(true);
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
			Utilities.printStackTraceToLogs(ManualLanguage.class.getName(), "updateLanguageDetails()", e);
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
	
	private void performAccessCheck(ManualLanguageBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(ManualLanguage.class.getName(), "performAccessCheck()", e);
		}
	}

}
