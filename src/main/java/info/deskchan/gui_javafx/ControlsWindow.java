package info.deskchan.gui_javafx;

import java.util.LinkedList;
import java.util.List;

class ControlsWindow extends TemplateBox {

    private static List<ControlsWindow> customWindowOpened = new LinkedList<>();
    private ControlsPanel controls;

    public ControlsWindow(ControlsPanel controls){
        super(controls.name);
        this.controls = controls;
        getDialogPane().setContent(controls.createControlsPane(this));
        setResizable(true);
        setId(controls.getFullName());

        customWindowOpened.add(this);
        requestFocus();
        show();
        getDialogPane().getChildren().get(0).requestFocus();
        setOnHiding(event -> {
            customWindowOpened.remove(this);
        });
        addOnCloseRequest(event -> {
            customWindowOpened.remove(this);
        });
        getDialogPane().getScene().getWindow().setOnHiding(event -> {
            customWindowOpened.remove(this);
        });

    }

    public static void closeCustomWindow(ControlsPanel panel){
        for(ControlsWindow window : customWindowOpened){
            if(window.controls.getFullName().equals(panel.getFullName())){
                window.close();
                return;
            }
        }
    }
}
