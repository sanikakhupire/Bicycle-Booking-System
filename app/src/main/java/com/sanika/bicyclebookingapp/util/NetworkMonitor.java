package com.sanika.bicyclebookingapp.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

/**
 * Production-Grade Network Connectivity Monitor & Offline State Engine.
 *
 * Tracks network capability changes in real time, manages offline states,
 * and notifies listeners when network connectivity is restored for automatic retries.
 */
public class NetworkMonitor {

    public interface OnNetworkReconnectedListener {
        void onNetworkReconnected();
    }

    private static volatile NetworkMonitor instance;
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isConnectedLiveData = new MutableLiveData<>(false);
    private final List<OnNetworkReconnectedListener> reconnectionListeners = new ArrayList<>();

    private boolean isCurrentlyConnected = false;

    private NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        initMonitoring();
    }

    public static NetworkMonitor getInstance(Context context) {
        if (instance == null) {
            synchronized (NetworkMonitor.class) {
                if (instance == null) {
                    instance = new NetworkMonitor(context);
                }
            }
        }
        return instance;
    }

    private void initMonitoring() {
        if (connectivityManager == null) {
            isConnectedLiveData.postValue(false);
            return;
        }

        // Initial check
        isCurrentlyConnected = checkCurrentConnectivity();
        isConnectedLiveData.postValue(isCurrentlyConnected);

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connectivityManager.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                boolean wasOffline = !isCurrentlyConnected;
                isCurrentlyConnected = true;
                isConnectedLiveData.postValue(true);
                SecureLog.d("NetworkMonitor", "Internet connectivity restored.");

                if (wasOffline) {
                    notifyReconnectionListeners();
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                isCurrentlyConnected = checkCurrentConnectivity();
                isConnectedLiveData.postValue(isCurrentlyConnected);
                if (!isCurrentlyConnected) {
                    SecureLog.w("NetworkMonitor", "Internet connection lost. Switching to offline cache mode.");
                }
            }
        });
    }

    private boolean checkCurrentConnectivity() {
        if (connectivityManager == null) return false;
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null &&
                (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
    }

    public boolean isConnected() {
        return isCurrentlyConnected;
    }

    public LiveData<Boolean> getIsConnectedLiveData() {
        return isConnectedLiveData;
    }

    public synchronized void addReconnectionListener(OnNetworkReconnectedListener listener) {
        if (listener != null && !reconnectionListeners.contains(listener)) {
            reconnectionListeners.add(listener);
        }
    }

    public synchronized void removeReconnectionListener(OnNetworkReconnectedListener listener) {
        if (listener != null) {
            reconnectionListeners.remove(listener);
        }
    }

    private synchronized void notifyReconnectionListeners() {
        for (OnNetworkReconnectedListener listener : reconnectionListeners) {
            try {
                listener.onNetworkReconnected();
            } catch (Exception e) {
                SecureLog.e("NetworkMonitor", "Error executing reconnection callback: " + e.getMessage());
            }
        }
    }
}
