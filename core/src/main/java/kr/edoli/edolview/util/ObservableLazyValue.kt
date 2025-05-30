package kr.edoli.edolview.util

import rx.Subscription
import rx.subjects.BehaviorSubject
import rx.subjects.Subject

/**
 * Created by daniel on 16. 10. 2.
 */
class ObservableLazyValue<T>(val initValue: T, val name: String, val checkValue: (T) -> T = {it}): BaseObservable() {
    private val observable: Subject<T, T> = BehaviorSubject.create()
    val subscribers = ArrayList<LazySubscriber>()
    private var value = initValue
    private var storedAction: (() -> T)? = null

    var doExecuteCount = 0

    init {
        observable.onNext(initValue)
        ObservableContext.observableLazyValues.add(this)
    }

    fun subscribe(subject: Any, description: String, doExecute: Boolean = true, onNext: (T) -> Unit): LazySubscriber {
        if (doExecute) {
            doExecuteCount += 1
            executeUpdate()
        }
        val subscription = observable.subscribe(onNext)
        val subscriber = LazySubscriber(subject, subscription, description, doExecute)
        subscribers.add(subscriber)
        return subscriber
    }

    fun unsubscribe(subject: Any) {
        subscribers.filter { it.subject == subject }.forEach {
            if (it.doExecute) {
                doExecuteCount -= 1
            }
            unsubscribe(it)
        }
    }

    fun unsubscribe(subscriber: LazySubscriber) {
        if (subscriber.doExecute) {
            doExecuteCount -= 1
        }
        subscriber.subscription.unsubscribe()
        subscribers.remove(subscriber)
    }

    fun update(action: () -> T) {
        if (!ObservableContext.push(this)) {
            return
        }

        storedAction = action

        if (doExecuteCount > 0) {
            executeUpdate()
        }

        ObservableContext.pop(this)
    }

    private fun executeUpdate() {
        val action = storedAction
        storedAction = null
        if (action != null) {
            val startTime = System.nanoTime()
            value = checkValue(action())
            observable.onNext(value)
            lastTotalUpdateTime = System.nanoTime() - startTime
        }
    }

    fun get(doExecute: Boolean = true): T {
        if (doExecute) {
            executeUpdate()
        }
        return value
    }


    fun once(onNext: (T) -> Unit, doExecute: Boolean = true) {
        if (doExecute) {
            executeUpdate()
        }
        observable.subscribe(onNext).unsubscribe()
    }

    fun reset() {
        storedAction = null
        observable.onNext(initValue)
    }
}