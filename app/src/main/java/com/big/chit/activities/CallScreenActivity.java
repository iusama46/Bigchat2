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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
import com.big.chit.models.GroupUser;
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
import com.big.chit.utils.Helper;
import com.big.chit.utils.OnDragTouchListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

public class CallScreenActivity extends BaseActivity implements SensorEventListener {
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
    private final HashMap<Integer, SurfaceView> muIdsList = new HashMap<>();

    private final boolean mIsLandscape = false;
    public int mLayoutType = LAYOUT_TYPE_DEFAULT;
    List<GroupUser> userList = new ArrayList<>();
    List<GroupUser> userInCallList = new ArrayList<>();
    List<String> uIdsList = new ArrayList<>();
    int configUid = 0;
    String roomId = "group";
    PowerManager.WakeLock wlOff = null, wlOn = null;
    boolean isConnected = false;
    boolean isSpeaker = false;
    int counter = 0;
    boolean isUserFound = false;
    ImageView addPerson;
    boolean isVideoCall;
    User tempUser;
    boolean isLoggedIn = false;
    RelativeLayout remoteVideo2;

    String key;
    FrameLayout remoteVideo;

    private SmallVideoViewAdapter mSmallVideoViewAdapter;
    private GridVideoViewContainer mGridVideoViewContainer;
    private RelativeLayout mSmallVideoViewDock;
    private RtcEngine mRtcEngine;
    private AudioPlayer mAudioPlayer;
    private String mCallId, inOrOut;
    private boolean mAddedListener, isMute, alphaInvisible, logSaved;
    private RelativeLayout myCallScreenRootRLY;
    private TextView mCallState, mCallerName, myTxtCalling;
    private Chronometer mCallDuration;
    private ImageView userImage1, userImage2, switchVideo, switchMic, switchVolume;
    private View tintBlue, bottomButtons;
    private RelativeLayout localVideo;
    private LinearLayout mySwitchCameraLLY;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private String roomToken;


    private String receiverToken = "";
    private Boolean isAndroid = false;
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

                    if (!inOrOut.equals("IN")) {
                        mAudioPlayer.playProgressTone();
                        pushCallNotification(false, userMe, false);
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
                        if (isVideoCall)
                            Toast.makeText(CallScreenActivity.this, "Tip: Double tap for small/multiple views", Toast.LENGTH_SHORT).show();
                    }
                    isConnected = true;

                    if (userList.isEmpty()) {
                        userList = getUsers();
                    }

                    if (!userList.isEmpty()) {
                        for (GroupUser groupUser : userList) {
                            if (groupUser.getId().contains(String.valueOf(uid))) {
                                Log.d("clima", groupUser.getId());
                                userInCallList.add(new GroupUser(groupUser.getId(), groupUser.getImage(), uid));
                                uIdsList.add(String.valueOf(uid));
                                updateUsers();

                                if (!isUserFound) {
                                    if (user.getId().equals(groupUser.getId())) {
                                        isUserFound = true;
                                    }
                                }
                                break;
                            }
                        }
                    }

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

