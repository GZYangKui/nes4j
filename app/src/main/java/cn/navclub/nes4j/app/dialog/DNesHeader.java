package cn.navclub.nes4j.app.dialog;

import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.bin.config.NameMirror;
import cn.navclub.nes4j.bin.config.TV;
import cn.navclub.nes4j.bin.io.Cartridge;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import java.io.File;
import java.util.Arrays;

public class DNesHeader extends Dialog<Boolean> {
    @FXML
    private ToggleGroup vGroup;
    @FXML
    private ChoiceBox<String> cb0;
    @FXML
    private ChoiceBox<String> cb1;
    @FXML
    private ChoiceBox<NameMirror> cb2;
    @FXML
    private ChoiceBox<TV> cb3;

    public DNesHeader(File file, Window owner) {
        Cartridge cartridge = new Cartridge(file);


        var pane = this.getDialogPane();

        var gridPane = FXResource.loadFXML(this);

        //Mask GridPane mouse event
        gridPane.addEventFilter(MouseEvent.ANY, Event::consume);

        Arrays.stream(TV.values()).forEach(it -> this.cb3.getItems().add(it));
        Arrays.stream(NameMirror.values()).forEach(it -> this.cb2.getItems().add(it));

        this.cb1.getSelectionModel().select(StrUtil.toKB(cartridge.getChSize()));
        this.cb0.getSelectionModel().select(StrUtil.toKB(cartridge.getRgbSize()));
        this.cb2.getSelectionModel().select(cartridge.getMirrors().ordinal());
        this.cb3.getSelectionModel().select(cartridge.getTv().ordinal());
        this.vGroup.selectToggle(this.vGroup.getToggles().get(cartridge.getFormat().ordinal()));


        pane.setContent(gridPane);
        pane.getButtonTypes().addAll(ButtonType.APPLY);
        ((Button) (pane.lookupButton(ButtonType.APPLY))).setText(INes.localeValue("nes4j.run"));

        this.initOwner(owner);
        this.setTitle(StrUtil.getFileName(file));
        this.setResultConverter(buttonType -> !(buttonType == null));
    }
}
