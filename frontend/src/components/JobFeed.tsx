import React, { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
import { useDispatch, useSelector } from 'react-redux';
import type { RootState } from '../store/store';
import { setJobs, addJobs, updateJobStatus } from '../store/jobSlice';
import type { Job } from '../store/jobSlice';
import { Card, List, Tag, Button, Spin, Badge, Typography, Space, Input, Select, Tabs, Modal } from 'antd';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { ExternalLink, Clock, MapPin, Building2 } from 'lucide-react';

const { Text, Title } = Typography;

const API_URL = import.meta.env.VITE_API_URL || '';
console.log("Current API_URL:", API_URL || "(empty, using relative path)");

const fetchJobs = async (): Promise<Job[]> => {
    const response = await axios.get(`${API_URL}/api/jobs`);
    return response.data;
};

const timeAgo = (dateString: string) => {
    if (!dateString) return 'Just now';
    try {
        const seconds = Math.floor((new Date().getTime() - new Date(dateString).getTime()) / 1000);
        if (isNaN(seconds)) return 'Just now';

        let interval = seconds / 31536000;
        if (interval > 1) return Math.floor(interval) + " years ago";
        interval = seconds / 2592000;
        if (interval > 1) return Math.floor(interval) + " months ago";
        interval = seconds / 86400;
        if (interval > 1) return Math.floor(interval) + " days ago";
        interval = seconds / 3600;
        if (interval > 1) return Math.floor(interval) + " hours ago";
        interval = seconds / 60;
        if (interval > 1) return Math.floor(interval) + " mins ago";
        if (seconds < 10) return "Just now";
        return Math.floor(seconds) + " secs ago";
    } catch (e) {
        return 'Recently';
    }
};

const JobFeed: React.FC = () => {
    const dispatch = useDispatch();
    const jobs = useSelector((state: RootState) => state.jobs.jobs);
    const [searchText, setSearchText] = useState('');
    const [selectedSources, setSelectedSources] = useState<string[]>([]);
    const [connected, setConnected] = useState(false);
    const [pendingJobs, setPendingJobs] = useState<Job[]>([]);
    const [scanningStatus, setScanningStatus] = useState('Initializing...');

    // Pagination State
    const [pageSize, setPageSize] = useState<number>(10);
    const [currentPage, setCurrentPage] = useState<number>(1);

    // Sort State
    const [sortOrder, setSortOrder] = useState<'date-desc' | 'date-asc' | 'company'>('date-desc');

    // Tab State
    const [activeTab, setActiveTab] = useState<'NEW' | 'APPLIED' | 'DRAFT'>('NEW');

    // Experience Filter State
    const [experienceFilter, setExperienceFilter] = useState<'ALL' | 'FRESHER' | 'EXPERIENCED'>('ALL');

    // Experience Detection Logic
    const detectExperience = (title: string, description: string): 'FRESHER' | 'EXPERIENCED' | 'UNKNOWN' => {
        const text = (title + " " + description).toLowerCase();
        const fresherKeywords = ['intern', 'internship', 'fresher', 'graduate', 'trainee', 'entry level', 'junior', '0-1 year', '0-2 years'];
        const experiencedKeywords = ['senior', 'lead', 'principal', 'manager', 'architect', 'head', 'years experience', 'mid-senior'];

        if (fresherKeywords.some(k => text.includes(k))) return 'FRESHER';
        if (experiencedKeywords.some(k => text.includes(k))) return 'EXPERIENCED';
        return 'UNKNOWN';
    };

    // Modal State
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [selectedJob, setSelectedJob] = useState<Job | null>(null);

    const handleApplyClick = (job: Job) => {
        setSelectedJob(job);
        window.open(job.url, '_blank');
        setIsModalOpen(true);
    };

    const handleModalResponse = async (status: 'APPLIED' | 'DRAFT' | 'CANCEL') => {
        if (selectedJob && status !== 'CANCEL') {
            try {
                await axios.put(`${API_URL}/api/jobs/${selectedJob.id}/status`, status, {
                    headers: { 'Content-Type': 'text/plain' }
                });
                dispatch(updateJobStatus({ id: selectedJob.id, status }));
            } catch (e) {
                console.error("Failed to update status", e);
            }
        }
        setIsModalOpen(false);
        setSelectedJob(null);
    };

    // Filter Logic
    const filteredJobs = jobs.filter(job => {
        // Status Filter (Tab)
        const jobStatus = job.status || 'NEW'; // Handle legacy jobs with no status
        if (jobStatus !== activeTab) return false;

        // Experience Filter
        const expLevel = detectExperience(job.title, job.description);
        if (experienceFilter === 'FRESHER' && expLevel !== 'FRESHER') return false;
        if (experienceFilter === 'EXPERIENCED' && expLevel !== 'EXPERIENCED') return false;
        // Note: 'ALL' includes UNKNOWN

        const matchesSearch = job.title.toLowerCase().includes(searchText.toLowerCase()) ||
            job.company.toLowerCase().includes(searchText.toLowerCase());
        const matchesSource = selectedSources.length === 0 || selectedSources.includes(job.source);
        return matchesSearch && matchesSource;
    }).sort((a, b) => {
        if (sortOrder === 'date-asc') {
            const dateA = new Date(a.postedAt).getTime();
            const dateB = new Date(b.postedAt).getTime();
            return (isNaN(dateA) ? 0 : dateA) - (isNaN(dateB) ? 0 : dateB);
        }
        if (sortOrder === 'company') return a.company.localeCompare(b.company);

        // Default: date-desc (Newest First)
        const dateA = new Date(a.postedAt).getTime();
        const dateB = new Date(b.postedAt).getTime();

        // If dates are equal or invalid, use ID as tiebreaker to show latest scraped
        if (dateA === dateB || isNaN(dateA) || isNaN(dateB)) {
            return b.id - a.id;
        }
        return dateB - dateA;
    });

    const availableSources = Array.from(new Set(jobs.map(job => job.source)));

    // ... (Hooks for useQuery, WebSocket use effect remain roughly same, kept connected logic below)

    // Initial Fetch
    const { data: initialJobs, isLoading, isError } = useQuery({
        queryKey: ['jobs'],
        queryFn: fetchJobs,
    });

    useEffect(() => {
        if (initialJobs) {
            dispatch(setJobs(initialJobs));
        }
    }, [initialJobs, dispatch]);

    // WebSocket Connection
    useEffect(() => {
        const socket = new SockJS(`${API_URL}/ws`);
        const client = new Client({
            webSocketFactory: () => socket,
            onConnect: () => {
                setConnected(true);
                client.subscribe('/topic/jobs', (message) => {
                    if (message.body) {
                        const newJobs: Job[] = JSON.parse(message.body);
                        // Buffer jobs instead of immediate update to avoid disturbing current list
                        setPendingJobs(prev => {
                            // Filter out jobs that are already in pending or in current jobs
                            const filteredNewJobs = newJobs.filter(newJob =>
                                !prev.some(p => p.url === newJob.url) &&
                                !jobs.some(j => j.url === newJob.url)
                            );
                            return [...prev, ...filteredNewJobs];
                        });
                    }
                });

                client.subscribe('/topic/status', (message) => {
                    if (message.body) {
                        setScanningStatus(message.body);
                    }
                });
            },
            onDisconnect: () => setConnected(false),
        });
        client.activate();
        return () => { client.deactivate(); };
    }, [dispatch]);

    if (isLoading) return <div className="flex justify-center p-12"><Spin size="large" /></div>;
    if (isError) return <div className="text-red-500 text-center p-12">Error fetching jobs. Backend might be down.</div>;

    const handleRefresh = () => {
        if (pendingJobs.length > 0) {
            dispatch(addJobs(pendingJobs));
            setPendingJobs([]);
        }
    };

    const renderJobItem = (job: Job) => {
        const expLevel = detectExperience(job.title, job.description);
        let expTagColor = 'default';
        if (expLevel === 'FRESHER') expTagColor = 'green';
        if (expLevel === 'EXPERIENCED') expTagColor = 'gold';

        return (
            <List.Item>
                <Badge.Ribbon text={job.source} color={job.source === 'LinkedIn' ? 'blue' : job.source === 'Naukri' ? 'orange' : 'purple'}>
                    <Card hoverable className="shadow-sm border-l-4 border-l-blue-500 rounded-lg">
                        <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                            <div className="flex-1">
                                <Title level={5} className="!mb-1">
                                    <a href={job.url} target="_blank" rel="noopener noreferrer" className="text-gray-800 hover:text-blue-600 transition-colors">
                                        {job.title}
                                    </a>
                                    {expLevel !== 'UNKNOWN' && <Tag color={expTagColor} className="ml-2 text-[10px] uppercase">{expLevel}</Tag>}
                                </Title>
                                <div className="flex flex-wrap gap-3 text-gray-500 mt-2">
                                    <Space size="small"><Building2 size={16} /><Text type="secondary">{job.company}</Text></Space>
                                    <Space size="small"><MapPin size={16} /><Text type="secondary">{job.location}</Text></Space>
                                    <Space size="small">
                                        <Clock size={16} />
                                        <Text type="success" strong style={{ fontSize: '0.8rem' }}>
                                            {timeAgo(job.postedAt)}
                                        </Text>
                                    </Space>
                                </div>
                            </div>
                            {(activeTab === 'NEW' || activeTab === 'DRAFT') && (
                                <Button type="primary" onClick={() => handleApplyClick(job)} icon={<ExternalLink size={16} />}>
                                    {activeTab === 'DRAFT' ? 'Continue Applying' : 'Apply Now'}
                                </Button>
                            )}
                            {/* Actions for other tabs could go here */}
                        </div>
                    </Card>
                </Badge.Ribbon>
            </List.Item>
        );
    };

    return (
        <div>
            {/* Header & Stats */}
            <div className="flex justify-between items-center mb-4">
                <Space>
                    <Title level={4} className="!mb-0">Job Dashboard</Title>
                    {connected ? (
                        <div className="flex items-center gap-2">
                            <Tag color="success" className="rounded-full px-3">Live</Tag>
                            <Text type="secondary" className="text-xs italic animate-pulse">
                                {scanningStatus}
                            </Text>
                        </div>
                    ) : <Tag color="warning" className="rounded-full px-3">Connecting...</Tag>}
                </Space>
                <div className="text-gray-500">
                    Total: {jobs.length} | Shown: {filteredJobs.length}
                </div>
            </div>

            {/* Floating Refresh Button */}
            {pendingJobs.length > 0 && (
                <div className="fixed top-24 left-1/2 transform -translate-x-1/2 z-50 animate-bounce">
                    <Button
                        type="primary"
                        size="large"
                        shape="round"
                        icon={<Badge count={pendingJobs.length} offset={[10, -5]} size="small">
                            <Clock size={16} />
                        </Badge>}
                        onClick={handleRefresh}
                        className="bg-blue-600 hover:bg-blue-700 shadow-xl border-none h-auto py-2 px-6"
                    >
                        <span className="ml-2 font-semibold">New Jobs Available - Click to Refresh</span>
                    </Button>
                </div>
            )}

            {/* Filters Area */}
            <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-100 flex flex-wrap gap-4 items-center mb-6">
                <Input.Search placeholder="Search..." allowClear onChange={(e) => setSearchText(e.target.value)} style={{ width: 250 }} />

                <Select mode="multiple" allowClear style={{ width: 200 }} placeholder="Platform" onChange={setSelectedSources} options={availableSources.map(s => ({ label: s, value: s }))} />

                <Select value={sortOrder} style={{ width: 180 }} onChange={setSortOrder} options={[
                    { label: 'Date: Newest First', value: 'date-desc' },
                    { label: 'Date: Oldest First', value: 'date-asc' },
                    { label: 'Company Name', value: 'company' },
                ]} />

                <Select value={experienceFilter} style={{ width: 180 }} onChange={setExperienceFilter} options={[
                    { label: 'All Experiences', value: 'ALL' },
                    { label: 'Fresher / Entry', value: 'FRESHER' },
                    { label: 'Experienced', value: 'EXPERIENCED' },
                ]} />
            </div>

            {/* Tabs & List */}
            <Tabs defaultActiveKey="NEW" activeKey={activeTab} onChange={(key) => setActiveTab(key as any)} type="card">
                <Tabs.TabPane tab={`New Jobs (${jobs.filter(j => (j.status || 'NEW') === 'NEW').length})`} key="NEW">
                    <List grid={{ gutter: 16, column: 1 }} dataSource={filteredJobs}
                        pagination={{
                            pageSize: pageSize,
                            current: currentPage,
                            onChange: (page, size) => { setCurrentPage(page); setPageSize(size); },
                            showSizeChanger: true,
                            pageSizeOptions: ['10', '20', '50', '100'],
                            onShowSizeChange: (_, size) => { setPageSize(size); setCurrentPage(1); }
                        }}
                        renderItem={renderJobItem} />
                </Tabs.TabPane>
                <Tabs.TabPane tab={`Applied (${jobs.filter(j => j.status === 'APPLIED').length})`} key="APPLIED">
                    <List grid={{ gutter: 16, column: 1 }} dataSource={filteredJobs}
                        pagination={{
                            pageSize: pageSize,
                            current: currentPage,
                            onChange: (page, size) => { setCurrentPage(page); setPageSize(size); },
                            showSizeChanger: true,
                            pageSizeOptions: ['10', '20', '50', '100'],
                            onShowSizeChange: (_, size) => { setPageSize(size); setCurrentPage(1); }
                        }}
                        renderItem={renderJobItem} />
                </Tabs.TabPane>
                <Tabs.TabPane tab={`Drafts (${jobs.filter(j => j.status === 'DRAFT').length})`} key="DRAFT">
                    <List grid={{ gutter: 16, column: 1 }} dataSource={filteredJobs}
                        pagination={{
                            pageSize: pageSize,
                            current: currentPage,
                            onChange: (page, size) => { setCurrentPage(page); setPageSize(size); },
                            showSizeChanger: true,
                            pageSizeOptions: ['10', '20', '50', '100'],
                            onShowSizeChange: (_, size) => { setPageSize(size); setCurrentPage(1); }
                        }}
                        renderItem={renderJobItem} />
                </Tabs.TabPane>
            </Tabs>

            {/* Apply Confirmation Modal */}
            <Modal
                title="Application Tracking"
                open={isModalOpen}
                onCancel={() => handleModalResponse('CANCEL')}
                footer={[
                    <Button key="cancel" onClick={() => handleModalResponse('CANCEL')}>Cancel</Button>,
                    <Button key="draft" onClick={() => handleModalResponse('DRAFT')}>Save as Draft</Button>,
                    <Button key="applied" type="primary" className="bg-blue-600 text-white hover:!bg-blue-700 hover:!text-white border-none" onClick={() => handleModalResponse('APPLIED')}>Yes, I Applied</Button>,
                ]}
            >
                <p>Did you successfully apply to <strong>{selectedJob?.title}</strong> at <strong>{selectedJob?.company}</strong>?</p>
                <p className="text-gray-500 text-sm">Clicking "Yes" will move this job to the 'Applied' tab.</p>
            </Modal>
        </div>
    );
};

export default JobFeed;
