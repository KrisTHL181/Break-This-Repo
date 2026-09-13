package net.minecraft.client.renderer.item.properties.select;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record ItemBlockState(String property) implements SelectItemModelProperty<String> {
   public static final PrimitiveCodec<String> VALUE_CODEC = Codec.STRING;
   public static final SelectItemModelProperty.Type<ItemBlockState, String> TYPE = SelectItemModelProperty.Type.create(
      RecordCodecBuilder.mapCodec(
         p_378781_ -> p_378781_.group(Codec.STRING.fieldOf("block_state_property").forGetter(ItemBlockState::property)).apply(p_378781_, ItemBlockState::new)
      ),
      VALUE_CODEC
   );

   public @Nullable String get(
      ItemStack p_375813_, @Nullable ClientLevel p_378420_, @Nullable LivingEntity p_377910_, int p_376430_, ItemDisplayContext p_376342_
   ) {
      BlockItemStateProperties blockitemstateproperties = p_375813_.get(DataComponents.BLOCK_STATE);
      return blockitemstateproperties == null ? null : blockitemstateproperties.properties().get(this.property);
   }

   @Override
   public SelectItemModelProperty.Type<ItemBlockState, String> type() {
      return TYPE;
   }

   @Override
   public Codec<String> valueCodec() {
      return VALUE_CODEC;
   }
}
