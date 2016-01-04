package gilp.rdf;

 
import java.sql.SQLException;

import de.mpii.rdf3x.Driver;
import gilp.learning.GILPSettings;
import gilp.rule.Clause;
import gilp.rule.ClauseSimpleImpl;
import gilp.rule.Predicate;
import gilp.rule.RDFPredicate;
import gilp.utility.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Properties;
 

public class RDF3XEngine implements QueryEngine{
	
	private static java.sql.Connection _con = null;
	
	private static final boolean KB_WITH_PREFIX = false;
	
	private static final String YAGO_PREFIX_RDF = "<http://www.w3.org/1999/02/22-rdf-syntax-ns#>"; 
	private static final String YAGO_PREFIX_RDFS = "<http://www.w3.org/2000/01/rdf-schema#>";
	private static final String YAGO_PREFIX_X = "<http://www.w3.org/2001/XMLSchema#>";
	private static final String YAGO_PREFIX_Y = "<http://mpii.de/yago/resource/>";
	private static final String YAGO_PREFIX_BASE = "<http://mpii.de/yago/resource/>";
	
	//to generate a query header when KB has prefix
	private String getQueryHeader(){
		String header = "PREFIX rdf:" + RDF3XEngine.YAGO_PREFIX_RDF;
		header += " PREFIX  rdfs:" +  RDF3XEngine.YAGO_PREFIX_RDFS;
		header += " PREFIX x:" + RDF3XEngine.YAGO_PREFIX_X;
		header += " PREFIX y:" + RDF3XEngine.YAGO_PREFIX_Y;
		return header;
	}
	
	void buildConn(){
		//Class.forName("com.mysql.jdbc.Driver");
		try {
			Properties prop = new Properties();
			prop.put("rdf3xembedded", GILPSettings.RDF3X_PATH + "rdf3xembedded");
			//DriverManager.getConnection(url, info);
			Driver myDriver = new Driver();
			RDF3XEngine._con = myDriver.connect("rdf3x://" + GILPSettings.RDF3X_PATH + GILPSettings.RDF3X_DBFILE , prop);
			//RDF3XEngine._con = myDriver.connect("rdf3x:///home/jinchuan/rdf3x-0.3.8/rdf3x-0.3.7/bin/yago_persons",prop);
			
		} catch (SQLException e) { 
			e.printStackTrace(System.out);
		}
		 
	}
	
