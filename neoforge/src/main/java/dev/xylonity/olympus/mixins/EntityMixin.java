package dev.xylonity.olympus.mixins;

import dev.xylonity.olympus.common.item.HelmetOfHadesItem;
import dev.xylonity.olympus.config.OlympusConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyVariable(method = "moveRelative", at = @At("HEAD"), argsOnly = true)
    private float olympus$increaseHadesHelmetLavaSpeed(final float baseSpeed) {
        final Entity entity = (Entity) (Object) this;
        if (entity instanceof LivingEntity livingEntity
                && entity.isInLava()
                && HelmetOfHadesItem.isEquipped(livingEntity)) {
            return baseSpeed * (1.0F + OlympusConfig.INSTANCE.helmetOfHadesLavaMovementSpeedBonus.get().floatValue());
        }

        return baseSpeed;
    }
}
