package checkyourmods.main;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Logging {
    private static final File LOG_FILE = new File("logs", "checkyourmods-log.txt");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

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

    public static final Component PREFIX =
            Component.literal("[")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal("CheckYourMods")
                            .withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("] ")
                            .withStyle(ChatFormatting.DARK_GRAY));

    public static JoinFailureInfo findLatestFailure(
            String playerName,
            int hoursBack
    ) {
        if (!LOG_FILE.exists()) {
            return null;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusHours(hoursBack);

        JoinFailureInfo latest = null;

        try {
            List<String> lines = Files.readAllLines(LOG_FILE.toPath());

            Pattern pattern = Pattern.compile(
                    "\\[(.*?)] JOIN_CHECK_FAILED \\| Player=(.*?) \\| UUID=(.*?) \\| Missing=(.*?) \\| Unallowed=(.*)"
            );

            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);

                if (!matcher.matches()) {
                    continue;
                }

                String loggedPlayer = matcher.group(2);

                if (!loggedPlayer.equalsIgnoreCase(playerName)) {
                    continue;
                }

                LocalDateTime timestamp =
                        LocalDateTime.parse(matcher.group(1), FORMATTER);

                if (timestamp.isBefore(cutoff)) {
                    continue;
                }

                if (latest == null || timestamp.isAfter(latest.timestamp)) {
                    latest = new JoinFailureInfo();

                    latest.timestamp = timestamp;

                    latest.missingMods =
                            List.of(matcher.group(4)
                                    .replace("[", "")
                                    .replace("]", "")
                                    .split(",\\s*"));

                    latest.unallowedMods =
                            List.of(matcher.group(5)
                                    .replace("[", "")
                                    .replace("]", "")
                                    .split(",\\s*"));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return latest;
    }
}

