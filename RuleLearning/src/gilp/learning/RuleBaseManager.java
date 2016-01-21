package gilp.learning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import gilp.feedback.Feedback;
import gilp.rdf.PGEngine;
import gilp.rdf.RDFSubGraphSet;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.Predicate;
import gilp.rule.RDFPredicate;
import gilp.rule.RDFRuleImpl;


public class RuleBaseManager {
	static ArrayList<RDFRuleImpl> _rule_set; 
	
	
	static void init(){
		_rule_set = new ArrayList<>();
	}
	
	static boolean alignTwoRulesOnHead(RDFRuleImpl r1, RDFRuleImpl r2){
		RDFPredicate h1 = (RDFPredicate)r1.get_head();
		RDFPredicate h2 = (RDFPredicate)r2.get_head();
		HashMap<String, String> hmapH1Substitution = new HashMap<>(); 
		HashMap<String, String> hmapH2Substitution = new HashMap<>(); 		
		
		if (h1.isSubjectVariable() && h2.isSubjectVariable()){
			hmapH1Substitution.put(h1.getSubject(), h2.getSubject());
				//replace subject(var) of h1 as the subject(var) of h2
		}
		else if (h1.isSubjectVariable()){
			hmapH1Substitution.put(h1.getSubject(), h2.getSubject());
				//replace subject(var) of h1 as the subject(constant) of h2
		}
		else if (h2.isSubjectVariable()){
			hmapH2Substitution.put(h2.getSubject(), h1.getSubject());
				//replace subject(var) of h2 as the subject(constant) of h1
		}
		else{
			if (!h1.getSubject().equals(h2.getSubject()))
				return false;
			//both are constants and do not equal, cannot align
		}
		
		if (h1.isObjectVariable() && h2.isObjectVariable()){
			hmapH1Substitution.put(h1.getObject(), h2.getObject());
				//replace Object(var) of h1 as the Object(var) of h2
		}
		else if (h1.isObjectVariable()){
			hmapH1Substitution.put(h1.getObject(), h2.getObject());
				//replace Object(var) of h1 as the Object(constant) of h2
		}
		else if (h2.isObjectVariable()){
			hmapH2Substitution.put(h2.getObject(), h1.getObject());
				//replace Object(var) of h2 as the Object(constant) of h1
		}
		else{
			if (!h1.getObject().equals(h2.getObject()))
				return false;
			//both are constants and do not equal, cannot align
		}
		
		r1 = appySubstituions(r1, hmapH1Substitution);
		r2 = appySubstituions(r2, hmapH2Substitution);
		
		return true;
	}
	
	static RDFRuleImpl appySubstituions(RDFRuleImpl r, HashMap<String, String> hmapSubstitutions){
		String newVar = null;
		Iterator<Predicate> preds = r.get_body().getIterator();
		while(preds.hasNext()){
			RDFPredicate tp = (RDFPredicate) preds.next();
			newVar = hmapSubstitutions.get(tp.getSubject());
			if(newVar != null)
				tp.setSubject(newVar);			
			newVar = hmapSubstitutions.get(tp.getObject());
			if(newVar!=null)
				tp.setObject(newVar);
		}
		RDFPredicate head =(RDFPredicate) r.get_head();
		newVar = hmapSubstitutions.get(head.getSubject());
		if(newVar!=null)
			head.setSubject(newVar);
		newVar = hmapSubstitutions.get(head.getObject());
		if(newVar!=null)
			head.setObject(newVar);
		
		
		return r;
	}
	
	public static void addRule(RDFRuleImpl r){
		if (_rule_set == null)
			init();
		_rule_set.add(r);
	}
	
	public static boolean isConflict(RDFRuleImpl r1, RDFRuleImpl r2){
		//logical check: the heads of r1 and r2 are opposite and have the same property
		RDFPredicate h1 = (RDFPredicate)r1.get_head();
		RDFPredicate h2 = (RDFPredicate)r2.get_head();
		if(r1.isExclusive() && r2.isExclusive())
			return false;
		
		if (r1.isInclusive() && r2.isInclusive())
			return false;

		if (!h1.mapToOriginalPred().getPredicateName().equals(h2.mapToOriginalPred().getPredicateName()))
			return false;		
		
		//instances check: align two rules w.r.t the heads, and conduct a query r1.Body And r2.Body on the KB		
		RDFRuleImpl r1Copy, r2Copy;
		r1Copy = r1.clone();
		r2Copy = r2.clone();
		if (!alignTwoRulesOnHead(r1Copy, r2Copy))
			return false;
		
		Clause cls = r1Copy.get_body().clone();
		Iterator<Predicate> preds = r2Copy.get_body().getIterator();
		while(preds.hasNext())
			cls.addPredicate(preds.next());
		
		PGEngine qe = new PGEngine();
		RDFSubGraphSet sg_set = qe.getTriplesByCNF(cls, 1);
		
		if(sg_set == null)
			return false;
		
		if(sg_set.getSubGraphs().size()>0)
			return true;
		else		
			return false;
	}
	
	public static ArrayList<RDFRuleImpl> getConflictRules(RDFRuleImpl r){
		ArrayList<RDFRuleImpl> listRlts = new ArrayList<>(); 
		if (_rule_set == null){
			init();
			return listRlts;
		}
			
		for (RDFRuleImpl r1: _rule_set){
			if(isConflict(r1,r)){
				listRlts.add(r1.clone());
			}
		}
		
		return listRlts;
	}
	
	
	public static RDFRuleImpl refine(RDFRuleImpl r, Feedback fb){
		RDFBFSLearner learner = new RDFBFSLearner(fb, 1);
		RulePackage rp = new RulePackage(r, fb, null); 
		ArrayList<RulePackage> listRPs = new ArrayList<>();
		
		ArrayList<RulePackage> refinedRules = learner.learn_rule(listRPs);
		
		return refinedRules.get(0).getRule();
	}
	
	//###############################################################################

	//                   unit  tests
		
	//###############################################################################	
	
	static void testConflict(){		 
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o"));
		
		RDFRuleImpl r1 = new RDFRuleImpl();
		r1.get_body().addPredicate(tp.clone());
		
		tp = tp.mapToCorrectPred();
		r1.set_head(tp);
		
		tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("wasBornIn");
		tp.setObject(new String("Shanghai"));		
		r1.get_body().addPredicate(tp.clone());
		
		
				
		tp = new RDFPredicate();
		tp.setSubject(new String("?s1"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o1"));
		
		RDFRuleImpl r2 = new RDFRuleImpl();
		tp = tp.mapToIncorrectPred();
		r2.set_head(tp);
		
		tp = new RDFPredicate();
		tp.setSubject(new String("?s1"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o1"));		
		r2.get_body().addPredicate(tp.clone());
		
		tp = new RDFPredicate();
		tp.setSubject(new String("?s1"));
		tp.setPredicateName("isCitizenOf");
		tp.setObject(new String("China"));		
		r2.get_body().addPredicate(tp.clone());
		
		if(RuleBaseManager.isConflict(r1, r2)){
			System.out.println(r1 + " is conflicit with " + r2 );
		} 
		else{
			System.out.println(r1 + " is not conflicit with " + r2 );
		}
		
	}
	
	public static void main(String[] args){
		testConflict();	
	}
	
}
