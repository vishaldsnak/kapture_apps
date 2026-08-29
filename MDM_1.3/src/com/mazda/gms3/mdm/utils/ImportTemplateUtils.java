package com.mazda.gms3.mdm.utils;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mazda.gms3.mdm.dao.ManualLanguageDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.vo.ImportTemplateDetails;

/**
 * Central registry + generator for the import templates offered on the screens.
 *
 * ONE place defines every screen's import column layout, and ONE generator turns that
 * definition into the .xlsx the user downloads. Adding the feature to another screen is
 * therefore a single entry in buildRegistry() plus the download link on that screen's JSP
 * - no new servlet and no new Excel code per screen.
 *
 * The ACTION column is appended here for EVERY screen, so the new marker column is
 * guaranteed to be the LAST column everywhere, which is exactly what the import expects.
 */
public class ImportTemplateUtils {

	static Logger logger = LogManager.getLogger(ImportTemplateUtils.class);

	/** Header of the action / marker column - always the LAST column of every template. */
	public static final String ACTION_COLUMN_HEADER = "ACTION";

	/** Values accepted in the ACTION column (also used to build the in-cell dropdown). */
	public static final String[] ACTION_VALUES = new String[] { "A", "U", "D" };

	/** Screen keys - use these on the download URL (?screen=...). */
	public static final String SCREEN_MANUAL_TYPE = "MANUAL_TYPE";
	public static final String SCREEN_CARLINE = "CARLINE";
	public static final String SCREEN_VIN = "VIN";
	public static final String SCREEN_ESI_CATEGORY = "ESI_CATEGORY";
	public static final String SCREEN_CVC_CATEGORY = "CVC_CATEGORY";
	public static final String SCREEN_ENGINE_BOOK = "ENGINE_BOOK";
	public static final String SCREEN_ENGINE_TYPE = "ENGINE_TYPE";
	public static final String SCREEN_TRANSMISSION_BOOK = "TRANSMISSION_BOOK";
	public static final String SCREEN_TRANSMISSION_TYPE = "TRANSMISSION_TYPE";
	public static final String SCREEN_ABBREVIATION_MASTER = "ABBREVIATION_MASTER";
	public static final String SCREEN_DIVISION_MASTER = "DIVISION_MASTER";
	public static final String SCREEN_SECTION_MASTER = "SECTION_MASTER";
	public static final String SCREEN_SST_IMAGE_MASTER = "SSTIMAGE_MASTER";
	public static final String SCREEN_USE_IMAGE_MASTER = "USEIMAGE_MASTER";
	public static final String SCREEN_SST_MASTER = "SST_MASTER";
	public static final String SCREEN_SST_SECTION_PARENT_MAPPING = "SST_SECTION_PARENT_MAPPING";
	public static final String SCREEN_SST_DIVISION_MODEL_MAPPING = "SST_DIVISION_MODEL_MAPPING";
	public static final String SCREEN_SST_MODEL_VIN_MAPPING = "SST_MODEL_VIN_MAPPING";
	public static final String SCREEN_SST_OEM_MAPPING = "SST_OEM_MAPPING";
	public static final String SCREEN_SST_MDM_VIN_MAPPING = "SST_MDM_VIN_MAPPING";
	public static final String SCREEN_SST_SECTION_MODEL_MAPPING = "SST_SECTION_MODEL_MAPPING";
	public static final String SCREEN_VIN_CROSS_REFERENCE = "VIN_CROSS_REFERENCE";
	public static final String SCREEN_MASTERDATA_LOCALE_MAPPING = "MASTERDATA_LOCALE_MAPPING";

	/**
	 * Variants for the screens whose columns depend on the selected country / language.
	 * MNAO = the North American markets listed in mnao.countries.locales.codes.
	 */
	public static final String VARIANT_MNAO = "MNAO";
	public static final String VARIANT_REGIONAL = "REGIONAL";

	/** Separator used to key a variant template in the registry. */
	private static final String VARIANT_SEPARATOR = "|";

	private static final Map<String, ImportTemplateDetails> TEMPLATES = buildRegistry();

