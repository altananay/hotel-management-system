package com.hotel.media.internal.application;

import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class MediaProcessingScheduler {

    private final Executor executor;
    private final MediaProcessingWorker worker;

    public MediaProcessingScheduler(
            @Qualifier("mediaTaskExecutor") Executor executor, MediaProcessingWorker worker) {
        this.executor = executor;
        this.worker = worker;
    }

    public void enqueue(UUID mediaId) {
        executor.execute(() -> worker.process(mediaId));
    }
}
