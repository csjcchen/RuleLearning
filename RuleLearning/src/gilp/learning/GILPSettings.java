package gilp.learning;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

/*
 * store the configuration settings of our GILP system.
 * and the global information, such as all predicates, database name. 
 * */

public class GILPSettings {
	
	public static double EPSILON = 1.0e-7;	
	 
	public static double MINIMUM_FOIL_GAIN = -1.0e7;
	
	public static double MAXIMUM_FOIL_GAIN = 1.0e7;
	
	public static double FREQUENT_CONST_IN_FB; 
		//A threshold used in the process of feature construction. 
		//A constant can be regarded as a frequent constant and be adopted in building a new feature, only if 
		//it appears in more than FREQUENT_CONST_IN_FB*100 percents in all triples in user feedback with an identical predicate.  
	
	public static double FREQUENT_CONST_IN_DB; 
		//A threshold used in the process of feature construction. 
		//A constant can be regarded as a frequent constant and be adopted in building a new feature, only if 
		//it appears in more than FREQUENT_CONST_IN_DB*100 percents in all triples in DB with an identical predicate.  
		//TODO this parameter may be related to the COVERAGE_IN_FB
	
	public static int NUM_RDFTYPE_PARTITIONS = 9;
		//there are NUM_RDFTYPE_PARTITIONS tables in the kb. E.g., NUM_RDFTYPE_PARTITIONS=9, rdftype0, ..., rdftype8 
	
	public static double MINIMUM_SELECTIVITY = 1.0E-10;
	
	public static int MINIMUM_MAX_MATCHED = 1;
	
	
	public static double LAMBDA;
		//the parameter to trade-off between support scores of DB and feedback 
	
	public static double CONFIDENCE_Z; 
		// 1 - \alpha/2 quantile of a standard normal distribution
	public static  double THRESHOLD_OF_PR; 
		// threshold for the probability that r is correct 
	public static int MAXIMUM_RULE_LENGTH; 
		// the permitted maximum value of the length of a rule
	public static boolean IS_DEBUG;
		//whether the running is in the debug mode
	public static double MINIMUM_PRECISION = EPSILON;
		//minimum requirement for an intermediate rule
	
	public static int DB_ENGINE; 
		//1: RDF3X; 2: PostgreSQL; 
	
	public static String RDF3X_PATH;
	public static String RDF3X_DBFILE;
	
	//settings for connecting PG
	public static String DB_URL; 
	public static String DB_NAME;
	public static String DB_USER;
	public static String DB_DHPASS;
	
	public static String[] NUMERICAL_PREDICATES = { "hasArea", "hasBudget", "hasDuration", "hasEconomicGrowth", 
			"hasExpenses", "hasExport", "hasGDP", "hasGini", "hasHeight", "hasImport", "hasInflation", "hasLatitude", 
			"hasLength", "hasLongitude", "hasNumberOfPeople", "hasPages", "hasPopulationDensity","hasPoverty", 
			"hasRevenue", "hasUnemployment", "hasWeight", 
		};
	//TODO current implementation does not deal with dates 
	//"diedOnDate", "happenedOnDate", "startedOnDate","wasBornOnDate", "wasCreatedOnDate", "wasDestroyedOnDate"
	
	static ArrayList<String> _all_predicates = null;
		//store all predicates' names
	static HashMap<String, Double> _yago_stat = null;
		//store the statistics about yago3: the probability that triples in each predicate are correct
	
	
	static private PrintWriter log;
	 
	static{
		init();
	}
	
	 /**
     * Writes a message to the log file.
     */
    public static void log(String msg) {
        log.println(new Date() + ": " + msg);
    }

    /**
     * Writes a message with an Exception to the log file.
     */
    public static void log(Throwable e, String msg) {
        log.println(new Date() + ": " + msg);
        e.printStackTrace(log);
    }
    
	//get the names of all predicates stored in DB
	public static ArrayList<String> getAllPredicateNames(){
		if (_all_predicates == null){
			try {
				RandomAccessFile file = new RandomAccessFile("predicates","r");
				_all_predicates = new ArrayList<String>();
				String line = null;
				while((line = file.readLine())!=null){
					if (line.startsWith("rdftype")){
						for (int i=0;i<GILPSettings.NUM_RDFTYPE_PARTITIONS;i++)
							_all_predicates.add(line + i);
					}
					else
						_all_predicates.add(line);
				}
				file.close();
			} catch (Exception e) {
				e.printStackTrace(System.out);
			} 
		}
		return _all_predicates;
	}

