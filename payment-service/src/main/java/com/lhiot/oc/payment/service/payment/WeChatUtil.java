/**
* Copyright © 2016 SGSL
* 湖南绿航恰果果农产品有限公司
* http://www.sgsl.com 
* All rights reserved. 
*/
package com.lhiot.oc.payment.service.payment;

import com.leon.microx.support.result.Tips;
import com.leon.microx.util.Jackson;
import com.leon.microx.util.StringUtils;
import com.leon.microx.util.auditing.Random;
import com.leon.microx.util.xml.XNode;
import com.leon.microx.util.xml.XReader;
import com.lhiot.oc.payment.util.DateFormatUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.*;
import java.util.Map.Entry;


/**
 * 微信工具类
 *
 * @author leon
 * @version 1.0 2016年11月18日下午3:31:26
 */
@Slf4j
@Service
@Data
public class WeChatUtil {
	public final  String encoding = "UTF-8";

	/** 微信支付 - 退款接口 (POST) */
	public final  String REFUND_URL = "https://api.mch.weixin.qq.com/secapi/pay/refund";
	//public final  String REFUND_URL = "https://api.mch.weixin.qq.com/sandboxnew/pay/refund";//沙箱环境测试
	/**微信支付撤销订单接口 (POST) */
	public final  String CLOSEORDER_URL = "https://api.mch.weixin.qq.com/pay/closeorder";

	/** 微信支付 - 获取沙箱密钥 (POST) */
	//public final  String GET_SING_KEY = "https://api.mch.weixin.qq.com/sandboxnew/pay/getsignkey";//沙箱环境测试

	/** 微信支付统一接口(POST) */
	public final  String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
	//public final  String UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/sandboxnew/pay/unifiedorder";//沙箱环境测试

	/** 下载对账单(POST) */
	//public final  String 	DOWN_LOAD_BILL_URL = "https://api.mch.weixin.qq.com/sandboxnew/pay/downloadbill";//沙箱环境测试

	/** 查询支付订单状态(POST) */
	//public final  String 	ORDER_QUERY_URL = "https://api.mch.weixin.qq.com/sandboxnew/pay/orderquery";//沙箱环境测试

	/** 查询退款订单状态(POST) */
	//public final  String 	REFUND_QUERY_URL = "https://api.mch.weixin.qq.com/sandboxnew/pay/refundquery";//沙箱环境测试

	/** 获取token接口(GET) */
	public final  String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={0}&secret={1}";

	/** 获取ticket接口(GET) */
	public final  String TICKET_URL = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token={0}&type=jsapi";

	/** 获取OPEN ID (GET) */
	public final  String OPEN_ID_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={0}&secret={1}&code={2}&grant_type=authorization_code";

	/** 获取微信用户信息 (GET) */
	public final  String USER_INFO_URL = "https://api.weixin.qq.com/cgi-bin/user/info?access_token={0}&openid={1}&lang=zh_CN";

	/** oauth2网页授权接口(GET) */
	public final  String OAUTH2_URL = "https://open.weixin.qq.com/connect/oauth2/authorize?appid={0}&redirect_uri={1}&response_type=code&scope={2}&state={3}#wechat_redirect";

	/** 获取网友授权微信用户信息 (GET) */
	public final  String OAUTH2_USER_INFO_URL = "https://api.weixin.qq.com/sns/userinfo?access_token={0}&openid={1}&lang=zh_CN";

	/**通过refresh_token刷新ACCESS_TOKEN*/
	public final String OAUTH2_REFRESH_ACCESS_TOKEN="https://api.weixin.qq.com/sns/oauth2/refresh_token?appid={0}&grant_type=refresh_token&refresh_token={1}";

	/**获取微信支付沙箱环境的支付账号和密钥*/
	public final String GETSIGNKEY="https://api.mch.weixin.qq.com/sandboxnew/pay/getsignkey";
	/************微信认证登录与支付配置*******************************************/
	@Autowired
	private PaymentProperties properties;


	public WeChatUtil(PaymentProperties properties) {
		this.properties = properties;
	}
	/**
	 * 申请退款
	 *@param appid 前端微信应用id
	 *
	 * @param tradeNo
	 *            订单号
	 * @param totalFee
	 *            退款金额
	 * @return 微信返回的XML
	 * @throws Exception
	 */
	public  boolean refund(String appid,final String tradeNo, final int totalFee){
		return refund(appid,tradeNo, tradeNo, totalFee, totalFee);
	}

