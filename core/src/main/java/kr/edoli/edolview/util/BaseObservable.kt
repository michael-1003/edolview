package kr.edoli.edolview.util

open class BaseObservable {

    var lastTotalUpdateTime = 0L
        set(value) {
            lastUpdateStackTrace = Thread.currentThread().stackTrace.drop(3).toTypedArray()
            field = value
        }

    var lastUpdateStackTrace: Array<StackTraceElement> = arrayOf()
        private set
}