package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
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
      (p_308696_, p_308697_) -> Component.translatableEscape("commands.fillbiome.toobig", p_308696_, p_308697_)
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_261867_, CommandBuildContext p_262155_) {
      p_261867_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("fillbiome").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("from", BlockPosArgument.blockPos())
                  .then(
                     Commands.argument("to", BlockPosArgument.blockPos())
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("biome", ResourceArgument.resource(p_262155_, Registries.BIOME))
                                 .executes(
                                    p_262554_ -> fill(
                                       (CommandSourceStack)p_262554_.getSource(),
                                       BlockPosArgument.getLoadedBlockPos(p_262554_, "from"),
                                       BlockPosArgument.getLoadedBlockPos(p_262554_, "to"),
                                       ResourceArgument.getResource(p_262554_, "biome", Registries.BIOME),
                                       p_308700_ -> true
                                    )
                                 ))
                              .then(
                                 Commands.literal("replace")
                                    .then(
                                       Commands.argument("filter", ResourceOrTagArgument.resourceOrTag(p_262155_, Registries.BIOME))
                                          .executes(
                                             p_448919_ -> fill(
                                                (CommandSourceStack)p_448919_.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(p_448919_, "from"),
                                                BlockPosArgument.getLoadedBlockPos(p_448919_, "to"),
                                                ResourceArgument.getResource(p_448919_, "biome", Registries.BIOME),
                                                ResourceOrTagArgument.getResourceOrTag(p_448919_, "filter", Registries.BIOME)
                                             )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int quantize(int p_261998_) {
      return QuartPos.toBlock(QuartPos.fromBlock(p_261998_));
   }

   private static BlockPos quantize(BlockPos p_262148_) {
      return new BlockPos(quantize(p_262148_.getX()), quantize(p_262148_.getY()), quantize(p_262148_.getZ()));
   }

   private static BiomeResolver makeResolver(
      MutableInt p_262615_, ChunkAccess p_262698_, BoundingBox p_262622_, Holder<Biome> p_262705_, Predicate<Holder<Biome>> p_262695_
   ) {
      return (p_262550_, p_262551_, p_262552_, p_262553_) -> {
         int i = QuartPos.toBlock(p_262550_);
         int j = QuartPos.toBlock(p_262551_);
         int k = QuartPos.toBlock(p_262552_);
         Holder<Biome> holder = p_262698_.getNoiseBiome(p_262550_, p_262551_, p_262552_);
         if (p_262622_.isInside(i, j, k) && p_262695_.test(holder)) {
            p_262615_.increment();
            return p_262705_;
         } else {
            return holder;
         }
      };
   }

   public static Either<Integer, CommandSyntaxException> fill(ServerLevel p_312613_, BlockPos p_311970_, BlockPos p_311934_, Holder<Biome> p_310918_) {
      return fill(p_312613_, p_311970_, p_311934_, p_310918_, p_262543_ -> true, p_308701_ -> {});
   }

   public static Either<Integer, CommandSyntaxException> fill(
      ServerLevel p_312916_,
      BlockPos p_312866_,
      BlockPos p_311741_,
      Holder<Biome> p_311864_,
      Predicate<Holder<Biome>> p_311950_,
      Consumer<Supplier<Component>> p_313249_
   ) {
      BlockPos blockpos = quantize(p_312866_);
      BlockPos blockpos1 = quantize(p_311741_);
      BoundingBox boundingbox = BoundingBox.fromCorners(blockpos, blockpos1);
      int i = boundingbox.getXSpan() * boundingbox.getYSpan() * boundingbox.getZSpan();
      int j = p_312916_.getGameRules().get(GameRules.MAX_BLOCK_MODIFICATIONS);
      if (i > j) {
         return Either.right(ERROR_VOLUME_TOO_LARGE.create(j, i));
      }

      List<ChunkAccess> list = new ArrayList<>();

      for (int k = SectionPos.blockToSectionCoord(boundingbox.minZ()); k <= SectionPos.blockToSectionCoord(boundingbox.maxZ()); k++) {
         for (int l = SectionPos.blockToSectionCoord(boundingbox.minX()); l <= SectionPos.blockToSectionCoord(boundingbox.maxX()); l++) {
            ChunkAccess chunkaccess = p_312916_.getChunk(l, k, ChunkStatus.FULL, false);
            if (chunkaccess == null) {
               return Either.right(ERROR_NOT_LOADED.create());
            }

            list.add(chunkaccess);
         }
      }

      MutableInt mutableint = new MutableInt(0);

      for (ChunkAccess chunkaccess1 : list) {
         chunkaccess1.fillBiomesFromNoise(
            makeResolver(mutableint, chunkaccess1, boundingbox, p_311864_, p_311950_), p_312916_.getChunkSource().randomState().sampler()
         );
         chunkaccess1.markUnsaved();
      }

      p_312916_.getChunkSource().chunkMap.resendBiomesForChunks(list);
      p_313249_.accept(
         () -> Component.translatable(
            "commands.fillbiome.success.count",
            mutableint.intValue(),
            boundingbox.minX(),
            boundingbox.minY(),
            boundingbox.minZ(),
            boundingbox.maxX(),
            boundingbox.maxY(),
            boundingbox.maxZ()
         )
      );
      return Either.left(mutableint.intValue());
   }

   private static int fill(
      CommandSourceStack p_262664_, BlockPos p_262651_, BlockPos p_262678_, Holder.Reference<Biome> p_262612_, Predicate<Holder<Biome>> p_262697_
   ) throws CommandSyntaxException {
      Either<Integer, CommandSyntaxException> either = fill(
         p_262664_.getLevel(), p_262651_, p_262678_, p_262612_, p_262697_, p_308699_ -> p_262664_.sendSuccess(p_308699_, true)
      );
      Optional<CommandSyntaxException> optional = either.right();
      if (optional.isPresent()) {
         throw (CommandSyntaxException)optional.get();
      } else {
         return (Integer)either.left().get();
      }
   }
}
