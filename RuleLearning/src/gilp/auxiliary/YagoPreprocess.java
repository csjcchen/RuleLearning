package gilp.auxiliary;

import java.io.File;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.StringTokenizer;

import gilp.db.DBController;
import gilp.learning.GILPSettings;

/*to perform some data preprocessing on Yago
 * CJC, Dec. 15, 2015
 * */
public class YagoPreprocess {
	
	static void rdftTypeDistribution(){
		
		char ch = 'a';
		for (ch='a';ch<='z';ch++){
			String sql = "select count(*) from temp_tab where S like ";
			String s = ch + "";
			sql +="'"+  s + "%' or S like '" + s.toUpperCase() + "%'" ;
			//System.out.println(sql);
			String rlt = DBController.getSingleValue(sql);
			System.out.println(s + ":" + rlt);
		}
		
	}
	
	static void completeSelectivities(){
		//some pairs of join-selectivities are missing because they are quite large 
		String fileName = "predicates";
		RandomAccessFile file = null;
		RandomAccessFile fileOut = null;
		try {
			file = new RandomAccessFile(fileName, "r");
			fileOut = new RandomAccessFile("/home/jchen/temp/more_selectivity.sql","rw");
			String sql = ""; 
		 	
			String line = null;
			ArrayList<String> preds = new ArrayList<>(); 
			
			HashMap<String, BigDecimal> tableSizes = new HashMap<>(); 
			
			while((line=file.readLine())!=null){
				line = line.replace(":", "");
				if (line.equals("rdftype")){
					for (int i=0;i<GILPSettings.NUM_RDFTYPE_PARTITIONS;i++){
						preds.add(line + i);			
						String query = "select count(*) from " + line + i; 
						String rlt = DBController.getSingleValue(query);
						BigDecimal count = new BigDecimal(rlt);
						tableSizes.put(line+i,count);	
					}
				}
				else{
					preds.add(line);			
					String query = "select count(*) from " + line; 
					String rlt = DBController.getSingleValue(query);
					BigDecimal count = new BigDecimal(rlt);
					tableSizes.put(line,count);	
				}
					
				
			}
			file.close();

			//sore predicates by dictionary order
			String[] pred_array = preds.toArray(new String[0]);
			Arrays.sort(pred_array);
			preds.clear();
			for (int i=0;i<pred_array.length;i++)
				preds.add(pred_array[i]);
			
			for (int i=0;i<preds.size();i++){
				String pred1 = preds.get(i);
				System.out.println("processing " + pred1);
				for (int j=i+1;j<preds.size();j++){	
					String pred2 = preds.get(j); 
					if(pred1.equals(pred2))
						continue;
					if (pred1.startsWith("rdftype") && pred2.startsWith("rdftype")){
						sql = "insert into JoinSelect(condition1, condition2, selc, maxMatched) values(";
						sql += "'" + pred1 + "_O','" + pred2 + "_O'," + 5.0E-7  + ", " + 10000 + ");";
						fileOut.writeBytes(sql+"\n");
						
						sql = "insert into JoinSelect(condition1, condition2, selc, maxMatched) values(";
						sql += "'" + pred1 + "_S','" + pred2 + "_O'," + 0  + ", " + 0 + ");";
						fileOut.writeBytes(sql+"\n");
						
						sql = "insert into JoinSelect(condition1, condition2, selc, maxMatched) values(";
						sql += "'" + pred1 + "_O','" + pred2 + "_S'," + 0  + ", " + 0 + ");";
						fileOut.writeBytes(sql+"\n");
						
						sql = "insert into JoinSelect(condition1, condition2, selc, maxMatched) values(";
						sql += "'" + pred1 + "_S','" + pred2 + "_S'," + 0  + ", " + 0 + ");";
						fileOut.writeBytes(sql+"\n");
					}					
					else if (pred1.startsWith("rdftype") || pred2.startsWith("rdftype")){
						String[] cons = new String[]{"S","O"};
						for (String c1: cons){
							for (String c2: cons){
								//String query = "select count(distinct " + pred1 + "." + c1 + " || " + pred2 + "." + c2 + ")";  
								sql = "insert into JoinSelect(condition1, condition2, selc, maxMatched) values(";
								if (pred1.startsWith("rdftype") && c1.equals("S")){
									sql += "'" + pred1 + "_" + c1 + "','" + pred2 + "_" + c2 + "'," + 5.0E-7  + ", " + 10000 + ");";								
								} 
								else if (pred2.startsWith("rdftype") && c2.equals("S"))
								{
									sql += "'" + pred1 + "_" + c1 + "','" + pred2 + "_" + c2 + "'," + 5.0E-7  + ", " + 10000 + ");";
								}								
								else{
									sql += "'" + pred1 + "_" + c1 + "','" + pred2 + "_" + c2 + "'," + 0  + ", " + 0 + ");";
								}	
								fileOut.writeBytes(sql+"\n");
							}
						}
					}//if 
					else{
						String[] cons = new String[]{"S","O"};
						for (String c1: cons){
							for (String c2: cons){
								String query = "select count(*) from JoinSelect where condition1='";
								query += pred1 + "_" + c1 + "' and condition2='" +  pred2 + "_" + c2 + "'";
								String rlt = DBController.getSingleValue(query);
								if (rlt.startsWith("0")){
									sql = "insert into JoinSelect(condition1, condition2, selc, maxMatched) values(";
									sql += "'" + pred1 + "_" + c1 + "','" + pred2 + "_" + c2 + "'," + 1  + ", " + 10000 + ");";
									fileOut.writeBytes(sql+"\n");
								}
							}
						}
						
					}  				
				}//for			
			}//for
			 
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} 
		
	}
	
