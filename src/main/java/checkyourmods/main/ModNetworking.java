package checkyourmods.main;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.*;

@EventBusSubscriber(modid = "checkyourmods")
public class ModNetworking {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(ModListPayload.TYPE, ModListPayload.STREAM_CODEC, ModNetworking::handleServer);
    }

    private static void handleServer(ModListPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            MinecraftServer server = player.getServer();
            if (server == null || Main.SERVER_MODS_CACHE == null) return;

            String expectedVersion = net.neoforged.fml.ModList.get().getModContainerById("checkyourmods")
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("unknown");

            if (!expectedVersion.equals(payload.cymVersion())) {
                player.connection.disconnect(Component.literal(
                        "§c[CheckYourMods] Version mismatch!\n§7Server expects version: §e" + expectedVersion + "\n§7You have: §e" + payload.cymVersion()
                ));
                Main.log("VERSION MISMATCH: " + player.getName().getString() + " tried to join with " + payload.cymVersion() + ", expected " + expectedVersion);
                return;
            }

            processMods(player, server, payload.mods());
            PackNetworking.handlePackVerification(player, payload.packs());
        });
    }

    private static void processMods(ServerPlayer player, MinecraftServer server, Map<String, ModListPayload.ModData> clientMods) {
        String playerUUID = player.getUUID().toString();
        String playerName = player.getName().getString();
        List<? extends String> requiredIds = Config.REQUIRED_MOD_IDS.get();
        List<? extends String> optionalIds = Config.OPTIONAL_MOD_IDS.get();

        Set<String> serverIds = new HashSet<>();
        Main.SERVER_MODS_CACHE.values().forEach(d -> serverIds.add(d.modId()));

        // Check for missing required mods
        List<String> missingRequired = new ArrayList<>();
        for (String required : requiredIds) {
            if (!serverIds.contains(required)) {
                boolean found = false;
                for (ModListPayload.ModData data : clientMods.values()) {
                    String cleanId = data.modId().split("#")[0].trim();
                    if (cleanId.equals(required)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    missingRequired.add(required);
                }
            }
        }

        if (!missingRequired.isEmpty()) {
            player.connection.disconnect(Component.literal(
                    "§c[CheckYourMods] You are missing required mods:\n§e" + String.join(", ", missingRequired)
            ));
            Main.log("MISSING REQUIRED MODS: " + playerName + " (UUID: " + playerUUID + ") | Missing: " + missingRequired);
            return;
        }

        // Check for extra/optional mods
        List<String> unallowedMods = new ArrayList<>();
        for (ModListPayload.ModData data : clientMods.values()) {
            String cleanId = data.modId().split("#")[0].trim();
            if (!serverIds.contains(cleanId) && !optionalIds.contains(cleanId)) {
                // This mod is not on the server and not marked as optional
                unallowedMods.add(cleanId);
            }
        }

        if (!unallowedMods.isEmpty()) {
            // Ban player and log details
            Main.log("BANNED PLAYER: " + playerName + " (UUID: " + playerUUID + ") -> " + unallowedMods);
        }
    }
}