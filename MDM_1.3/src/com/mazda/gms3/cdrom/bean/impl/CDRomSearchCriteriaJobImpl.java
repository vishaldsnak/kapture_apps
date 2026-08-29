package com.mazda.gms3.cdrom.bean.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.mazda.gms3.cdrom.bean.CDRomScheduleItemDetails;
import com.mazda.gms3.cdrom.dao.CDRomDAO;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.Utilities;

public class CDRomSearchCriteriaJobImpl {

	private Logger logger = LogManager.getLogger(CDRomSearchCriteriaJobImpl.class);
	

	/*
	 * HAS THE SCREEN ABORTED THIS SCHEDULE?
	 *
	 * The Abort button used to call Thread.stop() on this worker. That was REMOVED in Java 20 and
	 * throws UnsupportedOperationException, which the screen's catch(Exception) swallowed - so the
	 * schedule was marked Aborted while this thread carried on to the end. Nothing can kill another
	 * thread any more, so the worker stops itself by reading the status the screen already wrote.
	 *
	 * Throttled: the answer is cached for ABORT_CHECK_INTERVAL_MS and latches once aborted, so a
	 * fast loop cannot turn this into a query per iteration. Worst case the check itself is half a second
	 * behind the click - the rest of any delay is the operation already in flight.
	 */
	private static final long ABORT_CHECK_INTERVAL_MS = 500L;

	private long lastAbortCheckTime = 0L;

	private boolean scheduleAborted = false;

	private String abortScheduleId = null;

	private boolean isScheduleAborted()
	{
		if(scheduleAborted==true)
		{
			return true;
		}
		if(null==abortScheduleId || "".equals(abortScheduleId))
		{
			return false;
		}
		long now = System.currentTimeMillis();
		if(now-lastAbortCheckTime < ABORT_CHECK_INTERVAL_MS)
		{
			return false;
		}
		lastAbortCheckTime = now;
		scheduleAborted = CDRomDAO.isAborted(abortScheduleId);
		if(scheduleAborted==true)
		{
			logger.info("isScheduleAborted :: SCHEDULE {"+abortScheduleId+"} WAS ABORTED FROM THE"
					+" SCREEN. Stopping after the item in flight.");
		}
		return scheduleAborted;
	}
	public void startProcessing(String localeCode,String modelName,String carlineCode,String wmiCode,String vdsCode,String vinStartRange, long scheduleId)
	{
		// remember the schedule so the abort check works in the helpers
		abortScheduleId = String.valueOf(scheduleId);
		try
		{
			/*
			 * UPDATE SCHEDULE STATUS TO PROCESSING.
			 */
			CDRomDAO.updateSearchCriteriaJobStatus(String.valueOf(scheduleId), ApplicationProperties.getProperty("schedule.status.processing.value"));
			/*
			 * START IDENTIFYING ENGINE BOOKS & MISSION BOOKS ON THE BASIS OF 
			 * SELECTION
			 */
			ArrayList<CDRomScheduleItemDetails> itemsList = new ArrayList<CDRomScheduleItemDetails>();
			CDRomScheduleItemDetails details = null;
			List<String> selBookCodes = CDRomDAO.getSelectedBooks(localeCode, modelName, carlineCode, wmiCode,vdsCode, vinStartRange, "ENGINE");
			if(null!=selBookCodes && selBookCodes.size()>0)
			{
				for(int a=0;a<selBookCodes.size();a++)
				{
					if(isScheduleAborted()) { break; }
					details = new CDRomScheduleItemDetails();
					details.setManualTypeCode(selBookCodes.get(a).toString());
					details.setManualType("EngineManuals");
					itemsList.add(details);
					details = null;
				}
			}
			selBookCodes =new ArrayList<String>();
			// FETCH MISSION BOOK CODES
			selBookCodes = CDRomDAO.getSelectedBooks(localeCode, modelName, carlineCode, wmiCode,vdsCode, vinStartRange, "MISSION");
			if(null!=selBookCodes && selBookCodes.size()>0)
			{
				for(int a=0;a<selBookCodes.size();a++)
				{
					if(isScheduleAborted()) { break; }
					details = new CDRomScheduleItemDetails();
					details.setManualTypeCode(selBookCodes.get(a).toString());
					details.setManualType("TransmissionManuals");
					itemsList.add(details);
					details = null;
				}
			}
			
			/*
			 * PROCEED FOR CREATING SEARCH JOB ITEMS DATA
			 */
			boolean createFlag = CDRomDAO.createSearchCriteriaItemsData(itemsList, scheduleId);
			if(createFlag==true)
			{
				logger.info("startProcessing :: Item Details created Successfully for Search Criteria Job Id :: >"+ scheduleId);
				/*
				 * UPDATE SCHEDULE STATUS TO SUCCESS & COMPLETED.
				 */
				CDRomDAO.updateSearchCriteriaJobStatus(String.valueOf(scheduleId), ApplicationProperties.getProperty("schedule.status.success.value"));
			}
			else
			{
				logger.info("startProcessing :: Failed to create Item Details for Search Criteria Job Id :: >"+ scheduleId);
				/*
				 * UPDATE SCHEDULE STATUS TO FAILURE & COMPLETED.
				 */
				CDRomDAO.updateSearchCriteriaJobStatus(String.valueOf(scheduleId), ApplicationProperties.getProperty("schedule.status.failure.value"));
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(CDRomSearchCriteriaJobImpl.class.getName(), "startProcessing()", e);
			/*
			 * UPDATE SCHEDULE STATUS TO FAILURE & COMPLETED.
			 */
			CDRomDAO.updateSearchCriteriaJobStatus(String.valueOf(scheduleId), ApplicationProperties.getProperty("schedule.status.failure.value"));
		}
		
	}
	
	

	
}
