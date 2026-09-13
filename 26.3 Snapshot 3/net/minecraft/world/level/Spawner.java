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
   void setEntityId(final EntityType<?> type, final RandomSource random);

   void setEntityData(final TypedEntityData<EntityType<?>> entityData, final RandomSource random);

   static void appendHoverText(final @Nullable TypedEntityData<BlockEntityType<?>> data, final Consumer<Component> consumer, final String nextSpawnDataTagKey) {
      Component displayName = getSpawnEntityDisplayName(data, nextSpawnDataTagKey);
      if (displayName != null) {
         consumer.accept(displayName);
      } else {
         consumer.accept(CommonComponents.EMPTY);
         consumer.accept(Component.translatable("block.minecraft.spawner.desc1").withStyle(ChatFormatting.GRAY));
         consumer.accept(CommonComponents.space().append(Component.translatable("block.minecraft.spawner.desc2").withStyle(ChatFormatting.BLUE)));
      }
   }

   static @Nullable Component getSpawnEntityDisplayName(final @Nullable TypedEntityData<BlockEntityType<?>> data, final String nextSpawnDataTagKey) {
      return data == null
         ? null
         : data.getUnsafe()
            .getCompound(nextSpawnDataTagKey)
            .flatMap(nextSpawnData -> nextSpawnData.getCompound("entity"))
            .flatMap(entityTag -> entityTag.read("id", EntityType.CODEC))
            .map(entityType -> Component.translatable(entityType.getDescriptionId()).withStyle(ChatFormatting.GRAY))
            .orElse(null);
   }
}
