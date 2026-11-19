package com.mazda.gms3.mdm.utils;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Splits an export report over several Excel files when it has more rows than one Excel may hold
 * (RMI Export + MNAO Data Export - document, files, inner links and view content reports).
 *
 * The first Excel always keeps the report name as is - reportName.xlsx.
 * A report that does not fit continues in reportName_1.xlsx, reportName_2.xlsx ... each with the
 * same header row, so no row is lost and the report is always generated.
 * A report that has a summary row gets one summary row per Excel (same naming).
 *
 * The number of data rows per Excel comes from application.properties (no default in code).
 */
public class ExportReportSplit
{
	/** Data rows one .xlsx sheet can hold besides the header row (Excel sheet = 1,048,576 rows). */
	private static final int EXCEL_SHEET_DATA_ROWS = 1048575;

	private ExportReportSplit()
	{
	}

	/**
	 * Data rows allowed in one Excel - REQUIRED key, 1 or more and not above what an Excel sheet holds.
	 * A missing or wrong value throws, so the report step stops with a clear message in the log.
	 */
	public static int maxRowsPerExcel(String propertyKey)
	{
		int maxRows = ApplicationProperties.requirePositiveInt(propertyKey);
		if (maxRows > EXCEL_SHEET_DATA_ROWS)
		{
			throw new IllegalStateException("application.properties :: key {" + propertyKey + "} = {" + maxRows
					+ "} is above the rows one Excel sheet can hold {" + EXCEL_SHEET_DATA_ROWS + "}.");
		}
		return maxRows;
	}

	/** Part 1 = reportName.xlsx as is, part 2 = reportName_1.xlsx, part 3 = reportName_2.xlsx ... */
	public static String partFileName(String fileName, int partNo)
	{
		if (partNo <= 1)
		{
			return fileName;
		}
		int dot = fileName.lastIndexOf(".");
		if (dot < 0)
		{
			return fileName + "_" + (partNo - 1);
		}
		return fileName.substring(0, dot) + "_" + (partNo - 1) + fileName.substring(dot);
	}

	/** Summary name of a part - part 1 = the report name as is, part 2 = REPORT NAME_1 ... (as the file names). */
	public static String partReportName(String reportName, int partNo)
	{
		if (partNo <= 1)
		{
			return reportName;
		}
		return reportName + "_" + (partNo - 1);
	}

	/** Header row (row 0) - labels printed with _ replaced by space, as the report writers do. */
	public static void writeHeader(Sheet sheet, String[] headerLabels)
	{
		Row headerRow = sheet.createRow(0);
		if (null != headerLabels && headerLabels.length > 0)
		{
			Cell headerCell = null;
			for (int a = 0; a < headerLabels.length; a++)
			{
				headerCell = headerRow.createCell(a);
				headerCell.setCellValue(headerLabels[a].replace("_", " "));
				headerCell = null;
			}
		}
	}

	/** The current Excel is full: write it to disk and release it. */
	public static void closeFullPart(SXSSFWorkbook workBook, File partFile) throws Exception
	{
		FileOutputStream os = null;
		try
		{
			os = new FileOutputStream(partFile);
			workBook.write(os);
			os.flush();
		}
		finally
		{
			if (null != os)
			{
				os.close();
			}
			workBook.dispose();
		}
	}
}
