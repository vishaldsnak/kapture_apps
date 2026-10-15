package com.mazda.gms3.mdm.utils;

import java.util.Comparator;

import com.mazda.gms3.mdm.vo.CountryLocaleDetails;

public class CountryLocaleComparator implements Comparator<CountryLocaleDetails>{

	@Override
	public int compare(CountryLocaleDetails p1, CountryLocaleDetails p2) {
		return p1.getCountryLocaleCode().compareTo(p2.getCountryLocaleCode());
	}

	
}
