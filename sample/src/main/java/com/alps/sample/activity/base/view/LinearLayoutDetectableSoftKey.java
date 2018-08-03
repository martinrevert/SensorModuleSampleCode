package com.alps.sample.activity.base.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Display;
import android.widget.LinearLayout;


/**
 * [JP]ソフトウェアキーボードの表示・非表示を検知します。
 */
public class LinearLayoutDetectableSoftKey extends LinearLayout {

	public static final int INT_VIRTUAL_SOFTWARE_KEYBOARD_HEIGHT = 100;
	private static final String TAG = "SoftKeyDetector";

	public LinearLayoutDetectableSoftKey(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * [JP]ソフトウェアキーボードの表示・非表示を通知します。
	 */
	public interface OnSoftKeyShownListener {
		/**
		 * @param isShown trueの場合、ソフトウェアキーボードが表示されていることを示します。
		 *                falseの場合、ソフトウェアキーボードが閉じられたことを示します。
		 *
		 */
		void onSoftKeyShown(boolean isShown);
	}

	private static class ResultHolder {
		private boolean notifiedResult = false;

		public ResultHolder(boolean b) {
			setResult(b);
		}

		public boolean getResult() {
			return notifiedResult;
		}

		public void setResult(boolean b) {
			notifiedResult = b;
		}
	}
	private ResultHolder holder = null;


	private OnSoftKeyShownListener listener;

	public void setOnDetectSoftKeyboard(OnSoftKeyShownListener listener) {
		this.listener = listener;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (listener != null) {
			int viewHeight = MeasureSpec.getSize(heightMeasureSpec);
			Context context = getContext();
			if (context instanceof Activity) {
				Activity activity = (Activity) context;
				Rect rect = new Rect();
				activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
				int statusBarHeight = rect.top;
				Display display = activity.getWindowManager().getDefaultDisplay();
				Point size = new Point();
				display.getSize(size);
				int screenHeight = size.y;
				int diff = (screenHeight - statusBarHeight) - viewHeight;
				boolean result = diff > INT_VIRTUAL_SOFTWARE_KEYBOARD_HEIGHT;

				boolean shouldNotify = false;

				if (holder == null) {
					holder = new ResultHolder(result);
					shouldNotify = true;
				}
				else if (holder.getResult() != result) {
					holder.setResult(result);
					shouldNotify = true;
				}

				if (shouldNotify) {
//					Logg.d(TAG, "(screenHeight:%d - statusBarHeight:%d) - viewHeight:%d = diff:%d > INT_VIRTUAL_SOFTWARE_KEYBOARD_HEIGHT:%d ? %b", screenHeight, statusBarHeight, viewHeight, diff, INT_VIRTUAL_SOFTWARE_KEYBOARD_HEIGHT, result);
					listener.onSoftKeyShown(result);
				}
			}
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
