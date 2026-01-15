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
public class NaukriScraper extends BaseSeleniumScraper {

    private static final String BASE_URL = "https://www.naukri.com/";
    private static final List<String> KEYWORDS = Arrays.asList(
            "software-engineer-fresher-jobs",
            "software-engineer-graduate-jobs",
            "google-jobs",
            "amazon-jobs",
            "flipkart-jobs",
            "meesho-jobs",
            "fresher-jobs-in-india");

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
                    String url = BASE_URL + keyword;
                    driver.get(url);
                    Thread.sleep(5000);
                    autoScroll(driver);

                    List<WebElement> cards = driver.findElements(By.cssSelector("div.srp-jobtuple-wrapper"));

                    for (WebElement card : cards) {
                        try {
                            Job job = new Job();
                            job.setSource(getSourceName());

                            WebElement titleEl = card.findElement(By.cssSelector("a.title"));
                            job.setTitle(titleEl.getText());
                            job.setUrl(titleEl.getAttribute("href"));

                            WebElement companyEl = card.findElement(By.cssSelector("a.comp-name"));
                            job.setCompany(companyEl.getText());

                            WebElement locEl = card.findElement(By.cssSelector("span.locWdth"));
                            job.setLocation(locEl.getText());

                            job.setDescription("View on Naukri - " + keyword);
                            job.setPostedAt(java.time.LocalDateTime.now());

                            allJobs.add(job);
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error search Naukri keyword " + keyword + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error scraping Naukri: " + e.getMessage());
        } finally {
            quitDriver(driver);
        }
        return allJobs;
    }

    @Override
    public String getSourceName() {
        return "Naukri";
    }
}
