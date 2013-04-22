package com.example.smartfs;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class FolderPickerTest extends Activity implements
		DialogInterface.OnClickListener {
	private FolderPicker mFolderDialog;
	String path;
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFolderDialog = new FolderPicker(this, this, 0);
		mFolderDialog.show();
	}

	public void onClick(DialogInterface dialog, int which) {
		if (dialog == mFolderDialog && which == DialogInterface.BUTTON_POSITIVE) {
			// ((TextView)
			// findViewById(R.id.folder_path)).setText(mFolderDialog.getPath());
			path =(mFolderDialog.getPath());
			Log.i("Folder Picker","Path:"+path);
			Intent result = new Intent();
			result.putExtra("folderPath", path);
			setResult(RESULT_OK, result);
			Log.i(this.toString(), "Moving to source Activity");
			finish();
		}else{
			Log.i("Folder Picker","Path: Cancels ");
			Intent result = new Intent();
			result.putExtra("folderPath", "");
			setResult(RESULT_OK, result);
			Log.i(this.toString(), "Moving to source Activity");
			finish();
		}
	}
	

}