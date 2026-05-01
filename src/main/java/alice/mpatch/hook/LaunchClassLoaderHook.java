package alice.mpatch.hook;

import alice.log.Logger;
import alice.mpatch.patcher.FMLClassPatcher;
import net.minecraft.launchwrapper.IClassTransformer;

@Deprecated
@SuppressWarnings("all")
public class LaunchClassLoaderHook {

    static {
        if(true) {
            throw new IllegalStateException();
        }
    }

    public static IClassTransformer registerTransformer(IClassTransformer transformer) {
        String name = transformer.getClass().getName();
        Logger.MAIN.info("intercept transformer: " + name);
        if (name.equals("net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer") || name.equals("cpw.mods.fml.common.asm.transformers.DeobfuscationTransformer")) {
            FMLClassPatcher.startDeobfuscation();
        }
        if (name.equals("net.minecraftforge.fml.common.asm.transformers.PatchingTransformer") || name.equals("cpw.mods.fml.common.asm.transformers.PatchingTransformer")) {
            FMLClassPatcher.startPatching();
            return (s, s1, bytes) -> bytes;
        }
        return transformer;
    }
}
