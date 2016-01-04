package deletedCodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;

import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.learning.GILPSettings;
import gilp.learning.RuleQualityComparator;
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
	private Feedback _fb; 
	private RDFRuleImpl _r0; 
	private double _P;//the number of consistent triples w.r.t _ro and _fb
	private double _N;//the number of inconsistent triples w.r.t _ro and _fb
	
	
	/*public FeatureConstructor(Feedback fb, RDFRuleImpl r, int k){
		this._k = k;
		this._fb = fb; 
		this._r0 = r; 
		double[] PN = this._r0.getP_N(this._fb);	
		this._P = PN[0];
		this._N = PN[1];
	}
	 */
	/*
	 * From the DB, search all predicates which can be joined with the input
	 * rule, and extract frequent patterns from these joined sub-graphs.
	 */
	/*public ArrayList<Rule> constructFeatures() {
		ArrayList<String> pred_names = GILPSettings.getAllPredicateNames();
		ArrayList<String> args = this._r0.getArguments();
		
		double tau = 0;
			 
		
		PriorityQueue<Rule> candidates = new PriorityQueue<Rule>(10 * this._k, new RuleQualityComparator());
		
		ArrayList<Rule> listRlts = new ArrayList<Rule>(); 
		for (String U: args){
			for (String V: args){
				for (String pr_name : pred_names){
					//TODO: construct a new extended feature as R(U,V) and add it to the candidates
					//Now, just ignore this!!!
					
					//introduce a new variable in the subject
					String var =  this._r0.getNextSubjectVar(false);
					RDFPredicate tp = new RDFPredicate(); 
					tp.setPredicateName(pr_name);
					tp.setSubject(var);
					tp.setObject(U);//object is shared with r0					
					tau = expand(tp, candidates, tau); 
					
					//introduce a new variable in the object
					var =  this._r0.getNextObjectVar(false);
					tp = new RDFPredicate(); 
					tp.setPredicateName(pr_name);
					tp.setSubject(U);//subject is shared with r0
					tp.setObject(var);
					tau = expand(tp, candidates, tau); 
				}	
			}
		}		
		
		//only keep the top-k candidates 
		updateTau(candidates);
		
		listRlts.addAll(candidates); 
		return listRlts;
	}
	*/
	//returned value is the new tau
	//the @candidates will be also updated. 
	/*double expand(RDFPredicate tp, PriorityQueue<Rule> candidates, double tau){
		PGEngine qe = new PGEngine(); 
		ArrayList<KVPair<String, Integer>> listPHats = new ArrayList<>(); 
		HashMap<String, Integer> hmapNHats = new HashMap<>();
		qe.getPHatNhats(this._fb, this._r0, tp, listPHats, hmapNHats);
 		
		for (KVPair<String, Integer> kv: listPHats){
			String a = kv.get_key();
			double p_hat = (double)kv.get_value();
			double n_hat = (double)hmapNHats.get(a);
			double h = this.calc_foil_gain(p_hat, n_hat); 
			double h_max = calc_foil_gain(p_hat, 0); 
			if (h_max < tau)
				break; 
 			if (h>tau){
				//construct a new tp by replacing the fresh variable in @tp with a 
 				ArrayList<String> fresh_vars = this._r0.findFreshArgument(tp);
 				RDFPredicate new_tp = tp.clone();
 				if (tp.getSubject().equals(fresh_vars.get(0)))
 					new_tp.setSubject(a);
 				
 				else if (tp.getObject().equals(fresh_vars.get(0)))
 					new_tp.setObject(a);
 				
 				Rule new_r = this._r0.clone();
 				new_r.get_body().addPredicate(new_tp);
 				//new_r.setQuality(h);
 				candidates.add(new_r);
				tau = updateTau(candidates);
			}
		} 

		return tau;
	}
	*/
	
	double calc_foil_gain(double p_hat, double n_hat){
		if ((p_hat + n_hat) < GILPSettings.EPSILON){
			return 0;
		}
		
		double prec_new = p_hat/(p_hat + n_hat);
		
		double pre_part = 0; 
		if ((this._P + this._N)<GILPSettings.EPSILON){
			pre_part = 0;
		}
		else{
			double prec_old = this._P/(this._P + this._N);
			pre_part = Math.log(prec_old)/Math.log(2.0); 
		}
		
		return p_hat *(Math.log(prec_new)/Math.log(2.0) - pre_part);
	}
	
	//calculate and return the k^th highest quality score and remove all candidates with scores lower than the computed threshold
	private double updateTau(PriorityQueue<Rule> candidates){
		double tau = 0;
		while(candidates.size()>this._k){
			candidates.poll();
		}
		//tau = candidates.peek().getQuality();
		return tau;
	}
	
	
	public ArrayList<Rule> constructFeatures_old(Feedback fb, RDFRuleImpl r) {
		ArrayList<String> pred_names = GILPSettings.getAllPredicateNames();
		ArrayList<String> vars = r.getVariables();
		
		ArrayList<Rule> listRlts = new ArrayList<Rule>(); 
		for (String pr_name : pred_names) {	
			if (!r.containPredicate(pr_name)) {
				for (String lv : vars) {
					System.out.println("try the new predicate:" + pr_name + " for rule:" + r);
					listRlts.addAll(findPossibleFeatures(pr_name, lv, fb, r, 1));
						//subject is a join variable
					listRlts.addAll(findPossibleFeatures(pr_name, lv, fb, r, 2));
						//object is a join variable
				}
			}
		}
		return listRlts;
	}
	
	//find the location of @lv in a clause @cls_qry
	//@return [flag, lv_prd_idx]
	//if @lv appears in subject, flag = 1; otherwise, flag = -1
	//lv_prd_idx: the index of the predicate where @lv appears
	private int[] findVarInClause(Clause cls_qry, String lv){
		int lv_prd_idx = -1; // the index of the predicate where @lv appears
		boolean lv_in_sub = true;// whethere @lv appears in the subject
		Iterator<Predicate> iterClsRPred = cls_qry.getIterator();
		int i = 0;
		while (iterClsRPred.hasNext()) {
			RDFPredicate temp_p = (RDFPredicate) iterClsRPred.next();
			if (temp_p.getSubject().equals(lv)) {
				lv_prd_idx = i;
				break;
			} else if (temp_p.getObject().equals(lv)) {
				lv_prd_idx = i;
				lv_in_sub = false;
				break;
			}
			i++;
		}
		if (lv_prd_idx < 0) {
			System.out.println("findVarInClause: Error! The input @lv must appear in the input @r.");
			return null;
		}
		
		if (lv_in_sub) return new int[]{1, lv_prd_idx};
		else return new int[]{-1, lv_prd_idx};
	}
	
	private ExtendedFeature joinFeatureRule(RDFPredicate tp, Rule r, Feedback fb, String lv) {
		// 1. query feedback, find all triples covered by @r
		Clause cls_qry = new ClauseSimpleImpl();
		// the clause to be used to query the rdf3x engine
		// e.g. hasGivenName(?x, ?y) AND type(?x, Chinese)
		// filter (?x = 'Yao_Ming')

		// first we construct its part from feedback, e.g. hasGivenName(?x, ?y)
		cls_qry = r.get_body().clone();
		if (r.get_head() != null)
			cls_qry.addPredicate(r.get_head());

		// we then find all twigs covered by r and contained in the feedback
		// each twig will be used later as a filter condition, e.g. filter (?x =
		// 'Yao_Ming')
		SimpleCNFQueryEngine sqe = new SimpleCNFQueryEngine();
		// the query engine to query feedbacked triples
		sqe.setDataSet(fb.getAllTriples());
		RDFSubGraphSet fb_twig_set = null;
		
		fb_twig_set = sqe.getTriplesByCNF(cls_qry);// query the feedback
		
		ArrayList<RDFSubGraph> listFBSubGraphs = fb_twig_set.getSubGraphs();
		if (listFBSubGraphs.size() == 0) {
			return null; // empty set
		}

		// find the location of @lv in @cls_qry
		int lv_positions[] = findVarInClause(cls_qry, lv);
		
		cls_qry.addPredicate(tp);
		
		//2. for each twig t covered by @r, construct a join query and store triples which in KB and can join t
	 	ArrayList<Triple> listJoinedTriplesInKB = new ArrayList<Triple> (); 
	 	ArrayList<Triple> listJoinedTriplesInFB = new ArrayList<Triple> (); 		
	  	for(RDFSubGraph sg_cover_by_r: listFBSubGraphs){
			ArrayList<Triple> listSGTriples = sg_cover_by_r.getTriples();
			ArrayList<RDFFilter> listFilters = new ArrayList<RDFFilter>(); 

			int i = 0;
			for(RDFPredicate tp_fb: fb_twig_set.getPredicates()){
				if(tp_fb.isSubjectVariable()){
					String sub = sg_cover_by_r.getTriples().get(i).get_subject();
					RDFFilter filter = new RDFFilter(tp_fb.getSubject(),CompareOperator.EQUAL, sub);
					listFilters.add(filter);
				}
				if (tp_fb.isObjectVariable()){
					String obj = sg_cover_by_r.getTriples().get(i).get_obj();
					RDFFilter filter = new RDFFilter(tp_fb.getObject(),CompareOperator.EQUAL, obj);
					listFilters.add(filter);
				}
				i++;
			}  
			//find the string which the join key 
			/*String joinStr = listSGTriples.get(lv_positions[1]).get_subject();
			if (lv_positions[0]!=1)
				joinStr = listSGTriples.get(lv_positions[1]).get_obj();
			//the filter condition according to this twig
			ArrayList<RDFFilter> listFilters = new ArrayList<RDFFilter>(); 
			RDFFilter filter = new RDFFilter(lv.toString(),CompareOperator.EQUAL, joinStr);
			listFilters.add(filter); 
			*/
			
			RDF3XEngine rdf3x_qe = new RDF3XEngine();
			RDFSubGraphSet db_twig_set = null;			
			db_twig_set = rdf3x_qe.getTriplesByCNF(cls_qry, listFilters);			 
			if (db_twig_set!=null){	
				ArrayList<Triple> listTriples = db_twig_set.getTriplesByPredicate(tp.getPredicateName());
					//project the triples joined with those covered by @r			
				if (listTriples!=null){
					if(listTriples.size()>0){
						listJoinedTriplesInKB.addAll(listTriples);
						listJoinedTriplesInFB.addAll(listSGTriples);
					}
				}
			}
		}
	  	
	  	listJoinedTriplesInFB = Triple.removeDuplicated(listJoinedTriplesInFB); 
		
	  	//ExtendedFeature feature = new ExtendedFeature(r, tp); 
	  	//Feedback constrained_fb = buildConstrainedFeedback(fb, r);
	  	//feature.set_fb(constrained_fb);
	  	//feature.set_joinedTriplesInFB(listJoinedTriplesInFB);
	  	//feature.set_joinedTriplesInKB(listJoinedTriplesInKB);
	  	return null;
	  	//return feature;
	}
	 
 
	//build a feedback which contains only the triples covered by @origin_rule
	private Feedback buildConstrainedFeedback(Feedback origin_fb, Rule origin_rule){
		HashMap<String, Boolean> hmapComments = new HashMap<String, Boolean>(); 
		
		for (Comment cmt: origin_fb.get_comments()){
			hmapComments.put(cmt.get_triple().toString(), cmt.get_decision());
		}
		
		ArrayList<Comment> listComments = new ArrayList<Comment>(); 
		SimpleCNFQueryEngine sqe = new SimpleCNFQueryEngine(); 
		sqe.setDataSet(origin_fb.getAllTriples()); 
		RDFSubGraphSet covered_sgset = null;
					
		covered_sgset = sqe.getTriplesByCNF(origin_rule.getCorrespondingClause());
				
		ArrayList<Triple> listCoveredTriples = covered_sgset.getAllTriples();
				
		for (Triple t: listCoveredTriples){
			Boolean decision = hmapComments.get(t.toString());
			Comment cmt = new Comment(t,decision.booleanValue());
			listComments.add(cmt);
		}
		
		Feedback fb = new Feedback();
		fb.set_comments(listComments);
		
		return fb;
	}

	/*try to expand a rule @r by adding a new predicate p with name as @pr_name
	 * The expansion is conducted by using @lv as a join variable and the @join_pos^th 
	 * //argument of p. 
	 * A frequent constant will be another argument of p. 
	 * */	
	private ArrayList<ExtendedFeature> findPossibleFeatures(String pr_name, String lv,  Feedback fb, RDFRuleImpl r, int join_pos){
		ArrayList<ExtendedFeature> listRlts = new ArrayList<ExtendedFeature>();
			//to store the found predicates
	 	
		// we build an atom from KB, e.g. type(?x, Chinese) 
		RDFPredicate tp = new RDFPredicate();
		tp.setPredicateName(pr_name);
		if (join_pos == 1) {
			// p.subject is the join str, p.object is set as a new object_variable
			tp.setSubject(lv);
			tp.setObject(r.getNextObjectVar(false));
			// the parameter false means no to increase the obj_variable index of r
		} else {
			// p.object is the join str
			tp.setSubject(r.getNextSubjectVar(false));
			tp.setObject(lv);
		}
		
		//join the atom with the rule and build an extended feature
		ExtendedFeature inter_feature = joinFeatureRule(tp, r, fb, lv);
		if (inter_feature==null)
			return listRlts;//empty result 
		
		//next, find the frequent constants of these triples which can join those comments covered by @r		
		int const_pos = 1;
		// the position from which we try to find frequent constants
		if (join_pos == 1)
			const_pos = 2; // it should not be the same as the @join_pos

		ArrayList<String> const_strs = FeatureConstructor.findConstants(pr_name, const_pos, inter_feature.get_joinedTriplesInKB(),
				GILPSettings.FREQUENT_CONST_IN_DB);
		// try to find the frequent constants which appear in @pos^th argument
		// of all the triples

		for (String cons_str : const_strs) {
			// for each frequent constant string, generate a new predicate
			// e.g. P(A, Y) where A is a frequent constant and Y is a join  varible
			RDFPredicate temp_p = new RDFPredicate();
			temp_p.setPredicateName(pr_name);
			if (join_pos == 1){
				temp_p.setSubject(lv);
				temp_p.setObject(new String(cons_str));
			}
			else{
				temp_p.setSubject(new String(cons_str));
				temp_p.setObject(lv);
			}
			
			//now, a variable in the atom has been replaced by a constant			
			ExtendedFeature feature = joinFeatureRule(temp_p, r, fb, lv);

			listRlts.add(feature);
		}	 
		
		return listRlts;
	}
	 
	
	//find the index of the predicate in @sg_set which contains the variable @lv
	//return -1 if not found
	private int findPredicate(RDFSubGraphSet sg_set, String lv){
		int len = sg_set.getPredicates().size();
		for (int i=0;i<len;i++){
			RDFPredicate tp = sg_set.getPredicates().get(i);
			if (tp.getSubject().equals(lv) || tp.getObject().equals(lv))
				return i;
		}
		return -1;
	}
	
	// try to find prominent constants in triples w.r.t the given predicate
	// pos =1 : subject; pos = 2: object;
	// TODO need to handle numerical values ( e.g. most values are smaller than 10)
	public static ArrayList<String> findConstants(String predicate, int pos, ArrayList<Triple> listTriples, double threshold) {
		/*
		 * current strategy if |P(?s, a)|/|P(?s,?o)| > threshold, P(?s, a)
		 * will be an atom
		 */
	 
		HashMap<String, Integer> hmapStrs = new HashMap<String, Integer>();
		for (Triple t : listTriples) {
			String str = "";
			if (pos == 1)
				str = t.get_subject();
			else
				str = t.get_obj();
			if (hmapStrs.containsKey(str)) {
				int count = hmapStrs.get(str);
				hmapStrs.put(str, count + 1);
			} else
				hmapStrs.put(str, 1);
		}

		ArrayList<String> listRlts = new ArrayList<String>();
		for (String str : hmapStrs.keySet()) {
			int count = hmapStrs.get(str);
			if (1.0 * count / (1.0 * listTriples.size()) > threshold) {
				listRlts.add(str);
			}
		}

		return listRlts;
	}
 /*
	public static void main(String[] args) {
		Feedback fb = new Feedback(); 
		ArrayList<Comment> listComments = new ArrayList<Comment>(); 
		Comment cmt1 = new Comment(); 
		cmt1.set_triple(new Triple("Yao_Ming","hasGivenName", "Yao"));
		cmt1.set_decision(false);
		listComments.add(cmt1);
		Comment cmt2 = new Comment(); 
		cmt2.set_triple(new Triple("Liu_Xiang","hasGivenName", "Liu"));
		cmt2.set_decision(false);
		listComments.add(cmt2);
		Comment cmt3 = new Comment(); 
		cmt3.set_triple(new Triple("Li_Na","hasGivenName", "Li"));
		cmt3.set_decision(false);
		listComments.add(cmt3);
		fb.set_comments(listComments);

		RDFRuleImpl r = new RDFRuleImpl(); 
		r.set_head(null);
		Clause cls = new ClauseSimpleImpl(); 
		cls.addPredicate(new RDFPredicate("?s","hasGivenName","?o"));
		r.set_body(cls);
		
		//ArrayList<RDFPredicate> list_tp = new FeatureConstructor().constructFeatures(fb, r); 			
		ArrayList<Rule> list_candi_rules = new FeatureConstructor(fb, r, 3).constructFeatures();
		//list_candi_rules.add(r);
		Rule.evaluteQuality(list_candi_rules, fb);
		//System.out.println(r);
		//System.out.println("quality score:" + r.getQuality());
		  
		for(Rule temp_r: list_candi_rules){
			System.out.println(temp_r);
			System.out.println("quality score:" + temp_r.getQuality());
			ExtendedFeature ef = (ExtendedFeature)temp_r;
			System.out.println("prec, supp_fb, supp_kb :" + ef.get_prec() + "," + ef.get_supp_fb() + "," + ef.get_supp_kb()); 
		}
	}
*/
}
