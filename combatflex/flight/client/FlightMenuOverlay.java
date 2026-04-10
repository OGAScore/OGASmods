package com.OGAS.combatflex.flight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

final class FlightMenuOverlay {
    static final int NO_SELECTION_MODE = -1;

    private static final int MENU_OFFSET_X = 16;
    private static final int OPTION_PADDING_X = 8;
    private static final int OPTION_PADDING_Y = 4;
    private static final int OPTION_HEIGHT = 16;
    private static final int OPTION_SPACING = 4;
    private static final int OPTION_ARC_SWEEP = 12;
    private static final List<MenuOption> MENU_OPTIONS = List.of(
            new MenuOption(1, "screen.cbtflex.flight_menu.fast"),
            new MenuOption(3, "screen.cbtflex.flight_menu.slow"),
            new MenuOption(2, "screen.cbtflex.flight_menu.hover"),
            new MenuOption(0, "screen.cbtflex.flight_menu.stop")
    );

    private static boolean visible = false;
    private static int hoveredMode = NO_SELECTION_MODE;

    private FlightMenuOverlay() {
    }

    static boolean isVisible() {
        return visible;
    }

    static void open(Minecraft mc) {
        visible = true;
        hoveredMode = NO_SELECTION_MODE;
        FlightFeature.NETWORK.sendToServer(new MenuStatePacket(true));
        mc.mouseHandler.releaseMouse();
    }

    static void close(Minecraft mc) {
        if (!visible) {
            hoveredMode = NO_SELECTION_MODE;
            return;
        }

        visible = false;
        hoveredMode = NO_SELECTION_MODE;
        FlightFeature.NETWORK.sendToServer(new MenuStatePacket(false));
        if (mc.screen == null) {
            mc.mouseHandler.grabMouse();
        }
    }

    static int getHoveredMode(Minecraft mc) {
        updateHoveredMode(mc);
        return hoveredMode;
    }

    static void render(GuiGraphics graphics, Minecraft mc) {
        updateHoveredMode(mc);
        if (mc.font == null) {
            return;
        }

        int menuLeft = getMenuLeft(mc);
        int menuTop = getMenuTop(mc);
        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int centerY = mc.getWindow().getGuiScaledHeight() / 2;

        graphics.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, 0xCCF3D67A);

        for (int index = 0; index < MENU_OPTIONS.size(); index++) {
            MenuOption option = MENU_OPTIONS.get(index);
            int optionLeft = getOptionLeft(menuLeft, index);
            int optionTop = getOptionTop(menuTop, index);
            int optionWidth = getOptionWidth(mc, option);
            boolean available = isOptionAvailable(mc.player, option.mode());
            boolean hovered = available && hoveredMode == option.mode();
            int fillColor = !available ? 0xCC232930 : hovered ? 0xE6E8BF56 : 0xCC4A5560;
            int borderColor = !available ? 0xFF4D5762 : hovered ? 0xFFFFF0B8 : 0xFFB7C0CA;
            int textColor = hovered ? 0xFF1D1200 : available ? 0xFFF5F7FA : 0xFF707A84;
            if (index == 0) {
                graphics.fill(centerX + 4, centerY - 1, optionLeft - 4, centerY + 1,
                        available ? 0x99F3D67A : 0x33464E59);
            }
            graphics.fill(optionLeft - 1, optionTop - 1, optionLeft + optionWidth + 1, optionTop + OPTION_HEIGHT + 1, 0x66101215);
            graphics.fill(optionLeft, optionTop, optionLeft + optionWidth, optionTop + OPTION_HEIGHT, fillColor);
            graphics.fill(optionLeft, optionTop, optionLeft + optionWidth, optionTop + 1, borderColor);
            graphics.fill(optionLeft, optionTop + OPTION_HEIGHT - 1, optionLeft + optionWidth, optionTop + OPTION_HEIGHT, borderColor);
            graphics.fill(optionLeft, optionTop, optionLeft + 1, optionTop + OPTION_HEIGHT, borderColor);
            graphics.fill(optionLeft + optionWidth - 1, optionTop, optionLeft + optionWidth, optionTop + OPTION_HEIGHT, borderColor);
            graphics.drawString(mc.font, Component.translatable(option.labelKey()), optionLeft + OPTION_PADDING_X,
                    optionTop + OPTION_PADDING_Y, textColor, false);
        }
    }

    private static void updateHoveredMode(Minecraft mc) {
        hoveredMode = NO_SELECTION_MODE;
        if (mc.mouseHandler == null || mc.getWindow() == null) {
            return;
        }

        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        int screenWidth = mc.getWindow().getScreenWidth();
        int screenHeight = mc.getWindow().getScreenHeight();
        if (guiWidth <= 0 || guiHeight <= 0 || screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        int menuLeft = getMenuLeft(mc);
        int menuTop = getMenuTop(mc);
        double mouseX;
        double mouseY;
        try {
            mouseX = mc.mouseHandler.xpos() * guiWidth / screenWidth;
            mouseY = mc.mouseHandler.ypos() * guiHeight / screenHeight;
        } catch (RuntimeException ex) {
            return;
        }

        for (int index = 0; index < MENU_OPTIONS.size(); index++) {
            MenuOption option = MENU_OPTIONS.get(index);
            if (!isOptionAvailable(mc.player, option.mode())) {
                continue;
            }

            int optionTop = getOptionTop(menuTop, index);
            int optionLeft = getOptionLeft(menuLeft, index);
            int optionWidth = getOptionWidth(mc, option);
            if (isInside(mouseX, mouseY, optionLeft, optionTop, optionWidth, OPTION_HEIGHT)) {
                hoveredMode = option.mode();
                return;
            }
        }
    }

    private static boolean isOptionAvailable(Player player, int mode) {
        return mode == 0 || FlightFeature.canUseMode(player, mode);
    }

    private static int getMenuLeft(Minecraft mc) {
        return (mc.getWindow().getGuiScaledWidth() / 2) + MENU_OFFSET_X;
    }

    private static int getMenuTop(Minecraft mc) {
        return (mc.getWindow().getGuiScaledHeight() / 2) - (getMenuHeight() / 2);
    }

    private static int getMenuHeight() {
        return (MENU_OPTIONS.size() * OPTION_HEIGHT) + ((MENU_OPTIONS.size() - 1) * OPTION_SPACING);
    }

    private static int getOptionTop(int menuTop, int index) {
        return menuTop + (index * (OPTION_HEIGHT + OPTION_SPACING));
    }

    private static int getOptionLeft(int menuLeft, int index) {
        double centerIndex = (MENU_OPTIONS.size() - 1) / 2.0;
        double centerWeight = Math.max(0.0, 1.0 - (Math.abs(index - centerIndex) / Math.max(1.0, centerIndex)));
        int offset = (int) Math.round(centerWeight * OPTION_ARC_SWEEP);
        return menuLeft + offset;
    }

    private static int getOptionWidth(Minecraft mc, MenuOption option) {
        return mc.font.width(Component.translatable(option.labelKey())) + (OPTION_PADDING_X * 2);
    }

    private static boolean isInside(double mouseX, double mouseY, int left, int top, int width, int height) {
        return mouseX >= left && mouseX <= left + width && mouseY >= top && mouseY <= top + height;
    }

    private record MenuOption(int mode, String labelKey) {
    }
}