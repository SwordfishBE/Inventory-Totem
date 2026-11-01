package net.inventory_totem;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public class InventoryTotemGameTests {
    private ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        // Vanilla effect synchronization needs a listener even for a headless test player.
        p.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(),
                new Connection(PacketFlow.SERVERBOUND), p,
                CommonListenerCookie.createInitial(p.getGameProfile(), false)) {
            @Override
            public void send(Packet<?> packet) {}
            @Override
            public boolean hasClientLoaded() { return true; }
        };
        return p;
    }

    private boolean allow(ServerPlayer player) {
        return ServerLivingEntityEvents.ALLOW_DEATH.invoker()
                .allowDeath(player, player.damageSources().generic(), 1000);
    }

    @GameTest
    public void bypassDamageDoesNotActivateOrConsumeTotem(GameTestHelper h) {
        for (boolean voidDamage : new boolean[] {false, true}) {
            ServerPlayer p = player(h);
            p.getInventory().setItem(15, new ItemStack(Items.TOTEM_OF_UNDYING));
            p.addEffect(new MobEffectInstance(MobEffects.POISON, 500));
            p.setHealth(0);
            var source = voidDamage ? p.damageSources().fellOutOfWorld() : p.damageSources().genericKill();
            h.assertTrue(ServerLivingEntityEvents.ALLOW_DEATH.invoker().allowDeath(p, source, 1000),
                    "Vanilla bypass damage must allow death");
            h.assertTrue(p.getHealth() == 0, "Bypass damage must not restore health");
            h.assertTrue(p.getInventory().getItem(15).getCount() == 1, "Bypass damage must not consume a totem");
            h.assertTrue(p.hasEffect(MobEffects.POISON), "Bypass damage must not clear effects");
            h.assertFalse(p.hasEffect(MobEffects.REGENERATION), "Bypass damage must not grant totem effects");
        }
        h.succeed();
    }

    @GameTest
    public void killCommandRemainsFatal(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.getInventory().setItem(15, new ItemStack(Items.TOTEM_OF_UNDYING));
        // KillCommand invokes this same vanilla method on each target.
        p.kill(h.getLevel());
        h.assertFalse(p.isAlive(), "/kill must remain fatal with an inventory totem");
        h.assertFalse(p.hasEffect(MobEffects.REGENERATION), "/kill must not activate totem effects");
        h.succeed();
    }

    @GameTest
    public void voidDamageRemainsFatal(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.getInventory().setItem(15, new ItemStack(Items.TOTEM_OF_UNDYING));
        p.hurtServer(h.getLevel(), p.damageSources().fellOutOfWorld(), 1000);
        h.assertFalse(p.isAlive(), "Falling out of the world must remain fatal with an inventory totem");
        h.assertFalse(p.hasEffect(MobEffects.REGENERATION), "Void damage must not activate totem effects");
        h.succeed();
    }

    @GameTest
    public void lethalDamageInvokesProtection(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.getInventory().setItem(15, new ItemStack(Items.TOTEM_OF_UNDYING));
        p.setHealth(20);
        p.hurtServer(h.getLevel(), p.damageSources().generic(), 1000);
        h.assertTrue(p.isAlive() && p.getHealth() == 1, "Actual lethal damage must rescue the player");
        h.assertTrue(p.getInventory().getItem(15).isEmpty(), "Lethal damage must consume the inventory totem");
        h.succeed();
    }

    @GameTest
    public void inventoryRescueAndEffects(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.getInventory().setItem(20, new ItemStack(Items.TOTEM_OF_UNDYING));
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 500));
        p.setHealth(0);
        h.assertFalse(allow(p), "Inventory totem must cancel death through the registered event");
        h.assertTrue(p.getHealth() == 1, "Rescue must restore one health point");
        h.assertTrue(p.getInventory().getItem(20).isEmpty(), "Exactly one totem must be consumed");
        h.assertFalse(p.hasEffect(MobEffects.POISON), "Existing effects must be removed");
        h.assertTrue(p.getEffect(MobEffects.REGENERATION).getDuration() == 900, "Regeneration duration");
        h.assertTrue(p.getEffect(MobEffects.REGENERATION).getAmplifier() == 1, "Regeneration II");
        h.assertTrue(p.getEffect(MobEffects.ABSORPTION).getDuration() == 100, "Absorption duration");
        h.assertTrue(p.getEffect(MobEffects.ABSORPTION).getAmplifier() == 1, "Absorption II");
        h.assertTrue(p.getEffect(MobEffects.FIRE_RESISTANCE).getDuration() == 800, "Fire resistance duration");
        h.succeed();
    }

    @GameTest
    public void allStorageSlotsAndOneAtATime(GameTestHelper h) {
        for (int slot = 0; slot < 36; slot++) {
            ServerPlayer p = player(h);
            // Keep the selected hand free, including when testing hotbar slot zero.
            p.getInventory().setSelectedSlot(slot == 0 ? 1 : 0);
            p.getInventory().setItem(slot, new ItemStack(Items.TOTEM_OF_UNDYING, 2));
            p.setHealth(0);
            h.assertFalse(allow(p), "Inventory slot " + slot + " must protect");
            h.assertTrue(p.getInventory().getItem(slot).getCount() == 1, "Consume one in slot " + slot);
        }
        h.succeed();
    }

    @GameTest
    public void heldTotemsAreLeftToVanilla(GameTestHelper h) {
        for (InteractionHand hand : InteractionHand.values()) {
            ServerPlayer p = player(h);
            p.setItemInHand(hand, new ItemStack(Items.TOTEM_OF_UNDYING));
            p.getInventory().setItem(12, new ItemStack(Items.TOTEM_OF_UNDYING));
            h.assertTrue(allow(p), "Held totems must remain controlled by vanilla");
            h.assertTrue(p.getItemInHand(hand).getCount() == 1, "Hand stack must remain untouched");
            h.assertTrue(p.getInventory().getItem(12).getCount() == 1, "Spare must remain untouched");
        }
        h.succeed();
    }

    @GameTest
    public void missingTotemAndEnderChestDoNotProtect(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.getInventory().setItem(12, new ItemStack(Items.EMERALD));
        p.getEnderChestInventory().setItem(0, new ItemStack(Items.TOTEM_OF_UNDYING));
        h.assertTrue(allow(p), "Ender chest and unrelated items must not protect");
        h.assertTrue(p.getInventory().getItem(12).is(Items.EMERALD), "Other items must remain");
        h.succeed();
    }

    @GameTest
    public void mobsDoNotUseInventoryProtection(GameTestHelper h) {
        var mob = h.spawn(EntityTypes.ZOMBIE, 1, 2, 1);
        h.assertTrue(InventoryProtection.allowDeath(mob, mob.damageSources().generic()), "Only server players receive inventory protection");
        h.succeed();
    }

    @GameTest
    public void firstTotemOnlyAndRepeatedRescue(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.getInventory().setItem(9, new ItemStack(Items.TOTEM_OF_UNDYING));
        p.getInventory().setItem(35, new ItemStack(Items.TOTEM_OF_UNDYING));
        p.setHealth(0);
        h.assertFalse(allow(p), "First rescue");
        h.assertTrue(p.getInventory().getItem(9).isEmpty(), "First slot consumed");
        h.assertTrue(p.getInventory().getItem(35).getCount() == 1, "Second totem retained");
        p.setHealth(0);
        h.assertFalse(allow(p), "Second rescue");
        h.assertTrue(p.getInventory().getItem(35).isEmpty(), "Second totem consumed");
        p.setHealth(0);
        h.assertTrue(allow(p), "Death allowed after inventory is exhausted");
        h.succeed();
    }
}
