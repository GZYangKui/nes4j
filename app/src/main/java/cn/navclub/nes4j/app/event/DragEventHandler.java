package cn.navclub.nes4j.app.event;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.io.File;

/**
 * Wrap {@link Node} drag event.
 *
 * @author <a href="https://github.com/GZYangKui">GZYangKui</a>
 */
public class DragEventHandler {
    public interface DragEventService {
        /**
         * When DragOver event trigger will call this function check drag data whether satisfy condition.
         *
         * @param board {@inheritDoc}
         * @return If return {@code true} drag continue otherwise cancel
         */
        boolean before(Dragboard board);

        /**
         * When DragDrop event trigger will call this function and transform {@link Dragboard} object.
         *
         * @param board {@inheritDoc}
         * @apiNote If {@link DragEventService#before(Dragboard)} return {@code false} will can't call this function.
         */
        void after(Dragboard board);
    }

    /**
     * Simplify drag file operation.
     */
    public abstract static class FileDragEventService implements DragEventService {
        private final String suffix;
        private final boolean multiple;

        public FileDragEventService(String suffix, boolean multiple) {
            this.suffix = suffix;
            this.multiple = multiple;
        }

        @Override
        public boolean before(Dragboard board) {
            var list = board.getFiles();
            var ok = multiple || list.size() == 1;
            return ok && list
                    .stream()
                    .filter(File::isFile)
                    .filter(it -> it.getName().endsWith(suffix))
                    .count() == list.size();
        }
    }

    private boolean accept;

    private final Node node;
    private final ObjectProperty<Image> image;

    private DragEventHandler(Node node, DragEventService service, TransferMode... modes) {
        this.node = node;
        this.image = new SimpleObjectProperty<>(this, "image", null);

        this.node.setOnDragEntered(event -> {
            var board = event.getDragboard();
            this.accept = service.before(event.getDragboard());
            if (accept && this.getImage() != null) {
                board.setDragView(this.getImage());
            }
        });

        this.node.setOnDragOver(event -> {
            if (!accept) {
                event.consume();
                return;
            }
            event.acceptTransferModes(modes);
        });

        this.node.setOnDragDropped(event -> {
            if (!this.accept) {
                return;
            }
            service.after(event.getDragboard());
            event.setDropCompleted(true);
        });
    }

    public Image getImage() {
        return image.get();
    }

    public ObjectProperty<Image> imageProperty() {
        return image;
    }

    public void setImage(Image image) {
        this.image.set(image);
    }

    public Node getNode() {
        return node;
    }

    public static DragEventHandler register(Node node, DragEventService service, TransferMode... modes) {
        return new DragEventHandler(node, service, modes);
    }
}
