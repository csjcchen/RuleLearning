package gilp.rdf;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.StringTokenizer;

import gilp.db.DBController;
import gilp.db.DBPool;
import gilp.feedback.Comment;
import gilp.feedback.Feedback;
import gilp.learning.GILPSettings;
import gilp.learning.RulePackage;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.Predicate;
import gilp.rule.RDFPredicate;
import gilp.rule.RDFRuleImpl;
import gilp.rule.Rule;
import gilp.utility.KVPair;
import gilp.utility.StringUtils;

public class PGEngine implements QueryEngine {
	
	public static final String CONSISTENT_TABLE = "consistent";
	public static final String INCONSISTENT_TABLE = "inconsistent";

	@Override
	public Triple getTripleByTid(String tid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Triple> getTriplesBySubject(String subject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Triple> getTriplesByPredicate(String predicate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Triple> getAllTriples() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private class JoinedPredicate{
		String pred_name;
		String join_pos; // s or o
		
		JoinedPredicate(String pred, String pos){
			this.pred_name = pred;
			this.join_pos = pos;
		}
	}
	
	//we assume the RDF data are stored in a r-db where each table stores triples 
	//with the same predicate.
	//TODO: currently we do not support queries where variables appear in predicates
	//TODO: we also do not consider self-join 
	private String buildSQL(Clause cls){
		StringBuffer sb_head = new StringBuffer(); 
		sb_head.append("select * ");
		
		StringBuffer sb_from = new StringBuffer(" from ");
		StringBuffer sb_where = new StringBuffer(" where ");
		
		HashMap<String, Integer> hmap_preds = new HashMap<>(); 
		//ArrayList<String> listPredicates = new ArrayList<String>(); 
		HashMap<String,ArrayList<JoinedPredicate>> hmapJoins = new HashMap<String,ArrayList<JoinedPredicate>>();
				
		Iterator<Predicate> tpIterator = cls.getIterator();
		while(tpIterator.hasNext()){
			RDFPredicate tp = (RDFPredicate)tpIterator.next();
			
			String prop_name = tp.getPredicateName();
			if(!hmap_preds.containsKey(tp.getPredicateName())){
				hmap_preds.put(prop_name, 1);
				prop_name += "_1";
			}
			else{
				int num = hmap_preds.get(prop_name);
				num += 1; 
				hmap_preds.put(prop_name, num);
				prop_name += ("_" + num);
			}			
			
			sb_from.append(tp.getPredicateName()+ " as ").append(prop_name).append(" , ");			
			
			//if (!listPredicates.contains(tp.getPredicateName())){
			//	listPredicates.add(tp.getPredicateName());
			//}
			
			if (tp.isSubjectVariable()){
				ArrayList<JoinedPredicate> joinedPredicates = hmapJoins.get(tp.getSubject());
				if (joinedPredicates == null){
					joinedPredicates = new ArrayList<JoinedPredicate>();
					hmapJoins.put(tp.getSubject(), joinedPredicates);
				}
				joinedPredicates.add(new JoinedPredicate(prop_name,"s"));				
			}
			else{
				//s is a constant
				sb_where.append(prop_name + ".s='" + tp.getSubject()+"'");
				sb_where.append(" and ");
			}
			
			if (tp.isObjectVariable()){
				ArrayList<JoinedPredicate> joinedPredicates = hmapJoins.get(tp.getObject());
				if (joinedPredicates == null){
					joinedPredicates = new ArrayList<JoinedPredicate>();
					hmapJoins.put(tp.getObject(), joinedPredicates);
				}
				joinedPredicates.add(new JoinedPredicate(prop_name,"o"));
			}			
			else{
				//o is a constant
				sb_where.append(prop_name + ".o='" + tp.getObject()+"'");
				sb_where.append(" and ");
			}
		}
 		
		for (String var: hmapJoins.keySet()){
			ArrayList<JoinedPredicate> joinedPredicates = hmapJoins.get(var);
			if (joinedPredicates.size() < 2) 
				continue;//not a join
			else{
				for (int i=0;i<joinedPredicates.size()-1;i++){
					JoinedPredicate pr1 = joinedPredicates.get(i);
					JoinedPredicate pr2 = joinedPredicates.get(i+1);
					sb_where.append(pr1.pred_name + "." + pr1.join_pos);
					sb_where.append("=");
					sb_where.append(pr2.pred_name + "." + pr2.join_pos);
					sb_where.append(" and ");
				}
			}
		}
		
		String from = sb_from.toString();
		from = from.substring(0, from.lastIndexOf(",")-1);
		String where = sb_where.toString();
		if (where.indexOf("and")>=0)
			where = where.substring(0, where.lastIndexOf("and")-1);

		if (where.indexOf("=")<0)
			where = "";
		
		return sb_head.toString() + from + where;
	}
	
	 
	//Find the P_Hats (true positives) and and NHats (false positive) for a rule which is obtained by expanding @ro with @tp
	//One argument  of @tp must be shared with @r0, and the other argument, say X, of @tp is a fresh variable.  
	//This function will retrieve from KB all constants appearing in X and join them with @r0
	//The result will be saved in @listPHats and @hmapNHats
	//@listPHats contains pairs of <v_i, n_i>, where v_i is a constant, and n_i is the corresponding P_Hat
	//if v_i is used as an argument of  @tp  to expand @r0.
	//Elements in @listPHats are in descending order of n_i.
	//@hmapNHats contains <v_i, nhat_i>
	public boolean getPHatNhats(RulePackage rp, RDFPredicate tp, ArrayList<KVPair<String, Integer>> listPHats, HashMap<String,Integer> hmapNHats){
		//TODO
		/*Example
		 * r0 :   hasGivenName(X, Y) AND wasLivenIn(X, Shanghai) --> incorrect_hasGivenName(X, Y)
		 * tp:  rdftype(X, Z) 
		 * 1, we build an empty table: temp_triples_by_rule 
		 * temp_triples_by_rule ( hasGivenName_1_S, hasGivenName_1_O, wasLiveIn_1_S, wasLiveIn_1_O, incorrect_hasGivenName_S, incorrect_hasGivenName_O) 
		 * 2. get the instantiations covered by r0 in fb and insert them into table temp_triples_by_rule
		 * 3. execute the following SQL 
		 * select rdftype.O, count( distinct incorrect_hasGivenName_S || '-' || incorrect_hasGivenName_O) as PHat, 
		 * count( distinct hasGivenName_1_S || '-' || hasGivenName_1_O) as COV  
		 * from temp_triples_by_rule , rdftype 
		 * where temp_triples_by_rule.hasGivenName_1_S=rdftype.S 
		 * group by rdftype.O 
		 * order by PHat desc, COV
 
 		 * NOTE: the argument shared by @r0 and @tp may be a constant, e.g. 
		 * r0: wasLivenIn(X, Shanghai) --> incorrect_hasGivenName(X, Shanghai)
		 * tp: rdftype(Shanghai, Y)
		 * for this case, we need to add a where clause, e.g.  rdftype.S = 'Shanghai' 
		 * 4. build the result-list based on the returned tuples 
		 * */
		Feedback fb = rp.getFeedback();
		RDFRuleImpl r0 = rp.getRule();
		
		String temp_table = "temp_triples_by_rule";
		//1. build the temporary table 
		String sql = "DROP Table IF EXISTS " + temp_table;
		if (!DBController.exec_update(sql)){
			GILPSettings.log(this.getClass().getName() + " there is error when dropping table " + temp_table + ".");
			return false;
		}
		
		sql = "create table " + temp_table + "(";
		HashMap<String, Integer> hmap_preds = new HashMap<>(); 
		Iterator<Predicate> iterPreds = r0.get_body().getIterator();
		boolean findJoinedArgument = false;
		String joinedArgument = ""; 
		String joinedPositionInTP = ""; 
		String joinedPropInRule = ""; 
		
		ArrayList<String> head_vars = ((RDFPredicate)r0.get_head()).getVariables(); 
		HashMap<String,String> hmap_headvar_attr =new HashMap<> (); 
		
		while(iterPreds.hasNext()){
			RDFPredicate p = (RDFPredicate)iterPreds.next();
			String prop_name = p.getPredicateName();
			if(!hmap_preds.containsKey(p.getPredicateName())){
				hmap_preds.put(prop_name, 1);
				prop_name += "_1";
			}
			else{
				int num = hmap_preds.get(prop_name);
				num += 1; 
				hmap_preds.put(prop_name, num);
				prop_name += ("_" + num);
			}			
			sql += prop_name + "_S character varying(1024),"; 
			sql += prop_name + "_O character varying(1024),"; 
					
			if (p.isSubjectVariable()){
				for (String v: head_vars){
					if (p.getSubject().equals(v)){
						hmap_headvar_attr.put(v, prop_name + "_S");
					}
				}
			}
			if (p.isObjectVariable()){
				for(String v: head_vars){
					if(p.getObject().equals(v)){
						hmap_headvar_attr.put(v, prop_name + "_O");
					}
				}
			}			
			
			//find the joined argument
			if (!findJoinedArgument){
				JoinType[] jt = RDFPredicate.getJoinTypes(p, tp);
				if (jt!=null){
					findJoinedArgument = true; 
					switch(jt[0]){
					case SS:						
						joinedPropInRule = prop_name + "_S";
						joinedArgument = tp.getSubject();					
						joinedPositionInTP = "S";
						break; 
					case SO:
						joinedPropInRule = prop_name + "_S";
						joinedArgument = tp.getObject();
						joinedPositionInTP = "O";
						break;
					case OS:
						joinedPropInRule = prop_name + "_O";
						joinedArgument = tp.getSubject();
						joinedPositionInTP = "S";
						break;
					case OO:
						joinedPropInRule = prop_name + "_O";
						joinedArgument = tp.getObject();
						joinedPositionInTP = "O";
						break;
					}
				}
			}
		}
		
		//check the selectivity table first
		// JoinSelect(condition1, condition2, selc, maxMatched)
		String query = "select maxMatched from JoinSelect where ";
		String cond1 = new String(joinedPropInRule); 
		cond1 = cond1.substring(0, cond1.indexOf("_")); 
		cond1 += joinedPropInRule.substring(joinedPropInRule.length()-2);
		String cond2 = tp.getPredicateName() + "_" + joinedPositionInTP; 
		if (cond1.compareToIgnoreCase(cond2)>0){
			String temp = cond1;
			cond1 = cond2;
			cond2 = temp;
		}
		query += " condition1='" + cond1 + "' and condition2='" + cond2 + "'"; 
		String rlt = DBController.getSingleValue(query);
		if (rlt == null){
			return true;
		}
		else{
			int maxMatched = Integer.parseInt(rlt);
			if (maxMatched<= GILPSettings.MINIMUM_MAX_MATCHED){
				return true;
			}
		}
		
 			
		if(!findJoinedArgument){
			GILPSettings.log(this.getClass().getName() + " there are no common arguments between the original rule and the predicate.");
			return false;
		}
		
		sql += r0.get_head().getPredicateName() + "_S character varying(1024),";
		sql += r0.get_head().getPredicateName() + "_O character varying(1024)";
		
		//sql = sql.substring(0, sql.lastIndexOf(","));
		sql += ")"; 
		
		//System.out.println(sql);
		
		if (!DBController.exec_update(sql)){
			GILPSettings.log(this.getClass().getName() + " there is error when creating table consistent.");
			return false;
		}
	
		//2. get the triples covered by r0 in fb ( Body(F) left join H(T) ) and insert them into table consistent
		RDFSubGraphSet sg_set = rp.getSubgraphsCoveredByRule(); 
		for(RDFSubGraph sg: sg_set.getSubGraphs()){
			sql = "insert into " + temp_table + " values("; 
			for (Triple t: sg.getTriples()){
				if (t == null)
					sql += "null, null,";
				else
					sql += "'" + t.get_subject() + "', '" + t.get_obj() + "', "; 
			}
			sql = sql.substring(0, sql.lastIndexOf(","));
			sql += ")"; 
			
			//System.out.println(sql);
			
			if (!DBController.exec_update(sql)){
				GILPSettings.log(this.getClass().getName() + " there is error when inserting tuples into table consistent.");
				return false;
			}	
		}	
		
		// 3. execute the aggreation SQL 
		String aggregate_att = tp.getPredicateName() + "."; 		
		if (joinedPositionInTP=="S")
			aggregate_att += "O";
		else
			aggregate_att += "S"; 
		
		sql = "select " + aggregate_att; 
		sql += ", count( distinct "; 
		RDFPredicate head = (RDFPredicate)r0.get_head();
		if (head.isSubjectVariable())
			sql += head.getPredicateName() + "_S";
		if (head.isObjectVariable())
			sql += " || '-' || " + head.getPredicateName() + "_O"; 
		
		sql += ") as PHat, count( distinct ";  
		
		for (int i=0;i<head_vars.size();i++){
			String v = head_vars.get(i);
			if (i>0)
				sql += " || '-' || "; 
			String attr = hmap_headvar_attr.get(v);
			sql += attr; 
		}
		sql += ") as COV ";
			
		sql += " from " + temp_table +" , " + tp.getPredicateName();
		sql += " where " + temp_table + "." + joinedPropInRule + "=" + tp.getPredicateName() + "." + joinedPositionInTP;
		
		if (joinedPositionInTP=="S" && !tp.isSubjectVariable()){
			sql += " and " + tp.getPredicateName() + ".S='" + joinedArgument + "'";
		}
		if (joinedPositionInTP=="O" && !tp.isObjectVariable()){
			sql += " and " + tp.getPredicateName() + ".O='" + joinedArgument + "'";
		}
		sql += " group by " + aggregate_att; 
		sql += " order by PHat desc, COV" ; 
		
		System.out.println(sql);
		
		ArrayList<ArrayList<String>> listTuples = DBController.getTuples(sql);
		if (listTuples == null)
			return true;
		
		//constant, PHat, COV
		for(ArrayList<String> tuple: listTuples){
			String val = tuple.get(0);
			int p_hat = Integer.parseInt(tuple.get(1));
			int n_hat = Integer.parseInt(tuple.get(2)) - p_hat;
			listPHats.add(new KVPair<String, Integer>(val, p_hat)); 
			hmapNHats.put(val, n_hat);
		}
		return true;
	}
	
	public RDFSubGraphSet getTriplesByCNF(Clause cls, int num){
		String sql = this.buildSQL(cls);
		sql += "  limit " + num; 
		return doQuery(cls, sql);
	}

	@Override
	public RDFSubGraphSet getTriplesByCNF(Clause cls) {
		String sql = this.buildSQL(cls);
		return doQuery(cls, sql);
	}
		
	// get at most @num sub-graphs
	private RDFSubGraphSet doQuery(Clause cls, String query) {
		// execute the SPARQL		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;				 
		RDFSubGraphSet sg_set = null;
		try
		{
			conn = DBController.getConn();
			pstmt = conn.prepareStatement(query);		 
			rs = pstmt.executeQuery();			 
			sg_set =  mountSGSet(rs, cls);
				
		}catch (Exception e)
		{
			GILPSettings.log(this.getClass().getName() + "Error! ");
			e.printStackTrace();
			return null;
		}finally
		{
			DBPool.closeAll(conn, pstmt, rs);
		}	
		return sg_set;
	}

	
	// This function will mount sug-graphs based on the tuples returned by PG and clause (pattern of the sub-graphs).
	// The columns returned by PG are ordered in the same way as they appear in the cls. 
	// Every 3 columns (tid, s, o) correspond to a predicate . 	
	private RDFSubGraphSet mountSGSet(java.sql.ResultSet rlt, Clause cls) {
		
		Iterator<Predicate> myIter = cls.getIterator();
		ArrayList<RDFPredicate> preds = new ArrayList<RDFPredicate>();

		while (myIter.hasNext()) {
			RDFPredicate tp = (RDFPredicate) myIter.next();
			preds.add(tp);
		}
		// initialize the graph set
		RDFSubGraphSet sg_set = new RDFSubGraphSet();
		sg_set.setPredicates(preds);

		// for each result tuple, we mount a sub-graph
		try {
			while (rlt.next()) {
				RDFSubGraph sg = new RDFSubGraph();
				for (int i = 0; i < preds.size(); i++) {
					RDFPredicate tp = preds.get(i);
					Triple t = new Triple();
					t.set_predicate(tp.getPredicateName());
					t.set_subject(rlt.getString(i*3+2));
					t.set_obj(rlt.getString(i*3+3));
					sg.addTriple(t);
				}
				sg_set.addSubGraph(sg);
			}
		} catch (SQLException e) {
			e.printStackTrace(System.out);
			return null;
		}

		return sg_set;
	}
	
	//###############################################################################

	//                   unit  tests
	
	//###############################################################################
	static void testSimpleQuery(){
		Clause cls = new ClauseSimpleImpl();
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o"));
		cls.addPredicate(tp);
	 
		tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("wasBornIn");
		tp.setObject(new String("Shanghai"));		
		cls.addPredicate(tp);
		
		PGEngine pg = new PGEngine();
		System.out.println(pg.buildSQL(cls));
		
		
		System.out.println("#######################################################");
		try {
			System.out.println("query is:" + cls.toString());
			RDFSubGraphSet rlt = pg.getTriplesByCNF(cls);
			System.out.println("results:");
			if (rlt!=null){
				for (RDFSubGraph twig: rlt.getSubGraphs()){
					System.out.println(twig.toString());
				}
			}
			else{
				System.out.println("Empty results set.");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.out);
		}
		
	}
	
	static void testPNHats(){
		
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
		
		PGEngine pg = new PGEngine();
		
		RDFPredicate ex_tp = new RDFPredicate(); 
		ex_tp.setPredicateName("rdftype");
		ex_tp.setSubject("?s1");
		ex_tp.setObject("?o2");
		
		ArrayList<KVPair<String, Integer>> listPHats = new ArrayList<>(); 
		HashMap<String, Integer> hmapNHats = new HashMap<>();
		
		RulePackage rp = new RulePackage(r0, fb, null); 		
		pg.getPHatNhats(rp, ex_tp, listPHats, hmapNHats); 
		
		for (KVPair<String, Integer> kv: listPHats){
			String val = kv.get_key();
			int p_hat = kv.get_value();
			int n_hat = hmapNHats.get(val);
			System.out.println(val + "|" + p_hat + "|" + n_hat);
		}
	}
	
	public static void main(String[] args){
		testSimpleQuery();		
	}

}
