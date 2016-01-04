package gilp.rule;

import java.util.ArrayList;
import java.util.Iterator;

public class PredicateSimpleImpl extends Predicate {
	
	ArrayList<String> _variables;
	String _predicate_name;
	
	public PredicateSimpleImpl(){
		this._variables = new ArrayList<String>();
	}
	
	@Override
	public void setPredicateName(String name) { 
		this._predicate_name = name;
	}
	
	@Override
	public String getPredicateName(){
		return this._predicate_name;
	}
	
	@Override
	public void addVariable(String var) {
		this._variables.add(var);
	} 
	
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer(); 
		sb.append(this._predicate_name + "(");
		Iterator<String> iter = this.getVariableIterator();
		if (iter.hasNext()) 
			sb.append(iter.next());
		while(iter.hasNext())
			sb.append("," + iter.next());
		sb.append(")");
		return sb.toString();
	}

	@Override
	public Iterator<String> getVariableIterator() {
		return this._variables.iterator();
	}
	
	@Override
	public PredicateSimpleImpl clone(){
		PredicateSimpleImpl pr = new PredicateSimpleImpl();
		pr.setPredicateName(this._predicate_name);
		pr._variables.addAll(this._variables);
		return pr;
	}
}
