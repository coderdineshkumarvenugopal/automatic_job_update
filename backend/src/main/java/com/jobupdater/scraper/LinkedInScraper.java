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
public class LinkedInScraper extends BaseSeleniumScraper {

    private static final String BASE_URL = "https://www.linkedin.com/jobs/search?keywords=";
    private static final String SUFFIX_URL = "&location=India&geoId=102713980";

    private static final List<String> KEYWORDS = Arrays.asList(
            "Software Engineer Fresher India",
            "Software Engineer Graduate India",
            "Software Engineer Intern India",
            "SDE 1 India",
            "Junior Software Engineer India",
            "Graduate Engineer Trainee India",
            "Google SDE Graduate India",
            "Amazon SDE 1 India",
            "Flipkart SDE India",
            "Meesho Software Engineer India",
            "Microsoft SDE India",
            "Apple Software Engineer India",
            "Netflix Software Engineer India",
            "Adobe Software Engineer India",
            "Oracle SDE India",
            "Cisco Software Engineer India",
            "NVIDIA Software Engineer India");

    @Override
    public List<Job> scrape() {
        List<Job> allJobs = new ArrayList<>();
        WebDriver driver = null;
        try {
            driver = createDriver();

            // Scrape 2-3 random keywords each cycle to avoid hitting limits too fast
            java.util.Collections.shuffle(new ArrayList<>(KEYWORDS));
            List<String> targetKeywords = KEYWORDS.subList(0, Math.min(3, KEYWORDS.size()));

            for (String keyword : targetKeywords) {
                try {
                    String searchUrl = BASE_URL + keyword.replace(" ", "%20") + SUFFIX_URL;
                    System.out.println("Scraping LinkedIn for: " + keyword);
                    driver.get(searchUrl);

                    // Wait for list to load
                    Thread.sleep(4000);
                    autoScroll(driver);

                    List<WebElement> jobCards = driver.findElements(By.cssSelector("ul.jobs-search__results-list li"));
                    System.out.println("Found " + jobCards.size() + " cards for " + keyword);

                    for (WebElement card : jobCards) {
                        try {
                            Job job = new Job();
                            job.setSource(getSourceName());

                            // Title
                            String title = "";
                            try {
                                WebElement titleEl = card.findElement(By.cssSelector("h3.base-search-card__title"));
                                title = titleEl.getText().trim();
                            } catch (Exception e) {
                            }
                            if (title.isEmpty())
                                continue;
                            job.setTitle(title);

                            // Company
                            String company = "";
                            try {
                                WebElement companyEl = card
                                        .findElement(By.cssSelector("h4.base-search-card__subtitle"));
                                company = companyEl.getText().trim();
                            } catch (Exception e) {
                            }
                            job.setCompany(company.isEmpty() ? "Unknown Company" : company);

                            // Location
                            try {
                                WebElement locEl = card.findElement(By.cssSelector("span.job-search-card__location"));
                                job.setLocation(locEl.getText().trim());
                            } catch (Exception e) {
                                job.setLocation("India");
                            }

                            // Link
                            WebElement linkEl = card.findElement(By.cssSelector("a.base-card__full-link"));
                            job.setUrl(linkEl.getAttribute("href"));
                            job.setDescription("View on LinkedIn - " + keyword);
                            job.setPostedAt(java.time.LocalDateTime.now());

                            if (job.getUrl() != null && !job.getUrl().isEmpty()) {
                                allJobs.add(job);
                            }
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error search keyword " + keyword + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error scraping LinkedIn: " + e.getMessage());
        } finally {
            quitDriver(driver);
        }
        return allJobs;
    }

    @Override
    public String getSourceName() {
        return "LinkedIn";
    }
}
