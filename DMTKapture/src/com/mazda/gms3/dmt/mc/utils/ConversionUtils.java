package com.mazda.gms3.dmt.mc.utils;

import com.mazda.gms3.dmt.utils.PathUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;


import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import com.mazda.gms3.dmt.kapture.OtherManualLinkRules;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.mc.dao.MCDocumentManagementDAO;
import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.mc.vo.VinDetails;
import com.mazda.gms3.dmt.mme.dao.MMEDocumentManagementDAO;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.Utilities;
import com.mazda.gms3.dmt.vo.ContentDetails;
import com.mazda.gms3.dmt.vo.DeleteFileDetails;
import com.mazda.gms3.dmt.vo.LinkDetails;
import com.mazda.gms3.dmt.vo.ScheduleItemDetails;
import com.mazda.gms3.dmt.vo.SelectItemDetails;
import com.mazda.gms3.dmt.vo.UserGroupDetails;

public class ConversionUtils {

	static Logger logger = LogManager.getLogger(ConversionUtils.class);
	
	public static final String LINE_TYPE_VALID="VALID";
	public static final String LINE_TYPE_INVALID="INVALID";
	
	public static ArrayList<DeleteFileDetails> readDeleteTextFile(File deleteTextFile) throws IOException
	{
		BufferedReader br = null;
		ArrayList<DeleteFileDetails> deleteDetailsList = null;
		try
		{
			if(null!=deleteTextFile && deleteTextFile.exists() && deleteTextFile.isFile())
			{
				/*
				 * IDENTIFY LOCALE FOLDER NAME, MODEL FOLDER NAME, MANUAL TYPE FOLDER NAME, FACELIFT FOLDER NAME, MATERIAL FOLDER NAME
				 */
				String materialFolderName=deleteTextFile.getParentFile().getName();
				String faceliftFolderName = deleteTextFile.getParentFile().getParentFile().getName();
				String manualTypeFolderName = deleteTextFile.getParentFile().getParentFile().getParentFile().getName();
				String modelFolderName=deleteTextFile.getParentFile().getParentFile().getParentFile().getParentFile().getName();
				String localeFolderName = deleteTextFile.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getName();
				
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
			    
			    String fileToBeDeleted=null;
	    		String fileToBeDeletedPath=null;
	    		String okAssetsFileToBeDeleted=null;
	    		DeleteFileDetails details = new DeleteFileDetails();
			    String line;
			    while ((line = br.readLine()) != null) {
			    	if(null!=line && !"".equals(line))
			    	{
			    		line = line.trim();
			    		// convert full width chars to single width chars
//			    		line = Utilities.converFullwidth2HalfWidth(line);
			    		/*
			    		 * INCIDENT CHANGE - Step1.2 MC market_10022020_OEM - IN-201013-1680
			    		 * DO THIS ONLY FOR MODEL 
			    		 */
			    		line = Utilities.replaceJunkCharacterToFullbyte(line, modelFolderName, manualTypeFolderName, 
			    				faceliftFolderName, materialFolderName, localeFolderName);
			    		// REMOVE SIE= FROM LINE
			    		if(line.contains("SIE="))
			    		{
			    			line = line.replace("SIE=", "");
			    		}
			    		
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
				    				details = new DeleteFileDetails();	
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
					    			details = new DeleteFileDetails();
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
				    			 * FILE TO BE DELETED - NORMAL WD
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
				    			 * IDENTIFY MATERIAL FOLDER NAME, WHICH WILL BE - IMMEDIATE PARENT OF THE FILE
				    			 * IN THE MATERIAL FOLDER CHECK, THE CHARS AFTER LAST INDEX OF _
				    			 * IF NOT, HTML OR PDF OR DJVU OR XML
				    			 * THEN WD - COMBINATION OF HTML & PDF
				    			 * ELSE USE THE CHARS IN LOWERCASE AS PROCESSING FOLDER NAME
				    			 */
				    			String folderName="";
			    				String tok="";
			    				String mtName = deleteTextFile.getParentFile().getName();
			    				if(mtName.lastIndexOf("_")!=-1)
			    				{
			    					tok=  mtName.substring(mtName.lastIndexOf("_")+1, mtName.length());
			    				}
			    				if(null==tok)
			    				{
			    					tok="";
			    				}
			    				// DO NOT CHECK FOR NULL
			    				if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
			    						!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
			    						!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
			    						!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
			    				{
			    					// ADD PROCESSING FOLDER AS HTML (SINCE IT CAN BE EITHER THROUGH HTML OR PDF
			    					folderName = ApplicationProperties.getProperty("directory.html");
			    				}
			    				else
			    				{
			    					if(tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")))
			    					{
			    						folderName = ApplicationProperties.getProperty("directory.html");
			    					}
			    					else if(tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")))
			    					{
			    						folderName = ApplicationProperties.getProperty("directory.pdf");
			    					}
			    					else if(tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
			    					{
			    						folderName = ApplicationProperties.getProperty("directory.djvu");
			    					}
			    					else if(tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")))
			    					{
			    						folderName = ApplicationProperties.getProperty("directory.ent.dir");
			    					}
			    				}
			    				tok = null;
			    				mtName  = null;
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
					    			details = new DeleteFileDetails();
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
				    				|| line.toLowerCase().endsWith(ApplicationProperties.getProperty("extension.djvu")))
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
				    			else if(line.toLowerCase().endsWith(ApplicationProperties.getProperty("extension.djvu")))
				    			{
				    				folderName= ApplicationProperties.getProperty("directory.djvu");
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
					    			details = new DeleteFileDetails();
						    		details.setFileName(deleteTextFile.getName());
						    		details.setFilePath(PathUtil.winPath(deleteTextFile));
					    			details.setFileToBeDeletedName(fileToBeDeleted);
					    			details.setFileToBeDeletedPath(fileToBeDeletedPath);
					    			details.setFileType(DeleteFileDetails.TYPE_DOCUMENT);
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
				    		else
				    		{
				    			/*
				    			 * OK ASSETS FILE TO BE DELETED
				    			 */
				    			okAssetsFileToBeDeleted = line.trim();
				    			if(null!=okAssetsFileToBeDeleted && !"".equals(okAssetsFileToBeDeleted))
				    			{
				    				details = new DeleteFileDetails();
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
			    details=null;
			    fileToBeDeleted=null;
	    		fileToBeDeletedPath=null;
	    		okAssetsFileToBeDeleted=null;
	    		localeFolderName = null;
	    		modelFolderName=null;
	    		manualTypeFolderName = null;
	    		faceliftFolderName=null;
	    		materialFolderName=null;
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

	public static void main(String args[])
	{
		try
		{
			File foldersList = PathUtil.file("\\\\m2okfs10\\sourcecontentqa\\2byte_filename_sample\\original\\GMS3_Content\\MC\\ja-JP\\Suzuki_azwagon_mj\\WM\\FL0004\\E-W1J-4TE1_DJVU\\djvu");
			File[] istFile = foldersList.listFiles();
			String fileName="";
			for(int a=0;a<istFile.length;a++)
			{
				fileName = istFile[a].getName();
				System.out.println("fileName :: > "+ istFile[a].getName());
				logger.info("fileName :: > "+ istFile[a].getName());
				
				if(a==2)
				{
					break;
				}
			}
			
			
			
			
//			File esiCategoryFile = PathUtil.file("C:\\Users\\vdabkara\\Downloads\\2byte_filename_sample\\original\\GMS3_Content\\MC\\ja-JP\\Suzuki_azwagon_mj\\WM\\FL0004\\E-W1J-4TE1_DJVU\\esicat.txt");
//			InputStreamReader is = new InputStreamReader(PathUtil.fileInputStream(esiCategoryFile.getAbsoluteFile()));
//			System.out.println(is.getEncoding());
			
//			StringBuffer buffer = new StringBuffer();
//            FileInputStream fis = PathUtil.fileInputStream(esiCategoryFile);
//            InputStreamReader isr = new InputStreamReader(fis, "SJIS");
//            Reader in = new BufferedReader(isr);
//
//            int ch;
//            while ((ch = in.read()) > -1) {
//                buffer.append(Character.toChars(ch));
//                
//            }
//            in.close();
//
//            System.out.println(buffer.toString());
//			
			
//			ArrayList<ESICategoryDetails> list = readESICategoryTextFile(esiCategoryFile);
//			if(null!=list)
//			{
//				for(int a=0;a<list.size();a++)
//				{
//					ESICategoryDetails details = (ESICategoryDetails)list.get(a);
//					File f = PathUtil.file("C:\\Users\\vdabkara\\Downloads\\2byte_filename_sample\\original\\GMS3_Content\\MC\\ja-JP\\Suzuki_azwagon_mj\\WM\\FL0004\\E-W1J-4TE1_DJVU\\djvu\\"+details.getFileName());
//					System.out.println("File exists :: >>"+ f.exists()+" :: >> fileName :: >"+ details.getFileName());
//					logger.info("File exists :: >>"+ f.exists()+" :: >> fileName :: >"+ details.getFileName());
//				}
//			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Function will read all the Information from ESI CATEGORY TEXT FILE FOR MC AS WELL AS MME MARKET
	 * @param esiCategoryFile
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<ESICategoryDetails> readESICategoryTextFile(File esiCategoryFile) throws IOException
	{
		BufferedReader br = null;
		ArrayList<ESICategoryDetails> esiCategoryDetailsList = null;
		try
		{
			if(null!=esiCategoryFile && esiCategoryFile.exists() && esiCategoryFile.isFile())
			{
				String manualTypeFolderName = esiCategoryFile.getParentFile().getParentFile().getParentFile().getName();
				String modelFolderName=esiCategoryFile.getParentFile().getParentFile().getParentFile().getParentFile().getName();
				String localeFolderName = esiCategoryFile.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getName();
				String marketFolderPath = PathUtil.winPath(esiCategoryFile.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile());
				br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(esiCategoryFile.getAbsoluteFile()), "UTF-8"));
			    
			    ESICategoryDetails details = new ESICategoryDetails();
			    String[] tokens = null;
			    String line;
			    while ((line = br.readLine()) != null) {
			    	if(null!=line && !"".equals(line))
			    	{
			    		// convert full width chars to single width chars
//			    		line = Utilities.converFullwidth2HalfWidth(line);
			    		if(line.endsWith(";"))
			    		{
			    			line = line.substring(0, line.length()-1);
			    		}
			    		details = new ESICategoryDetails();
			    		// <FC FOLDERNAME>||<MATERIAL FOLDERNAME>||<PROCESSING FOLDERNAME>||<FILENAME>||<TITLE>||<CATEGORYLEVEL 1 CODE>||<CATEGORYLEVEL 3 CODE>||<CATEGORYLEVEL 2 CODE>
			    		tokens = line.split("\\|\\|");
			    		if(null!=tokens && tokens.length>0)
			    		{
			    			try
			    			{
			    				if(tokens.length>=0)
			    				{
			    					if(null!=tokens[0] && !"".equals(tokens[0]))
			    					{
			    						// set FACELIFT FOLDER NAME
			    						details.setFaceLiftFolderName(String.valueOf(tokens[0]));
			    					}
			    				}
			    			}
			    			catch (Exception e) {
			    			}

			    			try
			    			{
			    				if(tokens.length>=1)
			    				{
			    					if(null!=tokens[1] && !"".equals(tokens[1]))
			    					{
			    						// set MATERIAL FOLDER NAME
			    						details.setMaterialFolderName(String.valueOf(tokens[1]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=2)
			    				{
			    					if(null!=tokens[2] && !"".equals(tokens[2]))
			    					{
			    						// set PROCESSING FOLDER NAME
			    						details.setProcessingFolderName(String.valueOf(tokens[2]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=3)
			    				{
			    					if(null!=tokens[3] && !"".equals(tokens[3]))
			    					{
			    						// set FILE NAME
			    						details.setFileName(String.valueOf(tokens[3]).trim()); // trimmed: names are listed with a trailing space at times (MNAO e-WD)
			    						/*
			    			    		 * INCIDENT CHANGE - Step1.2 MC market_10022020_OEM - IN-201013-1680
			    			    		 * DO THIS ONLY FOR MODEL 
			    			    		 */
			    			    		details.setFileName(Utilities.replaceJunkCharacterToFullbyte(details.getFileName(), modelFolderName, manualTypeFolderName, details.getFaceLiftFolderName(), details.getMaterialFolderName(), localeFolderName)); 
			    			    		
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=4)
			    				{
			    					if(null!=tokens[4] && !"".equals(tokens[4]))
			    					{
			    						// set TITLE
			    						details.setTitle(String.valueOf(tokens[4]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=5)
			    				{
			    					if(null!=tokens[5] && !"".equals(tokens[5]))
			    					{
			    						// set CATEGORY LEVLE 1 CODE
			    						details.setCategoryLevel1Code(String.valueOf(tokens[5]));
			    						details.setCategoryLevel1Code(details.getCategoryLevel1Code().trim());
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=6)
			    				{
			    					if(null!=tokens[6] && !"".equals(tokens[6]))
			    					{
			    						// set CATEGORY LEVEL 2 CODE
			    						details.setCategoryLevel2Code(String.valueOf(tokens[6]));
			    						details.setCategoryLevel2Code(details.getCategoryLevel2Code().trim());
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=7)
			    				{
			    					if(null!=tokens[7] && !"".equals(tokens[7]))
			    					{
			    						// set CATEGORY LEVEL 3 CODE
			    						details.setCategoryLevel3Code(String.valueOf(tokens[7]));
			    						if(null!=details.getCategoryLevel3Code() && !"".equals(details.getCategoryLevel3Code()))
			    						{
			    							details.setCategoryLevel3Code(details.getCategoryLevel3Code().trim());
			    							if(details.getCategoryLevel3Code().endsWith(";"))
			    							{
			    								details.setCategoryLevel3Code(details.getCategoryLevel3Code().substring(0, details.getCategoryLevel3Code().length()-1));
			    							}
			    						}
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    		}
			    		
			    		/*
			    		 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
			    		 * STARTING FROM LOCALE
			    		 */
			    		String filePath=null;
			    		String fileAbsolutePath=null;
			    		if(null!=localeFolderName && !"".equals(localeFolderName) && null!=modelFolderName && !"".equals(modelFolderName) && null!=manualTypeFolderName && !"".equals(manualTypeFolderName))
			    		{
			    			if(null!=details && null!=details.getFaceLiftFolderName() && !"".equals(details.getFaceLiftFolderName()) 
			    					&& null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName()) 
			    					&& null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) 
			    					&& null!=details.getFileName() && !"".equals(details.getFileName()))
			    			{
			    				filePath = localeFolderName+"\\"+modelFolderName+"\\"+manualTypeFolderName+"\\"+details.getFaceLiftFolderName()+"\\"+details.getMaterialFolderName();
			    				filePath =filePath+"\\"+details.getProcessingFolderName()+"\\"+details.getFileName();
			    				
			    				if(null!=marketFolderPath && !"".equals(marketFolderPath))
			    				{
			    					if(!marketFolderPath.endsWith("\\"))
			    					{
			    						marketFolderPath=marketFolderPath+"\\";
			    					}
			    					fileAbsolutePath = marketFolderPath+filePath;
			    				}
			    			}
			    		}
			    		
			    		if(null!=fileAbsolutePath && !"".equals(fileAbsolutePath))
			    		{
			    			details.setFileAbsolutePath(fileAbsolutePath);
			    		}
			    		if(null!=filePath && !"".equals(filePath))
			    		{
			    			details.setLineType(LINE_TYPE_VALID);
			    			details.setFilePath(filePath);
			    		}
			    		else
			    		{
			    			details.setLineType(LINE_TYPE_INVALID);
			    		}
			    		
			    		details.setEsiCategorySourceFileName(esiCategoryFile.getName());
			    		details.setEsiCategorySourceFilePath(PathUtil.winPath(esiCategoryFile));
			    		if(null==esiCategoryDetailsList || esiCategoryDetailsList.size()<=0)
		    			{
		    				esiCategoryDetailsList = new ArrayList<ESICategoryDetails>();
		    			}
		    			// add details to ESI CATEGORY LIST
		    			esiCategoryDetailsList.add(details);
			    		tokens=  null;
			    		details= null;
			    		fileAbsolutePath=null;
			    		filePath=null;
		    		}
			    }
			    line = null;
			    manualTypeFolderName=null;
			    localeFolderName=null;
			    marketFolderPath=null;
			    modelFolderName=null;
			    details=null;
			    tokens=null;
			}
			else
			{
				logger.info("readESICategoryTextFile :: ESI Category Text file is either Corrupted or Does not Exist.");
				esiCategoryDetailsList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readESICategoryTextFile()", e);
//			esiCategoryDetailsList = null;
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return esiCategoryDetailsList;
	}

	/**
	 * Function will read the ESI CATEGORY TEXT FILE SPECIFICALLY FOR MME MARKET WHEN PROCESSING WIRING DIAGRAMS
	 * @param esiCategoryFile
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<ESICategoryDetails> readESICategoryTextFileForMME_WD(File esiCategoryFile) throws IOException
	{
		BufferedReader br = null;
		ArrayList<ESICategoryDetails> esiCategoryDetailsList = null;
		try
		{
			if(null!=esiCategoryFile && esiCategoryFile.exists() && esiCategoryFile.isFile())
			{
				String manualTypeFolderName = esiCategoryFile.getParentFile().getParentFile().getParentFile().getName();
				String modelFolderName=esiCategoryFile.getParentFile().getParentFile().getParentFile().getParentFile().getName();
				String localeFolderName = esiCategoryFile.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getName();
				String marketFolderPath = PathUtil.winPath(esiCategoryFile.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile());
			    br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(esiCategoryFile.getAbsoluteFile()), "UTF-8"));
			    ESICategoryDetails details = new ESICategoryDetails();
			    String[] tokens= null;
			    String line;
			    while ((line = br.readLine()) != null) {
			    	if(null!=line && !"".equals(line))
			    	{
			    		// convert full width chars to single width chars
//			    		line = Utilities.converFullwidth2HalfWidth(line);
			    		if(line.endsWith(";"))
			    		{
			    			line = line.substring(0, line.length()-1);
			    		}
			    		details = new ESICategoryDetails();
			    		// <FC FOLDERNAME>||<MATERIAL FOLDERNAME>||<PROCESSING FOLDERNAME>||<FILENAME>||<TITLE>||<STEERING TYPE1, STEERING TYPE 2>||<CATEGORYLEVEL 1 CODE>||<CATEGORYLEVEL 3 CODE>||<CATEGORYLEVEL 2 CODE>
			    		tokens = line.split("\\|\\|");
			    		if(null!=tokens && tokens.length>0)
			    		{
			    			try
			    			{
			    				if(tokens.length>=0)
			    				{
			    					if(null!=tokens[0] && !"".equals(tokens[0]))
			    					{
			    						// set FACELIFT FOLDER NAME
			    						details.setFaceLiftFolderName(String.valueOf(tokens[0]));
			    					}
			    				}
			    			}
			    			catch (Exception e) {
			    			}

			    			try
			    			{
			    				if(tokens.length>=1)
			    				{
			    					if(null!=tokens[1] && !"".equals(tokens[1]))
			    					{
			    						// set MATERIAL FOLDER NAME
			    						details.setMaterialFolderName(String.valueOf(tokens[1]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=2)
			    				{
			    					if(null!=tokens[2] && !"".equals(tokens[2]))
			    					{
			    						// set PROCESSING FOLDER NAME
			    						details.setProcessingFolderName(String.valueOf(tokens[2]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=3)
			    				{
			    					if(null!=tokens[3] && !"".equals(tokens[3]))
			    					{
			    						// set FILE NAME
			    						details.setFileName(String.valueOf(tokens[3]).trim()); // trimmed: names are listed with a trailing space at times (MNAO e-WD)
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=4)
			    				{
			    					if(null!=tokens[4] && !"".equals(tokens[4]))
			    					{
			    						// set TITLE
			    						details.setTitle(String.valueOf(tokens[4]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}
			    			
			    			try
			    			{
			    				if(tokens.length>=5)
			    				{
			    					if(null!=tokens[5] && !"".equals(tokens[5]))
			    					{
			    						// set STEERING TYPES
			    						details.setSteeringTypeInfoText(String.valueOf(tokens[5]));
			    						details.setSteeringTypeInfoText(details.getSteeringTypeInfoText().trim());
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}
			    			
			    			
			    			try{
			    				if(tokens.length>=6)
			    				{
			    					if(null!=tokens[6] && !"".equals(tokens[6]))
			    					{
			    						// set CATEGORY LEVLE 1 CODE
			    						details.setCategoryLevel1Code(String.valueOf(tokens[6]));
			    						details.setCategoryLevel1Code(details.getCategoryLevel1Code().trim());
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=7)
			    				{
			    					if(null!=tokens[7] && !"".equals(tokens[7]))
			    					{
			    						// set CATEGORY LEVEL 2 CODE
			    						details.setCategoryLevel2Code(String.valueOf(tokens[7]));
			    						details.setCategoryLevel2Code(details.getCategoryLevel2Code().trim());
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=8)
			    				{
			    					if(null!=tokens[8] && !"".equals(tokens[8]))
			    					{
			    						// set CATEGORY LEVEL 3 CODE
			    						details.setCategoryLevel3Code(String.valueOf(tokens[8]));
			    						if(null!=details.getCategoryLevel3Code() && !"".equals(details.getCategoryLevel3Code()))
			    						{
			    							details.setCategoryLevel3Code(details.getCategoryLevel3Code().trim());
			    							if(details.getCategoryLevel3Code().endsWith(";"))
			    							{
			    								details.setCategoryLevel3Code(details.getCategoryLevel3Code().substring(0, details.getCategoryLevel3Code().length()-1));
			    							}
			    						}
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    		}
			    		
			    		/*
			    		 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
			    		 * STARTING FROM LOCALE
			    		 */
			    		String filePath=null;
			    		String fileAbsolutePath=null;
			    		if(null!=localeFolderName && !"".equals(localeFolderName) && null!=modelFolderName && !"".equals(modelFolderName) && null!=manualTypeFolderName && !"".equals(manualTypeFolderName))
			    		{
			    			if(null!=details && null!=details.getFaceLiftFolderName() && !"".equals(details.getFaceLiftFolderName()) 
			    					&& null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName()) 
			    					&& null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) 
			    					&& null!=details.getFileName() && !"".equals(details.getFileName()))
			    			{
			    				filePath = localeFolderName+"\\"+modelFolderName+"\\"+manualTypeFolderName+"\\"+details.getFaceLiftFolderName()+"\\"+details.getMaterialFolderName();
			    				filePath =filePath+"\\"+details.getProcessingFolderName()+"\\"+details.getFileName();
			    				
			    				if(null!=marketFolderPath && !"".equals(marketFolderPath))
			    				{
			    					if(!marketFolderPath.endsWith("\\"))
			    					{
			    						marketFolderPath=marketFolderPath+"\\";
			    					}
			    					fileAbsolutePath = marketFolderPath+filePath;
			    				}
			    			}
			    		}
			    		
			    		if(null!=fileAbsolutePath && !"".equals(fileAbsolutePath))
			    		{
			    			details.setFileAbsolutePath(fileAbsolutePath);
			    		}
			    		if(null!=filePath && !"".equals(filePath))
			    		{
			    			details.setLineType(LINE_TYPE_VALID);
			    			details.setFilePath(filePath);
			    		}
			    		else
			    		{
			    			details.setLineType(LINE_TYPE_INVALID);
			    		}
			    		
			    		details.setEsiCategorySourceFileName(esiCategoryFile.getName());
			    		details.setEsiCategorySourceFilePath(PathUtil.winPath(esiCategoryFile));
			    		if(null==esiCategoryDetailsList || esiCategoryDetailsList.size()<=0)
		    			{
		    				esiCategoryDetailsList = new ArrayList<ESICategoryDetails>();
		    			}
		    			// add details to ESI CATEGORY LIST
		    			esiCategoryDetailsList.add(details);
			    		tokens=  null;
			    		details= null;
			    		fileAbsolutePath=null;
			    		filePath=null;
		    		}
			    }
			    line = null;
			    manualTypeFolderName=null;
			    localeFolderName=null;
			    marketFolderPath=null;
			    modelFolderName=null;
			    details = null;
			    tokens = null;
			}
			else
			{
				logger.info("readESICategoryTextFileForMME_WD :: ESI Category Text file is either Corrupted or Does not Exist.");
				esiCategoryDetailsList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readESICategoryTextFileForMME_WD()", e);
//			esiCategoryDetailsList = null;
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return esiCategoryDetailsList;
	}
	
	/**
	 * Function will read all the Information from VIN TEXT FILE for MC Market
	 * @param vinFile
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<VinDetails> readVINTextFile(File vinFile,String fetchModelDetails) throws IOException
	{
		BufferedReader br = null;
		ArrayList<VinDetails> vinDetailsList = null;
		try
		{
			if(null!=vinFile && vinFile.exists() && vinFile.isFile())
			{
				// VIN FILE IS AT LOCATION PARALLEL TO MATERIAL FOLDER.
				String modelFolderName=vinFile.getParentFile().getParentFile().getParentFile().getName();
				String localeFolderName = vinFile.getParentFile().getParentFile().getParentFile().getParentFile().getName();
				
			    br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(vinFile.getAbsoluteFile()), "UTF-8"));
			    
			    VinDetails details = new VinDetails();
			    String[] tokens = null;
			    String line;
			    while ((line = br.readLine()) != null) {
			    	if(null!=line && !"".equals(line))
			    	{
			    		// convert full width chars to single width chars
//			    		line = Utilities.converFullwidth2HalfWidth(line);
			    		if(line.endsWith(";"))
			    		{
			    			line = line.substring(0, line.length()-1);
			    		}
			    		details = new VinDetails();
			    		// <MANUAL TYPE FOLDER NAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||<CARLINE CODE>||<VDS CODE>||<VIS START RANGE>||<VIS END RANGE>||<DISPLAY ORDER TEXT FILE>
			    		tokens = line.split("\\|\\|");
			    		if(null!=tokens && tokens.length>0)
			    		{
			    			// tokens. length == 8 - all tokens exists
			    			if(tokens.length==8)
			    			{
			    				details.setAllTokensExists(true);
			    			}
			    			
			    			try
			    			{
			    				if(tokens.length>=0)
			    				{
			    					if(null!=tokens[0] && !"".equals(tokens[0]))
			    					{
			    						// set MANUAL TYPE FOLDER NAME
			    						details.setManualType(String.valueOf(tokens[0]));
			    					}
			    				}
			    			}
			    			catch (Exception e) {
			    			}

			    			try
			    			{
			    				if(tokens.length>=1)
			    				{
			    					if(null!=tokens[1] && !"".equals(tokens[1]))
			    					{
			    						// set FACE LIFT FOLDER NAME
			    						details.setFaceLiftFolder(String.valueOf(tokens[1]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=2)
			    				{
			    					if(null!=tokens[2] && !"".equals(tokens[2]))
			    					{
			    						// set MATERIAL FOLDER NAME
			    						details.setMaterialFolder(String.valueOf(tokens[2]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=3)
			    				{
			    					if(null!=tokens[3] && !"".equals(tokens[3]))
			    					{
			    						// set CARLINE CODE
			    						details.setCarlineCode(String.valueOf(tokens[3]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=4)
			    				{
			    					if(null!=tokens[4] && !"".equals(tokens[4]))
			    					{
			    						// set VDS CODE
			    						details.setVdsCode(String.valueOf(tokens[4]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=5)
			    				{
			    					if(null!=tokens[5] && !"".equals(tokens[5]))
			    					{
			    						// set VIS START RANGE
			    						details.setVisStartRange(String.valueOf(tokens[5]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=6)
			    				{
			    					if(null!=tokens[6] && !"".equals(tokens[6]))
			    					{
			    						// set VIS END RANGE
			    						details.setVisEndRange(String.valueOf(tokens[6]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=7)
			    				{
			    					if(null!=tokens[7] && !"".equals(tokens[7]))
			    					{
			    						// set DISPLAY ORDER FILE
			    						details.setDisplayOrderTextFileName(String.valueOf(tokens[7]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    		}
			    		
			    		/*
			    		 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
			    		 * STARTING FROM LOCALE
			    		 */
//			    		String filePath=null;
			    		if(null!=localeFolderName && !"".equals(localeFolderName) && 
			    				null!=modelFolderName && !"".equals(modelFolderName))
			    		{
			    			if(null!=details && null!=details.getManualType() && !"".equals(details.getManualType())
			    					&& null!=details.getFaceLiftFolder() && !"".equals(details.getFaceLiftFolder()) 
			    					&& null!=details.getMaterialFolder() && !"".equals(details.getMaterialFolder()) 
			    					&& null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()) 
			    					&& null!=details.getVdsCode() && !"".equals(details.getVdsCode()) 
			    					&& null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()) 
			    					&& null!=details.getVisEndRange() && !"".equals(details.getVisEndRange())) 
			    			{
			    				/*
			    				 * NO NEED OF ADDING ANY FILE PATH
			    				 * SET LINE TYPE AS VALID
			    				 */
			    				details.setLineType(LINE_TYPE_VALID);
//			    				filePath = localeFolderName+"\\"+modelFolderName+"\\"+details.getManualType()+"\\"+
//			    						details.getFaceLiftFolder()+"\\"+details.getMaterialFolder();
			    			}
			    			else
			    			{
			    				details.setLineType(LINE_TYPE_INVALID);
			    			}
			    		}
			    		else
			    		{
			    			details.setLineType(LINE_TYPE_INVALID);
			    		}
			    		
			    		details.setVinSourceFileName(vinFile.getName());
			    		details.setVinSorceFilePath(PathUtil.winPath(vinFile));
			    		
			    		/*
						 * SET WMI CODE EXPLICITLY AS - FOR MC MARKET
						 * date 9th august 2019
						 */
						details.setWmiCode("-");
			    		
			    		if(null==vinDetailsList || vinDetailsList.size()<=0)
		    			{
		    				vinDetailsList = new ArrayList<VinDetails>();
		    			}
		    			// add details to VIN LIST
		    			vinDetailsList.add(details);
			    		
			    		tokens=  null;
			    		details= null;
		    		}
			    }
			    line = null;
			   details = null;
			   tokens= null;
			   
			   if(null!=vinDetailsList && vinDetailsList.size()>0 && null!=fetchModelDetails && fetchModelDetails.equals("Y"))
			   {
				   // get carlineNameEnf & carlineNameReg for all VINs - use Master Locale
				   vinDetailsList=  MMEDocumentManagementDAO.getModelDetails(vinDetailsList, ApplicationProperties.getProperty("ja-jp"));
			   }
			}
			else
			{
				logger.info("readVINTextFile :: VIN Text file is either Corrupted or Does not Exist.");
				vinDetailsList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readVINTextFile()", e);
//			vinDetailsList = null;
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return vinDetailsList;
	}

	/**
	 * File will read all the Information from VIN TEXT FILE for MME Market
	 * @param vinFile
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<VinDetails> readVINTextFileForMME(File vinFile,String fetchModelDetails) throws IOException
	{
		BufferedReader br = null;
		ArrayList<VinDetails> vinDetailsList = null;
		try
		{
			if(null!=vinFile && vinFile.exists() && vinFile.isFile())
			{
				// VIN FILE IS AT LOCATION PARALLEL TO MATERIAL FOLDER.
				String modelFolderName=vinFile.getParentFile().getParentFile().getParentFile().getName();
				String localeFolderName = vinFile.getParentFile().getParentFile().getParentFile().getParentFile().getName();
				
			    br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(vinFile.getAbsoluteFile()), "UTF-8"));
			    
			    String line;
			    String[] tokens=null;
			    VinDetails details = new VinDetails();
			    while ((line = br.readLine()) != null) {
			    	if(null!=line && !"".equals(line))
			    	{
			    		// convert full width chars to single width chars
//			    		line = Utilities.converFullwidth2HalfWidth(line);
			    		if(line.endsWith(";"))
			    		{
			    			line = line.substring(0, line.length()-1);
			    		}
			    		details = new VinDetails();
			    		// <MANUAL TYPE FOLDER NAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||<CARLINE CODE>||<WMI_CODE>||<VDS CODE>||<VIS START RANGE>||<VIS END RANGE>||<DISPLAY ORDER TEXT FILE>
			    		tokens = line.split("\\|\\|");
			    		if(null!=tokens && tokens.length>0)
			    		{
			    			// tokens. length == 9 - all tokens exists
			    			if(tokens.length==9)
			    			{
			    				details.setAllTokensExists(true);
			    			}
			    			try
			    			{
			    				if(tokens.length>=0)
			    				{
			    					if(null!=tokens[0] && !"".equals(tokens[0]))
			    					{
			    						// set MANUAL TYPE FOLDER NAME
			    						details.setManualType(String.valueOf(tokens[0]));
			    					}
			    				}
			    			}
			    			catch (Exception e) {
			    			}

			    			try
			    			{
			    				if(tokens.length>=1)
			    				{
			    					if(null!=tokens[1] && !"".equals(tokens[1]))
			    					{
			    						// set FACE LIFT FOLDER NAME
			    						details.setFaceLiftFolder(String.valueOf(tokens[1]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=2)
			    				{
			    					if(null!=tokens[2] && !"".equals(tokens[2]))
			    					{
			    						// set MATERIAL FOLDER NAME
			    						details.setMaterialFolder(String.valueOf(tokens[2]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=3)
			    				{
			    					if(null!=tokens[3] && !"".equals(tokens[3]))
			    					{
			    						// set CARLINE CODE
			    						details.setCarlineCode(String.valueOf(tokens[3]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=4)
			    				{
			    					if(null!=tokens[4] && !"".equals(tokens[4]))
			    					{
			    						// set WMI CODE
			    						details.setWmiCode(String.valueOf(tokens[4]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}
			    			
			    			try{
			    				if(tokens.length>=5)
			    				{
			    					if(null!=tokens[5] && !"".equals(tokens[5]))
			    					{
			    						// set VDS CODE
			    						details.setVdsCode(String.valueOf(tokens[5]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=6)
			    				{
			    					if(null!=tokens[6] && !"".equals(tokens[6]))
			    					{
			    						// set VIS START RANGE
			    						details.setVisStartRange(String.valueOf(tokens[6]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=7)
			    				{
			    					if(null!=tokens[7] && !"".equals(tokens[7]))
			    					{
			    						// set VIS END RANGE
			    						details.setVisEndRange(String.valueOf(tokens[7]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}
			    			
			    			try{
			    				if(tokens.length>=8)
			    				{
			    					if(null!=tokens[8] && !"".equals(tokens[8]))
			    					{
			    						// set DISPLAY ORDER FILE
			    						details.setDisplayOrderTextFileName(String.valueOf(tokens[8]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    		}
			    		
			    		/*
			    		 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
			    		 * STARTING FROM LOCALE
			    		 */
//			    		String filePath=null;
			    		if(null!=localeFolderName && !"".equals(localeFolderName) && 
			    				null!=modelFolderName && !"".equals(modelFolderName))
			    		{
			    			if(null!=details && null!=details.getManualType() && !"".equals(details.getManualType())
			    					&& null!=details.getFaceLiftFolder() && !"".equals(details.getFaceLiftFolder()) 
			    					&& null!=details.getMaterialFolder() && !"".equals(details.getMaterialFolder()) 
			    					&& null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()) 
			    					&& null!=details.getWmiCode() && !"".equals(details.getWmiCode())
			    					&& null!=details.getVdsCode() && !"".equals(details.getVdsCode()) 
			    					&& null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()) 
			    					&& null!=details.getVisEndRange() && !"".equals(details.getVisEndRange())) 
			    			{
			    				/*
			    				 * NO NEED OF ADDING ANY FILE PATH
			    				 * SET LINE TYPE AS VALID
			    				 */
			    				details.setLineType(LINE_TYPE_VALID);
//			    				filePath = localeFolderName+"\\"+modelFolderName+"\\"+details.getManualType()+"\\"+
//			    						details.getFaceLiftFolder()+"\\"+details.getMaterialFolder();
			    			}
			    			else
			    			{
			    				details.setLineType(LINE_TYPE_INVALID);
			    			}
			    		}
			    		else
			    		{
			    			details.setLineType(LINE_TYPE_INVALID);
			    		}
			    		
			    		details.setVinSourceFileName(vinFile.getName());
			    		details.setVinSorceFilePath(PathUtil.winPath(vinFile));
			    		if(null==vinDetailsList || vinDetailsList.size()<=0)
		    			{
		    				vinDetailsList = new ArrayList<VinDetails>();
		    			}
		    			// add details to VIN LIST
		    			vinDetailsList.add(details);
			    		
			    		tokens=  null;
			    		details= null;
		    		}
			    }
			    line = null;
			   details = null;
			   tokens=null;
			   
			   if(null!=vinDetailsList && vinDetailsList.size()>0 && null!=fetchModelDetails && fetchModelDetails.equals("Y"))
			   {
				   // get carlineNameEnf & carlineNameReg for all VINs - use Master Locale
				   vinDetailsList=  MMEDocumentManagementDAO.getModelDetails(vinDetailsList, ApplicationProperties.getProperty("en-uk"));
			   }
			}
			else
			{
				logger.info("readVINTextFileForMME :: VIN Text file is either Corrupted or Does not Exist.");
				vinDetailsList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readVINTextFileForMME()", e);
//			vinDetailsList = null;
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return vinDetailsList;
	}

	/**
	 * READS THE vin.txt OF AN MNAO FACELIFT FOLDER (new MNAO content layout, same place as MC / MME).
	 *
	 * <MANUAL TYPE>||<FACELIFT>||<MATERIAL>||<CARLINE>||<WMI>||<VDS>||<VIS START>||<VIS END>||<MODEL>||<YEAR>||<DISPLAY ORDER FILE>
	 * e.g. WM||FL0012||E-WZE-8FC2_MY2026_HTML||TF||AMP2||TFR**JT*||600001||649999||CX90||2026MY||d01.txt
	 *
	 * MNAO carries the MODEL and the MODEL YEAR after the VIS end range (they were in vin.ent before) -
	 * the model year category is made from them. ALL 11 fields are required: a line without the model
	 * or the year is an INVALID line (reported with the VIN file), nothing is guessed. Every field is
	 * trimmed. The carline names are read from MDM in the language of the locale folder.
	 */
	public static ArrayList<VinDetails> readVINTextFileForMNAO(File vinFile,String fetchModelDetails) throws IOException
	{
		BufferedReader br = null;
		ArrayList<VinDetails> vinDetailsList = null;
		try
		{
			if(null!=vinFile && vinFile.exists() && vinFile.isFile())
			{
				// <locale>/<model>/<manual>/<facelift>/vin.txt
				String localeFolderName = vinFile.getParentFile().getParentFile().getParentFile().getParentFile().getName();
				br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(vinFile.getAbsoluteFile()), "UTF-8"));
				String line;
				while ((line = br.readLine()) != null)
				{
					if(null==line || "".equals(line.trim()))
					{
						continue;
					}
					line = line.trim();
					if(line.endsWith(";"))
					{
						line = line.substring(0, line.length()-1);
					}
					String[] tokens = line.split("\\|\\|", -1);
					VinDetails details = new VinDetails();
					details.setAllTokensExists(tokens.length==11);
					details.setManualType(token(tokens, 0));
					details.setFaceLiftFolder(token(tokens, 1));
					details.setMaterialFolder(token(tokens, 2));
					details.setCarlineCode(token(tokens, 3));
					details.setWmiCode(token(tokens, 4));
					details.setVdsCode(token(tokens, 5));
					details.setVisStartRange(token(tokens, 6));
					details.setVisEndRange(token(tokens, 7));
					if(tokens.length==11)
					{
						details.setModelName(token(tokens, 8));
						details.setModelYear(token(tokens, 9));
						details.setDisplayOrderTextFileName(token(tokens, 10));
					}
					else if(tokens.length>8)
					{
						// the display order file is always the LAST field - kept for the report of the invalid line
						details.setDisplayOrderTextFileName(token(tokens, tokens.length-1));
					}
					if(details.isAllTokensExists() && null!=details.getManualType() && null!=details.getFaceLiftFolder() && null!=details.getMaterialFolder()
							&& null!=details.getCarlineCode() && null!=details.getWmiCode() && null!=details.getVdsCode()
							&& null!=details.getVisStartRange() && null!=details.getVisEndRange() && null!=details.getModelName()
							&& null!=details.getModelYear() && null!=details.getDisplayOrderTextFileName())
					{
						details.setLineType(LINE_TYPE_VALID);
					}
					else
					{
						details.setLineType(LINE_TYPE_INVALID);
						logger.info("readVINTextFileForMNAO :: INVALID VIN LINE (11 fields required, " + tokens.length + " found) in " + vinFile.getName() + " :: " + line);
					}
					details.setVinSourceFileName(vinFile.getName());
					details.setVinSorceFilePath(PathUtil.winPath(vinFile));
					if(null==vinDetailsList)
					{
						vinDetailsList = new ArrayList<VinDetails>();
					}
					vinDetailsList.add(details);
				}
				if(null!=vinDetailsList && vinDetailsList.size()>0 && null!=fetchModelDetails && fetchModelDetails.equals("Y"))
				{
					// carline names (English / regional) in the language of the locale folder - MNAO has no master locale
					String mdmLocale = ApplicationProperties.getProperty(localeFolderName.trim().toLowerCase());
					vinDetailsList = MMEDocumentManagementDAO.getModelDetails(vinDetailsList, null!=mdmLocale ? mdmLocale : localeFolderName);
				}
			}
			else
			{
				logger.info("readVINTextFileForMNAO :: VIN Text file is either Corrupted or Does not Exist.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readVINTextFileForMNAO()", e);
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return vinDetailsList;
	}

	/** Field i of a split line, trimmed; null when missing or blank. */
	private static String token(String[] tokens, int i)
	{
		if(null==tokens || i<0 || i>=tokens.length || null==tokens[i] || "".equals(tokens[i].trim()))
		{
			return null;
		}
		return tokens[i].trim();
	}

	/**
	 * Function will read SCM VIN Text File FOR MME Market
	 * @param vinFile
	 * @param fetchModelDetails
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<VinDetails> readSCMVINTextFileForMME(File vinFile,String fetchModelDetails) throws IOException
	{
		BufferedReader br = null;
		ArrayList<VinDetails> vinDetailsList = null;
		try
		{
			if(null!=vinFile && vinFile.exists() && vinFile.isFile())
			{
				// VIN FILE IS AT LOCATION PARALLEL TO MATERIAL FOLDER.
				String modelFolderName=vinFile.getParentFile().getParentFile().getParentFile().getName();
				String localeFolderName = vinFile.getParentFile().getParentFile().getParentFile().getParentFile().getName();
				
			    br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(vinFile.getAbsoluteFile()), "UTF-8"));
			    
			    String line;
			    String[] tokens=null;
			    VinDetails details = new VinDetails();
			    while ((line = br.readLine()) != null) {
			    	if(null!=line && !"".equals(line))
			    	{
			    		// convert full width chars to single width chars
//			    		line = Utilities.converFullwidth2HalfWidth(line);
			    		if(line.endsWith(";"))
			    		{
			    			line = line.substring(0, line.length()-1);
			    		}
			    		details = new VinDetails();
			    		// <MANUAL TYPE FOLDER NAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||<CARLINE CODE>||<WMI_CODE>||<VDS CODE>||<VIS START RANGE>||<VIS END RANGE>||<SOURCE ENT FILE NAME>
			    		tokens = line.split("\\|\\|");
			    		if(null!=tokens && tokens.length>0)
			    		{
			    			// tokens. length == 9 - all tokens exists
			    			if(tokens.length==9)
			    			{
			    				details.setAllTokensExists(true);
			    			}
			    			try
			    			{
			    				if(tokens.length>=0)
			    				{
			    					if(null!=tokens[0] && !"".equals(tokens[0]))
			    					{
			    						// set MANUAL TYPE FOLDER NAME
			    						details.setManualType(String.valueOf(tokens[0]));
			    					}
			    				}
			    			}
			    			catch (Exception e) {
			    			}

			    			try
			    			{
			    				if(tokens.length>=1)
			    				{
			    					if(null!=tokens[1] && !"".equals(tokens[1]))
			    					{
			    						// set FACE LIFT FOLDER NAME
			    						details.setFaceLiftFolder(String.valueOf(tokens[1]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=2)
			    				{
			    					if(null!=tokens[2] && !"".equals(tokens[2]))
			    					{
			    						// set MATERIAL FOLDER NAME
			    						details.setMaterialFolder(String.valueOf(tokens[2]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=3)
			    				{
			    					if(null!=tokens[3] && !"".equals(tokens[3]))
			    					{
			    						// set CARLINE CODE
			    						details.setCarlineCode(String.valueOf(tokens[3]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try
			    			{
			    				if(tokens.length>=4)
			    				{
			    					if(null!=tokens[4] && !"".equals(tokens[4]))
			    					{
			    						// set WMI CODE
			    						details.setWmiCode(String.valueOf(tokens[4]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}
			    			
			    			try{
			    				if(tokens.length>=5)
			    				{
			    					if(null!=tokens[5] && !"".equals(tokens[5]))
			    					{
			    						// set VDS CODE
			    						details.setVdsCode(String.valueOf(tokens[5]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=6)
			    				{
			    					if(null!=tokens[6] && !"".equals(tokens[6]))
			    					{
			    						// set VIS START RANGE
			    						details.setVisStartRange(String.valueOf(tokens[6]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    			try{
			    				if(tokens.length>=7)
			    				{
			    					if(null!=tokens[7] && !"".equals(tokens[7]))
			    					{
			    						// set VIS END RANGE
			    						details.setVisEndRange(String.valueOf(tokens[7]));
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}
			    			
			    			try{
			    				if(tokens.length>=8)
			    				{
			    					if(null!=tokens[8] && !"".equals(tokens[8]))
			    					{
			    						// set SOURCE DOCUMENT FILE NAME
			    						details.setDocSourceFileName(String.valueOf(tokens[8]));
			    						if(null!=details.getDocSourceFileName() && !"".equals(details.getDocSourceFileName()))
			    						{
			    							details.setDocSourceFileName(details.getDocSourceFileName().replace(";", ""));
			    							/*
			    							 * IDENTIFY PROCESSING FOLDER NAME
			    							 */
			    							if(details.getDocSourceFileName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.ent")))
			    							{
			    								// set as ent.gms3
			    								details.setProcessingFolder(ApplicationProperties.getProperty("directory.ent.dir"));
			    							}
			    							else if(details.getDocSourceFileName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.html")) || 
			    									details.getDocSourceFileName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.htm")))
			    							{
			    								// set as html
			    								details.setProcessingFolder(ApplicationProperties.getProperty("directory.html"));
			    							}
			    							else if(details.getDocSourceFileName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf")))
			    							{
			    								// set as pdf
			    								details.setProcessingFolder(ApplicationProperties.getProperty("directory.pdf"));
			    							}
			    							else if(details.getDocSourceFileName().trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.djvu")))
			    							{
			    								// set as djvu
			    								details.setProcessingFolder(ApplicationProperties.getProperty("directory.djvu"));
			    							}
			    						}
			    					}
			    				}
			    			}
			    			catch(Exception e){
			    			}

			    		}
			    		
			    		/*
			    		 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
			    		 * STARTING FROM LOCALE
			    		 */
			    		String filePath=null;
			    		if(null!=localeFolderName && !"".equals(localeFolderName) && 
			    				null!=modelFolderName && !"".equals(modelFolderName))
			    		{
			    			if(null!=details && null!=details.getManualType() && !"".equals(details.getManualType())
			    					&& null!=details.getFaceLiftFolder() && !"".equals(details.getFaceLiftFolder()) 
			    					&& null!=details.getMaterialFolder() && !"".equals(details.getMaterialFolder()) 
			    					&& null!=details.getCarlineCode() && !"".equals(details.getCarlineCode()) 
			    					&& null!=details.getWmiCode() && !"".equals(details.getWmiCode())
			    					&& null!=details.getVdsCode() && !"".equals(details.getVdsCode()) 
			    					&& null!=details.getVisStartRange() && !"".equals(details.getVisStartRange()) 
			    					&& null!=details.getVisEndRange() && !"".equals(details.getVisEndRange()) && 
			    					null!=details.getDocSourceFileName() && !"".equals(details.getDocSourceFileName()) && 
			    					null!=details.getProcessingFolder() && !"".equals(details.getProcessingFolder())) 
			    			{
			    				/*
			    				 * NO NEED OF ADDING ANY FILE PATH
			    				 * SET LINE TYPE AS VALID
			    				 */
			    				details.setLineType(LINE_TYPE_VALID);
			    				/*
			    				 * FOR NOW ADD UNTIL MATERIAL FOLDER NAME
			    				 * SEARCH FOR DOCUMENT ID WILL BE PERFORMED ON DOC SOURCE FILE PATH (USING LIKE OPERATOR)
			    				 * AND DOC FILE NAME AS PROCESSING FOLDER NAME IS MISSING IN THE VIN TEXT FILE
			    				 */
			    				filePath = localeFolderName+"\\"+modelFolderName+"\\"+details.getManualType()+"\\"+
			    						details.getFaceLiftFolder()+"\\"+details.getMaterialFolder()+"\\"+details.getProcessingFolder()+"\\"+details.getDocSourceFileName();
			    				details.setDocSourceFilePath(filePath);
			    				filePath = null;
			    			}
			    			else
			    			{
			    				details.setLineType(LINE_TYPE_INVALID);
			    			}
			    		}
			    		else
			    		{
			    			details.setLineType(LINE_TYPE_INVALID);
			    		}
			    		
			    		details.setVinSourceFileName(vinFile.getName());
			    		details.setVinSorceFilePath(PathUtil.winPath(vinFile));
			    		if(null==vinDetailsList || vinDetailsList.size()<=0)
		    			{
		    				vinDetailsList = new ArrayList<VinDetails>();
		    			}
		    			// add details to VIN LIST
		    			vinDetailsList.add(details);
			    		
			    		tokens=  null;
			    		details= null;
		    		}
			    }
			    line = null;
			   details = null;
			   tokens=null;
			   
			   if(null!=vinDetailsList && vinDetailsList.size()>0 && null!=fetchModelDetails && fetchModelDetails.equals("Y"))
			   {
				   // get carlineNameEnf & carlineNameReg for all VINs - use Master Locale
				   vinDetailsList=  MMEDocumentManagementDAO.getModelDetails(vinDetailsList, ApplicationProperties.getProperty("en-uk"));
			   }
			}
			else
			{
				logger.info("readSCMVINTextFileForMME :: SCM VIN Text file is either Corrupted or Does not Exist.");
				vinDetailsList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readSCMVINTextFileForMME()", e);
//			vinDetailsList = null;
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return vinDetailsList;
	}

	
	/**
	 * Function will read all the information from DISPLAY ORDER TEXT FILE
	 * @param displayOrderFile
	 * @param itemDetails
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<DisplayOrderDetails> readDisplayOrderTextFile(File displayOrderFile, ScheduleItemDetails itemDetails, String identifyName) throws IOException
	{
		BufferedReader br = null;
		ArrayList<DisplayOrderDetails> displayOrderList = null;
		try
		{
			/*
			 * RULES FOR DISPLAY ORDER
			 * WHEN NOT ENGINE AT / MT (FOR ALL MODEL TYPES & CHANNELS)
			 * 
			 * 	THEN LINE WILL BE - 
			 * 	Carline Folder Name||Manual Type||FaceLift Folder Name|| Material Code||Processing Folder||SIE FileName||Title||
			 * 	EngineType||Transmission Type||BodyType||DriveAxleType||Navigation LevelCode1||Navigation Level Code 2||
			 * 	Navigation Level code 3||..||Navigation Level code 5||Sequence Order Number
			 */
			
			if(null!=displayOrderFile && displayOrderFile.exists() && displayOrderFile.isFile())
			{
				String localeFolderName = displayOrderFile.getParentFile().getParentFile().getParentFile().getParentFile().getName();
			    br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(displayOrderFile.getAbsoluteFile()), "UTF-8"));
			    
			    String line;
			    String sequenceNo=null;
			    DisplayOrderDetails details = null;
			    String[] tokens=null;
			    String filePath=null;
			    while ((line = br.readLine()) != null) {
			    	if(null!=line && !"".equals(line))
			    	{
			    		// convert full width chars to single width chars
//			    		line = Utilities.converFullwidth2HalfWidth(line);
			    		if(line.endsWith(";"))
			    		{
			    			line = line.substring(0, line.length()-1);
			    		}


			    		// IDENTIFY SEQUENCE NO, IT WILL ALWAYS BE THE VALUE AFTER LAST INDEX OF ||
			    		sequenceNo="";
			    		if(line.lastIndexOf("||")!=-1)
			    		{
			    			sequenceNo = line.substring(line.lastIndexOf("||")+2, line.length());
			    			// Now remove the last Index from LINE, since it is already read
			    			line = line.substring(0, line.lastIndexOf("||"));
			    		}

			    		details = new DisplayOrderDetails();
			    		if(null!=sequenceNo && !"".equals(sequenceNo))
			    		{
			    			details.setSequenceNo(sequenceNo);
			    		}
			    		// <MODEL FOLDER NAME>||<MANUAL TYPE FOLDERNAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||
			    		//	<PROCESSING FOLDERNAME>||<FILENAME>||<TITLE>||EngineType||Transmission Type||BodyType||DriveAxleType||
			    		//	<DISPORD LEVEL 1 CODE>||<DISPORD LEVEL 2 CODE>|| <DISPORD LEVEL 3 CODE>||
			    		// <DISPORD LEVEL 4 CODE>||<DISPORD LEVEL 5 CODE>||<DISPORD LEVEL 6 CODE>
			    		if(null!=line && !"".equals(line))
			    		{
			    			tokens = line.split("\\|\\|");

			    			// CONTAINS ENGINE TYPE, MISSION TYPE, DRIVE AXLE TYPE , BODY TYPE
			    			if(null!=tokens && tokens.length>0)
			    			{
			    				try
			    				{
			    					if(tokens.length>=0)
			    					{
			    						if(null!=tokens[0] && !"".equals(tokens[0]))
			    						{
			    							// set MODEL FOLDER NAME
			    							details.setModelFolderName(String.valueOf(tokens[0]));
			    						}
			    					}
			    				}
			    				catch (Exception e) {
			    				}

			    				try
			    				{
			    					if(tokens.length>=1)
			    					{
			    						if(null!=tokens[1] && !"".equals(tokens[1]))
			    						{
			    							// set MANUAL TYPE FOLDER NAME
			    							details.setManualType(String.valueOf(tokens[1]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try
			    				{
			    					if(tokens.length>=2)
			    					{
			    						if(null!=tokens[2] && !"".equals(tokens[2]))
			    						{
			    							// set FACE LIFT FOLDER NAME
			    							details.setFaceLiftFolderName(String.valueOf(tokens[2]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try
			    				{
			    					if(tokens.length>=3)
			    					{
			    						if(null!=tokens[3] && !"".equals(tokens[3]))
			    						{
			    							// set MATERIAL FOLDER NAME
			    							details.setMaterialFolderName(String.valueOf(tokens[3]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try
			    				{
			    					if(tokens.length>=4)
			    					{
			    						if(null!=tokens[4] && !"".equals(tokens[4]))
			    						{
			    							// set PROCESSING FOLDER NAME
			    							details.setProcessingFolderName(String.valueOf(tokens[4]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=5)
			    					{
			    						if(null!=tokens[5] && !"".equals(tokens[5]))
			    						{
			    							// set FILE NAME
			    							details.setFileName(String.valueOf(tokens[5]).trim()); // trimmed: names are listed with a trailing space at times (MNAO e-WD)
			    							/*
				    			    		 * INCIDENT CHANGE - Step1.2 MC market_10022020_OEM - IN-201013-1680
				    			    		 * DO THIS ONLY FOR MODEL 
				    			    		 */
				    			    		details.setFileName(Utilities.replaceJunkCharacterToFullbyte(details.getFileName(), details.getModelFolderName(), details.getManualType(), 
				    			    				details.getFaceLiftFolderName(), details.getMaterialFolderName(), localeFolderName));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}
			    				
			    				try{
			    					if(tokens.length>=6)
			    					{
			    						if(null!=tokens[6] && !"".equals(tokens[6]))
			    						{
			    							// set TITLE
			    							details.setTitle(String.valueOf(tokens[6]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=7)
			    					{
			    						if(null!=tokens[7] && !"".equals(tokens[7]))
			    						{
			    							// set ENGINE TYPE
			    							details.setEngineType(String.valueOf(tokens[7]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=8)
			    					{
			    						if(null!=tokens[8] && !"".equals(tokens[8]))
			    						{
			    							// set MISSION TYPE
			    							details.setMissionType(String.valueOf(tokens[8]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=9)
			    					{
			    						if(null!=tokens[9] && !"".equals(tokens[9]))
			    						{
			    							// set BODY TYPE
			    							details.setBodyType(String.valueOf(tokens[9]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=10)
			    					{
			    						if(null!=tokens[10] && !"".equals(tokens[10]))
			    						{
			    							// set DRIVE AXLE TYPE
			    							details.setDriveAxleType(String.valueOf(tokens[10]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=11)
			    					{
			    						if(null!=tokens[11] && !"".equals(tokens[11]))
			    						{
			    							// set DISPLAY ORDER LEVEL 1 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel1Name(String.valueOf(tokens[11]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=12)
			    					{
			    						if(null!=tokens[12] && !"".equals(tokens[12]))
			    						{
			    							// set DISPLAY ORDER LEVEL 2 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel2Name(String.valueOf(tokens[12]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=13)
			    					{
			    						if(null!=tokens[13] && !"".equals(tokens[13]))
			    						{
			    							// set DISPLAY ORDER LEVEL 3 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel3Name(String.valueOf(tokens[13]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=14)
			    					{
			    						if(null!=tokens[14] && !"".equals(tokens[14]))
			    						{
			    							// set DISPLAY ORDER LEVEL 4 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel4Name(String.valueOf(tokens[14]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=15)
			    					{
			    						if(null!=tokens[15] && !"".equals(tokens[15]))
			    						{
			    							// set DISPLAY ORDER LEVEL 5 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel5Name(String.valueOf(tokens[15]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}

			    				try{
			    					if(tokens.length>=16)
			    					{
			    						if(null!=tokens[16] && !"".equals(tokens[16]))
			    						{
			    							// set DISPLAY ORDER LEVEL 6 NAME (the file carries names; codes are no longer supplied)
			    							details.setDisplayOrderLevel6Name(String.valueOf(tokens[16]));
			    						}
			    					}
			    				}
			    				catch(Exception e){
			    				}
			    			}
			    			tokens=  null;
			    		}

			    		/*
			    		 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
			    		 * STARTING FROM LOCALE
			    		 */
			    		filePath=null;
			    		if(null!=localeFolderName && !"".equals(localeFolderName))
			    		{
			    			if(null!=details && null!=details.getModelFolderName() && !"".equals(details.getModelFolderName())
			    					&& null!=details.getManualType() && !"".equals(details.getManualType()) && 
			    					null!=details.getFaceLiftFolderName() && !"".equals(details.getFaceLiftFolderName()) 
			    					&& null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName()) 
			    					&& null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) 
			    					&& null!=details.getFileName() && !"".equals(details.getFileName()))
			    			{
			    				filePath = localeFolderName+"\\"+details.getModelFolderName()+"\\"+details.getManualType()+"\\"+details.getFaceLiftFolderName()+"\\"+details.getMaterialFolderName();
			    				filePath =filePath+"\\"+details.getProcessingFolderName()+"\\"+details.getFileName();
			    			}
			    		}

			    		if(null!=filePath && !"".equals(filePath))
			    		{
			    			details.setLineType(LINE_TYPE_VALID);
			    			details.setFilePath(filePath);
			    		}
			    		else
			    		{
			    			details.setLineType(LINE_TYPE_INVALID);
			    		}

			    		details.setDisplayOrderSourceFileName(displayOrderFile.getName());
			    		details.setDisplayOrderSourceFilePath(PathUtil.winPath(displayOrderFile));
			    		if(null==displayOrderList || displayOrderList.size()<=0)
			    		{
			    			displayOrderList = new ArrayList<DisplayOrderDetails>();
			    		}
			    		// add details to ESI CATEGORY LIST
			    		displayOrderList.add(details);
			    		details= null;
			    		filePath = null;
			    	}
			    }
			    line = null;
			    details =null;
			    tokens=null;
			    filePath = null;
			    sequenceNo = null;
			   /*
			    * identify Display Order Names 
			    */
			    if(null!=displayOrderList && displayOrderList.size()>0)
			    {
			    	displayOrderList = MCDocumentManagementDAO.getDisplayOrderNameForDisplayOrderProcessing(displayOrderList, itemDetails.getModelType(), localeFolderName, identifyName);
			    }
			    localeFolderName = null;
			}
			else
			{
				logger.info("readDisplayOrderTextFile :: DISPLAY ORDER Text file is either Corrupted or Does not Exist.");
				displayOrderList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readDisplayOrderTextFile()", e);
//			displayOrderList = null;
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return displayOrderList;
	}
	
	/**
	 * Function will read all the information from DISPLAY ORDER TEXT FILE ONLY FOR ENGINE / AT / MT
	 * @param displayOrderFile
	 * @param modelType
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<DisplayOrderDetails> readDisplayOrderTextFileForEngineATMT(File displayOrderFile,String modelType,String identifyName) throws IOException
	{
		BufferedReader br = null;
		ArrayList<DisplayOrderDetails> displayOrderList = null;
		try
		{
			/*
			 * DISPLAY ORDER FORMAT
			 * 	Carline Folder Name||Manual Type||FaceLift Folder Name|| Material Code||Processing Folder||SIE FileName||Title||
			 * 	Navigation LevelCode1||Navigation Level Code 2||
			 * 	Navigation Level code 3||..||Navigation Level code 5||Sequence Order Number
			 */
			
			if(null!=displayOrderFile && displayOrderFile.exists() && displayOrderFile.isFile())
			{
				String localeFolderName = displayOrderFile.getParentFile().getParentFile().getParentFile().getParentFile().getName();
			    br = new BufferedReader(new InputStreamReader(PathUtil.fileInputStream(displayOrderFile.getAbsoluteFile()), "UTF-8"));
			    
			    String line;
			    String sequenceNo=null;
			    DisplayOrderDetails details = new DisplayOrderDetails();
			    String[] tokens = null;
			    String filePath = null;
			    while ((line = br.readLine()) != null) {
			    	if(null!=line && !"".equals(line))
			    	{
			    		// convert full width chars to single width chars
//			    		line = Utilities.converFullwidth2HalfWidth(line);
			    		if(line.endsWith(";"))
			    		{
			    			line = line.substring(0, line.length()-1);
			    		}

			    		sequenceNo="";
			    		// IDENTIFY SEQUENCE NO, IT WILL ALWAYS BE THE VALUE AFTER LAST INDEX OF ||
			    		if(line.lastIndexOf("||")!=-1)
			    		{
			    			sequenceNo = line.substring(line.lastIndexOf("||")+2, line.length());
			    			// Now remove the last Index from LINE, since it is already read
			    			line = line.substring(0, line.lastIndexOf("||"));
			    		}

			    		details = new DisplayOrderDetails();
			    		if(null!=sequenceNo && !"".equals(sequenceNo))
			    		{
			    			details.setSequenceNo(sequenceNo);
			    		}
			    		// <MODEL FOLDER NAME>||<MANUAL TYPE FOLDERNAME>||<FC FOLDERNAME>||<MATERIAL FOLDERNAME>||
			    		//	<PROCESSING FOLDERNAME>||<FILENAME>||<DISPORD LEVEL 1 CODE>||<DISPORD LEVEL 2 CODE>|| <DISPORD LEVEL 3 CODE>||
			    		// <DISPORD LEVEL 4 CODE>||<DISPORD LEVEL 5 CODE>||<DISPORD LEVEL 6 CODE>
			    		if(null!=line && !"".equals(line))
			    		{
			    			tokens = line.split("\\|\\|");
			    			if(null!=tokens && tokens.length>0)
			    			{
			    				// WITHOUT ENGINE TYPE, MISSION TYPE, DRIVE AXLE AND BODY TYPE
			    				if(null!=tokens && tokens.length>0)
			    				{
			    					try
				    				{
				    					if(tokens.length>=0)
				    					{
				    						if(null!=tokens[0] && !"".equals(tokens[0]))
				    						{
				    							// set MODEL FOLDER NAME
				    							details.setModelFolderName(String.valueOf(tokens[0]));
				    						}
				    					}
				    				}
				    				catch (Exception e) {
				    				}

				    				try
				    				{
				    					if(tokens.length>=1)
				    					{
				    						if(null!=tokens[1] && !"".equals(tokens[1]))
				    						{
				    							// set MANUAL TYPE FOLDER NAME
				    							details.setManualType(String.valueOf(tokens[1]));
				    						}
				    					}
				    				}
				    				catch(Exception e){
				    				}

			    					try
			    					{
			    						if(tokens.length>=2)
			    						{
			    							if(null!=tokens[2] && !"".equals(tokens[2]))
			    							{
			    								// set FACE LIFT FOLDER NAME
			    								details.setFaceLiftFolderName(String.valueOf(tokens[2]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try
			    					{
			    						if(tokens.length>=3)
			    						{
			    							if(null!=tokens[3] && !"".equals(tokens[3]))
			    							{
			    								// set MATERIAL FOLDER NAME
			    								details.setMaterialFolderName(String.valueOf(tokens[3]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try
			    					{
			    						if(tokens.length>=4)
			    						{
			    							if(null!=tokens[4] && !"".equals(tokens[4]))
			    							{
			    								// set PROCESSING FOLDER NAME
			    								details.setProcessingFolderName(String.valueOf(tokens[4]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=5)
			    						{
			    							if(null!=tokens[5] && !"".equals(tokens[5]))
			    							{
			    								// set FILE NAME
			    								details.setFileName(String.valueOf(tokens[5]).trim()); // trimmed: names are listed with a trailing space at times (MNAO e-WD)
			    								/*
					    			    		 * INCIDENT CHANGE - Step1.2 MC market_10022020_OEM - IN-201013-1680
					    			    		 * DO THIS ONLY FOR MODEL 
					    			    		 */
					    			    		details.setFileName(Utilities.replaceJunkCharacterToFullbyte(details.getFileName(), details.getModelFolderName(), details.getManualType(), 
					    			    				details.getFaceLiftFolderName(), details.getMaterialFolderName(), localeFolderName));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}
			    					
			    					try{
			    						if(tokens.length>=6)
			    						{
			    							if(null!=tokens[6] && !"".equals(tokens[6]))
			    							{
			    								// set TITLE
			    								details.setTitle(String.valueOf(tokens[6]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=7)
			    						{
			    							if(null!=tokens[7] && !"".equals(tokens[7]))
			    							{
			    								// set DISPLAY ORDER LEVEL 1 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel1Name(String.valueOf(tokens[7]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=8)
			    						{
			    							if(null!=tokens[8] && !"".equals(tokens[8]))
			    							{
			    								// set DISPLAY ORDER LEVEL 2 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel2Name(String.valueOf(tokens[8]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=9)
			    						{
			    							if(null!=tokens[9] && !"".equals(tokens[9]))
			    							{
			    								// set DISPLAY ORDER LEVEL 3 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel3Name(String.valueOf(tokens[9]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=10)
			    						{
			    							if(null!=tokens[10] && !"".equals(tokens[10]))
			    							{
			    								// set DISPLAY ORDER LEVEL 4 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel4Name(String.valueOf(tokens[10]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=11)
			    						{
			    							if(null!=tokens[11] && !"".equals(tokens[11]))
			    							{
			    								// set DISPLAY ORDER LEVEL 5 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel5Name(String.valueOf(tokens[11]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}

			    					try{
			    						if(tokens.length>=12)
			    						{
			    							if(null!=tokens[12] && !"".equals(tokens[12]))
			    							{
			    								// set DISPLAY ORDER LEVEL 6 NAME (the file carries names; codes are no longer supplied)
			    								details.setDisplayOrderLevel6Name(String.valueOf(tokens[12]));
			    							}
			    						}
			    					}
			    					catch(Exception e){
			    					}
			    				}
			    			}
			    			tokens=  null;
			    		}

			    		/*
			    		 * CREATE FILE PATH AND FILE ABSOLUTE PATH FOR EACH ROW.
			    		 * STARTING FROM LOCALE
			    		 */
			    		filePath=null;
			    		if(null!=localeFolderName && !"".equals(localeFolderName))
			    		{
			    			if(null!=details && null!=details.getModelFolderName() && !"".equals(details.getModelFolderName())
			    					&& null!=details.getManualType() && !"".equals(details.getManualType()) && 
			    					null!=details.getFaceLiftFolderName() && !"".equals(details.getFaceLiftFolderName()) 
			    					&& null!=details.getMaterialFolderName() && !"".equals(details.getMaterialFolderName()) 
			    					&& null!=details.getProcessingFolderName() && !"".equals(details.getProcessingFolderName()) 
			    					&& null!=details.getFileName() && !"".equals(details.getFileName()))
			    			{
			    				filePath = localeFolderName+"\\"+details.getModelFolderName()+"\\"+details.getManualType()+"\\"+details.getFaceLiftFolderName()+"\\"+details.getMaterialFolderName();
			    				filePath =filePath+"\\"+details.getProcessingFolderName()+"\\"+details.getFileName();
			    			}
			    		}

			    		if(null!=filePath && !"".equals(filePath))
			    		{
			    			details.setLineType(LINE_TYPE_VALID);
			    			details.setFilePath(filePath);
			    		}
			    		else
			    		{
			    			details.setLineType(LINE_TYPE_INVALID);
			    		}

			    		details.setDisplayOrderSourceFileName(displayOrderFile.getName());
			    		details.setDisplayOrderSourceFilePath(PathUtil.winPath(displayOrderFile));
			    		if(null==displayOrderList || displayOrderList.size()<=0)
			    		{
			    			displayOrderList = new ArrayList<DisplayOrderDetails>();
			    		}
			    		// add details to ESI CATEGORY LIST
			    		displayOrderList.add(details);
			    		details= null;
			    		filePath = null;
			    	}
			    }
			    line = null;
			    details = null;
			    tokens=null;
			    filePath = null;
			    sequenceNo = null;
			    /*
			     * identify Display Order Names 
			     */
			    if(null!=displayOrderList && displayOrderList.size()>0)
			    {
			    	displayOrderList = MCDocumentManagementDAO.getDisplayOrderNameForDisplayOrderProcessing(displayOrderList, modelType, localeFolderName, identifyName);
			    }
			   localeFolderName= null;
			}
			else
			{
				logger.info("readDisplayOrderTextFile :: DISPLAY ORDER Text file is either Corrupted or Does not Exist.");
				displayOrderList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readDisplayOrderTextFile()", e);
//			displayOrderList = null;
		}
		finally
		{
			if(null!=br)
			{
				br.close();
			}
		}
		return displayOrderList;
	}
	
	/**
	 * Function will add User Groups to Content Details Object
	 * @param contentDetails
	 */
	public static ArrayList<UserGroupDetails> addUserGroupsToContent(String market, ArrayList<UserGroupDetails> groupsList)
	{
		try
		{
			String labels="";
			String refKeys="";
			String guids="";
			
			if(null!=market && market.trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mnao").trim().toLowerCase()))
			{
				labels=ApplicationProperties.getProperty("USERGROUPS_LABELS");
				refKeys = ApplicationProperties.getProperty("USERGROUPS_REF_KEYS");
				guids = ApplicationProperties.getProperty("USERGROUPS_GUIDS");
			}
			else if(null!=market && market.trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mc").trim().toLowerCase()))
			{
				labels=ApplicationProperties.getProperty("USERGROUPS_MC_LABELS");
				refKeys = ApplicationProperties.getProperty("USERGROUPS_MC_REF_KEYS");
				guids = ApplicationProperties.getProperty("USERGROUPS_MC_GUIDS");
			}
			else if(null!=market && market.trim().toLowerCase().equals(ApplicationProperties.getProperty("market.mme").trim().toLowerCase()))
			{
				labels=ApplicationProperties.getProperty("USERGROUPS_MME_LABELS");
				refKeys = ApplicationProperties.getProperty("USERGROUPS_MME_REF_KEYS");
				guids = ApplicationProperties.getProperty("USERGROUPS_MME_GUIDS");
			}
			
			StringTokenizer str = new StringTokenizer(labels,",");
			StringTokenizer strVal = new StringTokenizer(refKeys,",");
			StringTokenizer strGuid = new StringTokenizer(guids,",");
			UserGroupDetails ugDetails = new UserGroupDetails();
			while(str.hasMoreTokens() && strVal.hasMoreTokens() && strGuid.hasMoreTokens())
			{
				ugDetails = new UserGroupDetails();
				ugDetails.setUserGroupName(str.nextToken());
				ugDetails.setUserGroupRefKey(strVal.nextToken());
				ugDetails.setUserGroupGuid(strGuid.nextToken());
				// add ugDetails to userGroupList
				if(null==groupsList || groupsList.size()<=0)
				{
					groupsList = new ArrayList<UserGroupDetails>();
				}
				groupsList.add(ugDetails);
				ugDetails = null;
			}
			str = null;
			strVal=  null;
			strGuid=  null;
			ugDetails = null;
			labels=null;
			refKeys=  null;
			guids= null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "addUserGroupsToContent()", e);
		}
		return groupsList;
	}
	
	/**
	 * Function will identify TITLE & ESI CATEGORIES FOR A DOCUMENT.
	 * @param contentDetails
	 * @param esiCategoryDetailsList
	 * @return
	 */
	public static ContentDetails identifyAttributesFromESICategoryDetails(ContentDetails contentDetails, ArrayList<ESICategoryDetails> esiCategoryDetailsList, String processingFolderName)
	{
		try
		{
			if(null!=contentDetails && null!=contentDetails.getFilePath() && !"".equals(contentDetails.getFilePath()))
			{
				if(null!=esiCategoryDetailsList && esiCategoryDetailsList.size()>0)
				{
					for(int i=0;i<esiCategoryDetailsList.size();i++)
					{
						ESICategoryDetails details = (ESICategoryDetails) esiCategoryDetailsList.get(i);
						if(null!=details && null!=details.getLineType() && details.getLineType().equals(LINE_TYPE_VALID) && 
								null!=details.getFilePath() && !"".equals(details.getFilePath()))
						{
							/*
							 *  IF CHANNEL IS WIRING DIAGRAM AND THE MATERIAL FOLDER NAME IN SOURCE CONTENT DETAILS
							 *  DOES NOT CONTAIN ANY FILE EXTENSION E.G. HTML/DJVU/PDF
							 *  	FOR THESE CASES ESI CAT TEXT FILE WILL NOT HAVE ANY EXTENSIONS
							 *  THEN REMOVE EXTENSION ELSE CHECK WITH EXTENSION 
							 */
							String keyToCheck = contentDetails.getFilePath();
							
							/*
							 * THIS VARIABLE FOR COVERING 
							 * THE WD CHANNEL - (MATERIAL FOLDER HAVING BOTH HTML & PDF, 
							 * 	BUT ESI CAT HAVING VALUES FOR PFDS instead of HTML)
							 */
							String anotherKeyToCheck = "";
							
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
							
							
							if(removeExt==true && (null!=contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName())))
							{
								if(contentDetails.getChannelName().trim().toLowerCase().equals(
										ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME").trim().toLowerCase()))
								{
									String tok="";
									if(null!=contentDetails.getMaterialName() && !"".equals(contentDetails.getMaterialName()))
									{
										if(contentDetails.getMaterialName().lastIndexOf("_")!=-1)
										{
											tok= contentDetails.getMaterialName().substring(contentDetails.getMaterialName().lastIndexOf("_")+1, contentDetails.getMaterialName().length());
										}
									}
									if(null==tok)
									{
										tok="";
									}
									// CHECK TOK IS NOT HTML / PDF / DJVU / XML - THEN REMOVE EXTENSION
									if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
										!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
										!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
										!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
									{
										// REMOVE EXTENSION - NOT REQUIRED HERE SINCE IN THE FILE FILE, EXTENSION IS ALREADY REMOVED
//										if(keyToCheck.lastIndexOf(".")!=-1)
//										{
//											keyToCheck = keyToCheck.substring(0, keyToCheck.lastIndexOf("."));
//										}
										
										// set ANOTHER KEY TO CHECK
										anotherKeyToCheck = keyToCheck.replace("\\html\\", "\\pdf\\");;
										
										/*
										 * ALSO HERE CHANGE THE PROCESSING FOLDER NAME IN KEY TO CHECK TO HTML
										 */
										keyToCheck = keyToCheck.replace("\\pdf\\", "\\html\\");
									}
									tok = null;
								}
							}
							
							if((keyToCheck.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase())) 
									|| (null!=anotherKeyToCheck && anotherKeyToCheck.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase())))
							{
								// FILE PATH MATCHED - SET TITLE & ESI CATEGORIES
								contentDetails.setTitle(details.getTitle());
								contentDetails.setCategoryCode(details.getCategoryLevel1Code());
								contentDetails.setSubCategoryCode(details.getCategoryLevel2Code());
								contentDetails.setSubSubCategoryCode(details.getCategoryLevel3Code());
								
								// set ESICATEGORY SOURCE FILE NAME & PATH
								contentDetails.setEsiCategoryMappedFileName(details.getEsiCategorySourceFileName());
								contentDetails.setEsiCategoryMappedFilePath(details.getEsiCategorySourceFilePath());
								
								// ALSO SET ENTRY FOUND IN ESI CAT TO YES
								contentDetails.setEntryFoundInEsiCat("YES");
								
								// SET STEERING TYPE INFO IN CONTENT DETAILS IF AVAILABLE - APPLICABLE INCASE OF WIRING DIAGRAMS ONLY
								if(null!=details.getSteeringTypeInfoText() && !"".equals(details.getSteeringTypeInfoText()))
								{
									contentDetails.setEsiSteeringTypeInfo(details.getSteeringTypeInfoText());
								}
								break;
							}
							keyToCheck = null;
							anotherKeyToCheck = null;
						}
						details = null;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "identifyAttributesFromESICategoryDetails()", e);
		}
		return contentDetails;
	}
	
	/**
	 * Function will identify All the Applicable Display Orders for a Document
	 * @param contentDetails
	 * @param allDisplayOrderList
	 * @return
	 */
	public static ContentDetails identifyAttributesFromDisplayOrderDetails1(ContentDetails contentDetails, ArrayList<DisplayOrderDetails> allDisplayOrderList, String processingFolderName)
	{
		try
		{
			if(null!=contentDetails && null!=contentDetails.getFilePath() && !"".equals(contentDetails.getFilePath()))
			{
				contentDetails.setApplicableDisplayOrderList(new ArrayList<DisplayOrderDetails>());
				if(null!=allDisplayOrderList && allDisplayOrderList.size()>0)
				{
					for(int i=0;i<allDisplayOrderList.size();i++)
					{
						DisplayOrderDetails details = (DisplayOrderDetails) allDisplayOrderList.get(i);
						if(null!=details && null!=details.getLineType() && details.getLineType().equals(LINE_TYPE_VALID) && 
								null!=details.getFilePath() && !"".equals(details.getFilePath()))
						{
							// IF CHANNEL IS WIRING DIAGRAM - REMOVE THE EXTENSION FROM FILE PATH & CHECK
							String keyToCheck = contentDetails.getFilePath();
							
							/*
							 * THIS VARIABLE FOR COVERING 
							 * THE WD CHANNEL - (MATERIAL FOLDER HAVING BOTH HTML & PDF, 
							 * 	BUT ESI CAT HAVING VALUES FOR PFDS instead of HTML)
							 */
							String anotherKeyToCheck = "";
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
							if(removeExt==true && null!=contentDetails.getChannelName() && !"".equals(contentDetails.getChannelName()))
							{
								if(contentDetails.getChannelName().trim().toLowerCase().equals(
										ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME").trim().toLowerCase()))
								{
									String tok="";
									if(null!=contentDetails.getMaterialName() && !"".equals(contentDetails.getMaterialName()))
									{
										if(contentDetails.getMaterialName().lastIndexOf("_")!=-1)
										{
											tok= contentDetails.getMaterialName().substring(contentDetails.getMaterialName().lastIndexOf("_")+1, contentDetails.getMaterialName().length());
										}
									}
									if(null==tok)
									{
										tok="";
									}
									// CHECK TOK IS NOT HTML / PDF / DJVU / XML - THEN REMOVE EXTENSION
									if(!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.html")) && 
										!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.xml")) &&
										!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.pdf")) && 
										!tok.trim().toLowerCase().equals(ApplicationProperties.getProperty("directory.djvu")))
									{
										// REMOVE EXTENSION - NOT REQUIRED HERE SINCE IN THE FILE FILE, EXTENSION IS ALREADY REMOVED
//										if(keyToCheck.lastIndexOf(".")!=-1)
//										{
//											keyToCheck = keyToCheck.substring(0, keyToCheck.lastIndexOf("."));
//										}
										
										anotherKeyToCheck = keyToCheck.replace("\\html\\", "\\pdf\\");
										
										/*
										 * ALSO HERE CHANGE THE PROCESSING FOLDER NAME IN KEY TO CHECK TO HTML
										 */
										keyToCheck = keyToCheck.replace("\\pdf\\", "\\html\\");
									}
									tok = null;
								}
							}
							
							if((keyToCheck.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase())) || 
									 (null!=anotherKeyToCheck && anotherKeyToCheck.trim().toLowerCase().equals(details.getFilePath().trim().toLowerCase())))
							{
								// FILE PATH MATCHED - ADD TO CONTENT DETAILS DISPLAY ORDER LIST (MULTIPLE ROWS CAN BE FOUND)
								contentDetails.getApplicableDisplayOrderList().add(details);
							}
							keyToCheck = null;
						}
						details = null;
					}
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "identifyAttributesFromDisplayOrderDetails()", e);
		}
		return contentDetails;
	}
	
	/**
	 * Function will Identify the Applicable VINs for a Document on the basis of Mapped Display Order
	 * @param contentDetails
	 * @param allVINList
	 * @return
	 */
	public static ContentDetails identifyApplicableVINForDocument(ContentDetails contentDetails, ArrayList<VinDetails> allVINList)
	{
		try
		{
			// set Applicable VIN List to New List
			contentDetails.setApplicableVINList(new ArrayList<VinDetails>());
			if(null!=contentDetails && null!=contentDetails.getApplicableDisplayOrderList() && contentDetails.getApplicableDisplayOrderList().size()>0 && null!=allVINList && allVINList.size()>0)
			{
				for(VinDetails vins : allVINList)
				{
					if(null!=vins.getDisplayOrderTextFileName() && !"".equals(vins.getDisplayOrderTextFileName()))
					{
						for(DisplayOrderDetails dispOrdDetails : contentDetails.getApplicableDisplayOrderList())
						{
							if(null!=dispOrdDetails.getDisplayOrderSourceFileName() && !"".equals(dispOrdDetails.getDisplayOrderSourceFileName()))
							{
								if(dispOrdDetails.getDisplayOrderSourceFileName().trim().toLowerCase().equals(vins.getDisplayOrderTextFileName().trim().toLowerCase()))
								{
									// add VINS to applicableVINList in contentDetails
									contentDetails.getApplicableVINList().add(vins);
									break;
								}
							}
							dispOrdDetails = null;
						}
					}
					vins= null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "identifyApplicableVINForDocument()", e);
		}
		return contentDetails;
	}
	
	/**
	 * Function will identify Manual Type as Category for the Document
	 * @param manualTypeCode
	 * @return
	 */
	public static String identifyManualTypeAsCateogry(String manualTypeCode)
	{
		String categoryToBeAdded="";
		if(null!=manualTypeCode && !"".equals(manualTypeCode))
		{
			if(manualTypeCode.trim().toLowerCase().equals("bsm"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_SM_TYPE_BSM_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("sh"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_SM_TYPE_SH_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("sm"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_SM_TYPE_WM_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("wm"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_SM_TYPE_WM_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("at"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_SM_TYPE_AT_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("mt"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_SM_TYPE_MT_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("engine"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("eng"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_SM_TYPE_ENGINE_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("im"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_SM_TYPE_IM_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("wm&sh"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_SM_TYPE_WM_AND_SH_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("mc"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_SM_TYPE_MC_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("tqg"))
			{
				categoryToBeAdded= ApplicationProperties.getProperty("GMS3_SM_TYPE_TQG_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("tsm"))
			{
				categoryToBeAdded= ApplicationProperties.getProperty("GMS3_SM_TYPE_TSM_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("rq"))
			{
				categoryToBeAdded= ApplicationProperties.getProperty("GMS3_SM_TYPE_RQ_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("dm"))
			{
				categoryToBeAdded= ApplicationProperties.getProperty("GMS3_SM_TYPE_DM_REF_KEY");
			}
			else if(manualTypeCode.trim().toLowerCase().equals("mcm"))
			{
				categoryToBeAdded= ApplicationProperties.getProperty("GMS3_SM_TYPE_MCM_REF_KEY");
			}
		}
		return categoryToBeAdded;
	}

	/**
	 * Function will identify Other Manual Types as Category for the Document
	 * @param modelType
	 * @return
	 */
	public static String identifyOtherManualTypeAsCateogry(String modelType)
	{
		String categoryToBeAdded="";
		if(null!=modelType && !"".equals(modelType))
		{
			if(modelType.trim().toLowerCase().equals("oldm"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_OSM_TYPE_OLDM_REF_KEY");
			}
			else if(modelType.trim().toLowerCase().equals("nissan"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_OSM_TYPE_NISSAN_REF_KEY");
			}
			else if(modelType.trim().toLowerCase().equals("suzuki"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_OSM_TYPE_SUZUKI_REF_KEY");
			}
			else if(modelType.trim().toLowerCase().equals("isuzu"))
			{
				categoryToBeAdded = ApplicationProperties.getProperty("GMS3_OSM_TYPE_ISUZU_REF_KEY");
			}
		}
		return categoryToBeAdded;
	}
	
	
	/**
	 * Function will replace the Images Path in the Content for All Channels
	 * @param contentData
	 * @param contentVO
	 * @return
	 */
	public static String replaceOkAssetsImagesSrcContent(String contentData,ContentDetails contentVO) 
	{
		try
		{
			if (null != contentData && !"".equals(contentData)) 
			{
				String channelFolderName="";
				if(null!=contentVO.getChannelName() && 
						contentVO.getChannelName().equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME")))
				{
					channelFolderName =  ApplicationProperties.getProperty("wiring.diagram.folder.label");
				}
				else if(null!=contentVO.getChannelName() && 
						contentVO.getChannelName().equals(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME")))
				{
					channelFolderName = ApplicationProperties.getProperty("service.manuals.folder.label");
				}
				else if(null!=contentVO.getChannelName() && 
						contentVO.getChannelName().equals(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME")))
				{
					channelFolderName = ApplicationProperties.getProperty("other.service.manuals.folder.label");
				}
				
				String localeFolderName = ApplicationProperties.getProperty(contentVO.getLocale().trim().toLowerCase());
				 
				/*
				 * /library/MAZDAESI/Service
				 * Manuals/en_us/image/ac5uuw00000002.gif this needs to be
				 * replaced with /library/MAZDAESI/Service
				 * Manuals/en_us/<year>/<model>/image/ac5uuw00000002.gif
				 */
				// PREPARE CHANNEL REF KEY
				String channelFolderRefKey = channelFolderName;
				channelFolderRefKey = channelFolderRefKey.replace(" ", "_");
				
				String pathToBeReplaced = "/"	+ ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
				// // add repository
				pathToBeReplaced = pathToBeReplaced	+ ApplicationProperties.getProperty("REPOSITORY").toUpperCase() + "/";
				// add channel Name
				pathToBeReplaced = pathToBeReplaced + channelFolderRefKey.toUpperCase() + "/";
				// add locale Folder Name
				pathToBeReplaced = pathToBeReplaced + localeFolderName.toLowerCase() + "/";
				channelFolderRefKey = null;
				/*
				 * ADD MODEL AND YEAR FOLDER NAME ONLY WHEN WIRING DIAGRAMS
				 */
				if(!channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("service.manuals.folder.label").trim().toLowerCase()) 
						&& !channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("other.service.manuals.folder.label").trim().toLowerCase()))
				{
					// add Model Folder Name
					if(null!=contentVO.getModelFolderName() && !"".equals(contentVO.getModelFolderName()))
					{
						pathToBeReplaced = pathToBeReplaced + contentVO.getModelFolderName().toLowerCase() + "/";
					}
					
					// add Manual Type Folder Name
					if(null!=contentVO.getManualType() && !"".equals(contentVO.getManualType()))
					{
						pathToBeReplaced = pathToBeReplaced+ contentVO.getManualType().trim().toLowerCase()+"/";
					}
					// add FaceLift Folder
					if(null!=contentVO.getFaceLiftFolderName() && !"".equals(contentVO.getFaceLiftFolderName()))
					{
						pathToBeReplaced = pathToBeReplaced+ contentVO.getFaceLiftFolderName().trim().toLowerCase()+"/";
					}
					// add materialFolder
					if(null!=contentVO.getMaterialName() && !"".equals(contentVO.getMaterialName()))
					{
						pathToBeReplaced = pathToBeReplaced  + contentVO.getMaterialName().toLowerCase() + "/";
					}
				}

				// add image directory name
				pathToBeReplaced = pathToBeReplaced + "image";
				logger.info("replaceOkAssetsImagesSrcContent :: Path To Be Reaplced :: > " + pathToBeReplaced);
				org.jsoup.nodes.Document doc = Jsoup.parse(contentData);
				org.jsoup.select.Elements imagesTagsList = doc.select("img");
				if (null != imagesTagsList && imagesTagsList.size() > 0) {
					for (int i = 0; i < imagesTagsList.size(); i++) {
						String srcValue = imagesTagsList.get(i).attr("src");
						if (null != srcValue && !"".equals(srcValue)
								&& !"".equals(srcValue.trim()) && !srcValue.startsWith("#")  
								&& !srcValue.trim().toLowerCase().startsWith("http://")  
								&& !srcValue.trim().toLowerCase().startsWith("https://")) 
						{
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
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "replaceOkAssetsImagesSrcContent()", e);
		}
		return contentData;
	}
	
	/**
	 * Function will replace PDF Paths in the Content for All Channels
	 * @param contentData
	 * @param contentVO
	 * @return
	 */
	public static String replaceOKAssetsPdfPathsInContent(String contentData,ContentDetails contentVO) 
	{
		try
		{
			if (null != contentData && !"".equals(contentData)) 
			{
				String channelFolderName="";
				if(null!=contentVO.getChannelName() && 
						contentVO.getChannelName().equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME")))
				{
					channelFolderName =  ApplicationProperties.getProperty("wiring.diagram.folder.label");
				}
				else if(null!=contentVO.getChannelName() && 
						contentVO.getChannelName().equals(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME")))
				{
					channelFolderName = ApplicationProperties.getProperty("service.manuals.folder.label");
				}
				else if(null!=contentVO.getChannelName() && 
						contentVO.getChannelName().equals(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME")))
				{
					channelFolderName = ApplicationProperties.getProperty("other.service.manuals.folder.label");
				}
				
				String localeFolderName = ApplicationProperties.getProperty(contentVO.getLocale().trim().toLowerCase());
				 
				/*
				 * /library/MAZDAESI/Service
				 * Manuals/en_us/image/ac5uuw00000002.gif this needs to be
				 * replaced with /library/MAZDAESI/Service
				 * Manuals/en_us/<year>/<model>/image/ac5uuw00000002.gif
				 */
				// PREPARE CHANNEL REF KEY
				String channelFolderRefKey = channelFolderName;
				channelFolderRefKey = channelFolderRefKey.replace(" ", "_");
				
				String pathToBeReplaced = "/"	+ ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
				// // add repository
				pathToBeReplaced = pathToBeReplaced	+ ApplicationProperties.getProperty("REPOSITORY").toUpperCase() + "/";
				// add channel Name
				pathToBeReplaced = pathToBeReplaced + channelFolderRefKey.toUpperCase() + "/";
				// add locale Folder Name
				pathToBeReplaced = pathToBeReplaced + localeFolderName.toLowerCase() + "/";
				channelFolderRefKey = null;
				/*
				 * ADD MODEL AND YEAR FOLDER NAME ONLY WHEN WIRING DIAGRAMS
				 */
				if(!channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("service.manuals.folder.label").trim().toLowerCase()) 
						&& !channelFolderName.trim().toLowerCase().equals(ApplicationProperties.getProperty("other.service.manuals.folder.label").trim().toLowerCase()))
				{
					// add Model Folder Name
					if(null!=contentVO.getModelFolderName() && !"".equals(contentVO.getModelFolderName()))
					{
						pathToBeReplaced = pathToBeReplaced + contentVO.getModelFolderName().toLowerCase() + "/";
					}
					
					// add Manual Type Folder Name
					if(null!=contentVO.getManualType() && !"".equals(contentVO.getManualType()))
					{
						pathToBeReplaced = pathToBeReplaced+ contentVO.getManualType().trim().toLowerCase()+"/";
					}
					// add FaceLift Folder
					if(null!=contentVO.getFaceLiftFolderName() && !"".equals(contentVO.getFaceLiftFolderName()))
					{
						pathToBeReplaced = pathToBeReplaced+ contentVO.getFaceLiftFolderName().trim().toLowerCase()+"/";
					}
					// add materialFolder
					if(null!=contentVO.getMaterialName() && !"".equals(contentVO.getMaterialName()))
					{
						pathToBeReplaced = pathToBeReplaced  + contentVO.getMaterialName().toLowerCase() + "/";
					}
				}

				// add pdf directory name
				pathToBeReplaced = pathToBeReplaced + "pdf";
				logger.info("replacePdfPathsInContent :: Path To Be Reaplced :: > " + pathToBeReplaced);
				org.jsoup.nodes.Document doc = Jsoup.parse(contentData);
				org.jsoup.select.Elements aTagsList = doc.select("a");
				if (null != aTagsList && aTagsList.size() > 0) {
					for (int i = 0; i < aTagsList.size(); i++) {
						String hrefValue = aTagsList.get(i).attr("href");
						/*
						 * LOOK FOR ONLY PDFs
						 */
						if (null != hrefValue && !"".equals(hrefValue) && !"".equals(hrefValue.trim()) &&
								!hrefValue.startsWith("#") && !hrefValue.trim().toLowerCase().startsWith("http://") 
								&& !hrefValue.trim().toLowerCase().startsWith("https://")  
								&& hrefValue.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf")))
						{
							/*
							 * Get the pdfName Name
							 */
							// the file name only (was lost when the href had a folder: the link ended at .../pdf/)
							String pdfName = hrefValue.trim();
							if (pdfName.lastIndexOf("/") != -1) {
								pdfName = pdfName.substring(pdfName.lastIndexOf("/") + 1);
							}
							pdfName = pathToBeReplaced + "/" + pdfName;
							// remove the href attribute and set the new pdf
							// path
							aTagsList.get(i).removeAttr("href");
							// add the updated href path attribute
							aTagsList.get(i).attr("href", pdfName);
							pdfName = null;
						}
						hrefValue = null;
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
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "replacePdfPathsInContent()", e);
		}
		return contentData;
	}
	
	
	/**
	 * Function will read the STYLE CONTENT FROM HTML AND WILL RETURN IT
	 * @param htmlContent
	 * @return
	 */
	public static String readStyleTagContent(String htmlContent)
	{
		String styleContent="";
		try
		{
			if(null!=htmlContent && !"".equals(htmlContent))
			{
				org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
				org.jsoup.select.Elements styleTagsList = doc.select("style");
				if(null!=styleTagsList && styleTagsList.size()>0)
				{
					for(int a=0;a<styleTagsList.size();a++)
					{
						Element styleEle = styleTagsList.get(a);
						styleContent = styleContent+ styleEle.outerHtml();
						styleEle=  null;
					}
				}
				styleTagsList = null;
				doc = null;
			}
			
			
			/*
			 * CHECK HERE IF STYLE CONTENT IS NOT NULL
			 * THE SEARCH FOR THE table inside it
			 * and remove the style applied for it
			 * also search for the table, th, td
			 * remove this style as well
			 */
			if(null!=styleContent && !"".equals(styleContent))
			{
				if(styleContent.indexOf("table {")!=-1)
				{
					String before = styleContent.substring(0, styleContent.indexOf("table {"));
					String remaining = styleContent.substring(styleContent.indexOf("table {") , styleContent.length());
					if(null!=remaining && !"".equals(remaining))
					{
						// CHECK FOR THE FIRST INDEX OF }. AND READ THE DATA AFTER IT.
						if(remaining.indexOf("}")!=-1)
						{
							String after = remaining.substring(remaining.indexOf("}")+1, remaining.length());
							if(null!=before && !"".equals(before) && null!=after && !"".equals(after))
							{
								// TABLE CSS REMOVED.
								styleContent = before+after;
							}
							after = null;
						}
					}
					remaining = null;
					before= null;
				}
			}
			
			// NOW table, th, td {
			if(null!=styleContent && !"".equals(styleContent))
			{
				if(styleContent.indexOf("table, th, td {")!=-1)
				{
					String before = styleContent.substring(0, styleContent.indexOf("table, th, td {"));
					String remaining = styleContent.substring(styleContent.indexOf("table, th, td {") , styleContent.length());
					if(null!=remaining && !"".equals(remaining))
					{
						// CHECK FOR THE FIRST INDEX OF }. AND READ THE DATA AFTER IT.
						if(remaining.indexOf("}")!=-1)
						{
							String after = remaining.substring(remaining.indexOf("}")+1, remaining.length());
							if(null!=before && !"".equals(before) && null!=after && !"".equals(after))
							{
								// TABLE CSS REMOVED.
								styleContent = before+after;
							}
							after = null;
						}
					}
					remaining = null;
					before= null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readStyleTagContent()", e);
		}
		return styleContent;
	}

	/**
	 * Function will read the link CSS Tags from content and will prepare the OKASSETS PATH OF THE CSS
	 * and will return it.
	 * @param htmlContent
	 * @return
	 */
	public static String readLinkCSSContent(String htmlContent, ContentDetails contentVO)
	{
		String linkCssContent="";
		try
		{
			if(null!=htmlContent && !"".equals(htmlContent))
			{
				String channelFolderName="";
				if(null!=contentVO.getChannelName() && 
						contentVO.getChannelName().equals(ApplicationProperties.getProperty("WIRING_DIAGRAMS_CHANNEL_NAME")))
				{
					channelFolderName =  ApplicationProperties.getProperty("wiring.diagram.folder.label");
				}
				else if(null!=contentVO.getChannelName() && 
						contentVO.getChannelName().equals(ApplicationProperties.getProperty("SERVICE_MANUALS_CHANNEL_NAME")))
				{
					channelFolderName = ApplicationProperties.getProperty("service.manuals.folder.label");
				}
				else if(null!=contentVO.getChannelName() && 
						contentVO.getChannelName().equals(ApplicationProperties.getProperty("OTHER_SERVICE_MANUALS_CHANNEL_NAME")))
				{
					channelFolderName = ApplicationProperties.getProperty("other.service.manuals.folder.label");
				}
				
				String localeFolderName = ApplicationProperties.getProperty(contentVO.getLocale().trim().toLowerCase());
				 
				/*
				 * replaced with /library/MAZDAESI/Service
				 * Manuals/en_us/<year>/<model>/image/ac5uuw00000002.gif
				 */
				// PREPARE CHANNEL REF KEY
				String channelFolderRefKey = channelFolderName;
				channelFolderRefKey = channelFolderRefKey.replace(" ", "_");
				
				String pathToBeReplaced = "/"	+ ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
				// // add repository
				pathToBeReplaced = pathToBeReplaced	+ ApplicationProperties.getProperty("REPOSITORY").toUpperCase() + "/";
				// add channel Name
				pathToBeReplaced = pathToBeReplaced + channelFolderRefKey.toUpperCase() + "/";
				// add locale Folder Name
				pathToBeReplaced = pathToBeReplaced + localeFolderName.toLowerCase() + "/";
				channelFolderRefKey = null;

				// add Model Folder Name
				if(null!=contentVO.getModelFolderName() && !"".equals(contentVO.getModelFolderName()))
				{
					pathToBeReplaced = pathToBeReplaced + contentVO.getModelFolderName().toLowerCase() + "/";
				}
				
				// add Manual Type Folder Name
				if(null!=contentVO.getManualType() && !"".equals(contentVO.getManualType()))
				{
					pathToBeReplaced = pathToBeReplaced+ contentVO.getManualType().trim().toLowerCase()+"/";
				}
				// add FaceLift Folder
				if(null!=contentVO.getFaceLiftFolderName() && !"".equals(contentVO.getFaceLiftFolderName()))
				{
					pathToBeReplaced = pathToBeReplaced+ contentVO.getFaceLiftFolderName().trim().toLowerCase()+"/";
				}
				// add materialFolder
				if(null!=contentVO.getMaterialName() && !"".equals(contentVO.getMaterialName()))
				{
					pathToBeReplaced = pathToBeReplaced  + contentVO.getMaterialName().toLowerCase() + "/";
				}
				// add processingFolder - which will always be html
				pathToBeReplaced = pathToBeReplaced + "html";
				
				// start readingCSS Paths
				org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
				org.jsoup.select.Elements linkTagsList = doc.select("link");
				/*
				 * append only content.css or contents.css remove other links - No need to add them as they are breaking
				 * IM & IC Layout - Date 21 MAY 2021
				 */
				if(null!=linkTagsList && linkTagsList.size()>0)
				{
					String name=null;
					for(int a=0;a<linkTagsList.size();a++)
					{
						Element linkCssEle = linkTagsList.get(a);
						String hrefContent = linkCssEle.attr("href");
						name="";
						if(null!=hrefContent && !"".equals(hrefContent))
						{
							// value here will be = ./content.css - identify the name
							if(hrefContent.lastIndexOf("/")!=-1)
							{
								name = hrefContent.substring(hrefContent.lastIndexOf("/")+1, hrefContent.length());
							}
							else
							{
								name = hrefContent;
							}
						}
						
						// IF CONTENT.CSS OR CONTENTS.CSS THEN ONLY ADD
						if(null!=name && ("content.css".equalsIgnoreCase(name.trim().toLowerCase()) || "contents.css".equalsIgnoreCase(name.trim().toLowerCase())))
						{
							// now add OKAssetspath
							hrefContent = pathToBeReplaced+"/"+name;

							/// set the updated path in link tag
							linkCssEle.attr("href", hrefContent);
							linkCssContent = linkCssContent+linkCssEle.outerHtml();
						}
						hrefContent = null;
						linkCssEle=  null;
						name = null;
					}
				}
				linkTagsList = null;
				doc = null;
				
				pathToBeReplaced =  null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readLinkCSSContent()", e);
		}
		return linkCssContent;
	}

	
	/**
	 * Function will Prepare Paths for all the INNER LINKS
	 * @param htmlContent
	 * @param contentDetails
	 * @return
	 */
	public static ContentDetails prepareInnerLinkPaths(ContentDetails contentDetails)
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
			if(null!=contentDetails.getDocumentContent() && !"".equals(contentDetails.getDocumentContent()))
			{
				org.jsoup.nodes.Document doc = Jsoup.parse(contentDetails.getDocumentContent());
				org.jsoup.select.Elements aTagsList  = doc.select("a");
				if(null!=aTagsList && aTagsList.size()>0)
				{
					for(int a=0;a<aTagsList.size();a++)
					{
						Element aElement = aTagsList.get(a);
						String hrefAttr = aElement.attr("href");
						/*
						 * OTHER MANUAL LINKS (InfoManager search links) - TRACKED IN DATABASE FOR REFERENCE
						 *
						 * index?page=result_mc&startover=y&qstr=qstr&fac=CMS-CATEGORY-MAZDA-SERVICE_MANUAL_TYPE.WORKSHOP_MANUAL&question_box={Tag ID}
						 * index?page=result_mme&startover=y&fac=CMS-CATEGORY-MAZDA-SERVICE_MANUAL_TYPE.WORKSHOP_MANUAL&qstr=qstr&question_box={Tag ID}
						 * index?page=result&startover=y&qstr=qstr&fac=CMS-CATEGORY-MAZDA-SERVICE_MANUAL_TYPE.WORKSHOP_MANUAL&question_box={Tag ID}
						 *
						 * WERE PASSED AS IS; NOW REWRITTEN TO THE KAPTURE SEARCH LINK (result_mc?question_box={Tag ID})
						 * BY THE SAME RULES AS THE BULK CONTENT IMPORT JOB - OTHER_MANUAL_LINK_RULES IN application.properties.
						 * THE REPORT SHOWS BOTH (SOURCE + KAPTURE LINK); THE DATABASE KEEPS THE SOURCE LINK.
						 */
						OtherManualLinkRules.Rule otherManualRule = (null!=hrefAttr && !"".equals(hrefAttr.trim())) ? OtherManualLinkRules.get().match(hrefAttr) : null;
						if(null!=otherManualRule)
						{
							String kaptureLink = OtherManualLinkRules.get().apply(otherManualRule, hrefAttr);
							aElement.attr("href", kaptureLink);
							linkDetails = new LinkDetails();
							linkDetails.setInnerLinkPath(hrefAttr);
							linkDetails.setKaptureLinkPath(kaptureLink);
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
						else if(null!=hrefAttr && !"".equals(hrefAttr) && !"#".equals(hrefAttr.trim()) && 
								!hrefAttr.trim().toLowerCase().startsWith("#") && 
								!hrefAttr.trim().toLowerCase().startsWith("http:") && !hrefAttr.trim().toLowerCase().startsWith("https:") && 
								!hrefAttr.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf")))
						{
							hrefAttr= hrefAttr.trim();
							
							/*
							 * HERE INCASE OF ISUZU, 2 DIFFERENT TYPES OF INNER LINKS MAY COME
							 * 
							 * PATTERN 1 - 
							 * href="javascript:openAnotherTab('sie_1290337.html');"
							 * 
							 * PATTERN 2 - 
							 * href="javascript:openAnotherTab('./frame.html#sie_1290337');"
							 * 
							 * IF PATTERN 1 - 
							 * IDENTIFY THE HTML NAME  - & THEN PREPARE NORMAL INNER LINK PATH
							 * 
							 *  IF PATTERN 2 - 
							 *  IDENTIFY THE VALUES IN BETWEEN '' AND AFTER THAT IDENTIFY THE VALUE
							 *  AFTER FIRST #, USE THE VALUE FROM # AND SET IN HREF
							 *  	
							 *  ELSE NORMAL INNER LINK FLOW.
							 */
							boolean continueNormalFlow = true;
							if(hrefAttr.toLowerCase().contains("javascript:openanothertab(") && hrefAttr.toLowerCase().indexOf("javascript:openanothertab(")!=-1)
							{
								hrefAttr = hrefAttr.substring(hrefAttr.toLowerCase().indexOf("javascript:openanothertab(")+26, hrefAttr.length());
								if(null!=hrefAttr)
								{
									hrefAttr = hrefAttr.replace("(", "");
									hrefAttr = hrefAttr.replace(")", "");
									hrefAttr = hrefAttr.replace("'", "");
									hrefAttr = hrefAttr.replace(";", "");
									hrefAttr = hrefAttr.replace("\"", "");
									hrefAttr = hrefAttr.trim();
								}
								
								// NOW PATTERN 1 & PATTERN 2 CHECK
								if(null!=hrefAttr && !hrefAttr.contains("#"))
								{
									// PATTERN 1
									continueNormalFlow = true;
								}
								else if(null!=hrefAttr && hrefAttr.contains("#"))
								{
									// PATTERN 2
									continueNormalFlow = false;
								}
							}
							
							if(continueNormalFlow==false &&  null!=hrefAttr && !"".equals(hrefAttr))
							{
								if(hrefAttr.indexOf("#")!=-1)
								{
									hrefAttr = hrefAttr.substring(hrefAttr.indexOf("#"), hrefAttr.length());
								}
								// update href Attribute to the Link - Do not Track Such Inner Links
								aElement.attr("href",hrefAttr);
							}
							else if(continueNormalFlow==true && null!=hrefAttr && !"".equals(hrefAttr) && !"#".equals(hrefAttr.trim()) && 
									!hrefAttr.trim().toLowerCase().startsWith("#") && 
									!hrefAttr.trim().toLowerCase().startsWith("http:") && !hrefAttr.trim().toLowerCase().startsWith("https:") && 
									!hrefAttr.trim().toLowerCase().endsWith(ApplicationProperties.getProperty("extension.pdf")))
							{
								// APPLICABLE FOR ALL INNER LINKS
								
								// replace all / by \\ 
								hrefAttr = hrefAttr.replace("/", "\\");
								
								String fileName="";
								if(hrefAttr.lastIndexOf("\\")!=-1)
								{
									fileName= hrefAttr.substring(hrefAttr.lastIndexOf("\\")+1, hrefAttr.length());
								}
								else
								{
									fileName= hrefAttr;
								}
								
								/*
								 * CHECK FOR PROCESSING DOC PARENT AND EXTRACT
								 * THE PATH FROM LOCALE TO THE PROCESSING FILE'S PARENT FOLDER
								 * APPEND TO THE LINK 
								 */
								if(null!=contentDetails.getFileAbsolutePath() && !"".equals(contentDetails.getFileAbsolutePath()))
								{
									File processingFile = PathUtil.file(contentDetails.getFileAbsolutePath());
									if(processingFile.isFile() && processingFile.exists())
									{
										String parentDirPath = PathUtil.winPath(processingFile.getParentFile());
										if(null!=parentDirPath && !"".equals(parentDirPath))
										{
											parentDirPath = parentDirPath.substring(parentDirPath.lastIndexOf(contentDetails.getLocale()), parentDirPath.length());
											if(null!=parentDirPath && !"".equals(parentDirPath))
											{
												parentDirPath = parentDirPath.replace("/", "\\");
											}
											if(!parentDirPath.endsWith("\\"))
											{
												parentDirPath = parentDirPath+"\\";
											}
											// add PARENT DIR PATH + FILE NAME
											hrefAttr = parentDirPath+fileName.trim();
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
								
								// update href Attribute to the Link
								aElement.attr("href",hrefAttr);
								fileName= null;
							}
						}
						hrefAttr=null;
					}
					
					/*
					 * check if keyToCheckTokens is NOT NULL
					 * 	MAKE A DB QUERY AND CHECK IF ALL THE INNER LINKS ARE FOUND
					 * 		IF YES - THEN NO NEED OF UPDATING THE DOCUMENT AGAIN
					 */
					if(null!=uniqueInnerLinkPaths && uniqueInnerLinkPaths.size()>0 && null!=contentDetails.getInnerLinkFound() && contentDetails.getInnerLinkFound().equals("YES"))
					{
						logger.info("prepareInnerLinkPaths :: Unique uniqueInnerLinkPaths found are :: >" + uniqueInnerLinkPaths.size());
						List<SelectItemDetails> docsList = MMEDocumentManagementDAO.getDocumentDetailsForInnerLink(uniqueInnerLinkPaths, contentDetails.getModelType(), contentDetails.getLocale(), contentDetails.getMarket());
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
				if(null!=doc)
				{
					// REPLACE RAW SOURCE PATH OF INNER LINKS IN DOCUMENT CONTENT
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
			extensionToBeAdded = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "prepareInnerLinkPaths()", e);
		}
		logger.info("prepareInnerLinkPaths :: ------------------ ENDS ------------------ ");
		return contentDetails;
	}
	
	/**
	 * The MAZDA CSS of MC ENT documents: /library/MAZDA/GMS3_CUSTOM/<GMS3_MC_CUSTOM_CONTENT_CSS>.
	 * The one link tag the Kapture sanitizer keeps (KaptureHtmlSanitizer).
	 */
	public static String entStyleHref()
	{
		String cssPath = ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
		if(!cssPath.startsWith("/"))
		{
			cssPath = "/"+cssPath;
		}
		if(!cssPath.endsWith("/"))
		{
			cssPath = cssPath+"/";
		}
		// add repository path
		cssPath = cssPath+ApplicationProperties.getProperty("REPOSITORY").toUpperCase() +"/";
		// add custom directory name
		cssPath = cssPath+ApplicationProperties.getProperty("GMS3_CUSTOM_DIRECTORY")+"/";
		// add css name -
		return cssPath+ApplicationProperties.getProperty("GMS3_MC_CUSTOM_CONTENT_CSS");
	}

	/**
	 * true for a stylesheet link the conversion itself puts in a document, as legacy did - the links
	 * the Kapture sanitizer keeps (KaptureHtmlSanitizer):
	 *   ENT  : the MAZDA CSS (entStyleHref)
	 *   HTML : content.css / contents.css of the document's html folder (readLinkCSSContent)
	 */
	public static boolean isConversionStyleLink(String href)
	{
		if(null==href || "".equals(href.trim()))
		{
			return false;
		}
		String h = href.trim();
		if(h.equals(entStyleHref()))
		{
			return true;
		}
		String library = ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY").trim();
		if(!library.startsWith("/"))
		{
			library = "/"+library;
		}
		String lower = h.toLowerCase();
		return lower.startsWith(library.toLowerCase()) && (lower.endsWith("/html/content.css") || lower.endsWith("/html/contents.css"));
	}

	/**
	 * Function will Add MAZDA CSS FOR MC CONTENT FOR ENT DOCUMENTS
	 * EVERY OTHER LINK TAG IS REMOVED FIRST - THE MAZDA CSS IS THE ONLY LINK THE DOCUMENT CARRIES
	 * @param htmlContent
	 * @return
	 */
	public static String addStyleLinkForEntDocuments(String htmlContent)
	{
		try
		{
			if(null!=htmlContent && !"".equals(htmlContent))
			{
				org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
				doc.select("link").remove();
				org.jsoup.select.Elements bodyTagsList = doc.select("body");
				if(null!=bodyTagsList && bodyTagsList.size()>0)
				{
					Element bodyEle = bodyTagsList.get(0);
					Element linkEle = doc.createElement("link");
					String cssPath = entStyleHref();
					linkEle.attr("href",cssPath);
					linkEle.attr("type","text/css");
					linkEle.attr("rel","stylesheet");
					bodyEle.appendChild(linkEle);
					linkEle= null;
					cssPath= null;
				}
				
				if (null != doc) {
					htmlContent = doc.toString();
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "addStyleLinkForEntDocuments()", e);
		}
		return htmlContent;
	}

	/**
	 * The MAZDA CSS of MME ENT documents: /library/MAZDA/GMS3_CUSTOM/<GMS3_MME_CUSTOM_CONTENT_CSS>.
	 * The one link tag the Kapture sanitizer keeps in an MME ENT document (isConversionStyleLinkMME).
	 */
	public static String entStyleHrefMME()
	{
		String cssPath = ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
		if(!cssPath.startsWith("/"))
		{
			cssPath = "/"+cssPath;
		}
		if(!cssPath.endsWith("/"))
		{
			cssPath = cssPath+"/";
		}
		// add repository path
		cssPath = cssPath+ApplicationProperties.getProperty("REPOSITORY").toUpperCase() +"/";
		// add custom directory name
		cssPath = cssPath+ApplicationProperties.getProperty("GMS3_CUSTOM_DIRECTORY")+"/";
		// add css name -
		return cssPath+ApplicationProperties.getProperty("GMS3_MME_CUSTOM_CONTENT_CSS");
	}

	/**
	 * isConversionStyleLink() for the MME market: the MME MAZDA CSS of an ENT document
	 * (entStyleHrefMME), or content.css / contents.css of an HTML document's html folder.
	 */
	public static boolean isConversionStyleLinkMME(String href)
	{
		if(null==href || "".equals(href.trim()))
		{
			return false;
		}
		String h = href.trim();
		if(h.equals(entStyleHrefMME()))
		{
			return true;
		}
		String library = ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY").trim();
		if(!library.startsWith("/"))
		{
			library = "/"+library;
		}
		String lower = h.toLowerCase();
		return lower.startsWith(library.toLowerCase()) && (lower.endsWith("/html/content.css") || lower.endsWith("/html/contents.css"));
	}

	/**
	 * Function will Add MAZDA CSS FOR MME CONTENT FOR ENT DOCUMENTS
	 * EVERY OTHER LINK TAG IS REMOVED FIRST - THE MAZDA CSS IS THE ONLY LINK THE DOCUMENT CARRIES (as MC, O1)
	 * @param htmlContent
	 * @return
	 */
	public static String addStyleLinkForEntDocumentsForMMEMarket(String htmlContent)
	{
		try
		{
			if(null!=htmlContent && !"".equals(htmlContent))
			{
				org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
				doc.select("link").remove();
				org.jsoup.select.Elements bodyTagsList = doc.select("body");
				if(null!=bodyTagsList && bodyTagsList.size()>0)
				{
					Element bodyEle = bodyTagsList.get(0);
					Element linkEle = doc.createElement("link");
					String cssPath = entStyleHrefMME();
					linkEle.attr("href",cssPath);
					linkEle.attr("type","text/css");
					linkEle.attr("rel","stylesheet");
					bodyEle.appendChild(linkEle);
					linkEle= null;
					cssPath= null;
				}

				if (null != doc) {
					htmlContent = doc.toString();
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "addStyleLinkForEntDocumentsForMMEMarket()", e);
		}
		return htmlContent;
	}

	
	/**
	 * The MAZDA CSS of MNAO ENT documents: /library/MAZDA/GMS3_CUSTOM/<GMS3_CUSTOM_CONTENT_CSS>.
	 * The one link tag the Kapture sanitizer keeps in an MNAO ENT document (isConversionStyleLinkMNAO).
	 */
	public static String entStyleHrefMNAO()
	{
		String cssPath = ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
		if(!cssPath.startsWith("/"))
		{
			cssPath = "/"+cssPath;
		}
		if(!cssPath.endsWith("/"))
		{
			cssPath = cssPath+"/";
		}
		// add repository path
		cssPath = cssPath+ApplicationProperties.getProperty("REPOSITORY").toUpperCase() +"/";
		// add custom directory name
		cssPath = cssPath+ApplicationProperties.getProperty("GMS3_CUSTOM_DIRECTORY")+"/";
		// add css name -
		return cssPath+ApplicationProperties.getProperty("GMS3_CUSTOM_CONTENT_CSS");
	}

	/**
	 * isConversionStyleLink() for the MNAO market: the MNAO MAZDA CSS of an ENT document
	 * (entStyleHrefMNAO), or content.css / contents.css of an HTML document's html folder.
	 */
	public static boolean isConversionStyleLinkMNAO(String href)
	{
		if(null==href || "".equals(href.trim()))
		{
			return false;
		}
		String h = href.trim();
		if(h.equals(entStyleHrefMNAO()))
		{
			return true;
		}
		String library = ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY").trim();
		if(!library.startsWith("/"))
		{
			library = "/"+library;
		}
		String lower = h.toLowerCase();
		return lower.startsWith(library.toLowerCase()) && (lower.endsWith("/html/content.css") || lower.endsWith("/html/contents.css"));
	}

	/**
	 * Function will Add MAZDA CSS FOR MNAO CONTENT FOR ENT DOCUMENTS (GMS3_CUSTOM_CONTENT_CSS)
	 * EVERY OTHER LINK TAG IS REMOVED FIRST - THE MAZDA CSS IS THE ONLY LINK THE DOCUMENT CARRIES (as MC / MME)
	 * @param htmlContent
	 * @return
	 */
	public static String addStyleLinkForEntDocumentsForMNAOMarket(String htmlContent)
	{
		try
		{
			if(null!=htmlContent && !"".equals(htmlContent))
			{
				org.jsoup.nodes.Document doc = Jsoup.parse(htmlContent);
				doc.select("link").remove();
				org.jsoup.select.Elements bodyTagsList = doc.select("body");
				if(null!=bodyTagsList && bodyTagsList.size()>0)
				{
					Element bodyEle = bodyTagsList.get(0);
					Element linkEle = doc.createElement("link");
					String cssPath = entStyleHrefMNAO();
					linkEle.attr("href",cssPath);
					linkEle.attr("type","text/css");
					linkEle.attr("rel","stylesheet");
					bodyEle.appendChild(linkEle);
					linkEle= null;
					cssPath= null;
				}

				if (null != doc) {
					htmlContent = doc.toString();
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "addStyleLinkForEntDocumentsForMNAOMarket()", e);
		}
		return htmlContent;
	}

	/**
	 * Function will read the FONT CONTENT FROM HTML FILE.
	 * @param content
	 * @return
	 */
	public static String readFontDataFromHTML(String content)
	{
		String fontContent="";
		try
		{
			if(null!=content && !"".equals(content))
			{
				/*
				 * READ THE FONT NODE DATA AND SET IN FONT SELECTION
				 */
				org.jsoup.nodes.Document doc = Jsoup.parse(content);
				org.jsoup.select.Elements fontTagsList = doc.select("font");
				if(null!=fontTagsList && fontTagsList.size()>0)
				{
					Element fontEle = fontTagsList.get(0);
					fontContent = fontEle.html();
					fontEle =  null;
				}
				doc = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readFontDataFromHTML()", e);
		}
		return fontContent;
	}
	
	/**
	 * Function will identify whether TOYATO CONTENT Mapped or Not
	 * @param content
	 * @return
	 */
	public static String readMetaTagDescriptionValue(String content)
	{
		String value=null;
		try
		{
			if(null!=content && !"".equals(content))
			{
				/*
				 * READ THE FONT NODE DATA AND SET IN FONT SELECTION
				 */
				org.jsoup.nodes.Document doc = Jsoup.parse(content);
				org.jsoup.select.Elements metaTagsList = doc.select("meta");
				if(null!=metaTagsList && metaTagsList.size()>0)
				{
					for(int a=0;a<metaTagsList.size();a++)
					{
						Element metaEle = metaTagsList.get(a);
						String attr = metaEle.attr("name");
						if(null!=attr && !"".equals(attr) && attr.trim().toLowerCase().equals("description"))
						{
							// CHECK FOR CONTENT VALUE
							String contentAttr = metaEle.attr("content");
							if(null!=contentAttr && !"".equals(contentAttr))
							{
								value = contentAttr;
							}
							contentAttr = null;
							break;
						}
						attr = null;
						metaEle = null;
					}
				}
				metaTagsList = null;
				doc = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ConversionUtils.class.getName(), "readMetaTagDescriptionValue()", e);
		}
		return value;
	}
	
	/**
	 * The model folder name in the shape the MC / MME parsing reads: <MODEL TYPE>_<MODEL or OVERHAUL>_<CODE>.
	 * MNAO model folders have no model type (CX-5_KM, BT-50_TF, or a pseudo model such as DM), so for MNAO an empty
	 * model type is put in front: the model type comes out empty, the model as for MC / MME (CX-5, BT-50, DM).
	 */
	public static String modelFolderForParsing(String modelFolderName, String market)
	{
		if(null!=modelFolderName && null!=market
				&& market.trim().equalsIgnoreCase(ApplicationProperties.getProperty("market.mnao").trim()))
		{
			return "_" + modelFolderName;
		}
		return modelFolderName;
	}

	public static String identifyChannelFolderName(String manualType, String modelType)
	{
		String channelFolderName ="";
		/*
		 * IDENTIFY CHANNEL FOLDER NAME
		 *l WD or e-WD, this will depend on the processing Folder
		 * wiring.diagram.folder=wd
		 * electronic.wiring.diagram.folder=e-wd
		 * 
		 * Here only Process Display Order Entries of the Same Material Folder
		 */
		boolean wdFound =false;
		
		if(null!=manualType && !"".equals(manualType))
		{
			if(manualType.trim().toLowerCase().equals(ApplicationProperties.getProperty("wiring.diagram.folder")) || 
					manualType.trim().toLowerCase().equals(ApplicationProperties.getProperty("electronic.wiring.diagram.folder")))
			{
				wdFound = true;
				channelFolderName =  ApplicationProperties.getProperty("wiring.diagram.folder.label");
			}
		}

		if(wdFound==false)
		{
			/*
			 * CHECK ON THE BASIS OF MODEL TYPE
			 */
			if(null!=modelType && !"".equals(modelType))
			{
				if(modelType.trim().toLowerCase().equals(ApplicationProperties.getProperty("model.type.new")))
				{
					// SERVICE MANUALS
					channelFolderName = ApplicationProperties.getProperty("service.manuals.folder.label");
				}
				else
				{
					// OTHER SERVICE MANUALS
					channelFolderName = ApplicationProperties.getProperty("other.service.manuals.folder.label");
				}
			}
			else
			{
				// OTHER SERVICE MANUALS
				channelFolderName = ApplicationProperties.getProperty("other.service.manuals.folder.label");
			}
		}
		return channelFolderName;
	}

}