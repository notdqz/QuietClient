package com.quiet;

import com.mojang.blaze3d.platform.InputConstants;
import com.quiet.gui.Hud;
import com.quiet.gui.MenuScreen;
import com.quiet.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class QuietClient implements ClientModInitializer {
	private static KeyMapping openMenu;

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		Config.load();   // after init(): applies saved keys and settings to the modules

		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Quiet.id("hud"), (graphics, delta) -> Hud.render(graphics));

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
