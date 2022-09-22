package com.mcmiddleearth.entities.command.argument;

import com.mcmiddleearth.entities.command.BukkitCommandSender;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.composite.BakedAnimationEntity;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public class AnimationIdArgument extends HelpfulEntitiesArgumentType implements ArgumentType<String> {

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readUnquotedString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if(context.getSource() instanceof BukkitCommandSender) {
            McmeEntity entity = ((BukkitCommandSender)context.getSource()).getSelectedEntities().stream().findFirst().orElse(null);
            if(entity instanceof BakedAnimationEntity) {
                return addSuggestions(builder, ((BakedAnimationEntity)entity).getAnimations());
            }
        }
        return builder.buildFuture();
    }

}
