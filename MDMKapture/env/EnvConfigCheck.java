import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Pre-build check of one environment folder (env\DEV, env\QA, env\PROD), run by build.bat
 * before anything is compiled:
 *
 *   java EnvConfigCheck.java <master application.properties> <env folder>
 *
 * ERRORS (exit 1, the build stops):
 *   - a key of the master src\application.properties is missing from the environment's
 *     application.properties. The application reads its keys with no code default, so a
 *     missing key would only surface at run time, on the server.
 *   - any file of the environment folder still carries the __TBC__ marker - a value that
 *     has not been filled in from the environment config workbook yet.
 *
 * WARNINGS (printed, the build goes on):
 *   - a key the environment file has and the master does not (probably a stale key).
 *   - wsl.default.user is not blank (it must be blank behind WebSEAL).
 *
 * Single-file source program: no compile step, needs JDK 11+ (the build uses JDK 25).
 */
public class EnvConfigCheck {

    private static final String MARKER = "__TBC__";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java EnvConfigCheck.java <master application.properties> <env folder>");
            System.exit(2);
        }
        Path master = Paths.get(args[0]);
        Path envDir = Paths.get(args[1]);
        Path envProps = envDir.resolve("application.properties");
        int errors = 0;

        TreeSet<String> masterKeys = keys(master);
        TreeSet<String> envKeys = keys(envProps);

        TreeSet<String> missing = new TreeSet<>(masterKeys);
        missing.removeAll(envKeys);
        for (String k : missing) {
            System.out.println("   ERROR   key missing in " + envProps + " : " + k);
            errors++;
        }

        TreeSet<String> extra = new TreeSet<>(envKeys);
        extra.removeAll(masterKeys);
        for (String k : extra) {
            System.out.println("   WARNING key not in src\\application.properties : " + k);
        }

        List<Path> files;
        try (Stream<Path> s = Files.list(envDir)) {
            files = s.filter(Files::isRegularFile)
                     .filter(p -> !p.getFileName().toString().endsWith(".java"))
                     .sorted()
                     .collect(Collectors.toList());
        }
        for (Path f : files) {
            // ISO-8859-1 never rejects a byte; the marker is plain ASCII either way.
            List<String> lines = Files.readAllLines(f, StandardCharsets.ISO_8859_1);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String t = line.trim();
                if (t.startsWith("#") || t.startsWith("!")) {
                    continue;
                }
                if (line.contains(MARKER)) {
                    System.out.println("   ERROR   value not filled in (" + MARKER + ") " + f.getFileName()
                            + ":" + (i + 1) + " : " + t);
                    errors++;
                }
            }
        }

        Properties envValues = load(envProps);
        String wsl = envValues.getProperty("wsl.default.user");
        if (wsl != null && !wsl.trim().isEmpty()) {
            System.out.println("   WARNING wsl.default.user=" + wsl.trim()
                    + " - must be BLANK behind WebSEAL (only used when no iv-user header arrives)");
        }

        if (errors > 0) {
            System.out.println("   " + errors + " error(s) in " + envDir + " - fix them and build again.");
            System.exit(1);
        }
        System.out.println("   OK - " + envKeys.size() + " keys, all " + masterKeys.size()
                + " master keys present, no " + MARKER + " left.");
    }

    private static Properties load(Path p) throws Exception {
        if (!Files.isRegularFile(p)) {
            System.out.println("   ERROR   file not found: " + p);
            System.exit(1);
        }
        Properties props = new Properties();
        try (Reader r = new InputStreamReader(Files.newInputStream(p), StandardCharsets.UTF_8)) {
            props.load(r);
        }
        return props;
    }

    private static TreeSet<String> keys(Path p) throws Exception {
        return new TreeSet<>(load(p).stringPropertyNames());
    }
}
