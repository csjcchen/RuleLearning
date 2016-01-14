package gilp.rule;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import deletedCodes.ClauseDBAdaptor;
import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.learning.GILPSettings;
import gilp.rdf.PGEngine;
import gilp.rdf.QueryEngine;
import gilp.rdf.RDF3XEngine;
import gilp.rdf.RDFSubGraph;
import gilp.rdf.RDFSubGraphSet;
import gilp.rdf.SimpleCNFQueryEngine;
import gilp.rdf.Triple;

public class RDFRuleImpl extends Rule {
	
	int _sub_index; 
	int _obj_index;
	
	double _kb_support = -1.0; 
		//since the calculation of support over KB is expensive, and this value will not change
		// we better cache it (-1.0 means it has not been evaluated).
	public RDFRuleImpl(){
		super();
		this._head = null;
		this._body = new ClauseSimpleImpl();
		resetVarIndex();
	}
	
	private void resetVarIndex(){
		this._sub_index = 1;
		this._obj_index = 1;
	}
		
	//@inc_var_idx: true return a new variable and increase the var_idx by 1; 
	//false: only return a new variable. 
	public String getNextSubjectVar(boolean inc_var_idx){
		if (inc_var_idx)//return a new subject variable and increase the var_idx by 1
			return new String("?s" + _sub_index++);
		else//return a new subject variable only
			return new String("?s" + (_sub_index+1));
	}
	
	//return a new subject variable and increase the var_idx by 1
	public String getNextSubjectVar(){
		return getNextSubjectVar(true);
	}
	
	//@inc_var_idx: true return a new variable and increase the var_idx by 1; 
	//false: only return a new variable. 
	public String getNextObjectVar(boolean inc_var_idx){
		if (inc_var_idx)
			return new String("?o" + _obj_index++);
		else
			return new String("?o" + (_obj_index+1));
	}
	
	//return a new object variable and increase the var_idx by 1
	public String getNextObjectVar(){
		return getNextObjectVar(true);
	}	
	 
	
	@Override
	public RDFRuleImpl clone(){
		RDFRuleImpl r = new RDFRuleImpl();
		if(this._body!=null)
			r.set_body(this._body.clone());
		else
			r.set_body(null);

		if(this._head!=null)
			r.set_head(this._head.clone());
		else
			r.set_head(null);
		
		r._obj_index = this._obj_index;
		r._sub_index = this._sub_index;	
		return r;
	}
	
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer(); 
		sb.append(this._body).append("->");		
		sb.append(this._head);
	 
