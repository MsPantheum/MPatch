package alice.mpatch;

import alice.api.ClassByteProcessor;
import alice.injector.ClassPatcher;
import alice.log.Logger;
import alice.mpatch.game.deobfusction.DeobfuscationManager;
import alice.mpatch.patcher.ClassPatchManagerPatcher;
import alice.mpatch.patcher.FMLClassPatcher;
import alice.mpatch.patcher.LaunchClassLoaderPatcher;
import alice.mpatch.patcher.QuiltBasePathPatcher;
import alice.util.FileUtil;
import org.objectweb.asm.*;

@SuppressWarnings("unused")
public class Loader implements Opcodes {
    public static void load(String[] args) {
        Logger.MAIN.info("MPatch loading...");
        ClassPatcher.addProtectedJar(FileUtil.getJarPath(Loader.class));
        if (Environment.LAUNCHWRAPPER) {
            ClassPatcher.registerProcessor(new ClassByteProcessor() {

                boolean eol = false;

                @Override
                public byte[] processChecked(byte[] classBytes, String name) {
                    if ((Environment.CLEANROOM ? "top/outlands/foundation/TransformerDelegate.class" : "net/minecraft/launchwrapper/LaunchClassLoader.class").equals(name)) {
                        eol = true;
                        return LaunchClassLoaderPatcher.transform(classBytes,name);
                    }
                    return classBytes;
                }

                @Override
                public boolean endOfLife() {
                    return eol;
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

                boolean eol = false;

                @Override
                public byte[] processChecked(byte[] classBytes, String name) {
                    if ("net/minecraftforge/fml/common/patcher/ClassPatchManager.class".equals(name) || "cpw/mods/fml/common/patcher/ClassPatchManager.class".equals(name)) {
                        eol = true;
                        return ClassPatchManagerPatcher.process(classBytes, name);
                    }
                    return classBytes;
                }

                @Override
                public boolean endOfLife() {
                    return eol;
                }
            });
            ClassPatcher.registerProvider(name -> FMLClassPatcher.transform(null, name));
        }
        if (Environment.QUILT) {
            ClassPatcher.registerProcessor(new ClassByteProcessor() {

                boolean eol = false;

                @Override
                public byte[] processChecked(byte[] classBytes, String name) {
                    if ("org/quiltmc/loader/impl/filesystem/QuiltBasePath.class".equals(name)) {
                        eol = true;
                        return QuiltBasePathPatcher.transform(classBytes);
                    }
                    return classBytes;
                }

                @Override
                public boolean endOfLife() {
                    return eol;
                }
            });
        }
        DeobfuscationManager.init(args);
        Logger.MAIN.info("MPatch loading completed.");
    }
}
