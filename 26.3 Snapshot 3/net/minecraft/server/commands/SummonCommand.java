package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SummonCommand {
   private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.summon.failed"));
   private static final SimpleCommandExceptionType ERROR_FAILED_PEACEFUL = new SimpleCommandExceptionType(
      Component.translatable("commands.summon.failed.peaceful")
   );
   private static final SimpleCommandExceptionType ERROR_DUPLICATE_UUID = new SimpleCommandExceptionType(Component.translatable("commands.summon.failed.uuid"));
   private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(Component.translatable("commands.summon.invalidPosition"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("summon").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               ((RequiredArgumentBuilder)Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE))
                     .suggests(SuggestionProviders.cast(SuggestionProviders.SUMMONABLE_ENTITIES))
                     .executes(
                        c -> spawnEntity(
                           (CommandSourceStack)c.getSource(),
                           ResourceArgument.getSummonableEntityType(c, "entity"),
                           ((CommandSourceStack)c.getSource()).getPosition(),
                           new CompoundTag(),
                           true
                        )
                     ))
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("pos", Vec3Argument.vec3())
                           .executes(
                              c -> spawnEntity(
                                 (CommandSourceStack)c.getSource(),
                                 ResourceArgument.getSummonableEntityType(c, "entity"),
                                 Vec3Argument.getVec3(c, "pos"),
                                 new CompoundTag(),
                                 true
                              )
                           ))
                        .then(
                           Commands.argument("nbt", CompoundTagArgument.compoundTag())
                              .executes(
                                 c -> spawnEntity(
                                    (CommandSourceStack)c.getSource(),
                                    ResourceArgument.getSummonableEntityType(c, "entity"),
                                    Vec3Argument.getVec3(c, "pos"),
                                    CompoundTagArgument.getCompoundTag(c, "nbt"),
                                    false
                                 )
                              )
                        )
                  )
            )
      );
   }

   public static Entity createEntity(
      final CommandSourceStack source, final Holder.Reference<EntityType<?>> type, final Vec3 pos, final CompoundTag nbt, final boolean finalize
   ) throws CommandSyntaxException {
      BlockPos blockPos = BlockPos.containing(pos);
      if (!Level.isInSpawnableBounds(blockPos)) {
         throw INVALID_POSITION.create();
      }

      if (source.getLevel().getDifficulty() == Difficulty.PEACEFUL && !type.value().isAllowedInPeaceful()) {
         throw ERROR_FAILED_PEACEFUL.create();
      }

      CompoundTag entityTag = nbt.copy();
      entityTag.putString("id", type.key().identifier().toString());
      ServerLevel level = source.getLevel();
      Entity entity = EntityType.loadEntityRecursive(entityTag, level, new EntitySpawnRequest(EntitySpawnReason.COMMAND, false), e -> {
         e.snapTo(pos.x, pos.y, pos.z, e.getYRot(), e.getXRot());
         return e;
      });
      if (entity == null) {
         throw ERROR_FAILED.create();
      }

      if (finalize && entity instanceof Mob mob) {
         mob.finalizeSpawn(source.getLevel(), source.getLevel().getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.COMMAND, null);
      }

      if (!level.tryAddFreshEntityWithPassengers(entity)) {
         throw ERROR_DUPLICATE_UUID.create();
      } else {
         return entity;
      }
   }

   private static int spawnEntity(
      final CommandSourceStack source, final Holder.Reference<EntityType<?>> type, final Vec3 pos, final CompoundTag nbt, final boolean finalize
   ) throws CommandSyntaxException {
      Entity entity = createEntity(source, type, pos, nbt, finalize);
      source.sendSuccess(() -> Component.translatable("commands.summon.success", entity.getDisplayName()), true);
      return 1;
   }
}
