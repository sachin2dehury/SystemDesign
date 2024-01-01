import kotlin.concurrent.thread

data class Vehicle(val type: VehicleType, val number: String) {
    private var ticket: Ticket? = null
    fun showTicket() = ticket

    fun handleStatus(status: ParkingStatus) {
        Thread.sleep(1000)
        when (status) {
            is ParkingStatus.Exiting -> {
                ticket = null
                println("Exiting Vehicle type $type with $number Paying ${status.amount}")
            }

            is ParkingStatus.Parking -> {
                this.ticket = status.ticket
                println("Parked with ticket ${status.ticket}")
            }

            ParkingStatus.Waiting -> {
                println("waiting $number $type")
            }
        }
    }
}

sealed interface ParkingStatus {
    data class Parking(val ticket: Ticket) : ParkingStatus
    data class Exiting(val amount: Int) : ParkingStatus
    object Waiting : ParkingStatus
}

enum class VehicleType {
    BIKE, CAR, TRUCK
}

data class ParkingSpace(val id: Int)

data class SupportedVehicle(val type: VehicleType, val slots: Int)

class ParkingLot private constructor(
    supportedVehicles: List<SupportedVehicle>
) {

    private val availableParkingSlots = supportedVehicles.map { supportedVehicle ->
        supportedVehicle.type to (1..supportedVehicle.slots).map { ParkingSpace(it) }.toMutableList()
    }.toMap()

    @Synchronized
    private fun allotSpace(type: VehicleType): ParkingSpace? {
        return if (!availableParkingSlots.contains(type)) {
            throw Exception()
        } else if (availableParkingSlots[type]!!.isEmpty()) {
            null
        } else {
            availableParkingSlots[type]!!.removeFirst()
        }
    }

    @Synchronized
    private fun vacateSpace(type: VehicleType, space: ParkingSpace) {
        if (availableParkingSlots.contains(type)) {
            availableParkingSlots[type]!!.add(space)
        } else {
            throw Exception()
        }
    }

    fun parkVehicle(vehicle: Vehicle, callback: (ParkingStatus) -> Unit) {

        var parkingSpace = allotSpace(vehicle.type)
        if (parkingSpace == null) {
            callback.invoke(ParkingStatus.Waiting)
        }
        while (parkingSpace == null) {
            Thread.sleep(1000)
            parkingSpace = allotSpace(vehicle.type)
        }

        val ticket = Ticket(System.currentTimeMillis(), vehicle.number, vehicle.type, parkingSpace)
        callback.invoke(ParkingStatus.Parking(ticket))
    }

    fun exitVehicle(ticket: Ticket, callback: (ParkingStatus) -> Unit) {
        vacateSpace(ticket.type, ticket.space)
        val fair = calculatePrice(ticket.parkingTime)
        callback.invoke(ParkingStatus.Exiting(fair))
    }

    private fun calculatePrice(time: Long): Int {
        return ((((System.currentTimeMillis() - time) / 1000) + 1) * 10).toInt()
    }

    companion object {
        val INSTANCE = ParkingLot(listOf(SupportedVehicle(VehicleType.BIKE, 1), SupportedVehicle(VehicleType.CAR, 1)))
    }
}

data class Ticket(val parkingTime: Long, val vehicleNumber: String, val type: VehicleType, val space: ParkingSpace)


fun main() {
    for (i in 1..10) {
        thread(name = "Vehicle $i") {
            val vehicle = Vehicle(listOf(VehicleType.BIKE, VehicleType.CAR).random(), "$i")
            ParkingLot.INSTANCE.parkVehicle(vehicle, vehicle::handleStatus)
            Thread.sleep(listOf(1, 2, 3).random() * 100L)
            ParkingLot.INSTANCE.exitVehicle(vehicle.showTicket()!!, vehicle::handleStatus)
        }
    }
}
