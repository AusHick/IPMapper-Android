package com.austinhickey.lis4331.databaseapp;

import android.location.Location;
import net.sharewire.googlemapsclustering.ClusterItem;

public class Player implements ClusterItem {
	private String steamID64;
	private Location location;

	public Player(String sid64, double lat, double lng) {
		this.steamID64 = sid64;

		Location plLoc = new Location("Player" + sid64);

		plLoc.setLatitude(lat);
		plLoc.setLongitude(lng);

		this.location = plLoc;
	}

	@Override
	public double getLatitude() {
		return this.location.getLatitude();
	}

	@Override
	public double getLongitude() {
		return this.location.getLongitude();
	}

	@Override
	public String getTitle() {
		return "Player";
	}

	@Override
	public String getSnippet() {
		return this.steamID64;
	}
}
