package com.mazda.gms3.mdm.vo;

/**
 * ONE TILE ON MY PAGE (DASHBOARD).
 *
 * THE DASHBOARD USED TO RENDER com.mazda.gms3.mdm.vo.ModuleDetails DIRECTLY AND DECIDE THE
 * NAVIGATION TYPE / DISPLAY NAME WITH A LONG c:if CHAIN INSIDE dashboard.jsp. THAT DECISION IS
 * NOW MADE ONCE IN com.mazda.gms3.mdm.utils.DashboardCategories AND CARRIED HERE, SO THE JSP
 * ONLY LOOPS AND PRINTS.
 *
 * NOTE - navType / refKey ARE EXACTLY THE TWO VALUES THE Dashboard SERVLET ALREADY EXPECTS IN
 * DASH_TILE_CLICKED_VAL AND DASH_TILE_CLICKED_REFKEY_VAL. NOTHING ABOUT THE POST CONTRACT
 * CHANGES.
 */
public class DashboardTileDetails {

	// DASH_TILE_CLICKED_VAL - DEFAULT / NORMAL / ENGINE / MISSION / VEHICLE_TYPE /
	// SST_MAINTENANCE / SST_VEHICLE_MAINTENANCE / CD_CREATION / RUMVIN
	private String navType = null;

	// DASH_TILE_CLICKED_REFKEY_VAL - ONLY READ BY THE SERVLET FOR DEFAULT / NORMAL, EMPTY OTHERWISE
	private String refKey = "";

	// THE KEY USED TO LOOK THE TILE UP IN THE CATEGORY MAP AND IN THE PROPERTIES FILE
	private String tileKey = null;

	private String title = null;
	private String description = null;

	public String getNavType() {
		return navType;
	}

	public void setNavType(String navType) {
		this.navType = navType;
	}

	public String getRefKey() {
		return refKey;
	}

	public void setRefKey(String refKey) {
		this.refKey = refKey;
	}

	public String getTileKey() {
		return tileKey;
	}

	public void setTileKey(String tileKey) {
		this.tileKey = tileKey;
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

	/*
	 * THE INLINE SVG DRAWN ON THIS TILE, RESOLVED ONCE IN DashboardIcons.
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
