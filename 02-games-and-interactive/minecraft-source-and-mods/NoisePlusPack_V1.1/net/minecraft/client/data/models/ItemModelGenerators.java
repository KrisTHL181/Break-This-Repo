package net.minecraft.client.data.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.color.item.Firework;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.MapColor;
import net.minecraft.client.color.item.Potion;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.renderer.item.BundleSelectedItemSpecialRenderer;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.conditional.Broken;
import net.minecraft.client.renderer.item.properties.conditional.BundleHasSelectedItem;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.FishingRodCast;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngle;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.client.renderer.item.properties.numeric.CrossbowPull;
import net.minecraft.client.renderer.item.properties.numeric.Time;
import net.minecraft.client.renderer.item.properties.numeric.UseCycle;
import net.minecraft.client.renderer.item.properties.numeric.UseDuration;
import net.minecraft.client.renderer.item.properties.select.Charge;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.renderer.special.TridentSpecialRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemModelGenerators {
   private static final ItemTintSource BLANK_LAYER = ItemModelUtils.constantTint(-1);
   public static final Identifier TRIM_PREFIX_HELMET = prefixForSlotTrim("helmet");
   public static final Identifier TRIM_PREFIX_CHESTPLATE = prefixForSlotTrim("chestplate");
   public static final Identifier TRIM_PREFIX_LEGGINGS = prefixForSlotTrim("leggings");
   public static final Identifier TRIM_PREFIX_BOOTS = prefixForSlotTrim("boots");
   public static final List<ItemModelGenerators.TrimMaterialData> TRIM_MATERIAL_MODELS = List.of(
      new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.QUARTZ, TrimMaterials.QUARTZ),
      new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.IRON, TrimMaterials.IRON),
      new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.NETHERITE, TrimMaterials.NETHERITE),
      new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.REDSTONE, TrimMaterials.REDSTONE),
      new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.COPPER, TrimMaterials.COPPER),
      new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.GOLD, TrimMaterials.GOLD),
      new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.EMERALD, TrimMaterials.EMERALD),
      new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.DIAMOND, TrimMaterials.DIAMOND),
      new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.LAPIS, TrimMaterials.LAPIS),
      new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.AMETHYST, TrimMaterials.AMETHYST),
      new ItemModelGenerators.TrimMaterialData(MaterialAssetGroup.RESIN, TrimMaterials.RESIN)
   );
   private final ItemModelOutput itemModelOutput;
   private final BiConsumer<Identifier, ModelInstance> modelOutput;

   public static Identifier prefixForSlotTrim(String p_394902_) {
      return Identifier.withDefaultNamespace("trims/items/" + p_394902_ + "_trim");
   }

   public ItemModelGenerators(ItemModelOutput p_375677_, BiConsumer<Identifier, ModelInstance> p_377569_) {
      this.itemModelOutput = p_375677_;
      this.modelOutput = p_377569_;
   }

   private void declareCustomModelItem(Item p_376826_) {
      this.itemModelOutput.accept(p_376826_, ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(p_376826_)));
   }

   private Identifier createFlatItemModel(Item p_376313_, ModelTemplate p_376494_) {
      return p_376494_.create(ModelLocationUtils.getModelLocation(p_376313_), TextureMapping.layer0(p_376313_), this.modelOutput);
   }

   private void generateFlatItem(Item p_377153_, ModelTemplate p_375452_) {
      this.itemModelOutput.accept(p_377153_, ItemModelUtils.plainModel(this.createFlatItemModel(p_377153_, p_375452_)));
   }

   private Identifier createFlatItemModel(Item p_378447_, String p_455861_, ModelTemplate p_376080_) {
      return p_376080_.create(
         ModelLocationUtils.getModelLocation(p_378447_, p_455861_),
         TextureMapping.layer0(TextureMapping.getItemTexture(p_378447_, p_455861_)),
         this.modelOutput
      );
   }

   private Identifier createFlatItemModel(Item p_376880_, Item p_456317_, ModelTemplate p_375473_) {
      return p_375473_.create(ModelLocationUtils.getModelLocation(p_376880_), TextureMapping.layer0(p_456317_), this.modelOutput);
   }

   private void generateFlatItem(Item p_376380_, Item p_377414_, ModelTemplate p_375715_) {
      this.itemModelOutput.accept(p_376380_, ItemModelUtils.plainModel(this.createFlatItemModel(p_376380_, p_377414_, p_375715_)));
   }

   private void generateItemWithTintedOverlay(Item p_377767_, ItemTintSource p_376307_) {
      this.generateItemWithTintedOverlay(p_377767_, "_overlay", p_376307_);
   }

   private void generateItemWithTintedOverlay(Item p_376479_, String p_377630_, ItemTintSource p_375561_) {
      Identifier identifier = this.generateLayeredItem(p_376479_, TextureMapping.getItemTexture(p_376479_), TextureMapping.getItemTexture(p_376479_, p_377630_));
      this.itemModelOutput.accept(p_376479_, ItemModelUtils.tintedModel(identifier, BLANK_LAYER, p_375561_));
   }

   private void generateItemWithTintedBaseLayer(Item p_456909_, int p_450318_) {
      Identifier identifier = TextureMapping.getItemTexture(p_456909_);
      Identifier identifier1 = TextureMapping.getItemTexture(p_456909_, "_overlay");
      Identifier identifier2 = ModelLocationUtils.getModelLocation(p_456909_);
      ModelTemplates.TWO_LAYERED_ITEM.create(identifier2, TextureMapping.layered(identifier, identifier1), this.modelOutput);
      this.itemModelOutput.accept(p_456909_, ItemModelUtils.tintedModel(identifier2, new Dye(p_450318_)));
   }

   private List<RangeSelectItemModel.Entry> createCompassModels(Item p_378493_) {
      List<RangeSelectItemModel.Entry> list = new ArrayList<>();
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(p_378493_, "_16", ModelTemplates.FLAT_ITEM));
      list.add(ItemModelUtils.override(itemmodel$unbaked, 0.0F));

      for (int i = 1; i < 32; i++) {
         int j = Mth.positiveModulo(i - 16, 32);
         ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(
            this.createFlatItemModel(p_378493_, String.format(Locale.ROOT, "_%02d", j), ModelTemplates.FLAT_ITEM)
         );
         list.add(ItemModelUtils.override(itemmodel$unbaked1, i - 0.5F));
      }

      list.add(ItemModelUtils.override(itemmodel$unbaked, 31.5F));
      return list;
   }

   private void generateStandardCompassItem(Item p_377729_) {
      List<RangeSelectItemModel.Entry> list = this.createCompassModels(p_377729_);
      this.itemModelOutput
         .accept(
            p_377729_,
            ItemModelUtils.conditional(
               ItemModelUtils.hasComponent(DataComponents.LODESTONE_TRACKER),
               ItemModelUtils.rangeSelect(new CompassAngle(true, CompassAngleState.CompassTarget.LODESTONE), 32.0F, list),
               ItemModelUtils.rangeSelect(new CompassAngle(true, CompassAngleState.CompassTarget.SPAWN), 32.0F, list)
            )
         );
   }

   private void generateRecoveryCompassItem(Item p_375879_) {
      this.itemModelOutput
         .accept(
            p_375879_, ItemModelUtils.rangeSelect(new CompassAngle(true, CompassAngleState.CompassTarget.RECOVERY), 32.0F, this.createCompassModels(p_375879_))
         );
   }

   private void generateClockItem(Item p_376265_) {
      List<RangeSelectItemModel.Entry> list = new ArrayList<>();
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(p_376265_, "_00", ModelTemplates.FLAT_ITEM));
      list.add(ItemModelUtils.override(itemmodel$unbaked, 0.0F));

      for (int i = 1; i < 64; i++) {
         ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(
            this.createFlatItemModel(p_376265_, String.format(Locale.ROOT, "_%02d", i), ModelTemplates.FLAT_ITEM)
         );
         list.add(ItemModelUtils.override(itemmodel$unbaked1, i - 0.5F));
      }

      list.add(ItemModelUtils.override(itemmodel$unbaked, 63.5F));
      this.itemModelOutput
         .accept(
            p_376265_,
            ItemModelUtils.inOverworld(
               ItemModelUtils.rangeSelect(new Time(true, Time.TimeSource.DAYTIME), 64.0F, list),
               ItemModelUtils.rangeSelect(new Time(true, Time.TimeSource.RANDOM), 64.0F, list)
            )
         );
   }

   private Identifier generateLayeredItem(Item p_378743_, Identifier p_453875_, Identifier p_458508_) {
      return ModelTemplates.TWO_LAYERED_ITEM.create(p_378743_, TextureMapping.layered(p_453875_, p_458508_), this.modelOutput);
   }

   private Identifier generateLayeredItem(Identifier p_453115_, Identifier p_460812_, Identifier p_455048_) {
      return ModelTemplates.TWO_LAYERED_ITEM.create(p_453115_, TextureMapping.layered(p_460812_, p_455048_), this.modelOutput);
   }

   private void generateLayeredItem(Identifier p_459404_, Identifier p_458232_, Identifier p_458394_, Identifier p_458827_) {
      ModelTemplates.THREE_LAYERED_ITEM.create(p_459404_, TextureMapping.layered(p_458232_, p_458394_, p_458827_), this.modelOutput);
   }

   private void generateTrimmableItem(Item p_376312_, ResourceKey<EquipmentAsset> p_375739_, Identifier p_456185_, boolean p_377962_) {
      Identifier identifier = ModelLocationUtils.getModelLocation(p_376312_);
      Identifier identifier1 = TextureMapping.getItemTexture(p_376312_);
      Identifier identifier2 = TextureMapping.getItemTexture(p_376312_, "_overlay");
      List<SelectItemModel.SwitchCase<ResourceKey<TrimMaterial>>> list = new ArrayList<>(TRIM_MATERIAL_MODELS.size());

      for (ItemModelGenerators.TrimMaterialData itemmodelgenerators$trimmaterialdata : TRIM_MATERIAL_MODELS) {
         Identifier identifier3 = identifier.withSuffix("_" + itemmodelgenerators$trimmaterialdata.assets().base().suffix() + "_trim");
         Identifier identifier4 = p_456185_.withSuffix("_" + itemmodelgenerators$trimmaterialdata.assets().assetId(p_375739_).suffix());
         ItemModel.Unbaked itemmodel$unbaked;
         if (p_377962_) {
            this.generateLayeredItem(identifier3, identifier1, identifier2, identifier4);
            itemmodel$unbaked = ItemModelUtils.tintedModel(identifier3, new Dye(-6265536));
         } else {
            this.generateLayeredItem(identifier3, identifier1, identifier4);
            itemmodel$unbaked = ItemModelUtils.plainModel(identifier3);
         }

         list.add(ItemModelUtils.when(itemmodelgenerators$trimmaterialdata.materialKey, itemmodel$unbaked));
      }

      ItemModel.Unbaked itemmodel$unbaked1;
      if (p_377962_) {
         ModelTemplates.TWO_LAYERED_ITEM.create(identifier, TextureMapping.layered(identifier1, identifier2), this.modelOutput);
         itemmodel$unbaked1 = ItemModelUtils.tintedModel(identifier, new Dye(-6265536));
      } else {
         ModelTemplates.FLAT_ITEM.create(identifier, TextureMapping.layer0(identifier1), this.modelOutput);
         itemmodel$unbaked1 = ItemModelUtils.plainModel(identifier);
      }

      this.itemModelOutput.accept(p_376312_, ItemModelUtils.select(new TrimMaterialProperty(), itemmodel$unbaked1, list));
   }

   private void generateBundleModels(Item p_376224_) {
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(p_376224_, ModelTemplates.FLAT_ITEM));
      Identifier identifier = this.generateBundleCoverModel(p_376224_, ModelTemplates.BUNDLE_OPEN_BACK_INVENTORY, "_open_back");
      Identifier identifier1 = this.generateBundleCoverModel(p_376224_, ModelTemplates.BUNDLE_OPEN_FRONT_INVENTORY, "_open_front");
      ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.composite(
         ItemModelUtils.plainModel(identifier), new BundleSelectedItemSpecialRenderer.Unbaked(), ItemModelUtils.plainModel(identifier1)
      );
      ItemModel.Unbaked itemmodel$unbaked2 = ItemModelUtils.conditional(new BundleHasSelectedItem(), itemmodel$unbaked1, itemmodel$unbaked);
      this.itemModelOutput
         .accept(p_376224_, ItemModelUtils.select(new DisplayContext(), itemmodel$unbaked, ItemModelUtils.when(ItemDisplayContext.GUI, itemmodel$unbaked2)));
   }

   private Identifier generateBundleCoverModel(Item p_377759_, ModelTemplate p_377732_, String p_376771_) {
      Identifier identifier = TextureMapping.getItemTexture(p_377759_, p_376771_);
      return p_377732_.create(p_377759_, TextureMapping.layer0(identifier), this.modelOutput);
   }

   private void generateBow(Item p_376089_) {
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(p_376089_));
      ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(this.createFlatItemModel(p_376089_, "_pulling_0", ModelTemplates.BOW));
      ItemModel.Unbaked itemmodel$unbaked2 = ItemModelUtils.plainModel(this.createFlatItemModel(p_376089_, "_pulling_1", ModelTemplates.BOW));
      ItemModel.Unbaked itemmodel$unbaked3 = ItemModelUtils.plainModel(this.createFlatItemModel(p_376089_, "_pulling_2", ModelTemplates.BOW));
      this.itemModelOutput
         .accept(
            p_376089_,
            ItemModelUtils.conditional(
               ItemModelUtils.isUsingItem(),
               ItemModelUtils.rangeSelect(
                  new UseDuration(false),
                  0.05F,
                  itemmodel$unbaked1,
                  ItemModelUtils.override(itemmodel$unbaked2, 0.65F),
                  ItemModelUtils.override(itemmodel$unbaked3, 0.9F)
               ),
               itemmodel$unbaked
            )
         );
   }

   private void generateCrossbow(Item p_378673_) {
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(p_378673_));
      ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(this.createFlatItemModel(p_378673_, "_pulling_0", ModelTemplates.CROSSBOW));
      ItemModel.Unbaked itemmodel$unbaked2 = ItemModelUtils.plainModel(this.createFlatItemModel(p_378673_, "_pulling_1", ModelTemplates.CROSSBOW));
      ItemModel.Unbaked itemmodel$unbaked3 = ItemModelUtils.plainModel(this.createFlatItemModel(p_378673_, "_pulling_2", ModelTemplates.CROSSBOW));
      ItemModel.Unbaked itemmodel$unbaked4 = ItemModelUtils.plainModel(this.createFlatItemModel(p_378673_, "_arrow", ModelTemplates.CROSSBOW));
      ItemModel.Unbaked itemmodel$unbaked5 = ItemModelUtils.plainModel(this.createFlatItemModel(p_378673_, "_firework", ModelTemplates.CROSSBOW));
      this.itemModelOutput
         .accept(
            p_378673_,
            ItemModelUtils.select(
               new Charge(),
               ItemModelUtils.conditional(
                  ItemModelUtils.isUsingItem(),
                  ItemModelUtils.rangeSelect(
                     new CrossbowPull(),
                     itemmodel$unbaked1,
                     ItemModelUtils.override(itemmodel$unbaked2, 0.58F),
                     ItemModelUtils.override(itemmodel$unbaked3, 1.0F)
                  ),
                  itemmodel$unbaked
               ),
               ItemModelUtils.when(CrossbowItem.ChargeType.ARROW, itemmodel$unbaked4),
               ItemModelUtils.when(CrossbowItem.ChargeType.ROCKET, itemmodel$unbaked5)
            )
         );
   }

   private void generateBooleanDispatch(Item p_377310_, ConditionalItemModelProperty p_376519_, ItemModel.Unbaked p_376296_, ItemModel.Unbaked p_378712_) {
      this.itemModelOutput.accept(p_377310_, ItemModelUtils.conditional(p_376519_, p_376296_, p_378712_));
   }

   private void generateElytra(Item p_376521_) {
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(p_376521_, ModelTemplates.FLAT_ITEM));
      ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(this.createFlatItemModel(p_376521_, "_broken", ModelTemplates.FLAT_ITEM));
      this.generateBooleanDispatch(p_376521_, new Broken(), itemmodel$unbaked1, itemmodel$unbaked);
   }

   private void generateBrush(Item p_376591_) {
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(p_376591_));
      ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(p_376591_, "_brushing_0"));
      ItemModel.Unbaked itemmodel$unbaked2 = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(p_376591_, "_brushing_1"));
      ItemModel.Unbaked itemmodel$unbaked3 = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(p_376591_, "_brushing_2"));
      this.itemModelOutput
         .accept(
            p_376591_,
            ItemModelUtils.rangeSelect(
               new UseCycle(10.0F),
               0.1F,
               itemmodel$unbaked,
               ItemModelUtils.override(itemmodel$unbaked1, 0.25F),
               ItemModelUtils.override(itemmodel$unbaked2, 0.5F),
               ItemModelUtils.override(itemmodel$unbaked3, 0.75F)
            )
         );
   }

   private void generateFishingRod(Item p_377466_) {
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(p_377466_, ModelTemplates.FLAT_HANDHELD_ROD_ITEM));
      ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(this.createFlatItemModel(p_377466_, "_cast", ModelTemplates.FLAT_HANDHELD_ROD_ITEM));
      this.generateBooleanDispatch(p_377466_, new FishingRodCast(), itemmodel$unbaked1, itemmodel$unbaked);
   }

   private void generateGoatHorn(Item p_378813_) {
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(p_378813_));
      ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(ModelLocationUtils.decorateItemModelLocation("tooting_goat_horn"));
      this.generateBooleanDispatch(p_378813_, ItemModelUtils.isUsingItem(), itemmodel$unbaked1, itemmodel$unbaked);
   }

   private void generateShield(Item p_378111_) {
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.specialModel(ModelLocationUtils.getModelLocation(p_378111_), new ShieldSpecialRenderer.Unbaked());
      ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.specialModel(
         ModelLocationUtils.getModelLocation(p_378111_, "_blocking"), new ShieldSpecialRenderer.Unbaked()
      );
      this.generateBooleanDispatch(p_378111_, ItemModelUtils.isUsingItem(), itemmodel$unbaked1, itemmodel$unbaked);
   }

   private static ItemModel.Unbaked createFlatModelDispatch(ItemModel.Unbaked p_377503_, ItemModel.Unbaked p_377244_) {
      return ItemModelUtils.select(
         new DisplayContext(),
         p_377244_,
         ItemModelUtils.when(List.of(ItemDisplayContext.GUI, ItemDisplayContext.GROUND, ItemDisplayContext.FIXED, ItemDisplayContext.ON_SHELF), p_377503_)
      );
   }

   private void generateSpyglass(Item p_377890_) {
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(p_377890_, ModelTemplates.FLAT_ITEM));
      ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(p_377890_, "_in_hand"));
      this.itemModelOutput.accept(p_377890_, createFlatModelDispatch(itemmodel$unbaked, itemmodel$unbaked1));
   }

   private void generateTrident(Item p_376855_) {
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(p_376855_, ModelTemplates.FLAT_ITEM));
      ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.specialModel(
         ModelLocationUtils.getModelLocation(p_376855_, "_in_hand"), new TridentSpecialRenderer.Unbaked()
      );
      ItemModel.Unbaked itemmodel$unbaked2 = ItemModelUtils.specialModel(
         ModelLocationUtils.getModelLocation(p_376855_, "_throwing"), new TridentSpecialRenderer.Unbaked()
      );
      ItemModel.Unbaked itemmodel$unbaked3 = ItemModelUtils.conditional(ItemModelUtils.isUsingItem(), itemmodel$unbaked2, itemmodel$unbaked1);
      this.itemModelOutput.accept(p_376855_, createFlatModelDispatch(itemmodel$unbaked, itemmodel$unbaked3));
   }

   private void generateSpear(Item p_451090_) {
      ItemModel.Unbaked itemmodel$unbaked = ItemModelUtils.plainModel(this.createFlatItemModel(p_451090_, ModelTemplates.FLAT_ITEM));
      ItemModel.Unbaked itemmodel$unbaked1 = ItemModelUtils.plainModel(
         ModelTemplates.SPEAR_IN_HAND.create(p_451090_, TextureMapping.layer0(TextureMapping.getItemTexture(p_451090_, "_in_hand")), this.modelOutput)
      );
      this.itemModelOutput.accept(p_451090_, createFlatModelDispatch(itemmodel$unbaked, itemmodel$unbaked1), new ClientItem.Properties(true, false, 1.95F));
   }

   private void addPotionTint(Item p_376884_, Identifier p_451731_) {
      this.itemModelOutput.accept(p_376884_, ItemModelUtils.tintedModel(p_451731_, new Potion()));
   }

   private void generatePotion(Item p_375712_) {
      Identifier identifier = this.generateLayeredItem(
         p_375712_, ModelLocationUtils.decorateItemModelLocation("potion_overlay"), ModelLocationUtils.getModelLocation(p_375712_)
      );
      this.addPotionTint(p_375712_, identifier);
   }

   private void generateTippedArrow(Item p_377081_) {
      Identifier identifier = this.generateLayeredItem(
         p_377081_, ModelLocationUtils.getModelLocation(p_377081_, "_head"), ModelLocationUtils.getModelLocation(p_377081_, "_base")
      );
      this.addPotionTint(p_377081_, identifier);
   }

   private void generateDyedItem(Item p_377413_, int p_378189_) {
      Identifier identifier = this.createFlatItemModel(p_377413_, ModelTemplates.FLAT_ITEM);
      this.itemModelOutput.accept(p_377413_, ItemModelUtils.tintedModel(identifier, new Dye(p_378189_)));
   }

   private void generateTwoLayerDyedItem(Item p_376926_) {
      Identifier identifier = TextureMapping.getItemTexture(p_376926_);
      Identifier identifier1 = TextureMapping.getItemTexture(p_376926_, "_overlay");
      Identifier identifier2 = ModelTemplates.FLAT_ITEM.create(p_376926_, TextureMapping.layer0(identifier), this.modelOutput);
      Identifier identifier3 = ModelLocationUtils.getModelLocation(p_376926_, "_dyed");
      ModelTemplates.TWO_LAYERED_ITEM.create(identifier3, TextureMapping.layered(identifier, identifier1), this.modelOutput);
      this.itemModelOutput
         .accept(
            p_376926_,
            ItemModelUtils.conditional(
               ItemModelUtils.hasComponent(DataComponents.DYED_COLOR),
               ItemModelUtils.tintedModel(identifier3, BLANK_LAYER, new Dye(0)),
               ItemModelUtils.plainModel(identifier2)
            )
         );
   }

   public void run() {
      this.generateFlatItem(Items.ACACIA_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CHERRY_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ACACIA_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CHERRY_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.AMETHYST_SHARD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.APPLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ARMADILLO_SCUTE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ARMOR_STAND, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ARROW, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BAKED_POTATO, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BAMBOO, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.BEEF, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BEETROOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BEETROOT_SOUP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BIRCH_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BIRCH_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLACK_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLAZE_POWDER, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLAZE_ROD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.BLUE_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BONE_MEAL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BORDURE_INDENTED_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BOOK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BOWL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BREAD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BRICK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BREEZE_ROD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.BROWN_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CARROT_ON_A_STICK, ModelTemplates.FLAT_HANDHELD_ROD_ITEM);
      this.generateFlatItem(Items.WARPED_FUNGUS_ON_A_STICK, ModelTemplates.FLAT_HANDHELD_ROD_ITEM);
      this.generateFlatItem(Items.CHARCOAL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CHEST_MINECART, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CHICKEN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CHORUS_FRUIT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CLAY_BALL, ModelTemplates.FLAT_ITEM);
      this.generateClockItem(Items.CLOCK);
      this.generateFlatItem(Items.COAL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COD_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COMMAND_BLOCK_MINECART, ModelTemplates.FLAT_ITEM);
      this.generateStandardCompassItem(Items.COMPASS);
      this.generateRecoveryCompassItem(Items.RECOVERY_COMPASS);
      this.generateFlatItem(Items.COOKED_BEEF, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_CHICKEN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_COD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_MUTTON, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_PORKCHOP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_RABBIT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKED_SALMON, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COOKIE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RAW_COPPER, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COPPER_NUGGET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COPPER_INGOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COPPER_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.COPPER_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.COPPER_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.COPPER_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.COPPER_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.COPPER_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COPPER_NAUTILUS_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CREEPER_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CYAN_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DARK_OAK_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DARK_OAK_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DIAMOND, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DIAMOND_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.DIAMOND_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.DIAMOND_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DIAMOND_NAUTILUS_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DIAMOND_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.DIAMOND_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.DIAMOND_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.DRAGON_BREATH, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DRIED_KELP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLUE_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BROWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.EMERALD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ENCHANTED_BOOK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ENDER_EYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ENDER_PEARL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.END_CRYSTAL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.EXPERIENCE_BOTTLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FERMENTED_SPIDER_EYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FIELD_MASONED_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FIREWORK_ROCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FIRE_CHARGE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FLINT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FLINT_AND_STEEL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FLOW_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FLOWER_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FURNACE_MINECART, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GHAST_TEAR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLASS_BOTTLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLISTERING_MELON_SLICE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLOBE_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLOW_BERRIES, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLOWSTONE_DUST, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLOW_INK_SAC, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLOW_ITEM_FRAME, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RAW_GOLD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOLDEN_APPLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOLDEN_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.GOLDEN_CARROT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOLDEN_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.GOLDEN_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOLDEN_NAUTILUS_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOLDEN_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.GOLDEN_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.GOLDEN_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.GOLD_INGOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOLD_NUGGET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GRAY_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GREEN_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GUNPOWDER, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GUSTER_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HEART_OF_THE_SEA, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HONEYCOMB, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HONEY_BOTTLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HOPPER_MINECART, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.INK_SAC, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RAW_IRON, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.IRON_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.IRON_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.IRON_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.IRON_NAUTILUS_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.IRON_INGOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.IRON_NUGGET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.IRON_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.IRON_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.IRON_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.ITEM_FRAME, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.JUNGLE_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.JUNGLE_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.KNOWLEDGE_BOOK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LAPIS_LAZULI, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LAVA_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LEATHER, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LIGHT_BLUE_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LIGHT_GRAY_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LIME_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MAGENTA_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MAGMA_CREAM, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MANGROVE_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MANGROVE_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BAMBOO_RAFT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BAMBOO_CHEST_RAFT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MAP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MELON_SLICE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MILK_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MINECART, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MOJANG_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MUSHROOM_STEW, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DISC_FRAGMENT_5, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MUSIC_DISC_11, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_13, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_BLOCKS, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_CAT, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_CHIRP, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_CREATOR, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_CREATOR_MUSIC_BOX, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_FAR, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_LAVA_CHICKEN, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_MALL, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_MELLOHI, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_PIGSTEP, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_PRECIPICE, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_STAL, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_STRAD, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_WAIT, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_WARD, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_OTHERSIDE, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_RELIC, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_5, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUSIC_DISC_TEARS, ModelTemplates.MUSIC_DISC);
      this.generateFlatItem(Items.MUTTON, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NAME_TAG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NAUTILUS_SHELL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHERITE_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.NETHERITE_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.NETHERITE_INGOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHERITE_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.NETHERITE_SCRAP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHERITE_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.NETHERITE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.NETHERITE_NAUTILUS_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHERITE_HORSE_ARMOR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHER_BRICK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RESIN_BRICK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHER_STAR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.OAK_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.OAK_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ORANGE_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PAINTING, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PALE_OAK_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PALE_OAK_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PAPER, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PHANTOM_MEMBRANE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PIGLIN_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PINK_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.POISONOUS_POTATO, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.POPPED_CHORUS_FRUIT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PORKCHOP, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.POWDER_SNOW_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PRISMARINE_CRYSTALS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PRISMARINE_SHARD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PUFFERFISH, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PUFFERFISH_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PUMPKIN_PIE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PURPLE_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.QUARTZ, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RABBIT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RABBIT_FOOT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RABBIT_HIDE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RABBIT_STEW, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RED_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ROTTEN_FLESH, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SADDLE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SALMON, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SALMON_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TURTLE_SCUTE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHEARS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHULKER_SHELL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SKULL_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SLIME_BALL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SNOWBALL, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ECHO_SHARD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPECTRAL_ARROW, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPIDER_EYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPRUCE_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPRUCE_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.STICK, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.STONE_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.STONE_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.STONE_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.STONE_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.STONE_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.SUGAR, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SUSPICIOUS_STEW, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TNT_MINECART, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TOTEM_OF_UNDYING, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TROPICAL_FISH, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TROPICAL_FISH_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.AXOLOTL_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TADPOLE_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WATER_BUCKET, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WHEAT, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WHITE_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WIND_CHARGE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MACE, ModelTemplates.FLAT_HANDHELD_MACE_ITEM);
      this.generateFlatItem(Items.WOODEN_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.WOODEN_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.WOODEN_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.WOODEN_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.WOODEN_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.WRITABLE_BOOK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WRITTEN_BOOK, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.YELLOW_DYE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DEBUG_STICK, Items.STICK, ModelTemplates.FLAT_HANDHELD_ITEM);
      this.generateFlatItem(Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE, ModelTemplates.FLAT_ITEM);
      this.generateTrimmableItem(Items.TURTLE_HELMET, EquipmentAssets.TURTLE_SCUTE, TRIM_PREFIX_HELMET, false);
      this.generateTrimmableItem(Items.LEATHER_HELMET, EquipmentAssets.LEATHER, TRIM_PREFIX_HELMET, true);
      this.generateTrimmableItem(Items.LEATHER_CHESTPLATE, EquipmentAssets.LEATHER, TRIM_PREFIX_CHESTPLATE, true);
      this.generateTrimmableItem(Items.LEATHER_LEGGINGS, EquipmentAssets.LEATHER, TRIM_PREFIX_LEGGINGS, true);
      this.generateTrimmableItem(Items.LEATHER_BOOTS, EquipmentAssets.LEATHER, TRIM_PREFIX_BOOTS, true);
      this.generateTrimmableItem(Items.COPPER_HELMET, EquipmentAssets.COPPER, TRIM_PREFIX_HELMET, false);
      this.generateTrimmableItem(Items.COPPER_CHESTPLATE, EquipmentAssets.COPPER, TRIM_PREFIX_CHESTPLATE, false);
      this.generateTrimmableItem(Items.COPPER_LEGGINGS, EquipmentAssets.COPPER, TRIM_PREFIX_LEGGINGS, false);
      this.generateTrimmableItem(Items.COPPER_BOOTS, EquipmentAssets.COPPER, TRIM_PREFIX_BOOTS, false);
      this.generateTrimmableItem(Items.CHAINMAIL_HELMET, EquipmentAssets.CHAINMAIL, TRIM_PREFIX_HELMET, false);
      this.generateTrimmableItem(Items.CHAINMAIL_CHESTPLATE, EquipmentAssets.CHAINMAIL, TRIM_PREFIX_CHESTPLATE, false);
      this.generateTrimmableItem(Items.CHAINMAIL_LEGGINGS, EquipmentAssets.CHAINMAIL, TRIM_PREFIX_LEGGINGS, false);
      this.generateTrimmableItem(Items.CHAINMAIL_BOOTS, EquipmentAssets.CHAINMAIL, TRIM_PREFIX_BOOTS, false);
      this.generateTrimmableItem(Items.IRON_HELMET, EquipmentAssets.IRON, TRIM_PREFIX_HELMET, false);
      this.generateTrimmableItem(Items.IRON_CHESTPLATE, EquipmentAssets.IRON, TRIM_PREFIX_CHESTPLATE, false);
      this.generateTrimmableItem(Items.IRON_LEGGINGS, EquipmentAssets.IRON, TRIM_PREFIX_LEGGINGS, false);
      this.generateTrimmableItem(Items.IRON_BOOTS, EquipmentAssets.IRON, TRIM_PREFIX_BOOTS, false);
      this.generateTrimmableItem(Items.DIAMOND_HELMET, EquipmentAssets.DIAMOND, TRIM_PREFIX_HELMET, false);
      this.generateTrimmableItem(Items.DIAMOND_CHESTPLATE, EquipmentAssets.DIAMOND, TRIM_PREFIX_CHESTPLATE, false);
      this.generateTrimmableItem(Items.DIAMOND_LEGGINGS, EquipmentAssets.DIAMOND, TRIM_PREFIX_LEGGINGS, false);
      this.generateTrimmableItem(Items.DIAMOND_BOOTS, EquipmentAssets.DIAMOND, TRIM_PREFIX_BOOTS, false);
      this.generateTrimmableItem(Items.GOLDEN_HELMET, EquipmentAssets.GOLD, TRIM_PREFIX_HELMET, false);
      this.generateTrimmableItem(Items.GOLDEN_CHESTPLATE, EquipmentAssets.GOLD, TRIM_PREFIX_CHESTPLATE, false);
      this.generateTrimmableItem(Items.GOLDEN_LEGGINGS, EquipmentAssets.GOLD, TRIM_PREFIX_LEGGINGS, false);
      this.generateTrimmableItem(Items.GOLDEN_BOOTS, EquipmentAssets.GOLD, TRIM_PREFIX_BOOTS, false);
      this.generateTrimmableItem(Items.NETHERITE_HELMET, EquipmentAssets.NETHERITE, TRIM_PREFIX_HELMET, false);
      this.generateTrimmableItem(Items.NETHERITE_CHESTPLATE, EquipmentAssets.NETHERITE, TRIM_PREFIX_CHESTPLATE, false);
      this.generateTrimmableItem(Items.NETHERITE_LEGGINGS, EquipmentAssets.NETHERITE, TRIM_PREFIX_LEGGINGS, false);
      this.generateTrimmableItem(Items.NETHERITE_BOOTS, EquipmentAssets.NETHERITE, TRIM_PREFIX_BOOTS, false);
      this.generateItemWithTintedBaseLayer(Items.LEATHER_HORSE_ARMOR, -6265536);
      this.generateFlatItem(Items.ANGLER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ARCHER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ARMS_UP_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLADE_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BREWER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BURN_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DANGER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.EXPLORER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FLOW_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FRIEND_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GUSTER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HEART_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HEARTBREAK_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HOWL_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MINER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MOURNER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PLENTY_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PRIZE_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SCRAPE_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHEAF_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHELTER_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SKULL_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SNORT_POTTERY_SHERD, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TRIAL_KEY, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.OMINOUS_TRIAL_KEY, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.OMINOUS_BOTTLE, ModelTemplates.FLAT_ITEM);
      this.generateItemWithTintedOverlay(Items.FIREWORK_STAR, new Firework());
      this.generateItemWithTintedOverlay(Items.FILLED_MAP, "_markings", new MapColor());
      this.generateBundleModels(Items.BUNDLE);
      this.generateBundleModels(Items.BLACK_BUNDLE);
      this.generateBundleModels(Items.WHITE_BUNDLE);
      this.generateBundleModels(Items.GRAY_BUNDLE);
      this.generateBundleModels(Items.LIGHT_GRAY_BUNDLE);
      this.generateBundleModels(Items.LIGHT_BLUE_BUNDLE);
      this.generateBundleModels(Items.BLUE_BUNDLE);
      this.generateBundleModels(Items.CYAN_BUNDLE);
      this.generateBundleModels(Items.YELLOW_BUNDLE);
      this.generateBundleModels(Items.RED_BUNDLE);
      this.generateBundleModels(Items.PURPLE_BUNDLE);
      this.generateBundleModels(Items.MAGENTA_BUNDLE);
      this.generateBundleModels(Items.PINK_BUNDLE);
      this.generateBundleModels(Items.GREEN_BUNDLE);
      this.generateBundleModels(Items.LIME_BUNDLE);
      this.generateBundleModels(Items.BROWN_BUNDLE);
      this.generateBundleModels(Items.ORANGE_BUNDLE);
      this.generateSpyglass(Items.SPYGLASS);
      this.generateTrident(Items.TRIDENT);
      this.generateTwoLayerDyedItem(Items.WOLF_ARMOR);
      this.generateFlatItem(Items.WHITE_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ORANGE_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MAGENTA_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LIGHT_BLUE_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.YELLOW_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LIME_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PINK_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GRAY_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LIGHT_GRAY_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CYAN_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PURPLE_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLUE_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BROWN_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GREEN_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RED_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLACK_HARNESS, ModelTemplates.FLAT_ITEM);
      this.generateBow(Items.BOW);
      this.generateCrossbow(Items.CROSSBOW);
      this.generateElytra(Items.ELYTRA);
      this.generateBrush(Items.BRUSH);
      this.generateFishingRod(Items.FISHING_ROD);
      this.generateGoatHorn(Items.GOAT_HORN);
      this.generateShield(Items.SHIELD);
      this.generateSpear(Items.WOODEN_SPEAR);
      this.generateSpear(Items.STONE_SPEAR);
      this.generateSpear(Items.COPPER_SPEAR);
      this.generateSpear(Items.GOLDEN_SPEAR);
      this.generateSpear(Items.IRON_SPEAR);
      this.generateSpear(Items.DIAMOND_SPEAR);
      this.generateSpear(Items.NETHERITE_SPEAR);
      this.generateTippedArrow(Items.TIPPED_ARROW);
      this.generatePotion(Items.POTION);
      this.generatePotion(Items.SPLASH_POTION);
      this.generatePotion(Items.LINGERING_POTION);
      this.generateFlatItem(Items.CHICKEN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COW_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PIG_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHEEP_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CAMEL_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DONKEY_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HORSE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MULE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CAT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PARROT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WOLF_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ARMADILLO_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BAT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BEE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FOX_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GOAT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.LLAMA_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.OCELOT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PANDA_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.POLAR_BEAR_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RABBIT_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.AXOLOTL_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COD_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DOLPHIN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.FROG_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GLOW_SQUID_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.NAUTILUS_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PUFFERFISH_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SALMON_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SQUID_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TADPOLE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TROPICAL_FISH_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TURTLE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ALLAY_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MOOSHROOM_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SNIFFER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.COPPER_GOLEM_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.IRON_GOLEM_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SNOW_GOLEM_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.TRADER_LLAMA_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.VILLAGER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WANDERING_TRADER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BOGGED_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CAMEL_HUSK_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.DROWNED_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HUSK_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PARCHED_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SKELETON_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SKELETON_HORSE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.STRAY_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WITHER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WITHER_SKELETON_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ZOMBIE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ZOMBIE_HORSE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ZOMBIE_NAUTILUS_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ZOMBIE_VILLAGER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CAVE_SPIDER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SPIDER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BREEZE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CREAKING_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.CREEPER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ELDER_GUARDIAN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GUARDIAN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PHANTOM_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SILVERFISH_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SLIME_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WARDEN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.WITCH_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.EVOKER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PILLAGER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.RAVAGER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.VEX_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.VINDICATOR_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.BLAZE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.GHAST_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HAPPY_GHAST_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.HOGLIN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.MAGMA_CUBE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PIGLIN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.PIGLIN_BRUTE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.STRIDER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ZOGLIN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ZOMBIFIED_PIGLIN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ENDER_DRAGON_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ENDERMAN_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.ENDERMITE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.generateFlatItem(Items.SHULKER_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
      this.declareCustomModelItem(Items.AIR);
      this.declareCustomModelItem(Items.AMETHYST_CLUSTER);
      this.declareCustomModelItem(Items.SMALL_AMETHYST_BUD);
      this.declareCustomModelItem(Items.MEDIUM_AMETHYST_BUD);
      this.declareCustomModelItem(Items.LARGE_AMETHYST_BUD);
      this.declareCustomModelItem(Items.SMALL_DRIPLEAF);
      this.declareCustomModelItem(Items.BIG_DRIPLEAF);
      this.declareCustomModelItem(Items.HANGING_ROOTS);
      this.declareCustomModelItem(Items.POINTED_DRIPSTONE);
      this.declareCustomModelItem(Items.BONE);
      this.declareCustomModelItem(Items.COD);
      this.declareCustomModelItem(Items.FEATHER);
      this.declareCustomModelItem(Items.LEAD);
   }

   @OnlyIn(Dist.CLIENT)
   public record TrimMaterialData(MaterialAssetGroup assets, ResourceKey<TrimMaterial> materialKey) {
   }
}
