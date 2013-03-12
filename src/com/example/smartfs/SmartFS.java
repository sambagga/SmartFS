package com.example.smartfs;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class SmartFS extends Activity {
	android.net.wifi.WifiManager.MulticastLock lock;
	android.os.Handler handler = new android.os.Handler();
	public static ServiceInfo[] list;
	private String type = "_smartfs._tcp.local.";
	private static ServiceInfo serviceInfo;
	TextView tv;
	String devID;
	String TAG = "SmartFS";
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());

		handler.postDelayed(new Runnable() {
			public void run() {
				setUp();
			}
		}, 1000);
		devID = getDeviceID(this);
		Log.i(TAG,devID);
		Thread th = new Thread(new RegisterService(type,devID));
		th.start();
	}

	/** Called when the activity is first created. */
	private void setUp() {
		android.net.wifi.WifiManager wifi = (android.net.wifi.WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
		lock = wifi.createMulticastLock("mylockthereturn");
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
        TelephonyManager manager = 
            (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        String deviceId;
        if (manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
            //Tablet
             deviceId = Secure.getString(this.getContentResolver(),
                    Secure.ANDROID_ID);

        } else {
            //Mobile
             deviceId = manager.getDeviceId();

        }
        return deviceId;
    }
	
	public void listServices(View view) {
		Thread th = new Thread(new ListServices(type));
		th.start();
		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				tv.append("" + list[i]);
			}
		}
	}

	@Override
	protected void onStop() {
		try {
			JmDNS jmdns = JmDNS.create();
			jmdns.unregisterService(getServiceInfo());
			jmdns.unregisterAllServices();

			jmdns.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		lock.release();
		super.onStop();
	}
}