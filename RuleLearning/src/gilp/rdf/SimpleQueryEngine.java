
package gilp.rdf;

import java.util.ArrayList;
import java.util.Iterator;
import gilp.rule.Clause;
 

/*
 * This simple engine is to perform some testing or demo. 
 * It provides the abilities to retrieve RDF triples by id, subject, predicate or object. 
 * Currently, the triples are stored in a text file. 
 * CJC 2015.10.28
 * */

public class SimpleQueryEngine implements QueryEngine{
	ArrayList<Triple> _dataset = null;
	
	public SimpleQueryEngine(){
		initDataSet();
	}
	
	public void setDataSet(ArrayList<Triple> data){
		this._dataset = new ArrayList<Triple>();
		this._dataset.addAll(data);
	}
	
	public ArrayList<Triple> getDataSet(){		
		return this._dataset;
	}

	// initialize a sample data set
	private void initDataSet() {
		this._dataset = new ArrayList<Triple>();
		this._dataset.add(new Triple("Yao_Ming", "hasGivenName", "Yao"));
		this._dataset.add(new Triple("Yao_Ming", "hasFamilyName", "Ming"));
		this._dataset.add(new Triple("Yao_Ming", "isCitizenOf", "China"));
		this._dataset.add(new Triple("Yao_Ming", "isCitizenOf", "USA"));

		this._dataset.add(new Triple("Liu_Xiang", "hasGivenName", "Liu"));
		this._dataset.add(new Triple("Liu_Xiang", "hasFamilyName", "Xiang"));
		this._dataset.add(new Triple("Liu_Xiang", "isCitizenOf", "China"));

		this._dataset.add(new Triple("Li_Na", "hasGivenName", "Li"));
		this._dataset.add(new Triple("Li_Na", "hasFamilyName", "Na"));
		this._dataset.add(new Triple("Li_Na", "isCitizenOf", "China"));

		this._dataset.add(new Triple("Lionel_Mercy", "hasGivenName", "Lionel"));
		this._dataset.add(new Triple("Lionel_Mercy", "hasFamilyName", "Mercy"));
		this._dataset.add(new Triple("Lionel_Mercy", "isCitizenOf", "Agentine"));

	}
	public Triple getTripleByTid (String tid){
		ArrayList<Triple> rlts = retrieveTriples(0, -1, tid, "");
		if (rlts.isEmpty()) 
			return null;
		else 
			return rlts.get(0);
	}	
	
	public ArrayList<Triple>  getTriplesBySubject (String subject){
		return retrieveTriples(1, -1, subject, "");
	}
	
	
	public ArrayList<Triple>  getTriplesByPredicate (String predicate){
		return  retrieveTriples(2,-1,  predicate, "");
	}
	

	public ArrayList<Triple>  getTriplesByObject (String object){
		return retrieveTriples(3, -1, object, "");
	}
	
	public ArrayList<Triple>  getTriplesByPredicateObject (String predicate, String object){
		return retrieveTriples(2, 3, predicate, object);
	}
	
	//Try to match a triple and a string. The comparison is between the string and the id, 
	//subject, predicate or object of the triple, according to the value specified by type. 
	//type =-1: all; 0: id; 1 : subject; 2: predicate; 3: object.
	boolean isMatch(Triple triple, int type, String query){
		switch (type){
		case -1:
			return true;
		case 0: 
			if (triple.get_tid().equalsIgnoreCase(query)){
				return true;
			}
			break;
		case 1:
			if (triple.get_subject().equalsIgnoreCase(query)){
				return true;
			}
			break;
		case 2:
			if (triple.get_predicate().equalsIgnoreCase(query)){
				return true;
			}
			break;
		case 3:
			if (triple.get_obj().equalsIgnoreCase(query)){
				return true;
			}			 
			break;
		default:
			return true;
		} 
		return false; 
	}
	
	ArrayList<Triple>  retrieveTriples (int type1, int type2, String query1, String query2){

		ArrayList<Triple>  dataset = this.getDataSet();
		ArrayList<Triple>  results =  new ArrayList<Triple>(); 
		
		Iterator<Triple>  myIterator = dataset.iterator(); 
		while (myIterator.hasNext()){
			Triple triple = myIterator.next();
			if (isMatch(triple, type1, query1) && isMatch(triple, type2, query2)){
				results.add(triple);
			}			
		}
		return results;
	}
	
	//unit test
	public static void main(String[] args){
	 
		ArrayList<Triple> rlts = new SimpleQueryEngine().getAllTriples();
		Iterator<Triple> myIterator = rlts.iterator();
		while (myIterator.hasNext()){
			System.out.println(myIterator.next());
		}
	}

	@Override
	public ArrayList<Triple> getAllTriples() {
		return this.retrieveTriples(-1,-1, "", "");
	}

	@Override
	public RDFSubGraphSet getTriplesByCNF(Clause cls) {
		System.out.println(this.getClass().getName() + ": ERROR! This feature is not supported.");
		return null; 	 
	}
}
