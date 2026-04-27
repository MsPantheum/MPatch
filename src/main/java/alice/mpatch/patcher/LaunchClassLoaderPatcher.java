package alice.mpatch.patcher;

import alice.Platform;
import alice.log.Logger;
import org.objectweb.asm.*;

import java.lang.reflect.Modifier;

public class LaunchClassLoaderPatcher implements Opcodes {
    public static byte[] transform(byte[] classBytes, String name) {
        name = name.substring(0, name.length() - 6);
        Logger.MAIN.info("Patching ".concat(name).concat("."));
        final String class_name = name;
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Platform.ASM_LEVEL, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (name.equals("registerTransformer") || name.equals("lambda$fillTransformerHolder$2")) {
                    return new MethodVisitor(Platform.ASM_LEVEL, cv.visitMethod(access, name, descriptor, signature, exceptions)) {

                        @Override
                        public void visitCode() {
                            super.visitCode();
                            if (descriptor.startsWith("(Lnet/minecraft/launchwrapper/IClassTransformer;")) {
                                super.visitVarInsn(ALOAD, Modifier.isStatic(access) ? 0 : 1);
                                super.visitMethodInsn(INVOKESTATIC, class_name, "registerTransformerHook", "(Lnet/minecraft/launchwrapper/IClassTransformer;)Lnet/minecraft/launchwrapper/IClassTransformer;", false);
                                super.visitVarInsn(ASTORE, Modifier.isStatic(access) ? 0 : 1);
                            }
                        }

                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            super.visitTypeInsn(opcode, type);
                            if (opcode == CHECKCAST && type.equals("net/minecraft/launchwrapper/IClassTransformer")) {
                                super.visitMethodInsn(INVOKESTATIC, class_name, "registerTransformerHook", "(Lnet/minecraft/launchwrapper/IClassTransformer;)Lnet/minecraft/launchwrapper/IClassTransformer;", false);
                            }
                        }
                    };
                }
                return super.visitMethod(access, name, descriptor, signature, exceptions);
            }
        };
        cr.accept(cv,0);
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC, "registerTransformerHook", "(Lnet/minecraft/launchwrapper/IClassTransformer;)Lnet/minecraft/launchwrapper/IClassTransformer;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitFieldInsn(GETSTATIC, "alice/log/Logger", "MAIN", "Lalice/log/Logger;");
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitLdcInsn("intercept transformer: ");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "alice/log/Logger", "info", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn("net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        Label label3 = new Label();
        mv.visitJumpInsn(IFNE, label3);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn("cpw.mods.fml.common.asm.transformers.DeobfuscationTransformer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        Label label4 = new Label();
        mv.visitJumpInsn(IFEQ, label4);
        mv.visitLabel(label3);
        mv.visitLineNumber(20, label3);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[]{"java/lang/String"}, 0, null);
        mv.visitMethodInsn(INVOKESTATIC, "alice/mpatch/patcher/FMLClassPatcher", "startDeobfuscation", "()V", false);
        mv.visitLabel(label4);
        mv.visitLineNumber(22, label4);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn("net.minecraftforge.fml.common.asm.transformers.PatchingTransformer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        Label label5 = new Label();
        mv.visitJumpInsn(IFNE, label5);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn("cpw.mods.fml.common.asm.transformers.PatchingTransformer");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        Label label6 = new Label();
        mv.visitJumpInsn(IFEQ, label6);
        mv.visitLabel(label5);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitMethodInsn(INVOKESTATIC, "alice/mpatch/patcher/FMLClassPatcher", "startPatching", "()V", false);
        mv.visitInvokeDynamicInsn("transform", "()Lnet/minecraft/launchwrapper/IClassTransformer;", new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false), Type.getType("(Ljava/lang/String;Ljava/lang/String;[B)[B"), new Handle(Opcodes.H_INVOKESTATIC, class_name, "lambda$registerTransformer$0", "(Ljava/lang/String;Ljava/lang/String;[B)[B", false), Type.getType("(Ljava/lang/String;Ljava/lang/String;[B)[B"));
        mv.visitInsn(ARETURN);
        mv.visitLabel(label6);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(3, 2);
        mv = cw.visitMethod(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "lambda$registerTransformer$0", "(Ljava/lang/String;Ljava/lang/String;[B)[B", null, null);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 3);
        return cw.toByteArray();
    }
}
