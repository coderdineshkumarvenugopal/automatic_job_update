package com.jobupdater.scraper;

import com.jobupdater.Job;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GlassdoorScraper extends BaseSeleniumScraper {

    // Glassdoor India Software Engineer
    private static final String SEARCH_URL = "https://www.glassdoor.co.in/Job/india-software-engineer-jobs-SRCH_IL.0,5_IN115_KO6,23.htm";

    @Override
    public List<Job> scrape() {
        List<Job> jobs = new ArrayList<>();
        WebDriver driver = null;
        try {
            driver = createDriver();
            driver.get(SEARCH_URL);
            Thread.sleep(5000); // Wait for load

            // Close popup if it appears (often appears on scroll)
            try {
                WebElement closeButton = driver.findElement(By.cssSelector(".modal_closeIcon, button.CloseButton"));
                if (closeButton.isDisplayed()) {
                    closeButton.click();
                }
            } catch (Exception ignored) {
            }

            // Glassdoor selectors - Try multiple
            List<WebElement> cards = driver.findElements(
                    By.cssSelector("li[data-test='jobListing'], li.react-job-listing, li[class*='react-job-listing']"));
            System.out.println("Glassdoor Scraper found " + cards.size() + " cards.");

            for (WebElement card : cards) {
                try {
                    Job job = new Job();
                    job.setSource(getSourceName());

                    // Company
                    try {
                        WebElement companyEl = card
                                .findElement(By.cssSelector("div.employer-name, div[class*='EmployerProfile']"));
                        job.setCompany(companyEl.getText());
                    } catch (Exception e) {
                        job.setCompany("Glassdoor Company");
                    }

                    // Title & Link
                    WebElement titleEl = card.findElement(By.cssSelector("a[data-test='job-link']"));
                    job.setTitle(titleEl.getText());
                    job.setUrl(titleEl.getAttribute("href"));

                    // Location
                    try {
                        WebElement locEl = card.findElement(By.cssSelector("div[data-test='emp-location']"));
                        job.setLocation(locEl.getText());
                    } catch (Exception e) {
                        job.setLocation("India");
                    }

                    job.setDescription("View on Glassdoor");

                    jobs.add(job);
                } catch (Exception e) {
                    // Skip invalid cards
                }
            }

        } catch (Exception e) {
            System.err.println("Error scraping Glassdoor: " + e.getMessage());
        } finally {
            quitDriver(driver);
        }
        return jobs;
    }

    @Override
    public String getSourceName() {
        return "Glassdoor";
    }
}
