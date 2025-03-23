package me.lukiiy.smashDamage;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;

public class Cmd {
    public static LiteralCommandNode<CommandSourceStack> register() {
        var main = Commands.literal("smashdamage").requires(it -> it.getSender().hasPermission("smashdamage.cmd"));

        // Reload
        main.then(Commands.literal("reload")
            .executes(it -> {
                SmashDamage.getInstance().reloadConfig();
                it.getSource().getSender().sendMessage(Component.text("SmashDamage Reload complete!").color(NamedTextColor.GREEN));
                return Command.SINGLE_SUCCESS;
            })
        );

        // Damageable fatigue
        main.then(Commands.literal("get").then(Commands.argument("entity", ArgumentTypes.entity()).executes(it -> handle(it, -1))));

        main.then(Commands.literal("set")
                .then(Commands.argument("entity", ArgumentTypes.entity())
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0, 999.9))
                                .executes(it -> handle(it, it.getArgument("value", Double.class)))))
        );

        return main.build();
    }

    private static int handle(CommandContext<CommandSourceStack> it, double upd) throws CommandSyntaxException {
        Entity entity = it.getArgument("entity", EntitySelectorArgumentResolver.class).resolve(it.getSource()).getFirst();
        if (!(entity instanceof Damageable)) throw new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(Component.text("This entity cannot contain a fatigue value").color(NamedTextColor.RED))).create();

        CommandSender sender = it.getSource().getSender();

        if (upd == -1) sender.sendMessage(entity.name().append(Component.text(" has the fatigue value of ")).append(ColorUtil.formatted(SmashDamage.getInstance().getFatigue((Damageable) entity))));
        else {
            sender.sendMessage(Component.text("Set the fatigue value of ").append(entity.name()).append(Component.text(" to ").append(ColorUtil.formatted(upd))));
            SmashDamage.getInstance().setFatigue((Damageable) entity, upd);
        }

        return Command.SINGLE_SUCCESS;
    }
}
