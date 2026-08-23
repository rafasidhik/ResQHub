package com.resqhub.controller;

import java.time.LocalDateTime;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;
import com.resqhub.service.DisasterService;
import com.resqhub.util.InputParser;

/** Disaster screen controller: string/enum UI input -> typed service calls. */
public class DisasterController {

    private final DisasterService disasterService = new DisasterService();

    public ActionResult createDisaster(String title, DisasterType type,
                                       DisasterSeverity severity, String location,
                                       String populationText,
                                       String startText, String endText,
                                       String description) {
        try {
            int population = InputParser.parseInt(populationText,
                    "Affected population");
            LocalDateTime start = LocalDateTime.parse(startText == null ? ""
                    : startText.trim(), InputParser.DATE_TIME_FORMAT);
            LocalDateTime end = InputParser.parseOptionalDateTime(endText);

            Disaster disaster = disasterService.createDisaster(title, type,
                    severity, location, population, start, end, description);
            return ActionResult.successWithData(
                    "Disaster registered as #" + disaster.getId()
                            + " (" + disaster.getStatus().getLabel() + ")",
                    disaster);
        } catch (java.time.format.DateTimeParseException e) {
            return ActionResult.failure(
                    "Dates must use the format yyyy-MM-dd HH:mm");
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult closeDisaster(long disasterId) {
        try {
            Disaster closed = disasterService.closeDisaster(disasterId);
            return ActionResult.success("Disaster #" + closed.getId()
                    + " marked " + closed.getStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteDisaster(long disasterId) {
        try {
            disasterService.deleteDisaster(disasterId);
            return ActionResult.success("Disaster #" + disasterId
                    + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error deleting disaster: "
                    + e.getMessage());
        }
    }

    public ActionResult updateStatus(long disasterId, DisasterStatus status) {
        try {
            Disaster updated = disasterService.updateStatus(disasterId, status);
            return ActionResult.success("Disaster #" + updated.getId()
                    + " is now " + updated.getStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error updating status: "
                    + e.getMessage());
        }
    }

    public List<Disaster> getAllDisasters() throws DataAccessException {
        return disasterService.getAllDisasters();
    }

    public List<Disaster> getActiveDisasters() throws DataAccessException {
        return disasterService.getActiveDisasters();
    }

    /** Live search-as-you-type support for the search field. */
    public List<Disaster> search(String keyword) throws DataAccessException {
        return disasterService.search(keyword);
    }

    /** Row text for JTable rendering of a selected disaster. */
    public static Object[] toRow(Disaster d) {
        String ended = d.getEndDateTime() == null
                ? "ongoing" : d.getEndDateTime().toLocalDate().toString();
        return new Object[] {
                d.getId(),
                d.getTitle(),
                d.getDisasterType().getLabel(),
                d.getSeverity().getLabel(),
                d.getStatus().getLabel(),
                d.getLocation(),
                d.getAffectedPopulation(),
                ended
        };
    }

    public static String[] tableHeaders() {
        return new String[] {"ID", "Title", "Type", "Severity", "Status",
                "Location", "Affected", "Ended"};
    }
}
