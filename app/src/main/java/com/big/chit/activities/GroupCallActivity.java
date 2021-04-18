package com.big.chit.activities;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.big.chit.R;
import com.big.chit.Utils;
import com.big.chit.models.Contact;
import com.big.chit.models.Group;
import com.big.chit.models.LogCall;
import com.big.chit.models.Status;
import com.big.chit.models.User;
import com.big.chit.openvcall.model.ConstantApp;
import com.big.chit.openvcall.ui.layout.GridVideoViewContainer;
import com.big.chit.openvcall.ui.layout.SmallVideoViewAdapter;
import com.big.chit.openvcall.ui.layout.SmallVideoViewDecoration;
import com.big.chit.propeller.UserStatusData;
import com.big.chit.propeller.ui.RtlLinearLayoutManager;
import com.big.chit.utils.AudioPlayer;
import com.big.chit.utils.OnDragTouchListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

/**
 * Agora Calling SDK Added by Ussama Iftikhar on 12-April-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */

public class GroupCallActivity extends BaseActivity implements SensorEventListener {
    public static final int LAYOUT_TYPE_DEFAULT = 0;
    public static final int LAYOUT_TYPE_SMALL = 1;
    static final String TAG = "clima";
    static final String ADDED_LISTENER = "addedListener";
    private static final String EXTRA_DATA_USER = "extradatauser";
    private static final String EXTRA_DATA_IN_OR_OUT = "extradatainorout";
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSION_REQ_ID = 22;
    private final HashMap<Integer, SurfaceView> mUidsList = new HashMap<>(); // uid = 0 || uid == EngineConfig.mUid
    private final int mCallDurationSecond = 0;
    //private final Handler mUIHandler = new Handler();
    public int mLayoutType = LAYOUT_TYPE_DEFAULT;
    int configUid = 0;
    String callRoomId = "group";
    PowerManager.WakeLock wlOff = null, wlOn = null;
    boolean isConnected = false;
    boolean isSpeaker = false;
    int counter = 0;
    Group group;
    ImageView addPerson;
    boolean isVideoCall;
    User tempUser;
    DatabaseReference userCall;
    boolean isLoggedIn = false;
    GridLayout gridLayout;
    RelativeLayout remoteVideo2;

