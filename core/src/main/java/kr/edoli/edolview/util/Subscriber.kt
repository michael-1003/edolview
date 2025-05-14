package kr.edoli.edolview.util

import rx.Subscription

open class BaseSubscriber {
    // Save stackTrace for debugging, drop 3 elements to avoid noise (Thread, Subscriber, Observable)
    val stackTrace = Thread.currentThread().stackTrace.drop(3).toTypedArray()
}

data class Subscriber(val subject: Any, val subscription: Subscription, val description: String): BaseSubscriber()

data class LazySubscriber(val subject: Any, val subscription: Subscription, val description: String, val doExecute: Boolean): BaseSubscriber()