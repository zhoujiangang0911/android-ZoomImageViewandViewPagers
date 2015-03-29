package com.zhoujg77.zoomimageview;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MainActivity extends Activity {
	private ViewPager mViewPager;
	private int Images[] = new int[]{
			R.drawable.a,R.drawable.b,R.drawable.ic_launcher
	};
	private ImageView [] mImageViews = new ImageView[Images.length];
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewpager);
		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		mViewPager.setAdapter(new PagerAdapter() {
			
			@Override
			public boolean isViewFromObject(View arg0, Object arg1) {

				return arg0==arg1;
			}
			
			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				ZoomImageView imageView = new ZoomImageView(getApplicationContext());
				imageView.setImageResource(Images[position]);
				container.addView(imageView);
				mImageViews[position] = imageView;
				return imageView;
				
			}
			
			@Override
			public void destroyItem(ViewGroup container, int position,
					Object object) {
				container.removeView(mImageViews[position]);
			}
			@Override
			public int getCount() {
				return mImageViews.length;
			}
		});
		
	}

	
}
