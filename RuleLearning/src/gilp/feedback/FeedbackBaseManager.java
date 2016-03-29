package gilp.feedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;

import gilp.db.DBController;
import gilp.db.DBPool;
import gilp.learning.GILPSettings;
import gilp.rdf.PGEngine;
import gilp.rdf.RDFSubGraphSet;
import gilp.rdf.SimpleCNFQueryEngine;
import gilp.rdf.Triple;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.Rule;
 

public class FeedbackBaseManager {
	
	static final String TABLE = "feedbacks";
	
	public static boolean clearAll(){
		String query = "delete from " + TABLE;
		return DBController.exec_update(query);
	}
	
	static Feedback doQuery(String query){
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;				 
		Feedback fb = new Feedback();
		ArrayList<Comment> listComments = new ArrayList<>(); 
		try
		{
			conn = DBController.getConn();
			pstmt = conn.prepareStatement(query);		 
			rs = pstmt.executeQuery();			 
			while(rs.next()){
				String s = rs.getString(1);
				String p = rs.getString(2);
				String o = rs.getString(3);
				int decision = rs.getInt(4);
				Triple t = new Triple(s,p,o);
				Comment cmt = new Comment();
				cmt.set_triple(t);
				cmt.set_decision(decision>0);
				listComments.add(cmt);
			}
				
		}catch (Exception e)
		{
			GILPSettings.log(FeedbackBaseManager.class.getName() +  "DB Error! ");
			e.printStackTrace();
			return null;
		}finally
		{
			DBPool.closeAll(conn, pstmt, rs);
		}	
		fb.set_comments(listComments); 
		return fb;
	}
	
	public static Feedback loadAllFeedbacks(){
	
		String query = "select S, P, O, cmt from " + TABLE;
		return doQuery(query);
	}
	
	public static Feedback loadPositiveFeedbacks(){
		String query = "select S, P, O, cmt from " + TABLE;
		query += " where decision>0";
		return doQuery(query);
	}
	
	public static Feedback loadNegativeFeedbacks(){
		String query = "select S, P, O, cmt from " + TABLE;
		query +=  " where decision<0";
		return doQuery(query);
	}
	
	
	static Feedback loadCoveredFeedbacks(Clause cls){
		Feedback fb = loadAllFeedbacks();
		HashMap<String, Boolean> hmapDecisions = new HashMap<>();
		for (Comment cmt: fb._comments){
			hmapDecisions.put(cmt.get_triple().toString(), cmt.get_decision());
		}
		SimpleCNFQueryEngine sqe = new SimpleCNFQueryEngine();
		sqe.setDataSet(fb.getAllTriples());
		
		RDFSubGraphSet sg_set = sqe.getTriplesByCNF(cls);
		ArrayList<Comment> listComments = new ArrayList<>();
		ArrayList<Triple> listTriples = sg_set.getAllTriples();
		for(Triple t: listTriples){
			Boolean decision = hmapDecisions.get(t.toString());
			if (decision == null)
			{
				GILPSettings.log("error in" + FeedbackBaseManager.class.getName() + ":loadCoveredFeedbacks. ");
				GILPSettings.log(t.toString() + " does not appear in feedback base.");
				return null;
			}		
			else{
				listComments.add(new Comment(t, decision.booleanValue()));
			}
		}
		Feedback new_fb = new Feedback();
		new_fb.set_comments(listComments);
		
		return new_fb;
	}
	
	public static Feedback loadCoveredFeedbacks(Rule r){
		return loadCoveredFeedbacks(r.get_body());
	}
	
	//return comments covered by any rule in the rule set
	public static Feedback loadCoveredFeedbacks(ArrayList<Rule> listRules){
		ArrayList<Comment> listComments = new ArrayList<>();
		for (Rule r: listRules){
			Feedback fb = loadCoveredFeedbacks(r);
			for (Comment cmt: fb._comments){
				if (!listComments.contains(cmt)){
					listComments.add(cmt);
				}
			}
		}
		Feedback fb = new Feedback();
		fb.set_comments(listComments);
		return fb;
	}
	static boolean exists(Comment cmt){
		String query = "select S, P, O, cmt from " + TABLE;
		query += " where S='" + cmt.get_triple().get_subject() + "'";
		query += " and P='" + cmt.get_triple().get_predicate() + "'";
		query += " and O='" + cmt.get_triple().get_obj() + "'";
		
		Feedback fb = doQuery(query);
		if (fb == null)
			return false;
		else if (fb.get_comments().isEmpty()){
			return false;
		}
		else
			return true;
	}
	public static boolean insertFeedback(Comment cmt){
		if (exists(cmt))
			return true;
		else{
			String qry = "insert into " + TABLE + "(s, p, o, cmt)";
			qry += " values('" + cmt.get_triple().get_subject() + "'";
			qry += ",'" + cmt.get_triple().get_predicate() + "'";
			qry += ",'" + cmt.get_triple().get_obj() + "'";
			if (cmt._decision == true)
				qry += ",1)";
			else
				qry += ",-1)";
			if(DBController.exec_update(qry))
				return true;
			else
				return false;
		}
	}

}
