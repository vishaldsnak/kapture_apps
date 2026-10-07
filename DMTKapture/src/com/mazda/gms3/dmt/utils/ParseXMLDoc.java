package com.mazda.gms3.dmt.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;



public class ParseXMLDoc {

	private static Logger logger = LogManager.getLogger(ParseXMLDoc.class);


	/** Parses XML file and returns XML document.
	 * @param fileName XML file to parse
	 * @return XML document or <B>null</B> if error occurred
	 */
	public static Document parseFile(String fileName) 
	{
		if(logger.isInfoEnabled())
			logger.debug("parseFile :: Parsing XML file : >" + fileName);
		DocumentBuilder docBuilder;
		Document doc = null;
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setIgnoringElementContentWhitespace(true);
		File sourceFile = PathUtil.file(fileName);
		try 
		{
			docBuilder = docBuilderFactory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) 
		{
			if(logger.isInfoEnabled())
				logger.info("parseFile :: ParserConfigurationException :: Wrong parser configuration: >" + e.getMessage());
			
			Utilities.printStackTraceToLogs(ParseXMLDoc.class.getName(), "parseFile()", e);
			return null;
		}
		
		try 
		{
			doc = docBuilder.parse(sourceFile);
		}
		catch (SAXException e) 
		{
			if(logger.isInfoEnabled());
				logger.info("parseFile :: SAXException :: Wrong XML file structure: >" + e.getMessage());
			Utilities.printStackTraceToLogs(ParseXMLDoc.class.getName(), "parseFile()", e);
			return null;
		}
		catch (IOException e) 
		{
			if(logger.isInfoEnabled())
				logger.info("parseFile :: SAXException :: Could not read source file : >" + e.getMessage());
			Utilities.printStackTraceToLogs(ParseXMLDoc.class.getName(), "parseFile()", e);
			return null;
		}
		return doc;
	}
	/** Parses XML file and returns XML document.
	 * @param fileName XML file to parse
	 * @return XML document or <B>null</B> if error occured
	 */
	public static Document parseInputStream(InputStream inputStream) 
	{
		if(logger.isInfoEnabled())
			logger.info("parseInputStream :: Parsxing XML File as InputStream Starts.");
		DocumentBuilder docBuilder;
		Document doc = null;
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setIgnoringElementContentWhitespace(true);
		try 
		{
			docBuilder = docBuilderFactory.newDocumentBuilder();
		}
		catch (ParserConfigurationException e) 
		{
			if(logger.isInfoEnabled())
				logger.info("parseInputStream :: ParserConfigurationException :: Wrong parser configuration :: >" + e.getMessage());
			Utilities.printStackTraceToLogs(ParseXMLDoc.class.getName(), "parseInputStream()", e);
			return null;
		}
		try 
		{
			doc = docBuilder.parse(inputStream);
		}
		catch (SAXException e) 
		{
			if(logger.isInfoEnabled())
				logger.info("parseInputStream :: SAXException :: Wrong XML file structure: >" + e.getMessage());
			Utilities.printStackTraceToLogs(ParseXMLDoc.class.getName(), "parseInputStream()", e);
			return null;
		}
		catch (IOException e) 
		{
			if(logger.isInfoEnabled())
				logger.info("parseInputStream :: IOException :: Could not read source file : >" + e.getMessage());
			Utilities.printStackTraceToLogs(ParseXMLDoc.class.getName(), "parseInputStream()", e);
			return null;
		}
		return doc;
	}
	
	/**
	 * Method will be used for getting
	 * element value
	 * 
	 * @param elem
	 * @return
	 */
	public final static String getElementValue( Node elem ) 
	{
		Node kid;
		if( elem != null)
		{
			if (elem.hasChildNodes())
			{
				for( kid = elem.getFirstChild(); kid != null; kid = kid.getNextSibling() )
				{
					if( kid.getNodeType() == Node.TEXT_NODE  )
					{
						return kid.getNodeValue();
					}
				}
			}
		}
		return "";
	}
}
