package gilp.learning;
 
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.StringTokenizer;

import gilp.rule.*;
import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.rdf.QueryEngine;
import gilp.rdf.RDFSubGraph;
import gilp.rdf.RDFSubGraphSet;
import gilp.rdf.SimpleCNFQueryEngine;
import gilp.rdf.SimpleQueryEngine;
import gilp.rdf.Triple;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.Predicate;
import gilp.rule.RDFPredicate;
import gilp.rule.RDFRuleImpl;
import gilp.rule.Rule;
import gilp.utility.KVPair;
import gilp.utility.NumericFeatureTools;
/*
 * This class is to learn rules from feedback on RDF KB. The learning approach follows 
 * a BFS manner. Currently the implementation is based on AMIE. 
 * CJC Nov. 12 2015 
 * */
public class RDFBFSLearner{	
	Feedback _fb = null; 
	private int _k = 0;
	ArrayList<Comment> _neg_comments = null;
	ArrayList<Comment> _pos_comments = null;

	ArrayList<RDFPredicate> _fb_atoms = null;
	ArrayList<Triple> _fb_triples = null;
	
	public RDFBFSLearner (Feedback fb, int k){
		this._fb = fb;
		this._k = k;
		init();
	}

	public ArrayList<Rule> learn_rule(Feedback fb){
		//TODO we may need to implement this version
		GILPSettings.log(this.getClass().getName() + ": ERROR! Feature not supported!");
		return null;
	}
	
	private PriorityQueue<RulePackage> initilize_pool(ArrayList<RulePackage> listRules) {
		PriorityQueue<RulePackage> rulePool = new PriorityQueue<RulePackage>(10 * this._k, new RulePHatDSCComparator());		

		RDFRuleImpl r = null;
		// a temporary variable
		
		// if the @listRules is empty, we then need to initialize the rulePool by the atoms
		if (listRules.size() == 0) {
				// initialize the rule pool by the atoms extracted from the feedbacks
			for (RDFPredicate p : this._fb_atoms) {
					// for each aotm p, we generate two initial rules
					// []:->correct_p and []:-> incorrect_p
				//an inclusive rule
				r = new RDFRuleImpl();				
				r.set_head(p.mapToCorrectPred()); 
				r.set_body(new ClauseSimpleImpl());
				RulePackage inclusive_rp = new RulePackage(r, this._fb, null);
				rulePool.addAll(expandRule(inclusive_rp));
				//rulePool.add(inclusive_rp);
				
				//an exclusive rule
				RulePackage exclusive_rp = inclusive_rp.clone();
				r = r.clone();
				r.set_head(p.mapToIncorrectPred());
				exclusive_rp.setRule(r);	
				rulePool.addAll(expandRule(exclusive_rp));
				//rulePool.add(exclusive_rp);
			}
		}
		else {
			rulePool.addAll(listRules);
		}		
		return rulePool;
	}
	
	private void init(){
		this._fb_triples = this._fb.getAllTriples();
		buildNegPosComments();				
		this._fb_atoms = extractAtoms();	
	}
	
	public ArrayList<RulePackage> learn_rule(ArrayList<RulePackage> listRules) {		 
			
		PriorityQueue<RulePackage> rulePool =  initilize_pool(listRules);
				
		PriorityQueue<RulePackage> listRlts =  new PriorityQueue<RulePackage>(this._k, new RuleQualityComparator());		

		double tau = GILPSettings.MINIMUM_FOIL_GAIN;
		while(!rulePool.isEmpty()){
			RulePackage current_rule = rulePool.poll();
			listRlts.add(current_rule.clone());	
			tau = this.getThreshold(listRlts);				
			
			if(current_rule.getRule().isQualified())
				continue;
			
			
			//specialization by add more atoms				
			ArrayList<RulePackage> tempList = expandRule(current_rule);

			current_rule.setExtended(false);
			for (RulePackage child_rule : tempList) {
				double hMax = QualityManager.evalQuality(current_rule.getPHat(), 0);
				if (hMax > tau) {
					rulePool.add(child_rule);
					current_rule.setExtended(true);
					listRlts.add(child_rule.clone());	
					tau = this.getThreshold(listRlts);
				}
			}	

			pruneCandidates(rulePool, tau);
		}
		
		
		for (RulePackage r1: listRlts){				
			((RDFRuleImpl) r1.getRule()).normalize();				
		}
		ArrayList<RulePackage> results = new ArrayList<> ();
	 	results.addAll(listRlts);
		
		for (RulePackage rp: listRlts){
			if (rp.getQuality()<tau){
				results.remove(rp);
			}
		}
		
		RulePackageFactory.removeDuplicatedRP(results);

		if (GILPSettings.IS_DEBUG){
			System.out.println("learned rules:");
			for(RulePackage r: results){
				System.out.println(r);
			}
		}
	
		return results;
	}
	
