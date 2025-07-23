package dev.webfx.stack.orm.entity.controls.entity.selector;

import dev.webfx.extras.panes.MonoPane;
import dev.webfx.extras.panes.ScalePane;
import dev.webfx.extras.styles.materialdesign.textfield.MaterialTextFieldPane;
import dev.webfx.extras.util.layout.Layouts;
import dev.webfx.extras.util.scene.SceneUtil;
import dev.webfx.kit.util.properties.FXProperties;
import dev.webfx.platform.scheduler.Scheduled;
import dev.webfx.platform.scheduler.Scheduler;
import dev.webfx.platform.uischeduler.AnimationFramePass;
import dev.webfx.platform.uischeduler.UiScheduler;
import dev.webfx.platform.util.function.Callable;
import dev.webfx.extras.controlfactory.MaterialFactoryMixin;
import dev.webfx.extras.controlfactory.button.ButtonFactory;
import dev.webfx.extras.controlfactory.button.ButtonFactoryMixin;
import dev.webfx.extras.util.dialog.DialogCallback;
import dev.webfx.extras.util.dialog.DialogUtil;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import static dev.webfx.extras.util.layout.Layouts.setMaxPrefSize;
import static dev.webfx.extras.util.layout.Layouts.setMaxPrefSizeToInfinite;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import static javafx.scene.layout.Region.USE_PREF_SIZE;

/**
 * @author Bruno Salmon
 */
public abstract class ButtonSelector<T> {

    public enum ShowMode {
        MODAL_DIALOG,
        DROP_DOWN,
        DROP_UP,
        AUTO
    }

    private final ButtonSelectorParameters parameters;
    private boolean autoOpenOnMouseEntered;
    private boolean searchEnabled = true;
    private ObservableValue<?> loadedContentProperty;
    private BorderPane dialogPane;
    protected final ScalePane searchPane = new ScalePane();
    private TextField searchTextField;
    private DialogCallback dialogCallback;
    protected Button button;
    private Hyperlink cancelLink;
    private ShowMode decidedShowMode;

    private final Property<ShowMode> showModeProperty = new SimpleObjectProperty<>(ShowMode.AUTO);
    // Updating the content of the button when the selected item changes
    private final Property<T> selectedItemProperty = FXProperties.newObjectProperty(this::onSelectedItemChanged);
    private final DoubleProperty dialogHeightProperty = new SimpleDoubleProperty();

    public ButtonSelector(ButtonFactoryMixin buttonFactory, Callable<Pane> parentGetter) {
        this(buttonFactory, parentGetter, null);
    }

    public ButtonSelector(ButtonFactoryMixin buttonFactory, Pane parent) {
        this(buttonFactory, null, parent);
    }

    protected ButtonSelector(ButtonFactoryMixin buttonFactory, Callable<Pane> parentGetter, Pane parent) {
        this(new ButtonSelectorParameters(parentGetter, parent, buttonFactory));
    }

    public ButtonSelector(ButtonSelectorParameters parameters) {
        parameters.checkValid();
        this.parameters = parameters;
    }

    public boolean isAutoOpenOnMouseEntered() {
        return autoOpenOnMouseEntered;
    }

    public ButtonSelector<T> setAutoOpenOnMouseEntered(boolean autoOpenOnMouseEntered) {
        this.autoOpenOnMouseEntered = autoOpenOnMouseEntered;
        return this;
    }

    public boolean isSearchEnabled() {
        return searchEnabled;
    }

    public ButtonSelector<T> setSearchEnabled(boolean searchEnabled) {
        this.searchEnabled = searchEnabled;
        return this;
    }

    protected TextField getSearchTextField() {
        return isSearchEnabled() ? getOrCreateSearchTextField() : null;
    }

    private TextField getOrCreateSearchTextField() {
        if (searchTextField == null) {
            searchTextField = parameters.getButtonFactory().newTextField("GenericSearch");
            HBox.setHgrow(searchTextField, Priority.ALWAYS);
            searchTextField.getProperties().put("webfx-keepHtmlPlaceholder", true);
        }
        return searchTextField;
    }

    protected void setLoadedContentProperty(ObservableValue loadedContentProperty) {
        this.loadedContentProperty = loadedContentProperty;
    }

    private boolean isContentLoaded() {
        return loadedContentProperty == null || loadedContentProperty.getValue() != null;
    }

    public Property<T> selectedItemProperty() {
        return selectedItemProperty;
    }

