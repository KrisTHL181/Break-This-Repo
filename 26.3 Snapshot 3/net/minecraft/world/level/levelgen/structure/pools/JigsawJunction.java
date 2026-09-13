package net.minecraft.world.level.levelgen.structure.pools;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;

public class JigsawJunction {
   private final int sourceX;
   private final int sourceGroundY;
   private final int sourceZ;
   private final int deltaY;
   private final StructureTemplatePool.Projection destProjection;

   public JigsawJunction(final int sourceX, final int sourceGroundY, final int sourceZ, final int deltaY, final StructureTemplatePool.Projection destProjection) {
      this.sourceX = sourceX;
      this.sourceGroundY = sourceGroundY;
      this.sourceZ = sourceZ;
      this.deltaY = deltaY;
      this.destProjection = destProjection;
   }

   public int getSourceX() {
      return this.sourceX;
   }

   public int getSourceGroundY() {
      return this.sourceGroundY;
   }

   public int getSourceZ() {
      return this.sourceZ;
   }

   public int getDeltaY() {
      return this.deltaY;
   }

   public StructureTemplatePool.Projection getDestProjection() {
      return this.destProjection;
   }

   public <T> Dynamic<T> serialize(final DynamicOps<T> ops) {
      Builder<T, T> builder = ImmutableMap.builder();
      builder.put(ops.createString("source_x"), ops.createInt(this.sourceX))
         .put(ops.createString("source_ground_y"), ops.createInt(this.sourceGroundY))
         .put(ops.createString("source_z"), ops.createInt(this.sourceZ))
         .put(ops.createString("delta_y"), ops.createInt(this.deltaY))
         .put(ops.createString("dest_proj"), ops.createString(this.destProjection.getName()));
      return new Dynamic(ops, ops.createMap(builder.build()));
   }

   public static <T> JigsawJunction deserialize(final Dynamic<T> input) {
      return new JigsawJunction(
         input.get("source_x").asInt(0),
         input.get("source_ground_y").asInt(0),
         input.get("source_z").asInt(0),
         input.get("delta_y").asInt(0),
         StructureTemplatePool.Projection.byName(input.get("dest_proj").asString(""))
      );
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }

      if (o != null && this.getClass() == o.getClass()) {
         JigsawJunction that = (JigsawJunction)o;
         if (this.sourceX != that.sourceX) {
            return false;
         } else if (this.sourceZ != that.sourceZ) {
            return false;
         } else {
            return this.deltaY != that.deltaY ? false : this.destProjection == that.destProjection;
         }
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.sourceX;
      result = 31 * result + this.sourceGroundY;
      result = 31 * result + this.sourceZ;
      result = 31 * result + this.deltaY;
      return 31 * result + this.destProjection.hashCode();
   }

   @Override
   public String toString() {
      return "JigsawJunction{sourceX="
         + this.sourceX
         + ", sourceGroundY="
         + this.sourceGroundY
         + ", sourceZ="
         + this.sourceZ
         + ", deltaY="
         + this.deltaY
         + ", destProjection="
         + this.destProjection
         + "}";
   }
}
