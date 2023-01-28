package cn.navclub.nes4j.app.control;

import cn.navclub.nes4j.app.service.LoadingService;
import cn.navclub.nes4j.app.service.TaskService;
import cn.navclub.nes4j.app.view.GameHall;
import javafx.animation.FadeTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Setter;

/**
 * <p>
 * Custom async load resource visible pane.
 * </p>
 * usage example:
 * <p>
 * <pre>
 * If you use fxml:
 * {@code
 *           <LoadingPane VBox.vgrow="ALWAYS" fx:id="loadingPane" text="%nes4j.loading">
 *                 <node>
 *                     <ScrollPane fitToHeight="true" fitToWidth="true" VBox.vgrow="ALWAYS">
 *                         <FlowPane/>
 *                     </ScrollPane>
 *                 </node>
 *             </LoadingPane>
 * }
 * If you use java code:
 * ...
 *
 * {@code
 *
 *  //Instance LoadingService subclass
 *  var service = new LoadingServiceImpl();
 *  //Instance LoadingPane and transform loading service to it.
 *  var pane    = new LoadingPane(service);
 *  var button  = new Button("download");
 *  button.setOnAction(event->{
 *      pane.load(p0,p1,p2,p3);
 *  });
 *  }
 *
 * ...
 * </pre>
 * </p>
 *
 * @param <T> Wait load data type
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 * @see LoadingService,GameHall
 */
public class LoadingPane<T> extends StackPane {

    private final Label label;
    private final VBox maskPane;
    private final VBox container;

    @Setter
    private LoadingService<T> service;
    private TaskService<T> taskService;
    private final StringProperty text;
    private final FadeTransition transition;
    private final ObjectProperty<Region> node;

    public LoadingPane() {

        Label icon = new Label();
        this.label = new Label();
        this.maskPane = new VBox();
        this.container = new VBox();
        this.transition = new FadeTransition();

        transition.setToValue(0);
        transition.setFromValue(1.0);
        transition.setNode(this.maskPane);
        transition.setDuration(Duration.millis(400));
        transition.setOnFinished(event -> this.maskPane.setVisible(false));

        this.maskPane.getChildren().addAll(icon, this.label);

        this.getStyleClass().add("loading-pane");
        this.maskPane.getStyleClass().add("mask-pane");
        icon.getStyleClass().add("mask-pane-icon");
        this.getChildren().addAll(this.container, this.maskPane);

        this.text = new SimpleStringProperty(this, "text", null);
        this.node = new SimpleObjectProperty<>(this, "node", null);

        this.node.addListener((observable, oldValue, newValue) -> {
            //Remove all old listener
            if (oldValue != null) {
                this.container.prefWidthProperty().unbind();
                this.container.prefWidthProperty().unbind();
                this.container.getChildren().remove(oldValue);
            }

            //Register new listener
            if (newValue != null) {

                this.container.prefWidthProperty().bind(newValue.widthProperty());
                this.container.prefHeightProperty().bind(newValue.heightProperty());

                this.container.getChildren().add(newValue);
            }
        });
    }

    public LoadingPane(Region region) {
        this();
        this.nodeProperty().set(region);
    }

    /**
     * Load data from local file system or remote server.
     *
     * @param params Call params
     * @apiNote Call this method must in Javafx UI thread.
     */
    public void load(Object... params) {
        if (this.service == null) {
            throw new RuntimeException("Loading Service must not null.");
        }

        if (this.taskService != null) {
            this.taskService.cancel();
        }

        this.maskPane.setVisible(true);

        this.taskService = TaskService.execute(new Task<>() {
            @Override
            protected T call() {
                return LoadingPane.this.service.execute(params);
            }
        });
        this.taskService.setOnSucceeded(event -> {
            this.transition.play();
            @SuppressWarnings("all")
            var t = (T) (event.getSource().getValue());
            this.service.onSuccess(t);
        });
    }

    public Region getNode() {
        return node.get();
    }

    public ObjectProperty<Region> nodeProperty() {
        return node;
    }

    public void setNode(Region node) {
        this.node.set(node);
    }

    public String getText() {
        return text.get();
    }

    public StringProperty textProperty() {
        return text;
    }

    public void setText(String text) {
        this.text.set(text);
        this.label.setText(text);
    }
}
