package com.mazda.gms3.cdrom.bean;

public class ApplicableVINList {

	private String modelName=null;
	private String carlineCode=null;
	private String wmiCode=null;
	private String vdsCode=null;
	private String visStartRange=null;
	private String faceLiftFolderPath=null;
	
	
	
	public String getFaceLiftFolderPath() {
		return faceLiftFolderPath;
	}
	public void setFaceLiftFolderPath(String faceLiftFolderPath) {
		this.faceLiftFolderPath = faceLiftFolderPath;
	}
	public String getModelName() {
		return modelName;
	}
	public void setModelName(String modelName) {
		this.modelName = modelName;
	}
	public String getCarlineCode() {
		return carlineCode;
	}
	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}
	public String getWmiCode() {
		return wmiCode;
	}
	public void setWmiCode(String wmiCode) {
		this.wmiCode = wmiCode;
	}
	public String getVdsCode() {
		return vdsCode;
	}
	public void setVdsCode(String vdsCode) {
		this.vdsCode = vdsCode;
	}
	public String getVisStartRange() {
		return visStartRange;
	}
	public void setVisStartRange(String visStartRange) {
		this.visStartRange = visStartRange;
	}
}