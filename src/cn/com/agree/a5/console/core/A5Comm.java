package cn.com.agree.a5.console.core;
 
import gnu.io.SerialPort;
import java.util.Observer;
import java.util.*;

public class A5Comm implements Observer{    // 观察者
	 
	SerialReader sr=new SerialReader();   // 被观察者,串口监听读取线程
	
    public A5Comm()
    {    
       openSerialPort(""); //打开串口。message
    } 
    
    /**
     * 获取并打印串口收到的数据
     * NOTE:当被观察者变化，观察者的该函数被调用
     */
    public void update(Observable o, Object arg){    
    	String mt=new String((byte[])arg);  
    	System.out.println("---recv data:---"+mt); //串口数据 
    } 
    
    /**
     * 往串口发送数据,实现双向通讯.
     * @param string message
     */
    public void send(String message)
    {
    	A5Comm test = new A5Comm();
    	test.openSerialPort(message);
    }
	
    /**
     * 打开串口
     * @param String message
     */
	public void openSerialPort(String message)
    { 
        HashMap<String, Comparable> params = new HashMap<String, Comparable>();  
        String port="/dev/ttyS0";
        String rate = "9600";
        String dataBit = ""+SerialPort.DATABITS_8;
        String stopBit = ""+SerialPort.STOPBITS_1;
        String parity = ""+SerialPort.PARITY_NONE;    
        int parityInt = SerialPort.PARITY_NONE; 
        params.put( SerialReader.PARAMS_PORT, port ); // 端口名称
        params.put( SerialReader.PARAMS_RATE, rate ); // 波特率
        params.put( SerialReader.PARAMS_DATABITS,dataBit  ); // 数据位
        params.put( SerialReader.PARAMS_STOPBITS, stopBit ); // 停止位
        params.put( SerialReader.PARAMS_PARITY, parityInt ); // 无奇偶校验
        params.put( SerialReader.PARAMS_TIMEOUT,100 ); // 设备超时时间 1秒
        params.put( SerialReader.PARAMS_DELAY, 100 ); // 端口数据准备时间 1秒
        try {
			sr.open(params);
		    sr.addObserver(this);
			if(message!=null&&message.length()!=0)
			 {  
				sr.start();  
				sr.run(message);  
			 } 
		} catch (Exception e) { 
		}
    }
    
	
	 public String Bytes2HexString(byte[] b) { 
		   String ret = ""; 
		   for (int i = 0; i < b.length; i++) { 
			     String hex = Integer.toHexString(b[i] & 0xFF); 
			     if (hex.length() == 1) { 
			       hex = '0' + hex; 
				     } 
			     ret += hex.toUpperCase(); 
			   }
		return ret;
	   }


	  public  String hexString2binaryString(String hexString) {
	  if (hexString == null || hexString.length() % 2 != 0)
		 return null;
		 String bString = "", tmp;
		 for (int i = 0; i < hexString.length(); i++) {
		 tmp = "0000" + Integer.toBinaryString(Integer.parseInt(hexString.substring(i, i + 1), 16));
			  bString += tmp.substring(tmp.length() - 4);
		  }
		 return bString;
	  } 
	  
	  public static void main(String[] args) {
		  A5Comm test = new A5Comm();
//		  test.send("hello com1");
		
	}
} 
 
