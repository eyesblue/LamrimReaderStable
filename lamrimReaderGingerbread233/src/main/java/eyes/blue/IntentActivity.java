package eyes.blue;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;


public class IntentActivity  extends FragmentActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(getClass().getName(),"Check intent.");
		Intent intent=this.getIntent();
		String action=intent.getAction();
		Log.d(getClass().getName(),"Action: "+action+", Categories: "+intent.getCategories()+", Scheme: "+intent.getScheme()+", Mime type: "+intent.getType()+", Data: "+intent.getData());
		Uri intentPathUri=intent.getData();
		Log.d(getClass().getName(),"Intent data: "+intentPathUri);
		if(intentPathUri == null)finish();
		
		Log.d(getClass().getName(),"Scheme: "+intentPathUri.getScheme());
		Log.d(getClass().getName(),"EncodedFragment: "+intentPathUri.getEncodedFragment());
		Log.d(getClass().getName(),"EncodedPath: "+intentPathUri.getEncodedPath());
		Log.d(getClass().getName(),"EncodedQuery: "+intentPathUri.getEncodedQuery());
		Log.d(getClass().getName(),"EncodedSchemeSpecificPart: "+intentPathUri.getEncodedSchemeSpecificPart());
		Log.d(getClass().getName(),"Host: "+intentPathUri.getHost());
		Log.d(getClass().getName(),"LastPathSegment: "+intentPathUri.getLastPathSegment());
		Log.d(getClass().getName(),"Path: "+intentPathUri.getPath());
		
		if(!intentPathUri.toString().startsWith(getString(R.string.lamrimCmdUri))){
			Intent sendIntent = new Intent();
	    	sendIntent.setAction(Intent.ACTION_SEND);
	    	sendIntent.putExtra(Intent.EXTRA_TEXT, intentPathUri);
	    	sendIntent.setType("text/plain");
	    	startActivity(Intent.createChooser(sendIntent, "區段分享"));
	    	finish();
		}
		
		// We just support play command now.
		if(!intentPathUri.getPath().replace("/", "").equals("play"))finish();
		// We just support region play now.
		String mode=intentPathUri.getQueryParameter("mode");
		if(mode == null || !mode.equals("region"))finish();
		
		int speechStart[]= getSpeechData(intentPathUri.getQueryParameter("speechStart"));
		if(speechStart==null)finish();
		int speechEnd[]=getSpeechData(intentPathUri.getQueryParameter("speechEnd"));
		if(speechEnd==null)finish();
		int[] theoryStart=getTheoryData(intentPathUri.getQueryParameter("theoryStart"));
		if(theoryStart == null)finish();
		int[] theoryEnd=getTheoryData(intentPathUri.getQueryParameter("theoryEnd"));
		if(theoryEnd == null)finish();
		
		// Check the speech index not over 1
		if(Math.abs(speechStart[0] - speechEnd[0])>1)finish();
		
		Log.d(getClass().getName(),"Parse result: mediaStart="+speechStart[0]+", startTimeMs="+speechStart[1]+", mediaEnd="+speechEnd[0]+", theoryStartPage="+theoryStart[0]+", theoryStartLine="+theoryStart[1]
				+", theoryEndPage="+theoryEnd[0]+", theoryEndLine="+theoryEnd[1]+", title="+Uri.decode(intentPathUri.getQueryParameter("title")));
		Intent lrInt=new Intent(IntentActivity.this, LamrimReaderActivity.class);
		Log.d(getClass().getName(),"This intent="+lrInt);
		lrInt.putExtra("mediaStart", speechStart[0]);
		lrInt.putExtra("startTimeMs", speechStart[1]);
		lrInt.putExtra("mediaEnd", speechEnd[0]);
		lrInt.putExtra("endTimeMs", speechEnd[1]);
		lrInt.putExtra("theoryStartPage", theoryStart[0]);
		lrInt.putExtra("theoryStartLine", theoryStart[1]);
		lrInt.putExtra("theoryEndPage", theoryEnd[0]);
		lrInt.putExtra("theoryEndLine", theoryEnd[1]);
		lrInt.putExtra("title", Uri.decode(intentPathUri.getQueryParameter("title")));
		lrInt.putExtra("mode", "region");
		lrInt.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		lrInt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		lrInt.setAction(getIntent().getAction());
		this.startActivity(lrInt);
		finish();
	}
	
	private int[] getTheoryData(String str){
		if(!str.matches("\\d+:\\d+"))return null;
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
		String split[]=str.split(":");
		if(split.length!=3)return null;
		if(split[0].length()>4 || split[1].length()>2 || split[2].length()>6)return null;
		int speechData[]=GlRecord.getSpeechStrToInt(str);
		if(speechData[0]<0 || speechData[0]>=SpeechData.name.length)return null;
//		if(speechData[1] < 0 || speechData[1]>SpeechData.length[speechData[0]])return null;
		return speechData;
	}
}
