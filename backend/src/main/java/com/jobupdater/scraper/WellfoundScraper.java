package com.jobupdater.scraper;

import com.jobupdater.Job;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class WellfoundScraper extends BaseSeleniumScraper {

    private static final String BASE_URL = "https://wellfound.com/jobs?q=";
    private static final List<String> KEYWORDS = Arrays.asList(
            "Software Engineer",
            "Frontend Developer",
            "Backend Developer",
            "Full Stack Engineer");

    @Override
    public List<Job> scrape() {
        List<Job> allJobs = new ArrayList<>();
        WebDriver driver = null;
        try {
            driver = createDriver();

            // Randomize keywords to avoid pattern detection
            List<String> targetKeywords = new ArrayList<>(KEYWORDS);
            java.util.Collections.shuffle(targetKeywords);
            // Only take top 2 to avoid long runtimes
            targetKeywords = targetKeywords.subList(0, Math.min(2, targetKeywords.size()));

            for (String keyword : targetKeywords) {
                try {
                    // URL Encode keyword properly if needed, but simple spaces usually work or need
                    // ' ' -> '+'
                    String searchUrl = BASE_URL + keyword.replace(" ", "+");
                    driver.get(searchUrl);

                    // Random sleep to mimic human behavior
                    Thread.sleep(5000 + new Random().nextInt(3000));

                    autoScroll(driver);

                    // Selectors based on analysis
                    // Container: div[class*="styles_result__"]
                    List<WebElement> cards = driver.findElements(By.cssSelector("div[class*='styles_result__']"));

                    System.out.println("Wellfound: Found " + cards.size() + " cards for keyword " + keyword);

                    for (WebElement card : cards) {
                        try {
                            Job job = new Job();
                            job.setSource(getSourceName());

                            // Title: a[href^="/jobs/"]
                            try {
                                WebElement titleEl = card.findElement(By.cssSelector("a[href^='/jobs/']"));
                                job.setTitle(titleEl.getText());
                                String link = titleEl.getAttribute("href");
                                if (link != null && !link.startsWith("http")) {
                                    link = "https://wellfound.com" + link;
                                }
                                job.setUrl(link);
                            } catch (Exception e) {
                                // If title missing, skip
                                continue;
                            }

                            // Company: h2
                            try {
                                WebElement companyEl = card.findElement(By.tagName("h2"));
                                job.setCompany(companyEl.getText());
                            } catch (Exception e) {
                                job.setCompany("Unknown Company");
                            }

                            // Location: Text analysis
                            // Look for text containing "Remote", "In office", "Hybrid" or '•'
                            try {
                                String location = "Unknown Location";
                                List<WebElement> atomicElements = card.findElements(By.cssSelector("span, div"));
                                for (WebElement el : atomicElements) {
                                    String text = el.getText();
                                    if (text != null && (text.contains("Remote") || text.contains("In office")
                                            || text.contains("Hybrid") || text.contains("•"))) {
                                        location = text;
                                        break;
                                    }
                                }
                                job.setLocation(location);
                            } catch (Exception e) {
                                job.setLocation("Remote / Unspecified");
                            }

                            job.setDescription("View on Wellfound - " + keyword);
                            job.setPostedAt(java.time.LocalDateTime.now());

                            if (job.getTitle() != null && !job.getTitle().isEmpty()) {
                                allJobs.add(job);
                            }
                        } catch (Exception e) {
                            // Skip bad card
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Error search Wellfound keyword " + keyword + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error scraping Wellfound: " + e.getMessage());
        } finally {
            quitDriver(driver);
        }
        return allJobs;
    }

    @Override
    public String getSourceName() {
        return "Wellfound";
    }
}
