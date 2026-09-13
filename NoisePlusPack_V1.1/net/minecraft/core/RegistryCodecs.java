package net.minecraft.core;

import com.mojang.serialization.Codec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceKey;

public class RegistryCodecs {
   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> p_206280_, Codec<E> p_206281_) {
      return homogeneousList(p_206280_, p_206281_, false);
   }

   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> p_206288_, Codec<E> p_206289_, boolean p_206290_) {
      return HolderSetCodec.create(p_206288_, RegistryFileCodec.create(p_206288_, p_206289_), p_206290_);
   }

   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> p_206278_) {
      return homogeneousList(p_206278_, false);
   }

   public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> p_206311_, boolean p_206312_) {
      return HolderSetCodec.create(p_206311_, RegistryFixedCodec.create(p_206311_), p_206312_);
   }
}
