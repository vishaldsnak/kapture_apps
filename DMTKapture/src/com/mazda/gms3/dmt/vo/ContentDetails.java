package com.mazda.gms3.dmt.vo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.mazda.gms3.dmt.mc.vo.DisplayOrderDetails;

import com.mazda.gms3.dmt.mc.vo.VinDetails;

public class ContentDetails {

	public static final String PDF_DOCUMENT="PDF";
	public static final String ENT_DOCUMENT="ENTF";
	public static final String HTML_DOCUMENT="HTML";
	public static final String HTML5_DOCUMENT="HTML5";
	public static final String FLASH_DOCUMENT="FLASH";
	public static final String XML_DOCUMENT="XML";
	
	public static final String HTML_MC_DOCUMENT="HTML_MC";
	public static final String DJVU_DOCUMENT="DJVU";
	
	public static final String IM_DOC_TYPE_PARENT="";
	public static final String IM_DOC_TYPE_CHILD="SAME LOCALE";
	public static final String IM_DOC_TYPE_REPURPOSE="MULTIPLE LOCALE";
	
	
	private String previousDocumentId=null;
	private String threadId=null;
	private String imPrintReportDocType=null;
	private String scheduleId=null;
	private String itemId=null;
	private String market=null;
	private String locale=null;
	private String model=null;
	private String modelType=null;
	private String carlineCode=null;
	private String modelFolderName=null;
	private String manualType=null;
	private String faceLiftFolderName=null;
	private String materialName=null;
	private String fileNameAttribute=null;
	private String fileName=null;
	// THIS WILL BE USED FOR REPROTS PURPOSE
	private String fileAbsolutePath=null;
	private String filePath = null;
	private String documentType=null;
	private String bookFolderCode=null;
	
	private String yearFolderName=null;
	
	private String channelName=null;
	
	private Long fileData=null;
	
	private ArrayList<VINEntFileDetails> vinEntDetailsList = new ArrayList<VINEntFileDetails>();
	private ArrayList<VinAttributeEntFileDetails> vinAttributeEntDetailsList = new ArrayList<VinAttributeEntFileDetails>();
	
	private String title=null;
	private String categoryCode=null;
	private String categoryName=null;
	private String categoryRefKey=null;
	
	private String subCategoryCode=null;
	private String subCategoryName=null;
	private String subCategoryRefKey=null;
	
	private String subSubCategoryCode=null;
	
	private String previousLink=null;
	private String nextLink=null;
	private String previousLinkDocId=null;
	private String nextLinkDocId=null;
	
	private String documentContent=null;
	private String documentContentWithOutLinksUpdate=null;
	private String imProcessingStatus =null;
	
	private String pdfFilePathAsAttachment=null;
	private String pdfFileNameAsAttachment=null;
	/*
	 * IM ATTRIBUTES
	 */
	
	private String imDocumentId=null;
	private String imResourcePath=null;
	private String imContentType=null;
	private String imVersion=null;
	private String imDocStatus=null;
	
	private String year=null;
	private String mappedVinEntFileName=null;
	private String mappedVinAttributeFileName=null;
	
	private String rhdlhdIndicator=null;
	
	private ManualTypeDetails manualTypeDetails = null;
	
	private ArrayList<UserGroupDetails> userGroupsList = new ArrayList<UserGroupDetails>();
	
	private String navigation=null;
	private String flashContent=null;
	private String fontContent=null;
	
	private ArrayList<CategoryDetails> categoryList = new ArrayList<CategoryDetails>();
	
	private String wslId=null;
	private String errorComments=null;
	private String reportCurrentVersion=null;
	private String reportProcessedVersion=null;
	private String reportUdatedVersion=null;
	
