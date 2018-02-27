package info.deskchan.gui_javafx;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.LinkedList;

class TemplateBox extends Dialog<Void> {

	// SO YOU SHOULD NOT CALL setOnCloseRequest from outside PLEASE

	public static LinkedList<TemplateBox> openedDialogs = new LinkedList<>();
	private LinkedList<EventHandler<DialogEvent>> handlers = new LinkedList<>();

	public TemplateBox(String name) {
		setTitle(name);
		initModality(Modality.NONE);
		String style = LocalFont.getDefaultFontCSS();
		getDialogPane().setStyle(
				style
		);
		Stage stage = (Stage) getDialogPane().getScene().getWindow();
		stage.setAlwaysOnTop(checkForceOnTop());
		stage.getIcons().add(new Image(App.ICON_URL.toString()));
		getDialogPane().getButtonTypes().add(new ButtonType(Main.getString("close"), ButtonBar.ButtonData.CANCEL_CLOSE));

		openedDialogs.add(this);

		final TemplateBox thisBox = this;
		onCloseRequestProperty().addListener(observable -> {
			setOnCloseRequest(new EventHandler<DialogEvent>() {
				@Override
				public void handle(DialogEvent event) {
					openedDialogs.remove(thisBox);
					for(EventHandler<DialogEvent> handler : handlers){
						handler.handle(event);
					}
				}
			});
		});

		applyStyle();
	}
	
	public void requestFocus() {
		getDialogPane().getScene().getWindow().requestFocus();
	}

	public static boolean checkForceOnTop(){
		return Main.getProperties().getBoolean("interface.on_top", false);
	}

	public void addOnCloseRequest(EventHandler<DialogEvent> handler){
		handlers.add(handler);
	}

	protected void applyStyle() {
		getDialogPane().getScene().getStylesheets().clear();
		getDialogPane().getScene().getStylesheets().add(App.getStylesheet());
	}

	public static void updateFont(){
		Platform.runLater(() -> {
			String style = LocalFont.getDefaultFontCSS();
			for (TemplateBox dialog : openedDialogs)
				dialog.getDialogPane().setStyle(style);
		});
	}

	private void printHTML(Pane node, int indent){
		String _indent = "";
		for (int i=0; i<indent; i++) _indent += "  ";
		for (Node child : node.getChildren()){
			System.out.print(_indent + "<div" + (child.getId() != null ? (" id=\"" + child.getId() + "\"") : "") + " class=\"" + child.getClass().getSimpleName().toLowerCase() + "\">");
			if (child instanceof Pane && ((Pane) child).getChildren().size() > 0) {
				System.out.println();
				printHTML((Pane) child, indent + 1);
				System.out.print(_indent);
			}
			System.out.println("</div>");
		}
	}

	public void printHTML(){
		printHTML(getDialogPane(), 0);
	}

}