	/**
	 * REGISTRY - one entry per screen that supports import.
	 *
	 * The column headers MUST be listed in the same order in which that screen's
	 * readExcelData() reads the cells (cell 0, cell 1, ...). Do NOT include the ACTION
	 * column - it is appended automatically.
	 *
	 * The wording is taken from each screen's own export writer, so a downloaded template
	 * carries the same headers the users already know. Where the export and the import
	 * disagree the IMPORT wins - the template has to load back in.
	 */
	private static Map<String, ImportTemplateDetails> buildRegistry() {
		Map<String, ImportTemplateDetails> map = new HashMap<String, ImportTemplateDetails>();

		// MANUAL TYPE - /manualtype
		put(map, new ImportTemplateDetails(SCREEN_MANUAL_TYPE, "MANUALTYPE", "MANUAL TYPE",
				new String[] { "MANUAL TYPE CODE", "MANUAL TYPE NAME", "MANUAL TYPE REF KEY" }));

		/*
		 * CARLINE - /carline - VARIANT SCREEN.
		 * Cells 0-3 and cell 6 are common; cells 4 and 5 differ:
		 *   MNAO markets  -> YEAR START  / YEAR END
		 *   other markets -> MODEL TYPE  / ESI CATEGORY FLAG
		 * This mirrors Carline.readExcelData()'s isMnaoContent() branch exactly.
		 */
		put(map, new ImportTemplateDetails(SCREEN_CARLINE, "CARLINE", "CARLINE",
				new String[] { "CARLINE CODE", "CAR NAME ENG LANG", "CAR NAME REGIONAL LANG",
						"WMI CODE", "YEAR START", "YEAR END", "IC DISPLAY CARLINE CODE" },
				VARIANT_MNAO));
		put(map, new ImportTemplateDetails(SCREEN_CARLINE, "CARLINE", "CARLINE",
				new String[] { "CARLINE CODE", "CAR NAME ENG LANG", "CAR NAME REGIONAL LANG",
						"WMI CODE", "MODEL TYPE", "ESI CATEGORY FLAG", "IC DISPLAY CARLINE CODE" },
				VARIANT_REGIONAL));

		// VIN - /vin
		put(map, new ImportTemplateDetails(SCREEN_VIN, "VIN", "VIN",
				new String[] { "MODEL", "CARLINE CODE", "GROUP", "WMI", "VDS", "VIS START",
						"VIS END", "ENGINE CODE", "TRANSMISSION_CODE" }));

		// ESI CATEGORIES - /category
		put(map, new ImportTemplateDetails(SCREEN_ESI_CATEGORY, "ESICATEGORY", "ESI CATEGORY",
				new String[] { "CATEGORY LEVEL 1 CODE", "CATEGORY LEVEL 1", "CATEGORY LEVEL 2 CODE",
						"CATEGORY LEVEL 2", "CATEGORY LEVEL 3 CODE", "CATEGORY LEVEL 3" }));

		// CVC CATEGORIES - /cvccategory
		put(map, new ImportTemplateDetails(SCREEN_CVC_CATEGORY, "CVCCATEGORY", "CVC CATEGORY",
				new String[] { "RANK", "CATEGORY CODE", "CATEGORY", "SUBCATEGORY CODE",
						"SUBCATEGORY", "SYMPTOM CODE", "SYMPTOM", "SUBSYMPTOM CODE", "SUBSYMPTOM",
						"CONDITION CODE", "CONDITION" }));

		// ENGINE BOOK - /enginebook
		put(map, new ImportTemplateDetails(SCREEN_ENGINE_BOOK, "ENGINEBOOK", "ENGINE BOOK",
				new String[] { "ENGINE BOOK CODE", "ENGINE BOOK NAME REGIONAL LANG",
						"ENGINE BOOK NAME ENG LANG" }));

		// ENGINE TYPE - /enginetype
		put(map, new ImportTemplateDetails(SCREEN_ENGINE_TYPE, "ENGINETYPE", "ENGINE TYPE",
				new String[] { "ENGINE BOOK CODE", "ENGINE TYPE CODE", "ENGINE TYPE NAME",
						"GROUP TYPE" }));

		// TRANSMISSION BOOK - /transmissionbook
		put(map, new ImportTemplateDetails(SCREEN_TRANSMISSION_BOOK, "TRANSMISSIONBOOK",
				"TRANSMISSION BOOK", new String[] { "TRANSMISSION BOOK CODE",
						"TRANSMISSION BOOK NAME REGIONAL LANG",
						"TRANSMISSION BOOK NAME ENG LANG" }));

		// TRANSMISSION TYPE - /transmissiontype
		put(map, new ImportTemplateDetails(SCREEN_TRANSMISSION_TYPE, "TRANSMISSIONTYPE",
				"TRANSMISSION TYPE", new String[] { "TRANSMISSION BOOK CODE",
						"TRANSMISSION TYPE CODE", "TRANSMISSION TYPE NAME" }));

		// ABBREVIATION - /abbreviationmaster
		put(map, new ImportTemplateDetails(SCREEN_ABBREVIATION_MASTER, "ABBREVIATION",
				"ABBREVIATION", new String[] { "CODE", "NAME" }));

		// DIVISION - /divisionmaster
		put(map, new ImportTemplateDetails(SCREEN_DIVISION_MASTER, "DIVISION", "DIVISION",
				new String[] { "CODE", "NAME", "SORT ID" }));

		// SECTION - /sectionmaster
		put(map, new ImportTemplateDetails(SCREEN_SECTION_MASTER, "SECTION", "SECTION",
				new String[] { "DIVISION CODE", "SECTION CODE", "SECTION INDEX", "SECTION NAME",
						"SORT ID", "HTML PATH" }));

		// SST IMAGE - /sstimagesmaster
		put(map, new ImportTemplateDetails(SCREEN_SST_IMAGE_MASTER, "SSTIMAGE", "SST IMAGE",
				new String[] { "SST IMAGE CODE", "SST IMAGE REVISION", "SST IMPAGE PATH" }));

		// USE IMAGE - /useimagesmaster
		put(map, new ImportTemplateDetails(SCREEN_USE_IMAGE_MASTER, "USEIMAGE", "USE IMAGE",
				new String[] { "USE IMAGE CODE", "USE IMAGE REVISION", "USE IMPAGE PATH" }));

		// SST - /sstmaster
		put(map, new ImportTemplateDetails(SCREEN_SST_MASTER, "SST", "SST",
				new String[] { "SST NUMBER", "SST NAME", "SST REVISION", "SST IMAGE CODE",
						"USE IMAGE CODE" }));

		// PARENT SST MAPPING - /sstsectionparentmapping
		put(map, new ImportTemplateDetails(SCREEN_SST_SECTION_PARENT_MAPPING, "PARENTSSTMAPPING",
				"PARENT SST MAPPING", new String[] { "SECTION CODE", "SST NUMBER", "PARENT NUMBER",
						"REMARKS" }));

		// MODEL - /divisionmodelmapping
		put(map, new ImportTemplateDetails(SCREEN_SST_DIVISION_MODEL_MAPPING, "MODEL", "MODEL",
				new String[] { "DIVISION CODE", "ESI CARLINE CODE", "VEHICLE TYPE CODE",
						"VEHICLE TYPE NAME", "SORT ID" }));

		// ADAPTIVE MODEL - /sstmodelvinmapping
		put(map, new ImportTemplateDetails(SCREEN_SST_MODEL_VIN_MAPPING, "ADAPTIVEMODEL",
				"ADAPTIVE MODEL", new String[] { "VEHICLE TYPE CODE", "VIN" }));

		// TOOL NUMBERS COMPARISION CHART - /oemmapping
		put(map, new ImportTemplateDetails(SCREEN_SST_OEM_MAPPING, "TOOLNUMBERS", "TOOL NUMBERS",
				new String[] { "SST NUMBER", "FORD NUMBER", "NISSAN NUMBER", "ISUZU NUMBER",
						"SUZUKI NUMBER" }));

		/*
		 * SST MDM VIN MAPPING - /sstmdmvinmapping
		 * NOTE - the import reads SEVEN cells (WMI CODE is cell 2) while this screen's
		 * export writes only six and leaves WMI out. The template follows the IMPORT,
		 * otherwise the downloaded file would not load back in.
		 */
		put(map, new ImportTemplateDetails(SCREEN_SST_MDM_VIN_MAPPING, "SSTMDMVIN", "SST MDM VIN",
				new String[] { "CARLINE CODE", "CARRLINE NAME", "WMI CODE", "VDS CODE",
						"VIS START RANGE", "VIS END RANGE", "SST CALINE CODE" }));

		// VEHICLE TYPES - /sectionmodelmapping
		put(map, new ImportTemplateDetails(SCREEN_SST_SECTION_MODEL_MAPPING, "VEHICLETYPES",
				"VEHICLE TYPES", new String[] { "VEHICLE TYPE CODE", "SECTION CODE", "SST NUMBER",
						"REMARKS" }));

		// MNAO MODEL YEAR CODE MAPPING - /vincrossref
		put(map, new ImportTemplateDetails(SCREEN_VIN_CROSS_REFERENCE, "VINCROSSREF",
				"VIN CROSS REFERENCE", new String[] { "VIN XREF CODE", "VIN XREF YEAR" }));

		// MASTER DATA LOCALE MAPPING - /masterdatalocalemapping
		put(map, new ImportTemplateDetails(SCREEN_MASTERDATA_LOCALE_MAPPING, "MASTERDATALOCALE",
				"MASTER DATA LOCALE", new String[] { "MARKET", "LOCALE", "MASTER DATA TYPE",
						"ALTERNATE LOCALE" }));

		return map;
	}

