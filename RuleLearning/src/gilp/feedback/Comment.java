package gilp.feedback;

import gilp.rdf.Triple;

public class Comment {
	Triple _triple;
	boolean _decision;
	//TODO
	
	public Comment(){
		
	}
	public Comment(Triple t, boolean decision){
		_triple = t;
		_decision = decision;
	}

	public Triple get_triple() {
		return _triple;
	}

	public void set_triple(Triple triple) {
		this._triple = triple;
	}

	public boolean get_decision() {
		return _decision;
	}

	public void set_decision(boolean decision) {
		this._decision = decision;
	}
	
	@Override
	public Comment clone(){
		Comment cmt = new Comment(this._triple.clone(), this._decision);
		return cmt;
	}
	
	@Override
	public String toString(){
		return this._triple.toString() + this._decision;
	}
	
	@Override
	public boolean equals(Object obj){
		if (!this.getClass().isInstance(obj)){
			return false;
		}
		
		Comment cmt = (Comment) obj;
		if(!this._triple.equals(cmt._triple))
			return false;
		
		return (this.get_decision() == cmt.get_decision());
	}

}
