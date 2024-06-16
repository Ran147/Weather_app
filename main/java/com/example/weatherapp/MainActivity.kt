package com.example.weatherapp
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import java.text.SimpleDateFormat
import android.view.View
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.google.firebase.database.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var temperatureTextView: TextView // Add this line
    private lateinit var graph: GraphView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dateEditText = findViewById<EditText>(R.id.dateEditText)
        val queryButton = findViewById<Button>(R.id.queryButton)
        val backToMenuButton = findViewById<Button>(R.id.backToMenuButton)
        temperatureTextView = findViewById(R.id.temperatureTextView) // Initialize TextView
        database = FirebaseDatabase.getInstance().reference
        graph = findViewById(R.id.graph)

    //editar lo que hace falta de linechart
        // Display menu when activity starts
        showMenu()

        queryButton.setOnClickListener {
            val date = dateEditText.text.toString()
            if (date.isNotEmpty()) {
                queryTemperatureForDate(date)
            }
        }

        backToMenuButton.setOnClickListener {
            showMenu()
        }
    }
    fun backToMenu(view: View) {
        showMenu()
    }


    private fun showMenu() {
        val options = arrayOf("Option 1", "Option 2", "Option 3")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select an Option")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> {
                    // Option 1 is selected, do nothing
                }
                1 -> queryTemperatureForDateRange()
                2 -> queryTemperatureForDayAndHourRange()
            }
        }
        builder.show()
    }


    private fun queryTemperatureForDate(date: String) {
        val dateRef = database.child(date)
        dateRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temperatureList = mutableListOf<Double>()
                snapshot.children.forEach { childSnapshot ->
                    val temperaturaString = childSnapshot.child("Temperatura").getValue(String::class.java)
                    temperaturaString?.let {
                        val temperatureValue = it.substringAfter(":").trim().removeSuffix("°C").toDoubleOrNull()
                        temperatureValue?.let {
                            temperatureList.add(it)
                        }
                    }
                }

                if (temperatureList.isNotEmpty()) {
                    temperatureTextView.text = "Temperatures on $date: ${temperatureList.joinToString(", ")}C°"
                    displayGraph(temperatureList)
                } else {
                    temperatureTextView.text = "No temperature data found for $date"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Error fetching data: ${error.message}")
            }
        })
    }
    private fun queryTemperatureForDateRange() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Start and End Dates")
        val inputLayout = layoutInflater.inflate(R.layout.dialog_date_range, null)
        val startDateEditText = inputLayout.findViewById<EditText>(R.id.startDateEditText)
        val endDateEditText = inputLayout.findViewById<EditText>(R.id.endDateEditText)
        builder.setView(inputLayout)

        builder.setPositiveButton("OK") { dialog, which ->
            val startDate = startDateEditText.text.toString()
            val endDate = endDateEditText.text.toString()
            if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                queryTemperatureForDateRange(startDate, endDate)
            }
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun queryTemperatureForDateRange(startDate: String, endDate: String) {
        // Convert startDate and endDate to Date objects for comparison
        val startCalendar = Calendar.getInstance().apply {
            time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(startDate) ?: Date()
        }
        val endCalendar = Calendar.getInstance().apply {
            time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(endDate) ?: Date()
        }

        // Check if startDate is before or equal to endDate
        if (startCalendar.before(endCalendar) || startCalendar == endCalendar) {
            // Construct a list of dates between startDate and endDate
            val dateList = mutableListOf<String>()
            val currentCalendar = startCalendar.clone() as Calendar
            while (!currentCalendar.after(endCalendar)) {
                dateList.add(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentCalendar.time))
                currentCalendar.add(Calendar.DATE, 1)
            }

            // Retrieve temperature data for each date in the range
            val temperatureList = mutableListOf<Double>()
            dateList.forEach { date ->
                val dateRef = database.child(date)
                dateRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.children.forEach { childSnapshot ->
                            val temperaturaString = childSnapshot.child("Temperatura").getValue(String::class.java)
                            temperaturaString?.let {
                                val temperatureValue = it.substringAfter(":").trim().removeSuffix("°C").toDoubleOrNull()
                                temperatureValue?.let {
                                    temperatureList.add(it)
                                }
                            }
                        }
                        // Display temperature data on TextView
                        if (temperatureList.isNotEmpty()) {
                            temperatureTextView.text = "Temperatures between $startDate and $endDate: ${temperatureList.joinToString(", ")}C°"
                            displayGraph(temperatureList)
                        } else {
                            temperatureTextView.text = "No temperature data found between $startDate and $endDate"
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        println("Error fetching data: ${error.message}")
                    }
                })
            }
        } else {
            temperatureTextView.text = "Invalid date range: End date must be after or equal to start date"
        }
    }
    private fun queryTemperatureForDayAndHourRange() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Day and Hour Range")

        val inputLayout = layoutInflater.inflate(R.layout.dialog_day_hour_range, null)
        val dayEditText = inputLayout.findViewById<EditText>(R.id.dayEditText)
        val startHourEditText = inputLayout.findViewById<EditText>(R.id.startHourEditText)
        val endHourEditText = inputLayout.findViewById<EditText>(R.id.endHourEditText)

        builder.setView(inputLayout)

        builder.setPositiveButton("OK") { dialog, which ->
            val day = dayEditText.text.toString()
            val startHour = startHourEditText.text.toString()
            val endHour = endHourEditText.text.toString()

            if (day.isNotEmpty() && startHour.isNotEmpty() && endHour.isNotEmpty()) {
                queryTemperatureForDayAndHourRange(day, startHour, endHour)
            }
        }

        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun queryTemperatureForDayAndHourRange(day: String, startHour: String, endHour: String) {
        val startTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(startHour)?.time ?: 0
        val endTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(endHour)?.time ?: 0

        val temperatureList = mutableListOf<Double>()

        val dateRef = database.child(day) // Assuming day refers to the current date for the time comparison
        dateRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { childSnapshot ->
                    val timestampString = childSnapshot.child("Timestamp").getValue(String::class.java)
                    val temperatureString = childSnapshot.child("Temperatura").getValue(String::class.java)

                    timestampString?.let { timestamp ->
                        temperatureString?.let { temperature ->
                            // Adjust timestamp format if necessary
                            val timestampTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).parse(timestamp)?.time ?: 0
                            if (timestampTime in startTime..endTime) {
                                val temperatureValue = temperature.substringAfter(":").trim().removeSuffix(" °C").toDoubleOrNull()
                                temperatureValue?.let {
                                    temperatureList.add(it)
                                }
                            }
                        }
                    }
                }

                if (temperatureList.isNotEmpty()) {
                    temperatureTextView.text = "Temperatures between $startHour and $endHour on $day: ${temperatureList.joinToString(", ")} °C"
                    displayGraph(temperatureList)
                } else {
                    temperatureTextView.text = "No temperature data found between $startHour and $endHour on $day"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Error fetching data: ${error.message}")
            }
        })
    }
    private fun displayGraph(temperatureList: List<Double>) {
        // Clear existing series from the graph
        graph.removeAllSeries()

        // Create a new series for the current data
        val series = LineGraphSeries<DataPoint>()
        temperatureList.forEachIndexed { index, temperature ->
            series.appendData(DataPoint(index.toDouble(), temperature), true, temperatureList.size)
        }

        // Add the new series to the graph
        graph.addSeries(series)

        // Calculate highest, lowest, and average temperatures
        val highestTemperature = temperatureList.maxOrNull()
        val lowestTemperature = temperatureList.minOrNull()
        val averageTemperature = temperatureList.average()

        // Display the calculated values
        val message = "Highest Temperature: ${highestTemperature ?: "N/A"}°C\n" +
                "Lowest Temperature: ${lowestTemperature ?: "N/A"}°C\n" +
                "Average Temperature: ${String.format("%.2f", averageTemperature)}°C"
        temperatureTextView.text = message
    }






}