    public T getSelectedItem() {
        return selectedItemProperty.getValue();
    }

    public ButtonSelector<T> setSelectedItem(T item) {
        selectedItemProperty.setValue(item);
        return this;
    }

    protected void onSelectedItemChanged() {
        updateButtonContentFromSelectedItem();
    }

    protected ReadOnlyDoubleProperty dialogHeightProperty() {
        return dialogHeightProperty;
    }

    public ButtonSelector<T> setReadOnly(boolean readOnly) {
        getButton().setDisable(readOnly);
        return this;
    }

    public boolean isReadOnly() {
        return getButton().isDisabled();
    }

    public Button getButton() {
        if (button == null)
            setButton(ButtonFactory.newDropDownButton());
        return button;
    }

    public ButtonSelector<T> setButton(Button button) {
        this.button = button;
        button.setOnAction(e -> onButtonClicked());
        button.setCursor(Cursor.HAND);
        button.setOnMouseEntered(e -> onMouseEntered());
        button.setOnMouseExited( e -> onMouseExited());
        updateButtonContentFromSelectedItem();
        return this;
    }

    public MaterialTextFieldPane toMaterialButton(Object i18nKey) {
        // Assuming the passed buttonFactory is actually an instance of MaterialFactoryMixin when we call this method
        return ((MaterialFactoryMixin) parameters.getButtonFactory()).setMaterialLabelAndPlaceholder(newMaterialButton(), i18nKey);
    }

    public MaterialTextFieldPane toMaterialButton(ObservableValue<String> labelProperty, ObservableValue<String> placeholderProperty) {
        MaterialTextFieldPane materialButton = newMaterialButton();
        if (labelProperty != null)
            materialButton.labelTextProperty().bind(labelProperty);
        if (placeholderProperty != null)
            materialButton.placeholderTextProperty().bind(placeholderProperty);
        return materialButton;
    }

    private MaterialTextFieldPane newMaterialButton() {
        return new MaterialTextFieldPane(Layouts.setMaxWidthToInfinite(getButton()), materialInputProperty());
    }

    protected ObservableValue materialInputProperty() {
        return selectedItemProperty();
    }

    public ButtonSelector<T> updateButtonContentFromSelectedItem() {
        UiScheduler.runInUiThread(() -> getButton().setGraphic(getOrCreateButtonContentFromSelectedItem()));
        return this;
    }

    protected abstract Node getOrCreateButtonContentFromSelectedItem();


    private boolean isDialogOpen() {
        return dialogCallback != null && !dialogCallback.isDialogClosed();
    }

    private boolean userJustPressedButtonInOrderToCloseDialog;

    private boolean openDueToMouseEntered;
    private void onMouseEntered() {
        if (isAutoOpenOnMouseEntered() && !isReadOnly()) {
            openDueToMouseEntered = true;
            showDialog();
        }
    }

    private void onMouseExited() {
        scheduleMouseExistedDialogClose();
    }

    private void scheduleMouseExistedDialogClose() {
        if (openDueToMouseEntered) {
            openDueToMouseEntered = false;
            Scheduler.scheduleDelay(100, ()-> {
                if (!openDueToMouseEntered)
                    closeDialog();
            });
        }
    }

    private void onButtonClicked() {
        if (userJustPressedButtonInOrderToCloseDialog)
            userJustPressedButtonInOrderToCloseDialog = false;
        else if (!isReadOnly())
            toggleDialog();
    }

    private void toggleDialog() {
        if (isDialogOpen())
            closeDialog();
        else
            showDialog();
    }

    public ButtonSelector<T> showDialog() {
        setUpDialog(true);
        return this;
    }

    protected void setUpDialog(boolean show) {
        // Instantiating the dialog pane if not yet done
        if (dialogPane == null) {
            Node dialogContent = getOrCreateDialogContent();
            if (dialogContent == null)
                return;
            dialogPane = new BorderPane(dialogContent);
            dialogPane.getStyleClass().add("webfx-button-selector-dialog");
            dialogPane.setOnMouseExited(e -> scheduleMouseExistedDialogClose());
            dialogPane.setOnMouseEntered(e-> onMouseEntered());
        }
        if (!isContentLoaded()) {
            setInitialHiddenDialogHeightPropertyForContentLoading();
            startLoading();
        }
        if (show && !isDialogOpen())
            FXProperties.onPropertySet(loadedContentProperty, x -> {
                updateDecidedShowMode();
                show();
            }, true);
    }

