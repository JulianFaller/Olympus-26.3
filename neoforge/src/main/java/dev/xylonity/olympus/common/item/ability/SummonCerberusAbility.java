package dev.xylonity.olympus.common.item.ability;

import dev.xylonity.olympus.registry.OlympusEntities;
import dev.xylonity.olympus.common.entity.CerberusEntity;
import dev.xylonity.olympus.common.entity.CerberusRegistrySavedData;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

public final class SummonCerberusAbility {
    private SummonCerberusAbility() {
    }

    public static boolean hasActiveCerberus(MinecraftServer server, UUID ownerId) {
        return CerberusRegistrySavedData.get(server).hasActiveCerberus(ownerId);
    }

    public static Result summon(ServerPlayer player) {
        ServerLevel level = player.level();
        CerberusRegistrySavedData registry = CerberusRegistrySavedData.get(level.getServer());
        UUID ownerId = player.getUUID();
        if (registry.hasActiveCerberus(ownerId)) {
            return Result.ALREADY_ACTIVE;
        }

        CerberusEntity cerberus = OlympusEntities.CERBERUS.get()
                .create(level, EntitySpawnReason.MOB_SUMMONED);
        if (cerberus == null) {
            return Result.SPAWN_BLOCKED;
        }

        cerberus.tame(player);
        cerberus.setPersistenceRequired();

        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z);
        if (forward.lengthSqr() < 1.0E-6) {
            forward = new Vec3(0.0, 0.0, 1.0);
        } else {
            forward = forward.normalize();
        }
        Vec3 origin = player.position();
        Vec3 position = origin.add(forward.scale(1.8));

        cerberus.setPos(position.x, player.getY(), position.z);
        cerberus.setYRot(player.getYRot());
        cerberus.setXRot(0.0F);
        if (!level.isInWorldBounds(cerberus.blockPosition()) || !level.noCollision(cerberus)) {
            return Result.SPAWN_BLOCKED;
        }
        if (!level.addFreshEntity(cerberus)) {
            return Result.SPAWN_BLOCKED;
        }
        if (!registry.register(ownerId, cerberus.getUUID())) {
            cerberus.discard();
            return Result.ALREADY_ACTIVE;
        }
        return Result.SUMMONED;

    }

    public enum Result {
        SUMMONED,
        ALREADY_ACTIVE,
        SPAWN_BLOCKED
    }
}
