package com.poon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class PoonClickGui extends Screen {
    private int panelX, panelY;
    private final int panelW = 440, panelH = 354;
    private boolean dragging;
    private double dragX, dragY;
    private int tab = 0;

    private static final String[] TABS = {"VISUAL", "COMBAT", "MOVE", "HUD", "UTILITY"};
    private static final int[] ROWS = {104, 152, 200, 248};

    public PoonClickGui() { super(Component.literal("Poon Client")); }

    @Override
    protected void init() {
        panelX = Math.max(4, (width - panelW) / 2);
        panelY = Math.max(4, (height - panelH) / 2);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0x99060910);
        g.fill(panelX + 4, panelY + 5, panelX + panelW + 4, panelY + panelH + 5, 0x55000000);
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF10131B);
        g.fill(panelX, panelY, panelX + panelW, panelY + 43, 0xFF191D28);
        g.fill(panelX, panelY, panelX + 4, panelY + panelH, 0xFFE34262);

        g.drawString(font, "POON", panelX + 17, panelY + 9, 0xFFFFFFFF, false);
        g.drawString(font, "CLIENT", panelX + 56, panelY + 9, 0xFFE34262, false);
        g.drawString(font, "ENHANCED  /  1.21.11", panelX + panelW - 153, panelY + 12, 0xFF9CA5B8, false);
        g.drawString(font, "Independent Fabric client utilities", panelX + 17, panelY + 27, 0xFF737F95, false);

        int tabW = 79;
        for (int i = 0; i < TABS.length; i++) {
            int x = panelX + 12 + i * (tabW + 5);
            int y = panelY + 52;
            boolean selected = tab == i;
            g.fill(x, y, x + tabW, y + 25, selected ? 0xFF303747 : 0xFF1C202B);
            if (selected) g.fill(x, y + 22, x + tabW, y + 25, 0xFFE34262);
            int textX = x + (tabW - font.width(TABS[i])) / 2;
            g.drawString(font, TABS[i], textX, y + 8, selected ? 0xFFFFFFFF : 0xFF9CA5B8, false);
        }

        String heading = switch (tab) {
            case 0 -> "RENDER & WORLD";
            case 1 -> "COMBAT VISUALS";
            case 2 -> "MOVEMENT";
            case 3 -> "INFORMATION HUD";
            default -> "QUALITY OF LIFE";
        };
        g.drawString(font, heading, panelX + 16, panelY + 88, 0xFF7E8BA5, false);

        if (tab == 0) {
            drawModule(g, "Fullbright", "Increase local world brightness", PoonClient.fullbright, ROWS[0]);
            drawModule(g, "Watermark", "Show Poon Client label on HUD", PoonClient.watermark, ROWS[1]);
        } else if (tab == 1) {
            drawModule(g, "Entity Glow", "Highlight loaded entities locally", PoonClient.entityGlow, ROWS[0]);
            drawModule(g, "Target Info", "Show crosshair target and distance", PoonClient.targetHud, ROWS[1]);
        } else if (tab == 2) {
            drawModule(g, "Sprint Assist", "Sprint while moving forward", PoonClient.sprint, ROWS[0]);
        } else if (tab == 3) {
            drawModule(g, "Coordinates", "Display current XYZ position", PoonClient.coordsHud, ROWS[0]);
            drawModule(g, "FPS Counter", "Display frames per second", PoonClient.fpsHud, ROWS[1]);
            drawModule(g, "Ping Counter", "Display server latency", PoonClient.pingHud, ROWS[2]);
            drawModule(g, "Movement Speed", "Display horizontal speed", PoonClient.speedHud, ROWS[3]);
        } else {
            drawModule(g, "Auto Respawn", "Respawn automatically after death", PoonClient.autoRespawn, ROWS[0]);
        }

        g.fill(panelX + 14, panelY + panelH - 32, panelX + panelW - 14, panelY + panelH - 31, 0xFF292E3A);
        g.drawString(font, "RIGHT SHIFT / ESC  •  CLOSE", panelX + 16, panelY + panelH - 21, 0xFF858EA1, false);
        g.drawString(font, "SETTINGS SAVED AUTOMATICALLY", panelX + panelW - 180, panelY + panelH - 21, 0xFF596274, false);
        super.render(g, mouseX, mouseY, delta);
    }

    private void drawModule(GuiGraphics g, String title, String subtitle, boolean enabled, int offsetY) {
        int y = panelY + offsetY;
        int bg = enabled ? 0xFF252F35 : 0xFF1A1E28;
        g.fill(panelX + 14, y, panelX + panelW - 14, y + 41, bg);
        g.drawString(font, title, panelX + 24, y + 7, 0xFFF0F2F7, false);
        g.drawString(font, subtitle, panelX + 24, y + 22, 0xFF929BAE, false);
        int sx = panelX + panelW - 55;
        int sy = y + 12;
        g.fill(sx, sy, sx + 29, sy + 16, enabled ? 0xFFB93252 : 0xFF343A48);
        int knobX = enabled ? sx + 16 : sx + 2;
        g.fill(knobX, sy + 2, knobX + 11, sy + 14, 0xFFF5F6FA);
        g.drawString(font, enabled ? "ON" : "OFF", panelX + panelW - 91, y + 15,
                enabled ? 0xFFFF8099 : 0xFF737D91, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0) {
            if (mouseY >= panelY && mouseY <= panelY + 43 && mouseX >= panelX && mouseX <= panelX + panelW) {
                dragging = true; dragX = mouseX - panelX; dragY = mouseY - panelY; return true;
            }
            int tabW = 79;
            for (int i = 0; i < TABS.length; i++) {
                int x = panelX + 12 + i * (tabW + 5);
                if (mouseX >= x && mouseX <= x + tabW && mouseY >= panelY + 52 && mouseY <= panelY + 77) {
                    tab = i; return true;
                }
            }
            if (tab == 0) {
                if (insideRow(mouseX, mouseY, ROWS[0])) PoonClient.toggleFullbright();
                else if (insideRow(mouseX, mouseY, ROWS[1])) PoonClient.toggleWatermark();
            } else if (tab == 1) {
                if (insideRow(mouseX, mouseY, ROWS[0])) PoonClient.toggleGlow();
                else if (insideRow(mouseX, mouseY, ROWS[1])) PoonClient.toggleTargetHud();
            } else if (tab == 2) {
                if (insideRow(mouseX, mouseY, ROWS[0])) PoonClient.toggleSprint();
            } else if (tab == 3) {
                if (insideRow(mouseX, mouseY, ROWS[0])) PoonClient.toggleCoords();
                else if (insideRow(mouseX, mouseY, ROWS[1])) PoonClient.toggleFpsHud();
                else if (insideRow(mouseX, mouseY, ROWS[2])) PoonClient.togglePingHud();
                else if (insideRow(mouseX, mouseY, ROWS[3])) PoonClient.toggleSpeedHud();
            } else if (tab == 4 && insideRow(mouseX, mouseY, ROWS[0])) {
                PoonClient.toggleAutoRespawn();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean insideRow(double x, double y, int offsetY) {
        int rowY = panelY + offsetY;
        return x >= panelX + 14 && x <= panelX + panelW - 14 && y >= rowY && y <= rowY + 41;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (dragging && event.button() == 0) {
            panelX = Math.max(0, Math.min(width - panelW, (int) (event.x() - this.dragX)));
            panelY = Math.max(0, Math.min(height - panelH, (int) (event.y() - this.dragY)));
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) dragging = false;
        return super.mouseReleased(event);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose(); return true;
        }
        return super.keyPressed(event);
    }
}
