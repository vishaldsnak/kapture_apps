package com.mazda.gms3.mdm.utils;

import java.util.Comparator;

import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class CustomComparator implements Comparator<SelectItemDetails> {

	@Override
    public int compare(SelectItemDetails p1, SelectItemDetails p2)
    {
        if(p1.getLabel().equalsIgnoreCase(p2.getLabel()))
        {
            return p1.getValue().compareTo(p2.getValue());
        }
        return p1.getLabel().compareTo(p2.getLabel());
    }	
}
