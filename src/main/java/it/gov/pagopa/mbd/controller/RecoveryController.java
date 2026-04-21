package it.gov.pagopa.mbd.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.mbd.exception.MBDReportingException;
import it.gov.pagopa.mbd.service.GenerateReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@Tag(
        name = "Recovery",
        description = "API used to manually recover one or more days of unprocessed MBD flows"
)
@Validated
@RequiredArgsConstructor
public class RecoveryController {

    private final GenerateReportingService generateReportingService;

    @Operation(
            summary = "Start the MBD flows recovery for a date range",
            description = """
                    Starts an asynchronous recovery process to regenerate MBD reporting flows
                    for one or more business days.

                    Date format:
                    - The `from` and `to` request parameters must use the ISO date format `yyyy-MM-dd`.
                    - Example: `2025-06-01`.

                    Date range:
                    - The range is inclusive on both bounds.
                    - Therefore, `from=2025-06-01&to=2025-06-03` evaluates:
                      `2025-06-01`, `2025-06-02`, and `2025-06-03`.

                    Current-day behavior:
                    - If one of the requested dates is equal to the current system date,
                      that date is not processed directly.
                    - Instead, the service processes the previous day, consistently with
                      the scheduled generation logic.
                    - Example: if today is `2025-06-10` and the request is
                      `from=2025-06-10&to=2025-06-10`, the processed date will be `2025-06-09`.

                    Duplicate processing dates:
                    - Processing dates are deduplicated.
                    - Example: if today is `2025-06-10` and the request is
                      `from=2025-06-09&to=2025-06-10`, both requested dates resolve to
                      `2025-06-09`, which is processed only once.

                    Organization filter:
                    - The `organizations` parameter is optional.
                    - If omitted, all creditor institutions available in cache and having MBD data
                      in the requested period are considered.
                    - If provided, only the specified creditor institutions are processed,
                      provided that they have MBD data in the requested period.
                    - The parameter can be passed multiple times:
                      `organizations=77777777777&organizations=88888888888`.

                    Examples:
                    - Single-day recovery:
                      `PATCH /recover?from=2025-06-01&to=2025-06-01`

                    - Multi-day recovery:
                      `PATCH /recover?from=2025-06-01&to=2025-06-03`

                    - Recovery including the current system date:
                      assuming today is `2025-06-10`,
                      `PATCH /recover?from=2025-06-10&to=2025-06-10`
                      processes `2025-06-09`.

                    - Recovery filtered by creditor institution:
                      `PATCH /recover?from=2025-06-01&to=2025-06-03&organizations=77777777777&organizations=88888888888`

                    The API immediately accepts the request.
                    The actual recovery process is executed asynchronously.
                    Processing errors occurring after acceptance are logged by the asynchronous job
                    and are not returned in the HTTP response.
                    """,
            security = {@SecurityRequirement(name = "ApiKey")},
            tags = {"Recovery"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "202",
                    description = "MBD recovery request accepted and scheduled for asynchronous processing",
                    content = @Content(schema = @Schema())
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request parameters. Check the date format and the requested range",
                    content = @Content(schema = @Schema())
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Missing or invalid API key",
                    content = @Content(schema = @Schema())
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error while accepting the recovery request",
                    content = @Content(schema = @Schema())
            )
    })
    @PatchMapping(value = "/recover", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> recover(
            @Parameter(
                    description = """
                            Start date of the recovery range.
                            Required format: `yyyy-MM-dd`.
                            The lower bound is included in the recovery interval.

                            If this date is equal to the current system date, the service
                            processes the previous day instead of the current day.
                            """,
                    example = "2025-06-01",
                    required = true
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @Parameter(
                    description = """
                            End date of the recovery range.
                            Required format: `yyyy-MM-dd`.
                            The upper bound is included in the recovery interval.
                            It must be greater than or equal to `from`.

                            If this date is equal to the current system date, the service
                            processes the previous day instead of the current day.
                            """,
                    example = "2025-06-03",
                    required = true
            )
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @Parameter(
                    description = """
                            Optional list of creditor institution fiscal codes to process.
                            If omitted, the recovery is executed for all creditor institutions
                            having MBD data in the requested period.

                            The parameter can be provided multiple times in the query string.

                            Example:
                            `organizations=77777777777&organizations=88888888888`
                            """,
                    example = "[\"77777777777\", \"88888888888\"]"
            )
            @RequestParam(required = false)
            String[] organizations
    ) throws MBDReportingException {

        /*
         * Validate the requested date range before starting the asynchronous recovery.
         *
         * Validating here makes the HTTP contract explicit and allows the API to return
         * 400 Bad Request when `from` is greater than `to`.
         */
        if (from.isAfter(to)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "`from` must be less than or equal to `to`"
            );
        }

        /*
         * The recovery process is asynchronous.
         *
         * This endpoint only accepts the request and delegates the actual processing
         * to the service layer.
         *
         * Date range behavior:
         * - both `from` and `to` are included;
         * - if one of the requested dates is equal to the current system date,
         *   that date is not processed directly. Instead, the service processes the previous day;
         * - duplicated processing dates are automatically removed by the service.
         *
         * Example:
         * - assuming today is 2025-06-10,
         *   PATCH /recover?from=2025-06-09&to=2025-06-10
         *   will process 2025-06-09 only once.
         *
         * Organization filter behavior:
         * - if `organizations` is null or empty, all eligible creditor institutions are processed;
         * - otherwise, only the requested creditor institutions are considered.
         */
        generateReportingService.recovery(from, to, organizations);

        /*
         * HTTP 202 Accepted is returned because the request has been accepted for processing,
         * but the recovery execution is completed asynchronously after the response.
         */
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
}