	private ArrayList<RulePackage> expandRule(RulePackage rp){
		ArrayList<RulePackage> listRlts = new ArrayList<>(); 
		ArrayList<RulePackage> tempRlts = new ArrayList<>(); 
 			//specialization by add more atoms				
		tempRlts.addAll(addClosingAtoms(rp));
		tempRlts.addAll(addDanglingAtoms(rp));
		tempRlts.addAll(addInstantiatedAtoms(rp));
		
		for (RulePackage newRP: tempRlts){
			if (newRP.getRule().isSafe())
				listRlts.add(newRP);
		}
		return listRlts;
	}
	 
	
	//remove all candidates with maximum scores lower than the computed threshold
	//@candidates are sorted by their pHats  in descending order
	private void pruneCandidates(PriorityQueue<RulePackage> candidates, double tau) {
		// the rule at the head is the least one
		ArrayList<RulePackage> tempList = new ArrayList<>();
		while (!candidates.isEmpty()){
			RulePackage rp = candidates.poll();
			if (rp.getRule().isSafe()) {
				double hMax = QualityManager.evalQuality(rp.getPHat(), 0);
				if (hMax>tau)
					tempList.add(rp);
			}
		}
		candidates.clear();
		candidates.addAll(tempList);
	}
	
	//check whether there exist any rules in the pool whose quality scores can be larger than @tau
	private boolean existCandidatesInPool(PriorityQueue<RulePackage> pool, double tau){
		if (pool.isEmpty()) return false;
		RulePackage rp = pool.peek();
		double hMax =  QualityManager.evalQuality(rp.getPHat(), 0);//set nHat as zero, then precision is 1
		return hMax>tau;
	}
	
	//@queue are sorted by their quality scores in ascending order 
	private double getThreshold(PriorityQueue<RulePackage> queue){
		if (queue.size()<= this._k)
			return GILPSettings.MINIMUM_FOIL_GAIN;
		else{
			
			double tau = GILPSettings.MINIMUM_FOIL_GAIN;
			if (queue.size() <= this._k)
				return tau;
			
			ArrayList<RulePackage> temp_list = new ArrayList<>();
			while (queue.size() > this._k) {
				RulePackage rp = queue.poll();
				temp_list.add(rp);
			}
			tau = queue.peek().getQuality();
			
			queue.addAll(temp_list);
			temp_list.clear();
			return tau;
		}
	}
	
	
	private void buildNegPosComments(){
		_neg_comments = new ArrayList<Comment>();
		_pos_comments = new ArrayList<Comment>();
		
		for (Comment cmt: _fb.get_comments()){
			if (cmt.get_decision()==true)
				_pos_comments.add(cmt);
			else
				_neg_comments.add(cmt);
		}
	}
	
	//now, the most simple metric, body length <= 2
	//TODO other reasonable metrics
	private boolean isFinishable(Rule r){
		if (r.get_body()!=null)
			return (r.get_body().getBodyLength()>=2);			 
		else
			return false;//do not accept rules with empty body
	}
	
	
	private boolean isPrunable(RulePackage rp){
		return rp.getPrecision()< GILPSettings.MINIMUM_PRECISION;
	}
	
