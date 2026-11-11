package dev.xylonity.olympus.common.item;

import dev.xylonity.olympus.common.util.OlympusTooltip;
import dev.xylonity.olympus.config.OlympusConfig;
import dev.xylonity.olympus.registry.OlympusItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

/** Ares' shield uses Minecraft's native shield blocking behaviour. */
public final class AresShieldItem extends ShieldItem {

    public AresShieldItem(final Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final Item.TooltipContext context, final TooltipDisplay display,
                                final Consumer<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        OlympusTooltip.append(tooltip, "ares_shield", 0xB84A3B,
                OlympusTooltip.ability(1,
                        OlympusTooltip.property("knockback_immunity", "✓")
                ),
                OlympusTooltip.ability(2,
                        OlympusTooltip.property("strength", Integer.toString(OlympusConfig.INSTANCE.aresShieldStrengthLevel.get())),
                        OlympusTooltip.property("duration", OlympusTooltip.seconds(OlympusConfig.INSTANCE.aresShieldStrengthSeconds.get()))
                ),
                OlympusTooltip.ability(3,
                        OlympusTooltip.property("health", OlympusTooltip.number(OlympusConfig.INSTANCE.aresShieldTotemHealth.get().doubleValue() / 2.0D)),
                        OlympusTooltip.property("repair", "Goldblock")
                ));
    }

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        return isCharged(stack) ? super.use(level, player, hand) : InteractionResult.FAIL;
    }

    public static boolean isCharged(final ItemStack stack) {
        return stack.is(OlympusItems.ARES_SHIELD.get())
                && stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    public static boolean tryActivateTotem(final ServerPlayer player) {
        final ItemStack shield = getHeldShield(player);
        if (!isCharged(shield)) {
            return false;
        }

        shield.setDamageValue(shield.getMaxDamage() - 1);
        player.setHealth(Math.min(player.getMaxHealth(), OlympusConfig.INSTANCE.aresShieldTotemHealth.get().floatValue()));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static ItemStack getHeldShield(final LivingEntity entity) {
        if (isShield(entity.getMainHandItem())) {
            return entity.getMainHandItem();
        }
        if (isShield(entity.getOffhandItem())) {
            return entity.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    private static boolean isShield(final ItemStack stack) {
        return stack.is(OlympusItems.ARES_SHIELD.get());
    }
}
