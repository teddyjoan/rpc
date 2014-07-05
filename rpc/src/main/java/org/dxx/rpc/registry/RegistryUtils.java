package org.dxx.rpc.registry;

import io.netty.channel.Channel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.dxx.rpc.RpcConstants;
import org.dxx.rpc.config.Registry;
import org.dxx.rpc.config.loader.Loader;
import org.dxx.rpc.exception.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryUtils {
	private static final Logger logger = LoggerFactory.getLogger(RegistryUtils.class);
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private static AtomicLong sequence = new AtomicLong(0);
	private static Channel registyChannel;
	private static Map<Long, LocateRpcServerResponse> responses = new ConcurrentHashMap<Long, LocateRpcServerResponse>();

	public static boolean isRegistryInitialized() {
		return registyChannel != null;
	}

	public static void setResponse(LocateRpcServerResponse response) {
		RegistryUtils.responses.put(response.getId(), response);
	}

	public static void setRegistyChannel(Channel registyChannel) {
		RegistryUtils.registyChannel = registyChannel;
	}

	public static void removeRegistryChannel() {
		RegistryUtils.registyChannel = null;
	}

	/**
	 * 调用注册中心，并返回所有服务端地址
	 *
	 * @param intersWithoutUrl
	 * @return
	 */
	public static LocateRpcServerResponse locateServer(List<String> intersWithoutUrl) {
		if (registyChannel == null) {
			return null;
		}
		logger.debug("Locate urls for interfaces : {}", intersWithoutUrl);
		LocateRpcServerRequest request = new LocateRpcServerRequest();
		request.setId(sequence.incrementAndGet());
		request.setInterfaceClasses(intersWithoutUrl);
		registyChannel.writeAndFlush(request);

		long s = System.currentTimeMillis();
		while (true) {
			if (System.currentTimeMillis() - s > RpcConstants.LOCATE_TIME_OUT) {
				throw new RegistryException("调用注册中心获取server地址超时 ： " + RpcConstants.LOCATE_TIME_OUT);
			}
			if (responses.containsKey(request.getId())) {
				return responses.remove(request.getId());
			}

			try {
				Thread.sleep(10L);
			} catch (InterruptedException e) {
			}
		}
	}

	public static Class<?> getInter(String interfaceClass) {
		try {
			return Class.forName(interfaceClass);
		} catch (ClassNotFoundException e) {
			throw new RpcException(e.getMessage(), e);
		}
	}

	public static void scheduleRegistry() {
		scheduler.schedule(new RegistryStartup(), RpcConstants.REGISTRY_RETRY_TIME, TimeUnit.MILLISECONDS);
	}

	/**
	 * 创建注册中心的channel
	 */
	public static void createRegistryChannelSync() {
		Registry registry = Loader.getRpcConfig().getRegistry();
		if (registry == null) {
			logger.debug("<registry../> is not configured !");
			return;
		}
		new RegistryStartup().submitAndWait();
	}

}
