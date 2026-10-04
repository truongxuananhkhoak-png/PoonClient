package com.poon.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PoonClient implements ClientModInitializer {
    public static final String BRAND = "Poon Client";
    public static final String VERSION = "0.3.0";

    public static boolean sprint = false;
    public static boolean entityGlow = false;
    public static boolean coordsHud = true;
    public static boolean fullbright = false;
    public static boolean autoRespawn = false;
    public static boolean fpsHud = true;
    public static boolean pingHud = true;
    public static boolean speedHud = false;
    public static boolean targetHud = true;
    public static boolean watermark = true;

    private static KeyMapping openMenu;
    private static KeyMapping toggleSprintKey;
    private static KeyMapping toggleFullbrightKey;
    private static KeyMapping toggleCoordsKey;
    private static double previousGamma = 1.0;
    private static boolean gammaCaptured = false;
    private static double lastX, lastZ;
    private static double speedBlocksPerSecond;
    private static long lastSpeedSample;
    private static boolean positionSampled;
    private static Path configPath;

    @Override
    public void onInitializeClient() {
        configPath = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("poonclient.properties");
        loadConfig();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.poonclient.open_menu", GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));
        toggleSprintKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.poonclient.toggle_sprint", GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.MISC));
        toggleFullbrightKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.poonclient.toggle_fullbright", GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.MISC));
        toggleCoordsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.poonclient.toggle_coords", GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                if (client.screen == null) client.setScreen(new PoonClickGui());
                else if (client.screen instanceof PoonClickGui) client.setScreen(null);
            }
            while (toggleSprintKey.consumeClick()) toggleSprint();
            while (toggleFullbrightKey.consumeClick()) toggleFullbright();
            while (toggleCoordsKey.consumeClick()) toggleCoords();

            if (client.player == null || client.level == null) {
                positionSampled = false;
                if (gammaCaptured && client.player == null) restoreGamma(client);
                return;
            }

            if (sprint && client.options.keyUp.isDown() && !client.player.isCrouching()) {
                client.player.setSprinting(true);
            }

            if (fullbright) {
                if (!gammaCaptured) {
                    previousGamma = client.options.gamma().get();
                    gammaCaptured = true;
                }
                client.options.gamma().set(16.0);
            } else if (gammaCaptured) {
                restoreGamma(client);
            }

            if (autoRespawn && client.player.isDeadOrDying()) client.player.respawn();

            if (entityGlow) {
                for (Entity entity : client.level.entitiesForRendering()) {
                    if (entity != client.player) entity.setGlowingTag(true);
                }
            }
            sampleSpeed(client);
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> renderHud(Minecraft.getInstance(), graphics));
    }

    private static void sampleSpeed(Minecraft client) {
        long now = System.currentTimeMillis();
        if (lastSpeedSample != 0 && now - lastSpeedSample >= 250) {
            double dx = client.player.getX() - lastX;
            double dz = client.player.getZ() - lastZ;
            double seconds = (now - lastSpeedSample) / 1000.0;
            speedBlocksPerSecond = Math.sqrt(dx * dx + dz * dz) / Math.max(seconds, 0.001);
        }
        lastX = client.player.getX();
        lastZ = client.player.getZ();
        lastSpeedSample = now;
        positionSampled = true;
    }

    private static void renderHud(Minecraft client, net.minecraft.client.gui.GuiGraphics g) {
        if (client.player == null || client.options.hideGui) return;
        int x = 8;
        int y = 8;
        int row = 17;
        if (watermark) {
            drawHudRow(client, g, x, y, "POON CLIENT", "v" + VERSION);
            y += row;
        }
        if (coordsHud) {
            drawHudRow(client, g, x, y, "XYZ", String.format("%.1f  %.1f  %.1f",
                    client.player.getX(), client.player.getY(), client.player.getZ()));
            y += row;
        }
        if (fpsHud) {
            drawHudRow(client, g, x, y, "FPS", Integer.toString(client.getFps()));
            y += row;
        }
        if (pingHud && client.getConnection() != null) {
            var info = client.getConnection().getPlayerInfo(client.player.getUUID());
            if (info != null) {
                drawHudRow(client, g, x, y, "PING", info.getLatency() + " ms");
                y += row;
            }
        }
        if (speedHud) drawHudRow(client, g, x, y, "SPEED", String.format("%.2f m/s", speedBlocksPerSecond));

        if (targetHud && client.crosshairPickEntity != null) {
            Entity target = client.crosshairPickEntity;
            int tx = 8;
            int ty = client.getWindow().getGuiScaledHeight() - 34;
            g.fill(tx, ty, tx + 170, ty + 25, 0xB8101219);
            g.fill(tx, ty, tx + 2, ty + 25, 0xFFE34262);
            g.drawString(client.font, "TARGET", tx + 8, ty + 4, 0xFFE34262, false);
            g.drawString(client.font, target.getName().getString(), tx + 58, ty + 4, 0xFFFFFFFF, false);
            g.drawString(client.font, String.format("%.1f blocks", client.player.distanceTo(target)), tx + 8, ty + 14, 0xFFADB5C5, false);
        }
    }

    private static void drawHudRow(Minecraft client, net.minecraft.client.gui.GuiGraphics g, int x, int y, String label, String value) {
        int width = client.font.width(label) + client.font.width(value) + 22;
        g.fill(x, y, x + width, y + 15, 0xB8101219);
        g.fill(x, y, x + 2, y + 15, 0xFFE34262);
        g.drawString(client.font, label, x + 7, y + 4, 0xFFE8EBF2, false);
        g.drawString(client.font, value, x + width - client.font.width(value) - 7, y + 4, 0xFF9DA8BC, false);
    }

    private static void restoreGamma(Minecraft client) {
        if (client.options != null) client.options.gamma().set(previousGamma);
        gammaCaptured = false;
    }

    public static void toggleSprint() { sprint = !sprint; saveConfig(); }
    public static void toggleGlow() { entityGlow = !entityGlow; saveConfig(); }
    public static void toggleCoords() { coordsHud = !coordsHud; saveConfig(); }
    public static void toggleFullbright() { fullbright = !fullbright; saveConfig(); }
    public static void toggleAutoRespawn() { autoRespawn = !autoRespawn; saveConfig(); }
    public static void toggleFpsHud() { fpsHud = !fpsHud; saveConfig(); }
    public static void togglePingHud() { pingHud = !pingHud; saveConfig(); }
    public static void toggleSpeedHud() { speedHud = !speedHud; saveConfig(); }
    public static void toggleTargetHud() { targetHud = !targetHud; saveConfig(); }
    public static void toggleWatermark() { watermark = !watermark; saveConfig(); }

    private static void loadConfig() {
        Properties p = new Properties();
        try {
            Files.createDirectories(configPath.getParent());
            if (Files.exists(configPath)) try (InputStream in = Files.newInputStream(configPath)) { p.load(in); }
            sprint = bool(p, "sprint", sprint);
            entityGlow = bool(p, "entityGlow", entityGlow);
            coordsHud = bool(p, "coordsHud", coordsHud);
            fullbright = bool(p, "fullbright", fullbright);
            autoRespawn = bool(p, "autoRespawn", autoRespawn);
            fpsHud = bool(p, "fpsHud", fpsHud);
            pingHud = bool(p, "pingHud", pingHud);
            speedHud = bool(p, "speedHud", speedHud);
            targetHud = bool(p, "targetHud", targetHud);
            watermark = bool(p, "watermark", watermark);
        } catch (IOException ignored) { }
    }

    private static boolean bool(Properties p, String key, boolean fallback) {
        return p.containsKey(key) ? Boolean.parseBoolean(p.getProperty(key)) : fallback;
    }

    private static void saveConfig() {
        if (configPath == null) return;
        Properties p = new Properties();
        p.setProperty("sprint", Boolean.toString(sprint));
        p.setProperty("entityGlow", Boolean.toString(entityGlow));
        p.setProperty("coordsHud", Boolean.toString(coordsHud));
        p.setProperty("fullbright", Boolean.toString(fullbright));
        p.setProperty("autoRespawn", Boolean.toString(autoRespawn));
        p.setProperty("fpsHud", Boolean.toString(fpsHud));
        p.setProperty("pingHud", Boolean.toString(pingHud));
        p.setProperty("speedHud", Boolean.toString(speedHud));
        p.setProperty("targetHud", Boolean.toString(targetHud));
        p.setProperty("watermark", Boolean.toString(watermark));
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream out = Files.newOutputStream(configPath)) { p.store(out, "Poon Client settings"); }
        } catch (IOException ignored) { }
    }
}
