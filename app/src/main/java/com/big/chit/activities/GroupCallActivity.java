package com.big.chit.activities;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Chronometer;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.big.chit.R;
import com.big.chit.Utils;
import com.big.chit.models.Contact;
import com.big.chit.models.Group;
import com.big.chit.models.LogCall;
import com.big.chit.models.Status;
import com.big.chit.models.User;
import com.big.chit.services.SinchService;
import com.big.chit.utils.AudioPlayer;
import com.big.chit.utils.OnDragTouchListener;
import com.big.chit.utils.ScreenHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import im.zego.zegoexpress.ZegoExpressEngine;
import im.zego.zegoexpress.callback.IZegoEventHandler;
import im.zego.zegoexpress.constants.ZegoPlayerState;
import im.zego.zegoexpress.constants.ZegoPublisherState;
import im.zego.zegoexpress.constants.ZegoRoomState;
import im.zego.zegoexpress.constants.ZegoScenario;
import im.zego.zegoexpress.constants.ZegoUpdateType;
import im.zego.zegoexpress.constants.ZegoViewMode;
import im.zego.zegoexpress.entity.ZegoCanvas;
import im.zego.zegoexpress.entity.ZegoRoomConfig;
import im.zego.zegoexpress.entity.ZegoStream;
import im.zego.zegoexpress.entity.ZegoUser;

/**
 * Zego Calling SDK Added by Ussama Iftikhar on 03-April-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */

public class GroupCallActivity extends BaseActivity implements SensorEventListener {
    static final String TAG = GroupCallActivity.class.getSimpleName();
    static final String ADDED_LISTENER = "addedListener";
    private static final String EXTRA_DATA_USER = "extradatauser";
    private static final String EXTRA_DATA_IN_OR_OUT = "extradatainorout";
    private final int mCallDurationSecond = 0;
    String usersIds = " ";
    String callRoomId = "group";
    boolean isSavedID = true;
    PowerManager.WakeLock wlOff = null, wlOn = null;
    ZegoExpressEngine zegoExpressEngine;
    boolean isConnected = false;
    String streamID;
    boolean isSpeaker = false;
    int counter = 0;
    Group group;
    ImageView addPerson;
    boolean isVideoCall;
    boolean useFrontCamera = true;
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
    IZegoEventHandler eventHandler = new IZegoEventHandler() {

        @Override
        public void onRoomStreamUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoStream> streamList, JSONObject extendedData) {
            super.onRoomStreamUpdate(roomID, updateType, streamList, extendedData);
            Log.i("clima", "🚩 🚪 addedd " +
                    streamList.size());
            if (updateType == ZegoUpdateType.ADD) {
                if (isVideoCall) {
                    if (!isAll) {
                        remoteVideo2.setVisibility(View.GONE);
                        scrollView.setVisibility(View.VISIBLE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            remoteVideo.removeAllViews();
                            remoteVideo.addView(gridLayout);
                            initGridLayout();
                        }

                        ZegoCanvas zegoCanvas = new ZegoCanvas(localTextureView);
                        zegoCanvas.viewMode = ZegoViewMode.ASPECT_FILL;
                        streamIdList.add(streamID);
                        viewMap.put(streamID, localTextureView);

                        zegoExpressEngine.startPlayingStream(streamID, zegoCanvas);

                        isAll = true;
                    }

                    for (ZegoStream zegoStream : streamList) {

                        TextureView addTextureView = new TextureView(GroupCallActivity.this);
                        int row = streamIdList.size() / 2;
                        int column = streamIdList.size() % 2;
                        addToGridLayout(row, column, addTextureView);
                        viewMap.put(zegoStream.streamID, addTextureView);
                        streamIdList.add(zegoStream.streamID);
                        ZegoCanvas zegoCanvas = new ZegoCanvas(addTextureView);
                        zegoCanvas.viewMode = ZegoViewMode.ASPECT_FILL;
                        zegoExpressEngine.startPlayingStream(zegoStream.streamID, zegoCanvas);
                    }

                }

            } else if (updateType == ZegoUpdateType.DELETE) {
                Log.i("clima", "🚩 🚪 del stream ");
                //if (!inOrOut.equals("IN")) {
                for (ZegoStream zegoStream : streamList) {
                    zegoExpressEngine.stopPlayingStream(zegoStream.streamID);
                    streamIdList.remove(zegoStream.streamID);
                    notifyGridLayout();
                    viewMap.remove(zegoStream.streamID);
                }
                //}
            }
        }


