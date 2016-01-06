package gilp.learning;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.rdf.QueryEngine;
import gilp.rdf.RDF3XEngine;
import gilp.rdf.RDFSubGraph;
import gilp.rdf.RDFSubGraphSet;
import gilp.rdf.SimpleCNFQueryEngine;
import gilp.rdf.Triple;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.Predicate;
import gilp.rule.RDFPredicate;
import gilp.rule.RDFRuleImpl;
import gilp.rule.Rule;

public class RulePackage {
	RDFRuleImpl _rule = null; 
	RulePackage _base_RP = null;
	Feedback _fb = null; 
	double _PHat; 
	double _NHat; 
	double _quality;
	double _precision;
	double _fb_support; 
	double _kb_support; 
	boolean  _isExtended = false;
	
	public RulePackage (RDFRuleImpl r, Feedback fb, RulePackage baseRP){
		this._rule = r;
		this._fb = fb;
		this._base_RP = baseRP;
		this.init();
	}
	
	public RulePackage getBaseRP() {
		return this._base_RP;
	}

	public void setBaseRP(RulePackage rp) {
		this._base_RP = rp;
		this._quality = -1;
	}
 
	public RDFRuleImpl getRule(){
		return this._rule;
	}
	public void setRule(RDFRuleImpl r){
		this._rule = r;
		this.init();
	} 
	
	public void setFeedback(Feedback fb){
		this._fb = fb;
		this._quality = -1;
		this._PHat = -1;
		this._NHat = -1;
		this._precision = -1;
		this._fb_support = -1;
	} 
	public Feedback getFeedback(){
		return this._fb; 
	}
		
	void init(){
		this._quality = -1; 
		this._PHat = -1;
		this._NHat = -1;
		this._precision = -1;
		this._kb_support = -1; 
		this._fb_support = -1;
	}
	
		
	public double getQuality() {
		if (this._quality<0)
			this.calc_quality();		
		return _quality;
	}	 

	public double getPHat() {
		if(this._PHat<0)
			this.calcPN_Hats();
		return _PHat;
	} 

	public double getNHat() {
		if(this._NHat<0)
			this.calcPN_Hats();
		return _NHat;
	}
	
	public double getPrecision(){
		if (this._precision<0)
			this.calcPrecision();
		return this._precision;
	}
	
	public double getKBSupport(){
		if (this._kb_support<0)
			this.calcSupportInKB();
		return this._kb_support;
	}
	
	public double getFBSupport(){
		if (this._fb_support<0)
			this.calcSupportInFB();
		return this._fb_support;
	}

	 
	
	// P_plus * ( log_2 (prec) - log2 prec(phi_0)) )	 
	//the gain w.r.t rp0
	void calc_quality() {		
		if (this.getRule().get_body().getBodyLength()>=2  && this.getRule().toString().indexOf("China")>=0)
			this.getClass();
		this._quality = QualityManager.evalQuality(this.getPHat(), this.getNHat());
	}
  
	// precision = P_Hat / (P_Hat + N_Hat) 
	void calcPrecision() {		
		if (this.getPHat()<GILPSettings.EPSILON)
			this._precision = 0;
		else
			this._precision = this._PHat / (this._PHat + this._NHat);	 
	}

	// calculate the support according to a given query engine @qe
	//TODO this may be wrong, need to handle special head. 
	double calcSupport(QueryEngine qe) {
		Clause cls = this._rule.getCorrespondingClause();

		if (!cls.getIterator().hasNext()) {
			return 0;// an empty rule has no support
		}

		// obtain the triples covered by this rule
		RDFSubGraphSet sg_set = qe.getTriplesByCNF(cls);
		if (sg_set == null)
			return 0;// no triples covered by this rule

		ArrayList<Triple> covered_triples = sg_set.getAllTriples();
		if (covered_triples == null)
			return 0;
		else
			return covered_triples.size();
	}
	
	// supp(r) is the set of distinct triples covered by r and contained in the
	// feedback @fb.	 
	void calcSupportInFB() {
		SimpleCNFQueryEngine sqe = new SimpleCNFQueryEngine();
		sqe.setDataSet(this._fb.getAllTriples());		
		this._fb_support = calcSupport(sqe);		 
	}

