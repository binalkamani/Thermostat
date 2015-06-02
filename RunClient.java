package Client;

import java.io.IOException;
import java.net.Socket;

public class RunClient {

	public static void main(String[] args) {
        // Make sure command line arguments are valid

		Process p;
		try {
			p = Runtime.getRuntime().exec("sudo modprobe w1-gpio");
			p.waitFor();
			p = Runtime.getRuntime().exec("sudo modprobe w1-therm");
			p.waitFor();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        String hostname;
        int port;
        try {
            hostname = args[0];
            port = Integer.parseInt(args[1]);
          
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException ex) {
            System.err.println("Error: Please specify host name, port number and data file.\nExiting...");
            return;
        }

           Client c = null;
		try {
			c = new Client(new Socket(hostname, port));
			c.send();
			c.recv();
	        c.close();
	        
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			// now start the server socket which listens to server when it ping's 	
			MultiThreadedListener listener = new MultiThreadedListener(9000);
			new Thread(listener).start();

			try {
			    Thread.sleep(20 * 100000);
			} catch (InterruptedException e) {
			    e.printStackTrace();
			}
			
			listener.stop();
    }
}
