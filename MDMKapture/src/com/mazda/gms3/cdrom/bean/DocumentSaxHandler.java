package com.mazda.gms3.cdrom.bean;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DocumentSaxHandler extends DefaultHandler {

	ServiceManualDocment parsedDocument = null;

	String content = null;

	@Override
	// Triggered when the start of tag is found.
	public void startElement(String uri, String localName,

	String qName, Attributes attributes)

	throws SAXException {

		if ("SERVICE_MANUALS".equalsIgnoreCase(qName)) {

			parsedDocument = new ServiceManualDocment();
		}

	}

	@Override
	public void endElement(String uri, String localName,

	String qName) throws SAXException {

		if ("TITLE".equalsIgnoreCase(qName)) {
			parsedDocument.setTitle(content);
		} else if ("CONTENT".equalsIgnoreCase(qName)) {
			parsedDocument.setContent(content);
		}

	}

	@Override
	public void characters(char[] ch, int start, int length)

	throws SAXException {

		content = String.copyValueOf(ch, start, length).trim();

	}

}