	/*
	 * Author Id and Owner Id for IMWS Test Client
	 * and Other Variables as well
	 */
	private String authorId=null;
	private String ownerId=null;
	private String type=null;
	private String typeGuid=null;
	private String repository=null;
	private String repositoryGuid=null;
	
	
	private ArrayList<LinkDetails> innerLinksList = new ArrayList<LinkDetails>();
	private ArrayList<VINEntFileDetails> masterVinList = new ArrayList<VINEntFileDetails>();
	
	// CREATE / UPDATE / DELETE
	private String processingOperationType="";
	private Timestamp processingTime=null;
	private String processingOperationStatus=null;
	
	private ArrayList<CVCCategoryDetails> cvcCategoryList  = new ArrayList<CVCCategoryDetails>();
	
	private String uploadDirectoryPath=null;
	
	
	private String reportIMStatus=null;
	private String reporDBStatus=null;
	private String reportCategoryMappingStatus=null;
	private String reportInnerLinkMappingStatus=null;
	private String reportXCopyStatus=null;
	
	private boolean documentDeleted=false;
	
	private String vinDeleteFilePath=null;
	
	// USED TO STORE LEFT MENU LIST WHILE PROCESSING WITH THE DOCUMENT , REQUIRED WHEN UPDATING INNER LINKS AFTER JOB COMPLETION.
	private ArrayList<LeftMenuFileDetails> leftMenuList = new ArrayList<LeftMenuFileDetails>();
	
	
	// USED FOR SHOWING WHICH LEVEL CATEGORY IS MAPPED
	private String mappedCategoryLevel=null;
	private String mappedCategoryName=null;
	private String mappedTaxonomy=null;
	
	
	// VARIABLE FOR STORING WHETHER ENTRY FOUND IN LEFT MENU YES OR NO
	private String entryFoundInLeftMenu=null;
	// VARIABLE FOR STORING WHETHER ENTRY FOUND IN ESI CAT YES OR NO
	private String entryFoundInEsiCat = null;
	
	// VARIABLE FOR STORING PARENT DOCUMENT DETAILS FOR RE-PURPOSE MULTIPLE LOCALE
	private ContentDetails repurposeMultipleLocaleParentDetails=null;
	
	// VARIABLE APPLICABLE FOR MC
	private ArrayList<VinDetails> applicableVINList=null;
	private ArrayList<DisplayOrderDetails> applicableDisplayOrderList =null;
	private String esiCategoryMapped=null;
	private String esiCategoryMappedLevel=null;
	private String esiCategoryMappedRefKey=null;
	private String esiCategoryMappedFileName=null;
	private String esiCategoryMappedFilePath=null;
	
	private String vinMapped=null;
	private String vinMappedFileName=null;
	
	private String displayOrderMapped=null;
	private String displayOrderMappedFileName=null;
	
	private String replacePDFBYHTML=null;
	private String containsBothHTMLPDFForMNAO=null;
	
	private String innerLinkFound=null;
	private String allInnerLinksMapped=null;
	private String innerLinkMappingReason=null;
	
	private ArrayList<String> applicableEngineMissionTypeList = null;
	
	private String operationType=null;
	
	private String esiSteeringTypeInfo=null;
	private String oasisDirectoryPath=null;
	// OEM content (SM / OSM): web path of the document file in OKAssets, file name included
	private String oemFileLocation=null;
	
	
	private ArrayList<CategoryDetails> modelsWithMissingYearsList = null;
	private boolean toyotaContent=false;
	private String metaTagDescription=null;
	
	
	private String html5FileSourcePath=null;
	
	private String documentId=null;
	private Timestamp documentCreatedTime=null;
	private Timestamp documentModifiedTime=null;
	private String documentTypeRefKey=null;
	private String documentTypeName=null;
	
	private String esiCategoryLevel1Name=null;
	private String esiCategoryLevel2Name=null;
	private String esiCategoryLevel3Name=null;
	private String esiCatFlag =null;
	
	
	private List<LinkDetails> additionalInnerLinks=null;
	
	
	public List<LinkDetails> getAdditionalInnerLinks() {
		return additionalInnerLinks;
	}

