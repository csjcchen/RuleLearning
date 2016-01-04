package gilp.learning;

import java.util.ArrayList;

import gilp.feedback.Feedback;
import gilp.rule.Rule;

public abstract class Learner {
	 
	public ArrayList<Rule> learn_rule(Feedback fb){return null;}
	
	public ArrayList<Rule> learn_rule(Feedback fb, ArrayList<Rule> listRules){
		return learn_rule(fb);
	}

}
