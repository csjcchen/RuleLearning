package gilp.auxiliary;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Scanner;
import java.util.StringTokenizer;

import gilp.db.DBController;
import gilp.learning.GILPSettings;

/*to perform some data preprocessing on Yago
 * CJC, Dec. 15, 2015
 * */
public class YagoPreprocess {
	
	/*generate SQL for creating predicate tables*/
	static void generatePredicateTables(){
		
		String fileName = "D:\\temp\\yago-predicates.txt";
		RandomAccessFile file = null;
		
		try {
			file = new RandomAccessFile(fileName, "r");
			
			String line = null;
			while((line=file.readLine())!=null){
				line = line.replace(":", "");
				String sql = "DROP TABLE if exists " + line + " ;\n";
				sql += "CREATE TABLE ";
				sql += line + " ";
				sql += "( tid serial NOT NULL, ";
				sql += "s character varying(1024), ";
				sql += "o character varying(1024), ";
				sql += "CONSTRAINT \"" + line + "-pk\" PRIMARY KEY (tid) );";
				System.out.println(sql);
			}		 
		} catch (Exception e) {
			e.printStackTrace(System.out);
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
		generateLiteralInsersionSql(args[0] );
	}
}
