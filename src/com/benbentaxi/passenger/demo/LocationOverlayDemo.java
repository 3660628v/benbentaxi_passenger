package com.benbentaxi.passenger.demo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.BDNotifyListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKMapViewListener;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.benbentaxi.passenger.R;
import com.benbentaxi.passenger.taxirequest.detail.TaxiRequestDetail;
import com.benbentaxi.passenger.v1.function.ConfirmShow;
import com.benbentaxi.passenger.v1.function.DataPreference;
import com.benbentaxi.passenger.v1.function.GetInfoTask;
import com.benbentaxi.passenger.v1.function.IdShow;
import com.benbentaxi.passenger.v1.function.ListShow;
public class LocationOverlayDemo extends Activity {
	
	static MapView mMapView = null;
	
	private MapController mMapController = null;

	public MKMapViewListener mMapListener = null;
	FrameLayout mMapViewContainer = null;
	
	// 定位相关
	LocationClient mLocClient;
	public MyLocationListenner myListener = new MyLocationListenner();
    public NotifyLister mNotifyer=null;
	
	Button testUpdateButton = null;
	
	EditText indexText = null;
	MyLocationOverlay myLocationOverlay = null;
	int index =0;
	LocationData locData = null;
	
	//private long exitTime = 0;
	
	
	Handler MsgHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            //Toast.makeText(LocationOverlayDemo.this, "msg:" +msg.what, Toast.LENGTH_SHORT).show();
        	switch (msg.what) {
        	case MSG_HANDLE_MAP_MOVE:
        		break;
        	case MSG_HANDLE_POS_REFRESH:
                if ( LocationOverlayDemo.this.mIsDriver ) {
                	doDriver();
                } else {
                	doPassenger();
                }
        		break;
        	default:
        		if ( msg.what >= MSG_HANDLE_ITEM_TOUCH ) {
        			int idx = msg.what-MSG_HANDLE_ITEM_TOUCH;
            		try {
						if ( mIsDriver ) {
							mConfirmObj = mReqInfo.getJSONObject(idx);
							mReqId = mConfirmObj.getInt("id");
							showPassengerRequestInfo(mReqId, mConfirmObj);
						} else {
							// 乘客态，显示司机信息
							JSONObject obj = mReqInfo.getJSONObject(idx);
							int drvid = obj.getInt("driver_id");
							showDriverInfo(drvid, obj);
						}
					} catch (JSONException e) {
						resetStatus();
						// 下标异常
		        		Toast.makeText(LocationOverlayDemo.this.getApplicationContext(), "请求状态异常: "+idx+"/"+mReqInfo.length(),
								Toast.LENGTH_SHORT).show();
					}
        		}
        		break;
        	}
        };
    };
    
    private String mTokenKey, mTokenVal;
	private static final String mTestHost = "42.121.55.211:8081";
	
	OverlayTest ov = null;
	// 存放overlayitem 
	public List<OverlayItem> mGeoList = new ArrayList<OverlayItem>();
	// 保存司机/乘客请求的详细信息
	public JSONArray mReqInfo;
	// 被确认的司机/乘客请求信息
	public JSONObject mConfirmObj;
	// 存放overlay图片
	public List<Drawable>  res = new ArrayList<Drawable>();
	private Drawable mDrvMarker;
	
	private DataPreference mData;
	private String mUserMobile;
	private int mReqId = -1; // 乘客发起的请求id
	private boolean mIsGetLocation = false; // 判断是否成功获取地理位置
	private String mStatus;
	private boolean mIsDriver = false; // 是否是司机
	//private boolean mDrvConfirm = false; // 司机是否确认了请求
	private double mDistance = 0.0; // 乘客/司机间距离
	
	private AudioRecord mAudioRecord; //  乘客声音
	private AudioTrack mAudioTrack; // 播放乘客声音
	private int mAudioBufSize = 0;
	private byte[] mAudioBuffer;
	private long mRecTime; // 判断录音时间是否过短
	private View mDialogView; // 录音对话框的view
	private PopupWindow mPopCallTaxi; // 录音的弹出窗口
	
	private final static String STAT_DRV_TRY_GET_REQUEST = "Driver_Try_Get_Request";
	private final static String STAT_WAITING_DRV_RESP = "Waiting_Driver_Response";
	private final static String STAT_WAITING_PAS_CONF = "Waiting_Passenger_Confirm";
	private final static String STAT_PASSENGER_CONFIRM = "Passenger_Confirm";
	private final static String STAT_PASSENGER_TRY_CONFIRM = "Passenger_Try_Confirm";
	private final static String STAT_SUCCESS = "Success";
	private final static String STAT_PASSENGER_CANCEL = "Passenger_Cancel";
	private final static String STAT_PASSENGER_TRY_CANCEL = "Passenger_Try_cancel";
	
	public final static int MSG_HANDLE_MAP_MOVE = 1;
	public final static int MSG_HANDLE_POS_REFRESH = 2;
	public final static int MSG_HANDLE_ITEM_TOUCH = 10000;
	
	private static int mShowDialogStat = 0;
	
	private ListShow ssss =null;
	
	private OnClickListener mCallTaxiListener = new OnClickListener(){
		public void onClick(View v) {
			testUpdateClick();
			testUpdateButton.setText(LocationOverlayDemo.this.getResources().getString(R.string.recall_taxi));
			showCalltaxi();
		}
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DemoApplication app = (DemoApplication)this.getApplication();
        if (app.mBMapManager == null) {
            app.mBMapManager = new BMapManager(this);
            app.mBMapManager.init(DemoApplication.strKey,new DemoApplication.MyGeneralListener());
        }
        setContentView(R.layout.activity_locationoverlay);
        mMapView = (MapView)findViewById(R.id.bmapView);
        mMapController = mMapView.getController();
        
        mData = new DataPreference(this.getApplicationContext());
        mTokenKey = mData.LoadString("token_key");
        mTokenVal = mData.LoadString("token_value");
        Log.d("xssssss",mTokenKey);
        Log.d("xssssss",mTokenVal);
        mUserMobile = mData.LoadString("user");
        mIsDriver = mData.LoadBool("isdriver");
        
        initMapView();
        
        mLocClient = new LocationClient( this );
        mLocClient.registerLocationListener( myListener );
        
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);//打开gps
        option.setCoorType("bd09ll");     //设置坐标类型
        option.setScanSpan(5000);
        mLocClient.setLocOption(option);
        mLocClient.start();
        mMapView.getController().setZoom(14);
        mMapView.getController().enableClick(true);
        
        mMapView.setBuiltInZoomControls(true);
        mMapListener = new MKMapViewListener() {
			
			@Override
			public void onMapMoveFinish() {
				// Auto-generated method stub
			}
			
			@Override
			public void onClickMapPoi(MapPoi mapPoiInfo) {
				// Auto-generated method stub
				String title = "";
				if (mapPoiInfo != null){
					title = mapPoiInfo.strText;
					Toast.makeText(LocationOverlayDemo.this,title,Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onGetCurrentMap(Bitmap b) {
				//  Auto-generated method stub
				
			}

			@Override
			public void onMapAnimationFinish() {
				//  Auto-generated method stub
				
			}
		};
		mMapView.regMapViewListener(DemoApplication.getInstance().mBMapManager, mMapListener);
		
	    // 初始化出租车/乘客位置列表
		if ( mIsDriver ) {
			mDrvMarker = this.getResources().getDrawable(R.drawable.icon_marka);
			res.add(getResources().getDrawable(R.drawable.icon_marka));
			res.add(getResources().getDrawable(R.drawable.icon_markb));
			res.add(getResources().getDrawable(R.drawable.icon_markc));
			res.add(getResources().getDrawable(R.drawable.icon_markd));
			res.add(getResources().getDrawable(R.drawable.icon_marke));
			res.add(getResources().getDrawable(R.drawable.icon_markf));
			res.add(getResources().getDrawable(R.drawable.icon_markg));
			res.add(getResources().getDrawable(R.drawable.icon_markh));
			res.add(getResources().getDrawable(R.drawable.icon_marki));
			res.add(getResources().getDrawable(R.drawable.icon_markj));
		} else {
			mDrvMarker = this.getResources().getDrawable(R.drawable.steering);
			res.add(getResources().getDrawable(R.drawable.steering));
		}
	    ov = new OverlayTest(mDrvMarker, this,mMapView, MsgHandler); 
	    mMapView.getOverlays().add(ov);
	    
		myLocationOverlay = new MyLocationOverlay(mMapView);
		locData = new LocationData();
	    myLocationOverlay.setData(locData);
		mMapView.getOverlays().add(myLocationOverlay);
		myLocationOverlay.enableCompass();
		mMapView.refresh();
		
		testUpdateButton = (Button)findViewById(R.id.btn_callTaxi);
	    testUpdateButton.setOnClickListener(mCallTaxiListener);
	    
	    // 初始化声音组件
	    initAudio();
	    
	    if ( mIsDriver ) {
	    	testUpdateButton.setVisibility(View.GONE);
	    }
	    
    	mDialogView = getLayoutInflater().inflate(R.layout.record_dialog, null);
    	mPopCallTaxi = new PopupWindow(mDialogView, 600, 600);
	    
	    Toast.makeText(this.getApplicationContext(), mTokenKey+": "+mTokenVal, Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }
    
    
    @Override
    protected void onDestroy() {
        if (mLocClient != null)
            mLocClient.stop();
        mMapView.destroy();
        DemoApplication app = (DemoApplication)this.getApplication();
        if (app.mBMapManager != null) {
            app.mBMapManager.destroy();
            app.mBMapManager = null;
        }
        if (ssss != null){
        	ssss.ppppp();
        }
        super.onDestroy();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	mMapView.onSaveInstanceState(outState);
    	
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	mMapView.onRestoreInstanceState(savedInstanceState);
    }
    
    public void testUpdateClick(){
        mLocClient.requestLocation();
    }
    private void initMapView() {
        mMapView.setLongClickable(true);
        //mMapController.setMapClickEnable(true);
        //mMapView.setSatellite(false);
    }
    
    private void initAudio() {
    	if ( mIsDriver ) {
        	mAudioBufSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
    	} else {
        	mAudioBufSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
    	}

    	mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, 
    			AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, mAudioBufSize);
    	
    	mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, 
    			AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, mAudioBufSize, AudioTrack.MODE_STREAM);    	
    	
    	mAudioBuffer = new byte[mAudioBufSize];
    }
    
    private void doRecordAudio() {
    	mAudioRecord.startRecording();
    	mAudioRecord.read(mAudioBuffer, 0, mAudioBufSize);
    	mAudioRecord.stop();
    }

    private void doPlayAudio() {
    	mAudioTrack.play();
    	mAudioTrack.write(mAudioBuffer, 0, mAudioBufSize);
    	mAudioTrack.stop();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    
    private void doDriver() {
    	// 上报司机位置
    	GetTaxiTask dtt = new GetTaxiTask();
    	dtt.driverReport(locData.longitude, locData.latitude, locData.accuracy, "gsm");
    	
    	// 获取taxirequest
    	if ( mReqId < 0 ) {
    		GetTaxiTask drvreq = new GetTaxiTask();
    		drvreq.driverGetRequest(locData.longitude, locData.latitude, locData.accuracy);       	
    	} else {
    	// 轮询request
    		GetTaxiTask drvask = new GetTaxiTask();
    		drvask.driverAskRequest(mReqId);
    	}
    }
    
    private void doPassenger() {
    	// 获取周边Taxi
        GetTaxiTask gtt = new GetTaxiTask();
        gtt.getTaxi(locData.longitude, locData.latitude);

        if ( mReqId > 0 && mStatus != null && mStatus.equals(LocationOverlayDemo.STAT_WAITING_PAS_CONF) ) {
        // 确认司机请求，本次打车行为结束
        	if ( mShowDialogStat == 0 ) {
	        	mShowDialogStat = 1;
	        	
				ConfirmShow confirm = new ConfirmShow("有司机响应，距离您约", mDistance+"公里", this);
				Log.e("asdfasdfasdfasdfasdf", "232141234123412341234124121341");
	        	View.OnClickListener doOK = new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// 确认打车
						mStatus = STAT_PASSENGER_TRY_CONFIRM;
						mShowDialogStat = 0;
	            		GetTaxiTask pass = new GetTaxiTask();
	    				Log.e("asdfasdfasdfasdfasdf", "**************************");

	            		pass.passengerResponse(mReqId, GetTaxiTask.PASS_CONFIRM);
	    				Log.e("asdfasdfasdfasdfasdf", "111**************************");

					}
	        	};
	        	
	        	View.OnClickListener doCancel = new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// 取消打车
	        			mStatus = STAT_PASSENGER_TRY_CANCEL;
	        			mShowDialogStat = 0;
	            		GetTaxiTask pass = new GetTaxiTask();
	            		pass.passengerResponse(mReqId, GetTaxiTask.PASS_CANCEL);
	            		
	            		resetStatus();
					}
	        	};
	        	
	        	confirm.SetNegativeOnclick("重新打车", doCancel);
	        	confirm.SetPositiveOnclick("确认", doOK);
	        	confirm.show();
        	}
        } else if ( mReqId > 0 && mStatus != null && mStatus.equals(STAT_PASSENGER_CONFIRM) ) {
        	if ( mShowDialogStat == 0 ) {
	            DemoApplication app = (DemoApplication)getApplicationContext();
	            app.setCurrentShowTaxiRequest(app.getCurrentTaxiRequest(mConfirmObj));
				Intent taxiRequestDetailIntent = new Intent(LocationOverlayDemo.this,TaxiRequestDetail.class);
	        	LocationOverlayDemo.this.startActivity(taxiRequestDetailIntent);
	        	mShowDialogStat = 1;
        	}
        } else if ( mReqId > 0 ) {
        // 发起轮询
        	GetTaxiTask getrr = new GetTaxiTask();
        	getrr.getRequest(mReqId);
        }
    }
    
    private void showCalltaxi() {
    	// 显示打车请求录音界面    	
    	ImageButton imgBtn = (ImageButton)mDialogView.findViewById(R.id.imgBtnRec);
    	imgBtn.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				Application app = LocationOverlayDemo.this.getApplication();
				TextView tv = (TextView)mDialogView.findViewById(R.id.tvRec);
				
				// 按下录音，释放发送
				if (v.getId() == R.id.imgBtnRec) {
					boolean dismiss = true;
					
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						// TODO 录音或播放
						tv.setText(app.getResources().getString(R.string.rec_ing));
						mRecTime = System.currentTimeMillis();
					}
					
					if (event.getAction() == MotionEvent.ACTION_UP) {
						if ( (System.currentTimeMillis()-mRecTime) < 1000 ) {
							// 时间太短
							//Toast.makeText(LocationOverlayDemo.this, "录音时间太短，请重新录制", Toast.LENGTH_SHORT).show();
							tv.setText(app.getResources().getString(R.string.rec_short));
							
						} else if (mIsGetLocation == true ) {
							// 发起打车请求
							GetTaxiTask reqtt = new GetTaxiTask();
				            reqtt.requireTaxi(locData.longitude, locData.latitude);
				    		//Toast.makeText(LocationOverlayDemo.this, "发送数据", Toast.LENGTH_SHORT).show();
				            tv.setText(app.getResources().getString(R.string.rec_send));
				            dismiss = false;

						} else {
				    		//Toast.makeText(LocationOverlayDemo.this, "正在为您定位，请稍后再试", Toast.LENGTH_SHORT).show();
							tv.setText(app.getResources().getString(R.string.rec_retry));
						}
						
						if ( dismiss ) {
							// 延迟退出
							DelayTask dt = new DelayTask(DelayTask.TYPE_CLOSE_POPUP);
							dt.execute(1000);
						}
					}
				}
				return false;
			}
    	});
    	
    	Application app1 = LocationOverlayDemo.this.getApplication();
		TextView tvv = (TextView)mDialogView.findViewById(R.id.tvRec);
		tvv.setText(app1.getResources().getString(R.string.rec_info));
    	mPopCallTaxi.showAtLocation(mDialogView, Gravity.CENTER, 0, 0);
    }
    
    private void showDriverInfo(int idx, JSONObject obj) throws JSONException {
		int drvid = obj.getInt("driver_id");
		double drv_lat = obj.getDouble("lat");
		double drv_lng = obj.getDouble("lng");
		
		IdShow confirm = new IdShow("司机信息", "ID: "+drvid+"\n经度: "+drv_lng+"\n纬度: "+drv_lat, this);

    	confirm.SetNegativeOnclick(null, null);
    	confirm.SetPositiveOnclick("关闭", null);
    	confirm.getIdDialog().show();
    }
    
    private void showPassengerRequestInfo(int idx, final JSONObject obj) throws JSONException {
    	String[] voiceUrl = new String[5];
    	final DecimalFormat df = new DecimalFormat("#.##");
		try {
			voiceUrl[0] = "ID"+obj.getInt("id");
			voiceUrl[1] = "12345";
			voiceUrl[2] = df.format(obj.getDouble("passenger_lat"))+"/"+df.format(obj.getDouble("passenger_lng"));
			voiceUrl[3] = "大连西路120号";
			voiceUrl[4] = "2013-06-25 00:44:22";
			//voiceUrl[3] = obj.getString("passenger_voice_url");
		} catch (JSONException e) {
			voiceUrl[0] = "未知";
			voiceUrl[1] = "未知";
			voiceUrl[2] = "未知";
			voiceUrl[3] = "未知";
			voiceUrl[4] = "未知";
			//voiceUrl[3] = "乘客信息获取错误: "+e.toString();
		}
				
		ListShow info = new ListShow(voiceUrl, this);
		Log.e("444444444444", "**************");
    	View.OnClickListener doOK = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 确认request
	    		mStatus = STAT_DRV_TRY_GET_REQUEST;
	    		GetTaxiTask drvcon = new GetTaxiTask();
	    		drvcon.driverConfirm(locData.longitude, locData.latitude, mReqId);
	    		
	    		String mobile;
	    		try {
	    			mobile = "86"+obj.getInt("id");
	    			//mobile = "12345";
	    		} catch (JSONException e) {
	    			mobile = "000000";
	    		}
	    		Uri uri = Uri.parse("tel:"+mobile);
			    Intent incall = new Intent(Intent.ACTION_DIAL, uri);
			    LocationOverlayDemo.this.startActivity(incall);
			}
    	};
    	
    	info.SetPositiveOnclick("电话乘客", doOK);
    	info.show();
		Log.e("555555555", "**************");

    }
	
	/**
     * 监听函数，又新位置的时候，格式化成字符串，输出到屏幕中
     */
    public class MyLocationListenner implements BDLocationListener {
        //private int mCountFactor = 0; // 计数器，控制执行频率
        
    	@Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null)
                return ;
            
            locData.latitude = location.getLatitude();
            locData.longitude = location.getLongitude();
            locData.accuracy = location.getRadius();
            locData.direction = location.getDerect();
            myLocationOverlay.setData(locData);
            mMapView.refresh();
            mMapController.animateTo(new GeoPoint((int)(locData.latitude* 1e6), (int)(locData.longitude *  1e6)), 
            		MsgHandler.obtainMessage(MSG_HANDLE_MAP_MOVE));
            
            mIsGetLocation = true;
            MsgHandler.dispatchMessage(MsgHandler.obtainMessage(MSG_HANDLE_POS_REFRESH));
        }
        
        public void onReceivePoi(BDLocation poiLocation) {
            if (poiLocation == null){
                return ;
            }
        }
    }
    
    public class NotifyLister extends BDNotifyListener{
        public void onNotify(BDLocation mlocation, float distance) {
        }
    }
    
    private void resetStatus() {
    	this.mReqId = -1;
    	//this.mDrvConfirm = false;
    	this.mConfirmObj = null;
    	this.mStatus = "";
    	
    	testUpdateButton.setText(this.getResources().getString(R.string.call_taxi));
    }

    
	private class GetTaxiTask extends GetInfoTask {
		private static final int TYPE_GET_TAXI = 0;
		private static final int TYPE_REQ_TAXI = 1;
		private static final int TYPE_ASK_REQ = 2;
		private static final int TYPE_PAS_CON = 3;
		private static final int TYPE_PAS_CAN = 4;
		private static final int TYPE_DRV_REP = 5;
		private static final int TYPE_DRV_REQ = 6;
		private static final int TYPE_DRV_CON = 7;
		private static final int TYPE_DRV_ASK = 8;
		
		public static final String PASS_CONFIRM = "confirm";
		public static final String PASS_CANCEL = "cancel";
		
		private String _useragent = "ning@benbentaxi";
		private JSONObject _json_data;
		private int _type = -1;
		
		public void getTaxi(double lng, double lat) {
			_type = TYPE_GET_TAXI;
			String url =  "http://"+mTestHost+"/api/v1/users/nearby_driver?lat="+lat+"&lng="+lng;
			super.initCookies(mTokenKey, mTokenVal, "42.121.55.211");
			execute(url, _useragent, GetInfoTask.TYPE_GET);
		}
		
		public void CancelTaxi() {
			// TODO: 取消打车
		}
		
		public void getRequest(int id) {
			_type = TYPE_ASK_REQ;
			String url = "http://"+mTestHost+"/api/v1/taxi_requests/"+id;
			super.initCookies(mTokenKey, mTokenVal, "42.121.55.211");
			execute(url, _useragent, GetInfoTask.TYPE_GET);
		}
		
		public void requireTaxi(double lng, double lat) {
			_type = TYPE_REQ_TAXI;
			String url = "http://"+mTestHost+"/api/v1/taxi_requests";
			
			_json_data = new JSONObject();
			try {
				//{\"taxi_request\":{\"passenger_mobile\":\"15910676326\",\"passenger_lng\":\"8\",\"passenger_lat\":\"8\",\"waiting_time_range\":30}}" 
				JSONObject sess = new JSONObject();
				sess.put("passenger_mobile", mUserMobile);
				sess.put("passenger_lng", lng);
				sess.put("passenger_lat", lat);
				sess.put("waiting_time_range", 10);
				sess.put("passenger_voice", "aSB3aWxsIGJlIHRoZXJl");
				sess.put("passenger_voice_format", "m4a");
				_json_data.put("taxi_request", sess);
			} catch (JSONException e) {
				//_info.append("form json error: "+e.toString());
			}

			doPOST(url);
		}
		
		public void passengerResponse(int id, String type) {
			String url = "http://"+mTestHost+"/api/v1/taxi_requests/"+id+"/"+type;
			if ( type.equals(PASS_CONFIRM) ) {
				_type = TYPE_PAS_CON;
			} else {
				_type = TYPE_PAS_CAN;
			}
			
			_json_data = new JSONObject();
			try {
				//{\"taxi_request\":{\"passenger_mobile\":\"15910676326\",\"passenger_lng\":\"8\",\"passenger_lat\":\"8\",\"waiting_time_range\":30}}" 
				JSONObject sess = new JSONObject();
				sess.put("id", id);
				//_json_data.put("taxi_request", sess);
			} catch (JSONException e) {
				//_info.append("form json error: "+e.toString());
			}
			Log.e("dddddd","xxxxxxxx111111");
			doPOST(url);
			Log.e("dddddd","xxxxxx****x111111");

		}
		
		public void driverReport(double lng, double lat, double radius, String cootype) {
			String url = "http://"+mTestHost+"/api/v1/driver_track_points";
			_type = TYPE_DRV_REP;
			
			_json_data = new JSONObject();
			try {
				//{"driver_track_point":{"mobile":"15910676326", "lat":"8", "png":"8", "radius":100, "coortype":"gsm"}} 
				JSONObject sess = new JSONObject();
				sess.put("mobile", mUserMobile);
				sess.put("lng", lng);
				sess.put("lat", lat);
				sess.put("radius", radius);
				sess.put("coortype", cootype);
				_json_data.put("driver_track_point", sess);
			} catch (JSONException e) {
				
			}
			
			doPOST(url);
		}
		
		public void driverGetRequest(double lng, double lat, double radius) {
			// /api/v1/taxi_requests?lat=8&lng=8&radius=10
			_type = TYPE_DRV_REQ;
			String url =  "http://"+mTestHost+"/api/v1/taxi_requests/nearby?lat="+lat+"&lng="+lng+"&radius=5000";
			super.initCookies(mTokenKey, mTokenVal, "42.121.55.211");
			execute(url, _useragent, GetInfoTask.TYPE_GET);
		}
		
		public void driverConfirm(double lng, double lat, int id) {
			_type = TYPE_DRV_CON;
			String url = "http://"+mTestHost+"/api/v1/taxi_requests/"+id+"/response";
			
			_json_data = new JSONObject();
			try {
				JSONObject sess = new JSONObject();
				sess.put("driver_mobile", mUserMobile);
				sess.put("driver_lat", lat);
				sess.put("driver_lng", lng);
				_json_data.put("taxi_response", sess);
			} catch (JSONException e) {
				
			}
			
			doPOST(url);
		}
		
		public void driverAskRequest(int id) {
			_type = TYPE_DRV_ASK;
			
			String url = "http://"+mTestHost+"/api/v1/taxi_requests/"+id;
			super.initCookies(mTokenKey, mTokenVal, "42.121.55.211");
			execute(url, _useragent, GetInfoTask.TYPE_GET);
		}
		
		private void doPOST(String url) {
			// 一定要初始化cookie和content-type!!!!!
			super.initCookies(mTokenKey, mTokenVal, "42.121.55.211");
			super.initHeaders("Content-Type", "application/json");
			
			execute(url, _useragent, GetInfoTask.TYPE_POST);
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			if ( values[0] >= GetInfoTask.REQUEST_SEND && mPopCallTaxi.isShowing() ) {
				// 在这里关闭录音对话框，造成延迟效果
				mPopCallTaxi.dismiss();
			}
			super.onProgressUpdate(values);
		}

		@Override
		protected void initPostValues() {
			//sess_params.add(new BasicNameValuePair("","{\"session\":{\"name\":\"ceshi001\",\"password\":\"8\"}}"));
			//post_param = "{\"session\":{\"name\":\"ceshi_ning\",\"password\":\"8\"}}";
			if ( _json_data != null ) {
				post_param = _json_data.toString();
			}
		}
		
		@Override
		protected void onPostExecGet(Boolean succ) {
			//_info.setText("Get "+this.getHttpCode()+"\n");
			if ( succ ) {
				String data = this.toString();
				//_info.append("get result: \n"+data);
				JSONTokener jsParser = new JSONTokener(data);
				
				try {
					switch ( _type ) {
					case TYPE_GET_TAXI:
					case TYPE_DRV_REQ:
						doGetList(data);
						break;
					case TYPE_ASK_REQ:
					case TYPE_DRV_ASK:
						doGetRequest(jsParser);
						break;
					default:
						break;
					} 
				} catch (JSONException e) {
					//e.printStackTrace();
					try {
						JSONObject retobj = (JSONObject)jsParser.nextValue();
						JSONObject err = retobj.getJSONObject("errors");
						Toast.makeText(LocationOverlayDemo.this.getApplicationContext(), "返回: "+retobj.toString(), Toast.LENGTH_LONG).show();
						//_info.append("errmsg \""+err.getJSONArray("base").getString(0)+"\"");
						//_info.append("\ncookies: "+_sess_key.getName()+" "+_sess_key.getValue()+"\n");
					} catch (JSONException ee) {
						//_info.append("json error: "+ee.toString()+"\n"+"ret: "+data);
					}
				} catch (Exception e) {
					Toast.makeText(LocationOverlayDemo.this.getApplicationContext(), "错误返回: "+data, Toast.LENGTH_LONG).show();
				}
			} else {
				//_info.append("get errmsg: \n"+_errmsg);
			}
		}
		
		@Override
		protected void onPostExecPost(Boolean succ) {
			String data = this.toString();
			//_info.setText("Post "+this.getHttpCode()+"\n");
			if ( succ ) {
				//_info.append("result: "+this.getHttpCode()+"\n"+this.toString());
				JSONTokener jsParser = new JSONTokener(data);

				try {
					
					switch ( _type ) {
					case TYPE_REQ_TAXI:
						doCreateRequest(jsParser);
						break;
					case TYPE_PAS_CON:
						doPassengerConfirm(jsParser);
						break;
					case TYPE_PAS_CAN:
						doPassengerCancel(jsParser);
						break;
					case TYPE_DRV_CON:
						doDriverConfirm(jsParser);
						break;
					case TYPE_DRV_REP:
						doDriverReport(jsParser);
						break;
					default:
						break;
					}
					
					//_info.append("result \n"+ret.getString("token_key")+": "+ret.getString("token_value"));
					//_sess_key = new BasicNameValuePair(ret.getString("token_key"), ret.getString("token_value"));
				} catch (JSONException e) {
					//e.printStackTrace();
					try {
						JSONObject ret = (JSONObject) jsParser.nextValue();
						JSONObject err = ret.getJSONObject("errors");
						//_info.append("errmsg \""+err.getJSONArray("base").getString(0)+"\"");
						_errmsg = err.getJSONArray("base").getString(0);
						succ = false;
					} catch (Exception ee) {
						//_info.append("json error: "+ee.toString()+"\n");
						//_info.append("to json: "+_json_data.toString());
						_errmsg = "数据通信异常，请检查云服务器配置，或联系服务商";
						succ = false;
					}
				} catch (Exception e) {
					_errmsg = "网络错误，请检查云服务器配置，并确认网络正常后再试";
					succ = false;
				}
				
			} else {
				//_info.append("errmsg: \n"+_errmsg);
			}
			
			if( succ == false ) {
				Toast.makeText(LocationOverlayDemo.this.getApplicationContext(), "错误返回: "+_errmsg+"\n"+data, Toast.LENGTH_SHORT).show();
			}
		}
		
		private void doGetList(String data) throws JSONException {
			mReqInfo = new JSONArray(data);
				
			//清除所有添加的Overlay
	        ov.removeAll();
	        mGeoList.clear();
	        
			//添加一个item
	    	//当要添加的item较多时，可以使用addItem(List<OverlayItem> items) 接口
	        for( int i=0; i<mReqInfo.length(); ++i ) {
	        	JSONObject pos = mReqInfo.getJSONObject(i);
	        	int lat = 0, lng = 0;
	        	
	        	OverlayItem item = null;
	        	switch (_type) {
	        	case TYPE_GET_TAXI:
		        	lat = (int)(pos.getDouble("lat")*1e6);
		        	lng = (int)(pos.getDouble("lng")*1e6);
	        		item= new OverlayItem(new GeoPoint(lat, lng),
			        		"司机"+pos.getInt("driver_id"),"创建时间: "+pos.getString("created_at"));		
	        		break;
	        	case TYPE_DRV_REQ:
		        	lat = (int)(pos.getDouble("passenger_lat")*1e6);
		        	lng = (int)(pos.getDouble("passenger_lng")*1e6);
	        		item= new OverlayItem(new GeoPoint(lat, lng),
			        		"乘客"+pos.getInt("id"),"声音: "+pos.getString("passenger_voice_url"));
	        		break;
	        	default:
	        		break;
	        		
	        	}
		        
	        	if ( item != null ) {
				   	item.setMarker(res.get(i%res.size()));
				   	mGeoList.add(item);
	        	}
	        }
	    	if ( ov.size() < mGeoList.size()){
	    		//ov.addItem(mGeoList.get(ov.size() ));
	    		ov.addItem(mGeoList);
	    	}
		    mMapView.refresh();
		    
		    if ( _type == TYPE_GET_TAXI && mReqId < 0 ) {
		    	Toast.makeText(LocationOverlayDemo.this.getApplicationContext(), "附近有"+mReqInfo.length()+"辆出租车",
						Toast.LENGTH_SHORT).show();
		    }
		    
		    if ( _type == TYPE_DRV_REQ && mReqId < 0 ) {
		    	mStatus = STAT_WAITING_DRV_RESP;
		    	Toast.makeText(LocationOverlayDemo.this.getApplicationContext(), "附近有"+mReqInfo.length()+"个乘客请求",
						Toast.LENGTH_SHORT).show();
		    }
		}
		
		private void doGetRequest(JSONTokener jsParser) throws JSONException {
			// {"id":28,"state":"Waiting_Driver_Response","passenger_lat":8.0,"passenger_lng":8.0,"passenger_voice_url":"/uploads/taxi_request/voice/2013-05-31/03bd766e8ecc2e2429f1610c7bf6c3ec.m4a"}
			// 用户只要处理state即可
			mStatus = ((JSONObject)jsParser.nextValue()).getString("state");
			String msg = null;
			if ( mStatus.equals("Waiting_Driver_Response") ) {
				// 继续等待
				if ( _type == TYPE_ASK_REQ ) {
					// 乘客态
					msg = "请求["+mReqId+"]等待司机响应, 附近"+mGeoList.size()+"辆";
				} else {
					// 司机态
					msg = "乘客请求["+mReqId+"]等待您接受, 附近有"+mGeoList.size()+"个乘客";
				}
				
			} else if ( mStatus.equals("Waiting_Passenger_Confirm") ) {
				// 司机已应答，等待用户确认
				if ( _type == TYPE_ASK_REQ ) {
					// 乘客态
					msg = "请求["+mReqId+"]已有司机应答, 附近"+mGeoList.size()+"辆";
				} else {
					// 司机态
					msg = "乘客请求["+mReqId+"]您已接受, 附近有"+mGeoList.size()+"个乘客";
				}
				
			} else if ( mStatus.equals("TimeOut") ) {
				// 超时
				if ( _type == TYPE_ASK_REQ ) {
					// 乘客态
					msg = "请求["+mReqId+"]已超时, 附近"+mGeoList.size()+"辆";
				} else {
					// 司机态
					msg = "乘客请求["+mReqId+"]已超时, 附近有"+mGeoList.size()+"个乘客";
				}
				LocationOverlayDemo.this.resetStatus();
				
			} else if ( mStatus.equals("Canceled_By_Passenger") ) {
				// 用户取消
				if ( _type == TYPE_ASK_REQ ) {
					// 乘客态
					msg = "请求["+mReqId+"]已被乘客取消, 附近"+mGeoList.size()+"辆";
				} else {
					// 司机态
					msg = "乘客请求["+mReqId+"]已被取消, 附近有"+mGeoList.size()+"个乘客";
				}
				LocationOverlayDemo.this.resetStatus();
				
			} else if ( mStatus.equals("Success") ) {
				// 用户确认，本次打车成功
				if ( _type == TYPE_ASK_REQ ) {
					// 乘客态
					msg = "请求["+mReqId+"]打车成功！";
				} else {
					// 司机态
					msg = "乘客请求["+mReqId+"]已确认，请前往乘客所在地！";
				}
				LocationOverlayDemo.this.resetStatus();
			}
			
			if ( msg != null ) {
				Toast.makeText(LocationOverlayDemo.this.getApplicationContext(), msg,
						Toast.LENGTH_SHORT).show();
			}
		}
		
		private void doCreateRequest(JSONTokener jsParser) throws JSONException {
			JSONObject ret = (JSONObject)jsParser.nextValue();
			mReqId = ret.getInt("id");
		}
		
		private void doPassengerConfirm(JSONTokener jsParser) throws JSONException {
			// 保存并显示司机信息
			// {"id":53,"state":"Success","passenger_mobile":"15910676326","driver_mobile":"15910676326","passenger_lat":8.0,"passenger_lng":8.0,"passenger_voice_url":"/uploads/taxi_request/voice/2013-06-01/e6d709e0158d6b312e0a30e24a656347.m4a","driver_lat":8.0,"driver_lng":8.0}
			mConfirmObj = (JSONObject)jsParser.nextValue();
			mStatus = STAT_PASSENGER_CONFIRM;
		}
		
		private void doPassengerCancel(JSONTokener jsParser) throws JSONException {
			// 不需处理，由doGetRequest轮询得到
			mStatus = STAT_PASSENGER_CANCEL;
		}
		
		private void doDriverConfirm(JSONTokener jsParser) throws JSONException {
			JSONObject ret = (JSONObject) jsParser.nextValue();
			mStatus = ret.getString("state");
		}
		
		private void doDriverReport(JSONTokener jsParser) throws JSONException {
			JSONObject ret = (JSONObject) jsParser.nextValue();
			if ( ! ret.getString("response").equals("ok") ) {
				Toast.makeText(LocationOverlayDemo.this.getApplicationContext(), "司机上报失败: "+jsParser.toString(),
						Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	private class DelayTask extends AsyncTask<Integer, Integer, Boolean> {
		public final static int TYPE_CLOSE_POPUP = 0;
		
		private int _type;
		
		public DelayTask( int type ) {
			_type = type;
		}
		
		@Override
		protected Boolean doInBackground(Integer... params) {
			// 获取延迟时间, ms
			int delay = params[0];
			SystemClock.sleep(delay);
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			switch( _type ) {
			case TYPE_CLOSE_POPUP:
				if ( mPopCallTaxi.isShowing() ) {
					mPopCallTaxi.dismiss();
				}
				break;
			default:
				break;
			}
		}
	}
	
	@Override
	public boolean onKeyDown( int keyCode, KeyEvent event ) {
		/*
		if(keyCode == KeyEvent.KEYCODE_BACK && 
				event.getAction() == KeyEvent.ACTION_DOWN) {
			// 这里不需要再按两次，一次退出
			if( exitTime == 0 ) {
	            Toast.makeText(getApplicationContext(), "再按一次返回键退出", Toast.LENGTH_SHORT).show();
	            exitTime = System.currentTimeMillis();
			} else {
				Toast.makeText(getApplicationContext(), "退出中...", Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
				finish();
			}
	        return true;   
	    }
	    */
	    return super.onKeyDown(keyCode, event);
	}
}


