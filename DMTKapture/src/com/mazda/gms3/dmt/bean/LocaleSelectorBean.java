package com.mazda.gms3.dmt.bean;

import java.util.ArrayList;

import com.mazda.gms3.dmt.vo.SelectItemDetails;



public class LocaleSelectorBean {

	private ArrayList<SelectItemDetails> localeList = null;
	private String selectedLocale=null;
	
	public ArrayList<SelectItemDetails> getLocaleList() {
		return localeList;
	}
	public void setLocaleList(ArrayList<SelectItemDetails> localeList) {
		this.localeList = localeList;
	}
	public String getSelectedLocale() {
		return selectedLocale;
	}
	public void setSelectedLocale(String selectedLocale) {
		this.selectedLocale = selectedLocale;
	}
}
