package com.big.chit.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.big.chit.R;
import com.big.chit.activities.MainActivity;
import com.big.chit.interfaces.OnMessageItemClick;
import com.big.chit.models.AttachmentTypes;
import com.big.chit.models.Message;
import com.big.chit.models.User;
import com.big.chit.utils.Helper;
import com.big.chit.viewHolders.BaseMessageViewHolder;
import com.big.chit.viewHolders.MessageAttachmentAudioViewHolder;
import com.big.chit.viewHolders.MessageAttachmentContactViewHolder;
import com.big.chit.viewHolders.MessageAttachmentDocumentViewHolder;
import com.big.chit.viewHolders.MessageAttachmentImageViewHolder;
import com.big.chit.viewHolders.MessageAttachmentLocationViewHolder;
import com.big.chit.viewHolders.MessageAttachmentRecordingViewHolder;
import com.big.chit.viewHolders.MessageAttachmentVideoViewHolder;
import com.big.chit.viewHolders.MessageTextViewHolder;
import com.big.chit.viewHolders.MessageTypingViewHolder;

import java.util.ArrayList;
import java.util.HashMap;

public class MessageAdapter extends RecyclerView.Adapter<BaseMessageViewHolder> {
    private Helper helper;
    private OnMessageItemClick itemClickListener;
    private MessageAttachmentRecordingViewHolder.RecordingViewInteractor recordingViewInteractor;
    private String myId;
    private Context context;
    private ArrayList<Message> messages = new ArrayList<>();
    private View newMessage;
    private HashMap<String, User> myUsersNameInPhoneMap;
    private ImageView statusImg;
    private RelativeLayout statusLay;
    TextView statusText;

    public static final int MY = 0x00000000;
    public static final int OTHER = 0x0000100;

    public MessageAdapter(Context context, ArrayList<Message> messages, String myId, View newMessage) {
        this.context = context;
        this.messages = messages;
        this.myId = myId;
        this.newMessage = newMessage;
        this.helper = new Helper(context);
        this.myUsersNameInPhoneMap = helper.getCacheMyUsers();

        if (context instanceof OnMessageItemClick) {
            this.itemClickListener = (OnMessageItemClick) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnUserGroupItemClick");
        }

        if (context instanceof MessageAttachmentRecordingViewHolder.RecordingViewInteractor) {
            this.recordingViewInteractor = (MessageAttachmentRecordingViewHolder.RecordingViewInteractor) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement RecordingViewInteractor");
        }
    }

    @Override
    public BaseMessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        viewType &= 0x00000FF;
        switch (viewType) {
            case AttachmentTypes.RECORDING:
                return new MessageAttachmentRecordingViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_recording, parent, false), itemClickListener, recordingViewInteractor, messages);
            case AttachmentTypes.AUDIO:
                return new MessageAttachmentAudioViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_audio, parent, false), itemClickListener, messages);
            case AttachmentTypes.CONTACT:
                return new MessageAttachmentContactViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_contact, parent, false), itemClickListener, messages);
            case AttachmentTypes.DOCUMENT:
                return new MessageAttachmentDocumentViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_document, parent, false), itemClickListener, messages);
            case AttachmentTypes.IMAGE:
                return new MessageAttachmentImageViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_image, parent, false), itemClickListener, messages);
            case AttachmentTypes.LOCATION:
                return new MessageAttachmentLocationViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_location, parent, false), itemClickListener, messages);
            case AttachmentTypes.VIDEO:
                return new MessageAttachmentVideoViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_attachment_video, parent, false), itemClickListener, messages);
            case AttachmentTypes.NONE_TYPING:
                return new MessageTypingViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_typing, parent, false));
            case AttachmentTypes.NONE_TEXT:
            default:
                return new MessageTextViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_text, parent, false), newMessage, itemClickListener, messages);
        }
    }

    @Override
    public void onBindViewHolder(BaseMessageViewHolder holder, int position) {
        try {
            holder.setData(messages.get(position), position, myUsersNameInPhoneMap, MainActivity.myUsers);

            if (messages.get(position).isSelected())
                holder.parentLayout.setBackgroundColor(Color.parseColor("#d2b2e5"));
            else
                holder.parentLayout.setBackgroundColor(Color.parseColor("#00000000"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemViewType(int position) {
        try {
            if (getItemCount() == 0) {
                return super.getItemViewType(position);
            } else {
                Message message = messages.get(position);
                int userType;
                if (message.getSenderId().equals(myId))
                    userType = MY;
                else
                    userType = OTHER;
                return message.getAttachmentType() | userType;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.getItemViewType(position);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}
