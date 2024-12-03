package org.neu;

import org.junit.Test;

import java.util.List;
import java.util.ArrayList;
import java.net.MalformedURLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.*;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

import org.neu.neo4j.Neo4jTransactionHandler;

public class CrawlerTest {

    @BeforeEach
    void setUp() {
        mockitoCloseable = MockitoAnnotations.openMocks(this);
        crawler = Crawler.getInstance();
        crawler.init();
    }

    @AfterEach
    void tearDown() throws Exception {
        crawler.close();
        mockitoCloseable.close();
    }

    @Test
    public void testGetInstance() {
        Crawler instance1 = Crawler.getInstance();
        Crawler instance2 = Crawler.getInstance();
        assertSame(instance1, instance2, "Crawler.getInstance should return the same instance");
    }

    @Test
    public void testGrepHyperLinks() {
        List<String> links = new ArrayList<>();
        String html = "<a href=\"http://example.com\">Link</a>" +
                "<a href=\"http://test.com/\">Test</a>" +
                "<a href=\"https://invalid\">Invalid</a>";

        Crawler.grepHyperLinks(links, html);

        assertEquals(3, links.size(), "Should extract valid URLs");
        assertTrue(links.contains("http://example.com"));
        assertTrue(links.contains("http://test.com"));
    }

    @Test
    public void testGrepHyperLinksWithEmptyInput() {
        List<String> links = new ArrayList<>();
        Crawler.grepHyperLinks(links, "");
        assertTrue(links.isEmpty(), "Should handle empty input");
    }

    @Test
    public void testGrepHyperLinksWithNoLinks() {
        List<String> links = new ArrayList<>();
        String html = "<p>No links here</p>";
        Crawler.grepHyperLinks(links, html);
        assertTrue(links.isEmpty(), "Should handle HTML with no links");
    }

    @Test
    public void testGrepHyperLinksWithLongURL() {
        List<String> links = new ArrayList<>();
        String longUrl = "http://example.com/" + "a".repeat(1000);
        String html = "<a href=\"" + longUrl + "\">Long Link</a>";

        Crawler.grepHyperLinks(links, html);
        assertTrue(links.isEmpty(), "Should not add URLs longer than 1000 characters");
    }

    @Test
    public void testProcessURLAsync() throws ExecutionException, InterruptedException, MalformedURLException {
        String testUrl = "http://example.com";
        crawler = Crawler.getInstance();
        crawler.init();  // instantiates the ExecutorService along with the DB
        CompletableFuture<List<String>> futureResults = crawler.processURLAsync(testUrl);

        List<String> results = futureResults.get();
        assertNotNull(results, "Should return a non-null list");
    }

    @Test
    public void testWouldCreateCycle() {
        String parentUrl = "https://google.com";
        String childUrl = "https://youtube.com";

        crawler = Crawler.getInstance();
        crawler.init();
//            crawler.run("http://example.com");

        crawler.childToParent.put(childUrl, parentUrl);

        boolean isCycle = crawler.wouldCreateCycle(childUrl, parentUrl);

        assertTrue(isCycle, "Should create a cycle");
    }

    @Test
    public void testRunWithInvalidURL() {
        // Note: java.net.URL will throw MalformedURLException if the URL/URI violates the RFC2396

        // Note2: we are using assertDoesNotThrow instead of assertThrows because since we are sanitizing (filtering) links, an invalid link
        // would not throw exception, just get ignored

        assertDoesNotThrow(() -> {
            crawler = Crawler.getInstance();
            crawler.init();
            crawler.run("htp:/invalid-url\\");
        });
    }

    private Crawler crawler;
    private AutoCloseable mockitoCloseable;

    @Mock
    private Neo4jTransactionHandler mockDb;
}
