package gilp.learning;

import java.util.ArrayList;
import java.util.Iterator;

import gilp.feedback.Feedback;
import gilp.rdf.QueryEngine;
import gilp.rdf.RDF3XEngine;
import gilp.rdf.RDFSubGraphSet;
import gilp.rdf.SimpleCNFQueryEngine;
import gilp.rdf.Triple;
import gilp.rule.Clause;
import gilp.rule.Predicate;
import gilp.rule.RDFPredicate;
import gilp.rule.RDFRuleImpl;

//an expanded rule. 
//CJC DEC 31, 2016
public class ExpRulePackage extends RulePackage {
	
	//For expanded rules, we do not need to store their feedbacks, the pHat and nHat will be given.	 
	public ExpRulePackage(RDFRuleImpl r, RulePackage baseRP, double pHat, double nHat){
		 super(r, null, baseRP);//set feedback as null
		 this._PHat = pHat;
		 this._NHat = nHat;
		 init();
	} 
	
	void init(){
		this._quality = -1; 	
		this._kb_support = -1; 
		this._fb_support = -1;
		this._precision = -1;
	}
		
	@Override 
	public double getPHat() {
		return _PHat;
	} 
	@Override
	public double getNHat() {
		return _NHat;
	}		
	
	@Override
	@Deprecated
	public double getFBSupport(){
		GILPSettings.log(this.getClass().getName() + "Error! The getFBSupport is not supported by ExpRulePackage.");
		return -1;
	}
	
	@Override
	@Deprecated
	void calcSupportInFB(){
		GILPSettings.log(this.getClass().getName() + "Error! The calcSupportInFB is not supported by ExpRulePackage.");
	}
	
	@Override 
	@Deprecated
	public void setBaseRP(RulePackage rp) {
		GILPSettings.log(this.getClass().getName() + "Error! The setBaseRP is not supported by ExpRulePackage.");
	}
 
	@Override
	@Deprecated
	public void setRule(RDFRuleImpl r){
		GILPSettings.log(this.getClass().getName() + "Error! The setRule is not supported by ExpRulePackage.");
	} 
	
	@Override
	@Deprecated
	public void setFeedback(Feedback fb){
		GILPSettings.log(this.getClass().getName() + "Error! The setFeedback is not supported by ExpRulePackage.");
	} 
	@Override
	@Deprecated
	public Feedback getFeedback(){
		GILPSettings.log(this.getClass().getName() + "Error! The getFeedback is not supported by ExpRulePackage.");
		return null; 
	}
	
	@Override
	@Deprecated 
	void calcPN_Hats() {
		GILPSettings.log(this.getClass().getName() + "Error! The calcPN_Hats is not supported by ExpRulePackage."); 
	} 
}
