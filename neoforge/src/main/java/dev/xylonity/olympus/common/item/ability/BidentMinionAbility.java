package dev.xylonity.olympus.common.item.ability;

import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.config.OlympusConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import com.mojang.serialization.Codec;

/** Saved recent kill history and spawn logic for Bident-bound companions. */
public final class BidentMinionAbility {
    private static final String OWNER_TAG = Olympus.MOD_ID + ":bident_owner";
    private static final String MANUAL_ATTACK_TAG = Olympus.MOD_ID + ":bident_manual_attack";
    private static final String COUNTED_TAG = Olympus.MOD_ID + ":bident_slot_counted";
    private static final Codec<Map<String, List<String>>> CODEC = Codec.unboundedMap(
            Codec.STRING, Codec.STRING.listOf());
    private static final SavedDataType<History> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Olympus.MOD_ID, "bident_mob_history"),
            History::new,
            CODEC.xmap(History::new, History::serialize));

    private BidentMinionAbility() { }

    public static void recordKill(ServerPlayer player, Mob mob) {
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        if (entityId.equals("minecraft:ender_dragon") || entityId.equals("minecraft:warden") || entityId.equals("minecraft:wither")) return;
        History history = get(player.level().getServer());
        List<String> recent = new ArrayList<>(history.recent.getOrDefault(player.getStringUUID(), List.of()));
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
        recent.add(id);
        while (recent.size() > 3) recent.removeFirst();
        history.recent.put(player.getStringUUID(), recent);
        history.setCursor(player.getStringUUID(), recent.size() - 1);
        history.setDirty();
    }

    public static Result summon(ServerPlayer player) {
        History history = get(player.level().getServer());
        String owner = player.getStringUUID();
        List<String> storedRecent = history.recent.get(owner);
        if (storedRecent == null || storedRecent.isEmpty()) return Result.NO_RECENT_MOBS;
        List<String> recent = new ArrayList<>(storedRecent);
        int active = history.activeCount(owner);
        final int maximumMinions = OlympusConfig.INSTANCE.hadesBidentMaximumMinions.get();
        if (active >= maximumMinions) {
            return recallMinions(player, owner) ? Result.TELEPORTED : Result.LIMIT_REACHED;
        }

        // Cycle through the last three kills without changing their chronological order.
        int cursor = Math.min(history.cursor(owner, recent.size() - 1), recent.size() - 1);
        for (int offset = 0; offset < recent.size(); offset++) {
            int index = Math.floorMod(cursor - offset, recent.size());
            Identifier id = Identifier.tryParse(recent.get(index));
            if (id == null) continue;
            var type = BuiltInRegistries.ENTITY_TYPE.get(id).map(reference -> reference.value()).orElse(null);
            if (type == null) continue;
            Entity created = type.create(player.level(), EntitySpawnReason.MOB_SUMMONED);
            if (!(created instanceof Mob mob)) continue;
            Vec3 look = player.getLookAngle();
            Vec3 pos = player.position().add(look.x * 1.8, 0, look.z * 1.8);
            mob.setPos(pos.x, player.getY(), pos.z);
            mob.setPersistenceRequired();
            mob.getPersistentData().putString(OWNER_TAG, owner);
            mob.getPersistentData().putBoolean(COUNTED_TAG, true);
            boolean friendly = !(mob instanceof Enemy);
            if (friendly) {
                var attackDamage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attackDamage == null || attackDamage.getValue() <= 0.0) {
                    // Passive creatures without their own attack damage use a fixed two-damage strike.
                    mob.getPersistentData().putBoolean(MANUAL_ATTACK_TAG, true);
                }
            }
            if (!player.level().noCollision(mob) || !player.level().addFreshEntity(mob)) return Result.SPAWN_BLOCKED;
            history.setActiveCount(owner, active + 1);
            history.setCursor(owner, Math.floorMod(index - 1, recent.size()));
            history.setDirty();
            return Result.SUMMONED;
        }
        return Result.SPAWN_BLOCKED;
    }

    public static void tickMinion(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        String ownerId = mob.getPersistentData().getString(OWNER_TAG).orElse("");
        if (ownerId.isEmpty()) return;
        if (mob.tickCount % 5 == 0) {
            level.sendParticles(
                    ParticleTypes.WITCH,
                    mob.getX(), mob.getY() + mob.getBbHeight() + 0.25, mob.getZ(),
                    2,
                    0.18, 0.04, 0.18,
                    0.01
            );
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(UUID.fromString(ownerId));
        if (owner == null || owner.level() != level) {
            mob.setTarget(null);
            return;
        }
        Entity target = owner.getLastHurtMob();
        if (target instanceof net.minecraft.world.entity.LivingEntity living && target.isAlive() && target != owner) {
            if (mob.getTarget() != living) mob.setTarget(living);
        } else if (mob.getTarget() != null && mob.getTarget() != owner) {
            mob.setTarget(null);
        }
        if (mob.getPersistentData().getBoolean(MANUAL_ATTACK_TAG).orElse(false)
                && mob.getTarget() != null && mob.getTarget().isAlive()) {
            if (mob.distanceToSqr(mob.getTarget()) > 2.25) {
                if (mob.getNavigation().isDone()) mob.getNavigation().moveTo(mob.getTarget(), 1.15);
            } else {
                mob.getNavigation().stop();
            }
        }
        if (mob.distanceToSqr(owner) > 100.0 && mob.getTarget() == null) {
            mob.getNavigation().moveTo(owner, 1.15);
        } else if (mob.distanceToSqr(owner) < 9.0 && mob.getTarget() == null) {
            mob.getNavigation().stop();
        }
        if (mob.getPersistentData().getBoolean(MANUAL_ATTACK_TAG).orElse(false)
                && mob.getTarget() != null && mob.distanceToSqr(mob.getTarget()) <= 4.0
                && mob.tickCount % 20 == 0) {
            mob.getTarget().hurtServer(level, level.damageSources().mobAttack(mob), OlympusConfig.INSTANCE.hadesBidentManualAttackDamage.get().floatValue());
        }
    }

    private static boolean recallMinions(ServerPlayer player, String owner) {
        int index = 0;
        for (Entity entity : ((ServerLevel) player.level()).getEntities().getAll()) {
            if (!(entity instanceof Mob mob)
                    || !owner.equals(mob.getPersistentData().getString(OWNER_TAG).orElse(""))) continue;
            double angle = index * (Math.PI * 2.0 / Math.max(1, OlympusConfig.INSTANCE.hadesBidentMaximumMinions.get()));
            double x = player.getX() + Math.cos(angle) * 1.5;
            double z = player.getZ() + Math.sin(angle) * 1.5;
            mob.teleportTo(x, player.getY(), z);
            mob.setTarget(null);
            mob.getNavigation().stop();
            index++;
        }
        return index > 0;
    }

    public static boolean isMinion(Mob mob) {
        return isMinion((Entity) mob);
    }

    public static boolean isMinion(Entity entity) {
        return entity.getPersistentData().getString(OWNER_TAG).filter(value -> !value.isEmpty()).isPresent();
    }

    public static void onMinionRemoved(Entity entity) {
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason != Entity.RemovalReason.KILLED && reason != Entity.RemovalReason.DISCARDED) return;
        String owner = entity.getPersistentData().getString(OWNER_TAG).orElse("");
        if (owner.isEmpty() || !(entity.level() instanceof ServerLevel level)) return;
        if (!entity.getPersistentData().getBoolean(COUNTED_TAG).orElse(true)) return;
        entity.getPersistentData().putBoolean(COUNTED_TAG, false);
        History history = get(level.getServer());
        history.setActiveCount(owner, Math.max(0, history.activeCount(owner) - 1));
        history.setDirty();
    }

    public static void resetWhenDropped(ServerPlayer player) {
        String owner = player.getStringUUID();
        History history = get(player.level().getServer());
        history.recent.remove(owner);
        history.setCursor(owner, 0);
        history.setActiveCount(owner, 0);
        history.setDirty();

        List<Mob> ownedMinions = new ArrayList<>();
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            for (Entity entity : level.getEntities().getAll()) {
                if (entity instanceof Mob mob
                        && owner.equals(mob.getPersistentData().getString(OWNER_TAG).orElse(""))) {
                    ownedMinions.add(mob);
                }
            }
        }
        for (Mob mob : ownedMinions) {
            if (mob.level() instanceof ServerLevel level && mob.isAlive()) mob.kill(level);
        }
    }

    public static net.minecraft.world.entity.LivingEntity getOwnerTarget(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return null;
        String ownerId = mob.getPersistentData().getString(OWNER_TAG).orElse("");
        if (ownerId.isEmpty()) return null;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(UUID.fromString(ownerId));
        return owner == null ? null : owner.getLastHurtMob();
    }

    private static History get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public enum Result { SUMMONED, TELEPORTED, NO_RECENT_MOBS, LIMIT_REACHED, SPAWN_BLOCKED }

    private static final class History extends SavedData {
        private final Map<String, List<String>> recent = new HashMap<>();
        private History() { }
        private History(Map<String, List<String>> data) { recent.putAll(data); }
        private Map<String, List<String>> serialize() { return new HashMap<>(recent); }
        private int activeCount(String owner) {
            try {
                return Integer.parseInt(recent.getOrDefault("active:" + owner, List.of("0")).getFirst());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        private void setActiveCount(String owner, int count) {
            recent.put("active:" + owner, List.of(Integer.toString(count)));
        }
        private int cursor(String owner, int fallback) {
            try {
                return Integer.parseInt(recent.getOrDefault("cursor:" + owner, List.of(Integer.toString(fallback))).getFirst());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        private void setCursor(String owner, int index) {
            recent.put("cursor:" + owner, List.of(Integer.toString(index)));
        }
    }
}
