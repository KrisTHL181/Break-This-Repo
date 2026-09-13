package net.minecraft.client.gui.screens;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FlatLevelGeneratorPresetTags;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PresetFlatWorldScreen extends Screen {
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SLOT_BG_SIZE = 18;
    private static final int SLOT_STAT_HEIGHT = 20;
    private static final int SLOT_BG_X = 1;
    private static final int SLOT_BG_Y = 1;
    private static final int SLOT_FG_X = 2;
    private static final int SLOT_FG_Y = 2;
    private static final ResourceKey<Biome> DEFAULT_BIOME = Biomes.PLAINS;
    public static final Component UNKNOWN_PRESET = Component.translatable("flat_world_preset.unknown");
    private final CreateFlatWorldScreen parent;
    private Component shareText;
    private Component listText;
    private PresetFlatWorldScreen.PresetsList list;
    private Button selectButton;
    private EditBox export;
    private FlatLevelGeneratorSettings settings;

    public PresetFlatWorldScreen(final CreateFlatWorldScreen parent) {
        super(Component.translatable("createWorld.customize.presets.title"));
        this.parent = parent;
    }

    private static @Nullable FlatLayerInfo getLayerInfoFromString(final HolderGetter<Block> blocks, final String input, final int firstFree) {
        List<String> parts = Splitter.on('*').limit(2).splitToList(input);
        int height;
        String blockId;
        if (parts.size() == 2) {
            blockId = parts.get(1);

            try {
                height = Math.max(Integer.parseInt(parts.get(0)), 0);
            } catch (NumberFormatException e) {
                LOGGER.error("Error while parsing flat world string", e);
                return null;
            }
        } else {
            blockId = parts.get(0);
            height = 1;
        }

        int firstAbove = Math.min(firstFree + height, DimensionType.Y_SIZE);
        int actualHeight = firstAbove - firstFree;

        Optional<Holder.Reference<Block>> block;
        try {
            block = blocks.get(ResourceKey.create(Registries.BLOCK, Identifier.parse(blockId)));
        } catch (Exception e) {
            LOGGER.error("Error while parsing flat world string", e);
            return null;
        }

        if (block.isEmpty()) {
            LOGGER.error("Error while parsing flat world string => Unknown block, {}", blockId);
            return null;
        } else {
            return new FlatLayerInfo(actualHeight, block.get());
        }
    }

    private static List<FlatLayerInfo> getLayersInfoFromString(final HolderGetter<Block> blocks, final String input) {
        List<FlatLayerInfo> result = Lists.newArrayList();
        String[] depths = input.split(",");
        int firstFree = 0;

        for (String depth : depths) {
            FlatLayerInfo layer = getLayerInfoFromString(blocks, depth, firstFree);
            if (layer == null) {
                return Collections.emptyList();
            }

            int maxHeight = DimensionType.Y_SIZE - firstFree;
            if (maxHeight > 0) {
                result.add(layer.heightLimited(maxHeight));
                firstFree += layer.getHeight();
            }
        }

        return result;
    }

    public static FlatLevelGeneratorSettings fromString(
        final HolderGetter<Block> blocks,
        final HolderGetter<Biome> biomes,
        final HolderGetter<StructureSet> structureSets,
        final HolderGetter<PlacedFeature> placedFeatures,
        final String definition,
        final FlatLevelGeneratorSettings settings
    ) {
        Iterator<String> parts = Splitter.on(';').split(definition).iterator();
        if (!parts.hasNext()) {
            return FlatLevelGeneratorSettings.getDefault(biomes, structureSets, placedFeatures);
        }

        List<FlatLayerInfo> layers = getLayersInfoFromString(blocks, parts.next());
        if (layers.isEmpty()) {
            return FlatLevelGeneratorSettings.getDefault(biomes, structureSets, placedFeatures);
        }

        Holder.Reference<Biome> defaultBiome = biomes.getOrThrow(DEFAULT_BIOME);
        Holder<Biome> biome = defaultBiome;
        if (parts.hasNext()) {
            String biomeName = parts.next();
            biome = Optional.ofNullable(Identifier.tryParse(biomeName))
                .map(id -> ResourceKey.create(Registries.BIOME, id))
                .flatMap(biomes::get)
                .orElseGet(() -> {
                    LOGGER.warn("Invalid biome: {}", biomeName);
                    return defaultBiome;
                });
        }

        return settings.withBiomeAndLayers(layers, settings.structureOverrides(), biome);
    }

    private static String save(final FlatLevelGeneratorSettings settings) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < settings.getLayersInfo().size(); i++) {
            if (i > 0) {
                builder.append(",");
            }

            builder.append(settings.getLayersInfo().get(i));
        }

        builder.append(";");
        builder.append(settings.getBiome().unwrapKey().map(ResourceKey::identifier).orElseThrow(() -> new IllegalStateException("Biome not registered")));
        return builder.toString();
    }

    @Override
    protected void init() {
        this.shareText = Component.translatable("createWorld.customize.presets.share");
        this.listText = Component.translatable("createWorld.customize.presets.list");
        this.export = new EditBox(this.font, 50, 40, this.width - 100, 20, this.shareText);
        this.export.setMaxLength(1230);
        WorldCreationContext worldCreatingContext = this.parent.parent.getUiState().getSettings();
        RegistryAccess registryAccess = worldCreatingContext.worldgenLoadContext();
        FeatureFlagSet enabledFeatures = worldCreatingContext.dataConfiguration().enabledFeatures();
        HolderGetter<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);
        HolderGetter<StructureSet> structureSets = registryAccess.lookupOrThrow(Registries.STRUCTURE_SET);
        HolderGetter<PlacedFeature> placedFeatures = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
        HolderGetter<Block> blocks = registryAccess.lookupOrThrow(Registries.BLOCK).filterFeatures(enabledFeatures);
        this.export.setValue(save(this.parent.settings()));
        this.settings = this.parent.settings();
        this.addWidget(this.export);
        this.list = this.addRenderableWidget(new PresetFlatWorldScreen.PresetsList(registryAccess, enabledFeatures));
        this.selectButton = this.addRenderableWidget(Button.builder(Component.translatable("createWorld.customize.presets.select"), button -> {
            FlatLevelGeneratorSettings generator = fromString(blocks, biomes, structureSets, placedFeatures, this.export.getValue(), this.settings);
            this.parent.setConfig(generator);
            this.minecraft.gui.setScreen(this.parent);
        }).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build());
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.gui.setScreen(this.parent))
                .bounds(this.width / 2 + 5, this.height - 28, 150, 20)
                .build()
        );
        this.updateButtonValidity(this.list.getSelected() != null);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        return this.list.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public void resize(final int width, final int height) {
        String oldEdit = this.export.getValue();
        this.init(width, height);
        this.export.setValue(oldEdit);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, this.title, this.width / 2, 8, -1);
        graphics.text(this.font, this.shareText, 51, 30, -6250336);
        graphics.text(this.font, this.listText, 51, 68, -6250336);
        this.export.extractRenderState(graphics, mouseX, mouseY, a);
    }

    public void updateButtonValidity(final boolean hasSelected) {
        this.selectButton.active = hasSelected || this.export.getValue().length() > 1;
    }

    @OnlyIn(Dist.CLIENT)
    private class PresetsList extends ObjectSelectionList<PresetFlatWorldScreen.PresetsList.Entry> {
        public PresetsList(final RegistryAccess access, final FeatureFlagSet enabledFeatures) {
            super(PresetFlatWorldScreen.this.minecraft, PresetFlatWorldScreen.this.width, PresetFlatWorldScreen.this.height - 117, 80, 24);

            for (Holder<FlatLevelGeneratorPreset> preset : access.lookupOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
                .getTagOrEmpty(FlatLevelGeneratorPresetTags.VISIBLE)) {
                Set<Block> disabledBlocks = preset.value()
                    .settings()
                    .getLayersInfo()
                    .stream()
                    .map(p -> p.getBlockState().getBlock())
                    .filter(b -> !b.isEnabled(enabledFeatures))
                    .collect(Collectors.toSet());
                if (!disabledBlocks.isEmpty()) {
                    PresetFlatWorldScreen.LOGGER
                        .info(
                            "Discarding flat world preset {} since it contains experimental blocks {}",
                            preset.unwrapKey().map(e -> e.identifier().toString()).orElse("<unknown>"),
                            disabledBlocks
                        );
                } else {
                    this.addEntry(new PresetFlatWorldScreen.PresetsList.Entry(preset));
                }
            }
        }

        public void setSelected(final PresetFlatWorldScreen.PresetsList.@Nullable Entry selected) {
            super.setSelected(selected);
            PresetFlatWorldScreen.this.updateButtonValidity(selected != null);
        }

        @Override
        public boolean keyPressed(final KeyEvent event) {
            if (super.keyPressed(event)) {
                return true;
            }

            if (event.isSelection() && this.getSelected() != null) {
                this.getSelected().select();
            }

            return false;
        }

        @OnlyIn(Dist.CLIENT)
        public class Entry extends ObjectSelectionList.Entry<PresetFlatWorldScreen.PresetsList.Entry> {
            private final FlatLevelGeneratorPreset preset;
            private final Component name;

            public Entry(final Holder<FlatLevelGeneratorPreset> preset) {
                this.preset = preset.value();
                this.name = preset.unwrapKey()
                    .map(key -> (Component)Component.translatable(key.identifier().toLanguageKey("flat_world_preset")))
                    .orElse(PresetFlatWorldScreen.UNKNOWN_PRESET);
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
                this.blitSlot(graphics, this.getContentX(), this.getContentY(), this.preset.displayItem().value());
                graphics.text(PresetFlatWorldScreen.this.font, this.name, this.getContentX() + 18 + 5, this.getContentY() + 6, -1);
            }

            @Override
            public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
                this.select();
                return super.mouseClicked(event, doubleClick);
            }

            private void select() {
                PresetsList.this.setSelected(this);
                PresetFlatWorldScreen.this.settings = this.preset.settings();
                PresetFlatWorldScreen.this.export.setValue(PresetFlatWorldScreen.save(PresetFlatWorldScreen.this.settings));
                PresetFlatWorldScreen.this.export.moveCursorToStart(false);
            }

            private void blitSlot(final GuiGraphicsExtractor graphics, final int x, final int y, final Item item) {
                this.blitSlotBg(graphics, x + 1, y + 1);
                graphics.fakeItem(new ItemStack(item), x + 2, y + 2);
            }

            private void blitSlotBg(final GuiGraphicsExtractor graphics, final int x, final int y) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PresetFlatWorldScreen.SLOT_SPRITE, x, y, 18, 18);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.name);
            }
        }
    }
}
