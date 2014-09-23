/*
 * 源程序名称: ServletOutputStreamWrapper.java 
 * 软件著作权: 恒生电子股份有限公司 版权所有
 * 模块名称：TODO(这里注明模块名称)
 * 
 */

package com.hundsun.jresplus.web.nosession.wrapper;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import com.hundsun.jresplus.common.util.io.BufferedByteArrayOutputStream;

public class ServletOutputStreamWrapper extends ServletOutputStream {
	private BufferedByteArrayOutputStream bos;

	public ServletOutputStreamWrapper(BufferedByteArrayOutputStream bos) {
		super();
		this.bos = bos;
	}

	@Override
	public void write(int b) throws IOException {
		bos.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		bos.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		bos.write(b);
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public void flush() throws IOException {
	}

}
