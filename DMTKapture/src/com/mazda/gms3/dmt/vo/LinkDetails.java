package com.mazda.gms3.dmt.vo;

public class LinkDetails {

	private String sourceFilePath=null;
	private String innerLinkPath=null;
	private String innnerLinkDocumentId=null;
	private String mapStatus=null;
	
	
	private String sieId=null;
	private String vtocFileName=null;
	
	private String documentId=null;

	/** Other manual link: the link as sent to Kapture (innerLinkPath keeps the source link). */
	private String kaptureLinkPath=null;

	public String getKaptureLinkPath() {
		return kaptureLinkPath;
	}
	public void setKaptureLinkPath(String kaptureLinkPath) {
		this.kaptureLinkPath = kaptureLinkPath;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getSieId() {
		return sieId;
	}
	public void setSieId(String sieId) {
		this.sieId = sieId;
	}
	public String getVtocFileName() {
		return vtocFileName;
	}
	public void setVtocFileName(String vtocFileName) {
		this.vtocFileName = vtocFileName;
	}
	public String getSourceFilePath() {
		return sourceFilePath;
	}
	public void setSourceFilePath(String sourceFilePath) {
		this.sourceFilePath = sourceFilePath;
	}
	public String getInnerLinkPath() {
		return innerLinkPath;
	}
	public void setInnerLinkPath(String innerLinkPath) {
		this.innerLinkPath = innerLinkPath;
	}
	public String getInnnerLinkDocumentId() {
		return innnerLinkDocumentId;
	}
	public void setInnnerLinkDocumentId(String innnerLinkDocumentId) {
		this.innnerLinkDocumentId = innnerLinkDocumentId;
	}
	public String getMapStatus() {
		return mapStatus;
	}
	public void setMapStatus(String mapStatus) {
		this.mapStatus = mapStatus;
	}
	
	
}
