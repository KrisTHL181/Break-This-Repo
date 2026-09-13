package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameModeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;
import org.jspecify.annotations.Nullable;

public class PublishCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.publish.failed"));
   private static final DynamicCommandExceptionType ERROR_ALREADY_PUBLISHED = new DynamicCommandExceptionType(
      p_308792_ -> Component.translatableEscape("commands.publish.alreadyPublished", p_308792_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_138185_) {
      p_138185_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("publish")
                  .requires(Commands.hasPermission(Commands.LEVEL_OWNERS)))
               .executes(p_258235_ -> publish((CommandSourceStack)p_258235_.getSource(), HttpUtil.getAvailablePort(), false, null)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("allowCommands", BoolArgumentType.bool())
                     .executes(
                        p_258236_ -> publish(
                           (CommandSourceStack)p_258236_.getSource(), HttpUtil.getAvailablePort(), BoolArgumentType.getBool(p_258236_, "allowCommands"), null
                        )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("gamemode", GameModeArgument.gameMode())
                           .executes(
                              p_258237_ -> publish(
                                 (CommandSourceStack)p_258237_.getSource(),
                                 HttpUtil.getAvailablePort(),
                                 BoolArgumentType.getBool(p_258237_, "allowCommands"),
                                 GameModeArgument.getGameMode(p_258237_, "gamemode")
                              )
                           ))
                        .then(
                           Commands.argument("port", IntegerArgumentType.integer(0, 65535))
                              .executes(
                                 p_258238_ -> publish(
                                    (CommandSourceStack)p_258238_.getSource(),
                                    IntegerArgumentType.getInteger(p_258238_, "port"),
                                    BoolArgumentType.getBool(p_258238_, "allowCommands"),
                                    GameModeArgument.getGameMode(p_258238_, "gamemode")
                                 )
                              )
                        )
                  )
            )
      );
   }

   private static int publish(CommandSourceStack p_260117_, int p_259411_, boolean p_260137_, @Nullable GameType p_259145_) throws CommandSyntaxException {
      if (p_260117_.getServer().isPublished()) {
         throw ERROR_ALREADY_PUBLISHED.create(p_260117_.getServer().getPort());
      }

      if (!p_260117_.getServer().publishServer(p_259145_, p_260137_, p_259411_)) {
         throw ERROR_FAILED.create();
      }

      p_260117_.sendSuccess(() -> getSuccessMessage(p_259411_), true);
      return p_259411_;
   }

   public static MutableComponent getSuccessMessage(int p_259532_) {
      Component component = ComponentUtils.copyOnClickText(String.valueOf(p_259532_));
      return Component.translatable("commands.publish.started", component);
   }
}
