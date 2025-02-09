package net.osmand.plus.myplaces;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener;

import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TabActivity;
import net.osmand.plus.backup.ui.BackupAuthorizationFragment;
import net.osmand.plus.myplaces.favorites.dialogs.FavoritesTreeFragment;
import net.osmand.plus.myplaces.favorites.dialogs.FragmentStateHolder;
import net.osmand.plus.myplaces.tracks.dialogs.AvailableTracksFragment;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MyPlacesActivity extends TabActivity {


	public static final String TAB_ID = "selected_tab_id";

	public static final int GPX_TAB = R.string.shared_string_tracks;
	public static final int FAV_TAB = R.string.shared_string_my_favorites;

	private OsmandSettings settings;

	private ViewPager viewPager;
	private List<WeakReference<FragmentStateHolder>> fragmentsStateList = new ArrayList<>();
	private int tabSize;

	private Bundle intentParams;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		settings = app.getSettings();
		app.applyTheme(this);
		super.onCreate(savedInstanceState);

		app.logEvent("myplaces_open");

		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(R.string.shared_string_my_places);
			actionBar.setElevation(0);
		}
		setContentView(R.layout.my_places);
		viewPager = findViewById(R.id.pager);

		List<TabItem> tabItems = getTabItems();
		setTabs(tabItems);

		if (savedInstanceState == null) {
			Intent intent = getIntent();
			if (intent != null && intent.hasExtra(MapActivity.INTENT_PARAMS)) {
				intentParams = intent.getBundleExtra(MapActivity.INTENT_PARAMS);
				int tabId = intentParams.getInt(TAB_ID, FAV_TAB);
				int pagerItem = 0;
				for (int n = 0; n < tabItems.size(); n++) {
					if (tabItems.get(n).resId == tabId) {
						pagerItem = n;
						break;
					}
				}
				viewPager.setCurrentItem(pagerItem, false);
			}
		}
	}

	private void setTabs(@NonNull List<TabItem> tabItems) {
		PagerSlidingTabStrip mSlidingTabLayout = findViewById(R.id.sliding_tabs);
		Integer tabId = settings.FAVORITES_TAB.get();
		int tab = 0;
		for (int i = 0; i < tabItems.size(); i++) {
			if (tabItems.get(i).resId == tabId) {
				tab = i;
			}
		}
		tabSize = tabItems.size();
		setViewPagerAdapter(viewPager, tabItems);
		mSlidingTabLayout.setViewPager(viewPager);
		viewPager.setCurrentItem(tab);
	}

	@NonNull
	private List<TabItem> getTabItems() {
		List<TabItem> mTabs = new ArrayList<>();
		mTabs.add(getTabIndicator(FAV_TAB, FavoritesTreeFragment.class));
		mTabs.add(getTabIndicator(GPX_TAB, AvailableTracksFragment.class));
		PluginsHelper.addMyPlacesTabPlugins(this, mTabs, getIntent());
		return mTabs;
	}

	@Nullable
	public Bundle storeCurrentState() {
		int currentItem = viewPager.getCurrentItem();
		if (currentItem >= 0 && currentItem < fragmentsStateList.size()) {
			FragmentStateHolder stateHolder = fragmentsStateList.get(currentItem).get();
			if (stateHolder != null) {
				return stateHolder.storeState();
			}
		}
		return null;
	}

	@Override
	public void onAttachFragment(@NonNull Fragment fragment) {
		if (fragment instanceof FragmentStateHolder) {
			fragment.setArguments(intentParams);
			fragmentsStateList.add(new WeakReference<>((FragmentStateHolder) fragment));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		List<TabItem> tabItems = getTabItems();
		if (tabItems.size() != tabSize) {
			setTabs(tabItems);
		}
		viewPager.addOnPageChangeListener(new SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				settings.FAVORITES_TAB.set(tabItems.get(position).resId);
			}
		});
	}

	@Override
	protected void onPause() {
		super.onPause();
		viewPager.clearOnPageChangeListeners();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			finish();
			return true;
		}
		return false;
	}

	public void showOnMap(@Nullable FragmentStateHolder fragment, double latitude, double longitude,
	                      int zoom, PointDescription pointDescription, boolean addToHistory, Object toShow) {
		settings.setMapLocationToShow(latitude, longitude, zoom, pointDescription, addToHistory, toShow);
		Bundle bundle = fragment != null ? fragment.storeState() : null;
		MapActivity.launchMapActivityMoveToTop(this, bundle);
	}

	public void showOsmAndCloud(@Nullable FragmentStateHolder fragment) {
		Bundle bundle = fragment != null ? fragment.storeState() : null;
		Bundle openScreenArguments = new Bundle();
		openScreenArguments.putBoolean(BackupAuthorizationFragment.OPEN_BACKUP_AUTH, true);
		MapActivity.launchMapActivityMoveToTop(this, bundle, null, openScreenArguments);
	}

	@Nullable
	public <T> T getFragment(String fragmentTag) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
		return fragment != null && !fragment.isDetached() && !fragment.isRemoving() ? (T) fragment : null;
	}
}