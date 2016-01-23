package gilp.learning;

import java.util.ArrayList;

import gilp.feedback.Feedback;
import gilp.rdf.PGEngine;
import gilp.rdf.RDF3XEngine;
import gilp.rdf.RDFSubGraph;
import gilp.rdf.RDFSubGraphSet;
import gilp.rdf.Triple;
import gilp.rule.*; 

/*
 * choose more triples from KB to obtain user feedbacks
 * CJC Dec.8, 2015
 * */
public class TripleSelector {
	
	public ArrayList<Triple> selectTriples(ArrayList<RulePackage> listRules){
		//TODO we need some limitations on the number of total triples we want to probe
		ArrayList<Triple> listRlts = new ArrayList<Triple>();
		for (RulePackage rp: listRules){
			int n1 = calcRequiredNum(rp);
			int n2 = rp.getRule().get_body().getBodyLength() * 5;
			int n = Math.min(n1, n2);
			listRlts.addAll(sampleTriples(rp.getRule(),n));
			if (rp.getRule().isTooGeneral()){
				listRlts.addAll(selectExtendedTriples(rp));
			} 
		}
		return listRlts;
	}
	
	
	ArrayList<Triple> selectExtendedTriples(RulePackage rp){
		//TODO n should not be an independent parameter, and should be adjusted according to an overall resource limit
		int n = 5;	//# of samples for each rule
		int k= 3; // # of rules we choose from candidate extensions
		//TODO need to make k a global setting
		RDFRuleImpl rdf_r = rp.getRule();
		
		FeatureConstructor f_c = new FeatureConstructor( rp, k); 
		ArrayList<ExpRulePackage> candi_rules = f_c.constructFeatures();
			//try to extend/specialize the rule with data stored in the KB
		/*if (GILPSettings.IS_DEBUG){
			System.out.println("extended features for rule:" + r);
			for(Rule can_r: candi_rules){
				System.out.println(can_r);
			}
		}*/ 
		candi_rules = RulePackageFactory.chooseTopExpRP(candi_rules, k);
			//at each round, we only focus on the rules with top-k best qualities
		
		if (GILPSettings.IS_DEBUG){
			System.out.println("chosen features for rule:" + rp.getRule());
			for(ExpRulePackage candi_rp: candi_rules){
				System.out.println(candi_rp.getRule());
			}
		}

		ArrayList<Triple> list_rlts = new ArrayList<Triple>(); 
		for(ExpRulePackage candi_rp: candi_rules){
			ArrayList<Triple> temp_list = sampleTriples(candi_rp.getRule(), n);
				//get n samples for each chosen rule
			if (temp_list != null){
				list_rlts.addAll(temp_list);
			}
		}
		return list_rlts;
	}
	
	//randomly choose @n triples from those covered by @r in the KB
	ArrayList<Triple> sampleTriples(Rule r, int n){
		PGEngine qe = new PGEngine();
		
		Clause cls = ((RDFRuleImpl)r).getNoprefixCaluse(); 

		//try to get at most 10*n triples to be used for sampling
		RDFSubGraphSet sg_set = qe.getTriplesByCNF(cls, 10*n); 
		if(sg_set == null)
			return null;
		ArrayList<RDFSubGraph> listSGs = sg_set.getSubGraphs();
		
		int s = listSGs.size(); 
		int[] isChosen = new int[s];
		for(int i=0;i<s;i++){
			isChosen[i] = 0;
		}
		ArrayList<Triple> sampled_triples = new ArrayList<Triple> ();
		while(sampled_triples.size()<Math.min(s, n)){
			int idx = (int)Math.round(Math.random()*(s-1));
			if (isChosen[idx] == 0){
				RDFSubGraph sg = listSGs.get(idx);				
				sampled_triples.addAll(sg.getTriples());
				isChosen[idx] = 1;
			}	
		}
		
		//find all triples covered by @r in the KB
		//ArrayList<Triple> all_covered_triples = sg_set.getAllTriples();
		//initialize all triples as not been chosen
		//int s = all_covered_triples.size(); 
		//int[] isChosen = new int[s];
	//	for(int i=0;i<s;i++){
		//	isChosen[i] = 0;
		//}
		
		
		 
		
		//while(sampled_triples.size()<Math.min(s, n)){
		//	int idx = (int)Math.round(Math.random()*(s-1));
		//	if (isChosen[idx] == 0){
			//	sampled_triples.add(all_covered_triples.get(idx));
			//	isChosen[idx] = 1;
			//}	
		//}
		
		return sampled_triples;
	}
	
	//calculate the number of samples in order to satisfy the given quality threshold
	//@param p: the average of the the samples
	//we currently utilize the iteration method
	private int calcRequiredNum(RulePackage rp){
		
		double p = rp.getPrecision();
		
		//the min and max number of samples
		int num1 = 1;
		int num2 = 100;
		while(true){
			if (num2 - num1 <= 1)
				break;
			int mid = (num1+num2)/2;
			double l = calcWilsonInterval(p, mid)[0]; 
				
			if (l > GILPSettings.THRESHOLD_OF_PR)
				num2 = mid;			
			else
				num1 = mid;			
		}
		return num2;
	}
	
	//calculate the wilson interval
	//@param p: the observed average
	//@param n: number of samples
	//@return: the lower and upper bounds of the interval
	private double[] calcWilsonInterval(double p, double n){
		double z = GILPSettings.CONFIDENCE_Z; 
 		double v1 = p*(1.0-p)/n + z*z/(4*n*n);
		v1 = z * Math.sqrt(v1); 
		double v2 = p + z*z/(2*n);
		double coef = 1.0/(1 + z*z/n);
		double l = coef * (v2 - v1);
		double h = coef * (v2 + v1);
		
		return new double[]{l,h};
	}
	
	public static void main(String[] args){
		//TODO to make a reasonable unit test
		double [] bounds = new TripleSelector().calcWilsonInterval(1, 34);
		System.out.println(bounds[0] + "," +  bounds[1]);  
	}
}
