package joe.com.view.tablayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static android.support.v4.view.ViewPager.SCROLL_STATE_DRAGGING;
import static android.support.v4.view.ViewPager.SCROLL_STATE_IDLE;
import static android.support.v4.view.ViewPager.SCROLL_STATE_SETTLING;

/**
 * Description
 * Created by chenqiao on 2016/8/24.
 */
public class TabLayout extends HorizontalScrollView {

    private LinearLayout tabContainer;
    private ArrayList<String> tabs;
    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;
    private DataSetObserver mPagerAdapterObserver;
    private int selected = -1;
    private float tab_unselected_size;
    private float tab_selected_size;
    private int tab_unselected_color;
    private int tab_selected_color;
    private OnTabSelectedListener mOnTabSelectedListener;
    private TabLayoutOnPageChangeListener mPageChangeListener;

    public TabLayout(Context context) {
        this(context, null);
    }

    public TabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //获取选中与未选中下的大小及颜色设定
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TabLayout);
        tab_unselected_size = typedArray.getDimension(R.styleable.TabLayout_unselected_txt_size, 20);
        tab_selected_size = typedArray.getDimension(R.styleable.TabLayout_selected_txt_size, 30);
        tab_unselected_color = typedArray.getColor(R.styleable.TabLayout_unselected_txt_color, Color.GRAY);
        tab_selected_color = typedArray.getColor(R.styleable.TabLayout_selected_txt_color, Color.RED);
        typedArray.recycle();
        setBackgroundColor(Color.TRANSPARENT);

        //初始化tab容器
        tabContainer = new LinearLayout(context);
        tabContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabContainer.setGravity(Gravity.CENTER_VERTICAL);
        tabContainer.setBackgroundColor(Color.TRANSPARENT);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(tabContainer, params);
        tabs = new ArrayList<>();
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        int count = tabContainer.getChildCount();
        int sum = 0;
        for (int i = 0; i < count; i++) {
            View child = tabContainer.getChildAt(i);
            sum += child.getWidth() + 20;
            if (l < sum) {
                selectTab(i, false);
                break;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //将头和尾的tab的marginleft和marginright计算，使得tab选中时可以居中
        int count = tabContainer.getChildCount();
        if (count > 0) {
            View view = tabContainer.getChildAt(0);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
            params.setMargins(sizeWidth / 2 - view.getWidth() / 2, 0, 20, 0);
            View view2 = tabContainer.getChildAt(count - 1);
            LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) view2.getLayoutParams();
            params2.setMargins(20, 0, sizeWidth / 2 - view2.getWidth() / 2, 0);
        }
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    public void setTabs(ArrayList<String> tabs) {
        removeAllTabs();
        this.tabs = tabs;
        addTabsToLayout();
        selectTab(0);
    }

    public void setupWithViewPager(ViewPager viewPager) {
        mViewPager = viewPager;
        if (mViewPager == null) {
            return;
        }
        setPagerAdapter(viewPager.getAdapter(), true);
        setOnTabSelectedListener(new ViewPagerOnTabSelectedListener(mViewPager));
        if (mPageChangeListener != null) {
            mViewPager.removeOnPageChangeListener(mPageChangeListener);
        }
        if (mPageChangeListener == null) {
            mPageChangeListener = new TabLayoutOnPageChangeListener(this);
        }
        mPageChangeListener.reset();
        mViewPager.addOnPageChangeListener(mPageChangeListener);
    }

    private void setPagerAdapter(@Nullable final PagerAdapter adapter, final boolean addObserver) {
        if (mPagerAdapter != null && mPagerAdapterObserver != null) {
            // If we already have a PagerAdapter, unregister our observer
            mPagerAdapter.unregisterDataSetObserver(mPagerAdapterObserver);
        }

        mPagerAdapter = adapter;
        if (addObserver && adapter != null) {
            // Register our observer on the new adapter
            if (mPagerAdapterObserver == null) {
                mPagerAdapterObserver = new PagerAdapterObserver();
            }
            adapter.registerDataSetObserver(mPagerAdapterObserver);
        }

        // Finally make sure we reflect the new adapter
        populateFromPagerAdapter();
    }

    private void populateFromPagerAdapter() {
        removeAllTabs();
        if (mPagerAdapter != null) {
            final int adapterCount = mPagerAdapter.getCount();
            for (int i = 0; i < adapterCount; i++) {
                tabs.add(mPagerAdapter.getPageTitle(i).toString());
            }
            addTabsToLayout();

            // Make sure we reflect the currently set ViewPager item
            if (mViewPager != null && tabs.size() > 0) {
                final int curItem = mViewPager.getCurrentItem();
                if (curItem != getSelected() && curItem < tabs.size()) {
                    selectTab(curItem);
                }
            }
        } else {
            removeAllTabs();
        }
    }

    private void addTabsToLayout() {
        for (int i = 0; i < tabs.size(); i++) {
            TabView tabView = new TabView(getContext());
            tabView.setText(tabs.get(i));
            tabView.position = i;
            tabView.mParent = this;
            tabView.setSelected_color(tab_selected_color);
            tabView.setUnselected_color(tab_unselected_color);
            tabView.setSelected_size(tab_selected_size);
            tabView.setUnselected_size(tab_unselected_size);
            tabView.setSelected(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i == 0) {
                params.setMargins(20 + getWidth() / 2, 0, 20, 0);
            } else if (i == tabs.size() - 1) {
                params.setMargins(20, 0, 20 + getWidth() / 2, 0);
            } else {
                params.setMargins(20, 0, 20, 0);
            }
            tabContainer.addView(tabView, params);
            if (selected == i) {
                tabView.setSelected(true);
            }
        }
    }

    public void selectTab(int curItem, boolean needScrollTo) {
        if (getSelected() >= 0 && getSelected() < tabContainer.getChildCount()) {
            TabView pre_view = (TabView) tabContainer.getChildAt(getSelected());
            pre_view.setSelected(false);
        }
        TabView now_view = (TabView) tabContainer.getChildAt(curItem);
        now_view.setSelected(true);
        if (needScrollTo) {
            int itemX = (int) (now_view.getX() + now_view.getWidth() / 2);
            int centerX = getMeasuredWidth() / 2;
            int nowScroll = getScrollX();
            smoothScrollBy((itemX - centerX) - nowScroll, 0);
        }
        if (mOnTabSelectedListener != null) {
            if (selected == curItem) {
                mOnTabSelectedListener.onTabReselected(selected);
            } else {
                mOnTabSelectedListener.onTabSelected(curItem);
            }
        }
        selected = curItem;
    }

    public void selectTab(int curItem) {
        selectTab(curItem, true);
    }

    private void removeAllTabs() {
        tabContainer.removeAllViews();
        tabs.clear();
    }

    public int getSelected() {
        return selected;
    }

    public void setOnTabSelectedListener(OnTabSelectedListener onTabSelectedListener) {
        mOnTabSelectedListener = onTabSelectedListener;
    }

    public interface OnTabSelectedListener {
        void onTabSelected(int position);

        void onTabReselected(int position);
    }

    public static class ViewPagerOnTabSelectedListener implements TabLayout.OnTabSelectedListener {
        private ViewPager viewPager;

        public ViewPagerOnTabSelectedListener(ViewPager viewPager) {
            this.viewPager = viewPager;
        }

        @Override
        public void onTabSelected(int position) {
            if (this.viewPager != null) {
                this.viewPager.setCurrentItem(position);
            }
        }

        @Override
        public void onTabReselected(int position) {

        }
    }

    private class PagerAdapterObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            populateFromPagerAdapter();
        }

        @Override
        public void onInvalidated() {
            populateFromPagerAdapter();
        }
    }

    private class TabView extends TextView {
        public boolean isSelected = false;
        private float selected_size;
        private float unselected_size;
        private int selected_color;
        private int unselected_color;
        public TabLayout mParent;
        public int position;

        @Override
        public boolean isSelected() {
            return isSelected;
        }

        public void setUnselected_color(int unselected_color) {
            this.unselected_color = unselected_color;
        }

        public void setSelected_color(int selected_color) {
            this.selected_color = selected_color;
        }

        public void setUnselected_size(float unselected_size) {
            this.unselected_size = unselected_size;
        }

        public void setSelected_size(float selected_size) {
            this.selected_size = selected_size;
        }

        public TabView(Context context) {
            this(context, null);
        }

        public TabView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
            this.setMinWidth(80);
            this.setGravity(Gravity.CENTER);
            this.setLines(1);
            this.setPadding(0, 10, 0, 10);
        }

        public TabView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            this.setClickable(true);
        }

        @Override
        public boolean performClick() {
            if (mParent != null) {
                mParent.selectTab(position);
            }
            return super.performClick();
        }

        public void setSelected(boolean isSelected) {
            if (isSelected) {
                this.setTextSize(this.selected_size);
                this.setTextColor(this.selected_color);
            } else {
                this.setTextSize(this.unselected_size);
                this.setTextColor(this.unselected_color);
            }
        }
    }

    public static class TabLayoutOnPageChangeListener implements ViewPager.OnPageChangeListener {
        private final WeakReference<TabLayout> mTabLayoutRef;
        private int mPreviousScrollState;
        private int mScrollState;

        public TabLayoutOnPageChangeListener(TabLayout tabLayout) {
            mTabLayoutRef = new WeakReference<>(tabLayout);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            mPreviousScrollState = mScrollState;
            mScrollState = state;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset,
                                   int positionOffsetPixels) {
            final TabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null) {
                final boolean updateText = mScrollState != SCROLL_STATE_SETTLING ||
                        mPreviousScrollState == SCROLL_STATE_DRAGGING;
                // Update the indicator if we're not settling after being idle. This is caused
                // from a setCurrentItem() call and will be handled by an animation from
                // onPageSelected() instead.
                final boolean updateIndicator = !(mScrollState == SCROLL_STATE_SETTLING
                        && mPreviousScrollState == SCROLL_STATE_IDLE);
                //TODO 过渡动画
//                tabLayout.setScrollPosition(position, positionOffset, updateText, updateIndicator);
            }
        }

        @Override
        public void onPageSelected(int position) {
            final TabLayout tabLayout = mTabLayoutRef.get();
            if (tabLayout != null && tabLayout.getSelected() != position) {
                tabLayout.selectTab(position);
            }
        }

        private void reset() {
            mPreviousScrollState = mScrollState = SCROLL_STATE_IDLE;
        }
    }
}