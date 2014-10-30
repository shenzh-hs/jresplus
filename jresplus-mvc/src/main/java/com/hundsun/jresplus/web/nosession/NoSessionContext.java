/*
 * 模块名称：NoSession 上下文，提供session存储器的加载以及数据读写
 * 修改记录
 * 日期			修改人		修改原因
 * =====================================================================================
 * 2014-9-22	XIE			创建
 * =====================================================================================
 */

package com.hundsun.jresplus.web.nosession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

import com.hundsun.jresplus.beans.ObjectFactory;
import com.hundsun.jresplus.common.util.ArrayUtil;
import com.hundsun.jresplus.common.util.StringUtil;
import com.hundsun.jresplus.common.util.io.BufferedByteArrayOutputStream;
import com.hundsun.jresplus.web.nosession.cookie.AttributeCookieStore;
import com.hundsun.jresplus.web.nosession.cookie.Cookie;
import com.hundsun.jresplus.web.nosession.cookie.CookiesManager;
import com.hundsun.jresplus.web.nosession.cookie.Encode;
import com.hundsun.jresplus.web.nosession.cookie.SessionToken;

public class NoSessionContext implements InitializingBean {
	private final static Logger log = LoggerFactory
			.getLogger(NoSessionContext.class);
	public static final ThreadLocal<BufferedByteArrayOutputStream> outputStreams = new ThreadLocal<BufferedByteArrayOutputStream>();

	private ObjectFactory objectFactory;
	private CookiesManager cookiesManager;
	private static int outBufferSize = 1024 * 5;
	private String outCharset;
	private UUIDGenerator uuidGenerator;

	SessionToken sessionToken;
	List<AttributeCookieStore> attributeCookieStores = new ArrayList<AttributeCookieStore>();

	List<AttributeStore> attributeStores = new ArrayList<AttributeStore>();

	public static BufferedByteArrayOutputStream getOurputStream() {
		if (outputStreams.get() == null) {
			outputStreams.set(new BufferedByteArrayOutputStream(outBufferSize));
		}
		return outputStreams.get();
	}

	public void setObjectFactory(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}

	public void setUuidGenerator(UUIDGenerator uuidGenerator) {
		this.uuidGenerator = uuidGenerator;
	}

	public void setOutCharset(String outCharset) {
		this.outCharset = outCharset;
	}

	public void setCookiesManager(CookiesManager cookiesManager) {
		this.cookiesManager = cookiesManager;
	}

	public boolean isSessionKey(String key) {
		return SessionToken.keyNames.contains(key);
	}

	public void afterPropertiesSet() throws Exception {
		List<AttributeCookieStore> list1 = objectFactory
				.getBeansOfType4List(AttributeCookieStore.class);
		Set<String> attrs = new HashSet<String>();
		if (ArrayUtil.isEmpty(list1) == false) {
			for (AttributeCookieStore store : list1) {
				attributeCookieStores.add(store);
				if (store.getAttributeNames() == null) {
					continue;
				}
				if (store instanceof SessionToken) {
					sessionToken = (SessionToken) store;
				}
				attrs.addAll(store.getAttributeNames());
			}
		}
		Assert.notNull(sessionToken,
				"session store min data[SessionToken] losed");
		List<AttributeStore> list2 = objectFactory
				.getBeansOfType4List(AttributeStore.class);
		if (ArrayUtil.isEmpty(list2) == false) {
			for (AttributeStore store : list2) {
				attributeStores.add(store);
				if (store.getAttributeNames() == null) {
					continue;
				}
				attrs.addAll(store.getAttributeNames());
			}
		}
		StringBuffer attrStr = new StringBuffer("");
		for (String key : attrs) {
			attrStr.append(key).append(",");
		}
		log.info("Nosession store registed attribute[{}]", attrStr);
	}

	public void updateCookieStore(HttpServletRequest request,
			HttpServletResponse response, Map<String, StoreContext> attributes) {
		attributes.put(SessionToken.KEYS, new StoreContext(attributes.keySet(),
				true));
		for (AttributeCookieStore attributeStore : attributeCookieStores) {

			boolean isNotModified = isNotModified(attributes, attributeStore);

			if (isNotModified && attributeStore.getMaxInactiveInterval() == -1) {
				continue;
			}
			Map<String, Object> storeData = getMatchedStoreData(attributes,
					attributeStore);
			writeCookie(request, response, attributeStore, storeData);
		}
	}

	private boolean isNotModified(Map<String, StoreContext> attributes,
			AttributeCookieStore attributeStore) {
		for (Entry<String, StoreContext> entry : attributes.entrySet()) {
			if (!attributeStore.isMatch(entry.getKey())) {
				continue;
			}
			if (entry.getValue().isModified()) {
				return false;
			}
		}
		return true;
	}

