package gilp.learning;

import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.rdf.Triple;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.Predicate;
import gilp.rule.PredicateSimpleImpl;
import gilp.rule.RDFRuleImpl;
import gilp.rule.Rule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream; 
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/*
 * this class is to implement a rule learner based on the FOIL engine.
 * CJC 2015.10.28
 * */

public class FOILLearner extends Learner {
	
	static final String _FOIL_COMMAND = "/home/jinchuan/foil/foil6"; 
		//the Linux command to invoke foil6
	static final String _FOIL_OPTIONS = "-n -v3";
		//the options to run the foil
	static boolean _DEBUG = false;
		//whether or not running in debug mode
	

	@Override
	public ArrayList<Rule> learn_rule(Feedback fb) {
		
		Process pro_foil = null;

		try {
			pro_foil =  Runtime.getRuntime().exec( _FOIL_COMMAND+ " " + _FOIL_OPTIONS);
				//invoke foil6
		} catch (IOException e) {	 
			e.printStackTrace(System.out);
			return null;
		} 
		
		if (pro_foil.isAlive()){			
			if (generate_input_to_foil(pro_foil.getOutputStream(), fb)){
				ArrayList<Rule> rules = get_output_of_foil(pro_foil.getInputStream());
				pro_foil.destroy();
				return rules;
			}
			else{
				System.out.println("FOILLearner: cannot send data to foil6.");
				pro_foil.destroy();
				return null;
			}
		}
		else{
			System.out.println("FOILLearner: cannot start foil6.");
			pro_foil.destroy();
			return null;
		} 
	}
	
	/*
	 * According to a feedback, this function generate and send data to foil.
	 * @foil_os the stream of foil's input
	 * @fb, the feedback
	 * Please refer to foil6's MANUAL or example.explain for 
	 * the format of foil's input.  
	 * return false if any exception encountered; otherwise return true
	 * */ 
	boolean generate_input_to_foil(OutputStream foil_os, Feedback fb){
		/*
		 * 1. Extract the distinct predicates from the feedback, which will 
		 * be the relations. 
		 * 2. Obtain the distinct constants, i.e. subjects and objects. 
		 * 3. Write the data to foil
		 * */
		
		//Extract the distinct predicates and constants, and construct the tuples
		ArrayList<Comment> cmts = fb.get_comments();
		
		HashMap<String,String> cons = new HashMap<String, String>();
			//to store all the constants
		HashMap<String,ArrayList<FoilTuple>> relations = new HashMap<String,ArrayList<FoilTuple>>();
			//to store all the relations
		 
		
			Iterator<Comment> iter = cmts.iterator();		
		while(iter.hasNext()){
			//TODO need to handle negative comments
			
			Triple tr =  iter.next().get_triple();
			String sub = tr.get_subject();
			String obj = tr.get_obj();
			String pred = tr.get_predicate();
						
			//remove the delimters (left and right parenthesis, period, comma,semicolon)
			sub = sub.replaceAll("\\[\\]\\.,;", ""); 
			obj = obj.replaceAll("\\[\\]\\.,;", "");
			pred = pred.replaceAll("\\[\\]\\.,;", "");
			
			/*now, we simply change hasNationality(X, Y) to isChinese(X) if and only if Y equals to 'Chinese'*/
			//TODO need to make this more general		
			if (pred.trim().equalsIgnoreCase("hasNationality")){
				pred = "isChinese";
				if (obj.trim().equalsIgnoreCase("Chinese")){
					obj = "true";
				}
				else{
					obj = "false";
				}
			}
			
			
			//store constants and relations
			if (!cons.containsKey(sub)) 
				cons.put(sub, "");
			if (!cons.containsKey(obj))
				cons.put(obj,"");
			
			ArrayList<FoilTuple> listTuples = null;
			
			if (!relations.containsKey(pred)){
				listTuples = new ArrayList<FoilTuple>();
				relations.put(pred, listTuples);
			}else{
				listTuples = relations.get(pred);
			}
			
			//store each tuple
			FoilTuple tpl = new FoilTuple();
			tpl.set_relation(pred);
			
			ArrayList<String> vals = new ArrayList<String>();
			vals.add(sub);
			vals.add(obj);
			tpl.set_elements(vals);
			
			listTuples.add(tpl);
		}
		
		//write the stored information to foil
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new OutputStreamWriter(foil_os));
			//1. write constants
			bw.write("A:");
				//all rdf triples have the same type called A
			Iterator<String> iter_keys = cons.keySet().iterator();
	
