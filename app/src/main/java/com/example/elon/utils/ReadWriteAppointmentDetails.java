package com.example.elon.utils;

public class ReadWriteAppointmentDetails {
    private String date;
    private String barber;
    private String treatment;
    private String timeSlot;
    private int priority;

    public ReadWriteAppointmentDetails() {
    }

    public ReadWriteAppointmentDetails(String date, String barber, String treatment, String timeSlot) {
        this.date = date;
        this.barber = barber;
        this.treatment = treatment;
        this.timeSlot = timeSlot;
        this.priority = calculatePriority(treatment); // Calculate priority based on treatment
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getBarber() {
        return barber;
    }

    public void setBarber(String barber) {
        this.barber = barber;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
        this.priority = calculatePriority(treatment); // Update priority when treatment is set
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getAppointmentDetails() {
        String appointmentMessage = "Booking dijadwalkan untuk:\n"
                + getDate() + ", "
                + getBarber() + ", "
                + getTreatment() + ", "
                + getTimeSlot() + ".\n"
                + "Sampai jumpa di salon!";
        return appointmentMessage;
    }

    private int calculatePriority(String treatment) {
        switch (treatment.toLowerCase()) {
            case "smoothing":
                return 1; // Highest priority
            case "keratin":
                return 2; // Medium priority
            case "potong rambut":
                return 3; // Lowest priority
            default:
                return 4; // Default to lowest priority for unspecified treatments
        }
    }
}
