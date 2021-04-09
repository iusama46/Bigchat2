package com.big.chit.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class SinchService extends Service {
    public static final String CALL_ID = "CALL_ID";
    static final String TAG = SinchService.class.getSimpleName();
    private static final String APP_KEY = "6ba328fe-d9fa-4494-b238-2bf432573290";
    private static final String APP_SECRET = "7nhsPq7N506cGW+VsKCaQw==";
    private static final String ENVIRONMENT = "clientapi.sinch.com";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
//    private SinchServiceInterface mSinchServiceInterface = new SinchServiceInterface();
//    private SinchClient mSinchClient;
//    private String mUserId;
//
//    private StartFailedListener mListener;
//    private Helper helper;
//    private User userMe;
//
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        helper = new Helper(this);
//        userMe = helper.getLoggedInUser();
//        if (User.validate(userMe)) {
//            start(userMe.getId());
//        }
//    }
//
//    @Override
//    public void onDestroy() {
//        if (mSinchClient != null && mSinchClient.isStarted()) {
//            mSinchClient.terminate();
//        }
//        super.onDestroy();
//    }
//
//    private void start(String userName) {
//        if (mSinchClient == null) {
//            mUserId = userName;
//            mSinchClient = Sinch.getSinchClientBuilder().context(getApplicationContext()).userId(userName)
//                    .applicationKey(APP_KEY)
//                    .applicationSecret(APP_SECRET)
//                    .environmentHost(ENVIRONMENT).build();
//
//            mSinchClient.setSupportCalling(true);
////            mSinchClient.setSupportManagedPush(true);
//            mSinchClient.setSupportActiveConnectionInBackground(true);
//            mSinchClient.startListeningOnActiveConnection();
//
//            mSinchClient.addSinchClientListener(new MySinchClientListener());
//            // Permission READ_PHONE_STATE is needed to respect native calls.
//            mSinchClient.getCallClient().setRespectNativeCalls(false);
//            mSinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());
//
//            mSinchClient.start();
//        }
//    }
//
//    private void stop() {
//        if (mSinchClient != null) {
//            mSinchClient.terminate();
//            mSinchClient = null;
//        }
//    }
//
//    private boolean isStarted() {
//        return (mSinchClient != null && mSinchClient.isStarted());
//    }



    /* @Override
     public int onStartCommand(Intent intent, int flags, int startId) {

         Notification notification = new NotificationCompat.Builder(this)
                 .setSmallIcon(R.drawable.ic_logo_)
                 .setContentText(getString(R.string.app_name))
                 .build();
         startForeground(100, notification);


         return START_STICKY;
     }
 */


//}