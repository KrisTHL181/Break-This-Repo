package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;

public class RideCommand {
   private static final DynamicCommandExceptionType ERROR_NOT_RIDING = new DynamicCommandExceptionType(
      entity -> Component.translatableEscape("commands.ride.not_riding", entity)
   );
   private static final Dynamic2CommandExceptionType ERROR_ALREADY_RIDING = new Dynamic2CommandExceptionType(
      (entity, vehicle) -> Component.translatableEscape("commands.ride.already_riding", entity, vehicle)
   );
   private static final Dynamic2CommandExceptionType ERROR_MOUNT_FAILED = new Dynamic2CommandExceptionType(
      (entity, vehicle) -> Component.translatableEscape("commands.ride.mount.failure.generic", entity, vehicle)
   );
   private static final SimpleCommandExceptionType ERROR_MOUNTING_PLAYER = new SimpleCommandExceptionType(
      Component.translatable("commands.ride.mount.failure.cant_ride_players")
   );
   private static final SimpleCommandExceptionType ERROR_MOUNTING_LOOP = new SimpleCommandExceptionType(
      Component.translatable("commands.ride.mount.failure.loop")
   );
   private static final SimpleCommandExceptionType ERROR_WRONG_DIMENSION = new SimpleCommandExceptionType(
      Component.translatable("commands.ride.mount.failure.wrong_dimension")
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("ride").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("target", EntityArgument.entity())
                     .then(
                        Commands.literal("mount")
                           .then(
                              Commands.argument("vehicle", EntityArgument.entity())
                                 .executes(
                                    c -> mount((CommandSourceStack)c.getSource(), EntityArgument.getEntity(c, "target"), EntityArgument.getEntity(c, "vehicle"))
                                 )
                           )
                     ))
                  .then(Commands.literal("dismount").executes(c -> dismount((CommandSourceStack)c.getSource(), EntityArgument.getEntity(c, "target"))))
            )
      );
   }

   private static int mount(final CommandSourceStack source, final Entity target, final Entity vehicle) throws CommandSyntaxException {
      Entity currentVehicle = target.getVehicle();
      if (currentVehicle != null) {
         throw ERROR_ALREADY_RIDING.create(target.getDisplayName(), currentVehicle.getDisplayName());
      }

      if (vehicle.is(EntityTypes.PLAYER)) {
         throw ERROR_MOUNTING_PLAYER.create();
      }

      if (target.getSelfAndPassengers().anyMatch(e -> e == vehicle)) {
         throw ERROR_MOUNTING_LOOP.create();
      }

      if (target.level() != vehicle.level()) {
         throw ERROR_WRONG_DIMENSION.create();
      }

      if (!target.startRiding(vehicle, true, true)) {
         throw ERROR_MOUNT_FAILED.create(target.getDisplayName(), vehicle.getDisplayName());
      }

      source.sendSuccess(() -> Component.translatable("commands.ride.mount.success", target.getDisplayName(), vehicle.getDisplayName()), true);
      return 1;
   }

   private static int dismount(final CommandSourceStack source, final Entity target) throws CommandSyntaxException {
      Entity vehicle = target.getVehicle();
      if (vehicle == null) {
         throw ERROR_NOT_RIDING.create(target.getDisplayName());
      }

      target.stopRiding();
      source.sendSuccess(() -> Component.translatable("commands.ride.dismount.success", target.getDisplayName(), vehicle.getDisplayName()), true);
      return 1;
   }
}
