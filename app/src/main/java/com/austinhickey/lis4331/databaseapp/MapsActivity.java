package com.austinhickey.lis4331.databaseapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import net.sharewire.googlemapsclustering.Cluster;
import net.sharewire.googlemapsclustering.ClusterManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

	private GoogleMap mMap;
	private Location mCurrentLocation;
	private FusedLocationProviderClient fusedLocationClient;
	private RequestQueue mRequestQueue;
	private ArrayList<Player> playerArrayList = new ArrayList<>();
	private ClusterManager<Player> mClusterManager;
	private HeatmapTileProvider mProvider;
	private TileOverlay mOverlay;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);

		this.mRequestQueue = Volley.newRequestQueue(this);

		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			return;
		} else {
			setProgress("Finding current location",0,2);

			fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

			fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
				@Override
				public void onSuccess(Location location) {
					if(location != null) {
						mCurrentLocation = location;
						lowerTheGlobe();
					}
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.map_settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.switchMapMode:
				item.setChecked(!item.isChecked());
				if(item.isChecked()) {
					//heatmap on
					this.mClusterManager.setItems(new ArrayList<Player>());
					createHeatMap();
				} else {
					//heatmap off
					this.mOverlay.remove();
					createPlayerMapMarkers();
				}
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void lowerTheGlobe()
	{
		setProgress("Loading map", 1, 2);

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);

		mapFragment.getMapAsync(this);
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;

		mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
			@Override
			public void onInfoWindowClick(Marker marker) {
				doBrowserStuff("https://www.steamcommunity.com/profiles/" + marker.getSnippet());
			}
		});

		LatLng us = new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());

		mMap.addMarker(new MarkerOptions()
				.position(us)
				.title("Your Location")
				.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
		);
		mMap.moveCamera(CameraUpdateFactory.newLatLng(us));

		requestPlayerData();
	}

	private void requestPlayerData()
	{
		setProgress("Fetching player data", 0,0);

		JsonArrayRequest jsonPlayerLocations = new JsonArrayRequest(Request.Method.GET, "https://www.exiledservers.net/geoip/api.php", null, new Response.Listener<JSONArray>() {
			@Override
			public void onResponse(JSONArray response) {
				parsePlayerData(response);
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				error.printStackTrace();
			}
		});

		this.mRequestQueue.add(jsonPlayerLocations);
	}

	private void parsePlayerData(JSONArray playersJSON) {
		this.playerArrayList.clear();

		for (int i = 0; i < playersJSON.length(); i++) {
			setProgress(String.format(Locale.US,"Loading players(%d/%d)", i, playersJSON.length()), i, playersJSON.length());

			try {
				JSONObject p = playersJSON.getJSONObject(i);
				this.playerArrayList.add(new Player(p.getString("sid64"), p.getDouble("lat"), p.getDouble("lng")));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		setProgress();

		createPlayerMapMarkers();
		//createHeatMap();
	}

	private void createPlayerMapMarkers() {
		mClusterManager = new ClusterManager<>(this,this.mMap);

		this.mMap.setOnCameraIdleListener(mClusterManager);

		mClusterManager.setCallbacks(new ClusterManager.Callbacks<Player>() {
			@Override
			public boolean onClusterClick(@NonNull Cluster<Player> cluster) {
				return false;
			}

			@Override
			public boolean onClusterItemClick(@NonNull Player clusterItem) {
				return false;
			}
		});

		mClusterManager.setItems(playerArrayList);
	}

	private void createHeatMap() {
		List<LatLng> playerLocations = new ArrayList<>();

		for(Player p : playerArrayList)
			playerLocations.add(new LatLng(p.getLatitude(), p.getLongitude()));

		this.mProvider = new HeatmapTileProvider.Builder()
				.data(playerLocations)
				.radius(50)
				.build();

		this.mOverlay = this.mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
	}

	private void setProgress(String text, int current, int max)
	{
		LinearLayout progressBox = findViewById(R.id.progressBox);
		TextView progressText = findViewById(R.id.progressText);
		ProgressBar progressBar = findViewById(R.id.progressBar);

		progressBox.setVisibility(View.VISIBLE);

		progressText.setText(text);

		progressBar.setMax(max);
		progressBar.setProgress(current, true);

		if(max <= 0)
			progressBar.setIndeterminate(true);
	}

	private void setProgress()
	{
		LinearLayout progressBox = findViewById(R.id.progressBox);
		progressBox.setVisibility(View.GONE);
	}

	private void doBrowserStuff(String url)
	{
		this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
	}
}