		return sb.toString();
	}
	 
	//normalize this rule by
	//1. order all atoms by the alphabet order of their predicate names
	//2. rename the variables in the order of the container predicates
	//3. remove duplicated atoms
	public void normalize(){
		Iterator<Predicate> myIter = this.get_body().getIterator();
		Predicate[] predicates = new Predicate[this.get_body().getBodyLength()];
		int i = 0;
		while(myIter.hasNext())
			predicates[i++] = myIter.next();
		Arrays.sort(predicates, new PredicateComparator());
		
		HashMap<String, String> hmapNewNames = new HashMap<String,String>();
		HashMap<String, String> hmapPredicates = new HashMap<String, String>();
 		 
		this._body = new ClauseSimpleImpl();
		this.resetVarIndex();
		
		for (i=0;i<predicates.length;i++){
			RDFPredicate p = ((RDFPredicate)predicates[i]);
			renameVar(p,hmapNewNames);	
			if (!hmapPredicates.containsKey(p.toString())){
				this._body.addPredicate(p);
				hmapPredicates.put(p.toString(), "");
			}
		}	
		
		if (this._head!=null){
			RDFPredicate h = (RDFPredicate)this._head;
			renameVar(h, hmapNewNames);			 
		}
	}
	
	//rename the var in a predicate
	//e.g for P(?s2, A), if the var ?s2 has been replaced by ?s1 in  hmapNewNames
	//P(?s2, A) --> P(?s1, A)
	//otherwise, we use a new subject var 
	void renameVar(RDFPredicate p, HashMap<String, String> hmapNewNames){
		if (p.isSubjectVariable()){
			String oldVar = p.getSubject().toString();
			String newVar = hmapNewNames.get(oldVar);
			if (newVar==null){
				p.setSubject(this.getNextSubjectVar());
				newVar = p.getSubject().toString();
				hmapNewNames.put(oldVar, newVar);
			}
			else{
				p.setSubject(new String(newVar));
			}
		}
		
		if (p.isObjectVariable()){
			String oldVar = p.getObject().toString();
			String newVar = hmapNewNames.get(oldVar);
			if (newVar==null){
				p.setObject(this.getNextObjectVar());
				newVar = p.getObject().toString();
				hmapNewNames.put(oldVar, newVar);
			}
			else{
				p.setObject( newVar);
			}
		}
	}
	
	//if X is an argument of tp and X does not appear in this rule, then X is a fresh argument
	public ArrayList<String> findFreshArgument(RDFPredicate tp){
		ArrayList<String> listRlt = new ArrayList<>(); 
		Iterator<Predicate> iterPred = this.get_body().getIterator();
		
		boolean freshSub = true;
		boolean freshObj = true;
		while(iterPred.hasNext()){
			RDFPredicate p = (RDFPredicate) iterPred.next();
			if (p.getSubject().equals(tp.getSubject()))
				freshSub = false;			
			if (p.getObject().equals(tp.getObject()))
				freshObj = false;			
			if (!freshSub && !freshObj)
				break;
		}
		
		RDFPredicate head = (RDFPredicate) this.get_head();
		if (head.getSubject().equals(tp.getSubject()))
			freshSub = false;			
		if (head.getObject().equals(tp.getObject()))
			freshObj = false;
		
		if(freshSub)
			listRlt.add(tp.getSubject());
		if(freshObj)
			listRlt.add(tp.getObject());
		return listRlt;
	}
	
	//get all arguments(variables or constants) contained in this rule
	public ArrayList<String> getArguments(){
		HashMap<String,String> hmapArguments = new HashMap<>();
		ArrayList<String> listArguments = new ArrayList<>();
		Iterator<Predicate> iterPred = this.get_body().getIterator();
		while(iterPred.hasNext()){
			RDFPredicate tp = (RDFPredicate)iterPred.next();
			hmapArguments.put(tp.getSubject(), "");
			hmapArguments.put(tp.getObject(),"");
 		}
		RDFPredicate head = (RDFPredicate) this.get_head();
		hmapArguments.put(head.getSubject(), "");
		hmapArguments.put(head.getObject(),"");
		listArguments.addAll(hmapArguments.keySet());
		return listArguments;
	}
	
	 
	
	//return all variables contained in this rule
	public ArrayList<String> getVariables(){
		return extractElements(1);
	}  
	
	public ArrayList<String> getConstants(){
		return extractElements(0);
	}
	
	//type: 0 - constant, 1 -- variable
	private ArrayList<String> extractElements(int type){
		ArrayList<String> listRlts = new ArrayList<String>();
		if (this.get_head()!=null){
			listRlts.addAll( ((RDFPredicate)this.get_head()).extractElements(type) );			
		}	
		Iterator<Predicate> myIter = this.get_body().getIterator();
		while( myIter.hasNext()){
			RDFPredicate atom = (RDFPredicate)myIter.next();
			ArrayList<String> strs = atom.extractElements(type);
			for (String s: strs){
				if (!listRlts.contains(s))
					listRlts.add(s);
			}
		}		
		return listRlts;
	}
	
	//suppose r:  B -> H
	// this returns B AND H'
	// H' is obtained by removing prefixes from H 
	public Clause getNoprefixCaluse(){
		Clause cls = this.get_body().clone();
		RDFPredicate h = (RDFPredicate)this._head;
		h = h.mapToOriginalPred();
		if (!this.containAtom(h))
			cls.addPredicate(h);
		return cls;			
	}
	
	//if the head contains a non-shared variable, the rule is non-safe
	public boolean isSafe(){
		HashMap<String, String> hmapVars = new HashMap<>(); 
		Iterator<Predicate> iterPreds = this.get_body().getIterator();
		while(iterPreds.hasNext()){
			RDFPredicate tp = (RDFPredicate) iterPreds.next();
			if(tp.isSubjectVariable())
				hmapVars.put(tp.getSubject(), "");
			if(tp.isObjectVariable())
				hmapVars.put(tp.getObject(), "");
		}
		
		if (this._head != null){
			RDFPredicate tp = (RDFPredicate) this._head;
			if (tp.isSubjectVariable() && !hmapVars.containsKey(tp.getSubject()))
				return false;			
			if (tp.isObjectVariable() && !hmapVars.containsKey(tp.getObject()))
				return false;			
		}
		return true;
	}
	 
	//****************************************************************************
		
	//                     UNIT TEST  
	
	//****************************************************************************


	
	
	static void testBasic(){
		RDFRuleImpl r = new RDFRuleImpl();
		RDFPredicate h = new RDFPredicate("?s1","hasGivenName", "?o1");
		h = h.mapToCorrectPred();
		r.set_head(h);
		
		RDFPredicate p = new RDFPredicate("?o1","hasGivenName", "?s1");
		r.get_body().addPredicate(p);
		p = new RDFPredicate("?s1","hasGivenName", "?s1");
		r.get_body().addPredicate(p);
		p = new RDFPredicate("?s2","hasNationality", "China");
		r.get_body().addPredicate(p);
		System.out.println(r);
		r.normalize();
		System.out.println("normalized rule:" + r);
		ArrayList<String> arguments = r.getArguments();
		System.out.println("arguments in rule:");
		for (String arg: arguments){
			System.out.print(arg + " ");
		}
		System.out.println();
		ArrayList<String> vars = r.getVariables();
		System.out.println("variables in rule:");
		for (String v: vars){
			System.out.print(v + " ");
		}
		System.out.println();
		
		RDFPredicate p1 = new RDFPredicate("?s2", "hasGivenName", "?o2");
		ArrayList<String> freshArgs = r.findFreshArgument(p1);
		System.out.println("fresh arguments of " + p1 + " w.r.t this rule:");
		for (String v: freshArgs){
			System.out.print(v + " ");
		}
		System.out.println();
		
	}
	
	public static void main(String[] args){
		testBasic();
	}
 
}

