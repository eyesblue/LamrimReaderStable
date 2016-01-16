package eyes.blue.modified;

import eyes.blue.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.SeekBar;

public class RegionableSeekBar extends SeekBar {
	Context context = null;
	String logTag="RegionableSeekBar";
	int start=-1, end=-1, max=-1;
	public static int maxValueOfMCView=1000;
	
	public RegionableSeekBar(Context context) {
		super(context);
		this.context=context;
	}

	public RegionableSeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context=context;
	}

	public RegionableSeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context=context;
	}

	
	public void setRegionStart(int start, int max){
		this.start=start;
		this.max=max;
	}
	
	public void setRegionEnd(int end, int max){
		this.end=end;
		this.max=max;
	}
	
	public void setRegionMode(int start, int end, int max){
		this.start=start;
		this.end=end;
		this.max=max;
	}
	
	public void disableRegionMode(){
		this.start=-1;
		this.end=-1;
		drawRegionTimes=0;
	}
	
	public int[] getRegionValue(){
		return new int[]{start,end};
	}

	@Override
	public void invalidate(){
		drawRegionTimes=0;
		super.invalidate();
	}
	@Override
	protected void onDraw (Canvas canvas){
		
		if(start == -1 && end ==-1){
			super.onDraw(canvas);
			drawNormalMode(canvas);
		}			
		else {
			super.onDraw(canvas);
			drawRegionMode(canvas);
		}
	}
	
	int drawRegionTimes=0;
	protected void drawRegionMode(Canvas canvas){
		if(drawRegionTimes==1){
			drawRegionTimes=0;
			return;
		}
		Log.d(logTag,"Draw Region mode: start = "+start+", region end = "+end);
        LayerDrawable layer = (LayerDrawable) getProgressDrawable();
        Drawable drawableFg = (Drawable)layer.findDrawableByLayerId(android.R.id.progress);
        Drawable drawableBg = (Drawable)layer.findDrawableByLayerId(android.R.id.background);


        
        Log.d(logTag,"There are "+layer.getNumberOfLayers()+" layer in SeekBar object, forground: "+drawableFg+", background: "+drawableBg);
       
        Rect fgBound=drawableFg.copyBounds();
        Rect bgBound=drawableBg.copyBounds();
       
        // The view never draw, skip draw.
        if(fgBound.height()==0){
                Log.d(logTag,"The seekbar not layouted, skip");
                return ;
        }

        Log.d(logTag,"forgound: bound.right="+fgBound.right+", bound.left="+fgBound.left+", bound.top="+fgBound.top+", bound.botton="+fgBound.bottom+", IntrinsicWidth="+drawableFg.getIntrinsicWidth()+",IntrinsicHeight= "+drawableFg.getIntrinsicHeight()+", rect.height="+fgBound.height()+", rect.width="+fgBound.width());
        Log.d(logTag,"backgound: bound.right="+bgBound.right+", drawableFg.getIntrinsicWidth="+drawableBg.getIntrinsicWidth());
       
        Log.d(logTag,"Debug: drawableFg: "+drawableFg+", copyBounds(): "+ drawableFg.copyBounds()+", getIntrinsicWidth: "+drawableFg.getIntrinsicWidth());
       
        //int seekBarStartPosition=Math.round ((regionStartMs==-1)?fgBound.left:(float)regionStartMs/mediaPlayer.getDuration()*fgBound.width());
        int seekBarStartPosition=Math.round ((start==-1)?fgBound.left:(float)start/max*fgBound.width());
        // Add one pixel avoid while enableEnd = enableStart, there will throw exception while copy pixel.
        int seekBarEndPosition=Math.round (((end==-1)?bgBound.right:(float)end/max*bgBound.width())+1);
        Log.d(logTag,"Set start pixel and end pixel: regionStartMs="+start+", seekBarStartPosition="+seekBarStartPosition+", regionEndMs="+end+", seekBarEndPosition="+seekBarEndPosition);
       
        Log.d(logTag,"Create forground rec: width="+(seekBarEndPosition-seekBarStartPosition)+", height="+fgBound.bottom);
        //fgBmap=getNinepatch(R.drawable.scrubber_primary_holo, seekBarEndPosition-seekBarStartPosition,fgBound.bottom, activity);
        Bitmap fgBmap=getNinepatch(R.drawable.scrubber_primary_segment_mode, bgBound.width(), bgBound.height(), context);
        Log.d(logTag,"Create background rec: width="+bgBound.right+", height="+bgBound.bottom);
        Bitmap bgBmap=getNinepatch(R.drawable.scrubber_track_holo_dark, bgBound.width(), bgBound.height(), context);
       
        
        
        
        //fgBmap.setDensity()
//      Canvas fgCanvas=new Canvas(fgBmap);
        Canvas bgCanvas=new Canvas(bgBmap);

       
        Log.d(logTag,"Copy forground rect to background: x1="+seekBarStartPosition+", y1="+ bgBound.top+", x2="+ seekBarEndPosition+", y2="+ bgBound.bottom);
        //Rect src = new Rect(0, 0, fgBmap.getWidth(), fgBmap.getHeight());
//      int len=seekBarEndPosition-seekBarStartPosition;
//      int h=fgBound.height();
//      int[] colors=new int[len*h];
       
//      fgBmap.getPixels(colors, 0, len, seekBarStartPosition, 0, len, h);
//            getPixels (int[] pixels, int offset, int stride, int x, int y, int width, int height)
//      bgBmap.setPixels(colors, 0, len, seekBarStartPosition, 0, len, h);
        Rect src = new Rect(seekBarStartPosition, 0, seekBarEndPosition, fgBound.bottom);
        Rect dst = new Rect(seekBarStartPosition, 0, seekBarEndPosition, fgBound.bottom);

//        canvas.drawBitmap(fgBmap, dst, dst, null);
//        canvas.drawBitmap(bgBmap, dst, dst, null);
         
        
        //bgCanvas.drawBitmap(fgBmap, src, dst, new Paint());
//      bgCanvas.setDensity(Bitmap.DENSITY_NONE);
//      fgBmap.setDensity(Bitmap.DENSITY_NONE);
        bgCanvas.drawBitmap(fgBmap, dst, dst, null);
        
        
        
        Drawable drawable = new BitmapDrawable(context.getResources(), bgBmap);
        ClipDrawable progress = new ClipDrawable(drawable, Gravity.AXIS_PULL_BEFORE, ClipDrawable.HORIZONTAL);
        InsetDrawable background=  new InsetDrawable(drawable,0);
       
        progress.setBounds(fgBound);
        background.setBounds(bgBound);
       
 /*       layer.setId(0, android.R.id.background);
        layer.setId(1, android.R.id.progress);
        
        layer.setDrawableByLayerId(0, drawable);
        layer.setDrawableByLayerId(1, drawable);
       invalidate();
       */
        
        LayerDrawable mylayer = new LayerDrawable(new Drawable[]{background,progress});
        mylayer.setId(0, android.R.id.background);
        mylayer.setId(1, android.R.id.progress);
        setProgressDrawable(mylayer);
        drawRegionTimes++;
        
        
        //progress.setBounds(fgBound);
        //background.setBounds(bgBound);
        //layer.setDrawableByLayerId(android.R.id.background, background);
        //layer.setDrawableByLayerId(android.R.id.progress, progress);
//      postInvalidate();
   /*     this.postDelayed(new Runnable(){

			@Override
			public void run() {
				int value=getProgress();
		        setProgress(0);
		        setProgress(value);
			}}, 100);
  */      

	}
	
	protected void drawNormalMode(Canvas canvas){
		Log.d(logTag,"Release the region select mode.");
        Log.d(logTag,"Region start = "+start+", region end = "+end);
        LayerDrawable layer = (LayerDrawable) getProgressDrawable();
        Drawable drawableFg = (Drawable)layer.findDrawableByLayerId(android.R.id.progress);
        Drawable drawableBg = (Drawable)layer.findDrawableByLayerId(android.R.id.background);
       
//        drawableBg.draw(canvas);
 //       drawableFg.draw(canvas);

        Log.d(logTag,"There are "+layer.getNumberOfLayers()+" layer in SeekBar object, forground: "+drawableFg+", background: "+drawableBg);
       
        Rect fgBound=drawableFg.copyBounds();
        Rect bgBound=drawableBg.copyBounds();
       
        
        Bitmap seekBarFgBmap = getNinepatch(R.drawable.scrubber_primary_holo, fgBound.width(), fgBound.height(), context);
        
       
        
        BitmapDrawable fgDrawable = new BitmapDrawable(context.getResources(), seekBarFgBmap);
        ClipDrawable progress = new ClipDrawable(fgDrawable, Gravity.AXIS_PULL_BEFORE, ClipDrawable.HORIZONTAL);
        progress.setBounds(fgBound);
        layer.setDrawableByLayerId(android.R.id.progress, progress);
       
        Bitmap seekBarBgBmap = getNinepatch(R.drawable.scrubber_track_holo_dark, bgBound.width(), bgBound.height(), context);
        BitmapDrawable bgDrawable = new BitmapDrawable(context.getResources(), seekBarBgBmap);
        InsetDrawable background=  new InsetDrawable(bgDrawable,0);
        background.setBounds(bgBound);
        layer.setDrawableByLayerId(android.R.id.background, background);
       
 /*       postInvalidate();
        int value=getProgress();
        setProgress(0);
        setProgress(value);
 */       
/*        this.postDelayed(new Runnable(){

			@Override
			public void run() {
				postInvalidate();
		        int value=getProgress();
		        setProgress(0);
		        setProgress(value);
			}}, 100);
 */     

	}
	
	public static Bitmap getNinepatch(int id,int x, int y, Context context){
		//id is a resource id for a valid ninepatch

		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
		byte[] chunk = bitmap.getNinePatchChunk();
		//NinePatchDrawable np_drawable = new NinePatchDrawable(bitmap, chunk, new Rect(), null);
		NinePatchDrawable np_drawable = new NinePatchDrawable(context.getResources(),bitmap, chunk, new Rect(), null);
		np_drawable.setBounds(0, 0,x, y);

		Bitmap output_bitmap = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(output_bitmap);
		np_drawable.draw(canvas);

		return output_bitmap;
	}
}
