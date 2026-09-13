package net.minecraft.client.renderer.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record ClientItem(ItemModel.Unbaked model, ClientItem.Properties properties, @Nullable RegistryContextSwapper registrySwapper) {
    public static final Codec<ClientItem> CODEC = RecordCodecBuilder.create(
        i -> i.group(ItemModels.CODEC.fieldOf("model").forGetter(ClientItem::model), ClientItem.Properties.MAP_CODEC.forGetter(ClientItem::properties))
            .apply(i, ClientItem::new)
    );

    public ClientItem(final ItemModel.Unbaked model, final ClientItem.Properties properties) {
        this(model, properties, null);
    }

    public ClientItem withRegistrySwapper(final RegistryContextSwapper registrySwapper) {
        return new ClientItem(this.model, this.properties, registrySwapper);
    }

    @OnlyIn(Dist.CLIENT)
    public record Properties(boolean handAnimationOnSwap, boolean oversizedInGui, float swapAnimationScale) {
        public static final ClientItem.Properties DEFAULT = new ClientItem.Properties(true, false, 1.0F);
        public static final MapCodec<ClientItem.Properties> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.BOOL.optionalFieldOf("hand_animation_on_swap", true).forGetter(ClientItem.Properties::handAnimationOnSwap),
                    Codec.BOOL.optionalFieldOf("oversized_in_gui", false).forGetter(ClientItem.Properties::oversizedInGui),
                    Codec.FLOAT.optionalFieldOf("swap_animation_scale", 1.0F).forGetter(ClientItem.Properties::swapAnimationScale)
                )
                .apply(i, ClientItem.Properties::new)
        );
    }
}