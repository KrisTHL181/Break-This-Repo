package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.criterion.BlockPredicate;
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
      .xmap(AdventureModePredicate::new, p_329117_ -> p_329117_.predicates);
   public static final StreamCodec<RegistryFriendlyByteBuf, AdventureModePredicate> STREAM_CODEC = StreamCodec.composite(
      BlockPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), p_333442_ -> p_333442_.predicates, AdventureModePredicate::new
   );
   public static final Component CAN_BREAK_HEADER = Component.translatable("item.canBreak").withStyle(ChatFormatting.GRAY);
   public static final Component CAN_PLACE_HEADER = Component.translatable("item.canPlace").withStyle(ChatFormatting.GRAY);
   private static final Component UNKNOWN_USE = Component.translatable("item.canUse.unknown").withStyle(ChatFormatting.GRAY);
   private final List<BlockPredicate> predicates;
   private @Nullable List<Component> cachedTooltip;
   private @Nullable BlockInWorld lastCheckedBlock;
   private boolean lastResult;
   private boolean checksBlockEntity;

   public AdventureModePredicate(List<BlockPredicate> p_336068_) {
      this.predicates = p_336068_;
   }

   private static boolean areSameBlocks(BlockInWorld p_330769_, @Nullable BlockInWorld p_330025_, boolean p_331117_) {
      if (p_330025_ == null || p_330769_.getState() != p_330025_.getState()) {
         return false;
      }

      if (!p_331117_) {
         return true;
      }

      if (p_330769_.getEntity() == null && p_330025_.getEntity() == null) {
         return true;
      }

      if (p_330769_.getEntity() != null && p_330025_.getEntity() != null) {
         try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(LOGGER)) {
            RegistryAccess registryaccess = p_330769_.getLevel().registryAccess();
            CompoundTag compoundtag = saveBlockEntity(p_330769_.getEntity(), registryaccess, problemreporter$scopedcollector);
            CompoundTag compoundtag1 = saveBlockEntity(p_330025_.getEntity(), registryaccess, problemreporter$scopedcollector);
            return Objects.equals(compoundtag, compoundtag1);
         }
      } else {
         return false;
      }
   }

   private static CompoundTag saveBlockEntity(BlockEntity p_407833_, RegistryAccess p_410285_, ProblemReporter p_409700_) {
      TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(p_409700_.forChild(p_407833_.problemPath()), p_410285_);
      p_407833_.saveWithId(tagvalueoutput);
      return tagvalueoutput.buildResult();
   }

   public boolean test(BlockInWorld p_333716_) {
      if (areSameBlocks(p_333716_, this.lastCheckedBlock, this.checksBlockEntity)) {
         return this.lastResult;
      }

      this.lastCheckedBlock = p_333716_;
      this.checksBlockEntity = false;

      for (BlockPredicate blockpredicate : this.predicates) {
         if (blockpredicate.matches(p_333716_)) {
            this.checksBlockEntity = this.checksBlockEntity | blockpredicate.requiresNbt();
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

   public void addToTooltip(Consumer<Component> p_334654_) {
      this.tooltip().forEach(p_334654_);
   }

   private static List<Component> computeTooltip(List<BlockPredicate> p_328947_) {
      for (BlockPredicate blockpredicate : p_328947_) {
         if (blockpredicate.blocks().isEmpty()) {
            return List.of(UNKNOWN_USE);
         }
      }

      return p_328947_.stream()
         .flatMap(p_449774_ -> p_449774_.blocks().orElseThrow().stream())
         .distinct()
         .map(p_335858_ -> p_335858_.value().getName().withStyle(ChatFormatting.DARK_GRAY))
         .toList();
   }

   @Override
   public boolean equals(Object p_331232_) {
      if (this == p_331232_) {
         return true;
      } else {
         return p_331232_ instanceof AdventureModePredicate adventuremodepredicate ? this.predicates.equals(adventuremodepredicate.predicates) : false;
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
