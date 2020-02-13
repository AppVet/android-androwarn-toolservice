package gov.nist.appvet.tool.androwarn;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import gov.nist.appvet.tool.androwarn.util.Logger;

public class Report {

	private static final Logger log = Properties.log;

	private Report() {}


	public static String createHtml(double avgCvss, HashMap<String, ArrayList<AppMetadata>> metadataHashMap,
			HashMap<String, FindingsCategory> findingsHashMap, 
			String appId, String appFileIconPath, String errorMessage) {
		
		log.debug("Creating HTML file");

		StringBuffer htmlBuffer = new StringBuffer();


		String appPath = Properties.ANDROID_ANDROWARN_FILES_HOME + "/apps/" + appId;
		String imagesPath = Properties.ANDROID_ANDROWARN_FILES_HOME + "/images";

		String dhsLogoPath = null;
		String appvetLogoPath = null;
		String androwarnLogoPath = null;
		String toolOS = System.getProperty("os.name");
		if (toolOS.toUpperCase().indexOf("WIN") > -1) {
			dhsLogoPath = imagesPath + "\\dhs.jpg";
			appvetLogoPath = imagesPath + "\\appvet.png";
			androwarnLogoPath = imagesPath + "\\androwarn.png";
		} else if (toolOS.toUpperCase().indexOf("NUX") > -1) {
			dhsLogoPath = imagesPath + "/dhs.jpg";
			appvetLogoPath = imagesPath + "/appvet.png";
			androwarnLogoPath = imagesPath + "/androwarn.png";
		}

		log.debug("Creating HTML header");

		//StringBuffer htmlBuffer = new StringBuffer();
		htmlBuffer.append("<HTML>\n");
		htmlBuffer.append("<head>\n");
		htmlBuffer.append("<style type=\"text/css\">\n");

		htmlBuffer.append("h1 {font-family:arial;font-size:30px;font-weight:bold;}\n");
		htmlBuffer.append("h2 {font-family:arial;font-size:20px;font-weight:bold;}\n");	
		htmlBuffer.append("h3 {font-family:arial;color: gray;}\n");
		htmlBuffer.append("h4 {font-family:arial;}\n");
		htmlBuffer.append("p {font-family:arial;}\n");
		htmlBuffer.append(".itemName {font-family:arial;font-size: 16px;font-weight:bold; width:230px;}\n");
		htmlBuffer.append(".riskScore {font-family:arial;font-size: 16px;font-weight:bold; width:50px;}\n");
		htmlBuffer.append(".itemValue {font-family:arial;font-size: 16px;width:520px;}\n");

		htmlBuffer.append(".tableIcon {margin:40px;}\n");
		htmlBuffer.append(".tableMain {margin:40px;}\n");
		htmlBuffer.append(".notice {margin:40px;font-family:arial;font-size:16px;}\n");

		htmlBuffer.append(".tableOverallResult {margin:20px; border-collapse: collapse;border:1px solid white;width: 100%;}\n");

		htmlBuffer.append("th {color: blue; border-collapse: collapse;text-align:left; font-family:arial;font-size: 14px;font-weight:bold;}\n");
		htmlBuffer.append(".tableItemName {padding-top: 7px; padding-right: 5px; padding-bottom: 7px; align:left; font-family:arial;font-size: 14px;}\n");
		htmlBuffer.append(".tableItemValue {text-align: center; padding-left: 20px; padding-top: 7px; padding-right: 5px; padding-bottom: 7px; align:left; font-family:arial;font-size: 14px;}\n");
		htmlBuffer.append(".tableItemDescription {padding-left: 5px; padding-top: 7px; padding-right: 5px; padding-bottom: 7px; align:left; font-family:arial;font-size: 14px;}\n");

		htmlBuffer.append("</style>\n");

		//--------------------------- Page Title -----------------------------
		log.debug("Creating HTML title and banner");

		htmlBuffer.append("<title> Androwarn Report for " + appId + "</title>\n");
		htmlBuffer.append("</head>\n");
		htmlBuffer.append("<body style=\"margin:20px;padding:0\">\n");

		//----------------------------- Banner --------------------------------
		htmlBuffer.append("<table style=\"width: 100%; background:white;padding:0px;margin:0px;\">\n");
		htmlBuffer.append("<tr>");

		htmlBuffer.append("<td style=\"width:50%;padding:0px;margin:0px;\" align=\"left\"><img src=\"" + dhsLogoPath + "\" alt=\"DHS logo\" style=\"height: 35px;\"></td>");
		htmlBuffer.append("<td style=\"width:50%;padding:0px;margin:0px;\" align=\"right\"><img src=\"" + appvetLogoPath + "\" alt=\"AppVet logo\" style=\"height: 35px;\"></td>");

		htmlBuffer.append("</tr>");
		htmlBuffer.append("</td>\n");
		htmlBuffer.append("</table>\n");
		htmlBuffer.append("<br>\n");

		String androwarnLogo = imagesPath + "/androwarn-icon.png";


		htmlBuffer.append("<center><br><br><br>");			
		htmlBuffer.append("<img src=\"" + androwarnLogo + "\" alt=\"Androwarn logo\" title=\"Androwarn logo\" style=\"height: 60px;\">");
		htmlBuffer.append("<br>");			

		htmlBuffer.append("<div style=\"margin-top: 10px; font-family:arial; font-size: 30px; font-weight: 100; color: #595959;\">Androwarn Report</div>");
		htmlBuffer.append("<div style=\"text-decoration: none; font-family:arial; font-size: 16px; font-weight: 50; color: #a6a6a6;\">https://github.com/maaaaz/androwarn</div>");

		
		htmlBuffer.append("</center>");

		htmlBuffer.append("<br>\n");
		htmlBuffer.append("<br><br>\n");

		//------------------- Appname, version and logo ----------------------
		log.debug("Creating app name, version and logo");

		htmlBuffer.append("<table class=\"tableIcon\">\n");

		htmlBuffer.append("<tr>");
		htmlBuffer.append("<td rowspan = \"2\"> <img src=\"" + appFileIconPath + "\" height=\"80px\" /> </td>");
		htmlBuffer.append("<td style=\"font-family:arial;font-size:36px;padding-left:20px;\">" 
				+ getAllValues("application_name", metadataHashMap) + "</td>");
		htmlBuffer.append("</tr>");

		htmlBuffer.append("<tr>");
		htmlBuffer.append("<td style=\"font-family:arial; "
				+ "font-size:24px;padding-left:20px;margin:0px;\">Version " 
				+ getAllValues("application_version", metadataHashMap) + "</td>");
		htmlBuffer.append("</tr>");

		htmlBuffer.append("</table>");

		htmlBuffer.append("<table class=\"tableIcon\">\n");

		htmlBuffer.append("<tr>\n");
		htmlBuffer.append("<td witdh=\"50px\" class=\"riskScore\" style=\"font-size: 22px; font-weight: bold; color:gray; \">Score&nbsp;&nbsp;</td>\n");


		String strDouble = String.format("%.2f", avgCvss);

		if (avgCvss == -1) {
			htmlBuffer.append("<td class=\"itemValue\" style=\"font-size: 22px; font-weight: bold; color: green;\">ERROR</td>\n");
		} else if (avgCvss >= 0 && avgCvss < 4.0) {
			htmlBuffer.append("<td class=\"itemValue\" style=\"font-size: 22px; font-weight: bold; color: green;\">" + strDouble + " (LOW) </td>\n");
		} else if (avgCvss >= 4.0 && avgCvss < 7.0) {
			htmlBuffer.append("<td class=\"itemValue\" style=\"font-size: 22px; font-weight: bold; color: orange;\">" + strDouble + " (MEDIUM) </td>\n");
		} else if (avgCvss >= 7.0 && avgCvss < 9.0) {
			htmlBuffer.append("<td class=\"itemValue\" style=\"font-size: 22px; font-weight: bold; color: red;\">" + strDouble + " (HIGH) </td>\n");
		} else if (avgCvss >= 9.0 && avgCvss <= 10.0) {
			htmlBuffer.append("<td class=\"itemValue\" style=\"font-size: 22px; font-weight: bold; color: purple;\">" + strDouble + " (CRITICAL) </td>\n");
		}
		htmlBuffer.append("<tr>\n");
		htmlBuffer.append("</table>");

		//----------------------------------- Metadata ----------------------------------------
		log.debug("Creating metadata");

		htmlBuffer.append("<table class=\"tableIcon\">\n");

		htmlBuffer.append("<tr>\n");
		htmlBuffer.append("<td class=\"itemName\">AppVet ID: </td>\n");
		htmlBuffer.append("<td class=\"itemValue\">" + appId + "</td>\n");
		htmlBuffer.append("</tr>\n");

		//log.debug("Submit time: " + appInfo.submitTime);
		final SimpleDateFormat format = new SimpleDateFormat(
				"yyyy-MM-dd' 'HH:mm:ss");
		final Date submissionDate = new Date(System.currentTimeMillis());
		final String submissionDateFormatted = format.format(submissionDate);

		htmlBuffer.append("<tr>\n");
		htmlBuffer.append("<td class=\"itemName\">Analysis Date: </td>\n");
		htmlBuffer.append("<td class=\"itemValue\">" + submissionDateFormatted + "</td>\n");
		htmlBuffer.append("</tr>\n");

		htmlBuffer.append("<tr>\n");
		htmlBuffer.append("<td class=\"itemName\">OS: </td>\n");
		htmlBuffer.append("<td class=\"itemValue\">Android</td>\n");
		htmlBuffer.append("</tr>\n");

		htmlBuffer.append("<tr>\n");
		htmlBuffer.append("<td class=\"itemName\">Package: </td>\n");
		htmlBuffer.append("<td class=\"itemValue\">" + getAllValues("package_name", metadataHashMap) + "</td>\n");
		htmlBuffer.append("</tr>\n");

		htmlBuffer.append("<tr>\n");
		htmlBuffer.append("<td class=\"itemName\">File: </td>\n");
		htmlBuffer.append("<td class=\"itemValue\">" + getAllValues("file_name", metadataHashMap) + "</td>\n");
		htmlBuffer.append("</tr>\n");

		htmlBuffer.append("<tr>\n");
		htmlBuffer.append("<td class=\"itemName\">Fingerprint: </td>\n");
		htmlBuffer.append("<td class=\"itemValue\">" + getAllValues("fingerprint", metadataHashMap) + "</td>\n");
		htmlBuffer.append("</tr>\n");

		if (errorMessage == null) {
			htmlBuffer.append("<tr>\n");
			htmlBuffer.append("<td class=\"itemName\">Certificate: </td>\n");
			htmlBuffer.append("<td class=\"itemValue\">" + getValue("certificate_information", "Common Name:", metadataHashMap) + "</td>\n");
			htmlBuffer.append("</tr>\n");
		}

		htmlBuffer.append("</tr>\n");
		htmlBuffer.append("</table>\n");

		//----------------------------------- Findings ----------------------------------------

		if (errorMessage == null) {
			log.debug("Creating findings");

			htmlBuffer.append("<h2 style=\"margin-left:40px;color:gray;\">Findings Summary</h2>");

			String currentCategory = "";

			for (Map.Entry<String, FindingsCategory> mapElement : findingsHashMap.entrySet()) { 

				String issueCategory = (String) mapElement.getKey(); 
				FindingsCategory findingsCategory = mapElement.getValue();
				//			String xx = findingsCategory.issue;
				//			String cvssVector = findingsCategory.cvssVectorStr;
				//			double cvssBaseScore = findingsCategory.cvssBaseScore;
				if (findingsCategory.getNumIssuesFound() > 0) {
					
					if (!currentCategory.equals(issueCategory)) {
						currentCategory = issueCategory;

						System.out.println("Printing category header: " + issueCategory);
						System.out.println("Num defined issues: " + findingsCategory.definedIssues.size());

						htmlBuffer.append("<h3 style=\"padding: 5px; margin-left:40px; color: white; background-color: #0099ff;\">" 
								+ AnalysisCategory.getEnum(issueCategory).formalName + "</h3>\n");
						htmlBuffer.append("<table class=\"tableMain\" width=\"100%\">\n");
						htmlBuffer.append("<tr>\n");
						htmlBuffer.append("<th width=\"40%\" >Issue</th>\n");
						htmlBuffer.append("<th style=\"padding-left: 10px;\" width=\"35%\" >CVSS Vector</th>\n");
						htmlBuffer.append("<th width=\"15%\" >Score</th>\n");
						htmlBuffer.append("</tr>\n");
					}
					
					for (int i = 0; i < findingsCategory.definedIssues.size(); i++) {

						DefinedIssue knownIssue = findingsCategory.definedIssues.get(i);
						String cvssVector = knownIssue.cvssVectorStr;
						double cvssBaseScore = knownIssue.cvssBaseScore;

						log.debug("Detected num issues: " + knownIssue.detectedIssues.size());

						if (knownIssue.detectedIssues.size() > 0) {


							for (int j = 0; j < knownIssue.detectedIssues.size(); j++) {
								String detectedIssue = knownIssue.detectedIssues.get(j);

								htmlBuffer.append("<tr>\n");
								htmlBuffer.append("<td width=\"200px\" class=\"tableItemName\">" + detectedIssue + "</td>\n");
								htmlBuffer.append("<td style=\"padding-left: 10px;\" width=\"200px\" class=\"tableItemName\">" + cvssVector + "</td>\n");

								if (cvssBaseScore >= 0.0 && cvssBaseScore < 4.0) {
									htmlBuffer.append("<td class=\"tableItemName\" "
											+ "style=\"font-weight: bold; color: green;\">" 
											+ cvssBaseScore + " (LOW) </td>\n");
								} else if (cvssBaseScore >= 4.0 && cvssBaseScore < 7.0) {
									htmlBuffer.append("<td class=\"tableItemName\" "
											+ "style=\"font-weight: bold; color: orange;\">" 
											+ cvssBaseScore + " (MEDIUM) </td>\n");
								} else if (cvssBaseScore >= 7.0 && cvssBaseScore < 9.0) {
									htmlBuffer.append("<td class=\"tableItemName\" "
											+ "style=\"font-weight: bold; color: red;\">" 
											+ cvssBaseScore + " (HIGH) </td>\n");
								} else if (cvssBaseScore >= 9.0 && cvssBaseScore <= 10.0) {
									htmlBuffer.append("<td class=\"tableItemName\" "
											+ "style=\"font-weight: bold; color: purple;\">" 
											+ cvssBaseScore + " (CRITICAL) </td>\n");
								}

								//htmlBuffer.append("<td width=\"50%\" class=\"tableItemDescription\">" + " Description " + "</td>\n");
								htmlBuffer.append("</tr>\n");
							}
						}
					}
					
					htmlBuffer.append("</table>\n");
				}
			}
		} else {
			log.debug("Creating error message");

			// Error 
			htmlBuffer.append("<div style=\"padding: 5px; margin-left:40px; \">The following error was detected:</div>\n");
			htmlBuffer.append("<br><br>\n");
			
			htmlBuffer.append("<div style=\"padding: 5px; margin-left:40px; \">" + errorMessage + "</div>\n");
			htmlBuffer.append("<br><br>\n");
			
			htmlBuffer.append("<div style=\"padding: 5px; margin-left:40px; \">The AppVet Team is aware of this issue and will "
					+ "resubmit the app if and when this issue is resolved.</div>\n");

		}

		// END
		htmlBuffer.append("<br><br><center>\n");
		htmlBuffer.append("<div style=\"font-family:arial;\">End Report</div>");
		htmlBuffer.append("</center>\n");
		htmlBuffer.append("</body>");
		htmlBuffer.append("</html>");
		log.debug("Returning HTML buffer");

		return htmlBuffer.toString();
	}


	public static String getAllValues(String key, HashMap<String, ArrayList<AppMetadata>> metadataHashMap) {
		StringBuffer buf = new StringBuffer();

		ArrayList<AppMetadata> arrayList = metadataHashMap.get(key);
		if (arrayList != null) {

			for (int i = 0; i < arrayList.size(); i++) {
				AppMetadata f = arrayList.get(i);
				String value = f.issue;
				buf.append(value);
				if (i < arrayList.size()-1) {
					buf.append("<br>");
				}
			}
		}

		return buf.toString();
	}

	public static String getValue(String key, String search, HashMap<String, ArrayList<AppMetadata>> hashMap) {
		StringBuffer buf = new StringBuffer();

		ArrayList<AppMetadata> arrayList = hashMap.get(key);
		if (arrayList != null) {

			for (int i = 0; i < arrayList.size(); i++) {
				AppMetadata f = arrayList.get(i);
				String value = f.issue;
				if (value.indexOf(search) > -1) {
					return value;
				}
			}
		}

		return buf.toString();
	}
}
