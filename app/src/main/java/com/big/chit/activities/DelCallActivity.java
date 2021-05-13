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
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;

import static com.big.chit.activities.GroupCallActivity.LAYOUT_TYPE_DEFAULT;
import static com.big.chit.activities.GroupCallActivity.LAYOUT_TYPE_SMALL;

/**
 * Agora Calling SDK Added by Ussama Iftikhar on 12-April-2021.
 * Email iusama46@gmail.com
 * Email iusama466@gmail.com
 * Github https://github.com/iusama46
 */

public class DelCallActivity extends BaseActivity implements SensorEventListener {
    static final String TAG = DelCallActivity.class.getSimpleName();
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
    private final boolean mIsLandscape = false;
    private final HashMap<Integer, SurfaceView> muIdsList = new HashMap<>();
    public int mLayoutType = LAYOUT_TYPE_DEFAULT;
    String roomId = "room";
    PowerManager.WakeLock wlOff = null, wlOn = null;
    boolean isConnected = false;
    boolean isSpeaker = false;
    ValueEventListener valueEventListener;
    boolean isLoggedIn = false;
    DatabaseReference referenceDb;
    boolean isVideoCall;
    FrameLayout mLocalContainer, mRemoteContainer;
    SurfaceView mLocalView, mRemoteView;
    String roomToken = "";
    String key = "123";
    boolean isDenied = false;
    String receiverToken = "";
    ImageView addPerson;
    User tempUser;
    int configUid = 0;
    FrameLayout remoteVideo2;
    private Chronometer mCallDuration;
    private AudioPlayer mAudioPlayer;
    private String inOrOut;
    private boolean mAddedListener, isMute, alphaInvisible, logSaved;
    private RelativeLayout myCallScreenRootRLY;
    private TextView mCallState, mCallerName, myTxtCalling;
    private ImageView userImage1, userImage2, switchVideo, switchMic, switchVolume;
    private View tintBlue, bottomButtons;
    private RelativeLayout localVideo, remoteVideo;
    private LinearLayout mySwitchCameraLLY;
    private SensorManager mSensorManager;
    private boolean isGroupCall = false;
    private Sensor mProximity;
    private RtcEngine mRtcEngine;
    private int counter = 0;
    private boolean isAndroid = true;
    private GridVideoViewContainer mGridVideoViewContainer;
    private SmallVideoViewAdapter mSmallVideoViewAdapter;
    private RelativeLayout mSmallVideoViewDock;
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onRtcStats(RtcStats stats) {
            super.onRtcStats(stats);
            counter = stats.users;
            //Log.d("clima users", String.valueOf(stats.users));
            if (isConnected) {
                if (stats.users <= 1)
                    endCall();
            }
            if (stats.users > 1) {
                mAudioPlayer.stopProgressTone();
                mCallerName.setText(stats.users + " People");
                isGroupCall = true;
                isConnected = true;
                //Log.d("clima", "ddd");
//                remoteVideo.setVisibility(View.GONE);
//                remoteVideo2.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isLoggedIn = true;
                    if (!inOrOut.equals("IN")) {
                        mAudioPlayer.playProgressTone();
                        pushCallNotification(false, userMe, false);
                        //   callUser();
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
                    mAudioPlayer.stopProgressTone();
                    if (!isVideoCall)
                        if (!mRtcEngine.isSpeakerphoneEnabled())
                            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

                    if (!isConnected) {
                        myTxtCalling.setText(getResources().getString(R.string.app_name) + " Call Connected");

                        mCallDuration.setVisibility(View.VISIBLE);
                        mCallDuration.setFormat("%02d:%02d");
                        mCallDuration.setBase(SystemClock.elapsedRealtime());
                        //mCallDuration.setText("Connected");
                        mCallDuration.start();
                        isConnected = true;

                        remoteVideo2.setVisibility(View.VISIBLE);
                        remoteVideo.removeAllViews();
                        remoteVideo.setVisibility(View.GONE);

                        localVideo.removeAllViews();
                        localVideo.addView(mLocalContainer);
                        localVideo.setVisibility(View.VISIBLE);
                    }
//TODO
//                    if (inOrOut.equals("IN")) {
//                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(userMe.getId()).child(key);
//                        reference.child("call_status").setValue(4);
//                    }
//                    if (isVideoCall)
//                        doRenderRemoteUi(uid);
                    Log.d("clima id", String.valueOf(uid));

                    if (isVideoCall) {
                        if (muIdsList.isEmpty()) {
//
//                            SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
//                            mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid));
//                            surfaceV.setZOrderOnTop(false);
//                            surfaceV.setZOrderMediaOverlay(false);

//                            remoteVideo.setVisibility(View.GONE);
//
//                            muIdsList.put(configUid, surfaceV);
//                            mGridVideoViewContainer.initViewContainer(DelCallActivity.this, uid, muIdsList, mIsLandscape); // first is now full view
                        } else {
                            doRenderRemoteUi(uid);
                        }
                        //remoteVideo2.setVisibility(View.VISIBLE);

                    }
                }
            });
        }


        @Override
        public void onUserOffline(final int uid, final int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    if(counter==2)
//                    endCall();

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
//            if (isVideoCall) {
//                if (muIdsList.isEmpty()) {
//                    //                        remoteVideo.setVisibility(View.VISIBLE);
////                        mRemoteView = RtcEngine.CreateRendererView(getBaseContext());
////                        mRemoteContainer.addView(mRemoteView);
////                        mRtcEngine.setupRemoteVideo(new VideoCanvas(mRemoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
////                        remoteVideo.addView(mRemoteContainer);
//
//                    SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
//                    mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, uid));
//                    surfaceV.setZOrderOnTop(false);
//                    surfaceV.setZOrderMediaOverlay(false);
//                    remoteVideo2.setVisibility(View.VISIBLE);
//                    remoteVideo.setVisibility(View.GONE);
//
//
//                    muIdsList.put(configUid, surfaceV);
//                    mGridVideoViewContainer.initViewContainer(DelCallActivity.this, uid, muIdsList, mIsLandscape); // first is now full view
//                } else {
//
//                    doRenderRemoteUi(uid);
//                }
        }

        //doRemoveRemoteUi(configUid);
        //}

    };

    public static Intent newIntent(Context context, User user, String inOrOut, boolean callIsVideo, String roomId, String roomToken, String key, String receiverToken, boolean isAndroid) {
        Intent intent = new Intent(context, DelCallActivity.class);
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
                            Toast.makeText(DelCallActivity.this, "Something went gone wrong", Toast.LENGTH_SHORT).show();
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
                    //Toast.makeText(DelCallActivity.this, "Notification sent", Toast.LENGTH_SHORT).show();
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
        setContentView(R.layout.activity_call_screen);

        mLocalContainer = new FrameLayout(this);
        mRemoteContainer = new FrameLayout(this);
        mRemoteView = new SurfaceView(this);
        mLocalView = new SurfaceView(this);
        addPerson = findViewById(R.id.add_person);
        addPerson.setVisibility(View.VISIBLE);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

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

        mCallDuration = findViewById(R.id.callDuration);
        mCallerName = findViewById(R.id.remoteUser);
        mCallState = findViewById(R.id.callState);
        userImage1 = findViewById(R.id.userImage1);
        userImage2 = findViewById(R.id.userImage2);
        myTxtCalling = findViewById(R.id.txt_calling);
        tintBlue = findViewById(R.id.tintBlue);
        localVideo = findViewById(R.id.localVideo);
        remoteVideo = findViewById(R.id.remoteVideo);
        remoteVideo2 = findViewById(R.id.groupRemoteVideo);
        switchVideo = findViewById(R.id.switchVideo);
        switchMic = findViewById(R.id.switchMic);
        switchVolume = findViewById(R.id.switchVolume);
        bottomButtons = findViewById(R.id.layout_btns);
        mySwitchCameraLLY = findViewById(R.id.switchVideo_LLY);
        myCallScreenRootRLY = findViewById(R.id.layout_call_screen_root_RLY);

        mAudioPlayer = new AudioPlayer(this);
        onZegoCreated();

        remoteVideo2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAlphaAnimation();
            }
        });

        findViewById(R.id.hangupButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });

        addPerson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent callIntent = new Intent(DelCallActivity.this, ContactActivity.class).putExtra("group", true);
                startActivityForResult(callIntent, 101);
                //startActivity(callIntent);
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {
                tempUser = data.getParcelableExtra("contact");
                if (tempUser != null) {
                    if (tempUser.getId().equals(userMe.getId()))
                        Toast.makeText(this, "You cannot call yourself", Toast.LENGTH_SHORT).show();
                    else
                        pushCallNotification(false, tempUser, true);
                }
            }
        }
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
                //Toast.makeText(DelCallActivity.this, "Calling", Toast.LENGTH_SHORT).show();
                Log.d("clima", "updated");

            }
        });

        referenceDb = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(user.getId());
        try {
            valueEventListener = reference.child(key).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (dataSnapshot.getChildrenCount() > 0) {
                        if (dataSnapshot.getKey().equals(key)) {
                            int value = ((Long) dataSnapshot.child("call_status").getValue()).intValue();
                            if (value == 3) {
                                isDenied = true;
                                Toast.makeText(DelCallActivity.this, "Call Denied", Toast.LENGTH_SHORT).show();
                                endCall();
                            }
                        }
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        } catch (Exception e) {

        }
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

            try {
                if (!inOrOut.equals("IN")) {
                    //todo
                    //DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego").child(user.getId()).child(key);
                    if (isConnected) {
                        //todo
                        //reference.child("call_status").setValue(4);
                    } else {
                        //todo miss call message
                        pushCallNotification(true, userMe, false);
//                        if (isDenied)
//                            reference.child("call_status").setValue(3);
//                        else
//                            reference.child("call_status").setValue(1);
                    }
                }

            } catch (Exception e) {
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


        if (valueEventListener != null)
            referenceDb.removeEventListener(valueEventListener);

        RtcEngine.destroy();
        mRtcEngine = null;
        super.onDestroy();
    }

    private void endCall() {
        finish();
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

    void onZegoCreated() {
        Toast.makeText(this, "dell", Toast.LENGTH_SHORT).show();
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

        //todo room token for incomming
        Log.d("clima cha", roomId);
        Log.d("clima chal", roomToken);

        String uId = userMe.getId().substring(userMe.getId().length() - 5);
        configUid = Integer.parseInt(uId);

        if (isVideoCall) {
            mRtcEngine.enableVideo();
            //mGridVideoViewContainer = (GridVideoViewContainer) findViewById(R.id.grid_video_view_container);
            //remoteVideo2.setVisibility(View.GONE);
            remoteVideo2.setVisibility(View.VISIBLE);
            setVolumeControlStream(AudioManager.STREAM_SYSTEM);
            isSpeaker = true;
            mRtcEngine.enableVideo();


            // Create a SurfaceView object.
            mLocalView = RtcEngine.CreateRendererView(getBaseContext());
            mLocalView.setZOrderMediaOverlay(true);
            mLocalContainer.addView(mLocalView);


            VideoCanvas localVideoCanvas = new VideoCanvas(mLocalView, VideoCanvas.RENDER_MODE_HIDDEN, configUid);
            mRtcEngine.setupLocalVideo(localVideoCanvas);
            localVideo.removeAllViews();
            localVideo.addView(mLocalContainer);
            localVideo.setVisibility(View.VISIBLE);

            SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
            mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, configUid));
            surfaceV.setZOrderOnTop(false);
            surfaceV.setZOrderMediaOverlay(false);
            remoteVideo.removeAllViews();
            remoteVideo.addView(surfaceV);
            remoteVideo.setVisibility(View.VISIBLE);


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

            //SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
            //mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_HIDDEN, configUid));
            //surfaceV.setZOrderOnTop(false);
            //surfaceV.setZOrderMediaOverlay(false);

            muIdsList.put(configUid, mLocalView);
            mGridVideoViewContainer.initViewContainer(this, configUid, muIdsList, mIsLandscape);



        } else {
            mRtcEngine.disableVideo();
            remoteVideo2.setVisibility(View.GONE);
            remoteVideo.setVisibility(View.GONE);
            localVideo.setVisibility(View.GONE);
        }


        mRtcEngine.joinChannel(roomToken, roomId, "Extra Optional Data", configUid);

        isMute();
        enableSpeaker(isSpeaker);

        //String randomSuffix = String.valueOf(new Date().getTime() % (new Date().getTime() / 1000));
//        if (!inOrOut.equals("IN"))
//            roomId = roomId + userMe.getId() + randomSuffix;


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
//                tintBlue.setVisibility(View.INVISIBLE);
//                remoteVideo.setVisibility(View.INVISIBLE);
            }
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
            finish();
        }

        updateUI();

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

    private void updateUI() {

        if (roomId != null) {
            mCallerName.setText(user != null ? user.getNameToDisplay() : user.getId());
            myTxtCalling.setText(getResources().getString(R.string.app_name) + (isVideoCall ? " Video Calling" : " Voice Calling"));

            tintBlue.setVisibility(isVideoCall ? View.GONE : View.VISIBLE);
            localVideo.setVisibility(!isVideoCall ? View.GONE : View.VISIBLE);
        }

        switchVideo.setClickable(true);

        mySwitchCameraLLY.setVisibility(isVideoCall ? View.VISIBLE : View.GONE);
        switchVideo.setAlpha(isVideoCall ? 1f : 0.4f);
        userImage1.setVisibility(isVideoCall ? View.GONE : View.VISIBLE);

        localVideo.setOnTouchListener(new OnDragTouchListener(localVideo, remoteVideo2));
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
        //mRtcEngine.disableAudio()

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

    /////////////////
    private void onBigVideoViewClicked(View view, int position) {
        Log.d("clima", "onBig");
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
                Log.d("clima","hhh1");
                if (isFinishing()) {
                    return;
                }

                Log.d("clima","hhh12");
                Log.d("climaj", String.valueOf(uid));
                Object target = muIdsList.remove(uid);
                if (target == null) {
                    Log.d("clima","hhh12ll");
                    //mGridVideoViewContainer.initViewContainer(DelCallActivity.this, 3353, muIdsList, mIsLandscape);
                    return;
                }
                Log.d("clima",target.toString());
                Log.d("clima","hhh13");
                int bigBgUid = -1;
                if (mSmallVideoViewAdapter != null) {
                    bigBgUid = mSmallVideoViewAdapter.getExceptedUid();
                }
                Log.d("clima","hhh4");
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

    ///////////////////
}