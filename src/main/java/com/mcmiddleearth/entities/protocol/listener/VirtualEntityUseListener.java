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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
        if (entity instanceof VirtualEntity) {
            VirtualEntity virtualEntity = (VirtualEntity) entity;
            EnumWrappers.EntityUseAction action = packet.getEntityUseActions().read(0);

            if (entity.getGoal() instanceof GoalMimic && ((GoalMimic) entity.getGoal()).getMimic().equals(player)) {
                switch (action) {
                    case INTERACT:
                    case INTERACT_AT:
                        entity.playAnimation(ActionType.INTERACT);
                        break;
                    case ATTACK:
                        entity.playAnimation(ActionType.ATTACK);
                        break;
                }
                return;
            }
            event.setCancelled(true);

            EquipmentSlot hand;
            if(!action.equals(EnumWrappers.EntityUseAction.ATTACK)
                    && packet.getHands().read(0).equals(EnumWrappers.Hand.MAIN_HAND)) {
                hand = EquipmentSlot.HAND;
            } else {
                hand = EquipmentSlot.OFF_HAND;
            }

            boolean isSneaking = packet.getBooleans().read(0);

            switch (action) {
                case INTERACT_AT:
                    Vector vector = packet.getVectors().read(0);

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
                        Location location = virtualEntity.getLocation();
                        Bukkit.getScheduler().runTask(EntitiesPlugin.getInstance(), () ->
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound " + virtualEntity.getTriggeredSound() + " player " + player.getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ()));
                    }

                    if (virtualEntity.getSubtitleLayout() != null) {
                        virtualEntity.saySubtitles(player.getBukkitPlayer());
                    }
                    break;
                case INTERACT:
                    throwEvent(new VirtualPlayerInteractEvent(player, virtualEntity, hand, isSneaking));

                    System.out.println("Head Pitch: " + virtualEntity.getHeadPitch());
                    System.out.println("Head Yaw: " + virtualEntity.getHeadYaw());
                    System.out.println("Roll: " + virtualEntity.getRoll());

                    if (virtualEntity.getTriggeredSound() != null) {
                        Location location = virtualEntity.getLocation();
                        Bukkit.getScheduler().runTask(EntitiesPlugin.getInstance(), () ->
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "playsound " + virtualEntity.getTriggeredSound() + " player " + player.getName() + " " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ()));
                    }

                    if (virtualEntity.getSubtitleLayout() != null) {
                        virtualEntity.saySubtitles(player.getBukkitPlayer());
                    }
                    break;
                case ATTACK:
                    VirtualPlayerAttackEvent entityEvent = new VirtualPlayerAttackEvent(player, virtualEntity, isSneaking);
                    throwEvent(entityEvent);
                    if (!entityEvent.isCancelled()) {
                        player.attack(entity);
                    }
                    break;
            }
        }
    }
}
