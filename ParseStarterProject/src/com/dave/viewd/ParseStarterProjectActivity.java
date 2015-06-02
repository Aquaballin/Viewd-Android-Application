package com.dave.viewd;
/******************************************************************************
 * @author David Casey
 * This file is the main activity for Viewd.
 ******************************************************************************/
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.dave.viewd.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseImageView;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.dave.viewd.R;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;


public class ParseStarterProjectActivity extends Activity implements GoogleApiClient.OnConnectionFailedListener,
GoogleApiClient.ConnectionCallbacks {

    private ParseQueryAdapter<Post> queryAdapter;
    private ParseQueryAdapter.QueryFactory<Post> queryRequirements;
    private SeekBar seekBar;
    private Post post_object;
    private Bitmap image;
    private ParseFile picture_file;
    private int RANGE = 40;
    private TextView rangeTextView;
    private PullToRefreshListView pullToRefreshListView;
    double latitude, longitude;
    private static final String TAG = ParseStarterProjectActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();

        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        latitude = mLastLocation.getLatitude();
        longitude = mLastLocation.getLongitude();


        Toast.makeText(getApplicationContext(), "LOCATION UPDATE (0, 0) ... REFRESH LISTVIEW", Toast.LENGTH_LONG).show();

        setContentView(R.layout.activity_main);
        rangeTextView = (TextView) findViewById(R.id.textView2);



        queryRequirements = new ParseQueryAdapter.QueryFactory<Post>() {
                    @Override
                    public ParseQuery<Post> create() {
                        ParseQuery query = new ParseQuery("Post");
                        query.orderByDescending("createdAt");
                        query.whereWithinKilometers("Location",
                                new ParseGeoPoint(latitude,
                                        longitude), RANGE);
                        query.setLimit(75);
                        return query;
                    }
                };

        queryAdapter = new ParseQueryAdapter<Post>(this, queryRequirements) {
            @Override
            public View getItemView(final Post object, View v, ViewGroup parent) {
                if (v == null) {
                    v = View.inflate(getContext(), R.layout.post_item, null);
                }

                ParseImageView postImage = (ParseImageView) v.findViewById(R.id.icon);
                Button reportButton = (Button) v.findViewById(R.id.button2);
                reportButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int howManyReportedClicksAlready;
                        try {
                            howManyReportedClicksAlready = object.getTheNumberOfTimesReported();
                        } catch (NullPointerException e) {
                            howManyReportedClicksAlready = 0;
                        }
                        object.put("reported", howManyReportedClicksAlready + 1);
                        object.saveInBackground();
                        Toast.makeText(getBaseContext(),"image reported",
                                Toast.LENGTH_LONG).show();

                    }
                });

                ParseFile imageFile = object.getParseFile("Image");
                TextView tv = (TextView) v.findViewById(R.id.textView);
                DecimalFormat df = new DecimalFormat("##");
                tv.setText(formatTheDateString(object.getCreatedAt().toString())
                        + ", " + df.format(new ParseGeoPoint(latitude,longitude).distanceInMilesTo(object.getParseGeoPoint("Location")))
                + " miles away.");
                if (imageFile != null) {
                    postImage.setParseFile(imageFile);
                    postImage.loadInBackground();
                }
                return v;
            }
        };
        queryAdapter.setPaginationEnabled(true);
        queryAdapter.setTextKey("title");
        queryAdapter.setImageKey("Image");
        queryAdapter.loadObjects();
        pullToRefreshListView = (PullToRefreshListView) findViewById(R.id.list);
        pullToRefreshListView.setAdapter(queryAdapter);
        pullToRefreshListView.setOnRefreshListener( new PullToRefreshListView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                queryAdapter.loadObjects();
                pullToRefreshListView.onRefreshComplete();
            }
        });
        queryAdapter.loadObjects();


        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setProgress(0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                DecimalFormat dd = new DecimalFormat("##");
                RANGE = progress;
                if (progress == 0) {
                    rangeTextView.setText("NO LIMIT");
                } else {
                    rangeTextView.setText(String.valueOf(dd.format(RANGE * .62)) + " MILE RADIUS.");
                }

                queryAdapter.loadObjects();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }
    static final int REQUEST_IMAGE_CAPTURE = 1;

    /** METHOD TO TAKE PICTURE
     *
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /** METHOD TO DELIVER PICTURE RESULTS TO APPLICATION
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
     protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        byte[] image_byte_array;
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ParseGeoPoint parseGeoPoint;
            post_object = new Post();
            post_object.put("reported", 0);
            parseGeoPoint = new ParseGeoPoint(latitude, longitude);
            post_object.put("Location", parseGeoPoint);
            Bundle extras = data.getExtras();
            image = (Bitmap) extras.get("data");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
            image_byte_array = stream.toByteArray();
            picture_file = new ParseFile("Picture", image_byte_array);
            picture_file.saveInBackground();
            post_object.put("Image", picture_file);
            post_object.saveInBackground();
            }
        }

    /** This method formats Parse's military createdAt output so that
     * the dates and times are more easily readable.
     * @param string
     * @return The formatted string
     */
    private static String formatTheDateString(String string) {

        String return_string = ""; //string that will be returned
        String[] tokens = string.split("\\s+"); //initial getCreatedAt() string
        String[] military_date_split = tokens[3].split(":"); //split the military time

        if (Integer.parseInt(military_date_split[0]) == 00) {
            return_string = tokens[0] + " " + tokens[1] + " " + tokens[2] + " "
                    + 12 + ":" + military_date_split[1] + "AM"
                    + " " + tokens[4] + " " + tokens[5];
        } else if (Integer.parseInt(military_date_split[0]) == 24) {
            return_string = tokens[0] + " " + tokens[1] + " " + tokens[2] + " "
                    + 12 + ":" + military_date_split[1] + "AM"
                    + " " + tokens[4] + " " + tokens[5];
        } else if (Integer.parseInt(military_date_split[0]) == 12) {
            return_string = tokens[0] + " " + tokens[1] + " " + tokens[2] + " "
                    + 12 + ":" + military_date_split[1] + "PM"
                    + " " + tokens[4] + " " + tokens[5];
        } else if ((Integer.parseInt(military_date_split[0]) <= 12)) {//if its less than 12 its AM
            return_string = tokens[0] + " " + tokens[1] + " " + tokens[2] + " "
                    + military_date_split[0] + ":" + military_date_split[1] + "AM"
                    + " " + tokens[4] + " " + tokens[5];
            //Sat Mar 28
        } else if (Integer.parseInt(military_date_split[0]) > 12) { //if its >= 12 then its pm
            return_string = tokens[0] + " " + tokens[1] + " " + tokens[2] + " "
                    + (Integer.parseInt(military_date_split[0])%12) + ":" + military_date_split[1] + "PM"
                    + " " + tokens[4] + " " + tokens[5];
        }
        return return_string;
        // "Sat Mar 28 23:29:05 EDT 2015, 1 miles away";
    }

    /** BUILD THE GOOGLE CONNECTOR CLIENT
     *
     *
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }


    /** CHECK IF THE GOOGLE PLAY SERVICES ARE WORKING
     *
     * @return
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    /** ON START MAKE SURE THE CLIENT CONNECTS
     *
     */
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    /** WHEN YOU RESUME THE APP CHECK TO SEE IF SERVICES ARE STILL AVAILABLE
     *
     *
     */
    @Override
    protected void onResume() {
        super.onResume();

        checkPlayServices();
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    /** THIS METHOD DISPLAYS THE DATA ONCE CONNECTED TO GOOGLE PLAY
     *
     *
     * @param arg0
     */
    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        displayTheApplicationPictures();
    }

    /** THIS CONNECTS THE CLIENT ONCE IT IS SUSPENDED.
     *
     *
     * @param arg0
     */
    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }


    /**
     *
     *
     *
     */
    private void displayTheApplicationPictures() {
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();

        } else {
        }
    }
}