    FrameLayout mLocalContainer, mRemoteContainer;
    SurfaceView mLocalView, mRemoteView;
    String key, accessToken;
    FrameLayout remoteVideo;
    private SmallVideoViewAdapter mSmallVideoViewAdapter;
    private GridVideoViewContainer mGridVideoViewContainer;
    private RelativeLayout mSmallVideoViewDock;
    private volatile int mAudioRouting = Constants.AUDIO_ROUTE_DEFAULT;
    //private volatile boolean mFullScreen = false;
    private boolean mIsLandscape = false;
    private RtcEngine mRtcEngine;
    private AudioPlayer mAudioPlayer;
    private String mCallId, inOrOut;
    private boolean mAddedListener, isMute, alphaInvisible, logSaved;
    private RelativeLayout myCallScreenRootRLY;
    private TextView mCallState, mCallerName, myTxtCalling;
    private Chronometer mCallDuration;


    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {


        @Override
        public void onRtcStats(RtcStats stats) {
            super.onRtcStats(stats);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    counter = stats.users;
                    if (isConnected) {
                        if (stats.users <= 1)
                            endCall();
                    }
                    if (stats.users > 1) {
                        mAudioPlayer.stopProgressTone();
                        mCallerName.setText(stats.users + " People");
                        isConnected = true;
                    }
                }
            });
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isLoggedIn = true;
                    if (!isVideoCall)
                        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                    else {
                        setVolumeControlStream(AudioManager.STREAM_SYSTEM);
                    }

                    Log.d("clima", String.valueOf(uid));
                    if (!inOrOut.equals("IN")) {
                        mAudioPlayer.playProgressTone();
                        //pushNotification(false);
                        callUser();
                    } else {
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(userMe.getId()).child(key);
                        reference.child("call_status").setValue(4);
                    }
                }
            });
        }


        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isLoggedIn = true;
                    Log.d("clima user ", String.valueOf(uid));
                    if (!isVideoCall)
                        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                    else
                        setVolumeControlStream(AudioManager.STREAM_SYSTEM);

                    mAudioPlayer.stopProgressTone();
                    if (!isVideoCall)
                        if (!mRtcEngine.isSpeakerphoneEnabled())
                            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

                    if (!isConnected) {
                        mAudioPlayer.stopProgressTone();
                        if (!isVideoCall)
                            myTxtCalling.setText(getResources().getString(R.string.app_name) + " Call Connected");

                        mCallDuration.setVisibility(View.VISIBLE);
                        mCallDuration.setFormat("%02d:%02d");
                        mCallDuration.setBase(SystemClock.elapsedRealtime());
                        mCallDuration.start();
                        Toast.makeText(GroupCallActivity.this, "double tap for small views", Toast.LENGTH_SHORT).show();
                    }
                    isConnected = true;
                    
                }
            });
        }


        @Override
        public void onUserOffline(final int uid, final int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    int temp = counter - 1;
                    if (isConnected) {
                        if (temp <= 1)
                            endCall();
                    }

                    if (isVideoCall) {
                        doRemoveRemoteUi(uid);
                    }
                }
            });
        }

        @Override
        public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
            super.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
            if (isVideoCall)
                doRenderRemoteUi(uid);
        }
    };
    private ImageView userImage1, userImage2, switchVideo, switchMic, switchVolume;
    private View tintBlue, bottomButtons;
    private RelativeLayout localVideo;
    private LinearLayout mySwitchCameraLLY;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private String receiverToken, roomToken;


    public static Intent newIntent(Context context, Group user, String callId, String inOrOut, boolean callIsVideo, String token, String roomToken, String key) {
        Intent intent = new Intent(context, GroupCallActivity.class);
        intent.putExtra(EXTRA_DATA_USER, user);
        intent.putExtra(EXTRA_DATA_IN_OR_OUT, inOrOut);
        intent.putExtra("CALL_ID", callId);
        intent.putExtra("isVideoCall", callIsVideo);
        intent.putExtra("token", token);
        intent.putExtra("room_token", roomToken);
        intent.putExtra("key", key);

        return intent;
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float distance = sensorEvent.values[0];
        if (!isVideoCall && !isSpeaker) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (distance < 4) {
                if (wlOn != null && wlOn.isHeld()) {
                    wlOn.release();
                }
                if (pm != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        if (wlOff == null)
                            wlOff = pm.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "tag");
                        if (!wlOff.isHeld()) wlOff.acquire();
                    }
                }
            } else {
                if (wlOff != null && wlOff.isHeld()) {
                    wlOff.release();
                }
                if (pm != null) {
                    if (wlOn == null)
                        wlOn = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "tag");
                    if (!wlOn.isHeld()) wlOn.acquire();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(ADDED_LISTENER, mAddedListener);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mAddedListener = savedInstanceState.getBoolean(ADDED_LISTENER);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_call);

        mLocalContainer = new FrameLayout(this);
        mRemoteContainer = new FrameLayout(this);
        mRemoteView = new SurfaceView(this);
        mLocalView = new SurfaceView(this);
        userCall = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        addPerson = findViewById(R.id.add_person);
        addPerson.setVisibility(View.VISIBLE);


        remoteVideo2 = findViewById(R.id.remoteVideo2);


        Intent intent = getIntent();
        group = intent.getParcelableExtra(EXTRA_DATA_USER);
        mCallId = intent.getStringExtra("CALL_ID");
        inOrOut = intent.getStringExtra(EXTRA_DATA_IN_OR_OUT);
        receiverToken = intent.getStringExtra("token");
        if (inOrOut.equals("IN"))
            key = intent.getStringExtra("key");
        //todo check
        roomToken = intent.getStringExtra("room_token");
        isVideoCall = intent.getBooleanExtra("isVideoCall", false);
        mAudioPlayer = new AudioPlayer(this);
        mCallDuration = findViewById(R.id.callDuration);
        mCallerName = findViewById(R.id.remoteUser);
        mCallState = findViewById(R.id.callState);
        userImage1 = findViewById(R.id.userImage1);
        userImage2 = findViewById(R.id.userImage2);
        myTxtCalling = findViewById(R.id.txt_calling);
        tintBlue = findViewById(R.id.tintBlue);
        localVideo = findViewById(R.id.localVideo);
        remoteVideo = findViewById(R.id.remoteVideo);
        switchVideo = findViewById(R.id.switchVideo);
        switchMic = findViewById(R.id.switchMic);
        switchVolume = findViewById(R.id.switchVolume);
        bottomButtons = findViewById(R.id.layout_btns);
        mySwitchCameraLLY = findViewById(R.id.switchVideo_LLY);
        myCallScreenRootRLY = findViewById(R.id.layout_call_screen_root_RLY);


        onZegoCreated();


        findViewById(R.id.hangupButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });

        remoteVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAlphaAnimation();
            }
        });
        switchMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isMute = !isMute;
                isMute();
            }
        });
        switchVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSpeaker = !isSpeaker;
                enableSpeaker(isSpeaker);
            }
        });
        addPerson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent callIntent = new Intent(GroupCallActivity.this, ContactActivity.class).putExtra("group", true);
                startActivityForResult(callIntent, 101);
                //startActivity(callIntent);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {

                if (!inOrOut.equals("IN")) {
                    tempUser = data.getParcelableExtra("contact");
                    if (tempUser != null) {
                        boolean isFound = false;
                        for (int i = 0; i < group.getUserIds().size(); i++) {
                            if (group.getUserIds().get(i).contains(tempUser.getId())) {
                                isFound = true;
                                break;
                            }
                        }
                        if (isFound) {
                            Toast.makeText(this, "user already invited", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("is_video", isVideoCall);
                        hashMap.put("is_group", true);
                        hashMap.put("call_status", 0);
                        hashMap.put("channel_id", callRoomId);
                        hashMap.put("channel_token", accessToken);
                        hashMap.put("caller_id", group.getId());
                        hashMap.put("caller_name", group.getName());
                        hashMap.put("receiver_id", group.getId());

                        reference.child(tempUser.getId()).child(key).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Toast.makeText(GroupCallActivity.this, "Added to call", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(this, "Only caller can add person", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        updateUI();
    }

    @Override
    public void onBackPressed() {
        // User should exit activity by ending call, not by going back.
    }

    protected void onDestroy() {
        mRtcEngine.leaveChannel();
        mRtcEngine.stopPreview();
        mAudioPlayer.stopProgressTone();
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        if (isLoggedIn) {
            if (isVideoCall)

                RtcEngine.destroy();
            mRtcEngine = null;
        }
        try {
            if (wlOff != null && wlOff.isHeld()) {
                wlOff.release();
            } else if (wlOn != null && wlOn.isHeld()) {
                wlOn.release();
            }
        } catch (RuntimeException ex) {
        }

        super.onDestroy();
    }

    private void endCall() {
        mAudioPlayer.stopProgressTone();

        try {
            if (!inOrOut.equals("IN")) {
                sendMissedCallUpdate();
            } else {
                //
            }
        } catch (Exception e) {
        }
        finish();
    }

    private void callUser() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");
        key = reference.push().getKey();

        HashMap<String, Object> datamap = new HashMap<>();
        datamap.put("is_video", isVideoCall);
        datamap.put("is_group", true);
        datamap.put("call_status", 0);
        datamap.put("channel_id", callRoomId);
        datamap.put("channel_token", accessToken);
        datamap.put("caller_id", group.getId());
        datamap.put("caller_name", group.getName());
        datamap.put("receiver_id", group.getId());

        for (int i = 0; i < group.getUserIds().size(); i++) {
            if (!userMe.getId().equals(group.getUserIds().get(i))) {
                reference.child(group.getUserIds().get(i)).child(key).setValue(datamap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                    }
                });
            }
        }
    }

    private void saveLog() {
        if (!logSaved) {
            rChatDb.beginTransaction();
            rChatDb.copyToRealm(new LogCall(user, System.currentTimeMillis(), mCallDurationSecond,
                    isVideoCall, inOrOut, group.getId(), group.getId()));
            rChatDb.commitTransaction();
            logSaved = true;
        }
    }

    void onZegoCreated() {
        if (isVideoCall && !checkSelfPermission(REQUESTED_PERMISSIONS[0], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[1], PERMISSION_REQ_ID) &&
                checkSelfPermission(REQUESTED_PERMISSIONS[2], PERMISSION_REQ_ID)) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else if (!checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), Utils.appID2, mRtcEventHandler);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
        } catch (Exception e) {
            Log.e("clima e", Log.getStackTraceString(e));
            Toast.makeText(this, "NEED TO check rtc sdk init fatal error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        accessToken = Utils.token;
        String uId = userMe.getId().substring(userMe.getId().length() - 5);
        configUid = Integer.parseInt(uId);

        //todo room token for incomming

        if (isVideoCall) {
            mRtcEngine.enableVideo();
            mGridVideoViewContainer = (GridVideoViewContainer) findViewById(R.id.grid_video_view_container);
            mGridVideoViewContainer.setItemEventHandler(new io.agora.propeller.ui.RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    onBigVideoViewClicked(view, position);
                }

                @Override
                public void onItemLongClick(View view, int position) {

                }

                @Override
                public void onItemDoubleClick(View view, int position) {
                    onBigVideoViewDoubleClicked(view, position);
                }
            });

            SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
            //preview(true, surfaceV, 0);
            mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, configUid));
            surfaceV.setZOrderOnTop(false);
            surfaceV.setZOrderMediaOverlay(false);


            mUidsList.put(configUid, surfaceV);
            mGridVideoViewContainer.initViewContainer(this, configUid, mUidsList, mIsLandscape); // first is now full view

        }
        mRtcEngine.joinChannel(accessToken, "testChannel", "Extra Optional Data", 0);
        if (isVideoCall) {
            setVolumeControlStream(AudioManager.STREAM_SYSTEM);
            isSpeaker = true;
            remoteVideo.setVisibility(View.VISIBLE);
            remoteVideo2.setVisibility(View.GONE);

        } else {
            mRtcEngine.disableVideo();
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            remoteVideo.setVisibility(View.GONE);
            remoteVideo2.setVisibility(View.VISIBLE);
        }

        isMute();
        enableSpeaker(isSpeaker);

        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));

        if (!inOrOut.equals("IN"))
            callRoomId = callRoomId + userMe.getId() + randomSuffix;

        if (callRoomId != null) {
            mCallerName.setText(group != null ? group.getName() : "2 People");

            if (group != null) {
//                Glide.with(this).load(user.getImage()).apply(new RequestOptions().placeholder(R.drawable.ic_logo_)).into(userImage1);
//                Glide.with(this).load(user.getImage()).apply(RequestOptions.circleCropTransform().placeholder(R.drawable.ic_logo_)).into(userImage2);
//                if (group.getImage() != null && !group.getImage().isEmpty()) {
//                    if (user.getBlockedUsersIds() != null
//                            && !group.getBlockedUsersIds().contains(group.getId()))
//                        Picasso.get()
//                                .load(user.getImage())
//                                .tag(this)
//                                .error(R.drawable.ic_avatar)
//                                .placeholder(R.drawable.ic_avatar)
//                                .into(userImage1);
//                    else
//                        Picasso.get()
//                                .load(R.drawable.ic_avatar)
//                                .tag(this)
//                                .error(R.drawable.ic_avatar)
//                                .placeholder(R.drawable.ic_avatar)
//                                .into(userImage1);
//
//                    Picasso.get()
//                            .load(user.getImage())
//                            .tag(this)
//                            .error(R.drawable.ic_avatar)
//                            .placeholder(R.drawable.ic_avatar)
//                            .into(userImage2);
//                } else {
//                    userImage1.setBackgroundResource(R.drawable.ic_avatar);
//                    userImage2.setBackgroundResource(R.drawable.ic_avatar);
//                }
//                tintBlue.setVisibility(View.INVISIBLE);
//                remoteVideo.setVisibility(View.INVISIBLE);
            }
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
            finish();
        }

        updateUI();

        if (!inOrOut.equals("IN")) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    Log.d("clima", "waired");

                    sendMissedCallUpdate();
                }
            }, 25000);
        }
    }

    private void sendMissedCallUpdate() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");
        for (int i = 0; i < group.getUserIds().size(); i++) {
            if (!userMe.getId().equals(group.getUserIds().get(i))) {
                reference.child(group.getUserIds().get(i)).child(key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getChildrenCount() == 8) {
                            if (dataSnapshot.child("call_status").getValue() != null) {
                                int callStatus = Integer.parseInt(String.valueOf(dataSnapshot.child("call_status").getValue()));

                                if (callStatus == 0) {
                                    dataSnapshot.getRef().child("call_status").setValue(1);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        }
    }

    void updateUI() {
        if (callRoomId != null) {
            myTxtCalling.setText(getResources().getString(R.string.app_name) + (isVideoCall ? " Video Calling" : " Voice Calling"));
            tintBlue.setVisibility(isVideoCall ? View.GONE : View.VISIBLE);
            localVideo.setVisibility(!isVideoCall ? View.GONE : View.VISIBLE);
        }

        switchVideo.setClickable(true);

        mySwitchCameraLLY.setVisibility(isVideoCall ? View.VISIBLE : View.GONE);
        switchVideo.setAlpha(isVideoCall ? 1f : 0.4f);
        userImage1.setVisibility(isVideoCall ? View.GONE : View.VISIBLE);

        localVideo.setOnTouchListener(new OnDragTouchListener(localVideo, remoteVideo));
        switchVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRtcEngine.switchCamera();
            }
        });
    }

    private void startAlphaAnimation() {
        AlphaAnimation animation1 = new AlphaAnimation(alphaInvisible ? 0.0f : 1.0f, alphaInvisible ? 1.0f : 0.0f);
        animation1.setDuration(500);
        animation1.setStartOffset(25);
        animation1.setFillAfter(true);

        myTxtCalling.startAnimation(animation1);
        userImage2.startAnimation(animation1);
        mCallerName.startAnimation(animation1);
        mCallState.startAnimation(animation1);
        mCallDuration.startAnimation(animation1);
        bottomButtons.startAnimation(animation1);

        alphaInvisible = !alphaInvisible;
    }

    private void enableSpeaker(boolean enable) {
        mRtcEngine.setEnableSpeakerphone(enable);

        if (isVideoCall)
            switchVolume.setImageDrawable(ContextCompat.getDrawable(this, enable ? R.drawable.ic_speaker : R.drawable.ic_speaker_off));
        else
            switchVolume.setImageDrawable(ContextCompat.getDrawable(this, !enable ? R.drawable.ic_speaker : R.drawable.ic_speaker_off));
    }

    private void isMute() {
        switchMic.setImageDrawable(ContextCompat.getDrawable(this, isMute ? R.drawable.ic_mic_off : R.drawable.ic_mic_on));
        mRtcEngine.muteLocalAudioStream(isMute);
    }

    @Override
    void myUsersResult(ArrayList<User> myUsers) {

    }

    @Override
    void myContactsResult(ArrayList<Contact> myContacts) {

    }

    @Override
    void userAdded(User valueUser) {

    }

    @Override
    void groupAdded(Group valueGroup) {

    }

    @Override
    void userUpdated(User valueUser) {

    }

    @Override
    void groupUpdated(Group valueGroup) {

    }

    @Override
    void statusAdded(Status status) {

    }

    @Override
    void statusUpdated(Status status) {

    }

    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i("clima", "checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    private void onBigVideoViewClicked(View view, int position) {

        //toggleFullscreen();
    }

    private void onBigVideoViewDoubleClicked(View view, int position) {
        if (mUidsList.size() < 2) {
            return;
        }


        UserStatusData user = mGridVideoViewContainer.getItem(position);
        int uid = (user.mUid == 0) ? configUid : user.mUid;

        if (mLayoutType == LAYOUT_TYPE_DEFAULT && mUidsList.size() != 1) {
            switchToSmallVideoView(uid);
        } else {
            switchToDefaultVideoView();
        }
    }

    private void onSmallVideoViewDoubleClicked(View view, int position) {

        switchToDefaultVideoView();
    }


    private void switchToSmallVideoView(int bigBgUid) {
        HashMap<Integer, SurfaceView> slice = new HashMap<>(1);
        slice.put(bigBgUid, mUidsList.get(bigBgUid));
        Iterator<SurfaceView> iterator = mUidsList.values().iterator();
        while (iterator.hasNext()) {
            SurfaceView s = iterator.next();
            s.setZOrderOnTop(true);
            s.setZOrderMediaOverlay(true);
        }

        mUidsList.get(bigBgUid).setZOrderOnTop(false);
        mUidsList.get(bigBgUid).setZOrderMediaOverlay(false);

        mGridVideoViewContainer.initViewContainer(this, bigBgUid, slice, mIsLandscape);

        bindToSmallVideoView(bigBgUid);

        mLayoutType = LAYOUT_TYPE_SMALL;

        requestRemoteStreamType(mUidsList.size());
    }


    private void doHideTargetView(int targetUid, boolean hide) {
        HashMap<Integer, Integer> status = new HashMap<>();
        status.put(targetUid, hide ? UserStatusData.VIDEO_MUTED : UserStatusData.DEFAULT_STATUS);
        if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
            mGridVideoViewContainer.notifyUiChanged(mUidsList, targetUid, status, null);
        } else if (mLayoutType == LAYOUT_TYPE_SMALL) {
            UserStatusData bigBgUser = mGridVideoViewContainer.getItem(0);
            if (bigBgUser.mUid == targetUid) { // big background is target view
                mGridVideoViewContainer.notifyUiChanged(mUidsList, targetUid, status, null);
            } else { // find target view in small video view list

                mSmallVideoViewAdapter.notifyUiChanged(mUidsList, bigBgUser.mUid, status, null);
            }
        }
    }

    private void doRemoveRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                Object target = mUidsList.remove(uid);
                if (target == null) {
                    return;
                }

                int bigBgUid = -1;
                if (mSmallVideoViewAdapter != null) {
                    bigBgUid = mSmallVideoViewAdapter.getExceptedUid();
                }

