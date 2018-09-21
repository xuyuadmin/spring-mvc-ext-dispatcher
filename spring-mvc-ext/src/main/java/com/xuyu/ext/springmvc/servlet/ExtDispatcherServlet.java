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
//【一】：创建一个前端控制器 ExtDispatcherServlet 继承HttpServlet
public class ExtDispatcherServlet extends HttpServlet{

	//定义扫包范围
	private  String packageName="com.xuyu.ext.springmvc.controller";
	/**
	 * new 3个map集合来存
	 */
	//springmvc容器对象 key为类名id，value为类对象
	private ConcurrentHashMap<String, Object>map1=new ConcurrentHashMap<String, Object>();
	//springmvc容器对象 key为请求地址，value为类对象
	private ConcurrentHashMap<String, Object>map2=new ConcurrentHashMap<String, Object>();
	//springmvc 容器对象 key为请求地址，value为方法名称
	private ConcurrentHashMap<String, String>map3=new ConcurrentHashMap<String, String>();

	/**
	 * 【二】：重写HttpServlet的init()方法
	 */
	@Override
	public void init() throws ServletException {
		//1.扫包去获得当前包下的所有类
		List<Class<?>> classes = ClassUtil.getClasses(packageName);
		try {
			//2.判断类上是否加上ExtController注解
			findClassMvcAnnotation(classes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//3.将url映射和方法进行关联
		handlerMapping();
	}
	public void findClassMvcAnnotation(List<Class<?>> classes) throws Exception {
		for (Class<?> classInfo : classes) {
			ExtController extControllerAnnotation = classInfo.getDeclaredAnnotation(ExtController.class);
			//3.如果类上存在相对应的注解
			if(extControllerAnnotation!=null) {
				//①将类名首字母转小写作为beanId
				String beanId = ClassUtil.toLowerCaseFirstOne(classInfo.getSimpleName());
				//②使用java反射机制实例化对象得到beanObject
				Object beanObject = ClassUtil.newInstance(classInfo);
				//③把对象的beanId作为key，beanObject作为value存map1集合
				map1.put(beanId, beanObject);
			}
		}
	}
	//【三】：将url映射和方法进行关联
	public void handlerMapping() {
		for (Map.Entry<String, Object> map1BeanObject : map1.entrySet()) {
			//1.从map1集合中循环遍历获取beanObject实例对象
			Object beanObject = map1BeanObject.getValue();
			//2.通过beanObject实例对象得到对象类型
			Class<? extends Object> classInfo = beanObject.getClass();
			//3.通过这个对象类型去获得ExtRequestMapping注解
			ExtRequestMapping extRequestMapping = classInfo.getDeclaredAnnotation(ExtRequestMapping.class);
			String beanClassUrl ="";
			//4.如果类上存在相对应的注解
			if(extRequestMapping!=null) {
				//获取这个类上的注解的value值就是对应url映射地址
				beanClassUrl = extRequestMapping.value();
			}
			//5.根据这个类信息去获取这个类下的所有方法
			Method[] declaredMethods = classInfo.getDeclaredMethods();
			//6.循环遍历得到的方法
			for (Method method : declaredMethods) {
				//6.1根据方法名称去查找这个方法上的ExtRequestMapping注解
				ExtRequestMapping methodExTRequestMapping = method.getDeclaredAnnotation(ExtRequestMapping.class);
				//6.2.如果方法上有ExtRequestMapping注解
				if(methodExTRequestMapping!=null) {
					//1.获得这个方法上注解的value值就是对应的url映射地址
					String beanMethodUrl = methodExTRequestMapping.value();
					//2.获取方法名称
					String methodName = method.getName();
					//3.url拼接
					String realUrl=beanClassUrl+beanMethodUrl;
					//4.将地址和类对象实例存入map2集合中
					map2.put(realUrl, beanObject);
					//5.将地址和方法名称存map3集合中
					map3.put(realUrl, methodName);
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
		//2.从map2集合中使用url地址获取对象实例
		Object object = map2.get(requestURI);
		if(object==null) {
			resp.getWriter().println("not found 404 url");
			return;
		}
		//3.从map3集合中使用url地址获取方法名称
		String methodName = map3.get(requestURI);
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
			//1.获取对象类型
			Class<? extends Object> classInfo = object.getClass();
			//2.通过对象类型获得方法名称
			Method method = classInfo.getMethod(methodName);
			//通过方法名称反射获取方法
			Object invokeResult = method.invoke(object);
			//返回方法结果
			return invokeResult;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		
	}
}
