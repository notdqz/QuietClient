package com.quiet;

import com.mojang.blaze3d.platform.InputConstants;
import com.quiet.config.QuietConfig;
import com.quiet.gui.MenuScreen;
import com.quiet.hud.HudManager;
import com.quiet.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import com.quiet.render.Overlay;
//import net.minecraft.client.gui.screens.Overlay;

import org.lwjgl.glfw.GLFW;

public class QuietClient implements ClientModInitializer {
	private static KeyMapping openMenu;

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		HudManager.init();
		QuietConfig.load();   // must come after both init() calls

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Quiet.id("hud"),
		(graphics, delta) -> { Overlay.render(graphics); HudManager.render(graphics); });


		KeyMapping.Category category = KeyMapping.Category.register(Quiet.id("general"));
		openMenu = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.quiet.open_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (openMenu.consumeClick()) {
				if (mc.screen == null) mc.setScreen(new MenuScreen());
			}
			ModuleManager.tick(mc);
		});
	}
}