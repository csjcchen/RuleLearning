package gilp.rule;

import java.util.Iterator;

import gilp.learning.GILPSettings;
import gilp.rdf.JoinType;
import gilp.rdf.Triple;
import gilp.utility.NumericFeatureTools;

import java.util.ArrayList;

public class RDFPredicate extends Predicate{
	//TODO need to introduce more operators, like <, !=, > , or, just = and < 
	String _predicate_name = null;
	String _subject = null;
	String _object = null; 	
	
	
	
	public RDFPredicate(){}
	
	public RDFPredicate(String s, String p, String o){
		this._predicate_name = p;
		this._subject =  s;
		this._object = o;
	}	
	
	public RDFPredicate(Triple t) {
		this(t.get_subject(),t.get_predicate(),t.get_obj());
	}
	
	/*
	 * return the common variable of two triple pattern. 
	 * return null if there is no common variable. 
	 * */
	public static JoinType[] getJoinTypes(RDFPredicate tp1, RDFPredicate tp2){
		ArrayList<JoinType> rlts = new ArrayList<JoinType>();		 
		String sub1 = tp1.getSubject().toString();
		String sub2 = tp2.getSubject().toString();
		String obj1 = tp1.getObject().toString();
		String obj2 = tp2.getObject().toString();
		
		if (sub1.equals(sub2))			
 			rlts.add(JoinType.SS);
		if(sub1.equals(obj2))
			rlts.add(JoinType.SO);		
		if (obj1.equals(sub2))
			rlts.add(JoinType.OS);
		if (obj1.equals(obj2))
			rlts.add(JoinType.OO);		
				
		if (rlts.size()==0)
			return null;
		else
			return rlts.toArray(new JoinType[0]);
	}
	
	@Override
	public void setPredicateName(String name) { 
		this._predicate_name = name;
	}
	@Override
	public String getPredicateName(){
		return this._predicate_name;
	}
	
	public void setSubject(String var){
		this._subject = var;
	}
	public void setObject(String var){
		this._object = var;
	}
	
	public String getSubject(){
		return this._subject;
	}
	public String getObject(){
		return this._object;
	}
	
	public boolean isSubjectVariable(){
		return isVariable(this._subject);
	}
	
	public boolean isObjectVariable(){
		return isVariable(this._object);
	}
	
	public boolean isPredicateVariable(){
		return isVariable(this._predicate_name);
	}
	
	public boolean isPredicateComparator(){
		for (String pred: RDFPredicate._comparators){
			if(pred.equals(this._predicate_name))
				return true;
		}
		return false;
	}
	
	public static String[] _comparators = {"lessThan", "largerThan", "equalTo", "notEqualTo"};
	
	public static boolean satisfyComparison(String comparator, String s, String o){
		if (comparator.equals(_comparators[0])){
			//lessThan
			double d1 = Double.parseDouble(s);
			double d2 = Double.parseDouble(o);
			return d1<d2;
		}
		else if(comparator.equals(_comparators[1])){
			//largerThan
			double d1 = Double.parseDouble(s);
			double d2 = Double.parseDouble(o);
			return d1>d2;
		}
		else if(comparator.equals(_comparators[2])){
			//equanlTo
			return s.equals(o);			
		}
		else if(comparator.equals(_comparators[3])){
			//notEqualTo
			return !s.equals(o);
		}
		else{
			System.out.println("Error! RDFPredicate.satisfyComparison: the input predicate '" + comparator + "' is undefined."  );
			return false;
		}
	}
	
	public boolean isObjectNumeric(){
		for (String strPred : GILPSettings.NUMERICAL_PREDICATES){
			if (this.getPredicateName().equalsIgnoreCase(strPred)){
				return true;
			}
		}
		return false;
	}
	
	boolean isVariable(String val){
		return val.toString().startsWith("?");
	}
	
	
	@Override
	public RDFPredicate clone(){
		RDFPredicate tp = new RDFPredicate();
		tp.setPredicateName(this._predicate_name);
		tp.setObject(this._object);
		tp.setSubject(this._subject);
		return tp;
	}

