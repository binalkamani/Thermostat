package Client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

public class run {

	public static void main(String[] args) {
		Socket client;
		try {
			client = new Socket("localhost", 8080);
			if(client.isConnected()){
				OutputStream out = client.getOutputStream();
				out.write(("GET login/user=teja;pass=pass").getBytes(Charset.forName("UTF-8")));
				out.flush();
				DataInputStream dataInputStream = new DataInputStream(client.getInputStream());

	            int attempts = 0;
	            while(dataInputStream.available() == 0 && attempts < 1000)
	            {
	                attempts++;
	               
	            }
	            
	        	  String in= getStringFromInputStream(dataInputStream);
	        	  System.out.println("GET reply."+in) ;
		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
}