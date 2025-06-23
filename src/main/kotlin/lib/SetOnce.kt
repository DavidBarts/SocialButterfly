package name.blackcap.socialbutterfly.lib

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Delegate that makes a var that can only be set once. This is commonly
 * needed in Swing, because some vars inevitably need to be declared at
 * outer levels but initialized in the Swing event dispatch thread.
 *
 * @param &lt;T&gt; type of the associated value
 */
class SetOnceImpl<T: Any>: ReadWriteProperty<Any?, T> {
    private var setOnceValue: T? = null

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (setOnceValue == null) {
            throw RuntimeException("${property.name} has not been initialized")
        } else {
            return setOnceValue!!
        }
    }

    @Synchronized
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T): Unit {
        if (setOnceValue != null) {
            throw RuntimeException("${property.name} has already been initialized")
        }
        setOnceValue = value
    }
}

/**
 * Normal way to create a setOnce var:
 * var something: SomeType by setOnce()
 */
fun <T: Any> setOnce(): SetOnceImpl<T> = SetOnceImpl<T>()
