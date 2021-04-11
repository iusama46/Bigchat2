package com.big.chit.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.big.chit.R;
import com.big.chit.adapters.MenuUsersRecyclerAdapter;
import com.big.chit.adapters.ViewPagerAdapter;
import com.big.chit.fragments.GroupCreateDialogFragment;
import com.big.chit.fragments.MyCallsFragment;
import com.big.chit.fragments.MyGroupsFragment;
import com.big.chit.fragments.MyStatusFragmentNew;
import com.big.chit.fragments.MyUsersFragment;
import com.big.chit.fragments.UserSelectDialogFragment;
import com.big.chit.interfaces.ContextualModeInteractor;
import com.big.chit.interfaces.HomeIneractor;
import com.big.chit.interfaces.OnUserGroupItemClick;
import com.big.chit.interfaces.UserGroupSelectionDismissListener;
import com.big.chit.models.Contact;
import com.big.chit.models.Group;
import com.big.chit.models.LogCall;
import com.big.chit.models.Message;
import com.big.chit.models.Status;
import com.big.chit.models.User;
import com.big.chit.services.FetchMyUsersService;
import com.big.chit.utils.Helper;
import com.big.chit.views.SwipeControlViewPager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mxn.soul.flowingdrawer_core.ElasticDrawer;
import com.mxn.soul.flowingdrawer_core.FlowingDrawer;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import io.realm.Realm;

public class MainActivity extends BaseActivity implements HomeIneractor, OnUserGroupItemClick, View.OnClickListener, ContextualModeInteractor, UserGroupSelectionDismissListener {
    private static final int REQUEST_CODE_CHAT_FORWARD = 99;
    private static final int REQUEST_PERMISSION_CALL = 951;
    private static final String CHANNEL_ID_USER_MISSCALL = "my_channel_04";
    public static ArrayList<User> myUsers = new ArrayList<>();
    public static String userId;
    public static String callerId;
    public static String RoomId;
    public static String name;
    private static String USER_SELECT_TAG = "userselectdialog";
    private static String OPTIONS_MORE = "optionsmore";
    private static String GROUP_CREATE_TAG = "groupcreatedialog";
    private static String CONFIRM_TAG = "confirmtag";
    private final int CONTACTS_REQUEST_CODE = 321;
    private ImageView usersImage, backImage, dialogUserImage;
    private RecyclerView menuRecyclerView;
    private SwipeRefreshLayout swipeMenuRecyclerView;
    private FlowingDrawer drawerLayout;
    private EditText searchContact;
    private TextView invite, selectedCount;
    private RelativeLayout toolbarContainer, cabContainer;
    private TabLayout tabLayout;
    private SwipeControlViewPager viewPager;
    private FloatingActionButton floatingActionButton;
    private CoordinatorLayout coordinatorLayout;
    private MenuUsersRecyclerAdapter menuUsersRecyclerAdapter;
    private ArrayList<Contact> contactsData = new ArrayList<>();
    private ArrayList<Group> myGroups = new ArrayList<>();
    private ArrayList<Status> myStatus = new ArrayList<>();
    private ArrayList<Message> messageForwardList = new ArrayList<>();
    private UserSelectDialogFragment userSelectDialogFragment;
    private ViewPagerAdapter adapter;

    private ProgressDialog dialog;

    public DatabaseReference getDatabaseRef() {
        return statusRef;
    }
    /*private void loadAdd() {
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }*/

    public DatabaseReference getUserDatabaseRef() {
        return usersRef;
    }

    public Realm getRealmRef() {
        return rChatDb;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUi();
        userId = userMe.getId();
        //setup recyclerview in drawer layout
        setupMenu();
        setProfileImage(usersImage);
        usersImage.setOnClickListener(this);
        backImage.setOnClickListener(this);
        invite.setOnClickListener(this);
        findViewById(R.id.action_delete).setOnClickListener(this);
        floatingActionButton.setOnClickListener(this);
        floatingActionButton.setVisibility(View.VISIBLE);


        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                String value = getIntent().getExtras().getString(key);
                Log.d("clima 2 ", "Key: " + key + " Value: " + value);
            }
        }

        setupViewPager();
        fetchContacts();
        markOnline(true);

