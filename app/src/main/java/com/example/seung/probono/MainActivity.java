package com.example.seung.probono;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnTabSelectListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class MainActivity extends FragmentActivity {

    AlarmFragment alarmFragment;
    ListFragment listFragment;
    PlacesaveFragment placesaveFragment;
    SQLiteDatabase db;
    DBHelper dbHelper;
    NotificationManager notificationManager;
    static MyApplication device_info;
    DeviceInfoGetter deviceInfoGetter;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        alarmFragment.dbinput.cancel(true);
        alarmFragment.dbinput=null;
        alarmFragment.msgGetter.cancel(true);
        alarmFragment.msgGetter=null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //super.setShowWhenLocked(true);
        //super.setTurnScreenOn(true);
        alarmFragment = new AlarmFragment();
        listFragment = new ListFragment();
        placesaveFragment=new PlacesaveFragment();
        device_info=new MyApplication();

        if(deviceInfoGetter==null) {
            deviceInfoGetter = new DeviceInfoGetter("http://211.253.26.22/device_loc.php");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                deviceInfoGetter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
            }
            else{
                deviceInfoGetter.execute();
            }
        }

        FirebaseMessaging.getInstance().subscribeToTopic("ALL");
        FirebaseInstanceId.getInstance().getToken();

        //System.out.println(AlarmFragment.switch_flag);

        initFragment();


        BottomBar bottomBar=findViewById(R.id.bottombar);
        bottomBar.setOnTabSelectListener(new OnTabSelectListener() {
            @Override
            public void onTabSelected(int tabId) {

                FragmentTransaction transaction=getSupportFragmentManager().beginTransaction();

                switch (tabId){
                    case R.id.tab_alarm:
                        transaction.replace(R.id.contentContainer,alarmFragment);
                        break;
                    case R.id.tab_evacuation:
                        //transaction.replace(R.id.contentContainer,placesaveFragment);
                        transaction.replace(R.id.contentContainer,placesaveFragment);
                        //alarmFragment.dbinput.cancel(true);
                        break;
                    case R.id.tab_list:
                        //transaction.replace(R.id.contentContainer, listFragment);
                        transaction.replace(R.id.contentContainer, listFragment);
                        break;

                }
                transaction.commit();

            }
        });
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            //checkGPSService();
        }
    }

    public void initFragment(){
        alarmFragment = new AlarmFragment();
        FragmentTransaction transaction=getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.contentContainer,alarmFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    class DBInput extends AsyncTask<Void, HashMap<String, Float>, Void> {

        HashMap<String, Float> hm;
        String urlstr = null;
        String date;

        public DBInput(String url) {
            urlstr = url;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Void o) {

            notificationManager= (NotificationManager)MainActivity.this.getSystemService(MainActivity.this.NOTIFICATION_SERVICE);
            Intent intent = new Intent(MainActivity.this,MainActivity.class); //인텐트 생성.

            Notification.Builder builder = new Notification.Builder(getApplicationContext());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP| Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //현재 액티비티를 최상으로 올리고, 최상의 액티비티를 제외한 모든 액티비티를 없앤다.

            PendingIntent pendingNotificationIntent = PendingIntent.getActivity( MainActivity.this,0, intent, FLAG_UPDATE_CURRENT);
            //PendingIntent는 일회용 인텐트 같은 개념입니다.
            //FLAG_UPDATE_CURRENT - > 만일 이미 생성된 PendingIntent가 존재 한다면, 해당 Intent의 내용을 변경함.

            //FLAG_CANCEL_CURRENT - .이전에 생성한 PendingIntent를 취소하고 새롭게 하나 만든다.

            //FLAG_NO_CREATE -> 현재 생성된 PendingIntent를 반환합니다.

            //FLAG_ONE_SHOT - >이 플래그를 사용해 생성된 PendingIntent는 단 한번밖에 사용할 수 없습니다
            /*
            builder.setSmallIcon(R.drawable.alarm).setTicker("HETT").setWhen(System.currentTimeMillis())
                    .setNumber(1).setContentTitle("화재 발생 알림").setContentText("화재가 발생하였습니다.")
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE).setContentIntent(pendingNotificationIntent).setAutoCancel(true).setOngoing(true);
                    */
            //해당 부분은 API 4.1버전부터 작동합니다.

            notificationManager.notify(1, builder.build()); // Notification send

        }

        @Override
        protected void onProgressUpdate(HashMap<String, Float>... values) {

            super.onProgressUpdate(values);


        }

        @Override
        protected Void doInBackground(Void[] objects) {

            BufferedReader reader;

            try {
                while (true) {
                    URL url = new URL(urlstr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    if (conn != null) {
                        conn.setConnectTimeout(5000);   //연결 timeout
                        conn.setRequestMethod("GET");   //데이터 전송 방식

                        conn.setDoInput(true);   //데이터 input 허용
                        //conn.setDoOutput(true);
                        int resCode = conn.getResponseCode();
                        //System.out.println("resCode : " + resCode);
                        if (resCode == HttpURLConnection.HTTP_OK) {

                            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            hm = new HashMap<>();
                            String jsonstr;

                            reader.readLine();    //<meta>태그 버리기

                            int index=0;
                            while ((jsonstr = reader.readLine()) != null){
                                //array[index++]=jsonstr;
                            }
                        /*
                        JSONArray jarray = new JSONArray(jsonstr);

                        for (int i = 0; i < jarray.length(); i++) {
                            jObject = jarray.getJSONObject(i);
                        }
                        for (int i = 0; i < 16; i++)
                            hm.put(type[i], Float.parseFloat(jObject.getString(type[i])) / 10);
                        for(int i=0;i<toggle_type.length;i++)
                            hm.put(toggle_type[i], Float.parseFloat(jObject.getString(toggle_type[i])));

                        hm.put("OK", Float.parseFloat(jObject.getString("OK")));
                        hm.put("FL", Float.parseFloat(jObject.getString("FL")));
                        //System.out.println("time : " + jObject.getString("date"));  //시간
                        date=jObject.getString("date");
                        publishProgress(hm);*/
                        }
                    }
                }
            }catch (Exception e) {e.printStackTrace();}
            return null;
        }
    }
    class DeviceInfoGetter extends AsyncTask<Void,Void,Void>{
        String urlstr;


        DeviceInfoGetter(String urlstr){
            this.urlstr=urlstr;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            JSONObject jObject=null;
            BufferedReader reader;

            try {

                URL url = new URL(urlstr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                if (conn != null) {
                    conn.setConnectTimeout(5000);   //연결 timeout
                    conn.setRequestMethod("GET");   //데이터 전송 방식

                    conn.setDoInput(true);   //데이터 input 허용

                    int resCode = conn.getResponseCode();
                    if (resCode == HttpURLConnection.HTTP_OK) {

                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String jsonstr="";

                        jsonstr=reader.readLine();
                        JSONArray jarray = new JSONArray(jsonstr);
                        for(int i=0;i<jarray.length();i++){
                            jObject = jarray.getJSONObject(i);
                            device_info.addValue(jObject.getInt("deviceid"),jObject.getString("location"));
                        }
                        System.out.println(device_info.getValue(1)+" and "+device_info.getValue(2));
                        //System.out.println("device_info 크기  : "+device_info.getSize());
                        //jObject = jarray.getJSONObject(0);
                        //device_info.addValue(jObject.getInt("deviceid"),jObject.getString("location"));
                        /*
                        while((jsonstr=reader.readLine())!=null){
                            System.out.println(jsonstr);
                            JSONArray jarray = new JSONArray(jsonstr);
                            jObject = jarray.getJSONObject(0);
                            device_info.addValue(jObject.getInt("deviceid"),jObject.getString("location"));
                        }
                        */

                            //jsonstr=reader.readLine();
                            //System.out.println("출력 : "+jsonstr);

                            //JSONArray jarray = new JSONArray(jsonstr);
                            //jObject = jarray.getJSONObject(0);

                        publishProgress();
                        }
                    }

            }catch (Exception e) {e.printStackTrace();}
            return null;
        }
    }

}
class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME="firealarm.db";
    private static final int DATABASE_VERSION=3;

    public DBHelper(Context context){
        super(context,DATABASE_NAME,null,DATABASE_VERSION);

    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        //System.out.println("테이블 생성완료");
        //db.execSQL("CREATE TABLE IF NOT EXISTS alarmtoggle(date DATE,toggle INTEGER);");
        db.execSQL("CREATE TABLE IF NOT EXISTS pn_info(name TEXT,phone TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS address_info(address TEXT,latitude DOUBLE,longitude DOUBLE);");
        db.execSQL("CREATE TABLE IF NOT EXISTS mytoken(token TEXT);");
        //System.out.println("테이블 생성완료");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        //db.execSQL("DROP TABLE members");
        //db.execSQL("CREATE TABLE IF NOT EXISTS alarmtoggle(date DATE,toggle INTEGER);");
        db.execSQL("CREATE TABLE IF NOT EXISTS pn_info(name TEXT,phone TEXT);");
        db.execSQL("CREATE TABLE IF NOT EXISTS address_info(address TEXT,latitude DOUBLE,longitude DOUBLE);");
        db.execSQL("CREATE TABLE IF NOT EXISTS mytoken(token TEXT);");
        System.out.println("테이블 생성완료");
    }

}
