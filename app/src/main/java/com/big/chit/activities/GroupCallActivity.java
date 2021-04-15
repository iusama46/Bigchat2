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
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.big.chit.R;
import com.big.chit.Utils;
import com.big.chit.models.Contact;
import com.big.chit.models.Group;
import com.big.chit.models.LogCall;
import com.big.chit.models.Status;
import com.big.chit.models.User;
import com.big.chit.utils.AudioPlayer;
import com.big.chit.utils.OnDragTouchListener;
import com.big.chit.utils.ScreenHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final int mCallDurationSecond = 0;
    String usersIds = " ";
    String callRoomId = "group";
    boolean isSavedID = true;
    PowerManager.WakeLock wlOff = null, wlOn = null;

    boolean isConnected = false;
    String streamID;
    boolean isSpeaker = false;
    int counter = 0;
    Group group;
    ImageView addPerson;
    boolean isVideoCall;

    TextureView localTextureView;
    User tempUser;
    DatabaseReference userCall;
    String callLog = " ";
    boolean isLoggedIn = false;
    GridLayout gridLayout;
    RelativeLayout remoteVideo2;
    TextureView gridTextureView;
    ScrollView scrollView;
    boolean isAll = false;
    FrameLayout mLocalContainer, mRemoteContainer;
    SurfaceView mLocalView, mRemoteView;
    private RtcEngine mRtcEngine;
    private AudioPlayer mAudioPlayer;
    private String mCallId, inOrOut;
    private boolean mAddedListener, mLocalVideoViewAdded, mRemoteVideoViewAdded, isMute, alphaInvisible, logSaved;
    private RelativeLayout myCallScreenRootRLY;
    private TextView mCallState, mCallerName, myTxtCalling;
    private Chronometer mCallDuration;
    private ImageView userImage1, userImage2, switchVideo, switchMic, switchVolume;
    private View tintBlue, bottomButtons;
    private RelativeLayout localVideo, remoteVideo;
    private LinearLayout mySwitchCameraLLY;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private Map<String, TextureView> viewMap;
    private List<String> streamIdList;
    private String receiverToken, roomToken;
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
                        //if (!isVideoCall)
                        //zegoExpressEngine.startPlayingStream(streamID, new ZegoCanvas(null));
                        mCallerName.setText(stats.users + " People");
                        isConnected = true;
                    }
                    Toast.makeText(GroupCallActivity.this, "users " + stats.users, Toast.LENGTH_SHORT).show();
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
                    else
                        setVolumeControlStream(AudioManager.STREAM_SYSTEM);


                    if (!inOrOut.equals("IN")) {
                        mAudioPlayer.playProgressTone();
                        pushNotification(false);
                    }
                    //Toast.makeText(CallScreenActivity.this, "onJOinChannel", Toast.LENGTH_SHORT).show();
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

                    if (!isVideoCall)
                        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                    else
                        setVolumeControlStream(AudioManager.STREAM_SYSTEM);

                    mAudioPlayer.stopProgressTone();
                    if (!isVideoCall)
                        if (!mRtcEngine.isSpeakerphoneEnabled())
                            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

                    //Toast.makeText(CallScreenActivity.this, "onUersJoined", Toast.LENGTH_SHORT).show();

                    //Toast.makeText(CallScreenActivity.this, "Connected", Toast.LENGTH_SHORT).show();

                    if (!isConnected) {
                        //    mAudioPlayer.stopProgressTone();

                        if (!isVideoCall)
                            //zegoExpressEngine.startPlayingStream(streamID, new ZegoCanvas(null));
                            // Toast.makeText(GroupCallActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                            myTxtCalling.setText(getResources().getString(R.string.app_name) + " Call Connected");

                        mCallDuration.setVisibility(View.VISIBLE);
                        mCallDuration.setFormat("%02d:%02d");
                        mCallDuration.setBase(SystemClock.elapsedRealtime());
                        //mCallDuration.setText("Connected");
                        mCallDuration.start();
                    }
                    isConnected = true;

