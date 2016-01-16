package eyes.blue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.analytics.tracking.android.MapBuilder;

import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;


public class DownloadAllService extends IntentService {

	public static final String NOTIFICATION = "eyes.blue.action.DownloadAllService";
	SharedPreferences runtime = null;
	int defaultThreads = 4, downloadTimes=0,retryTimes=3;
	Downloader threadPool[] = null;
	String notifyMsg[]=null;
	Integer downloadIndex = 0;
	Integer successCount=0;
	Integer failureCount=0;
	FileSysManager fsm= null;
	boolean cancelled=false, hasFailure=false;
	PowerManager powerManager=null;
	WakeLock wakeLock = null;
	public static int notificationId=0;	// Always update notification but create new one.
	
	
	public DownloadAllService() {
		super("DownloadAllService");
	}
	public DownloadAllService(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	
	@Override
    public void onDestroy() {
		Log.d(getClass().getName(), "Stop download all service");
		cancelled=true;
		removeNotification();
    }

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(getClass().getName(), "Into onHandleIntent of download all service");
		
		fsm=new FileSysManager(getBaseContext());
		defaultThreads=intent.getIntExtra("threadCount", 4);
		if(defaultThreads<1){
			Log.d(getClass().getName(), "DownloadAllService receive uncurrect thread count "+defaultThreads+", skip service.");
			return;
		}
		threadPool = new Downloader[defaultThreads];
		notifyMsg=new String[defaultThreads];
		powerManager=(PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
		
		for(int i=0;i<defaultThreads;i++){
			notifyMsg[i]="啟動中";
		}
		
		boolean isAlive=false;
		for(int i=0;i<defaultThreads;i++){
			if(threadPool[i] != null && threadPool[i].isAlive())
				isAlive=true;
		}
		
		if(isAlive){
			Log.d(getClass().getName(), "Task downloading, skip the start command.");
			return;
		}
		
		runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);
		
/*		if(downloader!=null && downloader.getStatus()==AsyncTask.Status.RUNNING){
			Log.d(getClass().getName(), "Task downloading, skip the start command.");
			return;
		}
*/		
		if(!wakeLock.isHeld()){wakeLock.acquire();}
		Log.d(getClass().getName(), "Start download all service");
		for(int i=0;i<defaultThreads;i++){
			threadPool[i]=new Downloader(i);
			threadPool[i].start();
		}
		
		// Send a broadcast to notify receiver that service has start
		reportServiceStart();
		
		while (true) {
			Log.d(getClass().getName(),	"Main thread of service wake up, download index=" + downloadIndex);
			boolean alive = false;
			synchronized (threadPool) {
				for (int i = 0; i < defaultThreads; i++)
					if (threadPool[i].isAlive())
						alive = true;

				if (!alive) { // In to the scope while worker threads terminate.
					if (hasFailure) { // Some download task failure.
						downloadIndex = 0;
						hasFailure = false;
						if(++downloadTimes>=retryTimes){
							if(wakeLock.isHeld())wakeLock.release();
							reportDownloadAllTerminate();
							break;
						}
						for (int j = 0; j < defaultThreads; j++) {
							threadPool[j] = new Downloader(j);
							threadPool[j].start();
						}
					} else { // Download all finish and no failure.
						reportDownloadAllTerminate();
						if(wakeLock.isHeld())wakeLock.release();
						break;
					}
				}

/*				if (cancelled) {
					reportDownloadAllTerminate();
					removeNotification();
					break;
				}
				*/
				try {
					threadPool.wait(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} // synchroinzed
		} // while
		Log.d(getClass().getName(),"Download All Service terminate.");
	}
	

	private void reportServiceStart(){
		Intent bcIntent = new Intent();
		bcIntent.setAction(NOTIFICATION);
		bcIntent.putExtra("action", "start");
		sendBroadcast(bcIntent); 
		
		notifyMsg("LamrimReader Downloader All Service", "Service started.");
	}
	
	private void reportDownloadAllTerminate(){
		Intent bcIntent = new Intent();
		bcIntent.setAction(NOTIFICATION);
		bcIntent.putExtra("action", "terminate");
//		bcIntent.putExtra("cause", "finish");
		sendBroadcast(bcIntent); 
		
		removeNotification();
	}
	
	private void reportStorageUnusable(){
		Intent bcIntent = new Intent();
		bcIntent.setAction(NOTIFICATION);
		bcIntent.putExtra("action", "error");
		bcIntent.putExtra("desc", "儲存空間不足或無法使用儲存裝置，請檢查您的儲存裝置是否正常，或磁碟已被電腦連線所獨佔！");
		sendBroadcast(bcIntent); 
		
		removeNotification();
	}
	
/*	private void publishResults(int index) {
	    Intent intent = new Intent(NOTIFICATION);
	    intent.putExtra("action", "download");
	    intent.putExtra("index", index);
	    
	    sendBroadcast(intent);
	}
*/	
	/*
	 * This function just need for notification, but not SpeechMenuActivity.
	 * */
	private synchronized void reportStartDownloadIndex(int threadId, int downloadIndex) {
		notifyMsg[threadId]="\t"+SpeechData.getSubtitleName(downloadIndex);
		String msg="下載列表：";
		
		for(int i=0;i<defaultThreads;i++){
			msg+=notifyMsg[i];
		}
		notifyMsg("廣論App資源下載", msg);
	}
	
	/*
	 * Download state need for SpeeechMenuActivity, but not notification bar. 
	 * */
	private void reportDownloadState(int threadId, int downloadIndex, boolean isSuccess) {
		Intent bcIntent = new Intent();
		bcIntent.setAction(NOTIFICATION);
		bcIntent.putExtra("action", "download");
		bcIntent.putExtra("index", downloadIndex);
		bcIntent.putExtra("isSuccess", isSuccess);
		sendBroadcast(bcIntent);
	}
	
	private void notifyMsg(String title, String contentText) {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentTitle(title)
		        .setContentText(contentText);
		// Creates an explicit intent for an Activity in your app
		ComponentName c=new ComponentName(DownloadAllService.this, DownloadAllServiceHandler.class);
		Intent intent = new Intent();
		intent.setComponent(c);
		
		// Sets the Activity to start in a new, empty task
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		// Creates the PendingIntent
		PendingIntent notifyIntent =
		        PendingIntent.getActivity(
		        this,
		        0,
		        intent,
		        PendingIntent.FLAG_UPDATE_CURRENT
		);


		// Puts the PendingIntent into the notification builder
		builder.setContentIntent(notifyIntent);
		// Notifications are issued by sending them to the
		// NotificationManager system service.
		NotificationManager mNotificationManager =
		    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		// mId allows you to update the notification later on.
		mNotificationManager.notify(notificationId, builder.build());
	  }
	
	private void removeNotification(){
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(notificationId);
	}

	public class Downloader extends Thread{
		int tId = -1;
//		boolean cancelled=false;
		
		public Downloader(int id){
			this.tId=id;
		}
		
/*		public void stopRun(){
			this.cancelled=true;
		}
*/		
		
		@Override
		public void run(){
			try{
				runTask();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		private void runTask(){
			
	    	String locale = DownloadAllService.this.getResources().getConfiguration().locale.getCountry();
	    	RemoteSource rs = null;
	    	
	    	if(locale.equals("zh_CN")){
		    	// If there exist the source download site in China.
	    	}
	    	else {rs = new GoogleRemoteSource(DownloadAllService.this);}
	    	
	    	int downloadingIndex=-1;
	    	while(downloadIndex<SpeechData.name.length){
	    		synchronized(threadPool){
	    			if(downloadIndex>=SpeechData.name.length){
	    				Log.d("DownloadAllThread","Thread"+tId+" Terminate, End of media index reached.");
	    				return ;
	    			}
	    		
	    			downloadingIndex=downloadIndex;
	    			downloadIndex++;
	    			threadPool.notifyAll();
	    		}
	    		Log.d("DownloadAllThread","Thread"+tId+" get download task index "+downloadIndex);
	    		
	    		
	    		if(cancelled){
	    			Log.d(getClass().getName(),"Thread_"+tId+" Terminate, Task has canceled.");
	    			return;
	    		}
	    		
	    		boolean mediaExist=false, subtitleExist=false;
				File subtitleFile=fsm.getLocalSubtitleFile(downloadingIndex);
				File mediaFile=fsm.getLocalMediaFile(downloadingIndex);
				try{
					subtitleExist=subtitleFile.exists();
					mediaExist=mediaFile.exists();
				}catch(NullPointerException npe){
					Log.d(getClass().getName(),"The storage media has not usable, skip.");
					reportStorageUnusable();
					GaLogger.sendException("There is no storage usable.", npe, true);
					return;
				}
				if(!subtitleExist){
					Log.d("DownloadAllService","The subtitle not exist, download to "+subtitleFile.getAbsolutePath());
					subtitleExist=download(rs.getSubtitleFileAddress(downloadingIndex),subtitleFile.getAbsolutePath());
					reportStartDownloadIndex(tId, downloadingIndex);
				}
				
				if(!mediaExist){
					mediaExist=download(rs.getMediaFileAddress(downloadingIndex),mediaFile.getAbsolutePath());
					reportStartDownloadIndex(tId, downloadingIndex);
				}
				if(!subtitleExist || !mediaExist)hasFailure=true;
				reportDownloadState(notificationId, downloadingIndex, (subtitleExist && mediaExist));
			}
			return ;
		}
		
		public boolean download(String url, String outputPath){
	        Log.d(getClass().getName(),"Download file from "+url);
	        File tmpFile=new File(outputPath+getString(R.string.downloadTmpPostfix));
	        long startTime=System.currentTimeMillis(), respWaitStartTime;

	        int readLen=-1, counter=0, bufLen=getResources().getInteger(R.integer.downloadBufferSize);
	        FileOutputStream fos=null;

	        //HttpClient httpclient = getNewHttpClient();
	        HttpClient httpclient = new DefaultHttpClient();
	        HttpGet httpget = new HttpGet(url);
	        HttpResponse response=null;
	        int respCode=-1;
	        if(cancelled){Log.d(getClass().getName(),"User canceled, download procedure skip!");return false;}
	       
//	        setProgressMsg(activity.getString(R.string.dlgTitleConnecting),String.format(activity.getString(R.string.dlgDescConnecting), SpeechData.getNameId(mediaIndex),(type == activity.getResources().getInteger(R.integer.MEDIA_TYPE))?"音檔":"字幕"));
	       
	        try {
	        	respWaitStartTime=System.currentTimeMillis();
	        	response = httpclient.execute(httpget);
	        	respCode=response.getStatusLine().getStatusCode();

	        	// For debug
	        	if(respCode!=HttpStatus.SC_OK){
	        		httpclient.getConnectionManager().shutdown();
	        		System.out.println("CheckRemoteThread: Return code not equal 200! check return "+respCode);
	        	}
	        }catch (ClientProtocolException e) {
	        	httpclient.getConnectionManager().shutdown();
	        	e.printStackTrace();
	        	return false;
	        }catch (Exception e) {
	        	httpclient.getConnectionManager().shutdown();
	        	e.printStackTrace();
	        	return false;
	        }

	        if(cancelled){
	        	httpclient.getConnectionManager().shutdown();
	        	Log.d(getClass().getName(),"User canceled, download procedure skip!");
	        	return false;
	        }
	        GaLogger.send(MapBuilder
	              .createTiming("download",    // Timing category (required)
	                          System.currentTimeMillis()-respWaitStartTime,       // Timing interval in milliseconds (required)
	                    "wait resp time",  // Timing name
	                    null)           // Timing label
	      .build());

	        HttpEntity httpEntity=response.getEntity();
	        InputStream is=null;
	        try {
	                is = httpEntity.getContent();
	        } catch (IllegalStateException e2) {
	                try {   is.close();     } catch (IOException e) {e.printStackTrace();}
	                httpclient.getConnectionManager().shutdown();
	                tmpFile.delete();
	                e2.printStackTrace();
	                return false;
	        } catch (IOException e2) {
	                httpclient.getConnectionManager().shutdown();
	                tmpFile.delete();
	                e2.printStackTrace();
	                return false;
	        }
	       
	        if(cancelled){
	                Log.d(getClass().getName(),"User canceled, download procedure skip!");
	                try {   is.close();     } catch (IOException e) {e.printStackTrace();}
	                httpclient.getConnectionManager().shutdown();
	                tmpFile.delete();
	                return false;
	        }
	       
	        final long contentLength=httpEntity.getContentLength();

	        try {
	                fos=new FileOutputStream(tmpFile);
	        } catch (FileNotFoundException e1) {
	                Log.d(getClass().getName(),"File Not Found Exception happen while create output temp file ["+tmpFile.getName()+"] !");
	                httpclient.getConnectionManager().shutdown();
	                try {   is.close();     } catch (IOException e) {e.printStackTrace();}
	                tmpFile.delete();
	                e1.printStackTrace();
	                return false;
	        }

	        if(cancelled){
	        	httpclient.getConnectionManager().shutdown();
	        	try {   is.close();     } catch (IOException e) {e.printStackTrace();}
	        	try {   fos.close();    } catch (IOException e) {e.printStackTrace();}
	        	tmpFile.delete();
	        	Log.d(getClass().getName(),"User canceled, download procedure skip!");
	        	return false;
	        }

	        try {
		
	        	byte[] buf=new byte[bufLen];
	        	Log.d(getClass().getName(),Thread.currentThread().getName()+": Start read stream from remote site, is="+((is==null)?"NULL":"exist")+", buf="+((buf==null)?"NULL":"exist"));
	        	while((readLen=is.read(buf))!=-1){
	        		counter+=readLen;
	        		fos.write(buf,0,readLen);

	                if(cancelled){
	                	httpclient.getConnectionManager().shutdown();
	                	try {   is.close();     } catch (IOException e) {e.printStackTrace();}
	                	try {   fos.close();    } catch (IOException e) {e.printStackTrace();}
	                	tmpFile.delete();
	                	Log.d(getClass().getName(),"User canceled, download procedure skip!");
	                	return false;
	                }
	        	}
			is.close();
			fos.flush();
			fos.close();
	        } catch (IOException e) {
	        	httpclient.getConnectionManager().shutdown();
	        	try {   is.close();     } catch (IOException e2) {e2.printStackTrace();}
	        	try {   fos.close();    } catch (IOException e2) {e2.printStackTrace();}
	        	tmpFile.delete();
	        	e.printStackTrace();
	        	Log.d(getClass().getName(),Thread.currentThread().getName()+": IOException happen while download media.");
	        	return false;
	        }

	        if(counter!=contentLength || cancelled){
	        	httpclient.getConnectionManager().shutdown();
	        	tmpFile.delete();
	        	return false;
	        }

	        int spend=(int) (System.currentTimeMillis()-startTime);
	        GaLogger.send(MapBuilder
	              .createTiming("download",    // Timing category (required)
	                          (long)spend,       // Timing interval in milliseconds (required)
	                          "download time",  // Timing name
	                          null)           // Timing label
	                          .build());

	        // rename the protected file name to correct file name
	        tmpFile.renameTo(new File(outputPath));
	        httpclient.getConnectionManager().shutdown();
	        Log.d(getClass().getName(),Thread.currentThread().getName()+": Download finish, return true.");
	        return true;
		}
	}
}
