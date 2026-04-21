package com.yutu.modules.contract.service;

import com.yutu.modules.model.entity.MerchantShop;
import com.yutu.modules.model.entity.SysUser;
import com.yutu.modules.model.entity.TourContract;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.model.entity.TourOrderTraveler;
import com.yutu.modules.model.entity.TourRoute;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ContractContentRenderer {
    private static final Pattern DAY_COUNT_PATTERN = Pattern.compile("([0-9]+|[一二三四五六七八九十两])日游");
    private static final Pattern UNDERLINE_PATTERN = Pattern.compile("[_＿—-]{2,}");
    private static final Pattern CHINESE_DATE_PATTERN = Pattern.compile("_{2,}年_{2,}月_{2,}日|＿{2,}年＿{2,}月＿{2,}日");

    private ContractContentRenderer() {
    }

    public static String render(String templateContent,
                                TourContract contract,
                                TourOrder order,
                                TourRoute route,
                                TourDepartureDate departureDate,
                                SysUser user,
                                MerchantShop merchantShop,
                                List<TourOrderTraveler> travelers) {
        if (!StringUtils.hasText(templateContent)) {
            return templateContent;
        }

        RenderContext context = buildContext(contract, order, route, departureDate, user, merchantShop, travelers);
        String content = replacePlaceholders(templateContent, context);

        String[] lines = content.split("\\r?\\n", -1);
        List<String> rendered = new ArrayList<>(lines.length);
        for (String line : lines) {
            rendered.add(renderLine(line, context));
        }
        return String.join("\n", rendered);
    }

    private static RenderContext buildContext(TourContract contract,
                                              TourOrder order,
                                              TourRoute route,
                                              TourDepartureDate departureDate,
                                              SysUser user,
                                              MerchantShop merchantShop,
                                              List<TourOrderTraveler> travelers) {
        RenderContext context = new RenderContext();
        context.contractNo = safe(contract == null ? null : contract.getContractNo(), "待生成");
        context.partyA = resolvePartyA(user, travelers);
        context.partyB = resolvePartyB(merchantShop);
        context.routeName = safe(route == null ? null : route.getRouteName(), "待确认");
        context.departDate = departureDate == null ? null : departureDate.getDepartDate();
        context.endDate = resolveEndDate(context.departDate, route);
        context.destination = resolveDestination(route);
        context.totalAmount = order == null ? null : firstNonNull(order.getPayAmount(), order.getTotalAmount());
        context.amountUpper = toChineseMoney(context.totalAmount);
        context.amountText = formatAmount(context.totalAmount);
        context.orderNo = safe(order == null ? null : order.getOrderNo(), "待生成");
        context.contractDate = formatChineseDate(contract == null ? null : toDate(contract.getCreateTime()));
        context.signDate = formatChineseDate(contract == null ? null : toDate(contract.getSignTime()));
        context.currentDate = formatChineseDate(LocalDate.now());
        context.merchantName = safe(merchantShop == null ? null : merchantShop.getShopName(), "待确认");
        context.merchantContact = safe(merchantShop == null ? null : merchantShop.getContactName(), "待确认");
        context.merchantPhone = safe(merchantShop == null ? null : merchantShop.getContactPhone(), "待确认");
        context.travelerNames = resolveTravelerNames(travelers);
        context.travelerCount = String.valueOf(travelers == null ? 0 : travelers.size());
        context.routeSummary = safe(route == null ? null : route.getSummary(), "待确认");
        return context;
    }

    private static String replacePlaceholders(String templateContent, RenderContext context) {
        Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("{{contractNo}}", context.contractNo);
        replacements.put("${contractNo}", context.contractNo);
        replacements.put("{{orderNo}}", context.orderNo);
        replacements.put("${orderNo}", context.orderNo);
        replacements.put("{{partyA}}", context.partyA);
        replacements.put("${partyA}", context.partyA);
        replacements.put("{{partyB}}", context.partyB);
        replacements.put("${partyB}", context.partyB);
        replacements.put("{{routeName}}", context.routeName);
        replacements.put("${routeName}", context.routeName);
        replacements.put("{{destination}}", context.destination);
        replacements.put("${destination}", context.destination);
        replacements.put("{{departDate}}", formatChineseDate(context.departDate));
        replacements.put("${departDate}", formatChineseDate(context.departDate));
        replacements.put("{{endDate}}", formatChineseDate(context.endDate));
        replacements.put("${endDate}", formatChineseDate(context.endDate));
        replacements.put("{{amount}}", context.amountText);
        replacements.put("${amount}", context.amountText);
        replacements.put("{{amountUpper}}", context.amountUpper);
        replacements.put("${amountUpper}", context.amountUpper);
        replacements.put("{{merchantName}}", context.merchantName);
        replacements.put("${merchantName}", context.merchantName);
        replacements.put("{{merchantContact}}", context.merchantContact);
        replacements.put("${merchantContact}", context.merchantContact);
        replacements.put("{{merchantPhone}}", context.merchantPhone);
        replacements.put("${merchantPhone}", context.merchantPhone);
        replacements.put("{{travelerNames}}", context.travelerNames);
        replacements.put("${travelerNames}", context.travelerNames);
        replacements.put("{{travelerCount}}", context.travelerCount);
        replacements.put("${travelerCount}", context.travelerCount);
        replacements.put("{{routeSummary}}", context.routeSummary);
        replacements.put("${routeSummary}", context.routeSummary);
        replacements.put("{{contractDate}}", context.contractDate);
        replacements.put("${contractDate}", context.contractDate);
        replacements.put("{{signDate}}", context.signDate);
        replacements.put("${signDate}", context.signDate);
        replacements.put("{{currentDate}}", context.currentDate);
        replacements.put("${currentDate}", context.currentDate);

        String result = templateContent;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static String renderLine(String rawLine, RenderContext context) {
        String line = rawLine;
        String trimmed = line.trim();

        line = replaceLabeledUnderline(line, "合同编号", context.contractNo);
        line = replaceLabeledUnderline(line, "订单号", context.orderNo);
        line = replaceLabeledUnderline(line, "甲方（旅游者）", context.partyA);
        line = replaceLabeledUnderline(line, "甲方（游客）", context.partyA);
        line = replaceLabeledUnderline(line, "乙方（旅行社）", context.partyB);
        line = replaceLabeledUnderline(line, "乙方（商家）", context.partyB);
        line = replaceLabeledUnderline(line, "旅游线路", context.routeName);
        line = replaceLabeledUnderline(line, "线路名称", context.routeName);
        line = replaceLabeledUnderline(line, "旅游目的地", context.destination);
        line = replaceLabeledUnderline(line, "目的地", context.destination);
        line = replaceLabeledUnderline(line, "联系电话", context.merchantPhone);
        line = replaceLabeledUnderline(line, "法定代表人/委托代理人", context.merchantContact);
        line = replaceLabeledUnderline(line, "甲方（签字）", context.partyA);
        line = replaceLabeledUnderline(line, "甲方（签字）", context.partyA);
        line = replaceLabeledUnderline(line, "乙方（盖章）", context.partyB);

        if (trimmed.startsWith("合同编号")) {
            return replaceValueAfterColon(line, context.contractNo);
        }
        if (trimmed.startsWith("甲方（旅游者）") || trimmed.startsWith("甲方（游客）")) {
            return replaceValueAfterColon(line, context.partyA);
        }
        if (trimmed.startsWith("乙方（旅行社）") || trimmed.startsWith("乙方（商家）")) {
            return replaceValueAfterColon(line, context.partyB);
        }
        if (trimmed.startsWith("（一）旅游线路") || trimmed.startsWith("旅游线路") || trimmed.startsWith("线路名称")) {
            return replaceValueAfterColon(line, context.routeName + (trimmed.contains("具体线路名称") ? "（具体线路名称）" : ""));
        }
        if (trimmed.startsWith("（二）出发日期") || trimmed.startsWith("出发日期")) {
            return replaceDateAfterColon(line, formatChineseDate(context.departDate));
        }
        if (trimmed.startsWith("（三）结束日期") || trimmed.startsWith("结束日期")) {
            return replaceDateAfterColon(line, formatChineseDate(context.endDate));
        }
        if (trimmed.startsWith("（四）旅游目的地") || trimmed.startsWith("旅游目的地") || trimmed.startsWith("目的地")) {
            return replaceValueAfterColon(line, context.destination);
        }
        if (trimmed.startsWith("（一）旅游费用总额") || trimmed.startsWith("合同金额") || trimmed.startsWith("旅游费用总额")) {
            return replaceAmountLine(line, context.amountUpper, context.amountText);
        }
        if (trimmed.startsWith("签署日期")) {
            return replaceDateAfterColon(line, context.signDate);
        }
        if (trimmed.startsWith("联系电话")) {
            return replaceValueAfterColon(line, context.merchantPhone);
        }
        return line;
    }

    private static String replaceLabeledUnderline(String line, String label, String value) {
        if (!StringUtils.hasText(line) || !StringUtils.hasText(label) || !StringUtils.hasText(value)) {
            return line;
        }
        String normalized = line.replace("：", ":");
        String token = label.replace("：", ":");
        if (!normalized.contains(token)) {
            return line;
        }
        if (!UNDERLINE_PATTERN.matcher(line).find() && !CHINESE_DATE_PATTERN.matcher(line).find()) {
            return line;
        }
        int colonIndex = Math.max(line.indexOf('：'), line.indexOf(':'));
        if (colonIndex < 0) {
            return line;
        }
        return line.substring(0, colonIndex + 1) + value;
    }

    private static String replaceValueAfterColon(String line, String value) {
        int colonIndex = Math.max(line.indexOf('：'), line.indexOf(':'));
        if (colonIndex < 0) {
            return line;
        }
        return line.substring(0, colonIndex + 1) + value;
    }

    private static String replaceDateAfterColon(String line, String dateText) {
        if (!StringUtils.hasText(dateText)) {
            return line;
        }
        String replaced = CHINESE_DATE_PATTERN.matcher(line).replaceAll(dateText);
        if (!ObjectsEqual(replaced, line)) {
            return replaced;
        }
        return replaceValueAfterColon(line, dateText);
    }

    private static String replaceAmountLine(String line, String amountUpper, String amountText) {
        String result = line;
        result = result.replaceAll("人民币（大写）[_＿—-]{2,}元整", "人民币（大写）" + amountUpper);
        result = result.replaceAll("（￥[_＿—-]{2,}元）", "（￥" + amountText + "元）");
        if (!ObjectsEqual(result, line)) {
            return result;
        }
        return replaceValueAfterColon(line, "人民币（大写）" + amountUpper + "（￥" + amountText + "元）");
    }

    private static String resolvePartyA(SysUser user, List<TourOrderTraveler> travelers) {
        String travelerNames = resolveTravelerNames(travelers);
        if (StringUtils.hasText(travelerNames) && !"待确认".equals(travelerNames)) {
            return travelerNames;
        }
        if (user == null) {
            return "待确认";
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        return safe(user.getUsername(), "待确认");
    }

    private static String resolvePartyB(MerchantShop merchantShop) {
        if (merchantShop == null) {
            return "待确认";
        }
        if (StringUtils.hasText(merchantShop.getShopName())) {
            return merchantShop.getShopName().trim();
        }
        return safe(merchantShop.getContactName(), "待确认");
    }

    private static String resolveTravelerNames(List<TourOrderTraveler> travelers) {
        if (travelers == null || travelers.isEmpty()) {
            return "待确认";
        }
        String names = travelers.stream()
                .map(TourOrderTraveler::getTravelerName)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining("、"));
        return safe(names, "待确认");
    }

    private static LocalDate resolveEndDate(LocalDate departDate, TourRoute route) {
        if (departDate == null) {
            return null;
        }
        int dayCount = resolveDayCount(route);
        return departDate.plusDays(Math.max(dayCount - 1, 0));
    }

    private static int resolveDayCount(TourRoute route) {
        String text = "";
        if (route != null) {
            text = safe(route.getRouteName(), "") + " " + safe(route.getSummary(), "");
        }
        Matcher matcher = DAY_COUNT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return 1;
        }
        String token = matcher.group(1);
        if (!StringUtils.hasText(token)) {
            return 1;
        }
        if (token.matches("\\d+")) {
            return Math.max(Integer.parseInt(token), 1);
        }
        switch (token) {
            case "一":
                return 1;
            case "二":
            case "两":
                return 2;
            case "三":
                return 3;
            case "四":
                return 4;
            case "五":
                return 5;
            case "六":
                return 6;
            case "七":
                return 7;
            case "八":
                return 8;
            case "九":
                return 9;
            case "十":
                return 10;
            default:
                return 1;
        }
    }

    private static String resolveDestination(TourRoute route) {
        if (route == null) {
            return "待确认";
        }
        String routeName = safe(route.getRouteName(), "");
        if (StringUtils.hasText(routeName)) {
            String normalized = routeName
                    .replaceAll("([0-9]+|[一二三四五六七八九十两])日游.*$", "")
                    .replaceAll("([0-9]+|[一二三四五六七八九十两])天.*$", "")
                    .trim();
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return safe(route.getSummary(), "待确认");
    }

    private static String formatChineseDate(LocalDate date) {
        if (date == null) {
            return "待确认";
        }
        return String.format("%d年%d月%d日", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private static LocalDate toDate(LocalDateTime value) {
        return value == null ? null : value.toLocalDate();
    }

    private static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        return normalized.scale() < 0 ? normalized.setScale(0, RoundingMode.DOWN).toPlainString() : normalized.toPlainString();
    }

    private static String toChineseMoney(BigDecimal amount) {
        if (amount == null) {
            return "零元整";
        }
        String[] digit = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        String[] unit = {"", "拾", "佰", "仟"};
        String[] sectionUnit = {"", "万", "亿", "万亿"};

        long value = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        if (value == 0) {
            return "零元整";
        }

        StringBuilder builder = new StringBuilder();
        int sectionIndex = 0;
        boolean needZero = false;
        while (value > 0) {
            int section = (int) (value % 10000);
            if (section == 0) {
                if (builder.length() > 0) {
                    needZero = true;
                }
            } else {
                String sectionText = convertSection(section, digit, unit);
                if (needZero) {
                    builder.insert(0, "零");
                    needZero = false;
                }
                builder.insert(0, sectionText + sectionUnit[sectionIndex]);
            }
            value /= 10000;
            sectionIndex++;
        }
        return builder.append("元整").toString().replaceAll("零+", "零").replaceAll("零元", "元");
    }

    private static String convertSection(int section, String[] digit, String[] unit) {
        StringBuilder builder = new StringBuilder();
        int unitPos = 0;
        boolean zero = true;
        while (section > 0) {
            int value = section % 10;
            if (value == 0) {
                if (!zero) {
                    zero = true;
                    builder.insert(0, digit[0]);
                }
            } else {
                zero = false;
                builder.insert(0, digit[value] + unit[unitPos]);
            }
            unitPos++;
            section /= 10;
        }
        return builder.toString().replaceAll("零$", "");
    }

    private static BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private static String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static boolean ObjectsEqual(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }

    private static final class RenderContext {
        private String contractNo;
        private String orderNo;
        private String partyA;
        private String partyB;
        private String routeName;
        private LocalDate departDate;
        private LocalDate endDate;
        private String destination;
        private BigDecimal totalAmount;
        private String amountUpper;
        private String amountText;
        private String contractDate;
        private String signDate;
        private String currentDate;
        private String merchantName;
        private String merchantContact;
        private String merchantPhone;
        private String travelerNames;
        private String travelerCount;
        private String routeSummary;
    }
}
