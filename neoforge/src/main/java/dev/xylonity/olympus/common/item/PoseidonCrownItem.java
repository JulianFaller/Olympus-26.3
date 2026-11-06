package dev.xylonity.olympus.common.item;

import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.common.util.OlympusTooltip;
import dev.xylonity.olympus.registry.OlympusItems;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
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

    public PoseidonCrownItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(final SlotContext slotContext, final ItemStack stack) {
        return CuriosSlotTypes.Preset.HEAD.id().equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(final SlotContext slotContext, final ItemStack stack) {
        return canEquip(slotContext, stack);
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
                ));
    }

    public static boolean isEquipped(final LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .map(handler -> handler.isEquipped(OlympusItems.POSEIDON_CROWN.get()))
                .orElse(false);
    }

}
