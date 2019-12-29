package gov.nist.appvet.tool.androwarn;

import java.util.ArrayList;
import java.util.List;

public class FindingsCategory {
		
	public AnalysisCategory category = null;
	public List<DefinedIssue> definedIssues = null;
	
	public FindingsCategory(AnalysisCategory category) {
		this.category = category;
		this.definedIssues = new ArrayList<DefinedIssue>();
	}
	
	public int getNumIssuesFound() {
		int totalNumIssuesFound = 0;
		for (int i = 0; i < definedIssues.size(); i++) {
			DefinedIssue definedIssue = definedIssues.get(i);
			totalNumIssuesFound += definedIssue.detectedIssues.size();
		}
		return totalNumIssuesFound;
	}
	
	public void clear() {
		definedIssues.clear();
	}

}
