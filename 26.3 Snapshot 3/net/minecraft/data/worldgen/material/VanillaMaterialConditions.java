package net.minecraft.data.worldgen.material;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

public class VanillaMaterialConditions {
   public static final ResourceKey<SurfaceRules.ConditionSource> ON_FLOOR = createKey("on_floor");
   public static final ResourceKey<SurfaceRules.ConditionSource> UNDER_FLOOR = createKey("under_floor");
   public static final ResourceKey<SurfaceRules.ConditionSource> DEEP_UNDER_FLOOR = createKey("deep_under_floor");
   public static final ResourceKey<SurfaceRules.ConditionSource> VERY_DEEP_UNDER_FLOOR = createKey("very_deep_under_floor");
   public static final ResourceKey<SurfaceRules.ConditionSource> ON_CEILING = createKey("on_ceiling");
   public static final ResourceKey<SurfaceRules.ConditionSource> UNDER_CEILING = createKey("under_ceiling");
   public static final ResourceKey<SurfaceRules.ConditionSource> NOT_UNDERWATER = createKey("not_underwater");
   public static final ResourceKey<SurfaceRules.ConditionSource> NOT_UNDER_DEEP_WATER = createKey("not_under_deep_water");

   private static ResourceKey<SurfaceRules.ConditionSource> createKey(final String name) {
      return ResourceKey.create(Registries.MATERIAL_CONDITION, Identifier.withDefaultNamespace(name));
   }

   public static void bootstrap(final BootstrapContext<SurfaceRules.ConditionSource> context) {
      context.register(ON_FLOOR, SurfaceRules.stoneDepthCheck(0, false, CaveSurface.FLOOR));
      context.register(UNDER_FLOOR, SurfaceRules.stoneDepthCheck(0, true, CaveSurface.FLOOR));
      context.register(DEEP_UNDER_FLOOR, SurfaceRules.stoneDepthCheck(0, true, 6, CaveSurface.FLOOR));
      context.register(VERY_DEEP_UNDER_FLOOR, SurfaceRules.stoneDepthCheck(0, true, 30, CaveSurface.FLOOR));
      context.register(ON_CEILING, SurfaceRules.stoneDepthCheck(0, false, CaveSurface.CEILING));
      context.register(UNDER_CEILING, SurfaceRules.stoneDepthCheck(0, true, CaveSurface.CEILING));
      context.register(NOT_UNDERWATER, SurfaceRules.waterBlockCheck(-1, 0));
      context.register(NOT_UNDER_DEEP_WATER, SurfaceRules.waterStartCheck(-6, -1));
   }
}
