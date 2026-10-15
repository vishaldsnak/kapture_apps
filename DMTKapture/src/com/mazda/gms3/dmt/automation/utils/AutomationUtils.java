package com.mazda.gms3.dmt.automation.utils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.poi.ss.format.CellNumberFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.mazda.gms3.dmt.automation.dao.AutomationDAO;
import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.PathUtil;
import com.mazda.gms3.dmt.utils.Utilities;

public class AutomationUtils {

	private static Logger logger = LogManager.getLogger(AutomationUtils.class);
	
	/** Excel extensions read from the facelift folder. */
	private static final String[] FACELIFT_EXTENSIONS = { ".xlsx", ".xls", ".xlsm" };

	/**
	 * The facelift Excel files of the folder (.xlsx .xls .xlsm, Office lock files "~$..." left out), by name.
	 * Empty when the folder does not exist or holds none.
	 */
	public static List<File> listFaceliftFiles(File folder)
	{
		List<File> out = new ArrayList<File>();
		File[] files = null == folder || !folder.isDirectory() ? null : folder.listFiles();
		if(null!=files)
		{
			for(File f : files)
			{
				String name = f.getName().toLowerCase();
				if(!f.isFile() || name.startsWith("~$"))
				{
					continue;
				}
				for(String ext : FACELIFT_EXTENSIONS)
				{
					if(name.endsWith(ext))
					{
						out.add(f);
						break;
					}
				}
			}
		}
		Collections.sort(out, new Comparator<File>() {
			public int compare(File a, File b) {
				return a.getName().compareToIgnoreCase(b.getName());
			}
		});
		return out;
	}

	/**
	 * Reads EVERY facelift Excel of the folder - first sheet, row 1 = header, 11 columns: LOCALE | OLD CARLINE CODE |
	 * OLD WMI | OLD VDS | OLD VIS START | OLD VIS END | NEW CARLINE CODE | NEW WMI | NEW VDS | NEW VIS START | NEW VIS END.
	 * A file may mix locales. Blank rows are skipped. Each row keeps its file name and row number for the messages.
	 *
	 * @param errors receives one line per file that could not be read
	 */
	public static ArrayList<ExcelRowDetails> readFaceliftFolder(List<File> files, List<String> errors)
	{
		ArrayList<ExcelRowDetails> dataList = new ArrayList<ExcelRowDetails>();
		for(File file : files)
		{
			InputStream is = null;
			Workbook workbook = null;
			try
			{
				is = PathUtil.fileInputStream(file);
				workbook = WorkbookFactory.create(is);
				Sheet sheet = workbook.getSheetAt(0);
				int read = 0;
				for(int r=1; r<=sheet.getLastRowNum(); r++)
				{
					Row row = sheet.getRow(r);
					if(null==row)
					{
						continue;
					}
					String[] v = new String[11];
					boolean empty = true;
					for(int c=0;c<11;c++)
					{
						Object cell = readCellValue(row.getCell(c));
						v[c] = null==cell || "".equals(String.valueOf(cell).trim()) ? null : String.valueOf(cell).trim();
						if(null!=v[c])
						{
							empty = false;
						}
					}
					if(empty)
					{
						continue;
					}
					ExcelRowDetails details = new ExcelRowDetails();
					details.setFileName(file.getName());
					details.setRowNo(r+1);
					details.setLocale(v[0]);
					details.setOldCarlineCode(v[1]);
					details.setOldWmiCode(v[2]);
					details.setOldVdsCode(v[3]);
					details.setOldVisStartRange(v[4]);
					details.setOldVisEndRange(v[5]);
					details.setNewCarlineCode(v[6]);
					details.setNewWmiCode(v[7]);
					details.setNewVdsCode(v[8]);
					details.setNewVisStartRange(v[9]);
					details.setNewVisEndRange(v[10]);
					dataList.add(details);
					read++;
				}
				logger.info("readFaceliftFolder :: {"+file.getName()+"} :: rows read :: > "+read);
			}
			catch(Exception e)
			{
				Utilities.printStackTraceToLogs(AutomationUtils.class.getName(), "readFaceliftFolder()", e);
				errors.add(file.getName()+" could not be read: "+e.getMessage());
			}
			finally
			{
				try
				{
					if(null!=workbook)
						workbook.close();
					if(null!=is)
						is.close();
				}
				catch(Exception e)
				{
					Utilities.printStackTraceToLogs(AutomationUtils.class.getName(), "readFaceliftFolder()", e);
				}
			}
		}
		return dataList;
	}
	

