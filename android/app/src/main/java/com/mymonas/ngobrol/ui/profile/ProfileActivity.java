package com.mymonas.ngobrol.ui.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.flavienlaurent.notboringactionbar.AlphaForegroundColorSpan;
import com.flavienlaurent.notboringactionbar.KenBurnsView;
import com.makeramen.RoundedImageView;
import com.mymonas.ngobrol.R;
import com.mymonas.ngobrol.io.RestCallback;
import com.mymonas.ngobrol.io.RestClient;
import com.mymonas.ngobrol.io.model.BaseCallback;
import com.mymonas.ngobrol.model.UserData;
import com.mymonas.ngobrol.ui.holder.ScrollTabHolder;
import com.mymonas.ngobrol.util.Clog;
import com.mymonas.ngobrol.util.UserUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.util.ArrayList;

import retrofit.RetrofitError;
import retrofit.client.Response;


public class ProfileActivity extends FragmentActivity implements ScrollTabHolder, ViewPager.OnPageChangeListener {

    public static final String KEY_EXTRA_USER_DATA = "user_data";
    public static final String KEY_EXTRA_POSITION = "position";
    private static final int REQ_EDIT_PROFILE = 1;
    private int mMinHeaderHeight;
    private int mHeaderHeight;
    private int mActionBarHeight;
    private int mMinHeaderTranslation;
    private KenBurnsView mProfileBg;
    private RoundedImageView mProfileImg;
    private View mHeader;
    private PagerSlidingTabStrip mPagerSlidingTabStrip;
    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;
    private SpannableString mSpannableString;
    private AlphaForegroundColorSpan mAlphaForegroundColorSpan;
    private TypedValue mTypedValue = new TypedValue();
    private static AccelerateDecelerateInterpolator sSmoothInterpolator = new AccelerateDecelerateInterpolator();
    private RectF mRect1 = new RectF();
    private RectF mRect2 = new RectF();
    private TextView mTvName;
    private UserData mUserData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        mMinHeaderHeight = getResources().getDimensionPixelSize(R.dimen.min_header_height);
        mHeaderHeight = getResources().getDimensionPixelSize(R.dimen.header_height);
        mMinHeaderTranslation = -mMinHeaderHeight + getActionBarHeight();

        setContentView(R.layout.activity_profile);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mUserData = (UserData) getIntent().getSerializableExtra(KEY_EXTRA_USER_DATA);
        mTvName = (TextView) findViewById(R.id.name);
        mProfileBg = (KenBurnsView) findViewById(R.id.header_background);

       // mProfileBg.setResourceIds(R.drawable.profile_bg, R.drawable.profile_bg);
        mProfileImg = (RoundedImageView) findViewById(R.id.profile_img);

        mHeader = findViewById(R.id.header);
        mPagerSlidingTabStrip = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(3);

        mPagerAdapter = new PagerAdapter(this, getSupportFragmentManager());
        mPagerAdapter.setTabHolderScrollingContent(this);

        Bundle args = new Bundle();
        args.putSerializable(KEY_EXTRA_USER_DATA, mUserData);
        mPagerAdapter.addPage(getString(R.string.profile_info), InfoProfileFragment.class, args);
        mPagerAdapter.addPage(getString(R.string.profile_tab_threads), ThreadProfileFragment.class, args);
        mPagerAdapter.addPage("Posts", PostProfileFragment.class, args);

        mViewPager.setAdapter(mPagerAdapter);

        mPagerSlidingTabStrip.setViewPager(mViewPager);
        mPagerSlidingTabStrip.setOnPageChangeListener(this);
        mPagerSlidingTabStrip.setTextColor(Color.WHITE);
        mPagerSlidingTabStrip.setDividerColor(Color.WHITE);
        mPagerSlidingTabStrip.setIndicatorColor(Color.WHITE);

        mAlphaForegroundColorSpan = new AlphaForegroundColorSpan(0xffffffff);


        String name = mUserData.getUsername();
        if (mUserData.getFullname().length() > 0) {
            name = mUserData.getFullname();
        }

        mTvName.setText(name);
        mSpannableString = new SpannableString(name);

        getActionBarIconView().setAlpha(0f);
        getActionBar().setBackgroundDrawable(null);
        setTitleAlpha(0);

