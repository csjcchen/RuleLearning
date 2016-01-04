package gilp.api;

import java.util.ArrayList;

import gilp.rdf.Triple;

public interface FeedbackInterface {
	
	public boolean submitComment(Triple t, boolean decision);
		//to submit a bollean @decision on @t
		//return true if the submission is done successfully; otherwise return false;
	
	public boolean submitNewVal(Triple old_t, Triple new_t);
		//to specify that the @old_t should be replaced by the @new_t
	
	public boolean submitConflict(ArrayList<Triple> triples);
		//to specify that all triples inside @triples cannot co-exist
	
	
}
