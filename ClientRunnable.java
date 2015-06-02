package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

/**

 */
public class ClientRunnable implements Runnable{

    protected Socket clientSocket = null;
    protected RaspiObject rs   ;

    public ClientRunnable(Socket clientSocket, RaspiObject obj) {
        this.clientSocket = clientSocket;
        this.rs   = obj;
    }
 // convert InputStream to String
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

 	public void run() {
        new Thread(
				new GetDetails(rs,clientSocket)
		        ).start();
    }
    


  
}
