package com.poon.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class PoonClient implements ClientModInitializer {
    public static final String BRAND = "Poon Client";
    public static boolean sprint = false;
    public static boolean entityGlow = false;
    public static boolean coordsHud = true;
    private static KeyMapping openMenu;

    @Override
    public void onInitializeClient() {
        openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.poonclient.open_menu", GLFW.GLFW_KEY_RIGHT_SHIFT, KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick()) {
                if (client.screen == null) client.setScreen(new PoonClickGui());
                else if (client.screen instanceof PoonClickGui) client.setScreen(null);
            }
            if (client.player == null || client.level == null) return;
            if (sprint && client.options.keyUp.isDown()) client.player.setSprinting(true);
            for (var entity : client.level.entitiesForRendering()) {
                if (entity == client.player) continue;
                entity.setGlowingTag(entityGlow);
            }
        });
    }

    public static void toggleSprint() { sprint = !sprint; }
    public static void toggleGlow() { entityGlow = !entityGlow; }
    public static void toggleCoords() { coordsHud = !coordsHud; }
}
