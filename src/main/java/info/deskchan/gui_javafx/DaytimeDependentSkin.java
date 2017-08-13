package info.deskchan.gui_javafx;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


class DaytimeDependentSkin implements Skin {

    private static final int UPDATE_PERIOD = 3600000;   // once per hour

    private Skin skin;
    private Map<Daytime, Path> paths = new HashMap<>();

    DaytimeDependentSkin(Path path) {
        File[] dirs = path.toFile().listFiles();
        if (dirs == null) {
            throw new RuntimeException("Invalid daytime dependent image set. The directory is empty!");
        }
        Arrays.stream(dirs)
              .forEach(file -> {
                  Daytime daytime;
                  try {
                      daytime = Daytime.fromString(FilenameUtils.removeExtension(file.getName()));
                  } catch (Daytime.InvalidDatetimeException e) {
                      return;
                  }
                  paths.put(daytime, file.toPath());
              });

        update();
    }

    private void update() {
        Path path;
        switch (getCurrentDaytime()) {
            case MORNING:
                path = oneOf(Daytime.MORNING, Daytime.EVENING, Daytime.DAY);
                break;
            case EVENING:
                path = oneOf(Daytime.EVENING, Daytime.NIGHT, Daytime.MORNING, Daytime.DAY);
                break;
            case NIGHT:
                path = oneOf(Daytime.NIGHT, Daytime.EVENING, Daytime.MORNING, Daytime.DAY);
                break;
            default:
                path = oneOf(Daytime.DAY);
        }
        skin = Files.isDirectory(path) ? new ImageSetSkin(path) : new SingleImageSkin(path);

        Map<String, Object> m = new HashMap<>();
        m.put("delay", UPDATE_PERIOD);
        Main.getInstance().getPluginProxy().sendMessage("core-utils:notify-after-delay", m, (sender, data) -> update());
    }

    private Path oneOf(Daytime... keys) {
        for (Daytime key : keys) {
            if (paths.containsKey(key)) {
                return paths.get(key);
            }
        }
        return paths.get(Daytime.DAY);
    }

    private static Daytime getCurrentDaytime() {
        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (currentHour < 6) {
            return Daytime.NIGHT;
        }
        if (currentHour < 12) {
            return Daytime.MORNING;
        }
        if (currentHour < 17) {
            return Daytime.DAY;
        }
        if (currentHour < 23) {
            return Daytime.EVENING;
        }
        return Daytime.NIGHT;
    }

    @Override
    public String getName() {
        return skin.getName();
    }

    @Override
    public Image getImage(String name) {
        return skin.getImage(name);
    }

    @Override
    public Point2D getPreferredBalloonPosition(String imageName) {
        return skin.getPreferredBalloonPosition(imageName);
    }

    @Override
    public void overridePreferredBalloonPosition(String imageName, Point2D position) {
        skin.overridePreferredBalloonPosition(imageName, position);
    }

    @Override
    public String toString() {
        return skin.toString() + " [" + getCurrentDaytime().toString().toUpperCase() + "]";
    }

    enum Daytime {

        DAY, NIGHT, MORNING, EVENING;

        static Daytime fromString(String s) {
            switch (s) {
                case "day":
                    return DAY;
                case "night":
                    return NIGHT;
                case "morning":
                    return MORNING;
                case "evening":
                    return EVENING;
                default:
                    throw new InvalidDatetimeException("Invalid daytime string!");
            }
        }

        @Override
        public String toString() {
            switch (this) {
                case DAY:
                    return "day";
                case NIGHT:
                    return "night";
                case MORNING:
                    return "morning";
                case EVENING:
                    return "evening";
                default:
                    throw new InvalidDatetimeException("Unexpected daytime constant!");
            }
        }

        static class InvalidDatetimeException extends RuntimeException {
            InvalidDatetimeException(String s) {
                super(s);
            }
        }
    }


    static class Loader implements SkinLoader {

        @Override
        public boolean matchByPath(Path path) {
            if (!Files.isDirectory(path)) {
                return false;
            }
            File[] children = path.toFile().listFiles();
            return children != null && Arrays.stream(children).anyMatch(file -> FilenameUtils.removeExtension(file.getName()).equals("day"));
        }

        @Override
        public Skin loadByPath(Path path) {
            return new DaytimeDependentSkin(path);
        }

    }

}
