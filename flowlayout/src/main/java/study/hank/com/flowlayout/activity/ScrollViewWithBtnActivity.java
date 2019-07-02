package study.hank.com.flowlayout.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import study.hank.com.flowlayout.R;

public class ScrollViewWithBtnActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scroll_view_with_btn);

        View v = findViewById(R.id.v_tag);
        v.setOnTouchListener((v1, event) -> {
            Log.d("zzhou", "onTouch");
            return true;
        });
        v.setOnClickListener(v12 -> Log.d("zzhou", "onClick"));

    }
}
