package com.example.elon.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.navigation.Navigation;

import com.example.elon.R;
import com.example.elon.fragments.FragmentUserProfile;
import com.example.elon.utils.ReadWriteAppointmentDetails;
import com.example.elon.utils.ReadWriteUserDetails;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TimeZone;

public class AppointmentActivity extends Activity {

    private CalendarView calendarView;
    private Spinner barberSpinner;
    private Spinner treatmentSpinner;
    private Spinner timeSlotSpinner; // New Spinner for time slots
    private Button bookButton;

    private Button cancelBookButton;
    private FirebaseAuth authProfile;
    FirebaseUser firebaseUser;
    String currentDate;

    private boolean appointmentExists;

    // Tambahkan struktur data untuk menyimpan antrian
    private Queue<ReadWriteAppointmentDetails> highPriorityQueue = new LinkedList<>();
    private Queue<ReadWriteAppointmentDetails> mediumPriorityQueue = new LinkedList<>();
    private Queue<ReadWriteAppointmentDetails> lowPriorityQueue = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment);

        authProfile = FirebaseAuth.getInstance();
        firebaseUser = authProfile.getCurrentUser();

        calendarView = findViewById(R.id.calendarView);
        barberSpinner = findViewById(R.id.barberSpinner);
        treatmentSpinner = findViewById(R.id.treatmentSpinner);
        timeSlotSpinner = findViewById(R.id.timeSlotSpinner); // Initialize the new Spinner
        bookButton = findViewById(R.id.bookButton);
        cancelBookButton = findViewById(R.id.cancelBookButton);
        checkIfAppointmentExists();

        // Set up the Barber Spinner
        ArrayAdapter<CharSequence> barberAdapter = ArrayAdapter.createFromResource(this, R.array.barbers_array, android.R.layout.simple_spinner_item);
        barberAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        barberSpinner.setAdapter(barberAdapter);

        // Set up the Treatment Spinner
        ArrayAdapter<CharSequence> treatmentAdapter = ArrayAdapter.createFromResource(this, R.array.treatments_array, android.R.layout.simple_spinner_item);
        treatmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        treatmentSpinner.setAdapter(treatmentAdapter);

        // Set up the Time Slot Spinner with available time slots
        ArrayAdapter<CharSequence> timeSlotAdapter = ArrayAdapter.createFromResource(this, R.array.time_slots_array, android.R.layout.simple_spinner_item);
        timeSlotAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSlotSpinner.setAdapter(timeSlotAdapter);

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            public void onSelectedDayChange(@NonNull CalendarView calendarView, int year, int month, int dayOfMonth) {
                Date selectedDate = new Date(year - 1900, month, dayOfMonth);

                // Format the selected date as needed
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String formattedDate = sdf.format(selectedDate);
                currentDate = formattedDate;
            }
        });

        cancelBookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelAppointment();
                if(!appointmentExists)
                    cancelBookButton.setVisibility(View.GONE);

            }
        });

        // Set an onClickListener for the Book Button
        bookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentDate != null) {
                    Toast.makeText(getApplicationContext(), "Tanggal: " + currentDate, Toast.LENGTH_SHORT).show();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    Date today = new Date();
                    String todayDate = dateFormat.format(today);
                    if (currentDate.compareTo(todayDate) < 0) {
                        Toast.makeText(getApplicationContext(), "Pilih tanggal yang benar", Toast.LENGTH_SHORT).show();
                    } else {
                        // Get selected date from the CalendarView
                        String selectedBarber = barberSpinner.getSelectedItem().toString();
                        String selectedTreatment = treatmentSpinner.getSelectedItem().toString();
                        String selectedTimeSlot = timeSlotSpinner.getSelectedItem().toString();

                        // Perform booking logic here (e.g., save to a database)
                        if (selectedBarber.isEmpty()) {
                            Toast.makeText(getApplicationContext(), "Belum memilih hairstylist", Toast.LENGTH_SHORT).show();
                        } else if (selectedTreatment.isEmpty()) {
                            Toast.makeText(getApplicationContext(), "Belum memilih service", Toast.LENGTH_SHORT).show();
                        } else if (selectedTimeSlot.isEmpty()) {
                            Toast.makeText(getApplicationContext(), "Belum memilih tanggal dan waktu", Toast.LENGTH_SHORT).show();
                        } else {
                            TimeZone indonesiaTimeZone = TimeZone.getTimeZone("Asia/Jakarta");
                            Calendar calendar = Calendar.getInstance(indonesiaTimeZone);
                            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                            int selectedHour = Integer.parseInt(selectedTimeSlot.split(":")[0]);
                            String amPm = selectedTimeSlot.split(" ")[1];

                            if (amPm.equals("PM") && selectedHour != 12) {
                                selectedHour += 12;
                            } else if (amPm.equals("AM") && selectedHour == 12) {
                                selectedHour = 0;
                            }

                            if (currentDate.equals(todayDate)) {
                                if (selectedHour < currentHour) {
                                    Toast.makeText(getApplicationContext(), "Pilih tanggal yang benar", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Tambahkan janji temu ke antrian yang sesuai
                                    addToQueue(new ReadWriteAppointmentDetails(currentDate, selectedBarber, selectedTreatment, selectedTimeSlot));
                                    processQueues();
                                }
                            } else {
                                // Tambahkan janji temu ke antrian yang sesuai
                                addToQueue(new ReadWriteAppointmentDetails(currentDate, selectedBarber, selectedTreatment, selectedTimeSlot));
                                processQueues();
                            }
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Klik pada kalender untuk memilih tanggal", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Metode untuk menambahkan janji temu ke antrian yang sesuai
    private void addToQueue(ReadWriteAppointmentDetails appointmentDetails) {
        int priority = calculatePriority(appointmentDetails);
        switch (priority) {
            case 1:
                highPriorityQueue.add(appointmentDetails);
                break;
            case 2:
                mediumPriorityQueue.add(appointmentDetails);
                break;
            case 3:
                lowPriorityQueue.add(appointmentDetails);
                break;
        }
    }

    // Metode untuk menghitung prioritas janji temu
    private int calculatePriority(ReadWriteAppointmentDetails appointmentDetails) {
        String treatment = appointmentDetails.getTreatment().toLowerCase();
        switch (treatment) {
            case "smoothing":
                return 1;
            case "keratin":
                return 2;
            case "potong rambut":
                return 3;
            default:
                return 3; // Prioritas terendah secara default
        }
    }

    // Metode untuk memproses antrian
    private void processQueues() {
        // Proses antrian prioritas tinggi terlebih dahulu
        while (!highPriorityQueue.isEmpty()) {
            ReadWriteAppointmentDetails appointment = highPriorityQueue.poll();
            saveAppointmentToDatabase(appointment);
        }
        // Proses antrian prioritas sedang
        while (!mediumPriorityQueue.isEmpty()) {
            ReadWriteAppointmentDetails appointment = mediumPriorityQueue.poll();
            saveAppointmentToDatabase(appointment);
        }
        // Proses antrian prioritas rendah
        while (!lowPriorityQueue.isEmpty()) {
            ReadWriteAppointmentDetails appointment = lowPriorityQueue.poll();
            saveAppointmentToDatabase(appointment);
        }
    }

    // Metode untuk menyimpan janji temu ke database
    private void saveAppointmentToDatabase(ReadWriteAppointmentDetails appointmentDetails) {
        DatabaseReference referenceAppointment = FirebaseDatabase.getInstance().getReference("Appointments");
        referenceAppointment.child(firebaseUser.getUid()).setValue(appointmentDetails).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Tampilkan pesan konfirmasi
                    Toast.makeText(getApplicationContext(), "Booking untuk " +
                                    appointmentDetails.getDate() + "\nBarber: " + appointmentDetails.getBarber() + "\nTreatment: " + appointmentDetails.getTreatment() + "\nTime Slot: " + appointmentDetails.getTimeSlot(),
                            Toast.LENGTH_LONG).show();
                    appointmentExists = true;
                    Intent intent = new Intent(AppointmentActivity.this, MainActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Booking gagal, coba lagi nanti.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkIfAppointmentExists() {
        DatabaseReference referenceAppointment = FirebaseDatabase.getInstance().getReference("Appointments");
        Query query = referenceAppointment.orderByChild("userID").equalTo(firebaseUser.getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                appointmentExists = snapshot.exists(); // Check if any appointments exist for this user

                if (appointmentExists) {
                    cancelBookButton.setVisibility(View.VISIBLE); // Show the cancel button if an appointment exists
                } else {
                    cancelBookButton.setVisibility(View.GONE); // Hide the cancel button if no appointments exist
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AppointmentActivity.this, "Error checking appointments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelAppointment() {
        DatabaseReference referenceAppointment = FirebaseDatabase.getInstance().getReference("Appointments");
        Query query = referenceAppointment.orderByChild("userID").equalTo(firebaseUser.getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot appointmentSnapshot : snapshot.getChildren()) {
                    appointmentSnapshot.getRef().removeValue();
                }
                Toast.makeText(AppointmentActivity.this, "Appointment cancelled", Toast.LENGTH_SHORT).show();
                appointmentExists = false; // Update the flag to indicate no appointments exist
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AppointmentActivity.this, "Error cancelling appointment", Toast.LENGTH_SHORT).show();
            }
        });
    }
}