//                    if (counter > 1) {
//                        try {
//                            userCall.child(userList.get(counter).userID).removeValue();
//                            callLog = callLog + " " + userList.get(counter).userID;
//                        } catch (Exception e) {
//                        }
//                    }


//                    myTxtCalling.setText(getResources().getString(R.string.app_name) + " Call Connected");
//                    mCallDuration.setVisibility(View.VISIBLE);
//                    mCallDuration.setFormat("%02d:%02d");
//                    mCallDuration.setBase(SystemClock.elapsedRealtime());
//                    //mCallDuration.setText("Connected");
//                    mCallDuration.start();
//                    isConnected = true;
//
//                    if (isVideoCall) {
//                        remoteVideo.removeAllViews();
//                        remoteVideo.setVisibility(View.VISIBLE);
//                        mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
//                        mRemoteContainer.addView(mRemoteView);
//                        mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
//                        remoteVideo.addView(mRemoteContainer);
//
//                        localVideo.removeAllViews();
//                        localVideo.addView(mLocalContainer);
//                        localVideo.setVisibility(View.VISIBLE);
//                    }

//                    if (updateType == ZegoUpdateType.ADD) {
//                        if (isVideoCall) {
//                            if (!isAll) {
//                                remoteVideo2.setVisibility(View.GONE);
//                                scrollView.setVisibility(View.VISIBLE);
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                                    remoteVideo.removeAllViews();
//                                    remoteVideo.addView(gridLayout);
//                                    initGridLayout();
//                                }
//
//                                ZegoCanvas zegoCanvas = new ZegoCanvas(localTextureView);
//                                zegoCanvas.viewMode = ZegoViewMode.ASPECT_FILL;
//                                streamIdList.add(streamID);
//                                viewMap.put(streamID, localTextureView);
//
//                                zegoExpressEngine.startPlayingStream(streamID, zegoCanvas);
//
//                                isAll = true;
//                            }
//
//                            for (ZegoStream zegoStream : streamList) {
//
//                                TextureView addTextureView = new TextureView(GroupCallActivity.this);
//                                int row = streamIdList.size() / 2;
//                                int column = streamIdList.size() % 2;
//                                addToGridLayout(row, column, addTextureView);
//                                viewMap.put(zegoStream.streamID, addTextureView);
//                                streamIdList.add(zegoStream.streamID);
//                                ZegoCanvas zegoCanvas = new ZegoCanvas(addTextureView);
//                                zegoCanvas.viewMode = ZegoViewMode.ASPECT_FILL;
//                                zegoExpressEngine.startPlayingStream(zegoStream.streamID, zegoCanvas);
//                            }
//
//                        }
//
//                    } else if (updateType == ZegoUpdateType.DELETE) {
//                        Log.i("clima", "🚩 🚪 del stream ");
//                        //if (!inOrOut.equals("IN")) {
//                        for (ZegoStream zegoStream : streamList) {
//                            zegoExpressEngine.stopPlayingStream(zegoStream.streamID);
//                            streamIdList.remove(zegoStream.streamID);
//                            notifyGridLayout();
//                            viewMap.remove(zegoStream.streamID);
//                        }
//                        //}
                    // }
                }
            });
        }


        @Override
        public void onUserOffline(final int uid, final int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }


    };

    public static Intent newIntent(Context context, Group user, String callId, String inOrOut, boolean callIsVideo, String token, String roomToken) {
        Intent intent = new Intent(context, GroupCallActivity.class);
        intent.putExtra(EXTRA_DATA_USER, user);
        intent.putExtra(EXTRA_DATA_IN_OR_OUT, inOrOut);
        intent.putExtra("CALL_ID", callId);
        intent.putExtra("isVideoCall", callIsVideo);
        intent.putExtra("token", token);
        intent.putExtra("room_token", roomToken);

        return intent;
    }

