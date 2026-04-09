package alice.mpatch.patcher;

import org.objectweb.asm.*;

//Why doesn't this f***ing method read the cache?
public class ClassPatchManagerPatcher implements Opcodes {
    public static byte[] process(byte[] classBytes, String name) {
        String cls = name.substring(0, name.length() - 6);
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if ("getPatchedResource".equals(name)) {
                    mv = new MethodVisitor(ASM5, mv) {
                        @Override
                        public void visitCode() {
                            visitVarInsn(ALOAD, 0);
                            visitFieldInsn(GETFIELD, cls, "patchedClasses", "Ljava/util/Map;");
                            visitVarInsn(ALOAD, 1);
                            visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                            visitInsn(DUP);
                            Label l = new Label();
                            visitJumpInsn(IFNULL, l);
                            visitTypeInsn(CHECKCAST, "[B");
                            visitInsn(ARETURN);
                            visitLabel(l);
                            visitFrame(F_SAME1, 0, null, 1, new Object[]{"java/lang/Object", "java/lang/Object"});
                            visitInsn(POP);
                            super.visitCode();
                        }
                    };
                } else if ("applyPatch".equals(name)) {
                    mv.visitVarInsn(ALOAD,3);
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(1,4);
                    return super.visitMethod(access,"trueApplyPatch",descriptor,signature,exceptions);
                }
                return mv;
            }
        };
        cr.accept(cv, ClassReader.SKIP_DEBUG);
        return cw.toByteArray();
    }
}
