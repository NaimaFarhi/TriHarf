<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.*?>
<?import javafx.scene.image.*?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.text.*?>

<BorderPane xmlns="http://javafx.com/javafx"
            xmlns:fx="http://javafx.com/fxml"
            prefWidth="1100"
            prefHeight="650"
            stylesheets="@../style/main.css">

    <!-- ===== TOP BAR ===== -->
    <top>
        <HBox alignment="CENTER" spacing="15" styleClass="top-bar">
            <ImageView fitHeight="60" fitWidth="60">
                <image>
                    <Image url="@../images/logo.png"/>
                </image>
            </ImageView>
            <Label text="BACCALAURÉAT" styleClass="title"/>
        </HBox>
    </top>

    <!-- ===== CENTER CONTENT ===== -->
    <center>
        <HBox spacing="30" padding="20">

            <!-- ===== LEFT MENU ===== -->
            <VBox spacing="20" prefWidth="650">

                <Button text="PARTIE SOLO" styleClass="btn-solo"/>

                <HBox spacing="20">
                    <Button text="MULTI JOUEUR" styleClass="btn-multi"/>
                    <Button text="BATTLE ROYALE" styleClass="btn-battle"/>
                </HBox>

                <HBox spacing="20">
                    <Button text="BATTLE ROYALE" styleClass="btn-gold"/>
                    <Button text="MODE CHAOS" styleClass="btn-chaos"/>
                </HBox>

                <HBox spacing="15" alignment="CENTER_LEFT">
                    <Button text="CLASSE AMIS" styleClass="btn-small"/>
                    <Button text="TOP CLASSEMENT" styleClass="btn-small"/>
                </HBox>

            </VBox>

            <!-- ===== STATISTICS PANEL ===== -->
            <VBox spacing="15" prefWidth="350" styleClass="stats-panel">

                <Label text="STATISTIQUES" styleClass="stats-title"/>

                <Label text="🏆 Victoires : 21"/>
                <Label text="🎮 Parties jouées : 42"/>
                <Label text="⭐ Meilleur score : 63"/>

                <Separator/>

                <Label text="Records catégorie :" styleClass="stats-subtitle"/>
                <Label text="👑 50 sec  – Prénom : Lisa"/>
                <Label text="👑 46 sec  – Pays : Japon"/>
                <Label text="👑 32 sec  – Animal : Kangourou"/>

                <Button text="PROFIL" styleClass="btn-profile"/>

            </VBox>

        </HBox>
    </center>

</BorderPane>
