package eyes.blue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class RegionRecord {

	public int version = -1;
	public int contentSerial = -1;
	public String title=null, createTime=null, info = null;
	public int mediaStart=-1, mediaEnd = -1;
	public int theoryPageStart, theoryStartLine,theoryPageEnd, theoryEndLine;
	public int startTimeMs=-1, endTimeMs=-1;
	static ArrayList<RegionRecord> records = null;
	
	public static void init(Activity activity){records=getAllRecord(activity);}
	public static RegionRecord addRegionRecord(Activity activity, int contentSerial, String title, int mediaStart, int startTimeMs, int mediaEnd, int endTimeMs, int theoryPageStart, int theoryStartLine, int theoryPageEnd, int theoryEndLine, String info){
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd hh:mm:ss", Locale.getDefault());
		
		RegionRecord rr=new RegionRecord();
    	rr.version=2;
    	rr.contentSerial=contentSerial;
    	rr.title=title;
    	rr.mediaStart=mediaStart;
    	rr.mediaEnd=mediaEnd;
    	rr.theoryPageStart=theoryPageStart;
    	rr.theoryStartLine=theoryStartLine;
    	rr.theoryPageEnd=theoryPageEnd;
    	rr.theoryEndLine=theoryEndLine;
    	rr.createTime=sdf.format(Calendar.getInstance().getTime());
    	rr.startTimeMs=startTimeMs;
    	rr.endTimeMs=endTimeMs;
    	rr.info=info;
    	
    	records.add(0,rr);
    	syncToFile(activity); 

    	return rr;
	}
	
	public static RegionRecord getRegionRecord(Activity activity,int i){
		return records.get(i);
	}
	
	public static void removeRecord(Activity activity,int i){
		records.remove(i);
		syncToFile(activity);
	}
	
	public static void updateRecord(Activity activity, int contentSerial,String title,int mediaStart, int startTimeMs,int mediaEnd, int endTimeMs, int theoryPageStart, int theoryStartLine, int theoryPageEnd, int theoryEndLine, int recordIndex){
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy/MM/dd hh:mm:ss", Locale.getDefault());
		RegionRecord rr=records.get(recordIndex);
    	rr.version=2;
    	rr.contentSerial=contentSerial;
    	rr.title=title;
    	rr.mediaStart=mediaStart;
    	rr.mediaEnd=mediaEnd;
    	rr.theoryPageStart=theoryPageStart;
    	rr.theoryStartLine=theoryStartLine;
    	rr.theoryPageEnd=theoryPageEnd;
    	rr.theoryEndLine=theoryEndLine;
    	rr.createTime=sdf.format(Calendar.getInstance().getTime());
    	rr.startTimeMs=startTimeMs;
    	rr.endTimeMs=endTimeMs;
    	
		syncToFile(activity);
	}
	
	public static ArrayList<RegionRecord> getAllRecord(Activity context){
		if(records!=null)return records;
		
		BufferedReader br;
		String line="";
		records=new ArrayList<RegionRecord>();

		try {
			br=new BufferedReader(new InputStreamReader(context.openFileInput(context.getString(R.string.regionRecordColumeName))));
			while((line=br.readLine())!=null){
				records.add(RegionRecord.stringToObj(line));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return records;
		}catch (Exception e) {
			e.printStackTrace();
			showErrToast(context,"讀取檔案時發生錯誤，無法讀取區段記錄！");
		}
		return records;
	}
	
	public static void syncToFile(final Activity activity){
/*		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>(){
			@Override
			protected Void doInBackground(Void... arg0) {
*/				OutputStreamWriter osw;
				String str="";
				
				if(records.size()!=0)
				for(int  i=records.size()-1;i>0;i--)
					str+=objToString(records.get(i))+"\n";

				try {
					osw=new OutputStreamWriter(activity.openFileOutput(activity.getString(R.string.regionRecordColumeName),Context.MODE_WORLD_WRITEABLE));
					osw.write(str);
					osw.flush();
					osw.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					showErrToast(activity,"同步檔案時發生錯誤，找不到檔案！");
//					return null;
				} catch (IOException e) {
					e.printStackTrace();
					showErrToast(activity,"讀取檔案時發生錯誤，無法讀取區段記錄！");
//					return null;
				}
//				return null;
//			}};
//			task.execute();
	}
	
	
	public static RegionRecord stringToObj(String record){
		RegionRecord ro=new RegionRecord();
		try {
			JSONObject jObject = new JSONObject(record);
			ro.version=jObject.getInt("version");
			ro.contentSerial=jObject.getInt("contentSerial");
			ro.title=jObject.getString("title");
			ro.createTime=jObject.getString("createTime");
			ro.mediaStart=jObject.getInt("mediaStart");
			ro.mediaEnd=jObject.getInt("mediaEnd");
			ro.theoryPageStart=jObject.getInt("theoryPageStart");
			ro.theoryStartLine=jObject.getInt("theoryLineStart");
			ro.theoryPageEnd=jObject.getInt("theoryPageEnd");
			ro.theoryEndLine=jObject.getInt("theoryLineEnd");
			ro.startTimeMs=jObject.getInt("startTime");
			ro.endTimeMs=jObject.getInt("endTime");
			ro.info=jObject.getString("info");
		} catch (JSONException e) {	e.printStackTrace();}
		
		return ro;
		
	}
	
	public static String objToString(RegionRecord record){
		JSONObject jObj=new JSONObject();
		try {
		jObj.put("version", record.version);
		jObj.put("contentSerial", record.contentSerial);
		jObj.put("title", record.title);
		jObj.put("createTime", record.createTime);
		jObj.put("mediaStart", record.mediaStart);
		jObj.put("mediaEnd", record.mediaEnd);
		jObj.put("theoryPageStart",record.theoryPageStart);
		jObj.put("theoryLineStart",record.theoryStartLine);
		jObj.put("theoryPageEnd",record.theoryPageEnd);
		jObj.put("theoryLineEnd",record.theoryEndLine);
		jObj.put("startTime", record.startTimeMs);
		jObj.put("endTime", record.endTimeMs);
		jObj.put("info", record.info);
		} catch (JSONException e) {e.printStackTrace();}
		return jObj.toString();
	}
	
	private static void showErrToast(final Activity activity,final String msg){
		activity.runOnUiThread(new Runnable(){

			@Override
			public void run() {
				Toast toast=Toast.makeText(activity, msg, Toast.LENGTH_LONG);
				toast.show();
			}});
		
	}
}
