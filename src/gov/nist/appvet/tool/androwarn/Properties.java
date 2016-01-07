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

import gov.nist.appvet.tool.androwarn.util.Logger;
import gov.nist.appvet.tool.androwarn.util.ToolStatus;
import gov.nist.appvet.tool.androwarn.util.Xml;

import java.io.File;
import java.util.ArrayList;

/**
 * This class reads in property values defined in ToolProperties.xml as well as
 * required environment variables from the host system.
 * 
 * @author steveq@nist.gov
 */
public class Properties {

    /** Github release version number. */
    public static final String version = "1.0.1";

    /*
     * CHANGE: Declare tool-specific environment variables. Refactor
     * EXAMPLE_SERVICE_FILES_HOME to your environment variable name.
     */
    public static String ANDROID_ANDROWARN_FILES_HOME = null;
    public static String LOG_DISPLAY_NAME = "ANDROWARN";
    // Defines app file path in ToolProperties.xml
    public static final String APP_FILE_PATH = "[APP_FILE_PATH]";
    // Defines Androwarn home path in ToolProperties.xml
    public static final String ANDROWARN_HOME_SUB = "[ANDROWARN_HOME]";

    /** DON'T CHANGE (START) **/
    public static String JAVA_HOME = null;
    public static String ANDROWARN_HOME = null;
    public static final String PROPERTIES_FILE_NAME = "ToolProperties.xml";
    public static String toolName = null;
    public static String toolVersion = null;
    public static String protocol = null;
    public static String command = null;
    public static boolean keepApps = false;
    public static int commandTimeout = 0;
    public static String reportFormat = null;
    public static String serviceUrl = null;
    public static Logger log = null;
    public static String LOG_PATH = null;
    public static boolean LOG_TO_CONSOLE = false;
    public static String LOG_LEVEL = null;
    public static String TEMP_DIR = null;
    public static String CONF_DIR = null;
    public static String LOGS_DIR = null;
    public static ToolStatus defaultStatus = null;
    public static ArrayList<String> lowResults = null;
    public static ArrayList<String> moderateResults = null;
    public static ArrayList<String> highResults = null;
    public static ArrayList<String> errorResults = null;
    // Asynchronous authentication parameters for returning report to AppVet
    public static String appvetUrl = null;
    public static String appvetHttpMode = null;
    public static String toolId = null;
    public static String appvetUsername = null;
    public static String appvetPassword = null;
    /** DON'T CHANGE (END) **/

