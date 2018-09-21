package com.xuyu.ext.springmvc;
/**
 * 手写springmvc注解版本原理
 * 1.创建一个前端控制器（）ExtDispatcherServlet 拦截所有请求
 * 2.初始化操作 重写servlet 的init 方法
 * 		2.1开始扫包，将扫包范围的所有类注入到springmvc容器里面，存放map集合中key为默认类名小写，value为对象
 * 		2.2将URL映射和方法进行关联
 * 			2.2.1判断类上是否有注解，使用Java反射机制循环遍历方法是否有注解，进行封装url和方法对应
 * 3.处理请求 重写Get或者Post方法
 * 		3.1获取请求url，去urlBeans集合中获取实例对象，获取成功实例对象后，调用urlMethods集合获取方法名称，使用反射机制执行
 * 
 * @author Administrator
 *
 */

import java.util.concurrent.ConcurrentHashMap;

public class Test {

	//springmvc容器对象 key为类名id，value为类对象
	private ConcurrentHashMap<String, Object>springmvcBeans=new ConcurrentHashMap<String, Object>();
	//springmvc容器对象 key为请求地址，value为类名
	private ConcurrentHashMap<String, Object>urlBeans=new ConcurrentHashMap<String, Object>();
	//springmvc 容器对象 key为请求地址，value为方法名称
	private ConcurrentHashMap<String, Object>urlMethods=new ConcurrentHashMap<String, Object>();

}
