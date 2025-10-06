package com.doctorservice.entity;

public enum ReportStatus {
    DRAFT,      // Report is being edited, can be updated
    FINALIZED   // Report exported to PDF, cannot be modified
}