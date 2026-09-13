package net.minecraft.world.phys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class BlockHitResult extends HitResult {
   private final Direction direction;
   private final BlockPos blockPos;
   private final boolean miss;
   private final boolean inside;
   private final boolean worldBorderHit;

   public static BlockHitResult miss(final Vec3 location, final Direction direction, final BlockPos pos) {
      return new BlockHitResult(true, location, direction, pos, false, false);
   }

   public BlockHitResult(final Vec3 location, final Direction direction, final BlockPos pos, final boolean inside) {
      this(false, location, direction, pos, inside, false);
   }

   public BlockHitResult(final Vec3 location, final Direction direction, final BlockPos pos, final boolean inside, final boolean worldBorderHit) {
      this(false, location, direction, pos, inside, worldBorderHit);
   }

   private BlockHitResult(
      final boolean miss, final Vec3 location, final Direction direction, final BlockPos blockPos, final boolean inside, final boolean worldBorderHit
   ) {
      super(location);
      this.miss = miss;
      this.direction = direction;
      this.blockPos = blockPos;
      this.inside = inside;
      this.worldBorderHit = worldBorderHit;
   }

   public BlockHitResult withDirection(final Direction direction) {
      return new BlockHitResult(this.miss, this.location, direction, this.blockPos, this.inside, this.worldBorderHit);
   }

   public BlockHitResult withPosition(final BlockPos blockPos) {
      return new BlockHitResult(this.miss, this.location, this.direction, blockPos, this.inside, this.worldBorderHit);
   }

   public BlockHitResult hitBorder() {
      return new BlockHitResult(this.miss, this.location, this.direction, this.blockPos, this.inside, true);
   }

   public BlockPos getBlockPos() {
      return this.blockPos;
   }

   public Direction getDirection() {
      return this.direction;
   }

   @Override
   public HitResult.Type getType() {
      return this.miss ? HitResult.Type.MISS : HitResult.Type.BLOCK;
   }

   public boolean isInside() {
      return this.inside;
   }

   public boolean isWorldBorderHit() {
      return this.worldBorderHit;
   }
}
