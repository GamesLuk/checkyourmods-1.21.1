package checkyourmods.main;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import io.netty.buffer.ByteBuf;

public record ResourcePackData(String fileName, String description, String hash) {
    public static final StreamCodec<ByteBuf, ResourcePackData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ResourcePackData::fileName,
            ByteBufCodecs.STRING_UTF8, ResourcePackData::description,
            ByteBufCodecs.STRING_UTF8, ResourcePackData::hash,
            ResourcePackData::new
    );
}