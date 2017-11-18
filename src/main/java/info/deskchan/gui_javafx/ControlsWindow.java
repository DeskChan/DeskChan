package info.deskchan.gui_javafx;

import javafx.stage.Stage;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class ControlsWindow extends TemplateBox {
    private static List<ControlsWindow> customWindowOpened = new LinkedList<>();
    private ControlsContainer controls;
    private String owner;
    public ControlsWindow(String name, String owner, ControlsContainer controls){
        super(name);
        this.controls=controls;
        this.owner=owner;
        getDialogPane().setContent(this.controls.createControlsPane(getDialogPane().getScene().getWindow()));
    }
    public String getOwnerName(){
        return owner;
    }
    public void setControls(ControlsContainer controls) {
        this.controls = null;
        getDialogPane().setContent(null);
        this.controls = controls;
        getDialogPane().setContent(this.controls.createControlsPane(getDialogPane().getScene().getWindow()));
    }
    public void updateControls(List<Map<String,Object>> data){
        if(data!=null)
            controls.updateControlsPane(data);
    }
    public static void setupCustomWindow(String sender, Map<String, Object> data){
        String name = (String) data.getOrDefault("name", Main.getString("default_messagebox_name"));
        setupCustomWindow(sender, new ControlsContainer(name, (List<Map<String, Object>>) data.get("controls"),
                (String) data.getOrDefault("msgTag", null), (String) data.getOrDefault("onClose", null)));

    }
    public static void setupCustomWindow(String sender, ControlsContainer container){
        String name = container.name;
        for(ControlsWindow window : customWindowOpened){
            if(window.getTitle().equals(name) && window.getOwnerName().equals(sender)){
                window.setControls(container);
                ((Stage) window.getDialogPane().getScene().getWindow()).toFront();
                return;
            }
        }
        ControlsWindow dialog = new ControlsWindow(name, sender, container);
        customWindowOpened.add(dialog);
        dialog.requestFocus();
        dialog.show();
        dialog.getDialogPane().getChildren().get(0).requestFocus();
        dialog.setOnHiding(event -> {
            customWindowOpened.remove(dialog);
        });
        dialog.setOnCloseRequest(event -> {
            customWindowOpened.remove(dialog);
        });
        dialog.getDialogPane().getScene().getWindow().setOnHiding(event -> {
            customWindowOpened.remove(dialog);
        });
        dialog.getDialogPane().getScene().getWindow().setOnCloseRequest(event -> {
            customWindowOpened.remove(dialog);
        });
    }
    public static void updateCustomWindow(String sender, Map<String,Object> data){
        String name = (String) data.getOrDefault("name", Main.getString("default_messagebox_name"));
        List<Map<String,Object>> controls = (List<Map<String,Object>>) data.getOrDefault("controls", null);
        if(controls==null) return;
        for(ControlsWindow window : customWindowOpened){
            if(window.getTitle().equals(name)){
                window.updateControls(controls);
                return;
            }
        }
    }
    public static void updateCustomWindow(String sender, ControlsContainer container){
        for(ControlsWindow window : customWindowOpened){
            if(window.getTitle().equals(container.name)){
                window.setControls(container);
                return;
            }
        }
    }
}
