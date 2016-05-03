package com.sbsatter.gcmdemo.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.sbsatter.gcmdemo.R;
import com.sbsatter.gcmdemo.app.Config;
import com.sbsatter.gcmdemo.app.EndPoints;
import com.sbsatter.gcmdemo.app.MyApplication;
import com.sbsatter.gcmdemo.gcm.GcmIntentService;
import com.sbsatter.gcmdemo.gcm.NotificationUtils;
import com.sbsatter.gcmdemo.helper.MyPreferenceManager;
import com.sbsatter.gcmdemo.model.Message;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
    Button logoutBtn, sendBtn;
    EditText messageInputEt, receiverIdEt;
    TextView loginInfoTv;
    LinearLayout linearLayout;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";
    MyPreferenceManager prefs;

    private BroadcastReceiver mRegistrationBroadcastReceiver;
//    private ProgressBar mRegistrationProgressBar;
//    private TextView mInformationTextView;
    private boolean isReceiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // checking for type intent filter
                if (intent.getAction().equals(Config.REGISTRATION_COMPLETE)) {
                    // gcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    String token = intent.getStringExtra("token");

                    Toast.makeText(getApplicationContext(), "GCM registration token: " + token, Toast.LENGTH_LONG).show();

                } else if (intent.getAction().equals(Config.SENT_TOKEN_TO_SERVER)) {
                    // gcm registration id is stored in our server's MySQL

                    Toast.makeText(getApplicationContext(), "GCM registration token is stored in server!", Toast.LENGTH_LONG).show();

                } else if (intent.getAction().equals(Config.PUSH_NOTIFICATION)) {
                    // new push notification is received
                    handlePushNotification(intent);
                    Toast.makeText(getApplicationContext(), "Push notification is received!", Toast.LENGTH_LONG).show();
                }
            }
        };

        if (checkPlayServices()) {
            registerGCM();
        }
    }

    private void init() {
        logoutBtn= (Button)findViewById(R.id.logout_btn);
        sendBtn= (Button)findViewById(R.id.send_btn);
        messageInputEt=(EditText)findViewById(R.id.message_input_et);
        loginInfoTv=(TextView)findViewById(R.id.login_info_tv);
        linearLayout= (LinearLayout)findViewById(R.id.mainActivityLinearLayout);
        receiverIdEt= (EditText)findViewById(R.id.receiver_id_et);
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.getInstance().logout();
            }
        });

        loginInfoTv.setText("Signed in as: "+ MyApplication.getInstance().getPrefManager()
                .getUser().getId());
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String msg= messageInputEt.getText().toString();
                if(msg==""){
                    return;
                }
                AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
                RequestParams rp= new RequestParams();
                String id= receiverIdEt.getText().toString();
                id=(id==null || id == "")? "13101258": id;
                rp.put("to_user_id",id);
                rp.put("message",msg);
                rp.put("from_user_id",MyApplication.getInstance().getPrefManager().getUser().getId());
                asyncHttpClient.post(EndPoints.SENDMESSAGE, rp, new JsonHttpResponseHandler(){
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        super.onSuccess(statusCode, headers, response);
                        try {
                            if(response.getBoolean("error")==false){
                                Log.e(TAG,"Successfully sent message");
                                messageInputEt.setText("");
                                TextView sentMsgTv= new TextView(getApplicationContext());
                                sentMsgTv.setText(msg);
                                LinearLayout.LayoutParams lp= new LinearLayout.LayoutParams(ViewGroup.LayoutParams
                                        .MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//                                lp.gravity= Gravity.RIGHT;
                                lp.setMargins(400, 5, 0, 5);
                                sentMsgTv.setLayoutParams(lp);
//                                sentMsgTv.setPadding(300,5,10,5);
                                sentMsgTv.setBackgroundColor(0);
//                                sentMsgTv.setTextColor(0xfff);
                                linearLayout.addView(sentMsgTv);
                            }else{
                                Toast.makeText(MainActivity.this, "Resend message", Toast
                                        .LENGTH_SHORT).show();
                                return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        super.onFailure(statusCode, headers, responseString, throwable);
                        Log.e(TAG,responseString);
                    }
                });
            }
        });
    }

    /**
     * Handles new push notification
     */
    private void handlePushNotification(Intent intent) {
        int type = intent.getIntExtra("type", -1);

        // if the push is of chat room message
        // simply update the UI unread messages count
        if (type == Config.PUSH_TYPE_USER) {
            // push belongs to user alone
            // just showing the message in a toast
            Message message = (Message) intent.getSerializableExtra("message");
            Toast.makeText(getApplicationContext(), "New push: " + message.getMessage(), Toast.LENGTH_LONG).show();
            TextView receivedMsgTv= new TextView(this);
            receivedMsgTv.setText(message.getMessage());
            LinearLayout.LayoutParams lp= new LinearLayout.LayoutParams(ViewGroup.LayoutParams
                    .MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.gravity=Gravity.START;
            receivedMsgTv.setLayoutParams(lp);
//            receivedMsgTv.setPadding(10, 5, 50, 5);
            receivedMsgTv.setBackgroundColor(0xfff);
//            receivedMsgTv.setTextColor(0x000);
            linearLayout.addView(receivedMsgTv);

        }


    }

    private void launchLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register GCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.REGISTRATION_COMPLETE));

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.PUSH_NOTIFICATION));

        NotificationUtils.clearNotifications();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    private void registerGCM() {
        Intent intent = new Intent(this, GcmIntentService.class);
        intent.putExtra("key", "register");
        startService(intent);
    }

//    private void registerReceiver(){
//        if(!isReceiverRegistered) {
//            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
//                    new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
//            isReceiverRegistered = true;
//        }
//    }
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }


}