package com.lhiot.oc.basic.service.payment;

import com.leon.microx.util.xml.XNode;
import com.leon.microx.util.xml.XPathParser;

public class XPathWrapper {
	private XPathParser xpath;

	public XPathWrapper(XPathParser xpath) {
		this.xpath = xpath;
	}

	public String get(String key) {
		XNode xNode = this.xpath.evalNode("//" + key);
		if (xNode == null) {
			return null;
		}
		return xNode.body();
	}
}