	/** Register a definition under its screen key, or screen key + variant. */
	private static void put(Map<String, ImportTemplateDetails> map, ImportTemplateDetails t) {
		String key = t.getScreenKey();
		if (null != t.getVariantKey() && !"".equals(t.getVariantKey())) {
			key = key + VARIANT_SEPARATOR + t.getVariantKey();
		}
		map.put(key, t);
	}

	/**
	 * @return true when this screen's template depends on the selected country / language.
	 */
	public static boolean isVariantScreen(String screenKey) {
		if (null == screenKey || "".equals(screenKey.trim())) {
			return false;
		}
		String key = screenKey.trim().toUpperCase();
		return !TEMPLATES.containsKey(key)
				&& (TEMPLATES.containsKey(key + VARIANT_SEPARATOR + VARIANT_MNAO)
						|| TEMPLATES.containsKey(key + VARIANT_SEPARATOR + VARIANT_REGIONAL));
	}

	/**
	 * @return the template definition, or null when the key is unknown - or when the screen
	 *         needs a language to pick its variant and none was supplied.
	 */
	public static ImportTemplateDetails getTemplate(String screenKey, String manualLanguageId) {
		if (null == screenKey || "".equals(screenKey.trim())) {
			return null;
		}
		String key = screenKey.trim().toUpperCase();

		// Single-layout screen.
		ImportTemplateDetails single = TEMPLATES.get(key);
		if (null != single) {
			return single;
		}

		// Variant screen - the layout cannot be guessed without the selected language.
		if (null == manualLanguageId || "".equals(manualLanguageId.trim())) {
			return null;
		}
		String variant = isMnaoLanguage(manualLanguageId) ? VARIANT_MNAO : VARIANT_REGIONAL;
		return TEMPLATES.get(key + VARIANT_SEPARATOR + variant);
	}

