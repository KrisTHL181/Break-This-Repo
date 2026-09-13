package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;

public record Bees(List<BeehiveBlockEntity.Occupant> bees) implements TooltipProvider {
   public static final Codec<Bees> CODEC = BeehiveBlockEntity.Occupant.LIST_CODEC.xmap(Bees::new, Bees::bees);
   public static final StreamCodec<RegistryFriendlyByteBuf, Bees> STREAM_CODEC = BeehiveBlockEntity.Occupant.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(Bees::new, Bees::bees);
   public static final Bees EMPTY = new Bees(List.of());

   @Override
   public void addToTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components) {
      consumer.accept(Component.translatable("container.beehive.bees", this.bees.size(), 3).withStyle(ChatFormatting.GRAY));
   }
}
