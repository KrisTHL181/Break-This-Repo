package com.mojang.blaze3d.platform.cursor;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CursorTypes {
    public static final CursorType ARROW = CursorType.createStandardCursor(221185, "arrow", CursorType.DEFAULT);
    public static final CursorType IBEAM = CursorType.createStandardCursor(221186, "ibeam", CursorType.DEFAULT);
    public static final CursorType CROSSHAIR = CursorType.createStandardCursor(221187, "crosshair", CursorType.DEFAULT);
    public static final CursorType POINTING_HAND = CursorType.createStandardCursor(221188, "pointing_hand", CursorType.DEFAULT);
    public static final CursorType RESIZE_NS = CursorType.createStandardCursor(221190, "resize_ns", CursorType.DEFAULT);
    public static final CursorType RESIZE_EW = CursorType.createStandardCursor(221189, "resize_ew", CursorType.DEFAULT);
    public static final CursorType RESIZE_ALL = CursorType.createStandardCursor(221193, "resize_all", CursorType.DEFAULT);
    public static final CursorType NOT_ALLOWED = CursorType.createStandardCursor(221194, "not_allowed", CursorType.DEFAULT);
}