			while (true){
				bw.write(iter_keys.next());
				if (iter_keys.hasNext())
					bw.write(",\n");//to split a long sequence into many short lines
				else{
					bw.write(".\n");//the end of constants
					break;
				}
			}
			
			bw.write("\n");//the end of type definition
			//2. write each relation
			Iterator<String> iter_relation = relations.keySet().iterator();
			while(iter_relation.hasNext()){
				String rel_name = iter_relation.next();				
				if (rel_name.equalsIgnoreCase("isChinese")){
					/*now, we simply change hasNationality(X, Y) to isChinese(X) if and only if Y equals to 'Chinese'*/
					/*and, we only want to make hasFamilyName in the head*/
					//TODO need to make this more general		
					bw.write("*" + rel_name + "(A)\n");
					ArrayList<FoilTuple> tpls = relations.get(rel_name);
					for (int i=0;i<tpls.size();i++){
						ArrayList<String> list_cells = tpls.get(i).get_elements();
						if (list_cells.get(1).equals("true")){
							bw.write(list_cells.get(0) + "\n");
						}
					}
				}
				else{
					/*now, we only want to make hasFamilyName in the head*/
					//TODO need to make this more general	
					if (!rel_name.equalsIgnoreCase("hasFamilyName"))
						bw.write("*");

					bw.write(rel_name + "(A,A)\n");
						//each relation is exactly a triple predicate(subject, object)
				
					ArrayList<FoilTuple> tpls = relations.get(rel_name);
					for (int i=0;i<tpls.size();i++){
						ArrayList<String> list_cells = tpls.get(i).get_elements();
						bw.write(list_cells.get(0) + "," + list_cells.get(1) + "\n");
							//each tuple has exactly two cells, i.e. subject and object
					}
				}
				
				bw.write(".\n");//end of a relation
			}
		
			bw.write("\n"); //end of the relations section
		
