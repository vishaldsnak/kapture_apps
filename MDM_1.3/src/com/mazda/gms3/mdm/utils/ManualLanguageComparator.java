package com.mazda.gms3.mdm.utils;

import java.util.Comparator;

import com.mazda.gms3.mdm.vo.ManualLanguageDetails;

public class ManualLanguageComparator implements Comparator<ManualLanguageDetails>{

	@Override
	public int compare(ManualLanguageDetails o1, ManualLanguageDetails o2) {
		return o1.getManualLanguageCode().compareTo(o2.getManualLanguageCode());
	}

	
}
