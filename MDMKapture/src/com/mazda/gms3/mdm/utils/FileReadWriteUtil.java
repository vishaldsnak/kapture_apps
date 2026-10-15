package com.mazda.gms3.mdm.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

public class FileReadWriteUtil {

	private static final Logger logger = Logger
			.getLogger(FileReadWriteUtil.class.getName());
	
	
	private static InputStream inputStream;

	/**
	 * Function will write the File at passed File path
	 * 
	 * @param data
	 * @param filePath
	 * @return
	 */
	public static boolean writeFile(byte[] data, String filePath) {
		try {
			// check whether parameters are received or not
			if (null != data && null != filePath && !"".equals(filePath)) {
				// create file object and pass file path as parameter
				File file = new File(filePath);
				// now create File Output Stream object and pass the file object
				// as parameter to it
				FileOutputStream fos = new FileOutputStream(file);
				// add byte[] data to fos.write method
				fos.write(data);
				// flush the object
				fos.flush();
				// close the object
				fos.close();

				// set fos object to null
				fos = null;
				// set file object to null
				file = null;
				logger.info("writeFile :: File Written Successfully at Path - > ."
						+ filePath);
			} else {
				logger.info("writeFile :: Parameters not received. Returning False.");
				return false;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileReadWriteUtil.class.getName(), "writeFile()", e);
			return false;
		}
		return true;
	}

	/**
	 * Function will read the file form the physical path sent as parameter and
	 * return the byte[] data of it.
	 * 
	 * @param filePath
	 * @return
	 */
	public static byte[] readFile(String filePath) {
		byte[] data = null;
		try {
			if (null != filePath && !"".equals(filePath)) {
//				logger.info("readFile :: Parameter received; reading file from the File Path.");
				// create new file object with file path as parameter
				File file = new File(filePath);
				// check whether file is file or not
				if (file.isFile()) {
//					logger.info("readFile :: Parameter received; File Exists.");
					// calculate fileLength
					int fileLength = (int) file.length();
					inputStream = new FileInputStream(file);
					// initialize byte[] object with passing the size as file
					// length
					data = new byte[fileLength];
					// read all the bytes from inputStream and set in byte[]
					// array
					inputStream.read(data, 0, fileLength);

					// set inputStream to null
					inputStream = null;
				} else {
					logger.info("readFile :: Parameter received; File Not Found.");
					// set data to null
				}
				// set file to null
				file = null;
//				logger.info("readFile :: File Read Successfully and transformed to bytes.");
			} else {
				logger.info("readFile :: Parameters not received. Set byte[] data to null.");
				// set byte[] data to null - parameters not received.
				data = null;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileReadWriteUtil.class.getName(), "readFile()", e);
			// set byte[] data to null
			data = null;
		}
		return data;
	}

	/**
	 * Function will delete File from the provided filePath as parameter
	 * @param filePath
	 * @return
	 */
	public static boolean deleteFile(String filePath) {
		try {
			// check whether parameters are received or not
			if (null != filePath && !"".equals(filePath)) {
				// create file object and pass file path as parameter
				File file = new File(filePath);
				file.delete();
				// set file object to null
				file = null;
				logger.info("deleteFile :: File Deleted Successfully at Path - > ."
						+ filePath);
			} else {
				logger.info("deleteFile :: Parameters not received. Returning False.");
				return false;
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(FileReadWriteUtil.class.getName(), "deleteFile()", e);
			return false;
		}
		return true;
	}
	
	/**
	 * Function will convert the InputStream to String
	 * 
	 * @param is
	 * @return
	 */
	public static String readInputStramToString(InputStream is) {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try {
			if (null != is) {
				String line;
				br = new BufferedReader(new InputStreamReader(is));
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}
}