	/**
	 * Function will identify the Cell Value Type and accordingly will read
	 * their values and will return
	 * 
	 * @param cell
	 * @return
	 */
	private static Object readCellValue(Cell cell) {

		Object cellValue = null;
		try {
			if (null != cell) {
				/*
				 * check for Cell Type and format accordingly
				 */
				if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
					cellValue = cell.getNumericCellValue();
					
					/*
					 * Check here, if callValue is 0; THEN return "" as object
					 * else, check for decimal value in cell
					 * if decimal value is .0 or .00, then remove it
					 * if more than 0, then pass the value as it is
					 */

					String val = String.valueOf(cellValue);
					if(null!=val && !"".equals(val))
					{
						/*
						 *  check if val has any value in point to decimal and that value is 0 then remove the decimal Value
						 */
						if(val.lastIndexOf(".")!=-1)
						{
							String decimal = val.substring(val.lastIndexOf(".")+1,val.length());
							if(null!=decimal && !"".equals(decimal))
							{
								Double dec = new Double(decimal).doubleValue();
								if(dec==0)
								{
									// remove decimal from the val and value after decimal
									val = val.substring(0,val.lastIndexOf("."));
								}
								else
								{
									// GET THE NUMERIC VALUE IN THE FORMAT ########################## AND PASS IT
									CellNumberFormatter cn = new CellNumberFormatter("################################");
									val = cn.format(cell.getNumericCellValue());
									cn = null;
								}
								// set dec to null
								dec = null;
							}
							// set decimal to null
							decimal = null;
						}
					}
					
					// set cellValue as val
					cellValue = val;
					// set val to null
					val  = null;
				}
				else if(cell.getCellType() == Cell.CELL_TYPE_FORMULA)
				{
					cellValue = cell.getCellFormula();
					if(null!=cellValue)
					{
						// append a = with this formaula
						cellValue = "="+cellValue;
					}
				}
				else if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
					cellValue = cell.getStringCellValue();
				}
			}
		} catch (Exception e) {
			Utilities.printStackTraceToLogs(AutomationUtils.class.getName(), "readCellValue()", e);
			// set cellValue to null
			cellValue = null;
		}
		return cellValue;
	}
	
	public static List<ItemDetails> identifyImpactedDocumentsListModelWiseForSchedule(List<ExcelRowDetails> inputDataList)
	{
		List<ItemDetails> itemsList = new ArrayList<ItemDetails>();
		try
		{
			ArrayList<ItemDetails> tempItemsList = new ArrayList<ItemDetails>();
			
			if(null!=inputDataList && inputDataList.size()>0)
			{
				// identify uniqueLocales List from uploaded Excel
				ArrayList<String> uniqueLocalesList = new  ArrayList<String>();
				ExcelRowDetails data = new ExcelRowDetails();
				for(int a=0;a<inputDataList.size();a++)
				{
					data = (ExcelRowDetails)inputDataList.get(a);
					if(null!=data.getLocale() && !"".equals(data.getLocale()))
					{
						boolean add = true;
						if(null!=uniqueLocalesList && uniqueLocalesList.size()>0)
						{
							for(int b=0;b<uniqueLocalesList.size();b++)
							{
								if(data.getLocale().trim().toLowerCase().equals(uniqueLocalesList.get(b).trim().toLowerCase()))
								{
									add = false;
									break;
								}
							}
						}
						
						if(add == true)
						{
							uniqueLocalesList.add(data.getLocale());
						}
					}
				}
				data = null;
				
				/*
				 *  identify uniqueModels List from uploaded Excel for each specific Locale
				 *  and add as Item to itemsList
				 *  check here if Locale & Model already added to itemsList, then do not add as Item, but add the VIN to Applicable VIN List  of the item
				 *  else add as Item. 
				 */
				String oldCarlineLabel="";
				ItemDetails itemDetails = new ItemDetails();
				data = new ExcelRowDetails();
				ExcelRowDetails vinDetails = new ExcelRowDetails();
				for(int a=0;a<inputDataList.size();a++)
				{
					data = (ExcelRowDetails)inputDataList.get(a);
					if(null!=data.getLocale() && !"".equals(data.getLocale()) && null!=data.getOldModelName() && !"".equals(data.getOldModelName())  
							&& null!=data.getOldCarlineCode() && !"".equals(data.getOldCarlineCode()))
					{
						oldCarlineLabel = data.getOldModelName()+" ("+data.getOldCarlineCode()+")";
						boolean add = true;
						if(null!=tempItemsList && tempItemsList.size()>0)
						{
							itemDetails = new ItemDetails();
							for(int b=0;b<tempItemsList.size();b++)
							{
								itemDetails = (ItemDetails) tempItemsList.get(b);
								if(data.getLocale().trim().toLowerCase().equals(itemDetails.getLocale().trim().toLowerCase()) 
										&& oldCarlineLabel.trim().toLowerCase().equals(itemDetails.getCarlineInfo().trim().toLowerCase()))
								{
									if(null!=data.getOldRefKey() && !"".equals(data.getOldRefKey()))
									{
										// item already Exists - DO NOT ADD AS NEW ITEM - BUT ADD CURRENT VIN AS ITEM TO APPLICABLE VIN LIST
										boolean addToVin=true;
										if(null!=itemDetails.getApplicableVINList() && itemDetails.getApplicableVINList().size()>0)
										{
											vinDetails = new ExcelRowDetails();
											for(int c=0;c<itemDetails.getApplicableVINList().size();c++)
											{
												vinDetails = (ExcelRowDetails)itemDetails.getApplicableVINList().get(c);
												// current OLD VIN matches - item is already added....
												if(null!=vinDetails.getOldRefKey() && !"".equals(vinDetails.getOldRefKey()))
												{
													if(vinDetails.getOldRefKey().trim().toLowerCase().equals(data.getOldRefKey().trim().toLowerCase()))
													{
														// do not add Applicable VIN - already added.
														addToVin = false;
														break;
													}
												}
												vinDetails = null;
											}
											vinDetails = null;
										}
										
										// add Applicable VIN to existing Item
										if(addToVin==true)
										{
											if(null==itemDetails.getApplicableVINList() || itemDetails.getApplicableVINList().size()<=0)
											{
												itemDetails.setApplicableVINList(new ArrayList<ExcelRowDetails>());
											}
											itemDetails.getApplicableVINList().add(data);
										}
									}
									// already exists
									add = false;
									break;
								}
								itemDetails = null;
							}
							itemDetails = null;
						}
						
						if(add == true)
						{
							itemDetails = new ItemDetails();
							itemDetails.setSrNo(tempItemsList.size()+1);
							itemDetails.setLocale(data.getLocale());
							itemDetails.setCarlineInfo(oldCarlineLabel);
							// add applicable VIN 
							if(null==itemDetails.getApplicableVINList() || itemDetails.getApplicableVINList().size()<=0)
							{
								itemDetails.setApplicableVINList(new ArrayList<ExcelRowDetails>());
							}
							itemDetails.getApplicableVINList().add(data);
							// add to itemsList
							tempItemsList.add(itemDetails);
							itemDetails = null;
						}
						oldCarlineLabel =  null;
					}
					data = null;
				}
				data = null;
				oldCarlineLabel = null;
				
				/*
				 * now here, if itemsList is not then proceed for adding SM, SI & OTHER CHANNEL TYPES FOR EACH LOCALE + MODEL
				 */
				if(null!=tempItemsList && tempItemsList.size()>0)
				{
					// prepare allChannels List
					/*
					 * COMMENT SI, ACCESSORIES, TRAINING & VIDEOS
					 * DATE 24 JULY 2019
					 */
					itemsList.addAll(prepareItemsListChannelAndDocTypesWise(AutomationConstants.CHANNEL_REFKEY_SERVICE_MANUAL_TYPE, tempItemsList));
					itemsList.addAll(prepareItemsListChannelAndDocTypesWise(AutomationConstants.CHANNEL_REFKEY_OTHER_MANUAL_TYPE, tempItemsList));
//					itemsList.addAll(prepareItemsListChannelAndDocTypesWise(AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE, tempItemsList));
//					itemsList.addAll(prepareItemsListChannelAndDocTypesWise(AutomationConstants.CHANNEL_REFKEY_ACCESSORIES, tempItemsList));
					itemsList.addAll(prepareItemsListChannelAndDocTypesWise(AutomationConstants.CHANNEL_REFKEY_WIRING_DIAGRAMS, tempItemsList));
//					itemsList.addAll(prepareItemsListChannelAndDocTypesWise(AutomationConstants.CHANNEL_REFKEY_TRAINING, tempItemsList));
//					itemsList.addAll(prepareItemsListChannelAndDocTypesWise(AutomationConstants.CHANNEL_REFKEY_VIDEOS, tempItemsList));
					
					if(null!=itemsList && itemsList.size()>0)
					{
						// PROCEED FOR IDENTIFYING IMPACTED DOCUMENTS LIST FOR EACH CHANNEL
						itemsList = AutomationDAO.getImpactedDocumentsListModelWise((ArrayList<ItemDetails>)itemsList);
					}
					else
					{
						logger.info("identifyImpactedDocumentsListModelWise ::  Failed to Preapre Unique Locales + Models + Channel Types List. Cannot proceed further.Return null");
					}
				}
				else
				{
					logger.info("identifyImpactedDocumentsListModelWise ::  Failed to Preapre Unique Locales + Models List. Cannot proceed further.Return null");
				}
			}
			else
			{
				logger.info("identifyImpactedDocumentsListModelWise ::  InputDataList as Parameter is null.");
			}
			
			tempItemsList = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationUtils.class.getName(), "identifyImpactedDocumentsListModelWise()", e);
		}
		return itemsList;
	}
	
	
	private static ArrayList<ItemDetails> prepareItemsListChannelAndDocTypesWise(String channelRefKey, ArrayList<ItemDetails> existingItemsList)
	{
		ArrayList<ItemDetails> itemsList = new ArrayList<ItemDetails>();
		try
		{
			if(null!=channelRefKey && !"".equals(channelRefKey) && null!=existingItemsList && existingItemsList.size()>0)
			{
				// add channelOnly
				ItemDetails existingDetails = new ItemDetails();
				ItemDetails itemDetails = new ItemDetails();
				for(int a=0;a<existingItemsList.size();a++)
				{
					existingDetails = (ItemDetails)existingItemsList.get(a);
					itemDetails = new ItemDetails();
					itemDetails.setSrNo(itemsList.size()+1);
					itemDetails.setChannelRefKey(channelRefKey);
					itemDetails.setChannelLabel(identifyChannelLabel(channelRefKey));
					itemDetails.setLocale(existingDetails.getLocale());
					itemDetails.setCarlineInfo(existingDetails.getCarlineInfo());
					itemDetails.setApplicableVINList(existingDetails.getApplicableVINList());
					itemsList.add(itemDetails);
					itemDetails = null;
					existingDetails = null;
				}
				existingDetails = null;
				itemDetails  = null;
			
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationUtils.class.getName(), "prepareItemsListChannelAndDocTypesWise()", e);
		}
		return itemsList;
	}

	private static String identifyChannelLabel(String channelRefKey)
	{
		String label="";
		if(channelRefKey.equals(AutomationConstants.CHANNEL_REFKEY_ACCESSORIES))
		{
			label = AutomationConstants.CHANNEL_LABEL_ACCESSORIES;
		}
		else if(channelRefKey.equals(AutomationConstants.CHANNEL_REFKEY_WIRING_DIAGRAMS))
		{
			label = AutomationConstants.CHANNEL_LABEL_WIRING_DIAGRAMS;
		}
		else if(channelRefKey.equals(AutomationConstants.CHANNEL_REFKEY_TRAINING))
		{
			label = AutomationConstants.CHANNEL_LABEL_TRAINING;
		}
		else if(channelRefKey.equals(AutomationConstants.CHANNEL_REFKEY_VIDEOS))
		{
			label = AutomationConstants.CHANNEL_LABEL_VIDEOS;
		}
		else if(channelRefKey.equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_MANUAL_TYPE))
		{
			label = AutomationConstants.CHANNEL_LABEL_SERVICE_MANUAL_TYPE;
		}
		else if(channelRefKey.equals(AutomationConstants.CHANNEL_REFKEY_OTHER_MANUAL_TYPE))
		{
			label = AutomationConstants.CHANNEL_LABEL_OTHER_MANUAL_TYPE;
		}
		else if(channelRefKey.equals(AutomationConstants.CHANNEL_REFKEY_SERVICE_INFORMATION_TYPE))
		{
			label = AutomationConstants.CHANNEL_LABEL_SERVICE_INFORMATION_TYPE;
		}
		return label;
	}
	
}
