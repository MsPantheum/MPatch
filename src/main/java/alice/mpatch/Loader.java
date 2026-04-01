package alice.mpatch;

import alice.api.ClassByteProcessor;
import alice.injector.patch.ClassPatcher;
import alice.mpatch.patcher.LaunchClassLoaderPatcher;

public class Loader {
    public static void load() {
        System.out.println("MPatch loading...");
        ClassPatcher.registerProcessor(new ClassByteProcessor() {
            @Override
            public byte[] process(byte[] classBytes, String name) {
                if("net/minecraft/launchwrapper/LaunchClassLoader.class".equals(name)){
                    return LaunchClassLoaderPatcher.transform(classBytes);
                }
                return classBytes;
            }
        });
    }
}
