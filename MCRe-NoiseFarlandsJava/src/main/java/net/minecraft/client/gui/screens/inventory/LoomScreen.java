package net.minecraft.client.gui.screens.inventory;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class LoomScreen extends AbstractContainerScreen<LoomMenu> {
    private static final Identifier BANNER_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/banner");
    private static final Identifier DYE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/dye");
    private static final Identifier PATTERN_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/banner_pattern");
    private static final Identifier SCROLLER_SPRITE = Identifier.withDefaultNamespace("container/loom/scroller");
    private static final Identifier SCROLLER_DISABLED_SPRITE = Identifier.withDefaultNamespace("container/loom/scroller_disabled");
    private static final Identifier PATTERN_SELECTED_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern_selected");
    private static final Identifier PATTERN_HIGHLIGHTED_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern_highlighted");
    private static final Identifier PATTERN_SPRITE = Identifier.withDefaultNamespace("container/loom/pattern");
    private static final Identifier ERROR_SPRITE = Identifier.withDefaultNamespace("container/loom/error");
    private static final Identifier BG_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/loom.png");
    private static final int PATTERN_COLUMNS = 4;
    private static final int PATTERN_ROWS = 4;
    private static final int SCROLLER_WIDTH = 12;
    private static final int SCROLLER_HEIGHT = 15;
    private static final int PATTERN_IMAGE_SIZE = 14;
    private static final int SCROLLER_FULL_HEIGHT = 56;
    private static final int PATTERNS_X = 60;
    private static final int PATTERNS_Y = 13;
    private static final float BANNER_PATTERN_TEXTURE_SIZE = 64.0F;
    private static final float BANNER_PATTERN_WIDTH = 21.0F;
    private static final float BANNER_PATTERN_HEIGHT = 40.0F;
    private BannerFlagModel flag;
    private @Nullable BannerPatternLayers resultBannerPatterns;
    private ItemStack bannerStack = ItemStack.EMPTY;
    private ItemStack dyeStack = ItemStack.EMPTY;
    private ItemStack patternStack = ItemStack.EMPTY;
    private boolean displayPatterns;
    private boolean hasMaxPatterns;
    private float scrollOffs;
    private boolean scrolling;
    private int startRow;

    public LoomScreen(final LoomMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        menu.registerUpdateListener(this::containerChanged);
        this.titleLabelY -= 2;
    }

    @Override
    protected void init() {
        super.init();
        ModelPart modelPart = this.minecraft.getEntityModels().bakeLayer(ModelLayers.STANDING_BANNER_FLAG);
        this.flag = new BannerFlagModel(modelPart);
    }

    private int totalRowCount() {
        return Mth.positiveCeilDiv(this.menu.getSelectablePatterns().size(), 4);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG_LOCATION, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        Slot bannerSlot = this.menu.getBannerSlot();
        Slot dyeSlot = this.menu.getDyeSlot();
        Slot patternSlot = this.menu.getPatternSlot();
        Slot resultSlot = this.menu.getResultSlot();
        if (!bannerSlot.hasItem()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BANNER_SLOT_SPRITE, xo + bannerSlot.x, yo + bannerSlot.y, 16, 16);
        }

        if (!dyeSlot.hasItem()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, DYE_SLOT_SPRITE, xo + dyeSlot.x, yo + dyeSlot.y, 16, 16);
        }

        if (!patternSlot.hasItem()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PATTERN_SLOT_SPRITE, xo + patternSlot.x, yo + patternSlot.y, 16, 16);
        }

        int sy = (int)(41.0F * this.scrollOffs);
        Identifier sprite = this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
        int scrollerX = xo + 119;
        int scrollerY = yo + 13;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, scrollerX, scrollerY + sy, 12, 15);
        if (mouseX >= scrollerX && mouseX < scrollerX + 12 && mouseY >= scrollerY && mouseY < scrollerY + 56) {
            if (this.isScrollBarActive()) {
                graphics.requestCursor(this.scrolling ? CursorTypes.RESIZE_NS : CursorTypes.POINTING_HAND);
            } else {
                graphics.requestCursor(CursorTypes.NOT_ALLOWED);
            }
        }

        if (this.resultBannerPatterns != null && !this.hasMaxPatterns) {
            DyeColor baseColor = ((BannerItem)resultSlot.getItem().getItem()).getColor();
            int x0 = xo + 141;
            int y0 = yo + 8;
            graphics.bannerPattern(this.flag, baseColor, this.resultBannerPatterns, x0, y0, x0 + 20, y0 + 40);
        } else if (this.hasMaxPatterns) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, xo + resultSlot.x - 5, yo + resultSlot.y - 5, 26, 26);
        }

        if (this.displayPatterns) {
            int x = xo + 60;
            int y = yo + 13;
            List<Holder<BannerPattern>> selectablePatterns = this.menu.getSelectablePatterns();

            label82:
            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 4; column++) {
                    int actualRow = row + this.startRow;
                    int index = actualRow * 4 + column;
                    if (index >= selectablePatterns.size()) {
                        break label82;
                    }

                    int posX = x + column * 14;
                    int posY = y + row * 14;
                    Holder<BannerPattern> pattern = selectablePatterns.get(index);
                    boolean isHighlighted = mouseX >= posX && mouseY >= posY && mouseX < posX + 14 && mouseY < posY + 14;
                    Identifier buttonSprite;
                    if (index == this.menu.getSelectedBannerPatternIndex()) {
                        buttonSprite = PATTERN_SELECTED_SPRITE;
                    } else if (isHighlighted) {
                        buttonSprite = PATTERN_HIGHLIGHTED_SPRITE;
                        DyeColor patternColor = this.dyeStack.getOrDefault(DataComponents.DYE, DyeColor.WHITE);
                        graphics.setTooltipForNextFrame(Component.translatable(pattern.value().translationKey() + "." + patternColor.getName()), mouseX, mouseY);
                        graphics.requestCursor(CursorTypes.POINTING_HAND);
                    } else {
                        buttonSprite = PATTERN_SPRITE;
                    }

                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, buttonSprite, posX, posY, 14, 14);
                    TextureAtlasSprite bannerPatternSprite = graphics.getSprite(Sheets.getBannerSprite(pattern));
                    this.extractBannerOnButton(graphics, posX, posY, bannerPatternSprite);
                }
            }
        }

        Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
    }

    private boolean isScrollBarActive() {
        return this.displayPatterns && this.menu.getSelectablePatterns().size() > 16;
    }

    private void extractBannerOnButton(final GuiGraphicsExtractor graphics, final int posX, final int posY, final TextureAtlasSprite bannerPatternSprite) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(posX + 4, posY + 2);
        float patternU0 = bannerPatternSprite.getU0();
        float patternU1 = patternU0 + (bannerPatternSprite.getU1() - bannerPatternSprite.getU0()) * 21.0F / 64.0F;
        float patternVSpan = bannerPatternSprite.getV1() - bannerPatternSprite.getV0();
        float patternV0 = bannerPatternSprite.getV0() + patternVSpan / 64.0F;
        float patternV1 = patternV0 + patternVSpan * 40.0F / 64.0F;
        int bannerWidth = 5;
        int bannerHeight = 10;
        graphics.fill(0, 0, 5, 10, DyeColor.GRAY.getTextureDiffuseColor());
        graphics.blit(bannerPatternSprite.atlasLocation(), 0, 0, 5, 10, patternU0, patternU1, patternV0, patternV1);
        graphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        if (this.displayPatterns) {
            int xo = this.leftPos + 60;
            int yo = this.topPos + 13;

            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 4; column++) {
                    double xx = event.x() - (xo + column * 14);
                    double yy = event.y() - (yo + row * 14);
                    int actualRow = row + this.startRow;
                    int index = actualRow * 4 + column;
                    if (xx >= 0.0 && yy >= 0.0 && xx < 14.0 && yy < 14.0 && this.menu.clickMenuButton(this.minecraft.player, index)) {
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_LOOM_SELECT_PATTERN, 1.0F));
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
                        return true;
                    }
                }
            }

            xo = this.leftPos + 119;
            yo = this.topPos + 9;
            if (event.x() >= xo && event.x() < xo + 12 && event.y() >= yo && event.y() < yo + 56) {
                this.scrolling = true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
        int offscreenRows = this.totalRowCount() - 4;
        if (this.scrolling && this.displayPatterns && offscreenRows > 0) {
            int yscr = this.topPos + 13;
            int yscr2 = yscr + 56;
            this.scrollOffs = ((float)event.y() - yscr - 7.5F) / (yscr2 - yscr - 15.0F);
            this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
            this.startRow = Math.max((int)(this.scrollOffs * offscreenRows + 0.5), 0);
            return true;
        } else {
            return super.mouseDragged(event, dx, dy);
        }
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        this.scrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        if (super.mouseScrolled(x, y, scrollX, scrollY)) {
            return true;
        }

        int offscreenRows = this.totalRowCount() - 4;
        if (this.displayPatterns && offscreenRows > 0) {
            float scrolledDelta = (float)scrollY / offscreenRows;
            this.scrollOffs = Mth.clamp(this.scrollOffs - scrolledDelta, 0.0F, 1.0F);
            this.startRow = Math.max((int)(this.scrollOffs * offscreenRows + 0.5F), 0);
        }

        return true;
    }

    @Override
    protected boolean hasClickedOutside(final double mx, final double my, final int xo, final int yo) {
        return mx < xo || my < yo || mx >= xo + this.imageWidth || my >= yo + this.imageHeight;
    }

    private void containerChanged() {
        ItemStack resultStack = this.menu.getResultSlot().getItem();
        if (resultStack.isEmpty()) {
            this.resultBannerPatterns = null;
        } else {
            this.resultBannerPatterns = resultStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        }

        ItemStack bannerStack = this.menu.getBannerSlot().getItem();
        ItemStack dyeStack = this.menu.getDyeSlot().getItem();
        ItemStack patternStack = this.menu.getPatternSlot().getItem();
        BannerPatternLayers patterns = bannerStack.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        this.hasMaxPatterns = patterns.layers().size() >= 6;
        if (this.hasMaxPatterns) {
            this.resultBannerPatterns = null;
        }

        if (!ItemStack.matches(bannerStack, this.bannerStack)
            || !ItemStack.matches(dyeStack, this.dyeStack)
            || !ItemStack.matches(patternStack, this.patternStack)) {
            this.displayPatterns = !bannerStack.isEmpty() && !dyeStack.isEmpty() && !this.hasMaxPatterns && !this.menu.getSelectablePatterns().isEmpty();
        }

        if (this.startRow >= this.totalRowCount()) {
            this.startRow = 0;
            this.scrollOffs = 0.0F;
        }

        this.bannerStack = bannerStack.copy();
        this.dyeStack = dyeStack.copy();
        this.patternStack = patternStack.copy();
    }
}