/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.microfaas.java.wsaccess.client;

import net.microfaas.java.wsaccess.client.data.DataExample1;
import net.microfaas.java.wsaccess.client.data.DataConnect;
import com.google.gson.Gson;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;
import java.util.UUID;
import java.util.logging.Level;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import static net.microfaas.java.wsaccess.client.bench.MainWebSocketClient.end;
import static net.microfaas.java.wsaccess.client.bench.MainWebSocketClient.endSemaphore;
import static net.microfaas.java.wsaccess.client.bench.MainWebSocketClient.nbFrame;
import static net.microfaas.java.wsaccess.client.bench.MainWebSocketClient.start;
import static net.microfaas.java.wsaccess.client.bench.MainWebSocketClient.startSemaphore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author christophe
 */
public class WebSocketClient extends Thread {

	static final String URL = System.getProperty("url", "ws://127.0.0.1:8080/websocket");
	static final String URL_SSL = System.getProperty("url", "wss://127.0.0.1:8443/websocket");
	static final boolean SSL = System.getProperty("ssl") != null;
	static final boolean CLIENT_SSL = System.getProperty("client.ssl") != null;
	private final URI uri;
	private final SslContext sslCtx;
	private SslContext clientSslCtx = null;
	private final String scheme;
	private final String deviceId = UUID.randomUUID().toString();
	private static final Logger logger = LogManager.getLogger(WebSocketClient.class);

	public WebSocketClient() throws URISyntaxException, SSLException {
		uri = new URI(SSL ? URL_SSL : URL);
		scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
		final boolean ssl = "wss".equalsIgnoreCase(scheme);
		if (ssl) {
			sslCtx = SslContextBuilder.forClient()
					.trustManager(InsecureTrustManagerFactory.INSTANCE).build();
		} else {
			sslCtx = null;
		}
	}

	@Override
	public void run() {
		final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();

		final int port;
		if (uri.getPort() == -1) {
			if ("ws".equalsIgnoreCase(scheme)) {
				port = 80;
			} else if ("wss".equalsIgnoreCase(scheme)) {
				port = 443;
			} else {
				port = -1;
			}
		} else {
			port = uri.getPort();
		}
		setClientSsl();
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			// Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
			// If you change it to V00, ping is not supported and remember to change
			// HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
			final WebSocketClientHandler handler
					= new WebSocketClientHandler(
							WebSocketClientHandshakerFactory.newHandshaker(
									uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()));

			Bootstrap b = new Bootstrap();
			b.group(group)
					.channel(NioSocketChannel.class)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) {
							ChannelPipeline p = ch.pipeline();
							if (clientSslCtx != null) {
								p.addLast(clientSslCtx.newHandler(ch.alloc()));
							}
							if (sslCtx != null) {
								p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
							}
							p.addLast(
									new HttpClientCodec(),
									new HttpObjectAggregator(8192),
									WebSocketClientCompressionHandler.INSTANCE,
									handler);
						}
					});
			Channel ch = b.connect(uri.getHost(), port).sync().channel();
			handler.handshakeFuture().sync();

			WebSocketFrame frame = new TextWebSocketFrame(
					new Gson().toJson(
							new DataConnect(deviceId,
									"MODEL::" + UUID.randomUUID().toString(),
									"VERSION::" + UUID.randomUUID().toString(),
									System.currentTimeMillis())));
			ch.writeAndFlush(frame);
			if (startSemaphore != null) {
				startSemaphore.countDown();
				//Thread.sleep(this.delayInMillis);
				startSemaphore.await();
			}
			int i = 0;
			long l = System.currentTimeMillis();
			System.out.println(new Date() + " start to serve: " + l);
			if (l < start.get()) {
				start.set(l);
			}
			while (i < 10000) {
				String msg = new Gson().toJson(new DataExample1(System.currentTimeMillis(), i));
				frame = new TextWebSocketFrame(msg);
				ch.writeAndFlush(frame);
				i++;
				//Thread.sleep(1000);
			}
			long e = System.currentTimeMillis();
			if (e > end.get()) {
				end.set(e);
			}
			nbFrame.addAndGet(i);
			//time.addAndGet(System.currentTimeMillis()-l);
			System.out.println(new Date() + " Time to serve: " + (System.currentTimeMillis() - l) + ", nb: " + i);
			if (endSemaphore != null) {
				endSemaphore.countDown();
			}
			ch.close();
		} catch (InterruptedException ex) {
			logger.error(ex);
		} finally {
			group.shutdownGracefully();
		}
	}

	private void setClientSsl() {
		if (!CLIENT_SSL) {
			return;
		}
		try {
			TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager() {
					@Override
					public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}

					@Override
					public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
					}

					@Override
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}
				}
			};

			final KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
			File file = new File("device_1.p12");
			InputStream ksIs = new FileInputStream(file);
			clientKeyStore.load(ksIs, "microfaas.net".toCharArray());
			final KeyManagerFactory clientKeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			clientKeyManagerFactory.init(clientKeyStore, "microfaas.net".toCharArray());

			logger.debug("client key default algo: {}", clientKeyManagerFactory.getAlgorithm());
			clientSslCtx = SslContextBuilder.forClient()
					.sslProvider(SslProvider.JDK)
					.keyManager(clientKeyManagerFactory)
					.trustManager(new File("keystore.pem"))
					.ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
					//.sessionCacheSize(0)
					//.sessionTimeout(0)
					.build();
		} catch (UnrecoverableEntryException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
			java.util.logging.Logger.getLogger(WebSocketClient.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
