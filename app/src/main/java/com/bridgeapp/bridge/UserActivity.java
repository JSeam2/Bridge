package com.bridgeapp.bridge;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String TAG = "UserActivity";

    // For firebase
    private FirebaseAuth mFirebaseAuth = FirebaseAuth.getInstance();

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;


    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get local bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // if adapter is null then bluetooth is not supported
        if (mBluetoothAdapter == null){
            Toast.makeText(
                    getApplicationContext(),
                    "Bluetooth is not available on this device",
                    Toast.LENGTH_LONG).show();
        }


        if (savedInstanceState == null) {
            setTitle("Home");
            HomeFragment homeFragment = new HomeFragment();
            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().replace(
                    R.id.content_user,
                    homeFragment,
                    homeFragment.getTag()
            ).commit();

        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Name details, name and email are the same thing temporarily
        TextView myname = (TextView) findViewById(R.id.myname);
        TextView myemail = (TextView) findViewById(R.id.myemail);
        FirebaseUser user = mFirebaseAuth.getCurrentUser();
        //myname.setText(user.getDisplayName()); //Display name doesn't exist so this would give an error
        //myemail.setText(user.getEmail());

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
    * Options menu is currently transferred to HomeFragment
     **/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.user, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.bluetooth_connect) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                Toast.makeText(
                        getApplicationContext(),
                        "Select Connect to Device again to show BT dialog after enabling Bluetooth",
                        Toast.LENGTH_LONG).show();
            } else {
                startActivity(new Intent(getApplicationContext(), DeviceListActivity.class));
            }

        } else if (id == R.id.join_session){
            // Temporary
            Toast.makeText(
                    getApplicationContext(),
                    "Function not yet created",
                    Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home){
            // This should be the default view
            setTitle("Home");
            HomeFragment homeFragment = new HomeFragment();
            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().replace(
                    R.id.content_user,
                    homeFragment,
                    homeFragment.getTag()
            ).commit();
        }
        else if (id == R.id.nav_profile) {
            // Allow student to add profile picture
            // Add learning styles
            // Add MBTI
            setTitle("Profile");
            ProfileFragment profileFragment = new ProfileFragment();
            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().replace(
                    R.id.content_user,
                    profileFragment,
                    profileFragment.getTag()
            ).commit();
            //FragmentTransaction mFragmentTransaction = getSupportFragmentManager().beginTransaction();
            //mFragmentTransaction.replace(R.id.frame, , "home");

        } else if (id == R.id.nav_logout) {
            // log out user
            mFirebaseAuth.signOut();

            // close activity
            finish();

            // go back to login page
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


}
