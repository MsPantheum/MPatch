package cpw.mods.fml.common.patcher;

public class ClassPatchManager {
    public static final ClassPatchManager INSTANCE = new ClassPatchManager();

    public byte[] trueApplyPatch(String name, String mappedName, byte[] inputData) {
        return inputData;
    }
}