	public  boolean refund(String appid,final String tradeNo, final int totalFee,final int refundFee){
		return refund(appid,tradeNo, tradeNo, totalFee, refundFee);
	}

	/**
	 * 申请退款（分多次退款）
	 *@param  appid 前端微信应用id
	 *
	 * @param tradeNo
	 *            订单号
	 * @param refundNo
	 *            退款单号
	 * @param totalFee
	 *            订单总额
	 * @param refundFee
	 *            退款金额
	 * @return 微信返回的XML
	 * @throws Exception
	 */
	public boolean refund(final String appid,final String tradeNo, final String refundNo, final int totalFee, final int refundFee){
		String currTime = DateFormatUtil.format3(new Date());
		String strTime = currTime.substring(8, currTime.length());
		String nonce = strTime + Random.length(4);

		SortedMap<Object, Object> packageParams = new TreeMap<Object, Object>();
		packageParams.put("appid", appid);
		packageParams.put("mch_id", properties.getWeChatPayConfig().getPartnerId());
		packageParams.put("nonce_str", nonce);
		packageParams.put("out_trade_no", tradeNo);
		packageParams.put("out_refund_no", refundNo);
		packageParams.put("total_fee", totalFee);
		packageParams.put("refund_fee", refundFee);
		//packageParams.put("op_user_id", AppProps.get("partner_id"));
		String sign = this.createSign(properties.getWeChatPayConfig().getPartnerKey(), packageParams); // 获取签名
		packageParams.put("sign", sign);
		String xml = this.getRequestXml(packageParams); // 获取请求微信的XML
		HttpPost httpPost = new HttpPost(REFUND_URL);
		try {
			InputStream in= properties.getWeChatPayConfig().getPkcs12().getInputStream();
			KeyStore keystore = KeyStore.getInstance("PKCS12");
			keystore.load(in, properties.getWeChatPayConfig().getPartnerId().toCharArray());
			SSLContext sslContext = SSLContexts.custom().loadKeyMaterial(keystore, properties.getWeChatPayConfig().getPartnerId().toCharArray()).build();
			SSLConnectionSocketFactory sslConnection = new SSLConnectionSocketFactory(sslContext,
					new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"}, null,
					SSLConnectionSocketFactory.getDefaultHostnameVerifier()
			);
			HttpClient client = HttpClients.custom().setSSLSocketFactory(sslConnection).build();
			httpPost.setEntity(new StringEntity(xml, "UTF-8"));
			HttpResponse resp = client.execute(httpPost);
			HttpEntity entity = resp.getEntity();
			if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.error("refund:"+resp.getStatusLine().getStatusCode());
				return false;
			}
			String resource = EntityUtils.toString(entity, encoding);
			log.info("refund resource:"+resource);
			String resultXml = resource.replace("<![CDATA[", "").replace("]]>", "");
			log.debug("=====微信退款结果=====>>> " + resultXml);
			XReader xPath = XReader.of(resultXml);
			XNode returnCode = xPath.evalNode("//return_code");
			if (Objects.isNull(returnCode)) {
				throw new RuntimeException(resultXml);
			}
			if (!"SUCCESS".equalsIgnoreCase(returnCode.body())) {
				throw new RuntimeException(xPath.evalNode("//return_msg").body());
			}
			XNode resultCode = xPath.evalNode("//result_code");
			if (!"SUCCESS".equalsIgnoreCase(resultCode.body())) {
				throw new RuntimeException(xPath.evalNode("//err_code_des").body());
			}
			return true;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * 撤销微信支付
	* @param appid 微信应用appid
	 * @param payCode
	 * @return
	 */
	public boolean cancel(final String appid,final String payCode){
		String currTime = DateFormatUtil.format3(new Date());
		String strTime = currTime.substring(8, currTime.length());
		String nonce = strTime + Random.length(4);

		SortedMap<Object, Object> packageParams = new TreeMap<Object, Object>();
		packageParams.put("appid", appid);
		packageParams.put("mch_id", properties.getWeChatPayConfig().getPartnerId());
		packageParams.put("nonce_str", nonce);
		packageParams.put("out_trade_no", payCode);
		String sign = this.createSign(properties.getWeChatPayConfig().getPartnerKey(), packageParams); // 获取签名
		packageParams.put("sign", sign);
		String xml = this.getRequestXml(packageParams); // 获取请求微信的XML
		HttpPost httpPost = new HttpPost(CLOSEORDER_URL);
		try {

			HttpClient client = HttpClients.custom().build();
			httpPost.setEntity(new StringEntity(xml, "UTF-8"));
			HttpResponse resp = client.execute(httpPost);
			HttpEntity entity = resp.getEntity();
			if (resp.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.error("refund:"+resp.getStatusLine().getStatusCode());
				return false;
			}
			String resource = EntityUtils.toString(entity, encoding);
			log.info("refund resource:"+resource);
			String resultXml = resource.replace("<![CDATA[", "").replace("]]>", "");
			log.debug("=====微信退款结果=====>>> " + resultXml);
			XReader xPath = XReader.of(resultXml);
			XNode returnCode = xPath.evalNode("//return_code");
			if (Objects.isNull(returnCode)) {
				throw new RuntimeException(resultXml);
			}
			if (!"SUCCESS".equalsIgnoreCase(returnCode.body())) {
				throw new RuntimeException(xPath.evalNode("//return_msg").body());
			}
			XNode resultCode = xPath.evalNode("//result_code");
			if (!"SUCCESS".equalsIgnoreCase(resultCode.body())) {
				throw new RuntimeException(xPath.evalNode("//err_code_des").body());
			}
			return true;

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}


	/**
	 * 微信回调时，获取参数
	 *
	 * @param request
	 * @return
	 * @throws IOException
	 */
	public XReader getParametersByWeChatCallback(final HttpServletRequest request) throws IOException {
		BufferedReader reader = request.getReader();
		StringBuffer inputString = new StringBuffer();
		String line = "";
		while ((line = reader.readLine()) != null) {
			inputString.append(line);
		}
		request.getReader().close();
		log.info("微信回调时，获取参数getParametersByWeChatCallback:"+inputString.toString());
		InputStream in = new ByteArrayInputStream(inputString.toString().getBytes());
		XReader xReader =XReader.of(in);
		in.close();
		return xReader;
	}

	/**
	 * 获取预支付ID
	 *
	 * @param xml
	 * @return
	 */
	public  String sendWeChatGetPrepayId(final String xml) {
		String resultXml = null;
		String prepay_id = null;
		try {
		HttpClient client = HttpClients.custom().build();
		HttpPost httpost = new HttpPost(UNIFIED_ORDER_URL);
		httpost.setEntity(new StringEntity(xml, encoding));
		HttpResponse httpClientResponse = null;
		try {
			log.info("发送请求获取预支付id");
			httpClientResponse = client.execute(httpost);
		}catch (Exception e){
			log.error("第一次获取预支付id失败重试第一次");
			try{
				httpClientResponse = client.execute(httpost);
			}catch (Exception e2){
				log.error("第一次获取预支付id失败重试第二次");
				try{
					httpClientResponse = client.execute(httpost);
				}catch (Exception e3){
                    log.error("第二次重试失败");
					e3.printStackTrace();
				}
			}
		}
		resultXml = EntityUtils.toString(httpClientResponse.getEntity(), encoding);
		log.info("\n" + resultXml);
		InputStream in = new ByteArrayInputStream(resultXml.getBytes(encoding));
		XReader xpath =  XReader.of(in);
		XNode xNode = xpath.evalNode("//prepay_id");
		if (xNode == null) {
			return null;
		}
		prepay_id = xNode.body();
		in.close();
		} catch (Exception e) {
			log.error(e.getMessage() + "\n" + resultXml, e);
		}
		return prepay_id;
	}


	/**
	 * 【微信支付】返回给微信的参数
	 *
	 * @param return_code
	 *            返回编码
	 * @param return_msg
	 *            返回信息
	 * @return
	 */
	public  String setXML(final String return_code, final String return_msg) {
		StringBuilder sb = new StringBuilder();
		sb.append("<xml><return_code><![CDATA[").append(return_code).append("]]></return_code><return_msg><![CDATA[")
				.append(return_msg).append("]]></return_msg></xml>");
		return sb.toString();
	}

	/**
	 * 【微信支付】 将请求参数转换为xml格式的string
	 *
	 * @param parameters
	 *            请求参数
	 * @return
	 */
	public  String getRequestXml(final SortedMap<Object, Object> parameters) {
		StringBuilder sb = new StringBuilder();
		sb.append("<xml>");
		Set<Entry<Object, Object>> es = parameters.entrySet();
		Iterator<Entry<Object, Object>> it = es.iterator();
		while (it.hasNext()) {
			Entry<Object, Object> entry = it.next();
			String k = (String) entry.getKey();
			Object v = entry.getValue();
			if ("sign".equalsIgnoreCase(k)) {
				continue;
			}
			if ("attach".equalsIgnoreCase(k) || "body".equalsIgnoreCase(k)) {
				sb.append("<").append(k).append("><![CDATA[").append(v).append("]]></").append(k).append(">");
			} else {
				sb.append("<").append(k).append(">").append(v).append("</").append(k).append(">");
			}
		}
		sb.append("<sign>").append(parameters.get("sign")).append("</sign>").append("</xml>");
		return sb.toString();
	}

	/**
	 * sign签名
	 *
	 * @param partner_key
	 *            商户支付标识
	 *
	 * @param parameters
	 *            请求参数
	 * @return
	 */
	public  String createSign(final String partner_key, final SortedMap<Object, Object> parameters) {
		StringBuffer sb = new StringBuffer();
		Set<Entry<Object, Object>> es = parameters.entrySet();
		Iterator<Entry<Object, Object>> it = es.iterator();
		while (it.hasNext()) {
			Entry<Object, Object> entry = it.next();
			String k = (String) entry.getKey();
			Object v = entry.getValue();
			if (null != v && !"".equals(v) && !"sign".equals(k) && !"key".equals(k)) {
				sb.append(k).append("=").append(v).append("&");
			}
		}
		sb.append("key=").append(partner_key);
		String sign = DigestUtils.md5Hex(sb.toString().getBytes());
		return sign.toUpperCase();
	}

	/**
	 * 微信支付签名
	 *
	 * @return String
	 */
	public Tips wxCreateSign(String ipAddress, String openid,String appid, Long fee, String userAgent,
							 String payCode, String body, String attach, WeChatUtil weChatUtil){
		if (StringUtils.isEmpty(openid)) {
			return Tips.of("-1","用户信息为空！");
		}
		if (fee < 1) {
			return Tips.of("-1","您输入的金额不正确！");
		}

		String currTime = DateFormatUtil.format3(new Date());
		String strTime = currTime.substring(8, currTime.length());
		String nonce = strTime + Random.length(4);
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MINUTE, weChatUtil.getProperties().getWeChatPayConfig().getTimeoutExpress());// 设置6分钟过期
		String timeExpire = DateFormatUtil.format3(cal.getTime());
		SortedMap<Object, Object> packageParams = new TreeMap<Object, Object>();
		packageParams.put("appid", appid);
		packageParams.put("mch_id", weChatUtil.getProperties().getWeChatPayConfig().getPartnerId());
		packageParams.put("nonce_str", nonce);// 随机串
		packageParams.put("body", body);// 商品描述
		packageParams.put("attach", attach);//自定义附加数据
		packageParams.put("out_trade_no", payCode);// 商户业务订单号
		packageParams.put("total_fee", fee);// 微信支付金额单位为（分）
		packageParams.put("time_expire", timeExpire);
		packageParams.put("spbill_create_ip", ipAddress);// 订单生成的机器ip
		packageParams.put("notify_url", weChatUtil.getProperties().getWeChatPayConfig().getNotifyUrl());// 支付完成后微信发给该链接信息，可以判断会员是否支付成功，改变订单状态等。
		packageParams.put("trade_type", "JSAPI");
		packageParams.put("openid", openid);
		String sign = weChatUtil.createSign(weChatUtil.getProperties().getWeChatPayConfig().getPartnerKey(), packageParams); // 获取签名
		packageParams.put("sign", sign);
		log.info("=================获取预支付ID===============");
		String xml = weChatUtil.getRequestXml(packageParams); // 获取请求微信的XML
		String prepayId = weChatUtil.sendWeChatGetPrepayId(xml);
		if (com.leon.microx.util.StringUtils.isEmpty(prepayId)) {
			return Tips.of("-1","微信预支付出错");
		}
		log.info("=================微信预支付成功，响应到JSAPI完成微信支付===============");
		SortedMap<Object, Object> finalpackage = new TreeMap<Object, Object>();
		finalpackage.put("appId", appid);
		String timestamp = Long.toString(System.currentTimeMillis() / 1000);
		finalpackage.put("timeStamp", timestamp);
		finalpackage.put("nonceStr", nonce);
		String packages = "prepay_id=" + prepayId;
		finalpackage.put("package", packages);
		finalpackage.put("signType", "MD5");

		Map<String, String> signMap = new HashMap();
		signMap.put("appid", appid);
		signMap.put("timeStamp", timestamp);
		signMap.put("nonceStr", nonce);
		signMap.put("packageValue", packages);
		signMap.put("sign", weChatUtil.createSign(weChatUtil.getProperties().getWeChatPayConfig().getPartnerKey(), finalpackage));
		signMap.put("orderId", payCode);
		char agent = userAgent.charAt(userAgent.indexOf("MicroMessager") + 15);
		signMap.put("agent", new String(new char[]{agent}));
		return Tips.of("0", Jackson.json(signMap));
	}

	/**
	 * 将字节数组转换为十六进制字符串
	 *
	 * @param byteArray
	 * @return
	 */
/*	private  String byteToStr(final byte[] byteArray) {
		String strDigest = "";
		for (int i = 0; i < byteArray.length; i++) {
			strDigest += byteToHexStr(byteArray[i]);
		}
		return strDigest;
	}*/

	/**
	 * 将字节转换为十六进制字符串
	 *
	 * @param mByte
	 * @return
	 */
/*	private  String byteToHexStr(final byte mByte) {
		char[] Digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		char[] tempArr = new char[2];
		tempArr[0] = Digit[(mByte >>> 4) & 0X0F];
		tempArr[1] = Digit[mByte & 0X0F];
		String s = new String(tempArr);
		return s;
	}*/


	/**
	 * 取出一个指定长度大小的随机正整数.
	 *
	 * @param length
	 *            int 设定所取出随机数的长度。length小于11
	 * @return int 返回生成的随机数。
	 */
/*	public  int buildRandom(final int length) {
		int num = 1;
		double random = Math.random();
		if (random < 0.1) {
			random = random + 0.1;
		}
		for (int i = 0; i < length; i++) {
			num = num * 10;
		}
		return (int) ((random * num));
	}*/

/*	public String getTempSignkey(){
		SortedMap<Object, Object> postParamMap = new TreeMap<Object, Object>();
		//商户号	mch_id	是	1305638280	String(32)	微信支付分配的微信商户号
		//随机字符串	nonce_str	是	5K8264ILTKCH16CQ2502SI8ZNMTM67VS	String(32)	随机字符串，不长于32位
		//签名	sign	是	5K8264ILTKCH16CQ2502SI8ZNMTM67VS	String(32)	签名值
		postParamMap.put("mch_id", properties.getWeChatPay().getLhiot().getPartnerId());
		postParamMap.put("nonce_str", Random.length(20));
		String finalsign = this.createSign(properties.getWeChatPay().getLhiot().getPartnerKey(), postParamMap);
		postParamMap.put("sign", finalsign);
		String xml = this.getRequestXml(postParamMap); // 获取请求微信的XML
		return this.sendWeChatGetTempSignkey(xml);
	}*/
	
	/**
	 * 
	 *
	 * @param xml
	 * @return
	 */
	/*public  String sendWeChatGetTempSignkey(final String xml) {
		String resultXml = null;
		try {
		HttpClient client = HttpClients.custom().build();
		HttpPost httpost = new HttpPost(GETSIGNKEY);
		httpost.setEntity(new StringEntity(xml, encoding));
		HttpResponse httpClientResponse = null;
		try {
			log.info("发送请求获取沙箱环境商户支付临时parent_id");
			httpClientResponse = client.execute(httpost);
		}catch (Exception e){
			log.error("获取沙箱环境商户支付临时parent_id",e);
		}
		resultXml = EntityUtils.toString(httpClientResponse.getEntity(), encoding);
		log.info("\n" + resultXml);
		InputStream in = new ByteArrayInputStream(resultXml.getBytes(encoding));
		XReader xpath = XReader.of(in);
		log.info(xpath.toString());
		in.close();
		XNode returnCode = xpath.evalNode("//return_code");
		if(Objects.equals("SUCCESS", returnCode.body())){
			XNode sandboxSignkey=xpath.evalNode("//sandbox_signkey");
			return sandboxSignkey.body();
		}
		//商户号	mch_id	是	1305638280	String(32)	微信支付分配的微信商户号
		//沙箱密钥	sandbox_signkey	否	013467007045764	String(32)	返回的沙箱密钥

		//prepay_id = xNode.body();
		
		} catch (Exception e) {
			log.error(e.getMessage() + "\n" + resultXml, e);
		}
		return "";
	}*/
}
