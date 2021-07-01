package gui;

import gamemodel.model.GameConfig;
import gamemodel.model.GameState;
import gamemodel.model.auxiliary.Coordinate;
import gamemodel.model.auxiliary.Direction;
import gamemodel.model.auxiliary.FieldSize;
import gametree.GameTreeNode;
import gametree.GameTreeNodeData;
import gametree.NodeRole;
import gametree.ServerInfo;
import gui.auxiliary.PlayerRating;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class MainViewController {

    @FXML
    private HBox canvasBox;

    @FXML
    private TableView<ServerInfo> serverTableView;

    @FXML
    private TableColumn<ServerInfo, String> serverMasterColumn;

    @FXML
    private TableColumn<ServerInfo, Integer> serverPlayersNumberColumn;

    @FXML
    private TableColumn<ServerInfo, String> serverFieldSizeColumn;

    @FXML
    private TableColumn<ServerInfo, String> serverFoodFormulaColumn;

    @FXML
    private Button playerConnectButton;

    @FXML
    private Button viewerConnectButton;

    @FXML
    private ListView<String> infoMessageListView;

    @FXML
    private Label currentMasterLabel;

    @FXML
    private Label currentFieldSizeLabel;

    @FXML
    private Label currentFoodLabel;

    @FXML
    private TableView<PlayerRating> ratingTableView;

    @FXML
    private TableColumn<PlayerRating, String> ratingNameColumn;

    @FXML
    private TableColumn<PlayerRating, Integer> ratingScoreColumn;

    @FXML
    private Button newGameButton;

    @FXML
    private Button leaveGameButton;

    private Canvas canvas;
    private GameTreeNode gameTreeNode;
    private GameTreeNodeData gameTreeNodeData;
    private volatile int PIXELS_PER_CELL;

    public void init(Stage primaryStage, GameTreeNode gameTreeNode) {
        this.gameTreeNode = gameTreeNode;
        gameTreeNodeData = gameTreeNode.getNodeData();

        Platform.runLater(() -> {
            primaryStage.setMaximized(true);
            primaryStage.setResizable(false);
            setButtonsDisabled(false);

            initRatingTable();
            initServerTable();
            createCanvas();
        });

        gameTreeNodeData.addPropertyChangeListener(GameTreeNodeData.Property.GAME_STARTED,
                evt -> Platform.runLater(() -> {
                    createCanvas();
                    fillCanvas();
                    fillLabels();
                })
        );

        gameTreeNodeData.addPropertyChangeListener(GameTreeNodeData.Property.GAME_STATE_CHANGED,
                evt -> Platform.runLater(() -> {
                    fillCanvas();
                    fillLabels();
                    fillRatingTable();
                })
        );

        gameTreeNodeData.addPropertyChangeListener(GameTreeNodeData.Property.MASTER_CHANGED,
                evt -> Platform.runLater(this::fillMasterLabel)
        );

        gameTreeNodeData.addPropertyChangeListener(GameTreeNodeData.Property.DISCONNECTED,
                evt -> Platform.runLater(() -> {
                    setButtonsDisabled(false);
                    clearCanvas();
                    clearLabels();
                    clearRatingTable();
                })
        );

        gameTreeNodeData.addPropertyChangeListener(GameTreeNodeData.Property.INFO_MESSAGE_GOT,
                evt -> Platform.runLater(() -> {
                    String infoMessage = (String) evt.getNewValue();
                    List<String> items = infoMessageListView.getItems();
                    int lastIndex = infoMessageListView.getEditingIndex();
                    items.add(infoMessage);
                    infoMessageListView.scrollTo(lastIndex);
                })
        );

        gameTreeNodeData.addPropertyChangeListener(GameTreeNodeData.Property.SERVER_INFO_GOT,
                evt -> Platform.runLater(() -> {
                    ServerInfo serverInfo = (ServerInfo) evt.getNewValue();
                    List<ServerInfo> items = serverTableView.getItems();
                    items.remove(serverInfo);
                    items.add(serverInfo);
                })
        );

        gameTreeNodeData.addPropertyChangeListener(GameTreeNodeData.Property.SERVER_INFO_REMOVED,
                evt -> Platform.runLater(() -> {
                    ServerInfo serverInfo = (ServerInfo) evt.getNewValue();
                    List<ServerInfo> items = serverTableView.getItems();
                    items.remove(serverInfo);
                })
        );
    }

    @FXML
    void onKeyPressed(KeyEvent event) {
        Platform.runLater(() -> {
            if (!gameTreeNodeData.isGameStarted()) {
                return;
            }

            try {
                switch (event.getCode()) {
                    case A:
                        gameTreeNode.setMovementDirection(Direction.LEFT);
                        break;
                    case D:
                        gameTreeNode.setMovementDirection(Direction.RIGHT);
                        break;
                    case S:
                        gameTreeNode.setMovementDirection(Direction.DOWN);
                        break;
                    case W:
                        gameTreeNode.setMovementDirection(Direction.UP);
                        break;
                    default:
                        break;
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        });
    }

    @FXML
    void leaveCurrentGame(ActionEvent event) {
        Platform.runLater(() -> {
            clearLabels();
            clearCanvas();
            clearRatingTable();

            try {
                gameTreeNode.leaveGame();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    void startNewGame(ActionEvent event) {
        Platform.runLater(() -> {
            setButtonsDisabled(true);

            gameTreeNode.startOwnGame();
        });
    }

    @FXML
    void connectAsPlayer(ActionEvent event) {
        connect(false);
    }

    @FXML
    void connectAsViewer(ActionEvent event) {
        connect(true);
    }

    private void connect(boolean onlyView) {
        Platform.runLater(() -> {
            ServerInfo selectedServerInfo = serverTableView.getSelectionModel().getSelectedItem();
            if (selectedServerInfo != null) {
                setButtonsDisabled(true);
                try {
                    gameTreeNode.connect(selectedServerInfo, onlyView);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setButtonsDisabled(boolean isInGame) {
        newGameButton.setDisable(isInGame);
        leaveGameButton.setDisable(!isInGame);
        playerConnectButton.setDisable(isInGame);
        viewerConnectButton.setDisable(isInGame);
    }

    private void createCanvas() {
        GameConfig gameConfig = gameTreeNodeData.getGameConfig();
        FieldSize fieldSize = gameConfig.getFieldSize();

        double pixelsPerX = canvasBox.getWidth() / fieldSize.getWidth();
        double pixelsPerY = canvasBox.getHeight() / fieldSize.getHeight();
        PIXELS_PER_CELL = (int) Math.min(pixelsPerX, pixelsPerY);

        canvas = new Canvas(
                fieldSize.getWidth() * PIXELS_PER_CELL,
                fieldSize.getHeight() * PIXELS_PER_CELL
        );
        canvasBox.getChildren().clear();
        canvasBox.getChildren().add(canvas);
    }

    private void fillCanvas() {
        clearCanvas();

        synchronized (gameTreeNodeData) {
            GameState gameState = gameTreeNodeData.getGameState();
            if (gameState == null) {
                return;
            }

            synchronized (gameState) {
                GraphicsContext gc = canvas.getGraphicsContext2D();

                gc.setFill(Color.RED);
                for (var foodCoordinate: gameState.getFoodCoordinates()) {
                    drawCell(gc, foodCoordinate);
                }

                int playerID = gameTreeNodeData.getPlayerID();
                var snakes = gameState.getSnakes();
                for (var id: snakes.keySet()) {
                    if (id == playerID) {
                        gc.setFill(Color.ANTIQUEWHITE);
                    } else {
                        gc.setFill(Color.AQUAMARINE);
                    }

                    var snake = snakes.get(id);
                    var bodyCoordinates = snake.getBodyCoordinates();
                    for (var coordinate: bodyCoordinates) {
                        drawCell(gc, coordinate);
                    }

                    var headCoordinate = snake.getHeadCoordinate();
                    if (id == playerID) {
                        gc.setFill(Color.FIREBRICK);
                    } else {
                        gc.setFill(Color.BLUEVIOLET);
                    }
                    drawCell(gc, headCoordinate);
                }
            }
        }
    }

    private void clearCanvas() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void fillLabels() {
        GameConfig gameConfig = gameTreeNodeData.getGameConfig();

        FieldSize fieldSize = gameConfig.getFieldSize();
        currentFieldSizeLabel.setText(
                String.format("%d x %d", fieldSize.getWidth(), fieldSize.getHeight())
        );

        int staticFoodAmount = gameConfig.getStaticFoodAmount();
        double foodPerPlayer = gameConfig.getFoodPerPlayer();
        currentFoodLabel.setText(
                String.format("%d + %.2f * x", staticFoodAmount, foodPerPlayer)
        );

        fillMasterLabel();
    }

    private void fillMasterLabel() {
        Integer masterID = gameTreeNodeData.getMasterPlayerID();
        if (masterID == null) {
            currentMasterLabel.setText("...");
            return;
        }

        GameState gameState = gameTreeNodeData.getGameState();
        if (gameState == null) {
            currentMasterLabel.setText("...");
            return;
        }

        synchronized (gameState) {
            var masterPlayer = gameState.getPlayers().get(masterID);
            String masterName = (masterPlayer == null) ? "..." : masterPlayer.getName();
            currentMasterLabel.setText(masterName);
        }
    }

    private void clearLabels() {
        currentMasterLabel.setText("...");
        currentFieldSizeLabel.setText("...");
        currentFoodLabel.setText("...");
    }

    private void initRatingTable() {
        ratingTableView.setPlaceholder(new Label("..."));

        Comparator<Integer> integerComparator = Integer::compareTo;
        ratingScoreColumn.comparatorProperty().set(integerComparator.reversed());
        ratingTableView.getSortOrder().add(ratingScoreColumn);

        ratingNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        ratingScoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
    }

    private void fillRatingTable() {
        clearRatingTable();

        GameState gameState = gameTreeNodeData.getGameState();
        if (gameState == null) {
            return;
        }

        synchronized (gameState) {
            gameState.getPlayers().values().stream()
                    .filter(player -> player.getNodeRole() != NodeRole.VIEWER)
                    .forEach(player -> ratingTableView.getItems().add(
                            new PlayerRating(player.getName(), player.getScore()))
                    );
        }
    }

    private void clearRatingTable() {
        ratingTableView.getItems().clear();
    }

    private void initServerTable() {
        serverTableView.setPlaceholder(new Label("..."));
        serverMasterColumn.setCellValueFactory(new PropertyValueFactory<>("masterName"));
        serverFieldSizeColumn.setCellValueFactory(new PropertyValueFactory<>("fieldSize"));
        serverPlayersNumberColumn.setCellValueFactory(new PropertyValueFactory<>("playersNumber"));
        serverFoodFormulaColumn.setCellValueFactory(new PropertyValueFactory<>("foodFormula"));
    }

    private void drawCell(GraphicsContext gc, Coordinate coordinate) {
        gc.fillRect(
                coordinate.getX() * PIXELS_PER_CELL,
                coordinate.getY() * PIXELS_PER_CELL,
                PIXELS_PER_CELL,
                PIXELS_PER_CELL
        );
    }
}

