package com.test.p2pwithgps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.net.wifi.WifiManager;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import android.util.Size;
import java.util.concurrent.Executors;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity extends AppCompatActivity {
    Button btnOnOff, btnDiscover, btnSend, btnDisconnect;
    ListView listView;
    ImageView videofeed;
    TextView GPSLocation, connectionStatus;
    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive, videoSendReceive, gpsSendReceive;
    FusedLocationProviderClient flclient;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private String GPScoordinates;
    private int TYPE_VIDEO = 1;
    private int TYPE_GPS = 2;


    //onCreate========================================================================================================================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        initializer(); //function to initialize all values
        getCurrentLocation();
        buttonfunctions(); //function to assign button functions
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    //onRequestPermissionsResult========================================================================================================================================================
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 6) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(MainActivity.this, "Permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //buttonfunctions========================================================================================================================================================
    private void buttonfunctions() {
        btnOnOff.setOnClickListener(view -> {
            Intent intent = new Intent(Settings.Panel.ACTION_WIFI); //request (intent) to show the Wi-Fi Settings Panel (Settings.Panel.ACTION_WIFI)
            startActivity(intent); //start it
        });
        btnDiscover.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES}, 6);
                return;
            }
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this,
                            "discoverPeers() SUCCESS",
                            Toast.LENGTH_LONG).show();
                    connectionStatus.setText("Discovery Started");
                }

                @SuppressLint("SetTextI18n")
                @Override
                public void onFailure(int reason) {
                    Toast.makeText(MainActivity.this,
                            "discoverPeers() FAILED: " + reason,
                            Toast.LENGTH_LONG).show();
                    connectionStatus.setText("Discovery Starting Failed");
                }
            });
        });
        listView.setOnItemClickListener((adapterView, view, i, l) -> {

            final WifiP2pDevice device = deviceArray[i];

            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES}, 6);
                return;
            }
            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onFailure(int reason) {
                    Toast.makeText(getApplicationContext(),
                            "Not Connected", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess() {
                    Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName, Toast.LENGTH_SHORT).show();
                    mManager.requestConnectionInfo(mChannel, connectionInfoListener);
                }
            });
        });
        btnSend.setOnClickListener(v -> {
            //String msg = writeMsg.getText().toString(); //set msg to message written in textbox
            if (GPScoordinates != null && !GPScoordinates.isEmpty()) {
                try {
                    byte[] data = GPScoordinates.getBytes();
                    sendReceive.dataOutputStream.writeInt(TYPE_GPS);
                    sendReceive.dataOutputStream.writeInt(data.length);
                    sendReceive.dataOutputStream.write(data);
                    sendReceive.dataOutputStream.flush();
                } catch (Exception e)
                {
                    e.printStackTrace();
                }

            } else {
                Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_SHORT).show();
            }
        });
        btnDisconnect.setOnClickListener(v->{
            if (serverClass != null) {
                try{
                    if (videoSendReceive != null && videoSendReceive.socket != null)
                    {
                        videoSendReceive.socket.close();
                    }
                    if(gpsSendReceive != null && gpsSendReceive.socket != null)
                    {
                        gpsSendReceive.socket.close();
                    }
                    if(serverClass != null)
                    {
                        if(serverClass.videoserverSocket != null)
                        {
                            serverClass.videoserverSocket.close();
                        }
                        if(serverClass.GPSserverSocket != null)
                        {
                            serverClass.GPSserverSocket.close();
                        }
                    }
                }catch(IOException e)
                {
                    e.printStackTrace();
                }

            }
            //disconnect the phones
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.NEARBY_WIFI_DEVICES)!= PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.NEARBY_WIFI_DEVICES}, 6);
                return;
            }
            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onFailure(int i) {
                    runOnUiThread(()->
                            Toast.makeText(MainActivity.this, "Disconnect didn't work because " + i, Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onSuccess() {
                    runOnUiThread(()->{
                    connectionStatus.setText("Disconnected");
                    Toast.makeText(MainActivity.this, "Successful DIsconnection", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    //initializevar========================================================================================================================================================
    private void initializer() {
        btnOnOff = findViewById(R.id.onOff);
        btnDiscover = findViewById(R.id.discover);
        btnSend = findViewById(R.id.sendButton);
        listView = findViewById(R.id.peerListView);
        GPSLocation = findViewById(R.id.readMsg);
        connectionStatus = findViewById(R.id.connectionStatus);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE); //class provides API for managing Wi-Fi Peer-toPeer connectivity
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WifiBR(mManager, mChannel, this);
        flclient = LocationServices.getFusedLocationProviderClient(this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        btnDisconnect = findViewById(R.id.disconnectbtn);
        videofeed = findViewById(R.id.VideoFeedScreen);
    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());

                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;

                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                listView.setAdapter(adapter);
            }
            if (peers.isEmpty()) {
                Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    };
    //ConnectionInfoListener: give info about each phone on whether they're a host / client, get IP add of server, and start server==================================
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            //display on phone if a group was formed and if they're the owner or client of the server
            Toast.makeText(MainActivity.this, "Formed: " + info.groupFormed + " | Owner: " + info.isGroupOwner, Toast.LENGTH_LONG).show();
            if (!info.groupFormed) { //if the group wasn't formed (false)
                Toast.makeText(MainActivity.this, "Group NOT formed", Toast.LENGTH_LONG).show(); //display message to show that a group wasn't formed
                return; //exit
            }
            if (info.isGroupOwner) { //if the phone is the host (true)
                Toast.makeText(MainActivity.this, "Starting Server", Toast.LENGTH_LONG).show(); //display message to show that server is starting up
                serverClass = new ServerClass(); //create instance of server class
                serverClass.start();// start server class thread
            } else { //if the phone is the client (false)
                Toast.makeText(MainActivity.this, "Starting Client", Toast.LENGTH_LONG).show(); //display message to show that client is starting up
                new Handler(Looper.getMainLooper()).postDelayed(()->{ //adding a delay to starting client class
                    clientClass = new ClientClass(info.groupOwnerAddress); //create instance of client class and get IP address of host
                    clientClass.start(); //start client class thread
                    startCamera();
                },2000); //wait 2 seconds to let host start up before client so that there isn't a connection issue
            }
        }
    };

    private void startCamera()
    {
        ListenableFuture<ProcessCameraProvider> cPF = ProcessCameraProvider.getInstance(this);
        cPF.addListener(()-> {
            try{
                ProcessCameraProvider cP = cPF.get();
                ImageAnalysis iA = new ImageAnalysis.Builder().setTargetResolution(new Size(320, 240)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                iA.setAnalyzer(Executors.newSingleThreadExecutor(), image -> {
                    Bitmap bitmap = imageProxyToBitmap(image);
                    if(sendReceive != null && bitmap != null)
                    {
                        sendFrame(bitmap);
                    }
                    image.close();
                });
                CameraSelector cS = CameraSelector.DEFAULT_BACK_CAMERA;
                cP.unbindAll();
                cP.bindToLifecycle(this, cS, iA);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private Bitmap imageProxyToBitmap(ImageProxy i)
    {
        try{
            ByteBuffer buffer = i.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }catch(Exception e)
        {
            return null;
        }
    }
    private void sendFrame(Bitmap bp)
    {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bp.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] frame = baos.toByteArray();
            sendReceive.dataOutputStream.writeInt(TYPE_VIDEO);
            sendReceive.dataOutputStream.writeInt(frame.length);
            sendReceive.dataOutputStream.write(frame);
            sendReceive.dataOutputStream.flush();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    //onResume========================================================================================================================================================
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter, Context.RECEIVER_EXPORTED); //Context.RECIEVER_EXPORTED needed so that app can receive broadcasts
    }

    //Pause========================================================================================================================================================
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    //SendReceive========================================================================================================================================================
    private class SendReceive extends Thread {
        private final Socket socket; //stores connected socket & it can't be reassigned
        private InputStream inputStream; //receive data
        private OutputStream outputStream; //send data
        private DataInputStream dataInputStream;
        private DataOutputStream dataOutputStream;
        private int messType;
        private int type;

        public SendReceive(Socket skt, int type) {
            socket = skt; //store socket
            this.type = type;
            try {
                inputStream = socket.getInputStream(); //get comm input channel from socket
                outputStream = socket.getOutputStream(); //get comm output channel from socket
                dataInputStream = new DataInputStream(socket.getInputStream());
                dataOutputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) //if fail for some reason
            {
                e.printStackTrace(); //display error
            }
        }

        @Override
        public void run() { //run when calling sendReceive.start()
            byte[] buffer = new byte[1024]; //1KB buffer to store incoming data
            int bytes; //store number of bytes read
            while (socket != null && socket.isConnected()) //continue to listen if socket is still connected to continuously receive messages
            {
                try {
                    int msgType = dataInputStream.readInt();
                    int length = dataInputStream.readInt();
                    byte[] data = new byte[length];
                    dataInputStream.readFully(data);
                    if(msgType == TYPE_VIDEO)
                    {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        runOnUiThread(()->videofeed.setImageBitmap(bitmap));
                    }
                    else if(msgType == TYPE_GPS)
                    {
                        String msg = new String(data);
                        runOnUiThread(()->GPSLocation.setText(msg));
                    }
                } catch (IOException e) { //if fail for some reason
                    e.printStackTrace(); //display error
                    break; //get out of loop
                }
            }
        }

        public void write(byte[] bytes) //send data to other device
        {
            new Thread(() -> { //create another thread to not block receive loop
                try {
                    if (socket != null && socket.isConnected() && !socket.isClosed()) //if socket is valid (something in socket, is connected, and isn't closed)
                    {
                        outputStream.write(bytes); //send the bytes
                        outputStream.flush(); //send the bytes now
                    }
                } catch (IOException e) //if it fails for some reason
                {
                    e.printStackTrace(); //display error
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection lost", Toast.LENGTH_SHORT).show());// show small message on sceen that connection is lost
                }
            }).start();
        }
    }
        // ServerClass========================================================================================================================================================
        public class ServerClass extends Thread {
            Socket videosocket;
            Socket gpssocket;
            ServerSocket videoserverSocket;
            ServerSocket GPSserverSocket;

            @Override
            public void run() {
                try {
                    videoserverSocket = new ServerSocket(6000);
                    GPSserverSocket = new ServerSocket(7000);
                    videosocket = videoserverSocket.accept();
                    gpssocket = GPSserverSocket.accept();
                    videoSendReceive = new SendReceive(videosocket, TYPE_VIDEO);
                    gpsSendReceive = new SendReceive(gpssocket, TYPE_GPS);
                    videoSendReceive.start();
                    gpsSendReceive.start();
                    runOnUiThread(() -> {
                        btnSend.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Socket Connected (Host)", Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //ClientClass========================================================================================================================================================
        public class ClientClass extends Thread {
            Socket videosocket;
            Socket gpssocket;
            String hostAdd;

            public ClientClass(InetAddress hostAddress) {
                hostAdd = hostAddress.getHostAddress();
                videosocket = new Socket();
                gpssocket = new Socket();
            }

            @Override
            public void run() {
                int attempts = 0;// # of attempts to connect
                while (attempts < 10) { //allow 10 attempts to join
                    try {
                        Thread.sleep(1000);//wait 1 second for each attempt
                        videosocket.connect(new InetSocketAddress(hostAdd, 6000), 3000);//connect to port 6000
                        gpssocket.connect(new InetSocketAddress(hostAdd, 7000), 3000);//connect to port 6000
                        videoSendReceive = new SendReceive(videosocket, TYPE_VIDEO);
                        gpsSendReceive = new SendReceive(gpssocket, TYPE_GPS);
                        videoSendReceive.start();
                        gpsSendReceive.start();
                        runOnUiThread(() -> {
                            btnSend.setEnabled(true);
                            Toast.makeText(MainActivity.this, "Socket Connected (Client)", Toast.LENGTH_SHORT).show();
                        });
                        return; // success → exit loop
                    } catch (Exception e) {
                        attempts++;
                    }
                }
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Client could not connect after retries", Toast.LENGTH_LONG).show());
            }
        }

        //function to get current location
        private void getCurrentLocation() {
            //checking location permission is granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //request permission if not granted
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
                return;
            }
            //fetch last known location
            flclient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        //get lat & long
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        //display Location in TextView
                        GPScoordinates = "Latitude: " + lat + "\nLongitude: " + lon;
                    } else {
                        //display error message if location is null
                        GPScoordinates = "Can't get location, it's null";
                    }
                }
            });
        }
    }