	/** Convenience for the screens that have a single layout. */
	public static ImportTemplateDetails getTemplate(String screenKey) {
		return getTemplate(screenKey, null);
	}

	/**
	 * Fetch a specific variant directly, without resolving it from a language. Used where
	 * the caller already knows the layout it wants.
	 */
	public static ImportTemplateDetails getTemplateForVariant(String screenKey, String variantKey) {
		if (null == screenKey || "".equals(screenKey.trim())) {
			return null;
		}
		String key = screenKey.trim().toUpperCase();
		if (null == variantKey || "".equals(variantKey.trim())) {
			return TEMPLATES.get(key);
		}
		return TEMPLATES.get(key + VARIANT_SEPARATOR + variantKey.trim().toUpperCase());
	}

	/**
	 * Decide whether the selected manual language belongs to the MNAO markets, using the
	 * SAME rule the screens use (Carline.readParamsFromRequest): take the language code,
	 * turn "en-US" into "en_US" and look it up in mnao.countries.locales.codes.
	 */
	public static boolean isMnaoLanguage(String manualLanguageId) {
		try {
			if (null == manualLanguageId || "".equals(manualLanguageId.trim())) {
				return false;
			}
			String langCode = ManualLanguageDAO.getManualLanguageCode(manualLanguageId.trim());
			if (null == langCode || "".equals(langCode.trim())) {
				return false;
			}
			langCode = langCode.trim().replace("-", "_");
			String configured = ApplicationProperties.getProperty("mnao.countries.locales.codes");
			if (null == configured || "".equals(configured.trim())) {
				return false;
			}
			StringTokenizer str = new StringTokenizer(configured, ",");
			while (str.hasMoreTokens()) {
				String tok = str.nextToken();
				if (null != tok && tok.trim().equalsIgnoreCase(langCode)) {
					return true;
				}
			}
		} catch (Exception e) {
			logger.info("isMnaoLanguage() :: Exception :: >" + e.getMessage());
		}
		return false;
	}