    private static final double INITIAL_HIDDEN_DIALOG_HEIGHT = 400;

    private void setInitialHiddenDialogHeightPropertyForContentLoading() {
        dialogPane.setVisible(false);
        dialogHeightProperty.unbind();
        dialogHeightProperty.setValue(INITIAL_HIDDEN_DIALOG_HEIGHT);
    }

    protected abstract void startLoading();

    protected abstract Region getOrCreateDialogContent();

    protected StringProperty searchTextProperty() {
        return getOrCreateSearchTextField().textProperty();
    }


    public Property<ShowMode> showModeProperty() {
        return showModeProperty;
    }

    public ShowMode getShowMode() {
        return showModeProperty().getValue();
    }

    public ButtonSelector<T> setShowMode(ShowMode showModeProperty) {
        this.showModeProperty().setValue(showModeProperty);
        return this;
    }

    private double dialogHighestHeight;

    private ShowMode updateDecidedShowMode() {
        ShowMode showMode = getShowMode();
        if (showMode != ShowMode.AUTO)
            decidedShowMode = showMode;
        else if (dialogPane.getScene() == null)
            decidedShowMode = ShowMode.DROP_DOWN;
        else if (!SceneUtil.isVirtualKeyboardShowing(dialogPane.getScene())) { // we don't change the decided show mode while the virtual keyboard is showing
            double spaceAboveButton = computeMaxAvailableHeightAboveButton();
            double spaceBelowButton = computeMaxAvailableHeightBelowButton();
            double dialogHeight = dialogPane.prefHeight(-1);
            // Making the decision from the highest dialog height (we don't change any decision when it shrinks, only when it grows)
            dialogHighestHeight = Math.max(dialogHighestHeight, dialogHeight);
            decidedShowMode = dialogHighestHeight < spaceBelowButton ? ShowMode.DROP_DOWN
                    : dialogHighestHeight < spaceAboveButton ? ShowMode.DROP_UP
                    : isSearchEnabled() ? (spaceBelowButton > spaceAboveButton ? ShowMode.DROP_DOWN : ShowMode.DROP_UP)
                    : ShowMode.MODAL_DIALOG;
        }
        return decidedShowMode;
    }

    public ShowMode getDecidedShowMode() {
        return decidedShowMode;
    }

    private double computeMaxAvailableHeightForDropDialog() {
        double spaceAboveButton = computeMaxAvailableHeightAboveButton();
        double spaceBelowButton = computeMaxAvailableHeightBelowButton();
        return Math.max(spaceAboveButton, spaceBelowButton);
    }

    private double computeMaxAvailableHeightAboveButton() {
        return computeMaxAvailableHeight(true);
    }

    private double computeMaxAvailableHeightBelowButton() {
        return computeMaxAvailableHeight(false);
    }

    private double computeMaxAvailableHeight(boolean above) {
        Point2D buttonPositionInScene = button.localToScene(0, above ? 0 : button.getHeight());
        Pane dropParent = parameters.getDropParent();
        Point2D buttonPositionInDropParent = dropParent.sceneToLocal(buttonPositionInScene);
        if (above) { // returning the distance between the button top and the scene top in the dropParent coordinates
            Point2D sceneLeftTopInDropParent = dropParent.sceneToLocal(0, 0);
            return buttonPositionInDropParent.getY() - sceneLeftTopInDropParent.getY();
        }
        // below => returning the distance between the button bottom and the scene height in the dropParent coordinates
        Point2D sceneLeftBottomInDropParent = dropParent.sceneToLocal(0, button.getScene().getHeight());
        return sceneLeftBottomInDropParent.getY() - buttonPositionInDropParent.getY();
    }


