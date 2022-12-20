package cn.navclub.nes4j.app.event;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ControlDragEvent implements EventHandler<MouseEvent> {
    private final Node node;

    private double x;
    private double y;

    public ControlDragEvent(Node node) {
        this.node = node;
        this.node.addEventFilter(MouseEvent.ANY, this);
    }

    @Override
    public void handle(MouseEvent event) {
        var scene = node.getScene();
        var window = (Stage) scene.getWindow();
        var type = event.getEventType();

        if (type == MouseEvent.MOUSE_PRESSED) {
            this.x = event.getSceneX();
            this.y = event.getSceneY();
        } else if (type == MouseEvent.MOUSE_DRAGGED) {
            var xo = event.getScreenX() - this.x;
            var yo = event.getScreenY() - this.y;

            window.setX(Math.max(xo, 0));
            window.setY(Math.max(yo, 0));
        }
    }


    public static void setBind(Node node,String nullable) {
        new ControlDragEvent(node);
    }
}
