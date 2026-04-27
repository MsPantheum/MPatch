package alice.mpatch.patcher;

import alice.Platform;
import alice.mpatch.Environment;
import alice.util.BytecodeUtil;
import org.objectweb.asm.*;

public class ClassPatchManagerPatcher implements Opcodes {
    public static byte[] process(byte[] classBytes, String name) {
        String cls = name.substring(0, name.length() - 6);
        return BytecodeUtil.patchClass(classBytes, cw -> new ClassVisitor(Platform.ASM_LEVEL, cw) {

            boolean flag = Environment.FORGE;

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if ("getPatchedResource".equals(name)) {
                    mv = new MethodVisitor(Platform.ASM_LEVEL, mv) {
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
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitInsn(ARETURN);
                    mv.visitMaxs(1, 4);
                    MethodVisitor neo_mv = super.visitMethod(access, "trueApplyPatch", descriptor, signature, exceptions);
                    return flag ? new MethodVisitor(Platform.ASM_LEVEL, neo_mv) {

                        boolean delete = false;

                        @Override
                        public void visitInsn(int opcode) {
                            if (delete) {
                                if (opcode == ATHROW) {
                                    delete = false;
                                }
                                return;
                            }
                            super.visitInsn(opcode);
                        }

                        @Override
                        public void visitIntInsn(int opcode, int operand) {
                            if (delete) {
                                return;
                            }
                            super.visitIntInsn(opcode, operand);
                        }

                        @Override
                        public void visitVarInsn(int opcode, int varIndex) {
                            if (delete) {
                                return;
                            }
                            super.visitVarInsn(opcode, varIndex);
                        }

                        @Override
                        public void visitTypeInsn(int opcode, String type) {
                            if (delete) {
                                return;
                            }
                            super.visitTypeInsn(opcode, type);
                        }

                        @Override
                        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                            if (delete) {
                                return;
                            }
                            super.visitFieldInsn(opcode, owner, name, descriptor);
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            if (delete) {
                                return;
                            }
                            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                        }

                        @Override
                        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
                            if (delete) {
                                return;
                            }
                            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
                        }

                        @Override
                        public void visitJumpInsn(int opcode, Label label) {
                            if (delete) {
                                return;
                            }
                            super.visitJumpInsn(opcode, label);
                        }

                        @Override
                        public void visitLdcInsn(Object value) {
                            if (value.equals("Patcher expecting non-empty class data file for {}, but received empty.")) {
                                delete = true;
                                super.visitInsn(POP);
                                return;
                            }
                            if (delete) {
                                return;
                            }
                            super.visitLdcInsn(value);
                        }

                        @Override
                        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
                            if (delete) {
                                return;
                            }
                            super.visitTableSwitchInsn(min, max, dflt, labels);
                        }

                        @Override
                        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                            if (delete) {
                                return;
                            }
                            super.visitLookupSwitchInsn(dflt, keys, labels);
                        }

                        @Override
                        public void visitIincInsn(int varIndex, int increment) {
                            if (delete) {
                                return;
                            }
                            super.visitIincInsn(varIndex, increment);
                        }

                        @Override
                        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
                            if (delete) {
                                return;
                            }
                            super.visitMultiANewArrayInsn(descriptor, numDimensions);
                        }

                        boolean generated = false;

                        @Override
                        public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
                            super.visitFrame(type, numLocal, local, numStack, stack);
                            if (delete && !generated) {
                                generated = true;
                                super.visitInsn(ACONST_NULL);
                                super.visitInsn(ARETURN);
                            }
                        }
                    } : neo_mv;
                }
                return mv;
            }
        });
    }
}
