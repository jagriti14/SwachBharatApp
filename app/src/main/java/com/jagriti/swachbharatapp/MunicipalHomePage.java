package com.jagriti.swachbharatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MunicipalHomePage extends AppCompatActivity implements OnCameraTrackingChangedListener, PermissionsListener {

    private HashMap<LatLng,String> latLngList;
    private ProgressDialog pd;
    private LatLng mCurrentLocation;
    private MapboxMap mMapboxMap;
    private MapView mapView;
    private Style mStyle;
    private static final String ID_ICON_BIN_GREEN = "green";
    private static final String ID_ICON_BIN_RED = "red";
    private static final String ID_ICON_BIN_YELLOW = "yellow";
    HashMap<Symbol, LatLng> symbolIntegerHashMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1IjoiamFncml0aTE0OCIsImEiOiJja2NzcXRmNmcxYnR6MnFtbmdobW83bnFtIn0.TRccP9wNh08fp8KtV5VnOQ");
        setContentView(R.layout.activity_municipal_home_page);

        latLngList = new HashMap<>();
        symbolIntegerHashMap = new HashMap<>();
        pd = new ProgressDialog(this);
        pd.setTitle("Loading");
        pd.setMessage("Please Wait.....");
        pd.show();

        readDataFromCloud();

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {
                mapboxMap.setStyle(Style.MAPBOX_STREETS,new Style.OnStyleLoaded() {

                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        mMapboxMap = mapboxMap;
                        enableLocationComponent(mapboxMap, style);

                        mStyle = style;
                        //Set Symbol
                        LatLngBounds.Builder boundsBuilder;
                        boundsBuilder = new LatLngBounds.Builder();
                        final HashMap<Symbol, Integer> symbolIntegerHashMap = new HashMap<>();

                        style.addImage(ID_ICON_BIN_GREEN, Objects.requireNonNull(BitmapFactory.decodeResource(MunicipalHomePage.this.getResources(), R.drawable.green_bin)), false);
                        style.addImage(ID_ICON_BIN_RED, Objects.requireNonNull(BitmapFactory.decodeResource(MunicipalHomePage.this.getResources(), R.drawable.green_bin)), false);
                        style.addImage(ID_ICON_BIN_RED, Objects.requireNonNull(BitmapFactory.decodeResource(MunicipalHomePage.this.getResources(), R.drawable.green_bin)), true);
                    }
                });
            }
        });
    }

    private void readDataFromCloud() {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Dumps");
        ref.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    double latitude = dataSnapshot1.child("Location").child("Latitude").getValue(Double.class);
                    double longitude = dataSnapshot1.child("Location").child("Longitude").getValue(Double.class);
                    String how_large_is_dump = dataSnapshot1.child("Dump_Size").getValue(String.class);
                    LatLng latLng = new LatLng(latitude, longitude);
                    latLngList.put(latLng,how_large_is_dump);
                }
                pd.dismiss();
                setStyle();
                ref.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                pd.dismiss();
                Toast.makeText(MunicipalHomePage.this, "Connection Error. Try Again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setStyle() {
        //Set green for nearest bin and rest all set black
        LatLngBounds.Builder boundsBuilder;
        boundsBuilder = new LatLngBounds.Builder();
        SymbolManager symbolManager = new SymbolManager(mapView, mMapboxMap, mStyle);
        symbolManager.setIconAllowOverlap(true);
        symbolManager.setIconIgnorePlacement(true);
        if (latLngList != null) {
            for (LatLng latLng: latLngList.keySet()) {
                Symbol s = symbolManager.create(new SymbolOptions()
                        .withLatLng(latLng)
                        .withIconImage(getImage(latLngList.get(latLng)))
                        .withIconSize(0.06f));
                symbolIntegerHashMap.put(s, latLng);
                boundsBuilder.include(latLng);
            }
        }
        boundsBuilder.include(mCurrentLocation);
        CameraPosition position = mMapboxMap.getCameraForLatLngBounds(boundsBuilder.build(),new int[]{100,200,100,350});
        mMapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(Objects.requireNonNull(position)), 1000);

        symbolManager.addClickListener(new OnSymbolClickListener() {
            @Override
            public void onAnnotationClick(Symbol symbol) {
                Constants.mLatLng = symbolIntegerHashMap.get(symbol);
                startActivity(new Intent(MunicipalHomePage.this,NavigationClass.class));
            }
        });


    }

    private String getImage(String s) {
        if(s.equals("small"))
            return ID_ICON_BIN_GREEN;
        else if(s.equals("medium"))
            return ID_ICON_BIN_YELLOW;
        else
            return ID_ICON_BIN_RED;
    }


    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(MapboxMap mapboxMap, @NonNull Style loadedMapStyle) {
        if (PermissionsManager.areLocationPermissionsGranted(MunicipalHomePage.this)) {
            LocationComponentOptions customLocationComponentOptions = LocationComponentOptions.builder(MunicipalHomePage.this)
                    .elevation(5)
                    .accuracyAlpha(.6f)
                    .accuracyColor(Color.BLUE)
                    .build();

            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(MunicipalHomePage.this, loadedMapStyle)
                            .locationComponentOptions(customLocationComponentOptions)
                            .build();

            locationComponent.activateLocationComponent(locationComponentActivationOptions);
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
            locationComponent.addOnCameraTrackingChangedListener(this);

            Location location = locationComponent.getLastKnownLocation();
            mCurrentLocation = new LatLng(location.getLatitude(),location.getLongitude());
        }

        else {
            PermissionsManager permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(MunicipalHomePage.this);
        }


    }

    @Override
    public void onCameraTrackingDismissed() {

    }

    @Override
    public void onCameraTrackingChanged(int currentMode) {

    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mMapboxMap.getStyle(style -> enableLocationComponent(mMapboxMap,style));
        } else {
            Toast.makeText(MunicipalHomePage.this, "Permission to read current location not granted", Toast.LENGTH_LONG).show();
        }
    }

}
