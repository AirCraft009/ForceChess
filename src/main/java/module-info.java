module org.mxnik.forcechess {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires javafx.graphics;
    requires annotations;
    requires java.desktop;
    requires nd4j.api;
    requires deeplearning4j.nn;

    exports org.mxnik.forcechess.user.UI;
    exports org.mxnik.forcechess.Util;
    exports org.mxnik.forcechess.user.UI.ChessControllView;
    exports org.mxnik.forcechess.user.ChessLogic;
}