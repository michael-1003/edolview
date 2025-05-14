package kr.edoli.edolview.ui.window

import kr.edoli.edolview.util.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import kotlin.concurrent.fixedRateTimer

class ObservableInfo private constructor() : BaseWindow() {
    companion object {
        var infoPanel: ObservableInfo? = null

        fun show() {
            val panel = infoPanel
            if (panel == null) {
                infoPanel = ObservableInfo()
            } else {
                panel.isVisible = true
            }
        }
    }

    var tableData = createData()
    var observableData = ObservableContext.getAllObservables()

    val refresher = fixedRateTimer(period = 1000) {
        if (isVisible) {
            tableData = createData()
            observableData = ObservableContext.getAllObservables()

            val newData = tableData

            (0 until table.rowCount).forEach { i ->
                (0 until table.columnCount).forEach { j ->
                    table.setValueAt(newData[i][j], i, j)
                }
            }
        }
    }
    val table = JTable(object : DefaultTableModel(tableData, arrayOf("Name", "Count", "Update Time", "Subjects")) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false // 모든 셀을 읽기 전용으로 설정
        }
    }).apply {
        addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val row = selectedRow
                    if (row != -1) {
                        val observableName = tableData[row][0] as String

                        if (e.isControlDown) {
                            // Show the stack trace of subscribers
                            val subscribers = when (val observable = observableData[row]) {
                                is ObservableLazyValue<*> -> observable.subscribers
                                is ObservableValue<*> -> observable.subscribers
                                is Observable<*> -> observable.subscribers
                                is ObservableList<*> -> observable.subscribers
                                else -> null // 다른 타입의 경우 처리
                            }

                            if (subscribers != null) {
                                println("========== Subscribers stack trace for $observableName: ==========")
                                for (subscriber in subscribers) {
                                    val stackTrace = subscriber.stackTrace
                                    for (stackTraceElement in stackTrace) {
                                        println(stackTraceElement)
                                    }
                                    println("-----------------------------------")
                                }
                            }
                        } else {
                            // Show the stack trace of the last update of the observable
                            val observable = observableData[row]
                            println("========== Last update stack trace for $observableName: ==========")
                            for (stackTraceElement in observable.lastUpdateStackTrace) {
                                println(stackTraceElement)
                            }
                            println("-----------------------------------")
                        }
                    }
                }
            }
        })
    }

    init {
        title = "Info"

        (0 until table.rowCount).forEach { i ->
            (0 until table.columnCount).forEach { j ->
                table.editCellAt(i, j)
            }
        }
        table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        table.columnModel.getColumn(1).preferredWidth = 32
        table.columnModel.getColumn(3).preferredWidth = 500

        add(JScrollPane(table))

        table.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
            }

            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_F5) {
                    val newData = createData()
                    newData.forEachIndexed { i, row ->
                        row.forEachIndexed { j, value ->
                            table.model.setValueAt(value, i, j)
                        }
                    }
                }
            }

            override fun keyReleased(e: KeyEvent) {
            }

        })
        pack()

        table.isFocusable = true
        isVisible = true

    }

    override fun dispose() {
        super.dispose()
        refresher.cancel()
    }

    fun createData(): Array<Array<Any>> {
        val allSize = ObservableContext.observables.size +
                ObservableContext.observableValues.size +
                ObservableContext.observableLazyValues.size +
                ObservableContext.observableLists.size

        val data = Array(allSize) { Array<Any>(4) {} }
        var index = 0
        ObservableContext.observables.forEachIndexed { _, value ->
            data[index][0] = value.name
            data[index][1] = value.subscribers.size
            data[index][2] = "${value.lastTotalUpdateTime.toFloat() / 1000 / 1000}ms"
            data[index][3] = value.subscribers.map { it.subject }
            index += 1
        }
        ObservableContext.observableValues.forEachIndexed { _, value ->
            data[index][0] = value.name
            data[index][1] = value.subscribers.size
            data[index][2] = "${value.lastTotalUpdateTime.toFloat() / 1000 / 1000}ms"
            data[index][3] = value.subscribers.map { it.subject }
            index += 1
        }
        ObservableContext.observableLazyValues.forEachIndexed { _, value ->
            data[index][0] = value.name
            data[index][1] = value.subscribers.size
            data[index][2] = "${value.lastTotalUpdateTime.toFloat() / 1000 / 1000}ms"
            data[index][3] = value.subscribers.map { it.subject }
            index += 1
        }
        ObservableContext.observableLists.forEachIndexed { i, value ->
            data[index][0] = value.name
            data[index][1] = value.subscribers.size
            data[index][2] = "${value.lastTotalUpdateTime.toFloat() / 1000 / 1000}ms"
            data[index][3] = value.subscribers.map { it.subject }
            index += 1
        }
        return data
    }
}