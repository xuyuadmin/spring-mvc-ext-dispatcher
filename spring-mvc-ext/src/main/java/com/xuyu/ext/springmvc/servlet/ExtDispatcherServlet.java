package com.xuyu.ext.springmvc.servlet;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.xuyu.ext.springmvc.exannotation.ExtController;
import com.xuyu.ext.springmvc.exannotation.ExtRequestMapping;
import com.xuyu.ext.springmvc.utils.ClassUtil;

/** 自定义前端控制器

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
public class ExtDispatcherServlet extends HttpServlet{

	//扫包范围
	private  String packageName="com.xuyu.ext.springmvc.controller";
	//springmvc容器对象 key为类名id，value为类对象
	private ConcurrentHashMap<String, Object>springmvcBeans=new ConcurrentHashMap<String, Object>();
	//springmvc容器对象 key为请求地址，value为类对象
	private ConcurrentHashMap<String, Object>urlBeans=new ConcurrentHashMap<String, Object>();
	//springmvc 容器对象 key为请求地址，value为方法名称
	private ConcurrentHashMap<String, String>urlMethods=new ConcurrentHashMap<String, String>();

	@Override
	public void init() throws ServletException {
		//1.获取当前包下的所有类
		List<Class<?>> classes = ClassUtil.getClasses(packageName);
		//2.判断类上是否有注解，使用Java反射机制循环遍历方法是否有注解，进行封装url和方法对应
		try {
			findClassMvcAnnotation(classes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//3.将url映射和方法进行关联
		handlerMapping();
	}
	public void findClassMvcAnnotation(List<Class<?>> classes) throws Exception {
		for (Class<?> classInfo : classes) {
			//判断类上是否加了注解
			ExtController extControllerAnnotation = classInfo.getDeclaredAnnotation(ExtController.class);
			if(extControllerAnnotation!=null) {
				//将类名转小写就是默认的类对应的beanid
				String beanId = ClassUtil.toLowerCaseFirstOne(classInfo.getSimpleName());
				//调用反射机制初始化得到实例化对象
				Object beanObject = ClassUtil.newInstance(classInfo);
				//如果类上加上注解，就把类放到map集合中
				springmvcBeans.put(beanId, beanObject);
			}
		}
	}
	//将url和方法进行关联
	public void handlerMapping() {
		//1.获取springmvcBeans bean的容器对象
		//2.遍历springmvcBeans bean容器 判断类上是否有url映射注解
		for (Map.Entry<String, Object> springmvcBeanObject : springmvcBeans.entrySet()) {
			//3.遍历所有方法上是否有url映射注解 获取bean对象
			Object beanObject = springmvcBeanObject.getValue();
			//4.判断类上是否加上了URL映射注解
			Class<? extends Object> classInfo = beanObject.getClass();
			ExtRequestMapping extRequestMapping = classInfo.getDeclaredAnnotation(ExtRequestMapping.class);
			String beanClassUrl ="";
			if(extRequestMapping!=null) {
				//获取类上的url映射地址
				beanClassUrl = extRequestMapping.value();
			}
			//4.判断方法上是否加url映射地址
			Method[] declaredMethods = classInfo.getDeclaredMethods();
			for (Method method : declaredMethods) {
				//判断方法上是否加上url映射注解
				ExtRequestMapping methodExTRequestMapping = method.getDeclaredAnnotation(ExtRequestMapping.class);
				if(methodExTRequestMapping!=null) {
					//获取方法上的url映射地址
					String beanMethodUrl = methodExTRequestMapping.value();
					//获取方法名称
					String methodName = method.getName();
					//url拼接
					String realUrl=beanClassUrl+beanMethodUrl;
					//将地址和类对象存入map集合中
					urlBeans.put(realUrl, beanObject);
					urlMethods.put(realUrl, methodName);
				}
			}
		}
	}
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		doPost(req,resp);
	}
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		//####################处理请求###########################
		//1.获取url地址
		String requestURI = req.getRequestURI();
		if(StringUtils.isEmpty(requestURI)) {
			return;
		}
		//2.从map集合中获取控制对象
		Object object = urlBeans.get(requestURI);
		if(object==null) {
			resp.getWriter().println("not found 404 url");
			return;
		}
		//3.使用url地址获取方法
		String methodName = urlMethods.get(requestURI);
		if(StringUtils.isEmpty(methodName)) {
			resp.getWriter().println("not found 404 methodName");
		}
		//4.使用java反射机制调用方法
		String methodInvokeResultPage = (String) methodInvoke(object,methodName);
		//5.使用Java反射机制获取方法返回结果
		resp.getWriter().println(methodInvokeResultPage);
		//6.调用视图转换器渲染给页面展示
		extResourceViewResolver(methodInvokeResultPage,req,resp);
		
	}
	private void extResourceViewResolver(String pageName,HttpServletRequest req,HttpServletResponse res) throws ServletException, IOException  {
		String prefix="/";
		String suffix=".jsp";
		req.getRequestDispatcher(prefix+pageName+suffix).forward(req, res);
	}
	private Object methodInvoke(Object object,String methodName) {
		try {
			Class<? extends Object> classInfo = object.getClass();
			Method method = classInfo.getMethod(methodName);
			Object invokeResult = method.invoke(object);
			return invokeResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		
	}
}
