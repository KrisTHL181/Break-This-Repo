package net.minecraft.world.level.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class BannerPatterns {
   public static final ResourceKey<BannerPattern> BASE = create("base");
   public static final ResourceKey<BannerPattern> SQUARE_BOTTOM_LEFT = create("square_bottom_left");
   public static final ResourceKey<BannerPattern> SQUARE_BOTTOM_RIGHT = create("square_bottom_right");
   public static final ResourceKey<BannerPattern> SQUARE_TOP_LEFT = create("square_top_left");
   public static final ResourceKey<BannerPattern> SQUARE_TOP_RIGHT = create("square_top_right");
   public static final ResourceKey<BannerPattern> STRIPE_BOTTOM = create("stripe_bottom");
   public static final ResourceKey<BannerPattern> STRIPE_TOP = create("stripe_top");
   public static final ResourceKey<BannerPattern> STRIPE_LEFT = create("stripe_left");
   public static final ResourceKey<BannerPattern> STRIPE_RIGHT = create("stripe_right");
   public static final ResourceKey<BannerPattern> STRIPE_CENTER = create("stripe_center");
   public static final ResourceKey<BannerPattern> STRIPE_MIDDLE = create("stripe_middle");
   public static final ResourceKey<BannerPattern> STRIPE_DOWNRIGHT = create("stripe_downright");
   public static final ResourceKey<BannerPattern> STRIPE_DOWNLEFT = create("stripe_downleft");
   public static final ResourceKey<BannerPattern> STRIPE_SMALL = create("small_stripes");
   public static final ResourceKey<BannerPattern> CROSS = create("cross");
   public static final ResourceKey<BannerPattern> STRAIGHT_CROSS = create("straight_cross");
   public static final ResourceKey<BannerPattern> TRIANGLE_BOTTOM = create("triangle_bottom");
   public static final ResourceKey<BannerPattern> TRIANGLE_TOP = create("triangle_top");
   public static final ResourceKey<BannerPattern> TRIANGLES_BOTTOM = create("triangles_bottom");
   public static final ResourceKey<BannerPattern> TRIANGLES_TOP = create("triangles_top");
   public static final ResourceKey<BannerPattern> DIAGONAL_LEFT = create("diagonal_left");
   public static final ResourceKey<BannerPattern> DIAGONAL_RIGHT = create("diagonal_up_right");
   public static final ResourceKey<BannerPattern> DIAGONAL_LEFT_MIRROR = create("diagonal_up_left");
   public static final ResourceKey<BannerPattern> DIAGONAL_RIGHT_MIRROR = create("diagonal_right");
   public static final ResourceKey<BannerPattern> CIRCLE_MIDDLE = create("circle");
   public static final ResourceKey<BannerPattern> RHOMBUS_MIDDLE = create("rhombus");
   public static final ResourceKey<BannerPattern> HALF_VERTICAL = create("half_vertical");
   public static final ResourceKey<BannerPattern> HALF_HORIZONTAL = create("half_horizontal");
   public static final ResourceKey<BannerPattern> HALF_VERTICAL_MIRROR = create("half_vertical_right");
   public static final ResourceKey<BannerPattern> HALF_HORIZONTAL_MIRROR = create("half_horizontal_bottom");
   public static final ResourceKey<BannerPattern> BORDER = create("border");
   public static final ResourceKey<BannerPattern> CURLY_BORDER = create("curly_border");
   public static final ResourceKey<BannerPattern> GRADIENT = create("gradient");
   public static final ResourceKey<BannerPattern> GRADIENT_UP = create("gradient_up");
   public static final ResourceKey<BannerPattern> BRICKS = create("bricks");
   public static final ResourceKey<BannerPattern> GLOBE = create("globe");
   public static final ResourceKey<BannerPattern> CREEPER = create("creeper");
   public static final ResourceKey<BannerPattern> SKULL = create("skull");
   public static final ResourceKey<BannerPattern> FLOWER = create("flower");
   public static final ResourceKey<BannerPattern> MOJANG = create("mojang");
   public static final ResourceKey<BannerPattern> PIGLIN = create("piglin");
   public static final ResourceKey<BannerPattern> FLOW = create("flow");
   public static final ResourceKey<BannerPattern> GUSTER = create("guster");

   private static ResourceKey<BannerPattern> create(String p_222757_) {
      return ResourceKey.create(Registries.BANNER_PATTERN, Identifier.withDefaultNamespace(p_222757_));
   }

   public static void bootstrap(BootstrapContext<BannerPattern> p_335175_) {
      register(p_335175_, BASE);
      register(p_335175_, SQUARE_BOTTOM_LEFT);
      register(p_335175_, SQUARE_BOTTOM_RIGHT);
      register(p_335175_, SQUARE_TOP_LEFT);
      register(p_335175_, SQUARE_TOP_RIGHT);
      register(p_335175_, STRIPE_BOTTOM);
      register(p_335175_, STRIPE_TOP);
      register(p_335175_, STRIPE_LEFT);
      register(p_335175_, STRIPE_RIGHT);
      register(p_335175_, STRIPE_CENTER);
      register(p_335175_, STRIPE_MIDDLE);
      register(p_335175_, STRIPE_DOWNRIGHT);
      register(p_335175_, STRIPE_DOWNLEFT);
      register(p_335175_, STRIPE_SMALL);
      register(p_335175_, CROSS);
      register(p_335175_, STRAIGHT_CROSS);
      register(p_335175_, TRIANGLE_BOTTOM);
      register(p_335175_, TRIANGLE_TOP);
      register(p_335175_, TRIANGLES_BOTTOM);
      register(p_335175_, TRIANGLES_TOP);
      register(p_335175_, DIAGONAL_LEFT);
      register(p_335175_, DIAGONAL_RIGHT);
      register(p_335175_, DIAGONAL_LEFT_MIRROR);
      register(p_335175_, DIAGONAL_RIGHT_MIRROR);
      register(p_335175_, CIRCLE_MIDDLE);
      register(p_335175_, RHOMBUS_MIDDLE);
      register(p_335175_, HALF_VERTICAL);
      register(p_335175_, HALF_HORIZONTAL);
      register(p_335175_, HALF_VERTICAL_MIRROR);
      register(p_335175_, HALF_HORIZONTAL_MIRROR);
      register(p_335175_, BORDER);
      register(p_335175_, GRADIENT);
      register(p_335175_, GRADIENT_UP);
      register(p_335175_, BRICKS);
      register(p_335175_, CURLY_BORDER);
      register(p_335175_, GLOBE);
      register(p_335175_, CREEPER);
      register(p_335175_, SKULL);
      register(p_335175_, FLOWER);
      register(p_335175_, MOJANG);
      register(p_335175_, PIGLIN);
      register(p_335175_, FLOW);
      register(p_335175_, GUSTER);
   }

   public static void register(BootstrapContext<BannerPattern> p_330964_, ResourceKey<BannerPattern> p_329824_) {
      p_330964_.register(p_329824_, new BannerPattern(p_329824_.identifier(), "block.minecraft.banner." + p_329824_.identifier().toShortLanguageKey()));
   }
}