    static {
	/* CHANGE: Loading statement */
	System.out.println("*** Starting Android Androwarn Service v" + version + " ***");
	String toolOS = System.getProperty("os.name");
	JAVA_HOME = System.getenv("JAVA_HOME");
	if (JAVA_HOME == null) {
	    System.err.println("Environment variable JAVA_HOME not set.");
	}
	ANDROWARN_HOME = System.getenv("ANDROWARN_HOME");
	if (ANDROWARN_HOME == null) {
	    System.err.println("Environment variable ANDROWARN_HOME not set. "
		    + "Please set to the installation directory of Androwarn.");
	}
	/* Change! Set tool-specific environment variables here */
	ANDROID_ANDROWARN_FILES_HOME = System
		.getenv("ANDROID_ANDROWARN_FILES_HOME");
	if (ANDROID_ANDROWARN_FILES_HOME == null) {
	    System.err
		    .println("Environment variable ANDROID_ANDROWARN_FILES_HOME not set."
			    + "Please set to the /appvet_tools/android-androwarn-toolservice "
			    + "directory.");
	}

	/* CHANGE (START): Check if tool exists */
	String androwarn = ANDROWARN_HOME;
	if (toolOS.toUpperCase().indexOf("WIN") > -1) {
	    androwarn += "/androwarn.py";
	} else if (toolOS.toUpperCase().indexOf("NUX") > -1) {
	    androwarn += "/androwarn.py";
	}
	File file = new File(androwarn);
	if (!file.exists()) {
	    System.err.println(androwarn + " not found");
	}
	/* CHANGE (END): Check if tool exists */

	TEMP_DIR = ANDROID_ANDROWARN_FILES_HOME + "/apps";
	File tempDir = new File(TEMP_DIR);
	if (!tempDir.exists()) {
	    tempDir.mkdirs();
	    System.out.println("Created temp directory for Appollo");
	}

	CONF_DIR = ANDROID_ANDROWARN_FILES_HOME + "/conf";
	if (!new File(CONF_DIR).exists()) {
	    System.err
		    .println("Directory $ANDROID_ANDROWARN_FILES_HOME/conf does not exist.");
	}

	// Load XML property file
	File configFile = new File(CONF_DIR + "/" + PROPERTIES_FILE_NAME);
	if (!configFile.exists()) {
	    System.err.println("Configuration file does not exist.");
	}
	final Xml xml = new Xml(configFile);

	// Do Logging first so we can use log below
	LOGS_DIR = ANDROID_ANDROWARN_FILES_HOME + "/logs";
	String logName = xml.getXPathValue("/Tool/Logging/LogName");
	LOG_PATH = LOGS_DIR + "/" + logName;
	LOG_LEVEL = xml.getXPathValue("/Tool/Logging/Level");
	LOG_TO_CONSOLE = new Boolean(
		xml.getXPathValue("/Tool/Logging/ToConsole")).booleanValue();
	log = new Logger(LOG_PATH, LOG_DISPLAY_NAME);
	log.info("/Tool/Logging/LogName: " + logName);
	log.info("/Tool/Logging/Level: " + LOG_LEVEL);
	log.info("/Tool/Logging/ToConsole: "
		+ new Boolean(LOG_TO_CONSOLE).toString());

	// Tool name
	toolName = xml.getXPathValue("/Tool/Name");
	log.info("/Tool/MainURL: " + toolName);

	// Tool version
	toolVersion = xml.getXPathValue("/Tool/Version");
	log.info("/Tool/MainURL: " + toolVersion);

	// Service URL
	serviceUrl = xml.getXPathValue("/Tool/ServiceURL");
	log.info("/Tool/ServiceURL: " + serviceUrl);

	// AppVet protocol
	protocol = xml.getXPathValue("/Tool/AppVetProtocol");
	log.info("/Tool/AppVetProtocol: " + protocol);

	// Command
	command = xml.getXPathValue("/Tool/Command");
	log.info("/Tool/Command: " + command);
	
	// Keep Apps
	keepApps = new Boolean(xml.getXPathValue("/Tool/KeepApps")).booleanValue();
	log.info("/Tool/KeepApps: " + keepApps);

	// Command Timeout
	String cmdTimeoutStr = xml.getXPathValue("/Tool/CommandTimeout");
	commandTimeout = new Integer(cmdTimeoutStr).intValue();

	// Get report format
	reportFormat = xml.getXPathValue("/Tool/Report/Format");
	log.info("/Tool/Report/Format: " + reportFormat);

	// Get result categories
	String defaultStatusString = xml
		.getXPathValue("/Tool/Report/Result/DefaultStatus");
	log.info("/Tool/Report/Result/DefaultStatus: " + defaultStatusString);
	if (defaultStatusString == null || defaultStatusString.isEmpty()) {
	    System.err.println("Default status cannot be null or empty.");
	}
	defaultStatus = ToolStatus.getEnum(defaultStatusString);
	if (defaultStatus == null) {
	    System.err.println("Unknown default status.");
	}
	lowResults = xml.getXPathValues("/Tool/Report/Result/Low");
	if (lowResults != null) {
	    for (int i = 0; i < lowResults.size(); i++) {
		log.debug("LOW STRINGS: " + lowResults.get(i));
	    }
	}
	moderateResults = xml.getXPathValues("/Tool/Report/Result/Moderate");
	if (moderateResults != null) {
	    for (int i = 0; i < moderateResults.size(); i++) {
		log.debug("MODERATE STRINGS: " + moderateResults.get(i));
	    }
	}
	highResults = xml.getXPathValues("/Tool/Report/Result/High");
	if (highResults != null) {
	    for (int i = 0; i < highResults.size(); i++) {
		log.debug("HIGH STRINGS: " + highResults.get(i));
	    }
	}
	errorResults = xml.getXPathValues("/Tool/Report/Result/Error");
	if (errorResults != null) {
	    for (int i = 0; i < errorResults.size(); i++) {
		log.debug("ERROR STRINGS: " + errorResults.get(i));
	    }
	}
	// Get any additional parameters for asynchronous reports back to AppVet
	appvetUrl = xml.getXPathValue("/Tool/AppVet/URL");
	log.info("/Tool/AppVet/URL: " + appvetUrl);
	appvetHttpMode = xml.getXPathValue("/Tool/AppVet/Method");
	log.info("/Tool/AppVet/Method: " + appvetHttpMode);
	toolId = xml.getXPathValue("/Tool/AppVet/ToolId");
	log.info("/Tool/AppVet/ToolId: " + toolId);
	appvetUsername = xml.getXPathValue("/Tool/AppVet/Username");
	// log.info("/Tool/AppVet/Username: " + appvetUsername);
	appvetPassword = xml.getXPathValue("/Tool/AppVet/Password");
	// log.info("/Tool/AppVet/Password: " + appvetPassword);

    }
}