	/*
	for r:=  u-> False. for all g \in u(F), 
	if exists t\in g and t\in F+, r should be pruned.
	for r:= A->B, let u= A AND B, for all <t1, t2> \in u(F),
	if exists t1\in F+ and t2 \in F-, r is pruned. 
	( A-> B cannot be satisfied if t1 is correct and t2 is incorrect) 
	for both case, r is pruned if |u(F)|=0
	*/
	private boolean isConsistent(Rule r){
		
		//always keep those rules with empty bodies
		if (r.get_body().getBodyLength()==0)
			return false;
		
	 	SimpleCNFQueryEngine myQE = new SimpleCNFQueryEngine();
		myQE.setDataSet(this._fb_triples);		

		if (r.get_head()==null){
			//r:=  u-> False
			Clause cls = r.get_body(); 
			RDFSubGraphSet twigs = null;
			
			twigs = myQE.getTriplesByCNF(cls);
			
			if (twigs ==null) 
				return true;
			else if (twigs.getSubGraphs().size()==0) 
				return true;
			for (RDFSubGraph tw: twigs.getSubGraphs()){
				boolean twigIsPositive = true;
				//if exists twig\in twig and t\in F+, r should be pruned.
				for (Triple t: tw.getTriples()){					
					if (!inPositiveFB(t)){
						twigIsPositive = false;
						break;
					}
				}
				if (twigIsPositive)
					return true;
			}
		}
		else{
			//r:= A->B
			Clause cls = r.get_body().clone(); 
			cls.addPredicate(r.get_head());
			RDFSubGraphSet twigs = null;
			try{
				twigs = myQE.getTriplesByCNF(cls);
			}catch (Exception ex){
				ex.printStackTrace(System.out);
				return true;
			}
			if (twigs ==null) 
				return true;
			else if (twigs.getSubGraphs().size()==0) 
				return true;
			for (RDFSubGraph tw: twigs.getSubGraphs()){
				Triple t1 = tw.getTriples().get(0);
				Triple t2 = tw.getTriples().get(1);
				//if t1\in F+ and t2 \in F-, r is pruned. 
				if (inPositiveFB(t1) && inNegativeFB(t2))
					return true;				
			}
		}
		
		return false;
	}
	
	private boolean inNegativeFB(Triple t){
		for(Comment cmt: this._neg_comments){
			if(cmt.get_triple().equals(t)){
				return true;
			}
		}
		return false;
	}
	
	private boolean inPositiveFB(Triple t){
		for(Comment cmt: this._pos_comments){
			if(cmt.get_triple().equals(t)){
				return true;
			}
		}
		return false;
	}
	
