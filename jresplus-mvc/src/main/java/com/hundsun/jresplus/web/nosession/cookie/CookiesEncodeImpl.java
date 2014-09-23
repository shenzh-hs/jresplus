package com.hundsun.jresplus.web.nosession.cookie;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.SerializationException;

import com.hundsun.jresplus.common.util.StringUtil;

public class CookiesEncodeImpl implements Encode {
	private String salt;

	public String encode(Object object) throws SerializationException {
		if (object == null) {
			return null;
		}
		String str = new String(Base64.encodeBase64(HessianZipSerializer
				.encode(object)));
		if (StringUtil.isNotBlank(salt)) {
			return salt + str;
		}
		return str;
	}

	public Object decode(String str) throws SerializationException {
		if (StringUtil.isEmpty(str) == true) {
			return null;
		}
		if (StringUtil.isNotBlank(salt)) {
			String nStr = str.substring(salt.length());
			return HessianZipSerializer.decode(Base64.decodeBase64(nStr));
		}
		return HessianZipSerializer.decode(Base64.decodeBase64(str));
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

}
