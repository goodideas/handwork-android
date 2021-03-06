package com.daiyan.handwork.app.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.daiyan.handwork.R;
import com.daiyan.handwork.adapter.CommonAdapter;
import com.daiyan.handwork.app.BaseActivity;
import com.daiyan.handwork.app.widget.RefreshLayout;
import com.daiyan.handwork.app.widget.RoundImageView;
import com.daiyan.handwork.app.widget.RefreshLayout.OnLoadListener;
import com.daiyan.handwork.bean.Activitys;
import com.daiyan.handwork.bean.Comment;
import com.daiyan.handwork.common.ImageLoader;
import com.daiyan.handwork.common.UIHelper;
import com.daiyan.handwork.common.ViewHolder;
import com.daiyan.handwork.common.ImageLoader.Type;
import com.daiyan.handwork.common.server.DataServer;
import com.daiyan.handwork.constant.Consts;
import com.daiyan.handwork.utils.JsonUtils;
import com.daiyan.handwork.utils.LocationUtil;
import com.daiyan.handwork.utils.StringUtils;
import com.daiyan.handwork.utils.ToastUtils;

import android.app.Activity;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView.ScaleType;

/**
 * 活动列表
 * 
 * @author 魏工
 * @Date 2015年05月09日
 */
public class ActivitysList extends BaseActivity implements OnClickListener {

	private Activity mContext;
	private Resources mResources;

	/** 图片加载类 */
	private ImageLoader mImageLoader = ImageLoader.getInstance(3, Type.LIFO);
	
	/** 调用接口返回数据 */
	private HashMap<String, Object> mDatas;
	
	private ImageView mTitleLeftBtn;
	private TextView mTitleTextView;
	private TextView mTitleRightTextView;

	/** 带下拉刷新和加载更多的Layout */
	private RefreshLayout mRefreshLayout;
	
	/** 活动列表 */
	private ListView mListView;
	private CommonAdapter<Activitys> mAdapter;
	private List<Activitys> mListViewDatas = new ArrayList<Activitys>();
	
	/** 是否还有数据可加载 */
	private boolean mCanLoadMore = true;
	/** 是否加载更多 */
	private boolean mIsPullUpToLoadMore;

	/** 列表数据总数 */
	private int mTotal = 0;
	/** 页码 */
	private int mPageIndex = 0;


	/** 标志位，标志已经初始化完成 */
	private boolean isPrepared;

