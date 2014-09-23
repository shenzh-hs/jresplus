/*
 * 修改记录
 * 2013-8-13	XIE		STORY #6534 -nosession增加开关配置
 * 2013-9-29	XIE		增加cookie方式的session存储单元接口替换原来继承方式的扩展机制
 * 2014-2-7		XIE		STORY #7563 -nosession的cookiestore支持domain
 * 2014-9-22	XIE		重构
 * 2014-9-22	XIE		修复session.getServletContext为空
 * 2014-9-22	XIE		attributeStore增加sessionId隔离数据特性
 */
package com.hundsun.jresplus.web.nosession;

import java.io.IOException;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hundsun.jresplus.web.nosession.wrapper.RequestWrapper;
import com.hundsun.jresplus.web.nosession.wrapper.ResponseWrapper;

/**
 * 
 * @author LeoHu copy by sagahl
 * 
 */
public class NoSessionFilter extends OncePerRequestFilter implements Filter {
	private final static Logger log = LoggerFactory
			.getLogger(NoSessionFilter.class);

	private boolean onoff = true;

	NoSessionContext context;

	public void setOnoff(boolean onoff) {
		this.onoff = onoff;
	}

	public void setContext(NoSessionContext context) {
		this.context = context;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse, FilterChain filterChain)
			throws ServletException, IOException {
		if (onoff) {
			final Map<String, StoreContext> storeAttribute = new HashMap<String, StoreContext>();

			RequestWrapper request = new RequestWrapper(servletRequest);
			request.setStoreAttribute(storeAttribute);
			request.setContext(context);
			request.setServletContext(getServletContext());

			ResponseWrapper response = new ResponseWrapper(servletResponse);
			response.setRequest(request);
			response.setContext(context);
			response.setStoreAttributes(storeAttribute);
			response.init();

			boolean isFlush = true;
			try {
				filterChain.doFilter(request, response);
			} catch (Throwable e) {
				isFlush = false;
				throw new ServletException(e);
			} finally {
				if (request.getSession() != null) {
					context.updateAttributeStore(storeAttribute);
				}
				try {
					if (isFlush) {
						response.flushBuffer();
					} else {
						response.reset();
					}
				} catch (Exception e) {
					String err = e.getMessage();
					if (e.getCause() != null
							&& (e.getCause() instanceof SocketException)) {
						err = e.getCause().getMessage();
					}
					log.error("response flush or reset failed[{}]", err);
				}
				storeAttribute.clear();
			}
		} else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

}
