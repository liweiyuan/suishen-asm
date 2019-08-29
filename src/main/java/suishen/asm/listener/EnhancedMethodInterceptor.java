package suishen.asm.listener;

import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @Author :lwy
 * @Date : 2019/8/29 11:15
 * @Description :
 */
public class EnhancedMethodInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {

        long startTime = System.currentTimeMillis();
        Object invoke = methodProxy.invokeSuper(o, objects);
        System.err.println("cost time: " + (System.currentTimeMillis() - startTime) + "ms");
        return invoke;
    }
}
