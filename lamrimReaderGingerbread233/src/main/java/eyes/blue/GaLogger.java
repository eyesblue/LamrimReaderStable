package eyes.blue;

import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.ExceptionParser;
import com.google.analytics.tracking.android.ExceptionReporter;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GAServiceManager;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;

import android.app.Activity;
import android.os.Build;

public class GaLogger{
	static EasyTracker easyTracker=null;
	static Activity activity=null;
	
	public static void activityStart(Activity activity_){
		easyTracker=EasyTracker.getInstance(activity_);
		activity=activity_;
		
		Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
	    if (uncaughtExceptionHandler instanceof ExceptionReporter) {
	      ExceptionReporter exceptionReporter = (ExceptionReporter) uncaughtExceptionHandler;
	      exceptionReporter.setExceptionParser(new ExceptionParser(){

			@Override
			public String getDescription(String threadId, Throwable throwable) {
				return "UNCHATCHED: V"+android.os.Build.VERSION.RELEASE+", "+Util.getDeviceName()+", "+"Thread: {" + threadId + "}, Exception: " + ExceptionUtils.getStackTrace(throwable);
			}});
	    }
	    
	    easyTracker.set(Fields.SCREEN_NAME, activity_.getClass().getName());
	    easyTracker.send(MapBuilder.createAppView().build());
	}
	
	public static void activityStop(Activity actvity){EasyTracker.getInstance(actvity).activityStop(actvity);}
	
	public static void sendEvent(String category, String action, String label, Long value){
		if(easyTracker==null)return;
		easyTracker.send(MapBuilder
			      .createEvent(category,     // Event category (required)
			    		  action,  // Event action (required)
			    		  label,   // Event label
			    		  value)            // Event value
			      .build()
			  );
	}
	
	public static void sendEvent(String category, String action, String label, int value){
		if(easyTracker==null)return;
		easyTracker.send(MapBuilder
			      .createEvent(category,     // Event category (required)
			    		  action,  // Event action (required)
			    		  label,   // Event label
			    		  (long)value)            // Event value
			      .build()
			  );
	}
	
	public static void send(Map<String, String> builder){
		if(easyTracker==null)return;
		easyTracker.send(builder);
	}
	
	public static void sendException(Throwable ta,boolean isFatal){sendException(null, ta, isFatal);}
	public static void sendException(String msg, Throwable ta,boolean isFatal){
		String s="CATCHED: V"+android.os.Build.VERSION.RELEASE+", ";
		s+=Util.getDeviceName()+", ";
		if(msg!=null)s+=msg+": ";
		s+=ExceptionUtils.getStackTrace(ta);
		s+=" {"+Thread.currentThread().getName()+"}";
		easyTracker.send(MapBuilder.createException(s,isFatal).build());
	}
	
	
}
