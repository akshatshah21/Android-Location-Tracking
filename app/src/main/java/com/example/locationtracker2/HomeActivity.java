package com.example.locationtracker2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class HomeActivity extends AppCompatActivity {

  Button btnBegin;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_home);

    btnBegin = (Button) findViewById(R.id.btn_begin);

    btnBegin.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Toast.makeText(HomeActivity.this, "Button clicked", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(HomeActivity.this, CodeActivity.class);
        intent.putExtra("isBegin", true);
        startActivity(intent);

      }
    });
  }
}