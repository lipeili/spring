package com.lagou.edu.factory;

import com.alibaba.druid.util.StringUtils;
import com.lagou.edu.annotation.Autowired;
import com.lagou.edu.annotation.Service;
import com.lagou.edu.annotation.Transational;
import com.lagou.edu.utils.ConnectionUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.reflections.Reflections;

import javax.servlet.http.HttpServlet;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.lang.reflect.*;
import java.util.*;

/**
 * @author 应癫
 *
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String,Object> map = new HashMap<>();  // 存储对象


    /**
     * 加载xml中的
     */
    static {
        // 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
        // 加载xml
        InputStream resourceAsStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析xml
        SAXReader saxReader = new SAXReader();
        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();
            List<Element> beanList = rootElement.selectNodes("//bean");
            for (int i = 0; i < beanList.size(); i++) {
                Element element =  beanList.get(i);
                // 处理每个bean元素，获取到该元素的id 和 class 属性
                String id = element.attributeValue("id");        // accountDao
                String clazz = element.attributeValue("class");  // com.lagou.edu.dao.impl.JdbcAccountDaoImpl
                // 通过反射技术实例化对象
                Class<?> aClass = Class.forName(clazz);
                Object o = aClass.newInstance();  // 实例化之后的对象

                // 存储到map中待用
                map.put(id,o);

            }

            // 实例化完成之后维护对象的依赖关系，检查哪些对象需要传值进入，根据它的配置，我们传入相应的值
            // 有property子元素的bean就有传值需求
            List<Element> propertyList = rootElement.selectNodes("//property");
            // 解析property，获取父元素
            for (int i = 0; i < propertyList.size(); i++) {
                Element element =  propertyList.get(i);   //<property name="AccountDao" ref="accountDao"></property>
                String name = element.attributeValue("name");
                String ref = element.attributeValue("ref");

                // 找到当前需要被处理依赖关系的bean
                Element parent = element.getParent();

                // 调用父元素对象的反射功能
                String parentId = parent.attributeValue("id");
                Object parentObject = map.get(parentId);
                // 遍历父对象中的所有方法，找到"set" + name
                Method[] methods = parentObject.getClass().getMethods();
                for (int j = 0; j < methods.length; j++) {
                    Method method = methods[j];
                    if(method.getName().equalsIgnoreCase("set" + name)) {  // 该方法就是 setAccountDao(AccountDao accountDao)
                        method.invoke(parentObject,map.get(ref));
                    }
                }

                // 把处理之后的parentObject重新放到map中
                map.put(parentId,parentObject);
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    /**
     * 加载注解的bean，这里实现了service其他的其实一样
     */
    static {
        Reflections reflections = new Reflections("com.lagou.edu");
        Set<Class<?>> types =  reflections.getTypesAnnotatedWith(Service.class);
        types.forEach(clazz -> {
            // Service注解的类加入工厂
            Service service = clazz.getAnnotation(Service.class);
            Class<?>[] clazzs = clazz.getInterfaces();
            Object instance = null;

            try {
                instance = clazz.newInstance();
                if (StringUtils.isEmpty(service.value())) {
                    if (null != clazzs && clazzs.length > 0) {
                        // 单接口的service
                        String name = clazzs[0].getName();
                        map.put(toHump(name),instance);
                    } else {
                        map.put(toHump(clazz.getName()),instance);
                    }
                } else {
                    map.put(service.value(),instance);
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

        });
    }

    static {
        Reflections reflections = new Reflections("com.lagou.edu");
        Set<Class<?>> types =  reflections.getTypesAnnotatedWith(Service.class);
        types.forEach(clazz -> {


            // 类中属性带autowired的属性装配值
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                Autowired autowired = field.getAnnotation(Autowired.class);

                if (null == autowired) {
                    continue;
                }
                String beanName = autowired.value();
                field.setAccessible(true);

                String declarClass = getBeanName(field.getDeclaringClass());
                try {
                    if (!StringUtils.isEmpty(beanName)) {
                        field.set(map.get(declarClass),map.get(beanName));
                    } else {
                        field.set(map.get(declarClass),map.get(field.getName()));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static  Object getBean(String id) {
        Object o = map.get(id);
        if (o.getClass().getAnnotation(Transational.class) != null) {
            Object finalO = o;
            o = Proxy.newProxyInstance(o.getClass().getClassLoader(), o.getClass().getInterfaces(), (proxy, method, args) -> {
                ConnectionUtils.getCurrentThreadConn().setAutoCommit(false);
                Object obj = method.invoke(finalO,args);
                ConnectionUtils.getCurrentThreadConn().commit();

                return obj;
            });
        }
        return o;
    }

    public static  String toHump(String name) {
        String[] nameSeg = name.split("\\.");
        name = nameSeg[nameSeg.length - 1];
        String firstChar = name.substring(0,1);
        return firstChar.toLowerCase().concat(name.substring(1));
    }

    public static  String getBeanName(Class clazz) {
        Class[] classes = clazz.getInterfaces();
        if (null != classes && classes.length > 0) {
            return toHump(classes[0].getName());
        }

        return toHump(clazz.getName());
    }


}
