package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;

public class JigsawRotationFix extends AbstractBlockPropertyFix {
   private static final Map<String, String> RENAMES = ImmutableMap.builder()
      .put("down", "down_south")
      .put("up", "up_north")
      .put("north", "north_up")
      .put("south", "south_up")
      .put("west", "west_up")
      .put("east", "east_up")
      .build();

   public JigsawRotationFix(Schema p_16191_) {
      super(p_16191_, "jigsaw_rotation_fix");
   }

   @Override
   protected boolean shouldFix(String p_397701_) {
      return p_397701_.equals("minecraft:jigsaw");
   }

   @Override
   protected <T> Dynamic<T> fixProperties(String p_391845_, Dynamic<T> p_397264_) {
      String s = p_397264_.get("facing").asString("north");
      return p_397264_.remove("facing").set("orientation", p_397264_.createString(RENAMES.getOrDefault(s, s)));
   }
}
