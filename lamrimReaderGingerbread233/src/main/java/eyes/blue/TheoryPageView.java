package eyes.blue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import eyes.blue.R;

public class TheoryPageView extends TextView {
	Context context;
	boolean onCmd=false;
	final static boolean debug=false;

	// For onTouchListener
	static final int NONE = 0;  
	static final int ZOOM = 1;  
	int mode = NONE;  
	static final int MIN_FONT_SIZE = 20;  
	static final int MAX_FONT_SIZE = 150;  
	float orgDist = 1f;
	float orgFontSize=0;
	Dot[] dots=new Dot[100];
	int textColor, highColorWord,highColorLine,bgColor,numTextColor,boldColor,dotTextColor;

	SharedPreferences runtime ;
	
	public TheoryPageView(Context context) {
		super(context);
		this.context=context;
//		this.setOnTouchListener(touchListener);
		loadColor();
	}

	public TheoryPageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context=context;
 //       this.setOnTouchListener(touchListener);
        loadColor();
    }
	
	public TheoryPageView(Context context, AttributeSet attrs, int defStyle) 
	{
	    super(context, attrs, defStyle);
	    this.context=context;
	    loadColor();
	}
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void loadColor(){
		runtime = context.getSharedPreferences(context.getString(R.string.runtimeStateFile), 0);
		boolean isDarkTheme=runtime.getBoolean(context.getString(R.string.isDarkThemeKey), true);
		
		textColor = ((isDarkTheme)?context.getResources().getInteger(R.color.darkTheoryTextColor):context.getResources().getColor(R.color.lightTheoryTextColor));
		// There is no background color for light theme.
		//bgColor = ((isDarkTheme)?-1:context.getResources().getColor(R.color.lightTheoryBgColor));
		numTextColor = ((isDarkTheme)?context.getResources().getColor(R.color.darkTheoryNumTextColor):context.getResources().getColor(R.color.lightTheoryNumTextColor));
		dotTextColor = ((isDarkTheme)?context.getResources().getColor(R.color.darkTheoryDotTextColor):context.getResources().getColor(R.color.lightTheoryDotTextColor));
		boldColor = ((isDarkTheme)?context.getResources().getColor(R.color.darkTheoryBoldColor):context.getResources().getColor(R.color.lightTheoryBoldColor));
		highColorLine = ((isDarkTheme)?context.getResources().getColor(R.color.darkTheoryHighColorLine):context.getResources().getColor(R.color.lightTheoryHighColorLine));
		highColorWord = ((isDarkTheme)?context.getResources().getColor(R.color.darkTheoryHighColorWord):context.getResources().getColor(R.color.lightTheoryHighColorWord));
		
/*		if(isDarkTheme){
			int sdk = android.os.Build.VERSION.SDK_INT;
			if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
			    setBackgroundDrawable(getResources().getDrawable(R.drawable.theory_background));
			} else {
				setBackground(getResources().getDrawable(R.drawable.theory_background));
			}
		}
		else
			this.setBackgroundColor(context.getResources().getColor(R.color.lightTheoryBgColor));
		*/
	}
	
	public void highlightWord(int startIndex, int length){
		SpannableStringBuilder text=new SpannableStringBuilder(getText());
		String str=text.toString();
		int invalidStrCount=0;
		for(int i=startIndex;i<startIndex+length;i++){
			if(str.charAt(i)=='\n')
				invalidStrCount++;
		}
		
		int strLen=length+invalidStrCount;
		text.setSpan(new BackgroundColorSpan(highColorWord), startIndex, startIndex+strLen, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
		super.setText(text);
	}
	
	
	/*
	 * Set highlight to whole line, from startLine to endLine, assign -1 to endLine that mean to whole end of line, if you want clear highlight call setText(String text).
	 * */
	public void setHighlightLine(int startLine, int endLine){
		Log.d(getClass().getName(),"get setHighlightLine call: startLine="+startLine+", endLine="+endLine);
		SpannableStringBuilder text=new SpannableStringBuilder(getText());
		String str=text.toString();
		String[] lines = str.split("\n");
		if(endLine==-1){
			endLine=lines.length-1;
			Log.d(getClass().getName(),"highlight to end: "+endLine);
		}
		int wordCounter=0;
		
		for(int i=0;i<lines.length;i++){
			if(i>=startLine && i<=endLine){
				Log.d(getClass().getName(),"highlight: start="+wordCounter+", end="+wordCounter+lines[i].length());
				text.setSpan(new BackgroundColorSpan(highColorLine), wordCounter, wordCounter+lines[i].length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			}
			wordCounter+=lines[i].length()+1;
		}
		super.setText(text);
	}

	public void setText(String text){
		int lineCounter=0;
		int start=0,end=0;
		float smallSize=(float)getContext().getResources().getInteger(R.integer.theorySmallTextSizePercent)/100;
        
		SpannableStringBuilder  page = new SpannableStringBuilder ();
        SpannableStringBuilder  line = new SpannableStringBuilder ();
        
        boolean isBold=false, isNum=false, isSmall=false;
        int dotIndex=0;
        
		for(int i=0;i<text.length();i++){
        	char c=text.charAt(i);
        	if(onCmd){
        		if(c!='>'){end++;continue;}
        		if(debug)Log.d("LamrimReader","Find a command stop");
            	onCmd=false;
            	
           		switch(text.charAt(start)){
           			case '/':
           				switch(text.charAt(start+1)){
           					case 'b':if(debug)Log.d("LamrimReader","release bold command");isBold=false;break;
           					case 'n':if(debug)Log.d("LamrimReader","release num command");isNum=false;;break;
           					case 's':if(debug)Log.d("LamrimReader","release small command");isSmall=false;break;
           				};
           				break;
           			case 'b':if(debug)Log.d("LamrimReader","set bold command");isBold=true;break;
           			case 'n':if(debug)Log.d("LamrimReader","set num command");isNum=true;break;
           			case 's':if(debug)Log.d("LamrimReader","set small command");isSmall=true;break;
           		}
           		start=i+1;
           		end=start;
        	}
        	else if(c=='‧' || c=='。'){
        		if(text.charAt(start)!='‧'){
        			SpannableString str=new SpannableString (text.substring(start, end));
        			str.setSpan(new ForegroundColorSpan(textColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			if(isBold){
        				str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        				str.setSpan(new ForegroundColorSpan(boldColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			}
        			if(isNum)str.setSpan(new ForegroundColorSpan(numTextColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			if(isSmall)str.setSpan(new RelativeSizeSpan(smallSize), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			line.append(str);
        			dots[dotIndex]=new Dot(lineCounter,line.length(),(((isSmall)?smallSize:1)),c);
//        			dots[dotIndex].line=lineCounter;
//        			dots[dotIndex].word=line.length();
 //       			dots[dotIndex].rate= (((isSmall)?smallSize:1));
 //       			dots[dotIndex].c=c;

        		}
        		if(debug)Log.d("LamrimReader","Print "+text.substring(start, end)+", start: "+start+", end: "+end+", ("+(end-start)+")");
//        		Log.d("LamrimReader","Get point, Before:"+words);
        		//canvas.drawCircle(x, y+pointSize+2, pointSize, getPaint());
        		dots[dotIndex]=new Dot(lineCounter,line.length(),(((isSmall)?smallSize:1)),c);
        		//dots[dotIndex].line=lineCounter;
    			//dots[dotIndex].word=line.length();
    			//dots[dotIndex].rate= (((isSmall)?smallSize:1));
    			dotIndex++;
    			
        		start=i+1;
        		end=start;
        		continue;
        	}
        	else if(c=='\n'){
//        		Log.d("LamrimReader","Get new line, draw text from "+start+" to "+end+",on ("+x+","+y+") text length="+text.length());
        		//canvas.drawText(text, start, end, x, y, getPaint());
        		SpannableString str=new SpannableString (text.substring(start, end)+"\n");
        		str.setSpan(new ForegroundColorSpan(textColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        		if(isBold){
    				str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    				str.setSpan(new ForegroundColorSpan(boldColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			}
        		if(isNum)str.setSpan(new ForegroundColorSpan(numTextColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			if(isSmall)str.setSpan(new RelativeSizeSpan(smallSize), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			line.append(str);
    			page.append(line);
    			line.clear();
        		start=i+1;
        		end=start;
        		lineCounter++;
        		continue;
        	}
        	else if(c=='<'){
        		if(debug)Log.d("LamrimReader","Find a command start");
        		if(end-start>0){
        			SpannableString str=new SpannableString (text.substring(start, end));
        			str.setSpan(new ForegroundColorSpan(textColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			if(isBold){
        				str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        				str.setSpan(new ForegroundColorSpan(boldColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			}
        			if(isNum)str.setSpan(new ForegroundColorSpan(numTextColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			if(isSmall)str.setSpan(new RelativeSizeSpan(smallSize), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			line.append(str);
        			//page.append(line);
        			//canvas.drawText(text, start, end, x, y, getPaint());
        			//x+=getPaint().measureText("中")*(end-start);
        		}
        		
        		start=i+1;
        		end=start;
        		onCmd=true;
        	}
        	else if(i==text.length()-1){
        		if(end-start<0)continue;
        		SpannableString str=new SpannableString (text.substring(start, end+1));
        		str.setSpan(new ForegroundColorSpan(textColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			if(isBold){
    				str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    				str.setSpan(new ForegroundColorSpan(boldColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			}
    			if(isNum)str.setSpan(new ForegroundColorSpan(numTextColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			if(isSmall)str.setSpan(new RelativeSizeSpan(smallSize), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			line.append(str);
//    			page.append(line);
        	}
        	else{
        		end++;
        	}
        }
		page.append(line);

		dots[dotIndex]=null;
//		dots[dotIndex].line=-1;
//		dots[dotIndex].word=-1;
//		dots[dotIndex].rate=-1;
		super.setText(page);
    }
	
	
	
	@Override
	public void setTextSize(float size){
		super.setTextSize(size);
//		this.setOnTouchListener(touchListener);
		if(debug)Log.d("LamrimLeader","TheoryPageView.setTextSize() Set font size to "+size);
	}
	
	@Override
    protected void onDraw(Canvas canvas)
    {
		if(canvas == null)return;
		super.onDraw(canvas);
		int pointSize=(int) (getTextSize()/7);
		int yShift=(int) (getTextSize()/5);
		Paint paint=getPaint();
		paint.setColor(dotTextColor);
		
//		StaticLayout tempLayout = new StaticLayout(boldText, paint, 10000, android.text.Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
//		int lineCount = tempLayout.getLineCount();
		
		int count=0;
		float orgTextSize=getTextSize();
		
//WG		paint.setTextSize(orgTextSize);
		String[] lineContent=getText().toString().split("\n");
		for(Dot d:dots){
			//if(d.line==-1)break;
			if(d==null)break;
			Rect rect=new Rect();
			count++;
			
			int y=getLineBounds((int) d.line, rect);
			float fontSize=orgTextSize*d.rate;
			paint.setTextSize(fontSize);
			if(d.c=='。'){
				paint.setStyle(Paint.Style.STROKE);
				paint.setStrokeWidth(2);
			}
//			Log.d("TheoryPageView","Draw line: "+lineContent[d.line]);
			canvas.drawCircle(rect.left+(paint.measureText(lineContent[d.line],0,(int) d.word)), y+yShift, pointSize, paint);
			paint.setStyle(Paint.Style.FILL);
		}
		
		getPaint().setTextSize(orgTextSize);
    }
/*
	@Override
    protected void onDraw(Canvas canvas)
    {
        float orgTextSize=getTextSize();
        Rect bounds=new Rect();
        int pointSize=(int) (getTextSize()/7);
        String text = (String) super.getText().toString();
        int lineCounter=0;
        int start=0,end=0;
        int y=getLineBounds(lineCounter, bounds);
        int x=bounds.left;

        for(int i=0;i<text.length();i++){
        	char c=text.charAt(i);
        	if(onCmd){
        		if(c!='>'){end++;continue;}
        		if(debug)Log.d("LamrimReader","Find a command stop");
            	onCmd=false;
            	
           		switch(text.charAt(start)){
           			case '/':
           				switch(text.charAt(start+1)){
           				case 'b':if(debug)Log.d("LamrimReader","release bold command");getPaint().setFakeBoldText(false);break;
           				case 'n':if(debug)Log.d("LamrimReader","release num command");getPaint().setColor(Color.BLACK);break;
           				case 's':if(debug)Log.d("LamrimReader","release small command");getPaint().setTextSize(orgTextSize);break;
           				};break;
           			case 'b':if(debug)Log.d("LamrimReader","set bold command");getPaint().setFakeBoldText(true);break;
           			case 'n':if(debug)Log.d("LamrimReader","set num command");getPaint().setColor(Color.BLUE);break;
           			case 's':if(debug)Log.d("LamrimReader","set small command");getPaint().setTextSize((float) (getTextSize()*0.9));break;
           		}
           		start=i+1;
           		end=start;
        	}
        	else if(c=='.'){
        		if(text.charAt(start)!='.'){
        			canvas.drawText(text, start, end, x, y, getPaint());
        			x+=getPaint().measureText("中")*(end-start);
        		}
        		if(debug)Log.d("LamrimReader","Print "+text.substring(start, end)+", start: "+start+", end: "+end+", ("+(end-start)+")");
        		canvas.drawCircle(x, y+pointSize+2, pointSize, getPaint());
        		start=i+1;
        		end=start;
        		continue;
        	}
        	else if(c=='\n'){
        		canvas.drawText(text, start, end, x, y, getPaint());
        		start=i+1;
        		end=start;
        		y=getLineBounds(++lineCounter, bounds);
        		x=bounds.left;
        		continue;
        	}
        	else if(c=='<'){
        		if(debug)Log.d("LamrimReader","Find a command start");
        		if(end-start>0){
        			canvas.drawText(text, start, end, x, y, getPaint());
        			x+=getPaint().measureText("中")*(end-start);
        		}
        		
        		start=i+1;
        		end=start;
        		onCmd=true;
        	}
        	else{
        		end++;
        	}
/* Print with copy data to new object "words"
        	char c=text.charAt(i);
        	if(c=='.'){
        		canvas.drawText(words, 0, words.length(), x, y, getPaint());
        		wordCounter+=words.length();
        		x+=wordLen*words.length();
        		
        		Log.d("LamrimReader","Get point, Before:"+words);
        		canvas.drawCircle(x, y+pointSize+2, pointSize, paint);
        		words="";
        		continue;
        	}
        	else if(c=='\n'){
        		Log.d("LamrimReader","Get new line.");
        		canvas.drawText(words, 0, words.length(), x, y, getPaint());
//        		x+=wordLen*words.length();
        		words="";
        		lineCounter++;
        		wordCounter=0;
        		y=getLineBounds(lineCounter, bounds);
        		x=bounds.left;
        		continue;
        	}
        	else{
        		words+=c;
        	}
        	
        	
//        	int baseLine=this.getLineBounds(lineCounter, bounds);
//        	canvas.translate(bounds.left, baseLine);
//        	canvas.drawText(c, 0, 0, getPaint());
        	
        }
//        canvas.drawLine(bounds.left, baseLine + 1, bounds.right, baseLine + 1, paint);
    }
*/
	/*
	OnTouchListener touchListener=new OnTouchListener(){
		public boolean onTouch(View v, MotionEvent event) {
			float x,y;
//			editText = (EditText) findViewById(R.id.editText1);  
			
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				
				// return false for long click listener
				Log.d(getClass().getName(),"First click event in to onTouchListener, return fals.");
				return false;

			case MotionEvent.ACTION_POINTER_DOWN:
				
				if(event.getActionIndex()==1){
					Log.d(getClass().getName(),"Second click event in to onTouchListener.");
				x = (event.getX(0) - event.getX(1));  
				y = event.getY(0) - event.getY(1);  
				orgDist = (float) Math.sqrt(x * x + y * y);
				if (orgDist > 12f) {
					mode = ZOOM;
					orgFontSize=getTextSize();
				}
				return true;
				}
				
				 
				break;
			case MotionEvent.ACTION_POINTER_UP:  
				mode = NONE;  
				break;  
			case MotionEvent.ACTION_MOVE:  
				if (mode == ZOOM && event.getActionIndex()==1) {
					x = (event.getX(0) - event.getX(1));  
				    y = event.getY(0) - event.getY(1);  
					float newDist = (float) Math.sqrt(x * x + y * y);
					float dp=(newDist - orgDist) * getResources().getDisplayMetrics().density;
					float size = orgFontSize+dp/12;
					Log.d(getClass().getName(),"OrgDist="+orgDist+", Dist= "+dp+"dp, scale="+size);
	 
					if ((size < MAX_FONT_SIZE && size > MIN_FONT_SIZE)) {  
						setTextSize(TypedValue.COMPLEX_UNIT_PX, size);  
					}  
				}  
				break;  
			}  
			return true;  
		  }  	
	};
	*/
	
	class Dot{
		public int line=-1, word=-1;
		public float rate;
		public char c;
		
		public Dot(int line, int word, float rate, char c){
			this.line=line;
			this.word=word;
			this.rate=rate;
			this.c=c;
		}
	}
}
