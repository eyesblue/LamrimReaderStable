package eyes.blue;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.google.android.gms.analytics.ExceptionParser;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;



public class AnalyticsApplication extends Application{
	//static EasyTracker easyTracker=null;
	//static Activity activity=null;
	//static private Tracker easyTracker;//mTracker;
	static private Tracker mTracker=null;

		/**
		 * Gets the default {@link Tracker} for this {@link Application}.
		 * @return tracker
		 */
		synchronized public Tracker getDefaultTracker() {
			if (mTracker == null) {
				GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
				// To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
				mTracker = analytics.newTracker(R.xml.analytics);
				mTracker.enableAdvertisingIdCollection(true);
				mTracker.enableExceptionReporting(true);
			}

			Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
			if (uncaughtExceptionHandler instanceof ExceptionReporter) {
				ExceptionReporter exceptionReporter = (ExceptionReporter) uncaughtExceptionHandler;
				exceptionReporter.setExceptionParser(new ExceptionParser(){

					@Override
					public String getDescription(String threadId, Throwable throwable) {
						return "UNCHATCHED: V"+android.os.Build.VERSION.RELEASE+", "+Util.getDeviceName()+", "+"Thread: {" + threadId + "}, Exception: " + ExceptionUtils.getStackTrace(throwable);
					}});
			}
			Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

			return mTracker;
		}


/*
	synchronized public static void activityStart(Activity activity_){


		activity=activity_;

		if (easyTracker == null) {
			GoogleAnalytics ga=GoogleAnalytics.getInstance(activity_);
			//easyTracker = GoogleAnalytics.newTracker(R.xml.ga_trackingId);
			easyTracker = ga.newTracker(activity_.getString(R.string.ga_trackingId));
			easyTracker.enableAdvertisingIdCollection(true);
			ga.reportActivityStart(activity_);

		}

		Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
	    if (uncaughtExceptionHandler instanceof ExceptionReporter) {
	      ExceptionReporter exceptionReporter = (ExceptionReporter) uncaughtExceptionHandler;
	      exceptionReporter.setExceptionParser(new ExceptionParser(){

			@Override
			public String getDescription(String threadId, Throwable throwable) {
				return "UNCHATCHED: V"+android.os.Build.VERSION.RELEASE+", "+Util.getDeviceName()+", "+"Thread: {" + threadId + "}, Exception: " + ExceptionUtils.getStackTrace(throwable);
			}});
	    }

	    //easyTracker.set(Fields.SCREEN_NAME, activity_.getClass().getName());
	    //easyTracker.send(MapBuilder.createAppView().build());
		// Set screen name.

		easyTracker.setScreenName(activity_.getClass().getName());

		// Send a screen view.
		easyTracker.send(new HitBuilders.ScreenViewBuilder().build());
	}
	*/

	//public static void activityStop(Activity actvity_){GoogleAnalytics.getInstance(actvity_).reportActivityStop(actvity_);}

	public static void sendEvent(String category, String action, String label){
		sendEvent(category,  action,  label,null);
	}
	public static void sendEvent(String category, String action, String label, Long value){
		if(mTracker==null){
			Log.d("LamrimReader","The TRACKER of GOOGLE ANALYTICS is NULL !!!!");
			return;
		}

		if(value!=null)
			mTracker.send(new HitBuilders.EventBuilder()
				.setCategory(category)
				.setAction(action)
				.setLabel(label)
				.setValue(value)
				.build());
		else
			mTracker.send(new HitBuilders.EventBuilder()
				.setCategory(category)
				.setAction(action)
				.setLabel(label)
				.build());

		/*easyTracker.send(MapBuilder
						.createEvent(category,     // Event category (required)
								action,  // Event action (required)
								label,   // Event label
								value)            // Event value
						.build()
		);
*/
	}


	public static void sendEvent(String category, String action, String label, int value){
		if(mTracker==null)return;

		mTracker.send(new HitBuilders.EventBuilder()
				.setCategory(category)
				.setAction(action)
				.setLabel(label)
				.setValue(value)
				.build());
	}


	public static void sendException(Throwable ta,boolean isFatal){sendException(null, ta, isFatal);}

	public static void sendException(String msg, Throwable ta,boolean isFatal){
		if(mTracker==null)return;

		String s="CATCHED: GA_V4, V"+android.os.Build.VERSION.RELEASE+", ";
		s+=Util.getDeviceName()+", ";
		if(msg!=null)s+=msg+": ";
		s+=ExceptionUtils.getStackTrace(ta);
		s+=" {"+Thread.currentThread().getName()+"}";

		mTracker.send(new HitBuilders.ExceptionBuilder()
				.setDescription(s)
				.setFatal(false)
				.build());
	}

	public static void sendTimming(String category, long value,String variable, String label){
		if(mTracker==null)return;
		String labelStr=(label==null)?"":label;

		// Build and send timing.
		mTracker.send(new HitBuilders.TimingBuilder()
				.setCategory(category)
				.setValue(value)
				.setVariable(variable)
				.setLabel(labelStr)
				.build());
	}

}
