package suishen.asm.asm;

import org.objectweb.asm.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * @Author :lwy
 * @Date : 2019/8/28 15:11
 * @Description :
 */
public class ClassAdapter extends ClassVisitor implements Opcodes {

   public static final String INIT = "<init>";
    private ClassWriter classWriter;
    private String originalClassName;
    private String enhancedClassName;
    private Class<?> originalClass;


    public ClassAdapter(String enhancedClassName,
                        Class<?> targetClass, ClassWriter writer) {
        super(Opcodes.ASM5, writer);
        this.classWriter = writer;
        this.originalClassName = targetClass.getName();
        this.enhancedClassName = enhancedClassName;
        this.originalClass = targetClass;
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

        cv.visit(version, Opcodes.ACC_PUBLIC, toAsmCls(enhancedClassName), signature, name, interfaces);
    }


    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        //删除所有方法
        return null;
    }

    @Override
    public void visitEnd() {
       //* // 如果originalClass定义了私有成员变量，那么直接在visitMethod中复制originalClass的<init>会报错。
        // 创建<init>并调用父类的<init>
        // 调用originalClassName的<init>方法，否则class不能实例化

        MethodVisitor mvInit = classWriter.visitMethod(ACC_PUBLIC, INIT, "()V", null, null);
        //push this variable

        mvInit.visitVarInsn(ALOAD, 0);
        mvInit.visitMethodInsn(Opcodes.INVOKESPECIAL, toAsmCls(originalClassName), INIT, "()V");
        mvInit.visitInsn(Opcodes.RETURN);
        // this code uses a maximum of one stack element and one local variable
        mvInit.visitMaxs(0, 0);
        mvInit.visitEnd();

        //获取所有方法，重写(main和Object方法除外)
        Method[] methods = originalClass.getMethods();
        for (Method method : methods) {
            if (!needOverride(method)) {
                return;
            }
            // mt.toString() == ()Ljava/lang/String
            Type mt = Type.getType(method);
            //生成打印信息
            StringBuilder methodInfo = new StringBuilder(originalClassName);
            methodInfo.append(".").append(method.getName());
            methodInfo.append("|");

            Class<?>[] parameterTypes = method.getParameterTypes();

            for (Class<?> t : parameterTypes) {
                methodInfo.append(t.getName()).append(",");
            }
            if (parameterTypes.length > 0) {
                methodInfo.deleteCharAt(methodInfo.length() - 1);
            }

            //方法描述
            MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, method.getName(), mt.toString(), null, null);

            //insert code here (before)
            doMethodBefore(methodVisitor, methodInfo.toString());

            int i = 0;
            //如果不是静态方法 load this对象
            if (!Modifier.isStatic(method.getModifiers())) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, i++);
            }

            //StringBuilder sb = new StringBuilder(m.getName());
            // load 出方法的所有参数

            for (Class<?> tCls : method.getParameterTypes()) {
                Type t = Type.getType(tCls);
                //sb.append(loadCode(t)).append(",");
                //sb.append(loadCode(t)).append(",");
                methodVisitor.visitVarInsn(loadCode(t), i++);
                // long和double 用64位表示，要后移一个位置，否则会报错
                if (t.getSort() == Type.LONG || t.getSort() == Type.DOUBLE) {
                    i++;
                }
            }

            //方法属性全名
            String declaringCls = toAsmCls(method.getDeclaringClass().getName());
            //super.xxx()
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, declaringCls, method.getName(), mt.toString());

            //处理返回值类型

            Type rt = Type.getReturnType(method);
            if (rt.toString().equals("V")) {
                doMethodAfter(methodVisitor, methodInfo.toString());
                methodVisitor.visitInsn(Opcodes.RETURN);
            } else {
                // 把return xxx() 转变成 ： Object o = xxx(); return o;
                // store/load 存储并载入变量
                int storeCode = storeCode(rt);
                int loadCode = loadCode(rt);
                int returnCode = rtCode(rt);

                methodVisitor.visitVarInsn(storeCode, i);
                doMethodAfter(methodVisitor, methodInfo.toString());
                methodVisitor.visitVarInsn(loadCode, i);
                methodVisitor.visitInsn(returnCode);
            }
            methodVisitor.visitMaxs(i, ++i);
            methodVisitor.visitEnd();
        }
        cv.visitEnd();
    }


    private static void doMethodAfter(MethodVisitor methodVisitor, String methodInfo) {
        methodVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        methodVisitor.visitLdcInsn("after method : " + methodInfo);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V");

    }


    /**
     * 方法进入之前执行
     *
     * @param methodVisitor
     * @param methodInfo
     */
    private static void doMethodBefore(MethodVisitor methodVisitor, String methodInfo) {
        methodVisitor.visitFieldInsn(GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");
        methodVisitor.visitLdcInsn("before method : " + methodInfo);
        methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        // 或者直接调用静态方法
        // mWriter.visitLdcInsn(methodInfo);
        // mWriter.visitMethodInsn(INVOKESTATIC,toAsmCls(TxHandler.class.getName()),"before","(Ljava/lang/String;)V");

    }


    private static int storeCode(Type type) {
        int sort = type.getSort();
        switch (sort) {
            case Type.ARRAY:
                sort = ASTORE;
                break;
            case Type.BOOLEAN:
                sort = ISTORE;
                break;
            case Type.BYTE:
                sort = ISTORE;
                break;
            case Type.CHAR:
                sort = ISTORE;
                break;
            case Type.DOUBLE:
                sort = DSTORE;
                break;
            case Type.FLOAT:
                sort = FSTORE;
                break;
            case Type.INT:
                sort = ISTORE;
                break;
            case Type.LONG:
                sort = LSTORE;
                break;
            case Type.OBJECT:
                sort = ASTORE;
                break;
            case Type.SHORT:
                sort = ISTORE;
                break;
            default:
                break;
        }
        return sort;

    }


    private static int loadCode(Type type) {
        int sort = type.getSort();
        switch (sort) {
            case Type.ARRAY:
                sort = ALOAD;
                break;
            case Type.BOOLEAN:
                sort = ILOAD;
                break;
            case Type.BYTE:
                sort = ILOAD;
                break;
            case Type.CHAR:
                sort = ILOAD;
                break;
            case Type.DOUBLE:
                sort = DLOAD;
                break;
            case Type.FLOAT:
                sort = FLOAD;
                break;
            case Type.INT:
                sort = ILOAD;
                break;
            case Type.LONG:
                sort = LLOAD;
                break;
            case Type.OBJECT:
                sort = ALOAD;
                break;
            case Type.SHORT:
                sort = ILOAD;
                break;
            default:
                break;
        }
        return sort;
    }


    private static int rtCode(Type type) {
        int sort = type.getSort();
        switch (sort) {
            case Type.ARRAY:
                sort = ARETURN;
                break;
            case Type.BOOLEAN:
                sort = IRETURN;
                break;
            case Type.BYTE:
                sort = IRETURN;
                break;
            case Type.CHAR:
                sort = IRETURN;
                break;
            case Type.DOUBLE:
                sort = DRETURN;
                break;
            case Type.FLOAT:
                sort = FRETURN;
                break;
            case Type.INT:
                sort = IRETURN;
                break;
            case Type.LONG:
                sort = LRETURN;
                break;
            case Type.OBJECT:
                sort = ARETURN;
                break;
            case Type.SHORT:
                sort = IRETURN;
                break;
            default:
                break;
        }
        return sort;

    }


    private boolean needOverride(Method method) {
        //Object类方法不重写
        if (method.getDeclaringClass().getName().equalsIgnoreCase(Object.class.getName())) {
            return false;
        }
        // main方法
        if (Modifier.isPublic(method.getModifiers())
                && Modifier.isStatic(method.getModifiers())
                && method.getReturnType().getName().equals("void")
                && method.getName().equals("main")) {
            return false;

        }
        return true;
    }

    private String toAsmCls(String enhancedClassName) {
        return enhancedClassName.replace(".", "/");
    }


    /*@Override
    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        cv.visit(version, Opcodes.ACC_PUBLIC,
                toAsmCls(enhancedClassName), signature, name,interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
        // 拷贝所有属性   可使用java反射给属性赋值（生成class后newInstance在赋值）
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        // 删除所有方法
        return null;
    }

    private static String toAsmCls(String className) {
        return className.replace('.', '/');
    }

    private static void doBefore(MethodVisitor mWriter,String methodInfo) {
        mWriter.visitFieldInsn(GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");
        mWriter.visitLdcInsn("before method : " + methodInfo);
        mWriter.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
        // 或者直接调用静态方法
        // mWriter.visitLdcInsn(methodInfo);
        // mWriter.visitMethodInsn(INVOKESTATIC,toAsmCls(TxHandler.class.getName()),"before","(Ljava/lang/String;)V");

    }

    private static void doAfter(MethodVisitor mWriter,String methodInfo) {
        mWriter.visitFieldInsn(GETSTATIC,"java/lang/System","out","Ljava/io/PrintStream;");
        mWriter.visitLdcInsn("after method : " + methodInfo);
        mWriter.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
                "(Ljava/lang/String;)V");
    }

    public static boolean needOverride(Method m) {
        // object类本身的方法不做重写
        if (m.getDeclaringClass().getName().equals(Object.class.getName())) {
            return false;
        }
        // "main" 方法不做重写
        if (Modifier.isPublic(m.getModifiers())
                && Modifier.isStatic(m.getModifiers())
                && m.getReturnType().getName().equals("void")
                && m.getName().equals("main")) {
            return false;
        }

        return true;
    }

    @Override
    public void visitEnd() {
        // 如果originalClass定义了私有成员变量，那么直接在visitMethod中复制originalClass的<init>会报错。
        // 创建<init>并调用父类的<init>
        // 调用originalClassName的<init>方法，否则class不能实例化
        MethodVisitor mvInit = classWriter.visitMethod(ACC_PUBLIC, INIT, "()V", null, null);
        // pushes the 'this' variable
        mvInit.visitVarInsn(ALOAD, 0);
        mvInit.visitMethodInsn(INVOKESPECIAL, toAsmCls(originalClassName), INIT, "()V");
        mvInit.visitInsn(RETURN);
        // this code uses a maximum of one stack element and one local variable
        mvInit.visitMaxs(0, 0);
        mvInit.visitEnd();

        // 获取所有方法，并重写(main方法 和 Object的方法除外)
        Method[] methods = originalClass.getMethods();
        for(Method m : methods) {
            if(!needOverride(m)) {
                continue;
            }
            // mt.toString() == ()Ljava/lang/String
            Type mt = Type.getType(m);

            // 生成打印信息
            StringBuilder methodInfo = new StringBuilder(originalClassName);
            methodInfo.append(".").append(m.getName());
            methodInfo.append("|");

            Class<?>[] paramTypes = m.getParameterTypes();
            for(Class<?> t : paramTypes) {
                methodInfo.append(t.getName()).append(",");
            }
            if(paramTypes.length > 0) {
                methodInfo.deleteCharAt(methodInfo.length() - 1);
            }

            // 方法 description
            MethodVisitor mWriter = classWriter.visitMethod(ACC_PUBLIC, m.getName(), mt.toString(), null, null);
            // insert code here (before)
            doBefore(mWriter,methodInfo.toString());

            int i = 0;
            // 如果不是静态方法 load this对象
            if(!Modifier.isStatic(m.getModifiers())) {
                mWriter.visitVarInsn(ALOAD, i++);
            }
            //StringBuilder sb = new StringBuilder(m.getName());
            // load 出方法的所有参数
            for(Class<?> tCls : m.getParameterTypes()) {
                Type t = Type.getType(tCls);
                //sb.append(loadCode(t)).append(",");
                mWriter.visitVarInsn(loadCode(t), i++);
                // long和double 用64位表示，要后移一个位置，否则会报错
                if(t.getSort() == Type.LONG || t.getSort() == Type.DOUBLE) {
                    i++;
                }
            }

            // 方法所属类全名
            String declaringCls = toAsmCls(m.getDeclaringClass().getName());
            // super.xxx();
            mWriter.visitMethodInsn(INVOKESPECIAL,declaringCls,m.getName(),mt.toString());

            // 处理返回值类型
            Type rt = Type.getReturnType(m);
            // 没有返回值
            if(rt.toString().equals("V")) {
                doAfter(mWriter,methodInfo.toString());
                mWriter.visitInsn(RETURN);
            }
            // 把return xxx() 转变成 ： Object o = xxx(); return o;
            // store/load 存储并载入变量
            else {
                int storeCode = storeCode(rt);
                int loadCode = loadCode(rt);
                int returnCode = rtCode(rt);

                mWriter.visitVarInsn(storeCode, i);
                doAfter(mWriter,methodInfo.toString());
                mWriter.visitVarInsn(loadCode, i);
                mWriter.visitInsn(returnCode);
            }

            mWriter.visitMaxs(i, ++i);
            mWriter.visitEnd();
        }
        cv.visitEnd();
    }

    public static int storeCode(Type type) {
        int sort = type.getSort();
        switch (sort) {
            case Type.ARRAY:
                sort = ASTORE;
                break;
            case Type.BOOLEAN:
                sort = ISTORE;
                break;
            case Type.BYTE:
                sort = ISTORE;
                break;
            case Type.CHAR:
                sort = ISTORE;
                break;
            case Type.DOUBLE:
                sort = DSTORE;
                break;
            case Type.FLOAT:
                sort = FSTORE;
                break;
            case Type.INT:
                sort = ISTORE;
                break;
            case Type.LONG:
                sort = LSTORE;
                break;
            case Type.OBJECT:
                sort = ASTORE;
                break;
            case Type.SHORT:
                sort = ISTORE;
                break;
            default:
                break;
        }
        return sort;
    }

    public static int loadCode(Type type) {
        int sort = type.getSort();
        switch (sort) {
            case Type.ARRAY:
                sort = ALOAD;
                break;
            case Type.BOOLEAN:
                sort = ILOAD;
                break;
            case Type.BYTE:
                sort = ILOAD;
                break;
            case Type.CHAR:
                sort = ILOAD;
                break;
            case Type.DOUBLE:
                sort = DLOAD;
                break;
            case Type.FLOAT:
                sort = FLOAD;
                break;
            case Type.INT:
                sort = ILOAD;
                break;
            case Type.LONG:
                sort = LLOAD;
                break;
            case Type.OBJECT:
                sort = ALOAD;
                break;
            case Type.SHORT:
                sort = ILOAD;
                break;
            default:
                break;
        }
        return sort;
    }

    public static int rtCode(Type type) {
        int sort = type.getSort();
        switch (sort) {
            case Type.ARRAY:
                sort = ARETURN;
                break;
            case Type.BOOLEAN:
                sort = IRETURN;
                break;
            case Type.BYTE:
                sort = IRETURN;
                break;
            case Type.CHAR:
                sort = IRETURN;
                break;
            case Type.DOUBLE:
                sort = DRETURN;
                break;
            case Type.FLOAT:
                sort = FRETURN;
                break;
            case Type.INT:
                sort = IRETURN;
                break;
            case Type.LONG:
                sort = LRETURN;
                break;
            case Type.OBJECT:
                sort = ARETURN;
                break;
            case Type.SHORT:
                sort = IRETURN;
                break;
            default:
                break;
        }
        return sort;
    }*/

}
