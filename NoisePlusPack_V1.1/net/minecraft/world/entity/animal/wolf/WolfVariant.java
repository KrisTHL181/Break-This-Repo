package net.minecraft.world.entity.animal.wolf;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnCondition;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;

public record WolfVariant(WolfVariant.AssetInfo assetInfo, SpawnPrioritySelectors spawnConditions) implements PriorityProvider<SpawnContext, SpawnCondition> {
   public static final Codec<WolfVariant> DIRECT_CODEC = RecordCodecBuilder.create(
      p_396035_ -> p_396035_.group(
            WolfVariant.AssetInfo.CODEC.fieldOf("assets").forGetter(WolfVariant::assetInfo),
            SpawnPrioritySelectors.CODEC.fieldOf("spawn_conditions").forGetter(WolfVariant::spawnConditions)
         )
         .apply(p_396035_, WolfVariant::new)
   );
   public static final Codec<WolfVariant> NETWORK_CODEC = RecordCodecBuilder.create(
      p_395926_ -> p_395926_.group(WolfVariant.AssetInfo.CODEC.fieldOf("assets").forGetter(WolfVariant::assetInfo)).apply(p_395926_, WolfVariant::new)
   );
   public static final Codec<Holder<WolfVariant>> CODEC = RegistryFixedCodec.create(Registries.WOLF_VARIANT);
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<WolfVariant>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.WOLF_VARIANT);

   private WolfVariant(WolfVariant.AssetInfo p_396636_) {
      this(p_396636_, SpawnPrioritySelectors.EMPTY);
   }

   @Override
   public List<PriorityProvider.Selector<SpawnContext, SpawnCondition>> selectors() {
      return this.spawnConditions.selectors();
   }

   public record AssetInfo(ClientAsset.ResourceTexture wild, ClientAsset.ResourceTexture tame, ClientAsset.ResourceTexture angry) {
      public static final Codec<WolfVariant.AssetInfo> CODEC = RecordCodecBuilder.create(
         p_421838_ -> p_421838_.group(
               ClientAsset.ResourceTexture.CODEC.fieldOf("wild").forGetter(WolfVariant.AssetInfo::wild),
               ClientAsset.ResourceTexture.CODEC.fieldOf("tame").forGetter(WolfVariant.AssetInfo::tame),
               ClientAsset.ResourceTexture.CODEC.fieldOf("angry").forGetter(WolfVariant.AssetInfo::angry)
            )
            .apply(p_421838_, WolfVariant.AssetInfo::new)
      );
   }
}
