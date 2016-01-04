package gilp.rdf;

import java.util.ArrayList;

import gilp.rule.RDFPredicate;

/* A simple implementation of RDF subgraph set. 
 * Only support a subgraph containing at most two edges (RDFPredicates). 
 * CJC Nov. 13, 2015
 * */
public class RDFTwigSet extends RDFSubGraphSet{
	private RDFPredicate _p1, _p2;
	
	private ArrayList<RDFSubGraph> _twigs; 
	
	public RDFTwigSet(RDFPredicate p1, RDFPredicate p2){
		this._p1 = p1;
		this._p2 = p2;
		_twigs = new ArrayList<RDFSubGraph>();
	}
	
	public void addTwig(RDFTwig twig){
		this._twigs.add(twig);
	}
	public void addTwig(Triple t1, Triple t2){
		RDFTwig twig = new RDFTwig(t1, t2);
		addTwig(twig);
	}
	
	@Override
	public ArrayList<RDFPredicate> getPredicates() {
		ArrayList<RDFPredicate> listRlts = new ArrayList<RDFPredicate>();
		listRlts.add(_p1);
		listRlts.add(_p2);
		return listRlts;
	}

	@Override
	//@return:
	//0 not connected; 1 s-s; 2 s-o; 3 o-s; 4 o-o 
	public int getJoinRelation(int triple_idx_1, int triple_idx_2) {
		if (triple_idx_1!=0 || triple_idx_2!=1)
			return 0;
		if (_p1.getSubject().equals(_p2.getSubject()))
			return 1;
		else if (_p1.getSubject().equals(_p2.getObject()))
			return 2;
		else if (_p1.getObject().equals(_p2.getSubject()))
			return 3;
		else if (_p1.getObject().equals(_p2.getObject()))
			return 4;
		else
			return 0;		 
	}

	@Override
	public ArrayList<RDFSubGraph> getSubGraphs() {
		return _twigs;
	}

}
