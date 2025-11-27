package ui.pinboard;

import common.dto.JournalEntryDTO;
import common.dto.pinboard.*;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PinboardController {

    private final Stage stage;
    private final Pane canvas;
    private final List<PinboardItemModel> items = new ArrayList<>();
    private final List<PinboardLinkModel> links = new ArrayList<>();
    private final Map<String, Node> itemNodeMap = new HashMap<>();
    private final Map<PinboardLinkModel, Line> linkNodeMap = new HashMap<>();
    private final Map<String, TextArea> templateNotesMap = new HashMap<>();
    private final Map<String, VBox> templateDropTargetsMap = new HashMap<>();

    // State
    private boolean isLinkMode = false;
    private boolean isDeleteLinkMode = false;
    private String linkStartId = null;
    private String deleteLinkStartId = null;
    private PinboardItemModel draggedItem = null;
    private PinboardItemModel selectedItem = null;
    private double dragDeltaX, dragDeltaY;
    private double nextItemX = 50;
    private double nextItemY = 50;
    private long lastMoveUpdateTime = 0;

    // UI Components
    private ScrollPane canvasScrollPane;
    private VBox templateVBox;
    private ComboBox<String> linkColorSelector;

    // Removed addedJournalEntryIds set to allow sync/reload
    // private final Set<String> addedJournalEntryIds = new HashSet<>();

    public PinboardController() {
        this.stage = new Stage();
        this.canvas = new Pane();
        initializeUI();
    }

    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.getStylesheets().add(getClass().getResource("/css/pinboard.css").toExternalForm());
        root.getStyleClass().add("pinboard-root");

        // --- Toolbar ---
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("pinboard-toolbar");

        Button addNoteBtn = new Button("Add Note");
        addNoteBtn.setOnAction(e -> createNoteAtCenter());

        ToggleButton linkModeBtn = new ToggleButton("Link Mode");

        ToggleButton deleteLinkModeBtn = new ToggleButton("Delete Link");
        deleteLinkModeBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isDeleteLinkMode = newVal;
            if (isDeleteLinkMode) {
                 linkModeBtn.setSelected(false); // Mutually exclusive
                 deleteLinkStartId = null;
            }
        });

        linkModeBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            isLinkMode = newVal;
            if (isLinkMode) {
                deleteLinkModeBtn.setSelected(false);
                linkStartId = null;
            } else {
                linkStartId = null;
            }
        });

        linkColorSelector = new ComboBox<>();
        linkColorSelector.getItems().addAll("Green", "Yellow", "Red");
        linkColorSelector.setValue("Red");
        linkColorSelector.setPromptText("Link Color");

        Button deleteBtn = new Button("Delete");
        deleteBtn.setOnAction(e -> deleteSelectedItem());

        Button clearBtn = new Button("Clear Board");
        clearBtn.setOnAction(e -> clearBoard());

        Button syncBtn = new Button("Sync Journal");
        syncBtn.setOnAction(e -> syncJournal());

        toolBar.getItems().addAll(addNoteBtn, linkModeBtn, deleteLinkModeBtn, linkColorSelector, new Separator(), deleteBtn, clearBtn, new Separator(), syncBtn);
        root.setTop(toolBar);

        // --- Center: Canvas ---
        canvas.getStyleClass().add("pinboard-canvas");
        canvas.setPrefSize(2000, 2000); // Large canvas

        // Zoom support
        Scale scale = new Scale(1, 1);
        canvas.getTransforms().add(scale);

        canvasScrollPane = new ScrollPane(canvas);
        canvasScrollPane.setPannable(true); // Allow panning with mouse drag when not on an item
        canvasScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        canvasScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // Mouse wheel zoom
        canvasScrollPane.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                double delta = e.getDeltaY();
                double scaleFactor = (delta > 0) ? 1.1 : 0.9;
                double newScaleX = scale.getX() * scaleFactor;
                double newScaleY = scale.getY() * scaleFactor;

                // Clamp zoom
                if (newScaleX >= 0.5 && newScaleX <= 3.0) {
                    scale.setX(newScaleX);
                    scale.setY(newScaleY);
                }
                e.consume();
            }
        });

        root.setCenter(canvasScrollPane);

        // --- Right Panel: Final Exam Template ---
        templateVBox = new VBox(10);
        templateVBox.setPadding(new Insets(10));

        addTemplateSection("Main Suspect(s)");
        addTemplateSection("Primary Motive(s)");
        addTemplateSection("Opportunity / Means");
        addTemplateSection("Weapon");
        addTemplateSection("Key Contradictions");
        addTemplateSection("Supporting Evidence");
        addTemplateSection("Suspicious Behavior");
        addTemplateSection("Alibi Verification");
        addTemplateSection("Remaining Questions");

        ScrollPane rightScroll = new ScrollPane(templateVBox);
        rightScroll.setFitToWidth(true);
        rightScroll.setPrefWidth(250);
        rightScroll.getStyleClass().add("pinboard-sidebar");

        VBox rightPanel = new VBox(new Label("Case Summary Template"), rightScroll);
        rightPanel.setPadding(new Insets(5));
        root.setRight(rightPanel);

        Scene scene = new Scene(root, 1200, 900);
        stage.setScene(scene);
        stage.setTitle("Detective Pinboard");

    }

    private void addTemplateSection(String title) {
        Label header = new Label(title);
        header.getStyleClass().add("section-header");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Type notes here...");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);

        templateNotesMap.put(title, notesArea);

        // Drop target for evidence
        VBox dropTarget = new VBox(5);
        dropTarget.setStyle("-fx-border-color: #555; -fx-border-style: dashed; -fx-padding: 5; -fx-min-height: 40;");
        Label dropLabel = new Label("Drop Evidence Here");
        dropLabel.setTextFill(Color.GRAY);
        dropTarget.getChildren().add(dropLabel);
        dropTarget.setAlignment(javafx.geometry.Pos.CENTER);

        templateDropTargetsMap.put(title, dropTarget);

        dropTarget.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            e.consume();
        });

        dropTarget.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString()) {
                String[] parts = db.getString().split("\\|", 3);
                if (parts.length >= 2) {
                    Label itemLabel = new Label("• " + parts[1]);
                    itemLabel.setTooltip(new Tooltip(parts.length > 2 ? parts[2] : ""));
                    itemLabel.setTextFill(Color.LIGHTGRAY);
                    dropTarget.getChildren().add(itemLabel);
                    dropLabel.setVisible(false);
                    e.setDropCompleted(true);
                }
            }
            e.consume();
        });

        VBox sectionBox = new VBox(2, header, notesArea, dropTarget);
        templateVBox.getChildren().add(sectionBox);
    }

    public void show() {
        stage.show();
        stage.toFront();
    }

    public void reset() {
        clearBoard();
        // Clear template notes
        for (TextArea area : templateNotesMap.values()) {
            area.clear();
        }
        // Clear dropped items in template
        for (VBox box : templateDropTargetsMap.values()) {
            // Keep the "Drop Evidence Here" label, remove others
            box.getChildren().removeIf(node -> node instanceof Label && ((Label) node).getText().startsWith("• "));
            box.getChildren().forEach(node -> node.setVisible(true)); // Show "Drop Here" again
        }

        nextItemX = 50;
        nextItemY = 50;
    }

    private void clearBoard() {
        items.clear();
        links.clear();
        canvas.getChildren().clear();
        itemNodeMap.clear();
        linkNodeMap.clear();
    }

    public void addJournalEntry(JournalEntryDTO entry) {
        // Prevent duplicates by checking if item exists on board
        String refId = String.valueOf(Objects.hash(entry.getText(), entry.getTimestamp()));
        boolean exists = items.stream().anyMatch(item -> refId.equals(item.getRelatedJournalEntryId()));
        if (exists) {
            return;
        }
        // addedJournalEntryIds.add(refId); // Removed

        // Smart Title Generation
        String text = entry.getText();
        String smartTitle = "Journal Entry";
        if (text.toLowerCase().contains("ask") || text.toLowerCase().contains("question")) {
            smartTitle = "Questioning";
        } else if (text.toLowerCase().contains("deduce") || text.toLowerCase().contains("deduction")) {
            smartTitle = "Deduction";
        } else if (text.toLowerCase().contains("found") || text.toLowerCase().contains("examine")) {
            smartTitle = "Evidence";
        }

        // Directly add to board
        PinboardItemModel item = new PinboardItemModel();
        item.setType(PinboardItemModel.ItemType.EVIDENCE);
        item.setTitle(smartTitle);
        item.setContent(text);
        item.setRelatedJournalEntryId(refId);
        item.setX(nextItemX);
        item.setY(nextItemY);
        item.setWidth(200);
        item.setHeight(150);

        addItemToBoard(item);

        // Cascade positioning
        nextItemX += 20;
        nextItemY += 20;
        if (nextItemX > 400) nextItemX = 50;
        if (nextItemY > 400) nextItemY = 50;
    }

    private void createNoteAtCenter() {
        double x = Math.abs(canvasScrollPane.getViewportBounds().getMinX()) + 100;
        double y = Math.abs(canvasScrollPane.getViewportBounds().getMinY()) + 100;

        PinboardItemModel note = new PinboardItemModel();
        note.setType(PinboardItemModel.ItemType.NOTE);
        note.setTitle("New Note");
        note.setContent("Double click to edit");
        note.setX(x);
        note.setY(y);
        note.setColor("#fdfd96");

        addItemToBoard(note);
    }

    private void addItemToBoard(PinboardItemModel item) {
        items.add(item);
        Node node = createItemNode(item);
        itemNodeMap.put(item.getId(), node);
        canvas.getChildren().add(node);
        node.setLayoutX(item.getX());
        node.setLayoutY(item.getY());
    }

    private Node createItemNode(PinboardItemModel item) {
        VBox box = new VBox(2);
        box.setPrefSize(item.getWidth(), item.getHeight());
        box.setMinSize(100, 80); // Minimum size
        box.getStyleClass().add("pinboard-item");
        box.setStyle("-fx-background-color: " + item.getColor() + ";");

        Label titleLabel = new Label(item.getTitle());
        titleLabel.getStyleClass().add("pinboard-item-title");
        titleLabel.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-font-weight: bold;");
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        TextArea contentArea = new TextArea(item.getContent());
        contentArea.getStyleClass().add("pinboard-item-content");
        contentArea.setWrapText(true);
        contentArea.setEditable(true);
        contentArea.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent; -fx-text-fill: black;");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // Resize Handle
        Label resizeHandle = new Label("◢");
        resizeHandle.setTextFill(Color.GRAY);
        resizeHandle.setStyle("-fx-cursor: se-resize; -fx-font-size: 10px;");
        resizeHandle.setAlignment(Pos.BOTTOM_RIGHT);

        // Re-implement resize correctly using wrapper or event filter
        // A simpler way for VBox:
        HBox bottomBar = new HBox(resizeHandle);
        bottomBar.setAlignment(Pos.BOTTOM_RIGHT);
        bottomBar.setPadding(new Insets(0, 2, 0, 0));

        resizeHandle.setOnMousePressed(e -> {
            e.consume(); // Prevent drag of parent
        });
        resizeHandle.setOnMouseDragged(e -> {
            // Convert mouse scene coordinates to canvas local coordinates to account for zoom (scale)
            Point2D mouseLocal = canvas.sceneToLocal(e.getSceneX(), e.getSceneY());

            // Calculate new size relative to the item's position on the canvas
            double newW = Math.max(100, mouseLocal.getX() - item.getX());
            double newH = Math.max(80, mouseLocal.getY() - item.getY());

            box.setPrefSize(newW, newH);
            item.setWidth(newW);
            item.setHeight(newH);
            updateLinks(item); // Force redraw of links
            e.consume();
        });

        // Update model on change
        // Title editing removed per user request to fix cursor issues and dragging.
        // If title updates are needed, implement double-click handler on titleLabel.
        contentArea.textProperty().addListener((obs, o, n) -> item.setContent(n));

        // Drag Here Hint (small text in title area)
        Label dragHint = new Label("Drag to Template");
        dragHint.setStyle("-fx-font-size: 8px; -fx-text-fill: gray; -fx-cursor: hand;");
        dragHint.setOnDragDetected(e -> {
            Dragboard db = dragHint.startDragAndDrop(TransferMode.COPY);
            ClipboardContent cc = new ClipboardContent();
            // Format: TYPE|TITLE|CONTENT|REF_ID
            String refId = item.getRelatedJournalEntryId() != null ? item.getRelatedJournalEntryId() : "NOTE";
            cc.putString("EVIDENCE|" + item.getTitle() + "|" + item.getContent() + "|" + refId);
            db.setContent(cc);
            e.consume();
        });
        dragHint.setOnMousePressed(e -> e.consume()); // Prevent window drag start

        StackPane titleStack = new StackPane(titleLabel, dragHint);
        StackPane.setAlignment(titleLabel, Pos.CENTER_LEFT);
        StackPane.setAlignment(dragHint, Pos.CENTER_RIGHT);
        StackPane.setMargin(dragHint, new Insets(0, 5, 0, 0));
        StackPane.setMargin(titleLabel, new Insets(0, 60, 0, 5)); // Leave space for hint

        // Link logic & Selection
        box.setOnMouseClicked(e -> {
            if (isLinkMode) {
                handleLinkClick(item);
            } else if (isDeleteLinkMode) {
                handleDeleteLinkClick(item);
            } else {
                selectItem(item, box);
            }
            // Do not consume here if we want drag to work?
            // Actually drag is handled by MousePressed/Dragged filters or handlers.
            // MouseClicked happens after drag if movement is small.
            // So consuming here is fine for selection logic.
            e.consume();
        });

        box.getChildren().addAll(titleStack, contentArea, bottomBar);

        // Dragging logic on the canvas
        makeDraggable(box, item);

        return box;
    }

    private void selectItem(PinboardItemModel item, Node node) {
        // Deselect old
        if (selectedItem != null) {
            Node oldNode = itemNodeMap.get(selectedItem.getId());
            if (oldNode != null) {
                oldNode.setStyle(oldNode.getStyle().replace("-fx-effect: dropshadow(three-pass-box, red, 10, 0, 0, 0);", ""));
            }
        }

        selectedItem = item;

        // Highlight new
        if (selectedItem != null) {
            node.setStyle(node.getStyle() + "-fx-effect: dropshadow(three-pass-box, red, 10, 0, 0, 0);");
        }
    }

    private void deleteSelectedItem() {
        if (selectedItem != null) {
            removeItem(selectedItem);
            selectedItem = null;
        }
    }

    private void makeDraggable(Node node, PinboardItemModel item) {
        node.setOnMousePressed(e -> {
            if (!isLinkMode) {
                dragDeltaX = node.getLayoutX() - e.getSceneX();
                dragDeltaY = node.getLayoutY() - e.getSceneY();
                node.toFront();
                e.consume();
            }
        });

        node.setOnMouseDragged(e -> {
            if (!isLinkMode) {
                double newX = e.getSceneX() + dragDeltaX;
                double newY = e.getSceneY() + dragDeltaY;
                node.setLayoutX(newX);
                node.setLayoutY(newY);
                item.setX(newX);
                item.setY(newY);
                updateLinks(item);
                e.consume();
            }
        });
    }

    private void handleLinkClick(PinboardItemModel item) {
        if (linkStartId == null) {
            linkStartId = item.getId();
            // Highlight start node (optional)
            Node node = itemNodeMap.get(linkStartId);
            if (node != null) node.setStyle(node.getStyle() + "-fx-effect: dropshadow(three-pass-box, blue, 10, 0, 0, 0);");
        } else {
            if (!linkStartId.equals(item.getId())) {
                createLink(linkStartId, item.getId());
            }
            // Reset style of start node
            Node node = itemNodeMap.get(linkStartId);
            if (node != null) {
                 // Re-apply selection style if it was selected, otherwise remove effect
                 // Simpler: Just refresh selection visual state or clear effect
                 if (selectedItem != null && selectedItem.getId().equals(linkStartId)) {
                      selectItem(selectedItem, node);
                 } else {
                     node.setStyle(node.getStyle().replace("-fx-effect: dropshadow(three-pass-box, blue, 10, 0, 0, 0);", ""));
                 }
            }
            linkStartId = null;
        }
    }

    private void handleDeleteLinkClick(PinboardItemModel item) {
        if (deleteLinkStartId == null) {
            deleteLinkStartId = item.getId();
            // Visual feedback for start of deletion (maybe different color)
            Node node = itemNodeMap.get(deleteLinkStartId);
            if (node != null) node.setStyle(node.getStyle() + "-fx-effect: dropshadow(three-pass-box, red, 10, 0, 0, 0);");
        } else {
            if (!deleteLinkStartId.equals(item.getId())) {
                removeLinkBetween(deleteLinkStartId, item.getId());
            }

            // Reset visual feedback
            Node node = itemNodeMap.get(deleteLinkStartId);
            if (node != null) {
                 if (selectedItem != null && selectedItem.getId().equals(deleteLinkStartId)) {
                      selectItem(selectedItem, node);
                 } else {
                     node.setStyle(node.getStyle().replace("-fx-effect: dropshadow(three-pass-box, red, 10, 0, 0, 0);", ""));
                 }
            }
            deleteLinkStartId = null;
        }
    }

    private void removeLinkBetween(String startId, String endId) {
        PinboardLinkModel target = null;
        for (PinboardLinkModel link : links) {
             if ((link.getStartItemId().equals(startId) && link.getEndItemId().equals(endId)) ||
                 (link.getStartItemId().equals(endId) && link.getEndItemId().equals(startId))) {
                 target = link;
                 break;
             }
        }
        if (target != null) {
            removeLink(target);
        }
    }

    private void createLink(String startId, String endId) {
        // Check if link exists
        for (PinboardLinkModel link : links) {
            if ((link.getStartItemId().equals(startId) && link.getEndItemId().equals(endId)) ||
                (link.getStartItemId().equals(endId) && link.getEndItemId().equals(startId))) {
                return; // Link exists
            }
        }

        String color = linkColorSelector.getValue() != null ? linkColorSelector.getValue().toUpperCase() : "RED";
        PinboardLinkModel link = new PinboardLinkModel(startId, endId, color);
        links.add(link);
        drawLink(link);
    }

    private void drawLink(PinboardLinkModel link) {
        Node startNode = itemNodeMap.get(link.getStartItemId());
        Node endNode = itemNodeMap.get(link.getEndItemId());

        if (startNode instanceof Region && endNode instanceof Region) {
            Region startRegion = (Region) startNode;
            Region endRegion = (Region) endNode;

            Line line = new Line();

            Color strokeColor;
            switch (link.getColor()) {
                case "GREEN": strokeColor = Color.GREEN; break;
                case "YELLOW": strokeColor = Color.YELLOW; break;
                case "RED":
                default: strokeColor = Color.RED; break;
            }

            line.setStroke(strokeColor);
            line.setStrokeWidth(2);
            // Bind coordinates
            line.startXProperty().bind(startRegion.layoutXProperty().add(startRegion.widthProperty().divide(2)));
            line.startYProperty().bind(startRegion.layoutYProperty().add(startRegion.heightProperty().divide(2)));
            line.endXProperty().bind(endRegion.layoutXProperty().add(endRegion.widthProperty().divide(2)));
            line.endYProperty().bind(endRegion.layoutYProperty().add(endRegion.heightProperty().divide(2)));

            // Allow right click to remove link
            line.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    removeLink(link);
                }
            });

            linkNodeMap.put(link, line);
            canvas.getChildren().add(0, line); // Add behind items
        }
    }

    private void updateLinks(PinboardItemModel item) {
        // JavaFX bindings handle this automatically!
    }

    private void removeItem(PinboardItemModel item) {
        items.remove(item);
        Node node = itemNodeMap.remove(item.getId());
        canvas.getChildren().remove(node);

        // Remove associated links
        List<PinboardLinkModel> toRemove = links.stream()
                .filter(l -> l.getStartItemId().equals(item.getId()) || l.getEndItemId().equals(item.getId()))
                .collect(Collectors.toList());

        toRemove.forEach(this::removeLink);
    }

    private void removeLink(PinboardLinkModel link) {
        links.remove(link);
        Line line = linkNodeMap.remove(link);
        canvas.getChildren().remove(line);
    }

    // Removed persistence methods

    private void syncJournal() {
        // Sync functionality is handled by MainController calling addJournalEntry.
        // But if we want to manually trigger a re-sync, we need access to the data source.
        // Currently, addJournalEntry handles deduplication.
        // So this button essentially is a placeholder or confirmation.
        // However, MainController pushes data to us. We don't pull data.
        // To make this button functional, we'd need a callback or just trust MainController updates.
        // BUT the user asked to "load the journal entries back to the pinboard".
        // If the user clears the board, they might want to get the journal entries back.
        // So we need to reset the addedJournalEntryIds so they can be re-added?
        // No, MainController only calls addJournalEntry when a NEW entry happens or on load.
        // If we want to "Reload", we really need to ask MainController to send us everything again.
        // For now, let's just make it clear the tracking so duplicates can be re-added if pushed again?
        // Actually, the simplest interpretation is that the user cleared the board and wants the journal cards back.
        // But we don't store the journal history here.
        // Let's rely on the auto-save loading for now, but since we removed the Load button...
        // Wait, if "Sync" is clicked, we assume the user wants to fetch data.
        // Since we don't have a reference to the Game Engine here, we might need a Functional Interface callback.
        if (onSyncRequest != null) {
            onSyncRequest.run();
        }
    }

    private Runnable onSyncRequest;
    private java.util.function.Consumer<PinboardUpdateDTO> onUpdateCallback;

    public void setOnSyncRequest(Runnable onSyncRequest) {
        this.onSyncRequest = onSyncRequest;
    }

    public void setOnUpdateCallback(java.util.function.Consumer<PinboardUpdateDTO> callback) {
        this.onUpdateCallback = callback;
    }

    private void sendUpdate(PinboardUpdateDTO update) {
        if (onUpdateCallback != null) {
            onUpdateCallback.accept(update);
        }
    }

    public PinboardStateDTO getState() {
        PinboardStateDTO state = new PinboardStateDTO();
        // Convert models to DTOs
        List<PinboardItemDTO> itemDTOs = items.stream().map(this::toDTO).collect(Collectors.toList());
        List<PinboardLinkDTO> linkDTOs = links.stream().map(this::toDTO).collect(Collectors.toList());

        state.setItems(itemDTOs);
        state.setLinks(linkDTOs);

        Map<String, String> tData = new HashMap<>();
        for (Map.Entry<String, TextArea> entry : templateNotesMap.entrySet()) {
            tData.put(entry.getKey(), entry.getValue().getText());
        }
        state.setTemplateData(tData);

        Map<String, List<String>> droppedItemsMap = new HashMap<>();
        for (Map.Entry<String, VBox> entry : templateDropTargetsMap.entrySet()) {
            List<String> items = new ArrayList<>();
            for (Node child : entry.getValue().getChildren()) {
                if (child instanceof Label && ((Label) child).getText().startsWith("• ")) {
                    items.add(((Label) child).getText().substring(2) + "|" + ((Label) child).getTooltip().getText());
                }
            }
            droppedItemsMap.put(entry.getKey(), items);
        }
        state.setTemplateDroppedItems(droppedItemsMap);

        return state;
    }

    public void applyState(PinboardStateDTO state) {
        if (state == null) return;

        // Avoid sending updates back when applying state
        java.util.function.Consumer<PinboardUpdateDTO> savedCallback = this.onUpdateCallback;
        this.onUpdateCallback = null;

        try {
            clearBoard();

            if (state.getItems() != null) {
                for (PinboardItemDTO dto : state.getItems()) {
                    addItemToBoard(fromDTO(dto));
                }
            }

            if (state.getLinks() != null) {
                for (PinboardLinkDTO dto : state.getLinks()) {
                    PinboardLinkModel link = fromDTO(dto);
                    links.add(link);
                    drawLink(link);
                }
            }

            if (state.getTemplateData() != null) {
                for (Map.Entry<String, String> entry : state.getTemplateData().entrySet()) {
                    TextArea area = templateNotesMap.get(entry.getKey());
                    if (area != null) {
                        area.setText(entry.getValue());
                    }
                }
            }

            if (state.getTemplateDroppedItems() != null) {
                for (Map.Entry<String, List<String>> entry : state.getTemplateDroppedItems().entrySet()) {
                    VBox target = templateDropTargetsMap.get(entry.getKey());
                    if (target != null) {
                        target.getChildren().removeIf(node -> node instanceof Label && ((Label) node).getText().startsWith("• "));
                        target.getChildren().forEach(n -> {
                            if (n instanceof Label && "Drop Evidence Here".equals(((Label) n).getText())) {
                                n.setVisible(false);
                            }
                        });

                        for (String itemStr : entry.getValue()) {
                            String[] parts = itemStr.split("\\|", 2);
                            String text = parts[0];
                            String tooltip = parts.length > 1 ? parts[1] : "";

                            Label itemLabel = new Label("• " + text);
                            itemLabel.setTooltip(new Tooltip(tooltip));
                            itemLabel.setTextFill(Color.LIGHTGRAY);
                            target.getChildren().add(itemLabel);
                        }
                    }
                }
            }
        } finally {
            this.onUpdateCallback = savedCallback;
        }
    }

    public void applyUpdate(PinboardUpdateDTO update) {
        // Run on UI thread
        javafx.application.Platform.runLater(() -> {
            // Temporarily disable callback to prevent echoes
            java.util.function.Consumer<PinboardUpdateDTO> savedCallback = this.onUpdateCallback;
            this.onUpdateCallback = null;
            try {
                switch (update.getType()) {
                    case ADD_ITEM:
                        addItemToBoard(fromDTO(update.getItem()));
                        break;
                    case MOVE_ITEM:
                        updateItemPosition(update.getTargetId(), update.getNewX(), update.getNewY());
                        break;
                    case RESIZE_ITEM:
                         PinboardItemModel itemToResize = findItemById(update.getTargetId());
                         if (itemToResize != null && update.getItem() != null) {
                             itemToResize.setWidth(update.getItem().getWidth());
                             itemToResize.setHeight(update.getItem().getHeight());
                             Node node = itemNodeMap.get(itemToResize.getId());
                             if (node instanceof Region) {
                                 ((Region) node).setPrefSize(itemToResize.getWidth(), itemToResize.getHeight());
                             }
                             updateLinks(itemToResize);
                         }
                         break;
                    case UPDATE_CONTENT:
                        PinboardItemModel itemToUpdate = findItemById(update.getTargetId());
                        if (itemToUpdate != null) {
                            itemToUpdate.setContent(update.getValue());
                            Node node = itemNodeMap.get(itemToUpdate.getId());
                            if (node instanceof VBox) {
                                for (Node child : ((VBox) node).getChildren()) {
                                    if (child instanceof TextArea) {
                                        ((TextArea) child).setText(update.getValue());
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    case REMOVE_ITEM:
                        PinboardItemModel itemToRemove = findItemById(update.getTargetId());
                        if (itemToRemove != null) removeItem(itemToRemove);
                        break;
                    case ADD_LINK:
                        PinboardLinkModel link = fromDTO(update.getLink());
                        links.add(link);
                        drawLink(link);
                        break;
                    case REMOVE_LINK:
                        if (update.getLink() != null) {
                            // Find matching link
                            PinboardLinkModel target = null;
                            for (PinboardLinkModel l : links) {
                                if (l.getStartItemId().equals(update.getLink().getStartItemId()) &&
                                    l.getEndItemId().equals(update.getLink().getEndItemId())) {
                                    target = l;
                                    break;
                                }
                            }
                            if (target != null) removeLink(target);
                        }
                        break;
                    case UPDATE_TEMPLATE_NOTE:
                        TextArea area = templateNotesMap.get(update.getKey());
                        if (area != null) area.setText(update.getValue());
                        break;
                    case UPDATE_TEMPLATE_DROP:
                        // Full refresh of drop target for simplicity
                         VBox target = templateDropTargetsMap.get(update.getKey());
                         if (target != null) {
                             target.getChildren().removeIf(node -> node instanceof Label && ((Label) node).getText().startsWith("• "));
                             // We assume value is empty or handled elsewhere for complex list updates.
                             // Ideally send full list or add/remove action.
                             // For now, let's skip complex sidebar sync or assume full state sync handles it best.
                             // Or parsing the value?
                         }
                        break;
                     case CLEAR_BOARD:
                        clearBoard();
                        break;
                }
            } finally {
                this.onUpdateCallback = savedCallback;
            }
        });
    }

    private void updateItemPosition(String id, double x, double y) {
        PinboardItemModel item = findItemById(id);
        if (item != null) {
            item.setX(x);
            item.setY(y);
            Node node = itemNodeMap.get(id);
            if (node != null) {
                node.setLayoutX(x);
                node.setLayoutY(y);
                updateLinks(item);
            }
        }
    }

    private PinboardItemModel findItemById(String id) {
        return items.stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
    }

    // --- DTO Converters ---

    private PinboardItemDTO toDTO(PinboardItemModel model) {
        PinboardItemDTO dto = new PinboardItemDTO();
        dto.setId(model.getId());
        dto.setType(model.getType().name());
        dto.setTitle(model.getTitle());
        dto.setContent(model.getContent());
        dto.setRelatedJournalEntryId(model.getRelatedJournalEntryId());
        dto.setX(model.getX());
        dto.setY(model.getY());
        dto.setWidth(model.getWidth());
        dto.setHeight(model.getHeight());
        dto.setColor(model.getColor());
        return dto;
    }

    private PinboardItemModel fromDTO(PinboardItemDTO dto) {
        PinboardItemModel model = new PinboardItemModel();
        if (dto.getId() != null) model.setId(dto.getId()); // Use existing ID if provided
        model.setType(PinboardItemModel.ItemType.valueOf(dto.getType()));
        model.setTitle(dto.getTitle());
        model.setContent(dto.getContent());
        model.setRelatedJournalEntryId(dto.getRelatedJournalEntryId());
        model.setX(dto.getX());
        model.setY(dto.getY());
        model.setWidth(dto.getWidth());
        model.setHeight(dto.getHeight());
        model.setColor(dto.getColor());
        return model;
    }

    private PinboardLinkDTO toDTO(PinboardLinkModel model) {
        return new PinboardLinkDTO(model.getStartItemId(), model.getEndItemId(), model.getColor());
    }

    private PinboardLinkModel fromDTO(PinboardLinkDTO dto) {
        return new PinboardLinkModel(dto.getStartItemId(), dto.getEndItemId(), dto.getColor());
    }
}
