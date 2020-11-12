package com.example.locationtracker2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;

public class CodeActivity extends AppCompatActivity {

  boolean isBegin = true;
  String beginText = "Enter a code to begin transfer";
  String endText = "Enter the code to end transfer";

  int CODE_LEN = 4;

  TextView textView;
  Button button;
  EditText etCode;

  private static AsyncHttpClient client;
  private static final String url = "http://192.168.29.2:5000/api/transfer/verify-code";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_code);

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
          } else {
            requestParams.put("type", "end");
          }
          client.post(url, requestParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
              Intent intent;
              if(isBegin) {
                intent = new Intent(CodeActivity.this, TripActivity.class);
                startActivity(intent);
                Toast.makeText(CodeActivity.this, "Code Verified", Toast.LENGTH_SHORT).show();
              } else {
                intent = new Intent(CodeActivity.this, TripActivity.class);
                intent.putExtra("end", true);
                setResult(2, intent);
                Toast.makeText(CodeActivity.this, "Trip ended", Toast.LENGTH_SHORT).show();
              }
              finish();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
              Log.d("myTag", "Code Req failed: " + statusCode);

              Toast.makeText(CodeActivity.this, "Code Verification failed", Toast.LENGTH_SHORT).show();
            }
          });
        }
      }
    });
  }
}