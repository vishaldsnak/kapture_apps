package com.webapp.mazda.assetstool.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.webapp.mazda.assetstool.bean.ManageAssetsBean;
import com.webapp.mazda.assetstool.logging.LogManager;
import com.webapp.mazda.assetstool.logging.Logger;
import com.webapp.mazda.assetstool.util.ApplicationPropertiesUtil;
import com.webapp.mazda.assetstool.vo.SelectItemDetails;

/**
 * Servlet implementation class ManageAssets
 */
public class ManageAssets extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static Logger logger = LogManager.getLogger(ManageAssets.class);

	static String wslId = null;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ManageAssets() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		boolean useReqDis = true;
		try {
			// Get WSL ID From request Header and set in Variable
			wslId = request.getHeader("iv-user");
			if (null == wslId || "".equals(wslId)) {
				/*
				 * re direct to Error
				 */
				response.sendRedirect(request.getContextPath() + "/error");
				useReqDis = false;
			}
			if (null != wslId && !"".equals(wslId)) {
				ManageAssetsBean sessionBean = getSessionBean(request);
				/*
				 * call reset operation
				 */
				resetOperation(sessionBean, request);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("doget :: Exception :: > " + e.getMessage());
		}
		if (useReqDis == true) {
			RequestDispatcher rs = request
					.getRequestDispatcher("OKAssetsUpload.jsp");
			rs.forward(request, response);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// Get WSL ID From request Header and set in Variable
		wslId = request.getHeader("iv-user");
		if (null != wslId && !"".equals(wslId)) {
			try {

				ManageAssetsBean sessionBean = getSessionBean(request);
				sessionBean.setInfoMessage(null);
				sessionBean.setErrorMessage(null);

				/*
				 * call function to read Parameters from request
				 */
				performFieldsValuesRetrievalAndUploadImageOperation(request,
						sessionBean);

				if (null != sessionBean.getActionClicked()
						&& !"".equals(sessionBean.getActionClicked())) {
					if (sessionBean.getActionClicked().equals("GET_LOCALES")) {
						// set uploadedFileList to null
						sessionBean.setUploadedFileList(null);
						/*
						 * call getLocale Function
						 */
						getLocalesList(sessionBean);
					} else if (sessionBean.getActionClicked().equals("RESET")) {
						/*
						 * call reset operation
						 */
						resetOperation(sessionBean, request);
					} else if (sessionBean.getActionClicked().equals(
							"OVERWRITE")) {
						/*
						 * call overWrite Function
						 */
						performOverWriteOpertion(sessionBean, request);
					} else if (sessionBean.getActionClicked().equals("DISCARD")) {
						/*
						 * call discard function
						 */
						performDiscardOpertion(sessionBean, request);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			RequestDispatcher rs = request
					.getRequestDispatcher("OKAssetsUpload.jsp");
			rs.forward(request, response);
		} else {
			/*
			 * re direct to Error
			 */
			response.sendRedirect(request.getContextPath() + "/error");
		}

	}

	private ManageAssetsBean getSessionBean(HttpServletRequest request) {
		ManageAssetsBean sessionBean = null;
		if (null != request.getSession().getAttribute("manageAssetsBean")
				&& !"".equals(request.getSession().getAttribute(
						"manageAssetsBean"))) {
			sessionBean = (ManageAssetsBean) request.getSession().getAttribute(
					"manageAssetsBean");
		} else {
			// initialize the sessionBean and set it in HTTP Session
			sessionBean = new ManageAssetsBean();
			request.getSession().setAttribute("manageAssetsBean", sessionBean);
		}
		return sessionBean;
	}

	private static void resetOperation(ManageAssetsBean sessionBean,
			HttpServletRequest request) {
		sessionBean.setInfoMessage(null);
		sessionBean.setErrorMessage(null);
		sessionBean.setChannelName(null);
		sessionBean.setChannelsList(null);
		sessionBean.setLocaleList(null);
		sessionBean.setLocaleName(null);
		sessionBean.setActionClicked(null);
		sessionBean.setShowLocale(false);
		sessionBean.setUploadedFileList(null);
		sessionBean.setSelectedRows(null);
		sessionBean.setDisplayPageLength(null);
		sessionBean.setDisplayPageNo(null);

		/*
		 * added for UI FLOW 2
		 */
		sessionBean.setAcceptableFilesTypesForSelectedChannel(null);

		// remove progressBar attribute
		request.getSession().removeAttribute("testProgressListener");

		/*
		 * call function to get ChannelsList
		 */
		getChannelsList(sessionBean);
	}

	private static void performOverWriteOpertion(ManageAssetsBean sessionBean,
			HttpServletRequest request) {
		try {
			if (null != sessionBean.getSelectedRows()
					&& !"".equals(sessionBean.getSelectedRows())) {
				String[] selectedData = sessionBean.getSelectedRows()
						.split(",");
				if (null != selectedData && selectedData.length > 0) {
					if (null != sessionBean.getUploadedFileList()
							&& sessionBean.getUploadedFileList().size() > 0) {
						for (int i = 0; i < sessionBean.getUploadedFileList()
								.size(); i++) {
							Map<Object, Object> dataMap = (Map<Object, Object>) sessionBean
									.getUploadedFileList().get(i);
							if (null != dataMap
									&& null != dataMap.get("FILE_NAME")) {
								for (int a = 0; a < selectedData.length; a++) {
									String fileNameToCheck = selectedData[a];
									if (null != fileNameToCheck
											&& !"".equals(fileNameToCheck)) {
										if (fileNameToCheck
												.trim()
												.toLowerCase()
												.equals(dataMap
														.get("FILE_NAME")
														.toString().trim()
														.toLowerCase())) {
											boolean status = false;
											/*
											 * OVER WRITE THIS FILE.
											 */
											if (null != dataMap
													.get("UPLOADED_PATH")
													&& null != dataMap
															.get("FILE_DATA")) {
												String filePath = dataMap.get(
														"UPLOADED_PATH")
														.toString();
												byte[] data = (byte[]) dataMap
														.get("FILE_DATA");
												if (null != data
														&& data.length > 0) {
													try {
														File upFile = new File(
																filePath);
														FileOutputStream fos = new FileOutputStream(
																upFile);
														fos.write(data);
														fos.flush();
														fos.close();
													} catch (Exception e) {
														e.printStackTrace();
													}
													/*
													 * NOW CHECK FILE WHETHER
													 * EXISTS OR NOT
													 */
													File checkFile = new File(
															filePath);
													if (checkFile.exists()
															&& checkFile
																	.isFile()) {
														status = true;
													}
													checkFile = null;
												}
												data = null;
												filePath = null;
											}

											if (status == true) {
												dataMap.put("MESSAGE_COLOR",
														"green");
											} else {
												dataMap.put("MESSAGE_COLOR",
														"red");
												dataMap.put("UPLOADED_PATH",
														"Failed to upload file.");
											}
											break;
										}
									}
									fileNameToCheck = null;
								}
							}
						}
					}
				}
			} else {
				sessionBean
						.setErrorMessage("Please select atleast 1 row to overwrite.");
			}

			sessionBean.setSelectedRows(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void performDiscardOpertion(ManageAssetsBean sessionBean,
			HttpServletRequest request) {
		try {
			if (null != sessionBean.getSelectedRows()
					&& !"".equals(sessionBean.getSelectedRows())) {
				String[] selectedData = sessionBean.getSelectedRows()
						.split(",");
				if (null != selectedData && selectedData.length > 0) {
					if (null != sessionBean.getUploadedFileList()
							&& sessionBean.getUploadedFileList().size() > 0) {
						for (int i = 0; i < sessionBean.getUploadedFileList()
								.size(); i++) {
							Map<Object, Object> dataMap = (Map<Object, Object>) sessionBean
									.getUploadedFileList().get(i);
							if (null != dataMap
									&& null != dataMap.get("FILE_NAME")) {
								for (int a = 0; a < selectedData.length; a++) {
									String fileNameToCheck = selectedData[a];
									if (null != fileNameToCheck
											&& !"".equals(fileNameToCheck)) {
										if (fileNameToCheck
												.trim()
												.toLowerCase()
												.equals(dataMap
														.get("FILE_NAME")
														.toString().trim()
														.toLowerCase())) {
											// remove the Item from List.
											sessionBean.getUploadedFileList()
													.remove(i);
											i--;
											break;
										}
									}
									fileNameToCheck = null;
								}
							}
						}
					}
				}
			}
			sessionBean.setSelectedRows(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function will get all the channelsList available
	 * 
	 * @param sessionBean
	 */
	private static void getChannelsList(ManageAssetsBean sessionBean) {
		ArrayList<SelectItemDetails> channelsList = new ArrayList<SelectItemDetails>();
		try {
			sessionBean.setChannelsList(new ArrayList<SelectItemDetails>());
			/*
			 * add the channels from the properties file
			 * 
			 * Accessory=Accessories Training=Training TSB=Technical Service
			 * Bulletins M-TIPS= Mazda Tips SSP_Recall=SSP Recalls / Campaigns
			 * Data_Files=Data_Files / Immobilizer Group_Message=Group Message
			 * Service_Alerts=Service Alerts Static_Content=Static Content
			 * Videos=videos
			 */
			SelectItemDetails si = new SelectItemDetails();
			si.setLabel(ApplicationPropertiesUtil.getProperty("Accessory"));
			si.setValue(ApplicationPropertiesUtil.getProperty("Accessory"));
			channelsList.add(si);
			si = null;

			si = new SelectItemDetails();
			si.setLabel(ApplicationPropertiesUtil.getProperty("Group_Message"));
			si.setValue(ApplicationPropertiesUtil.getProperty("Group_Message"));
			channelsList.add(si);
			si = null;

			si = new SelectItemDetails();
			si.setLabel(ApplicationPropertiesUtil
					.getProperty("Service_Information"));
			si.setValue(ApplicationPropertiesUtil
					.getProperty("Service_Information"));
			channelsList.add(si);
			si = null;

			si = new SelectItemDetails();
			si.setLabel(ApplicationPropertiesUtil
					.getProperty("Service_Manuals"));
			si.setValue(ApplicationPropertiesUtil
					.getProperty("Service_Manuals"));
			channelsList.add(si);
			si = null;

			si = new SelectItemDetails();
			si.setLabel(ApplicationPropertiesUtil.getProperty("Static_Content"));
			si.setValue(ApplicationPropertiesUtil.getProperty("Static_Content"));
			channelsList.add(si);
			si = null;

			si = new SelectItemDetails();
			si.setLabel("Videos");
			si.setValue(ApplicationPropertiesUtil.getProperty("Videos"));
			channelsList.add(si);
			si = null;

			if (null != channelsList && channelsList.size() > 0) {
				sessionBean.setChannelsList(channelsList);
			}
			channelsList = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Function will get all the localesList available for a channel
	 * 
	 * @param sessionBean
	 * @return
	 */
	private static void getLocalesList(ManageAssetsBean sessionBean) {
		try {
			sessionBean.setLocaleList(new ArrayList<SelectItemDetails>());
			sessionBean.setShowLocale(false);
			/*
			 * added to UI Flow 2
			 */
			sessionBean.setAcceptableFilesTypesForSelectedChannel(null);
			if (null != sessionBean.getChannelName()
					&& !"".equals(sessionBean.getChannelName())) {

				/*
				 * added for UI Flow 2
				 */
				sessionBean
						.setAcceptableFilesTypesForSelectedChannel(identifyAllowedFileTypesForSelectedChannel(sessionBean
								.getChannelName()));

				/*
				 * get all the locales folders inside the channel which will be
				 * at path - okAssets path + channelName
				 */
				String path = ApplicationPropertiesUtil
						.getProperty("OK_ASSETS_PATH");
				if (null != path && !"".equals(path)) {
					if (!path.endsWith("/")) {
						path = path + "/";
					}
					String channelName = sessionBean.getChannelName();
					channelName = channelName.replace(" ", "_");
					channelName = channelName.toUpperCase();
					// add channelName
					path = path + channelName;
					channelName = null;

					// now check whether channelName exists or not
					File channelDir = new File(path);
					if (channelDir.exists()) {
						if (channelDir.isDirectory()) {
							File[] localesFoldersList = channelDir.listFiles();
							if (null != localesFoldersList
									&& localesFoldersList.length > 0) {
								for (int a = 0; a < localesFoldersList.length; a++) {
									File locale = localesFoldersList[a];
									if (locale.exists() && locale.isDirectory()) {
										// add to localesList
										SelectItemDetails si = new SelectItemDetails();
										si.setLabel(locale.getName());
										si.setValue(locale.getName());
										sessionBean.getLocaleList().add(si);
										si = null;
									}
									locale = null;
								}
							}
							localesFoldersList = null;
						}
					}
					channelDir = null;
				}
				path = null;

				if (!sessionBean.getChannelName().equals(
						ApplicationPropertiesUtil.getProperty("Group_Message"))) {
					// set showLocale to TRUE
					sessionBean.setShowLocale(true);
				}

				if (!sessionBean.getChannelName().equals(
						ApplicationPropertiesUtil.getProperty("Group_Message"))) {
					if (null == sessionBean.getLocaleList()
							|| sessionBean.getLocaleList().size() <= 0) {
						// set error Message, no Locales found for the Selected
						// Channel
						sessionBean
								.setErrorMessage("No Locales found for the Seleected Channel.");
					}
				}
			} else {
				logger.info("getLocalesList :: Channel Name is passed as null in parameter.");
				// set error message - please fill all the fields marked as
				// mandatory (*).
				// sessionBean.setErrorMessage("Please fill all the fields marked as mandatory (*).");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void performFieldsValuesRetrievalAndUploadImageOperation(
			HttpServletRequest request, ManageAssetsBean sessionBean) {
		if (logger.isInfoEnabled())
			logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: Method Starts.");
		try {
			// set all the values in sessionBean to null
			sessionBean.setChannelName(null);
			sessionBean.setLocaleName(null);
			sessionBean.setActionClicked(null);
			sessionBean.setSelectedRows(null);
			sessionBean.setDisplayPageLength(null);
			sessionBean.setDisplayPageNo(null);

			ArrayList<Map<Object, Object>> uploadedFileList = new ArrayList<Map<Object, Object>>();

			boolean showError = false;
			String errorFileNames = "";
			String successFileNames = "";
			String failureFileNames = "";
			String invalidFileNames = "";
			String validFileMessage = "";

			// Create FileItemFactory instance
			DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();
			// By using fileItemFactory instance get the ServletFileUpload
			// object
			// as
			ServletFileUpload servletFileUpload = new ServletFileUpload(
					fileItemFactory);
			TestProgressListener testProgressListener = new TestProgressListener();
			servletFileUpload.setProgressListener(testProgressListener);

			HttpSession session = request.getSession();
			session.setAttribute("testProgressListener", testProgressListener);

			// Now get the list of all files by parsing the request
			@SuppressWarnings("unchecked")
			List<FileItem> fileItems = servletFileUpload.parseRequest(request);
			// iterate fileItems and check for the Image File
			Iterator<FileItem> iterator = fileItems.iterator();
			logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: iterating File Items");
			while (iterator.hasNext()) {
				FileItem fileItem = iterator.next();
				if (!fileItem.isFormField()) {
					logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: Field is File; Proceed for Checking whether any File is Uploaded or Not.");
					if (null != sessionBean.getActionClicked()
							&& !"".equals(sessionBean.getActionClicked())
							&& sessionBean.getActionClicked().equals("UPLOAD")) {
						if (validate(sessionBean)) {
							// EMPTY THE EXISTING FILE.
							sessionBean.setUploadedFileList(null);

							String fileName = fileItem.getName();
							if (null != fileName && !"".equals(fileName)) {
								/*
								 * THERE COULD BE A POSSIBILITY THAT SOME
								 * BROWSERS MAY RETURN COMPLETE PATH IN FILE
								 * NAME SO EXTRACT THE EXACT FILE NAME
								 */
								fileName = fileName.replace("\\", "/");
								if (fileName.lastIndexOf("/") != -1) {
									fileName = fileName.substring(
											fileName.lastIndexOf("/") + 1,
											fileName.length());
								}
							}
							byte[] data = fileItem.get();
							// check image file name is not null
							if (null != fileName && !"".equals(fileName)
									&& null != data) {
								if (data.length > 0) {
									logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: File Name is not Null. Check for the extension of the File and Check Whether valid image or not.");
									String extension = fileName.substring(
											fileName.lastIndexOf("."),
											fileName.length());
									logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: File Extension :: = > "
											+ extension);
									/*
									 * now check here the selected Channel Type
									 * and on the basis of it check for allowed
									 * File Types, if the file is in allowed
									 * types, proceed else throw message - now
									 * check if extension is in any of the
									 * supported acceptable then only proceed;
									 * else prompt please upload a valid image.
									 */
									if (null != extension
											&& !"".equals(extension)) {
										String allowedFileTypesForChannels = identifyAllowedFileTypesForSelectedChannel(sessionBean
												.getChannelName());
										boolean proceedForWriting = false;
										String folderTypeToProcess = "";

										// Check the file whether allowed for
										// channel or not
										if (null != allowedFileTypesForChannels
												&& !"".equals(allowedFileTypesForChannels)) {
											StringTokenizer str = new StringTokenizer(
													allowedFileTypesForChannels,
													",");
											while (str.hasMoreTokens()) {
												Object token = str.nextToken();
												if (null != token
														&& !"".equals(token)) {
													if (token
															.toString()
															.trim()
															.toLowerCase()
															.equals(extension
																	.trim()
																	.toLowerCase())) {
														proceedForWriting = true;
														/*
														 * Identify the
														 * folderTypeToProcess
														 * on the basis of
														 * extension
														 */

														folderTypeToProcess = identifyFolderTypeOnTheBasisOfExtension(
																extension,
																sessionBean
																		.getChannelName());
														break;
													}
												}

												token = null;
											}
											str = null;
										}
										allowedFileTypesForChannels = null;

										/*
										 * check if proceedForWiting is TRUE -
										 * then proceed for writing file
										 */
										if (proceedForWriting == true
												&& null != data
												&& null != folderTypeToProcess
												&& !"".equals(folderTypeToProcess)) {
											logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: File is an acceptable Image. Proceed for Writing it.");
											/*
											 * Here, check the fileTypeToProcess
											 * and then prepare the path of the
											 * directories accordingly.
											 */
											String uploadPath = ApplicationPropertiesUtil
													.getProperty("OK_ASSETS_PATH");
											// add channel Name and localeName
											// to
											// the
											// uploadPath
											if (null != uploadPath
													&& !"".equals(uploadPath)) {
												if (!uploadPath.endsWith("/")) {
													uploadPath = uploadPath
															+ "/";
												}
											}

											/*
											 * check for uploadPath, it exists
											 * or not if does not exists, then
											 * create
											 */
											File uploadDir = new File(
													uploadPath);
											if (!uploadDir.exists()
													|| !uploadDir.isDirectory()) {
												// create uploadDir
												uploadDir.mkdir();
											}
											uploadDir = null;
											// now add channelName
											/*
											 * FOR ADDING CHANNEL NAME REPLACE
											 * ALL SPACES BY _ TO MAKE REF KEYS
											 * AND IN UPPER CASE
											 */
											String channelName = sessionBean
													.getChannelName();
											channelName = channelName.replace(
													" ", "_");
											channelName = channelName
													.toUpperCase();
											uploadPath = uploadPath
													+ channelName;
											channelName = null;
											/*
											 * check for uploadPath with
											 * channelName, exists or not if
											 * not, then create
											 */
											uploadDir = new File(uploadPath);
											if (!uploadDir.exists()
													|| !uploadDir.isDirectory()) {
												// create uploadDir
												uploadDir.mkdir();
											}
											uploadDir = null;

											/*
											 * LOCALE NAME only when the Channel
											 * is not Group Message & Data_Files
											 */
											if (!sessionBean
													.getChannelName()
													.trim()
													.toLowerCase()
													.equals(ApplicationPropertiesUtil
															.getProperty(
																	"Group_Message")
															.trim()
															.toLowerCase())) {
												// now add localeName
												uploadPath = uploadPath
														+ "/"
														+ sessionBean
																.getLocaleName();
												/*
												 * check for uploadPath with
												 * localeName, exists or not if
												 * not, then create
												 */
												uploadDir = new File(uploadPath);
												if (!uploadDir.exists()
														|| !uploadDir
																.isDirectory()) {
													// create uploadDir
													uploadDir.mkdir();
												}
												uploadDir = null;
											}

											/*
											 * now add the fileTypeToProcess
											 * folderName and check it exists or
											 * not, if not then create it
											 */
											// now add fileTypeToProcess
											uploadPath = uploadPath + "/"
													+ folderTypeToProcess;
											/*
											 * check for uploadPath with
											 * fileTypeToProcess, exists or not
											 * if not, then create
											 */
											uploadDir = new File(uploadPath);
											if (!uploadDir.exists()
													|| !uploadDir.isDirectory()) {
												// create uploadDir
												uploadDir.mkdir();
											}
											uploadDir = null;

											/*
											 * Now add the, file Name to upload
											 * it to the uploadPath
											 */
											uploadPath = uploadPath + "/"
													+ fileName;

											/*
											 * BEFORE POCEEDING HERE, CHECK IF
											 * FILE ALREADY EXISTS AT THAT
											 * LOCATION DO NOT UPLOAD MARK IT AS
											 * OVER WRITE FILE
											 */
											boolean proceedForCreation = true;
											File upFile = new File(uploadPath);
											if (upFile.exists()
													&& upFile.isFile()) {
												proceedForCreation = false;
											}

											if (proceedForCreation == true) {
												logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: Image Path :: > "
														+ uploadPath);
												logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: Image Name :: > "
														+ fileName);
												FileOutputStream fos = new FileOutputStream(
														upFile);
												fos.write(data);
												fos.close();
												fos.flush();
												logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: File as Image Written Successfully.");
												boolean status = false;
												try {
													/*
													 * now check whether the
													 * file is written or not
													 */
													File checkFile = new File(
															uploadPath);
													if (checkFile.exists()
															&& checkFile
																	.isFile()) {
														status = true;
														/*
														 * add to show Uploaded
														 * File list
														 */
														Map<Object, Object> dataMap = new HashMap<Object, Object>();
														dataMap.put(
																"FILE_NAME",
																fileName);
														if (null != uploadPath
																&& !"".equals(uploadPath)) {
															uploadPath = uploadPath
																	.replace(
																			"/",
																			"\\");
														}
														dataMap.put(
																"UPLOADED_PATH",
																uploadPath);

														/*
														 * added for UI FLOW 2,
														 * Remove it to restore
														 * for FLOW 1
														 * MESSAGE_COLOR (NOT
														 * REQUIRED FOR FLOW 1)
														 */
														dataMap.put(
																"MESSAGE_COLOR",
																"green");
														if (null == uploadedFileList
																|| uploadedFileList
																		.size() <= 0) {
															uploadedFileList = new ArrayList<Map<Object, Object>>();
														}
														// add dataMap to
														// uploadedFileList
														uploadedFileList
																.add(dataMap);
														dataMap = null;
													}
													checkFile = null;
												} catch (Exception e) {
													e.printStackTrace();
												}

												if (status == true) {
													logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: File Uploaded Successfully.");

													/*
													 * Now, check here if file
													 * is PDF, then it may be
													 * mapped to a document as
													 * well so search for the
													 * pdf file in database on
													 * the basis of PDF Name and
													 * channelName get the
													 * resourcePath for the
													 * file, if found, then
													 * upload the PDF File there
													 * as well
													 */
													// if
													// (fileTypeToProcess.equals("pdf"))
													// {
													// String resourcePath =
													// ManageAssetsDAO
													// .getResourcePath(fileName);
													// if (null != resourcePath
													// &&
													// !"".equals(resourcePath))
													// {
													// // add
													// SERVER_RESOURCES_PATH
													// // to
													// // the fetched
													// resourcePath
													// File resFile = new File(
													// ApplicationPropertiesUtil
													// .getProperty("SERVER_RESOURCES_PATH")
													// + resourcePath
													// + fileName);
													// FileOutputStream resFos =
													// new
													// FileOutputStream(
													// resFile);
													// resFos.write(data);
													// resFos.close();
													// resFos.flush();
													// resFos = null;
													// resFile = null;
													// }
													// resourcePath = null;
													// }

													if (null != successFileNames
															&& !"".equals(successFileNames)) {
														successFileNames = successFileNames
																+ "  ,  "
																+ fileName;
													} else {
														successFileNames = fileName;
													}
													// int count = 0;
													if (null != successFileNames
															&& !"".equals(successFileNames)) {
														String comma[] = successFileNames
																.split(",");
														if (null != comma
																&& comma.length > 0) {
															// count =
															// comma.length;
														}
														comma = null;
													}
													// sessionBean.setInfoMessage("Files {  "
													// + successFileNames+
													// "  } Uploaded Successfully.");

													/*
													 * COMMENTING FOR UI FLOW 2
													 * - No need to show files
													 * count, and Success
													 * Message Uncomment to
													 * restore for UI FLOW 1
													 */
													// sessionBean.setInfoMessage(""+count+" Files Uploaded Successfully. Click <a href=\"javascript:void(0);\" onclick=\"ms3_showUploadedFilesDialog();\">here</a> to view details.");
												} else {
													if (null != failureFileNames
															&& !"".equals(failureFileNames)) {
														failureFileNames = failureFileNames
																+ ", "
																+ fileName;
													} else {
														failureFileNames = fileName;
													}
													showError = true;
													// sessionBean.setErrorMessage("Failed to upload files {  "+
													// failureFileNames+
													// "  }.");
													// session.removeAttribute("testProgressListener");

													/*
													 * Adding for UI FLOW 2, ADD
													 * THE FAILURE FILE NAMES TO
													 * UPLOADED FILE LIST In
													 * order to restore to UI
													 * FLOW 1 - Remove this part
													 * of adding to
													 * uploadedFileList add to
													 * show Uploaded File list
													 */
													Map<Object, Object> dataMap = new HashMap<Object, Object>();
													dataMap.put("FILE_NAME",
															fileName);
													if (null != uploadPath
															&& !"".equals(uploadPath)) {
														uploadPath = uploadPath
																.replace("/",
																		"\\");
													}
													dataMap.put(
															"UPLOADED_PATH",
															"Failed to upload file.");

													/*
													 * added for UI FLOW 2,
													 * Remove it to restore for
													 * FLOW 1 MESSAGE_COLOR (NOT
													 * REQUIRED FOR FLOW 1)
													 */
													dataMap.put(
															"MESSAGE_COLOR",
															"red");
													if (null == uploadedFileList
															|| uploadedFileList
																	.size() <= 0) {
														uploadedFileList = new ArrayList<Map<Object, Object>>();
													}
													// add dataMap to
													// uploadedFileList
													uploadedFileList
															.add(dataMap);
													dataMap = null;

												}
												// set all used variables to
												// null
												uploadPath = null;
												upFile = null;
												fos = null;
											} else {
												/*
												 * add to show Uploaded File
												 * list
												 */
												Map<Object, Object> dataMap = new HashMap<Object, Object>();
												dataMap.put("FILE_NAME",
														fileName);
												if (null != uploadPath
														&& !"".equals(uploadPath)) {
													uploadPath = uploadPath
															.replace("/", "\\");
												}
												dataMap.put("UPLOADED_PATH",
														uploadPath);

												if (null != data
														&& data.length > 0) {
													dataMap.put("FILE_DATA",
															data);
												}
												/*
												 * added for UI FLOW 2, Remove
												 * it to restore for FLOW 1
												 * MESSAGE_COLOR (NOT REQUIRED
												 * FOR FLOW 1)
												 */
												dataMap.put("MESSAGE_COLOR",
														"orange");
												if (null == uploadedFileList
														|| uploadedFileList
																.size() <= 0) {
													uploadedFileList = new ArrayList<Map<Object, Object>>();
												}
												// add dataMap to
												// uploadedFileList
												uploadedFileList.add(dataMap);
												dataMap = null;
											}
										} else {
											if (null != errorFileNames
													&& !"".equals(errorFileNames)) {
												// add fileName to it
												errorFileNames = errorFileNames
														+ ", " + fileName;
											} else {
												errorFileNames = fileName;
											}
											logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: Files is Not an acceptable file {"
													+ errorFileNames
													+ "} . Prompt Error Message.");
											showError = true;
											// sessionBean.setErrorMessage("File(s) {  "+
											// errorFileNames +
											// "  } not acceptable for the selected channel.");
											// session.removeAttribute("testProgressListener");
											// break the while loop
											// break;

											/*
											 * Adding for UI FLOW 2, ADD THE
											 * FAILURE FILE NAMES TO UPLOADED
											 * FILE LIST In order to restore to
											 * UI FLOW 1 - Remove this part of
											 * adding to uploadedFileList add to
											 * show Uploaded File list
											 */
											Map<Object, Object> dataMap = new HashMap<Object, Object>();
											dataMap.put("FILE_NAME", fileName);
											dataMap.put("UPLOADED_PATH",
													"File is not acceptable for the selected channel.");

											/*
											 * added for UI FLOW 2, Remove it to
											 * restore for FLOW 1 MESSAGE_COLOR
											 * (NOT REQUIRED FOR FLOW 1)
											 */
											dataMap.put("MESSAGE_COLOR", "red");
											if (null == uploadedFileList
													|| uploadedFileList.size() <= 0) {
												uploadedFileList = new ArrayList<Map<Object, Object>>();
											}
											// add dataMap to uploadedFileList
											uploadedFileList.add(dataMap);
											dataMap = null;
										}
										folderTypeToProcess = null;
									}
									// set extension to null
									extension = null;
								} else {
									if (null != invalidFileNames
											&& !"".equals(invalidFileNames)) {
										// add fileName to it
										invalidFileNames = invalidFileNames
												+ ", " + fileName;
									} else {
										invalidFileNames = fileName;
									}

									logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: Files are invalid files {"
											+ invalidFileNames
											+ "} . Prompt Error Message.");
									showError = true;
									// sessionBean.setErrorMessage("File(s) {  "+
									// invalidFileNames +
									// "  } "+verb+" invalid. The size is 0 MB.");

									/*
									 * Adding for UI FLOW 2, ADD THE FAILURE
									 * FILE NAMES TO UPLOADED FILE LIST In order
									 * to restore to UI FLOW 1 - Remove this
									 * part of adding to uploadedFileList add to
									 * show Uploaded File list
									 */
									Map<Object, Object> dataMap = new HashMap<Object, Object>();
									dataMap.put("FILE_NAME", fileName);
									dataMap.put("UPLOADED_PATH",
											"File is invalid. The size is 0 MB.");

									/*
									 * added for UI FLOW 2, Remove it to restore
									 * for FLOW 1 MESSAGE_COLOR (NOT REQUIRED
									 * FOR FLOW 1)
									 */
									dataMap.put("MESSAGE_COLOR", "red");
									if (null == uploadedFileList
											|| uploadedFileList.size() <= 0) {
										uploadedFileList = new ArrayList<Map<Object, Object>>();
									}
									// add dataMap to uploadedFileList
									uploadedFileList.add(dataMap);
									dataMap = null;
								}
							} else {
								logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: No File Selected To upload . Prompt Error Message.");
								showError = true;
								validFileMessage = "Please fill all the fields marked as mandatory (*).";
								// validFileMessage =
								// "A corrupt file has been tried to upload. The file cannot be identified.";
								// validFileMessage =
								// "Please select a file to upload.";
								// sessionBean.setErrorMessage("Please select a file to upload.");

							}
							// set fileName and data to null
							fileName = null;
							data = null;
						}
					}

					// session.removeAttribute("testProgressListener");
				} else {
					logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: When Fields are Not Form Fields. Check each File Name and set the values accordingly in each attribute.");
					String fieldName = fileItem.getFieldName();

					if (null != fieldName && !"".equals(fieldName)) {
						if (fieldName.equals("MS3_Channel_Name")) {
							// set the value in sessionBean.setChannel
							String value = fileItem.getString();
							if (null != value && !"".equals(value)
									&& !"''".equals(value)
									&& !"\"\"".equals(value)
									&& !"null".equals(value)) {
								sessionBean.setChannelName(value);
							}
							// set value to null
							value = null;

						}
						if (fieldName.equals("MS3_Locale_Name")) {
							// set the value in sessionBean.setLocale
							String value = fileItem.getString();
							if (null != value && !"".equals(value)
									&& !"''".equals(value)
									&& !"\"\"".equals(value)
									&& !"null".equals(value)) {
								sessionBean.setLocaleName(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("MS3_SelectedRows")) {
							// set the value in sessionBean.setSelectedRows
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value)
									&& !"''".equals(value)
									&& !"\"\"".equals(value)
									&& !"null".equals(value)) {
								sessionBean.setSelectedRows(value);
							}
							// set value to null
							value = null;
						}

						/*
						 * set displayPageNo and displayPageLenght
						 */
						if (fieldName.equals("MS3_DataTabel_displayPageNo")) {
							// set the value in sessionBean.displayPageNo
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value)
									&& !"''".equals(value)
									&& !"\"\"".equals(value)
									&& !"null".equals(value)) {
								sessionBean.setDisplayPageNo(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("MS3_DataTabel_displayPageLen")) {
							// set the value in sessionBean.displayPageLenght
							String value = fileItem.getString("UTF-8");
							if (null != value && !"".equals(value)
									&& !"''".equals(value)
									&& !"\"\"".equals(value)
									&& !"null".equals(value)) {
								sessionBean.setDisplayPageLength(value);
							}
							// set value to null
							value = null;
						}

						if (fieldName.equals("MS3_Action_Clicked")) {
							sessionBean.setActionClicked(fileItem.getString());
						}

						logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: Channel Name :: >"
								+ sessionBean.getChannelName());
						logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: Locale :: >"
								+ sessionBean.getLocaleName());
					}
					// set fieldName
					fieldName = null;
				}

				// set fileItem to null
				fileItem = null;
			}

			// set all the other used variables to null
			iterator = null;
			fileItems = null;
			servletFileUpload = null;
			fileItemFactory = null;

			if (showError == true) {
				String message = "";

				/*
				 * 
				 * COMMENTING THE ADDITION OF EFFOR FILE NAMES FAILURE FILE
				 * NAMES INVALID FILE NAMES to ERROR MESSAGE IN SESSIONBEAN FOR
				 * UI FLOW 2 to restore to UI FLOW 1, Uncomment this section.
				 * 
				 * if(null!=errorFileNames && !"".equals(errorFileNames)) {
				 * if(null!=message && !"".equals(message)) { message =
				 * message+"<br>"; } message=message+"File(s) {  "+
				 * errorFileNames +
				 * "  } not acceptable for the selected channel."; }
				 * 
				 * if(null!=failureFileNames && !"".equals(failureFileNames)) {
				 * if(null!=message && !"".equals(message)) { message =
				 * message+"<br>"; }
				 * message=message+"Failed to upload files {  "+
				 * failureFileNames+ "  }."; }
				 * 
				 * if(null!=invalidFileNames && !"".equals(invalidFileNames)) {
				 * String[] commas = invalidFileNames.split(","); String
				 * verb="are"; if(null!=commas && commas.length>0) {
				 * if(commas.length==1) { verb = "is"; } }
				 * 
				 * if(null!=message && !"".equals(message)) { message =
				 * message+"<br>"; } message = message+"File(s) {  "+
				 * invalidFileNames + "  } "+verb+" invalid. The size is 0 MB.";
				 * 
				 * verb = null; commas= null; }
				 */

				if (null != validFileMessage && !"".equals(validFileMessage)) {
					if (null != message && !"".equals(message)) {
						message = message + "<br>";
					}
					message = message + validFileMessage;
				}

				if (null != message && !"".equals(message)) {
					sessionBean.setErrorMessage(message);
				}
				message = null;
			}

			if (null != uploadedFileList && uploadedFileList.size() > 0) {
				sessionBean.setUploadedFileList(uploadedFileList);
			}

			uploadedFileList = null;
			errorFileNames = null;
			successFileNames = null;
			failureFileNames = null;
			invalidFileNames = null;
			validFileMessage = null;
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: Exception :: >"
					+ e.getMessage());
			sessionBean
					.setErrorMessage("Unable to process request at the moment; please try after sometime.");
		}
		logger.info("performFieldsValuesRetrievalAndUploadImageOperation() :: Method Ends.");
	}

	/**
	 * Identify the allowed file types on the basis of selected channel
	 * 
	 * @param channelName
	 * @return
	 */
	private static String identifyAllowedFileTypesForSelectedChannel(
			String channelName) {
		logger.info("identifyAllowedFileTypesForSelectedChannel :: Method Starts :: Identify Allowed File Types for Channel {"
				+ channelName + "}.");
		String supportedFileTypesForChannel = "";
		if (null != channelName && !"".equals(channelName)) {
			if (channelName
					.trim()
					.toLowerCase()
					.equals(ApplicationPropertiesUtil.getProperty("Accessory")
							.trim().toLowerCase())) {
				supportedFileTypesForChannel = ApplicationPropertiesUtil
						.getProperty("CHANNEL_FILE_TYPES_ACCESSORY");
			} else if (channelName
					.trim()
					.toLowerCase()
					.equals(ApplicationPropertiesUtil
							.getProperty("Service_Information").trim()
							.toLowerCase())) {
				supportedFileTypesForChannel = ApplicationPropertiesUtil
						.getProperty("CHANNEL_FILE_TYPES_SERVICE_INFORMATION");
			} else if (channelName
					.trim()
					.toLowerCase()
					.equals(ApplicationPropertiesUtil
							.getProperty("Service_Manuals").trim()
							.toLowerCase())) {
				supportedFileTypesForChannel = ApplicationPropertiesUtil
						.getProperty("CHANNEL_FILE_TYPES_SERVICE_MANUALS");
			} else if (channelName
					.trim()
					.toLowerCase()
					.equals(ApplicationPropertiesUtil
							.getProperty("Group_Message").trim().toLowerCase())) {
				supportedFileTypesForChannel = ApplicationPropertiesUtil
						.getProperty("CHANNEL_FILE_TYPES_GROUP_MESSAGE");
			} else if (channelName
					.trim()
					.toLowerCase()
					.equals(ApplicationPropertiesUtil
							.getProperty("Static_Content").trim().toLowerCase())) {
				supportedFileTypesForChannel = ApplicationPropertiesUtil
						.getProperty("CHANNEL_FILE_TYPES_STATIC_CONTENT");
			} else if (channelName
					.trim()
					.toLowerCase()
					.equals(ApplicationPropertiesUtil.getProperty("Videos")
							.trim().toLowerCase())) {
				supportedFileTypesForChannel = ApplicationPropertiesUtil
						.getProperty("CHANNEL_FILE_TYPES_VIDEOS");
			}
		}
		logger.info("identifyAllowedFileTypesForSelectedChannel :: Method Ends :: Identified Supported File Types are :: > "
				+ supportedFileTypesForChannel);
		return supportedFileTypesForChannel;
	}

	/**
	 * Function will validate the form Fields.
	 * 
	 * @param sessionBean
	 * @return
	 */
	private static boolean validate(ManageAssetsBean sessionBean) {
		if (null == sessionBean.getChannelName()
				|| "".equals(sessionBean.getChannelName())) {
			sessionBean
					.setErrorMessage("Please fill all the fields marked as mandatory (*).");
			return false;
		}
		if (null != sessionBean.getChannelName()
				&& !"".equals(sessionBean.getChannelName())) {
			if (!sessionBean
					.getChannelName()
					.trim()
					.toLowerCase()
					.equals(ApplicationPropertiesUtil
							.getProperty("Group_Message").trim().toLowerCase())) {
				if (null == sessionBean.getLocaleName()
						|| "".equals(sessionBean.getLocaleName())) {
					sessionBean
							.setErrorMessage("Please fill all the fields marked as mandatory (*).");
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Function will identify the folder name to be used for processing file
	 * channelName is used to identify the image folder name if Group Message -
	 * then images, else image
	 * 
	 * @param extension
	 * @param channelName
	 * @return
	 */
	private static String identifyFolderTypeOnTheBasisOfExtension(
			String extension, String channelName) {
		logger.info("identifyFolderTypeOnTheBasisOfExtension :: Method Starts :: Identify Folder Type for Extension {"
				+ extension + "} And Channel {" + channelName + "}.");
		String folderType = "";
		// image
		if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_IMAGES").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			if (channelName
					.trim()
					.toLowerCase()
					.equals(ApplicationPropertiesUtil
							.getProperty("Group_Message").trim().toLowerCase())) {
				folderType = ApplicationPropertiesUtil
						.getProperty("FOLDER_NAME_IMAGES");
			} else {
				folderType = ApplicationPropertiesUtil
						.getProperty("FOLDER_NAME_IMAGE");
			}
		}
		// video
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_VIDEOS")
				.trim().toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_VIDEO");
		}
		// doc
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_DOC").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_DOC");
		}
		// docx
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_DOCX").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_DOCX");
		}
		// xls
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_XLS").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_XLS");
		}
		// xlsx
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_XLSX").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_XLSX");
		}
		// pdf
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_PDF").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_PDF");
		}
		// exe
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_EXE").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_EXE");
		}
		// zip
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_ZIP").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_ZIP");
		}
		// xml
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_XML").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_XML");
		}
		// css
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_CSS").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_CSS");
		}
		// js
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_JS").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_JS");
		}
		// cab
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_CAB").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_CAB");
		}
		// up
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_UP").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_UP");
		}
		// dat
		else if (ApplicationPropertiesUtil.getProperty("FILE_TYPE_DAT").trim()
				.toLowerCase().contains(extension.trim().toLowerCase())) {
			folderType = ApplicationPropertiesUtil
					.getProperty("FOLDER_NAME_DAT");
		}
		logger.info("identifyFolderTypeOnTheBasisOfExtension :: Method Ends :: Identified Folder Type is :: > "
				+ folderType);
		return folderType;
	}
}
