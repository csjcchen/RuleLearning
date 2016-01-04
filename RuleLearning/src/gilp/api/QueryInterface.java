package gilp.api;

import java.util.ArrayList;

import gilp.rdf.Triple;

/*Defines a set of API's to be used by the JSF files*/
public interface QueryInterface {
	ArrayList<Triple> getTriplesBySubject(String sub);
		//retrieve a set of triples whose subjects are all equal to @sub
	
	ArrayList<Triple> getTriplesByPredicate(String pred);
		//retrieve a set of triples whose predicates are all equal to @pred
	
	ArrayList<Triple> getTriplesByObject(String obj);
		//retrieve a set of triples whose objects are all equal to @obj
	
	ArrayList<Triple> getTriplesBySPO(String[] keys, String[] vals);
		//retrieve a set of triples which satisfy a conjunction of <key, value> requirements
		//each element in @keys can only be "s", "p, or "o"
		//the @vals[i] corresponds to @keys[i]
		//Example: getTriplesBySPO([s,p], (Yao_Ming, hasGivenName]) returns all triples whose 
		//subjects equal to "Yao_Ming" and predicates equal to "hasGivenName"
	
	ArrayList<Triple> getTriplesByType(String type);
		//retrieve a set of triples which all belong @type or a subclass of @type	

}
