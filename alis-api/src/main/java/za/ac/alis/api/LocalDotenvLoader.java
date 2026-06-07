package za.ac.alis.api;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.github.cdimascio.dotenv.Dotenv;

public final class LocalDotenvLoader {

    private static final String ENV_FILE_NAME = ".env";
    private static final List<RequiredSetting> REQUIRED_SETTINGS = List.of(
            new RequiredSetting("DB_URL", "DB_URL", "spring.datasource.url"),
            new RequiredSetting("DB_USERNAME", "DB_USERNAME", "spring.datasource.username"),
            new RequiredSetting("DB_PASSWORD", "DB_PASSWORD", "spring.datasource.password"),
            new RequiredSetting("ALIS_JWT_SECRET", "ALIS_JWT_SECRET", "alis.jwt.secret")
    );

    private LocalDotenvLoader() {
    }

    public static boolean load() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            Path envFile = directory.resolve("config").resolve(ENV_FILE_NAME);
            if (Files.isRegularFile(envFile)) {
                loadFrom(envFile.getParent());
                return true;
            }

            if (Files.exists(directory.resolve(".git"))) {
                return false;
            }

            directory = directory.getParent();
        }

        return false;
    }

    public static void validateRequiredConfiguration(String[] args) {
        if (isTestProfile(args)) {
            return;
        }

        List<String> missing = REQUIRED_SETTINGS.stream()
                .filter(setting -> isBlank(valueFor(args, setting.keys())))
                .map(RequiredSetting::displayName)
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required configuration: " + String.join(", ", missing)
                            + ". Copy config/.env.example to config/.env, or set the same values as "
                            + "environment variables, then fill in the database values and ALIS_JWT_SECRET."
            );
        }

        String dbUrl = valueFor(args, "DB_URL", "spring.datasource.url");
        if (!dbUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException(
                    "DB_URL must be a PostgreSQL JDBC URL starting with jdbc:postgresql://. "
                            + "If Supabase gave you postgresql://..., prefix it with jdbc:."
            );
        }

        String jwtSecret = valueFor(args, "ALIS_JWT_SECRET", "alis.jwt.secret");
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ALIS_JWT_SECRET must be at least 32 bytes for JWT signing.");
        }
    }

    private static void loadFrom(Path directory) {
        Dotenv dotenv = Dotenv.configure()
                .directory(directory.toString())
                .ignoreIfMissing()
                .load();

        dotenv.entries().forEach(entry -> {
            if (System.getenv(entry.getKey()) == null && System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
    }

    private static boolean isTestProfile(String[] args) {
        String profiles = valueFor(args, "SPRING_PROFILES_ACTIVE", "spring.profiles.active");
        if (isBlank(profiles)) {
            profiles = "dev";
        }

        return Arrays.stream(profiles.split(","))
                .map(String::trim)
                .anyMatch("test"::equalsIgnoreCase);
    }

    private static String valueFor(String[] args, String... keys) {
        for (String key : keys) {
            String commandLineValue = commandLineValue(args, key);
            if (!isBlank(commandLineValue)) {
                return commandLineValue;
            }

            String systemValue = System.getProperty(key);
            if (!isBlank(systemValue)) {
                return systemValue;
            }

            String envValue = System.getenv(key);
            if (!isBlank(envValue)) {
                return envValue;
            }

            envValue = System.getenv(toEnvironmentName(key));
            if (!isBlank(envValue)) {
                return envValue;
            }
        }

        return null;
    }

    private static String commandLineValue(String[] args, String key) {
        if (args == null) {
            return null;
        }

        String prefix = "--" + key + "=";
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }

        return null;
    }

    private static String toEnvironmentName(String key) {
        return key.toUpperCase(Locale.ROOT)
                .replace('.', '_')
                .replace('-', '_');
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank() || value.startsWith("${");
    }

    private record RequiredSetting(String displayName, String... keys) {
    }
}
