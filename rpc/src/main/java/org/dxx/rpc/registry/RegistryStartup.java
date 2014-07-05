package org.dxx.rpc.registry;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.dxx.rpc.codec.DexnDecoder;
import org.dxx.rpc.codec.DexnEncoder;
import org.dxx.rpc.common.Awakeable;
import org.dxx.rpc.config.Registry;
import org.dxx.rpc.config.loader.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用于创建和注册中心的连接
 * 
 * @author   dixingxing
 * @Date	 2014-6-25
 */
public class RegistryStartup extends Awakeable {
	static final Logger logger = LoggerFactory.getLogger(RegistryStartup.class);
	private String host;
	private int port;

	public RegistryStartup() {
		super();
		Registry registry = Loader.getRpcConfig().getRegistry();
		if (registry == null) {
			logger.info("<registry../> is not configured !");
			return;
		}

		if (registry.getHost() == null || registry.getHost().isEmpty()) {
			logger.error("Registry host must not be empty");
		} else {
			this.host = registry.getHost();
		}

		this.port = (registry.getPort() <= 0 ? RegistryConstants.DEFUALT_PORT : registry.getPort());
	}

	@Override
	public void run() {
		if (this.host == null) {
			return;
		}
		if (RegistryUtils.isRegistryInitialized()) {
			logger.debug("Already inited ...");
			return;
		}

		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(workerGroup);
			b.channel(NioSocketChannel.class);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			b.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new DexnEncoder(), new DexnDecoder(), new RegistryHandler());
				}
			});
			ChannelFuture f = b.connect(host, port).sync();
			RegistryUtils.setRegistyChannel(f.channel());
			awake();
			f.channel().closeFuture().sync();
		} catch (Exception e) {
			logger.warn("连接注册中心异常 : " + e.getMessage());
			RegistryUtils.scheduleRegistry();
			awake();
		} finally {
			workerGroup.shutdownGracefully();
		}
	}

}
