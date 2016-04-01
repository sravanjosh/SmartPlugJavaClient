package com.linkconnet;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;


class LOGGER {
	public static boolean DEBUG = false;
}

class User {
	private String accessToken = "";

	public User(String _accessToken) {
		this.accessToken = _accessToken;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
}

class Device implements Runnable {
	private static final int KEEPALIVE_TIME = 2000;// Milliseconds
	private static final int KEEPALIVE_LOSS_BEFORE_DOWN = 3;

	private String deviceId = "";
	private String hostname = "";
	private int port = 48879;
	private Socket tcpClient = null;
	private DataInputStream di = null;
	private DataOutputStream outToServer = null;
	private int numOfKeepAliveMissed = 0;
	private int dynamicKeepAliveTime = KEEPALIVE_TIME;

	private boolean localAccessible = false;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isLocalAccessible() {
		return localAccessible;
	}

	public void setLocalAccessible(boolean localAccessible) {
		this.localAccessible = localAccessible;
	}

	public Device(String _deviceId, String _hostname, int _port) {
		this.deviceId = _deviceId;
		this.hostname = _hostname;
		this.port = _port;
	}

	// HTTP GET request
	private void sendGet() throws Exception {

		String url = "http://www.google.com/search?q=mkyong";

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		//add request header
		con.setRequestProperty("User-Agent", "App/1.0");

		int responseCode = con.getResponseCode();
		if(LOGGER.DEBUG) System.out.println("\nSending 'GET' request to URL : " + url);
		if(LOGGER.DEBUG) System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		if(LOGGER.DEBUG) System.out.println(response.toString());

	}

	// HTTP POST request
	private void sendPost(String func, Map params) throws Exception {

		if(LOGGER.DEBUG) System.out.println("In Send Post");
		String url = "https://api.particle.io/v1/devices/" + this.deviceId + "/" + func;
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		//		if(LOGGER.DEBUG) System.out.println(url);
		//add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; X11)");
		con.setRequestProperty("Accept", "*/*");
		con.setRequestProperty("Accept-Encoding", "gzip, deflate");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.8,te;q=0.6");

		String urlParameters = "access_token=532f8573ef000cc09fd99435c2459c5f6a7da4a1";

		for (Iterator iterator = params.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry pair = (Map.Entry) iterator.next();
			urlParameters = urlParameters + "&" + pair.getKey() + "=" + pair.getValue();
		}
		// Send post request
		//		if(LOGGER.DEBUG) System.out.println(urlParameters);
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		//		if(LOGGER.DEBUG) System.out.println("\nSending 'POST' request to URL : " + url);
		//		if(LOGGER.DEBUG) System.out.println("Post para`meters : " + urlParameters);
		//		if(LOGGER.DEBUG) System.out.println("Response Code : " + responseCode);

		try {
			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
		} catch (IOException e) {
			if(LOGGER.DEBUG) System.out.println(e.getMessage());
		}

		if(LOGGER.DEBUG) System.out.println("Out Sendpost");
		//print result
		//		if(LOGGER.DEBUG) System.out.println(response.toString());

	}

	private void resetTcp() {
		if (tcpClient != null) {
			this.numOfKeepAliveMissed++;

			if (this.numOfKeepAliveMissed < KEEPALIVE_LOSS_BEFORE_DOWN) {
				return;
			}
		}

		try{
			tcpClient.close();
			di.close();
			outToServer.close();
		} catch(Exception e1) {

		}

		tcpClient = null;
		di = null;
		outToServer = null;
		this.dynamicKeepAliveTime = 5000;
		localAccessible = false;
	}

	private synchronized int writeToSocket(byte[] bytes) throws IOException {
		if (outToServer != null)
			outToServer.write(bytes);

		try {
			return di.read();
		} catch(Exception e) {
			return 0xff;
		}
	}

	public synchronized boolean checkLocalAvailablity() {
		try{
			if(tcpClient == null) {
				if(LOGGER.DEBUG) System.out.println("Trying to create TCP Client");
				tcpClient = new Socket();
				tcpClient.connect(new InetSocketAddress(hostname, port), 2000);
				di = new DataInputStream(tcpClient.getInputStream());
				outToServer = new DataOutputStream(tcpClient.getOutputStream());
				localAccessible = true;
				if(LOGGER.DEBUG) System.out.println("Client Connected");
				tcpClient.setSoTimeout(2000);
				this.numOfKeepAliveMissed = 0;
				this.dynamicKeepAliveTime = 1000;
			} else  {
				try {
					if (this.writeToSocket(new byte[]{0x00}) != 0) {
						this.resetTcp();
						if(LOGGER.DEBUG) System.out.println("Keep alive missed");
					} else {
						this.numOfKeepAliveMissed = 0;
						if(LOGGER.DEBUG) System.out.println("Client alive");
					}
				}catch (Exception e) {
					this.resetTcp();
					e.printStackTrace();
					if(LOGGER.DEBUG) System.out.println("Client disconnected");
				}
			} 

		} catch(Exception e) {
			this.resetTcp();
			if(LOGGER.DEBUG) System.out.println("Client Not available");
		}

		return localAccessible;
	}

