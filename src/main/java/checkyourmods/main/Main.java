package checkyourmods.main;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Mod("checkyourmods")
public class Main {
    public static Map<String, ModListPayload.ModData> SERVER_MODS_CACHE;
    public static Map<UUID, Map<String, ModListPayload.ModData>> OP_PLAYER_MODS = new java.util.concurrent.ConcurrentHashMap<>();

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
        Logging.log("SERVER START: CheckYourMods initialized with " + SERVER_MODS_CACHE.size() + " server side, " + Config.REQUIRED_MOD_IDS.get().size() + " required and " + Config.OPTIONAL_MOD_IDS.get().size() + " optional mods.");
    }

    public static void banPlayer(String playerName, UUID playerUUID, String banReason, Component kickReason) {
        Logging.log("BAN: " + playerName + " (" + playerUUID + ")");

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            Logging.log("BAN FAILED: server instance is null");
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);

        GameProfile profile = new GameProfile(playerUUID, playerName);

        UserBanList banList = server.getPlayerList().getBans();

        UserBanListEntry entry = new UserBanListEntry(
                profile,
                null,
                "CheckYourMods",
                null,
                banReason
        );

        banList.add(entry);
        try {
            banList.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (player != null) {
            player.connection.disconnect(kickReason);
        }
    }
}