//        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data");
//        String ph = FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber();
//        //ph="+923104772882";
//        try {
//            reference.child("call_zego").child(ph).addValueEventListener(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//
//                    if (dataSnapshot.getChildrenCount() > 0) {
//                        if (dataSnapshot.getKey().equals(ph.toString())) {
//                            if( dataSnapshot.child("answered").getValue()==null || dataSnapshot.child("canceled").getValue()==null || dataSnapshot.child("video").getValue()==null
//                            || dataSnapshot.child("isGroup").getValue()==null){
//                                reference.child("call_zego").child(ph).removeValue();
//                                return;
//                            }
//                            boolean value = (boolean) dataSnapshot.child("answered").getValue();
//                            boolean cancel = (boolean) dataSnapshot.child("canceled").getValue();
//                            isVideo = (boolean) dataSnapshot.child("video").getValue();
//                            callerId = dataSnapshot.child("uId").getValue().toString();
//                            RoomId = dataSnapshot.child("room").getValue().toString();
//                            name = dataSnapshot.child("name").getValue().toString();
//
//                            boolean isGroup = (boolean) dataSnapshot.child("isGroup").getValue();
//                            if (isGroup && value) {
//                                if (value && !cancel) {
//                                    //Toast.makeText(MainActivity.this, "true group", Toast.LENGTH_SHORT).show();
//                                    startActivity(new Intent(MainActivity.this, GroupIncomingActivity.class));
//                                    return;
//                                } else if(!value &&!cancel){
//                                    notifyMisscall();
//                                    return;
//                                }
//
//                            } else {
//                                if (value && !cancel && !isGroup) {
//                                    //Toast.makeText(MainActivity.this, "true", Toast.LENGTH_SHORT).show();
//                                    startActivity(new Intent(MainActivity.this, IncomingCallScreenActivity.class));
//                                } else if(!value &&!cancel){
//                                    //Toast.makeText(MainActivity.this, "missed Call", Toast.LENGTH_SHORT).show();
//                                    HashMap<String, User> myUsers = helper.getCacheMyUsers();
//                                    if (myUsers != null && myUsers.containsKey(callerId)) {
//                                        user = myUsers.get(callerId);
//                                    }
//                                }
//                                if (!isGroup && !value ) {
//                                    LogCall logCall = null;
//                                    if (user == null) {
//                                        user = new User(MainActivity.callerId, MainActivity.callerId, getString(R.string.app_name), "");
//                                    }
//
//                                    rChatDb.beginTransaction();
//                                    logCall = new LogCall(user, System.currentTimeMillis(), 0, false, "cause.toString()", userMe.getId(), user.getId());
//                                    rChatDb.copyToRealm(logCall);
//                                    rChatDb.commitTransaction();
//                                    notifyMisscall(logCall);
//                                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("data").child("call_zego");
//                                    reference.child(userMe.getId()).removeValue();
//                                }
//                            }
//                        }
//                    }
//
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError databaseError) {
//
//                }
//            });
//        } catch (Exception e) {
//
//        }
//          loadAdd();
    }
    public  static  boolean isVideo =false;
