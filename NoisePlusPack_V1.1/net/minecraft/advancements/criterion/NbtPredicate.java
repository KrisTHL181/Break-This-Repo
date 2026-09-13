package net.minecraft.advancements.criterion;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record NbtPredicate(CompoundTag tag) {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<NbtPredicate> CODEC = TagParser.LENIENT_CODEC.xmap(NbtPredicate::new, NbtPredicate::tag);
   public static final StreamCodec<ByteBuf, NbtPredicate> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(NbtPredicate::new, NbtPredicate::tag);
   public static final String SELECTED_ITEM_TAG = "SelectedItem";

   public boolean matches(DataComponentGetter p_457616_) {
      CustomData customdata = p_457616_.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
      return customdata.matchedBy(this.tag);
   }

   public boolean matches(Entity p_455382_) {
      return this.matches(getEntityTagToCompare(p_455382_));
   }

   public boolean matches(@Nullable Tag p_451373_) {
      return p_451373_ != null && NbtUtils.compareNbt(this.tag, p_451373_, true);
   }

   public static CompoundTag getEntityTagToCompare(Entity p_456125_) {
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_456125_.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_456125_.registryAccess());
         p_456125_.saveWithoutId(tagvalueoutput);
         if (p_456125_ instanceof Player player) {
            ItemStack itemstack = player.getInventory().getSelectedItem();
            if (!itemstack.isEmpty()) {
               tagvalueoutput.store("SelectedItem", ItemStack.CODEC, itemstack);
            }
         }

         return tagvalueoutput.buildResult();
      }
   }
}