	// supp(r) is the set of distinct triples covered by r and contained in the
	// back-end knowledge base
	// TODO we need to revise this function to move the codes of executing
	// queries to DB layer
	void calcSupportInKB() {
		// we assume a rule does not contain duplicated predicates.
		// suppose hasGivenName is the first predicate in this rule, we build a
		// special query as count ?p ... ?s ?p ?o ... filter(?p =
		// <hasGivenName>)
		 
		// generate the special query
		Clause cls = this._rule.getCorrespondingClause();

		StringBuffer sb = new StringBuffer("select count ?p where {");
		RDFPredicate first_tp = (RDFPredicate) cls.getIterator().next();

		Iterator<Predicate> myIter = cls.getIterator();
		boolean isFirst = true;
		while (myIter.hasNext()) {
			RDFPredicate tp = (RDFPredicate) myIter.next();

			// in our current RDF3x data set, each constant is enclosed by
			// <>
			if (!tp.isSubjectVariable())
				sb.append("<");
			sb.append(tp.getSubject());
			if (!tp.isSubjectVariable())
				sb.append("> ");
			else
				sb.append(" ");

			if (isFirst) {
				sb.append(" ?p ");
				isFirst = false;
			} else {
				if (!tp.getPredicateName().startsWith("?"))
					sb.append("<");
				sb.append(tp.getPredicateName());
				if (!tp.getPredicateName().startsWith("?"))
					sb.append("> ");
				else
					sb.append(" ");
			}

			if (!tp.isObjectVariable())
				sb.append("<");
			sb.append(tp.getObject());
			if (!tp.isObjectVariable())
				sb.append(">. ");
			else
				sb.append(". ");
		}

		sb.append(" filter(?p =<" + first_tp.getPredicateName() + ">) ");
		sb.append("}");

		RDF3XEngine xqe = new RDF3XEngine();
		this._kb_support = xqe.getCount(sb.toString()); 
	}
	
	// calculate the P_Hat and N_Hat 
	void calcPN_Hats() {
		double[] pn = new double[2];
		pn[0] = pn[1] = 0;

		if (this._rule.isEmpty()){
			this._PHat=this._NHat = 0;// an empty rule's PN hats are zero
			return;
		}
		if (this._rule.get_head() == null){
			GILPSettings.log(this.getClass().getName() + "Error! The head of a rule cannot be null.");
			this._PHat=this._NHat = 0;
			return;
		}
		
		RDFSubGraphSet sg_set = this.getSubgraphsCoveredByRule(); 
		if (sg_set == null){
			this._PHat = 0;
			this._NHat = 0;
		}
		else{
			double cov = sg_set.getSubGraphs().size();
			this._PHat = 0;
			for (RDFSubGraph sg: sg_set.getSubGraphs()){
				ArrayList<Triple> sg_triples = sg.getTriples();
				Triple head_t = sg_triples.get(sg_triples.size()-1);
					//the head triple must appear in the last position of a subgraph
				
				if (head_t!=null)
					this._PHat += 1;//a valid head means this instantiation (subgraph) is covered by the rule			
			} 
			this._NHat = cov - this._PHat;
		}		
	}
	
	public boolean isExtended(){
		return this._isExtended;
	}
	
	public void setExtended(boolean val){
		this._isExtended = val;
	}
	
	@Override
	public RulePackage clone(){
		RulePackage new_rp = null;
		if (this._base_RP!=null)
			new_rp = new RulePackage(this._rule.clone(),this._fb,this._base_RP.clone());
		else
			new_rp = new RulePackage(this._rule.clone(),this._fb,null);
		
		return new_rp;
	}
	
