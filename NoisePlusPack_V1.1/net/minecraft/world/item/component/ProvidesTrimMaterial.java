package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

public record ProvidesTrimMaterial(EitherHolder<TrimMaterial> material) {
   public static final Codec<ProvidesTrimMaterial> CODEC = EitherHolder.codec(Registries.TRIM_MATERIAL, TrimMaterial.CODEC)
      .xmap(ProvidesTrimMaterial::new, ProvidesTrimMaterial::material);
   public static final StreamCodec<RegistryFriendlyByteBuf, ProvidesTrimMaterial> STREAM_CODEC = EitherHolder.streamCodec(
         Registries.TRIM_MATERIAL, TrimMaterial.STREAM_CODEC
      )
      .map(ProvidesTrimMaterial::new, ProvidesTrimMaterial::material);

   public ProvidesTrimMaterial(Holder<TrimMaterial> p_394626_) {
      this(new EitherHolder<>(p_394626_));
   }

   @Deprecated
   public ProvidesTrimMaterial(ResourceKey<TrimMaterial> p_391631_) {
      this(new EitherHolder<>(p_391631_));
   }

   public Optional<Holder<TrimMaterial>> unwrap(HolderLookup.Provider p_393416_) {
      return this.material.unwrap(p_393416_);
   }
}
