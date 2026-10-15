package com.mazda.gms3.mdm.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

import com.mazda.gms3.mdm.bean.RuleBean;
import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.dao.RuleDAO;
import com.mazda.gms3.mdm.email.generators.AlternateManualLanguageEmailGenerator;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.MessageProperties;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.RuleDetails;
import com.mazda.gms3.mdm.vo.RuleLanguageDetails;
import com.mazda.gms3.mdm.vo.RuleModelDetails;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

/**
 * Servlet implementation class AlternateManualLanguage
 */
public class AlternateManualLanguage extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	static Logger logger = LogManager.getLogger(AlternateManualLanguage.class);
	
	static MessageProperties msgProps= null;
	
	static String wslId=null;

	static String defaultRule = ApplicationProperties.getProperty("aml.default.rule");
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AlternateManualLanguage() {
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
			RuleBean sessionBean = getSessionBean(request);
			sessionBean.setFlagList(null);
			sessionBean.setFieldDetails(null);
			sessionBean.setErrorMessage(null);
			sessionBean.setSuccessMessage(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setShowUpdate(false);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setRuleList(null);
			sessionBean.setRuleListForCombo(null);
			sessionBean.setRuleId(null);
			sessionBean.setRuleModelsList(null);
			sessionBean.setRuleLanguageList(null);
			sessionBean.setWdRuleLanguageList(null);
			sessionBean.setActionClicked(null);
			sessionBean.setModelSrNo(null);
			sessionBean.setLanguageSrNo(null);
			sessionBean.setLanguageDelType(null);
			sessionBean.setUpdatedRows(null);
			sessionBean.setDefaultRule(false);
			/*
			 * call function to load all the RuleData
			 */
			getRuleList(sessionBean);
			
			/*
			 * call function to load flag status values
			 */
			getFlagList(sessionBean);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		if (useReqDis == true) 
		{
			RequestDispatcher rs = request.getRequestDispatcher("/jsps/altManualLanguage.jsp");
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
			RuleBean sessionBean = getSessionBean(request);
			sessionBean.setErrorMessage("");
			sessionBean.setSuccessMessage("");
			
			
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
				if(sessionBean.getActionClicked().equals("SAVE_RULE"))
				{
					/*
					 * Save Operation called
					 */
					saveRuleDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("EDIT_RULE"))
				{
					/*
					 * Edit Operation Called
					 */
					editRuleDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_RULE"))
				{
					/*
					 * Delete Operation Called
					 */
					deleteRuleDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("UPDATE_RULE"))
				{
					/*
					 * Update Operation Called
					 */
					updateRuleDetails(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("RULE_SELECTION"))
				{
					/*
					 * cHANGE Rule selection Operation
					 * fetch Models and LanguageMapping details
					 */
					performRuleSelectionOperation(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("RESET_ACTION"))
				{

					// reset some fields
					sessionBean.setRuleList(null);
					sessionBean.setFlagList(null);
					sessionBean.setErrorMessage(null);
					sessionBean.setSuccessMessage(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setDisplayPageLength(null);
					sessionBean.setDisplayPageNo(null);
					/*
					 * call getRuleList
					 */
					getRuleList(sessionBean);	
				}
				else if(sessionBean.getActionClicked().equals("SAVE_MAPPING"))
				{

					/*
					 * call save Mapping function
					 */
					saveMapping(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_MODEL_MAPPING"))
				{
					/*
					 * CALL MODEL DELETE OPERATION
					 */
					deleteModelMapping(sessionBean);
				}
				else if(sessionBean.getActionClicked().equals("DELETE_LANGUAGE_MAPPING"))
				{
					/*
					 * Call LANGAUGE DELETE OPERATION
					 */
					deleteLanguageMapping(sessionBean);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		if(useReqDis==true)
		{
			RequestDispatcher rs = request.getRequestDispatcher("/jsps/altManualLanguage.jsp");
			rs.forward(request, response);
		}
	}
	
	private RuleBean getSessionBean(HttpServletRequest request) 
	{
		RuleBean sessionBean = null;
		if (null != request.getSession().getAttribute("ruleBean") && !"".equals(request.getSession().getAttribute("ruleBean"))) 
		{
			sessionBean = (RuleBean) request.getSession().getAttribute("ruleBean");
		} 
		else 
		{
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new RuleBean();
			request.getSession().setAttribute("ruleBean", sessionBean);
		}
		return sessionBean;
	}
	
	private static void getRuleList(RuleBean sessionBean)
	{
		try
		{
			sessionBean.setDefaultRule(false);
			sessionBean.setRuleListForCombo(null);
			sessionBean.setRuleList(new ArrayList<RuleDetails>());
			sessionBean.setRuleListForCombo(new ArrayList<RuleDetails>());
			ArrayList<RuleDetails> list = new ArrayList<RuleDetails>();
			list = RuleDAO.getRuleDetailsList();
			if(null!=list && list.size()>0)
			{
				for(int i=0;i<list.size();i++)
				{
					RuleDetails rDetails = (RuleDetails)list.get(i);
					if(rDetails.getRuleName().trim().toLowerCase().equals(defaultRule.trim().toLowerCase()))
					{
						rDetails.setDefaultRule(true);
					}
					if(null!=rDetails.getFlag() && !"".equals(rDetails.getFlag()))
					{
						// set Label
						if(rDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
						{
							rDetails.setFlagLabel(msgProps.getProperty("flag.label.active"));
						}
						else if(rDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.sleep")))
						{
							rDetails.setFlagLabel(msgProps.getProperty("flag.label.sleep"));
						}
						else if(rDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.draft")))
						{
							rDetails.setFlagLabel(msgProps.getProperty("flag.label.draft"));
						}
						else if(rDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
						{
							rDetails.setFlagLabel(msgProps.getProperty("flag.label.deprecated"));
						}
					}
					rDetails=  null;
				}
				sessionBean.setRuleList(list);
				
				/*
				 * ITERATE AND SKIP RULES WITH STATUS AS DELETE AND DRAFT
				 */
				for(int i=0;i<list.size();i++)
				{
					RuleDetails rDetails = (RuleDetails)list.get(i);
					if(null!=rDetails.getFlag() && !"".equals(rDetails.getFlag()))
					{
						if(rDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")) ||
								rDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.deprecated")))
						{
							sessionBean.getRuleListForCombo().add(rDetails);
						}
					}
					rDetails=null;
				}
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "getRuleList()", e);
		}
	}
	
	private static void getFlagList(RuleBean sessionBean)
	{
		sessionBean.setFlagList(new ArrayList<SelectItemDetails>());
		
		sessionBean.setFlagList(Utilities.prepareFlagsList(msgProps));
	}

	private static void readParamsFromRequest(RuleBean sessionBean, HttpServletRequest request)
	{
		try
		{
			sessionBean.setFieldDetails(new RuleDetails());
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setRuleId(null);
			sessionBean.setActionClicked(null);
			sessionBean.setModelSrNo(null);
			sessionBean.setLanguageSrNo(null);
			sessionBean.setLanguageDelType(null);
			sessionBean.setUpdatedRows(null);
			
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
			logger.info("readParamsFromRequest() :: iterating File Items");
			while (iterator.hasNext()) 
			{
				FileItem fileItem = iterator.next();
				if (fileItem.isFormField())
				{
					logger.info("readParamsFromRequest() :: When Fields are Not Form Fields. Check each File Name and set the values accordingly in each attribute.");
					String fieldName = fileItem.getFieldName();
					if (null != fieldName && !"".equals(fieldName)) 
					{
						if (fieldName.equals("ALT_RULE_RuleId")) 
						{
							// set the value in sessionBean.displayPageNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setRuleId(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("ALT_RULE_UpdatedRows")) 
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
						/*
						 * set displayPageNo and displayPageLenght
						 */
						if (fieldName.equals("ALT_RULE_DataTabel_displayPageNo")) 
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
						
						if (fieldName.equals("ALT_RULE_DataTabel_displayPageLen")) 
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
						
						if (fieldName.equals("ALT_RULE_SelectedRows")) 
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
						
						if (fieldName.equals("ALT_RULE_Name")) 
						{
							// set the value in sessionBean.fieldDetails.ruleName  
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setRuleName(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("ALT_RULE_Desc")) 
						{
							// set the value in sessionBean.fieldDetails.ruleDesc
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.getFieldDetails().setRuleDesc(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("ALT_RULE_Model_Delete")) 
						{
							// set the value in sessionBean.setModelSrNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setModelSrNo(value);
							}
							// set value to null
							value = null;
						}
						
						if (fieldName.equals("ALT_RULE_Language_Delete")) 
						{
							// set the value in sessionBean.languageSrNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setLanguageSrNo(value);
							}
							// set value to null
							value = null;
						}
						
						if(fieldName.equals("ALT_RULE_Language_DeleteType"))
						{
							// set the value in sessionBean.languageDelType
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value) && !"''".equals(value) && !"\"\"".equals(value) && !"null".equals(value)) 
							{
								sessionBean.setLanguageDelType(value);
							}
							// set value to null
							value = null;
						}
						if (fieldName.equals("ALT_RULE_ActionClicked")) 
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
							 * check for CSV
							 */
							String extension="";
							if(fileName.lastIndexOf(".")!=-1)
							{
								extension = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
								if(null!=extension && !"".equals(extension))
								{
									if(extension.trim().toLowerCase().equals("csv"))
									{						
										/*
										 * proceed for uploading and parsing.
										 */
										readCSVData(data, sessionBean, fileName);
									}
									else
									{
										logger.info("readParamsFromRequest() :: Extension is not CSV. Throw Message - Uploaded file not supported."); 
										sessionBean.setErrorMessage(msgProps.getProperty("error.valid.file.notsupported"));
									}
								}
								else
								{
									logger.info("readParamsFromRequest() :: File Name does not contain valid extension. Throw message - Please upload a valid CSV File.");
									sessionBean.setErrorMessage(msgProps.getProperty("error.valid.csv"));
								}
							}
							else
							{
								logger.info("readParamsFromRequest() :: File Name does not contain any extension. Throw message - Please upload a valid CSV File.");
								sessionBean.setErrorMessage(msgProps.getProperty("error.valid.csv"));
							}
							extension = null;
						}	
						else
						{
							logger.info("readParamsFromRequest() :: File Name / Size is null. Throw message - Please upload a valid CSV File.");
							sessionBean.setErrorMessage(msgProps.getProperty("error.valid.csv"));
						}
						fileName=  null;
						data = null;
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private static boolean validate(RuleDetails fieldDetails, RuleBean sessionBean)
	{
		
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getRuleName() || "".equals(fieldDetails.getRuleName()))
		{
			errorMessage.append(msgProps.getProperty("error.mandatory.fields"));
		}
		
		if(null!=fieldDetails.getRuleName() && !"".equals(fieldDetails.getRuleName()))
		{
			if(fieldDetails.getRuleName().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.rulename"),"200"));
			}
		}
		
		if(null!=fieldDetails.getRuleDesc() && !"".equals(fieldDetails.getRuleDesc()))
		{
			if(fieldDetails.getRuleDesc().length()>300)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				errorMessage.append(msgProps.addMessage("error.length.greater.characters", msgProps.getProperty("label.ruledesc"),"300"));
			}
		}
		
		if(null!=fieldDetails.getRuleName() && !"".equals(fieldDetails.getRuleName()) && 
		null!=sessionBean.getRuleList() && sessionBean.getRuleList().size()>0)
		{
			for(int a=0;a<sessionBean.getRuleList().size();a++)
			{
				RuleDetails rDetails = (RuleDetails)sessionBean.getRuleList().get(a);
				if(rDetails.getRuleName().trim().toLowerCase().equals(fieldDetails.getRuleName().trim().toLowerCase()))
				{
					if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
					{
						errorMessage.append("<MSG_TOKEN>");
					}
					errorMessage.append(msgProps.addMessage("error.unique", msgProps.getProperty("label.rulename")));
					break;
				}
				rDetails=  null;
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
	
	private static boolean validateUpdate(RuleDetails fieldDetails, RuleBean sessionBean)
	{
		StringBuilder errorMessage = new StringBuilder();
		if(null==fieldDetails.getRuleName() || "".equals(fieldDetails.getRuleName()))
		{
			errorMessage.append(msgProps.addMessage("error.mandatory.fields.for.row", String.valueOf(fieldDetails.getSrNo())));
		}

		if(null!=fieldDetails.getRuleName() && !"".equals(fieldDetails.getRuleName()))
		{
			if(fieldDetails.getRuleName().length()>200)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.rulename")+",200,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;

			}
		}
		
		if(null!=fieldDetails.getRuleDesc() && !"".equals(fieldDetails.getRuleDesc()))
		{
			if(fieldDetails.getRuleDesc().length()>300)
			{
				if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
				{
					errorMessage.append("<MSG_TOKEN>");
				}
				String data = msgProps.getProperty("label.ruledesc")+",300,"+String.valueOf(fieldDetails.getSrNo());
				String[] id = data.split(",");
				errorMessage.append(msgProps.getMessage(id, "error.length.greater.characters.for.row"));
				data = null;
				id = null;

			}
		}
		
		if(null!=fieldDetails.getRuleName() && !"".equals(fieldDetails.getRuleName()) && 
				null!=sessionBean.getRuleList() && sessionBean.getRuleList().size()>0)
				{
					for(int a=0;a<sessionBean.getRuleList().size();a++)
					{
						RuleDetails rDetails = (RuleDetails)sessionBean.getRuleList().get(a);
						if(rDetails.getRuleId()!=fieldDetails.getRuleId() && rDetails.getRuleName().trim().toLowerCase().equals(fieldDetails.getRuleName().trim().toLowerCase()))
						{
							if(null!=errorMessage && null!=errorMessage.toString() && !"".equals(errorMessage.toString()))
							{
								errorMessage.append("<MSG_TOKEN>");
							}
							String data = msgProps.getProperty("label.rulename")+","+String.valueOf(fieldDetails.getSrNo());
							String[] id = data.split(",");
							errorMessage.append(msgProps.getMessage(id, "error.unique.update"));
							data = null;
							id = null;
							break;
						}
						rDetails=  null;
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
	
	private static void saveRuleDetails(RuleBean sessionBean)
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
				boolean bool = RuleDAO.saveRuleDetails(sessionBean.getFieldDetails());
				if(bool==true)
				{
					/*
					 * call function to send Email Notification at back end
					 */
					AlternateManualLanguageEmailGenerator.emailDataForNewRecord(sessionBean.getFieldDetails(), wslId);
					
					logger.info("saveRuleDetails :: Rule Details inserted successfully.");
					sessionBean.setSuccessMessage(msgProps.addMessage("entry.success", msgProps.getProperty("label.rule")));
					// reset fields
					sessionBean.setErrorMessage(null);
					sessionBean.setFieldDetails(null);
					sessionBean.setRuleList(null);
					sessionBean.setRuleListForCombo(null);
					sessionBean.setRuleId(null);
					sessionBean.setSelectedRows(null);
					sessionBean.setShowUpdate(false);
					sessionBean.setRuleModelsList(null);
					sessionBean.setRuleLanguageList(null);
					/*
					 * call getRuleList
					 */
					getRuleList(sessionBean);
				}
				else
				{
					logger.info("saveRuleDetails :: Insertion Fails. ");
					// set errorMessage
					sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.rule")));
				}
			}
			else
			{
				logger.info("saveRuleDetails :: Validation Fails :: Error Messages :: > " + sessionBean.getErrorMessage());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "saveRuleDetails()", e);
		}
	}
	
	private static void editRuleDetails(RuleBean sessionBean)
	{
		try
		{
			sessionBean.setShowUpdate(false);
//			sessionBean.setSelectedRows(null);
			// set editableFlag for all rows to false
			if(null!=sessionBean.getRuleList() && !"".equals(sessionBean.getRuleList().size()>0))
			{
				for(int a=0;a<sessionBean.getRuleList().size();a++)
				{
					RuleDetails rDetails = (RuleDetails)sessionBean.getRuleList().get(a);
					rDetails.setEditableFlag(false);
				}
			}
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				
				/*
				 * set the EDITABLE FLAG TO TRUE in LANGUAGE LIST
				 */
				if(null!=sessionBean.getRuleList() && !"".equals(sessionBean.getRuleList().size()>0))
				{
					String[] rows = sessionBean.getSelectedRows().split(",");
					if(null!=rows && rows.length>0)
					{
						for(int i=0;i<rows.length;i++)
						{
							String rowId = String.valueOf(rows[i]);
							for(int a=0;a<sessionBean.getRuleList().size();a++)
							{
								RuleDetails rDetails = (RuleDetails)sessionBean.getRuleList().get(a);
								if(rowId.equals(String.valueOf(rDetails.getRuleId())))
								{
									logger.info("editRuleDetails :: Making Row No {"+rDetails.getSrNo()+"} Editable.");
									rDetails.setEditableFlag(true);
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
				logger.info("editRuleDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.edit");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "editRuleDetails()", e);
		}
	}

	private static void deleteRuleDetails(RuleBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getSelectedRows() && !"".equals(sessionBean.getSelectedRows()))
			{
				String deleteIds = sessionBean.getSelectedRows();
				if(null!=deleteIds && !"".equals(deleteIds))
				{
					if(deleteIds.endsWith(","))
					{
						deleteIds = deleteIds.substring(0,deleteIds.length()-1);
					}
					String[] deleteTokens = deleteIds.split(",");
					deleteIds = "("+deleteIds+")";
					boolean bool = RuleDAO.deleteRuleDetails(deleteIds);
					if(bool==true)
					{
						ArrayList<RuleDetails> listForSendingNotifications=new ArrayList<RuleDetails>();
						if(null!=sessionBean.getRuleList() && sessionBean.getRuleList().size()>0)
						{
							for(int t=0;t<sessionBean.getRuleList().size();t++)
							{
								RuleDetails rDetails = (RuleDetails)sessionBean.getRuleList().get(t);
								if(null!=deleteTokens && deleteTokens.length>0)
								{
									for(int y=0;y<deleteTokens.length;y++)
									{
										if(String.valueOf(rDetails.getRuleId()).equals(deleteTokens[y]))
										{
											// add to listForSendingNotifications
											listForSendingNotifications.add(rDetails);
											break;
										}
									}
								}
							}
						}
						
						if(null!=listForSendingNotifications && listForSendingNotifications.size()>0)
						{
							/*
							 * call function to send notification email
							 */
							AlternateManualLanguageEmailGenerator.emailDataForDeleteRecord(listForSendingNotifications, wslId);
						}
						listForSendingNotifications = null;
						deleteIds= null;
						
						sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.rule")));
						/*
						 * call getCarList
						 */
						getRuleList(sessionBean);
						
					}
					else
					{
						sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.rule")));
					}
				}
				deleteIds = null;
			}
			else
			{
				logger.info("deleteRuleDetails :: No Row selected for Edit, throwing message.");
				String errorMessage = msgProps.getProperty("error.select.onerow.delete");
				sessionBean.setErrorMessage(errorMessage);
				errorMessage  =null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "deleteRuleDetails()", e);
		}
	}

	
	private static void updateRuleDetails(RuleBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getUpdatedRows() && !"".equals(sessionBean.getUpdatedRows()))
			{
				ArrayList<RuleDetails> updateDataList = new ArrayList<RuleDetails>();
				// break all fieldStrings
				String[] updatedRowsTokens=sessionBean.getUpdatedRows().split("<MDM_FS>");
				if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
				{
					if(null!=sessionBean.getRuleList() && sessionBean.getRuleList().size()>0)
					{

						for(int i=0;i<sessionBean.getRuleList().size();i++)
						{
							RuleDetails rDetails = (RuleDetails)sessionBean.getRuleList().get(i);
							if(rDetails.isEditableFlag()==true)
							{
								// set fields empty 
								rDetails.setRuleName("");
								rDetails.setRuleDesc("");
								rDetails.setFlag("");

								/*
								 * fetch the values from request
								 * and set in LanguageList
								 */
								String ruleNameId="ALT_RULE_RuleList_Name_"+String.valueOf(rDetails.getRuleId());
								String ruleDescId="ALT_RULE_RuleList_Desc_"+String.valueOf(rDetails.getRuleId());
								String flag="ALT_RULE_LangList_Flag_"+String.valueOf(rDetails.getRuleId());


								if(null!=updatedRowsTokens && updatedRowsTokens.length>0)
								{
									for(int t=0;t<updatedRowsTokens.length;t++)
									{
										String token = updatedRowsTokens[t];
										String key = token.substring(0,token.indexOf("<MDM_TS>"));
										String value = token.substring(token.indexOf("<MDM_TS>")+8, token.length());
										if(key.equals(ruleNameId))
										{
											// BREAK TOKEN STRING
											rDetails.setRuleName(value);
										}
										else if(key.equals(ruleDescId))
										{
											rDetails.setRuleDesc(value);
										}
										else if(key.equals(flag))
										{
											rDetails.setFlag(value);	
										}
										key=null;
										value=null;
										token=null;
									}
								}

								// set all request params ids to null
								ruleDescId = null;
								ruleNameId=null;
								flag= null;
							}
						}

						for(int i=0;i<sessionBean.getRuleList().size();i++)
						{
							RuleDetails rDetails = (RuleDetails)sessionBean.getRuleList().get(i);
							if(rDetails.isEditableFlag()==true)
							{
								/*
								 * add to Update List
								 * Before adding validate data for each Row.
								 * validate rDetails Object
								 */
								if(validateUpdate(rDetails, sessionBean))
								{
									/*
									 * add data to updateList
									 */
									updateDataList.add(rDetails);
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
							logger.info("updateRuleDetails :: Selected Rows Size for Update are :: >  " + updateDataList.size());
							/*
							 * Iterate UpdateList and update each row one by one.
							 */
							String errorMessage="";
							String successMessage="";
							
							ArrayList<RuleDetails> listForSendingMails = new ArrayList<RuleDetails>();
							for(int a=0;a<updateDataList.size();a++)
							{
								RuleDetails rDetails = (RuleDetails)updateDataList.get(a);
								boolean updateFlag = RuleDAO.updateRuleDetails(rDetails);
								if(updateFlag==true)
								{
									// add to list for sending notifications
									if(null!=rDetails.getFlag() && !"".equals(rDetails.getFlag()))
									{
										// set Label
										if(rDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.active")))
										{
											rDetails.setFlagLabel(msgProps.getProperty("flag.label.active"));
										}
										else if(rDetails.getFlag().equals(ApplicationProperties.getProperty("flag.value.sleep")))
										{
											rDetails.setFlagLabel(msgProps.getProperty("flag.label.sleep"));
										}
									}
									listForSendingMails.add(rDetails);
									
									logger.info("updateRuleDetails :: Rule Details updated successfully for Row No :: > " + rDetails.getSrNo());
									successMessage = successMessage+String.valueOf(rDetails.getSrNo())+",";
								}
								else
								{
									logger.info("updateRuleDetails :: Failed to Update Rule Details for Row No :: >  "+ rDetails.getSrNo());
									errorMessage = errorMessage+String.valueOf(rDetails.getSrNo())+",";	
								}
								rDetails=  null;
							}
							
							if(null!=listForSendingMails && listForSendingMails.size()>0)
							{
								/*
								 * call function to send email Notification for Update
								 */
								AlternateManualLanguageEmailGenerator.emailDataForUpdateRecord(listForSendingMails, wslId);
							}
							listForSendingMails = null;
							
							if(null!=successMessage && !"".equals(successMessage))
							{
								if(successMessage.endsWith(","))
								{
									successMessage= successMessage.substring(0,successMessage.length()-1);
								}
								successMessage = "("+successMessage+")";
								sessionBean.setSuccessMessage(msgProps.addMessage("update.success", msgProps.getProperty("label.rule"), successMessage));
							}

							if(null!=errorMessage && !"".equals(errorMessage))
							{
								if(errorMessage.endsWith(","))
								{
									errorMessage= errorMessage.substring(0,errorMessage.length()-1);
								}
								errorMessage = "("+errorMessage+")";
								sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.update", msgProps.getProperty("label.rule"), errorMessage));
							}
							if(null==errorMessage || "".equals(errorMessage))
							{
								logger.info("updateRuleDetails :: No errors reported resetting the form.");
								// reset fields
								sessionBean.setErrorMessage(null);
								sessionBean.setRuleList(null);
								sessionBean.setRuleListForCombo(null);
								sessionBean.setRuleId(null);
								sessionBean.setSelectedRows(null);
								sessionBean.setShowUpdate(false);
								sessionBean.setRuleModelsList(null);
								sessionBean.setRuleLanguageList(null);
								/*
								 * call getRuleList
								 */
								getRuleList(sessionBean);
							}
							else if(null!=errorMessage && !"".equals(errorMessage))
							{
								logger.info("updateRuleDetails :: Error found in rows :: > " + errorMessage);
								/*
								 * then only make the update fields viewable
								 */
								if(null!=sessionBean.getRuleList() && sessionBean.getRuleList().size()>0)
								{
									String tokens[] = errorMessage.split(",");
									if(null!=tokens && tokens.length>0)
									{
										for(int a=0;a<sessionBean.getRuleList().size();a++)
										{
											RuleDetails cDetails = (RuleDetails)sessionBean.getRuleList().get(a);
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
				updatedRowsTokens = null;
			}
			
			sessionBean.setUpdatedRows(null);
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "updateRuleDetails()", e);
		}
	}
	
	private static void performRuleSelectionOperation(RuleBean sessionBean)
	{
		try
		{
			sessionBean.setDefaultRule(false);
			sessionBean.setRuleModelsList(new ArrayList<RuleModelDetails>());
			sessionBean.setRuleLanguageList(new ArrayList<RuleLanguageDetails>());
			sessionBean.setWdRuleLanguageList(new ArrayList<RuleLanguageDetails>());
			if(null!=sessionBean.getRuleId() && !"".equals(sessionBean.getRuleId()))
			{
				/*
				 * ITERATE RULE LIST AND CHECK WHETHER DEFAULT RULE OR NOT
				 */
				if(null!=sessionBean.getRuleListForCombo() && sessionBean.getRuleListForCombo().size()>0)
				{
					for(int i=0;i<sessionBean.getRuleListForCombo().size();i++)
					{
						RuleDetails rDetails = (RuleDetails)sessionBean.getRuleListForCombo().get(i);
						if(String.valueOf(rDetails.getRuleId()).equals(sessionBean.getRuleId()))
						{
							if(rDetails.getRuleName().trim().toLowerCase().equals(defaultRule.trim().toLowerCase()))
							{
								sessionBean.setDefaultRule(true);
							}
							break;
						}
					}
				}
				
				/*
				 * call function to fetch mapped Model and Rule Details with Rule
				 */
				ArrayList<RuleModelDetails> modelsList = new ArrayList<RuleModelDetails>();
				ArrayList<RuleLanguageDetails> languageList = new ArrayList<RuleLanguageDetails>();
				
				modelsList= RuleDAO.getRuleModelsDetailsList(sessionBean.getRuleId());
				if(null!=modelsList && modelsList.size()>0)
				{
					sessionBean.setRuleModelsList(modelsList);
				}
				
				languageList = RuleDAO.getRuleLanguageDetailsList(sessionBean.getRuleId());
				if(null!=languageList && languageList.size()>0)
				{
					ArrayList<RuleLanguageDetails> commonList = new ArrayList<RuleLanguageDetails>();
					ArrayList<RuleLanguageDetails> wiringDiagramList = new ArrayList<RuleLanguageDetails>();
					for(int i=0;i<languageList.size();i++)
					{
						RuleLanguageDetails lDetails = (RuleLanguageDetails)languageList.get(i);
						if(null!=lDetails.getRefType() && !"".equals(lDetails.getRefType()))
						{
							if(lDetails.getRefType().equals(ApplicationProperties.getProperty("rule.language.ref.type.common")))
							{
								lDetails.setSrNo(commonList.size()+1);
								// add to commonList
								commonList.add(lDetails);
							}
							else if(lDetails.getRefType().equals(ApplicationProperties.getProperty("rule.language.ref.type.wiringdiagram")))
							{
								lDetails.setSrNo(wiringDiagramList.size()+1);
								// add to wiringDiagramList
								wiringDiagramList.add(lDetails);
								// add to commonList
								commonList.add(lDetails);
							}
						}
						lDetails= null;
					}
					
					if(null!=commonList && commonList.size()>0)
					{
						sessionBean.setRuleLanguageList(commonList);
					}
					if(null!=wiringDiagramList && wiringDiagramList.size()>0)
					{
						sessionBean.setWdRuleLanguageList(wiringDiagramList);
					}
					commonList = null;
					wiringDiagramList= null;
				}
				modelsList = null;
				languageList  = null;
			}
			else
			{
				logger.info("performRuleSelectionOperation :: Rule id in session bean is null. Cannot fetch Model and Language Mapping.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "performRuleSelectionOperation()", e);
		}
	}
	
	
	/*
	 * Function will read the models and language mapping data from 1 CSV File itself
	 */
	
	private static void readCSVData(byte[] csvData, RuleBean sessionBean, String fileName)
	{
		BufferedReader br = null;
		String line="";
		InputStream is = null;
		try
		{
			if(null!=csvData && csvData.length>0 && null!=fileName && !"".equals(fileName))
			{
				is = new ByteArrayInputStream(csvData);
				br = new BufferedReader(new InputStreamReader(is));
				Integer rowCount=0;
				String[] headerRow = null;
				String improperLines="";
				String duplicateLines="";
				while ((line = br.readLine()) != null) 
				{
					/*
					 * Check Header Row
					 * if 5 columns - then MODEL CSV
					 * if 2 columns - then LANGUAGE CSV
					 */
					if(rowCount==0)
					{
						headerRow = line.split(",");
					}
					
					/*
					 * avoid header row while reading data
					 */
					if(rowCount > 0)
					{
						// use comma as separator
						String[] csvLineData = line.split(",");
						if(null!=headerRow && headerRow.length>0)
						{
							if(headerRow.length==5)
							{
								/*
								 * MODELS CSV
								 */
								if(null!=csvLineData && csvLineData.length>0)
								{
									String carCode="";
									String wmiCode="";
									String vdsCode="";
									String visStartRange="";
									String visEndRange="";
									
									for(int w=0;w<csvLineData.length;w++)
									{
										if(w==0)
										{
											carCode= String.valueOf(csvLineData[w]).trim();
										}
										else if(w==1)
										{
											wmiCode=String.valueOf(csvLineData[w]).trim();
										}
										else if(w==2)
										{
											vdsCode = String.valueOf(csvLineData[w]).trim();
										}
										else if(w==3)
										{
											visStartRange = String.valueOf(csvLineData[w]).trim();
										}
										else if(w==4)
										{
											visEndRange = String.valueOf(csvLineData[w]).trim();
										}
									}
									
									if(null!=carCode && !"".equals(carCode) && null!=wmiCode && !"".equals(wmiCode) &&
											null!=vdsCode && !"".equals(vdsCode) && null!=visStartRange && !"".equals(visEndRange))
									{
										boolean addLine =  true;
										
										/*
										 * check if row Exists or not
										 */
										if(null!=sessionBean.getRuleModelsList() && sessionBean.getRuleModelsList().size()>0)
										{
											for(int r=0;r<sessionBean.getRuleModelsList().size();r++)
											{
												RuleModelDetails existDetails = (RuleModelDetails)sessionBean.getRuleModelsList().get(r);
												if(null!=existDetails.getCarCode() && !"".equals(existDetails.getCarCode()) && null!=carCode && !"".equals(carCode) &&
														null!=existDetails.getWmiCode() && !"".equals(existDetails.getWmiCode()) && null!=wmiCode && !"".equals(wmiCode) &&
														null!=existDetails.getVdsCode() && !"".equals(existDetails.getVdsCode()) && null!=vdsCode && !"".equals(vdsCode) &&
														null!=existDetails.getVisStartRange() && !"".equals(existDetails.getVisStartRange()) && null!=visStartRange && !"".equals(visStartRange) &&
														null!=existDetails.getVisEndRange() && !"".equals(existDetails.getVisEndRange()) && null!=visEndRange && !"".equals(visEndRange))
												{
													if(existDetails.getCarCode().equals(carCode) && existDetails.getWmiCode().equals(wmiCode) && 
															existDetails.getVdsCode().equals(vdsCode) && existDetails.getVisStartRange().equals(visStartRange) 
															&& existDetails.getVisEndRange().equals(visEndRange))
													{
														// row exists - Skip the Line.
														duplicateLines = duplicateLines+String.valueOf(rowCount)+",";
														addLine=false;
														break;
													}
												}
											}
										}
										
										if(addLine==true)
										{
											/*
											 * Proceed for Adding
											 */
											if(null==sessionBean.getRuleModelsList())
											{
												sessionBean.setRuleModelsList(new ArrayList<RuleModelDetails>());
											}
											/*
											 * add to ruleModesList
											 */
											RuleModelDetails rmd = new RuleModelDetails();
											rmd.setSrNo(sessionBean.getRuleModelsList().size()+1);
											rmd.setCarCode(carCode);
											rmd.setWmiCode(wmiCode);
											rmd.setVdsCode(vdsCode);
											rmd.setVisStartRange(visStartRange);
											rmd.setVisEndRange(visEndRange);
											sessionBean.getRuleModelsList().add(rmd);
											rmd = null;
										}
									}
									else
									{
										/*
										 * Improper Row.
										 */
										improperLines = improperLines+String.valueOf(rowCount)+",";
									}
									
									carCode=  null;
									wmiCode = null;
									vdsCode= null;
									visStartRange = null;
									visEndRange = null;
								}
							}
							else if(headerRow.length==2)
							{
								/*
								 * LANGUAGE CSV
								 * if FILE NAME STARTS WITH WD - then Wiring Diagram Language Mapping
								 * else - Common Language Mapping
								 */
								if(null!=csvLineData && csvLineData.length>0)
								{
									String fromLangId="";
									String toLangId="";
									
									for(int w=0;w<csvLineData.length;w++)
									{
										if(w==0)
										{
											fromLangId= String.valueOf(csvLineData[w]).trim();
										}
										else if(w==1)
										{
											toLangId=String.valueOf(csvLineData[w]).trim();
										}
									}
									
									if(null!=fromLangId && !"".equals(fromLangId) && null!=toLangId && !"".equals(toLangId))
									{
										if(!fileName.trim().toLowerCase().startsWith("wd"))
										{
											logger.info("readCSVData :: Processing Common Language Mapping.");
											boolean addLine =  true;
											/*
											 * check if row Exists or not
											 */
											if(null!=sessionBean.getRuleLanguageList() && sessionBean.getRuleLanguageList().size()>0)
											{
												for(int w=0;w<sessionBean.getRuleLanguageList().size();w++)
												{
													RuleLanguageDetails existLang = (RuleLanguageDetails)sessionBean.getRuleLanguageList().get(w);
													if(null!=existLang.getFromLangCode() && !"".equals(existLang.getFromLangCode()) 
															&& null!=existLang.getToLangCode() && !"".equals(existLang.getToLangCode()))
													{
														if(existLang.getFromLangCode().trim().toLowerCase().equals(fromLangId.trim().toLowerCase()) &&
																existLang.getToLangCode().trim().toLowerCase().equals(toLangId.trim().toLowerCase()))
														{
															// row exists - Skip the Line.
															duplicateLines = duplicateLines+String.valueOf(rowCount)+",";
															addLine=false;
															break;
														}
													}
												}
											}
											
											if(addLine ==true)
											{
												if(null==sessionBean.getRuleLanguageList())
												{
													sessionBean.setRuleLanguageList(new ArrayList<RuleLanguageDetails>());
												}
												
												RuleLanguageDetails rld = new RuleLanguageDetails();
												rld.setSrNo(sessionBean.getRuleLanguageList().size()+1);
												rld.setFromLangCode(fromLangId);
												/*
												 * call function to getLanguage Name on the basis of Language Code
												 */
//												String langName = ManualLanguageDAO.getManualLanguageName(rld.getFromLangCode());
//												if(null!=langName && !"".equals(langName))
//												{
//													rld.setFromLangName(langName);
//												}
//												langName = null;
												
												rld.setToLangCode(toLangId);
												/*
												 * call function to getLanguage Name on the basis of Language Code
												 */
//												langName = ManualLanguageDAO.getManualLanguageName(rld.getToLangCode());
//												if(null!=langName && !"".equals(langName))
//												{
//													rld.setToLangName(langName);
//												}
//												langName = null;
												// set refType as Common
												rld.setRefType(ApplicationProperties.getProperty("rule.language.ref.type.common"));
												sessionBean.getRuleLanguageList().add(rld);
												
												rld = null;
											}
										}
										else
										{
											logger.info("readCSVData :: Processing Wiring Diagram Language Mapping.");
											boolean addLine =  true;
											/*
											 * check if row Exists or not
											 */
											if(null!=sessionBean.getWdRuleLanguageList() && sessionBean.getWdRuleLanguageList().size()>0)
											{
												for(int w=0;w<sessionBean.getWdRuleLanguageList().size();w++)
												{
													RuleLanguageDetails existLang = (RuleLanguageDetails)sessionBean.getWdRuleLanguageList().get(w);
													if(null!=existLang.getFromLangCode() && !"".equals(existLang.getFromLangCode()) 
															&& null!=existLang.getToLangCode() && !"".equals(existLang.getToLangCode()))
													{
														if(existLang.getFromLangCode().trim().toLowerCase().equals(fromLangId.trim().toLowerCase()) &&
																existLang.getToLangCode().trim().toLowerCase().equals(toLangId.trim().toLowerCase()))
														{
															// row exists - Skip the Line.
															duplicateLines = duplicateLines+String.valueOf(rowCount)+",";
															addLine=false;
															break;
														}
													}
												}
											}
											
											if(addLine ==true)
											{
												if(null==sessionBean.getWdRuleLanguageList())
												{
													sessionBean.setWdRuleLanguageList(new ArrayList<RuleLanguageDetails>());
												}
												
												RuleLanguageDetails rld = new RuleLanguageDetails();
												rld.setSrNo(sessionBean.getWdRuleLanguageList().size()+1);
												rld.setFromLangCode(fromLangId);
												/*
												 * call function to getLanguage Name on the basis of Language Code
												 */
												String langName = ManualLanguageDAO.getManualLanguageName(rld.getFromLangCode());
												if(null!=langName && !"".equals(langName))
												{
													rld.setFromLangName(langName);
												}
												langName = null;
												
												rld.setToLangCode(toLangId);
												/*
												 * call function to getLanguage Name on the basis of Language Code
												 */
												langName = ManualLanguageDAO.getManualLanguageName(rld.getToLangCode());
												if(null!=langName && !"".equals(langName))
												{
													rld.setToLangName(langName);
												}
												langName = null;
												// set refType as Wiring Diagram
												rld.setRefType(ApplicationProperties.getProperty("rule.language.ref.type.wiringdiagram"));
												sessionBean.getWdRuleLanguageList().add(rld);
												
												rld = null;
											}
										}
									}
									else
									{
										/*
										 * Improper Row.
										 */
										improperLines = improperLines+String.valueOf(rowCount)+",";
									}
									fromLangId = null;
									toLangId= null;
								}
							}
						}
					}
					rowCount++;
				}
				
				String item="";
				if(null!=headerRow && headerRow.length>0)
				{
					if(headerRow.length==5)
					{
						item = msgProps.getProperty("label.modelmapping");
					}
					else if(headerRow.length==2)
					{
						if(!fileName.trim().toLowerCase().startsWith("wd"))
						{
							item = msgProps.getProperty("label.languagemapping");
						}
						else
						{
							item = msgProps.getProperty("label.wdlanguagemapping");
						}
						
					}
				}
				
				String errorMessage = "";
				if(null!=duplicateLines && !"".equals(duplicateLines))
				{
					if(duplicateLines.endsWith(","))
					{
						duplicateLines = duplicateLines.substring(0,duplicateLines.length()-1);
					}
					if(null!=errorMessage  && !"".equals(errorMessage))
					{
						errorMessage=errorMessage+"<MSG_TOKEN>";
					}
					errorMessage = errorMessage+msgProps.addMessage("error.csv.duplicate.lines", item, duplicateLines);
				}
				
				if(null!=improperLines && !"".equals(improperLines))
				{
					if(improperLines.endsWith(","))
					{
						improperLines = improperLines.substring(0,improperLines.length()-1);
					}
					if(null!=errorMessage  && !"".equals(errorMessage))
					{
						errorMessage=errorMessage+"<MSG_TOKEN>";
					}
					errorMessage = errorMessage+msgProps.addMessage("error.csv.improper.lines", item, improperLines);
				}
				
				if(null!=errorMessage && !"".equals(errorMessage))
				{
					sessionBean.setErrorMessage(errorMessage);
				}
				
				errorMessage = null;
				item= null;
				duplicateLines = null;
				improperLines = null;
				headerRow = null;
				rowCount = null;
			}
		}
		catch (FileNotFoundException e) 
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "readCSVData()", e);
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "readCSVData()", e);
		}
		finally 
		{
			if(null!=is)
			{
				try 
				{
					is.close();
				} 
				catch (IOException e) 
				{
					Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "readCSVData()", e);
				}
			}
			if (br != null) 
			{
				try 
				{
					br.close();
				} 
				catch (IOException e) 
				{
					Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "readCSVData()", e);
				}
			}
		}
	}
	
	private static void deleteModelMapping(RuleBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getModelSrNo() && !"".equals(sessionBean.getModelSrNo() ))
			{
				if(null!=sessionBean.getRuleModelsList() && sessionBean.getRuleModelsList().size()>0)
				{
					for(int a=0;a<sessionBean.getRuleModelsList().size();a++)
					{
						RuleModelDetails rmd = (RuleModelDetails)sessionBean.getRuleModelsList().get(a);
						if(rmd.getSrNo()==new Integer(sessionBean.getModelSrNo()).intValue())
						{
							if(null!=rmd.getRuleModelMappingId() && rmd.getRuleModelMappingId()>0)
							{
								logger.info("deleteModelMapping :: Rule Model Mapping found {"+rmd.getRuleModelMappingId()+"} Proceed for deleting from Database.");
								boolean deleteFlag = RuleDAO.deleteRuleModelMapping(rmd.getRuleModelMappingId());
								if(deleteFlag==true)
								{
									// remove from List
									sessionBean.getRuleModelsList().remove(a);
									a--;
									// show success Message
									sessionBean.setSuccessMessage(msgProps.addMessage("delete.ruledata.success", msgProps.getProperty("label.modelmapping")));
								}
								else
								{
									// show error Message
									sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete.ruledata", msgProps.getProperty("label.modelmapping")));
								}
							}
							else
							{
								logger.info("deleteModelMapping :: No Rule Model Mapping found Just delete the Index from List.");
								sessionBean.getRuleModelsList().remove(a);
								a--;
								// show success Message
								sessionBean.setSuccessMessage(msgProps.addMessage("delete.ruledata.success", msgProps.getProperty("label.modelmapping")));
							}
							break;
						}
					}
				}
			}
			else
			{
				logger.info("deleteModelMapping :: Sr No. as parameter is null.");
			}
			sessionBean.setModelSrNo(null);
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "deleteModelMapping()", e);
		}
	}

	private static void deleteLanguageMapping(RuleBean sessionBean)
	{
		try
		{
			if(null!=sessionBean.getLanguageSrNo() && !"".equals(sessionBean.getLanguageSrNo()) && 
					null!=sessionBean.getLanguageDelType() && !"".equals(sessionBean.getLanguageDelType()))
			{
				if(sessionBean.getLanguageDelType().equals("C"))
				{
					if(null!=sessionBean.getRuleLanguageList() && sessionBean.getRuleLanguageList().size()>0)
					{
						for(int a=0;a<sessionBean.getRuleLanguageList().size();a++)
						{
							RuleLanguageDetails rld = (RuleLanguageDetails)sessionBean.getRuleLanguageList().get(a);
							if(rld.getSrNo()==new Integer(sessionBean.getLanguageSrNo()).intValue())
							{
								if(null!=rld.getRuleLanguageMappingId() && rld.getRuleLanguageMappingId()>0)
								{
									logger.info("deleteLanguageMapping :: Rule Language Mapping found {"+rld.getRuleLanguageMappingId()+"} Proceed for deleting from Database.");
									boolean deleteFlag = RuleDAO.deleteRuleLanguageMapping(rld.getRuleLanguageMappingId());
									if(deleteFlag==true)
									{
										// remove from List
										sessionBean.getRuleLanguageList().remove(a);
										a--;
										// show success Message
										sessionBean.setSuccessMessage(msgProps.addMessage("delete.ruledata.success", msgProps.getProperty("label.languagemapping")));
									}
									else
									{
										// show error Message
										sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete.ruledata", msgProps.getProperty("label.languagemapping")));
									}
								}
								else
								{
									logger.info("deleteLanguageMapping :: No Rule Language Mapping found Just delete the Index from List.");
									sessionBean.getRuleLanguageList().remove(a);
									a--;
									// show success Message
									sessionBean.setSuccessMessage(msgProps.addMessage("delete.ruledata.success", msgProps.getProperty("label.languagemapping")));
								}
								break;
							}
						}
					}
				}
				else if(sessionBean.getLanguageDelType().equals("WD"))
				{
					if(null!=sessionBean.getWdRuleLanguageList() && sessionBean.getWdRuleLanguageList().size()>0)
					{
						for(int a=0;a<sessionBean.getWdRuleLanguageList().size();a++)
						{
							RuleLanguageDetails rld = (RuleLanguageDetails)sessionBean.getWdRuleLanguageList().get(a);
							if(rld.getSrNo()==new Integer(sessionBean.getLanguageSrNo()).intValue())
							{
								if(null!=rld.getRuleLanguageMappingId() && rld.getRuleLanguageMappingId()>0)
								{
									logger.info("deleteLanguageMapping :: Rule Language Mapping found {"+rld.getRuleLanguageMappingId()+"} Proceed for deleting from Database.");
									boolean deleteFlag = RuleDAO.deleteRuleLanguageMapping(rld.getRuleLanguageMappingId());
									if(deleteFlag==true)
									{
										// remove from List
										sessionBean.getWdRuleLanguageList().remove(a);
										a--;
										// show success Message
										sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.languagemapping")));
									}
									else
									{
										// show error Message
										sessionBean.setErrorMessage(msgProps.addMessage("error.message.operation.delete", msgProps.getProperty("label.languagemapping")));
									}
								}
								else
								{
									logger.info("deleteLanguageMapping :: No Rule Language Mapping found Just delete the Index from List.");
									sessionBean.getWdRuleLanguageList().remove(a);
									a--;
									// show success Message
									sessionBean.setSuccessMessage(msgProps.addMessage("delete.success", msgProps.getProperty("label.languagemapping")));
								}
								break;
							}
						}
					}
				}
			}
			else
			{
				logger.info("deleteLanguageMapping :: Sr No. and Del Type as parameter are null.");
			}
			sessionBean.setLanguageSrNo(null);
			sessionBean.setLanguageDelType(null);
			
			if(null!=sessionBean.getRuleLanguageList() && sessionBean.getRuleLanguageList().size()>0)
			{
				for(int a=0;a<sessionBean.getRuleLanguageList().size();a++)
				{
					RuleLanguageDetails rlDetails = (RuleLanguageDetails)sessionBean.getRuleLanguageList().get(a);
					rlDetails.setSrNo(a+1);
				}
			}
			
			if(null!=sessionBean.getWdRuleLanguageList() && sessionBean.getWdRuleLanguageList().size()>0)
			{
				for(int a=0;a<sessionBean.getWdRuleLanguageList().size();a++)
				{
					RuleLanguageDetails rlDetails = (RuleLanguageDetails)sessionBean.getWdRuleLanguageList().get(a);
					rlDetails.setSrNo(a+1);
				}
			}
			
			
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "deleteLanguageMapping()", e);
		}
	}
	
	private static void saveMapping(RuleBean sessionBean)
	{
		try
		{
			if(validaeMapping(sessionBean))
			{
				String errorMessage="";
				String successMessage="";
				if(null!=sessionBean.getRuleModelsList() && sessionBean.getRuleModelsList().size()>0)
				{
					boolean saveModelMapping=RuleDAO.updateModelMappingDetails(sessionBean.getRuleId(), sessionBean.getRuleModelsList());
					if(saveModelMapping==true)
					{
						successMessage = msgProps.addMessage("entry.success", msgProps.getProperty("label.modelmapping"));
					}
					else
					{
						errorMessage = msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.modelmapping"));
					}
				}
				
				if(null!=sessionBean.getRuleLanguageList() && sessionBean.getRuleLanguageList().size()>0)
				{
					boolean saveLanguageMapping = RuleDAO.updateLanguageMappingDetails(sessionBean.getRuleId(), sessionBean.getRuleLanguageList());
					if(saveLanguageMapping==true)
					{
						if(null!=successMessage && !"".equals(successMessage))
						{
							successMessage = successMessage+"<MSG_TOKEN>";
						}
						successMessage = successMessage+ msgProps.addMessage("entry.success", msgProps.getProperty("label.languagemapping"));
					}
					else
					{
						if(null!=errorMessage && !"".equals(errorMessage))
						{
							errorMessage = errorMessage+"<MSG_TOKEN>";
						}	
						errorMessage =errorMessage+ msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.languagemapping"));
					}
				}
				

				if(null!=sessionBean.getWdRuleLanguageList() && sessionBean.getWdRuleLanguageList().size()>0)
				{
					boolean saveLanguageMapping = RuleDAO.updateLanguageMappingDetails(sessionBean.getRuleId(), sessionBean.getWdRuleLanguageList());
					if(saveLanguageMapping==true)
					{
						if(null!=successMessage && !"".equals(successMessage))
						{
							successMessage = successMessage+"<MSG_TOKEN>";
						}
						successMessage = successMessage+ msgProps.addMessage("entry.success", msgProps.getProperty("label.wdlanguagemapping"));
					}
					else
					{
						if(null!=errorMessage && !"".equals(errorMessage))
						{
							errorMessage = errorMessage+"<MSG_TOKEN>";
						}	
						errorMessage =errorMessage+ msgProps.addMessage("error.message.operation.save", msgProps.getProperty("label.wdlanguagemapping"));
					}
				}
				
				
				if(null!=errorMessage  && !"".equals(errorMessage))
				{
					sessionBean.setErrorMessage(errorMessage);
				}
				if(null!=successMessage && !"".equals(successMessage))
				{
					sessionBean.setSuccessMessage(successMessage);
				}
				successMessage = null;
				errorMessage= null;
			}
		}
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(AlternateManualLanguage.class.getName(), "saveMapping()", e);
		}
	}

	private static boolean validaeMapping(RuleBean sessionBean)
	{
		if(null==sessionBean.getRuleId() || "".equals(sessionBean.getRuleId()))
		{
			sessionBean.setErrorMessage(msgProps.getProperty("error.mandatory.fields"));
			return false;
		}
		if((null==sessionBean.getRuleLanguageList() || sessionBean.getRuleLanguageList().size()<=0) && 
				(null==sessionBean.getRuleModelsList() || sessionBean.getRuleModelsList().size()<=0))
		{
			sessionBean.setErrorMessage(msgProps.getProperty("error.modellanguage.mapping"));
			return false;
		}
		return true;
	}
}
