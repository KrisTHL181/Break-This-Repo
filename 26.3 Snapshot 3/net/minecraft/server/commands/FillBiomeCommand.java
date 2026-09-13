package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.apache.commons.lang3.mutable.MutableInt;

public class FillBiomeCommand {
   public static final SimpleCommandExceptionType ERROR_NOT_LOADED = new SimpleCommandExceptionType(Component.translatable("argument.pos.unloaded"));
   private static final Dynamic2CommandExceptionType ERROR_VOLUME_TOO_LARGE = new Dynamic2CommandExceptionType(
      (max, count) -> Component.translatableEscape("commands.fillbiome.toobig", max, count)
   );
   private static final SimpleCommandExceptionType ERROR_NO_BIOMES_SET = new SimpleCommandExceptionType(Component.translatable("commands.fillbiome.no_changes"));

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher, final CommandBuildContext context) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fillbiome").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("from", BlockPosArgument.blockPos())
                  .then(
                     Commands.argument("to", BlockPosArgument.blockPos())
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("biome", ResourceArgument.resource(context, Registries.BIOME))
                                 .executes(
                                    c -> fill(
                                       (CommandSourceStack)c.getSource(),
                                       BlockPosArgument.getLoadedBlockPos(c, "from"),
                                       BlockPosArgument.getLoadedBlockPos(c, "to"),
                                       ResourceArgument.getResource(c, "biome", Registries.BIOME),
                                       b -> true
                                    )
                                 ))
                              .then(
                                 Commands.literal("replace")
                                    .then(
                                       Commands.argument("filter", ResourceOrTagArgument.resourceOrTag(context, Registries.BIOME))
                                          .executes(
                                             c -> fill(
                                                (CommandSourceStack)c.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(c, "from"),
                                                BlockPosArgument.getLoadedBlockPos(c, "to"),
                                                ResourceArgument.getResource(c, "biome", Registries.BIOME),
                                                ResourceOrTagArgument.getResourceOrTag(c, "filter", Registries.BIOME)
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int quantize(final int blockCoord) {
      return QuartPos.toBlock(QuartPos.fromBlock(blockCoord));
   }

   private static BlockPos quantize(final BlockPos block) {
      return new BlockPos(quantize(block.getX()), quantize(block.getY()), quantize(block.getZ()));
   }

   private static BiomeResolver makeResolver(
      final MutableInt count, final ChunkAccess chunk, final BoundingBox region, final Holder<Biome> toFill, final Predicate<Holder<Biome>> filter
   ) {
      return (quartX, quartY, quartZ, var8) -> {
         int blockX = QuartPos.toBlock(quartX);
         int blockY = QuartPos.toBlock(quartY);
         int blockZ = QuartPos.toBlock(quartZ);
         Holder<Biome> currentBiome = chunk.getNoiseBiome(quartX, quartY, quartZ);
         if (region.isInside(blockX, blockY, blockZ) && filter.test(currentBiome)) {
            if (!currentBiome.is(toFill)) {
               count.increment();
            }

            return toFill;
         } else {
            return currentBiome;
         }
      };
   }

   public static Either<Integer, CommandSyntaxException> fill(final ServerLevel level, final BlockPos rawFrom, final BlockPos rawTo, final Holder<Biome> biome) {
      return fill(level, rawFrom, rawTo, biome, var0 -> true, var0 -> {});
   }

   public static Either<Integer, CommandSyntaxException> fill(
      final ServerLevel level,
      final BlockPos rawFrom,
      final BlockPos rawTo,
      final Holder<Biome> biome,
      final Predicate<Holder<Biome>> filter,
      final Consumer<Supplier<Component>> successMessageConsumer
   ) {
      BlockPos from = quantize(rawFrom);
      BlockPos to = quantize(rawTo);
      BoundingBox region = BoundingBox.fromCorners(from, to);
      long volume = (long)region.getXSpan() * region.getYSpan() * region.getZSpan();
      int limit = level.getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS);
      if (volume > limit) {
         return Either.right(ERROR_VOLUME_TOO_LARGE.create(limit, volume));
      }

      List<ChunkAccess> chunks = new ArrayList<>();

      for (int chunkZ = SectionPos.blockToSectionCoord(region.minZ()); chunkZ <= SectionPos.blockToSectionCoord(region.maxZ()); chunkZ++) {
         for (int chunkX = SectionPos.blockToSectionCoord(region.minX()); chunkX <= SectionPos.blockToSectionCoord(region.maxX()); chunkX++) {
            ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
            if (chunk == null) {
               return Either.right(ERROR_NOT_LOADED.create());
            }

            chunks.add(chunk);
         }
      }

      MutableInt changedCount = new MutableInt(0);
      Iterator<ChunkAccess> iterator = chunks.iterator();

      while (iterator.hasNext()) {
         ChunkAccess chunk = iterator.next();
         int previousChangedCount = changedCount.intValue();
         chunk.fillBiomesFromNoise(makeResolver(changedCount, chunk, region, biome, filter), level.getChunkSource().randomState().sampler());
         if (previousChangedCount != changedCount.intValue()) {
            chunk.markUnsaved();
         } else {
            iterator.remove();
         }
      }

      int finalChangedCount = changedCount.intValue();
      if (finalChangedCount > 0) {
         level.getChunkSource().chunkMap.resendBiomesForChunks(chunks);
         successMessageConsumer.accept(
            () -> Component.translatable(
               "commands.fillbiome.success.count", finalChangedCount, region.minX(), region.minY(), region.minZ(), region.maxX(), region.maxY(), region.maxZ()
            )
         );
         return Either.left(finalChangedCount);
      } else {
         return Either.right(ERROR_NO_BIOMES_SET.create());
      }
   }

   private static int fill(
      final CommandSourceStack source, final BlockPos rawFrom, final BlockPos rawTo, final Holder.Reference<Biome> biome, final Predicate<Holder<Biome>> filter
   ) throws CommandSyntaxException {
      Either<Integer, CommandSyntaxException> result = fill(source.getLevel(), rawFrom, rawTo, biome, filter, m -> source.sendSuccess(m, true));
      Optional<CommandSyntaxException> exception = result.right();
      if (exception.isPresent()) {
         throw (CommandSyntaxException)exception.get();
      } else {
         return (Integer)result.left().get();
      }
   }
}
