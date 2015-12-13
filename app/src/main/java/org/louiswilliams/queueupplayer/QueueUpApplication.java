package org.louiswilliams.queueupplayer;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;


public class QueueUpApplication extends Application {

    private Tracker mTracker;
//
//    synchronized public Tracker getDefaultTracker() {
//        GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
//        mTracker = analytics.newTracker(R.xml.global_tracker);
//        return mTracker;
//    }

}
