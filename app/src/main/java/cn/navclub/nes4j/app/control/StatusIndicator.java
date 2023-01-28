//package cn.navclub.nes4j.app.control;
//
//import cn.navclub.nes4j.app.assets.FXResource;
//import javafx.animation.FadeTransition;
//import javafx.beans.property.ObjectProperty;
//import javafx.beans.property.SimpleObjectProperty;
//import javafx.collections.ListChangeListener;
//import javafx.scene.Node;
//import javafx.scene.Parent;
//
//import javafx.scene.image.Image;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.VBox;
//import javafx.util.Duration;
//
///**
// * Custom status indicator control,current implement only support {@link Parent} and subclass.
// *
// * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
// */
//public class StatusIndicator extends VBox {
//    private static final Image DEFAULT_IMAGE = FXResource.loadImage("empty.png");
//    private static final Image DEFAULT_ERROR_IMAGE = FXResource.loadImage("error.png");
//
//    private final ImageView icon;
//    private ObjectProperty<Parent> attach;
//    private final ObjectProperty<Type> type;
//    private ListChangeListener<Node> listChangeListener;
//
//    public StatusIndicator() {
//        this.icon = new ImageView();
//        this.type = new SimpleObjectProperty<>(this, "type", null);
//
//        this.getStyleClass().add("status-indicator");
//        this.getChildren().add(this.icon);
//
//
//        //listener type change
//        this.type.addListener(((observable, oldValue, newValue) -> {
//            var image = switch (newValue) {
//                case ERROR -> DEFAULT_ERROR_IMAGE;
//                case LOAD -> DEFAULT_LOAD_IMAGE;
//                default -> DEFAULT_IMAGE;
//            };
//            this.icon.setImage(image);
//        }));
//    }
//
//    private ListChangeListener<Node> listChangeListener() {
//        return c -> {
//            var children = this.getAttach().getChildrenUnmodifiable();
//            if (children.size() > 0) {
//                transition.play();
//            } else {
//                this.setVisible(true);
//            }
//        };
//    }
//
//    public Parent getAttach() {
//        return this.attachProperty().get();
//    }
//
//    public ObjectProperty<Parent> attachProperty() {
//        if (this.attach == null) {
//            this.attach = new SimpleObjectProperty<>(this, "attach", null);
//        }
//        return attach;
//    }
//
//    public void setAttach(Parent attach) {
//        var oldValue = this.getAttach();
//        if (oldValue == attach) {
//            return;
//        }
//        if (oldValue != null) {
//            oldValue.getChildrenUnmodifiable().removeListener(this.listChangeListener);
//        }
//        this.listChangeListener = this.listChangeListener();
//        attach.getChildrenUnmodifiable().addListener(this.listChangeListener);
//        this.attachProperty().set(attach);
//    }
//
//    public Type getType() {
//        return type.get();
//    }
//
//    public ObjectProperty<Type> typeProperty() {
//        return type;
//    }
//
//    public void setType(Type type) {
//        this.type.set(type);
//    }
//
//    public enum Type {
//        //Empty
//        EMPTY,
//        //Load
//        LOAD,
//        //error
//        ERROR
//    }
//}
