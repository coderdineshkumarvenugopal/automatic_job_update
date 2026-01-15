package com.jobupdater.scraper;

import com.jobupdater.Job;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class IndeedScraper extends BaseSeleniumScraper {

    private static final String BASE_URL = "https://in.indeed.com/jobs?q=";
    private static final String SUFFIX_URL = "&l=India";

    private static final List<String> KEYWORDS = Arrays.asList(
            "software+engineer+fresher",
            "software+engineer+graduate",
            "google+software+engineer",
            "amazon+software+engineer",
            "flipkart+software+engineer",
            "meesho+software+engineer",
            "intern+software+engineer");

    @Override
    public List<Job> scrape() {
        List<Job> allJobs = new ArrayList<>();
        WebDriver driver = null;
        try {
            driver = createDriver();

            // Rotate keywords
            java.util.Collections.shuffle(new ArrayList<>(KEYWORDS));
            List<String> targetKeywords = KEYWORDS.subList(0, Math.min(2, KEYWORDS.size()));

            for (String keyword : targetKeywords) {
                try {
                    String url = BASE_URL + keyword + SUFFIX_URL;
                    driver.get(url);
                    Thread.sleep(5000);
                    autoScroll(driver);

                    // Improved Indeed Selectors
                    // Try standard 'job_seen_beacon' widely used by Indeed
                    List<WebElement> cards = driver
                            .findElements(By.cssSelector("div.job_seen_beacon, td.resultContent, li.css-5lfssm"));

                    for (WebElement card : cards) {
                        try {
                            Job job = new Job();
                            job.setSource(getSourceName());

                            // Title: usually inside h2.jobTitle
                            try {
                                WebElement titleEl = card
                                        .findElement(By.cssSelector("h2.jobTitle span[title], a[data-jk]"));
                                job.setTitle(titleEl.getText().isEmpty() ? titleEl.getAttribute("title")
                                        : titleEl.getText());
                            } catch (Exception e) {
                                // Fallback
                                WebElement titleEl = card.findElement(By.cssSelector("h2.jobTitle"));
                                job.setTitle(titleEl.getText());
                            }
                            if (job.getTitle() == null || job.getTitle().isEmpty())
                                continue;

                            // Company
                            try {
                                WebElement companyEl = card
                                        .findElement(By.cssSelector("span[data-testid='company-name']"));
                                job.setCompany(companyEl.getText());
                            } catch (Exception e) {
                                job.setCompany("Indeed Company");
                            }

                            // Location
                            try {
                                WebElement locEl = card.findElement(By.cssSelector("div[data-testid='text-location']"));
                                job.setLocation(locEl.getText());
                            } catch (Exception e) {
                                job.setLocation("India");
                            }

                            // Link
                            try {
                                WebElement linkEl = card.findElement(By.cssSelector("a.jcs-JobTitle, a[data-jk]"));
                                job.setUrl(linkEl.getAttribute("href"));
                            } catch (Exception e) {
                                // Sometimes the card itself or a parent anchor is the link, but tricky.
                                // Construct from data-jk if possible
                                String jk = card.getAttribute("data-jk");
                                if (jk != null) {
                                    job.setUrl("https://www.indeed.com/viewjob?jk=" + jk);
                                } else {
                                    continue; // Skip if no link
                                }
                            }

                            job.setDescription("View on Indeed - " + keyword);
                            job.setPostedAt(java.time.LocalDateTime.now());

                            allJobs.add(job);
                        } catch (Exception e) {
                            // Skip
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error search Indeed keyword " + keyword + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error scraping Indeed: " + e.getMessage());
        } finally {
            quitDriver(driver);
        }
        return allJobs;
    }

    @Override
    public String getSourceName() {
        return "Indeed";
    }
}
