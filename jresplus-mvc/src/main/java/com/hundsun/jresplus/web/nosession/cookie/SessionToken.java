/*
 * 源程序名称: SessionMainCookieStore.java 
 * 软件著作权: 恒生电子股份有限公司 版权所有
 * 模块名称：TODO(这里注明模块名称)
 * 
 */

package com.hundsun.jresplus.web.nosession.cookie;

import java.util.HashSet;
import java.util.Set;

public class SessionToken implements AttributeCookieStore {

	public static final String CREATION_TIME = "creationTime";
	public static final String LAST_ACCESSED_TIME = "lastAccessedTime";
	public static final String SESSION_ID = "jsessionId";
	public static final String KEYS = "sessionKeys";
	public static Set<String> keyNames = new HashSet<String>();
	static {
		keyNames.add(CREATION_TIME);
		keyNames.add(SESSION_ID);
		keyNames.add(LAST_ACCESSED_TIME);
		keyNames.add(KEYS);
	}

	private String metaDomain;
	private String metaCookieName;
	private int maxInactiveInterval = -1;
	private Encode encode;

	public void setEncode(Encode encode) {
		this.encode = encode;
	}

	public void setMetaDomain(String metaDomain) {
		this.metaDomain = metaDomain;
	}

	public String getMetaDomain() {
		return this.metaDomain;
	}

	public void setMetaCookieName(String metaCookieName) {
		this.metaCookieName = metaCookieName;
	}

	public int getOrder() {
		return 0;
	}

	public boolean isMatch(String key) {
		return keyNames.contains(key);
	}

	public String getCookieName() {
		return metaCookieName;
	}

	public Encode getEncode() {
		return encode;
	}

	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}

	public int getMaxInactiveInterval() {
		return this.maxInactiveInterval;
	}

	public String getPath() {
		return "/";
	}

	public Set<String> getAttributeNames() {
		return keyNames;
	}

	public String getDomain() {
		return metaDomain;
	}

}
