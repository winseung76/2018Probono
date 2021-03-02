package com.example.seung.probono;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;


public class ListFragment extends Fragment implements CompoundButton.OnCheckedChangeListener{
    View view,listview;
    LinearLayout layout;
    final int REQUEST_CONTACTS=1;
    final int PERMISSIONS_REQUEST_READ_CONTACTS=100;
    Intent intent;
    FloatingActionButton fab;
    DBHelper dbHelper;
    SQLiteDatabase db;
    TextView edit,delete;
    LayoutInflater inflater;
    TextView empty_list;
    ArrayList<LinearLayout> delete_layout_list=new ArrayList<>();  //체크된 전화번호 리스트들 = '삭제'버튼을 누르면 제거될 전화번호들
    ArrayList<CheckBox> checkBox=new ArrayList<>();
    static ArrayList<String> db_name=new ArrayList<>();
    static ArrayList<String> db_pn=new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view=inflater.inflate(R.layout.listfragment, container, false);
        layout=view.findViewById(R.id.layout);  //전화번호를 추가할 레이아웃
        edit=view.findViewById(R.id.edit);
        delete=view.findViewById(R.id.delete);
        dbHelper = new DBHelper(getContext());
        empty_list=view.findViewById(R.id.empty_list);

        getDB();

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(edit.getText().toString().equals("편집")){
                    edit.setText("완료");

                    if (layout.getChildCount() > 0) {   //레이아웃에 목록이 하나라도 있는 경우
                        for (int i = 0; i < layout.getChildCount(); i++) {
                            CheckBox new_checkbox=layout.getChildAt(i).findViewById(R.id.checkbox);
                            new_checkbox.setVisibility(View.VISIBLE);
                            new_checkbox.setOnCheckedChangeListener(ListFragment.this);
                            checkBox.add(new_checkbox);
                        }
                        delete.setVisibility(View.VISIBLE);
                        fab.setVisibility(View.INVISIBLE);
                    }

                }
                else if(edit.getText().toString().equals("완료")){
                    edit.setText("편집");
                    for (int i = 0; i < layout.getChildCount(); i++) {
                        CheckBox new_checkbox=layout.getChildAt(i).findViewById(R.id.checkbox);
                        if(new_checkbox!=null) {
                            new_checkbox.setVisibility(View.GONE);
                        }
                        //new_checkbox.setVisibility(View.GONE);
                    }
                    delete.setVisibility(View.INVISIBLE);
                    fab.setVisibility(View.VISIBLE);
                }
            }
        });
        /* '삭제'버튼 클릭 시*/
        delete.setOnClickListener(new View.OnClickListener() {
            TextView name,phone;
            @Override
            public void onClick(View view) {
                //System.out.println(delete_layout_list.size());

                for(int i=0;i<delete_layout_list.size();i++){
                    name=delete_layout_list.get(i).findViewById(R.id.name);
                    phone=delete_layout_list.get(i).findViewById(R.id.phone);

                    String rm_name=name.getText().toString();
                    String rm_phone=phone.getText().toString();

                    delete_layout_list.get(i).removeAllViews();
                    layout.removeView(delete_layout_list.get(i));
                    db.execSQL("DELETE FROM pn_info WHERE name='"+rm_name+"' and phone='"+rm_phone+"';");
                    db_name.remove(rm_name);
                    db_pn.remove(rm_phone);
                    if(db_pn.isEmpty()){
                        layout.addView(empty_list);
                    }
                }
                delete_layout_list.clear();
            }
        });

        fab=view.findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadPNInfo();
            }
        });

        showContacts();   //Read contents 허가 받기


        return view;
    }

    /* 체크 버튼 클릭 시*/
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        LinearLayout parent=(LinearLayout)compoundButton.getParent();
        if(b){
            delete_layout_list.add(parent);     //delete_layout_list리스트에 체크한 전화번호의 레이아웃을 포함시킴
        }
        else{
            if(delete_layout_list.contains(parent)){  //체크를 해제했을 때 이미 delete_layout_list리스트에 있으면 제거함
                delete_layout_list.remove(parent);
            }
        }
    }
    public void getDB(){

        try {
            db = dbHelper.getWritableDatabase();
            dbHelper.onUpgrade(db, 3, 3);
        } catch (SQLException ex) {
            db = dbHelper.getReadableDatabase();
        }
        Cursor cursor=db.rawQuery("SELECT * FROM pn_info",null);

        System.out.println("개수 "+cursor.getColumnCount());
        while(cursor.moveToNext()) {
            String new_name=cursor.getString(0);
            String new_phone=cursor.getString(1);

            db_name.add(new_name);
            db_pn.add(new_phone);

            inflater = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE);
            listview= inflater.inflate(R.layout.phone_listitem,null);
            layout.addView(listview);  //레이아웃에 새로운 리스트레이아웃 추가
            TextView name=listview.findViewById(R.id.name);
            TextView phone=listview.findViewById(R.id.phone);
            name.setText(new_name); //db에서 가져온 이름 리스트에서 가져옴
            phone.setText(new_phone);  //db에서 가져온 전화번호 리스트에서 가져옴
        }

        //리스트에 연락처가 있으면 empty_list 제거
        if(!db_pn.isEmpty())
            layout.removeView(empty_list);
    }

    public void loadPNInfo(){
        intent = new Intent(Intent.ACTION_PICK);
        intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);

        startActivityForResult(intent,REQUEST_CONTACTS);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == RESULT_OK)
        {
            Cursor cursor = getActivity().getContentResolver().query(data.getData(),
                    new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
            cursor.moveToFirst();
            String new_name=cursor.getString(0);        //0은 이름을 얻어옵니다.
            String new_phone=cursor.getString(1);   //1은 번호를 받아옵니다.

            String no_hyphen=new_phone.replace("-","");

            if(db_pn.contains(no_hyphen)){
                Toast.makeText(getContext(),"이미 저장된 번호입니다.",Toast.LENGTH_LONG).show();
            }
            else {
                db.execSQL("INSERT INTO pn_info (name,phone) VALUES('" + new_name + "','" + no_hyphen + "');");
                //System.out.println(new_name+new_phone);
                inflater = (LayoutInflater)getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                listview= inflater.inflate(R.layout.phone_listitem,null);
                layout.addView(listview);
                TextView name=listview.findViewById(R.id.name);
                TextView phone=listview.findViewById(R.id.phone);
                name.setText(new_name);
                phone.setText(no_hyphen);

                if(db_pn.isEmpty())
                    layout.removeView(empty_list);
                db_name.add(new_name);
                db_pn.add(no_hyphen);
            }
            cursor.close();
        }
        //super.onActivityResult(requestCode, resultCode, data);

    }
    private void showContacts() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(getContext(),Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
            System.out.println("Yes");
        }
        else
            System.out.println("NO");
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {

        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                showContacts();
            } else {
                Toast.makeText(getContext(), "Until you grant the permission, we canot display the names", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
