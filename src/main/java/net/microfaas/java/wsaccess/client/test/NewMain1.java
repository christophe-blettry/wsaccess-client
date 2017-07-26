/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.microfaas.java.wsaccess.client.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.ssl.SslProvider;
import io.netty.util.NetUtil;
import io.netty.util.internal.PlatformDependent;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.Provider;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import org.apache.logging.log4j.LogManager;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author christophe
 */
public class NewMain1 {

	protected Throwable serverException;
	protected Throwable clientException;
	protected Channel serverChannel;
	protected Channel serverConnectedChannel;
	protected Channel clientChannel;
	protected CountDownLatch serverLatch;
	protected CountDownLatch clientLatch;
	protected MessageReceiver serverReceiver;
	protected MessageReceiver clientReceiver;
	protected SslContext serverSslCtx;
	protected SslContext clientSslCtx;
	protected ServerBootstrap sb;
	protected Bootstrap cb;
	private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger(NewMain1.class);

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		new NewMain1(BufferType.Direct);
	}

	enum BufferType {
		Direct,
		Heap,
		Mixed
	}
	private final BufferType type;

	protected NewMain1(BufferType type) {
		this.type = type;
		serverLatch = new CountDownLatch(1);
		clientLatch = new CountDownLatch(1);
		try {
			testMutualAuthInvalidClientCertSucceed(ClientAuth.NONE);
		} catch (Exception ex) {
			Logger.getLogger(NewMain1.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private SslProvider sslClientProvider() {
		return SslProvider.JDK;
	}

	private SslProvider sslServerProvider() {
		return SslProvider.JDK;

	}

	protected Provider clientSslContextProvider() {
		return null;
	}

	protected Provider serverSslContextProvider() {
		return null;
	}

	protected void mySetupMutualAuthServerInitSslHandler(SslHandler handler) {
	}

	public final void testMutualAuthDiffCertsClientFailure(ClientAuth auth) throws Exception {
		final KeyStore serverKeyStore = KeyStore.getInstance("PKCS12");
		serverKeyStore.load(new FileInputStream("keystore.p12"), "microfaas.net".toCharArray());
		final KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
		clientKeyStore.load(new FileInputStream("device_1.p12"), "microfaas.net".toCharArray());
		final KeyManagerFactory serverKeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		serverKeyManagerFactory.init(serverKeyStore, "microfaas.net".toCharArray());
		final KeyManagerFactory clientKeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		clientKeyManagerFactory.init(clientKeyStore, "microfaas.net".toCharArray());
		File commonCertChain = new File("keystore.pem");
		serverLatch = new CountDownLatch(1);
		clientLatch = new CountDownLatch(1);

		mySetupMutualAuth(serverKeyManagerFactory, commonCertChain, clientKeyManagerFactory, commonCertChain,
				auth, false, false);
		assertTrue(clientLatch.await(5, TimeUnit.SECONDS));
		assertTrue(clientException instanceof SSLHandshakeException);
		assertTrue(serverLatch.await(5, TimeUnit.SECONDS));
		assertTrue(serverException instanceof SSLHandshakeException);
	}

	private void testMutualAuthInvalidClientCertSucceed(ClientAuth auth) throws Exception {
		final KeyStore serverKeyStore = KeyStore.getInstance("PKCS12");
		serverKeyStore.load(new FileInputStream("keystore.p12"), "microfaas.net".toCharArray());
		final KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");
		clientKeyStore.load(new FileInputStream("device_1.p12"), "microfaas.net".toCharArray());
		final KeyManagerFactory serverKeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		serverKeyManagerFactory.init(serverKeyStore, "microfaas.net".toCharArray());
		final KeyManagerFactory clientKeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		clientKeyManagerFactory.init(clientKeyStore, "microfaas.net".toCharArray());
		File commonCertChain = new File("keystore.pem");

		mySetupMutualAuth(serverKeyManagerFactory, commonCertChain, clientKeyManagerFactory, commonCertChain,
				auth, false, false);
		assertTrue(clientLatch.await(5, TimeUnit.SECONDS));
		assertNull(clientException);
		assertTrue(serverLatch.await(5, TimeUnit.SECONDS));
		assertNull(serverException);
	}

	private void mySetupMutualAuth(KeyManagerFactory serverKMF, final File serverTrustManager,
			KeyManagerFactory clientKMF, File clientTrustManager,
			ClientAuth clientAuth, final boolean failureExpected,
			final boolean serverInitEngine)
			throws SSLException, InterruptedException {
		serverSslCtx = SslContextBuilder.forServer(serverKMF)
				.sslProvider(sslServerProvider())
				.sslContextProvider(serverSslContextProvider())
				.trustManager(serverTrustManager)
				.clientAuth(clientAuth)
				.ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
				.sessionCacheSize(0)
				.sessionTimeout(0)
				.build();

		clientSslCtx = SslContextBuilder.forClient()
				.sslProvider(sslClientProvider())
				.sslContextProvider(clientSslContextProvider())
				.trustManager(clientTrustManager)
				.keyManager(clientKMF)
				.ciphers(null, IdentityCipherSuiteFilter.INSTANCE)
				.sessionCacheSize(0)
				.sessionTimeout(0)
				.build();
		serverConnectedChannel = null;
		sb = new ServerBootstrap();
		cb = new Bootstrap();

		sb.group(new NioEventLoopGroup(), new NioEventLoopGroup());
		sb.channel(NioServerSocketChannel.class);
		sb.childHandler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ch.config().setAllocator(new TestByteBufAllocator(ch.config().getAllocator(), type));

				ChannelPipeline p = ch.pipeline();
				SslHandler handler = serverSslCtx.newHandler(ch.alloc());
				if (serverInitEngine) {
					mySetupMutualAuthServerInitSslHandler(handler);
				}
				p.addLast(handler);
				p.addLast(new MessageDelegatorChannelHandler(serverReceiver, serverLatch));
				p.addLast(new ChannelInboundHandlerAdapter() {
					@Override
					public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
						if (evt == SslHandshakeCompletionEvent.SUCCESS) {
							if (failureExpected) {
								serverException = new IllegalStateException("handshake complete. expected failure");
							}
							serverLatch.countDown();
						} else if (evt instanceof SslHandshakeCompletionEvent) {
							serverException = ((SslHandshakeCompletionEvent) evt).cause();
							serverLatch.countDown();
						}
						ctx.fireUserEventTriggered(evt);
					}

					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
						if (cause.getCause() instanceof SSLHandshakeException) {
							serverException = cause.getCause();
							serverLatch.countDown();
						} else {
							serverException = cause;
							ctx.fireExceptionCaught(cause);
						}
					}
				});
				serverConnectedChannel = ch;
			}
		});

		cb.group(new NioEventLoopGroup());
		cb.channel(NioSocketChannel.class);
		cb.handler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ch.config().setAllocator(new TestByteBufAllocator(ch.config().getAllocator(), type));
				ChannelPipeline p = ch.pipeline();
				p.addLast(clientSslCtx.newHandler(ch.alloc()));
				p.addLast(new MessageDelegatorChannelHandler(clientReceiver, clientLatch));
				p.addLast(new ChannelInboundHandlerAdapter() {
					@Override
					public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
						if (evt == SslHandshakeCompletionEvent.SUCCESS) {
							if (failureExpected) {
								clientException = new IllegalStateException("handshake complete. expected failure");
							}
							clientLatch.countDown();
						} else if (evt instanceof SslHandshakeCompletionEvent) {
							clientException = ((SslHandshakeCompletionEvent) evt).cause();
							clientLatch.countDown();
						}
						ctx.fireUserEventTriggered(evt);
					}

					@Override
					public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
						if (cause.getCause() instanceof SSLHandshakeException) {
							clientException = cause.getCause();
							clientLatch.countDown();
						} else {
							ctx.fireExceptionCaught(cause);
						}
					}
				});
			}
		});

		serverChannel = sb.bind(new InetSocketAddress(0)).sync().channel();
		int port = ((InetSocketAddress) serverChannel.localAddress()).getPort();

		ChannelFuture ccf = cb.connect(new InetSocketAddress(NetUtil.LOCALHOST, port));
		assertTrue(ccf.awaitUninterruptibly().isSuccess());
		clientChannel = ccf.channel();
	}

	interface MessageReceiver {

		void messageReceived(ByteBuf msg);
	}

	protected static final class MessageDelegatorChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

		private final MessageReceiver receiver;
		private final CountDownLatch latch;

		public MessageDelegatorChannelHandler(MessageReceiver receiver, CountDownLatch latch) {
			super(false);
			this.receiver = receiver;
			this.latch = latch;
		}

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
			receiver.messageReceived(msg);
			latch.countDown();
		}
	}

	protected ByteBuffer allocateBuffer(int len) {
		switch (type) {
			case Direct:
				return ByteBuffer.allocateDirect(len);
			case Heap:
				return ByteBuffer.allocate(len);
			case Mixed:
				return PlatformDependent.threadLocalRandom().nextBoolean()
						? ByteBuffer.allocateDirect(len) : ByteBuffer.allocate(len);
			default:
				throw new Error();
		}
	}

	private static final class TestByteBufAllocator implements ByteBufAllocator {

		private final ByteBufAllocator allocator;
		private final BufferType type;

		TestByteBufAllocator(ByteBufAllocator allocator, BufferType type) {
			this.allocator = allocator;
			this.type = type;
		}

		@Override
		public ByteBuf buffer() {
			switch (type) {
				case Direct:
					return allocator.directBuffer();
				case Heap:
					return allocator.heapBuffer();
				case Mixed:
					return PlatformDependent.threadLocalRandom().nextBoolean()
							? allocator.directBuffer() : allocator.heapBuffer();
				default:
					throw new Error();
			}
		}

		@Override
		public ByteBuf buffer(int initialCapacity) {
			switch (type) {
				case Direct:
					return allocator.directBuffer(initialCapacity);
				case Heap:
					return allocator.heapBuffer(initialCapacity);
				case Mixed:
					return PlatformDependent.threadLocalRandom().nextBoolean()
							? allocator.directBuffer(initialCapacity) : allocator.heapBuffer(initialCapacity);
				default:
					throw new Error();
			}
		}

		@Override
		public ByteBuf buffer(int initialCapacity, int maxCapacity) {
			switch (type) {
				case Direct:
					return allocator.directBuffer(initialCapacity, maxCapacity);
				case Heap:
					return allocator.heapBuffer(initialCapacity, maxCapacity);
				case Mixed:
					return PlatformDependent.threadLocalRandom().nextBoolean()
							? allocator.directBuffer(initialCapacity, maxCapacity)
							: allocator.heapBuffer(initialCapacity, maxCapacity);
				default:
					throw new Error();
			}
		}

		@Override
		public ByteBuf ioBuffer() {
			return allocator.ioBuffer();
		}

		@Override
		public ByteBuf ioBuffer(int initialCapacity) {
			return allocator.ioBuffer(initialCapacity);
		}

		@Override
		public ByteBuf ioBuffer(int initialCapacity, int maxCapacity) {
			return allocator.ioBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public ByteBuf heapBuffer() {
			return allocator.heapBuffer();
		}

		@Override
		public ByteBuf heapBuffer(int initialCapacity) {
			return allocator.heapBuffer(initialCapacity);
		}

		@Override
		public ByteBuf heapBuffer(int initialCapacity, int maxCapacity) {
			return allocator.heapBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public ByteBuf directBuffer() {
			return allocator.directBuffer();
		}

		@Override
		public ByteBuf directBuffer(int initialCapacity) {
			return allocator.directBuffer(initialCapacity);
		}

		@Override
		public ByteBuf directBuffer(int initialCapacity, int maxCapacity) {
			return allocator.directBuffer(initialCapacity, maxCapacity);
		}

		@Override
		public CompositeByteBuf compositeBuffer() {
			switch (type) {
				case Direct:
					return allocator.compositeDirectBuffer();
				case Heap:
					return allocator.compositeHeapBuffer();
				case Mixed:
					return PlatformDependent.threadLocalRandom().nextBoolean()
							? allocator.compositeDirectBuffer()
							: allocator.compositeHeapBuffer();
				default:
					throw new Error();
			}
		}

		@Override
		public CompositeByteBuf compositeBuffer(int maxNumComponents) {
			switch (type) {
				case Direct:
					return allocator.compositeDirectBuffer(maxNumComponents);
				case Heap:
					return allocator.compositeHeapBuffer(maxNumComponents);
				case Mixed:
					return PlatformDependent.threadLocalRandom().nextBoolean()
							? allocator.compositeDirectBuffer(maxNumComponents)
							: allocator.compositeHeapBuffer(maxNumComponents);
				default:
					throw new Error();
			}
		}

		@Override
		public CompositeByteBuf compositeHeapBuffer() {
			return allocator.compositeHeapBuffer();
		}

		@Override
		public CompositeByteBuf compositeHeapBuffer(int maxNumComponents) {
			return allocator.compositeHeapBuffer(maxNumComponents);
		}

		@Override
		public CompositeByteBuf compositeDirectBuffer() {
			return allocator.compositeDirectBuffer();
		}

		@Override
		public CompositeByteBuf compositeDirectBuffer(int maxNumComponents) {
			return allocator.compositeDirectBuffer(maxNumComponents);
		}

		@Override
		public boolean isDirectBufferPooled() {
			return allocator.isDirectBufferPooled();
		}

		@Override
		public int calculateNewCapacity(int minNewCapacity, int maxCapacity) {
			return allocator.calculateNewCapacity(minNewCapacity, maxCapacity);
		}
	}

}
