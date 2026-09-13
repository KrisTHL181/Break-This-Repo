package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public final class CustomData {
   public static final CustomData EMPTY = new CustomData(new CompoundTag());
   public static final Codec<CompoundTag> COMPOUND_TAG_CODEC = Codec.withAlternative(CompoundTag.CODEC, TagParser.FLATTENED_CODEC);
   public static final Codec<CustomData> CODEC = COMPOUND_TAG_CODEC.xmap(CustomData::new, p_327962_ -> p_327962_.tag);
   @Deprecated
   public static final StreamCodec<ByteBuf, CustomData> STREAM_CODEC = ByteBufCodecs.COMPOUND_TAG.map(CustomData::new, p_329964_ -> p_329964_.tag);
   private final CompoundTag tag;

   private CustomData(CompoundTag p_331981_) {
      this.tag = p_331981_;
   }

   public static CustomData of(CompoundTag p_334177_) {
      return new CustomData(p_334177_.copy());
   }

   public boolean matchedBy(CompoundTag p_328523_) {
      return NbtUtils.compareNbt(p_328523_, this.tag, true);
   }

   public static void update(DataComponentType<CustomData> p_336008_, ItemStack p_335562_, Consumer<CompoundTag> p_332401_) {
      CustomData customdata = p_335562_.getOrDefault(p_336008_, EMPTY).update(p_332401_);
      if (customdata.tag.isEmpty()) {
         p_335562_.remove(p_336008_);
      } else {
         p_335562_.set(p_336008_, customdata);
      }
   }

   public static void set(DataComponentType<CustomData> p_327973_, ItemStack p_332195_, CompoundTag p_330130_) {
      if (!p_330130_.isEmpty()) {
         p_332195_.set(p_327973_, of(p_330130_));
      } else {
         p_332195_.remove(p_327973_);
      }
   }

   public CustomData update(Consumer<CompoundTag> p_336344_) {
      CompoundTag compoundtag = this.tag.copy();
      p_336344_.accept(compoundtag);
      return new CustomData(compoundtag);
   }

   public boolean isEmpty() {
      return this.tag.isEmpty();
   }

   public CompoundTag copyTag() {
      return this.tag.copy();
   }

   @Override
   public boolean equals(Object p_335284_) {
      if (p_335284_ == this) {
         return true;
      } else {
         return p_335284_ instanceof CustomData customdata ? this.tag.equals(customdata.tag) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.tag.hashCode();
   }

   @Override
   public String toString() {
      return this.tag.toString();
   }
}
