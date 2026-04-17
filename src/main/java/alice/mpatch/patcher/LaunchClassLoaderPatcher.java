package alice.mpatch.patcher;

import alice.Platform;
import alice.log.Logger;
import alice.util.BytecodeUtil;
import org.objectweb.asm.*;

public class LaunchClassLoaderPatcher implements Opcodes {
    public static byte[] transform(byte[] classBytes) {
        Logger.MAIN.info("Patching LaunchClassLoader.");
        return BytecodeUtil.patchClass(classBytes, cw -> new ClassVisitor(Platform.ASM_LEVEL, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                if (name.equals("registerTransformer")) {
                    Logger.MAIN.info("Get registerTransformer.");
                    return new MethodVisitor(Platform.ASM_LEVEL, cv.visitMethod(access, name, descriptor, signature, exceptions)) {
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
        });
    }
}
