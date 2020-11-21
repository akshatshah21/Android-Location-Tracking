package com.example.locationtracker2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.ColorSpace;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class CodeActivity extends AppCompatActivity {

  boolean isBegin = true;
  String beginText = "Enter a code to begin transfer";
  String endText = "Enter the code to end transfer";

  TextView textView;
  Button button;
  EditText etCode;

  private static AsyncHttpClient client;
  private static final String url = "http://192.168.29.2:5000/api/transfer/";

  SharedPreferences sharedPref;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_code);
    sharedPref = this.getSharedPreferences("com.example.locationtracker2.TRANSFER_DETAILS", Context.MODE_PRIVATE);
    isBegin = getIntent().getBooleanExtra("isBegin", true);

    client = new AsyncHttpClient();

    textView = (TextView) findViewById(R.id.tv_text);
    button = (Button) findViewById(R.id.btn_begin_end);
    etCode = (EditText) findViewById(R.id.et_code);

    if(isBegin) {
      textView.setText(beginText);
    } else {
      textView.setText(endText);
    }

    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        String inputCode = etCode.getText().toString();

        if(inputCode.length() == 8) {
          RequestParams requestParams = new RequestParams();
          requestParams.put("code", inputCode);

          if(isBegin) {
            requestParams.put("type", "begin");

            client.post(url + "verifySourceCode", requestParams, new JsonHttpResponseHandler() {
              @Override
              public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                try {
                  String transferId = response.getString("transferId");
                  String destinationId = response.getString("destinationId");
                  Log.d("myTag", "Transfer ID: " + transferId);
                  SharedPreferences.Editor editor = sharedPref.edit();
                  editor.putString("transferId", transferId);
                  editor.putString("destinationId", destinationId);
                  editor.apply();

                  Intent intent = new Intent(CodeActivity.this, TripActivity.class);
                  startActivity(intent);
                  Toast.makeText(CodeActivity.this, "Code Verified", Toast.LENGTH_SHORT).show();
                  finish();
                } catch (JSONException e) {
                  e.printStackTrace();
                  Log.d("myTag", e.getMessage());
                  Toast.makeText(CodeActivity.this, "Error in onSuccess", Toast.LENGTH_SHORT).show();
                }

              }

              @Override
              public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.d("myTag", "Code Req failed: " + statusCode);
                Toast.makeText(CodeActivity.this, "Code Verification failed", Toast.LENGTH_SHORT).show();
              }
            });

          } else if(!sharedPref.getString("transferId", "").isEmpty()) {

            requestParams.put("type", "end");
            requestParams.put("transferId", sharedPref.getString("transferId", "ERR"));
            requestParams.put("destinationId", sharedPref.getString("destinationId", "ERR"));

            client.post(url + "verifyDestinationCode", requestParams, new AsyncHttpResponseHandler() {
              @Override
              public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.remove("transferId");
                editor.remove("destinationId");
                editor.apply();

                Intent intent = new Intent(CodeActivity.this, TripActivity.class);
                intent.putExtra("end", true);
                setResult(2, intent);
                Toast.makeText(CodeActivity.this, "Trip ended", Toast.LENGTH_SHORT).show();
                finish();
              }

              @Override
              public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("myTag", "End req failed, status: " + statusCode);
                Toast.makeText(CodeActivity.this, "Code Verification failed", Toast.LENGTH_SHORT).show();
              }

            });

          } else {
            Log.d("myTag", "End, but no transferId");
          }

        }
      }
    });
  }
}