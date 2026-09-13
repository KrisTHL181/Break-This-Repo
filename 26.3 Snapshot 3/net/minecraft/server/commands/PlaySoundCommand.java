package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class PlaySoundCommand {
   private static final SimpleCommandExceptionType ERROR_TOO_FAR = new SimpleCommandExceptionType(Component.translatable("commands.playsound.failed"));
   private static final CommandResponseTracker.MessagesWithArg<ServerPlayer, Identifier> RESPONSE_PLAY = CommandResponseTracker.messages(
      ERROR_TOO_FAR,
      (player, var1, sound) -> Component.translatable("commands.playsound.success.single", Component.translationArg(sound), player.getDisplayName()),
      (playerCount, var1, sound) -> Component.translatable("commands.playsound.success.multiple", Component.translationArg(sound), playerCount)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      RequiredArgumentBuilder<CommandSourceStack, Identifier> name = (RequiredArgumentBuilder<CommandSourceStack, Identifier>)Commands.argument(
            "sound", IdentifierArgument.id()
         )
         .suggests(SuggestionProviders.cast(SuggestionProviders.AVAILABLE_SOUNDS))
         .executes(
            c -> playSound(
               (CommandSourceStack)c.getSource(),
               getCallingPlayerAsCollection(((CommandSourceStack)c.getSource()).getPlayer()),
               IdentifierArgument.getId(c, "sound"),
               SoundSource.MASTER,
               ((CommandSourceStack)c.getSource()).getPosition(),
               1.0F,
               1.0F,
               0.0F
            )
         );

      for (SoundSource source : SoundSource.values()) {
         name.then(source(source));
      }

      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("playsound").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(name)
      );
   }

   private static LiteralArgumentBuilder<CommandSourceStack> source(final SoundSource source) {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.literal(source.getName())
            .executes(
               c -> playSound(
                  (CommandSourceStack)c.getSource(),
                  getCallingPlayerAsCollection(((CommandSourceStack)c.getSource()).getPlayer()),
                  IdentifierArgument.getId(c, "sound"),
                  source,
                  ((CommandSourceStack)c.getSource()).getPosition(),
                  1.0F,
                  1.0F,
                  0.0F
               )
            ))
         .then(
            ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.players())
                  .executes(
                     c -> playSound(
                        (CommandSourceStack)c.getSource(),
                        EntityArgument.getPlayers(c, "targets"),
                        IdentifierArgument.getId(c, "sound"),
                        source,
                        ((CommandSourceStack)c.getSource()).getPosition(),
                        1.0F,
                        1.0F,
                        0.0F
                     )
                  ))
               .then(
                  ((RequiredArgumentBuilder)Commands.argument("pos", Vec3Argument.vec3())
                        .executes(
                           c -> playSound(
                              (CommandSourceStack)c.getSource(),
                              EntityArgument.getPlayers(c, "targets"),
                              IdentifierArgument.getId(c, "sound"),
                              source,
                              Vec3Argument.getVec3(c, "pos"),
                              1.0F,
                              1.0F,
                              0.0F
                           )
                        ))
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("volume", FloatArgumentType.floatArg(0.0F))
                              .executes(
                                 c -> playSound(
                                    (CommandSourceStack)c.getSource(),
                                    EntityArgument.getPlayers(c, "targets"),
                                    IdentifierArgument.getId(c, "sound"),
                                    source,
                                    Vec3Argument.getVec3(c, "pos"),
                                    (Float)c.getArgument("volume", Float.class),
                                    1.0F,
                                    0.0F
                                 )
                              ))
                           .then(
                              ((RequiredArgumentBuilder)Commands.argument("pitch", FloatArgumentType.floatArg(0.0F, 2.0F))
                                    .executes(
                                       c -> playSound(
                                          (CommandSourceStack)c.getSource(),
                                          EntityArgument.getPlayers(c, "targets"),
                                          IdentifierArgument.getId(c, "sound"),
                                          source,
                                          Vec3Argument.getVec3(c, "pos"),
                                          (Float)c.getArgument("volume", Float.class),
                                          (Float)c.getArgument("pitch", Float.class),
                                          0.0F
                                       )
                                    ))
                                 .then(
                                    Commands.argument("minVolume", FloatArgumentType.floatArg(0.0F, 1.0F))
                                       .executes(
                                          c -> playSound(
                                             (CommandSourceStack)c.getSource(),
                                             EntityArgument.getPlayers(c, "targets"),
                                             IdentifierArgument.getId(c, "sound"),
                                             source,
                                             Vec3Argument.getVec3(c, "pos"),
                                             (Float)c.getArgument("volume", Float.class),
                                             (Float)c.getArgument("pitch", Float.class),
                                             (Float)c.getArgument("minVolume", Float.class)
                                          )
                                       )
                                 )
                           )
                     )
               )
         );
   }

   private static Collection<ServerPlayer> getCallingPlayerAsCollection(final @Nullable ServerPlayer player) {
      return player != null ? List.of(player) : List.of();
   }

   private static int playSound(
      final CommandSourceStack source,
      final Collection<ServerPlayer> players,
      final Identifier sound,
      final SoundSource soundSource,
      final Vec3 position,
      final float volume,
      final float pitch,
      final float minVolume
   ) throws CommandSyntaxException {
      Holder<SoundEvent> soundHolder = Holder.direct(SoundEvent.createVariableRangeEvent(sound));
      double maxDistSqr = Mth.square(soundHolder.value().getRange(volume));
      ServerLevel level = source.getLevel();
      long seed = level.getRandom().nextLong();
      CommandResponseTracker<ServerPlayer> tracker = CommandResponseTracker.create();

      for (ServerPlayer player : players) {
         if (player.level() == level) {
            double deltaX = position.x - player.getX();
            double deltaY = position.y - player.getY();
            double deltaZ = position.z - player.getZ();
            double distSqr = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
            Vec3 localPosition = position;
            float localVolume = volume;
            if (distSqr > maxDistSqr) {
               if (minVolume <= 0.0F) {
                  continue;
               }

               double distance = Math.sqrt(distSqr);
               localPosition = new Vec3(
                  player.getX() + deltaX / distance * 2.0, player.getY() + deltaY / distance * 2.0, player.getZ() + deltaZ / distance * 2.0
               );
               localVolume = minVolume;
            }

            player.connection
               .send(new ClientboundSoundPacket(soundHolder, soundSource, localPosition.x(), localPosition.y(), localPosition.z(), localVolume, pitch, seed));
            tracker.track(player);
         }
      }

      return tracker.sendFeedback(source, true, RESPONSE_PLAY, sound);
   }
}
