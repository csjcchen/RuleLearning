package gilp.rdf;

import java.util.ArrayList;

import gilp.rule.RDFPredicate;

/* A general representation of RDF subgraphs. 
 * This subgraph must be connected.
 * CJC Nov. 13, 2015
 * */
public class RDFSubGraph {
	private ArrayList<Triple> _triples;
	
	public RDFSubGraph(){
		this._triples = new ArrayList<Triple>();
	}
	
	public ArrayList<Triple> getTriples(){
		 return _triples;
	}
	
	//add a triple at the end of this._triples
	public boolean addTriple(Triple t){
		this._triples.add(t);
		return true;
	}
	
	//add a triple at the idx position of this._triples
	public boolean addTriple(Triple t, int idx){
		try{
			this._triples.add(idx, t);
		}
		catch(Exception ex){
			ex.printStackTrace(System.out);
			return false;
		}
		return true;
	}
	
	/*try to match this sub_graph with a triple t 
	 * by join one of this triples with t.
	 * join_tp_idx: specifies which triple in this sug_graph involves in join
	 * join_pos1 is the join position of the triple in this subgraph
	 * : 1 -- subject; 2 -- object;
	 * join_pos2 is the join position of t
	 * */
	public boolean match(Triple t, int join_tp_idx, int join_pos1, int join_pos2){
	 
		String str1 = this._triples.get(join_tp_idx).get_subject();
		String str2 = t.get_subject();
		
		if (join_pos1 != 1)
			str1 = this._triples.get(join_tp_idx).get_obj();
		if (join_pos2 != 1)
			str2 = t.get_obj();
		
		return str1.equals(str2);
	}
	
	@Override
	public RDFSubGraph clone(){
		RDFSubGraph sg = new RDFSubGraph();
		sg._triples.addAll(this._triples);
		return sg;
	}
	
	@Override
	public String toString(){
		if (this._triples.size()==0)
			return "";
		else {
			StringBuffer sb = new StringBuffer();
			sb.append(this._triples.get(0).toString());
			for (int i=1;i<this._triples.size();i++){
				sb.append("--").append(this._triples.get(i));
			}
			return sb.toString();
		}
	}
	
}

