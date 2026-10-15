package com.mazda.gms3.mdm.rmitool.utils;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoup.Jsoup;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.w3c.dom.Node;

import com.mazda.gms3.mdm.dao.KapturePublishedContentDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.rmitool.vo.RMIExportToolAttachmentDetails;
import com.mazda.gms3.mdm.rmitool.vo.RMIExportToolInnerLinkDetails;
import com.mazda.gms3.mdm.rmitool.vo.RMIExportToolItemDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.FileReadWriteUtil;
import com.mazda.gms3.mdm.utils.ParseXMLDoc;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.ScheduleConstants;

public class RMIExportToolUtils {

	private Logger logger = LogManager.getLogger(RMIExportToolUtils.class);

	/*
	 * BELOW - getSeriesFolderName / appendZeros / getDocumentFile - IS THE INFOMANAGER FOLDER
	 * ALGORITHM AND IS NO LONGER ON THE LIVE PATH. It is reachable only from
	 * getDocumentXMLFile_BackUp, which was already unused before this migration.
	 *
	 * KEPT DELIBERATELY, not deleted: it is the only record of how InfoManager bucketed these
	 * folders, and until CD Creation and RMI are both proven on VDI that is worth being able to
	 * read. Delete it once this screen is signed off there.
	 */
	private String getSeriesFolderName(String seriesValue, int endPosition)
	{
		String seriesFolderName=null;
		try
		{
			if(null!=seriesValue && !"".equals(seriesValue))
			{
				// always read from 0 position
				String prefix = seriesValue.substring(0, endPosition);
				seriesFolderName = appendZeros(prefix, seriesValue.length()-prefix.length());
				prefix = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "getSeriesFolderName()", e);
		}
		return seriesFolderName;
	}
	
	private String appendZeros(String prefix, int noOfZeros)
	{
		String value=prefix;
		for(int a=0;a<noOfZeros;a++)
		{
			value+="0";
		}
		return value;
	}
	
	/**
	 * THE DOCUMENT'S PUBLISHED XML FILE.
	 *
	 * WAS: a walk of the InfoManager live tree - channelFolder/seriesFolder/DOCID/locale - where
	 * seriesFolder was the document number with a widening prefix kept and the rest zeroed
	 * (1000000, then 1600000, then 1610000 ...), RETRYING until a directory happened to exist.
	 * NOW: KapturePublishedContentDAO. Kapture buckets by one arithmetic step instead of that
	 * search, and - the change that actually matters - it adds a VERSION level below the locale.
	 * A version cannot be guessed from the path, so finding the file now begins with a k_article
	 * read for the newest PUBLISHED version. The file NAME is unchanged: still content_*.xml,
	 * still matched by pattern because the name carries the CHANNEL's contentid, not the
	 * document's.
	 *
	 * SHARED WITH CD CREATION ON PURPOSE. Both screens ask the same question - which version of
	 * this document is published, and where is its XML - and a second copy here would be free to
	 * drift from the one CD Creation uses.
	 *
	 * @param channelFolderPath IGNORED, and kept only so the call site did not have to change.
	 *                          The channel folder is no longer composed by the caller: Kapture
	 *                          names it from the document id prefix (SM1027 -> SM), not from the
	 *                          channel name (SERVICE_MANUALS), and the root now comes from
	 *                          kapture.content.xml.root rather than rmitool.live.folder.path.
	 */
	public File getDocumentXMLFile(String channelFolderPath,String documentId,String locale)
	{
		File xmlFile = null;
		try
		{
			xmlFile = KapturePublishedContentDAO.findContentXmlFile(documentId, locale);
			if(null==xmlFile)
			{
				logger.info("getDocumentXMLFile :: No published content XML found for {"+documentId+"} / {"+locale+"}.");
			}
			else
			{
				logger.info("getDocumentXMLFile :: {"+documentId+"} / {"+locale+"} resolved to {"+xmlFile.getAbsolutePath()+"}");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "getDocumentXMLFile()", e);
		}
		return xmlFile;
	}

