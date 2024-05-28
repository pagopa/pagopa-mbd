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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping
@Validated
@RequiredArgsConstructor
@Tag(name = "Recovery", description = "Recover one or more days of unprocessed MBD flows")
@Slf4j
public class RecoveryController {

    private GenerateReportingService generateReportingService;

    @Operation(summary = "", description = "", security = {@SecurityRequirement(name = "ApiKey")}, tags = {"Redirect"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Recovery FdR MDB taken", content = @Content(schema = @Schema()))
    })
    @PostMapping(value = "/")
    @ResponseBody
    public ResponseEntity recover(
                           @RequestParam("from") LocalDate from,
                           @RequestParam("to") LocalDate to) {
        generateReportingService.recovery(from, to);
        return new ResponseEntity(HttpStatus.CREATED);
    }

}