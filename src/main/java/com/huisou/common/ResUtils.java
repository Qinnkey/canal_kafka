package com.huisou.common;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * 统一消息回复工具类.
 * caoxt
 * 提供成功异常等返回信息.
 */
public class ResUtils {

	//成功，编码100
	public static String sucCode = "100";
	//失败，编码404
	public static String exceCode = "404";
	
	//token失效，编码504
	public static String tokenErrCode = "504";
	//token失效
	public static String tokenErrMsg = "token过期";
	
	//查询手机号码错误
	public static String error_phone="601";
	
	//成功，编码100
	public static String sucMsg = "成功";
	//失败，编码404
	public static String exceMsg = "异常";
	//账号被禁用
	public static String lockUser="账号被禁用，请与管理员联系";
	//账号或者密码错误
	public static String errUser="账号或密码错误";

	public static String CODE = "code";
	public static String MSG = "msg";
	public static String DATA = "data";
	
	public static String okRes(){
		Map map = new HashMap();
		map.put(CODE,sucCode);
		map.put(MSG,sucMsg);
		return JSON.toJSONString(map);
	}
	
	public static String okRes(Object obj){
		
		Map map = new HashMap();
		map.put(CODE,sucCode);
		map.put(MSG,sucMsg);
		map.put(DATA,obj);
		
		try {
			return JacksonUtil.toJson(map);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		return JSON.toJSONString(map, SerializerFeature.WriteMapNullValue,SerializerFeature.WriteDateUseDateFormat);
		return execRes();
	}
	
	public static String execRes(Object obj){
		Map map = new HashMap();
		map.put(CODE,exceCode);
		map.put(MSG,exceMsg);
		map.put(DATA,obj);
		return JSON.toJSONString(map);
	}
	
	public static String execRes(){
		Map map = new HashMap();
		map.put(CODE,exceCode);
		map.put(MSG,exceMsg);
		return JSON.toJSONString(map);
	}
	
	public static String errRes(String code,String msg){
		Map map = new HashMap();
		map.put(CODE,code);
		map.put(MSG,msg);
		return JSON.toJSONString(map);
	}
	
	public static String tokenErrRes(){
		Map map = new HashMap();
		map.put(CODE,tokenErrCode);
		map.put(MSG,tokenErrMsg);
		return JSON.toJSONString(map);
	}
}
