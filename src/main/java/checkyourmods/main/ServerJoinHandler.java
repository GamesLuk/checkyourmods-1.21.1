package checkyourmods.main;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.Collection;

@EventBusSubscriber(modid = "checkyourmods")
public class ServerJoinHandler {
    
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        String playerUUID = player.getUUID().toString();
        String playerName = player.getName().getString();
        
        // Check if player has warnings and notify
        if (Config.ENABLE_MOD_WARNINGS.get()) {
            notifyPlayerOfWarnings(player, playerUUID, playerName);
        }
        
        // Check if player was previously warned and provide options
        notifyPlayerOfForbiddenModUsage(player, playerUUID, playerName);
        
        // Log successful join
        ModLogger.log("PLAYER JOIN: " + playerName + " (UUID: " + playerUUID + ")");
    }
    
    private static void notifyPlayerOfWarnings(ServerPlayer player, String playerUUID, String playerName) {
        java.util.Set<String> warned = PlayerDataManager.getWarnedAboutMods(playerUUID);
        if (!warned.isEmpty()) {
            player.sendSystemMessage(Component.literal(
                    "§e[CheckYourMods] §7You have been warned about using client-only mods: §f" + String.join(", ", warned)
            ));
        }
    }
    
    private static void notifyPlayerOfForbiddenModUsage(ServerPlayer player, String playerUUID, String playerName) {
        java.util.Collection<? extends String> bannedMods = Config.BANNED_MOD_IDS.get();
        java.util.Set<String> warned = PlayerDataManager.getWarnedAboutMods(playerUUID);
        
        for (String warnedMod : warned) {
            if (bannedMods.contains(warnedMod)) {
                net.minecraft.network.chat.ClickEvent clickEvent = 
                        new net.minecraft.network.chat.ClickEvent(
                                net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, 
                                "/modcheck allow " + warnedMod
                        );
                Component button = Component.literal("§d[Allow '" + warnedMod + "' as optional]")
                        .withStyle(style -> style.withClickEvent(clickEvent));
                player.sendSystemMessage(button);
            }
        }
    }
}

