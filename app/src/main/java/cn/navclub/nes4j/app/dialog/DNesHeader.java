package cn.navclub.nes4j.app.dialog;

import cn.navclub.nes4j.app.INes;
import cn.navclub.nes4j.app.assets.FXResource;
import cn.navclub.nes4j.app.util.StrUtil;
import cn.navclub.nes4j.bin.config.NMapper;
import cn.navclub.nes4j.bin.config.NameMirror;
import cn.navclub.nes4j.bin.config.TV;
import cn.navclub.nes4j.bin.io.Cartridge;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.io.File;
import java.util.Arrays;

public class DNesHeader extends Dialog<Boolean> {
    private static final int DEFAULT_SCALE = 3;
    @FXML
    private GridPane attrGrid;
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
    @FXML
    private TextField scaleTextField;
    @FXML
    private ChoiceBox<NMapper> cbMapper;

    public DNesHeader(File file, Window owner) {
        Cartridge cartridge = new Cartridge(file);


        var pane = this.getDialogPane();

        pane.setContent(FXResource.loadFXML(this));

        //Read only attribute
        this.attrGrid.addEventFilter(MouseEvent.ANY, Event::consume);


        Arrays.stream(TV.values()).forEach(it -> this.cb3.getItems().add(it));
        Arrays.stream(NameMirror.values()).forEach(it -> this.cb2.getItems().add(it));
        Arrays.stream(NMapper.values()).forEach(it -> this.cbMapper.getItems().add(it));

        this.scaleTextField.setText(Integer.toString(DEFAULT_SCALE));
        this.cb1.getSelectionModel().select(StrUtil.toKB(cartridge.getChSize()));
        this.cb0.getSelectionModel().select(StrUtil.toKB(cartridge.getRgbSize()));
        this.cb2.getSelectionModel().select(cartridge.getMirrors().ordinal());
        this.cb3.getSelectionModel().select(cartridge.getTv().ordinal());
        this.cbMapper.getSelectionModel().select(cartridge.getMapper());
        this.vGroup.selectToggle(this.vGroup.getToggles().get(cartridge.getFormat().ordinal()));


        pane.getButtonTypes().addAll(ButtonType.APPLY);
        var btn = ((Button) (pane.lookupButton(ButtonType.APPLY)));
        if (cartridge.getMapper().isImpl()) {
            btn.setText(INes.localeValue("nes4j.run"));
        } else {
            btn.setText(INes.localeValue("nes4j.unsupport"));
        }
        this.initOwner(owner);
        this.setTitle(StrUtil.getFileName(file));
        this.setResultConverter(buttonType -> !(buttonType == null) && cartridge.getMapper().isImpl());
    }

    public int viewportScale() {
        var scale = DEFAULT_SCALE;
        var text = scaleTextField.getText();
        if (StrUtil.isNotBlank(text)) {
            try {
                scale = Integer.parseInt(text);
            } catch (Exception ignore) {

            }
        }
        return scale;
    }
}
