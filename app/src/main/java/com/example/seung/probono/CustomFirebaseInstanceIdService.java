package com.example.seung.probono;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


public class CustomFirebaseInstanceIdService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";
    DBHelper dbHelper;
    SQLiteDatabase db;

    // [START refresh_token]
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        dbHelper = new DBHelper(getApplicationContext());
        try {
            db = dbHelper.getWritableDatabase();
            dbHelper.onUpgrade(db, 3, 3);
        } catch (SQLException ex) {
            db = dbHelper.getReadableDatabase();
        }
        System.out.println("토근 생성 전");
        String token = FirebaseInstanceId.getInstance().getToken();
        System.out.println("토큰 생성 : "+token);
        Log.d(TAG, "Refreshed token: " + token);

        // 생성등록된 토큰을 개인 앱서버에 보내 저장해 두었다가 추가 뭔가를 하고 싶으면 할 수 있도록 한다.
        sendRegistrationToServer(token);
        db.execSQL("INSERT INTO mytoken (token) VALUES('"+token+"');");

    }
    // [END refresh_token]


    private void sendRegistrationToServer(String token) {
        //FCM 토큰 갱신
        OkHttpClient client = new OkHttpClient();
        RequestBody body = new FormBody.Builder()
                .add("token", token)
                .build();

        //request
        Request request = new Request.Builder()
                .url("http://211.253.26.22/register_token.php")
                .post(body)
                .build();

        try {
            client.newCall(request).execute();
            System.out.println("서버에 토큰 전송 완료");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}