package com.mazda.gms3.mdm.utils;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.json.generators.JSONGenerator;
import com.json.generators.JsonGeneratorFactory;
import com.mazda.gms3.mdm.logging.LogManager;
import com.mazda.gms3.mdm.logging.Logger;

public class GenerateFinalResponse {

	private static Logger logger = LogManager
			.getLogger(GenerateFinalResponse.class);

	/**
	 * Function will generate a JSON String in the following format -
	 * 
	 * { token:"DATA", responseString:"<generatedHTML> }
	 * 
	 * and will set in http response
	 * 
	 * @param response
	 * @param tokenValue
	 * @param responseStringData
	 */
	public static void generateFinalResponse(HttpServletResponse response,
			String tokenValue, String responseStringData) {
		logger.info("generateFinalResponse() ::  Method Starts.");
		try {
			logger.info("generateFinalResponse() ::  HTML Generated, setting in Response.");
			/*
			 * before settings data in response generate a JSON String in the
			 * following format -
			 * 
			 * { token:"DATA", responseString:"<generatedHTML> }
			 * 
			 * Create a Map Object and add Keys Token & responseString
			 */
			Map<String, Object> dataMap = new HashMap<String, Object>();
			// add token , value will be data
			dataMap.put("TOKEN", tokenValue);
			// add responseString, value will be responseStringData
			dataMap.put("RESPONSESTRING", responseStringData);

			// create JSON Generator Factory Object
			JsonGeneratorFactory generatorFactory = JsonGeneratorFactory
					.getInstance();
			// create JSON Generator Object
			JSONGenerator jsonGenerator = generatorFactory.newJsonGenerator();
			// Generate JSON for final response.
			String finalResponseString = jsonGenerator.generateJson(dataMap);
			// set generatorFactory,jsonGenerator to null
			generatorFactory = null;
			jsonGenerator = null;
			// set dataMap to null
			dataMap = null;
			/*
			 * Remove extra added tokens [] from the generated JSON String
			 */
			finalResponseString = finalResponseString.substring(1,
					finalResponseString.length() - 1);
			/*
			 * Now the generated JSON is in following structure -
			 * 
			 * { "RESPONSESTRING":"[[" - ","<a href=\
			 * "javascript:void(0);\" class=\"userDetailDialog\">supeer.admin@triptini.com</a>"
			 * ,"Operator",
			 * "<span class=\"label label-sm label-success statusConfirm\">Active</span>"
			 * ," - "]]", "TOKEN":"DATA" } Here Replace the "" before the value
			 * of RESPONSESTRING
			 */

			finalResponseString = finalResponseString.replace("\"[", "[");
			finalResponseString = finalResponseString.replace("]\"", "]");

			finalResponseString = finalResponseString.replace("\"{", "{");
			finalResponseString = finalResponseString.replace("}\"", "}");

			finalResponseString = finalResponseString.replace("NULL",
					"\"null\"");

			// System.out.println(" ----------------------------finalResponseString:: > "
			// + finalResponseString);

			logger.info("generateFinalResponse() ::  Setting final Generated JSON in response.");
			response.setContentType("text/html");
			response.getWriter().write(finalResponseString);
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("generateFinalResponse() ::  Exception :: >"
					+ e.getMessage());
		}
		logger.info("generateFinalResponse() ::  Method Ends.");
	}

}
