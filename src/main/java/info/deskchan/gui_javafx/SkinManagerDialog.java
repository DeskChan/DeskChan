package info.deskchan.gui_javafx;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Window;

class SkinManagerDialog extends Dialog<Void> {
	
	private ListView<Skin> skinsList = new ListView<>();
	
	SkinManagerDialog(Window parent) {
		setTitle(Main.getString("skin_manager"));
		initOwner(parent);
		initModality(Modality.WINDOW_MODAL);
		skinsList.setPrefSize(400, 300);
		for (String skinName : Skin.getSkinList()) {
			skinsList.getItems().add(Skin.load(skinName));
		}
		getDialogPane().setContent(skinsList);
		getDialogPane().getButtonTypes().add(ButtonType.APPLY);
		getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
		getDialogPane().lookupButton(ButtonType.APPLY).addEventFilter(ActionEvent.ACTION, (event) -> {
			event.consume();
			Skin skin = skinsList.getSelectionModel().getSelectedItem();
			Character character = App.getInstance().getCharacter();
			character.setSkin(skin);
			character.setDefaultPosition();
		});
	}
	
}
