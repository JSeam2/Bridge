package com.bridgeapp.bridge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    TextView myLabel;
    TextView myTextBox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream  mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    // For plotting
    // typical values to catch prefix, do some string manipulation append values to an array for plotting
    // 11.00 C;
    // 363 H;
    // 673 G;
    private Runnable mTimerG; // for gsr
    private Runnable mTimerC; // for temperature
    private Runnable mTimerH; // For heart rate variability

    private LineGraphSeries<DataPoint> mSeriesG;
    private LineGraphSeries<DataPoint> mSeriesC;
    private LineGraphSeries<DataPoint> mSeriesH;

    private double lastXvalue = 32d;


    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null){
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "No bluetooth adapter available", Toast.LENGTH_LONG).show();
            activity.finish();
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // layout definitions


        View view = inflater.inflate(R.layout.fragment_home, container, false);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        Button closeButton = (Button) view.findViewById(R.id.close);
        myLabel = (TextView) view.findViewById(R.id.label);
        myTextBox = (TextView) view.findViewById(R.id.entry);

        // Graphs
        GraphView graphG = (GraphView) view.findViewById(R.id.GSRgraph);
        mSeriesG = new LineGraphSeries<>(generateData());
        graphG.addSeries(mSeriesG);
        graphG.getViewport().setXAxisBoundsManual(true);
        graphG.getViewport().setMinX(0);
        graphG.getViewport().setMaxX(40);
        GridLabelRenderer glrG = graphG.getGridLabelRenderer();
        glrG.setPadding(45);


        GraphView graphC = (GraphView) view.findViewById(R.id.temperatureGraph);
        mSeriesC = new LineGraphSeries<>(generateData());
        graphC.addSeries(mSeriesC);
        graphC.getViewport().setXAxisBoundsManual(true);
        graphC.getViewport().setMinX(0);
        graphC.getViewport().setMaxX(40);
        GridLabelRenderer glrC = graphC.getGridLabelRenderer();
        glrC.setPadding(45);

        GraphView graphH = (GraphView) view.findViewById(R.id.HRGraph);
        mSeriesH = new LineGraphSeries<>(generateData());
        graphH.addSeries(mSeriesH);
        graphH.getViewport().setXAxisBoundsManual(true);
        graphH.getViewport().setMinX(0);
        graphH.getViewport().setMaxX(40);
        GridLabelRenderer glrH = graphH.getGridLabelRenderer();
        glrH.setPadding(45);



        closeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //Toast.makeText(getContext(),"hi",Toast.LENGTH_LONG).show();
                try {
                    closeBT(); // this is pretty hacky and buggy atm no time to polish up
                }
                catch(IOException ex) {
                    // leave this empty
                }
            }
        });
    }

    @Override
    public void onStart(){
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 0);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

    }

    @Override
    public void onResume() {
        super.onResume();


    }



    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.user, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bluetooth_connect2:{
                try{
                    findBT();
                    openBT();
                }
                catch (IOException ex) {
                    // leave this empty
                }
                return true;
            }
//            case R.id.secure_connect_scan: {
//                // Launch the DeviceListActivity to see devices and do scan
//                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
//                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
//                return true;
//            }
//            case R.id.insecure_connect_scan: {
//                // Launch the DeviceListActivity to see devices and do scan
//                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
//                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
//                return true;
//            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    /**
     * BT supporting functions
     * This is pretty hacky, it is probably useful only for 1 device ie. HC-05 only
     * Anything more elaborate we need to probably recode this
     * But due to the lack of time this will do
     */

    void findBT(){
        // This won't allow you to refresh. Need to restart app
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            //myLabel.setText("No bluetooth adapter available");
            Toast.makeText(getContext(), "No bluetooth adapter available", Toast.LENGTH_LONG).show();
        }

        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        // look for HC-05
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() >0 ){
            Toast.makeText(getContext(), "Looking for HC-05", Toast.LENGTH_LONG).show();
            for(BluetoothDevice device: pairedDevices){
                if (device.getName().equals("HC-05")){
                    Toast.makeText(getContext(), "Found HC-05", Toast.LENGTH_LONG).show();
                    mmDevice = device;
                    break;
                }
            }
        }
        //myLabel.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //STD SERIAL PORT ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();
        Toast.makeText(getContext(), "Bluetooth Open", Toast.LENGTH_LONG).show();
        //myLabel.setText("Bluetooth Opened");

    }

    void beginListenForData(){
        final Handler handler = new Handler();
        final byte delimiter = 10;

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable(){
           public void run(){
               while(!Thread.currentThread().isInterrupted() && !stopWorker){
                   try{
                       int bytesAvailable = mmInputStream.available();
                       if(bytesAvailable > 0){
                           byte[] packetBytes = new byte[bytesAvailable];
                           mmInputStream.read(packetBytes);
                           for(int i = 0; i <bytesAvailable; i++){
                               byte b = packetBytes[i];

                               if (b == delimiter){
                                   byte[] encodedBytes = new byte[readBufferPosition];
                                   System.arraycopy(
                                           readBuffer, 0,
                                           encodedBytes, 0,
                                           encodedBytes.length);
                                   final String data = new String(encodedBytes, "US-ASCII");

                                   readBufferPosition = 0;

                                   handler.post(new Runnable(){
                                       public void run(){
                                           lastXvalue += 1d;
                                           myTextBox.setText(data); //the gems goes here

                                           //Log.d(TAG, data.substring(data.length()-3,data.length()-2));
                                           double y = 0; // this is hacky, probably need to think of a better way
                                           String substr = data.substring(data.length()-3,data.length()-2);
                                           if (substr.contains("G")){
                                               // append to GSR
                                               //Log.d(TAG, ""+y);

                                               try{
                                                   y = Double.parseDouble(data.substring(0,3));
                                               } catch (NumberFormatException nex) {}


                                               mSeriesG.appendData(
                                                       new DataPoint(lastXvalue, y),true,40);

                                           } else if (substr.contains("C")){
                                               // append to temperature

                                               try{
                                                   y = Double.parseDouble(data.substring(0,4));
                                               } catch (NumberFormatException nex) {}


                                               mSeriesC.appendData(
                                                       new DataPoint(lastXvalue, y),true,40);

                                           } else if (substr.contains("H")){
                                               // append to Heart rate
                                               try {
                                                   y = Double.parseDouble(data.substring(0,3));
                                               } catch (NumberFormatException nex){}


                                               mSeriesH.appendData(
                                                       new DataPoint(lastXvalue, y),true,40);
                                           }

                                           handler.postDelayed(this, 2000);
                                       }
                                   });
                               } else {
                                   readBuffer[readBufferPosition++] = b;
                               }
                           }
                       }
                   } catch (IOException ex) {
                       stopWorker = true;
                   }
               }
           }
        });

        workerThread.start();
    }


    void closeBT() throws IOException{
        stopWorker = true;
        //mmOutputStream.close();
        //mmInputStream.close();
        //mmSocket.close();
        Toast.makeText(getContext(), "Bluetooth Closed", Toast.LENGTH_LONG).show();
        //myLabel.setText("Bluetooth Closed");
    }


    // initialize with 0
    private DataPoint[] generateData() {
        int count = 30;
        DataPoint[] values = new DataPoint[count];
        for (int i = 0; i< count; i++){
            double x = i;
            double y = 0;
            DataPoint v = new DataPoint(x,y);
            values[i] = v;
        }
        return values;
    }
}
