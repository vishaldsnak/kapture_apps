package com.mazda.gms3.mdm.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;
import com.mazda.gms3.mdm.utils.DBConnectionHelper;
import com.mazda.gms3.mdm.utils.Utilities;
import com.mazda.gms3.mdm.vo.IMCategoryDetails;
import com.mazda.gms3.mdm.vo.MNAOViewContentDetails;
import com.mazda.gms3.mdm.vo.SIVinDetails;

public class FetchIMDataDAO extends DBConnectionHelper{
	
	private static Logger logger = LogManager.getLogger(FetchIMDataDAO.class);
	
	private static String getObjectIdForVINHierarchy(String refKey, Connection conn) throws SQLException
	{
		String vinObjectId=null;
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=refKey && !"".equals(refKey))
			{
				String sql="SELECT OBJECTID FROM OK_IM.TAG WHERE REFERENCEKEY='"+refKey.trim()+"'";
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					vinObjectId=  rs.getString("OBJECTID");
				}
				sql = null;
				rs.close();rs=null;
				stmt.close();stmt=null;
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(FetchIMDataDAO.class.getName(), "getObjectIdForVINHierarchy()", e);
			vinObjectId=  null;
		}
		finally
		{
			if(null!=stmt)
				stmt.close();
			if(null!=rs)
				rs.close();
		}
		return vinObjectId;
	}
	
	public static SIVinDetails getDocumentsData(String documentId, String localeId, String vinParentRefKey, String requestType, Connection conn , String closeConnection) throws SQLException
	{
		Statement stmt = null;
		ResultSet rs = null;
		SIVinDetails details = null;
		try
		{
			if(null!=documentId && !"".equals(documentId) && null!=localeId && !"".equals(localeId) && null!=vinParentRefKey && !"".equals(vinParentRefKey))
			{
				if(null==conn || conn.isClosed()==true)
				{
					conn = getConnection();
				}
				
				localeId = localeId.replace("-", "_");
				String getDocumentsDataSql = "SELECT documentid,LOCALEID,CHECKEDOUT, PUBLISHED, RECORDID "
						+ " FROM OK_IM.CONTENTTEXT WHERE documentid ='"+documentId.trim()+"' AND LATESTVERSION='Y' "
						+ " AND LOCALEID ='"+localeId.trim()+"'";
				
//				logger.info("getDocumentsData :: getDocumentsDataSql :: > " + getDocumentsDataSql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(getDocumentsDataSql);
				if(rs.next())
				{
					details = new SIVinDetails();
					
					// set Repository from properties file
					details.setDocumentId(rs.getString("DOCUMENTID"));
					details.setLocale(rs.getString("LOCALEID").trim());
					details.setRecordId(rs.getString("RECORDID"));
					if(null!=rs.getString("PUBLISHED"))
					{
						if(rs.getString("PUBLISHED").trim().equals("Y"))
						{
							details.setDocumentPublished(true);
						}
						else
						{
							details.setDocumentPublished(false);
						}
					}
					else
					{
						details.setDocumentPublished(false);
					}
					
					if(null!=rs.getString("CHECKEDOUT"))
					{
						if(rs.getString("CHECKEDOUT").equals("Y"))
						{
							details.setCheckedOut(true);
						}
					}
				}
				rs.close();rs=null;
				stmt.close();stmt = null;
				getDocumentsDataSql = null;
				
				
				/*
				 * FETCH OTHER ATTRIBUTES OF THE DOCUMENT
				 * FETCH ALL IM VIN CATEGORIES OF THE DOCUMENT
				 */
				if(null!=details && null!=details.getDocumentId() && !"".equals(details.getDocumentId())
						&& null!=details.getRecordId() && !"".equals(details.getRecordId()))
				{
					/*
					 * GET object Id for VIN
					 */
					String vinObjectId=getObjectIdForVINHierarchy(vinParentRefKey, conn);
					if(null!=vinObjectId && !"".equals(vinObjectId))
					{
						// CATEGORIES
						details = getCategoriesList(details, conn, vinObjectId, requestType);
					}
					vinObjectId = null;
				}
				else
				{
					logger.info("getDocumentData :: No Data Found for Document Id {"+documentId+"} for Locale :: > " + localeId);
				}
				
				if(null==closeConnection)
				{
					// close connection
					if(null!=conn)
					{
						conn.close();
					}
				}
			}
			else
			{
				logger.info("getDocumentsData :: Document id, Locale & VIN Parent Key as Parameter are null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(FetchIMDataDAO.class.getName(), "getDocumentsData()", e);
			details= null;
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			// set Params to null;
			documentId = null;
			localeId=  null;
		}
		return details ;
	}
	
	/**
	 * Function will fetch all the Mapped Categories of a Document.
	 * @param details
	 * @param conn
	 * @return
	 */
	private static SIVinDetails getCategoriesList(SIVinDetails details, Connection conn, String vinObjectId, String requestType) 
	{
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=details.getRecordId() && !"".equals(details.getRecordId()) && 
					null!=vinObjectId && !"".equals(vinObjectId))
			{
				details.setCategoryList(new ArrayList<IMCategoryDetails>());
				
				String getCategoriesSql="SELECT B.OBJECTID,B.REFERENCEKEY FROM OK_IM.CONTENTTEXTCATEGORY A, OK_IM.TAG B  "
						+ "	WHERE A.CONTENTTEXTID='"+details.getRecordId()+"' AND A.TAGID=B.RECORDID  "
						+ " AND B.OBJECTID LIKE '"+vinObjectId+".%'";
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(getCategoriesSql);
				while(rs.next())
				{
					String objectId=rs.getString("OBJECTID");
					if(null!=objectId && !"".equals(objectId))
					{
						if(null!=requestType && (requestType.equals("MC") || requestType.equals("MME")))
						{
							// FOR MC / ME - TOKENS LENGHT MUST BE 6, TO AVOID POPULATING MAPPED MODELS DATA
							String[] tok = objectId.split("\\.");
							if(null!=tok && tok.length==6)
							{
								// ONLY VINS
								IMCategoryDetails catDetails = new IMCategoryDetails();
								//catDetails.setCategoryName(rs.getString("NAME"));
								catDetails.setObjectId(rs.getString("OBJECTID"));
								catDetails.setCategoryRefKey(rs.getString("REFERENCEKEY"));
								details.getCategoryList().add(catDetails);
								catDetails = null;
							}
							tok = null;
						}
						else
						{
							// FOR MNAO
							String[] tok = objectId.split("\\.");
							if(null!=tok && tok.length==4)
							{
								// ONLY VINS
								IMCategoryDetails catDetails = new IMCategoryDetails();
								//catDetails.setCategoryName(rs.getString("NAME"));
								catDetails.setObjectId(rs.getString("OBJECTID"));
								catDetails.setCategoryRefKey(rs.getString("REFERENCEKEY"));
								details.getCategoryList().add(catDetails);
								catDetails = null;
							}
							tok = null;
						}
						
					}
					objectId= null;
				}
				getCategoriesSql= null;
				rs.close();rs=null;
				stmt.close();stmt =null;
				
				if(null!=details.getCategoryList() && details.getCategoryList().size()>0)
				{
					logger.info("getCategoriesList :: TOTAL VIN CATEGORIES FOUND FOR {"+details.getDocumentId()+"} OF {"+details.getLocale()+"} ARE :: > " + details.getCategoryList().size());
					/*
					 * GET CATEGOIRES NAMES
					 */
					String sql="";
					String enukLocale=ApplicationProperties.getProperty("en_uk");
					/*
					 * ALSO CHECK IF EN_CA LOCALE - THEN USE EN_US
					 */
					String enusLocale = ApplicationProperties.getProperty("en_us");
					
					for(IMCategoryDetails catDetails : details.getCategoryList())
					{
						sql="SELECT A.NAME FROM OK_IM.TAGRESOURCE A, OK_IM.TAG B where A.TAGID = B.RECORDID and B.REFERENCEKEY='"+catDetails.getCategoryRefKey()+"' "
								+ "AND ";
						if(null!=requestType && requestType.equals("MME"))
						{
							sql = sql+"(A.LOCALEID='"+details.getLocale()+"' OR A.LOCALEID='"+enukLocale+"')";
						}
						else
						{
							if(details.getLocale().trim().toLowerCase().equals(ApplicationProperties.getProperty("en_ca").trim().toLowerCase()))
							{
								sql = sql+"(A.LOCALEID='"+details.getLocale()+"' OR A.LOCALEID='"+enusLocale+"')";
							}
							else
							{
								sql=sql+" A.LOCALEID='"+details.getLocale()+"'";
							}
						}
				
						stmt = conn.createStatement();
						rs = stmt.executeQuery(sql);
						if(rs.next())
						{
							catDetails.setCategoryName(rs.getString("NAME"));
						}
						stmt.close();stmt=null;
						rs.close();rs=null;
						sql=null;
					}
					sql=null;
					enukLocale=  null;
					enusLocale = null;
				}
				else
				{
					logger.info("getCategoriesList :: NO CATEGORIES FOUND FOR {"+details.getDocumentId()+"} OF {"+details.getLocale()+"}.");
				}
			}
			else
			{
				logger.info("getCategoriesList :: RecordId as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(FetchIMDataDAO.class.getName(), "getCategoriesList()", e);
		}
		return details;
	}
	
	/**
	 * The table will Populate View Content Data for following Channel Documents.
	 * SI, SM, WD & ACC.
	 * @param documentId
	 * @param localeId
	 * @throws SQLException
	 */
	public static MNAOViewContentDetails prepareViewContentData(String documentId, String localeId)  throws SQLException
	{
		MNAOViewContentDetails docDetails = new MNAOViewContentDetails();
		Statement stmt = null;
		Connection conn = null;
		ResultSet rs = null;
		try
		{
			if(null!=documentId && !"".equals(documentId) && null!=localeId && !"".equals(localeId))
			{
				conn = getConnection();
				// GET CATEGORIES REF KEYS ID
				
				String esiCategoryObjectId=getObjectIdForVINHierarchy("ESI", conn);
				String serviceInformationObjectId = getObjectIdForVINHierarchy("SERVICE_INFORMATION_TYPE", conn);
				String serviceManaualObjectId = getObjectIdForVINHierarchy("SERVICE_MANUAL_TYPE", conn);
				String modelYearObjectId= getObjectIdForVINHierarchy("MODEL_YEAR", conn);
				String vinObjectId = getObjectIdForVINHierarchy("VIN", conn);
				
				boolean deleteDocumentDataFromViewContent=false;
				
				/*
				 * Fetch Document Details & All its Mapped Categories
				 * Required for Populating VIEW CONTENT DATA
				 */
				String localeIdForIm = localeId;
				localeIdForIm = localeIdForIm.replace("-", "_");
				String getDocumentsDataSql = "SELECT CONTENTID,documentid,LOCALEID,CHECKEDOUT, PUBLISHED, RECORDID , INDEXMASTERIDENTIFIERS,"
						+ " PUBLISHDATE, CREATEDATE, LASTMODIFIEDDATE, DISPLAYENDDATE"
						+ " FROM OK_IM.CONTENTTEXT WHERE documentid ='"+documentId.trim()+"' AND LATESTVERSION='Y' "
						+ " AND LOCALEID ='"+localeIdForIm.trim()+"'";
				
				logger.info("prepareViewContentData :: getDocumentsDataSql :: > " + getDocumentsDataSql);
				
				stmt = conn.createStatement();
				rs = stmt.executeQuery(getDocumentsDataSql);
				if(rs.next())
				{
					docDetails.setDocumentId(rs.getString("DOCUMENTID"));
					docDetails.setLocale(rs.getString("LOCALEID"));
					docDetails.setDocumentStatus(rs.getString("PUBLISHED"));
					docDetails.setPublishDate(rs.getTimestamp("PUBLISHDATE"));
					docDetails.setDisplayEndDate(rs.getTimestamp("DISPLAYENDDATE"));
					docDetails.setTitle(rs.getString("INDEXMASTERIDENTIFIERS"));
					docDetails.setDocumentCreateDate(rs.getTimestamp("CREATEDATE"));
					docDetails.setDocumentLastModifiedDate(rs.getTimestamp("LASTMODIFIEDDATE"));
					docDetails.setContentId(rs.getString("CONTENTID"));
					docDetails.setRecordId(rs.getString("RECORDID"));
					
					if(null!=docDetails.getDocumentStatus() && docDetails.getDocumentStatus().equals("Y"))
					{
						// CHECK IF DISPLAY END DATE IS LESS THAN PUBLISH DATE - THEN DOC EXPIRED, 
						if(null!=docDetails.getPublishDate() && null!=docDetails.getDisplayEndDate() 
								&& docDetails.getDisplayEndDate().getTime() > docDetails.getPublishDate().getTime())
						{
							// DOCUMENT IS PUBLISHED.
							
//							if(null!=docDetails.getTitle())
//							{
//								if(docDetails.getTitle().length()>100)
//								{
//									docDetails.setDescription(docDetails.getTitle().substring(0, 100));
//								}
//								else
//								{
//									docDetails.setDescription(docDetails.getTitle());
//								}
//							}
						}
						else
						{
							// DELETE DOCUMENT DATA FOR LOCALE FROM VIEW CONTENT TABLES - DOCUMENT EXPIRED
							deleteDocumentDataFromViewContent = true;
						}
					}
					else
					{
						// DELETE DOCUMENT DATA FOR LOCALE FROM VIEW CONTENT TABLES - DOCUMENT UNPUBLISHED.
						deleteDocumentDataFromViewContent = true;
					}
				}
				getDocumentsDataSql = null;
				rs.close();rs=null;
				stmt.close();stmt = null;
				
				
				if(deleteDocumentDataFromViewContent==false)
				{
					ArrayList<IMCategoryDetails> categoriesList = new ArrayList<IMCategoryDetails>();
					// PROCEED FOR FETCHING CATEGORIES OF THE DOCUMENT
					String getCategoriesSql="SELECT B.OBJECTID,B.REFERENCEKEY, C.NAME FROM OK_IM.CONTENTTEXTCATEGORY A, OK_IM.TAG B, OK_IM.TAGRESOURCE C  "
							+ "	WHERE A.CONTENTTEXTID='"+docDetails.getRecordId()+"' AND A.TAGID=B.RECORDID AND "
							+ " A.TAGID = C.TAGID AND C.LOCALEID='"+docDetails.getLocale()+"' ";
					
					logger.info("prepareViewContentData :: getCategoriesSql :: > " + getCategoriesSql);
					stmt = conn.createStatement();
					rs = stmt.executeQuery(getCategoriesSql);
					while(rs.next())
					{
						IMCategoryDetails catDetails = new IMCategoryDetails();
						catDetails.setCategoryName(rs.getString("NAME"));
						catDetails.setCategoryRefKey(rs.getString("REFERENCEKEY"));
						catDetails.setObjectId(rs.getString("OBJECTID"));
						
						categoriesList.add(catDetails);
						catDetails = null;
					}
					getCategoriesSql = null;
					rs.close();rs=null;
					stmt.close();stmt = null;
					
					
					if(null!=categoriesList && categoriesList.size()>0)
					{
						/*
						 * ITERATE AND CHECK IF UNPUBLISH_CONTENT / EXPIRE_CONTENT MAPPED OR NOT
						 * if MAPPED - THEN DELETE THE DOCUMENT FROM VIEW CONTENT TABLES
						 */
						boolean unpublishExpireCatMapped = false;
						for(IMCategoryDetails catDetails : categoriesList)
						{
							if(null!=catDetails.getCategoryRefKey())
							{
								if(catDetails.getCategoryRefKey().equals("UBPUBLISH_CONTENT") || 
										catDetails.getCategoryRefKey().equals("EXPIRE_CONTENT"))
								{
									unpublishExpireCatMapped = true;
									// UNPUBLISH_CONTENT / EXPIRE_CONTENT MAPPED.
									// set Document Status as N & IT NEEDS TO BE DELETED FROM VIEW CONTENT TABLES.
									docDetails.setDocumentStatus("N");
									break;
								}
							}
							catDetails=  null;
						}
						
						if(unpublishExpireCatMapped==false)
						{
							/*
							 * ITERATE CATEGORY LIST AND START IDENTIFYING 
							 * ESI CATEGORIES
							 * DOCUMENT TYPE CATEGORIES
							 * MODEL_YEAR CATEGORIES
							 * VIN CATEGORIES
							 */
							// ESI CATEGORIES
							for(IMCategoryDetails catDetails : categoriesList)
							{
								if(null!=catDetails.getObjectId() && !"".equals(catDetails.getObjectId()))
								{
									if(catDetails.getObjectId().startsWith(esiCategoryObjectId))
									{
										String[] tok = catDetails.getObjectId().split("\\.");
										if(null!=tok && tok.length==2)
										{
											// LEVEL 1
											// IDENTIFY ESI CATEGORY CODE HERE
											String rKey = catDetails.getCategoryRefKey();
											rKey= rKey.trim().toLowerCase();
											rKey = rKey.replace("esi", "");
											
											docDetails.setCatCodeLevel1(rKey.trim().toUpperCase());
											docDetails.setCatNameLevel1(catDetails.getCategoryName());
											rKey = null;
										}
										else if(null!=tok && tok.length==3)
										{
											// LEVEL 2
											// 2ND LEVEL CODE WILL BE THE REF KEY ONLY
											docDetails.setCatCodeLevel2(catDetails.getCategoryRefKey());
											docDetails.setCatNameLevel2(catDetails.getCategoryName());
											// IDENTIFY LEVEL 1
											String level1ObjectId=tok[0]+"."+tok[1];
											IMCategoryDetails level1Details = getCategoriesOnObjectId(docDetails.getLocale(), conn, level1ObjectId);
											if(null!=level1Details)
											{
												String rKey = level1Details.getCategoryRefKey();
												if(null!=rKey && !"".equals(rKey))
												{
													rKey= rKey.trim().toLowerCase();
													rKey = rKey.replace("esi", "");
													docDetails.setCatCodeLevel1(rKey.trim().toUpperCase());
												}
												rKey = null;
												docDetails.setCatNameLevel1(level1Details.getCategoryName());
											}
											level1Details = null;
											level1ObjectId = null;
										}
										else if(null!=tok && tok.length==4)
										{
											// LEVEL 3
											// IDENTIFY 3RD LEVEL CODE
											String thirdLevelCode="";
											if(null!=catDetails.getCategoryRefKey() && catDetails.getCategoryRefKey().trim().length()==8)
											{
												thirdLevelCode= catDetails.getCategoryRefKey().substring(6,8);
											}
											docDetails.setCatCodeLevel3(thirdLevelCode);
											docDetails.setCatNameLevel3(catDetails.getCategoryName());
											thirdLevelCode = null;
											
											// IDENTIFY LEVLE 1 & LEVEL 2
											String level2ObjectId=tok[0]+"."+tok[1]+"."+tok[2];
											String level1ObjectId=tok[0]+"."+tok[1];
											
											IMCategoryDetails level2Details = getCategoriesOnObjectId(docDetails.getLocale(), conn, level2ObjectId);
											if(null!=level2Details)
											{
												// 2nd level code will be the reference key
												docDetails.setCatCodeLevel2(level2Details.getCategoryRefKey());
												docDetails.setCatNameLevel2(level2Details.getCategoryName());
											}
											level2Details = null;
											level2ObjectId = null;
											
											IMCategoryDetails level1Details = getCategoriesOnObjectId(docDetails.getLocale(), conn, level1ObjectId);
											if(null!=level1Details)
											{
												String rKey = level1Details.getCategoryRefKey();
												if(null!=rKey && !"".equals(rKey))
												{
													rKey= rKey.trim().toLowerCase();
													rKey = rKey.replace("esi", "");
													docDetails.setCatCodeLevel1(rKey.trim().toUpperCase());
												}
												rKey = null;
												docDetails.setCatNameLevel1(level1Details.getCategoryName());
											}
											level1Details = null;
											level1ObjectId = null;
										}
										break;
									}
								}
								catDetails= null;
							}
							
							// DOCUMENT TYPE & SUB TYPE
							if(docDetails.getDocumentId().trim().startsWith("WD"))
							{
								docDetails.setDocumentType("WIRING_DIAGRAMS");
								// IDENTIY NAME FOR THE WIRING_DIAGRAMS
								docDetails.setDocumentTypeName(getCategorieNameOnRefKeyAndLocale(docDetails.getLocale(), conn, docDetails.getDocumentType()));
							}
							else if(docDetails.getDocumentId().trim().startsWith("AC"))
							{
								docDetails.setDocumentType("ACCESSORIES");
								// IDENTIY NAME FOR THE ACCESSORIES
								docDetails.setDocumentTypeName(getCategorieNameOnRefKeyAndLocale(docDetails.getLocale(), conn, docDetails.getDocumentType()));
							}
							else if(docDetails.getDocumentId().trim().startsWith("SM"))
							{
								for(IMCategoryDetails catDetails : categoriesList)
								{
									if(null!=catDetails.getObjectId() && catDetails.getObjectId().trim().startsWith(serviceManaualObjectId))
									{
										docDetails.setDocumentType(catDetails.getCategoryRefKey());
										docDetails.setDocumentTypeName(catDetails.getCategoryName());
										break;
									}
									catDetails=  null;
								}
							}
							else if(docDetails.getDocumentId().trim().startsWith("SI"))
							{
								/*
								 * IDENTIFY DESCRIPTIUON FOR THE SI DOCUMENT
								 */
								String description = getDescription(docDetails.getRecordId(), conn);
								if(null!=description && !"".equals(description))
								{
									if(description.length()>100)
									{
										docDetails.setDescription(description.substring(0, 99));
									}
									else
									{
										docDetails.setDescription(description);
									}
								}
								description=  null;
								for(IMCategoryDetails catDetails : categoriesList)
								{
									if(null!=catDetails.getObjectId() && catDetails.getObjectId().trim().startsWith(serviceInformationObjectId))
									{
										String[] tok = catDetails.getObjectId().split("\\.");
										if(null!=tok && tok.length==2)
										{
											docDetails.setDocumentType(catDetails.getCategoryRefKey());
											docDetails.setDocumentTypeName(catDetails.getCategoryName());
											
											// NO SUB TYPE
										}
										else if(null!=tok && tok.length==3)
										{
											docDetails.setDocumentSubType(catDetails.getCategoryRefKey());
											docDetails.setDocumentSubTypeName(catDetails.getCategoryName());
											// IDENTIFY DOCUMENT TYPE
											String docTypeObjectId = tok[0]+"."+tok[1];
											
											IMCategoryDetails level1Details = getCategoriesOnObjectId(docDetails.getLocale(), conn, docTypeObjectId);
											if(null!=level1Details)
											{
												docDetails.setDocumentType(level1Details.getCategoryRefKey());
												docDetails.setDocumentTypeName(level1Details.getCategoryName());
											}
											level1Details = null;
											docTypeObjectId = null;
										}
										break;
									}
									catDetails=  null;
								}
								
								// IDENTIFY SI DOCUMENT DETAILS - APPLICABLE ONLY FOR SI DOCUMENTS
								docDetails  = identifySIDocumentTypeDetails(docDetails, conn);
							}
							
							// IDENTIFY MODEL TYPE CATEGORIES
							for(IMCategoryDetails catDetails : categoriesList)
							{
								if(null!=catDetails.getObjectId() && catDetails.getObjectId().startsWith(modelYearObjectId))
								{
									// PREPARE FINAL MODEL YEAR DATA
									MNAOViewContentDetails modelYearData = new MNAOViewContentDetails();
									modelYearData.setDocumentId(docDetails.getDocumentId());
									modelYearData.setLocale(docDetails.getLocale());
									modelYearData.setDocumentStatus(docDetails.getDocumentStatus());
									modelYearData.setPublishDate(docDetails.getPublishDate());
									modelYearData.setDisplayEndDate(docDetails.getDisplayEndDate());
									modelYearData.setTitle(docDetails.getTitle());
									modelYearData.setDocumentCreateDate(docDetails.getDocumentCreateDate());
									modelYearData.setDocumentLastModifiedDate(docDetails.getDocumentLastModifiedDate());
									modelYearData.setContentId(docDetails.getContentId());
									modelYearData.setRecordId(docDetails.getRecordId());
									modelYearData.setDescription(docDetails.getDescription());
									
									modelYearData.setCatCodeLevel1(docDetails.getCatCodeLevel1());
									modelYearData.setCatCodeLevel2(docDetails.getCatCodeLevel2());
									modelYearData.setCatCodeLevel3(docDetails.getCatCodeLevel3());
									
									modelYearData.setCatNameLevel1(docDetails.getCatNameLevel1());
									modelYearData.setCatNameLevel2(docDetails.getCatNameLevel2());
									modelYearData.setCatNameLevel3(docDetails.getCatNameLevel3());
									
									modelYearData.setDocumentType(docDetails.getDocumentType());
									modelYearData.setDocumentTypeName(docDetails.getDocumentTypeName());
									modelYearData.setDocumentSubType(docDetails.getDocumentSubType());
									modelYearData.setDocumentSubTypeName(docDetails.getDocumentSubTypeName());
									
									modelYearData.setFirstPublicationDate(docDetails.getFirstPublicationDate());
									modelYearData.setSiNumber(docDetails.getSiNumber());
									modelYearData.setIssueDate(docDetails.getIssueDate());
									
									String[] tok = catDetails.getObjectId().split("\\.");
									if(null!=tok && tok.length==3)
									{
										// MODEL_YEAR -
										if(null!=catDetails.getCategoryRefKey() && !"".equals(catDetails.getCategoryRefKey()))
										{
											catDetails.setCategoryRefKey(catDetails.getCategoryRefKey().trim());
											if(catDetails.getCategoryRefKey().lastIndexOf("_")!=-1)
											{
												// SET YEAR.
												modelYearData.setYear(catDetails.getCategoryRefKey().substring(catDetails.getCategoryRefKey().lastIndexOf("_")+1, catDetails.getCategoryRefKey().length()));
											}
										}
										
										// IDENTIFY MODEL AND ITS NAME
										String modelObjectId = tok[0]+"."+tok[1];
										
										IMCategoryDetails level1Details = getCategoriesOnObjectId(docDetails.getLocale(), conn, modelObjectId);
										if(null!=level1Details)
										{
											modelYearData.setModel(level1Details.getCategoryName());
										}
										level1Details = null;
										modelObjectId = null;
									}
									else if(null!=tok && tok.length==2)
									{
										// MODEL ONLY NO YEAR AVAILABLE
										modelYearData.setModel(catDetails.getCategoryName());
									}
									tok = null;
									
									if(null==docDetails.getModelYearList() || docDetails.getModelYearList().size()<=0)
									{
										docDetails.setModelYearList(new ArrayList<MNAOViewContentDetails>());
									}
									
									// ADD TO MODEL YEAR VIEWCONTENT LIST TO DOC DETAILS
									docDetails.getModelYearList().add(modelYearData);
									modelYearData = null;
								}
							}
							
							// IDENTIFY VIN CATEGORIES
							for(IMCategoryDetails catDetails : categoriesList)
							{
								if(null!=catDetails.getObjectId() && catDetails.getObjectId().startsWith(vinObjectId))
								{
									// PREPARE FINAL VIN DATA
									MNAOViewContentDetails vinData = new MNAOViewContentDetails();
									vinData.setDocumentId(docDetails.getDocumentId());
									vinData.setLocale(docDetails.getLocale());
									vinData.setDocumentStatus(docDetails.getDocumentStatus());
									vinData.setPublishDate(docDetails.getPublishDate());
									vinData.setDisplayEndDate(docDetails.getDisplayEndDate());
									vinData.setTitle(docDetails.getTitle());
									vinData.setDocumentCreateDate(docDetails.getDocumentCreateDate());
									vinData.setDocumentLastModifiedDate(docDetails.getDocumentLastModifiedDate());
									vinData.setContentId(docDetails.getContentId());
									vinData.setRecordId(docDetails.getRecordId());
									vinData.setDescription(docDetails.getDescription());
									
									vinData.setCatCodeLevel1(docDetails.getCatCodeLevel1());
									vinData.setCatCodeLevel2(docDetails.getCatCodeLevel2());
									vinData.setCatCodeLevel3(docDetails.getCatCodeLevel3());
									
									vinData.setCatNameLevel1(docDetails.getCatNameLevel1());
									vinData.setCatNameLevel2(docDetails.getCatNameLevel2());
									vinData.setCatNameLevel3(docDetails.getCatNameLevel3());
									
									vinData.setDocumentType(docDetails.getDocumentType());
									vinData.setDocumentTypeName(docDetails.getDocumentTypeName());
									vinData.setDocumentSubType(docDetails.getDocumentSubType());
									vinData.setDocumentSubTypeName(docDetails.getDocumentSubTypeName());
									
									vinData.setFirstPublicationDate(docDetails.getFirstPublicationDate());
									vinData.setSiNumber(docDetails.getSiNumber());
									vinData.setIssueDate(docDetails.getIssueDate());
									
									
									String[] tok = catDetails.getObjectId().split("\\.");
									if(null!=tok && tok.length==4)
									{
										// IDENTIFY WMI, VDS, VIN START & VIN END
										String wmiObjectId = tok[0]+"."+tok[1];
										String vdsObjectId = tok[0]+"."+tok[1]+"."+tok[2];
										IMCategoryDetails wmiDetails = getCategoriesOnObjectId(docDetails.getLocale(), conn, wmiObjectId);
										if(null!=wmiDetails)
										{
											vinData.setWmiCode(wmiDetails.getCategoryName());
										}
										wmiDetails = null;
										IMCategoryDetails vdsDetails = getCategoriesOnObjectId(docDetails.getLocale(), conn, vdsObjectId);
										if(null!=vdsDetails)
										{
											vinData.setVdsCode(vdsDetails.getCategoryName());
										}
										vdsDetails  = null;
										String keyToBeReplaced="";
										if(null!=vinData.getWmiCode() && !"".equals(vinData.getWmiCode()) && null!=vinData.getVdsCode() && !"".equals(vinData.getVdsCode()))
										{
											if(!vinData.getWmiCode().trim().equals("-"))
											{
												keyToBeReplaced = keyToBeReplaced+vinData.getWmiCode().trim();
											}
											keyToBeReplaced = keyToBeReplaced+ vinData.getVdsCode().trim();
											keyToBeReplaced = keyToBeReplaced.trim();
											
											// REMOVE IT FROM CATEGORY NAME
											String visRangeName=catDetails.getCategoryName().replace(keyToBeReplaced, "");
											if(null!=visRangeName && !"".equals(visRangeName))
											{
												visRangeName = visRangeName.trim();
												// FIRST 6 CHARS ARE VIS START RANGE AND REMAINING ARE VIS END RANGE
												if(visRangeName.length()>=7)
												{
													vinData.setVisStartRange(visRangeName.substring(0,6));
													vinData.setVisEndRange(visRangeName.substring(6,visRangeName.length()));
												}
												else
												{
													vinData.setVisStartRange(visRangeName);
												}
											}
											visRangeName = null;
										}
										keyToBeReplaced = null;
										wmiObjectId= null;
										vdsObjectId = null;
									}
									tok = null;
									if(null==docDetails.getVinList() || docDetails.getVinList().size()<=0)
									{
										docDetails.setVinList(new ArrayList<MNAOViewContentDetails>());
									}
									
									// ADD TO VIN VIEWCONTENT LIST TO DOC DETAILS
									docDetails.getVinList().add(vinData);
									vinData = null;
								}
							}
						}
					}
					else
					{
						// NO CATEGORIES FETCHED. DELETE DOCUMENT DATA FROM VIEW CONTENT TABLES
						// set Document Status as N & IT NEEDS TO BE DELETED FROM VIEW CONTENT TABLES.
						docDetails.setDocumentStatus("N");
					}
				}
				else
				{
					// set Document Status as N & IT NEEDS TO BE DELETED FROM VIEW CONTENT TABLES.
					docDetails.setDocumentStatus("N");
				}
			}
			
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(FetchIMDataDAO.class.getName(), "prepareViewContentData()", e);
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
			if(null!=conn)
				conn.close();
			// set Params to null;
			documentId = null;
			localeId=  null;
		}
		return docDetails;
	}
	
	private static IMCategoryDetails getCategoriesOnObjectId(String locale, Connection conn, String objectId) 
	{
		IMCategoryDetails catDetails = new IMCategoryDetails();
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=locale && !"".equals(locale) && 
					null!=objectId && !"".equals(objectId))
			{
				String getCategoriesSql="SELECT DISTINCT A.REFERENCEKEY, B.NAME FROM OK_IM.TAG A, OK_IM.TAGRESOURCE B  "
						+ "	WHERE A.RECORDID=B.TAGID AND B.LOCALEID='"+locale+"' "
								+ " AND A.OBJECTID = '"+objectId+"' GROUP BY A.REFERENCEKEY, B.NAME";
				
				logger.info("getCategoriesOnObjectId :: getCategoriesSql :: > " + getCategoriesSql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(getCategoriesSql);
				if(rs.next())
				{
					catDetails.setCategoryName(rs.getString("NAME"));
					catDetails.setCategoryRefKey(rs.getString("REFERENCEKEY"));
				}
				getCategoriesSql= null;
				rs.close();rs=null;
				stmt.close();stmt =null;
			}
			else
			{
				logger.info("getCategoriesOnObjectId :: Locale id / Object id as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(FetchIMDataDAO.class.getName(), "getCategoriesOnObjectId()", e);
		}
		return catDetails;
	}

	private static String getCategorieNameOnRefKeyAndLocale(String locale, Connection conn, String referenceKey) 
	{
		String categoryName="";
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			if(null!=locale && !"".equals(locale) && 
					null!=referenceKey && !"".equals(referenceKey))
			{
				/*
				 * SINCE THIS FUNCTION IS ONLY USED FOR MNAO
				 * SIMPLY CHECK IF LOCALE IS EN_CA, USE EN_US AS WELL
				 */
				String getCategoriesSql="SELECT DISTINCT A.REFERENCEKEY, B.NAME FROM OK_IM.TAG A, OK_IM.TAGRESOURCE B  "
						+ "	WHERE A.RECORDID=B.TAGID AND B.LOCALEID='"+locale+"' "
								+ " AND A.REFERENCEKEY = '"+referenceKey+"' ";
				if(locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("en_ca").trim().toLowerCase()))
				{
					getCategoriesSql = getCategoriesSql+"(B.LOCALEID=='"+locale+"' OR B.LOCALEID='"+ApplicationProperties.getProperty("en_us").trim()+"')";
				}
				else
				{
					getCategoriesSql=getCategoriesSql+" B.LOCALEID='"+locale+"'";
				}
				getCategoriesSql = getCategoriesSql+" GROUP BY A.REFERENCEKEY, B.NAME";
				
				logger.info("getCategorieNameOnRefKeyAndLocale :: getCategoriesSql :: > " + getCategoriesSql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(getCategoriesSql);
				if(rs.next())
				{
					categoryName = rs.getString("NAME");
				}
				getCategoriesSql= null;
				rs.close();rs=null;
				stmt.close();stmt =null;
			}
			else
			{
				logger.info("getCategorieNameOnRefKeyAndLocale :: Locale id / ReferenceKey as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(FetchIMDataDAO.class.getName(), "getCategorieNameOnRefKeyAndLocale()", e);
		}
		return categoryName;
	}
	
	private static MNAOViewContentDetails identifySIDocumentTypeDetails(MNAOViewContentDetails docDetails,Connection conn) 
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		if (null != docDetails.getDocumentType() && !"".equals(docDetails.getDocumentType())) 
		{
			String documentTypeRefKey = docDetails.getDocumentType(); 
			try 
			{
				/*
				 * IDENTIFY SI NUMBER SINCE THE DOCUMENT IS A SI DOCUMENT.
				 */
				String siXpath = "";
				String issueDateXpath = "//SERVICE_INFORMATION/TSB_ISSUE_DATE";
				String firstPublicationXpath = "//SERVICE_INFORMATION/FIRST_PUBLICATION_DATE";
				if (documentTypeRefKey.trim().toLowerCase().equals("CAMPAIGN".toLowerCase())) 
				{
					// CAMPAING NO
					siXpath = "//SERVICE_INFORMATION/CAMPAIGN_NUMBER";
				} 
				else if (documentTypeRefKey.trim().toLowerCase().equals("TECHNICAL_SERVICE_BULLETIN".toLowerCase())) 
				{
					// TSB NO
					siXpath = "//SERVICE_INFORMATION/TSB_NUMBER";

				} 
				else if (documentTypeRefKey.trim().toLowerCase().equals("M_TIPS".toLowerCase())) 
				{
					// MTIPS NO
					siXpath = "//SERVICE_INFORMATION/MTIPS_NUMBER";
				} 
				else if (documentTypeRefKey.trim().toLowerCase().equals("SERVICE_ALERT".toLowerCase())) 
				{
					// SA NO
					siXpath = "//SERVICE_INFORMATION/SA_NUMBER";
				} 
				else if (documentTypeRefKey.trim().toLowerCase().equals("TECHNICAL_INFORMATION".toLowerCase())) 
				{
					// TI NO
					siXpath = "//SERVICE_INFORMATION/TI_NUMBER";
				}

				logger.info("identifySIDocumentTypeName :: SI Number XPATH :: > " + siXpath);

				/*
				 * FETCH SI NUMBER AND ISSUE DATE on the BASIS OF CONTENT ID &
				 * XPATH
				 */
				if (null != docDetails.getRecordId() && !"".equals(docDetails.getRecordId())) 
				{
					String sql = "SELECT A.VALUE,A.XPATH FROM OK_IM.CONTENTVALUE A, OK_IM.CONTENTTEXTPUB B WHERE "
							+ " B.CONTENTTEXTID=? AND A.CONTENTTEXTPUBID = B.RECORDID ";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, docDetails.getRecordId().trim());
					rs = pstmt.executeQuery();
					while (rs.next()) 
					{
						String xPath = rs.getString("XPATH");
						String value = rs.getString("VALUE");
						// SI NUMBER
						if (null != xPath && !"".equals(xPath) 	&& null != siXpath && !"".equals(siXpath)) 
						{
							if (xPath.trim().equals(siXpath.trim())) 
							{
								docDetails.setSiNumber(value);
								logger.info("identifySIDocumentTypeName :: SI Number :: > " + value);
							}
						}
						// ISSUE DATE
						if (null != xPath && !"".equals(xPath) && null != issueDateXpath && !"".equals(issueDateXpath)) 
						{
							if (xPath.trim().equals(issueDateXpath.trim())) 
							{
								docDetails.setIssueDate(value);
								logger.info("identifySIDocumentTypeName :: Issue Date :: > " + value);
							}
						}
						// FIRT PUBLICATION DATE
						if (null != xPath && !"".equals(xPath) 	&& null != firstPublicationXpath && !"".equals(firstPublicationXpath)) 
						{
							if (xPath.trim().equals(firstPublicationXpath.trim())) 
							{
								docDetails.setFirstPublicationDate(value);
								logger.info("identifySIDocumentTypeName :: First Publication Date :: > " + value);
							}
						}
						xPath = null;
						value = null;

					}
					rs.close();
					rs = null;
					pstmt.close();
					pstmt = null;
					sql = null;
				}
				siXpath = null;
				issueDateXpath = null;
				firstPublicationXpath = null;
			}
			catch (Exception e) 
			{
				Utilities.printStackTraceToLogs(FetchIMDataDAO.class.getName(),"identifySIDocumentTypeName()", e);
			}
			documentTypeRefKey = null;
		}
		return docDetails;
	}
	
	private static String getDescription(String recordId, Connection conn)
	{
		String description="";
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			/*
			 * FETCH DESCRIPTION APPLICABLE ONLY FOR SI
			 * CHANNEL DOCUMENTS
			 */
			String getDescriptionSql = "SELECT A.VALUE FROM OK_IM.CONTENTVALUE A, OK_IM.CONTENTTEXTPUB B WHERE "
					+ " B.CONTENTTEXTID='"+ recordId.trim()	+ "' AND A.CONTENTTEXTPUBID = B.RECORDID AND "
					+ " A.XPATH='//SERVICE_INFORMATION/DESCRIPTION' ORDER BY A.SEQUENCE ASC";
			stmt = conn.createStatement();
			rs = stmt.executeQuery(getDescriptionSql);
			String tempDesc = "";
			while (rs.next()) 
			{
				if (null != rs.getString("VALUE") && !"".equals(rs.getString("VALUE"))) 
				{
					tempDesc = tempDesc+ rs.getString("VALUE");
				}
			}
			rs.close();
			rs = null;
			stmt.close();
			stmt = null;
			getDescriptionSql = null;

			if (null != tempDesc && !"".equals(tempDesc)) 
			{
				// CONVERT THIS TO HTML AND THEN GET BODY
				// TEXT
				org.jsoup.nodes.Document doc = Jsoup.parse(tempDesc);
				org.jsoup.select.Elements bodyTagsList = doc.select("body");
				if (null != bodyTagsList
						&& bodyTagsList.size() > 0) 
				{
					Element bodyEle = bodyTagsList.get(0);
					description = bodyEle.text();
					bodyEle = null;
				}
				bodyTagsList = null;
				doc = null;
			}
			tempDesc = null;
		
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(FetchIMDataDAO.class.getName(), "getDescription()", e);
		}
		return description;
	}


	public static String getCategoryNameOnObjectId(String objectId, String locale, String requestType, Connection conn, String closeConnection)throws SQLException
	{
		String categoryName="";
		ResultSet rs=  null;
		Statement stmt = null;
		try
		{
			if(null!=objectId && !"".equals(objectId) && null!=locale && !"".equals(locale))
			{
				if(null==conn || conn.isClosed()==true)
				{
					conn = getConnection();
				}
				String enUkLocale = ApplicationProperties.getProperty("en_uk");
				String enUsLocale = ApplicationProperties.getProperty("en_us");
				String sql = "SELECT A.NAME FROM OK_IM.TAGRESOURCE A, OK_IM.TAG B where A.TAGID = B.RECORDID and B.OBJECTID='"+objectId+"' AND";
				if(null!=requestType && requestType.equals("MME"))
				{
					sql = sql+"(A.LOCALEID='"+locale+"' OR A.LOCALEID='"+enUkLocale+"')";
				}
				else
				{
					if(locale.trim().toLowerCase().equals(ApplicationProperties.getProperty("en_ca").trim().toLowerCase()))
					{
						sql = sql+"(A.LOCALEID='"+locale+"' OR A.LOCALEID='"+enUsLocale+"')";
					}
					else
					{	
						sql = sql+" A.LOCALEID='"+locale+"'";
					}
				}
//				logger.info("getCategoryNameOnObjectId :: Sql :: > "+ sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					categoryName = rs.getString("NAME");
				}
				sql = null;
				enUkLocale = null;
				enUsLocale = null;
				if(null==closeConnection)
				{
					// close connection
					if(null!=conn)
					{
						conn.close();
					}
				}
			}
			else
			{
				logger.info("getCategoryNameOnObjectId :: Locale id / Object id as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(FetchIMDataDAO.class.getName(), "getCategoryNameOnObjectId()", e);
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
		}
		return categoryName;
	}
	
	public static String getCategoryRefKeyOnObjectId(String objectId, Connection conn, String closeConnection)throws SQLException
	{
		String categoryName="";
		ResultSet rs=  null;
		Statement stmt = null;
		try
		{
			if(null!=objectId && !"".equals(objectId))
			{
				if(null==conn || conn.isClosed()==true)
				{
					conn = getConnection();
				}
				
				String sql = "SELECT REFERENCEKEY FROM OK_IM.TAG WHERE OBJECTID='"+objectId+"' ";
//				logger.info("getCategoryRefKeyOnObjectId :: Sql :: > "+ sql);
				stmt = conn.createStatement();
				rs = stmt.executeQuery(sql);
				if(rs.next())
				{
					categoryName = rs.getString("REFERENCEKEY");
				}
				sql = null;
				if(null==closeConnection)
				{
					// close connection
					if(null!=conn)
					{
						conn.close();
					}
				}
			}
			else
			{
				logger.info("getCategoryRefKeyOnObjectId :: Object id as Parameter is null.");
			}
		}
		catch(Exception e)
		{
			Utilities.printStackTraceToLogs(FetchIMDataDAO.class.getName(), "getCategoryRefKeyOnObjectId()", e);
		}
		finally
		{
			if(null!=rs)
				rs.close();
			if(null!=stmt)
				stmt.close();
		}
		return categoryName;
	}
}