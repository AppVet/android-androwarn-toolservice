package gov.nist.appvet.tool.androwarn;

import java.util.ArrayList;
import java.util.List;

public class DefinedIssue {
	public String issueName = null;
	public String cvssVectorStr = null;
	public double cvssBaseScore = -1.0;
	public List<String> detectedIssues = new ArrayList<String>();
	
	public DefinedIssue(String issueName, String cvssVectorStr, double cvssBaseScore) {
		this.issueName = issueName;
		this.cvssVectorStr = cvssVectorStr;
		this.cvssBaseScore = cvssBaseScore;
	}
}
