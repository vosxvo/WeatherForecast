package com.vosxvo.weatherforecast;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.vosxvo.weatherforecast.api.OpenWeatherRespond;
import com.vosxvo.weatherforecast.api.OpenWeatherService;
import com.vosxvo.weatherforecast.ui.MainWeatherFragment;
import com.vosxvo.weatherforecast.ui.MoreWeatherFragment;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class MainActivity extends FragmentActivity {
    private static final String OpenWeather_API_KEY = BuildConfig.OpenWeatherApiKey;
    private static final int NUM_PAGES = 2;
    private static final long MIN_TIME_INTERVAL = 600000; //    5 min
    private static final float MIN_DISTANCE_UPDATE = 5000; //    5 km

    private ViewPager2 pager;
    private FragmentStateAdapter adapter;
    private LocationManager locationManager;
    private OpenWeatherRespond openWeatherRespond;
    private final LocationListener locationListener = location -> getWeather(location.getLatitude(), location.getLongitude());

    public static final String MAIN_WEATHER_REQUEST_KEY = "MAIN_WEATHER";
    public static final String MORE_WEATHER_REQUEST_KEY = "MORE_WEATHER";
    public static final String WEATHER_REQUEST_KEY = "WEATHER_REQUEST_KEY";
    @Inject
    public OpenWeatherService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.pager);
        adapter = new ScreenSlidePagerAdapter(this);
        pager.setAdapter(adapter);

        TabLayout tabLayout = findViewById(R.id.tabDots);
        new TabLayoutMediator(tabLayout, pager, (tab, position) -> {
        }).attach();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        if (savedInstanceState != null) {
            updateData(MAIN_WEATHER_REQUEST_KEY, savedInstanceState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkPermission();
        if (locationManager != null) locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onBackPressed() {
        if (pager.getCurrentItem() == 0) {
            super.onBackPressed();
        } else {
            pager.setCurrentItem(pager.getCurrentItem() - 1);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("geoLocation", openWeatherRespond.getName());
        outState.putString("main", openWeatherRespond.getWeather()[0].getMain());
        outState.putDouble("temp", openWeatherRespond.getMain().getTemp());
        outState.putDouble("tempMin", openWeatherRespond.getMain().getTempMin());
        outState.putDouble("tempMax", openWeatherRespond.getMain().getTempMax());
    }

    public void getWeather(double lat, double lon) {
        Call<OpenWeatherRespond> call = service.getOpenWeatherRespond(lat, lon, "metric", OpenWeather_API_KEY);
        call.enqueue(new Callback<OpenWeatherRespond>() {
            @Override
            public void onResponse(Call<OpenWeatherRespond> call, Response<OpenWeatherRespond> response) {
                openWeatherRespond = response.body();
                updateData(MAIN_WEATHER_REQUEST_KEY, makeMainWeatherBundle());
                updateData(MORE_WEATHER_REQUEST_KEY, makeMoreWeatherBundle());
            }

            @Override
            public void onFailure(Call<OpenWeatherRespond> call, Throwable t) {
                Log.e("Retrofit", t.getMessage());
            }
        });
    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 99);
            }
        }
    }

    public Bundle makeMainWeatherBundle() {
        if (openWeatherRespond == null) return null;

        Bundle bundle = new Bundle();
        bundle.putString("geoLocation", openWeatherRespond.getName());
        bundle.putString("main", openWeatherRespond.getWeather()[0].getMain());
        bundle.putDouble("temp", openWeatherRespond.getMain().getTemp());
        bundle.putDouble("tempMin", openWeatherRespond.getMain().getTempMin());
        bundle.putDouble("tempMax", openWeatherRespond.getMain().getTempMax());

        return bundle;
    }

    public Bundle makeMoreWeatherBundle() {
        if (openWeatherRespond == null) return null;

        Bundle bundle = new Bundle();
        bundle.putString("geoLocation", openWeatherRespond.getName() + " - " + openWeatherRespond.getSys().getCountry());
        bundle.putString("description", openWeatherRespond.getWeather()[0].getDescription());
        bundle.putDouble("temp", openWeatherRespond.getMain().getTemp());
        bundle.putDouble("tempMin", openWeatherRespond.getMain().getTempMin());
        bundle.putDouble("tempMax", openWeatherRespond.getMain().getTempMax());
        bundle.putDouble("feels_like", openWeatherRespond.getMain().getFeelsLike());
        bundle.putInt("humidity", openWeatherRespond.getMain().getHumidity());
        bundle.putDouble("speed", openWeatherRespond.getWind().getSpeed());
        bundle.putDouble("deg", openWeatherRespond.getWind().getDeg());
        bundle.putLong("sunrise", openWeatherRespond.getSys().getSunrise());
        bundle.putLong("sunset", openWeatherRespond.getSys().getSunset());

        return bundle;
    }

    public void updateLocation() {
        checkPermission();
        if (locationManager != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_INTERVAL,
                    MIN_DISTANCE_UPDATE, locationListener);
        }
    }

    public void updateData(String requestKey, Bundle result) {
        getSupportFragmentManager().setFragmentResult(requestKey, result);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @NotNull String[] permissions,
                                           @NonNull @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 99:
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(@NonNull
                                       @org.jetbrains.annotations.NotNull
                                               FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @NotNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new MainWeatherFragment();
            } else {
                return new MoreWeatherFragment();
            }
        }

        @Override
        public int getItemCount() {
            return NUM_PAGES;
        }
    }
}