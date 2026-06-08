package checkyourmods.main;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import io.netty.buffer.ByteBuf;
import java.util.*;

public record ModListPayload(Map<String, ModData> mods, List<ResourcePackData> packs) implements CustomPacketPayload {
    public static final Type<ModListPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("checkyourmods", "verify"));

    public record ModData(String modId, String hash) {}

    public static final StreamCodec<ByteBuf, ModData> MOD_DATA_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ModData::modId, ByteBufCodecs.STRING_UTF8, ModData::hash, ModData::new
    );

    public static final StreamCodec<ByteBuf, ModListPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, MOD_DATA_CODEC), ModListPayload::mods,
            ByteBufCodecs.collection(ArrayList::new, ResourcePackData.STREAM_CODEC), ModListPayload::packs,
            ModListPayload::new
    );

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}