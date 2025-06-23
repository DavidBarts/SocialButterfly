package name.blackcap.socialbutterfly.lib

/* Connectors wire a class representing permanent data stored in a file to
   ListModels used for presentation to the user. */

import javax.swing.DefaultListModel

class MapConnector<T: Any, U: Comparable<U>>(
    val map: MutableMap<String, T>,
    val sortKeyGenerator: (String, T) -> U
) {
    val listModel = DefaultListModel<MapConnector<T, U>.ListModelEntry>().apply {
        val entries = map.entries
            .map { ListModelEntry(it.key, sortKeyGenerator(it.key, it.value), it.value) }
            .sortedBy { it.sortKey }
        addAll(entries)
    }

    inner class ListModelEntry(
        val accessKey: String,
        val sortKey: U,
        val value: T
    )

    fun add(accessKey: String, value: T) {
        map[accessKey] = value
        val sortKey = sortKeyGenerator(accessKey, value)
        val limit = listModel.size()
        var index = 0
        while (index < limit  && listModel[index].sortKey < sortKey) {
            index++
        }
        listModel.insertElementAt(ListModelEntry(accessKey, sortKey, value), index)
    }

    fun remove(index: Int) {
        val accessKey = listModel.remove(index).accessKey
        map.remove(accessKey)
    }
}

class SetConnector<T: Any, U:Comparable<U>>(
    val set: MutableSet<T>,
    val sortKeyGenerator: (T) -> U
){
    val listModel = DefaultListModel<SetConnector<T, U>.ListModelEntry>().apply {
        val entries = set
            .map { ListModelEntry(sortKeyGenerator(it), it) }
            .sortedBy { it.sortKey }
        addAll(entries)
    }

    inner class ListModelEntry(
        val sortKey: U,
        val value: T
    )

    fun add(value: T) {
        set.add(value)
        val sortKey = sortKeyGenerator(value)
        val limit = listModel.size()
        var index = 0
        while (index < limit  && listModel[index].sortKey < sortKey) {
            index++
        }
        listModel.insertElementAt(ListModelEntry(sortKey, value), index)
    }

    fun remove(index: Int) {
        val item = listModel.remove(index).value
        set.remove(item)
    }
}
