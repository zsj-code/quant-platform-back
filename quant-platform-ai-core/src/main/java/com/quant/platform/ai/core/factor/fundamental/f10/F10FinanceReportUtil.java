package com.quant.platform.ai.core.factor.fundamental.f10;

import com.quant.platform.ai.core.client.dto.EastmoneyF10GBalanceRowDTO;
import com.quant.platform.ai.core.client.dto.EastmoneyF10GIncomeRowDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * F10 财务报表报告期筛选（按 {@code REPORT_DATE} 倒序列表）。
 */
public final class F10FinanceReportUtil {

    private static final DateTimeFormatter REPORT_DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private F10FinanceReportUtil() {
    }

    public static LocalDate parseReportDate(String reportDate) {
        if (reportDate == null || reportDate.isBlank()) {
            return null;
        }
        String s = reportDate.trim();
        try {
            if (s.length() >= 19) {
                return LocalDateTime.parse(s.substring(0, 19), REPORT_DATE_TIME_FMT).toLocalDate();
            }
            return LocalDate.parse(s.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isYearEndReport(LocalDate reportDate) {
        return reportDate != null
                && reportDate.getMonthValue() == 12
                && reportDate.getDayOfMonth() == 31;
    }

    public static EastmoneyF10GIncomeRowDTO pickLatestYearIncome(List<EastmoneyF10GIncomeRowDTO> rowsDesc) {
        return pickLatestYear(rowsDesc, F10FinanceReportUtil::isYearEndIncome, F10FinanceReportUtil::nonNullIncome);
    }

    public static List<EastmoneyF10GIncomeRowDTO> pickLatestTwoYearIncome(List<EastmoneyF10GIncomeRowDTO> rowsDesc) {
        return pickLatestNYear(rowsDesc, 2, F10FinanceReportUtil::isYearEndIncome, F10FinanceReportUtil::nonNullIncome);
    }

    public static EastmoneyF10GBalanceRowDTO pickLatestYearBalance(List<EastmoneyF10GBalanceRowDTO> rowsDesc) {
        return pickLatestYear(rowsDesc, F10FinanceReportUtil::isYearEndBalance, F10FinanceReportUtil::nonNullBalance);
    }

    public static List<EastmoneyF10GBalanceRowDTO> pickLatestTwoYearBalance(List<EastmoneyF10GBalanceRowDTO> rowsDesc) {
        return pickLatestNYear(rowsDesc, 2, F10FinanceReportUtil::isYearEndBalance, F10FinanceReportUtil::nonNullBalance);
    }

    public static EastmoneyF10GBalanceRowDTO findBalanceByReportDate(
            List<EastmoneyF10GBalanceRowDTO> rowsDesc, String reportDate) {
        if (rowsDesc == null || reportDate == null || reportDate.isBlank()) {
            return null;
        }
        String key = reportDate.trim();
        for (EastmoneyF10GBalanceRowDTO row : rowsDesc) {
            if (row != null && row.reportDate != null && key.equals(row.reportDate.trim())) {
                return row;
            }
        }
        LocalDate target = parseReportDate(key);
        if (target == null) {
            return null;
        }
        for (EastmoneyF10GBalanceRowDTO row : rowsDesc) {
            LocalDate d = row == null ? null : parseReportDate(row.reportDate);
            if (target.equals(d)) {
                return row;
            }
        }
        return null;
    }

    private static boolean isYearEndIncome(EastmoneyF10GIncomeRowDTO row) {
        return row != null && isYearEndReport(parseReportDate(row.reportDate()));
    }

    private static boolean isYearEndBalance(EastmoneyF10GBalanceRowDTO row) {
        return row != null && isYearEndReport(parseReportDate(row.reportDate));
    }

    private static boolean nonNullIncome(EastmoneyF10GIncomeRowDTO row) {
        return row != null;
    }

    private static boolean nonNullBalance(EastmoneyF10GBalanceRowDTO row) {
        return row != null;
    }

    private interface YearEndPredicate<T> {
        boolean test(T row);
    }

    private interface NonNullPredicate<T> {
        boolean test(T row);
    }

    private static <T> T pickLatestYear(List<T> rowsDesc, YearEndPredicate<T> yearEnd, NonNullPredicate<T> nonNull) {
        List<T> picked = pickLatestNYear(rowsDesc, 1, yearEnd, nonNull);
        return picked.isEmpty() ? null : picked.get(0);
    }

    private static <T> List<T> pickLatestNYear(
            List<T> rowsDesc, int n, YearEndPredicate<T> yearEnd, NonNullPredicate<T> nonNull) {
        if (rowsDesc == null || rowsDesc.isEmpty() || n < 1) {
            return List.of();
        }
        List<T> year = new ArrayList<>();
        for (T row : rowsDesc) {
            if (yearEnd.test(row)) {
                year.add(row);
            }
            if (year.size() >= n) {
                return year;
            }
        }
        if (year.size() >= n) {
            return year;
        }
        List<T> out = new ArrayList<>();
        for (T row : rowsDesc) {
            if (nonNull.test(row)) {
                out.add(row);
            }
            if (out.size() >= n) {
                break;
            }
        }
        return out;
    }
}
