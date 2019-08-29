package suishen.asm.listener;

import net.bytebuddy.asm.Advice;

/**
 * @Author :lwy
 * @Date : 2019/8/29 14:46
 * @Description :
 */
public class BeanInterceptor {

    @Advice.OnMethodEnter
    public static void enter(
            @Advice.Origin("#t") String className,
            @Advice.Origin("#m") String methodName,
            @Advice.AllArguments Object[] arguments,
            @Advice.Local("start") long start) {
        start = System.currentTimeMillis();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void exit(
            @Advice.Origin("#t") String className,
            @Advice.Origin("#m") String methodName,
            @Advice.AllArguments Object[] arguments,
            @Advice.Thrown Throwable t,
            @Advice.Local("start") long start) {

        long cost = System.currentTimeMillis() - start;

        System.err.println("cost: " + cost + "ms");
    }
}
