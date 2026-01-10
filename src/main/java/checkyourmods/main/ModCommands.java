package checkyourmods.main;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "checkyourmods")
public class ModCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("modcheck")
                .then(Commands.literal("modlist")
                        .executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fAllowed Mod IDs: §a" + Config.ALLOWED_MOD_IDS.get()), false);
                            return 1;
                        }))
                .then(Commands.literal("packslist")
                        .executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fWatched Pack Hashes: §b" + Config.MANUAL_XRAY_HASHES.get()), false);
                            return 1;
                        }))
        );
    }
}
