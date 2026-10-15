package com.mazda.gms3.mdm.utils;

import java.util.Comparator;

import com.mazda.gms3.mdm.vo.ModuleDetails;

public class ModuleComparator implements Comparator<ModuleDetails>{

	@Override
	public int compare(ModuleDetails p1, ModuleDetails p2) {
		if(null!=p1.getDisplayOrder() && null!=p2.getDisplayOrder())
		{
			return p1.getDisplayOrder().compareTo(p2.getDisplayOrder());
		}
		else
		{
			return 0;
		}
		
	}
	
	

}
