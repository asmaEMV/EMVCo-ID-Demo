import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;


public class IDTSPDB {
	Connection conn;
	PreparedStatement p;
	Calendar cal;
	

	public IDTSPDB() {
		conn = null;
		p = null;
		cal = null;
		//this.sdf = new SimpleDateFormat("yyyy-MM-dd");
		//this.util_date = new java.util.Date();
		//this.date = new java.sql.Date(util_date.);
	}

	public void makeConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/id_tsp", "root", "root");
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public String insertToken(String token, int id, long id_expiry) {
		java.sql.Date token_expiry_date = null;
		java.sql.Date id_expiry_date = null;
		try {
			cal = Calendar.getInstance();
			cal.add((Calendar.YEAR), 3);
			token_expiry_date = new java.sql.Date(cal.getTimeInMillis());
			id_expiry_date = new java.sql.Date(id_expiry);
			p = conn.prepareStatement("INSERT INTO token_id VALUES(?,?,?)");
			p.setString(1, token);
			p.setInt(2, id);
			p.setDate(3, token_expiry_date);
			p.setDate(4, id_expiry_date);
			p.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e);
		}
		
		return token_expiry_date.toString();

	}
	
	public ResultSet retrieveData(String token){
		String query = "SELECT * FROM toke_id WHERE token = " + "'" + token + "'";
		ResultSet rs = null;
		
		try{
			Statement stmt = conn.createStatement();
			rs = stmt.executeQuery(query);

		}catch(Exception e){
			e.printStackTrace();
		}
		return rs;
	}
	public void insertRegistration(int id, String entity){
		try{
			p = conn.prepareStatement("INSERT INTO " + entity +" VALUES(?,?,?)");
			cal = Calendar.getInstance();
			java.sql.Date create_date = new java.sql.Date(cal.getTimeInMillis());
			cal.add((Calendar.YEAR),3);
			java.sql.Date expiry_date = new java.sql.Date(cal.getTimeInMillis());
			p.setInt(1, id);
			p.setDate(2, create_date);
			p.setDate(3, expiry_date);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public boolean valiadteRegistraion(int id, String entity) {
		String query = "select * from" + entity + " where " + entity + " = " + "'" + id + "'" ;
		int register_id = 0;
		java.sql.Date current_date = new java.sql.Date(new java.util.Date().getTime());
		java.sql.Date expiry_date = null;
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				register_id = rs.getInt(1);
				expiry_date = rs.getDate(3);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (register_id!=0 && current_date.before(expiry_date)){
			return true;
		}
		
		return false;
	}
	
	public boolean validateDate(String token, java.sql.Date date){
		String query = "select * from token_id where token = " +"'"+token+"'";
		java.sql.Date token_expiry_date = null;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if(rs.next()){
				token_expiry_date = rs.getDate(3);
			}
		}catch (SQLException e){
			e.printStackTrace();
		}
		
		if(token_expiry_date.before(date)){
			return true;
		}
		return false;
	}

	
}
