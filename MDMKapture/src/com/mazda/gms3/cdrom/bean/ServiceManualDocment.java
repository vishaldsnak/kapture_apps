package com.mazda.gms3.cdrom.bean;

import java.util.ArrayList;
import java.util.List;

public class ServiceManualDocment {

	String title;
	String content;
	List<LabelBean> attchments = new ArrayList<LabelBean>();
	String DJVUFileLocation;
	String attachment = null;
	public String getAttachment() {
		return attachment;
	}
	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}
	public String getDJVUFileLocation() {
		return DJVUFileLocation;
	}
	public void setDJVUFileLocation(String dJVUFileLocation) {
		DJVUFileLocation = dJVUFileLocation;
	}
	public List<LabelBean> getAttchments() {
		return attchments;
	}
	public void setAttchments(List<LabelBean> attchments) {
		this.attchments = attchments;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
}