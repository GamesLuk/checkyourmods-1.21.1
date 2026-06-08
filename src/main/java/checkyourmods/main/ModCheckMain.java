package checkyourmods.main;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import java.util.Map;

@Mod("checkyourmods")
public class ModCheckMain {
    public static Map<String, ModListPayload.ModData> SERVER_MODS_CACHE;

    public ModCheckMain(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        SERVER_MODS_CACHE = ModCheckUtil.generateCurrentModList();
        System.out.println("[CheckYourMods] Server starting. Registered mods: " + SERVER_MODS_CACHE.size());
        System.out.println("[CheckYourMods] Required mods: " + Config.REQUIRED_MOD_IDS.get().size());
        System.out.println("[CheckYourMods] Banned mods: " + Config.BANNED_MOD_IDS.get().size());
        
        // Initialize managers
        PlayerBanManager.getAllBans(); // Load bans
        ModLogger.log("SERVER START: CheckYourMods initialized with " + SERVER_MODS_CACHE.size() + " server mods");
    }
}
