package eyes.blue.modified;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eyes.blue.GaLogger;
import eyes.blue.R;
import eyes.blue.SpeechData;
import eyes.blue.TheoryData;
import eyes.blue.TheoryPageView;
import eyes.blue.Util;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class MyListView extends ListView {
	public final static int TO_START=1;
	public final static int TO_END=2;

	Context context;
	SharedPreferences runtime = null;
	Typeface educFont = null;
	TheoryListAdapter adapter = null;
	ArrayList<HashMap<String, String>> bookList = null;
	ScaleGestureDetector scaleGestureDetector=null;
	OnDoubleTapEventListener doubleTapEventListener=null;
//	View.OnTouchListener onTouchListener=null;
	int mFadeColor=0;
	int highlightLine[][];//[PageIndex][startLine][endLine]
	
	public MyListView(Context context) {
		super(context);this.context=context;
		init();
	}
	
	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context=context;
		init();
	}
	
	public MyListView(Context context, AttributeSet attrs, int defStyle) 
	{
	    super(context, attrs, defStyle);
	    this.context=context;
	    init();
	}
	
	private void init(){
		runtime = context.getSharedPreferences(context.getString(R.string.runtimeStateFile), 0);
		educFont=Typeface.createFromAsset(context.getAssets(), "EUDC.TTF");
		bookList = new ArrayList<HashMap<String, String>>();
        int pIndex = 0;

        for (String value : TheoryData.content) {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put("page", value);
                item.put("desc", "第 " + (++pIndex) + " 頁");
                bookList.add(item);
        }
        
        adapter = new TheoryListAdapter(context, R.layout.theory_page_view, R.id.aboutTextTitle, TheoryData.content);
		setAdapter(adapter);
    	
    	setScaleGestureDetector(new ScaleGestureDetector(context,new SimpleOnScaleGestureListener() {
    		//int textSizeMax=context.getResources().getInteger(R.integer.textMaxSize);
    		//int textSizeMin=context.getResources().getInteger(R.integer.textMinSize);
    		
    		int textSizeMax=Util.getMaxFontSize((Activity)context);
    		int textSizeMin=Util.getMinFontSize((Activity)context);
    		@Override
    		public boolean onScaleBegin(ScaleGestureDetector detector) {
    			Log.d(getClass().getName(),"Begin scale called factor: "+detector.getScaleFactor());
    			GaLogger.sendEvent("ui_action", "bookview_event", "change_text_size_start", null);
    			return true;
    		}
    		@Override
    		public boolean onScale(ScaleGestureDetector detector) {
    			float size=adapter.getTextSize()*detector.getScaleFactor();
    			if(size<textSizeMin || size > textSizeMax)return true;
//    				Log.d(getClass().getName(),"Get scale rate: "+detector.getScaleFactor()+", current Size: "+adapter.getTextSize()+", setSize: "+adapter.getTextSize()*detector.getScaleFactor());
    				
    			adapter.setTextSize(size);
    			adapter.notifyDataSetChanged();
//    				Log.d(getClass().getName(),"set size after setting: "+adapter.getTextSize());
    				return true;
    			}
    		@Override
    		public void onScaleEnd(ScaleGestureDetector detector){
    			SharedPreferences.Editor editor = runtime.edit();
    			editor.putInt(context.getString(R.string.bookFontSizeKey), (int) adapter.getTextSize());
    			editor.commit();
    			GaLogger.sendEvent("ui_action", "bookview_event", "change_text_size_end", null);
    		}
    		}));
    	
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		boolean res=false;
		if(scaleGestureDetector==null)return false;
//		if(onTouchListener!=null)onTouchListener.
		if(event.getPointerCount()==2){
			// The scale gesture detector always return true.
			try{// Here will throw IllegalArgumentException sometimes.
				res=scaleGestureDetector.onTouchEvent(event);
			}catch(Exception e){
				e.printStackTrace();
				GaLogger.sendException(e, true);
				return false;
			}
//			Log.d(getClass().getName(),"Scale return "+res);
			return res;
		}
		
		try{// Here will throw IllegalArgumentException sometimes.
			res=super.onTouchEvent(event) | gestureListener.onTouchEvent(event) ;
		}catch(Exception e){
			e.printStackTrace();
			GaLogger.sendException(e, true);
			return false;
		}
//		Log.d(getClass().getName(),"TheoryPageView onTouchEvent return "+res);
		return res;
	}
