package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public record BlockItemStateProperties(Map<String, String> properties) implements TooltipProvider {
   public static final BlockItemStateProperties EMPTY = new BlockItemStateProperties(Map.of());
   public static final Codec<BlockItemStateProperties> CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING)
      .xmap(BlockItemStateProperties::new, BlockItemStateProperties::properties);
   private static final StreamCodec<ByteBuf, Map<String, String>> PROPERTIES_STREAM_CODEC = ByteBufCodecs.map(
      Object2ObjectOpenHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8
   );
   public static final StreamCodec<ByteBuf, BlockItemStateProperties> STREAM_CODEC = PROPERTIES_STREAM_CODEC.map(
      BlockItemStateProperties::new, BlockItemStateProperties::properties
   );

   public <T extends Comparable<T>> BlockItemStateProperties with(Property<T> p_334707_, T p_329394_) {
      return new BlockItemStateProperties(Util.copyAndPut(this.properties, p_334707_.getName(), p_334707_.getName(p_329394_)));
   }

   public <T extends Comparable<T>> BlockItemStateProperties with(Property<T> p_332443_, BlockState p_334050_) {
      return this.with(p_332443_, p_334050_.getValue(p_332443_));
   }

   public <T extends Comparable<T>> @Nullable T get(Property<T> p_329754_) {
      String s = this.properties.get(p_329754_.getName());
      return s == null ? null : p_329754_.getValue(s).orElse(null);
   }

   public BlockState apply(BlockState p_330089_) {
      StateDefinition<Block, BlockState> statedefinition = p_330089_.getBlock().getStateDefinition();

      for (Entry<String, String> entry : this.properties.entrySet()) {
         Property<?> property = statedefinition.getProperty(entry.getKey());
         if (property != null) {
            p_330089_ = updateState(p_330089_, property, entry.getValue());
         }
      }

      return p_330089_;
   }

   private static <T extends Comparable<T>> BlockState updateState(BlockState p_335297_, Property<T> p_336285_, String p_328779_) {
      return p_336285_.getValue(p_328779_).map(p_359808_ -> p_335297_.setValue(p_336285_, p_359808_)).orElse(p_335297_);
   }

   public boolean isEmpty() {
      return this.properties.isEmpty();
   }

   @Override
   public void addToTooltip(Item.TooltipContext p_397198_, Consumer<Component> p_396525_, TooltipFlag p_391403_, DataComponentGetter p_394586_) {
      Integer integer = this.get(BeehiveBlock.HONEY_LEVEL);
      if (integer != null) {
         p_396525_.accept(Component.translatable("container.beehive.honey", integer, 5).withStyle(ChatFormatting.GRAY));
      }
   }
}
