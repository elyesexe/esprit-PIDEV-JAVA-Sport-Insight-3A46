package tn.esprit.gui;

import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

public final class SidebarModuleGroup {
    private static final String ACTIVE_STYLE = "navbar-nav-button-active";
    private static final String MATCHS_LABEL = "Matchs";

    private final Button matchsButton;
    private final Pane childrenBox;
    private final Button equipesButton;
    private final Button leaguesButton;
    private final Button joueursButton;

    private ActiveModule activeModule = ActiveModule.NONE;
    private boolean expanded;

    public SidebarModuleGroup(
            Button matchsButton,
            Pane childrenBox,
            Button equipesButton,
            Button leaguesButton,
            Button joueursButton
    ) {
        this.matchsButton = matchsButton;
        this.childrenBox = childrenBox;
        this.equipesButton = equipesButton;
        this.leaguesButton = leaguesButton;
        this.joueursButton = joueursButton;
    }

    public void initialize(ActiveModule activeModule) {
        this.activeModule = activeModule == null ? ActiveModule.NONE : activeModule;
        expanded = this.activeModule == ActiveModule.EQUIPES
                || this.activeModule == ActiveModule.LEAGUES
                || this.activeModule == ActiveModule.JOUEURS;
        applyActiveState();
        applyExpandedState();
    }

    public boolean handleMatchsClick() {
        if (activeModule == ActiveModule.MATCHS) {
            setExpanded(!expanded);
            return true;
        }
        if (!expanded) {
            setExpanded(true);
            return true;
        }
        return false;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        applyExpandedState();
    }

    private void applyExpandedState() {
        boolean visible = expanded && childrenBox != null;
        if (childrenBox != null) {
            childrenBox.setManaged(visible);
            childrenBox.setVisible(visible);
        }
        if (matchsButton != null) {
            matchsButton.setText(MATCHS_LABEL + (visible ? " -" : " +"));
        }
    }

    private void applyActiveState() {
        clearActive(equipesButton);
        clearActive(matchsButton);
        clearActive(leaguesButton);
        clearActive(joueursButton);

        switch (activeModule) {
            case EQUIPES -> addActive(equipesButton);
            case MATCHS -> addActive(matchsButton);
            case LEAGUES -> addActive(leaguesButton);
            case JOUEURS -> addActive(joueursButton);
            case NONE -> {
            }
        }
    }

    private void addActive(Button button) {
        if (button != null && !button.getStyleClass().contains(ACTIVE_STYLE)) {
            button.getStyleClass().add(ACTIVE_STYLE);
        }
    }

    private void clearActive(Button button) {
        if (button != null) {
            button.getStyleClass().remove(ACTIVE_STYLE);
        }
    }

    public enum ActiveModule {
        NONE,
        EQUIPES,
        MATCHS,
        LEAGUES,
        JOUEURS
    }
}
