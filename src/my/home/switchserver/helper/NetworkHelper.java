package my.home.switchserver.helper;

import android.R.bool;
import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings.Global;
import android.util.Log;


public class NetworkHelper {
	
	public static final String TAG = "NetworkHelper";
	
	private static WifiLock wifiLock;
	private static WakeLock  wakeLock;
	private static final Object lockObject = new Object();
	
	public static void initPlugsAddressAndMAC() {
		
	}
	
	public static boolean setNeverSleepPolicy(Context context){
	    ContentResolver cr = context.getContentResolver();
	    int set = android.provider.Settings.Global.WIFI_SLEEP_POLICY_NEVER;
	    Boolean didchangepolicy=android.provider.Settings.System.putInt(cr, android.provider.Settings.Global.WIFI_SLEEP_POLICY, set);
	    return didchangepolicy;
	}
	
	public static void keepWifiOn(Context context)
    {
        Log.i(TAG, "Taking wifi lock");
        if (wifiLock == null) {
        	synchronized (lockObject) {
        		if (wifiLock == null) {
	        		WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	        		wifiLock = manager.createWifiLock(WifiManager.WIFI_MODE_FULL, "my.home.SwitchServer.wifi");
//	        		wifiLock.setReferenceCounted(false);
                }
        	}
        }
        if (wakeLock == null) {
        	synchronized (lockObject) {
        		if (wakeLock == null) {
        			PowerManager pMgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        			wakeLock = pMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, "my.home.SwitchServer.wake");
//        			wakeLock.setReferenceCounted(false);
                }
        	}
        }
        wifiLock.acquire();
        wakeLock.acquire();
    }

	public static void cancelKeepWifiOn()
    {
    	Log.i(TAG, "Releasing wifi lock");
        if (wifiLock != null) {
        	synchronized (lockObject) {
        		if (wifiLock != null && wifiLock.isHeld()) {
    				wifiLock.release();
    				wifiLock = null;
            	}
        	}
        }
        if (wakeLock != null) {
        	synchronized (lockObject) {
        		if (wakeLock != null && wakeLock.isHeld()) {
        			wakeLock.release();
        			wakeLock = null;
            	}
        	}
        }
    }
}
