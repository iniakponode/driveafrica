package com.uoa.core.model

enum class SyncState {
    /**
     * Trip completed, classification done, summaries computed.
     */
    SUMMARY_READY,

    /**
     * Raw sensor data and trip details successfully uploaded to the backend.
     */
    RAW_DATA_UPLOADED,

    /**
     * LLM outputs (reports/tips) have been generated and saved locally.
     */
    LLM_OUTPUT_READY,

    /**
     * Heavy data cleaned; only summaries and retained lightweight records remain.
     */
    ARCHIVED
}
