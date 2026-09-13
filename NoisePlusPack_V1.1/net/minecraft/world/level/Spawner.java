package net.minecraft.world.level;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

public interface Spawner {
   void setEntityId(EntityType<?> var1, RandomSource var2);

   static void appendHoverText(@Nullable TypedEntityData<BlockEntityType<?>> p_429083_, Consumer<Component> p_394104_, String p_310819_) {
      Component component = getSpawnEntityDisplayName(p_429083_, p_310819_);
      if (component != null) {
         p_394104_.accept(component);
      } else {
         p_394104_.accept(CommonComponents.EMPTY);
         p_394104_.accept(Component.translatable("block.minecraft.spawner.desc1").withStyle(ChatFormatting.GRAY));
         p_394104_.accept(CommonComponents.space().append(Component.translatable("block.minecraft.spawner.desc2").withStyle(ChatFormatting.BLUE)));
      }
   }

   static @Nullable Component getSpawnEntityDisplayName(@Nullable TypedEntityData<BlockEntityType<?>> p_427322_, String p_309907_) {
      return p_427322_ == null
         ? null
         : p_427322_.getUnsafe()
            .getCompound(p_309907_)
            .flatMap(p_390886_ -> p_390886_.getCompound("entity"))
            .flatMap(p_390887_ -> p_390887_.read("id", EntityType.CODEC))
            .map(p_311493_ -> Component.translatable(p_311493_.getDescriptionId()).withStyle(ChatFormatting.GRAY))
            .orElse(null);
   }
}
