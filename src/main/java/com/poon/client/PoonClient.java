package com.poon.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public final class PoonClient implements ClientModInitializer {
    public static final String BRAND = "Poon Client";
    public static final String VERSION = "0.6.0";

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
    public static boolean maceHud = true;
    public static boolean totemCounter = true;
    public static boolean armorHud = false;
    public static boolean throwableCounter = true;
    public static boolean hitboxes = false;
    public static boolean autoMace = false;
    public static boolean triggerBot = false;
    public static boolean autoTotem = false;
    public static boolean velocity = false;

    // Tùy chỉnh màu chủ đạo (Mặc định: Đỏ Hồng hiện đại - #E34262)
    public static int accentColorIndex = 0;
    public static final int[] ACCENT_COLORS = {
            0xFFE34262, // Đỏ hồng (Default)
            0xFF3B82F6, // Xanh dương
            0xFF10B981, // Xanh lá
            0xFF8B5CF6, // Tím
            0xFFF59E0B, // Cam vàng
            0xFFEC4899  // Hồng sen
    };
    public static final String[] COLOR_NAMES = {"Crimson Red", "Ocean Blue", "Emerald Green", "Neon Purple", "Sunset Amber", "Hot Pink"};

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

    private static int tickCounter;
    private static String cachedCoords = "0.0  0.0  0.0";
    private static String cachedFps = "0";
    private static String cachedPing = "-- ms";
    private static String cachedSpeed = "0.00 m/s";
    private static String cachedMaceStatus = "NO MACE  |  Fall 0.0";
    private static String cachedArmor = "H --  C --  L --  B --";
    private static int cachedTotems;
    private static int cachedPearls;
    private static int cachedWindCharges;
    private static final Map<Entity, Boolean> glowOriginalStates = new IdentityHashMap<>();
    private static final double GLOW_RANGE_SQUARED = 48.0 * 48.0;

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
                if (gammaCaptured) restoreGamma(client);
                restoreTrackedGlow();
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
                if (client.options.gamma().get() != 16.0) client.options.gamma().set(16.0);
            } else if (gammaCaptured) {
                restoreGamma(client);
            }

            if (autoRespawn && client.player.isDeadOrDying()) client.player.respawn();

            if (autoTotem) {
                handleAutoTotem(client);
            }

            if (autoMace && client.gameMode != null && client.crosshairPickEntity != null
                    && itemPath(client.player.getMainHandItem()).equals("mace")
                    && !client.player.onGround() && client.player.getDeltaMovement().y < 0.0
                    && client.player.fallDistance >= 1.5F
                    && client.player.distanceTo(client.crosshairPickEntity) <= 4.5F
                    && client.player.getAttackStrengthScale(0.0F) >= 1.0F
                    && client.crosshairPickEntity.isAlive()) {
                client.gameMode.attack(client.player, client.crosshairPickEntity);
                client.player.swing(InteractionHand.MAIN_HAND);
            }

            if (triggerBot && client.gameMode != null && client.crosshairPickEntity != null
                    && client.crosshairPickEntity.isAlive()
                    && client.player.distanceTo(client.crosshairPickEntity) <= 4.0F
                    && client.player.getAttackStrengthScale(0.0F) >= 1.0F) {
                client.gameMode.attack(client.player, client.crosshairPickEntity);
                client.player.swing(InteractionHand.MAIN_HAND);
            }

            if (++tickCounter % 10 == 0) {
                updateEntityGlow(client);
                refreshHudCache(client);
            }
            sampleSpeed(client);
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> renderHud(Minecraft.getInstance(), graphics));
    }

    public static int getAccentColor() {
        if (accentColorIndex < 0 || accentColorIndex >= ACCENT_COLORS.length) accentColorIndex = 0;
        return ACCENT_COLORS[accentColorIndex];
    }

    private static void handleAutoTotem(Minecraft client) {
        if (client.player == null) return;
        ItemStack offhandItem = client.player.getOffhandItem();
        if (offhandItem.is(Items.TOTEM_OF_UNDYING)) return;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                client.gameMode.handleInventoryMouseClick(0, i < 9 ? i + 36 : i, 0, net.minecraft.world.inventory.ClickType.PICKUP, client.player);
                client.gameMode.handleInventoryMouseClick(0, 45, 0, net.minecraft.world.inventory.ClickType.PICKUP, client.player);
                client.gameMode.handleInventoryMouseClick(0, i < 9 ? i + 36 : i, 0, net.minecraft.world.inventory.ClickType.PICKUP, client.player);
                break;
            }
        }
    }

    private static void updateEntityGlow(Minecraft client) {
        if (!entityGlow || client.player == null || client.level == null) {
            restoreTrackedGlow();
            return;
        }
        Iterator<Map.Entry<Entity, Boolean>> iterator = glowOriginalStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, Boolean> entry = iterator.next();
            Entity entity = entry.getKey();
            if (!entity.isAlive() || entity.distanceToSqr(client.player) > GLOW_RANGE_SQUARED) {
                entity.setGlowingTag(entry.getValue());
                iterator.remove();
            }
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == client.player || !entity.isAlive()
                    || entity.distanceToSqr(client.player) > GLOW_RANGE_SQUARED) continue;
            if (!glowOriginalStates.containsKey(entity)) {
                glowOriginalStates.put(entity, entity.isCurrentlyGlowing());
                entity.setGlowingTag(true);
            }
        }
    }

    private static void restoreTrackedGlow() {
        if (glowOriginalStates.isEmpty()) return;
        for (Map.Entry<Entity, Boolean> entry : glowOriginalStates.entrySet()) {
            entry.getKey().setGlowingTag(entry.getValue());
        }
        glowOriginalStates.clear();
    }

    private static void refreshHudCache(Minecraft client) {
        if (client.player == null) return;
        cachedCoords = String.format("%.1f  %.1f  %.1f",
                client.player.getX(), client.player.getY(), client.player.getZ());
        cachedFps = Integer.toString(client.getFps());
        cachedPing = "-- ms";
        if (client.getConnection() != null) {
            var info = client.getConnection().getPlayerInfo(client.player.getUUID());
            if (info != null) cachedPing = info.getLatency() + " ms";
        }
        cachedSpeed = String.format("%.2f m/s", speedBlocksPerSecond);
        String held = itemPath(client.player.getMainHandItem()).equals("mace") ? "MACE HELD" :
                (countItem(client, "mace") > 0 ? "MACE IN INVENTORY" : "NO MACE");
        cachedMaceStatus = held + "  |  Fall " + String.format("%.1f", client.player.fallDistance);
        cachedTotems = countItem(client, "totem_of_undying");
        cachedPearls = countItem(client, "ender_pearl");
        cachedWindCharges = countItem(client, "wind_charge");
        cachedArmor = "H " + durability(client.player.getItemBySlot(EquipmentSlot.HEAD))
                + "  C " + durability(client.player.getItemBySlot(EquipmentSlot.CHEST))
                + "  L " + durability(client.player.getItemBySlot(EquipmentSlot.LEGS))
                + "  B " + durability(client.player.getItemBySlot(EquipmentSlot.FEET));
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
        int accent = getAccentColor();

        if (watermark) {
            drawHudRow(client, g, x, y, "POON CLIENT", "v" + VERSION, accent);
            y += row;
        }
        if (coordsHud) {
            drawHudRow(client, g, x, y, "XYZ", cachedCoords, accent);
            y += row;
        }
        if (fpsHud) {
            drawHudRow(client, g, x, y, "FPS", cachedFps, accent);
            y += row;
        }
        if (pingHud) {
            drawHudRow(client, g, x, y, "PING", cachedPing, accent);
            y += row;
        }
        if (speedHud) {
            drawHudRow(client, g, x, y, "SPEED", cachedSpeed, accent);
            y += row;
        }
        if (maceHud) {
            drawHudRow(client, g, x, y, "MACE PVP", cachedMaceStatus, accent);
            y += row;
        }
        if (totemCounter) {
            drawHudRow(client, g, x, y, "TOTEMS", Integer.toString(cachedTotems), accent);
            y += row;
        }
        if (throwableCounter) {
            String counts = "Pearls " + cachedPearls + "  Wind " + cachedWindCharges;
            drawHudRow(client, g, x, y, "THROWABLES", counts, accent);
            y += row;
        }
        if (armorHud) {
            drawHudRow(client, g, x, y, "ARMOR %", cachedArmor, accent);
        }

        if (targetHud && client.crosshairPickEntity != null) {
            Entity target = client.crosshairPickEntity;
            int tx = 8;
            int ty = client.getWindow().getGuiScaledHeight() - 34;
            g.fill(tx, ty, tx + 170, ty + 25, 0xB8101219);
            g.fill(tx, ty, tx + 2, ty + 25, accent);
            g.drawString(client.font, "TARGET", tx + 8, ty + 4, accent, false);
            g.drawString(client.font, target.getName().getString(), tx + 58, ty + 4, 0xFFFFFFFF, false);
            g.drawString(client.font, String.format("%.1f blocks", client.player.distanceTo(target)), tx + 8, ty + 14, 0xFFADB5C5, false);
        }
    }

    private static void drawHudRow(Minecraft client, net.minecraft.client.gui.GuiGraphics g, int x, int y, String label, String value, int accent) {
        int width = client.font.width(label) + client.font.width(value) + 22;
        g.fill(x, y, x + width, y + 15, 0xB8101219);
        g.fill(x, y, x + 2, y + 15, accent);
        g.drawString(client.font, label, x + 7, y + 4, 0xFFE8EBF2, false);
        g.drawString(client.font, value, x + width - client.font.width(value) - 7, y + 4, 0xFF9DA8BC, false);
    }

    private static String itemPath(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static int countItem(Minecraft client, String itemPath) {
        if (client.player == null) return 0;
        int count = 0;
        for (int i = 0; i < Math.min(36, client.player.getInventory().getContainerSize()); i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (itemPath(stack).equals(itemPath)) count += stack.getCount();
        }
        ItemStack offhand = client.player.getOffhandItem();
        if (itemPath(offhand).equals(itemPath)) count += offhand.getCount();
        return count;
    }

    private static String durability(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) return "--";
        int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        int percent = Math.round(100.0f * remaining / Math.max(1, stack.getMaxDamage()));
        return percent + "%";
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
    public static void toggleMaceHud() { maceHud = !maceHud; saveConfig(); }
    public static void toggleTotemCounter() { totemCounter = !totemCounter; saveConfig(); }
    public static void toggleArmorHud() { armorHud = !armorHud; saveConfig(); }
    public static void toggleThrowableCounter() { throwableCounter = !throwableCounter; saveConfig(); }
    public static void toggleHitboxes() { hitboxes = !hitboxes; saveConfig(); }
    public static void toggleAutoMace() { autoMace = !autoMace; saveConfig(); }
    public static void toggleTriggerBot() { triggerBot = !triggerBot; saveConfig(); }
    public static void toggleAutoTotem() { autoTotem = !autoTotem; saveConfig(); }
    public static void toggleVelocity() { velocity = !velocity; saveConfig(); }
    public static void cycleAccentColor() {
        accentColorIndex = (accentColorIndex + 1) % ACCENT_COLORS.length;
        saveConfig();
    }

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
            maceHud = bool(p, "maceHud", maceHud);
            totemCounter = bool(p, "totemCounter", totemCounter);
            armorHud = bool(p, "armorHud", armorHud);
            throwableCounter = bool(p, "throwableCounter", throwableCounter);
            hitboxes = bool(p, "hitboxes", hitboxes);
            autoMace = bool(p, "autoMace", autoMace);
            triggerBot = bool(p, "triggerBot", triggerBot);
            autoTotem = bool(p, "autoTotem", autoTotem);
            velocity = bool(p, "velocity", velocity);
            if (p.containsKey("accentColorIndex")) {
                try { accentColorIndex = Integer.parseInt(p.getProperty("accentColorIndex")); } catch (Exception ignored) {}
            }
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
        p.setProperty("maceHud", Boolean.toString(maceHud));
        p.setProperty("totemCounter", Boolean.toString(totemCounter));
        p.setProperty("armorHud", Boolean.toString(armorHud));
        p.setProperty("throwableCounter", Boolean.toString(throwableCounter));
        p.setProperty("hitboxes", Boolean.toString(hitboxes));
        p.setProperty("autoMace", Boolean.toString(autoMace));
        p.setProperty("triggerBot", Boolean.toString(triggerBot));
        p.setProperty("autoTotem", Boolean.toString(autoTotem));
        p.setProperty("velocity", Boolean.toString(velocity));
        p.setProperty("accentColorIndex", Integer.toString(accentColorIndex));
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream out = Files.newOutputStream(configPath)) { p.store(out, "Poon Client settings"); }
        } catch (IOException ignored) { }
    }
}
