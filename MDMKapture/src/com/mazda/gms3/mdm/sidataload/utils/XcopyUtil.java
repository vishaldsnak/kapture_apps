package com.mazda.gms3.mdm.sidataload.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelImageDetails;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelSchemaDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.FileReadWriteUtil;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.ScheduleConstants;
import com.mazda.gms3.mdm.utils.AbortCheck;


public class XcopyUtil {

	/*
	 * SET BY THE WORKER SO THE IMAGE LOOPS BELOW CAN STOP WHEN THE SCHEDULE IS ABORTED.
	 *
	 * A document can carry hundreds of images and each one is a file copy, so without this the
	 * copy runs to the end no matter when the user pressed Abort. Left null by any caller that
	 * has no schedule to abort, in which case nothing changes.
	 */
	private AbortCheck abortCheck = null;

	public void setAbortCheck(AbortCheck abortCheck)
	{
		this.abortCheck = abortCheck;
	}
	Logger logger = LogManager.getLogger(XcopyUtil.class);

	private List<SIChannelImageDetails> fileProcessingList = null;

	public List<SIChannelImageDetails> getFileProcessingList() {
		return fileProcessingList;
	}

	public void setFileProcessingList(List<SIChannelImageDetails> fileProcessingList) {
		this.fileProcessingList = fileProcessingList;
	}

