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
	// ��ʼ���ŵ�ֵ
	private float mInitScale;
	// ˫���Ŵ��ֵ
	private float mMidScale;
	// �Ŵ�ļ���
	private float mMaxScale;

	private Matrix mScaleMatrix;
	// �����㴥��ʱ���ŵı���
	private ScaleGestureDetector mScaleGestureDetector;

	// --�����ƶ�

	// ��¼��һ�ζ���ƶ�������(��ָ�����ı� ����λ�����ĵ�ı� )
	private int mLastPointerCount;

	private float mLastX;
	private float mLastY;
	
	private int mTouchSlop;
	
	private boolean isCanDrag;
	
	private boolean isCheckLeftAndRight;
	private boolean isCheckTopAndBottom;
	
	//--˫���Ŵ�����С
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
		//˫����������
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
	 * @author ����
	 *	�Զ��Ŵ�����С
	 */
	private class AutoScaleRunnable implements Runnable{
		//���ŵ�Ŀ��ֵ
		private float mTargetScale;
		//���ŵ����ĵ�
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
			//��������
			mScaleMatrix.postScale(tmpScale, tmpScale,x,y);
			checkBorderAndCenterWhenScale();
			setImageMatrix(mScaleMatrix);
			float currentScale =getScale(); 
			if ((tmpScale>1.0f&& currentScale<mTargetScale)||(tmpScale<1.0f&&currentScale>mTargetScale)) {
				postDelayed(this,16);
			}else//����Ŀ��ֵ
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
	 * ��ȡImageView������ɵ�ͼƬ��С
	 */
	public void onGlobalLayout() {

		if (!mOnce) {
			// �ؼ��Ŀ��
			int width = getWidth();
			int height = getHeight();

			// �õ�ͼƬ�Ŀ��
			Drawable d = getDrawable();
			if (d == null) {
				return;
			}
			int dw = d.getIntrinsicWidth();
			int dh = d.getIntrinsicHeight();
			float scale = 1.0f;
			/**
			 * ���ͼƬ�Ŀ�ȴ��ڿؼ��Ŀ�ȣ����Ǹ߶�С�ڿؼ��ĸ߶ȣ���С
			 * 
			 */
			if (dw > width && dh < height) {
				scale = width * 1.0f / dw;
			}
			/**
			 * ���ͼƬ�ĸ߶ȴ��ڿؼ��ĸ߶ȣ����ǿ��С�ڿؼ��Ŀ�ȣ���С
			 * 
			 */
			if (dh > height && dw < width) {
				scale = height * 1.0f / dh;
			}
			/**
			 * �����С�� ���߶����ڣ������������ؼ��ڲ�ʹ����ȫ��ʾ
			 * 
			 */
			if ((dw > width && dh < height) || (dw < width && dh < height)) {
				scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
			}
			/**
			 * �õ���ʼ�������ű���
			 */
			mInitScale = scale;
			mMaxScale = 4 * mInitScale;
			mMidScale = 2 * mInitScale;

			// ��ͼƬ�ƶ����ؼ�������

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
	 * @return ��ȡ��ǰͼƬ������ֵ
	 */
	public float getScale() {
		float[] values = new float[9];
		mScaleMatrix.getValues(values);
		return values[Matrix.MSCALE_X];
	}

	// �������� ��initScale maxScale
	public boolean onScale(ScaleGestureDetector detector) {

		float scale = getScale();
		float scaleFactor = detector.getScaleFactor();

		if (getDrawable() == null) {
			return true;
		}

		// ���ŷ�Χ�Ŀ���

		if ((scale < mMaxScale && scaleFactor > 1.0f)
				|| (scale > mInitScale && scaleFactor < 1.0f)) {
			if (scale * scaleFactor < mInitScale) {
				scaleFactor = mInitScale / scale;
			}
			if (scale * scaleFactor > mMaxScale) {
				scale = mMaxScale / scale;
			}
			// ����
			mScaleMatrix.postScale(scaleFactor, scaleFactor,
					detector.getFocusX(), detector.getFocusY());

			checkBorderAndCenterWhenScale();

			setImageMatrix(mScaleMatrix);
		}

		return true;
	}

	/**
	 * 
	 * @return ���ͼƬ�Ŵ����С�Ժ�Ŀ�ߣ��Լ�l,r,t,b
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
	 * �����ŵ�ʱ����б߽���ƺ�λ�ÿ���
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
		// �����Ȼ��߸߶�С�ڿؼ��Ŀ��ߣ��������
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

		// �õ���㴥�ص�����

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
					//������С�ڿؼ���ȣ�����������ƶ�
					//boolean s = rectsF.width()<=getWidth();
					//Log.i("---zhoujg77", "--"+s);
					isCheckLeftAndRight=isCheckTopAndBottom = true;
					
					if (rectsF.width()<=getWidth()) {
						isCheckLeftAndRight = false;
						dx = 0;
					}
					//����߶�С�ڿؼ��߶ȣ������������ƶ�
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
	 *�ƶ�ʽ���б߽��� 
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
	 * �ж��Ƿ���MOVE
	 * @param dx
	 * @param dy
	 * @return
	 */
	private boolean isMoveAciton(float dx, float dy) {
		return (Math.sqrt(dx*dx+dy*dy))>mTouchSlop;
	}

}