	private File getDocumentFile(String channelFolderPath,String documentId,String locale, String seriesValue, int endPosition)
	{
		File xmlFile = null;
		try
		{
			String seriesFolderName = getSeriesFolderName(seriesValue, endPosition);
			if(null!=seriesFolderName && !"".equals(seriesFolderName))
			{
				/*
				 * prepare content....xml path for the document, which will be
				 * channelFolderPath/seriesFolder/documentIdFolder/localeFolder
				 */
				String path = channelFolderPath;
				if(!path.endsWith("/"))
				{
					path+="/";
				}
				path+=seriesFolderName+"/"+documentId.trim().toUpperCase()+"/"+locale;
				File localeDir = new File(path);
				if(localeDir.exists() && localeDir.isDirectory())
				{
					logger.info("getDocumentXMLFile :: Document Directory exists at Path {"+path+"} For {"+documentId+"} where SeriesFolder identified was :: >"+ seriesFolderName);
					File[] filesList = localeDir.listFiles(); 
					if(null!=filesList && filesList.length>0)
					{
						for(int e=0;e<filesList.length;e++)
						{
							if(filesList[e].exists() && filesList[e].isFile()) 
							{
								if(filesList[e].getName().trim().toLowerCase().startsWith("content_") && filesList[e].getName().trim().toLowerCase().endsWith(".xml"))
								{
									// DOCUMENT XML FILE FOUND
									xmlFile = filesList[e];
									break;
								}
							}
						}
					}
					filesList = null;
				}
				else
				{
					logger.info("getDocumentXMLFile :: Document Directory at Path {"+path+"} does not exist For {"+documentId+"} where SeriesFolder identified was :: >"+ seriesFolderName);
					if(endPosition < seriesValue.length())
					{
						// recursive call function to prepare different seriesFolderPath
						xmlFile = getDocumentFile(channelFolderPath, documentId, locale, seriesValue, (endPosition+1));
					}
				}
				localeDir = null;
				path = null;
			}
			seriesFolderName = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "getDocumentFile()", e);
		}
		return xmlFile;
	}
	
	
	
	/**
	 * Below Function Iterate the complete Channel Folder directory 
	 * 		e.g. iterate all Series Folders to identify the directory for Document ID
	 * 	With heavy data, this function was degrading the performance, specially in case of SM Documents
	 * @param channelFolderPath
	 * @param documentId
	 * @param locale
	 * @return
	 */
	public File getDocumentXMLFile_BackUp(String channelFolderPath, String documentId, String locale)
	{
		File xmlFile = null;
		try
		{
			locale = locale.replace("-", "_");
			File channelFolder = new File(channelFolderPath);
			if(channelFolder.exists() && channelFolder.isDirectory())
			{
				File[] seriesFoldersList = channelFolder.listFiles();
				if(null!=seriesFoldersList && seriesFoldersList.length>0)
				{
					File seriesFolder = null;
					File[] documentFoldersList = null;
					boolean documentFolderFound = false;
					File[] localeFoldersList = null;
					File[] filesList = null;
					for(int b=0;b<seriesFoldersList.length;b++)
					{
						seriesFolder = seriesFoldersList[b];
						if(seriesFolder.exists() && seriesFolder.isDirectory())
						{
							documentFoldersList = seriesFolder.listFiles();
							if(null!=documentFoldersList && documentFoldersList.length>0)
							{
								for(int c=0;c<documentFoldersList.length;c++)
								{
									if(documentFoldersList[c].getName().trim().toLowerCase().equals(documentId.trim().toLowerCase()))
									{
										/*
										 * Document Folder Found
										 */
										documentFolderFound = true;
										localeFoldersList = documentFoldersList[c].listFiles();
										if(null!=localeFoldersList && localeFoldersList.length>0)
										{
											for(int d=0;d<localeFoldersList.length;d++)
											{
												if(localeFoldersList[d].getName().trim().toLowerCase().equals(locale.trim().toLowerCase()))
												{
													/*
													 * Locale Folder Found
													 */
													filesList = localeFoldersList[d].listFiles();
													if(null!=filesList && filesList.length>0)
													{
														for(int e=0;e<filesList.length;e++)
														{
															if(filesList[e].exists() && filesList[e].isFile()) 
															{
																if(filesList[e].getName().trim().toLowerCase().startsWith("content_") && filesList[e].getName().trim().toLowerCase().endsWith(".xml"))
																{
																	// DOCUMENT XML FILE FOUND
																	xmlFile = filesList[e];
																	break;
																}
															}
														}
													}
													filesList = null;
													break;
												}
											}
										}
										localeFoldersList = null;
										break;
									}
								}
							}
							documentFoldersList = null;
						}

						if(documentFolderFound==true)
						{
							// break the parent seriesFolder loop as well
							break;
						}
						seriesFolder = null;
					}
				}
				seriesFoldersList = null;
			}
			channelFolder = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "getDocumentXMLFile()", e);
		}
		finally
		{
			channelFolderPath = null;
			documentId = null;
			locale = null;
		}
		return xmlFile;
	}
	
	/**
	 * THE EXPORTED DOCUMENT XML - Kapture's file with everything internal taken out and the
	 * categories annotated.
	 *
	 * THE CONTAINER IS THE ROOT ELEMENT, addressed as getDocumentElement(). It used to be found
	 * with getElementsByTagName("CONTENT").item(0), which worked only because InfoManager's
	 * container element was called CONTENT. Kapture's is &lt;content&gt; - LOWERCASE - and DOM tag
	 * matching is case sensitive, so that expression now matches the RICH TEXT &lt;CONTENT&gt; inside
	 * the channel node instead. Every strip would have removed nothing, and the categories block
	 * would have been appended INSIDE the document's HTML. Both failures are silent, which is why
	 * this is spelled out here.
	 *
	 * @param channelName SERVICE_INFORMATION / SERVICE_MANUALS / OTHER_SERVICE_MANUALS /
	 *                    WIRING_DIAGRAMS / VIDEOS / TRAINING - still the CHANNEL NODE's name inside
	 *                    the file, which Kapture did not change. It is no longer the folder name.
	 */
	public String getXMLFilteredData(File xmlFile, String channelName)
	{
		String xmlData = null;
		try
		{
			Document document = ParseXMLDoc.parseFile(xmlFile.getAbsolutePath());
			if(null!=document)
			{
				/*
				 * 1. STRIP THE INTERNAL NODES.
				 * The container-level list is Kapture's equivalent of InfoManager's
				 * SECURITY / AUTHOR / OWNER / LASTMODIFIER / RESOURCEPATH set; the CHANNEL-level
				 * lists are unchanged, because those nodes live inside the channel node and
				 * Kapture kept their names.
				 */
				document = removeContainerNodes(document);

				if(channelName.equals("SERVICE_INFORMATION"))
				{
					document = removeServiceInformationNodes(document, channelName);
				}
				else if(channelName.equals("SERVICE_MANUALS") || channelName.equals("OTHER_SERVICE_MANUALS"))
				{
					document = removeServiceManualNodes(document, channelName);
				}
				else
				{
					// FOR TRAINING / VIDEOS / WIRING DIAGRAMS
					document = removeOtherChannelNodes(document, channelName);
				}

				if(null!=document)
				{
					/*
					 * 2. COLLECT THE CATEGORIES, AND DROP THE UNSELECTED ONES.
					 * A Kapture document lists EVERY category in its trees and flags the ones
					 * actually mapped with <selected>true</selected>. Only those are the document's
					 * categories; exporting the rest would claim mappings that do not exist.
					 */
					List<Map<String, String>> categoryList = collectSelectedCategories(document);

					if(null!=document)
					{
						/*
						 * 3. TRANSFORM, RE-PARSE, THEN APPEND THE ANNOTATED CATEGORIES BLOCK.
						 * The round trip through a String is how this always worked - kept as it
						 * was rather than rewritten, so the output is built the same way.
						 */
						xmlData= Utilities.transformString(document);

						DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
						InputSource is = new InputSource();
						is.setCharacterStream(new StringReader(xmlData));
						Document doc = db.parse(is);
						if(null!=doc)
						{
							Element container = doc.getDocumentElement();
							if(null!=container)
							{
								Element categoriesElement = doc.createElement("CATEGORIES");
								if(null!=categoryList && categoryList.size()>0)
								{
									for(int r=0;r<categoryList.size();r++)
									{
										Map<String, String> dataMap = categoryList.get(r);
										Element categoryElement = doc.createElement("CATEGORY");
										/*
										 * THE ELEMENT SET IS UNCHANGED, INCLUDING THE TWO KAPTURE
										 * CANNOT FILL. GUID and OBJECTID were InfoManager's
										 * identifiers and have no counterpart - they are emitted
										 * EMPTY rather than dropped, so the exported schema stays
										 * exactly what the recipient already parses.
										 */
										appendCData(doc, categoryElement, "NAME", dataMap.get("NAME"));
										appendCData(doc, categoryElement, "REFERENCE_KEY", dataMap.get("REFERENCE_KEY"));
										appendCData(doc, categoryElement, "GUID", dataMap.get("GUID"));
										appendCData(doc, categoryElement, "OBJECTID", dataMap.get("OBJECTID"));
										appendCData(doc, categoryElement, "TYPE", dataMap.get("TYPE"));
										categoriesElement.appendChild(categoryElement);
										categoryElement = null;
										dataMap = null;
									}
								}
								container.appendChild(categoriesElement);
								categoriesElement = null;
							}
							container = null;
							xmlData= Utilities.transformString(doc);
						}
						db = null;
						is = null;
						doc = null;
					}
					categoryList = null;
				}
			}
			document = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "getXMLFilteredData()", e);
		}
		finally
		{
			xmlFile = null;
		}
		return xmlData;
	}

	/**
	 * JOINS THE OK ASSET ROOT TO A PATH FOUND IN THE CONTENT, with exactly one separator.
	 *
	 * The two halves come from different places and neither can be relied on for its slashes: the
	 * root is a property somebody edits per environment, and the path is whatever Kapture wrote
	 * into the src or href. Kapture serves assets one context deeper than InfoManager did, so these
	 * paths now start /content/library/MAZDA/... where they used to start /library/MAZDA/... , and
	 * a plain concatenation only happens to work while exactly one side carries the slash.
	 *
	 * THE PATH IS OTHERWISE UNTOUCHED - no context is added or stripped here. If the /content/
	 * segment ever has to be added or removed, that belongs in the property, not in code.
	 */
	private String resolveAssetPath(String assetRoot, String pathInContent)
	{
		String root = (null == assetRoot ? "" : assetRoot.trim());
		String path = (null == pathInContent ? "" : pathInContent.trim());
		while (root.endsWith("/") || root.endsWith("\\"))
		{
			root = root.substring(0, root.length() - 1);
		}
		while (path.startsWith("/") || path.startsWith("\\"))
		{
			path = path.substring(1);
		}
		return root + "/" + path;
	}

	/** One CDATA-valued child element. A missing value becomes an EMPTY element, never a missing one. */
	private void appendCData(Document doc, Element parent, String name, String value)
	{
		Element child = doc.createElement(name);
		child.appendChild(doc.createCDATASection(null==value ? "" : value));
		parent.appendChild(child);
	}

	/**
	 * THE ARRAYS KAPTURE KEEPS CATEGORIES IN.
	 *
	 * TWO, NOT ONE - the ESI tree is held separately from everything else, and both are read. ESI
	 * is a master tree like any other (it is in rmitool.master.category.refkeys), so an ESI mapping
	 * belongs in the exported CATEGORIES exactly as a VIN or CARLINE mapping does. Reading only
	 * articlelistcategory would drop every ESI mapping out of the export without a word.
	 */
	private static final String[] KAPTURE_CATEGORY_ARRAYS = { "articlelistcategory", "articleesicategory" };

	/**
	 * THE DOCUMENT'S MAPPED CATEGORIES, taken from Kapture's category arrays, with the unselected
	 * entries REMOVED FROM THE DOCUMENT as it goes.
	 *
	 * SHAPE CHANGE. InfoManager wrote &lt;CATEGORY&gt; with NAME / REFERENCE_KEY / GUID / OBJECTID, and
	 * the category's TYPE was worked out from the OBJECTID's numeric prefix. Kapture has no object
	 * ids at all - it writes categoryRefKey / categoryName / masterCategoryRefKey / selected, and
	 * masterCategoryRefKey names the tree the category belongs to directly. So the type lookup
	 * matches on the REF KEY, against rmitool.master.category.refkeys, which was already in the
	 * properties file as the parallel list to the object ids.
	 *
	 * SELECTED IS THE FILTER, IN BOTH ARRAYS. Kapture lists every category in the trees a document
	 * touches and marks only the mapped ones selected=true. Anything else is a tree entry, not a
	 * mapping, and exporting it would claim a mapping that does not exist.
	 */
	private List<Map<String, String>> collectSelectedCategories(Document document)
	{
		List<Map<String, String>> categoryList = null;
		try
		{
			for(int c=0;c<KAPTURE_CATEGORY_ARRAYS.length;c++)
			{
				String arrayName = KAPTURE_CATEGORY_ARRAYS[c];
				NodeList categoryNodesList = document.getElementsByTagName(arrayName);
				if(null==categoryNodesList || categoryNodesList.getLength()<=0)
				{
					continue;
				}

				/*
				 * COPIED OUT BEFORE ANYTHING IS REMOVED. getElementsByTagName returns a LIVE
				 * NodeList - removing a node while iterating it shifts every later index and
				 * silently skips entries.
				 */
				List<Node> nodes = new ArrayList<Node>();
				for(int a=0;a<categoryNodesList.getLength();a++)
				{
					nodes.add(categoryNodesList.item(a));
				}
				categoryNodesList = null;

				for(int a=0;a<nodes.size();a++)
				{
					Node categoryNode = nodes.get(a);
					if(null==categoryNode || "#text".equalsIgnoreCase(categoryNode.getNodeName()))
					{
						continue;
					}
					Map<String,String> dataMap = new HashMap<String,String>();
					boolean selected = false;
					NodeList childNodesList = categoryNode.getChildNodes();
					if(null!=childNodesList && childNodesList.getLength()>0)
					{
						for(int b=0;b<childNodesList.getLength();b++)
						{
							Node childNode = childNodesList.item(b);
							if("#text".equalsIgnoreCase(childNode.getNodeName()))
							{
								continue;
							}
							String childValue = Utilities.getCharacterDataFromElement((Element)childNode);
							if(childNode.getNodeName().equalsIgnoreCase("categoryName"))
							{
								dataMap.put("NAME", childValue);
							}
							else if(childNode.getNodeName().equalsIgnoreCase("categoryRefKey"))
							{
								dataMap.put("REFERENCE_KEY", childValue);
							}
							else if(childNode.getNodeName().equalsIgnoreCase("masterCategoryRefKey"))
							{
								// NAMES THE TREE - this is what the TYPE is resolved from
								dataMap.put("TYPE", identifyCategoryType(childValue));
							}
							else if(childNode.getNodeName().equalsIgnoreCase("selected"))
							{
								selected = (null!=childValue && "true".equalsIgnoreCase(childValue.trim()));
							}
						}
					}
					childNodesList = null;

					if(selected==true)
					{
						if(null==categoryList)
						{
							categoryList = new ArrayList<Map<String,String>>();
						}
						categoryList.add(dataMap);
					}

					/*
					 * THE RAW ENTRY IS REMOVED EITHER WAY - THIS IS A CONVERSION, NOT A FILTER.
					 *
					 * An unselected entry is a tree entry, not a mapping, and has no business in
					 * the export at all. A SELECTED one is not lost by being removed here: it
					 * becomes a <CATEGORY> inside the <CATEGORIES> block appended by the caller,
					 * carrying more than the raw array did (a resolved TYPE). Leaving it as well
					 * would ship the same mapping twice, in two different vocabularies.
					 *
					 * THIS IS WHAT INFOMANAGER DID. It removed the raw CATEGORIES node from the
					 * container and appended the rebuilt one, so the recipient only ever parsed
					 * <CATEGORIES><CATEGORY>. Kapture's articlelistcategory / articleesicategory
					 * arrays are the raw form here, so they go the same way and the exported shape
					 * is unchanged.
					 */
					if(null!=categoryNode.getParentNode())
					{
						categoryNode.getParentNode().removeChild(categoryNode);
					}
					dataMap = null;
					categoryNode = null;
				}
				nodes = null;
				arrayName = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "collectSelectedCategories()", e);
		}
		return categoryList;
	}

	/**
	 * NODES REMOVED FROM EVERY EXPORTED DOCUMENT, whatever its channel.
	 *
	 * KAPTURE'S EQUIVALENT OF InfoManager's SECURITY / AUTHOR / AUTHORID / OWNER / OWNERID /
	 * LASTMODIFIER / LASTMODIFIERID / RESOURCEPATH. Those names no longer appear in the file, so
	 * the old list would have matched nothing and the export would have carried internal author
	 * names, user ids and email addresses out of the building - the exact thing the strip exists to
	 * prevent. The replacement list was given by the user.
	 *
	 * EVERY OCCURRENCE IS REMOVED, not just the first. The old removeNodes took item(0) only,
	 * which was survivable when each node appeared once; several of these repeat.
	 *
	 * articlelistusergroup IS ON THE LIST AS InfoManager's SECURITY. It names who may see the
	 * document - CORPORATE_CONTENT_KEY, TECHNICIAN_CONTENT_KEY, DEFAULT_USER_GROUP - which is
	 * internal access control and was stripped for EVERY channel before. articlelistview is the
	 * other half of that pair, VIEWS, but it is NOT here because it was channel-specific; see
	 * removeServiceManualNodes / removeOtherChannelNodes.
	 *
	 * NOT ON THE LIST, AND THAT IS DELIBERATE: the contentid ATTRIBUTE on the root element (only
	 * the &lt;contentId&gt; ELEMENT is named), and the category arrays, which are not stripped but
	 * CONVERTED into the CATEGORIES block by collectSelectedCategories().
	 */
	private static final String[] KAPTURE_CONTAINER_NODES_TO_REMOVE = {
			"articlelistusergroup",
			"WORKFLOW_STEP_NAME",
			"articleOwner",
			"categoriesTreeRefKeys",
			"ISARTICLE",
			"articleCreator",
			"categoriesESITreeRefKeys",
			"emailId",
			"contentId",
			"modifiedBy",
			"crawlType",
			"userLocale",
			"categoriesTreeLabels",
			"createdBy",
			"articleCreatorUserID",
			"status" };

	private Document removeContainerNodes(Document document)
	{
		try
		{
			for(int a=0;a<KAPTURE_CONTAINER_NODES_TO_REMOVE.length;a++)
			{
				document = removeAllNodes(document, KAPTURE_CONTAINER_NODES_TO_REMOVE[a]);
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "removeContainerNodes()", e);
		}
		return document;
	}

	/**
	 * Removes EVERY element with this name, wherever it sits.
	 *
	 * THE LIVE NodeList IS COPIED FIRST - removing while iterating one shifts the indices and skips
	 * half the matches.
	 */
	private Document removeAllNodes(Document document, String nodeName)
	{
		try
		{
			NodeList list = document.getElementsByTagName(nodeName);
			if(null!=list && list.getLength()>0)
			{
				List<Node> nodes = new ArrayList<Node>();
				for(int a=0;a<list.getLength();a++)
				{
					nodes.add(list.item(a));
				}
				for(int a=0;a<nodes.size();a++)
				{
					Node node = nodes.get(a);
					if(null!=node && null!=node.getParentNode())
					{
						node.getParentNode().removeChild(node);
					}
					node = null;
				}
				nodes = null;
			}
			list = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "removeAllNodes()", e);
		}
		return document;
	}

	private Document removeServiceInformationNodes(Document document, String channelName)
	{
		try
		{
			/*
			 *  o	SECURITY
				o	<AUTHOR>ok admin</AUTHOR><AUTHORID>59c17c214a1643b589661ec625ec11a5</AUTHORID>
				o	<OWNER>Srikanth reddy Koguru</OWNER><OWNERID>1718fcabe51a4bd69c547ff3d05b7394</OWNERID>
				o	<LASTMODIFIER>Carina Becker</LASTMODIFIER><LASTMODIFIERID>0ecf3b437b774a05bcc7d7369463cf43</LASTMODIFIERID>
				o	RESOURCEPATH � or do we need it for the pictures?
				o	LEGACY_ID
				o	SI_HISTORY
				o	SHOW_AS_NEWS_FOR_WEEKS
				o	MC_AUTHORIZATION_NUMBER
				o	The attachments, which are not for dealers (I do not know the tag) is it OTHER_REFERENCE_FILE_ATTACHMENT/? Or CAMPAIGN_ATTACHMENTS?
						ANSWER FOR ABOVE ONE IS OTHER_REFERENCE_FILE_ATTACHMENT
				o	MD_LETTER_ATTACHMENT
				o	SIGNATURE
				o	TI_NUMBER
				o	SA_NUMBER
				o	RECALL_NUMBERS
				o	TI_SUMMARY
				o	INTERNAL_DOCUMENT_REFERENCE_NUMBER
			 */
			/*
			 * THE CONTAINER-LEVEL STRIP MOVED TO removeContainerNodes(). The names that used
			 * to be listed here - SECURITY, AUTHOR, OWNER, LASTMODIFIER, RESOURCEPATH, VIEWS -
			 * are InfoManager's and do not exist in a Kapture document; its equivalents are
			 * stripped for every channel, so keeping a per-channel copy would only let the
			 * lists drift. WHAT STAYS HERE IS THE CHANNEL-LEVEL STRIP BELOW, unchanged,
			 * because those nodes live inside the channel node and Kapture kept their names.
			 */

			String[] channelNodes="LEGACY_ID,SI_HISTORY,SHOW_AS_NEWS_FOR_WEEKS,MC_AUTHORIZATION_NUMBER,OTHER_REFERENCE_FILE_ATTACHMENT,MD_LETTER_ATTACHMENT,SIGNATURE,TI_NUMBER,SA_NUMBER,RECALL_NUMBERS,TI_SUMMARY,INTERNAL_DOCUMENT_REFERENCE_NUMBER".split(",");
			for(int a=0;a<channelNodes.length;a++)
			{
				document = removeNodes(document, channelNodes[a], true, channelName);
			}
			channelNodes = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "removeServiceInformationNodes()", e);
		}
		return document;
	}

	private Document removeServiceManualNodes(Document document, String channelName)
	{
		try
		{
			/*
			 *  o	VIEWS
				o	SECURITY
				o	<AUTHOR>ok admin</AUTHOR><AUTHORID>cccd726f825b455ab54eb71e2f5162d4</AUTHORID>
					<OWNER>ok admin</OWNER><OWNERID>cccd726f825b455ab54eb71e2f5162d4</OWNERID>
					<LASTMODIFIER>ok admin</LASTMODIFIER><LASTMODIFIERID>cccd726f825b455ab54eb71e2f5162d4</LASTMODIFIERID>
				o	VTOC_FILENAME
				o	SIE_ID
				o	OASIS_FILE_LOCATION

			 */
			/*
			 * THE CONTAINER-LEVEL STRIP MOVED TO removeContainerNodes(). The names that used
			 * to be listed here - SECURITY, AUTHOR, OWNER, LASTMODIFIER, RESOURCEPATH, VIEWS -
			 * are InfoManager's and do not exist in a Kapture document; its equivalents are
			 * stripped for every channel, so keeping a per-channel copy would only let the
			 * lists drift. WHAT STAYS HERE IS THE CHANNEL-LEVEL STRIP BELOW, unchanged,
			 * because those nodes live inside the channel node and Kapture kept their names.
			 */

			/*
			 * VIEWS -> articlelistview. Stripped here and in removeOtherChannelNodes, but NOT in
			 * removeServiceInformationNodes - SERVICE_INFORMATION's original strip list omitted
			 * VIEWS while every other channel's included it. That difference is deliberate in the
			 * original and is kept.
			 */
			document = removeAllNodes(document, "articlelistview");

			String[] channelNodes="VTOC_FILENAME,SIE_ID,OASIS_FILE_LOCATION".split(",");
			for(int a=0;a<channelNodes.length;a++)
			{
				document = removeNodes(document, channelNodes[a], true, channelName);
			}
			channelNodes = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "removeServiceManualNodes()", e);
		}
		return document;
	}

	private Document removeOtherChannelNodes(Document document, String channelName)
	{
		try
		{
			/*
			 *  o	VIEWS
				o	SECURITY
				o	<AUTHOR>ok admin</AUTHOR><AUTHORID>cccd726f825b455ab54eb71e2f5162d4</AUTHORID>
					<OWNER>ok admin</OWNER><OWNERID>cccd726f825b455ab54eb71e2f5162d4</OWNERID><LASTMODIFIER>ok admin</LASTMODIFIER>
					<LASTMODIFIERID>cccd726f825b455ab54eb71e2f5162d4</LASTMODIFIERID>
			 */
			/*
			 * The container-level half of this method moved to removeContainerNodes(), which runs
			 * for every channel. What is left is VIEWS.
			 */
			/*
			 * VIEWS -> articlelistview. Stripped here and in removeOtherChannelNodes, but NOT in
			 * removeServiceInformationNodes - SERVICE_INFORMATION's original strip list omitted
			 * VIEWS while every other channel's included it. That difference is deliberate in the
			 * original and is kept.
			 */
			document = removeAllNodes(document, "articlelistview");
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "removeOtherChannelNodes()", e);
		}
		return document;
	}


	private Document removeNodes(Document document, String nodeName, boolean isChannelNode,String channelName)
	{
		try
		{
			if(isChannelNode==false)
			{
				// NODE TO BE REMOVED FROM CONTENT NODE
				NodeList contentNodesList = document.getElementsByTagName("CONTENT");
				if(null!=contentNodesList && contentNodesList.getLength()>0)
				{
					Node contentNode = (Node)contentNodesList.item(0);
					NodeList itemsTobeDeletedNodesList = document.getElementsByTagName(nodeName);
					if(null!=itemsTobeDeletedNodesList && itemsTobeDeletedNodesList.getLength()>0)
					{
						Node itemTobeDeletedNode = (Node)itemsTobeDeletedNodesList.item(0);
						// remove from root Node
						if(null!=contentNode && null!=itemTobeDeletedNode)
						{
							contentNode.removeChild(itemTobeDeletedNode);
						}
						itemTobeDeletedNode  = null;
					}
					itemsTobeDeletedNodesList = null;
					contentNode = null;
				}
				contentNodesList = null;
			}
			else if(isChannelNode==true)
			{
				// NODE TO BE REMOVED FROM CHANNEL NODE
				NodeList channelNodesList = document.getElementsByTagName(channelName);
				if(null!=channelNodesList && channelNodesList.getLength()>0)
				{
					Node channelNode = (Node)channelNodesList.item(0);
					NodeList itemsTobeDeletedNodesList = document.getElementsByTagName(nodeName);
					if(null!=itemsTobeDeletedNodesList && itemsTobeDeletedNodesList.getLength()>0)
					{
						Node itemTobeDeletedNode = (Node)itemsTobeDeletedNodesList.item(0);
						// remove from channelNode
						if(null!=channelNode && null!=itemTobeDeletedNode)
						{
							channelNode.removeChild(itemTobeDeletedNode);
						}
						itemTobeDeletedNode  = null;
					}
					itemsTobeDeletedNodesList = null;
					channelNode = null;
				}
				channelNodesList = null;
			}

		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "removeNodes()", e);
		}
		return document;
	}

	/**
	 * Function will create desired directory structure in the destination path
	 * 
	 * @param tempPath
	 * @return
	 */
	public String createFolderStructure(String tempPath,String serverPath) 
	{
		int i = 0;
		File file;
		String token;
		String path = null;
		try
		{
			path = serverPath;
			StringTokenizer str = new StringTokenizer(tempPath, "/");
			while (str.hasMoreTokens()) 
			{
				i = i + 1;
				if (i > 2) {
					token = str.nextToken();
					if (path.substring(path.length() - 1, path.length())
							.equalsIgnoreCase("/")) {
						path = path + token;
					} else {
						path = path + "/" + token;
					}
					file = new File(path);
					if (null != file && file.isDirectory()) {
						continue;
					} else {
						file.mkdir();
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "createFolderStructure()", e);
		}
		return path;
	}

	public ArrayList<RMIExportToolAttachmentDetails> readServiceInformationAttachmentsList(String xmlFileData, String sourceDirPath, RMIExportToolItemDetails itemDetails)
	{
		ArrayList<RMIExportToolAttachmentDetails> attachmentsList = null;
		try
		{
			sourceDirPath = sourceDirPath.replace("\\", "/");
			if(!sourceDirPath.endsWith("/"))
			{
				sourceDirPath = sourceDirPath+"/";
			}
			/*
			 * Convert XML String Data to Document Node and read the below Attachment Nodes
			 * <ATTACHMENTS>
                 <ATTACHMENT_TITLE SECURITY="TECHNICIAN"/>
                 <ATTACHMENT/>
                 <PDF_ATTACHMENTS>
                    <LEGACY_PDF SECURITY="TECHNICIAN"/>
                    <REVISION_HISTORY/>
                    <OTHER_PDF SECURITY="TECHNICIAN"/>
                 </PDF_ATTACHMENTS>
              </ATTACHMENTS>

              <CAMPAIGN_ATTACHMENTS>
                 <CAMPAIGN_ATTACHMENT_TITLE SECURITY="CORPORATE"/>
                 <CAMPAIGN_ATTACHMENT SECURITY="CORPORATE"/>
              </CAMPAIGN_ATTACHMENTS>
			 */
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xmlFileData));
			Document doc = db.parse(is);
			String name = null;
			if(null!=doc)
			{
				NodeList attachmentNodesList = doc.getElementsByTagName("ATTACHMENTS");
				if(null!=attachmentNodesList && attachmentNodesList.getLength()>0)
				{
					Node attachmentNode = null;
					NodeList childNodesList = null;
					Node childNode = null;
					NodeList pdfNodesList = null;
					Node pdfNode = null;
					for(int a=0;a<attachmentNodesList.getLength();a++)
					{
						attachmentNode = (Node)attachmentNodesList.item(a);
						if (!"#text".equalsIgnoreCase(attachmentNode.getNodeName()))
						{
							childNodesList = attachmentNode.getChildNodes();
							if(null!=childNodesList && childNodesList.getLength()>0)
							{
								childNode = null;
								for(int b=0;b<childNodesList.getLength();b++)
								{
									childNode = (Node)childNodesList.item(b);
									if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
									{
										if(childNode.getNodeName().equalsIgnoreCase("ATTACHMENT"))
										{
											name = Utilities.getCharacterDataFromElement((Element)childNode);
											if(null!=name && !"".equals(name))
											{
												attachmentsList = addAttachmentDetails(attachmentsList, name, itemDetails, sourceDirPath);
											}
											name = null;
										}
										else if(childNode.getNodeName().equalsIgnoreCase("PDF_ATTACHMENTS"))
										{
											pdfNodesList = childNode.getChildNodes();
											if(null!=pdfNodesList && pdfNodesList.getLength()>0)
											{
												pdfNode = null;
												for(int c=0;c<pdfNodesList.getLength();c++)
												{
													pdfNode = (Node)pdfNodesList.item(c);
													if (!"#text".equalsIgnoreCase(pdfNode.getNodeName()))
													{
														if(pdfNode.getNodeName().equalsIgnoreCase("LEGACY_PDF") || pdfNode.getNodeName().equalsIgnoreCase("REVISION_HISTORY")  || pdfNode.getNodeName().equalsIgnoreCase("OTHER_PDF") )
														{
															name = Utilities.getCharacterDataFromElement((Element)pdfNode);
															if(null!=name && !"".equals(name))
															{
																attachmentsList = addAttachmentDetails(attachmentsList, name, itemDetails, sourceDirPath);
															}
															name = null;
														}
													}
													pdfNode = null;
												}
											}
											pdfNodesList = null;
										}
									}
									childNode = null;
								}
							}
							childNodesList = null;
						}
						attachmentNode = null;
					}

					attachmentNode = null;
					childNodesList = null;
					childNode = null;
					pdfNodesList = null;
					pdfNode = null;
				}
				attachmentNodesList = null;
				name = null;
				/*
				 * NOW CAMPAIGN_ATTACHMENTS
				 */
				NodeList campaignAttachmentsNodesList = doc.getElementsByTagName("CAMPAIGN_ATTACHMENTS");
				if(null!=campaignAttachmentsNodesList && campaignAttachmentsNodesList.getLength()>0)
				{
					Node campaignNode = null;
					for(int a=0;a<campaignAttachmentsNodesList.getLength();a++)
					{
						campaignNode = (Node)campaignAttachmentsNodesList.item(a);
						if (!"#text".equalsIgnoreCase(campaignNode.getNodeName()))
						{
							if(campaignNode.getNodeName().equalsIgnoreCase("CAMPAIGN_ATTACHMENT"))
							{
								name = Utilities.getCharacterDataFromElement((Element)campaignNode);
								if(null!=name && !"".equals(name))
								{
									attachmentsList = addAttachmentDetails(attachmentsList, name, itemDetails, sourceDirPath);
								}
								name = null;
							}
						}
						campaignNode = null;
					}
				}	
				campaignAttachmentsNodesList = null;
			}
			doc = null;
			is = null;
			db = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "readServiceInformationAttachmentsList()", e);
		}
		return attachmentsList;
	}

	public ArrayList<RMIExportToolAttachmentDetails> readServiceManualsAttachmentsList(String xmlFileData, String sourceDirPath, RMIExportToolItemDetails itemDetails)
	{
		ArrayList<RMIExportToolAttachmentDetails> attachmentsList = null;
		try
		{
			sourceDirPath = sourceDirPath.replace("\\", "/");
			if(!sourceDirPath.endsWith("/"))
			{
				sourceDirPath = sourceDirPath+"/";
			}
			/*
			 * Convert XML String Data to Document Node and read the below Attachment Nodes
			 * 	  <ATTACHMENTS>
                     <ATTACHMENT_TITLE />
                     <ATTACHMENT />
                  </ATTACHMENTS>
			 */
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xmlFileData));
			Document doc = db.parse(is);
			String name = null;
			if(null!=doc)
			{
				NodeList attachmentNodesList = doc.getElementsByTagName("ATTACHMENTS");
				if(null!=attachmentNodesList && attachmentNodesList.getLength()>0)
				{
					Node attachmentNode = null;
					NodeList childNodesList = null;
					Node childNode = null;
					for(int a=0;a<attachmentNodesList.getLength();a++)
					{
						attachmentNode = (Node)attachmentNodesList.item(a);
						if (!"#text".equalsIgnoreCase(attachmentNode.getNodeName()))
						{
							childNodesList = attachmentNode.getChildNodes();
							if(null!=childNodesList && childNodesList.getLength()>0)
							{
								childNode = null;
								for(int b=0;b<childNodesList.getLength();b++)
								{
									childNode = (Node)childNodesList.item(b);
									if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
									{
										if(childNode.getNodeName().equalsIgnoreCase("ATTACHMENT"))
										{
											name = Utilities.getCharacterDataFromElement((Element)childNode);
											if(null!=name && !"".equals(name))
											{
												attachmentsList = addAttachmentDetails(attachmentsList, name, itemDetails, sourceDirPath);
											}
											name = null;
										}
									}
									childNode = null;
								}
							}
							childNodesList = null;
						}
						attachmentNode = null;
					}

					attachmentNode = null;
					childNodesList = null;
					childNode = null;
				}
				attachmentNodesList = null;
				name = null;
			}
			doc = null;
			is = null;
			db = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "readServiceManualsAttachmentsList()", e);
		}
		return attachmentsList;
	}

	public ArrayList<RMIExportToolAttachmentDetails> readWiringDiagramsAttachmentsList(String xmlFileData, String sourceDirPath, RMIExportToolItemDetails itemDetails)
	{
		ArrayList<RMIExportToolAttachmentDetails> attachmentsList = null;
		try
		{
			sourceDirPath = sourceDirPath.replace("\\", "/");
			if(!sourceDirPath.endsWith("/"))
			{
				sourceDirPath = sourceDirPath+"/";
			}
			/*
			 * Convert XML String Data to Document Node and read the below Attachment Nodes
			 * 	  <ATTACHMENT />
			 */
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xmlFileData));
			Document doc = db.parse(is);
			String name = null;
			if(null!=doc)
			{
				NodeList channelNodesList = doc.getElementsByTagName("WIRING_DIAGRAMS");
				if(null!=channelNodesList && channelNodesList.getLength()>0)
				{
					Node channelNode = null;
					NodeList childNodesList = null;
					Node childNode = null;
					for(int a=0;a<channelNodesList.getLength();a++)
					{
						channelNode = (Node)channelNodesList.item(a);
						if (!"#text".equalsIgnoreCase(channelNode.getNodeName()))
						{
							childNodesList = channelNode.getChildNodes();
							if(null!=childNodesList && childNodesList.getLength()>0)
							{
								childNode = null;
								for(int b=0;b<childNodesList.getLength();b++)
								{
									childNode = (Node)childNodesList.item(b);
									if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
									{
										if(childNode.getNodeName().equalsIgnoreCase("ATTACHMENT"))
										{
											name = Utilities.getCharacterDataFromElement((Element)childNode);
											if(null!=name && !"".equals(name))
											{
												attachmentsList = addAttachmentDetails(attachmentsList, name, itemDetails, sourceDirPath);
											}
											name = null;
										}
									}
									childNode = null;
								}
							}
							childNodesList = null;
						}
						channelNode = null;
					}

					channelNode = null;
					childNodesList = null;
					childNode = null;
				}
				channelNodesList = null;
				name = null;
			}
			doc = null;
			is = null;
			db = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "readWiringDiagramsAttachmentsList()", e);
		}
		return attachmentsList;
	}

	public ArrayList<RMIExportToolAttachmentDetails> readVideosAttachmentsList(String xmlFileData, String sourceDirPath, RMIExportToolItemDetails itemDetails)
	{
		ArrayList<RMIExportToolAttachmentDetails> attachmentsList = null;
		try
		{
			sourceDirPath = sourceDirPath.replace("\\", "/");
			if(!sourceDirPath.endsWith("/"))
			{
				sourceDirPath = sourceDirPath+"/";
			}
			/*
			 * Convert XML String Data to Document Node and read the below Attachment Nodes
			 * 	  <THUMBNAIL_FILE />
                  <VIDEO_FILE />
			 */
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(xmlFileData));
			Document doc = db.parse(is);
			String name = null;
			if(null!=doc)
			{
				NodeList channelNodesList = doc.getElementsByTagName("VIDEOS");
				if(null!=channelNodesList && channelNodesList.getLength()>0)
				{
					Node channelNode = null;
					NodeList childNodesList = null;
					Node childNode = null;
					for(int a=0;a<channelNodesList.getLength();a++)
					{
						channelNode = (Node)channelNodesList.item(a);
						if (!"#text".equalsIgnoreCase(channelNode.getNodeName()))
						{
							childNodesList = channelNode.getChildNodes();
							if(null!=childNodesList && childNodesList.getLength()>0)
							{
								childNode = null;
								for(int b=0;b<childNodesList.getLength();b++)
								{
									childNode = (Node)childNodesList.item(b);
									if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
									{
										if(childNode.getNodeName().equalsIgnoreCase("THUMBNAIL_FILE") || childNode.getNodeName().equalsIgnoreCase("VIDEO_FILE"))
										{
											name = Utilities.getCharacterDataFromElement((Element)childNode);
											if(null!=name && !"".equals(name))
											{
												attachmentsList = addAttachmentDetails(attachmentsList, name, itemDetails, sourceDirPath);
											}
											name = null;
										}
									}
									childNode = null;
								}
							}
							childNodesList = null;
						}
						channelNode = null;
					}

					channelNode = null;
					childNodesList = null;
					childNode = null;
				}
				channelNodesList = null;
				name = null;
			}
			doc = null;
			is = null;
			db = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "readVideosAttachmentsList()", e);
		}
		return attachmentsList;
	}

	private ArrayList<RMIExportToolAttachmentDetails> addAttachmentDetails(ArrayList<RMIExportToolAttachmentDetails> attachmentsList, String name, RMIExportToolItemDetails itemDetails, String sourceDirPath)
	{
		try
		{
			RMIExportToolAttachmentDetails attachmentDetails = new RMIExportToolAttachmentDetails();
			attachmentDetails.setItemDetails(new RMIExportToolItemDetails());
			attachmentDetails.setItemDetails(itemDetails);
			/*
			 * replace %20 in name by space
			 */
			name = name.replace("%20", " ");
			attachmentDetails.setName(name);
			attachmentDetails.setSourcePath(sourceDirPath+attachmentDetails.getName());
			// ATTACHMENT TYPE WILL BE ATTACHMENT
			attachmentDetails.setAttachmentType("ATTACHMENT");
			/*
			 * DESTINATION PATH WILL BE - DOCUMENT DIR PATH
			 */
			attachmentDetails.setDestinationPath(itemDetails.getDocumentDirPath());
			/*
			 * XCOPY ATTACHMENT FILE
			 */
			attachmentDetails = xcopyOperation(attachmentDetails);
			// add to attachmentsList
			if(null==attachmentsList || attachmentsList.size()<=0)
			{
				attachmentsList=new ArrayList<RMIExportToolAttachmentDetails>();
			}
			attachmentsList.add(attachmentDetails);
			attachmentDetails = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "addAttachmentDetails()", e);
		}
		return attachmentsList;
	}

	public RMIExportToolAttachmentDetails xcopyOperation(RMIExportToolAttachmentDetails attachmentDetails)
	{
		boolean readFileInBytes = false;
		try
		{
			File sourceFile = new File(attachmentDetails.getSourcePath());
			if(sourceFile.exists() && sourceFile.isFile())
			{
				File destinationFile = new File(attachmentDetails.getDestinationPath());
				Runtime r = Runtime.getRuntime();
				/*Process p = r
						.exec("xcopy " + "\""
								+ sourceFile.getAbsolutePath() + "\""
								+ " " + "\"" + destinationFile
								+ "\"/y/c/h/j/q/z");*/
				// use robocopy
				Process p = r.exec("robocopy \""+sourceFile.getParentFile()+"\" \""+destinationFile+"\" \""+sourceFile.getName()+"\" ");
				p.waitFor();
				p.destroy();
				
//				FileUtils.copyFileToDirectory(sourceFile, destinationFile);

				/*
				 * check if file has been moved successfully or not
				 */
				String tempPath = attachmentDetails.getDestinationPath();
				if(!tempPath.endsWith("/"))
				{
					tempPath+="/";
				}
				tempPath=tempPath+attachmentDetails.getName();
				File tFile = new File(tempPath);
				if(tFile.exists() && tFile.isFile())
				{
					logger.info("xcopyOperation :: File {"+attachmentDetails.getName()+"} moved successfully from {"+ attachmentDetails.getSourcePath()+"} to {"+attachmentDetails.getDestinationPath()+"}." );
					// set Processing Status to Success
					attachmentDetails.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
				}
				else
				{
					/*
					 * SINCE XCOPY HAS FAILED - 
					 * TRY READING FILE FROM SOURCE LOCATION IN BYTES
					 * AND WRITE FILE AT THE DESTINATION LOCATION AND THEN RE-CHECK IF EXISTS OR NOT
					 * SET READ FILE IN BYTES TO TRUE
					 */
					readFileInBytes = true;
					
					logger.info("xcopyOperation :: Failed to XCOPY file {"+attachmentDetails.getName()+"} from {"+ attachmentDetails.getSourcePath()+"} to {"+attachmentDetails.getDestinationPath()+"}." );
					logger.info("xcopyOperation :: Proceed for reading source File in Bytes and Write it on Destination Location.");
					// set Processing Status to Failure
//					attachmentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
//					attachmentDetails.setErrorMessage("FAILED TO MOVE FILE TO DESTINATION LOCATION. PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
				}
				tFile = null;
				destinationFile = null;
			}
			else
			{
				attachmentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
//				attachmentDetails.setErrorMessage("FILE DOES NOT EXIST AT SOURCE PATH.");
				attachmentDetails.setErrorMessage("FAILED TO FIND "+attachmentDetails.getAttachmentType()+" INSIDE MGSS SERVER.");
			}
			sourceFile = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "xcopyOperation()", e);
			
			/*
			 * SINCE XCOPY HAS FAILED - 
			 * TRY READING FILE FROM SOURCE LOCATION IN BYTES
			 * AND WRITE FILE AT THE DESTINATION LOCATION AND THEN RE-CHECK IF EXISTS OR NOT
			 * SET READ FILE IN BYTES TO TRUE
			 */
			readFileInBytes = true;
			logger.info("xcopyOperation :: Proceed for reading source File in Bytes and Write it on Destination Location.");
			// set Processing Status to Failure
//			attachmentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
//			attachmentDetails.setErrorMessage("UNABLE TO PERFORM XCOPY OPERATION. ERROR :: >"+ e.getMessage()+". PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
		}
		
		if(readFileInBytes==true)
		{
			try
			{
				byte[] data = FileReadWriteUtil.readFile(attachmentDetails.getSourcePath());
				if(null!=data && data.length>0)
				{
					boolean reverifyFlag =false;
					String tempPath = attachmentDetails.getDestinationPath();
					if(!tempPath.endsWith("/"))
					{
						tempPath+="/";
					}
					tempPath=tempPath+attachmentDetails.getName();
					boolean writeFlag = FileReadWriteUtil.writeFile(data, tempPath);
					if(writeFlag==true)
					{
						try
						{
							File revrifyFile = new File(tempPath);
							if(revrifyFile.exists() && revrifyFile.isFile())
							{
								// file Written Successfully.
								reverifyFlag=true;
							}
							revrifyFile = null;
						}
						catch(Exception e)
						{
							Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "xcopyOperation()", e);
						}
					}
					tempPath = null;
					
					if(writeFlag==true && reverifyFlag==true)
					{
						// set Processing Status to Success
						attachmentDetails.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
					}
					else
					{
						logger.info("xcopyOperation :: Failed to COPY file {"+attachmentDetails.getName()+"} from {"+ attachmentDetails.getSourcePath()+"} to {"+attachmentDetails.getDestinationPath()+"}." );
						// set Processing Status to Failure
						attachmentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
						attachmentDetails.setErrorMessage("FAILED TO MOVE FILE TO DESTINATION LOCATION. PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
					}
				}
				else
				{
					attachmentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
//					attachmentDetails.setErrorMessage("FILE DOES NOT EXIST AT SOURCE PATH.");
					attachmentDetails.setErrorMessage("FAILED TO FIND "+attachmentDetails.getAttachmentType()+" INSIDE MGSS SERVER.");
				}
				data = null;
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "xcopyOperation()", e);
				// set Processing Status to Failure
				attachmentDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
				attachmentDetails.setErrorMessage("UNABLE TO PERFORM XCOPY OPERATION. ERROR :: >"+ e.getMessage()+". PLEASE CONTACT IT ADMIN SUPPORT FOR MORE DETAIL.");
			}
		}
		return attachmentDetails;
	}

	public ArrayList<RMIExportToolAttachmentDetails> readInlineImages(String xmlFileData, RMIExportToolItemDetails itemDetails)
	{
		ArrayList<RMIExportToolAttachmentDetails> imagesList = null;
		try
		{
			/*
			 * Prepare Destination Path, which Will be 
			 * //m4okfs10/okassetsdev/library/MAZDA/GMS3_CUSTOM/RMI_EXPORT_DIR/ + <ScheduleCode> + 
			 * /OkAssets/library/MAZDA/CHANNEL_NAME/LOCALE/image/
			 */
			String rmiToolWorkingDirPath=ApplicationProperties.getProperty("rmitool.working.dir.folder.path");
			// add scheduleId
			String tempDestinationDirPath=String.valueOf(itemDetails.getScheduleId());
			// add OkassestDestDirSubPath
			String okAssetsDirPath = ApplicationProperties.getProperty("rmitool.okassets.dir.sub.path");
			if(!okAssetsDirPath.startsWith("/"))
			{
				okAssetsDirPath="/"+okAssetsDirPath;
			}
			tempDestinationDirPath = tempDestinationDirPath+okAssetsDirPath;
			if(!tempDestinationDirPath.endsWith("/"))
			{
				tempDestinationDirPath +="/";
			}
			String localeDir = itemDetails.getLocale().replace("-", "_");
			// add channelName/localeDir/image
			tempDestinationDirPath=tempDestinationDirPath+itemDetails.getChannelName()+"/"+localeDir+"/"+ApplicationProperties.getProperty("rmitool.image.dir.name");

			/*
			 * CREATE DESTINATION FOLDER STRUCTURE
			 */
			String destinationNetworkPath = createFolderStructure(tempDestinationDirPath, rmiToolWorkingDirPath);
			if(null==destinationNetworkPath || "".equals(destinationNetworkPath))
			{
				// SET TO EMPTY IF ANY ISSUE OCCURS WHILE CREATING FOLDER STRUCTURE
				destinationNetworkPath="";
			}

			/*
			 * Prepare Path to Be replaced Path
			 * /OkAssets/library/MAZDA/CHANNEL_NAME/LOCALE/image/<image_name>
			 */
			String pathToBeReplaced=okAssetsDirPath;
			if(!pathToBeReplaced.endsWith("/"))
			{
				pathToBeReplaced+="/";
			}
			// add Channel/localeDir/image
			pathToBeReplaced+=itemDetails.getChannelName()+"/"+localeDir+"/"+ApplicationProperties.getProperty("rmitool.image.dir.name");
			if(!pathToBeReplaced.endsWith("/"))
			{
				pathToBeReplaced+="/";
			}
			okAssetsDirPath = null;
			localeDir = null;
			/*
			 * IDENITFY THE CONTENT FOR RICH TEXT NODES - BASED ON CHANNELS 
			 * Identify all IMG Tags, read their SRC Values
			 * 		Ensure, SRC Value is not null, not starts with http:// or https:// or #
			 */

			String[] richTextContentNodes=null;
			if(itemDetails.getChannelName().equals("SERVICE_INFORMATION"))
			{
				richTextContentNodes = "SI_HISTORY,TI_SUMMARY,BULLETIN_NOTES,DESCRIPTION,REPAIR_PROCEDURE,CALIBRATION,PARTS_INFORMATION,WARRANTY_INFORMATION".split(",");
			}
			else if(itemDetails.getChannelName().equals("SERVICE_MANUALS") || itemDetails.getChannelName().equals("OTHER_SERVICE_MANUALS"))
			{
				richTextContentNodes ="CONTENT".split(",");
			}
			else if(itemDetails.getChannelName().equals("TRAINING"))
			{
				richTextContentNodes ="DESCRIPTION,ETAS_LINK".split(",");
			}
			else if(itemDetails.getChannelName().equals("WIRING_DIAGRAMS"))
			{
				richTextContentNodes ="HTML_CONTENT,FLASH_CONTENT".split(",");
			}
			// NO RICH TEXT NODE FOR VIDEOS

			if(null!=richTextContentNodes && richTextContentNodes.length>0)
			{
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(xmlFileData));
				Document document = db.parse(is);
				NodeList richTextNodesList = null;
				Node richTextNode = null;
				String htmlContent = null;

				String sourceOkAssetsPath = ApplicationProperties.getProperty("rmitool.okassets.source.dir.path");
				org.jsoup.nodes.Document doc = null;
				org.jsoup.select.Elements imagesTagsList = null;
				RMIExportToolAttachmentDetails imagesDetails = null;
				String srcValue = null;
				for(int t=0;t<richTextContentNodes.length;t++)
				{
					richTextNodesList = document.getElementsByTagName(richTextContentNodes[t]);
					if(null!=richTextNodesList && richTextNodesList.getLength()>0)
					{
						for(int r=0;r<richTextNodesList.getLength();r++)
						{
							richTextNode = (Node)richTextNodesList.item(r);
							if(!"#text".equalsIgnoreCase(richTextNode.getNodeName()))
							{
								htmlContent = Utilities.getCharacterDataFromElement((Element)richTextNode);
								if(null!=htmlContent && !"".equals(htmlContent))
								{
									/*
									 * CONVERT THIS TO HTML DOC AND START IDENITFYING INLINE IMAGES
									 */
									doc = Jsoup.parse(htmlContent);
									imagesTagsList = doc.select("img");
									if(null!=imagesTagsList && imagesTagsList.size()>0)
									{
										srcValue = null;
										imagesDetails = null;
										for(int a=0;a<imagesTagsList.size();a++)
										{
											srcValue = imagesTagsList.get(a).attr("src");
											if (null != srcValue && !"".equals(srcValue)
													&& !"".equals(srcValue.trim()) && !srcValue.startsWith("#")  
													&& !srcValue.trim().toLowerCase().startsWith("http://")  
													&& !srcValue.trim().toLowerCase().startsWith("https://")) 
											{
												imagesDetails  = new RMIExportToolAttachmentDetails();
												/*
												 * replace %20 in srcValue by space
												 */
												srcValue = srcValue.replace("%20", " ");
												// SAMPLE SRC PATH = /library/MAZDA/SERVICE MANUALS/en_us/cx-5/1a11-1u-12b_xml/image/ac5wzw00002111.png
												imagesDetails.setName(srcValue.substring(srcValue.lastIndexOf("/")+1, srcValue.length()));
												imagesDetails.setSourcePath(resolveAssetPath(sourceOkAssetsPath, srcValue));
												imagesDetails.setDestinationPath(destinationNetworkPath);
												// set SRC Path
												imagesDetails.setSrcHrefPath(srcValue);
												// set Path TO be replaced = pathToBeReplaced+imageName
												imagesDetails.setPathToBeReplaced(pathToBeReplaced+imagesDetails.getName());
												imagesDetails.setAttachmentType("IMAGE");
												// set ITEM Details
												imagesDetails.setItemDetails(new RMIExportToolItemDetails());
												imagesDetails.setItemDetails(itemDetails);
												/*
												 * PERFORM XCOPY OPERATION
												 */
												imagesDetails=  xcopyOperation(imagesDetails);
												// add to imagesList
												if(null==imagesList || imagesList.size()<=0)
												{
													imagesList = new ArrayList<RMIExportToolAttachmentDetails>();
												}
												imagesList.add(imagesDetails);
												imagesDetails = null;
											}
											srcValue = null;
										}
									}

									doc  =null;
									imagesTagsList = null;
								}
								htmlContent = null;
								richTextNode = null;
							}
						}
					}
					richTextNodesList = null;
				}

				srcValue = null;
				doc = null;
				imagesDetails = null;
				imagesDetails = null;
				htmlContent = null;
				richTextNodesList = null;
				richTextNode = null;
				db = null;
				is = null;
				document = null;
				sourceOkAssetsPath = null;
			}
			richTextContentNodes = null;
			pathToBeReplaced = null;
			destinationNetworkPath = null;
			tempDestinationDirPath = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "readInlineImages()", e);
		}
		return imagesList;
	}

	public ArrayList<RMIExportToolAttachmentDetails> readInlinePDFs(String xmlFileData, RMIExportToolItemDetails itemDetails)
	{
		ArrayList<RMIExportToolAttachmentDetails> pdfsList = null;
		try
		{
			/*
			 * Prepare Destination Path, which Will be 
			 * //m4okfs10/okassetsdev/library/MAZDA/GMS3_CUSTOM/RMI_EXPORT_DIR/ + <ScheduleCode> + 
			 * /OkAssets/library/MAZDA/CHANNEL_NAME/LOCALE/pdf/
			 */
			String rmiToolWorkingDirPath=ApplicationProperties.getProperty("rmitool.working.dir.folder.path");
			// add scheduleId
			String tempDestinationDirPath=String.valueOf(itemDetails.getScheduleId());
			// add OkassestDestDirSubPath
			String okAssetsDirPath = ApplicationProperties.getProperty("rmitool.okassets.dir.sub.path");
			if(!okAssetsDirPath.startsWith("/"))
			{
				okAssetsDirPath="/"+okAssetsDirPath;
			}
			tempDestinationDirPath = tempDestinationDirPath+okAssetsDirPath;
			if(!tempDestinationDirPath.endsWith("/"))
			{
				tempDestinationDirPath +="/";
			}
			String localeDir = itemDetails.getLocale().replace("-", "_");
			// add channelName/localeDir/pdf
			tempDestinationDirPath=tempDestinationDirPath+itemDetails.getChannelName()+"/"+localeDir+"/"+ApplicationProperties.getProperty("rmitool.pdf.dir.name");

			/*
			 * CREATE DESTINATION FOLDER STRUCTURE
			 */
			String destinationNetworkPath = createFolderStructure(tempDestinationDirPath, rmiToolWorkingDirPath);
			if(null==destinationNetworkPath || "".equals(destinationNetworkPath))
			{
				// SET TO EMPTY IF ANY ISSUE OCCURS WHILE CREATING FOLDER STRUCTURE
				destinationNetworkPath="";
			}

			/*
			 * Prepare Path to Be replaced Path
			 * /OkAssets/library/MAZDA/CHANNEL_NAME/LOCALE/pdf/<pdf_name>
			 */
			String pathToBeReplaced=okAssetsDirPath;
			if(!pathToBeReplaced.endsWith("/"))
			{
				pathToBeReplaced+="/";
			}
			// add Channel/localeDir/pdf
			pathToBeReplaced=itemDetails.getChannelName()+"/"+localeDir+"/"+ApplicationProperties.getProperty("rmitool.pdf.dir.name");
			if(!pathToBeReplaced.endsWith("/"))
			{
				pathToBeReplaced+="/";
			}
			okAssetsDirPath = null;
			localeDir = null;
			/*
			 * IDENITFY THE CONTENT FOR RICH TEXT NODES - BASED ON CHANNELS
			 * Identify all <A> Tags, read their HREF Values
			 * 		Ensure, HEF Value is not null, not starts with http:// or https:// or # AND MUST END WITH .pdf
			 */
			String[] richTextContentNodes=null;
			if(itemDetails.getChannelName().equals("SERVICE_INFORMATION"))
			{
				richTextContentNodes = "SI_HISTORY,TI_SUMMARY,BULLETIN_NOTES,DESCRIPTION,REPAIR_PROCEDURE,CALIBRATION,PARTS_INFORMATION,WARRANTY_INFORMATION".split(",");
			}
			else if(itemDetails.getChannelName().equals("SERVICE_MANUALS") || itemDetails.getChannelName().equals("OTHER_SERVICE_MANUALS"))
			{
				richTextContentNodes ="CONTENT".split(",");
			}
			else if(itemDetails.getChannelName().equals("TRAINING"))
			{
				richTextContentNodes ="DESCRIPTION,ETAS_LINK".split(",");
			}
			else if(itemDetails.getChannelName().equals("WIRING_DIAGRAMS"))
			{
				richTextContentNodes ="HTML_CONTENT,FLASH_CONTENT".split(",");
			}
			// NO RICH TEXT NODE FOR VIDEOS

			if(null!=richTextContentNodes && richTextContentNodes.length>0)
			{
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(xmlFileData));
				Document document = db.parse(is);
				NodeList richTextNodesList = null;
				Node richTextNode = null;
				String htmlContent = null;

				String sourceOkAssetsPath = ApplicationProperties.getProperty("rmitool.okassets.source.dir.path");
				org.jsoup.nodes.Document doc = null;
				org.jsoup.select.Elements aTagsList = null;
				RMIExportToolAttachmentDetails pdfDetails = null;
				String hrefValue = null;
				for(int t=0;t<richTextContentNodes.length;t++)
				{
					richTextNodesList = document.getElementsByTagName(richTextContentNodes[t]);
					if(null!=richTextNodesList && richTextNodesList.getLength()>0)
					{
						for(int r=0;r<richTextNodesList.getLength();r++)
						{
							richTextNode = (Node)richTextNodesList.item(r);
							if(!"#text".equalsIgnoreCase(richTextNode.getNodeName()))
							{
								htmlContent = Utilities.getCharacterDataFromElement((Element)richTextNode);
								if(null!=htmlContent && !"".equals(htmlContent))
								{
									/*
									 * CONVERT THIS TO HTML DOC AND START IDENITFYING INLINE IMAGES
									 */
									doc = Jsoup.parse(htmlContent);
									aTagsList = doc.select("a");
									if(null!=aTagsList && aTagsList.size()>0)
									{
										hrefValue = null;
										pdfDetails = null;
										for(int a=0;a<aTagsList.size();a++)
										{
											hrefValue = aTagsList.get(a).attr("href");
											if (null != hrefValue && !"".equals(hrefValue) && !"".equals(hrefValue.trim()) &&
													!hrefValue.startsWith("#") && !hrefValue.trim().toLowerCase().startsWith("http://") 
													&& !hrefValue.trim().toLowerCase().startsWith("https://")  
													&& hrefValue.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf")))
											{
												/*
												 * replace %20 in hrefValue by space
												 */
												hrefValue = hrefValue.replace("%20", " ");
												pdfDetails  = new RMIExportToolAttachmentDetails();
												// SAMPLE SRC PATH = /library/MAZDA/SERVICE MANUALS/en_us/cx-5/1a11-1u-12b_xml/pdf/pbc.pdf
												pdfDetails.setName(hrefValue.substring(hrefValue.lastIndexOf("/")+1, hrefValue.length()));
												pdfDetails.setSourcePath(resolveAssetPath(sourceOkAssetsPath, hrefValue));
												pdfDetails.setDestinationPath(destinationNetworkPath);
												// set SRC Path
												pdfDetails.setSrcHrefPath(hrefValue);
												// set Path TO be replaced = pathToBeReplaced+pdfName
												pdfDetails.setPathToBeReplaced(pathToBeReplaced+pdfDetails.getName());
												pdfDetails.setAttachmentType("PDF");
												// set ITEM Details
												pdfDetails.setItemDetails(new RMIExportToolItemDetails());
												pdfDetails.setItemDetails(itemDetails);
												/*
												 * PERFORM XCOPY OPERATION
												 */
												pdfDetails=  xcopyOperation(pdfDetails);
												// add to imagesList
												if(null==pdfsList || pdfsList.size()<=0)
												{
													pdfsList = new ArrayList<RMIExportToolAttachmentDetails>();
												}
												pdfsList.add(pdfDetails);
												pdfDetails = null;
											}
											hrefValue = null;
										}
									}

									doc  =null;
									aTagsList = null;
								}
								htmlContent = null;
								richTextNode = null;
							}
						}
					}
					richTextNodesList = null;
				}

				hrefValue = null;
				doc = null;
				aTagsList = null;
				pdfDetails = null;
				htmlContent = null;
				richTextNodesList = null;
				richTextNode = null;
				db = null;
				is = null;
				document = null;
				sourceOkAssetsPath = null;
			}
			richTextContentNodes = null;
			pathToBeReplaced = null;
			destinationNetworkPath = null;
			tempDestinationDirPath = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "readInlineImages()", e);
		}
		return pdfsList;
	}

	public ArrayList<RMIExportToolInnerLinkDetails> readInnerLinks(String xmlFileData, RMIExportToolItemDetails itemDetails,String actualWriteFolderPath)
	{
		ArrayList<RMIExportToolInnerLinkDetails> innerLinksList = null;
		try
		{
			/*
			 * IDENITFY THE CONTENT FOR RICH TEXT NODES - BASED ON CHANNELS
			 * Identify all <A> Tags, read their HREF Values
			 * 		Ensure, HEF Value is not null, not starts with http:// or https:// or # AND MUST NOT END WITH .pdf
			 * 		And contains INNER LINK URL = index?page=content&id=
			 */
			String[] richTextContentNodes=null;
			if(itemDetails.getChannelName().equals("SERVICE_INFORMATION"))
			{
				richTextContentNodes = "SI_HISTORY,TI_SUMMARY,BULLETIN_NOTES,DESCRIPTION,REPAIR_PROCEDURE,CALIBRATION,PARTS_INFORMATION,WARRANTY_INFORMATION".split(",");
			}
			else if(itemDetails.getChannelName().equals("SERVICE_MANUALS") || itemDetails.getChannelName().equals("OTHER_SERVICE_MANUALS"))
			{
				richTextContentNodes ="CONTENT".split(",");
			}
			else if(itemDetails.getChannelName().equals("TRAINING"))
			{
				richTextContentNodes ="DESCRIPTION,ETAS_LINK".split(",");
			}
			else if(itemDetails.getChannelName().equals("WIRING_DIAGRAMS"))
			{
				richTextContentNodes ="HTML_CONTENT,FLASH_CONTENT".split(",");
			}
			// NO RICH TEXT NODE FOR VIDEOS

			if(null!=richTextContentNodes && richTextContentNodes.length>0)
			{
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(xmlFileData));
				Document document = db.parse(is);
				NodeList richTextNodesList = null;
				Node richTextNode = null;
				String htmlContent = null;

				String innerLinkContent=ApplicationProperties.getProperty("rmitool.innerlink.url");
				org.jsoup.nodes.Document doc = null;
				org.jsoup.select.Elements aTagsList = null;
				RMIExportToolInnerLinkDetails innerLinkDetails = null;
				String hrefValue = null;
				for(int t=0;t<richTextContentNodes.length;t++)
				{
					richTextNodesList = document.getElementsByTagName(richTextContentNodes[t]);
					if(null!=richTextNodesList && richTextNodesList.getLength()>0)
					{
						for(int r=0;r<richTextNodesList.getLength();r++)
						{
							richTextNode = (Node)richTextNodesList.item(r);
							if(!"#text".equalsIgnoreCase(richTextNode.getNodeName()))
							{
								htmlContent = Utilities.getCharacterDataFromElement((Element)richTextNode);
								if(null!=htmlContent && !"".equals(htmlContent))
								{
									/*
									 * CONVERT THIS TO HTML DOC AND START IDENITFYING INLINE IMAGES
									 */
									doc = Jsoup.parse(htmlContent);
									aTagsList = doc.select("a");
									if(null!=aTagsList && aTagsList.size()>0)
									{
										hrefValue = null;
										innerLinkDetails = null;
										for(int a=0;a<aTagsList.size();a++)
										{
											hrefValue = aTagsList.get(a).attr("href");
											/*
											 * ADD ALL INNERLINKS EXCEPT VALUES WITH NULL / BLANK / #
											 */
											if (null != hrefValue && !"".equals(hrefValue) && !"".equals(hrefValue.trim()) &&
													!hrefValue.startsWith("#"))
											{
												innerLinkDetails = new RMIExportToolInnerLinkDetails();
												// set SRC Path
												innerLinkDetails.setSrcHrefPath(hrefValue);
												
												// set ITEM Details
												innerLinkDetails.setItemDetails(new RMIExportToolItemDetails());
												innerLinkDetails.setItemDetails(itemDetails);
												// set DOCUMENT PATH OF RMI WORKING DIRECTORY IN ITEM DETAILS
												innerLinkDetails.getItemDetails().setDocumentDirPath(actualWriteFolderPath);
												
												/*
												 * check if Actual InnerLink for Processing or link just for tracking Purpose in Report
												 */
												if (!hrefValue.trim().toLowerCase().startsWith("http://") 
														&& !hrefValue.trim().toLowerCase().startsWith("https://")  
														&& !hrefValue.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf")) && 
														hrefValue.trim().toLowerCase().indexOf(innerLinkContent.trim().toLowerCase())!=-1)
												{
													/*
													 * 	INNER LINK FOUND
													 *  HREF VALUE CONTAINS - index?page=content&id=
													 *  READ THE DOCUMENT ID 
													 */
													// set INNERLINK DOCUMENT ID
													innerLinkDetails.setInnerLinkDocumentId(hrefValue.substring(hrefValue.trim().toLowerCase().indexOf(innerLinkContent.trim().toLowerCase())+(innerLinkContent.trim().length()), hrefValue.length()));
													
													if(null!=innerLinkDetails.getInnerLinkDocumentId() && !"".equals(innerLinkDetails.getInnerLinkDocumentId()))
													{
														// set CONSIDER INNERLINK PROCESSING STATUS AS YES
														innerLinkDetails.setConsiderInnerLinkForProcessing(ScheduleConstants.STATUS_YES);
													}
													else
													{
														// just track this Link for reporting Process
														// set CONSIDER INNERLINK PROCESSING STATUS AS NO
														innerLinkDetails.setConsiderInnerLinkForProcessing(ScheduleConstants.STATUS_NO);
														// set PROCESSING STATUS AS SKIPPED
														innerLinkDetails.setProcessingStatus(ScheduleConstants.STATUS_SKIPPED);
													}
												}
												else
												{
													// just track this Link for reporting Process
													// set CONSIDER INNERLINK PROCESSING STATUS AS NO
													innerLinkDetails.setConsiderInnerLinkForProcessing(ScheduleConstants.STATUS_NO);
													// set PROCESSING STATUS AS SKIPPED
													innerLinkDetails.setProcessingStatus(ScheduleConstants.STATUS_SKIPPED);
												}
												
												// add to INNERLINKS LIST
												if(null==innerLinksList || innerLinksList.size()<=0)
												{
													innerLinksList = new ArrayList<RMIExportToolInnerLinkDetails>();
												}
												innerLinksList.add(innerLinkDetails);
											}
											hrefValue = null;
										}
									}
									doc  =null;
									aTagsList = null;
								}
								htmlContent = null;
								richTextNode = null;
							}
						}
					}
					richTextNodesList = null;
				}

				hrefValue = null;
				doc = null;
				aTagsList = null;
				innerLinkDetails = null;
				htmlContent = null;
				richTextNodesList = null;
				richTextNode = null;
				db = null;
				is = null;
				document = null;
				innerLinkContent = null;
			}
			richTextContentNodes = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "readInnerLinks()", e);
		}
		return innerLinksList;
	}

	public List<RMIExportToolInnerLinkDetails> readOtherManualInnerLinks(String xmlFileData, RMIExportToolItemDetails itemDetails)
	{
		List<RMIExportToolInnerLinkDetails> otherManualLinks = null;
		try
		{
			/*
			 * IDENITFY THE CONTENT FOR RICH TEXT NODES - BASED ON CHANNELS
			 * Identify all <A> Tags, read their HREF Values
			 * 		Ensure, HEF Value is not null, not starts with http:// or https:// or # AND MUST NOT END WITH .pdf
			 * 		And contains INNER LINK URL = index?page=content&id=
			 */
			String[] richTextContentNodes=null;
			if(itemDetails.getChannelName().equals("SERVICE_INFORMATION"))
			{
				richTextContentNodes = "SI_HISTORY,TI_SUMMARY,BULLETIN_NOTES,DESCRIPTION,REPAIR_PROCEDURE,CALIBRATION,PARTS_INFORMATION,WARRANTY_INFORMATION".split(",");
			}
			else if(itemDetails.getChannelName().equals("SERVICE_MANUALS") || itemDetails.getChannelName().equals("OTHER_SERVICE_MANUALS"))
			{
				richTextContentNodes ="CONTENT".split(",");
			}
			else if(itemDetails.getChannelName().equals("TRAINING"))
			{
				richTextContentNodes ="DESCRIPTION,ETAS_LINK".split(",");
			}
			else if(itemDetails.getChannelName().equals("WIRING_DIAGRAMS"))
			{
				richTextContentNodes ="HTML_CONTENT,FLASH_CONTENT".split(",");
			}
			// NO RICH TEXT NODE FOR VIDEOS

			if(null!=richTextContentNodes && richTextContentNodes.length>0)
			{
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(xmlFileData));
				Document document = db.parse(is);
				NodeList richTextNodesList = null;
				Node richTextNode = null;
				String htmlContent = null;
				
				
				String otherManualLinkMNAOPattern=ApplicationProperties.getProperty("rimtool.othermanualink.mnao.url.pattern");
				String otherManualLinkMCPattern=ApplicationProperties.getProperty("rimtool.othermanualink.mc.url.pattern");
				String otherManualLinkMMEPattern=ApplicationProperties.getProperty("rimtool.othermanualink.mme.url.pattern");
				org.jsoup.nodes.Document doc = null;
				org.jsoup.select.Elements aTagsList = null;
				RMIExportToolInnerLinkDetails innerLinkDetails = null;
				String hrefValue = null;
				for(int t=0;t<richTextContentNodes.length;t++)
				{
					richTextNodesList = document.getElementsByTagName(richTextContentNodes[t]);
					if(null!=richTextNodesList && richTextNodesList.getLength()>0)
					{
						for(int r=0;r<richTextNodesList.getLength();r++)
						{
							richTextNode = (Node)richTextNodesList.item(r);
							if(!"#text".equalsIgnoreCase(richTextNode.getNodeName()))
							{
								htmlContent = Utilities.getCharacterDataFromElement((Element)richTextNode);
								if(null!=htmlContent && !"".equals(htmlContent))
								{
									/*
									 * CONVERT THIS TO HTML DOC AND START IDENITFYING INLINE IMAGES
									 */
									doc = Jsoup.parse(htmlContent);
									aTagsList = doc.select("a");
									if(null!=aTagsList && aTagsList.size()>0)
									{
										hrefValue = null;
										innerLinkDetails = null;
										for(int a=0;a<aTagsList.size();a++)
										{
											hrefValue = aTagsList.get(a).attr("href");
											
											/*
											 * CHECK IF HREF TAG IS NOT NULL AND STARTS WITH 
											 * index?page=result / index?page=result_mc / index?page=result_mme
											 * convert them to dead links in HTML Content
											 * 
											 * DATE -CHANGE - Convert these links to text data rather than dead links
											 * 21 May 2024
											 */
											if (null != hrefValue && !"".equals(hrefValue) && !"".equals(hrefValue.trim()) &&
													!hrefValue.startsWith("#") && 
													(hrefValue.trim().toLowerCase().startsWith(otherManualLinkMNAOPattern.trim().toLowerCase()) || 
															hrefValue.trim().toLowerCase().startsWith(otherManualLinkMCPattern.trim().toLowerCase()) || 
															hrefValue.trim().toLowerCase().startsWith(otherManualLinkMMEPattern.trim().toLowerCase())))
											{
												innerLinkDetails = new RMIExportToolInnerLinkDetails();
//												innerLinkDetails.setSrcHrefPath(hrefValue);
//												innerLinkDetails.setPathToBeReplaced("javascript:void(0);");
												innerLinkDetails.setSrcHrefPath(aTagsList.get(a).outerHtml());
												innerLinkDetails.setPathToBeReplaced(aTagsList.get(a).html());
												if(null==otherManualLinks || otherManualLinks.size()<=0)
												{
													otherManualLinks = new ArrayList<RMIExportToolInnerLinkDetails>();
												}
												otherManualLinks.add(innerLinkDetails);
												innerLinkDetails = null;
											}
											hrefValue = null;
										}
									}
									doc  =null;
									aTagsList = null;
								}
								htmlContent = null;
								richTextNode = null;
							}
						}
					}
					richTextNodesList = null;
				}

				hrefValue = null;
				doc = null;
				aTagsList = null;
				innerLinkDetails = null;
				htmlContent = null;
				richTextNodesList = null;
				richTextNode = null;
				db = null;
				is = null;
				document = null;
				otherManualLinkMCPattern = null;
				otherManualLinkMMEPattern = null;
				otherManualLinkMNAOPattern = null;
			}
			richTextContentNodes = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "readOtherManualInnerLinks()", e);
		}
		return otherManualLinks;
	}

	
	public String searchDocumentXMLinDirectory(String directoryPath,String localeDir, String fileName)
	{
		String relativePathForFile = null;
		try
		{
			String actualDirectoryPath = directoryPath;
			localeDir=  localeDir.replace("-", "_");
			/*
			 * add LocaleDir to directoryPath for searching Document within the Same Locale
			 */
			if(!actualDirectoryPath.endsWith("/"))
			{
				actualDirectoryPath+="/";
			}
			actualDirectoryPath+=localeDir;
			File directory = new File(actualDirectoryPath);
			relativePathForFile = findFileInDirectory(directory, directoryPath, fileName, relativePathForFile);
			directory = null;
			actualDirectoryPath = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "searchDocumentXMLinDirectory()", e);
			relativePathForFile = null;
		}
		finally
		{
			localeDir=null;
			directoryPath = null;
			fileName=  null;
		}
		return relativePathForFile;
	}
	
	private String findFileInDirectory(File directory,String directoryPathToBeReplaced, String fileName,String relativePathForFile)
	{
		try
		{
			if(directory.exists() && directory.isDirectory())
			{
				File[] listFiles = directory.listFiles();
				if(null!=listFiles && listFiles.length>0)
				{
					for(int a=0;a<listFiles.length;a++)
					{
						relativePathForFile = findFileInDirectory(listFiles[a], directoryPathToBeReplaced, fileName,relativePathForFile);
					}
				}
				listFiles = null;
			}
			else if(directory.exists() && directory.isFile())
			{
				if(directory.getName().trim().toLowerCase().equals(fileName.trim().toLowerCase()))
				{
					// fileFound
					/*
					 * remove DirectoryPath (RMITOOL WorkinDir/ScheduleId) from the AbsolutePath of the File
					 * and send the remaining Path as relativePathForFile
					 */
					relativePathForFile = directory.getAbsolutePath();
					relativePathForFile = relativePathForFile.replace("\\", "/");
					relativePathForFile = relativePathForFile.replace(directoryPathToBeReplaced, "");
					if(null!=relativePathForFile && !"".equals(relativePathForFile))
					{
						if(!relativePathForFile.startsWith("/"))
						{
							relativePathForFile="/"+relativePathForFile;
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "findFileInDirectory()", e);
			relativePathForFile = null;
		}
		return relativePathForFile;
	}
	
	public RMIExportToolInnerLinkDetails searchDocumentXMLinTransactionList(String localeDir, String fileName, ArrayList<RMIExportToolItemDetails> transactionList, RMIExportToolInnerLinkDetails innerLinkDetails)
	{
		try
		{
			localeDir=  localeDir.replace("-", "_");
			if(null!=transactionList && transactionList.size()>0)
			{
				RMIExportToolItemDetails itemDetails = null;
				for(int a=0;a<transactionList.size();a++)
				{
					itemDetails= (RMIExportToolItemDetails)transactionList.get(a);
					if(null!=itemDetails.getLocale() && null!=itemDetails.getDocumentId() && 
							itemDetails.getLocale().replace("-", "_").equals(localeDir) && itemDetails.getDocumentId().equals(fileName))
					{
						if(null!=itemDetails.getProcessingStatus() && !"".equals(itemDetails.getProcessingStatus()))
						{
							if(itemDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_SUCCESS))
							{
								// document.xml written successfully - no errors
								if(null!=itemDetails.getDocumentDirPath() && !"".equals(itemDetails.getDocumentDirPath()))
								{
									innerLinkDetails.setPathToBeReplaced(itemDetails.getDocumentDirPath().substring(itemDetails.getDocumentDirPath().indexOf(localeDir), itemDetails.getDocumentDirPath().length()));
								}
							}
							else if(itemDetails.getProcessingStatus().equals(ScheduleConstants.STATUS_FAILURE))
							{
								/* 
								 * CHECK FOR ERROR MESSAGE
								 */
								if(null!=itemDetails.getErrorMessage() && 
										((itemDetails.getErrorMessage().indexOf("FAILED TO WRITE XML FILE FOR DOCUMENT IN THE WORKING DIRECTORY ON SERVER")!=1) || 
										(itemDetails.getErrorMessage().indexOf("FAILED TO CREATE REQUIRED DIRECTORY STRUCTURE FOR DOCUMENT IN THE WORKING DIRECTORY ON SERVER")!=1) || 
										(itemDetails.getErrorMessage().indexOf("FAILED TO REMOVE UNWANTED XML NODES DURING DATA EXTRACTION")!=1) || 
										(itemDetails.getErrorMessage().indexOf("FAILED TO FIND XML DOCUMENT FILE INSIDE MGSS SERVER")!=1)))
								{
									// SOME ISSUE WITH DOCUMENT.XML IN WRITING TO EXPORT DIR
									// do not setRelative Path
									innerLinkDetails.setPathToBeReplaced(null);
								}
								else
								{
									// document.xml written successfully - errors with Inline IMAGES / PDFS / ATTACHMENTS
									if(null!=itemDetails.getDocumentDirPath() && !"".equals(itemDetails.getDocumentDirPath()))
									{
										innerLinkDetails.setPathToBeReplaced(itemDetails.getDocumentDirPath().substring(itemDetails.getDocumentDirPath().indexOf(localeDir), itemDetails.getDocumentDirPath().length()));
									}
								}
							}
						}
						break;
					}
					itemDetails = null;
				}
				itemDetails= null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "searchDocumentXMLinDirectory()", e);
		}
		finally
		{
			localeDir=null;
			fileName=  null;
			transactionList = null;
		}
		return innerLinkDetails;
	}
	
	/**
	 * WHICH CATEGORY TREE A CATEGORY BELONGS TO - ESI, VIN, CVC, CARLINE and so on.
	 *
	 * MATCHED ON THE REF KEY NOW, NOT AN OBJECT ID. InfoManager identified a tree by the numeric
	 * prefix of the category's OBJECTID (00019, 00001 ...), so this walked
	 * rmitool.master.category.objectids and rmitool.master.category.refkeys in parallel to turn
	 * one into the other. Kapture has no object ids at all - it names the tree directly, in
	 * masterCategoryRefKey - so the translation step is gone and the value is compared against the
	 * REF KEY list on its own. That list was already in the properties file; only the match key
	 * changed.
	 *
	 * rmitool.master.category.objectids is now UNUSED. It is left in application.properties
	 * because it is the only remaining record of the InfoManager id-to-tree mapping.
	 *
	 * THE CARLINE RULE IS UNCHANGED - the CARLINE tree is exported as CARLINE_VIN, as it always was.
	 *
	 * @param masterCategoryRefKey Kapture's masterCategoryRefKey for the category
	 * @return the tree's reference key, or null when it is not one of the master trees
	 */
	private String identifyCategoryType(String masterCategoryRefKey)
	{
		String categoryType=null;
		try
		{
			if(null!=masterCategoryRefKey && !"".equals(masterCategoryRefKey.trim()))
			{
				String wanted = masterCategoryRefKey.trim();
				StringTokenizer str = new StringTokenizer(ApplicationProperties.getProperty("rmitool.master.category.refkeys"),",");
				String refKey = null;
				while(str.hasMoreTokens())
				{
					refKey= str.nextToken();
					if(null!=refKey && refKey.trim().equalsIgnoreCase(wanted))
					{
						// TREE FOUND
						categoryType= refKey.trim();
						if(categoryType.equals("CARLINE"))
						{
							categoryType= "CARLINE_VIN";
						}
						break;
					}
					refKey=null;
				}
				refKey = null;
				str =null;
				wanted = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(RMIExportToolUtils.class.getName(), "identifyCategoryType()", e);
			categoryType = null;
		}
		return categoryType;
	}
}
