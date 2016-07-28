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
		
		StringBuffer sb_sel = new StringBuffer("select ");
		StringBuffer sb_from = new StringBuffer(" from ");
		StringBuffer sb_where = new StringBuffer(" where ");
		
		HashMap<String, Integer> hmap_preds = new HashMap<>(); 
		//ArrayList<String> listPredicates = new ArrayList<String>(); 
		HashMap<String,ArrayList<JoinedPredicate>> hmapJoins = new HashMap<String,ArrayList<JoinedPredicate>>();
				
		Iterator<Predicate> tpIterator = cls.getIterator();
		boolean hasWhereClause = false;
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
			sb_sel.append(prop_name +  ".tid, " + prop_name + ".S, ");
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
				hasWhereClause = true;
			}
			
			if (tp.isObjectVariable()){
				ArrayList<JoinedPredicate> joinedPredicates = hmapJoins.get(tp.getObject());
				if (joinedPredicates == null){
					joinedPredicates = new ArrayList<JoinedPredicate>();
					hmapJoins.put(tp.getObject(), joinedPredicates);
				}
				joinedPredicates.add(new JoinedPredicate(prop_name,"o"));
				if (tp.isObjectNumeric())
					sb_sel.append(prop_name + ".NUM_O, ");
				else
					sb_sel.append(prop_name + ".O, ");
			}			
			else{
				//o is a constant
				hasWhereClause = true;
				if (tp.isObjectNumeric()){
					double[] bounds = tp.getObjBounds();
					sb_where.append(prop_name + ".num_o between " + bounds[0] + " and " + bounds[1] + " "); 
					sb_sel.append(prop_name + ".NUM_O, ");
				}
				else{
					sb_where.append(prop_name + ".o='" + tp.getObject()+"'");
					sb_sel.append(prop_name + ".O, ");
				}
				sb_where.append(" and ");
			}
		}
 		
		for (String var: hmapJoins.keySet()){
			ArrayList<JoinedPredicate> joinedPredicates = hmapJoins.get(var);
			if (joinedPredicates.size() < 2) 
				continue;//not a join
			else{
				for (int i=0;i<joinedPredicates.size()-1;i++){
					hasWhereClause = true;
					JoinedPredicate pr1 = joinedPredicates.get(i);
					JoinedPredicate pr2 = joinedPredicates.get(i+1);
					sb_where.append(pr1.pred_name + "." + pr1.join_pos);
					sb_where.append("=");
					sb_where.append(pr2.pred_name + "." + pr2.join_pos);
					sb_where.append(" and ");
				}
			}
		}
		
		String sel = sb_sel.toString();
		sel = sel.substring(0, sel.lastIndexOf(","));
		
		String from = sb_from.toString();
		from = from.substring(0, from.lastIndexOf(","));
		String where = sb_where.toString();
		if (where.indexOf("and")>=0)
			where = where.substring(0, where.lastIndexOf("and"));

		if (!hasWhereClause)
			where = "";
	
 		return sel + from + where;
	}
	
	public boolean isTripleCovered(Triple t, RDFRuleImpl r){
		RDFPredicate head = (RDFPredicate) r.get_head();
		String head_relation = head.mapToOriginalPred().getPredicateName();
		head_relation = head_relation + "_1";// the relations will be renamed in
												// the buildSQL
		String sel = "SELECT  count(*) ";
		
		if(containRDFType(r.get_body())){
			for(int i=0;i<GILPSettings.NUM_RDFTYPE_PARTITIONS;i++){
				String newPred = "rdftype" + i; 
				Clause convertedCls = replaceRDFType(r.get_body(), newPred);
				String sql = this.buildSQL(convertedCls);
				sql =  sql.substring(sql.indexOf("from"));		
				sql = sel + sql;
				if (sql.indexOf("where")>=0){
					sql += " and ";
				}
				else{
					sql += " where ";
				}
				sql += head_relation + ".S='" + t._subject + "'";
				sql += " and " + head_relation + ".O='" + t._obj + "'";
				
				String rlt = DBController.getSingleValue(sql); 
				int num = Integer.parseInt(rlt); 
				if (num>0)
					return true;				
			}
			return false; 
		}
		else{
			String sql = this.buildSQL(r.get_body());
			sql =  sql.substring(sql.indexOf("from"));		
			sql = sel + sql;	
			if (sql.indexOf("where")>=0){
				sql += " and ";
			}
			else{
				sql += " where ";
			}
			sql +=  head_relation + ".S='" + t._subject + "'";
			sql += " and " + head_relation + ".O='" + t._obj + "'";
			
			String rlt = DBController.getSingleValue(sql); 
			int num = Integer.parseInt(rlt); 
			if (num>0)
				return true;	
			else
				return false;
		}
		
	}
	
	//return HC(r1)\cap HC(r2) / HC(r1)  
	public double getHCContainedPr(RDFRuleImpl r1, RDFRuleImpl r2){
		int num = 200; // # of tuples sampled from both HC
		
		ArrayList<Triple> listHC1 = getHeadCoverage(r1, num); 
		
		if(listHC1.size()==0)
			return 0; 

		int overlap = 0;
		for(Triple t: listHC1){
			if(isTripleCovered(t, r2)){
				overlap++;
			}
		}
		
		double d0 = (double)overlap / (double)listHC1.size();
		
		return d0;
		
		/*
		ArrayList<Triple> listHC2 = getHeadCoverage(r2, num); 
		
		int min_num = Math.min(listHC1.size(), listHC2.size());
		
		if (min_num ==0 ){
			return new double[]{0.0, 0.0}; 
		}
		
		int max_num = Math.max(listHC1.size(), listHC2.size());
		if (min_num < num){
			for (int i=max_num-1;i>=min_num; i--){
				if(listHC1.size()>i){
					listHC1.remove(i);
				}
				if(listHC2.size()>i){
					listHC2.remove(i);
				}
			}
		}
		
		HashMap<String, String> hmapTriples = new HashMap<>(); 
		for (int i=0; i<listHC1.size();i++){
			hmapTriples.put(listHC1.get(i).toString(), "");
		}
		
		
		for (int i=0; i<listHC2.size();i++){
			if(hmapTriples.containsKey(listHC2.get(i).toString())){
				overlap ++;
			}
		}
		*/

	}
	

	//compute and return the head coverage of the input rule
	//@n the number of returned triples 
	public ArrayList<Triple> getHeadCoverage(RDFRuleImpl r, int n) {
		// get the relation name appearing in the head
		RDFPredicate head = (RDFPredicate) r.get_head();
		String head_relation = head.mapToOriginalPred().getPredicateName();
		head_relation = head_relation + "_1";// the relations will be renamed in
												// the buildSQL
		String sel = "SELECT  " + head_relation + ".*  ";
		
		if(containRDFType(r.get_body())){
			RDFSubGraphSet rltSet = new RDFSubGraphSet();
			for(int i=0;i<GILPSettings.NUM_RDFTYPE_PARTITIONS;i++){
				String newPred = "rdftype" + i; 
				Clause convertedCls = replaceRDFType(r.get_body(), newPred);
				String sql = this.buildSQL(convertedCls);
				sql =  sql.substring(sql.indexOf("from"));		
				sql = sel + sql;				
				
				if (sql.indexOf("where")>=0)
					sql += " and random()<0.05 ";
				else 
					sql += " where random()<0.05 "; 
				
				//sql += " order by random() ";//remove this condition to get efficiency
				sql += "  limit " + n; 
				
				Clause cls = new ClauseSimpleImpl();
				cls.addPredicate(head.mapToOriginalPred());
				
				RDFSubGraphSet oneSet = doQuery(cls, sql);
				if (i==0)
					rltSet.setPredicates(oneSet.getPredicates());
				for (RDFSubGraph sg: oneSet.getSubGraphs())
					rltSet.addSubGraph(sg);
			}
			
			if(rltSet.getSubGraphs().size()>n){
				int s = rltSet.getSubGraphs().size();
				int[] isChosen = new int[s];
				for (int i = 0; i < s; i++) {
					isChosen[i] = 0;
				}
				ArrayList<RDFSubGraph> listChosenSGs = new ArrayList<>();
				while (listChosenSGs.size() < Math.min(n, s)) {
					int idx = (int) Math.round(Math.random() * (s - 1));
					if (isChosen[idx] == 0) {
						RDFSubGraph sg = rltSet.getSubGraphs().get(idx);
						isChosen[idx] = 1;
						listChosenSGs.add(sg);
					}
				}
				rltSet.getSubGraphs().clear();
				rltSet.getSubGraphs().addAll(listChosenSGs);
			}
			
			return rltSet.getAllTriples();		
			 
		}
		else{
			String sql = this.buildSQL(r.get_body());
			sql =  sql.substring(sql.indexOf("from"));		
			sql = sel + sql;	
			if (sql.indexOf("where")>=0)
				sql += " and random()<0.05 ";
			else 
				sql += " where random()<0.05 "; 
			
			//sql += " order by random() ";
			sql += "  limit " + n; 
			Clause cls = new ClauseSimpleImpl();
			cls.addPredicate(head.mapToOriginalPred());
			RDFSubGraphSet rltSet = doQuery(cls, sql);
			return rltSet.getAllTriples();
		}
		
		
		//String sql = this.buildSQL(r.get_body());
		// replace the Select part in the SQL
		//sql = sql.substring(sql.indexOf("from"));
		//String sel = "SELECT distinct " +  head_relation + ".* ";
		//sql = sel + sql; 
		
		
		
		//RDFSubGraphSet sg_set = doQuery(cls, sql);
		
		 	
		
		//return null;
	}
	
	//compute and return the size of head coverage of the input rule
	public boolean isLargerThanMinHC(RDFRuleImpl r){
		//close this check due to efficiency problem
		int a = 1;
		if (a == 1)
			return true;
		
		/*basically the main steps are to build SQL
		 * use the body to generate a SQL
		 * project the result on the head
		 * count
		 * hasNationality(x, China) and hasFamilyName(x, y) --> incorrect_hasFamilyName(x, y)
		 * 
		 * select count (distinct hasFamilyName.*) from hasNationality, hasFamilyName where hasNationality.S = hasFamilyName.S
		 * and  hasNationality.O = 'China'
		 * */		
		//get the relation name appearing in the head
		RDFPredicate head = (RDFPredicate) r.get_head();
		String head_relation = head.mapToOriginalPred().getPredicateName();
		head_relation = head_relation + "_1";//the relations will be renamed in the buildSQL
		String sel = "SELECT " + head_relation + ".* ";
		//TODO we now do not consider the case that rdftype appears in the head
	 	if(containRDFType(r.get_body())){
			for(int i=0;i<GILPSettings.NUM_RDFTYPE_PARTITIONS;i++){
				String newPred = "rdftype" + i; 
				Clause convertedCls = replaceRDFType(r.get_body(), newPred);
				String sql = this.buildSQL(convertedCls);
				sql =  sql.substring(sql.indexOf("from"));		
				sql = sel + sql;				
				if (estimateCompareHC(sql))
					return true;
			}
			return false;
		}
		else{
			String sql = this.buildSQL(r.get_body());
			// replace the Select part in the SQL
			sql = sql.substring(sql.indexOf("from"));
			sql = sel + sql;				
			return estimateCompareHC(sql);
			//String rlt = DBController.getSingleValue(sql);
			//return Integer.parseInt(rlt);			
		}
	}
	
	//estimate whether the result of the sql is larger than min_HC
	//this function is useful if the result of sql is too large
	private boolean estimateCompareHC(String sql){
		//idea  1. sql = sql + limit 100*min_HC
		//select distinct * from (sql) as temp;
		//excec the query and check whether the num of results is large enough
		sql = sql + " limit " + 100*GILPSettings.MINIMUM_HC; 
		sql = "(" + sql + ")"; 
		sql = "select distinct * from " + sql + " as temp"; 
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;				 
		int num = 0;
		try
		{
			conn = DBController.getConn();
			pstmt = conn.prepareStatement(sql);		 
			rs = pstmt.executeQuery();			 
			while(rs.next()){
				num ++;
				if (num>=GILPSettings.MINIMUM_HC)
					break;
			}
				
		}catch (Exception e)
		{
			GILPSettings.log(this.getClass().getName() + "Error! ");
			e.printStackTrace();
			return false;
		}finally
		{
			DBPool.closeAll(conn, pstmt, rs);
		}	
		
		return (num>=GILPSettings.MINIMUM_HC);
	}
	
	private String constructStrAgg(RDFRuleImpl r, RDFPredicate tp){
		String aggregate_att = tp.getPredicateName() + "_1."; 		
		JoinType[] jts = r.findJoinType(tp); 
		String joinedPositionInTP = "S";
		if(jts != null){
			if (jts[0] == JoinType.OO || jts[0] == JoinType.SO){
				joinedPositionInTP = "O";
			}
		}
		else{
			GILPSettings.log(this.getClass().getName() + " cannot find matched joins.");
			return null;
		}
		if (joinedPositionInTP=="S"){
			if (tp.isObjectNumeric())
				aggregate_att += "num_o";
			else
				aggregate_att += "O";
		}
		else
			aggregate_att += "S";
		return aggregate_att;
	}
	
	//get the results of true positives in @fb join @r.body (KB)
	//example @fb: hasGivenName (YM, Y) incorrect, hasFamilyName(YM, M) incorrect, hasGivenName(BO, B) correct
	//@r: hasGivenName(?s1, ?o1) and rdfType(?s1, person) -> incorrect_hasGivenName(?s1, ?o1)
	//@return select t.s, t.o, r.s, r.o
	//from temp_tab as t, rdfType as r 
	//where t.s=r.s and r.o='pernson' 
	//here temp_tab (s, o) stores one tuple hasGivenName (YM, Y)
	public RDFSubGraphSet getFBJoinRule(Feedback fb, RDFRuleImpl r){
		//1. create temp_tab (tid, s, o) : the tid attr is not useful, just to be aligned to other predicates
		//2. store the feedbacks into temp_tab
		//3. replace the head_predicate in the body by temp_tab
		//4. do the query and mount the sg-set
		
		//1. create temp_tab (tid, s, o) 
		String temp_table = "temp_FBJoinRule";
		String attr_type = "character varying(1024)";
		
		if(!DBController.drop_tab(temp_table)){
			GILPSettings.log(this.getClass() + " Error! Cannot drop the table " + temp_table);
			return null;
		}
		
		String sql  = "create table " + temp_table ; 
		sql += "(tid int, S " + attr_type + ", O " + attr_type + ")"; 
		if(!DBController.exec_update(sql)){
			GILPSettings.log(this.getClass() + " Error! Cannot create the table " + temp_table);
			return null;
		}
		
		//2. store the feedbacks
		RDFPredicate head_tp = (RDFPredicate)(r.get_head().clone());
		String head_name = head_tp.mapToOriginalPred().getPredicateName(); 
		ArrayList<Comment> listCmts = fb.get_comments(); 
		for (Comment cmt: listCmts){
			if(cmt.get_triple().get_predicate().equals(head_name)){
				if (cmt.get_decision() == r.isInclusive()){
					sql = "insert into " + temp_table + " values(-1, " ;//-1 is a fake tid
					sql += "'" + cmt.get_triple()._subject + "','" + cmt.get_triple()._obj + "')"; 
					if(!DBController.exec_update(sql)){
						GILPSettings.log(this.getClass() + " Error! Cannot insert a triple " + cmt.get_triple());
						return null;
					}	
				}				
			}
		}
		
		//3. replace the head_predicate in the body by temp_tab
		RDFRuleImpl r1 = r.clone();
		HashMap<String, String> hmapPredNames = r1.getRenamedPredicates();
		Iterator<Predicate> myIter = r1.get_body().getIterator(); 
		while(myIter.hasNext()){
			RDFPredicate tp =  (RDFPredicate) myIter.next(); 
			String tp_rename = hmapPredNames.get(tp.toString()); 
			String head_rename = hmapPredNames.get(head_tp.toString()); 
			if(tp_rename.equals(head_rename)){
				tp.setPredicateName(temp_table);
				break; 
			}
		}
		
		//4. do the query 
		sql = buildSQL(r1.get_body()); 
		
		return doQuery(r.get_body(), sql);
	}
	
	
	
	public double getPHatForSingleAtom(RulePackage rp, RDFPredicate tp) {
		// example:
		// r0: hasGivenName(x, y) and rdftype(x, China) -> incorrect_hasGivenName(x, y)
		// tp: livesIn(x, Beijing)
		// sql: select count(distinct hasGivenName_1_S || '-' ||
		// hasGivenName_1_O) as pHat
		// from temp_tab_F0G0 as temp, livesIn as ex
		// where hasGivenName_1_S=ex.S
		// and  ex.O='Beijing'
		
		RDFRuleImpl r0 = rp.getRule();
		RDFPredicate head = (RDFPredicate)r0.get_head();
		HashMap<String, String> hmapPredNames = r0.getRenamedPredicates();
		String head_rn = hmapPredNames.get(head.toString());
		
		String sql_wh = "where "; 
		String sql_sel = " ";
		String str_count = ""; 
		
		boolean find_join = false;
		Iterator<Predicate> myIter = r0.get_body().getIterator(); 
		JoinType[] jts = null;
		while(myIter.hasNext()){
			RDFPredicate p = (RDFPredicate) myIter.next(); 
			String pred_rn = hmapPredNames.get(p.toString());
			if (!find_join){
		    	jts = RDFPredicate.getJoinTypes(p, tp);
				if(jts!=null){					
					switch(jts[0]){
					case 
						SS: sql_wh += pred_rn + "_S=ex.S ";    
						break; 
					case 
						SO: sql_wh += pred_rn + "_S=ex.O ";
						break;
					case OS:
						sql_wh += pred_rn + "_O=ex.S ";
						break; 
					case OO:
						sql_wh += pred_rn + "_O=ex.O ";
						break;
					}				
					find_join = true;
				}				
		    }
			str_count = " count(distinct " + pred_rn + "_S || '-' ||" + pred_rn + "_O)";
		    if(pred_rn.equals(head_rn)){
		    	sql_sel += str_count + " as pHat "; 
		    	if(find_join) 
		    		break;
		    }			
		}		
		
		if(!find_join){
			GILPSettings.log(this.getClass() + " cannot find join." + r0.toString() + " || " + tp.toString());
			return -1;
		}
		
		if(!tp.isSubjectVariable()){
			sql_wh += " and ex.S='" + tp.getSubject() + "'";
		}
		if(!tp.isObjectVariable()){
			if (tp.isObjectNumeric())
				sql_wh += " and ex.num_o between " + tp.getObjBounds()[0] + " and " + tp.getObjBounds()[1];
			else
				sql_wh += " and ex.O='" + tp.getObject() + "' ";
		}
		
		
		String sql_from = " from " + GILPSettings.TEMP_TABLE_F0R0 + ", " + tp.getPredicateName() + " as ex "; 
		String sql = "select " + sql_sel + sql_from + sql_wh;
		
		//System.out.println(sql);
		
		String rlt = DBController.getSingleValue(sql);
				
		return Double.parseDouble(rlt);
	}
	
	private boolean getPHatForExpandedRule(RulePackage rp, RDFPredicate tp, ArrayList<KVPair<String, Integer>> listPHats, HashMap<String,Integer> hmapNHats){
		//example:
		// r0: hasGivenName(x, y) and rdftype(x, China) -> incorrect_hasGivenName(x, y)
		// tp: livesIn(x, z)
		//sql: select ex.O,  count(distinct hasGivenName_1_S || '-' ||  hasGivenName_1_O) as pHat
		//	   from temp_tab_F0G0 as temp, livesIn as ex
		//	   where hasGivenName_1_S=ex.S
		//	   group by ex.O 
		//	   having count(distinct hasGivenName_1_S || '-' ||  hasGivenName_1_O)> rp._P0
		
		//step -1. The join results of FB and r0 should be stored in the table GILPSettings.TEMP_TABLE_F0R0
	
		RDFRuleImpl r0 = rp.getRule();
		RDFPredicate head = (RDFPredicate)r0.get_head();
		HashMap<String, String> hmapPredNames = r0.getRenamedPredicates();
		String head_rn = hmapPredNames.get(head.toString());
		
		String sql_wh = "where "; 
		String sql_sel = " ";
		String str_count = ""; 
		
		boolean find_join = false;
		Iterator<Predicate> myIter = r0.get_body().getIterator(); 
		JoinType[] jts = null;
		while(myIter.hasNext()){
			RDFPredicate p = (RDFPredicate) myIter.next(); 
			String pred_rn = hmapPredNames.get(p.toString());
			if (!find_join){
		    	jts = RDFPredicate.getJoinTypes(p, tp);
				if(jts!=null){					
					switch(jts[0]){
					case 
						SS: sql_wh += pred_rn + "_S=ex.S ";    
						break; 
					case 
						SO: sql_wh += pred_rn + "_S=ex.O ";
						break;
					case OS:
						sql_wh += pred_rn + "_O=ex.S ";
						break; 
					case OO:
						sql_wh += pred_rn + "_O=ex.O ";
						break;
					}				
					find_join = true;
				}				
		    }
			str_count = " count(distinct " + pred_rn + "_S || '-' ||" + pred_rn + "_O)";
		    if(pred_rn.equals(head_rn)){
		    	sql_sel += str_count + " as pHat "; 
		    	if(find_join) 
		    		break;
		    }			
		}
		
		/*if (sql_wh.length()<10){
			System.out.println(r0.toString() + " || " + tp.toString());
			return getPHatForExpandedRule(rp, tp, listPHats, hmapNHats);
		}*/
		
		if(!find_join){
			GILPSettings.log(this.getClass() + "cannot find join." + r0.toString() + " || " + tp.toString());
			return false;
		}
		
		String sql_from = " from " + GILPSettings.TEMP_TABLE_F0R0 + ", " + tp.getPredicateName() + " as ex "; 
		
		 
		//compute p_hats and n_hats for variable atom
		String sql = "select " + sql_sel + sql_from + sql_wh;
		//System.out.println(sql);
		ArrayList<ArrayList<String>> listTuples = DBController.getTuples(sql);
		if (listTuples == null)
			return true;
				
		ArrayList<String> tuple = listTuples.get(0);
		int p_hat = Integer.parseInt(tuple.get(0));
		int n_hat = 0;
		listPHats.add(new KVPair<String, Integer>("--variable--", p_hat)); 
		hmapNHats.put("--variable--", n_hat);
		
		//construct constant atoms
		
		String aggregate_att = constructStrAgg(r0, tp);
		aggregate_att = aggregate_att.substring(aggregate_att.indexOf("."));
		aggregate_att = "ex" + aggregate_att; 
		
		sql_sel = "select " + aggregate_att + ", " + sql_sel; 
		sql = sql_sel + sql_from + sql_wh; 
		sql += " group by " + aggregate_att; 
		sql += " order by pHat desc";
		//sql += " having " + str_count + ">" + (rp.getP0()-0.01); 
		//System.out.println(sql);
		
			
		listTuples = DBController.getTuples(sql);
		if (listTuples == null)
			return true;
		
		GILPSettings.log("getPhat:" + rp.getRule().hashCode() + ":# of chosen atoms:" + listTuples.size() + ":" + rp.getRule());
		int num = 0;
		//constant, PHat, COV
		for(ArrayList<String> tuple1: listTuples){
			String val = tuple1.get(0);
			if (p_hat<rp.getP0()){	
				break;
			}
			num++;
			p_hat = Integer.parseInt(tuple1.get(1));
			n_hat = 0;
			listPHats.add(new KVPair<String, Integer>(val, p_hat)); 
			hmapNHats.put(val, n_hat);
		}
		GILPSettings.log("getPhat:" + rp.getRule().hashCode() + ":# of qualified atoms:" + num + ":" + rp.getRule());		
		return true;
	}
	
	private boolean getPHatForExpandedRule_old(RulePackage rp, RDFPredicate tp, ArrayList<KVPair<String, Integer>> listPHats, HashMap<String,Integer> hmapNHats){
		//Step 1. Create temple table, store the true positives in current feedbacks
		//Step 2. build the expanded rule and build a SQL for it
		//Step 3. replace the 'head' table in SQL by temp table
		//Step 4. construct the select and aggreate part of SQL 
		/*Example 
		 * r0 :   hasGivenName(X, Y) AND wasLivenIn(X, Shanghai) --> incorrect_hasGivenName(X, Y)
		 * tp:  rdftype(X, Z) 
		 * head table is : hasGivenName
		 * create table  temp_triples_by_rule
		 * insert all true positives of r0 inside rp.fb 
		 * build the expanded rule and the corresponding SQL 
		 * select * from hasGivenName_1, wasLivenIn_1,  rdftype_1 where hasGivenName_1.s=wasLivenIn_1.s
		 * and hasGivenName_1.s = rdftype_1.s
		 * replace all hasGivenName_1 by temp_triples_by_rule
		 * select * from temp_triples_by_rule, wasLivenIn_1,  rdftype_1 where temp_triples_by_rule.s=wasLivenIn_1.s
		 * and temp_triples_by_rule.s = rdftype_1.s
		 * construct the select and aggreate part of SQL 
		 * select rdftype.O, count(temp_triples_by_rule.*) as PHat from temp_triples_by_rule, wasLivenIn_1,  rdftype_1 where temp_triples_by_rule.s=wasLivenIn_1.s
		 * and temp_triples_by_rule.s = rdftype_1.s
		 * group by rdftype.O
		 * 
		 * */
		Feedback fb = rp.getFeedback();
		RDFRuleImpl r0 = rp.getRule();
		
		String temp_table = "temp_triples_by_rule";
		//1. build the temporary table 
		if (!DBController.drop_tab(temp_table)){
			GILPSettings.log(this.getClass().getName() + " there is error when dropping table " + temp_table + ".");
			return false;
		}
		
		String sql = "create table " + temp_table + "(";		
		sql += "S character varying(1024),"; 
		sql += "O character varying(1024)"; 
		sql += ")"; 
		
		if (!DBController.exec_update(sql)){
			GILPSettings.log(this.getClass().getName() + " there is error when creating table " + temp_table );
			return false;
		}
		
		for(Comment cmt : rp.getFeedback().get_comments()){
			if (r0.coversComment(cmt)>0){
				Triple t = cmt.get_triple(); 
				sql = "insert into " + temp_table + " values("; 
				sql += "'" + t.get_subject() + "', '" + t.get_obj() + "' "; 
				sql += ")"; 
				
				//System.out.println(sql);
				
				if (!DBController.exec_update(sql)){
					GILPSettings.log(this.getClass().getName() + " there is error when inserting tuples into table consistent.");
					return false;
				}	
			}
		}
		
		//Step 2. build the expanded rule and build a SQL for it
		
		RDFRuleImpl r1 = r0.clone();
		r1.get_body().addPredicate(tp);
		
		RDFPredicate head = (RDFPredicate) r1.get_head();
		String head_relation = head.mapToOriginalPred().getPredicateName();
		
		Iterator<Predicate> myIter = r1.get_body().getIterator();
		while(myIter.hasNext()){
			Predicate p = myIter.next();
			if (p.getPredicateName().equals(head_relation)){
				p.setPredicateName(temp_table);
				break;
			}
		}
		
		sql = buildSQL(r1.get_body());
		
		
		
		//need to handle the case : hasGivenName(?s1,?o1),rdfslabel(?s2,?o1),hasGivenName(?s4,?o1)->incorrect_hasGivenName(?s1,?o1)
		//Step 3. replace the 'head' table in SQL by temp table
		//sql = sql.replaceAll(head_relation + "_1", temp_table + "_1");
		//sql = sql.replaceFirst(head_relation, temp_table);
		//head_relation = head_relation + "_1";// the relations will be renamed in the buildSQL
		//sql = sql.replaceAll(head_relation, temp_table);
		
		
		
		//Step 4. construct the select and aggreate part of SQL 
		String sel = "select  count( distinct " + temp_table + "_1.*) as pHat "; 
		sql = sql.substring(sql.indexOf("from")); 
		sql = sel + sql; 		
		
		//compute p_hats and n_hats for variable atom
		ArrayList<ArrayList<String>> listTuples = DBController.getTuples(sql);
		if (listTuples == null)
			return true;
		
		ArrayList<String> tuple = listTuples.get(0);
		int p_hat = Integer.parseInt(tuple.get(0));
		int n_hat = 0;
		listPHats.add(new KVPair<String, Integer>("--variable--", p_hat)); 
		hmapNHats.put("--variable--", n_hat);
		
		//compute p_hats and n_hats for constant atoms
		String aggregate_att = constructStrAgg(r0, tp);
		if (aggregate_att==null){
			GILPSettings.log(this.getClass().getName() + " cannot find matched joins.");
			return false;
		}
		sel = sel.replaceFirst("select", "select " + aggregate_att + " , "); 
		sql = sql.substring(sql.indexOf("from")); 
		sql = sel + sql; 
		String agg = " group by " + aggregate_att; 
		sql = sql + agg; 		
		sql += " order by pHat desc";
		
		listTuples = DBController.getTuples(sql);
		if (listTuples == null)
			return true;
		
		//constant, PHat, COV
		for(ArrayList<String> tuple1: listTuples){
			String val = tuple1.get(0);
			p_hat = Integer.parseInt(tuple1.get(1));
			n_hat = 0;
			listPHats.add(new KVPair<String, Integer>(val, p_hat)); 
			hmapNHats.put(val, n_hat);
		}
				
		return true;
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
		if (rp.isExtended()){
			return this.getPHatForExpandedRule(rp, tp, listPHats, hmapNHats);
		}
		
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
		if (joinedPositionInTP=="S"){
			if (tp.isObjectNumeric())
				aggregate_att += "num_o";
			else
				aggregate_att += "O";
		}
		else
			aggregate_att += "S"; 
		
		String str_sel = "select count( distinct "; 
		RDFPredicate head = (RDFPredicate)r0.get_head();
		if (head.isSubjectVariable())
			str_sel += head.getPredicateName() + "_S";
		if (head.isObjectVariable())
			str_sel += " || '-' || " + head.getPredicateName() + "_O"; 
		
		str_sel += ") as PHat, count( distinct ";  
		
		for (int i=0;i<head_vars.size();i++){
			String v = head_vars.get(i);
			if (i>0)
				str_sel += " || '-' || "; 
			String attr = hmap_headvar_attr.get(v);
			str_sel += attr; 
		}
		str_sel += ") as COV ";
		
		String str_from = " from " + temp_table +" , " + tp.getPredicateName();
		String str_where = " where " + temp_table + "." + joinedPropInRule + "=" + tp.getPredicateName() + "." + joinedPositionInTP;
		
		if (joinedPositionInTP=="S" && !tp.isSubjectVariable()){
			str_where += " and " + tp.getPredicateName() + ".S='" + joinedArgument + "'";
		}
		if (joinedPositionInTP=="O" && !tp.isObjectVariable()){
			str_where += " and " + tp.getPredicateName() + ".O='" + joinedArgument + "'";
		}
		
		String str_group = " group by " + aggregate_att; 
		String str_order = " order by PHat desc, COV" ; 
		
		//compute p_hats and n_hats for variable atom
		sql = str_sel + str_from + str_where; 
		//System.out.println(sql);
		ArrayList<ArrayList<String>> listTuples = DBController.getTuples(sql);
		if (listTuples == null)
			return true;
		
		ArrayList<String> tuple = listTuples.get(0);
		int p_hat = Integer.parseInt(tuple.get(0));
		int n_hat = Integer.parseInt(tuple.get(1)) - p_hat;
		listPHats.add(new KVPair<String, Integer>("--variable--", p_hat)); 
		hmapNHats.put("--variable--", n_hat);
		
		//compute p_hats and n_hats for constant atoms
		str_sel = str_sel.replaceFirst("select", "select " + aggregate_att + " , "); 
		sql = str_sel + str_from + str_where + str_group + str_order;
		//System.out.println(sql);
		
		listTuples = DBController.getTuples(sql);
		if (listTuples == null)
			return true;
		
		//constant, PHat, COV
		for(ArrayList<String> tuple1: listTuples){
			String val = tuple1.get(0);
			p_hat = Integer.parseInt(tuple1.get(1));
			n_hat = Integer.parseInt(tuple1.get(2)) - p_hat;
			listPHats.add(new KVPair<String, Integer>(val, p_hat)); 
			hmapNHats.put(val, n_hat);
		}
		return true;
	}
	
	boolean containRDFType(Clause cls){
		Iterator<Predicate> iter = cls.getIterator();
		while(iter.hasNext()){
			if(iter.next().getPredicateName().equalsIgnoreCase("rdftype")){
				return true;
			}
		}
		return false;
	}
	
	Clause replaceRDFType(Clause cls, String newPred){
		Clause newCls = cls.clone();
		Iterator<Predicate> iter = newCls.getIterator();
		while(iter.hasNext()){
			Predicate p = iter.next();
			if(p.getPredicateName().equalsIgnoreCase("rdftype")){
				p.setPredicateName(newPred);
			}
		}
		return newCls;
	}
	
	public RDFSubGraphSet getTriplesByCNF(Clause cls, int num){
		if(containRDFType(cls)){
			RDFSubGraphSet rltSet = new RDFSubGraphSet();
			for(int i=0;i<GILPSettings.NUM_RDFTYPE_PARTITIONS;i++){
				String newPred = "rdftype" + i; 
				Clause convertedCls = replaceRDFType(cls, newPred);
				String sql = this.buildSQL(convertedCls);
				sql += "  limit " + num; 
				RDFSubGraphSet oneSet = doQuery(cls, sql);
				if (i==0)
					rltSet.setPredicates(oneSet.getPredicates());
				for (RDFSubGraph sg: oneSet.getSubGraphs())
					rltSet.addSubGraph(sg);
			}
			//do sampling
			if(rltSet.getSubGraphs().size()>num){
				int s = rltSet.getSubGraphs().size();
				int[] isChosen = new int[s];
				for (int i = 0; i < s; i++) {
					isChosen[i] = 0;
				}
				ArrayList<RDFSubGraph> listChosenSGs = new ArrayList<>();
				while (listChosenSGs.size() < Math.min(num, s)) {
					int idx = (int) Math.round(Math.random() * (s - 1));
					if (isChosen[idx] == 0) {
						RDFSubGraph sg = rltSet.getSubGraphs().get(idx);
						isChosen[idx] = 1;
						listChosenSGs.add(sg);
					}
				}
				rltSet.getSubGraphs().clear();
				rltSet.getSubGraphs().addAll(listChosenSGs);
			}
			return rltSet;
		}
		else{		
			String sql = this.buildSQL(cls);
			sql += "  limit " + num; 
			return doQuery(cls, sql);
		}
	}

	@Override
	public RDFSubGraphSet getTriplesByCNF(Clause cls) {
		if(containRDFType(cls)){
			RDFSubGraphSet rltSet = new RDFSubGraphSet();
			for(int i=0;i<GILPSettings.NUM_RDFTYPE_PARTITIONS;i++){
				String newPred = "rdftype" + i; 
				Clause convertedCls = replaceRDFType(cls, newPred);
				String sql = this.buildSQL(convertedCls);
				RDFSubGraphSet oneSet = doQuery(cls, sql);
				if (i==0)
					rltSet.setPredicates(oneSet.getPredicates());
				for (RDFSubGraph sg: oneSet.getSubGraphs())
					rltSet.addSubGraph(sg);
			}			
			return rltSet;
		}
		else{
			String sql = this.buildSQL(cls);
			return doQuery(cls, sql);
		}
	}
		
	// get at most @num sub-graphs
	private RDFSubGraphSet doQuery(Clause cls, String query) {
		// execute the SPARQL		
		long time0 = System.currentTimeMillis();
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
		long time1 = System.currentTimeMillis();
		if(time1-time0>3000){
			GILPSettings.log("expensive query:" + (time1-time0) +  ":" + query); 
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
	
	static void testHeadCoverage(){
		Clause cls1 = new ClauseSimpleImpl();
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o"));
		cls1.addPredicate(tp);
	 
		tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("rdftype");
		tp.setObject(new String("wikicat_Chinese_people"));		
		cls1.addPredicate(tp);
		
		RDFRuleImpl r1 = new RDFRuleImpl();
		r1.set_body(cls1);
		tp = new RDFPredicate();		
		tp.setSubject(new String("?s"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o"));
		tp = tp.mapToIncorrectPred();
		r1.set_head(tp);
		
		PGEngine pg = new PGEngine();
		ArrayList<Triple> triples = pg.getHeadCoverage(r1, 10);
		for (Triple t:triples){
			System.out.println(t);
		}
		if(pg.isLargerThanMinHC(r1))
			System.out.println("HC is large enough");
		
		
		Clause cls2 = new ClauseSimpleImpl();
		tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o"));
		cls2.addPredicate(tp);
	 
		tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("rdftype");
		tp.setObject(new String("wordnet_person_100007846"));		
		cls2.addPredicate(tp);
		
		RDFRuleImpl r2 = new RDFRuleImpl();
		r2.set_body(cls2);
		tp = new RDFPredicate();		
		tp.setSubject(new String("?s"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o"));
		tp = tp.mapToIncorrectPred();
		r2.set_head(tp);
		
		double containedPr= pg.getHCContainedPr(r1, r2); 
		System.out.println("r1/r2: " + containedPr);
		
		containedPr= pg.getHCContainedPr(r2, r1); 
		System.out.println("r2/r1: " + containedPr);
		
	}
	
	static void testSimpleQuery(){
		Clause cls = new ClauseSimpleImpl();
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o"));
		cls.addPredicate(tp);
	 
		tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("rdftype");
		tp.setObject(new String("wikicat_Chinese_people"));		
		cls.addPredicate(tp);
		
		PGEngine pg = new PGEngine();
		System.out.println(pg.buildSQL(cls));
		
		
		System.out.println("#######################################################");
		try {
			System.out.println("query is:" + cls.toString());
			RDFSubGraphSet rlt = pg.getTriplesByCNF(cls);
			System.out.println("There are " + rlt.getSubGraphs().size() + " results:");
			if (rlt!=null){
				ArrayList<Triple> triples = rlt.getTriplesByPredicate("hasGivenName");
				RandomAccessFile file = new RandomAccessFile("/home/jchen/gilp/chinese_persons.txt","rw");
				
				for (Triple t: triples){
					String str = t.toString();
					str = str.replaceAll("<", "");
					str = str.replaceAll(">", "");
					file.writeBytes(str + " -1\n");
					System.out.println(t);
				}
				//for (RDFSubGraph twig: rlt.getSubGraphs()){
				//	System.out.println(twig.toString());
				//}
				file.close();
			}
			else{
				System.out.println("Empty results set.");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.out);
		}
		
	}
	
	private static RDFRuleImpl construct_rule(){
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
		
		return r0; 
	}
	
	private static Feedback construct_fb(){
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
		return fb;
	}
	
	static void testGetFBJoinRule(){

		PGEngine pg = new PGEngine();
		
		Feedback fb = construct_fb(); 
		RDFRuleImpl r0 = construct_rule(); 
		RDFSubGraphSet sg_set = pg.getFBJoinRule(fb, r0); 
		
		for(RDFSubGraph sg:sg_set.getSubGraphs()){
			System.out.println(sg);
		}
	}
	
	static void testPNHats(){
		
		PGEngine pg = new PGEngine();
		
		Feedback fb = construct_fb(); 
		RDFRuleImpl r0 = construct_rule(); 
		
		RDFPredicate ex_tp = new RDFPredicate(); 
		ex_tp.setPredicateName("hasFamilyName");
		ex_tp.setSubject("?s1");
		ex_tp.setObject("?o2");
		
		ArrayList<KVPair<String, Integer>> listPHats = new ArrayList<>(); 
		HashMap<String, Integer> hmapNHats = new HashMap<>();
		
		RulePackage rp = new RulePackage(r0, fb, null); 
		rp.setExtended(true);
		pg.getPHatNhats(rp, ex_tp, listPHats, hmapNHats); 
		
		for (KVPair<String, Integer> kv: listPHats){
			String val = kv.get_key();
			int p_hat = kv.get_value();
			int n_hat = hmapNHats.get(val);
			System.out.println(val + "|" + p_hat + "|" + n_hat);
		}
	}
	
	public static void main(String[] args){
		testGetFBJoinRule();		
	}

}
