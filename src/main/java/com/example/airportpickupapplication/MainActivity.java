package com.example.airportpickupapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * This class contains the starting actions of the application, this is what is shown when the application
 * is loaded up. It carries out the search function for the flight and the functionality of the speech to text
 * feature.
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;
    private final int REQUEST_LOCATION_PERMISSION = 0;

    private String voiceInput;
    private String flightCode;
    private EditText flightCodeInput;

    private Button searchFlightButton;

    private FlightInfoService flightInfoService;

    private Context context;

    boolean isPermissionGranted;

    /**
     * This is where the activity is initialised and the content view is set to the flight searcher screen xml file. Here is
     * where the methods are called to start the application.
     *
     * @param savedInstanceState A reference to a Bundle object that is passed into the method.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sets the layout, this is what gets called to be displayed to the screen.
        setContentView(R.layout.flight_searcher_screen);
        // Method call to initialise all required objects used in the class.
        initialise();
        // Method call to start the main processes of the application.
        start();
    }

    /**
     * This method initialises all the required objects needed to call other classes and locate objects from the xml layout files.
     */
    public void initialise() {
        context = this;

        flightInfoService = new FlightInfoService(context);

        flightCodeInput = (EditText) findViewById(R.id.flightCodeInputEditText);

        searchFlightButton = (Button) findViewById(R.id.searchFlightButton);

        isPermissionGranted = false;
    }

    /**
     * This holds the button listener for the search flight code button, when pressed, the method call for
     * getting the flight code is called and then checked to see if it's valid and be used to search for the
     * desired flight.
     */
    public void start() {
        // Initialises the on click listener for the search flight button, making the button clickable.
        // Carries out the following actions when the button clicked.
        searchFlightButton.setOnClickListener(view -> {

            // Method call to check the location permissions.
            requestLocationPermission();

            // If statement used to check if isPermissionGranted is true, if true this means that the application has
            // access to the users locations service on their mobile device. When allowed access, the process of searching
            // for the flight code begins.
            if (isPermissionGranted) {
                // Calls the get flight code method and stores the returned value into the flightCode variable.
                flightCode = getFlightCode();

                // If statement used to check if the value stored in the flightCode variable is not empty.
                if (!flightCode.equals("") && !flightCode.equals(null)) {
                    // If there is a valid flight code stored in the flightCode variable, the searchFlightCode method is called passing through the flight code.
                    searchFlightCode(flightCode);
                }
                // Else, if the application does not have access to the location services, a message is displayed to
                // the screen telling the user to allow location permissions.
            } else {
                Toast.makeText(context, "Allow Location Permission", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * When called, the displayed is changed and moved onto the flight information screen.
     */
    public void displayManager() {
        // Sets what screen is currently being displayed and what the screen will change to.
        Intent intent = new Intent(MainActivity.this, FlightInformation.class);
        // Passes through the flightCode into the next class.
        intent.putExtra("flightCode", flightCode);
        // Starts the new activity for the flight information screen to be displayed.
        startActivity(intent);
    }

    /**
     * This method searchers for the flight using the flight code that the user has entered, the getFlightData method
     * from the FlightInfoService gets called and returns the desired flight. When a flight has been found, the displayManager
     * method gets called to proceed onto the FlightInformation screen. An error message gets displayed to the screen if an
     * error occurs during the flight search.
     *
     * @param flightCode This is the users input that gets passed in.
     */
    public void searchFlightCode(String flightCode) {

        // Sets the requiredData to Flight Code, being final means that it cannot be changed.
        final String requiredData = "Flight Code";

        // Calls the method getFlightData from the FlightInfoService class and passes in the flightCode that will be used to search,
        // the requiredData being what will be searched for and the VolleyResponseListener which is used for passing the data back
        // from the flight service class.
        flightInfoService.getFlightData(flightCode, requiredData, new FlightInfoService.VolleyResponseListener() {

            // Controls the response for errors that occur when searching for a flight.
            @Override
            public void onError(String message) {
                // Displays an error message to the screen if something wrong happens when searching for the flight.
                Toast.makeText(MainActivity.this, "You Must be Connected to the Internet.", Toast.LENGTH_LONG).show();
            }

            @Override
            // Controls what gets responded back when a String value has been passed through.
            public void onResponse(String searchedData) {
                // Calls the displayManager to move onto the flight information screen.
                displayManager();
            }
        });
    }

    /**
     * Gets the input from the user when text has been entered into the edit text flightCodeInput from the xml layout file.
     * The input gets validated and returned.
     *
     * @return The validated input for the flightCode.
     */
    public String getFlightCode() {
        // Local variable created and initialised to be used to store, validate and return the users input.
        String userInput = "";

        // Try catch used to ensure that the application doesn't crash when an error occurs.
        try {
            // Gets the input from the edit text flightCodeInput, converts it to a String and stores the value in the userInput variable.
            userInput = flightCodeInput.getText().toString();
            // Catches any null pointer errors and displays a message to the screen if a flight code has not been entered.
        } catch (NullPointerException exception) {
            Toast.makeText(MainActivity.this, "A Flight Code Must Be Entered", Toast.LENGTH_SHORT).show();
            // Displays what the exception is in the console, used for testing and debugging.
            exception.printStackTrace();
            // Catches any additional errors that may occur and displays an error message to the screen.
        } catch (Exception exception) {
            Toast.makeText(MainActivity.this, "An Error Has Occurred", Toast.LENGTH_SHORT).show();
            // Displays what the exception is in the console, used for testing and debugging.
            exception.printStackTrace();
        }

        // Validates the inputted flight code from the user and returns it.
        return validateFlightCode(userInput);
    }

    /**
     * This checks to see if the flight code that has been entered is valid and can be passed onto the searcher, it ensures that
     * the users input has not been left and displays an error message to the screen if so.
     *
     * @param userInput The flight code that has been entered by the user and is passed through.
     * @return The flight code that the user has entered, either a valid value or set to blank for further validation at a later
     * stage in the search process.
     */
    public String validateFlightCode(String userInput) {
        // If else statement used to check if the user has not entered anything, if so an error message is displayed to the screen,
        if (userInput.equals("")) {
            // Displays an error message to the screen informing the user that a flight code must be entered in order to proceed.
            Toast.makeText(MainActivity.this, "A Flight Code Must Be Entered", Toast.LENGTH_SHORT).show();
            // Returns a black string to ensure that the later validation in the search does not run into any issues.
            return "";
            // If the user input is not blank then the value gets returned.
        } else {
            return userInput;
        }
    }

    /**
     * This onClick method gets called when the speech to text button is clicked on the application.
     *
     * @param view Passes in the interface that will be used when the onClick command is started.
     */
    public void onClick(View view) {
        //Creates an Intent with with action recognize speech as the action, allowing the device to take a users voice.
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        // Try catch used to catch any errors that may occur and display them, also ensures that the application.
        // does not crash if an error were to occur.
        try {
            // Starts the activity and waits for a response
            startActivityForResult(intent, REQUEST_CODE);
            // Catches any exceptions and displays them in the command line
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method handles the result returned from the voice input from the user which gets processes
     * and sent to be displayed on to the screen to then be used for searching for a flight.
     *
     * @param requestCode An integer value that never gets changed that gets used to carryout the case statement,
     *                    gets passed in by value.
     * @param resultCode  An integer value passed in to be used to check in the if statement ti ensure that the results
     *                    are successful.
     * @param data        An intent is an abstract description of an operation to be performed passed in.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If statement used ensure that the code gets run as the request code never gets changed so it will guarantee to run
        // every time to voice input is activated.
        if (requestCode == REQUEST_CODE) {// If statement used to check that result code is result is ok and is successful to carryout the activity and also to check
            // that the intent of data is not holding a null value.
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                // Gets the input from the array list and stores it the voiceInput variable as a String value, as it
                // stores the value it makes sure that all characters are upper case. This doesn't make a different for searching,
                // it's only done to make it look nicer when it gets displayed on the screen.
                voiceInput = result.get(0).toUpperCase();
                // Replaces all spaces in the input and replaces them with nothing, this needs to be done as there is not spaces
                // in any of the api codes, ensure that they can actually be used to search for a flight.
                voiceInput = voiceInput.replaceAll("\\s+", "");
                // Sets the text value of the textView to the String value stored inside the voiceInput variable.
                flightCodeInput.setText(voiceInput);
            }
            // Break used to get out of the case statement.
        }
    }

    /**
     * Callback for the result from requesting permissions.
     *
     * @param requestCode  An integer value that never gets changed that gets used to carryout the case statement,
     *                     gets passed in by value.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Prompts the user to allow the application access the location services, displays an error message insisting
     * the user to allow access if denied.
     */
    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};

        // If statement used to ask the user for location permission and set the is permission granted boolean
        // to true to allow the application search for the desired flight when the user presses the search button.
        if (EasyPermissions.hasPermissions(this, perms)) {
            isPermissionGranted = true;
            // If the user denies permissions, an error message is displayed to the screen and the user is prompted to allow it again. If the user
            // denies it once more, they are still shown an error message but now have to go into the settings on their device and allow the
            // permissions that way instead to get the application to work correctly.
        } else {
            EasyPermissions.requestPermissions(this, "Please Grant the Location Permission", REQUEST_LOCATION_PERMISSION, perms);
        }
    }
}