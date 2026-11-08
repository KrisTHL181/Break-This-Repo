package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.client.ClientModelManager;
import com.elfmcys.yesstevemodel.client.model.ModelAssembly;
import com.elfmcys.yesstevemodel.config.GeneralConfig;
import com.elfmcys.yesstevemodel.geckolib3.core.molang.util.StringPool;
import com.elfmcys.yesstevemodel.resource.models.AuthorInfo;
import com.elfmcys.yesstevemodel.resource.models.Metadata;
import com.elfmcys.yesstevemodel.resource.models.ModelPackData;
import com.elfmcys.yesstevemodel.util.FileTypeUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.StringUtils;
import rip.ysm.gpu.BlurStack;
import rip.ysm.gpu.GpuCapability;
import rip.ysm.pinyin.PinyinMatcher;

import java.util.*;

public class SearchSuggestions {
    private final Font font;
    private final EditBox searchBox;
    private final Map<String, ModelPackData> packs;

    private final List<Entry> entries = new ArrayList<>();
    private String lastInput;
    private int selected;
    private int scrollOffset;
    private int cachedWidth;
    private boolean suppressed;
    private String pendingPackPath;

    private float openDisplay;
    private float selectionDisplay;
    private float scrollDisplay;
    private long lastFrameNanos;

    public SearchSuggestions(Font font, EditBox searchBox, Map<String, ModelPackData> packs, SearchSuggestions previous) {
        this.font = font;
        this.searchBox = searchBox;
        this.packs = packs;
        if (previous != null) {
            this.entries.addAll(previous.entries);
            this.lastInput = previous.lastInput;
            this.selected = previous.selected;
            this.scrollOffset = previous.scrollOffset;
            this.suppressed = previous.suppressed;
            this.openDisplay = previous.openDisplay;
            this.selectionDisplay = previous.selectionDisplay;
            this.scrollDisplay = previous.scrollDisplay;
        }
    }

    public void refresh() {
        String input = searchBox.getValue();
        if (input.equals(lastInput)) {
            return;
        }
        lastInput = input;
        suppressed = false;
        rebuild(input);
    }

    public void suppress() {
        suppressed = true;
    }

    private void rebuild(String input) {
        entries.clear();
        cachedWidth = 0;
        selected = 0;
        scrollOffset = 0;
        scrollDisplay = 0.0f;
        selectionDisplay = 0.0f;

        String query = input.toLowerCase(Locale.ENGLISH);
        if (query.startsWith("@")) {
            collectAuthors(query.substring(1));
        } else if (query.startsWith("#")) {
            collectPacks(query.substring(1));
        } else {
            collectModels(query);
        }

        entries.sort(Comparator.<Entry>comparingInt(entry -> entry.matchIndex).thenComparing(entry -> entry.text, String.CASE_INSENSITIVE_ORDER));
    }

    private void collectModels(String query) {
        for (Map.Entry<String, ModelAssembly> entry : ClientModelManager.getModelAssemblyMap().entrySet()) {
            String fileName = FileTypeUtil.splitFileNameAndParentDir(entry.getKey()).left();
            String display = fileName;
            Metadata metadata = entry.getValue().getModelData().getExtraInfo();
            if (metadata != null && StringUtils.isNotBlank(metadata.getName())) {
                String localized = ModelMetadataPresenter.getLocalizedModelString(entry.getValue(), "metadata.name", metadata.getName());
                if (StringUtils.isNotBlank(localized)) {
                    display = localized;
                }
            }
            int index = indexOf(display, query);
            if (index < 0) {
                index = indexOf(fileName, query);
                if (index >= 0) {
                    add(new Entry(display, display, StringPool.EMPTY, 0, 0));
                }
                continue;
            }
            add(new Entry(display, display, StringPool.EMPTY, index, query.length()));
        }
    }

