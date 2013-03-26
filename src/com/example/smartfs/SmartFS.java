package com.example.smartfs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

public class SmartFS extends Activity {
	android.net.wifi.WifiManager.MulticastLock lock;
	android.os.Handler handler = new android.os.Handler();
	volatile public static ServiceInfo[] list;
	private String type = "_smartfs._tcp.local.";
	private static ServiceInfo serviceInfo;
	JmDNS mJmDNS;
	TextView tv;
	String devID, phoneNo;
	String userId;
	String TAG = "SmartFS";
	InetAddress hostName;
	ListView listV;
	SparseArray<ServiceInfo> dev;
	AlertDialog mDialog;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		userId = getIntent().getStringExtra("userID");

		listV = (ListView) findViewById(R.id.listView1);
		listV.canScrollVertically(0);

		// Create a message handling object as an anonymous class.
		OnItemClickListener devListHandler = new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View v, int position,
					long id) {
				ServiceInfo s = dev.get((int) id);
				Random randomGenerator = new Random();
				initiateTwoButtonAlert("Pair Code "+randomGenerator.nextInt(99999),"Yes","No");
				Log.i("Requesting connection to " + id + " device",
						s.getPropertyString("PhoneNo"));
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
				phoneNo));
		th.start();
	}
	
	private void initiateTwoButtonAlert(String displayText,
	         String positiveButtonText, String negativeButtonText) {
	     mDialog = new AlertDialog.Builder(this)
	             .setTitle(getResources().getString(R.string.app_name))
	             .setMessage(displayText)
	             .setIcon(R.drawable.ic_launcher)
	             .setPositiveButton(positiveButtonText, null)
	             .setNegativeButton(negativeButtonText, null)
	             .show();
	 
	     WindowManager.LayoutParams layoutParams = mDialog.getWindow().getAttributes();
	     layoutParams.dimAmount = 0.9f;
	     mDialog.getWindow().setAttributes(layoutParams);
	     mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	 }
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
}