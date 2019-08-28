package com.appmoviles.maps;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.PolyUtil;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.Queue;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleMap.OnMapLongClickListener {

    private final static int LOCATION_REQUEST_CODE = 12;

    private final static int REFRESH_COORDINATES_RATE = 1000;

    private GoogleMap mMap;

    private ImageView icesiImg;

    private Polygon icesiArea;

    private Marker myUbication;

    private Queue<Marker> fifo;

    private TextView distanceTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        icesiImg = findViewById(R.id.icesi_img);
        distanceTv = findViewById(R.id.distance_tv);

        fifo = new LinkedList<>();

        ActivityCompat.requestPermissions(this, new String[] {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, LOCATION_REQUEST_CODE);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        icesiArea = mMap.addPolygon(new PolygonOptions().add(
                new LatLng(3.342896,-76.530736),
                new LatLng(3.338633,-76.531240),
                new LatLng(3.338719,-76.528205),
                new LatLng(3.343238,-76.527894)
        ));

        // Add a marker in Sydney and move the camera
        LatLng icesi = new LatLng(3.342896,-76.530736);
        myUbication = mMap.addMarker(new MarkerOptions().position(icesi).title("Marker in Icesi"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(icesi, 15));

        // Request to the sensor to start to measure
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, REFRESH_COORDINATES_RATE, 0, this);
    }

    // LOCATION UPDATES (SENSOR)

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            LatLng newCoordinate = new LatLng(location.getLatitude(), location.getLongitude());
            myUbication.setPosition(newCoordinate);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newCoordinate, 15));

            boolean amIinIcesi = PolyUtil.containsLocation(newCoordinate, icesiArea.getPoints(), true);
            if (amIinIcesi) {
                icesiImg.setVisibility(View.VISIBLE);
            } else {
                icesiImg.setVisibility(View.GONE);
            }
        }
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

    // ON LONG CLICK IN MAP

    @Override
    public void onMapLongClick(LatLng latLng) {
        Marker marker = mMap.addMarker(new MarkerOptions().position(latLng));
        if (fifo.size() < 2) {
            fifo.add(marker);
        } else {
            Marker deprecatedMarker = fifo.poll();
            deprecatedMarker.remove();
            fifo.add(marker);
        }

        if (fifo.size() == 2) {
            Marker a = fifo.poll();
            Marker b = fifo.poll();
            fifo.add(a);
            fifo.add(b);

            double deltaLatitude = a.getPosition().latitude - b.getPosition().latitude;
            double deltaLongitude = a.getPosition().longitude - b.getPosition().longitude;
            double distance = Math.sqrt(Math.pow(deltaLatitude, deltaLongitude));
            distance = distance * 111.12 * 1000;
            DecimalFormat format = new DecimalFormat("#####.####");
            distanceTv.setText(format.format(distance));
        }
    }

    @Override
    protected void onPause() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("text", distanceTv.getText().toString());
        editor.apply();
        super.onPause();
    }

    @Override
    protected void onResume() {
        String text = PreferenceManager.getDefaultSharedPreferences(this).getString("text", "0");
        distanceTv.setText(text);
        super.onResume();
    }
}
