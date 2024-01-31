package com.example.kanbun.common

import com.google.common.truth.Truth.assertThat
import com.google.firebase.firestore.FieldValue
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

    data class Task(
        val id: String,
        var position: Int
    )

    /**
     * Rearranges items [from] to [to]
     */
    fun rearrangeItemsPositionMap(tasks: List<Task>, from: Int, to: Int): Map<String, Int> {
        val updMap = mutableMapOf<String, Int>()
        if (from < to) {

            for (i in (from + 1)..to) {
                updMap["tasks.${tasks[i].id}.position"] = tasks[i].position.dec()
            }

        } else {
            for (i in to..<from) {
                updMap["tasks.${tasks[i].id}.position"] = tasks[i].position.inc()
            }
        }

        updMap["tasks.${tasks[from].id}.position"] = to

        return updMap
    }

    /**
     * Rearranges items after task deletion at [from]
     */
    fun rearrangeItemsPositionMap(tasks: List<Task>, from: Int): Map<String, Any> {
        val updMap = mutableMapOf<String, Any>()

        for (i in (from + 1)..<tasks.size) {
            updMap["tasks.${tasks[i].id}.position"] = tasks[i].position.dec()
        }


        updMap["tasks.${tasks[from].id}"] = FieldValue.delete()

        return updMap
    }

    /**
     * Inserts task in a new list
     */
    fun rearrangeItemsPositionMap(listToInsert: List<Task>, task: Task, to: Int): Map<String, Any> {
        val updMap = mutableMapOf<String, Any>()

        for (i in to..<listToInsert.size) {
            updMap["tasks.${listToInsert[i].id}.position"] = listToInsert[i].position.inc()
        }


        updMap["tasks.${task.id}"] = mapOf(
            "position" to task.position
        )

        return updMap
    }

    @Test
    fun `rearrangeItemsPositionMap where from position less than to`() {
        val tasks = listOf(
            Task("1", 0),
            Task("2", 1),
            Task("3", 2),
            Task("4", 3)
        )

        val from = 0
        val to = 2

        val expectedResult = mapOf(
            "tasks.2.position" to 0,
            "tasks.3.position" to 1,
            "tasks.1.position" to 2
        )

        val actualResult = rearrangeItemsPositionMap(tasks, from, to)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `rearrangeItemsPositionMap where from position greater than to`() {
        val tasks = listOf(
            Task("1", 0),
            Task("2", 1),
            Task("3", 2),
            Task("4", 3)
        )

        val from = 2
        val to = 0

        val expectedResult = mapOf(
            "tasks.1.position" to 1,
            "tasks.2.position" to 2,
            "tasks.3.position" to 0
        )

        val actualResult = rearrangeItemsPositionMap(tasks, from, to)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `rearrangeItemsPositionMap when task is deleted`() {
        val tasks = listOf(
            Task("1", 0),
            Task("2", 1),
            Task("3", 2),
            Task("4", 3)
        )

        val from = 1

        val expectedResult = mapOf(
            "tasks.3.position" to 1,
            "tasks.4.position" to 2,
            "tasks.2" to FieldValue.delete()
        )

        val actualResult = rearrangeItemsPositionMap(tasks, from)

        assertThat(actualResult).isEqualTo(expectedResult)
    }

    @Test
    fun `rearrangeItemsPositionMap when task is inserted in a new list`() {
        val tasks1 = listOf(
            Task("1", 0),
            Task("2", 1),
            Task("3", 2),
            Task("4", 3)
        )

        val tasks2 = listOf(
            Task("15", 0),
            Task("10", 1),
            Task("17", 2),
            Task("20", 3),
        )

        val from = 1
        val to = 1

        val expectedResult = mapOf(
            "tasks.10.position" to 2,
            "tasks.17.position" to 3,
            "tasks.20.position" to 4,
            "tasks.2" to mapOf(
                "position" to to
            )
        )

        val actualResult = rearrangeItemsPositionMap(tasks2, tasks1[from], to)

        assertThat(actualResult).isEqualTo(expectedResult)
    }
}