        @Override
        public void onRoomUserUpdate(String roomID, ZegoUpdateType updateType, ArrayList<ZegoUser> userList) {
            super.onRoomUserUpdate(roomID, updateType, userList);

            if (ZegoUpdateType.ADD == updateType) {
                counter = counter + 1;
                Log.i("clima", "🚩 🚪 added ");

                if (!isConnected) {
                    mAudioPlayer.stopProgressTone();

                    if (!isVideoCall)
                        zegoExpressEngine.startPlayingStream(streamID, new ZegoCanvas(null));
                   // Toast.makeText(GroupCallActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                    myTxtCalling.setText(getResources().getString(R.string.app_name) + " Call Connected");

                    mCallDuration.setVisibility(View.VISIBLE);
                    mCallDuration.setFormat("%02d:%02d");
                    mCallDuration.setBase(SystemClock.elapsedRealtime());
                    //mCallDuration.setText("Connected");
                    mCallDuration.start();
                }
                isConnected = true;

                if (counter > 1) {
                    try {
                        userCall.child(userList.get(counter).userID).removeValue();
                        callLog = callLog + " " + userList.get(counter).userID.toString();
                    } catch (Exception e) {
                    }
                }

            } else if (ZegoUpdateType.DELETE == updateType) {
                Log.i("clima", "🚩 🚪 del ");
                int temp = counter - 1;
                counter = counter - 1;
                if (isConnected) {
                    //mCallerName.setText(counter + " People");
                    if (temp <= 1) {
                        if (!isVideoCall)
                            zegoExpressEngine.startPlayingStream(streamID, new ZegoCanvas(null));
                        Log.i("clima", "🚩 🚪 del last " + roomID);
                        endCall();
                    }
                }

            }
            try {
                mCallerName.setText(counter + " People");
            } catch (Exception e) {
            }
        }

        @Override
        public void onRoomStateUpdate(String roomID, ZegoRoomState state, int errorCode, JSONObject extendedData) {
            if (state == ZegoRoomState.CONNECTED && errorCode == 0) {
                Log.i("clima", "🚩 🚪 Login room success");
                isLoggedIn = true;

                if (!isVideoCall)
                    setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                else
                    setVolumeControlStream(AudioManager.STREAM_SYSTEM);

                if (!inOrOut.equals("IN"))
                    mAudioPlayer.playProgressTone();

            }

            if (errorCode != 0) {

                Log.i("clima", "🚩 ❌ 🚪 Login room fail, errorCode: " + errorCode);
                isLoggedIn = false;

                endCall();

            }
        }


        @Override
        public void onRoomOnlineUserCountUpdate(String roomID, int count) {
            Log.i("clima", "🚩 users,  " + count);
            super.onRoomOnlineUserCountUpdate(roomID, count);

            counter = count;
            if (isConnected) {
                if (count <= 1)
                    endCall();
            }
            if (count > 1) {
                mAudioPlayer.stopProgressTone();
                if (!isVideoCall)
                    zegoExpressEngine.startPlayingStream(streamID, new ZegoCanvas(null));
                mCallerName.setText(count + " People");
                isConnected = true;
            }
        }

        @Override
        public void onPublisherStateUpdate(String streamID, ZegoPublisherState state, int errorCode, JSONObject extendedData) {
            if (state == ZegoPublisherState.PUBLISHING && errorCode == 0) {
                Log.i("clima", "🚩 📤 Publishing stream success");
            }

            if (errorCode != 0) {
                Log.i("clima", "🚩 ❌ 📤 Publishing stream fail, errorCode: " + errorCode);
            }
        }

