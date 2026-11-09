package dev.xylonity.olympus.common.event;

import dev.xylonity.olympus.Olympus;
import dev.xylonity.olympus.common.effect.InvisibilityOfHadesEffect;
import dev.xylonity.olympus.common.entity.HarpyEntity;
import dev.xylonity.olympus.common.util.ArtemisArrow;
import dev.xylonity.olympus.common.item.BracersOfZeusItem;
import dev.xylonity.olympus.common.item.AresHelmetItem;
import dev.xylonity.olympus.common.item.AmuletOfHadesItem;
import dev.xylonity.olympus.common.item.HelmetOfHadesItem;
import dev.xylonity.olympus.common.item.HermesSandalsItem;
import dev.xylonity.olympus.common.item.InstrumentsOfHephaestusItem;
import dev.xylonity.olympus.common.item.PersephoneCupItem;
import dev.xylonity.olympus.common.item.PoseidonCrownItem;
import dev.xylonity.olympus.common.item.SpearOfAresItem;
import dev.xylonity.olympus.config.OlympusConfig;
import dev.xylonity.olympus.registry.OlympusDamageTypes;
import dev.xylonity.olympus.registry.OlympusEntities;
import dev.xylonity.olympus.registry.OlympusItems;
import dev.xylonity.olympus.registry.OlympusParticles;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.enchanting.EnchantedEntityLootEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Olympus.MOD_ID)
public final class OlympusServerEvents {
    private static final TagKey<Item> KEEP_ON_DEATH = TagKey.create(Registries.ITEM, Olympus.of("keep_on_death"));
    private static final Map<java.util.UUID, List<ItemStack>> KEPT_ITEMS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerChangedDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AmuletOfHadesItem.onDimensionChanged(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDrops(final LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        final List<ItemStack> keptItems = new ArrayList<>();
        for (final ItemEntity drop : event.getDrops()) {
            if (drop.getItem().is(KEEP_ON_DEATH)) {
                keptItems.add(drop.getItem().copy());
                drop.discard();
            }
        }

        if (!keptItems.isEmpty()) {
            KEPT_ITEMS.put(player.getUUID(), keptItems);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(final PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        final List<ItemStack> keptItems = KEPT_ITEMS.remove(event.getOriginal().getUUID());
        if (keptItems == null) {
            return;
        }

        for (final ItemStack stack : keptItems) {
            if (!player.getInventory().add(stack)) {
                player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(final PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PoseidonCrownItem.clearWaterBoost(player);
            AresHelmetItem.clearLaunchCharge(player);
        }
    }

    @SubscribeEvent
    public static void registerAttributes(final EntityAttributeCreationEvent event) {
        OlympusEntities.registerAttributes(event);
        event.put(OlympusEntities.HARPY.get(), HarpyEntity.createAttributes().build());
        event.put(OlympusEntities.ELITE_HARPY.get(), HarpyEntity.createEliteAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(final RegisterSpawnPlacementsEvent event) {
        event.register(OlympusEntities.HARPY.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(OlympusEntities.ELITE_HARPY.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkAnyLightMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    @SubscribeEvent
    public static void onItemAttributeModifiers(final ItemAttributeModifierEvent event) {
        final ItemStack stack = event.getItemStack();
        if (stack.is(OlympusItems.POSEIDON_TRIDENT.get())) {
            event.replaceModifier(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, OlympusConfig.INSTANCE.poseidonTridentAttackDamageBonus.get(), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
            );
            event.replaceModifier(
                    NeoForgeMod.SWIM_SPEED,
                    new AttributeModifier(Olympus.of("poseidon_trident_swim_speed"), OlympusConfig.INSTANCE.poseidonTridentSwimSpeedBonus.get(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                    EquipmentSlotGroup.HAND
            );

        }
        else if (stack.is(OlympusItems.SPEAR_OF_ARES.get())) {
            event.replaceModifier(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, OlympusConfig.INSTANCE.aresSpearAttackDamageBonus.get(), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
            );

        }

    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(final LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && HelmetOfHadesItem.isEquipped(player)
                && player.level().dimension() == Level.NETHER
                && OlympusConfig.INSTANCE.helmetOfHadesFireResistanceEnabled.get()
                && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            return;
        }

        if (!OlympusConfig.INSTANCE.persephoneCupDamageAppliesToWeapons.get() || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Applies the extra damage given by the cup to the weapon in use
        final ItemStack weapon = event.getSource().getWeaponItem();
        final boolean isDirectAttack = event.getSource().is(DamageTypeTags.IS_PLAYER_ATTACK);
        final boolean isRangedWeaponAttack = event.getSource().getDirectEntity() != player && weapon != null && !weapon.isEmpty();
        if (!isDirectAttack && !isRangedWeaponAttack) {
            return;
        }

        final float damage = PersephoneCupItem.getEquippedDamageBonus(player);
        if (damage > 0) {
            event.setAmount(event.getAmount() + damage);
        }

    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamagePre(final LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (AresHelmetItem.hasLaunchFallProtection(player) && event.getSource().is(DamageTypeTags.IS_FALL)) {
            event.setNewDamage(0);
            AresHelmetItem.clearLaunchFallProtection(player);
            return;
        }

        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || event.getNewDamage() < player.getHealth() + player.getAbsorptionAmount()) {
            return;
        }

        // Prevents the player from dying on a mortal hit if the helmet or the cup are present
        if (HelmetOfHadesItem.tryActivateAbility(player) || PersephoneCupItem.tryActivateAbility(player)) {
            event.setNewDamage(0);
        }
    }

    @SubscribeEvent
    public static void onLivingChangeTarget(final LivingChangeTargetEvent event) {
        final LivingEntity target = event.getNewAboutToBeSetTarget();
        // Clears the current target when the invisibility of hades status effect is applied
        if (target != null && InvisibilityOfHadesEffect.preventsTargeting(target)) {
            event.setNewAboutToBeSetTarget(null);
            return;
        }

        if (target instanceof ServerPlayer player && event.getEntity() instanceof Mob mob) {
            final boolean protectedByHadesHelmet = HelmetOfHadesItem.isEquipped(player) && mob.level().dimension() == Level.NETHER && OlympusConfig.INSTANCE.helmetOfHadesNetherMobsIgnoreEnabled.get();
            final boolean protectedByPoseidonCrown = PoseidonCrownItem.isEquipped(player) && isSeaMob(mob) && OlympusConfig.INSTANCE.poseidonCrownSeaMobsIgnoreEnabled.get();
            if (protectedByHadesHelmet || protectedByPoseidonCrown) {
                event.setNewAboutToBeSetTarget(null);
            }
        }

    }

    @SubscribeEvent
    public static void onMobEffectApplicable(final MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player
                && PoseidonCrownItem.isEquipped(player)
                && OlympusConfig.INSTANCE.poseidonCrownMiningFatigueImmunityEnabled.get()
                && event.getEffectInstance().is(MobEffects.MINING_FATIGUE)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    private static boolean isSeaMob(final Mob mob) {
        final MobCategory category = mob.getType().getCategory();
        return mob instanceof WaterAnimal
                || mob instanceof Drowned
                || mob instanceof Guardian
                || category == MobCategory.WATER_CREATURE
                || category == MobCategory.WATER_AMBIENT
                || category == MobCategory.UNDERGROUND_WATER_CREATURE
                || category == MobCategory.AXOLOTLS;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(final LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Spawns a soul if the cup is equipped
        if (event.getEntity() instanceof Mob defeatedMob) {
            PersephoneCupItem.spawnSoulOnKill(defeatedMob, player);
        }

        // Charges the spear of ares active ability
        SpearOfAresItem.chargeSpecialAbilityForKill(player, event.getSource());
        // Reduces the instruments of hephaestus repair cooldown
        InstrumentsOfHephaestusItem.reduceCooldownOnKill(player);
    }

    @SubscribeEvent
    public static void onLivingFall(final LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && HelmetOfHadesItem.isEquipped(player)
                && OlympusConfig.INSTANCE.helmetOfHadesFallImmunityEnabled.get()
                && player.level().dimension() == Level.NETHER) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player && AresHelmetItem.hasLaunchFallProtection(player)) {
            event.setCanceled(true);
            AresHelmetItem.clearLaunchFallProtection(player);
            return;
        }

        // If the spear of ares active ability is active, cancels the damage
        if (event.getEntity() instanceof ServerPlayer player && SpearOfAresItem.handleSpecialLanding(player, event.getDistance())) {
            event.setCanceled(true);
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player
                && PoseidonCrownItem.isEquipped(player)
                && OlympusConfig.INSTANCE.poseidonCrownFallImmunityEnabled.get()
                && event.getDistance() > 3.0F) {
            event.setCanceled(true);
            final ServerLevel level = (ServerLevel) player.level();
            final double x = player.getX();
            final double y = player.getY() + 0.08D;
            final double z = player.getZ();
            level.sendParticles(OlympusParticles.TRIDENT_SPLASH_OF_WATER.get(), x, y, z, 0, 2.2D, 0, 0, 1);
            level.sendParticles(OlympusParticles.TRIDENT_WATER_DROP.get(), x, y + 0.15D, z, 24, 0.32D, 0.2D, 0.32D, 0.28D);
            level.sendParticles(ParticleTypes.BUBBLE, x, y, z, 12, 0.35D, 0.08D, 0.35D, 0.12D);
            level.playSound(null, x, y, z, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.8F, 1.0F);
        }

    }

    @SubscribeEvent
    public static void onPlayerTick(final PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AmuletOfHadesItem.restoreCooldownIfNeeded(player);
            HermesSandalsItem.rechargeExtraJumps(player);
            SpearOfAresItem.updateSpecialFall(player);
            AresHelmetItem.updateLaunchFallProtection(player);
            PoseidonCrownItem.updateWaterBoost(player);
            AresHelmetItem.updateLaunchCharge(player);
            final boolean aresHelmetEquipped = AresHelmetItem.isEquipped(player);
            final boolean crownEquipped = PoseidonCrownItem.isEquipped(player);
            final boolean crownInWater = crownEquipped && player.isInWater();
            final boolean hadesHelmetInNether = HelmetOfHadesItem.isEquipped(player) && player.level().dimension() == Level.NETHER;
            final boolean hadesHelmetInLava = HelmetOfHadesItem.isEquipped(player) && player.isInLava();
            if (crownEquipped) {
                if (OlympusConfig.INSTANCE.poseidonCrownWaterBreathingEnabled.get()) maintainCrownEffect(player, MobEffects.WATER_BREATHING);
                else player.removeEffect(MobEffects.WATER_BREATHING);
                if (OlympusConfig.INSTANCE.poseidonCrownDolphinsGraceEnabled.get()) maintainCrownEffect(player, MobEffects.DOLPHINS_GRACE);
                else player.removeEffect(MobEffects.DOLPHINS_GRACE);
                if (OlympusConfig.INSTANCE.poseidonCrownMiningFatigueImmunityEnabled.get()) player.removeEffect(MobEffects.MINING_FATIGUE);
            }

            final var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealth != null) {
                setConditionalHealthBonus(maxHealth, new AttributeModifier(Olympus.of("poseidon_crown_water_health"), OlympusConfig.INSTANCE.poseidonCrownBonusHealth.get(), AttributeModifier.Operation.ADD_VALUE), crownInWater);
                setConditionalHealthBonus(maxHealth, new AttributeModifier(Olympus.of("hades_helmet_nether_health"), OlympusConfig.INSTANCE.helmetOfHadesBonusHealth.get(), AttributeModifier.Operation.ADD_VALUE), hadesHelmetInNether);
                setConditionalHealthBonus(player.getAttribute(Attributes.MOVEMENT_SPEED), new AttributeModifier(Olympus.of("hades_helmet_lava_speed"), OlympusConfig.INSTANCE.helmetOfHadesLavaMovementSpeedBonus.get(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE), hadesHelmetInLava);
            }

            if (crownInWater || hadesHelmetInNether) {
                if (crownInWater && OlympusConfig.INSTANCE.poseidonCrownRegenerationEnabled.get() || hadesHelmetInNether && OlympusConfig.INSTANCE.helmetOfHadesRegenerationEnabled.get()) {
                    maintainCrownEffect(player, MobEffects.REGENERATION, crownInWater ? OlympusConfig.INSTANCE.poseidonCrownRegenerationLevel.get() - 1 : OlympusConfig.INSTANCE.helmetOfHadesRegenerationLevel.get() - 1);
                } else player.removeEffect(MobEffects.REGENERATION);
                if (crownInWater && OlympusConfig.INSTANCE.poseidonCrownStrengthEnabled.get() || hadesHelmetInNether && OlympusConfig.INSTANCE.helmetOfHadesStrengthEnabled.get()) {
                    maintainCrownEffect(player, MobEffects.STRENGTH, crownInWater ? OlympusConfig.INSTANCE.poseidonCrownStrengthLevel.get() - 1 : OlympusConfig.INSTANCE.helmetOfHadesStrengthLevel.get() - 1);
                } else player.removeEffect(MobEffects.STRENGTH);
            }
            else if (aresHelmetEquipped) {
                if (OlympusConfig.INSTANCE.aresHelmetRegenerationEnabled.get()) maintainAresEffect(player, MobEffects.REGENERATION, OlympusConfig.INSTANCE.aresHelmetRegenerationLevel.get() - 1);
                else player.removeEffect(MobEffects.REGENERATION);
                if (OlympusConfig.INSTANCE.aresHelmetStrengthEnabled.get()) maintainAresEffect(player, MobEffects.STRENGTH, OlympusConfig.INSTANCE.aresHelmetStrengthLevel.get() - 1);
                else player.removeEffect(MobEffects.STRENGTH);
            }
            else {
                player.removeEffect(MobEffects.REGENERATION);
                player.removeEffect(MobEffects.STRENGTH);
            }

            if (aresHelmetEquipped) {
                if (OlympusConfig.INSTANCE.aresHelmetSpeedEnabled.get()) maintainAresEffect(player, MobEffects.SPEED, OlympusConfig.INSTANCE.aresHelmetSpeedLevel.get() - 1);
                else player.removeEffect(MobEffects.SPEED);
                if (OlympusConfig.INSTANCE.aresHelmetJumpBoostEnabled.get()) maintainAresEffect(player, MobEffects.JUMP_BOOST, OlympusConfig.INSTANCE.aresHelmetJumpBoostLevel.get() - 1);
                else player.removeEffect(MobEffects.JUMP_BOOST);
            }

            if (crownInWater && OlympusConfig.INSTANCE.poseidonCrownSaturationEnabled.get()) {
                maintainCrownEffect(player, MobEffects.SATURATION, 0);
            }
            else {
                player.removeEffect(MobEffects.SATURATION);
            }
        }

    }

    @SubscribeEvent
    public static void onPlayerTickPre(final PlayerTickEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && AresHelmetItem.hasLaunchFallProtection(player)) {
            player.fallDistance = 0;
        }
    }

    private static void maintainCrownEffect(final ServerPlayer player, final Holder<MobEffect> effect) {
        maintainCrownEffect(player, effect, 0);
    }

    private static void setConditionalHealthBonus(final net.minecraft.world.entity.ai.attributes.AttributeInstance attribute, final AttributeModifier modifier, final boolean enabled) {
        if (enabled) {
            if (!attribute.hasModifier(modifier.id())) {
                attribute.addTransientModifier(modifier);
            }
        }
        else {
            attribute.removeModifier(modifier.id());
        }
    }

    private static void maintainCrownEffect(final ServerPlayer player, final Holder<MobEffect> effect, final int amplifier) {
        final MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getDuration() <= 20 || current.getAmplifier() != amplifier) {
            player.addEffect(new MobEffectInstance(effect, 60, amplifier, true, false, true));
        }
    }

    private static void maintainAresEffect(final ServerPlayer player, final Holder<MobEffect> effect, final int amplifier) {
        final MobEffectInstance current = player.getEffect(effect);
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() <= 20) {
            player.addEffect(new MobEffectInstance(effect, 60, amplifier, true, false, true));
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePost(final LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // On entity hit, if the player has the invisibility of hades affect, the duration is halved
        if (event.getEntity() != player && !event.getSource().is(OlympusDamageTypes.LIGHTNING) && event.getHealthDamage() > 0) {
            InvisibilityOfHadesEffect.shortenAfterAttack(player);
        }

        // Bracers of zeus lightning bolt
        if (event.getEntity() instanceof Mob target) {
            BracersOfZeusItem.tryActivateAbility(target, player, event.getSource(), event.getHealthDamage());
        }

    }

    @SubscribeEvent
    public static void onEnchantedEntityLoot(final EnchantedEntityLootEvent event) {
        if (event.getDamageSource() != null && event.getDamageSource().getDirectEntity() instanceof ArtemisArrow artemisArrow && artemisArrow.olympus$isArtemisArrow() && event.getEnchantment().is(Enchantments.LOOTING)) {
            event.setEnchantmentLevel(event.getEnchantmentLevel() + OlympusConfig.INSTANCE.artemisBowLootingBonus.get());
        }

    }

}
