package com.mazda.gms3.dmt.conversion.dao;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.mazda.gms3.dmt.conversion.utils.ConversionUtils;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.CVCCategoryDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;

public class EntParsing {

	static Logger logger = LogManager.getLogger(EntParsing.class);
	
	public static void main(String args[]) throws IOException
//	public static void testData() 
	{
		try
		{

			System.out.println(" ------------------- inside testData------------");
			logger.info(" ------------------- inside testData------------");
			//File entFile = PathUtil.file("C:\\Users\\vishal\\Desktop\\GMS3\\SampleContent\\GMS3_Contents_oLD\\MNAO\\en_US\\MX-5\\SH\\3564-1U-15D_XML\\ent.dir\\id0920zz113600.ent");
			File entFile = PathUtil.file("C:\\Users\\v118433\\Documents\\WD\\id9902i0300300.ent");
//			File entFile = PathUtil.file("\\\\ocmst0029\\DEV\\sourcecontentdev\\test\\MC\\ja-JP\\NewM_DM\\DC01\\FL0000\\DC01_XML\\ent.gms3\\id980103001500.ent");
			ContentDetails contentDetails= new ContentDetails();
			contentDetails.setFilePath(PathUtil.winPath(entFile));
			contentDetails.setLocale("ja_JP");
			contentDetails = parseEntFile(entFile, "ja_JP", contentDetails);
			
			System.out.println(" fileName :: > "+ contentDetails.getFileNameAttribute());
			logger.info(" fileName :: > "+ contentDetails.getFileNameAttribute());
			
//			if(null!=contentDetails.getCvcCategoryList())
//			{
//				for(int i=0;i<contentDetails.getCvcCategoryList().size();i++)
//				{
//					CVCCategoryDetails ccDetails = (CVCCategoryDetails)contentDetails.getCvcCategoryList().get(i);
//					System.out.println(" cat code {"+ccDetails.getCategoryCode()+"}      sym code {"+ccDetails.getSymptomCode()+"}      sybsym code {"+ccDetails.getSubSymptomCode()+"}      cond code {"+ccDetails.getConditionCode()+"}");		
//				}
//			}
//			
			
//			System.out.println("html content:: >" + contentDetails.getDocumentContent());
//			logger.info("html content:: >" + contentDetails.getDocumentContent());
//			System.out.println( " mapped VIN :: > " + contentDetails.getMappedVinEntFileName());
//			System.out.println( " mapped VIN Att :: > " + contentDetails.getMappedVinAttributeFileName());
//			System.out.println(" cat code :: > " + contentDetails.getCategoryCode());
//			System.out.println(" Sub cat code :: > " + contentDetails.getSubCategoryCode());
//			
//			contentDetails =  WiringDiagramUtils.prepareInnerLinkPaths(contentDetails);
			
//			String htmlContent = WiringDiagramUtils.replaceImagesSrcContent(contentDetails.getDocumentContent(), contentDetails);
			
//			String regEx = "\\\\^[\\\\p{L}\\\\p{P}\\\\p{N}\\\\p{So}\\\\p{Sc}\\\\s+%26&=&amp%\\\\\\\\-_|<>/]+$";
//			String regEx = "<\\\\/?\\\\w+\\\\s+[\\\\^>]*>";
//			String regEx="\\\\w+\\\\s*=\\\\s*\".*?\"";
			
			
//			File htmlFile = PathUtil.file("C:\\GMS3_WD\\id152000004900.html");
			File htmlFile = PathUtil.file("C:\\Users\\v118433\\Documents\\WD\\abc.html");
			FileOutputStream fos  = PathUtil.fileOutputStream(htmlFile);
			fos.write(contentDetails.getDocumentContent().getBytes());
			fos.flush();fos.close();
			
		
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main_1(String[] args) {
		try
		{
			String dtdPath = "file:///C:/Users/vishal/Desktop/GMS3/StyleSheet/StyleSheet/DtdsEntities/xml/mazdaace.dtd";
			
			File entFile = PathUtil.file("C:\\Users\\vishal\\Desktop\\GMS3\\SampleContent\\GMS3_Contents\\MNAO\\en-US\\Mission\\B30\\1A93-1U-15F_XML\\ent.dir\\id000000156700.ent");
			File xslFile=  PathUtil.file("C:\\Users\\vishal\\Desktop\\GMS3\\StyleSheet\\StyleSheet\\Mazda-html\\mazda-html-en.xsl");
			
			
			String entFileContent = ConversionUtils.getStringFromXML(entFile);
			String xslFileContent = ConversionUtils.getStringFromXML(xslFile);
			entFileContent = entFileContent.replace("..\\dtd\\mazdaace.dtd", dtdPath);
			
			String initialContent = "";
			if(entFileContent.indexOf("]>")!=-1)
			{
				initialContent = entFileContent.substring(0,entFileContent.indexOf("]>")+2);
			}
			String middleContent="";
			
			if(entFileContent.indexOf("]>")!=-1 && entFileContent.indexOf("<Servinfo")!=-1)
			{
				middleContent= entFileContent.substring(entFileContent.indexOf("]>")+2,entFileContent.indexOf("<Servinfo"));
				System.out.println("middleContent :: > " + middleContent);
//				
//				<VINData>
//				<VINFile>vin1.ent</VINFile>
//				</VINData>
//				<VINATTRIBUTEData>
//				<VINATTRIBUTEFile>vin_attribute1.ent</VINATTRIBUTEFile>
//				</VINATTRIBUTEData>
//				<Servcatype>09</Servcatype>
//				<Servsubcatype>0980</Servsubcatype>
			}
			
			String remainingContent = "";
			if(entFileContent.indexOf("<Servinfo")!=-1)
			{
				remainingContent = entFileContent.substring(entFileContent.indexOf("<Servinfo"),entFileContent.length());
			}
			entFileContent = initialContent+remainingContent;
	
			InputStream xmlIpStream = new ByteArrayInputStream(
					entFileContent.getBytes("UTF-8"));
			InputStream xslIpStream = new ByteArrayInputStream(
					xslFileContent.getBytes("UTF-8"));
			
			String htmlContent = transformXML(xmlIpStream, xslIpStream);
//			System.out.println(htmlContent);
			

			File htmlFile=  PathUtil.file("C:\\Users\\vishal\\Desktop\\GMS3\\test.html");
			FileOutputStream fos = PathUtil.fileOutputStream(htmlFile);
			fos.write(htmlContent.getBytes());
			fos.flush();fos.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static ContentDetails parseEntFile(File entFile, String locale, ContentDetails contentDetails)
	{
		try
		{
			String htmlContent="";
			if(null!=entFile && entFile.isFile())
			{
				String xslName = getXslName(locale);
				if(null!=xslName && !"".equals(xslName))
				{
					logger.info("parseEntFile :: XSL Identified for Parsing ENT Which is :: > " + xslName);
					// append XSL Path to the Name
					String xslPath = ApplicationProperties.getProperty("XSL_LOCATION");
					if(null!=xslPath && !"".equals(xslPath))
					{
						xslPath =xslPath.replace("/", "\\");
						if(!xslPath.endsWith("\\"))
						{
							xslPath = xslPath+"\\";
						}
						xslPath= xslPath+xslName;
						File xslFile = PathUtil.file(xslPath);
						if(xslFile.isFile() && xslFile.exists())
						{
							String dtdPath = "file:///"+ApplicationProperties.getProperty("MAZDA_DTD_PATH");
							
							String entFileContent = ConversionUtils.getStringFromXML(entFile);
							String xslFileContent = ConversionUtils.getStringFromXML(xslFile);
							
							/*
							 * Perform XSL Operations
							 */
							if(null!=xslFileContent && !"".equals(xslFileContent))
							{
								String parentPath = PathUtil.winPath(xslFile.getParentFile());
								parentPath = parentPath.replace("\\", "/");
								parentPath = "file:///"+parentPath;
								if(!parentPath.endsWith("/"))
								{
									parentPath = parentPath+"/";
								}
								parentPath = parentPath+"mazda-comm-html-main.xsl";
								xslFileContent= xslFileContent.replace("mazda-comm-html-main.xsl", parentPath);
								parentPath = null;
							}
								
							/*
							 * Perform ENT Operations
							 */
							if(null!=entFileContent && !"".equals(entFileContent))
							{
								// update File Name Attribute in CONTENT DETAILS
								String fileNameAttribute = getFileNameAttributeFromEnt(entFileContent);
								if(null!=fileNameAttribute && !"".equals(fileNameAttribute))
								{
									contentDetails.setFileNameAttribute(fileNameAttribute);
								}
								
								fileNameAttribute= null;
								
								// replace DTD Path
								entFileContent = entFileContent.replace("..\\dtd\\mazdaace.dtd", dtdPath);
								
								/*
								 * Now remove the CVC DATA, VIN ENT MAPPING AND VIN ATTRIBUTE ENT MAPPING
								 * DATA
								 */
								String initialContent = "";
								if(entFileContent.indexOf("]>")!=-1)
								{
									initialContent = entFileContent.substring(0,entFileContent.indexOf("]>")+2);
								}
								String middleContent="";
								
								if(entFileContent.indexOf("]>")!=-1 && entFileContent.indexOf("<Servinfo")!=-1)
								{
									middleContent= entFileContent.substring(entFileContent.indexOf("]>")+2,entFileContent.indexOf("<Servinfo"));
//									System.out.println("middleContent :: > " + middleContent);
									contentDetails = parseMiddleContentBlock(middleContent, contentDetails);
//									
//									<VINData>
//									<VINFile>vin1.ent</VINFile>
//									</VINData>
//									<VINATTRIBUTEData>
//									<VINATTRIBUTEFile>vin_attribute1.ent</VINATTRIBUTEFile>
//									</VINATTRIBUTEData>
//									<Servcatype>09</Servcatype>
//									<Servsubcatype>0980</Servsubcatype>
								}
								
								String remainingContent = "";
								if(entFileContent.indexOf("<Servinfo")!=-1)
								{
									remainingContent = entFileContent.substring(entFileContent.indexOf("<Servinfo"),entFileContent.length());
								}
								entFileContent = initialContent+remainingContent;
								remainingContent = null;
								initialContent = null;
							}
							
							
							/*
							 * TRANSFORM DATA
							 */
							if(null!=entFileContent && !"".equals(entFileContent) && null!=xslFileContent && !"".equals(xslFileContent))
							{
								InputStream xmlIpStream = new ByteArrayInputStream(entFileContent.getBytes("UTF-8"));
								InputStream xslIpStream = new ByteArrayInputStream(xslFileContent.getBytes("UTF-8"));
								// transform Data
								htmlContent = transformXML(xmlIpStream, xslIpStream);
//								logger.info("------------------- parsing -------------------- 1");
								if(null!=htmlContent && !"".equals(htmlContent))
								{
									/*
									 * Before proceeding remove the Following Tags from the generated Content
									 * <a name="id000000156700"></a> - e.g. remove all the a Tags with NAME attr as not null
									 * and <p class="servinfo-title-id" align="right">id000000156700</p>
									 * 
									 * DO NOT REMOVE SUCH TAGS - 
									 * 
									 * AS PER DICUSSION ON 11 JUNE 2016
									 * 
									 * 
									 *
									org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
									org.jsoup.select.Elements aTagsList = doc.select("a");

									if (null != aTagsList && aTagsList.size() > 0) {
										for (int i = 0; i < aTagsList.size(); i++) {
											if(null!=aTagsList.get(i).attr("name") && !"".equals(aTagsList.get(i).attr("name")) && aTagsList.get(i).attr("name").toLowerCase().startsWith("id"))
											{
												// remove the HTML Content
												aTagsList.get(i).remove();
											}
										}
									}
									aTagsList = null;
									
									
									org.jsoup.select.Elements pTagsList = doc.select("p");
									if (null != pTagsList && pTagsList.size() > 0) {
										for (int i = 0; i < pTagsList.size(); i++) {
											if(null!=pTagsList.get(i).attr("class") && !"".equals(pTagsList.get(i).attr("class")) && pTagsList.get(i).attr("class").toLowerCase().equals("servinfo-title-id"))
											{
												// remove the HTML Content
												pTagsList.get(i).remove();
											}
										}
									}
									pTagsList = null;
									htmlContent = doc.toString();
									*/
									
									if(null!=htmlContent && !"".equals(htmlContent))
									{
										// replace &nbsp;&nbsp;
										htmlContent = htmlContent.replace("&nbsp;&nbsp;", "");
//										logger.info("-=------- ENT PARSING ::>"+ htmlContent);
										contentDetails.setDocumentContent(htmlContent);
									}
//									doc=  null;
								}
								xmlIpStream.close();xmlIpStream=null;
								xslIpStream.close();xslIpStream=null;
							}
							entFileContent = null;
							xslFileContent = null;
							dtdPath= null;
						}
						else
						{
							logger.info("parseEntFile :: XSL File Does not Exists at Path :: > " + xslPath);
						}
						xslFile= null;
					}
					xslPath=null;
				}
				xslName= null;
			}
			else
			{
				logger.info("parseEntFile :: Ent File Does not Exists at Path :: > " + PathUtil.winPath(entFile));
			}
			htmlContent = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(EntParsing.class.getName(), "parseEntFile()", e);
		}
		return contentDetails;
	}
	
	public static String transformXML(InputStream xmlIpStream,
			InputStream xslIpStream) throws Exception {
		ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
		try {
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory
					.newTransformer(new javax.xml.transform.stream.StreamSource(
							xslIpStream));
			transformer.transform(new StreamSource(xmlIpStream),
					new StreamResult(resultStream));
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return resultStream.toString();

	}

	private static String getXslName(String locale)
	{
		// set default XSL as EN XSL
		String xslName=ApplicationProperties.getProperty("EN_XSL_NAME");
		if(null!=locale && !"".equals(locale))
		{
			if(locale.equals(ApplicationProperties.getProperty("en-us")))
			{
				xslName=ApplicationProperties.getProperty("EN_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("es-mx")))
			{
				xslName=ApplicationProperties.getProperty("ES_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("fr-ca")))
			{
				xslName=ApplicationProperties.getProperty("FR_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("ja-jp")))
			{
				xslName = ApplicationProperties.getProperty("JP_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("en-uk")))
			{
				xslName = ApplicationProperties.getProperty("EN_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("cs-cz")))
			{
				xslName = ApplicationProperties.getProperty("CS_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("de-de")))
			{
				xslName = ApplicationProperties.getProperty("DE_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("fi-fi")))
			{
				xslName = ApplicationProperties.getProperty("FI_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("it-it")))
			{
				xslName = ApplicationProperties.getProperty("IT_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("pl-pl")))
			{
				xslName = ApplicationProperties.getProperty("PL_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("sv-se")))
			{
				xslName = ApplicationProperties.getProperty("SV_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("pt-pt")))
			{
				xslName = ApplicationProperties.getProperty("PT_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("tr-tr")))
			{
				xslName = ApplicationProperties.getProperty("TR_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("fr-fr")))
			{
				xslName = ApplicationProperties.getProperty("FR_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("el-gr")))
			{
				xslName = ApplicationProperties.getProperty("EL_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("ru-ru")))
			{
				xslName = ApplicationProperties.getProperty("RU_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("nl-nl")))
			{
				xslName = ApplicationProperties.getProperty("NL_XSL_NAME");
			}
			else if(locale.equals(ApplicationProperties.getProperty("es-es")))
			{
				xslName = ApplicationProperties.getProperty("ES_XSL_NAME");
			}
		}
		return xslName;
	}
	
	/**
	 * Parse the Following Data
	 * 	<VINData>
			<VINFile>vin1.ent</VINFile>
		</VINData>
		<VINATTRIBUTEData>
			<VINATTRIBUTEFile>vin_attribute1.ent</VINATTRIBUTEFile>
		</VINATTRIBUTEData>
		<Servcatype>00</Servcatype>
		<Servsubcatype>0000</Servsubcatype>
		<CVCData>
			<CVCcatcode>Q3</CVCcatcode>
			<CVCsympcode>4D</CVCsympcode>
			<CVCsubsympcode>PE</CVCsubsympcode>
			<CVCcondcode>00</CVCcondcode>
		</CVCData>
	 * @param xmlDocument
	 * @param contentDetails
	 * @return
	 */
	private static ContentDetails parseMiddleContentBlock(String xmlDocument, ContentDetails contentDetails)
	{
		try
		{
			if(null!=xmlDocument && !"".equals(xmlDocument))
			{
				xmlDocument = "<ROOT>"+xmlDocument+"</ROOT>";
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(xmlDocument));
				Document doc = db.parse(is);
				if(null!=doc)
				{
					/*
					 * DO NOTHING FOR Servcatype & Servsubcatype
					 * AS THEY WILL BE READ FROM LEFT MENU.TXT FILE
					NodeList nodes = doc.getElementsByTagName("Servcatype");
					if(null!=nodes && nodes.getLength()>0)
					{
						// set category
						contentDetails.setCategoryCode(nodes.item(0).getTextContent());
					}
					nodes = null;

					nodes = doc.getElementsByTagName("Servsubcatype");
					if(null!=nodes && nodes.getLength()>0)
					{
						// set category
						contentDetails.setSubCategoryCode(nodes.item(0).getTextContent());
					}
					nodes = null;
					*/

					NodeList nodes = doc.getElementsByTagName("VINATTRIBUTEFile");
					if(null!=nodes && nodes.getLength()>0)
					{
						// set category
						contentDetails.setMappedVinAttributeFileName(nodes.item(0).getTextContent());
					}
					nodes = null;

					nodes = doc.getElementsByTagName("VINFile");
					if(null!=nodes && nodes.getLength()>0)
					{
						// set category
						contentDetails.setMappedVinEntFileName(nodes.item(0).getTextContent());
					}
					nodes = null;
					
					
					nodes = doc.getElementsByTagName("CVCData");
					if(null!=nodes && nodes.getLength()>0)
					{
						for(int a=0;a<nodes.getLength();a++)
						{
							Node cvcDataNode = nodes.item(a);
							// GET CHILD LIST
							NodeList cvcChildList = cvcDataNode.getChildNodes();
							if(null!=cvcChildList && cvcChildList.getLength()>0)
							{
								CVCCategoryDetails cDetails = new CVCCategoryDetails();
								for(int r=0;r<cvcChildList.getLength();r++)
								{
									Node childNode = cvcChildList.item(r);
									if(!"#text".equalsIgnoreCase(childNode.getNodeName()))
									{
										if(childNode.getNodeName().equals("CVCcatcode"))
										{
											cDetails.setCategoryCode(childNode.getTextContent());
										}
										else if(childNode.getNodeName().equals("CVCsubcatcode"))
										{
											cDetails.setSubCategoryCode(childNode.getTextContent());
										}
										else if(childNode.getNodeName().equals("CVCsympcode"))
										{
											cDetails.setSymptomCode(childNode.getTextContent());
										}
										else if(childNode.getNodeName().equals("CVCsubsympcode"))
										{
											cDetails.setSubSymptomCode(childNode.getTextContent());
										}
										else if(childNode.getNodeName().equals("CVCcondcode"))
										{
											cDetails.setConditionCode(childNode.getTextContent());
										}
									}
									childNode = null;
								}
								if(null==contentDetails.getCvcCategoryList() || contentDetails.getCvcCategoryList().size()<=0)
								{
									contentDetails.setCvcCategoryList(new ArrayList<CVCCategoryDetails>());
								}
								// add cDetails to contentDetails
								contentDetails.getCvcCategoryList().add(cDetails);
								cDetails= null;
							}
							cvcChildList = null;
							cvcDataNode = null;
						}
					}
					nodes= null;
				}
				doc=  null;
				is = null;
				db = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(EntParsing.class.getName(), "parseMiddleContentBlock()", e);
		}
		
		return contentDetails;
	}
	
	/**
	 * Function will get the FILE NAME ATTRIBUTE FROM ENT FILE
	 * @param entContent
	 * @return
	 */
	public static String getFileNameAttributeFromEnt(String entContent)
	{
		String fileNameAttribute="";
		try
		{
			if(null!=entContent && !"".equals(entContent))
			{
				org.jsoup.nodes.Document doc = Jsoup.parse(entContent);
				org.jsoup.select.Elements servInfoTagsList  = doc.select("Servinfo");
				if(null!=servInfoTagsList && servInfoTagsList.size()>0)
				{
					Element servInfoElement = servInfoTagsList.get(0);
					String fileNameAttr = servInfoElement.attr("filename");
					if(null!=fileNameAttr && !"".equals(fileNameAttr))
					{
						fileNameAttribute = fileNameAttr;
					}
					fileNameAttr=null;
					servInfoElement=null;
				}
				servInfoTagsList=null;
				doc=null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(EntParsing.class.getName(), "getFileNameAttributeFromEnt()", e);
		}
		return fileNameAttribute;
	}
}
