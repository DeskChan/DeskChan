package info.deskchan.gui_javafx;

import javafx.event.ActionEvent;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Window;

class SkinManagerDialog extends TemplateBox {
	
	private ListView<Skin> skinsList = new ListView<>();
	
	SkinManagerDialog(Window parent) {
		super("skin_manager", Main.getString("skin_manager"));
		initOwner(parent);
		initModality(Modality.WINDOW_MODAL);
		skinsList.setPrefSize(400 * App.getInterfaceScale(), 400 * App.getInterfaceScale());
		setResizable(true);
		//TreeTableView<SkinOrPack> skinsTree = new TreeTableView<>();
		for (String skinName : Skin.getSkinList()) {
			skinsList.getItems().add(Skin.load(skinName));
		}
		getDialogPane().setContent(skinsList);
		getDialogPane().getButtonTypes().add(ButtonType.APPLY);
		getDialogPane().lookupButton(ButtonType.APPLY).addEventFilter(ActionEvent.ACTION, (event) -> {
			event.consume();
			Skin skin = skinsList.getSelectionModel().getSelectedItem();
			info.deskchan.gui_javafx.panes.Character character = App.getInstance().getCharacter();
			character.setSkin(skin);
		});
	}
	
	/* private static class SkinOrPack {
		
		final Skin skin;
		final String title;
		
		SkinOrPack(Skin skin) {
			this.skin = skin;
			String[] parts = skin.toString().split("/");
			title = parts[parts.length - 1];
		}
		
		SkinOrPack(String pack) {
			skin = null;
			String[] parts = pack.split("/");
			title = parts[parts.length - 1];
		}
		
		@Override
		public String toString() {
			return title;
		}
		
	} */
	
}
