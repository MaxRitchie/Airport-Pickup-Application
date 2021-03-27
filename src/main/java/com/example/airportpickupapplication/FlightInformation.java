package com.example.airportpickupapplication;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * This class is called when the user searches for a flight from the flight searcher screen, it gets initiated when
 * the search button is clicked. It contains the functions that get the relevant flight information to be displayed along with
 * all of the Google Maps features that displays markers, routes and the calculations for the leaving time.
 */
public class FlightInformation extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "Map";
    private int ACCESS_LOCATION_REQUEST_CODE = 10001;

    private TextView flightCodeTextView;
    private TextView flightLandingTimeTextView;
    private TextView flightStatusTextView;
    private TextView flightDelayTextView;
    private TextView timeToLeaveTextView;

    private Button backButton;
    private Button startButton;
    private Button stopButton;

    private FlightInfoService flightInfoService;
    private MainActivity mainActivity;

    private String flightCode = "";
    private String flightCodeSearch = "Flight Code";
    private String flightStatusSearch = "Flight Status";
    private String flightLandingTimeSearch = "Flight Landing Time";
    private String flightDelaySearch = "Flight Delay";
    private String flightArrivalAirport = "Airport";
    private String timeToLeave;

    private FusedLocationProviderClient fusedLocationProviderClient;

    private FusedLocationProviderClient mFusedLocationProviderClient;

    private LocationRequest locationRequest;

    private Marker userLocationMarker;

    private GoogleMap mMap;

    boolean isStarted;

    private LatLng glasgowAirportLatLng;
    private LatLng edinburghAirportLatLng;
    private LatLng heathrowAirport;
    private LatLng liverpoolJohnLennonAirport;
    private LatLng manchesterAirport;
    private LatLng dublinInternationalAirport;

    private LatLng mOrigin;
    private LatLng mDestination;

    /**
     * This is where the activity is initialised and the content view is set to the flight searcher screen xml file. Here is
     * where the methods are called to start the application.
     *
     * @param savedInstanceState A reference to a Bundle object that is passed into the method.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sets the layout, this is what gets called to be displayed to the screen.
        setContentView(R.layout.flight_information_screen);
        // Method call to initialise all required objects used in the class.
        initialise();
        // Method call to set the flight code that will be searched for.
        setFlightCode();
        // Method call that gets all the desired flight information.
        getFlightInfo();
        // Method call that controls the actions of the button that are on the flight information screen.
        buttonActions();
        // Method call to initialise the Google Maps feature.
        initGoogleMap();
    }

    /**
     * This method initialises all the required objects needed to call other classes and locate objects from the xml layout files.
     */
    private void initialise() {

        flightInfoService = new FlightInfoService(FlightInformation.this);
        mainActivity = new MainActivity();

        flightCodeTextView = findViewById(R.id.flightCode);
        flightLandingTimeTextView = findViewById(R.id.flightLandingTime);
        flightStatusTextView = findViewById(R.id.flightStatus);
        flightDelayTextView = findViewById(R.id.flightDelay);
        timeToLeaveTextView = findViewById(R.id.timeToLeave);

        backButton =findViewById(R.id.backButton);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        isStarted = false;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(200);
        locationRequest.setFastestInterval(200);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        glasgowAirportLatLng = new LatLng(55.86998215451676, -4.435320336935214);
        edinburghAirportLatLng = new LatLng(55.95920708781988, -3.358570120649192);
        heathrowAirport = new LatLng(51.48500062119503, -0.4520150082062885);
        liverpoolJohnLennonAirport = new LatLng(53.373711938109594, -2.8297960274266933);
        manchesterAirport = new LatLng(53.42614918664477, -2.2416438771298433);
        dublinInternationalAirport = new LatLng(53.46443193392894, -6.1914418823499755);
    }

    /**
     * Initialises the Google maps fragment using the maps view in the flight information
     * xml file.
     */
    private void initGoogleMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * This methods gets the flight code passed through from the searcher screen and sets it to
     * the flightCode variable to then be used in this class.
     */
    public void setFlightCode() {
        Intent intent = getIntent();
        flightCode = intent.getExtras().getString("flightCode");
    }

    /**
     * Holds the method calls for getting the required flight information for the text views.
     */
    public void getFlightInfo() {
        searchFlightCode(flightCode, flightCodeSearch);
        searchFlightCode(flightCode, flightLandingTimeSearch);
        searchFlightCode(flightCode, flightStatusSearch);
        searchFlightCode(flightCode, flightDelaySearch);
        searchFlightCode(flightCode, flightArrivalAirport);
    }

    /**
     * /**
     * This method searchers for the flight using the flight code that the user has entered, the getFlightData method
     * from the FlightInfoService gets called and returns the desired flight. When a flight has been found, the displayManager
     * method gets called to proceed onto the FlightInformation screen. An error message gets displayed to the screen if an
     * error occurs during the flight search.
     *
     * @param flightCode   This is the users input that gets passed in.
     * @param requiredData This is the parameter used to search for a certain type of data that gets passed in.
     */
    public void searchFlightCode(String flightCode, String requiredData) {
        // Calls the method getFlightData from the FlightInfoService class and passes in the flightCode that will be used to search,
        // the requiredData being what will be searched for and the VolleyResponseListener which is used for passing the data back
        // from the flight service class.
        flightInfoService.getFlightData(flightCode, requiredData, new FlightInfoService.VolleyResponseListener() {
            @Override
            // Controls the response for errors that occur when searching for a flight.
            public void onError(String message) {
                // Displays an error message to the screen if something wrong happens when searching for the flight.
                Toast.makeText(FlightInformation.this, "Error, Please Try Again", Toast.LENGTH_LONG).show();
            }

            @Override
            // Controls the response when a flight has been found then carries out the next action.
            public void onResponse(String flightData) {
                // Calls the setFlightInfo method by passing in the returned flight data along with the required data.
                setFlightInfo(requiredData, flightData);
            }
        });
    }

    /**
     * Sets the data for the text views after the correct flight has been found.
     *
     * @param requiredData This is the parameter used to search for a certain type of data that gets passed in.
     * @param flightData   This is the returned value that has been search for and returned from the
     *                     FlightInfoService Class.
     */
    public void setFlightInfo(String requiredData, String flightData) {
        // If statement used to check that the required data is currently set to flight code, if it is, the data held in the
        // flightData variable gets as the text in the textView.
        if (requiredData.equals(flightCodeSearch)) {
            flightCodeTextView.setText(flightData);
        }

        // If statement used to check that the required data is currently set to flight landing time, if it is, the data held in the
        // flightData variable gets as the text in the textView.
        if (requiredData.equals(flightLandingTimeSearch)) {
            flightData = convertDate(flightData);
            flightLandingTimeTextView.setText(flightData);
        }

        // If statement used to check that the required data is currently set to flight status, if it is, the data held in the
        // flightData variable gets as the text in the textView.
        if (requiredData.equals(flightStatusSearch)) {
            // Sets the first character of the returned to upper case and saved back into the flightData variable.
            flightData = flightData.substring(0, 1).toUpperCase() + flightData.substring(1);
            flightStatusTextView.setText(flightData);
        }

        // If statement used to check that the required data is currently set to flight delay, if it is, the data held in the
        // flightData variable gets as the text in the textView.
        if (requiredData.equals(flightDelaySearch)) {
            // Sets this as the default value in the textView, it will stay like this if there is null value is
            // returned in flightData variable.
            flightDelayTextView.setText("No Delay");

            // If statement used to check that the flightData does not contain null and returns a valid value, if a value is
            // found, it gets set in the textView along with concatenating the String with minutes as the value returned is an
            // integer in minutes.
            if (!flightData.equals(null) && !flightData.equals("")) {
                flightDelayTextView.setText(flightData + " minute(s)");
            }
        }

        // If statement used to check that the required data is currently set to flight airport, if it is, the data held in the
        // flightData variable gets as the text in the textView.
        if (requiredData.equals(flightArrivalAirport)) {
            // Case statements us to check what airports have been returned, which then sets the LatLang of the returned airport
            // by passing the value into the setAirportLocation method.
            switch (flightData) {
                case "Glasgow International":
                    setAirportLocation(glasgowAirportLatLng);
                    break;
                case "Edinburgh":
                    setAirportLocation(edinburghAirportLatLng);
                    break;
                case "Heathrow":
                    setAirportLocation(heathrowAirport);
                    break;
                case "Liverpool John Lennon":
                    setAirportLocation(liverpoolJohnLennonAirport);
                    break;
                case "Manchester Airport":
                    setAirportLocation(manchesterAirport);
                    break;
                case "Dublin International":
                    setAirportLocation(dublinInternationalAirport);
                    break;
                // If a flight code that contains an airport that is not listed in the else if statements then an error message is
                // displayed to the screen and the Google Maps feature does not work along with the calculated time to leave but the flights
                // information is still displayed to the user.
                default:
                    Toast.makeText(this, "Unable Display Directions and Leaving Time for this Flight", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    /**
     * Holds all the on click listeners for the buttons that are clickable and held on the flight information screen.
     */
    public void buttonActions() {
        // Back button allows the user to go back to the flight searcher screen and search for another flight.
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Sets what screen is currently being displayed and what the screen will change to.
                Intent intent = new Intent(FlightInformation.this, MainActivity.class);
                // Starts the new activity for the flight information screen to be displayed.
                startActivity(intent);
            }
        });

        // Start button sets the boolean value to true to allow the Google Maps camera to lock onto the users
        // location alon with drawing the route on the map if is hasn't been drawn out already.
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isStarted = true;
                drawRoute();
            }
        });

        // Stop button stops the Google Maps camera be locked onto the users location and allows the user
        // to move freely around the map.
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isStarted = false;
            }
        });
    }

    /**
     * This method takes in the flights date and time from the flights data and returns just the time
     * in a created format to get displayed to the user and used in the leaving time calculation process.
     *
     * @param flightDateAndTime Date and Time passed in by value after the flight data search.
     * @return The formatted time to be displayed and included in the leaving time calculations.
     */
    public String convertDate(String flightDateAndTime) {
        String formattedTime = "";

        // Try catch statement used to catch any parsing issues will the formatting process is being executed,
        // it will display an error message to the screen if an error were to occur
        try {
            // Sets the date format that the date and time format is currently in so it can take in the String value stored
            // in the flightDateAndTime variable, parse it and store it in the time variable as a Date object.
            DateFormat dateAndTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+ss:ss");
            Date time = dateAndTimeFormat.parse(flightDateAndTime);

            // Sets the new date format for the time that will be used, shows the hours, minutes and whether it's am or pm.
            DateFormat formatter = new SimpleDateFormat("HH:mm aa");
            formattedTime = formatter.format(time);

            // Catch statement used to display an error message if a parsing exception occurs.
        } catch (ParseException exception) {
            Toast.makeText(mainActivity, "Unable to Retrieve Flight Time", Toast.LENGTH_SHORT).show();
            exception.printStackTrace();
        }

        // Returns the formatted time.
        return formattedTime;
    }

    /**
     * Gets called when the map is ready to be used.
     *
     * @param googleMap Passes in the Google Maps object.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // if statement used to determine whether you have been granted a particular permission.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        }
        // Else used to Gets whether you should show UI with rationale for requesting a permission
        else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // We can shows the user a dialog why this permission is necessary
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_REQUEST_CODE);
            }
        }
    }

    /**
     * Draws the route from the users location to the destination on the maps display.
     */
    private void drawRoute() {
        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(mOrigin, mDestination);

        DownloadTask downloadTask = new DownloadTask();
        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
    }

    /**
     * Creates the URL used to get the directions between two points, the user location and
     * the relevant airport.
     *
     * @param origin The starting location.
     * @param dest The destination location.
     * @return A URL ready to be used to search for directions between two points.
     */
    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Sets the Google Maps key
        String key = "key=" + getString(R.string.google_maps_key);
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + key;
        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        // Returns the url
        return url;
    }

    /**
     * Sets the airports LatLng (location on the map) to the airports LatLng value that has been
     * passed in and displays it as a marker on the map.
     *
     * @param latLng Passes in the airports LatLng (location on the map) by value.
     */
    private void setAirportLocation(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng));
        mDestination = latLng;

        // Method call used to draw out the route on the map.
        drawRoute();
    }

    LocationCallback locationCallback = new LocationCallback() {
        /**
         * Called when device location information is available.
         *
         * @param locationResult Passes on the location of the user.
         */
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Log.d(TAG, "onLocationResult: " + locationResult.getLastLocation());

            // If statement used to check if there is a map available to use, if one is found a
            // method is called to set the users marker on the map by passing in the most recent location
            // of the user.
            if (mMap != null) {
                setUserLocationMarker(locationResult.getLastLocation());
            }
        }
    };

    /**
     * This method sets the users location marker on the map, it handles how the Google maps
     * camera settings and the marker options and settings.
     *
     * @param location Passes in the location of the user.
     */
    private void setUserLocationMarker(Location location) {
        // Creating and initialising the LatLng object that holds the user current location,
        // gets the latitude and longitude of the location.
        LatLng userLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        // If statement used to check if a marker for the user already exists, if not, one is created
        // and placed on the map.
        if (userLocationMarker == null) {
            // Creating and initialising the marker options object to allow changes and adjustments to be
            // made to the marker on the map.
            MarkerOptions markerOptions = new MarkerOptions();
            // Sets the position of the marker on the map using the users Lat
            markerOptions.position(userLocationLatLng);
            // Sets the icon for the marker to a small red car, to make it look more like the user is traveling
            // in a vehicle rather than the default blue dot on the map.
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.redcar));
            // Rotates the marker as the user turns.
            markerOptions.rotation(location.getBearing());
            // Places the icon of the car in the middle of the marker.
            markerOptions.anchor((float) 0.5, (float) 0.5);
            // Adds the marker onto map.
            userLocationMarker = mMap.addMarker(markerOptions);
            // Sets the height of the camera of the map when it's first shown, it zooms in the map
            // depending the on the integer value, the smaller the number, the more it zooms out.
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocationLatLng, 9));
            // Sets the origin value to the users LatLng
            mOrigin = userLocationLatLng;

        }
        // If a map already exists, then the markers position and bearing just gets updated instead of being
        // created and altered.
        else {
            // Uses the previously created marker.
            userLocationMarker.setPosition(userLocationLatLng);
            userLocationMarker.setRotation(location.getBearing());

            // If statement used to check if the isStarted is true to lock the camera view onto the users
            // location, if the user tries to move on the map it will automatically put the camera postition
            // back to the users location.
            if (isStarted) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocationLatLng, 17));
            }
        }
    }

    /**
     * Starts the location updates for the users location.
     */
    private void startLocationUpdates() {
        // Carries out a permissions check to see if the application can use the users location services.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // This is used for interacting with the location using fused location provider, the GPS on the device must be enabled
        // in order for this to work.
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * Stops the location updates for the users location.
     */
    private void stopLocationUpdates() {
        // Stops the location updates being passed through.
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    /**
     * This method is called when an activity becomes visible to the user and is called after onCreate.
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Carries out a permissions check to see if the application can use the users location services.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Method call done to start the process of getting the users current location.
            startLocationUpdates();
        }
        // Else statement used to display an error message to the screen if the permissions are denied.
        else {
            Toast.makeText(this, "Location Permissions Must Be Enabled", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * This method is called when the activity is no longer visible to the user.
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Method call to stop the
        stopLocationUpdates();
    }

    /**
     * A method to download json data from url
     */

    /**
     * This method downloads the json data from the url
     *
     * @param strUrl Passed in URL.
     * @return The data from the url.
     * @throws IOException Throws the exception to stop the application from crashing.
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception with url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /**
     * Fetches the data from the url that has been passed.
     */
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }


        /**
         * Executes in UI thread, after the parsing process.
         *
         * @param result
         */
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            String distance = "";
            String duration = "";

            if (result.size() < 1) {
                Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    if (j == 0) {    // Get distance from the list
                        distance = (String) point.get("distance");
                        continue;
                    } else if (j == 1) { // Get duration from the list
                        duration = (String) point.get("duration");
                        continue;
                    }

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.RED);
            }

            // Replaces all String characters with nothing, leaving just the numbers.
            duration = duration.replaceAll("\\D+", "");
            // Method call to calculate the leaving time, passing in the duration value.
            calculateLeavingTime(duration);
            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }

    /**
     * This method calculates the leaving time using the flight landing time and taking away the
     * duration time from the users location the airport and any time delays on the flight.
     *
     * @param duration Passes in the travel time between the users location and the airport.
     */
    public void calculateLeavingTime(String duration) {
        int finalDurationTime;
        int finalDelay;

        String delay;
        String flightLandingTime = flightLandingTimeTextView.getText().toString();
        String flightDelayTime = flightDelayTextView.getText().toString();

        Date date;
        DateFormat formatter;
        Calendar cal;

        // Parses the String value of duration to an integer value and storing the parsed
        // value into finalDurationTime variable
        finalDurationTime = Integer.parseInt(duration);

        // If statement used to check if there is any flight delay, if not the value for finalDelay is
        // set to zero.
        if (flightDelayTime.equals("No Delay")) {
            finalDelay = 0;
        }
        // Else if a delay time is found, all String characters are removed and replaced with nothing, leaving
        // the number values and then gets parsed into an integer value and stored into the finalDelay variable.
        else {
            delay = flightDelayTime.replaceAll("\\D+", "");
            finalDelay = Integer.parseInt(delay);
        }

        // Format for the time to leave set.
        formatter = new SimpleDateFormat("HH:mm aa");
        // Try catch statement used to catch any ParseExceptions that may occur, stops the application
        // from crashing if an error does occur.
        try {
            // The formatter is used to parse the flightLanding from a String into a date.
            date = formatter.parse(flightLandingTime);
            // A calender object is used to get the local time zone, the default on the device.
            cal = Calendar.getInstance();
            // Sets this calendar's time with the given date.
            cal.setTime(date);
            // Subtracts the duration of the journey along with the flight delay from the landing time and adding
            // the new time into the calendar.
            cal.add(Calendar.MINUTE, -finalDurationTime - finalDelay);
            // Parses the calendar time back into String format using the formatter.
            timeToLeave = formatter.format(cal.getTime());
            // Sets the time to leave as the text for the timeToLeaveTextView.
            timeToLeaveTextView.setText(timeToLeave);

            // Catch statement used to catch any errors and display an error message to the screen.
        } catch (ParseException parseException) {
            Toast.makeText(this, "Unable to get Leaving Time", Toast.LENGTH_SHORT).show();
        }
    }
}
