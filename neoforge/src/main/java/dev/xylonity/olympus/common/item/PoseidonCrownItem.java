package dev.xylonity.olympus.common.item;

import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.common.util.OlympusTooltip;
import dev.xylonity.olympus.registry.OlympusItems;
import dev.xylonity.olympus.registry.OlympusParticles;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.GeyserParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import top.theillusivec4.curios.api.CurioAttributeModifiers;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosSlotTypes;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class PoseidonCrownItem extends Item implements ICurioItem {
    private static final int MAX_BOOST_CHARGE_TICKS = 40;
    private static final int MIN_BOOST_CHARGE_TICKS = 10;
    private static final Map<UUID, Integer> BOOST_CHARGE = new HashMap<>();

    public PoseidonCrownItem(final Properties properties) {
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
    public CurioAttributeModifiers getDefaultCurioAttributeModifiers(final ItemStack stack) {
        // Vanilla submerged mining speed defaults to 0.2; Aqua Affinity brings it to 1.0.
        return CurioAttributeModifiers.builder()
                .addModifier(
                        Attributes.SUBMERGED_MINING_SPEED,
                        new AttributeModifier(Olympus.of("poseidon_crown_submerged_mining_speed"), 0.8, AttributeModifier.Operation.ADD_VALUE),
                        CuriosSlotTypes.Preset.HEAD.id()
                )
                .build();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display, final Consumer<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        OlympusTooltip.append(tooltip, "poseidon_crown", 0x42C7D1,
                OlympusTooltip.ability(1,
                        OlympusTooltip.property("sea_mobs_ignore", "✓"),
                        OlympusTooltip.property("mining_fatigue_immunity", "✓")
                ),
                OlympusTooltip.ability(2,
                        OlympusTooltip.property("regeneration", "II"),
                        OlympusTooltip.property("strength", "II"),
                        OlympusTooltip.property("saturation", "✓"),
                        OlympusTooltip.property("bonus_hearts", "10")
                ),
                OlympusTooltip.ability(3,
                        OlympusTooltip.property("fall_damage_immunity", "✓")
                ),
                OlympusTooltip.ability(4,
                        OlympusTooltip.property("water_boost_charge", "2 s")
                ));
    }

    public static boolean isEquipped(final LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .map(handler -> handler.isEquipped(OlympusItems.POSEIDON_CROWN.get()))
                .orElse(false);
    }

    public static void updateWaterBoost(final ServerPlayer player) {
        final UUID playerId = player.getUUID();
        final int charge = BOOST_CHARGE.getOrDefault(playerId, 0);

        if (!isEquipped(player) || player.getAbilities().flying) {
            BOOST_CHARGE.remove(playerId);
            return;
        }

        if (player.isShiftKeyDown() && (player.onGround() || charge > 0)) {
            final int nextCharge = Math.min(charge + 1, MAX_BOOST_CHARGE_TICKS);
            BOOST_CHARGE.put(playerId, nextCharge);
        }
        else if (!player.isShiftKeyDown()) {
            BOOST_CHARGE.remove(playerId);
        }
    }

    public static void activateWaterBoost(final ServerPlayer player) {
        final UUID playerId = player.getUUID();
        final int charge = BOOST_CHARGE.getOrDefault(playerId, 0);
        if (!isEquipped(player) || !player.isShiftKeyDown() || charge < MIN_BOOST_CHARGE_TICKS
                || player.getAbilities().flying || player.isInWater() || player.isFallFlying()) {
            return;
        }

        BOOST_CHARGE.remove(playerId);
        final double chargeProgress = (double) (charge - MIN_BOOST_CHARGE_TICKS) / (MAX_BOOST_CHARGE_TICKS - MIN_BOOST_CHARGE_TICKS);
        final double upwardBoost = chargeProgress < 0.5D
                ? 0.9D + chargeProgress * 0.2D
                : 1.0D + (chargeProgress - 0.5D) * 0.7D;
        final var velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, Math.max(velocity.y, upwardBoost), velocity.z);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.fallDistance = 0;

        final ServerLevel level = (ServerLevel) player.level();
        final double x = player.getX();
        final double y = player.getY() + 0.1D;
        final double z = player.getZ();
        final int waterBlocks = chargeProgress < 0.5D ? 1 : 2;
        level.sendParticles(new GeyserParticleOptions(OlympusParticles.POSEIDON_GEYSER.get(), waterBlocks), x, y, z, 1, 0, 0, 0, 0);
        level.playSound(null, x, y, z, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0F, 0.75F);
    }

    public static void clearWaterBoost(final ServerPlayer player) {
        BOOST_CHARGE.remove(player.getUUID());
    }

}
