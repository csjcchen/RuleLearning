package gilp.rule;

import java.util.Iterator;

import gilp.learning.GILPSettings;
import gilp.rdf.JoinType;
import gilp.rdf.Triple;

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
	
	public boolean match(Triple tr){
		if (!this.getPredicateName().equals(tr.get_predicate()))
			return false;
		if (!this.isSubjectVariable() && !this.getSubject().equals(tr.get_subject())){
			return false;
		}		
		if (!this.isObjectVariable() && !this.getObject().equals(tr.get_obj())){
			return false;
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
	 
}
