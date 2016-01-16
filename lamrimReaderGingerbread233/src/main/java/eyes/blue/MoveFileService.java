package eyes.blue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


public class MoveFileService extends IntentService {
	static final String logTag="MoveFileService";
	public static final String NOTIFICATION = "eyes.blue.action.MoveFileService";
	SharedPreferences runtime = null;
	FileSysManager fsm=new FileSysManager(MoveFileService.this);
	PowerManager powerManager=null;
	WakeLock wakeLock = null;
	public static int notificationId=1;	// Always update notification but create new one.
	
	
	public MoveFileService() {
		super("MoveFileService");
	}
	public MoveFileService(String name) {
		super(name);
	}
	
	@Override
    public void onDestroy() {
		if(wakeLock.isHeld())wakeLock.release();
		removeNotification();
    }

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(getClass().getName(), "Into onHandleIntent of Move File service");
		
		powerManager=(PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
		
		
		ArrayList<String> srcDirs=intent.getStringArrayListExtra("srcDirs");
		String destDir=intent.getStringExtra("destDir");
		File destFile=new File(destDir);
		if(!wakeLock.isHeld()){wakeLock.acquire();}
//		moveAllMediaFileToUserSpecifyDir(new File(destDir));
		for(String src:srcDirs){
			File srcFile=new File(src);
			if(!moveContentsOfDir(srcFile, destFile))
				break;
		}
		if(wakeLock.isHeld())wakeLock.release();
		
		GaLogger.sendEvent("statistics", "MOVE_FILE_TO_SPECIFY_FOLDER", "FINISH", 1);
		Log.d(getClass().getName(),"Move File Service terminate.");
	}

/*	public boolean moveAllMediaFileToUserSpecifyDir(File destDir){
    	File intDir=new File(fsm.getSrcRootPath(FileSysManager.INTERNAL)+File.separator+getString(R.string.audioDirName));
    	if(!moveContentsOfDir(intDir,destDir))return false;
    	if(fsm.getSrcRootPath(FileSysManager.EXTERNAL) != null){
    		File extDir=new File(fsm.getSrcRootPath(FileSysManager.EXTERNAL)+File.separator+getString(R.string.audioDirName));
    		if(!moveContentsOfDir(extDir,destDir))return false;
    	}
    	
    	return true;
    }
 */   
    private boolean moveContentsOfDir(File srcDir, File destDir){
    	// if source folder equals destination folder, return true directly.
    	if(srcDir.equals(destDir))return true;
    	
    	final File[] files=srcDir.listFiles();
    	Log.d(logTag,"There are "+files.length+" files wait for move.");

    	// Check is the destination has the same file, delete source one.
		for(File src: files){
			File dist=new File(destDir.getAbsolutePath()+File.separator+src.getName());
			if(dist.exists()){
				if(src.length()==dist.length()){
					src.delete();
					continue;
				}
			}

			/* Copy To */
			notifyMsg("移動檔案", "移動"+src.getName()+" 到 "+dist.getAbsolutePath());
			if(!src.renameTo(dist))
				if(!moveFile(src, dist))
					return false;
		}
		return true;
    }
    
    private boolean moveFile(File from, File to){
    	File distTemp=new File(to.getAbsolutePath()+getString(R.string.downloadTmpPostfix));
		FileInputStream fis = null;
		FileOutputStream fos = null;
		Log.d(logTag,"Copy "+from.getAbsolutePath()+" to "+to.getAbsolutePath());
		try {
			fis = new FileInputStream(from);
			fos =new FileOutputStream(distTemp);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		
		byte[] buf=new byte[getResources().getInteger(R.integer.downloadBufferSize)];
		int readLen=0;
		
		try {
			while((readLen=fis.read(buf))!=-1)
				fos.write(buf, 0, readLen);
			
			fis.close();
			fos.flush();
			fos.close();
			
			to.delete();
			distTemp.renameTo(to);
			from.delete();
		} catch (IOException e) {
			reportStorageUnusable();
			distTemp.delete();
			e.printStackTrace();
			if(wakeLock.isHeld())wakeLock.release();
			return false;
		}
		return true;
    }
	
	private void reportStorageUnusable(){
		Intent bcIntent = new Intent();
		bcIntent.setAction(NOTIFICATION);
		bcIntent.putExtra("action", "error");
		bcIntent.putExtra("desc", "無法使用儲存裝置或儲存空間不足，請檢查您的儲存裝置是否正常，或磁碟已被電腦連線所獨佔！");
		sendBroadcast(bcIntent); 
		
		removeNotification();
	}
	
	private void notifyMsg(String title, String contentText) {
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
		        .setContentTitle(title)
		        .setContentText(contentText);
		
/*		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, ResultActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(ResultActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);
		*/
		NotificationManager mNotificationManager =
		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(notificationId, mBuilder.build());
	  }
	
	private void removeNotification(){
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(notificationId);
	}
}
