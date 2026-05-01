package alice.mpatch.game.deobfusction;

import alice.exception.BadEnvironment;
import alice.exception.ShouldNotReachHere;
import alice.log.Logger;
import alice.mpatch.Environment;
import alice.util.FileUtil;
import alice.util.IOUtil;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.JSONObject;
import org.objectweb.asm.Type;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class DeobfuscationManager {

    private static final boolean SAVE_RAW_MAPPINGS = "true".equals(System.getProperty("mpatch.debug.save_raw_mappings"));

    public static DeobfuscationType TYPE;

    //from obfuscated name to mid-name
    private static final BiMap<String, String> classMappings = HashBiMap.create();
    private static final Map<String, BiMap<String, String>> fieldMappings = new HashMap<>();
    private static final Map<String, BiMap<ImmutablePair<String, String>, ImmutablePair<String, String>>> methodMappings = new HashMap<>();

    public enum DeobfuscationType {
        OFFICIAL, YARN, MCP,
    }

    public static void init(String[] args) {
        Logger.MAIN.info("DeobfuscationManager loading...");
        String raw_mappings = null;
        if (Environment.VANILLA) {
            //TYPE = DeobfuscationType.NONE;
            Path VERSION_DIRECTORY = Environment.VERSION_DIRECTORY;
            Path version_json = VERSION_DIRECTORY.resolve(VERSION_DIRECTORY.getFileName().toString().concat(".json"));
            JSONObject json = new JSONObject(new String(FileUtil.read(version_json)));
            if (json.has("downloads")) {
                json = json.getJSONObject("downloads");
                String element_name = Environment.SIDE.isClient() ? "client_mappings" : "server_mappings";
                if (json.has(element_name)) {
                    json = json.getJSONObject(element_name);
                    TYPE = DeobfuscationType.OFFICIAL;
                    try {
                        URL url = new URL(json.getString("url"));
                        Logger.MAIN.info("Downloading official mappings from ".concat(url.toString()));
                        raw_mappings = new String(IOUtil.readURL(url));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if (raw_mappings == null) {
                try {
                    URL url = new URL("https://maven.neoforged.net/releases/de/oceanlabs/mcp/mcp_config/maven-metadata.xml");
                    Logger.MAIN.info("Downloading mcp maven metadata from ".concat(url.toString()).concat("."));
                    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                    String version = Environment.MC_VERSION;
                    final boolean[] exist = {false};
                    parser.parse(url.openStream(), new DefaultHandler() {

                        String current;

                        @Override
                        public void startElement(String uri, String localName, String qName, Attributes attributes) {
                            current = qName;
                            String id = attributes.getValue("id");
                            if (id != null) System.out.println("ID: " + id);
                        }

                        @Override
                        public void characters(char[] ch, int start, int length) {
                            if ("version".equals(current)) {
                                String data = new String(ch, start, length).trim();
                                if (!data.isEmpty()) {
                                    if (data.equals(version)) {
                                        exist[0] = true;
                                    }
                                }
                            }
                        }
                    });
                    if (exist[0]) {
                        TYPE = DeobfuscationType.MCP;
                        url = new URL("https://maven.neoforged.net/releases/de/oceanlabs/mcp/mcp_config/".concat(version).concat("/mcp_config-").concat(version).concat(".zip"));
                        Logger.MAIN.info("Downloading mcp from ".concat(url.toString()).concat("."));
                        try (ZipInputStream zis = new ZipInputStream(url.openStream())) {
                            ZipEntry entry = zis.getNextEntry();
                            while (entry != null) {
                                if (entry.getName().equals("config/joined.tsrg")) {
                                    raw_mappings = new String(IOUtil.getByteArray(zis));
                                    break;
                                }
                                entry = zis.getNextEntry();
                            }
                        }
                        if (raw_mappings != null) {
                            parseTsrg(raw_mappings);
                        } else {
                            throw new ShouldNotReachHere();
                        }
                    } else {
                        url = new URL("https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp/".concat(version).concat("/mcp-").concat(version).concat("-srg.zip"));
                        Logger.MAIN.info("Trying to download mcp from ".concat(url.toString()).concat("."));
                        try (ZipInputStream zis = new ZipInputStream(url.openStream())) {
                            ZipEntry entry = zis.getNextEntry();
                            while (entry != null) {
                                if (entry.getName().equals("joined.srg")) {
                                    TYPE = DeobfuscationType.MCP;
                                    raw_mappings = new String(IOUtil.getByteArray(zis));
                                    break;
                                }
                                entry = zis.getNextEntry();
                            }
                        } catch (FileNotFoundException e) {
                            throw new BadEnvironment("We can't find deobfucation mappings for your version:".concat(version));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if (raw_mappings == null) {
                            throw new BadEnvironment("We can't find deobfucation mappings for your version:".concat(version));
                        }
                        parseMappings(raw_mappings);
                    }
                } catch (IOException | ParserConfigurationException | SAXException e) {
                    throw new RuntimeException(e);
                }
            } else {
                parseMappings(raw_mappings);
            }
        } else if (Environment.FABRIC || Environment.QUILT) {
            TYPE = DeobfuscationType.YARN;
            URL url = ClassLoader.getSystemResource("mappings/mappings.tiny");
            if (url != null) {
                try {
                    raw_mappings = new String(IOUtil.readURL(url));
                    parseMappings(raw_mappings);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Cannot read fabric/quilt's yarn mappings!");
            }
        } else if (Environment.LAUNCHWRAPPER) {
            URL url = ClassLoader.getSystemResource("deobfuscation_data-".concat(Environment.MC_VERSION).concat(".lzma"));
            if (url != null) {
                TYPE = DeobfuscationType.MCP;
                try {
                    LZMACompressorInputStream is = new LZMACompressorInputStream(url.openStream());
                    raw_mappings = new String(IOUtil.getByteArray(is));
                    parseMappings(raw_mappings);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new ShouldNotReachHere();
            }
        } else if (Environment.MODLAUNCHER) {
            String mcp_version = null;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("--fml.mcpVersion")) {
                    i++;
                    mcp_version = args[i];
                    break;
                }
            }

            if (mcp_version != null) {
                TYPE = DeobfuscationType.MCP;
                Path path = FileUtil.WORKING_DIR;
                if (FileUtil.isDirectory(path.resolve("libraries"))) {
                    path = path.resolve("libraries");
                } else {
                    path = path.getParent().getParent().resolve("libraries");
                }
                String name = Environment.MC_VERSION.concat("-").concat(mcp_version);
                path = path.resolve("de").resolve("oceanlabs").resolve("mcp").resolve("mcp_config").resolve(name).resolve("mcp_config-".concat(name).concat(".zip"));
                if (!FileUtil.exists(path)) {
                    System.out.println(path.toString());
                    throw new IllegalStateException("Cannot find deobfuscation file!");
                }
                try (ZipFile file = new ZipFile(path.toFile())) {
                    ZipEntry entry = file.getEntry("config/joined.tsrg");
                    raw_mappings = new String(IOUtil.getByteArray(file.getInputStream(entry)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                parseMappings(raw_mappings);
            } else {
                throw new IllegalStateException();
            }
        } else if (Environment.CLEANROOM) {
            TYPE = DeobfuscationType.MCP;
            try {
                raw_mappings = new String(IOUtil.readURL(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("deobf_data-1.12.2.tsrg"))));
                parseMappings(raw_mappings);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        methodMappings.values().forEach(mapping -> mapping.replaceAll((k, v) -> {
            Type type = Type.getType(v.getRight());
            Type ret_type = type.getReturnType();
            Type[] arg_types = type.getArgumentTypes();
            String desc = "(";
            for (Type arg : arg_types) {
                desc = desc.concat(convert(arg));
            }
            desc = desc.concat(")").concat(convert(ret_type));
            return ImmutablePair.of(v.getLeft(), desc);
        }));
        Logger.MAIN.info("DeobfuscationManager loaded.");
    }

    private static void parseMappings(String raw_mappings) {
        if (SAVE_RAW_MAPPINGS) {
            FileUtil.write("raw_mappings", raw_mappings.getBytes(StandardCharsets.UTF_8));
        }
        switch (TYPE) {
            case MCP: {
                if (raw_mappings.startsWith("PK:") || raw_mappings.startsWith("CL:") || raw_mappings.startsWith("FD:") || raw_mappings.startsWith("MD:")) {
                    parseForgeMappings(raw_mappings);
                } else {
                    parseTsrg(raw_mappings);
                }
                break;
            }
            case OFFICIAL: {
                parseOfficialMappings(raw_mappings);
                break;
            }
            case YARN: {
                parseYarn(raw_mappings);
                break;
            }
            default: {
                throw new ShouldNotReachHere();
            }
        }
    }

    private static void parseYarn(String raw_mappings) {
        String[] mappings = raw_mappings.split("\n");
        for (String mapping : mappings) {
            String[] split = mapping.split("\t");
            String type = split[0];
            switch (type) {
                case "CLASS":
                    classMappings.put(split[1], split[2]);
                    break;
                case "FIELD": {
                    String className = split[1];
                    fieldMappings.computeIfAbsent(className, k -> HashBiMap.create()).put(split[3], split[4]);
                    break;
                }
                case "METHOD": {
                    String className = split[1];
                    String desc = split[2];
                    methodMappings.computeIfAbsent(className, k -> HashBiMap.create()).put(ImmutablePair.of(split[3], desc), ImmutablePair.of(split[4], desc));
                    break;
                }
            }
        }
    }

    private static void parseForgeMappings(String raw_mappings) {
        String[] mappings = raw_mappings.split("\n");
        for (String mapping : mappings) {
            String[] split = mapping.split(" ");
            String type = split[0];
            switch (type) {
                case "CL:": {
                    classMappings.put(split[1], split[2]);
                    break;
                }
                case "FD:": {
                    String ob_name = split[1];
                    String class_name = ob_name.substring(0, ob_name.lastIndexOf('/'));
                    ob_name = ob_name.substring(ob_name.lastIndexOf('/') + 1);
                    String mid_name = split[2];
                    mid_name = mid_name.substring(mid_name.lastIndexOf('/') + 1);
                    fieldMappings.computeIfAbsent(class_name, k -> HashBiMap.create()).put(ob_name, mid_name);
                    break;
                }

                case "MD:": {
                    String ob_name = split[1];
                    String class_name = ob_name.substring(0, ob_name.lastIndexOf('/'));
                    ob_name = ob_name.substring(ob_name.lastIndexOf('/') + 1);
                    String ob_desc = split[2];
                    String mid_name = split[3];
                    mid_name = mid_name.substring(mid_name.lastIndexOf('/') + 1);
                    String mid_desc = split[4];
                    methodMappings.computeIfAbsent(class_name, k -> HashBiMap.create()).put(ImmutablePair.of(ob_name, ob_desc), ImmutablePair.of(mid_name, mid_desc));
                    break;
                }
            }
        }
    }

    private static void parseOfficialMappings(String rawMappings) {
        //TODO
    }

    private static void parseTsrg(String raw_mappings) {
        String[] mappings = raw_mappings.split("\n");
        String current_class = null;
        for (int i = 1; i < mappings.length; i++) {
            String mapping = mappings[i];
            if (mapping.startsWith("\t\t")) {
                continue;
            }
            String[] split;
            if (!mapping.startsWith("\t")) {
                split = mapping.split(" ");
                current_class = split[0];
                classMappings.put(split[0], split[1]);
            } else {
                mapping = mapping.substring(1);
                split = mapping.split(" ");
                if (!mapping.contains("(")) {//field
                    fieldMappings.computeIfAbsent(current_class, k -> HashBiMap.create()).put(split[0], split[1]);
                } else {//method
                    String desc = split[1];
                    methodMappings.computeIfAbsent(current_class, k -> HashBiMap.create()).put(ImmutablePair.of(split[0], desc), ImmutablePair.of(split[2], desc));
                }
            }
        }
    }

    private static String convert(Type type) {
        boolean array = type.getSort() == Type.ARRAY;
        if(type.getSort() == Type.OBJECT || (array && type.getElementType().getSort() == Type.OBJECT)) {
            String s = array ? type.getElementType().getInternalName() : type.getInternalName();
            String _try = classMappings.get(s);
            if (_try != null) {
                return (array ? "[L" : "L").concat(_try).concat(";");
            }
        }
        return type.toString();
    }

    public static String mapClass(String name) {
        return classMappings.getOrDefault(name, name);
    }

    public static String unmapClass(String name) {
        return classMappings.inverse().getOrDefault(name, name);
    }

    public static String mapField(String className, String fieldName) {
        BiMap<String, String> mappings = fieldMappings.get(unmapClass(className));
        if (mappings != null) {
            return mappings.get(fieldName);
        }
        return fieldName;
    }

    public static String unmapField(String className, String fieldName) {
        BiMap<String, String> mappings = fieldMappings.get(unmapClass(className));
        if (mappings != null) {
            return mappings.inverse().get(fieldName);
        }
        return fieldName;
    }

    public static ImmutablePair<String, String> mapMethod(String className, ImmutablePair<String, String> method) {
        BiMap<ImmutablePair<String, String>, ImmutablePair<String, String>> mappings = methodMappings.get(unmapClass(className));
        if (mappings != null) {
            return mappings.getOrDefault(method, method);
        }
        return method;
    }

    public static void test() {

    }
}
