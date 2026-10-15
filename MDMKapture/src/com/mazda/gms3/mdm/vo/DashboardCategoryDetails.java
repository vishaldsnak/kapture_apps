package com.mazda.gms3.mdm.vo;

import java.util.ArrayList;

/**
 * ONE CATEGORY TAB ON MY PAGE (DASHBOARD), HOLDING THE TILES THAT BELONG TO IT.
 *
 * A CATEGORY IS ONLY HANDED TO THE JSP WHEN IT HAS AT LEAST ONE TILE, SO A USER WHOSE ROLE GIVES
 * NO ACCESS TO - SAY - THE SST SCREENS SIMPLY DOES NOT GET AN SST TAB.
 */
public class DashboardCategoryDetails {

	private String categoryKey = null;
	private String title = null;
	private String description = null;

	private ArrayList<DashboardTileDetails> tilesList = new ArrayList<DashboardTileDetails>();

	public String getCategoryKey() {
		return categoryKey;
	}

	public void setCategoryKey(String categoryKey) {
		this.categoryKey = categoryKey;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArrayList<DashboardTileDetails> getTilesList() {
		return tilesList;
	}

	public void setTilesList(ArrayList<DashboardTileDetails> tilesList) {
		this.tilesList = tilesList;
	}

	// CONVENIENCE FOR THE JSP - THE TAB SHOWS THE TILE COUNT
	public int getTileCount() {
		return null == tilesList ? 0 : tilesList.size();
	}

	/*
	 * THE INLINE SVG DRAWN ON THIS CATEGORY TAB, RESOLVED ONCE IN DashboardIcons.
	 *
	 * IT IS MARKUP, NOT TEXT, SO dashboard.jsp PRINTS IT WITH ${...} AND NOT <c:out> - c:out
	 * WOULD ESCAPE THE ANGLE BRACKETS AND PRINT THE SVG SOURCE ONTO THE PAGE. THE VALUE IS
	 * NEVER USER SUPPLIED; IT COMES FROM A FIXED TABLE OF CONSTANTS.
	 */
	private String icon = "";

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}
}
