package com.mazda.gms3.dmt.publish;

import com.mazda.gms3.dmt.kapture.KaptureApiClient;

/**
 * THE MARKET OF A PUBLISH CONTENT JOB - MC, MME or MNAO - and the names that differ between them.
 *
 * A publish job belongs to the market of the DMT conversion job it is scheduled from, which is
 * told by that job's name (schedule.name.mc.key / schedule.name.mme.key / schedule.name.key).
 *
 *                     MC                              MME                              MNAO
 *   publish job name  schedule.name.publish.mc.key    schedule.name.publish.mme.key    schedule.name.publish.mnao.key
 *   document table    gms3_dmt_mc_[newm_]imdoc        gms3_dmt_<locale>_[nm_]imdoc     gms3_dmt_<locale>_imdoc
 *   active flag       DC_DOC_STATUS                   DC_DOC_STATUS                    DC_IM_DOC_FLAG
 *   published flag    DC_IM_DOC_PUBLISHED_STATUS      DC_IM_DOC_PUBLISHED_STATUS       DC_IM_DOC_STATUS
 *   view content      gms3_vc_japan_vin_details       gms3_vc_mme_vin_dtl_<locale>     gms3_vc_model_year_details (VC_MY_)
 *                                                                                      + gms3_vc_vin_details (VC_VIN_)
 */
public final class PublishMarket {

	public static final PublishMarket MC = new PublishMarket("market.mc", "schedule.name.mc.key", "schedule.name.publish.mc.key", false);
	public static final PublishMarket MME = new PublishMarket("market.mme", "schedule.name.mme.key", "schedule.name.publish.mme.key", true);
	public static final PublishMarket MNAO = new PublishMarket("market.mnao", "schedule.name.key", "schedule.name.publish.mnao.key", true);

	private final String marketKey;
	private final String conversionNameKey;
	private final String publishNameKey;
	private final boolean tablesPerLocale;

	private PublishMarket(String marketKey, String conversionNameKey, String publishNameKey, boolean tablesPerLocale) {
		this.marketKey = marketKey;
		this.conversionNameKey = conversionNameKey;
		this.publishNameKey = publishNameKey;
		this.tablesPerLocale = tablesPerLocale;
	}

	/** The market of a DMT conversion job by its name; null for a job of any other kind. */
	public static PublishMarket ofConversionJob(String scheduleName) {
		if (null == scheduleName) {
			return null;
		}
		if (scheduleName.startsWith(KaptureApiClient.require(MC.conversionNameKey))) {
			return MC;
		}
		if (scheduleName.startsWith(KaptureApiClient.require(MME.conversionNameKey))) {
			return MME;
		}
		if (scheduleName.startsWith(KaptureApiClient.require(MNAO.conversionNameKey))) {
			return MNAO;
		}
		return null;
	}

	/** MC / MME / MNAO - the value of the DC_MARKET column. */
	public String market() {
		return KaptureApiClient.require(marketKey);
	}

	/** Name prefix (and thread prefix) of the conversion jobs of the market. */
	public String conversionPrefix() {
		return KaptureApiClient.require(conversionNameKey);
	}

	/** Name prefix (and thread prefix) of the publish jobs of the market. */
	public String publishPrefix() {
		return KaptureApiClient.require(publishNameKey);
	}

	private boolean mnao() {
		return this == MNAO;
	}

	/**
	 * The DMT document table of a document (as MCDocumentManagementDAO / MMEDocumentManagementDAO /
	 * MNAODocumentManagementDAO). MNAO has no model type.
	 *
	 * @param locale Kapture locale of the document (en_UK / en-UK alike); used by MME and MNAO
	 */
	public String documentTable(String modelType, String locale) {
		if (mnao()) {
			return "gms3_dmt_" + tableLocale(locale) + "_imdoc";
		}
		boolean newModel = null != modelType && modelType.trim().equalsIgnoreCase(KaptureApiClient.require("model.type.new"));
		if (!tablesPerLocale) {
			return newModel ? "gms3_dmt_mc_newm_imdoc" : "gms3_dmt_mc_imdoc";
		}
		return "gms3_dmt_" + tableLocale(locale) + "_" + (newModel ? "nm_" : "") + "imdoc";
	}

	/** The column of the document table holding the active / deleted flag (flag.value.active ...). */
	public String activeFlagColumn() {
		return mnao() ? "DC_IM_DOC_FLAG" : "DC_DOC_STATUS";
	}

	/** The column of the document table holding the Kapture published / draft flag. */
	public String publishedFlagColumn() {
		return mnao() ? "DC_IM_DOC_STATUS" : "DC_IM_DOC_PUBLISHED_STATUS";
	}

	/** The carline code column of the document table, or NULL - MNAO documents have none. */
	public String carlineColumn() {
		return mnao() ? "NULL" : "DC_CARLINE_CODE";
	}

	/**
	 * The view content tables of a document, each as { table, column prefix } - the locale column is
	 * <prefix>LOCALE, the document column <prefix>DOCUMENT_ID, the status column VC_CONTENT_STATUS.
	 */
	public String[][] viewContentTables(String locale) {
		if (mnao()) {
			return new String[][] { { "gms3_vc_model_year_details", "VC_MY_" }, { "gms3_vc_vin_details", "VC_VIN_" } };
		}
		return new String[][] { { tablesPerLocale ? "gms3_vc_mme_vin_dtl_" + tableLocale(locale) : "gms3_vc_japan_vin_details", "VC_VIN_" } };
	}

	private static String tableLocale(String locale) {
		if (null == locale || "".equals(locale.trim())) {
			throw new IllegalArgumentException("no locale for the per-locale tables");
		}
		return locale.trim().replace('-', '_').toLowerCase();
	}

	@Override
	public String toString() {
		return market();
	}
}
