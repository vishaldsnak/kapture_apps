package com.mazda.gms3.dmt.masterdata;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.ApplicationProperties;
import com.mazda.gms3.dmt.utils.DBConnectionHelper;
import com.mazda.gms3.dmt.utils.PathUtil;
import com.mazda.gms3.dmt.utils.Utilities;

/**
 * LOADS THE MASTER DATA EXCEL FILES OF ONE LOCALE INTO THE MDM TABLES.
 *
 * File name:  <country>_<locale>_<type>.xlsx      e.g. JP_JA-JP_CARLINE.xlsx
 * Sheet:      masterdata.sheet.name               (EXPORTED DATA); the first row is the header
 * Last column masterdata.indicator.column         (Indicator): A = add, U = update, D = delete,
 *             blank = add or update
 *
 * A and U are handled the same way: the row is looked up by the values that identify it and
 * is inserted when it is not there, updated when something differs, and left alone when
 * nothing differs. A row that is inserted or changed becomes ACTIVE with sync status N - it
 * is then sent to Kapture by KaptureCategorySync, which sets the sync status to Y.
 * D marks the row deleted and clears its sync status; nothing is removed in Kapture.
 *
 * The files are loaded in the order of TYPES: a carline before its VIN ranges, a book before
 * its types.
 *
 * CARLINE has two layouts, the two variants of the MDM import template, told apart by the header
 * of column 5: MC / MME  ... | WMI CODE | MODEL TYPE | ESI CATEGORY FLAG | IC DISPLAY CARLINE CODE
 *              MNAO      ... | WMI CODE | YEAR START | YEAR END          | IC DISPLAY CARLINE CODE
 * DRIVELINE_AXLE_TYPE and BODY_TYPE (all markets) use the MDM Drive Axle / Body Type import template.
 */
public class MdmExcelLoader {

	private static Logger logger = LogManager.getLogger(MdmExcelLoader.class);

	public static final String TYPE_CARLINE = "CARLINE";
	public static final String TYPE_CAT = "CAT";
	public static final String TYPE_ENGINE_BOOK = "ENGINE_BOOK";
	public static final String TYPE_ENGINE_TYPE = "ENGINE_TYPE";
	public static final String TYPE_TRANSMISSION_BOOK = "TRANSMISSION_BOOK";
	public static final String TYPE_TRANSMISSION_TYPE = "TRANSMISSION_TYPE";
	public static final String TYPE_ALL_VIN = "ALL_VIN";
	public static final String TYPE_DRIVE_AXLE = "DRIVELINE_AXLE_TYPE";
	public static final String TYPE_BODY_TYPE = "BODY_TYPE";

	/** Load order, and the number of data columns (the Indicator column not counted) of each type. */
	private static final String[] TYPES = { TYPE_CARLINE, TYPE_CAT, TYPE_ENGINE_BOOK, TYPE_ENGINE_TYPE,
			TYPE_TRANSMISSION_BOOK, TYPE_TRANSMISSION_TYPE, TYPE_DRIVE_AXLE, TYPE_BODY_TYPE, TYPE_ALL_VIN };
	private static final int[] DATA_COLUMNS = { 7, 6, 3, 4, 3, 3, 3, 3, 9 };

	/** Header of CARLINE column 5 in the MNAO variant of the template (YEAR START / YEAR END). */
	private static final String CARLINE_MNAO_COLUMN_5 = "YEAR START";

	private static final Pattern FILE_NAME = Pattern.compile("^([A-Za-z]{2})_([A-Za-z]{2}-[A-Za-z]{2})_(.+)\\.xlsx?$");

	/** One line per Excel row handled (and one per file that could not be read). */
	public final List<MasterDataRecord> records = new ArrayList<MasterDataRecord>();
	/** The files found for the locale, by type. */
	public final Map<String, File> filesFound = new LinkedHashMap<String, File>();

	private final String active = ApplicationProperties.getProperty("flag.value.active");
	private final String deleted = ApplicationProperties.getProperty("flag.value.delete");

	private String locale;
	private String countryCode;
	private long clId;
	private long mlId;
	/** The CARLINE file being loaded has the MNAO layout (YEAR START / YEAR END). */
	private boolean carlineYears;

	private static class Spec {
		final String table, pk, flag, created, updated;

		Spec(String table, String pk, String prefix) {
			this.table = table;
			this.pk = pk;
			this.flag = prefix + "_flag";
			this.created = prefix + "_created_tmstp";
			this.updated = prefix + "_updated_tmstp";
		}
	}

