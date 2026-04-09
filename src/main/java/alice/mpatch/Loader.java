package alice.mpatch;

import alice.api.ClassByteProcessor;
import alice.injector.ClassPatcher;
import alice.mpatch.patcher.ClassPatchManagerPatcher;
import alice.mpatch.patcher.FMLClassPatcher;
import alice.mpatch.patcher.LaunchClassLoaderPatcher;
import alice.util.ClassUtil;
import org.objectweb.asm.*;

public class Loader implements Opcodes {
    public static void load() {
        System.out.println("MPatch loading...");
        ClassPatcher.addProtectedJar(ClassUtil.getJarPath(Loader.class));
        if (Environment.LAUNCHWRAPPER) {
            ClassPatcher.registerProcessor(new ClassByteProcessor() {
                @Override
                public byte[] process(byte[] classBytes, String name) {
                    if ("net/minecraft/launchwrapper/LaunchClassLoader.class".equals(name)) {
                        return LaunchClassLoaderPatcher.transform(classBytes);
                    }
                    return classBytes;
                }
            });
        }
        if (Environment.FORGE || Environment.FORGE_LEGACY) {
            ClassPatcher.registerProcessor(new ClassByteProcessor() {
                @Override
                public byte[] process(byte[] classBytes, String name) {
                    return FMLClassPatcher.transform(classBytes, name);
                }

                @Override
                public int priority() {
                    return -1;
                }
            });
            ClassPatcher.registerProcessor(new ClassByteProcessor() {
                @Override
                public byte[] process(byte[] classBytes, String name) {
                    if ("net/minecraftforge/fml/common/patcher/ClassPatchManager.class".equals(name) || "cpw/mods/fml/common/patcher/ClassPatchManager.class".equals(name)) {
                        return ClassPatchManagerPatcher.process(classBytes, name);
                    }
                    return classBytes;
                }
            });
        }
        System.out.println("MPatch loading completed.");
    }
}
