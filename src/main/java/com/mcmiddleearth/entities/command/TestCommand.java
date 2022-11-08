package com.mcmiddleearth.entities.command;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mcmiddleearth.command.McmeCommandSender;
import com.mcmiddleearth.command.builder.HelpfulLiteralBuilder;
import com.mcmiddleearth.command.builder.HelpfulRequiredArgumentBuilder;
import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.Permission;
import com.mcmiddleearth.entities.ai.goal.GoalPath;
import com.mcmiddleearth.entities.ai.goal.GoalType;
import com.mcmiddleearth.entities.ai.goal.GoalVirtualEntity;
import com.mcmiddleearth.entities.ai.pathfinding.Path;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.entities.RealPlayer;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.util.BoundingBox;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class TestCommand extends McmeEntitiesCommandHandler {

    public TestCommand(String command) {
        super(command);
    }

    @Override
    protected HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder commandNodeBuilder) {
        commandNodeBuilder
            .requires(sender -> (sender instanceof RealPlayer)
                                && ((RealPlayer) sender).getBukkitPlayer().hasPermission(Permission.USER.getNode()))
            .then(HelpfulLiteralBuilder.literal("path")
                .executes(context -> findPath(context.getSource()))
            )
            .then(HelpfulLiteralBuilder.literal("convert")
                .then(HelpfulRequiredArgumentBuilder.argument("first", StringArgumentType.word())
                    .then(
                        HelpfulRequiredArgumentBuilder.argument("second", StringArgumentType.word())
                            .then(
                                HelpfulRequiredArgumentBuilder.argument("zone", StringArgumentType.word())
                                    .executes(commandContext -> {
                                        String first = commandContext.getArgument("first", String.class);
                                        String[] firstSplit = first.split("-");
                                        Vector firstVector = new Vector(
                                            Integer.parseInt(firstSplit[0]),
                                            Integer.parseInt(firstSplit[1]),
                                            Integer.parseInt(firstSplit[2])
                                        );

                                        String second = commandContext.getArgument("second", String.class);
                                        String[] secondSplit = second.split("-");

                                        Vector secondVector = new Vector(
                                            Integer.parseInt(secondSplit[0]),
                                            Integer.parseInt(secondSplit[1]),
                                            Integer.parseInt(secondSplit[2])
                                        );

                                        List<String> zones = new ArrayList<>();
                                        zones.add("new_npcs");
                                        zones.add("new_npcs_2");
                                        zones.add("new_npcs_3");
                                        zones.add("custom_npcs");

                                        BoundingBox boundingBox = BoundingBox.of(firstVector, secondVector);
                                        boundingBox.normalize();
                                        Map<String, List<VirtualEntityFactory>> savedFiles = new HashMap<>();

                                        for (String zone : zones) {
                                            final File file = new File(EntitiesPlugin.getEntitiesFolder(), zone + ".json");
                                            final Gson gson = EntitiesPlugin.getEntitiesGsonBuilder().create();
                                            System.out.println("loading " + zone);
                                            try (final JsonReader reader = gson.newJsonReader(new FileReader(file))) {
                                                reader.beginArray();
                                                while (reader.hasNext()) {
                                                    final VirtualEntityFactory factory = gson.fromJson(reader, VirtualEntityFactory.class);
                                                    if (!boundingBox.contains(factory.getLocation())) {
                                                        continue;
                                                    }

                                                    String type;
                                                    if (factory.getDataFile().contains("female_of_color")) {
                                                        type = "female_of_color";
                                                    } else if (factory.getDataFile().contains("female")) {
                                                        type = "female";
                                                    } else if (factory.getDataFile().contains("male_of_color")) {
                                                        type = "male_of_color";
                                                    } else if (factory.getDataFile().contains("male")) {
                                                        type = "male";
                                                    } else if (factory.getDataFile().contains("union")) {
                                                        type = "union";
                                                    } else if (factory.getDataFile().contains("confederate")) {
                                                        type = "confederate";
                                                    } else if (factory.getDataFile().contains("custom")) {
                                                        type = "custom";
                                                    } else {
                                                        type = "unknown";
                                                    }

                                                    List<VirtualEntityFactory> factories = savedFiles.getOrDefault(type, new ArrayList<>());
                                                    factories.add(factory);

                                                    if (factories.contains(factory)) {
                                                        System.out.println("FOUND DUPLICATE IN FLE " + type);
                                                    }

                                                    savedFiles.put(type, factories);
                                                }
                                                reader.endArray();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }

                                        String zone = commandContext.getArgument("zone", String.class);

                                        Gson gson = EntitiesPlugin.getEntitiesGsonBuilder().create();
                                        AtomicInteger counter = new AtomicInteger();

                                        savedFiles.forEach((s, virtualEntityFactories) -> {
                                            final File file = new File(EntitiesPlugin.getEntitiesFolder(), zone + "_" + s + ".json");
                                            try (JsonWriter writer = gson.newJsonWriter(new FileWriter(file))) {
                                                writer.beginArray();
                                                for (VirtualEntityFactory virtualEntityFactory : virtualEntityFactories) {
                                                    gson.toJson(virtualEntityFactory, VirtualEntityFactory.class, writer);
                                                    counter.getAndIncrement();
                                                }
                                                writer.endArray();
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                        });

                                        commandContext.getSource()
                                            .sendMessage(new ComponentBuilder("Saved " + counter.get() + " entities to " + zone).create());

                                        return 1;
                                    })
                            )
                    )
                )
            )
            .then(HelpfulLiteralBuilder.literal("fix")
                .executes(commandContext -> {
                    List<String> filesToFix = new ArrayList<>();
                    for (int i = 1; i < 5; i++) {
                        filesToFix.add("general_" + i + "_female");
                        filesToFix.add("general_" + i + "_female_of_color");
                        filesToFix.add("general_" + i + "_male");
                        filesToFix.add("general_" + i + "_male_of_color");
                        filesToFix.add("general_" + i + "_confederate");
                        filesToFix.add("general_" + i + "_union");
                        filesToFix.add("general_" + i + "_custom");
                        filesToFix.add("general_" + i + "_unknown");
                    }

                    for (int i = 1; i < 38; i++) {
                        filesToFix.add("town_" + i + "_female");
                        filesToFix.add("town_" + i + "_female_of_color");
                        filesToFix.add("town_" + i + "_male");
                        filesToFix.add("town_" + i + "_male_of_color");
                        filesToFix.add("town_" + i + "_confederate");
                        filesToFix.add("town_" + i + "_union");
                        filesToFix.add("town_" + i + "_custom");
                        filesToFix.add("town_" + i + "_unknown");
                    }

                    for (int i = 1; i < 14; i++) {
                        filesToFix.add("trench" + i + "_female");
                        filesToFix.add("trench" + i + "_female_of_color");
                        filesToFix.add("trench" + i + "_male");
                        filesToFix.add("trench" + i + "_male_of_color");
                        filesToFix.add("trench" + i + "_confederate");
                        filesToFix.add("trench" + i + "_union");
                        filesToFix.add("trench" + i + "_custom");
                        filesToFix.add("town_" + i + "_unknown");
                    }

                    Map<File, List<VirtualEntityFactory>> savedFiles = new HashMap<>();
                    Set<Location> locations = new HashSet<>();

                    final Gson gson = EntitiesPlugin.getEntitiesGsonBuilder().create();
                    int duplicates = 0;
                    for (String zone : filesToFix) {
                        final File file = new File(EntitiesPlugin.getEntitiesFolder(), zone + ".json");
                        if (!file.exists()) {
                            System.out.println(zone + " does not exist");
                            continue;
                        }

                        List<VirtualEntityFactory> factories = new ArrayList<>();

                        try (final JsonReader reader = gson.newJsonReader(new FileReader(file))) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                final VirtualEntityFactory factory = gson.fromJson(reader, VirtualEntityFactory.class);
                                if (locations.contains(factory.getLocation())) {
                                    duplicates++;
                                    continue;
                                }

                                factories.add(factory);
                                locations.add(factory.getLocation());
                            }
                            reader.endArray();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        savedFiles.put(file, factories);
                    }

                    savedFiles.forEach((file, virtualEntityFactories) -> {
                        try (JsonWriter writer = gson.newJsonWriter(new FileWriter(file))) {
                            writer.beginArray();
                            for (VirtualEntityFactory virtualEntityFactory : virtualEntityFactories) {
                                gson.toJson(virtualEntityFactory, VirtualEntityFactory.class, writer);
                            }
                            writer.endArray();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    commandContext.getSource()
                        .sendMessage(new ComponentBuilder("Fixed " + duplicates + " entities").create());

                    return 1;
                })
            )
            .then(HelpfulLiteralBuilder.literal("count")
                .executes(commandContext -> {
                    List<String> filesToFix = new ArrayList<>();
                    for (int i = 1; i < 5; i++) {
                        filesToFix.add("general_" + i + "_female");
                        filesToFix.add("general_" + i + "_female_of_color");
                        filesToFix.add("general_" + i + "_male");
                        filesToFix.add("general_" + i + "_male_of_color");
                        filesToFix.add("general_" + i + "_confederate");
                        filesToFix.add("general_" + i + "_union");
                        filesToFix.add("general_" + i + "_custom");
                        filesToFix.add("general_" + i + "_unknown");
                    }

                    for (int i = 1; i < 38; i++) {
                        filesToFix.add("town_" + i + "_female");
                        filesToFix.add("town_" + i + "_female_of_color");
                        filesToFix.add("town_" + i + "_male");
                        filesToFix.add("town_" + i + "_male_of_color");
                        filesToFix.add("town_" + i + "_confederate");
                        filesToFix.add("town_" + i + "_union");
                        filesToFix.add("town_" + i + "_custom");
                        filesToFix.add("town_" + i + "_unknown");
                    }

                    for (int i = 1; i < 14; i++) {
                        filesToFix.add("trench" + i + "_female");
                        filesToFix.add("trench" + i + "_female_of_color");
                        filesToFix.add("trench" + i + "_male");
                        filesToFix.add("trench" + i + "_male_of_color");
                        filesToFix.add("trench" + i + "_confederate");
                        filesToFix.add("trench" + i + "_union");
                        filesToFix.add("trench" + i + "_custom");
                        filesToFix.add("town_" + i + "_unknown");
                    }

                    Bukkit.getScheduler().runTaskAsynchronously(EntitiesPlugin.getInstance(), () -> {
                        final Gson gson = EntitiesPlugin.getEntitiesGsonBuilder().create();
                        int entities = 0;
                        for (String zone : filesToFix) {
                            final File file = new File(EntitiesPlugin.getEntitiesFolder(), zone + ".json");
                            if (!file.exists()) {
                                continue;
                            }

                            try (final JsonReader reader = gson.newJsonReader(new FileReader(file))) {
                                reader.beginArray();
                                while (reader.hasNext()) {
                                    gson.fromJson(reader, VirtualEntityFactory.class);
                                    entities++;
                                }
                                reader.endArray();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        int finalEntities = entities;
                        Bukkit.getScheduler().runTask(EntitiesPlugin.getInstance(), () -> {
                            commandContext.getSource()
                                .sendMessage(new ComponentBuilder("Counted " + finalEntities + " entities").create());
                        });
                    });


                    return 1;
                })
            )
            .then(HelpfulLiteralBuilder.literal("old")
                .executes(commandContext -> {
                    List<String> filesToFix = new ArrayList<>();
                    filesToFix.add("new_npcs");
                    filesToFix.add("new_npcs_2");
                    filesToFix.add("new_npcs_3");
                    filesToFix.add("custom_npcs");

                    Bukkit.getScheduler().runTaskAsynchronously(EntitiesPlugin.getInstance(), () -> {
                        final Gson gson = EntitiesPlugin.getEntitiesGsonBuilder().create();
                        int entities = 0;
                        for (String zone : filesToFix) {
                            final File file = new File(EntitiesPlugin.getEntitiesFolder(), zone + ".json");
                            if (!file.exists()) {
                                continue;
                            }

                            try (final JsonReader reader = gson.newJsonReader(new FileReader(file))) {
                                reader.beginArray();
                                while (reader.hasNext()) {
                                    gson.fromJson(reader, VirtualEntityFactory.class);
                                    entities++;
                                }
                                reader.endArray();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        int finalEntities = entities;
                        Bukkit.getScheduler().runTask(EntitiesPlugin.getInstance(), () -> {
                            commandContext.getSource()
                                .sendMessage(new ComponentBuilder("Counted " + finalEntities + " old entities").create());
                        });
                    });

                    return 1;
                })
            );
        return commandNodeBuilder;
    }

    private int findPath(McmeCommandSender sender) {
        RealPlayer player = ((RealPlayer) sender);
        VirtualEntity entity = (VirtualEntity) player.getSelectedEntities().iterator().next();
        try {
            GoalVirtualEntity goal = new VirtualEntityGoalFactory(GoalType.FOLLOW_ENTITY)
                .withTargetEntity(player)
                .build(entity);
            goal.update();
            Path path = ((GoalPath) goal).getPath();
            if (path != null) {
                Logger.getGlobal().info("Target: " + path.getTarget());
                Logger.getGlobal().info("Start: " + path.getStart());
                Logger.getGlobal().info("End: " + path.getEnd());
                path.getPoints().forEach(point -> {
                    Logger.getGlobal().info(point.getBlockX() + " " + point.getBlockY() + " " + point.getBlockZ());
                    player.getLocation().getWorld()
                        .dropItem(point.toLocation(player.getLocation().getWorld()), new ItemStack(Material.STONE));
                });
            } else {
                Logger.getGlobal().info("no path found");
            }
        } catch (Exception ignore) {
        }
        return 0;
    }

}
