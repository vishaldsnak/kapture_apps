package com.mazda.gms3.dmt.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatter {

	public static String parseStringToDate_DD_MMM_YYYY(String stringDate) {
		String convertedDate = "";
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			Date d = sdf.parse(stringDate);
			/*
			 * now convert d to string date in format dd - MMM - yyyy
			 */
			sdf = new SimpleDateFormat("dd - MMM - yyyy");
			convertedDate = sdf.format(d);
			// set d to null
			d = null;
			// set sdf to null
			sdf = null;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return convertedDate;
	}
	
	public static String parseDateToString_YYYY_MM_DD(Date date) {
		String convertedDate = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			convertedDate = sdf.format(date);
			// set sdf to null
			sdf = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return convertedDate;
	}
	
	public static String converTimeStampToString(Timestamp ts) {
		String convertedTime = "";
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm a");
			convertedTime = sdf.format(ts);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return convertedTime;
	}

}
