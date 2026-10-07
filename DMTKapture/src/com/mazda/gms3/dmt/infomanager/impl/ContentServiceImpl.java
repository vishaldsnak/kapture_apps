/**
 * 
 */
package com.mazda.gms3.dmt.infomanager.impl;

import org.apache.log4j.Logger;

/**
 * @author darora
 * 
 */
public class ContentServiceImpl {

	static Logger log = Logger.getLogger(ContentServiceImpl.class);

	public String deleteContent(String token, String contentId) {
		String output = null;
		try {
			/*
			 * Create Object of infoquiraServiceImpl class and call function to
			 * create Content and get the document id using web service.
			 */
			InfoquiraServiceImpl infoquiraServiceImpl = new InfoquiraServiceImpl();
			output = infoquiraServiceImpl.getContentServices().deleteContent(token, contentId);
			// set infoquiraServiceImpl to null
			infoquiraServiceImpl = null;
		} catch (Exception exception) {
			log.error("Delete Content :: Exception :: >"
					+ exception.getMessage());
			exception.printStackTrace();
		}
		return output;
	}

	
	public String createContent(String token, String content, boolean publish) {
		String output = null;
		try {
			/*
			 * Create Object of infoquiraServiceImpl class and call function to
			 * create Content and get the document id using web service.
			 */
			InfoquiraServiceImpl infoquiraServiceImpl = new InfoquiraServiceImpl();
			output = infoquiraServiceImpl.getContentServices().createContent(
					token, content, publish);
			// set infoquiraServiceImpl to null
			infoquiraServiceImpl = null;
		} catch (Exception exception) {
			log.error("Create Content :: Exception :: >"
					+ exception.getMessage());
			exception.printStackTrace();
		}
		return output;
	}

	public String updateContent(String token, String content, boolean publish) {
		String output = null;
		try {
			/*
			 * Create Object of infoquiraServiceImpl class and call function to
			 * modify Content and get the document id using web service.
			 */

			InfoquiraServiceImpl infoquiraServiceImpl = new InfoquiraServiceImpl();
			output = infoquiraServiceImpl.getContentServices().modifyContent(
					token, content, publish);
			// set infoquiraServiceImpl to null
			infoquiraServiceImpl = null;
		} catch (Exception exception) {
			log.error("Update Content :: Exception :: >"
					+ exception.getMessage());
			exception.printStackTrace();
			System.err.println("Update Content :: Exception :: >"
					+ exception.getMessage());
		}
		return output;
	}

	public String getContentRecord(String token, String inputXML) {
		String output = null;
		try {
			/*
			 * Create Object of infoquiraServiceImpl class and call function to
			 * get Document Details for the document Id
			 */
			InfoquiraServiceImpl infoquiraServiceImpl = new InfoquiraServiceImpl();
			output = infoquiraServiceImpl.getContentServices()
					.getContentRecord(token, inputXML);
			// set infoquiraServiceImpl to null
			infoquiraServiceImpl = null;
		} catch (Exception exception) {
			log.error("Get Content Record :: Exception :: >"
					+ exception.getMessage());
			exception.printStackTrace();
		}
		return output;
	}

}
