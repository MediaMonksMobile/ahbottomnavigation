package com.aurelhubert.ahbottomnavigation;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * AHBottomNavigationLayout
 * Material Design guidelines : https://www.google.com/design/spec/components/bottom-navigation.html
 */
public class AHBottomNavigation extends FrameLayout {

	// Constant
	public static final int CURRENT_ITEM_NONE = -1;

	// Static
	private static String TAG = "AHBottomNavigation";
	private static final int MAX_ITEMS = 5;

	// Listener
	private OnTabSelectedListener tabSelectedListener;
	private OnNavigationPositionListener navigationPositionListener;

	// Variables
	private Context context;
	private Resources resources;
	private ArrayList<AHBottomNavigationItem> items = new ArrayList<>();
	private ArrayList<View> views = new ArrayList<>();
	private AHBottomNavigationBehavior<AHBottomNavigation> bottomNavigationBehavior;
	private LinearLayout linearLayoutContainer;
	private View backgroundColorView;
	private Animator circleRevealAnim;
	private boolean colored = false;
	private boolean selectedBackgroundVisible = false;
	private boolean translucentNavigationEnabled;
	private boolean isBehaviorTranslationSet = false;
	private int currentItem = 0;
	private int currentColor = 0;
	private boolean behaviorTranslationEnabled = true;
	private boolean needHideBottomNavigation = false;
	private boolean hideBottomNavigationWithAnimation = false;
	private boolean soundEffectsEnabled = true;

	// Variables (Styles)
	private Typeface titleTypeface;
	private int defaultBackgroundColor = Color.TRANSPARENT;
	private @ColorInt int itemActiveColor;
	private @ColorInt int itemInactiveColor;
	private @ColorInt int titleColorActive;
	private @ColorInt int titleColorInactive;
	private @ColorInt int coloredTitleColorActive;
	private @ColorInt int coloredTitleColorInactive;
	private float titleActiveTextSize, titleInactiveTextSize;
	private int bottomNavigationHeight, navigationBarHeight = 0;
	private boolean forceTint = false;

	/**
	 * Constructors
	 */
	public AHBottomNavigation(Context context) {
		super(context);
		init(context, null);
	}

