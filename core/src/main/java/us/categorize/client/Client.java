package us.categorize.client;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;

public class Client {

	private HttpClient client;
	private String serverBase; 
	
	public Client(String serverBase) {
		client = new HttpClient();
		this.serverBase = serverBase;
		try {
			client.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void stop() throws Exception {
		client.stop();
	}
	public void readMessage(long id) throws InterruptedException, ExecutionException, TimeoutException {
		ContentResponse response = client.GET(serverBase+"/msg/"+id);
		System.out.println(response.getContentAsString());
	}
	
	
	
	public static void main(String args[]) throws Exception {
		Client client = new Client("http://localhost:8080");
		client.readMessage(1);
		client.stop();
	}
}
