package com.jagriti.swachbharatapp;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.matrix.v1.MapboxMatrix;
import com.mapbox.api.matrix.v1.models.MatrixResponse;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.InstructionListListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class NavigationClass extends AppCompatActivity implements OnNavigationReadyCallback,
        NavigationListener, ProgressChangeListener, InstructionListListener, SpeechAnnouncementListener,
        BannerInstructionsListener, MapboxMap.OnMarkerClickListener, RouteListener, OnMapReadyCallback {

    private NavigationView navigationView;
    private static final int INITIAL_ZOOM = 16;

    private View spacer;
    //private TextView speedWidget;
    private FloatingActionButton refresh;
    //private FloatingActionButton fabStyleToggle;

    private boolean bottomSheetVisible = true;
    private boolean instructionListShown = false;

    private StyleCycle styleCycle = new StyleCycle();


    //Origin and Destination
    private Point mOrigin;


    //Weather
    long unixTime;
    HashMap<Point,Long> weatherPointTimeHashMap;
    int mCompleted;
    int flag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat_Light_NoActionBar);
        initNightMode();
        Mapbox.getInstance(this, "pk.eyJ1IjoibXNvZnR0ZXhhc2NvbnN1bHRhbmN5IiwiYSI6ImNqcXBpaDdvdDAxcmQ0MnJ0OGxxODJyMG0ifQ.yWdirT8JDDeACaSfA9NF3A");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_class);

        //Initialise Origin
        //mOrigin = Constants.ORIGIN;
        mOrigin = Point.fromLngLat(Constants.mOrigin.getLongitude(),Constants.mOrigin.getLatitude());


        navigationView = findViewById(R.id.navigationView);
        refresh = findViewById(R.id.fabToggleNightMode);
        //fabStyleToggle = findViewById(R.id.fabToggleStyle);
        //speedWidget = findViewById(R.id.speed_limit);
        spacer = findViewById(R.id.spacer);
        setSpeedWidgetAnchor(R.id.summaryBottomSheet);



        CameraPosition initialPosition = new CameraPosition.Builder()
                .target(new LatLng(Constants.mOrigin.getLatitude(),Constants.mOrigin.getLongitude()))
                .zoom(INITIAL_ZOOM)
                .build();

        navigationView.onCreate(savedInstanceState);
        navigationView.initialize(this, initialPosition);

    }

    //////////////////////////////////////Navigation Override///////////////////////////
    @Override
    public void onNavigationReady(boolean isRunning) {
        fetchRoute();
    }

    @Override
    public BannerInstructions willDisplay(BannerInstructions instructions) {

        return instructions;
    }

    @Override
    public void onInstructionListVisibilityChanged(boolean shown) {
        instructionListShown = shown;
        //speedWidget.setVisibility(shown ? View.GONE : View.VISIBLE);
        if (instructionListShown) {
            refresh.hide();
        } else if (bottomSheetVisible) {
            refresh.show();
        }
    }

    @Override
    public void onCancelNavigation() {
        finish();
    }

    @Override
    public void onNavigationFinished() {
    }

    @Override
    public void onNavigationRunning() {

    }

    @Override
    public SpeechAnnouncement willVoice(SpeechAnnouncement announcement) {
        return announcement;
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {
        if (flag == 0) {
            navigationView.retrieveNavigationMapboxMap().retrieveMap().setStyle(Style.MAPBOX_STREETS);
            flag =1;
        }
        //setSpeed(location);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        return false;
    }


    ////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////Other Override///////////////////////////////////

    @Override
    public void onStart() {
        super.onStart();
        navigationView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.onResume();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
    }

    @Override
    public void onBackPressed() {
        // If the navigation view didn't need to do anything, call super
        if (!navigationView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        navigationView.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        navigationView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        navigationView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationView.onDestroy();
        if (isFinishing()) {
            saveNightModeToPreferences(AppCompatDelegate.MODE_NIGHT_AUTO);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        }
    }

    @Override
    public boolean allowRerouteFrom(Point offRoutePoint) {
        mOrigin = offRoutePoint;
        return true;
    }

    @Override
    public void onOffRoute(Point offRoutePoint) {
    }

    @Override
    public void onRerouteAlong(DirectionsRoute directionsRoute) {
        if(navigationView.retrieveNavigationMapboxMap().retrieveMap()!=null) {
            navigationView.retrieveNavigationMapboxMap().retrieveMap().clear();
            Constants.route = directionsRoute;
        }
    }

    @Override
    public void onFailedReroute(String errorMessage) {

    }

    @Override
    public void onArrival() {

    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {

    }

    ///////////////////////////////////////////////////////////////////////////

    private static class StyleCycle {
        private static final String[] STYLES = new String[]{
                Style.MAPBOX_STREETS,
                Style.OUTDOORS,
                Style.LIGHT,
                Style.DARK,
                Style.SATELLITE_STREETS
        };

        private int index;

        private String getNextStyle() {
            index++;
            if (index == STYLES.length) {
                index = 0;
            }
            return getStyle();
        }

        private String getStyle() {
            return STYLES[index];
        }
    }

    private void initNightMode() {
        int nightMode = retrieveNightModeFromPreferences();
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    private int retrieveNightModeFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getInt(getString(R.string.current_night_mode), AppCompatDelegate.MODE_NIGHT_AUTO);
    }

    /**
     * Sets the anchor of the spacer for the speed widget, thus setting the anchor for the speed widget
     * (The speed widget is anchored to the spacer, which is there because padding between items and
     * their anchors in CoordinatorLayouts is finicky.
     *
     * @param res resource for view of which to anchor the spacer
     */
    private void setSpeedWidgetAnchor(@IdRes int res) {
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) spacer.getLayoutParams();
        layoutParams.setAnchorId(res);
        spacer.setLayoutParams(layoutParams);
    }

    ////////////////////////////Start Navigation/////////////////////////////////
    private void startNavigation(DirectionsRoute directionsRoute) {
        NavigationViewOptions.Builder options =
                NavigationViewOptions.builder()
                        .navigationListener(this)
                        .directionsRoute(directionsRoute)
                        .shouldSimulateRoute(false)
                        .progressChangeListener(this)
                        .instructionListListener(this)
                        .speechAnnouncementListener(this)
                        .bannerInstructionsListener(this)
                        .routeListener(this)
                        .offlineRoutingTilesPath(obtainOfflineDirectory())
                        .offlineRoutingTilesVersion(obtainOfflineTileVersion());
        setBottomSheetCallback(options);


        //setupStyleFab();
        setupNightModeFab();


        MapboxMap mapboxMap = navigationView.retrieveNavigationMapboxMap().retrieveMap();
        mapboxMap.setOnMarkerClickListener(this);
        navigationView.startNavigation(options.build());

    }

    private String obtainOfflineDirectory() {
        File offline = Environment.getExternalStoragePublicDirectory("Offline");
        if (!offline.exists()) {
            Timber.d("Offline directory does not exist");
            offline.mkdirs();
        }
        return offline.getAbsolutePath();
    }

    private String obtainOfflineTileVersion() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.offline_version_key), "");
    }

    private void fetchRoute() {
        startNavigation(Constants.route);
    }

    private void setBottomSheetCallback(NavigationViewOptions.Builder options) {
        options.bottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                        bottomSheetVisible = false;
                        refresh.hide();
                        setSpeedWidgetAnchor(R.id.recenterBtn);
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        bottomSheetVisible = true;
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        if (!bottomSheetVisible) {
                            refresh.show();
                            setSpeedWidgetAnchor(R.id.summaryBottomSheet);
                        }
                        break;
                    default:
                        return;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
    }

