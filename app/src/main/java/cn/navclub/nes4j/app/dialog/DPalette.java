package cn.navclub.nes4j.app.dialog;

import cn.navclub.nes4j.app.FXResource;

import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class DPalette extends Dialog<ButtonType> {
    private static final String STYLE_SHEET = FXResource.loadStyleSheet("SystemPalette.css");

    private final int[][] copy;
    private final GridPane gridPane;
    private final ColorPicker colorPicker;

    private int index;
    private boolean manual;


    public DPalette(int[][] palette) {
        this.gridPane = new GridPane();
        this.colorPicker = new ColorPicker();
        this.copy = new int[palette.length][];
        var content = new VBox(this.colorPicker, this.gridPane);

        this.arrDeepCopy(palette, copy);

        this.colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (this.gridPane.getChildren().isEmpty() || !manual) {
                this.manual = true;
                return;
            }

            var rgb = palette[this.index];

            rgb[0] = (int) Math.round(newValue.getRed() * 0xff);
            rgb[1] = (int) Math.round(newValue.getGreen() * 0xff);
            rgb[2] = (int) Math.round(newValue.getBlue() * 0xff);

            var cell = (Rectangle) this.gridPane.getChildren().get(this.index);
            cell.setFill(newValue);
        });

        this.initPalette();

        this.setTitle("System Palette");
        this.getDialogPane().setContent(content);
        this.getDialogPane().getStylesheets().add(STYLE_SHEET);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.APPLY, ButtonType.CANCEL);
    }

    private void initPalette() {
        //Loop generate color cell grid
        var size = this.copy.length;
        for (int i = 0; i < size; i++) {
            var col = i % 20;
            var row = i / 20;
            var rgb = this.copy[i];
            var cell = new Rectangle(30, 30);
            cell.setFill(Color.rgb(rgb[0], rgb[1], rgb[2]));
            if (i == 0) {
                this.colorPicker.setValue((Color) cell.getFill());
            }
            int finalI = i;
            cell.setOnMouseClicked(event -> {
                this.index = finalI;
                this.manual = false;
                this.colorPicker.setValue((Color) cell.getFill());
            });
            this.gridPane.add(cell, col, row);
        }
    }

    private void arrDeepCopy(int[][] src, int[][] dst) {
        for (int i = 0; i < src.length; i++) {
            var s = src[i];
            var d = dst[i];
            if (d == null) {
                dst[i] = d = new int[s.length];
            }
            System.arraycopy(s, 0, d, 0, s.length);
        }
    }

    public void restore(int[][] palette) {
        this.arrDeepCopy(this.copy, palette);
    }
}
