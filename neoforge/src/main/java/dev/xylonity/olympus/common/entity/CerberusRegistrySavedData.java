package dev.xylonity.olympus.common.entity;

import com.mojang.serialization.Codec;
import dev.xylonity.olympus.Olympus;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class CerberusRegistrySavedData extends SavedData {
    private static final Codec<Map<String, String>> ENTRIES_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.STRING);
    private static final Codec<CerberusRegistrySavedData> CODEC = ENTRIES_CODEC.xmap(
            CerberusRegistrySavedData::fromSerialized,
            CerberusRegistrySavedData::toSerialized
    );
    private static final SavedDataType<CerberusRegistrySavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Olympus.MOD_ID, "cerberus_registry"),
            CerberusRegistrySavedData::new,
            CODEC
    );

    private final Map<UUID, UUID> activeByOwner = new HashMap<>();

    private CerberusRegistrySavedData() {
    }

    public static CerberusRegistrySavedData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("The overworld is not available");
        }
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean hasActiveCerberus(UUID ownerId) {
        return activeByOwner.containsKey(ownerId);
    }

    public UUID getActiveCerberusId(UUID ownerId) {
        return activeByOwner.get(ownerId);
    }

    public boolean register(UUID ownerId, UUID cerberusId) {
        if (activeByOwner.containsKey(ownerId)) {
            return false;
        }
        activeByOwner.put(ownerId, cerberusId);
        setDirty();
        return true;
    }

    public void clearActiveRegistryEntry(UUID ownerId, UUID cerberusId) {
        if (activeByOwner.remove(ownerId, cerberusId)) {
            setDirty();
        }
    }

    private static CerberusRegistrySavedData fromSerialized(Map<String, String> serialized) {
        CerberusRegistrySavedData data = new CerberusRegistrySavedData();
        serialized.forEach((ownerId, cerberusId) -> {
            try {
                data.activeByOwner.put(UUID.fromString(ownerId), UUID.fromString(cerberusId));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed entries rather than preventing the world from loading.
            }
        });
        return data;
    }

    private Map<String, String> toSerialized() {
        Map<String, String> serialized = new HashMap<>();
        activeByOwner.forEach((ownerId, cerberusId) ->
                serialized.put(ownerId.toString(), cerberusId.toString())
        );
        return serialized;
    }
}
