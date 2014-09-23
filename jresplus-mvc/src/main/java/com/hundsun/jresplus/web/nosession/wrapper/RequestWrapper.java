/*
 * 模块名称：NoSession request包装器，封装getSession
 * 修改记录
 * 日期			修改人		修改原因
 * =====================================================================================
 * 2014-9-22	XIE			创建
 * =====================================================================================
 */

package com.hundsun.jresplus.web.nosession.wrapper;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import com.hundsun.jresplus.web.nosession.NoSessionContext;
import com.hundsun.jresplus.web.nosession.StoreContext;

public class RequestWrapper extends HttpServletRequestWrapper {
	private NoSessionContext context;
	private ServletContext servletContext;
	private Map<String, StoreContext> storeAttribute;
	private HttpSession _httpSession;

	public RequestWrapper(HttpServletRequest request) {
		super(request);
	}

	public void setContext(NoSessionContext context) {
		this.context = context;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public void setStoreAttribute(Map<String, StoreContext> storeAttributes) {
		this.storeAttribute = storeAttributes;
	}

	@Override
	public HttpSession getSession() {
		return getSession(false);
	}

	@Override
	public HttpSession getSession(boolean create) {
		if (_httpSession != null) {
			return _httpSession;
		}
		HttpSessionWrapper session = new HttpSessionWrapper();
		session.setStoreAttribute(storeAttribute);
		session.setRequest(this);
		session.setContext(context);
		session.setServletContext(servletContext);
		_httpSession = session.create(create);
		return _httpSession;
	}
}
