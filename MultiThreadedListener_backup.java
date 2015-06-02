package Client;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.*;

import com.pi4j.io.gpio.*;

import java.io.IOException;


public class MultiThreadedListener implements Runnable{

    protected int          serverPort   = 8080;
   
    String SrvrMsg="";
    InputStream inputex  ;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    public int sysOff = 0, snsrFault = 0;
	public int fanStat = 0; // 0 for Auto (by default) : 1 for Manual (set by user)
    InputStream input = null;
    public static final int SAMPLE_COUNT = 5;
    public static int avgTemp = 0, prevAvgTemp = 0;

    public MultiThreadedListener(int port){
        this.serverPort = port;
    }
    private static String getStringFromInputStream(InputStream is) {
    	  
 		BufferedReader br = null;
 		StringBuilder sb = new StringBuilder();
  
 		String line;
 		try {
  
 			br = new BufferedReader(new InputStreamReader(is));
 			while ((line = br.readLine()) != null) {
 				sb.append(line);
 			}
  
 		} catch (IOException e) {
 			e.printStackTrace();
 		} 
  
 		return sb.toString();
  
 	}
   /* private Runnable handler(String type, String request,Socket clientSocket) {
  		if (type.compareTo("details")==0){
  			return new GetDetails(rs,clientSocket);
  		}
  		else if (type.compareTo("change")==0){
  			return null;
  		}
      	
  		return null;
  	}*/
    public void run(){
        synchronized(this){
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
	
        Thread t1 = new Thread(new SensorHandler());
        Thread t2 = new Thread(new ServerHandler());
        t1.start();
        t2.start();
        while(! isStopped()){
            Socket clientSocket = null;
            String in = null;
            try {
                clientSocket = this.serverSocket.accept();
                InputStream input  = clientSocket.getInputStream();
                 in= getStringFromInputStream(input);
                System.out.println("@@@@@@Client Value."+ in) ;
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException(
                    "Error accepting client connection", e);
            }
            if(in.startsWith("details/")){
            new Thread(
                new GetDetails(new RaspiObject(avgTemp,0,fanStat,sysOff),clientSocket)
            ).start();}
            else if(in.startsWith("StoPi")){
            	 System.out.println("$$$$$$$$$") ;
            	  SrvrMsg=in;
			
            }
        }
        System.out.println("Server Stopped.") ;
    }

	private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }
  
    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } catch (IOException e) {
            throw new RuntimeException("Cannot open port "+this.serverPort, e);
        }
    }

    class ServerHandler implements Runnable
	{

		BufferedReader br;
		Socket sock;
		InputStream Input;
		Scanner scanner;
		final GpioController gpio = GpioFactory.getInstance();

		GpioPinDigitalOutput myLED[]={
		gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "LED #1", PinState.HIGH),
		gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "LED #2", PinState.HIGH),
		gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "LED #3", PinState.HIGH)
		};

		public ServerHandler()
		{
			try
			{
				
				//scanner = new Scanner(inputex);
				 //br = new BufferedReader(new InputStreamReader(inputex));
			}catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		public void run()
		{
			
			int serverTemp = 0;
			int compressorType = 2; // 0: Cool, 1: Heat, 2: None
			
			try{
				while(true)
				{
					if(snsrFault == 0)
					{	
						if((SrvrMsg.startsWith("StoPi")))
						{
							//SrvrMsg = (String)scanner.nextLine();
							//System.out.println("Message from server: " + SrvrMsg);
							
							String tokens[] = SrvrMsg.split(",");
							
							// Msg Format: StoPi,Heat,FanAuto,1,76
							System.out.println("tokens[0] " + tokens[0]);
							System.out.println("tokens[1] " + tokens[1]);
							System.out.println("tokens[2] " + tokens[2]);
							System.out.println("tokens[3] " + tokens[3]);
							System.out.println("tokens[4] " + tokens[4]);
							if(tokens[0].compareTo("StoPi")==0)
							{
								System.out.println("Message from server: " + SrvrMsg);
								if(tokens[3].compareTo("0")==0)
								{
									System.out.println("Turning off the AC system");

									sysOff = 1;
									
								}
								else if (tokens[3].compareTo("1")==0)
								{
									System.out.println("Temperature received from Server in F = " + tokens[4]);
									serverTemp = Integer.parseInt(tokens[4]);
									sysOff = 0;
									
									if(tokens[1].compareTo("Cool")==0)
									{
										compressorType = 0;
									}
									else if(tokens[1].compareTo("Heat")==0)
									{
										compressorType = 1;
									}
									else
									{
										compressorType = 2;
									}
								}
								else if (tokens[3].compareTo("2")==0)
								{
									//readTemp = 1;
								}
								else
								{
									/* Do Nothing */
								}
								
								/* Fan Status to be checked regardless of System Off/On */
								if(tokens[2].compareTo("FanAuto")==0)
								{
									//Turn Fan Off in Auto Mode
									fanStat = 0;
								}
								else if(tokens[2].compareTo("FanMan")==0)
								{
									myLED[2].low(); //Turn Fan On in Manual Mode
									fanStat = 1; //Manual
								}
								else
								{
									/* Do Nothing */
								}
								
								
							}
							SrvrMsg ="";
						}
						else{
							
						}
						if(avgTemp > 0 && sysOff == 0)
						{
							if(serverTemp < avgTemp && compressorType == 0)
							{
								myLED[0].low(); //Cooler GPIO 4 pin 16
								myLED[1].high();
								if(fanStat == 0)
								{
									myLED[2].low(); // Fan in Auto Mode
								}
							}
							else if(serverTemp >= avgTemp && compressorType == 0)
							{
								myLED[0].high(); //Cooler GPIO 4 pin 16
								myLED[1].high();
								if(fanStat == 0)
								{
									myLED[2].high(); // Fan in Auto Mode
								}
							}
							else if(serverTemp > avgTemp && compressorType == 1)
							{
								myLED[0].high();
								myLED[1].low(); //Heater GPIO 5 pin 18
								if(fanStat == 0)
								{
									myLED[2].low(); // Fan in Auto Mode
								}
								 
							}
							else if(serverTemp <= avgTemp && compressorType == 1)
							{
								myLED[0].high();
								myLED[1].high();  //Heater GPIO 5 pin 18
								if(fanStat == 0)
								{
									myLED[2].high(); // Fan in Auto Mode
								}
							}
							else
							{
								myLED[0].high();
								myLED[1].high();
							}
						}
						else if(sysOff == 1)
						{
							myLED[0].high();
							myLED[1].high();
							if(fanStat == 0)
							{
								myLED[2].high(); // Fan in Auto Mode
							}
						}
						else
						{
							/* Do Nothing */
						}
					}
					else
					{
						/* Turn Off the Entire System if Sensor goes Faulty */
						myLED[0].high();
						myLED[1].high();
						myLED[2].high();
					}
				}

			}
			catch(Exception e)
			{
				myLED[0].high();
				myLED[1].high();
				myLED[2].high();
				gpio.shutdown();
				e.printStackTrace();
			}


		}

	}

