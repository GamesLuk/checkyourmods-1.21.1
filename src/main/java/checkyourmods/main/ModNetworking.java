package checkyourmods.main;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
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
            if (server == null || ModCheckMain.SERVER_MODS_CACHE == null) return;

            String expectedVersion = net.neoforged.fml.ModList.get().getModContainerById("checkyourmods")
                    .map(c -> c.getModInfo().getVersion().toString())
                    .orElse("unknown");

            if (!expectedVersion.equals(payload.cymVersion())) {
                player.connection.disconnect(Component.literal(
                        "§c[CheckYourMods] Version mismatch!\n§7Server expects version: §e" + expectedVersion + "\n§7You have: §e" + payload.cymVersion()
                ));
                ModLogger.log("VERSION MISMATCH: " + player.getName().getString() + " tried to join with " + payload.cymVersion() + ", expected " + expectedVersion);
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
        List<? extends String> bannedIds = Config.BANNED_MOD_IDS.get();
        
        // Check if player is already banned
        if (PlayerBanManager.isPlayerBanned(playerUUID)) {
            PlayerBanManager.BanInfo banInfo = PlayerBanManager.getBanInfo(playerUUID);
            player.connection.disconnect(Component.literal(
                    "§c[CheckYourMods] You are banned for using forbidden mods!\n§7Reason: " + banInfo.reason
            ));
            
            if (!banInfo.notified && Config.ENABLE_BAN_NOTIFICATIONS.get()) {
                notifyOpsAboutBan(server, playerName, banInfo);
                PlayerBanManager.setNotified(playerUUID, true);
            }
            return;
        }

        Set<String> serverIds = new HashSet<>();
        ModCheckMain.SERVER_MODS_CACHE.values().forEach(d -> serverIds.add(d.modId()));

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
            ModLogger.log("MISSING REQUIRED MODS: " + playerName + " (UUID: " + playerUUID + ") | Missing: " + missingRequired);
            AuditLog.log(AuditLog.AuditAction.REQUIRED_MOD_MISSING, playerName, playerUUID, String.join(", ", missingRequired));
            return;
        }

        // Check for banned mods
        List<String> detectedBannedMods = new ArrayList<>();
        for (ModListPayload.ModData data : clientMods.values()) {
            String cleanId = data.modId().split("#")[0].trim();
            if (bannedIds.contains(cleanId)) {
                detectedBannedMods.add(cleanId);
                ModStatisticsManager.recordViolation(cleanId);
            }
        }

        if (!detectedBannedMods.isEmpty()) {
            List<String> banList = detectedBannedMods.size() > 3 ? 
                    detectedBannedMods.subList(0, 3) : detectedBannedMods;
            PlayerBanManager.banPlayer(playerName, playerUUID, detectedBannedMods, 
                    "Forbidden mod(s) detected: " + String.join(", ", banList));
            AuditLog.log(AuditLog.AuditAction.BANNED_MOD_DETECTED, playerName, playerUUID, 
                    String.join(", ", detectedBannedMods));
            player.connection.disconnect(Component.literal(
                    "§c[CheckYourMods] You are permanently banned for using forbidden mods!"
            ));
            return;
        }

        // Check for extra/optional mods
        List<String> extraMods = new ArrayList<>();
        for (ModListPayload.ModData data : clientMods.values()) {
            String cleanId = data.modId().split("#")[0].trim();
            if (!serverIds.contains(cleanId)) {
                if (!PlayerDataManager.isOptionallyAllowed(playerUUID, cleanId)) {
                    extraMods.add(cleanId);
                    ModStatisticsManager.recordModUsage(cleanId, playerName);
                }
            } else {
                ModStatisticsManager.recordModUsage(cleanId, playerName);
            }
        }

        if (!extraMods.isEmpty()) {
            // Send warning to player
            if (Config.ENABLE_MOD_WARNINGS.get()) {
                List<String> newWarnings = new ArrayList<>();
                for (String mod : extraMods) {
                    if (!PlayerDataManager.hasBeenWarned(playerUUID, mod)) {
                        newWarnings.add(mod);
                        PlayerDataManager.addWarning(playerUUID, playerName, mod);
                    }
                }
                
                if (!newWarnings.isEmpty()) {
                    sendWarningToPlayer(player, newWarnings);
                }
            }
            
            // Broadcast to admins/everyone
            String extraList = String.join("§f, ", extraMods);
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§6[CheckYourMods] §f" + playerName + " §7Mods: " + extraList), 
                    false
            );
            ModLogger.log("EXTRA MODS: " + playerName + " (UUID: " + playerUUID + ") -> " + extraMods);
        }
    }

    private static void notifyOpsAboutBan(MinecraftServer server, String playerName, PlayerBanManager.BanInfo banInfo) {
        try {
            int permLevel = Integer.parseInt(Config.BAN_NOTIFICATION_PERMISSION.get());
            for (ServerPlayer op : server.getPlayerList().getPlayers()) {
                if (op.hasPermissions(permLevel)) {
                    List<String> modsToShow = banInfo.bannedMods.size() > 3 ? 
                            banInfo.bannedMods.subList(0, 3) : banInfo.bannedMods;
                    String modsList = String.join(", ", modsToShow);
                    if (banInfo.bannedMods.size() > 3) {
                        modsList += " §6(+" + (banInfo.bannedMods.size() - 3) + " more)";
                    }
                    
                    Component message = Component.literal(
                            "§4[SECURITY] §c" + playerName + " §7was banned for forbidden mods§8: " + modsList
                    );
                    op.sendSystemMessage(message);
                }
            }
        } catch (Exception e) {
            ModLogger.log("ERROR: Failed to notify ops about ban: " + e.getMessage());
        }
    }

    private static void sendWarningToPlayer(ServerPlayer player, List<String> warnedMods) {
        String modsStr = String.join(", ", warnedMods);
        Component warningMessage = Component.literal("§6[CheckYourMods] §eYou are using client-only mods: §f" + modsStr);
        player.sendSystemMessage(warningMessage);
        
        // Optionally send clickable text for allowing mods
        for (String mod : warnedMods) {
            Component allowButton = Component.literal("§a[Allow '" + mod + "' as optional]")
                    .withStyle(style -> style.withClickEvent(
                            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/modcheck allow " + mod)
                    ));
            player.sendSystemMessage(allowButton);
        }
    }
}