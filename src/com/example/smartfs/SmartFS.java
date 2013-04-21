package com.example.smartfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SmartFS extends Activity {
	android.net.wifi.WifiManager.MulticastLock lock;
	android.os.Handler handler = new android.os.Handler();
	volatile public static ServiceInfo[] list;
	private String type = "_smartfs._tcp.local.";
	private static ServiceInfo serviceInfo;
	JmDNS mJmDNS;
	public static ListView tv;
	String devID, phoneNo;
	String userId;
	String TAG = "SmartFS";
	InetAddress hostName;
	ServerSocket serverSocket; /* serverSocket.get */
	ListView listV;
	SparseArray<ServiceInfo> dev;
	AlertDialog mDialog;
	Handler handle = new Handler();
	private List<String> fileList = new ArrayList<String>();
	public static LinkedList<PairedNode> pairedList = new LinkedList<PairedNode>(); // svae
																					// to
																					// internal
																					// Storage
																					// later

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		userId = getIntent().getStringExtra("userID");

		/* Server for File Transfer */
		/* Init Server */

		try {
			serverSocket = new ServerSocket(0);
			tv = (ListView) findViewById(R.id.listView1);
			Thread th = new Thread(new ServerAction(serverSocket));
			th.start();
		} catch (IOException e) {
			// tv.append("\nError opening Server! Please Try again");
			return;
		}
		System.out.println("IP and Port");
		System.out.println(serverSocket.getLocalPort());
		System.out.println(serverSocket.getInetAddress());
		// System.out.println(serverSocket.getInetAddress().getCanonicalHostName());

		listV = (ListView) findViewById(R.id.listView1);
		listV.canScrollVertically(0);

		// Create a message handling object as an anonymous class.
		OnItemClickListener devListHandler = new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View v, int position,
					long id) {
				boolean arePaired = false;
				ServiceInfo s = dev.get((int) id);
				String pairto = s.getPropertyString("TCP Port");
				InetAddress ipStr = s.getInet4Addresses()[s.getInet4Addresses().length - 1];

				String pairingID = s.getPropertyString("PhoneNo");
				for (PairedNode p : pairedList) {
					if (p.pairedID.equals(pairingID) && (p.pairedFlag == true)) {
						arePaired = true;
					}
				}

				if (arePaired == false) {
					Random randomGenerator = new Random();
					int pairKey = randomGenerator.nextInt(99999);
					// send pair key to port.

					String msg = "Pair_Device" + ","
							+ Integer.toString(pairKey) + "," + getPhoneNo();
					try {
						Thread th = new Thread(new ClientAction(ipStr, pairto,
								msg));
						th.start();
					} catch (IOException e) {
						System.out.println("Error in sending passkey");
					}

					initiateTwoButtonAlert("Pair Code " + pairKey, "Yes", "No",
							ipStr, pairto, pairKey, pairingID/* Phone no */);

					Log.i("Requesting connection to " + id + " device",
							s.getPropertyString("PhoneNo"));
				} else {
					//Do Nothing. 
					//Populate the list
					
				}
			}
		};
		listV.setOnItemClickListener(devListHandler);
		Thread th1 = new Thread() {
			public void run() {
				try {
					setUp();
					mJmDNS = JmDNS.create(hostName);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		th1.start();
		try {
			th1.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		devID = getDeviceID(this);
		phoneNo = getPhoneNo();
		Log.i(TAG, "ID=" + devID + "Phone no=" + phoneNo + "UserName=" + userId);
		Thread th = new Thread(new RegisterService(mJmDNS, type, devID, userId,
				phoneNo, serverSocket.getLocalPort(), serverSocket
						.getInetAddress().toString()));
		th.start();
	}

	private void initiateTwoButtonAlert(String displayText,
			String positiveButtonText, String negativeButtonText,
			final InetAddress ip, final String port, final int pairKey,
			final String pairingID) {

		mDialog = new AlertDialog.Builder(this)
				.setTitle(getResources().getString(R.string.app_name))
				.setMessage(displayText)
				.setIcon(R.drawable.ic_launcher)
				.setPositiveButton(positiveButtonText,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface arg0, int arg1) {

								// Already Received his Directory List?
								boolean otherPairapproved = false;
								PairedNode temp = null;
								for (PairedNode p : pairedList) {
									if (p.pairedID.equals(pairingID) && (p.pairedDirectory != null)) {
										otherPairapproved = true;
										temp = p;
										break;
									}
								}
								// do stuff onclick of YES
								sendDirectoryView(ip, port, pairKey, pairingID);
								if (otherPairapproved == true) {
									temp.selfPairApproved = true;
									temp.setPaired();
									//Show temp.pairedDirectory; : Todo
									
									//Populate the list of directory: Todo			
								} else {
									if (pairedList.size() > 10) {
										pairedList.removeFirst();
									}
									PairedNode newPair = new PairedNode(
											pairingID, pairKey);
									newPair.selfPairApproved = true;
									pairedList.add(newPair);
								}
								finish();
							}
						}).setNegativeButton(negativeButtonText, null).show();

		WindowManager.LayoutParams layoutParams = mDialog.getWindow()
				.getAttributes();
		layoutParams.dimAmount = 0.9f;
		mDialog.getWindow().setAttributes(layoutParams);
		mDialog.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	}

	private void sendDirectoryView(InetAddress ip, String port, int pairKey,
			String pairingID/* Phone No. */) {

		File root = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath());
		// ListDir(root);
		File[] files = root.listFiles();
		String dirFileList = null;
		dirFileList = new String("Directory_List:" + pairKey + ":"
				+ getPhoneNo() + ":");
		for (File file : files) {
			dirFileList = dirFileList.concat(file.getPath() + ",");
		}

		try {
			Thread th = new Thread(new ClientAction(ip, port, dirFileList));
			th.start();
		} catch (IOException e) {
			System.out.println("Error in sending passkey");
		}

	}

	/*
	 * void ListDir(File f) { File[] files = f.listFiles(); fileList.clear();
	 * for (File file : files) { fileList.add(file.getPath()); }
	 * 
	 * }
	 */

	/** Called when the activity is first created. */
	private void setUp() {
		android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
		lock = wifi.createMulticastLock("mylockthereturn");
		// hostName = "" + wifi.getConnectionInfo().getNetworkId();
		String ip = Formatter.formatIpAddress(wifi.getConnectionInfo()
				.getIpAddress());
		try {
			hostName = InetAddress.getByName(ip);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		lock.setReferenceCounted(true);
		lock.acquire();
	}

	public static ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	public static void setServiceInfo(ServiceInfo serviceInfo) {
		SmartFS.serviceInfo = serviceInfo;
	}

	public String getDeviceID(Context context) {
		TelephonyManager manager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		String deviceId;
		if (manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
			// Tablet
			deviceId = Secure.getString(this.getContentResolver(),
					Secure.ANDROID_ID);

		} else {
			// Mobile
			deviceId = manager.getDeviceId();

		}
		return deviceId;
	}

	private String getPhoneNo() {
		TelephonyManager mTelephonyMgr;
		mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		return mTelephonyMgr.getLine1Number();
	}

	public void listServices(View view) {
		Thread th = new Thread(new ListServices(mJmDNS, type));
		th.start();
		if (list != null) {
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(
					getBaseContext(), android.R.layout.simple_list_item_1);
			dev = new SparseArray<ServiceInfo>();
			for (int i = 0; i < list.length; i++) {
				adapter.add(list[i].getName() + "\n"
						+ list[i].getPropertyString("PhoneNo"));
				dev.put(i, list[i]);
			}
			listV.setAdapter(adapter);
		}
	}

	@Override
	protected void onStop() {
		try {
			mJmDNS.unregisterService(getServiceInfo());
			mJmDNS.unregisterAllServices();

			mJmDNS.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		lock.release();
		super.onStop();
	}

	class ServerAction extends Thread {

		ServerSocket ssock = null;

		public ServerAction(ServerSocket ss) throws IOException {
			this.ssock = ss;
			// SmartFS.tv.append("Whats Up dude? ");
		}

		public void run() {
			System.out.println("Waiting for connection!");

			while (true) {
				try {
					Socket connection = ssock.accept();
					Thread th = new Thread(new ConnectionHandler(connection));
					th.start();
				} catch (IOException e) {
					System.err.println("Connetion Accept failed!");
					continue;
				}
			}
		}
	}

	class ConnectionHandler extends Thread {

		Socket sock = null;

		public ConnectionHandler(Socket conn) throws IOException {
			this.sock = conn;
		}

		public void run() {

			String sreceive;
			try {

				PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						sock.getInputStream()));
				while ((sreceive = in.readLine()) != null) {
					if (sreceive.contains("get_root")) {
						/* Get the MetaData and send to out */

					} else if (sreceive.contains("get_folder")) {
						/* Get the MetaData and send to out */

					} else if (sreceive.contains("Pair_Device")) {
						
/*						String msg = "Pair_Device" + ","
								+ Integer.toString(pairKey) + "," + getPhoneNo();*/
						String[] split = sreceive.split(",");
						
						
						
						
						final String pairKey = split[1];
						final String phoneNo = split[2];
								//sreceive.substring(sreceive.indexOf(",") + 1);
						InetSocketAddress remoteAddr = (InetSocketAddress) sock
								.getRemoteSocketAddress();
						//String ip = remoteAddr.getHostName();
						final InetAddress ip = remoteAddr.getAddress();
						final int port = remoteAddr.getPort();
						handle.post(new Runnable(){
							public void run(){
								initiateTwoButtonAlert("Pair Code " + pairKey, "Yes",
										"No", ip, Integer.toString(port),
										Integer.parseInt(pairKey), phoneNo );
		
							}
						});
						
					}else if (sreceive.contains("Directory_List")) {
						/* Get the MetaData and send to out */
						String[] split = sreceive.split(":");
						String pairKey = split[1];
						String phoneNo = split[2];
						String dirList = split[3];
						boolean selfApproved = false;
						PairedNode temp = null;
						for(PairedNode p: pairedList){
							if(p.pairedID.equals(phoneNo) ){
								if(p.selfPairApproved == true)
								selfApproved = true;
								temp = p;
								break;
							}
						}
			
						if(selfApproved == true){
							//Save the Directory
							temp.OtherPairApproved = true;
							temp.setPaired();
							temp.pairedDirectory = dirList;
							//Populate the directory Struct : Todo
							
						}else{
							PairedNode newPair = new PairedNode(phoneNo, Integer.parseInt(pairKey));
							newPair.OtherPairApproved = true;
							newPair.pairedDirectory = dirList;
							pairedList.add(newPair);
							
						}
						
						

					}

					
				}
				in.close();
				out.close();
				sock.close();
			} catch (IOException e) {
				System.out.println("Error in Server Thread");
			}
		}
	}
}

class ClientAction extends Thread {
	InetAddress destIp;
	int portInt;
	Socket csock;
	String msg = null;
	String ip;
	String port;

	public ClientAction(InetAddress ip, String port, String msg) throws IOException {
		this.destIp = ip;
		this.port = port;
		Log.i("Cliuent Action", ip+" "+port);
/*		try {
			this.destIp = InetAddress.getByName(ip);
		} catch (IOException e) {
			System.out.println("Argument Error: Invalid IP");
			return;
		}
		portInt = Integer.parseInt(port);

		this.csock = new Socket(destIp, portInt);*/
		this.msg = msg;
	}

	public void run() {
		try {
		//	this.destIp = InetAddress.getByName(ip);
			portInt = Integer.parseInt(port);
			this.csock = new Socket(destIp, portInt);
		} catch (IOException e) {
			System.out.println("Argument Error: Invalid IP");
			return;
		}

		
		try {
			PrintWriter out = new PrintWriter(csock.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					csock.getInputStream()));
			out.println(msg);
		} catch (IOException e) {
			System.out.println(e.getMessage() + " Connection Failed");
		}
	}

}