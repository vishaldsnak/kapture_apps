package com.mazda.gms3.dmt.conversion.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.ParseXMLDoc;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ChildModelFolderDetails;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.DeleteFileDetails;
import com.mazda.gms3.dmt.vo.LeftMenuFileDetails;
import com.mazda.gms3.dmt.vo.VINEntFileDetails;
import com.mazda.gms3.dmt.vo.VinAttributeEntFileDetails;

public class ConversionUtils {

	static Logger logger = LogManager.getLogger(ConversionUtils.class);
	
	
	
	public static ArrayList<LeftMenuFileDetails> readLeftMenuTextFile(File leftMenuFile) throws IOException
	{
		BufferedReader br = null;
		ArrayList<LeftMenuFileDetails> leftMenuDetailsList = null;
		try
		{
			if(null!=leftMenuFile && leftMenuFile.exists() && leftMenuFile.isFile())
			{
				String categoryCode=null;
				String subCategoryCode=null;
				String subSubCategoryCode=null;
				String title=null;
				String fileName=null;
				String vtocFileName=null;
			    br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(leftMenuFile.getAbsoluteFile()), "UTF-8"));
			    
			    String line;
			    while ((line = br.readLine()) != null) 
			    {
			    	if(null!=line && !"".equals(line))
			    	{
			    		if(line.indexOf("SIE=")!=-1)
			    		{
			    			String beforeData = line.substring(0,line.indexOf("SIE="));
			    			
			    			String dataFromSIE = line.substring(line.indexOf("SIE="), line.length());
			    			if(null!=dataFromSIE && !"".equals(dataFromSIE))
			    			{
			    				dataFromSIE = dataFromSIE.trim();
			    				/*
			    				 * CHECK HERE FOR FILENAME ATTRIBUTE (e.g. VTOC File Name)
			    				 * if found, then set VTOC File Name in LEFT MENU
			    				 */
			    				if(dataFromSIE.indexOf("FILENAME=")!=-1)
			    				{
			    					fileName = dataFromSIE.substring(0, dataFromSIE.indexOf("FILENAME="));
			    					if(null!=fileName && !"".equals(fileName))
			    					{
			    						fileName= fileName.replace("SIE=", "");
			    						fileName= fileName.trim();
			    					}
			    					vtocFileName = dataFromSIE.substring(dataFromSIE.indexOf("FILENAME="), dataFromSIE.length());
			    					if(null!=vtocFileName && !"".equals(vtocFileName))
			    					{
			    						vtocFileName= vtocFileName.replace("FILENAME=", "");
			    						vtocFileName = vtocFileName.trim();
			    					}
			    				}
			    				else
			    				{
			    					// FILE NAME ATTRIBUTE DOES NOT EXISTS - THE DATA IS SIE ID
			    					fileName= dataFromSIE.trim();
			    					if(null!=fileName && !"".equals(fileName))
			    					{
			    						fileName= fileName.replace("SIE=", "");
			    						fileName= fileName.trim();
			    					}
			    				}
			    			}
			    			dataFromSIE = null;
			    			if(null!=beforeData && !"".equals(beforeData))
			    			{
			    				beforeData = beforeData.trim();
			    				int catCheckLength=0;
			    				int subCatCheckLength=0;
			    				int subSubCatCheckLength=0;
			    				if(beforeData.trim().toLowerCase().startsWith("d"))
			    				{
			    					catCheckLength = 4;
			    					subCatCheckLength = 7;
			    					subSubCatCheckLength= 2;
			    				}
			    				else
			    				{
			    					catCheckLength = 3;
			    					subCatCheckLength = 6;
			    					subSubCatCheckLength= 2;
			    				}
			    				if(beforeData.length()>catCheckLength)
			    				{
			    					categoryCode = beforeData.substring(0,catCheckLength);
			    					String afterCatCode = beforeData.substring(catCheckLength,beforeData.length());
			    					if(null!=afterCatCode && !"".equals(afterCatCode))
			    					{
			    						afterCatCode = afterCatCode.trim();
			    						if(afterCatCode.length()>subCatCheckLength)
			    						{
			    							subCategoryCode = afterCatCode.substring(0,subCatCheckLength);
			    							String afterSubCatCode = afterCatCode.substring(subCatCheckLength, afterCatCode.length());
			    							if(null!=afterSubCatCode && !"".equals(afterSubCatCode))
			    							{
			    								afterSubCatCode = afterSubCatCode.trim();
			    								if(afterSubCatCode.length()>subSubCatCheckLength)
			    								{
			    									subSubCategoryCode = afterSubCatCode.substring(0, subSubCatCheckLength);
			    									title = afterSubCatCode.substring(subSubCatCheckLength, afterSubCatCode.length());
			    								}
			    							}
			    							afterSubCatCode = null;
			    						}
			    					}
			    					afterCatCode = null;
			    				}
			    			}
			    			beforeData = null;
			    			
			    			
			    			/*
			    			 * Check here whether the record has to be skipped or not.
			    			 * if subCategoryCode endsWith - C00 - these are CONN HTMLs 
			    			 * skip these entries
			    			 */
			    			if(null!=categoryCode && !"".equals(categoryCode) && 
			    					null!=subCategoryCode && !"".equals(subCategoryCode) && null!=subSubCategoryCode && !"".equals(subSubCategoryCode) 
			    					&& null!=title && !"".equals(title) && null!=fileName && !"".equals(fileName))
			    			{
			    				if(!subCategoryCode.trim().toLowerCase().endsWith("c00"))
			    				{
			    					categoryCode = categoryCode.trim();
			    					subCategoryCode = subCategoryCode.trim();
			    					subSubCategoryCode = subSubCategoryCode.trim();
			    					/*
			    					 * add to Left Menu List - not for CONN HTMLs
			    					 */
			    					LeftMenuFileDetails lfDetails = new LeftMenuFileDetails();
			    					if(categoryCode.trim().toLowerCase().startsWith("d"))
			    					{
			    						categoryCode = categoryCode.substring(1,categoryCode.length());
			    					}
			    					if(subCategoryCode.trim().toLowerCase().startsWith("d"))
			    					{
			    						subCategoryCode = subCategoryCode.substring(1, subCategoryCode.length());
			    					}
			    					
//			    					logger.info("readLeftMenuTextFile :: CAT {"+categoryCode.trim()+"}  SUB CAT {"+subCategoryCode+"}  TITLE {"+title+"}  FILE NAME {"+fileName+"}");
			    					lfDetails.setCategoryCode(categoryCode.trim());
			    					lfDetails.setSubCategoryCode(subCategoryCode.trim());
			    					lfDetails.setSubSubCategoryCode(subSubCategoryCode.trim());
			    					lfDetails.setTitle(title.trim());
			    					System.out.println(SpecialCharactersUtils.replaceSpecialChars(title));
			    					lfDetails.setFileName(fileName.trim());
			    					if(null!=vtocFileName && !"".equals(vtocFileName))
			    					{
			    						lfDetails.setVtocFileNameAttribute(vtocFileName.trim());
			    					}
			    					if(null==leftMenuDetailsList || leftMenuDetailsList.size()<=0)
			    					{
			    						leftMenuDetailsList = new ArrayList<LeftMenuFileDetails>();
			    					}
			    					// add lfDetails to leftMenuDetailsList
			    					leftMenuDetailsList.add(lfDetails);
			    					lfDetails=null;
			    				}
			    			}
			    		}
			    	}
			    }
			    line = null;
			   
			}
			else
			{
				logger.info("readLeftMenuTextFile :: Left Menu Text file is either Corrupted or Does not Exist.");
				leftMenuDetailsList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readLeftMenuTextFile()", e);
			leftMenuDetailsList = null;
			e.printStackTrace();
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return leftMenuDetailsList;
	}
	
