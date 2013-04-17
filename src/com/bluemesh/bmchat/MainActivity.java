package com.bluemesh.bmchat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import blue.mesh.BlueMeshService;
import blue.mesh.BlueMeshServiceBuilder;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class MainActivity extends Activity {
	
	protected BlueMeshService bms = null;
	private ReadThread readThread;
	private static Button btnSend;
	private static EditText txtMessage;
	private static ListView lstMessages;
	private static ArrayList <String> messages;
	private static ArrayAdapter<String> adapter;
	private static MediaPlayer mp;
	private static final String beepbeep = "BEEPBEEP";
	
	private static final Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			byte[] bytes = (byte[]) msg.obj;
			String msgString = new String(bytes);
			if( msgString.equals(beepbeep)){
	        	mp.start();
			}
			else{
				messages.add(msgString);
				adapter.notifyDataSetChanged();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		btnSend = (Button)findViewById(R.id.btnSend);
		txtMessage = (EditText)findViewById(R.id.txtType);
		lstMessages = (ListView)findViewById(R.id.lstMessages);
		lstMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
		messages = new ArrayList<String>();
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, messages);
		lstMessages.setAdapter(adapter);
		btnSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				byte[] message = txtMessage.getText().toString().getBytes();
				bms.write(message);
				mHandler.obtainMessage(0, message.length, -1, message).sendToTarget();
				txtMessage.setText("");
			}
		});
        mp = MediaPlayer.create(this, R.raw.beep);
        mp.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                try {
					mp.prepare();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        });  
		
		try{
			BlueMeshServiceBuilder bmsb = new BlueMeshServiceBuilder();
			bms = bmsb.bluetooth(true).debug(false).uuid(new UUID(1337, 80085)).build();
		}
		catch(NullPointerException e){
			finish();
			return;
		}
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		
		bms.launch();
				
		readThread = new ReadThread();		
		readThread.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.action_beep:
	        	bms.write(beepbeep.getBytes());
				mHandler.obtainMessage(0, beepbeep.getBytes().length, -1, beepbeep.getBytes()).sendToTarget();
	            return true;
	        case R.id.action_quit:
	        	cleanUp();
	            finish();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	private void cleanUp(){
		readThread.interrupt();		
		if(bms != null){
			bms.disconnect();
		}
	}
	
	private class ReadThread extends Thread{
		public void run(){
			Looper.myLooper();
			Looper.prepare();
			byte[] message = null;
			
			while(!this.isInterrupted()){
				message = null;
				try{
					message = bms.pull();
				}
				catch( NullPointerException e ){
					break;
				}
				
				if(message == null){
					try{
						sleep(100);
					}
					catch(InterruptedException e){
					}
				}
				else{
					mHandler.obtainMessage(0, message.length, -1, message).sendToTarget();
				}
			}
		}
	}
}
