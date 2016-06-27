package gilp.simulation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.feedback.FeedbackBaseManager;
import gilp.learning.GILPSettings;
import gilp.rdf.Triple;
import gilp.db.*;

//generate feedbacks based on the comments stored in the table feedbacks
public class FBGeneratorFromFacts {
	private  ArrayList<Comment> _known_comments;
	
	//load all comments from table feedbacks
	void initialize(){
		this._known_comments = new ArrayList<Comment>();
		String sql = "select s, p, o, cmt from feedbacks"; 
		ArrayList<ArrayList<String>> rlts = DBController.getTuples(sql);
		for (ArrayList<String> tuple : rlts){
			String s = tuple.get(0);
			String p = tuple.get(1);
			String o = tuple.get(2);
			boolean decision = false;
			if (tuple.get(3).equals("1"))
				decision = true;
			Comment cmt = new Comment(new Triple(s,p,o), decision);
			_known_comments.add(cmt);
		}
	}
	
	public FBGeneratorFromFacts(){
		initialize();
	}
	
	// randomly choose @num comments as a set of feedbacks
	public Feedback getRandomComments(int num) {
		String PREDICATE = "hasGivenName";
		
		int s = this._known_comments.size();
		int[] isChosen = new int[s];
		for (int i = 0; i < s; i++) {
			isChosen[i] = 0;
		}

		ArrayList<Comment> listCmts = new ArrayList<Comment>();
		/*listCmts.add(new Comment(new Triple("Du_Feng","hasGivenName", "Du"), false));
		listCmts.add(new Comment(new Triple("Fu_Zuoyi","hasGivenName", "Fu"), false));
		listCmts.add(new Comment(new Triple("Li_Peijing","hasGivenName", "Li"), false));
		listCmts.add(new Comment(new Triple("Lu_Wenyu","hasGivenName", "Lu"), false));
		listCmts.add(new Comment(new Triple("Hu_Zongnan","hasGivenName", "Hu"), false));
		*///listCmts.add(new Comment(new Triple("Kurt_Oppelt","hasGivenName", "Kurt"), true));
		
 
		while (listCmts.size() < Math.min(num, s)) {
			int idx = (int) Math.round(Math.random() * (s - 1));
			if (isChosen[idx] == 0) {
				Comment cmt = this._known_comments.get(idx).clone();
				if (cmt.get_triple().get_predicate().equalsIgnoreCase(PREDICATE) && cmt.get_decision()==false){
					isChosen[idx] = 1;
					listCmts.add(cmt);
				}
			}
		}
		Feedback fb = new Feedback();
		fb.set_comments(listCmts);
		return fb;
	}

	//if a tuple is contained in the feedbacks, we set its feedback with the comment in the table. 
	//otherwise, we require the user to input the decision
	public Feedback getComments(ArrayList<Triple> listTriples){
		HashMap<String,Comment> hmapComments = new HashMap<String,Comment>();
		for (Comment cmt: this._known_comments){
			hmapComments.put(cmt.get_triple().toString(), cmt);
		}
		
		ArrayList<Comment> listCmts = new ArrayList<Comment>();
		for (Triple t: listTriples){
			Comment cmt = hmapComments.get(t.toString());
			if (cmt != null){
				listCmts.add(cmt.clone());
			}
			else{
				//ask use to input comment
				long time0 = System.currentTimeMillis();
				System.out.println("Is the triple \"" + t + "\" correct?(y/n):");

				try {
					char decision = (char)System.in.read();
					while(decision!='y' && decision!='n'){
						System.out.println("Sorry! Please input y or n.");
						System.out.println("Is the triple \"" + t + "\" correct?(y/n):");
						decision = (char)System.in.read();
					}
					if (decision == 'y')
						cmt = new Comment(t,true);
					else
						cmt = new Comment(t,false);
			
					listCmts.add(cmt);
					
					this._known_comments.add(cmt);
					hmapComments.put(cmt.get_triple().toString(), cmt);
					FeedbackBaseManager.insertFeedback(cmt);					
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace(System.out);
				}
				long time1 = System.currentTimeMillis();
				System.out.println("time cost in pulling feedbacks:" + (time1-time0));
				GILPSettings.log("time cost in pulling feedbacks:" + (time1-time0));
			}
		}
		Feedback fb = new Feedback();
		fb.set_comments(listCmts);
		return fb;
	}
	
	
	public static void main(String[] args){
		FBGeneratorFromFacts fbg = new FBGeneratorFromFacts();
		ArrayList<Triple> listTriples = new ArrayList<Triple>(); 
		listTriples.add(new Triple("Yao_Ming", "hasGivenName", "Yao"));
		listTriples.add(new Triple("Zhao_Yuhao", "hasGivenName", "Zhao"));
		listTriples.add(new Triple("Yao_Ming", "isCitizenOf", "China"));
		
		Feedback fb = fbg.getComments(listTriples);
		
		for (Comment cmt :fb.get_comments()){
			System.out.println(cmt);
		}
		
		fb =fbg.getRandomComments(10);
		for (Comment cmt :fb.get_comments()){
			System.out.println(cmt);
		}
	}
}
