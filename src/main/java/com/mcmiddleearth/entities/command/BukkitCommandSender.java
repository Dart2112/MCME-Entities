package com.mcmiddleearth.entities.command;

import com.mcmiddleearth.command.McmeCommandSender;
import com.mcmiddleearth.entities.api.McmeEntityType;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.entities.McmeEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

public class BukkitCommandSender implements McmeCommandSender {

    CommandSender sender;

    Set<McmeEntity> selectedEntities = new HashSet<>();

    McmeEntity selectedTargetEntity = null;

    List<Location> selectedPoints = new ArrayList<>();

    VirtualEntityFactory factory = new VirtualEntityFactory(new McmeEntityType(McmeEntityType.CustomEntityType.BAKED_ANIMATION), null);

    public BukkitCommandSender(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public void sendMessage(BaseComponent[] baseComponents) {
        this.sender.sendMessage(Component.text("[Entities] ", NamedTextColor.AQUA).append(Component.text(baseComponents[0].toLegacyText())));
    }

    @Override
    public void sendMessage(String message) {
        this.sender.sendMessage(Component.text("[Entities] ", NamedTextColor.AQUA).append(Component.text(message)));
    }

    @Override
    public void sendError(String message) {
        this.sender.sendMessage(Component.text("[Entities] ", NamedTextColor.AQUA).append(Component.text(message, NamedTextColor.RED)));
    }

    public CommandSender getCommandSender() {
        return this.sender;
    }

    public Set<McmeEntity> getSelectedEntities() {
        this.selectedEntities = this.selectedEntities.stream().filter(selectedTargetEntity -> !selectedTargetEntity.isTerminated())
            .collect(Collectors.toSet());
        return new HashSet<>(this.selectedEntities);
    }

    public void setSelectedEntities(McmeEntity entity) {
        this.clearSelectedEntities();
        this.addToSelectedEntities(entity);
    }

    public void setSelectedEntities(Set<McmeEntity> entities) {
        this.selectedEntities = entities;
    }

    public void clearSelectedEntities() {
        this.selectedEntities.clear();
    }

    public void addToSelectedEntities(McmeEntity entity) {
        this.selectedEntities.add(entity);
    }

    public void removeFromSelectedEntities(McmeEntity entity) {
        this.selectedEntities.remove(entity);
    }

    public List<Location> getSelectedPoints() {
        return this.selectedPoints;
    }

    public void setSelectedPoints(List<Location> selectedPoints) {
        this.selectedPoints = selectedPoints;
    }

    public void setSelectedPoints(Location point) {
        this.selectedPoints.clear();
        this.selectedPoints.add(point);
    }
    public void clearSelectedPoints() {
        this.selectedPoints.clear();
    }

    public void addToSelectedPoints(Location  point) {
        this.selectedPoints.add(point);
    }

    public void removeFromSelectedPoints(Location point) {
        this.selectedPoints.remove(point);
    }

    public VirtualEntityFactory getEntityFactory() {
        return this.factory;
    }

    public void setEntityFactory(VirtualEntityFactory factory) {
        this.factory = factory;
    }

    public McmeEntity getSelectedTargetEntity() {
        return this.selectedTargetEntity;
    }

    public void setSelectedTargetEntity(McmeEntity target) {
        this.selectedTargetEntity = target;
    }
}
