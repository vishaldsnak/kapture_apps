package com.mazda.gms3.cdrom.bean;

public class KeyWordSearchBean {

	String title= null ;
	String ESICat1 = null;
	String ESICat2 = null;
	String ESICat3 = null;
	String manualType = null;
	String documentId = null;
	String filePath =null;
	String manualTypeName =null;
	String generateDocument = null;
	
	public String getGenerateDocument() {
		return generateDocument;
	}
	public void setGenerateDocument(String generateDocument) {
		this.generateDocument = generateDocument;
	}
	public String getManualTypeName() {
		return manualTypeName;
	}
	public void setManualTypeName(String manualTypeName) {
		this.manualTypeName = manualTypeName;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getManualType() {
		return manualType;
	}
	public void setManualType(String manualType) {
		this.manualType = manualType;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getESICat1() {
		return ESICat1;
	}
	public void setESICat1(String eSICat1) {
		ESICat1 = eSICat1;
	}
	public String getESICat2() {
		return ESICat2;
	}
	public void setESICat2(String eSICat2) {
		ESICat2 = eSICat2;
	}
	public String getESICat3() {
		return ESICat3;
	}
	public void setESICat3(String eSICat3) {
		ESICat3 = eSICat3;
	}
	
	public void reset(){
		this.documentId="";
		this.ESICat1="";
		this.ESICat2="";
		this.ESICat3="";
		this.manualType="";
		this.manualTypeName="";
		this.generateDocument="";
		this.filePath="";
		this.title="";
	}
}
