package com.example.chaturbateclient.data

fun pageOffsets(totalCount: Int, pageSize: Int, maxPages: Int): List<Int> {
    require(totalCount >= 0)
    require(pageSize > 0)
    require(maxPages > 0)
    val pages = ((totalCount + pageSize - 1) / pageSize).coerceAtMost(maxPages)
    return (0 until pages).map { it * pageSize }
}
