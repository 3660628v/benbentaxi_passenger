package com.benbentaxi.passenger.taxirequest.detail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.benbentaxi.passenger.R;
import com.benbentaxi.passenger.demo.DemoApplication;
import com.benbentaxi.passenger.taxirequest.TaxiRequest;
import com.benbentaxi.passenger.taxirequest.TaxiRequestApiConstant;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class TaxiRequestDetail extends Activity {
	
	
	private TaxiRequest mTaxiRequest = null;

	private static Object DetailTable[][]=
		{
			{R.drawable.user,"司机姓名",TaxiRequestApiConstant.ID},
			{R.drawable.plate,"司机车牌",TaxiRequestApiConstant.PLATE},
			{R.drawable.telephone,"司机电话",TaxiRequestApiConstant.DRIVER_MOBILE},
			{R.drawable.location,"距离约为(公里)",TaxiRequestApiConstant.DISTANCE}
		};
	
	@SuppressWarnings("static-access")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		DemoApplication app = (DemoApplication)getApplicationContext();
		mTaxiRequest	    = app.getCurrentShowTaxiRequest(); 
		
		setContentView(R.layout.activity_taxi_request_detail);
		
		setContentView(R.layout.activity_taxi_request_detail);
		
		bindButton();
		
        ListView lv= (ListView)findViewById(R.id.detail_info_list);
        lv.setAdapter(new SimpleAdapter(this, getData(), R.layout.taxi_request_detail_item,   
                new String[]{"taxi_request_detail_item_icon", "taxi_request_detail_item_title", "tqxi_request_detail_item_content"},   
                new int[]{R.id.taxi_request_detail_item_icon, R.id.taxi_request_detail_item_title, R.id.tqxi_request_detail_item_content}));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_taxi_request_detail, menu);
		return true;
	}
	private List<Map<String, Object>> getData() {  
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();        
        for(int i =0;i<DetailTable.length;i++ ){
            Map<String, Object> map = new HashMap<String, Object>();  
            map.put("taxi_request_detail_item_icon",DetailTable[i][0]);
            map.put("taxi_request_detail_item_title",DetailTable[i][1]);
            map.put("tqxi_request_detail_item_content", mTaxiRequest.getField((String) (DetailTable[i][2])));
            list.add(map);  
        } 
        return list;  
    }  
	private void bindButton()
	{
		Button button = (Button)findViewById(R.id.taxi_request_detail_call_driver);
		button.setOnClickListener(
				new View.OnClickListener() {
					public void onClick(View view) {
						Uri uri = Uri.parse("tel:"+TaxiRequestDetail.this.mTaxiRequest.getDriverMobile());
					    Intent call = new Intent(Intent.ACTION_DIAL, uri);
					    TaxiRequestDetail.this.startActivity(call);
					}
		});

	}

}