			bw.close();
			
			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		 
	}
	
	
	//from the output of foil_is, extract the obtained rule
	ArrayList<Rule> get_output_of_foil(InputStream foil_is){
		if (_DEBUG){
			//in debug mode, just screen the results of foil

			BufferedReader br = new BufferedReader(new InputStreamReader(foil_is));
	        String line_out = null;
	        try {
				while ((line_out = br.readLine()) != null) {
					System.out.println(line_out);
				}
			} catch (IOException e) {
				e.printStackTrace(System.out);
			}
	        
			return null;
		}
		else{
			//the output of foil is many lines. the obtained rules are a sequence of lines between "Clause ..." and "Time "
			// and contain ":-"
			ArrayList<Rule> rlts = new ArrayList<Rule>();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(foil_is));
			ArrayList<String> outputs = new ArrayList<String>();
			String line_out = null;
	        try {
				while ((line_out = br.readLine()) != null) {
					outputs.add(line_out);
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace(System.out);
				return null; 
			}
	        
	        if(outputs.isEmpty())
	        	return null;
	        
	        int i;
	        for(i=outputs.size()-1;i>=0;i--){
	        	//from the end of output, searching for the learned rule	        	
	        	if (outputs.get(i).indexOf("Time")>=0)
	        		break;//return an empty result set
	        }
	        
    		for (;i>=0;i--){
    			line_out = outputs.get(i);
    			if (line_out.indexOf(":-")>0){
    				rlts.add(parseRule(line_out));
    			}
    			else if(line_out.indexOf("Clause")>=0)
    				break;
    		}
    		
    		return rlts;
	        
		}
	}
	
	//try to parse a rule from foil's output
	//format example: 
	//member(A,B) :- components(B,C,D), member(A,D).
	//[a-zA-Z]*([a-zA-Z],[a-zA-Z])
	private Rule parseRule(String str_rule){
		Rule r = new RDFRuleImpl();
		Pattern p = Pattern.compile(" *:- *");
		String[] segs = p.split(str_rule);
		String str_predicate = segs[0];
			//extract the head;
		Predicate prd = parsePredicate(str_predicate);			
		r.set_head(prd);
		
		//extract the body
		Clause cls = new ClauseSimpleImpl();		
		p = Pattern.compile("\\) *, *");
		String[] str_body_prs = p.split(segs[1]);
			//each obtained body predicate is like components(B,C,D  
		for(int i=0;i<str_body_prs.length;i++){
			if (str_body_prs[i].indexOf(")")<0)
				prd = parsePredicate(str_body_prs[i] + ")");
			else
				prd = parsePredicate(str_body_prs[i].substring(0, str_body_prs[i].indexOf(")")+1));
			cls.addPredicate(prd);
		}
		r.set_body(cls);
		
		return r;
	}
	
	//parse member(A,B) as a Predicate
	private Predicate parsePredicate(String str_predicate){
		String str_prd_name = str_predicate.substring(0, str_predicate.indexOf('(')).trim();
		Predicate prd = new PredicateSimpleImpl();
		prd.setPredicateName(str_prd_name);
		String str_vars = str_predicate.substring(str_predicate.indexOf('(')+1, str_predicate.indexOf(')'));
		String[] vars =  str_vars.split(",");
		for (int i=0;i<vars.length;i++){
			prd.addVariable(new String(vars[i]));
		}
		return prd;	
	}
	
	private class FoilTuple{
		String _relation;
		ArrayList<String> _elements;
		
		public FoilTuple(){
		}
		
		public FoilTuple(String relation, ArrayList<String> elements) {
			super();
			this._relation = relation;
			this._elements = elements;
		}
		
		public String get_relation() {
			return _relation;
		}
		public void set_relation(String relation) {
			this._relation = relation;
		}
		public ArrayList<String> get_elements() {
			return _elements;
		}
		public void set_elements(ArrayList<String> elements) {
			this._elements = elements;
		}
		
		@Override
		//generate string according to foil6's format of tuple
		public String toString(){
			//2, C
			StringBuffer sb = new StringBuffer();
			
			if (this._elements.isEmpty())
				return "";
			else{
				Iterator<String> iter = this._elements.iterator();
				sb.append(iter.next());
				while (iter.hasNext()){
					sb.append("," + iter.next());
				}
			}
			return sb.toString();
		}
	}
	
	/*
	 * unit test 
	 * */
	
	public static void main(String[] args){
		_DEBUG = true;
				
		//1. generate a set of triples and comments
		ArrayList<Comment> listComments = new ArrayList<Comment>();

		Triple t;
		Comment cmt;
		RandomAccessFile file_data = null;
	
		try {
			file_data = new RandomAccessFile("comments.txt","r");
			String line = "";
			while((line=file_data.readLine())!=null){
				StringTokenizer st = new StringTokenizer(line," ");
				String s, p, o;
				s = st.nextToken();
				p = st.nextToken();
				o = st.nextToken();
				int d = Integer.parseInt(st.nextToken());
				t= new Triple(s, p, o);
				cmt = new Comment(t, (d>0));
				listComments.add(cmt);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Feedback fb = new Feedback();
		fb.set_comments(listComments);
		
		FOILLearner learner = new FOILLearner();
		ArrayList<Rule> rules = learner.learn_rule(fb);
	 	for (int i=0;i<rules.size();i++){
	 		System.out.println(rules.get(i));
	 	}
	}
}
