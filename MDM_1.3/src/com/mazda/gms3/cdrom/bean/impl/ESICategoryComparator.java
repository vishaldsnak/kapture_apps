package com.mazda.gms3.cdrom.bean.impl;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

import com.mazda.gms3.mdm.vo.CategoryDetails;

public class ESICategoryComparator implements Comparator<Object>{

	
	public int compare(Object o1,Object o2){  
		CategoryDetails s1=(CategoryDetails)o1;  
		CategoryDetails s2=(CategoryDetails)o2;  
		 if(StringUtils.isNotBlank(s1.getCategoryCode()) && StringUtils.isNotBlank(s2.getCategoryCode())){
			 return s1.getCategoryCode().compareTo(s2.getCategoryCode());  
		 }else if(StringUtils.isBlank(s1.getCategoryCode()) && StringUtils.isBlank(s2.getCategoryCode())){
			 return 0;
		 }else if(StringUtils.isBlank(s1.getCategoryCode())){
			 return -1;
		 }
		 return +1;
	}  
}
