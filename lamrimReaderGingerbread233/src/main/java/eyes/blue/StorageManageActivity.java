package eyes.blue;

import java.io.File;
import java.util.ArrayList;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
//import android.support.v4.provider.DocumentFile;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class StorageManageActivity extends AppCompatActivity {
	private static final int STORAGE_ACCESS_PERMISSION_REQUEST=1;
	private static final int OPEN_DOCUMENT_TREE_ABV_23 = 1;
	private static final int OPEN_DOCUMENT_TREE_UND_23 = 2;
	FileSysManager fsm=null;
	TextView extSpeechPathInfo, extSubtitlePathInfo, intSpeechPathInfo, intSubtitlePathInfo, extFreePercent, intFreePercent, extAppUsagePercent, intAppUsagePercent, intFree, extFree, extAppUseage, intAppUseage, labelChoicePath;
	Button btnMoveAllToExt, btnMoveAllToInt, btnMoveToUserSpy, btnDelExtFiles, btnDelIntFiles, btnOk;
	ImageButton btnChoicePath;
	RadioGroup radioMgnType =null;
	EditText filePathInput;
	boolean isUseThirdDir=false;
	
//	private PowerManager.WakeLock wakeLock = null;
	SharedPreferences runtime = null;
	
	long intFreeB, extFreeB, intTotal, extTotal, intUsed, extUsed;
	String userSpecDir;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.storage_manage);
		Log.d(getClass().getName(),"Into onCreate");
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//		PowerManager powerManager=(PowerManager) getSystemService(Context.POWER_SERVICE);
//		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getClass().getName());

		AnalyticsApplication application = (AnalyticsApplication) getApplication();
		application.getDefaultTracker();

		runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);
		fsm=new FileSysManager(this);
		//if(!wakeLock.isHeld()){wakeLock.acquire();}
		
		extFreePercent = (TextView)findViewById(R.id.extFreePercent);
		intFreePercent = (TextView)findViewById(R.id.intFreePercent);
		extAppUsagePercent = (TextView)findViewById(R.id.extAppUsagePercent);
		intAppUsagePercent = (TextView)findViewById(R.id.intAppUsagePercent);
		intFree = (TextView)findViewById(R.id.intFree);
		extFree = (TextView)findViewById(R.id.extFree);
		extAppUseage = (TextView)findViewById(R.id.extAppUseage);
		intAppUseage = (TextView)findViewById(R.id.intAppUseage);
		labelChoicePath = (TextView)findViewById(R.id.labelChoicePath);
		btnMoveAllToExt = (Button) findViewById(R.id.btnMoveAllToExt);
		btnMoveAllToInt = (Button) findViewById(R.id.btnMoveAllToInt);
		btnDelExtFiles = (Button) findViewById(R.id.btnDelExtFiles);
		btnDelIntFiles = (Button) findViewById(R.id.btnDelIntFiles);
		btnChoicePath = (ImageButton) findViewById(R.id.btnChoicePath);
		btnMoveToUserSpy = (Button) findViewById(R.id.moveToUserSpyDirBtn);
		btnOk = (Button) findViewById(R.id.btnOk);
		radioMgnType = (RadioGroup) findViewById(R.id.radioMgnType);
		filePathInput = (EditText) findViewById(R.id.fieldPathInput);
		extSpeechPathInfo = (TextView) findViewById(R.id.extSpeechPathInfo);
		extSubtitlePathInfo = (TextView) findViewById(R.id.extSubtitlePathInfo);
		intSpeechPathInfo = (TextView) findViewById(R.id.intSpeechPathInfo);
		intSubtitlePathInfo = (TextView) findViewById(R.id.intSubtitlePathInfo);

		// The ImageButton can't disable from xml.
		btnChoicePath.setClickable(false);
		btnChoicePath.setEnabled(false);
		btnMoveToUserSpy.setEnabled(false);
		
		isUseThirdDir=runtime.getBoolean(getString(R.string.isUseThirdDir),false);
		if(isUseThirdDir){
			radioMgnType.check(R.id.radioUserMgnStorage);
			filePathInput.setEnabled(true);
			btnChoicePath.setClickable(true);
			btnChoicePath.setEnabled(true);
			labelChoicePath.setEnabled(true);
			btnMoveToUserSpy.setEnabled(true);
		}
		
		String thirdDir=runtime.getString(getString(R.string.userSpecifySpeechDir),null);
		if(thirdDir==null || thirdDir.length()==0)thirdDir=fsm.getSysDefMediaDir();
		filePathInput.setText(thirdDir,null);
		
		btnMoveAllToExt.setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder=getConfirmDialog();
				builder.setTitle("移動檔案");
				builder.setMessage("您確定要移動檔案嗎？");
				builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						fsm.moveAllFilesTo(FileSysManager.INTERNAL,FileSysManager.EXTERNAL,new CopyListener(){
							@Override
							public void copyFinish(){
								refreshUsage();
							}
						});
					}});
				builder.create().show();
			}});
		btnMoveAllToInt.setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder=getConfirmDialog();
				builder.setTitle("移動檔案");
				builder.setMessage("您確定要移動檔案嗎？");
				builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						fsm.moveAllFilesTo(FileSysManager.EXTERNAL,FileSysManager.INTERNAL,new CopyListener(){
							@Override
							public void copyFinish(){
								refreshUsage();
							}
							@Override
							public void copyFail(final File from, final File to){
								runOnUiThread(new Runnable(){
									@Override
									public void run() {
										Util.showErrorPopupWindow(StorageManageActivity.this, findViewById(R.id.smRootView), "搬移檔案時發生錯誤: 來源 "+from.getAbsolutePath()+", 目的地:  "+to.getAbsolutePath());
								}});
								
							}
						});
					}});
				builder.create().show();
			}});
		
		btnMoveToUserSpy.setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View arg0) {
				btnMoveToUserSpy.setEnabled(false);

				AnalyticsApplication.sendEvent("statistics", "MOVE_FILE_TO_SPECIFY_FOLDER", "CLICK", 1);
				
				Log.d(getClass().getName(),"thread started");
				String path=filePathInput.getText().toString();
				if(path==null||path.length() == 0){
					Util.showErrorPopupWindow(StorageManageActivity.this, "使用者指定路徑錯誤，無法移動檔案。");
					btnMoveToUserSpy.setEnabled(true);
					return;
				}
				File filePath=new File(path);
				if(filePath.isFile()){
					Util.showErrorPopupWindow(StorageManageActivity.this, "使用者指定目錄所指定的位置為已存在的檔案，請重新選擇！");
					btnMoveToUserSpy.setEnabled(true);
					return;
				}
				Log.d(getClass().getName(),"Create folder: "+path);
				filePath.mkdir();
				if(!filePath.exists() || !filePath.isDirectory() || !filePath.canWrite()){
					Util.showErrorPopupWindow(StorageManageActivity.this, "使用者指定目錄錯誤或無寫入權限，無法移動檔案。");
					btnMoveToUserSpy.setEnabled(true);
					return;
				}
				
				// Check the path is not external/internal default storage path.
				ArrayList<String> srcList=new ArrayList<String>();
				srcList.add(fsm.getSrcRootPath(FileSysManager.INTERNAL)+File.separator+getString(R.string.audioDirName));
				String ext=fsm.getSrcRootPath(FileSysManager.EXTERNAL);
				if(ext!=null)srcList.add(ext+File.separator+getString(R.string.audioDirName));
		    	
				Log.d(getClass().getName(),	"There are "+srcList.size()+" src folder for move file.");
				Intent intent = new Intent(StorageManageActivity.this,	MoveFileService.class);
				intent.putStringArrayListExtra("srcDirs", srcList);
				intent.putExtra("destDir",path);
				
				Log.d(getClass().getName(),	"Start move file service.");
				
				// While user press the move button that mean the path is specified.
				SharedPreferences.Editor editor = runtime.edit();
				editor.putBoolean(getString(R.string.isUseThirdDir), true);
				editor.putString(getString(R.string.userSpecifySpeechDir), filePathInput.getText().toString());
				editor.commit();
				
				Util.showInfoPopupWindow(StorageManageActivity.this, "背景移動中，請檢視通知列以瞭解進度，移動過程中請勿執行其他操作。");
				startService(intent);
				AnalyticsApplication.sendEvent("ui_action", "botton_pressed", "reloadLastState_MoveFileToUserSpecify", null);
				
				refreshUsage();
				btnMoveToUserSpy.setEnabled(true);
		}});
		
		btnDelExtFiles.setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder=getConfirmDialog();
				builder.setTitle(String.format(getString(R.string.dlgDelWarnTitle),"檔案"));
				builder.setMessage(String.format(getString(R.string.dlgDelWarnMsg),"檔案"));
				builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final ProgressDialog pd= new ProgressDialog(StorageManageActivity.this);
						pd.setTitle("刪除檔案");
						pd.setMessage("刪除中，請稍候...");
						pd.show();
						
						Thread t=new Thread(new Runnable(){
							@Override
							public void run() {
								fsm.deleteAllSpeechFiles(FileSysManager.EXTERNAL);
								fsm.deleteAllSubtitleFiles(FileSysManager.EXTERNAL);
								if(pd.isShowing())pd.dismiss();
								runOnUiThread(new Runnable(){
									@Override
									public void run() {
										refreshUsage();
										btnDelExtFiles.setEnabled(false);
								}});
							}});
						t.start();
					}});
				builder.create().show();
			}});
		btnDelIntFiles.setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder=getConfirmDialog();
				builder.setTitle(String.format(getString(R.string.dlgDelWarnTitle),"檔案"));
				builder.setMessage(String.format(getString(R.string.dlgDelWarnMsg),"檔案"));
				builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final ProgressDialog pd= new ProgressDialog(StorageManageActivity.this);
						pd.setTitle("刪除檔案");
						pd.setMessage("刪除中，請稍候...");
						pd.show();
						
						Thread t=new Thread(new Runnable(){
							@Override
							public void run() {
								fsm.deleteAllSpeechFiles(FileSysManager.INTERNAL);
								fsm.deleteAllSubtitleFiles(FileSysManager.INTERNAL);
								if(pd.isShowing())pd.dismiss();
								runOnUiThread(new Runnable(){
									@Override
									public void run() {
										refreshUsage();
										btnDelIntFiles.setEnabled(false);
								}});
								
							}});
						t.start();
					}});
				builder.create().show();
			}});
		btnChoicePath.setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				if(Build.VERSION.SDK_INT >= 23){
					int permissionCheck = ContextCompat.checkSelfPermission(StorageManageActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
					if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
						ActivityCompat.requestPermissions(StorageManageActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_ACCESS_PERMISSION_REQUEST);
					} else {
						// Your app already has the permission to access files and folders
						// so you can simply open FileChooser here.
						showFileDialogActivity();
					}
				}
				else
					showFileDialogActivity();
			}});

