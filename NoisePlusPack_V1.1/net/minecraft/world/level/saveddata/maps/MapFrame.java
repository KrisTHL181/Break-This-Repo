package net.minecraft.world.level.saveddata.maps;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record MapFrame(BlockPos pos, int rotation, int entityId) {
   public static final Codec<MapFrame> CODEC = RecordCodecBuilder.create(
      p_393606_ -> p_393606_.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(MapFrame::pos),
            Codec.INT.fieldOf("rotation").forGetter(MapFrame::rotation),
            Codec.INT.fieldOf("entity_id").forGetter(MapFrame::entityId)
         )
         .apply(p_393606_, MapFrame::new)
   );

   public String getId() {
      return frameId(this.pos);
   }

   public static String frameId(BlockPos p_77871_) {
      return "frame-" + p_77871_.getX() + "," + p_77871_.getY() + "," + p_77871_.getZ();
   }
}
