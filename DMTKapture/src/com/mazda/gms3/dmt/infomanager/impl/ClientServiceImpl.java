package com.mazda.gms3.dmt.infomanager.impl;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.inquira.client.serviceclient.IQServiceClient;
import com.inquira.client.serviceclient.IQServiceClientManager;
import com.inquira.client.serviceclient.request.IQRepositoryRequest;
import com.inquira.im.ito.RepositoryKeyITO;
import com.inquira.util.ewr.ErrorRecord;
import com.inquira.util.ewr.ErrorWarningResponse;
import com.mazda.gms3.dmt.utils.ApplicationProperties;

/**
 * @author darora
 * 
 */
public class ClientServiceImpl {
	
	static Logger log = Logger.getLogger(ClientServiceImpl.class);

	public String getRepositoryByReferenceKey() {
		IQServiceClient client = null;
		String result = null;
		boolean throwExceptionOnError = false;
//		ApplicationPropertiesUtil
//		.getProperty(CommonConstants.INFOQUIRA_DOMAIN_PROPERTY)
		try {
			client = IQServiceClientManager
					.connect(
							ApplicationProperties.getProperty("USERNAME"),
							ApplicationProperties.getProperty("PASSWORD"),
							"",
							ApplicationProperties
									.getProperty("REPOSITORY"),
									ApplicationProperties
									.getProperty("IM_CLIENT_LIBRARY_ENDPOINT"),
									ApplicationProperties
									.getProperty("SEARCH_CLIENT_LIBRARY_ENDPOINT"),
							throwExceptionOnError);
			ErrorWarningResponse ewr = client.getEWR();
			// if the IQService Client object is valid we will store it in the
			// session
			if (ewr != null && !ewr.hasErrorsOrWarnings()) {
				if (client != null) 
				{
					// result = "client connection was successfull!!\n\n";
					log.info("Get Repository By Reference Key :: Client Connection was Successfull.");
				} 
//				else
//				{
//					// result = "client connection failed!!! no EWR found \n\n";
//					log.error("Get Repository By Reference Key :: Client Connection Failed.");
//				}
			} else {
				// an exception happened
				List<ErrorRecord> errors = ewr.getErrors();
				StringBuffer sb = new StringBuffer();
				for (Iterator<ErrorRecord> iter = errors.iterator(); iter.hasNext();) {
					ErrorRecord rec = (ErrorRecord) iter.next();
					sb.append(rec);
				}
				result = sb.toString();
			}

			IQRepositoryRequest req = client.getRepositoryRequest();
			RepositoryKeyITO repository = req
					.getRepositoryKeyByReferenceKey(ApplicationProperties
							.getProperty("REPOSITORY"));
			ewr = req.getEWR();
			if (ewr != null && !ewr.hasErrorsOrWarnings()) {
				if (client != null) {
					result = repository.toXml();
				} 
//				else {
//					// result = "client connection failed!!! no EWR found \n\n";
//				}
			} else {
				// an exception happened
				List<ErrorRecord> errors = ewr.getErrors();
				StringBuffer sb = new StringBuffer();
				sb.append("EWR=");
				for (Iterator<ErrorRecord> iter = errors.iterator(); iter.hasNext();) {
					ErrorRecord rec = (ErrorRecord) iter.next();
					sb.append(rec);
				}
				result = sb.toString();
			}
		} catch (Exception e) {
			log.error("Get Repository By Reference Key :: Exception :: >" + e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

}
