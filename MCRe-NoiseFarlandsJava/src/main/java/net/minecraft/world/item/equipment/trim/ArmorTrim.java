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
        i -> i.group(TrimMaterial.CODEC.fieldOf("material").forGetter(ArmorTrim::material), TrimPattern.CODEC.fieldOf("pattern").forGetter(ArmorTrim::pattern))
            .apply(i, ArmorTrim::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ArmorTrim> STREAM_CODEC = StreamCodec.composite(
        TrimMaterial.STREAM_CODEC, ArmorTrim::material, TrimPattern.STREAM_CODEC, ArmorTrim::pattern, ArmorTrim::new
    );
    private static final Component UPGRADE_TITLE = Component.translatable(
            Util.makeDescriptionId("item", Identifier.withDefaultNamespace("smithing_template.upgrade"))
        )
        .withStyle(ChatFormatting.GRAY);

    @Override
    public void addToTooltip(
        final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components
    ) {
        consumer.accept(UPGRADE_TITLE);
        consumer.accept(CommonComponents.space().append(this.pattern.value().copyWithStyle(this.material)));
        consumer.accept(CommonComponents.space().append(this.material.value().description()));
    }

    public Identifier layerAssetId(final String layerAssetPrefix, final ResourceKey<EquipmentAsset> equipmentAsset) {
        MaterialAssetGroup.AssetInfo materialAsset = this.material().value().assets().assetId(equipmentAsset);
        return this.pattern().value().assetId().withPath(patternPath -> layerAssetPrefix + "/" + patternPath + "_" + materialAsset.suffix());
    }
}