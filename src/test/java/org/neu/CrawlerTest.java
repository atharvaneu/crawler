package org.neu;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CrawlerTest {

    @Test
    public void testGrepHyperLinksWithNoLinks() {
        List<String> links = new ArrayList<>();
        String html = "This is a test string with no hyperlinks";
        Crawler.grepHyperLinks(links, html);
        assertTrue(links.isEmpty());
    }

    @Test
    public void testGrepHyperLinksSingleLink() {
        List<String> links = new ArrayList<>();
        String html = "<a href=\"https://example.com\">";
        Crawler.grepHyperLinks(links, html);
        assertEquals(1, links.size());
        assertEquals("https://example.com", links.get(0));
    }

    @Test
    public void testGrepHyperLinksMultipleLinks() {
        List<String> links = new ArrayList<>();
        String html = "<a href=\"http://test1.com\"> <a href=\"https://test2.com\">";
        Crawler.grepHyperLinks(links, html);
        assertEquals(2, links.size());
        assertEquals("http://test1.com", links.get(0));
        assertEquals("https://test2.com", links.get(1));
    }
}