	static void generateSelectivities(){
		String fileName = "predicates";
		RandomAccessFile file = null;
		RandomAccessFile fileOut = null;
		try {
			file = new RandomAccessFile(fileName, "r");
			fileOut = new RandomAccessFile("/home/jchen/temp/selectivity.sql","rw");
			
			String sql = "DROP Table if exists JoinSelect ;"; 
			System.out.println(sql);
			fileOut.writeBytes(sql + "\n");
			
			sql = "CREATE TABLE JoinSelect(tid serial NOT NULL, condition1 character varying(1024), ";
			sql += " condition2 character varying(1024), selc numeric(20, 19), maxMatched int,  PRIMARY KEY (tid) ); ";
			System.out.print(sql);
			fileOut.writeBytes(sql + "\n");			
			
			String line = null;
			ArrayList<String> preds = new ArrayList<>(); 
			
			HashMap<String, BigDecimal> tableSizes = new HashMap<>(); 
			
			while((line=file.readLine())!=null){
				line = line.replace(":", "");
				preds.add(line);			
				String query = "select count(*) from " + line; 
				String rlt = DBController.getSingleValue(query);
				BigDecimal count = new BigDecimal(rlt);
				tableSizes.put(line,count);
			}
			file.close();
			//sore predicates by dictionary order
			String[] pred_array = preds.toArray(new String[0]);
			Arrays.sort(pred_array);
			preds.clear();
			for (int i=0;i<pred_array.length;i++)
				preds.add(pred_array[i]);
			
			for (int i=0;i<preds.size();i++){
				String pred1 = preds.get(i);
				System.out.println("processing " + pred1);
				for (int j=i+1;j<preds.size();j++){	
					String pred2 = preds.get(j); 
					if (pred1.equalsIgnoreCase("rdftype") || pred2.equalsIgnoreCase("rdftype"))
						continue;
					
					if(pred1.equals(pred2))
						continue;
									
					String[] cons = new String[]{"S","O"};
					for (String c1: cons){

						for (String c2: cons){
							//String query = "select count(distinct " + pred1 + "." + c1 + " || " + pred2 + "." + c2 + ")";  
							String query = "select count(*) from " + pred1 + "," +  pred2 + " where ";							
							query += pred1 + "." + c1 + "=" + pred2 + "." + c2; 
							String rlt = DBController.getSingleValue(query);
							if (rlt == null ){
								System.out.println(query);
								continue;
							}
							BigDecimal sel = new BigDecimal(rlt);
							BigDecimal count1 = tableSizes.get(pred1);
							BigDecimal count2 = tableSizes.get(pred2);
							BigDecimal count = count1.multiply(count2);
							
							if (count.compareTo(BigDecimal.ZERO)>0)
								sel = sel.divide(count, 19, BigDecimal.ROUND_HALF_UP);
							else
								sel = BigDecimal.ZERO;

							if (sel.compareTo(BigDecimal.ZERO)<0)
								System.out.println("ERROR:" + query);
							
							int maxMatched = 0;
							
							if (sel.compareTo(BigDecimal.ZERO)>0){
								query = "select count(*) as num from " + pred1 + "," +  pred2 + " where ";							
								query += pred1 + "." + c1 + "=" + pred2 + "." + c2;
								query += " group by " + pred1 + "." + c1;
								query += " order by num desc limit 1";
							
								rlt = DBController.getSingleValue(query);
								if (rlt == null ){
									System.out.println("Error:" + query);
									continue;
								}
								maxMatched = Integer.parseInt(rlt);
							}
							
							
							sql = "insert into JoinSelect(condition1, condition2, selc, maxMatched) values(";
							sql += "'" + pred1 + "_" + c1 + "','" + pred2 + "_" + c2 + "'," + sel  + ", " + maxMatched + ");";
							//System.out.println(sql);							
							fileOut.writeBytes(sql + "\n");
							
						}
					}					
				}				
			}
			fileOut.close();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} 
	}
	
