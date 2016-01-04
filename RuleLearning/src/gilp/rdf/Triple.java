package gilp.rdf;

import java.util.ArrayList;

import gilp.rule.Predicate;
import gilp.rule.RDFPredicate;

/*
 * To envelope the information about a RDF triple. 
 * CJC 2015.10.28
 * */
public class Triple {
	String _subject;
	String _predicate; 
	String _obj;
	String _tid; 

	public Triple(){}
	
	public Triple(String tid, String subject, String predicate, String obj) {
		this._tid = tid;
		this._subject = subject;
		this._predicate = predicate;
		this._obj = obj;
	}
	
	public Triple(String subject, String predicate, String obj) {
		this._tid = "unknown";
		this._subject = subject;
		this._predicate = predicate;
		this._obj = obj;
	}
	
	public String get_tid() {
		return _tid;
	}

	public void set_tid(String  tid) {
		this._tid = tid;
	}

	public String get_subject() {
		return _subject;
	}
	public void set_subject(String subject) {
		this._subject = subject;
	}
	public String get_predicate() {
		return _predicate;
	}
	public void set_predicate(String  predicate) {
		this._predicate = predicate;
	}
	public String get_obj() {
		return _obj;
	}
	public void set_obj(String obj) {
		this._obj = obj;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer("");
		//sb.append("<" +this.tid + "> ");
		sb.append("<" + this._subject + "> ");
		sb.append("<" + this._predicate + "> ");
		sb.append("<" + this._obj + ">");
		return sb.toString();
	}
	
	@Override
	public Triple clone(){
		Triple t = new Triple();
		t.set_obj(this._obj);
		t.set_subject(this._subject);
		t.set_predicate(this._predicate);
		t.set_tid(this._tid);
		return t;
	}
	
	@Override
	public boolean equals(Object o){
		if (!this.getClass().isInstance(o)){
			return false;
		}
		Triple t = (Triple)o;
		if(!this.get_subject().equals(t.get_subject()))
			return false;
		if (!this.get_obj().equals(t.get_obj()))
			return false;
		if (!this.get_predicate().equals(t.get_predicate()))
			return false;
		
		return true;
	}

	public static ArrayList<Triple> removeDuplicated(ArrayList<Triple> listTriples){
		ArrayList<Triple> listRlts = new ArrayList<Triple>(); 
		for(Triple t: listTriples){
			if (!listRlts.contains(t)){
				listRlts.add(t);
			}
		}
		listTriples.clear();
		return listRlts;
	}
	
	// get the corresponding correct_prefixed predicate
	// e.g. hasGivenName(X,Y) -----> correct_hasGivenName(X,Y)
	public Triple mapToCorrectTriple() {
		Triple t = this.clone();
		String pred_name = t.get_predicate();
		pred_name = Predicate.CORRECT_PREDICATE + "_" + pred_name;
		t.set_predicate(pred_name);
		return t;
	}

	public Triple mapToIncorrectTriple() {
		Triple t = this.clone();
		String pred_name = t.get_predicate();
		pred_name = Predicate.INCORRECT_PREDICATE + "_" + pred_name;
		t.set_predicate(pred_name);
		return t;
	}

	// e.g. correct_hasGivenName(X,Y) -----> hasGivenName(X,Y)
	public Triple mapToOriginalTriple() {
		Triple t = this.clone();
		String pred_name = t.get_predicate();
		pred_name = pred_name.substring(pred_name.indexOf("_") + 1);
		t.set_predicate(pred_name);
		return t;
	}
	
	public static ArrayList<Triple> getCommonTriples (ArrayList<Triple> list1, ArrayList<Triple> list2){
		ArrayList<Triple> listRlts = new ArrayList<Triple>();
		for (Triple t1: list1){
			for (Triple t2: list2){
				if (t1.equals(t2)){
					if (!listRlts.contains(t1))
						listRlts.add(t1);
				}
			}
		}
		return listRlts;
	}
	
	public static void main(String[] args){
		Triple t = new Triple("Yao_Ming","hasGivenName","Yao");
		Triple t1 = t.clone();
		System.out.println(t.equals(t1));
		System.out.println(t == t1);
		t = t.mapToCorrectTriple();
		System.out.println(t);
		t = t.mapToOriginalTriple();
		System.out.println(t);
		t = t.mapToIncorrectTriple();
		System.out.println(t); 
	}

}
