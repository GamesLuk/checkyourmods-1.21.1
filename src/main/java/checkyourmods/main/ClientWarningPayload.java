package checkyourmods.main;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import io.netty.buffer.ByteBuf;
import java.util.*;

public record ClientWarningPayload(String warningType, List<String> data) implements CustomPacketPayload {
    public static final Type<ClientWarningPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("checkyourmods", "client_warning"));

    public static final StreamCodec<ByteBuf, ClientWarningPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ClientWarningPayload::warningType,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), ClientWarningPayload::data,
            ClientWarningPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

