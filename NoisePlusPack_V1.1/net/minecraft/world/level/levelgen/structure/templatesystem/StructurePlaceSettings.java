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
      StructurePlaceSettings structureplacesettings = new StructurePlaceSettings();
      structureplacesettings.mirror = this.mirror;
      structureplacesettings.rotation = this.rotation;
      structureplacesettings.rotationPivot = this.rotationPivot;
      structureplacesettings.ignoreEntities = this.ignoreEntities;
      structureplacesettings.boundingBox = this.boundingBox;
      structureplacesettings.liquidSettings = this.liquidSettings;
      structureplacesettings.random = this.random;
      structureplacesettings.palette = this.palette;
      structureplacesettings.processors.addAll(this.processors);
      structureplacesettings.knownShape = this.knownShape;
      structureplacesettings.finalizeEntities = this.finalizeEntities;
      return structureplacesettings;
   }

   public StructurePlaceSettings setMirror(Mirror p_74378_) {
      this.mirror = p_74378_;
      return this;
   }

   public StructurePlaceSettings setRotation(Rotation p_74380_) {
      this.rotation = p_74380_;
      return this;
   }

   public StructurePlaceSettings setRotationPivot(BlockPos p_74386_) {
      this.rotationPivot = p_74386_;
      return this;
   }

   public StructurePlaceSettings setIgnoreEntities(boolean p_74393_) {
      this.ignoreEntities = p_74393_;
      return this;
   }

   public StructurePlaceSettings setBoundingBox(BoundingBox p_74382_) {
      this.boundingBox = p_74382_;
      return this;
   }

   public StructurePlaceSettings setRandom(@Nullable RandomSource p_230325_) {
      this.random = p_230325_;
      return this;
   }

   public StructurePlaceSettings setLiquidSettings(LiquidSettings p_343019_) {
      this.liquidSettings = p_343019_;
      return this;
   }

   public StructurePlaceSettings setKnownShape(boolean p_74403_) {
      this.knownShape = p_74403_;
      return this;
   }

   public StructurePlaceSettings clearProcessors() {
      this.processors.clear();
      return this;
   }

   public StructurePlaceSettings addProcessor(StructureProcessor p_74384_) {
      this.processors.add(p_74384_);
      return this;
   }

   public StructurePlaceSettings popProcessor(StructureProcessor p_74398_) {
      this.processors.remove(p_74398_);
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

   public RandomSource getRandom(@Nullable BlockPos p_230327_) {
      if (this.random != null) {
         return this.random;
      } else {
         return p_230327_ == null ? RandomSource.create(Util.getMillis()) : RandomSource.create(Mth.getSeed(p_230327_));
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

   public StructureTemplate.Palette getRandomPalette(List<StructureTemplate.Palette> p_74388_, @Nullable BlockPos p_74389_) {
      int i = p_74388_.size();
      if (i == 0) {
         throw new IllegalStateException("No palettes");
      } else {
         return p_74388_.get(this.getRandom(p_74389_).nextInt(i));
      }
   }

   public StructurePlaceSettings setFinalizeEntities(boolean p_74406_) {
      this.finalizeEntities = p_74406_;
      return this;
   }

   public boolean shouldFinalizeEntities() {
      return this.finalizeEntities;
   }
}
