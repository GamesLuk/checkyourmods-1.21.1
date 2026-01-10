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
            if (server == null || ModCheckMain.SERVER_MODS_CACHE == null) return;

            processMods(player, server, payload.mods());
            PackNetworking.handlePackVerification(player, payload.packs());
        });
    }

    private static void processMods(ServerPlayer player, MinecraftServer server, Map<String, ModListPayload.ModData> clientMods) {
        List<? extends String> allowedIds = Config.ALLOWED_MOD_IDS.get();
        Set<String> serverIds = new HashSet<>();
        ModCheckMain.SERVER_MODS_CACHE.values().forEach(d -> serverIds.add(d.modId()));
        List<String> extra = new ArrayList<>();

        clientMods.forEach((file, data) -> {
            if (!serverIds.contains(data.modId()) && !allowedIds.contains(data.modId())) {
                extra.add("§e" + file + " §7(ID: " + data.modId() + ")");
            }
        });

        if (!extra.isEmpty()) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("§6[CheckYourMods] §f" + player.getName().getString() + " tiene mods extra: " + String.join("§f, ", extra)), false);
            ModLogger.log("MODS EXTRA: " + player.getName().getString() + " -> " + extra);
        }
    }
}