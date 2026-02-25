package com.example.namazm.data.model;

public class FavoriteContent {

    private final String id;
    private final String dateLabel;
    private final String title;
    private final String summary;
    private final String fullText;
    private final String source;

    public FavoriteContent(
            String id,
            String dateLabel,
            String title,
            String summary,
            String fullText,
            String source
    ) {
        this.id = id;
        this.dateLabel = dateLabel;
        this.title = title;
        this.summary = summary;
        this.fullText = fullText;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getFullText() {
        return fullText;
    }

    public String getSource() {
        return source;
    }
}
