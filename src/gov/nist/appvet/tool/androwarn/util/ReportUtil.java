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
package gov.nist.appvet.tool.androwarn.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

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

import gov.nist.appvet.tool.androwarn.Properties;


public class ReportUtil {
	
	private static final Logger log = Properties.log;



	
	/**
	 * This method returns report information to the AppVet ToolAdapter as ASCII
	 * text and cannot attach a file to the response.
	 */
	public static boolean sendInHttpResponse(HttpServletResponse response,
			String reportText, ToolStatus reportStatus) {
		try {
			response.setStatus(HttpServletResponse.SC_OK); // HTTP 200
			response.setContentType("text/html");
			response.setHeader("toolrisk", reportStatus.name());
			PrintWriter out = response.getWriter();
			out.println(reportText);
			out.flush();
			out.close();
			log.debug("Returned report to AppVet");
			return true;
		} catch (IOException e) {
			log.error(e.toString());
			return false;
		}
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
	
	public static String getHtmlReport(HttpServletResponse response, String fileName,
			ToolStatus reportStatus, String report,
			String lowDescription, String moderateDescription,
			String highDescription, String errorDescription, boolean error) {
		
		String dhsLogoPath = null;
		String appvetLogoPath = null;
		String androwarnLogoPath = null;
		String toolOS = System.getProperty("os.name");
		if (toolOS.toUpperCase().indexOf("WIN") > -1) {
			dhsLogoPath = "C:\\appvet_tools\\android_mkef_files\\images\\dhs.jpg";
			appvetLogoPath = "C:\\appvet_tools\\android_mkef_files\\images\\appvet.png";
			androwarnLogoPath = "C:\\appvet_tools\\android_mkef_files\\images\\androwarn.png";
		} else if (toolOS.toUpperCase().indexOf("NUX") > -1) {
			dhsLogoPath = "/data/appvet_tools/android_androwarn_files/images/dhs.jpg";
			appvetLogoPath = "/data/appvet_tools/android_androwarn_files/images/appvet.png";
			androwarnLogoPath = "/data/appvet_tools/android_androwarn_files/images/androwarn.png";
		}
		
		StringBuffer htmlBuffer = new StringBuffer();
		htmlBuffer.append("<HTML>\n");
		htmlBuffer.append("<head>\n");
		htmlBuffer.append("<style type=\"text/css\">\n");
		htmlBuffer.append("h3 {font-family:arial;}\n");
		htmlBuffer.append("h4 {font-family:arial;}\n");
		htmlBuffer.append("p {font-family:arial;}\n");
		htmlBuffer.append("</style>\n");
		htmlBuffer.append("<title>" + Properties.toolName + "</title>\n");
		htmlBuffer.append("</head>\n");
		htmlBuffer.append("<body>\n");
		
		
		// AppVet banner
		htmlBuffer.append("<table style=\"width: 100%; background:white;padding:0px;margin:0px;\">\n");
		htmlBuffer.append("<tr>");
		
		htmlBuffer.append("<td style=\"width:50%;padding:0px;margin:0px;\" align=\"left\"><img src=\"" + dhsLogoPath + "\" alt=\"DHS logo\" style=\"height: 40px;\"></td>");
		htmlBuffer.append("<td style=\"width:50%;padding:0px;margin:0px;\" align=\"right\"><img src=\"" + appvetLogoPath + "\" alt=\"AppVet logo\" style=\"height: 35px;\"></td>");

		htmlBuffer.append("</tr>");
		htmlBuffer.append("</td>\n");
		htmlBuffer.append("</table>\n");
		htmlBuffer.append("<br>\n");
		
		// Content
		htmlBuffer.append("<br><br><br><br><img src=\"" + androwarnLogoPath
				+ "\" alt=\"Androwarn\" height=\"75\">\n");
		htmlBuffer.append("<br>\n");
		htmlBuffer.append("<h3>" + Properties.toolName + " Report</h3>\n");
		htmlBuffer.append("<pre>\n");
		htmlBuffer.append("File: \t\t" + fileName + "\n");
		final Date date = new Date();
		final SimpleDateFormat format = new SimpleDateFormat(
				"yyyy-MM-dd' 'HH:mm:ss.SSSZ");
		final String currentDate = format.format(date);
		htmlBuffer.append("Date: \t\t" + currentDate + "\n\n");
		if (reportStatus == ToolStatus.LOW) {
			htmlBuffer.append("Risk: \t\t<font color=\"green\">"
					+ reportStatus.name() + "</font>\n");
			htmlBuffer.append(lowDescription);
		} else if (reportStatus == ToolStatus.MODERATE) {
			htmlBuffer.append("Risk: \t\t<font color=\"orange\">"
					+ reportStatus.name() + "</font>\n");
			htmlBuffer.append(moderateDescription);
		} else if (reportStatus == ToolStatus.HIGH) {
			htmlBuffer.append("Risk: \t\t<font color=\"red\">"
					+ reportStatus.name() + "</font>\n");
			htmlBuffer.append(highDescription);
		} else {
			htmlBuffer.append("Status: \t<font color=\"red\">"
					+ reportStatus.name() + "</font>\n");
			htmlBuffer.append(errorDescription);
		}
		
		if (!error) {
			String instructions = "<br><br>To determine the identified problems, "
					+ "<b>search the report below for one or more of the following vulnerabilities</b>:<br>\n" + 
					"<ul>" + 
					"<li>Telephony identifiers exfiltration\n" + 
					"<li>Device settings exfiltration\n" + 
					"<li>Geolocation information leakage\n" + 
					"<li>Connection interfaces information exfiltration\n" +
					"<li>Telephony services abuse\n" +
					"<li>Audio/video flow interception\n" +
					"<li>Remote connection establishment\n" +
					"<li>PIM data leakage\n" +
					"<li>External memory operations\n" +
					"<li>PIM data modification\n" +
					"<li>Arbitrary code execution\n" +
					"<li>Denial of Service\n" +
					"</ul>";
			
			htmlBuffer.append(instructions);

		}
		htmlBuffer.append("<h3>Details</h3>");
		htmlBuffer.append(report);
		htmlBuffer.append("</body>\n");
		htmlBuffer.append("</HTML>\n");
		return htmlBuffer.toString();
	}


}