/*
 
	 * suppose r is a rule, let I(r) is the set of distinct triples appearing in the instantiations of r. 
	 * If r.head is null, support(r) = |I(r) /\ F-|/F-, here F- is the set of triples in the negative feedback. 
	 * if r.head is not null,  support(r) = |I(r) /\ F+|/|F+|, here F+ is the set of triples in the positive feedback. 
	 *  
	@Override
	public double calcSupport(ArrayList<Triple> dataset, Feedback fb) {
		SimpleCNFQueryEngine sqe = new SimpleCNFQueryEngine();
		sqe.setDataSet(dataset);
		
		if (this.get_head()==null){
			Clause cls = this._body.clone();
			try{
				//obtain I(r)
				RDFSubGraphSet sg_set = sqe.getTriplesByCNF(cls);	
				if (sg_set==null)
					return 0;
				ArrayList<Triple> covered_triples = extractDistinctTriples(sg_set); 
				if (covered_triples == null)
					return 0; 
				
				//obtain F-
				ArrayList<Triple> negative_triples = new ArrayList<Triple>(); 
				for(Comment cmt: fb.get_comments()){
					if (cmt.get_decision() == false){
						negative_triples.add(cmt.get_triple());
					}
				}
			 
				if (negative_triples.size()==0) 
					return 0;
				
				//common set
				ArrayList<Triple> common_triples = getCommonTriples(covered_triples,negative_triples);
				
				return (1.0 * common_triples.size())/(1.0 * negative_triples.size()); 
			}
			catch(Exception ex){
				ex.printStackTrace(System.out);
				return 0;
			}
		}
		else{
			Clause cls = this._body.clone();
			cls.addPredicate(this._head.clone());
			
			try{
				//obtain I(r)
				RDFSubGraphSet sg_set = sqe.getTriplesByCNF(cls);	
				if (sg_set==null)
					return 0;
				ArrayList<Triple> covered_triples = extractDistinctTriples(sg_set); 
				if (covered_triples == null)
					return 0; 
				
				//obtain F+
				ArrayList<Triple> positive_triples = new ArrayList<Triple>(); 
				for(Comment cmt: fb.get_comments()){
					if (cmt.get_decision() == true){
						positive_triples.add(cmt.get_triple());
					}
				}
				 
				if (positive_triples.size()==0) 
					return 0;
				
				//common set
				ArrayList<Triple> common_triples = getCommonTriples(covered_triples,positive_triples);
				
				return (1.0 * common_triples.size())/(1.0 * positive_triples.size()); 
			}
			catch(Exception ex){
				ex.printStackTrace(System.out);
				return 0;
			}
			
		} 
	}
 * */
