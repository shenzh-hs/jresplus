/*
 * 模块名称：NoSession response包装器，封装flushBuffer时支持cookie提交
 * 修改记录
 * 日期			修改人		修改原因
 * =====================================================================================
 * 2014-9-22	XIE			创建
 * =====================================================================================
 */

package com.hundsun.jresplus.web.nosession.wrapper;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.hundsun.jresplus.common.util.io.BufferedByteArrayOutputStream;
import com.hundsun.jresplus.web.nosession.NoSessionContext;
import com.hundsun.jresplus.web.nosession.ServerCookie;
import com.hundsun.jresplus.web.nosession.StoreContext;

public class ResponseWrapper extends HttpServletResponseWrapper {
	private Map<String, StoreContext> storeAttribute = null;
	private HttpServletResponse response;
	private HttpServletRequest request;
	private boolean _commited = false;
	private Set<Cookie> _cookies = new HashSet<Cookie>();
	private BufferedByteArrayOutputStream _outputStream;
	private ServletOutputStreamWrapper _servletOutputStream;
	private PrintWriter _pw;
	private int recyclingBufferBlockSize = 2;
	private NoSessionContext context;

	public ResponseWrapper(HttpServletResponse response) {
		super(response);
		this.response = response;

	}

	public void init() throws IOException {
		_outputStream = NoSessionContext.getOurputStream();
		_pw = new PrintWriter(new OutputStreamWriter(_outputStream,
				context.getOutCharset()));
		_servletOutputStream = new ServletOutputStreamWrapper(_outputStream);
	}

	public void setStoreAttributes(Map<String, StoreContext> attributes) {
		this.storeAttribute = attributes;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public void setContext(NoSessionContext context) {
		this.context = context;
	}

	@Override
	public void addCookie(Cookie cookie) {
		if (isCommitted() || _commited == true) {
			return;
		}
		Iterator<Cookie> it = _cookies.iterator();
		while (it.hasNext() == true) {
			Cookie cookieTemp = it.next();
			if (cookieTemp.getName().equals(cookie.getName())) {
				_cookies.remove(cookieTemp);
				it = _cookies.iterator();
			}
		}
		_cookies.add(cookie);
	}

	private void cookiesCommit() {
		if (_commited || isCommitted()) {
			return;
		}

		if (request.getSession() != null) {
			/** 更新数据到cookie的存储单元 */
			context.updateCookieStore(request, this, storeAttribute);
		}
		for (Cookie cookie : _cookies) {
			if (cookie instanceof com.hundsun.jresplus.web.nosession.cookie.Cookie) {
				addHeader(
						getCookieHeaderName(cookie),
						getCookieHeaderValue((com.hundsun.jresplus.web.nosession.cookie.Cookie) cookie));
			} else {
				super.addCookie(cookie);
			}
		}
		_commited = true;
	}

	@Override
	public void flushBuffer() throws IOException {
		try {
			cookiesCommit();
			ServletOutputStream sos = super.getOutputStream();
			_pw.flush();
			_outputStream.writeTo(sos);
			sos.flush();
		} finally {
			resetBufferedOutputStream();
		}
	}

	private void resetBufferedOutputStream() {
		_outputStream.reset(recyclingBufferBlockSize);
	}

	@Override
	public void reset() {
		resetBufferedOutputStream();
	}

	@Override
	public void resetBuffer() {
		resetBufferedOutputStream();
	}

	@Override
	public int getBufferSize() {
		return Integer.MAX_VALUE;
	}

	private String getCookieHeaderName(Cookie cookie) {
		return ServerCookie.getCookieHeaderName(cookie.getVersion());
	}

	private String getCookieHeaderValue(
			com.hundsun.jresplus.web.nosession.cookie.Cookie cookie)
			throws IllegalArgumentException {
		return appendCookieHeaderValue(new StringBuilder(), cookie).toString();
	}

	private StringBuilder appendCookieHeaderValue(StringBuilder buf,
			com.hundsun.jresplus.web.nosession.cookie.Cookie cookie)
			throws IllegalArgumentException {
		ServerCookie.appendCookieValue(buf, cookie.getVersion(),
				cookie.getName(), cookie.getValue(), cookie.getPath(),
				cookie.getDomain(), cookie.getComment(), cookie.getMaxAge(),
				cookie.getSecure(), cookie.isHttpOnly());
		return buf;
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		cookiesCommit();
		super.sendRedirect(location);
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return _servletOutputStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return _pw;
	}

	@Override
	public void setBufferSize(int size) {
	}
}
