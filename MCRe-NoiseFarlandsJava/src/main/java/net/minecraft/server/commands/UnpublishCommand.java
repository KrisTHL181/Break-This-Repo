package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class UnpublishCommand {
    public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("unpublish").requires(Commands.hasPermission(Commands.LEVEL_OWNERS)).executes(c -> unpublish(c.getSource())));
    }

    private static int unpublish(final CommandSourceStack source) {
        if (source.getServer().unpublishServer()) {
            source.sendSuccess(() -> Component.translatable("commands.unpublish.success"), true);
            return 1;
        } else {
            source.sendFailure(Component.translatable("commands.unpublish.notPublished"));
            return 0;
        }
    }
}