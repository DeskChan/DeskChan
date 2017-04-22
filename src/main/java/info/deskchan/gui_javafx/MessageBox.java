package info.deskchan.gui_javafx;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;

class MessageBox extends Dialog<Void> {
	
	MessageBox(String name, String text) {
		setTitle(name);
		setContentText(text);
		initModality(Modality.NONE);
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.setAlwaysOnTop(true);
		getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
		getDialogPane().getScene().getWindow().requestFocus();
	}
	
}