	//Body(F) left join H(T)
	public RDFSubGraphSet getSubgraphsCoveredByRule(){	
		RDFRuleImpl r = this.getRule();
		
		SimpleCNFQueryEngine sqe = new SimpleCNFQueryEngine();
		sqe.setDataSet(this._fb.getAllTriples());
		
		RDFSubGraphSet sg_set = sqe.getTriplesByCNF(r.get_body()); 
		if (sg_set == null)
			return null;// no triples covered by this rule
	
		ArrayList<Triple> consistent_triples = null; 
		
		if (r.isInclusive())
			consistent_triples = this._fb.getPositiveTriples();
		else if (r.isExclusive())
			consistent_triples = this._fb.getNegativeTriples();
		
		ArrayList<String> head_vars = ((RDFPredicate)r.get_head()).getVariables(); 
		
		RDFPredicate no_prefix_head = ((RDFPredicate)r.get_head()).mapToOriginalPred(); 
		HashMap<String, Integer> hmap_vars = new HashMap<>(); 
		for(int i=0;i<head_vars.size();i++)
			hmap_vars.put(head_vars.get(i), i); 
		
		for (RDFSubGraph sg: sg_set.getSubGraphs()){
			String[] binding = new String[head_vars.size()]; 
			for (int i=0;i<sg_set.getPredicates().size();i++){
				RDFPredicate tp = sg_set.getPredicates().get(i);
				Triple t = sg.getTriples().get(i);
				Integer var_idx = hmap_vars.get(tp.getSubject()); 
				if (var_idx!=null){
					binding[var_idx] = t.get_subject();
				}
				var_idx = hmap_vars.get(tp.getObject()); 
				if (var_idx!=null){
					binding[var_idx] = t.get_obj();
				}				
			}
			ArrayList<String> list_binding = new ArrayList<>(); // extract binding of head_vars from sg 
			for (String b: binding){
				list_binding.add(b);
			}
			
			Triple t = no_prefix_head.bind(head_vars, list_binding);  
			if (consistent_triples.contains(t)){
				if (r.isInclusive())
					sg.addTriple(t.mapToCorrectTriple());
				else if (r.isExclusive())
					sg.addTriple(t.mapToIncorrectTriple());
				else 
					sg.addTriple(t);
			}	
			else{
				sg.addTriple(null);
			}
		}
		sg_set.getPredicates().add((RDFPredicate)r.get_head());
 		return sg_set;
	}
	 
	
	
	//body (F) join head(T)
	public RDFSubGraphSet getSubGraphsConsistentWithRule() {
		ArrayList<Triple> listTriples = this._fb.getAllTriples();

		ArrayList<Triple> consistent_triples = null;
		if (this.getRule().isInclusive())
			consistent_triples = this._fb.getPositiveTriples();
		else if (this.getRule().isExclusive())
			consistent_triples = this._fb.getNegativeTriples();
		
		for (Triple t : consistent_triples) {
			Triple t1 = null;
			if (this.getRule().isInclusive())
				t1 = t.mapToCorrectTriple();
			else if (this.getRule().isExclusive())
				t1 = t.mapToIncorrectTriple();
			listTriples.add(t1);
		}

		Clause cls = this.getRule().getCorrespondingClause();
			//Body AND Head
		SimpleCNFQueryEngine sqe = new SimpleCNFQueryEngine();
		sqe.setDataSet(listTriples);

		RDFSubGraphSet sg_set = sqe.getTriplesByCNF(cls);
		return sg_set;
	}
	
	public String toString(){
		String str = this._rule.toString();
		str += " | ";
		if (this._base_RP!=null){
			str += this._base_RP._rule.toString();
		}
		return str;
	}
	
	// ****************************************************************************

	// UNIT TEST

	// ****************************************************************************

	static void testCalPrecision() {
		ArrayList<Comment> listComments = new ArrayList<Comment>();

		Triple t;
		Comment cmt;
		RandomAccessFile file_data = null;

		try {
			file_data = new RandomAccessFile("comments.txt", "r");
			String line = "";
			while ((line = file_data.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, " ");
				String s, p, o;
				s = st.nextToken();
				p = st.nextToken();
				o = st.nextToken();
				int d = Integer.parseInt(st.nextToken());
				t = new Triple(s, p, o);
				cmt = new Comment(t, (d > 0));
				listComments.add(cmt);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		Feedback fb = new Feedback();
		fb.set_comments(listComments);

		Clause cls = new ClauseSimpleImpl();
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String("?s1"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o1"));
		cls.addPredicate(tp);

		RDFPredicate tp1 = tp.mapToIncorrectPred();

		RDFRuleImpl r0 = new RDFRuleImpl();
		r0.set_body(cls);
		r0.set_head(tp1);
		
		RulePackage rp = new RulePackage(r0, fb, null);
		System.out.println(rp.getPHat());
		System.out.println(rp.getNHat());
		System.out.println(rp.getPrecision());
		System.out.println(rp.getQuality());
		
		RDFSubGraphSet sg_set = rp.getSubGraphsConsistentWithRule();
		System.out.println("consistent instantiations.");
		for (RDFSubGraph sg: sg_set.getSubGraphs()){
			System.out.println(sg.toString());
		}
		
		sg_set = rp.getSubgraphsCoveredByRule();
		System.out.println("covered instantiations.");
		for (RDFSubGraph sg: sg_set.getSubGraphs()){
			System.out.println(sg.toString());
		}
	 
	}
	
	public static void main(String[] args){
		testCalPrecision();
	}

}
