package com.bupt.poirot.jettyServer.jetty;

import com.bupt.poirot.utils.Config;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class SocketSendData {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(System.getProperty("os.name"));
		try (
				ServerSocket serverSocket = new ServerSocket(30000);
				){
//			BufferedWriter bufferedWriter = new BufferedWriter(s.getInputStream());
			while (true) {
				System.out.println("begin listening");
				Socket s = serverSocket.accept();
				String os = System.getProperty("os.name");
				System.out.println(os);
				String datafile = "";
				
				if (os.contains("Mac")) {
					datafile = Config.getString("mac");
				} else {
					datafile = Config.getString("win");
				}
				System.out.println(datafile);
                File file = new File(datafile);
         
				new Thread(new ThreadSolve(s, file)).start();
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block0
 			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

}
