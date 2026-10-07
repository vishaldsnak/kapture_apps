package com.mazda.gms3.dmt.vo;

public class DeleteFileDetails {

	public static final String TYPE_DOCUMENT="DOCUMENT";
	public static final String TYPE_OKASSETS="OKASSETS";
	private String fileName=null;
	private String filePath = null;
	
	private String fileToBeDeletedName=null;
	private String fileToBeDeletedPath=null;
	private String okAssetsFileToBeDeletedName=null;
	private String okAssetsFileToBeDeletedPath=null;
	private String locale=null;
	
	private String fileType=null;
	
	
	
	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getFileToBeDeletedName() {
		return fileToBeDeletedName;
	}

	public void setFileToBeDeletedName(String fileToBeDeletedName) {
		this.fileToBeDeletedName = fileToBeDeletedName;
	}

	public String getFileToBeDeletedPath() {
		return fileToBeDeletedPath;
	}

	public void setFileToBeDeletedPath(String fileToBeDeletedPath) {
		this.fileToBeDeletedPath = fileToBeDeletedPath;
	}

	public String getOkAssetsFileToBeDeletedName() {
		return okAssetsFileToBeDeletedName;
	}

	public void setOkAssetsFileToBeDeletedName(String okAssetsFileToBeDeletedName) {
		this.okAssetsFileToBeDeletedName = okAssetsFileToBeDeletedName;
	}

	public String getOkAssetsFileToBeDeletedPath() {
		return okAssetsFileToBeDeletedPath;
	}

	public void setOkAssetsFileToBeDeletedPath(String okAssetsFileToBeDeletedPath) {
		this.okAssetsFileToBeDeletedPath = okAssetsFileToBeDeletedPath;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}
	
	
	
	
}
