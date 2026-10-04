package com.poon.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class PoonClickGui extends Screen {
    private int panelX, panelY;
    private final int panelW = 460, panelH = 370;
    private boolean dragging;
    private double dragX, dragY;
    private int tab = 0;

    private static final String[] TABS = {"VISUAL", "COMBAT", "MOVE", "HUD", "PVP", "PRO", "THEME", "UTILITY"};
    private static final int[] ROWS = {104, 152, 200, 248};

    public PoonClickGui() { super(Component.literal("Poon Client")); }

    @Override
    protected void init() {
        panelX = Math.max(4, (width - panelW) / 2);
        panelY = Math.max(4, (height - panelH) / 2);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int accent = PoonClient.getAccentColor();

        // Hiệu ứng nền tối mờ sang trọng
        g.fill(0, 0, width, height, 0xAA030508);
        g.fill(panelX + 6, panelY + 6, panelX + panelW + 6, panelY + panelH + 6, 0x44000000);
        
        // Khung nền chính của Menu
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF0D1017);
        g.fill(panelX, panelY, panelX + panelW, panelY + 45, 0xFF141824);
        g.fill(panelX, panelY, panelX + 5, panelY + panelH, accent); // Thanh màu chủ đạo bên trái

        // Tiêu đề & Logo
        g.drawString(font, "POON", panelX + 18, panelY + 10, 0xFFFFFFFF, false);
        g.drawString(font, "CLIENT", panelX + 58, panelY + 10, accent, false);
        g.drawString(font, "ENHANCED v0.6.0", panelX + panelW - 130, panelY + 13, 0xFF8B95A9, false);
        g.drawString(font, "Customizable Fabric Utility Menu", panelX + 18, panelY + 28, 0xFF636E85, false);

        // Các tab chức năng phía trên
        int tabW = 48;
        for (int i = 0; i < TABS.length; i++) {
            int x = panelX + 12 + i * (tabW + 3);
            int y = panelY + 54;
            boolean selected = tab == i;
            g.fill(x, y, x + tabW, y + 25, selected ? 0xFF262D3D : 0xFF171B26);
            if (selected) g.fill(x, y + 23, x + tabW, y + 25, accent);
            int textX = x + (tabW - font.width(TABS[i])) / 2;
            g.drawString(font, TABS[i], textX, y + 8, selected ? 0xFFFFFFFF : 0xFF808B9F, false);
        }

        String heading = switch (tab) {
            case 0 -> "RENDER & WORLD MODULES";
            case 1 -> "COMBAT VISUALS";
            case 2 -> "MOVEMENT ENHANCEMENTS";
            case 3 -> "INFORMATION HUD MODULES";
            case 4 -> "MACE & PVP HELPERS";
            case 5 -> "COMBAT PRO FEATURES";
            case 6 -> "THEME & ACCENT COLOR SETTINGS";
            default -> "QUALITY OF LIFE";
        };
        g.drawString(font, heading, panelX + 16, panelY + 90, 0xFF7E8BA5, false);

        // Nội dung hiển thị theo từng Tab
        if (tab == 0) {
            drawModule(g, "Fullbright", "Increase local world brightness", PoonClient.fullbright, ROWS[0], accent);
            drawModule(g, "Watermark", "Show Poon Client label on HUD", PoonClient.watermark, ROWS[1], accent);
        } else if (tab == 1) {
            drawModule(g, "Entity Glow", "Highlight loaded entities locally", PoonClient.entityGlow, ROWS[0], accent);
            drawModule(g, "Target Info", "Show crosshair target and distance", PoonClient.targetHud, ROWS[1], accent);
            drawModule(g, "Hitboxes", "Show entity collision boxes", PoonClient.hitboxes, ROWS[2], accent);
        } else if (tab == 2) {
            drawModule(g, "Sprint Assist", "Sprint while moving forward", PoonClient.sprint, ROWS[0], accent);
        } else if (tab == 3) {
            drawModule(g, "Coordinates", "Display current XYZ position", PoonClient.coordsHud, ROWS[0], accent);
            drawModule(g, "FPS Counter", "Display frames per second", PoonClient.fpsHud, ROWS[1], accent);
            drawModule(g, "Ping Counter", "Display server latency", PoonClient.pingHud, ROWS[2], accent);
            drawModule(g, "Movement Speed", "Display horizontal speed", PoonClient.speedHud, ROWS[3], accent);
        } else if (tab == 4) {
            drawModule(g, "Mace PvP HUD", "Show mace availability and fall distance", PoonClient.maceHud, ROWS[0], accent);
            drawModule(g, "Auto Mace", "Attack target while falling with mace", PoonClient.autoMace, ROWS[1], accent);
            drawModule(g, "Totem Counter", "Count totems in inventory and offhand", PoonClient.totemCounter, ROWS[2], accent);
            drawModule(g, "Armor Durability", "Show remaining armor durability percent", PoonClient.armorHud, ROWS[3], accent);
        } else if (tab == 5) {
            drawModule(g, "TriggerBot", "Auto attack target under crosshair on cooldown", PoonClient.triggerBot, ROWS[0], accent);
            drawModule(g, "Auto Totem", "Auto replenish totem into offhand slot", PoonClient.autoTotem, ROWS[1], accent);
            drawModule(g, "Velocity", "Reduce knockback effects", PoonClient.velocity, ROWS[2], accent);
        } else if (tab == 6) {
            // Tab chỉnh màu chủ đạo giao diện
            int y = panelY + ROWS[0];
            g.fill(panelX + 14, y, panelX + panelW - 14, y + 60, 0xFF191D28);
            g.drawString(font, "Current Theme Accent: " + PoonClient.COLOR_NAMES[PoonClient.accentColorIndex], panelX + 24, y + 12, 0xFFFFFFFF, false);
            g.drawString(font, "Click the button below to cycle through colors.", panelX + 24, y + 28, 0xFF8B95A9, false);
            
            // Nút bấm đổi màu
            int btnX = panelX + 24;
            int btnY = y + 42;
            g.fill(btnX, btnY, btnX + 160, btnY + 24, accent);
            g.drawString(font, "Change Color Theme", btnX + 18, btnY + 8, 0xFFFFFFFF, false);
        } else {
            drawModule(g, "Auto Respawn", "Respawn automatically after death", PoonClient.autoRespawn, ROWS[0], accent);
        }

        // Chân trang Menu
        g.fill(panelX + 14, panelY + panelH - 32, panelX + panelW - 14, panelY + panelH - 31, 0xFF222836);
        g.drawString(font, "RIGHT SHIFT / ESC  •  CLOSE", panelX + 16, panelY + panelH - 20, 0xFF727D93, false);
        g.drawString(font, "CONFIG SAVED", panelX + panelW - 95, panelY + panelH - 20, accent, false);
        super.render(g, mouseX, mouseY, delta);
    }

    private void drawModule(GuiGraphics g, String title, String subtitle, boolean enabled, int offsetY, int accent) {
        int y = panelY + offsetY;
        int bg = enabled ? 0xFF1F2633 : 0xFF151922;
        g.fill(panelX + 14, y, panelX + panelW - 14, y + 41, bg);
        g.drawString(font, title, panelX + 24, y + 7, 0xFFF0F2F7, false);
        g.drawString(font, subtitle, panelX + 24, y + 22, 0xFF8B95A9, false);
        int sx = panelX + panelW - 55;
        int sy = y + 12;
        g.fill(sx, sy, sx + 29, sy + 16, enabled ? accent : 0xFF2E3544);
        int knobX = enabled ? sx + 16 : sx + 2;
        g.fill(knobX, sy + 2, knobX + 11, sy + 14, 0xFFFFFFFF);
        g.drawString(font, enabled ? "ON" : "OFF", panelX + panelW - 92, y + 15,
                enabled ? accent : 0xFF636E85, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0) {
            if (mouseY >= panelY && mouseY <= panelY + 45 && mouseX >= panelX && mouseX <= panelX + panelW) {
                dragging = true; dragX = mouseX - panelX; dragY = mouseY - panelY; return true;
            }
            int tabW = 48;
            for (int i = 0; i < TABS.length; i++) {
                int x = panelX + 12 + i * (tabW + 3);
                if (mouseX >= x && mouseX <= x + tabW && mouseY >= panelY + 54 && mouseY <= panelY + 79) {
                    tab = i; return true;
                }
            }
            if (tab == 0) {
                if (insideRow(mouseX, mouseY, ROWS[0])) PoonClient.toggleFullbright();
                else if (insideRow(mouseX, mouseY, ROWS[1])) PoonClient.toggleWatermark();
            } else if (tab == 1) {
                if (insideRow(mouseX, mouseY, ROWS[0])) PoonClient.toggleGlow();
                else if (insideRow(mouseX, mouseY, ROWS[1])) PoonClient.toggleTargetHud();
                else if (insideRow(mouseX, mouseY, ROWS[2])) PoonClient.toggleHitboxes();
            } else if (tab == 2) {
                if (insideRow(mouseX, mouseY, ROWS[0])) PoonClient.toggleSprint();
            } else if (tab == 3) {
                if (insideRow(mouseX, mouseY, ROWS[0])) PoonClient.toggleCoords();
                else if (insideRow(mouseX, mouseY, ROWS[1])) PoonClient.toggleFpsHud();
                else if (insideRow(mouseX, mouseY, ROWS[2])) PoonClient.togglePingHud();
                else if (insideRow(mouseX, mouseY, ROWS[3])) PoonClient.toggleSpeedHud();
            } else if (tab == 4) {
                if (insideRow(mouseX, mouseY, ROWS[0])) PoonClient.toggleMaceHud();
                else if (insideRow(mouseX, mouseY, ROWS[1])) PoonClient.toggleAutoMace();
                else if (insideRow(mouseX, mouseY, ROWS[2])) PoonClient.toggleTotemCounter();
                else if (insideRow(mouseX, mouseY, ROWS[3])) PoonClient.toggleArmorHud();
            } else if (tab == 5) {
                if (insideRow(mouseX, mouseY, ROWS[0])) PoonClient.toggleTriggerBot();
                else if (insideRow(mouseX, mouseY, ROWS[1])) PoonClient.toggleAutoTotem();
                else if (insideRow(mouseX, mouseY, ROWS[2])) PoonClient.toggleVelocity();
            } else if (tab == 6) {
                // Kiểm tra click vào nút đổi màu theme
                int y = panelY + ROWS[0];
                int btnX = panelX + 24;
                int btnY = y + 42;
                if (mouseX >= btnX && mouseX <= btnX + 160 && mouseY >= btnY && mouseY <= btnY + 24) {
                    PoonClient.cycleAccentColor();
                }
            } else if (tab == 7 && insideRow(mouseX, mouseY, ROWS[0])) {
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
