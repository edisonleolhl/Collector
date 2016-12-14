package com.example.collector;

/*
 * 
 * @author   liaohongfei
 * 
 * @Date  2015 2015-1-27  上午9:53:52
 * 
 */
public class BluetoothMsg {

	/** 
     * 蓝牙连接类型 
     * @author liao 
     * 
     */  
    public enum ServerOrCilent {
        NONE,  
        SERVICE,  
        CILENT  
    };
    
    //蓝牙连接方式  
    public static ServerOrCilent serviceOrCilent = ServerOrCilent.NONE;  
    //连接蓝牙地址  
    public static String BlueToothAddress = null, lastblueToothAddress = null;  
    //通信线程是否开启  
    public static boolean isOpen = false;
    
}