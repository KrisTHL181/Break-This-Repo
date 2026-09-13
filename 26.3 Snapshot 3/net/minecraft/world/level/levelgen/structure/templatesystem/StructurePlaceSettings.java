package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jspecify.annotations.Nullable;

public class StructurePlaceSettings {
   private Mirror mirror = Mirror.NONE;
   private Rotation rotation = Rotation.NONE;
   private BlockPos rotationPivot = BlockPos.ZERO;
   private boolean ignoreEntities;
   private @Nullable BoundingBox boundingBox;
   private LiquidSettings liquidSettings = LiquidSettings.APPLY_WATERLOGGING;
   private @Nullable RandomSource random;
   private int palette;
   private final List<StructureProcessor> processors = Lists.newArrayList();
   private boolean knownShape;
   private boolean finalizeEntities;

   public StructurePlaceSettings copy() {
      StructurePlaceSettings setting = new StructurePlaceSettings();
      setting.mirror = this.mirror;
      setting.rotation = this.rotation;
      setting.rotationPivot = this.rotationPivot;
      setting.ignoreEntities = this.ignoreEntities;
      setting.boundingBox = this.boundingBox;
      setting.liquidSettings = this.liquidSettings;
      setting.random = this.random;
      setting.palette = this.palette;
      setting.processors.addAll(this.processors);
      setting.knownShape = this.knownShape;
      setting.finalizeEntities = this.finalizeEntities;
      return setting;
   }

   public StructurePlaceSettings setMirror(final Mirror mirror) {
      this.mirror = mirror;
      return this;
   }

   public StructurePlaceSettings setRotation(final Rotation rotation) {
      this.rotation = rotation;
      return this;
   }

   public StructurePlaceSettings setRotationPivot(final BlockPos rotationPivot) {
      this.rotationPivot = rotationPivot;
      return this;
   }

   public StructurePlaceSettings setIgnoreEntities(final boolean ignoreEntities) {
      this.ignoreEntities = ignoreEntities;
      return this;
   }

   public StructurePlaceSettings setBoundingBox(final BoundingBox boundingBox) {
      this.boundingBox = boundingBox;
      return this;
   }

   public StructurePlaceSettings setRandom(final @Nullable RandomSource random) {
      this.random = random;
      return this;
   }

   public StructurePlaceSettings setLiquidSettings(final LiquidSettings liquidSettings) {
      this.liquidSettings = liquidSettings;
      return this;
   }

   public StructurePlaceSettings setKnownShape(final boolean knownShape) {
      this.knownShape = knownShape;
      return this;
   }

   public StructurePlaceSettings clearProcessors() {
      this.processors.clear();
      return this;
   }

   public StructurePlaceSettings addProcessor(final StructureProcessor processor) {
      this.processors.add(processor);
      return this;
   }

   public StructurePlaceSettings popProcessor(final StructureProcessor processor) {
      this.processors.remove(processor);
      return this;
   }

   public Mirror getMirror() {
      return this.mirror;
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public BlockPos getRotationPivot() {
      return this.rotationPivot;
   }

   public RandomSource getRandom(final @Nullable BlockPos pos) {
      if (this.random != null) {
         return this.random;
      } else {
         return pos == null ? RandomSource.create(Util.getMillis()) : RandomSource.create(Mth.getSeed(pos));
      }
   }

   public boolean isIgnoreEntities() {
      return this.ignoreEntities;
   }

   public @Nullable BoundingBox getBoundingBox() {
      return this.boundingBox;
   }

   public boolean getKnownShape() {
      return this.knownShape;
   }

   public List<StructureProcessor> getProcessors() {
      return this.processors;
   }

   public boolean shouldApplyWaterlogging() {
      return this.liquidSettings == LiquidSettings.APPLY_WATERLOGGING;
   }

   public StructureTemplate.Palette getRandomPalette(final List<StructureTemplate.Palette> palettes, final @Nullable BlockPos pos) {
      int paletteSize = palettes.size();
      if (paletteSize == 0) {
         throw new IllegalStateException("No palettes");
      } else {
         return palettes.get(this.getRandom(pos).nextInt(paletteSize));
      }
   }

   public StructurePlaceSettings setFinalizeEntities(final boolean finalizeEntities) {
      this.finalizeEntities = finalizeEntities;
      return this;
   }

   public boolean shouldFinalizeEntities() {
      return this.finalizeEntities;
   }
}
