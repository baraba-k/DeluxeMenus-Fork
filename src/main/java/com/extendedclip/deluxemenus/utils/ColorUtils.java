package com.extendedclip.deluxemenus.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class ColorUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})|#([A-Fa-f0-9]{6})");

    public static MutableComponent format(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        // Convert hex &#RRGGBB or #RRGGBB to §x§r§r§g§g§b§b
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            StringBuilder hexReplacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                hexReplacement.append("§").append(Character.toLowerCase(c));
            }
            matcher.appendReplacement(sb, hexReplacement.toString());
        }
        matcher.appendTail(sb);
        String formatted = sb.toString().replace('&', '§');

        MutableComponent root = Component.empty();
        Style currentStyle = Style.EMPTY;
        StringBuilder currentText = new StringBuilder();

        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (c == '§' && i + 1 < formatted.length()) {
                char code = Character.toLowerCase(formatted.charAt(++i));
                if (code == 'x' && i + 12 < formatted.length()) {
                    // §x§r§r§g§g§b§b sequence
                    StringBuilder hexCode = new StringBuilder("#");
                    boolean validHex = true;
                    for (int j = 0; j < 6; j++) {
                        if (formatted.charAt(i + 1 + j * 2) == '§') {
                            hexCode.append(formatted.charAt(i + 2 + j * 2));
                        } else {
                            validHex = false;
                            break;
                        }
                    }
                    if (validHex) {
                        if (!currentText.isEmpty()) {
                            root.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                            currentText.setLength(0);
                        }
                        currentStyle = Style.EMPTY.withColor(TextColor.parseColor(hexCode.toString()).getOrThrow());
                        i += 12;
                        continue;
                    }
                }

                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    if (!currentText.isEmpty()) {
                        root.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                        currentText.setLength(0);
                    }
                    if (formatting == ChatFormatting.RESET) {
                        currentStyle = Style.EMPTY;
                    } else if (formatting.isColor()) {
                        currentStyle = Style.EMPTY.withColor(formatting);
                    } else {
                        currentStyle = currentStyle.applyFormat(formatting);
                    }
                    continue;
                }
            }
            currentText.append(c);
        }

        if (!currentText.isEmpty()) {
            root.append(Component.literal(currentText.toString()).setStyle(currentStyle));
        }

        return root;
    }

    public static String stripColor(String input) {
        if (input == null) return "";
        return input.replaceAll("(?i)[&§][0-9a-fk-orx]", "").replaceAll("#[A-Fa-f0-9]{6}", "");
    }
}
