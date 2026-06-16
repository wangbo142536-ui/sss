package com.zswy.shipsupply.procurement.materials;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

final class MaterialDemandNo {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private MaterialDemandNo() {
    }

    static String next(LocalDate inquiryDate, String lastDemandNo) {
        String prefix = "REQ-" + FORMATTER.format(inquiryDate) + "-";
        int nextSequence = 1;
        if (lastDemandNo != null && lastDemandNo.startsWith(prefix) && lastDemandNo.length() >= prefix.length() + 3) {
            String sequence = lastDemandNo.substring(prefix.length());
            try {
                nextSequence = Integer.parseInt(sequence) + 1;
            } catch (NumberFormatException ignored) {
                nextSequence = 1;
            }
        }
        return prefix + String.format("%03d", nextSequence);
    }
}
