package com.mazda.gms3.dmt.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

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

import com.mazda.gms3.dmt.dao.ScheduleDAO;
import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;
import com.mazda.gms3.dmt.vo.MarketLocaleMasterDataTypeMapping;


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

	/**
	 * The element serialised as XML text, CDATA sections kept as they are. Replaces the Axis
	 * XMLUtils.ElementToString() call so that this class needs no InfoManager client library.
	 */
	private static String elementToString(Element element)
	{
		try
		{
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(element), new StreamResult(writer));
			return writer.toString();
		}
		catch(Exception e)
		{
			printStackTraceToLogs(Utilities.class.getName(), "elementToString()", e);
			return null;
		}
	}

	public static String readNodeValue(Node node)
	{
		String nodeValue="";
		Element valueElement= (Element)node;
		String text = elementToString(valueElement);
		if(null!=text && !"".equals(text))
		{
			if(text.contains("<![CDATA["))
			{
				// GET CDATA VALUE OF THE ELEMENT
				nodeValue = Utilities.getCharacterDataFromElement(valueElement);
			}
			else
			{
				nodeValue = valueElement.getTextContent();
			}
		}
		text= null;
		valueElement = null;
		return nodeValue;
	}
	
	/**
	 * Function will help to get the CDATA value of an Element in the XML Document
	 * @param e
	 * @return
	 */
	private static String getCharacterDataFromElement(Element e) {
		Node child = e.getFirstChild();
		if (child instanceof CharacterData) {
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		return "";
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

	public static String replaceCharsForRefKeys(String refKey)
	{
		refKey = refKey.replace("&", "AND");
		refKey = refKey.replace("-", "_");
		refKey = refKey.replace(" ", "_");
		refKey = refKey.replace("/", "_");
		refKey = refKey.replace("*", "_");
		refKey = refKey.replace("#", "_");
		
		// replace multiple underscores by 1
//		refKey= refKey.replace("_____", "_");
//		refKey= refKey.replace("____", "_");
//		refKey= refKey.replace("___", "_");
//		refKey= refKey.replace("__", "_");
		return refKey;
	}
	
	public static String getMarketWiseLocalesList(String market)
	{
		String locales="";
		try
		{
			if(null!=market && !"".equals(market))
			{
				// get Markets All Locales
				List<MarketLocaleMasterDataTypeMapping> list = ScheduleDAO.getMarketBasedLocalesList(market);
				if(null!=list && list.size()>0)
				{
					MarketLocaleMasterDataTypeMapping data = null;
					for(int a=0;a<list.size();a++)
					{
						data = (MarketLocaleMasterDataTypeMapping)list.get(a);
						locales+=data.getLocale();
						if(a!=(list.size()-1))
						{
							locales+=",";
						}
						data  = null;
					}
					data = null;
				}
				list = null;
				if(null!=locales && !"".equals(locales))
				{
					locales = locales.replace("-", "_");
				}
			}
		}
		catch(Exception e)
		{
			printStackTraceToLogs(Utilities.class.getName(), "getMarketWiseLocalesList()", e);
		}
		return locales;
	}
	

	/**
	 * The function will convert FULL WIDTH CHARS TO HAFL WIDTH AND MAKE THEM IN READABLE FORMAT IN UTF-8
	 * APPLICABLE FOR JAPANESE CHARS MAJORLY - KATAKANA CHARSET
	 * @param fullwidthstr
	 * @return
	 */
	public static String converFullwidth2HalfWidth1(String fullwidthstr) 
	{ 
		if (null == fullwidthstr || fullwidthstr.length () <= 0) { 
			return ""; 
		} 
		char[] chararray = fullwidthstr.toCharArray(); 
		//Char array traversal of full-width character conversions 
		for (int i = 0; i < chararray.length; ++i) { 
			int charintvalue = (int) chararray[i]; 
			//If the conversion relationship is satisfied, the offset between the corresponding subscript is reduced by 65248; if it's a space, just do the conversion. 
			if (charintvalue >= 65281 && charintvalue <= 65374) { 
				chararray[i] = (char) (charintvalue-65248);
			}
			else if (charintvalue == 12288) { 
				chararray[i] = (char) 32; 
			} 
		} 
		return new String (chararray); 
	}
	
	/**
	 * SQL predicate that is true when the given kapture_cms_db.k_user status column holds one
	 * of the values configured in kapture.user.active.status. Compared upper-cased and trimmed
	 * because Kapture stores "Active" for one user and "ACTIVE" for the next.
	 */
	public static String activeUserStatusPredicate(String column)
	{
		String configured = ApplicationProperties.getProperty("kapture.user.active.status");
		java.util.ArrayList<String> tokens = new java.util.ArrayList<String>();
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
			throw new IllegalStateException("kapture.user.active.status is not set in application.properties");
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

	public static String replaceJunkCharacterToFullbyte(String content, String modelFolderName, String manualType, String faceliftFolderName,String materialFolderName,String localeFolderName)
	{
		if(localeFolderName.trim().toLowerCase().equals("ja-jp") && modelFolderName.trim().toLowerCase().equals("Suzuki_azwagon_mj".toLowerCase()) &&
				manualType.trim().toLowerCase().equals("wm") && faceliftFolderName.trim().toLowerCase().equals("FL0004".toLowerCase()) && 
				materialFolderName.trim().toLowerCase().equals("E-W1J-4TE1_DJVU".toLowerCase()))
		{
			// replace ? by 2 byte character
			content = content.replace("?", "１");
		}
		else if(localeFolderName.trim().toLowerCase().equals("ja-jp") && modelFolderName.trim().toLowerCase().equals("Suzuki_azwagon_mj".toLowerCase()) &&
				manualType.trim().toLowerCase().equals("wm") && faceliftFolderName.trim().toLowerCase().equals("FL0005".toLowerCase()) && 
				materialFolderName.trim().toLowerCase().equals("E-W1J-4TE2_DJVU".toLowerCase()))
		{
			// replace ? by 2 byte character
			content = content.replace("?", "１");
		}
		return content;
	}
}
