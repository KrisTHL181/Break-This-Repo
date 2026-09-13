package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record IsKeybindDown(KeyMapping keybind) implements ConditionalItemModelProperty {
   private static final Codec<KeyMapping> KEYBIND_CODEC = Codec.STRING.comapFlatMap(p_378188_ -> {
      KeyMapping keymapping = KeyMapping.get(p_378188_);
      return keymapping != null ? DataResult.success(keymapping) : DataResult.error(() -> "Invalid keybind: " + p_378188_);
   }, KeyMapping::getName);
   public static final MapCodec<IsKeybindDown> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_377660_ -> p_377660_.group(KEYBIND_CODEC.fieldOf("keybind").forGetter(IsKeybindDown::keybind)).apply(p_377660_, IsKeybindDown::new)
   );

   @Override
   public boolean get(ItemStack p_378551_, @Nullable ClientLevel p_378144_, @Nullable LivingEntity p_377945_, int p_377968_, ItemDisplayContext p_378715_) {
      return this.keybind.isDown();
   }

   @Override
   public MapCodec<IsKeybindDown> type() {
      return MAP_CODEC;
   }
}
