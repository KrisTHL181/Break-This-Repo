package com.mojang.blaze3d.systems;

public class ScissorState {
   private boolean enabled;
   private int x;
   private int y;
   private int width;
   private int height;

   public ScissorState() {
   }

   public ScissorState(final ScissorState state) {
      this.enabled = state.enabled;
      this.x = state.x;
      this.y = state.y;
      this.width = state.width;
      this.height = state.height;
   }

   public void enable(final int x, final int y, final int width, final int height) {
      this.enabled = true;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public void disable() {
      this.enabled = false;
   }

   public boolean enabled() {
      return this.enabled;
   }

   public int x() {
      return this.x;
   }

   public int y() {
      return this.y;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   public void setFrom(final ScissorState state) {
      this.enabled = state.enabled;
      this.x = state.x;
      this.y = state.y;
      this.width = state.width;
      this.height = state.height;
   }
}
