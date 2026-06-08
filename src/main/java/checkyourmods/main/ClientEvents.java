package checkyourmods.main;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "checkyourmods", value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onJoin(ClientPlayerNetworkEvent.LoggingIn event) {
        String version = net.neoforged.fml.ModList.get().getModContainerById("checkyourmods")
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");

        PacketDistributor.sendToServer(new ModListPayload(
                ModCheckUtil.generateCurrentModList(),
                PackCheckUtil.generateCurrentPackList(),
                version
        ));
    }
}