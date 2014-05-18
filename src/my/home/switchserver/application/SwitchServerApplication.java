package my.home.switchserver.application;

import my.home.switchserver.broadcast.SwitchServerBroadcastReceiver;
import android.app.Application;
import android.content.Intent;
import android.content.IntentFilter;

public class SwitchServerApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK); 
	    SwitchServerBroadcastReceiver receiver = new SwitchServerBroadcastReceiver(); 
	    registerReceiver(receiver, filter); 
	}
}
