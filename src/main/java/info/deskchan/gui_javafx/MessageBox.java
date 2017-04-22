package info.deskchan.gui_javafx;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

class MessageBox extends Dialog<Void> {

    MessageBox(String name,String text) {
        setTitle(name);
        setContentText(text);
        initModality(Modality.NONE);
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().getScene().getWindow().requestFocus();
    }

}
