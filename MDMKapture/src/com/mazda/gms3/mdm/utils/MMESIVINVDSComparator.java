package com.mazda.gms3.mdm.utils;

import java.util.Comparator;

import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class MMESIVINVDSComparator implements Comparator<SelectItemDetails>{

	@Override
	public int compare(SelectItemDetails d1, SelectItemDetails d2) {
		return d1.getLabel().compareTo(d2.getLabel());
	}
	
	

}
