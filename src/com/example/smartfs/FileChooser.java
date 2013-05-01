package com.example.smartfs;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.text.DateFormat;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.ListActivity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ListView;

public class FileChooser extends ListActivity {

	private File currentDir;
	private FileArrayAdapter adapter;
	public String ipAddress;
	public String port;
	public String phoneNumber;
	public String rootPath;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
		String path = null;
		if (extras != null) {
			path = extras.getString("filePath");
			ipAddress = extras.getString("IP_Address");
			port = extras.getString("Port");
			phoneNumber = extras.getString("PhoneNumber");
			rootPath = extras.getString("Path");
		}
		Log.i("In File Manager", rootPath+" "+ipAddress);
		currentDir = new File(path);
		fill(currentDir);
	}

	private void fill(File f) {
		File[] dirs = f.listFiles();
		this.setTitle("Current Dir: " + f.getName());
		List<Item> dir = new ArrayList<Item>();
		List<Item> fls = new ArrayList<Item>();
		try {
			for (File ff : dirs) {
				Date lastModDate = new Date(ff.lastModified());
				DateFormat formater = DateFormat.getDateTimeInstance();
				String date_modify = formater.format(lastModDate);
				if (ff.isDirectory()) {

					File[] fbuf = ff.listFiles();
					int buf = 0;
					if (fbuf != null) {
						buf = fbuf.length;
					} else
						buf = 0;
					String num_item = String.valueOf(buf);
					if (buf == 0)
						num_item = num_item + " item";
					else
						num_item = num_item + " items";

					// String formated = lastModDate.toString();
					dir.add(new Item(ff.getName(), num_item, date_modify, ff
							.getAbsolutePath(), "directory_icon"));
				} else {
					fls.add(new Item(ff.getName(), ff.length() + " Byte",
							date_modify, ff.getAbsolutePath(), "file_icon"));
				}
			}
		} catch (Exception e) {

		}
		Collections.sort(dir);
		Collections.sort(fls);
		dir.addAll(fls);
		if (!f.getName().equalsIgnoreCase("sdcard"))
			dir.add(0, new Item("..", "Parent Directory", "", f.getParent(),
					"directory_up"));
		adapter = new FileArrayAdapter(FileChooser.this, R.layout.file_view,
				dir);
		this.setListAdapter(adapter);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		Item o = adapter.getItem(position);
		if (o.getImage().equalsIgnoreCase("directory_icon")
				|| o.getImage().equalsIgnoreCase("directory_up")) {
			currentDir = new File(o.getPath());
			fill(currentDir);
		} else {
			onFileClick(o);
		}
	}

	private void onFileClick(Item o) {
		// Toast.makeText(this, "Folder Clicked: "+ currentDir,
		// Toast.LENGTH_SHORT).show();
		String curPath = currentDir.toString();
		String curFile = o.getName();
		Log.i("File clicked", "Path:" + curPath + ",FileName:" + curFile);
		// getting IP/port Address here
		if (!curFile.equals("Empty")) {

			String temp = rootPath;

			String rootFolder = Environment.getExternalStorageDirectory()
					.getPath() + "/SmartFS/" + phoneNumber + "/";

			String localLocation = curPath + "/" + curFile;
			String remoteLocation = localLocation.replace(rootFolder, "");
			int charPos = remoteLocation.indexOf('/');
			Log.i("Index ", "" + charPos);
			if (charPos > 0) {
				remoteLocation = remoteLocation.substring(charPos);
				Log.i("remoteLocation", remoteLocation);
			}
			remoteLocation = temp + remoteLocation;
			Log.i("remoteLocation", remoteLocation);

			String extension = MimeTypeMap
					.getFileExtensionFromUrl(localLocation);
			if (extension.contains("3gp")) {
				String httpUrl = "http://" + ipAddress + ":" + port
						+ remoteLocation;
				Log.i("Http", httpUrl);
				// File file = new File(httpUrl);
				Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse(httpUrl), "video/*");
				startActivity(intent);

			} else {
				Thread th = null;
				try {
					th = new Thread(new ClientAction(
							InetAddress.getByName(ipAddress), port,
							"FILE_TRANSFER;" + remoteLocation + ";"
									+ localLocation, getBaseContext()));
					th.start();
					Log.i("client Thread Started ", remoteLocation + " ; "
							+ localLocation);
					th.join();
					Log.i("client Thread ended ", remoteLocation + " ; "
							+ localLocation);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				File file = new File(localLocation);
				Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				if (extension.contains("png") || extension.contains("jpg")
						|| extension.contains("gif")) {
					intent.setDataAndType(Uri.fromFile(file), "image/*");
				} else if (extension.contains("pdf")) {
					intent.setDataAndType(Uri.fromFile(file), "application/pdf");
				} else if (extension.contains("doc")
						|| extension.contains("docx")) {
					intent.setDataAndType(Uri.fromFile(file),
							"application/msword");
				} else {
					intent.setDataAndType(Uri.fromFile(file), "*/*");
				}
				startActivity(intent);
				Log.i("Send file:", "IP: " + ipAddress + " Port: " + port);
			}
		}

	}
	
	@Override
	public void onBackPressed(){
		Intent intent = new Intent();
		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	protected void onStop() {
		Intent intent = new Intent();
//		intent.putExtra("GetPath", "Empty");
//		intent.putExtra("GetFileName", "Empty");
		setResult(RESULT_OK, intent);
//		finish();
		super.onStop();
	}
}
