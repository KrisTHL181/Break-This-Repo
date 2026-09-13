package net.minecraft.world.damagesource;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public record FallLocation(String id) {
   public static final FallLocation GENERIC = new FallLocation("generic");
   public static final FallLocation LADDER = new FallLocation("ladder");
   public static final FallLocation VINES = new FallLocation("vines");
   public static final FallLocation WEEPING_VINES = new FallLocation("weeping_vines");
   public static final FallLocation TWISTING_VINES = new FallLocation("twisting_vines");
   public static final FallLocation SCAFFOLDING = new FallLocation("scaffolding");
   public static final FallLocation OTHER_CLIMBABLE = new FallLocation("other_climbable");
   public static final FallLocation WATER = new FallLocation("water");

   public static FallLocation blockToFallLocation(final BlockState blockState) {
      if (blockState.is(Blocks.LADDER) || blockState.is(BlockTags.TRAPDOORS)) {
         return LADDER;
      } else if (blockState.is(Blocks.VINE)) {
         return VINES;
      } else if (blockState.is(Blocks.WEEPING_VINES) || blockState.is(Blocks.WEEPING_VINES_PLANT)) {
         return WEEPING_VINES;
      } else if (blockState.is(Blocks.TWISTING_VINES) || blockState.is(Blocks.TWISTING_VINES_PLANT)) {
         return TWISTING_VINES;
      } else {
         return blockState.is(Blocks.SCAFFOLDING) ? SCAFFOLDING : OTHER_CLIMBABLE;
      }
   }

   public static @Nullable FallLocation getCurrentFallLocation(final LivingEntity mob) {
      Optional<BlockPos> lastClimbablePos = mob.getLastClimbablePos();
      if (lastClimbablePos.isPresent()) {
         BlockState blockState = mob.level().getBlockState(lastClimbablePos.get());
         return blockToFallLocation(blockState);
      } else {
         return mob.isInWater() ? WATER : null;
      }
   }

   public String languageKey() {
      return "death.fell.accident." + this.id;
   }
}
