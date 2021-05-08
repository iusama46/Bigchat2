package com.big.chit.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.big.chit.R;
import com.big.chit.adapters.CallListAdapter;
import com.big.chit.models.Contact;
import com.big.chit.models.Group;
import com.big.chit.models.Status;
import com.big.chit.models.User;
import com.big.chit.services.FetchMyUsersService;
import com.big.chit.utils.Helper;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Random;

public class CallListActivity extends BaseActivity {
    private final int CONTACTS_REQUEST_CODE = 321;
    boolean isTokenReceived = false;
    String token = "";
    private ArrayList<User> myUsers = new ArrayList<>();
    private SwipeRefreshLayout swipeMenuRecyclerView;
    private SearchView searchView;
    private RecyclerView callListRecyclerView;
    private CallListAdapter callListAdapter;
    private ImageView back_button;
    private TextView title;
    private Helper helper;
    private String roomUid;

    @Override
    void myUsersResult(ArrayList<User> myUsers) {
        helper.setCacheMyUsers(myUsers);
        this.myUsers.clear();
        this.myUsers.addAll(myUsers);
        try {
            callListAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
        swipeMenuRecyclerView.setRefreshing(false);
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
        if (userMe.getId().equalsIgnoreCase(valueUser.getId())) {
            valueUser.setNameInPhone(helper.getLoggedInUser().getNameInPhone());
            helper.setLoggedInUser(valueUser);
            callListAdapter = new CallListAdapter(CallListActivity.this, MainActivity.myUsers,
                    helper.getLoggedInUser());
            callListRecyclerView.setAdapter(callListAdapter);
        } else {
            int existingPos = MainActivity.myUsers.indexOf(valueUser);
            if (existingPos != -1) {
                valueUser.setNameInPhone(MainActivity.myUsers.get(existingPos).getNameInPhone());
                MainActivity.myUsers.set(existingPos, valueUser);
                helper.setCacheMyUsers(MainActivity.myUsers);
                callListAdapter = new CallListAdapter(CallListActivity.this, MainActivity.myUsers,
                        helper.getLoggedInUser());
                callListRecyclerView.setAdapter(callListAdapter);
            }
        }
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_list);

        uiInit();

        roomUid = getRoomId();
        pushNotification(roomUid);



    }
    private void pushNotification(String uId) {
        try {

            RequestQueue queue = Volley.newRequestQueue(this);

            String url = "https://agoratokenbig.herokuapp.com/access_token?channel=" + uId;

            StringRequest request = new StringRequest(url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    if (response != null) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            token = jsonObject.getString("token");
                            Log.d("clima token", token);
                            Log.d("clima uid", roomUid);
                            isTokenReceived = true;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }


                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    isTokenReceived = false;
                }
            });

            queue.add(request);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    String getRoomId() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        int length = 7;

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(alphabet.length());
            char randomChar = alphabet.charAt(index);
            sb.append(randomChar);
        }

        return sb.toString().toLowerCase();
    }

    private void uiInit() {
        helper = new Helper(CallListActivity.this);
        swipeMenuRecyclerView = findViewById(R.id.callListSwipeRefresh);
        searchView = findViewById(R.id.searchView);
        back_button = findViewById(R.id.back_button);
        title = findViewById(R.id.title);
        callListRecyclerView = findViewById(R.id.callListRecyclerView);

        searchView.setIconified(true);
        ImageView searchIcon = searchView.findViewById(android.support.v7.appcompat.R.id.search_button);
        searchIcon.setImageDrawable(ContextCompat.getDrawable(CallListActivity.this, R.drawable.ic_search_white));
        SearchView.SearchAutoComplete searchAutoComplete =
                searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setHintTextColor(getResources().getColor(android.R.color.white));
        searchAutoComplete.setTextColor(getResources().getColor(android.R.color.white));

        callListAdapter = new CallListAdapter(CallListActivity.this, myUsers, helper.getLoggedInUser());
        callListRecyclerView.setAdapter(callListAdapter);
        fetchContacts();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                callListAdapter.getFilter().filter(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                callListAdapter.getFilter().filter(s);
                return false;
            }
        });

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    back_button.setVisibility(View.GONE);
                    title.setVisibility(View.GONE);
                } else {
                    back_button.setVisibility(View.VISIBLE);
                    title.setVisibility(View.VISIBLE);
                    searchView.setIconified(true);
                }
            }
        });

        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();

            }
        });
    }

    private void fetchContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            if (!FetchMyUsersService.STARTED) {
                if (!swipeMenuRecyclerView.isRefreshing())
                    swipeMenuRecyclerView.setRefreshing(true);
                FetchMyUsersService.startMyUsersService(CallListActivity.this, userMe.getId(), "");
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},
                    CONTACTS_REQUEST_CODE);
        }
    }

    public void makeCall(boolean b, User user) {
        FragmentManager manager = getSupportFragmentManager();
        Fragment frag = manager.findFragmentByTag("DELETE_TAG");
        if (frag != null) {
            manager.beginTransaction().remove(frag).commit();
        }
        if (user != null && userMe != null && userMe.getBlockedUsersIds() != null
                && userMe.getBlockedUsersIds().contains(user.getId())) {
            Helper.unBlockAlert(user.getNameToDisplay(), userMe, CallListActivity.this,
                    helper, user.getId(), manager);
        } else
            placeCall(b, user);
    }


    private void placeCall(boolean callIsVideo, User user) {
        if (permissionsAvailable(permissionsSinch)) {
            try {


                if (user == null) {
                    // Service failed for some reason, show a Toast and abort
                    Toast.makeText(this, "Service is not started. Try stopping the service and starting it again before placing a call.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (!isTokenReceived) {
                    Toast.makeText(this, "Unable to make call", Toast.LENGTH_SHORT).show();
                    return;
                }

                startActivity(CallScreenActivity.newIntent(this, user, "OUT", callIsVideo,roomUid, token,"key","token",true));
            } catch (Exception e) {
                Log.e("CHECK", e.getMessage());
                //ActivityCompat.requestPermissions(this, new String[]{e.getRequiredPermission()}, 0);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissionsSinch, 69);
        }
    }

}
