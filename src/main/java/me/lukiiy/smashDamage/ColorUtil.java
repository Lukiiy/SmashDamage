package me.lukiiy.smashDamage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class ColorUtil { // confusing & hardcoded(?)
    public static Component formatted(double value) {
        TextColor white = TextColor.color(255, 255, 255);
        TextColor yellow = TextColor.color(255, 198, 0);
        TextColor orange = TextColor.color(255, 117, 29);
        TextColor red = TextColor.color(255, 24, 17);
        TextColor red2 = TextColor.color(152, 11, 25);

        TextColor color;
        if (value < 30) color = white;
        else if (value < 60) color = interpolate(white, yellow, (float) (value - 30) / (60 - 30));
        else if (value < 90) color = interpolate(yellow, orange, (float) (value - 60) / (90 - 60));
        else if (value < 130) color = interpolate(orange, red, (float) (value - 90) / (130 - 90));
        else if (value < 190) color = interpolate(red, red2,(float) (value - 130) / (190 - 130));
        else color = red2;

        return Component.text(String.format("%.1f", value) + "%").color(color);
    }

    private static TextColor interpolate(TextColor start, TextColor end, float ratio) {
        int red = (int) (start.red() * (1 - ratio) + end.red() * ratio);
        int green = (int) (start.green() * (1 - ratio) + end.green() * ratio);
        int blue = (int) (start.blue() * (1 - ratio) + end.blue() * ratio);
        return TextColor.color(red, green, blue);
    }
}
