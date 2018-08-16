package info.androidhive.firebase;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import info.androidhive.firebase.app.Config;
import info.androidhive.firebase.service.MyFirebaseInstanceIDService;
import info.androidhive.firebase.service.MyFirebaseMessagingService;
import info.androidhive.firebase.service.utils.NotificationUtils;

public class LocationShareActivity extends AppCompatActivity {

    private final static int PERMISSION_REQUEST = 1,RESUULT_PICK_CONTACT=0;
    private static final String TAG = LocationShareActivity.class.getSimpleName();

    private Button gpsButton;
    private TextView progressTitle;
    private ProgressBar progressBar;
    private TextView detailsText;
    private FirebaseAuth auth;

    private Button shareButton;
    private Button copyButton;
    private Button viewButton;
    private Button ShowListBtn;

    private LocationManager locManager;
    private Location lastLocation;

    private final LocationListener locListener = new LocationListener() {
        public void onLocationChanged(Location loc) {
            updateLocation(loc);
        }
        public void onProviderEnabled(String provider) {
            updateLocation();
        }
        public void onProviderDisabled(String provider) {
            updateLocation();
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    private static final String TAGG = MainActivity.class.getSimpleName();
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;


    //get current user
    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    ListView listViewUser;
    List<User> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_share);
      //  setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        // Display area
        gpsButton = (Button) findViewById(R.id.gpsButton);
        progressTitle = (TextView) findViewById(R.id.progressTitle);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        detailsText = (TextView) findViewById(R.id.detailsText);

        // Button area
        shareButton = (Button) findViewById(R.id.shareButton);
        copyButton = (Button) findViewById(R.id.copyButton);
        viewButton = (Button) findViewById(R.id.viewButton);
        ShowListBtn = (Button) findViewById(R.id.btn_back);

        // Set default values for preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        locManager = (LocationManager)getSystemService(LOCATION_SERVICE);


        mFirebaseInstance = FirebaseDatabase.getInstance();
        mFirebaseDatabase = mFirebaseInstance.getReference("users");
        listViewUser = (ListView) findViewById(R.id.listviewuser);
        //get firebase auth instance
        auth = FirebaseAuth.getInstance();
        userList = new ArrayList<>();
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // checking for type intent filter
                if (intent.getAction().equals(Config.REGISTRATION_COMPLETE)) {
                    // gcm successfully registered
                    // now subscribe to `global` topic to receive app wide notifications
                    FirebaseMessaging.getInstance().subscribeToTopic(Config.TOPIC_GLOBAL);

                    displayFirebaseRegId();

                } else if (intent.getAction().equals(Config.PUSH_NOTIFICATION)) {
                    // new push notification is received

                    String message = intent.getStringExtra("message");

                    Toast.makeText(getApplicationContext(), "Push notification: " + message, Toast.LENGTH_LONG).show();

                }
            }
        };

        listViewUser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                mFirebaseDatabase.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        userList.clear();
                        for (DataSnapshot usersnapshot : dataSnapshot.getChildren()){
                            User user = usersnapshot.getValue(User.class);
                            userList.add(user);
                        }
                        UserList adapter = new UserList(LocationShareActivity.this,userList);
                        listViewUser.setAdapter(adapter);
                        Log.d("list o","");
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("Error :",databaseError.getCode()+"");
                    }
                });
                LinearLayout parent = (LinearLayout) view;
                TextView test = (TextView) parent.findViewById(R.id.tokenid);
                String link = formatLocation(lastLocation, PreferenceManager.getDefaultSharedPreferences(LocationShareActivity.this).getString("prefLinkType", ""));
                Log.d("Tuch loc",link+"");
                new NetworkAsyncTask(test.getText().toString(),String.valueOf(link),"Share Location").execute();

            }
        });

        displayFirebaseRegId();
    }

    // Fetches reg id from shared preferences
    // and displays on the screen
    private void displayFirebaseRegId() {

        SharedPreferences pref = getApplicationContext().getSharedPreferences(Config.SHARED_PREF, 0);
        String regId = pref.getString("regId", null);
        Log.e(TAGG, "Firebase reg id: " + regId);

    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            locManager.removeUpdates(locListener);
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to stop listening for location updates", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register GCM registration complete receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.REGISTRATION_COMPLETE));

        // register new push message receiver
        // by doing this, the activity will be notified each time a new message arrives
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.PUSH_NOTIFICATION));

        // clear the notification area when the app is opened
        NotificationUtils.clearNotifications(getApplicationContext());
        startRequestingLocation();
        updateLocation();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    public void Show_List(View view) {

        mFirebaseDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                userList.clear();
                for (DataSnapshot usersnapshot : dataSnapshot.getChildren()){
                    User user = usersnapshot.getValue(User.class);
                    userList.add(user);
                }
                UserList adapter = new UserList(LocationShareActivity.this,userList);
                listViewUser.setAdapter(adapter);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("Error :",databaseError.getCode()+"");
            }
        });
    }


    static class NetworkAsyncTask extends AsyncTask<Void, Void, String> {

        private String to;
        private String body;
        private String title;

        public NetworkAsyncTask(String to, String body, String title) {

            this.to = to;
            this.body = body;
            this.title=title;
        }

        @Override
        protected String doInBackground(Void... params) {
                try {

                    final String apiKey = "AIzaSyCDzwccSB-uqC3-sQhiClmUJsUZKWydxM8";
                    URL url = new URL("https://fcm.googleapis.com/fcm/send");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "key=" + apiKey);
                    conn.setDoOutput(true);
                    JSONObject message = new JSONObject();
                    message.put("to", to);
                    message.put("priority", "high");

                    JSONObject notification = new JSONObject();
                    notification.put("title", title);
                    notification.put("body", body);
                    notification.put("click_action","info.androidhive.firebase_MapActivity");
                    message.put("data", notification);
                    message.put("notification",notification);
                    OutputStream os = conn.getOutputStream();
                    os.write(message.toString().getBytes());

                    os.flush();
                    os.close();

                    int responseCode = conn.getResponseCode();
                    System.out.println("\nSending 'POST' request to URL : " + url);
                    System.out.println("Post parameters : " + message.toString());
                    System.out.println("Response Code : " + responseCode);
                    System.out.println("Response Code : " + conn.getResponseMessage());

                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // print result
                    System.out.println(response.toString());
                    return response.toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "error";
            }
        }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST &&
                grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRequestingLocation();
        } else {
            Toast.makeText(getApplicationContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // ----------------------------------------------------
// UI
// ----------------------------------------------------
    private void updateLocation() {
        // Trigger a UI update without changing the location
        updateLocation(lastLocation);
    }

    private void updateLocation(Location location) {
        boolean locationEnabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean waitingForLocation = locationEnabled && !validLocation(location);
        boolean haveLocation = locationEnabled && !waitingForLocation;

        // Update display area
        gpsButton.setVisibility(locationEnabled ? View.GONE : View.VISIBLE);
        progressTitle.setVisibility(waitingForLocation ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(waitingForLocation ? View.VISIBLE : View.GONE);
        detailsText.setVisibility(haveLocation ? View.VISIBLE : View.GONE);

        // Update buttons
        shareButton.setEnabled(haveLocation);
        copyButton.setEnabled(haveLocation);
        viewButton.setEnabled(haveLocation);

        if (haveLocation) {
            String newline = System.getProperty("line.separator");
            detailsText.setText(String.format("%s: %s%s%s: %s%s%s: %s",
                    getString(R.string.accuracy), getAccuracy(location), newline,
                    getString(R.string.latitude), getLatitude(location), newline,
                    getString(R.string.longitude), getLongitude(location)));

            lastLocation = location;
            Log.d ("have ",lastLocation.toString () + ":");
        }
    }
//-----------------------------------------------------
// Menu related methods
//-----------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return  true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings:
                Log.d("Item",item+"");
                Log.d("Setting",R.id.action_settings+"");
                /*Intent intentSettingsActivity = new Intent(LocationShareActivity.this, SettingsActivity.class);
                this.startActivity(intentSettingsActivity);*/
                auth.signOut();
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    // ----------------------------------------------------
// Actions
// ----------------------------------------------------
    public void shareLocation(View view) {
        if (!validLocation(lastLocation)) {
            return;
        }

        String link = formatLocation(lastLocation, PreferenceManager.getDefaultSharedPreferences(this).getString("prefLinkType", ""));

        Intent intent = new Intent(("android.intent.action.MAIN"));
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, link);
        startActivity(Intent.createChooser(intent, getString(R.string.share_location_via)));

    }

    public void copyLocation(View view) {
        if (!validLocation(lastLocation)) {
            return;
        }

        String link = formatLocation(lastLocation, PreferenceManager.getDefaultSharedPreferences(this).getString
                ("prefLinkType", ""));
        ClipboardManager clipboard = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null){
            ClipData clip = ClipData.newPlainText(getString(R.string.app_name), link);
           //Log.d("LINK IS",link+"");
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(), R.string.copied, Toast.LENGTH_SHORT).show();
        }
        else {
            Log.e(TAG, "Failed to get the clipboard service");
            Toast.makeText(getApplicationContext(), R.string.clipboard_error, Toast.LENGTH_SHORT).show();
        }
    }

    public void viewLocation(View view) {
        if (!validLocation(lastLocation)) {
            return;
        }

        String uri = formatLocation(lastLocation, "geo:{0},{1}?q={0},{1}");
        String lat=uri.substring(4,12);
        String lng=uri.substring(13,20);
        Log.d("Lat",lat+"");
        Intent intent = new Intent(getBaseContext(),MapActivity.class);
        intent.putExtra("DataLat",lat);
        intent.putExtra("DataLng",lng);
        intent.putExtra("last",lastLocation);
        startActivity(intent);
    }

    public void openLocationSettings(View view) {
        if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }

    // ----------------------------------------------------
