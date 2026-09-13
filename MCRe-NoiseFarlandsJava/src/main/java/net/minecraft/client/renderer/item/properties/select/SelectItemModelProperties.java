package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SelectItemModelProperties {
    private static final ExtraCodecs.LateBoundIdMapper<Identifier, SelectItemModelProperty.Type<?, ?>> ID_MAPPER = new ExtraCodecs.LateBoundIdMapper<>();
    public static final Codec<SelectItemModelProperty.Type<?, ?>> CODEC = ID_MAPPER.codec(Identifier.CODEC);

    public static void bootstrap() {
        ID_MAPPER.put(Identifier.withDefaultNamespace("custom_model_data"), CustomModelDataProperty.TYPE);
        ID_MAPPER.put(Identifier.withDefaultNamespace("main_hand"), MainHand.TYPE);
        ID_MAPPER.put(Identifier.withDefaultNamespace("charge_type"), Charge.TYPE);
        ID_MAPPER.put(Identifier.withDefaultNamespace("trim_material"), TrimMaterialProperty.TYPE);
        ID_MAPPER.put(Identifier.withDefaultNamespace("block_state"), ItemBlockState.TYPE);
        ID_MAPPER.put(Identifier.withDefaultNamespace("display_context"), DisplayContext.TYPE);
        ID_MAPPER.put(Identifier.withDefaultNamespace("local_time"), LocalTime.TYPE);
        ID_MAPPER.put(Identifier.withDefaultNamespace("context_entity_type"), ContextEntityType.TYPE);
        ID_MAPPER.put(Identifier.withDefaultNamespace("context_dimension"), ContextDimension.TYPE);
        ID_MAPPER.put(Identifier.withDefaultNamespace("component"), ComponentContents.castType());
    }
}