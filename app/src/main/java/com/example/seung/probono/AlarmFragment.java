package com.example.seung.probono;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class AlarmFragment extends Fragment {

    View view;
    //Switch switch_button;
    SQLiteDatabase db;
    DBHelper dbHelper;
    TextView flame, co, temp, hum, pos;
    ImageView update_pos;
    int recent;
    //static boolean switch_flag=false;
    static Vibrator vibrator;

    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private final int PERMISSIONS_ACCESS_COARSE_LOCATION = 1001;
    static final int SMS_SEND_PERMISSON = 1;

    private boolean isAccessFineLocation = false;
    private boolean isAccessCoarseLocation = false;
    private boolean isPermission = false;
    private boolean isSMSPermission = false;

    GpsInfo gps;
    String message;
    final String number119 = "01029771939";
    static String address = null;
    LinearLayout layout, msg_layout;
    static boolean alarm = false;
    DBInput dbinput = null;
    AlertDialog.Builder builder;
    ImageView co_state, flame_state, fireAlarm;
    TextView temp_unit, hum_unit, msg, msg_head, fire_location;
    MsgGetter msgGetter = null;
    Spinner spinner;
    MyApplication device_info;
    ArrayList<String> list = new ArrayList<>();
    LinearLayout fireAlarm_layout;
    //프래그먼트를 commit()하거나 처음 로드할때 호출됨


    @Override
    public void onDestroy() {
        super.onDestroy();
        vibrator.cancel();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.alarmfragment, container, false);
        builder = new AlertDialog.Builder(getActivity());
        System.out.println("onCreateView()");


        fireAlarm = view.findViewById(R.id.fireAlarm);
        device_info = MainActivity.device_info;
        layout = view.findViewById(R.id.layout);
        temp_unit = view.findViewById(R.id.temp_unit);
        hum_unit = view.findViewById(R.id.hum_unit);
        flame = view.findViewById(R.id.flame);
        co = view.findViewById(R.id.co);
        temp = view.findViewById(R.id.temp);
        hum = view.findViewById(R.id.hum);
        pos = view.findViewById(R.id.pos);
        update_pos = view.findViewById(R.id.update_pos);
        co_state = view.findViewById(R.id.co_state);
        flame_state = view.findViewById(R.id.flame_state);
        msg = view.findViewById(R.id.msg);
        msg_head = view.findViewById(R.id.msg_head);
        msg_layout = view.findViewById(R.id.msg_layout);
        spinner = view.findViewById(R.id.spinner);
        fireAlarm_layout = view.findViewById(R.id.fireAlarm_layout);
        checkGPS();

        if (!isSMSPermission)
            callSMSPermission();

        dbHelper = new DBHelper(getContext());
        try {
            db = dbHelper.getWritableDatabase();
        } catch (SQLException ex) {
            db = dbHelper.getReadableDatabase();
        }

        // 새로 안내방송이 발생했는지를 주기적으로 체크하는 스레드를 실행시킴
        if (msgGetter == null) {
            msgGetter = new MsgGetter("http://172.20.10.2/hp_bukcoja/getBroadcast.php");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                msgGetter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
            } else {
                msgGetter.execute();
            }
        }
        msgGetter.commit = true;

        // DB로부터 새로운 flame, co, cen 값 등을 주기적으로 받아오는 스래드를 실행시킴
        if (dbinput == null) {
            dbinput = new DBInput("http://211.253.26.22/load.php");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                dbinput.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
            } else {
                dbinput.execute();
            }
        }

        update_pos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkGPS();
                //update_pos.setImageResource(R.drawable.gps4);
                //pos.setText(address);
                Toast.makeText(getContext(), "위치 업데이트 완료", Toast.LENGTH_SHORT).show();
            }
        });
        fireAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fireAlarm_layout.setVisibility(View.GONE);
                vibrator.cancel();
            }
        });


        System.out.println("size : " + device_info.getSize());
        list = new ArrayList<>();
        for (int i = 1; i <= device_info.getSize(); i++) {
            list.add(device_info.getValue(i));
            System.out.println("주소 : " + device_info.getValue(i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item,
                new String[]{"화재경보기1", "화재경보기2"}) {
            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15); //dp
                tv.setTextColor(Color.BLACK);

                return view;
            }
        };
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (adapterView.getChildAt(0) != null)
                    ((TextView) adapterView.getChildAt(0)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });


        return view;
    }


    public void showMsgDialog(String msg) {
        final Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(1000);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("안내방송");
        builder.setMessage(msg);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                vibrator.cancel();

            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void manageBlinkEffect(ObjectAnimator anim) {

        anim.setDuration(1500);
        anim.setEvaluator(new ArgbEvaluator());
        anim.setRepeatMode(ValueAnimator.REVERSE);
        anim.start();
    }

    // 현재 gps를 체크하는 메소드
    public void checkGPS() {
        if (!isPermission) {
            callGPSPermission();
        }
        gps = new GpsInfo(getActivity());
        // GPS 사용유무 가져오기
        if (gps.isGetLocation()) {

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            address = getAddress(getContext(), latitude, longitude);
            update_pos.setImageResource(R.drawable.gps4);
            // 업데이트된 주소를 화면에 보여준다.
            pos.setText(address);
        } else {
            // GPS 를 사용할수 없으므로
            gps.showSettingsAlert();
        }

    }

    private void callSMSPermission() {
        int permissonCheck = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.SEND_SMS);

        if (permissonCheck == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "SMS 수신권한 있음", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "SMS 수신권한 없음", Toast.LENGTH_SHORT).show();

            //권한설정 dialog에서 거부를 누르면
            //ActivityCompat.shouldShowRequestPermissionRationale 메소드의 반환값이 true가 된다.
            //단, 사용자가 "Don't ask again"을 체크한 경우
            //거부하더라도 false를 반환하여, 직접 사용자가 권한을 부여하지 않는 이상, 권한을 요청할 수 없게 된다.

            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), android.Manifest.permission.SEND_SMS)) {
                //이곳에 권한이 왜 필요한지 설명하는 Toast나 dialog를 띄워준 후, 다시 권한을 요청한다.
                Toast.makeText(getContext(), "SMS권한이 거부되어 문자 서비스가 지원되지 않습니다.", Toast.LENGTH_SHORT).show();
                //ActivityCompat.requestPermissions(getActivity(), new String[]{ android.Manifest.permission.SEND_SMS}, SMS_SEND_PERMISSON);
            } else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.SEND_SMS}, SMS_SEND_PERMISSON);
            }
        }
    }

    private void callGPSPermission() {
        int permissonCheck = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        // Check the SDK version and whether the permission is already granted or not.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissonCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            //이곳에 권한이 왜 필요한지 설명하는 Toast나 dialog를 띄워준 후, 다시 권한을 요청한다.
            Toast.makeText(getContext(), "위치 권한이 거부되어 화재 발생 시, 119에 위치정보를 전송할 수 없습니다.", Toast.LENGTH_SHORT).show();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_ACCESS_COARSE_LOCATION);
        } else {
            isPermission = true;
            //db.execSQL("INSERT INTO permission (gps) VALUES('true');");
        }


    }
    // 지인이나 119로 화재 알림 문자 메시지를 보내는 메소드
    public void sendSNS(String phoneNumber, String message) {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(getActivity(), 0, new Intent(SENT), 0);
        PendingIntent deliverIP = PendingIntent.getBroadcast(getActivity(), 0, new Intent(DELIVERED), 0);

        getContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        break;
                }
            }

        }, new IntentFilter(SENT));

        try {
            SmsManager sms = SmsManager.getDefault();

            sms.sendTextMessage(phoneNumber, null, message, sentPI, deliverIP);
            Toast.makeText(getContext(), "SMS 전송 성공", Toast.LENGTH_SHORT).show();
            System.out.println("SMS 전송 성공");

        } catch (Exception e) {
            Toast.makeText(getContext(), "SMS 전송 실패", Toast.LENGTH_SHORT).show();
            System.out.println("SMS 전송 실패");
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        for (int i = 0; i < grantResults.length; i++) {
            if (requestCode == SMS_SEND_PERMISSON) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Toast.makeText(getContext(), "SMS권한 승인함", Toast.LENGTH_SHORT).show();
                    isSMSPermission = true;
                } else {
                    Toast.makeText(getContext(), "SMS권한 거부함", Toast.LENGTH_SHORT).show();
                }
            }
            if (requestCode == PERMISSIONS_ACCESS_FINE_LOCATION
                    && grantResults[i] == PackageManager.PERMISSION_GRANTED) {

                isAccessFineLocation = true;

            } else if (requestCode == PERMISSIONS_ACCESS_COARSE_LOCATION
                    && grantResults[i] == PackageManager.PERMISSION_GRANTED) {

                isAccessCoarseLocation = true;
            }
        }
        if (isAccessFineLocation && isAccessCoarseLocation) {
            isPermission = true;
        }

    }

    public static String getAddress(Context mContext, double lat, double lng) {
        String nowAddress = null;
        Geocoder geocoder = new Geocoder(mContext, Locale.KOREA);
        List<Address> address;
        try {
            if (geocoder != null) {
                //세번째 파라미터는 좌표에 대해 주소를 리턴 받는 갯수로
                //한좌표에 대해 두개이상의 이름이 존재할수있기에 주소배열을 리턴받기 위해 최대갯수 설정
                address = geocoder.getFromLocation(lat, lng, 1);

                if (address != null && address.size() > 0) {
                    // 주소 받아오기
                    String currentLocationAddress = address.get(0).getAddressLine(0).toString();
                    nowAddress = currentLocationAddress;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return nowAddress;
    }

    public void startBlinkingAnimation() {
        fireAlarm_layout.setVisibility(View.VISIBLE);
        Animation startAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.blink_image_anim);
        fireAlarm.startAnimation(startAnimation);
    }

    public class GpsInfo extends Service implements LocationListener {

        private final Context mContext;

        // 현재 GPS 사용유무
        boolean isGPSEnabled = false;

        // 네트워크 사용유무
        boolean isNetworkEnabled = false;

        // GPS 상태값
        boolean isGetLocation = false;

        Location location;
        double lat; // 위도
        double lon; // 경도

        // 최소 GPS 정보 업데이트 거리 10미터
        private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;

        // 최소 GPS 정보 업데이트 시간 밀리세컨이므로 1분
        private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;

        protected LocationManager locationManager;

        public GpsInfo(Context context) {
            this.mContext = context;
            getLocation();
        }

        @TargetApi(23)
        public Location getLocation() {
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(
                            mContext, android.Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                            mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {

                return null;
            }

            try {
                locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

                // GPS 정보 가져오기
                isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

                // 현재 네트워크 상태 값 알아오기
                isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (!isGPSEnabled && !isNetworkEnabled) {
                    // GPS 와 네트워크사용이 가능하지 않을때 소스 구현
                } else {
                    this.isGetLocation = true;
                    // 네트워크 정보로 부터 위치값 가져오기
                    if (isNetworkEnabled) {
                        locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                            if (location != null) {
                                // 위도 경도 저장
                                lat = location.getLatitude();
                                lon = location.getLongitude();
                            }
                        }
                    }

                    if (isGPSEnabled) {
                        if (location == null) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    MIN_TIME_BW_UPDATES,
                                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                            if (locationManager != null) {
                                location = locationManager
                                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (location != null) {
                                    lat = location.getLatitude();
                                    lon = location.getLongitude();
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return location;
        }


        /**
         * 위도값을 가져옵니다.
         */
        public double getLatitude() {
            if (location != null) {
                lat = location.getLatitude();
            }
            return lat;
        }

        /**
         * 경도값을 가져옵니다.
         */
        public double getLongitude() {
            if (location != null) {
                lon = location.getLongitude();
            }
            return lon;
        }

        /**
         * GPS 나 wife 정보가 켜져있는지 확인합니다.
         */
        public boolean isGetLocation() {
            return this.isGetLocation;
        }

        /**
         * GPS 정보를 가져오지 못했을때
         * 설정값으로 갈지 물어보는 alert 창
         */
        public void showSettingsAlert() {
            LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

                alertDialog.setTitle("GPS 사용유무셋팅");
                alertDialog.setMessage("GPS 셋팅이 되지 않았습니다.\n 설정창으로 가시겠습니까?");

                // OK 를 누르게 되면 설정창으로 이동합니다.
                alertDialog.setPositiveButton("예",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                System.out.println("intent : " + intent);
                                startActivity(intent);
                            }
                        });
                // Cancle 하면 종료 합니다.
                alertDialog.setNegativeButton("아니요",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                alertDialog.show();
            }

        }

        @Override
        public IBinder onBind(Intent arg0) {
            return null;
        }

        public void onLocationChanged(Location location) {
            // TODO Auto-generated method stub
            //checkGPS();

        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub

        }

        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub

        }

        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub

        }
    }

    class DBInput extends AsyncTask<Void, HashMap<String, Float>, Void> {

        String urlstr = null;
        String date;
        ArrayList<String> location = new ArrayList<>();

        public DBInput(String url) {
            urlstr = url;
        }

        @Override
        protected void onPreExecute() {
            System.out.println("start!");
        }


        @Override
        protected void onProgressUpdate(HashMap<String, Float>... values) {
            ObjectAnimator flame_anim = ObjectAnimator.ofInt(flame, "textColor", Color.parseColor("#6799FF"),
                    Color.RED, Color.parseColor("#6799FF"));
            ObjectAnimator temp_anim = ObjectAnimator.ofInt(temp, "textColor", Color.parseColor("#6799FF"),
                    Color.RED, Color.parseColor("#6799FF"));
            ObjectAnimator tempUnit_anim = ObjectAnimator.ofInt(temp_unit, "textColor", Color.parseColor("#6799FF"),
                    Color.RED, Color.parseColor("#6799FF"));

            float new_flame = values[0].get("flame");
            float new_cen = values[0].get("cen");
            float new_co = values[0].get("co");
            float new_hum = values[0].get("hum");

            // 화재 알람이 울릴 경우
            if (alarm) {
                // 앱이 동작하고 있지 않은 상황에서도 앱을 꺠워서 화면에 띄우도록 한다.
                final Window win = getActivity().getWindow();
                win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

                // 알람이 울리면 진동이 발생하도록 한다.
                vibrator=(Vibrator)getActivity().getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(new long[]{1000,1000,100,1000,100,1000,100,1000}, 0);
                /*
                if (address == null) {
                    // 현재 gps를 확인하여 현재 위치를 얻어온다.
                    checkGPS();
                }
                */
                address="경기도 고양시 일산서구 킨텍스로 217-60";

                message = "[청각장애인 화재알림서비스]\n'" + address + "'에 화재가 발생하였습니다.\n";

                // db에 저장되어있는 지인의 연락처를 쿼리를 통해 얻어온다.
                Cursor cursor = db.rawQuery("SELECT * FROM pn_info", null);

                while (cursor.moveToNext()) {
                    String phone = cursor.getString(1);
                    // 지인의 연락처를 하나씩 얻어와서 문자 메시지를 전송한다.
                    sendSNS(phone, message);
                }
                message = "[119 자동신고서비스]\n'"+ address+"'에 화재가 발생하였습니다.\n";
                // 119에 화재 발생 메시지를 전송한다.
                sendSNS(number119, message);
                startBlinkingAnimation();

                alarm = false;
            }

            // 불꽃이 감지되지 않은 경우
            if (new_flame == 1.0) {
                flame_state.setImageResource(R.drawable.safe2);
                flame_anim.cancel();
            }
            // 불꽃이 감지된 경우
            else if (new_flame == 0.0) {
                flame_state.setImageResource(R.drawable.fire5);
                manageBlinkEffect(flame_anim);
            }

            // 일산화탄소가 감지되지 않은 경우
            if (new_co == 1.0) {
                co_state.setImageResource(R.drawable.smile);
                co.setText("Good");
            }
            // 일산화탄소가 감지된 경우
            else if (new_co == 0.0) {
                co_state.setImageResource(R.drawable.sad);
                co.setText("Bad");
            }


            // 온도가 30도 이상인 경우
            if (new_cen >= 30.0) {
                manageBlinkEffect(temp_anim);
                manageBlinkEffect(tempUnit_anim);
            }
            // 온도가 30도보다 작은 경우
            else if (new_cen < 30.0) {
                temp_anim.cancel();
                tempUnit_anim.cancel();
            }

            hum.setText(String.valueOf(new_hum));
            temp.setText(String.valueOf(new_cen));

            if (CustomFirebaseMessagingService.count > 0 && new_flame == 1.0)
                CustomFirebaseMessagingService.count = 0;

        }

        @Override
        protected Void doInBackground(Void[] objects) {

            JSONObject jObject = null;
            float hum, cen, co, flame;
            BufferedReader reader;
            HashMap<String, Float> data = new HashMap<>();

            try {
                while (true) {

                    if (isCancelled()) break;
                    URL url = new URL(urlstr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    if (conn != null) {
                        conn.setConnectTimeout(5000);   //연결 timeout
                        conn.setRequestMethod("GET");   //데이터 전송 방식

                        conn.setDoInput(true);   //데이터 input 허용

                        int resCode = conn.getResponseCode();
                        if (resCode == HttpURLConnection.HTTP_OK) {

                            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            String jsonstr = "";


                            // load.php에서 업데이트된 정보들을 읽어온다.
                            jsonstr = reader.readLine();
                            //System.out.println("출력 : " + jsonstr);

                            // 받아온 정보를 JSON형식으로 변환하는 과정
                            JSONArray jarray = new JSONArray(jsonstr);
                            jObject = jarray.getJSONObject(0);

                            if (jObject.getString("hum") != "null") {
                                hum = Float.parseFloat(jObject.getString("hum"));  //습도
                            } else
                                hum = 0;

                            if (jObject.getString("cen") != "null") {
                                cen = Float.parseFloat(jObject.getString("cen")); //온도(섭씨)
                            } else
                                cen = 0;

                            if (jObject.getString("co") != "null") {
                                co = Float.parseFloat(jObject.getString("co"));
                            } else
                                co = 1;

                            if (jObject.getString("flame") != "null") {
                                flame = Float.parseFloat(jObject.getString("flame"));
                            } else
                                flame = 1;

                            date = jObject.getString("datetime");

                            data.put("hum", hum);
                            data.put("cen", cen);
                            data.put("co", co);
                            data.put("flame", flame);

                            publishProgress(data);
                            Thread.sleep(1000);
                        }
                    }
                }
                System.out.println("dbinput빠져나옴");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    class MsgGetter extends AsyncTask<Void, String, Void> {

        String urlstr;
        String date = "";
        String pre_date = "";
        boolean commit = false;

        MsgGetter(String urlstr) {
            this.urlstr = urlstr;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            System.out.println("onPreExecute()");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            BufferedReader reader;
            String msg = "";
            JSONObject jObject = null;

            try {
                while (true) {
                    URL url = new URL(urlstr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    try {
                        if (isCancelled()) break;

                        if (conn != null) {
                            conn.setConnectTimeout(5000);   //연결 timeout
                            conn.setRequestMethod("GET");   //데이터 전송 방식

                            conn.setDoInput(true);   //데이터 input 허용

                            int resCode = -100;
                            try {
                                resCode = conn.getResponseCode();
                            } catch (SocketTimeoutException e) {
                                e.printStackTrace();
                            }

                            if (resCode == HttpURLConnection.HTTP_OK) {

                                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                                String jsonstr;

                                reader.readLine();  //<meta> 태그 버리기
                                jsonstr = reader.readLine();

                                JSONArray jarray = new JSONArray(jsonstr);

                                jObject = jarray.getJSONObject(0);

                                date = jObject.getString("date");

                                if (!date.equals(pre_date) || commit) {
                                    msg = jObject.getString("msg");
                                    publishProgress(msg);
                                    pre_date = date;
                                }
                            } else
                                System.out.println("resCode : " + resCode);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //publishProgress(msg);
                    Thread.sleep(2000);
                }
                System.out.println("빠져나옹~");

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            System.out.println("onProgressUpdate()");

            if (!values[0].equals("")) {
                msg_head.setText("최근 안내 방송 : " + date);
                msg.setText(values[0]);
            }
            /* fragment가 commit()된 상태가 아니고, 새로운 데이터가 들어온 상황인 경우에만
               레이아웃 배경의 애니메이션 효과를 넣는다.
             */
            if (!commit) {
                showMsgDialog(values[0]);
                ObjectAnimator anim = ObjectAnimator.ofInt(msg_layout, "backgroundColor", Color.parseColor("#eaeaea"),
                        Color.parseColor("#FF6C6C"), Color.parseColor("#eaeaea"));

                anim.setDuration(2000);
                anim.setEvaluator(new ArgbEvaluator());
                anim.setRepeatMode(ValueAnimator.REVERSE);
                anim.setRepeatCount(5);
                anim.start();
            }
            commit = false;
        }
    }
}