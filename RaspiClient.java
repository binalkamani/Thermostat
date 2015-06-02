package Client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;

public class RaspiClient {
private static String id ="raspi001";
	public static void main(String argv[]){
		try {
			Socket client=new Socket("localhost", 8080);
			if(client.isConnected()){
			OutputStream out = client.getOutputStream();
			out.write(("Zspark /id="+id).getBytes(Charset.forName("UTF-8")));
			out.flush();
			Thread.sleep(100);
			InputStream in = client.getInputStream();
			DataInputStream dataInputStream = new DataInputStream(in);

            int attempts = 0;
            while(dataInputStream.available() == 0 && attempts < 1000)
            {
                attempts++;
                Thread.sleep(10);
            }
            
        	 // String input= getStringFromInputStream(dataInputStream);
        	  System.out.println("InputStream: "+ in);
			client.close();
			}
		} catch (IOException| InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
//	private static String getStringFromInputStream(InputStream is) {
//		   
//  		BufferedReader br = null;
//  		StringBuilder sb = new StringBuilder();
//   
//  		String line;
//  		try {
//   
//  			br = new BufferedReader(new InputStreamReader(is));
//  			while ((line = br.readLine()) != null) {
//  				sb.append(line);
//  			}
//   
//  		} catch (IOException e) {
//  			e.printStackTrace();
//  		} 
//   
//  		return sb.toString();
//   
//  	}

}
