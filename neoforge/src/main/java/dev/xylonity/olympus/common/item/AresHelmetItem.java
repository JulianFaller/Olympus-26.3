package dev.xylonity.olympus.common.item;

import dev.xylonity.olympus.common.util.OlympusTooltip;
import dev.xylonity.olympus.config.OlympusConfig;
import dev.xylonity.olympus.registry.OlympusItems;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosSlotTypes;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class AresHelmetItem extends Item implements ICurioItem {
    private static final Map<UUID, Integer> LAUNCH_CHARGE = new HashMap<>();
    private static final Set<UUID> LAUNCH_FALL_PROTECTION = new HashSet<>();
    private static final Set<UUID> LAUNCH_LEFT_GROUND = new HashSet<>();

    public AresHelmetItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(final SlotContext slotContext, final ItemStack stack) {
        return CuriosSlotTypes.Preset.HEAD.id().equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(final SlotContext slotContext, final ItemStack stack) {
        return canEquip(slotContext, stack) && CuriosApi.getCuriosInventory(slotContext.entity())
                .flatMap(handler -> handler.getStacksHandler(slotContext.identifier()))
                .map(handler -> handler.getStacks().getStackInSlot(slotContext.index()).isEmpty())
                .orElse(false);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        OlympusTooltip.append(tooltip, "ares_helmet", 0xB84A3B,
                OlympusTooltip.ability(1,
                        OlympusTooltip.property("strength", "I"),
                        OlympusTooltip.property("regeneration", "I"),
                        OlympusTooltip.property("speed", "I"),
                        OlympusTooltip.property("jump_boost", "I")
                ),
                OlympusTooltip.ability(2,
                        OlympusTooltip.property("launch_charge", "2 s")
                ));
    }

    public static boolean isEquipped(final LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .map(handler -> handler.isEquipped(OlympusItems.ARES_HELMET.get()))
                .orElse(false);
    }

    public static void updateLaunchCharge(final ServerPlayer player) {
        final int maxChargeTicks = Math.max(1, OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.aresHelmetLaunchMaxChargeSeconds.get()));
        final int minChargeTicks = Math.min(maxChargeTicks, OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.aresHelmetLaunchMinChargeSeconds.get()));
        final UUID playerId = player.getUUID();
        final int charge = LAUNCH_CHARGE.getOrDefault(playerId, 0);

        if (!isEquipped(player) || player.getAbilities().flying) {
            LAUNCH_CHARGE.remove(playerId);
            return;
        }

        if (player.isShiftKeyDown() && (player.onGround() || charge > 0)) {
            final int nextCharge = Math.min(charge + 1, maxChargeTicks);
            LAUNCH_CHARGE.put(playerId, nextCharge);
            if (nextCharge >= maxChargeTicks && charge < maxChargeTicks) {
                spawnMaximumChargeParticles(player);
            }
            else if (nextCharge < maxChargeTicks && nextCharge % 4 == 0) {
                spawnChargeParticles(player, nextCharge);
            }
            else if (nextCharge >= maxChargeTicks && player.level().getGameTime() % 4 == 0) {
                spawnMaximumChargePulse(player);
            }
        }
        else if (!player.isShiftKeyDown()) {
            LAUNCH_CHARGE.remove(playerId);
        }
    }

    public static void activateLaunch(final ServerPlayer player) {
        final UUID playerId = player.getUUID();
        final int charge = LAUNCH_CHARGE.getOrDefault(playerId, 0);
        final int maxChargeTicks = Math.max(1, OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.aresHelmetLaunchMaxChargeSeconds.get()));
        final int minChargeTicks = Math.min(maxChargeTicks, OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.aresHelmetLaunchMinChargeSeconds.get()));
        if (!isEquipped(player) || !player.isShiftKeyDown() || charge < minChargeTicks
                || player.getAbilities().flying || player.isInWater() || player.isFallFlying()) {
            return;
        }

        LAUNCH_CHARGE.remove(playerId);
        final double chargeProgress = maxChargeTicks == minChargeTicks ? 1.0D : (double) (charge - minChargeTicks) / (maxChargeTicks - minChargeTicks);
        final double upwardBoost = OlympusConfig.INSTANCE.aresHelmetLaunchMinUpwardBoost.get()
                + chargeProgress * (OlympusConfig.INSTANCE.aresHelmetLaunchMaxUpwardBoost.get() - OlympusConfig.INSTANCE.aresHelmetLaunchMinUpwardBoost.get());
        final double forwardBoost = OlympusConfig.INSTANCE.aresHelmetLaunchMinForwardBoost.get()
                + chargeProgress * (OlympusConfig.INSTANCE.aresHelmetLaunchMaxForwardBoost.get() - OlympusConfig.INSTANCE.aresHelmetLaunchMinForwardBoost.get());
        final double yawRadians = Math.toRadians(player.getYRot());
        final double forwardX = -Math.sin(yawRadians);
        final double forwardZ = Math.cos(yawRadians);
        final var velocity = player.getDeltaMovement();
        player.setDeltaMovement(
                velocity.x + forwardX * forwardBoost,
                Math.max(velocity.y, upwardBoost),
                velocity.z + forwardZ * forwardBoost
        );
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.fallDistance = 0;
        LAUNCH_FALL_PROTECTION.add(playerId);
        LAUNCH_LEFT_GROUND.remove(playerId);
    }

    public static boolean hasLaunchFallProtection(final ServerPlayer player) {
        return LAUNCH_FALL_PROTECTION.contains(player.getUUID());
    }

    public static void updateLaunchFallProtection(final ServerPlayer player) {
        final UUID playerId = player.getUUID();
        if (!LAUNCH_FALL_PROTECTION.contains(playerId)) {
            return;
        }

        if (!player.onGround()) {
            LAUNCH_LEFT_GROUND.add(playerId);
        }
        else if (LAUNCH_LEFT_GROUND.remove(playerId)) {
            LAUNCH_FALL_PROTECTION.remove(playerId);
        }
    }

    public static void clearLaunchFallProtection(final ServerPlayer player) {
        LAUNCH_FALL_PROTECTION.remove(player.getUUID());
        LAUNCH_LEFT_GROUND.remove(player.getUUID());
    }

    public static void clearLaunchCharge(final ServerPlayer player) {
        LAUNCH_CHARGE.remove(player.getUUID());
        clearLaunchFallProtection(player);
    }

    private static void spawnChargeParticles(final ServerPlayer player, final int charge) {
        final ServerLevel level = (ServerLevel) player.level();
        final int particleCount = 1 + charge / 8;
        level.sendParticles(
                ParticleTypes.END_ROD,
                player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                particleCount,
                0.3D, player.getBbHeight() * 0.4D, 0.3D,
                0.015D
        );
    }

    private static void spawnMaximumChargeParticles(final ServerPlayer player) {
        final ServerLevel level = (ServerLevel) player.level();
        final double y = player.getY() + player.getBbHeight() * 0.5D;
        level.sendParticles(ParticleTypes.FLAME, player.getX(), y, player.getZ(), 18, 0.45D, player.getBbHeight() * 0.5D, 0.45D, 0.025D);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), y, player.getZ(), 10, 0.4D, player.getBbHeight() * 0.5D, 0.4D, 0.02D);
    }

    private static void spawnMaximumChargePulse(final ServerPlayer player) {
        final ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(), 4, 0.3D, player.getBbHeight() * 0.4D, 0.3D, 0.015D);
    }
}
