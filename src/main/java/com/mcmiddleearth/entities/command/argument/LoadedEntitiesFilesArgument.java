package com.mcmiddleearth.entities.command.argument;

import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LoadedEntitiesFilesArgument extends HelpfulEntitiesArgumentType implements ArgumentType<String> {

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readUnquotedString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return this.addSuggestions(builder, getDataFiles());
    }

    public static List<String> getDataFiles() {
        return Arrays.stream(Objects.requireNonNull(getEntities().listFiles((dir, name) -> name.endsWith(".json")))).map(file -> file.getName().substring(0,
            file.getName().lastIndexOf('.'))).collect(
            Collectors.toList());
    }

    private static File getEntities() {
        final File file = new File(EntitiesPlugin.getInstance().getDataFolder(), "entity");
        if(!file.exists()) {
            file.mkdir();
        }
        return file;
    }



}
