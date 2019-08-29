package suishen.asm.listener;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service;
import suishen.asm.asm.AsmEnhancedTools;

import java.util.Map;

/**
 * @Author :lwy
 * @Date : 2019/8/29 10:40
 * @Description :
 */
public class EnhancedApplicationListener implements ApplicationListener<ApplicationEvent>, Ordered {

    /**
     * The default order for the LoggingApplicationListener.
     */
    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 20;


    @Override
    public void onApplicationEvent(ApplicationEvent event) {

        if (event instanceof ContextRefreshedEvent) {
            System.out.println("==========ContextRefreshedEvent==========");
            try {
                handler((ContextRefreshedEvent) event);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }


    private void handler(ContextRefreshedEvent event) throws ClassNotFoundException {

        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event.getApplicationContext();

        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(Service.class);

        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getBeanFactory();

        for (String key : beansWithAnnotation.keySet()) {

            Object obj = beansWithAnnotation.get(key);




            //自定义asm
            //Class<?> enhancedClass = AsmEnhancedTools.getEnhancedClass(obj.getClass());

            //Class<?> aClass = AsmEnhancedTools.classLoader.loadClass(enhancedClass.getName());
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(obj.getClass());
            enhancer.setCallback(new EnhancedMethodInterceptor());

            Object enhancement = enhancer.create();

            // 消除原先的bean
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(key);

            beanFactory.removeBeanDefinition(key);

            beanDefinition.setBeanClassName(enhancement.getClass().getCanonicalName());

            beanFactory.registerBeanDefinition(key, beanDefinition);

            beanFactory.registerSingleton(key, enhancement);
        }
    }
}
