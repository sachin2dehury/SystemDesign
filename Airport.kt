import kotlin.concurrent.thread

class Airplane {
    fun handleCallback(status: Status, type: RunwayUseType, runway: Runway?) {
        val name = Thread.currentThread().name
        when (status) {
            Status.APPROVED -> {
                println("$name is ${type.name} on ${runway?.id}")
                Thread.sleep(1000)
                println("$name Done")
            }

            Status.WAITING -> {
                println("$name waiting")
            }

            Status.REJECTED -> throw Exception()
        }
    }
}

enum class RunwayUseType {
    LANDING, TAKING_OFF
}

enum class Status {
    APPROVED, WAITING, REJECTED
}

class Airport private constructor(private val runways: Int) {
    private var availableRunways = (1..runways).map { Runway(it) }.toMutableList()

    @Synchronized
    private fun getRunway(): Runway? {
        return if (availableRunways.isNotEmpty()) {
            availableRunways.removeFirst()
        } else {
            null
        }
    }

    @Synchronized
    private fun vacateRunway(runway: Runway) {
        availableRunways.add(runway)
    }

    fun useRunway(type: RunwayUseType, callback: (Status, RunwayUseType, Runway?) -> Unit) {
        if (runways < 1) {
            callback.invoke(Status.REJECTED, type, null)
            return
        }

        var runway = getRunway()

        if (runway == null) {
            callback.invoke(Status.WAITING, type, runway)
            while (runway == null) {
                Thread.sleep(1000)
                runway = getRunway()
            }
        }

        callback.invoke(Status.APPROVED, type, runway)
        vacateRunway(runway)
    }


    companion object {
        val INSTANCE = Airport(2)
    }
}

class Runway(val id: Int)

fun main() {
    for (i in 1..10) {
        thread(name = "Plane $i") {
            val airplane = Airplane()
            Airport.INSTANCE.useRunway(
                listOf(RunwayUseType.TAKING_OFF, RunwayUseType.LANDING).random(),
                airplane::handleCallback
            )
        }
    }
}