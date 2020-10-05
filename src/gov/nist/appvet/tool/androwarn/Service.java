/* This software was developed by employees of the National Institute of
 * Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 15 United States Code Section 105, works of NIST
 * employees are not subject to copyright protection in the United States
 * and are considered to be in the public domain.  As a result, a formal
 * license is not needed to use the software.
 * 
 * This software is provided by NIST as a service and is expressly
 * provided "AS IS".  NIST MAKES NO WARRANTY OF ANY KIND, EXPRESS, IMPLIED
 * OR STATUTORY, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT
 * AND DATA ACCURACY.  NIST does not warrant or make any representations
 * regarding the use of the software or the results thereof including, but
 * not limited to, the correctness, accuracy, reliability or usefulness of
 * the software.
 * 
 * Permission to use this software is contingent upon your acceptance
 * of the terms of this agreement.
 */
package gov.nist.appvet.tool.androwarn;

import gov.nist.appvet.tool.androwarn.util.FileUtil;
import gov.nist.appvet.tool.androwarn.util.HttpUtil;
import gov.nist.appvet.tool.androwarn.util.Logger;
import gov.nist.appvet.tool.androwarn.util.Native;
import gov.nist.appvet.tool.androwarn.util.SSLWrapper;
import net.minidev.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.jayway.jsonpath.JsonPath;


public class Service extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger log = Properties.log;
	private static String appDirPath = null;
	private String jsonFileReportPath = null;
