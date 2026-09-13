package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class AdventureModePredicate {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<AdventureModePredicate> CODEC = ExtraCodecs.compactListCodec(
         BlockPredicate.CODEC, ExtraCodecs.nonEmptyList(BlockPredicate.CODEC.listOf())
      )
      .xmap(AdventureModePredicate::new, p -> p.predicates);
   public static final StreamCodec<RegistryFriendlyByteBuf, AdventureModePredicate> STREAM_CODEC = StreamCodec.composite(
      BlockPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), predicate -> predicate.predicates, AdventureModePredicate::new
   );
   public static final Component CAN_BREAK_HEADER = Component.translatable("item.canBreak").withStyle(ChatFormatting.GRAY);
   public static final Component CAN_PLACE_HEADER = Component.translatable("item.canPlace").withStyle(ChatFormatting.GRAY);
   private static final Component UNKNOWN_USE = Component.translatable("item.canUse.unknown").withStyle(ChatFormatting.GRAY);
   private final List<BlockPredicate> predicates;
   private @Nullable List<Component> cachedTooltip;
   private @Nullable BlockInWorld lastCheckedBlock;
   private boolean lastResult;
   private boolean checksBlockEntity;

   public AdventureModePredicate(final List<BlockPredicate> predicates) {
      this.predicates = predicates;
   }

   private static boolean areSameBlocks(final BlockInWorld blockInWorld, final @Nullable BlockInWorld cachedBlock, final boolean checkBlockEntity) {
      if (cachedBlock == null || blockInWorld.getState() != cachedBlock.getState()) {
         return false;
      }

      if (!checkBlockEntity) {
         return true;
      }

      if (blockInWorld.getEntity() == null && cachedBlock.getEntity() == null) {
         return true;
      }

      if (blockInWorld.getEntity() != null && cachedBlock.getEntity() != null) {
         try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER)) {
            RegistryAccess registryAccess = blockInWorld.getLevel().registryAccess();
            CompoundTag inWorldTag = saveBlockEntity(blockInWorld.getEntity(), registryAccess, reporter);
            CompoundTag cachedTag = saveBlockEntity(cachedBlock.getEntity(), registryAccess, reporter);
            return Objects.equals(inWorldTag, cachedTag);
         }
      } else {
         return false;
      }
   }

   private static CompoundTag saveBlockEntity(final BlockEntity blockEntity, final RegistryAccess registryAccess, final ProblemReporter reporter) {
      TagValueOutput inWorldOutput = TagValueOutput.createWithContext(reporter.forChild(blockEntity.problemPath()), registryAccess);
      blockEntity.saveWithId(inWorldOutput);
      return inWorldOutput.buildResult();
   }

   public boolean test(final BlockInWorld blockInWorld) {
      if (areSameBlocks(blockInWorld, this.lastCheckedBlock, this.checksBlockEntity)) {
         return this.lastResult;
      }

      this.lastCheckedBlock = blockInWorld;
      this.checksBlockEntity = false;

      for (BlockPredicate predicate : this.predicates) {
         if (predicate.matches(blockInWorld)) {
            this.checksBlockEntity = this.checksBlockEntity | predicate.requiresNbt();
            this.lastResult = true;
            return true;
         }
      }

      this.lastResult = false;
      return false;
   }

   private List<Component> tooltip() {
      if (this.cachedTooltip == null) {
         this.cachedTooltip = computeTooltip(this.predicates);
      }

      return this.cachedTooltip;
   }

   public void addToTooltip(final Consumer<Component> consumer) {
      this.tooltip().forEach(consumer);
   }

   private static List<Component> computeTooltip(final List<BlockPredicate> predicates) {
      for (BlockPredicate predicate : predicates) {
         if (predicate.blocks().isEmpty()) {
            return List.of(UNKNOWN_USE);
         }
      }

      return predicates.stream()
         .flatMap(predicatex -> predicatex.blocks().orElseThrow().stream())
         .distinct()
         .map(block -> block.value().getName().withStyle(ChatFormatting.DARK_GRAY))
         .toList();
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      } else {
         return obj instanceof AdventureModePredicate predicate ? this.predicates.equals(predicate.predicates) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.predicates.hashCode();
   }

   @Override
   public String toString() {
      return "AdventureModePredicate{predicates=" + this.predicates + "}";
   }
}
