package com.example.collector;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import android.bluetooth.BluetoothSocket;
import android.media.AudioRecord;
import android.util.Log;
import android.widget.Toast;

public class ClsCollector {
    private ArrayList<short[]> inBuf = new ArrayList<short[]>();  
    private boolean isRecording = false;// 线程控制标记  
 
    public ClsCollector() {
		// TODO Auto-generated constructor stub
	}
    /** 
     * 开始 
     *  
     * @param recBufSize 
     *            AudioRecord的MinBufferSize 
     */  
    public void Start(AudioRecord audioRecord, int recBufSize, BluetoothSocket socket) {  
        isRecording = true;
        System.out.println(socket.toString());
        System.out.println("开始录制！");
        new RecordThread(audioRecord, recBufSize).start();// 开始录制线程  
        System.out.println("开始传输！");
        new TransmissionThread(socket).start();// 开始传输线程  
    }  

    /** 
     * 停止 
     */  
    public void Stop() {  
        isRecording = false;  
        inBuf.clear();// 清除  
    }  

    /** 
     * 负责从MIC保存数据到inBuf 
     *  
     *  
     */  
    class RecordThread extends Thread {  
        private int recBufSize;  
        private AudioRecord audioRecord;  
        public RecordThread(AudioRecord audioRecord, int recBufSize) {  
            this.audioRecord = audioRecord;  
            this.recBufSize = recBufSize;  
        }  
        public void run() {  
            try {  
                short[] buffer = new short[recBufSize];  
                audioRecord.startRecording();// 开始录制  
                while (isRecording) {  
                    // 从MIC保存数据到缓冲区  
                    int bufferReadResult = audioRecord.read(buffer, 0,  
                            recBufSize);  
                    short[] tmpBuf = new short[bufferReadResult / 12];  
                    for (int i = 0, ii = 0; i < tmpBuf.length; i++, ii = i  
                            * 12) {  
                        tmpBuf[i] = buffer[ii];  
                    }  
                    synchronized (inBuf) {//  
                        inBuf.add(tmpBuf);// 添加数据  
                        Log.v("RecordThread", "当前inBuf的大小为：" + inBuf.size());
                    }  
                }  
                audioRecord.stop();  
            } catch (Throwable t) {  
            }  
        }  
    };  
    
    /** 
     * 负责向蓝牙传输录音数据
     */  
    class TransmissionThread extends Thread {
    	private BluetoothSocket socket;
    	TransmissionThread(BluetoothSocket socket){
    		this.socket = socket;
    	}
        public void run() {  
            while (isRecording) {  
                ArrayList<short[]> buf = new ArrayList<short[]>();  
                synchronized (inBuf) {  
                    if (inBuf.size() == 0)  
                        continue;  
                    buf = (ArrayList<short[]>) inBuf.clone();// 保存  
                    Log.v("TransmissionThread", "当前inBuf大小为:" + inBuf.size());
                    inBuf.clear();// 清除  
                }
                for (int i = 0; i < buf.size(); i++) {  
                    short[] tmpBuf = buf.get(i);
                    byte[] byteData = toByteArray(tmpBuf);
                    Log.v("TransmissionThread", "写入socket中的byte[]长度为：" + byteData.length);
                    Log.v("TransmissionThread", "写入socket中的byte[]的值为：" + Arrays.toString(byteData));

                    try {
            			OutputStream os = socket.getOutputStream();
            			os.write(byteData);
            		} catch (IOException e) {
            			e.printStackTrace();
            		}
                }  
            }  
        } 
    }
    
	public static byte[] toByteArray(short[] src) {
		
	    int count = src.length;
	    byte[] dest = new byte[count * 2];
	    for (int i = 0; i < count; i++) {
	            dest[i * 2] = (byte) (src[i] >> 8);
	            dest[i * 2 + 1] = (byte) (src[i] >> 0);
	    }
	
	    return dest;
	}
}
