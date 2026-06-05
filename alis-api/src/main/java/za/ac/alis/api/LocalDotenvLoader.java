package za.ac.alis.api;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.cdimascio.dotenv.Dotenv;

public final class LocalDotenvLoader {

    private static final String ENV_FILE_NAME = ".env";

    private LocalDotenvLoader() {
    }

    public static boolean load() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();

        while (directory != null) {
            Path envFile = directory.resolve(ENV_FILE_NAME);
            if (Files.isRegularFile(envFile)) {
                loadFrom(directory);
                return true;
            }

            if (Files.exists(directory.resolve(".git"))) {
                return false;
            }

            directory = directory.getParent();
        }

        return false;
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
}
