package com.sam.video.util

public inline fun <T> MutableList<T>.removeObj(item: T): Collection<T> {
    val index = this.indexOfFirst { it === item }
    if (index in 0 until size) {
        this.removeAt(index)
    }
    return this
}
