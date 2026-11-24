package com.djawad1.dont.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Fake crash screen. The world is already saved; after it the client shuts down cleanly. */
public class CrashScreen extends Screen {
    private int ticks = 0;

    public CrashScreen() { super(Text.literal("")); }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }

    @Override
    public void tick() {
        if (++ticks >= 170) this.client.scheduleStop();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xFF000000);
        int cx = this.width / 2, y = this.height / 2 - 40;
        ctx.drawCenteredTextWithShadow(this.textRenderer, "MINECRAFT HAS STOPPED RESPONDING", cx, y, 0xFFFFFF);
        if (ticks > 25) ctx.drawCenteredTextWithShadow(this.textRenderer, "UNKNOWN_PROCESS_DETECTED", cx, y + 16, 0xAAAAAA);
        if (ticks > 50) ctx.drawCenteredTextWithShadow(this.textRenderer, "ENTITY_SESSION_TERMINATED", cx, y + 30, 0xAAAAAA);
        if (ticks > 100) ctx.drawCenteredTextWithShadow(this.textRenderer, "IT FOLLOWED YOU OUT.", cx, y + 60, 0xCC0000);
    }
}
