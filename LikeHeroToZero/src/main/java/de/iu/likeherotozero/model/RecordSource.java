package de.iu.likeherotozero.model;

public enum RecordSource {

    WORLD_BANK("Weltbank", "badge badge-import"),
    RESEARCH("Forschungsbeitrag", "badge badge-research");

    private final String label;
    private final String badgeStyleClass;

    RecordSource(String label, String badgeStyleClass) {
        this.label = label;
        this.badgeStyleClass = badgeStyleClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeStyleClass() {
        return badgeStyleClass;
    }
}
