package net.minecraft.client.renderer;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public enum FaceInfo {
   DOWN(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z)
   ),
   UP(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z)
   ),
   NORTH(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z)
   ),
   SOUTH(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z)
   ),
   WEST(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z)
   ),
   EAST(
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
      new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z)
   );

   private static final Map<Direction, FaceInfo> BY_FACING = Util.make(new EnumMap<>(Direction.class), p_448148_ -> {
      p_448148_.put(Direction.DOWN, DOWN);
      p_448148_.put(Direction.UP, UP);
      p_448148_.put(Direction.NORTH, NORTH);
      p_448148_.put(Direction.SOUTH, SOUTH);
      p_448148_.put(Direction.WEST, WEST);
      p_448148_.put(Direction.EAST, EAST);
   });
   private final FaceInfo.VertexInfo[] infos;

   public static FaceInfo fromFacing(Direction p_108985_) {
      return BY_FACING.get(p_108985_);
   }

   FaceInfo(final FaceInfo.VertexInfo... p_108981_) {
      this.infos = p_108981_;
   }

   public FaceInfo.VertexInfo getVertexInfo(int p_108983_) {
      return this.infos[p_108983_];
   }

   @OnlyIn(Dist.CLIENT)
   public enum Extent {
      MIN_X,
      MIN_Y,
      MIN_Z,
      MAX_X,
      MAX_Y,
      MAX_Z;

      public float select(Vector3fc p_455488_, Vector3fc p_454448_) {
         return switch (this) {
            case MIN_X -> p_455488_.x();
            case MIN_Y -> p_455488_.y();
            case MIN_Z -> p_455488_.z();
            case MAX_X -> p_454448_.x();
            case MAX_Y -> p_454448_.y();
            case MAX_Z -> p_454448_.z();
         };
      }

      public float select(float p_456230_, float p_460437_, float p_458288_, float p_451081_, float p_451867_, float p_452341_) {
         return switch (this) {
            case MIN_X -> p_456230_;
            case MIN_Y -> p_460437_;
            case MIN_Z -> p_458288_;
            case MAX_X -> p_451081_;
            case MAX_Y -> p_451867_;
            case MAX_Z -> p_452341_;
         };
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record VertexInfo(FaceInfo.Extent xFace, FaceInfo.Extent yFace, FaceInfo.Extent zFace) {
      public Vector3f select(Vector3fc p_454828_, Vector3fc p_451997_) {
         return new Vector3f(this.xFace.select(p_454828_, p_451997_), this.yFace.select(p_454828_, p_451997_), this.zFace.select(p_454828_, p_451997_));
      }
   }
}