    private void show() {
        // Doing nothing if the dialog is already showing (otherwise same node inserted twice in scene graph => error)
        if (dialogPane != null && dialogPane.getParent() != null) // May happen when quickly moving the mouse over several
            return; // entity buttons in auto-open mode
        Region dialogContent = getOrCreateDialogContent();
        dialogPane.setBackground(Background.fill(Color.WHITE)); // TODO: move this to CSS (as well as borders below)
        TextField searchTextField = getSearchTextField(); // may return null in case search is not enabled
        Scene scene = button.getScene();
        switch (decidedShowMode) {
            case MODAL_DIALOG:
                // This is to help automatically close dialogs when clicking outside
                Pane dialogParent = parameters.getDialogParent();
                dialogParent.setOnMouseClicked(e -> dialogParent.requestFocus());
                // Removing the (square) border as it will be displayed in a modal gold layout which already has a (rounded) border
                dialogPane.setBorder(null);
                setMaxPrefSizeToInfinite(dialogContent);
                if (cancelLink == null) {
                    cancelLink = parameters.getButtonFactory().newHyperlink("Cancel", e -> onDialogCancel());
                    cancelLink.setContentDisplay(ContentDisplay.TEXT_ONLY); // To hide the cancel icon in the back-office
                }
                if (searchTextField == null)
                    dialogPane.setTop(null);
                else {
                    StackPane stackPane = new StackPane(searchTextField, cancelLink);
                    StackPane.setAlignment(cancelLink, Pos.CENTER_RIGHT);
                    StackPane.setMargin(cancelLink, new Insets(0, 5, 0, 0));
                    stackPane.setMaxHeight(USE_PREF_SIZE); // Necessary to make the scale work
                    searchPane.setContent(stackPane);
                    dialogPane.setTop(searchPane);
                }
                dialogCallback = DialogUtil.showModalNodeInGoldLayout(dialogPane, dialogParent, 0.95, 0.95);
                dialogHeightProperty.bind(dialogPane.heightProperty());
                dialogPane.setVisible(true);
                break;

            case DROP_DOWN:
            case DROP_UP:
                Layouts.removePadding(dialogPane).setBorder(Border.stroke(Color.LIGHTGRAY));
                setMaxPrefSize(dialogContent, USE_COMPUTED_SIZE);
                double maxHeight = computeMaxAvailableHeightForDropDialog();
                if (searchTextField != null)
                    maxHeight = Math.min(maxHeight, INITIAL_HIDDEN_DIALOG_HEIGHT);
                dialogContent.setMaxHeight(maxHeight);
                HBox searchBox;
                if (searchTextField != null) {
                    SVGPath switchIcon = new SVGPath();
                    switchIcon.setContent("M 2.2857143,10.285714 H 0 V 16 H 5.7142857 V 13.714286 H 2.2857143 Z M 0,5.7142857 H 2.2857143 V 2.2857143 H 5.7142857 V 0 H 0 Z M 13.714286,13.714286 H 10.285714 V 16 H 16 V 10.285714 H 13.714286 Z M 10.285714,0 v 2.2857143 h 3.428572 V 5.7142857 H 16 V 0 Z");
                    switchIcon.setFill(Color.GRAY);
                    MonoPane switchButton = new MonoPane(switchIcon);
                    HBox.setMargin(switchButton, new Insets(5));
                    // Note: we would be tempted to use setOnMouseClicked() on switchButton, but that won't work because
                    // we are inside a ScrollPane and ScrollPaneBehaviour.mousePressed() - which is called before mouse
                    // clicked - requests the focus on the ScrollPane, and this finally prevents the button mouse click.
                    // So we must use mouse pressed to set up our switch button action:
                    switchButton.setOnMousePressed(e -> switchToModalDialog());
                    switchButton.setCursor(Cursor.HAND);
                    searchBox = new HBox(searchTextField, switchButton);
                    searchPane.setContent(searchBox);
                    searchPane.setPrefHeight(USE_COMPUTED_SIZE);
                }
                installSearchBoxForDecidedShowModeIfEnabled();
                dialogCallback = DialogUtil.showDropUpOrDownDialog(dialogPane, button, parameters.getDropParent(), loadedContentProperty, decidedShowMode == ShowMode.DROP_UP);
                dialogCallback.addCloseHook(
                            FXProperties.runNowAndOnPropertiesChange(this::applyNewDecidedShowMode,
                                scene.heightProperty(),
                                dialogPane.heightProperty(),
                                loadedContentProperty
                            )::unregister);
                break;
        }
        // Saving the default (Enter) and cancel (ESC) accelerators before changing them (so we can restore them later)
        var accelerators = SceneUtil.getDefaultAndCancelAccelerators(scene);
        // The restore will happen when the dialog closes
        dialogCallback.addCloseHook(() -> SceneUtil.setDefaultAndCancelAccelerators(scene, accelerators));
        // But while the dialog is open, these are the accelerators we want:
        SceneUtil.setDefaultAccelerator(scene, this::onDialogOk); // Enter = Ok
        SceneUtil.setCancelAccelerator(scene, this::onDialogCancel); // ESC = Cancel
        dialogCallback.addCloseHook(() -> {
            // Button focus management: 2 questions: 1) Should we restore the focus to the button? 2) Should the next button click reopen the dialog?
            if (button != null) {
                // Reply to 1): yes if the last focus was inside the dialog, no otherwise (ex: the dialog closed because the user clicked outside)
                if (SceneUtil.isFocusInsideNode(dialogPane))
                    button.requestFocus();
                // Reply to 2): no if the dialog was closed because the user just pressed the button (his intention is to close the dialog, not to reopen it!)
                userJustPressedButtonInOrderToCloseDialog = button.isPressed(); // See onButtonClicked() which is using this flag
            }
            reset();
        });
        if (searchTextField != null)
            searchTextField.setText(null); // Resetting the search box
    }

