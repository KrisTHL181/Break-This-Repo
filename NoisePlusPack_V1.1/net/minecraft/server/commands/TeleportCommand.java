package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.RotationArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TeleportCommand {
   private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(
      Component.translatable("commands.teleport.invalidPosition")
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_139009_) {
      LiteralCommandNode<CommandSourceStack> literalcommandnode = p_139009_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("teleport")
                     .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                  .then(
                     Commands.argument("location", Vec3Argument.vec3())
                        .executes(
                           p_358635_ -> teleportToPos(
                              (CommandSourceStack)p_358635_.getSource(),
                              Collections.singleton(((CommandSourceStack)p_358635_.getSource()).getEntityOrException()),
                              ((CommandSourceStack)p_358635_.getSource()).getLevel(),
                              Vec3Argument.getCoordinates(p_358635_, "location"),
                              null,
                              null
                           )
                        )
                  ))
               .then(
                  Commands.argument("destination", EntityArgument.entity())
                     .executes(
                        p_139049_ -> teleportToEntity(
                           (CommandSourceStack)p_139049_.getSource(),
                           Collections.singleton(((CommandSourceStack)p_139049_.getSource()).getEntityOrException()),
                           EntityArgument.getEntity(p_139049_, "destination")
                        )
                     )
               ))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("targets", EntityArgument.entities())
                     .then(
                        ((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument("location", Vec3Argument.vec3())
                                 .executes(
                                    p_358636_ -> teleportToPos(
                                       (CommandSourceStack)p_358636_.getSource(),
                                       EntityArgument.getEntities(p_358636_, "targets"),
                                       ((CommandSourceStack)p_358636_.getSource()).getLevel(),
                                       Vec3Argument.getCoordinates(p_358636_, "location"),
                                       null,
                                       null
                                    )
                                 ))
                              .then(
                                 Commands.argument("rotation", RotationArgument.rotation())
                                    .executes(
                                       p_358639_ -> teleportToPos(
                                          (CommandSourceStack)p_358639_.getSource(),
                                          EntityArgument.getEntities(p_358639_, "targets"),
                                          ((CommandSourceStack)p_358639_.getSource()).getLevel(),
                                          Vec3Argument.getCoordinates(p_358639_, "location"),
                                          RotationArgument.getRotation(p_358639_, "rotation"),
                                          null
                                       )
                                    )
                              ))
                           .then(
                              ((LiteralArgumentBuilder)Commands.literal("facing")
                                    .then(
                                       Commands.literal("entity")
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("facingEntity", EntityArgument.entity())
                                                   .executes(
                                                      p_358640_ -> teleportToPos(
                                                         (CommandSourceStack)p_358640_.getSource(),
                                                         EntityArgument.getEntities(p_358640_, "targets"),
                                                         ((CommandSourceStack)p_358640_.getSource()).getLevel(),
                                                         Vec3Argument.getCoordinates(p_358640_, "location"),
                                                         null,
                                                         new LookAt.LookAtEntity(
                                                            EntityArgument.getEntity(p_358640_, "facingEntity"), EntityAnchorArgument.Anchor.FEET
                                                         )
                                                      )
                                                   ))
                                                .then(
                                                   Commands.argument("facingAnchor", EntityAnchorArgument.anchor())
                                                      .executes(
                                                         p_358638_ -> teleportToPos(
                                                            (CommandSourceStack)p_358638_.getSource(),
                                                            EntityArgument.getEntities(p_358638_, "targets"),
                                                            ((CommandSourceStack)p_358638_.getSource()).getLevel(),
                                                            Vec3Argument.getCoordinates(p_358638_, "location"),
                                                            null,
                                                            new LookAt.LookAtEntity(
                                                               EntityArgument.getEntity(p_358638_, "facingEntity"),
                                                               EntityAnchorArgument.getAnchor(p_358638_, "facingAnchor")
                                                            )
                                                         )
                                                      )
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.argument("facingLocation", Vec3Argument.vec3())
                                       .executes(
                                          p_358637_ -> teleportToPos(
                                             (CommandSourceStack)p_358637_.getSource(),
                                             EntityArgument.getEntities(p_358637_, "targets"),
                                             ((CommandSourceStack)p_358637_.getSource()).getLevel(),
                                             Vec3Argument.getCoordinates(p_358637_, "location"),
                                             null,
                                             new LookAt.LookAtPosition(Vec3Argument.getVec3(p_358637_, "facingLocation"))
                                          )
                                       )
                                 )
                           )
                     ))
                  .then(
                     Commands.argument("destination", EntityArgument.entity())
                        .executes(
                           p_139011_ -> teleportToEntity(
                              (CommandSourceStack)p_139011_.getSource(),
                              EntityArgument.getEntities(p_139011_, "targets"),
                              EntityArgument.getEntity(p_139011_, "destination")
                           )
                        )
                  )
            )
      );
      p_139009_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("tp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .redirect(literalcommandnode)
      );
   }

   private static int teleportToEntity(CommandSourceStack p_139033_, Collection<? extends Entity> p_139034_, Entity p_139035_) throws CommandSyntaxException {
      for (Entity entity : p_139034_) {
         performTeleport(
            p_139033_,
            entity,
            (ServerLevel)p_139035_.level(),
            p_139035_.getX(),
            p_139035_.getY(),
            p_139035_.getZ(),
            EnumSet.noneOf(Relative.class),
            p_139035_.getYRot(),
            p_139035_.getXRot(),
            null
         );
      }

      if (p_139034_.size() == 1) {
         p_139033_.sendSuccess(
            () -> Component.translatable("commands.teleport.success.entity.single", p_139034_.iterator().next().getDisplayName(), p_139035_.getDisplayName()),
            true
         );
      } else {
         p_139033_.sendSuccess(() -> Component.translatable("commands.teleport.success.entity.multiple", p_139034_.size(), p_139035_.getDisplayName()), true);
      }

      return p_139034_.size();
   }

   private static int teleportToPos(
      CommandSourceStack p_139026_,
      Collection<? extends Entity> p_139027_,
      ServerLevel p_139028_,
      Coordinates p_139029_,
      @Nullable Coordinates p_139030_,
      @Nullable LookAt p_362958_
   ) throws CommandSyntaxException {
      Vec3 vec3 = p_139029_.getPosition(p_139026_);
      Vec2 vec2 = p_139030_ == null ? null : p_139030_.getRotation(p_139026_);

      for (Entity entity : p_139027_) {
         Set<Relative> set = getRelatives(p_139029_, p_139030_, entity.level().dimension() == p_139028_.dimension());
         if (vec2 == null) {
            performTeleport(p_139026_, entity, p_139028_, vec3.x, vec3.y, vec3.z, set, entity.getYRot(), entity.getXRot(), p_362958_);
         } else {
            performTeleport(p_139026_, entity, p_139028_, vec3.x, vec3.y, vec3.z, set, vec2.y, vec2.x, p_362958_);
         }
      }

      if (p_139027_.size() == 1) {
         p_139026_.sendSuccess(
            () -> Component.translatable(
               "commands.teleport.success.location.single",
               p_139027_.iterator().next().getDisplayName(),
               formatDouble(vec3.x),
               formatDouble(vec3.y),
               formatDouble(vec3.z)
            ),
            true
         );
      } else {
         p_139026_.sendSuccess(
            () -> Component.translatable(
               "commands.teleport.success.location.multiple", p_139027_.size(), formatDouble(vec3.x), formatDouble(vec3.y), formatDouble(vec3.z)
            ),
            true
         );
      }

      return p_139027_.size();
   }

   private static Set<Relative> getRelatives(Coordinates p_362667_, @Nullable Coordinates p_361212_, boolean p_361629_) {
      Set<Relative> set = Relative.direction(p_362667_.isXRelative(), p_362667_.isYRelative(), p_362667_.isZRelative());
      Set<Relative> set1 = p_361629_ ? Relative.position(p_362667_.isXRelative(), p_362667_.isYRelative(), p_362667_.isZRelative()) : Set.of();
      Set<Relative> set2 = p_361212_ == null ? Relative.ROTATION : Relative.rotation(p_361212_.isYRelative(), p_361212_.isXRelative());
      return Relative.union(set, set1, set2);
   }

   private static String formatDouble(double p_142776_) {
      return String.format(Locale.ROOT, "%f", p_142776_);
   }

   private static void performTeleport(
      CommandSourceStack p_139015_,
      Entity p_139016_,
      ServerLevel p_139017_,
      double p_139018_,
      double p_139019_,
      double p_139020_,
      Set<Relative> p_139021_,
      float p_139022_,
      float p_139023_,
      @Nullable LookAt p_365991_
   ) throws CommandSyntaxException {
      BlockPos blockpos = BlockPos.containing(p_139018_, p_139019_, p_139020_);
      if (!Level.isInSpawnableBounds(blockpos)) {
         throw INVALID_POSITION.create();
      }

      double d0 = p_139021_.contains(Relative.X) ? p_139018_ - p_139016_.getX() : p_139018_;
      double d1 = p_139021_.contains(Relative.Y) ? p_139019_ - p_139016_.getY() : p_139019_;
      double d2 = p_139021_.contains(Relative.Z) ? p_139020_ - p_139016_.getZ() : p_139020_;
      float f = p_139021_.contains(Relative.Y_ROT) ? p_139022_ - p_139016_.getYRot() : p_139022_;
      float f1 = p_139021_.contains(Relative.X_ROT) ? p_139023_ - p_139016_.getXRot() : p_139023_;
      float f2 = Mth.wrapDegrees(f);
      float f3 = Mth.wrapDegrees(f1);
      if (p_139016_.teleportTo(p_139017_, d0, d1, d2, p_139021_, f2, f3, true)) {
         if (p_365991_ != null) {
            p_365991_.perform(p_139015_, p_139016_);
         }

         if (!(p_139016_ instanceof LivingEntity livingentity && livingentity.isFallFlying())) {
            p_139016_.setDeltaMovement(p_139016_.getDeltaMovement().multiply(1.0, 0.0, 1.0));
            p_139016_.setOnGround(true);
         }

         if (p_139016_ instanceof PathfinderMob pathfindermob) {
            pathfindermob.getNavigation().stop();
         }
      }
   }
}
