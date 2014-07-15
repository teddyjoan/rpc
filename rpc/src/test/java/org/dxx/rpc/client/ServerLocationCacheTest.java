/**
 * ServerLocationCacheTest.java
 * org.dxx.rpc.client
 * Copyright (c) 2014, 北京微课创景教育科技有限公司版权所有.
*/

package org.dxx.rpc.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.dxx.rpc.registry.UpdateServerLocationRequest;
import org.junit.Test;

/**
 * 
 * 
 * @author   dixingxing
 * @Date	 2014年7月15日
 */

public class ServerLocationCacheTest {

	@Test
	public void test() {
		Map<String, List<String>> interAndUrl = new ConcurrentHashMap<String, List<String>>();
		List<String> urls = new ArrayList<String>();
		urls.add("192.168.1.11:50020");
		urls.add("192.168.1.12:50020");
		interAndUrl.put("UserService", urls);

		UpdateServerLocationRequest request = new UpdateServerLocationRequest();
		request.setInterAndUrl(interAndUrl);
		ServerLocationCache.update(request);

		assertNotNull(ServerLocationCache.getServerLocation("UserService", null));

		assertEquals("192.168.1.12", ServerLocationCache.getServerLocation("UserService", "192.168.1.11:50020")
				.getHost());
	}

}