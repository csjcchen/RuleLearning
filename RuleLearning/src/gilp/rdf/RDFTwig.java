package gilp.rdf;

import java.util.ArrayList;

import gilp.rule.RDFPredicate;

/* A simple implementation of RDF subgraph. 
 * Only support at most a subgraph containing at most two edges (RDFPredicates). 
 * CJC Nov. 13, 2015
 * */
public class RDFTwig extends RDFSubGraph {
	
	private ArrayList<Triple> _triples;
	
	public RDFTwig(){
		_triples = new ArrayList<Triple> ();
	}
	
	//the constructor for a twig with two edges
	public RDFTwig(Triple t1, Triple t2){
		this();
		_triples.add(t1);
		_triples.add(t2);
	}
	
	@Override
	public boolean addTriple(Triple t1){
		if (_triples.size()<2){
			_triples.add(t1);
			return true;
		}	
		else{
			System.out.println("ERROR: RDFTwig can contain at most two triples.");
			return true;
		}
	}
	
	 
	@Override
	public boolean addTriple(Triple t, int idx) {
		if (idx>1){
			System.out.println("ERROR: RDFTwig can contain at most two triples.");
			return true;
		}
		return super.addTriple(t, idx);
	}

	@Override
	public ArrayList<Triple> getTriples() {	 
		return _triples;
	}
	
	@Override
	public String toString(){	
		if (this._triples.size()==1)
			return(_triples.get(0).toString());
		else if (this._triples.size()==2){
			return (_triples.get(0).toString() + "--" + _triples.get(1).toString());
		}
		else
			return "Invalid Data";
	
	}
 
}
