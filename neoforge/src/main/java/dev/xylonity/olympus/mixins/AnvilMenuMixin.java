package dev.xylonity.olympus.mixins;

import dev.xylonity.olympus.registry.OlympusItems;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void olympus$allowFreeAresShieldRepair(final Player player, final boolean hasResult, final CallbackInfoReturnable<Boolean> callback) {
        final AnvilMenu anvil = (AnvilMenu) (Object) this;
        if (hasResult
                && anvil.getSlot(0).getItem().is(OlympusItems.ARES_SHIELD.get())
                && anvil.getSlot(1).getItem().is(Items.GOLD_BLOCK)) {
            callback.setReturnValue(true);
        }
    }
}
