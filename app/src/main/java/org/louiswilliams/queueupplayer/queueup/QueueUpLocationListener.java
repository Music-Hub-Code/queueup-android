package org.louiswilliams.queueupplayer.queueup;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

public class QueueUpLocationListener implements LocationListener {

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final int MAX_UPDATE_TIME = 1000 * 60 * 15;
    private static final int MIN_DISTANCE = 100; // Meters

    private LocationManager locationManager;
    private Location currentBestLocation;

    private Criteria bestProviderCriteria;

    private boolean stopOnBest;

    public QueueUpLocationListener(LocationManager locationManager) {
        this.locationManager = locationManager;
        bestProviderCriteria = new Criteria();
        bestProviderCriteria.setAccuracy(Criteria.ACCURACY_FINE);

        /* Update the current best location */
        List<String> providers = locationManager.getProviders(true);
        for (String provider : providers) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (isBetterLocation(location, currentBestLocation)) {
                currentBestLocation = location;
            }
        }
        Log.d(QueueUp.LOG_TAG, "Current best location: " + currentBestLocation);
    }

    public Location getCurrentBestLocation() {
        Log.d(QueueUp.LOG_TAG, "Current best location: " + currentBestLocation);
        return currentBestLocation;
    }

    public void startPassiveUpdates() {
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,
                MAX_UPDATE_TIME, MIN_DISTANCE, this);
    }

    public void startUpdates() {
        Log.d(QueueUp.LOG_TAG, "Starting location updates");
        locationManager.requestLocationUpdates(0, 0, bestProviderCriteria, this, null);
    }

    public void stopUpdates() {
        Log.d(QueueUp.LOG_TAG, "Stopping location updates");
        locationManager.removeUpdates(this);
    }

    public void startUpdatesUntilBest() {
        stopOnBest = true;
        startUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location, currentBestLocation)) {
            Log.d(QueueUp.LOG_TAG, "Found better location: " + location);
            currentBestLocation = location;

            if (stopOnBest) {
                stopOnBest = false;
                stopUpdates();
            }
            /* If we have our best location and it is from the GPS, then stop updates*/
//            String bestProvider = locationManager.getBestProvider(bestProviderCriteria, true);
//            if (stopOnBest && location.getProvider().equals(bestProvider)){
//                stopUpdates();
//                stopOnBest = false;
//            } else {
//                Log.d(QueueUp.LOG_TAG, "Best provider: " + bestProvider);
//            }
        }

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    public boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > currentBestLocation.getAccuracy();

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
}