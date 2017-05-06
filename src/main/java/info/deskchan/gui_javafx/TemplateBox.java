package info.deskchan.gui_javafx;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;

class TemplateBox extends Dialog<Void> {

	public TemplateBox(String name) {
		setTitle(name);
		initModality(Modality.NONE);
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.setAlwaysOnTop(true);
		stage.getIcons().add(new Image(App.ICON_URL.toString()));
		getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
	}
	public void requestFocus(){
		getDialogPane().getScene().getWindow().requestFocus();
	}
	
}
