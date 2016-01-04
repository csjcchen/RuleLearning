package deletedCodes;

import java.util.ArrayList;

import gilp.rdf.RDFSubGraphSet;
import gilp.rdf.Triple;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.RDFPredicate;

public class ClauseDBAdaptor {
	
	private Clause _cls; 
	private String _table; 
	
	public ClauseDBAdaptor(Clause cls, String tab){
		this._cls = cls;
		this._table = tab;
	}
	
	public ClauseDBAdaptor(RDFPredicate tp, String tab){
		this._cls = new ClauseSimpleImpl();
		this._cls.addPredicate(tp);
		this._table = tab;
	}
	
	public boolean createTable(){
		//TODO 
		return false;
	}
	
	public boolean insertSingleTuple(ArrayList<Triple> triples){
		//TODO
		return false;
	}
	
	public boolean dropTable(){
		//TODO
		return false;
	}
	
	public boolean insertTuples(RDFSubGraphSet sg_set){
		//TODO
		return false;
	}
	
	public RDFSubGraphSet loadTuples(){
		//TODO
		return null;
	}
	
	public String getAttrName(int pred_idx){
		//TODO
		return null;
	}
	
	public String getAttrName(RDFPredicate tp){
		//TODO
		return null;
	}
	
	public String getTableName(){
		return this._table;
	}

}