    private void collectAuthors(String query) {
        Map<String, Integer> authors = new LinkedHashMap<>();
        for (Map.Entry<String, ModelAssembly> entry : ClientModelManager.getModelAssemblyMap().entrySet()) {
            Metadata metadata = entry.getValue().getModelData().getExtraInfo();
            if (metadata == null) {
                continue;
            }
            int authorIndex = 0;
            for (AuthorInfo author : metadata.getAuthors()) {
                String name = ModelMetadataPresenter.getLocalizedModelString(entry.getValue(), "metadata.authors.%d.name".formatted(authorIndex), author.getName());
                authorIndex++;
                if (StringUtils.isBlank(name)) {
                    continue;
                }
                authors.merge(name, 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> author : authors.entrySet()) {
            int index = indexOf(author.getKey(), query);
            if (index >= 0) {
                add(new Entry("@" + author.getKey(), author.getKey(), author.getValue() + " model(s)", index, query.length()));
            }
        }
    }

    private void collectPacks(String query) {
        for (Map.Entry<String, ModelPackData> entry : packs.entrySet()) {
            ModelPackData pack = entry.getValue();
            String name = ModelMetadataPresenter.getLocalizedString(pack, "name", pack.getName());
            if (StringUtils.isBlank(name)) {
                name = FileTypeUtil.getFinalPathSegment(entry.getKey());
            }
            int index = indexOf(name, query);
            if (index >= 0) {
                add(new Entry("#" + name, name, StringPool.EMPTY, index, query.length(), entry.getKey()));
            }
        }
    }

    private void add(Entry entry) {
        for (Entry existing : entries) {
            if (existing.text.equalsIgnoreCase(entry.text)) {
                return;
            }
        }
        entries.add(entry);
    }

    private static int indexOf(String value, String query) {
        return PinyinMatcher.indexOf(value, query);
    }

    private static int maxVisible() {
        if (GeneralConfig.SEARCH_SUGGESTION_COUNT == null) {
            return 8;
        }
        return (int) Math.round(GeneralConfig.SEARCH_SUGGESTION_COUNT.get());
    }

    public boolean isVisible() {
        return !suppressed && searchBox.isFocused() && !entries.isEmpty();
    }

    public boolean isOccluding(double mouseX, double mouseY) {
        if (!isVisible()) {
            return false;
        }
        int left = getLeft();
        int top = getTop();
        int height = Math.min(entries.size(), maxVisible()) * 12;
        return mouseX >= left && mouseX <= left + getWidth() && mouseY >= top && mouseY <= top + height;
    }

    public boolean keyPressed(int keyCode) {
        if (!isVisible()) {
            return false;
        }
        if (keyCode == 264) {
            move(1);
            return true;
        }
        if (keyCode == 265) {
            move(-1);
            return true;
        }
        if (keyCode == 258 || keyCode == 257) {
            pendingPackPath = applySelected();
            return true;
        }
        if (keyCode == 256) {
            suppress();
            return true;
        }
        return false;
    }

    private void move(int delta) {
        selected = Math.floorMod(selected + delta, entries.size());
        if (selected < scrollOffset) {
            scrollOffset = selected;
        } else if (selected >= scrollOffset + maxVisible()) {
            scrollOffset = selected - maxVisible() + 1;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!isVisible()) {
            return false;
        }
        int index = indexAt(mouseX, mouseY);
        if (index < 0) {
            return false;
        }
        selected = index;
        pendingPackPath = applySelected();
        return true;
    }

    public String consumePendingPackPath() {
        String path = pendingPackPath;
        pendingPackPath = null;
        return path;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isVisible() || indexAt(mouseX, mouseY) < 0) {
            return false;
        }
        int max = Math.max(0, entries.size() - maxVisible());
        scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(delta), 0, max);
        return true;
    }

    private int indexAt(double mouseX, double mouseY) {
        int visibleCount = Math.min(entries.size(), maxVisible());
        int left = getLeft();
        int top = getTop();
        if (mouseX < left || mouseX > left + getWidth() || mouseY < top || mouseY > top + visibleCount * 12) {
            return -1;
        }
        int row = (int) ((mouseY - top) / 12);
        int index = scrollOffset + row;
        return index >= 0 && index < entries.size() ? index : -1;
    }

    private String applySelected() {
        if (entries.isEmpty()) {
            return null;
        }
        Entry entry = entries.get(selected);
        if (entry.packPath != null) {
            searchBox.setValue(StringPool.EMPTY);
            searchBox.setFocused(false);
        } else {
            searchBox.setValue(entry.insertion);
            searchBox.moveCursorToEnd();
        }
        suppress();
        lastInput = searchBox.getValue();
        return entry.packPath;
    }

    private int getLeft() {
        return searchBox.getX() - 1;
    }

    private int getTop() {
        return searchBox.getY() + searchBox.getHeight() + 1;
    }

    private int getWidth() {
        if (cachedWidth == 0) {
            cachedWidth = searchBox.getWidth() + 2;
            for (Entry entry : entries) {
                int hintWidth = entry.hint.isEmpty() ? 0 : font.width(entry.hint) + 8;
                cachedWidth = Math.max(cachedWidth, font.width(entry.text) + hintWidth + 14);
            }
            cachedWidth = Math.min(cachedWidth, 220); // 最大宽度220。
        }
        return cachedWidth;
    }

