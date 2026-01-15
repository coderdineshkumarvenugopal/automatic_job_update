package com.jobupdater.scraper;

import com.jobupdater.Job;
import java.util.List;

public interface ScraperStrategy {
    List<Job> scrape();

    String getSourceName();
}
