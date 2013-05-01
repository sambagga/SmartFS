package com.example.smartfs;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.matthaynes.xml.dirlist.XmlDirectoryListing;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

	public static InetAddress currentIP;
	public static String currentPort;
	public static String currentPhoneNumber;
	public static InetAddress ipStr = null;
	public static String pairto = null;
	volatile public static ServiceInfo[] list;
	public static DataProvider dbProvider;
	private String type = "_smartfs._tcp.local.";
	private static ServiceInfo serviceInfo;
	JmDNS mJmDNS;
	public static ListView tv;
	String devID, phoneNo;
	String userId;
	String TAG = "SmartFS";
	String folderPath;
	InetAddress hostName;
	ServerSocket serverSocket; /* serverSocket.get */
	ListView listV;
	SparseArray<ServiceInfo> dev;
	AlertDialog mDialog;
	Handler handle = new Handler();
	private List<String> fileList = new ArrayList<String>();
	List<String[]> pairedList;
	public static boolean transferComplete = false;

	// public static LinkedList<PairedNode> pairedList = new
	// LinkedList<PairedNode>();
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		userId = getIntent().getStringExtra("userID");
		dbProvider = new DataProvider(getBaseContext());
		dbProvider.open();
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
				pairto = s.getPropertyString("TCPPort");
				ipStr = s.getInet4Addresses()[s.getInet4Addresses().length - 1];
				String pairingIMEI = s.getPropertyString("IMEI");
				String pairingID = s.getPropertyString("PhoneNo");
				currentPhoneNumber = pairingID;

				pairedList = dbProvider.getAllPairedDevices();
				for (String[] p : pairedList) {
					Log.i("All Devices", p[0]);
					if (p[0].equals(pairingID)) {
						arePaired = true;
					}
				}

				if (arePaired == false) {
					Random randomGenerator = new Random();
					int pairKey = randomGenerator.nextInt(99999);
					// send pair key to port.

					String msg = "Pair_Device" + ","
							+ Integer.toString(pairKey) + "," + getPhoneNo()
							+ "," + getDeviceID(getBaseContext());
					try {
						Thread th = new Thread(new ClientAction(ipStr, pairto,
								msg, getBaseContext()));
						th.start();
					} catch (IOException e) {
						System.out.println("Error in sending passkey");
					}

					initiateTwoButtonAlert("Pairing request to " + pairingID
							+ ", Code " + pairKey, "Yes", "No", ipStr, pairto,
							pairKey, pairingID, pairingIMEI);

					Log.i("Requesting connection to " + id + " device",
							s.getPropertyString("PhoneNo"));
				} else {
					// Do Nothing.
					// Populate the list
					String rootFolder = Environment
							.getExternalStorageDirectory().getPath()
							+ "/SmartFS/" + pairingID;
					String rpath = dbProvider.getPath(currentPhoneNumber);
					Log.i("Moving to file explorer", rootFolder + " " + rpath);
					Intent intentFileExp = new Intent(getBaseContext(),
							FileChooser.class);
					intentFileExp.putExtra("filePath", rootFolder);
					intentFileExp.putExtra("IP_Address", ipStr.getHostAddress()
							.toString());
					intentFileExp.putExtra("Port", pairto);
					intentFileExp.putExtra("PhoneNumber", currentPhoneNumber);
					intentFileExp.putExtra("Path", rpath);
					startActivityForResult(intentFileExp, 2);
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
			final String pairingID, final String pairingIMEI) {

		mDialog = new AlertDialog.Builder(this)
				.setTitle(getResources().getString(R.string.app_name))
				.setMessage(displayText)
				.setIcon(R.drawable.ic_launcher)
				.setPositiveButton(positiveButtonText,
						new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface arg0, int arg1) {
								Log.i("Dialog", "Clicked Yes");
								// Already Received his Directory List?
								sendDirectoryView(ip, port, pairingID);
							}
						}).setNegativeButton(negativeButtonText, null).show();

		WindowManager.LayoutParams layoutParams = mDialog.getWindow()
				.getAttributes();
		layoutParams.dimAmount = 0.9f;
		mDialog.getWindow().setAttributes(layoutParams);
		mDialog.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	}

	private void sendDirectoryView(InetAddress ip, String port, String pairingID) {

		currentIP = ip;
		currentPort = port;

		Intent intent = new Intent(getBaseContext(), FolderPickerTest.class);
		Log.v(this.toString(), "Intent created. Moving to Folder Picker.");
		startActivityForResult(intent, 1);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.i("folderPath", "Returned to main");
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1 && resultCode == RESULT_OK) {
			folderPath = data.getStringExtra("folderPath");
			if (folderPath.equals("")) {
				Log.i("folderPath", "Cancel");
			} else {
				Log.i("folderPath", folderPath);
				// Generate The xml File here for folder.

				XmlDirectoryListing lister = new XmlDirectoryListing();
				FileOutputStream out = null;
				try {
					out = this.openFileOutput("file.xml", Context.MODE_PRIVATE);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				lister.generateXmlDirectoryListing(new File(folderPath), out);
				try {
					out.close();
					Thread th = new Thread(new ClientAction(currentIP,
							currentPort, "METADATA_TRANSFER;" + phoneNo
									+ ";file.xml;", getBaseContext()));
					th.start();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// do stuff with path
		} else if (requestCode == 2) {
			Log.i("Returned from File Chooser", "1");
			if (resultCode == RESULT_OK) {
				Log.i("Returned from File Chooser", "2");
			}
		} else
			Log.i("folderPath", "Not good!");
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
			String myPhone = getPhoneNo();
			Log.i("myPhone", myPhone);
			int j = 0;
			for (int i = 0; i < list.length; i++) {
				if (!list[i].getPropertyString("PhoneNo").equals(myPhone)) {
					String[] temp = dbProvider.getSelectedDevice(list[i]
							.getPropertyString("PhoneNo"));
					if (temp != null) {
						adapter.add(list[i].getName() + "\n"
								+ list[i].getPropertyString("PhoneNo")
								+ " Paired");
					} else {
						adapter.add(list[i].getName() + "\n"
								+ list[i].getPropertyString("PhoneNo"));
					}
					dev.put(j, list[i]);
					j++;
				}
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
		// dbProvider.close();
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
					Log.i("PORT number ", "" + ssock.getLocalPort());
					Thread th = new Thread(new ConnectionHandler(connection));
					th.start();
				} catch (Exception e) {
					System.err.println("Connetion Accept failed!");
					e.printStackTrace();
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
					Log.i("Message Received", sreceive);
					if (sreceive.contains("get_root")) {
						/* Get the MetaData and send to out */

					} else if (sreceive.contains("Transfer_Complete")) {
						/* Get the MetaData and send to out */
						Log.i("Transfer", "setting Transfer Complete");
						transferComplete = true;

					} else if (sreceive.contains("METADATA_TRANSFER")) {
						/* Get the MetaData and send to out */

						StringTokenizer stok = new StringTokenizer(sreceive,
								";");
						String command = stok.nextToken();
						String phoneNumber = stok.nextToken();
						String fileName = stok.nextToken();
						int length = Integer.parseInt(stok.nextToken());

						Log.i("File Transfer Receiver ", fileName);

						byte[] mybytearray = new byte[length];

						InputStream is = sock.getInputStream();
						// BufferedReader br = new BufferedReader(
						// new InputStreamReader(is));

						FileOutputStream fos = new FileOutputStream(Environment
								.getExternalStorageDirectory().getPath()
								+ "/"
								+ phoneNumber + ".xml");
						BufferedOutputStream bos = new BufferedOutputStream(fos);
						Log.i("File path ", Environment
								.getExternalStorageDirectory().getPath()
								+ "/"
								+ phoneNumber + ".xml");

						while ((sreceive = in.readLine()) != null) {
							Log.i("Received ", sreceive);
							bos.write(sreceive.getBytes(), 0, sreceive.length());
						}

						Log.i("File Receiving Done", "--");
						bos.close();
						fos.close();
						is.close();
						String rootFolder = Environment
								.getExternalStorageDirectory().getPath()
								+ "/SmartFS/" + phoneNumber;

						DocumentBuilderFactory factory = DocumentBuilderFactory
								.newInstance();
						DocumentBuilder builder;
						Document doc = null;

						try {
							builder = factory.newDocumentBuilder();
							String filePath = Environment
									.getExternalStorageDirectory().getPath()
									+ "/" + phoneNumber + ".xml";
							File file = new File(filePath);
							doc = builder.parse(file);
						} catch (ParserConfigurationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SAXException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						// Get a list of all elements in the document
						NodeList list = doc.getElementsByTagName("file");
						NodeList dir = doc.getElementsByTagName("directory");

						Element dirElement = (Element) dir.item(0);
						String absolutePath = dirElement
								.getAttribute("absolutePath");
						// absolutePth to be put in DB
						Log.i("Get Phone no", phoneNumber + " " + phoneNo);
						Log.i("New Device found", phoneNumber);

						dbProvider
								.insertPairedDevice(phoneNumber, absolutePath);
						String[] temp = dbProvider
								.getSelectedDevice(phoneNumber);
						Log.i("Device", temp[0] + " " + temp[2]);
						String folderName = dirElement.getAttribute("name");

						for (int i = 0; i < list.getLength(); i++) {
							// Get element
							Element element = (Element) list.item(i);
							System.out.println(element
									.getAttribute("absolutePath"));

							File destFile = new File(rootFolder + "/"
									+ folderName, element.getAttribute(
									"absolutePath").replace(absolutePath, ""));
							destFile.getParentFile().mkdirs();
							try {
								destFile.createNewFile();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						Log.i("Dir creation done", "");
					} else if (sreceive.contains("FILE_TRANSFER")) {

						StringTokenizer stok = new StringTokenizer(sreceive,
								";");
						String command = stok.nextToken();
						String localLocation = stok.nextToken();
						FileInputStream fileStream = new FileInputStream(
								localLocation);

						long fileSize = fileStream.available();
						Log.i("File Size ", "" + fileSize);
						long completed = 0;
						int step = 0;
						ObjectOutputStream outStream = new ObjectOutputStream(
								sock.getOutputStream());
						outStream.writeLong(fileSize);
						outStream.flush();
						byte[] buffer = new byte[1024];
						step = fileStream.read(buffer);
						while (completed <= fileSize && step > 0) {
							Log.i("Sending.....", " " + completed);
							outStream.write(buffer);
							outStream.flush();
							completed += step;
							step = fileStream.read(buffer);
						}
						byte[] buffer1 = new byte[40];
						ObjectInputStream inStream = new ObjectInputStream(
								sock.getInputStream());
						inStream.read(buffer1);
						// if(buffer1.toString().contains("Transfer_Complete")){}

						Log.i("Transfer", "Exiting While Loop");
						transferComplete = false;
						fileStream.close();
						outStream.close();

					} else if (sreceive.contains("Pair_Device")) {

						/*
						 * String msg = "Pair_Device" + "," +
						 * Integer.toString(pairKey) + "," + getPhoneNo();
						 */
						String[] split = sreceive.split(",");

						final String pairKey = split[1];
						final String phoneNo = split[2];
						final String IMEI = split[3];
						// sreceive.substring(sreceive.indexOf(",") + 1);
						InetSocketAddress remoteAddr = (InetSocketAddress) sock
								.getRemoteSocketAddress();
						// String ip = remoteAddr.getHostName();
						final InetAddress ip = remoteAddr.getAddress();
						final int port = remoteAddr.getPort();
						handle.post(new Runnable() {
							public void run() {
								String pairto_ = null;
								InetAddress ipStr_ = null;
								String pairingIMEI;
								String pairingID;
								
								for (int i = 0; i < dev.size(); i++) {
									ServiceInfo s = dev.get((int) i);
									if (s.getPropertyString("PhoneNo").equals(
											phoneNo)) {
										pairto_ = s
												.getPropertyString("TCPPort");
										ipStr_ = s
												.getInet4Addresses()[s
												.getInet4Addresses().length - 1];
										pairingIMEI = s
												.getPropertyString("IMEI");
										pairingID = s
												.getPropertyString("PhoneNo");
									}
								}
								initiateTwoButtonAlert("Pairing Request from "
										+ phoneNo + ", Code " + pairKey, "Yes",
										"No", ipStr_, pairto_,
										Integer.parseInt(pairKey),
										currentPhoneNumber, IMEI);

							}
						});

					} else if (sreceive.contains("HTTP")) {

						StringTokenizer stok = new StringTokenizer(sreceive,
								" ");
						stok.nextToken();
						String fileName = stok.nextToken();
						String header = null;
						int cbSkip = 0;
						while ((header = in.readLine()) != null) {
							if (header.isEmpty()) {
								Log.i("Empty", " ");
								break;
							}
							Log.i("HTTp ", header);
							if (header.startsWith("Range: bytes=")) {
								String headerLine = header.substring(13);
								int charPos = headerLine.indexOf('-');
								if (charPos > 0) {
									headerLine = headerLine.substring(0,
											charPos);
								}
								cbSkip = Integer.parseInt(headerLine);
								break;
							}
						}

						String response = null;
						byte[] buff = new byte[64 * 1024];
						File myFile = new File(fileName);
						response = "HTTP/1.1 200 OK\r\nContent-Type: video/3gpp\r\ncontent-length: "
								+ myFile.length()
								+ "\r\nAccept-Ranges: bytes\r\nConnection: close\r\n\r\n";
						Log.i("File Transfer", "File Sending");

						FileInputStream input_ = new FileInputStream(fileName);

						long cbToSend = input_.available() - cbSkip;
						input_.close();
						BufferedOutputStream output = new BufferedOutputStream(
								sock.getOutputStream(), 32 * 1024);
						output.write(response.getBytes());

						while (cbToSend > 0 && !sock.isClosed()) {

							// See if there's more to send
							File file = new File(fileName);

							if (file.exists()) {
								FileInputStream input = new FileInputStream(
										file);
								input.skip(cbSkip);
								int cbToSendThisBatch = input.available();
								while (cbToSendThisBatch > 0
										&& !sock.isClosed()) {
									int cbToRead = Math.min(cbToSendThisBatch,
											buff.length);
									int cbRead = input.read(buff, 0, cbToRead);
									if (cbRead == -1) {
										break;
									}
									cbToSendThisBatch -= cbRead;
									cbToSend -= cbRead;
									output.write(buff, 0, cbRead);
									output.flush();
									cbSkip += cbRead;
								}
								input.close();
							}
						}
					}
				}
				in.close();
				out.close();
				sock.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Error in Server Thread");
			}
		}
	}
}
