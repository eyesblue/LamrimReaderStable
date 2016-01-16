package eyes.blue.modified;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class MyViewPager extends ViewPager {
	Context context=null;
	ViewPagerController controller=null;
	public MyViewPager(Context context) {
		super(context);
		this.context=context;
	}

	public MyViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context=context;
    }
	
	public void setController(ViewPagerController controller){
		this.controller=controller;
	}
	
/*	@Override
	public boolean canScrollHorizontally(int direction){
		Log.d(getClass().getName(),"canScrollHorizontally been call in MyViewPager.");
		return controller.canScrollHorizontally(direction);
	}
*/	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final View v=this.getChildAt(this.getCurrentItem());
		GestureDetector gestureListener=new GestureDetector(context ,new android.view.GestureDetector.SimpleOnGestureListener(){
			@Override
			public boolean 	onDown(MotionEvent e){
				boolean res=e.getPointerCount()> 1;
				Log.d(getClass().getName(),"My ViewPager return "+res+" in onDown.");
				if(res)return false;
				return true;
			}
			@Override
			public boolean 	onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){return true;}
			@Override
			public boolean 	onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
				Log.d(getClass().getName(),"Into onScroll, distance("+distanceX+", "+distanceY+"), scroll point=("+getScrollX()+", "+getScrollY()+")");
				boolean res=v.canScrollHorizontally((int) distanceX);
				Log.d(getClass().getName(),"TheoryPageView return canScrollHorizontally "+res+" return "+(!res));
				
				return !res;
			}
		});
		//if(gestureListener.onTouchEvent(event))
			return super.onTouchEvent(event);
		//return false;
	}
}
