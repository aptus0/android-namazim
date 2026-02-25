package com.example.namazm.data.model;

public class HadithCollection {

    private final String id;
    private final String title;
    private final String description;
    private final String sourceLabel;
    private final String topic;
    private final String url;

    public HadithCollection(
            String id,
            String title,
            String description,
            String sourceLabel,
            String topic,
            String url
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.sourceLabel = sourceLabel;
        this.topic = topic;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getTopic() {
        return topic;
    }

    public String getUrl() {
        return url;
    }
}
