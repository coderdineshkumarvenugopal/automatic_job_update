package com.jobupdater;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*") // Allow all for dev
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @GetMapping
    public List<Job> getAllJobs() {
        System.out.println("Received request to fetch all jobs");
        List<Job> jobs = jobRepository.findAll();
        System.out.println("Returning " + jobs.size() + " jobs");
        return jobs;
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/status")
    public Job updateStatus(@org.springframework.web.bind.annotation.PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestBody String status) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new RuntimeException("Job not found"));
        job.setStatus(status);
        return jobRepository.save(job);
    }
}
