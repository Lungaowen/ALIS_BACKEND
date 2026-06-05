-- ALIS Report PDF artifact storage.
-- Run this once in Supabase before enabling S3-backed report downloads.

ALTER TABLE summary_report
    ADD COLUMN IF NOT EXISTS report_url TEXT;
