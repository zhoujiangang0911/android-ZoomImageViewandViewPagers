package com.zhoujg77.zoomimageview;

import android.content.Context;
import android.gesture.Gesture;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.support.v4.view.ViewConfigurationCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.view.View.OnTouchListener;

;

public class ZoomImageView extends ImageView implements OnGlobalLayoutListener,
		OnScaleGestureListener, OnTouchListener {

	private boolean mOnce;
	// 初始缩放的值
	private float mInitScale;
	// 双击放大的值
	private float mMidScale;
	// 放大的极限
	private float mMaxScale;

	private Matrix mScaleMatrix;
	// 捕获多点触控时缩放的比例
	private ScaleGestureDetector mScaleGestureDetector;

	// --自由移动

	// 记录上一次多点移动的数量(手指数量改变 缩放位置中心点改变 )
	private int mLastPointerCount;

	private float mLastX;
	private float mLastY;
	
	private int mTouchSlop;
	
	private boolean isCanDrag;
	
	private boolean isCheckLeftAndRight;
	private boolean isCheckTopAndBottom;
	
	//--双击放大与缩小
	private GestureDetector mGestureDetector;
	private boolean isAutoScale;
	
	
	public ZoomImageView(Context context) {
		this(context, null);
		mScaleMatrix = new Matrix();

	}

	public ZoomImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ZoomImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mScaleMatrix = new Matrix();
		super.setScaleType(ScaleType.MATRIX);
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		setOnTouchListener(this);
		mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop(); 
		//双击方法监听
		mGestureDetector = new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
				@Override
				public boolean onDoubleTap(MotionEvent e) {
					float x =e.getX();	
					float y =e.getY();	
					if (isAutoScale) {
						return true;
					}
					
					if (getScale()<mMidScale) {
//						mScaleMatrix.postScale(mMidScale/getScale(), mMidScale/getScale(),x,y);
//						setImageMatrix(mScaleMatrix);
						postDelayed(new AutoScaleRunnable(mMidScale, x, y), 16);
						isAutoScale = true;
					}else{
//						mScaleMatrix.postScale(mInitScale/getScale(), mInitScale/getScale(),x,y);
//						setImageMatrix(mScaleMatrix);
						postDelayed(new AutoScaleRunnable(mInitScale, x, y), 16);
						isAutoScale = true;
					}
				
					return true;
				};
		} );
		
	}
	
	/**
	 * @author 建刚
	 *	自动放大与缩小
	 */
	private class AutoScaleRunnable implements Runnable{
		//缩放的目标值
		private float mTargetScale;
		//缩放的中心点
		private float x;
		private float y;
		
		private final float BIGGER = 1.02f;
		private final float SMALL = 0.98f;
		
		private float tmpScale;
		
		public AutoScaleRunnable(float mTargetScale, float x, float y) {
			this.mTargetScale = mTargetScale;
			this.x = x;
			this.y = y;
			
			if (getScale()<mTargetScale) {
				tmpScale = BIGGER;
			}
			if (getScale()>mTargetScale) {
				tmpScale = SMALL;
			}
			
		}
		public void run() {
			//进行缩放
			mScaleMatrix.postScale(tmpScale, tmpScale,x,y);
			checkBorderAndCenterWhenScale();
			setImageMatrix(mScaleMatrix);
			float currentScale =getScale(); 
			if ((tmpScale>1.0f&& currentScale<mTargetScale)||(tmpScale<1.0f&&currentScale>mTargetScale)) {
				postDelayed(this,16);
			}else//设置目标值
			{ 
				float scale = mTargetScale/currentScale;
				mScaleMatrix.postScale(scale, scale,x,y);
				checkBorderAndCenterWhenScale();
				setImageMatrix(mScaleMatrix);
				isAutoScale = false;
			}
		}
	}
	
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		getViewTreeObserver().addOnGlobalLayoutListener(this);

	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		getViewTreeObserver().removeGlobalOnLayoutListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.view.ViewTreeObserver.OnGlobalLayoutListener#onGlobalLayout()
	 * 获取ImageView加载完成的图片大小
	 */
	public void onGlobalLayout() {

		if (!mOnce) {
			// 控件的宽高
			int width = getWidth();
			int height = getHeight();

			// 得到图片的宽高
			Drawable d = getDrawable();
			if (d == null) {
				return;
			}
			int dw = d.getIntrinsicWidth();
			int dh = d.getIntrinsicHeight();
			float scale = 1.0f;
			/**
			 * 如果图片的宽度大于控件的宽度，但是高度小于控件的高度，缩小
			 * 
			 */
			if (dw > width && dh < height) {
				scale = width * 1.0f / dw;
			}
			/**
			 * 如果图片的高度大于控件的高度，但是宽度小于控件的宽度，缩小
			 * 
			 */
			if (dh > height && dw < width) {
				scale = height * 1.0f / dh;
			}
			/**
			 * 如果都小于 或者都大于！将其所有至控件内部使其完全显示
			 * 
			 */
			if ((dw > width && dh < height) || (dw < width && dh < height)) {
				scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
			}
			/**
			 * 得到初始化的缩放比例
			 */
			mInitScale = scale;
			mMaxScale = 4 * mInitScale;
			mMidScale = 2 * mInitScale;

			// 将图片移动到控件的中心

			int dx = getWidth() / 2 - dw / 2;
			int dy = getHeight() / 2 - dh / 2;

			mScaleMatrix.postTranslate(dx, dy);
			mScaleMatrix.postScale(mInitScale, mInitScale, width / 2,
					height / 2);
			setImageMatrix(mScaleMatrix);

			mOnce = true;
		}

	}

	/**
	 * @return 获取当前图片的缩放值
	 */
	public float getScale() {
		float[] values = new float[9];
		mScaleMatrix.getValues(values);
		return values[Matrix.MSCALE_X];
	}

	// 缩放区间 ：initScale maxScale
	public boolean onScale(ScaleGestureDetector detector) {

		float scale = getScale();
		float scaleFactor = detector.getScaleFactor();

		if (getDrawable() == null) {
			return true;
		}

		// 缩放范围的控制

		if ((scale < mMaxScale && scaleFactor > 1.0f)
				|| (scale > mInitScale && scaleFactor < 1.0f)) {
			if (scale * scaleFactor < mInitScale) {
				scaleFactor = mInitScale / scale;
			}
			if (scale * scaleFactor > mMaxScale) {
				scale = mMaxScale / scale;
			}
			// 缩放
			mScaleMatrix.postScale(scaleFactor, scaleFactor,
					detector.getFocusX(), detector.getFocusY());

			checkBorderAndCenterWhenScale();

			setImageMatrix(mScaleMatrix);
		}

		return true;
	}

	/**
	 * 
	 * @return 获得图片放大或缩小以后的宽高，以及l,r,t,b
	 * 
	 */
	private RectF getMatrixRect() {
		Matrix matrix = mScaleMatrix;
		RectF rectF = new RectF();
		Drawable d = getDrawable();

		if (d != null) {
			rectF.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
			matrix.mapRect(rectF);
		}

		return rectF;
	}

	/**
	 * 在缩放的时候进行边界控制和位置控制
	 */
	private void checkBorderAndCenterWhenScale() {
		RectF rect = getMatrixRect();
		float deltaX = 0;
		float deltaY = 0;

		int width = getWidth();
		int height = getHeight();

		if (rect.width() >= width) {
			if (rect.left > 0) {
				deltaX = -rect.left;
			}

			if (rect.right < width) {
				deltaX = width - rect.right;
			}

		}

		if (rect.height() >= height) {
			if (rect.top > 0) {
				deltaY = -rect.top;
			}

			if (rect.bottom < height) {
				deltaY = height - rect.bottom;
			}
		}
		// 如果宽度或者高度小于控件的宽或高；让其剧中
		if (rect.width() < width) {
			deltaX = width / 2f - rect.right + rect.width() / 2f;
		}
		if (rect.height() < height) {
			deltaY = height / 2f - rect.bottom + rect.height() / 2f;
		}

		mScaleMatrix.postTranslate(deltaX, deltaY);

	}

	public boolean onScaleBegin(ScaleGestureDetector detector) {
		return true;
	}

	public void onScaleEnd(ScaleGestureDetector detector) {

	}

	public boolean onTouch(View v, MotionEvent event) {
		if (mGestureDetector.onTouchEvent(event)) {
			return true;
		}
		mScaleGestureDetector.onTouchEvent(event);

		float x = 0;
		float y = 0;

		// 拿到多点触控的数量

		int pointCount = event.getPointerCount();
		for (int i = 0; i < pointCount; i++) {
			x += event.getX(i);
			y += event.getY(i);
		}

		x /= pointCount;
		y /= pointCount;

		if (mLastPointerCount != pointCount) {
			isCanDrag = false;
			mLastX = x;
			mLastY = y;
		}
		mLastPointerCount = pointCount;
		RectF rect = getMatrixRect();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (rect.width()>getWidth()+0.01||rect.height()>getHeight()+0.01) {
				if (getParent() instanceof ViewPager){
					getParent().requestDisallowInterceptTouchEvent(true);
					}
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (rect.width()>getWidth()+0.01||rect.height()>getHeight()+0.01) {
					if (getParent() instanceof ViewPager){
					getParent().requestDisallowInterceptTouchEvent(true);
					}
			}
			float dx = x - mLastX;
			float dy = y - mLastY;
			if (!isCanDrag) {
				isCanDrag = isMoveAciton(dx,dy);
			}
			if (isCanDrag) {
				RectF rectsF = getMatrixRect();
				if (getDrawable()!=null) {
					//如果宽度小于控件宽度，不允许横向移动
					//boolean s = rectsF.width()<=getWidth();
					//Log.i("---zhoujg77", "--"+s);
					isCheckLeftAndRight=isCheckTopAndBottom = true;
					
					if (rectsF.width()<=getWidth()) {
						isCheckLeftAndRight = false;
						dx = 0;
					}
					//如果高度小于控件高度，不允许纵向移动
					if (rectsF.height()<=getHeight()) {
						isCheckTopAndBottom = false;
						dy=0;	
					}
					mScaleMatrix.postTranslate(dx, dy);
					
					checkBorderWhenTranslate();
					
					setImageMatrix(mScaleMatrix);
				}
			}
			mLastX =x ;
			mLastY = y;
			break;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
				mLastPointerCount = 0;
			break;

		default:
			break;
		}

		return true;
	}
	
	/**
	 *移动式进行边界检查 
	 */
	private void checkBorderWhenTranslate() {
		RectF rectF = getMatrixRect();
		float deltaX=0;
		float deltaY=0;
		int widght = getWidth();
		int heiget = getHeight();
		if (rectF.top >0 && isCheckTopAndBottom) {
			deltaY = -rectF.top;
		}
		
		if (rectF.bottom <heiget && isCheckTopAndBottom) {
			deltaY = heiget -rectF.bottom;
		}
		
		if (rectF.left >0 && isCheckLeftAndRight) {
			deltaX = -rectF.left;
		}
		if (rectF.right <widght && isCheckLeftAndRight) {
			deltaX = widght-rectF.right;
		}
		mScaleMatrix.postTranslate(deltaX, deltaY);
	}

	/**
	 * 判断是否是MOVE
	 * @param dx
	 * @param dy
	 * @return
	 */
	private boolean isMoveAciton(float dx, float dy) {
		return (Math.sqrt(dx*dx+dy*dy))>mTouchSlop;
	}

}