    private void reset() {
        // This dialog instance could be reused in theory. However, for some reason (?) it has some width resizing issue
        // after having been shown in the modal dialog, so we force re-creation to have a brand-new instance next time
        // with no width issue
        if (decidedShowMode == ShowMode.MODAL_DIALOG)
            forceDialogRebuiltOnNextShow();
        decidedShowMode = null;
        dialogHighestHeight = 0;
    }

    protected void forceDialogRebuiltOnNextShow() {
        dialogPane = null;
    }

    private Scheduled scheduled;

    private void applyNewDecidedShowMode() {
        if (isLoadedContentLayoutInDialog())
            applyNewDecidedShowModeNow();
        else if (scheduled == null)
            scheduled = UiScheduler.scheduleInAnimationFrame(() -> {
                scheduled = null;
                applyNewDecidedShowMode();
            }, 1, AnimationFramePass.SCENE_PULSE_LAYOUT_PASS);
    }

    private boolean isLoadedContentLayoutInDialog() {
        return dialogPane != null && (dialogPane.isVisible() || dialogPane.getCenter().prefHeight(-1) > 5);
    }

    private void applyNewDecidedShowModeNow() {
        ShowMode previousDecidedShowMode = decidedShowMode;
        if (updateDecidedShowMode() == ShowMode.MODAL_DIALOG)
            switchToModalDialog();
        else {
            if (decidedShowMode != previousDecidedShowMode) {
                installSearchBoxForDecidedShowModeIfEnabled();
            } else {
                // This code is in case a virtual keyboard just appeared, at this stage, the layout is not finished, so
                // we update the dialog position again later (2 animation frames later seems necessary)
                UiScheduler.scheduleInAnimationFrame(this::updateDropUpOrDownDialogPosition, 2,
                        AnimationFramePass.SCENE_PULSE_LAYOUT_PASS);
            }
            updateDropUpOrDownDialogPosition();
            if (!dialogPane.isVisible())
                dialogPane.setVisible(true);
        }
    }

    private void updateDropUpOrDownDialogPosition() {
        TextField searchTextField = getSearchTextField();
        DialogUtil.setDropDialogBounded (dialogPane, searchTextField != null && searchTextField.isFocused());
        DialogUtil.setDropDialogUp(dialogPane, decidedShowMode == ShowMode.DROP_UP);
        DialogUtil.updateDropUpOrDownDialogPosition(dialogPane);
    }

    private void installSearchBoxForDecidedShowModeIfEnabled() {
        if (decidedShowMode == ShowMode.DROP_DOWN) {
            dialogPane.setBottom(null);
            dialogPane.setTop(searchPane);
        } else {
            dialogPane.setTop(null);
            dialogPane.setBottom(searchPane);
        }
        requestSearchTextFieldFocus();
    }

    private void requestSearchTextFieldFocus() {
        TextField searchTextField = getSearchTextField();
        if (searchTextField != null /*&& !searchTextField.isFocused()*/)
            UiScheduler.scheduleInAnimationFrame(() -> SceneUtil.autoFocusIfEnabled(searchTextField), 5);
    }

    private void switchToModalDialog() {
        closeDialog();
        forceDialogRebuiltOnNextShow(); setUpDialog(false); // This line could be removed but
        decidedShowMode = ShowMode.MODAL_DIALOG;
        show();
        requestSearchTextFieldFocus();
    }

    protected void onDialogOk() {
        closeDialog();
    }

    protected void onDialogCancel() {
        closeDialog();
    }

    protected void closeDialog() {
        if (isDialogOpen()) {
            dialogCallback.closeDialog();
            if (closeHandler != null)
                closeHandler.run();
        }
    }

    private Runnable closeHandler;
    public ButtonSelector<T> setCloseHandler(Runnable closeHandler) {
        this.closeHandler = closeHandler;
        return this;
    }

}
