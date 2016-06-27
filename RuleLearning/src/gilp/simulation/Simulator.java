package gilp.simulation;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.rdf.PGEngine;
import gilp.rdf.Triple;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.RDFPredicate;
import gilp.rule.RDFRuleImpl;
import gilp.rule.Rule;
import gilp.learning.ExpRulePackage;
import gilp.learning.FeatureConstructor;
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
	
	private  RDFPredicate parsePredicate(String str_pred){
		RDFPredicate tp = new RDFPredicate();
		String pred_name = str_pred.substring(0, str_pred.indexOf("("));
		tp.setPredicateName(pred_name);
		String str_var = str_pred.substring(str_pred.indexOf("(")+1, str_pred.indexOf(","));
		tp.setSubject(str_var.trim());
		str_var = str_pred.substring(str_pred.indexOf(",")+1, str_pred.indexOf(")"));
		tp.setObject(str_var.trim());
		return tp;
	}
	
	 ArrayList<RDFRuleImpl> parseRules(String fileName){
		//example input:
		//hasGivenName(?s1,?o1),rdfType(?s1,Chinese)->correct_hasGivenName(?s1,?o1)
		RandomAccessFile file = null; 
		ArrayList<RDFRuleImpl> listRlts = new ArrayList<>();
		try{
			file = new RandomAccessFile(fileName,"r"); 
			String line = "";
			String str_pred = "";
			
			while((line = file.readLine())!=null){
				RDFRuleImpl r = new RDFRuleImpl();
				while(line.indexOf("),")>=0){
					str_pred = line.substring(0, line.indexOf("),")+1); 
					
					if (str_pred.length()>0){
						r.get_body().addPredicate(parsePredicate(str_pred));
					}
					line = line.substring(line.indexOf("),")+2);
				}
				
				str_pred = line.substring(0, line.indexOf("->"));
				if (str_pred.length()>0){
					r.get_body().addPredicate(parsePredicate(str_pred));
				}
				line = line.substring(line.indexOf("->")+2);
				
				if(line.length()>1){
					r.set_head(parsePredicate(line));
				}
				listRlts.add(r);
			}
			file.close();
		}
		catch (Exception ex){
			ex.printStackTrace(System.out);
		}
		return listRlts;
	}
	
	void offlineAnalysis( ){
		ArrayList<RDFRuleImpl> rules = parseRules("/home/jchen/gilp/example-rules");
		
		//suppose rules[0] is the parent
		RDFRuleImpl r0 = rules.get(0);
		System.out.println(r0);
		PGEngine pg = new PGEngine();
		for(int i=1;i<rules.size();i++){
			RDFRuleImpl r = rules.get(i);
			System.out.println(r);
			double p1 = pg.getHCContainedPr(r0, r);
			System.out.println("Pr(r0 in r):" + p1);
		}
	}
	
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
	
	void simulate_twoPhases(){
		int num_comments = 5; //number of initial comments 
		int k = 1;//top-k best rules
		
		//FeedbackGenerator fb_gen = new FeedbackGenerator(); 
		FBGeneratorFromFacts fb_gen = new FBGeneratorFromFacts();
		 	
		Feedback fb = fb_gen.getRandomComments(num_comments);

		Feedback initial_fb = new Feedback(); 
		initial_fb.set_comments(fb.get_comments()); 
		
		RDFBFSLearner learner = null; 		
		
		ArrayList<RulePackage> candi_rules = new ArrayList<>();  
		ArrayList<RulePackage> accepted_rules = new ArrayList<>(); 
		
		learner = new RDFBFSLearner(fb, k);
		long time0 = System.currentTimeMillis(); 
		candi_rules = learner.learn_rule(candi_rules);
		long time1 = System.currentTimeMillis();
		
		System.out.println("time cost in phase one:" + (time1-time0));
		time0 = time1; 
		
		TripleSelector triple_sel = new TripleSelector();
		
		//1st phase: expand rule
		int len = 1;
		int num_verified_rules = 0;
		
		while (len <= GILPSettings.MAXIMUM_RULE_LENGTH){
			ArrayList<RulePackage> working_list = chooseRPByLength(candi_rules, len);
				//all rules in candi_rules whose length equal to len
			
			for (RulePackage rp : working_list){
				num_verified_rules++;
				int c = triple_sel.verifyRule(rp);
					//verify this rule and store the obtained feedbacks
	
				//we do not need to expand a rule if it can be accepted
				if (c<=0 && len < GILPSettings.MAXIMUM_RULE_LENGTH){
					//get rp's child and put into candi_list		
					FeatureConstructor f_c = new FeatureConstructor(rp, initial_fb, rp.getP0()); 
					ArrayList<ExpRulePackage> expanded_rules = f_c.constructFeatures();
					
					PGEngine pg = new PGEngine();
					GILPSettings.log("childrenRules:" + rp.getRule().hashCode() + ":# of children:" +  expanded_rules.size() + ":" + rp.getRule());
					//each child will inherit the feedbacks of its parent	
					int num = 0;
					for (ExpRulePackage exRP: expanded_rules){
						RDFRuleImpl child_r = exRP.getRule().clone();
						
						/*double pr = pg.getHCContainedPr(rp.getRule(), child_r); 
						if(pr>GILPSettings.SAME_HC_THRESHOLD){
							//if r1 is child of r0, and HC(r0)/HC(r1) > SAME_HC_THRESHOLD, we prune r1 
							continue;
						}*/
						
						num++;
						child_r.normalize();
						RulePackage child_rp = new RulePackage(child_r, rp.getFeedback(), rp);
						
						/*ArrayList<RDFPredicate> atoms  = rp.getQualifiedAtoms();						 
						for (RDFPredicate quali_tp: atoms){
							child_rp.addQualifiedAtom(quali_tp);
						}*/
						
						child_rp.setExtended(true);
						child_rp.setP0(rp.getP0());
						candi_rules.add(child_rp);
					}

					GILPSettings.log("childrenRules:" + rp.getRule().hashCode() + ":# of qualified children:" +  num + ":" + rp.getRule());
				}
				else if(c>0){
					accepted_rules.add(rp);
				}
			}
			len++;
		}
		time1 = System.currentTimeMillis();
		System.out.println("time cost in phase two:" + (time1-time0));
		
		GILPSettings.log("# of verified rules:" + num_verified_rules);
		
		if (accepted_rules.size()>0){
			System.out.print("accepted rules:");
			for (RulePackage rp: accepted_rules){				
				System.out.println(rp.getRule());
				GILPSettings.log(rp.getRule().toString());
				rp.calcPN_Hats(); 
				double prec = rp.getPHat()/ (rp.getPHat() + rp.getNHat()); 
				
				double[] wi = TripleSelector.calcWilsonInterval(prec, rp.getPHat() + rp.getNHat());
				System.out.println("pHat, nHat:" + rp.getPHat() + "," + rp.getNHat());
				System.out.println("Wilson Interval:[" + wi[0] + ","  + wi[1] + "]");
				GILPSettings.log("pHat, nHat:" + rp.getPHat() + "," + rp.getNHat());
				GILPSettings.log("Wilson Interval:[" + wi[0] + ","  + wi[1] + "]");
				
			}	
		}
		
	}
	
	ArrayList<RulePackage> chooseRPByLength(ArrayList<RulePackage> origin_list, int len){
		ArrayList<RulePackage> listRlts = new ArrayList<>();
		for (RulePackage rp: origin_list){
			if(rp.getRule().get_body().getBodyLength() == len){
				listRlts.add(rp);
			}
		}
		return listRlts;
	}
 
	void simulate(){
		int num_comments = 5; //number of initial comments 
		int k = 1;//top-k best rules
		
		//FeedbackGenerator fb_gen = new FeedbackGenerator(); 
		FBGeneratorFromFacts fb_gen = new FBGeneratorFromFacts();
		
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
			
			ArrayList<Triple> probing_triples = triple_sel.selectTriples(candi_rules);
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
		//new Simulator().offlineAnalysis();
		new Simulator().simulate_twoPhases();
	}

}
