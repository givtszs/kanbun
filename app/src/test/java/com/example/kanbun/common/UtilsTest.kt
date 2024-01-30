package com.example.kanbun.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UtilsTest {

    data class Item(
        val value: Int,
        var position: Int
    )

    fun rearrangeItemsPosition(items: List<Item>, from: Int, to: Int): List<Item> {
        if (from == to) {
            return items
        }

        //[
        //            Item(1, 0),
        //            Item(2, 1),
        //            Item(3, 2),
        //            Item(4, 3)
        //        ]
//        var newItems = listOf<Item>()

        val newItems: MutableList<Item> = items.toMutableList()

        if (from < to) {

//            ** PASSED **
//            val itemFrom = newItems[from]
//            newItems.removeAt(from)
//
//            newItems.forEach { item ->
//                val pos = item.position
//                if (pos in (from + 1)..to) {
//                    item.position = pos - 1
//                }
//            }
//
//            newItems.add(to, itemFrom.copy(position = to))


//           ** PASSED **
//            val i = newItems.filter { it.position in (from + 1)..to }
//            i.forEach { it.position = it.position - 1 }
//            newItems[from].position = to

            for (i in (from + 1)..to) {
                newItems[i].position = newItems[i].position - 1
            }

        } else {
            for (i in to..<from) {
                newItems[i].position = newItems[i].position.inc()
            }
        }

        newItems[from].position = to

        return newItems.sortedBy { it.position }
    }

    @Test
    fun `rearrangeItemsPosition where from position less than to`() {
        val items = listOf(
            Item(1, 0),
            Item(2, 1),
            Item(3, 2),
            Item(4, 3)
        )

        val expectedResult = listOf(
            Item(2, 0),
            Item(3, 1),
            Item(1, 2),
            Item(4, 3)
        )

        val actualResult = rearrangeItemsPosition(items, 0, 2)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `rearrangeItemsPosition where from position greater than to`() {
        val items = listOf(
            Item(1, 0),
            Item(2, 1),
            Item(3, 2),
            Item(4, 3)
        )

        val expectedResult = listOf(
            Item(3, 0),
            Item(1, 1),
            Item(2, 2),
            Item(4, 3)
        )

        val actualResult = rearrangeItemsPosition(items, 2, 0)

        assertThat(actualResult).isEqualTo(expectedResult)
    }
}