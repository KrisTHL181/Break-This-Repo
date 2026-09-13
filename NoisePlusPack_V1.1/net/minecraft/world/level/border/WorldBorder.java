package net.minecraft.world.level.border;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WorldBorder extends SavedData {
   public static final double MAX_SIZE = 5.999997E7F;
   public static final double MAX_CENTER_COORDINATE = 2.9999984E7;
   public static final Codec<WorldBorder> CODEC = WorldBorder.Settings.CODEC.xmap(WorldBorder::new, WorldBorder.Settings::new);
   public static final SavedDataType<WorldBorder> TYPE = new SavedDataType<>("world_border", WorldBorder::new, CODEC, DataFixTypes.SAVED_DATA_WORLD_BORDER);
   private final WorldBorder.Settings settings;
   private boolean initialized;
   private final List<BorderChangeListener> listeners = Lists.newArrayList();
   double damagePerBlock = 0.2;
   double safeZone = 5.0;
   int warningTime = 15;
   int warningBlocks = 5;
   double centerX;
   double centerZ;
   int absoluteMaxSize = Integer.MAX_VALUE;
   WorldBorder.BorderExtent extent = new WorldBorder.StaticBorderExtent(5.999997E7F);

   public WorldBorder() {
      this(WorldBorder.Settings.DEFAULT);
   }

   public WorldBorder(WorldBorder.Settings p_458268_) {
      this.settings = p_458268_;
   }

   public boolean isWithinBounds(BlockPos p_61938_) {
      return true;
   }

   public boolean isWithinBounds(Vec3 p_343899_) {
      return true;
   }

   public boolean isWithinBounds(ChunkPos p_61928_) {
      return true;
   }

   public boolean isWithinBounds(AABB p_61936_) {
      return true;
   }

   private boolean isWithinBounds(double p_342617_, double p_344821_, double p_344911_, double p_344145_) {
      return true;
   }

   public boolean isWithinBounds(double p_156094_, double p_156095_) {
      return true;
   }

   public boolean isWithinBounds(double p_187563_, double p_187564_, double p_187565_) {
      return true;
   }

   public BlockPos clampToBounds(BlockPos p_342374_) {
      return p_342374_;
   }

   public BlockPos clampToBounds(Vec3 p_345328_) {
      return this.clampToBounds(p_345328_.x(), p_345328_.y(), p_345328_.z());
   }

   public BlockPos clampToBounds(double p_187570_, double p_187571_, double p_187572_) {
      return BlockPos.containing(p_187570_, p_187571_, p_187572_);
   }

   public Vec3 clampVec3ToBound(Vec3 p_369267_) {
      return this.clampVec3ToBound(p_369267_.x, p_369267_.y, p_369267_.z);
   }

   public Vec3 clampVec3ToBound(double p_363761_, double p_367247_, double p_362240_) {
      return new Vec3(p_363761_, p_367247_, p_362240_);
   }

   public double getDistanceToBorder(Entity p_61926_) {
      return this.getDistanceToBorder(p_61926_.getX(), p_61926_.getZ());
   }

   public VoxelShape getCollisionShape() {
      return this.extent.getCollisionShape();
   }

   public double getDistanceToBorder(double p_61942_, double p_61943_) {
      double d0 = p_61943_ - this.getMinZ();
      double d1 = this.getMaxZ() - p_61943_;
      double d2 = p_61942_ - this.getMinX();
      double d3 = this.getMaxX() - p_61942_;
      double d4 = Math.min(d2, d3);
      d4 = Math.min(d4, d0);
      return Math.min(d4, d1);
   }

   public boolean isInsideCloseToBorder(Entity p_187567_, AABB p_187568_) {
      double d0 = Math.max(Mth.absMax(p_187568_.getXsize(), p_187568_.getZsize()), 1.0);
      return this.getDistanceToBorder(p_187567_) < d0 * 2.0 && this.isWithinBounds(p_187567_.getX(), p_187567_.getZ(), d0);
   }

   public BorderStatus getStatus() {
      return this.extent.getStatus();
   }

   public double getMinX() {
      return this.getMinX(0.0F);
   }

   public double getMinX(float p_451800_) {
      return this.extent.getMinX(p_451800_);
   }

   public double getMinZ() {
      return this.getMinZ(0.0F);
   }

   public double getMinZ(float p_452349_) {
      return this.extent.getMinZ(p_452349_);
   }

   public double getMaxX() {
      return this.getMaxX(0.0F);
   }

   public double getMaxX(float p_456307_) {
      return this.extent.getMaxX(p_456307_);
   }

   public double getMaxZ() {
      return this.getMaxZ(0.0F);
   }

   public double getMaxZ(float p_451858_) {
      return this.extent.getMaxZ(p_451858_);
   }

   public double getCenterX() {
      return this.centerX;
   }

   public double getCenterZ() {
      return this.centerZ;
   }

   public void setCenter(double p_61950_, double p_61951_) {
      this.centerX = p_61950_;
      this.centerZ = p_61951_;
      this.extent.onCenterChange();
      this.setDirty();

      for (BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onSetCenter(this, p_61950_, p_61951_);
      }
   }

   public double getSize() {
      return this.extent.getSize();
   }

   public long getLerpTime() {
      return this.extent.getLerpTime();
   }

   public double getLerpTarget() {
      return this.extent.getLerpTarget();
   }

   public void setSize(double p_61918_) {
      this.extent = new WorldBorder.StaticBorderExtent(p_61918_);
      this.setDirty();

      for (BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onSetSize(this, p_61918_);
      }
   }

   public void lerpSizeBetween(double p_61920_, double p_61921_, long p_61922_, long p_457202_) {
      this.extent = p_61920_ == p_61921_
         ? new WorldBorder.StaticBorderExtent(p_61921_)
         : new WorldBorder.MovingBorderExtent(p_61920_, p_61921_, p_61922_, p_457202_);
      this.setDirty();

      for (BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onLerpSize(this, p_61920_, p_61921_, p_61922_, p_457202_);
      }
   }

   protected List<BorderChangeListener> getListeners() {
      return Lists.newArrayList(this.listeners);
   }

   public void addListener(BorderChangeListener p_61930_) {
      this.listeners.add(p_61930_);
   }

   public void removeListener(BorderChangeListener p_156097_) {
      this.listeners.remove(p_156097_);
   }

   public void setAbsoluteMaxSize(int p_61924_) {
      this.absoluteMaxSize = p_61924_;
      this.extent.onAbsoluteMaxSizeChange();
   }

   public int getAbsoluteMaxSize() {
      return this.absoluteMaxSize;
   }

   public double getSafeZone() {
      return this.safeZone;
   }

   public void setSafeZone(double p_424052_) {
      this.safeZone = p_424052_;
      this.setDirty();

      for (BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onSetSafeZone(this, p_424052_);
      }
   }

   public double getDamagePerBlock() {
      return 0.0;
   }

   public void setDamagePerBlock(double p_61948_) {
      this.damagePerBlock = p_61948_;
      this.setDirty();

      for (BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onSetDamagePerBlock(this, p_61948_);
      }
   }

   public double getLerpSpeed() {
      return this.extent.getLerpSpeed();
   }

   public int getWarningTime() {
      return this.warningTime;
   }

   public void setWarningTime(int p_61945_) {
      this.warningTime = p_61945_;
      this.setDirty();

      for (BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onSetWarningTime(this, p_61945_);
      }
   }

   public int getWarningBlocks() {
      return this.warningBlocks;
   }

   public void setWarningBlocks(int p_61953_) {
      this.warningBlocks = p_61953_;
      this.setDirty();

      for (BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onSetWarningBlocks(this, p_61953_);
      }
   }

   public void tick() {
      this.extent = this.extent.update();
   }

   public void applyInitialSettings(long p_455036_) {
      if (!this.initialized) {
         this.setCenter(this.settings.centerX(), this.settings.centerZ());
         this.setDamagePerBlock(this.settings.damagePerBlock());
         this.setSafeZone(this.settings.safeZone());
         this.setWarningBlocks(this.settings.warningBlocks());
         this.setWarningTime(this.settings.warningTime());
         if (this.settings.lerpTime() > 0L) {
            this.lerpSizeBetween(this.settings.size(), this.settings.lerpTarget(), this.settings.lerpTime(), p_455036_);
         } else {
            this.setSize(this.settings.size());
         }

         this.initialized = true;
      }
   }

   interface BorderExtent {
      double getMinX(float var1);

      double getMaxX(float var1);

      double getMinZ(float var1);

      double getMaxZ(float var1);

      double getSize();

      double getLerpSpeed();

      long getLerpTime();

      double getLerpTarget();

      BorderStatus getStatus();

      void onAbsoluteMaxSizeChange();

      void onCenterChange();

      WorldBorder.BorderExtent update();

      VoxelShape getCollisionShape();
   }

   class MovingBorderExtent implements WorldBorder.BorderExtent {
      private final double from;
      private final double to;
      private final long lerpEnd;
      private final long lerpBegin;
      private final double lerpDuration;
      private long lerpProgress;
      private double size;
      private double previousSize;

      MovingBorderExtent(final double p_61979_, final double p_61980_, final long p_61981_, final long p_452640_) {
         this.from = p_61979_;
         this.to = p_61980_;
         this.lerpDuration = p_61981_;
         this.lerpProgress = p_61981_;
         this.lerpBegin = p_452640_;
         this.lerpEnd = this.lerpBegin + p_61981_;
         double d0 = this.calculateSize();
         this.size = d0;
         this.previousSize = d0;
      }

      @Override
      public double getMinX(float p_451442_) {
         return Mth.clamp(
            WorldBorder.this.getCenterX() - Mth.lerp(p_451442_, this.getPreviousSize(), this.getSize()) / 2.0,
            -WorldBorder.this.absoluteMaxSize,
            WorldBorder.this.absoluteMaxSize
         );
      }

      @Override
      public double getMinZ(float p_450660_) {
         return Mth.clamp(
            WorldBorder.this.getCenterZ() - Mth.lerp(p_450660_, this.getPreviousSize(), this.getSize()) / 2.0,
            -WorldBorder.this.absoluteMaxSize,
            WorldBorder.this.absoluteMaxSize
         );
      }

      @Override
      public double getMaxX(float p_451178_) {
         return Mth.clamp(
            WorldBorder.this.getCenterX() + Mth.lerp(p_451178_, this.getPreviousSize(), this.getSize()) / 2.0,
            -WorldBorder.this.absoluteMaxSize,
            WorldBorder.this.absoluteMaxSize
         );
      }

      @Override
      public double getMaxZ(float p_453776_) {
         return Mth.clamp(
            WorldBorder.this.getCenterZ() + Mth.lerp(p_453776_, this.getPreviousSize(), this.getSize()) / 2.0,
            -WorldBorder.this.absoluteMaxSize,
            WorldBorder.this.absoluteMaxSize
         );
      }

      @Override
      public double getSize() {
         return this.size;
      }

      public double getPreviousSize() {
         return this.previousSize;
      }

      private double calculateSize() {
         double d0 = (this.lerpDuration - this.lerpProgress) / this.lerpDuration;
         return d0 < 1.0 ? Mth.lerp(d0, this.from, this.to) : this.to;
      }

      @Override
      public double getLerpSpeed() {
         return Math.abs(this.from - this.to) / (this.lerpEnd - this.lerpBegin);
      }

      @Override
      public long getLerpTime() {
         return this.lerpProgress;
      }

      @Override
      public double getLerpTarget() {
         return this.to;
      }

      @Override
      public BorderStatus getStatus() {
         return this.to < this.from ? BorderStatus.SHRINKING : BorderStatus.GROWING;
      }

      @Override
      public void onCenterChange() {
      }

      @Override
      public void onAbsoluteMaxSizeChange() {
      }

      @Override
      public WorldBorder.BorderExtent update() {
         this.lerpProgress--;
         this.previousSize = this.size;
         this.size = this.calculateSize();
         if (this.lerpProgress <= 0L) {
            WorldBorder.this.setDirty();
            return WorldBorder.this.new StaticBorderExtent(this.to);
         } else {
            return this;
         }
      }

      @Override
      public VoxelShape getCollisionShape() {
         return Shapes.join(
            Shapes.INFINITY,
            Shapes.box(
               Math.floor(this.getMinX(0.0F)),
               Double.NEGATIVE_INFINITY,
               Math.floor(this.getMinZ(0.0F)),
               Math.ceil(this.getMaxX(0.0F)),
               Double.POSITIVE_INFINITY,
               Math.ceil(this.getMaxZ(0.0F))
            ),
            BooleanOp.ONLY_FIRST
         );
      }
   }

   public record Settings(
      double centerX, double centerZ, double damagePerBlock, double safeZone, int warningBlocks, int warningTime, double size, long lerpTime, double lerpTarget
   ) {
      public static final WorldBorder.Settings DEFAULT = new WorldBorder.Settings(0.0, 0.0, 0.2, 5.0, 5, 300, 5.999997E7F, 0L, 0.0);
      public static final Codec<WorldBorder.Settings> CODEC = RecordCodecBuilder.create(
         p_423671_ -> p_423671_.group(
               Codec.doubleRange(-2.9999984E7, 2.9999984E7).fieldOf("center_x").forGetter(WorldBorder.Settings::centerX),
               Codec.doubleRange(-2.9999984E7, 2.9999984E7).fieldOf("center_z").forGetter(WorldBorder.Settings::centerZ),
               Codec.DOUBLE.fieldOf("damage_per_block").forGetter(WorldBorder.Settings::damagePerBlock),
               Codec.DOUBLE.fieldOf("safe_zone").forGetter(WorldBorder.Settings::safeZone),
               Codec.INT.fieldOf("warning_blocks").forGetter(WorldBorder.Settings::warningBlocks),
               Codec.INT.fieldOf("warning_time").forGetter(WorldBorder.Settings::warningTime),
               Codec.DOUBLE.fieldOf("size").forGetter(WorldBorder.Settings::size),
               Codec.LONG.fieldOf("lerp_time").forGetter(WorldBorder.Settings::lerpTime),
               Codec.DOUBLE.fieldOf("lerp_target").forGetter(WorldBorder.Settings::lerpTarget)
            )
            .apply(p_423671_, WorldBorder.Settings::new)
      );

      public Settings(WorldBorder p_62032_) {
         this(
            p_62032_.centerX,
            p_62032_.centerZ,
            p_62032_.damagePerBlock,
            p_62032_.safeZone,
            p_62032_.warningBlocks,
            p_62032_.warningTime,
            p_62032_.extent.getSize(),
            p_62032_.extent.getLerpTime(),
            p_62032_.extent.getLerpTarget()
         );
      }
   }

   class StaticBorderExtent implements WorldBorder.BorderExtent {
      private final double size;
      private double minX;
      private double minZ;
      private double maxX;
      private double maxZ;
      private VoxelShape shape;

      public StaticBorderExtent(final double p_62059_) {
         this.size = p_62059_;
         this.updateBox();
      }

      @Override
      public double getMinX(float p_452854_) {
         return this.minX;
      }

      @Override
      public double getMaxX(float p_450229_) {
         return this.maxX;
      }

      @Override
      public double getMinZ(float p_458572_) {
         return this.minZ;
      }

      @Override
      public double getMaxZ(float p_454450_) {
         return this.maxZ;
      }

      @Override
      public double getSize() {
         return this.size;
      }

      @Override
      public BorderStatus getStatus() {
         return BorderStatus.STATIONARY;
      }

      @Override
      public double getLerpSpeed() {
         return 0.0;
      }

      @Override
      public long getLerpTime() {
         return 0L;
      }

      @Override
      public double getLerpTarget() {
         return this.size;
      }

      private void updateBox() {
         this.minX = Mth.clamp(WorldBorder.this.getCenterX() - this.size / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
         this.minZ = Mth.clamp(WorldBorder.this.getCenterZ() - this.size / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
         this.maxX = Mth.clamp(WorldBorder.this.getCenterX() + this.size / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
         this.maxZ = Mth.clamp(WorldBorder.this.getCenterZ() + this.size / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
         this.shape = Shapes.join(
            Shapes.INFINITY,
            Shapes.box(
               Math.floor(this.getMinX(0.0F)),
               Double.NEGATIVE_INFINITY,
               Math.floor(this.getMinZ(0.0F)),
               Math.ceil(this.getMaxX(0.0F)),
               Double.POSITIVE_INFINITY,
               Math.ceil(this.getMaxZ(0.0F))
            ),
            BooleanOp.ONLY_FIRST
         );
      }

      @Override
      public void onAbsoluteMaxSizeChange() {
         this.updateBox();
      }

      @Override
      public void onCenterChange() {
         this.updateBox();
      }

      @Override
      public WorldBorder.BorderExtent update() {
         return this;
      }

      @Override
      public VoxelShape getCollisionShape() {
         return this.shape;
      }
   }
}
