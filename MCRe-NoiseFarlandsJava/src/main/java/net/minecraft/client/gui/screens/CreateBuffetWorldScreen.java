package net.minecraft.client.gui.screens;

import com.ibm.icu.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CreateBuffetWorldScreen extends Screen {
    private static final Component SEARCH_HINT = Component.translatable("createWorld.customize.buffet.search").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final int SPACING = 3;
    private static final int SEARCH_BOX_HEIGHT = 15;
    private final HeaderAndFooterLayout layout;
    private final Screen parent;
    private final Consumer<Holder<Biome>> applySettings;
    private final Registry<Biome> biomes;
    private CreateBuffetWorldScreen.BiomeList list;
    private Holder<Biome> biome;
    private Button doneButton;

    public CreateBuffetWorldScreen(final Screen parent, final WorldCreationContext settings, final Consumer<Holder<Biome>> applySettings) {
        super(Component.translatable("createWorld.customize.buffet.title"));
        this.parent = parent;
        this.applySettings = applySettings;
        this.layout = new HeaderAndFooterLayout(this, 13 + 9 + 3 + 15, 33);
        this.biomes = settings.worldgenLoadContext().lookupOrThrow(Registries.BIOME);
        Holder<Biome> defaultBiome = this.biomes.get(Biomes.PLAINS).or(() -> this.biomes.listElements().findAny()).orElseThrow();
        this.biome = settings.selectedDimensions().overworld().getBiomeSource().possibleBiomes().stream().findFirst().orElse(defaultBiome);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    protected void init() {
        LinearLayout header = this.layout.addToHeader(LinearLayout.vertical().spacing(3));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(this.getTitle(), this.font));
        EditBox search = header.addChild(new EditBox(this.font, 200, 15, Component.empty()));
        CreateBuffetWorldScreen.BiomeList biomeList = new CreateBuffetWorldScreen.BiomeList();
        search.setHint(SEARCH_HINT);
        search.setResponder(biomeList::filterEntries);
        this.list = this.layout.addToContents(biomeList);
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        this.doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> {
            this.applySettings.accept(this.biome);
            this.onClose();
        }).build());
        footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());
        this.list.setSelected(this.list.children().stream().filter(e -> Objects.equals(e.biome, this.biome)).findFirst().orElse(null));
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        this.list.updateSize(this.width, this.layout);
    }

    private void updateButtonValidity() {
        this.doneButton.active = this.list.getSelected() != null;
    }

    @OnlyIn(Dist.CLIENT)
    private class BiomeList extends ObjectSelectionList<CreateBuffetWorldScreen.BiomeList.Entry> {
        private BiomeList() {
            super(
                CreateBuffetWorldScreen.this.minecraft,
                CreateBuffetWorldScreen.this.width,
                CreateBuffetWorldScreen.this.layout.getContentHeight(),
                CreateBuffetWorldScreen.this.layout.getHeaderHeight(),
                15
            );
            this.filterEntries("");
        }

        private void filterEntries(final String filter) {
            Collator localeCollator = Collator.getInstance(Locale.getDefault());
            String lowercaseFilter = filter.toLowerCase(Locale.ROOT);
            List<CreateBuffetWorldScreen.BiomeList.Entry> list = CreateBuffetWorldScreen.this.biomes
                .listElements()
                .map(x$0 -> new CreateBuffetWorldScreen.BiomeList.Entry((Holder.Reference<Biome>)x$0))
                .sorted(Comparator.comparing(e -> e.name.getString(), localeCollator))
                .filter(entry -> filter.isEmpty() || entry.name.getString().toLowerCase(Locale.ROOT).contains(lowercaseFilter))
                .toList();
            this.replaceEntries(list);
            this.refreshScrollAmount();
        }

        public void setSelected(final CreateBuffetWorldScreen.BiomeList.@Nullable Entry selected) {
            super.setSelected(selected);
            if (selected != null) {
                CreateBuffetWorldScreen.this.biome = selected.biome;
            }

            CreateBuffetWorldScreen.this.updateButtonValidity();
        }

        @OnlyIn(Dist.CLIENT)
        private class Entry extends ObjectSelectionList.Entry<CreateBuffetWorldScreen.BiomeList.Entry> {
            private final Holder.Reference<Biome> biome;
            private final Component name;

            public Entry(final Holder.Reference<Biome> biome) {
                this.biome = biome;
                Identifier id = biome.key().identifier();
                String translationKey = id.toLanguageKey("biome");
                if (Language.getInstance().has(translationKey)) {
                    this.name = Component.translatable(translationKey);
                } else {
                    this.name = Component.literal(id.toString());
                }
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", this.name);
            }

            @Override
            public void extractContent(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
                graphics.text(CreateBuffetWorldScreen.this.font, this.name, this.getContentX() + 5, this.getContentY() + 2, -1);
            }

            @Override
            public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
                BiomeList.this.setSelected(this);
                return super.mouseClicked(event, doubleClick);
            }
        }
    }
}