	void reset(){
		try {
			RDF3XEngine._con.close();
			this.buildConn();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.out);
		}
	}

	@Override
	public Triple getTripleByTid(String tid) {
		// TODO Auto-generated method stub
		//RDF3X does not store triple id
		return null;
	}

	@Override
	public ArrayList<Triple> getTriplesBySubject(String subject) {
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String(subject));
		tp.setObject(new String("?y"));
		tp.setPredicateName("?p");
		return getTriples(tp);
	}	 

	@Override
	public ArrayList<Triple> getTriplesByPredicate(String predicate) {
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String("?x"));
		tp.setObject(new String("?y"));
		tp.setPredicateName(predicate);
		return getTriples(tp);
	}

	@Override
	public ArrayList<Triple> getAllTriples() {
		System.err.println(this.getClass().getName() + "# Feature not supported, because there are too many triples.");
		//Too many results
		return null;
	}
	
	
	private ArrayList<Triple> getTriples (RDFPredicate tp){
		
		Clause cls = new ClauseSimpleImpl();
		cls.addPredicate(tp);
		String query = buildSPARQL(cls, null);
		
		// execute the SPARQL
		if (RDF3XEngine._con == null) {
			this.buildConn();
		}
		ArrayList<Triple> triples = new ArrayList<Triple>();
		try {
			java.sql.Statement stat = ((de.mpii.rdf3x.Connection) _con).createStatement();

			java.sql.ResultSet rlt = stat.executeQuery(query);
			
			RDFSubGraphSet sg_set = mountSGSet(rlt, cls);
			for (RDFSubGraph sg : sg_set.getSubGraphs()){
				Triple t = sg.getTriples().get(0);//each sub-graph contains only one triple in this case
				triples.add(t);
			}
	 		return triples;
		} catch (SQLException e) {
			e.printStackTrace(System.out);
			return null;
		}
	}
	
	
	
	//transform a Clause into a SPARQL
	private String buildSPARQL(Clause cls, ArrayList<RDFFilter> listFilters ) {
		// sparql example: select * where {?s1 <hasGivenName> <Yao>. ?s1 ?y ?o1
		// .} 		 
		
		StringBuffer sb = new StringBuffer();
		if (RDF3XEngine.KB_WITH_PREFIX){
			sb.append(this.getQueryHeader()+ " ");
		}
		sb.append(" select * where {");
		
		Iterator<Predicate> myIter = cls.getIterator();
		while (myIter.hasNext()) {
			RDFPredicate tp = (RDFPredicate) myIter.next();
			
			if (RDF3XEngine.KB_WITH_PREFIX){
				 if (tp.isSubjectVariable())
					 sb.append(tp.getSubject() + " ");
				 else
					 sb.append(RDF3XEngine.YAGO_PREFIX_BASE.replace(">", "")).append(tp.getSubject()).append("> ");
				
				 if (tp.getPredicateName().startsWith("?"))
					 sb.append(tp.getPredicateName() + " ");
				 else
					 sb.append("y:").append(tp.getPredicateName() + " ");
				 //TODO need to handle different prefixes of the predicates, like y: and rdf: 
				 
				 if (tp.isObjectVariable())
					 sb.append(tp.getObject() + ". ");
				 else
					 sb.append("\"").append(tp.getObject()).append("\". ");
			}
			else{
				//in our current RDF3x data set, each constant is enclosed by <>
				if (!tp.isSubjectVariable())
					sb.append("<");
				sb.append(tp.getSubject());
				if (!tp.isSubjectVariable())
					sb.append("> ");
				else
					sb.append(" ");
				
				if (!tp.getPredicateName().startsWith("?"))
					sb.append("<");
				sb.append(tp.getPredicateName());
				if (!tp.getPredicateName().startsWith("?"))
					sb.append("> ");
				else
					sb.append(" ");
				
				if (!tp.isObjectVariable())
					sb.append("<");
				sb.append(tp.getObject());
				if (!tp.isObjectVariable())
					sb.append(">. ");
				else
					sb.append(". ");
			}			
		}
		if (listFilters!=null){
			//sb.append(" filter(?x = <Yao_Ming>)");
			for (RDFFilter filter: listFilters){
				if (RDF3XEngine.KB_WITH_PREFIX){
					sb.append(" filter(" + filter.get_variable() + filter.get_opt() + RDF3XEngine.YAGO_PREFIX_BASE.replace(">", "") + filter.get_value() + ">) ");
				}else{
					sb.append(" filter(" + filter.get_variable() + filter.get_opt() + "<" + filter.get_value() + ">) ");
				}
			}
		}
		sb.append("}");
		return sb.toString();
	}
	
	
	//execute a sparql query which must contain "count" in the select clause
	//only return the count value of the first result tuple
	public int getCount(String sparql){
		if (RDF3XEngine._con == null) {
			this.buildConn();
		}
		try {
			java.sql.Statement stat = ((de.mpii.rdf3x.Connection) _con).createStatement();

			java.sql.ResultSet rlt = stat.executeQuery(sparql);
						
			if(rlt.next()){
				String strVal = rlt.getString(2);
				int num = Integer.parseInt(strVal);
				rlt.close();
				return num; 				
			}
			else{
				rlt.close();
				return 0;
			}
			
		} catch (Exception e) {
			System.out.println(sparql);
			e.printStackTrace(System.out);
			return -1;
		}	
	}
	
	public RDFSubGraphSet getTriplesByCNF(Clause cls, ArrayList<RDFFilter> listFilters, int num){
		String query = buildSPARQL(cls, listFilters);		
		RDFSubGraphSet rlt = doQuery(cls, query, num);
		this.reset();
		return rlt;
	}

	public RDFSubGraphSet getTriplesByCNF(Clause cls, ArrayList<RDFFilter> listFilters){
		String query = buildSPARQL(cls, listFilters);
		//System.out.println(query);
		return doQuery(cls, query);		
	}
	
	@Override
	public RDFSubGraphSet getTriplesByCNF(Clause cls){
		return getTriplesByCNF(cls, null);
	}
	
	//get all sub-graphs
	private RDFSubGraphSet doQuery(Clause cls,String query){
		return doQuery(cls, query, -1);
	}
	
	//get at most @num sub-graphs
	private RDFSubGraphSet doQuery(Clause cls,String query, int num){
		// execute the SPARQL
		if (RDF3XEngine._con == null) {
			this.buildConn();
		}
		try {
			java.sql.Statement stat = ((de.mpii.rdf3x.Connection) _con).createStatement();
			de.mpii.rdf3x.Statement rdf3x_stat = (de.mpii.rdf3x.Statement)stat; 
			java.sql.ResultSet rlt = rdf3x_stat.executeQuery(query, num);
			//java.sql.ResultSet rlt = stat.executeQuery(query);
			
			return mountSGSet(rlt, cls, num);
		} catch (SQLException e) {
			e.printStackTrace(System.out);
			return null;
		}	
	} 
	
	//get all SubGraphs
	private RDFSubGraphSet mountSGSet(java.sql.ResultSet rlt, Clause cls){
		return mountSGSet(rlt, cls, -1);
	}
	
	//remore the prefix retruned by RDF3X
	private String removePrefix(String db_val){
		//example1 <http://mpii.de/yago/resource/Wolfgang_Preiss> --> Wolfgang_Preiss
		//example2 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> -->type
 		int idx = db_val.lastIndexOf("#");
 		if (idx < 0)
 			idx = db_val.lastIndexOf("/");
 		
		String rlt = db_val.substring(idx + 1);
		rlt = rlt.replace(">", "");
		return rlt;
	}
	
	//get at most @num sub-graphs
	//The results returned by RDF3x are variable instantiations, and not complete sub-graphs. 
	//This function will mount sug-graphs based on these variable instantiations and 
	//clause (pattern of the sub-graphs).  
	private RDFSubGraphSet mountSGSet(java.sql.ResultSet rlt, Clause cls, int num){
		//In results of RDF3x, the columns are sorted in lexical order of the variable names 
		//if the query is like select * . 
		
		//First, we need to compute the column index of each variable. 
		PriorityQueue<String> pqVars = new PriorityQueue<String>();		 
		Iterator<Predicate> myIter = cls.getIterator();
		ArrayList<RDFPredicate> preds = new ArrayList<RDFPredicate>();
						
		while( myIter.hasNext()){
			RDFPredicate tp = (RDFPredicate)myIter.next();
			preds.add(tp);
			if (tp.isSubjectVariable()){
				if (!pqVars.contains(tp.getSubject().toString())){
					pqVars.add(tp.getSubject().toString());
				}
			}
			if (tp.isObjectVariable()){
				if(!pqVars.contains(tp.getObject().toString())){
					pqVars.add(tp.getObject().toString());
				}
			}
			if(tp.isPredicateVariable()){
				if (!pqVars.contains(tp.getPredicateName())){
					pqVars.add(tp.getPredicateName());
				}
			}
		}
		//initialize the graph set
		RDFSubGraphSet sg_set = new RDFSubGraphSet();
		sg_set.setPredicates(preds);
		
		String[] var_names = pqVars.toArray(new String[0]);
		Arrays.sort(var_names);
			//the order generated by PriorityQueue is not good, thus we have to sort the var names again. 
		HashMap<String,Integer> hmapVars = new HashMap<String, Integer>();
		for(int i = 0;i<var_names.length;i++){
			hmapVars.put(var_names[i], i+1);
				//store i+1 instead of i, because the column index of java.sql.resultset starts from 1 while array starts from 0.
		}
		
		//for each result tuple, we mount a sub-graph
		try{			
			int count = 0;
			while(rlt.next()){
				RDFSubGraph sg = new RDFSubGraph();
				boolean meetError = false;
			 	for (int i=0;i<preds.size();i++){
					RDFPredicate tp = preds.get(i);
					Triple t = new Triple();
					if(tp.isPredicateVariable()){
						int idx = hmapVars.get(tp.getPredicateName());	
						String strVal = rlt.getString(idx);
						if (RDF3XEngine.KB_WITH_PREFIX)
							strVal = this.removePrefix(strVal);
						t.set_predicate(strVal);
					}
					else
						t.set_predicate(tp.getPredicateName());
					
					if(tp.isSubjectVariable()){
						int idx = hmapVars.get(tp.getSubject().toString());						
					    String strVal = rlt.getString(idx);
					    if (RDF3XEngine.KB_WITH_PREFIX)
					    	strVal = this.removePrefix(strVal);
					    
					    if (strVal!=null)
					    	t.set_subject(StringUtils.removePointBrackets(strVal));
					    else{ 
					    	meetError = true;
					    	break;
					    }
					}
					else
						t.set_subject(tp.getSubject().toString());
					
					if(tp.isObjectVariable()){
						int idx = hmapVars.get(tp.getObject().toString());	
						String strVal = rlt.getString(idx);
						if (RDF3XEngine.KB_WITH_PREFIX)
					    	strVal = this.removePrefix(strVal);
					    if (strVal!=null)
					    	t.set_obj(StringUtils.removePointBrackets(strVal));
					    else{ 
					    	meetError = true;
					    	break;
					    }
					}
					else
						t.set_obj(tp.getObject().toString());
					
					sg.addTriple(t);	
					
					if(num>0 && ++count>=num){
						break;
					}
				}  
			 	if (!meetError)
			 		sg_set.addSubGraph(sg);
			}
		}
		catch (SQLException e) { 
			e.printStackTrace(System.out);
			return null;
		}
		
		return sg_set;
	}
	
	public static void main(String[] args){
		Clause cls = new ClauseSimpleImpl();
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("Yao"));
		cls.addPredicate(tp);
	 
		/*tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("Yao"));		
		cls.addPredicate(tp);
*/
		ArrayList<RDFFilter> filters = null;
		//filters = new ArrayList<RDFFilter>(); 
		//RDFFilter f1 = new RDFFilter("?x",CompareOperator.EQUAL, "Victor_Ling");
		//RDFFilter f2 = new RDFFilter("?y",CompareOperator.EQUAL, "Victor");
		//<wasBornIn> <Shanghai>
		//filters.add(f1);
		//filters.add(f2);
		
		System.out.println("#######################################################");
		try {
			System.out.println("query is:" + cls.toString());
			RDFSubGraphSet rlt = new RDF3XEngine().getTriplesByCNF(cls,filters);
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
		
		/*System.out.println("#######################################################");
		System.out.println("query is: find all triples associated with the predicate <wasBornIn>");
		ArrayList<Triple> triples = new RDF3XEngine().getTriplesByPredicate("wasBornIn");
		System.out.println("the first 10 results:");
		if(triples!=null){
			int count = 0;
			for(Triple t: triples){
				System.out.println(t);
				if (++count>=10) break;
			}
		}	 
		else{
			System.out.println("Empty results set.");
		}*/
	}

}