	//extract atoms from the feedback 
	private ArrayList<RDFPredicate> extractAtoms(){
		
		HashMap<String, String> hmapPrediates = new HashMap<String, String>();
		ArrayList<RDFPredicate> listRlts = new ArrayList<RDFPredicate> ();
		
		for (Comment cmt : this._fb.get_comments()){		
			Triple t = cmt.get_triple();
			if (!hmapPrediates.containsKey(t.get_predicate())){
				hmapPrediates.put(t.get_predicate(),"");
				
				RDFPredicate atom = null;
				atom = new RDFPredicate();
				atom.setPredicateName(t.get_predicate());
				atom.setSubject("?s");
				atom.setObject("?o");						
				listRlts.add(atom);
				
				//find constants in this predicate
				ArrayList<String> sub_consts = findConstants(t.get_predicate(),1);
				ArrayList<String> obj_consts = findConstants(t.get_predicate(),2);
				
				
				if (sub_consts.size()>0){
					//for each const_subject, add P(a, ?o)
					for (String con_str: sub_consts){
						atom = new RDFPredicate();
						atom.setPredicateName(t.get_predicate());
						atom.setSubject(new String(con_str));
						atom.setObject("?o");
						listRlts.add(atom);
					}
				}
				
				if (obj_consts.size()>0){
					if(atom.isObjectNumeric()){
						double[] keys = new double[obj_consts.size()];
						int i = 0;
						for(String con_str: obj_consts){
							keys[i++] = Double.parseDouble(con_str);
						}
						
						double[] bound_points = NumericFeatureTools.calcBoundPoints(keys);
						for(i=0;i<bound_points.length-1;i++){
							for (int j=i+1;j<bound_points.length;j++){
								double low = bound_points[i];
								double high = bound_points[j];
								String range = "(" + low + "," + high + ")";
								atom = new RDFPredicate();
								atom.setPredicateName(t.get_predicate());
								atom.setSubject("?s");
								atom.setObject(range);						
								listRlts.add(atom);
							}
						} 
					}
					else{
						//for each const_object, add P(?s, b)
						for (String con_str: obj_consts){
							atom = new RDFPredicate();
							atom.setPredicateName(t.get_predicate());
							atom.setSubject("?s");
							atom.setObject(new String(con_str));						
							listRlts.add(atom);
						}
					}
				}
				
				//if (sub_consts.size()==0 && obj_consts.size()==0){
				
				//} 
			}
		}
		 
		return listRlts;
	}
	
	
	//try to find prominent constants in triples w.r.t the given predicate 
	//pos =1 : subject; pos = 2: object;
	//TODO need to handle numerical values ( e.g. most values are smaller than 10)
	private ArrayList<String> findConstants(String predicate, int pos){
		/* current strategy
		 * if |P(?s, a)|/|P(?s,?o)| > CONST_THRESHOLD, P(?s, a) will be an atom
		*/	
		SimpleQueryEngine qe = new SimpleQueryEngine();
		qe.setDataSet(this._fb_triples);
		ArrayList<Triple> listTriples = qe.getTriplesByPredicate(predicate);
		
		return this.findConstants(predicate, pos, listTriples, 0); 
    }
	
	// try to find prominent constants in triples w.r.t the given predicate
	// pos =1 : subject; pos = 2: object;
	// TODO need to handle numerical values ( e.g. most values are smaller than
	// 10)
	private ArrayList<String> findConstants(String predicate, int pos, ArrayList<Triple> listTriples,
			double threshold) {
		HashMap<String, Integer> hmapStrs = new HashMap<String, Integer>();
		for (Triple t : listTriples) {
			String str = "";
			if (pos == 1)
				str = t.get_subject();
			else
				str = t.get_obj();
			if (hmapStrs.containsKey(str)) {
				int count = hmapStrs.get(str);
				hmapStrs.put(str, count + 1);
			} else
				hmapStrs.put(str, 1);
		}

		ArrayList<String> listRlts = new ArrayList<String>();
		for (String str : hmapStrs.keySet()) {
			int count = hmapStrs.get(str);
			if (1.0 * count / (1.0 * listTriples.size()) > threshold) {
				listRlts.add(str);
			}
		}

		return listRlts;
	}

