package gilp.rdf;

public class RDFFilter {
	
	private String _variable; 
	private CompareOperator _opt;
	private String _value; 
	
	public RDFFilter (String var, CompareOperator opt, String val){
		this._variable = var;
		this._opt = opt;
		this._value = val;
	}

	public String get_variable() {
		return _variable;
	}

	public CompareOperator get_opt() {
		return _opt;
	}

	public String get_value() {
		return _value;
	}
	
	public String toString(){
		String rlt = this._variable; 
		switch(this._opt){
		case EQUAL: 
			rlt += "=";
			break;
		case NON_EQUAL:
			rlt += "!=";
			break;
		case LARGER_THAN:
			rlt += ">";
			break;
		case LESS_THAN:
			rlt += "<";
			break;
		}
		rlt += this._value;
		return rlt;
	}
	

}
