package com.mazda.gms3.dmt.conversion.impl;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.inquira.im.ito.impl.ContentRecordITOImpl;
import com.mazda.gms3.dmt.conversion.utils.SpecialCharactersUtils;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ContentDetails;


public class InfoQueriaUtils {

	public static String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		return "";
	}
	
	/**
	 * <SERVICE_MANUALS>
	 * 	<TITLE>
	 * 		<SERVICE_MANUAL_TYPE></SERVICE_MANUAL_TYPE>
	 * 		<CONTENT></CONTENT>
	 * 		<ATTACHMENTS>
	 * 			<ATTACHMENT_TITLE>
	 * 			<ATTACHMENT>
	 * 		</ATTACHMENTS>
	 * 		<VTOC_FILENAME>
	 * 		<SIE_ID>
	 * </SERVICE_MANUALS>
	 * @param contentVO
	 * @return
	 */
	public static StringBuilder createServiceManualsContentXML(ContentDetails contentVO, StringBuilder str)
	{
		try
		{
			if(null!=contentVO)
			{

				str.append("<"
						+ contentVO.getChannelName().toUpperCase()
								.replace(" ", "_") + ">");

				str.append("<TITLE><![CDATA[");
				if (null != contentVO.getTitle() && !"".equals(contentVO.getTitle())) 
				{
					/*
					 * replace special characters in the title
					 * 
					 * Do it for all Locales - 8th November 2016
					 */
					String title = contentVO.getTitle();
					title = SpecialCharactersUtils.replaceSpecialChars(title);
					/*
					 * also replace <SUP> TAG BOTH OPENING & CLOSING
					 * DATE 17 DEC 2016
					 */
					title = title.replace("<SUP>", "");
					title = title.replace("</SUP>", "");
					title = title.replace("<sup>", "");
					title = title.replace("</sup>", "");
					title = title.replace("<Sup>", "");
					title = title.replace("</Sup>", "");
					title = title.replace("<sUp>", "");
					title = title.replace("</sUp>", "");
					title = title.replace("<suP>", "");
					title = title.replace("</suP>", "");
					title = title.replace("<SUp>", "");
					title = title.replace("</SUp>", "");
					title = title.replace("<sUP>", "");
					title = title.replace("</sUP>", "");
					title = title.replace("<SuP>", "");
					title = title.replace("</SuP>", "");
					
					
					/*
//					String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());
					
					String locale="";
					if(null!=contentVO.getLocale() && !"".equals(contentVO.getLocale()))
					{
						locale = contentVO.getLocale().toLowerCase();
						locale= locale.replace("_", "-");
						if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("es-mx")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
						else if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("fr-ca")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
					}
					locale = null;
					*/
					str.append(title);
					title = null;
				}
				str.append("]]></TITLE>");
				
				str.append("<CONTENT><![CDATA[");
				if (null != contentVO.getDocumentContent() 	&& !"".equals(contentVO.getDocumentContent())) 
				{
					str.append(contentVO.getDocumentContent());
				}
				str.append("]]></CONTENT>");

				str.append("<VTOC_FILENAME><![CDATA[");
				if (null != contentVO.getFileNameAttribute() 	&& !"".equals(contentVO.getFileNameAttribute())) 
				{
					str.append(contentVO.getFileNameAttribute());
				}
				str.append("]]></VTOC_FILENAME>");
				
				str.append("<SIE_ID><![CDATA[");
				if (null != contentVO.getFileName() 	&& !"".equals(contentVO.getFileName())) 
				{
					str.append(contentVO.getFileName());
				}
				str.append("]]></SIE_ID>");
				
				str.append("<DJVU_FILE_LOCATION><![CDATA[");
				if(null!=contentVO.getUploadDirectoryPath() && !"".equals(contentVO.getUploadDirectoryPath()))
				{
					str.append(contentVO.getUploadDirectoryPath());
				}
				str.append("]]></DJVU_FILE_LOCATION>");
				
				
				str.append("<OASIS_FILE_LOCATION><![CDATA[");
				if(null!=contentVO.getOasisDirectoryPath() && !"".equals(contentVO.getOasisDirectoryPath()))
				{
					str.append(contentVO.getOasisDirectoryPath());
				}
				str.append("]]></OASIS_FILE_LOCATION>");
				
				
				
				if (contentVO.getDocumentType().equals(ContentDetails.PDF_DOCUMENT))
				{
					// add Attachment
					str.append("<ATTACHMENTS>");
					str.append("<ATTACHMENT_TITLE><![CDATA[");
					if(null!=contentVO.getTitle() && !"".equals(contentVO.getTitle()))
					{
						str.append(contentVO.getTitle());
					}
					str.append("]]></ATTACHMENT_TITLE>");
					str.append("<ATTACHMENT>");
					str.append("<![CDATA[");
					if (null != contentVO.getPdfFileNameAsAttachment() 	&& !"".equals(contentVO.getPdfFileNameAsAttachment())) 
					{
						str.append(contentVO.getPdfFileNameAsAttachment());
					}
					str.append("]]>");
					str.append("</ATTACHMENT>");
					str.append("</ATTACHMENTS>");
				}
				str.append("</"+ contentVO.getChannelName().toUpperCase().replace(" ", "_") + ">");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(InfoQueriaUtils.class.getName(), "createServiceManualsContentXML()", e);
		}
		return str;
	}

	/**
	 *  <WIRING_DIAGRAMS>
	 * 	<TITLE>
	 * 	<NAVIGATION>
	 * 	<HTML_CONTENT>
	 * 	<FLASH_CONTENT>
	 * 	<HTML5_FILE_NAME>
	 * 	<HTML5_UPLOAD_DIRECTORY>
	 * 	<HTML5_FONT_SELECTION>
	 * 	<ATTACHMENT>
	 * 
	 * </WIRING_DIAGRAMS>
	 * @param contentVO
	 * @return
	 */
	public static StringBuilder createWiringDiagramsContentXML(ContentDetails contentVO, StringBuilder str)
	{
		try
		{
			if(null!=contentVO)
			{

				str.append("<"+ contentVO.getChannelName().toUpperCase().replace(" ", "_") + ">");

				str.append("<TITLE><![CDATA[");
				if (null != contentVO.getTitle() && !"".equals(contentVO.getTitle())) 
				{
					/*
					 * replace special characters in the title
					 */
//					String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());

					/*
					 * replace special characters in the title
					 * 
					 * Do it for all Locales - 8th November 2016
					 */
					String title = contentVO.getTitle();
					title = SpecialCharactersUtils.replaceSpecialChars(title);
					
					/*
					 * REPLACE SUP BOTH OPENING & CLOSING TAGS
					 * DATE 17 DEC 2016
					 */
					title = title.replace("<SUP>", "");
					title = title.replace("</SUP>", "");
					title = title.replace("<sup>", "");
					title = title.replace("</sup>", "");
					title = title.replace("<Sup>", "");
					title = title.replace("</Sup>", "");
					title = title.replace("<sUp>", "");
					title = title.replace("</sUp>", "");
					title = title.replace("<suP>", "");
					title = title.replace("</suP>", "");
					title = title.replace("<SUp>", "");
					title = title.replace("</SUp>", "");
					title = title.replace("<sUP>", "");
					title = title.replace("</sUP>", "");
					title = title.replace("<SuP>", "");
					title = title.replace("</SuP>", "");
					/*
//					String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());
					
					String locale="";
					if(null!=contentVO.getLocale() && !"".equals(contentVO.getLocale()))
					{
						locale = contentVO.getLocale().toLowerCase();
						locale= locale.replace("_", "-");
						if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("es-mx")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
						else if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("fr-ca")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
					}
					locale = null;
					*/
					str.append(title);
					title = null;
				
				}
				str.append("]]></TITLE>");
				
				/*
				 * DO NOT SEND ANYTHING IN HTML_CONTENT
				 * FOR HTML5 DOCUMENTS.
				 * NOT FOR PDFS
				 */
				str.append("<HTML_CONTENT><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.HTML_DOCUMENT) 
						|| contentVO.getDocumentType().equals(ContentDetails.ENT_DOCUMENT))
				{
					if (null != contentVO.getDocumentContent() 	&& !"".equals(contentVO.getDocumentContent())) 
					{
						str.append(contentVO.getDocumentContent());
					}
				}
				str.append("]]></HTML_CONTENT>");

				str.append("<FLASH_CONTENT><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.FLASH_DOCUMENT))
				{
					if (null != contentVO.getDocumentContent() 	&& !"".equals(contentVO.getDocumentContent())) 
					{
						str.append(contentVO.getDocumentContent());
					}
				}
				str.append("]]></FLASH_CONTENT>");
				
				
				str.append("<NAVIGATION><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.HTML5_DOCUMENT))
				{
					if(null!=contentVO.getNavigation() && !"".equals(contentVO.getNavigation()))
					{
						str.append(contentVO.getNavigation());
					}
				}
				str.append("]]></NAVIGATION>");
				
				str.append("<HTML5_FILE_NAME><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.HTML5_DOCUMENT))
				{
					if (null != contentVO.getFileName() && !"".equals(contentVO.getFileName())) 
					{
						str.append(contentVO.getFileName());
					}
				}
				str.append("]]></HTML5_FILE_NAME>");
				
				
				str.append("<HTML5_UPLOAD_DIRECTORY><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.HTML5_DOCUMENT))
				{
					if(null!=contentVO.getUploadDirectoryPath() && !"".equals(contentVO.getUploadDirectoryPath()))
					{
						str.append(contentVO.getUploadDirectoryPath());
					}
				}
				str.append("]]></HTML5_UPLOAD_DIRECTORY>");
				
				str.append("<HTML5_FONT_SELECTION><![CDATA[");
				if(contentVO.getDocumentType().equals(ContentDetails.HTML5_DOCUMENT) || 
						contentVO.getDocumentType().equals(ContentDetails.FLASH_DOCUMENT) || 
						contentVO.getDocumentType().equals(ContentDetails.HTML_MC_DOCUMENT))
				{
					if (null != contentVO.getFontContent() 	&& !"".equals(contentVO.getFontContent())) 
					{
						str.append(contentVO.getFontContent());
					}
				}
				str.append("]]></HTML5_FONT_SELECTION>");
				
				if(null!=contentVO.getHtml5FileSourcePath() && !"".equals(contentVO.getHtml5FileSourcePath()))
				{
					// WHEN ONLY HTML5 DOCUMENT - THE VALUE WILL COME
					str.append("<DJVU_FILE_LOCATION><![CDATA[");
					if(null!=contentVO.getHtml5FileSourcePath() && !"".equals(contentVO.getHtml5FileSourcePath()))
					{
						str.append(contentVO.getHtml5FileSourcePath());
					}
					str.append("]]></DJVU_FILE_LOCATION>");
				}
				// WHEN NOT HTML5 DOCUMENT
				if(!contentVO.getDocumentType().equals(ContentDetails.HTML5_DOCUMENT))
				{
					str.append("<DJVU_FILE_LOCATION><![CDATA[");
					if(null!=contentVO.getUploadDirectoryPath() && !"".equals(contentVO.getUploadDirectoryPath()))
					{
						str.append(contentVO.getUploadDirectoryPath());
					}
					str.append("]]></DJVU_FILE_LOCATION>");
				}
				
				// NO NEED OF CHECKING PDF_DOCUMENT
				if(null!=contentVO.getPdfFileNameAsAttachment() && !"".equals(contentVO.getPdfFileNameAsAttachment()))
				{
					// add Attachment -- add Sizd as Attribute
					/*					long length=0;
									if(null!=contentVO.getFileData())
									{
										length= contentVO.getFileData();
					 *///					}
					str.append("<ATTACHMENT>");
					str.append("<![CDATA[");
					if (null != contentVO.getPdfFileNameAsAttachment() 	&& !"".equals(contentVO.getPdfFileNameAsAttachment())) 
					{
						str.append(contentVO.getPdfFileNameAsAttachment());
					}
					str.append("]]>");
					str.append("</ATTACHMENT>");
				}
				else
				{
					str.append("<ATTACHMENT>");
					str.append("<![CDATA[");
					str.append("]]>");
					str.append("</ATTACHMENT>");
				}
				
				str.append("</"+ contentVO.getChannelName().toUpperCase().replace(" ", "_") + ">");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(InfoQueriaUtils.class.getName(), "createWiringDiagramsContentXML()", e);
		}
		return str;
	}

	/**
	 * Function will generate SCHEMA XML FOR OTHER SERVICE MANUALS CHANNEL
	 * @param contentVO
	 * @param str
	 * @return
	 */
	public static StringBuilder createOtherServiceManualsContentXML(ContentDetails contentVO, StringBuilder str)
	{
		try
		{
			if(null!=contentVO)
			{

				str.append("<"
						+ contentVO.getChannelName().toUpperCase()
								.replace(" ", "_") + ">");

				str.append("<TITLE><![CDATA[");
				if (null != contentVO.getTitle() && !"".equals(contentVO.getTitle())) 
				{
					/*
					 * replace special characters in the title
					 * 
					 * Do it for all Locales - 8th November 2016
					 */
					String title = contentVO.getTitle();
					title = SpecialCharactersUtils.replaceSpecialChars(title);
					/*
					 * also replace <SUP> TAG BOTH OPENING & CLOSING
					 * DATE 17 DEC 2016
					 */
					title = title.replace("<SUP>", "");
					title = title.replace("</SUP>", "");
					title = title.replace("<sup>", "");
					title = title.replace("</sup>", "");
					title = title.replace("<Sup>", "");
					title = title.replace("</Sup>", "");
					title = title.replace("<sUp>", "");
					title = title.replace("</sUp>", "");
					title = title.replace("<suP>", "");
					title = title.replace("</suP>", "");
					title = title.replace("<SUp>", "");
					title = title.replace("</SUp>", "");
					title = title.replace("<sUP>", "");
					title = title.replace("</sUP>", "");
					title = title.replace("<SuP>", "");
					title = title.replace("</SuP>", "");
					
					
					/*
//					String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());
					
					String locale="";
					if(null!=contentVO.getLocale() && !"".equals(contentVO.getLocale()))
					{
						locale = contentVO.getLocale().toLowerCase();
						locale= locale.replace("_", "-");
						if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("es-mx")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
						else if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("fr-ca")))
						{
							title = SpecialCharactersUtils.replaceSpecialChars(title);
						}
					}
					locale = null;
					*/
					str.append(title);
					title = null;
				}
				str.append("]]></TITLE>");
				
				str.append("<CONTENT><![CDATA[");
				if (null != contentVO.getDocumentContent() 	&& !"".equals(contentVO.getDocumentContent())) 
				{
					str.append(contentVO.getDocumentContent());
				}
				str.append("]]></CONTENT>");

				str.append("<DJVU_FILE_LOCATION><![CDATA[");
				if(null!=contentVO.getUploadDirectoryPath() && !"".equals(contentVO.getUploadDirectoryPath()))
				{
					str.append(contentVO.getUploadDirectoryPath());
				}
				str.append("]]></DJVU_FILE_LOCATION>");
				
				str.append("<OASIS_FILE_LOCATION><![CDATA[");
				if(null!=contentVO.getOasisDirectoryPath() && !"".equals(contentVO.getOasisDirectoryPath()))
				{
					str.append(contentVO.getOasisDirectoryPath());
				}
				str.append("]]></OASIS_FILE_LOCATION>");
				
				if (contentVO.getDocumentType().equals(ContentDetails.PDF_DOCUMENT))
				{
					// add Attachment
					str.append("<ATTACHMENTS>");
					str.append("<ATTACHMENT_TITLE><![CDATA[");
					if(null!=contentVO.getTitle() && !"".equals(contentVO.getTitle()))
					{
						str.append(contentVO.getTitle());
					}
					str.append("]]></ATTACHMENT_TITLE>");
					str.append("<ATTACHMENT>");
					str.append("<![CDATA[");
					if (null != contentVO.getPdfFileNameAsAttachment() 	&& !"".equals(contentVO.getPdfFileNameAsAttachment())) 
					{
						str.append(contentVO.getPdfFileNameAsAttachment());
					}
					str.append("]]>");
					str.append("</ATTACHMENT>");
					str.append("</ATTACHMENTS>");
				}
				str.append("</"+ contentVO.getChannelName().toUpperCase().replace(" ", "_") + ">");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(InfoQueriaUtils.class.getName(), "createOtherServiceManualsContentXML()", e);
		}
		return str;
	}

	/**
	 * Function will remove the Existing Attachment & Title Node and will Return the Updated XML
	 * @param contentDetails
	 * @param content
	 */
	public static String performContentXMLOperationsFORWD_PDF(ContentDetails contentDetails, ContentRecordITOImpl content)
	{
		String contentXML = "";
		try
		{
			/*
			 * REMOVE THE ATTACHMENT NODE
			 * RREMOVE THE TITLE NODE
			 */

			// REPLACE THE ATTACHMENT NODE
			contentXML = content.getXml();
			if(null!=contentXML && !"".equals(contentXML))
			{
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(contentXML));
				Document doc = db.parse(is);
				if(null!=doc)
				{
					NodeList channelNodes = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					if(null!=channelNodes && channelNodes.getLength()>0)
					{
						for(int a=0;a<channelNodes.getLength();a++)
						{
							Node chNode = channelNodes.item(a);
							NodeList childNodesList = chNode.getChildNodes();
							if(null!=childNodesList && childNodesList.getLength()>0)
							{
								for(int b=0;b<childNodesList.getLength();b++)
								{
									Node childNode = childNodesList.item(b);
									if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
									{
										if(childNode.getNodeName().equals("ATTACHMENT"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("TITLE"))
										{
											chNode.removeChild(childNode);
										}
									}
									childNode = null;
								}
							}
							childNodesList = null;
							chNode  = null;
						}
					}
					
					/*
					 * Check for Attachment Node Explicitly
					 */
					NodeList chNodesListForSecurity = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					Node channelNode = null;
					if (null != chNodesListForSecurity && chNodesListForSecurity.getLength() > 0) 
					{
						// get the node at 0 Index
						channelNode = chNodesListForSecurity.item(0);
					}

					NodeList attachmentNodesList = doc.getElementsByTagName("ATTACHMENT");
					Node attchNode = null;
					if (null != attachmentNodesList && attachmentNodesList.getLength() > 0) 
					{
						// get the node at 0 Index
						attchNode = attachmentNodesList.item(0);
					}
					if (null != channelNode && null != attchNode) 
					{
						// remove the attachment node from content node
						channelNode.removeChild(attchNode);
					}
					
					attchNode = null;
					attachmentNodesList = null;
					
					/*
					 * Check for Title Node Explicitly
					 */
					NodeList titleNodesList = doc.getElementsByTagName("TITLE");
					Node titleNode=null;
					if(null!=titleNodesList && titleNodesList.getLength()>0)
					{
						// get the node at 0 index
						titleNode = titleNodesList.item(0);
					}
					
					if(null!=channelNode && null!=titleNode)
					{
						// remove the title node from content node
						channelNode.removeChild(titleNode);
					}
					titleNode = null;
					titleNodesList = null;
					chNodesListForSecurity = null;
					channelNode = null;
					
					
					if(null!=doc)
					{
						// TRANSFORM TO STRING
						contentXML = Utilities.transformString(doc);
						if(null!=contentXML && !"".equals(contentXML))
						{
							if(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">")!=-1)
							{
								String beforeText = contentXML.substring(0,contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">") );
								String afterText = contentXML.substring(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">"), contentXML.length());
								
								if(null!=beforeText && !"".equals(beforeText) && null!=afterText && !"".equals(afterText))
								{
									contentXML = beforeText;
									StringBuilder str = new StringBuilder();
									// ADD ATTACHMENT NODE
									if(null!=contentDetails.getPdfFileNameAsAttachment() && 
											!"".equals(contentDetails.getPdfFileNameAsAttachment()))
									{
										// add Attachment -- add Sizd as Attribute
										/*					long length=0;
														if(null!=contentVO.getFileData())
														{
															length= contentVO.getFileData();
										 *///					}
										str.append("<ATTACHMENT>");
										str.append("<![CDATA[");
										if (null != contentDetails.getPdfFileNameAsAttachment() 	&& !"".equals(contentDetails.getPdfFileNameAsAttachment())) 
										{
											str.append(contentDetails.getPdfFileNameAsAttachment());
										}
										str.append("]]>");
										str.append("</ATTACHMENT>");
									}
									else
									{
										str.append("<ATTACHMENT>");
										str.append("<![CDATA[");
										str.append("]]>");
										str.append("</ATTACHMENT>");
									}
									
									// ADD TITLE NODE
									str.append("<TITLE>");
									str.append("<![CDATA[");
									if(null!=contentDetails.getTitle() && !"".equals(contentDetails.getTitle()))
									{
										/*
										 * replace special characters in the title
										 */
//										String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());

										/*
										 * replace special characters in the title
										 * 
										 * Do it for all Locales - 8th November 2016
										 */
										String title = contentDetails.getTitle();
										title = SpecialCharactersUtils.replaceSpecialChars(title);
										
										/*
										 * REPLACE SUP BOTH OPENING & CLOSING TAGS
										 * DATE 17 DEC 2016
										 */
										title = title.replace("<SUP>", "");
										title = title.replace("</SUP>", "");
										title = title.replace("<sup>", "");
										title = title.replace("</sup>", "");
										title = title.replace("<Sup>", "");
										title = title.replace("</Sup>", "");
										title = title.replace("<sUp>", "");
										title = title.replace("</sUp>", "");
										title = title.replace("<suP>", "");
										title = title.replace("</suP>", "");
										title = title.replace("<SUp>", "");
										title = title.replace("</SUp>", "");
										title = title.replace("<sUP>", "");
										title = title.replace("</sUP>", "");
										title = title.replace("<SuP>", "");
										title = title.replace("</SuP>", "");
										/*
//										String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());
										
										String locale="";
										if(null!=contentVO.getLocale() && !"".equals(contentVO.getLocale()))
										{
											locale = contentVO.getLocale().toLowerCase();
											locale= locale.replace("_", "-");
											if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("es-mx")))
											{
												title = SpecialCharactersUtils.replaceSpecialChars(title);
											}
											else if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("fr-ca")))
											{
												title = SpecialCharactersUtils.replaceSpecialChars(title);
											}
										}
										locale = null;
										*/
										str.append(title);
										title=null;
									}
									str.append("]]>");
									str.append("</TITLE>");
									contentXML=  contentXML+str.toString()+ afterText;
									str = null;
								}
								beforeText= null;
								afterText= null;
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(InfoQueriaUtils.class.getName(),"performContentXMLOperationsFORWD_PDF()", e);
		}
		return contentXML;
	}

	/**
	 * Function will remove the Existing DJVU FILE LOCATION & TITLE Node and will Return the Updated XML
	 * @param contentDetails
	 * @param content
	 */
	public static String performContentXMLOperationsFORWD_HTML(ContentDetails contentDetails, ContentRecordITOImpl content)
	{
		String contentXML="";
		try
		{
			/*
			 * REMOVE THE DJVU_FILE_LOCATION NODE
			 * RREMOVE THE TITLE NODE
			 */

			// REPLACE THE ATTACHMENT NODE
			contentXML = content.getXml();
			if(null!=contentXML && !"".equals(contentXML))
			{
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(contentXML));
				Document doc = db.parse(is);
				if(null!=doc)
				{
					NodeList channelNodes = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					if(null!=channelNodes && channelNodes.getLength()>0)
					{
						for(int a=0;a<channelNodes.getLength();a++)
						{
							Node chNode = channelNodes.item(a);
							NodeList childNodesList = chNode.getChildNodes();
							if(null!=childNodesList && childNodesList.getLength()>0)
							{
								for(int b=0;b<childNodesList.getLength();b++)
								{
									Node childNode = childNodesList.item(b);
									if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
									{
										if(childNode.getNodeName().equals("DJVU_FILE_LOCATION"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("TITLE"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("HTML5_FONT_SELECTION"))
										{
											chNode.removeChild(childNode);
										}
									}
									childNode = null;
								}
							}
							childNodesList = null;
							chNode  = null;
						}
					}
					
					/*
					 * Check for DJVU_FILE_LOCATION Node Explicitly
					 */
					NodeList chNodesListForSecurity = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					Node channelNode = null;
					if (null != chNodesListForSecurity && chNodesListForSecurity.getLength() > 0) 
					{
						// get the node at 0 Index
						channelNode = chNodesListForSecurity.item(0);
					}

					NodeList djvuNodesList = doc.getElementsByTagName("DJVU_FILE_LOCATION");
					Node djvuNode = null;
					if (null != djvuNodesList && djvuNodesList.getLength() > 0) 
					{
						// get the node at 0 Index
						djvuNode = djvuNodesList.item(0);
					}
					if (null != channelNode && null != djvuNode) 
					{
						// remove the DJVU_FILE_LOCATION node from content node
						channelNode.removeChild(djvuNode);
					}
					
					djvuNode = null;
					djvuNodesList = null;
					
					/*
					 * check for HTML FONT NDOE EXPLICITLY
					 */
					NodeList fontNodesList = doc.getElementsByTagName("HTML5_FONT_SELECTION");
					Node fontNode = null;
					if (null != fontNodesList && fontNodesList.getLength() > 0) 
					{
						// get the node at 0 Index
						fontNode = fontNodesList.item(0);
					}
					if (null != channelNode && null != fontNode) 
					{
						// remove the HTML5_FONT_SELECTION node from content node
						channelNode.removeChild(fontNode);
					}
					
					fontNode = null;
					fontNodesList = null;
					
					/*
					 * Check for Title Node Explicitly
					 */
					NodeList titleNodesList = doc.getElementsByTagName("TITLE");
					Node titleNode=null;
					if(null!=titleNodesList && titleNodesList.getLength()>0)
					{
						// get the node at 0 index
						titleNode = titleNodesList.item(0);
					}
					
					if(null!=channelNode && null!=titleNode)
					{
						// remove the title node from content node
						channelNode.removeChild(titleNode);
					}
					chNodesListForSecurity = null;
					channelNode = null;
					
					
					if(null!=doc)
					{
						// TRANSFORM TO STRING
						contentXML = Utilities.transformString(doc);
						if(null!=contentXML && !"".equals(contentXML))
						{
							if(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">")!=-1)
							{
								String beforeText = contentXML.substring(0,contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">") );
								String afterText = contentXML.substring(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">"), contentXML.length());
								
								if(null!=beforeText && !"".equals(beforeText) && null!=afterText && !"".equals(afterText))
								{
									contentXML = beforeText;
									StringBuilder str = new StringBuilder();
									// ADD DJVU_FILE_LOCATION NODE
									str.append("<DJVU_FILE_LOCATION>");
									str.append("<![CDATA[");
									if(null!=contentDetails.getUploadDirectoryPath() && !"".equals(contentDetails.getUploadDirectoryPath()))
									{
										str.append(contentDetails.getUploadDirectoryPath());
									}
									str.append("]]>");
									str.append("</DJVU_FILE_LOCATION>");
									
									// ADD HTML5_FONT_SELECTION NODE
									str.append("<HTML5_FONT_SELECTION>");
									str.append("<![CDATA[");
									if(null!=contentDetails.getFontContent() && !"".equals(contentDetails.getFontContent()))
									{
										str.append(contentDetails.getFontContent());
									}
									str.append("]]>");
									str.append("</HTML5_FONT_SELECTION>");
									
									// ADD TITLE NODE
									str.append("<TITLE>");
									str.append("<![CDATA[");
									if(null!=contentDetails.getTitle() && !"".equals(contentDetails.getTitle()))
									{
										/*
										 * replace special characters in the title
										 */
//										String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());

										/*
										 * replace special characters in the title
										 * 
										 * Do it for all Locales - 8th November 2016
										 */
										String title = contentDetails.getTitle();
										title = SpecialCharactersUtils.replaceSpecialChars(title);
										
										/*
										 * REPLACE SUP BOTH OPENING & CLOSING TAGS
										 * DATE 17 DEC 2016
										 */
										title = title.replace("<SUP>", "");
										title = title.replace("</SUP>", "");
										title = title.replace("<sup>", "");
										title = title.replace("</sup>", "");
										title = title.replace("<Sup>", "");
										title = title.replace("</Sup>", "");
										title = title.replace("<sUp>", "");
										title = title.replace("</sUp>", "");
										title = title.replace("<suP>", "");
										title = title.replace("</suP>", "");
										title = title.replace("<SUp>", "");
										title = title.replace("</SUp>", "");
										title = title.replace("<sUP>", "");
										title = title.replace("</sUP>", "");
										title = title.replace("<SuP>", "");
										title = title.replace("</SuP>", "");
										/*
//										String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());
										
										String locale="";
										if(null!=contentVO.getLocale() && !"".equals(contentVO.getLocale()))
										{
											locale = contentVO.getLocale().toLowerCase();
											locale= locale.replace("_", "-");
											if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("es-mx")))
											{
												title = SpecialCharactersUtils.replaceSpecialChars(title);
											}
											else if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("fr-ca")))
											{
												title = SpecialCharactersUtils.replaceSpecialChars(title);
											}
										}
										locale = null;
										*/
										str.append(title);
										title=null;
									}
									str.append("]]>");
									str.append("</TITLE>");
									contentXML=  contentXML+str.toString()+ afterText;
									str = null;
								}
								beforeText= null;
								afterText= null;
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(InfoQueriaUtils.class.getName(),"performContentXMLOperationsFORWD_HTML()", e);
		}
		return contentXML;
	}

	
	public static String performContentXMLOperationsFORWD_HTMLMNAO(ContentDetails contentDetails, ContentRecordITOImpl content)
	{
		String contentXML="";
		try
		{
			/*
			 * REMOVE THE DJVU_FILE_LOCATION NODE
			 * RREMOVE THE TITLE NODE
			 */

			// REPLACE THE ATTACHMENT NODE
			contentXML = content.getXml();
			if(null!=contentXML && !"".equals(contentXML))
			{
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(contentXML));
				Document doc = db.parse(is);
				if(null!=doc)
				{
					NodeList channelNodes = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					if(null!=channelNodes && channelNodes.getLength()>0)
					{
						for(int a=0;a<channelNodes.getLength();a++)
						{
							Node chNode = channelNodes.item(a);
							NodeList childNodesList = chNode.getChildNodes();
							if(null!=childNodesList && childNodesList.getLength()>0)
							{
								for(int b=0;b<childNodesList.getLength();b++)
								{
									Node childNode = childNodesList.item(b);
									if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
									{
										if(childNode.getNodeName().equals("HTML_CONTENT"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("TITLE"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("FLASH_CONTENT"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("HTML5_FONT_SELECTION"))
										{
											chNode.removeChild(childNode);
										}
									}
									childNode = null;
								}
							}
							childNodesList = null;
							chNode  = null;
						}
					}
					
					/*
					 * Check for DJVU_FILE_LOCATION Node Explicitly
					 */
					NodeList chNodesListForSecurity = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					Node channelNode = null;
					if (null != chNodesListForSecurity && chNodesListForSecurity.getLength() > 0) 
					{
						// get the node at 0 Index
						channelNode = chNodesListForSecurity.item(0);
					}

					NodeList htmlContentNodesList = doc.getElementsByTagName("HTML_CONTENT");
					Node htmlContentNode = null;
					if (null != htmlContentNodesList && htmlContentNodesList.getLength() > 0) 
					{
						// get the node at 0 Index
						htmlContentNode = htmlContentNodesList.item(0);
					}
					if (null != channelNode && null != htmlContentNode) 
					{
						// remove the HTML_CONTENT node from content node
						channelNode.removeChild(htmlContentNode);
					}
					
					htmlContentNode = null;
					htmlContentNodesList = null;
					
					NodeList flashContentNodesList = doc.getElementsByTagName("FLASH_CONTENT");
					Node flashContentNode = null;
					if (null != flashContentNodesList && flashContentNodesList.getLength() > 0) 
					{
						// get the node at 0 Index
						flashContentNode = flashContentNodesList.item(0);
					}
					if (null != channelNode && null != flashContentNode) 
					{
						// remove the HTML_CONTENT node from content node
						channelNode.removeChild(flashContentNode);
					}
					
					flashContentNode = null;
					flashContentNodesList = null;
					
					NodeList fontContentNodesList = doc.getElementsByTagName("HTML5_FONT_SELECTION");
					Node fontContentNode = null;
					if (null != fontContentNodesList && fontContentNodesList.getLength() > 0) 
					{
						// get the node at 0 Index
						fontContentNode = fontContentNodesList.item(0);
					}
					if (null != channelNode && null != fontContentNode) 
					{
						// remove the HTML_CONTENT node from content node
						channelNode.removeChild(fontContentNode);
					}
					
					fontContentNode = null;
					fontContentNodesList = null;
					
					/*
					 * Check for Title Node Explicitly
					 */
					NodeList titleNodesList = doc.getElementsByTagName("TITLE");
					Node titleNode=null;
					if(null!=titleNodesList && titleNodesList.getLength()>0)
					{
						// get the node at 0 index
						titleNode = titleNodesList.item(0);
					}
					
					if(null!=channelNode && null!=titleNode)
					{
						// remove the title node from content node
						channelNode.removeChild(titleNode);
					}
					chNodesListForSecurity = null;
					channelNode = null;
					
					
					if(null!=doc)
					{
						// TRANSFORM TO STRING
						contentXML = Utilities.transformString(doc);
						if(null!=contentXML && !"".equals(contentXML))
						{
							if(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">")!=-1)
							{
								String beforeText = contentXML.substring(0,contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">") );
								String afterText = contentXML.substring(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">"), contentXML.length());
								
								if(null!=beforeText && !"".equals(beforeText) && null!=afterText && !"".equals(afterText))
								{
									contentXML = beforeText;
									StringBuilder str = new StringBuilder();
									// ADD HTML_CONTENT NODE
									str.append("<HTML_CONTENT>");
									str.append("<![CDATA[");
									if(contentDetails.getDocumentType().equals(ContentDetails.HTML_DOCUMENT))
									{
										if(null!=contentDetails.getDocumentContent() && !"".equals(contentDetails.getDocumentContent()))
										{
											str.append(contentDetails.getDocumentContent());
										}
									}
									str.append("]]>");
									str.append("</HTML_CONTENT>");
									
									// ADD FLASH CONTENT NODE
									str.append("<FLASH_CONTENT><![CDATA[");
									if(contentDetails.getDocumentType().equals(ContentDetails.FLASH_DOCUMENT))
									{
										if(null!=contentDetails.getDocumentContent() && !"".equals(contentDetails.getDocumentContent()))
										{
											str.append(contentDetails.getDocumentContent());
										}
									}
									str.append("]]></FLASH_CONTENT>");
									
									str.append("<HTML5_FONT_SELECTION><![CDATA[");
									if(contentDetails.getDocumentType().equals(ContentDetails.FLASH_DOCUMENT))
									{
										if (null != contentDetails.getFontContent() && !"".equals(contentDetails.getFontContent())) 
										{
											str.append(contentDetails.getFontContent());
										}
									}
									str.append("]]></HTML5_FONT_SELECTION>");
									
									// ADD TITLE NODE
									str.append("<TITLE>");
									str.append("<![CDATA[");
									if(null!=contentDetails.getTitle() && !"".equals(contentDetails.getTitle()))
									{
										/*
										 * replace special characters in the title
										 */
//										String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());

										/*
										 * replace special characters in the title
										 * 
										 * Do it for all Locales - 8th November 2016
										 */
										String title = contentDetails.getTitle();
										title = SpecialCharactersUtils.replaceSpecialChars(title);
										
										/*
										 * REPLACE SUP BOTH OPENING & CLOSING TAGS
										 * DATE 17 DEC 2016
										 */
										title = title.replace("<SUP>", "");
										title = title.replace("</SUP>", "");
										title = title.replace("<sup>", "");
										title = title.replace("</sup>", "");
										title = title.replace("<Sup>", "");
										title = title.replace("</Sup>", "");
										title = title.replace("<sUp>", "");
										title = title.replace("</sUp>", "");
										title = title.replace("<suP>", "");
										title = title.replace("</suP>", "");
										title = title.replace("<SUp>", "");
										title = title.replace("</SUp>", "");
										title = title.replace("<sUP>", "");
										title = title.replace("</sUP>", "");
										title = title.replace("<SuP>", "");
										title = title.replace("</SuP>", "");
										/*
//										String title = ConversionUtils.replaceSpecialCharactersInName(contentVO.getTitle());
										
										String locale="";
										if(null!=contentVO.getLocale() && !"".equals(contentVO.getLocale()))
										{
											locale = contentVO.getLocale().toLowerCase();
											locale= locale.replace("_", "-");
											if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("es-mx")))
											{
												title = SpecialCharactersUtils.replaceSpecialChars(title);
											}
											else if(ApplicationProperties.getProperty(locale).equals(ApplicationProperties.getProperty("fr-ca")))
											{
												title = SpecialCharactersUtils.replaceSpecialChars(title);
											}
										}
										locale = null;
										*/
										str.append(title);
										title=null;
									}
									str.append("]]>");
									str.append("</TITLE>");
									contentXML=  contentXML+str.toString()+ afterText;
									str = null;
								}
								beforeText= null;
								afterText= null;
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(InfoQueriaUtils.class.getName(),"performContentXMLOperationsFORWD_HTMLMNAO()", e);
		}
		return contentXML;
	}


	public static String deleteOperationsForServiceManuals(ContentDetails contentDetails, ContentRecordITOImpl content)
	{
		String contentXML = "";
		try
		{
			/*
			 * EMPTY ALL THE NODES EXCEPT TITLE
			 */

			// REPLACE THE ATTACHMENT NODE
			contentXML = content.getXml();
			if(null!=contentXML && !"".equals(contentXML))
			{
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(contentXML));
				Document doc = db.parse(is);
				if(null!=doc)
				{
					NodeList channelNodes = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					if(null!=channelNodes && channelNodes.getLength()>0)
					{
						for(int a=0;a<channelNodes.getLength();a++)
						{
							Node chNode = channelNodes.item(a);
							NodeList childNodesList = chNode.getChildNodes();
							if(null!=childNodesList && childNodesList.getLength()>0)
							{
								for(int b=0;b<childNodesList.getLength();b++)
								{
									Node childNode = childNodesList.item(b);
									if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
									{
										if(childNode.getNodeName().equals("ATTACHMENTS"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("VTOC_FILENAME"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("CONTENT"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("SIE_ID"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("DJVU_FILE_LOCATION"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("OASIS_FILE_LOCATION"))
										{
											chNode.removeChild(childNode);
										}
									}
									childNode = null;
								}
							}
							childNodesList = null;
							chNode  = null;
						}
					}
					
					/*
					 * Check for Attachment Node Explicitly
					 */
					NodeList chNodesListForSecurity = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					Node channelNode = null;
					if (null != chNodesListForSecurity && chNodesListForSecurity.getLength() > 0) 
					{
						// get the node at 0 Index
						channelNode = chNodesListForSecurity.item(0);
					}

					NodeList attachmentNodesList = doc.getElementsByTagName("ATTACHMENTS");
					Node attchNode = null;
					if (null != attachmentNodesList && attachmentNodesList.getLength() > 0) 
					{
						// get the node at 0 Index
						attchNode = attachmentNodesList.item(0);
					}
					if (null != channelNode && null != attchNode) 
					{
						// remove the attachment node from content node
						channelNode.removeChild(attchNode);
					}
					
					attchNode = null;
					attachmentNodesList = null;
					
					/*
					 * Check for CONTENT Node Explicitly
					 */
					NodeList contentNodesList = doc.getElementsByTagName("CONTENT");
					Node contentNode=null;
					if(null!=contentNodesList && contentNodesList.getLength()>0)
					{
						// get the node at 0 index
						contentNode = contentNodesList.item(0);
					}
					
					if(null!=channelNode && null!=contentNode)
					{
						// remove the title node from content node
						channelNode.removeChild(contentNode);
					}
					contentNode = null;
					contentNodesList = null;
					
					/*
					 * Check for VTOC Node Explicitly
					 */
					NodeList vtocNodesList = doc.getElementsByTagName("VTOC_FILENAME");
					Node vtocNode=null;
					if(null!=vtocNodesList && vtocNodesList.getLength()>0)
					{
						// get the node at 0 index
						vtocNode = vtocNodesList.item(0);
					}
					
					if(null!=channelNode && null!=vtocNode)
					{
						// remove the title node from content node
						channelNode.removeChild(vtocNode);
					}
					vtocNode = null;
					vtocNodesList = null;
					
					/*
					 * Check for SIE_ID Node Explicitly
					 */
					NodeList sieNodesList = doc.getElementsByTagName("SIE_ID");
					Node sieNode=null;
					if(null!=sieNodesList && sieNodesList.getLength()>0)
					{
						// get the node at 0 index
						sieNode = sieNodesList.item(0);
					}
					
					if(null!=channelNode && null!=sieNode)
					{
						// remove the title node from content node
						channelNode.removeChild(sieNode);
					}
					sieNode = null;
					sieNodesList = null;
					
					/*
					 * Check for DJVU FILE LOCATION Node Explicitly
					 */
					NodeList djvuFileNodesList = doc.getElementsByTagName("DJVU_FILE_LOCATION");
					Node djvuNode=null;
					if(null!=djvuFileNodesList && djvuFileNodesList.getLength()>0)
					{
						// get the node at 0 index
						djvuNode = djvuFileNodesList.item(0);
					}
					
					if(null!=channelNode && null!=djvuNode)
					{
						// remove the title node from content node
						channelNode.removeChild(djvuNode);
					}
					djvuNode = null;
					djvuFileNodesList = null;
					
					/*
					 * Check for OASIS FILE LOCATION Node Explicitly
					 */
					NodeList oasisFileNodesList = doc.getElementsByTagName("OASIS_FILE_LOCATION");
					Node oasisNode=null;
					if(null!=oasisFileNodesList && oasisFileNodesList.getLength()>0)
					{
						// get the node at 0 index
						oasisNode = oasisFileNodesList.item(0);
					}
					
					if(null!=channelNode && null!=oasisNode)
					{
						// remove the oasis node from content node
						channelNode.removeChild(oasisNode);
					}
					oasisNode = null;
					oasisFileNodesList = null;
					
					
					chNodesListForSecurity = null;
					channelNode = null;
					
					
					if(null!=doc)
					{
						// TRANSFORM TO STRING
						contentXML = Utilities.transformString(doc);
						if(null!=contentXML && !"".equals(contentXML))
						{
							if(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">")!=-1)
							{
								String beforeText = contentXML.substring(0,contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">") );
								String afterText = contentXML.substring(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">"), contentXML.length());
								
								if(null!=beforeText && !"".equals(beforeText) && null!=afterText && !"".equals(afterText))
								{
									contentXML = beforeText;
									StringBuilder str = new StringBuilder();
									// ADD ATTACHMENT NODE
//									str.append("<ATTACHMENT>");
//									str.append("<![CDATA[");
//									str.append("]]>");
//									str.append("</ATTACHMENT>");
									
									str.append("<CONTENT><![CDATA[");
									str.append("]]></CONTENT>");

									str.append("<VTOC_FILENAME><![CDATA[");
									str.append("]]></VTOC_FILENAME>");
									
									str.append("<SIE_ID><![CDATA[");
									str.append("]]></SIE_ID>");
									
									str.append("<DJVU_FILE_LOCATION><![CDATA[");
									str.append("]]></DJVU_FILE_LOCATION>");
									
									str.append("<OASIS_FILE_LOCATION><![CDATA[");
									str.append("]]></OASIS_FILE_LOCATION>");
									
									contentXML=  contentXML+str.toString()+ afterText;
									str = null;
								}
								beforeText= null;
								afterText= null;
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(InfoQueriaUtils.class.getName(),"deleteOperationsForServiceManuals()", e);
		}
		return contentXML;
	}

	public static String deleteOperationsForWiringDiagrams(ContentDetails contentDetails, ContentRecordITOImpl content)
	{
		String contentXML = "";
		try
		{
			/*
			 * EMPTY ALL THE NODES EXCEPT TITLE
			 */

			// REPLACE THE ATTACHMENT NODE
			contentXML = content.getXml();
			if(null!=contentXML && !"".equals(contentXML))
			{
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(contentXML));
				Document doc = db.parse(is);
				if(null!=doc)
				{
					NodeList channelNodes = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					if(null!=channelNodes && channelNodes.getLength()>0)
					{
						for(int a=0;a<channelNodes.getLength();a++)
						{
							Node chNode = channelNodes.item(a);
							NodeList childNodesList = chNode.getChildNodes();
							if(null!=childNodesList && childNodesList.getLength()>0)
							{
								for(int b=0;b<childNodesList.getLength();b++)
								{
									Node childNode = childNodesList.item(b);
									if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
									{
										if(childNode.getNodeName().equals("ATTACHMENT"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("HTML_CONTENT"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("FLASH_CONTENT"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("NAVIGATION"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("HTML5_FILE_NAME"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("HTML5_UPLOAD_DIRECTORY"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("HTML5_FONT_SELECTION"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("DJVU_FILE_LOCATION"))
										{
											chNode.removeChild(childNode);
										}
									}
									childNode = null;
								}
							}
							childNodesList = null;
							chNode  = null;
						}
					}
					
					/*
					 * Check for Attachment Node Explicitly
					 */
					NodeList chNodesListForSecurity = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					Node channelNode = null;
					if (null != chNodesListForSecurity && chNodesListForSecurity.getLength() > 0) 
					{
						// get the node at 0 Index
						channelNode = chNodesListForSecurity.item(0);
					}

					NodeList attachmentNodesList = doc.getElementsByTagName("ATTACHMENT");
					Node attchNode = null;
					if (null != attachmentNodesList && attachmentNodesList.getLength() > 0) 
					{
						// get the node at 0 Index
						attchNode = attachmentNodesList.item(0);
					}
					if (null != channelNode && null != attchNode) 
					{
						// remove the attachment node from content node
						channelNode.removeChild(attchNode);
					}
					
					attchNode = null;
					attachmentNodesList = null;
					
					/*
					 * Check for CONTENT Node Explicitly
					 */
					NodeList contentNodesList = doc.getElementsByTagName("HTML_CONTENT");
					Node contentNode=null;
					if(null!=contentNodesList && contentNodesList.getLength()>0)
					{
						// get the node at 0 index
						contentNode = contentNodesList.item(0);
					}
					
					if(null!=channelNode && null!=contentNode)
					{
						// remove the title node from content node
						channelNode.removeChild(contentNode);
					}
					contentNode = null;
					contentNodesList = null;
					
					/*
					 * Check for FLASH_CONTENT Node Explicitly
					 */
					NodeList flashNodesList = doc.getElementsByTagName("FLASH_CONTENT");
					Node flashNode=null;
					if(null!=flashNodesList && flashNodesList.getLength()>0)
					{
						// get the node at 0 index
						flashNode = flashNodesList.item(0);
					}
					
					if(null!=channelNode && null!=flashNode)
					{
						// remove the flashNode node from content node
						channelNode.removeChild(flashNode);
					}
					flashNode = null;
					flashNodesList = null;
					
					/*
					 * Check for NAVIGATION Node Explicitly
					 */
					NodeList navNodesList = doc.getElementsByTagName("NAVIGATION");
					Node navNode=null;
					if(null!=navNodesList && navNodesList.getLength()>0)
					{
						// get the node at 0 index
						navNode = navNodesList.item(0);
					}
					
					if(null!=channelNode && null!=navNode)
					{
						// remove the title node from content node
						channelNode.removeChild(navNode);
					}
					navNode = null;
					navNodesList = null;
					
					/*
					 * Check for DJVU FILE LOCATION Node Explicitly
					 */
					NodeList djvuFileNodesList = doc.getElementsByTagName("DJVU_FILE_LOCATION");
					Node djvuNode=null;
					if(null!=djvuFileNodesList && djvuFileNodesList.getLength()>0)
					{
						// get the node at 0 index
						djvuNode = djvuFileNodesList.item(0);
					}
					
					if(null!=channelNode && null!=djvuNode)
					{
						// remove the title node from content node
						channelNode.removeChild(djvuNode);
					}
					djvuNode = null;
					djvuFileNodesList = null;
					
					/*
					 * Check for HTML5_FILE_NAME Node Explicitly
					 */
					NodeList htmlFileNodesList = doc.getElementsByTagName("HTML5_FILE_NAME");
					Node htmlFileNode=null;
					if(null!=htmlFileNodesList && htmlFileNodesList.getLength()>0)
					{
						// get the node at 0 index
						htmlFileNode = htmlFileNodesList.item(0);
					}
					
					if(null!=channelNode && null!=htmlFileNode)
					{
						// remove the title node from content node
						channelNode.removeChild(htmlFileNode);
					}
					htmlFileNode = null;
					htmlFileNodesList = null;
					
					/*
					 * Check for HTML5_UPLOAD_DIRECTORY Node Explicitly
					 */
					NodeList htmlUploadDirNodesList = doc.getElementsByTagName("HTML5_UPLOAD_DIRECTORY");
					Node htmlUploadDirNode=null;
					if(null!=htmlUploadDirNodesList && htmlUploadDirNodesList.getLength()>0)
					{
						// get the node at 0 index
						htmlUploadDirNode = htmlUploadDirNodesList.item(0);
					}
					
					if(null!=channelNode && null!=htmlUploadDirNode)
					{
						// remove the title node from content node
						channelNode.removeChild(htmlUploadDirNode);
					}
					htmlUploadDirNode = null;
					htmlUploadDirNodesList = null;
					
					/*
					 * Check for HTML5_FONT_SELECTION Node Explicitly
					 */
					NodeList htmlFontNodesList = doc.getElementsByTagName("HTML5_FONT_SELECTION");
					Node htmlFontNode=null;
					if(null!=htmlFontNodesList && htmlFontNodesList.getLength()>0)
					{
						// get the node at 0 index
						htmlFontNode = htmlFontNodesList.item(0);
					}
					
					if(null!=channelNode && null!=htmlFontNode)
					{
						// remove the title node from content node
						channelNode.removeChild(htmlFontNode);
					}
					htmlFontNode = null;
					htmlFontNodesList = null;
					
					
					chNodesListForSecurity = null;
					channelNode = null;
					
					
					if(null!=doc)
					{
						// TRANSFORM TO STRING
						contentXML = Utilities.transformString(doc);
						if(null!=contentXML && !"".equals(contentXML))
						{
							if(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">")!=-1)
							{
								String beforeText = contentXML.substring(0,contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">") );
								String afterText = contentXML.substring(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">"), contentXML.length());
								
								if(null!=beforeText && !"".equals(beforeText) && null!=afterText && !"".equals(afterText))
								{
									contentXML = beforeText;
									StringBuilder str = new StringBuilder();
									// ADD ATTACHMENT NODE
									str.append("<ATTACHMENT>");
									str.append("<![CDATA[");
									str.append("]]>");
									str.append("</ATTACHMENT>");
									
									str.append("<HTML_CONTENT><![CDATA[");
									str.append("]]></HTML_CONTENT>");

									str.append("<FLASH_CONTENT><![CDATA[");
									str.append("]]></FLASH_CONTENT>");
									
									str.append("<NAVIGATION><![CDATA[");
									str.append("]]></NAVIGATION>");
									
									str.append("<DJVU_FILE_LOCATION><![CDATA[");
									str.append("]]></DJVU_FILE_LOCATION>");
									
									str.append("<HTML5_FILE_NAME><![CDATA[");
									str.append("]]></HTML5_FILE_NAME>");
									
									str.append("<HTML5_UPLOAD_DIRECTORY><![CDATA[");
									str.append("]]></HTML5_UPLOAD_DIRECTORY>");
									
									str.append("<HTML5_FONT_SELECTION><![CDATA[");
									str.append("]]></HTML5_FONT_SELECTION>");
									
									contentXML=  contentXML+str.toString()+ afterText;
									str = null;
								}
								beforeText= null;
								afterText= null;
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(InfoQueriaUtils.class.getName(),"deleteOperationsForWiringDiagrams()", e);
		}
		return contentXML;
	}

	public static String deleteOperationsForOtherServiceManuals(ContentDetails contentDetails, ContentRecordITOImpl content)
	{
		String contentXML = "";
		try
		{
			/*
			 * EMPTY ALL THE NODES EXCEPT TITLE
			 */
			// REPLACE THE ATTACHMENT NODE
			contentXML = content.getXml();
			if(null!=contentXML && !"".equals(contentXML))
			{
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(contentXML));
				Document doc = db.parse(is);
				if(null!=doc)
				{
					NodeList channelNodes = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					if(null!=channelNodes && channelNodes.getLength()>0)
					{
						for(int a=0;a<channelNodes.getLength();a++)
						{
							Node chNode = channelNodes.item(a);
							NodeList childNodesList = chNode.getChildNodes();
							if(null!=childNodesList && childNodesList.getLength()>0)
							{
								for(int b=0;b<childNodesList.getLength();b++)
								{
									Node childNode = childNodesList.item(b);
									if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
									{
										if(childNode.getNodeName().equals("ATTACHMENTS"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("CONTENT"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("DJVU_FILE_LOCATION"))
										{
											chNode.removeChild(childNode);
										}
										else if(childNode.getNodeName().equals("OASIS_FILE_LOCATION"))
										{
											chNode.removeChild(childNode);
										}
									}
									childNode = null;
								}
							}
							childNodesList = null;
							chNode  = null;
						}
					}
					
					/*
					 * Check for Attachment Node Explicitly
					 */
					NodeList chNodesListForSecurity = doc.getElementsByTagName(contentDetails.getChannelName().toUpperCase().replace(" ", "_"));
					Node channelNode = null;
					if (null != chNodesListForSecurity && chNodesListForSecurity.getLength() > 0) 
					{
						// get the node at 0 Index
						channelNode = chNodesListForSecurity.item(0);
					}

					NodeList attachmentNodesList = doc.getElementsByTagName("ATTACHMENTS");
					Node attchNode = null;
					if (null != attachmentNodesList && attachmentNodesList.getLength() > 0) 
					{
						// get the node at 0 Index
						attchNode = attachmentNodesList.item(0);
					}
					if (null != channelNode && null != attchNode) 
					{
						// remove the attachment node from content node
						channelNode.removeChild(attchNode);
					}
					
					attchNode = null;
					attachmentNodesList = null;
					
					/*
					 * Check for CONTENT Node Explicitly
					 */
					NodeList contentNodesList = doc.getElementsByTagName("CONTENT");
					Node contentNode=null;
					if(null!=contentNodesList && contentNodesList.getLength()>0)
					{
						// get the node at 0 index
						contentNode = contentNodesList.item(0);
					}
					
					if(null!=channelNode && null!=contentNode)
					{
						// remove the title node from content node
						channelNode.removeChild(contentNode);
					}
					contentNode = null;
					contentNodesList = null;
					
					/*
					 * Check for DJVU FILE LOCATION Node Explicitly
					 */
					NodeList djvuFileNodesList = doc.getElementsByTagName("DJVU_FILE_LOCATION");
					Node djvuNode=null;
					if(null!=djvuFileNodesList && djvuFileNodesList.getLength()>0)
					{
						// get the node at 0 index
						djvuNode = djvuFileNodesList.item(0);
					}
					
					if(null!=channelNode && null!=djvuNode)
					{
						// remove the title node from content node
						channelNode.removeChild(djvuNode);
					}
					djvuNode = null;
					djvuFileNodesList = null;
					
					NodeList oasisFileNodesList = doc.getElementsByTagName("OASIS_FILE_LOCATION");
					Node oasisNode=null;
					if(null!=oasisFileNodesList && oasisFileNodesList.getLength()>0)
					{
						// get the node at 0 index
						oasisNode = oasisFileNodesList.item(0);
					}
					
					if(null!=channelNode && null!=oasisNode)
					{
						// remove the oasis node from content node
						channelNode.removeChild(oasisNode);
					}
					oasisNode = null;
					oasisFileNodesList = null;
					
					
					chNodesListForSecurity = null;
					channelNode = null;
					
					
					if(null!=doc)
					{
						// TRANSFORM TO STRING
						contentXML = Utilities.transformString(doc);
						if(null!=contentXML && !"".equals(contentXML))
						{
							if(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">")!=-1)
							{
								String beforeText = contentXML.substring(0,contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">") );
								String afterText = contentXML.substring(contentXML.indexOf("</"+ contentDetails.getChannelName().toUpperCase().replace(" ", "_") + ">"), contentXML.length());
								
								if(null!=beforeText && !"".equals(beforeText) && null!=afterText && !"".equals(afterText))
								{
									contentXML = beforeText;
									StringBuilder str = new StringBuilder();
									// ADD ATTACHMENT NODE
//									str.append("<ATTACHMENT>");
//									str.append("<![CDATA[");
//									str.append("]]>");
//									str.append("</ATTACHMENT>");
									
									str.append("<CONTENT><![CDATA[");
									str.append("]]></CONTENT>");

									str.append("<DJVU_FILE_LOCATION><![CDATA[");
									str.append("]]></DJVU_FILE_LOCATION>");
									
									str.append("<OASIS_FILE_LOCATION><![CDATA[");
									str.append("]]></OASIS_FILE_LOCATION>");
									
									contentXML=  contentXML+str.toString()+ afterText;
									str = null;
								}
								beforeText= null;
								afterText= null;
							}
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(InfoQueriaUtils.class.getName(),"deleteOperationsForOtherServiceManuals()", e);
		}
		return contentXML;
	}

	/**
	 * FUNCTION WILL SET THE DISPLAY DATE BEFORE MODIFY OPERATION
	 * @param content
	 */
	public static ContentRecordITOImpl modifyDisplayDate(ContentRecordITOImpl content)
	{
		try
		{
			/*
			 * BEFORE SETTING CHECK IF DISPLAY START DATE YEAR IS 1900 
			 * THEN UPDATE START DATE AS DOCUMENT CREATE DATE
			 */
			if(null!=content.getDisplayStartDate())
			{
				SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
				String convStringDate = sdf.format(content.getDisplayStartDate());
				Date convDate = sdf.parse(convStringDate);
				@SuppressWarnings("deprecation")
				int year = convDate.getYear();
				if(year==1900)
				{
					// CHECK FOR CREATE DATE
					if(null!=content.getCreateDate())
					{
						convStringDate = sdf.format(content.getDisplayStartDate());
						convDate = sdf.parse(convStringDate);
						content.setDisplayStartDate(convDate);
					}
					else
					{
						// set a date less than few minutes before creation date
						Date systemDate = new Date();
						long time = systemDate.getTime();
						// get time before 10 minutes -  600000
						time = time-600000;
						Date newDate = new Date(time);
						
						content.setDisplayStartDate(newDate);
						newDate = null;
						systemDate=  null;
					}
				}
				convDate = null;
				convStringDate= null;
				sdf = null;
			}
			else
			{
				// set a date less than few minutes before creation date
				Date systemDate = new Date();
				long time = systemDate.getTime();
				// get time before 10 minutes -  600000
				time = time-600000;
				Date newDate = new Date(time);
				
				content.setDisplayStartDate(newDate);
				newDate = null;
				systemDate=  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(InfoQueriaUtils.class.getName(), "modifyDisplayDate()", e);
		}
		return content;
	}
}
