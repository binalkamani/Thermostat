import java.io.*;
import java.util.*;
import java.net.*;
import com.pi4j.io.gpio.*;
import java.io.IOException;

public class thermostat
{
	public static final int SAMPLE_COUNT = 5;

	public static int avgTemp = 0, prevAvgTemp = 0;

	public PrintWriter w;

	public int sysOff = 0, snsrFault = 0;
	
	public int readTemp = 0;

	//Socket s = new Socket("time-A.timefreq.bldrdoc.gov", 37);
	
	Socket s = new Socket("time.nist.gov", 13);
	
	public Socket clisock = new Socket("acngroup7.utdallas.edu", 9393);

	//public Socket clisock = new Socket("localhost", 9393);

	public thermostat(String args[]) throws IOException,InterruptedException
	{
		Process p = Runtime.getRuntime().exec("sudo modprobe w1-gpio");
		p.waitFor();
		p = Runtime.getRuntime().exec("sudo modprobe w1-therm");
		p.waitFor();

		System.out.println("Connected to Server "+ clisock.getInetAddress()+ ":"+ clisock.getPort());
		System.out.println("local port is " + clisock.getLocalPort());
		
		/*InputStream inStream = s.getInputStream();
        	Scanner in = new Scanner(inStream);

        	while (in.hasNextLine())
		{
			String line = in.nextLine();
            		System.out.println(line);
        	}*/
		
		w = new PrintWriter(clisock.getOutputStream(),true);

		Thread t1 = new Thread(new SensorHandler());
		Thread t2 = new Thread(new ServerHandler(clisock));
		t1.start();
		t2.start();
		
		while(w.checkError())
					{
						try
						{
							clisock = new Socket("acngroup7.utdallas.edu", 9393);
							//sock = new Socket("localhost", 9393);
							w = new PrintWriter(clisock.getOutputStream(),true);
							System.out.println("connected to Server...");
						}
						catch(SocketTimeoutException ex)
						{
							System.out.println("Trying to connect to Server...");
						}
						catch(IOException ex)
						{
							System.out.println("Trying to connect to Server...");
						}
			
					}
	}


	class ServerHandler implements Runnable
	{

		//BufferedReader reader;
		Socket sock;
		InputStream Input;
		Scanner scanner;
		final GpioController gpio = GpioFactory.getInstance();

		GpioPinDigitalOutput myLED[]={
		gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "LED #1", PinState.HIGH),
		gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "LED #2", PinState.HIGH),
		gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "LED #3", PinState.HIGH)
		};

		public ServerHandler(Socket s)
		{
			try
			{
				sock = s;
				//InputStreamReader iReader = new InputStreamReader(sock.getInputStream());
				Input = sock.getInputStream();
				scanner = new Scanner(Input);
				//reader = new BufferedReader(iReader);

			}catch(Exception e)
			{
				e.printStackTrace();
			}
		}

		public void run()
		{
			String SrvrMsg;
			int serverTemp = 0;
			
			try{
				while(true)
				{
						
						if(scanner.hasNextLine())
						{
							SrvrMsg = (String)scanner.nextLine();
							System.out.println("Message from server" + SrvrMsg);
							
							String tokens[] = SrvrMsg.split(",");

							if(tokens[0].equals("StoPI"))
							{
								if(tokens[1].equals("0"))
								{
									System.out.println("Turning off the AC system");
									sysOff = 1;
								}
								else if (tokens[1].equals("1"))
								{
									System.out.println("Temperature received from Server in F = " + tokens[2]);
									serverTemp = Integer.parseInt(tokens[2]);
									sysOff = 0;
								}
								else if (tokens[1].equals("2"))
								{
									readTemp = 1;
								}
							}
						}
						
						//SrvrMsg = reader.readLine();
						

						if(avgTemp != 0 && sysOff == 0 && snsrFault == 0){
							if(avgTemp > serverTemp && avgTemp > 0)
							{
								myLED[0].low(); //Cooler GPIO 4 pin 16
								myLED[1].high();
								myLED[2].high();
							}
							else if(avgTemp < serverTemp && avgTemp > 0)
							{
								myLED[0].high();
								myLED[1].low(); //Heater GPIO 5 pin 18
								myLED[2].high();
							}
							else if(avgTemp == serverTemp && avgTemp > 0)
							{
								myLED[0].high();
								myLED[1].high();
								myLED[2].low(); //Fan only GPIO 6 pin 22
							}
							else
							{
								myLED[0].high();
								myLED[1].high();
								myLED[2].high();
							}
						}
						else if(sysOff == 1 || snsrFault == 1)
						{
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

	class SensorHandler implements Runnable
	{
		int flag = 1, srvrTemp = 0, srvrTempPrev = 0;

		FileInputStream tempSnsr1 = null;
		FileInputStream tempSnsr2 = null;

		BufferedReader tempSnsrRdr1 = null;
		BufferedReader tempSnsrRdr2 = null;



		int tempValSnsr1 = 0, tempValSnsrPrev1 = 0, sampleSnsr1 = 0;
		int tempValSnsr2 = 0, tempValSnsrPrev2 = 0, sampleSnsr2 = 0;

		String snsr1 = "YES";
		String snsr2 = "YES";


		String fileNameSensr1 = "/sys/bus/w1/devices/28-0000055d9e95/w1_slave";
		String fileNameSensr2 = "/sys/bus/w1/devices/28-0000055d9859/w1_slave";


		public void run()
		{
			String SrvrMsg;

			try{
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
						line = tempSnsrRdr1.readLine();
						line2 = tempSnsrRdr2.readLine();

						String file_read_temp1[] = line.split("=");
						String file_read_temp2[] = line2.split("=");

						tempValSnsr1 = (Integer.parseInt(file_read_temp1[1]))/1000;
						tempValSnsr2 = (Integer.parseInt(file_read_temp2[1]))/1000;
						tempValSnsr1 = ((tempValSnsr1*9)/5)+32;
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
							w.write("PItoS,1," + avgTemp + "\n");
							w.flush();
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
	public static void main(String args[]) throws InterruptedException, IOException
	{
		new thermostat(args);
	}
}
