package com.szl.zhaozhao2.view.pulltorefresh;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.Scroller;
import android.widget.TextView;

import com.szl.zhaozhao2.R;


/**
 * 下拉刷新控件，目前支持内嵌ListView
 * 
 * @author nieyu
 */
public class PullDownView extends FrameLayout implements OnGestureListener,
		AnimationListener {
	public static SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat(
			"MM-dd HH:mm");
	/**
	 * 关闭状态，即未下拉的初始状态
	 */
	public static final int STATE_CLOSE = 1;
	/**
	 * 下拉打开状态，未拉动到可以刷新的位置
	 */
	public static final int STATE_OPEN = 2;
	/**
	 * 下拉打开释放状态，未拉动到可以刷新的位置下松开手指。
	 */
	public static final int STATE_OPEN_RELEASE = 3;
	/**
	 * 下拉打开超过刷新位置状态，拉动超过可以刷新的位置
	 */
	public static final int STATE_OPEN_MAX = 4;
	/**
	 * 下拉打开释放状态，拉动超过可以刷新的位置下松开手指。
	 */
	public static final int STATE_OPEN_MAX_RELEASE = 5;
	/**
	 * 更新状态，STATE_OPEN_MAX_RELEASE状态之后转入这个状态
	 */
	public static final int STATE_UPDATE = 6;
	/**
	 * 更新后继续滚动状态，STATE_UPDATE下继续上下滚动进入这个状态
	 */
	public static final int STATE_UPDATE_SCROLL = 7;
	/**
	 * 箭头的方向,向上
	 */
	public static final int ARROW_DIRECTION_UP = 1;
	/**
	 * 箭头的方向,向下
	 */
	public static final int ARROW_DIRECTION_DOWN = 2;
	/**
	 * 下来最大高度，根据这个值判断是否可以刷新
	 */
	public static int UPDATE_LENGHT;

	/**
	 * 下拉刷新条最大高度，包括广告高度
	 */
	public int mMaxHeight;

	private String dropDownString;
	private String releaseUpdateString;
	private String doingUpdateString;

	protected ImageView mArrow;
	private ProgressBar mProgressBar;
	protected FrameLayout mUpdateContent;
	protected TextView mTitle;
	private GestureDetector mDetector;
	private Animation mAnimationUp;
	private Animation mAnimationDown;
	private boolean mIsAutoScroller;
	private View mAdView;
	protected Flinger mFlinger = new Flinger();
	/**
	 * 当前的下拉位移
	 */
	protected int mPading;
	/**
	 * 当前的状态，初始值为1
	 */
	protected int mState = STATE_CLOSE;

	/**
	 * 更新事件监听器
	 */
	protected UpdateHandle mUpdateHandle;
	/**
	 * 最后更新时间
	 */
	private Date mDate;
	protected View vUpdateBar;
	private String mFrom;

	private boolean mEnable = true;

	protected Drawable mUpArrow;
	protected Drawable mDownArrow;
	protected ImageView mDivider;
	private boolean mNeedAd;
	private Bitmap mAdBmp;

	/**
	 * 是否显示时间
	 */
	private boolean mIsShowDate = true;

	/**
	 * 是否显示箭头和加载状态
	 */
	private boolean mIsShowStatusIcon = true;

	private int mArrowDirect = ARROW_DIRECTION_DOWN;

	public void setDropDownString(String dropDownString) {
		this.dropDownString = dropDownString;
	}

	public void setReleaseUpdateString(String releaseUpdateString) {
		this.releaseUpdateString = releaseUpdateString;
	}

	public void setDoingUpdateString(String doingUpdateString) {
		this.doingUpdateString = doingUpdateString;
	}

	public void setShowDate(boolean isShowDate, String from) {
		mFrom = from;
		mIsShowDate = isShowDate;
	}

	public void setShowStatusIcon(boolean isShowStatusIcon) {
		mIsShowStatusIcon = isShowStatusIcon;

		if (!mIsShowStatusIcon) {
			mArrow.setImageDrawable(null);
		}
	}

	public PullDownView(Context context) {
		super(context);
		init();
	}

	private void init() {
		if (isInEditMode()) {
			return;
		}
		initResources();
		addUpdateBar();
	}

	private void initResources() {

		UPDATE_LENGHT = getResources().getDimensionPixelSize(
				R.dimen.updatebar_height);
		mMaxHeight = UPDATE_LENGHT;
		setDrawingCacheEnabled(false);
		setBackgroundDrawable(null);
		setClipChildren(false);
		mDetector = new GestureDetector(this);
		mDetector.setIsLongpressEnabled(true);
		mUpArrow = getResources().getDrawable(R.drawable.ctl_z_arrow_up);
		mDownArrow = getResources().getDrawable(R.drawable.ctl_z_arrow_down);
		dropDownString = getResources().getString(R.string.drop_dowm);
		releaseUpdateString = getResources().getString(R.string.release_update);
		doingUpdateString = getResources().getString(R.string.doing_update);
	}

	public PullDownView(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.PullDownView);
		if (a != null) {
			boolean needAD = a.getBoolean(R.styleable.PullDownView_needAD,
					false);
			needAD(needAD);
		}
		init();
	}

	protected View getContentView() {
		return getChildAt(1);
	}

	public void needAD(boolean needAD) {
		mNeedAd = needAD;
	}

	public void setUpdateHandle(UpdateHandle handle) {
		mUpdateHandle = handle;
	}

	public static void onGMTChange() {
		DISPLAY_DATE_FORMAT = new SimpleDateFormat("MM-dd HH:mm");
	}

	public void initSkin() {
		mUpArrow = getResources().getDrawable(R.drawable.ctl_z_arrow_up);
		mDownArrow = getResources().getDrawable(R.drawable.ctl_z_arrow_down);
		mArrow.setImageDrawable(mIsShowStatusIcon ? mDownArrow : null);
		if (mDivider == null) {
			mDivider = (ImageView) findViewById(R.id.vw_update_divider_id);
		}
		mDivider.setImageDrawable(getResources().getDrawable(
				R.drawable.ctl_divider_horizontal_timeline));

		mTitle.setTextColor(getResources().getColor(
				R.color.pull_down_text_color));
	}

	/**
	 * 添加下拉更新条和更新条中的view
	 */
	protected void addUpdateBar() {
		mAnimationUp = AnimationUtils.loadAnimation(getContext(),
				R.anim.anim_rotate_up);
		mAnimationUp.setFillAfter(true);
		mAnimationUp.setFillBefore(false);
		mAnimationUp.setAnimationListener(this);

		mAnimationDown = AnimationUtils.loadAnimation(getContext(),
				R.anim.anim_rotate_down);
		mAnimationDown.setFillAfter(true);
		mAnimationDown.setFillBefore(false);
		mAnimationDown.setAnimationListener(this);

		vUpdateBar = LayoutInflater.from(getContext()).inflate(
				R.layout.ctl_update_bar, null);
		vUpdateBar.setVisibility(View.GONE);
		addView(vUpdateBar);
		mArrow = new ImageView(getContext());
		LayoutParams lp = new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);
		mArrow.setScaleType(ScaleType.FIT_CENTER);
		mArrow.setLayoutParams(lp);
		mArrow.setImageDrawable(mIsShowStatusIcon ? mDownArrow : null);
		mUpdateContent = ((FrameLayout) vUpdateBar
				.findViewById(R.id.iv_content));
		mUpdateContent.addView(mArrow);
		LayoutParams lp1 = new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		lp1.gravity = Gravity.CENTER;

		mProgressBar = new ProgressBar(getContext());
		mProgressBar.setIndeterminate(true);
		mProgressBar.setIndeterminateDrawable(getResources().getDrawable(
				R.drawable.progressbar));
		mProgressBar.setLayoutParams(lp1);
		mUpdateContent.addView(mProgressBar);

		mTitle = (TextView) findViewById(R.id.tv_title);
		mAdView = findViewById(R.id.pulldown_ad);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {

		if (!mEnable) {
			return super.dispatchTouchEvent(event);
		}
		if (mIsAutoScroller) {
			return true;
		}
		// 点击事件交由手势监听器处理
		boolean retValue = mDetector.onTouchEvent(event);

		int action = event.getAction();
		// mDetector没有关于action_up的处理，特殊添加，up的时候根据状态不同做不同的处理
		if (action == MotionEvent.ACTION_UP) {
			retValue = release();
		} else if (action == MotionEvent.ACTION_CANCEL) {
			retValue = release();
		}
		// 在STATE_UPDATE，和STATE_UPDATE_SCROLL状态下本控件不处理事件，全部交给listview处理
		if (mState == STATE_UPDATE || mState == STATE_UPDATE_SCROLL) {
			updateView();
			return super.dispatchTouchEvent(event);
		}
		if ((retValue || mState == STATE_OPEN || mState == STATE_OPEN_MAX
				|| mState == STATE_OPEN_MAX_RELEASE || mState == STATE_OPEN_RELEASE)
				&& getContentView().getTop() != 0) {
			event.setAction(MotionEvent.ACTION_CANCEL);// 当处于上面几种状态时，需要给listview发一个cancel的touch事件来防止listview出现长按操作
			super.dispatchTouchEvent(event);
			updateView();
			return true;
		} else {
			updateView();
			return super.dispatchTouchEvent(event);
		}
	}

	private boolean release() {
		if (mPading >= 0) {
			return false;
		}
		switch (mState) {
		case STATE_OPEN:
		case STATE_OPEN_RELEASE:
			if (Math.abs(mPading) < UPDATE_LENGHT) {
				mState = STATE_OPEN_RELEASE;
			}
			scrollToClose();
			break;
		case STATE_OPEN_MAX:
		case STATE_OPEN_MAX_RELEASE:
			mState = STATE_OPEN_MAX_RELEASE;
			scrollToUpdate();
			break;
		default:
			break;
		}
		return true;
	}

	/**
	 * 滚动到更新位置
	 */
	protected void scrollToUpdate() {
		mFlinger.startUsingDistance(-mPading - UPDATE_LENGHT, 300);
	}

	/**
	 * 滚动到关闭位置
	 */
	protected void scrollToClose() {
		mFlinger.startUsingDistance(-mPading, 300);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// 将下拉速度降低到一半
		distanceY = (float) (distanceY * 0.5);
		AdapterView v = (AdapterView) getContentView();
		if (v == null || v.getCount() == 0 || v.getChildCount() == 0) {
			return false;
		}
		boolean isStart = v.getFirstVisiblePosition() == 0;
		if (isStart) {
			isStart = v.getChildAt(0).getTop() == 0;
		}
		// isStart用来判断listView是不是滚动到最上位置，如果listview在最上位置，继续下拉则响应下拉事件，否则将事件交给listview自己处理
		if ((distanceY < 0 && isStart) || mPading < 0) {
			boolean r = move(distanceY, true);
			return r;
		} else {
			return false;
		}
	}

	/**
	 * 下拉指定像素距离
	 * 
	 * @param distanceY
	 *            ，下拉距离
	 * @param isScroll
	 *            ，是否是用户手指触发的滚动，还是自动滚回发生的滚动
	 * @return
	 */
	private boolean move(float distanceY, boolean isScroll) {
		// 更新状态下，不允许继续下拉，但可以向上滚动进入STATE_UPDATE_SCROLL状态
		if (mState == STATE_UPDATE) {
			if (distanceY < 0) {
				return true;
			} else if (isScroll == true) {
				mState = STATE_UPDATE_SCROLL;
			}
		}
		// STATE_UPDATE_SCROLL状态下，下拉超过MAX_LENGHT，不允许继续下拉
		if (mState == STATE_UPDATE_SCROLL && distanceY < 0
				&& -mPading >= UPDATE_LENGHT) {
			return true;
		}
		// 累加下拉距离，mPading始终未负值
		mPading += distanceY;
		if (mPading > 0) {
			mPading = 0;
		}
		if (!isScroll) {
			if (mState == STATE_OPEN_MAX_RELEASE) {// 自动滚动情况下，STATE_OPEN_MAX_RELEASE自动滚动进入STATE_UPDATE状态，通知更新监听器
				mState = STATE_UPDATE;
				updateHandler();
			} else if (mState == STATE_UPDATE && mPading == 0) {
				mState = STATE_CLOSE;
			} else if (mState == STATE_OPEN_RELEASE && mPading == 0) {
				mState = STATE_CLOSE;
			} else if (mState == STATE_UPDATE_SCROLL && mPading == 0) {
				mState = STATE_CLOSE;
			}
			invalidate();
			return true;
		}
		// 更具不同的状态和mapding值来切换状态，并通知界面重绘
		switch (mState) {
		case STATE_CLOSE:
			if (mPading < 0) {
				mState = STATE_OPEN;
				mProgressBar.setVisibility(View.GONE);
				mArrow.setVisibility(View.VISIBLE);
			}
			break;
		case STATE_OPEN:
			if (Math.abs(mPading) >= UPDATE_LENGHT) {
				mState = STATE_OPEN_MAX;
				mProgressBar.setVisibility(View.GONE);
				mArrow.setVisibility(View.VISIBLE);
				makeArrowUp();
			} else if (mPading == 0) {
				mState = STATE_CLOSE;
			}
			break;
		case STATE_OPEN_MAX:
			if (Math.abs(mPading) < UPDATE_LENGHT) {
				mState = STATE_OPEN;
				mProgressBar.setVisibility(View.GONE);
				mArrow.setVisibility(View.VISIBLE);
				makeArrowDown();
			}
			break;
		case STATE_OPEN_RELEASE:
		case STATE_OPEN_MAX_RELEASE:
			if (isScroll) {
				if (Math.abs(mPading) >= UPDATE_LENGHT) {
					mState = STATE_OPEN_MAX;
					mProgressBar.setVisibility(View.GONE);
					mArrow.setVisibility(View.VISIBLE);
					makeArrowUp();
				} else if (Math.abs(mPading) < UPDATE_LENGHT) {
					mState = STATE_OPEN;
					mProgressBar.setVisibility(View.GONE);
					mArrow.setVisibility(View.VISIBLE);
					makeArrowDown();
				} else if (mPading == 0) {
					mState = STATE_CLOSE;
				}
			} else {
				if (mPading == 0) {
					mState = STATE_CLOSE;
				}
			}
			invalidate();
			return true;
		case STATE_UPDATE:
			if (mPading == 0) {
				mState = STATE_CLOSE;
			}
			invalidate();
			return true;
		default:
			break;
		}
		return true;
	}

	protected void updateHandler() {
		if (mUpdateHandle != null) {
			mUpdateHandle.onUpdate();
		}
	}

	/**
	 * 更新view状态
	 */
	protected void updateView() {
		View updateBar = vUpdateBar;
		View content = getContentView();
		long date = 0;
		if (date == 0) {
			mDate = new Date();
		} else {
			mDate = new Date(date);
		}
		// 根据不同的状态绘制界面
		switch (mState) {
		case STATE_CLOSE:// close状态不显示更新条
			if (updateBar.getVisibility() != View.GONE) {
				updateBar.setVisibility(View.GONE);
			}
			content.offsetTopAndBottom(-content.getTop());
			break;
		case STATE_OPEN:
		case STATE_OPEN_RELEASE:
			int l = content.getTop();
			content.offsetTopAndBottom(-mPading - l);
			if (updateBar.getVisibility() != View.VISIBLE) {
				updateBar.setVisibility(View.VISIBLE);
			}
			int ul = updateBar.getTop();
			updateBar.offsetTopAndBottom(-mMaxHeight - mPading - ul);// 根据下拉的位移来确定updateBarde
																		// 位置

			String dropDownTitleText = dropDownString;
			if (mIsShowDate) {
				dropDownTitleText += "\n"
						+ getContext().getString(R.string.update_time) + ":"
						+ DISPLAY_DATE_FORMAT.format(mDate);
			}
			mTitle.setText(dropDownTitleText);
			break;
		case STATE_OPEN_MAX:
		case STATE_OPEN_MAX_RELEASE:
			l = content.getTop();
			content.offsetTopAndBottom(-mPading - l);
			if (updateBar.getVisibility() != View.VISIBLE) {
				updateBar.setVisibility(View.VISIBLE);
			}
			ul = updateBar.getTop();
			updateBar.offsetTopAndBottom(-mMaxHeight - mPading - ul);

			String releaseTitleText = releaseUpdateString;
			if (mIsShowDate) {
				releaseTitleText += "\n"
						+ getContext().getString(R.string.update_time) + ":"
						+ DISPLAY_DATE_FORMAT.format(mDate);
			}
			mTitle.setText(releaseTitleText);
			break;
		case STATE_UPDATE:
		case STATE_UPDATE_SCROLL:
			l = content.getTop();
			content.offsetTopAndBottom(-mPading - l);
			ul = updateBar.getTop();
			if (mProgressBar.getVisibility() != View.VISIBLE
					&& mIsShowStatusIcon) {
				mProgressBar.setVisibility(View.VISIBLE);
			}
			if (mArrow.getVisibility() != View.GONE) {
				mArrow.setVisibility(View.GONE);
			}

			String doingTitleText = doingUpdateString;
			if (mIsShowDate) {
				doingTitleText += "\n"
						+ getContext().getString(R.string.update_time) + ":"
						+ DISPLAY_DATE_FORMAT.format(mDate);
			}
			mTitle.setText(doingTitleText);

			updateBar.offsetTopAndBottom(-mMaxHeight - mPading - ul);
			if (updateBar.getVisibility() != View.VISIBLE) {
				updateBar.setVisibility(View.VISIBLE);
			}
			break;
		default:
			break;
		}
		invalidate();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		if (isInEditMode()) {
			return;
		}
		vUpdateBar.layout(0, -mMaxHeight - mPading, getMeasuredWidth(),
				-mPading);
		// vUpdateBar.layout(0, 0, getMeasuredWidth(), - mPading);
		getContentView().layout(0, -mPading, getMeasuredWidth(),
				getMeasuredHeight() - mPading);
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	/**
	 * 自动滚回时使用
	 * 
	 * @author nieyu
	 */
	class Flinger implements Runnable {
		private Scroller mScroller;
		private int mLastFlingX;
		private boolean mIsStart;

		public Flinger() {
			mScroller = new Scroller(getContext());
		}

		private void startCommon() {
			removeCallbacks(this);
		}

		/**
		 * 在制定的时间内滚动制定距离
		 * 
		 * @param distance
		 *            滚动距离
		 * @param dur
		 *            滚动时间
		 */
		public void startUsingDistance(int distance, int dur) {
			if (distance == 0)
				distance--;
			// 初始化参数
			startCommon();
			mLastFlingX = 0;
			// 使用Scroller计算滚动的时间和位移
			mScroller.startScroll(0, 0, -distance, 0, dur);
			mIsAutoScroller = true;
			post(this);
		}

		@Override
		public void run() {

			final Scroller scroller = mScroller;
			boolean more = scroller.computeScrollOffset();
			final int x = scroller.getCurrX();
			// scroller计算本次需要滚动位移
			int delta = mLastFlingX - x;

			move(delta, false);
			updateView();
			if (more) {
				mLastFlingX = x;
				post(this);
			} else {
				mIsAutoScroller = false;
				removeCallbacks(this);
			}
		}
	}

	public void setUpdateDate(Date date) {
		mDate = date;
	}

	/**
	 * 结束更新
	 * 
	 * @param date
	 *            更新时间
	 */
	public void endUpdate() {
		// System.out.println("PullDownView.java.endUpdate " + "date = [" + date
		// + "]");
		if (mPading != 0) {
			scrollToClose();
		} else {
			mState = STATE_CLOSE;
		}
		mArrow.clearAnimation();
		mArrow.setImageDrawable(mIsShowStatusIcon ? mDownArrow : null);
		mArrowDirect = ARROW_DIRECTION_DOWN;
	}

	public interface UpdateHandle {
		public void onUpdate();
	}

	/**
	 * 设置状态为正在刷新
	 */
	public void update() {
		mPading = -UPDATE_LENGHT;
		mState = STATE_UPDATE_SCROLL;
		postDelayed(new Runnable() {

			@Override
			public void run() {
				updateView();
			}
		}, 10);
	}

	public void updateWithoutOffset() {
		mState = STATE_UPDATE_SCROLL;
		invalidate();
	}

	public void setEnable(boolean enable) {
		mEnable = enable;
		invalidate();
	}

	public boolean isEnable() {
		return mEnable;
	}

	public void onConfigChange() {
		if (mNeedAd && mAdBmp != null) {
			Bitmap bitmap = mAdBmp;
			if (bitmap != null) {
				int height = bitmap.getHeight();
				int width = bitmap.getWidth();
				DisplayMetrics dm = getResources().getDisplayMetrics();
				int screenWidth = dm.widthPixels;
				// 需要修改成等比缩放后的高度
				height = height * screenWidth / width;
				mMaxHeight = height;

				BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
				bitmapDrawable.setBounds(0, 0, dm.widthPixels, mMaxHeight);
				mAdView.setBackgroundDrawable(bitmapDrawable);
			}
		}
	}

	private void makeArrowUp() {
		if (mArrowDirect == ARROW_DIRECTION_UP) {
			return;
		}
		mArrow.startAnimation(mAnimationUp);
		mArrowDirect = ARROW_DIRECTION_UP;
	}

	private void makeArrowDown() {
		if (mArrowDirect == ARROW_DIRECTION_DOWN) {
			return;
		}
		mArrow.startAnimation(mAnimationDown);
		mArrowDirect = ARROW_DIRECTION_DOWN;
	}

	@Override
	public void onAnimationStart(Animation animation) {
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		if (mArrowDirect == ARROW_DIRECTION_UP) {
			getHandler().postDelayed(new Runnable() {
				@Override
				public void run() {
					mArrow.clearAnimation();
					mArrow.setImageDrawable(mIsShowStatusIcon ? mUpArrow : null);
				}
			}, 0);

		} else if (mArrowDirect == ARROW_DIRECTION_DOWN) {
			getHandler().postDelayed(new Runnable() {
				@Override
				public void run() {
					mArrow.clearAnimation();
					mArrow.setImageDrawable(mIsShowStatusIcon ? mDownArrow
							: null);
				}
			}, 0);
		}
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
	}
}
