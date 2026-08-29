package com.mazda.gms3.cdrom.bean;

public class TransactionReportBean {
	String manualType="";
	String documentId="";
	String destinationLocation="";
	int innerLinkCount;
	String allInnerLinkMapped="";
	int okAssetCount;
	String allOkAssetMapped="";
	String status="";
	String failureReason="";
	public String getManualType() {
		return manualType;
	}
	public void setManualType(String manualType) {
		this.manualType = manualType;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getDestinationLocation() {
		return destinationLocation;
	}
	public void setDestinationLocation(String destinationLocation) {
		this.destinationLocation = destinationLocation;
	}
	public int getInnerLinkCount() {
		return innerLinkCount;
	}
	public void setInnerLinkCount(int innerLinkCount) {
		this.innerLinkCount = innerLinkCount;
	}
	public String getAllInnerLinkMapped() {
		return allInnerLinkMapped;
	}
	public void setAllInnerLinkMapped(String allInnerLinkMapped) {
		this.allInnerLinkMapped = allInnerLinkMapped;
	}
	public int getOkAssetCount() {
		return okAssetCount;
	}
	public void setOkAssetCount(int okAssetCount) {
		this.okAssetCount = okAssetCount;
	}
	public String getAllOkAssetMapped() {
		return allOkAssetMapped;
	}
	public void setAllOkAssetMapped(String allOkAssetMapped) {
		this.allOkAssetMapped = allOkAssetMapped;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getFailureReason() {
		return failureReason;
	}
	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}
}