	/**
	 * Build the download file name, e.g. MANUALTYPE_TEMPLATE_26072026_113045.xlsx
	 */
	public static String buildFileName(ImportTemplateDetails template) {
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
		String stamp = sdf.format(new Date());
		String base = (null != template.getFileBaseName() && !"".equals(template.getFileBaseName()))
				? template.getFileBaseName() : "IMPORT";
		return base + "_TEMPLATE_" + stamp + ".xlsx";
	}

	/**
	 * Generate the template workbook in memory.
	 *
	 * Sheet 1 holds the header row the import reads (data columns + ACTION). Sheet 2
	 * documents the accepted ACTION values so the user does not have to be told
	 * separately - unknown values are rejected by the import.
	 *
	 * @return the .xlsx bytes, ready to stream to the browser.
	 */
	public static byte[] buildTemplateWorkbook(ImportTemplateDetails template) throws Exception {
		XSSFWorkbook workBook = null;
		ByteArrayOutputStream os = null;
		try {
			workBook = new XSSFWorkbook();

			/*
			 * HEADER STYLE - bold on a light fill so the header row is obvious.
			 *
			 * POI 3.17 REMOVED the short-constant styling API this used to call:
			 * Font.setBoldweight(Font.BOLDWEIGHT_BOLD), CellStyle.SOLID_FOREGROUND and
			 * CellStyle.BORDER_THIN are gone, replaced by setBold(boolean) and the
			 * FillPatternType / BorderStyle enums. Same rendering, type-safe arguments.
			 */
			Font headerFont = workBook.createFont();
			headerFont.setBold(true);
			CellStyle headerStyle = workBook.createCellStyle();
			headerStyle.setFont(headerFont);
			headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			headerStyle.setBorderBottom(BorderStyle.THIN);

			String sheetName = (null != template.getSheetName()
					&& !"".equals(template.getSheetName())) ? template.getSheetName() : "IMPORT";
			XSSFSheet sheet = workBook.createSheet(sheetName);

			String[] dataHeaders = template.getColumnHeaders();
			int columnCount = (null == dataHeaders ? 0 : dataHeaders.length) + 1;

			Row headerRow = sheet.createRow(0);
			for (int i = 0; null != dataHeaders && i < dataHeaders.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(dataHeaders[i]);
				cell.setCellStyle(headerStyle);
			}
			// ACTION IS ALWAYS THE LAST COLUMN
			Cell actionCell = headerRow.createCell(columnCount - 1);
			actionCell.setCellValue(ACTION_COLUMN_HEADER);
			actionCell.setCellStyle(headerStyle);

			// Column widths + a frozen header row so the layout is usable as-is.
			for (int i = 0; i < columnCount; i++) {
				sheet.setColumnWidth(i, 25 * 256);
			}
			sheet.createFreezePane(0, 1);

			addActionDropDown(sheet, columnCount - 1);
			addInstructionsSheet(workBook, headerStyle, template, columnCount);

			os = new ByteArrayOutputStream();
			workBook.write(os);
			os.flush();
			return os.toByteArray();
		} finally {
			if (null != os) {
				try {
					os.close();
				} catch (Exception e) {
					// nothing meaningful to do while closing an in-memory stream
				}
			}
			workBook = null;
		}
	}

