package com.poon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class PoonClickGui extends Screen {
    private int panelX, panelY, panelW = 260, panelH = 220;
    private boolean dragging;
    private double dragX, dragY;

    public PoonClickGui() {
        super(Component.literal("Poon Client"));
    }

    @Override
    protected void init() {
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x99000000);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF111319);
        g.fill(panelX, panelY, panelX + panelW, panelY + 28, 0xFF20232B);
        g.fill(panelX, panelY, panelX + 3, panelY + panelH, 0xFFE34242);

        g.drawString(font, "POON CLIENT", panelX + 12, panelY + 10, 0xFFFFFFFF, false);
        g.drawString(font, "1.21.11 | RIGHT SHIFT to close",
                panelX + 12, panelY + 36, 0xFF9DA3B1, false);

        drawModule(g, "Sprint", PoonClient.sprint, panelY + 60);
        drawModule(g, "Entity Glow", PoonClient.entityGlow, panelY + 94);
        drawModule(g, "Coordinates HUD (setting)", PoonClient.coordsHud, panelY + 128);

        g.drawString(font, "Click a row to toggle",
                panelX + 12, panelY + 178, 0xFFB8BDC8, false);
        g.drawString(font, "Poon Client - early build",
                panelX + 12, panelY + 198, 0xFF737987, false);

        super.render(g, mouseX, mouseY, delta);
    }

    private void drawModule(GuiGraphics g, String name, boolean enabled, int y) {
        int bg = enabled ? 0xFF303B36 : 0xFF242730;
        g.fill(panelX + 10, y, panelX + panelW - 10, y + 27, bg);
        g.drawString(font, name, panelX + 18, y + 9, 0xFFFFFFFF, false);

        String state = enabled ? "ON" : "OFF";
        int color = enabled ? 0xFF78E0A0 : 0xFF9298A5;
        g.drawString(font, state, panelX + panelW - 38, y + 9, color, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        if (button == 0) {
            if (mouseY >= panelY && mouseY <= panelY + 28
                    && mouseX >= panelX && mouseX <= panelX + panelW) {
                dragging = true;
                dragX = mouseX - panelX;
                dragY = mouseY - panelY;
                return true;
            }

            if (insideRow(mouseX, mouseY, panelY + 60)) {
                PoonClient.toggleSprint();
            } else if (insideRow(mouseX, mouseY, panelY + 94)) {
                PoonClient.toggleGlow();
            } else if (insideRow(mouseX, mouseY, panelY + 128)) {
                PoonClient.toggleCoords();
            }
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private boolean insideRow(double x, double y, int rowY) {
        return x >= panelX + 10 && x <= panelX + panelW - 10
                && y >= rowY && y <= rowY + 27;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging && event.button() == 0) {
            panelX = (int) (event.x() - dragX);
            panelY = (int) (event.y() - dragY);
            return true;
        }

        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            dragging = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();

        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }

        return super.keyPressed(event);
    }
}
