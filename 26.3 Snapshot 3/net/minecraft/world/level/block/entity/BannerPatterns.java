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

   private static ResourceKey<BannerPattern> create(final String id) {
      return ResourceKey.create(Registries.BANNER_PATTERN, Identifier.withDefaultNamespace(id));
   }

   public static void bootstrap(final BootstrapContext<BannerPattern> context) {
      register(context, BASE);
      register(context, SQUARE_BOTTOM_LEFT);
      register(context, SQUARE_BOTTOM_RIGHT);
      register(context, SQUARE_TOP_LEFT);
      register(context, SQUARE_TOP_RIGHT);
      register(context, STRIPE_BOTTOM);
      register(context, STRIPE_TOP);
      register(context, STRIPE_LEFT);
      register(context, STRIPE_RIGHT);
      register(context, STRIPE_CENTER);
      register(context, STRIPE_MIDDLE);
      register(context, STRIPE_DOWNRIGHT);
      register(context, STRIPE_DOWNLEFT);
      register(context, STRIPE_SMALL);
      register(context, CROSS);
      register(context, STRAIGHT_CROSS);
      register(context, TRIANGLE_BOTTOM);
      register(context, TRIANGLE_TOP);
      register(context, TRIANGLES_BOTTOM);
      register(context, TRIANGLES_TOP);
      register(context, DIAGONAL_LEFT);
      register(context, DIAGONAL_RIGHT);
      register(context, DIAGONAL_LEFT_MIRROR);
      register(context, DIAGONAL_RIGHT_MIRROR);
      register(context, CIRCLE_MIDDLE);
      register(context, RHOMBUS_MIDDLE);
      register(context, HALF_VERTICAL);
      register(context, HALF_HORIZONTAL);
      register(context, HALF_VERTICAL_MIRROR);
      register(context, HALF_HORIZONTAL_MIRROR);
      register(context, BORDER);
      register(context, GRADIENT);
      register(context, GRADIENT_UP);
      register(context, BRICKS);
      register(context, CURLY_BORDER);
      register(context, GLOBE);
      register(context, CREEPER);
      register(context, SKULL);
      register(context, FLOWER);
      register(context, MOJANG);
      register(context, PIGLIN);
      register(context, FLOW);
      register(context, GUSTER);
   }

   public static void register(final BootstrapContext<BannerPattern> context, final ResourceKey<BannerPattern> key) {
      context.register(key, new BannerPattern(key.identifier(), "block.minecraft.banner." + key.identifier().toShortLanguageKey()));
   }
}
