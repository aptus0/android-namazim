package com.example.namazm.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "hadith_items")
public class HadithEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private final String id;

    @ColumnInfo(name = "title")
    private final String title;

    @ColumnInfo(name = "text")
    private final String text;

    @ColumnInfo(name = "source")
    private final String source;

    @ColumnInfo(name = "content_type")
    private final String contentType;

    @ColumnInfo(name = "book")
    private final String book;

    @ColumnInfo(name = "topic")
    private final String topic;

    @ColumnInfo(name = "short_text")
    private final String shortText;

    public HadithEntity(
            @NonNull String id,
            String title,
            String text,
            String source,
            String contentType,
            String book,
            String topic,
            String shortText
    ) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.source = source;
        this.contentType = contentType;
        this.book = book;
        this.topic = topic;
        this.shortText = shortText;
    }

    @NonNull
    public String getId() {
        return id;
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
