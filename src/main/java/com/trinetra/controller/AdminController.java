package com.trinetra.controller;

import com.trinetra.dto.AdminResponseRequest;
import com.trinetra.dto.ComplaintResponse;
import com.trinetra.dto.ReportResponse;
import com.trinetra.dto.StatusUpdateRequest;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.trinetra.exception.BadRequestException;
import com.trinetra.model.ComplaintStatus;
import com.trinetra.service.AdminService;
import com.trinetra.service.ComplaintService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ComplaintService complaintService;

    @GetMapping("/reports")
    public ResponseEntity<?> getReports(
            @RequestParam(value = "status", required = false) String status
    ) {
        try {
            List<ReportResponse> reports = Optional.ofNullable(adminService.getAllReports(parseStatus(status))).orElse(List.of());
            return successResponse(reports);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @PutMapping("/report/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        try {
            ReportResponse updated = adminService.updateStatus(id, request);
            return successResponse(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @PostMapping("/respond/{reportId}")
    public ResponseEntity<?> respond(
            @PathVariable UUID reportId,
            @Valid @RequestBody AdminResponseRequest request,
            Principal principal
    ) {
        try {
            String actor = Optional.ofNullable(principal).map(Principal::getName).orElse("SYSTEM");
            ReportResponse response = adminService.respondToReport(reportId, request, actor);
            return successResponse(response);
        } catch (Exception e) {
            e.printStackTrace();
            return errorResponse(e);
        }
    }

    @GetMapping("/reports/export/csv")
    public void exportCSV(HttpServletResponse response) throws IOException {
        try {
            List<ComplaintResponse> list = Optional.ofNullable(complaintService.getAllComplaints()).orElse(List.of());

            response.setContentType("text/csv");
            response.setHeader("Content-Disposition", "attachment; filename=complaints_report.csv");

            PrintWriter writer = response.getWriter();
            writer.println("Tracking ID,Title,Description,Category,Priority,Status,Date");

            for (ComplaintResponse c : list) {
                writer.println(
                        toCsv(c.getTrackingId()) + ","
                                + toCsv(c.getTitle()) + ","
                                + toCsv(c.getDescription()) + ","
                                + toCsv(c.getCategory()) + ","
                                + toCsv(c.getPriority()) + ","
                                + toCsv(c.getStatus()) + ","
                                + toCsv(c.getCreatedAt())
                );
            }

            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            response.getWriter().write("{\"data\":[],\"error\":\"" + Optional.ofNullable(e.getMessage()).orElse("Unexpected error") + "\"}");
        }
    }

    @GetMapping("/reports/export/pdf")
    public void exportPDF(HttpServletResponse response) throws Exception {
        List<ComplaintResponse> list = Optional.ofNullable(complaintService.getAllComplaints()).orElse(List.of());

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=complaints_report.pdf");

        Document document = new Document();
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();
        document.add(new Paragraph("Complaint Report\n\n"));

        for (ComplaintResponse c : list) {
            document.add(new Paragraph(
                    "ID: " + Optional.ofNullable(c.getTrackingId()).orElse("N/A")
                            + "\nTitle: " + Optional.ofNullable(c.getTitle()).orElse("N/A")
                            + "\nCategory: " + Optional.ofNullable(c.getCategory()).orElse(null)
                            + "\nPriority: " + Optional.ofNullable(c.getPriority()).orElse(null)
                            + "\nStatus: " + Optional.ofNullable(c.getStatus()).orElse(null)
                            + "\nDate: " + Optional.ofNullable(c.getCreatedAt()).orElse(null)
                            + "\n---------------------------\n"
            ));
        }

        document.close();
    }

    private ComplaintStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ComplaintStatus.from(value);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> successResponse(Object data) {
        Object safeData = Optional.ofNullable(data).orElse(List.of());
        return ResponseEntity.ok(Map.of(
                "data", safeData,
                "message", "success"
        ));
    }

    private ResponseEntity<Map<String, Object>> errorResponse(Exception e) {
        return ResponseEntity.ok(Map.of(
                "data", List.of(),
                "error", Optional.ofNullable(e.getMessage()).orElse("Unexpected error")
        ));
    }

    private String toCsv(Object value) {
        String text = value == null ? "" : value.toString();
        String escaped = text.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}