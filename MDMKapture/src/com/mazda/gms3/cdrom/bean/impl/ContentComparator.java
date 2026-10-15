package com.mazda.gms3.cdrom.bean.impl;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

import com.mazda.gms3.cdrom.bean.ViewContentBean;

public class ContentComparator implements Comparator<Object>{

	
	public int compare(Object o1,Object o2){  
		ViewContentBean s1=(ViewContentBean)o1;  
		ViewContentBean s2=(ViewContentBean)o2;  
		 if(StringUtils.isNotBlank(s1.getSequencenumber()) && StringUtils.isNotBlank(s2.getSequencenumber())){
			 return s1.getSequencenumber().compareTo(s2.getSequencenumber());  
		 }else if(StringUtils.isBlank(s1.getSequencenumber()) && StringUtils.isBlank(s2.getSequencenumber())){
			 return 0;
		 }else if(StringUtils.isBlank(s1.getSequencenumber())){
			 return -1;
		 }
		 return +1;
	}  
}
