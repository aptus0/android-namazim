package com.example.namazm.data.model;

public class HadithOfTheDay {

    private final String id;
    private final String dayLabel;
    private final String title;
    private final String text;
    private final String source;
    private final String contentType;
    private final String book;
    private final String topic;
    private final String shortText;

    public HadithOfTheDay(
            String id,
            String dayLabel,
            String title,
            String text,
            String source,
            String contentType
    ) {
        this(id, dayLabel, title, text, source, contentType, "", "", "");
    }

    public HadithOfTheDay(
            String id,
            String dayLabel,
            String title,
            String text,
            String source,
            String contentType,
            String book,
            String topic,
            String shortText
    ) {
        this.id = id;
        this.dayLabel = dayLabel;
        this.title = title;
        this.text = text;
        this.source = source;
        this.contentType = contentType;
        this.book = book;
        this.topic = topic;
        this.shortText = shortText;
    }

    public String getId() {
        return id;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getSource() {
        return source;
    }

    public String getContentType() {
        return contentType;
    }

    public String getBook() {
        return book;
    }

    public String getTopic() {
        return topic;
    }

    public String getShortText() {
        return shortText;
    }
}
