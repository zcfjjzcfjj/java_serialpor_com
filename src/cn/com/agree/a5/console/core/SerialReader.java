package cn.com.agree.a5.console.core;

import gnu.io.*;
import java.io.*; 
import java.util.*;  
 
 
/**
 * 
 * @author root
 * 问题：
 * 0 创建串口通信仓库
 * 0 这里的线程的start和run的关系？
 * 1 顺流程，需要重新包装
 * 2 串口接受数据和发送数据需要重新包装接口，某些变量和配置可以时一次性配置和共用的
 *
 */

public class SerialReader extends Observable implements Runnable,SerialPortEventListener
    {
    static CommPortIdentifier portId;
    int delayRead = 100;
    int numBytes; 										// buffer中的实际数据字节数
    private static byte[] readBuffer = new byte[1024];  // 4k的buffer空间,缓存串口读入的数据
    static Enumeration portList;
    InputStream inputStream;                            // 串口输出流，从此获取数据
    OutputStream outputStream;                          // 串口输入流, 从此向串口写入数据
    static SerialPort serialPort;                       // NOTE:串口对象,所有的线程 共用统一个静态串口对象
    
    HashMap serialParams;								// 串口配置hasmap
    
    Thread readThread;								    // 本来是static类型的
    boolean isOpen = false; 	                        // 端口是否打开了
    														  // 端口读入数据事件触发后,等待n毫秒后再读取,以便让数据一次性读完
    public static final String PARAMS_DELAY = "delay read" ;  // 延时等待端口数据准备的时间
    public static final String PARAMS_TIMEOUT = "timeout";	  // 超时时间
    public static final String PARAMS_PORT = "port name";     // 端口名称
    public static final String PARAMS_DATABITS = "data bits"; // 数据位
    public static final String PARAMS_STOPBITS = "stop bits"; // 停止位
    public static final String PARAMS_PARITY = "parity";      // 奇偶校验
    public static final String PARAMS_RATE = "rate";          // 波特率

    public boolean isOpen(){
    	return isOpen;
    }
    
    
    public SerialReader()
    {
    	isOpen = false;
    }

    
    /**
     * 打开串口，从map params中获取相关参数,并对串口进行初始化设定
     * @param params 初始化参数map
     */
    public void open(HashMap params) 
    { 
    	serialParams = params;
    	// 如果串口已经打开，则关闭串口
    	if(isOpen){
    		close();
    	}
        try
        {
            // 参数初始化 ==  从params获取端口设置参数
            int timeout = Integer.parseInt( serialParams.get( PARAMS_TIMEOUT ).toString() );    // 超时时间
            int rate = Integer.parseInt( serialParams.get( PARAMS_RATE ).toString() );			// 波特率
            int dataBits = Integer.parseInt( serialParams.get( PARAMS_DATABITS ).toString() );  // 数据位
            int stopBits = Integer.parseInt( serialParams.get( PARAMS_STOPBITS ).toString() );  // 停止位
            int parity = Integer.parseInt( serialParams.get( PARAMS_PARITY ).toString() );      // 奇偶校验
            delayRead = Integer.parseInt( serialParams.get( PARAMS_DELAY ).toString() );		// 等待数据时间
            String port = serialParams.get( PARAMS_PORT ).toString();							// 端口名称
            // 打开端口
            portId = CommPortIdentifier.getPortIdentifier( port );
            serialPort = ( SerialPort ) portId.open( "SerialReader", timeout );
            inputStream = serialPort.getInputStream();
            serialPort.addEventListener( this );        //  串口事件监听
            serialPort.notifyOnDataAvailable( true );	//  !!!!NOTE： 被观察者，observable ,通知数据到达 ,该函数会通知观察者调用update方法
            serialPort.setSerialPortParams( rate, dataBits, stopBits, parity ); // 初始化串口
            
            isOpen = true; //设定 串口已打开
        }
        catch ( PortInUseException e )
        {
           // 端口"+serialParams.get( PARAMS_PORT ).toString()+"已经被占用";
        	
        }
        catch ( TooManyListenersException e )
        {
           //"端口"+serialParams.get( PARAMS_PORT ).toString()+"监听者过多";
        }
        catch ( UnsupportedCommOperationException e )
        {
           //"端口操作命令不支持";
        }
        catch ( NoSuchPortException e )
        {
          //"端口"+serialParams.get( PARAMS_PORT ).toString()+"不存在";
        }
        catch ( IOException e )
        {
           //"打开端口"+serialParams.get( PARAMS_PORT ).toString()+"失败";
        }
        serialParams.clear();  						// 初始化后清空参数map
        Thread readThread = new Thread( this ); 	// NOTE:创建串口读取线程
        readThread.start();
    }

     
    public void run()
    {
        try
        {
            Thread.sleep(50);
        }
        catch ( InterruptedException e )
        {
        }
    } 
    
    /**
     * 创建读取线程，并启动
     */
    public void start(){
    	try {  
    		outputStream = serialPort.getOutputStream();
    	} 
    	catch (IOException e) {}
    	
    	try{ 
    		readThread = new Thread(this);
    		readThread.start();
    	} 
    	catch (Exception e) {  }
    }  //start() end


    /**
     *   
     */
   public void run(String message) {
	   
	   try { 
		   Thread.sleep(4); 
	   } 
	   catch (InterruptedException e)
	   {  

	   } 
	   
	   try {
		   if( message!=null && message.length()!=0 )
		   { 	 
			   System.out.println("run message:"+message);
			   outputStream.write(message.getBytes()); //往串口发送数据，是双向通讯的。
		   }
	   } catch (IOException e)
	   {

	   }
   } 
    

   /**
    * 如果串口在打开状态，则关闭串口,并关闭相关设置，清除相关资源
    */
    public void close() 
    { 
        if (isOpen)
        {
            try
            {
            	serialPort.notifyOnDataAvailable(false);
            	serialPort.removeEventListener();
                inputStream.close();
                serialPort.close();
                isOpen = false;
            } catch (IOException ex)
            {
            //"关闭串口失败";
            }
        }
    }
    
    /**
     * 串口监听事件 当有事件到达，进行事件分类处理；<br>
     * 主要处理 DATA_AVAILABLE 事件，即有数据到达时，读取数据;<br>
     * 过程：事件发生，如数据到来，触发 本函数的事件处理函数，调用changeMessage（），该函数会通知观察者;<br>
     */
    public void serialEvent( SerialPortEvent event )
    {
        try
        {
            Thread.sleep( delayRead ); //  延时等待端口数据准备的时间 
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
        switch ( event.getEventType() )
        {
            case SerialPortEvent.BI: // 10
            case SerialPortEvent.OE: // 7
            case SerialPortEvent.FE: // 9
            case SerialPortEvent.PE: // 8
            case SerialPortEvent.CD: // 6
            case SerialPortEvent.CTS: // 3
            case SerialPortEvent.DSR: // 4
            case SerialPortEvent.RI: // 5
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY: // 2
                break;
            case SerialPortEvent.DATA_AVAILABLE: // 1   数据到来事件处理, 读取数据
                try
                {
                    // 多次读取,将所有数据读入
                     while (inputStream.available() > 0) {
                     numBytes = inputStream.read(readBuffer);
                     }
                     
                     //打印接收到的字节数据的ASCII码
                     for(int i=0;i<numBytes;i++){
                    	// System.out.println("msg[" + numBytes + "]: [" +readBuffer[i] + "]:"+(char)readBuffer[i]);
                     }
//                    numBytes = inputStream.read( readBuffer );
                    changeMessage( readBuffer, numBytes );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
                break;
        }
    }

    
    /**
     * 通过observer pattern将收到的数据发送给observer, 将buffer中的空字节删除后再发送更新消息,通知观察者;<br>
     * 关键函数：(observer 与 observerable 关联)<br>
     * 调用setChanged(),notifyObservers()通过观察者,并传递接受到的数据;<br>
     * @param 串口接受到的数据(在缓存readBuffer中)
     * @param 数据长度
     */
    public void changeMessage( byte[] message, int length )
    {
        setChanged();
        byte[] temp = new byte[length];
        System.arraycopy( message, 0, temp, 0, length );
        notifyObservers( temp );
    }

    /**
     * 列出所有串口
     */
    static void listPorts()
    {
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        while ( portEnum.hasMoreElements() )
        {
            CommPortIdentifier portIdentifier = ( CommPortIdentifier ) portEnum
                .nextElement();
            
        }
    }
    
    
    /**
     *  打开串口并发送数据message单独的接口，可以直接使用，params参数设定在本函数中设定
     *  @param message 要发送的字符串
     */
    public void openSerialPort(String message)
    {
        HashMap<String, Comparable> params = new HashMap<String, Comparable>();  
//        String port="COM1";
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
        params.put( SerialReader.PARAMS_TIMEOUT, 100 ); // 设备超时时间 1秒
        params.put( SerialReader.PARAMS_DELAY, 100 ); // 端口数据准备时间 1秒
        try {
			open(params);//打开串口
			//LoginFrame cf=new LoginFrame();
			//addObserver(cf);
			//也可以像上面一个通过LoginFrame来绑定串口的通讯输出.
			if(message!=null&&message.length()!=0)
			 {
				String str="";
				for(int i=0;i<10;i++)
				{
					str+=message;
				}
				 start(); 
			     run(str);  
			 } 
		} catch (Exception e) { 
		}
    }

    static String getPortTypeName( int portType )
    {
        switch ( portType )
        {
            case CommPortIdentifier.PORT_I2C:
                return "I2C";
            case CommPortIdentifier.PORT_PARALLEL:
                return "Parallel";
            case CommPortIdentifier.PORT_RAW:
                return "Raw";
            case CommPortIdentifier.PORT_RS485:
                return "RS485";
            case CommPortIdentifier.PORT_SERIAL:
                return "Serial";
            default:
                return "unknown type";
        }
    }

     
    /**
     * 获取可用的串口hashset
     */
    public  HashSet<CommPortIdentifier> getAvailableSerialPorts()//本来static
    {
        HashSet<CommPortIdentifier> h = new HashSet<CommPortIdentifier>();
        Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
        while ( thePorts.hasMoreElements() )
        {
            CommPortIdentifier com = ( CommPortIdentifier ) thePorts
                .nextElement();
            switch ( com.getPortType() )
            {
                case CommPortIdentifier.PORT_SERIAL:
                    try
                    {
                        CommPort thePort = com.open( "CommUtil", 50 );
                        thePort.close();
                        h.add( com );
                    }
                    catch ( PortInUseException e )
                    {
                        System.out.println( "Port, " + com.getName()
                            + ", is in use." );
                    }
                    catch ( Exception e )
                    {
                        System.out.println( "Failed to open port "
                            + com.getName() + e );
                    }
            }
        }
        return h;
    }
}
