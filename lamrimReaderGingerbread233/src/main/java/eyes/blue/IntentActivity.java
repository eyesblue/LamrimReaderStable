package eyes.blue;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitationResult;
import com.google.android.gms.appinvite.AppInviteReferral;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.List;


public class IntentActivity  extends FragmentActivity {

	TextView msgView;
	Button actBtn;
	private GoogleApiClient mGoogleApiClient;
	private FirebaseAnalytics mFirebaseAnalytics;
	String logTag=getClass().getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.intent_activity);
		// ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				//.enableAutoManage(this, this)
				.enableAutoManage(this, null)
				.addApi(AppInvite.API)
				.addApi(AppIndex.API).build();
/*
		boolean autoLaunchDeepLink = false;
		AppInvite.AppInviteApi.getInvitation(mGoogleApiClient, this, autoLaunchDeepLink)
				.setResultCallback(
						new ResultCallback<AppInviteInvitationResult>() {
							@Override
							public void onResult(@NonNull AppInviteInvitationResult result) {
								if (result.getStatus().isSuccess()) {
									// Extract deep link from Intent
									Intent intent = result.getInvitationIntent();
									String deepLink = AppInviteReferral.getDeepLink(intent);
									Log.d(logTag,"Deep Link: "+deepLink);

									Uri path = Uri.parse(deepLink);
									parseRest(path);
									// Handle the deep link. For example, open the linked
									// content, or apply promotional credit to the user's
									// account.

									// ...
								} else {
									Log.d(logTag, "getInvitation: no deep link found.");
								}
							}
						});
*/
		msgView=(TextView)findViewById(R.id.msgView);
		actBtn=(Button)findViewById(R.id.actBtn);
		actBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Util.fireException("Failure parse parameter of Deep Link: "+msgView.getText().toString()+": "+getIntent().getData(), new Exception());
				finish();
			}
		});

		Intent intent = this.getIntent();
		Log.d(getClass().getName(), "Action: " + intent.getAction() + ", Categories: " + intent.getCategories() + ", Scheme: " + intent.getScheme() + ", Mime type: " + intent.getType() + ", Data: " + intent.getData());
		Uri intentPathUri = intent.getData();
		Log.d(getClass().getName(), "Check intent.");
		Log.d(getClass().getName(), "Intent data: " + intentPathUri);
		Log.d(getClass().getName(), "Scheme: " + intentPathUri.getScheme());
		Log.d(getClass().getName(), "EncodedFragment: " + intentPathUri.getEncodedFragment());
		Log.d(getClass().getName(), "EncodedPath: " + intentPathUri.getEncodedPath());
		Log.d(getClass().getName(), "EncodedQuery: " + intentPathUri.getEncodedQuery());
		Log.d(getClass().getName(), "EncodedSchemeSpecificPart: " + intentPathUri.getEncodedSchemeSpecificPart());
		Log.d(getClass().getName(), "Host: " + intentPathUri.getHost());
		Log.d(getClass().getName(), "LastPathSegment: " + intentPathUri.getLastPathSegment());
		Log.d(getClass().getName(), "Path: " + intentPathUri.getPath());

		if(intentPathUri.getHost().equalsIgnoreCase(getString(R.string.firebase_deeplink_host))) {
			String link = intentPathUri.getQueryParameter("link");
			Log.d(logTag,"Firebase link: "+link);
			parseParam(Uri.parse(link));
		}
		else if(intentPathUri.getEncodedQuery() == null) parseRest(intentPathUri);
		else parseParam(intentPathUri);
	}


	private void parseParam(Uri intentPathUri){
		List<String> list = intentPathUri.getPathSegments();
		// We just support play command now.
		if (list.size()==0 || !list.get(0).equalsIgnoreCase("play")) {paramArgError(-1, "路徑[play]不存在: "+intentPathUri.getScheme()+":"+intentPathUri.getEncodedSchemeSpecificPart());return;}
		// We just support region play now.
		String mode = intentPathUri.getQueryParameter("mode");
		if (mode == null || !mode.equals("region")) {paramArgError(0, list.get(0));return;}	// We just support region play mode now.

		int speechStart[] = getSpeechData(intentPathUri.getQueryParameter("speechStart"));
		if (speechStart == null) {paramArgError(1, intentPathUri.getQueryParameter("speechStart"));return;}	// We just support region play mode now.
		int speechEnd[] = getSpeechData(intentPathUri.getQueryParameter("speechEnd"));
		if (speechEnd == null) {paramArgError(2, intentPathUri.getQueryParameter("speechEnd"));return;}
		int[] theoryStart = getTheoryData(intentPathUri.getQueryParameter("theoryStart"));
		if (theoryStart == null) {paramArgError(3, intentPathUri.getQueryParameter("theoryStart"));return;}
		int[] theoryEnd = getTheoryData(intentPathUri.getQueryParameter("theoryEnd"));
		if (theoryEnd == null) {paramArgError(4, intentPathUri.getQueryParameter("theoryEnd"));return;}

		String title=intentPathUri.getQueryParameter("title");
		if(title==null)title="";
		else title=Uri.decode(intentPathUri.getQueryParameter("title"));

		Log.d(getClass().getName(), "Parse result: mediaStart=" + speechStart[0] + ", startTimeMs=" + speechStart[1] + ", mediaEnd=" + speechEnd[0] + ", theoryStartPage=" + theoryStart[0] + ", theoryStartLine=" + theoryStart[1]
				+ ", theoryEndPage=" + theoryEnd[0] + ", theoryEndLine=" + theoryEnd[1] + ", title=" + Uri.decode(intentPathUri.getQueryParameter("title")));
		startMainActivity(speechStart, speechEnd, theoryStart, theoryEnd, title);
	}

	private void paramArgError(int i, String arg){
		String msg;
		if(i==-1)msg="命令錯誤："+arg;
		else msg="參數"+(i+1)+"錯誤："+arg;

		msgView.setText(msg);
		actBtn.setVisibility(View.VISIBLE);
	}

	private void parseRest(Uri path){
		int[] speechStart, speechEnd, theoryStart, theoryEnd;
		List<String> list = path.getPathSegments();

		if(list.size()<6)restArgError(-1, path.getEncodedSchemeSpecificPart());
		if(!list.get(0).equalsIgnoreCase("play")){restArgError(0, list.get(0));return;}	// We just support play command now.
		if(!list.get(1).equalsIgnoreCase("region")){restArgError(1, list.get(1));return;}	// We just support region play mode now.
		if((speechStart=getSpeechData(list.get(2)))==null){restArgError(2, list.get(2));return;}
		if((speechEnd=getSpeechData(list.get(3)))==null){restArgError(3, list.get(3));return;}
		if((theoryStart=getTheoryData(list.get(4)))==null){restArgError(4, list.get(4));return;}
		if((theoryEnd=getTheoryData(list.get(5)))==null){restArgError(5, list.get(5));return;}
		String title=((list.size()==6?"":list.get(6)));

		startMainActivity(speechStart, speechEnd, theoryStart, theoryEnd, title);
	}

	private void restArgError(int i, String arg){
		String msg;
		if(i==-1)msg="參數數量不足："+arg;
		else msg="參數"+(i+1)+"錯誤："+arg;

		msgView.setText(msg);
		actBtn.setVisibility(View.VISIBLE);
	}

	private int[] getTheoryData(String str){
		if(str==null)return null;
		if(!str.matches("\\d{1,3}:\\d{1,2}"))return null;
		String[] split=str.split(":");
		if(split[0].length()>3 || split[1].length() > 3)return null;
		int[] result=new int[2];
		result[0]=Integer.parseInt(split[0])-1;
		result[1]=Integer.parseInt(split[1])-1;
		if(result[0]>=TheoryData.content.length)return null;
		if(result[1]>=TheoryData.content[result[0]].length())return null;
		return result;
	}
	
	private int[] getSpeechData(String str){
		if(str==null)return null;
		if(!str.matches("\\d{1,3}[AaBb]:\\d{1,2}:\\d{1,2}(\\.\\d{1,3})?"))return null;
		String split[]=str.split(":");
		if(split.length!=3)return null;
		if(split[0].length()>4 || split[1].length()>2 || split[2].length()>6)return null;
		int speechData[]=GlRecord.getSpeechStrToInt(str);
		if(speechData==null || speechData[0]<0 || speechData[0]>=SpeechData.name.length)return null;
		return speechData;
	}

	private void startMainActivity(int[] speechStart, int[] speechEnd, int[] theoryStart, int[] theoryEnd, String title){
		Intent lrInt = new Intent(IntentActivity.this, LamrimReaderActivity.class);
		Log.d(getClass().getName(), "This intent=" + lrInt);
		lrInt.putExtra("mediaStart", speechStart[0]);
		lrInt.putExtra("startTimeMs", speechStart[1]);
		lrInt.putExtra("mediaEnd", speechEnd[0]);
		lrInt.putExtra("endTimeMs", speechEnd[1]);
		lrInt.putExtra("theoryStartPage", theoryStart[0]);
		lrInt.putExtra("theoryStartLine", theoryStart[1]);
		lrInt.putExtra("theoryEndPage", theoryEnd[0]);
		lrInt.putExtra("theoryEndLine", theoryEnd[1]);
		lrInt.putExtra("mode", "region");
		if(title!=null)lrInt.putExtra("title", title);

		Util.fireSelectEvent(mFirebaseAnalytics, logTag, Util.STATISTICS, "LAUNCH_APP_WITH_DEEP_LINKING");
		lrInt.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		lrInt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		lrInt.setAction(getIntent().getAction());
		this.startActivity(lrInt);
		finish();
	}

	/**
	 * ATTENTION: This was auto-generated to implement the App Indexing API.
	 * See https://g.co/AppIndexing/AndroidStudio for more information.
	 */
	public Action getIndexApiAction() {
		Thing object = new Thing.Builder()
				.setName("Intent Page") // TODO: Define a title for the content shown.
				// TODO: Make sure this auto-generated URL is correct.
				.setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
				.build();
		return new Action.Builder(Action.TYPE_VIEW)
				.setObject(object)
				.setActionStatus(Action.STATUS_TYPE_COMPLETED)
				.build();
	}

	@Override
	public void onStart() {
		super.onStart();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		mGoogleApiClient.connect();
		AppIndex.AppIndexApi.start(mGoogleApiClient, getIndexApiAction());
	}

	@Override
	public void onStop() {
		super.onStop();

		// ATTENTION: This was auto-generated to implement the App Indexing API.
		// See https://g.co/AppIndexing/AndroidStudio for more information.
		AppIndex.AppIndexApi.end(mGoogleApiClient, getIndexApiAction());
		mGoogleApiClient.disconnect();
	}
}
