package suishen.asm.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @Author :lwy
 * @Date : 2019/8/28 17:14
 * @Description :
 */
public class AsmEnhancedTools {


    private static final String SUFIX = "$EnhancedByAsm";


    public  static BytecodeLoader classLoader = new BytecodeLoader();

    /**
     * 根据字节码加载class
     */
    public static class BytecodeLoader extends ClassLoader {


        public Class<?> defineClass(String className, byte[] byteCodes) {
            return super.defineClass(className, byteCodes, 0, byteCodes.length);
        }
    }


    @SuppressWarnings("unchecked")
    public static <T> Class<T> getEnhancedClass(Class<T> beanClazz) {

        String enhancedClassName = beanClazz.getName() + SUFIX;
        try {
            return (Class<T>) beanClazz.getClassLoader().loadClass(enhancedClassName);
        } catch (ClassNotFoundException e) {
            //classNotFound
            ClassReader reader = null;
            try {
                reader = new ClassReader(beanClazz.getName());
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new ClassAdapter(enhancedClassName, beanClazz, writer);
            reader.accept(visitor, 0);
            byte[] byteCodes = writer.toByteArray();

            writeClass(enhancedClassName, byteCodes);
            Class<T> result = (Class<T>) classLoader.defineClass(enhancedClassName, byteCodes);
            Thread.currentThread().getContextClassLoader();
            return result;
        }
    }


    private static void writeClass(String name, byte[] data) {
        try {
            File file = new File("E:\\csv\\" + name + ".class");
            FileOutputStream fout = new FileOutputStream(file);

            fout.write(data);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
