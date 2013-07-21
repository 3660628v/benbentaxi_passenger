package com.benbentaxi.passenger.ad;

import java.util.Random;

import com.benbentaxi.passenger.R;
import com.benbentaxi.passenger.background.AdServiceConnection;
import com.benbentaxi.passenger.background.BackgroundService;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TextAdFragment extends Fragment{
	private static final String TAG								=  TextAdFragment.class.getName();
	private AdServiceConnection	mAdServiceConnection 			=	null;
	private TextAdReceiver		mTextAdReceiver					=   null;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v =  inflater.inflate(R.layout.fragment_text_ad, container, false);
		return v;
	}
	public void onResume()
	{
        TextView adInfo = (TextView) getActivity().findViewById(R.id.ad_info_text);
        if (new Random().nextBoolean()){
        	adInfo.setText("fffffffffffffffffff3333333333333333吗u哦22222222.。。。。。。");
        }else{
        	adInfo.setText("阳泉人民欢迎您。。。。。。。。。。。。。。。。。。。!!!!!!!!!");
        }
        boundService();
        registerReceiver();
        super.onResume();
	}
	public void onPause()
	{
		unregisterReceiver();
		unboundService();
		super.onPause();
	}
	
	public BackgroundService getBackgroundService()
	{
		if (mAdServiceConnection != null)
			return mAdServiceConnection.getService();
		return null;
	}
	
	private void boundService()
	{
		if (mAdServiceConnection == null){
			mAdServiceConnection = new AdServiceConnection();
		}
	    Intent intent 	= new Intent(getActivity(), BackgroundService.class);
	    getActivity().bindService(intent, mAdServiceConnection, Context.BIND_AUTO_CREATE);
	    Log.i(TAG,"Bind AdService ");

	}
	private void unboundService()
	{
		if (mAdServiceConnection != null && mAdServiceConnection.isBound()){
            getActivity().unbindService(mAdServiceConnection);
            mAdServiceConnection.close();
		}
	    Log.i(TAG,"unBind AdService!");

	}
	private void registerReceiver()
	{
		if (mTextAdReceiver == null){
			this.mTextAdReceiver 		=	 new TextAdReceiver(this);
		}
    	LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mTextAdReceiver,new IntentFilter(BackgroundService.TEXT_AD_ACTION));
	}
	private void unregisterReceiver()
	{
  	  LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mTextAdReceiver);

	}
}
