package gilp.rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.rdf.RDFSubGraph;
import gilp.rdf.RDFSubGraphSet;
import gilp.rdf.SimpleCNFQueryEngine;
import gilp.rdf.Triple;
import gilp.learning.GILPSettings;
import gilp.learning.RuleQualityComparator;

public abstract class Rule {
	
	/*
	 * Horn Clause
	 * */
	
	Predicate _head;
	Clause _body;
	double _quality = -1;
	
	public abstract Rule clone();
	 
	public Predicate get_head() {
		return _head;
	}
	public void set_head(Predicate head) {
		this._head = head.clone();
	}
	
	public Clause get_body() {
		return _body;
	}
	public void set_body(Clause _body) {
		this._body = _body.clone();
	}
	
	public boolean isExclusive(){
		if (this._head == null){
			GILPSettings.log(this.getClass().getName() + ": Error! The head of a rule cannot be null.");
			return false;
		}
		return this._head.isIncorrectPredicate();
	}
	
	public boolean isInclusive(){
		if (this._head == null){
			GILPSettings.log(this.getClass().getName() + ": Error! The head of a rule cannot be null.");
			return false;
		}
		return this._head.isCorrectPredicate();
	}
	 
	//check whether the input predicate_name appears in this rule
	public boolean containPredicate(String pred_name){
		if (this._head == null){
			GILPSettings.log(this.getClass().getName() + ": Error! The head of a rule cannot be null.");
			return false;
		}		
		if(this.get_head().getPredicateName().equals(pred_name))
			return true;
		
		Iterator<Predicate> myIter = this.get_body().getIterator();
		while(myIter.hasNext()){
			if(myIter.next().getPredicateName().equals(pred_name))
				return true;
		}		
		return false;
	}
	
	//check whether an atom has already in the body or head of this rule
	public boolean containAtom(Predicate tp){
		if (this._head == null){
			GILPSettings.log(this.getClass().getName() + ": Error! The head of a rule cannot be null.");
			return false;
		}
		if (this._head.equals(tp)) 
			return true;
		Iterator<Predicate> myIter = this._body.getIterator();
		while(myIter.hasNext()){
			if (myIter.next().equals(tp))
				return true;
		}
	 	return false;
	}
	
	//TODO if head cannot be null, how can a rule be empty?
	public boolean isEmpty(){
		if (this._head == null){
			GILPSettings.log(this.getClass().getName() + ": Error! The head of a rule cannot be null.");
			return false;
		}
		if (this._body == null){
			GILPSettings.log(this.getClass().getName() + ": Error! The body of a rule cannot be null.");
			return false;
		}		
		if (this.get_body().getBodyLength()>0)
			return false;		
		return true;	
	}	  
	//TODO need to rethink about this 
	public boolean isTooGeneral(){
		return this.getLength()<GILPSettings.MAXIMUM_RULE_LENGTH;
	}
	
	//TODO need to rethink about this 	
	public boolean isQualified(){
		return this.getLength()>=GILPSettings.MAXIMUM_RULE_LENGTH;
	}
	
	public int getLength(){
		if (this._head == null){
			GILPSettings.log(this.getClass().getName() + ": Error! The head of a rule cannot be null.");
			return 0;
		}
		else{
			int len = 1;//head
			len += this.get_body().getBodyLength();
			return len;			
		}
	}

	@Override
	public boolean equals(Object o){
		if(!this.getClass().isInstance(o))
			return false;
		return 
			this.toString().equals(o.toString());
	}
	
	//suppose r:  B -> H
	// this returns B AND H
	public Clause getCorrespondingClause() {
		Clause cls = null;
		cls = this.get_body().clone();
		cls.addPredicate(this.get_head().clone());
		return cls;
	}
	
	
				
	protected static class PredicateComparator implements Comparator<Predicate>{
		@Override
		public int compare(Predicate p1, Predicate p2) {			
			return p1.getPredicateName().compareTo(p2.getPredicateName());
		}
		
	}
}


/* 

	//decide whether a comment is consistent with the rule
	boolean isConsistent(Comment cmt){
		if (cmt.get_decision() == true && this.isInclusive())
			return true; 
		if (cmt.get_decision() == false && this.isExclusive())
			return true; 
		return false;		
	}
 * */
 