	/**
	 * Put an A / U / D dropdown on the ACTION column so the value cannot be mistyped -
	 * the import rejects the WHOLE file when it finds an unknown action value.
	 *
	 * Guarded: several POI versions are on the classpath and data validation is not
	 * available in all of them. If it is unavailable the template is still perfectly
	 * usable, just without the dropdown.
	 */
	private static void addActionDropDown(XSSFSheet sheet, int actionColumnIndex) {
		try {
			XSSFDataValidationHelper helper = new XSSFDataValidationHelper(sheet);
			XSSFDataValidationConstraint constraint = (XSSFDataValidationConstraint) helper
					.createExplicitListConstraint(ACTION_VALUES);
			// rows 1..500 (row 0 is the header)
			CellRangeAddressList range = new CellRangeAddressList(1, 500, actionColumnIndex,
					actionColumnIndex);
			XSSFDataValidation validation = (XSSFDataValidation) helper.createValidation(
					constraint, range);
			validation.setShowErrorBox(true);
			sheet.addValidationData(validation);
		} catch (Throwable t) {
			// Template stays valid without the dropdown - do not fail the download.
			logger.info("addActionDropDown() :: dropdown not added :: >" + t.getMessage());
		}
	}

	/**
	 * Second sheet explaining how the template is filled in.
	 */
	private static void addInstructionsSheet(XSSFWorkbook workBook, CellStyle headerStyle,
			ImportTemplateDetails template, int columnCount) {
		Sheet sheet = workBook.createSheet("INSTRUCTIONS");
		sheet.setColumnWidth(0, 22 * 256);
		sheet.setColumnWidth(1, 90 * 256);

		int r = 0;
		Row header = sheet.createRow(r++);
		Cell h0 = header.createCell(0);
		h0.setCellValue("COLUMN");
		h0.setCellStyle(headerStyle);
		Cell h1 = header.createCell(1);
		h1.setCellValue("DESCRIPTION");
		h1.setCellStyle(headerStyle);

		String[] dataHeaders = template.getColumnHeaders();
		for (int i = 0; null != dataHeaders && i < dataHeaders.length; i++) {
			Row row = sheet.createRow(r++);
			row.createCell(0).setCellValue(dataHeaders[i]);
			row.createCell(1).setCellValue("Mandatory unless the screen states otherwise.");
		}

		Row actionRow = sheet.createRow(r++);
		actionRow.createCell(0).setCellValue(ACTION_COLUMN_HEADER);
		actionRow.createCell(1).setCellValue(
				"A or ADD - the row is created. U or UPDATE - the row is updated. "
						+ "D or DELETE - the row is deleted. Leaving it blank behaves like A / U.");

		r++;
		String[] notes = new String[] {
				"Do NOT remove, rename or reorder the columns on the '" + template.getSheetName()
						+ "' sheet - the import reads them by position.",
				"Do NOT delete the header row. Data starts from the second row.",
				"A and U both create the row when it does not exist yet and update it when it does, "
						+ "so the exact one of the two does not have to be known in advance.",
				"D removes the row that matches the same unique combination the screen uses; "
						+ "the row is deactivated, not erased.",
				"An unknown value in the ACTION column, or the same record listed twice, "
						+ "stops the whole file before anything is saved.",
				"This sheet is ignored by the import and can be left in the file." };
		for (int i = 0; i < notes.length; i++) {
			Row row = sheet.createRow(r++);
			row.createCell(0).setCellValue(i == 0 ? "NOTES" : "");
			row.createCell(1).setCellValue(notes[i]);
		}
	}
}
