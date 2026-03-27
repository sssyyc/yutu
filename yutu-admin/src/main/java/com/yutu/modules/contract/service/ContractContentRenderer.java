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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ContractContentRenderer {
    private static final Pattern DAY_COUNT_PATTERN = Pattern.compile("([0-9]+|[一二三四五六七八九十两])日游");

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

        String[] lines = templateContent.split("\\r?\\n", -1);
        List<String> rendered = new ArrayList<>(lines.length);

        String contractNo = safe(contract == null ? null : contract.getContractNo(), "待生成");
        String partyA = resolvePartyA(user, travelers);
        String partyB = resolvePartyB(merchantShop);
        String routeName = safe(route == null ? null : route.getRouteName(), "待确认");
        LocalDate departDate = departureDate == null ? null : departureDate.getDepartDate();
        LocalDate endDate = resolveEndDate(departDate, route);
        String destination = resolveDestination(route);
        BigDecimal totalAmount = order == null ? null : firstNonNull(order.getPayAmount(), order.getTotalAmount());

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("合同编号")) {
                rendered.add("合同编号：" + contractNo);
            } else if (trimmed.startsWith("甲方（旅游者）")) {
                rendered.add("甲方（旅游者）：" + partyA);
            } else if (trimmed.startsWith("乙方（旅行社）")) {
                rendered.add("乙方（旅行社）：" + partyB);
            } else if (trimmed.startsWith("（一）旅游线路")) {
                rendered.add("（一）旅游线路：" + routeName + "（具体线路名称）");
            } else if (trimmed.startsWith("（二）出发日期")) {
                rendered.add("（二）出发日期：" + formatChineseDate(departDate));
            } else if (trimmed.startsWith("（三）结束日期")) {
                rendered.add("（三）结束日期：" + formatChineseDate(endDate));
            } else if (trimmed.startsWith("（四）旅游目的地")) {
                rendered.add("（四）旅游目的地：" + destination);
            } else if (trimmed.startsWith("（一）旅游费用总额")) {
                rendered.add("（一）旅游费用总额：人民币（大写）" + toChineseMoney(totalAmount)
                        + "（￥" + formatAmount(totalAmount) + "元）");
            } else {
                rendered.add(line);
            }
        }
        return String.join("\n", rendered);
    }

    private static String resolvePartyA(SysUser user, List<TourOrderTraveler> travelers) {
        String travelerNames = travelers == null ? "" : travelers.stream()
                .map(TourOrderTraveler::getTravelerName)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining("、"));
        if (StringUtils.hasText(travelerNames)) {
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
                    .replace('·', '、')
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
        return builder.toString().replaceAll("零+$", "");
    }

    private static BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private static String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
