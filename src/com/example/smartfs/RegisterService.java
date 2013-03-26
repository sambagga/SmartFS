// Licensed under Apache License version 2.0
// Original license LGPL

//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.example.smartfs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import android.util.Log;

/**
 * Sample Code for Service Registration using JmDNS.
 * <p>
 * To see what happens, launch the TTY browser of JmDNS using the following command:
 *
 * <pre>
 * java -jar lib/jmdns.jar -bs _http._tcp local.
 * </pre>
 *
 * Then run the main method of this class. When you press 'r' and enter, you should see the following output on the TTY browser:
 *
 * <pre>
 * ADD: service[foo._http._tcp.local.,192.168.2.5:1234,path=index.html]
 * </pre>
 *
 * Press 'r' and enter, you should see the following output on the TTY browser:
 *
 * <pre>
 * ADD: service[foo._http._tcp.local.,192.168.2.5:1234,path=index.html]
 * </pre>
 *
 * REMOVE: foo
 *
 * @author Werner Randelshofer
 */
public class RegisterService extends Thread{

    public final static String TAG = "RegisterService";
    String type,id,name,phoneNo;
    JmDNS mJmDNS;
    public RegisterService(JmDNS mJmDNS, String type, String devID,String username,String phoneNo){
    	this.type = type;
    	this.id = devID;
    	this.name = username;
    	this.phoneNo = phoneNo;
    	this.mJmDNS = mJmDNS;
    }
    /**
     * @param args
     *            the command line arguments
     */
    public void run() {
        
        try {
            Log.i(TAG,"Opening JmDNS...");
            Log.i(TAG,"Opened JmDNS!");
            Random random = new Random();
            
            final HashMap<String, String> values = new HashMap<String, String>();
            values.put("DvNm", id);
            values.put("RemV", "10000");
            values.put("DvTy", "Android");
            values.put("RemN", "Remote");
            values.put("txtvers", "1");
            values.put("PhoneNo", phoneNo);
            byte[] pair = new byte[8];
            random.nextBytes(pair);
            values.put("Pair", toHex(pair));
            
            Log.i(TAG,"Requesting pairing for " + name);
            SmartFS.setServiceInfo(ServiceInfo.create(type, name, 1025, 0, 0, values));
            mJmDNS.registerService(SmartFS.getServiceInfo());

            Log.i(TAG,"\nRegistered Service as " + SmartFS.getServiceInfo());
            while(true){}
//            Log.i(TAG,"Closing JmDNS...");
//            
//           // jmdns.unregisterService(pairservice);
//           // jmdns.unregisterAllServices();
//            jmdns.close();
//            Log.i(TAG,"Done!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final char[] _nibbleToHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static String toHex(byte[] code) {
        StringBuilder result = new StringBuilder(2 * code.length);

        for (int i = 0; i < code.length; i++) {
            int b = code[i] & 0xFF;
            result.append(_nibbleToHex[b / 16]);
            result.append(_nibbleToHex[b % 16]);
        }

        return result.toString();
    }
    
}
