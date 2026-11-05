package dev.xylonity.olympus.common.item.ability;

import dev.xylonity.olympus.registry.OlympusItems;
import dev.xylonity.olympus.Olympus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = Olympus.MOD_ID)
public final class BidentMinionEvents {
    private BidentMinionEvents() { }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob deadMob)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!event.getSource().getWeaponItem().is(OlympusItems.HADES_BIDENT.get())) return;
        BidentMinionAbility.recordKill(player, deadMob);
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (BidentMinionAbility.isMinion(event.getEntity())) {
            BidentMinionAbility.onMinionRemoved(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (event.getEntity().getItem().is(OlympusItems.HADES_BIDENT.get())) {
            BidentMinionAbility.resetWhenDropped(player);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob) BidentMinionAbility.tickMinion(mob);
    }

    @SubscribeEvent
    public static void onTargetChange(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (BidentMinionAbility.isMinion(mob)
                && event.getNewAboutToBeSetTarget() != BidentMinionAbility.getOwnerTarget(mob)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }
}
