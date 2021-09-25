package com.mcmiddleearth.entities.protocol.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.entity.Player;

import java.util.Optional;

public class DisplayNamePacket extends AbstractPacket {

    private final PacketContainer meta;

    public DisplayNamePacket(int entityId) {
        meta = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        meta.getIntegers().write(0, entityId);
    }

    public void setName(String name) {
        WrappedDataWatcher watcher = new WrappedDataWatcher();
        if(name != null) {
            WrappedDataWatcher.WrappedDataWatcherObject nameValue = new WrappedDataWatcher
                    .WrappedDataWatcherObject(2,
                    WrappedDataWatcher.Registry.getChatComponentSerializer(true));
            watcher.setObject(nameValue, Optional.of(WrappedChatComponent
                    .fromChatMessage(name/*.replace('&','§')*/)[0].getHandle()));
        }
        WrappedDataWatcher.WrappedDataWatcherObject nameVisible = new WrappedDataWatcher
                .WrappedDataWatcherObject(3,
                WrappedDataWatcher.Registry.get(Boolean.class));
        watcher.setObject(nameVisible, name!=null);
//Logger.getGlobal().info("Set Display name packet name to: "+name);
        meta.getWatchableCollectionModifier().write(0,watcher.getWatchableObjects());
    }

    @Override
    public void send(Player recipient) {
//Logger.getGlobal().info("Sending Display Name Packet.");
        send(meta, recipient);
    }
}
