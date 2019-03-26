package info.deskchan.gui_javafx.skins;

import info.deskchan.core.Path;
import info.deskchan.gui_javafx.Main;
import info.deskchan.gui_javafx.panes.sprite_drawers.Sprite;
import info.deskchan.gui_javafx.panes.sprite_drawers.StackSprite;
import javafx.scene.Node;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class ImageStackSetSkin extends ImageSetSkin {

    ImageStackSetSkin(Path path) {
        super(path);
    }

    @Override
    protected Sprite getFromFiles(List<File> list){
        int i = ThreadLocalRandom.current().nextInt(0, list.size());
        File file = list.get(i);
        try {
            List<Node> sprites = new LinkedList<>();
            for (String image : new Path(file).readAllLines()){
                try {
                    Node node = Sprite.getSpriteFromFile(path.resolve(image)).getSpriteNode();
                    if (node != null)
                        sprites.add(node);
                } catch (Exception e){
                    Main.log(e.getMessage());
                }
            }
            return new StackSprite(sprites);
        } catch (Exception e){
            Main.log(e);
            return null;
        }
    }

    static class Loader implements SkinLoader {

        @Override
        public boolean matchByPath(Path path) {
            return path.isDirectory() &&
                    path.getName().endsWith(".image_stack");
        }

        @Override
        public Skin loadByPath(Path path) {
            return new ImageStackSetSkin(path);
        }

    }

}
