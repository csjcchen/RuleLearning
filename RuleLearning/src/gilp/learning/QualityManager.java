package gilp.learning;

public class QualityManager {
	
	//P_hat: true positive; N_hat: false positive; 
	public static double evalQuality(double P_hat, double N_hat){
		return calc_jc_quality(P_hat, N_hat);
	}
	
	//P_hat: true positive; N_hat: false positive; P: really true; N: really false;
	public static double evalQuality(double P_hat, double N_hat, double P, double N){
		return evalQuality(P_hat, N_hat);
			//currently, the same as jc_quality
	}
	
	//calculate the quality as a gain
	//P_hat: true positive; N_hat: false positive; P: really true; N: really false;
	//baseRP: the original rule
	public static double evalQualityGain(double P_hat, double N_hat, double P, double N, RulePackage baseRP){
		return calc_foil_gain(P_hat, N_hat, baseRP);
	}
	
	//calculate the quality as a gain
	//P_hat: true positive; N_hat: false positive; P: really true; N: really false;
	//P_hat_0 and N_hat_0: the true positive and false positive of the original rule
	public static double evalQualityGain(double P_hat, double N_hat, double P, double N, double P_hat_0, double N_hat_0){
		return calc_foil_gain(P_hat, N_hat, P_hat_0, N_hat_0);
	}
	
	private static double calc_jc_quality(double P_hat, double N_hat){
		double d = Math.pow(P_hat, 0.5);
		return d * (calc_precision(P_hat, N_hat));
	}	
	
	private static double calc_precision(double P_hat, double N_hat){
		if (P_hat < GILPSettings.EPSILON) {
			return 0;
		}
		else{
			return P_hat / (P_hat + N_hat);
		}
	}
	
	private static double calc_foil_gain(double P_hat, double N_hat, double P_hat_0, double N_hat_0) {
		if (P_hat < GILPSettings.EPSILON) {
			return 0;
		}

		double prec_new = P_hat / (P_hat + N_hat);

		double prec_old = Math.max(GILPSettings.EPSILON, calc_precision(P_hat_0,N_hat_0));

		return P_hat * (Math.log(prec_new) / Math.log(2.0) - Math.log(prec_old)/Math.log(2.0));
	}
	
	private static double calc_foil_gain(double P_hat, double N_hat, RulePackage baseRP) {
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
		return calc_foil_gain(P_hat, N_hat, P, N);
	}
}
