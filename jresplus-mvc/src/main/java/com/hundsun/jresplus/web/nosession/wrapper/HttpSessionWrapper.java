/*
 * 模块名称：NoSession session包装器，封装session的get/setAttributes
 * 修改记录
 * 日期			修改人		修改原因
 * =====================================================================================
 * 2014-9-22	XIE			创建
 * =====================================================================================
 */

package com.hundsun.jresplus.web.nosession.wrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hundsun.jresplus.common.util.StringUtil;
import com.hundsun.jresplus.web.nosession.NoSessionContext;
import com.hundsun.jresplus.web.nosession.StoreContext;
import com.hundsun.jresplus.web.nosession.cookie.SessionToken;

public class HttpSessionWrapper implements HttpSession {
	private final static Logger log = LoggerFactory
			.getLogger(HttpSessionWrapper.class);
	private NoSessionContext context;
	private ServletContext servletContext;
	private HttpServletRequest request;
	private Map<String, StoreContext> storeAttribute;
	private Set<String> enumerations;
	private boolean isNew = true;
	private boolean isValid = true;

	public HttpSessionWrapper() {
		enumerations = new HashSet<String>();
	}

	public void setContext(NoSessionContext context) {
		this.context = context;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void setStoreAttribute(Map<String, StoreContext> attributes) {
		this.storeAttribute = attributes;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public void setId(String id) {
		this.setAttribute(SessionToken.SESSION_ID, id);
	}

	public long getCreationTime() {
		return getAttribute((SessionToken.CREATION_TIME)) == null ? 0l
				: ((Long) getAttribute((SessionToken.CREATION_TIME)))
						.longValue();
	}

	public String getId() {
		return (String) getAttribute(SessionToken.SESSION_ID);

	}

	private void checkInvalid() {
		if (false == isValid()) {
			throw new IllegalStateException("session has been invalidated");
		}
	}

	private boolean isValid() {
		return isValid;
	}

	public long getLastAccessedTime() {
		return getAttribute((SessionToken.LAST_ACCESSED_TIME)) == null ? 0l
				: ((Long) getAttribute((SessionToken.LAST_ACCESSED_TIME)))
						.longValue();
	}

	public ServletContext getServletContext() {
		checkInvalid();
		return servletContext;
	}

	public void setMaxInactiveInterval(int interval) {

	}

	public int getMaxInactiveInterval() {
		return context.getMaxInactiveInterval();
	}

	@Deprecated
	public HttpSessionContext getSessionContext() {
		throw new UnsupportedOperationException(
				"No longer supported method: getSessionContext");
	}

	public Object getAttribute(String key) {
		if (!context.isSessionKey(key)) {
			checkInvalid();
		}
		if (storeAttribute.containsKey(key)) {
			return storeAttribute.get(key).getValue();
		}

		return storeAttribute.containsKey(key) ? storeAttribute.get(key)
				.getValue() : null;
	}

	public Object getValue(String name) {
		checkInvalid();
		return getAttribute(name);
	}

	@SuppressWarnings({ "rawtypes" })
	public Enumeration getAttributeNames() {
		return Collections.enumeration(enumerations);
	}

	public String[] getValueNames() {
		return (String[]) enumerations.toArray();
	}

	public void setAttribute(String key, Object value) {
		checkInvalid();
		if (context.isSessionKey(key)) {
			updateAttribute(key, value);
		}
		if (context.isMatchStore(key)) {
			enumerations.add(key);
			updateAttribute(key, value);
		}
	}

	private void updateAttribute(String key, Object value) {
		if (storeAttribute.containsKey(key)) {
			storeAttribute.get(key).setValue(value);
		} else {
			storeAttribute.put(key, new StoreContext(value, true));
		}
	}

	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	public void removeAttribute(String key) {
		checkInvalid();
		setAttribute(key, null);
	}

	public void removeValue(String name) {
		removeAttribute(name);
	}

	public void invalidate() {
		this.isValid = false;
		context.invalidateStore(getId());
		Iterator<String> it = storeAttribute.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();
			if (!context.isSessionKey(key)) {
				storeAttribute.get(key).setValue(null);
				enumerations.remove(key);
			}
		}

	}

	public boolean isNew() {
		return this.isNew;
	}

	/**
	 * 
	 * @return
	 */
	public boolean timeOut() {
		long nowTime = System.currentTimeMillis();
		if (getMaxInactiveInterval() != -1
				&& nowTime > (getLastAccessedTime() + getMaxInactiveInterval() * 1000)) {
			return true;
		}
		return false;
	}

	public void setIsNew(boolean b) {
		isNew = b;

	}

	public HttpSession create(boolean createNew) {
		fitStoreAttribute();
		String jesssionid = getId();
		if (StringUtil.isNotBlank(jesssionid)) {
			isNew = false;
			if (timeOut()) {
				if (log.isDebugEnabled()) {
					log.debug("the session is time out! id =" + getId());
				}
				if (createNew) {
					isNew = true;
				} else {
					return null;
				}
			}
		}

		if (isNew) {
			setId(context.genSessionID());
			setAttribute(SessionToken.CREATION_TIME, System.currentTimeMillis());
		}
		setAttribute(SessionToken.LAST_ACCESSED_TIME,
				System.currentTimeMillis());

		return this;
	}

	private void fitStoreAttribute() {
		Map<String, Object> storeData = context.getStoreData(request);
		for (Entry<String, Object> entry : storeData.entrySet()) {
			storeAttribute.put(entry.getKey(),
					new StoreContext(entry.getValue()));
		}
	}

}
