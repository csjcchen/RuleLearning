package gilp.learning;

import java.util.ArrayList;
import java.util.Arrays;

import gilp.feedback.Feedback;
import gilp.rule.Rule;

public class RulePackageFactory {

	//choose rules with top-k best quality scores
	public static ArrayList<RulePackage> chooseTopRP(ArrayList<RulePackage> listCandidates, int k){		
		RulePackage[] rules = (RulePackage[]) listCandidates.toArray(new RulePackage[0]);
		Arrays.sort(rules, new RuleQualityComparator());
		ArrayList<RulePackage> listRlts = new ArrayList<>(); 
		for (int i=rules.length-1;i>=Math.max(0, rules.length-k);i--){
			listRlts.add(rules[i].clone());
		}
		return listRlts;
	}
	
	//stupid java!!!	
	public static ArrayList<ExpRulePackage> chooseTopExpRP(ArrayList<ExpRulePackage> listCandidates, int k){		
		ArrayList<RulePackage> temp_list = new ArrayList<>();
		temp_list.addAll(listCandidates);
		temp_list = chooseTopRP(temp_list, k);
		ArrayList<ExpRulePackage> listRlts = new ArrayList<>();
		for (RulePackage rp: temp_list){
			listRlts.add((ExpRulePackage) rp);
		}
		return listRlts;
	}
	
	// clean duplicated rules
	public static void removeDuplicatedRP(ArrayList<RulePackage> listRules) {
		ArrayList<RulePackage> listRlts = new ArrayList<>();
		for (RulePackage rp : listRules) {
			boolean existed = false;
			for (RulePackage rp1: listRlts){
				if (rp1.getRule().equals(rp.getRule())){
					existed = true;
					break;
				}
			}
			if (!existed)
				listRlts.add(rp);
		}
		listRules.clear();
		listRules.addAll(listRlts);
	}
	
	 
	public static double calc_foil_gain(double P_hat, double N_hat, double P, double N) {
		if (P_hat < GILPSettings.EPSILON) {
			return 0;
		}

		double prec_new = P_hat / (P_hat + N_hat);

		double prec_old = 0;
		if (P < GILPSettings.EPSILON) {
			prec_old = GILPSettings.MINIMUM_PRECISION;
		} else {
			prec_old = P / (P + N);
		}

		return P_hat * (Math.log(prec_new) / Math.log(2.0) - Math.log(prec_old)/Math.log(2.0));
	}
	
	public static double calc_foil_gain(double P_hat, double N_hat, RulePackage baseRP) {
		double P, N; 
		if (baseRP == null)
			P = N = 0;
		else if(baseRP.getRule().isEmpty()) {
			P = N = 0;
		}
		else{
			P = baseRP.getPHat();
			N = baseRP.getNHat();
		}
		return RulePackageFactory.calc_foil_gain(P_hat, N_hat, P, N);
	}
	
}
