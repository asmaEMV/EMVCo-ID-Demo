import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.json.JSONObject;

public class IDMerchant implements Runnable {

	JSONObject packet;
	String target_ip;
	int target_port;
	String host_ip;
	int host_port;

	public IDMerchant(String target_ip, int target_port, String host_ip, int host_port) {
		this.target_ip = target_ip;
		this.target_port = target_port;
		this.host_ip = host_ip;
		this.host_port = host_port;
		packet = new JSONObject();
	}

	public void handleRequest(JSONObject cp) {

		packet = cp;

		if (host_port == 2222) {
			try {
				packet.put("Merchant ID", 99999999);
			} catch (Exception e) {
				e.printStackTrace();
			}
			sendPacket();
		}

		else if (host_port == 2223) {
			sendPacket();
		}
	}

	public void sendPacket() {
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

	@Override
	public void run() {
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

	public static void main(String[] args) {
		
		IDMerchant merchant_tsp = new IDMerchant("localhost", 3333, "localhost", 2222);
		IDMerchant merchant_user = new IDMerchant("localhost", 1111, "localhost", 2223);
		Thread merchant_tsp_thread = new Thread(merchant_tsp);
		Thread merchant_user_thread = new Thread(merchant_user);
		merchant_tsp_thread.start();
		merchant_user_thread.start();

	}
}
