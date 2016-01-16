package eyes.blue.modified;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;

public class MyHorizontalScrollView extends HorizontalScrollView {
	Context context;
	public MyHorizontalScrollView(Context context) {super(context);this.context=context;}
	public MyHorizontalScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context=context;
	}
	public MyHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) 
	{
	    super(context, attrs, defStyle);
	    this.context=context;
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev){
		return false;
	}
}