//    public static Intent newIntent(Context context, String callId, String inOrOut) {
//        Intent intent = new Intent(context, GroupCallActivity.class);
//
//        intent.putExtra(EXTRA_DATA_IN_OR_OUT, inOrOut);
//        intent.putExtra("CALL_ID", callId);
//        return intent;
//    }

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

        localTextureView = new TextureView(this);
        gridTextureView = new TextureView(this);
        remoteVideo2 = findViewById(R.id.remoteVideo2);
        scrollView = findViewById(R.id.scroll);

        Intent intent = getIntent();
        group = intent.getParcelableExtra(EXTRA_DATA_USER);
        mCallId = intent.getStringExtra("CALL_ID");
        inOrOut = intent.getStringExtra(EXTRA_DATA_IN_OR_OUT);
        receiverToken = intent.getStringExtra("token");
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

        if (!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) && isVideoCall) {
            Toast.makeText(this, "Your device isn't supported", Toast.LENGTH_SHORT).show();
            return;
        }


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
                        Toast.makeText(this, "user already exits", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String phNO = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");
                    HashMap<String, Object> datamap = new HashMap<>();
                    datamap.put("name", group.getName());
                    datamap.put("id", group.getId());
                    datamap.put("callerId", "user.getId()");
                    datamap.put("answered", true);
                    datamap.put("uId", phNO);
                    datamap.put("room", callRoomId);
                    datamap.put("isGroup", true);
                    datamap.put("streamId", streamID);
                    datamap.put("video", isVideoCall);
                    datamap.put("canceled", false);

                    reference.child(tempUser.getId()).setValue(datamap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(GroupCallActivity.this, "Added to call", Toast.LENGTH_SHORT).show();
                        }
                    });


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
        mAudioPlayer.stopProgressTone();
        mAudioPlayer.stopRingtone();

        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        if (isLoggedIn) {
            RtcEngine.destroy();
            mRtcEngine = null;
//            zegoExpressEngine.stopPlayingStream(streamID);
//            zegoExpressEngine.stopPublishingStream();
//            zegoExpressEngine.stopPreview();
//            zegoExpressEngine.logoutRoom(callRoomId);
        }

        //ZegoExpressEngine.destroyEngine(null);

        try {
            if (wlOff != null && wlOff.isHeld()) {
                wlOff.release();
            } else if (wlOn != null && wlOn.isHeld()) {
                wlOn.release();
            }
        } catch (RuntimeException ex) {
        }


