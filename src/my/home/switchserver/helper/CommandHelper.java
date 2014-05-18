package my.home.switchserver.helper;

import hangzhou.kankun.WifiJniC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import my.home.switchserver.config.Config;
import my.home.switchserver.model.Plug;
import my.home.switchserver.service.CommandService;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class CommandHelper {
	
	private static String TAG = "CommandHelper";
	
	public static WifiJniC jnic = new WifiJniC();
	
	public static String sendOpen(String ipString) {
		return sendOperation(ipString, "open");
	}
	
	public static String sendClose(String ipString) {
		return sendOperation(ipString, "close");
	}
	
	public static String checkState(String ipString) {
		DatagramSocket cmdSocket = null;
		try {
			cmdSocket = new DatagramSocket();
			cmdSocket.setSoTimeout(2000);
			
			DatagramPacket packet = getCheckStatePacket(ipString);
			if (packet == null) {
				return "error";
			}
			cmdSocket.send(packet);
			
			String recvString = recvResponse(cmdSocket);
			String[] recvConfirmStrings = recvString.split("%");
			return recvConfirmStrings[3];
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			cmdSocket.close();
		}
		return "error";
	}
	
	public static String sendOperation(String ipString, String op) {
		DatagramSocket cmdSocket = null;
		try {
			for (int i = 0; i < 5; i++) {
				cmdSocket = new DatagramSocket();
				cmdSocket.setSoTimeout(2000);
				DatagramPacket packet = getOpPacket(op, ipString);
				if (packet == null) {
					return "error";
				}
				cmdSocket.send(packet);
				
				String recvString = recvResponse(cmdSocket);
				String[] confirmNumStrings = recvString.split("%");
				String confirmNum = confirmNumStrings[3].split("#")[1];
				
				DatagramPacket confirmPacket = getConfirmPacket(ipString, confirmNum);
				cmdSocket.send(confirmPacket);
				
				String confirmString = recvResponse(cmdSocket);
				String[] confirmStrings = confirmString.split("%");
				if (confirmStrings[3].equals(op)) {
					return op;
				}else {
					try {
						Thread.sleep(200L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		} finally {
			cmdSocket.close();
		}
		return "error";
	}
	
	public static DatagramPacket getOpPacket(String op, String ipString) throws UnknownHostException {
		String mac = CommandService.getMacFromIp(ipString);
		if (TextUtils.isEmpty(mac)) {
			return null;
		}
		String cmd = "lan_phone%" + mac + "%nopassword%" + op + "%request";
		return getPacket(ipString, cmd);
	}
	
	public static DatagramPacket getConfirmPacket(String ipString, String confirmNum) throws UnknownHostException {
		String mac = CommandService.getMacFromIp(ipString);
		if (TextUtils.isEmpty(mac)) {
			return null;
		}
		String cmd = "lan_phone%" + mac + "%nopassword%" + "confirm#" + confirmNum + "%request";
		return getPacket(ipString, cmd);
	}
	
	public static DatagramPacket getCheckStatePacket(String ipString) throws UnknownHostException {
		String mac = CommandService.getMacFromIp(ipString);
		if (TextUtils.isEmpty(mac)) {
			return null;
		}
		String cmd = "lan_phone%" + mac + "%nopassword%check%request";
		return getPacket(ipString, cmd);
	}
	
	public static DatagramPacket getHeartbeatPacket() throws UnknownHostException {
		String dateString = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date(System.currentTimeMillis()));
		String cmd = "lan_phone%mac%nopassword%" + dateString + "%heart";
		InetAddress server_ip = InetAddress.getByName(Config.BROADCAST_ADDRESS);
		byte[] confirmByte = jnic.encode(cmd, cmd.length());
		DatagramPacket packet = new DatagramPacket(confirmByte, confirmByte.length, server_ip, Config.PLUG_PORT);
		return packet;
	}
	
	public static DatagramPacket getPacket(String ipString, String dataString) throws UnknownHostException {
		InetAddress server_ip = InetAddress.getByName(ipString);
		String mac = CommandService.getMacFromIp(ipString);
		if (TextUtils.isEmpty(mac)) {
			return null;
		}
		byte[] confirmByte = jnic.encode(dataString, dataString.length());
		DatagramPacket packet = new DatagramPacket(confirmByte, confirmByte.length, server_ip, Config.PLUG_PORT);
		return packet;
	}
	
	public static String recvResponse(DatagramSocket cmdSocket) throws IOException {
		byte[] recvByte = new byte[1024];
        DatagramPacket recvConfirmPacket = new DatagramPacket(recvByte, recvByte.length);
        cmdSocket.receive(recvConfirmPacket);
        String recvString = jnic.decode(recvByte, recvConfirmPacket.getLength());
        return recvString;
	}
	
	public static DatagramPacket recvResponsePacket(DatagramSocket cmdSocket) throws IOException {
		byte[] recvByte = new byte[1024];
        DatagramPacket recvConfirmPacket = new DatagramPacket(recvByte, recvByte.length);
        cmdSocket.receive(recvConfirmPacket);
        return recvConfirmPacket;
	}
	
	public static String byte2hex(byte[] b) {
		StringBuffer hs = new StringBuffer(b.length);
		String stmp = "";
		int len = b.length;
		for (int n = 0; n < len; n++) {
			stmp = Integer.toHexString(b[n] & 0xFF);
			if (stmp.length() == 1)
				hs = hs.append("0").append(stmp);
			else {
				hs = hs.append(stmp);
			}
		}
		return String.valueOf(hs);
	}
	
	public static byte[] hex2byte(String str) { // 字符串转二进制
		if (str == null)
			return null;
		str = str.trim();
		int len = str.length();
		if (len == 0 || len % 2 == 1)
			return null;

		byte[] b = new byte[len / 2];
		try {
			for (int i = 0; i < str.length(); i += 2) {
				b[i / 2] = (byte) Integer
						.decode("0x" + str.substring(i, i + 2)).intValue();
			}
			return b;
		} catch (Exception e) {
			return null;
		}
	}
	
	public void writeFileToSD(String content) {  
	    String sdStatus = Environment.getExternalStorageState();  
	    if(!sdStatus.equals(Environment.MEDIA_MOUNTED)) {  
	        Log.d("TestFile", "SD card is not avaiable/writeable right now.");  
	        return;  
	    }  
	    try {  
	        String pathName=Environment.getExternalStorageDirectory().getPath() + "/123123123";  
	        String fileName="/file.txt";  
	        File path = new File(pathName);  
	        File file = new File(pathName + fileName);  
	        if( !path.exists()) {  
	            Log.d("TestFile", "Create the path:" + pathName);  
	            path.mkdir();  
	        }  
	        if( !file.exists()) {  
	            Log.d("TestFile", "Create the file:" + fileName);  
	            file.createNewFile();  
	        }  
	        FileOutputStream stream = new FileOutputStream(file);  
	        byte[] buf = content.getBytes();  
	        stream.write(buf);            
	        stream.close();  
	    } catch(Exception e) {
	        Log.e("TestFile", "Error on writeFilToSD.");
	        e.printStackTrace();
	    }  
	}  
}
