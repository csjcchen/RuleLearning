package gilp.learning;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.StringTokenizer;

import deletedCodes.ExtendedFeature;
import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.rdf.CompareOperator;
import gilp.rdf.JoinType;
import gilp.rdf.PGEngine;
import gilp.rdf.RDF3XEngine;
import gilp.rdf.RDFFilter;
import gilp.rdf.RDFSubGraph;
import gilp.rdf.RDFSubGraphSet;
import gilp.rdf.SimpleCNFQueryEngine;
import gilp.rdf.SimpleQueryEngine;
import gilp.rdf.Triple;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.Predicate;
import gilp.rule.RDFPredicate;
import gilp.rule.RDFRuleImpl;
import gilp.rule.Rule;
import gilp.utility.KVPair;


/*
 * construct features 
 * CJC Nov. 26, 2015
 * */

public class FeatureConstructor {
	
	private int _k; 
	private RulePackage _baseRP; 
	private double _P;//the number of consistent triples w.r.t _ro and _fb
	private double _N;//the number of inconsistent triples w.r.t _ro and _fb	
	
	public FeatureConstructor(RulePackage rp, int k){
		this._k = k;	
		this._baseRP = rp;
		this._P = this._baseRP.getPHat(); 
		this._N = this._baseRP.getNHat();
	}
	 
	/*
	 * From the DB, search all predicates which can be joined with the input
	 * rule, and extract frequent patterns from these joined sub-graphs.
	 */
	public ArrayList<ExpRulePackage> constructFeatures() {
		ArrayList<String> pred_names = GILPSettings.getAllPredicateNames();
		RDFRuleImpl r0 = this._baseRP.getRule();	
		ArrayList<String> args = r0.getArguments();
		
		double tau = GILPSettings.MINIMUM_FOIL_GAIN;			 
		PriorityQueue<ExpRulePackage> candidates = new PriorityQueue<ExpRulePackage>(10 * this._k, new RuleQualityComparator());		
		ArrayList<ExpRulePackage> listRlts = new ArrayList<ExpRulePackage>(); 
		for (String U: args){
			//for (String V: args){					
				//TODO: construct a new extended feature as R(U,V) and add it to the candidates
				//Now, just ignore this!!!			
			//}			
			for (String pr_name : pred_names){		
				//TODO special logic for rdftype 
				
				//introduce a new variable in the subject
				String var =  r0.getNextSubjectVar(false);
				RDFPredicate tp = new RDFPredicate(); 
				tp.setPredicateName(pr_name);
				tp.setSubject(var);
				tp.setObject(U);//object is shared with r0					
				tau = expand(tp, candidates, tau); 
				
				//introduce a new variable in the object
				var =  r0.getNextObjectVar(false);
				tp = new RDFPredicate(); 
				tp.setPredicateName(pr_name);
				tp.setSubject(U);//subject is shared with r0
				tp.setObject(var);
				tau = expand(tp, candidates, tau); 				
			}
		}		
		
		//only keep the top-k candidates 
		updateCandidates(candidates);
		
		listRlts.addAll(candidates); 
		return listRlts;
	}
	
	//returned value is the new tau
	//the @candidates will be also updated. 
	double expand(RDFPredicate tp, PriorityQueue<ExpRulePackage> candidates, double tau){
		PGEngine qe = new PGEngine(); 
		ArrayList<KVPair<String, Integer>> listPHats = new ArrayList<>(); 
		HashMap<String, Integer> hmapNHats = new HashMap<>();
		 	
		qe.getPHatNhats(this._baseRP, tp, listPHats, hmapNHats);
		//if (tp.getPredicateName().indexOf("type")>=0)
		//	this.getClass();
 		
		for (KVPair<String, Integer> kv: listPHats){
			String a = kv.get_key();
			double p_hat = (double)kv.get_value();
			double n_hat = (double)hmapNHats.get(a);
			double h = QualityManager.evalQuality(p_hat, n_hat); 
			double h_max = QualityManager.evalQuality(p_hat, 0); 
			if (h_max < tau)
				break; //the remaining features cannot have scores larger than tau

			if (h>tau){
				//construct a new tp by replacing the fresh variable in @tp with a 
							
 				ArrayList<String> fresh_vars = this._baseRP.getRule().findFreshArgument(tp);
 				RDFPredicate new_tp = tp.clone();
 				if (tp.getSubject().equals(fresh_vars.get(0)))
 					new_tp.setSubject(a); 				
 				else if (tp.getObject().equals(fresh_vars.get(0)))
 					new_tp.setObject(a);
 				
 				RDFRuleImpl new_r = this._baseRP.getRule().clone();
 				new_r.get_body().addPredicate(new_tp);
 				ExpRulePackage new_rp = new ExpRulePackage(new_r, this._baseRP, p_hat, n_hat);
 				candidates.add(new_rp);
				tau = updateCandidates(candidates);
			}
		} 
		return tau;
	}	
	 
	
	//calculate and return the k^th highest quality score and remove all candidates with scores lower than the computed threshold
	private double updateCandidates(PriorityQueue<ExpRulePackage> candidates){
		//the rule at the head is the least one
		double tau = 0;
		if (candidates.size()<= this._k)
			return tau;
		
		while(candidates.size()>this._k){
			candidates.poll();
		}
		tau = candidates.peek().getQuality();
		return tau;
	}
	  
 
	public static void main(String[] args) {
		ArrayList<Comment> listComments = new ArrayList<Comment>();

		Triple t;
		Comment cmt;
		RandomAccessFile file_data = null;

		try {
			file_data = new RandomAccessFile("comments.txt", "r");
			String line = "";
			while ((line = file_data.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, " ");
				String s, p, o;
				s = st.nextToken();
				p = st.nextToken();
				o = st.nextToken();
				int d = Integer.parseInt(st.nextToken());
				t = new Triple(s, p, o);
				cmt = new Comment(t, (d > 0));
				listComments.add(cmt);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		Feedback fb = new Feedback();
		fb.set_comments(listComments);

		RDFPredicate tp = new RDFPredicate("?s","hasGivenName","?o");
		RDFPredicate tp1 = tp.mapToIncorrectPred(); 	
		
		RDFRuleImpl r = new RDFRuleImpl(); 
		r.set_head(tp1);
		Clause cls = new ClauseSimpleImpl(); 
		cls.addPredicate(tp);
		r.set_body(cls);
		RulePackage rp = new RulePackage(r, fb, null);
		
		FeatureConstructor fc = new FeatureConstructor(rp, 1);
		
		ArrayList<ExpRulePackage> list_candi_rules = fc.constructFeatures(); 			
	
		//ArrayList<Rule> list_candi_rules = new FeatureConstructor(fb, r, 3).constructFeatures();
		//list_candi_rules.add(r);
		//System.out.println(r);
		//System.out.println("quality score:" + r.getQuality());
		  
		for(ExpRulePackage temp_r: list_candi_rules){
			System.out.println(temp_r.getRule());
			System.out.println("quality score:" + temp_r.getQuality());
 		} 
	}

}
