package com.mazda.gms3.cdrom.bean;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.utils.AbortCheck;

public class CDRomZipUtils {

	/*
	 * SET BY THE CD CREATION JOB SO THE FILE WALK AND THE ZIP BELOW CAN STOP ON ABORT.
	 *
	 * Zipping a whole CD image is the longest single step of the data extract, and it runs
	 * inside the schedule's own thread. Without this it completes however early the user
	 * pressed Abort. Null for any caller with no schedule to abort - then nothing changes.
	 */
	private AbortCheck abortCheck = null;

	public void setAbortCheck(AbortCheck abortCheck)
	{
		this.abortCheck = abortCheck;
	}
	static Logger logger = LogManager.getLogger(CDRomZipUtils.class);

	private List<CDRomErrorDetails> errorsList = null;
	private CDRomErrorDetails ed=null;
	private List <String> fileList;
//    private static String OUTPUT_ZIP_FILE;
    private String SOURCE_FOLDER; // SourceFolder path

//    public CDRomZipUtils(String outputFile, String sourceFolder) {
//        fileList = new ArrayList < String > ();
////        OUTPUT_ZIP_FILE = outputFile;
//        SOURCE_FOLDER = sourceFolder;
//    }
    public CDRomZipUtils(){
        fileList = new ArrayList < String > ();
        errorsList = null;
        ed = null;
    }

    public void initialize(String outputFile, String sourceFolder)
    {
    	 fileList = new ArrayList < String > ();
//       OUTPUT_ZIP_FILE = outputFile;
       SOURCE_FOLDER = sourceFolder;
    }

    public List<CDRomErrorDetails> getErrorsList() {
		return errorsList;
	}
	public void setErrorsList(List<CDRomErrorDetails> errorsList) {
		this.errorsList = errorsList;
	}
	public boolean zipIt(String zipFile) {
    	logger.info("CDROMZipUtils zipIt starts here ");
    	boolean bool = true;
        byte[] buffer = new byte[1024];
        String source = new File(SOURCE_FOLDER).getName();
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(fos);

            FileInputStream in = null;

            for (String file: this.fileList) {
            	if(null!=abortCheck && abortCheck.isAborted()) { break; }
                ZipEntry ze = new ZipEntry(source + File.separator + file);
                zos.putNextEntry(ze);
                try {
                    in = new FileInputStream(SOURCE_FOLDER + File.separator + file);
                    int len;
                    while ((len = in .read(buffer)) > 0) {
                    	if(null!=abortCheck && abortCheck.isAborted()) { break; }
                        zos.write(buffer, 0, len);
                    }
                }
                catch(Exception ext)
                {
                	/*
                	 * add error to tracking List 
                	 */
                	ed = new CDRomErrorDetails();
                	// add file
                	ed.setFilePath(SOURCE_FOLDER + File.separator + file);
                	// add errors
                	Writer writer = new StringWriter();
        			PrintWriter print = new PrintWriter(writer);
        			ext.printStackTrace(print);
        			ed.setErrorCode(ext.getMessage());
        			ed.setErrorMessage(writer.toString());
        			try{ if(null!=writer) {writer.close();}} catch(Exception ex) {}
        			writer = null;
        			try{ if(null!=print) {print.close();}} catch(Exception ex) {}
        			print = null;
        			
        			// add to errorList
        			if(null==errorsList || errorsList.size()<=0)
        			{
        				errorsList = new ArrayList<CDRomErrorDetails>();
        			}
        			errorsList.add(ed);
        			ed = null;
                	Utilities.printStackTraceToLogs(CDRomZipUtils.class.getName(), "zipIt()", ext);
                	// set bool = false as some exception has occured
                	bool = false;
                }
                finally {
                
                    in.close();
                }
            }

            zos.closeEntry();

        } catch (IOException ex) {
        	logger.info("zipIt :: ################ Exception ################");
			Utilities.printStackTraceToLogs(CDRomZipUtils.class.getName(), "zipIt()", ex);
			logger.info("zipIt :: ################ Exception ################");
			
			
			/*
        	 * add error to tracking List 
        	 */
        	ed = new CDRomErrorDetails();
        	// add file
        	ed.setFilePath(zipFile);
        	// add errors
        	Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			ex.printStackTrace(print);
			ed.setErrorCode(ex.getMessage());
			ed.setErrorMessage(writer.toString());
			try{ if(null!=writer) {writer.close();}} catch(Exception ext) {}
			writer = null;
			try{ if(null!=print) {print.close();}} catch(Exception ext) {}
			print = null;
			
			// add to errorList
			if(null==errorsList || errorsList.size()<=0)
			{
				errorsList = new ArrayList<CDRomErrorDetails>();
			}
			errorsList.add(ed);
			ed = null;
			
			
			bool = false;
        } finally {
            try {
                zos.close();
            } catch (IOException e) {
            	logger.info("zipIt :: ################ Exception ################");
    			Utilities.printStackTraceToLogs(CDRomZipUtils.class.getName(), "zipIt()", e);
    			logger.info("zipIt :: ################ Exception ################");
            }
        }
    	logger.info("CDROMZipUtils zipIt ends here ");
    	return bool;
    }

    public void generateFileList(File node) {
        if (node.isFile()) {
            fileList.add(generateZipEntry(node.toString()));
        }

        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename: subNote) {
            	if(null!=abortCheck && abortCheck.isAborted()) { break; }
                generateFileList(new File(node, filename));
            }
        }

    }

    private String generateZipEntry(String file) {
        return file.substring(SOURCE_FOLDER.length() + 1, file.length());
    }
}


