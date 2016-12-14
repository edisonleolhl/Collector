package com.example.collector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class MainActivity extends Activity implements OnItemClickListener {

	private Context mContext;
	private BluetoothAdapter mBluetoothAdapter; // Bluetooth适配器
	private BluetoothDevice device;             // 蓝牙设备
	private ListView mListView;
	private ArrayList<ChatMessage> list;
	private ClientAdapter clientAdapter;        // ListView适配器
	private Button disconnectButton = null, collectButton = null;

    static final int frequency = 8000;//分辨率  
    static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;  
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;  
    int recBufSize;//录音最小buffer大小  
    AudioRecord audioRecord;     
    ClsCollector clsCollector=new ClsCollector();  

    private BluetoothSocket socket;     // 客户端socket
	private ClientThread mClientThread; // 客户端运行线程
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		init();
	}

	// 变量初始化
	private void init() {
		// TODO Auto-generated method stub
		mContext = this;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		list = new ArrayList<ChatMessage>();
		clientAdapter = new ClientAdapter(mContext, list);
		mListView = (ListView) findViewById(R.id.list);
		mListView.setFastScrollEnabled(true);
		mListView.setAdapter(clientAdapter);
		mListView.setOnItemClickListener(this);

        //录音组件  
        recBufSize = AudioRecord.getMinBufferSize(frequency,  
                channelConfiguration, audioEncoding);  
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,  
                channelConfiguration, audioEncoding, recBufSize);  
        
		// 注册receiver监听
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);
		
		// 获取已经配对过的蓝牙设备
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0) {
			for (BluetoothDevice device : pairedDevices) {
				list.add(new ChatMessage(device.getName() + "\n" + device.getAddress(), true));
				clientAdapter.notifyDataSetChanged();
				mListView.setSelection(list.size() - 1);
			}
		} else {
			list.add(new ChatMessage("没有已经配对过的设备", true));
			clientAdapter.notifyDataSetChanged();
			mListView.setSelection(list.size() - 1);
		}
		
		collectButton = (Button) findViewById(R.id.btn_collect);
		collectButton.setEnabled(false);
		collectButton.setOnClickListener(new ClickEvent());
		
		disconnectButton = (Button) findViewById(R.id.disconnect);
		disconnectButton.setEnabled(false);
		disconnectButton.setOnClickListener(new ClickEvent());
	}
	
    /** 
     * 按键事件处理 
     * 
     */  
    class ClickEvent implements View.OnClickListener {  
        @Override  
        public void onClick(View v) {  
            if (v == collectButton) {  
            	System.out.println("开始采集");
                clsCollector.Start(audioRecord,recBufSize,socket);  
				Toast.makeText(mContext, "Collecting!!!", Toast.LENGTH_SHORT).show();
            } else if (v == disconnectButton) {  
            	System.out.println("断开采集");
                clsCollector.Stop();  
				Toast.makeText(mContext, "Disconnect!!!", Toast.LENGTH_SHORT).show();
            }  
        }  
    }  
	
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		if (mBluetoothAdapter != null) {
			if (!mBluetoothAdapter.isEnabled()) {
				// 发送打开蓝牙的意图，系统会弹出一个提示对话框
        		Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        		startActivityForResult(enableIntent, RESULT_FIRST_USER);
        		
        		// 设置蓝牙的可见性，最大值3600秒，默认120秒，0表示永远可见(作为客户端，可见性可以不设置，服务端必须要设置)
        		Intent displayIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        		displayIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        		startActivity(displayIntent);
        		
        		// 直接打开蓝牙
        		mBluetoothAdapter.enable();
			}
		}
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		// 扫描
		scanDevice();
	}
	
	/**
	 * 蓝牙设备扫描过程中(mBluetoothAdapter.startDiscovery())会发出的消息
	 * ACTION_FOUND 扫描到远程设备
	 * ACTION_DISCOVERY_FINISHED 扫描结束
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) 
            {
                // Get the BluetoothDevice object from the Intent
            	// 通过EXTRA_DEVICE附加域来得到一个BluetoothDevice设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                // If it's already paired, skip it, because it's been listed already
                // 如果这个设备是不曾配对过的，添加到list列表
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) 
                {
                	list.add(new ChatMessage(device.getName() + "\n" + device.getAddress(), false));
                	clientAdapter.notifyDataSetChanged();
            		mListView.setSelection(list.size() - 1);
                }
            // When discovery is finished, change the Activity title
            } 
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) 
            {
                setProgressBarIndeterminateVisibility(false);
                if (mListView.getCount() == 0) 
                {
                	list.add(new ChatMessage("没有发现蓝牙设备", false));
                	clientAdapter.notifyDataSetChanged();
            		mListView.setSelection(list.size() - 1);
                }
            }
        }
    };
    
    // Handler更新UI

    private Handler LinkDetectedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	//Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_SHORT).show();
        	if(msg.what==1)
        	{
        		list.add(new ChatMessage((String)msg.obj, true));
        	}
        	else
        	{
        		list.add(new ChatMessage((String)msg.obj, false));
        	}
			clientAdapter.notifyDataSetChanged();
			mListView.setSelection(list.size() - 1);
			
        }
        
    };
    
    /**
     * 当连接上服务器的时候才可以选择发送数据和断开连接
     */
    private Handler refreshUI = new Handler() {
    	public void handleMessage(Message msg) {
    		if (msg.what == 0) {
    			disconnectButton.setEnabled(true);
				collectButton.setEnabled(true);
			}
    	}
    };
    
    /**
     * 开启客户端连接服务端
     */
    private class ClientThread extends Thread {
    	@Override
    	public void run() {
    		// TODO Auto-generated method stub
    		if (device != null) {
				try {
					socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
					// 连接
					Message msg = new Message();
					msg.obj = "请稍候，正在连接服务器: "+ BluetoothMsg.BlueToothAddress;
					msg.what = 0;
					LinkDetectedHandler.sendMessage(msg);
					
					// 通过socket连接服务器，这是一个阻塞过程，直到连接建立或者连接失效
					socket.connect();
					
					Message msg2 = new Message();
					msg2.obj = "已经连接上服务端！可以发送信息";
					msg2.what = 0;
					LinkDetectedHandler.sendMessage(msg2);
					
					// 更新UI界面
					Message uiMessage = new Message();
					uiMessage.what = 0;
					refreshUI.sendMessage(uiMessage);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					// socket.connect()连接失效
					Message msg = new Message();
					msg.obj = "连接服务端异常！断开连接重新试一试。";
					msg.what = 0;
					LinkDetectedHandler.sendMessage(msg);
				}
			}
    	}
    }
    
    /**
     * 停止蓝牙连接服务
     */
    private void closeClient() {
    	new Thread() {
    		public void run() {
    			if (mClientThread != null) {
    				mClientThread.interrupt();
    				mClientThread = null;
				}
    			try {
					if (socket != null) {
						socket.close();
						socket = null;
					}
				} catch (IOException e) {
					// TODO: handle exception
				}
    		}
    	}.start();
    }
    
    /**
     * 扫描设备
     */
    private void scanDevice() {
		// TODO Auto-generated method stub
    	if (mBluetoothAdapter.isDiscovering()) {
			mBluetoothAdapter.cancelDiscovery();
		} else {
			list.clear();
			clientAdapter.notifyDataSetChanged();
			
			// 每次扫描前都先判断一下是否存在已经配对过的设备
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			if (pairedDevices.size() > 0) {
		            for (BluetoothDevice device : pairedDevices) {
		            	list.add(new ChatMessage(device.getName() + "\n" + device.getAddress(), true));
		            	clientAdapter.notifyDataSetChanged();
		        		mListView.setSelection(list.size() - 1);
		            }
		    } else {
		        	list.add(new ChatMessage("No devices have been paired", true));
		        	clientAdapter.notifyDataSetChanged();
		    		mListView.setSelection(list.size() - 1);
		     }				
	        /* 开始搜索 */
			mBluetoothAdapter.startDiscovery();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
		ChatMessage item = list.get(arg2);
		String info = item.getMessage();
		String address = info.substring(info.length() - 17);
		BluetoothMsg.BlueToothAddress = address;
		
		// 停止扫描
		// BluetoothAdapter.startDiscovery()很耗资源，在尝试配对前必须中止它
		mBluetoothAdapter.cancelDiscovery();
		
		// 通过Mac地址去尝试连接一个设备
		device = mBluetoothAdapter.getRemoteDevice(BluetoothMsg.BlueToothAddress);
		mClientThread = new ClientThread();
		mClientThread.start();
		BluetoothMsg.isOpen = true;
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (mBluetoothAdapter != null) {
			mBluetoothAdapter.cancelDiscovery();
			// 关闭蓝牙
			mBluetoothAdapter.disable();
		}
		unregisterReceiver(mReceiver);
		closeClient();
        clsCollector.Stop();  
	}
}