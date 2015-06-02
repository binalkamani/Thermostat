/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 *
 * @author ashkany
 */
public class AshServer {

    public static void main(String args[]) throws Exception{

        AshServer server = new AshServer();
	
        server.console();
	
	
	
    }

    private void console() {
	
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    //Create a server socket at port 9393
                    
                    //Server goes into a permanent loop accepting connections from clients
                    String recString;
			ServerSocket serverSock = new ServerSocket(9393);
			Socket sock = serverSock.accept();
        		PrintWriter printWriter = new PrintWriter(sock.getOutputStream());
                        
                        printWriter.write("StoPI,1,65\r\n");
                        printWriter.flush();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			Thread t1 = new Thread(new RequestClient(printWriter));
			t1.start();
                    while (true) {
                        
                        recString = reader.readLine();
			if(recString.equals(""))
			{
			}
			else
			{	
                        	System.out.println(recString);
			}
			printWriter.write("\n");
                        printWriter.flush();
                    }

                } catch (IOException ex) {
                    System.out.println("Cannot make server socket!");
                    System.exit(1);
                }

            }
        };
        thread.start();

        
        while (true) {
//            try {
//                
//            } catch (IOException ex) {
//                System.out.println("Cannot read from console!");
//            }
            
        }
    }

    class RequestClient implements Runnable
    {
		PrintWriter p;
		public RequestClient(PrintWriter printWriter)
		{
			try
			{
				p = printWriter;

			}catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	public void run()
	{
		try
		{	
			int csDelay, temp;
			while(true)
			{
			
				csDelay = 9000 + (int)(Math.random() * ((500-200)+1));
				temp = 60 + (int)(Math.random() * ((20-1)+1));
				p.write("StoPI,1," + temp + "\n");
				p.flush();
				Thread.sleep(csDelay);
				Thread.sleep(csDelay);
				p.write("StoPI,0," + temp + "\n");
				p.flush();
				Thread.sleep(csDelay);
				Thread.sleep(csDelay);
	                        
			}
		}
		catch(Exception e)
		{
			System.out.println("Error");
		}	
	}
    }
}

