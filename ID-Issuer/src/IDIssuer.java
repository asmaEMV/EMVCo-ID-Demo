import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.JSONException;
import org.json.JSONObject;

public class IDIssuer implements Runnable {
	
	IDIssuerDB db;
	String target_ip;
	int target_port;
	String host_ip;
	int host_port;
	
	public IDIssuer(String target_ip, int target_port, String host_ip, int host_port){
		this.target_ip = target_ip;
		this.target_port = target_port;
		this.host_ip = host_ip;
		this.host_port = host_port;
		db = new IDIssuerDB();
		db.makeConnection();
	}
	
	public void handleRequest(JSONObject cp) throws JSONException{
		//This function is called to handle packet based on different port
		int id = cp.getInt("ID");
		java.sql.Date provided_expiry_date = java.sql.Date.valueOf(cp.getString("ID Expiry Date"));//TODO
		JSONObject issuer_packet = new JSONObject();
		if(host_port == 4444){
			//This port is used for use token case
			//Validate id based on its existence and expiry date
			if(validateID(id, provided_expiry_date)){
				issuer_packet.put("Status of the Request", "Success");
				issuer_packet.put("ID", id);
				issuer_packet.put("ID Expiry Date", provided_expiry_date);
				sendPacket(issuer_packet);
			}
			else{
				//If validation fails, it outputs a failuer message with reason.
				issuer_packet.put("Status of the Request", "Fail");
				issuer_packet.put("Reason", "Inactive Account");
				//issuer_packet.put("ID", id);
				//issuer_packet.put("ID Expiry Date", provided_expiry_date.toString());
				sendPacket(issuer_packet);
			}
			
		}
		else if(host_port == 4445){
			//This port is used for add id case
			//Validate id based on its existence and expiry date
			//TODO OTP implementations
			if(validateID(id, provided_expiry_date)){
				issuer_packet.put("Status of the Request", "Success");
				issuer_packet.put("ID", id);
				issuer_packet.put("ID Expiry Date", provided_expiry_date.toString());
				sendPacket(issuer_packet);
			}
		}
	}
	
	public void sendPacket(JSONObject p){
		//This function is called to send packet to target destination 
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
	
	public boolean validateID(int id, java.sql.Date date){
		//This function is to validate ID
		db.makeConnection();
		return db.valiadteID(id,date);
	}
	public void run(){
		//This function is called to listen on a port for service
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
	
	public static void main(String[] args){
		//This port is opened for use token case
		IDIssuer issuer_tsp = new IDIssuer("localhost", 3334, "localhost", 4444);
		//This port is opened for add id case
		IDIssuer issuer_user = new IDIssuer("localhost", 3336, "localhost", 4445);
		Thread issuer_tsp_thread = new Thread(issuer_tsp);
		Thread issuer_user_thread = new Thread(issuer_user);
		issuer_tsp_thread.start();
		issuer_user_thread.start();
	}
}
