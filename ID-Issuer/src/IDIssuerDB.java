import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class IDIssuerDB {
	
	Connection conn;
	PreparedStatement p;
	ResultSet rs;
	java.sql.Date sql_date;
	java.util.Date util_date;
	
	
	public IDIssuerDB() {
			conn = null;
			p = null;
			rs = null;
			sql_date = null;
			util_date = null;
			//this.sdf = new SimpleDateFormat("yyyy-MM-dd");
			//this.util_date = new java.util.Date();
			//this.date = new java.sql.Date(util_date.);
		}

	public void makeConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/id_issuer", "root", "root");
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	
	public boolean valiadteID(int id, java.sql.Date date) {
		String query = "select * from id_info where id = "+"'"+id+"'";
		int register_id = 0;
		java.sql.Date current_date = new java.sql.Date(new java.util.Date().getTime());
		java.sql.Date provided_expiry_date = date;
		java.sql.Date expiry_date = null;
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				register_id = rs.getInt(1);
				expiry_date = rs.getDate(2);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (register_id != 0 && provided_expiry_date.equals(expiry_date) && current_date.before(expiry_date)) {
			return true;
		}

		return false;
	}
	
}

