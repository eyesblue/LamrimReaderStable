package eyes.blue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;


public class MoveFileService extends IntentService {
	final String logTag=getClass().getName();
	public static final String NOTIFICATION = "eyes.blue.action.MoveFileService";
	SharedPreferences runtime = null;
	FileSysManager fsm=new FileSysManager(MoveFileService.this);
	PowerManager powerManager=null;
	WakeLock wakeLock = null;
	public static int notificationId=1;	// Always update notification but create new one.
	private FirebaseAnalytics mFirebaseAnalytics;
	
	public MoveFileService() {
		super("MoveFileService");
	}
	public MoveFileService(String name) {
		super(name);
	}
	
	@Override
    public void onDestroy() {
		if(wakeLock.isHeld())wakeLock.release();
	//	removeNotification();
    }

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(getClass().getName(), "Into onHandleIntent of Move File service");
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
		
		powerManager=(PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
		
		
		ArrayList<String> srcDirs=intent.getStringArrayListExtra("srcDirs");
		String destDir=intent.getStringExtra("destDir");
		File destFile=new File(destDir);
		if(!wakeLock.isHeld()){wakeLock.acquire();}
//		moveAllMediaFileToUserSpecifyDir(new File(destDir));
		int counter=0, res=-1;
		long startTime=System.currentTimeMillis();
		for(String src:srcDirs){
			File srcFile=new File(src);
			if((res=moveContentsOfDir(srcFile, destFile))==-1)
				return;
			else counter+=res;
		}
		if(wakeLock.isHeld())wakeLock.release();

		int time=(int)(System.currentTimeMillis()-startTime);
		float spend=(float)time/(float)1000;
		String readAble=String.format(Locale.ENGLISH, "%.3f%n",spend);
		AnalyticsApplication.sendEvent(logTag, "MOVE_FILE_TO_SPECIFY_FOLDER", "FINISH");
		Util.fireSelectEvent(mFirebaseAnalytics, logTag, Util.STATISTICS, "MOVE_FILE_TO_SPECIFY_FOLDER_FINISH");
		notifyMsg("檔案搬移完成","共搬移 "+counter+" 個檔案，耗時 "+readAble+" 秒。");
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
    private int moveContentsOfDir(File srcDir, File destDir){
    	// if source folder equals destination folder, return true directly.
    	if(srcDir.equals(destDir))return 0;

		int counter=0;
    	final File[] files=srcDir.listFiles();
    	Log.d(logTag,"There are "+files.length+" files wait for move.");

    	// Check is the destination has the same file, delete source one.
		for(File src: files){
			File dist=new File(destDir.getAbsolutePath()+File.separator+src.getName());
			if(dist.exists()){
				if(src.length()==dist.length()){
					src.delete();
					counter++;
					continue;
				}
			}

			/* Copy To */
			notifyMsg("移動檔案", "移動"+src.getName()+" 到 "+dist.getAbsolutePath());
			if(src.renameTo(dist)){
				counter++;
				continue;
			}else if(moveFile(src, dist)){
				counter++;
				continue;
			}
			else return -1;
		}
		return counter;
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
		
		//removeNotification();
		notifyMsg("檔案搬移失敗","無法使用儲存裝置或儲存空間不足，請檢查您的儲存裝置是否正常，或磁碟已被電腦連線所獨佔！");
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
