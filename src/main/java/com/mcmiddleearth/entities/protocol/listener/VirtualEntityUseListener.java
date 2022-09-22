package com.mcmiddleearth.entities.protocol.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.ai.goal.GoalMimic;
import com.mcmiddleearth.entities.api.ActionType;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.RealPlayer;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.events.events.player.VirtualPlayerAttackEvent;
import com.mcmiddleearth.entities.events.events.player.VirtualPlayerInteractAtEvent;
import com.mcmiddleearth.entities.events.events.player.VirtualPlayerInteractEvent;
import com.mcmiddleearth.entities.server.EntityServer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class VirtualEntityUseListener extends EntityListener {

    public VirtualEntityUseListener(Plugin plugin, EntityServer entityServer) {
        super(plugin, entityServer, PacketType.Play.Client.USE_ENTITY);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        PacketContainer packet = event.getPacket();
        RealPlayer player = EntitiesPlugin.getEntityServer().getOrCreateMcmePlayer(event.getPlayer());
        int entityId = packet.getIntegers().read(0);
        McmeEntity entity = entityServer.getEntity(entityId);
        if (entity instanceof VirtualEntity virtualEntity) {
            EnumWrappers.EntityUseAction action = EnumWrappers.EntityUseAction.INTERACT;

            //1.18 fix to replace malfunctioning ProtocolLib Snapshot 4.8.0
            try {
                Field b = packet.getHandle().getClass().getDeclaredField("b");
                b.setAccessible(true);
                Object enumEntityUseAction = b.get(packet.getHandle());
                Method a = enumEntityUseAction.getClass().getMethod("a");
                a.setAccessible(true);
                Object enumAction = a.invoke(enumEntityUseAction);
                //Logger.getGlobal().info("Action: "+((Enum<?>) enumAction).name());
                switch (((Enum<?>) enumAction).name()) {
                    case "ATTACK" -> action = EnumWrappers.EntityUseAction.ATTACK;
                    case "INTERACT_AT" -> action = EnumWrappers.EntityUseAction.INTERACT_AT;
                }
            } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException |
                     InvocationTargetException e) {
                e.printStackTrace();
            }
            // end of ProtocolLib replacement
            //not working in 1.18 (not done yet?) EnumWrappers.EntityUseAction action = packet.getEntityUseActions().read(0);

            if (entity.getGoal() instanceof GoalMimic goalMimic && goalMimic.getMimic().equals(player)) {
                switch (action) {
                    case INTERACT, INTERACT_AT -> entity.playAnimation(ActionType.INTERACT);
                    case ATTACK -> entity.playAnimation(ActionType.ATTACK);
                }
                return;
            }
            event.setCancelled(true);

            EquipmentSlot hand = EquipmentSlot.HAND;

            try {
                Field b = packet.getHandle().getClass().getDeclaredField("b");
                b.setAccessible(true);
                Object enumEntityUseAction = b.get(packet.getHandle());
                Field a = enumEntityUseAction.getClass().getDeclaredField("a");
                a.setAccessible(true);
                Object enumHand = a.get(enumEntityUseAction);
                if ((((Enum<?>) enumHand).name()).equals("OFF_HAND")) {
                    hand = EquipmentSlot.OFF_HAND;
                } else {
                    Logger.getGlobal().info("Hand: " + ((Enum<?>) enumHand).name());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException ignore) {
            }

            boolean isSneaking = packet.getBooleans().read(0);

            switch (action) {
                case INTERACT_AT -> {
                    Vector vector = new Vector(0, 0, 0);

                    ///1.18 fix to replace malfunctioning ProtocolLib Snapshot 4.8.0
                    try {
                        Field b = packet.getHandle().getClass().getDeclaredField("b");
                        b.setAccessible(true);
                        Object enumEntityUseAction = b.get(packet.getHandle());
                        Field b_b = enumEntityUseAction.getClass().getDeclaredField("b");
                        b_b.setAccessible(true);
                        Object vec2D = b_b.get(enumEntityUseAction);
                        Method getter = vec2D.getClass().getMethod("a");
                        getter.setAccessible(true);
                        vector.setX((double) getter.invoke(vec2D));
                        getter = vec2D.getClass().getMethod("b");
                        getter.setAccessible(true);
                        vector.setY((double) getter.invoke(vec2D));
                        getter = vec2D.getClass().getMethod("c");
                        getter.setAccessible(true);
                        vector.setZ((double) getter.invoke(vec2D));

                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException ignore) {
                    }

                    throwEvent(new VirtualPlayerInteractAtEvent(player, virtualEntity, vector, hand, isSneaking));

                    if (player.getBukkitPlayer().getInventory().getItemInMainHand().getType().equals(Material.STICK)) {
                        if (hand.equals(EquipmentSlot.HAND)) {
                            if (isSneaking) {
                                player.addToSelectedEntities(entity);
                                player.sendMessage(new ComponentBuilder("Entity added to your selection.").create());
                            } else {
                                player.clearSelectedEntities();
                                player.addToSelectedEntities(entity);
                                player.sendMessage(new ComponentBuilder("Entity selected.").create());
                            }
                        }
                    }

                    if (virtualEntity.getTriggeredSound() != null) {
                        player.playSound(Sound.sound(Key.key(virtualEntity.getTriggeredSound()), Sound.Source.VOICE, 1f, 1f));
                    }

                    if (virtualEntity.getSubtitleLayout() != null) {
                        virtualEntity.saySubtitles(player.getBukkitPlayer());
                    }

                }
                case INTERACT -> {
                    throwEvent(new VirtualPlayerInteractEvent(player, virtualEntity, hand, isSneaking));
                    if (virtualEntity.getTriggeredSound() != null) {
                        player.playSound(Sound.sound(Key.key(virtualEntity.getTriggeredSound()), Sound.Source.VOICE, 1f, 1f));
                    }

                    if (virtualEntity.getSubtitleLayout() != null) {
                        virtualEntity.saySubtitles(player.getBukkitPlayer());
                    }
                }
                case ATTACK -> {
                    VirtualPlayerAttackEvent entityEvent = new VirtualPlayerAttackEvent(player, virtualEntity, isSneaking);
                    throwEvent(entityEvent);
                    if (!entityEvent.isCancelled()) {
                        player.attack(entity);
                    }
                }
            }
        }
    }
}
