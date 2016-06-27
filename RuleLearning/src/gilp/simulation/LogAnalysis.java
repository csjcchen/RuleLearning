package gilp.simulation;

import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.StringTokenizer;

public class LogAnalysis {
	
	//# of rules requiring feedbacks
	//# avg. # of feedbacks pulled for each rule
	
	//avg. # of chosen atoms for a rule
	//avg. # of qualified atoms for a rule
	
	//avg. # of children rules for a rule
	//avg. # of qualified children rules for a rule
	
	static void pruningEffects(String fileName){
		RandomAccessFile file = null; 
		HashMap<Integer, Integer> hmapFeedbacks = new HashMap<>();
		HashMap<Integer, Integer> hmapAtoms = new HashMap<>(); 
		HashMap<Integer, Integer> hmapQualifiedAtoms = new HashMap<>(); 
		HashMap<Integer, Integer> hmapChildren = new HashMap<>();
		HashMap<Integer, Integer> hmapQualifiedChildren = new HashMap<>();
		int total_time_pull_fb = 0;
		
		try{
			file = new RandomAccessFile(fileName, "r"); 
			String line = ""; 
			while((line=file.readLine())!=null){
				//get the type of log
				String msgType = line.substring(line.indexOf("2016: ")+6); 
				
				if(msgType.startsWith("feedbacks")){
					int num = getNum(msgType); 
					int r_code = getRuleCode(msgType); 
					insertItem(hmapFeedbacks, r_code, num);
				}
				else if (msgType.startsWith("childrenRules")){
					int num = getNum(msgType); 
					int r_code = getRuleCode(msgType); 
					if(msgType.indexOf("qualified")>=0){						
						insertItem(hmapQualifiedChildren, r_code, num); 
					}
					else{
						insertItem(hmapChildren,r_code, num); 
					}
				}
				else if (msgType.startsWith("getPhat")){
					int num = getNum(msgType); 
					int r_code = getRuleCode(msgType); 
					if(msgType.indexOf("qualified")>=0){
						insertItem(hmapQualifiedAtoms,r_code, num); 
					}
					else{
						insertItem(hmapAtoms, r_code, num); 
					}
				}
				else if(msgType.startsWith("time cost in pulling feedbacks")){
					//time cost in pulling feedbacks:57011
					int num = Integer.parseInt(msgType.substring(msgType.lastIndexOf(":")+1)); 
					total_time_pull_fb += num;
				}
				else if (msgType.startsWith("time cost in phase two")){
					//time cost in phase two: 90912132
					int num = Integer.parseInt(msgType.substring(msgType.lastIndexOf(":")+1));
					System.out.println("the time cost in phase two: " + (num-total_time_pull_fb));					
				}
				else{
					System.out.println(line);
				}
			}
			file.close();
		}
		catch(Exception ex){
			ex.printStackTrace(System.out);
		}
		
		//calc. the average 
		System.out.println("# of rules requiring feedbacks:" + hmapFeedbacks.size());
		//System.out.println("# of total rules:" + hmapAtoms.size());
		System.out.println("avg. feedbacks:" + calcAverage(hmapFeedbacks)); 
		
		System.out.println("avg. atoms per rule:" + calcAverage(hmapAtoms));
		System.out.println("avg. qualified atoms per rule:" + calcAverage(hmapQualifiedAtoms));
		
		System.out.println("avg. children per rule:" + calcAverage(hmapChildren));
		System.out.println("avg. qualified children per rule:" + calcAverage(hmapQualifiedChildren));
	}
	
	private static void insertItem(HashMap<Integer, Integer> hmapStat, int r_code, int num){
		if (hmapStat.containsKey(r_code)){
			num += hmapStat.get(r_code);
		}
		hmapStat.put(r_code, num); 

	}
	
	private static double calcAverage(HashMap<Integer, Integer> hmapStat){
		double numEntries = hmapStat.size();
		double sum = 0; 
		for (int r_code: hmapStat.keySet()){
			int num = hmapStat.get(r_code); 
			sum += num;
		}
		return sum/numEntries;
	}
	
	private static int getRuleCode(String msg){
		//feedbacks:1336437944:2:hasFamilyName(?s1,?o1),hasGivenName(?s2,?o1)->incorrect_hasGivenName(?s2,?o1)
		//getPhat:1336437944:# of chosen atoms:0:hasFamilyName(?s1,?o1),hasGivenName(?s2,?o1)->incorrect_hasGivenName(?s2,?o1)
		StringTokenizer st = new StringTokenizer(msg, ":");
		st.nextToken();		
		
		return Integer.parseInt(st.nextToken());
	}
	
	private static int getNum(String msg){
		//feedbacks:1336437944:2:hasFamilyName(?s1,?o1),hasGivenName(?s2,?o1)->incorrect_hasGivenName(?s2,?o1)
		StringTokenizer st = new StringTokenizer(msg, ":");
		st.nextToken();
		st.nextToken(); 
		if (msg.indexOf("feedbacks")<0)
			st.nextToken();
		return Integer.parseInt(st.nextToken());
	}
	
	public static void main(String[] args){
		pruningEffects("D:\\Works\\paperwork\\rule-mining\\simulation\\hc50.log");
		//pruningEffects("/home/jchen/gilp/gilp18-21-55.log");
	}
}
