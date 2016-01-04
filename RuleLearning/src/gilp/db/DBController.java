package gilp.db;

import java.util.ArrayList;

import gilp.learning.GILPSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DBController {

	public static boolean exec_update(String sql){
		
		Connection conn = null; 
		 
		try
		{
			conn = DBController.getConn();
			conn.createStatement().executeUpdate(sql);
			conn.close();
			return true;
			
		}catch (Exception e)
		{
			GILPSettings.log( "Error in DBController when executing: " + sql);
			e.printStackTrace(System.out);
			return false;
		}
	}
	
	public ArrayList<Object> exec_query(String sql){
		//TODO
		return null;
	} 
	
	public static ArrayList<ArrayList<String>> getTuples(String qry){
		 
		Connection con = DBPool.getConnection();
		PreparedStatement pstmt = null;
		ArrayList<ArrayList<String>> rlts = new ArrayList<ArrayList<String>> ();
		ResultSet rs = null;
		try{
			pstmt=con.prepareStatement(qry);
			rs = pstmt.executeQuery();
			int num = rs.getMetaData().getColumnCount();
			while (rs.next()){
				ArrayList<String> tuple = new ArrayList<String>();
				for (int i=1;i<=num;i++){
					tuple.add(rs.getString(i));
				}
				rlts.add(tuple);
			}			
		}
		catch(Exception ex){
			GILPSettings.log("DBController.getTuples: " + ex.getMessage());
			GILPSettings.log("Query: " + qry);
			return null;
		}		
		finally{
			DBPool.closeAll(con, pstmt, rs);
		}		 
		return rlts;
	}
	
	public static Connection getConn(){  //鑾峰彇鏁版嵁搴撹繛鎺ユ柟娉�
		Connection conn=DBPool.getConnection();
		return conn;
	}
}
