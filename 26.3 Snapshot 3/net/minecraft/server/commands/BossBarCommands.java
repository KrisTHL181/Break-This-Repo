package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.ResolutionContext;
import net.minecraft.resources.Identifier;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;

public class BossBarCommands {
   private static final DynamicCommandExceptionType ERROR_ALREADY_EXISTS = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("commands.bossbar.create.failed", id)
   );
   private static final DynamicCommandExceptionType ERROR_DOESNT_EXIST = new DynamicCommandExceptionType(
      id -> Component.translatableEscape("commands.bossbar.unknown", id)
   );
   private static final SimpleCommandExceptionType ERROR_NO_PLAYER_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.players.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_NO_NAME_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.name.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_NO_COLOR_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.color.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_NO_STYLE_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.style.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_NO_VALUE_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.value.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_NO_MAX_CHANGE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.max.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_ALREADY_HIDDEN = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.visibility.unchanged.hidden")
   );
   private static final SimpleCommandExceptionType ERROR_ALREADY_VISIBLE = new SimpleCommandExceptionType(
      Component.translatable("commands.bossbar.set.visibility.unchanged.visible")
   );
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_BOSS_BAR = (c, b) -> SharedSuggestionProvider.suggestResource(
      ((CommandSourceStack)c.getSource()).getServer().getCustomBossEvents().getIds(), b
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                              "bossbar"
                           )
                           .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                        .then(
                           Commands.literal("add")
                              .then(
                                 Commands.argument("id", IdentifierArgument.id())
                                    .then(
                                       Commands.argument("name", ComponentArgument.textComponent(context))
                                          .executes(
                                             c -> createBar(
                                                (CommandSourceStack)c.getSource(),
                                                IdentifierArgument.getId(c, "id"),
                                                ComponentArgument.getResolvedComponent(c, "name")
                                             )
                                          )
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("remove")
                           .then(
                              Commands.argument("id", IdentifierArgument.id())
                                 .suggests(SUGGEST_BOSS_BAR)
                                 .executes(c -> removeBar((CommandSourceStack)c.getSource(), getBossBar(c)))
                           )
                     ))
                  .then(Commands.literal("list").executes(c -> listBars((CommandSourceStack)c.getSource()))))
               .then(
                  Commands.literal("set")
                     .then(
                        ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                "id", IdentifierArgument.id()
                                             )
                                             .suggests(SUGGEST_BOSS_BAR)
                                             .then(
                                                Commands.literal("name")
                                                   .then(
                                                      Commands.argument("name", ComponentArgument.textComponent(context))
                                                         .executes(
                                                            c -> setName(
                                                               (CommandSourceStack)c.getSource(),
                                                               getBossBar(c),
                                                               ComponentArgument.getResolvedComponent(c, "name")
                                                            )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                                     "color"
                                                                  )
                                                                  .then(
                                                                     Commands.literal("pink")
                                                                        .executes(
                                                                           c -> setColor(
                                                                              (CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarColor.PINK
                                                                           )
                                                                        )
                                                                  ))
                                                               .then(
                                                                  Commands.literal("blue")
                                                                     .executes(
                                                                        c -> setColor(
                                                                           (CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarColor.BLUE
                                                                        )
                                                                     )
                                                               ))
                                                            .then(
                                                               Commands.literal("red")
                                                                  .executes(
                                                                     c -> setColor((CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarColor.RED)
                                                                  )
                                                            ))
                                                         .then(
                                                            Commands.literal("green")
                                                               .executes(
                                                                  c -> setColor((CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarColor.GREEN)
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("yellow")
                                                            .executes(
                                                               c -> setColor((CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarColor.YELLOW)
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.literal("purple")
                                                         .executes(
                                                            c -> setColor((CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarColor.PURPLE)
                                                         )
                                                   ))
                                                .then(
                                                   Commands.literal("white")
                                                      .executes(c -> setColor((CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarColor.WHITE))
                                                )
                                          ))
                                       .then(
                                          ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                                            "style"
                                                         )
                                                         .then(
                                                            Commands.literal("progress")
                                                               .executes(
                                                                  c -> setStyle(
                                                                     (CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarOverlay.PROGRESS
                                                                  )
                                                               )
                                                         ))
                                                      .then(
                                                         Commands.literal("notched_6")
                                                            .executes(
                                                               c -> setStyle(
                                                                  (CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarOverlay.NOTCHED_6
                                                               )
                                                            )
                                                      ))
                                                   .then(
                                                      Commands.literal("notched_10")
                                                         .executes(
                                                            c -> setStyle((CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarOverlay.NOTCHED_10)
                                                         )
                                                   ))
                                                .then(
                                                   Commands.literal("notched_12")
                                                      .executes(
                                                         c -> setStyle((CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarOverlay.NOTCHED_12)
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("notched_20")
                                                   .executes(
                                                      c -> setStyle((CommandSourceStack)c.getSource(), getBossBar(c), BossEvent.BossBarOverlay.NOTCHED_20)
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("value")
                                          .then(
                                             Commands.argument("value", IntegerArgumentType.integer(0))
                                                .executes(
                                                   c -> setValue((CommandSourceStack)c.getSource(), getBossBar(c), IntegerArgumentType.getInteger(c, "value"))
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("max")
                                       .then(
                                          Commands.argument("max", IntegerArgumentType.integer(1))
                                             .executes(c -> setMax((CommandSourceStack)c.getSource(), getBossBar(c), IntegerArgumentType.getInteger(c, "max")))
                                       )
                                 ))
                              .then(
                                 Commands.literal("visible")
                                    .then(
                                       Commands.argument("visible", BoolArgumentType.bool())
                                          .executes(c -> setVisible((CommandSourceStack)c.getSource(), getBossBar(c), BoolArgumentType.getBool(c, "visible")))
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("players")
                                    .executes(c -> setPlayers((CommandSourceStack)c.getSource(), getBossBar(c), Collections.emptyList())))
                                 .then(
                                    Commands.argument("targets", EntityArgument.players())
                                       .executes(
                                          c -> setPlayers((CommandSourceStack)c.getSource(), getBossBar(c), EntityArgument.getOptionalPlayers(c, "targets"))
                                       )
                                 )
                           )
                     )
               ))
            .then(
               Commands.literal("get")
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("id", IdentifierArgument.id())
                                 .suggests(SUGGEST_BOSS_BAR)
                                 .then(Commands.literal("value").executes(c -> getValue((CommandSourceStack)c.getSource(), getBossBar(c)))))
                              .then(Commands.literal("max").executes(c -> getMax((CommandSourceStack)c.getSource(), getBossBar(c)))))
                           .then(Commands.literal("visible").executes(c -> getVisible((CommandSourceStack)c.getSource(), getBossBar(c)))))
                        .then(Commands.literal("players").executes(c -> getPlayers((CommandSourceStack)c.getSource(), getBossBar(c))))
                  )
            )
      );
   }

   private static int getValue(final CommandSourceStack source, final CustomBossEvent bossBar) {
      source.sendSuccess(() -> Component.translatable("commands.bossbar.get.value", bossBar.getDisplayName(), bossBar.value()), true);
      return bossBar.value();
   }

   private static int getMax(final CommandSourceStack source, final CustomBossEvent bossBar) {
      source.sendSuccess(() -> Component.translatable("commands.bossbar.get.max", bossBar.getDisplayName(), bossBar.max()), true);
      return bossBar.max();
   }

   private static int getVisible(final CommandSourceStack source, final CustomBossEvent bossBar) {
      if (bossBar.isVisible()) {
         source.sendSuccess(() -> Component.translatable("commands.bossbar.get.visible.visible", bossBar.getDisplayName()), true);
         return 1;
      } else {
         source.sendSuccess(() -> Component.translatable("commands.bossbar.get.visible.hidden", bossBar.getDisplayName()), true);
         return 0;
      }
   }

   private static int getPlayers(final CommandSourceStack source, final CustomBossEvent bossBar) {
      if (bossBar.getPlayers().isEmpty()) {
         source.sendSuccess(() -> Component.translatable("commands.bossbar.get.players.none", bossBar.getDisplayName()), true);
      } else {
         source.sendSuccess(
            () -> Component.translatable(
               "commands.bossbar.get.players.some",
               bossBar.getDisplayName(),
               bossBar.getPlayers().size(),
               ComponentUtils.formatList(bossBar.getPlayers(), Player::getDisplayName)
            ),
            true
         );
      }

      return bossBar.getPlayers().size();
   }

   private static int setVisible(final CommandSourceStack source, final CustomBossEvent bossBar, final boolean visible) throws CommandSyntaxException {
      if (bossBar.isVisible() == visible) {
         if (visible) {
            throw ERROR_ALREADY_VISIBLE.create();
         } else {
            throw ERROR_ALREADY_HIDDEN.create();
         }
      } else {
         bossBar.setVisible(visible);
         if (visible) {
            source.sendSuccess(() -> Component.translatable("commands.bossbar.set.visible.success.visible", bossBar.getDisplayName()), true);
         } else {
            source.sendSuccess(() -> Component.translatable("commands.bossbar.set.visible.success.hidden", bossBar.getDisplayName()), true);
         }

         return 0;
      }
   }

   private static int setValue(final CommandSourceStack source, final CustomBossEvent bossBar, final int value) throws CommandSyntaxException {
      if (bossBar.value() == value) {
         throw ERROR_NO_VALUE_CHANGE.create();
      }

      bossBar.setValue(value);
      source.sendSuccess(() -> Component.translatable("commands.bossbar.set.value.success", bossBar.getDisplayName(), value), true);
      return value;
   }

   private static int setMax(final CommandSourceStack source, final CustomBossEvent bossBar, final int value) throws CommandSyntaxException {
      if (bossBar.max() == value) {
         throw ERROR_NO_MAX_CHANGE.create();
      }

      bossBar.setMax(value);
      source.sendSuccess(() -> Component.translatable("commands.bossbar.set.max.success", bossBar.getDisplayName(), value), true);
      return value;
   }

   private static int setColor(final CommandSourceStack source, final CustomBossEvent bossBar, final BossEvent.BossBarColor color) throws CommandSyntaxException {
      if (bossBar.getColor().equals(color)) {
         throw ERROR_NO_COLOR_CHANGE.create();
      }

      bossBar.setColor(color);
      source.sendSuccess(() -> Component.translatable("commands.bossbar.set.color.success", bossBar.getDisplayName()), true);
      return 0;
   }

   private static int setStyle(final CommandSourceStack source, final CustomBossEvent bossBar, final BossEvent.BossBarOverlay style) throws CommandSyntaxException {
      if (bossBar.getOverlay().equals(style)) {
         throw ERROR_NO_STYLE_CHANGE.create();
      }

      bossBar.setOverlay(style);
      source.sendSuccess(() -> Component.translatable("commands.bossbar.set.style.success", bossBar.getDisplayName()), true);
      return 0;
   }

   private static int setName(final CommandSourceStack source, final CustomBossEvent bossBar, final Component name) throws CommandSyntaxException {
      Component replaced = ComponentUtils.resolve(ResolutionContext.builder().withSource(source).withEntityOverride(null).build(), name);
      if (bossBar.getName().equals(replaced)) {
         throw ERROR_NO_NAME_CHANGE.create();
      }

      bossBar.setName(replaced);
      source.sendSuccess(() -> Component.translatable("commands.bossbar.set.name.success", bossBar.getDisplayName()), true);
      return 0;
   }

   private static int setPlayers(final CommandSourceStack source, final CustomBossEvent bossBar, final Collection<ServerPlayer> targets) throws CommandSyntaxException {
      boolean changed = bossBar.setPlayers(targets);
      if (!changed) {
         throw ERROR_NO_PLAYER_CHANGE.create();
      }

      if (bossBar.getPlayers().isEmpty()) {
         source.sendSuccess(() -> Component.translatable("commands.bossbar.set.players.success.none", bossBar.getDisplayName()), true);
      } else {
         source.sendSuccess(
            () -> Component.translatable(
               "commands.bossbar.set.players.success.some",
               bossBar.getDisplayName(),
               targets.size(),
               ComponentUtils.formatList(targets, Player::getDisplayName)
            ),
            true
         );
      }

      return bossBar.getPlayers().size();
   }

   private static int listBars(final CommandSourceStack source) {
      Collection<CustomBossEvent> events = source.getServer().getCustomBossEvents().getEvents();
      if (events.isEmpty()) {
         source.sendSuccess(() -> Component.translatable("commands.bossbar.list.bars.none"), false);
      } else {
         source.sendSuccess(
            () -> Component.translatable("commands.bossbar.list.bars.some", events.size(), ComponentUtils.formatList(events, CustomBossEvent::getDisplayName)),
            false
         );
      }

      return events.size();
   }

   private static int createBar(final CommandSourceStack source, final Identifier id, final Component name) throws CommandSyntaxException {
      CustomBossEvents events = source.getServer().getCustomBossEvents();
      if (events.get(id) != null) {
         throw ERROR_ALREADY_EXISTS.create(id.toString());
      }

      CustomBossEvent event = events.create(
         source.getLevel().getRandom(), id, ComponentUtils.resolve(ResolutionContext.builder().withSource(source).withEntityOverride(null).build(), name)
      );
      source.sendSuccess(() -> Component.translatable("commands.bossbar.create.success", event.getDisplayName()), true);
      return events.getEvents().size();
   }

   private static int removeBar(final CommandSourceStack source, final CustomBossEvent bossBar) {
      CustomBossEvents events = source.getServer().getCustomBossEvents();
      bossBar.removeAllPlayers();
      events.remove(bossBar);
      source.sendSuccess(() -> Component.translatable("commands.bossbar.remove.success", bossBar.getDisplayName()), true);
      return events.getEvents().size();
   }

   public static CustomBossEvent getBossBar(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      Identifier id = IdentifierArgument.getId(context, "id");
      CustomBossEvent event = ((CommandSourceStack)context.getSource()).getServer().getCustomBossEvents().get(id);
      if (event == null) {
         throw ERROR_DOESNT_EXIST.create(id.toString());
      } else {
         return event;
      }
   }
}
