package com.mazda.gms3.dmt.automation.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.format.CellNumberFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mazda.gms3.dmt.automation.dao.AutomationDAO;
import com.mazda.gms3.dmt.automation.vo.AutomationConstants;
import com.mazda.gms3.dmt.automation.vo.ExcelRowDetails;
import com.mazda.gms3.dmt.automation.vo.ItemDetails;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.utils.Utilities;

public class AutomationUtils {

	private static Logger logger = LogManager.getLogger(AutomationUtils.class);
	
	public static void main(String args[])
	{
		ArrayList<ExcelRowDetails> list=  new ArrayList<ExcelRowDetails>();
		ExcelRowDetails data = new ExcelRowDetails();
		data.setLocale("en_UK");
//		data.setCarlineRefKey("MAZDA6_GJ");
		data.setOldRefKey("GJJMZGJ4238__100001ZZZZZZ");
		list.add(data);
		data = null;
		
		data = new ExcelRowDetails();
		data.setLocale("ja_JP");
//		data.setCarlineRefKey("ATENZA_GG");
		data.setOldRefKey("GG___GGES101640399999");
		list.add(data);
		data = null;
		
		data = new ExcelRowDetails();
		data.setLocale("ja_JP");
//		data.setCarlineRefKey("FLAIR_CROSSOVER_MS");
		data.setOldRefKey("MS___MS31S100001999999");
		list.add(data);
		data = null;
		
		
		ArrayList<ItemDetails> items = identifyImpactedDocumentsListModelWise(list);
		
		System.out.println(items.size());
		
		for(int a=0;a<items.size();a++)
		{
			ItemDetails details = (ItemDetails)items.get(a);
			if(null==details.getImpactedDocumentsList())
			{
				details.setImpactedDocumentsList(new ArrayList<Map<String,Object>>());
			}
			System.out.println(details.getLocale()+"        "+ details.getCarlineInfo()+"  > "+ details.getChannelLabel()+"/"+details.getDocumentTypeLabel()+"------------>"+ details.getApplicableVINList().size()+" ------->"+ details.getImpactedDocumentsList().size());
			if(null!=details.getApplicableVINList())
			{
				for(int c=0;c<details.getApplicableVINList().size();c++)
				{
					ExcelRowDetails rest = (ExcelRowDetails)details.getApplicableVINList().get(c);
					System.out.println("##### refKey :: > "+ rest.getOldRefKey());
				}
			}
		}
	}
	
	
	/**
	 * Function will read the uploaded VIN Excel Data.
	 * @param data
	 * @param extension
	 * @return
	 */
	public static ArrayList<ExcelRowDetails> readAutomationExcelData(byte[] data, String extension) 
	{
		ArrayList<ExcelRowDetails> dataList = null;
		try
		{
			if(null!=data)
			{
				InputStream is = new ByteArrayInputStream(data);
				XSSFWorkbook workbook  = null;
				XSSFSheet sheet = null;
				HSSFWorkbook xlsWorkBook = null;
				HSSFSheet xlsSheet = null;
				Iterator<Row> rowIterator = null;
				
				if(null!=extension && (extension.trim().toLowerCase().equals("xlsx") || 
						extension.trim().toLowerCase().equals("xlsm")))
				{
					//Create Workbook instance holding reference to .xlsx file
					workbook = new XSSFWorkbook(is);
					//Get first/desired sheet from the workbook
					sheet = workbook.getSheetAt(0);
					//Iterate through each rows one by one
					rowIterator = sheet.iterator();
				}
				else if(null!=extension && extension.trim().toLowerCase().equals("xls"))
				{
					// Create workbook instance holding reference to .xls file
					xlsWorkBook = new HSSFWorkbook(is);
					//Get first/desired sheet from the workbook
					xlsSheet=  xlsWorkBook.getSheetAt(0);
					//Iterate through each rows one by one
					rowIterator = xlsSheet.iterator();
				}
				
				long rowCount=0;
				ExcelRowDetails details = new ExcelRowDetails();
				dataList = new ArrayList<ExcelRowDetails>();
				Object dataCell = null;
				while(null!=rowIterator && rowIterator.hasNext())
				{
					// IGNORE HEADERS ROW
					Row row = rowIterator.next();
					if(rowCount>0)
					{
						details = new ExcelRowDetails();
						// LOCALE
						dataCell = readCellValue(row.getCell(0));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setLocale(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						// OLD CARLINECODE
						dataCell = readCellValue(row.getCell(1));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setOldCarlineCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						// OLD WMI CODE
						dataCell = readCellValue(row.getCell(2));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setOldWmiCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						// OLD VDS CODE
						dataCell = readCellValue(row.getCell(3));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setOldVdsCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						// OLD VIS START
						dataCell = readCellValue(row.getCell(4));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setOldVisStartRange(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						// OLD VIS END
						dataCell = readCellValue(row.getCell(5));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setOldVisEndRange(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						// NEW CARLINE CODE
						dataCell = readCellValue(row.getCell(6));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setNewCarlineCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						// NEW WMI CODE
						dataCell = readCellValue(row.getCell(7));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setNewWmiCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						// NEW VDS CODE
						dataCell = readCellValue(row.getCell(8));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setNewVdsCode(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						// NEW VIS  START
						dataCell = readCellValue(row.getCell(9));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setNewVisStartRange(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						// NEW VIS END
						dataCell = readCellValue(row.getCell(10));
						if(null!=dataCell && !"".equals(dataCell))
						{
							details.setNewVisEndRange(String.valueOf(dataCell).trim());
						}
						dataCell = null;
						
						/*
						 * add details to dataList 
						 */
						if(null==dataList || dataList.size()<=0)
						{
							dataList = new ArrayList<ExcelRowDetails>();
						}

						dataList.add(details);
						details=null;
					}
					// INCREMENT ROW COUNT BY 1
					rowCount++;
					row = null;
				}
				sheet = null;
				workbook = null;
				is.close();
				is = null;
				xlsWorkBook=  null;
				xlsSheet = null;
				rowIterator=  null;
			}
			else
			{
				logger.info("readAutomationExcelData :: Either Excel File as Parameter is null or Excel File is not Valid. Failed to read Source Content Excel.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(AutomationUtils.class.getName(), "readAutomationExcelData()", e);
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
	
	public static ArrayList<ItemDetails> identifyImpactedDocumentsListModelWise(ArrayList<ExcelRowDetails> inputDataList)
	{
		ArrayList<ItemDetails> itemsList = new ArrayList<ItemDetails>();
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
						itemsList = AutomationDAO.getImpactedDocumentsListModelWise(itemsList);
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
