package net.minecraft.world.item.equipment.trim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.equipment.EquipmentAsset;

public record ArmorTrim(Holder<TrimMaterial> material, Holder<TrimPattern> pattern) implements TooltipProvider {
   public static final Codec<ArmorTrim> CODEC = RecordCodecBuilder.create(
      p_390858_ -> p_390858_.group(
            TrimMaterial.CODEC.fieldOf("material").forGetter(ArmorTrim::material), TrimPattern.CODEC.fieldOf("pattern").forGetter(ArmorTrim::pattern)
         )
         .apply(p_390858_, ArmorTrim::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ArmorTrim> STREAM_CODEC = StreamCodec.composite(
      TrimMaterial.STREAM_CODEC, ArmorTrim::material, TrimPattern.STREAM_CODEC, ArmorTrim::pattern, ArmorTrim::new
   );
   private static final Component UPGRADE_TITLE = Component.translatable(
         Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.upgrade"))
      )
      .withStyle(ChatFormatting.GRAY);

   @Override
   public void addToTooltip(Item.TooltipContext p_360931_, Consumer<Component> p_367392_, TooltipFlag p_368625_, DataComponentGetter p_392266_) {
      p_367392_.accept(UPGRADE_TITLE);
      p_367392_.accept(CommonComponents.space().append(this.pattern.value().copyWithStyle(this.material)));
      p_367392_.accept(CommonComponents.space().append(this.material.value().description()));
   }

   public Identifier layerAssetId(String p_391784_, ResourceKey<EquipmentAsset> p_397619_) {
      MaterialAssetGroup.AssetInfo materialassetgroup$assetinfo = this.material().value().assets().assetId(p_397619_);
      return this.pattern().value().assetId().withPath(p_390861_ -> p_391784_ + "/" + p_390861_ + "_" + materialassetgroup$assetinfo.suffix());
   }
}
