module org.example.triharf {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires javafx.media;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires java.sql;
    requires java.naming;
    requires com.google.gson;
    requires org.slf4j;

    opens org.example.triharf to javafx.fxml;
    opens org.example.triharf.models to org.hibernate.orm.core;
    opens org.example.triharf.controllers to javafx.fxml;
    opens org.example.triharf.utils to javafx.fxml;
    
    exports org.example.triharf;
    exports org.example.triharf.controllers;
    exports org.example.triharf.models;
    exports org.example.triharf.utils;
    
}