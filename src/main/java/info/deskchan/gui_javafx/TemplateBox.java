package info.deskchan.gui_javafx;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
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


	/*--   HTML Generation   --*/


	private void printHTML(Node node, int indent){
		if (node instanceof Pane)
			printHTML((Pane) node, indent);
		else if (node instanceof Control)
			printHTML((Control) node, indent);
		else if (node instanceof Text){
			String _indent = "";
			for (int i=0; i<indent; i++) _indent += "  ";
			System.out.print(_indent+"<p" + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"text\">");
			System.out.print(((Text) node).getText());
			System.out.println("</p>");
		}
	}

	private void printHTML(Pane node, int indent){
		if (node == null) return;

		String _indent = "";
		for (int i=0; i<indent; i++) _indent += "  ";

		String itemClass = getClassName(node);
		switch (itemClass){
			case "grid-pane": {
				System.out.println(_indent + "<table realclass=\"GridPane\"" + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + ">");
				GridPane grid = (GridPane) node;
				int w=0,h=0;
				for (Node item : grid.getChildren()) {
					Integer index = GridPane.getRowIndex(item);
					if (index != null) h = Math.max(h, index + 1);
					index = GridPane.getColumnIndex(item);
					if (index != null) w = Math.max(w, index + 1);
				}
				for (int i=0; i<h; i++) {
					System.out.println(_indent+" <tr>");
					for (int j=0; j<w; j++) {
						Node cell = getNodeFromGridPane(grid, i, j);
						System.out.println(_indent+"  <td>");
						printHTML(cell, indent + 2);
						System.out.println(_indent+"  </td>");
					}
					System.out.println(_indent+"</tr>");
				}
				System.out.println(_indent + "</table>");
			} break;
			default: {
				String c = node.getClass().getSimpleName();
				System.out.println(_indent + "<" + c + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"" + itemClass + "\">");
				for (Node child : node.getChildren())
					printHTML(child, indent + 1);
				System.out.println(_indent + "</"+c+">");
			} break;
		}
	}

	private void printHTML(Control node, int indent){
		String _indent = "";
		for (int i=0; i<indent; i++) _indent += "  ";

		String className = getClassName(node);
		switch (className){
			case "list-view": {
				System.out.println(_indent + "<ul realclass=\"ListView\"" + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"" + className + "\">");
				ListView view = (ListView) node;
				for (Object item : view.getItems())
					System.out.println(_indent+"  <li class=\"list-cell\">"+item.toString()+"</li>");
				System.out.println(_indent + "</ul>");
			} break;
			case "scroll-pane": {
				System.out.println(_indent + "<ScrollPane" + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"" + className + "\">");
				printHTML(((ScrollPane) node).getContent(), indent+1);
				System.out.println(_indent + "</ScrollPane>");
			} break;
			case "hyperlink": {
				System.out.print(_indent + "<a" + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"hyperlink\" href=\"#\">");
				System.out.print(((Hyperlink) node).getText());
				System.out.println("</a>");
			} break;
			case "button-bar": {
				System.out.println(_indent + "<ButtonBar " + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"" + className + "\">");
				for (Node child : ((ButtonBar) node).getButtons())
					printHTML(child, indent + 1);
				System.out.println(_indent + "</ButtonBar>");
			} break;
			case "check-box": {
				System.out.println(_indent + "<input type=\"checkbox\" " + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"" + className + "\"/>");
			} break;
			case "button": case "font-picker": case "color-picker": {
				System.out.print(_indent + "<input type=\"button\" " + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"" + className + "\" ");
				System.out.println("value=\"" + ((Button) node).getText() + "\" />");
			} break;
			case "combo-box": {
				System.out.println(_indent + "<select " + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"" + className + "\"><option>Variant</option></select>");
			} break;
			case "improved-spinner": {
				System.out.print(_indent + "<input type=\"number\" " + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"" + className + "\" ");
				System.out.println("value=\"" + ((Spinner) node).getValue() + "\" />");
			} break;
			case "text-field": {
				System.out.print(_indent + "<input type=\"text\" " + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"" + className + "\" ");
				System.out.println("value=\"" + ((TextField) node).getText() + "\" />");
			} break;
			default: {
				String classTag = node.getClass().getSimpleName();
				System.out.print(_indent + "<" + classTag + (node.getId() != null ? (" id=\"" + node.getId() + "\"") : "") + " class=\"" + className + "\">");
				if (node instanceof Labeled)
					System.out.print(((Labeled) node).getText());
				System.out.println("</" + classTag + ">");
			} break;
		}
	}

	public void printHTML(){
		printHTML(getDialogPane(), 0);
	}

	private Node getNodeFromGridPane(GridPane gridPane, int row, int col) {
		for (Node node : gridPane.getChildren()) {
			if (GridPane.getColumnIndex(node) == col && GridPane.getRowIndex(node) == row) {
				return node;
			}
		}
		return null;
	}

	private String getClassName(Object item){
		StringBuilder _itemClass = new StringBuilder(item.getClass().getSimpleName());
		for (int i=1; i<_itemClass.length(); i++)
			if (Character.isUpperCase(_itemClass.charAt(i))) { _itemClass.insert(i, '-'); i++; }

		String name = _itemClass.toString().toLowerCase();
		if (name.endsWith("-item")) name = name.substring(0, name.length()-5);
		return name;
	}
}
