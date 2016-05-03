package com.sbsatter.gcmdemo.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.sbsatter.gcmdemo.R;
import com.sbsatter.gcmdemo.app.EndPoints;
import com.sbsatter.gcmdemo.app.MyApplication;
import com.sbsatter.gcmdemo.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.ButterKnife;
import cz.msebera.android.httpclient.Header;

public class LoginActivity extends AppCompatActivity {
    Button loginBtn;
    EditText nameEt;
    EditText emailEt;
    EditText userIdEt;
    public SharedPreferences sharedPreferences;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ButterKnife.bind(this);
        sharedPreferences=getSharedPreferences("myPreferences",MODE_PRIVATE);
        loginBtn= (Button)findViewById(R.id.login_btn);
        nameEt= (EditText) findViewById(R.id.name_et);
        emailEt= (EditText) findViewById(R.id.email_et);
        userIdEt= (EditText) findViewById(R.id.user_id_et);

        loginBtn.setText("LOGIN HERE");
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login_btn();
            }
        });
        if(sharedPreferences.getBoolean("isUserLoggedIn",false)){
            Intent intent= new Intent(LoginActivity.this,MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void login_btn() {
        // TODO submit data to server...
        Toast.makeText(LoginActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
        final String name= nameEt.getText().toString();
        final String email= emailEt.getText().toString();
        final String userId= userIdEt.getText().toString();
        AsyncHttpClient asyncHttpClient= new AsyncHttpClient();
        RequestParams requestParams= new RequestParams();
        requestParams.put("name",name);
        requestParams.put("email",email);
        requestParams.put("user_id",userId);
        requestParams.put("gcm_registration_id","new_gcm_id");

        asyncHttpClient.post(EndPoints.LOGIN,requestParams, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                try {
                    Log.e("Success_JSONObject",response.toString(4));
                    if(response.getBoolean("error")==false){
                        SharedPreferences.Editor editor= sharedPreferences.edit();
                        editor.putBoolean("isUserLoggedIn",true);
                        User newUser= new User(userId,name,email);
                        MyApplication.getInstance().getPrefManager().storeUser(newUser);
//                        editor.putString("user_id", userId);
//                        editor.putString("user_name", name);
//                        editor.putString("user_email", email);
                        editor.commit();
                        Intent intent= new Intent(LoginActivity.this,MainActivity.class);
                        startActivity(intent);
                        finish();
                    }else{
                        Toast.makeText(LoginActivity.this, "Login or reg failed", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                
                
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                try {
                    Log.e("Error", errorResponse.toString(4));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

}
