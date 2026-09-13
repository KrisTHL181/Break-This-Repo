package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Set;
import net.minecraft.advancements.criterion.NbtPredicate;
import net.minecraft.nbt.Tag;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextArg;
import org.jspecify.annotations.Nullable;

public class ContextNbtProvider implements NbtProvider {
   private static final Codec<LootContextArg<Tag>> GETTER_CODEC = LootContextArg.createArgCodec(
      p_450118_ -> p_450118_.anyBlockEntity(ContextNbtProvider.BlockEntitySource::new).anyEntity(ContextNbtProvider.EntitySource::new)
   );
   public static final MapCodec<ContextNbtProvider> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_300408_ -> p_300408_.group(GETTER_CODEC.fieldOf("target").forGetter(p_450117_ -> p_450117_.source)).apply(p_300408_, ContextNbtProvider::new)
   );
   public static final Codec<ContextNbtProvider> INLINE_CODEC = GETTER_CODEC.xmap(ContextNbtProvider::new, p_450119_ -> p_450119_.source);
   private final LootContextArg<Tag> source;

   private ContextNbtProvider(LootContextArg<Tag> p_459589_) {
      this.source = p_459589_;
   }

   @Override
   public LootNbtProviderType getType() {
      return NbtProviders.CONTEXT;
   }

   @Override
   public @Nullable Tag get(LootContext p_165573_) {
      return this.source.get(p_165573_);
   }

   @Override
   public Set<ContextKey<?>> getReferencedContextParams() {
      return Set.of(this.source.contextParam());
   }

   public static NbtProvider forContextEntity(LootContext.EntityTarget p_165571_) {
      return new ContextNbtProvider(new ContextNbtProvider.EntitySource(p_165571_.contextParam()));
   }

   record BlockEntitySource(ContextKey<? extends BlockEntity> contextParam) implements LootContextArg.Getter<BlockEntity, Tag> {
      public Tag get(BlockEntity p_428056_) {
         return p_428056_.saveWithFullMetadata(p_428056_.getLevel().registryAccess());
      }
   }

   record EntitySource(ContextKey<? extends Entity> contextParam) implements LootContextArg.Getter<Entity, Tag> {
      public Tag get(Entity p_426254_) {
         return NbtPredicate.getEntityTagToCompare(p_426254_);
      }
   }
}
