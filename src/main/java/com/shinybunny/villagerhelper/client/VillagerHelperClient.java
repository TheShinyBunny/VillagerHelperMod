package com.shinybunny.villagerhelper.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class VillagerHelperClient implements ClientModInitializer {
    public static final KeyBinding LIBRARIAN_HELPER_KEY = new KeyBinding("Librarian Helper", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, KeyBinding.MISC_CATEGORY);
    public static boolean awaitingVillagerScreen;
    public static boolean enabled = false;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(LIBRARIAN_HELPER_KEY);
        ClientTickEvents.END_WORLD_TICK.register(world -> {
            if (LIBRARIAN_HELPER_KEY.isPressed() && !LIBRARIAN_HELPER_KEY.wasPressed()) {
                enabled = !enabled;
                MinecraftClient.getInstance().inGameHud.setOverlayMessage(Text.literal((enabled ? "Enabled" : "Disabled") + " librarian helper"), false);
            }
        });
        ClientCommandRegistrationCallback.EVENT.register(LookingForCommand::register);
    }
}
