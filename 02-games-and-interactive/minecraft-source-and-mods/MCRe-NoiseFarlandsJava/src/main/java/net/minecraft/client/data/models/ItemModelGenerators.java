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
import net.minecraft.client.resources.model.sprite.Material;
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

    public static Identifier prefixForSlotTrim(final String slotName) {
        return Identifier.withDefaultNamespace("trims/items/" + slotName + "_trim");
    }

    public ItemModelGenerators(final ItemModelOutput itemModelOutput, final BiConsumer<Identifier, ModelInstance> modelOutput) {
        this.itemModelOutput = itemModelOutput;
        this.modelOutput = modelOutput;
    }

    private void declareCustomModelItem(final Item item) {
        this.itemModelOutput.accept(item, ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item)));
    }

    private Identifier createFlatItemModel(final Item item, final ModelTemplate template) {
        return template.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(item), this.modelOutput);
    }

    private void generateFlatItem(final Item item, final ModelTemplate template) {
        this.itemModelOutput.accept(item, ItemModelUtils.plainModel(this.createFlatItemModel(item, template)));
    }

    private Identifier createFlatItemModel(final Item item, final String suffix, final ModelTemplate template) {
        return template.create(
            ModelLocationUtils.getModelLocation(item, suffix), TextureMapping.layer0(TextureMapping.getItemTexture(item, suffix)), this.modelOutput
        );
    }

    private Identifier createFlatItemModel(final Item item, final Item textureDonor, final ModelTemplate template) {
        return template.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(textureDonor), this.modelOutput);
    }

    private void generateFlatItem(final Item item, final Item textureDonor, final ModelTemplate template) {
        this.itemModelOutput.accept(item, ItemModelUtils.plainModel(this.createFlatItemModel(item, textureDonor, template)));
    }

    private void generateItemWithTintedOverlay(final Item item, final ItemTintSource overlayTint) {
        this.generateItemWithTintedOverlay(item, "_overlay", overlayTint);
    }

    private void generateItemWithTintedOverlay(final Item item, final String overlaySuffix, final ItemTintSource overlayTint) {
        Identifier model = this.generateLayeredItem(item, TextureMapping.getItemTexture(item), TextureMapping.getItemTexture(item, overlaySuffix));
        this.itemModelOutput.accept(item, ItemModelUtils.tintedModel(model, BLANK_LAYER, overlayTint));
    }

    private void generateItemWithTintedBaseLayer(final Item item, final int defaultColor) {
        Material tintedLayer = TextureMapping.getItemTexture(item);
        Material untintedLayer = TextureMapping.getItemTexture(item, "_overlay");
        Identifier model = ModelLocationUtils.getModelLocation(item);
        ModelTemplates.TWO_LAYERED_ITEM.create(model, TextureMapping.layered(tintedLayer, untintedLayer), this.modelOutput);
        this.itemModelOutput.accept(item, ItemModelUtils.tintedModel(model, new Dye(defaultColor)));
    }

    private List<RangeSelectItemModel.Entry> createCompassModels(final Item compass) {
        List<RangeSelectItemModel.Entry> overrides = new ArrayList<>();
        ItemModel.Unbaked base = ItemModelUtils.plainModel(this.createFlatItemModel(compass, "_16", ModelTemplates.FLAT_ITEM));
        overrides.add(ItemModelUtils.override(base, 0.0F));

        for (int i = 1; i < 32; i++) {
            int textureIndex = Mth.positiveModulo(i - 16, 32);
            ItemModel.Unbaked overrideModel = ItemModelUtils.plainModel(
                this.createFlatItemModel(compass, String.format(Locale.ROOT, "_%02d", textureIndex), ModelTemplates.FLAT_ITEM)
            );
            overrides.add(ItemModelUtils.override(overrideModel, i - 0.5F));
        }

        overrides.add(ItemModelUtils.override(base, 31.5F));
        return overrides;
    }

    private void generateStandardCompassItem(final Item compass) {
        List<RangeSelectItemModel.Entry> overrides = this.createCompassModels(compass);
        this.itemModelOutput
            .accept(
                compass,
                ItemModelUtils.conditional(
                    ItemModelUtils.hasComponent(DataComponents.LODESTONE_TRACKER),
                    ItemModelUtils.rangeSelect(new CompassAngle(true, CompassAngleState.CompassTarget.LODESTONE), 32.0F, overrides),
                    ItemModelUtils.rangeSelect(new CompassAngle(true, CompassAngleState.CompassTarget.SPAWN), 32.0F, overrides)
                )
            );
    }

    private void generateRecoveryCompassItem(final Item compass) {
        this.itemModelOutput
            .accept(
                compass, ItemModelUtils.rangeSelect(new CompassAngle(true, CompassAngleState.CompassTarget.RECOVERY), 32.0F, this.createCompassModels(compass))
            );
    }

    private void generateClockItem(final Item clock) {
        List<RangeSelectItemModel.Entry> overrides = new ArrayList<>();
        ItemModel.Unbaked base = ItemModelUtils.plainModel(this.createFlatItemModel(clock, "_00", ModelTemplates.FLAT_ITEM));
        overrides.add(ItemModelUtils.override(base, 0.0F));

        for (int i = 1; i < 64; i++) {
            ItemModel.Unbaked overrideModel = ItemModelUtils.plainModel(
                this.createFlatItemModel(clock, String.format(Locale.ROOT, "_%02d", i), ModelTemplates.FLAT_ITEM)
            );
            overrides.add(ItemModelUtils.override(overrideModel, i - 0.5F));
        }

        overrides.add(ItemModelUtils.override(base, 63.5F));
        this.itemModelOutput
            .accept(
                clock,
                ItemModelUtils.inOverworld(
                    ItemModelUtils.rangeSelect(new Time(true, Time.TimeSource.DAYTIME), 64.0F, overrides),
                    ItemModelUtils.rangeSelect(new Time(true, Time.TimeSource.RANDOM), 64.0F, overrides)
                )
            );
    }

    private Identifier generateLayeredItem(final Item target, final Material layer0, final Material layer1) {
        return ModelTemplates.TWO_LAYERED_ITEM.create(target, TextureMapping.layered(layer0, layer1), this.modelOutput);
    }

    private Identifier generateLayeredItem(final Identifier target, final Material layer0, final Material layer1) {
        return ModelTemplates.TWO_LAYERED_ITEM.create(target, TextureMapping.layered(layer0, layer1), this.modelOutput);
    }

    private void generateLayeredItem(final Identifier target, final Material layer0, final Material layer1, final Material layer2) {
        ModelTemplates.THREE_LAYERED_ITEM.create(target, TextureMapping.layered(layer0, layer1, layer2), this.modelOutput);
    }

    private void generateTrimmableItem(
        final Item armor, final ResourceKey<EquipmentAsset> equipmentAssetId, final Identifier slotTrimPrefix, final boolean hasDyedLayer
    ) {
        Identifier modelLocation = ModelLocationUtils.getModelLocation(armor);
        Material itemTexture = TextureMapping.getItemTexture(armor);
        Material overlayTexture = TextureMapping.getItemTexture(armor, "_overlay");
        List<SelectItemModel.SwitchCase<ResourceKey<TrimMaterial>>> cases = new ArrayList<>(TRIM_MATERIAL_MODELS.size());

        for (ItemModelGenerators.TrimMaterialData material : TRIM_MATERIAL_MODELS) {
            Identifier trimModelLocation = modelLocation.withSuffix("_" + material.assets().base().suffix() + "_trim");
            Material trimOverlayTexture = new Material(slotTrimPrefix.withSuffix("_" + material.assets().assetId(equipmentAssetId).suffix()));
            ItemModel.Unbaked trimModel;
            if (hasDyedLayer) {
                this.generateLayeredItem(trimModelLocation, itemTexture, overlayTexture, trimOverlayTexture);
                trimModel = ItemModelUtils.tintedModel(trimModelLocation, new Dye(-6265536));
            } else {
                this.generateLayeredItem(trimModelLocation, itemTexture, trimOverlayTexture);
                trimModel = ItemModelUtils.plainModel(trimModelLocation);
            }

            cases.add(ItemModelUtils.when(material.materialKey, trimModel));
        }

        ItemModel.Unbaked untrimmedModel;
        if (hasDyedLayer) {
            ModelTemplates.TWO_LAYERED_ITEM.create(modelLocation, TextureMapping.layered(itemTexture, overlayTexture), this.modelOutput);
            untrimmedModel = ItemModelUtils.tintedModel(modelLocation, new Dye(-6265536));
        } else {
            ModelTemplates.FLAT_ITEM.create(modelLocation, TextureMapping.layer0(itemTexture), this.modelOutput);
            untrimmedModel = ItemModelUtils.plainModel(modelLocation);
        }

        this.itemModelOutput.accept(armor, ItemModelUtils.select(new TrimMaterialProperty(), untrimmedModel, cases));
    }

    private void generateBundleModels(final Item bundle) {
        ItemModel.Unbaked closedModel = ItemModelUtils.plainModel(this.createFlatItemModel(bundle, ModelTemplates.FLAT_ITEM));
        Identifier openBackCover = this.generateBundleCoverModel(bundle, ModelTemplates.BUNDLE_OPEN_BACK_INVENTORY, "_open_back");
        Identifier openFrontCover = this.generateBundleCoverModel(bundle, ModelTemplates.BUNDLE_OPEN_FRONT_INVENTORY, "_open_front");
        ItemModel.Unbaked openModel = ItemModelUtils.composite(
            ItemModelUtils.plainModel(openBackCover), new BundleSelectedItemSpecialRenderer.Unbaked(), ItemModelUtils.plainModel(openFrontCover)
        );
        ItemModel.Unbaked inGuiModel = ItemModelUtils.conditional(new BundleHasSelectedItem(), openModel, closedModel);
        this.itemModelOutput.accept(bundle, ItemModelUtils.select(new DisplayContext(), closedModel, ItemModelUtils.when(ItemDisplayContext.GUI, inGuiModel)));
    }

    private Identifier generateBundleCoverModel(final Item item, final ModelTemplate template, final String suffix) {
        Material texture = TextureMapping.getItemTexture(item, suffix);
        return template.create(item, TextureMapping.layer0(texture), this.modelOutput);
    }

    private void generateBow(final Item item) {
        ItemModel.Unbaked bowModel = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item));
        ItemModel.Unbaked pulling0 = ItemModelUtils.plainModel(this.createFlatItemModel(item, "_pulling_0", ModelTemplates.BOW));
        ItemModel.Unbaked pulling1 = ItemModelUtils.plainModel(this.createFlatItemModel(item, "_pulling_1", ModelTemplates.BOW));
        ItemModel.Unbaked pulling2 = ItemModelUtils.plainModel(this.createFlatItemModel(item, "_pulling_2", ModelTemplates.BOW));
        this.itemModelOutput
            .accept(
                item,
                ItemModelUtils.conditional(
                    ItemModelUtils.isUsingItem(),
                    ItemModelUtils.rangeSelect(
                        new UseDuration(false), 0.05F, pulling0, ItemModelUtils.override(pulling1, 0.65F), ItemModelUtils.override(pulling2, 0.9F)
                    ),
                    bowModel
                )
            );
    }

    private void generateCrossbow(final Item item) {
        ItemModel.Unbaked crossbowModel = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item));
        ItemModel.Unbaked pulling0 = ItemModelUtils.plainModel(this.createFlatItemModel(item, "_pulling_0", ModelTemplates.CROSSBOW));
        ItemModel.Unbaked pulling1 = ItemModelUtils.plainModel(this.createFlatItemModel(item, "_pulling_1", ModelTemplates.CROSSBOW));
        ItemModel.Unbaked pulling2 = ItemModelUtils.plainModel(this.createFlatItemModel(item, "_pulling_2", ModelTemplates.CROSSBOW));
        ItemModel.Unbaked loadedArrow = ItemModelUtils.plainModel(this.createFlatItemModel(item, "_arrow", ModelTemplates.CROSSBOW));
        ItemModel.Unbaked loadedFirework = ItemModelUtils.plainModel(this.createFlatItemModel(item, "_firework", ModelTemplates.CROSSBOW));
        this.itemModelOutput
            .accept(
                item,
                ItemModelUtils.select(
                    new Charge(),
                    ItemModelUtils.conditional(
                        ItemModelUtils.isUsingItem(),
                        ItemModelUtils.rangeSelect(
                            new CrossbowPull(), pulling0, ItemModelUtils.override(pulling1, 0.58F), ItemModelUtils.override(pulling2, 1.0F)
                        ),
                        crossbowModel
                    ),
                    ItemModelUtils.when(CrossbowItem.ChargeType.ARROW, loadedArrow),
                    ItemModelUtils.when(CrossbowItem.ChargeType.ROCKET, loadedFirework)
                )
            );
    }

    private void generateBooleanDispatch(
        final Item item, final ConditionalItemModelProperty property, final ItemModel.Unbaked modelOnTrue, final ItemModel.Unbaked modelOnFalse
    ) {
        this.itemModelOutput.accept(item, ItemModelUtils.conditional(property, modelOnTrue, modelOnFalse));
    }

    private void generateElytra(final Item item) {
        ItemModel.Unbaked normalElytra = ItemModelUtils.plainModel(this.createFlatItemModel(item, ModelTemplates.FLAT_ITEM));
        ItemModel.Unbaked brokenElytra = ItemModelUtils.plainModel(this.createFlatItemModel(item, "_broken", ModelTemplates.FLAT_ITEM));
        this.generateBooleanDispatch(item, new Broken(), brokenElytra, normalElytra);
    }

    private void generateBrush(final Item item) {
        ItemModel.Unbaked base = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item));
        ItemModel.Unbaked brushing0 = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item, "_brushing_0"));
        ItemModel.Unbaked brushing1 = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item, "_brushing_1"));
        ItemModel.Unbaked brushing2 = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item, "_brushing_2"));
        this.itemModelOutput
            .accept(
                item,
                ItemModelUtils.rangeSelect(
                    new UseCycle(10.0F),
                    0.1F,
                    base,
                    ItemModelUtils.override(brushing0, 0.25F),
                    ItemModelUtils.override(brushing1, 0.5F),
                    ItemModelUtils.override(brushing2, 0.75F)
                )
            );
    }

    private void generateFishingRod(final Item item) {
        ItemModel.Unbaked normal = ItemModelUtils.plainModel(this.createFlatItemModel(item, ModelTemplates.FLAT_HANDHELD_ROD_ITEM));
        ItemModel.Unbaked cast = ItemModelUtils.plainModel(this.createFlatItemModel(item, "_cast", ModelTemplates.FLAT_HANDHELD_ROD_ITEM));
        this.generateBooleanDispatch(item, new FishingRodCast(), cast, normal);
    }

    private void generateGoatHorn(final Item item) {
        ItemModel.Unbaked normal = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item));
        ItemModel.Unbaked tooting = ItemModelUtils.plainModel(ModelLocationUtils.decorateItemModelLocation("tooting_goat_horn"));
        this.generateBooleanDispatch(item, ItemModelUtils.isUsingItem(), tooting, normal);
    }

    private void generateShield(final Item item) {
        ItemModel.Unbaked normal = ItemModelUtils.specialModel(ModelLocationUtils.getModelLocation(item), new ShieldSpecialRenderer.Unbaked());
        ItemModel.Unbaked blocking = ItemModelUtils.specialModel(ModelLocationUtils.getModelLocation(item, "_blocking"), new ShieldSpecialRenderer.Unbaked());
        this.itemModelOutput
            .accept(item, ItemModelUtils.conditional(ShieldSpecialRenderer.DEFAULT_TRANSFORMATION, ItemModelUtils.isUsingItem(), blocking, normal));
    }

    private static ItemModel.Unbaked createFlatModelDispatch(final ItemModel.Unbaked flatModel, final ItemModel.Unbaked inHandModel) {
        return ItemModelUtils.select(
            new DisplayContext(),
            inHandModel,
            ItemModelUtils.when(List.of(ItemDisplayContext.GUI, ItemDisplayContext.GROUND, ItemDisplayContext.FIXED, ItemDisplayContext.ON_SHELF), flatModel)
        );
    }

    private void generateSpyglass(final Item item) {
        ItemModel.Unbaked flatModel = ItemModelUtils.plainModel(this.createFlatItemModel(item, ModelTemplates.FLAT_ITEM));
        ItemModel.Unbaked inHandModel = ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item, "_in_hand"));
        this.itemModelOutput.accept(item, createFlatModelDispatch(flatModel, inHandModel));
    }

    private void generateTrident(final Item item) {
        ItemModel.Unbaked flatModel = ItemModelUtils.plainModel(this.createFlatItemModel(item, ModelTemplates.FLAT_ITEM));
        ItemModel.Unbaked inHandNormalModel = ItemModelUtils.specialModel(
            ModelLocationUtils.getModelLocation(item, "_in_hand"), new TridentSpecialRenderer.Unbaked()
        );
        ItemModel.Unbaked inHandThrowingModel = ItemModelUtils.specialModel(
            ModelLocationUtils.getModelLocation(item, "_throwing"), new TridentSpecialRenderer.Unbaked()
        );
        ItemModel.Unbaked inHandModel = ItemModelUtils.conditional(
            TridentSpecialRenderer.DEFAULT_TRANSFORMATION, ItemModelUtils.isUsingItem(), inHandThrowingModel, inHandNormalModel
        );
        this.itemModelOutput.accept(item, createFlatModelDispatch(flatModel, inHandModel));
    }

    private void generateSpear(final Item item) {
        ItemModel.Unbaked flatModel = ItemModelUtils.plainModel(this.createFlatItemModel(item, ModelTemplates.FLAT_ITEM));
        ItemModel.Unbaked inHandModel = ItemModelUtils.plainModel(
            ModelTemplates.SPEAR_IN_HAND.create(item, TextureMapping.layer0(TextureMapping.getItemTexture(item, "_in_hand")), this.modelOutput)
        );
        this.itemModelOutput.accept(item, createFlatModelDispatch(flatModel, inHandModel), new ClientItem.Properties(true, false, 1.95F));
    }

    private void addPotionTint(final Item item, final Identifier model) {
        this.itemModelOutput.accept(item, ItemModelUtils.tintedModel(model, new Potion()));
    }

    private void generatePotion(final Item item) {
        Identifier model = this.generateLayeredItem(
            item, new Material(Identifier.withDefaultNamespace("item/potion_overlay")), TextureMapping.getItemTexture(item)
        );
        this.addPotionTint(item, model);
    }

    private void generateTippedArrow(final Item item) {
        Identifier model = this.generateLayeredItem(item, TextureMapping.getItemTexture(item, "_head"), TextureMapping.getItemTexture(item, "_base"));
        this.addPotionTint(item, model);
    }

    private void generateDyedItem(final Item item, final int defaultColor) {
        Identifier model = this.createFlatItemModel(item, ModelTemplates.FLAT_ITEM);
        this.itemModelOutput.accept(item, ItemModelUtils.tintedModel(model, new Dye(defaultColor)));
    }

    private void generateTwoLayerDyedItem(final Item item) {
        Material baseLayer = TextureMapping.getItemTexture(item);
        Material tintedLayer = TextureMapping.getItemTexture(item, "_overlay");
        Identifier plainModel = ModelTemplates.FLAT_ITEM.create(item, TextureMapping.layer0(baseLayer), this.modelOutput);
        Identifier dyedModel = ModelLocationUtils.getModelLocation(item, "_dyed");
        ModelTemplates.TWO_LAYERED_ITEM.create(dyedModel, TextureMapping.layered(baseLayer, tintedLayer), this.modelOutput);
        this.itemModelOutput
            .accept(
                item,
                ItemModelUtils.conditional(
                    ItemModelUtils.hasComponent(DataComponents.DYED_COLOR),
                    ItemModelUtils.tintedModel(dyedModel, BLANK_LAYER, new Dye(0)),
                    ItemModelUtils.plainModel(plainModel)
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
        this.generateFlatItem(Items.BLAZE_POWDER, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BLAZE_ROD, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.BONE_MEAL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BORDURE_INDENTED_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BOOK, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BOWL, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BREAD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BRICK, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.BREEZE_ROD, ModelTemplates.FLAT_HANDHELD_ITEM);
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
        Items.DYE.forEach(item -> this.generateFlatItem(item, ModelTemplates.FLAT_ITEM));
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
        this.generateFlatItem(Items.MUSIC_DISC_BOUNCE, ModelTemplates.MUSIC_DISC);
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
        this.generateFlatItem(Items.PAINTING, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PALE_OAK_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PALE_OAK_CHEST_BOAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PAPER, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PHANTOM_MEMBRANE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PIGLIN_BANNER_PATTERN, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.POISONOUS_POTATO, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.POPPED_CHORUS_FRUIT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PORKCHOP, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.POWDER_SNOW_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PRISMARINE_CRYSTALS, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PRISMARINE_SHARD, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PUFFERFISH, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PUFFERFISH_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.PUMPKIN_PIE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.QUARTZ, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RABBIT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RABBIT_FOOT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RABBIT_HIDE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.RABBIT_STEW, ModelTemplates.FLAT_ITEM);
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
        this.generateFlatItem(Items.SULFUR_CUBE_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WATER_BUCKET, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WHEAT, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WIND_CHARGE, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.MACE, ModelTemplates.FLAT_HANDHELD_MACE_ITEM);
        this.generateFlatItem(Items.WOODEN_AXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.WOODEN_HOE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.WOODEN_PICKAXE, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.WOODEN_SHOVEL, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.WOODEN_SWORD, ModelTemplates.FLAT_HANDHELD_ITEM);
        this.generateFlatItem(Items.WRITABLE_BOOK, ModelTemplates.FLAT_ITEM);
        this.generateFlatItem(Items.WRITTEN_BOOK, ModelTemplates.FLAT_ITEM);
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
        Items.DYED_BUNDLE.forEach(this::generateBundleModels);
        this.generateSpyglass(Items.SPYGLASS);
        this.generateTrident(Items.TRIDENT);
        this.generateTwoLayerDyedItem(Items.WOLF_ARMOR);
        Items.HARNESS.forEach(item -> this.generateFlatItem(item, ModelTemplates.FLAT_ITEM));
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
        this.generateFlatItem(Items.SULFUR_CUBE_SPAWN_EGG, ModelTemplates.FLAT_ITEM);
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
        this.declareCustomModelItem(Items.SULFUR_SPIKE);
        this.declareCustomModelItem(Items.BONE);
        this.declareCustomModelItem(Items.COD);
        this.declareCustomModelItem(Items.FEATHER);
        this.declareCustomModelItem(Items.LEAD);
    }

    @OnlyIn(Dist.CLIENT)
    public record TrimMaterialData(MaterialAssetGroup assets, ResourceKey<TrimMaterial> materialKey) {
    }
}