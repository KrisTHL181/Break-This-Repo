package net.minecraft.world.level.block.sounds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record AmbientLeavesBlockSoundPlayer(
   Optional<Holder<SoundEvent>> ambientSound,
   int chance,
   Optional<TagKey<Block>> satisfyingBlocks,
   int nearbySatisfyingBlocksRequired,
   int nearbySameLeavesRequired
) {
   private static final int AMBIENT_SOUND_CHANCE = 300;
   private static final int NEARBY_SATISFYING_BLOCKS_REQUIRED = 1;
   private static final int NEARBY_SAME_LEAVES_REQUIRED = 3;
   public static final Codec<AmbientLeavesBlockSoundPlayer> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            SoundEvent.CODEC.optionalFieldOf("ambient_sound").forGetter(AmbientLeavesBlockSoundPlayer::ambientSound),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("chance", 300).forGetter(AmbientLeavesBlockSoundPlayer::chance),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("satisfying_blocks").forGetter(AmbientLeavesBlockSoundPlayer::satisfyingBlocks),
            ExtraCodecs.NON_NEGATIVE_INT
               .optionalFieldOf("nearby_satisfying_blocks_required", 1)
               .forGetter(AmbientLeavesBlockSoundPlayer::nearbySatisfyingBlocksRequired),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("nearby_same_leaves_required", 3).forGetter(AmbientLeavesBlockSoundPlayer::nearbySameLeavesRequired)
         )
         .apply(i, AmbientLeavesBlockSoundPlayer::new)
   );
   private static final AmbientLeavesBlockSoundPlayer NO_SOUND = new AmbientLeavesBlockSoundPlayer(Optional.empty(), 0, Optional.empty(), 0, 0);

   public static AmbientLeavesBlockSoundPlayer of(final Holder<SoundEvent> ambientSound, final TagKey<Block> satisfyingBlocks) {
      return new AmbientLeavesBlockSoundPlayer(Optional.of(ambientSound), 300, Optional.of(satisfyingBlocks), 1, 3);
   }

   public static AmbientLeavesBlockSoundPlayer noAmbientSound() {
      return NO_SOUND;
   }

   public void playAmbientLeavesSounds(final Level level, final BlockPos pos, final Block block, final RandomSource random) {
      if (this.ambientSound.isPresent() && random.nextInt(this.chance) == 0) {
         TagKey<Block> satisfyingBlocks = this.satisfyingBlocks.orElse(null);
         int nearbyLogs = 0;
         int nearbyLeaves = 0;

         for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (satisfyingBlocks != null && neighborState.is(satisfyingBlocks)) {
               nearbyLogs++;
            }

            if (neighborState.is(block)) {
               nearbyLeaves++;
            }

            if (nearbyLogs == this.nearbySatisfyingBlocksRequired && nearbyLeaves == this.nearbySameLeavesRequired) {
               level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), this.ambientSound.get().value(), SoundSource.AMBIENT, 1.0F, 1.0F, false);
               return;
            }
         }
      }
   }
}
