package net.minecraft.client.data.models.model;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class TextureSlot {
   public static final TextureSlot ALL = create("all");
   public static final TextureSlot TEXTURE = create("texture", ALL);
   public static final TextureSlot PARTICLE = create("particle", TEXTURE);
   public static final TextureSlot END = create("end", ALL);
   public static final TextureSlot BOTTOM = create("bottom", END);
   public static final TextureSlot TOP = create("top", END);
   public static final TextureSlot FRONT = create("front", ALL);
   public static final TextureSlot BACK = create("back", ALL);
   public static final TextureSlot SIDE = create("side", ALL);
   public static final TextureSlot NORTH = create("north", SIDE);
   public static final TextureSlot SOUTH = create("south", SIDE);
   public static final TextureSlot EAST = create("east", SIDE);
   public static final TextureSlot WEST = create("west", SIDE);
   public static final TextureSlot UP = create("up");
   public static final TextureSlot DOWN = create("down");
   public static final TextureSlot CROSS = create("cross");
   public static final TextureSlot CROSS_EMISSIVE = create("cross_emissive");
   public static final TextureSlot PLANT = create("plant");
   public static final TextureSlot WALL = create("wall", ALL);
   public static final TextureSlot RAIL = create("rail");
   public static final TextureSlot WOOL = create("wool");
   public static final TextureSlot PATTERN = create("pattern");
   public static final TextureSlot PANE = create("pane");
   public static final TextureSlot EDGE = create("edge");
   public static final TextureSlot FAN = create("fan");
   public static final TextureSlot STEM = create("stem");
   public static final TextureSlot UPPER_STEM = create("upperstem");
   public static final TextureSlot CROP = create("crop");
   public static final TextureSlot DIRT = create("dirt");
   public static final TextureSlot FIRE = create("fire");
   public static final TextureSlot LANTERN = create("lantern");
   public static final TextureSlot PLATFORM = create("platform");
   public static final TextureSlot UNSTICKY = create("unsticky");
   public static final TextureSlot TORCH = create("torch");
   public static final TextureSlot LAYER0 = create("layer0");
   public static final TextureSlot LAYER1 = create("layer1");
   public static final TextureSlot LAYER2 = create("layer2");
   public static final TextureSlot LIT_LOG = create("lit_log");
   public static final TextureSlot CANDLE = create("candle");
   public static final TextureSlot INSIDE = create("inside");
   public static final TextureSlot CONTENT = create("content");
   public static final TextureSlot INNER_TOP = create("inner_top");
   public static final TextureSlot FLOWERBED = create("flowerbed");
   public static final TextureSlot TENTACLES = create("tentacles");
   public static final TextureSlot BARS = create("bars");
   private final String id;
   private final @Nullable TextureSlot parent;

   private static TextureSlot create(String p_375627_) {
      return new TextureSlot(p_375627_, null);
   }

   private static TextureSlot create(String p_375985_, TextureSlot p_377184_) {
      return new TextureSlot(p_375985_, p_377184_);
   }

   private TextureSlot(String p_377477_, @Nullable TextureSlot p_377668_) {
      this.id = p_377477_;
      this.parent = p_377668_;
   }

   public String getId() {
      return this.id;
   }

   public @Nullable TextureSlot getParent() {
      return this.parent;
   }

   @Override
   public String toString() {
      return "#" + this.id;
   }
}
