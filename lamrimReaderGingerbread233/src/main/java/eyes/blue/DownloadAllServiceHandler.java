package eyes.blue;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class DownloadAllServiceHandler extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// try{
		super.onCreate(savedInstanceState);
///		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.download_all_service_handler);
		
		Button yesBtn=(Button) findViewById(R.id.yesBtn);
		Button noBtn=(Button) findViewById(R.id.noBtn);
		
		yesBtn.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// Check is the service already terminate.
				boolean isAlive=isMyServiceRunning(DownloadAllService.class);
				if(!isAlive){
					finish();
				}
				
				Intent intent = new Intent(DownloadAllServiceHandler.this, DownloadAllService.class);
				intent.putExtra("cmd", "stop");
				Log.d(getClass().getName(),"Stop download all service.");
		        stopService(intent);
		        removeNotification();
		        finish();
			}
		});
		
		noBtn.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View arg0) {
		        finish();
			}
		});
	}
	
	private boolean isMyServiceRunning(Class<?> serviceClass) {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceClass.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	private void removeNotification(){
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(DownloadAllService.notificationId);
	}
}
