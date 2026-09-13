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
import org.jspecify.annotations.Nullable;

public record IsKeybindDown(KeyMapping keybind) implements ConditionalItemModelProperty {
   private static final Codec<KeyMapping> KEYBIND_CODEC = Codec.STRING.comapFlatMap(id -> {
      KeyMapping mapping = KeyMapping.get(id);
      return mapping != null ? DataResult.success(mapping) : DataResult.error(() -> "Invalid keybind: " + id);
   }, KeyMapping::getName);
   public static final MapCodec<IsKeybindDown> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(KEYBIND_CODEC.fieldOf("keybind").forGetter(IsKeybindDown::keybind)).apply(i, IsKeybindDown::new)
   );

   @Override
   public boolean get(
      final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable LivingEntity owner, final int seed, final ItemDisplayContext displayContext
   ) {
      return this.keybind.isDown();
   }

   @Override
   public MapCodec<IsKeybindDown> type() {
      return MAP_CODEC;
   }
}
