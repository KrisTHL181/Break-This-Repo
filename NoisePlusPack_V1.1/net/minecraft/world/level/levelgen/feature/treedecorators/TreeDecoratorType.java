package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public class TreeDecoratorType<P extends TreeDecorator> {
   public static final TreeDecoratorType<TrunkVineDecorator> TRUNK_VINE = register("trunk_vine", TrunkVineDecorator.CODEC);
   public static final TreeDecoratorType<LeaveVineDecorator> LEAVE_VINE = register("leave_vine", LeaveVineDecorator.CODEC);
   public static final TreeDecoratorType<PaleMossDecorator> PALE_MOSS = register("pale_moss", PaleMossDecorator.CODEC);
   public static final TreeDecoratorType<CreakingHeartDecorator> CREAKING_HEART = register("creaking_heart", CreakingHeartDecorator.CODEC);
   public static final TreeDecoratorType<CocoaDecorator> COCOA = register("cocoa", CocoaDecorator.CODEC);
   public static final TreeDecoratorType<BeehiveDecorator> BEEHIVE = register("beehive", BeehiveDecorator.CODEC);
   public static final TreeDecoratorType<AlterGroundDecorator> ALTER_GROUND = register("alter_ground", AlterGroundDecorator.CODEC);
   public static final TreeDecoratorType<AttachedToLeavesDecorator> ATTACHED_TO_LEAVES = register("attached_to_leaves", AttachedToLeavesDecorator.CODEC);
   public static final TreeDecoratorType<PlaceOnGroundDecorator> PLACE_ON_GROUND = register("place_on_ground", PlaceOnGroundDecorator.CODEC);
   public static final TreeDecoratorType<AttachedToLogsDecorator> ATTACHED_TO_LOGS = register("attached_to_logs", AttachedToLogsDecorator.CODEC);
   private final MapCodec<P> codec;

   private static <P extends TreeDecorator> TreeDecoratorType<P> register(String p_70053_, MapCodec<P> p_329425_) {
      return Registry.register(BuiltInRegistries.TREE_DECORATOR_TYPE, p_70053_, new TreeDecoratorType<>(p_329425_));
   }

   private TreeDecoratorType(MapCodec<P> p_332594_) {
      this.codec = p_332594_;
   }

   public MapCodec<P> codec() {
      return this.codec;
   }
}
