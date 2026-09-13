package net.minecraft.client.model.geom.builders;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;

public class CubeListBuilder {
   private static final Set<Direction> ALL_VISIBLE = EnumSet.allOf(Direction.class);
   private final List<CubeDefinition> cubes = Lists.newArrayList();
   private int xTexOffs;
   private int yTexOffs;
   private boolean mirror;

   public CubeListBuilder texOffs(final int xTexOffs, final int yTexOffs) {
      this.xTexOffs = xTexOffs;
      this.yTexOffs = yTexOffs;
      return this;
   }

   public CubeListBuilder mirror() {
      return this.mirror(true);
   }

   public CubeListBuilder mirror(final boolean mirror) {
      this.mirror = mirror;
      return this;
   }

   public CubeListBuilder addBox(
      final String id,
      final float x0,
      final float y0,
      final float z0,
      final int w,
      final int h,
      final int d,
      final CubeDeformation g,
      final int xTexOffs,
      final int yTexOffs
   ) {
      this.texOffs(xTexOffs, yTexOffs);
      this.cubes.add(new CubeDefinition(id, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, g, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
      return this;
   }

   public CubeListBuilder addBox(
      final String id, final float x0, final float y0, final float z0, final int w, final int h, final int d, final int xTexOffs, final int yTexOffs
   ) {
      this.texOffs(xTexOffs, yTexOffs);
      this.cubes.add(new CubeDefinition(id, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, CubeDeformation.NONE, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
      return this;
   }

   public CubeListBuilder addBox(final float x0, final float y0, final float z0, final float w, final float h, final float d) {
      this.cubes.add(new CubeDefinition(null, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, CubeDeformation.NONE, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
      return this;
   }

   public CubeListBuilder addBox(final float x0, final float y0, final float z0, final float w, final float h, final float d, final Set<Direction> visibleSides) {
      this.cubes.add(new CubeDefinition(null, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, CubeDeformation.NONE, this.mirror, 1.0F, 1.0F, visibleSides));
      return this;
   }

   public CubeListBuilder addBox(final String id, final float x0, final float y0, final float z0, final float w, final float h, final float d) {
      this.cubes.add(new CubeDefinition(id, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, CubeDeformation.NONE, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
      return this;
   }

   public CubeListBuilder addBox(
      final String id, final float x0, final float y0, final float z0, final float w, final float h, final float d, final CubeDeformation g
   ) {
      this.cubes.add(new CubeDefinition(id, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, g, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
      return this;
   }

   public CubeListBuilder addBox(final float x0, final float y0, final float z0, final float w, final float h, final float d, final boolean mirror) {
      this.cubes.add(new CubeDefinition(null, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, CubeDeformation.NONE, mirror, 1.0F, 1.0F, ALL_VISIBLE));
      return this;
   }

   public CubeListBuilder addBox(
      final float x0,
      final float y0,
      final float z0,
      final float w,
      final float h,
      final float d,
      final CubeDeformation g,
      final float xTexScale,
      final float yTexScale
   ) {
      this.cubes.add(new CubeDefinition(null, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, g, this.mirror, xTexScale, yTexScale, ALL_VISIBLE));
      return this;
   }

   public CubeListBuilder addBox(final float x0, final float y0, final float z0, final float w, final float h, final float d, final CubeDeformation g) {
      this.cubes.add(new CubeDefinition(null, this.xTexOffs, this.yTexOffs, x0, y0, z0, w, h, d, g, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
      return this;
   }

   public List<CubeDefinition> getCubes() {
      return ImmutableList.copyOf(this.cubes);
   }

   public static CubeListBuilder create() {
      return new CubeListBuilder();
   }
}
