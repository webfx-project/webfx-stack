package dev.webfx.stack.ui.controls.dialog;

import dev.webfx.platform.util.collection.Collections;
import dev.webfx.platform.util.tuples.Pair;
import dev.webfx.stack.i18n.controls.I18nControls;
import dev.webfx.stack.ui.dialog.DialogCallback;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Bruno Salmon
 */
public final class GridPaneBuilder implements DialogBuilder {

    private final GridPane gridPane = new GridPane();
    private int rowCount;
    private int colCount;
    private final List<Pair<Property, Object>> watchedUserProperties = new ArrayList<>();
    private final Property<Boolean> noChangesProperty = new SimpleObjectProperty<>(true);
    private final ChangeListener watchedUserPropertyListener = (observable, oldValue, newValue) ->
            noChangesProperty.setValue(Collections.noneMatch(watchedUserProperties, pair -> !Objects.equals(pair.get1().getValue(), pair.get2())));
    private DialogCallback dialogCallback;

    public GridPaneBuilder() {
        gridPane.setHgap(10);
        gridPane.setVgap(10);
    }

    public void setDialogCallback(DialogCallback dialogCallback) {
        this.dialogCallback = dialogCallback;
    }

    @Override
    public DialogCallback getDialogCallback() {
        return dialogCallback;
    }

    public GridPaneBuilder addLabelTextInputRow(Object i18nKey, TextInputControl textInput) {
        return addNewRow(createLabel(i18nKey), setUpTextInput(textInput));
    }

    public GridPaneBuilder addCheckBoxTextInputRow(Object i18nKey, CheckBox checkBox, TextInputControl textInput) {
        textInput.visibleProperty().bind(checkBox.selectedProperty());
        return addNewRow(setUpLabeled(checkBox, i18nKey), setUpTextInput(textInput));
    }

    public GridPaneBuilder addNewRow(Node... children) {
        colCount = Math.max(colCount, children.length);
        gridPane.addRow(rowCount++, children);
        if (colCount >= 2 && gridPane.getColumnConstraints().isEmpty()) {
            ColumnConstraints cc1 = new ColumnConstraints();
            cc1.setHgrow(Priority.NEVER);
            ColumnConstraints cc2 = new ColumnConstraints();
            cc2.setHgrow(Priority.ALWAYS);
            gridPane.getColumnConstraints().setAll(cc1, cc2);
        }
        return this;
    }

    public GridPaneBuilder addTextRow(String text) {
        return addNodeFillingRow(setUpLabeled(new Label(), text));
    }

    public GridPaneBuilder addNodeFillingRow(Node node) {
        return addNodeFillingRow(0, node);
    }

    public GridPaneBuilder addNodeFillingRow(int topMargin, Node node) {
        return addNodeFillingRow(topMargin, node, Math.max(colCount, 1));
    }

    public GridPaneBuilder addNodeFillingRow(Node node, int colSpan) {
        return addNodeFillingRow(0, node, colSpan);
    }

    public GridPaneBuilder addNodeFillingRow(int topMargin, Node node, int colSpan) {
        if (topMargin != 0)
            GridPane.setMargin(node, new Insets(topMargin, 0, 0, 0));
        gridPane.add(node, 0, rowCount++, colSpan, 1);
        GridPane.setHgrow(node, Priority.ALWAYS);
        return this;
    }

    public GridPaneBuilder addButtons(String button1Key, Consumer<DialogCallback> action1, String button2Key, Consumer<DialogCallback> action2) {
        return addNodeFillingRow(20, createButtonBar(button1Key, action1, button2Key, action2));
    }

    public GridPaneBuilder addButtons(String button1Key, Button button1, String button2Key, Button button2) {
        return addNodeFillingRow(20, createButtonBar(button1Key, button1, button2Key, button2));
    }

    private Pane createButtonBar(String button1Key, Consumer<DialogCallback> action1, String button2Key, Consumer<DialogCallback> action2) {
        return createButtonBar(button1Key, newButton(button1Key, action1), button2Key, newButton(button1Key, action2));
    }

    private Pane createButtonBar(String button1Key, Button button1, String button2Key, Button button2) {
        if ("Ok".equals(button1Key) && !watchedUserProperties.isEmpty())
            button1.disableProperty().bind(noChangesProperty);
        button1.setText(button1Key);
        button2.setText(button2Key);
        return createButtonBar(button2, button1);
    }

    private Pane createButtonBar(Button... buttons) {
        HBox buttonBar = new HBox(20, buttons);
        buttonBar.setAlignment(Pos.CENTER);
        return buttonBar;
    }

    @Override
    public GridPane build() {
        return gridPane;
    }

    //// private methods

    private Label createLabel(Object i18nKey) {
        return setUpLabeled(new Label(), i18nKey);
    }

    private <T extends Labeled> T setUpLabeled(T labeled, Object i18nKey) {
        I18nControls.bindI18nProperties(labeled, i18nKey);
        //labeled.setText(i18nKey.toString());
        //label.textFillProperty().bind(Theme.dialogTextFillProperty());
        GridPane.setHalignment(labeled, HPos.RIGHT);
        if (labeled instanceof CheckBox)
            watchUserProperty(((CheckBox) labeled).selectedProperty());
        return labeled;
    }

    private TextInputControl setUpTextInput(TextInputControl textInput) {
        textInput.setPrefWidth(150d);
        watchUserProperty(textInput.textProperty());
        return textInput;
    }

    private void watchUserProperty(Property userProperty) {
        watchedUserProperties.add(new Pair<>(userProperty, userProperty.getValue()));
        userProperty.addListener(watchedUserPropertyListener);
    }
}
