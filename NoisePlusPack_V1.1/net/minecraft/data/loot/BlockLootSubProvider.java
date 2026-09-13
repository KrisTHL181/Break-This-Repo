package net.minecraft.data.loot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.EnchantmentPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.MossyCarpetBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SegmentableBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.CopyBlockState;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LimitCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public abstract class BlockLootSubProvider implements LootTableSubProvider {
   protected final HolderLookup.Provider registries;
   protected final Set<Item> explosionResistant;
   protected final FeatureFlagSet enabledFeatures;
   protected final Map<ResourceKey<LootTable>, LootTable.Builder> map;
   protected static final float[] NORMAL_LEAVES_SAPLING_CHANCES = new float[]{0.05F, 0.0625F, 0.083333336F, 0.1F};
   private static final float[] NORMAL_LEAVES_STICK_CHANCES = new float[]{0.02F, 0.022222223F, 0.025F, 0.033333335F, 0.1F};

   protected LootItemCondition.Builder hasSilkTouch() {
      return MatchTool.toolMatches(
         ItemPredicate.Builder.item()
            .withComponents(
               DataComponentMatchers.Builder.components()
                  .partial(
                     DataComponentPredicates.ENCHANTMENTS,
                     EnchantmentsPredicate.enchantments(
                        List.of(
                           new EnchantmentPredicate(
                              this.registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), MinMaxBounds.Ints.atLeast(1)
                           )
                        )
                     )
                  )
                  .build()
            )
      );
   }

   protected LootItemCondition.Builder doesNotHaveSilkTouch() {
      return this.hasSilkTouch().invert();
   }

   protected LootItemCondition.Builder hasShears() {
      return MatchTool.toolMatches(ItemPredicate.Builder.item().of(this.registries.lookupOrThrow(Registries.ITEM), Items.SHEARS));
   }

   private LootItemCondition.Builder hasShearsOrSilkTouch() {
      return this.hasShears().or(this.hasSilkTouch());
   }

   private LootItemCondition.Builder doesNotHaveShearsOrSilkTouch() {
      return this.hasShearsOrSilkTouch().invert();
   }

   protected BlockLootSubProvider(Set<Item> p_281507_, FeatureFlagSet p_283552_, HolderLookup.Provider p_345174_) {
      this(p_281507_, p_283552_, new HashMap<>(), p_345174_);
   }

   protected BlockLootSubProvider(
      Set<Item> p_249153_, FeatureFlagSet p_251215_, Map<ResourceKey<LootTable>, LootTable.Builder> p_343991_, HolderLookup.Provider p_343444_
   ) {
      this.explosionResistant = p_249153_;
      this.enabledFeatures = p_251215_;
      this.map = p_343991_;
      this.registries = p_343444_;
   }

   protected <T extends FunctionUserBuilder<T>> T applyExplosionDecay(ItemLike p_248695_, FunctionUserBuilder<T> p_248548_) {
      return !this.explosionResistant.contains(p_248695_.asItem()) ? p_248548_.apply(ApplyExplosionDecay.explosionDecay()) : p_248548_.unwrap();
   }

   protected <T extends ConditionUserBuilder<T>> T applyExplosionCondition(ItemLike p_249717_, ConditionUserBuilder<T> p_248851_) {
      return !this.explosionResistant.contains(p_249717_.asItem()) ? p_248851_.when(ExplosionCondition.survivesExplosion()) : p_248851_.unwrap();
   }

   public LootTable.Builder createSingleItemTable(ItemLike p_251912_) {
      return LootTable.lootTable()
         .withPool(this.applyExplosionCondition(p_251912_, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(p_251912_))));
   }

   private static LootTable.Builder createSelfDropDispatchTable(
      Block p_252253_, LootItemCondition.Builder p_248764_, LootPoolEntryContainer.Builder<?> p_249146_
   ) {
      return LootTable.lootTable()
         .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(p_252253_).when(p_248764_).otherwise(p_249146_)));
   }

   protected LootTable.Builder createSilkTouchDispatchTable(Block p_250203_, LootPoolEntryContainer.Builder<?> p_252089_) {
      return createSelfDropDispatchTable(p_250203_, this.hasSilkTouch(), p_252089_);
   }

   protected LootTable.Builder createShearsDispatchTable(Block p_252195_, LootPoolEntryContainer.Builder<?> p_250102_) {
      return createSelfDropDispatchTable(p_252195_, this.hasShears(), p_250102_);
   }

   protected LootTable.Builder createSilkTouchOrShearsDispatchTable(Block p_250539_, LootPoolEntryContainer.Builder<?> p_251459_) {
      return createSelfDropDispatchTable(p_250539_, this.hasShearsOrSilkTouch(), p_251459_);
   }

   protected LootTable.Builder createSingleItemTableWithSilkTouch(Block p_249305_, ItemLike p_251905_) {
      return this.createSilkTouchDispatchTable(
         p_249305_, (LootPoolEntryContainer.Builder<?>)this.applyExplosionCondition(p_249305_, LootItem.lootTableItem(p_251905_))
      );
   }

   protected LootTable.Builder createSingleItemTable(ItemLike p_251584_, NumberProvider p_249865_) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(
                  (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                     p_251584_, LootItem.lootTableItem(p_251584_).apply(SetItemCountFunction.setCount(p_249865_))
                  )
               )
         );
   }

   protected LootTable.Builder createSingleItemTableWithSilkTouch(Block p_251449_, ItemLike p_248558_, NumberProvider p_250047_) {
      return this.createSilkTouchDispatchTable(
         p_251449_,
         (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
            p_251449_, LootItem.lootTableItem(p_248558_).apply(SetItemCountFunction.setCount(p_250047_))
         )
      );
   }

   private LootTable.Builder createSilkTouchOnlyTable(ItemLike p_252216_) {
      return LootTable.lootTable()
         .withPool(LootPool.lootPool().when(this.hasSilkTouch()).setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(p_252216_)));
   }

   private LootTable.Builder createPotFlowerItemTable(ItemLike p_249395_) {
      return LootTable.lootTable()
         .withPool(
            this.applyExplosionCondition(
               Blocks.FLOWER_POT, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(Blocks.FLOWER_POT))
            )
         )
         .withPool(this.applyExplosionCondition(p_249395_, LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(p_249395_))));
   }

   protected LootTable.Builder createSlabItemTable(Block p_251313_) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(
                  (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                     p_251313_,
                     LootItem.lootTableItem(p_251313_)
                        .apply(
                           SetItemCountFunction.setCount(ConstantValue.exactly(2.0F))
                              .when(
                                 LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_251313_)
                                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(SlabBlock.TYPE, SlabType.DOUBLE))
                              )
                        )
                  )
               )
         );
   }

   protected <T extends Comparable<T> & StringRepresentable> LootTable.Builder createSinglePropConditionTable(
      Block p_252154_, Property<T> p_250272_, T p_250292_
   ) {
      return LootTable.lootTable()
         .withPool(
            this.applyExplosionCondition(
               p_252154_,
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(
                     LootItem.lootTableItem(p_252154_)
                        .when(
                           LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_252154_)
                              .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(p_250272_, p_250292_))
                        )
                  )
            )
         );
   }

   protected LootTable.Builder createNameableBlockEntityTable(Block p_252291_) {
      return LootTable.lootTable()
         .withPool(
            this.applyExplosionCondition(
               p_252291_,
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(
                     LootItem.lootTableItem(p_252291_)
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DataComponents.CUSTOM_NAME))
                  )
            )
         );
   }

   protected LootTable.Builder createShulkerBoxDrop(Block p_252164_) {
      return LootTable.lootTable()
         .withPool(
            this.applyExplosionCondition(
               p_252164_,
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(
                     LootItem.lootTableItem(p_252164_)
                        .apply(
                           CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                              .include(DataComponents.CUSTOM_NAME)
                              .include(DataComponents.CONTAINER)
                              .include(DataComponents.LOCK)
                              .include(DataComponents.CONTAINER_LOOT)
                        )
                  )
            )
         );
   }

   protected LootTable.Builder createCopperOreDrops(Block p_251306_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      return this.createSilkTouchDispatchTable(
         p_251306_,
         (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
            p_251306_,
            LootItem.lootTableItem(Items.RAW_COPPER)
               .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))
               .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
         )
      );
   }

   protected LootTable.Builder createLapisOreDrops(Block p_251511_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      return this.createSilkTouchDispatchTable(
         p_251511_,
         (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
            p_251511_,
            LootItem.lootTableItem(Items.LAPIS_LAZULI)
               .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 9.0F)))
               .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
         )
      );
   }

   protected LootTable.Builder createRedstoneOreDrops(Block p_251906_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      return this.createSilkTouchDispatchTable(
         p_251906_,
         (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
            p_251906_,
            LootItem.lootTableItem(Items.REDSTONE)
               .apply(SetItemCountFunction.setCount(UniformGenerator.between(4.0F, 5.0F)))
               .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
         )
      );
   }

   protected LootTable.Builder createBannerDrop(Block p_249810_) {
      return LootTable.lootTable()
         .withPool(
            this.applyExplosionCondition(
               p_249810_,
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(
                     LootItem.lootTableItem(p_249810_)
                        .apply(
                           CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)
                              .include(DataComponents.CUSTOM_NAME)
                              .include(DataComponents.ITEM_NAME)
                              .include(DataComponents.TOOLTIP_DISPLAY)
                              .include(DataComponents.BANNER_PATTERNS)
                              .include(DataComponents.RARITY)
                        )
                  )
            )
         );
   }

   protected LootTable.Builder createBeeNestDrop(Block p_250988_) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .when(this.hasSilkTouch())
               .setRolls(ConstantValue.exactly(1.0F))
               .add(
                  LootItem.lootTableItem(p_250988_)
                     .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DataComponents.BEES))
                     .apply(CopyBlockState.copyState(p_250988_).copy(BeehiveBlock.HONEY_LEVEL))
               )
         );
   }

   protected LootTable.Builder createBeeHiveDrop(Block p_248770_) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(
                  LootItem.lootTableItem(p_248770_)
                     .when(this.hasSilkTouch())
                     .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DataComponents.BEES))
                     .apply(CopyBlockState.copyState(p_248770_).copy(BeehiveBlock.HONEY_LEVEL))
                     .otherwise(LootItem.lootTableItem(p_248770_))
               )
         );
   }

   protected LootTable.Builder createCaveVinesDrop(Block p_251070_) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .add(LootItem.lootTableItem(Items.GLOW_BERRIES))
               .when(
                  LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_251070_)
                     .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CaveVines.BERRIES, true))
               )
         );
   }

   protected LootTable.Builder createCopperGolemStatueBlock(Block p_431350_) {
      return LootTable.lootTable()
         .withPool(
            this.applyExplosionCondition(
               p_431350_,
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(
                     LootItem.lootTableItem(p_431350_)
                        .apply(CopyComponentsFunction.copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY).include(DataComponents.CUSTOM_NAME))
                        .apply(CopyBlockState.copyState(p_431350_).copy(CopperGolemStatueBlock.POSE))
                  )
            )
         );
   }

   protected LootTable.Builder createOreDrop(Block p_250450_, Item p_249745_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      return this.createSilkTouchDispatchTable(
         p_250450_,
         (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
            p_250450_, LootItem.lootTableItem(p_249745_).apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))
         )
      );
   }

   protected LootTable.Builder createMushroomBlockDrop(Block p_249959_, ItemLike p_249315_) {
      return this.createSilkTouchDispatchTable(
         p_249959_,
         (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
            p_249959_,
            LootItem.lootTableItem(p_249315_)
               .apply(SetItemCountFunction.setCount(UniformGenerator.between(-6.0F, 2.0F)))
               .apply(LimitCount.limitCount(IntRange.lowerBound(0)))
         )
      );
   }

   protected LootTable.Builder createGrassDrops(Block p_252139_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      return this.createShearsDispatchTable(
         p_252139_,
         (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
            p_252139_,
            LootItem.lootTableItem(Items.WHEAT_SEEDS)
               .when(LootItemRandomChanceCondition.randomChance(0.125F))
               .apply(ApplyBonusCount.addUniformBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE), 2))
         )
      );
   }

   public LootTable.Builder createStemDrops(Block p_250957_, Item p_249098_) {
      return LootTable.lootTable()
         .withPool(
            this.applyExplosionDecay(
               p_250957_,
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(
                     LootItem.lootTableItem(p_249098_)
                        .apply(
                           StemBlock.AGE.getPossibleValues(),
                           p_448681_ -> SetItemCountFunction.setCount(BinomialDistributionGenerator.binomial(3, (p_448681_ + 1) / 15.0F))
                              .when(
                                 LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_250957_)
                                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(StemBlock.AGE, p_448681_.intValue()))
                              )
                        )
                  )
            )
         );
   }

   public LootTable.Builder createAttachedStemDrops(Block p_249778_, Item p_250678_) {
      return LootTable.lootTable()
         .withPool(
            this.applyExplosionDecay(
               p_249778_,
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(LootItem.lootTableItem(p_250678_).apply(SetItemCountFunction.setCount(BinomialDistributionGenerator.binomial(3, 0.53333336F))))
            )
         );
   }

   protected LootTable.Builder createShearsOnlyDrop(ItemLike p_250684_) {
      return LootTable.lootTable()
         .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(this.hasShears()).add(LootItem.lootTableItem(p_250684_)));
   }

   protected LootTable.Builder createShearsOrSilkTouchOnlyDrop(ItemLike p_364223_) {
      return LootTable.lootTable()
         .withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).when(this.hasShearsOrSilkTouch()).add(LootItem.lootTableItem(p_364223_)));
   }

   protected LootTable.Builder createMultifaceBlockDrops(Block p_249088_, LootItemCondition.Builder p_251535_) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .add(
                  (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                     p_249088_,
                     LootItem.lootTableItem(p_249088_)
                        .when(p_251535_)
                        .apply(
                           Direction.values(),
                           p_448686_ -> SetItemCountFunction.setCount(ConstantValue.exactly(1.0F), true)
                              .when(
                                 LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_249088_)
                                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(MultifaceBlock.getFaceProperty(p_448686_), true))
                              )
                        )
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(-1.0F), true))
                  )
               )
         );
   }

   protected LootTable.Builder createMultifaceBlockDrops(Block p_377116_) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .add(
                  (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                     p_377116_,
                     LootItem.lootTableItem(p_377116_)
                        .apply(
                           Direction.values(),
                           p_448679_ -> SetItemCountFunction.setCount(ConstantValue.exactly(1.0F), true)
                              .when(
                                 LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_377116_)
                                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(MultifaceBlock.getFaceProperty(p_448679_), true))
                              )
                        )
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(-1.0F), true))
                  )
               )
         );
   }

   protected LootTable.Builder createMossyCarpetBlockDrops(Block p_363021_) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .add(
                  (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                     p_363021_,
                     LootItem.lootTableItem(p_363021_)
                        .when(
                           LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_363021_)
                              .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(MossyCarpetBlock.BASE, true))
                        )
                  )
               )
         );
   }

   protected LootTable.Builder createLeavesDrops(Block p_250088_, Block p_250731_, float... p_248949_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      return this.createSilkTouchOrShearsDispatchTable(
            p_250088_,
            ((LootPoolSingletonContainer.Builder)this.applyExplosionCondition(p_250088_, LootItem.lootTableItem(p_250731_)))
               .when(BonusLevelTableCondition.bonusLevelFlatChance(registrylookup.getOrThrow(Enchantments.FORTUNE), p_248949_))
         )
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .when(this.doesNotHaveShearsOrSilkTouch())
               .add(
                  ((LootPoolSingletonContainer.Builder)this.applyExplosionDecay(
                        p_250088_, LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                     ))
                     .when(BonusLevelTableCondition.bonusLevelFlatChance(registrylookup.getOrThrow(Enchantments.FORTUNE), NORMAL_LEAVES_STICK_CHANCES))
               )
         );
   }

   protected LootTable.Builder createOakLeavesDrops(Block p_249535_, Block p_251505_, float... p_250753_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      return this.createLeavesDrops(p_249535_, p_251505_, p_250753_)
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .when(this.doesNotHaveShearsOrSilkTouch())
               .add(
                  ((LootPoolSingletonContainer.Builder)this.applyExplosionCondition(p_249535_, LootItem.lootTableItem(Items.APPLE)))
                     .when(
                        BonusLevelTableCondition.bonusLevelFlatChance(
                           registrylookup.getOrThrow(Enchantments.FORTUNE), 0.005F, 0.0055555557F, 0.00625F, 0.008333334F, 0.025F
                        )
                     )
               )
         );
   }

   protected LootTable.Builder createMangroveLeavesDrops(Block p_251103_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      return this.createSilkTouchOrShearsDispatchTable(
         p_251103_,
         ((LootPoolSingletonContainer.Builder)this.applyExplosionDecay(
               Blocks.MANGROVE_LEAVES, LootItem.lootTableItem(Items.STICK).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
            ))
            .when(BonusLevelTableCondition.bonusLevelFlatChance(registrylookup.getOrThrow(Enchantments.FORTUNE), NORMAL_LEAVES_STICK_CHANCES))
      );
   }

   protected LootTable.Builder createCropDrops(Block p_249457_, Item p_248599_, Item p_251915_, LootItemCondition.Builder p_252202_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      return this.applyExplosionDecay(
         p_249457_,
         LootTable.lootTable()
            .withPool(LootPool.lootPool().add(LootItem.lootTableItem(p_248599_).when(p_252202_).otherwise(LootItem.lootTableItem(p_251915_))))
            .withPool(
               LootPool.lootPool()
                  .when(p_252202_)
                  .add(
                     LootItem.lootTableItem(p_251915_)
                        .apply(ApplyBonusCount.addBonusBinomialDistributionCount(registrylookup.getOrThrow(Enchantments.FORTUNE), 0.5714286F, 3))
                  )
            )
      );
   }

   protected LootTable.Builder createDoublePlantShearsDrop(Block p_248678_) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool().when(this.hasShears()).add(LootItem.lootTableItem(p_248678_).apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F))))
         );
   }

   protected LootTable.Builder createDoublePlantWithSeedDrops(Block p_248590_, Block p_248735_) {
      HolderLookup.RegistryLookup<Block> registrylookup = this.registries.lookupOrThrow(Registries.BLOCK);
      LootPoolEntryContainer.Builder<?> builder = LootItem.lootTableItem(p_248735_)
         .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
         .when(this.hasShears())
         .otherwise(
            ((LootPoolSingletonContainer.Builder)this.applyExplosionCondition(p_248590_, LootItem.lootTableItem(Items.WHEAT_SEEDS)))
               .when(LootItemRandomChanceCondition.randomChance(0.125F))
         );
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .add(builder)
               .when(
                  LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_248590_)
                     .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER))
               )
               .when(
                  LocationCheck.checkLocation(
                     LocationPredicate.Builder.location()
                        .setBlock(
                           BlockPredicate.Builder.block()
                              .of(registrylookup, p_248590_)
                              .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER))
                        ),
                     new BlockPos(0, 1, 0)
                  )
               )
         )
         .withPool(
            LootPool.lootPool()
               .add(builder)
               .when(
                  LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_248590_)
                     .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER))
               )
               .when(
                  LocationCheck.checkLocation(
                     LocationPredicate.Builder.location()
                        .setBlock(
                           BlockPredicate.Builder.block()
                              .of(registrylookup, p_248590_)
                              .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER))
                        ),
                     new BlockPos(0, -1, 0)
                  )
               )
         );
   }

   protected LootTable.Builder createCandleDrops(Block p_250896_) {
      return LootTable.lootTable()
         .withPool(
            LootPool.lootPool()
               .setRolls(ConstantValue.exactly(1.0F))
               .add(
                  (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                     p_250896_,
                     LootItem.lootTableItem(p_250896_)
                        .apply(
                           List.of(2, 3, 4),
                           p_448677_ -> SetItemCountFunction.setCount(ConstantValue.exactly(p_448677_.intValue()))
                              .when(
                                 LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_250896_)
                                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CandleBlock.CANDLES, p_448677_.intValue()))
                              )
                        )
                  )
               )
         );
   }

   public LootTable.Builder createSegmentedBlockDrops(Block p_397024_) {
      return p_397024_ instanceof SegmentableBlock segmentableblock
         ? LootTable.lootTable()
            .withPool(
               LootPool.lootPool()
                  .setRolls(ConstantValue.exactly(1.0F))
                  .add(
                     (LootPoolEntryContainer.Builder<?>)this.applyExplosionDecay(
                        p_397024_,
                        LootItem.lootTableItem(p_397024_)
                           .apply(
                              IntStream.rangeClosed(1, 4).boxed().toList(),
                              p_448684_ -> SetItemCountFunction.setCount(ConstantValue.exactly(p_448684_.intValue()))
                                 .when(
                                    LootItemBlockStatePropertyCondition.hasBlockStateProperties(p_397024_)
                                       .setProperties(
                                          StatePropertiesPredicate.Builder.properties()
                                             .hasProperty(segmentableblock.getSegmentAmountProperty(), p_448684_.intValue())
                                       )
                                 )
                           )
                     )
                  )
            )
         : noDrop();
   }

   protected static LootTable.Builder createCandleCakeDrops(Block p_250280_) {
      return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(LootItem.lootTableItem(p_250280_)));
   }

   public static LootTable.Builder noDrop() {
      return LootTable.lootTable();
   }

   protected abstract void generate();

   @Override
   public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> p_249322_) {
      this.generate();
      Set<ResourceKey<LootTable>> set = new HashSet<>();

      for (Block block : BuiltInRegistries.BLOCK) {
         if (block.isEnabled(this.enabledFeatures)) {
            block.getLootTable()
               .ifPresent(
                  p_358210_ -> {
                     if (set.add((ResourceKey<LootTable>)p_358210_)) {
                        LootTable.Builder loottable$builder = this.map.remove(p_358210_);
                        if (loottable$builder == null) {
                           throw new IllegalStateException(
                              String.format(Locale.ROOT, "Missing loottable '%s' for '%s'", p_358210_.identifier(), BuiltInRegistries.BLOCK.getKey(block))
                           );
                        }

                        p_249322_.accept((ResourceKey<LootTable>)p_358210_, loottable$builder);
                     }
                  }
               );
         }
      }

      if (!this.map.isEmpty()) {
         throw new IllegalStateException("Created block loot tables for non-blocks: " + this.map.keySet());
      }
   }

   protected void addNetherVinesDropTable(Block p_252269_, Block p_250696_) {
      HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
      LootTable.Builder loottable$builder = this.createSilkTouchOrShearsDispatchTable(
         p_252269_,
         LootItem.lootTableItem(p_252269_)
            .when(BonusLevelTableCondition.bonusLevelFlatChance(registrylookup.getOrThrow(Enchantments.FORTUNE), 0.33F, 0.55F, 0.77F, 1.0F))
      );
      this.add(p_252269_, loottable$builder);
      this.add(p_250696_, loottable$builder);
   }

   protected LootTable.Builder createDoorTable(Block p_252166_) {
      return this.createSinglePropConditionTable(p_252166_, DoorBlock.HALF, DoubleBlockHalf.LOWER);
   }

   protected void dropPottedContents(Block p_251064_) {
      this.add(p_251064_, p_308498_ -> this.createPotFlowerItemTable(((FlowerPotBlock)p_308498_).getPotted()));
   }

   protected void otherWhenSilkTouch(Block p_249932_, Block p_252053_) {
      this.add(p_249932_, this.createSilkTouchOnlyTable(p_252053_));
   }

   protected void dropOther(Block p_248885_, ItemLike p_251883_) {
      this.add(p_248885_, this.createSingleItemTable(p_251883_));
   }

   protected void dropWhenSilkTouch(Block p_250855_) {
      this.otherWhenSilkTouch(p_250855_, p_250855_);
   }

   protected void dropSelf(Block p_249181_) {
      this.dropOther(p_249181_, p_249181_);
   }

   protected void add(Block p_251966_, Function<Block, LootTable.Builder> p_251699_) {
      this.add(p_251966_, p_251699_.apply(p_251966_));
   }

   protected void add(Block p_250610_, LootTable.Builder p_249817_) {
      this.map.put(p_250610_.getLootTable().orElseThrow(() -> new IllegalStateException("Block " + p_250610_ + " does not have loot table")), p_249817_);
   }
}
