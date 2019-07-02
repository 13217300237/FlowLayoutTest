package study.hank.com.flowlayout.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import study.hank.com.flowlayout.R;
import study.hank.com.flowlayout.Utils;
import study.hank.com.flowlayout.custom.FlowLayoutUsingPropertyAnimator;

public class VeryLongActivity3 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.very_long3);

        FlowLayoutUsingPropertyAnimator main = findViewById(R.id.main);

        findViewById(R.id.btnUp).setOnClickListener(v -> main.moveUsingPropertyAnimator(-Utils.dp2px(200)));
        findViewById(R.id.btnDown).setOnClickListener(v -> main.moveUsingPropertyAnimator(Utils.dp2px(200)));
        findViewById(R.id.btnReset0).setOnClickListener(v -> main.moveUsingPropertyAnimator(-main.getTranslationY()));
    }
}