	public AHBottomNavigation(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public AHBottomNavigation(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	@Override
	public void setSoundEffectsEnabled(final boolean soundEffectsEnabled) {
		super.setSoundEffectsEnabled(soundEffectsEnabled);
		this.soundEffectsEnabled = soundEffectsEnabled;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		createItems();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (!isBehaviorTranslationSet) {
			//The translation behavior has to be set up after the super.onMeasure has been called.
			setBehaviorTranslationEnabled(behaviorTranslationEnabled);
			isBehaviorTranslationSet = true;
		}
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putParcelable("superState", super.onSaveInstanceState());
		bundle.putInt("current_item", currentItem);
		return bundle;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;
			currentItem = bundle.getInt("current_item");
			state = bundle.getParcelable("superState");
		}
		super.onRestoreInstanceState(state);
	}

	/////////////
	// PRIVATE //
	/////////////

	/**
	 * Init
	 *
	 * @param context
	 */
	private void init(Context context, AttributeSet attrs) {
		this.context = context;
		resources = this.context.getResources();

		if (attrs != null) {
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AHBottomNavigationBehavior_Params, 0, 0);
			try {
				selectedBackgroundVisible = ta.getBoolean(R.styleable.AHBottomNavigationBehavior_Params_selectedBackgroundVisible, false);
				translucentNavigationEnabled = ta.getBoolean(R.styleable.AHBottomNavigationBehavior_Params_translucentNavigationEnabled, false);
			} finally {
				ta.recycle();
			}
		}

		// Item colors
		titleColorActive = ContextCompat.getColor(context, R.color.colorBottomNavigationAccent);
		titleColorInactive = ContextCompat.getColor(context, R.color.colorBottomNavigationInactive);
		// Colors for colored bottom navigation
		coloredTitleColorActive = ContextCompat.getColor(context, R.color.colorBottomNavigationActiveColored);
		coloredTitleColorInactive = ContextCompat.getColor(context, R.color.colorBottomNavigationInactiveColored);

		itemActiveColor = titleColorActive;
		itemInactiveColor = titleColorInactive;

		ViewCompat.setElevation(this, resources.getDimension(R.dimen.bottom_navigation_elevation));
		setClipToPadding(false);

		ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, bottomNavigationHeight);
		setLayoutParams(params);
	}

	/**
	 * Create the items in the bottom navigation
	 */
	private void createItems() {
		if (items.size() > MAX_ITEMS) {
			Log.w(TAG, "The items list should not have more than 5 items");
		}

		int layoutHeight = getNavigationBarHeight();

		removeAllViews();
		views.clear();
		backgroundColorView = new View(context);
		backgroundColorView.setBackgroundColor(Color.WHITE);
		LayoutParams backgroundLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, layoutHeight);
		backgroundLayoutParams.gravity = Gravity.BOTTOM;
		addView(backgroundColorView, backgroundLayoutParams);
		bottomNavigationHeight = layoutHeight;
		
		linearLayoutContainer = new LinearLayout(context);
		linearLayoutContainer.setOrientation(LinearLayout.HORIZONTAL);
		linearLayoutContainer.setGravity(Gravity.CENTER);

		createClassicItems(linearLayoutContainer);

		LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, layoutHeight);
		layoutParams.gravity = Gravity.BOTTOM;
		addView(linearLayoutContainer, layoutParams);

		// Force a request layout after all the items have been created
		post(new Runnable() {
			@Override
			public void run() {
				requestLayout();
			}
		});
	}

	// updated

	/**
	 * Create classic items (only 3 items in the bottom navigation)
	 *
	 * @param linearLayout The layout where the items are added
	 */
	private void createClassicItems(LinearLayout linearLayout) {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		float height = getNavigationBarHeight();
		float minWidth = resources.getDimension(R.dimen.bottom_navigation_min_width);
		float maxWidth = resources.getDimension(R.dimen.bottom_navigation_max_width);

		int layoutWidth = getWidth();
		if (layoutWidth == 0 || items.size() == 0) {
			return;
		}

		float itemWidth = layoutWidth / items.size();
		if (itemWidth < minWidth) {
			itemWidth = minWidth;
		} else if (itemWidth > maxWidth) {
			itemWidth = maxWidth;
		}

		float activeSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_active);
		float inactiveSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_inactive);

		if (titleActiveTextSize != 0 && titleInactiveTextSize != 0) {
			activeSize = titleActiveTextSize;
			inactiveSize = titleInactiveTextSize;
		}

		int leni = items.size();
		for (int i = 0; i < leni; i++) {
			final boolean current = currentItem == i;
			final int itemIndex = i;
			AHBottomNavigationItem item = items.get(itemIndex);

			View view = inflater.inflate(R.layout.bottom_navigation_item, this, false);
			ImageView icon = (ImageView) view.findViewById(R.id.bottom_navigation_item_icon);
			TextView title = (TextView) view.findViewById(R.id.bottom_navigation_item_title);

			icon.setImageDrawable(item.getDrawable(context));

			boolean hasTitle = !item.getTitle(context).equals("");
			if (hasTitle) {
				title.setText(item.getTitle(context));
			} else {
				title.setTextSize(3);
			}

			if (titleTypeface != null) {
				title.setTypeface(titleTypeface);
			}

			if (current && hasTitle) {
				if (selectedBackgroundVisible) {
					view.setSelected(true);
				}
				icon.setSelected(true);
			} else {
				icon.setSelected(false);
			}

			linearLayoutContainer.setBackgroundColor(Color.TRANSPARENT);

			icon.setImageDrawable(AHHelper.getTintDrawable(items.get(i).getDrawable(context),
					current && hasTitle ? itemActiveColor : itemInactiveColor, forceTint));

			if (hasTitle) {
				title.setTextColor(current ? itemActiveColor : itemInactiveColor);
				title.setTextSize(TypedValue.COMPLEX_UNIT_PX, current ? activeSize : inactiveSize);
			}
			view.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					updateItems(itemIndex, true);
				}
			});
			view.setSoundEffectsEnabled(soundEffectsEnabled);

			LayoutParams params = new LayoutParams((int) itemWidth, (int) height);
			linearLayout.addView(view, params);
			views.add(view);
		}
	}

	/**
	 * Update Items UI
	 *
	 * @param itemIndex   int: Selected item position
	 * @param useCallback boolean: Use or not the callback
	 */
	private void updateItems(final int itemIndex, boolean useCallback) {

		if (currentItem == itemIndex) {
			if (tabSelectedListener != null && useCallback) {
				tabSelectedListener.onTabSelected(itemIndex, true);
			}
			return;
		}

		if (tabSelectedListener != null && useCallback) {
			boolean selectionAllowed = tabSelectedListener.onTabSelected(itemIndex, false);
			if (!selectionAllowed) return;
		}

		float activeSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_active);
		float inactiveSize = resources.getDimension(R.dimen.bottom_navigation_text_size_forced_inactive);

		if (titleActiveTextSize != 0 && titleInactiveTextSize != 0) {
			activeSize = titleActiveTextSize;
			inactiveSize = titleInactiveTextSize;
		}

		for (int i = 0; i < views.size(); i++) {

			final View view = views.get(i);
			if (selectedBackgroundVisible) {
				view.setSelected(i == itemIndex);
			}

			if (i == itemIndex) {

				final TextView title = (TextView) view.findViewById(R.id.bottom_navigation_item_title);
				final boolean hasTitle = !title.getText().equals("");
				final ImageView icon = (ImageView) view.findViewById(R.id.bottom_navigation_item_icon);

				if (hasTitle)
				{
					icon.setSelected(true);
					AHHelper.updateTextColor(title, itemInactiveColor, itemActiveColor);
					AHHelper.updateTextSize(title, inactiveSize, activeSize);
					AHHelper.updateDrawableColor(context, items.get(itemIndex).getDrawable(context), icon,
							itemInactiveColor, itemActiveColor, forceTint);
				}

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && colored) {

					int finalRadius = Math.max(getWidth(), getHeight());
					int cx = (int) view.getX() + view.getWidth() / 2;
					int cy = view.getHeight() / 2;

					if (circleRevealAnim != null && circleRevealAnim.isRunning()) {
						circleRevealAnim.cancel();
					}

					circleRevealAnim = ViewAnimationUtils.createCircularReveal(backgroundColorView, cx, cy, 0, finalRadius);
					circleRevealAnim.setStartDelay(5);
					circleRevealAnim.start();
				} else if (colored) {
					AHHelper.updateViewBackgroundColor(this, currentColor,
							items.get(itemIndex).getColor(context));
				}

			} else if (i == currentItem) {

				final TextView title = (TextView) view.findViewById(R.id.bottom_navigation_item_title);
				final boolean hasTitle = !title.getText().equals("");
				final ImageView icon = (ImageView) view.findViewById(R.id.bottom_navigation_item_icon);

				if (hasTitle)
				{
					icon.setSelected(false);
					AHHelper.updateTextColor(title, itemActiveColor, itemInactiveColor);
					AHHelper.updateTextSize(title, activeSize, inactiveSize);
					AHHelper.updateDrawableColor(context, items.get(currentItem).getDrawable(context), icon,
							itemActiveColor, itemInactiveColor, forceTint);
				}
			}
		}

		currentItem = itemIndex;
		if (currentItem > 0 && currentItem < items.size()) {
			currentColor = items.get(currentItem).getColor(context);
		}
	}

