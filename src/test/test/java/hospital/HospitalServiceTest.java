/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */
package hospital;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author barat
 */

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class HospitalServiceTest {
    
    

class Hospital1ServiceTest {

    private HospitalService service;

    @BeforeEach
    void setUp() {
        service = new HospitalService();
    }

   
    @Test
    void registerPatient_addsPatientToRegistry() {
        Patient p = service.registerPatient("P001", "Jane", "Doe", 34, "Female", "Flu", PatientCategory.OUTPATIENT);

        assertEquals(1, service.getTotalPatients());
        assertEquals("P001", p.getPatientId());
        assertFalse(p instanceof Inpatient, "Outpatient should not be an Inpatient instance");
    }

    @Test
    void registerPatient_withInpatientCategory_createsInpatientInstance() {
        Patient p = service.registerPatient("P002", "John", "Smith", 45, "Male", "Pneumonia", PatientCategory.INPATIENT);

        assertTrue(p instanceof Inpatient);
        Inpatient inpatient = (Inpatient) p;
        assertFalse(inpatient.hasBedAllocated(), "Newly registered inpatient should not yet have a bed");
    }

    @Test
    void registerPatient_preventsDuplicatePatientIds() {
        service.registerPatient("P003", "Alice", "Jones", 28, "Female", "Asthma", PatientCategory.OUTPATIENT);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.registerPatient("P003", "Bob", "Brown", 50, "Male", "Diabetes", PatientCategory.EMERGENCY));

        assertTrue(ex.getMessage().contains("already registered"));
        assertEquals(1, service.getTotalPatients(), "Duplicate registration must not add a second patient");
    }

   
    @Test
    void searchPatient_findsExistingPatient() {
        service.registerPatient("P010", "Sam", "Green", 22, "Male", "Broken arm", PatientCategory.EMERGENCY);

        Optional<Patient> found = service.searchPatient("P010");

        assertTrue(found.isPresent());
        assertEquals("Green", found.get().getLastName());
    }

    @Test
    void searchPatient_returnsEmptyForUnknownId() {
        Optional<Patient> found = service.searchPatient("DOES_NOT_EXIST");
        assertTrue(found.isEmpty());
    }



    @Test
    void updatePatient_changesDetailsSuccessfully() {
        service.registerPatient("P020", "Tom", "White", 30, "Male", "Cold", PatientCategory.OUTPATIENT);

        boolean updated = service.updatePatient("P020", "Tom", "White", 31, "Male", "Recovered");

        assertTrue(updated);
        Patient p = service.searchPatient("P020").orElseThrow();
        assertEquals(31, p.getAge());
        assertEquals("Recovered", p.getMedicalCondition());
    }

    @Test
    void updatePatient_returnsFalseForUnknownId() {
        boolean updated = service.updatePatient("UNKNOWN", "A", "B", 1, "M", "None");
        assertFalse(updated);
    }

    // ---------------------------------------------------------------
    // Delete a patient
    // ---------------------------------------------------------------

    @Test
    void deletePatient_removesPatientFromRegistry() {
        service.registerPatient("P030", "Nina", "Adams", 40, "Female", "Migraine", PatientCategory.OUTPATIENT);

        boolean deleted = service.deletePatient("P030");

        assertTrue(deleted);
        assertEquals(0, service.getTotalPatients());
        assertTrue(service.searchPatient("P030").isEmpty());
    }

    @Test
    void deletePatient_releasesBedIfInpatientHadOne() {
        service.registerPatient("P031", "Leo", "King", 60, "Male", "Surgery recovery", PatientCategory.INPATIENT);
        service.allocateBed("P031", "B01");
        assertFalse(service.getWard().isBedAvailable("B01"));

        service.deletePatient("P031");

        assertTrue(service.getWard().isBedAvailable("B01"), "Bed should be freed when its occupant is deleted");
    }

    @Test
    void deletePatient_returnsFalseForUnknownId() {
        assertFalse(service.deletePatient("GHOST"));
    }

   
    @Test
    void allocateBed_succeedsForInpatient() {
        service.registerPatient("P040", "Ruth", "Bell", 55, "Female", "Fracture", PatientCategory.INPATIENT);

        service.allocateBed("P040", "B05");

        Inpatient inpatient = (Inpatient) service.searchPatient("P040").orElseThrow();
        assertEquals("B05", inpatient.getBedNumber());
        assertFalse(service.getWard().isBedAvailable("B05"));
    }

    @Test
    void allocateBed_throwsWhenPatientIsNotInpatient() {
        service.registerPatient("P041", "Carl", "Young", 25, "Male", "Checkup", PatientCategory.OUTPATIENT);

        assertThrows(IllegalStateException.class, () -> service.allocateBed("P041", "B02"));
    }

    @Test
    void allocateBed_preventsAllocatingAnOccupiedBed() {
        service.registerPatient("P042", "Eva", "Stone", 33, "Female", "Infection", PatientCategory.INPATIENT);
        service.registerPatient("P043", "Max", "Reed", 47, "Male", "Injury", PatientCategory.INPATIENT);
        service.allocateBed("P042", "B03");

        assertThrows(IllegalStateException.class, () -> service.allocateBed("P043", "B03"));
    }

    @Test
    void allocateBed_preventsAllocationWhenWardIsFull() {
        List<String> beds = service.getWard().getAvailableBeds();
        // Fill all 20 beds
        for (int i = 0; i < beds.size(); i++) {
            String id = "F" + i;
            service.registerPatient(id, "First" + i, "Last" + i, 30, "Male", "N/A", PatientCategory.INPATIENT);
            service.allocateBed(id, beds.get(i));
        }
        assertTrue(service.getWard().isFull());

        service.registerPatient("P044", "Overflow", "Patient", 20, "Female", "N/A", PatientCategory.INPATIENT);

        assertThrows(IllegalStateException.class, () -> service.allocateBed("P044", "B01"));
    }

    @Test
    void allocateBed_preventsDoubleAllocationForSamePatient() {
        service.registerPatient("P045", "Gina", "Ford", 29, "Female", "Fever", PatientCategory.INPATIENT);
        service.allocateBed("P045", "B06");

        assertThrows(IllegalStateException.class, () -> service.allocateBed("P045", "B07"));
    }

    
    @Test
    void releaseBed_freesBedAndClearsPatientRecord() {
        service.registerPatient("P050", "Owen", "Clarke", 38, "Male", "Observation", PatientCategory.INPATIENT);
        service.allocateBed("P050", "B08");

        service.releaseBed("P050");

        Inpatient inpatient = (Inpatient) service.searchPatient("P050").orElseThrow();
        assertFalse(inpatient.hasBedAllocated());
        assertTrue(service.getWard().isBedAvailable("B08"));
    }

    @Test
    void releaseBed_throwsWhenPatientHasNoBed() {
        service.registerPatient("P051", "Ivy", "Shaw", 26, "Female", "Checkup", PatientCategory.INPATIENT);

        assertThrows(IllegalStateException.class, () -> service.releaseBed("P051"));
    }

    // ---------------------------------------------------------------
    // Sorting
    // ---------------------------------------------------------------

    @Test
    void getSortedBySurname_ordersAlphabetically() {
        service.registerPatient("P060", "A", "Zebra", 20, "M", "N/A", PatientCategory.OUTPATIENT);
        service.registerPatient("P061", "B", "Apple", 20, "M", "N/A", PatientCategory.OUTPATIENT);
        service.registerPatient("P062", "C", "Mango", 20, "M", "N/A", PatientCategory.OUTPATIENT);

        List<Patient> sorted = service.getSortedBySurname();

        assertEquals("Apple", sorted.get(0).getLastName());
        assertEquals("Mango", sorted.get(1).getLastName());
        assertEquals("Zebra", sorted.get(2).getLastName());
    }

    @Test
    void getSortedByPatientId_ordersAlphanumerically() {
        service.registerPatient("P100", "A", "One", 20, "M", "N/A", PatientCategory.OUTPATIENT);
        service.registerPatient("P050", "B", "Two", 20, "M", "N/A", PatientCategory.OUTPATIENT);
        service.registerPatient("P075", "C", "Three", 20, "M", "N/A", PatientCategory.OUTPATIENT);

        List<Patient> sorted = service.getSortedByPatientId();

        assertEquals("P050", sorted.get(0).getPatientId());
        assertEquals("P075", sorted.get(1).getPatientId());
        assertEquals("P100", sorted.get(2).getPatientId());
    }

    // ---------------------------------------------------------------
    // Reports
    // ---------------------------------------------------------------

    @Test
    void occupancyReport_reflectsAllocations() {
        service.registerPatient("P070", "Dan", "Lee", 34, "Male", "N/A", PatientCategory.INPATIENT);
        service.registerPatient("P071", "Amy", "Lee", 29, "Female", "N/A", PatientCategory.INPATIENT);
        service.allocateBed("P070", "B01");
        service.allocateBed("P071", "B02");

        assertEquals(2, service.getOccupiedBedCount());
        assertEquals(10.0, service.getOccupancyPercentage(), 0.001); // 2 / 20 beds = 10%
    }
}


