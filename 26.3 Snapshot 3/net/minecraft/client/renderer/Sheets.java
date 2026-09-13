package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.state.properties.ChestType;

public class Sheets {
   public static final Identifier SHULKER_SHEET = Identifier.withDefaultNamespace("textures/atlas/shulker_boxes.png");
   public static final Identifier BANNER_SHEET = Identifier.withDefaultNamespace("textures/atlas/banner_patterns.png");
   public static final Identifier SHIELD_SHEET = Identifier.withDefaultNamespace("textures/atlas/shield_patterns.png");
   public static final Identifier CHEST_SHEET = Identifier.withDefaultNamespace("textures/atlas/chest.png");
   public static final Identifier DECORATED_POT_SHEET = Identifier.withDefaultNamespace("textures/atlas/decorated_pot.png");
   public static final Identifier GUI_SHEET = Identifier.withDefaultNamespace("textures/atlas/gui.png");
   public static final Identifier MAP_DECORATIONS_SHEET = Identifier.withDefaultNamespace("textures/atlas/map_decorations.png");
   public static final Identifier PAINTINGS_SHEET = Identifier.withDefaultNamespace("textures/atlas/paintings.png");
   public static final Identifier CELESTIAL_SHEET = Identifier.withDefaultNamespace("textures/atlas/celestials.png");
   private static final RenderType CUTOUT_BLOCK_ITEM_SHEET = RenderTypes.itemCutout(TextureAtlas.LOCATION_BLOCKS);
   private static final RenderType CUTOUT_BLOCK_ITEM_GLINT_SHEET = RenderTypes.itemCutoutGlint(TextureAtlas.LOCATION_BLOCKS);
   private static final RenderType CUTOUT_BLOCK_ITEM_GLINT_SPECIAL_SHEET = RenderTypes.itemCutoutGlintSpecial(TextureAtlas.LOCATION_BLOCKS);
   private static final RenderType TRANSLUCENT_BLOCK_ITEM_SHEET = RenderTypes.itemTranslucent(TextureAtlas.LOCATION_BLOCKS);
   private static final RenderType TRANSLUCENT_BLOCK_ITEM_GLINT_SHEET = RenderTypes.itemTranslucentGlint(TextureAtlas.LOCATION_BLOCKS);
   private static final RenderType TRANSLUCENT_BLOCK_ITEM_GLINT_SPECIAL_SHEET = RenderTypes.itemTranslucentGlintSpecial(TextureAtlas.LOCATION_BLOCKS);
   private static final RenderType CUTOUT_ITEM_SHEET = RenderTypes.itemCutout(TextureAtlas.LOCATION_ITEMS);
   private static final RenderType CUTOUT_ITEM_GLINT_SHEET = RenderTypes.itemCutoutGlint(TextureAtlas.LOCATION_ITEMS);
   private static final RenderType CUTOUT_ITEM_GLINT_SPECIAL_SHEET = RenderTypes.itemCutoutGlintSpecial(TextureAtlas.LOCATION_ITEMS);
   private static final RenderType TRANSLUCENT_ITEM_SHEET = RenderTypes.itemTranslucent(TextureAtlas.LOCATION_ITEMS);
   private static final RenderType TRANSLUCENT_ITEM_GLINT_SHEET = RenderTypes.itemTranslucentGlint(TextureAtlas.LOCATION_ITEMS);
   private static final RenderType TRANSLUCENT_ITEM_GLINT_SPECIAL_SHEET = RenderTypes.itemTranslucentGlintSpecial(TextureAtlas.LOCATION_ITEMS);
   public static final SpriteMapper ITEMS_MAPPER = new SpriteMapper(TextureAtlas.LOCATION_ITEMS, "item");
   public static final SpriteMapper BLOCKS_MAPPER = new SpriteMapper(TextureAtlas.LOCATION_BLOCKS, "block");
   public static final SpriteMapper BLOCK_ENTITIES_MAPPER = new SpriteMapper(TextureAtlas.LOCATION_BLOCKS, "entity");
   public static final SpriteMapper BANNER_MAPPER = new SpriteMapper(BANNER_SHEET, "entity/banner");
   public static final SpriteMapper SHIELD_MAPPER = new SpriteMapper(SHIELD_SHEET, "entity/shield");
   public static final SpriteMapper CHEST_MAPPER = new SpriteMapper(CHEST_SHEET, "entity/chest");
   public static final SpriteMapper DECORATED_POT_MAPPER = new SpriteMapper(DECORATED_POT_SHEET, "entity/decorated_pot");
   public static final SpriteMapper SHULKER_MAPPER = new SpriteMapper(SHULKER_SHEET, "entity/shulker");
   public static final SpriteId DEFAULT_SHULKER_TEXTURE_LOCATION = SHULKER_MAPPER.defaultNamespaceApply("shulker");
   public static final List<SpriteId> SHULKER_TEXTURE_LOCATION = Arrays.stream(DyeColor.values())
      .sorted(Comparator.comparingInt(DyeColor::getId))
      .map(Sheets::createShulkerSprite)
      .collect(ImmutableList.toImmutableList());
   public static final SpriteId BANNER_BASE = BANNER_MAPPER.defaultNamespaceApply("banner_base");
   public static final SpriteId SHIELD_BASE = SHIELD_MAPPER.defaultNamespaceApply("shield_base");
   public static final SpriteId SHIELD_BASE_NO_PATTERN = SHIELD_MAPPER.defaultNamespaceApply("shield_base_nopattern");
   public static final SpriteId BANNER_PATTERN_BASE = BANNER_MAPPER.defaultNamespaceApply("base");
   public static final SpriteId SHIELD_PATTERN_BASE = SHIELD_MAPPER.defaultNamespaceApply("base");
   private static final Map<Identifier, SpriteId> BANNER_SPRITES = new HashMap<>();
   private static final Map<Identifier, SpriteId> SHIELD_SPRITES = new HashMap<>();
   public static final SpriteId DECORATED_POT_BASE = DECORATED_POT_MAPPER.defaultNamespaceApply("decorated_pot_base");
   public static final SpriteId DECORATED_POT_SIDE = DECORATED_POT_MAPPER.defaultNamespaceApply("decorated_pot_side");
   public static final SpriteId ENDER_CHEST_LOCATION = CHEST_MAPPER.defaultNamespaceApply("ender");
   public static final MultiblockChestResources<SpriteId> CHEST_REGULAR = ChestSpecialRenderer.REGULAR.map(CHEST_MAPPER::apply);
   public static final MultiblockChestResources<SpriteId> CHEST_TRAPPED = ChestSpecialRenderer.TRAPPED.map(CHEST_MAPPER::apply);
   public static final MultiblockChestResources<SpriteId> CHEST_CHRISTMAS = ChestSpecialRenderer.CHRISTMAS.map(CHEST_MAPPER::apply);
   public static final WeatheringCopperCollection.ByState<MultiblockChestResources<SpriteId>> CHEST_COPPER = ChestSpecialRenderer.COPPER
      .map(r -> r.map(CHEST_MAPPER::apply));

