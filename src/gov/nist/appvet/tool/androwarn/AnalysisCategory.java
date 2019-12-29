package gov.nist.appvet.tool.androwarn;


public enum AnalysisCategory {telephony_identifiers_leakage ("Telephony Identifiers Leakage"), 
		device_settings_harvesting ("Device Settings Harvesting"),
		location_lookup ("Location Lookup"),
		connection_interfaces_exfiltration ("Connection Interfaces Exfiltration"),
		telephony_services_abuse ("Telephony Services Abuse"),
		audio_video_eavesdropping ("Audio Video Eavesdropping"),
		suspicious_connection_establishment ("Suspicious Connection Establishment"),
		PIM_data_leakage ("PIM Data Leakage"),
		code_execution ("Code Execution");
	
    private static final AnalysisCategory[] copyOfValues = values();
    public String formalName = null;
    

  
    // enum constructor - cannot be public or protected 
    private AnalysisCategory(String formalName) 
    { 
        this.formalName = formalName; 
    } 
    
    // getter method 
    public String getFormalName() 
    { 
        return this.formalName; 
    } 
    
    public static AnalysisCategory getEnum(String name) {
        for (AnalysisCategory value : copyOfValues) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return null;
    }
}