//	public void setOnTouchListener(View.OnTouchListener onTouchListener){this.onTouchListener=onTouchListener;}
	public void setScaleGestureDetector(ScaleGestureDetector scaleGestureDetector){this.scaleGestureDetector=scaleGestureDetector;}
	
	GestureDetector gestureListener=new GestureDetector(context ,new android.view.GestureDetector.SimpleOnGestureListener(){
		@Override
		public boolean 	onDown(MotionEvent e){
			if(e.getPointerCount()> 1)return false;
			return true;
		}
		@Override
		public boolean 	onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){return true;}
		@Override
		public boolean 	onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
//			Log.d(getClass().getName(),"Into onScroll, distance("+distanceX+", "+distanceY+"), scroll point=("+getScrollX()+", "+getScrollY()+")");
			float scrollX=getScrollX()+distanceX;
//			float scrollY=getScrollY()+distanceY;
			float rightBoundY=getHeight()-getMeasuredHeight();
			float rightBoundX=getWidth()-getMeasuredWidth();
//			Log.d(getClass().getName(),"Layout params: ("+getLayout().getWidth()+", "+getLayout().getHeight()+", content size: ("+getWidth()+", "+getHeight()+"), meansure size: "+getMeasuredWidth()+", "+getMeasuredHeight());
			// reached Up/Left bound.
			
			// Restrict the left side can't be over.
			// if(scrollX<=0)scrollX=0;
			// Restrict the right side can't be over.
			//	if(scrollY<=0)scrollY=0;
			
				
/*			Log.d(getClass().getName(),"textWidth: "+textWidth+", scrollX: "+getScrollX()+", getMeasuredWidth: "+getMeasuredWidth());
			// Reached Right/bottom bound.
			if(textWidth-getMeasuredWidth()-getScrollX()<=0){
				Log.d(getClass().getName(),"Right bound reached Return false");
				return false;
			}			
			Log.d(getClass().getName(),"Scroll to ("+scrollX+", "+getScrollY()+")");
*/			scrollTo((int)scrollX,(int)getScrollY());
			
			// Left bound has reached, and still scroll to left
			//this.onDoubleTapEvent(e)
			if(scrollX==0 && distanceX < 0)return false;
			return true;
		}
		@Override
		public boolean onDoubleTapEvent(MotionEvent e){
			return doubleTapEventListener.onDoubleTap(e);
		}
	});
	
	
	
	
	@Override
	  public int getSolidColor()
	  {
	    return mFadeColor;
	  }
	  public void setFadeColor( int fadeColor )
	  {
	    mFadeColor = fadeColor;
	    this.invalidate();
	  }
	  public int getFadeColor()
	  {
	    return mFadeColor;
	  }
	  
	public void setOnDoubleTapEventListener(OnDoubleTapEventListener listener){this.doubleTapEventListener=listener;}
	public void setTextSize(float size){adapter.setTextSize(size);}
	public float getTextSize(){return adapter.getTextSize();}
	public void refresh(){
//		setAdapter(adapter);
		((Activity)context).getWindow().getDecorView().findViewById(android.R.id.content).post(new Runnable(){
			@Override
			public void run() {
				adapter.notifyDataSetChanged();
			}});
	}
	
	public void rebuildView(){
		int size=Math.round(getTextSize());
		adapter = new TheoryListAdapter(context, R.layout.theory_page_view, R.id.aboutTextTitle, TheoryData.content);
		setAdapter(adapter);
		adapter.notifyDataSetChanged();
		this.setTextSize(size);
	}
	
	public void setHighlightLine(int startPage, int startLine,int endPage, int endLine){
		int pageCount=endPage-startPage+1;
		highlightLine=new int[pageCount][3];
		if(pageCount==1){
			highlightLine[0][0]=startPage;
			highlightLine[0][1]=startLine;
			highlightLine[0][2]=endLine;
		}
		else{
			highlightLine[0][0]=startPage;
			highlightLine[0][1]=startLine;
			highlightLine[0][2]=-1;
			for(int i=1;i<pageCount-1;i++){
				highlightLine[i][0]=startPage+i;
				highlightLine[i][1]=0;
				highlightLine[i][2]=-1;
			}
			highlightLine[pageCount-1][0]=endPage;
			highlightLine[pageCount-1][1]=0;
			highlightLine[pageCount-1][2]=endLine;
		}
		
		// debug
		System.out.println("Content of highlineLine:");
		for(int i=0;i<pageCount;i++){
			System.out.println("["+highlightLine[i][0]+"] ["+highlightLine[i][1]+"] ["+highlightLine[i][2]+"]");
		}
		refresh();
	}
	public void clearHighlightLine(){
		highlightLine=null;
		((Activity)context).runOnUiThread(new Runnable(){
			@Override
			public void run() {
				refresh();
			}});
		
	}
	
	int[][] highlightWord=null; // this is real used for mark region.
	int[] highlightWordCallArg=null; // This is use for return to user query last call record.
	public void setHighlightWord(int startPage, int line, int startIndex, int length){
		highlightWordCallArg=new int[]{startPage, line, startIndex, length};
		Log.d(getClass().getName(),"Set highlight word at page "+startPage+", Line "+line+", index "+startIndex+", length "+length);
		int index=lineWordToIndex(startPage, line, startIndex);
		Log.d(getClass().getName(),"Set highlight word at page "+startPage+", index "+index+", length "+length);
		String sample=getContentStr(startPage,0,TO_END);
		highlightWord=new int[2][3];
		Log.d(getClass().getName(),"startIndex+length="+(index+length)+", sample length="+sample.length());
		if(index+length<sample.length()){
			Log.d(getClass().getName(),"The highlight in one page");
			highlightWord[0][0]=startPage;
			highlightWord[0][1]=index;
			highlightWord[0][2]=length;
			highlightWord[1][0]=-1;
			highlightWord[1][1]=-1;
			highlightWord[1][2]=-1;
		}
		else{
			Log.d(getClass().getName(),"The highlight words over second page");
			highlightWord[0][0]=startPage;
			highlightWord[0][1]=index;
			highlightWord[0][2]=sample.length()-index;
			highlightWord[1][0]=startPage+1;
			highlightWord[1][1]=0;
			highlightWord[1][2]=length-(sample.length()-index);
			Log.d(getClass().getName(),"Set highlight at page,line,word: "+highlightWord[0][0]+", "+highlightWord[0][1]+", "+highlightWord[0][2]+" and "+highlightWord[1][0]+", "+highlightWord[1][1]+", "+highlightWord[1][2]);
		}
		refresh();
	}

	public int[] getHighlightWord(){
		return highlightWordCallArg;
	}