//                log.debug("doRemoveRemoteUi " + (uid & 0xFFFFFFFFL) + " " + (bigBgUid & 0xFFFFFFFFL) + " " + mLayoutType);

                if (mLayoutType == LAYOUT_TYPE_DEFAULT || uid == bigBgUid) {
                    switchToDefaultVideoView();
                } else {
                    switchToSmallVideoView(bigBgUid);
                }

                //notifyMessageChanged(new Message(new User(0, null), "user " + (uid & 0xFFFFFFFFL) + " left"));
            }
        });
    }


    private void requestRemoteStreamType(final int currentHostCount) {
        //   log.debug("requestRemoteStreamType " + currentHostCount);
    }

    private void switchToDefaultVideoView() {
        if (mSmallVideoViewDock != null) {
            mSmallVideoViewDock.setVisibility(View.GONE);
        }
        //todo
        mGridVideoViewContainer.initViewContainer(this, configUid, mUidsList, mIsLandscape);

        mLayoutType = LAYOUT_TYPE_DEFAULT;
        boolean setRemoteUserPriorityFlag = false;
        int sizeLimit = mUidsList.size();
        if (sizeLimit > ConstantApp.MAX_PEER_COUNT + 1) {
            sizeLimit = ConstantApp.MAX_PEER_COUNT + 1;
        }
        for (int i = 0; i < sizeLimit; i++) {
            int uid = mGridVideoViewContainer.getItem(i).mUid;
            if (configUid != uid) {
                if (!setRemoteUserPriorityFlag) {
                    setRemoteUserPriorityFlag = true;
                    mRtcEngine.setRemoteUserPriority(uid, Constants.USER_PRIORITY_HIGH);
                    //log.debug("setRemoteUserPriority USER_PRIORITY_HIGH " + mUidsList.size() + " " + (uid & 0xFFFFFFFFL));
                } else {
                    mRtcEngine.setRemoteUserPriority(uid, 100);
                    //log.debug("setRemoteUserPriority USER_PRIORITY_NORANL " + mUidsList.size() + " " + (uid & 0xFFFFFFFFL));
                }
            }
        }
    }

    private void bindToSmallVideoView(int exceptUid) {
        if (mSmallVideoViewDock == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.small_video_view_dock);
            mSmallVideoViewDock = (RelativeLayout) stub.inflate();
        }

        boolean twoWayVideoCall = mUidsList.size() == 2;

        RecyclerView recycler = (RecyclerView) findViewById(R.id.small_video_view_container);

        boolean create = false;

        if (mSmallVideoViewAdapter == null) {
            create = true;
            mSmallVideoViewAdapter = new SmallVideoViewAdapter(this, configUid, exceptUid, mUidsList);
            mSmallVideoViewAdapter.setHasStableIds(true);
        }
        recycler.setHasFixedSize(true);


        if (twoWayVideoCall) {
            recycler.setLayoutManager(new RtlLinearLayoutManager(getApplicationContext(), RtlLinearLayoutManager.HORIZONTAL, false));
        } else {
            recycler.setLayoutManager(new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.HORIZONTAL, false));
        }
        recycler.addItemDecoration(new SmallVideoViewDecoration());
        recycler.setAdapter(mSmallVideoViewAdapter);
        recycler.addOnItemTouchListener(new io.agora.propeller.ui.RecyclerItemClickListener(getBaseContext(), new io.agora.propeller.ui.RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {

            }

            @Override
            public void onItemLongClick(View view, int position) {

            }

            @Override
            public void onItemDoubleClick(View view, int position) {
                onSmallVideoViewDoubleClicked(view, position);
            }
        }));

        recycler.setDrawingCacheEnabled(true);
        recycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

        if (!create) {
            mSmallVideoViewAdapter.setLocalUid(configUid);
            mSmallVideoViewAdapter.notifyUiChanged(mUidsList, exceptUid, null, null);
        }
        for (Integer tempUid : mUidsList.keySet()) {
            if (configUid != tempUid) {
                if (tempUid == exceptUid) {
                    mRtcEngine.setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_HIGH);
                    //log.debug("setRemoteUserPriority USER_PRIORITY_HIGH " + mUidsList.size() + " " + (tempUid & 0xFFFFFFFFL));
                } else {
                    mRtcEngine.setRemoteUserPriority(tempUid, 100);
                    //log.debug("setRemoteUserPriority USER_PRIORITY_NORANL " + mUidsList.size() + " " + (tempUid & 0xFFFFFFFFL));
                }
            }
        }
        recycler.setVisibility(View.VISIBLE);
        mSmallVideoViewDock.setVisibility(View.VISIBLE);
    }

    private void doRenderRemoteUi(final int uid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }

                if (mUidsList.containsKey(uid)) {
                    return;
                }

                /*
                  Creates the video renderer view.
                  CreateRendererView returns the SurfaceView type. The operation and layout of the
                  view are managed by the app, and the Agora SDK renders the view provided by the
                  app. The video display view must be created using this method instead of
                  directly calling SurfaceView.
                 */
                SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
                mUidsList.put(uid, surfaceV);

                boolean useDefaultLayout = mLayoutType == LAYOUT_TYPE_DEFAULT;

                surfaceV.setZOrderOnTop(true);
                surfaceV.setZOrderMediaOverlay(true);

                /*
                  Initializes the video view of a remote user.
                  This method initializes the video view of a remote stream on the local device. It affects only the video view that the local user sees.
                  Call this method to bind the remote video stream to a video view and to set the rendering and mirror modes of the video view.
                 */
                mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                if (useDefaultLayout) {
                    //log.debug("doRenderRemoteUi LAYOUT_TYPE_DEFAULT " + (uid & 0xFFFFFFFFL));
                    switchToDefaultVideoView();
                } else {
                    int bigBgUid = mSmallVideoViewAdapter == null ? uid : mSmallVideoViewAdapter.getExceptedUid();
                    //log.debug("doRenderRemoteUi LAYOUT_TYPE_SMALL " + (uid & 0xFFFFFFFFL) + " " + (bigBgUid & 0xFFFFFFFFL));
                    switchToSmallVideoView(bigBgUid);
                }

                // notifyMessageChanged(new Message(new User(0, null), "video from user " + (uid & 0xFFFFFFFFL) + " decoded"));
            }
        });
    }
}

