package info.deskchan.gui_javafx;

import javafx.stage.Stage;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class ControlsWindow extends TemplateBox {

    private static List<ControlsWindow> customWindowOpened = new LinkedList<>();
    private ControlsPanel controls;

    public ControlsWindow(ControlsPanel controls){
        super(controls.name);
        this.controls = controls;
        getDialogPane().setContent(this.controls.createControlsPane(this));
    }

    public String getOwnerName(){
        return controls.owner;
    }

    public void setControls(ControlsPanel controls) {
        this.controls = null;
        getDialogPane().setContent(null);
        this.controls = controls;
        getDialogPane().setContent(this.controls.createControlsPane(this));
    }

    public void updateControls(List<Map<String,Object>> data){
        if(data != null) controls.updateControlsPane(data);
    }

    public static void setupCustomWindow(ControlsPanel panel){
        for(ControlsWindow window : customWindowOpened){
            if(window.getTitle().equals(panel.name) && window.getOwnerName().equals(panel.owner)){
                window.setControls(panel);
                ((Stage) window.getDialogPane().getScene().getWindow()).toFront();
                return;
            }
        }
        ControlsWindow dialog = new ControlsWindow(panel);
        customWindowOpened.add(dialog);
        dialog.requestFocus();
        dialog.show();
        dialog.getDialogPane().getChildren().get(0).requestFocus();
        dialog.setOnHiding(event -> {
            customWindowOpened.remove(dialog);
        });
        dialog.addOnCloseRequest(event -> {
            customWindowOpened.remove(dialog);
        });
        dialog.getDialogPane().getScene().getWindow().setOnHiding(event -> {
            customWindowOpened.remove(dialog);
        });
    }

    public static void updateCustomWindow(ControlsPanel panel){
        for(ControlsWindow window : customWindowOpened){
            if(window.getTitle().equals(panel.name) && window.getOwnerName().equals(panel.owner)){
                window.setControls(panel);
                window.requestFocus();
                return;
            }
        }
        setupCustomWindow(panel);
    }

    public static void closeCustomWindow(ControlsPanel panel){
        for(ControlsWindow window : customWindowOpened){
            if(window.getTitle().equals(panel.name) && window.getOwnerName().equals(panel.owner)){
                window.close();
                return;
            }
        }
    }
}
