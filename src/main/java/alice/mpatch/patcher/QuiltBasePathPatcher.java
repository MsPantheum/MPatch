package alice.mpatch.patcher;

import alice.Platform;
import alice.util.BytecodeUtil;
import alice.util.FileUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;

public class QuiltBasePathPatcher implements Opcodes {
    public static byte[] transform(byte[] classBytes) {
        byte[] data = BytecodeUtil.patchClass(classBytes, cw -> new ClassVisitor(Platform.ASM_LEVEL, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

                if ("openUrlInputStream".equals(name)) {
                    return new MethodVisitor(Platform.ASM_LEVEL, mv) {

                        @Override
                        public void visitInsn(int opcode) {
                            if (opcode == ARETURN) {

                                super.visitMethodInsn(INVOKESTATIC, "alice/injector/ClassPatcher", "shouldRunTransformers", "()Z", false);
                                Label if_label = new Label();
                                super.visitJumpInsn(IFEQ, if_label);
                                super.visitMethodInsn(INVOKEVIRTUAL, "java/io/InputStream", "readAllBytes", "()[B", false);
                                //Get raw class bytes
                                super.visitVarInsn(ALOAD, 0);
                                BytecodeUtil.toString(mv);
                                super.visitInsn(ICONST_1);
                                super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
                                //Get the class name.
                                super.visitMethodInsn(INVOKESTATIC, "alice/injector/ClassPatcher", "runTransformers", "([BLjava/lang/String;)[B", false);
                                super.visitMethodInsn(INVOKESTATIC, "alice/util/IOUtil", "getInputStream", "([B)Ljava/io/InputStream;", false);

                                super.visitLabel(if_label);
                                super.visitFrame(F_SAME1, 0, null, 2, new Object[]{"java/io/InputStream", "I"});
                            }
                            super.visitInsn(opcode);
                        }
                    };
                }
                return mv;
            }
        });
        FileUtil.write("/home/Alice/IdeaProjects/AliceAPI/QuiltBasePath.class", data);
        return data;
    }
}
