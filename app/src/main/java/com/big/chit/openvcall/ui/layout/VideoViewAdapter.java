package com.big.chit.openvcall.ui.layout;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.big.chit.R;
import com.big.chit.propeller.UserStatusData;
import com.big.chit.propeller.VideoInfoData;

import java.util.ArrayList;
import java.util.HashMap;


public abstract class VideoViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    public static final boolean DEBUG = false;

    protected final LayoutInflater mInflater;
    protected final Context mContext;

    protected final ArrayList<UserStatusData> mUsers;

    protected int mLocalUid;
    protected int mItemWidth;
    protected int mItemHeight;
    protected HashMap<Integer, VideoInfoData> mVideoInfo; // left user should removed from this HashMap
    private int mDefaultChildItem = 0;

    public VideoViewAdapter(Activity activity, int localUid, HashMap<Integer, SurfaceView> uids) {
        mInflater = ((Activity) activity).getLayoutInflater();
        mContext = ((Activity) activity).getApplicationContext();

        mLocalUid = localUid;

        mUsers = new ArrayList<>();

        init(uids);
    }

    private void init(HashMap<Integer, SurfaceView> uids) {
        mUsers.clear();

        customizedInit(uids, true);
    }

    protected abstract void customizedInit(HashMap<Integer, SurfaceView> uids, boolean force);

    public abstract void notifyUiChanged(HashMap<Integer, SurfaceView> uids, int uidExtra, HashMap<Integer, Integer> status, HashMap<Integer, Integer> volume);

    public void addVideoInfo(int uid, VideoInfoData video) {
        if (mVideoInfo == null) {
            mVideoInfo = new HashMap<>();
        }
        mVideoInfo.put(uid, video);
    }

    public void cleanVideoInfo() {
        mVideoInfo = null;
    }

    public void setLocalUid(int uid) {
        mLocalUid = uid;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewGroup v = (ViewGroup) mInflater.inflate(R.layout.video_view_container, parent, false);
        v.getLayoutParams().width = mItemWidth;
        v.getLayoutParams().height = mItemHeight;
        mDefaultChildItem = v.getChildCount();
        return new VideoUserStatusHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        VideoUserStatusHolder myHolder = ((VideoUserStatusHolder) holder);

        final UserStatusData user = mUsers.get(position);

        if (DEBUG) {
            //  log.debug("onBindViewHolder " + position + " " + user + " " + myHolder + " " + myHolder.itemView + " " + mDefaultChildItem);
        }

        FrameLayout holderView = (FrameLayout) myHolder.itemView;

        if (holderView.getChildCount() == mDefaultChildItem) {
            SurfaceView target = user.mView;
            VideoViewAdapterUtil.stripView(target);
            holderView.addView(target, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        VideoViewAdapterUtil.renderExtraData(mContext, user, myHolder);
    }

    @Override
    public int getItemCount() {
        if (DEBUG) {
            //log.debug("getItemCount " + mUsers.size());
        }
        return mUsers.size();
    }

    @Override
    public long getItemId(int position) {
        UserStatusData user = mUsers.get(position);

        SurfaceView view = user.mView;
        if (view == null) {
            throw new NullPointerException("SurfaceView destroyed for user " + user.mUid + " " + user.mStatus + " " + user.mVolume);
        }

        return (String.valueOf(user.mUid) + System.identityHashCode(view)).hashCode();
    }
}
