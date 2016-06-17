package gilp.auxiliary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;
import java.util.StringTokenizer;

import gilp.db.DBController;
import gilp.db.DBPool;
import gilp.learning.GILPSettings;

public class TestEncoding {
	
	static void readDB(){
		String sql = "select * from test";
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;		
		 
		try
		{
			conn = DBController.getConn();
			pstmt = conn.prepareStatement(sql);			 
			rs = pstmt.executeQuery();			 
			while(rs.next()){
				String o = rs.getString(4);
				System.out.println(o);
			}
			
		}catch (Exception e)
		{			 
			e.printStackTrace(System.out);
		 
		}finally
		{
			DBPool.closeAll(conn, pstmt, rs);
		}
	}
	
	static void test(){
		String fileName = "D:\\temp\\encoding_pr.txt";
		File file = null;
		
		try {
			file = new File(fileName);
			Scanner scan = new Scanner(file,"UTF-8");
			while(scan.hasNextLine()){
				String line = scan.nextLine();
			//while( (line= file.readUTF())!=null){
				StringTokenizer st = new StringTokenizer(line);
				String tid = st.nextToken();
				String s = st.nextToken("\t");
				String p = st.nextToken("\t");
				String o = st.nextToken("\t");
				System.out.println(line);
				String sql = "insert into test values('" + tid + "','" + s + "','" + p + "','" + o + "')" ;
				System.out.println(sql);
				
				DBController.exec_update(sql);
				System.out.println(tid + "|" + s + "|" + p + "|" + o);
				 
			}
			scan.close();
			//}
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} 
		
	}
	
	public static void main(String[] args){
		String str = "select from hasGivenName, hasFamilyName,";
		System.out.println(str.replaceFirst("hasFamilyName", "abc"));
		System.out.println(str.substring(0, str.lastIndexOf(",")));
	}
	

}
