package com.example.smartfs;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DataProvider {

	// Database fields
	private SQLiteDatabase database;
	private CreateDatabase dbHelper;
	private String[] allPairedColumns = { CreateDatabase.COLUMN_Phoneno,
			CreateDatabase.COLUMN_IMEI, CreateDatabase.COLUMN_Directory };

	public DataProvider(Context context) {
		dbHelper = new CreateDatabase(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public void insertPairedDevice(String phone,String path) {
		
		ContentValues values = new ContentValues();
		values.put(CreateDatabase.COLUMN_Phoneno, phone);
		values.put(CreateDatabase.COLUMN_IMEI, "");
		values.put(CreateDatabase.COLUMN_Directory, path);
		Log.i("Insert database", phone + " " + path);
		long i = database.insert(CreateDatabase.TABLE_Paired, null, values);
		Log.i("Insert ", " " + i);
	}

//	public void insertPath(String phone,String path) {
//		ContentValues values = new ContentValues();
//		values.put(CreateDatabase.COLUMN_Directory, path);
//		database.update(CreateDatabase.TABLE_Paired, values, "Phoneno='"+phone +"'", null);
//	}

	public void deletePairedDevice(String phone) {
		// Log.i("ItemsMap deleted with id: " ,phone);
		// database.delete(CreateDataBase.TABLE_ItemsMapS,
		// CreateDataBase.COLUMN_ID + " = " + id, null);

	}

	public String getPath(String phone) {

		Cursor cursor = database.query(CreateDatabase.TABLE_Paired,
				allPairedColumns, CreateDatabase.COLUMN_Phoneno + " = '"
						+ phone + "'", null, null, null, null);

		cursor.moveToFirst();
		String path = cursorToPath(cursor);
		cursor.close();

		return path;
	}

	private String cursorToPath(Cursor cursor) {
		String path = (cursor.getString(2));
		return path;
	}

	private String[] cursorToStrings(Cursor cursor) {
		String[] path = new String[3];
		path[0] = cursor.getString(0);
		path[1] = cursor.getString(1);
		path[2] = cursor.getString(2);
		return path;
	}

	public String[] getSelectedDevice(String phoneno) {
		String[] device = null;

		Cursor cursor = database.query(CreateDatabase.TABLE_Paired,
				allPairedColumns, CreateDatabase.COLUMN_Phoneno + " = '"
						+ phoneno + "'", null, null, null, null);
		
		cursor.moveToFirst();
		if (!cursor.isAfterLast()) {
			device = cursorToStrings(cursor);
		}

		cursor.close();

		return device;
	}

	public List<String[]> getAllPairedDevices() {
		List<String[]> pairedDevices = new ArrayList<String[]>();

		Cursor cursor = database.query(CreateDatabase.TABLE_Paired,
				allPairedColumns, null, null, null, null, null);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			String[] devices = cursorToStrings(cursor);
			pairedDevices.add(devices);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return pairedDevices;
	}
}