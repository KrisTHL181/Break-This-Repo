package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record HasComponent(DataComponentType<?> componentType, boolean ignoreDefault) implements ConditionalItemModelProperty {
   public static final MapCodec<HasComponent> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_377625_ -> p_377625_.group(
            BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec().fieldOf("component").forGetter(HasComponent::componentType),
            Codec.BOOL.optionalFieldOf("ignore_default", false).forGetter(HasComponent::ignoreDefault)
         )
         .apply(p_377625_, HasComponent::new)
   );

   @Override
   public boolean get(ItemStack p_376045_, @Nullable ClientLevel p_376454_, @Nullable LivingEntity p_377963_, int p_375694_, ItemDisplayContext p_375575_) {
      return this.ignoreDefault ? p_376045_.hasNonDefault(this.componentType) : p_376045_.has(this.componentType);
   }

   @Override
   public MapCodec<HasComponent> type() {
      return MAP_CODEC;
   }
}
