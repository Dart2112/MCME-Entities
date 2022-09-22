package com.mcmiddleearth.entities.command;

import com.mcmiddleearth.command.McmeCommandSender;
import com.mcmiddleearth.command.builder.HelpfulLiteralBuilder;
import com.mcmiddleearth.command.builder.HelpfulRequiredArgumentBuilder;
import com.mcmiddleearth.entities.Permission;
import com.mcmiddleearth.entities.ai.goal.GoalType;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.command.argument.AttributeTypeArgument;
import com.mcmiddleearth.entities.command.argument.GoalTypeArgument;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.RealPlayer;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.entities.composite.bones.SpeechBalloonLayout;
import com.mcmiddleearth.entities.entities.simple.SimpleHorse;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class SetCommand extends McmeEntitiesCommandHandler {

    public SetCommand(String command) {
        super(command);
    }

    @Override
    protected HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder commandNodeBuilder) {
        commandNodeBuilder
                .requires(sender -> (sender instanceof RealPlayer)
                        && ((RealPlayer) sender).getBukkitPlayer().hasPermission(Permission.USER.getNode())
                )
                .then(HelpfulLiteralBuilder.literal("goal")
                        .then(HelpfulRequiredArgumentBuilder.argument("type", new GoalTypeArgument())
                                .executes(context -> setGoal(context.getSource(), context.getArgument("type", String.class), false))
                                .then(HelpfulLiteralBuilder.literal("loop")
                                        .executes(context -> setGoal(context.getSource(), context.getArgument("type", String.class), true))
                                )
                        )
                )
                .then(HelpfulLiteralBuilder.literal("displayname")
                        .then(HelpfulRequiredArgumentBuilder.argument("displayname", word())
                                .executes(context -> setDisplayName(context.getSource(), context.getArgument("displayname", String.class)))
                        )
                )
                .then(HelpfulLiteralBuilder.literal("item")
                        .then(HelpfulRequiredArgumentBuilder.argument("slot", word())
                                .then(HelpfulRequiredArgumentBuilder.argument("item", word())
                                        .executes(context -> setItem(context.getSource(), context.getArgument("slot", String.class),
                                                context.getArgument("item", String.class))
                                        )
                                )
                        )
                )
                .then(HelpfulLiteralBuilder.literal("attribute")
                        .then(HelpfulRequiredArgumentBuilder.argument("type", new AttributeTypeArgument())
                                .then(HelpfulRequiredArgumentBuilder.argument("value", word())
                                        .executes(context -> setAttribute(context.getSource(), context.getArgument("type", String.class),
                                                context.getArgument("value", String.class)))
                                )
                        )
                )
                .then(HelpfulLiteralBuilder.literal("viewingdistance")
                        .then(HelpfulRequiredArgumentBuilder.argument("distance", IntegerArgumentType.integer(1, Integer.MAX_VALUE))
                                .executes(
                                        context -> setViewDistance(context.getSource(), IntegerArgumentType.getInteger(context, "distance"))
                                )
                        )
                )
                .then(HelpfulLiteralBuilder.literal("sound")
                        .then(HelpfulRequiredArgumentBuilder.argument("sound", word())
                                .executes(
                                        context -> setTriggerSound(context.getSource(), context.getArgument("sound", String.class))
                                )
                        )
                )
                .then(HelpfulLiteralBuilder.literal("subtitle")
                        .then(HelpfulRequiredArgumentBuilder.argument("side", word())
                                .then(HelpfulRequiredArgumentBuilder.argument("duration", integer())
                                        .then(HelpfulRequiredArgumentBuilder.argument("text", greedyString())
                                                .executes(context ->
                                                        setSubtitle(
                                                                context.getSource(),
                                                                context.getArgument("side", String.class),
                                                                context.getArgument("text", String.class),
                                                                context.getArgument("duration", Integer.class)
                                                        )
                                                )
                                        )
                                )
                        )
                );
        return commandNodeBuilder;
    }

    public int setItem(McmeCommandSender sender, String slotName, String itemMaterial) {
        RealPlayer player = (RealPlayer) sender;
        Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            sender.sendMessage(new ComponentBuilder("You need to select at least one entity to set an item").color(ChatColor.RED).create());
            return 0;
        }

        if (slotName.equalsIgnoreCase("saddle")) {
            Set<SimpleHorse> horses = selectedEntities.stream()
                    .filter(mcMeEntity -> mcMeEntity instanceof SimpleHorse)
                    .map(mcMeEntity -> ((SimpleHorse) mcMeEntity))
                    .collect(Collectors.toSet());
            if (horses.isEmpty()) {
                sender.sendMessage(new ComponentBuilder("Not implemented for custom entities.").color(ChatColor.RED).create());
                return 0;
            }

            final AtomicInteger saddledEntities = new AtomicInteger(0);
            final AtomicInteger unSaddledEntities = new AtomicInteger(0);
            for (SimpleHorse horse : horses) {
                if (itemMaterial.equalsIgnoreCase("saddle")) {
                    horse.setSaddled(true);

                    saddledEntities.incrementAndGet();
                } else {
                    horse.setSaddled(false);
                    unSaddledEntities.incrementAndGet();
                }
            }

            if (saddledEntities.get() > 0) {
                sender.sendMessage(new ComponentBuilder(Integer.toString(saddledEntities.get())).color(ChatColor.YELLOW)
                        .append(new ComponentBuilder(" entities saddled").color(ChatColor.GREEN).create()).create());
            }

            if (unSaddledEntities.get() > 0) {
                sender.sendMessage(new ComponentBuilder(Integer.toString(unSaddledEntities.get())).color(ChatColor.YELLOW)
                        .append(new ComponentBuilder(" entities unsaddled").color(ChatColor.GREEN).create()).create());
            }

        } else {
            EquipmentSlot slot = EquipmentSlot.HAND;
            try {
                slot = EquipmentSlot.valueOf(slotName.toUpperCase());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(new ComponentBuilder("Can't parse equipment slot. Using main hand.").color(ChatColor.RED).create());
            }
            Material material = Material.LEATHER_CHESTPLATE;
            try {
                material = Material.valueOf(itemMaterial.toUpperCase());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(new ComponentBuilder("Can't parse item material. Using leather chest plate.").color(ChatColor.RED).create());
            }
            ItemStack item = new ItemStack(material);

            EquipmentSlot finalSlot = slot;
            selectedEntities.forEach(mcmeEntity -> mcmeEntity.setEquipment(finalSlot, item));
            sender.sendMessage(new ComponentBuilder(slot.name().toLowerCase() + " item set to " + material.name().toLowerCase() + ".").color(ChatColor.GREEN).create());
        }
        return 0;
    }

    private int setAttribute(McmeCommandSender sender, String type, String valueString) {
        try {
            double value = Double.parseDouble(valueString);
            RealPlayer player = (RealPlayer) sender;
            Set<McmeEntity> selectedEntities = player.getSelectedEntities();

            if (selectedEntities.isEmpty()) {
                sender.sendMessage(new ComponentBuilder("You need to select at least one entity to set new attributes").color(ChatColor.RED).create());
                return 0;
            }
            Attribute attributeType = Attribute.valueOf(type.toUpperCase());

            final AtomicBoolean sentAttributeNotFoundMessage = new AtomicBoolean(false);
            final AtomicBoolean sentAttributeSetMessage = new AtomicBoolean(false);

            selectedEntities.stream()
                    .filter(mcMeEntity -> mcMeEntity instanceof VirtualEntity)
                    .map(mcMeEntity -> ((VirtualEntity) mcMeEntity))
                    .forEach(entity -> {
                        AttributeInstance attribute = entity.getAttribute(attributeType);
                        if (attribute == null) {
                            entity.registerAttribute(attributeType);
                            attribute = entity.getAttribute(attributeType);
                        }

                        if (attribute == null) {
                            if (sentAttributeNotFoundMessage.get()) return;

                            sender.sendMessage(new ComponentBuilder("Attribute not found!").color(ChatColor.RED).create());
                            sentAttributeNotFoundMessage.set(true);
                            return;
                        }

                        attribute.setBaseValue(value);

                        if (sentAttributeSetMessage.get()) return;

                        sender.sendMessage(new ComponentBuilder("Attribute '" + type + "' set to " + value).create());
                        sentAttributeSetMessage.set(true);
                    });

        } catch (NumberFormatException ex) {
            sender.sendMessage(new ComponentBuilder("Attribute value must be  a decimal number!").color(ChatColor.RED).create());
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(new ComponentBuilder("Invalid attribute type!").color(ChatColor.RED).create());
        }
        return 0;
    }

    private int setDisplayName(McmeCommandSender source, String displayName) {
        RealPlayer player = (RealPlayer) source;
        Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            source.sendMessage(new ComponentBuilder("You need to select at least one entity to set a new display name").color(ChatColor.RED).create());
            return 0;
        }

        selectedEntities.stream()
                .filter(mcMeEntity -> mcMeEntity instanceof VirtualEntity)
                .map(mcMeEntity -> ((VirtualEntity) mcMeEntity))
                .forEach(entity -> entity.setDisplayName(displayName));

        source.sendMessage(new ComponentBuilder("Set display name to: ").color(ChatColor.YELLOW).append(new ComponentBuilder(displayName).create()).create());
        return 0;
    }


    private int setGoal(McmeCommandSender sender, String type, boolean loop) {
        try {
            RealPlayer player = (RealPlayer) sender;
            Set<McmeEntity> selectedEntities = player.getSelectedEntities();
            if (selectedEntities.isEmpty()) {
                sender.sendMessage(new ComponentBuilder("You need to select at least one entity to apply the goal to.").color(ChatColor.RED).create());
                return 0;
            }
            GoalType goalType = GoalType.valueOf(type.toUpperCase());
            VirtualEntityGoalFactory factory = new VirtualEntityGoalFactory(goalType)
                    .withLoop(loop)
                    .withTargetEntity(player.getSelectedTargetEntity())
                    .withTargetLocation(player.getSelectedPoints().stream().findFirst().orElse(null))
                    .withCheckpoints(player.getSelectedPoints().toArray(new Location[0]));

            final AtomicBoolean sentInvalidLocationException = new AtomicBoolean(false);
            final AtomicBoolean sentInvalidDataException = new AtomicBoolean(false);
            selectedEntities.stream().filter(mcmeEntity -> mcmeEntity instanceof VirtualEntity).map(mcmeEntity -> ((VirtualEntity) mcmeEntity)).forEach(entity -> {
                try {
                    entity.setGoal(factory.build(entity));
                } catch (InvalidLocationException e) {
                    if (sentInvalidLocationException.get()) return;
                    sender.sendMessage(new ComponentBuilder("Invalid location. All location must be same world!").color(ChatColor.RED).create());

                    sentInvalidLocationException.set(true);
                } catch (InvalidDataException e) {
                    if (sentInvalidDataException.get()) return;
                    sender.sendMessage(new ComponentBuilder(e.getMessage()).color(ChatColor.RED).create());

                    sentInvalidDataException.set(true);
                }
            });

            sender.sendMessage(new ComponentBuilder("Goal has been updated").color(ChatColor.YELLOW).create());

        } catch (IllegalArgumentException ex) {
            sender.sendMessage(new ComponentBuilder("Invalid goal type!").color(ChatColor.RED).create());
        }
        return 0;
    }

    private int setViewDistance(McmeCommandSender source, int distance) {
        RealPlayer player = (RealPlayer) source;
        Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            source.sendMessage(new ComponentBuilder("You need to select at least one entity to set a new view distance").color(ChatColor.RED).create());
            return 0;
        }

        selectedEntities.stream()
                .filter(mcMeEntity -> mcMeEntity instanceof VirtualEntity)
                .map(mcMeEntity -> ((VirtualEntity) mcMeEntity))
                .forEach(entity -> entity.setViewDistance(distance));

        source.sendMessage(new ComponentBuilder("Set view distance to: " + distance).color(ChatColor.YELLOW).create());
        return 0;
    }

    private int setTriggerSound(McmeCommandSender source, String sound) {
        RealPlayer player = (RealPlayer) source;
        Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            source.sendMessage(new ComponentBuilder("You need to select at least one entity to set the trigger sound").color(ChatColor.RED).create());
            return 0;
        }

        selectedEntities.stream()
                .filter(mcMeEntity -> mcMeEntity instanceof VirtualEntity)
                .map(mcMeEntity -> ((VirtualEntity) mcMeEntity))
                .forEach(entity -> entity.setTriggeredSound(sound));

        source.sendMessage(new ComponentBuilder("Set triggered sound to: " + sound).color(ChatColor.YELLOW).create());
        return 0;
    }

    private int setSubtitle(McmeCommandSender source, String side, String text, int duration) {
        RealPlayer player = (RealPlayer) source;
        Set<McmeEntity> selectedEntities = player.getSelectedEntities();

        if (selectedEntities.isEmpty()) {
            source.sendMessage(new ComponentBuilder("You need to select at least one entity to set the subtitle").color(ChatColor.RED).create());
            return 0;
        }

        SpeechBalloonLayout.Position position = (side.equals("l") ? SpeechBalloonLayout.Position.LEFT :
                (side.equals("t") ? SpeechBalloonLayout.Position.TOP : SpeechBalloonLayout.Position.RIGHT));
        SpeechBalloonLayout layout = new SpeechBalloonLayout(position, SpeechBalloonLayout.Width.OPTIMAL)
                .withDuration(duration)
                .withMessage(text);

        selectedEntities.stream()
                .filter(mcMeEntity -> mcMeEntity instanceof VirtualEntity)
                .map(mcMeEntity -> ((VirtualEntity) mcMeEntity))
                .forEach(entity -> entity.setSubtitleLayout(layout));

        source.sendMessage(new ComponentBuilder("Set subtitle to: " + text).color(ChatColor.YELLOW).create());
        return 0;
    }

}