	/**
	 * Function will move inputFile from sourceLocation to destinationLocation
	 * 
	 * @param inputFileDetailsVO
	 */
	public void copyFilesToServer(SIChannelSchemaDetails contentDetails) 
	{
		try 
		{
			if (null!=contentDetails && null!=contentDetails.getImagesList() && contentDetails.getImagesList().size()>0)
			{
				logger.info("copyFilesToServer :: Source File, Locale and Channel Folders Names are not null as Parameter. Proceed for Uploading File.");
				/*
				 * Prepare Destination Path /library/MAZDAESI/GMS3/MARKET FOLDER/SI/en_us/image/ac5uuw00000002.gif
				 */
				String destinationPath = "";
				destinationPath = ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
				// add repository
				destinationPath = destinationPath+ ApplicationProperties.getProperty("REPOSITORY").toUpperCase() + "/";
				// add gms3 folder Name
				destinationPath = destinationPath+ ApplicationProperties.getProperty("sichannel.data.load.gms3.diretory").toUpperCase() + "/";
				// add market folder name
				destinationPath=destinationPath+ contentDetails.getMarket().trim().toUpperCase() +"/";
				// add si folder name
				destinationPath = destinationPath+ ApplicationProperties.getProperty("sichannel.data.load.si.diretory").toUpperCase() + "/";
				// add locale Folder Name in lowercase
				destinationPath = destinationPath+ contentDetails.getLocale().replace("-", "_").trim().toLowerCase() + "/";
				// add image folder
				destinationPath = destinationPath + "image";

				// Now check for all the folders whether they exist or not
				// in
				// the destination path
				destinationPath = createFolderStructure(destinationPath,ApplicationProperties.getProperty("SERVER_OKASSETS_PHYSICAL_PATH"));
				if(!"/".endsWith(destinationPath))
				{
					destinationPath = destinationPath+"/";
				}
				SIChannelImageDetails imageDetails = null;
				if(null!=destinationPath && !"".equals(destinationPath))
				{
					byte[] imageData = null;
					boolean writeStatus = false;
					for(int a=0;a<contentDetails.getImagesList().size();a++)
					{
						if(null!=abortCheck && abortCheck.isAborted()) { break; }
						imageDetails  =(SIChannelImageDetails)contentDetails.getImagesList().get(a);
						imageData=  FileReadWriteUtil.readFile(imageDetails.getImageSourcePath());
						if(null!=imageData && imageData.length>0)
						{
							writeStatus = FileReadWriteUtil.writeFile(imageData, destinationPath+imageDetails.getDestinationImageName());
							if(writeStatus==true)
							{
								logger.info("copyFilesToServer :: Input File {"+ imageDetails.getImageName()+ "}. Moved Successfully at Path :: >"+ destinationPath+imageDetails.getDestinationImageName());
								// set Processing status as Success
								imageDetails.setProcessingStatus(ScheduleConstants.STATUS_SUCCESS);
							}
							else
							{
								logger.info("copyFilesToServer :: Failed to move Input File {"+ imageDetails.getImageName()+ "} at Path :: >"+ destinationPath+imageDetails.getDestinationImageName());
								imageDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
								imageDetails.setErrorCode("XP00002");
								imageDetails.setErrorMessage("Failed to move File at the Destination Path.");
							}
						}
						else
						{
							logger.info("copyFilesToServer :: Failed to Read Image File {"+imageDetails.getImageName()+"} At Path :: >"+ imageDetails.getImageSourcePath());
							imageDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
							imageDetails.setErrorCode("XP00003");
							imageDetails.setErrorMessage("Failed to read File at the Source Path. Either the image does not exists or is Corrputed.");
						}
							
						// ADD TO FILE PROCESSING LIST
						if(null==fileProcessingList || fileProcessingList.size()<=0)
						{
							fileProcessingList = new ArrayList<SIChannelImageDetails>();
						}
						fileProcessingList.add(imageDetails);
						imageDetails = null;
						imageData = null;
					}
					imageData = null;
					imageDetails = null;
				}
				else
				{
					logger.info("copyFilesToServer :: Failed to Read Desired Directory Structure at Destination. Path :: >"+ destinationPath);
					
					/*
					 *  iterate imagesList and set errorMessage as Failed to Read Directory Structure at the Destination Path. for each
					 *  add to fileProcessingList
					 */
					imageDetails = null;
					if(null!=contentDetails.getImagesList() && contentDetails.getImagesList().size()>0)
					{
						for(int a=0;a<contentDetails.getImagesList().size();a++)
						{
							if(null!=abortCheck && abortCheck.isAborted()) { break; }
							imageDetails = (SIChannelImageDetails)contentDetails.getImagesList().get(a);
							imageDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
							imageDetails.setErrorCode("XP00001");
							imageDetails.setErrorMessage("Failed to Read Directory Structure at the Destination Path.");
							if(null==fileProcessingList || fileProcessingList.size()<=0)
							{
								fileProcessingList = new ArrayList<SIChannelImageDetails>();
							}
							fileProcessingList.add(imageDetails);
							imageDetails = null;
						}
					}
					imageDetails=  null;
				}
				// set destinationPath to null
				destinationPath = null;
			} 
		} 
		catch (Exception e) 
		{
			Utilities.printStackTraceToLogs(XcopyUtil.class.getName(),"copyFilesToServer()", e);
			/*
			 * add all images to failure incase of any excpetion
			 */
			SIChannelImageDetails imageDetails = null;
			if(null!=contentDetails.getImagesList() && contentDetails.getImagesList().size()>0)
			{
				for(int a=0;a<contentDetails.getImagesList().size();a++)
				{
					if(null!=abortCheck && abortCheck.isAborted()) { break; }
					imageDetails = (SIChannelImageDetails)contentDetails.getImagesList().get(a);
					imageDetails.setProcessingStatus(ScheduleConstants.STATUS_FAILURE);
					imageDetails.setErrorCode("XP00003");
					imageDetails.setErrorMessage(e.getMessage());
					if(null==fileProcessingList || fileProcessingList.size()<=0)
					{
						fileProcessingList = new ArrayList<SIChannelImageDetails>();
					}
					fileProcessingList.add(imageDetails);
					imageDetails = null;
				}
			}
			imageDetails=  null;
		}
	}

	/**
	 * Function will create desired directory structure in the destination path
	 * 
	 * @param tempPath
	 * @return
	 */
	public String createFolderStructure(String tempPath, String serverPath) {
		StringTokenizer str = new StringTokenizer(tempPath, "/");
		int i = 0;
		File file;
		String token;
		String path = serverPath;
		while (str.hasMoreTokens()) {
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
		return path;
	}
}
