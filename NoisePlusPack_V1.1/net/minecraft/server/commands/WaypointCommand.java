package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HexColorArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.WaypointArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.waypoints.Waypoint;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import net.minecraft.world.waypoints.WaypointStyleAssets;
import net.minecraft.world.waypoints.WaypointTransmitter;

public class WaypointCommand {
   public static void register(CommandDispatcher<CommandSourceStack> p_407181_, CommandBuildContext p_408262_) {
      p_407181_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("waypoint")
                  .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
               .then(Commands.literal("list").executes(p_409851_ -> listWaypoints((CommandSourceStack)p_409851_.getSource()))))
            .then(
               Commands.literal("modify")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("waypoint", EntityArgument.entity())
                           .then(
                              ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("color")
                                       .then(
                                          Commands.argument("color", ColorArgument.color())
                                             .executes(
                                                p_407116_ -> setWaypointColor(
                                                   (CommandSourceStack)p_407116_.getSource(),
                                                   WaypointArgument.getWaypoint(p_407116_, "waypoint"),
                                                   ColorArgument.getColor(p_407116_, "color")
                                                )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("hex")
                                          .then(
                                             Commands.argument("color", HexColorArgument.hexColor())
                                                .executes(
                                                   p_409442_ -> setWaypointColor(
                                                      (CommandSourceStack)p_409442_.getSource(),
                                                      WaypointArgument.getWaypoint(p_409442_, "waypoint"),
                                                      HexColorArgument.getHexColor(p_409442_, "color")
                                                   )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("reset")
                                       .executes(
                                          p_407074_ -> resetWaypointColor(
                                             (CommandSourceStack)p_407074_.getSource(), WaypointArgument.getWaypoint(p_407074_, "waypoint")
                                          )
                                       )
                                 )
                           ))
                        .then(
                           ((LiteralArgumentBuilder)Commands.literal("style")
                                 .then(
                                    Commands.literal("reset")
                                       .executes(
                                          p_408059_ -> setWaypointStyle(
                                             (CommandSourceStack)p_408059_.getSource(),
                                             WaypointArgument.getWaypoint(p_408059_, "waypoint"),
                                             WaypointStyleAssets.DEFAULT
                                          )
                                       )
                                 ))
                              .then(
                                 Commands.literal("set")
                                    .then(
                                       Commands.argument("style", IdentifierArgument.id())
                                          .executes(
                                             p_449056_ -> setWaypointStyle(
                                                (CommandSourceStack)p_449056_.getSource(),
                                                WaypointArgument.getWaypoint(p_449056_, "waypoint"),
                                                ResourceKey.create(WaypointStyleAssets.ROOT_ID, IdentifierArgument.getId(p_449056_, "style"))
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int setWaypointStyle(CommandSourceStack p_410572_, WaypointTransmitter p_407430_, ResourceKey<WaypointStyleAsset> p_407424_) {
      mutateIcon(p_410572_, p_407430_, p_407629_ -> p_407629_.style = p_407424_);
      p_410572_.sendSuccess(() -> Component.translatable("commands.waypoint.modify.style"), false);
      return 0;
   }

   private static int setWaypointColor(CommandSourceStack p_406634_, WaypointTransmitter p_409443_, ChatFormatting p_409295_) {
      mutateIcon(p_406634_, p_409443_, p_409923_ -> p_409923_.color = Optional.of(p_409295_.getColor()));
      p_406634_.sendSuccess(() -> Component.translatable("commands.waypoint.modify.color", Component.literal(p_409295_.getName()).withStyle(p_409295_)), false);
      return 0;
   }

   private static int setWaypointColor(CommandSourceStack p_407798_, WaypointTransmitter p_407282_, Integer p_410204_) {
      mutateIcon(p_407798_, p_407282_, p_409255_ -> p_409255_.color = Optional.of(p_410204_));
      p_407798_.sendSuccess(
         () -> Component.translatable(
            "commands.waypoint.modify.color", Component.literal(HexFormat.of().withUpperCase().toHexDigits(ARGB.color(0, p_410204_), 6)).withColor(p_410204_)
         ),
         false
      );
      return 0;
   }

   private static int resetWaypointColor(CommandSourceStack p_410056_, WaypointTransmitter p_406957_) {
      mutateIcon(p_410056_, p_406957_, p_410469_ -> p_410469_.color = Optional.empty());
      p_410056_.sendSuccess(() -> Component.translatable("commands.waypoint.modify.color.reset"), false);
      return 0;
   }

   private static int listWaypoints(CommandSourceStack p_410380_) {
      ServerLevel serverlevel = p_410380_.getLevel();
      Set<WaypointTransmitter> set = serverlevel.getWaypointManager().transmitters();
      String s = serverlevel.dimension().identifier().toString();
      if (set.isEmpty()) {
         p_410380_.sendSuccess(() -> Component.translatable("commands.waypoint.list.empty", s), false);
         return 0;
      } else {
         Component component = ComponentUtils.formatList(
            set.stream()
               .map(
                  p_449055_ -> {
                     if (p_449055_ instanceof LivingEntity livingentity) {
                        BlockPos blockpos = livingentity.blockPosition();
                        return livingentity.getFeedbackDisplayName()
                           .copy()
                           .withStyle(
                              p_406446_ -> p_406446_.withClickEvent(
                                    new ClickEvent.SuggestCommand(
                                       "/execute in " + s + " run tp @s " + blockpos.getX() + " " + blockpos.getY() + " " + blockpos.getZ()
                                    )
                                 )
                                 .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
                                 .withColor(p_449055_.waypointIcon().color.orElse(-1))
                           );
                     } else {
                        return Component.literal(p_449055_.toString());
                     }
                  }
               )
               .toList(),
            Function.identity()
         );
         p_410380_.sendSuccess(() -> Component.translatable("commands.waypoint.list.success", set.size(), s, component), false);
         return set.size();
      }
   }

   private static void mutateIcon(CommandSourceStack p_407459_, WaypointTransmitter p_409821_, Consumer<Waypoint.Icon> p_410371_) {
      ServerLevel serverlevel = p_407459_.getLevel();
      serverlevel.getWaypointManager().untrackWaypoint(p_409821_);
      p_410371_.accept(p_409821_.waypointIcon());
      serverlevel.getWaypointManager().trackWaypoint(p_409821_);
   }
}
