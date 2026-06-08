package checkyourmods.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ModLogger {
    private static final File LOG_FILE = new File("logs", "checkyourmods-log.txt");
    private static final File BANS_LOG_FILE = new File("logs", "checkyourmods-bans.log");
    private static final File WARNINGS_LOG_FILE = new File("logs", "checkyourmods-warnings.log");
    private static final File VIOLATIONS_LOG_FILE = new File("logs", "checkyourmods-violations.log");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String message) {
        File logsDir = new File("logs");
        if (!logsDir.exists()) logsDir.mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            writer.println("[" + timestamp + "] " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logBan(String message) {
        File logsDir = new File("logs");
        if (!logsDir.exists()) logsDir.mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(BANS_LOG_FILE, true))) {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            writer.println("[" + timestamp + "] [BAN] " + message);
            // Also log to main log
            log("[BAN] " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logWarning(String message) {
        File logsDir = new File("logs");
        if (!logsDir.exists()) logsDir.mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(WARNINGS_LOG_FILE, true))) {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            writer.println("[" + timestamp + "] [WARNING] " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logViolation(String message) {
        File logsDir = new File("logs");
        if (!logsDir.exists()) logsDir.mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(VIOLATIONS_LOG_FILE, true))) {
            String timestamp = LocalDateTime.now().format(FORMATTER);
            writer.println("[" + timestamp + "] [VIOLATION] " + message);
            // Also log to main log
            log("[VIOLATION] " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}