        @Override
        public void onPlayerStateUpdate(String streamID, ZegoPlayerState state, int errorCode, JSONObject extendedData) {
            if (state == ZegoPlayerState.PLAYING && errorCode == 0) {
                Log.i("clima", "🚩 📥 Playing stream success");
                mAudioPlayer.stopProgressTone();
                if (!isVideoCall)
                    setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            }

            if (errorCode != 0) {

                Log.i("clima", "🚩 ❌ 📥 Playing stream fail, errorCode: " + errorCode);
            }
        }


    };

    public static Intent newIntent(Context context, Group user, String callId, String inOrOut, boolean isVideoCall) {
        Intent intent = new Intent(context, GroupCallActivity.class);
        intent.putExtra(EXTRA_DATA_USER, user);
        intent.putExtra(EXTRA_DATA_IN_OR_OUT, inOrOut);
        intent.putExtra(SinchService.CALL_ID, callId);
        intent.putExtra("isVideoCall", isVideoCall);

        return intent;
    }

    public static Intent newIntent(Context context, String callId, String inOrOut) {
        Intent intent = new Intent(context, GroupCallActivity.class);

        intent.putExtra(EXTRA_DATA_IN_OR_OUT, inOrOut);
        intent.putExtra(SinchService.CALL_ID, callId);
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
        mCallId = intent.getStringExtra(SinchService.CALL_ID);
        inOrOut = intent.getStringExtra(EXTRA_DATA_IN_OR_OUT);
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
            zegoExpressEngine.stopPlayingStream(streamID);
            zegoExpressEngine.stopPublishingStream();
            zegoExpressEngine.stopPreview();
            zegoExpressEngine.logoutRoom(callRoomId);
        }

        ZegoExpressEngine.destroyEngine(null);

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

        try {
            if (!inOrOut.equals("IN")) {
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");
                if (isConnected) {
                    reference.child(userMe.getId()).removeValue();
                } else {
                    if (tempUser != null && !callLog.contains(tempUser.getId())) {
                        HashMap<String, Object> datamap = new HashMap<>();
                        datamap.put("name", group.getName());
                        datamap.put("streamId", streamID);
                        datamap.put("callerId", user.getId());
                        datamap.put("answered", false);
                        datamap.put("connected", isConnected);
                        datamap.put("canceled", false);
                        datamap.put("video", isVideoCall);
                        datamap.put("uId", FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
                        datamap.put("room", callRoomId);
                        datamap.put("isGroup", true);
                        reference.child(tempUser.getId()).setValue(datamap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //Toast.makeText(GroupCallActivity.this, "updated", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    HashMap<String, Object> datamap = new HashMap<>();
                    datamap.put("name", group.getName());
                    datamap.put("streamId", streamID);
                    datamap.put("callerId", user.getId());
                    datamap.put("answered", false);
                    datamap.put("connected", isConnected);
                    datamap.put("canceled", false);
                    datamap.put("video", isVideoCall);
                    datamap.put("uId", FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());
                    datamap.put("room", callRoomId);
                    datamap.put("isGroup", true);
                    for (int i = 0; i < group.getUserIds().size(); i++) {

                        if (!userMe.getId().equals(group.getUserIds().get(i)) && !callLog.contains(group.getUserIds().get(i))) {

                            reference.child(group.getUserIds().get(i).toString()).setValue(datamap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    //Toast.makeText(GroupCallActivity.this, "updated", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }


                }
            } else {
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(userMe.getId());
                reference.removeValue();
            }
        } catch (Exception e) {
        }
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

        zegoExpressEngine = ZegoExpressEngine.createEngine(Utils.appID, Utils.appSign, Utils.isTestEnv, ZegoScenario.COMMUNICATION, getApplication(), eventHandler);
        if (isVideoCall) {
            zegoExpressEngine.enableCamera(true);
            zegoExpressEngine.muteMicrophone(false);
            zegoExpressEngine.muteSpeaker(false);
            setVolumeControlStream(AudioManager.STREAM_SYSTEM);


            viewMap = new HashMap<>();
            streamIdList = new ArrayList<>();
            gridLayout = new GridLayout(this);

        } else {
            zegoExpressEngine.enableCamera(false);
            zegoExpressEngine.setAudioRouteToSpeaker(false);
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        }


        String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));

        if (!inOrOut.equals("IN"))
            callRoomId = callRoomId + userMe.getId() + randomSuffix;
        else {
            callRoomId = MainActivity.RoomId;
        }


        isMute();
        enableSpeaker(isSpeaker);

        if (!isVideoCall && !inOrOut.equals("IN"))
            streamID = userMe.getId();
        else if (!isVideoCall && inOrOut.equals("IN"))
            streamID = MainActivity.callerId;

        if (isVideoCall) {
            streamID = userMe.getId();
        }


        if (isVideoCall) {
            remoteVideo2.setVisibility(View.VISIBLE);
            remoteVideo2.removeAllViews();
            remoteVideo2.addView(gridTextureView);

            ZegoCanvas zegoCanvas = new ZegoCanvas(gridTextureView);
            zegoCanvas.viewMode = ZegoViewMode.ASPECT_FILL;
            zegoExpressEngine.startPreview(zegoCanvas);
        }

        ZegoRoomConfig config = new ZegoRoomConfig();
        config.isUserStatusNotify = true;
        config.maxMemberCount = 8;
        ZegoUser userZego = new ZegoUser(userMe.getId(), userMe.getName());

        zegoExpressEngine.loginRoom(callRoomId, userZego, config);

        if (isVideoCall) {
            zegoExpressEngine.startPublishingStream(streamID);
        }


        if (!inOrOut.equals("IN")) {

            if (!isVideoCall)
                zegoExpressEngine.startPublishingStream(streamID);

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
                useFrontCamera = !useFrontCamera;
                zegoExpressEngine.useFrontCamera(useFrontCamera);
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
        if (isVideoCall)
            zegoExpressEngine.muteSpeaker(enable);
        else
            zegoExpressEngine.setAudioRouteToSpeaker(enable);

        switchVolume.setImageDrawable(ContextCompat.getDrawable(this, !isSpeaker ? R.drawable.ic_speaker : R.drawable.ic_speaker_off));
    }

    private void isMute() {
        if (isMute) {
            zegoExpressEngine.muteMicrophone(true);
            switchMic.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_off));
        } else {
            zegoExpressEngine.muteMicrophone(false);
            switchMic.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_mic_on));
        }
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
}
