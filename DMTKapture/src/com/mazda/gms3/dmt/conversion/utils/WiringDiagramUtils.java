package com.mazda.gms3.dmt.conversion.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mazda.gms3.dmt.conversion.dao.MNAODocumentManagementDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.vo.WindowJSDetails;
import com.mazda.gms3.dmt.mme.dao.MMEDocumentManagementDAO;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.LinkDetails;
import com.mazda.gms3.dmt.vo.SelectItemDetails;

public class WiringDiagramUtils {

	private Logger logger = LogManager.getLogger(WiringDiagramUtils.class);

	private ArrayList<WindowJSDetails> windowJSList = new ArrayList<WindowJSDetails>();
	
	private ArrayList<WindowJSDetails> voltageMapJSList = new ArrayList<WindowJSDetails>();
	 
	private ArrayList<WindowJSDetails> voltageMapLinkJSList = new ArrayList<WindowJSDetails>();
	
	public ArrayList<WindowJSDetails> getVoltageMapLinkJSList() {
		return voltageMapLinkJSList;
	}

	public void setVoltageMapLinkJSList(ArrayList<WindowJSDetails> voltageMapLinkJSList) {
		this.voltageMapLinkJSList = voltageMapLinkJSList;
	}

	public ArrayList<WindowJSDetails> getWindowJSList() {
		return windowJSList;
	}

	public void setWindowJSList(ArrayList<WindowJSDetails> windowJSList) {
		this.windowJSList = windowJSList;
	}

	public ArrayList<WindowJSDetails> getVoltageMapJSList() {
		return voltageMapJSList;
	}

	public void setVoltageMapJSList(ArrayList<WindowJSDetails> voltageMapJSList) {
		this.voltageMapJSList = voltageMapJSList;
	}

	/**
	 * FUNCTION will update all the PATHS in the window.js and update the js
	 * file
	 * 
	 * @param windowsJSFile
	 * @param tempDestinationPathForJS
	 */
	public  String performWindowsJSOperation_BackUp(File windowsJSFile,
			String tempDestinationPathForJS) {
		String jsContent = "";
		try {
			/*
			 * CHECK HERE IF tempDestinationPathForJS DOES NOT STARTS WITH /
			 * THEN ADD
			 */
			if (null != tempDestinationPathForJS
					&& !"".equals(tempDestinationPathForJS)) {
				if (!tempDestinationPathForJS.startsWith("/")) {
					tempDestinationPathForJS = "/" + tempDestinationPathForJS;
				}
			}
			jsContent = ConversionUtils.getStringFromXML(windowsJSFile);
			if (null != jsContent && !"".equals(jsContent)) {
				// jsContent.indexOf("window.open(\"");
				int startingIndex = 0;
				int endingIndex = 0;

				String aTagString = "";
				while (startingIndex < jsContent.length()) {
					// search for <a tag
					startingIndex = jsContent.indexOf("window.open(\"",
							endingIndex);
					if (startingIndex == -1 || startingIndex < 0) {
						break;
					} else {
						// search for > tag
						endingIndex = jsContent.indexOf("\"",
								startingIndex + 13);

						aTagString = jsContent.substring(startingIndex,
								endingIndex);
						if (null != aTagString && !"".equals(aTagString)) {
							String newaTagString = "";
							/*
							 * here aTagString will be -
							 * window.open("../0922_1a.html
							 */
							if (aTagString.indexOf("\"") != -1) {
								String before = aTagString.substring(0,
										aTagString.indexOf("\"") + 1);
								String after = aTagString.substring(
										aTagString.indexOf("\"") + 1,
										aTagString.length());
								if (null != after && !"".equals(after)) {
									if (after.startsWith("../")) {
										// then add the path to html folder only
										// , remove ../ from the aTagString
										after = after.replace("../", "");
										after = tempDestinationPathForJS
												+ after;
									} else if (after
											.startsWith(ApplicationProperties
													.getProperty("directory.conn"))) {
										// for conn/ab.html
										after = after
												.replace(
														ApplicationProperties
																.getProperty("directory.conn"),
														tempDestinationPathForJS
																+ ApplicationProperties
																		.getProperty("directory.conn"));
									} else {
										// for only htmls
										after = tempDestinationPathForJS
												+ after;
									}
								}

								// update newaTagString
								newaTagString = before + after;
								before = null;
								after = null;
							}
							if (null != newaTagString
									&& !"".equals(newaTagString)) {
								jsContent = jsContent.replace(aTagString,
										newaTagString);
							}
							newaTagString = null;
						}
						aTagString = null;

						startingIndex = startingIndex + 13;
						endingIndex = startingIndex;
					}
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),
					"performWindowsJSOperation()", e);
		}
		return jsContent;
	}

