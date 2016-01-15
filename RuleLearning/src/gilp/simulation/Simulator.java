package gilp.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.rdf.Triple;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.RDFRuleImpl;
import gilp.rule.Rule;
import gilp.learning.GILPSettings;
import gilp.learning.RDFBFSLearner;
import gilp.learning.RuleBaseManager;
import gilp.learning.RulePackage;
import gilp.learning.RulePackageFactory;
import gilp.learning.TripleSelector;

/*
 * This simulator (simplified as SIM in the following) requires a DB and a CB.
 * DB stores all triples, and CB stores all the triples which we have known their correctness. 
 * First, SIM randomly choose a set of triples T from CB.
 * Next, SIM invoke the GILP process
 * CJC Nov. 23, 2015
 * */
public class Simulator {
	
	//remove rules which are already qualified	
	//all qualified rules will be removed form @listCandidates and inserted into @listQualified
	void removeQualified(ArrayList<RulePackage> listCandidates, ArrayList<RulePackage> listQualified){	 
		ArrayList<RulePackage> remained_rules = new ArrayList<>();
		for (RulePackage rp: listCandidates){
			if (rp.getRule().isQualified()){
				listQualified.add(rp);
			}
			else{
				remained_rules.add(rp);
			}
		}
		listCandidates = remained_rules;
	}
	
	ArrayList<RDFRuleImpl> checkConflicts(Rule r){
		RDFRuleImpl r0 = (RDFRuleImpl) r; 
		
		ArrayList<RDFRuleImpl> listConflicts = RuleBaseManager.getConflictRules(r0); 
		ArrayList<RDFRuleImpl> listRemained = new ArrayList<>();
		
		if(listConflicts.size()>0){
			for (RDFRuleImpl r1: listConflicts){
				while(RuleBaseManager.isConflict(r0, r1)){
					if(r1.getLength()>r0.getLength()){
						r0 = RuleBaseManager.refine(r0, null);
					}
					else{
						r1 = RuleBaseManager.refine(r1, null);
					}						
					if (Math.min(r1.getLength(), r0.getLength()) >= GILPSettings.MAXIMUM_RULE_LENGTH)
						break;
				}
				if(RuleBaseManager.isConflict(r0, r1)){
					listRemained.add(r1);
				}
			}
		}
		
		return listRemained;		
	}
 
	void simulate(){
		int num_comments = 5; //number of initial comments 
		int k = 1;//top-k best rules
		
		FeedbackGenerator fb_gen = new FeedbackGenerator(); 
		TripleSelector triple_sel = new TripleSelector();
		
		Feedback fb = fb_gen.getRandomComments(num_comments);
		
		RDFBFSLearner learner = null; 		
		
		ArrayList<RulePackage> candi_rules = new ArrayList<>();  
		
		ArrayList<RulePackage> listQualifiedRules = new ArrayList<> ();
		
		HashMap<String, String> hmapPreRules = new HashMap<String,String>();
		while(true){
			learner = new RDFBFSLearner(fb, k);
			
			for(RulePackage rp: candi_rules){
				//System.out.println(rp.getRule());
				hmapPreRules.put(rp.getRule().toString(), "");
			}
			
			candi_rules = learner.learn_rule(candi_rules);
		
			removeQualified(candi_rules, listQualifiedRules); 
			
			Iterator<RulePackage> ruleIter = candi_rules.iterator();
			
			while(ruleIter.hasNext()){
				RulePackage rp = ruleIter.next();
				if(!rp.getRule().isTooGeneral() || hmapPreRules.containsKey(rp.getRule().toString())){
					System.out.println("this rule cannot be further extended:" + rp.getRule());
					listQualifiedRules.add(rp);
					ruleIter.remove();					
				}
			}
 			
			if (candi_rules.size()==0)
				break;
			
			candi_rules = RulePackageFactory.chooseTopRP(candi_rules , k);
			
			ArrayList<Triple> probing_triples = triple_sel.selectTriples(candi_rules, fb);
			Feedback new_fb = fb_gen.getComments(probing_triples);
			for (Comment cmt: new_fb.get_comments()){
				if (!fb.get_comments().contains(cmt))
					fb.get_comments().add(cmt);
			}
		}
		
		for (RulePackage rp1: listQualifiedRules){				
			rp1.getRule().normalize();				
		}
		
		RulePackageFactory.removeDuplicatedRP(listQualifiedRules);	
		 
		//rank the result rules 
		candi_rules = RulePackageFactory.chooseTopRP(listQualifiedRules, k);
		
		
		
		System.out.println("Rules learned by GILP:");
		for (RulePackage rp: candi_rules){
			System.out.println(rp.getRule());
		}
	}
	
	public static void main(String[] args){
		new Simulator().simulate();
	}

}