	private static final Spec CARLINE = new Spec("gms3_mdm_carline_codes", "mdm_crln_code_id", "mdm_crln");
	private static final Spec CATEGORY = new Spec("gms3_mdm_category", "mdm_cat_id", "mdm_cat");
	private static final Spec ENGINE_BOOK = new Spec("gms3_mdm_engine_book", "mdm_eb_id", "mdm_eb");
	private static final Spec ENGINE_TYPE = new Spec("gms3_mdm_engine_type", "mdm_et_id", "mdm_et");
	private static final Spec TRANS_BOOK = new Spec("gms3_mdm_trans_book", "mdm_transbk_id", "mdm_transbk");
	private static final Spec TRANS_TYPE = new Spec("gms3_mdm_trans_type", "mdm_trans_type_id", "mdm_trans_type");
	private static final Spec VIN = new Spec("gms3_mdm_vin_detail", "mdm_vin_id", "mdm_vin");
	private static final Spec AXLE_TYPE = new Spec("gms3_mdm_axle_type", "mdm_at_id", "mdm_at");
	private static final Spec BODY_TYPE = new Spec("gms3_mdm_body_type", "mdm_bt_id", "mdm_bt");

	/**
	 * @param masterDataFolder the folder holding the Excel files
	 * @param locale           locale of the content being loaded, e.g. ja-JP
	 * @return false when nothing could be loaded at all (no folder, no file for the locale, or
	 *         the locale is not known to MDM); the reason is in records
	 */
	public boolean load(File masterDataFolder, String locale) {
		this.locale = locale;
		if (null == masterDataFolder || !masterDataFolder.isDirectory()) {
			fileFailure("", "", "Master data folder not found: " + (null == masterDataFolder ? "" : PathUtil.winPath(masterDataFolder)));
			return false;
		}
		File[] files = masterDataFolder.listFiles();
		Map<String, File> byType = new LinkedHashMap<String, File>();
		if (null != files) {
			for (int i = 0; i < files.length; i++) {
				Matcher m = FILE_NAME.matcher(files[i].getName());
				if (files[i].isFile() && m.matches() && m.group(2).equalsIgnoreCase(locale)) {
					countryCode = m.group(1).toUpperCase();
					byType.put(m.group(3).trim().toUpperCase(), files[i]);
				}
			}
		}
		if (byType.isEmpty()) {
			fileFailure("", "", "No master data file found for locale " + locale + " in " + PathUtil.winPath(masterDataFolder));
			return false;
		}

		Connection conn = null;
		try {
			conn = DBConnectionHelper.getConnection();
			if (!resolveLocaleIds(conn)) {
				fileFailure("", "", "Country " + countryCode + " / manual language " + locale + " is not set up in MDM.");
				return false;
			}
			for (int t = 0; t < TYPES.length; t++) {
				File file = byType.remove(TYPES[t]);
				if (null != file) {
					filesFound.put(TYPES[t], file);
					loadFile(conn, TYPES[t], DATA_COLUMNS[t], file);
				}
			}
			for (Map.Entry<String, File> other : byType.entrySet()) {
				fileFailure(other.getKey(), other.getValue().getName(), "Unknown master data type - file not loaded.");
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(MdmExcelLoader.class.getName(), "load()", e);
			fileFailure("", "", "Master data load stopped: " + e.getMessage());
		} finally {
			try {
				if (null != conn) {
					conn.close();
				}
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(MdmExcelLoader.class.getName(), "load()", e);
			}
		}
		return true;
	}

	public long getManualLanguageId() {
		return mlId;
	}

	private boolean resolveLocaleIds(Connection conn) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT c.mdm_cl_id, m.mdm_ml_id FROM gms3_mdm_country_locale c"
					+ " JOIN gms3_mdm_manual_language m ON m.mdm_cl_id = c.mdm_cl_id"
					+ " WHERE UPPER(TRIM(c.mdm_cl_locale_code)) = ? AND LOWER(TRIM(m.mdm_ml_lang_code)) = ?"
					+ " AND c.mdm_cl_flag = ? AND m.mdm_ml_flag = ?");
			ps.setString(1, countryCode);
			ps.setString(2, locale.toLowerCase());
			ps.setString(3, active);
			ps.setString(4, active);
			rs = ps.executeQuery();
			if (rs.next()) {
				clId = rs.getLong(1);
				mlId = rs.getLong(2);
				return true;
			}
			return false;
		} finally {
			close(rs, ps);
		}
	}

	// ---- one file ----------------------------------------------------------

	private void loadFile(Connection conn, String type, int dataColumns, File file) {
		logger.info("loadFile :: Loading master data file {" + file.getName() + "} as " + type);
		InputStream in = null;
		Workbook workbook = null;
		try {
			in = PathUtil.fileInputStream(file);
			workbook = WorkbookFactory.create(in);
			Sheet sheet = workbook.getSheet(ApplicationProperties.getProperty("masterdata.sheet.name").trim());
			if (null == sheet) {
				fileFailure(type, file.getName(), "Sheet '" + ApplicationProperties.getProperty("masterdata.sheet.name").trim() + "' not found.");
				return;
			}
			DataFormatter formatter = new DataFormatter();
			Row header = sheet.getRow(0);
			String indicatorHeader = null == header ? "" : cell(formatter, header, dataColumns);
			if (!ApplicationProperties.getProperty("masterdata.indicator.column").trim().equalsIgnoreCase(indicatorHeader)) {
				fileFailure(type, file.getName(), "Column " + (dataColumns + 1) + " of the header must be '"
						+ ApplicationProperties.getProperty("masterdata.indicator.column").trim() + "' - the file does not have the layout of " + type + ".");
				return;
			}
			carlineYears = TYPE_CARLINE.equals(type) && CARLINE_MNAO_COLUMN_5.equalsIgnoreCase(cell(formatter, header, 4));
			if (carlineYears) {
				logger.info("loadFile :: {" + file.getName() + "} has the MNAO CARLINE layout (YEAR START / YEAR END)");
			}
			for (int r = 1; r <= sheet.getLastRowNum(); r++) {
				Row row = sheet.getRow(r);
				if (null == row) {
					continue;
				}
				String[] v = new String[dataColumns];
				boolean empty = true;
				for (int c = 0; c < dataColumns; c++) {
					v[c] = cell(formatter, row, c);
					if (!"".equals(v[c])) {
						empty = false;
					}
				}
				if (empty) {
					continue;
				}
				MasterDataRecord rec = new MasterDataRecord();
				rec.type = type;
				rec.fileName = file.getName();
				rec.locale = locale;
				rec.rowNumber = r + 1;
				rec.indicator = cell(formatter, row, dataColumns);
				try {
					String ind = rec.indicator.toUpperCase();
					boolean delete = "D".equals(ind) || "DELETE".equals(ind);
					if (!delete && !"".equals(ind) && !"A".equals(ind) && !"ADD".equals(ind) && !"U".equals(ind) && !"UPDATE".equals(ind)) {
						throw new IllegalArgumentException("Indicator must be A, U or D.");
					}
					loadRow(conn, type, v, delete, rec);
				} catch (Exception e) {
					rec.result = MasterDataRecord.RESULT_FAILED;
					rec.message = null == e.getMessage() ? e.toString() : e.getMessage();
					if (!(e instanceof IllegalArgumentException)) {
						Utilities.printStackTraceToLogs(MdmExcelLoader.class.getName(), "loadFile()", e);
					}
				}
				records.add(rec);
			}
			logger.info("loadFile :: {" + file.getName() + "} finished :: " + summary(file.getName()));
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(MdmExcelLoader.class.getName(), "loadFile()", e);
			fileFailure(type, file.getName(), "File could not be read: " + e.getMessage());
		} finally {
			try {
				if (null != workbook) {
					workbook.close();
				}
				if (null != in) {
					in.close();
				}
			} catch (Exception e) {
				Utilities.printStackTraceToLogs(MdmExcelLoader.class.getName(), "loadFile()", e);
			}
		}
	}

	private void loadRow(Connection conn, String type, String[] v, boolean delete, MasterDataRecord rec) throws SQLException {
		Map<String, Object> scope = new LinkedHashMap<String, Object>();
		Map<String, String> keys = new LinkedHashMap<String, String>();
		Map<String, String> values = new LinkedHashMap<String, String>();
		Map<String, Object> insertOnly = new LinkedHashMap<String, Object>();
		scope.put("mdm_cl_id", Long.valueOf(clId));
		scope.put("mdm_ml_id", Long.valueOf(mlId));
		Spec spec;

		if (TYPE_CARLINE.equals(type) && carlineYears) {
			// MNAO: CARLINE CODE | CAR NAME ENG LANG | CAR NAME REGIONAL LANG | WMI CODE | YEAR START | YEAR END | IC DISPLAY CARLINE CODE
			// unique as in MDM: carline code + English name + WMI + year start
			require(v[0], "CARLINE CODE");
			require(v[1], "CAR NAME ENG LANG");
			require(v[4], "YEAR START");
			year(v[4], "YEAR START");
			if (!"".equals(v[5])) {
				year(v[5], "YEAR END");
				if (Integer.parseInt(v[5]) < Integer.parseInt(v[4])) {
					throw new IllegalArgumentException("YEAR END is before YEAR START.");
				}
			}
			spec = CARLINE;
			rec.key = v[0] + " / " + v[1] + " / " + v[3] + " / " + v[4];
			keys.put("mdm_crln_code", v[0]);
			keys.put("mdm_crln_name_eng_lang", v[1]);
			keys.put("mdm_crln_wmi_code", v[3]);
			keys.put("mdm_crln_year_start", v[4]);
			values.put("mdm_crln_name_regional_lang", v[2]);
			values.put("mdm_crln_year_end", v[5]);
			values.put("mdm_ic_display_carline_code", v[6]);
			insertOnly.put("mdm_cl_locale_code", countryCode);
			insertOnly.put("mdm_ml_lang_code", locale);
		} else if (TYPE_DRIVE_AXLE.equals(type)) {
			// DRIVE AXLE CODE | DRIVE AXLE NAME REGIONAL LANG | DRIVE AXLE NAME ENG LANG
			require(v[0], "DRIVE AXLE CODE");
			spec = AXLE_TYPE;
			rec.key = v[0];
			keys.put("mdm_at_axle_type", v[0]);
			values.put("mdm_at_axle_type_desc_reg", v[1]);
			values.put("mdm_at_axle_type_desc_eng", v[2]);
		} else if (TYPE_BODY_TYPE.equals(type)) {
			// BODY TYPE CODE | BODY TYPE NAME REGIONAL LANG | BODY TYPE NAME ENG LANG
			require(v[0], "BODY TYPE CODE");
			spec = BODY_TYPE;
			rec.key = v[0];
			keys.put("mdm_bt_body_type", v[0]);
			values.put("mdm_bt_body_type_desc_reg", v[1]);
			values.put("mdm_bt_body_type_desc_eng", v[2]);
		} else if (TYPE_CARLINE.equals(type)) {
			// CARLINE CODE | CAR NAME ENG LANG | CAR NAME REGIONAL LANG | WMI CODE | MODEL TYPE | ESI CATEGORY FLAG | IC DISPLAY CARLINE CODE
			require(v[0], "CARLINE CODE");
			require(v[1], "CAR NAME ENG LANG");
			spec = CARLINE;
			rec.key = v[0] + " / " + v[1] + " / " + v[3] + " / " + v[4];
			keys.put("mdm_crln_code", v[0]);
			keys.put("mdm_crln_name_eng_lang", v[1]);
			keys.put("mdm_crln_wmi_code", v[3]);
			keys.put("mdm_crln_model_type", v[4]);
			values.put("mdm_crln_name_regional_lang", v[2]);
			values.put("mdm_crln_esi_cat_flag", v[5]);
			values.put("mdm_ic_display_carline_code", v[6]);
			insertOnly.put("mdm_cl_locale_code", countryCode);
			insertOnly.put("mdm_ml_lang_code", locale);
		} else if (TYPE_CAT.equals(type)) {
			// LEVEL 1 CODE | LEVEL 1 | LEVEL 2 CODE | LEVEL 2 | LEVEL 3 CODE | LEVEL 3
			require(v[0], "CATEGORY LEVEL 1 CODE");
			spec = CATEGORY;
			rec.key = v[0] + " / " + v[2] + " / " + v[4];
			keys.put("mdm_cat_code", v[0]);
			keys.put("mdm_scat_code", v[2]);
			keys.put("mdm_sscat_code", v[4]);
			values.put("mdm_cat_name_eng_lang", v[1]);
			values.put("mdm_scat_name_eng_lang", v[3]);
			values.put("mdm_sscat_name_eng_lang", v[5]);
		} else if (TYPE_ENGINE_BOOK.equals(type)) {
			// ENGINE BOOK CODE | NAME REGIONAL LANG | NAME ENG LANG
			require(v[0], "ENGINE BOOK CODE");
			spec = ENGINE_BOOK;
			rec.key = v[0];
			keys.put("mdm_eb_code", v[0]);
			values.put("mdm_eb_name_regional_lang", v[1]);
			values.put("mdm_eb_name_eng_lang", v[2]);
		} else if (TYPE_ENGINE_TYPE.equals(type)) {
			// ENGINE BOOK CODE | ENGINE TYPE CODE | ENGINE TYPE NAME | GROUP TYPE
			require(v[0], "ENGINE BOOK CODE");
			require(v[1], "ENGINE TYPE CODE");
			spec = ENGINE_TYPE;
			rec.key = v[0] + " / " + v[1];
			scope.put("mdm_eb_id", Long.valueOf(bookId(conn, ENGINE_BOOK, "mdm_eb_code", v[0], "Engine book")));
			keys.put("mdm_et_type_code", v[1]);
			values.put("mdm_et_type_name", v[2]);
			values.put("mdm_et_group_type", v[3]);
		} else if (TYPE_TRANSMISSION_BOOK.equals(type)) {
			// TRANSMISSION BOOK CODE | NAME REGIONAL LANG | NAME ENG LANG
			require(v[0], "TRANSMISSION BOOK CODE");
			spec = TRANS_BOOK;
			rec.key = v[0];
			keys.put("mdm_transbk_code", v[0]);
			values.put("mdm_transbk_name_regional_lang", v[1]);
			values.put("mdm_transbk_name_eng_lang", v[2]);
		} else if (TYPE_TRANSMISSION_TYPE.equals(type)) {
			// TRANSMISSION BOOK CODE | TRANSMISSION TYPE CODE | TRANSMISSION TYPE NAME
			require(v[0], "TRANSMISSION BOOK CODE");
			require(v[1], "TRANSMISSION TYPE CODE");
			spec = TRANS_TYPE;
			rec.key = v[0] + " / " + v[1];
			scope.put("mdm_transbk_id", Long.valueOf(bookId(conn, TRANS_BOOK, "mdm_transbk_code", v[0], "Transmission book")));
			keys.put("mdm_trans_type_type_code", v[1]);
			values.put("mdm_trans_type_type_name", v[2]);
		} else {
			// MODEL | CARLINE CODE | GROUP | WMI | VDS | VIS START | VIS END | ENGINE CODE | TRANSMISSION_CODE
			require(v[0], "MODEL");
			require(v[1], "CARLINE CODE");
			require(v[4], "VDS");
			require(v[5], "VIS START");
			require(v[6], "VIS END");
			spec = VIN;
			v[6] = v[6].toUpperCase();
			rec.key = v[1] + " / " + v[3] + " / " + v[4] + " / " + v[5] + " / " + v[6];
			keys.put("mdm_crln_name_eng_lang", v[0]);
			keys.put("mdm_crln_code", v[1]);
			keys.put("mdm_vin_group_code", v[2]);
			keys.put("mdm_vin_wmi_code", v[3]);
			keys.put("mdm_vin_vds_code", v[4]);
			keys.put("mdm_vin_vis_start_range", v[5]);
			keys.put("mdm_vin_vis_end_range", v[6]);
			values.put("mdm_vin_engine_code", v[7]);
			values.put("mdm_vin_transmission_code", v[8]);
			if (!delete) {
				// THE CARLINE OF THE VIN RANGE MUST BE THERE - its regional name is part of the category name
				String[] carline = carlineOf(conn, v[1], v[0]);
				values.put("mdm_crln_name_regional_lang", carline[1]);
				values.put("mdm_ic_display_carline_code", carline[2]);
				insertOnly.put("mdm_crln_code_id", Long.valueOf(carline[0]));
				insertOnly.put("mdm_cl_locale_code", countryCode);
				insertOnly.put("mdm_ml_lang_code", locale);
			}
		}
		rec.result = upsert(conn, spec, scope, keys, values, insertOnly, delete);
	}

	// ---- the one write path --------------------------------------------------

	private String upsert(Connection conn, Spec s, Map<String, Object> scope, Map<String, String> keys,
			Map<String, String> values, Map<String, Object> insertOnly, boolean delete) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// THE ROW AS IT IS NOW (a deleted row is never revived - a new row is inserted instead):
			// from the rows of the table read once for this scope, else the one-row lookup
			Map<String, List<Existing>> rows = existingRows(conn, s, scope, keys.keySet(), values.keySet());
			String rowKey = rowKey(keys.values());
			List<Existing> matches = rows.get(rowKey);
			Existing row = null == matches || matches.isEmpty() ? lookup(conn, s, scope, keys, values.keySet()) : matches.get(0);
			boolean found = null != row;
			long id = 0;
			boolean changed = false;
			if (found) {
				id = row.id;
				changed = !active.equals(row.flag) || (!"Y".equals(row.sync) && !"N".equals(row.sync));
				for (String col : values.keySet()) {
					if (!nz(row.values.get(col)).trim().equals(values.get(col))) {
						changed = true;
					}
				}
			}
			int i;
			StringBuilder sql;
			Timestamp now = new Timestamp(System.currentTimeMillis());

			if (delete) {
				if (!found) {
					throw new IllegalArgumentException("No record found to delete.");
				}
				ps = conn.prepareStatement("UPDATE " + s.table + " SET " + s.flag + " = ?, mdm_sync_status = NULL, "
						+ s.updated + " = ? WHERE " + s.pk + " = ?");
				ps.setString(1, deleted);
				ps.setTimestamp(2, now);
				ps.setLong(3, id);
				ps.executeUpdate();
				forget(rows, id);
				return MasterDataRecord.RESULT_DELETED;
			}
			if (found && !changed) {
				return MasterDataRecord.RESULT_NO_CHANGE;
			}
			if (found) {
				sql = new StringBuilder("UPDATE " + s.table + " SET " + s.flag + " = ?, mdm_sync_status = 'N', " + s.updated + " = ?");
				for (String col : values.keySet()) {
					sql.append(", ").append(col).append(" = ?");
				}
				sql.append(" WHERE ").append(s.pk).append(" = ?");
				ps = conn.prepareStatement(sql.toString());
				i = 1;
				ps.setString(i++, active);
				ps.setTimestamp(i++, now);
				for (String val : values.values()) {
					ps.setString(i++, emptyToNull(val));
				}
				ps.setLong(i++, id);
				ps.executeUpdate();
				// a later row of the file with the same key sees the row as it is now
				Existing current = findById(rows, id);
				if (null != current) {
					current.flag = active;
					current.sync = "N";
					for (Map.Entry<String, String> e : values.entrySet()) {
						current.values.put(e.getKey(), emptyToNull(e.getValue()));
					}
				}
				return MasterDataRecord.RESULT_UPDATED;
			}

			StringBuilder cols = new StringBuilder(s.flag + ", mdm_sync_status, " + s.created);
			StringBuilder marks = new StringBuilder("?, 'N', ?");
			List<Object> params = new ArrayList<Object>();
			params.add(active);
			params.add(now);
			for (Map.Entry<String, Object> e : scope.entrySet()) {
				cols.append(", ").append(e.getKey());
				marks.append(", ?");
				params.add(e.getValue());
			}
			for (Map.Entry<String, String> e : keys.entrySet()) {
				cols.append(", ").append(e.getKey());
				marks.append(", ?");
				params.add(e.getValue().trim());
			}
			for (Map.Entry<String, String> e : values.entrySet()) {
				cols.append(", ").append(e.getKey());
				marks.append(", ?");
				params.add(emptyToNull(e.getValue()));
			}
			for (Map.Entry<String, Object> e : insertOnly.entrySet()) {
				cols.append(", ").append(e.getKey());
				marks.append(", ?");
				params.add(e.getValue());
			}
			ps = conn.prepareStatement("INSERT INTO " + s.table + " (" + cols + ") VALUES (" + marks + ")", java.sql.Statement.RETURN_GENERATED_KEYS);
			for (int p = 0; p < params.size(); p++) {
				ps.setObject(p + 1, params.get(p));
			}
			ps.executeUpdate();
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// a later row of the file with the same key finds the new row (it has the highest id)
				Existing added = new Existing(rs.getLong(1), active, "N");
				for (Map.Entry<String, String> e : values.entrySet()) {
					added.values.put(e.getKey(), emptyToNull(e.getValue()));
				}
				List<Existing> list = rows.get(rowKey);
				if (null == list) {
					list = new ArrayList<Existing>();
					rows.put(rowKey, list);
				}
				list.add(added);
			} else {
				// the new row cannot be remembered: the scope is read again at its next use
				existing.remove(scopeKey(s, scope));
			}
			return MasterDataRecord.RESULT_ADDED;
		} finally {
			close(rs, ps);
		}
	}

	// ---- the rows already in a table ---------------------------------------
	//
	// Looking a row up with TRIM(LOWER(..)) on every key column cannot use an index: each Excel row
	// read the whole table (gms3_mdm_category ~26,000 rows, ~250 ms a row on MC Dev). The rows of a
	// scope (country + manual language, and the book for engine / transmission types) are read ONCE
	// and the Excel rows are matched in memory on the same trimmed lower-case key, lowest id first -
	// what the lookup returned. A row not matched that way still gets the one-row lookup (lookup),
	// so the database collation decides as before (e.g. a key that differs only in an accent).

	/** A row already in the table: id, flag, sync status and the compared value columns. */
	private static class Existing {
		final long id;
		String flag, sync;
		final Map<String, String> values = new LinkedHashMap<String, String>();

		Existing(long id, String flag, String sync) {
			this.id = id;
			this.flag = flag;
			this.sync = sync;
		}
	}

	/** scope -> key -> the rows with that key, lowest id first */
	private final Map<String, Map<String, List<Existing>>> existing = new LinkedHashMap<String, Map<String, List<Existing>>>();

	private Map<String, List<Existing>> existingRows(Connection conn, Spec s, Map<String, Object> scope, java.util.Collection<String> keyColumns,
			java.util.Collection<String> valueColumns) throws SQLException {
		String scopeKey = scopeKey(s, scope);
		Map<String, List<Existing>> rows = existing.get(scopeKey);
		if (null != rows) {
			return rows;
		}
		rows = new LinkedHashMap<String, List<Existing>>();
		StringBuilder sql = new StringBuilder("SELECT " + s.pk + ", " + s.flag + ", mdm_sync_status");
		for (String col : valueColumns) {
			sql.append(", ").append(col);
		}
		for (String col : keyColumns) {
			sql.append(", ").append(col);
		}
		sql.append(" FROM ").append(s.table).append(" WHERE ").append(s.flag).append(" <> ?");
		for (String col : scope.keySet()) {
			sql.append(" AND ").append(col).append(" = ?");
		}
		sql.append(" ORDER BY ").append(s.pk);
		PreparedStatement ps = null;
		ResultSet rs = null;
		long started = System.currentTimeMillis();
		try {
			ps = conn.prepareStatement(sql.toString());
			int i = 1;
			ps.setString(i++, deleted);
			for (Object val : scope.values()) {
				ps.setObject(i++, val);
			}
			rs = ps.executeQuery();
			int n = 0;
			while (rs.next()) {
				Existing row = new Existing(rs.getLong(1), rs.getString(2), rs.getString(3));
				for (String col : valueColumns) {
					row.values.put(col, rs.getString(col));
				}
				List<String> key = new ArrayList<String>();
				for (String col : keyColumns) {
					// the database side of TRIM(LOWER(IFNULL(col,''))): TRIM removes spaces only
					key.add(nz(rs.getString(col)).toLowerCase().replaceAll("^ +| +$", ""));
				}
				String k = String.join("\u0000", key);
				List<Existing> list = rows.get(k);
				if (null == list) {
					list = new ArrayList<Existing>();
					rows.put(k, list);
				}
				list.add(row);
				n++;
			}
			logger.info("existingRows :: " + s.table + " :: " + n + " row(s) read for " + scope + " :: " + (System.currentTimeMillis() - started) + " ms");
		} finally {
			close(rs, ps);
		}
		existing.put(scopeKey, rows);
		return rows;
	}

	/** The key of an Excel row - the parameter side of the lookup: trimmed and lower case. */
	private static String rowKey(java.util.Collection<String> keyValues) {
		List<String> key = new ArrayList<String>();
		for (String v : keyValues) {
			key.add(v.trim().toLowerCase());
		}
		return String.join("\u0000", key);
	}

	private static String scopeKey(Spec s, Map<String, Object> scope) {
		return s.table + scope;
	}

	/** THE ONE-ROW LOOKUP (the rule before the rows were read once) - for a row not matched in memory. */
	private Existing lookup(Connection conn, Spec s, Map<String, Object> scope, Map<String, String> keys, java.util.Collection<String> valueColumns)
			throws SQLException {
		StringBuilder sql = new StringBuilder("SELECT " + s.pk + ", " + s.flag + ", mdm_sync_status");
		for (String col : valueColumns) {
			sql.append(", ").append(col);
		}
		sql.append(" FROM ").append(s.table).append(" WHERE ").append(s.flag).append(" <> ?");
		for (String col : scope.keySet()) {
			sql.append(" AND ").append(col).append(" = ?");
		}
		for (String col : keys.keySet()) {
			sql.append(" AND TRIM(LOWER(IFNULL(").append(col).append(",''))) = ?");
		}
		sql.append(" ORDER BY ").append(s.pk).append(" LIMIT 1");
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			int i = 1;
			ps.setString(i++, deleted);
			for (Object val : scope.values()) {
				ps.setObject(i++, val);
			}
			for (String val : keys.values()) {
				ps.setString(i++, val.trim().toLowerCase());
			}
			rs = ps.executeQuery();
			if (!rs.next()) {
				return null;
			}
			Existing row = new Existing(rs.getLong(1), rs.getString(2), rs.getString(3));
			for (String col : valueColumns) {
				row.values.put(col, rs.getString(col));
			}
			return row;
		} finally {
			close(rs, ps);
		}
	}

	private static Existing findById(Map<String, List<Existing>> rows, long id) {
		for (List<Existing> list : rows.values()) {
			for (Existing e : list) {
				if (e.id == id) {
					return e;
				}
			}
		}
		return null;
	}

	/** A deleted row is no longer found. */
	private static void forget(Map<String, List<Existing>> rows, long id) {
		for (List<Existing> list : rows.values()) {
			for (java.util.Iterator<Existing> it = list.iterator(); it.hasNext();) {
				if (it.next().id == id) {
					it.remove();
					return;
				}
			}
		}
	}

	private long bookId(Connection conn, Spec book, String codeColumn, String code, String label) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT " + book.pk + " FROM " + book.table + " WHERE mdm_cl_id = ? AND mdm_ml_id = ?"
					+ " AND TRIM(LOWER(" + codeColumn + ")) = ? AND " + book.flag + " <> ? ORDER BY " + book.pk + " LIMIT 1");
			ps.setLong(1, clId);
			ps.setLong(2, mlId);
			ps.setString(3, code.trim().toLowerCase());
			ps.setString(4, deleted);
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getLong(1);
			}
			throw new IllegalArgumentException(label + " " + code + " not found in MDM.");
		} finally {
			close(rs, ps);
		}
	}

	/** @return { carline code id, regional name, IC display carline code } */
	private String[] carlineOf(Connection conn, String carlineCode, String englishName) throws SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = conn.prepareStatement("SELECT mdm_crln_code_id, mdm_crln_name_regional_lang, mdm_ic_display_carline_code"
					+ " FROM gms3_mdm_carline_codes WHERE mdm_cl_id = ? AND mdm_ml_id = ? AND TRIM(LOWER(mdm_crln_code)) = ?"
					+ " AND TRIM(LOWER(mdm_crln_name_eng_lang)) = ? AND mdm_crln_flag <> ? ORDER BY mdm_crln_code_id LIMIT 1");
			ps.setLong(1, clId);
			ps.setLong(2, mlId);
			ps.setString(3, carlineCode.trim().toLowerCase());
			ps.setString(4, englishName.trim().toLowerCase());
			ps.setString(5, deleted);
			rs = ps.executeQuery();
			if (rs.next()) {
				return new String[] { String.valueOf(rs.getLong(1)), nz(rs.getString(2)).trim(), nz(rs.getString(3)).trim() };
			}
			throw new IllegalArgumentException("Carline " + carlineCode + " (" + englishName + ") not found in MDM.");
		} finally {
			close(rs, ps);
		}
	}

	// ---- helpers -------------------------------------------------------------

	private void fileFailure(String type, String fileName, String message) {
		MasterDataRecord rec = new MasterDataRecord();
		rec.type = type;
		rec.fileName = fileName;
		rec.locale = locale;
		rec.result = MasterDataRecord.RESULT_FAILED;
		rec.message = message;
		records.add(rec);
		logger.info("MASTER DATA LOAD :: " + fileName + " :: " + message);
	}

	/** e.g. ADDED 3, NO CHANGE 300, FAILED 1 - for the rows of one file. */
	private String summary(String fileName) {
		Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
		for (MasterDataRecord rec : records) {
			if (fileName.equals(rec.fileName)) {
				counts.put(rec.result, Integer.valueOf(counts.containsKey(rec.result) ? counts.get(rec.result).intValue() + 1 : 1));
				if (rec.failed()) {
					logger.info("loadFile :: {" + fileName + "} ROW " + rec.rowNumber + " [" + rec.key + "] FAILED :: " + rec.message);
				}
			}
		}
		return counts.toString();
	}

	private static void require(String value, String column) {
		if (null == value || "".equals(value)) {
			throw new IllegalArgumentException(column + " is empty.");
		}
	}

	/** A year as MDM accepts it: four digits, not 0. */
	private static void year(String value, String column) {
		if (!value.matches("\\d{4}") || Integer.parseInt(value) == 0) {
			throw new IllegalArgumentException(column + " must be a year of four digits.");
		}
	}

	private static String cell(DataFormatter formatter, Row row, int index) {
		String s = formatter.formatCellValue(row.getCell(index));
		return null == s ? "" : s.trim();
	}

	private static String nz(String s) {
		return null == s ? "" : s;
	}

	private static String emptyToNull(String s) {
		return null == s || "".equals(s) ? null : s;
	}

	private static void close(ResultSet rs, PreparedStatement ps) {
		try {
			if (null != rs) {
				rs.close();
			}
			if (null != ps) {
				ps.close();
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(MdmExcelLoader.class.getName(), "close()", e);
		}
	}
}
