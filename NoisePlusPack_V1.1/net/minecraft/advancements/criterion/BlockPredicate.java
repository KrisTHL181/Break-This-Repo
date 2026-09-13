package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.jspecify.annotations.Nullable;

public record BlockPredicate(
   Optional<HolderSet<Block>> blocks, Optional<StatePropertiesPredicate> properties, Optional<NbtPredicate> nbt, DataComponentMatchers components
) {
   public static final Codec<BlockPredicate> CODEC = RecordCodecBuilder.create(
      p_454421_ -> p_454421_.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("blocks").forGetter(BlockPredicate::blocks),
            StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(BlockPredicate::properties),
            NbtPredicate.CODEC.optionalFieldOf("nbt").forGetter(BlockPredicate::nbt),
            DataComponentMatchers.CODEC.forGetter(BlockPredicate::components)
         )
         .apply(p_454421_, BlockPredicate::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, BlockPredicate> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.optional(ByteBufCodecs.holderSet(Registries.BLOCK)),
      BlockPredicate::blocks,
      ByteBufCodecs.optional(StatePropertiesPredicate.STREAM_CODEC),
      BlockPredicate::properties,
      ByteBufCodecs.optional(NbtPredicate.STREAM_CODEC),
      BlockPredicate::nbt,
      DataComponentMatchers.STREAM_CODEC,
      BlockPredicate::components,
      BlockPredicate::new
   );

   public boolean matches(ServerLevel p_460235_, BlockPos p_454322_) {
      if (!p_460235_.isLoaded(p_454322_)) {
         return false;
      }

      if (!this.matchesState(p_460235_.getBlockState(p_454322_))) {
         return false;
      }

      if (this.nbt.isPresent() || !this.components.isEmpty()) {
         BlockEntity blockentity = p_460235_.getBlockEntity(p_454322_);
         if (this.nbt.isPresent() && !matchesBlockEntity(p_460235_, blockentity, this.nbt.get())) {
            return false;
         }

         if (!this.components.isEmpty() && !matchesComponents(blockentity, this.components)) {
            return false;
         }
      }

      return true;
   }

   public boolean matches(BlockInWorld p_453426_) {
      return !this.matchesState(p_453426_.getState())
         ? false
         : !this.nbt.isPresent() || matchesBlockEntity(p_453426_.getLevel(), p_453426_.getEntity(), this.nbt.get());
   }

   private boolean matchesState(BlockState p_450268_) {
      return this.blocks.isPresent() && !p_450268_.is(this.blocks.get()) ? false : !this.properties.isPresent() || this.properties.get().matches(p_450268_);
   }

   private static boolean matchesBlockEntity(LevelReader p_451674_, @Nullable BlockEntity p_457244_, NbtPredicate p_452985_) {
      return p_457244_ != null && p_452985_.matches(p_457244_.saveWithFullMetadata(p_451674_.registryAccess()));
   }

   private static boolean matchesComponents(@Nullable BlockEntity p_455937_, DataComponentMatchers p_459108_) {
      return p_455937_ != null && p_459108_.test(p_455937_.collectComponents());
   }

   public boolean requiresNbt() {
      return this.nbt.isPresent();
   }

   public static class Builder {
      private Optional<HolderSet<Block>> blocks = Optional.empty();
      private Optional<StatePropertiesPredicate> properties = Optional.empty();
      private Optional<NbtPredicate> nbt = Optional.empty();
      private DataComponentMatchers components = DataComponentMatchers.ANY;

      private Builder() {
      }

      public static BlockPredicate.Builder block() {
         return new BlockPredicate.Builder();
      }

      public BlockPredicate.Builder of(HolderGetter<Block> p_460386_, Block... p_459807_) {
         return this.of(p_460386_, Arrays.asList(p_459807_));
      }

      public BlockPredicate.Builder of(HolderGetter<Block> p_453123_, Collection<Block> p_458959_) {
         this.blocks = Optional.of(HolderSet.direct(Block::builtInRegistryHolder, p_458959_));
         return this;
      }

      public BlockPredicate.Builder of(HolderGetter<Block> p_453965_, TagKey<Block> p_459212_) {
         this.blocks = Optional.of(p_453965_.getOrThrow(p_459212_));
         return this;
      }

      public BlockPredicate.Builder hasNbt(CompoundTag p_453261_) {
         this.nbt = Optional.of(new NbtPredicate(p_453261_));
         return this;
      }

      public BlockPredicate.Builder setProperties(StatePropertiesPredicate.Builder p_455119_) {
         this.properties = p_455119_.build();
         return this;
      }

      public BlockPredicate.Builder components(DataComponentMatchers p_458058_) {
         this.components = p_458058_;
         return this;
      }

      public BlockPredicate build() {
         return new BlockPredicate(this.blocks, this.properties, this.nbt, this.components);
      }
   }
}
