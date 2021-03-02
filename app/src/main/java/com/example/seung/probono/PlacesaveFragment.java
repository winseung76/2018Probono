package com.example.seung.probono;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by USER on 2018-08-29.
 */

public class PlacesaveFragment extends Fragment implements CompoundButton.OnCheckedChangeListener{

    ListView searchlist;
    LinearLayout list_layout,registered_layout,search_layout;
    ScrollView registered;
    View view;
    EditText editText;
    ImageButton backbtn;
    TextView edit,delete,whenempty;
    private final String _key = "e81652aaaec447f6f1535790351363";
    String _putAddress;
    private ArrayList<String> _addressSearchResultArr = new ArrayList<String>();
    private ArrayAdapter<String> _addressListAdapter;
    AddressDataTask addressDataTask;
    DBHelper dbHelper;
    SQLiteDatabase db;
    Geocoder geocoder;
    AddressViewManager avm;
    ArrayList<CheckBox> checkBox=new ArrayList<>();
    TextView empty_list;
    Drawable searchicon;

    @Override
    public void onStop() {
        super.onStop();
        if(addressDataTask!=null) {
            addressDataTask.cancel(true);
            addressDataTask = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view=inflater.inflate(R.layout.placesavefragment, container, false);

        list_layout=view.findViewById(R.id.list_layout);
        searchlist=view.findViewById(R.id.searchlist);
        registered_layout=view.findViewById(R.id.registered_layout);
        search_layout=view.findViewById(R.id.search_layout);
        editText=view.findViewById(R.id.search);
        edit=view.findViewById(R.id.edit);
        delete=view.findViewById(R.id.delete);
        registered=view.findViewById(R.id.registered);
        whenempty=view.findViewById(R.id.when_empty);
        backbtn=view.findViewById(R.id.back);
        empty_list=view.findViewById(R.id.empty_list);

        addressDataTask=new AddressDataTask();
        avm=new AddressViewManager();
        avm.loadAddress();

        geocoder = new Geocoder(getActivity());

        searchlist.setAdapter(_addressListAdapter);
        searchlist.setOnItemClickListener(itemClickListener);
        searchlist.setEmptyView(whenempty); //listview가 비어있을 때 나타나는 view를 설정

        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editText.clearFocus();
                registered_layout.setVisibility(View.VISIBLE);
                search_layout.setVisibility(View.GONE);
                backbtn.setVisibility(View.GONE);
            }
        });
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    backbtn.setVisibility(View.VISIBLE);
                    registered_layout.setVisibility(View.GONE);
                    search_layout.setVisibility(View.VISIBLE);

                }
            }
        });
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(edit.getText().toString().equals("편집")){
                    edit.setText("완료");

                    if (list_layout.getChildCount() > 0) {   //레이아웃에 목록이 하나라도 있는 경우
                        for (int i = 0; i < list_layout.getChildCount(); i++) {
                            CheckBox new_checkbox=list_layout.getChildAt(i).findViewById(R.id.checkbox);
                            list_layout.getChildAt(i).findViewById(R.id.checkbox).setVisibility(View.VISIBLE);
                            new_checkbox.setOnCheckedChangeListener(PlacesaveFragment.this);
                            checkBox.add(new_checkbox);
                        }
                        delete.setVisibility(View.VISIBLE);
                    }

                }
                else if(edit.getText().toString().equals("완료")){
                    edit.setText("편집");
                    System.out.println(list_layout.getChildCount());
                    for (int i = 0; i < list_layout.getChildCount(); i++) {
                        CheckBox new_checkbox=list_layout.getChildAt(i).findViewById(R.id.checkbox);
                        if(new_checkbox!=null) {
                            new_checkbox.setVisibility(View.GONE);
                        }
                    }
                    delete.setVisibility(View.INVISIBLE);
                }
            }
        });
        /* '삭제'버튼 클릭 시*/
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                avm.deleteAddress();
            }
        });
        searchicon = getActivity().getResources().getDrawable( R.drawable.search_icon );
        editText.setCompoundDrawablesWithIntrinsicBounds(searchicon,null,null,null);
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                switch (actionId) {
                    //키보드에서 검색버튼을 눌렀을때 해야할 일을 기술
                    case EditorInfo.IME_ACTION_SEARCH:
                        avm.getAddress(editText.getText().toString());
                        break;

                    default:
                        return false;
                }

                return true;

            }

        });
        return view;
    }
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        LinearLayout parent=(LinearLayout)compoundButton.getParent();
        if(b){
            avm.delete_list.add(parent);     //delete_layout_list리스트에 체크한 전화번호의 레이아웃을 포함시킴
        }
        else{
            if(avm.delete_list.contains(parent)){  //체크를 해제했을 때 이미 delete_layout_list리스트에 있으면 제거함
                avm.delete_list.remove(parent);
            }
        }
    }
    public AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long l_position) {

            String str = (String)parent.getAdapter().getItem(position);

            avm.registerAddress(str);

            editText.clearFocus();
            registered_layout.setVisibility(View.VISIBLE);
            search_layout.setVisibility(View.GONE);
            backbtn.setVisibility(View.GONE);
        }
    };
    // 주소를 위도와 경도로 바꾸어주는 메소드

    class AddressDataTask extends AsyncTask<Void,Void,HttpResponse>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            System.out.println("준비");
        }

        @Override
        protected HttpResponse doInBackground(Void... voids) {

            HttpResponse response = null;
            final String apiurl = "http://biz.epost.go.kr/KpostPortal/openapi";
            ArrayList<String> addressInfo = new ArrayList<String>();
            HttpURLConnection conn = null;

            try {
                System.out.println("시작");
                StringBuffer sb = new StringBuffer(3);
                sb.append(apiurl);
                sb.append("?regkey=" + _key + "&target=postNew&query=");
                sb.append(URLEncoder.encode(_putAddress, "EUC-KR"));
                String query = sb.toString();

                URL url = new URL(query);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setRequestProperty("accept-language", "ko");

                DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                byte[] bytes = new byte[4096];

                InputStream in = conn.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                while (true) {
                    int red = in.read(bytes);
                    if (red < 0) break;
                    baos.write(bytes, 0, red);
                }

                String xmlData = baos.toString("utf-8");
                baos.close();
                in.close();
                conn.disconnect();
                Document doc = docBuilder.parse(new InputSource(new StringReader(xmlData)));
                Element el = (Element) doc.getElementsByTagName("itemlist").item(0);

                System.out.println("el : "+xmlData);

                for (int i = 0; i < el.getChildNodes().getLength(); i++) {
                    Node node = ((Node) el).getChildNodes().item(i);
                    if (!node.getNodeName().equals("item")) {
                        continue;
                    }
                    //String address = node.getChildNodes().item(1).getFirstChild().getNodeValue();
                    String post = node.getChildNodes().item(3).getFirstChild().getNodeValue();
                    addressInfo.add(post.substring(0));
                }
                _addressSearchResultArr = addressInfo;
                publishProgress();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (conn != null) conn.disconnect();
                }
                catch (Exception e) { }
            }
            return response;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            String[] addressStrArray = new String[_addressSearchResultArr.size()];
            addressStrArray = _addressSearchResultArr.toArray(addressStrArray);
            _addressListAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, addressStrArray);
            searchlist.setAdapter(_addressListAdapter);

        }

    }
    class AddressViewManager{
        ArrayList<LinearLayout> delete_list=new ArrayList<>();
        LayoutInflater inflater = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        ArrayList<String> addresses=new ArrayList<>();

        public void loadAddress(){

            dbHelper = new DBHelper(getContext());
            try {
                db = dbHelper.getWritableDatabase();
                dbHelper.onUpgrade(db, 3, 3);
            } catch (SQLException ex) {
                db = dbHelper.getReadableDatabase();
            }
            Cursor cursor=db.rawQuery("SELECT * FROM address_info",null);

            if(cursor.getColumnCount()>0)
                list_layout.removeView(empty_list);
            //System.out.println("개수 "+cursor.getColumnCount());
            while(cursor.moveToNext()) {
                String str=cursor.getString(0);
                addresses.add(str);

                inflater = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE);
                View listview= inflater.inflate(R.layout.address_listitem,null);
                list_layout.addView(listview);  //레이아웃에 새로운 리스트레이아웃 추가
                TextView address=listview.findViewById(R.id.address);
                address.setText(str); //db에서 가져온 이름 리스트에서 가져옴
            }
        }
        public void registerAddress(String address_str){
            double latitude,longitude;
            //chgAddressToLatLon(str); //받아온 주소로부터 위도와 경도를 얻음
            View listview= inflater.inflate(R.layout.address_listitem,null);
            list_layout.addView(listview);
            list_layout.removeView(empty_list);
            TextView address=listview.findViewById(R.id.address);
            address.setText(address_str);
            addresses.add(address_str);
            latitude=getLat(address_str);
            longitude=getLon(address_str);
            db.execSQL("INSERT INTO address_info (address,latitude,longitude) VALUES('"+address_str+"', " + latitude + ", " + longitude + ");");
            addressDataTask=new AddressDataTask();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                new DBInput(address_str,latitude,longitude).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
            else
                new DBInput(address_str,latitude,longitude).execute();

            search_layout.setVisibility(View.GONE);
            registered.setVisibility(View.VISIBLE);
        }
        public void deleteAddress(){
            TextView address;
            for(int i=0;i<delete_list.size();i++){
                address=delete_list.get(i).findViewById(R.id.address);

                String address_str=address.getText().toString();

                delete_list.get(i).removeAllViews();
                list_layout.removeView(delete_list.get(i));
                db.execSQL("DELETE FROM address_info WHERE address='"+address_str+"';");
                addresses.remove(address_str);
                if(addresses.size()==0){
                    list_layout.addView(empty_list);
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                    new DBRemover(address_str).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
                else
                    new DBRemover(address_str).execute();
            }
            delete_list.clear();
        }
        public void getAddress(String kAddress) {
            _putAddress = kAddress;

            if(addressDataTask!=null)
                addressDataTask.cancel(true);

            addressDataTask=new AddressDataTask();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                addressDataTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
            else
                addressDataTask.execute();

        /*
        doInBackground()가 실행되지 않았던 이유는
        executeOnExecutor()가 아닌 execute()로 실행을 시켰기 때문임.
         */

        }
        public double getLat(String address){
            List<Address> list=null;
            double latitude=0;

            try {
                list = geocoder.getFromLocationName(address, 10); // 읽을 개수
            }catch (IOException e){
                Log.e("test","입출력 오류 - 서버에서 주소변환시 에러발생");
            }

            if (list != null) {
                if (list.size() == 0) {
                    Toast.makeText(getContext(),"해당되는 주소 정보는 없습니다",Toast.LENGTH_SHORT).show();
                } else {
                    // 해당되는 주소로 인텐트 날리기
                    Address addr = list.get(0);
                    latitude = addr.getLatitude();

                }
            }
            return latitude;
        }
        public double getLon(String address){
            List<Address> list=null;
            double longitude=0;

            try {
                list = geocoder.getFromLocationName(address, 10); // 읽을 개수
            }catch (IOException e){
                Log.e("test","입출력 오류 - 서버에서 주소변환시 에러발생");
            }

            if (list != null) {
                if (list.size() == 0) {
                    Toast.makeText(getContext(),"해당되는 주소 정보는 없습니다",Toast.LENGTH_SHORT).show();
                } else {
                    // 해당되는 주소로 인텐트 날리기
                    Address addr = list.get(0);
                    longitude = addr.getLongitude();
                    //Toast.makeText(getContext(),latitude+"  "+longitude,Toast.LENGTH_SHORT).show();
                }
            }
            return longitude;
        }
    }
    class DBInput extends AsyncTask<Void,Void,Void>{
        HttpClient client;
        HttpPost httpPost;
        String urlstr="http://211.253.26.22/register_memberinfo.php";
        String new_place;
        double latitude,longitude;
        CopyOnWriteArrayList<NameValuePair> post = new CopyOnWriteArrayList<>();

        DBInput(String new_place,double lat,double lon){
            this.new_place=new_place;
            this.latitude=lat;
            this.longitude=lon;
        }

        @Override
        protected void onPreExecute() {
            post.add(new BasicNameValuePair("token", ""));
            post.add(new BasicNameValuePair("place", ""));
            post.add(new BasicNameValuePair("latitude", ""));
            post.add(new BasicNameValuePair("longitude", ""));
        }

        @Override
        protected Void doInBackground(Void... voids) {

            InputStream inputStream = null;
            BufferedReader rd;

            try {
                // 연결 HttpClient 객체 생성
                client = new DefaultHttpClient();

                // 객체 연결 설정 부분, 연결 최대시간 등등
                HttpParams params = client.getParams();
                HttpConnectionParams.setConnectionTimeout(params, 5000);
                HttpConnectionParams.setSoTimeout(params, 5000);

                // Post객체 생성
                httpPost = new HttpPost(urlstr);

                post.set(0, new BasicNameValuePair("token", getToken()));
                post.set(1, new BasicNameValuePair("place", new_place));
                post.set(2, new BasicNameValuePair("latitude", String.valueOf(latitude)));
                post.set(3, new BasicNameValuePair("longitude", String.valueOf(longitude)));

                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(post, "UTF-8");
                httpPost.setEntity(entity);
                HttpResponse httpResponse = client.execute(httpPost);

                System.out.println("post : " + post);

                // 9. 서버로 부터 응답 메세지를 받는다.
                inputStream = httpResponse.getEntity().getContent();
                rd = new BufferedReader(new InputStreamReader(inputStream));


            }catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            System.out.println("완료");
        }


    }

    protected String getToken() {
        Cursor cursor = db.rawQuery("SELECT token FROM mytoken", null);
        String token=null;

        //System.out.println("개수 "+cursor.getColumnCount());
        while (cursor.moveToNext()) {
            token = cursor.getString(0);
        }
        return token;
    }
    class DBRemover extends AsyncTask<Void,Void,Void>{
        HttpClient client;
        HttpPost httpPost;
        String urlstr="http://211.253.26.22/delete_memberinfo.php";
        String place;
        CopyOnWriteArrayList<NameValuePair> post = new CopyOnWriteArrayList<>();

        DBRemover(String place){
            this.place=place;
        }
        @Override
        protected void onPreExecute() {
            post.add(new BasicNameValuePair("token", ""));
            post.add(new BasicNameValuePair("place", ""));
        }

        @Override
        protected Void doInBackground(Void... voids) {

            InputStream inputStream = null;
            BufferedReader rd;

            try {
                // 연결 HttpClient 객체 생성
                client = new DefaultHttpClient();

                // 객체 연결 설정 부분, 연결 최대시간 등등
                HttpParams params = client.getParams();
                HttpConnectionParams.setConnectionTimeout(params, 5000);
                HttpConnectionParams.setSoTimeout(params, 5000);

                // Post객체 생성
                httpPost = new HttpPost(urlstr);

                post.set(0, new BasicNameValuePair("token", getToken()));
                System.out.println("token : "+getToken());
                System.out.println("size : "+getToken().length());
                post.set(1, new BasicNameValuePair("place", place));

                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(post, "UTF-8");
                httpPost.setEntity(entity);
                HttpResponse httpResponse = client.execute(httpPost);

                System.out.println("post : " + post);

                // 9. 서버로 부터 응답 메세지를 받는다.
                inputStream = httpResponse.getEntity().getContent();
                rd = new BufferedReader(new InputStreamReader(inputStream));
                System.out.println("result : "+rd.readLine());


            }catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
