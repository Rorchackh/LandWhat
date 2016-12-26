package com.rorchackh.landwhat.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import rorchackh.com.landwhat.R;

public class MainActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private SharedPreferences preferenceManager;

    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                toggleLoader();

                LocationListener locationListener = new MyLocationListener(MainActivity.this);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "No permissions? No results for you sir.", Toast.LENGTH_SHORT).show();
                    return;
                }

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);

            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        preferenceManager = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent settingsActivity = new Intent(this, PreferencesActivity.class);
            startActivity(settingsActivity);
        }

        return super.onOptionsItemSelected(item);
    }

    private void findLandMarks(double latitude, double longitude) {
        String API_KEY = getString(R.string.GOOGLE_API_KEY);
        String urlString = String.format(
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?radius=%s&location=%s,%s&key=%s&types=%s",
            Integer.parseInt(preferenceManager.getString("radius", "50")),
            longitude,
            latitude,
            API_KEY,
            "art_gallery|library|mosque|museum|cemetery|church|city_hall|zoo|hindu_temple"
        );

        Log.i("URL USED", urlString);

        (new PlaceTask()).execute(urlString);
    }

    // Todo: needs to be implemented.
    private void toggleLoader() {
    }

    private class MyLocationListener implements LocationListener {

        private Context context;

        MyLocationListener(Context context) {
            super();
            this.context = context;
        }

        @Override
        public void onLocationChanged(android.location.Location loc) {
            Toast.makeText(context, "Location changed: Lat: " + loc.getLatitude() + " Lng: " + loc.getLongitude(), Toast.LENGTH_SHORT).show();
            findLandMarks(loc.getLongitude(), loc.getLatitude());
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {

        }
    }

    private class PlaceTask extends AsyncTask<String, Void, String> {

        private String body = "";

        protected String doInBackground(String... urlStrings) {

            try {

                URL url = new URL(urlStrings[0]);

                URLConnection con = url.openConnection();
                InputStream in = con.getInputStream();

                String encoding = con.getContentEncoding();
                encoding = encoding == null ? "UTF-8" : encoding;

                this.body = IOUtils.toString(in, encoding);

                Log.e("Body", this.body);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return this.body;
        }

        protected void onPostExecute(String body) {
            LinearLayout lay = (LinearLayout) findViewById(R.id.results);
            lay.removeAllViews();

            try {

                JSONObject jObject = new JSONObject(body);
                JSONArray results = jObject.getJSONArray("results");

                for (int i = 0; i < results.length(); i++) {
                    JSONObject result = results.getJSONObject(i);
                    String name = result.getString("name");

                    TextView t = new TextView(MainActivity.this);

                    t.setText(name);
                    t.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    t.setTextSize(18);
                    t.setTextColor(getResources().getColor(android.R.color.black));
                    t.setBackgroundResource(R.drawable.textlines);

                    Integer dimen = (int) getResources().getDimension(R.dimen.activity_vertical_margin);
                    t.setPadding(dimen, dimen, dimen, dimen);

                    t.setOnClickListener(new TextViewListener(name));

                    lay.addView(t);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        class TextViewListener implements View.OnClickListener {
            private String name;

            public TextViewListener(String name) {
                super();
                this.name = name;
            }
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("http://www.google.com/#q=" + this.name);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        }
    }
}
