package gilp.rdf;

import java.util.ArrayList;

import gilp.rule.Clause;

public interface QueryEngine {
	
	public Triple getTripleByTid (String tid);
	
	public ArrayList<Triple>  getTriplesBySubject (String subject);
	
	public ArrayList<Triple>  getTriplesByPredicate (String predicate);
	
	public ArrayList<Triple>  getAllTriples ();
	
	public RDFSubGraphSet getTriplesByCNF (Clause cls);
	
	 
}
