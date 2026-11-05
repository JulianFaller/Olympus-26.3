package dev.xylonity.olympus.common.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.instance.InstancedAnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class CerberusEntity extends Wolf implements GeoEntity {
    private static final RawAnimation WALK_ANIMATION = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache animatableCache = new InstancedAnimatableInstanceCache(this);

    public CerberusEntity(EntityType<? extends CerberusEntity> entityType, Level level) {
        super(entityType, level);
        setCustomName(Component.translatable("entity.olympus.cerberus"));
        setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Unlike an ordinary tamed wolf, Cerberus actively hunts nearby hostile mobs.
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void registerControllers(ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 5, state ->
                state.setAndContinue(state.isMoving() ? WALK_ANIMATION : IDLE_ANIMATION)
        ));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    @Override
    public void die(DamageSource damageSource) {
        clearActiveRegistryEntry();
        super.die(damageSource);
    }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
            clearActiveRegistryEntry();
        }
        super.onRemoval(reason);
    }

    private void clearActiveRegistryEntry() {
        if (level() instanceof ServerLevel serverLevel && getOwnerReference() != null) {
            CerberusRegistrySavedData.get(serverLevel.getServer()).clearActiveRegistryEntry(
                    getOwnerReference().getUUID(),
                    getUUID()
            );
        }
    }
}