class SensorHandler implements Runnable{
  
	int flag = 1, srvrTemp = 0, srvrTempPrev = 0;
	
	public int readTemp = 0;
	FileInputStream tempSnsr1 = null;
	FileInputStream tempSnsr2 = null;
	FileInputStream readMac = null;

	BufferedReader tempSnsrRdr1 = null;
	BufferedReader tempSnsrRdr2 = null;
	BufferedReader buffReadMac = null;


	int tempValSnsr1 = 0, tempValSnsrPrev1 = 0, sampleSnsr1 = 0;
	int tempValSnsr2 = 0, tempValSnsrPrev2 = 0, sampleSnsr2 = 0;

	String snsr1 = "YES";
	String snsr2 = "YES";


	String fileNameSensr1 = "/sys/bus/w1/devices/28-0000055d9e95/w1_slave";
	String fileNameSensr2 = "/sys/bus/w1/devices/28-0000055d9859/w1_slave";
	String fileNameMAC = "/sys/class/net/eth0/address";
	
@Override
public void run() {
	{
		String SrvrMsg, MACAddr,line1 = "";

		try{
			readMac = new FileInputStream(fileNameMAC);
			buffReadMac = new BufferedReader(new InputStreamReader(readMac));
			MACAddr = buffReadMac.readLine();
			System.out.println("MAC Addr = " + MACAddr);

			Socket s = new Socket("time-c.nist.gov",13);
			InputStream time = s.getInputStream();
			Scanner in = new Scanner(time);
			while(in.hasNextLine())
			{
				line1 = in.nextLine();
				System.out.println(line1);
			}
			Calendar calendar = Calendar.getInstance();
			int day = calendar.get(Calendar.DAY_OF_WEEK);
			
			System.out.println(day);
			while(true)
			{
				tempSnsr1 = new FileInputStream(fileNameSensr1);
				tempSnsr2 = new FileInputStream(fileNameSensr2);

				tempSnsrRdr1 = new BufferedReader(new InputStreamReader(tempSnsr1));
				tempSnsrRdr2 = new BufferedReader(new InputStreamReader(tempSnsr2));

				String line = tempSnsrRdr1.readLine();
				String line2 = tempSnsrRdr2.readLine();

				snsr1 = line.substring(36,39);
				snsr2 = line2.substring(36,39);

				if(snsr1.equals("YES") && snsr2.equals("YES"))
				{
					snsrFault = 0;

					line = tempSnsrRdr1.readLine();
					line2 = tempSnsrRdr2.readLine();

					String file_read_temp1[] = line.split("=");
					String file_read_temp2[] = line2.split("=");

					tempValSnsr1 = (Integer.parseInt(file_read_temp1[1]))/1000;
					tempValSnsr2 = (Integer.parseInt(file_read_temp2[1]))/1000;
					
					tempValSnsr1 = ((tempValSnsr1*9)/5)+32;
					avgTemp = tempValSnsr1; //Tentative Temperature until the Avg. is calculated

					tempValSnsr2 = ((tempValSnsr2*9)/5)+32;

					if(tempValSnsr1 == tempValSnsrPrev1)
					{
						if(sampleSnsr1 < SAMPLE_COUNT)
						{
							sampleSnsr1++;
							tempValSnsrPrev1 = tempValSnsr1;
						}
						else if (sampleSnsr1 == SAMPLE_COUNT)
						{
							System.out.println("Sensor 1 temperature in F = " + tempValSnsr1);
							sampleSnsr1++;
						}
					}
					else
					{
						sampleSnsr1 = 0;
						tempValSnsrPrev1 = tempValSnsr1;
						flag = 1;
					}

					if(tempValSnsr2 == tempValSnsrPrev2)
					{
						if(sampleSnsr2 < SAMPLE_COUNT)
						{
							sampleSnsr2++;
							tempValSnsrPrev2 = tempValSnsr2;
						}
						else if (sampleSnsr2 == SAMPLE_COUNT)
						{
							System.out.println("Sensor 2 temperature in F = " + tempValSnsr2);
							sampleSnsr2++;
						}
					}
					else
					{
						sampleSnsr2 = 0;
						tempValSnsrPrev2 = tempValSnsr2;
						flag = 1;
					}

					if(sampleSnsr1 > SAMPLE_COUNT && sampleSnsr2 > SAMPLE_COUNT && flag == 1)
					{
						avgTemp = (tempValSnsr1 + tempValSnsr2)/2;
						System.out.println("Average Temp in F = " + avgTemp);
						flag = 0;
						if(prevAvgTemp != avgTemp)
						{
							prevAvgTemp = avgTemp;
							//w.write("PItoS,1," + avgTemp + "\n");
							//w.flush();
						}
					}
		
					if(readTemp == 1)
					{
						if(sysOff == 1)
						{
							//w.write("PItoS,0,N/A \n");
							//w.flush();
						}
						else
						{
							//w.write("PItoS,1," + avgTemp + "\n");
							//w.flush();
						}
						readTemp = 0;
					}
				}
				else
				{
					System.out.println("Sensor Fault");
					snsrFault = 1;
					break;
				}

				tempSnsrRdr1.close();
				tempSnsr1.close();
				tempSnsrRdr2.close();
				tempSnsr2.close();
				Thread.sleep(5);
			}

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}


	}

}
}
}

