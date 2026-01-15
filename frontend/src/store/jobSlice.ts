import { createSlice } from '@reduxjs/toolkit';
import type { PayloadAction } from '@reduxjs/toolkit';

export interface Job {
    id: number;
    title: string;
    company: string;
    location: string;
    description: string;
    url: string;
    source: string;
    status: 'NEW' | 'APPLIED' | 'DRAFT';
    postedAt: string;
}

interface JobState {
    jobs: Job[];
}

const initialState: JobState = {
    jobs: [],
};

const jobSlice = createSlice({
    name: 'jobs',
    initialState,
    reducers: {
        setJobs: (state, action: PayloadAction<Job[]>) => {
            state.jobs = action.payload;
        },
        addJobs: (state, action: PayloadAction<Job[]>) => {
            // Append new jobs, filtering duplicates by ID
            const newJobs = action.payload.filter(
                (newJob) => !state.jobs.some((existingJob) => existingJob.id === newJob.id)
            );
            state.jobs = [...newJobs, ...state.jobs];
        },
        updateJobStatus: (state, action: PayloadAction<{ id: number, status: 'NEW' | 'APPLIED' | 'DRAFT' }>) => {
            const job = state.jobs.find(j => j.id === action.payload.id);
            if (job) {
                job.status = action.payload.status;
            }
        },
    },
});

export const { setJobs, addJobs, updateJobStatus } = jobSlice.actions;
export default jobSlice.reducer;
