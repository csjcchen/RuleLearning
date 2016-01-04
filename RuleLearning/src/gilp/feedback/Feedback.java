package gilp.feedback;

import java.util.ArrayList;

import gilp.rdf.Triple;

public class Feedback {
	ArrayList<Comment> _comments;
	//TODO
	
	public ArrayList<Comment> get_comments() {
		return _comments;
	}

	public void set_comments(ArrayList<Comment> _comments) {
		this._comments = _comments;
	} 
	
	//@judgement =0 : returan all triples;
	//@judgement >0 : returan all positive triples;
	//@judgement <0 : returan all negative triples;
	private ArrayList<Triple> getTriples(int judgement){
		ArrayList<Triple> triples = new ArrayList<Triple> ();
		for (Comment cmt:this._comments){	
			if (judgement == 0)
				triples.add(cmt._triple);
			if (judgement>0 && cmt.get_decision())
				triples.add(cmt._triple);
			if (judgement<0 && !cmt.get_decision())
				triples.add(cmt._triple);			
		}
		return triples;
	}
	
	public ArrayList<Triple> getAllTriples(){		
		return getTriples(0);
	}
	
	public ArrayList<Triple> getPositiveTriples(){
		return getTriples(1);
	}
	
	public ArrayList<Triple> getNegativeTriples(){
		return getTriples(-1);
	}
	
}
