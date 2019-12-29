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
import gov.nist.appvet.tool.androwarn.util.Protocol;
import gov.nist.appvet.tool.androwarn.util.SSLWrapper;
import gov.nist.appvet.tool.androwarn.util.ToolStatus;
import net.minidev.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
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
	private String appFilePath = null;
	private String jsonFileReportPath = null;
	private String htmlFileReportPath = null;
	private String pdfFileReportPath = null;
	private String appIconFilePath = null;
	private String fileName = null;
	private String appId = null;
	private static String python3Cmd = null;
	private static String wkhtmltopdfCmd = null;
	
	public static HashMap<String, ArrayList<AppMetadata>> metadataHashMap = new HashMap<String, ArrayList<AppMetadata>>();
	public static HashMap<String, FindingsCategory> findingsHashMap = new HashMap<String, FindingsCategory>();


	public Service() {
		super();
		String toolOS = System.getProperty("os.name");
		if (toolOS.toUpperCase().indexOf("WIN") > -1) {
			python3Cmd = "python3";
			wkhtmltopdfCmd = "wkhtmltopdf";

		} else if (toolOS.toUpperCase().indexOf("NUX") > -1) {
			python3Cmd = "python3";
			wkhtmltopdfCmd = Properties.htmlToPdfCommand;
		}

		loadCvssData();
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
					log.debug("\nCATEGORY: " + issueData[0]);
					if (issueData[0] == null)
						log.error("Read null category from cvss file");
					log.debug("\nISSUE: " + issueData[1]);
					if (issueData[1] == null)
						log.error("Read null issue from cvss file");
					log.debug("\nVECTOR: " + issueData[2]);
					if (issueData[2] == null)
						log.error("Read null vector from cvss file");
					log.debug("\nCVSS: " + issueData[3]);
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
						log.debug("Updating category: " + issueData[0].trim());
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
	


	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		// Get received HTTP parameters and file upload
		FileItemFactory factory = new DiskFileItemFactory();
		ServletFileUpload upload = new ServletFileUpload(factory);
		List<FileItem> items = null;
		FileItem appFileItem = null;
		FileItem iconFileItem = null;

		try {
			items = upload.parseRequest(request);
		} catch (FileUploadException e) {
			e.printStackTrace();
		}

		// Get received items
		Iterator<FileItem> iter = items.iterator();
		FileItem item = null;

		// Get all incoming parameters
		while (iter.hasNext()) {
			item = (FileItem) iter.next();
			if (item.isFormField()) {
				// Get HTML form parameters
				String incomingParameter = item.getFieldName();
				String incomingValue = item.getString();
				if (incomingParameter.equals("appid")) {
					appId = incomingValue;
				}
			} else {
				// item is a file
				if (item != null) {
					if (item.getName().endsWith(".apk")) {
						appFileItem = item;
						log.debug("Received app: " + appFileItem.getName());
					} else if (item.getName().endsWith(".png")){
						iconFileItem = item;
						log.debug("Received icon: " + iconFileItem.getName());
					}
				} else {
					log.warn("Item is null");
				}
			}
		}


		if (appId == null) {
			// All tool services require an AppVet app ID
			HttpUtil.sendHttp400(response, "No app ID specified");
			return;
		}

		if (appFileItem != null && iconFileItem != null) {
			// Get app file
			fileName = FileUtil.getFileName(appFileItem.getName());
			// Must be an APK file
			if (!fileName.endsWith(".apk")) {
				HttpUtil.sendHttp400(response,
						"Invalid app file: " + appFileItem.getName());
				return;
			}

			// Create app directory
			appDirPath = Properties.TEMP_DIR + "/" + appId;

			File appDir = new File(appDirPath);
			if (!appDir.exists()) {
				appDir.mkdir();
			}

			// Set paths
			appFilePath = appDirPath + "/" + fileName;
			log.debug("APP FILE PATH: " + appFilePath);
			appIconFilePath = appDirPath + "/icon.png";
			log.debug("APP ICON PATH: " + appIconFilePath);
			jsonFileReportPath = appDirPath + "/report.json";
			log.debug("JSON REPORT PATH: " + jsonFileReportPath);

			htmlFileReportPath = appDirPath + "/report.html";
			log.debug("HTML REPORT PATH: " + htmlFileReportPath);

			pdfFileReportPath = appDirPath + "/report.pdf";
			log.debug("PDF REPORT PATH: " + pdfFileReportPath);

			if (!FileUtil.saveFileUpload(appFileItem, appFilePath)) {
				HttpUtil.sendHttp500(response, "Could not save app");
				return;
			}

			if (!FileUtil.saveFileUpload(iconFileItem, appIconFilePath)) {
				HttpUtil.sendHttp500(response, "Could not save icon");
				return;
			}

		} else {
			HttpUtil.sendHttp400(response, "No app or icon was received.");
			return;
		}

		// Send acknowledgement back to AppVet
		if (Properties.protocol.equals(Protocol.ASYNCHRONOUS.name())) {
			HttpUtil.sendHttp202(response, "Received app " + appId
					+ " for processing.");
		}

		// Start processing app
		log.debug("Executing Androwarn on app");

		ProcessBuilder pb = new ProcessBuilder(
				python3Cmd,
				Properties.ANDROWARN_HOME + "/androwarn.py",
				"-i",
				appFilePath,
				"-o",
				jsonFileReportPath,
				"-v",
				"3",
				"-r",
				"json",
				"-d",
				"-w"
				);

		// Run Androwarn and generate JSON report
		int exitCode = runCommand(pb, 3600);
		log.debug("Exit code for command: " + exitCode);
		if (exitCode != 0) {
			// All tool services require an AppVet app ID
			HttpUtil.sendHttp500(response, "Androwarn could not execute python3 command");
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
		String htmlReport = Report.createHtml(avgCvss, metadataHashMap, findingsHashMap, appId, appIconFilePath);
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
		exitCode = runCommand(pb, 60);
		log.debug("Exit code for command: " + exitCode);
		if (exitCode != 0) {
			// All tool services require an AppVet app ID
			HttpUtil.sendHttp500(response, "Androwarn could not execute wkhtmltopdf");
			return;
		}
		
		// Send to AppVet
		//sendInNewHttpRequest(appId, pdfFileReportPath, reportStatus);
		
		// Clear structures
		metadataHashMap.clear();
		findingsHashMap.clear();
		
		// Clean up
		if (!Properties.keepApps) {
			try {
				log.debug("Removing app " + appId + " files.");
				FileUtils.deleteDirectory(new File(appDirPath));
			} catch (IOException ioe) {
				log.error(ioe.getMessage());
			}
		}

		// Clean up
		System.gc();
		log.debug("End processing");

		log.debug("STOP");
		System.exit(0);

	}
	
	// Get app metadata - does not get 'analysis_results' here
	public void getMetadata(String json) {

		//		Integer arrayLength = JsonPath.read(json, "$.length()");
		//		log.debug("JSON array length: " + arrayLength);

		// Get Findings
		JSONArray jsonArray = JsonPath.read(json, "$");
		log.debug("jsonArray.size(): " + jsonArray.size());

		for (int i = 0; i < jsonArray.size(); i++) {

			LinkedHashMap<String, Object> hashMapObj = 
					(LinkedHashMap<String, Object>) jsonArray.get(i);

			for (Map.Entry<String,Object> entry : hashMapObj.entrySet()) { 

				String key = entry.getKey();
				Object value = entry.getValue();

				log.debug("MAIN KEY: " + key); 

				switch (key) {
				case "application_information": 
					log.debug("Got application_information");
					log.debug("entry here");

					getAnalysisInfo("application_information", (JSONArray) value, metadataHashMap);
					break;

				case "apk_file": 
					log.debug("Got apk_file");
					getAnalysisInfo("apk_file", (JSONArray) value, metadataHashMap);
					break;	

//				case "androidmanifest.xml": 
//					log.debug("Got androidmanifest.xml");
//					break;	
//
//				case "apis_used": 
//					log.debug("Got apis_used");
//					break;	

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
				log.debug("Got application_name");
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
				log.debug("Got file_name");
				metadataHashMap.put("file_name", arrayList);
				break;

			case "fingerprint": 
				log.debug("Got fingerprint");
				metadataHashMap.put("fingerprint", arrayList);
				break;

			case "certificate_information": 
				log.debug("Got certificate_information");
				metadataHashMap.put("certificate_information", arrayList);
				break;

			default: 
				log.error("Got unsupported key: " + key);
				break;

			}
		} 
	}

	public static double analyzeResults(String json, HashMap <String, FindingsCategory> findingsHashMap) {
		int foundCount = 0;
		double totalCvss = 0.0;
		
		//log.debug("JSON: " + json);
		
		for (Map.Entry<String, FindingsCategory> categories : findingsHashMap.entrySet()) { 
			
			String catetory = (String) categories.getKey(); 
			
			FindingsCategory findingsCategory = findingsHashMap.get(catetory);
			
			log.debug("Catagory matching: " + catetory);
			
			for (int i = 0; i < findingsCategory.definedIssues.size(); i++) {
				
				DefinedIssue definedIssue = findingsCategory.definedIssues.get(i);
				log.debug("Looking for match: " + definedIssue.issueName + "|");
				
				// MUST USE TRIM
				int startIndex = json.indexOf(definedIssue.issueName.trim());
				log.debug("startIndex: " + startIndex);
				
				if (startIndex > -1) {
					log.error("Match!");
					int endIndex = json.indexOf("\"", startIndex);
					if (startIndex > -1 && endIndex > -1 && endIndex <= json.length()) {
						String match = json.substring(startIndex, endIndex);
						log.error("ADDING ACTUAL: " + match);
						definedIssue.detectedIssues.add(match);
						foundCount++;
						totalCvss += definedIssue.cvssBaseScore;
					}
				} 
				
			}
		
		}
		
		log.debug("Total CVSS found: " + foundCount);
		log.debug("Total CVSS: " + totalCvss);
		double avgCvss = totalCvss / foundCount;
		log.debug("Average: " + avgCvss);
		return avgCvss;
	}

	/** This method should be used for sending files back to AppVet. */
	public static boolean sendInNewHttpRequest(String appId,
			String reportFilePath,
			ToolStatus reportStatus) {
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
			entity.addPart("toolrisk", new StringBody(reportStatus.name(),
					Charset.forName("UTF-8")));
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
			log.error(e.toString());
			return false;
		}
	}

	/** Note that no error stream is captured. */
	public static int runCommand(ProcessBuilder pb, int minutes) {
		log.debug("Executing: " + pb.command());

		Process p = null;
		int exitCode = 1;
		
		try {
			p = pb.start();

			InputStream is = p.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);

			// blocked
			BufferedReader reader = new BufferedReader(isr);

			String line = null;
			while ((line = reader.readLine()) != null) {
				//log.debug(line);
			}

			try {
				p.waitFor(minutes, TimeUnit.MINUTES);
				exitCode = p.exitValue();
				
			} catch (InterruptedException e) {
				// Timed-out waiting for process to complete
				Thread.currentThread().interrupt();
				log.error("Process timed-out executing: " + pb.command());
			}
			   
			reader.close();
			isr.close();
			is.close();

			return exitCode;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			p.destroy();
		}
		return 1;
	}

	public static String readJsonFile(String jsonFilePath) {
		log.debug("Reading: " + jsonFilePath);

		try {
			FileReader fr = new FileReader(jsonFilePath);

			BufferedReader reader = new BufferedReader(fr);

			String line = null;
			StringBuffer buf = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				buf.append(line);
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
