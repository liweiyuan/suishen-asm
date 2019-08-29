package suishen.asm.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import suishen.asm.service.HelloService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * @Author :lwy
 * @Date : 2019/8/28 16:12
 * @Description :
 */
public class ASMFactory {

    public static final String SUFIX = "$EnhancedByCc";


    public static BytecodeLoader classLoader = new BytecodeLoader();

    /**
     * 根据字节码加载class
     */
    public static class BytecodeLoader extends ClassLoader {


        public Class<?> defineClass(String className, byte[] byteCodes) {
            return super.defineClass(className, byteCodes, 0, byteCodes.length);
        }
    }


    @SuppressWarnings("unchecked")
    protected static <T> Class<T> getEnhancedClass(Class<T> clazz) {

        String enhancedClassName = clazz.getName() + SUFIX;
        try {
            return (Class<T>) classLoader.loadClass(enhancedClassName);
        } catch (ClassNotFoundException e) {
            //classNotFound

            ClassReader reader = null;
            try {
                reader = new ClassReader(clazz.getName());
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new ClassAdapter(enhancedClassName, clazz, writer);
            reader.accept(visitor, 0);
            byte[] byteCodes = writer.toByteArray();
            writeClass(enhancedClassName, byteCodes);

            Class<T> result = (Class<T>) classLoader.defineClass(enhancedClassName, byteCodes);
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

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        Class<HelloService> rsCls = getEnhancedClass(HelloService.class);
        rsCls.newInstance();
    }


}