//	private String appIconFilePath = null;
//	private String appId = null;
//	private static String python3Cmd = null;
	private static String wkhtmltopdfCmd = null;
	
	public static HashMap<String, ArrayList<AppMetadata>> metadataHashMap = new HashMap<String, ArrayList<AppMetadata>>();
	public static HashMap<String, FindingsCategory> findingsHashMap = new HashMap<String, FindingsCategory>();
	public Native cmd = null;


	public Service() {
		super();
		String toolOS = System.getProperty("os.name");
		if (toolOS.toUpperCase().indexOf("WIN") > -1) {
			//python3Cmd = "python3";
			wkhtmltopdfCmd = "wkhtmltopdf";

		} else if (toolOS.toUpperCase().indexOf("NUX") > -1) {
			//python3Cmd = "python3";
			wkhtmltopdfCmd = Properties.htmlToPdfCommand;
		}
		
		this.cmd = new Native(Properties.log);

	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		// Get received HTTP parameters and file upload
		String appFilePath = null;
		String appIconFilePath = null;
		String htmlFileReportPath = null;
		String pdfFileReportPath = null;
		String fileName = null;
		String appId = null;
		String appName = null;
		String appOs = null;
		String appPackage = null;
		String appVersion = null;
		String appSha256 = null;
		String appStoreUrl = null;

		// Get received HTTP parameters and file upload
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		List<FileItem> items = null;
		FileItem iconFileItem = null;
		FileItem appFileItem = null;
		HashMap<String, Value> appMetadataHashMap = new HashMap<String, Value>();

		try {
			items = upload.parseRequest(request);

			/* Get received items */
			Iterator<FileItem> iter = items.iterator();
			FileItem item = null;

			while (iter.hasNext()) {
				item = (FileItem) iter.next();
				//log.debug("Got item: " + item.getName());
				if (item.isFormField()) {
					// Get HTML form parameters
					String incomingParameter = item.getFieldName();
					String incomingValue = item.getString();

					log.debug("Received " + incomingParameter + " = " + incomingValue);
					if (incomingParameter.equals("appid")) {
						appId = incomingValue;
						appMetadataHashMap.put("appid", new Value(incomingValue, Value.DataType.STRING, Value.InfoType.METADATA));
					} else if (incomingParameter.equals("appname")) {
						// Note incoming value from AppVet will have '%20' for spaces!
						appName = incomingValue;
						appMetadataHashMap.put("application_name", new Value(incomingValue, Value.DataType.STRING, Value.InfoType.METADATA));
					} else if (incomingParameter.equals("appos")) {
						appOs = incomingValue;
						appMetadataHashMap.put("appos", new Value(incomingValue, Value.DataType.STRING, Value.InfoType.METADATA));
					} else if (incomingParameter.equals("apppackage")) {
						appPackage = incomingValue;
						appMetadataHashMap.put("package_name", new Value(incomingValue, Value.DataType.STRING, Value.InfoType.METADATA));
					} else if (incomingParameter.equals("appversion")) {
						appVersion = incomingValue;
						appMetadataHashMap.put("application_version", new Value(incomingValue, Value.DataType.STRING, Value.InfoType.METADATA));
					} else if (incomingParameter.equals("appsha")) {
						appSha256 = incomingValue;
						appMetadataHashMap.put("fingerprint", new Value(incomingValue, Value.DataType.STRING, Value.InfoType.METADATA));
					} else if (incomingParameter.equals("appstoreurl")) {
						appStoreUrl = incomingValue;
						appMetadataHashMap.put("appstoreurl", new Value(incomingValue, Value.DataType.STRING, Value.InfoType.METADATA));
					} 					
				} else {
					// item is a file
					if (item != null) {
						log.debug("Received file: " + item.getName());

						if (item.getName().endsWith(".apk") || item.getName().endsWith(".ipa")) {
							appFileItem = item;
							appMetadataHashMap.put("file_name", new Value(appFileItem.getName(), Value.DataType.STRING, Value.InfoType.METADATA));
							//log.debug("Received app file: " + appFileItem.getName());
						} else if (item.getName().endsWith(".png")) {
							iconFileItem = item;
							//log.debug("Received icon file: " + iconFileItem.getName());
						} else {
							log.warn("Ignoring received file " + item.getName());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (appId == null) {
			// All tool services require an AppVet app ID
			HttpUtil.sendHttp400(response, "No app ID specified");
			return;
		}

		// Create app directory
		appDirPath = Properties.TEMP_DIR + "/" + appId;
		
		File appDir = new File(appDirPath);
		if (!appDir.exists()) {
			appDir.mkdir();
		}
		
		if (appFileItem != null) {

			// Get app file
			fileName = FileUtil.getFileName(appFileItem.getName());
			
			appFilePath = appDirPath + "/" + fileName;
			//log.debug("APP FILE PATH: " + appFilePath);
			
			// Must be an APK file
			if (!fileName.endsWith(".apk")) {
				HttpUtil.sendHttp400(response,
						"Invalid app file: " + appFileItem.getName());
				return;
			}
	
			if (!FileUtil.saveFileUpload(appFileItem, appFilePath)) {
				HttpUtil.sendHttp500(response, "Could not save app");
				return;
			}
		}
			
		if (iconFileItem != null) {
			
			appIconFilePath = appDirPath + "/icon.png";
			//log.debug("APP ICON PATH: " + appIconFilePath);
			
			if (!FileUtil.saveFileUpload(iconFileItem, appIconFilePath)) {
				HttpUtil.sendHttp500(response, "Could not save icon");
				return;
			}
			
		}
		
		jsonFileReportPath = appDirPath + "/report.json";
		//log.debug("JSON REPORT PATH: " + jsonFileReportPath);
		
		htmlFileReportPath = appDirPath + "/report.html";
		//log.debug("HTML REPORT PATH: " + htmlFileReportPath);

		pdfFileReportPath = appDirPath + "/report.pdf";
		//log.debug("PDF REPORT PATH: " + pdfFileReportPath);

		// Send acknowledgement back to AppVet
		HttpUtil.sendHttp202(response, "Received app " + appId
				+ " for processing.");
		
		// Load known vulnerabilities
		//log.debug("Loading CVSS data");
		loadCvssData();

		// Start processing app
		//log.debug("Executing Androwarn on app");

		// Androwarn command without args (i.e., sudo python3 androwarn.py)
		String newCmd = Properties.androwarnCmd 
				+ " -i " + appFilePath 
				+ " -o " + jsonFileReportPath 
				+ " -v 3 " 
				+ " -r json "
				+ " -d "
				+ " -w ";
		
		String[] fullCmd = newCmd.split("\\s+");
		ProcessBuilder pb = new ProcessBuilder(fullCmd);

		// Run Androwarn and generate JSON report
		int waitMinutes = 60; // wait in minutes
		StringBuffer result = new StringBuffer();
		int exitValue = cmd.exec(pb, waitMinutes, result);

		if (exitValue != 0) {
			// All tool services require an AppVet app ID
			sendErrorReport(appId, appName, appVersion, 
					appPackage, fileName, metadataHashMap, result.toString(), appIconFilePath, htmlFileReportPath, pdfFileReportPath);
			return;
		}
		
		// Change permissions on the file
		pb = new ProcessBuilder(
				"/usr/bin/sudo",
				"/usr/bin/chown",
				"root:root",
				jsonFileReportPath
				);
		
		result = new StringBuffer();
		exitValue = cmd.exec(pb, waitMinutes, result);

		if (exitValue != 0) {
			// All tool services require an AppVet app ID
			sendErrorReport(appId, appName, appVersion, 
					appPackage, fileName, metadataHashMap, result.toString(), appIconFilePath, htmlFileReportPath, pdfFileReportPath);
			return;
		}
		
		// Read JSON result file into string
		String json = readJsonFile(jsonFileReportPath);

		if (json == null) {
			// All tool services require an AppVet app ID
			HttpUtil.sendHttp500(response, "Androwarn could not generate JSON for this app");
			return;
		}

		// Get metadata from results
		getMetadata(json);
		
		// Scan for issue in JSON string (no need to parse JSON)
		double avgCvss = analyzeResults(json, findingsHashMap);
		
		// Create HTML report
		String htmlReport = 
				Report.createHtml(avgCvss, 
						metadataHashMap, 
						findingsHashMap, 
						appId, 
						appIconFilePath,
						appName,
						null,
						null,
						null,
						null);
		if (!FileUtil.saveReport(htmlReport, htmlFileReportPath)) {
			HttpUtil.sendHttp500(response, "Androwarn could not save HTML report");
			return;
		}
		
		// Generate PDF file from HTML report
		pb = new ProcessBuilder(
				wkhtmltopdfCmd,
				htmlFileReportPath,
				pdfFileReportPath
				);
		result = new StringBuffer();
		exitValue = cmd.exec(pb, new Integer(waitMinutes).intValue(), result);

		if (exitValue != 0) {
			// All tool services require an AppVet app ID
			//log.debug(result.toString());
			sendErrorReport(appId, appName, appVersion, 
					appPackage, fileName, metadataHashMap, result.toString(), appIconFilePath, htmlFileReportPath, pdfFileReportPath);
			return;
		}
		
		// Send to AppVet
		sendInNewHttpRequest(appId, pdfFileReportPath, avgCvss);
		
		// Clear structures
		//log.debug("Clearing hashmaps");
		metadataHashMap.clear();
		findingsHashMap.clear();
		
		// Clean up
		if (!Properties.keepApps) {
			//log.debug("Removing tmp files");
			try {
				//log.debug("Removing app " + appId + " files.");
				FileUtils.deleteDirectory(new File(appDirPath));
			} catch (IOException ioe) {
				log.error(ioe.getMessage());
			}
		}

		// Clean up
		System.gc();
		log.debug("End processing");

	}
	
	public void sendErrorReport(String appId, String name, String version, String appPackage, 
			String fileName, HashMap<String, ArrayList<AppMetadata>> metadataHashMap, 
			String errorMessage, String appIconFilePath, String htmlFileReportPath, String pdfFileReportPath) {
		
		//log.debug("Writing error message: " + errorMessage);
		
		String htmlReport = 
				Report.createHtml(-1.0, 
						metadataHashMap, 
						null, 
						appId, 
						appIconFilePath,
						name,
						version,
						appPackage,
						fileName,
						errorMessage);
		
		//log.debug("Saving report");

		if (!FileUtil.saveReport(htmlReport, htmlFileReportPath)) {
			log.error("Could not generate HTML report.");
			return;
		}
		
		//log.debug("Generating PDF");

		// Generate PDF file from HTML report
		ProcessBuilder pb = new ProcessBuilder(
				wkhtmltopdfCmd,
				htmlFileReportPath,
				pdfFileReportPath
				);
		int waitMinutes = 20; // 30 minutes
		StringBuffer result = new StringBuffer();
		int exitValue = cmd.exec(pb, waitMinutes, result);
		
		if (exitValue != 0) {
			
			// All tool services require an AppVet app ID
			//log.debug(result.toString());

			log.error("Issues generating PDF");
			//return;
		}
		
		log.debug("Sending report to AppVet");

		// Send to AppVet
		sendInNewHttpRequest(appId, pdfFileReportPath, -1.0);
	}
	
	private static void loadCvssData() {
		// Read in CVSS data file
		String filePath = Properties.CONF_DIR + "/androwarn_cvss_3.1_scores.txt";
		File file = new File(filePath); 
		if (!file.exists()) {
			log.error("Could not find CVSS properties file: " + filePath);
		}
		
		try {

			BufferedReader br = new BufferedReader(new FileReader(file)); 

			String str; 
			while ((str = br.readLine()) != null) {
				//System.out.println("line: " + str); 
				if (str != null && !str.startsWith("#") && !str.trim().isEmpty()) {
					String[] issueData = str.split(";");
					//log.debug("\nCATEGORY: " + issueData[0]);
					if (issueData[0] == null)
						log.error("Read null category from cvss file");
					//log.debug("\nISSUE: " + issueData[1]);
					if (issueData[1] == null)
						log.error("Read null issue from cvss file");
					//log.debug("\nVECTOR: " + issueData[2]);
					if (issueData[2] == null)
						log.error("Read null vector from cvss file");
					//log.debug("\nCVSS: " + issueData[3]);
					if (issueData[0] == null)
						log.error("Read null base score from cvss file");
					
					// Check if already in hash map
					FindingsCategory findingsCategory = findingsHashMap.get(issueData[0]);
					if (findingsCategory == null) {
						//log.debug("Adding new category: " + issueData[0].trim());
						// MAKE SURE TO TRIM DATA BEFORE USING!
						findingsCategory = new FindingsCategory(
								AnalysisCategory.getEnum(issueData[0].trim()));
						DefinedIssue issue = new DefinedIssue(issueData[1], 
								issueData[2], 
								new Double(issueData[3]));
						findingsCategory.definedIssues.add(issue);
						
						findingsHashMap.put(issueData[0].trim(), findingsCategory);
						
					} else {
						//log.debug("Updating category: " + issueData[0].trim());
						DefinedIssue issue = new DefinedIssue(issueData[1], 
								issueData[2], 
								new Double(issueData[3]));
						findingsCategory.definedIssues.add(issue);
						
					}
				}
			}

			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	
	// Get app metadata - does not get 'analysis_results' here
	public void getMetadata(String json) {

		//log.debug("Getting metadata");
		//		Integer arrayLength = JsonPath.read(json, "$.length()");
		//		log.debug("JSON array length: " + arrayLength);

		// Get Findings
		JSONArray jsonArray = JsonPath.read(json, "$");
		//log.debug("jsonArray.size(): " + jsonArray.size());

		for (int i = 0; i < jsonArray.size(); i++) {

			LinkedHashMap<String, Object> hashMapObj = 
					(LinkedHashMap<String, Object>) jsonArray.get(i);

			for (Map.Entry<String,Object> entry : hashMapObj.entrySet()) { 

				String key = entry.getKey();
				Object value = entry.getValue();

				//log.debug("MAIN KEY: " + key); 

				switch (key) {
				case "application_information": 
					//log.debug("Got application_information");
					//log.debug("entry here");

					getAnalysisInfo("application_information", (JSONArray) value, metadataHashMap);
					break;

				case "apk_file": 
					//log.debug("Got apk_file");
					getAnalysisInfo("apk_file", (JSONArray) value, metadataHashMap);
					break;	

				default: 
					log.error("Got unsupported key: " + key);
					break;

				}
			}
		} 
	}

	public void getAnalysisInfo(String category, JSONArray categoryArray, HashMap<String, ArrayList<AppMetadata>> metadataHashMap) {

		for (int i = 0; i < categoryArray.size(); i++) {

			JSONArray appInfoArray = (JSONArray) categoryArray.get(i);

			//log.debug("appInfoArray.size(): " + appInfoArray.size());

			String key = (String) appInfoArray.get(0);
			//log.debug("MAIN KEY GET ANALYSIS: " + key);

			JSONArray dataArray = (JSONArray) appInfoArray.get(1);
			//log.debug("dataArray.size(): " + dataArray.size());
			
			if (dataArray.size() == 0) {
				log.error("Data array is empty for: " + key);
			}
			
			ArrayList<AppMetadata> arrayList = new ArrayList<AppMetadata>();
			
			for (int j = 0; j < dataArray.size(); j++) {
				//log.debug("GET ANALYSIS INFO VALUE: " + dataArray.get(j));
				AppMetadata f = new AppMetadata (category, (String) dataArray.get(j));
				arrayList.add(f);
			}

			switch (key) {

			// App Information
			case "application_name": 
				//log.debug("Got application_name");
				metadataHashMap.put("application_name", arrayList);
				break;

			case "application_version": 
				//log.debug("Got application_version");
				metadataHashMap.put("application_version", arrayList);
				break;

			case "package_name": 
				//log.debug("Got package_name");
				metadataHashMap.put("package_name", arrayList);
				break;

			case "description": 
				//log.debug("Got description");
				metadataHashMap.put("description", arrayList);
				break;
				

			// APK File
			case "file_name": 
				//log.debug("Got file_name");
				metadataHashMap.put("file_name", arrayList);
				break;

			case "fingerprint": 
				//log.debug("Got fingerprint");
				metadataHashMap.put("fingerprint", arrayList);
				break;

			case "certificate_information": 
				//log.debug("Got certificate_information");
				metadataHashMap.put("certificate_information", arrayList);
				break;

			default: 
				log.error("Got unsupported key: " + key);
				break;

			}
		} 
	}

	public static double analyzeResults(String json, HashMap <String, FindingsCategory> findingsHashMap) {
		
		//log.debug("Analyzing results");
		int foundCount = 0;
		double totalCvss = 0.0;
		
		//log.debug("JSON: " + json);
		
		for (Map.Entry<String, FindingsCategory> categories : findingsHashMap.entrySet()) { 
			
			String catagory = (String) categories.getKey(); 
			
			FindingsCategory findingsCategory = findingsHashMap.get(catagory);
			
			//log.debug("Catagory matching: " + catagory);
			
			for (int i = 0; i < findingsCategory.definedIssues.size(); i++) {
				
				DefinedIssue definedIssue = findingsCategory.definedIssues.get(i);
				//log.debug("Looking for match: " + definedIssue.issueName + "|");
				
				// MUST USE TRIM
				int startIndex = json.indexOf(definedIssue.issueName.trim());
				//log.debug("startIndex: " + startIndex);
				
				if (startIndex > -1) {
					//log.debug("Match!");
					int endIndex = json.indexOf("\"", startIndex);
					if (startIndex > -1 && endIndex > -1 && endIndex <= json.length()) {
						String match = json.substring(startIndex, endIndex);
						// Check if this already exists
						boolean alreadyExists = checkExists(definedIssue.detectedIssues, match);
						if (!alreadyExists) {
							//log.error("ADDING ACTUAL: " + match);
							definedIssue.detectedIssues.add(match);
							foundCount++;
							totalCvss += definedIssue.cvssBaseScore;
						} else {
							//log.warn("Match already exists in detectedIssues list");
						}
					}
				} 
			}
		}
		
		log.debug("Total CVSS found: " + foundCount);
		log.debug("Total CVSS: " + totalCvss);
		double avgCvss = (double) totalCvss / foundCount;
		log.debug("Average: " + avgCvss);
		return avgCvss;
	}
	
	public static boolean checkExists(List<String> list, String value) {
		for (int i = 0; i < list.size(); i++) {
			String str = list.get(i);
			if (str.equals(value)) {
				return true;
			}
		}
		return false;
	}

	/** This method should be used for sending files back to AppVet. */
	public static boolean sendInNewHttpRequest(String appId,
			String reportFilePath,
			double toolScore) {
		
		//log.debug("Sending report to AppVet");
		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 30000);
		HttpConnectionParams.setSoTimeout(httpParameters, 1200000);
		HttpClient httpClient = new DefaultHttpClient(httpParameters);
		httpClient = SSLWrapper.wrapClient(httpClient);

		try {
			/*
			 * To send reports back to AppVet, the following parameters must be
			 * sent: - command: SUBMIT_REPORT - username: AppVet username -
			 * password: AppVet password - appid: The app ID - toolid: The ID of
			 * this tool - toolrisk: The risk assessment
			 * (LOW, MODERATE, HIGH, ERROR) - report: The report file.
			 */
			MultipartEntity entity = new MultipartEntity();
			entity.addPart("command",
					new StringBody("SUBMIT_REPORT", Charset.forName("UTF-8")));
			entity.addPart("appid",
					new StringBody(appId, Charset.forName("UTF-8")));
			entity.addPart("toolid",
					new StringBody(Properties.toolId, Charset.forName("UTF-8")));
			entity.addPart("toolscore", new StringBody(toolScore + "",
					Charset.forName("UTF-8")));
//			entity.addPart("toolrisk", new StringBody(reportStatus.name(),
//					Charset.forName("UTF-8")));
			File report = new File(reportFilePath);
			FileBody fileBody = new FileBody(report);
			entity.addPart("file", fileBody);
			HttpPost httpPost = new HttpPost(Properties.appvetUrl);
			String credentials = Base64.getEncoder().encodeToString((Properties.appvetUsername + ":" + Properties.appvetPassword).getBytes());
			httpPost.setHeader("Authorization", "Basic " + credentials);

			httpPost.setEntity(entity);
			// Send the report to AppVet
			log.debug("Sending report file to AppVet");
			final HttpResponse response = httpClient.execute(httpPost);
			log.debug("Received from AppVet: " + response.getStatusLine());
			// Clean up
			httpPost = null;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
			return false;
			
		}
	}

	public static String readJsonFile(String jsonFilePath) {
		//log.debug("Reading: " + jsonFilePath);

		try {
			//log.debug("Getting FileReader");

			FileReader fr = new FileReader(jsonFilePath);
			//log.debug("Getting BufferedReader");

			BufferedReader reader = new BufferedReader(fr);

			String line = null;
			//log.debug("Setting StringBuffer");
			StringBuffer buf = new StringBuffer();
			//log.debug("Reading json line");
			while ((line = reader.readLine()) != null) {
				buf.append(line);
				//log.debug(line);
			}

			reader.close();
			fr.close();

			return buf.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
