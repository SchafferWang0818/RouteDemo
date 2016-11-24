package gaode.schaffer.routedemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.InfoWindowAdapter;
import com.amap.api.maps.AMap.OnInfoWindowClickListener;
import com.amap.api.maps.AMap.OnMapClickListener;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.RouteSearch.DriveRouteQuery;
import com.amap.api.services.route.RouteSearch.OnRouteSearchListener;
import com.amap.api.services.route.WalkRouteResult;

import java.util.ArrayList;
import java.util.List;


public class DriveRouteActivity extends Activity implements LocationSource, AMap.OnMapLoadedListener, OnMapClickListener,
        OnMarkerClickListener, OnInfoWindowClickListener, InfoWindowAdapter, OnRouteSearchListener, OnClickListener {
    protected TextView mTvSetPassPositions;
    private AMap aMap;
    private MapView mapView;
    private Context mContext;
    private RouteSearch mRouteSearch;
    private DriveRouteResult mDriveRouteResult;
    private LatLonPoint mStartPoint;
    private LatLonPoint mEndPoint;
//	private LatLonPoint mStartPoint = new LatLonPoint(39.942295,116.335891);//起点，39.942295,116.335891
//	private LatLonPoint mEndPoint = new LatLonPoint(39.995576,116.481288);//终点，39.995576,116.481288

    private final int ROUTE_TYPE_DRIVE = 2;

    private RelativeLayout mBottomLayout, mHeadLayout;
    private TextView mRotueTimeDes, mRouteDetailDes;
    private ProgressDialog progDialog = null;// 搜索时进度条
    private TextView mTvSetStartPosition;
    private TextView mTvSetEndPosition;
    List<LatLng> locations = new ArrayList<>();
    boolean isFirst = true;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明定位回调监听器
    public AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation aMapLocation) {
            if (listener != null) {
                listener.onLocationChanged(aMapLocation);
                //显示位置
            }
            if (aMapLocation != null) {
                if (aMapLocation.getErrorCode() == 0) {
                    //可在其中解析amapLocation获取相应内容。
                    double longitude = aMapLocation.getLongitude();//获取经度
                    double latitude = aMapLocation.getLatitude();//获取纬度
//                    Log.w("TAG", Thread.currentThread().getName());
                    if (isFirst) {
                        isFirst = false;
                        mTvSetStartPosition.setText("当前位置");
                        mStartPoint = new LatLonPoint(latitude, longitude);
                        aMap.addMarker(new MarkerOptions()
                                .position(AMapUtil.convertToLatLng(mStartPoint))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.start)));
                    }
                    if (hasStarted) {
                        LatLng latLng = new LatLng(latitude, longitude);
                        locations.add(latLng);
                        //绘制轨迹?
                        aMap.addPolyline(new PolylineOptions().addAll(locations).width(10).color(Color.RED));
                    }
                } else {
                    //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                    Log.e("AmapError", "location Error, ErrCode:"
                            + aMapLocation.getErrorCode() + ", errInfo:"
                            + aMapLocation.getErrorInfo());
                }
            }
        }
    };
    //配置发起定位的模式和相关参数
    private AMapLocationClientOption mLocationOption;
    private UiSettings uiSettings;
    private OnLocationChangedListener listener;
    private float route;
    private EditText mPriceEdt;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        super.setContentView(R.layout.route_activity);

        mContext = this.getApplicationContext();
        mapView = (MapView) findViewById(R.id.route_map);
        mapView.onCreate(bundle);// 此方法必须重写
        init();
        uiSettings = aMap.getUiSettings();
        aMap.setLocationSource(this);
        aMap.setMyLocationEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        mLocationClient.startLocation();
