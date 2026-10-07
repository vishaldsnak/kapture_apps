package com.mazda.gms3.dmt.mc.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.mc.dao.MCDocumentManagementDAO;
import com.mazda.gms3.dmt.mc.vo.ESICategoryDetails;
import com.mazda.gms3.dmt.utils.ApplicationProperties;

/**
 * WHICH DOCUMENTS OF A MATERIAL FOLDER HAVE BEEN WITHDRAWN (MC, MME and MNAO markets).
 *
 * There is no delete text file any more. The ESI category text file of the material folder
 * lists every document the folder holds now; a document the database still has as ACTIVE for
 * the folder and that the file no longer lists has been withdrawn and is to be deleted.
 *
 * Used both when a schedule is created (to count the documents for deletion) and by the
 * conversion job (to delete them), so that the two can never disagree.
 */
public class WithdrawnDocumentsFinder {

	private static Logger logger = LogManager.getLogger(WithdrawnDocumentsFinder.class);

	/**
	 * @param esiCategoryDetailsList the rows of the material folder's ESI category text file
	 * @return one entry per withdrawn document: { document id, DC_SOURCE_NETWORK_LOC }.
	 *         EMPTY - nothing is deleted - when the file has no valid row: an empty or
	 *         unreadable file must never remove a whole folder.
	 */
	public static List<String[]> find(ArrayList<ESICategoryDetails> esiCategoryDetailsList, String locale,
			String modelFolderName, String manualType, String faceLiftFolderName, String materialFolderName,
			String modelType) throws Exception
	{
		return find(esiCategoryDetailsList, locale, modelFolderName, manualType, faceLiftFolderName, materialFolderName, modelType, false);
	}

	/**
	 * @param mme true for the MME market: the documents are looked up in the locale's own
	 *            tables (gms3_dmt_<locale>_[nm_]imdoc) instead of the MC tables
	 */
	public static List<String[]> find(ArrayList<ESICategoryDetails> esiCategoryDetailsList, String locale,
			String modelFolderName, String manualType, String faceLiftFolderName, String materialFolderName,
			String modelType, boolean mme) throws Exception
	{
		return find(esiCategoryDetailsList, locale, modelFolderName, manualType, faceLiftFolderName, materialFolderName, modelType,
				ApplicationProperties.getProperty(mme ? "market.mme" : "market.mc"));
	}

	/**
	 * @param market market.mc / market.mme / market.mnao: whose document tables are looked up -
	 *               MC gms3_dmt_mc_[newm_]imdoc, MME gms3_dmt_<locale>_[nm_]imdoc, MNAO gms3_dmt_<locale>_imdoc
	 *               (MNAO has no model type)
	 */
	public static List<String[]> find(ArrayList<ESICategoryDetails> esiCategoryDetailsList, String locale,
			String modelFolderName, String manualType, String faceLiftFolderName, String materialFolderName,
			String modelType, String market) throws Exception
	{
		boolean mme = null!=market && market.trim().equalsIgnoreCase(ApplicationProperties.getProperty("market.mme").trim());
		boolean mnao = null!=market && market.trim().equalsIgnoreCase(ApplicationProperties.getProperty("market.mnao").trim());
		List<String[]> withdrawn = new ArrayList<String[]>();

		/*
		 * EVERY DOCUMENT THE FILE LISTS. A Wiring Diagram document can be stored without its
		 * extension and under the html folder while the file lists the pdf (or the reverse), so
		 * each listed path is also kept without extension and with the two folders swapped.
		 * A stored document that matches any of these forms is treated as LISTED - when in
		 * doubt a document is kept, never deleted.
		 */
		Set<String> listedPaths = new HashSet<String>();
		String htmlFolder = "\\" + ApplicationProperties.getProperty("directory.html") + "\\";
		String pdfFolder = "\\" + ApplicationProperties.getProperty("directory.pdf") + "\\";
		if(null!=esiCategoryDetailsList)
		{
			for(int a=0;a<esiCategoryDetailsList.size();a++)
			{
				ESICategoryDetails esiDetails = (ESICategoryDetails)esiCategoryDetailsList.get(a);
				if(null!=esiDetails && ConversionUtils.LINE_TYPE_VALID.equals(esiDetails.getLineType())
						&& null!=esiDetails.getFilePath() && !"".equals(esiDetails.getFilePath()))
				{
					String listed = esiDetails.getFilePath().trim().toLowerCase();
					String swapped = listed.contains(htmlFolder) ? listed.replace(htmlFolder, pdfFolder) : listed.replace(pdfFolder, htmlFolder);
					listedPaths.add(listed);
					listedPaths.add(removeExtension(listed));
					listedPaths.add(swapped);
					listedPaths.add(removeExtension(swapped));
				}
				esiDetails = null;
			}
		}
		if(listedPaths.size()<=0)
		{
			logger.info("find :: ESI CATEGORY text file of Material Folder {"+materialFolderName+"} is missing or has no valid row. No document is treated as withdrawn.");
			return withdrawn;
		}

		// EVERY ACTIVE DOCUMENT THE DATABASE HOLDS FOR THIS MATERIAL FOLDER
		String materialFolderKey = locale+"\\"+modelFolderName+"\\"+manualType+"\\"+faceLiftFolderName+"\\"+materialFolderName+"\\";
		List<String[]> activeDocuments = mnao
				? com.mazda.gms3.dmt.conversion.dao.MNAODocumentManagementDAO.getActiveDocumentsForMaterialFolder(materialFolderKey, locale)
				: mme
				? new com.mazda.gms3.dmt.mme.dao.MMEDocumentManagementDAO().getActiveDocumentsForMaterialFolder(materialFolderKey, modelType, locale)
				: new MCDocumentManagementDAO().getActiveDocumentsForMaterialFolder(materialFolderKey, modelType);
		for(int a=0;a<activeDocuments.size();a++)
		{
			String[] document = (String[])activeDocuments.get(a);
			String storedPath = document[1].trim().toLowerCase();
			if(!listedPaths.contains(storedPath) && !listedPaths.contains(removeExtension(storedPath)))
			{
				withdrawn.add(document);
			}
		}
		logger.info("find :: Material Folder {"+materialFolderKey+"} :: Active Documents in Database :: >"+activeDocuments.size()
				+" :: Withdrawn (no longer in the ESI CATEGORY text file) :: >"+withdrawn.size());
		return withdrawn;
	}

	/** The path without the extension of its file name; unchanged when the file name has none. */
	private static String removeExtension(String path)
	{
		int dot = path.lastIndexOf(".");
		if(dot!=-1 && dot>path.lastIndexOf("\\"))
		{
			return path.substring(0, dot);
		}
		return path;
	}
}
