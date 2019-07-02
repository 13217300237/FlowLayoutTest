package study.hank.com.flowlayout.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import study.hank.com.flowlayout.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigator);

        findViewById(R.id.btn_long_content).setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, VeryLongActivity.class);
            startActivity(i);
        });

        findViewById(R.id.btn_long_content2).setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, VeryLongActivity2.class);
            startActivity(i);
        });

        findViewById(R.id.btn_nromal_content).setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, NormalActivity.class);
            startActivity(i);
        });

        findViewById(R.id.btn_scrollview).setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, ScrollViewWithBtnActivity.class);
            startActivity(i);
        });


    }
}
