package com.gff.dto.response;

public class DashboardStatsResponse {

    private long totalRecords;
    private long uploadedCount;
    private long pendingCount;
    private long failedCount;
    private long ocrCompletedCount;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(long totalRecords, long uploadedCount, long pendingCount, long failedCount, long ocrCompletedCount) {
        this.totalRecords = totalRecords;
        this.uploadedCount = uploadedCount;
        this.pendingCount = pendingCount;
        this.failedCount = failedCount;
        this.ocrCompletedCount = ocrCompletedCount;
    }

    public static DashboardStatsResponseBuilder builder() {
        return new DashboardStatsResponseBuilder();
    }

    public static class DashboardStatsResponseBuilder {
        private long totalRecords;
        private long uploadedCount;
        private long pendingCount;
        private long failedCount;
        private long ocrCompletedCount;

        public DashboardStatsResponseBuilder totalRecords(long totalRecords) {
            this.totalRecords = totalRecords;
            return this;
        }

        public DashboardStatsResponseBuilder uploadedCount(long uploadedCount) {
            this.uploadedCount = uploadedCount;
            return this;
        }

        public DashboardStatsResponseBuilder pendingCount(long pendingCount) {
            this.pendingCount = pendingCount;
            return this;
        }

        public DashboardStatsResponseBuilder failedCount(long failedCount) {
            this.failedCount = failedCount;
            return this;
        }

        public DashboardStatsResponseBuilder ocrCompletedCount(long ocrCompletedCount) {
            this.ocrCompletedCount = ocrCompletedCount;
            return this;
        }

        public DashboardStatsResponse build() {
            return new DashboardStatsResponse(totalRecords, uploadedCount, pendingCount, failedCount, ocrCompletedCount);
        }
    }

    public long getTotalRecords() { return totalRecords; }
    public void setTotalRecords(long totalRecords) { this.totalRecords = totalRecords; }

    public long getUploadedCount() { return uploadedCount; }
    public void setUploadedCount(long uploadedCount) { this.uploadedCount = uploadedCount; }

    public long getPendingCount() { return pendingCount; }
    public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }

    public long getFailedCount() { return failedCount; }
    public void setFailedCount(long failedCount) { this.failedCount = failedCount; }

    public long getOcrCompletedCount() { return ocrCompletedCount; }
    public void setOcrCompletedCount(long ocrCompletedCount) { this.ocrCompletedCount = ocrCompletedCount; }
}
