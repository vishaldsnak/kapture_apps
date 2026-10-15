package com.mazda.gms3.cdrom.bean;

public class LabelBean {

	String key;
	String value;
	String extraAttribute;
	/*
	 * THE ARTICLE ID THE CONTENT CAME FROM - e.g. "SM1027".
	 *
	 * Set by CDRomKaptureContentDAO, which knows it because the article id IS part of the path the
	 * XML was read from. It replaces the old CDRomManualsDAO.getDocumentID(recordId) lookup, which
	 * existed only because OK_IM.CONTENTDATA held the XML under a record id and nothing else.
	 */
	String documentId;
//	String modelTypeForCarline;
	
	
	
//	public String getModelTypeForCarline() {
//		return modelTypeForCarline;
//	}
//	public void setModelTypeForCarline(String modelTypeForCarline) {
//		this.modelTypeForCarline = modelTypeForCarline;
//	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getDocumentId() {
		return documentId;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public String getExtraAttribute() {
		return extraAttribute;
	}
	public void setExtraAttribute(String extraAttribute) {
		this.extraAttribute = extraAttribute;
	}
	
	
}
