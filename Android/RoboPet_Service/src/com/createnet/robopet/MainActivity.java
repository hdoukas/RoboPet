package com.createnet.robopet;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.bugsense.trace.BugSenseHandler;


public class MainActivity extends Activity {
	
	private Button fwd,backwd,left,right, send;
	
	private ToggleButton laserButton1;
	private EditText input;
	
	private static final int REQUEST_ENABLE_BT = 1;
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;
	

	String TAG = "RoboPet";
	private String wsUri = "http://192.168.9.218:3000";
	private SocketIO wsClient;
	private boolean connecting;
	
	
	
	// Well known SPP UUID
	  private static final UUID MY_UUID =UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	 
	  // Insert your server's MAC address
	  private static String address = "00:06:66:66:28:A2";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		BugSenseHandler.initAndStartSession(this, "78d42129");
		setContentView(R.layout.activity_main);
		
		fwd = (Button) findViewById(R.id.button1);
		backwd = (Button) findViewById(R.id.button2);
		left = (Button) findViewById(R.id.button3);
		right = (Button) findViewById(R.id.button4);
		send = (Button) findViewById(R.id.button5);
		
		input = (EditText) findViewById(R.id.editText1);
		
		
		//btAdapter = BluetoothAdapter.getDefaultAdapter();
	    //checkBTState();
	    
	    
	    fwd.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	            //sendData("111");
	          }
	    });
	    
	    backwd.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	           // sendData("1");
	          }
	    });
	    
	    left.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	           // sendData("333");
	          }
	    });
	    
	    
	    right.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	           // sendData("444");
	          }
	    });
	    
	    send.setOnClickListener(new OnClickListener() {
	    	public void onClick(View v) {
	            //sendData(input.getText().toString());
	          }
	    });
	    
//	    connectWs(false);
	    
	    //Start the Backround service:
	    Intent background = new Intent(this, BackService.class);
		startService(background);
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
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
//	@Override
//	  public void onResume() {
//	    super.onResume();
//	 
//	    Log.d("", "...In onResume - Attempting client connect...");
//	   
//	    // Set up a pointer to the remote node using it's address.
//	    BluetoothDevice device = btAdapter.getRemoteDevice(address);
//	   
//	    // Two things are needed to make a connection:
//	    //   A MAC address, which we got above.
//	    //   A Service ID or UUID.  In this case we are using the
//	    //     UUID for SPP.
//	    try {
//	      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
//	    } catch (IOException e) {
//	      errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
//	    }
//	   
//	    // Discovery is resource intensive.  Make sure it isn't going on
//	    // when you attempt to connect and pass your message.
//	    btAdapter.cancelDiscovery();
//	   
//	    // Establish the connection.  This will block until it connects.
//	    Log.d("", "...Connecting to Remote...");
//	    try {
//	      btSocket.connect();
//	      Log.d("", "...Connection established and data link opened...");
//	    } catch (IOException e) {
//	      try {
//	        btSocket.close();
//	      } catch (IOException e2) {
//	        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
//	      }
//	    }
//	     
//	    // Create a data stream so we can talk to server.
//	    Log.d("", "...Creating Socket...");
//	 
//	    try {
//	      outStream = btSocket.getOutputStream();
//	    } catch (IOException e) {
//	      errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.getMessage() + ".");
//	    }
//	  }
	
//	@Override
//	  public void onPause() {
//	    super.onPause();
//	 
//	    Log.d("", "...In onPause()...");
//	 
//	    if (outStream != null) {
//	      try {
//	        outStream.flush();
//	      } catch (IOException e) {
//	        errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
//	      }
//	    }
//	 
//	    try     {
//	      btSocket.close();
//	    } catch (IOException e2) {
//	      errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
//	    }
//	  }
	
	private void checkBTState() {
	    // Check for Bluetooth support and then check to make sure it is turned on
	 
	    // Emulator doesn't support Bluetooth and will return null
	    if(btAdapter==null) { 
	      errorExit("Fatal Error", "Bluetooth Not supported. Aborting.");
	    } else {
	      if (btAdapter.isEnabled()) {
	        Log.d("", "...Bluetooth is enabled...");
	      } else {
	        //Prompt user to turn on Bluetooth
	        Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
	        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	      }
	    }
	  }
	 
	  private void errorExit(String title, String message){
	    Toast msg = Toast.makeText(getBaseContext(),
	        title + " - " + message, Toast.LENGTH_SHORT);
	    msg.show();
	    finish();
	  }
	 
	  private void sendData(String message) {
	    byte[] msgBuffer = message.getBytes();
	 
	    Log.d("", "...Sending data: " + message + "...");
	 
	    try {
	      outStream.write(msgBuffer);
	    } catch (IOException e) {
	      String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
	      if (address.equals("00:00:00:00:00:00")) 
	        msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
	      msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";
	       
	      errorExit("Fatal Error", msg);       
	    }
	  }

}