//        initView();
    }

    /**
     * 初始化AMap对象
     */
    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapLoadedListener(this);
            initLocationConfiguration();
        }
        registerListener();
        mRouteSearch = new RouteSearch(this);
        mRouteSearch.setRouteSearchListener(this);
        mBottomLayout = (RelativeLayout) findViewById(R.id.bottom_layout);
        mHeadLayout = (RelativeLayout) findViewById(R.id.routemap_header);
        mRotueTimeDes = (TextView) findViewById(R.id.firstline);
        mRouteDetailDes = (TextView) findViewById(R.id.secondline);
        mTvSetStartPosition = (TextView) findViewById(R.id.start_position_tv);
        mTvSetEndPosition = (TextView) findViewById(R.id.end_position_tv);
        mTvSetPassPositions = (TextView) findViewById(R.id.pass_position_tv);
        mPriceEdt = (EditText) findViewById(R.id.price_edt);
        mTvSetStartPosition.setOnClickListener(this);
        mTvSetEndPosition.setOnClickListener(this);
        mTvSetPassPositions.setOnClickListener(this);
        mHeadLayout.setVisibility(View.GONE);
    }

    private void initLocationConfiguration() {
        //初始化定位
        mLocationClient = new AMapLocationClient(this);
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        mLocationOption.setInterval(2000);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置是否强制刷新WIFI，默认为true，强制刷新。
        mLocationOption.setWifiActiveScan(false);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(false);
        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.setHttpTimeOut(20000);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
    }

    /**
     * 注册监听
     */
    private void registerListener() {
        aMap.setOnMapClickListener(DriveRouteActivity.this);
        aMap.setOnMarkerClickListener(DriveRouteActivity.this);
        aMap.setOnInfoWindowClickListener(DriveRouteActivity.this);
        aMap.setInfoWindowAdapter(DriveRouteActivity.this);

    }

    @Override
    public View getInfoContents(Marker arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public View getInfoWindow(Marker arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onInfoWindowClick(Marker arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onMarkerClick(Marker arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onMapClick(LatLng arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * 开始搜索路径规划方案
     */
    public void searchRouteResult(int routeType, int mode) {
        if (mStartPoint == null) {
            ToastUtil.show(mContext, "定位中，稍后再试...");
            return;
        }
        if (mEndPoint == null) {
            ToastUtil.show(mContext, "终点未设置");
        }
        showProgressDialog();
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                mStartPoint, mEndPoint);
        if (routeType == ROUTE_TYPE_DRIVE) {// 驾车路径规划
            DriveRouteQuery query = new DriveRouteQuery(fromAndTo, mode, null,
                    null, "");// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式，第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
            mRouteSearch.calculateDriveRouteAsyn(query);// 异步路径规划驾车模式查询
        }
    }

    @Override
    public void onBusRouteSearched(BusRouteResult result, int errorCode) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult result, int errorCode) {
        dissmissProgressDialog();
        aMap.clear();// 清理地图上的所有覆盖物
        if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getPaths() != null) {
                if (result.getPaths().size() > 0) {
                    mDriveRouteResult = result;
                    final DrivePath drivePath = mDriveRouteResult.getPaths()
                            .get(0);
                    DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                            mContext, aMap, drivePath,
                            mDriveRouteResult.getStartPos(),
                            mDriveRouteResult.getTargetPos(), null);
                    drivingRouteOverlay.setNodeIconVisibility(false);//设置节点marker是否显示
                    drivingRouteOverlay.setIsColorfulline(true);//是否用颜色展示交通拥堵情况，默认true
                    drivingRouteOverlay.removeFromMap();
                    drivingRouteOverlay.addToMap();
                    drivingRouteOverlay.zoomToSpan();
                    mBottomLayout.setVisibility(View.VISIBLE);
                    int dis = (int) drivePath.getDistance();
                    int dur = (int) drivePath.getDuration();
                    String des = AMapUtil.getFriendlyTime(dur) + "(" + AMapUtil.getFriendlyLength(dis) + ")";
                    mRotueTimeDes.setText(des);
                    mRouteDetailDes.setVisibility(View.VISIBLE);
                    int taxiCost = (int) mDriveRouteResult.getTaxiCost();
                    mRouteDetailDes.setText("打车约" + taxiCost + "元");
                    mBottomLayout.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(mContext,
                                    DriveRouteDetailActivity.class);
                            intent.putExtra("drive_path", drivePath);
                            intent.putExtra("drive_result",
                                    mDriveRouteResult);
                            startActivity(intent);
                        }
                    });

                } else if (result != null && result.getPaths() == null) {
                    ToastUtil.show(mContext, R.string.no_result);
                }

            } else {
                ToastUtil.show(mContext, R.string.no_result);
            }
        } else {
            ToastUtil.showerror(this.getApplicationContext(), errorCode);
        }


    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult result, int errorCode) {

    }


    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (progDialog == null)
            progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(true);
        progDialog.setMessage("正在搜索");
        progDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        deactivate();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        mLocationClient = null;
        listener = null;
    }

    @Override
    public void onRideRouteSearched(RideRouteResult arg0, int arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, PoiKeywordSearchActivity.class);
        Log.w("TAG",mEndPoint+"");
        switch (v.getId()) {
            case R.id.start_position_tv:
                //设置开始位置
                startActivityForResult(intent, 1);
                break;
            case R.id.end_position_tv:
                //设置最终位置
                startActivityForResult(intent, 2);
                break;
            case R.id.pass_position_tv:
                //设置途经位置
                startActivityForResult(intent, 3);
                break;
        }

    }

    /**
     * 获取当前的路线
     *
     * @param view
     */
    public void getRoute(View view) {

//            setfromandtoMarker();
//            searchRouteResult(ROUTE_TYPE_DRIVE, RouteSearch.DrivingDefault);
            Intent intent = new Intent(this,CustomRouteActivity.class);
            intent.putExtra("start_lat", mStartPoint.getLatitude());
            intent.putExtra("start_lon", mStartPoint.getLongitude());
            intent.putExtra("end_lat", mEndPoint.getLatitude());
            intent.putExtra("end_lon", mEndPoint.getLongitude());
            intent.putExtra("pass_position", passPositions);
            startActivity(intent);

    }


    double[] passPositions = new double[8];
    int time = 0;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        SerializableBean postion = (SerializableBean) data.getSerializableExtra("position");
        if (requestCode == 1) {
            //得到的是起点的位置
            mTvSetStartPosition.setText(postion.positionName);
            mStartPoint = new LatLonPoint(postion.latitude, postion.longitude);
            aMap.addMarker(new MarkerOptions()
                    .position(AMapUtil.convertToLatLng(mStartPoint))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.start)));

        } else if (requestCode == 2) {
            //得到的是终点的位置
            mTvSetEndPosition.setText(postion.positionName);
            mEndPoint = new LatLonPoint(postion.latitude, postion.longitude);
            aMap.addMarker(new MarkerOptions()
                    .position(AMapUtil.convertToLatLng(mEndPoint))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.end)));

        } else if (requestCode == 3) {
            //得到的是终点的位置
            mTvSetPassPositions.setText(postion.positionName);
//            mEndPoint = new LatLonPoint(postion.latitude, postion.longitude);
            if (time < 4) {
                time++;
                passPositions[time * 2 - 2] = postion.latitude;
                passPositions[time * 2 - 1] = postion.longitude;
            } else {
                Toast.makeText(this, "超出四个途经点", Toast.LENGTH_SHORT).show();
            }
            Log.w("TAG","终点"+mEndPoint);
        }
    }

    @Override
    public void onMapLoaded() {

    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        if (listener == null)
            listener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
//            mLocationClient = null;
//            listener = null;
        }
    }

    public void startRecord(View view) {
        if (mLocationClient != null) {
            mLocationClient.startLocation();
            locations.clear();
            mPriceEdt.setEnabled(false);
        }
    }

    public void endRecord(View view) {

        if (mLocationClient != null) {
            mPriceEdt.setEnabled(true);
            mLocationClient.stopLocation();
            route = 0f;
            for (int i = 0; i < locations.size() - 1; i++) {
                route += AMapUtils.calculateLineDistance(locations.get(i), locations.get(i + 1));
            }
            StringBuilder sb = new StringBuilder();
            sb.append("行走的路程为").append((long) route).append("m");
            if (!TextUtils.isEmpty(mPriceEdt.getText().toString())) {
                Log.w("TAG", route + "-" + Float.parseFloat(mPriceEdt.getText().toString()));
                float money = (long) route * Float.parseFloat(mPriceEdt.getText().toString()) / 1000f;
                sb.append(",需要").append(money).append("元");
            }
            Toast.makeText(this, sb.toString(), Toast.LENGTH_SHORT).show();
            locations.clear();
        }
    }

    public boolean hasStarted = false;

    public void startOrStop(View view) {
        Log.w("TAG", "是否为空" + (mLocationClient == null));
        if (!hasStarted) {
            startRecord(view);
            hasStarted = true;
        } else {
            endRecord(view);
            hasStarted = false;
        }
        Log.w("TAG", "当前是否开始-->" + hasStarted);
    }


}

