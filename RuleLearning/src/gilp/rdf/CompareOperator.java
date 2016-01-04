package gilp.rdf;

public enum CompareOperator {
	EQUAL, NON_EQUAL, LARGER_THAN, LESS_THAN; 
	
	public String toString(){
		switch (this.ordinal()){
		case 0:
			return "=";
		case 1:
			return "!=";
		case 2:
			return ">";
		case 3:
			return "<";
		default:
			return "";
		}
	}
	
	public static void main(String[] args){
		CompareOperator opt = LARGER_THAN; 
		System.out.println(opt);
	}
}
