package com.mazda.gms3.mdm.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.vo.SelectItemDetails;

public class Utilities {

	static Logger logger = LogManager.getLogger(Utilities.class);
	
	public static void printStackTraceToLogs(String className, String methodName, Exception e)
	{
		try
		{
			Writer writer = new StringWriter();
			PrintWriter print = new PrintWriter(writer);
			e.printStackTrace(print);
			
			logger.info(className+"::"+methodName+":: Error :: > " + e.getMessage());
			logger.info(className+"::"+methodName+":: Error :: > " + writer.toString());
//			String errorCode = e.getMessage();
//			String errorMessage = writer.toString();
			
			print = null;
			writer= null;
		}
		catch(Exception f)
		{
			f.printStackTrace();
		}
	}	
	
	public static String readInputStramToString(InputStream is) 
	{
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try
		{
			if(null!=is)
			{
				String line;
				br = new BufferedReader(new InputStreamReader(is));
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
			}
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally 
		{
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}

	public static String fromatDateForEmail(Date date)
	{
		String convertedDate="";
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
		convertedDate = sdf.format(date);
		return convertedDate;
	}
	
	public static String convertStringForDisplay(String name)
	{
		try
		{
			if(null!=name && !"".equals(name))
			{
				name = name.trim();
				String newName="";
				/*
				 * here prepare the name in format - General Information
				 */
				String[] tokens = name.split(" ");
				if(null!=tokens && tokens.length>0)
				{
					for(int i=0;i<tokens.length;i++)
					{
						String key = tokens[i];
						String newKey = "";
						if(null!=key && !"".equals(key))
						{
							String firstAlphabet = key.substring(0,1);
							String restAlphabet = key.substring(1,key.length());
							// now Update key
							if(null!=firstAlphabet && !"".equals(firstAlphabet))
							{
								newKey = firstAlphabet.toUpperCase();
							}
							if(null!=restAlphabet && !"".equals(restAlphabet))
							{
								newKey = newKey+restAlphabet.toLowerCase();
							}
							firstAlphabet = null;
							restAlphabet= null;
						}
						
						if(null==newKey || "".equals(newKey))
						{
							newKey = key;
						}
						// add newKey to newName
						if(null!=newKey && !"".equals(newKey))
						{
							if(null!=newName && !"".equals(newName))
							{
								newName= newName+" "+newKey;
							}
							else
							{
								newName= newKey;
							}
						}
						newKey= null;
						key = null;
					}
				}
				
				if(null!=newName && !"".equals(newName))
				{
					name = newName;
				}
				newName= null;
			}
		}
		catch(Exception e)
		{
			printStackTraceToLogs(Utilities.class.getName(), "convertStringForDisplay()", e);
		}
		return name;
	}

	public static String convertStringForCode(String code)
	{
		try
		{
			if(null!=code && !"".equals(code))
			{
				code = code.trim();
				String newCode="";
				/*
				 * here prepare the name in format - General Information
				 */
				String[] tokens = code.split("-");
				if(null!=tokens && tokens.length>0)
				{
					String key = tokens[0];
					String keyAfter = tokens[1];
					String newKey = "";
					newKey= key.toLowerCase()+"-"+keyAfter.toUpperCase();
					
					// add newKey to newName
					if(null!=newKey && !"".equals(newKey))
					{
						newCode = newKey;
					}
					newKey= null;
					key = null;
					keyAfter=  null;
				}
				
				if(null!=newCode && !"".equals(newCode))
				{
					code = newCode;
				}
				newCode= null;
			}
		}
		catch(Exception e)
		{
			printStackTraceToLogs(Utilities.class.getName(), "convertStringForCode()", e);
		}
		return code;
	}


	public static ArrayList<SelectItemDetails> prepareFlagsList(MessageProperties msgProps)
	{
		ArrayList<SelectItemDetails> flagsList = new ArrayList<SelectItemDetails>();
		
		SelectItemDetails si = new SelectItemDetails();
		si.setLabel(msgProps.getProperty("flag.label.active"));
		si.setValue(ApplicationProperties.getProperty("flag.value.active"));
		flagsList.add(si);
		si = null;
		
		/*si = new SelectItemDetails();
		si.setLabel(msgProps.getProperty("flag.label.sleep"));
		si.setValue(ApplicationProperties.getProperty("flag.value.sleep"));
		flagsList.add(si);
		si = null;*/
		
		si = new SelectItemDetails();
		si.setLabel(msgProps.getProperty("flag.label.draft"));
		si.setValue(ApplicationProperties.getProperty("flag.value.draft"));
		flagsList.add(si);
		si = null;
		

//		si = new SelectItemDetails();
//		si.setLabel(msgProps.getProperty("flag.label.deprecated"));
//		si.setValue(ApplicationProperties.getProperty("flag.value.deprecated"));
//		flagsList.add(si);
//		si = null;
//		
		return flagsList;
	}
	
	public static String replaceRefKeys(String refKey)
	{
		refKey = refKey.trim().toUpperCase();
		refKey = refKey.replace("-", "_");
		refKey = refKey.replace("/", "_");
		refKey = refKey.replace(" ", "_");
		refKey = refKey.replace("&", "_");
		refKey = refKey.replace("*", "_");
		refKey = refKey.replace("#", "_");
		return refKey;
	}
	
	/**
	 * Function will generate Exported Data Zips
	 * @param zipPath
	 * @param excelFile
	 * @return
	 */
	public static boolean createReportsZip(String zipPath, File excelFile) 
	{
		try
		{
			/*
			 * Path for the ZIP File will be same as reportsDirectoryPath and
			 * name will be - schDate_REPOROTS.ZIP
			 */
			if (null != zipPath && !"".equals(zipPath) && excelFile.isFile() && excelFile.exists()) 
			{
				FileOutputStream fos = new FileOutputStream(zipPath);
				ZipOutputStream zos = new ZipOutputStream(fos);
				addToZipFile(excelFile, zos);
				zos.close();
				fos.close();

				/*
				 * Now check here, if the Zip file is generated, then check for
				 * all the other files and delete them inside the directory
				 */
				File zip = new File(zipPath);
				if (zip.exists() && zip.length() > 0) 
				{
					logger.info("createReportsZip :: Zip file {"
							+ zipPath
							+ "} Generated Successfully");
					/*
					 * Proceed for deleting all the other files
					 */
					if (excelFile.exists() && excelFile.isFile()) {
						excelFile.delete();
					}
				} 
				else 
				{
					logger.info("createReportsZip :: Failed to generate Zip file {"
							+ zipPath
							+ "}. Download the Excel reports manually.");
					return false;
				}
				zipPath = null;
			} else {
				logger.info("createReportsZip :: Reports Directory Path and Scheduled Date is passed as null in parameters. Return false");
				return false;
			}
		} catch (FileNotFoundException e) {
			Utilities.printStackTraceToLogs(Utilities.class.getName(), "createReportsZip()", e);
			logger.info("createReportsZip :: File Not Found Exception :: >"	+ e.getMessage());
			return false;
		} catch (IOException e) {
			Utilities.printStackTraceToLogs(Utilities.class.getName(), "createReportsZip()", e);
			logger.info("createReportsZip :: Input Output Found Exception :: >"
					+ e.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * Function will add each Child file to the Zip file
	 * 
	 * @param childFile
	 * @param zos
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void addToZipFile(File childFile, ZipOutputStream zos)
			throws FileNotFoundException, IOException {
		logger.info("addToZipFile :: Writing '" + childFile.getName()
				+ "' to zip file");
		FileInputStream fis = new FileInputStream(childFile);
		zos.putNextEntry(new ZipEntry(childFile.getName()));
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zos.write(bytes, 0, length);
		}

		zos.closeEntry();
		fis.close();
	}
	
	public static String transformString(Document doc)
	{	
		String stringDoc=null;
		try
		{
			StringWriter stw = new StringWriter();
			Transformer serializer = TransformerFactory.newInstance().newTransformer();
			serializer.transform(new DOMSource(doc), new StreamResult(stw));
			stringDoc=stw.toString();
		}
		catch(TransformerConfigurationException tce)
		{
			return null;
		}
		catch(TransformerFactoryConfigurationError tfc)
		{
			return null;
		}
		catch(TransformerException te)
		{
			return null;
		}
		return stringDoc;
	}
	
	/**
	 * Function will help to get the CDATA value of an Element in the XML Document
	 * @param e
	 * @return
	 */
	public static String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		return "";
	}


	/**
	 * THE LOCALE, IN THE FORM A TABLE NAME USES: lowercase, hyphen turned into underscore.
	 *
	 * EVERY per-locale table name IS BUILT THROUGH THIS - gms3_dmt_&lt;locale&gt;_imdoc,
	 * gms3_dmt_&lt;locale&gt;_nm_imdoc, gms3_dmt_&lt;locale&gt;_cd_data, gms3_vc_mme_vin_dtl_&lt;locale&gt;.
	 *
	 * WHY IT IS A METHOD AND NOT AN EXPRESSION REPEATED AT EACH SITE. MySQL on Linux is CASE
	 * SENSITIVE about table names and the migrated schema is lowercase, so a name built with
	 * toUpperCase() resolves on a Windows workstation and fails on the deployed server - it works
	 * everywhere it is developed and nowhere it matters. That mistake was made independently in
	 * four screens, and found only by sweeping for it. One rule in one place cannot drift; the
	 * twenty-six copies of this expression it replaces already had.
	 *
	 * The locale arrives as en-UK from the screens and en_UK from the import books, so BOTH
	 * separators are accepted and normalised to the underscore the tables use.
	 *
	 * @param locale a locale in either separator and any case, e.g. "en-UK" or "en_UK"
	 * @return the table-name form, e.g. "en_uk"; "" when the locale is null or blank
	 */
	public static String tableLocale(String locale)
	{
		if(null==locale || "".equals(locale.trim()))
		{
			return "";
		}
		return locale.trim().replace("-", "_").toLowerCase();
	}

	/**
	 * SQL PREDICATE THAT MATCHES AN *ACTIVE* KAPTURE USER, WHATEVER CASE THE ROW HAPPENS TO USE.
	 *
	 * kapture_cms_db.k_user.user_status is not written consistently - the same environment holds
	 * "Active" for one user and "ACTIVE" for the next, and Kapture also uses the short form "A".
	 * An exact <code>user_status = 'Active'</code> therefore silently treats a live user as
	 * inactive: they get no notification e-mail, or fail to log in, with nothing in the logs to
	 * say why. Comparing UPPER(TRIM(...)) against a configured set removes the guesswork.
	 *
	 * The accepted values come from <code>kapture.user.active.status</code> so a new spelling can
	 * be handled by editing the deployed properties file, without a rebuild. Matching is case
	 * insensitive, so listing "Active" is enough to accept "ACTIVE" and "active" too.
	 *
	 * Only letters, digits and underscore survive from each configured token: the result is
	 * concatenated into SQL, and a stray quote in a properties file must not be able to change the
	 * statement. A blank or missing property falls back to ACTIVE and A rather than matching
	 * nothing, because a typo there would otherwise lock every user out.
	 *
	 * @param column the qualified column to test, e.g. "user_status" or "u.user_status"
	 * @return a ready-to-concatenate predicate, e.g. <code>UPPER(TRIM(user_status)) IN ('ACTIVE','A')</code>
	 */
	public static String activeUserStatusPredicate(String column)
	{
		String configured = ApplicationProperties.getProperty("kapture.user.active.status");
		ArrayList<String> tokens = new ArrayList<String>();
		if(null!=configured && !"".equals(configured.trim()))
		{
			String[] parts = configured.trim().split(",");
			for(int i=0;i<parts.length;i++)
			{
				String token = (null==parts[i]) ? "" : parts[i].trim().toUpperCase();
				token = token.replaceAll("[^A-Z0-9_]", "");
				if(!"".equals(token) && !tokens.contains(token))
				{
					tokens.add(token);
				}
				token = null;
			}
			parts = null;
		}
		if(tokens.isEmpty())
		{
			tokens.add("ACTIVE");
			tokens.add("A");
		}

		StringBuilder in = new StringBuilder();
		for(int i=0;i<tokens.size();i++)
		{
			if(i>0)
			{
				in.append(",");
			}
			in.append("'").append(tokens.get(i)).append("'");
		}
		tokens = null;
		return " UPPER(TRIM(" + column + ")) IN (" + in.toString() + ") ";
	}

}
