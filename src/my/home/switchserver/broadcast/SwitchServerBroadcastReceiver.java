package my.home.switchserver.broadcast;

import my.home.switchserver.activity.MainActivity;
import my.home.switchserver.service.CommandService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SwitchServerBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		boolean isServiceRunning = false;
		if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
			ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			for (RunningServiceInfo service : manager
					.getRunningServices(Integer.MAX_VALUE)) {
				if ("my.home.switchserver.service.CommandService".equals(service.service.getClassName())){
					isServiceRunning = true;
					break;
				}
			}
			if (!isServiceRunning && !MainActivity.STOPPED) {
				Intent i = new Intent(context, CommandService.class);
				context.startService(i);
			}
		}
	}

}
