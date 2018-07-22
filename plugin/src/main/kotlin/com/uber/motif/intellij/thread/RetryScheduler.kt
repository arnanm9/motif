package com.uber.motif.intellij.thread

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Single-threaded scheduler that executes the given Job when the following is true:
 *
 * 1) Indexes are available.
 * 2) Read access is allowed.
 *
 * If a ProcessedCancelledException is thrown during execution, the Job is automatically rescheduled.
 *
 * Note: It's possible that indexes are not available if runWithRetry is executed when we already have read access (See
 * DumbService.runReadActionInSmartMode for details). In this case, an IndexNotReadyException is thrown.
 */
class RetryScheduler(project: Project) {

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val dumbService: DumbService = DumbService.getInstance(project)
    private val progressManager: ProgressManager = ProgressManager.getInstance()

    fun runWithRetry(job: Job) {
        scheduler.submit(object: Runnable {

            override fun run() {
                dumbService.runReadActionInSmartMode {
                    val success = progressManager.runInReadActionWithWriteActionPriority({
                        job.run()
                    }, null)
                    if (!success) {
                        job.onRetry()
                        scheduler.submit(this)
                    }
                }
            }
        })
    }

    interface Job {
        fun run()
        fun onRetry()
    }
}