	//get the corresponding correct_prefixed predicate
	//e.g. hasGivenName(X,Y)   ----->  correct_hasGivenName(X,Y)  
	public RDFPredicate mapToCorrectPred(){
		RDFPredicate tp = this.clone();
		String pred_name = tp.getPredicateName(); 
		pred_name = Predicate.CORRECT_PREDICATE + "_" + pred_name;
		tp.setPredicateName(pred_name);
		return tp;
	}
	
	public RDFPredicate mapToIncorrectPred(){
		RDFPredicate tp = this.clone();
		String pred_name = tp.getPredicateName(); 
		pred_name = Predicate.INCORRECT_PREDICATE + "_" + pred_name;
		tp.setPredicateName(pred_name);
		return tp;
	}
	
	//e.g. correct_hasGivenName(X,Y)   ----->  hasGivenName(X,Y)  
	public RDFPredicate mapToOriginalPred(){
		RDFPredicate tp = this.clone();
		String pred_name = tp.getPredicateName(); 
		pred_name = pred_name.substring(pred_name.indexOf("_")+1);
		tp.setPredicateName(pred_name);
		return tp;
	}

	@Override
	@Deprecated
	//use setSubject() and setObject instead
	public void addVariable(String var) {
		GILPSettings.log(this.getClass().getName() + "Error! The addVariable feature is not supported.");
	}

	@Override
	@Deprecated
	//use getSubject() and getObject() instead
	public Iterator<String> getVariableIterator() {
		GILPSettings.log(this.getClass().getName() + "Error! The getVariableIterator feature is not supported.");
		return null;
	}
	
	public ArrayList<String> getConstants(){
		return this.extractElements(0);
	}
	
	public ArrayList<String> getVariables(){		
		ArrayList<String> vars = this.extractElements(1);
		return vars;
	}
	
	@Override
	public boolean equals(Object o){
		if (!this.getClass().isInstance(o)){
			return false;
		}
		
		RDFPredicate tp = (RDFPredicate)o;
		
		if(!this.getSubject().equals(tp.getSubject()))
			return false;
		if (!this.getObject().equals(tp.getObject()))
			return false;
		if (!this.getPredicateName().equals(tp.getPredicateName()))
			return false;
		
		return true;
	}
	
	
	
	public double[] getObjBounds(){
		return NumericFeatureTools.getBounds(this._object);
	}
	
	
	public boolean match(Triple tr){
		if (!this.getPredicateName().equals(tr.get_predicate()))
			return false;
		if (!this.isSubjectVariable() && !this.getSubject().equals(tr.get_subject())){
			return false;
		}		
		
		if (!this.isObjectVariable()){
			if (this.isObjectNumeric()){
				if (NumericFeatureTools.compareRange(this.getObject(), tr.get_obj())<= 0 )
					return false;
			}
			else{
				if (!this.getObject().equals(tr.get_obj()))
					return false;
			}
		} 
		return true;
	} 
	
