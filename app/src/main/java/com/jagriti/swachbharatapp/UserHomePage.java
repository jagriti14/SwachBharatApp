package com.jagriti.swachbharatapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.geojson.Point;
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
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserHomePage extends AppCompatActivity implements OnCameraTrackingChangedListener, PermissionsListener {

    private List<LatLng> latLngList;
    private ProgressDialog pd;
    private LatLng mCurrentLocation;
    private MapboxMap mMapboxMap;
    private PermissionsManager permissionsManager;
    private MapView mapView;
    private HashMap<LatLng,Float> distnaceHashMap;
    private int mCompleted = 0;
    private Style mStyle;
    private LatLng nearestBin;
    private static final String ID_ICON_BIN_GREEN = "green";
    private static final String ID_ICON_BIN_BLACK = "black";
    HashMap<Symbol, Integer> symbolIntegerHashMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1IjoiamFncml0aTE0OCIsImEiOiJja2NzcXRmNmcxYnR6MnFtbmdobW83bnFtIn0.TRccP9wNh08fp8KtV5VnOQ");
        setContentView(R.layout.activity_user_home_page);

        latLngList = new ArrayList<>();
        distnaceHashMap = new HashMap<>();
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

                        style.addImage(ID_ICON_BIN_GREEN, Objects.requireNonNull(BitmapFactory.decodeResource(UserHomePage.this.getResources(), R.drawable.green_bin)), false);
                        style.addImage(ID_ICON_BIN_BLACK, Objects.requireNonNull(BitmapFactory.decodeResource(UserHomePage.this.getResources(), R.drawable.green_bin)), true);
                    }
                });
            }
        });
    }


    private void readDataFromCloud() {
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Bins");
        ref.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    double latitude = dataSnapshot1.child("Location").child("Latitude").getValue(Double.class);
                    double longitude = dataSnapshot1.child("Location").child("Longitude").getValue(Double.class);
                    LatLng latLng = new LatLng(latitude, longitude);
                    latLngList.add(latLng);
                }
                pd.dismiss();
                calculateDistanceOfEachHashMap(latLngList);
                ref.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                pd.dismiss();
                Toast.makeText(UserHomePage.this, "Connection Error. Try Again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateDistanceOfEachHashMap(List<LatLng> latLngList) {
        Point p = Point.fromLngLat(mCurrentLocation.getLongitude(),mCurrentLocation.getLatitude());
        final float[] min_dis = {-1};
        for(LatLng destination:latLngList) {
            Point dest = Point.fromLngLat(destination.getLongitude(),destination.getLatitude());
            NavigationRoute.Builder builder = NavigationRoute.builder(this)
                    .accessToken(Mapbox.getAccessToken())
                    .origin(p)
                    .destination(dest)
                    .alternatives(true);
            builder.build()
                    .getRoute(new Callback<DirectionsResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<DirectionsResponse> call, @NonNull Response<DirectionsResponse> response) {
                            // You can get the generic HTTP info about the response
                            if (response.body() == null) {
                                Toast.makeText(UserHomePage.this, "No routes found, make sure you set the right user and access token.", Toast.LENGTH_SHORT).show();
                                return;
                            } else if (response.body().routes().size() < 1) {
                                Toast.makeText(UserHomePage.this, "No routes found.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            double distance = response.body().routes().get(0).distance();
                            float d = (float) distance;
                            if(min_dis[0] == -1 || min_dis[0] >d)
                            {
                                min_dis[0] = d;
                                nearestBin = destination;
                            }
                            distnaceHashMap.put(destination,d);
                        }

                        @Override
                        public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                            Toast.makeText(UserHomePage.this, "No routes found, make sure you set the right user and access token.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        setStyle();
    }

    private void setStyle() {
        //Set green for nearest bin and rest all set black
        LatLngBounds.Builder boundsBuilder;
        boundsBuilder = new LatLngBounds.Builder();
        SymbolManager symbolManager = new SymbolManager(mapView, mMapboxMap, mStyle);
        symbolManager.setIconAllowOverlap(true);
        symbolManager.setIconIgnorePlacement(true);
        if (latLngList != null){
            for (int i = 0; i < latLngList.size(); i++) {
                Symbol s = symbolManager.create(new SymbolOptions()
                        .withLatLng(latLngList.get(i))
                        .withIconImage(getImage(latLngList.get(i)))
                        .withIconSize(0.06f));
                symbolIntegerHashMap.put(s, i);
                boundsBuilder.include(latLngList.get(i));
            }
        }
        boundsBuilder.include(mCurrentLocation);
        CameraPosition position = mMapboxMap.getCameraForLatLngBounds(boundsBuilder.build(),new int[]{100,200,100,350});
        mMapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(Objects.requireNonNull(position)), 1000);

        symbolManager.addClickListener(new OnSymbolClickListener() {
            @Override
            public void onAnnotationClick(Symbol symbol) {
                int i = symbolIntegerHashMap.get(symbol);
                Constants.mLatLng = latLngList.get(i);
                startActivity(new Intent(UserHomePage.this,NavigationClass.class));
            }
        });


    }

    private String getImage(LatLng latLng) {
        if(latLng==nearestBin)
            return ID_ICON_BIN_GREEN;
        else
            return ID_ICON_BIN_BLACK;
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(MapboxMap mapboxMap, @NonNull Style loadedMapStyle) {
        if (PermissionsManager.areLocationPermissionsGranted(UserHomePage.this)) {
            LocationComponentOptions customLocationComponentOptions = LocationComponentOptions.builder(UserHomePage.this)
                    .elevation(5)
                    .accuracyAlpha(.6f)
                    .accuracyColor(Color.BLUE)
                    .build();

            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(UserHomePage.this, loadedMapStyle)
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
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(UserHomePage.this);
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
            Toast.makeText(UserHomePage.this, "Permission to read current location not granted", Toast.LENGTH_LONG).show();
        }
    }
}
