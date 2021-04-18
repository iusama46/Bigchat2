package com.big.chit.openvcall.ui.layout;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

public class InChannelMessageListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
//    private ArrayList<Message> mMsgList;
//
//    private final LayoutInflater mInflater;
//
//    public InChannelMessageListAdapter(Activity activity, ArrayList<Message> list) {
//        mInflater = activity.getLayoutInflater();
//        mMsgList = list;
//    }
//
//    @Override
//    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View v = mInflater.inflate(R.layout.in_channel_message, parent, false);
//        return new MessageHolder(v);
//    }
//
//    @Override
//    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
//        Message msg = mMsgList.get(position);
//
//        MessageHolder myHolder = (MessageHolder) holder;
//        String sender = msg.getSender().name;
//        if (TextUtils.isEmpty(sender)) {
//            myHolder.itemView.setBackgroundResource(R.drawable.rounded_bg_blue);
//        } else {
//            myHolder.itemView.setBackgroundResource(R.drawable.rounded_bg);
//        }
//        myHolder.mMsgContent.setText(msg.getContent());
//    }
//
//    @Override
//    public int getItemCount() {
//        return mMsgList.size();
//    }
//
//    @Override
//    public long getItemId(int position) {
//        return mMsgList.get(position).hashCode();
//    }
//
//    public class MessageHolder extends RecyclerView.ViewHolder {
//        TextView mMsgContent;
//
//        MessageHolder(View v) {
//            super(v);
//            mMsgContent = (TextView) v.findViewById(R.id.msg_content);
//        }
//    }
}
