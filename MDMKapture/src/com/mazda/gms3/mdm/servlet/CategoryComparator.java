package com.mazda.gms3.mdm.servlet;

import java.util.Comparator;

import com.mazda.gms3.mdm.vo.CategoryDetails;

public class CategoryComparator implements Comparator<CategoryDetails>{

	public int compare(CategoryDetails o1, CategoryDetails o2) {
		return Boolean.compare(o1.isEditableFlag(),o2.isEditableFlag());
	}
}
