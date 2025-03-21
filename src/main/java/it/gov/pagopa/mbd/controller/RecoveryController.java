package it.gov.pagopa.mbd.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.mbd.service.GenerateReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@Tag(name = "Recovery", description = "Recover one or more days of unprocessed MBD flows")
@Validated
@RequiredArgsConstructor
public class RecoveryController {

    private final GenerateReportingService generateReportingService;

    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Recovery"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Recovery FdR MDB taken", content = @Content(schema = @Schema()))
    })
    @PatchMapping(value = "/recover", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> recover(
                           @RequestParam("from") LocalDate from,
                           @RequestParam("to") LocalDate to,
                           @RequestParam(value="organizations", required = false) String[] organizations) {
        generateReportingService.recovery(from, to, organizations);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
