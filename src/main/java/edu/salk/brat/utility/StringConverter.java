package edu.salk.brat.utility;

import java.awt.*;

public class StringConverter {
    public static Rectangle toRectangle(String str) {
        String[] parts = str.split(",");
        if (parts.length != 4){
            return null;
        }
        try {
            int[] ints = new int[parts.length];
            for (int i = 0; i < 4; ++i) {
                ints[i] = Integer.parseInt(parts[i].trim());
            }
            return new Rectangle(ints[0], ints[1], ints[2], ints[3]);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }
}