	public void setAdditionalInnerLinks(List<LinkDetails> additionalInnerLinks) {
		this.additionalInnerLinks = additionalInnerLinks;
	}

	public String getEsiCategoryLevel1Name() {
		return esiCategoryLevel1Name;
	}

	public void setEsiCategoryLevel1Name(String esiCategoryLevel1Name) {
		this.esiCategoryLevel1Name = esiCategoryLevel1Name;
	}

	public String getEsiCategoryLevel2Name() {
		return esiCategoryLevel2Name;
	}

	public void setEsiCategoryLevel2Name(String esiCategoryLevel2Name) {
		this.esiCategoryLevel2Name = esiCategoryLevel2Name;
	}

	public String getEsiCategoryLevel3Name() {
		return esiCategoryLevel3Name;
	}

	public void setEsiCategoryLevel3Name(String esiCategoryLevel3Name) {
		this.esiCategoryLevel3Name = esiCategoryLevel3Name;
	}

	public String getEsiCatFlag() {
		return esiCatFlag;
	}

	public void setEsiCatFlag(String esiCatFlag) {
		this.esiCatFlag = esiCatFlag;
	}

	public String getDocumentTypeRefKey() {
		return documentTypeRefKey;
	}

	public void setDocumentTypeRefKey(String documentTypeRefKey) {
		this.documentTypeRefKey = documentTypeRefKey;
	}

	public String getDocumentTypeName() {
		return documentTypeName;
	}

	public void setDocumentTypeName(String documentTypeName) {
		this.documentTypeName = documentTypeName;
	}

	public Timestamp getDocumentCreatedTime() {
		return documentCreatedTime;
	}

	public void setDocumentCreatedTime(Timestamp documentCreatedTime) {
		this.documentCreatedTime = documentCreatedTime;
	}

	public Timestamp getDocumentModifiedTime() {
		return documentModifiedTime;
	}

	public void setDocumentModifiedTime(Timestamp documentModifiedTime) {
		this.documentModifiedTime = documentModifiedTime;
	}

