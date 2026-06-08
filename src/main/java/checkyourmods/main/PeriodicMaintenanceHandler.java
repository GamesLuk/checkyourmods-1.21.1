package checkyourmods.main;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@EventBusSubscriber(modid = "checkyourmods")
public class PeriodicMaintenanceHandler {
    private static long lastCleanup = 0;
    private static final long CLEANUP_INTERVAL_TICKS = 6000 * 60; // Every minute on server

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!event.hasTime()) return;
        
        lastCleanup++;
        if (lastCleanup >= CLEANUP_INTERVAL_TICKS) {
            lastCleanup = 0;
            performMaintenance();
        }
    }

    private static void performMaintenance() {
        // Clean up old log files (older than 30 days)
        cleanOldLogs(30);
        
        // Save current statistics
        saveMaintenanceCheckpoint();
    }

    private static void cleanOldLogs(int days) {
        File logsDir = new File("logs");
        if (!logsDir.exists()) return;

        File[] files = logsDir.listFiles();
        if (files == null) return;

        long cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L);

        for (File file : files) {
            try {
                if (file.isFile() && file.getName().startsWith("checkyourmods")) {
                    BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                    if (attrs.lastModifiedTime().toMillis() < cutoffTime) {
                        file.delete();
                        ModLogger.log("MAINTENANCE: Deleted old log file: " + file.getName());
                    }
                }
            } catch (IOException e) {
                ModLogger.log("ERROR: Failed to check log file: " + e.getMessage());
            }
        }
    }

    private static void saveMaintenanceCheckpoint() {
        try {
            File maintenanceFile = new File("data", "maintenance_checkpoint.txt");
            File dataDir = new File("data");
            if (!dataDir.exists()) dataDir.mkdirs();

            java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(maintenanceFile));
            writer.println("Last Maintenance: " + LocalDateTime.now());
            writer.println("Total Banned Players: " + PlayerBanManager.getAllBans().size());
            writer.println("Total Player Data Entries: " + PlayerDataManager.getAllPlayerData().size());
            writer.println("Total Mod Statistics: " + ModStatisticsManager.getAllStatistics().size());
            writer.close();

            ModLogger.log("MAINTENANCE: Checkpoint saved");
        } catch (IOException e) {
            ModLogger.log("ERROR: Failed to save maintenance checkpoint: " + e.getMessage());
        }
    }
}