	private Map<String, Object> getMatchedStoreData(
			Map<String, StoreContext> attributes,
			AttributeCookieStore attributeStore) {
		Map<String, Object> storeData = new HashMap<String, Object>();
		for (Entry<String, StoreContext> entry : attributes.entrySet()) {
			if (!attributeStore.isMatch(entry.getKey())) {
				continue;
			}
			if (entry.getValue().getValue() != null) {
				storeData.put(entry.getKey(), entry.getValue().getValue());
			}

		}
		return storeData;
	}

	private void writeCookie(HttpServletRequest request,
			HttpServletResponse response, AttributeCookieStore attributeStore,
			Map<String, Object> tmp) {
		try {
			int cookieTime = 0;
			String cookieName = attributeStore.getCookieName();
			String cookiePath = attributeStore.getPath();
			String cookieDomain = attributeStore.getDomain();
			boolean httpOnly = true;
			String cookieValue = null;
			if (tmp.size() > 0) {
				cookieValue = attributeStore.getEncode().encode(tmp);
				cookieTime = attributeStore.getMaxInactiveInterval();
			}
			cookiesManager.writeCookie(request, response, new Cookie(
					cookieName, cookieValue, httpOnly, cookieTime, cookiePath,
					cookieDomain));
		} catch (SessionEncoderException e) {
			log.error("write cookie store error !", e);
		} catch (SerializationException e) {
			log.error("write cookie store error !", e);
		}
	}

	public void updateAttributeStore(Map<String, StoreContext> attributes) {
		StoreContext sessionStore = attributes.get(SessionToken.SESSION_ID);
		String sessionId = (String) sessionStore.getValue();
		for (AttributeStore store : attributeStores) {
			store.setValue(sessionId, getMatchedStoreData(attributes, store));
		}
	}

	private Map<String, StoreContext> getMatchedStoreData(
			Map<String, StoreContext> attributes, AttributeStore attributeStore) {
		Map<String, StoreContext> storeData = new HashMap<String, StoreContext>();
		for (Entry<String, StoreContext> entry : attributes.entrySet()) {
			if (!attributeStore.isMatch(entry.getKey())) {
				continue;
			}
			storeData.put(entry.getKey(), entry.getValue());

		}
		return storeData;
	}

	public boolean isMatchStore(String key) {
		for (AttributeCookieStore store : attributeCookieStores) {
			if (store.isMatch(key)) {
				return true;
			}
		}
		for (AttributeStore store : attributeStores) {
			if (store.isMatch(key)) {
				return true;
			}
		}
		return false;
	}

	public String getOutCharset() {
		return outCharset;
	}

	public int getMaxInactiveInterval() {
		return sessionToken.getMaxInactiveInterval();
	}

	public String genSessionID() {
		return uuidGenerator.gain();
	}

	public void invalidateStore(String sessionId) {
		for (AttributeStore store : attributeStores) {
			store.invalidate(sessionId);
		}

	}

	public Map<String, Object> getStoreData(HttpServletRequest request) {
		Map<String, Object> storeData = new HashMap<String, Object>();
		String sessionId = "";
		for (AttributeCookieStore store : attributeCookieStores) {
			final Encode storeEncode = store.getEncode();
			final String storeCookieName = store.getCookieName();
			String cookieValue = cookiesManager.readCookieValue(request,
					storeCookieName);
			if (StringUtil.isEmpty(cookieValue)) {
				continue;
			}
			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) storeEncode
						.decode(cookieValue);
				if (map == null) {
					continue;
				}
				if (map.containsKey(SessionToken.SESSION_ID)) {
					sessionId = (String) map.get(SessionToken.SESSION_ID);
				}
				storeData.putAll(map);
			} catch (Exception e) {
				log.error("Get data from cookie error[{}]", e.getMessage());
			}
		}
		if (StringUtil.isBlank(sessionId)) {
			return storeData;
		}
		for (AttributeStore store : attributeStores) {
			Map<String, Object> map = store.loadValue(sessionId);
			if (map != null) {
				storeData.putAll(map);
			}

		}
		return storeData;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getSessionTokenData(HttpServletRequest request) {
		String cookieValue = cookiesManager.readCookieValue(request,
				sessionToken.getCookieName());
		if (StringUtil.isBlank(cookieValue)) {
			return null;
		}
		try {
			return (Map<String, Object>) sessionToken.getEncode().decode(
					cookieValue);
		} catch (Exception e) {
			log.error("SessionToken cookieValue decode error[{}].",
					e.getMessage());
		}
		return null;
	}

	public String getSessionIdFormStore(HttpServletRequest request) {
		Map<String, Object> map = getSessionTokenData(request);
		if (map == null) {
			return null;
		}
		return (String) map.get(SessionToken.SESSION_ID);

	}
}