	/*adds a new atom to a rule so that both of its arguments 
	 * are shared with the rule.*/
	private  ArrayList<RulePackage> addClosingAtoms(RulePackage rp){
		/*find all constants listCon and variables listVar in r
		 *for each atom Q(X,Y) with X and Y can be var or constants
		 *if X (Y) is constant, we try to find whether X (Y) exists in listCons
		 *otherwise, we simply replace X (Y) as any variable in listVar
		 * */
		ArrayList<String> listCon = rp.getRule().getConstants(); 
		ArrayList<String> listVar  = rp.getRule().getVariables();
		
		ArrayList<RulePackage> listRlts = new ArrayList< >();
		
		//handle special case:   []:->False
		if (listCon.size()==0 && listVar.size()==0)
			return listRlts;
		
		ArrayList<RDFPredicate> listAtoms = new ArrayList<RDFPredicate>();
		listAtoms.addAll(this._fb_atoms);
		 
		
		for (RDFPredicate atom: listAtoms){
			if(rp.getRule().containAtom(atom))
				continue;
			
			
			boolean sc = !atom.isSubjectVariable();
			boolean oc = !atom.isObjectVariable();
			boolean sub_in_list = listCon.contains(atom.getSubject().toString()); 
			boolean obj_in_list = listCon.contains(atom.getObject().toString());
			if (sc && oc){
				//both s and o are constants, we add a new atom if both appear in the listCons
				if (sub_in_list && obj_in_list){
					RulePackage rp1 = rp.clone();
					rp1.getRule().get_body().addPredicate(atom.clone());
					listRlts.add(rp1);
				}
			}
			else if (sc && !oc){
				//only s is a constant and exists in the listCons,
				// we replace o by any variable in listVar
				if (sub_in_list){
					for (String var: listVar){
						RDFPredicate a1 = atom.clone();
						a1.setObject(new String(var));
							//change the object to a variable in listVar
						if (!rp.getRule().containAtom(a1)){
							RulePackage rp1 = rp.clone();
							rp1.getRule().get_body().addPredicate(a1);
							listRlts.add(rp1);
 	 					}
					}
				}
			}
			else if (!sc && oc){
				//only o is a constant and exists in the listCons,
				// we replace s by any variable in listVar
				if (obj_in_list){
					for (String var: listVar){
						RDFPredicate a1 = atom.clone();
						a1.setSubject(new String(var));
							//change the subject to a variable in listVar
						if (!rp.getRule().containAtom(a1)){
							RulePackage rp1 = rp.clone();
							rp1.getRule().get_body().addPredicate(a1);
							listRlts.add(rp1);
						}
					}
				}
			}
			else {
				// !sc && !oc
				for (String var_s: listVar){
					for (String var_o: listVar){
						RDFPredicate a1 = atom.clone();
						a1.setSubject(new String(var_s));
						a1.setObject(new String(var_o));
							//change the <subject,object> to any possible
							//pair of variables in listVar
						if (!rp.getRule().containAtom(a1)){
							RulePackage rp1 = rp.clone();
							rp1.getRule().get_body().addPredicate(a1);
							listRlts.add(rp1);
						}
					}
				}
			}
		}
		
		return listRlts;
	} 
	
	/*The new atom uses a fresh variable for one of its two arguments. The other 
	 * argument (variable or entity) is shared with the rule, i.e., it occurs 
	 * in some other atom of the rule.
	 * */
	private  ArrayList<RulePackage> addDanglingAtoms(RulePackage rp){
		/*similar to addClosingAtoms
		 * the difference is : for an atom Q(X,Y), we need to find a match of X or Y
		 * and use a new variable for another
		 * */
		ArrayList<String> listCon = rp.getRule().getConstants();
		ArrayList<String> listVar  = rp.getRule().getVariables();
		
		ArrayList<RDFPredicate> listAtoms = new ArrayList<RDFPredicate>();
		listAtoms.addAll(this._fb_atoms);
	 	
		ArrayList<RulePackage> listRlts = new ArrayList<>();
		
		for (RDFPredicate atom: listAtoms){
			if(rp.getRule().containAtom(atom))
				continue;
			
			boolean sc = !atom.isSubjectVariable();
			boolean oc = !atom.isObjectVariable();
			boolean sub_in_list = (listCon.contains(atom.getSubject().toString())) || (listVar.contains(atom.getSubject().toString())); 
			boolean obj_in_list = (listCon.contains(atom.getObject().toString())) || (listVar.contains(atom.getObject().toString()));
			
			if (sub_in_list && !obj_in_list){
				 //obj is then a dangling element
				RDFPredicate a1 = atom.clone();				
				//if object is a constant, we just add the  original atom
				RulePackage rp1 = rp.clone(); 
				if (!oc){
					//object is a variable, we need to first rewrite the variable to the one
					// with the highest var index in the rule
					a1.setObject((rp1.getRule().getNextObjectVar()));		
				}
				if (!rp.getRule().containAtom(a1)){
					rp1.getRule().get_body().addPredicate(a1);
					listRlts.add(rp1);
				}
			}
			else if (!sub_in_list && obj_in_list){
				//subject is then a dangling element
				RDFPredicate a1 = atom.clone();
				//if subject is a constant, we just add the original atom
				RulePackage rp1 = rp.clone(); 
				if (!sc){
					//subject is a variable, we need to first rewrite the variable to the one
					// with the highest var index in the rule
					a1.setSubject(rp1.getRule().getNextSubjectVar());			
				}
				if (!rp.getRule().containAtom(a1)){
					rp1.getRule().get_body().addPredicate(a1);
					listRlts.add(rp1);
				}				
			}
		}
		
		return listRlts;
	} 
	
