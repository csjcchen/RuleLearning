package gilp.learning;
 
import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import amie.mining.AMIE;
import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.rdf.Triple;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.Predicate;
import gilp.rule.PredicateSimpleImpl;
import gilp.rule.Rule;
import gilp.rule.RDFRuleImpl;
import javatools.datatypes.ByteString;

public class AMIELearner extends Learner {
	static boolean _DEBUG = false;
		//whether or not running in debug mode
	
	String _temp_data_file = "chinese.txt";
		//the name of the file to store the temp data
	
	@Override
	public ArrayList<gilp.rule.Rule> learn_rule(Feedback fb) {
		//first of all, create a temporal file storing the fb's content
		
		File data_file; 
		data_file = new File(_temp_data_file);
		if (data_file.exists())
			data_file.delete();
		
		try {
			RandomAccessFile ra_file = new RandomAccessFile(_temp_data_file, "rw");
			ArrayList<Comment> comments = fb.get_comments();
			for (Comment cmt: comments){
				ra_file.writeBytes("<" + cmt.get_triple().get_subject() + ">\t");
				ra_file.writeBytes("<" + cmt.get_triple().get_predicate() + ">\t");
				ra_file.writeBytes("<" + cmt.get_triple().get_obj()+ ">\n");
			}
			ra_file.close();		
			
			//then call AMIE miner
		 	String[] paras= {"-mins", "1", "-minis", "1", "-const", _temp_data_file}; 
		
			AMIE miner = AMIE.getInstance(paras);
			
			List<amie.rules.Rule> rules = miner.mine();
			
			if (rules!=null){
				ArrayList<gilp.rule.Rule> listRlts = new ArrayList<gilp.rule.Rule> ();
				for (amie.rules.Rule rule : rules) {
				  listRlts.add(transform(rule));
				}
				return listRlts;
			} 
		} catch (Exception e) {			
			e.printStackTrace(System.out);
		}
		
		return null;
	}
	
	private Predicate extractPredicate(ByteString[] bs){
		Predicate prd = new PredicateSimpleImpl();
	    String sub  =  bs[0].toString();
	    String p = bs[1].toString();
	    String obj = bs[2].toString();
	    sub = sub.replaceAll("\\?", "").replaceAll("<", "'").replaceAll(">", "'");
	    obj = obj.replaceAll("\\?", "").replaceAll("<", "'").replaceAll(">", "'");
	    p = p.replaceAll("<", "").replaceAll(">", "");
	    prd.setPredicateName(p);
	    prd.addVariable(new String(sub));
	    prd.addVariable(new String(obj));
	    
		return prd;
	}
	
	//transform an AMIE Rule object to a edu.ulm.rule.Rule object
	private gilp.rule.Rule transform(amie.rules.Rule amie_rule){
		gilp.rule.Rule  r = new gilp.rule.RDFRuleImpl ();
		Predicate head = extractPredicate(amie_rule.getHead());
		
		Clause cls = new ClauseSimpleImpl();
		for (ByteString[] bs: amie_rule.getBody() ){
			cls.addPredicate(extractPredicate(bs));
		}
		
		r.set_head(head);
		r.set_body(cls);
		
		return r;
	}
	
	//unit test
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
		
		AMIELearner learner = new AMIELearner();
		ArrayList<gilp.rule.Rule> rules = learner.learn_rule(fb);
	 	for (int i=0;i<rules.size();i++){
	 		System.out.println(rules.get(i));
	 	} 
	}

}