////////////
	// PUBLIC //
	////////////

	/**
	 * Add an item
	 */
	public void addItem(AHBottomNavigationItem item) {
		if (this.items.size() > MAX_ITEMS) {
			Log.w(TAG, "The items list should not have more than 5 items");
		}
		items.add(item);
		createItems();
	}

	/**
	 * Add all items
	 */
	public void addItems(List<AHBottomNavigationItem> items) {
		if (items.size() > MAX_ITEMS || (this.items.size() + items.size()) > MAX_ITEMS) {
			Log.w(TAG, "The items list should not have more than 5 items");
		}
		this.items.addAll(items);
		createItems();
	}

	/**
	 * Remove an item at the given index
	 */
	public void removeItemAtIndex(int index) {
		if (index < items.size()) {
			this.items.remove(index);
			createItems();
		}
	}

	/**
	 * Remove all items
	 */
	public void removeAllItems() {
		this.items.clear();
		createItems();
	}

	/**
	 * Refresh the AHBottomView
	 */
	public void refresh() {
		createItems();
	}

	/**
	 * Return the number of items
	 *
	 * @return int
	 */
	public int getItemsCount() {
		return items.size();
	}

	/**
	 * Return if the Bottom Navigation is colored
	 */
	public boolean isColored() {
		return colored;
	}

	/**
	 * Set if the Bottom Navigation is colored
	 */
	public void setColored(boolean colored) {
		this.colored = colored;
		this.itemActiveColor = colored ? coloredTitleColorActive : titleColorActive;
		this.itemInactiveColor = colored ? coloredTitleColorInactive : titleColorInactive;
		createItems();
	}

	/**
	 * Return the bottom navigation background color
	 *
	 * @return The bottom navigation background color
	 */
	public int getDefaultBackgroundColor() {
		return defaultBackgroundColor;
	}

	/**
	 * Set the bottom navigation background color
	 *
	 * @param defaultBackgroundColor The bottom navigation background color
	 */
	public void setDefaultBackgroundColor(@ColorInt int defaultBackgroundColor) {
		this.defaultBackgroundColor = defaultBackgroundColor;
	}

	/**
	 * Get the accent color (used when the view contains 3 items)
	 *
	 * @return The default accent color
	 */
	public int getAccentColor() {
		return itemActiveColor;
	}

	/**
	 * Set the accent color (used when the view contains 3 items)
	 *
	 * @param accentColor The new accent color
	 */
	public void setAccentColor(int accentColor) {
		this.titleColorActive = accentColor;
		this.itemActiveColor = accentColor;
	}

	/**
	 * Get the inactive color (used when the view contains 3 items)
	 *
	 * @return The inactive color
	 */
	public int getInactiveColor() {
		return itemInactiveColor;
	}

	/**
	 * Set the inactive color (used when the view contains 3 items)
	 *
	 * @param inactiveColor The inactive color
	 */
	public void setInactiveColor(int inactiveColor) {
		this.titleColorInactive = inactiveColor;
		this.itemInactiveColor = inactiveColor;
	}

	/**
	 * Set the colors used when the bottom bar uses the colored mode
	 *
	 * @param colorActive   The active color
	 * @param colorInactive The inactive color
	 */
	public void setColoredModeColors(@ColorInt int colorActive, @ColorInt int colorInactive) {
		this.coloredTitleColorActive = colorActive;
		this.coloredTitleColorInactive = colorInactive;
		createItems();
	}

	/**
	 * Set selected background visibility
     */
	public void setSelectedBackgroundVisible(boolean visible) {
		this.selectedBackgroundVisible = visible;
	}

	/**
	 * Set typeface
	 *
	 * @param typeface Typeface
	 */
	public void setTitleTypeface(Typeface typeface) {
		this.titleTypeface = typeface;
	}

	/**
	 * Set title text size in pixels
	 *
	 * @param activeSize
	 * @param inactiveSize
	 */
	public void setTitleTextSize(float activeSize, float inactiveSize) {
		this.titleActiveTextSize = activeSize;
		this.titleInactiveTextSize = inactiveSize;
	}

	/**
	 * Set title text size in SP
	 *
	 +	 * @param activeSize in sp
	 +	 * @param inactiveSize in sp
	 */
	public void setTitleTextSizeInSp(float activeSize, float inactiveSize) {
		this.titleActiveTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, activeSize, resources.getDisplayMetrics());
		this.titleInactiveTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, inactiveSize, resources.getDisplayMetrics());
	}

	/**
	 * Get navigation bar height
	 */
	public int getNavigationBarHeight() {
		return this.navigationBarHeight == 0 ? (int) resources.getDimension(R.dimen.bottom_navigation_height) : this.navigationBarHeight;
	}

	/**
	 * Set navigation bar height in SP
	 *
	 +	 * @param height in sp
	 */
	public void setNavigationBarHeight(int height) {
		this.navigationBarHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, height, resources.getDisplayMetrics());
	}

	/**
	 * Get item at the given index
	 *
	 * @param position int: item position
	 * @return The item at the given position
	 */
	public AHBottomNavigationItem getItem(int position) {
		if (position < 0 || position > items.size() - 1) {
			Log.w(TAG, "The position is out of bounds of the items (" + items.size() + " elements)");
		}
		return items.get(position);
	}

	/**
	 * Get the current item
	 *
	 * @return The current item position
	 */
	public int getCurrentItem() {
		return currentItem;
	}

	/**
	 * Set the current item
	 *
	 * @param position int: position
	 */
	public void setCurrentItem(int position) {
		setCurrentItem(position, true);
	}

	/**
	 * Set the current item
	 *
	 * @param position    int: item position
	 * @param useCallback boolean: use or not the callback
	 */
	public void setCurrentItem(int position, boolean useCallback) {
		if (position >= items.size()) {
			Log.w(TAG, "The position is out of bounds of the items (" + items.size() + " elements)");
			return;
		}

		updateItems(position, useCallback);
	}

	/**
	 * Return if the behavior translation is enabled
	 *
	 * @return a boolean value
	 */
	public boolean isBehaviorTranslationEnabled() {
		return behaviorTranslationEnabled;
	}

	/**
	 * Set the behavior translation value
	 *
	 * @param behaviorTranslationEnabled boolean for the state
	 */
	public void setBehaviorTranslationEnabled(boolean behaviorTranslationEnabled) {
		this.behaviorTranslationEnabled = behaviorTranslationEnabled;
		if (getParent() instanceof CoordinatorLayout) {
			ViewGroup.LayoutParams params = getLayoutParams();
			if (bottomNavigationBehavior == null) {
				bottomNavigationBehavior = new AHBottomNavigationBehavior<>(behaviorTranslationEnabled, navigationBarHeight);
			} else {
				bottomNavigationBehavior.setBehaviorTranslationEnabled(behaviorTranslationEnabled, navigationBarHeight);
			}
			if (navigationPositionListener != null) {
				bottomNavigationBehavior.setOnNavigationPositionListener(navigationPositionListener);
			}
			((CoordinatorLayout.LayoutParams) params).setBehavior(bottomNavigationBehavior);
			if (needHideBottomNavigation) {
				needHideBottomNavigation = false;
				bottomNavigationBehavior.hideView(this, bottomNavigationHeight, hideBottomNavigationWithAnimation);
			}
		}
	}

	/**
	 * Manage the floating action button behavior with AHBottomNavigation
	 * @param fab Floating Action Button
	 */
	public void manageFloatingActionButtonBehavior(FloatingActionButton fab) {
		if (fab.getParent() instanceof CoordinatorLayout) {
			AHBottomNavigationFABBehavior fabBehavior = new AHBottomNavigationFABBehavior(navigationBarHeight);
			((CoordinatorLayout.LayoutParams) fab.getLayoutParams())
					.setBehavior(fabBehavior);
		}
	}

	/**
	 * Hide Bottom Navigation with animation
	 */
	public void hideBottomNavigation() {
		hideBottomNavigation(true);
	}

	/**
	 * Hide Bottom Navigation with or without animation
	 *
	 * @param withAnimation Boolean
	 */
	public void hideBottomNavigation(boolean withAnimation) {
		if (bottomNavigationBehavior != null) {
			bottomNavigationBehavior.hideView(this, bottomNavigationHeight, withAnimation);
		} else if (getParent() instanceof CoordinatorLayout) {
			needHideBottomNavigation = true;
			hideBottomNavigationWithAnimation = withAnimation;
		} else {
			// Hide bottom navigation
			ViewCompat.animate(this)
					.translationY(bottomNavigationHeight)
					.setInterpolator(new LinearOutSlowInInterpolator())
					.setDuration(withAnimation ? 300 : 0)
					.start();
		}
	}

	/**
	 * Restore Bottom Navigation with animation
	 */
	public void restoreBottomNavigation() {
		restoreBottomNavigation(true);
	}

	/**
	 * Restore Bottom Navigation with or without animation
	 *
	 * @param withAnimation Boolean
	 */
	public void restoreBottomNavigation(boolean withAnimation) {
		if (bottomNavigationBehavior != null) {
			bottomNavigationBehavior.resetOffset(this, withAnimation);
		} else {
			// Show bottom navigation
			ViewCompat.animate(this)
					.translationY(0)
					.setInterpolator(new LinearOutSlowInInterpolator())
					.setDuration(withAnimation ? 300 : 0)
					.start();
		}
	}

	/**
	 * Return if the translucent navigation is enabled
	 */
	public boolean isTranslucentNavigationEnabled() {
		return translucentNavigationEnabled;
	}

	/**
	 * Set the translucent navigation value
	 */
	public void setTranslucentNavigationEnabled(boolean translucentNavigationEnabled) {
		this.translucentNavigationEnabled = translucentNavigationEnabled;
	}

	/**
	 * Return if the tint should be forced (with setColorFilter)
	 *
	 * @return Boolean
	 */
	public boolean isForceTint() {
		return forceTint;
	}

	/**
	 * Set the force tint value
	 * If forceTint = true, the tint is made with drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
	 *
	 * @param forceTint Boolean
	 */
	public void setForceTint(boolean forceTint) {
		this.forceTint = forceTint;
	}

	/**
	 * Set AHOnTabSelectedListener
	 */
	public void setOnTabSelectedListener(OnTabSelectedListener tabSelectedListener) {
		this.tabSelectedListener = tabSelectedListener;
	}

	/**
	 * Remove AHOnTabSelectedListener
	 */
	public void removeOnTabSelectedListener() {
		this.tabSelectedListener = null;
	}

	/**
	 * Set OnNavigationPositionListener
	 */
	public void setOnNavigationPositionListener(OnNavigationPositionListener navigationPositionListener) {
		this.navigationPositionListener = navigationPositionListener;
		if (bottomNavigationBehavior != null) {
			bottomNavigationBehavior.setOnNavigationPositionListener(navigationPositionListener);
		}
	}

	/**
	 * Remove OnNavigationPositionListener()
	 */
	public void removeOnNavigationPositionListener() {
		this.navigationPositionListener = null;
		if (bottomNavigationBehavior != null) {
			bottomNavigationBehavior.removeOnNavigationPositionListener();
		}
	}

	/**
	 * Return if the Bottom Navigation is hidden or not
	 */
	public boolean isHidden() {
		return bottomNavigationBehavior.isHidden();
	}

	/**
	 * Get the view at the given position
	 * @param position int
	 * @return The view at the position, or null
	 */
	public View getViewAtPosition(int position) {
		if (linearLayoutContainer != null && position >= 0
				&& position < linearLayoutContainer.getChildCount()) {
			return linearLayoutContainer.getChildAt(position);
		}
		return null;
	}

	////////////////
	// INTERFACES //
	////////////////

	/**
	 *
	 */
	public interface OnTabSelectedListener {
		/**
		 * Called when a tab has been selected (clicked)
		 *
		 * @param position    int: Position of the selected tab
		 * @param wasSelected boolean: true if the tab was already selected
		 * @return boolean: true for updating the tab UI, false otherwise
		 */
		boolean onTabSelected(int position, boolean wasSelected);
	}

	public interface OnNavigationPositionListener {
		/**
		 * Called when the bottom navigation position is changed
		 *
		 * @param y int: y translation of bottom navigation
		 */
		void onPositionChange(int y);
	}

}
