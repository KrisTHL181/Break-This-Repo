package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

public record LodestoneTracker(Optional<GlobalPos> target, boolean tracked) {
   public static final Codec<LodestoneTracker> CODEC = RecordCodecBuilder.create(
      p_328600_ -> p_328600_.group(
            GlobalPos.CODEC.optionalFieldOf("target").forGetter(LodestoneTracker::target),
            Codec.BOOL.optionalFieldOf("tracked", true).forGetter(LodestoneTracker::tracked)
         )
         .apply(p_328600_, LodestoneTracker::new)
   );
   public static final StreamCodec<ByteBuf, LodestoneTracker> STREAM_CODEC = StreamCodec.composite(
      GlobalPos.STREAM_CODEC.apply(ByteBufCodecs::optional), LodestoneTracker::target, ByteBufCodecs.BOOL, LodestoneTracker::tracked, LodestoneTracker::new
   );

   public LodestoneTracker tick(ServerLevel p_333312_) {
      if (this.tracked && !this.target.isEmpty()) {
         if (this.target.get().dimension() != p_333312_.dimension()) {
            return this;
         }

         BlockPos blockpos = this.target.get().pos();
         return p_333312_.isInWorldBounds(blockpos) && p_333312_.getPoiManager().existsAtPosition(PoiTypes.LODESTONE, blockpos)
            ? this
            : new LodestoneTracker(Optional.empty(), true);
      } else {
         return this;
      }
   }
}
