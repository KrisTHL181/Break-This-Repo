package net.minecraft.core.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public record BlockTransformer(List<BlockTransformer.BlockTransformData> transforms) {
   public static final Codec<BlockTransformer> CODEC = BlockTransformer.BlockTransformData.CODEC
      .listOf(1, 200)
      .xmap(BlockTransformer::new, BlockTransformer::transforms);
   public static final StreamCodec<RegistryFriendlyByteBuf, BlockTransformer> STREAM_CODEC = StreamCodec.composite(
      BlockTransformer.BlockTransformData.STREAM_CODEC.apply(ByteBufCodecs.list()), BlockTransformer::transforms, BlockTransformer::new
   );

   public InteractionResult transformBlock(final UseOnContext context) {
      if (playerHasBlockingItemUseIntent(context)) {
         return InteractionResult.PASS;
      }

      BlockPos pos = context.getClickedPos();
      Level level = context.getLevel();

      for (BlockTransformer.BlockTransformData transformData : this.transforms) {
         Direction clickedFace = context.getClickedFace();
         if (!transformData.disallowedFaces().contains(clickedFace)) {
            BlockState newBlockState = transformData.blockStateProvider.getOptionalState(level, level.getRandom(), pos);
            if (newBlockState != null) {
               BlockState updatedShape = transformData.updateFromNeighbors ? Block.updateFromNeighbourShapes(newBlockState, level, pos) : newBlockState;
               Player player = context.getPlayer();
               ItemStack itemInHand = context.getItemInHand();
               if (player instanceof ServerPlayer serverPlayer) {
                  CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, pos, itemInHand);
               }

               BlockState oldBlockState = level.getBlockState(pos);
               if (level instanceof ServerLevel serverLevel) {
                  transformData.loot
                     .ifPresent(
                        lt -> Block.dropFromBlockInteractLootTable(
                           serverLevel,
                           (ResourceKey<LootTable>)lt,
                           oldBlockState,
                           level.getBlockEntity(pos),
                           itemInHand,
                           player,
                           (sl, stack) -> transformData.dropStrategy.resourcePopper.pop(sl, pos, clickedFace, stack)
                        )
                     );
               }

               if (itemInHand.isStackable()) {
                  itemInHand.consume(transformData.consumeOnUse ? 1 : 0, player);
               } else if (player != null) {
                  itemInHand.hurtAndBreak(transformData.itemDamagePerUse, player, context.getHand().asEquipmentSlot());
               }

               level.setBlock(pos, updatedShape, 11);
               level.playSound(player, pos, transformData.sound.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
               transformData.particle.send(level, player, pos);
               level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, updatedShape));
               if (transformData.transformType == BlockTransformer.TransformType.COPPER_CHEST
                  && oldBlockState.getBlock() instanceof CopperChestBlock
                  && oldBlockState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                  BlockPos neighborPos = ChestBlock.getConnectedBlockPos(pos, oldBlockState);
                  level.gameEvent(GameEvent.BLOCK_CHANGE, neighborPos, GameEvent.Context.of(player, level.getBlockState(neighborPos)));
                  transformData.particle.send(level, player, neighborPos);
               }

               return InteractionResult.SUCCESS;
            }
         }
      }

      return InteractionResult.PASS;
   }

   private static boolean playerHasBlockingItemUseIntent(final UseOnContext context) {
      Player player = context.getPlayer();
      return context.getHand().equals(InteractionHand.MAIN_HAND)
         && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)
         && !player.isSecondaryUseActive();
   }

   public record BlockTransformData(
      BlockStateProvider blockStateProvider,
      Holder<SoundEvent> sound,
      BlockTransformer.TransformParticle particle,
      List<Direction> disallowedFaces,
      Optional<ResourceKey<LootTable>> loot,
      BlockTransformer.DropStrategy dropStrategy,
      boolean updateFromNeighbors,
      BlockTransformer.TransformType transformType,
      boolean consumeOnUse,
      int itemDamagePerUse
   ) {
      public static final Codec<BlockTransformer.BlockTransformData> CODEC = RecordCodecBuilder.create(
         i -> i.group(
               BlockStateProvider.CODEC.fieldOf("block_state_provider").forGetter(BlockTransformer.BlockTransformData::blockStateProvider),
               SoundEvent.CODEC
                  .optionalFieldOf("sound", BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY))
                  .forGetter(BlockTransformer.BlockTransformData::sound),
               BlockTransformer.TransformParticle.CODEC
                  .optionalFieldOf("particle", BlockTransformer.TransformParticle.NONE)
                  .forGetter(BlockTransformer.BlockTransformData::particle),
               Direction.CODEC.listOf().optionalFieldOf("disallowed_faces", List.of()).forGetter(BlockTransformer.BlockTransformData::disallowedFaces),
               LootTable.KEY_CODEC.optionalFieldOf("loot").forGetter(BlockTransformer.BlockTransformData::loot),
               BlockTransformer.DropStrategy.CODEC
                  .optionalFieldOf("drop_strategy", BlockTransformer.DropStrategy.FROM_MIDDLE)
                  .forGetter(BlockTransformer.BlockTransformData::dropStrategy),
               Codec.BOOL.optionalFieldOf("update_from_neighbors", true).forGetter(BlockTransformer.BlockTransformData::updateFromNeighbors),
               BlockTransformer.TransformType.CODEC
                  .optionalFieldOf("transform_type", BlockTransformer.TransformType.SINGLE_BLOCK)
                  .forGetter(BlockTransformer.BlockTransformData::transformType),
               Codec.BOOL.optionalFieldOf("consume_on_use", true).forGetter(BlockTransformer.BlockTransformData::consumeOnUse),
               ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("item_damage_per_use", 0).forGetter(BlockTransformer.BlockTransformData::itemDamagePerUse)
            )
            .apply(i, BlockTransformer.BlockTransformData::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, BlockTransformer.BlockTransformData> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.fromCodecWithRegistries(BlockStateProvider.CODEC),
         BlockTransformer.BlockTransformData::blockStateProvider,
         SoundEvent.STREAM_CODEC,
         BlockTransformer.BlockTransformData::sound,
         BlockTransformer.TransformParticle.STREAM_CODEC,
         BlockTransformer.BlockTransformData::particle,
         Direction.STREAM_CODEC.apply(ByteBufCodecs.list()),
         BlockTransformer.BlockTransformData::disallowedFaces,
         ResourceKey.streamCodec(Registries.LOOT_TABLE).apply(ByteBufCodecs::optional),
         BlockTransformer.BlockTransformData::loot,
         BlockTransformer.DropStrategy.STREAM_CODEC,
         BlockTransformer.BlockTransformData::dropStrategy,
         ByteBufCodecs.BOOL,
         BlockTransformer.BlockTransformData::updateFromNeighbors,
         BlockTransformer.TransformType.STREAM_CODEC,
         BlockTransformer.BlockTransformData::transformType,
         ByteBufCodecs.BOOL,
         BlockTransformer.BlockTransformData::consumeOnUse,
         ByteBufCodecs.VAR_INT,
         BlockTransformer.BlockTransformData::itemDamagePerUse,
         BlockTransformer.BlockTransformData::new
      );

      public static BlockTransformer.BlockTransformData.Builder builder(final BlockStateProvider targetStateProvider) {
         return new BlockTransformer.BlockTransformData.Builder(targetStateProvider);
      }

      public static BlockTransformer.BlockTransformData.Builder builder(final BlockPredicate predicate, final BlockStateProvider stateProvider) {
         return builder(RuleBasedStateProvider.builder().ifTrueThenProvide(predicate, stateProvider).build());
      }

      public static BlockTransformer.BlockTransformData.Builder builder(final BlockPredicate predicate, final Block block) {
         return builder(RuleBasedStateProvider.builder().ifTrueThenProvide(predicate, block).build());
      }

      public static class Builder {
         private final BlockStateProvider targetStateProvider;
         private Holder<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY);
         private BlockTransformer.TransformParticle particle = BlockTransformer.TransformParticle.NONE;
         private List<Direction> disallowedFaces = List.of();
         private Optional<ResourceKey<LootTable>> loot = Optional.empty();
         private BlockTransformer.DropStrategy dropStrategy = BlockTransformer.DropStrategy.FROM_MIDDLE;
         private boolean updateFromNeighbors = true;
         private BlockTransformer.TransformType transformType = BlockTransformer.TransformType.SINGLE_BLOCK;
         private boolean consumeOnUse = true;
         private int itemDamagePerUse = 1;

         private Builder(final BlockStateProvider targetStateProvider) {
            this.targetStateProvider = targetStateProvider;
         }

         public BlockTransformer.BlockTransformData.Builder sound(final Holder<SoundEvent> sound) {
            this.sound = sound;
            return this;
         }

         public BlockTransformer.BlockTransformData.Builder particle(final BlockTransformer.TransformParticle particle) {
            this.particle = particle;
            return this;
         }

         public BlockTransformer.BlockTransformData.Builder disallowedFaces(final List<Direction> disallowedFaces) {
            this.disallowedFaces = disallowedFaces;
            return this;
         }

         public BlockTransformer.BlockTransformData.Builder loot(final ResourceKey<LootTable> loot) {
            this.loot = Optional.of(loot);
            return this;
         }

         public BlockTransformer.BlockTransformData.Builder dropStrategy(final BlockTransformer.DropStrategy dropStrategy) {
            this.dropStrategy = dropStrategy;
            return this;
         }

         public BlockTransformer.BlockTransformData.Builder updateFromNeighbors(final boolean updateFromNeighbors) {
            this.updateFromNeighbors = updateFromNeighbors;
            return this;
         }

         public BlockTransformer.BlockTransformData.Builder transformType(final BlockTransformer.TransformType transformType) {
            this.transformType = transformType;
            return this;
         }

         public BlockTransformer.BlockTransformData.Builder consumeOnUse(final boolean consumeOnUse) {
            this.consumeOnUse = consumeOnUse;
            return this;
         }

         public BlockTransformer.BlockTransformData.Builder itemDamagePerUse(final int itemDamagePerUse) {
            this.itemDamagePerUse = itemDamagePerUse;
            return this;
         }

         public BlockTransformer.BlockTransformData build() {
            return new BlockTransformer.BlockTransformData(
               this.targetStateProvider,
               this.sound,
               this.particle,
               this.disallowedFaces,
               this.loot,
               this.dropStrategy,
               this.updateFromNeighbors,
               this.transformType,
               this.consumeOnUse,
               this.itemDamagePerUse
            );
         }
      }
   }

   public enum DropStrategy implements StringRepresentable {
      CLICKED_FACE(0, "clicked_face", BlockTransformer.DropStrategy.ResourcePopper.FROM_FACE),
      FROM_MIDDLE(1, "from_middle", BlockTransformer.DropStrategy.ResourcePopper.FROM_MIDDLE);

      private final int id;
      private final String name;
      private final BlockTransformer.DropStrategy.ResourcePopper resourcePopper;
      public static final Codec<BlockTransformer.DropStrategy> CODEC = StringRepresentable.fromValues(BlockTransformer.DropStrategy::values);
      private static final IntFunction<BlockTransformer.DropStrategy> BY_ID = ByIdMap.continuous(
         BlockTransformer.DropStrategy::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      public static final StreamCodec<ByteBuf, BlockTransformer.DropStrategy> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, BlockTransformer.DropStrategy::getId);

      DropStrategy(final int id, final String name, final BlockTransformer.DropStrategy.ResourcePopper resourcePopper) {
         this.id = id;
         this.name = name;
         this.resourcePopper = resourcePopper;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public int getId() {
         return this.id;
      }

      public void pop(final Level level, final BlockPos pos, final Direction direction, final ItemStack stack) {
         this.resourcePopper.pop(level, pos, direction, stack);
      }

      @FunctionalInterface
      public interface ResourcePopper {
         BlockTransformer.DropStrategy.ResourcePopper FROM_FACE = Block::popResourceFromFace;
         BlockTransformer.DropStrategy.ResourcePopper FROM_MIDDLE = (level, pos, var2, stack) -> Block.popResource(level, pos, stack);

         void pop(final Level level, final BlockPos pos, final Direction direction, final ItemStack stack);
      }
   }

   public enum TransformParticle implements StringRepresentable {
      NONE(0, "none", 0),
      SCRAPE(1, "scrape", 3005),
      WAX_ON(2, "wax_on", 3003),
      WAX_OFF(3, "wax_off", 3004);

      private final int id;
      private final String name;
      private final int levelEvent;
      public static final Codec<BlockTransformer.TransformParticle> CODEC = StringRepresentable.fromValues(BlockTransformer.TransformParticle::values);
      private static final IntFunction<BlockTransformer.TransformParticle> BY_ID = ByIdMap.continuous(
         BlockTransformer.TransformParticle::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      public static final StreamCodec<ByteBuf, BlockTransformer.TransformParticle> STREAM_CODEC = ByteBufCodecs.idMapper(
         BY_ID, BlockTransformer.TransformParticle::getId
      );

      TransformParticle(final int id, final String name, final int levelEvent) {
         this.id = id;
         this.name = name;
         this.levelEvent = levelEvent;
      }

      public void send(final Level level, final @Nullable Entity player, final BlockPos pos) {
         if (!this.equals(NONE)) {
            level.levelEvent(player, this.getLevelEvent(), pos, 0);
         }
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public int getId() {
         return this.id;
      }

      public int getLevelEvent() {
         return this.levelEvent;
      }
   }

   public enum TransformType implements StringRepresentable {
      SINGLE_BLOCK(0, "single_block"),
      COPPER_CHEST(1, "copper_chest");

      private final int id;
      private final String name;
      public static final Codec<BlockTransformer.TransformType> CODEC = StringRepresentable.fromValues(BlockTransformer.TransformType::values);
      private static final IntFunction<BlockTransformer.TransformType> BY_ID = ByIdMap.continuous(
         BlockTransformer.TransformType::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      public static final StreamCodec<ByteBuf, BlockTransformer.TransformType> STREAM_CODEC = ByteBufCodecs.idMapper(
         BY_ID, BlockTransformer.TransformType::getId
      );

      TransformType(final int id, final String name) {
         this.id = id;
         this.name = name;
      }

      @Override
      public String getSerializedName() {
         return this.name;
      }

      public int getId() {
         return this.id;
      }
   }
}
