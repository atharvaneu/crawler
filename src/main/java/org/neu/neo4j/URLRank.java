package org.neu.neo4j;

public class URLRank {
    private final String url;
    private final int inDegree;

    public URLRank(String url, int inDegree) {
        this.url = url;
        this.inDegree = inDegree;
    }

    public String getUrl() {
        return url;
    }

    public int getInDegree() {
        return inDegree;
    }

    @Override
    public String toString() {
        return String.format("URL: %s (Referenced by %d pages)", url, inDegree);
    }
}
