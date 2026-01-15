package com.jobupdater.scraper;

import com.jobupdater.Job;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class LeverScraper extends BaseSeleniumScraper {

    // List of companies using Lever to scrape
    private static final List<String> TARGET_BOARDS = Arrays.asList(
            "sliceit", // Fintech Slice
            "amazon",
            "google",
            "microsoft",
            "meta",
            "apple",
            "flipkart",
            "netflix",
            "palantir",
            "figma",
            "notion",
            "asana",
            "digitalocean",
            "stack-overflow",
            "atlassian",
            "twilio",
            "auth0",
            "deliveroo",
            "grab",
            "go-jek",
            "byjus",
            "swiggy",
            "hotstar",
            "dream11",
            "pocket-aces",
            "zeta");

    @Override
    public List<Job> scrape() {
        List<Job> allJobs = new ArrayList<>();
        WebDriver driver = null;
        try {
            driver = createDriver();

            for (String board : TARGET_BOARDS) {
                try {
                    String url = "https://jobs.lever.co/" + board;
                    System.out.println("Scraping Lever board: " + board);

                    driver.get(url);
                    Thread.sleep(3000);

                    // Lever selector for job postings
                    List<WebElement> rows = driver.findElements(By.cssSelector("div.posting"));

                    System.out.println("Found " + rows.size() + " jobs for " + board);

                    for (WebElement row : rows) {
                        try {
                            Job job = new Job();
                            job.setSource("Career Site (Lever)");
                            job.setCompany(board.substring(0, 1).toUpperCase() + board.substring(1));

                            WebElement titleEl = row.findElement(By.cssSelector("a.posting-title h5"));
                            WebElement linkEl = row.findElement(By.cssSelector("a.posting-btn-submit"));

                            // Sometimes the link is on the title itself
                            if (linkEl == null) {
                                linkEl = row.findElement(By.cssSelector("a.posting-title"));
                            }

                            job.setTitle(titleEl.getText());
                            job.setUrl(linkEl.getAttribute("href"));

                            // Location
                            try {
                                WebElement loc = row.findElement(By.cssSelector("span.location"));
                                job.setLocation(loc.getText());
                            } catch (Exception e) {
                                job.setLocation("Remote / See Details");
                            }

                            job.setDescription("Apply directly on official career site.");
                            job.setPostedAt(java.time.LocalDateTime.now());

                            // Basic tech filter
                            String title = job.getTitle().toLowerCase();
                            if (title.contains("software") || title.contains("engineer") ||
                                    title.contains("developer") || title.contains("tech") ||
                                    title.contains("data") || title.contains("backend") ||
                                    title.contains("frontend") || title.contains("fullstack")) {
                                allJobs.add(job);
                            }

                        } catch (Exception e) {
                            // Skip row
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error scraping Lever board " + board + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error in Lever scraper: " + e.getMessage());
        } finally {
            quitDriver(driver);
        }
        return allJobs;
    }

    @Override
    public String getSourceName() {
        return "Career Sites (Lever)";
    }
}