	public String getDocumentId() {
		return documentId;
	}

	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}

	public String getHtml5FileSourcePath() {
		return html5FileSourcePath;
	}

	public void setHtml5FileSourcePath(String html5FileSourcePath) {
		this.html5FileSourcePath = html5FileSourcePath;
	}

	public String getMetaTagDescription() {
		return metaTagDescription;
	}

	public void setMetaTagDescription(String metaTagDescription) {
		this.metaTagDescription = metaTagDescription;
	}

	public boolean isToyotaContent() {
		return toyotaContent;
	}

	public void setToyotaContent(boolean toyotaContent) {
		this.toyotaContent = toyotaContent;
	}

	public ArrayList<CategoryDetails> getModelsWithMissingYearsList() {
		return modelsWithMissingYearsList;
	}

	public void setModelsWithMissingYearsList(
			ArrayList<CategoryDetails> modelsWithMissingYearsList) {
		this.modelsWithMissingYearsList = modelsWithMissingYearsList;
	}

	public String getOasisDirectoryPath() {
		return oasisDirectoryPath;
	}

	public void setOasisDirectoryPath(String oasisDirectoryPath) {
		this.oasisDirectoryPath = oasisDirectoryPath;
	}

	public String getEsiSteeringTypeInfo() {
		return esiSteeringTypeInfo;
	}

	public void setEsiSteeringTypeInfo(String esiSteeringTypeInfo) {
		this.esiSteeringTypeInfo = esiSteeringTypeInfo;
	}

	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	public String getContainsBothHTMLPDFForMNAO() {
		return containsBothHTMLPDFForMNAO;
	}

	public void setContainsBothHTMLPDFForMNAO(String containsBothHTMLPDFForMNAO) {
		this.containsBothHTMLPDFForMNAO = containsBothHTMLPDFForMNAO;
	}

	public ArrayList<String> getApplicableEngineMissionTypeList() {
		return applicableEngineMissionTypeList;
	}

	public void setApplicableEngineMissionTypeList(
			ArrayList<String> applicableEngineMissionTypeList) {
		this.applicableEngineMissionTypeList = applicableEngineMissionTypeList;
	}

	public String getInnerLinkFound() {
		return innerLinkFound;
	}

	public void setInnerLinkFound(String innerLinkFound) {
		this.innerLinkFound = innerLinkFound;
	}

	public String getAllInnerLinksMapped() {
		return allInnerLinksMapped;
	}

	public void setAllInnerLinksMapped(String allInnerLinksMapped) {
		this.allInnerLinksMapped = allInnerLinksMapped;
	}

	public String getInnerLinkMappingReason() {
		return innerLinkMappingReason;
	}

	public void setInnerLinkMappingReason(String innerLinkMappingReason) {
		this.innerLinkMappingReason = innerLinkMappingReason;
	}

	public String getEsiCategoryMappedFilePath() {
		return esiCategoryMappedFilePath;
	}

	public String getReplacePDFBYHTML() {
		return replacePDFBYHTML;
	}

	public void setReplacePDFBYHTML(String replacePDFBYHTML) {
		this.replacePDFBYHTML = replacePDFBYHTML;
	}

	public void setEsiCategoryMappedFilePath(String esiCategoryMappedFilePath) {
		this.esiCategoryMappedFilePath = esiCategoryMappedFilePath;
	}

	public String getEsiCategoryMapped() {
		return esiCategoryMapped;
	}

	public void setEsiCategoryMapped(String esiCategoryMapped) {
		this.esiCategoryMapped = esiCategoryMapped;
	}

	public String getEsiCategoryMappedLevel() {
		return esiCategoryMappedLevel;
	}

	public void setEsiCategoryMappedLevel(String esiCategoryMappedLevel) {
		this.esiCategoryMappedLevel = esiCategoryMappedLevel;
	}

	public String getEsiCategoryMappedRefKey() {
		return esiCategoryMappedRefKey;
	}

	public void setEsiCategoryMappedRefKey(String esiCategoryMappedRefKey) {
		this.esiCategoryMappedRefKey = esiCategoryMappedRefKey;
	}

	public String getEsiCategoryMappedFileName() {
		return esiCategoryMappedFileName;
	}

	public void setEsiCategoryMappedFileName(String esiCategoryMappedFileName) {
		this.esiCategoryMappedFileName = esiCategoryMappedFileName;
	}

	public String getVinMapped() {
		return vinMapped;
	}

	public void setVinMapped(String vinMapped) {
		this.vinMapped = vinMapped;
	}

	public String getVinMappedFileName() {
		return vinMappedFileName;
	}

	public void setVinMappedFileName(String vinMappedFileName) {
		this.vinMappedFileName = vinMappedFileName;
	}

	public String getDisplayOrderMapped() {
		return displayOrderMapped;
	}

	public void setDisplayOrderMapped(String displayOrderMapped) {
		this.displayOrderMapped = displayOrderMapped;
	}

	public String getDisplayOrderMappedFileName() {
		return displayOrderMappedFileName;
	}

	public void setDisplayOrderMappedFileName(String displayOrderMappedFileName) {
		this.displayOrderMappedFileName = displayOrderMappedFileName;
	}

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public ArrayList<VinDetails> getApplicableVINList() {
		return applicableVINList;
	}

	public void setApplicableVINList(ArrayList<VinDetails> applicableVINList) {
		this.applicableVINList = applicableVINList;
	}

	public ArrayList<DisplayOrderDetails> getApplicableDisplayOrderList() {
		return applicableDisplayOrderList;
	}

	public void setApplicableDisplayOrderList(
			ArrayList<DisplayOrderDetails> applicableDisplayOrderList) {
		this.applicableDisplayOrderList = applicableDisplayOrderList;
	}

	public String getCarlineCode() {
		return carlineCode;
	}

	public void setCarlineCode(String carlineCode) {
		this.carlineCode = carlineCode;
	}

	public String getModelType() {
		return modelType;
	}

	public void setModelType(String modelType) {
		this.modelType = modelType;
	}

	public String getFaceLiftFolderName() {
		return faceLiftFolderName;
	}

	public void setFaceLiftFolderName(String faceLiftFolderName) {
		this.faceLiftFolderName = faceLiftFolderName;
	}

	public ContentDetails getRepurposeMultipleLocaleParentDetails() {
		return repurposeMultipleLocaleParentDetails;
	}

	public void setRepurposeMultipleLocaleParentDetails(
			ContentDetails repurposeMultipleLocaleParentDetails) {
		this.repurposeMultipleLocaleParentDetails = repurposeMultipleLocaleParentDetails;
	}

	public String getEntryFoundInLeftMenu() {
		return entryFoundInLeftMenu;
	}

	public void setEntryFoundInLeftMenu(String entryFoundInLeftMenu) {
		this.entryFoundInLeftMenu = entryFoundInLeftMenu;
	}

	public String getMappedCategoryLevel() {
		return mappedCategoryLevel;
	}

	public void setMappedCategoryLevel(String mappedCategoryLevel) {
		this.mappedCategoryLevel = mappedCategoryLevel;
	}

	public String getMappedCategoryName() {
		return mappedCategoryName;
	}

	public void setMappedCategoryName(String mappedCategoryName) {
		this.mappedCategoryName = mappedCategoryName;
	}

	public String getMappedTaxonomy() {
		return mappedTaxonomy;
	}

	public void setMappedTaxonomy(String mappedTaxonomy) {
		this.mappedTaxonomy = mappedTaxonomy;
	}

	public ArrayList<LeftMenuFileDetails> getLeftMenuList() {
		return leftMenuList;
	}

	public void setLeftMenuList(ArrayList<LeftMenuFileDetails> leftMenuList) {
		this.leftMenuList = leftMenuList;
	}

	public String getVinDeleteFilePath() {
		return vinDeleteFilePath;
	}

	public void setVinDeleteFilePath(String vinDeleteFilePath) {
		this.vinDeleteFilePath = vinDeleteFilePath;
	}

	public boolean isDocumentDeleted() {
		return documentDeleted;
	}

	public void setDocumentDeleted(boolean documentDeleted) {
		this.documentDeleted = documentDeleted;
	}

	public String getReportIMStatus() {
		return reportIMStatus;
	}

	public void setReportIMStatus(String reportIMStatus) {
		this.reportIMStatus = reportIMStatus;
	}

	public String getReporDBStatus() {
		return reporDBStatus;
	}

	public void setReporDBStatus(String reporDBStatus) {
		this.reporDBStatus = reporDBStatus;
	}

	public String getReportCategoryMappingStatus() {
		return reportCategoryMappingStatus;
	}

	public void setReportCategoryMappingStatus(String reportCategoryMappingStatus) {
		this.reportCategoryMappingStatus = reportCategoryMappingStatus;
	}

	public String getReportXCopyStatus() {
		return reportXCopyStatus;
	}

	public void setReportXCopyStatus(String reportXCopyStatus) {
		this.reportXCopyStatus = reportXCopyStatus;
	}

	public String getUploadDirectoryPath() {
		return uploadDirectoryPath;
	}

	public void setUploadDirectoryPath(String uploadDirectoryPath) {
		this.uploadDirectoryPath = uploadDirectoryPath;
	}

	public ArrayList<CVCCategoryDetails> getCvcCategoryList() {
		return cvcCategoryList;
	}

	public void setCvcCategoryList(ArrayList<CVCCategoryDetails> cvcCategoryList) {
		this.cvcCategoryList = cvcCategoryList;
	}

	public String getProcessingOperationStatus() {
		return processingOperationStatus;
	}

	public void setProcessingOperationStatus(String processingOperationStatus) {
		this.processingOperationStatus = processingOperationStatus;
	}

	public String getProcessingOperationType() {
		return processingOperationType;
	}

	public void setProcessingOperationType(String processingOperationType) {
		this.processingOperationType = processingOperationType;
	}

	public Timestamp getProcessingTime() {
		return processingTime;
	}

	public void setProcessingTime(Timestamp processingTime) {
		this.processingTime = processingTime;
	}

	public ArrayList<VINEntFileDetails> getMasterVinList() {
		return masterVinList;
	}

	public void setMasterVinList(ArrayList<VINEntFileDetails> masterVinList) {
		this.masterVinList = masterVinList;
	}

	public ArrayList<LinkDetails> getInnerLinksList() {
		return innerLinksList;
	}

	public void setInnerLinksList(ArrayList<LinkDetails> innerLinksList) {
		this.innerLinksList = innerLinksList;
	}

	public String getDocumentContentWithOutLinksUpdate() {
		return documentContentWithOutLinksUpdate;
	}

	public void setDocumentContentWithOutLinksUpdate(
			String documentContentWithOutLinksUpdate) {
		this.documentContentWithOutLinksUpdate = documentContentWithOutLinksUpdate;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public String getRepositoryGuid() {
		return repositoryGuid;
	}

	public void setRepositoryGuid(String repositoryGuid) {
		this.repositoryGuid = repositoryGuid;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTypeGuid() {
		return typeGuid;
	}

	public void setTypeGuid(String typeGuid) {
		this.typeGuid = typeGuid;
	}

	public String getAuthorId() {
		return authorId;
	}

	public void setAuthorId(String authorId) {
		this.authorId = authorId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getBookFolderCode() {
		return bookFolderCode;
	}

	public void setBookFolderCode(String bookFolderCode) {
		this.bookFolderCode = bookFolderCode;
	}

	public String getWslId() {
		return wslId;
	}

	public void setWslId(String wslId) {
		this.wslId = wslId;
	}

	public ArrayList<CategoryDetails> getCategoryList() {
		return categoryList;
	}

	public void setCategoryList(ArrayList<CategoryDetails> categoryList) {
		this.categoryList = categoryList;
	}

	public String getFileNameAttribute() {
		return fileNameAttribute;
	}

	public void setFileNameAttribute(String fileNameAttribute) {
		this.fileNameAttribute = fileNameAttribute;
	}

	public String getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(String scheduleId) {
		this.scheduleId = scheduleId;
	}

	public String getPreviousLinkDocId() {
		return previousLinkDocId;
	}

	public void setPreviousLinkDocId(String previousLinkDocId) {
		this.previousLinkDocId = previousLinkDocId;
	}

	public String getNextLinkDocId() {
		return nextLinkDocId;
	}

	public void setNextLinkDocId(String nextLinkDocId) {
		this.nextLinkDocId = nextLinkDocId;
	}

	public String getImDocStatus() {
		return imDocStatus;
	}

	public void setImDocStatus(String imDocStatus) {
		this.imDocStatus = imDocStatus;
	}

	public String getNavigation() {
		return navigation;
	}

	public void setNavigation(String navigation) {
		this.navigation = navigation;
	}

	public String getFlashContent() {
		return flashContent;
	}

	public void setFlashContent(String flashContent) {
		this.flashContent = flashContent;
	}

	public String getFontContent() {
		return fontContent;
	}

	public void setFontContent(String fontContent) {
		this.fontContent = fontContent;
	}

	public ManualTypeDetails getManualTypeDetails() {
		return manualTypeDetails;
	}

	public void setManualTypeDetails(ManualTypeDetails manualTypeDetails) {
		this.manualTypeDetails = manualTypeDetails;
	}

	public ArrayList<UserGroupDetails> getUserGroupsList() {
		return userGroupsList;
	}

	public void setUserGroupsList(ArrayList<UserGroupDetails> userGroupsList) {
		this.userGroupsList = userGroupsList;
	}

	public String getRhdlhdIndicator() {
		return rhdlhdIndicator;
	}

	public void setRhdlhdIndicator(String rhdlhdIndicator) {
		this.rhdlhdIndicator = rhdlhdIndicator;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String year) {
		this.year = year;
	}

	public String getMappedVinEntFileName() {
		return mappedVinEntFileName;
	}

	public void setMappedVinEntFileName(String mappedVinEntFileName) {
		this.mappedVinEntFileName = mappedVinEntFileName;
	}

	public String getMappedVinAttributeFileName() {
		return mappedVinAttributeFileName;
	}

	public void setMappedVinAttributeFileName(String mappedVinAttributeFileName) {
		this.mappedVinAttributeFileName = mappedVinAttributeFileName;
	}

	public String getPdfFileNameAsAttachment() {
		return pdfFileNameAsAttachment;
	}

	public void setPdfFileNameAsAttachment(String pdfFileNameAsAttachment) {
		this.pdfFileNameAsAttachment = pdfFileNameAsAttachment;
	}

	public String getPdfFilePathAsAttachment() {
		return pdfFilePathAsAttachment;
	}

	public void setPdfFilePathAsAttachment(String pdfFilePathAsAttachment) {
		this.pdfFilePathAsAttachment = pdfFilePathAsAttachment;
	}

	public String getImDocumentId() {
		return imDocumentId;
	}

	public void setImDocumentId(String imDocumentId) {
		this.imDocumentId = imDocumentId;
	}

	public String getImResourcePath() {
		return imResourcePath;
	}

	public void setImResourcePath(String imResourcePath) {
		this.imResourcePath = imResourcePath;
	}

	public String getImContentType() {
		return imContentType;
	}

	public void setImContentType(String imContentType) {
		this.imContentType = imContentType;
	}

	public String getImVersion() {
		return imVersion;
	}

	public void setImVersion(String imVersion) {
		this.imVersion = imVersion;
	}

	public String getImProcessingStatus() {
		return imProcessingStatus;
	}

	public void setImProcessingStatus(String imProcessingStatus) {
		this.imProcessingStatus = imProcessingStatus;
	}

	public String getDocumentContent() {
		return documentContent;
	}

	public void setDocumentContent(String documentContent) {
		this.documentContent = documentContent;
	}

	public String getManualType() {
		return manualType;
	}

	public void setManualType(String manualType) {
		this.manualType = manualType;
	}

	public String getPreviousLink() {
		return previousLink;
	}

	public void setPreviousLink(String previousLink) {
		this.previousLink = previousLink;
	}

	public String getNextLink() {
		return nextLink;
	}

	public void setNextLink(String nextLink) {
		this.nextLink = nextLink;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCategoryCode() {
		return categoryCode;
	}

	public void setCategoryCode(String categoryCode) {
		this.categoryCode = categoryCode;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

	public String getCategoryRefKey() {
		return categoryRefKey;
	}

	public void setCategoryRefKey(String categoryRefKey) {
		this.categoryRefKey = categoryRefKey;
	}

	public String getSubCategoryCode() {
		return subCategoryCode;
	}

	public void setSubCategoryCode(String subCategoryCode) {
		this.subCategoryCode = subCategoryCode;
	}

	public String getSubCategoryName() {
		return subCategoryName;
	}

	public void setSubCategoryName(String subCategoryName) {
		this.subCategoryName = subCategoryName;
	}

	public String getSubCategoryRefKey() {
		return subCategoryRefKey;
	}

	public void setSubCategoryRefKey(String subCategoryRefKey) {
		this.subCategoryRefKey = subCategoryRefKey;
	}

	public ArrayList<VINEntFileDetails> getVinEntDetailsList() {
		return vinEntDetailsList;
	}

	public void setVinEntDetailsList(ArrayList<VINEntFileDetails> vinEntDetailsList) {
		this.vinEntDetailsList = vinEntDetailsList;
	}

	public ArrayList<VinAttributeEntFileDetails> getVinAttributeEntDetailsList() {
		return vinAttributeEntDetailsList;
	}

	public void setVinAttributeEntDetailsList(
			ArrayList<VinAttributeEntFileDetails> vinAttributeEntDetailsList) {
		this.vinAttributeEntDetailsList = vinAttributeEntDetailsList;
	}

	public String getMarket() {
		return market;
	}

	public void setMarket(String market) {
		this.market = market;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public Long getFileData() {
		return fileData;
	}

	public void setFileData(Long fileData) {
		this.fileData = fileData;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getImPrintReportDocType() {
		return imPrintReportDocType;
	}

	public void setImPrintReportDocType(String imPrintReportDocType) {
		this.imPrintReportDocType = imPrintReportDocType;
	}

	public String getYearFolderName() {
		return yearFolderName;
	}

	public void setYearFolderName(String yearFolderName) {
		this.yearFolderName = yearFolderName;
	}

	public String getFileAbsolutePath() {
		return fileAbsolutePath;
	}

	public void setFileAbsolutePath(String fileAbsolutePath) {
		this.fileAbsolutePath = fileAbsolutePath;
	}

	public String getErrorComments() {
		return errorComments;
	}

	public void setErrorComments(String errorComments) {
		this.errorComments = errorComments;
	}
	
	public String getReportCurrentVersion() {
		return reportCurrentVersion;
	}

	public void setReportCurrentVersion(String reportCurrentVersion) {
		this.reportCurrentVersion = reportCurrentVersion;
	}

	public String getReportProcessedVersion() {
		return reportProcessedVersion;
	}

	public void setReportProcessedVersion(String reportProcessedVersion) {
		this.reportProcessedVersion = reportProcessedVersion;
	}

	public String getReportUdatedVersion() {
		return reportUdatedVersion;
	}

	public void setReportUdatedVersion(String reportUdatedVersion) {
		this.reportUdatedVersion = reportUdatedVersion;
	}

	public String getModelFolderName() {
		return modelFolderName;
	}

	public void setModelFolderName(String modelFolderName) {
		this.modelFolderName = modelFolderName;
	}

	public String getMaterialName() {
		return materialName;
	}

	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	public String getThreadId() {
		return threadId;
	}

	public void setThreadId(String threadId) {
		this.threadId = threadId;
	}

	public String getSubSubCategoryCode() {
		return subSubCategoryCode;
	}

	public void setSubSubCategoryCode(String subSubCategoryCode) {
		this.subSubCategoryCode = subSubCategoryCode;
	}

	public String getReportInnerLinkMappingStatus() {
		return reportInnerLinkMappingStatus;
	}

	public void setReportInnerLinkMappingStatus(String reportInnerLinkMappingStatus) {
		this.reportInnerLinkMappingStatus = reportInnerLinkMappingStatus;
	}

	public String getPreviousDocumentId() {
		return previousDocumentId;
	}

	public void setPreviousDocumentId(String previousDocumentId) {
		this.previousDocumentId = previousDocumentId;
	}

	public String getEntryFoundInEsiCat() {
		return entryFoundInEsiCat;
	}

	public void setEntryFoundInEsiCat(String entryFoundInEsiCat) {
		this.entryFoundInEsiCat = entryFoundInEsiCat;
	}

	

	public String getOemFileLocation() {
		return oemFileLocation;
	}

	public void setOemFileLocation(String oemFileLocation) {
		this.oemFileLocation = oemFileLocation;
	}
}
