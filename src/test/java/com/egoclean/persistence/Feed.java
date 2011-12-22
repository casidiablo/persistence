package com.egoclean.persistence;

import java.util.List;

public class Feed {
    private long id;
    private String feed;
    private List<County> counties;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFeed() {
        return feed;
    }

    public void setFeed(String feed) {
        this.feed = feed;
    }

    public List<County> getCounties() {
        return counties;
    }

    public void setCounties(List<County> counties) {
        this.counties = counties;
    }

    @Override
    public String toString() {
        return "Feed{" +
                "id=" + id +
                ", feed='" + feed + '\'' +
                ", counties=" + counties +
                '}';
    }
}