	/*
	 *add a new atom to a rule that uses an entity for one argument and shares the other
	 * argument (variable or entity) with the rule.
	 * */
	private  ArrayList<RulePackage> addInstantiatedAtoms(RulePackage rp){
		ArrayList<String> listCon = rp.getRule().getConstants();
		ArrayList<String> listVar  = rp.getRule().getVariables();
		
		ArrayList<RDFPredicate> listAtoms = new ArrayList<RDFPredicate>();
		listAtoms.addAll(this._fb_atoms);
	 	
		ArrayList<RulePackage> listRlts = new ArrayList<>();
		
		for (RDFPredicate atom: listAtoms){
			if (rp.getRule().containAtom(atom))
				continue;
		
			boolean sc = !atom.isSubjectVariable();
			boolean oc = !atom.isObjectVariable();
			boolean sub_in_r = listCon.contains(atom.getSubject().toString()); 
			boolean obj_in_r = listCon.contains(atom.getObject().toString());
			
			if (sc && !oc){
				//s is a constant and o is a variable, then o should be replaced by any variable in r 
				for (String var: listVar){
					RulePackage rp1 = rp.clone();
					RDFPredicate a1 = atom.clone();
					a1.setObject(var);//set o as  a variable in r 
					rp1.getRule().get_body().addPredicate(a1);
					listRlts.add(rp1);
				}			
			}			
			if (!sc && oc){
				//s is a variable and o is a constant, then s should be replaced by any variable in r 
				for (String var: listVar){
					RulePackage rp1 = rp.clone();
					RDFPredicate a1 = atom.clone();
					a1.setSubject(var);//set s as  a variable in r 
					rp1.getRule().get_body().addPredicate(a1);
					listRlts.add(rp1);
				}	
			}
			
			//both s and o are constants and both appear in r, we simply add this atom
			if (sub_in_r && obj_in_r){
				RulePackage rp1 = rp.clone();
				RDFPredicate a1 = atom.clone();		
				rp1.getRule().get_body().addPredicate(a1);
				listRlts.add(rp1);
			}
 		}
		
		return listRlts;
	}
	
	 
	
	public static void main(String[] args){
		// 1. generate a set of triples and comments
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

		RDFBFSLearner learner = new RDFBFSLearner(fb, 3);
		ArrayList<Rule> rules = learner.learn_rule(fb);
		for (int i = 0; i < rules.size(); i++) {
			System.out.println(rules.get(i));
		}
	}

}

/*
 * 	HashMap<String, RDFPredicate> hmapAtoms = new HashMap<String, RDFPredicate>();
		for (Comment cmt : this._fb.get_comments()){		
			Triple t = cmt.get_triple();
			RDFPredicate atom = hmapAtoms.get(t.get_predicate());
			if (atom == null){
				//find constants in this predicate
				atom = new RDFPredicate();
				atom.setPredicateName(t.get_predicate());
				atom.setSubject(new LogicVarSimpleImpl(t.get_subject()));
				atom.setObject(new LogicVarSimpleImpl(t.get_obj()));
				hmapAtoms.put(t.get_predicate(), atom);
			}
			else{
				//e.g. atom = R(Y_M, Y) and t = R(Y_C, Y) ==> atom = R(?s0, Y)
				//note that we suppose this is the first atom to be inserted into a rule
				if (!atom.getSubject().equals(t.get_subject())){
					atom.setSubject(new LogicVarSimpleImpl("?s0"));
				}
				if (!atom.getObject().equals(t.get_obj())){
					atom.setObject(new LogicVarSimpleImpl("?o0"));
				}
			}
		}
		ArrayList<RDFPredicate> listRlts = new ArrayList<RDFPredicate> ();
		listRlts.addAll(hmapAtoms.values());
		return listRlts;
 * */
