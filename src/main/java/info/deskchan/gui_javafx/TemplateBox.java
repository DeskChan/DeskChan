package info.deskchan.gui_javafx;

import com.sun.javafx.css.Declaration;
import com.sun.javafx.css.Rule;
import com.sun.javafx.css.Selector;
import com.sun.javafx.css.Stylesheet;
import com.sun.javafx.css.parser.CSSParser;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

class TemplateBox extends Dialog<Void> {

	public static LinkedList<TemplateBox> openedDialogs = new LinkedList<>();
	private LinkedList<EventHandler<DialogEvent>> handlers = new LinkedList<>();
	private StageStyle stageStyle = null;
	Point2D dragDelta = new Point2D(0, 0);

	public TemplateBox(String id, String title) {
		setDialogPane(new BoxPane());
		setId(id);
		setTitle(title);

		applyStyle();

		initModality(Modality.NONE);

		String style = LocalFont.getDefaultFontCSS();

		getDialogPane().setStyle(
				style
		);

		/*setAlwaysOnTop(checkForceOnTop());
		getIcons().add(new Image(App.ICON_URL.toString()));

		ButtonType closeButton = new ButtonType(Main.getString("close"), ButtonBar.ButtonData.CANCEL_CLOSE);
		getDialogPane().getButtonTypes().add(closeButton);
		getDialogPane().lookupButton(closeButton).addEventFilter(ActionEvent.ACTION, (event) -> {
			event.consume();
			Platform.runLater(() -> close());
		});*/

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
					System.out.println("closed");
					openedDialogs.remove(thisBox);
					for(EventHandler<DialogEvent> handler : handlers){
						handler.handle(event);
					}
				}
			});
		});

		getDialogPane().setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override public void handle(MouseEvent mouseEvent) {
				dragDelta = new Point2D(stage.getX() - mouseEvent.getScreenX(), stage.getY() - mouseEvent.getScreenY());
			}
		});
		getDialogPane().setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override public void handle(MouseEvent mouseEvent) {
				stage.setX(mouseEvent.getScreenX() + dragDelta.getX());
				stage.setY(mouseEvent.getScreenY() + dragDelta.getY());
			}
		});
	}

	public static boolean checkForceOnTop(){
		return Main.getProperties().getBoolean("interface.on_top", false);
	}

	public void addOnCloseRequest(EventHandler<DialogEvent> handler){
		handlers.add(handler);
	}

	protected void applyStyle() {
		try {
			getDialogPane().getScene().getStylesheets().clear();
			String stylesheet = App.getStylesheet();
			setStyling(stylesheet);
			getDialogPane().getScene().getStylesheets().add(stylesheet);
		} catch (Exception e){
			Main.log(new Exception("Error parsing style file: " + e.getMessage(), e));
		}
	}

	protected void setStyling(String style){
		CSSParser parser = new CSSParser();
		try {
			String set = null;
			Selector selected = null;

			Stylesheet css = parser.parse(new URL(style));

			Selector w = Selector.createSelector("Window"),
					 n = Selector.createSelector("." + getDialogPane().getId());

			System.out.println(getDialogPane() + " " + getDialogPane().getId() + " " + n);
			boolean cw, cn;

			for (Rule rule : css.getRules()) {
				cw = cn = false;
				if ((cw = rule.getSelectors().contains(w)) || (cn = rule.getSelectors().contains(n))){
					for (Declaration declaration : rule.getDeclarations()) {
						if (!declaration.getProperty().equals("pfx-stage-style")) continue;
						try {
							String value = declaration.getParsedValue().getValue().toString().substring(6);
							if (cn || (cw && selected != n))
								set = value;
						} catch (Exception e){ }
					}
				}
			}
			if (set == null){
				if (stageStyle != null) return;
				stageStyle = StageStyle.DECORATED;
			} else {
				StageStyle newStageStyle = StageStyle.valueOf(set.toUpperCase());

				if (newStageStyle == stageStyle) return;
				stageStyle = newStageStyle;
			}
		} catch (IOException ex) {
			Main.log(ex);
			stageStyle = StageStyle.DECORATED;
		}
		try {
			if (stageStyle == StageStyle.TRANSPARENT)
				getDialogPane().getScene().setFill(Color.TRANSPARENT);
			initStyle(stageStyle);
		} catch (Exception e){
			App.showNotification(Main.getString("error"), "Please, restart the program to apply changes");
			Main.log(e);
		}
	}

	//public void setContentText(String text){
	//	getDialogPane().setContentText(text);
	//}

	public static void updateStyle(){
		Platform.runLater(() -> {
			String style = LocalFont.getDefaultFontCSS();
			for (TemplateBox dialog : openedDialogs) {
				dialog.getDialogPane().setStyle(style);
				dialog.applyStyle();
				dialog.hide();
				dialog.show();
			}
		});
	}

	//public DialogPane getDialogPane(){  return pane; }

	public void setId(String id){
		getDialogPane().getStyleClass().add(id);
		getDialogPane().setId(id);
		System.out.println(getDialogPane() + " " + getDialogPane().getId());
	}

	public void requestFocus(){ getDialogPane().requestFocus(); }

	class BoxPane extends DialogPane {
		Region background = new Region();
		boolean onFront = false;
		BoxPane(){
			background.setId("background");
			getChildren().add(background);
		}
		@Override
		protected void layoutChildren(){
			background.toBack();
			background.resizeRelocate(0, 0, getWidth(), getHeight());
			if (!onFront)
			for (Object node : getChildren().toArray())
				if (node instanceof ButtonBar) {
					((ButtonBar) node).toFront();
					onFront = true;
					break;
				}
			super.layoutChildren();
		}
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
