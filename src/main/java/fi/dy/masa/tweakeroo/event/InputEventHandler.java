package fi.dy.masa.tweakeroo.event;

import java.util.HashSet;
import java.util.Set;
import org.lwjgl.input.Keyboard;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.config.Hotkeys;
import fi.dy.masa.tweakeroo.config.interfaces.IKeybind;
import fi.dy.masa.tweakeroo.util.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;

public class InputEventHandler
{
    private static final InputEventHandler INSTANCE = new InputEventHandler();
    private static final Set<Integer> TWEAK_TOGGLES_USED_KEYS = new HashSet<>();
    private static final Set<Integer> GENERIC_HOTKEYS_USED_KEYS = new HashSet<>();

    public static InputEventHandler getInstance()
    {
        return INSTANCE;
    }

    public static void updateUsedKeys()
    {
        TWEAK_TOGGLES_USED_KEYS.clear();
        GENERIC_HOTKEYS_USED_KEYS.clear();

        for (FeatureToggle toggle : FeatureToggle.values())
        {
            TWEAK_TOGGLES_USED_KEYS.addAll(toggle.getKeybind().getKeys());
        }

        for (Hotkeys hotkey : Hotkeys.values())
        {
            GENERIC_HOTKEYS_USED_KEYS.addAll(hotkey.getKeybind().getKeys());
        }
    }

    public void onKeyInput()
    {
        Minecraft mc = Minecraft.getMinecraft();

        // Not in a GUI
        if (mc.currentScreen == null)
        {
            int eventKey = Keyboard.getEventKey();

            if (TWEAK_TOGGLES_USED_KEYS.contains(eventKey))
            {
                for (FeatureToggle toggle : FeatureToggle.values())
                {
                    IKeybind keybind = toggle.getKeybind();

                    // Note: isPressed() has to get called for key releases too, to reset the state
                    if (keybind.isPressed() && Keyboard.getEventKeyState())
                    {
                        toggle.setBooleanValue(! toggle.getBooleanValue());
                        String str = toggle.getBooleanValue() ? "ON" : "OFF";
                        this.printMessage(mc, toggle.getToggleMessage(), str);
                    }
                }
            }

            if (GENERIC_HOTKEYS_USED_KEYS.contains(eventKey))
            {
                for (Hotkeys hotkey : Hotkeys.values())
                {
                    IKeybind keybind = hotkey.getKeybind();

                    // Note: isPressed() has to get called for key releases too, to reset the state
                    if (keybind.isPressed() && Keyboard.getEventKeyState())
                    {
                        switch (hotkey)
                        {
                            case HOTBAR_SWAP_1:
                                InventoryUtils.swapHotbarWithInventoryRow(mc.player, 0);
                                break;
                            case HOTBAR_SWAP_2:
                                InventoryUtils.swapHotbarWithInventoryRow(mc.player, 1);
                                break;
                            case HOTBAR_SWAP_3:
                                InventoryUtils.swapHotbarWithInventoryRow(mc.player, 2);
                                break;
                            default:
                        }
                    }
                }
            }
        }
    }

    public static void onTick()
    {
        for (FeatureToggle toggle : FeatureToggle.values())
        {
            toggle.getKeybind().tick();
        }

        for (Hotkeys hotkey : Hotkeys.values())
        {
            hotkey.getKeybind().tick();
        }
    }

    private void printMessage(Minecraft mc, String key, Object... args)
    {
        mc.ingameGUI.addChatMessage(ChatType.GAME_INFO, new TextComponentTranslation(key, args));
    }
}