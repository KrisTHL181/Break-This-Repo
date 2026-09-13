package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LinearLayout implements Layout {
    private final GridLayout wrapped;
    private final LinearLayout.Orientation orientation;
    private int nextChildIndex = 0;

    private LinearLayout(final LinearLayout.Orientation orientation) {
        this(0, 0, orientation);
    }

    public LinearLayout(final int x, final int y, final LinearLayout.Orientation orientation) {
        this.wrapped = new GridLayout(x, y);
        this.orientation = orientation;
    }

    public LinearLayout spacing(final int spacing) {
        this.orientation.setSpacing(this.wrapped, spacing);
        return this;
    }

    public LayoutSettings newCellSettings() {
        return this.wrapped.newCellSettings();
    }

    public LayoutSettings defaultCellSetting() {
        return this.wrapped.defaultCellSetting();
    }

    public <T extends LayoutElement> T addChild(final T child, final LayoutSettings cellSettings) {
        return this.orientation.addChild(this.wrapped, child, this.nextChildIndex++, cellSettings);
    }

    public <T extends LayoutElement> T addChild(final T child) {
        return this.addChild(child, this.newCellSettings());
    }

    public <T extends LayoutElement> T addChild(final T child, final Consumer<LayoutSettings> layoutSettingsAdjustments) {
        return this.orientation.addChild(this.wrapped, child, this.nextChildIndex++, Util.make(this.newCellSettings(), layoutSettingsAdjustments));
    }

    @Override
    public void visitChildren(final Consumer<LayoutElement> layoutElementVisitor) {
        this.wrapped.visitChildren(layoutElementVisitor);
    }

    @Override
    public void removeChildren() {
        this.wrapped.removeChildren();
        this.nextChildIndex = 0;
    }

    @Override
    public void arrangeElements() {
        this.wrapped.arrangeElements();
    }

    @Override
    public int getWidth() {
        return this.wrapped.getWidth();
    }

    @Override
    public int getHeight() {
        return this.wrapped.getHeight();
    }

    @Override
    public void setX(final int x) {
        this.wrapped.setX(x);
    }

    @Override
    public void setY(final int y) {
        this.wrapped.setY(y);
    }

    @Override
    public int getX() {
        return this.wrapped.getX();
    }

    @Override
    public int getY() {
        return this.wrapped.getY();
    }

    public static LinearLayout vertical() {
        return new LinearLayout(LinearLayout.Orientation.VERTICAL);
    }

    public static LinearLayout horizontal() {
        return new LinearLayout(LinearLayout.Orientation.HORIZONTAL);
    }

    @OnlyIn(Dist.CLIENT)
    public enum Orientation {
        HORIZONTAL,
        VERTICAL;

        private void setSpacing(final GridLayout gridLayout, final int spacing) {
            switch (this) {
                case HORIZONTAL:
                    gridLayout.columnSpacing(spacing);
                    break;
                case VERTICAL:
                    gridLayout.rowSpacing(spacing);
            }
        }

        public <T extends LayoutElement> T addChild(final GridLayout gridLayout, final T child, final int index, final LayoutSettings cellSettings) {
            return (T)(switch (this) {
                case HORIZONTAL -> gridLayout.addChild(child, 0, index, cellSettings);
                case VERTICAL -> gridLayout.addChild(child, index, 0, cellSettings);
            });
        }
    }
}