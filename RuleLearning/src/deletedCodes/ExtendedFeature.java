package deletedCodes;

import java.util.ArrayList;

import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.rdf.Triple;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.RDFPredicate;
import gilp.rule.RDFRuleImpl;
import gilp.rule.Rule;

/*When a rule is to be specialized by data in the KB, we need to 
 * choose some features from KB in order to extend it. A feature 
 * is exactly an atom in the form of R(X,Y) where X, Y can be either
 * variables of constants, and R is a predicate. 
 * This class is to store some quality scores of an extended feature. 
 * CJC Dec. 7, 2015
 * */
public class ExtendedFeature extends RDFRuleImpl {
	private double _supp_fb; 
	private double _supp_kb; 
	private double _prec; 
	private Feedback _fb; 
	private Rule _orign_rule;
	private RDFPredicate _ex_predicate;
	private ArrayList<Triple> _joinedTriplesInKB; 
	private ArrayList<Triple> _joinedTriplesInFB; 
	
	/*public ExtendedFeature(Rule r, RDFPredicate tp) {
		if (r.get_body() != null)
			this.set_body(r._body.clone());
		else
			this._body = new ClauseSimpleImpl(); 
		this._body.addPredicate(tp);
		
		this.set_head(r.get_head());
		
		this._orign_rule = r;
		this._ex_predicate = tp;		

		this._fb = null;
		this._supp_fb = this._supp_kb = this._prec = -1; 
	}
	*/
	public ArrayList<Triple> get_joinedTriplesInKB() {
		return _joinedTriplesInKB;
	}

	public void set_joinedTriplesInKB(ArrayList<Triple> joinedTriplesInKB) {
		this._joinedTriplesInKB = joinedTriplesInKB;
	}

	public ArrayList<Triple> get_joinedTriplesInFB() {
		return _joinedTriplesInFB;
	}

	public void set_joinedTriplesInFB(ArrayList<Triple> joinedTriplesInFB) {
		this._joinedTriplesInFB = joinedTriplesInFB;
	}
	
	public Rule get_orign_rule() {
		return _orign_rule;
	}
	
	public void set_fb(Feedback fb) {
		this._fb = fb;
	}
	
	public RDFPredicate getExPredicate(){
		return this._ex_predicate;
	}
	   

	public double get_supp_fb() {
		if (this._supp_fb < 0){
			this._supp_fb = this._joinedTriplesInFB.size();
		}
		return this._supp_fb;
	}

	/*public double get_supp_kb() {
		if(_supp_kb < 0)
			_supp_kb = this.calcSupportInKB(); 
		return _supp_kb;
	}*/

	//@Override
	// calculate the P_Plus and the precision
	// @return [0] P_Plus, [1] precision	
	//two specials for ExtendedFeature: 
	//1. it has its own feedback. 
	//2. it has a joinedTriplesInFB and no need to compute the covered triples
	/*protected double[] calcP_Hat_AND_Precision(Feedback fb) {
		ArrayList<Triple> consistent_triples = new ArrayList<Triple>();
		for (Comment cmt : this._fb.get_comments()) {
			if (this.isConsistent(cmt))
				consistent_triples.add(cmt.get_triple());
		}
		
		double[] pp = new double[2];
		pp[0] = pp[1] = 0;
		if (this._joinedTriplesInFB == null)
			return pp;

		// common set of covered and positive ones
		ArrayList<Triple> common_triples = getCommonTriples(this._joinedTriplesInFB, consistent_triples);

		double P_Hat = 0, N_Hat = 0;
 
		P_Hat = common_triples.size();
		N_Hat = this._joinedTriplesInFB.size() - P_Hat;
		pp[0] = P_Hat;
		pp[1] = P_Hat/(P_Hat+N_Hat);
		return pp;
	}
	*/
	// P_plus * ( log_2 (prec) - log2 prec(phi_0)) )
	//Note that the parameters are not useful. 
	//ExtendedFeature always takes its original_rule as r0
	//and uses its own fb.
	/*@Override
	public double calc_foil_gain(Rule r0, Feedback fb) {
		double q0 = 0;
		
		if (!this._orign_rule.isEmpty()) {
			q0 = this._orign_rule.calcPrecision(this._fb);
			if (q0 != 0)
				q0 = Math.log(q0) / Math.log(2.0);
		}

		double[] pp = this.calcP_Hat_AND_Precision(this._fb);

		double P_Plus = pp[0];
		double precision = pp[1];

		if (precision == 0)
			return 0;
		else
			return P_Plus * (Math.log(precision) / Math.log(2.0) - q0);
	}

	public double get_prec() {
		if (_prec < 0) {
			double[] pp = calcP_Hat_AND_Precision(this._fb); 
			this._prec = pp[1];
		}
		return this._prec;
	}
	
	@Override
	public double calcPrecision(Feedback fb) {
		 //ExtendedFeature is a special rule, which has its own feedback set. 
		return this.get_prec();
	}
	
	@Override
	public double calcSupportInFB(Feedback fb) {
		 //ExtendedFeature is a special rule, which has its own feedback set. 
		 return this.get_supp_fb();
	}*/
}
