package name.blackcap.socialbutterfly.lib

import javax.swing.SwingWorker
import kotlin.reflect.KMutableProperty0

/**
 * Thrown if the programmer botches something in our DSL.
 */
class SwingWorkerException(message: String): Exception(message) { }

/**
 * A simplified SwingWorker DSL. It does not support intermediate
 * results. Just lets one define a background task and something
 * to execute when complete.
 *
 * @param T Type returned by inBackground (Java doInBackground) task.
 */
class SwingWorkerBuilder<T>: SwingWorker<T,Unit>() {
    private var inBackgroundLambda: (SwingWorkerBuilder<T>.() -> T)? = null
    private var whenDoneLambda: (SwingWorkerBuilder<T>.() -> Unit)? = null

    private fun <U> setOnce(prop: KMutableProperty0<(SwingWorkerBuilder<T>.() -> U)?>, value: SwingWorkerBuilder<T>.() -> U) {
        if (prop.get() != null) {
            throw SwingWorkerException(prop.name.removeSuffix("Lambda") + " already defined!")
        }
        prop.set(value)
    }

    /**
     * Define the inBackground task.
     */
    fun inBackground(lambda: SwingWorkerBuilder<T>.() -> T): Unit {
        setOnce<T>(::inBackgroundLambda, lambda)
    }

    /**
     * Define the whenDone task.
     */
    fun whenDone(lambda: SwingWorkerBuilder<T>.() -> Unit): Unit {
        setOnce<Unit>(::whenDoneLambda, lambda)
    }

    /**
     * Validates we've been properly initialized.
     */
    fun validate(): Unit {
        if (inBackgroundLambda == null) {
            throw SwingWorkerException("inBackground not defined!")
        }
    }

    /* standard overrides for SwingWorker follow */

    override fun doInBackground(): T = inBackgroundLambda!!.invoke(this)

    override fun done(): Unit = whenDoneLambda?.invoke(this) ?: Unit
}

/**
 * Provides for an outer swingWorker block to contain the DSL.
 */
fun <T> swingWorker(initializer: SwingWorkerBuilder<T>.() -> Unit): Unit {
    SwingWorkerBuilder<T>().run {
        initializer()
        validate()
        execute()
    }
}
