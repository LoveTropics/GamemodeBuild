package com.lovetropics.gamemodebuild.command;

import com.lovetropics.gamemodebuild.GamemodeBuild;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class GBCommandSelectors
{
    public static void init() {
        EntitySelectorOptions.register("buildmode", parser -> {
            boolean inverted = parser.shouldInvertValue();
            parser.setSuggestions((builder, consumer) -> SharedSuggestionProvider.suggest(List.of("enabled","disabled"), builder));
            String name = parser.getReader().readUnquotedString();
            boolean isEnabled = "enabled".equals(name);
            parser.addPredicate(entity -> {
                if (!(entity instanceof Player player)) return false;
                if (inverted) {
                    return isEnabled == GamemodeBuild.isActive(player);
                } else {
                    return isEnabled != GamemodeBuild.isActive(player);
                }
            });
        }, parser -> true, Component.literal("Build Mode Enabled"));
    }
}
