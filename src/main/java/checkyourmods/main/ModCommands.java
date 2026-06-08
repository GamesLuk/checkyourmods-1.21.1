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
                .requires(c -> c.hasPermission(4))
                .then(Commands.literal("requiredlist")
                        .executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal("§6[CheckYourMods] §fRequired Mod IDs: §b" + Config.REQUIRED_MOD_IDS.get()), false);
                            return 1;
                        }))
                .then(Commands.literal("require")
                        .then(Commands.argument("modid", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(c -> {
                                    CommandSourceStack context = c.getSource();

                                    String modId = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "modid");
                                    Config.addRequiredMod(modId);

                                    context.sendSystemMessage(Component.literal("§a[CheckYourMods] §fMod '§e" + modId + "§f' is now required on the server"));
                                    Main.log("MOD REQUIRED: " + modId + " set as required");
                                    return 1;
                                })))
                .then(Commands.literal("allow")
                        .then(Commands.argument("modid", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(c -> {
                                    CommandSourceStack context = c.getSource();

                                    String modId = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "modid");
                                    Config.addOptionalMod(modId);

                                    context.sendSystemMessage(Component.literal("§a[CheckYourMods] §fMod '§e" + modId + "§f' is now optional on the server"));
                                    Main.log("MOD ALLOWED: " + modId + " set as optional");
                                    return 1;
                                })))
                .then(Commands.literal("forbid")
                        .then(Commands.argument("modid", com.mojang.brigadier.arguments.StringArgumentType.word())
                                .executes(c -> {
                                    CommandSourceStack context = c.getSource();

                                    String modId = com.mojang.brigadier.arguments.StringArgumentType.getString(c, "modid");
                                    Config.removeMod(modId);

                                    context.sendSystemMessage(Component.literal("§a[CheckYourMods] §fMod '§e" + modId + "§f' is now forbidden on the server (works not for server side mods)"));
                                    Main.log("MOD REMOVED: " + modId + " removed from allowed lists");
                                    return 1;
                                })))
        );
    }
}
