package net.minecraft.advancements.predicates;

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
      i -> i.group(
            RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("blocks").forGetter(BlockPredicate::blocks),
            StatePropertiesPredicate.CODEC.optionalFieldOf("state").forGetter(BlockPredicate::properties),
            NbtPredicate.CODEC.optionalFieldOf("nbt").forGetter(BlockPredicate::nbt),
            DataComponentMatchers.CODEC.forGetter(BlockPredicate::components)
         )
         .apply(i, BlockPredicate::new)
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

   public boolean matches(final ServerLevel level, final BlockPos pos) {
      if (!level.isLoaded(pos)) {
         return false;
      }

      if (!this.matchesState(level.getBlockState(pos))) {
         return false;
      }

      if (this.nbt.isPresent() || !this.components.isEmpty()) {
         BlockEntity blockEntity = level.getBlockEntity(pos);
         if (this.nbt.isPresent() && !matchesBlockEntity(level, blockEntity, this.nbt.get())) {
            return false;
         }

         if (!this.components.isEmpty() && !matchesComponents(blockEntity, this.components)) {
            return false;
         }
      }

      return true;
   }

   public boolean matches(final BlockInWorld blockInWorld) {
      return !this.matchesState(blockInWorld.getState())
         ? false
         : !this.nbt.isPresent() || matchesBlockEntity(blockInWorld.getLevel(), blockInWorld.getEntity(), this.nbt.get());
   }

   private boolean matchesState(final BlockState state) {
      return this.blocks.isPresent() && !state.is(this.blocks.get()) ? false : !this.properties.isPresent() || this.properties.get().matches(state);
   }

   private static boolean matchesBlockEntity(final LevelReader level, final @Nullable BlockEntity entity, final NbtPredicate nbt) {
      return entity != null && nbt.matches(entity.saveWithFullMetadata(level.registryAccess()));
   }

   private static boolean matchesComponents(final @Nullable BlockEntity entity, final DataComponentMatchers components) {
      return entity != null && components.test(entity.collectComponents());
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

      public BlockPredicate.Builder of(final HolderGetter<Block> lookup, final Block... blocks) {
         return this.of(lookup, Arrays.asList(blocks));
      }

      public BlockPredicate.Builder of(final HolderGetter<Block> lookup, final Collection<Block> blocks) {
         this.blocks = Optional.of(HolderSet.direct(Block::builtInRegistryHolder, blocks));
         return this;
      }

      public BlockPredicate.Builder of(final HolderGetter<Block> lookup, final TagKey<Block> tag) {
         this.blocks = Optional.of(lookup.getOrThrow(tag));
         return this;
      }

      public BlockPredicate.Builder hasNbt(final CompoundTag nbt) {
         this.nbt = Optional.of(new NbtPredicate(nbt));
         return this;
      }

      public BlockPredicate.Builder setProperties(final StatePropertiesPredicate.Builder properties) {
         this.properties = properties.build();
         return this;
      }

      public BlockPredicate.Builder components(final DataComponentMatchers components) {
         this.components = components;
         return this;
      }

      public BlockPredicate build() {
         return new BlockPredicate(this.blocks, this.properties, this.nbt, this.components);
      }
   }
}
