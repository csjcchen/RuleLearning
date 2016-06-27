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
import gilp.rdf.JoinType;
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
	
	public boolean isConnected(){
		 
		HashMap<String, ArrayList<Predicate>> hmapVar = new HashMap<>(); 
		
		Iterator<Predicate> myIter = this._body.getIterator();
		while(myIter.hasNext()){
			RDFPredicate tp = (RDFPredicate) myIter.next(); 
			if (tp.isSubjectVariable()){
				String var = tp.getSubject();
				ArrayList<Predicate> pres =  new ArrayList<>();				
				if(hmapVar.containsKey(var)){
					pres = hmapVar.get(var); 					
				}
				pres.add(tp);
				hmapVar.put(var,pres);				
			}
			if(tp.isObjectVariable() && !tp.getObject().equals(tp.getSubject())){
				String var = tp.getObject();
				ArrayList<Predicate> pres =  new ArrayList<>();				
				if(hmapVar.containsKey(var)){
					pres = hmapVar.get(var); 					
				}
				pres.add(tp);
				hmapVar.put(var,pres);
			}	
		}
		
		for (String var : hmapVar.keySet()){
			ArrayList<Predicate> pres = hmapVar.get(var); 
			if(pres.size()<2){
				RDFPredicate tp = (RDFPredicate) pres.get(0);
				if(tp.getSubject().equals(var)){
					if(!tp.isObjectVariable()) 
						return false;
					else{
						String v2 = tp.getObject();
						if(hmapVar.get(v2).size()<2){
							return false;
						}
					}					
				}
				else{
					if(!tp.isSubjectVariable()) 
						return false;
					else{
						String v2 = tp.getSubject();
						if(hmapVar.get(v2).size()<2){
							return false;
						}
					}
				}				
			}
		}
		
		return true;
	}
	
	//example
	//@r: hasGivenName(abc, ?o1) and hasGivenName(?s1, ?o1) and rdfType(?s1, person) -> incorrect_hasGivenName(?s1, ?o1)
	//@return: 
	//	hasGivenName(abc, ?o1)--> hasGivenName_1
	//	hasGivenName(?s1, ?o1) --> hasGivenName_2
	//	rdfType(?s1, person) --> rdfType_1
	//  incorrect_hasGivenName(?s1, ?o1)-->hasGivenName_2

	public HashMap<String, String> getRenamedPredicates(){
		HashMap<String, String> hmapRlts = new HashMap<>(); 
		
		HashMap<String, Integer> hmap_preds = new HashMap<>(); 
		//ArrayList<String> listPredicates = new ArrayList<String>(); 
		
		Iterator<Predicate> tpIterator = this._body.getIterator();
		RDFPredicate head = (RDFPredicate)this.get_head().clone();
		head = head.mapToOriginalPred(); 
		
		while(tpIterator.hasNext()){
			RDFPredicate tp = (RDFPredicate)tpIterator.next();
			
			String prop_name = tp.getPredicateName();
			
			if(!hmap_preds.containsKey(tp.getPredicateName())){
				hmap_preds.put(prop_name, 1);
				prop_name += "_1";				
			}
			else{
				int num = hmap_preds.get(prop_name);
				num += 1; 
				hmap_preds.put(prop_name, num);
				prop_name += ("_" + num);
			}
			hmapRlts.put(tp.toString(), prop_name);
			if(tp.equals(head)){
				hmapRlts.put(((RDFPredicate)this.get_head().clone()).toString(), prop_name); 
			}
		}
		
		return hmapRlts;
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
	
	//if tp1 is more general than tp2, we then remove tp1 from this rule
	//current implementation only considers numerical properties
	public void removeSubsumedPredicates(){
		Iterator<Predicate> iterPred = this._body.getIterator();
		Iterator<Predicate> iterPred2 = this._body.getIterator();	
		ArrayList<Predicate> listPredicates = new ArrayList<>();
		
		while(iterPred.hasNext()){
			listPredicates.add(iterPred.next());
		}
		
		ArrayList<Predicate> subsumedPredicates = new ArrayList<>();
		for (int i=0;i<listPredicates.size();i++){
			RDFPredicate tp1 =(RDFPredicate) listPredicates.get(i);
			for (int j=i+1;j<listPredicates.size();j++){
				RDFPredicate tp2 =(RDFPredicate) listPredicates.get(j);
				int rel = tp1.compareGeneralization(tp2);
				if (rel >0 ){
					if (!subsumedPredicates.contains(tp1))
						subsumedPredicates.add(tp1);
				}
				else if(rel<0){
					if (!subsumedPredicates.contains(tp2))
						subsumedPredicates.add(tp2);
				}
			}
		}
		
		for (Predicate p: subsumedPredicates){
			this._body.removePredicate(p);
		}
	}
	
	//check whether a comment is covered by this rule
	//return 1: true positive; -1: false positive; 0: not covered
	public int coversComment(Comment cmt){
		Triple t = cmt.get_triple();
		PGEngine qe = new PGEngine();
		if (qe.isTripleCovered(t, this)){
			if(cmt.get_decision() == this.isInclusive()){
				return 1;
			}
			else
				return -1;
		}
		return 0;
	}
	 
	//normalize this rule by
	//1. order all atoms by the alphabet order of their predicate names
	//2. rename the variables in the order of the container predicates
	//3. remove duplicated atoms
	public void normalize(){
		//this.removeSubsumedPredicates();
		
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
		
		HashMap<String,String> hmapArgs = new HashMap<>();
		boolean freshSub = true;
		boolean freshObj = true;
		while(iterPred.hasNext()){
			RDFPredicate p = (RDFPredicate) iterPred.next();
			if(!hmapArgs.containsKey(p.getSubject())){
				hmapArgs.put(p.getSubject(), "");
			}
			if(!hmapArgs.containsKey(p.getObject())){
				hmapArgs.put(p.getObject(),"");
			}
		}		
		RDFPredicate head = (RDFPredicate) this.get_head();
		if(!hmapArgs.containsKey(head.getSubject())){
			hmapArgs.put(head.getSubject(), "");
		}
		if(!hmapArgs.containsKey(head.getObject())){
			hmapArgs.put(head.getObject(),"");
		}
		
		if(hmapArgs.containsKey(tp.getSubject())){
			freshSub = false;
		}
		if(hmapArgs.containsKey(tp.getObject())){
			freshObj = false;
		}		
		
		if(freshSub)
			listRlt.add(tp.getSubject());
		if(freshObj)
			listRlt.add(tp.getObject());
		return listRlt;
	}
	
	//find joinType between this rule and a predicate	
	public JoinType[] findJoinType(RDFPredicate tp){
		Iterator<Predicate> myIter = this._body.getIterator();
		while(myIter.hasNext()){
			RDFPredicate p =(RDFPredicate) myIter.next();
			JoinType[] jts = RDFPredicate.getJoinTypes(p, tp);
			if(jts!=null)
				return jts;
		}
		return null;
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
	
	//check whether this rules is a specialization of @r
	public boolean isSpecialization(RDFRuleImpl r){
		//idea: first normalize the rules and then compare by string
		
		RDFRuleImpl r1 = this.clone(); 
		r1.normalize();
		RDFRuleImpl r2 = r.clone();
		r2.normalize();
		
		if (!r1.get_head().toString().equals(r2.get_head().toString())){
			return false;
		}
		
		if (r1.get_body().toString().indexOf(r2.get_body().toString())>=0)
			return true;
		else
			return false;
	}
	
	public boolean isGeneralization(RDFRuleImpl r){
		return r.isSpecialization(this);
	}
	 
	//****************************************************************************
		
	//                     UNIT TEST  
	
	//****************************************************************************

	static void testRemoveSubsumed(){
		RDFRuleImpl r = new RDFRuleImpl();
		
		RDFPredicate h = new RDFPredicate("?s1","hasGivenName", "?o1");
		h = h.mapToCorrectPred();
		r.set_head(h);
		
		RDFPredicate p = new RDFPredicate("?s2","hasLength", "?o1");
		r.get_body().addPredicate(p);
		p = new RDFPredicate("?s1","hasLength", "[1,4]");
		r.get_body().addPredicate(p);
		p = new RDFPredicate("?s1","hasNationality", "(0,10)");
		r.get_body().addPredicate(p);
		
		System.out.println(r);
		System.out.println("after removing subsumed predicates:");
		r.removeSubsumedPredicates();
		System.out.println(r);
	}
	
	static void testSpecialization(){
		RDFRuleImpl r1 = new RDFRuleImpl();
		RDFPredicate h = new RDFPredicate("?s1","hasGivenName", "?o1");
		h = h.mapToCorrectPred();
		r1.set_head(h);
		
		RDFPredicate p = new RDFPredicate("?o1","hasGivenName", "?s1");
		r1.get_body().addPredicate(p);
		p = new RDFPredicate("?s1","hasGivenName", "?s1");
		r1.get_body().addPredicate(p);
		p = new RDFPredicate("?s2","hasNationality", "China");
		r1.get_body().addPredicate(p);
		
		RDFRuleImpl r2 = r1.clone(); 
		//RDFPredicate h2 = new RDFPredicate("?s1","hasGivenName", "?o1");
		//h2 = h2.mapToIncorrectPred();
		//r2.set_head(h2);
		
		r2.get_body().addPredicate(new RDFPredicate("?o1","rdfType", "person"));
		
		System.out.print(r1.isGeneralization(r2));
	}

	
	
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
		testSpecialization();
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
