package com.example.seung.probono;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;

/**
 * Created by USER on 2018-07-25.
 */
public class DialogActivity extends Activity {
    Context context;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        context = new ContextThemeWrapper(DialogActivity.this, android.R.style.Theme_Holo_Light);
    }
        else {
        context = new ContextThemeWrapper(DialogActivity.this, android.R.style.Theme_Holo_Light);
    }

        new AlertDialog.Builder(context)
            .setTitle("Alarm")
                .setMessage("현 위치 화재 발생!\n대피하시길 바랍니다.")
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            AlarmFragment.vibrator.cancel();
            finish();
        }
    })
            .show();

}

}