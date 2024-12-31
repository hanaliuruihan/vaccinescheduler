package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        String password = tokens[2];
        if (!isStrongPassword(password)) {
            System.out.println("Password is not strong. Please follow the password guidelines.");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }



    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }


    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        String password = tokens[2];
        if (!isStrongPassword(password)) {
            System.out.println("Password is not strong. Please follow the password guidelines.");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }

        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }

        if (tokens.length < 2) {
            System.out.println("Please try again");
            return;
        }

        String dateStr = tokens[1];

        try {
            Date date = Date.valueOf(dateStr);
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getAvailableCaregivers = "SELECT Username FROM Availabilities " +
                    "WHERE Time = ? " +
                    "ORDER BY Username";

            try (PreparedStatement caregiversStatement = con.prepareStatement(getAvailableCaregivers)) {
                caregiversStatement.setDate(1, date);
                ResultSet caregiversResult = caregiversStatement.executeQuery();

                while (caregiversResult.next()) {
                    System.out.println(caregiversResult.getString("Username"));
                }
            }

            String getAvailableVaccines = "SELECT Name, Doses FROM Vaccines ORDER BY Name";

            try (PreparedStatement vaccinesStatement = con.prepareStatement(getAvailableVaccines)) {
                ResultSet vaccinesResult = vaccinesStatement.executeQuery();

                while (vaccinesResult.next()) {
                    System.out.println(vaccinesResult.getString("Name") + " " + vaccinesResult.getInt("Doses"));
                }
            }

            cm.closeConnection();
        } catch (IllegalArgumentException | SQLException e) {
            System.out.println("Please try again");
        }
    }

    private static void reserve(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }

        if (currentPatient == null) {
            System.out.println("Please login as a patient");
            return;
        }

        if (tokens.length != 3) {
            System.out.println("Please try again");
            return;
        }

        String dateStr = tokens[1];
        String vaccineName = tokens[2];

        try {
            Date date = Date.valueOf(dateStr);
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getAvailableCaregiver =  "SELECT TOP 1 Username FROM Availabilities " +
                    "WHERE Time = ? " +
                    "ORDER BY Username";

            String getVaccineDoses = "SELECT Doses FROM Vaccines WHERE Name = ?";
            String reserveAppointment = "INSERT INTO Appointment (PName, CName, VName, Time, ApptID) VALUES (?, ?, ?, ?, ?)";
            String decreaseVaccineDoses = "UPDATE Vaccines SET Doses = Doses - 1 WHERE Name = ?";
            String deleteAvailability = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";

            try (PreparedStatement availableCaregiverStatement = con.prepareStatement(getAvailableCaregiver);
                 PreparedStatement vaccineDosesStatement = con.prepareStatement(getVaccineDoses);
                 PreparedStatement reserveStatement = con.prepareStatement(reserveAppointment);
                 PreparedStatement decreaseDosesStatement = con.prepareStatement(decreaseVaccineDoses);
                 PreparedStatement deleteAvailabilityStatement = con.prepareStatement(deleteAvailability)) {

                availableCaregiverStatement.setDate(1, date);
                ResultSet caregiverResult = availableCaregiverStatement.executeQuery();

                if (caregiverResult.next()) {
                    String caregiverUsername = caregiverResult.getString("Username");

                    vaccineDosesStatement.setString(1, vaccineName);
                    ResultSet vaccineResult = vaccineDosesStatement.executeQuery();

                    if (vaccineResult.next()) {
                        int availableDoses = vaccineResult.getInt("Doses");

                        if (availableDoses > 0) {
                            reserveStatement.setString(1, currentPatient.getUsername());
                            reserveStatement.setString(2, caregiverUsername);
                            reserveStatement.setString(3, vaccineName);
                            reserveStatement.setDate(4, date);
                            reserveStatement.setInt(5, getLatestAppointmentID(con) + 1);
                            reserveStatement.executeUpdate();

                            decreaseDosesStatement.setString(1, vaccineName);
                            decreaseDosesStatement.executeUpdate();

                            deleteAvailabilityStatement.setString(1, caregiverUsername);
                            deleteAvailabilityStatement.setDate(2, date);
                            deleteAvailabilityStatement.executeUpdate();

                            System.out.println("Appointment ID " + getLatestAppointmentID(con) +
                                    ", Caregiver username " + caregiverUsername);
                        } else {
                            System.out.println("Not enough available doses");
                        }
                    } else {
                        System.out.println("Vaccine not found");
                    }
                } else {
                    System.out.println("No caregiver is available");
                }
            }

            cm.closeConnection();
        } catch (IllegalArgumentException e) {
            System.out.println("Please try again");
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // T.ODO: Part 2

        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }

        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }

        try {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getAppointmentsQuery;

            if (currentPatient != null) {
                // For patients
                getAppointmentsQuery = "SELECT ApptID, VName AS Vaccine, Time, CName AS Caregiver FROM Appointment " +
                        "WHERE PName = ? ORDER BY ApptID";
            } else {
                // For caregivers
                getAppointmentsQuery = "SELECT ApptID, VName AS Vaccine, Time, PName AS Patient FROM Appointment " +
                        "WHERE CName = ? ORDER BY ApptID";
            }

            try (PreparedStatement getAppointmentsStatement = con.prepareStatement(getAppointmentsQuery)) {

                String currentUserUsername = (currentPatient != null) ? currentPatient.getUsername() : currentCaregiver.getUsername();

                getAppointmentsStatement.setString(1, currentUserUsername);

                ResultSet appointmentsResult = getAppointmentsStatement.executeQuery();

                while (appointmentsResult.next()) {
                    System.out.println(appointmentsResult.getInt("ApptID") + " " +
                            appointmentsResult.getString("Vaccine") + " " +
                            appointmentsResult.getDate("Time") + " " +
                            appointmentsResult.getString((currentPatient != null) ? "Caregiver" : "Patient"));
                }
            }

            cm.closeConnection();
        } catch (SQLException e) {
            System.out.println("Please try again");
        }
    }

    private static int getLatestAppointmentID(Connection con) throws SQLException {
        String getLatestIDQuery = "SELECT MAX(ApptID) as LatestID FROM Appointment";

        try (PreparedStatement getLatestIDStatement = con.prepareStatement(getLatestIDQuery);
             ResultSet resultSet = getLatestIDStatement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getInt("LatestID");
            }
        }

        throw new SQLException("Error getting the latest appointment ID");
    }

    private static void logout(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }

        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
        } else {
            System.out.println("Successfully logged out");
            currentPatient = null;
            currentCaregiver = null;
        }
    }

    private static boolean isStrongPassword(String password) {
        if (password.length()<8) {
            return false;
        }

        if (!(password.matches(".*[a-z].*") && password.matches(".*[A-Z].*"))) {
            return false;
        }

        if (!password.matches(".*[a-zA-Z].*\\d.*")) {
            return false;
        }

        Pattern specialCharPattern = Pattern.compile("[!@#?]");
        Matcher specialCharMatcher = specialCharPattern.matcher(password);
        if (!specialCharMatcher.find()) {
            return false;
        }

        return true;
    }
}
