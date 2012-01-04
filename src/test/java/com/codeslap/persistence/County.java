package com.codeslap.persistence;

import java.util.List;

public class County {
    private long id;
    private String county;
    private List<Feed> feeds;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public List<Feed> getFeeds() {
        return feeds;
    }

    public void setFeeds(List<Feed> feeds) {
        this.feeds = feeds;
    }

    @Override
    public String toString() {
        return "County{" +
                "id=" + id +
                ", county='" + county + '\'' +
                ", feeds=" + feeds +
                '}';
    }
}