        ImageLoader imageLoader = ImageLoader.getInstance();
        ImageLoaderConfiguration imageLoaderConfiguration = new ImageLoaderConfiguration.Builder(this).build();
        imageLoader.init(imageLoaderConfiguration);

        DisplayImageOptions imageOptions = new DisplayImageOptions.Builder()
                .showImageOnFail(R.drawable.profile_bg)
                .showImageOnLoading(R.drawable.profile_bg)
                .showImageForEmptyUri(R.drawable.profile_bg)
                .cacheOnDisk(true)
                .cacheInMemory(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .resetViewBeforeLoading(false)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();


        imageLoader.displayImage(mUserData.getProfileUrl(), mProfileImg);
        imageLoader.displayImage(mUserData.getProfileBg(), mProfileBg.getImageViews()[0], imageOptions);


    }

    private ImageView getActionBarIconView() {
        return (ImageView) findViewById(android.R.id.home);
    }

    private int getActionBarHeight() {
        if (mActionBarHeight != 0) {
            return mActionBarHeight;
        }

        getTheme().resolveAttribute(android.R.attr.actionBarSize, mTypedValue, true);

        mActionBarHeight = TypedValue.complexToDimensionPixelSize(mTypedValue.data, getResources().getDisplayMetrics());

        return mActionBarHeight;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile, menu);

        UserUtils userUtils = new UserUtils(this);
        if(userUtils.getUserId() != mUserData.getId() ) {
            MenuItem item = menu.findItem(R.id.action_edit_profile);
            item.setVisible(false);
        }