//        if (valueEventListener != null)
//            referenceDb.removeEventListener(valueEventListener);
        super.onDestroy();
    }

    private void endCall() {
        mAudioPlayer.stopProgressTone();

        mAudioPlayer.stopProgressTone();
        mAudioPlayer.stopRingtone();
        //saveLog();

//        try {
//            if (!inOrOut.equals("IN")) {
//                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");
//                if (isConnected) {
//                    reference.child(userMe.getId()).removeValue();
//                } else {
//                    if (tempUser != null && !callLog.contains(tempUser.getId())) {
//                        HashMap<String, Object> datamap = new HashMap<>();
//                        datamap.put("name", group.getName());
//                        datamap.put("streamId", streamID);
//                        datamap.put("callerId", user.getId());
//                        datamap.put("answered", false);
//                        datamap.put("connected", isConnected);
//                        datamap.put("canceled", false);
//                        datamap.put("video", isVideoCall);
//                        datamap.put("uId", FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
//                        datamap.put("room", callRoomId);
//                        datamap.put("isGroup", true);
//                        reference.child(tempUser.getId()).setValue(datamap).addOnCompleteListener(new OnCompleteListener<Void>() {
//                            @Override
//                            public void onComplete(@NonNull Task<Void> task) {
//                                //Toast.makeText(GroupCallActivity.this, "updated", Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    }
//
//                    HashMap<String, Object> datamap = new HashMap<>();
//                    datamap.put("name", group.getName());
//                    datamap.put("streamId", streamID);
//                    datamap.put("callerId", user.getId());
//                    datamap.put("answered", false);
//                    datamap.put("connected", isConnected);
//                    datamap.put("canceled", false);
//                    datamap.put("video", isVideoCall);
//                    datamap.put("uId", FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
//                    datamap.put("room", callRoomId);
//                    datamap.put("isGroup", true);
//                    for (int i = 0; i < group.getUserIds().size(); i++) {
//
//                        if (!userMe.getId().equals(group.getUserIds().get(i)) && !callLog.contains(group.getUserIds().get(i))) {
//
//                            reference.child(group.getUserIds().get(i)).setValue(datamap).addOnCompleteListener(new OnCompleteListener<Void>() {
//                                @Override
//                                public void onComplete(@NonNull Task<Void> task) {
//                                    //Toast.makeText(GroupCallActivity.this, "updated", Toast.LENGTH_SHORT).show();
//                                }
//                            });
//                        }
//                    }
//
//
//                }
//            } else {
//                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(userMe.getId());
//                reference.removeValue();
//            }
//        } catch (Exception e) {
//        }
        finish();
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

    public void addToGridLayout(int row, int column, TextureView textureView) {

        GridLayout.Spec rowSpec = null;//行
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            rowSpec = GridLayout.spec(row, 1.0f);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            GridLayout.Spec row1 = GridLayout.spec(row, 1.0f);
        }
        GridLayout.Spec columnSpec = null;//列
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            columnSpec = GridLayout.spec(column, 1.0f);
        }
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, columnSpec);
        params.setGravity(Gravity.CENTER);
        params.setMargins(2, 2, 2, 10);//px
        params.height = (int) ((ScreenHelper.getSingleton(this.getApplication()).getScreenWidthPixels() / 2 - 20) * 1.6);//px
        params.width = ScreenHelper.getSingleton(this.getApplication()).getScreenWidthPixels() / 2 - 20;
        gridLayout.addView(textureView, params);
    }

    private void notifyGridLayout() {
        int j = 0;
        gridLayout.removeAllViews();
        for (String streamId : streamIdList) {
            int row = j / 2;
            int column = j % 2;
            addToGridLayout(row, column, viewMap.get(streamId));
            j++;
        }
    }

    private void initGridLayout() {
        gridLayout.setRowCount(4);
        gridLayout.setColumnCount(3);
        addToGridLayout(0, 0, localTextureView);
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

        String accessToken = Utils.token;

        mRtcEngine.joinChannel(accessToken, "testChannel", "Extra Optional Data", 0);
        //zegoExpressEngine = ZegoExpressEngine.createEngine(Utils.appID, Utils.appSign, Utils.isTestEnv, ZegoScenario.COMMUNICATION, getApplication(), eventHandler);
        if (isVideoCall) {
            setVolumeControlStream(AudioManager.STREAM_SYSTEM);
            isSpeaker = true;
            mRtcEngine.enableVideo();
            // Create a SurfaceView object.
            remoteVideo.removeAllViews();
            mLocalView = RtcEngine.CreateRendererView(getBaseContext());
            mLocalView.setZOrderMediaOverlay(true);
            mLocalContainer.addView(mLocalView);
            remoteVideo.addView(mLocalContainer);
            VideoCanvas localVideoCanvas = new VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, 0);
            mRtcEngine.setupLocalVideo(localVideoCanvas);

            viewMap = new HashMap<>();
            streamIdList = new ArrayList<>();
            gridLayout = new GridLayout(this);

        } else {
            mRtcEngine.disableVideo();
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }

        isMute();
        enableSpeaker(isSpeaker);

        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));

        if (!inOrOut.equals("IN"))
            callRoomId = callRoomId + userMe.getId() + randomSuffix;
        else {
            callRoomId = MainActivity.RoomId;
        }


        if (!isVideoCall && !inOrOut.equals("IN"))
            streamID = userMe.getId();
        else if (!isVideoCall && inOrOut.equals("IN"))
            streamID = MainActivity.callerId;

        if (isVideoCall) {
            streamID = userMe.getId();
        }


        if (isVideoCall) {
//            remoteVideo2.setVisibility(View.VISIBLE);
//            remoteVideo2.removeAllViews();
//            remoteVideo2.addView(gridTextureView);
//
//            ZegoCanvas zegoCanvas = new ZegoCanvas(gridTextureView);
//            zegoCanvas.viewMode = ZegoViewMode.ASPECT_FILL;
//            zegoExpressEngine.startPreview(zegoCanvas);
        }

