package com.mazda.gms3.mdm.servlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import com.mazda.gms3.mdm.bean.CountryLocaleBean;
import com.mazda.gms3.mdm.bean.UserAccessBean;
import com.mazda.gms3.mdm.dao.CountryLocaleDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.AccessManagementInterface;
import com.mazda.gms3.mdm.vo.CountryLocaleDetails;
import com.mazda.gms3.mdm.vo.ModuleDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class CountryLocale
 */

public class CountryLocale extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	static Logger logger = LogManager.getLogger(CountryLocale.class);
	
	static MessageProperties msgProps= null;
	
    static String wslId="";
    
    static String moduleRefKey=AccessManagementInterface.REF_KEY_COUNTRY_LOCALE;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CountryLocale() {
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
			
			/*
			 * Initialize bean
			 */
			CountryLocaleBean sessionBean = getSessionBean(request);
			performAccessCheck(sessionBean, request);
			if(sessionBean.isShowReadControls()==false && sessionBean.isShowWriteControls()==false)
			{
				// USER DOES NOT HAVE ACCESS TO THIS PAGE - REDIRECT TO NO ACCESS PAGE
				response.sendRedirect(request.getContextPath() + "/noaccess");
				useReqDis = false;
			}
			
			
			msgProps = new MessageProperties(request.getSession().getAttribute("MDM_LS_Locale"));
			sessionBean.setCountryLocaleList(null);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			/*
			 * call function to load all the Manual language data
			 */
			getCountryLocaleList(sessionBean);
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CountryLocale.class.getName(), "doGet()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/countryLocale.jsp");
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
			CountryLocaleBean sessionBean = getSessionBean(request);
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
			if(null!=request.getParameter("CON_DataTabel_displayPageNo") && !"".equals(request.getParameter("CON_DataTabel_displayPageNo")))
			{
				sessionBean.setDisplayPageNo((String)request.getParameter("CON_DataTabel_displayPageNo"));
			}
			if(null!=request.getParameter("CON_DataTabel_displayPageLen") && !"".equals(request.getParameter("CON_DataTabel_displayPageLen")))
			{
				sessionBean.setDisplayPageLength((String)request.getParameter("CON_DataTabel_displayPageLen"));
			}
			
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
			
			/*
			 * call function to read parameters from request
			 */
			readParamsFromRequest(sessionBean, request);
			
			
			if(null!=request.getParameter("CON_Entry") && !"".equals(request.getParameter("CON_Entry")))
			{
				/*
				 * Save Operation called
				 */
				saveCountryLocaleDetails(sessionBean);
			}
			
			if(null!=request.getParameter("CON_EditAction") 
					&& !"".equals(request.getParameter("CON_EditAction")) && !"null".equals(request.getParameter("CON_EditAction")))
			{
				/*
				 * Edit Operation called
				 */
				editCountryLocaleDetails(request, sessionBean);
			
			}
			
			if(null!=request.getParameter("CON_DeleteAction") && !"".equals(request.getParameter("CON_DeleteAction")) 
					&& !"null".equals(request.getParameter("CON_DeleteAction")))
			{
				/*
				 * Delete Operation called
				 */
				deleteCountryLocaleDetails(request, sessionBean);	
			}
			
			if(null!=request.getParameter("CON_PermanentDeleteAction") && !"".equals(request.getParameter("CON_PermanentDeleteAction")) 
				&& !"null".equals(request.getParameter("CON_PermanentDeleteAction")))
			{
				/*
				 * PERMAMNENT DELETE OPERATION
				 */
				permanentDeleteCountryLocaleDetails(request, sessionBean);
			}
			
			if(null!=request.getParameter("CON_Update") && !"".equals(request.getParameter("CON_Update")) && !"null".equals(request.getParameter("CON_Update")))
			{
				/*
				 * Update Operation called
				 */
				updateCountryLocaleDetails(request, sessionBean);
			}
			
			if(null!=request.getParameter("CON_ResetAction") && !"".equals(request.getParameter("CON_ResetAction")) && !"null".equals(request.getParameter("CON_ResetAction")))
			{
				// reset fields
				sessionBean.setCountryLocaleList(null);
				sessionBean.setFlagList(null);
				sessionBean.setErrorMessage(null);
				sessionBean.setSuccessMessage(null);
				sessionBean.setSelectedRows(null);
				sessionBean.setShowUpdate(false);
				sessionBean.setDisplayPageLength(null);
				sessionBean.setDisplayPageNo(null);
				/*
				 * call getCountryLocaleList
				 */
				getCountryLocaleList(sessionBean);	
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CountryLocale.class.getName(), "doPost()", e);
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
				RequestDispatcher rs = request.getRequestDispatcher("/jsps/countryLocale.jsp");
				rs.forward(request, response);
			}
		}
		
	}


	private CountryLocaleBean getSessionBean(HttpServletRequest request) 
	{
		CountryLocaleBean sessionBean = null;
		if (null != request.getSession().getAttribute("countryLocaleBean") && !"".equals(request.getSession().getAttribute("countryLocaleBean"))) 
		{
			sessionBean = (CountryLocaleBean) request.getSession().getAttribute("countryLocaleBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new CountryLocaleBean();
			request.getSession().setAttribute("countryLocaleBean", sessionBean);
		}
		return sessionBean;
	}
	
	private static void getCountryLocaleList(CountryLocaleBean sessionBean)
	{
		try
		{
			sessionBean.setCountryLocaleList(new ArrayList<CountryLocaleDetails>());
			ArrayList<CountryLocaleDetails> list = new ArrayList<CountryLocaleDetails>();
			list = CountryLocaleDAO.getCountryLocaleDetailsList();
			if(null!=list && list.size()>0)
			{
				for(int i=0;i<list.size();i++)
				{
					CountryLocaleDetails clDetails = (CountryLocaleDetails)list.get(i);
					if(null!=clDetails.getFlag() && !"".equals(clDetails.getFlag()))
					{
						// set Label
						if(clDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
						{
							clDetails.setFlagLabel(msgProps.getProperty("flag.label.active"));
						}
						else if(clDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.sleep")))
						{
							clDetails.setFlagLabel(msgProps.getProperty("flag.label.sleep"));
						}
						else if(clDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
						{
							clDetails.setFlagLabel(msgProps.getProperty("flag.label.draft"));
						}
						else if(clDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
						{
							clDetails.setFlagLabel(msgProps.getProperty("flag.label.deprecated"));
						}
						else if(clDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.delete")))
						{
							clDetails.setFlagLabel(msgProps.getProperty("flag.label.delete"));
						}
					}
					clDetails=  null;
				}
				sessionBean.setCountryLocaleList(list);
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CountryLocale.class.getName(), "getCountryLocaleList()", e);
		}
	}
	
	private static void getFlagList(CountryLocaleBean sessionBean)
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


	private static void readParamsFromRequest(CountryLocaleBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new CountryLocaleDetails());
			if(null!=request.getParameter("CON_Locale_Code") && !"".equals(request.getParameter("CON_Locale_Code")))
			{
				sessionBean.getFieldDetails().setCountryLocaleCode((String)request.getParameter("CON_Locale_Code").trim());
			}
			
			if(null!=request.getParameter("CON_Locale_Desc") && !"".equals(request.getParameter("CON_Locale_Desc")))
			{
				sessionBean.getFieldDetails().setCountryLocaleDesc((String)request.getParameter("CON_Locale_Desc").trim());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static boolean validate(CountryLocaleDetails fieldDetails, CountryLocaleBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getCountryLocaleCode() || "".equals(fieldDetails.getCountryLocaleCode()))  
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}
		
		if(null!=fieldDetails.getCountryLocaleCode() && !"".equals(fieldDetails.getCountryLocaleCode()))
		{
			if(fieldDetails.getCountryLocaleCode().length()!=2)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.exact.characters", msgProps.getProperty("label.countrylocalecode"),"2"));
			}
		}
		
		if(null!=fieldDetails.getCountryLocaleDesc() && !"".equals(fieldDetails.getCountryLocaleDesc()))
		{
			if(fieldDetails.getCountryLocaleDesc().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.countrylocaledesc"),"200"));
			}
		}
		
		if(null!=fieldDetails.getCountryLocaleCode() && !"".equals(fieldDetails.getCountryLocaleCode()))
		{
			if(null!=sessionBean.getCountryLocaleList() && sessionBean.getCountryLocaleList().size()>0)
			{
				for(int a=0;a<sessionBean.getCountryLocaleList().size();a++)
				{
					CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(a);
					if(clDetails.getCountryLocaleCode().trim().toLowerCase().equals(fieldDetails.getCountryLocaleCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.countrylocalecode")));
						break;
					}
					clDetails=  null;
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
	
	private static boolean validateUpdate(CountryLocaleDetails fieldDetails, CountryLocaleBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getCountryLocaleCode() || "".equals(fieldDetails.getCountryLocaleCode())) 
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}
		
		if(null!=fieldDetails.getCountryLocaleCode() && !"".equals(fieldDetails.getCountryLocaleCode()))
		{
			if(fieldDetails.getCountryLocaleCode().length()!=2)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.countrylocalecode")+",2,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.exact.characters.for.row"));
				data = null;
				id = null;
			}
		}

		if(null!=fieldDetails.getCountryLocaleDesc() && !"".equals(fieldDetails.getCountryLocaleDesc()))
		{
			if(fieldDetails.getCountryLocaleDesc().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.countrylocaledesc")+",200,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;
			}
		}
		
		if(null!=fieldDetails.getCountryLocaleCode() && !"".equals(fieldDetails.getCountryLocaleCode()))
		{
			if(null!=sessionBean.getCountryLocaleList() && sessionBean.getCountryLocaleList().size()>0)
			{
				for(int a=0;a<sessionBean.getCountryLocaleList().size();a++)
				{
					CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(a);
					if(clDetails.getCountryLocaleId()!=fieldDetails.getCountryLocaleId() && 
							clDetails.getCountryLocaleCode().trim().toLowerCase().equals(fieldDetails.getCountryLocaleCode().trim().toLowerCase()))
					{
						if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
						{
							errorMessage.append("<MSG_TOKEN>");
						}
						String data = msgProps.getProperty("label.countrylocalecode")+","+String.valueOf(fieldDetails.getSrNo());
						String[] id = data.split(",");
						errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
						data = null;
						id = null;
						break;
					}
					clDetails=  null;
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
	
	private static void saveCountryLocaleDetails(CountryLocaleBean sessionBean)
	{
		try
		{
			if(validate(sessionBean.getFieldDetails(), sessionBean))
			{
				/*
				 * call database function
				 * before that set flag as Active
				 */
				sessionBean.getFieldDetails().setFlag(ApplicationProperties.getProperty("flag.value.draft"));
				boolean bool = CountryLocaleDAO.saveCountryLocaleDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					logger.info("saveCountryLocaleDetails :: Country Locale Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.countrylocale")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setCountryLocaleList(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					/*
					 * call getCountryLocaleList
					 */
					getCountryLocaleList(sessionBean);
				}
				else
				{
					logger.info("saveCountryLocaleDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.countrylocale")));
				}
			}
			else
			{
				logger.info("saveCountryLocaleDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CountryLocale.class.getName(), "saveCountryLocaleDetails()", e);
		}
	}
	
	private static void editCountryLocaleDetails(HttpServletRequest request, CountryLocaleBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
			sessionBean.setSelectedRows(null);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getCountryLocaleList() && !"".equals(sessionBean.getCountryLocaleList().size()>0))
			{
				for(int a=0;a<sessionBean.getCountryLocaleList().size();a++)
				{
					CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(a);
					clDetails.setEditableFlag(false);
				}
			}
			if(null!=request.getParameter("CON_SelectedRows") && !"".equals(request.getParameter("CON_SelectedRows")))
			{
				sessionBean.setSelectedRows(String.valueOf(request.getParameter("CON_SelectedRows")));
				
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getCountryLocaleList() && !"".equals(sessionBean.getCountryLocaleList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getCountryLocaleList().size();a++)
							{
								CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(a);
								if(rowId.equals(String.valueOf(clDetails.getCountryLocaleId())))
								{
									logger.info("editCountryLocaleDetails :: Making Row No {"+clDetails.getSrNo()+"} Editable.");
									clDetails.setEditableFlag(true);
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
				logger.info("editCountryLocaleDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CountryLocale.class.getName(), "editCountryLocaleDetails()", e);
		}
	}

	private static void deleteCountryLocaleDetails(HttpServletRequest request, CountryLocaleBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("CON_SelectedRows") && !"".equals(request.getParameter("CON_SelectedRows")))
			{
				String deleteIds=String.valueOf(request.getParameter("CON_SelectedRows"));
				if(null!=deleteIds && !"".equals(deleteIds))
				{
					if(deleteIds.endsWith(","))
					{
						deleteIds = deleteIds.substring(0,deleteIds.length()-1);
					}
					boolean bool = CountryLocaleDAO.deleteCountryLocaleDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.countrylocale")));
						/*
						 * call getCountryLocaleList
						 */
						getCountryLocaleList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.countrylocale")));
					}
				}
			}
			else
			{
				logger.info("deleteCountryLocaleDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CountryLocaleDAO.class.getName(), "deleteCountryLocaleDetails()", e);
		}
	}

	private static void permanentDeleteCountryLocaleDetails(HttpServletRequest request, CountryLocaleBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("CON_SelectedRows") && !"".equals(request.getParameter("CON_SelectedRows")))
			{
				String deleteIds=String.valueOf(request.getParameter("CON_SelectedRows"));
				if(null!=deleteIds && !"".equals(deleteIds))
				{
					if(deleteIds.endsWith(","))
					{
						deleteIds = deleteIds.substring(0,deleteIds.length()-1);
					}
					boolean bool = CountryLocaleDAO.permanentDeleteCountryLocaleDetails(deleteIds);
					if(bool==true)
					{
						sessionBean.setSuccessMessage(msgProps.addMessage("permanent.delete.success", msgProps.getProperty("label.countrylocale")));
						/*
						 * call getCountryLocaleList
						 */
						getCountryLocaleList(sessionBean);
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.permanent.delete", msgProps.getProperty("label.countrylocale")));
					}
				}
			}
			else
			{
				logger.info("permanentDeleteCountryLocaleDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.permanent.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(java.sql.SQLIntegrityConstraintViolationException e)
		{
			/*
			 * The selected Country/Locale is still referenced by dependent data, so the DB
			 * refused the permanent delete (MySQL 1451). Show the specific "in use" message
			 * rather than the generic operation-failed one.
			 */
			logger.info("permanentDeleteCountryLocaleDetails :: Country/Locale is in use - cannot permanently delete.");
			sessionBean.setErrorMessage(msgProps.addMessage("error.message.permanent.delete.inuse", msgProps.getProperty("label.countrylocale")));
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CountryLocaleDAO.class.getName(), "permanentDeleteCountryLocaleDetails()", e);
		}
	}


	private static void updateCountryLocaleDetails(HttpServletRequest request, CountryLocaleBean sessionBean)
	{
		try
		{
			if(null!=request.getParameter("CON_UpdatedRows") && !"".equals(request.getParameter("CON_UpdatedRows")))
			{
				ArrayList<CountryLocaleDetails> updateDataList = new ArrayList<CountryLocaleDetails>();
				String updatedRows=(String)request.getParameter("CON_UpdatedRows");
				String[] updatedRowsTokens=updatedRows.split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getCountryLocaleList() && sessionBean.getCountryLocaleList().size()>0)
					{

						for(int i=0;i<sessionBean.getCountryLocaleList().size();i++)
						{
							CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(i);
							if(clDetails.isEditableFlag()==true)
							{
								// set fields empty 
								clDetails.setCountryLocaleDesc("");
								clDetails.setFlag("");
								clDetails.setCountryLocaleCode("");

								/*
								 * fetch the values from request
								 * and set in LanguageList
								 */
								String languageCodeId="CON_LocaleList_Code_"+String.valueOf(clDetails.getCountryLocaleId());
								String languageNameId="CON_LocaleList_Name_"+String.valueOf(clDetails.getCountryLocaleId());
								String flag="CON_LocaleList_Flag_"+String.valueOf(clDetails.getCountryLocaleId());

								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key=token.substring(0,token.indexOf("<MDM_TS>"));
										String value=token.substring(token.indexOf("<MDM_TS>")+8, token.length());
										if(key.equals(languageCodeId))
										{
											clDetails.setCountryLocaleCode(value);
										}
										if(key.equals(languageNameId))
										{
											clDetails.setCountryLocaleDesc(value);
										}
										else if(key.equals(flag))
										{
											clDetails.setFlag(value);	
										}
										key = null;
										value= null;
										token = null;
									}
								}

								// set all request params ids to null
								languageCodeId = null;
								languageNameId = null;
								/*
								cdromId=  null;
								ewInducingId = null;
								contentLang = null;
								*/
								flag= null;
							}
						}

						for(int i=0;i<sessionBean.getCountryLocaleList().size();i++)
						{
							CountryLocaleDetails clDetails = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(i);
							if(clDetails.isEditableFlag()==true)
							{
								/*
								 * add to Update List
								 * Before adding validate data for each Row.
								 * validate clDetails Object
								 */
								if(validateUpdate(clDetails, sessionBean))
								{
									/*
									 * add data to updateList
									 */
									updateDataList.add(clDetails);
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
							logger.info("updateCountryLocaleDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
							/*
							 * Iterate UpdateList and update each row one by one.
							 */
							String errorMessage="";
							String successMessage="";

							for(int a=0;a<updateDataList.size();a++)
							{
								CountryLocaleDetails clDetails = (CountryLocaleDetails)updateDataList.get(a);
								boolean updateFlag = CountryLocaleDAO.updateCountryLocaleDetails(clDetails);
								if(updateFlag==true)
								{
									logger.info("updateCountryLocaleDetails :: Country Locale Details updated successfully for Row No :: > " + clDetails.getSrNo());
									successMessage = successMessage+String.valueOf(clDetails.getSrNo())+",";
								}
								else
								{
									logger.info("updateCountryLocaleDetails :: Failed to Update Country Locale Details for Row No :: >  "+ clDetails.getSrNo());
									errorMessage = errorMessage+String.valueOf(clDetails.getSrNo())+",";	
								}
								clDetails=  null;
							}

							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.countrylocale"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.countrylocale"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateCountryLocaleDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setCountryLocaleList(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								/*
								 * call getCountryLocaleList
								 */
								getCountryLocaleList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateCountryLocaleDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getCountryLocaleList() && sessionBean.getCountryLocaleList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										for(int a=0;a<sessionBean.getCountryLocaleList().size();a++)
										{
											CountryLocaleDetails mlD = (CountryLocaleDetails)sessionBean.getCountryLocaleList().get(a);
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
			Utilities.printStackTraceToLogs(CountryLocale.class.getName(), "updateCountryLocaleDetails()", e);
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
	
	private void performAccessCheck(CountryLocaleBean sessionBean, HttpServletRequest request)
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
			Utilities.printStackTraceToLogs(CountryLocale.class.getName(), "performAccessCheck()", e);
		}
	}
}
