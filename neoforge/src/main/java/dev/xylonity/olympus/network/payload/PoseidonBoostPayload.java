package dev.xylonity.olympus.network.payload;

import dev.xylonity.olympus.Olympus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record PoseidonBoostPayload() implements CustomPacketPayload {

    public static final PoseidonBoostPayload INSTANCE = new PoseidonBoostPayload();
    public static final Type<PoseidonBoostPayload> TYPE = new Type<>(Olympus.of("poseidon_boost"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PoseidonBoostPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public @NonNull Type<PoseidonBoostPayload> type() {
        return TYPE;
    }

}