                    try {
                        uIdsList.remove(String.valueOf(uid));
                        for (GroupUser groupUser : userInCallList) {
                            if (groupUser.getShortID() == uid) {

                                userInCallList.remove(groupUser);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                    updateUsers();


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

    public static Intent newIntent(Context context, User user, String inOrOut, boolean callIsVideo, String roomId, String roomToken, String key, String receiverToken, boolean isAndroid) {
        Intent intent = new Intent(context, CallScreenActivity.class);
        intent.putExtra(EXTRA_DATA_USER, user);
        intent.putExtra(EXTRA_DATA_IN_OR_OUT, inOrOut);
        intent.putExtra("callIsVideo", callIsVideo);
        intent.putExtra("token", roomId);
        intent.putExtra("room_token", roomToken);
        intent.putExtra("key", key);
        intent.putExtra("receiverToken", receiverToken);
        intent.putExtra("isAndroid", isAndroid);
        return intent;
    }

    void updateUsers() {

        StringBuilder text = new StringBuilder();
        Log.d("clima", String.valueOf(userInCallList.size()));

        if (userInCallList.isEmpty()) {
            return;
        }
        HashMap<String, User> myUsers = new Helper(CallScreenActivity.this).getCacheMyUsers();
        if (userInCallList.size() == 1) {
            if (myUsers != null && myUsers.containsKey(userInCallList.get(0).getId())) {
                text.append(Objects.requireNonNull(myUsers.get(userInCallList.get(0).getId())).getNameToDisplay());
            } else {
                text.append(userInCallList.get(0).getId());
            }
        } else {
            for (int i = 0; i < userInCallList.size(); i++) {
                try {
                    if (myUsers != null && myUsers.containsKey(userInCallList.get(i).getId())) {
                        text.append(Objects.requireNonNull(myUsers.get(userInCallList.get(i).getId())).getNameToDisplay());
                    } else {
                        text.append(userInCallList.get(i).getId());
                    }

                    Log.d("clima index", String.valueOf(i));

                    if (userInCallList.size() - 1 != i)
                        text.append(", ");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        mCallerName.setText(text);
    }

    private List<GroupUser> getUsers() {
        List<GroupUser> tempList = new ArrayList<>();
        try {
            Query reference = FirebaseDatabase.getInstance().getReference().child("data").child("users");//.orderByKey().startAt("72882").endAt("72882\uf8ff");
            reference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    //Log.d("clima data", dataSnapshot.getValue().toString());
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        Log.d("User key", child.getKey());
//                        if(child.getKey().contains(String.valueOf(uid))) {
//                            //Toast.makeText(CallScreenActivity.this, "user " + child.getKey(), Toast.LENGTH_SHORT).show();
//                            //Log.d("clima", child.getKey());
//                            break;
//                        }
                        Log.d("User ref", child.getRef().toString());
                        Log.d("User val", child.getValue().toString());

                        try {
                            if (child.child("image").getValue() != null) {
                                tempList.add(new GroupUser(child.getKey(), Objects.requireNonNull(child.child("image").getValue()).toString()));
                            }
                        } catch (Exception e) {
                        }
                    }


                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        } catch (Exception e) {

        }

        return tempList;
    }

    private void pushCallNotification(boolean isMissedCall, User tempUser, boolean isGroupCall) {
        final String[] tempUserToken = {receiverToken};
        final Boolean[] tempIsAndroid = {isAndroid};
        if (isGroupCall) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("data").child("users").child(tempUser.getId());
            try {
                reference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        try {
                            if (dataSnapshot.exists()) {
                                tempUserToken[0] = Objects.requireNonNull(dataSnapshot.child("deviceToken").getValue()).toString();
                                Log.d("clima token", tempUserToken[0]);

                                String deviceText = Objects.requireNonNull(dataSnapshot.child("osType").getValue()).toString();
                                tempIsAndroid[0] = deviceText.equals("android");

                                pushNotificationToDevice(isMissedCall, tempUserToken[0], tempIsAndroid[0], true);
                            }
                        } catch (Exception e) {
                            Toast.makeText(CallScreenActivity.this, "Something went gone wrong", Toast.LENGTH_SHORT).show();
                            Log.d("clima", "Failed to get token");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            } catch (Exception e) {

            }
        } else {

            pushNotificationToDevice(isMissedCall, tempUserToken[0], tempIsAndroid[0], false);
        }

    }

    private void pushNotificationToDevice(boolean isMissedCall, String tempUserToken, Boolean tempIsAndroid, boolean isAddedInCall) {
        try {
            RequestQueue queue = Volley.newRequestQueue(this);

            String url = "https://fcm.googleapis.com/fcm/send";

            JSONObject dataObj = new JSONObject();

            JSONObject notificationObject = new JSONObject();
            if (!tempIsAndroid) {
                //notificationObject.put("title", isVideoCall ? "Video Call" : "Voice Call");
                notificationObject.put("title", isMissedCall ? userMe.getId() + " gave you missed call" : userMe.getId() + " is calling you");
                notificationObject.put("body", isMissedCall ? "PakOne" : "PakOne Voice Calling...");
                notificationObject.put("mutable_content", true);
                notificationObject.put("sound", "incoming.wav");
                notificationObject.put("content_available", true);
            }

            dataObj.put("isGroupCall", "0");
            dataObj.put("isVideoCall", isVideoCall ? "1" : "0");
            dataObj.put("isAddedInCall", isAddedInCall ? "1" : "0");
            dataObj.put("channelName", roomId);
            dataObj.put("token", roomToken);
            dataObj.put("callStatus", isMissedCall ? "1" : "0");
            dataObj.put("callerId", userMe.getId());
            dataObj.put("callerName", userMe.getName());
            dataObj.put("sound", "incoming.wav");
            dataObj.put("timeStamp", System.currentTimeMillis());
            dataObj.put("alert", "SomeOne is Calling You!");
            dataObj.put("osType", "1");

            JSONObject jsonObject = new JSONObject();

            if (!tempIsAndroid)
                jsonObject.put("notification", notificationObject);
            jsonObject.put("data", dataObj);
            jsonObject.put("to", tempUserToken);

            JsonObjectRequest request = new JsonObjectRequest(url, jsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d("clima", response.toString());
                    //Toast.makeText(CallScreenActivity.this, "Notification sent", Toast.LENGTH_SHORT).show();
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

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        addPerson = findViewById(R.id.add_person);
        addPerson.setVisibility(View.VISIBLE);

        remoteVideo2 = findViewById(R.id.remoteVideo2);

        Intent intent = getIntent();
        user = intent.getParcelableExtra(EXTRA_DATA_USER);

        inOrOut = intent.getStringExtra(EXTRA_DATA_IN_OR_OUT);

        //todo check later for incoming call
        roomToken = intent.getStringExtra("room_token");
        roomId = intent.getStringExtra("token");
        if (inOrOut.equals("IN")) {
            key = intent.getStringExtra("key");
        } else {
            receiverToken = intent.getStringExtra("receiverToken");
            isAndroid = intent.getBooleanExtra("isAndroid", true);

            Log.d("clima", String.format("ostype %s", isAndroid));
        }
        isVideoCall = intent.getBooleanExtra("callIsVideo", false);

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

        onAgoraEngineStarted();

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

        remoteVideo2.setOnClickListener(new View.OnClickListener() {
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
                Intent callIntent = new Intent(CallScreenActivity.this, ContactActivity.class).putExtra("group", true);
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
                    if (tempUser.getId().equals(userMe.getId()))
                        Toast.makeText(this, "You can't call yourself", Toast.LENGTH_SHORT).show();
                    else
                        pushCallNotification(false, tempUser, true);
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

    @Override
    protected void onDestroy() {
        mAudioPlayer.stopProgressTone();
        mAudioPlayer.stopRingtone();

        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
        if (isLoggedIn) {
            mRtcEngine.leaveChannel();
            if (isVideoCall)
                mRtcEngine.stopPreview();
            saveLog();
            if (isVideoCall) {
                mRtcEngine.stopPreview();
                muIdsList.clear();
            }

            if (!inOrOut.equals("IN")) {
                if (!isConnected && !isUserFound) {
                    //todo miss call message
                    pushCallNotification(true, userMe, false);
                }
            }
        }

        try {
            if (wlOff != null && wlOff.isHeld()) {
                wlOff.release();
            } else if (wlOn != null && wlOn.isHeld()) {
                wlOn.release();
            }
        } catch (RuntimeException ex) {
        }

        RtcEngine.destroy();
        mRtcEngine = null;
        super.onDestroy();
    }

    private void endCall() {
        finish();
    }

    private void callUser() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(user.getId());
        key = reference.push().getKey();

        HashMap<String, Object> datamap = new HashMap<>();
        datamap.put("is_video", isVideoCall);
        datamap.put("is_group", false);
        datamap.put("call_status", 0);
        datamap.put("channel_id", roomId);
        datamap.put("channel_token", roomToken);
        datamap.put("caller_id", userMe.getId());
        datamap.put("caller_name", userMe.getName());
        datamap.put("receiver_id", user.getId());
        try {
            datamap.put("timeStamp", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        reference.child(key).setValue(datamap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                //Toast.makeText(CallScreenActivity.this, "Calling", Toast.LENGTH_SHORT).show();
                Log.d("clima", "updated");

            }
        });

//        referenceDb = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(user.getId());
//        try {
//            valueEventListener = reference.child(key).addValueEventListener(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//
//                    if (dataSnapshot.getChildrenCount() > 0) {
//                        if (dataSnapshot.getKey().equals(key)) {
//                            int value = ((Long) dataSnapshot.child("call_status").getValue()).intValue();
//                            if (value == 3) {
//                                isDenied = true;
//                                Toast.makeText(CallScreenActivity.this, "Call Denied", Toast.LENGTH_SHORT).show();
//                                endCall();
//                            }
//                        }
//                    }
//
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError databaseError) {
//                }
//            });
//        } catch (Exception e) {
//
//        }
    }

    private void saveLog() {
        if (!logSaved) {
            rChatDb.beginTransaction();
            rChatDb.copyToRealm(new LogCall(user, System.currentTimeMillis(), 0,
                    isVideoCall, inOrOut, userMe.getId(), user.getId()));
            rChatDb.commitTransaction();
            logSaved = true;
        }
    }

    void onAgoraEngineStarted() {
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
            Toast.makeText(this, "NEED TO check rtc sdk init fatal error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("clima roomId", roomId);
        Log.d("clima  roomToken", roomToken);

        String uId = userMe.getId().substring(userMe.getId().length() - 6);
        configUid = Integer.parseInt(uId);

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
            mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, configUid));
            surfaceV.setZOrderOnTop(false);
            surfaceV.setZOrderMediaOverlay(false);

            muIdsList.put(configUid, surfaceV);
            mGridVideoViewContainer.initViewContainer(this, configUid, muIdsList, mIsLandscape); // first is now full view

        }
        mRtcEngine.joinChannel(roomToken, roomId, "Extra Optional Data", configUid);
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

        mCallerName.setText(user.getName());
        if (user != null) {
            mCallerName.setText(user != null ? user.getNameToDisplay() : user.getId());
            mCallState.setText("call.getState().toString()");
            if (user != null) {
                if (user.getImage() != null && !user.getImage().isEmpty()) {
                    if (user.getBlockedUsersIds() != null
                            && !user.getBlockedUsersIds().contains(userMe.getId()))
                        Picasso.get()
                                .load(user.getImage())
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage1);
                    else
                        Picasso.get()
                                .load(R.drawable.ic_avatar)
                                .tag(this)
                                .error(R.drawable.ic_avatar)
                                .placeholder(R.drawable.ic_avatar)
                                .into(userImage1);

                    Picasso.get()
                            .load(user.getImage())
                            .tag(this)
                            .error(R.drawable.ic_avatar)
                            .placeholder(R.drawable.ic_avatar)
                            .into(userImage2);
                } else {
                    userImage1.setBackgroundResource(R.drawable.ic_avatar);
                    userImage2.setBackgroundResource(R.drawable.ic_avatar);
                }
            }
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
            finish();
        }
        updateUI();
        userList = getUsers();
        if (!inOrOut.equals("IN"))
            callUser();

    }

    void updateUI() {
        if (roomId != null) {
            mCallerName.setText(user != null ? user.getNameToDisplay() : user.getId());
            myTxtCalling.setText(String.format("%s%s", getResources().getString(R.string.app_name), isVideoCall ? " Video Calling" : " Voice Calling"));

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
        addPerson.startAnimation(animation1);

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

    }

    private void onBigVideoViewDoubleClicked(View view, int position) {
        if (muIdsList.size() < 2) {
            return;
        }


        UserStatusData user = mGridVideoViewContainer.getItem(position);
        int uid = (user.mUid == 0) ? configUid : user.mUid;

        if (mLayoutType == LAYOUT_TYPE_DEFAULT && muIdsList.size() != 1) {
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
        slice.put(bigBgUid, muIdsList.get(bigBgUid));
        Iterator<SurfaceView> iterator = muIdsList.values().iterator();
        while (iterator.hasNext()) {
            SurfaceView s = iterator.next();
            s.setZOrderOnTop(true);
            s.setZOrderMediaOverlay(true);
        }

        muIdsList.get(bigBgUid).setZOrderOnTop(false);
        muIdsList.get(bigBgUid).setZOrderMediaOverlay(false);

        mGridVideoViewContainer.initViewContainer(this, bigBgUid, slice, mIsLandscape);

        bindToSmallVideoView(bigBgUid);

        mLayoutType = LAYOUT_TYPE_SMALL;

        requestRemoteStreamType(muIdsList.size());
    }


    private void doHideTargetView(int targetUid, boolean hide) {
        HashMap<Integer, Integer> status = new HashMap<>();
        status.put(targetUid, hide ? UserStatusData.VIDEO_MUTED : UserStatusData.DEFAULT_STATUS);
        if (mLayoutType == LAYOUT_TYPE_DEFAULT) {
            mGridVideoViewContainer.notifyUiChanged(muIdsList, targetUid, status, null);
        } else if (mLayoutType == LAYOUT_TYPE_SMALL) {
            UserStatusData bigBgUser = mGridVideoViewContainer.getItem(0);
            if (bigBgUser.mUid == targetUid) { // big background is target view
                mGridVideoViewContainer.notifyUiChanged(muIdsList, targetUid, status, null);
            } else { // find target view in small video view list

                mSmallVideoViewAdapter.notifyUiChanged(muIdsList, bigBgUser.mUid, status, null);
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

                Object target = muIdsList.remove(uid);
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

            }
        });
    }


    private void requestRemoteStreamType(final int currentHostCount) {
    }

    private void switchToDefaultVideoView() {
        if (mSmallVideoViewDock != null) {
            mSmallVideoViewDock.setVisibility(View.GONE);
        }
        //todo
        mGridVideoViewContainer.initViewContainer(this, configUid, muIdsList, mIsLandscape);

        mLayoutType = LAYOUT_TYPE_DEFAULT;
        boolean setRemoteUserPriorityFlag = false;
        int sizeLimit = muIdsList.size();
        if (sizeLimit > ConstantApp.MAX_PEER_COUNT + 1) {
            sizeLimit = ConstantApp.MAX_PEER_COUNT + 1;
        }
        for (int i = 0; i < sizeLimit; i++) {
            int uid = mGridVideoViewContainer.getItem(i).mUid;
            if (configUid != uid) {
                if (!setRemoteUserPriorityFlag) {
                    setRemoteUserPriorityFlag = true;
                    mRtcEngine.setRemoteUserPriority(uid, Constants.USER_PRIORITY_HIGH);

                } else {
                    mRtcEngine.setRemoteUserPriority(uid, 100);

                }
            }
        }
    }

    private void bindToSmallVideoView(int exceptUid) {
        if (mSmallVideoViewDock == null) {
            ViewStub stub = (ViewStub) findViewById(R.id.small_video_view_dock);
            mSmallVideoViewDock = (RelativeLayout) stub.inflate();
        }

        boolean twoWayVideoCall = muIdsList.size() == 2;

        RecyclerView recycler = (RecyclerView) findViewById(R.id.small_video_view_container);

        boolean create = false;

        if (mSmallVideoViewAdapter == null) {
            create = true;
            mSmallVideoViewAdapter = new SmallVideoViewAdapter(this, configUid, exceptUid, muIdsList);
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
            mSmallVideoViewAdapter.notifyUiChanged(muIdsList, exceptUid, null, null);
        }
        for (Integer tempUid : muIdsList.keySet()) {
            if (configUid != tempUid) {
                if (tempUid == exceptUid) {
                    mRtcEngine.setRemoteUserPriority(tempUid, Constants.USER_PRIORITY_HIGH);

                } else {
                    mRtcEngine.setRemoteUserPriority(tempUid, 100);

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

                if (muIdsList.containsKey(uid)) {
                    return;
                }

                SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
                muIdsList.put(uid, surfaceV);

                boolean useDefaultLayout = mLayoutType == LAYOUT_TYPE_DEFAULT;

                surfaceV.setZOrderOnTop(true);
                surfaceV.setZOrderMediaOverlay(true);

                mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid));

                if (useDefaultLayout) {
                    switchToDefaultVideoView();
                } else {
                    int bigBgUid = mSmallVideoViewAdapter == null ? uid : mSmallVideoViewAdapter.getExceptedUid();

                    switchToSmallVideoView(bigBgUid);
                }
            }
        });
    }
}