        if(!userUtils.isModerator()) {
            MenuItem item = menu.findItem(R.id.action_remove_user);
            item.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.action_edit_profile) {
            Intent intent = new Intent(this, EditProfileActivity.class);
            startActivityForResult(intent, REQ_EDIT_PROFILE);
        } else if(id == R.id.action_remove_user) {
            removeUserAction();
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeUserAction() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_dialog_warning);
        builder.setTitle(getString(R.string.profile_remove_user_warning_title));
        builder.setMessage(getString(R.string.profile_remove_user_warning_message));
        builder.setPositiveButton(R.string.general_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                proceedToRemoveUser();
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton(R.string.general_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void proceedToRemoveUser() {
        UserUtils userUtils = new UserUtils(this);

        final ProgressDialog pDialog = new ProgressDialog(this);
        pDialog.setTitle(getString(R.string.profile_remove_user_dialog_title));
        pDialog.setMessage(getString(R.string.general_please_wait));
        pDialog.setCancelable(false);
        pDialog.show();

        RestClient.get().removeUser(userUtils.getAPI(), userUtils.getUserId(), userUtils.getAndroidId(), mUserData.getId(), new RestCallback<BaseCallback>(this){
            @Override
            public void success(BaseCallback o, Response response) {
                super.success(o, response);
                pDialog.dismiss();
                if(o.getSuccess() == 1) {
                    Toast.makeText(ProfileActivity.this, getString(R.string.profile_remove_user_success), Toast.LENGTH_SHORT).show();
                    ProfileActivity.this.finish();
                }

            }

            @Override
            public void failure(RetrofitError error) {
                super.failure(error);
                pDialog.dismiss();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQ_EDIT_PROFILE) {
            if(resultCode == Activity.RESULT_OK) {
                finish();
                UserUtils userUtils = new UserUtils(this);
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra(KEY_EXTRA_USER_DATA, userUtils.getUserData());
                startActivity(intent);
            }
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int i) {
        SparseArrayCompat<ScrollTabHolder> scrollTabHolder = mPagerAdapter.getScrollTabHolders();
        ScrollTabHolder currentHolder = scrollTabHolder.valueAt(i);

        currentHolder.adjustScroll((int) (mHeader.getHeight() + mHeader.getTranslationY()));
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    @Override
    public void adjustScroll(int scrollHeight) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount, int pagePosition) {
        Clog.d("pagePosition : "+pagePosition + " mViewPager.getCurrentItem : "+mViewPager.getCurrentItem());
        if (mViewPager.getCurrentItem() == pagePosition) {
            int scrollY = getScrollY(view);
            mHeader.setTranslationY(Math.max(-scrollY, mMinHeaderTranslation));
            float ratio = clamp(mHeader.getTranslationY() / mMinHeaderTranslation, 0.0f, 1.0f);
            interpolate(mProfileImg, getActionBarIconView(), sSmoothInterpolator.getInterpolation(ratio));
            setTitleAlpha(clamp(5.0F * ratio - 4.0F, 0.0F, 1.0F));
            setNameAlpha(clamp(4.0F - 5.0F * ratio, 0.0F, 1.0F));

        }
    }

    private void setNameAlpha(float alpha) {
        mTvName.setAlpha(alpha);
    }

    private void setTitleAlpha(float alpha) {
        mAlphaForegroundColorSpan.setAlpha(alpha);
        mSpannableString.setSpan(mAlphaForegroundColorSpan, 0, mSpannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        getActionBar().setTitle(mSpannableString);
    }

    private void interpolate(View view1, View view2, float interpolation) {
        getOnScreenRect(mRect1, view1);
        getOnScreenRect(mRect2, view2);

        float scaleX = 1.0F + interpolation * (mRect2.width() / mRect1.width() - 1.0F);
        float scaleY = 1.0F + interpolation * (mRect2.height() / mRect1.height() - 1.0F);
        float translationX = 0.5F * (interpolation * (mRect2.left + mRect2.right - mRect1.left - mRect1.right));
        float translationY = 0.5F * (interpolation * (mRect2.top + mRect2.bottom - mRect1.top - mRect1.bottom));

        view1.setTranslationX(translationX);
        view1.setTranslationY(translationY - mHeader.getTranslationY());
        view1.setScaleX(scaleX);
        view1.setScaleY(scaleY);
    }

    private RectF getOnScreenRect(RectF rect, View view) {
        rect.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        return rect;
    }

    private float clamp(float value, float max, float min) {
        return Math.max(Math.min(value, min), max);
    }

    private int getScrollY(AbsListView view) {
        View c = view.getChildAt(0);
        if (c == null) {
            return 0;
        }

        int firstVisiblePosition = view.getFirstVisiblePosition();
        int top = c.getTop();

        int headerHeight = 0;

        if (firstVisiblePosition >= 1) {
            headerHeight = mHeaderHeight;
        }

        return -top + firstVisiblePosition * c.getHeight() + headerHeight;
    }

    public class PagerAdapter extends FragmentPagerAdapter {
        private SparseArrayCompat<ScrollTabHolder> mScrollTabHolders;
        private ScrollTabHolder mListener;

        private ArrayList<PagerInfo> mPager = new ArrayList<PagerInfo>();
        private Context mContext;

        class PagerInfo {
            private final String name;
            private final Class<?> clss;
            private final Bundle args;

            PagerInfo(String name, Class<?> clss, Bundle args) {
                this.name = name;
                this.clss = clss;
                this.args = args;
            }

            public Class<?> getClss() {
                return clss;
            }

            public String getName() {
                return name;
            }

            public Bundle getArgs() {
                return args;
            }

        }

        public PagerAdapter(Context context, FragmentManager fm) {
            super(fm);
            mContext = context;
            mScrollTabHolders = new SparseArrayCompat<ScrollTabHolder>();

        }

        public void addPage(String name, Class<?> clss, Bundle args) {
            PagerInfo pagerInfo = new PagerInfo(name, clss, args);
            mPager.add(pagerInfo);
            notifyDataSetChanged();
        }

        public void setTabHolderScrollingContent(ScrollTabHolder listener) {
            mListener = listener;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mPager.get(position).getName();
        }

        @Override
        public int getCount() {
            return mPager.size();
        }

        @Override
        public Fragment getItem(int position) {
            Bundle args = mPager.get(position).getArgs();
            ScrollTabHolderFragment fragment = (ScrollTabHolderFragment) Fragment.instantiate(mContext, mPager.get(position).getClss().getName(), args);

            mScrollTabHolders.put(position, fragment);
            if (mListener != null) {
                fragment.setScrollTabHolder(mListener);
            }

            return fragment;
        }

        public SparseArrayCompat<ScrollTabHolder> getScrollTabHolders() {
            return mScrollTabHolders;
        }

    }

}
