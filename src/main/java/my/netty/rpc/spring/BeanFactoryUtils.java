package my.netty.rpc.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.springframework.beans.factory.BeanFactoryUtils.beanNamesForTypeIncludingAncestors;
import static org.springframework.beans.factory.BeanFactoryUtils.beansOfTypeIncludingAncestors;

public class BeanFactoryUtils implements BeanFactoryAware { // spring之BeanFactoryAware接口：https://blog.csdn.net/xyw591238/article/details/51995486
    // Spring(三)Bean继续入门：https://www.cnblogs.com/liunanjava/p/4401089.html
    // 【Spring4揭秘 BeanFactory】基本容器-BeanFactory：https://blog.csdn.net/u011179993/article/details/51636742

    private static BeanFactory beanFactory;

    private static boolean isContains(String[] values, String value) {
        if(value != null && value.length() > 0 && values != null && values.length > 0) {
            for(String v : values) {
                if(value.equals(v)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static <T> T getBean(String name) { // 从BeanFactory里得到特定名称的bean，这个BeanFactory一般为XmlBeanFactory。
        if(beanFactory == null) {
            return null;
        }
        try {
            return (T) beanFactory.getBean(name);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    public static <T> T getOptionalBean(ListableBeanFactory beanFactory, String beanName, Class<T> beanType) { // 从特定的ListableBeanFactory中获取已注册的特定类型和名称的bean
        String[] allBeanNames = beanNamesForTypeIncludingAncestors(beanFactory, beanType);
        if(!isContains(allBeanNames, beanName)) {
            return null;
        }
        Map<String, T> beansOfType = beansOfTypeIncludingAncestors(beanFactory, beanType);
        return beansOfType.get(beanName);
    }

    public static <T> List<T> getBeans(ListableBeanFactory beanFactory, String[] beanNames, Class<T> beanType) { // 从特定的ListableBeanFactory中获取已注册的某种类型的多个特定名称的bean
        String[] allBeanNames = beanNamesForTypeIncludingAncestors(beanFactory, beanType);
        List<T> beans = new ArrayList<>(beanNames.length);
        for(String beanName: beanNames) {
            if(isContains(allBeanNames, beanName)) {
                beans.add(beanFactory.getBean(beanName, beanType));
            }
        }
        return Collections.unmodifiableList(beans);
    }

    /**
     * xml配置中的这行代码，会使springboot启动时调用setBeanFactory来注册beanFactory。
     * <bean id="beanFactory" class="my.netty.rpc.spring.BeanFactoryUtils"/>
     */
    @Override
    public void setBeanFactory(BeanFactory factory) throws BeansException {
        this.beanFactory = factory;
    }
}
