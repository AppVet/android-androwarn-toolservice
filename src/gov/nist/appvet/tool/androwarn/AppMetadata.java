package gov.nist.appvet.tool.androwarn;

import gov.nist.appvet.tool.androwarn.util.Logger;

/** Note that the KEY value for these is stored in the hashmap. */
public class AppMetadata {
	
	private static final Logger log = Properties.log;

	public String category;
	public String issue;
	
	public AppMetadata() {
		this.issue = null;
	}
	
	public AppMetadata(String category, String issue) {
		this.category = category;
		this.issue = issue;
	}
	
	public AppMetadata(String issue) {
		this.issue = issue;
	}
}