	// type: 0 - constant, 1 -- variable
	public ArrayList<String> extractElements( int type) {
		ArrayList<String> listRlts = new ArrayList<String>();

		if (this.isSubjectVariable() && (type == 1)) {
			listRlts.add(this.getSubject().toString());
		} else if (!this.isSubjectVariable() && (type == 0)) {
			if (!listRlts.contains(this.getSubject().toString()))
				listRlts.add(this.getSubject().toString());
		}

		if (this.isObjectVariable() && (type == 1)) {
			if (!listRlts.contains(this.getObject().toString()))
				listRlts.add(this.getObject().toString());
		} else if (!this.isObjectVariable() && (type == 0)) {
			if (!listRlts.contains(this.getObject().toString()))
				listRlts.add(this.getObject().toString());
		}
		return listRlts;
	}
	
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer(); 
		sb.append(this._predicate_name + "(");
		sb.append(this._subject).append(",");
		sb.append(this._object).append(")");
		return sb.toString();
	}
	
	// e.g. vars ={X,Y}, bindings = {Yao_Ming, Yao}, tp = hasGivenName(X,Y)
	// return hasGivenName(Yao_Ming, Yao)
	public Triple bind(ArrayList<String> vars, ArrayList<String> bindings) {
		Triple t = new Triple(this.getSubject(), this.getPredicateName(), this.getObject());
		for (int i = 0; i < vars.size(); i++) {
			String v = vars.get(i);
			if (this.getSubject().equals(v))
				t.set_subject(bindings.get(i));
			if (this.getObject().equals(v))
				t.set_obj(bindings.get(i));
		}
		return t;
	}
	
	//return 1: this is more general than tp
	//-1: tp is more general than this
	//0: no one is more general than the other
	public int compareGeneralization(RDFPredicate tp){
		//current implementation only considers numerical predicates like hasLength
		if (!this.getSubject().equals(tp.getSubject()))
			return 0;
		
		if (!this.isObjectNumeric())
			return 0;
		
		if (this.isObjectVariable() && tp.isObjectVariable())
			return 0; // R(?s, ?o1) == R(?s, ?o2)
		
		if(this.isObjectVariable() && !tp.isObjectVariable())
			return 1;// R(?s, ?o) > R(?s, "[2,3]") 
		
		if (!this.isObjectVariable() && tp.isObjectVariable())
			return -1; //R(?s, "[2,3]") < R(?s, ?o)  
		
		if (!this.isObjectVariable() && !tp.isObjectVariable()){
			return NumericFeatureTools.compareRange(this._object, tp._object);			
		}
		
		return 0;
	}
	
	//###############################################################################

	//                   unit  tests
		
	//###############################################################################

	static void testMatchNumeric(){
		RDFPredicate tp = new RDFPredicate();
		tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("hasWeight");
		tp.setObject(new String("[1,200)"));		
	
		Triple tr = new Triple();
		tr.set_subject("Yao_Ming");
		tr.set_predicate("hasWeight");
		tr.set_obj("199.88");
		
		System.out.println(tr + " matches "  + tp + ": " + tp.match(tr));
		tr.set_obj("-90");
		System.out.println(tr + " matches "  + tp + ": " + tp.match(tr));
		tr.set_obj("900");
		System.out.println(tr + " matches "  + tp + ": " + tp.match(tr));
		tr.set_obj("1");
		System.out.println(tr + " matches "  + tp + ": " + tp.match(tr));			
	}
	
	static void testCompareGeneralization(){
		RDFPredicate tp1 = new RDFPredicate();
		tp1 = new RDFPredicate();
		tp1.setSubject(new String("?s"));
		tp1.setPredicateName("hasWeight");
		tp1.setObject(new String("[1,200)"));
		
		RDFPredicate tp2 = new RDFPredicate();
		tp2 = new RDFPredicate();
		tp2.setSubject(new String("?s"));
		tp2.setPredicateName("hasWeight");
		tp2.setObject(new String("[1,200)"));
		
		System.out.println(tp1 + " vs. " + tp2 + ":" + tp1.compareGeneralization(tp2));
		tp2.setObject(new String("[2,200)"));
		System.out.println(tp1 + " vs. " + tp2 + ":" + tp1.compareGeneralization(tp2));
		tp2.setObject(new String("(1,200)"));
		System.out.println(tp1 + " vs. " + tp2 + ":" + tp1.compareGeneralization(tp2));
		tp2.setObject(new String("[3,20]"));
		System.out.println(tp1 + " vs. " + tp2 + ":" + tp1.compareGeneralization(tp2));
		tp2.setObject(new String("[-1,200)"));
		System.out.println(tp1 + " vs. " + tp2 + ":" + tp1.compareGeneralization(tp2));
		tp2.setObject(new String("[1,200]"));
		System.out.println(tp1 + " vs. " + tp2 + ":" + tp1.compareGeneralization(tp2));
		tp2.setObject(new String("[2,201)"));
		System.out.println(tp1 + " vs. " + tp2 + ":" + tp1.compareGeneralization(tp2));
	}

	public static void main(String[] args){
		testCompareGeneralization();
	}
}
