package gilp.rule;

import java.util.ArrayList;
import java.util.Iterator;

import gilp.rdf.JoinType;

 

public abstract class Predicate {
	//special predicates
	public static final String CORRECT_PREDICATE = "correct";
	public static final String INCORRECT_PREDICATE = "incorrect";
	public static final String LESS_THAN_PREDICATE = "lessThan";
	
	
	public abstract void setPredicateName(String name);
	public abstract String getPredicateName();
	public abstract void addVariable(String var);
	public abstract Iterator<String> getVariableIterator();
	public abstract Predicate clone();
	
	 
	public boolean isSpecialPredicate(){
		String tp_name = this.getPredicateName();
		if (tp_name.startsWith(CORRECT_PREDICATE))
			return true;
		if (tp_name.startsWith(INCORRECT_PREDICATE))
			return true;
		if (tp_name.startsWith(LESS_THAN_PREDICATE))
			return true;		 
		
		return false;
	}
	
	public boolean isCorrectPredicate(){
		return this.getPredicateName().startsWith(CORRECT_PREDICATE);
	}
	
	public boolean isIncorrectPredicate(){
		return this.getPredicateName().startsWith(INCORRECT_PREDICATE);
	}
	
	
	
}
