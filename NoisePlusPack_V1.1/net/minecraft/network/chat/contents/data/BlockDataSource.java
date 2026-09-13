package net.minecraft.network.chat.contents.data;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public record BlockDataSource(String posPattern, @Nullable Coordinates compiledPos) implements DataSource {
   public static final MapCodec<BlockDataSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_430478_ -> p_430478_.group(Codec.STRING.fieldOf("block").forGetter(BlockDataSource::posPattern)).apply(p_430478_, BlockDataSource::new)
   );

   public BlockDataSource(String p_423517_) {
      this(p_423517_, compilePos(p_423517_));
   }

   private static @Nullable Coordinates compilePos(String p_428825_) {
      try {
         return BlockPosArgument.blockPos().parse(new StringReader(p_428825_));
      } catch (CommandSyntaxException commandsyntaxexception) {
         return null;
      }
   }

   @Override
   public Stream<CompoundTag> getData(CommandSourceStack p_428119_) {
      if (this.compiledPos != null) {
         ServerLevel serverlevel = p_428119_.getLevel();
         BlockPos blockpos = this.compiledPos.getBlockPos(p_428119_);
         if (serverlevel.isLoaded(blockpos)) {
            BlockEntity blockentity = serverlevel.getBlockEntity(blockpos);
            if (blockentity != null) {
               return Stream.of(blockentity.saveWithFullMetadata(p_428119_.registryAccess()));
            }
         }
      }

      return Stream.empty();
   }

   @Override
   public MapCodec<BlockDataSource> codec() {
      return MAP_CODEC;
   }

   @Override
   public String toString() {
      return "block=" + this.posPattern;
   }

   @Override
   public boolean equals(Object p_427186_) {
      return this == p_427186_ ? true : p_427186_ instanceof BlockDataSource blockdatasource && this.posPattern.equals(blockdatasource.posPattern);
   }

   @Override
   public int hashCode() {
      return this.posPattern.hashCode();
   }
}