	static void generateIndex(){
		String fileName = "predicates";
		RandomAccessFile file = null;
		
		try {
			file = new RandomAccessFile(fileName, "r");
			
			String line = null;
			while((line=file.readLine())!=null){
				line = line.replace(":", "");
				String[] index_names = {"idx_" + line + "_S", "idx_" + line + "_O"};
				for (String str: index_names){
					String sql = "DROP INDEX if exists " + str + " ;";
					System.out.println(sql);
					sql = "CREATE index ";
					sql += str + " on ";
					sql += line + "(" + str.substring(str.lastIndexOf("_")+1) + ");";					
					System.out.println(sql);
				}
			
			}		 
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} 
	}
	
	/*generate SQL for creating predicate tables*/
	static void generatePredicateTables(){
		
		String fileName = "D:\\temp\\yago-predicates.txt";
		RandomAccessFile file = null;
		
		try {
			file = new RandomAccessFile(fileName, "r");
			
			String line = null;
			while((line=file.readLine())!=null){
				line = line.replace(":", "");
				String sql = "DROP TABLE if exists " + line + " ;";
				System.out.println(sql);
				sql += "CREATE TABLE ";
				sql += line + " ";
				sql += "( tid serial NOT NULL, ";
				sql += "s character varying(1024), ";
				sql += "o character varying(1024), ";
				sql += "CONSTRAINT \"" + line + "-pk\" PRIMARY KEY (tid) );";
				System.out.println(sql);
			}		 
		} catch (Exception e) {
			//e.printStackTrace(System.out);
		} 
	}
	
	//remove < > ", and @
	static String formatYagoToken(String str){
		//<id_1958v2w_l7i_10ev6mt>	<Saatly_Rayon>	<hasNumberOfPeople>	"95100"^^xsd:integer	95100
		String rlt = str.replaceAll("<", "");
		rlt = rlt.replaceAll(">", "");
		rlt = rlt.replaceAll("\'", "-");
		rlt = rlt.replaceAll("\"", "");
		rlt = rlt.replace(":", "");
		if (rlt.contains("@")){
			rlt = rlt.substring(0, rlt.indexOf("@"));
		}
		return rlt;
	}
	
	//read yagoLiteralfacts.tsv , for each line, generate a insert sql
		static void insertRFDTypes(String fileName){
			String[][] partitions = {{"a","c"}, {"d","d"}, {"e", "f"}, {"g","i"}, {"j","l"}, {"m","n"},{"o","r"}, {"s","t"}, {"u","z"}};
			
		 	File file_in = null;
		 	
			try {
				file_in = new File(fileName);
			 		
				 
				String line = null;
				Scanner scan = new Scanner(file_in,"UTF-8");
				
				scan.nextLine();//skip the first line
				int count = 0;
				while(scan.hasNextLine()){			
					line = scan.nextLine();
		
					StringTokenizer st = new StringTokenizer(line, "\t");
					st.nextToken();//skip the first token
		
					String s = formatYagoToken(st.nextToken());
					String p = formatYagoToken(st.nextToken());
					String o = formatYagoToken(st.nextToken());
					if (s.length()<=2){
						GILPSettings.log(line);
						continue;
					}
					p = "rdftype"; 
					String h = s.substring(0,1).toLowerCase();
					int i = 0;
					for (i=0;i<partitions.length;i++){
						if (h.compareToIgnoreCase(partitions[i][0])>=0 && h.compareToIgnoreCase(partitions[i][1])<=0){
							p += i + "";
							break;
						}
					}
					if (i==partitions.length)
						p += (partitions.length-1) + "";
					
					
					String sql = "insert into ";
					sql +=  p + "(s, o) ";
					sql += "values ('" + s + "','" + o + "');"; 
					
					DBController.exec_update(sql);
					if (++count % 10000 == 0){
						GILPSettings.log(new java.util.Date().toString() + ": having loaded " + count  + " triples." );
					}
				}		 
				
				scan.close();
			} catch (Exception e) {
			
				e.printStackTrace(System.out);
			} 
		}
	
	//read yagoLiteralfacts.tsv , for each line, generate a insert sql
	static void generateLiteralInsersionSql(String fileName){
	 	File file_in = null;
	 	
		try {
			file_in = new File(fileName);
		 		
			 
			String line = null;
			Scanner scan = new Scanner(file_in,"UTF-8");
			
			scan.nextLine();//skip the first line
			int count = 0;
			while(scan.hasNextLine()){			
				line = scan.nextLine();
	
				StringTokenizer st = new StringTokenizer(line, "\t");
				st.nextToken();//skip the first token
	
				String s = formatYagoToken(st.nextToken());
				String p = formatYagoToken(st.nextToken());
				String o = formatYagoToken(st.nextToken());
				
				String sql = "insert into ";
				sql +=  p + "(s, o) ";
				sql += "values ('" + s + "','" + o + "');"; 
				
				DBController.exec_update(sql);
				if (++count % 10000 == 0){
					GILPSettings.log(new java.util.Date().toString() + ": having loaded " + count  + " triples." );
				}
			}		 
			
			scan.close();
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} 
	}
	
	public static void main(String[] args){
		completeSelectivities();
	}
}
