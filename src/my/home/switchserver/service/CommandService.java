package my.home.switchserver.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import my.home.switchserver.activity.MainActivity;
import my.home.switchserver.config.Config;
import my.home.switchserver.helper.CommandHelper;
import my.home.switchserver.model.Plug;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class CommandService extends Service {
	
	public static final String TAG = "CommandService";

	public static ConcurrentHashMap<String, Plug> ip2PlugHashMap = new ConcurrentHashMap<String, Plug>();
	
	private final LocalBinder subscribeBinder = new LocalBinder();
	private Socket socket;
	
	private Thread connectionThread;
	private Thread heartbeatThread;
	private Thread recvHeartbeatThread;
	private DatagramSocket cmdSocket;
	private boolean hasSendHeartbeat = false;
	private Object syncObject = new Object();
	
	private boolean stopRunning;
	
	private int socketTimeout = 5000;
	
	@Override
	public void onCreate() {
		super.onCreate();     
        Runnable connect = new ConnectionRunnable();
        connectionThread = new Thread(connect);
        connectionThread.start();
		
		try {
			cmdSocket = new DatagramSocket();
			cmdSocket.setSoTimeout(socketTimeout);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
        Runnable heartbeat = new HeartbeatRunnable();
        heartbeatThread = new Thread(heartbeat);
        heartbeatThread.start();
        RecvHeartbeatRunnable recvHeartbeat = new RecvHeartbeatRunnable();
        recvHeartbeatThread = new Thread(recvHeartbeat);
        recvHeartbeatThread.start();
        Log.d(TAG, "onCreate() executed");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand() executed");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		stopRunning = true;
		cmdSocket.close();
		super.onDestroy();
		Log.d(TAG, "onDestroy() executed");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return subscribeBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}
	
	public class LocalBinder extends Binder {  
        public CommandService getService() {  
            return CommandService.this;  
        }
    }
	
	public static String getMacFromIp(String ip) {
		if (ip2PlugHashMap.containsKey(ip)) {
			return ip2PlugHashMap.get(ip).mac;
		}else {
			return null;
		}
	}
	
	class HeartbeatRunnable implements Runnable {
		
        @Override
        public void run() {
        	
        	stopRunning = false;
            while(!stopRunning) {
        		try {
        			DatagramPacket packet = CommandHelper.getHeartbeatPacket();
        			synchronized (syncObject) {
        				syncObject.notifyAll();
    				}
        			if (packet != null) {
    					cmdSocket.send(packet);
        			}
        		}catch (SocketTimeoutException e) {
        			Log.w(TAG, "send heartbeat timeout.");
        			e.printStackTrace();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
        		
        		try {
					Thread.sleep(socketTimeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }
            Log.d(TAG, "HeartbeatRunnable stop......");
        }
    }
	
	class RecvHeartbeatRunnable implements Runnable {
		
        @Override
        public void run() {
        	
        	stopRunning = false;
            while(!stopRunning) {
        		try {
					synchronized (syncObject) {
						try {
							syncObject.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
        			DatagramPacket recvPacket = CommandHelper.recvResponsePacket(cmdSocket);
        			String recvString = CommandHelper.jnic.decode(recvPacket.getData(), recvPacket.getLength());
        			String[] recvConfirmStrings = recvString.split("%");
        			if (recvConfirmStrings.length != 0) {
        				String ipString = recvPacket.getAddress().getHostAddress();
        				String macString = recvConfirmStrings[1];
        				String stateString = recvConfirmStrings[3];
        				Log.i(TAG, ipString + "|" + macString + "|" + stateString);
        				
        				Plug plug = new Plug();
        				plug.ip = ipString;
        				plug.mac = macString;
        				plug.state = stateString;
        				CommandService.ip2PlugHashMap.put(ipString, plug);
					}
        		}catch (SocketTimeoutException e) {
        			Log.w(TAG, "heartbeat timeout.");
        			MainActivity.PlaceholderFragment.showMsg("heartbeat timeout.");
        			CommandService.ip2PlugHashMap.clear();
        		} catch (IOException e) {
        			e.printStackTrace();
        		} finally {
        		}
            }
            Log.d(TAG, "RecvHeartbeatRunnable stop......");
        }
    }
	
	class ConnectionRunnable implements Runnable {
		
        @Override
        public void run() {
        	Context msgContext = ZMQ.context(1);
        	socket = msgContext.socket(ZMQ.REP);
        	socket.bind("tcp://*:" + Config.BIND_PORT);
        	
        	Poller poller = new Poller(1);
        	poller.register(socket, Poller.POLLIN | Poller.POLLOUT);
        	
        	stopRunning = false;
            while(!stopRunning) {
            	try {
            		poller.poll(1000);
                	if (poller.pollin(0)) {
                		String recvString = socket.recvStr(ZMQ.NOBLOCK);
                    	Log.i(TAG, "recv: " + recvString);
                    	MainActivity.PlaceholderFragment.showMsg("recv: " + recvString);
                    	if (recvString == null) {
                    	 	socket.send("error");
        				}else {
        					String sendString = cmdHandler(recvString);
        					MainActivity.PlaceholderFragment.showMsg("send: " + sendString);
        					socket.send(sendString);
        				}
    				}
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
            }
            socket.close();
            Log.d(TAG, "ConnectionRunnable stop......");
        }
    }
	/*
	 * cmd type: open, close, list, check
	 * 
	 */
	private String cmdHandler(String jsonString) {
		String cmd = "";
		String target = "";
		try {
			JSONTokener jsonParser = new JSONTokener(jsonString);
			JSONObject cmdObject = (JSONObject) jsonParser.nextValue();
			cmd = cmdObject.getString("cmd");
			if (cmdObject.has("target")) {
				target = cmdObject.getString("target");
			}
		} catch (JSONException e) {
			return "{\"res\":\"error\"}";
		}
		if (cmd.equals("list")) {
			try {
				JSONObject repJSON = new JSONObject();
				JSONArray plugsJSON = new JSONArray();
				repJSON.put("switchs", plugsJSON);
				for (String ip : CommandService.ip2PlugHashMap.keySet()) {
					Plug plug = CommandService.ip2PlugHashMap.get(ip);
					JSONObject plugJSON = new JSONObject();
					plugJSON.put("ip", ip);
					plugJSON.put("mac", plug.mac);
					plugJSON.put("state", plug.state);
					plugsJSON.put(plugJSON);
				}
				return "{\"res\":" + repJSON.toString() + "}";
			} catch (JSONException e) {
				e.printStackTrace();
				return "{\"res\":\"error\"}";
			}
		}else {
			if (cmd.equals("open") || cmd.equals("close")) {
				
				return "{\"res\":\"" + CommandHelper.sendOperation(target, cmd) + "\"}";
			}else if (cmd.equals("check")) {
				return "{\"res\":\"" + CommandHelper.checkState(target) + "\"}";
			}
		}
		return "{\"res\":\"error\"}";
	}
}
