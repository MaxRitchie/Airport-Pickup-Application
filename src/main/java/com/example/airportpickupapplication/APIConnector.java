package com.example.airportpickupapplication;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * This class controls the connection between the API and the classes, this being
 * the FlightInfoService class and the other classes, allowing information to be passed
 * through.
 */
public class APIConnector {
    private static APIConnector instance;
    private RequestQueue requestQueue;
    private static Context ctx;

    private APIConnector(Context context) {
        ctx = context;
        requestQueue = getRequestQueue();
    }

    public static synchronized APIConnector getInstance(Context context) {
        if (instance == null) {
            instance = new APIConnector(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    // Adds a request to the queue, takes it one by one to avoid request getting spammed to either
    // slow down the application or crash it.
    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}