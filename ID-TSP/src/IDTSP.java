import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

public class IDTSP implements Runnable {
	JSONObject packet;
	JSONObject tsp_packet;
	IDTSPDB db;
	String target_ip;
	int target_port;
	String host_ip;
	int host_port;

	public IDTSP(String target_ip, int target_port, String host_ip, int host_port) {
		db = new IDTSPDB();
		db.makeConnection();
		packet = new JSONObject();
		tsp_packet = new JSONObject();
		this.target_ip = target_ip;
		this.target_port = target_port;
		this.host_ip = host_ip;
		this.host_port = host_port;
	}

	public int tokenRequestorRegistration() {
		//This function is called to register token requestor
		int token_requestor_id = 0;
		Random rand = new Random();
		token_requestor_id = rand.nextInt(99999999) + 10000000;
		db.insertRegistration(token_requestor_id, "token_requestor");
		return token_requestor_id;
	}

	public int merchantRegistration() {
		//This function i called to register merchant
		int merchant_id = 0;
		Random rand = new Random();
		merchant_id = rand.nextInt(99999999) + 10000000;
		db.insertRegistration(merchant_id, "merchant");
		return merchant_id;
	}

	public boolean validateTokenRequestor(int id) {
		// Read JSONObject Content and validate if token requestor is registered
		return db.valiadteRegistraion(id, "token_requestor");

	}

	public boolean validateMerchant(int id) {
		// Read JSONObject Content and validate if merchant is registered
		return db.valiadteRegistraion(id, "merchant");
	}

	public void generateToken(int id) throws JSONException {
		//This function is used to generate token and insert them to token vault
		//And it also sends back output message to user
		//Add BIN Controller to prevent duplicate token
		String token = null;
		String id_num = Integer.toString(id);
		JSONObject tsp_packet = new JSONObject();
		MessageDigest md = null;

		try {
			md = MessageDigest.getInstance("SHA-256");
			md.update(id_num.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}

		token = md.digest().toString();
		long expiry_date = new java.util.Date().getTime();
		db.insertToken(token, id, expiry_date);
		if(token!=null){
			tsp_packet.put("Token", token);
			tsp_packet.put("Token Expiry Date", new java.sql.Date(expiry_date).toString());
			tsp_packet.put("Status", "success");
		}
		sendPacket(tsp_packet);
	}

	public void sendUseIDPacket(String token) throws SQLException, JSONException {
		//Send packet to card issuer for validation
		int id = 0;
		java.sql.Date token_expiry_date = null;
		java.sql.Date id_expiry_date = null;
		java.util.Date current_date = new java.util.Date();
		ResultSet rs = null;
		rs = db.retrieveData(token);
		JSONObject tsp_issuer_packet = new JSONObject();
		
		if(rs.next()){
			id = rs.getInt(2);
			token_expiry_date = rs.getDate(3);
			id_expiry_date = rs.getDate(4);
		}
	
		if(new java.sql.Date(current_date.getTime()).before(token_expiry_date)){
			tsp_issuer_packet.put("ID", id);
			tsp_issuer_packet.put("ID Expiry Date", id_expiry_date);
			tsp_issuer_packet.put("Token", token);
			tsp_issuer_packet.put("Token Expiry Date", token_expiry_date);
			sendPacket(tsp_packet);
		}
		else{
			System.out.println("Token is expired");
		}
	}

	public void sendValidateID(JSONObject p) throws JSONException {
		//This function is to send packet to issuer for validation in add id case
		JSONObject user_tsp_issuer = new JSONObject();
		user_tsp_issuer.put("ID", p.getInt("ID"));
		user_tsp_issuer.put("ID Expiry Date", p.getString("ID Expiry Date"));
		sendPacket(user_tsp_issuer);
	}

	public void sendPacket(JSONObject p) {

		try {
			Socket socket = new Socket(target_ip, target_port);
			OutputStream output = socket.getOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(output);
			objectOutput.writeObject(p.toString());
			objectOutput.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleRequest(JSONObject cp) throws JSONException, SQLException {
		// TODO Handle different request
		packet = cp;
		String token = null;
		java.sql.Date provided_expiry_date = null;
		int user_id = 0;
		int merchant_id = 0;
		int token_requestor_id = 0;

		if (host_port == 3333) {
			merchant_id = packet.getInt("Merchant ID");
			token_requestor_id = packet.getInt("Token Requestor ID");
			token = packet.getString("Token");
			provided_expiry_date = java.sql.Date.valueOf(packet.getString("Token Expiry Date"));
			//TODO justify validations
			if (validateMerchant(merchant_id) && validateTokenRequestor(token_requestor_id) && validateTokenExpiryDate(token, provided_expiry_date)) {
				sendUseIDPacket(token);
			} else {
				System.out.println("Validation Fails");
			}
		}
		
		
		else if (host_port == 3334){
			sendPacket(packet);
		}

		else if (host_port == 3335) {
			sendValidateID(packet);
		}
	
		else if (host_port == 3336) {
			if (cp.getString("Status").equals("Success")) {
				user_id = packet.getInt("ID");
				generateToken(user_id);
			}

		}
	}
	
	public boolean validateTokenExpiryDate(String token, java.sql.Date date){
		return db.validateDate(token, date);
	}

	@Override
	public void run() {
		while (true) {
			try {
				JSONObject client_packet = null;
				ServerSocket serverSocket = new ServerSocket(host_port);
				Socket socket = serverSocket.accept();
				ObjectInputStream objectInput = null;

				InputStream in = socket.getInputStream();
				objectInput = new ObjectInputStream(in);
				client_packet = new JSONObject(objectInput.readObject().toString());
				handleRequest(client_packet);
				serverSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) {
		IDTSP merchant_tsp_issuer = new IDTSP("localhost", 4444, "localhost", 3333);
		IDTSP issuer_tsp_merchant = new IDTSP("localhost", 2223, "localhost", 3334);
		IDTSP user_tsp_issuer = new IDTSP("localhost", 4445, "localhost", 3335);
		IDTSP issuer_tsp_user = new IDTSP("localhost", 1113, "localhost", 3336);

		Thread merchant_tsp_issuer_thread = new Thread(merchant_tsp_issuer);
		Thread issuer_tsp_merchant_thread = new Thread(issuer_tsp_merchant);
		Thread user_tsp_issuer_thread = new Thread(user_tsp_issuer);
		Thread issuer_tsp_user_thread = new Thread(issuer_tsp_user);
		merchant_tsp_issuer_thread.start();
		issuer_tsp_merchant_thread.start();
		user_tsp_issuer_thread.start();
		issuer_tsp_user_thread.start();
	}
}
