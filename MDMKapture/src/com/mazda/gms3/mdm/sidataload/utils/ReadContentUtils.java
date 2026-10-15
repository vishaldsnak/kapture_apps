package com.mazda.gms3.mdm.sidataload.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mazda.gms3.mdm.sidataload.vo.SIChannelImageDetails;
import com.mazda.gms3.mdm.sidataload.vo.SIChannelSchemaDetails;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.Utilities;

/**
 * Reads the schema field values out of the HTML that the uploaded Word file was converted into.
 *
 * ONE INSTANCE PER UPLOAD - THIS CLASS USED TO BE ENTIRELY STATIC AND THAT WAS A REAL DEFECT.
 *
 * Every working field below (the element list, the current element, the image list, the style
 * block, the schedule id, the timestamp) was a private STATIC field, while SIChannelDataLoad
 * starts a SEPARATE THREAD for every uploaded document. Two schedules running at once shared all
 * of it: document B's paragraph list would overwrite document A's mid-read, so A's fields were
 * sliced out of B's HTML, and A's images were stamped with B's schedule id and timestamp. The
 * screen exists to queue schedules, so this was reachable in normal use, not a theoretical race.
 *
 * The public entry point is still static so the two call sites in
 * SIChannelDataLoadProcessingImpl are unchanged - it now builds an instance and delegates, giving
 * each thread its own state.
 */
public class ReadContentUtils {

	private static final String FILED_TYPE_TEXT = "TEXT";

	private static final String FILED_TYPE_RICH_TEXT = "RICH_TEXT";

	/**
	 * EVERY MARKER THE READER KNOWS, IN ONE PLACE.
	 *
	 * This list is used for BOTH jobs it has to do, which is the point of centralising it:
	 *
	 *  1. the fields that are read out of the document, and
	 *  2. the terminators - a field's content runs from its own marker up to the NEXT marker of
	 *     any kind, so a field missing from this list would silently swallow the field after it.
	 *
	 * The two used to be maintained as separate hardcoded lists (the read calls below, and a
	 * single 10-line boolean condition inside getNextElementContent), which is exactly the kind of
	 * pair that drifts apart when a field is added.
	 *
	 * COMMENT IS A TERMINATOR BUT NOT A FIELD - per Yoshioka San's confirmation of 18 May 2021,
	 * content under [||COMMENT||] is deliberately NOT loaded.
	 *
	 * THE WORD TOKEN STAYS TSB_ISSUE_DATE, DELIBERATELY. Kapture's SERVICE_INFORMATION schema has
	 * no field of that name - it calls this one "Issue Date" - but that rename belongs to the
	 * PAYLOAD, not to the document business authors. The JSON builder emits the value under the key
	 * ISSUE_DATE; nothing changes here, so no Word file and no line of SI_Import_Guidelines.pdf has
	 * to be reissued.
	 */
	private static final String[] SCHEMA_MARKERS = {
		"TITLE", "TSB_NUMBER", "DESCRIPTION", "CALIBRATION", "REPAIR_PROCEDURE",
		"PARTS_INFORMATION", "WARRANTY_INFORMATION", "BULLETIN_NOTES", "CAMPAIGN_NUMBER",
		"TI_NUMBER", "MTIPS_NUMBER", "LEGACY_ID", "TSB_ISSUE_DATE",
		"FIRST_PUBLICATION_DATE", "INTERNAL_DOCUMENT_REFERENCE_NUMBER", "SI_HISTORY",
		"SHOW_AS_NEWS_FOR_WEEKS", "MC_AUTHORIZATION_NUMBER", "COMMENT"
	};

	private Elements elementsList = null;

	private Element element = null;

	private String nextElementText = null;

	private List<SIChannelImageDetails> imagesList = null;

	private String imagesDestinationPhysicalPath = null;

	private String imagesDestinationRelativePath = null;

	private String timestampValue = null;

	private String scheduleId = null;

	private String styleTags = null;

	/**
	 * Reads every schema field out of the converted HTML.
	 *
	 * STATIC FACADE OVER PER-THREAD STATE - see the class comment. The instance is created here
	 * and never escapes, so two concurrent schedules cannot see each other's working data.
	 */
	public static SIChannelSchemaDetails getSIChannelSchemaDetails(SIChannelSchemaDetails contentDetails,
			File htmlFile, String locale, String scheduleCode) {
		return new ReadContentUtils().read(contentDetails, htmlFile, locale, scheduleCode);
	}

