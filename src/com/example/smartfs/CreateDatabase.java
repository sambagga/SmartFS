package com.example.smartfs;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class CreateDatabase extends SQLiteOpenHelper {

	public static final String TABLE_Paired = "paireddevices";
	public static final String COLUMN_Phoneno = "Phoneno";
	public static final String COLUMN_IMEI = "Imei";
	public static final String COLUMN_Directory = "path";
	
	private static final String DATABASE_NAME = "smartfs.db";
	private static final int DATABASE_VERSION = 43;

	// Database creation sql statement
	private static final String DATABASE_CREATE_Paired = "create table "
			+ TABLE_Paired + "( " + COLUMN_Phoneno + " text ,  "
			+ COLUMN_IMEI + " text, " + COLUMN_Directory + " text);";

//	private static final String DATABASE_CREATE_Directory = "create table "
//			+ TABLE_Map + "(" + COLUMN_ID
//			+ " integer primary key autoincrement," + COLUMN_Catagory
//			+ " text, " + COLUMN_P1_X + " integer not null, " + COLUMN_P1_Y
//			+ " integer not null, " + COLUMN_P2_X + " integer not null, "
//			+ COLUMN_P2_Y + " integer not null);";

	public CreateDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_CREATE_Paired);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(CreateDatabase.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_Paired);
		// db.execSQL("DROP TABLE IF EXISTS " + TABLE_Map);
		onCreate(db);
	}

}