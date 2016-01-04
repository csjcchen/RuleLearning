package gilp.api;

import java.util.ArrayList;

import gilp.rdf.Triple;
import gilp.rdf.SimpleQueryEngine;

public class SimpleQI implements QueryInterface {
	
	SimpleQueryEngine _sqe = null;
	ArrayList<Triple> _test_data = null;

	public SimpleQI(){
		this._sqe = new SimpleQueryEngine(); 
	}
	
	
	
	@Override
	public ArrayList<Triple> getTriplesBySubject(String sub) {
		return this._sqe.getTriplesBySubject(sub);
	}

	@Override
	public ArrayList<Triple> getTriplesByPredicate(String pred) {
		return this._sqe.getTriplesByPredicate(pred);
	}

	@Override
	public ArrayList<Triple> getTriplesByObject(String obj) {
		return this._sqe.getTriplesByObject(obj);
	}

	@Override
	public ArrayList<Triple> getTriplesBySPO(String[] keys, String[] vals) {
		System.err.println(this.getClass().getName() + "ERROR: This feature is not implemented yet.");
		return null;
	}

	@Override
	public ArrayList<Triple> getTriplesByType(String type) {
		System.err.println(this.getClass().getName() + "ERROR: This feature is not implemented yet.");
		return null;
	}
	
	public static void main(String[] args){
		QueryInterface qi = new SimpleQI();
		ArrayList<Triple> listRlts = qi.getTriplesByPredicate("hasGivenName"); 
		for(Triple t: listRlts){
			System.out.println(t);
		}
	}

}
