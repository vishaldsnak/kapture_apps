package com.mazda.gms3.mdm.utils;

import java.util.Comparator;

import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class VDSComparator implements Comparator<SelectItemDetails>{

	public int compare(SelectItemDetails p1, SelectItemDetails p2) {
		if(p1.getVdsType().equalsIgnoreCase(p2.getVdsType()))
        {
            return p1.getVdsType().compareTo(p2.getVdsType());
        }
        return p1.getVdsType().compareTo(p2.getVdsType());
	}	

}
