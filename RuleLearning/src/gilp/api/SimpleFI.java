package gilp.api;

import java.util.ArrayList;

import gilp.rdf.Triple;

public class SimpleFI implements FeedbackInterface {

	@Override
	public boolean submitComment(Triple t, boolean decision) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean submitNewVal(Triple old_t, Triple new_t) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean submitConflict(ArrayList<Triple> triples) {
		// TODO Auto-generated method stub
		return false;
	}

}
