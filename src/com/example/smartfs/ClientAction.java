package com.example.smartfs;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.StringTokenizer;

import android.content.Context;
import android.util.Log;


public class ClientAction extends Thread {
	InetAddress destIp;
	int portInt;
	Socket csock;
	String msg = null;
	String ip;
	String port;
	Context cntx;

	public ClientAction(InetAddress ip, String port, String msg, Context cntx)
			throws IOException {
		this.destIp = ip;
		this.port = port;
		this.cntx = cntx;

		Log.i("Client Action", ip + " " + port);
		/*
		 * try { this.destIp = InetAddress.getByName(ip); } catch (IOException
		 * e) { System.out.println("Argument Error: Invalid IP"); return; }
		 * portInt = Integer.parseInt(port);
		 * 
		 * this.csock = new Socket(destIp, portInt);
		 */
		this.msg = msg;
	}

	public void run() {
		try {
			// this.destIp = InetAddress.getByName(ip);
			
			portInt = Integer.parseInt(port);
			Log.i("ClientAction ",destIp+" "+portInt);
			this.csock = new Socket(destIp, portInt);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Argument Error: Invalid IP");
			
			try {
				try {
					if(this.csock != null)
					this.csock.close();
					
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Socket clisock = new Socket(destIp, portInt);
				Log.i("kk", "kk");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return;
			}
			
		}

		if (msg.contains("METADATA_TRANSFER")) {

			Log.i("File Transfer", msg);

			StringTokenizer stok = new StringTokenizer(msg, ";");
			String command = stok.nextToken();
			String phoneNumber = stok.nextToken();
			String argument = stok.nextToken();

			File myFile = cntx.getFileStreamPath(argument);
			byte[] mybytearray = new byte[(int) myFile.length()];
			PrintWriter out = null;
			try {
				out = new PrintWriter(csock.getOutputStream(), false);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						csock.getInputStream()));
				Log.i("File Size ", " " +  myFile.length());
				out.println(msg + Integer.toString((int) myFile.length()) + "\n");
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(e.getMessage() + " Connection Failed");
			}

			Log.i("File Transfer", "File Sending");
			try {
				String str = null;;
				BufferedInputStream bis = new BufferedInputStream(
						cntx.openFileInput(argument));
				BufferedReader brr = new BufferedReader(new InputStreamReader(
						bis));

				while ((str = brr.readLine()) != null) {
					Log.i("Sending ", str);
					out.println(str);
					out.flush();
				}
				brr.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				csock.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} if (msg.contains("FILE_TRANSFER")){
			
			StringTokenizer stok = new StringTokenizer(msg, ";");
			String command = stok.nextToken();
			String remoteLocation = stok.nextToken();
			String localLocation = stok.nextToken();
			
			ObjectOutputStream outStream;
			try {
				PrintWriter out = new PrintWriter(csock.getOutputStream(), false);
				out.println("FILE_TRANSFER" + ";" + remoteLocation);
				out.flush();
				
				outStream = new ObjectOutputStream(
				        csock.getOutputStream());
				
				FileOutputStream fileoutStream = 
	                      new FileOutputStream(localLocation);
				 
	            byte[] buffer = new byte[2000];
	            int bytesRead = 0, counter = 0;
	            
	            ObjectInputStream inStream;
				inStream = new ObjectInputStream(
					        csock.getInputStream());
				long fileSize = inStream.readLong();
				Log.i("TRANSFER: File size", fileSize+"" );
	            while (bytesRead >= 0) {
	                bytesRead = inStream.read(buffer);
	                if (bytesRead >= 0) {
	                    fileoutStream.write(buffer, 0, bytesRead);
	                    counter += bytesRead;
	                    Log.i("total bytes read: " , " "+
	                                                    counter);
	                    fileoutStream.flush();
	                    if(counter >= fileSize)break;
	                }
	            }
	            
	            String msg = "Transfer_Complete";
	            InetSocketAddress remoteAddr = (InetSocketAddress)csock.getRemoteSocketAddress();
	            Log.i("Transfer:" , remoteAddr.toString());
	            csock.getRemoteSocketAddress();
				try {
					Thread th = new Thread(new ClientAction(remoteAddr.getAddress(), Integer.toString(remoteAddr.getPort()),
							msg, null));
					Log.i("Transfer:", remoteAddr.getAddress() + " Port " + remoteAddr.getPort());
					th.start();
				} catch (IOException e) {
					System.out.println("Error in sending passkey");
				}
				
	            
	            
	            Log.i("Download Successfully!" , " ");
	            inStream.close();
	            fileoutStream.close();
	            outStream.close();
	            csock.close();
	            
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
		} else {
			try {
				PrintWriter out = new PrintWriter(csock.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						csock.getInputStream()));
				out.println(msg);
			} catch (IOException e) {
				System.out.println(e.getMessage() + " Connection Failed");
			}
		}
		
		try {
			csock.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}