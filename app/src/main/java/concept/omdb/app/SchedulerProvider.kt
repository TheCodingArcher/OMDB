package concept.omdb.app

import io.reactivex.Scheduler

interface SchedulerProvider {

    val io: Scheduler

    val main: Scheduler
}