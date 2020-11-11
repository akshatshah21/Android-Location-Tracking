package com.example.locationtracker2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CodeActivity extends AppCompatActivity {

    boolean isBegin = true;
    String beginText = "Enter a code to begin transfer";
    String endText = "Enter the code to end transfer";

    int CODE_LEN = 4;

    TextView textView;
    Button button;
    EditText etCode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code);

        isBegin = getIntent().getBooleanExtra("isBegin", true);

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
                int inputCode = Integer.parseInt(etCode.getText().toString());
                if(inputCode > 999) {
                    // send req to server.
                    Toast.makeText(CodeActivity.this, inputCode, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}