/*	The function has bug that '\n' will into the result.	
	public String getHighlightWord(){
		if(highlightWord == null)return null;
		
		int firstPage=highlightWord[0][0],firstIndex=highlightWord[0][1],firstLength=highlightWord[0][2];
		if(firstLength<=0)return null;
		
		String sample=getContentStr(firstPage,0,TO_END);
		String res=sample.substring(firstIndex, firstIndex+firstLength);

		
		int secPage=highlightWord[1][0],secIndex=highlightWord[1][1],secLength=highlightWord[1][2];
		if(secLength>0){
			sample=getContentStr(secPage,0,TO_END);
			res+=sample.substring(secIndex, secIndex+secLength);
		}
		return res;
	}
*/	
	public void clearHighlightWord(){
		highlightWord=null;
		refresh();
	}
	
	public float setViewToPosition(final int page,int line){
/*		int firstView=getFirstVisiblePosition()+1;
		Log.d(getClass().getName(),"First view index: "+firstView);
		View v=getChildAt(firstView);
		TextView tpView=(TextView)v.findViewById(R.id.pageContentView);

		int padding=tpView.getPaddingTop();
*/		
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
		paint.setStyle(Paint.Style.FILL);
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setTextSize(adapter.getTextSize());
		Rect bounds = new Rect();
		paint.getTextBounds("a", 0, 1, bounds);
		
		float textSize=bounds.height();
//		float shift=textSize*line/context.getResources().getDisplayMetrics().densityDpi*160f;
		final float shift=-textSize*line*2.4f;
		Log.d(getClass().getName(),"Move view to page "+page+" shift "+shift);
		post(new Runnable(){
			@Override
			public void run() {
				rebuildView();
				setSelectionFromTop(page, (int) shift);
			}});
		refresh();
		return shift;
	}
	
	
	public int[] searchLast(int startPage, int startLine, int startWord, String str){
		Log.d(getClass().getName(),"search last "+str+" from page "+startPage+", line "+startLine+", word "+startWord);
		int rangePageStart=startPage;
		int rangePageEnd=startPage;
		int pageLen[][]=new int[5][2];
		int pageIndex=0;
		
		// if startWord == -1 that mean the search is from end to start
		if(startWord<0)startLine--;
		if(startLine<0)startPage--;
		if(startPage<0)return null;
		
		Log.d(getClass().getName(),"Call lineWordToIndex(page="+startPage+", line="+startLine+", word="+startWord+")"); 
		int startIndex=lineWordToIndex(startPage, startLine, startWord);
		
		String sample=getContentStr(startPage, startIndex, TO_START);
		pageLen[0][0]=startPage;
		pageLen[0][1]=sample.length();
		//while(rangePageEnd<TheoryData.content.length-1){
		while(rangePageEnd >= 0){  // Make sure the index not over page range.
			while(sample.length()<str.length()){  // Make sure the content longer then searching string.
				++pageIndex;
				if(--rangePageEnd<0)return null;  // The rest length of content not longer then searching string.
				
				sample=getContentStr(rangePageEnd,0,TO_END)+sample;
				pageLen[pageIndex][0]=rangePageEnd;
				pageLen[pageIndex][1]=sample.length();
				Log.d(getClass().getName(),"Add page "+rangePageEnd+" to sample");
			}
		
			int searchResult=searchString(sample,str,TO_START);
			if(searchResult==-1){
				rangePageStart=rangePageEnd;
				//int len=sample.length()-str.length()+1;
				sample=sample.substring(0,str.length()-1);
				pageIndex=0;
				pageLen[0][0]=rangePageStart;
				pageLen[0][1]=str.length()-1;
				System.out.println("Not found, sample ="+sample);
			}
			else{
				System.out.println("Found at "+searchResult+" of "+sample);
				for(int i=0;i<=pageIndex;i++){
					if(searchResult<pageLen[i][1]){
						
						int index=0;
						int[] result=new int[3];
						if(i==0){
							String pageContent=getContentStr(pageLen[i][0], 0,TO_END);
							//index=pageContent.length()-pageLen[i][1]+searchResult;
							//index=pageLen[i][1]-searchResult;
							index=searchResult-str.length()+1;
							System.out.println("Page content length="+pageContent.length()+", Start index = "+index);
						}
						else {
							index=searchResult-pageLen[i-1][1];
							System.out.println("searchResult("+searchResult+") - page index["+(i-1)+"][1]("+pageLen[i-1][1]+") = "+index);
						}
						
						int[] lineWord=indexToLineWord(pageLen[i][0],index);
												
						result[0]=pageLen[i][0];
						result[1]=lineWord[0];
						result[2]=lineWord[1];
						System.out.println("Found at page="+result[0]+", line="+result[1]+", word="+result[2]);
						return result;
					}
				}
			}
		}
		
		return null;
	}
	
	public int[] searchNext(int startPage, int startLine, int startWord, String str){
		return searchNext(startPage, TheoryData.content.length-1, startLine, startWord, str);
	}
	public int[] searchNext(int startPage, int endPage, int startLine, int startWord, String str){
		Log.d(getClass().getName(),"search next"+str+" from page "+startPage+", line "+startLine+", word "+startWord);
		int rangePageStart=startPage;
		int rangePageEnd=startPage;
		int pageLen[][]=new int[5][2];
		int pageIndex=0;
		int startIndex=lineWordToIndex(startPage, startLine, startWord);
		
		String sample=getContentStr(startPage, startIndex, TO_END);
		pageLen[0][0]=startPage;
		pageLen[0][1]=sample.length();
		while(rangePageEnd<endPage){
			while(sample.length()<str.length()){
				Log.d(getClass().getName(),"Sample="+sample+"sample length="+sample.length()+", str length="+str.length());
				++pageIndex;
				if(++rangePageEnd>=TheoryData.content.length)return null;  // The rest length of content not longer then searching string.
				
				sample+=getContentStr(rangePageEnd,0,TO_END);
				pageLen[pageIndex][0]=rangePageEnd;
				pageLen[pageIndex][1]=sample.length();
				Log.d(getClass().getName(),"Add page "+rangePageEnd+" to sample");
			}
		
			int searchResult=searchString(sample,str,TO_END);
			if(searchResult==-1){
				rangePageStart=rangePageEnd;
				int len=sample.length()-str.length()+1;
				sample=sample.substring(len);
				pageIndex=0;
				pageLen[0][0]=rangePageStart;
				pageLen[0][1]=sample.length();
				System.out.println("Not found, sample ="+sample);
			}
			else{
				System.out.println("Found at "+searchResult+" of "+sample);
				for(int i=0;i<=pageIndex;i++){
					if(searchResult<pageLen[i][1]){
						
						int index=0;
						int[] result=new int[3];
						if(i==0){
							String pageContent=getContentStr(pageLen[i][0], 0,TO_END);
							index=pageContent.length()-pageLen[i][1]+searchResult;
							System.out.println("Page content length="+pageContent.length()+", Start index = "+index);
						}
						else {
							index=searchResult-pageLen[i-1][1];
							System.out.println("searchResult("+searchResult+") - page index["+(i-1)+"][1]("+pageLen[i-1][1]+") = "+index);
						}
						
						int[] lineWord=indexToLineWord(pageLen[i][0],index);
												
						result[0]=pageLen[i][0];
						result[1]=lineWord[0];
						result[2]=lineWord[1];
						System.out.println("Found at page="+result[0]+", line="+result[1]+", word="+result[2]);
						return result;
					}
				}
			}
		}
		System.out.println("return null");
		return null;
	}
	
	/*
	 * Convert the PAGE,LINE,WORD index to linear index, if word == -1, that mean end of the LINE of PAGE, if line == -1, that mean end of the page.
	 * The index exclude "<b>","</b>","<n>", "</n>", "<s>", "</s>" but include '\n'.
	 * */
	private static int lineWordToIndex(int page, int lineIndex, int word){
		String sample=getContentStr(page, 0,TO_END);
		String line[]=sample.split("\n");
		int len[]=new int[line.length];
		
		for(int i=0;i<line.length;i++){
			if(i==0)len[0]=line[0].length()+1;
			else len[i]=line[i].length()+len[i-1]+1;
		}
		len[line.length-1]--;
		
		// Check is the word index or lineIndex has become -1.
		if(lineIndex==-1)return len[line.length-1];
		if(word==-1)return len[lineIndex];
		
		// Debug
		for(int i=0;i<line.length;i++)
			System.out.println(line[i]+"\n"+len[i]);
				
		int index=-1;
		if(lineIndex==0)index = word;
		else index = len[lineIndex-1]+word;
		
		try{
			System.out.println("Exam the index("+index+") of word = "+sample.charAt(index));
		}catch(Exception e){e.printStackTrace();}
		return index;
	}
	
	/*
	 * Convert the linear index to PAGE,LINE,WORD index, if word == -1, that mean end of the LINE of PAGE, if line == -1, that mean end of the page.
	 * The index exclude "<b>","</b>","<n>", "</n>", "<s>", "</s>" but include '\n'.
	 * */
	private int[] indexToLineWord(int page,int index){
		System.out.println("Convert page "+page+" index "+ index +" to line, word");
		String sample=getContentStr(page, 0,TO_END);
		String line[]=sample.split("\n");
		int len[]=new int[line.length];
		
		for(int i=0;i<line.length;i++){
			if(i==0)len[0]=line[0].length()+1;
			else len[i]=line[i].length()+len[i-1]+1;
		}
		len[line.length-1]--;
		
		// Debug
		for(int i=0;i<line.length;i++){
			System.out.println(line[i]+"\n"+len[i]);
		}
		
		int lineIndex=-1, word=-1;
		for(int i=0;i<line.length;i++){
			if(index < len[i]){
				if(i==0){
					lineIndex=0;
					word=index;
					if(word==line[lineIndex].length())System.out.println("Exam the converted string \\n");
					else System.out.println("Exam the converted string "+line[lineIndex].charAt(word));
					break;
				}
				else{
					lineIndex=i;
				
					word=index-len[i-1];
					System.out.println("Word = "+word);
					if(word==line[lineIndex].length())System.out.println("Exam the converted string \\n");
					else System.out.println("Exam the converted string "+line[lineIndex].charAt(word));
					break;
				}
			}
		}

		
		int result[]={lineIndex,word};
		System.out.println("Result page "+page+" line "+ lineIndex +" word "+word);
		return result;
	}
	
	/*
	 * Get content of page of theory data, the data exclude "<b>","</b>","<n>", "</n>", "<s>", "</s>" but include '\n'.
	 * The direct must be MyListView.TO_START(from 0 to fromIndex) or MyListView.TO_END(from fromIndex to end of content).
	 * */
	public static String getContentStr(int startPage, int fromIndex, int direct){
		System.out.println("Get content string page: "+startPage+", startWord: "+fromIndex);
		String page=TheoryData.content[startPage].replace("‧", "").replace("。", "");
		page=page.replace("<b>", "");
		page=page.replace("</b>", "");
		page=page.replace("<n>", "");
		page=page.replace("</n>", "");
		page=page.replace("<s>", "");
		page=page.replace("</s>", "");
		
		
		String result=null;
		if(direct == TO_START)result=page.substring(0,fromIndex);
		else result=page.substring(fromIndex); // <==== ************** need exam. ************* 
		
//		Log.d("MyListView","sample="+result);
		
		return result;
	}

	/*
	 * Search the str from sample string that return the index of sample where exist the same string with str.
	 * The direct must be MyListView.TO_START(from 0 to fromIndex) or MyListView.TO_END(from fromIndex to end of content).
	 * */
	private static int searchString(String sample, String str, int direct){
		Log.d("MyListView","The direct is "+((direct==TO_START)?"TO_START":"TO_END"));
		int shift=0;
		
		if(direct == TO_START){
			for(int i=sample.length()-1;i>=0;i--){
				shift=0;
				if(sample.charAt(i)=='\n')continue;
				for(int j=0;j<str.length();j++){
					while(true){	// Drop '\n'
						if((i-j-shift)<0)
							return -1;
						if(sample.charAt(i-j-shift) == '\n'){
							shift++;
							continue;
						}
						break;
					}
					// System.out.print("Compare: "+sample.charAt(i-j-shift)+" and "+str.charAt(str.length()-1-j));
					if(sample.charAt(i-j-shift) != str.charAt(str.length()-1-j)){
						System.out.println();
						break;
					}
					if(j==str.length()-1){
						System.out.println(" match!");
						return i-shift;
					}
				}
			}
			
			return -1;
		}
		
		// TO_END
		
		for(int i=0;i<sample.length()-str.length()+1;i++){
			shift=0;
			if(sample.charAt(i)=='\n')continue;
			for(int j=0;j<str.length();j++){
				while(true){	// Drop '\n'
					if((i+j+shift)>=sample.length())
						return -1;
					if(sample.charAt(i+j+shift) == '\n'){
						shift++;
						continue;
					}
					break;
				}
				
				//System.out.print("Compare: "+sample.charAt(i+j+shift)+" and "+str.charAt(j));
				
				if(sample.charAt(i+j+shift) != str.charAt(j)){
					System.out.println();
					break;
				}
				if(j==str.length()-1){
					System.out.println(" match!");
					return i;
				}
			}
		}
		return -1;
	}
	
	
	class TheoryListAdapter extends ArrayAdapter {
		float textSize = 0;

		public TheoryListAdapter(Context context, int resource) {
			super(context, resource);
		}
		public <T> TheoryListAdapter (Context context, int resource, int textViewResourceId, T[] objects){
			super(context, resource, textViewResourceId, objects);
		}
		
		@SuppressLint("NewApi")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				Log.d(getClass().getName(), "row=null, construct it.");
				LayoutInflater inflater = ((Activity)context).getLayoutInflater();
				row = inflater.inflate(R.layout.theory_page_view, parent, false);
				
				boolean isDarkTheme=runtime.getBoolean(context.getString(R.string.isDarkThemeKey), true);
				TextView pNum = (TextView) row.findViewById(R.id.pageNumView);
				int sdk = android.os.Build.VERSION.SDK_INT;
				int background=-1,textColor=-1;
				if(isDarkTheme){
					background=R.drawable.theory_dark_bg;
					textColor=R.color.darkTheoryTextColor;
				}
				else {
					background=R.drawable.theory_light_bg;
					textColor=R.color.lightTheoryTextColor;
				}

				if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) 
					row.setBackgroundDrawable(getResources().getDrawable(background));
				else 
					row.setBackground(getResources().getDrawable(background));
				pNum.setTextColor(context.getResources().getColor(textColor));
			}

			// / Log.d(logTag, "row=" + row+", ConvertView="+convertView);
			TheoryPageView bContent = (TheoryPageView) row.findViewById(R.id.pageContentView);
			bContent.setHorizontallyScrolling(true);
			// bContent.drawPoints(new int[0][0]);
			bContent.setTypeface(educFont);
			bContent.setText(bookList.get(position).get("page"));
			if(highlightLine!=null && position>=highlightLine[0][0] && position<=highlightLine[highlightLine.length-1][0]){
				int index=position-highlightLine[0][0];
				bContent.setHighlightLine(highlightLine[index][1], highlightLine[index][2]);
			}
			if(highlightWord!=null){
				if(position==highlightWord[0][0]){
					bContent.highlightWord(highlightWord[0][1], highlightWord[0][2]);
				}
				if(position==highlightWord[1][0]){
					bContent.highlightWord(highlightWord[1][1], highlightWord[1][2]);
				}
			}
			// bContent.setText(Html.fromHtml("<font color=\"#FF0000\">No subtitle</font>"));
			TextView pNum = (TextView) row.findViewById(R.id.pageNumView);
			if (pNum.getTextSize() != textSize){
				bContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
				pNum.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
			}
			pNum.setText(bookList.get(position).get("desc"));
			return row;
		}

		public void setTextSize(float size) {
			textSize = size;
		}
		public float getTextSize(){
			return textSize;
		}
		
	}
}
