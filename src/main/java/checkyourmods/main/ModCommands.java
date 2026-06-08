package checkyourmods.main;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;

@EventBusSubscriber(modid = "checkyourmods")
public class ModCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("modcheck")
                .then(Commands.literal("requiredlist")
                        .executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fRequired Mod IDs: §b" + Config.REQUIRED_MOD_IDS.get()), false);
                            return 1;
                        }))
                .then(Commands.literal("bannedlist")
                        .executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fBanned Mod IDs: §c" + Config.BANNED_MOD_IDS.get()), false);
                            return 1;
                        }))
                .then(Commands.literal("packslist")
                        .executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fWatched Pack Hashes: §b" + Config.MANUAL_XRAY_HASHES.get()), false);
                            return 1;
                        }))
                .then(Commands.literal("togglepacks")
                        .requires(c -> c.hasPermission(2))
                        .executes(c -> {
                            boolean newState = !PackManager.isStrictModeEnabled();
                            PackManager.setStrictModeEnabled(newState);
                            c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fStrict Pack Verify is now: " + (newState ? "§aON" : "§cOFF")), false);
                            return 1;
                        }))
                .then(Commands.literal("allowpack")
                        .requires(c -> c.hasPermission(2))
                        .then(Commands.argument("hash", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .executes(c -> {
                                    String hash = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "hash");
                                    PackManager.allowPack(hash);
                                    c.getSource().sendSuccess(() -> Component.literal("§a[CheckYourMods] §fPack hash '§e" + hash + "§f' has been allowed globally"), false);
                                    return 1;
                                }))
                        .requires(c -> c.hasPermission(2))
                        .executes(ModCommands::showDashboard))
                .then(Commands.literal("bans")
                        .requires(c -> c.hasPermission(2))
                        .executes(c -> {
                            Map<String, PlayerBanManager.BanInfo> bans = PlayerBanManager.getAllBans();
                            if (bans.isEmpty()) {
                                c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fNo banned players"), false);
                            } else {
                                c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fBanned Players: §c" + bans.size()), false);
                                for (PlayerBanManager.BanInfo ban : bans.values()) {
                                    c.getSource().sendSuccess(() -> Component.literal("  §7- " + ban.playerName + " §8(" + ban.playerUUID + "): " + String.join(", ", ban.bannedMods)), false);
                                }
                            }
                            return 1;
                        }))
                .then(Commands.literal("unban")
                        .requires(c -> c.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> {
                                    ServerPlayer player = EntityArgument.getPlayer(c, "player");
                                    String uuid = player.getUUID().toString();
                                    if (PlayerBanManager.isPlayerBanned(uuid)) {
                                        PlayerBanManager.unbanPlayer(uuid);
                                        AuditLog.log(AuditLog.AuditAction.PLAYER_UNBAN, player.getName().getString(), uuid, "Unbanned by admin");
                                        c.getSource().sendSuccess(() -> Component.literal("§a[CheckYourMods] §f" + player.getName().getString() + " has been unbanned"), false);
                                    } else {
                                        c.getSource().sendSuccess(() -> Component.literal("§c[CheckYourMods] §f" + player.getName().getString() + " is not banned"), false);
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("stats")
                        .requires(c -> c.hasPermission(2))
                        .executes(c -> {
                            Map<String, ModStatisticsManager.ModStats> stats = ModStatisticsManager.getAllStatistics();
                            if (stats.isEmpty()) {
                                c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fNo statistics available"), false);
                            } else {
                                List<Map.Entry<String, ModStatisticsManager.ModStats>> topMods = ModStatisticsManager.getTopMods(10);
                                c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fTop 10 Most Used Mods:"), false);
                                for (Map.Entry<String, ModStatisticsManager.ModStats> entry : topMods) {
                                    ModStatisticsManager.ModStats stat = entry.getValue();
                                    c.getSource().sendSuccess(() -> Component.literal("  §7- " + entry.getKey() + ": §f" + stat.usageCount + " uses§8 | §cViolations: " + stat.violations), false);
                                }
                            }
                            return 1;
                        }))
                .then(Commands.literal("audit")
                        .requires(c -> c.hasPermission(3))
                        .executes(c -> {
                            List<String> auditLog = AuditLog.getAuditLog(20);
                            if (auditLog.isEmpty()) {
                                c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fNo audit entries"), false);
                            } else {
                                c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fLast 20 Audit Entries:"), false);
                                for (String entry : auditLog) {
                                    c.getSource().sendSuccess(() -> Component.literal("§8" + entry), false);
                                }
                            }
                            return 1;
                        }))
                .then(Commands.literal("allow")
                        .then(Commands.argument("modid", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(c -> {
                                    if (!(c.getSource().getEntity() instanceof ServerPlayer player)) {
                                        c.getSource().sendFailure(Component.literal("Only players can use this command"));
                                        return 0;
                                    }
                                    String modId = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "modid");
                                    String playerUUID = player.getUUID().toString();
                                    String playerName = player.getName().getString();
                                    
                                    PlayerDataManager.allowModOptionally(playerUUID, playerName, modId);
                                    AuditLog.log(AuditLog.AuditAction.MOD_ALLOWED, playerName, playerUUID, "Mod: " + modId);
                                    player.sendSystemMessage(Component.literal("§a[CheckYourMods] §fMod '§e" + modId + "§f' has been allowed as optional"));
                                    ModLogger.log("MOD ALLOWED: " + playerName + " allowed '" + modId + "' as optional");
                                    return 1;
                                })))
        );
    }

    private static int showDashboard(CommandContext<CommandSourceStack> c) {
        c.getSource().sendSuccess(() -> Component.literal("§6════════════════════════════════════════"), false);
        c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §bDASHBOARD"), false);
        c.getSource().sendSuccess(() -> Component.literal("§6════════════════════════════════════════"), false);
        
        Map<String, PlayerBanManager.BanInfo> bans = PlayerBanManager.getAllBans();
        Map<String, PlayerDataManager.PlayerData> playerData = PlayerDataManager.getAllPlayerData();
        Map<String, ModStatisticsManager.ModStats> stats = ModStatisticsManager.getAllStatistics();
        
        c.getSource().sendSuccess(() -> Component.literal("§7Banned Players: §c" + bans.size()), false);
        c.getSource().sendSuccess(() -> Component.literal("§7Total Players Tracked: §f" + playerData.size()), false);
        c.getSource().sendSuccess(() -> Component.literal("§7Total Mods Detected: §f" + stats.size()), false);
        c.getSource().sendSuccess(() -> Component.literal("§7Configuration:"), false);
        c.getSource().sendSuccess(() -> Component.literal("  §8- Required Mods: §f" + Config.REQUIRED_MOD_IDS.get().size()), false);
        c.getSource().sendSuccess(() -> Component.literal("  §8- Banned Mods: §f" + Config.BANNED_MOD_IDS.get().size()), false);
        
        List<Map.Entry<String, ModStatisticsManager.ModStats>> topMods = ModStatisticsManager.getTopMods(3);
        if (!topMods.isEmpty()) {
            c.getSource().sendSuccess(() -> Component.literal("§7Top 3 Mods:"), false);
            for (Map.Entry<String, ModStatisticsManager.ModStats> entry : topMods) {
                c.getSource().sendSuccess(() -> Component.literal("  §8- " + entry.getKey() + ": §f" + entry.getValue().usageCount + " uses"), false);
            }
        }
        
        c.getSource().sendSuccess(() -> Component.literal("§6════════════════════════════════════════"), false);
        return 1;
    }
}