	static private void init() {
		Properties sim_prop = new Properties();
		try {

			InputStream is = new FileInputStream(new File("config.txt"));
			sim_prop.load(is);
		} catch (Exception e) {
			System.err.println("Can't read the properties file. ");
			return;
		}

		String logFile = sim_prop.getProperty("logfile");
		Date d = new Date();

		logFile += d.getDate() + "-" + d.getHours() + "-" + d.getMinutes() + ".log";

		try {
			log = new PrintWriter(new FileWriter(logFile, true), true);
		} catch (IOException e) {
			System.err.println("Can't open the log file: " + logFile);
			log = new PrintWriter(System.err);
		}

		FREQUENT_CONST_IN_FB = Double.parseDouble(sim_prop.getProperty("FREQUENT_CONST_IN_FB"));
		FREQUENT_CONST_IN_DB = Double.parseDouble(sim_prop.getProperty("FREQUENT_CONST_IN_DB"));
		LAMBDA = Double.parseDouble(sim_prop.getProperty("LAMBDA"));
		CONFIDENCE_Z = Double.parseDouble(sim_prop.getProperty("CONFIDENCE_Z"));
		THRESHOLD_OF_PR = Double.parseDouble(sim_prop.getProperty("THRESHOLD_OF_PR"));
		MAXIMUM_RULE_LENGTH = Integer.parseInt(sim_prop.getProperty("MAXIMUM_RULE_LENGTH"));
		IS_DEBUG = (sim_prop.getProperty("IS_DEBUG").equals("true"));
	 	MINIMUM_PRECISION = Double.parseDouble(sim_prop.getProperty("MINIMUM_PRECISION"));
	    
	 	DB_ENGINE = Integer.parseInt(sim_prop.getProperty("DB_ENGINE"));
	 		//1: RDF3X; 2: PostgreSQL; 	
	 	switch(DB_ENGINE){
	 	case 1:
	 		RDF3X_PATH = sim_prop.getProperty("RDF3X_PATH");
	 		RDF3X_DBFILE = sim_prop.getProperty("RDF3X_DBFILE");
	 		break;
	 	case 2:
	 		DB_URL = sim_prop.getProperty("DB_URL");
	 		DB_NAME = sim_prop.getProperty("DB_NAME");
	 		DB_USER = sim_prop.getProperty("DB_USER");
	 		DB_DHPASS = sim_prop.getProperty("DB_DHPASS");
	 		break;
	 	default:
	 		GILPSettings.log("Error: the prop value not valid for  DB_ENGINE:" + DB_ENGINE);
	 	}
	  
	 
	
		System.out.println("Settings:");
		System.out.println("LogFile:" + logFile);
		System.out.println("FREQUENT_CONST_IN_FB" + FREQUENT_CONST_IN_FB);
		System.out.println("FREQUENT_CONST_IN_DB:" + FREQUENT_CONST_IN_DB);
		System.out.println("LAMBDA:" + LAMBDA);
		System.out.println("CONFIDENCE_Z:" + CONFIDENCE_Z);
		System.out.println("THRESHOLD_OF_PR:" + THRESHOLD_OF_PR);		
		System.out.println("MAXIMUM_RULE_LENGTH:" + MAXIMUM_RULE_LENGTH);
		System.out.println("IS_DEBUG:" + IS_DEBUG);
		System.out.println("MINIMUM_PRECISION:" + MINIMUM_PRECISION);
		
		switch(DB_ENGINE){
	 	case 1:
	 		System.out.println("RDF3X_PATH:" + RDF3X_PATH);
	 		System.out.println("RDF3X_DBFILE:" + RDF3X_DBFILE);
	 		break;
	 	case 2:
	 		System.out.println("DB_URL:" + DB_URL);
	 		System.out.println("DB_NAME:" + DB_NAME);
	 		System.out.println("DB_USER:" + DB_USER);
	 		System.out.println("DB_DHPASS:" + DB_DHPASS);
	 	}	
	}
	
	//get the statistics of yago facts
	public static HashMap<String, Double> getYagoStat(){
		if (_yago_stat == null){
			try {
				RandomAccessFile file = new RandomAccessFile("yago_stat","r");
				_yago_stat = new HashMap<String, Double>();
				String line = null;
				while((line = file.readLine())!=null){
					String pred = line.substring(0, line.indexOf('\t'));
					double pr = Double.parseDouble(line.substring(line.indexOf('\t')+1));
					_yago_stat.put(pred, pr);
				}
				file.close();
			} catch (Exception e) {
				e.printStackTrace(System.out);
			} 
		}
		return _yago_stat;
	}
	
	public static void main(String[] args){
		ArrayList<String> predicates = getAllPredicateNames(); 
		
		HashMap<String, Double>  stat = getYagoStat();
		
		for(String pred: predicates){
			GILPSettings.log(pred + ", " + stat.get(pred));
		}
	}

}