	/** 是否已被加载过一次 */
	private boolean mHasLoadedOnce;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState, R.layout.activity_activitys_list);
		mContext = ActivitysList.this;
		mResources = getResources();
		
		initView();
		initRefreshLayout();
		
		Connection(Consts.LOAD_NORMAL);
	}

	/**
	 * 初始化列表
	 */
	private void initView() {

		initTitleBar();

		// 通知列表
		mListView = (ListView) this.findViewById(R.id.id_lv_only);
		mListView.setOnItemClickListener(onItemClickListener);

		mAdapter = new CommonAdapter<Activitys>(mContext, mListViewDatas,
				R.layout.item_for_activitys_listview) {
			@Override
			public void convert(ViewHolder holder, final Activitys item) {
				ImageView avatarImageView = (ImageView) holder
						.getView(R.id.thumbIconImageView);
				//avatarImageView.setImageResource(R.drawable.default_avatar);
				mImageLoader.loadImage(item.thumbIcon, avatarImageView, true);
				
				holder.setText(R.id.nameTextView, item.title);
				holder.setText(R.id.placeTextView, item.place);
				holder.setText(R.id.dateTextView, item.date);
			}
		};
		mListView.setAdapter(mAdapter);
	}

	/**
	 * 初始化标题栏
	 */
	private void initTitleBar() {
		mTitleLeftBtn = (ImageView) findViewById(R.id.id_ib_title_left);
		mTitleLeftBtn.setImageResource(R.drawable.icon_back);
		mTitleLeftBtn.setOnClickListener(this);
		mTitleTextView = (TextView) findViewById(R.id.id_tv_title_center);
		mTitleTextView.setText(getResources().getString(
				R.string.mine_activitys));
		mTitleRightTextView = (TextView) findViewById(R.id.id_tv_title_right);
		mTitleRightTextView.setVisibility(View.GONE);
	}
	
	/**
	 * 初始化RefreshLayout
	 */
	private void initRefreshLayout() 
	{
		// 设置下拉刷新和加载更多
		mRefreshLayout = (RefreshLayout) this.findViewById(R.id.myRefreshLayout);
		mRefreshLayout.setListView(mListView);
		mRefreshLayout.setColorSchemeResources(R.color.green, R.color.orange,
				R.color.white, R.color.light_orange_pre);
		// 设置下拉刷新监听器
		mRefreshLayout.setOnRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {
				mRefreshLayout.postDelayed(new Runnable() {

					@Override
					public void run() {
						//下拉刷新
						Connection(Consts.LOAD_REFRESH);
					}
				}, 1000);
			}
		});
		// 设置加载更多监听器
		mRefreshLayout.setOnLoadListener(new OnLoadListener() {

			@Override
			public void onLoad() {
				mRefreshLayout.postDelayed(new Runnable() {

					@Override
					public void run() {
						//加载更多
						Connection(Consts.LOAD_MORE);
					}
				}, 1500);
			}
		});
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.id_ib_title_left:
			finish();
			break;
		}

	}

	private OnItemClickListener onItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Activitys item = mListViewDatas.get(position);
			String token = LocationUtil.readInit(mContext, Consts.TOKEN, "");
			String webUrl = Consts.URL_ACTIVITY_DETAIL_BASE + "?token=" + token + "&atid=" + item.id;
			UIHelper.showWebView(mContext, webUrl, Consts.WEBVIEW_PAGE_ACTIVITY_DETAIL);
		}
	};
	
	/**
	 * 解析并显示调用接口返回数据
	 */
	private void setActivitysFromNet()
	{
		if (!mIsPullUpToLoadMore) {
			// 不是加载更多，先清空列表数据，再添加数据
			mListViewDatas.clear();
		}

		try
		{
			if (mDatas.get(Consts.DATA_LIST) != null
					&& !mDatas.get(Consts.DATA_LIST).toString().equals(Consts.VALUE_NULL)) 
			{
				List<HashMap<String, Object>> activityListData = JsonUtils.getJsonValuesInArray(mDatas.get(Consts.DATA_LIST).toString());
				if (activityListData != null && activityListData.size() > 0) {
					int activityCount = activityListData.size();
					for (int index = 0; index < activityCount; index++) 
					{
						HashMap<String, Object> activityItemMap = activityListData.get(index);

						// 读取活动数据
						Activitys item = new Activitys();
						item.id = activityItemMap.get(Consts.ACT_ID).toString();
						item.thumbIcon = activityItemMap.get(Consts.ACT_THUMB_ICON).toString();
						item.title = activityItemMap.get(Consts.ACT_TITLE).toString();
						item.date = activityItemMap.get(Consts.ACT_START_TIME).toString();
						item.place = activityItemMap.get(Consts.ACT_ADDRESS).toString();
						item.detailUrl = "http://statictest.daiyan123.com/demo/activityinfo.html";

						mListViewDatas.add(item);
					}
					// 刷新活动列表控件
					mAdapter.notifyDataSetChanged();
				}

				// 是否还有数据可被加载
				mCanLoadMore = StringUtils.checkValid(
						mDatas.get(Consts.HAS_MORE).toString()).equals("1");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 调用接口加载数据
	 * 
	 * @param flag 0:普通加载, 1:下拉刷新, 2:加载更多
	 */
	private void Connection(int flag) {
		switch (flag) {
		case Consts.LOAD_NORMAL: // 第一次加载数据，先加载广告栏，再加载推荐作品列表
			mPageIndex = 0;
			String loading = mResources.getString(R.string.loading_data_tips);
			UIHelper.showDialogForLoading(mContext, loading, true);
			new GetMyActivitysTask().execute(flag);
			break;
		case Consts.LOAD_REFRESH: // 下拉刷新
			mPageIndex = 0;
			new GetMyActivitysTask().execute(flag);
			break;
		case Consts.LOAD_MORE: // 加载更多作品
			mPageIndex = mPageIndex + 1;
			mIsPullUpToLoadMore = true;
			new GetMyActivitysTask().execute(flag);
			break;
		}
	}
	
	/**
	 * 获取评论列表
	 */
	private class GetMyActivitysTask extends AsyncTask<Integer, Void, Boolean> {

		private int mFlag = -1;

		@Override
		protected Boolean doInBackground(Integer... params) {
			mFlag = params[0];
			try {
				if(mIsPullUpToLoadMore && !mCanLoadMore) // 【加载更多】到最后一页了
				{
					mDatas = null;
					Consts.NET_WORK_ERROR = mResources.getString(R.string.loading_no_more_data_tips);
				} 
				else 
				{
					mDatas = DataServer.getInstance().getMyActivity(mPageIndex);	
				}
				return mDatas != null;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean isSuccess) {
			// 加载数据显示处理
			if (isSuccess) {
				setActivitysFromNet();
				mHasLoadedOnce = true;
			} else {
				ToastUtils.show(mContext, Consts.NET_WORK_ERROR);
			}
			
			// 后处理
			switch (mFlag) {
			case Consts.LOAD_NORMAL:
				UIHelper.hideDialogForLoading();
				break;
			case Consts.LOAD_REFRESH:
				mRefreshLayout.setRefreshing(false);
				break;
			case Consts.LOAD_MORE:
				mRefreshLayout.setLoading(false);
				mIsPullUpToLoadMore = false;
				break;
			}
		}
	}
}