/*
		btnChoicePath.setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getBaseContext(), FileChooserActivity.class);
				intent.putExtra(FileChooserActivity.INPUT_FOLDER_MODE, true);
				intent.putExtra(FileChooserActivity.INPUT_CAN_CREATE_FILES, true);
				intent.putExtra(FileChooserActivity.INPUT_START_FOLDER, Environment.getExternalStorageDirectory());
				startActivityForResult(intent, OPEN_DOCUMENT_TREE_UND_19);
			}});
*/
		
		btnOk.setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor editor = runtime.edit();
				if(!isUseThirdDir){
					Log.d(getClass().getName(),"is user specify the third dir? "+isUseThirdDir);
					editor.putBoolean(getString(R.string.isUseThirdDir), false);
					editor.commit();
					finish();
					return;
				}
				
				if(filePathInput.getText().toString().length()==0){
					filePathInput.setText(fsm.getSysDefMediaDir());
					BaseDialogs.showErrorDialog(StorageManageActivity.this, "目錄錯誤", "路徑不可為空！請重新選擇。");
					return;
				}
					// Check is the path is FILE
				File f=new File(filePathInput.getText().toString());
				if(f.isFile()){
					BaseDialogs.showErrorDialog(StorageManageActivity.this, "目錄錯誤", "您所指定的儲存位置為檔案！請重新選擇。");
					return;
				}
				
				// Write file test
				f.mkdir();//- add for android 5.0 support -
				// The kitkat can read external area, but not write.
				if(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT){
					if((!f.exists() || !f.canRead())){
						BaseDialogs.showErrorDialog(StorageManageActivity.this, "權限錯誤", "您所指定的儲存目錄無法建立或無讀取權限！請重新選擇。");
						return;
					}
				}
				else{
					if(!f.exists() || !f.canWrite())
					{
						BaseDialogs.showErrorDialog(StorageManageActivity.this, "權限錯誤", "您所指定的儲存位置無法寫入！請重新選擇。");
						return;
					}
				}
				
				editor.putBoolean(getString(R.string.isUseThirdDir), true);
				editor.putString(getString(R.string.userSpecifySpeechDir), filePathInput.getText().toString());
				editor.commit();
				
				if(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT){
					BaseDialogs.showErrorDialog(StorageManageActivity.this, "無法自動補檔警告", "您的系統為Kitkat(4.4)版，由於系統限制，外部目錄僅能讀取，無法自動補檔，請確認該目錄中包含所有音檔。");
				}
				else
					finish();
			}});
		
		radioMgnType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
			public void onCheckedChanged(RadioGroup group, int checkedId){

				switch(checkedId)
				{
				case R.id.radioAutoMgnStorage:
					isUseThirdDir=false;
					filePathInput.setEnabled(false);
					btnChoicePath.setEnabled(false);
					btnChoicePath.setClickable(false);
					labelChoicePath.setEnabled(false);
					btnMoveToUserSpy.setEnabled(false);
					break;
				case R.id.radioUserMgnStorage:
					isUseThirdDir=true;
					filePathInput.setEnabled(true);
					btnChoicePath.setEnabled(true);
					btnChoicePath.setClickable(true);
					labelChoicePath.setEnabled(true);
					btnMoveToUserSpy.setEnabled(true);
					break;
				}
			}});
		
		String extSpeechDir=fsm.getLocateDir(FileSysManager.EXTERNAL, getResources().getInteger(R.integer.MEDIA_TYPE));
		String extSubtitleDir=fsm.getLocateDir(FileSysManager.EXTERNAL, getResources().getInteger(R.integer.SUBTITLE_TYPE));
		String intSpeechDir=fsm.getLocateDir(FileSysManager.INTERNAL, getResources().getInteger(R.integer.MEDIA_TYPE));
		String intSubtitleDir=fsm.getLocateDir(FileSysManager.INTERNAL, getResources().getInteger(R.integer.SUBTITLE_TYPE));
		
		extSpeechPathInfo.setText(((extSpeechDir != null)?extSpeechDir:getString(R.string.noExtSpace)));
		extSubtitlePathInfo.setText(((extSubtitleDir != null)?extSubtitleDir:getString(R.string.noExtSpace)));
		intSpeechPathInfo.setText(intSpeechDir);
		intSubtitlePathInfo.setText(intSubtitleDir);
		
		Log.d(getClass().getName(),"Leave onCreate");
	}
	
	private void refreshUsage(){
		new Thread(new Runnable(){
			@Override
			public void run() {
				intFreeB=fsm.getFreeMemory(FileSysManager.INTERNAL);
				extFreeB=fsm.getFreeMemory(FileSysManager.EXTERNAL);
				intTotal=fsm.getTotalMemory(FileSysManager.INTERNAL);
				extTotal=fsm.getTotalMemory(FileSysManager.EXTERNAL);
				intUsed=fsm.getAppUsed(FileSysManager.INTERNAL);
				extUsed=fsm.getAppUsed(FileSysManager.EXTERNAL);
				userSpecDir=runtime.getString(getString(R.string.userSpecifySpeechDir), null);
				
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						extFreePercent.setText(Math.round(((double)extFreeB/extTotal)*100)+"%");
						intFreePercent.setText(Math.round(((double)intFreeB/intTotal)*100)+"%");
						extAppUsagePercent.setText(Math.round(((double)extUsed/extTotal)*100)+"%");
						intAppUsagePercent.setText(Math.round(((double)intUsed/intTotal)*100)+"%");
						extFree.setText(numToKMG(extFreeB)+"B");
						intFree.setText(numToKMG(intFreeB)+"B");
						extAppUseage.setText(numToKMG(extUsed)+"B");
						intAppUseage.setText(numToKMG(intUsed)+"B");
						
						if(intFreeB>extUsed&&extUsed>0)btnMoveAllToInt.setEnabled(true);
						else btnMoveAllToInt.setEnabled(false);
						if(extFreeB>intUsed&&intUsed>0)btnMoveAllToExt.setEnabled(true);
						else btnMoveAllToExt.setEnabled(false);
						if(intUsed>0)btnDelIntFiles.setEnabled(true);
						else btnDelIntFiles.setEnabled(false);
						if(extUsed>0)btnDelExtFiles.setEnabled(true);
						else btnDelExtFiles.setEnabled(false);
						
						if(userSpecDir!=null)filePathInput.setText(userSpecDir);
				}});
				
			}}).start();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshUsage();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}
	
	@Override
	public void finish() {
		super.finish();
		AnalyticsApplication.sendEvent("storage_status", "ext_storage", "free_percent", Math.round(((double)extFreeB/extTotal)*100));
		AnalyticsApplication.sendEvent("storage_status", "ext_storage", "usage_percent", Math.round(Math.round(((double)extUsed/extTotal)*100)));
		AnalyticsApplication.sendEvent("storage_status", "ext_storage", "free_byte", extFreeB);
		AnalyticsApplication.sendEvent("storage_status", "ext_storage", "used_byte", extUsed);
		
		AnalyticsApplication.sendEvent("storage_status", "int_storage", "free_percent", Math.round(((double)intFreeB/intTotal)*100));
		AnalyticsApplication.sendEvent("storage_status", "int_storage", "usage_percent", Math.round(Math.round(((double)intUsed/intTotal)*100)));
		AnalyticsApplication.sendEvent("storage_status", "int_storage", "free_byte", intFreeB);
		AnalyticsApplication.sendEvent("storage_status", "int_storage", "used_byte", intUsed);
		
		boolean isUserSpecifyDir=runtime.getBoolean(getString(R.string.isUseThirdDir),false);
		AnalyticsApplication.sendEvent("storage_status", "user_specify_dir", "boolean", ((isUserSpecifyDir)?1:0));
		if(isUserSpecifyDir)
			AnalyticsApplication.sendEvent("storage_status", "user_specify_dir", runtime.getString(getString(R.string.userSpecifySpeechDir), null), null);
			
		else AnalyticsApplication.sendEvent("storage_status", "user_specify_dir", fsm.getSysDefMediaDir(), null);
	}
	
	public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {
		if (resultCode != Activity.RESULT_OK || data == null) return;

		switch(requestCode){
		case OPEN_DOCUMENT_TREE_UND_23:

/*			String filePath=null;
			Bundle bundle = data.getExtras();

			if(bundle != null)
			{
				if(bundle.containsKey(FileChooserActivity.OUTPUT_NEW_FILE_NAME)) {
					File folder = (File) bundle.get(FileChooserActivity.OUTPUT_FILE_OBJECT);
					String name = bundle.getString(FileChooserActivity.OUTPUT_NEW_FILE_NAME);
					filePath = folder.getAbsolutePath() + "/" + name;
					File newFolder=new File(filePath);
					if(!newFolder.mkdirs()){
						BaseDialogs.showErrorDialog(StorageManageActivity.this,"您所選擇的位置 "+filePath+" 無法建立資料夾，請重新操作。");
						break;
					}

				} else {
					File file = (File) bundle.get(FileChooserActivity.OUTPUT_FILE_OBJECT);
					if(!file.canWrite()){
						BaseDialogs.showErrorDialog(StorageManageActivity.this,"您所選擇的位置 "+filePath+" 無法寫入檔案，請重新操作。");
						break;
					}
					filePath = file.getAbsolutePath();
				}
			}
			final String mustFinalString=filePath;
			filePathInput.postDelayed(new Runnable(){
				@Override
				public void run() {
					filePathInput.setText(mustFinalString);
				}},200);
*/
			final String filePath = data.getStringExtra(FileDialogActivity.RESULT_PATH);
			filePathInput.postDelayed(new Runnable(){
				@Override
				public void run() {
					filePathInput.setText(filePath);
				}},500);
			break;
			case OPEN_DOCUMENT_TREE_ABV_23:
			final Uri treeUri = data.getData();
	        final DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
	        
	        filePathInput.postDelayed(new Runnable(){
				@Override
				public void run() {
					filePathInput.setText(pickedDir.getUri().getPath());
				}},200);
	        break;

		}
/*
		// Avoid EditText bug,  the EditText will not change to the new value without the thread.
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					Thread.sleep(500);
					Log.d(getClass().getName(),"Set path the EditText: "+filePath);
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							filePathInput.setText(filePath);
						}});
					
				} catch (InterruptedException e) {e.printStackTrace();}
			}}).start();
	*/	
    }

	@Override
	public void onRequestPermissionsResult(int requestCode,  String[] permissions,  int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == STORAGE_ACCESS_PERMISSION_REQUEST) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Permission granted.
			}
		}
	}

	private void showFileDialogActivity(){
		File f=null;
		String ind="";
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {  // Check is external storage mounted, no matter read only or read/write.
			f = Environment.getExternalStorageDirectory();
			ind="外部儲存區域";
		}
		else {
			f = getFilesDir();
			ind="內部儲存區域";
		}

		Toast.makeText(this, ind, Toast.LENGTH_LONG).show();
		Intent intent = new Intent(getBaseContext(), FileDialogActivity.class);
		intent.putExtra(FileDialogActivity.TITLE, "請選擇存放目錄");
		intent.putExtra(FileDialogActivity.START_PATH, f.getAbsolutePath());
		intent.putExtra(FileDialogActivity.CAN_SELECT_DIR, true);
		startActivityForResult(intent, OPEN_DOCUMENT_TREE_UND_23);
/*
		Intent intent = new Intent(getBaseContext(), FileChooserActivity.class);
		intent.putExtra(FileChooserActivity.INPUT_FOLDER_MODE, true);
		intent.putExtra(FileChooserActivity.INPUT_CAN_CREATE_FILES, true);
		intent.putExtra(FileChooserActivity.INPUT_START_FOLDER, Environment.getExternalStorageDirectory());
		startActivityForResult(intent, OPEN_DOCUMENT_TREE_UND_23);
		*/
	}
	
	private void showAskMoveToSpecifyDialog(final String path) {
		final AlertDialog.Builder builder=getConfirmDialog();
		builder.setTitle("移動檔案");
		builder.setMessage("您要將所有的音檔移動到您所指定的位置嗎？");
		builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final ProgressDialog pd= new ProgressDialog(StorageManageActivity.this);
				pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        	pd.setCancelable(false);
	        	pd.setTitle("檔案搬移");
	        	pd.setMessage("搬移中，請稍候...");
				Thread t=new Thread(new Runnable(){
					@Override
					public void run() {
						File destFile=new File(path);
						if(!fsm.moveAllMediaFileToUserSpecifyDir(destFile, pd) || !fsm.moveAllMediaFileToUserSpecifyDir(destFile, pd))
							Util.showErrorPopupWindow(StorageManageActivity.this, findViewById(R.id.smRootView), "檔案搬移失敗，請確認目的地空間是否足夠。");
						if(pd.isShowing())pd.dismiss();
						refreshUsage();
					}});
				t.start();
				
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						pd.show();
					}});
				
			}});
		builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(final DialogInterface dialog, int which) {
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						dialog.cancel();
					}});
			}});
		
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				builder.create().show();
			}});
	}

	private String numToKMG(long num){
		Log.d(getClass().getName(),"Cac: "+num);
		String[] unit={"","K","M","G","T"};
		String s=""+num;
		int len=s.length();

		int sign=(int) (len/3);
		if(sign*3==len)sign--;

		int index=sign*3;
		String result=s.substring(0, s.length()-index)+'.'+s.charAt(index)+unit[sign];
		Log.d(getClass().getName(),"Num= "+s+", Length: "+s.length()+", result="+result);
		return result;
	}


	private AlertDialog.Builder getConfirmDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
//	        	if(wakeLock.isHeld())wakeLock.release();
	            dialog.cancel();
	        }
	    });
		return builder;
	}
	
	

}