	public  String performWindowsJSOperation(File windowsJSFile,
			String tempDestinationPathForJS) {
		String jsContent = "";
		try {
			/*
			 * CHECK HERE IF tempDestinationPathForJS DOES NOT STARTS WITH /
			 * THEN ADD
			 */
			if (null != tempDestinationPathForJS
					&& !"".equals(tempDestinationPathForJS)) {
				if (!tempDestinationPathForJS.startsWith("/")) {
					tempDestinationPathForJS = "/" + tempDestinationPathForJS;
				}
			}
			jsContent = ConversionUtils.getStringFromXML(windowsJSFile);
			if (null != jsContent && !"".equals(jsContent)) {
				/*
				 * FIRST REPLACE window.open("../ BY window.open(" Then REPLACE
				 * window.open(" + /dokweb+tempDestinationPathForJS
				 * 
				 * 12TH OCTOBER CHANGE = REMOVE /dokweb
				 */
//				String valueToBeUpdated = "window.open(\""
//						+ ApplicationProperties.getProperty("GMS3_INFOCENTER_WEB_CONTEXT")+tempDestinationPathForJS;
				String valueToBeUpdated = "window.open(\""
						+ tempDestinationPathForJS;
				jsContent = jsContent.replace("window.open(\"../",
						"window.open(\"");
				jsContent = jsContent.replace("window.open(\"",
						valueToBeUpdated);
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),
					"performWindowsJSOperation()", e);
		}
		return jsContent;
	}

	/**
	 * window.js (MC, already copied into OKAssets): every quoted "/library/..." address of an html or
	 * pdf file it opens becomes "/content/library/..." (okassets.web.context), the address the browser
	 * reaches OKAssets at. Relative addresses need nothing. Already prefixed addresses are left alone,
	 * so a file processed twice is unchanged.
	 * The file is rewritten byte for byte (ISO-8859-1 in and out) - window.js is not UTF-8 and only
	 * ASCII is replaced, so its Japanese text is kept exactly.
	 *
	 * @return the number of addresses changed (-1 when the file could not be updated)
	 */
	public int prefixOkAssetsContextInWindowJS(File windowsJSFile)
	{
		try
		{
			java.nio.charset.Charset bytes = java.nio.charset.StandardCharsets.ISO_8859_1;
			String jsContent = new String(java.nio.file.Files.readAllBytes(windowsJSFile.toPath()), bytes);
			String library = "/" + ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY").trim().replace("\\", "/");
			if (!library.endsWith("/"))
			{
				library = library + "/";
			}
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("([\"'])" + java.util.regex.Pattern.quote(library)).matcher(jsContent);
			String replacement = java.util.regex.Matcher.quoteReplacement(com.mazda.gms3.dmt.utils.OkAssetsWeb.context() + library);
			StringBuffer sb = new StringBuffer(jsContent.length() + 4096);
			int changed = 0;
			while (m.find())
			{
				m.appendReplacement(sb, "$1" + replacement);
				changed++;
			}
			m.appendTail(sb);
			if (changed > 0)
			{
				java.nio.file.Files.write(windowsJSFile.toPath(), sb.toString().getBytes(bytes));
			}
			logger.info("prefixOkAssetsContextInWindowJS :: " + PathUtil.winPath(windowsJSFile) + " :: addresses prefixed with "
					+ com.mazda.gms3.dmt.utils.OkAssetsWeb.context() + " :: > " + changed);
			return changed;
		}
		catch (Exception e)
		{
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(), "prefixOkAssetsContextInWindowJS()", e);
			return -1;
		}
	}

	public  String performWindowsJSOperationForMC(File windowsJSFile,String tempDestinationPathForJS, String locale, ContentDetails contentDetails)
	{
		String jsContent = "";
//		try {
//			
//			/*
//			 * IDENTIFY IF MME LOCALE OR MC LOCALE
//			 */
//			// replace - by _ in locale
//			locale=locale.replace("-", "_");
//			boolean mcLocale=false;
//			if(null!=locale && locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("ja-jp").trim().toLowerCase()))
//			{
//				// locale is ja_JP
//				mcLocale = true;
//			}
//			// replace _ in locale by -
//			locale=locale.replace("_", "-");
//			/*
//			 * CHECK HERE IF tempDestinationPathForJS DOES NOT STARTS WITH /
//			 * THEN ADD
//			 */
//			if (null != tempDestinationPathForJS
//					&& !"".equals(tempDestinationPathForJS)) {
//				if (!tempDestinationPathForJS.startsWith("/")) {
//					tempDestinationPathForJS = "/" + tempDestinationPathForJS;
//				}
//			}
//			
//			jsContent = ConversionUtils.getStringFromXML(windowsJSFile);
//			if (null != jsContent && !"".equals(jsContent)) {
//				// jsContent.indexOf("window.open(\"");
//				int startingIndex = 0;
//				int endingIndex = 0;
//
//				String aTagString = "";
//				while (startingIndex < jsContent.length()) {
//					// search for <a tag
//					startingIndex = jsContent.indexOf("window.open(\"",
//							endingIndex);
//					if (startingIndex == -1 || startingIndex < 0) {
//						break;
//					} else {
//						// search for > tag
//						endingIndex = jsContent.indexOf("\"",
//								startingIndex + 13);
//						aTagString = jsContent.substring(startingIndex + 13,
//								endingIndex);
//						if (null != aTagString && !"".equals(aTagString)) {
//							// PROCEED ONLY WHEN PATH ENDS WITH - .ent/
//							if (aTagString
//									.trim()
//									.toLowerCase()
//									.endsWith(
//											ApplicationProperties
//													.getProperty("extension.ent"))) {
//								if (aTagString.startsWith("/")) {
//									aTagString = aTagString.substring(1,
//											aTagString.length());
//								}
//								/*
//								 * prepare New URL - Append Locale to the Path
//								 * Identify Model Type for the Path
//								 */
//								String modelType = "";
//								if (aTagString
//										.trim()
//										.toLowerCase()
//										.startsWith(
//												ApplicationProperties
//														.getProperty("model.type.new"))) {
//									modelType = ApplicationProperties
//											.getProperty("model.type.new");
//								} else if (aTagString
//										.trim()
//										.toLowerCase()
//										.startsWith(
//												ApplicationProperties
//														.getProperty("model.type.old"))) {
//									modelType = ApplicationProperties
//											.getProperty("model.type.old");
//								}
//
//								// add Locale to aTag String
//								String filePath = locale + "/" + aTagString;
//								filePath = filePath.replace("/", "\\");
//
//								String infoAppContext = ApplicationProperties
//										.getProperty("GMS3_INFOCENTER_APPLICATION_CONTENT");
//								// add InfoCenter Application Context as well.
//								String linkURL = "";
//								String imDocumentId="";
//								/*
//								 * PREPARE WINDOW JS DETAILS OBJECT AND START ADDING DETAILS TO IT
//								 */
//								WindowJSDetails jsDetails = new WindowJSDetails();
//								jsDetails.setFilePath(PathUtil.winPath(windowsJSFile));
//								jsDetails.setFileName(windowsJSFile.getName());
//								jsDetails.setInnerLinkPath(filePath);
//								// by DEFAULT FAILURE
//								jsDetails.setProcessingStatus("FAILURE");
//								jsDetails.setContentDetails(contentDetails);
//								if(mcLocale==true)
//								{
//									// MC LOCALE
//									MCDocumentManagementDAO mcDao = new MCDocumentManagementDAO();
//									imDocumentId = mcDao
//											.getDocumentDetailsForInnerLink(
//													filePath, modelType);
//									mcDao = null;
//									linkURL = infoAppContext
//											+ ApplicationProperties
//													.getProperty("LINK_URL_MC_WINDOW_JS");
//								}
//								else
//								{
//									// MME LOCALE
//									MMEDocumentManagementDAO mmeDao = new MMEDocumentManagementDAO();
//									imDocumentId = mmeDao.getDocumentDetailsForInnerLink(
//											filePath, modelType,locale);
//									mmeDao=  null;
//									linkURL = infoAppContext
//											+ ApplicationProperties
//													.getProperty("LINK_URL_MME_WINDOW_JS");
//								}
//								if (null != imDocumentId
//										&& !"".equals(imDocumentId)) {
//									linkURL = linkURL + imDocumentId;
//									// set processingStatus as SUCCESS
//									jsDetails.setProcessingStatus("SUCCESS");
//									jsDetails.setInnerLinkDocumentId(imDocumentId);
//									jsDetails.setPreapredInnerLinkURLForIC(linkURL);
//								}
//								else
//								{
//									// set processingStatus as FAILURE
//									jsDetails.setProcessingStatus("FAILURE");
//									jsDetails.setPreapredInnerLinkURLForIC(linkURL);
//								}
//								imDocumentId = null;
//								
//								/*
//								 * add jsDetails to windowJSList
//								 */
//								if(null==windowJSList || windowJSList.size()<=0)
//								{
//									windowJSList = new ArrayList<WindowJSDetails>();
//								}
//								windowJSList.add(jsDetails);
//								jsDetails=null;
//
//								// REPLACE THE ATAG STRING WITH LINK URL
//								jsContent = jsContent.replace(aTagString,
//										linkURL);
//								filePath = null;
//								modelType = null;
//								infoAppContext = null;
//							}
//						}
//						aTagString = null;
//
//						startingIndex = startingIndex + 13;
//						endingIndex = startingIndex;
//					}
//				}
//
//				/*
//				 * LOOK FOR THE SECTION WINDOW.OPEN IF IT CONTAINS -
//				 * WINDOW.OPEN("../, REPLACE IT BY WINDOW.OPEN(" IF IT CONTAINS
//				 * - WINDOW.OPEN("./, REPLACE IT BY WINDOW.OPEN(" ELSE IF IF IT
//				 * CONTAINS - WINDOW.OPEN("/, DO NOTHING
//				 */
//				/*
//				 * FIRST REPLACE
//				 * window.open("../ BY window.open("+/dokweb+tempDestinationPathForJS
//				 * THEN REPLACE window.open("./ BY window.open(" +
//				 * tempDestinationPathForJS Then REPLACE window.open(" +
//				 * tempDestinationPathForJS
//				 * 
//				 * 12TH OCTOBER CHANGE = REMOVE /dokweb
//				 */
////				String valueToBeUpdated = "window.open(\""
////						+ ApplicationProperties.getProperty("GMS3_INFOCENTER_WEB_CONTEXT")+tempDestinationPathForJS;
//				String valueToBeUpdated = "window.open(\""
//						+ tempDestinationPathForJS;
//				jsContent = jsContent.replace("window.open(\"../",
//						valueToBeUpdated);
//				jsContent = jsContent.replace("window.open(\"./",
//						valueToBeUpdated);
//
//				// safe side replace /html/pdf/ by /pdf/ because tempJSPath has
//				// /html already appended to it.
//				jsContent = jsContent.replace("/html/pdf/", "/pdf/");
//
//				valueToBeUpdated = null;
//			}
//		} catch (Exception e) {
//			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),
//					"performWindowsJSOperationForMC()", e);
//		}
		return jsContent;
	}

	public  String performVoltageMapJSOperation(File voltageMapJSFile,String locale, ContentDetails contentDetails) 
	{
		String jsContent = "";
		// MNAO market (new FL content layout): links resolve against gms3_dmt_<locale>_imdoc
		if(isMNAOMarket(contentDetails))
		{
			return performVoltageMapJSOperationForMNAO(voltageMapJSFile, locale, contentDetails);
		}
		try 
		{
			/*
			 * IDENTIFY IF MME LOCALE OR MC LOCALE
			 */
			// replace - by _ in locale
			locale=locale.replace("-", "_");
			boolean mcLocale=false;
			if(null!=locale && locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("ja-jp").trim().toLowerCase()))
			{
				// locale is ja_JP
				mcLocale = true;
			}
			// replace _ in locale by -
			locale=locale.replace("_", "-");
			
			jsContent = ConversionUtils.getStringFromXML(voltageMapJSFile);
			if (null != jsContent && !"".equals(jsContent)) 
			{
				// jsContent.indexOf("window.open(\"");
				int startingIndex = 0;
				int endingIndex = 0;

				String aTagString = "";
				String modelType=null;
				String filePath = null;
				List<String> uniqueInnerLinksPath = null;
				boolean addInnerLink = true;
				WindowJSDetails jsDetails = null;
				while (startingIndex < jsContent.length()) 
				{
					// search for <a tag
					startingIndex = jsContent.indexOf(":'",	endingIndex);
					if (startingIndex == -1 || startingIndex < 0) 
					{
						break;
					} else 
					{
						// search for ' tag
						endingIndex = jsContent.indexOf("'",startingIndex + 2);
						aTagString = jsContent.substring(startingIndex + 2,	endingIndex);
						if (null != aTagString && !"".equals(aTagString)) 
						{
							// PROCEED ONLY WHEN PATH ENDS WITH - .ent/
							if (aTagString.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.ent"))) 
							{
								if (aTagString.startsWith("/")) 
								{
									aTagString = aTagString.substring(1,aTagString.length());
								}
								/*
								 * prepare New URL - Append Locale to the Path
								 * Identify Model Type for the Path
								 */
								if(aTagString.trim().toLowerCase().startsWith(ApplicationProperties.getProperty("model.type.new"))) 
								{
									modelType = ApplicationProperties.getProperty("model.type.new");
								} 
								else if (aTagString.trim().toLowerCase().startsWith(ApplicationProperties.getProperty("model.type.old")))
								{
									modelType = ApplicationProperties.getProperty("model.type.old");
								}

								// add Locale to aTag String
								filePath = locale + "/" + aTagString;
								filePath = filePath.replace("/", "\\");
								
								/*
								 * add this FilePath to unique Inner Link List
								 */
								addInnerLink = true;
								if(null!=uniqueInnerLinksPath && uniqueInnerLinksPath.size()>0)
								{
									for(int r=0;r<uniqueInnerLinksPath.size();r++)
									{
										if(uniqueInnerLinksPath.get(r).trim().toLowerCase().equals(filePath.trim().toLowerCase()))
										{
											// already added
											addInnerLink = false;
											 break;
										}
									}
								}
								
								if(addInnerLink==true)
								{
									if(null==uniqueInnerLinksPath || uniqueInnerLinksPath.size()<=0)
									{
										uniqueInnerLinksPath = new ArrayList<String>();
									}
									uniqueInnerLinksPath.add(filePath);
								}

								/*
								 * PREPARE WINDOW JS DETAILS OBJECT AND START ADDING DETAILS TO IT
								 */
								jsDetails = new WindowJSDetails();
								jsDetails.setFilePath(PathUtil.winPath(voltageMapJSFile));
								jsDetails.setFileName(voltageMapJSFile.getName());
								jsDetails.setInnerLinkPath(filePath);
								jsDetails.setaTagString(aTagString);
								// by DEFAULT FAILURE
								jsDetails.setProcessingStatus("FAILURE");
								jsDetails.setContentDetails(contentDetails);
								
								/*
								 * ADD THESE LINKS TO VOLTAGE MAP JS LIST
								 */
								if(null==voltageMapJSList || voltageMapJSList.size()<=0)
								{
									voltageMapJSList = new ArrayList<WindowJSDetails>();
								}
								voltageMapJSList.add(jsDetails);
								jsDetails=null;
								filePath = null;
								// DO NOT SET MODEL TYPE AS NULL
							}
						}
						aTagString = null;

						startingIndex = startingIndex + 2;
						endingIndex = startingIndex;
					}
				}
				
				/*
				 * NOW VEIRFY IF UNIQUE INNER LINKS ARE FOUND = FETCH DOCUMENT IDS FOR THEM
				 */
				
				// set default as MME Market
				String market = ApplicationProperties.getProperty("market.mme").trim().toLowerCase();
				// set linkURL as default for MME - ADD INFOCENTER CONTEXT AS WELL
				String linkURL = ApplicationProperties.getProperty("GMS3_INFOCENTER_APPLICATION_CONTENT")+ ApplicationProperties.getProperty("LINK_URL_MME_WINDOW_JS");
				
				if(mcLocale==true)
				{
					// Japan Locale - set Market as MC
					market = ApplicationProperties.getProperty("market.mc").trim().toLowerCase();
					// set linkURL for MC Market since Japan Locale - ADD INFOCENTER CONTEXT AS WELL
					linkURL = ApplicationProperties.getProperty("GMS3_INFOCENTER_APPLICATION_CONTENT")+ ApplicationProperties.getProperty("LINK_URL_MC_WINDOW_JS");
				}
				
				if(null!=uniqueInnerLinksPath && uniqueInnerLinksPath.size()>0)
				{
					logger.info("performVoltageMapJSOperation :: Total Unique Inner Links Found are :: >" + uniqueInnerLinksPath.size());
					/*
					 * FETCH INNERLINKS DOCUMENT IDS
					 */
					List<SelectItemDetails> innerLinksList = MMEDocumentManagementDAO.getDocumentDetailsForInnerLink(uniqueInnerLinksPath, modelType, locale, market);
					if(null!=innerLinksList && innerLinksList.size()>0 && null!=voltageMapJSList && voltageMapJSList.size()>0)
					{
						SelectItemDetails itemDetails = null;
						jsDetails=  null;
						/*
						 * iterate innerLinks List and update the document Ids against each of the innerLink
						 */
						for(int b=0;b<voltageMapJSList.size();b++)
						{
							jsDetails=  (WindowJSDetails)voltageMapJSList.get(b);
							if(null!=jsDetails.getInnerLinkPath() && !"".equals(jsDetails.getInnerLinkPath()))
							{
								itemDetails = null;
								for(int a=0;a<innerLinksList.size();a++)
								{
									itemDetails= (SelectItemDetails)innerLinksList.get(a);
									if(null!=itemDetails.getValue() && !"".equals(itemDetails.getValue()))
									{
										if(jsDetails.getInnerLinkPath().trim().toLowerCase().equals(itemDetails.getValue().trim().toLowerCase()))
										{
											// set document Id in voltageList
											jsDetails.setInnerLinkDocumentId(itemDetails.getLabel());
											break;
										}
									}
									itemDetails = null;
								}
								itemDetails = null;
							}
							jsDetails=  null;
						}
						jsDetails = null;
					}
					innerLinksList = null;
				}
				else
				{
					logger.info("performVoltageMapJSOperation ::  No Unique Inner Links Found for JS File :: >"+ voltageMapJSFile.getName());
				}
				uniqueInnerLinksPath = null;
				
				
				/*
				 * NOW SINCE ALL DOCUMENT IDS ARE IDENTIFIED - PREPARE INNER LINKS FOR REPLACEMENT AND
				 * UPDATE IN VOLTAGE MAP LIST AS WELL AS JS CONTENT
				 */
				if(null!=voltageMapJSList && voltageMapJSList.size()>0)
				{
					jsDetails=  null;
					for(int b=0;b<voltageMapJSList.size();b++)
					{
						jsDetails=  (WindowJSDetails)voltageMapJSList.get(b);
						if(null!=jsDetails.getInnerLinkDocumentId() && !"".equals(jsDetails.getInnerLinkDocumentId()))
						{
							// set processingStatus as SUCCESS
							jsDetails.setProcessingStatus("SUCCESS");
							jsDetails.setInnerLinkDocumentId(jsDetails.getInnerLinkDocumentId());
							jsDetails.setPreapredInnerLinkURLForIC(linkURL+jsDetails.getInnerLinkDocumentId());
						}
						else
						{
							// set processingStatus as FAILURE
							jsDetails.setProcessingStatus("FAILURE");
							jsDetails.setPreapredInnerLinkURLForIC(linkURL);
						}
						
						/*
						 * REPLACE INNER LINK PATH WITH JS CONTENT
						 */
						// REPLACE THE SOURCE STRING (ATAGSTRING) WITH LINK URL
						if(null!=jsContent && !"".equals(jsContent))
						{
							if(null!=jsDetails.getaTagString())
							{
								jsContent = jsContent.replace(jsDetails.getaTagString(),jsDetails.getPreapredInnerLinkURLForIC());
							}
						}
						jsDetails=  null;
					}
				}
				jsDetails= null;
				market=  null;
				linkURL=  null;
			}
		} 
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),"performVoltageMapJSOperation()", e);
		}
		return jsContent;
	}

	public  String performVoltageMapLinkJSOperation(File voltageMapLinkJSFile,String locale, ContentDetails contentDetails) 
	{
		String jsContent = "";
		// MNAO market (new FL content layout): links resolve against gms3_dmt_<locale>_imdoc
		if(isMNAOMarket(contentDetails))
		{
			return performVoltageMapLinkJSOperationForMNAO(voltageMapLinkJSFile, locale, contentDetails);
		}
		try 
		{
			/*
			 * IDENTIFY IF MME LOCALE OR MC LOCALE
			 */
			// replace - by _ in locale
			locale=locale.replace("-", "_");
			boolean mcLocale=false;
			if(null!=locale && locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("ja-jp").trim().toLowerCase()))
			{
				// locale is ja_JP
				mcLocale = true;
			}
			// replace _ in locale by -
			locale=locale.replace("_", "-");
			
			jsContent = ConversionUtils.getStringFromXML(voltageMapLinkJSFile);
			if (null != jsContent && !"".equals(jsContent)) 
			{
				// jsContent.indexOf("window.open(\"");
				int startingIndex = 0;
				int endingIndex = 0;

				String aTagString = "";
				String modelType=null;
				String filePath = null;
				List<String> uniqueInnerLinksPath = null;
				boolean addInnerLink = true;
				WindowJSDetails jsDetails = null;
				while (startingIndex < jsContent.length()) 
				{
					// search for <a tag
					startingIndex = jsContent.indexOf("link:'",	endingIndex);
					if (startingIndex == -1 || startingIndex < 0) 
					{
						break;
					} else 
					{
						// search for ' tag
						endingIndex = jsContent.indexOf("'",startingIndex + 6);
						aTagString = jsContent.substring(startingIndex + 6,	endingIndex);
						if (null != aTagString && !"".equals(aTagString)) 
						{
							// PROCEED ONLY WHEN PATH ENDS WITH - .ent/
							if (aTagString.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.ent"))) 
							{
								if (aTagString.startsWith("/")) 
								{
									aTagString = aTagString.substring(1,aTagString.length());
								}
								/*
								 * prepare New URL - Append Locale to the Path
								 * Identify Model Type for the Path
								 */
								if(aTagString.trim().toLowerCase().startsWith(ApplicationProperties.getProperty("model.type.new"))) 
								{
									modelType = ApplicationProperties.getProperty("model.type.new");
								} 
								else if (aTagString.trim().toLowerCase().startsWith(ApplicationProperties.getProperty("model.type.old")))
								{
									modelType = ApplicationProperties.getProperty("model.type.old");
								}

								// add Locale to aTag String
								filePath = locale + "/" + aTagString;
								filePath = filePath.replace("/", "\\");
								
								/*
								 * add this FilePath to unique Inner Link List
								 */
								addInnerLink = true;
								if(null!=uniqueInnerLinksPath && uniqueInnerLinksPath.size()>0)
								{
									for(int r=0;r<uniqueInnerLinksPath.size();r++)
									{
										if(uniqueInnerLinksPath.get(r).trim().toLowerCase().equals(filePath.trim().toLowerCase()))
										{
											// already added
											addInnerLink = false;
											 break;
										}
									}
								}
								
								if(addInnerLink==true)
								{
									if(null==uniqueInnerLinksPath || uniqueInnerLinksPath.size()<=0)
									{
										uniqueInnerLinksPath = new ArrayList<String>();
									}
									uniqueInnerLinksPath.add(filePath);
								}

								/*
								 * PREPARE WINDOW JS DETAILS OBJECT AND START ADDING DETAILS TO IT
								 */
								jsDetails = new WindowJSDetails();
								jsDetails.setFilePath(PathUtil.winPath(voltageMapLinkJSFile));
								jsDetails.setFileName(voltageMapLinkJSFile.getName());
								jsDetails.setInnerLinkPath(filePath);
								jsDetails.setaTagString(aTagString);
								// by DEFAULT FAILURE
								jsDetails.setProcessingStatus("FAILURE");
								jsDetails.setContentDetails(contentDetails);
								
								/*
								 * ADD THESE LINKS TO VOLTAGE MAP LINK JS LIST
								 */
								if(null==voltageMapLinkJSList || voltageMapLinkJSList.size()<=0)
								{
									voltageMapLinkJSList = new ArrayList<WindowJSDetails>();
								}
								voltageMapLinkJSList.add(jsDetails);
								jsDetails=null;
								filePath = null;
								// DO NOT SET MODEL TYPE AS NULL
							}
						}
						aTagString = null;

						startingIndex = startingIndex + 6;
						endingIndex = startingIndex;
					}
				}
				
				/*
				 * NOW VEIRFY IF UNIQUE INNER LINKS ARE FOUND = FETCH DOCUMENT IDS FOR THEM
				 */
				
				// set default as MME Market
				String market = ApplicationProperties.getProperty("market.mme").trim().toLowerCase();
				// set linkURL as default for MME - ADD INFOCENTER CONTEXT AS WELL
				String linkURL = ApplicationProperties.getProperty("GMS3_INFOCENTER_APPLICATION_CONTENT")+ ApplicationProperties.getProperty("LINK_URL_MME_WINDOW_JS");
				
				if(mcLocale==true)
				{
					// Japan Locale - set Market as MC
					market = ApplicationProperties.getProperty("market.mc").trim().toLowerCase();
					// set linkURL for MC Market since Japan Locale - ADD INFOCENTER CONTEXT AS WELL
					linkURL = ApplicationProperties.getProperty("GMS3_INFOCENTER_APPLICATION_CONTENT")+ ApplicationProperties.getProperty("LINK_URL_MC_WINDOW_JS");
				}
				
				if(null!=uniqueInnerLinksPath && uniqueInnerLinksPath.size()>0)
				{
					logger.info("performVoltageMapLinkJSOperation :: Total Unique Inner Links Found are :: >" + uniqueInnerLinksPath.size());
					/*
					 * FETCH INNERLINKS DOCUMENT IDS
					 */
					List<SelectItemDetails> innerLinksList = MMEDocumentManagementDAO.getDocumentDetailsForInnerLink(uniqueInnerLinksPath, modelType, locale, market);
					if(null!=innerLinksList && innerLinksList.size()>0 && null!=voltageMapLinkJSList && voltageMapLinkJSList.size()>0)
					{

						SelectItemDetails itemDetails = null;
						jsDetails=  null;
						/*
						 * iterate innerLinks List and update the document Ids against each of the innerLink
						 */
						for(int b=0;b<voltageMapLinkJSList.size();b++)
						{
							jsDetails=  (WindowJSDetails)voltageMapLinkJSList.get(b);
							if(null!=jsDetails.getInnerLinkPath() && !"".equals(jsDetails.getInnerLinkPath()))
							{
								itemDetails = null;
								for(int a=0;a<innerLinksList.size();a++)
								{
									itemDetails= (SelectItemDetails)innerLinksList.get(a);
									if(null!=itemDetails.getValue() && !"".equals(itemDetails.getValue()))
									{
										if(jsDetails.getInnerLinkPath().trim().toLowerCase().equals(itemDetails.getValue().trim().toLowerCase()))
										{
											// set document Id in voltageList
											jsDetails.setInnerLinkDocumentId(itemDetails.getLabel());
											break;
										}
									}
									itemDetails = null;
								}
								itemDetails = null;
							}
							jsDetails=  null;
						}
						jsDetails = null;
					}
					innerLinksList = null;
				}
				else
				{
					logger.info("performVoltageMapLinkJSOperation ::  No Unique Inner Links Found for JS File :: >"+ voltageMapLinkJSFile.getName());
				}
				uniqueInnerLinksPath = null;
				
				
				/*
				 * NOW SINCE ALL DOCUMENT IDS ARE IDENTIFIED - PREPARE INNER LINKS FOR REPLACEMENT AND
				 * UPDATE IN VOLTAGE MAP LIST AS WELL AS JS CONTENT
				 */
				if(null!=voltageMapLinkJSList && voltageMapLinkJSList.size()>0)
				{
					jsDetails=  null;
					for(int b=0;b<voltageMapLinkJSList.size();b++)
					{
						jsDetails=  (WindowJSDetails)voltageMapLinkJSList.get(b);
						if(null!=jsDetails.getInnerLinkDocumentId() && !"".equals(jsDetails.getInnerLinkDocumentId()))
						{
							// set processingStatus as SUCCESS
							jsDetails.setProcessingStatus("SUCCESS");
							jsDetails.setInnerLinkDocumentId(jsDetails.getInnerLinkDocumentId());
							jsDetails.setPreapredInnerLinkURLForIC(linkURL+jsDetails.getInnerLinkDocumentId());
						}
						else
						{
							// set processingStatus as FAILURE
							jsDetails.setProcessingStatus("FAILURE");
							jsDetails.setPreapredInnerLinkURLForIC(linkURL);
						}
						
						/*
						 * REPLACE INNER LINK PATH WITH JS CONTENT
						 */
						// REPLACE THE SOURCE STRING (ATAGSTRING) WITH LINK URL
						if(null!=jsContent && !"".equals(jsContent))
						{
							if(null!=jsDetails.getaTagString())
							{
								jsContent = jsContent.replace(jsDetails.getaTagString(),jsDetails.getPreapredInnerLinkURLForIC());
							}
						}
						jsDetails=  null;
					}
				}
				jsDetails= null;
				market=  null;
				linkURL=  null;
			}
		} 
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),"performVoltageMapLinkJSOperation()", e);
		}
		return jsContent;
	}

	public  String performVoltageMapJSOperationForMNAO(File voltageMapJSFile,String locale, ContentDetails contentDetails) 
	{
		String jsContent = "";
		try 
		{
			// replace _ in locale by -
			locale=locale.replace("_", "-");
			
			jsContent = ConversionUtils.getStringFromXML(voltageMapJSFile);
			if (null != jsContent && !"".equals(jsContent)) 
			{
				// jsContent.indexOf("window.open(\"");
				int startingIndex = 0;
				int endingIndex = 0;

				String aTagString = "";
				String filePath = null;
				List<String> uniqueInnerLinksPath = null;
				boolean addInnerLink = true;
				WindowJSDetails jsDetails = null;
				while (startingIndex < jsContent.length()) 
				{
					// search for <a tag
					startingIndex = jsContent.indexOf(":'",	endingIndex);
					if (startingIndex == -1 || startingIndex < 0) 
					{
						break;
					} else 
					{
						// search for ' tag
						endingIndex = jsContent.indexOf("'",startingIndex + 2);
						aTagString = jsContent.substring(startingIndex + 2,	endingIndex);
						if (null != aTagString && !"".equals(aTagString)) 
						{
							// PROCEED ONLY WHEN PATH ENDS WITH - .ent/
							if (aTagString.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.ent"))) 
							{
								if (aTagString.startsWith("/")) 
								{
									aTagString = aTagString.substring(1,aTagString.length());
								}
								/*
								 * prepare New URL - Append Locale to the Path
								 */

								// add Locale to aTag String
								filePath = locale + "/" + aTagString;
								filePath = filePath.replace("/", "\\");
								
								/*
								 * add this FilePath to unique Inner Link List
								 */
								addInnerLink = true;
								if(null!=uniqueInnerLinksPath && uniqueInnerLinksPath.size()>0)
								{
									for(int r=0;r<uniqueInnerLinksPath.size();r++)
									{
										if(uniqueInnerLinksPath.get(r).trim().toLowerCase().equals(filePath.trim().toLowerCase()))
										{
											// already added
											addInnerLink = false;
											 break;
										}
									}
								}
								
								if(addInnerLink==true)
								{
									if(null==uniqueInnerLinksPath || uniqueInnerLinksPath.size()<=0)
									{
										uniqueInnerLinksPath = new ArrayList<String>();
									}
									uniqueInnerLinksPath.add(filePath);
								}

								/*
								 * PREPARE WINDOW JS DETAILS OBJECT AND START ADDING DETAILS TO IT
								 */
								jsDetails = new WindowJSDetails();
								jsDetails.setFilePath(PathUtil.winPath(voltageMapJSFile));
								jsDetails.setFileName(voltageMapJSFile.getName());
								jsDetails.setInnerLinkPath(filePath);
								jsDetails.setaTagString(aTagString);
								// by DEFAULT FAILURE
								jsDetails.setProcessingStatus("FAILURE");
								jsDetails.setContentDetails(contentDetails);
								
								/*
								 * ADD THESE LINKS TO VOLTAGE MAP JS LIST
								 */
								if(null==voltageMapJSList || voltageMapJSList.size()<=0)
								{
									voltageMapJSList = new ArrayList<WindowJSDetails>();
								}
								voltageMapJSList.add(jsDetails);
								jsDetails=null;
								filePath = null;
								// DO NOT SET MODEL TYPE AS NULL
							}
						}
						aTagString = null;

						startingIndex = startingIndex + 2;
						endingIndex = startingIndex;
					}
				}
				
				/*
				 * NOW VEIRFY IF UNIQUE INNER LINKS ARE FOUND = FETCH DOCUMENT IDS FOR THEM
				 */
				
				// set linkURL as default for MNAO - ADD INFOCENTER CONTEXT AS WELL
				String linkURL = ApplicationProperties.getProperty("GMS3_INFOCENTER_APPLICATION_CONTENT")+ ApplicationProperties.getProperty("LINK_URL");
				
				if(null!=uniqueInnerLinksPath && uniqueInnerLinksPath.size()>0)
				{
					logger.info("performVoltageMapJSOperationForMNAO :: Total Unique Inner Links Found are :: >" + uniqueInnerLinksPath.size());
					/*
					 * FETCH INNERLINKS DOCUMENT IDS
					 */
					List<SelectItemDetails> innerLinksList = MNAODocumentManagementDAO.getDocumentDetailsForInnerLink(uniqueInnerLinksPath, locale);
					if(null!=innerLinksList && innerLinksList.size()>0 && null!=voltageMapJSList && voltageMapJSList.size()>0)
					{
						SelectItemDetails itemDetails = null;
						jsDetails=  null;
						/*
						 * iterate innerLinks List and update the document Ids against each of the innerLink
						 */
						for(int b=0;b<voltageMapJSList.size();b++)
						{
							jsDetails=  (WindowJSDetails)voltageMapJSList.get(b);
							if(null!=jsDetails.getInnerLinkPath() && !"".equals(jsDetails.getInnerLinkPath()))
							{
								itemDetails = null;
								for(int a=0;a<innerLinksList.size();a++)
								{
									itemDetails= (SelectItemDetails)innerLinksList.get(a);
									if(null!=itemDetails.getValue() && !"".equals(itemDetails.getValue()))
									{
										if(jsDetails.getInnerLinkPath().trim().toLowerCase().equals(itemDetails.getValue().trim().toLowerCase()))
										{
											// set document Id in voltageList
											jsDetails.setInnerLinkDocumentId(itemDetails.getLabel());
											break;
										}
									}
									itemDetails = null;
								}
								itemDetails = null;
							}
							jsDetails=  null;
						}
						jsDetails = null;
					}
					innerLinksList = null;
				}
				else
				{
					logger.info("performVoltageMapJSOperationForMNAO ::  No Unique Inner Links Found for JS File :: >"+ voltageMapJSFile.getName());
				}
				uniqueInnerLinksPath = null;
				
				
				/*
				 * NOW SINCE ALL DOCUMENT IDS ARE IDENTIFIED - PREPARE INNER LINKS FOR REPLACEMENT AND
				 * UPDATE IN VOLTAGE MAP LIST AS WELL AS JS CONTENT
				 */
				if(null!=voltageMapJSList && voltageMapJSList.size()>0)
				{
					jsDetails=  null;
					for(int b=0;b<voltageMapJSList.size();b++)
					{
						jsDetails=  (WindowJSDetails)voltageMapJSList.get(b);
						if(null!=jsDetails.getInnerLinkDocumentId() && !"".equals(jsDetails.getInnerLinkDocumentId()))
						{
							// set processingStatus as SUCCESS
							jsDetails.setProcessingStatus("SUCCESS");
							jsDetails.setInnerLinkDocumentId(jsDetails.getInnerLinkDocumentId());
							jsDetails.setPreapredInnerLinkURLForIC(linkURL+jsDetails.getInnerLinkDocumentId());
						}
						else
						{
							// set processingStatus as FAILURE
							jsDetails.setProcessingStatus("FAILURE");
							jsDetails.setPreapredInnerLinkURLForIC(linkURL);
						}
						
						/*
						 * REPLACE INNER LINK PATH WITH JS CONTENT
						 */
						// REPLACE THE SOURCE STRING (ATAGSTRING) WITH LINK URL
						if(null!=jsContent && !"".equals(jsContent))
						{
							if(null!=jsDetails.getaTagString())
							{
								jsContent = jsContent.replace(jsDetails.getaTagString(),jsDetails.getPreapredInnerLinkURLForIC());
							}
						}
						jsDetails=  null;
					}
				}
				jsDetails= null;
				linkURL=  null;
			}
		} 
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),"performVoltageMapJSOperationForMNAO()", e);
		}
		return jsContent;
	}

	public  String performVoltageMapLinkJSOperationForMNAO(File voltageMapLinkJSFile,String locale, ContentDetails contentDetails) 
	{
		String jsContent = "";
		try 
		{
			/*
			 * IDENTIFY IF MME LOCALE OR MC LOCALE
			 */
			// replace _ in locale by -
			locale=locale.replace("_", "-");
			
			jsContent = ConversionUtils.getStringFromXML(voltageMapLinkJSFile);
			if (null != jsContent && !"".equals(jsContent)) 
			{
				// jsContent.indexOf("window.open(\"");
				int startingIndex = 0;
				int endingIndex = 0;

				String aTagString = "";
				String filePath = null;
				List<String> uniqueInnerLinksPath = null;
				boolean addInnerLink = true;
				WindowJSDetails jsDetails = null;
				while (startingIndex < jsContent.length()) 
				{
					// search for <a tag
					startingIndex = jsContent.indexOf("link:'",	endingIndex);
					if (startingIndex == -1 || startingIndex < 0) 
					{
						break;
					} else 
					{
						// search for ' tag
						endingIndex = jsContent.indexOf("'",startingIndex + 6);
						aTagString = jsContent.substring(startingIndex + 6,	endingIndex);
						if (null != aTagString && !"".equals(aTagString)) 
						{
							// PROCEED ONLY WHEN PATH ENDS WITH - .ent/
							if (aTagString.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.ent"))) 
							{
								if (aTagString.startsWith("/")) 
								{
									aTagString = aTagString.substring(1,aTagString.length());
								}
								/*
								 * prepare New URL - Append Locale to the Path
								 */

								// add Locale to aTag String
								filePath = locale + "/" + aTagString;
								filePath = filePath.replace("/", "\\");
								
								/*
								 * add this FilePath to unique Inner Link List
								 */
								addInnerLink = true;
								if(null!=uniqueInnerLinksPath && uniqueInnerLinksPath.size()>0)
								{
									for(int r=0;r<uniqueInnerLinksPath.size();r++)
									{
										if(uniqueInnerLinksPath.get(r).trim().toLowerCase().equals(filePath.trim().toLowerCase()))
										{
											// already added
											addInnerLink = false;
											 break;
										}
									}
								}
								
								if(addInnerLink==true)
								{
									if(null==uniqueInnerLinksPath || uniqueInnerLinksPath.size()<=0)
									{
										uniqueInnerLinksPath = new ArrayList<String>();
									}
									uniqueInnerLinksPath.add(filePath);
								}

								/*
								 * PREPARE WINDOW JS DETAILS OBJECT AND START ADDING DETAILS TO IT
								 */
								jsDetails = new WindowJSDetails();
								jsDetails.setFilePath(PathUtil.winPath(voltageMapLinkJSFile));
								jsDetails.setFileName(voltageMapLinkJSFile.getName());
								jsDetails.setInnerLinkPath(filePath);
								jsDetails.setaTagString(aTagString);
								// by DEFAULT FAILURE
								jsDetails.setProcessingStatus("FAILURE");
								jsDetails.setContentDetails(contentDetails);
								
								/*
								 * ADD THESE LINKS TO VOLTAGE MAP LINK JS LIST
								 */
								if(null==voltageMapLinkJSList || voltageMapLinkJSList.size()<=0)
								{
									voltageMapLinkJSList = new ArrayList<WindowJSDetails>();
								}
								voltageMapLinkJSList.add(jsDetails);
								jsDetails=null;
								filePath = null;
								// DO NOT SET MODEL TYPE AS NULL
							}
						}
						aTagString = null;

						startingIndex = startingIndex + 6;
						endingIndex = startingIndex;
					}
				}
				
				/*
				 * NOW VEIRFY IF UNIQUE INNER LINKS ARE FOUND = FETCH DOCUMENT IDS FOR THEM
				 */
				
				// set linkURL as default for MNAO - ADD INFOCENTER CONTEXT AS WELL
				String linkURL = ApplicationProperties.getProperty("GMS3_INFOCENTER_APPLICATION_CONTENT")+ ApplicationProperties.getProperty("LINK_URL");
				
				if(null!=uniqueInnerLinksPath && uniqueInnerLinksPath.size()>0)
				{
					logger.info("performVoltageMapLinkJSOperationForMNAO :: Total Unique Inner Links Found are :: >" + uniqueInnerLinksPath.size());
					/*
					 * FETCH INNERLINKS DOCUMENT IDS
					 */
					List<SelectItemDetails> innerLinksList = MNAODocumentManagementDAO.getDocumentDetailsForInnerLink(uniqueInnerLinksPath, locale);
					if(null!=innerLinksList && innerLinksList.size()>0 && null!=voltageMapLinkJSList && voltageMapLinkJSList.size()>0)
					{
						SelectItemDetails itemDetails = null;
						jsDetails=  null;
						/*
						 * iterate innerLinks List and update the document Ids against each of the innerLink
						 */
						for(int b=0;b<voltageMapLinkJSList.size();b++)
						{
							jsDetails=  (WindowJSDetails)voltageMapLinkJSList.get(b);
							if(null!=jsDetails.getInnerLinkPath() && !"".equals(jsDetails.getInnerLinkPath()))
							{
								itemDetails = null;
								for(int a=0;a<innerLinksList.size();a++)
								{
									itemDetails= (SelectItemDetails)innerLinksList.get(a);
									if(null!=itemDetails.getValue() && !"".equals(itemDetails.getValue()))
									{
										if(jsDetails.getInnerLinkPath().trim().toLowerCase().equals(itemDetails.getValue().trim().toLowerCase()))
										{
											// set document Id in voltageList
											jsDetails.setInnerLinkDocumentId(itemDetails.getLabel());
											break;
										}
									}
									itemDetails = null;
								}
								itemDetails = null;
							}
							jsDetails=  null;
						}
						jsDetails = null;
					}
					innerLinksList = null;
				}
				else
				{
					logger.info("performVoltageMapLinkJSOperationForMNAO ::  No Unique Inner Links Found for JS File :: >"+ voltageMapLinkJSFile.getName());
				}
				uniqueInnerLinksPath = null;
				
				
				/*
				 * NOW SINCE ALL DOCUMENT IDS ARE IDENTIFIED - PREPARE INNER LINKS FOR REPLACEMENT AND
				 * UPDATE IN VOLTAGE MAP LIST AS WELL AS JS CONTENT
				 */
				if(null!=voltageMapLinkJSList && voltageMapLinkJSList.size()>0)
				{
					jsDetails=  null;
					for(int b=0;b<voltageMapLinkJSList.size();b++)
					{
						jsDetails=  (WindowJSDetails)voltageMapLinkJSList.get(b);
						if(null!=jsDetails.getInnerLinkDocumentId() && !"".equals(jsDetails.getInnerLinkDocumentId()))
						{
							// set processingStatus as SUCCESS
							jsDetails.setProcessingStatus("SUCCESS");
							jsDetails.setInnerLinkDocumentId(jsDetails.getInnerLinkDocumentId());
							jsDetails.setPreapredInnerLinkURLForIC(linkURL+jsDetails.getInnerLinkDocumentId());
						}
						else
						{
							// set processingStatus as FAILURE
							jsDetails.setProcessingStatus("FAILURE");
							jsDetails.setPreapredInnerLinkURLForIC(linkURL);
						}
						
						/*
						 * REPLACE INNER LINK PATH WITH JS CONTENT
						 */
						// REPLACE THE SOURCE STRING (ATAGSTRING) WITH LINK URL
						if(null!=jsContent && !"".equals(jsContent))
						{
							if(null!=jsDetails.getaTagString())
							{
								jsContent = jsContent.replace(jsDetails.getaTagString(),jsDetails.getPreapredInnerLinkURLForIC());
							}
						}
						jsDetails=  null;
					}
				}
				jsDetails= null;
				linkURL=  null;
			}
		} 
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),"performVoltageMapLinkJSOperationForMNAO()", e);
		}
		return jsContent;
	}

	
	/** true when the document being converted belongs to the MNAO market */
	private static boolean isMNAOMarket(ContentDetails contentDetails)
	{
		return null!=contentDetails && null!=contentDetails.getMarket()
				&& contentDetails.getMarket().trim().equalsIgnoreCase(ApplicationProperties.getProperty("market.mnao").trim());
	}

	/**
	 * Function will replace the CONN FOLDER PATH IN THE JS FILE
	 * 
	 * @param jsFile
	 * @param temPathForDestionation
	 * @return
	 */
	public  String updateJSContentForHTML5(File jsFile,
			String temPathForDestionation) {
		String jsContent = "";
		try {
			/*
			 * CHECK HERE IF tempDestinationPathForJS DOES NOT STARTS WITH /
			 * THEN ADD
			 */
			if (null != temPathForDestionation
					&& !"".equals(temPathForDestionation)) {
				if (!temPathForDestionation.startsWith("/")) {
					temPathForDestionation = "/" + temPathForDestionation;
				}
			}
			/*
			 * HERE TEMPDestinationPathFor JS WILL END WITH JS DIRECTORY replace
			 * it WITH CONN DIRECTORY
			 */
			temPathForDestionation = temPathForDestionation.replace("/"
					+ ApplicationProperties.getProperty("directory.js"), "/"
					+ ApplicationProperties.getProperty("directory.conn"));

			if (jsFile.exists()) {
				jsContent = ConversionUtils.getStringFromXML(jsFile);
				/*
				 * CHECK HERE - IF THERE IS ANY PATH LIKE = ../conn/ REPLACE IT
				 * WITH conn/
				 */
				jsContent = jsContent.replace(
						"../"
								+ ApplicationProperties
										.getProperty("directory.conn") + "/",
						ApplicationProperties.getProperty("directory.conn")
								+ "/");

				jsContent = jsContent.replace(
						ApplicationProperties.getProperty("directory.conn")
								+ "/", temPathForDestionation);
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),
					"updateJSContentForHTML5()", e);
		}
		return jsContent;
	}

	/**
	 * Function will replace the Meta Tags from the HTML Content
	 * 
	 * @param htmlContent
	 * @return
	 */
	public  String removeMetaTags(String htmlContent) {
		try {
			org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
			org.jsoup.select.Elements metaTagsList = doc.select("meta");

			if (null != metaTagsList && metaTagsList.size() > 0) {
				for (int i = 0; i < metaTagsList.size(); i++) {
					metaTagsList.get(i).remove();
				}
			}

			if (null != doc) {
				htmlContent = doc.toString();
			}
			metaTagsList = null;
			doc = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return htmlContent;
	}

	/**
	 * Function will remove the IFRAME AND SCRIPT TAGS FROM HTML CONTENT;
	 * 
	 * @param htmlContent
	 * @return
	 */
	public  String removeIframeAndScriptTag(String htmlContent) {
		try {
			org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
			org.jsoup.select.Elements iFramesTagsList = doc.select("iframe");

			if (null != iFramesTagsList && iFramesTagsList.size() > 0) {
				for (int i = 0; i < iFramesTagsList.size(); i++) {
					iFramesTagsList.get(i).remove();
				}
			}

			// Check for Script Tags and remove it
			org.jsoup.select.Elements scriptTagsList = doc.select("script");

			if (null != scriptTagsList && scriptTagsList.size() > 0) {
				for (int i = 0; i < scriptTagsList.size(); i++) {
					scriptTagsList.get(i).remove();
				}
			}

			if (null != doc) {
				htmlContent = doc.toString();
			}

			iFramesTagsList = null;
			scriptTagsList = null;

			doc = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return htmlContent;
	}

	/**
	 * Function will remove the STYLE TAGS FROM THE HTML CONTENT
	 * 
	 * @param htmlContent
	 * @return
	 */
	public  String removeStyleTag(String htmlContent) {
		try {
			org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
			org.jsoup.select.Elements styleTagsList = doc.select("style");

			if (null != styleTagsList && styleTagsList.size() > 0) {
				for (int i = 0; i < styleTagsList.size(); i++) {
					styleTagsList.get(i).remove();
				}
			}

			org.jsoup.select.Elements linkTagsList = doc.select("link");

			if (null != linkTagsList && linkTagsList.size() > 0) {
				for (int i = 0; i < linkTagsList.size(); i++) {
					linkTagsList.get(i).remove();
				}
			}

			/*
			 * IDENTIFY body TAG - AND ADD A LINK FOR MAZDA.CSS
			 */
			org.jsoup.select.Elements bodyTagsList = doc.select("body");
			if (null != bodyTagsList && bodyTagsList.size() > 0) {
				Element bodyEle = bodyTagsList.get(0);
				Element linkEle = doc.createElement("link");
				// add Library Path
				String cssPath = ApplicationProperties
						.getProperty("SERVER_LIBRARY_DIRECTORY");
				if (!cssPath.startsWith("/")) {
					cssPath = "/" + cssPath;
				}
				if (!cssPath.endsWith("/")) {
					cssPath = cssPath + "/";
				}
				// add repository path
				cssPath = cssPath
						+ ApplicationProperties.getProperty("REPOSITORY")
								.toUpperCase() + "/";
				// add custom directory name
				cssPath = cssPath
						+ ApplicationProperties
								.getProperty("GMS3_CUSTOM_DIRECTORY") + "/";
				// add css name -
				cssPath = cssPath
						+ ApplicationProperties
								.getProperty("GMS3_CUSTOM_CONTENT_CSS");
				linkEle.attr("href", cssPath);
				linkEle.attr("type", "text/css");
				linkEle.attr("rel", "stylesheet");
				bodyEle.appendChild(linkEle);
				linkEle = null;
				cssPath = null;
			}

			if (null != doc) {
				htmlContent = doc.toString();
			}

			styleTagsList = null;
			linkTagsList = null;
			doc = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return htmlContent;
	}

	/**
	 * Function will remove OnLoad Tag from Body Element
	 * 
	 * @param htmlContent
	 * @return
	 */
	public  String removeOnloadAttributeFromBody(String htmlContent) {
		try {
			if (null != htmlContent && !"".equals(htmlContent)) {
				org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
				org.jsoup.select.Elements bodyTagsList = doc.select("body");
				if (null != bodyTagsList && bodyTagsList.size() > 0) {
					Element bodyEle = bodyTagsList.get(0);
					bodyEle.removeAttr("onload");
				}
				if (null != doc) {
					htmlContent = doc.toString();
				}
				doc = null;
				bodyTagsList = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),
					"removeOnLoadAttributeFromBody", e);
		}
		return htmlContent;
	}

	/**
	 * Function will Prepare the InnerLink Paths for the Document
	 * 
	 * @param contentDetails
	 * @return
	 */
	public  ContentDetails prepareInnerLinkPaths(ContentDetails contentDetails) 
	{
		logger.info("prepareInnerLinkPaths :: ------------------ STARTS ------------------ ");
		try 
		{
			String extensionToBeAdded=null;
			if(null!=contentDetails.getFilePath() && !"".equals(contentDetails.getFilePath())) 
			{
				extensionToBeAdded  =contentDetails.getFilePath().substring(contentDetails.getFilePath().lastIndexOf("."),contentDetails.getFilePath().length());
			}
			List<String> uniqueInnerLinkPaths=null;
			LinkDetails linkDetails = null;
			boolean addToUniqueList = true;
			contentDetails.setInnerLinksList(new ArrayList<LinkDetails>());
			contentDetails.setAdditionalInnerLinks(new ArrayList<LinkDetails>());
			if (null != contentDetails.getDocumentContent() && !"".equals(contentDetails.getDocumentContent())) 
			{
				org.jsoup.nodes.Document doc = Jsoup.parse(contentDetails.getDocumentContent());
				org.jsoup.select.Elements aTagsList = doc.select("a");
				if (null != aTagsList && aTagsList.size() > 0) 
				{
					String hrefAttr = null;
					for (int a = 0; a < aTagsList.size(); a++) 
					{
						Element aElement = aTagsList.get(a);
						hrefAttr = aElement.attr("href");
						/*
						 * NEW INNERLINK TYPE INTRODUCED WHICH NEEDS TO BE PASSED AS IT IS
						 * NO PROCESSING REQUIRED - BUT NEEDS TO BE TRACKED IN DATABASE FOR REFERENCE
						 * 
						 * FOR MNAO LINK WILL BE LIKE THIS - 
						 * 	index?page=result&startover=y&qstr=qstr&fac=CMS-CATEGORY-MAZDA-SERVICE_MANUAL_TYPE.WORKSHOP_MANUAL&question_box={Tag ID}
						 */
						if(null!=hrefAttr && !"".equals(hrefAttr) && hrefAttr.trim().toLowerCase().startsWith("index?page=result"))
						{
							// PASS THIS AS IS - DO NOTHING - TRACE AS ADDITIONAL INNERLINKS
							linkDetails = new LinkDetails();
							linkDetails.setInnerLinkPath(hrefAttr);
							linkDetails.setSourceFilePath(contentDetails.getFilePath());
							linkDetails.setMapStatus("Y");
							if(null==contentDetails.getAdditionalInnerLinks() || contentDetails.getAdditionalInnerLinks().size()<=0)
							{
								contentDetails.setAdditionalInnerLinks(new ArrayList<LinkDetails>());
							}
							contentDetails.getAdditionalInnerLinks().add(linkDetails);
							linkDetails= null;
							
							// for such links append - target="_blank"
							aElement.attr("target","_blank");
						}
						else if(null != hrefAttr && !"".equals(hrefAttr) && !"#".equals(hrefAttr) && !hrefAttr.startsWith("#") && !hrefAttr.startsWith("http:")
								&& !hrefAttr.startsWith("https:") && !hrefAttr.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf"))) 
						{
							hrefAttr = hrefAttr.trim();
							/*
							 * ALSO CHECK HERE,IF HREF STARTS WITH # IN SCENARIO
							 * WHEN HTML GENERATED FROM ENT FILE RMEOVE IT
							 */
							// if(hrefAttr.startsWith("#"))
							// {
							// hrefAttr = hrefAttr.replaceAll("#", "");
							// }

							// replace all / by \\
							hrefAttr = hrefAttr.replace("/", "\\");

							/*
							 * BEFORE CONVERTING LINK TO A INNER LINK = CHECK IF
							 * THE HREF ATTR CONTAINS SAME FILE ID AND LENGGTH
							 * OF HREFATTR IS GREATER THAN OR MORE THAN FILE
							 * NAME MAKE IT A DEAD LINK
							 * 
							 * COMMENT THIS NO LINKS WITH # WILL BE PROCESSED
							 * FURTHER
							 */
							boolean proceedFurther = true;
							if (hrefAttr.startsWith("#")) {
								proceedFurther = false;
							}

							/*
							 * CHECK HERE, IF MODEL IS RX-8 AND DOCUMENT TYPE IS
							 * ENT FOR ANY FILE IF EXTENSION ENDS WITH .HTM DO
							 * NOT ADD IT TO INNERLINK LIST, INSTEAD - PREPARE
							 * THE OKASSET PATH FOR THAT LINK
							 */

							if (null != contentDetails.getDocumentType() && contentDetails.getDocumentType().equals(ContentDetails.ENT_DOCUMENT)) 
							{
								if (contentDetails.getModel().trim().toLowerCase().startsWith("rx-8")) 
								{
									if (hrefAttr.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.htm"))) 
									{
										proceedFurther = false;
										/*
										 * PERFORM OKASSET OPERATION FOR THIS
										 * ONE.
										 */
										String channelFolderName = ApplicationProperties.getProperty("service.manuals.folder.label");
										String localeFolderName = ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase());
										// PREPARE CHANNEL REF KEY
										String channelFolderRefKey = channelFolderName;
										channelFolderRefKey = channelFolderRefKey.replace(" ", "_");
										String pathToBeReplaced = "/"+ ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
										// // add repository
										pathToBeReplaced = pathToBeReplaced+ ApplicationProperties.getProperty("REPOSITORY").toUpperCase() + "/";
										// add channel Name
										pathToBeReplaced = pathToBeReplaced+ channelFolderRefKey.toUpperCase() + "/";
										// add locale Folder Name
										pathToBeReplaced = pathToBeReplaced+ localeFolderName.trim().toLowerCase() + "/";
										channelFolderRefKey = null;
										// add html as processing folderName
										pathToBeReplaced = pathToBeReplaced+ "html";
										String fileName = "";
										if (hrefAttr.lastIndexOf("/") != -1) 
										{
											fileName = hrefAttr.substring(hrefAttr.lastIndexOf("/") + 1,hrefAttr.length());
										} 
										else 
										{
											fileName = hrefAttr;
										}

										fileName = pathToBeReplaced + "/"+ fileName;
										hrefAttr = fileName;
										fileName = null;
										pathToBeReplaced = null;
										channelFolderName = null;
										localeFolderName = null;
									}
								}
							}
							if (proceedFurther == true) 
							{
								/*
								 * CHECK FOR PROCESSING DOC PARENT AND EXTRACT
								 * THE PATH FROM LOCALE TO THE PROCESSING FILE'S
								 * PARENT FOLDER APPEND TO THE LINK
								 */
								if (null != contentDetails.getFileAbsolutePath() && !"".equals(contentDetails.getFileAbsolutePath())) 
								{
									File processingFile = PathUtil.file(contentDetails.getFileAbsolutePath());
									if (processingFile.isFile() && processingFile.exists()) 
									{
										String parentDirPath = PathUtil.winPath(processingFile.getParentFile());
										if (null != parentDirPath && !"".equals(parentDirPath)) 
										{
											parentDirPath = parentDirPath.substring(parentDirPath.lastIndexOf(contentDetails.getLocale()),parentDirPath.length());
											if (null != parentDirPath && !"".equals(parentDirPath)) 
											{
												parentDirPath = parentDirPath.replace("/", "\\");
											}
											if (!parentDirPath.endsWith("\\")) 
											{
												parentDirPath = parentDirPath+ "\\";
											}
											// add to current HREF Attr
											if (hrefAttr.startsWith("\\")) 
											{
												hrefAttr = hrefAttr.substring(1, hrefAttr.length());
											}
											hrefAttr = parentDirPath+ hrefAttr.trim();
										}
										parentDirPath = null;
									}
									processingFile = null;
								}

								
								// add extensionToBeAdded to hrefAttr
								if(null!=hrefAttr && !"".equals(hrefAttr) && null!=extensionToBeAdded && !"".equals(extensionToBeAdded))
								{
									/*
									 * CHECK HERE IF THE INNERLINK ALREADY CONTAINS THE EXSTENSION OR NOT
									 * IF YES, THEN DO NOT ADD IT - SCENARIO WHEN PROCESSING NORMAL HTML FILE
									 * 	THEY ALREADY CONTAIN EXTENSION SO NO NEED TO ADD EXPLICITLY
									 * add this incase of ENT only
									 */
									if(extensionToBeAdded.trim().toLowerCase().equals(".ent"))
									{
										hrefAttr=hrefAttr+extensionToBeAdded;
									}
								}
								
								/*
								 * ADD THIS AS LINK TO CONTENT DETAILS
								 */
								linkDetails = new LinkDetails();
								linkDetails.setInnerLinkPath(hrefAttr);
								linkDetails.setSourceFilePath(contentDetails.getFilePath());
								linkDetails.setMapStatus("N");
								if(null==contentDetails.getInnerLinksList() || contentDetails.getInnerLinksList().size()<=0)
								{
									contentDetails.setInnerLinksList(new ArrayList<LinkDetails>());
								}
								// SET INNERLINKS FOUND TO YES
								contentDetails.setInnerLinkFound("YES");
								contentDetails.getInnerLinksList().add(linkDetails);
								linkDetails= null;
								
								// add this to InnerLink to uniqueInnerLinkPaths
								addToUniqueList= true;
								if(null!=uniqueInnerLinkPaths && uniqueInnerLinkPaths.size()>0)
								{
									for(int m=0;m<uniqueInnerLinkPaths.size();m++)
									{
										if(uniqueInnerLinkPaths.get(m).toString().trim().toLowerCase().equals(hrefAttr.trim().toLowerCase()))
										{
											addToUniqueList = false;
											break;
										}
									}
								}
								
								if(addToUniqueList==true)
								{
									if(null==uniqueInnerLinkPaths || uniqueInnerLinkPaths.size()<=0)
									{
										uniqueInnerLinkPaths = new ArrayList<String>();
									}
									uniqueInnerLinkPaths.add(hrefAttr);
								}
								
							}
							// update href Attribute to the Link
							aElement.attr("href", hrefAttr);
						}
						hrefAttr = null;
					}
					
					if(null!=uniqueInnerLinkPaths && uniqueInnerLinkPaths.size()>0 && null!=contentDetails.getInnerLinkFound() && contentDetails.getInnerLinkFound().equals("YES"))
					{
						logger.info("prepareInnerLinkPaths :: Unique uniqueInnerLinkPaths found are :: >" + uniqueInnerLinkPaths.size());
						List<SelectItemDetails> docsList = MNAODocumentManagementDAO.getDocumentDetailsForInnerLink(uniqueInnerLinkPaths, contentDetails.getLocale());
						if(null!=docsList && docsList.size()>0)
						{
							/*
							 * UPDATE THE STATUS IN INNER LINKS FOR THE MATCHING LINS
							 * 	IF ALL LINKS FOUND - THEN NO NEED OF RE-UPDATING THE DOCUMENT
							 */
							SelectItemDetails si  =null;
							linkDetails = null;
							for(int a=0;a<contentDetails.getInnerLinksList().size();a++)
							{
								linkDetails = (LinkDetails)contentDetails.getInnerLinksList().get(a);
								if(null!=linkDetails.getInnerLinkPath() && !"".equals(linkDetails.getInnerLinkPath()))
								{
									si = null;
									for(int r=0;r<docsList.size();r++)
									{
										si = (SelectItemDetails)docsList.get(r);
										if(linkDetails.getInnerLinkPath().trim().toLowerCase().equals(si.getValue().trim().toLowerCase()))
										{
											linkDetails.setInnnerLinkDocumentId(si.getLabel());
											linkDetails.setMapStatus("Y");
											break;
										}
										si = null;
									}
								}
								linkDetails=  null;
							}
						}
						docsList=  null;
					}
					uniqueInnerLinkPaths = null;
				}
				if (null != doc) 
				{
					contentDetails.setDocumentContent(doc.toString());
					
					/*
					 * CHECK IF ALL INNER LINKS ARE MAPPED OR NOT
					 */
					if(null!=contentDetails.getInnerLinkFound() && contentDetails.getInnerLinkFound().equals("YES") && 
							null!=contentDetails.getInnerLinksList() && contentDetails.getInnerLinksList().size()>0)
					{
						boolean allLinksFound = true;
						linkDetails = null;
						for(int a=0;a<contentDetails.getInnerLinksList().size();a++)
						{
							linkDetails = (LinkDetails)contentDetails.getInnerLinksList().get(a);
							if(null==linkDetails.getInnnerLinkDocumentId() || "".equals(linkDetails.getInnnerLinkDocumentId()))
							{
								allLinksFound = false;
								break;
							}
							linkDetails= null;
						}
						
						if(allLinksFound==true)
						{
							contentDetails.setAllInnerLinksMapped("YES");
						}
						else
						{
							contentDetails.setAllInnerLinksMapped("NO");
						}
						
						if(null!=contentDetails.getAllInnerLinksMapped() && contentDetails.getAllInnerLinksMapped().equals("YES"))
						{
							// update the respective source paths for innerLinks in documentContent
							String linkUrl = ApplicationProperties.getProperty("LINK_URL");
							linkDetails = null;
							for(int a=0;a<contentDetails.getInnerLinksList().size();a++)
							{
								linkDetails = (LinkDetails)contentDetails.getInnerLinksList().get(a);
								// replace in Document Content
								if(null!=contentDetails.getDocumentContent() && !"".equals(contentDetails.getDocumentContent()))
								{
									contentDetails.setDocumentContent(contentDetails.getDocumentContent().replace(linkDetails.getInnerLinkPath(), (linkUrl+linkDetails.getInnnerLinkDocumentId()).replace("&", "&amp;")));
								}
								linkDetails= null;
							}
							linkUrl = null;
						}
					}
					
				}
				doc = null;
				aTagsList = null;
			}
			extensionToBeAdded=  null;
			uniqueInnerLinkPaths = null;
			linkDetails = null;
			
		} 
		catch (Exception e)
		{
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),"prepareInnerLinkPaths()", e);
		}
		logger.info("prepareInnerLinkPaths :: ------------------ ENDS ------------------ ");
		return contentDetails;
	}

	/**
	 * Function will replace all the EMBED SRC Data from the Content Data,
	 * incase if no value is found in the src tag, then will check for the SWF
	 * File with the same name as of HTML and add to EMBED Tag
	 * 
	 * @param contentData
	 * @param contentVO
	 * @return
	 */
	public  ContentDetails replaceEmbedSrcContent(String contentData,
			ContentDetails contentVO, String processingConn) {
		try {
			if (null != contentData && !"".equals(contentData)) {
				/*
				 * the src path for the SWF File, needs to be replaced , which
				 * is 00D_1a.swf this needs to be replaced with
				 * /library/MAZDAESI/Service Information/en_us/html/00D_1a.swf
				 */
				String channelFolderName = "";
				if (null != contentVO.getManualType()
						&& !"".equals(contentVO.getManualType())) {
					if (contentVO
							.getManualType()
							.trim()
							.toLowerCase()
							.equals(ApplicationProperties
									.getProperty("wiring.diagram.folder"))) {
						channelFolderName = ApplicationProperties
								.getProperty("wiring.diagram.folder.label");
					} else if (contentVO
							.getManualType()
							.trim()
							.toLowerCase()
							.equals(ApplicationProperties
									.getProperty("electronic.wiring.diagram.folder"))) {
						channelFolderName = ApplicationProperties
								.getProperty("electronic.wiring.diagram.folder.label");
					} else {
						channelFolderName = ApplicationProperties
								.getProperty("service.manuals.folder.label");
					}
				} else {
					channelFolderName = ApplicationProperties
							.getProperty("service.manuals.folder.label");
				}

				// PREPARE CHANNEL REF KEY
				String channelFolderRefKey = channelFolderName;
				channelFolderRefKey = channelFolderRefKey.replace(" ", "_");

				String localeFolderName = ApplicationProperties
						.getProperty(contentVO.getLocale().trim().toLowerCase());

				String pathToBeReplaced = "/"
						+ ApplicationProperties
								.getProperty("SERVER_LIBRARY_DIRECTORY");
				// add repository
				pathToBeReplaced = pathToBeReplaced
						+ ApplicationProperties.getProperty("REPOSITORY")
								.toUpperCase() + "/";
				// add channel Name
				pathToBeReplaced = pathToBeReplaced
						+ channelFolderRefKey.toUpperCase() + "/";
				// add locale Folder Name
				pathToBeReplaced = pathToBeReplaced
						+ localeFolderName.trim().toLowerCase() + "/";
				channelFolderRefKey = null;
				/*
				 * ADD MODEL & YEAR FOLDER NAME, ONLY WHEN - PROCESSING WIRING
				 * DIAGRAMS
				 */
				if (!channelFolderName
						.trim()
						.toLowerCase()
						.equals(ApplicationProperties
								.getProperty("service.manuals.folder.label")
								.trim().toLowerCase())) {
					// add model Folder Name
					if (null != contentVO.getModelFolderName()
							&& !"".equals(contentVO.getModelFolderName())) {
						pathToBeReplaced = pathToBeReplaced
								+ contentVO.getModelFolderName().toLowerCase()
								+ "/";
					}
					// add yearFolderName
					if (null != contentVO.getYearFolderName()
							&& !"".equals(contentVO.getYearFolderName())) {
						pathToBeReplaced = pathToBeReplaced
								+ contentVO.getYearFolderName().toLowerCase()
								+ "/";
					}
				}
				/*
				 *  add html / html5 directory name
				 *  IDENTIFICATION WILL BE DONE ON THE BASIS OF DOCUMENT TYPE
				 *  IF AVAILABLE - HTML / FLASH DOCUMENT EITHER OF THE CASES, IT IS GOING TO - html
				 * 	IF HTML5 - then html5 (this scenario will never occur, as HTML5 cannot be flash Document, no SWF Files in it)
				 * 		handling is for safe side.
				 */
//				pathToBeReplaced = pathToBeReplaced + "html";
				if (null != contentVO.getDocumentType() && !"".equals(contentVO.getDocumentType())) 
				{
					if (contentVO.getDocumentType().equals(ContentDetails.HTML_DOCUMENT)) 
					{
						pathToBeReplaced = pathToBeReplaced + "html";
					} 
					else if (contentVO.getDocumentType().equals(ContentDetails.HTML5_DOCUMENT)) 
					{
						pathToBeReplaced = pathToBeReplaced + "html5";
					}
					else
					{
						pathToBeReplaced = pathToBeReplaced + "html";
					}
				}
				else
				{
					pathToBeReplaced = pathToBeReplaced + "html";
				}

				if (null != processingConn && !"".equals(processingConn)
						&& processingConn.equals("YES")) {
					pathToBeReplaced = pathToBeReplaced
							+ "/"
							+ ApplicationProperties
									.getProperty("directory.conn");
				}

				org.jsoup.nodes.Document doc = Jsoup.parse(contentData);
				org.jsoup.select.Elements embedTagsList = doc.select("embed");
				if (null != embedTagsList && embedTagsList.size() > 0) {
					for (int i = 0; i < embedTagsList.size(); i++) {
						String swfName = embedTagsList.get(i).attr("src");
						if (null != swfName && !"".equals(swfName)
								&& !"".equals(swfName.trim())) {
							String swfFileName = "";
							if (swfName.lastIndexOf("/") != -1) {
								swfFileName = swfName.substring(
										swfName.lastIndexOf("/") + 1,
										swfName.length());
							} else {
								swfFileName = swfName;
							}

							swfName = pathToBeReplaced + "/"
									+ swfFileName.trim();
							// remove the src attribute and set the new SWF path
							embedTagsList.get(i).removeAttr("src");
							embedTagsList.get(i).removeAttr("width");
							embedTagsList.get(i).removeAttr("height");
							// add the updated src path attribute
							embedTagsList.get(i).attr("src", swfName.trim());
							embedTagsList.get(i).attr("width", "100%");
							embedTagsList.get(i).attr("height", "100%");
							swfFileName = null;
						} else {
							/*
							 * check for the SWF File with file name and
							 * extension as .swf if exists then add it to SRC
							 */
							String newSwfPath = "";
							if (null != contentVO.getFilePath()
									&& !"".equals(contentVO.getFilePath())) {
								// remove the extension from the path and add
								// .swf to it
								if (contentVO.getFilePath().lastIndexOf(".") != -1) {
									newSwfPath = contentVO.getFilePath()
											.substring(
													0,
													contentVO.getFilePath()
															.lastIndexOf("."));
									// add .swf extension
									newSwfPath = newSwfPath
											+ ApplicationProperties
													.getProperty("extension.swf");
									if (null != newSwfPath
											&& !"".equals(newSwfPath)) {
										File swfFile = PathUtil.file(newSwfPath);
										if (swfFile.exists()
												&& swfFile.isFile()) {
											if (null != contentVO.getFileName()
													&& !"".equals(contentVO
															.getFileName())) {
												if (contentVO.getFileName()
														.lastIndexOf(".") != -1) {
													// remove the extension from
													// fileName
													swfName = contentVO
															.getFileName()
															.substring(
																	0,
																	contentVO
																			.getFileName()
																			.lastIndexOf(
																					"."));
													// add .swf extension
													swfName = swfName
															+ ApplicationProperties
																	.getProperty("extension.swf");

													swfName = pathToBeReplaced
															+ "/"
															+ swfName.trim();

													// remove the src attribute
													// and set the new SWF path
													embedTagsList.get(i)
															.removeAttr("src");
													embedTagsList
															.get(i)
															.removeAttr("width");
													embedTagsList.get(i)
															.removeAttr(
																	"height");
													// add the updated src path
													// attribute
													embedTagsList.get(i).attr(
															"src",
															swfName.trim());
													embedTagsList.get(i).attr(
															"width", "100%");
													embedTagsList.get(i).attr(
															"height", "100%");
												}
											}
										}
										swfFile = null;
									}
								}
							}
							newSwfPath = null;
						}
						swfName = null;
					}
				}

				/*
				 * now search for the Object tag in the HTML Content, if found,
				 * then remove the Object Tag and replace the Object tags with
				 * the child embed tags
				 */
				org.jsoup.select.Elements objectTagsList = doc.select("object");
				if (null != objectTagsList && objectTagsList.size() > 0) {
					for (int a = 0; a < objectTagsList.size(); a++) {
						org.jsoup.select.Elements embTagList = objectTagsList
								.get(a).getElementsByTag("embed");
						if (null != embTagList && embTagList.size() > 0) {
							Element ele = embTagList.get(0);
							// now replace the Object tag with this ele
							objectTagsList.get(a).replaceWith(ele);
							ele = null;
						}
						embTagList = null;
					}
				}
				objectTagsList = null;

				if (null != doc) {
					/*
					 * Identify the EMBED TAGS FROM THE CONTENT Identify the
					 * FONT TAGS FROM THE CONTENT Identify the NAVIGATION TAGS
					 * FROM THE CONTENT
					 * 
					 * REMOVE THE UL TAG FOR HTML5 DOCUMENT REMOVE THE TABLE FOR
					 * NORMAL HTML DOCUMENT
					 */

					org.jsoup.select.Elements bodyTagsList = doc.select("body");
					if (null != bodyTagsList && bodyTagsList.size() > 0) {
						Element bodyEle = bodyTagsList.get(0);
						/*
						 * Get the Embed Tag from it
						 */
						Element embedEle = null;
						org.jsoup.select.Elements embTagList = bodyEle
								.getElementsByTag("embed");
						if (null != embTagList && embTagList.size() > 0) {
							embedEle = embTagList.get(0);
							embedEle.attr("width", "100%");
							embedEle.attr("height", "100%");

							// ALSO REMOVE THE PARENT TD & TR OF THE EMBED TAG
							// NOW PARENT TD & TR
							Element parentTRElement = null;
							Element parentTDElement = null;
							if (null != embedEle.parent()) {
								if (embedEle.parent().nodeName().equals("td")) {
									parentTDElement = embedEle.parent();
									// LOOK FOR ITS PARENT TR - IF FOUNF THEN
									// REMOVE IT
									if (null != embedEle.parent().parent()) {
										if (embedEle.parent().parent()
												.nodeName().equals("tr")) {
											parentTRElement = embedEle.parent()
													.parent();
										}
									}
								}
							}

							// remove the Embed Tag from Body
							embedEle.remove();
							if (null != parentTDElement) {
								// remove TD Element
								parentTDElement.remove();
							}
							if (null != parentTRElement) {
								// remove TR Element
								parentTRElement.remove();
							}

							parentTDElement = null;
							parentTRElement = null;
						}
						// append Embed tag to Body
						bodyEle.appendChild(embedEle);
						embedEle = null;

						/*
						 * NOW SEARCH FOR THE TABLE ELEMENT IN HTML CONTENT IF
						 * HEIGHT IS SET TO REMOVE THE HEIGHT ATTR
						 */
						Elements tableElements = bodyEle
								.getElementsByTag("table");
						if (null != tableElements && tableElements.size() > 0) {
							for (int t = 0; t < tableElements.size(); t++) {
								Element tableEle = tableElements.get(t);
								// remove Height attr
								tableEle.removeAttr("height");
							}
						}
						tableElements = null;
						// SET THE UPDATED CONTENT ON THE CONTENT VO
						contentVO.setDocumentContent(doc.toString());
					}
					bodyTagsList = null;
				}
				embedTagsList = null;
				pathToBeReplaced = null;
				doc = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return contentVO;
	}

	/**
	 * Function will read the Meta Data Content for the HTML File
	 * 
	 * @param htmlContent
	 * @param contentVO
	 */
	public  ContentDetails readMetaData(String htmlContent,
			ContentDetails contentVO) {
		org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
		org.jsoup.select.Elements metaTagsList = doc.select("meta");
		if (null != metaTagsList && metaTagsList.size() > 0) {
			for (int i = 0; i < metaTagsList.size(); i++) {
				String attrName = metaTagsList.get(i).attr("name");
				String attrContent = metaTagsList.get(i).attr("content");
				/*
				 * COMMENT READING YEAR INFROMATION FROM META TAG NOT REQUIRED
				 * HERE
				 */
				// read Model Year
				// if(attrName.trim().toLowerCase().equals("doc.model-year"))
				// {
				// String modelYear = metaTagsList.get(i).attr("content");
				// if(null!=modelYear && !"".equals(modelYear))
				// {
				// if(modelYear.indexOf("::")!=-1)
				// {
				// contentVO.setYear(modelYear.substring(modelYear.indexOf("::")+2,modelYear.length()));
				// }
				// }
				// modelYear = null;
				// }
				// else
				// if(attrName.trim().toLowerCase().equals("rhdlhdindicator"))
				if (attrName.trim().toLowerCase().equals("rhdlhdindicator")) {
					contentVO.setRhdlhdIndicator(attrContent);
				}

				// identify MAPPED VIN ENT AND VIN ATTRIBUTE ENT FILE
				if (null != attrContent && !"".equals(attrContent)
						&& !"null".equals(attrContent)) {
					if (attrContent.toLowerCase().trim().equals("vin data")) {
						contentVO.setMappedVinEntFileName(attrName);
					} else if (attrContent.toLowerCase().trim()
							.equals("vin attribute")) {
						contentVO.setMappedVinAttributeFileName(attrName);
					}
				}

				attrName = null;
				attrContent = null;
			}
		}
		return contentVO;
	}

	/**
	 * Function will update the Images Path
	 * 
	 * @param contentData
	 * @param contentVO
	 * @return
	 */
	public  String replaceImagesSrcContent(String contentData,
			ContentDetails contentVO) {
		try {
			if (null != contentData && !"".equals(contentData)) {
				String channelFolderName = "";
				if (null != contentVO.getManualType()
						&& !"".equals(contentVO.getManualType())) {
					if (contentVO
							.getManualType()
							.trim()
							.toLowerCase()
							.equals(ApplicationProperties
									.getProperty("wiring.diagram.folder"))) {
						channelFolderName = ApplicationProperties
								.getProperty("wiring.diagram.folder.label");
					} else if (contentVO
							.getManualType()
							.trim()
							.toLowerCase()
							.equals(ApplicationProperties
									.getProperty("electronic.wiring.diagram.folder"))) {
						channelFolderName = ApplicationProperties
								.getProperty("electronic.wiring.diagram.folder.label");
					} else {
						channelFolderName = ApplicationProperties
								.getProperty("service.manuals.folder.label");
					}
				} else {
					channelFolderName = ApplicationProperties
							.getProperty("service.manuals.folder.label");
				}

				String localeFolderName = ApplicationProperties
						.getProperty(contentVO.getLocale().trim().toLowerCase());

				/*
				 * /library/MAZDAESI/Service
				 * Manuals/en_us/image/ac5uuw00000002.gif this needs to be
				 * replaced with /library/MAZDAESI/Service
				 * Manuals/en_us/<year>/<model>/image/ac5uuw00000002.gif
				 */
				// PREPARE CHANNEL REF KEY
				String channelFolderRefKey = channelFolderName;
				channelFolderRefKey = channelFolderRefKey.replace(" ", "_");

				String pathToBeReplaced = "/"
						+ ApplicationProperties
								.getProperty("SERVER_LIBRARY_DIRECTORY");
				// // add repository
				pathToBeReplaced = pathToBeReplaced
						+ ApplicationProperties.getProperty("REPOSITORY")
								.toUpperCase() + "/";
				// add channel Name
				pathToBeReplaced = pathToBeReplaced
						+ channelFolderRefKey.toUpperCase() + "/";
				// add locale Folder Name
				pathToBeReplaced = pathToBeReplaced
						+ localeFolderName.toLowerCase() + "/";
				channelFolderRefKey = null;
				/*
				 * ADD MODEL AND YEAR FOLDER NAME ONLY WHEN WIRING DIAGRAMS
				 */
				if (!channelFolderName
						.trim()
						.toLowerCase()
						.equals(ApplicationProperties
								.getProperty("service.manuals.folder.label")
								.trim().toLowerCase())) {
					// add Model Folder Name
					if (null != contentVO.getModelFolderName()
							&& !"".equals(contentVO.getModelFolderName())) {
						pathToBeReplaced = pathToBeReplaced
								+ contentVO.getModelFolderName().toLowerCase()
								+ "/";
					}

					// add yearFolderName
					if (null != contentVO.getYearFolderName()
							&& !"".equals(contentVO.getYearFolderName())) {
						pathToBeReplaced = pathToBeReplaced
								+ contentVO.getYearFolderName().toLowerCase()
								+ "/";
					}
				}

				// add image directory name
				pathToBeReplaced = pathToBeReplaced + "image";
				logger.info("replaceImagesSrcContent :: Path To Be Reaplced :: > "
						+ pathToBeReplaced);
				org.jsoup.nodes.Document doc = Jsoup.parse(contentData);
				org.jsoup.select.Elements imagesTagsList = doc.select("img");
				if (null != imagesTagsList && imagesTagsList.size() > 0) {
					for (int i = 0; i < imagesTagsList.size(); i++) {
						String srcValue = imagesTagsList.get(i).attr("src");
						if (null != srcValue && !"".equals(srcValue)
								&& !"".equals(srcValue.trim())) {
							/*
							 * Get the Image Name
							 */
							String imageName = "";
							if (srcValue.lastIndexOf("/") != -1) {
								imageName = srcValue.substring(
										srcValue.lastIndexOf("/") + 1,
										srcValue.length());
							} else {
								imageName = srcValue;
							}
							imageName = pathToBeReplaced + "/" + imageName;
							// remove the src attribute and set the new image
							// path
							imagesTagsList.get(i).removeAttr("src");
							// add the updated src path attribute
							imagesTagsList.get(i).attr("src", imageName);
							imageName = null;
						}
						srcValue = null;
					}
				}

				if (null != doc) {
					contentData = doc.toString();
				}
				imagesTagsList = null;
				pathToBeReplaced = null;
				doc = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(), "replaceImagesSrcContent()", e);
		}
		return contentData;
	}

	/**
	 * Function will update the PDF Paths.
	 * 
	 * @param contentData
	 * @param contentVO
	 * @return
	 */
	public  String replacePDFPaths(String contentData,
			ContentDetails contentVO) {
		try {
			if (null != contentData && !"".equals(contentData)) {
				String channelFolderName = "";
				if (null != contentVO.getManualType()
						&& !"".equals(contentVO.getManualType())) {
					if (contentVO
							.getManualType()
							.trim()
							.toLowerCase()
							.equals(ApplicationProperties
									.getProperty("wiring.diagram.folder"))) {
						channelFolderName = ApplicationProperties
								.getProperty("wiring.diagram.folder.label");
					} else if (contentVO
							.getManualType()
							.trim()
							.toLowerCase()
							.equals(ApplicationProperties
									.getProperty("electronic.wiring.diagram.folder"))) {
						channelFolderName = ApplicationProperties
								.getProperty("electronic.wiring.diagram.folder.label");
					} else {
						channelFolderName = ApplicationProperties
								.getProperty("service.manuals.folder.label");
					}
				} else {
					channelFolderName = ApplicationProperties
							.getProperty("service.manuals.folder.label");
				}

				String localeFolderName = ApplicationProperties
						.getProperty(contentVO.getLocale().trim().toLowerCase());

				// PREPARE CHANNEL REF KEY
				String channelFolderRefKey = channelFolderName;
				channelFolderRefKey = channelFolderRefKey.replace(" ", "_");

				/*
				 * /library/MAZDAESI/Service
				 * Manuals/en_us/image/ac5uuw00000002.gif this needs to be
				 * replaced with /library/MAZDAESI/Service
				 * Manuals/en_us/<year>/<model>/image/ac5uuw00000002.gif
				 */

				String pathToBeReplaced = "/"
						+ ApplicationProperties
								.getProperty("SERVER_LIBRARY_DIRECTORY");
				// // add repository
				pathToBeReplaced = pathToBeReplaced
						+ ApplicationProperties.getProperty("REPOSITORY")
								.toUpperCase() + "/";
				// add channel Name
				pathToBeReplaced = pathToBeReplaced
						+ channelFolderRefKey.toUpperCase() + "/";
				// add locale Folder Name
				pathToBeReplaced = pathToBeReplaced
						+ localeFolderName.trim().toLowerCase() + "/";
				channelFolderRefKey = null;
				/*
				 * ADD MODEL AND YEAR FOLDER NAME ONLY WHEN WIRING DIAGRAMS
				 */
				if (!channelFolderName
						.trim()
						.toLowerCase()
						.equals(ApplicationProperties
								.getProperty("service.manuals.folder.label")
								.trim().toLowerCase())) {
					// add Model Folder Name
					if (null != contentVO.getModelFolderName()
							&& !"".equals(contentVO.getModelFolderName())) {
						pathToBeReplaced = pathToBeReplaced
								+ contentVO.getModelFolderName().toLowerCase()
								+ "/";
					}

					// add yearFolderName
					if (null != contentVO.getYearFolderName()
							&& !"".equals(contentVO.getYearFolderName())) {
						pathToBeReplaced = pathToBeReplaced
								+ contentVO.getYearFolderName().toLowerCase()
								+ "/";
					}
				}

				// add image directory name
				pathToBeReplaced = pathToBeReplaced + "pdf";

				org.jsoup.nodes.Document doc = Jsoup.parse(contentData);
				org.jsoup.select.Elements aTagsList = doc.select("a");
				if (null != aTagsList && aTagsList.size() > 0) {
					for (int i = 0; i < aTagsList.size(); i++) {
						String hrefAttr = aTagsList.get(i).attr("href");
						if (null != hrefAttr
								&& !"".equals(hrefAttr)
								&& !"#".equals(hrefAttr)
								&& !hrefAttr.startsWith("http:")
								&& !hrefAttr.startsWith("https:")
								&& hrefAttr
										.trim()
										.toLowerCase()
										.endsWith(
												ApplicationProperties
														.getProperty("extension.pdf"))) {
							// ONLY PDFS - GET THE PDF NAME FROM THE PATH
							// replace all \\ by /
							hrefAttr = hrefAttr.replace("\\", "/");
							/*
							 * Get the PDF Name
							 */
							String pdfName = "";
							if (hrefAttr.lastIndexOf("/") != -1) {
								pdfName = hrefAttr.substring(
										hrefAttr.lastIndexOf("/") + 1,
										hrefAttr.length());
							} else {
								pdfName = hrefAttr;
							}

							pdfName = pathToBeReplaced + "/" + pdfName;
							// remove the href attribute and set the new pdf
							// path
							aTagsList.get(i).removeAttr("href");
							// add the updated href path attribute
							aTagsList.get(i).attr("href", pdfName);
							pdfName = null;
						}
						hrefAttr = null;
					}
				}

				if (null != doc) {
					contentData = doc.toString();
				}
				aTagsList = null;
				pathToBeReplaced = null;
				doc = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),
					"replacePDFPaths()", e);
		}
		return contentData;
	}

	/**
	 * Function will Identify the EXACT HTML FILE DOCUMENT TYPE
	 * 
	 * @param htmlFile
	 * @param contentDetails
	 * @return
	 */
	public  ContentDetails identifyDocumentTypeForHTML(File htmlFile,
			ContentDetails contentDetails) {
		try {
			if (null != htmlFile && htmlFile.exists()) {
				/*
				 * Read Content from HTML FILE
				 */
				String htmlContent = ConversionUtils
						.getStringFromHTML(htmlFile);
				if (null != htmlContent && !"".equals(htmlContent)) {
					/*
					 * CHECK FOR THE STYLE SHEETS IF IT CONTAINS -
					 * jquery-ui.min.css THEN HTML5 DOCUMENT
					 * 
					 * DO NO CHECK FOR HTML 5 DOCUMENTWS, SINCE THE PROCESSING
					 * IS SEPARATED NOW
					 */
					org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);

					/*
					 * CHECK FOR FLASH CONTENT
					 */
					org.jsoup.select.Elements embedTagsList = doc
							.select("embed");
					if (null != embedTagsList && embedTagsList.size() > 0) {
						// DOCUMENT TYPE IS FLASH DOCUMENT
						contentDetails
								.setDocumentType(ContentDetails.FLASH_DOCUMENT);
					}
					embedTagsList = null;

					/*
					 * check here if still DOCUMENT TYPE IS NULL - THEN SET AS
					 * PLANT HTML DOCUMENT
					 */
					if (null == contentDetails.getDocumentType()
							|| "".equals(contentDetails.getDocumentType())) {
						contentDetails
								.setDocumentType(ContentDetails.HTML_DOCUMENT);
					}
					doc = null;
				}
				htmlContent = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),
					"identifyDocumentTypeForHTML()", e);
		}
		return contentDetails;
	}

	/**
	 * Function will read the BODY CONTENT AND WILL RETURN IT.
	 * 
	 * @param htmlContent
	 * @return
	 */
	public  String readBodyContent(String htmlContent) {
		try {
			if (null != htmlContent && !"".equals(htmlContent)) {
				org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
				org.jsoup.select.Elements bodyTagsList = doc.select("body");
				if (null != bodyTagsList && bodyTagsList.size() > 0) {
					Element bodyEle = bodyTagsList.get(0);
					htmlContent = bodyEle.html();
					bodyEle = null;
				}
				bodyTagsList = null;
				doc = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),
					"readBodyContent()", e);
		}
		return htmlContent;
	}

	public  ContentDetails arrangeHTML5Data(ContentDetails contentDetails) {
		try {
			if (null != contentDetails.getDocumentContent()
					&& !"".equals(contentDetails.getDocumentContent())) {
				/*
				 * READ THE FONT NODE DATA AND SET IN FONT SELECTION
				 */
				org.jsoup.nodes.Document doc = Jsoup.parse(contentDetails
						.getDocumentContent());
				org.jsoup.select.Elements fontTagsList = doc.select("font");
				if (null != fontTagsList && fontTagsList.size() > 0) {
					Element fontEle = fontTagsList.get(0);
					contentDetails.setFontContent(fontEle.html());
					fontEle.remove();
					fontEle = null;
				}

				/*
				 * SET NAVIGATION DATA & BODY DATA
				 */
//				org.jsoup.select.Elements ulTagsList = doc.select("ul");
//				StringBuilder navigationData = new StringBuilder();
//				StringBuilder contentData = new StringBuilder();
//				if (null != ulTagsList && ulTagsList.size() > 0) {
//					Element ulElement = ulTagsList.get(0);
//					if (null != ulElement) {
//						org.jsoup.select.Elements liTagslist = ulElement
//								.children();
//						if (null != liTagslist && liTagslist.size() > 0) {
//							for (int j = 0; j < liTagslist.size(); j++) {
//								Element liEle = liTagslist.get(j);
//								// CHECK FOR PREVIOUS AND NEXT ELE
//								String html = liEle.html();
//								if (html.trim().toLowerCase().contains("<a")) {
//									// NAVIGATION LINK
//									navigationData.append(html);
//								}
//
//								contentData.append(html);
//
//								html = null;
//								liEle = null;
//							}
//						}
//						liTagslist = null;
//					}
//					contentDetails.setNavigation(navigationData.toString());
//
//					/*
//					 * append contentData to body Ele
//					 */
//					org.jsoup.select.Elements bodyTagsList = doc.select("body");
//					if (null != bodyTagsList && bodyTagsList.size() > 0) {
//						Element bodyEle = bodyTagsList.get(0);
//						bodyEle.append(contentData.toString())
//								.before(ulElement);
//
//					}
//
//					ulElement.remove();
//					ulElement = null;
//				}
//				navigationData = null;
//				contentData = null;

				// remove div tags with id as - cont, class as print button with
				// id as print_go
//				org.jsoup.select.Elements divTagsList = doc.select("div");
//				if (null != divTagsList && divTagsList.size() > 0) {
//					for (int i = 0; i < divTagsList.size(); i++) {
//						Element divEle = divTagsList.get(i);
//						String idAttr = divEle.attr("id");
//						String classAttr = divEle.attr("class");
//						if (null != idAttr && !"".equals(idAttr)) {
//							if (idAttr.trim().toLowerCase().equals("cont")) {
//								// remvoe div element
//								divEle.remove();
//							}
//						}
//
//						if (null != classAttr && !"".equals(classAttr)) {
//							if (classAttr.trim().toLowerCase().equals("print")) {
//								// remove DivEle
//								divEle.remove();
//							}
//						}
//
//						idAttr = null;
//						classAttr = null;
//						divEle = null;
//					}
//				}
//				divTagsList = null;

//				// CHECK FOR BUTTON AND IF ID IS PRINT_GO remvoe it
//				org.jsoup.select.Elements buttonTagsList = doc.select("button");
//				if (null != buttonTagsList && buttonTagsList.size() > 0) {
//					for (int i = 0; i < buttonTagsList.size(); i++) {
//						Element buttonEle = buttonTagsList.get(i);
//						String idAttr = buttonEle.attr("id");
//						if (null != idAttr && !"".equals(idAttr)) {
//							if (idAttr.trim().toLowerCase().equals("print_go")) {
//								buttonEle.remove();
//							}
//						}
//						idAttr = null;
//						buttonEle = null;
//					}
//				}
//				buttonTagsList = null;

				if (null != doc) {
					contentDetails.setDocumentContent(doc.toString());
				}
				doc = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),
					"arrangeHTML5Data()", e);
		}
		return contentDetails;
	}

	public  ContentDetails arrangeHTMLData(ContentDetails contentDetails) {
		try {
			if (null != contentDetails.getDocumentContent()
					&& !"".equals(contentDetails.getDocumentContent())) {
				/*
				 * READ THE FONT NODE DATA AND SET IN FONT SELECTION
				 */
				org.jsoup.nodes.Document doc = Jsoup.parse(contentDetails
						.getDocumentContent());
				org.jsoup.select.Elements fontTagsList = doc.select("font");
				if (null != fontTagsList && fontTagsList.size() > 0) {
					Element fontEle = fontTagsList.get(0);
					contentDetails.setFontContent(fontEle.html());
					fontEle.remove();
					fontEle = null;
				}

				if (null != doc) {
					contentDetails.setDocumentContent(doc.toString());
				}
				doc = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),
					"arrangeHTMLData()", e);
		}
		return contentDetails;
	}

	/**
	 * Function will remove all the DEAD LINKS FROM HTML CONTENT
	 * 
	 * @param htmlContent
	 * @return
	 */
	public  String convertDeadLinksToText(String htmlContent) {
		try {
			if (null != htmlContent && !"".equals(htmlContent)) {
				org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
				org.jsoup.select.Elements aTagsList = doc.select("a");
				if (null != aTagsList && aTagsList.size() > 0) {
					for (int a = 0; a < aTagsList.size(); a++) {
						Element aElement = aTagsList.get(a);
						String hrefAttr = aElement.attr("href");
						if (null != hrefAttr
								&& !"".equals(hrefAttr)
								&& !"#".equals(hrefAttr)
								&& !hrefAttr.startsWith("#")
								&& !hrefAttr.startsWith("http:")
								&& !hrefAttr.startsWith("https:")
								&& !hrefAttr
										.trim()
										.toLowerCase()
										.endsWith(
												ApplicationProperties
														.getProperty("extension.pdf"))) {
							if (hrefAttr.startsWith("#")) {
								// replace this A ELEMENT WITH A CUSTOM ELEMENT
								// DEAD_LINK AND
								// THE ONCE ALL THE LINKS ARE CONVERTED, REMOVE
								// THESE OPENING AND CLOSING TAGS
								Element deadLinkElement = doc
										.createElement("DEAD_LINKS");
								deadLinkElement.text(aElement.html());
								aElement.replaceWith(deadLinkElement);
								deadLinkElement = null;
							}
						}
						hrefAttr = null;
						aElement = null;
					}
				}
				aTagsList = null;

				if (null != doc) {
					htmlContent = doc.toString();

					/*
					 * REPLACE ALL <DEAD_LINKS> AND </DEAD_LINKS>
					 */
					htmlContent = htmlContent.replace("<DEAD_LINKS>", "");
					htmlContent = htmlContent.replace("</DEAD_LINKS>", "");

					htmlContent = htmlContent.replace("<dead_links>", "");
					htmlContent = htmlContent.replace("</dead_links>", "");
				}
				doc = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(WiringDiagramUtils.class.getName(),
					"convertDeadLinksToText()", e);
		}
		return htmlContent;
	}
}
