package com.jobupdater;

import com.jobupdater.scraper.*;
import com.jobupdater.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobScraperService {

    private final JobRepository jobRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final List<ScraperStrategy> scrapers;
    private final java.util.concurrent.ExecutorService scraperExecutor = java.util.concurrent.Executors
            .newFixedThreadPool(3);
    private int currentScraperIndex = 0;

    @Autowired
    public JobScraperService(JobRepository jobRepository,
            SimpMessagingTemplate messagingTemplate,
            LinkedInScraper linkedInScraper,
            IndeedScraper indeedScraper,
            NaukriScraper naukriScraper,
            GlassdoorScraper glassdoorScraper,
            InstahyreScraper instahyreScraper,
            GreenhouseScraper greenhouseScraper,
            LeverScraper leverScraper,
            WellfoundScraper wellfoundScraper) {
        this.jobRepository = jobRepository;
        this.messagingTemplate = messagingTemplate;
        this.scrapers = new ArrayList<>();
        this.scrapers.add(linkedInScraper);
        this.scrapers.add(indeedScraper);
        this.scrapers.add(naukriScraper);
        this.scrapers.add(glassdoorScraper);
        this.scrapers.add(instahyreScraper);
        this.scrapers.add(greenhouseScraper);
        this.scrapers.add(leverScraper);
        this.scrapers.add(wellfoundScraper);
    }

    @PostConstruct
    public void init() {
        System.out.println("Starting initial scraping cycle on startup...");
        scrapeJobs();
    }

    // Scrape every 15 minutes (900000ms) to prevent ban and server crash
    @Scheduled(fixedRate = 900000)
    public void scrapeJobs() {
        System.out.println("Starting advanced scraping cycle...");
        for (ScraperStrategy scraper : scrapers) {
            scraperExecutor.submit(() -> {
                System.out.println("Running scraper: " + scraper.getSourceName());
                try {
                    List<Job> jobs = scraper.scrape();
                    for (Job job : jobs) {
                        if (isValidJob(job)) {
                            // Normalize URL
                            if (job.getUrl() != null && job.getUrl().contains("?")) {
                                job.setUrl(job.getUrl().split("\\?")[0]);
                            }

                            boolean existsByUrl = jobRepository.existsByUrl(job.getUrl());
                            boolean existsByContent = jobRepository.existsByTitleAndCompany(job.getTitle(),
                                    job.getCompany());

                            if (!existsByUrl && !existsByContent) {
                                jobRepository.save(job);
                            }
                        }
                    }
                    if (!jobs.isEmpty()) {
                        System.out.println("Saved " + jobs.size() + " jobs from " + scraper.getSourceName());
                        // Broadcast update
                        messagingTemplate.convertAndSend("/topic/jobs", jobs);
                    }
                } catch (Exception e) {
                    System.err.println("Error running scraper " + scraper.getSourceName() + ": " + e.getMessage());
                }
            });
        }
    }

    // Live Heartbeat every 1 second to satisfy "real-time" requirement
    @Scheduled(fixedRate = 1000)
    public void sendStatusUpdate() {
        if (scrapers.isEmpty())
            return;
        String source = scrapers.get(currentScraperIndex).getSourceName();
        messagingTemplate.convertAndSend("/topic/status", "Scanning " + source + "...");
        currentScraperIndex = (currentScraperIndex + 1) % scrapers.size();
    }

    private boolean isValidJob(Job job) {
        if (job.getTitle() == null || job.getUrl() == null || job.getCompany() == null)
            return false;

        String title = job.getTitle().trim();
        String company = job.getCompany().trim();

        if (title.isEmpty() || title.equals("Unknown Title"))
            return false;

        // Reject masked content (often from LinkedIn/Naukri premium or rate limit)
        if (title.contains("*******") || company.contains("*******"))
            return false;

        String text = (title + " " + job.getDescription() + " " + company).toLowerCase();

        // Filter out obvious ads or promoted content
        if (text.contains("promoted") || text.contains("sponsored") || text.contains("advertisement")) {
            return false;
        }
        return true;
    }
}
