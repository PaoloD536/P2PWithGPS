package com.test.p2pwithgps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.nio.channels.Channel;

public class WifiBR extends BroadcastReceiver {
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mActivity;

    public WifiBR(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, MainActivity mActivity) {
        this.mManager = mManager;
        this.mChannel = mChannel;
        this.mActivity = mActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) //if Wi-Fi is enabled and notify appropriate action
        {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1); //get state of Wi-Fi
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED)//if Wi-Fi is enabled
            {
                Toast.makeText(mActivity, "Wifi is On", Toast.LENGTH_SHORT).show(); //display message Wi-Fi is On
            } else {
                Toast.makeText(mActivity, "Wifi is Off", Toast.LENGTH_SHORT).show(); //display message Wi-Fi is Off
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) //get list of peers
        {
            if (mManager != null) {
                if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES}, 6);
                    return;
                }
                mManager.requestPeers(mChannel, mActivity.peerListListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) //respond to new connection / disconnection
        {

            mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener);
        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) //Respond to device's Wi-Fi state changing
        {

        }
    }
}