	private SIChannelSchemaDetails read(SIChannelSchemaDetails contentDetails, File htmlFile,
			String locale, String scheduleCode) {
		try
		{
			scheduleId = scheduleCode;
			timestampValue = String.valueOf(new Date().getTime());
			Document doc = Jsoup.parse(htmlFile, "UTF-8");
			if(null!=doc)
			{
				// GET STYLE TAGS FROM HTML DOCUMENT
				styleTags=getStyleTags(doc);

				// REMOVE IMAGES HEIGHT & WIDTH SPECIFIC ATTRIBUTES AND ADD THEM TO STYLE ATTRIBUTE FOIR EACH IMAGE
				doc = replaceImagesHeightWidth(doc);

				Element bodyElement = doc.select("body").first();
				if(null!=bodyElement)
				{
					// get FIRST CHILD OF THE BODY TAG
					Element firstDivEle = bodyElement.child(0);
					if(null!=firstDivEle)
					{
						// set elementsList
						elementsList = firstDivEle.children();
						/*
						 * START READING ALL SCHEMA FIELDS VALUES
						 */
						if(null==contentDetails)
						{
							contentDetails = new SIChannelSchemaDetails();
						}

						/*
						 * PREPARE DESTINATIOIN PHYSICAL & RELATIVE PATH FOR IMAGES
						 * NEW CHANGE 05 MAY 2010 PATH WILL BE /library/MAZDA/GMS3/MARKETFOLDER/SI/LOCALE_CODE/image
						 */
						imagesDestinationPhysicalPath = ApplicationProperties.getProperty("SERVER_OKASSETS_PHYSICAL_PATH");
						imagesDestinationPhysicalPath+=ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
						// add repository Name
						imagesDestinationPhysicalPath+=ApplicationProperties.getProperty("REPOSITORY").toUpperCase()+"/";
						// add GMS3 FOLDER NAME
						imagesDestinationPhysicalPath+=ApplicationProperties.getProperty("sichannel.data.load.gms3.diretory").toUpperCase()+"/";
						// add MARKET FOLDER NAME
						imagesDestinationPhysicalPath+=contentDetails.getMarket().trim().toUpperCase()+"/";
						// add SI FOLDER NAME
						imagesDestinationPhysicalPath+=ApplicationProperties.getProperty("sichannel.data.load.si.diretory").toUpperCase()+"/";
						// add Locale
						imagesDestinationPhysicalPath+=locale.replace("-", "_").trim().toLowerCase()+"/";
						// add image directory
						imagesDestinationPhysicalPath+=ApplicationProperties.getProperty("rmitool.image.dir.name")+"/";

						// RELATIVE PATH
						imagesDestinationRelativePath="/"+ApplicationProperties.getProperty("SERVER_LIBRARY_DIRECTORY");
						// add repository Name
						imagesDestinationRelativePath+=ApplicationProperties.getProperty("REPOSITORY").toUpperCase()+"/";
						// add GMS3 FOLDER NAME
						imagesDestinationRelativePath+=ApplicationProperties.getProperty("sichannel.data.load.gms3.diretory").toUpperCase()+"/";
						// add MARKET FOLDER NAME
						imagesDestinationRelativePath+=contentDetails.getMarket().trim().toUpperCase()+"/";
						// add SI FOLDER NAME
						imagesDestinationRelativePath+=ApplicationProperties.getProperty("sichannel.data.load.si.diretory").toUpperCase()+"/";
						// add Locale
						imagesDestinationRelativePath+=locale.replace("-", "_").trim().toLowerCase()+"/";
						// add image directory
						imagesDestinationRelativePath+=ApplicationProperties.getProperty("rmitool.image.dir.name")+"/";

						contentDetails.setTitle(getFieldValues("TITLE", FILED_TYPE_TEXT, htmlFile));
						contentDetails.setTsbNumber(getFieldValues("TSB_NUMBER", FILED_TYPE_TEXT, htmlFile));
						contentDetails.setDescription(getFieldValues("DESCRIPTION", FILED_TYPE_RICH_TEXT, htmlFile));
						contentDetails.setCallibration(getFieldValues("CALIBRATION", FILED_TYPE_RICH_TEXT, htmlFile));
						contentDetails.setRepairProcedure(getFieldValues("REPAIR_PROCEDURE", FILED_TYPE_RICH_TEXT, htmlFile));
						contentDetails.setPartsInformation(getFieldValues("PARTS_INFORMATION", FILED_TYPE_RICH_TEXT, htmlFile));
						contentDetails.setWarrantyInformation(getFieldValues("WARRANTY_INFORMATION", FILED_TYPE_RICH_TEXT, htmlFile));
						contentDetails.setBulletinNotes(getFieldValues("BULLETIN_NOTES", FILED_TYPE_RICH_TEXT, htmlFile));

						// OTHER SCHEMA FIELDS
						contentDetails.setCampaignNumber(getFieldValues("CAMPAIGN_NUMBER", FILED_TYPE_TEXT, htmlFile));
						contentDetails.setTiNumber(getFieldValues("TI_NUMBER", FILED_TYPE_TEXT, htmlFile));
						contentDetails.setMtipsNumber(getFieldValues("MTIPS_NUMBER", FILED_TYPE_TEXT, htmlFile));
						contentDetails.setLegacyId(getFieldValues("LEGACY_ID", FILED_TYPE_TEXT, htmlFile));
						/*
						 * READ UNDER THE WORD TOKEN, EMIT UNDER KAPTURE'S NAME. The document says
						 * TSB_ISSUE_DATE and always will; the JSON builder writes ISSUE_DATE.
						 */
						contentDetails.setTsbIssueDate(getFieldValues("TSB_ISSUE_DATE", FILED_TYPE_TEXT, htmlFile));
						contentDetails.setFirstPublicationDate(getFieldValues("FIRST_PUBLICATION_DATE", FILED_TYPE_TEXT, htmlFile));
						contentDetails.setInternalDocRefNumber(getFieldValues("INTERNAL_DOCUMENT_REFERENCE_NUMBER", FILED_TYPE_TEXT, htmlFile));

						// SI HISTORY = RICH TEXT FILED
						contentDetails.setSiHistory(getFieldValues("SI_HISTORY", FILED_TYPE_RICH_TEXT, htmlFile));
//
						contentDetails.setShowAsNewsForWeek(getFieldValues("SHOW_AS_NEWS_FOR_WEEKS", FILED_TYPE_TEXT, htmlFile));
						contentDetails.setMcAuthorizationNo(getFieldValues("MC_AUTHORIZATION_NUMBER", FILED_TYPE_TEXT, htmlFile));


						// SET TOTAL IMAGES FOUND IN THE DOCUMENT
						if(null!=imagesList && imagesList.size()>0)
						{
							contentDetails.setImagesList(imagesList);
							/*
							 * ITERATE IMAGES LIST AND REPLACE SRC VALUES FOR EACH IMAGE WITH DESTINATION RELATIVE PATH
							 */
							SIChannelImageDetails imgDetails = null;
							for(int a=0;a<imagesList.size();a++)
							{
								imgDetails = (SIChannelImageDetails)imagesList.get(a);
								if(null!=imgDetails.getSrcValue() && null!=imgDetails.getDestinationRelativePath())
								{
									/*
									 * REPLACE IMAGES IN ALL RICH TEXT FIELDS CONTENT
									 */
									contentDetails.setDescription(contentDetails.getDescription().replace(imgDetails.getSrcValue(), imgDetails.getDestinationRelativePath()));
									contentDetails.setCallibration(contentDetails.getCallibration().replace(imgDetails.getSrcValue(), imgDetails.getDestinationRelativePath()));
									contentDetails.setRepairProcedure(contentDetails.getRepairProcedure().replace(imgDetails.getSrcValue(), imgDetails.getDestinationRelativePath()));
									contentDetails.setPartsInformation(contentDetails.getPartsInformation().replace(imgDetails.getSrcValue(), imgDetails.getDestinationRelativePath()));
									contentDetails.setWarrantyInformation(contentDetails.getWarrantyInformation().replace(imgDetails.getSrcValue(), imgDetails.getDestinationRelativePath()));

									contentDetails.setSiHistory(contentDetails.getSiHistory().replace(imgDetails.getSrcValue(), imgDetails.getDestinationRelativePath()));
								}
								imgDetails = null;
							}
							imgDetails = null;
						}
						imagesList = null;
					}
					firstDivEle = null;
				}
				bodyElement = null;
			}
			doc=  null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ReadContentUtils.class.getName(), "getSIChannelSchemaDetails()", e);
		}
		finally
		{
			element = null;
			elementsList = null;
			nextElementText = null;
			imagesList = null;
			htmlFile= null;
			imagesDestinationPhysicalPath  =null;
			imagesDestinationRelativePath = null;
			timestampValue = null;
			scheduleId = null;
			scheduleCode = null;
			styleTags = null;
		}
		return contentDetails;
	}

	private String getFieldValues(String fieldName, String type, File sourceFile)
	{
		String fieldValue="";
		try
		{
			if(null!=elementsList && elementsList.size()>0)
			{
				element =null;
				for(int a=0;a<elementsList.size();a++)
				{
					element = (Element)elementsList.get(a);
					// USE ELEMENT. TEXT FOR FIELDS CHECK, SO THAT HTML INSIDE TAGS E..G <B>TITLE WILL NOT HAVE ANY IMPACT
					if(containsMarker(element.text(), fieldName))
					{
						fieldValue = getNextElementContent(element, fieldValue);
						if(null!=fieldValue && !"".equals(fieldValue) && type.equals(FILED_TYPE_RICH_TEXT))
						{
							// replace nullpx to 0px in content
							fieldValue = fieldValue.replace("nullpx", "0px");

							/*
							 *  IDENTIFY IF DOCUMENT CONTAINS ANY IMAGES FILES
							 *  LOOK FOR BELOW TAGS - <v:imagedata & IMG
							 */
							fieldValue =  getImagesDetails(fieldValue, sourceFile);


							/*
							 * ALSO ADD STYLE TAGS TO EACH RICH TEXT FIELD DATA
							 */
							if(null!=styleTags && !"".equals(styleTags))
							{
								fieldValue=styleTags+fieldValue;
							}
						}
						break;
					}
					element = null;
				}
				element = null;

				/*
				 * IF FIELD TYPE IS TEXT E.G. TITLE - TSB NUMBER ETC
				 * THEN USE THE TEXT METHOD TO GET THE SPECFIC CONTENT ONLY
				 */
				if(type.equals(FILED_TYPE_TEXT))
				{
					element= null;
					if(null!=fieldValue && !"".equals(fieldValue))
					{
						element = Jsoup.parse(fieldValue);
						fieldValue = cleanTextValue(element.text());
					}
					element = null;
				}
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ReadContentUtils.class.getName(), "getFieldValues()", e);
		}
		return fieldValue;
	}

	/**
	 * TRUE WHEN THIS ELEMENT CARRIES THE GIVEN FIELD'S MARKER.
	 *
	 * THE MATCH IS CASE SENSITIVE, AND THAT IS THE AGREED BEHAVIOUR - the marker in the Word file
	 * has to be upper case. Note this differs from SI_Import_Guidelines.pdf, which spells the
	 * examples as [||Title||] and [||Description||]: a document typed that way yields an EMPTY
	 * field and no error anywhere, because a marker that does not match is indistinguishable from
	 * a field the author left out. Worth knowing when a field arrives unexpectedly blank.
	 */
	private static boolean containsMarker(String elementText, String fieldName)
	{
		if(null==elementText || null==fieldName)
		{
			return false;
		}
		return elementText.contains("[||"+fieldName.toUpperCase()+"||]");
	}

	/**
	 * TRUE WHEN THIS ELEMENT STARTS A NEW FIELD, i.e. it carries ANY known marker.
	 *
	 * Driven off SCHEMA_MARKERS rather than a hand-written boolean chain, so adding a field to
	 * the read list above automatically makes it stop the PREVIOUS field's content.
	 */
	private static boolean containsAnyMarker(String elementText)
	{
		if(null==elementText)
		{
			return false;
		}
		for(int a=0;a<SCHEMA_MARKERS.length;a++)
		{
			if(elementText.contains("[||"+SCHEMA_MARKERS[a]+"||]"))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * TRIMS A PLAIN TEXT FIELD, INCLUDING WORD'S NON-BREAKING SPACES.
	 *
	 * String.trim() alone is not enough. Word emits U+00A0 for the padding an author leaves after
	 * a value, and it survives the HTML conversion, so 102_Content.docx yields a TSB number of
	 * "R038/21     " - four invisible characters that would be stored as part
	 * of the number and would then never match a search for R038/21.
	 *
	 * ONLY PLAIN TEXT FIELDS ARE CLEANED. A rich text field's whitespace is part of its markup and
	 * is left exactly as the author wrote it.
	 */
	private static String cleanTextValue(String value)
	{
		if(null==value)
		{
			return "";
		}
		return value.replace('\u00A0', ' ').trim();
	}

	/**
	 * A FIELD'S CONTENT: every sibling after the marker, up to the next marker of any kind.
	 *
	 * ITERATIVE, NOT RECURSIVE. This used to call itself once per sibling element, so the stack
	 * depth was the length of the document - 4322_Content.docx alone reaches 1024 siblings, and a
	 * larger file would have ended in a StackOverflowError that surfaced only as a generic parse
	 * failure. The walk is a straight loop over nextElementSibling(), which is the same traversal
	 * with no depth limit.
	 *
	 * MAIL ON 18TH MAY 2021 - Yoshioka San confirmed content under [||COMMENT||] is deliberately
	 * NOT loaded, which is why COMMENT is a terminator in SCHEMA_MARKERS.
	 */
	private String getNextElementContent(Element ele,String content)
	{
		Element current = ele;
		while(null!=current)
		{
			Element next = current.nextElementSibling();
			if(null==next)
			{
				break;
			}

			nextElementText = next.text();
			if(null==nextElementText)
			{
				nextElementText = "";
			}
			if(containsAnyMarker(nextElementText))
			{
				// NEXT FIELD STARTS HERE - this field's content is complete
				break;
			}

			/*
			 * CHECK IF next IS A <P> TAG
			 * 	IF YES - THEN CHECK IFIT CONTAINS STYLE TAG WITH TEXT-ALIGN:RIGHT
			 * 		IF YES - REPLACE IT WITH TEXT-ALIGN:LEFT
			 */
			if(next.nodeName().equals("p"))
			{
				String styleText = next.attr("style");
				if(null!=styleText && !"".equals(styleText) && styleText.indexOf("text-align:right")!=-1)
				{
					// replace right with left
					styleText = styleText.replace("text-align:right", "text-align:left");
					// update style element back
					next.attr("style", styleText);
				}
				styleText = null;
			}

			// add outerHTML e..g with complete Node
			content+=next.outerHtml();
			current = next;
		}
		nextElementText = null;
		return content;
	}

	private String getImagesDetails(String content, File htmlFile)
	{
		try
		{
			if(null!=content && !"".equals(content))
			{
				Document doc= Jsoup.parse(content);
				if(null!=doc)
				{
					Elements imagesEleList = null;
					imagesEleList = doc.getElementsByTag("img");
					if(null!=imagesEleList && imagesEleList.size()>0)
					{
						/*
						 * ITEATE ALL IMAGE SELEMENTS AND REPLACE IN ALL PATHS \\ BY /
						 */
						Element imaElement = null;
						String src=null;
						String height=null;
						String width=null;
						String style=null;
						for(int a=0;a<imagesEleList.size();a++)
						{
							imaElement = (Element)imagesEleList.get(a);
							src=  imaElement.attr("src");
							/*
							 * READ WIDTH & HEIGHT OF IMAGE ELEMENT
							 * AND ADD TO STYLE TAG BECAUSE OF PDF PRINT ISSUE IN INFO CENTER
							 */
							width= imaElement.attr("width");
							height = imaElement.attr("height");
							style = imaElement.attr("style");
							if(null==style)
							{
								style=  "";
							}
							if(null!=width && !"".equals(width))
							{
								if(null!=style && !"".equals(style) && !style.endsWith(";"))
								{
									style+=";";
								}
								style+="width:"+width+";";
							}
							if(null!=height && !"".equals(height))
							{
								if(null!=style && !"".equals(style) && !style.endsWith(";"))
								{
									style+=";";
								}
								style+="height:"+height+";";
							}

							if(null!=style && !"".equals(style))
							{
								// update style attribute
								imaElement.attr("style", style);
							}
							if(null!=src && !"".equals(src))
							{
								// replace forward & backward slashes
								content =  content.replace(src, src.replace("\\", "/"));
							}
							src = null;
							imaElement = null;
							height  = null;
							width=  null;
							style = null;
						}
						src =null;
						imaElement=  null;
						// read images data
						readImageSrcContent(imagesEleList, htmlFile);
					}
					imagesEleList = null;
				}
				element = null;
				doc = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ReadContentUtils.class.getName(), "getImagesDetails()", e);
		}
		return content;
	}

	private Document replaceImagesHeightWidth(Document doc)
	{
		try
		{
			if(null!=doc)
			{
				Elements imagesEleList = null;
				imagesEleList = doc.getElementsByTag("img");
				if(null!=imagesEleList && imagesEleList.size()>0)
				{
					/*
					 * ITEATE ALL IMAGE SELEMENTS AND REPLACE IN ALL PATHS \\ BY /
					 */
					Element imaElement = null;
					String height=null;
					String width=null;
					String style=null;
					for(int a=0;a<imagesEleList.size();a++)
					{
						imaElement = (Element)imagesEleList.get(a);
						/*
						 * READ WIDTH & HEIGHT OF IMAGE ELEMENT
						 * AND ADD TO STYLE TAG BECAUSE OF PDF PRINT ISSUE IN INFO CENTER
						 */
						width= imaElement.attr("width");
						height = imaElement.attr("height");
						style = imaElement.attr("style");
						if(null==style)
						{
							style=  "";
						}
						if(null!=width && !"".equals(width))
						{
							width = width.toLowerCase().replace("pt", "px");
							if(null!=style && !"".equals(style) && !style.endsWith(";"))
							{
								style+=";";
							}
							style+="width:"+width+";";
						}
						if(null!=height && !"".equals(height))
						{
							height = height.toLowerCase().replace("pt", "px");
							if(null!=style && !"".equals(style) && !style.endsWith(";"))
							{
								style+=";";
							}
							style+="height:"+height+";";
						}

						if(null!=style && !"".equals(style))
						{
							// update style attribute
							imaElement.attr("style", style);
						}

						// remove height & width attribute from image
						imaElement.removeAttr("width");
						imaElement.removeAttr("height");

						imaElement = null;
						height  = null;
						width=  null;
						style = null;
					}
					imaElement=  null;
				}
				imagesEleList = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ReadContentUtils.class.getName(), "replaceImagesHeightWidth()", e);
		}
		return doc;
	}

	/**
	 * EVERY <img> OCCURRENCE IS TRACKED, DUPLICATES INCLUDED - DELIBERATE.
	 *
	 * The same src can legitimately appear more than once in a document (4322_Content.docx repeats
	 * two of its five), and nothing here can tell a genuine repeat from an authoring accident. The
	 * list is what the File Transaction Report is built from, so keeping one row per occurrence is
	 * what tells the user how many images the Word file actually contained.
	 */
	private void readImageSrcContent(Elements imagesEleList, File htmlFile)
	{
		try
		{
			Element imageElement = null;
			String srcValue = null;
			SIChannelImageDetails images = null;
			for(int b=0;b<imagesEleList.size();b++)
			{
				imageElement = (Element)imagesEleList.get(b);
				srcValue=  imageElement.attr("src");
				if(null!=srcValue && !"".equals(srcValue))
				{
					srcValue = srcValue.replace("\\", "/");
					/*
					 * ADD THIS IMAGE AS OKASSETS
					 */
					images = new SIChannelImageDetails();
					images.setImageName(srcValue.substring(srcValue.lastIndexOf("/")+1, srcValue.length()));
					images.setSrcValue(srcValue);
					images.setImageSourcePath(srcValue);
					// add destination Image Name => scheduleId_timestampvalue_imageName
					images.setDestinationImageName(scheduleId+"_"+timestampValue+"_"+images.getImageName());
					// add destination physical path - add scheduleId_timestampvalue_imageName
					images.setDestinationPath(imagesDestinationPhysicalPath+images.getDestinationImageName());
					// add destination relative path - add scheduleId_timestampvalue_imageName
					images.setDestinationRelativePath(imagesDestinationRelativePath+images.getDestinationImageName());
					// add to IMages List
					if(null==imagesList || imagesList.size()<=0)
					{
						imagesList = new ArrayList<SIChannelImageDetails>();
					}
					imagesList.add(images);
					images = null;
				}
				imageElement = null;
				srcValue = null;
			}
			imageElement = null;
			srcValue = null;
			images = null;
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ReadContentUtils.class.getName(), "getImagesDetails()", e);
		}
	}

	/**
	 * The document's <style> blocks, which are prepended to every rich text field.
	 *
	 * USES A LOCAL Elements, NOT THE SHARED FIELD. It used to assign the instance-wide
	 * elementsList and then null it, which only worked because it happens to be called BEFORE
	 * elementsList is loaded with the body's children. A local removes that ordering trap.
	 */
	private String getStyleTags(Document doc)
	{
		String tags=null;
		try
		{
			if(null!=doc)
			{
				Elements styleElements = doc.select("style");
				if(null!=styleElements && styleElements.size()>0)
				{
					tags="";
					for(int a=0;a<styleElements.size();a++)
					{
						tags+=((Element)styleElements.get(a)).outerHtml();
					}
				}
				styleElements = null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(ReadContentUtils.class.getName(), "getStyleTags()", e);
		}
		return tags;
	}
}
