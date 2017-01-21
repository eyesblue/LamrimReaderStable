package eyes.blue;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class BaseDialogs {
	
	
	public static void showDelWarnDialog(Context context, String target, String positiveBtnString, DialogInterface.OnClickListener positiveListener, String negativeBtnString, DialogInterface.OnClickListener negativeListener){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(String.format(context.getString(R.string.dlgDelWarnTitle),target));
		builder.setMessage(String.format(context.getString(R.string.dlgDelWarnMsg),target));
		
		if(positiveBtnString == null)
			positiveBtnString=context.getString(R.string.dlgOk);
		
		builder.setPositiveButton(positiveBtnString, positiveListener);
		
		if(negativeListener==null)
			builder.setNegativeButton(context.getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
		else
			builder.setNegativeButton(negativeBtnString, negativeListener);
		
		builder.create().show();
	}
	
	//public static void showEditRegionDialog(final Activity activity,final int mediaIndex, final int startTimeMs, final int endTimeMs,final String titleStr,final SimpleAdapter adapter, final int recIndex){
	public static void showEditRegionDialog(final Activity activity,final int mediaStart, final int startTimeMs,final int mediaEnd, final int endTimeMs, final int theoryStartPage, final int theoryStartLine, final int theoryEndPage, final int theoryEndLine, final String info, final int recIndex,  final Runnable positiveListener){
		LayoutInflater factory = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    final View v = factory.inflate(R.layout.save_region_dialog, null);
	    final EditText regionTitle=(EditText) v.findViewById(R.id.regionTitle);
	    final EditText regionTheoryPageStart=(EditText) v.findViewById(R.id.startPage);
	    final EditText regionTheoryPageEnd=(EditText) v.findViewById(R.id.endPage);
	    final EditText regionTheoryStartLine=(EditText) v.findViewById(R.id.startLine);
	    final EditText regionTheoryEndLine=(EditText) v.findViewById(R.id.endLine);
	    final TextView startTime=(TextView) v.findViewById(R.id.startTime);
	    final TextView endTime=(TextView) v.findViewById(R.id.endTime);
	    final String startHMS=SpeechData.getSubtitleName(mediaStart)+"  "+Util.getMsToHMS(startTimeMs, ":", "", true);
		final String endHMS=SpeechData.getSubtitleName(mediaEnd)+"  "+Util.getMsToHMS(endTimeMs, ":", "", true);
		
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				regionTitle.requestFocus();
				if(recIndex!=-1){
					RegionRecord record=RegionRecord.getRegionRecord(activity, recIndex);
					regionTitle.setText(record.title);
					
					if(record.theoryPageStart!=-1 && record.theoryStartLine != -1 && record.theoryPageEnd != -1 && record.theoryEndLine != -1){
						regionTheoryPageStart.setText(""+(record.theoryPageStart+1));
						regionTheoryPageEnd.setText(""+(record.theoryPageEnd+1));
						regionTheoryStartLine.setText(""+(record.theoryStartLine+1));
						regionTheoryEndLine.setText(""+(record.theoryEndLine+1));
					}
				}
				else {
					Log.d(getClass().getName(),"theoryStartPage="+theoryStartPage+", theoryStartLine="+theoryStartLine+", theoryEndPage="+theoryEndPage+", theoryEndLine="+theoryEndLine);
					if(theoryStartPage != -1 && theoryStartLine != -1 && theoryEndPage != -1 && theoryEndLine != -1){
						Calendar c = Calendar.getInstance();
						int month = c.get(Calendar.MONTH) + 1;
						int week = c.get(Calendar.WEEK_OF_MONTH);
						String autoTitle = month + "月第" + week + "週";
						regionTitle.setText(autoTitle);
						regionTitle.setSelection(0, autoTitle.length());
						regionTheoryPageStart.setText(""+(theoryStartPage+1));
						regionTheoryPageEnd.setText(""+(theoryEndPage+1));
						regionTheoryStartLine.setText(""+(theoryStartLine+1));
						regionTheoryEndLine.setText(""+(theoryEndLine+1));
					}
				}
				startTime.setText(startHMS);
				endTime.setText(endHMS);
			}});
	    
	    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	    builder.setTitle("儲存區段");
	    builder.setPositiveButton(activity.getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Check name can't be empty.
				String title=regionTitle.getText().toString().trim();
				if(title.length()==0){
					showErrorDialog(activity, activity.getString(R.string.inputTitleHint));
					return;
				}

				String inPageStart=regionTheoryPageStart.getText().toString();
				String inPageEnd=regionTheoryPageEnd.getText().toString();
				String inLineStart=regionTheoryStartLine.getText().toString();
				String inLineEnd=regionTheoryEndLine.getText().toString();
				
				// Check Theory page, start line and end line.
				int theoryPageStart,theoryPageEnd, inStartLine, inEndLine;
				try{
					theoryPageStart=Integer.parseInt(inPageStart.trim())-1;
					theoryPageEnd=Integer.parseInt(inPageEnd.trim())-1;
					inStartLine=Integer.parseInt(inLineStart.trim())-1;
					inEndLine=Integer.parseInt(inLineEnd.trim())-1;
				}catch(NumberFormatException nfe){
					showErrorDialog(activity, activity.getString(R.string.dlgNumberFormatError));
					return;
				}
				
				if(theoryPageStart< 0 || theoryPageEnd< 0 || inStartLine<0 || inEndLine <0){
					BaseDialogs.showErrorDialog(activity, activity.getString(R.string.dlgPageNumOverPageCount));
					dialog.dismiss();
					return;
				}
				
				if(theoryPageStart>=TheoryData.content.length || theoryPageEnd >= TheoryData.content.length){
					BaseDialogs.showErrorDialog(activity, activity.getString(R.string.dlgPageNumOverPageCount));
					dialog.dismiss();
					return;
				}
				
				// Check if End page greater then Start page
				if(theoryPageEnd < theoryPageStart){
					showErrorDialog(activity, activity.getString(R.string.dlgEndPageGreaterThenStart));
					return;
				}
				
				// Check if the same page, but end line greater then start line
				if(theoryPageEnd == theoryPageStart && inEndLine < inStartLine){
					Log.d(getClass().getName(),"User input the same page, but line number end > start.");
					showErrorDialog(activity, activity.getString(R.string.dlgEndLineGreaterThenStart));
					return;
				}
				
				// Check if the line count over the count of page.
				if(inStartLine<0 || inEndLine >= TheoryData.content[theoryPageEnd].length()){
					showErrorDialog(activity, activity.getString(R.string.dlgLineNumOverPageCount));
					return;
				}
				
				if(recIndex==-1){
					Log.d(getClass().getName(),"Save record");
					RegionRecord.addRegionRecord(activity, 0, regionTitle.getText().toString(), mediaStart, startTimeMs, mediaEnd, endTimeMs, theoryPageStart, inStartLine, theoryPageEnd, inEndLine, info);
					}
				else
					RegionRecord.updateRecord(activity, 0, regionTitle.getText().toString(), mediaStart, startTimeMs, mediaEnd, endTimeMs, theoryPageStart, inStartLine, theoryPageEnd, inEndLine, recIndex);

				if(positiveListener!=null){
					Log.d(getClass().getName(),"Region data saved, notify dataset changed.");
					positiveListener.run();
				}
				dialog.dismiss();
			}
			
			
	    });
	    builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}});
	    
	    AlertDialog setTextSizeDialog=builder.create();
	    setTextSizeDialog.setView(v);
	    setTextSizeDialog.setCanceledOnTouchOutside(false);
	    setTextSizeDialog.show();
	}
	
	public static void showErrorDialog(final Activity activity, final String msg){
		showErrorDialog(activity, activity.getString(R.string.dlgInputError), msg);
		return;
	}

	public static void showErrorDialog(final Activity activity,final String title,  final String msg){
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(title).setMessage(msg).create();
				dialog.setCanceledOnTouchOutside(true);
				dialog.show();
			}});
		return;
	}

	
	public static void showToast(Context context,String msg){
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}

	public static void showMsgDialog(Context context, String title, String msg) {
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(context)
				.setTitle(title)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setMessage(msg);
		builderSingle.setPositiveButton("確定", null);
		builderSingle.setCancelable(false);
		builderSingle.show();
	}

	public static void  showSubtitleSearchDialog(Context c, SubtitleSearch[] sse){

	}
}
