package works.lysenko.base.parameters;

import org.junit.jupiter.api.Test;
import works.lysenko.util.prop.PropEnum;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigAuditTest {

    @Test
    void auditConfigDetail() throws IOException {
        Map<String, PropEnum> knownProperties = new TreeMap<>();
        for (PropEnum p : PropEnum.values()) {
            knownProperties.put(p.getPropertyName(), p);
        }

        List<File> partFiles = new ArrayList<>();
        File commonFile = new File("src/main/resources/config/common.configuration.properties");
        File partsDir = new File("src/main/resources/config/parts");
        if (partsDir.exists() && partsDir.isDirectory()) {
            File[] parts = partsDir.listFiles((dir, name) -> name.endsWith(".part"));
            if (parts != null) {
                Arrays.sort(parts, Comparator.comparing(File::getName));
                partFiles.addAll(Arrays.asList(parts));
            }
        }

        List<File> allFiles = new ArrayList<>();
        allFiles.add(commonFile);
        allFiles.addAll(partFiles);

        Pattern propLinePattern = Pattern.compile("^([#\\s]*)([.a-zA-Z0-9_-]+)=(.*)$");

        Set<String> propertiesFoundInParts = new TreeSet<>();
        List<String> errors = new ArrayList<>();

        for (File f : allFiles) {
            List<String> lines = Files.readAllLines(f.toPath());
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty() || line.startsWith("# ==") || line.startsWith("# --")) continue;
                if (line.startsWith(".include=")) continue;

                Matcher m = propLinePattern.matcher(line);
                if (m.find()) {
                    String prefix = m.group(1);
                    String key = m.group(2);
                    String val = m.group(3);

                    // All property examples should be commented out except active project root setting (.test.root)
                    if (!prefix.contains("#") && !".test.root".equals(key)) {
                        errors.add(f.getName() + ":" + (i+1) + " Property is NOT commented out: " + line);
                    }

                    if (!knownProperties.containsKey(key)) {
                        errors.add(f.getName() + ":" + (i+1) + " Unknown/outdated property key: " + key);
                    } else {
                        propertiesFoundInParts.add(key);
                        if (i > 0) {
                            String prevLine = lines.get(i - 1).trim();
                            if (prevLine.contains("Default:")) {
                                String commentDefault = prevLine.substring(prevLine.indexOf("Default:") + "Default:".length()).trim();
                                PropEnum pe = knownProperties.get(key);
                                String expectedDefault = pe.defaultValue();
                                if (expectedDefault == null) expectedDefault = "";
                                if (!commentDefault.equalsIgnoreCase(expectedDefault) && !(commentDefault.isEmpty() && expectedDefault.isEmpty())) {
                                    errors.add(f.getName() + ":" + (i+1) + " Comment default '" + commentDefault + "' does not match PropEnum default '" + expectedDefault + "' for key " + key);
                                }
                            }
                        }
                    }
                } else if (!line.startsWith("#")) {
                    errors.add(f.getName() + ":" + (i+1) + " Non-comment line that is not a property assignment: " + line);
                }
            }
        }

        for (String known : knownProperties.keySet()) {
            if (!propertiesFoundInParts.contains(known)) {
                errors.add("Missing example in .part files for property: " + known);
            }
        }

        assertTrue(errors.isEmpty(), "Config audit errors found:\n" + String.join("\n", errors));
    }

    @Test
    void auditPropertyHelp() throws IOException {
        Properties propHelp = new Properties();
        File propHelpFile = new File("src/main/resources/config/property-help.properties");
        try (FileInputStream fis = new FileInputStream(propHelpFile)) {
            propHelp.load(fis);
        }

        Set<String> knownKeys = new TreeSet<>();
        for (PropEnum p : PropEnum.values()) {
            knownKeys.add(p.getPropertyName());
        }

        Set<String> helpKeys = new TreeSet<>();
        for (String name : propHelp.stringPropertyNames()) {
            helpKeys.add(name);
        }

        assertEquals(knownKeys, helpKeys, "property-help.properties keys must match PropEnum 1-to-1");
    }
}
