package Client;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.net.Socket;

public class Client{
    private Socket socket;
    private String id = "raspi001";

    public Client(String host, int port) throws IOException {
    	 this.socket= new Socket(host, port);
    }

    public Client(Socket s) {
        this.socket = s;
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

    public void send() throws IOException {
    	this.socket.getOutputStream().write(("Zspark /id="+id).getBytes(Charset.forName("UTF-8")));
    	this.socket.shutdownOutput();
    	}

    public void recv() {
        try {
        	 
        	DataInputStream dataInputStream = new DataInputStream(this.socket.getInputStream());

            int attempts = 0;
            while(dataInputStream.available() == 0 && attempts < 1000)
            {
                attempts++;
                Thread.sleep(10);
            }
            
        	  String in= getStringFromInputStream(dataInputStream);
        	  System.out.println("InputStream: "+ in);
        } catch (IOException | InterruptedException ex) {
            System.err.println("Error: Unable to read server response\n\t" + ex);
        }        
    }

	public void close() {
		// TODO Auto-generated method stub
		try {
			this.socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    

}