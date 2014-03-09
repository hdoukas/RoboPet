package com.createnet.robopet;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

public class BackService extends Service {

	private static final int REQUEST_ENABLE_BT = 1;
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;

	String TAG = "RoboPet";

	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	// Insert your server's MAC address
	private static String address = "00:06:66:66:28:A2";

	private String wsUri = "http://192.168.9.218:3000";
	private SocketIO wsClient;
	private boolean connecting;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {

		super.onCreate();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO do something useful
		Log.i("FromDeamon", "Deamon is running...");

		btAdapter = BluetoothAdapter.getDefaultAdapter();
		checkBTState();
		tryConnectBT();

		// do websocket stuff:
		connectWs(true);

		return Service.START_STICKY;
	}

	private void tryConnectBT() {
		Log.d("", "...In onResume - Attempting client connect...");

		// Set up a pointer to the remote node using it's address.
		BluetoothDevice device = btAdapter.getRemoteDevice(address);

		// Two things are needed to make a connection:
		// A MAC address, which we got above.
		// A Service ID or UUID. In this case we are using the
		// UUID for SPP.
		try {
			btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
		} catch (IOException e) {
			errorExit("Fatal Error", "In onResume() and socket create failed: "
					+ e.getMessage() + ".");
		}

		// Discovery is resource intensive. Make sure it isn't going on
		// when you attempt to connect and pass your message.
		btAdapter.cancelDiscovery();

		// Establish the connection. This will block until it connects.
		Log.d("", "...Connecting to Remote...");
		try {
			btSocket.connect();
			Log.d("", "...Connection established and data link opened...");
		} catch (IOException e) {
			try {
				btSocket.close();
			} catch (IOException e2) {
				errorExit("Fatal Error",
						"In onResume() and unable to close socket during connection failure"
								+ e2.getMessage() + ".");
			}
		}

		// Create a data stream so we can talk to server.
		Log.d("", "...Creating Socket...");

		try {
			outStream = btSocket.getOutputStream();
		} catch (IOException e) {
			errorExit(
					"Fatal Error",
					"In onResume() and output stream creation failed:"
							+ e.getMessage() + ".");
		}

	}

	private boolean isNetworkConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni == null) {
			// There are no active networks.
			return false;
		} else
			return true;
	}

	private void checkBTState() {
		// Check for Bluetooth support and then check to make sure it is turned
		// on

		// Emulator doesn't support Bluetooth and will return null
		if (btAdapter == null) {
			errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
		} else {
			if (btAdapter.isEnabled()) {
				Log.d("", "...Bluetooth is enabled...");
			} else {
				// no BT do nothing
			}
		}
	}

	private void errorExit(String title, String message) {
		Toast msg = Toast.makeText(getBaseContext(), title + " - " + message,
				Toast.LENGTH_SHORT);
		Log.e(TAG, title + " - " + message);
		msg.show();

	}

	private void sendData(String message) {
		byte[] msgBuffer = message.getBytes();

		Log.d("", "...Sending data: " + message + "...");

		try {
			outStream.write(msgBuffer);
		} catch (IOException e) {
			String msg = "In onResume() and an exception occurred during write: "
					+ e.getMessage();
			if (address.equals("00:00:00:00:00:00"))
				msg = msg
						+ ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
			msg = msg + ".\n\nCheck that the SPP UUID: " + MY_UUID.toString()
					+ " exists on server.\n\n";

			errorExit("Fatal Error", msg);
		}
	}

	private void connectWs() {
		connectWs(true);
	}

	private void connectWs(boolean reconnect) {
		try {

			if (wsClient == null) {
				try {

					wsClient = new SocketIO(wsUri);

					connecting = true;
				} catch (Exception e) {
					Log.e(TAG, "WS error initializing: " + e.getMessage());
					wsClient = null;
					connecting = false;
					return;
				}
			} else {

				if (wsClient.isConnected()) {
					if (!reconnect) {
						connecting = false;
						return;
					} else {
						connecting = true;
						wsClient.reconnect();
						return;
					}
				}
			}

			Log.d(TAG, "Connect to WS");
			Log.d(TAG, "WS url is " + wsUri);

			wsClient.connect(new IOCallback() {
				@Override
				public void onMessage(JSONObject json, IOAcknowledge ack) {
					try {
						Log.d(TAG, "Server said:" + json.toString(2));
						// send it to BT:
						//sendData(json.toString(2));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void onMessage(String data, IOAcknowledge ack) {
					Log.d(TAG, "Server said: " + data);
				}

				@Override
				public void onError(SocketIOException socketIOException) {
					Log.e(TAG,
							"WS Error occured " + socketIOException.getMessage());
				}

				@Override
				public void onDisconnect() {
					Log.w(TAG, "Connection terminated.");
					connecting = false;
				}

				@Override
				public void onConnect() {
					connecting = false;
					Log.w(TAG, "Connection established");
				}

				@Override
				public void on(String event, IOAcknowledge ack, Object... args) {

					Log.w(TAG, "Server triggered event '" + event + "'");
					//Log.w(TAG, args.toString());
					
					Object[] obj = (Object[]) args;
					JSONObject h = (JSONObject) obj[0];
					Log.e(TAG, h.toString());					
					
					try {

						String cmd = h.getString("command");
//						String cmd = h.getString("command");
						Log.i(TAG, "command " + cmd);
						sendData(cmd);
					} catch (JSONException e) {
						Log.e(TAG, "command parsing error: " + e.getMessage());
					}
										
					
				}
			});

			connecting = false;

		} catch (Exception e) {
			Log.e(TAG, "WS error: " + e.getMessage());
		}
	}
	//
	// private class ConnectBTTask extends AsyncTask<URL, Integer, Long> {
	// protected Long doInBackground(URL... urls) {
	// int count = urls.length;
	// long totalSize = 0;
	// for (int i = 0; i < count; i++) {
	//
	// publishProgress((int) ((i / (float) count) * 100));
	//
	// // Escape early if cancel() is called
	// if (isCancelled())
	// break;
	// }
	// return totalSize;
	// }
	//
	// protected void onProgressUpdate(Integer... progress) {
	//
	// }
	//
	// protected void onPostExecute(Long result) {
	// Log.d(TAG, "Downloaded " + result + " bytes");
	// }
	// }

}
