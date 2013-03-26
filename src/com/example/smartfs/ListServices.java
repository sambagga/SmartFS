// Licensed under Apache License version 2.0
// Original license LGPL

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
import javax.jmdns.JmDNS;
import android.util.Log;

/**
 * Sample Code for Listing Services using JmDNS.
 * <p>
 * Run the main method of this class. This class prints a list of available HTTP services every 5 seconds.
 *
 * @author Werner Randelshofer
 */
public class ListServices extends Thread{
	public final static String TAG = "ListServices";
    /**
     * @param args
     *            the command line arguments
     */
	String type;
	JmDNS mJmDNS;
	public ListServices(JmDNS mJmDNS,  String type) {
		// TODO Auto-generated constructor stub
		this.type = type;
		this.mJmDNS = mJmDNS;
	}
	
    public void run() {        
        	Log.i(TAG,"Opening JmDNS...");
            Log.i(TAG,"Opened JmDNS!");
            Log.i(TAG,"Getting List...");
            SmartFS.list = mJmDNS.list(type);
            Log.i(TAG,"Got the List of Devices!");
    }
}
