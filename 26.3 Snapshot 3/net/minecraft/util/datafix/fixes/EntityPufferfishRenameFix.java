package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;
import java.util.Objects;

public class EntityPufferfishRenameFix extends SimplestEntityRenameFix {
   public static final Map<String, String> RENAMED_IDS = ImmutableMap.builder()
      .put("minecraft:puffer_fish_spawn_egg", "minecraft:pufferfish_spawn_egg")
      .build();

   public EntityPufferfishRenameFix(final Schema outputSchema, final boolean changesType) {
      super("EntityPufferfishRenameFix", outputSchema, changesType);
   }

   @Override
   protected String rename(final String name) {
      return Objects.equals("minecraft:puffer_fish", name) ? "minecraft:pufferfish" : name;
   }
}
