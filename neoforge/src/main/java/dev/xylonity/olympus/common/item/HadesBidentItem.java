package dev.xylonity.olympus.common.item;

import dev.xylonity.olympus.common.item.ability.SummonCerberusAbility;
import dev.xylonity.olympus.common.item.ability.BidentMinionAbility;
import dev.xylonity.olympus.common.util.OlympusTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import java.util.List;
import java.util.function.Consumer;

public final class HadesBidentItem extends Item {
    private static final int SUMMON_CHARGE_TICKS = 100;
    private static final int USE_DURATION_TICKS = 72_000;

    public HadesBidentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                BidentMinionAbility.Result result = BidentMinionAbility.summon(serverPlayer);
                String key = switch (result) {
                    case SUMMONED -> "message.olympus.minion_summoned";
                    case TELEPORTED -> "message.olympus.minions_recalled";
                    case NO_RECENT_MOBS -> "message.olympus.no_recent_mobs";
                    case LIMIT_REACHED -> "message.olympus.minion_limit";
                    case SPAWN_BLOCKED -> "message.olympus.minion_spawn_blocked";
                };
                sendFeedback(serverPlayer, key);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && SummonCerberusAbility.hasActiveCerberus(level.getServer(), serverPlayer.getUUID())) {
            sendFeedback(serverPlayer, "message.olympus.cerberus_already_active");
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_DURATION_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPEAR;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        OlympusTooltip.append(tooltip, "hades_bident", 0x9B7FD3,
                OlympusTooltip.ability(1),
                OlympusTooltip.ability(2),
                OlympusTooltip.ability(3));
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int ticksRemaining) {
        int useTicks = USE_DURATION_TICKS - ticksRemaining;
        if (level.isClientSide()) {
            if (useTicks < SUMMON_CHARGE_TICKS) {
                spawnSummonParticles(level, user, useTicks);
            }
            return;
        }
        if (useTicks < SUMMON_CHARGE_TICKS) {
            return;
        }
        if (!(user instanceof ServerPlayer player)) {
            user.stopUsingItem();
            return;
        }

        SummonCerberusAbility.Result result = SummonCerberusAbility.summon(player);
        String messageKey = switch (result) {
            case SUMMONED -> "message.olympus.cerberus_summoned";
            case ALREADY_ACTIVE -> "message.olympus.cerberus_already_active";
            case SPAWN_BLOCKED -> "message.olympus.cerberus_spawn_blocked";
        };
        sendFeedback(player, messageKey);
        player.stopUsingItem();
    }

    private static void spawnSummonParticles(Level level, LivingEntity user, int useTicks) {
        int particleCount = 2 + useTicks / 10;
        for (int i = 0; i < particleCount; i++) {
            double angle = user.getRandom().nextDouble() * Math.PI * 2.0;
            double radius = 0.45 + user.getRandom().nextDouble() * 0.55;
            double x = user.getX() + Math.cos(angle) * radius;
            double y = user.getY() + user.getRandom().nextDouble() * user.getBbHeight();
            double z = user.getZ() + Math.sin(angle) * radius;
            level.addParticle(ParticleTypes.PORTAL, x, y, z, 0.0, 0.02, 0.0);
        }
    }

    private static void sendFeedback(ServerPlayer player, String translationKey) {
        if (player.connection != null) {
            player.sendSystemMessage(Component.translatable(translationKey), true);
        }
    }
}