//        ZegoRoomConfig config = new ZegoRoomConfig();
//        config.isUserStatusNotify = true;
//        config.maxMemberCount = 8;
//        ZegoUser userZego = new ZegoUser(userMe.getId(), userMe.getName());
//
//        zegoExpressEngine.loginRoom(callRoomId, userZego, config);

        if (isVideoCall) {
            //  zegoExpressEngine.startPublishingStream(streamID);
        }


        if (!inOrOut.equals("IN")) {

//            if (!isVideoCall)
//                zegoExpressEngine.startPublishingStream(streamID);

            String phNO = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");
            HashMap<String, Object> datamap = new HashMap<>();
            datamap.put("name", group.getName());
            datamap.put("id", group.getId());
            datamap.put("callerId", "user.getId()");
            datamap.put("answered", true);
            datamap.put("uId", phNO);
            datamap.put("room", callRoomId);
            datamap.put("streamId", streamID);
            datamap.put("canceled", false);
            datamap.put("video", isVideoCall);
            datamap.put("isGroup", true);
            //Toast.makeText(GroupCallActivity.this, "Calling", Toast.LENGTH_SHORT).show();
            for (int i = 0; i < group.getUserIds().size(); i++) {
                if (!phNO.equals(group.getUserIds().get(i))) {
                    reference.child(group.getUserIds().get(i)).setValue(datamap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                        }
                    });
                }
            }
        }


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
    }

    private void pushNotification(boolean isMissedCall) {
        try {
            String token = FirebaseInstanceId.getInstance().getToken();
            Log.d("clima", token);
            RequestQueue queue = Volley.newRequestQueue(this);

            String url = "https://fcm.googleapis.com/fcm/send";

            JSONObject notificationObject = new JSONObject();
            notificationObject.put("title", isVideoCall ? "Video Call" : "Voice Call");
            notificationObject.put("body", userMe.getId() + " is calling you");
            Toast.makeText(this, "user" + group.getName(), Toast.LENGTH_SHORT).show();
            JSONObject dataObj = new JSONObject();
            dataObj.put("is_video", isVideoCall);
            dataObj.put("is_group", true);
            dataObj.put("is_call", true);
            dataObj.put("room_id", callRoomId);
            dataObj.put("room_token", "accessToken");
            dataObj.put("missed_call", isMissedCall);
            dataObj.put("caller_id", group.getId());
            dataObj.put("caller_name", group.getName());

            receiverToken = "dSVUQDBjym8:APA91bFAIdeIWwPe97IugDlIpLf24bG5AQ4K0zLFqQoDN5xL307qaTaZjSfQJPAo-uQ_OKzr_hEghvKwv05eUZwpBN3wlKh_0sG6FrmZblEB2Qsrh9qV7NsZDYoPHMyTXa0A49KSeWeQ";
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("notification", notificationObject);
            jsonObject.put("data", dataObj);
            jsonObject.put("to", receiverToken);

            JsonObjectRequest request = new JsonObjectRequest(url, jsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d("clima", response.toString());
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("clima", error.getMessage());
                }
            }) {
                @Override
                public Map<String, String> getHeaders() {
                    String api_key_header_value = "key=AAAAn4Y4Ciw:APA91bHAgDKs1SakEKc-cdMI4LYz7G8O3IZ6odbpKU8h5tu0SmyICpeOhMFeBnwdOsccZVmUDuwZ245PWt_kk09E2fnS78VelY_JbaJ1XtJVNl4Na6QCioeXSoFS4kMvlDHzJ9EJvX1Q";
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", api_key_header_value);
                    return headers;
                }
            };

            queue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
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
}
