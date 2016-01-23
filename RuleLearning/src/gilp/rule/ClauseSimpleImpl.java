package gilp.rule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
 * a very simple implementation of clause, just to store predicates and show them. 
 * CJC 2015.10.29
 * */

public class ClauseSimpleImpl implements Clause {
	
	private ArrayList<Predicate> _predicates;
	
	public ClauseSimpleImpl(){
		_predicates = new ArrayList<Predicate>();
	}

	@Override
	public void addPredicate(Predicate p) {
		this._predicates.add(p);
	}

	@Override
	public Iterator<Predicate> getIterator() {
		return this._predicates.iterator();
	}
 
	
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		Iterator<Predicate> iter = this.getIterator();
		if(iter.hasNext()) 
			sb.append(iter.next());
		while(iter.hasNext()){
			sb.append("," + iter.next());
		}
		return sb.toString();
	}

	@Override
	public int getBodyLength() {
		return this._predicates.size();
	}
	
	@Override
	public ClauseSimpleImpl clone(){
		ClauseSimpleImpl cls = new ClauseSimpleImpl();
		cls._predicates = new ArrayList<Predicate>();
		for (Predicate tp: this._predicates){
			cls._predicates.add(tp.clone());
		}
		return cls;
	}

	@Override
	public boolean removePredicate(Predicate p) {
		Predicate foundItem = null;
		for (Predicate child: this._predicates){
			if (child.equals(p)){
				foundItem = child;
				break;
			}
		}
		if(foundItem == null)
			return false;
		else{
			this._predicates.remove(foundItem);
			return true;
		}
	}

}