	public static void main(String args[])
	{
		try
		{
			File f = PathUtil.file("C:\\GMS3_WD\\leftmenu.txt");
			readLeftMenuTextFile(f);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static ArrayList<VINEntFileDetails> readVINENTFile(File vinEntFile) 
	{
		ArrayList<VINEntFileDetails> vinEntDetailsList = new ArrayList<VINEntFileDetails>();
		try
		{
			if(null!=vinEntFile && vinEntFile.isFile() && vinEntFile.exists())
			{
				Document document = ParseXMLDoc.parseFile(PathUtil.winPath(vinEntFile));
				if(null!=document)
				{
					NodeList vinInfoNodeList = document.getElementsByTagName("VININFO");
					if(null!=vinInfoNodeList && vinInfoNodeList.getLength()>0)
					{
						/*
						 * Parse Strcuture
						 * 	<VININFO>
								<VINDATA>
								<ModelName>CX90</ModelName>
								<Modelyear>2016MY</Modelyear>
								<VINWMI>JM1</VINWMI>
								<VINcarline>ND</VINcarline>
								<VINVDS>NDAA7*G#</VINVDS>
								<VINStart>100001</VINStart>
								<VINEnd>zzzzzz</VINEnd>
								</VINDATA>
							</VININFO>
						*/
						NodeList vinDataNodesList = vinInfoNodeList.item(0).getChildNodes();
						if(null!=vinDataNodesList && vinDataNodesList.getLength()>0)
						{
							for(int i=0;i<vinDataNodesList.getLength();i++)
							{
								Node vinDataNode = (Node)vinDataNodesList.item(i);
								if (!"#text".equalsIgnoreCase(vinDataNode.getNodeName()))
								{
									if(vinDataNode.getNodeName().equals("VINDATA"))
									{
										VINEntFileDetails details = new VINEntFileDetails();
										
										
										NodeList childNodesList = vinDataNode.getChildNodes();
										if(null!=childNodesList && childNodesList.getLength()>0)
										{
											for(int j=0;j<childNodesList.getLength();j++)
											{
												Node childNode = (Node)childNodesList.item(j);
												if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
												{
													if(childNode.getNodeName().equals("ModelName"))
													{
														details.setModelNameVINEnt(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("Modelyear"))
													{
														details.setModelYear(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINWMI"))
													{
														details.setVinWMI(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINcarline"))
													{
														details.setVinCarline(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINVDS"))
													{
														details.setVinVDS(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINStart"))
													{
														details.setVinStartRange(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINEnd"))
													{
														details.setVinEndRange(ParseXMLDoc.getElementValue(childNode));
													}
												}
												childNode=  null;
											}
										}
										
										/*
										 * add details to vinENtList
										 */
										// ADD FILE NAME AS WELL TO THE LIST
										details.setFileName(vinEntFile.getName());
										vinEntDetailsList.add(details);
										details =null;
										
										childNodesList=  null;
									}
								}
								vinDataNode=  null;
							}
						}
						vinDataNodesList = null;
					}
					vinInfoNodeList = null;
				}
				document = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readVINENTFile()", e);
			vinEntDetailsList = null;
		}
		return vinEntDetailsList;
	}

	public static ArrayList<VinAttributeEntFileDetails> readVINAttributeENTFile(File vinAttributeEntFile) 
	{
		ArrayList<VinAttributeEntFileDetails> vinAttributeEntDetailsList = new ArrayList<VinAttributeEntFileDetails>();
		try
		{
			if(null!=vinAttributeEntFile && vinAttributeEntFile.isFile() && vinAttributeEntFile.exists())
			{
				Document document = ParseXMLDoc.parseFile(PathUtil.winPath(vinAttributeEntFile));
				if(null!=document)
				{
					NodeList vinAttrNodeList = document.getElementsByTagName("VINATTR");
					if(null!=vinAttrNodeList && vinAttrNodeList.getLength()>0)
					{
						/*
						 * Parse Structure
						 * 	<VINATTR>
								<VINATTRDATA>
								<VINtranstype>B48</VINtranstype>
								<VINenginetype>A27</VINenginetype>
								<VINbodytype>3</VINbodytype>
								<VINbodytypevalue>OPEN</VINbodytypevalue>
								<VINdriveaxle>2</VINdriveaxle>
								<VINdriveaxlevakue>2WD</VINdriveaxlevakue>
								</VINATTRDATA>
							</VINATTR>
						*/
						NodeList vinAttrDataNodeList = vinAttrNodeList.item(0).getChildNodes();
						if(null!=vinAttrDataNodeList && vinAttrDataNodeList.getLength()>0)
						{
							for(int i=0;i<vinAttrDataNodeList.getLength();i++)
							{
								Node vinAttrDataNode = (Node)vinAttrDataNodeList.item(i);
								if (!"#text".equalsIgnoreCase(vinAttrDataNode.getNodeName()))
								{
									if(vinAttrDataNode.getNodeName().equals("VINATTRDATA"))
									{
										VinAttributeEntFileDetails details = new VinAttributeEntFileDetails();
										
										NodeList childNodesList = vinAttrDataNode.getChildNodes();
										if(null!=childNodesList && childNodesList.getLength()>0)
										{
											for(int j=0;j<childNodesList.getLength();j++)
											{
												Node childNode = (Node)childNodesList.item(j);
												if (!"#text".equalsIgnoreCase(childNode.getNodeName()))
												{
													if(childNode.getNodeName().equals("VINtranstype"))
													{
														details.setVinTransType(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINenginetype"))
													{
														details.setVinEngineType(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINbodytype"))
													{
														details.setVinBodyType(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINbodytypevalue"))
													{
														details.setVinBodyTypeValue(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINdriveaxle"))
													{
														details.setVinAxleType(ParseXMLDoc.getElementValue(childNode));
													}
													else if(childNode.getNodeName().equals("VINdriveaxlevakue"))
													{
														details.setVinAxleTypeValue(ParseXMLDoc.getElementValue(childNode));
													}
												}
												childNode=  null;
											}
										}
										/*
										 * add details to vinAttribnuteENtList
										 */
										// ADD FILE NAME AS WELL TO THE LIST
										details.setFileName(vinAttributeEntFile.getName());
										vinAttributeEntDetailsList.add(details);
										details =null;
										
										childNodesList=  null;
									}
								}
								vinAttrDataNode=  null;
							}
						}
						vinAttrDataNodeList = null;
					}
					vinAttrNodeList = null;
				}
				document = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readVINAttributeENTFile()", e);
			vinAttributeEntDetailsList = null;
		}
		return vinAttributeEntDetailsList;
	}
	
	public static ArrayList<DeleteFileDetails> readDeleteTextFile(File deleteTextFile) throws IOException
	{
		BufferedReader br = null;
		ArrayList<DeleteFileDetails> deleteDetailsList = null;
		try
		{
			if(null!=deleteTextFile && deleteTextFile.exists() && deleteTextFile.isFile())
			{
				boolean isDeleteTextForWD= false;
				/*
				 * IF DELETE TEXT SOURCE PATH 
				 * CONTAINS WD DIRECTORY NAME or e-WD Directory Name - then WIRING DIAGRAM
				 * ELSE NORMAL SCENARIO
				 * 
				 * FOR WD - DO NOT CHECK FOR EXTENSIONS FOR IDENTIFYING DOCUMENT.
				 */
				String wiringDigramDirectoryName="\\"+ApplicationProperties.getProperty("wiring.diagram.folder")+"\\";
				String ewiringDigramDirectoryName="\\"+ApplicationProperties.getProperty("electronic.wiring.diagram.folder")+"\\";
				if(PathUtil.winPath(deleteTextFile).trim().toLowerCase().contains(wiringDigramDirectoryName) || 
						PathUtil.winPath(deleteTextFile).trim().toLowerCase().contains(ewiringDigramDirectoryName))
				{
					// PROCESSING WDs
					isDeleteTextForWD=true;
				}
				wiringDigramDirectoryName = null;
				ewiringDigramDirectoryName= null;
				
			    br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(deleteTextFile.getAbsoluteFile()), "UTF-8"));
			    
			    String line;
			    while ((line = br.readLine()) != null) {
			    	if(null!=line && !"".equals(line))
			    	{
			    		line = line.trim();
			    		
			    		// REMOVE SIE= FROM LINE
			    		if(line.contains("SIE="))
			    		{
			    			line = line.replace("SIE=", "");
			    		}
			    		
			    		String fileToBeDeleted=null;
			    		String fileToBeDeletedPath=null;
			    		String okAssetsFileToBeDeleted=null;
			    		
			    		if(isDeleteTextForWD==true)
			    		{
			    			/*
			    			 * PROCESS WIRING DIAGRAM DATA
			    			 * Here, the PATHS WILL NOT HAVE ANY EXTENSIONS, 
			    			 * STILL CHECK FOR GIF, TIF, SWF FILES IF FOUND - THEN OKASSETS
			    			 * ELSE CONSIDER THEM AS DOCUMENTS
			    			 */
			    			if(line.toLowerCase().endsWith(".gif") || line.toLowerCase().endsWith(".tif") || line.toLowerCase().endsWith(".swf"))
			    			{
				    			/*
				    			 * OK ASSETS FILE TO BE DELETED
				    			 */
				    			okAssetsFileToBeDeleted = line.trim();
				    			if(null!=okAssetsFileToBeDeleted && !"".equals(okAssetsFileToBeDeleted))
				    			{
				    				DeleteFileDetails details = new DeleteFileDetails();
						    		details.setFileName(deleteTextFile.getName());
						    		details.setFilePath(PathUtil.winPath(deleteTextFile));
					    			details.setOkAssetsFileToBeDeletedName(okAssetsFileToBeDeleted);
					    			details.setFileType(DeleteFileDetails.TYPE_OKASSETS);
					    			/*
					    			 * add to de
					    			 */
					    			if(null==deleteDetailsList || deleteDetailsList.size()<=0)
					    			{
					    				deleteDetailsList = new ArrayList<DeleteFileDetails>();
					    			}
					    			deleteDetailsList.add(details);
					    			details = null;
				    			}
			    			}
			    			else if(line.trim().toLowerCase().contains(ApplicationProperties.getProperty("directory.html5")))
			    			{
			    				/*
			    				 * HTML5 WD, THE LINE NAME WILL BE IN FORMAT
			    				 * html5\fileName
			    				 */
			    				fileToBeDeleted = line.trim();
			    				/*
			    				 * INSTEAD REPLACE ALL / BY \\ IN THE FILE NAME
				    			 */
				    			fileToBeDeleted = fileToBeDeleted.replace("/", "\\");
				    			/*
				    			 * IDENITFY FILE NAME WHICH WILL BE AFTER \\
				    			 */
				    			if(fileToBeDeleted.lastIndexOf("\\")!=-1)
				    			{
				    				fileToBeDeleted= fileToBeDeleted.substring(fileToBeDeleted.lastIndexOf("\\")+1, fileToBeDeleted.length());
				    			}
				    			/*
				    			 * NOW PREPARE THE PATH FOR THE 
				    			 * WHICH WILL BE - PARent FOLDER PATH  
				    			 * DO NOT ADD ANY PROCESSING FOLDER EXPLICITYLY AS IT WILL BE ALREADY THERE IN THE LINE
				    			 */
				    			String parentPath = PathUtil.winPath(deleteTextFile.getParentFile());
				    			parentPath = parentPath.replace("/", "\\");
				    			if(!parentPath.endsWith("\\"))
				    			{
				    				parentPath = parentPath+"\\";
				    			}
				    			String dummyFile = line.trim();
				    			dummyFile = dummyFile.replace("/", "\\");
				    			if(dummyFile.trim().startsWith("\\"))
				    			{
				    				parentPath+=dummyFile.substring(1, dummyFile.length());
				    			}
				    			else
				    			{
				    				parentPath = parentPath+dummyFile;
				    			}
				    			dummyFile = null;
			    				fileToBeDeletedPath = parentPath;
			    				
			    				if(null!=fileToBeDeleted && !"".equals(fileToBeDeleted) && null!=fileToBeDeletedPath && !"".equals(fileToBeDeletedPath))
					    		{
			    					DeleteFileDetails details = new DeleteFileDetails();
						    		details.setFileName(deleteTextFile.getName());
						    		details.setFilePath(PathUtil.winPath(deleteTextFile));
					    			details.setFileToBeDeletedName(fileToBeDeleted);
					    			details.setFileToBeDeletedPath(fileToBeDeletedPath);
					    			details.setFileType(DeleteFileDetails.TYPE_DOCUMENT);
					    			/*
					    			 * add to deleteDetailsList
					    			 */
					    			if(null==deleteDetailsList || deleteDetailsList.size()<=0)
					    			{
					    				deleteDetailsList = new ArrayList<DeleteFileDetails>();
					    			}
					    			deleteDetailsList.add(details);
					    			details = null;
					    		}
			    				
			    				parentPath = null;
			    			}
			    			else
			    			{
			    				/*
				    			 * FILE TO BE DELETED
				    			 */
				    			fileToBeDeleted = line.trim();
				    			
				    			/*
				    			 * FOR WIRING DIAGRAMS - DO NOT CHECK FOR / IN THE FILE NAME
				    			 * BECAUSE SOME FILES FOR CONN FOLDER MAY ALSO COME, E.G. conn/1a_0b
				    			 * 
				    			 * INSTEAD REPLACE ALL / BY \\ IN THE FILE NAME
				    			 */
				    			fileToBeDeleted = fileToBeDeleted.replace("/", "\\");
				    			/*
				    			 * NOW PREPARE THE PATH FOR THE 
				    			 * WHICH WILL BE - PARent FOLDER PATH + ent.dir / html / pdf
				    			 */
				    			String parentPath = PathUtil.winPath(deleteTextFile.getParentFile());
				    			parentPath = parentPath.replace("/", "\\");
				    			if(!parentPath.endsWith("\\"))
				    			{
				    				parentPath = parentPath+"\\";
				    			}
				    			
				    			/*
				    			 * FOR HOLDER NAME USE HTML DIRECTORY BY DEFAULT
				    			 * AS IT COULD BE EITHER HTML OR PDF BOTH
				    			 */
				    			String folderName="";
				    			folderName = ApplicationProperties.getProperty("directory.html");
				    			
				    			if(null!=folderName && !"".equals(folderName))
				    			{
				    				parentPath = parentPath+folderName+"\\";
				    				// add fileName which is line
				    				// replace / by \\
				    				line = line.replace("/", "\\");
				    				parentPath = parentPath+line;
				    				fileToBeDeletedPath = parentPath;
				    			}
				    			folderName = null;
				    			parentPath= null;
				    			
				    			if(null!=fileToBeDeleted && !"".equals(fileToBeDeleted) && null!=fileToBeDeletedPath && !"".equals(fileToBeDeletedPath))
					    		{
					    			DeleteFileDetails details = new DeleteFileDetails();
						    		details.setFileName(deleteTextFile.getName());
						    		details.setFilePath(PathUtil.winPath(deleteTextFile));
					    			details.setFileToBeDeletedName(fileToBeDeleted);
					    			details.setFileToBeDeletedPath(fileToBeDeletedPath);
					    			details.setFileType(DeleteFileDetails.TYPE_DOCUMENT);
					    			/*
					    			 * add to deleteDetailsList
					    			 */
					    			if(null==deleteDetailsList || deleteDetailsList.size()<=0)
					    			{
					    				deleteDetailsList = new ArrayList<DeleteFileDetails>();
					    			}
					    			deleteDetailsList.add(details);
					    			details = null;
					    		}
			    			}
			    		}
			    		else
			    		{
			    			/*
			    			 * PROCESS NORMAL SCENARIO DATA
			    			 */
			    			if(line.toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf"))
				    				|| line.toLowerCase().endsWith(ApplicationProperties.getProperty("extension.html")) 
				    				|| line.toLowerCase().endsWith(ApplicationProperties.getProperty("extension.htm"))
				    				|| line.toLowerCase().endsWith(ApplicationProperties.getProperty("extension.ent"))
				    				|| line.toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf")))
				    		{
				    			/*
				    			 * FILE TO BE DELETED
				    			 */
				    			fileToBeDeleted = line.trim();
				    			/*
				    			 * FOR FILE NAME CHECK IF IT CONTAINS / THEN ADD THE NAME AFTER IT
				    			 * ELSE SET AS IT IS
				    			 */
				    			if(null!=fileToBeDeleted && !"".equals(fileToBeDeleted) )
				    			{
				    				if(fileToBeDeleted.lastIndexOf("/")!=-1)
				    				{
				    					fileToBeDeleted = fileToBeDeleted.substring(fileToBeDeleted.lastIndexOf("/")+1, fileToBeDeleted.length());
				    				}
				    			}
				    			/*
				    			 * NOW PREPARE THE PATH FOR THE 
				    			 * WHICH WILL BE - PARent FOLDER PATH + ent.dir / html / pdf
				    			 */
				    			String parentPath = PathUtil.winPath(deleteTextFile.getParentFile());
				    			parentPath = parentPath.replace("/", "\\");
				    			if(!parentPath.endsWith("\\"))
				    			{
				    				parentPath = parentPath+"\\";
				    			}
				    			String folderName="";
				    			if(line.toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf")))
				    			{
				    				folderName = ApplicationProperties.getProperty("directory.pdf");
				    			}
				    			else if(line.toLowerCase().endsWith(ApplicationProperties.getProperty("extension.htm")) || 
				    					line.toLowerCase().endsWith(ApplicationProperties.getProperty("extension.html")))
				    			{
				    				folderName = ApplicationProperties.getProperty("directory.html");
				    			}
				    			else if(line.toLowerCase().endsWith(ApplicationProperties.getProperty("extension.ent")))
				    			{
				    				folderName= ApplicationProperties.getProperty("directory.ent.dir");
				    			}
				    			
				    			if(null!=folderName && !"".equals(folderName))
				    			{
				    				parentPath = parentPath+folderName+"\\";
				    				// add fileName which is line
				    				// replace / by \\
				    				line = line.replace("/", "\\");
				    				parentPath = parentPath+line;
				    				fileToBeDeletedPath = parentPath;
				    			}
				    			folderName = null;
				    			parentPath= null;
				    			
				    			if(null!=fileToBeDeleted && !"".equals(fileToBeDeleted) && null!=fileToBeDeletedPath && !"".equals(fileToBeDeletedPath))
					    		{
					    			DeleteFileDetails details = new DeleteFileDetails();
						    		details.setFileName(deleteTextFile.getName());
						    		details.setFilePath(PathUtil.winPath(deleteTextFile));
					    			details.setFileToBeDeletedName(fileToBeDeleted);
					    			details.setFileToBeDeletedPath(fileToBeDeletedPath);
					    			details.setFileType(DeleteFileDetails.TYPE_DOCUMENT);
					    			/*
					    			 * add to delete list
					    			 */
					    			if(null==deleteDetailsList || deleteDetailsList.size()<=0)
					    			{
					    				deleteDetailsList = new ArrayList<DeleteFileDetails>();
					    			}
					    			deleteDetailsList.add(details);
					    			details = null;
					    		}
				    		}
				    		else
				    		{
				    			/*
				    			 * OK ASSETS FILE TO BE DELETED
				    			 */
				    			okAssetsFileToBeDeleted = line.trim();
				    			if(null!=okAssetsFileToBeDeleted && !"".equals(okAssetsFileToBeDeleted))
				    			{
				    				DeleteFileDetails details = new DeleteFileDetails();
						    		details.setFileName(deleteTextFile.getName());
						    		details.setFilePath(PathUtil.winPath(deleteTextFile));
					    			details.setOkAssetsFileToBeDeletedName(okAssetsFileToBeDeleted);
					    			details.setFileType(DeleteFileDetails.TYPE_OKASSETS);
					    			/*
					    			 * add to de
					    			 */
					    			if(null==deleteDetailsList || deleteDetailsList.size()<=0)
					    			{
					    				deleteDetailsList = new ArrayList<DeleteFileDetails>();
					    			}
					    			deleteDetailsList.add(details);
					    			details = null;
				    			}
				    		}	
			    		}
			    		
			    		fileToBeDeleted=null;
			    		fileToBeDeletedPath=null;
			    		okAssetsFileToBeDeleted=null;
			    	}
			    }
			    line = null;
			}
			else
			{
				logger.info("readDeleteTextFile :: Delete Text file is either Corrupted or Does not Exist.");
				deleteDetailsList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readDeleteTextFile()", e);
			deleteDetailsList = null;
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return deleteDetailsList;
	}
	
	
	/**
	 * Function will read the Parent and CHild VIN / VIN ATTRIBUTE ENT Files
	 * and Prepare the UNique VIN Data
	 * @param parentVinEntDetailsList
	 * @param childVinEntDetailsList
	 * @param parentVinAttributeList
	 * @param childVinAttributeList
	 * @param contentDetails
	 */
	public static ContentDetails prepareFinalVinEndAndAttributesData(ArrayList<VINEntFileDetails> parentVinEntDetailsList,
			ArrayList<VINEntFileDetails> childVinEntDetailsList, ArrayList<VinAttributeEntFileDetails> parentVinAttributeList,
			ArrayList<VinAttributeEntFileDetails> childVinAttributeList, ContentDetails contentDetails)
	{
		try
		{
			ArrayList<VINEntFileDetails> tempFinalVinList = new ArrayList<VINEntFileDetails>();
			ArrayList<VinAttributeEntFileDetails> tempfinalVinAttributeList = new ArrayList<VinAttributeEntFileDetails>();
			if(null!=parentVinEntDetailsList && parentVinEntDetailsList.size()>0)
			{
				logger.info("prepareFinalVinEndAndAttributesData :: parentVinEntDetailsList :: >"+ parentVinEntDetailsList.size());
				tempFinalVinList.addAll(parentVinEntDetailsList);
			}
			if(null!=childVinEntDetailsList && childVinEntDetailsList.size()>0)
			{
				logger.info("prepareFinalVinEndAndAttributesData :: childVinEntDetailsList :: >"+ childVinEntDetailsList.size());
				tempFinalVinList.addAll(childVinEntDetailsList);
			}
			
			if(null!=parentVinAttributeList && parentVinAttributeList.size()>0)
			{
				tempfinalVinAttributeList.addAll(parentVinAttributeList);
			}
			if(null!=childVinAttributeList && childVinAttributeList.size()>0)
			{
				tempfinalVinAttributeList.addAll(childVinAttributeList);
			}
			
			
			ArrayList<VINEntFileDetails> finalVinList = new ArrayList<VINEntFileDetails>();
			ArrayList<VinAttributeEntFileDetails> finalVinAttributeList = new ArrayList<VinAttributeEntFileDetails>();
			if(null!=tempFinalVinList && tempFinalVinList.size()>0)
			{
				VINEntFileDetails details  = null;
				boolean addToList = true;
				VINEntFileDetails existingDetails = null;
				for(int a=0;a<tempFinalVinList.size();a++)
				{
					details = (VINEntFileDetails)tempFinalVinList.get(a);
					addToList = true;
					if(null!=finalVinList && finalVinList.size()>0)
					{
						existingDetails = null;
						for(int r=0;r<finalVinList.size();r++)
						{
							existingDetails = (VINEntFileDetails)finalVinList.get(r);
							if(null!=existingDetails.getModelNameVINEnt() && !"".equals(existingDetails.getModelNameVINEnt()) && 
									null!=details.getModelNameVINEnt() && !"".equals(details.getModelNameVINEnt()) && 
									null!=existingDetails.getModelYear() && !"".equals(existingDetails.getModelYear())
								&& null!=details.getModelYear() && !"".equals(details.getModelYear())
								&& null!=existingDetails.getVinWMI() && !"".equals(existingDetails.getVinWMI())
								&& null!=details.getVinWMI() && !"".equals(details.getVinWMI())
								&& null!=existingDetails.getVinCarline() && !"".equals(existingDetails.getVinCarline())
								&& null!=details.getVinCarline() && !"".equals(details.getVinCarline())
								&& null!=existingDetails.getVinVDS() && !"".equals(existingDetails.getVinVDS())
								&& null!=details.getVinVDS() && !"".equals(details.getVinVDS())
								&& null!=existingDetails.getVinStartRange() && !"".equals(existingDetails.getVinStartRange())
								&& null!=details.getVinStartRange() && !"".equals(details.getVinStartRange())
								&& null!=existingDetails.getVinEndRange() && !"".equals(existingDetails.getVinEndRange())
								&& null!=details.getVinEndRange() && !"".equals(details.getVinEndRange()))
							{
								if(existingDetails.getModelNameVINEnt().equals(details.getModelNameVINEnt()) && 
										existingDetails.getModelYear().equals(details.getModelYear()) &&
										existingDetails.getVinWMI().equals(details.getVinWMI()) &&
										existingDetails.getVinCarline().equals(details.getVinCarline()) &&
										existingDetails.getVinVDS().equals(details.getVinVDS()) &&
										existingDetails.getVinStartRange().equals(details.getVinStartRange()) &&
										existingDetails.getVinEndRange().equals(details.getVinEndRange()))
								{
									// same data
									addToList = false;
									break;
								}
							}
							existingDetails = null;
						}
					}
					
					if(addToList==true)
					{
						finalVinList.add(details);
					}
					details = null;
				}
				details = null;
				existingDetails=null;
			}
			
			
			if(null!=tempfinalVinAttributeList && tempfinalVinAttributeList.size()>0)
			{
				VinAttributeEntFileDetails details = null;
				boolean addToList = true;
				VinAttributeEntFileDetails existingDetails = null;
				for(int a=0;a<tempfinalVinAttributeList.size();a++)
				{
					details = (VinAttributeEntFileDetails)tempfinalVinAttributeList.get(a);
					addToList = true;
					if(null!=finalVinAttributeList && finalVinAttributeList.size()>0)
					{
						existingDetails= null;
						for(int r=0;r<finalVinAttributeList.size();r++)
						{
							existingDetails = (VinAttributeEntFileDetails)finalVinAttributeList.get(r);
							if(null!=existingDetails.getVinAxleType() && !"".equals(existingDetails.getVinAxleType())
								&& null!=details.getVinAxleType() && !"".equals(details.getVinAxleType())
								&& null!=existingDetails.getVinAxleTypeValue() && !"".equals(existingDetails.getVinAxleTypeValue())
								&& null!=details.getVinAxleTypeValue() && !"".equals(details.getVinAxleTypeValue())
								&& null!=existingDetails.getVinBodyType() && !"".equals(existingDetails.getVinBodyType())
								&& null!=details.getVinBodyType() && !"".equals(details.getVinBodyType())
								&& null!=existingDetails.getVinBodyTypeValue() && !"".equals(existingDetails.getVinBodyTypeValue())
								&& null!=details.getVinBodyTypeValue() && !"".equals(details.getVinBodyTypeValue())
								&& null!=existingDetails.getVinEngineType() && !"".equals(existingDetails.getVinEngineType())
								&& null!=details.getVinEngineType() && !"".equals(details.getVinEngineType())
								&& null!=existingDetails.getVinTransType() && !"".equals(existingDetails.getVinTransType())
								&& null!=details.getVinTransType() && !"".equals(details.getVinTransType()))
							{
								if(existingDetails.getVinAxleType().equals(details.getVinAxleType()) &&
										existingDetails.getVinAxleTypeValue().equals(details.getVinAxleTypeValue()) &&
										existingDetails.getVinBodyType().equals(details.getVinBodyType()) &&
										existingDetails.getVinBodyTypeValue().equals(details.getVinBodyTypeValue()) &&
										existingDetails.getVinEngineType().equals(details.getVinEngineType()) &&
										existingDetails.getVinTransType().equals(details.getVinTransType()))
								{
									// same data
									addToList = false;
									break;
								}
							}
							existingDetails =null;
						}
					}
					
					if(addToList==true)
					{
						finalVinAttributeList.add(details);
					}
					details = null;
				}
				details = null;
				existingDetails = null;
			}
			
			if(null!=finalVinAttributeList && finalVinAttributeList.size()>0)
			{
				contentDetails.setVinAttributeEntDetailsList(finalVinAttributeList);
			}
			if(null!=finalVinList && finalVinList.size()>0)
			{
				contentDetails.setVinEntDetailsList(finalVinList);
			}
			finalVinAttributeList = null;
			finalVinList = null;
			tempfinalVinAttributeList = null;
			tempFinalVinList = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "prepareFinalVinEndAndAttributesData()", e);
		}
		return contentDetails;
	}
	
	/**
	 * Function will identify the Title / Category / Sub Category for the Document from the Left menu List
	 * @param fileName
	 * @param leftMenuList
	 * @return
	 */
	public static ContentDetails identifyDocumentAttributesFromLeftMenu(ContentDetails contentDetails, ArrayList<LeftMenuFileDetails> leftMenuList, String processingFolderName)
	{
		try
		{
			if(null!=contentDetails && null!=contentDetails.getFileName() && !"".equals(contentDetails.getFileName()) 
					&& null!=leftMenuList && leftMenuList.size()>0)
			{
				String fileName=contentDetails.getFileName();
				fileName =  fileName.trim();
				// do not add prefix as SIE for matching it with fileName
//				fileName =  "SIE="+fileName.trim();
				
				/*
				 * CHECK HERE, IF CHANNEL IS WIRING DIAGRMS, THEN REMOVE THE EXTENSION FROM THE FILE NAME,
				 * SINCE IN LEFT MENU FILE FOR WIRING DIAGRAMS, SIE ID WILL NOT HAVE EXTENSIONS
				 */
				if(null!=contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName()))
				{
					if(contentDetails.getChannelName().equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME")))
					{
						String tok="";
						String mtName = contentDetails.getMaterialName();
						if(mtName.lastIndexOf("_")!=-1)
						{
							tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
						}
						if(null==tok)
						{
							tok = "";
						}
						if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
						{
							/*
							 * BEFORE REMOVING CHECK IF PROCESSING FOLDER IS HTML5
							 * THEN DO NOT REMOVE EXTENSION
							 */
							boolean removeExt = true;
							if(null!=processingFolderName && !"".equals(processingFolderName) && 
									processingFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html5").trim().toLowerCase()))
							{
								removeExt = false;
							}
							
							if(removeExt==true)
							{
								// REMOVE EXTENSION FROM FILE NAME
								if(fileName.lastIndexOf(".")!=-1)
								{
									fileName= fileName.substring(0, fileName.lastIndexOf("."));
								}
							}
						}
						tok = null;
					}
				}
				fileName =  fileName.trim();
				
				for(int a=0;a<leftMenuList.size();a++)
				{
					LeftMenuFileDetails lfDetails = (LeftMenuFileDetails)leftMenuList.get(a);
					if(null!=lfDetails.getFileName() && !"".equals(lfDetails.getFileName()))
					{
						String leftMenuFileName = lfDetails.getFileName();
						leftMenuFileName = leftMenuFileName.replace("SIE=", "");
						leftMenuFileName = leftMenuFileName.replace("sie=", "");
						/*
						 * NOW CHECK, IF LEFT MENU FILE NAME STARTS WITH CONN/ SKIP IT
						 * ELSE IF STARTS WITH HTML/, GET THE VALUE AFTER /
						 * ELSE USE AS IT IS
						 */
						if(!leftMenuFileName.trim().startsWith("conn/"))
						{
							String finalFileName="";
							if(leftMenuFileName.startsWith("html/"))
							{
								finalFileName= leftMenuFileName.replace("html/", "");
							}
							else
							{
								finalFileName= leftMenuFileName;
							}
							
							if(fileName.trim().toLowerCase().equals(finalFileName.trim().toLowerCase()))
							{
								// file Found - set the Title
								if(null!=lfDetails.getTitle() && !"".equals(lfDetails.getTitle()))
								{
									logger.info("identifyDocumentAttributesFromLeftMenu :: Title for File {"+fileName+"} is :: > " + lfDetails.getTitle());
									contentDetails.setTitle(lfDetails.getTitle());
								}
								// set the Category
								if(null!=lfDetails.getCategoryCode() && !"".equals(lfDetails.getCategoryCode()))
								{
									logger.info("identifyDocumentAttributesFromLeftMenu :: Category for File {"+fileName+"} is :: > " + lfDetails.getCategoryCode());
									contentDetails.setCategoryCode(lfDetails.getCategoryCode());
								}
								// set the Sub Category
								if(null!=lfDetails.getSubCategoryCode() && !"".equals(lfDetails.getSubCategoryCode()))
								{
									logger.info("identifyDocumentAttributesFromLeftMenu :: Sub Category for File {"+fileName+"} is :: > " + lfDetails.getSubCategoryCode());
									contentDetails.setSubCategoryCode(lfDetails.getSubCategoryCode());
								}
								// set the Sub Sub Category
								if(null!=lfDetails.getSubSubCategoryCode() && !"".equals(lfDetails.getSubSubCategoryCode()))
								{
									logger.info("identifyDocumentAttributesFromLeftMenu :: Sub Sub Category for File {"+fileName+"} is :: > " + lfDetails.getSubSubCategoryCode());
									contentDetails.setSubSubCategoryCode(lfDetails.getSubSubCategoryCode());
								}
								
								// ALSO SET ENTRY FOUND IN LEFT MENU TO YES
								contentDetails.setEntryFoundInLeftMenu("YES");
								
								break;
							}
							finalFileName= null;
						}
						
						leftMenuFileName = null;
					}
					lfDetails = null;
				}
				fileName = null;
			}
			else
			{
				logger.info("identifyDocumentAttributesFromLeftMenu :: File Name and Left Menu List as parameters are null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "identifyDocumentAttributesFromLeftMenu()", e);
		}
		return contentDetails;
	}

	/**
	 * Function will identify the Title / Category / Sub Category for the Document from the Left menu List FOR CONN PDFS / HTMLS
	 * @param contentDetails
	 * @param leftMenuList
	 * @return
	 */
	public static ContentDetails identifyDocumentAttributesFromLeftMenuForConn(ContentDetails contentDetails, ArrayList<LeftMenuFileDetails> leftMenuList)
	{
		try
		{
			if(null!=contentDetails && null!=contentDetails.getFileName() && !"".equals(contentDetails.getFileName()) 
					&& null!=leftMenuList && leftMenuList.size()>0)
			{
				String fileName=contentDetails.getFileName();
				fileName =  fileName.trim();
				// do not add prefix as SIE for matching it with fileName
//				fileName =  "SIE="+fileName.trim();
				
				/*
				 * CHECK HERE, IF CHANNEL IS WIRING DIAGRMS, THEN REMOVE THE EXTENSION FROM THE FILE NAME,
				 * SINCE IN LEFT MENU FILE FOR WIRING DIAGRAMS, SIE ID WILL NOT HAVE EXTENSIONS
				 */
				if(null!=contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName()))
				{
					if(contentDetails.getChannelName().equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME")))
					{
						String tok="";
						String mtName = contentDetails.getMaterialName();
						if(mtName.lastIndexOf("_")!=-1)
						{
							tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
						}
						if(null==tok)
						{
							tok = "";
						}
						if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
								!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
						{
							// NO NEED TO CHECK HERE FOR HTML5 AS - CONN PROCESSING IS ONLY APPLICABLE FOR HTML + PDF (NORMAL WD)
							// REMOVE EXTENSION FROM FILE NAME
							if(fileName.lastIndexOf(".")!=-1)
							{
								fileName= fileName.substring(0, fileName.lastIndexOf("."));
							}

						}
						tok = null;
					}
				}
				
				// add conn to the fileName
				fileName =  fileName.trim();
				fileName = ApplicationProperties.getProperty("directory.conn")+"/"+fileName;
				
				for(int a=0;a<leftMenuList.size();a++)
				{
					LeftMenuFileDetails lfDetails = (LeftMenuFileDetails)leftMenuList.get(a);
					if(null!=lfDetails.getFileName() && !"".equals(lfDetails.getFileName()))
					{
						String leftMenuFileName = lfDetails.getFileName();
						leftMenuFileName = leftMenuFileName.replace("SIE=", "");
						leftMenuFileName = leftMenuFileName.replace("sie=", "");
						/*
						 * CHECK FOR THE FILES STARTS WITH CONN
						 */
						if(leftMenuFileName.trim().startsWith("conn/"))
						{
							String finalFileName="";
							if(leftMenuFileName.startsWith("html/"))
							{
								finalFileName= leftMenuFileName.replace("html/", "");
							}
							else
							{
								finalFileName= leftMenuFileName;
							}
							
							if(fileName.equals(finalFileName.trim()))
							{
								// file Found - set the Title
								if(null!=lfDetails.getTitle() && !"".equals(lfDetails.getTitle()))
								{
									logger.info("identifyDocumentAttributesFromLeftMenuForConn :: Title for File {"+fileName+"} is :: > " + lfDetails.getTitle());
									contentDetails.setTitle(lfDetails.getTitle());
								}
								// set the Category
								if(null!=lfDetails.getCategoryCode() && !"".equals(lfDetails.getCategoryCode()))
								{
									logger.info("identifyDocumentAttributesFromLeftMenuForConn :: Category for File {"+fileName+"} is :: > " + lfDetails.getCategoryCode());
									contentDetails.setCategoryCode(lfDetails.getCategoryCode());
								}
								// set the Sub Category
								if(null!=lfDetails.getSubCategoryCode() && !"".equals(lfDetails.getSubCategoryCode()))
								{
									logger.info("identifyDocumentAttributesFromLeftMenuForConn :: Sub Category for File {"+fileName+"} is :: > " + lfDetails.getSubCategoryCode());
									contentDetails.setSubCategoryCode(lfDetails.getSubCategoryCode());
								}
								// set the Sub Sub Category
								if(null!=lfDetails.getSubSubCategoryCode() && !"".equals(lfDetails.getSubSubCategoryCode()))
								{
									logger.info("identifyDocumentAttributesFromLeftMenuForConn :: Sub Sub Category for File {"+fileName+"} is :: > " + lfDetails.getSubSubCategoryCode());
									contentDetails.setSubSubCategoryCode(lfDetails.getSubSubCategoryCode());
								}
								
								// ALSO SET ENTRY FOUND IN LEFT MENU TO YES
								contentDetails.setEntryFoundInLeftMenu("YES");
								
								
								break;
							}
							finalFileName= null;
						}
						
						leftMenuFileName = null;
					}
					lfDetails = null;
				}
				fileName = null;
			}
			else
			{
				logger.info("identifyDocumentAttributesFromLeftMenuForConn :: File Name and Left Menu List as parameters are null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "identifyDocumentAttributesFromLeftMenuForConn()", e);
		}
		return contentDetails;
	}

	/**
	 * Function will read the Data from the File
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public static String getStringFromXML(File f) throws IOException 
	{
		StringBuffer xmlData = new StringBuffer();
		try
		{
			BufferedReader input = new BufferedReader(PathUtil.fileReader(f));
			try 
			{
				String line = null;
				while ((line = input.readLine()) != null) 
				{
					xmlData.append(line);
					xmlData.append(System.getProperty("line.separator"));
				}
			}
			finally 
			{
				input.close();
			}
		} 
		catch (IOException ex) 
		{
			ex.printStackTrace();
			logger.error("getStringFromXML :: Cannot Read the Input FileL :: "	+ f.getName());
			throw ex;
		}
		String articleXML = xmlData.toString();
		return articleXML;
	}
	
	/**
	 * Function will get the HTML Content from File
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public static String getStringFromHTML(File f) throws IOException {
		String htmlContent = "";
		try {
			org.jsoup.nodes.Document doc = Jsoup.parse(f, "UTF-8");
			if (null != doc) {
				htmlContent = doc.toString();
			}
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("getStringFromHTML :: Cannot Read the Input File :: "
					+ f.getName());
			throw e;
		}
		return htmlContent;
	}
	

	/**
	 * The Function will be used to replace all the special characters in the
	 * Name when checking for Category Name
	 * 
	 * @param key
	 * @return
	 */
	public static String replaceSpecialCharactersInName(String key) {
		try {
			key = key.replace("&egrave;", "�");
//			key = key.replace("&Eacute;", "�");
			key = key.replace("&eacute;", "�");
			key = key.replace("&ecirc;", "�");
			key = key.replace("&rsquo;", "'");
			key = key.replace("&icirc;", "�");
			key = key.replace("&ocirc;", "�");
			key = key.replace("&oacute;", "�");
			key = key.replace("&aacute;", "�");
			key = key.replace("&uacute;", "�");
			key = key.replace("&iacute;", "�");

			if (key.equals("Syst�mes d��clairage")) {
				key = "Syst�mes d'�clairage";
			}
			if (key.equals("Syst�me d��missions")) {
				key = "Syst�me d'�missions";
			}
			if (key.equals("Syst�me d��chappement")) {
				key = "Syst�me d'�chappement";
			}
			if (key.equals("Syst�me d��chappement�")) {
				key = "Syst�me d'�chappement";
			}
			if (key.equals("Air d�En-Prendre Syst�me")) {
				key = "Air d'En-Prendre Syst�me";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return key;
	}

	public static ContentDetails prepareNavigationLinkDataForPDFs(ContentDetails contentDetails)
	{
		try
		{
		String navigationData="";
		contentDetails.setNavigation("");
			if(null!=contentDetails.getPreviousLink() && !"".equals(contentDetails.getPreviousLink()))
			{
				String previousLabel="";
				if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("en-us")))
				{
					previousLabel = ApplicationProperties.getProperty("wd.previous.constant.eng");
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("es-mx")))
				{
					previousLabel = ApplicationProperties.getProperty("wd.previous.constant.esx");
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("fr-ca")))
				{
					previousLabel = ApplicationProperties.getProperty("wd.previous.constant.frc");
				}
				
				if(null==previousLabel || "".equals(previousLabel))
				{
					// set default eng
					previousLabel = ApplicationProperties.getProperty("wd.previous.constant.eng");
				}
				
				// add previousLink to navigationData
				navigationData = "<a href=\""+contentDetails.getPreviousLink()+"\">&lt; "+previousLabel+" </a> ";
				previousLabel = null;
			}
			
			if(null!=contentDetails.getNextLink() && !"".equals(contentDetails.getNextLink()))
			{
				// add previousLink to navigationData
				if(null!=navigationData && !"".equals(navigationData))
				{
					navigationData = navigationData+"&nbsp;&nbsp;";
				}
				
				String nextLabel="";
				if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("en-us")))
				{
					nextLabel = ApplicationProperties.getProperty("wd.next.constant.eng");
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("es-mx")))
				{
					nextLabel = ApplicationProperties.getProperty("wd.next.constant.esx");
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("fr-ca")))
				{
					nextLabel = ApplicationProperties.getProperty("wd.next.constant.frc");
				}
				if(null==nextLabel || "".equals(nextLabel))
				{
					// set default eng
					nextLabel = ApplicationProperties.getProperty("wd.next.constant.eng");
				}
				
				navigationData =navigationData + "<a href=\""+contentDetails.getNextLink()+"\">"+nextLabel+" &gt; </a> ";
				nextLabel = null;
				
			}
			
			if(null!=navigationData && !"".equals(navigationData))
			{
				contentDetails.setNavigation(navigationData);
			}
			navigationData = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "prepareNavigationLinkDataForPDF()", e);
		}
		return contentDetails;
	}

	

	
	public static ContentDetails prepareNavigationLinkDataForRePurpose(ContentDetails contentDetails)
	{
		try
		{
		String navigationData="";
		contentDetails.setNavigation("");
			if(null!=contentDetails.getPreviousLink() && !"".equals(contentDetails.getPreviousLink()))
			{
				String linkUrl = ApplicationProperties.getProperty("LINK_URL");
				/*
				 * call function to get Document Id for the Navigation Link
				 */
				if(null!=contentDetails.getPreviousLinkDocId() && !"".equals(contentDetails.getPreviousLinkDocId()))
				{
					linkUrl =linkUrl+ contentDetails.getPreviousLinkDocId();
				}
				
				String previousLabel="";
				if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("en-us")))
				{
					previousLabel = ApplicationProperties.getProperty("wd.previous.constant.eng");
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("es-mx")))
				{
					previousLabel = ApplicationProperties.getProperty("wd.previous.constant.esx");
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("fr-ca")))
				{
					previousLabel = ApplicationProperties.getProperty("wd.previous.constant.frc");
				}
				if(null==previousLabel || "".equals(previousLabel))
				{
					// set default eng
					previousLabel = ApplicationProperties.getProperty("wd.previous.constant.eng");
				}
				
				// add previousLink to navigationData
				navigationData = "<a href=\""+linkUrl+"\">&lt; "+previousLabel+" </a> ";
				previousLabel = null;
				
				linkUrl= null;
			}
			
			if(null!=contentDetails.getNextLink() && !"".equals(contentDetails.getNextLink()))
			{
				String linkUrl = ApplicationProperties.getProperty("LINK_URL");
				/*
				 * call function to get Document Id for the Navigation Link
				 */
				if(null!=contentDetails.getNextLinkDocId() && !"".equals(contentDetails.getNextLinkDocId()))
				{
					linkUrl =linkUrl+ contentDetails.getNextLinkDocId();
				}
				
				// add previousLink to navigationData
				if(null!=navigationData && !"".equals(navigationData))
				{
					navigationData = navigationData+"&nbsp;";
				}
				
				String nextLabel="";
				if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("en-us")))
				{
					nextLabel = ApplicationProperties.getProperty("wd.next.constant.eng");
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("es-mx")))
				{
					nextLabel = ApplicationProperties.getProperty("wd.next.constant.esx");
				}
				else if(ApplicationProperties.getProperty(contentDetails.getLocale().trim().toLowerCase()).equals(ApplicationProperties.getProperty("fr-ca")))
				{
					nextLabel = ApplicationProperties.getProperty("wd.next.constant.frc");
				}
				if(null==nextLabel || "".equals(nextLabel))
				{
					// set default eng
					nextLabel = ApplicationProperties.getProperty("wd.next.constant.eng");
				}
				
				navigationData =navigationData + "<a href=\""+linkUrl+"\">"+nextLabel+" &gt; </a> ";
				nextLabel = null;
				
				linkUrl= null;
			}
			
			if(null!=navigationData && !"".equals(navigationData))
			{
				contentDetails.setNavigation(navigationData);
			}
			navigationData = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "prepareNavigationLinkData()", e);
		}
		return contentDetails;
	}

	

	/**
	 * Function will arrange the Child Model Folders in the Sequence Order of Execution.
	 * Ascending Order - Year Wise
	 * then Ascending Order - Alphabet Wise
	 * @param childModelDirs
	 * @return
	 */

	
	public static List<ChildModelFolderDetails> arrangeSubModeFoldersinSequence(File[] childModelDirs)
	{
		List<ChildModelFolderDetails> childModelFoldersList = new ArrayList<ChildModelFolderDetails>();
		try
		{
			if(null!=childModelDirs && childModelDirs.length>0)
			{
				for(int j=0;j<childModelDirs.length;j++)
				{
					if(childModelDirs[j].exists() && childModelDirs[j].isDirectory())
					{
						String childModelDirName=  childModelDirs[j].getName();
						if(null!=childModelDirName && !"".equals(childModelDirName))
						{
							if(childModelDirName.lastIndexOf("-")!=-1)
							{
								String identificationCode= childModelDirName.substring(childModelDirName.lastIndexOf("-")+1,childModelDirName.length());
								if(null!=identificationCode && !"".equals(identificationCode) && identificationCode.length()>=3)
								{
									String year = identificationCode.substring(0,2);
									String alphabet = identificationCode.substring(2, 3);
									if(null!=year && !"".equals(year) && null!=alphabet && !"".equals(alphabet))
									{
										ChildModelFolderDetails chDetails = new ChildModelFolderDetails();
										chDetails.setYear(year);
										chDetails.setAlphabet(alphabet);
										chDetails.setChildModelFolder(childModelDirs[j]);
										// add chDetails to childModelFoldersList
										childModelFoldersList.add(chDetails);
										chDetails=  null;
									}
									year = null;
									alphabet= null;
								}
							}
						}
					}
					
				}
			}
			
			if(null!=childModelFoldersList && childModelFoldersList.size()>0)
			{
				/*
				 * Sort the ArrayList in the Sequence of the Year and Alphabets 
				 * in Ascending Order
				 */
		       Collections.sort(childModelFoldersList,new ChildModelFolderComparator());
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "arrangeSubModeFoldersinSequence()", e);
		}
		return childModelFoldersList;
	}
	
	public static String identifyChannelFolderName(String manualType)
	{
		String channelName=null;
		try
		{
			if(null!=manualType && (manualType.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder")) || 
					manualType.trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder"))))
			{
				// SET CHANNEL NAME - SERVICE MANUALS
				channelName = ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME");
			}
			else
			{
				// SET CHANNEL NAME - SERVICE MANUALS
				channelName=ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "identifyChannelFolderName()", e);
		}
		return channelName;
	}
	
}
