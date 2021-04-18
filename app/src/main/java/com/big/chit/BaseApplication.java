package com.big.chit;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.big.chit.models.User;
import com.big.chit.receivers.ConnectivityReceiver;
import com.big.chit.utils.Helper;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

import cafe.adriel.androidaudioconverter.AndroidAudioConverter;
import cafe.adriel.androidaudioconverter.callback.ILoadCallback;

public class BaseApplication extends Application implements LifecycleObserver {

    /**
     * Agora Calling SDK Added by Ussama Iftikhar on 12-April-2021.
     * Email iusama46@gmail.com
     * Email iusama466@gmail.com
     * Github https://github.com/iusama46
     */
    public static final String CALL = "INCOMING_CALL_N";
    private static FirebaseDatabase firebaseDatabase;
    private static DatabaseReference userRef, chatRef, groupsRef, statusRef, callRef;
    private static boolean isInBackground = false;
    protected Helper helper;
    protected User userMe;

    public static DatabaseReference getUserRef() {
        if (userRef == null) {
            userRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_USER);
            userRef.keepSynced(true);
        }
        return userRef;
    }

    public static DatabaseReference getChatRef() {
        if (chatRef == null) {
            chatRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_CHAT);
            chatRef.keepSynced(true);
        }
        return chatRef;
    }

    public static DatabaseReference getCallRef() {
        if (callRef == null) {
            callRef = firebaseDatabase.getReference(Helper.REF_DATA).child("call_zego");
            callRef.keepSynced(true);
        }
        return callRef;
    }

    public static DatabaseReference getGroupRef() {
        if (groupsRef == null) {
            groupsRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_GROUP);
            groupsRef.keepSynced(true);
        }
        return groupsRef;
    }

    public static DatabaseReference getStatusRef() {
        if (statusRef == null) {
            statusRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_STATUS_NEW);
            statusRef.keepSynced(true);
        }
        return statusRef;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);

    }

    @Override
    public void onCreate() {
        super.onCreate();
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            NotificationChannel notificationChannel =  new NotificationChannel(
//                    CALL,"Calling", NotificationManager.IMPORTANCE_HIGH
//            );
//
//            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//            manager.createNotificationChannel(notificationChannel);
//        }
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        ConnectivityReceiver.init(this);
        EmojiManager.install(new GoogleEmojiProvider());
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        //  MobileAds.initialize(this, "Statusr-app-id");
        init();
        createChannel();
    }

    public void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Uri ringUri = Settings.System.DEFAULT_RINGTONE_URI;
                NotificationChannel channel = new NotificationChannel(CALL, "Call Service2", NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("Incoming Call Notification");
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                channel.setImportance(NotificationManager.IMPORTANCE_HIGH);

                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.createNotificationChannel(channel);
//                channel.setSound(ringUri,
//                        new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                                .setLegacyStreamType(AudioManager.STREAM_RING)
//                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).build());
                //Objects.requireNonNull(AppController.getInstance().getContext().getSystemService(NotificationManager.class)).createNotificationChannel(channel);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void init() {
        firebaseDatabase = FirebaseDatabase.getInstance();
        userRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_USER);
        chatRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_CHAT);
        callRef = firebaseDatabase.getReference(Helper.REF_DATA).child("call_zego");
        groupsRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_GROUP);
        statusRef = firebaseDatabase.getReference(Helper.REF_DATA).child(Helper.REF_STATUS_NEW);

        AndroidAudioConverter.load(this, new ILoadCallback() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(Exception error) {
                error.printStackTrace();
            }
        });

        final FFmpeg ffmpeg = FFmpeg.getInstance(getApplicationContext());
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onSuccess() {

                }
            });
        } catch (FFmpegNotSupportedException e) {

        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void onAppBackgrounded() {
        markOnline(false);
        Helper.CURRENT_CHAT_ID = null;

        Log.d("clima", "App in background");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private void onAppForegrounded() {
        markOnline(true);

        Log.d("clima", "App in foreground");
    }

    private void markOnline(boolean b) {
        helper = new Helper(this);
        userMe = helper.getLoggedInUser();
        if (userMe != null && userMe.getId() != null) {
            getUserRef().child(userMe.getId()).child("timeStamp").setValue(System.currentTimeMillis());
            getUserRef().child(userMe.getId()).child("online").setValue(b);
        }
    }
}
