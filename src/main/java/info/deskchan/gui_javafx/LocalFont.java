package info.deskchan.gui_javafx;

import info.deskchan.core_utils.LimitHashMap;
import javafx.scene.text.Font;

public class LocalFont {
    private LocalFont(){ }

    public static Font defaultFont =  getSystemDefaultFont();

    private static Font getSystemDefaultFont(){
        return Font.font(Font.getDefault().getName(), Font.getDefault().getSize() * App.getInterfaceScale());
    }

    public static String getDefaultFontCSS(){
        String css = "-fx-font-size: " + defaultFont.getSize() + "px;";
        css += "-fx-font-family: '" + defaultFont.getFamily() + "';";
        String style = defaultFont.getStyle();
        css += "-fx-font-weight: " + (style.contains("Bold") ? "bold" : "normal") + ";";
        if (style.contains("Italic"))
            css += "-fx-font-style: italic;";
        else if (style.contains("Oblique"))
            css += "-fx-font-style: oblique;";
        return css;
    }

    public static String toString(Font font){
        return font.getName()+", "+font.getSize();
    }

    public static String defaultToString(){
        return toString(defaultFont);
    }

    public static void setDefaultFont(String font){
        if(font == null)
            defaultFont = getSystemDefaultFont();
        else
            defaultFont = fromString(font);
        Main.getProperties().put("interface.font", toString(defaultFont));
        TrayMenu.getContextMenu().setStyle(getDefaultFontCSS());
        TemplateBox.updateStyle();
    }

    private static LimitHashMap<String, Font> hash = new LimitHashMap<>(10);
    public static Font fromString(String name){
        Font font = hash.get(name);
        if (font != null) return font;
        try {
            try {
                font = Font.font(defaultFont.getFamily(), Float.parseFloat(name));
            } catch (Exception e) {
                String[] parts = name.split(",");
                font = Font.font(parts[0].trim(), Float.parseFloat(parts[1].trim()));
            }
            hash.put(name, font);
            return font;
        } catch (Exception e){
            return defaultFont;
        }
    }
}