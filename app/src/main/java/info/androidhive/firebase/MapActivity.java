package info.androidhive.firebase;

import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

public class MapActivity extends AppCompatActivity {

    public MapView mapView;
    double longitude;
    double latitude;
    CameraPosition cameraPosition;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1IjoicHJpdGFtMTQzIiwiYSI6ImNqZmR0cmxsejFydmUycW1zamljaDY1dXYifQ.4MDCnCrRYGnRrvO6oN7K_w");
        setContentView(R.layout.activity_map);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        final String lat=getIntent().getStringExtra("DataLat");
        final String lng=getIntent().getStringExtra("DataLng");
        final Location location = getIntent().getParcelableExtra("last");
        latitude = Double.parseDouble(lat);
        longitude= Double.parseDouble(lng);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                mapboxMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude,longitude))
                .title("I am Here")
                .snippet("Wellcome to My Location"));
                mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location),15));
            }
        });
        Log.d("ExtraData-",location+"");
        Log.d("ExtraData-",latitude+" "+longitude+"");
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

}
