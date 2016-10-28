import org.json.JSONObject;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class IDClient implements Runnable {

	String target_ip;
	int target_port;
	String host_ip;
	int host_port;
	JSONObject packet;

	public IDClient(String target_ip, int target_port, String host_ip, int host_port) {
		this.target_ip = target_ip;
		this.target_port = target_port;
		this.host_ip = host_ip;
		this.host_port = host_port;
		packet = new JSONObject();
	}


	public void sendPacket(JSONObject packet) {
		try {
			Socket socket = new Socket(target_ip, target_port);
			OutputStream output = socket.getOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(output);
			objectOutput.writeObject(packet.toString());
			objectOutput.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void handleRequest(JSONObject p) {
		try {
			String token = p.getString("Token");
			System.out.println(token);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void preparePacket(String dest) {
		
		//This function is used to craft packet, the data format
		//is based on EMVCo Framework NFC Use Case p.68-69
		if (dest.equals("TSP")) {
			try {
				packet.put("ID", new Integer(12345678));
				packet.put("ID Expiry Date", "2017-12-31");
				packet.put("Token Requestor ID", 10000000);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		else if (dest.equals("Merchant")) {
			try {
				packet.put("Token", "9281293");
				packet.put("Token Expiry Date", "2017-12-31");
				packet.put("Token Requestor ID", 10000000);
				packet.put("Token Cryptogram", "!@#$%^&");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		else if (dest.equals("Issuer")){
			//TODO 
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			System.out.println("Sent");
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

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		String request = sc.nextLine();
		sc.close();

		if (request.equals("ADD")) {
			IDClient id_add = new IDClient("localhost", 3335, "localhost", 1112);
			id_add.preparePacket("TSP");
			Thread thread_add_id = new Thread(id_add);
			thread_add_id.start();
			//Data format is according to EMVCo Framework p.68-69 NFC Use Case
			id_add.sendPacket(id_add.packet);
		}
		
		else if(request.equals("USE")){
			IDClient id_use = new IDClient("localhost", 2222, "localhost", 1111);
			id_use.preparePacket("Merchant");
			Thread thread_use_id = new Thread(id_use);
			thread_use_id.start();
			//Data format is according to EMVCo Framework p.68-69 NFC Use Case
			id_use.sendPacket(id_use.packet);
			//TODO add OTP connection with ID Issuer prior send tokens out
		}
		
		else{
			System.out.println("BAD INPUT");
		}

	}

}