//    private void setupStyleFab() {
//        fabStyleToggle.setOnClickListener(new View.OnClickListener() {
//                                              @Override
//                                              public void onClick(View v) {
//                                                  Log.d("Jags", "clicked");
//                                                  navigationView.retrieveNavigationMapboxMap().retrieveMap().setStyle(getStyle(getSharedPreferences(Constants.SHARED_PREF,MODE_PRIVATE).getString(Constants.MAP_THEME,Constants.STREET)));
//                                              }
//                                          });
//    }

    private void setupNightModeFab() {
        refresh.setOnClickListener(view -> {
            if(navigationView.retrieveNavigationMapboxMap().retrieveMap()!=null) {
                navigationView.retrieveNavigationMapboxMap().retrieveMap().clear();
            }
        });
    }

    private void toggleNightMode() {
        int currentNightMode = getCurrentNightMode();
        alternateNightMode(currentNightMode);
    }

    private int getCurrentNightMode() {
        return getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
    }

    private void alternateNightMode(int currentNightMode) {
        int newNightMode;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            newNightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            newNightMode = AppCompatDelegate.MODE_NIGHT_YES;
        }
        saveNightModeToPreferences(newNightMode);
        recreate();
    }

    private void saveNightModeToPreferences(int nightMode) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(getString(R.string.current_night_mode), nightMode);
        editor.apply();
    }

    private void setSpeed(Location location) {
    }





}