   public static RenderType cutoutBlockItemSheet() {
      return CUTOUT_BLOCK_ITEM_SHEET;
   }

   public static RenderType cutoutBlockItemGlintSheet() {
      return CUTOUT_BLOCK_ITEM_GLINT_SHEET;
   }

   public static RenderType cutoutBlockItemGlintSpecialSheet() {
      return CUTOUT_BLOCK_ITEM_GLINT_SPECIAL_SHEET;
   }

   public static RenderType cutoutItemSheet() {
      return CUTOUT_ITEM_SHEET;
   }

   public static RenderType cutoutItemGlintSheet() {
      return CUTOUT_ITEM_GLINT_SHEET;
   }

   public static RenderType cutoutItemGlintSpecialSheet() {
      return CUTOUT_ITEM_GLINT_SPECIAL_SHEET;
   }

   public static RenderType translucentItemSheet() {
      return TRANSLUCENT_ITEM_SHEET;
   }

   public static RenderType translucentItemGlintSheet() {
      return TRANSLUCENT_ITEM_GLINT_SHEET;
   }

   public static RenderType translucentItemGlintSpecialSheet() {
      return TRANSLUCENT_ITEM_GLINT_SPECIAL_SHEET;
   }

   public static RenderType translucentBlockItemSheet() {
      return TRANSLUCENT_BLOCK_ITEM_SHEET;
   }

   public static RenderType translucentBlockItemGlintSheet() {
      return TRANSLUCENT_BLOCK_ITEM_GLINT_SHEET;
   }

   public static RenderType translucentBlockItemGlintSpecialSheet() {
      return TRANSLUCENT_BLOCK_ITEM_GLINT_SPECIAL_SHEET;
   }

   public static SpriteId getShulkerBoxSprite(final DyeColor color) {
      return SHULKER_TEXTURE_LOCATION.get(color.getId());
   }

   public static Identifier colorToShulkerSprite(final DyeColor color) {
      return Identifier.withDefaultNamespace("shulker_" + color.getName());
   }

   public static SpriteId createShulkerSprite(final DyeColor color) {
      return SHULKER_MAPPER.apply(colorToShulkerSprite(color));
   }

   public static SpriteId getBannerSprite(final Holder<BannerPattern> pattern) {
      return BANNER_SPRITES.computeIfAbsent(pattern.value().assetId(), BANNER_MAPPER::apply);
   }

   public static SpriteId getShieldSprite(final Holder<BannerPattern> pattern) {
      return SHIELD_SPRITES.computeIfAbsent(pattern.value().assetId(), SHIELD_MAPPER::apply);
   }

   public static SpriteId chooseSprite(final ChestRenderState.ChestMaterialType materialType, final ChestType type) {
      return switch (materialType) {
         case ENDER_CHEST -> ENDER_CHEST_LOCATION;
         case REGULAR -> (SpriteId)CHEST_REGULAR.select(type);
         case CHRISTMAS -> (SpriteId)CHEST_CHRISTMAS.select(type);
         case TRAPPED -> (SpriteId)CHEST_TRAPPED.select(type);
         case COPPER_UNAFFECTED -> (SpriteId)CHEST_COPPER.unaffected().select(type);
         case COPPER_EXPOSED -> (SpriteId)CHEST_COPPER.exposed().select(type);
         case COPPER_WEATHERED -> (SpriteId)CHEST_COPPER.weathered().select(type);
         case COPPER_OXIDIZED -> (SpriteId)CHEST_COPPER.oxidized().select(type);
      };
   }
}
