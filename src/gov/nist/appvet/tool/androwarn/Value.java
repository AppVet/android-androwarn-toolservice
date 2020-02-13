package gov.nist.appvet.tool.androwarn;

/** Note that the KEY value for these is stored in the hashmap. */
public class Value {
	
	
	private Object value;
	private DataType dataType;
	public enum DataType 
	{ 
	    STRING, INTEGER, DOUBLE, BOOLEAN; 
	} 
	public InfoType infoType;
	public enum InfoType {
		METADATA, ISSUE
	}
	
	public Value() {
		this.value = 0.0;
		this.dataType = null;
	}
	
	public Value(Object value, DataType dataType, InfoType infoType) throws Exception {
		if (value == null || dataType == null || infoType == null) {
			throw new Exception("Trying to create a Value with null value, dataType or infoType");
		}
		this.value = value;
		this.dataType = dataType;
		this.infoType = infoType;
	}
	
	public DataType getDataType() {
		return dataType;
	}

	public void setValue(double value) {
		this.value = value;
	}
	
	public InfoType getInfoType() {
		return infoType;
	}

	public void setInfoType(InfoType infoType) {
		this.infoType = infoType;
	}
	
	public Object getValue() {
		switch (dataType){
			case STRING: return value;
			case INTEGER: return (Integer) value;
			case DOUBLE: return (Double) value;
			case BOOLEAN: return (Boolean) value;
			default: return null;
		}
	}
}