//    private void notifyMisscall(LogCall logCall) {
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 56, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
//
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        NotificationCompat.Builder notificationBuilder = null;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_USER_MISSCALL, "ChatBuddy Chat misscall notification", NotificationManager.IMPORTANCE_DEFAULT);
//            notificationManager.createNotificationChannel(channel);
//            notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_USER_MISSCALL);
//        } else {
//            notificationBuilder = new NotificationCompat.Builder(this);
//        }
//
//        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        notificationBuilder.setSmallIcon(R.drawable.ic_logo_)
//                .setContentTitle(logCall.getUser().getNameToDisplay())
//                .setContentText("Gave you a miss call")
//                .setAutoCancel(true)
//                .setSound(defaultSoundUri)
//                .setContentIntent(pendingIntent);
//        int msgId = 0;
//        try {
//            msgId = Integer.parseInt(logCall.getUser().getId());
//        } catch (NumberFormatException ex) {
//            msgId = Integer.parseInt(logCall.getUser().getId().substring(logCall.getUser().getId().length() / 2));
//        }
//        notificationManager.notify(msgId, notificationBuilder.build());
//    }

    private void notifyMisscall() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 56, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notificationBuilder = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_USER_MISSCALL, "ChatBuddy Chat misscall notification", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID_USER_MISSCALL);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        notificationBuilder.setSmallIcon(R.drawable.ic_logo_)
                .setContentTitle(name)
                .setContentText("Gave you a miss call")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);
        int msgId = 0;
        try {
            msgId = Integer.parseInt(callerId);
        } catch (NumberFormatException ex) {
            msgId = 1;
        }
        notificationManager.notify(msgId, notificationBuilder.build());
    }

    private void initUi() {
        usersImage = findViewById(R.id.users_image);
        menuRecyclerView = findViewById(R.id.menu_recycler_view);
        swipeMenuRecyclerView = findViewById(R.id.menu_recycler_view_swipe_refresh);
        drawerLayout = findViewById(R.id.drawer_layout);
        searchContact = findViewById(R.id.searchContact);
        invite = findViewById(R.id.invite);
        toolbarContainer = findViewById(R.id.toolbarContainer);
        cabContainer = findViewById(R.id.cabContainer);
        selectedCount = findViewById(R.id.selectedCount);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        viewPager = findViewById(R.id.viewPager);
        floatingActionButton = findViewById(R.id.addConversation);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        backImage = findViewById(R.id.back_button);
        drawerLayout.setTouchMode(ElasticDrawer.TOUCH_MODE_BEZEL);
        dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("Syncing. . .");
        dialog.setCancelable(false);
        dialog.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dialog != null && dialog.isShowing())
                    dialog.dismiss();
            }
        }, 4000);
    }

    private void setupViewPager() {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFrag(new MyUsersFragment(MainActivity.this), "Chats");
        adapter.addFrag(new MyGroupsFragment(MainActivity.this), "Groups");
        // adapter.addFrag(new MyStatusFragment(), "Status");
        adapter.addFrag(new MyStatusFragmentNew(), "Status");
        adapter.addFrag(new MyCallsFragment(), "Calls");
        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                if (i == 2)
                    floatingActionButton.hide();
                else
                    floatingActionButton.show();
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

    }

    private void setupMenu() {
        menuRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        menuUsersRecyclerAdapter = new MenuUsersRecyclerAdapter(this, myUsers, helper.getLoggedInUser());
        menuRecyclerView.setAdapter(menuUsersRecyclerAdapter);
        swipeMenuRecyclerView.setColorSchemeResources(R.color.colorAccent);
        swipeMenuRecyclerView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchContacts();
            }
        });
        searchContact.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                menuUsersRecyclerAdapter.getFilter().filter(editable.toString());
            }
        });
    }

    private void setProfileImage(ImageView imageView) {
        if (userMe != null)
//            Glide.with(this).load(userMe.getImage()).apply(new RequestOptions().placeholder(R.drawable.ic_placeholder)).into(imageView);

            if (userMe != null && userMe.getImage() != null && !userMe.getImage().isEmpty()) {
                Picasso.get()
                        .load(userMe.getImage())
                        .tag(this)
                        .error(R.drawable.ic_avatar)
                        .placeholder(R.drawable.ic_avatar)
                        .into(imageView);
            } else if (group != null && group.getImage() != null && !group.getImage().isEmpty()) {
                Picasso.get()
                        .load(group.getImage())
                        .tag(this)
                        .placeholder(R.drawable.ic_avatar)
                        .into(imageView);

            }

        /*Glide.with(this)
                .load(userMe.getImage())
                .placeholder(R.drawable.ic_placeholder)
                .into(imageView);*/

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CONTACTS_REQUEST_CODE:
                fetchContacts();
                break;
        }
    }


    private void fetchContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            if (!FetchMyUsersService.STARTED) {
                if (!swipeMenuRecyclerView.isRefreshing())
                    swipeMenuRecyclerView.setRefreshing(true);
                FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                if (firebaseUser != null) {
                    firebaseUser.getIdToken(true).addOnCompleteListener(new OnCompleteListener<GetTokenResult>() {
                        @Override
                        public void onComplete(@NonNull Task<GetTokenResult> task) {
                            if (task.isSuccessful()) {
                                String idToken = task.getResult().getToken();
                                FetchMyUsersService.startMyUsersService(MainActivity.this, userMe.getId(), idToken);
                            }
                        }
                    });
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, CONTACTS_REQUEST_CODE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        markOnline(false);
    }

    @Override
    public void onBackPressed() {
        if (ElasticDrawer.STATE_CLOSED != drawerLayout.getDrawerState()) {
            drawerLayout.closeMenu(true);
        } else if (isContextualMode()) {
            disableContextualMode();
        } else if (viewPager.getCurrentItem() != 0) {
            viewPager.post(new Runnable() {
                @Override
                public void run() {
                    viewPager.setCurrentItem(0);
                }
            });
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (REQUEST_CODE_CHAT_FORWARD):
                if (resultCode == Activity.RESULT_OK) {
                    //show forward dialog to choose users
                    messageForwardList.clear();
                    ArrayList<Message> temp = data.getParcelableArrayListExtra("FORWARD_LIST");
                    messageForwardList.addAll(temp);
                    userSelectDialogFragment = UserSelectDialogFragment.newInstance(this, myUsers);
                    FragmentManager manager = getSupportFragmentManager();
                    Fragment frag = manager.findFragmentByTag(USER_SELECT_TAG);
                    if (frag != null) {
                        manager.beginTransaction().remove(frag).commit();
                    }
                    userSelectDialogFragment.show(manager, USER_SELECT_TAG);
                }
                break;
        }
    }

    private void sortMyGroupsByName() {
        Collections.sort(myGroups, new Comparator<Group>() {
            @Override
            public int compare(Group group1, Group group2) {
                return group1.getName().compareToIgnoreCase(group2.getName());
            }
        });
    }

    private void sortMyUsersByName() {
        Collections.sort(myUsers, new Comparator<User>() {
            @Override
            public int compare(User user1, User user2) {
                return user1.getNameToDisplay().compareToIgnoreCase(user2.getNameToDisplay());
            }
        });
    }

    @Override
    void userAdded(User value) {
        if (value.getId().equals(userMe.getId()))
            return;
        else if (helper.getCacheMyUsers() != null && helper.getCacheMyUsers().containsKey(value.getId())) {
            value.setNameInPhone(helper.getCacheMyUsers().get(value.getId()).getNameToDisplay());
            addUser(value);
        } else {
            for (Contact savedContact : contactsData) {
                if (Helper.contactMatches(value.getId(), savedContact.getPhoneNumber())) {
                    value.setNameInPhone(savedContact.getName());
                    addUser(value);
                    helper.setCacheMyUsers(myUsers);
                    break;
                }
            }
        }
    }

    @Override
    void groupAdded(Group group) {
        if (!myGroups.contains(group)) {
            myGroups.add(group);
            sortMyGroupsByName();
        }
    }

    @Override
    void userUpdated(User value) {
        if (value.getId().equals(userMe.getId())) {
            userMe = value;
            setProfileImage(usersImage);
        } else if (helper.getCacheMyUsers() != null && helper.getCacheMyUsers().containsKey(value.getId())) {
            value.setNameInPhone(helper.getCacheMyUsers().get(value.getId()).getNameToDisplay());
            updateUser(value);
        } else {
            for (Contact savedContact : contactsData) {
                if (Helper.contactMatches(value.getId(), savedContact.getPhoneNumber())) {
                    value.setNameInPhone(savedContact.getName());
                    updateUser(value);
                    helper.setCacheMyUsers(myUsers);
                    break;
                }
            }
        }
    }

    private void updateUser(User value) {
        int existingPos = myUsers.indexOf(value);
        if (existingPos != -1) {
            myUsers.set(existingPos, value);
            menuUsersRecyclerAdapter.notifyItemChanged(existingPos);
            refreshUsers(existingPos);
        }
    }

    @Override
    void groupUpdated(Group group) {
        int existingPos = myGroups.indexOf(group);
        if (existingPos != -1) {
            myGroups.set(existingPos, group);
            //menuUsersRecyclerAdapter.notifyItemChanged(existingPos);
            //refreshUsers(existingPos);
        }
    }

    @Override
    void statusAdded(Status status) {

    }

    @Override
    void statusUpdated(Status status) {

    }

//    @Override
//    void onSinchConnected() {
//
//    }
//
//    @Override
//    void onSinchDisconnected() {
//
//    }

    private void addUser(User value) {
        if (!myUsers.contains(value)) {
            myUsers.add(value);
            sortMyUsersByName();
            menuUsersRecyclerAdapter.notifyDataSetChanged();
            refreshUsers(-1);
        }
    }


    @Override
    public void OnUserClick(final User user, int position, View userImage) {
        if (ElasticDrawer.STATE_CLOSED != drawerLayout.getDrawerState()) {
            drawerLayout.closeMenu(true);
        }
        if (userImage == null) {
            userImage = usersImage;
        }
        Intent intent = ChatActivity.newIntent(this, messageForwardList, user);
        if (Build.VERSION.SDK_INT > 21) {
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this,
                    userImage, "backImage");
            startActivityForResult(intent, REQUEST_CODE_CHAT_FORWARD, options.toBundle());
        } else {
            startActivityForResult(intent, REQUEST_CODE_CHAT_FORWARD);
            overridePendingTransition(0, 0);
        }

        if (userSelectDialogFragment != null)
            userSelectDialogFragment.dismiss();
    }

    @Override
    public void OnGroupClick(Group group, int position, View userImage) {
        Intent intent = ChatActivity.newIntent(this, messageForwardList, group);
        if (userImage == null) {
            userImage = usersImage;
        }
        if (Build.VERSION.SDK_INT > 21) {
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, userImage, "backImage");
            startActivityForResult(intent, REQUEST_CODE_CHAT_FORWARD, options.toBundle());
        } else {
            startActivityForResult(intent, REQUEST_CODE_CHAT_FORWARD);
            overridePendingTransition(0, 0);
        }

        if (userSelectDialogFragment != null)
            userSelectDialogFragment.dismiss();
    }

    private void refreshUsers(int pos) {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(USER_SELECT_TAG);
        if (frag != null) {
            userSelectDialogFragment.refreshUsers(pos);
        }
    }

    private void markOnline(boolean b) {
        //Mark online boolean as b in firebase
        usersRef.child(userMe.getId()).child("timeStamp").setValue(System.currentTimeMillis());
        usersRef.child(userMe.getId()).child("online").setValue(b);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_button:
                drawerLayout.openMenu(true);
                break;
            case R.id.addConversation:
                switch (viewPager.getCurrentItem()) {
                    case 0:
//                        drawerLayout.openMenu(true);
                        Intent callIntent = new Intent(MainActivity.this, ContactActivity.class);
                        startActivity(callIntent);
                        break;
                    case 1:
                        for (int i = 0; i < myUsers.size(); i++) {
                            myUsers.get(i).setSelected(false);
                        }
                        GroupCreateDialogFragment.newInstance(this, userMe, myUsers)
                                .show(getSupportFragmentManager(), GROUP_CREATE_TAG);
                        break;
                    case 3:
//                        drawerLayout.openMenu(true);
                        Intent aCallIntent = new Intent(MainActivity.this, CallListActivity.class);
                        startActivity(aCallIntent);
                        break;
                }
                break;
            case R.id.users_image:
                if (userMe != null)
                    //OptionsFragment.newInstance(getSinchServiceInterface()).show(getSupportFragmentManager(), OPTIONS_MORE);
                    break;
            case R.id.invite:
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.invitation_title));
                    shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.invitation_text), getPackageName()));
                    startActivity(Intent.createChooser(shareIntent, "Share using.."));
                } catch (Exception ignored) {
                }
                break;
            case R.id.action_delete:
