/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.microfaas.java.wsaccess.client.bench;

import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLException;
import net.microfaas.java.wsaccess.client.WebSocketClient;

/**
 *
 * @author christophe
 */
public class MainWebSocketClient {

	private static final int DEFAULT_NB_CLIENTS=10;
	public static AtomicLong nbFrame = new AtomicLong(0);
	public static AtomicLong start = new AtomicLong(Long.MAX_VALUE);
	public static AtomicLong end = new AtomicLong(Long.MIN_VALUE);
	public static CountDownLatch startSemaphore;
	public static CountDownLatch endSemaphore;

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws URISyntaxException, SSLException, InterruptedException {
		int nb = DEFAULT_NB_CLIENTS;
		try {
			nb = Integer.parseInt(args[0]);
		} catch (NumberFormatException ex) {
			nb = DEFAULT_NB_CLIENTS;
		}
		startSemaphore = new CountDownLatch(nb); 
		endSemaphore = new CountDownLatch(nb); 
		for (int i = 0; i < nb; i++) {
			WebSocketClient webSocketClient = new WebSocketClient();
			webSocketClient.start();
			Thread.sleep(10);
			System.out.println("countDownLatch: "+startSemaphore.getCount());
		}
		endSemaphore.await();
		System.out.println("start "+start.get()+", end "+end.get());
		long time = end.get()-start.get();
		System.out.println("nb: "+nbFrame.get()+", time: "+time+" mess/sec: "+nbFrame.get()/(time/1000));
	}

}
