package gilp.learning;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
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
import gilp.utility.KVComparatorWRTValue;
import gilp.utility.KVPair;
import gilp.utility.NumericFeatureTools;
import gilp.utility.NumericalKVPairComparator;


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
	
	
	private ArrayList<KVPair<String, Integer>> combinePHats(ArrayList<KVPair<String, Integer>> rltPHats, 
			ArrayList<KVPair<String, Integer>> tempPHats){
		
		for (KVPair<String, Integer> newkv : tempPHats){
			boolean exists = false;
			for (KVPair<String, Integer> oldkv: rltPHats){
				if (newkv.get_key().equalsIgnoreCase(oldkv.get_key())){
					int count = oldkv.get_value();
					count += newkv.get_value();
					oldkv.set_value(count);
					exists = true;
					break;
				}
			} 
			if(!exists){
				rltPHats.add(new KVPair<String, Integer>(newkv.get_key(), newkv.get_value()));
			}
		}
		
		//re-sort the list
		KVPair<String, Integer>[] temp_array = rltPHats.toArray(new KVPair[0]);
		Arrays.sort(temp_array, new KVComparatorWRTValue());
		rltPHats.clear();
		for (int i=temp_array.length-1;i>=0;i--)
			rltPHats.add(temp_array[i]);
		return rltPHats;
	}
	
	private HashMap<String, Integer> combineNHats(HashMap<String, Integer> rltNHats,
			HashMap<String, Integer> tempNHats){
		
		for (String newkey: tempNHats.keySet()){
			if(rltNHats.containsKey(newkey)){
				int count = rltNHats.get(newkey);
				int more_count = tempNHats.get(newkey);
				count +=  more_count;
				rltNHats.put(newkey, new Integer(count));
			}
			else{
				rltNHats.put(newkey, tempNHats.get(newkey));
			}
		}
		return rltNHats;
	}
	
	
	//returned value is the new tau
	//the @candidates will be also updated. 
	double expand(RDFPredicate tp, PriorityQueue<ExpRulePackage> candidates, double tau){
		PGEngine qe = new PGEngine(); 
		ArrayList<KVPair<String, Integer>> listPHats = new ArrayList<>(); 
		HashMap<String, Integer> hmapNHats = new HashMap<>();
		
		if (tp.getPredicateName().equalsIgnoreCase("rdftype")){
			//rdfType has multiple partitioned sub-tables
			for (int i=0;i<GILPSettings.NUM_RDFTYPE_PARTITIONS;i++){
				ArrayList<KVPair<String, Integer>> tempPHats = new ArrayList<>(); 
				HashMap<String, Integer> tempNHats = new HashMap<>();
				tp.setPredicateName("rdftype" + i);
				qe.getPHatNhats(this._baseRP, tp, tempPHats, tempNHats);
				if (tempPHats.size()>0 || !tempNHats.isEmpty()){
					listPHats = combinePHats(listPHats, tempPHats);
					hmapNHats = combineNHats(hmapNHats, tempNHats);
				}				
			}
			tp.setPredicateName("rdftype");
		}
		else
			qe.getPHatNhats(this._baseRP, tp, listPHats, hmapNHats);
		//if (tp.getPredicateName().indexOf("type")>=0)
		//	this.getClass();
		
		ArrayList<String> fresh_vars = this._baseRP.getRule().findFreshArgument(tp);
		
		//deal with numerical properties
		if (tp.getObject().equals(fresh_vars.get(0)) && tp.isObjectNumeric()){
			generateHistograms(listPHats, hmapNHats);
		}
		
		for (KVPair<String, Integer> kv: listPHats){
			
			String a = kv.get_key();
			double p_hat = (double)kv.get_value();
			double n_hat = (double)hmapNHats.get(a);
			double h = QualityManager.evalQuality(p_hat, n_hat); 
			double h_max = QualityManager.evalQuality(p_hat, 0); 
			if (h_max < tau)
				break; //the remaining features cannot have scores larger than tau

			if (h>tau-GILPSettings.EPSILON){
				//construct a new tp by replacing the fresh variable in @tp with a 
 				RDFPredicate new_tp = tp.clone();
 				
 				if (!a.equals("--variable--")){
 					if (tp.getSubject().equals(fresh_vars.get(0)))
 	 					new_tp.setSubject(a); 				
 	 				else if (tp.getObject().equals(fresh_vars.get(0)))
 	 					new_tp.setObject(a);	
 				}
 				
 				tau = addCandidate(candidates, tau, new_tp, p_hat, n_hat); 
 				
 				/*RDFRuleImpl new_r = this._baseRP.getRule().clone();
 				new_r.get_body().addPredicate(new_tp);
 				ExpRulePackage new_rp = new ExpRulePackage(new_r, this._baseRP, p_hat, n_hat);
 				candidates.add(new_rp);
				tau = updateCandidates(candidates);*/
			}
		} 
		
		
		return tau;
	}	
	
	private double addCandidate(PriorityQueue<ExpRulePackage> candidates, double tau, RDFPredicate new_tp, double p_hat, double n_hat){
		RDFRuleImpl new_r = this._baseRP.getRule().clone();
		new_r.get_body().addPredicate(new_tp);
		ExpRulePackage new_rp = new ExpRulePackage(new_r, this._baseRP, p_hat, n_hat);
		candidates.add(new_rp);
		tau = updateCandidates(candidates);
		return tau;
	}
	
	//input table
	/*
	  constant | phat | cov 
	  3        | 1    | 1
	  5        | 0    | 1
	  7        | 2    | 2
	  100000   | 0    | 2
	 create four boundary points  Double.min,  4 = (3+5)/2,  6=(5+7)/2, Double.Max
	 then aggregation
	 output table
	 range  | phat | cov
	 [Double.min, 4] | 1 | 1
	 [Double.min, 6] | 1 | 2
	 [Double.min, Double.max] |  3 |  6
	 [4, 6]   | 0  | 1
	 .......
	 update @listPHats and @hmapNHats, the elements in @listPHats are still ordered by phat desc  
	 */
	private void generateHistograms(ArrayList<KVPair<String, Integer>> listPHats,HashMap<String, Integer> hmapNHats ){
		//1. parse the constants as doubles and create the boundary points 
		//TODO current implementation does not deal with dates		
		double[] keys = new double[listPHats.size()];
		if (keys.length>0)
			this.getClass();
		int i = 0;
		
		int var_phat = -1, var_nhat = -1;

		for (KVPair<String, Integer> kv: listPHats){
			if (kv.get_key().equals("--variable--")){
				var_phat = kv.get_value();
				var_nhat = hmapNHats.get(kv.get_key());
			}				
			else
				keys[i++] = Double.parseDouble(kv.get_key());
		}
		
		double[] bound_points = NumericFeatureTools.calcBoundPoints(keys);

		
		//2. cache the old
		ArrayList<KVPair<String, Integer>> back_PHats = new ArrayList<>();
		HashMap<String, Integer> back_NHats = new HashMap<>();
		
		back_PHats.addAll(listPHats);
		back_NHats.putAll(hmapNHats);
		
		listPHats.clear();
		hmapNHats.clear();
		//3. compute the histogrammed phats and nhats
		
		for(i=0;i<bound_points.length-1;i++){
			for (int j=i+1;j<bound_points.length;j++){
				double low = bound_points[i];
				double high = bound_points[j];
				String range = "(" + low + "," + high + ")";
				int phat = 0;
				int nhat = 0;
				for (int k=0;k<keys.length;k++){
					if (keys[k]>low && keys[k]<high){
						phat += back_PHats.get(k).get_value();
						nhat += back_NHats.get(back_PHats.get(k).get_key());
					}
				}
				listPHats.add(new KVPair<String,Integer>(range, phat));
				hmapNHats.put(range, nhat);
			}
		} 
		KVPair<String, Integer>[] temp_phats = new KVPair[listPHats.size()]; 
		listPHats.toArray(temp_phats);
		
		Arrays.sort(temp_phats, new NumericalKVPairComparator());
		listPHats.clear();
		listPHats.add(new KVPair<String, Integer>("--variable--", var_phat));
		hmapNHats.put("--variable--", var_nhat); 
		for (i=temp_phats.length-1;i>=0;i--){
			listPHats.add(temp_phats[i]);
		}
		
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
