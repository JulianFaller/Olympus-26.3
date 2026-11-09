package dev.xylonity.olympus.common.item;

import dev.xylonity.olympus.common.util.OlympusTooltip;
import dev.xylonity.olympus.config.OlympusConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;

import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AmuletOfHadesItem extends Item {

    private static final String TAG_COOLDOWN_END = "olympus_hades_amulet_cooldown_end";
    private static final Map<UUID, ResourceKey<Level>> PENDING_DIMENSION_SYNC = new HashMap<>();

    public AmuletOfHadesItem(final Properties properties) {
        super(properties);
    }

    @Override
    public @NonNull InteractionResult use(final @NonNull Level level, final @NonNull Player player, final @NonNull InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide() && !level.dimension().equals(Level.OVERWORLD)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.translatable("message.olympus.amulet_of_hades.overworld_only"), true);
            }
            return InteractionResult.FAIL;
        }

        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(final @NonNull ItemStack stack, final @NonNull LivingEntity user) {
        return OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.amuletOfHadesChannelSeconds.get());
    }

    @Override
    public @NonNull ItemUseAnimation getUseAnimation(final @NonNull ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public void onUseTick(final @NonNull Level level, final @NonNull LivingEntity user, final @NonNull ItemStack stack, final int remainingUseDuration) {
        if (level.isClientSide()) {
            spawnChannelParticles(level, user, getChannelTicks() - remainingUseDuration);
        }
    }

    @Override
    public @NonNull ItemStack finishUsingItem(final @NonNull ItemStack stack, final @NonNull Level level, final @NonNull LivingEntity user) {
        if (!(level instanceof ServerLevel source) || !(user instanceof ServerPlayer player)) {
            return stack;
        }
        if (!source.dimension().equals(Level.OVERWORLD)) {
            player.sendSystemMessage(Component.translatable("message.olympus.amulet_of_hades.overworld_only"), true);
            return stack;
        }

        final ServerLevel nether = source.getServer().getLevel(Level.NETHER);
        if (nether == null) {
            player.sendSystemMessage(Component.translatable("message.olympus.amulet_of_hades.teleport_failed"), true);
            return stack;
        }

        final double targetX = player.getX() / 8.0D;
        final double targetZ = player.getZ() / 8.0D;
        final BlockPos safePos = findSafePosition(nether, targetX, targetZ);
        if (safePos == null) {
            player.sendSystemMessage(Component.translatable("message.olympus.amulet_of_hades.teleport_failed"), true);
            return stack;
        }

        final int cooldownTicks = OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.amuletOfHadesCooldownSeconds.get());
        setCooldownEnd(stack, (long) source.getServer().getTickCount() + cooldownTicks);
        if (!player.teleportTo(nether, safePos.getX() + 0.5D, safePos.getY(), safePos.getZ() + 0.5D, java.util.Set.of(), player.getYRot(), player.getXRot(), false)) {
            clearCooldownEnd(stack);
            player.sendSystemMessage(Component.translatable("message.olympus.amulet_of_hades.teleport_failed"), true);
            return stack;
        }
        player.sendSystemMessage(Component.translatable("message.olympus.amulet_of_hades.teleported"), true);
        return stack;
    }

    public static void onDimensionChanged(final ServerPlayer player) {
        PENDING_DIMENSION_SYNC.put(player.getUUID(), player.level().dimension());
    }

    public static void restoreCooldownIfNeeded(final ServerPlayer player) {
        final ServerLevel currentLevel = (ServerLevel) player.level();
        final long now = currentLevel.getServer().getTickCount();
        final ResourceKey<Level> pendingDimension = PENDING_DIMENSION_SYNC.get(player.getUUID());
        final boolean forceClientSync = currentLevel.dimension().equals(pendingDimension);
        if (forceClientSync) {
            PENDING_DIMENSION_SYNC.remove(player.getUUID());
        }
        for (final ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            restoreCooldown(player, stack, now, forceClientSync);
        }
        restoreCooldown(player, player.getOffhandItem(), now, forceClientSync);
        restoreCooldown(player, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD), now, forceClientSync);
        restoreCooldown(player, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST), now, forceClientSync);
        restoreCooldown(player, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS), now, forceClientSync);
        restoreCooldown(player, player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET), now, forceClientSync);
    }

    private static void restoreCooldown(final ServerPlayer player, final ItemStack stack, final long now, final boolean forceClientSync) {
        if (!stack.is(dev.xylonity.olympus.registry.OlympusItems.AMULET_OF_HADES.get())) {
            return;
        }
        final long cooldownEnd = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLongOr(TAG_COOLDOWN_END, 0);
        final int remaining = (int) Math.clamp(cooldownEnd - now, 0, Integer.MAX_VALUE);
        if (remaining > 0) {
            final Identifier cooldownGroup = player.getCooldowns().getCooldownGroup(stack);
            if (forceClientSync) {
                player.getCooldowns().removeCooldown(cooldownGroup);
            }
            if (forceClientSync || !player.getCooldowns().isOnCooldown(stack)) {
                player.getCooldowns().addCooldown(stack, remaining);
            }
        }
    }

    private static void setCooldownEnd(final ItemStack stack, final long cooldownEnd) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(TAG_COOLDOWN_END, cooldownEnd));
    }

    private static void clearCooldownEnd(final ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(TAG_COOLDOWN_END));
    }

    @Override
    public void appendHoverText(final @NonNull ItemStack stack, final @NonNull TooltipContext context, final @NonNull TooltipDisplay display, final @NonNull Consumer<Component> tooltip, final @NonNull TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        OlympusTooltip.append(tooltip, "amulet_of_hades", 0x8F69B8, OlympusTooltip.ability(1));
    }

    private static BlockPos findSafePosition(final ServerLevel level, final double targetX, final double targetZ) {
        final int originX = net.minecraft.util.Mth.floor(targetX);
        final int originZ = net.minecraft.util.Mth.floor(targetZ);
        final int minY = Math.max(level.getMinY() + 1, 32);
        final int maxY = Math.min(level.getMaxY() - 2, 120);

        for (int radius = 0; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }
                    final int x = originX + dx;
                    final int z = originZ + dz;
                    for (int y = maxY; y >= minY; y--) {
                        final BlockPos feet = new BlockPos(x, y, z);
                        final BlockPos floor = feet.below();
                        final BlockState floorState = level.getBlockState(floor);
                        if (floorState.getFluidState().isEmpty() && floorState.isFaceSturdy(level, floor, net.minecraft.core.Direction.UP)
                                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                                && level.getFluidState(feet).isEmpty() && level.getFluidState(feet.above()).isEmpty()) {
                            return feet;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static void spawnChannelParticles(final Level level, final LivingEntity user, final int useTicks) {
        final int channelTicks = Math.max(1, OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.amuletOfHadesChannelSeconds.get()));
        final double progress = Math.min(1.0D, (double) useTicks / channelTicks);
        final double radius = 0.9D;
        final double height = user.getBbHeight();
        final int particleCount = 2 + (int) (progress * 10.0D);
        final double rotation = useTicks * (0.12D + progress * 0.28D);
        for (int i = 0; i < particleCount; i++) {
            final double angle = rotation + (Math.PI * 2.0D * i / particleCount);
            final double x = user.getX() + Math.cos(angle) * radius;
            final double y = user.getY() + 0.15D + ((useTicks * (0.09D + progress * 0.1D) + i * 0.37D) % Math.max(0.5D, height));
            final double z = user.getZ() + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.PORTAL, x, y, z, -Math.cos(angle) * 0.025D, 0.025D + progress * 0.045D, -Math.sin(angle) * 0.025D);
            if (i % 2 == 0) {
                level.addParticle(ParticleTypes.SOUL, x, y, z, 0.0D, 0.025D + progress * 0.06D, 0.0D);
            }
        }
    }

    private static int getChannelTicks() {
        return Math.max(1, OlympusConfig.secondsToTicks(OlympusConfig.INSTANCE.amuletOfHadesChannelSeconds.get()));
    }
}
