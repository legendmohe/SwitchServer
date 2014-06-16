package my.home.switchserver.activity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import my.home.switchserver.R;
import my.home.switchserver.config.Config;
import my.home.switchserver.helper.NetworkHelper;
import my.home.switchserver.service.CommandService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {
	
	public static boolean STOPPED = false;
	
	private CommandService commandService;
    private ServiceConnection connection = new ServiceConnection() {  
		  
        @Override  
        public void onServiceDisconnected(ComponentName name) {  
        }  
  
        @Override  
        public void onServiceConnected(ComponentName name, IBinder service) {
        	commandService = ((CommandService.LocalBinder) service).getService();
        }  
    };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		setupService();
		NetworkHelper.keepWifiOn(this);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		this.unbindService(connection);
	}
	
	private void setupService() {
    	startService(new Intent(this, CommandService.class));
    	Intent bindIntent = new Intent(this, CommandService.class);  
    	this.bindService(bindIntent, connection, Context.BIND_AUTO_CREATE);
    	STOPPED = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent intent = new Intent();
	    	intent.setClass(MainActivity.this, SettingsActivity.class);
	    	startActivityForResult(intent, 0); 
	    	return true;
		}
		if (id == R.id.action_exit) {
			stopService(new Intent(this, CommandService.class));
			commandService.stopCommandService();
			this.finish();
			STOPPED = true;
	    	return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	loadPref();
    }
	
	private void loadPref(){
    	SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    	Config.BIND_PORT = Integer.valueOf(mySharedPreferences.getString("settings_bind_port", "8004"));
    	Config.PLUG_PORT = Integer.valueOf(mySharedPreferences.getString("settings_plug_port", "27431"));
    	Config.BROADCAST_ADDRESS = mySharedPreferences.getString("settings_plug_port", "192.168.1.255");
    }

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		
		public static Handler handler;
		private TextView msgTextView;
		private ScrollView msgScrollView;
		
		LinkedList<String> msgQueue = new LinkedList<String>();
		
		public PlaceholderFragment() {
			handler = new Handler(){
				@Override 
	            public void handleMessage(Message msg) { 
	                super.handleMessage(msg); 
	                String msgString = (String)msg.obj;
	                String dateString = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date(System.currentTimeMillis()));
	                msgString = dateString + ":\n" + "	> " + msgString;
	                
	                msgQueue.addLast(msgString);
	                while (msgQueue.size() > 100) {
						msgQueue.removeFirst();
					}
	                StringBuffer bf = new StringBuffer();
	                for (String value: msgQueue) {
						bf.append(value + "\n");
					}
	                msgTextView.setText(bf.toString());
	                msgScrollView.post(new Runnable()
	                {
	                    public void run()
	                    {
	                    	msgScrollView.fullScroll(View.FOCUS_DOWN);
	                    }
	                });
	            } 
			};
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			
			msgTextView = (TextView) rootView.findViewById(R.id.msg_textview);
			msgScrollView = (ScrollView) rootView.findViewById(R.id.msg_scrollview);
			return rootView;
		}
		
		public static void showMsg(String msg) {
			Message msgMessage = new Message();
			msgMessage.obj = msg;
			MainActivity.PlaceholderFragment.handler.sendMessage(msgMessage);
		}
	}

}