    public void render(GuiGraphics guiGraphics) {
        float dt = tickDelta();
        float target = isVisible() ? 1.0f : 0.0f;
        openDisplay += (target - openDisplay) * (1.0f - (float) Math.exp(-dt * 22.0f));
        if (openDisplay < 0.01f) {
            if (target == 0.0f) {
                return;
            }
            openDisplay = 0.01f;
        }
        if (entries.isEmpty()) {
            return;
        }

        selectionDisplay += (selected - selectionDisplay) * (1.0f - (float) Math.exp(-dt * 25.0f));
        scrollDisplay += (scrollOffset - scrollDisplay) * (1.0f - (float) Math.exp(-dt * 20.0f));

        int visibleCount = Math.min(entries.size(), maxVisible());
        int left = getLeft();
        int top = getTop();
        int width = getWidth();
        int fullHeight = visibleCount * 12;
        int height = Math.max(1, Math.round(fullHeight * openDisplay));

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0f, 0.0f, 500.0f);

        boolean blurred = GeneralConfig.BLUR_GUI.get() && GpuCapability.isAvailable();
        if (blurred) {
            BlurStack.pushBlur(left, top, width, height, 3.0f, 16.0f, 0xFF4F4F4F);
            BlurStack.flush(guiGraphics);
        }

        guiGraphics.fill(left, top, left + width, top + height, blurred ? 0x99000000 : 0xE6100010);

        guiGraphics.enableScissor(left, top, left + width, top + height);
        float selectionY = top + (selectionDisplay - scrollDisplay) * 12;
        if (selectionY + 12 > top && selectionY < top + height) {
            guiGraphics.fill(left, Math.round(selectionY), left + width, Math.round(selectionY) + 12, 0xFF3C3C50);
        }

        for (int i = 0; i < entries.size(); i++) {
            float rowY = top + (i - scrollDisplay) * 12;
            if (rowY + 12 < top || rowY > top + height) {
                continue;
            }
            Entry entry = entries.get(i);
            int textY = Math.round(rowY) + 2;
            int textX = left + 4;
            boolean isSelected = i == selected;
            int hintWidth = StringUtils.isNotBlank(entry.hint) ? font.width(entry.hint) + 8 : 0;
            renderHighlighted(guiGraphics, entry, textX, textY, isSelected, width - 8 - hintWidth);
            if (hintWidth > 0) {
                guiGraphics.drawString(font, Component.literal(entry.hint).withStyle(ChatFormatting.ITALIC), left + width - hintWidth + 4, textY, 0xFF5F5F6F, false);
            }
        }
        guiGraphics.disableScissor();

        if (entries.size() > maxVisible()) {
            int barHeight = Math.max(8, height * visibleCount / entries.size());
            int maxScroll = Math.max(1, entries.size() - visibleCount);
            int barY = top + Math.round((height - barHeight) * (scrollDisplay / maxScroll));
            guiGraphics.fill(left + width - 2, barY, left + width - 1, barY + barHeight, 0xFF7F7F9F);
        }

        guiGraphics.pose().popPose();
    }

    private void renderHighlighted(GuiGraphics guiGraphics, Entry entry, int x, int y, boolean isSelected, int maxWidth) {
        int baseColor = isSelected ? 0xFFFFFF55 : -1;
        String text = entry.text;
        if (font.width(text) > maxWidth) {
            text = font.plainSubstrByWidth(text, maxWidth - font.width("...")) + "...";
        }
        if (entry.matchLength <= 0 || entry.matchIndex < 0 || entry.matchIndex + entry.matchLength > text.length()) {
            guiGraphics.drawString(font, text, x, y, baseColor, false);
            return;
        }
        String before = text.substring(0, entry.matchIndex);
        String match = text.substring(entry.matchIndex, entry.matchIndex + entry.matchLength);
        String after = text.substring(entry.matchIndex + entry.matchLength);
        int cursor = x;
        guiGraphics.drawString(font, before, cursor, y, baseColor, false);
        cursor += font.width(before);
        guiGraphics.drawString(font, match, cursor, y, 0xFF55FF55, false);
        cursor += font.width(match);
        guiGraphics.drawString(font, after, cursor, y, baseColor, false);
    }

    private float tickDelta() {
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return 0.0f;
        }
        float dt = (now - lastFrameNanos) / 1.0E9f;
        lastFrameNanos = now;
        return Math.min(dt, 0.1f);
    }

    private static final class Entry {
        private final String insertion;
        private final String text;
        private final String hint;
        private final int matchIndex;
        private final int matchLength;
        private final String packPath;

        private Entry(String insertion, String text, String hint, int matchIndex, int matchLength) {
            this(insertion, text, hint, matchIndex, matchLength, null);
        }

        private Entry(String insertion, String text, String hint, int matchIndex, int matchLength, String packPath) {
            this.insertion = insertion;
            this.text = text;
            this.hint = hint == null ? StringPool.EMPTY : hint;
            this.matchIndex = matchIndex;
            this.matchLength = matchLength;
            this.packPath = packPath;
        }
    }
}
