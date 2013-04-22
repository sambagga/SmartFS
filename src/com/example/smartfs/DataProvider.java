package com.example.smartfs;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

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

	public void insertPairedDevice(String phone, String imei) {
		ContentValues values = new ContentValues();
		values.put(CreateDatabase.COLUMN_Phoneno, phone);
		values.put(CreateDatabase.COLUMN_IMEI, imei);
		database.insert(CreateDatabase.TABLE_Paired, null, values);
	}

	public void insertPath(String path) {
		ContentValues values = new ContentValues();
		values.put(CreateDatabase.COLUMN_Directory, path);
		database.insert(CreateDatabase.TABLE_Paired, null, values);
	}
	
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
}