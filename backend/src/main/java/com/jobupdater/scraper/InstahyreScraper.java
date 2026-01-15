package com.jobupdater.scraper;

import com.jobupdater.Job;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InstahyreScraper extends BaseSeleniumScraper {

    // Instahyre Java Software Engineer Search (Bangalore/Remote/India)
    private static final String SEARCH_URL = "https://www.instahyre.com/search-jobs/?string=Software%20Engineer&location=India";

    @Override
    public List<Job> scrape() {
        List<Job> jobs = new ArrayList<>();
        WebDriver driver = null;
        try {
            driver = createDriver();
            driver.get(SEARCH_URL);
            Thread.sleep(5000); // Wait for load

            // Scroll a bit
            autoScroll(driver);

            // Instahyre job card selector
            List<WebElement> cards = driver
                    .findElements(By.cssSelector("div.job-row, div[id^='job-row'], div.employer-job-card"));
            System.out.println("Instahyre Scraper found " + cards.size() + " cards.");

            for (WebElement card : cards) {
                try {
                    Job job = new Job();
                    job.setSource(getSourceName());

                    // Title
                    WebElement titleEl = card.findElement(By.cssSelector("a.position-link"));
                    job.setTitle(titleEl.getText());
                    job.setUrl(titleEl.getAttribute("href"));

                    // Company
                    WebElement companyEl = card.findElement(By.cssSelector("a.company-name"));
                    job.setCompany(companyEl.getText());

                    // Location - usually in a span class 'job-location' or similar
                    try {
                        WebElement locEl = card.findElement(By.cssSelector("span.job-location"));
                        job.setLocation(locEl.getText());
                    } catch (Exception e) {
                        job.setLocation("India (See Details)");
                    }

                    job.setDescription("View on Instahyre");

                    jobs.add(job);
                } catch (Exception e) {
                    // Skip
                }
            }

        } catch (Exception e) {
            System.err.println("Error scraping Instahyre: " + e.getMessage());
        } finally {
            quitDriver(driver);
        }
        return jobs;
    }

    @Override
    public String getSourceName() {
        return "Instahyre";
    }
}
