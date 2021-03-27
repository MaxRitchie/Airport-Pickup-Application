package com.example.airportpickupapplication;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import models.FlightDataModel;

public class FlightInfoService {

    // This is the query that is used to search for the flight using the given access key.
    private static final String QUERY_FOR_FLIGHT_CODE = "https://api.aviationstack.com/v1/flights?access_key=YOUR_API_KEY_GOES_HERE&limit=1&flight_iata=";

    private Context context;

    private String flightInfo;

    /**
     * This is the constructor method for the class which is used to set the value
     * of the context variable that will be get passed through when a new object is
     * created for the class.
     *
     * @param context Passes in the context of the object that will be used.
     */
    public FlightInfoService(Context context) {
        this.context = context;
    }

    /**
     * Gets a response back after the search action has been completed and returned either
     * data to be return or an error if something went wrong.
     */
    public interface VolleyResponseListener {
        void onError(String message);

        void onResponse(String searchedData);
    }

    /**
     * This method gets all the relevant flight data that is called for and returns each
     * piece of data one by one when a specific required data value has been passed through.
     *
     * @param flightCode Passes in the flight code that will be used to search.
     * @param requiredData Passes in the data that will be pulled from the api from the searched
     *                     data from the found flight.
     * @param volleyResponseListener Passes in the response listener to be used to return a value
     *                               back, whether the search was successful or not.
     */
    public void getFlightData(String flightCode, String requiredData, VolleyResponseListener volleyResponseListener) {
        String url = QUERY_FOR_FLIGHT_CODE + flightCode;

        // A request for retrieving a JSONObject response body at a given URL, allowing for an optional JSONObject to be passed in as part of the request body.
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                // Try catch used to catch any errors that may occur and display an error message to the screen stating what went wrong
                // instead of the application crashing.
                try {
                    Gson gson = new Gson();
                    // Flight data on response is contained within an array called data.
                    JSONArray data = response.getJSONArray("data");

                    // If else statement used to check is a flight has been returned from being search, if nothing has been returned,
                    // an error message is displayed to screen stating that a valid flight code must be entered in order to process a search.
                    if (data.length() == 0) {
                        Toast.makeText(context, "A Valid Flight Code Must Be Entered", Toast.LENGTH_SHORT).show();
                        // If the flight code is valid, the process of searching for the flight data is continued.
                    } else {
                        // Serialise the data from the first element in the array (should never be more).
                        FlightDataModel flightData = gson.fromJson(data.getJSONObject(0).toString(), FlightDataModel.class);

                        // If statement used to get the flight code and store it in the flight info variable to be then be returned.
                        if (requiredData.equals("Flight Code")) {
                            flightInfo = flightData.flight.iata;
                        }

                        // If statement used to get the flight landing time and store it in the flight info variable to be then be returned.
                        if (requiredData.equals("Flight Landing Time")) {
                            flightInfo = flightData.arrival.scheduled;
                        }

                        // If statement used to get the flight status code and store it in the flight info variable to be then be returned.
                        if (requiredData.equals("Flight Status")) {
                            flightInfo = flightData.flight_status;
                        }

                        // If statement used to get the flight delay and store it in the flight info variable to be then be returned.
                        if (requiredData.equals("Flight Delay")) {
                            flightInfo = flightData.arrival.delay;
                        }

                        // If statement used to get the airport that the flight is arriving at and store it in the flight info variable to be then be returned.
                        if (requiredData.equals("Airport")) {
                            flightInfo = flightData.arrival.airport;
                        }

                        // Calls the volley response to pass out the data stored in the flightInfo variable.
                        volleyResponseListener.onResponse(flightInfo);
                    }
                    // Catch used to catch any exception that may occur and display an error message to the screen, preventing
                    // the application from crashing.
                } catch (JSONException exception) {
                    Toast.makeText(context, "Error Accessing Flight Information", Toast.LENGTH_SHORT).show();
                } catch (NullPointerException exception) {
                }
            }
        }, new Response.ErrorListener() {
            @Override
            /**
             * Sends back an error with the relevant message if something goes wrong with finding
             * and returning flight data.
             */
            public void onErrorResponse(VolleyError error) {
                volleyResponseListener.onError("Error Gaining Flight Information");
            }
        });

        // Calls a method in the APIConnector class in order to communicate and send requests the other
        // classes in the project, passing through the JSON object.
        APIConnector.getInstance(context).addToRequestQueue(request);
    }
}
