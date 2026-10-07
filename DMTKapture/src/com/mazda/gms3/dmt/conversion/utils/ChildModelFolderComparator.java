package com.mazda.gms3.dmt.conversion.utils;

import java.util.Comparator;

import com.mazda.gms3.dmt.vo.ChildModelFolderDetails;

public class ChildModelFolderComparator implements Comparator<ChildModelFolderDetails>{

	@Override
	public int compare(ChildModelFolderDetails ch1, ChildModelFolderDetails ch2) {
		if(ch1.getYear().equalsIgnoreCase(ch2.getYear()))
        {
            return ch1.getAlphabet().compareTo(ch2.getAlphabet());
        }
        return ch1.getYear().compareTo(ch2.getYear());
	}
	

}
