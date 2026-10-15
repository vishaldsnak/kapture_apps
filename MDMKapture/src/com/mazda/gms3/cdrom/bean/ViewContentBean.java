package com.mazda.gms3.cdrom.bean;

public class ViewContentBean{

	String documentId = null;
	String displayLevel1 = null;
	String displayLevel2 = null;
	String displayLevel3 = null;
	String displayLevel4 = null;
	String displayLevel5 = null;
	String displayLevel6 = null;
	String displayLevel7 = null;
	String sequencenumber = null;
	String title = null;
	String manualTypeName = null;
	String filePath = null;
	String generateDocument = null;
	
	public String getGenerateDocument() {
		return generateDocument;
	}
	public void setGenerateDocument(String generateDocument) {
		this.generateDocument = generateDocument;
	}
	
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getManualTypeName() {
		return manualTypeName;
	}
	public void setManualTypeName(String manualTypeName) {
		this.manualTypeName = manualTypeName;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getDisplayLevel1() {
		return displayLevel1;
	}
	public void setDisplayLevel1(String displayLevel1) {
		this.displayLevel1 = displayLevel1;
	}
	public String getDisplayLevel2() {
		return displayLevel2;
	}
	public void setDisplayLevel2(String displayLevel2) {
		this.displayLevel2 = displayLevel2;
	}
	public String getDisplayLevel3() {
		return displayLevel3;
	}
	public void setDisplayLevel3(String displayLevel3) {
		this.displayLevel3 = displayLevel3;
	}
	public String getDisplayLevel4() {
		return displayLevel4;
	}
	public void setDisplayLevel4(String displayLevel4) {
		this.displayLevel4 = displayLevel4;
	}
	public String getDisplayLevel5() {
		return displayLevel5;
	}
	public void setDisplayLevel5(String displayLevel5) {
		this.displayLevel5 = displayLevel5;
	}
	public String getSequencenumber() {
		return sequencenumber;
	}
	public void setSequencenumber(String sequencenumber) {
		this.sequencenumber = sequencenumber;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDisplayLevel6() {
		return displayLevel6;
	}
	public void setDisplayLevel6(String displayLevel6) {
		this.displayLevel6 = displayLevel6;
	}
	public String getDisplayLevel7() {
		return displayLevel7;
	}
	public void setDisplayLevel7(String displayLevel7) {
		this.displayLevel7 = displayLevel7;
	}
	public void reset(){
		this.documentId="";
		this.displayLevel1="";
		this.displayLevel2="";
		this.displayLevel3="";
		this.displayLevel4="";
		this.displayLevel5="";
		this.displayLevel6="";
		this.displayLevel7="";
		this.manualTypeName="";
		this.generateDocument="";
		this.filePath="";
		this.title="";
		this.sequencenumber="";
		
	}
}
