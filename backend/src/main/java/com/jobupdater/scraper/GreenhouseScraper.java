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
public class GreenhouseScraper extends BaseSeleniumScraper {

    // List of companies using Greenhouse to scrape
    private static final List<String> TARGET_BOARDS = Arrays.asList(
            // Global / US Tech
            "stripe", "twitch", "airbnb", "uber", "doordash", "dropbox",
            "pinterest", "lyft", "slack", "robinhood", "coinbase", "reddit",
            "cloudflare", "gitlab", "hashicorp", "databricks", "confluent",
            "classpass", "eventbrite", "foursquare", "github", "gusto",
            "instacart", "khanacademy", "kickstarter", "medium", "mozillacorporation",
            "okta", "pagerduty", "peloton", "quora", "seatgeek", "shopify",
            "snapchat", "spacex", "spotify", "square", "surveymonkey",
            "twilio", "udemy", "vimeo", "wayfair", "yelp", "zendesk", "zoom",

            // Indian Tech / Startups
            "razorpay", "cred", "grammarly", "mpl", "groww", "zerodha",
            "dream11", "pharmeasy", "slice", "unacademy", "meesho",
            "browserstack", "chargebee", "postman", "freshworks",
            "urbancompany", "lenskart", "cars24", "zeta", "dunzo");

    @Override
    public List<Job> scrape() {
        List<Job> allJobs = new ArrayList<>();
        WebDriver driver = null;
        try {
            driver = createDriver();

            for (String board : TARGET_BOARDS) {
                try {
                    String url = "https://boards.greenhouse.io/" + board;
                    System.out.println("Scraping Greenhouse board: " + board);

                    driver.get(url);
                    Thread.sleep(3000);

                    // Greenhouse usually lists jobs in sections
                    // Selector for individual job rows
                    List<WebElement> rows = driver.findElements(By.cssSelector("div.opening"));

                    if (rows.isEmpty()) {
                        // Try alternate selector
                        rows = driver.findElements(By.cssSelector("section.level-0 tr"));
                    }

                    System.out.println("Found " + rows.size() + " jobs for " + board);

                    for (WebElement row : rows) {
                        try {
                            Job job = new Job();
                            job.setSource("Carrier Site (" + board + ")"); // e.g. "Carrier Site (stripe)"
                            job.setCompany(board.substring(0, 1).toUpperCase() + board.substring(1)); // Capitalize

                            WebElement link;
                            try {
                                link = row.findElement(By.tagName("a"));
                            } catch (Exception e) {
                                continue;
                            }

                            job.setTitle(link.getText());
                            job.setUrl(link.getAttribute("href"));

                            // Location
                            try {
                                WebElement loc = row.findElement(By.cssSelector("span.location"));
                                job.setLocation(loc.getText());
                            } catch (Exception e) {
                                job.setLocation("Remote / See Details");
                            }

                            job.setDescription("Apply directly on company career site.");
                            // Since Greenhouse doesn't always show dates on the main board,
                            // treat it as "Just Posted" (Current Time) so it floats to top of feed.
                            job.setPostedAt(java.time.LocalDateTime.now());

                            // Filter for "Software" or "Engineering" if needed,
                            // or just take all since these are specific company boards.
                            // For now, let's take all to populate the feed, user can filter.
                            if (job.getTitle().toLowerCase().contains("software") ||
                                    job.getTitle().toLowerCase().contains("engineer") ||
                                    job.getTitle().toLowerCase().contains("developer") ||
                                    job.getTitle().toLowerCase().contains("tech")) {
                                allJobs.add(job);
                            }

                        } catch (Exception e) {
                            // Skip row
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error scraping board " + board + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error in Greenhouse scraper: " + e.getMessage());
        } finally {
            quitDriver(driver);
        }
        return allJobs;
    }

    @Override
    public String getSourceName() {
        return "Career Sites (Greenhouse)";
    }
}
