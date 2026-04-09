package alice.mpatch.patcher;

import org.objectweb.asm.*;

public class LaunchClassLoaderPatcher implements Opcodes {
    public static byte[] transform(byte[] classBytes) {
        System.out.println("Patching LaunchClassLoader.");
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (name.equals("findClass")) {
                    System.out.println("Get findClass.");
                    return new MethodVisitor(ASM5, cv.visitMethod(access, name, descriptor, signature, exceptions)) {
                        @Override
                        public void visitCode() {
                            visitVarInsn(ALOAD, 1);
                            visitLdcInsn("alice.");
                            visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
                            Label alice = new Label();
                            visitJumpInsn(IFEQ, alice);
                            visitVarInsn(ALOAD, 0);
                            visitFieldInsn(GETFIELD, "net/minecraft/launchwrapper/LaunchClassLoader", "parent", "Ljava/lang/ClassLoader;");
                            visitVarInsn(ALOAD, 1);
                            visitMethodInsn(INVOKEVIRTUAL, "java/lang/ClassLoader", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);
                            visitInsn(ARETURN);
                            visitLabel(alice);
                            super.visitCode();
                        }
                    };
                } else if (name.equals("registerTransformer")) {
                    System.out.println("Get registerTransformer.");
                    return new MethodVisitor(ASM5, cv.visitMethod(access, name, descriptor, signature, exceptions)) {
                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            super.visitTypeInsn(opcode, type);
                            if (opcode == CHECKCAST && type.equals("net/minecraft/launchwrapper/IClassTransformer")) {
                                visitMethodInsn(INVOKESTATIC, "alice/mpatch/hook/LaunchClassLoaderHook", name, "(Lnet/minecraft/launchwrapper/IClassTransformer;)Lnet/minecraft/launchwrapper/IClassTransformer;", false);
                            }
                        }
                    };
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }
}
