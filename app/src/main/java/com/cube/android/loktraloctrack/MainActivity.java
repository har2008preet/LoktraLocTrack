package com.cube.android.loktraloctrack;

import android.Manifest;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, SeekBar.OnSeekBarChangeListener {

    private static final int MY_PERMISSIONS_LOCATION = 100;
    private static final int REQUEST_PERMISSION_SETTING = 101;
    private static final String TAG = "LocationActivity";
    GoogleMap mGoogleMap;
    String mLastUpdateTime;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FrameLayout frame;
    LayoutInflater li;
    FrameLayout.LayoutParams params;
    LocationManager locationManager;
    private ProgressBar progress;
    private SupportMapFragment googleMap;
    private boolean sentToSettings = false;
    private boolean isConnected;
    private ArrayList<LatLng> locations = new ArrayList<>();
    private boolean isShift;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progress = (ProgressBar) findViewById(R.id.progressBar1);
        buildGoogleApiClient();

        initilizeMap();
        setSwipeBar();


    }

    private void setSwipeBar() {
        frame = (FrameLayout) findViewById(R.id.frameActivity);
        li = LayoutInflater.from(this);
        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.setMargins(60, 0, 60, 60);
        SeekBar seekBar;
        if (!isShift) {
            View view = li.inflate(R.layout.swipe_bar_start_shift, frame, false);
            view.setLayoutParams(params);
            frame.addView(view);
            seekBar = (SeekBar) view.findViewById(R.id.seekbar);
        } else {
            View view = li.inflate(R.layout.swipe_bar_stop_swift, frame, false);
            view.setLayoutParams(params);
            frame.addView(view);
            seekBar = (SeekBar) view.findViewById(R.id.seekbar);
        }
        seekBar.setOnSeekBarChangeListener(this);
    }


    private void initilizeMap() {
        if (googleMap == null) {
            googleMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(
                    R.id.map));
            locationManager= (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
            googleMap.getMapAsync(this);

            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry! unable to create maps", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();

        locations.add(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
        Toast.makeText(this, "onPause() called", Toast.LENGTH_SHORT).show();
        android.location.LocationListener locationListener = getLocationListener();

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    }

    @NonNull
    private android.location.LocationListener getLocationListener() {
        return new android.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng newLatlng = new LatLng(location.getLatitude(), location.getLongitude());
                locations.add(newLatlng);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }

    @Override
    public void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    @Override
    protected void onResume() {
        super.onResume();
        /*initilizeMap();*/
        mGoogleApiClient.connect();

        if (!locations.isEmpty()) {
            int pos = locations.size();
            for (int i = 1; i < pos; i++) {
                mGoogleMap.addPolyline((new PolylineOptions())
                        .add(locations.get(i - 1), locations.get(i)).width(15).color(Color.BLUE)
                        .geodesic(true));
            }
            locations.clear();
        }

    }


    @Override
    public void onMapReady(GoogleMap map) {
        mGoogleMap = map;
        MainActivityPermissionsDispatcher.createMapWithCheck(this, mGoogleMap);
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void createMap(final GoogleMap map) {
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        map.setMyLocationEnabled(true);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(6000);
        mLocationRequest.setFastestInterval(1000);
        //mLocationRequest.setSmallestDisplacement(10);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        createLocationRequest();

    }

    @Override
    public void onLocationChanged(Location location) {
        Location lastLocation;
        LatLng previosLatLng = null;
        if (isShift) {
            lastLocation = mLastLocation;
            previosLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        }
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        //Toast.makeText(this, "Location changed", Toast.LENGTH_SHORT).show();
        Log.wtf(TAG, "Location changed");

        LatLng currentLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        if (isShift){
            mGoogleMap.addPolyline((new PolylineOptions())
                    .add(currentLatLng, previosLatLng).width(15).color(Color.BLUE)
                    .geodesic(true));
        }
        //addMarker(currentLatLng);

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16));
    }

    private void addMarker(LatLng currentLatLng) {
        MarkerOptions options = new MarkerOptions();
        options.position(currentLatLng);
        Marker mapMarker = mGoogleMap.addMarker(options);
        long atTime = mLastLocation.getTime();
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date(atTime));
        mapMarker.setTitle(mLastUpdateTime);
    }


    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (seekBar.getProgress() <= 5) {
            seekBar.setProgress(5);
        }
        if (seekBar.getProgress() >= 95) {
            seekBar.setProgress(95);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (!isShift) {
            if (seekBar.getProgress() < 50) {
                seekBar.setProgress(5);
                Toast.makeText(MainActivity.this, "Swipe full to Right to start SHIFT", Toast.LENGTH_SHORT).show();
            } else {
                isShift = true;
                startSHIFT();
                frame = (FrameLayout) findViewById(R.id.frameActivity);
                li = LayoutInflater.from(MainActivity.this);
                params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                params.setMargins(60, 0, 60, 60);
                View view = li.inflate(R.layout.swipe_bar_stop_swift, frame, false);
                view.setLayoutParams(params);
                frame.addView(view);
                SeekBar seekBar1 = (SeekBar) view.findViewById(R.id.seekbar);
                seekBar1.setOnSeekBarChangeListener(this);
            }
        } else {
            if (seekBar.getProgress() > 50) {
                seekBar.setProgress(95);
                Toast.makeText(MainActivity.this, "Swipe full to LEFT to stop SHIFT", Toast.LENGTH_SHORT).show();
            } else {
                isShift = false;
                stopSHIFT();
                frame = (FrameLayout) findViewById(R.id.frameActivity);
                li = LayoutInflater.from(MainActivity.this);
                params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                params.setMargins(60, 0, 60, 60);
                View view = li.inflate(R.layout.swipe_bar_start_shift, frame, false);
                view.setLayoutParams(params);
                frame.addView(view);
                seekBar = (SeekBar) view.findViewById(R.id.seekbar);
            }
            seekBar.setOnSeekBarChangeListener(this);
        }
    }

    private void stopSHIFT() {
        LatLng currentLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        addMarker(currentLatLng);

    }

    private void startSHIFT() {
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);
        LatLng currentLatLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        addMarker(currentLatLng);
    }
}


