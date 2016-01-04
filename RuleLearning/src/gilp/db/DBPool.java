package gilp.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet; 
import gilp.learning.GILPSettings;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
public class DBPool {

	static PoolProperties pool_prop = null;
	static DataSource datasource = null;

	static void initPool() {

		pool_prop = new PoolProperties();
		pool_prop.setUrl(GILPSettings.DB_URL + GILPSettings.DB_NAME);
		pool_prop.setDriverClassName("org.postgresql.Driver");
		pool_prop.setUsername(GILPSettings.DB_USER);
		pool_prop.setPassword(GILPSettings.DB_DHPASS);
		pool_prop.setJmxEnabled(true);
		pool_prop.setTestWhileIdle(false);
		pool_prop.setTestOnBorrow(true);
		pool_prop.setValidationQuery("SELECT 1");
		pool_prop.setTestOnReturn(false);
		pool_prop.setValidationInterval(30000);
		pool_prop.setTimeBetweenEvictionRunsMillis(30000);
		pool_prop.setMaxActive(200);
		pool_prop.setInitialSize(10);
		pool_prop.setMaxWait(10000);
		pool_prop.setRemoveAbandonedTimeout(60);
		pool_prop.setMinEvictableIdleTimeMillis(30000);
		pool_prop.setMinIdle(10);
		pool_prop.setLogAbandoned(true);
		pool_prop.setRemoveAbandoned(true);
		pool_prop.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
				+ "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
		datasource = new DataSource();
		datasource.setPoolProperties(pool_prop);

	}

	public static Connection getConnection() {

		Connection con = null;
		if (datasource == null)
			initPool();
		try {
			con = datasource.getConnection();
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
			return null;
		}

		return con;
	}

	/* 关闭资源方法 */
	public static void closeAll(Connection conn, PreparedStatement pstmt, ResultSet rs) {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				GILPSettings.log("Exception in when DBPool closes resultset");
				e.printStackTrace(System.out);
			}
		}
		if (pstmt != null) {
			try {
				pstmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				GILPSettings.log("Exception in when DBPool closes pstmt");
				e.printStackTrace(System.out);
			}
		}
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				GILPSettings.log("Exception in when DBPool closes conn");
				e.printStackTrace(System.out);
			}
		}
	}

}
