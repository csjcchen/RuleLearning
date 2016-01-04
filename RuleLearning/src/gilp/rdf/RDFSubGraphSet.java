package gilp.rdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import gilp.rule.RDFPredicate;

public class RDFSubGraphSet {
	private ArrayList<RDFPredicate> _predicates;	
	private ArrayList<RDFSubGraph> _sub_graphs; 
	private int[][] _join_conditions; 
	
	public RDFSubGraphSet(){
		_predicates =new ArrayList<RDFPredicate>();
		_sub_graphs = new ArrayList<RDFSubGraph>();
	}	
	public void setPredicates(ArrayList<RDFPredicate> preds){
		this._predicates.addAll(preds);
		_join_conditions = new int[_predicates.size()][_predicates.size()];
	}
	
	public void addSubGraph(RDFSubGraph sg){
		_sub_graphs.add(sg);
	}	
	
	public ArrayList<RDFPredicate> getPredicates(){
		return this._predicates;
	}	
	//find all triples in this graph-set all with an identical predicate as specified by the @pred_name
	public ArrayList<Triple> getTriplesByPredicate(String pred_name){
		ArrayList<Triple> listRlts =  new ArrayList<Triple>(); 
		for (RDFSubGraph sg: this.getSubGraphs()){
			for (Triple t: sg.getTriples()){
				if (t.get_predicate().equals(pred_name)){
					listRlts.add(t.clone());
				}
			}
		}
		return listRlts;
	} 	
	
	//@return:
	//0 not connected; 1 s-s; 2 s-o; 3 o-s; 4 o-o 
	public int getJoinRelation(int pattern_idx_1, int pattern_idx_2){
		return _join_conditions[pattern_idx_1][pattern_idx_2];
	}
	
	public void setJoinRelation(int pattern_idx_1, int pattern_idx_2, int type){
		_join_conditions[pattern_idx_1][pattern_idx_2] = type;
	}	
	public ArrayList<RDFSubGraph> getSubGraphs(){
		return this._sub_graphs;
	}
	//get all distinct triples contained in this sub-graph-set
	public ArrayList<Triple> getAllTriples(){
		ArrayList<Triple> listRlts = new ArrayList<Triple>();
		for (RDFSubGraph sg: this._sub_graphs){
			for(Triple t: sg.getTriples()){
				if(!listRlts.contains(t))
					listRlts.add(t);
			}
		}
		return listRlts;
	} 
}

/*
 * 
 * 
 *
 //join two sets of twigs @sg_set1 and @sg_set2
	//the predicates to be joined locating at the @joinPre1 and @joinPre2 
	//and the jointype is given in @join_type
	public static RDFSubGraphSet joinTwigSets(RDFSubGraphSet sg_set1, int joinPre1, 
			RDFSubGraphSet sg_set2, int joinPre2, JoinType join_type){
		RDFSubGraphSet rlt = new RDFSubGraphSet(); 
		ArrayList<RDFPredicate> rlt_predicates = new ArrayList<RDFPredicate>(); 
		rlt_predicates.addAll(sg_set1.getPredicates()); 
		rlt_predicates.addAll(sg_set2.getPredicates()); 
		rlt.setPredicates(rlt_predicates);
		
		for (RDFSubGraph sg1: sg_set1.getSubGraphs()){
			Triple t1 = sg1.getTriples().get(joinPre1);
			boolean can_join = false;
			for (RDFSubGraph sg2: sg_set2.getSubGraphs()){
				Triple t2 = sg2.getTriples().get(joinPre2);
				switch(join_type){
				case SS: 
					can_join = (t1.get_subject().equals(t2.get_subject()));
					break; 
				case SO:
					can_join = (t1.get_subject().equals(t2.get_obj()));
					break;
				case OS:
					can_join = (t1.get_obj().equals(t2.get_subject()));
					break;
				case OO:
					can_join = (t1.get_obj().equals(t2.get_obj()));
					break;
				}
				if (can_join){
					RDFSubGraph new_sg = new RDFSubGraph();
					new_sg.getTriples().addAll(sg1.getTriples()); 
					new_sg.getTriples().addAll(sg2.getTriples()); 
					rlt.addSubGraph(new_sg);
				}
			}
		}
		return rlt;
	}
	
 
 
 //get the set of distinct bindings for the given variables
	public ArrayList<ArrayList<String>> getDistinct(ArrayList<String> vars){
		HashMap<String, Integer> hmap_vars = new HashMap<>(); 
		//store the index of variables in @vars
		for(int i=0;i<vars.size();i++)
			hmap_vars.put(vars.get(i), i); 
		
		HashMap<String, String[]> hmap_rlts = new HashMap<>(); 
		for(RDFSubGraph sg: this._sub_graphs){
			String[] binding = new String[vars.size()]; 
			for (int i=0;i<this._predicates.size();i++){
				//e.g. tp = hasGivenName(X,Y) t= hasGivenName(Yao_Ming, Yao) 
				//var = <Y,X> then binding is [Yao, Yao_Ming]
				
				RDFPredicate tp = this._predicates.get(i);
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
			//the hash key is the connection of strings in binding
			String key = "";
			for (String v:binding){
				key += v + "|";
			}
			hmap_rlts.put(key, binding);
		}
		
		//construct the result based on the intermediate result hmap_rlts
		ArrayList<ArrayList<String>> rlts = new ArrayList<>();
		Iterator<String[]> iter_bindings = hmap_rlts.values().iterator();
		while(iter_bindings.hasNext()){
			String[] binding = iter_bindings.next();
			ArrayList<String> temp_list =new ArrayList<>();
			for (String v:binding){
				temp_list.add(v);
			}
			rlts.add(temp_list);
		}		
		return rlts;
	}
	
 
 * */
