package ru.mirea.hrsystem.dto;

import java.util.List;

public final class ImportDtos {
    private ImportDtos() {}

    public enum ImportMode {
        CREATE_ONLY,
        UPSERT_BY_CODE
    }

    public record RowIssue(int row, String message) {}

    public record ImportPreview(int totalRows, int validRows, int invalidRows, List<RowIssue> issues) {}

    public record ImportResult(int applied, int skipped, List<RowIssue> issues) {}
}