//                FragmentManager manager = getSupportFragmentManager();
//                Fragment frag = manager.findFragmentByTag(CONFIRM_TAG);
//                if (frag != null) {
//                    manager.beginTransaction().remove(frag).commit();
//                }
//
//                ConfirmationDialogFragment confirmationDialogFragment = ConfirmationDialogFragment.newInstance("Delete chat",
//                        "Continue deleting selected chats?",
//                        new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                ((MyUsersFragment) adapter.getItem(0)).deleteSelectedChats();
//                                ((MyGroupsFragment) adapter.getItem(1)).deleteSelectedChats();
//                                disableContextualMode();
//                            }
//                        },
//                        new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                disableContextualMode();
//                            }
//                        });
//                confirmationDialogFragment.show(manager, CONFIRM_TAG);

                if (viewPager.getCurrentItem() == 0) {
                    ((MyUsersFragment) adapter.getItem(0)).deleteSelectedChats();
                } else if (viewPager.getCurrentItem() == 1) {
                    ((MyGroupsFragment) adapter.getItem(1)).deleteSelectedChats();
                }
                break;
        }
    }

    @Override
    public void onUserGroupSelectDialogDismiss() {
        messageForwardList.clear();
//        if (helper.getSharedPreferenceHelper().getBooleanPreference(Helper.GROUP_CREATE, false)) {
//            helper.getSharedPreferenceHelper().setBooleanPreference(Helper.GROUP_CREATE, false);
//            GroupCreateDialogFragment.newInstance(this, userMe, myUsers).show(getSupportFragmentManager(), GROUP_CREATE_TAG);
//        }
    }

    @Override
    public void selectionDismissed() {
        //do nothing..
    }

    @Override
    public void myUsersResult(ArrayList<User> myUsers) {
        helper.setCacheMyUsers(myUsers);
        this.myUsers.clear();
        this.myUsers.addAll(myUsers);
        refreshUsers(-1);
        menuUsersRecyclerAdapter.notifyDataSetChanged();
        swipeMenuRecyclerView.setRefreshing(false);
    }

    @Override
    public void myContactsResult(ArrayList<Contact> myContacts) {
        contactsData.clear();
        contactsData.addAll(myContacts);
        MyUsersFragment myUsersFragment = ((MyUsersFragment) adapter.getItem(0));
        if (myUsersFragment != null) myUsersFragment.setUserNamesAsInPhone();
        MyCallsFragment myCallsFragment = ((MyCallsFragment) adapter.getItem(3));
        if (myCallsFragment != null) myCallsFragment.setUserNamesAsInPhone();
    }

    public void disableContextualMode() {
        cabContainer.setVisibility(View.GONE);
        toolbarContainer.setVisibility(View.VISIBLE);
        ((MyUsersFragment) adapter.getItem(0)).disableContextualMode();
        ((MyGroupsFragment) adapter.getItem(1)).disableContextualMode();
        viewPager.setSwipeAble(true);
    }

    @Override
    public void enableContextualMode() {
        cabContainer.setVisibility(View.VISIBLE);
        toolbarContainer.setVisibility(View.GONE);
        viewPager.setSwipeAble(false);
    }

    @Override
    public boolean isContextualMode() {
        return cabContainer.getVisibility() == View.VISIBLE;
    }

    @Override
    public void updateSelectedCount(int count) {
        if (count > 0) {
            selectedCount.setText(String.format("%d selected", count));
        } else {
            disableContextualMode();
        }
    }

    @Override
    public User getUserMe() {
        return userMe;
    }

    @Override
    public ArrayList<Contact> getLocalContacts() {
        return contactsData;
    }

    @Override
    protected void onResume() {
        super.onResume();

    }
}
