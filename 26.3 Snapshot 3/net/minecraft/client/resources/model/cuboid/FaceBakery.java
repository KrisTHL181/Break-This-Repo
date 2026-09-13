package net.minecraft.client.resources.model.cuboid;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.platform.Transparency;
import com.mojang.math.MatrixUtil;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import java.util.Objects;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.joml.GeometryUtils;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class FaceBakery {
   private static final Vector3fc BLOCK_MIDDLE = new Vector3f(0.5F, 0.5F, 0.5F);

   @VisibleForTesting
   static CuboidFace.UVs defaultFaceUV(final Vector3fc from, final Vector3fc to, final Direction facing) {
      return switch (facing) {
         case DOWN -> new CuboidFace.UVs(from.x(), 16.0F - to.z(), to.x(), 16.0F - from.z());
         case UP -> new CuboidFace.UVs(from.x(), from.z(), to.x(), to.z());
         case NORTH -> new CuboidFace.UVs(16.0F - to.x(), 16.0F - to.y(), 16.0F - from.x(), 16.0F - from.y());
         case SOUTH -> new CuboidFace.UVs(from.x(), 16.0F - to.y(), to.x(), 16.0F - from.y());
         case WEST -> new CuboidFace.UVs(from.z(), 16.0F - to.y(), to.z(), 16.0F - from.y());
         case EAST -> new CuboidFace.UVs(16.0F - to.z(), 16.0F - to.y(), 16.0F - from.z(), 16.0F - from.y());
      };
   }

   private static Transparency computeMaterialTransparency(final Material.Baked material, final CuboidFace.UVs uvs) {
      return material.forceTranslucent()
         ? Transparency.TRANSLUCENT
         : material.sprite()
            .contents()
            .computeTransparency(
               Math.min(uvs.minU(), uvs.maxU()) / 16.0F,
               Math.min(uvs.minV(), uvs.maxV()) / 16.0F,
               Math.max(uvs.minU(), uvs.maxU()) / 16.0F,
               Math.max(uvs.minV(), uvs.maxV()) / 16.0F
            );
   }

   public static BakedQuad bakeQuad(
      final ModelBaker modelBaker,
      final Vector3fc from,
      final Vector3fc to,
      final CuboidFace face,
      final Material.Baked material,
      final Direction facing,
      final ModelState modelState,
      final @Nullable CuboidRotation elementRotation,
      final @Nullable Direction shadeDirectionOverride,
      final int lightEmission
   ) {
      CuboidFace.UVs uvs = face.uvs();
      if (uvs == null) {
         uvs = defaultFaceUV(from, to, facing);
      }

      Transparency transparency = computeMaterialTransparency(material, uvs);
      ModelBaker.Interner interner = modelBaker.interner();
      BakedQuad.MaterialInfo materialInfo = interner.materialInfo(
         BakedQuad.MaterialInfo.of(material, transparency, face.tintIndex(), shadeDirectionOverride, lightEmission)
      );
      return bakeQuad(interner, from, to, uvs, face.rotation(), materialInfo, facing, modelState, elementRotation);
   }

   public static BakedQuad bakeQuad(
      final ModelBaker.Interner interner,
      final Vector3fc from,
      final Vector3fc to,
      final CuboidFace.UVs uvs,
      final Quadrant uvRotation,
      final BakedQuad.MaterialInfo materialInfo,
      final Direction facing,
      final ModelState modelState,
      final @Nullable CuboidRotation elementRotation
   ) {
      Matrix4fc uvTransform = modelState.inverseFaceTransformation(facing);
      Vector3fc[] vertexPositions = new Vector3fc[4];
      long[] vertexPackedUvs = new long[4];
      FaceInfo faceInfo = FaceInfo.fromFacing(facing);

      for (int i = 0; i < 4; i++) {
         bakeVertex(
            i,
            faceInfo,
            uvs,
            uvRotation,
            uvTransform,
            from,
            to,
            materialInfo,
            modelState.transformation(),
            elementRotation,
            vertexPositions,
            vertexPackedUvs,
            interner
         );
      }

      Direction finalDirection = calculateFacing(vertexPositions);
      if (elementRotation == null && finalDirection != null) {
         recalculateWinding(vertexPositions, vertexPackedUvs, finalDirection);
      }

      return new BakedQuad(
         vertexPositions[0],
         vertexPositions[1],
         vertexPositions[2],
         vertexPositions[3],
         vertexPackedUvs[0],
         vertexPackedUvs[1],
         vertexPackedUvs[2],
         vertexPackedUvs[3],
         Objects.requireNonNullElse(finalDirection, Direction.UP),
         materialInfo
      );
   }

   private static void bakeVertex(
      final int index,
      final FaceInfo faceInfo,
      final CuboidFace.UVs uvs,
      final Quadrant uvRotation,
      final Matrix4fc uvTransform,
      final Vector3fc from,
      final Vector3fc to,
      final BakedQuad.MaterialInfo materialInfo,
      final Transformation rotation,
      final @Nullable CuboidRotation elementRotation,
      final Vector3fc[] positionOutput,
      final long[] uvOutput,
      final ModelBaker.Interner interner
   ) {
      FaceInfo.VertexInfo vertexInfo = faceInfo.getVertexInfo(index);
      Vector3f vertex = vertexInfo.select(from, to).div(16.0F);
      if (elementRotation != null) {
         rotateVertexBy(vertex, elementRotation.origin(), elementRotation.transform());
      }

      if (rotation != Transformation.IDENTITY) {
         rotateVertexBy(vertex, BLOCK_MIDDLE, rotation.getMatrix());
      }

      float rawU = CuboidFace.getU(uvs, uvRotation, index);
      float rawV = CuboidFace.getV(uvs, uvRotation, index);
      float transformedV;
      float transformedU;
      if (MatrixUtil.isIdentity(uvTransform)) {
         transformedU = rawU;
         transformedV = rawV;
      } else {
         Vector3f transformedUV = uvTransform.transformPosition(new Vector3f(cornerToCenter(rawU), cornerToCenter(rawV), 0.0F));
         transformedU = centerToCorner(transformedUV.x);
         transformedV = centerToCorner(transformedUV.y);
      }

      positionOutput[index] = interner.vector(vertex);
      uvOutput[index] = UVPair.pack(materialInfo.sprite().getU(transformedU), materialInfo.sprite().getV(transformedV));
   }

   private static float cornerToCenter(final float value) {
      return value - 0.5F;
   }

   private static float centerToCorner(final float value) {
      return value + 0.5F;
   }

   private static void rotateVertexBy(final Vector3f vertex, final Vector3fc origin, final Matrix4fc transformation) {
      vertex.sub(origin);
      transformation.transformPosition(vertex);
      vertex.add(origin);
   }

   private static @Nullable Direction calculateFacing(final Vector3fc[] positions) {
      Vector3f normal = new Vector3f();
      GeometryUtils.normal(positions[0], positions[1], positions[2], normal);
      return findClosestDirection(normal);
   }

   private static @Nullable Direction findClosestDirection(final Vector3f direction) {
      if (!direction.isFinite()) {
         return null;
      }

      Direction best = null;
      float closestProduct = 0.0F;

      for (Direction candidate : Direction.values()) {
         float product = direction.dot(candidate.getUnitVec3f());
         if (product >= 0.0F && product > closestProduct) {
            closestProduct = product;
            best = candidate;
         }
      }

      return best;
   }

   private static void recalculateWinding(final Vector3fc[] positions, final long[] uvs, final Direction direction) {
      float minX = 999.0F;
      float minY = 999.0F;
      float minZ = 999.0F;
      float maxX = -999.0F;
      float maxY = -999.0F;
      float maxZ = -999.0F;

      for (int i = 0; i < 4; i++) {
         Vector3fc position = positions[i];
         float x = position.x();
         float y = position.y();
         float z = position.z();
         if (x < minX) {
            minX = x;
         }

         if (y < minY) {
            minY = y;
         }

         if (z < minZ) {
            minZ = z;
         }

         if (x > maxX) {
            maxX = x;
         }

         if (y > maxY) {
            maxY = y;
         }

         if (z > maxZ) {
            maxZ = z;
         }
      }

      FaceInfo info = FaceInfo.fromFacing(direction);

      for (int vertex = 0; vertex < 4; vertex++) {
         FaceInfo.VertexInfo vertInfo = info.getVertexInfo(vertex);
         float newX = vertInfo.xFace().select(minX, minY, minZ, maxX, maxY, maxZ);
         float newY = vertInfo.yFace().select(minX, minY, minZ, maxX, maxY, maxZ);
         float newZ = vertInfo.zFace().select(minX, minY, minZ, maxX, maxY, maxZ);
         int vertexToSwap = findVertex(positions, vertex, newX, newY, newZ);
         if (vertexToSwap == -1) {
            throw new IllegalStateException("Can't find vertex to swap");
         }

         if (vertexToSwap != vertex) {
            swap(positions, vertexToSwap, vertex);
            swap(uvs, vertexToSwap, vertex);
         }
      }
   }

   private static int findVertex(final Vector3fc[] positions, final int start, final float x, final float y, final float z) {
      for (int i = start; i < 4; i++) {
         Vector3fc position = positions[i];
         if (x == position.x() && y == position.y() && z == position.z()) {
            return i;
         }
      }

      return -1;
   }

   private static void swap(final Vector3fc[] array, final int indexA, final int indexB) {
      Vector3fc tmp = array[indexA];
      array[indexA] = array[indexB];
      array[indexB] = tmp;
   }

   private static void swap(final long[] array, final int indexA, final int indexB) {
      long tmp = array[indexA];
      array[indexA] = array[indexB];
      array[indexB] = tmp;
   }
}