// Helper functions
// ----------------------------------------------------
    private void startRequestingLocation() {
        if (!locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST);
            Log.d ("Permission Granted",":");
            return;
        }

        // GPS enabled and have permission - start requesting location updates
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 0, locListener);
    }

    private boolean validLocation(Location location) {//37.42200 122.08400

        if (location == null) {
            return false;
        }
        Log.d ("Location :",location.toString ()+" ");
        // Location must be from less than 30 seconds ago to be considered valid
        if (Build.VERSION.SDK_INT < 17) {
            return System.currentTimeMillis() - location.getTime() < 40e3;
        } else {
            return SystemClock.elapsedRealtime() - location.getElapsedRealtimeNanos() < 40e9;
        }
    }

    private String getAccuracy(Location location) {
        float accuracy = location.getAccuracy();
        if (accuracy < 0.01) {
            return "?";
        } else if (accuracy > 99) {
            return "99+";
        } else {
            return String.format(Locale.US, "%2.0fm", accuracy);
        }
    }

    private String getLatitude(Location location) {
        return String.format(Locale.US, "%2.5f", location.getLatitude());
    }

    private String getLongitude(Location location) {
        return String.format(Locale.US,"%3.5f", location.getLongitude());
    }

    private String formatLocation(Location location, String format) {
        return MessageFormat.format(format,
                getLatitude(location), getLongitude(location));
    }

}