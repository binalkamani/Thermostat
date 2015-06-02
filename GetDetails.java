package Client;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;

public class GetDetails implements Runnable{

	protected RaspiObject request;
	protected Socket clientSocket;
	public GetDetails(RaspiObject req, Socket cSocket) {
		// TODO Auto-generated constructor stub
		request = req;
		clientSocket = cSocket;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		OutputStream output;
		try {
			output = clientSocket.getOutputStream();
			RaspiObject result=request;
			 System.out.println("\n Raspi temp: "+result.currentTemp+"\tfan"+result.fan);
			 ObjectOutputStream oos = new ObjectOutputStream(output);  
			 oos.writeObject(result);
			clientSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

//	private RaspiObject get() {
//		// TODO Auto-generated method stub
//		RaspiObject rs = new RaspiObject(40,50,1,1);
//	
//		return rs;
//	}

}
