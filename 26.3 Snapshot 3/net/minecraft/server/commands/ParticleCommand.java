package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ParticleCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.particle.failed"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("particle").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("name", ParticleArgument.particle(context))
                     .executes(
                        c -> sendParticles(
                           (CommandSourceStack)c.getSource(),
                           ParticleArgument.getParticle(c, "name"),
                           ((CommandSourceStack)c.getSource()).getPosition(),
                           Vec3.ZERO,
                           0.0F,
                           0,
                           false,
                           ((CommandSourceStack)c.getSource()).getServer().getPlayerList().getPlayers()
                        )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("pos", Vec3Argument.vec3())
                           .executes(
                              c -> sendParticles(
                                 (CommandSourceStack)c.getSource(),
                                 ParticleArgument.getParticle(c, "name"),
                                 Vec3Argument.getVec3(c, "pos"),
                                 Vec3.ZERO,
                                 0.0F,
                                 0,
                                 false,
                                 ((CommandSourceStack)c.getSource()).getServer().getPlayerList().getPlayers()
                              )
                           ))
                        .then(
                           Commands.argument("delta", Vec3Argument.vec3(false))
                              .then(
                                 Commands.argument("speed", FloatArgumentType.floatArg(0.0F))
                                    .then(
                                       ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("count", IntegerArgumentType.integer(0))
                                                .executes(
                                                   c -> sendParticles(
                                                      (CommandSourceStack)c.getSource(),
                                                      ParticleArgument.getParticle(c, "name"),
                                                      Vec3Argument.getVec3(c, "pos"),
                                                      Vec3Argument.getVec3(c, "delta"),
                                                      FloatArgumentType.getFloat(c, "speed"),
                                                      IntegerArgumentType.getInteger(c, "count"),
                                                      false,
                                                      ((CommandSourceStack)c.getSource()).getServer().getPlayerList().getPlayers()
                                                   )
                                                ))
                                             .then(
                                                ((LiteralArgumentBuilder)Commands.literal("force")
                                                      .executes(
                                                         c -> sendParticles(
                                                            (CommandSourceStack)c.getSource(),
                                                            ParticleArgument.getParticle(c, "name"),
                                                            Vec3Argument.getVec3(c, "pos"),
                                                            Vec3Argument.getVec3(c, "delta"),
                                                            FloatArgumentType.getFloat(c, "speed"),
                                                            IntegerArgumentType.getInteger(c, "count"),
                                                            true,
                                                            ((CommandSourceStack)c.getSource()).getServer().getPlayerList().getPlayers()
                                                         )
                                                      ))
                                                   .then(
                                                      Commands.argument("viewers", EntityArgument.players())
                                                         .executes(
                                                            c -> sendParticles(
                                                               (CommandSourceStack)c.getSource(),
                                                               ParticleArgument.getParticle(c, "name"),
                                                               Vec3Argument.getVec3(c, "pos"),
                                                               Vec3Argument.getVec3(c, "delta"),
                                                               FloatArgumentType.getFloat(c, "speed"),
                                                               IntegerArgumentType.getInteger(c, "count"),
                                                               true,
                                                               EntityArgument.getPlayers(c, "viewers")
                                                            )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             ((LiteralArgumentBuilder)Commands.literal("normal")
                                                   .executes(
                                                      c -> sendParticles(
                                                         (CommandSourceStack)c.getSource(),
                                                         ParticleArgument.getParticle(c, "name"),
                                                         Vec3Argument.getVec3(c, "pos"),
                                                         Vec3Argument.getVec3(c, "delta"),
                                                         FloatArgumentType.getFloat(c, "speed"),
                                                         IntegerArgumentType.getInteger(c, "count"),
                                                         false,
                                                         ((CommandSourceStack)c.getSource()).getServer().getPlayerList().getPlayers()
                                                      )
                                                   ))
                                                .then(
                                                   Commands.argument("viewers", EntityArgument.players())
                                                      .executes(
                                                         c -> sendParticles(
                                                            (CommandSourceStack)c.getSource(),
                                                            ParticleArgument.getParticle(c, "name"),
                                                            Vec3Argument.getVec3(c, "pos"),
                                                            Vec3Argument.getVec3(c, "delta"),
                                                            FloatArgumentType.getFloat(c, "speed"),
                                                            IntegerArgumentType.getInteger(c, "count"),
                                                            false,
                                                            EntityArgument.getPlayers(c, "viewers")
                                                         )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int sendParticles(
      final CommandSourceStack source,
      final ParticleOptions particle,
      final Vec3 pos,
      final Vec3 delta,
      final float speed,
      final int count,
      final boolean force,
      final Collection<ServerPlayer> players
   ) throws CommandSyntaxException {
      int result = 0;

      for (ServerPlayer player : players) {
         if (source.getLevel().sendParticles(player, particle, force, false, pos.x, pos.y, pos.z, count, delta.x, delta.y, delta.z, speed)) {
            result++;
         }
      }

      if (result == 0) {
         throw ERROR_FAILED.create();
      }

      source.sendSuccess(() -> Component.translatable("commands.particle.success", BuiltInRegistries.PARTICLE_TYPE.getKey(particle.getType()).toString()), true);
      return result;
   }
}
