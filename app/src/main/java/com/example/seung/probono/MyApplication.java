package com.example.seung.probono;

import android.app.Application;

import java.util.Hashtable;

public class MyApplication extends Application {

    private Hashtable<Integer,String> device_info=new Hashtable<>();

    public void addValue(int key,String value){
        device_info.put(key,value);
    }
    public String getValue(int key){
        return device_info.get(key);
    }
    public int getSize(){
        return device_info.size();
    }

}