	public boolean switchOn(int time) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("args", new Integer(time).toString());
		try {
			if (localAccessible) {
				if(this.writeToSocket(new byte[]{0x01, (byte)time, (byte)(time>>8)}) != 0) {
					this.sendPost("switch_on", map);
				}
			} else {
				this.sendPost("switch_on", map);
			}
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	public boolean switchOff(int time) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("args", new Integer(time).toString());
		try {
			if (localAccessible) {
				if(this.writeToSocket(new byte[]{0x02, (byte)time, (byte)(time>>8)}) != 0) {
					this.sendPost("switch_off", map);
				}
			} else {
				this.sendPost("switch_off", map);
			}
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	public boolean autoOff(short time) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("args", new Integer(time).toString());
		try {
			if (localAccessible) {
				if(this.writeToSocket(new byte[]{0x07, (byte)time}) != 0) {
					this.sendPost("auto_off", map);
				}
			} else {
				this.sendPost("auto_off", map);
			}
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	public boolean autoOn(short time) {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("args", new Integer(time).toString());
		try {
			if (localAccessible) {
				if(this.writeToSocket(new byte[]{0x06, (byte)time}) != 0) {
					this.sendPost("auto_on", map);
				}
			} else {
				this.sendPost("auto_on", map);
			}
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public void run() {
		while(true) {
			this.checkLocalAvailablity();

			try {
				Thread.sleep(dynamicKeepAliveTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}


public class ClientApp {

	public static void clearScreen() throws IOException {
		final String operatingSystem = System.getProperty("os.name");

		if (operatingSystem.contains("Windows")) {
			Runtime.getRuntime().exec("cls");
		}
		else {
			Runtime.getRuntime().exec("clear");
		}

	}
	public static void main(String[] args) throws IOException {
		User user = new User("fcd827265959b0a553e208db71d4706844fa6924");
		final Device device = new Device("3e0025001747343338333633", "core-1.local", 48879);

		Thread deviceMonitor = new Thread(device);
		deviceMonitor.start();

		int ch;
		Scanner sc=new Scanner(System.in);
		sc.useDelimiter("\n");
		while(true) {
			clearScreen();
			System.out.println("");
			System.out.println("");
			System.out.println("");
			System.out.println("");
			System.out.println("\t1. Switch On");
			System.out.println("\t2. Switch Off");
			System.out.println("\t3. Auto Cut-Off Timer");
			System.out.println("\t4. Auto On Timer");
			System.out.println("\t5. Exit");
			System.out.println("");
			System.out.print("Enter any option: ");

			try {
				ch = new Integer(sc.next());
			} catch (Exception e) {
				ch = 0;
			}

			int time = 0;
			switch(ch) {

			case 1:
				clearScreen();
				System.out.println("");
				System.out.println("");

				System.out.print("\tPlease enter delay after which to switch on [0]: ");

				try {
					time = new Integer(sc.next());
				} catch (Exception e) {
					time = 0;
				}

				if(!device.switchOn(time)) {
					if(LOGGER.DEBUG) System.out.println("Couldn't switch on");
				}
				break;
			case 2:
				clearScreen();
				System.out.println("");
				System.out.println("");

				System.out.print("\tPlease enter delay after which to switch off [0]: ");

				try {
					time = new Integer(sc.next());
				} catch (Exception e) {
					time = 0;
				}

				if(!device.switchOff(time)) {
					if(LOGGER.DEBUG) System.out.println("Couldn't switch off");
				}
				break;
			case 3:
				clearScreen();
				System.out.println("");
				System.out.println("");

				
				while(true) {
					try {
						System.out.print("\tPlease enter cut-off timer after which to switch off: ");
						time = new Integer(sc.next());
						break;
					} catch (Exception e) {
						
					}
				}

				if(!device.autoOff((short)time)) {
					if(LOGGER.DEBUG) System.out.println("Couldn't set auto off");
				}
				break;
			case 4:
				clearScreen();
				System.out.println("");
				System.out.println("");

				
				while(true) {
					try {
						System.out.print("\tPlease enter cut-off timer after which to switch on: ");
						time = new Integer(sc.next());
						break;
					} catch (Exception e) {
						
					}
				}

				if(!device.autoOn((short)time)) {
					if(LOGGER.DEBUG) System.out.println("Couldn't set auto on");
				}
				break;
			case 5:
				System.exit(0);
				break;
			default:
				break;
			}
		}
	}
}
