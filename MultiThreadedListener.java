package Client;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.net.*;
import com.pi4j.wiringpi.*;
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
    public int AutoMode = 0; // 0 for Off, 1 for On
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
    	String currLine = "",lastLine = "";

		Calendar calendar = Calendar.getInstance();
		int day = calendar.get(Calendar.DAY_OF_WEEK);
        synchronized(this)
        {
            this.runningThread = Thread.currentThread();
        }
        try
        {
        	File toSend = new File("/home/pi/Zspark/AutoMode.txt");
            if (!toSend.exists())
    		{
    	        BufferedWriter StatWrite = new BufferedWriter(new FileWriter(toSend));
    			StatWrite.write("\n");
    			StatWrite.close();
    			AutoMode = 0;
    		}
            else
            {
            	BufferedReader br=new BufferedReader(new FileReader(new File("/home/pi/Zspark/AutoMode.txt")));
            	while ((currLine = br.readLine()) != null)
                {
                	lastLine = currLine;
                }
            	br.close();
            	if(lastLine.startsWith("StoPi"))
            	{
            		String mode_param[] = lastLine.split(",");
                	if(mode_param[1].equals("1"))
                	{
                		String Days[] = mode_param[6].split(":");
                		int days = Days.length;
                		for ( int i = 0; i< days; i++)
                		{
                			if(day == Integer.parseInt(Days[i]))
                			{
                				AutoMode = 1;
                				break;
                			}
                			AutoMode = 0;
                		}

                	}
            	}
            }
        }
        catch(Exception e)
        {
        	System.out.println("Unable to open the file");
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
            	  AutoMode = 0;
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

			int serverTemp = 0, slot = 0,prevslot = 4, offset = 5;
			int compressorType = 2; // 0: Cool, 1: Heat, 2: None
			String lastLine = "";
			String currLine = "";
			int Automodechange = 0;

			File toSend = new File("/home/pi/Zspark/AutoMode.txt");
			String AC_State[] = null;
			String Fan_State[]= null;
			String Sys_State[]= null;
			String Temp_State[]= null;
			String Days[]= null;
			String tokens[]= null;
			String time[],internettime = "";

			Calendar calendar = Calendar.getInstance();
			int day;
			int prev_day = 0;
			Socket s = null;

			try{
				while(true)
				{
					day = calendar.get(Calendar.DAY_OF_WEEK);
					String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
					//System.out.println("System Time"+timeStamp );
								        	
		        	time = timeStamp.split(":");
		        	
		        	int hour = Integer.parseInt(time[0]);

		        	if(hour < offset)
		        	{
		        		hour = hour + 24;
		        		day = day - 1;
		        	} 
		        	hour = hour - offset; // local time
		        	
					if(snsrFault == 0)
					{
						if((SrvrMsg.startsWith("StoPi")))
						{
							//SrvrMsg = (String)scanner.nextLine();
							//System.out.println("Message from server: " + SrvrMsg);

							tokens = SrvrMsg.split(",");

							// Msg Format: StoPi,Heat,FanAuto,1,76
							//StoPi,1,Heat:Heat:Cool:Heat,FanAuto:FanMan:FanAuto:FanAuto,1:1:1:0,65:66:72:63,1:2:4:7
							//Last field above, Sunday = 1... Saturday = 7
							if(tokens[0].compareTo("StoPi")==0 && ((tokens[1].compareTo("1")!=0)&&(tokens[1].compareTo("0")!=0)))
							{
								AutoMode = 0;

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
							else if(tokens[0].compareTo("StoPi")==0 && ((tokens[1].compareTo("1")==0)||(tokens[1].compareTo("0")==0)))
							{
								BufferedWriter StatWrite = new BufferedWriter(new FileWriter(toSend));
								StatWrite.write("\n");
								StatWrite.close();

								BufferedWriter wr = new BufferedWriter(new FileWriter(toSend));
								wr.newLine();
				            	wr.write(SrvrMsg);
				            	wr.newLine();
				            	wr.flush();
				            	wr.close();
				            	Automodechange = 1;
							}

							SrvrMsg ="";
						}

						if(day!=prev_day || Automodechange == 1)
						{
							System.out.println("Auto mode changed " + Automodechange);
							prev_day = day;
							Automodechange = 0;
							BufferedReader br=new BufferedReader(new FileReader(new File("/home/pi/Zspark/AutoMode.txt")));
				        	while ((currLine = br.readLine()) != null)
				            {
				            	lastLine = currLine;
				            }
				        	br.close();
				        	if(lastLine.startsWith("StoPi"))
				        	{
				        		String mode_param[] = lastLine.split(",");
					        	if(mode_param[1].equals("1"))
					        	{
					        		AC_State = mode_param[2].split(":");
									Fan_State = mode_param[3].split(":");
									Sys_State = mode_param[4].split(":");
									Temp_State = mode_param[5].split(":");
					        		Days = mode_param[6].split(":");
					        		int days = Days.length;
					        		for ( int i = 0; i< days; i++)
					        		{
					        			if(day == Integer.parseInt(Days[i]))
					        			{
					        				AutoMode = 1;
					        				break;
					        			}
					        			AutoMode = 0;
					        		}

					        	}
					        	else
					        	{
					        		AutoMode = 0;
					        	}					        	
				        	}
				        	System.out.println("Auto mode stat:  " + AutoMode);
				        	Automodechange = 0;	
				        	prevslot = 4;
						}

						if(AutoMode == 1)
						{
						/*	try
							{
								s = new Socket("time-c.nist.gov", 13);
							}
							catch(Exception e)
							{
								System.out.println("Cannot Connect to time server");
							}
							InputStream inStream = s.getInputStream();
				        	Scanner in = new Scanner(inStream);

				        	while (in.hasNextLine())
				        	{
				        		internettime = in.nextLine();
				            	System.out.println(internettime);
				        	}

				        	time = internettime.split(" ");				        	
				        	time = time[2].split(":");
				        	int hour = Integer.parseInt(time[0]);

				        	if(hour < offset)
				        	{
				        		hour = hour + 24;
				        	} */
				        	//hour = hour - offset; // local time
				        	

				        	if(hour >= 00 && hour < 06 )
				        	{
				        		slot = 0;
				        	}
				        	else if(hour >= 06 && hour < 12)
				        	{
				        		slot = 1;
				        	}
				        	else if(hour >= 13 && hour < 18)
				        	{
				        		slot = 2;
				        	}
				        	else
				        	{
				        		slot = 3;
				        	}

				        	if(prevslot!=slot)
				        	{
				        		prevslot = slot;

				        		if(Sys_State[slot].compareTo("0")==0)
								{
									System.out.println("Turning off the AC system");

									sysOff = 1;

								}
								else if (Sys_State[slot].compareTo("1")==0)
								{
									System.out.println("Temperature to be set in  Auto Mode (F) = " + Temp_State[slot]);
									serverTemp = Integer.parseInt(Temp_State[slot]);
									sysOff = 0;

									if(AC_State[slot].compareTo("Cool")==0)
									{
										compressorType = 0;
									}
									else if(AC_State[slot].compareTo("Heat")==0)
									{
										compressorType = 1;
									}
									else
									{
										compressorType = 2;
									}
								}
								else
								{
									/* Do Nothing */
								}

								/* Fan Status to be checked regardless of System Off/On */
								if(Fan_State[slot].compareTo("FanAuto")==0)
								{
									//Turn Fan Off in Auto Mode
									fanStat = 0;
								}
								else if(Fan_State[slot].compareTo("FanMan")==0)
								{
									myLED[2].low(); //Turn Fan On in Manual Mode
									fanStat = 1; //Manual
								}
								else
								{
									/* Do Nothing */
								}

				        	}
				        	//have a previous slot to find if the slot has changed , update variables.
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
		String SrvrMsg, MACAddr;

		try{
			readMac = new FileInputStream(fileNameMAC);
			buffReadMac = new BufferedReader(new InputStreamReader(readMac));
			MACAddr = buffReadMac.readLine();
			System.out.println("MAC Addr = " + MACAddr);

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

