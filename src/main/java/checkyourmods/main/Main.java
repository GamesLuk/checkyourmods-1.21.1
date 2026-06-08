package checkyourmods.main;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Mod("checkyourmods")
public class Main {
    public static Map<String, ModListPayload.ModData> SERVER_MODS_CACHE;

    public Main(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        SERVER_MODS_CACHE = ModCheckUtil.generateCurrentModList();
        System.out.println("[CheckYourMods] Server starting. Registered mods: " + SERVER_MODS_CACHE.size());
        System.out.println("[CheckYourMods] Required mods: " + Config.REQUIRED_MOD_IDS.get().size());
        System.out.println("[CheckYourMods] Optional mods: " + Config.OPTIONAL_MOD_IDS.get().size());
        
        // Initialize managers
        log("SERVER START: CheckYourMods initialized with " + SERVER_MODS_CACHE.size() + " server side, " + Config.REQUIRED_MOD_IDS.get().size() + " required and " + Config.OPTIONAL_MOD_IDS.get().size() + " optional mods.");
    }

    private static final File LOG_FILE = new File("logs", "checkyourmods-log.txt");
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

    public static void banPlayer(String playerName, String playerUUID, String reason) {

    }
}
