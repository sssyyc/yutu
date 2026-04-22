package com.yutu.common.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.modules.contract.service.ContractContentRenderer;
import com.yutu.modules.model.entity.ContractTemplate;
import com.yutu.modules.model.entity.MerchantShop;
import com.yutu.modules.model.entity.SysUser;
import com.yutu.modules.model.entity.TourCategory;
import com.yutu.modules.model.entity.TourContract;
import com.yutu.modules.model.entity.TourDepartureDate;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.model.entity.TourOrderTraveler;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.mapper.ContractTemplateMapper;
import com.yutu.modules.model.mapper.MerchantShopMapper;
import com.yutu.modules.model.mapper.SysUserMapper;
import com.yutu.modules.model.mapper.TourCategoryMapper;
import com.yutu.modules.model.mapper.TourContractMapper;
import com.yutu.modules.model.mapper.TourDepartureDateMapper;
import com.yutu.modules.model.mapper.TourOrderMapper;
import com.yutu.modules.model.mapper.TourOrderTravelerMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Order(20)
public class RouteContractBackfillRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(RouteContractBackfillRunner.class);

    private final TourRouteMapper tourRouteMapper;
    private final TourCategoryMapper tourCategoryMapper;
    private final ContractTemplateMapper contractTemplateMapper;
    private final TourContractMapper tourContractMapper;
    private final TourOrderMapper tourOrderMapper;
    private final TourDepartureDateMapper tourDepartureDateMapper;
    private final SysUserMapper sysUserMapper;
    private final MerchantShopMapper merchantShopMapper;
    private final TourOrderTravelerMapper tourOrderTravelerMapper;

    public RouteContractBackfillRunner(TourRouteMapper tourRouteMapper,
                                       TourCategoryMapper tourCategoryMapper,
                                       ContractTemplateMapper contractTemplateMapper,
                                       TourContractMapper tourContractMapper,
                                       TourOrderMapper tourOrderMapper,
                                       TourDepartureDateMapper tourDepartureDateMapper,
                                       SysUserMapper sysUserMapper,
                                       MerchantShopMapper merchantShopMapper,
                                       TourOrderTravelerMapper tourOrderTravelerMapper) {
        this.tourRouteMapper = tourRouteMapper;
        this.tourCategoryMapper = tourCategoryMapper;
        this.contractTemplateMapper = contractTemplateMapper;
        this.tourContractMapper = tourContractMapper;
        this.tourOrderMapper = tourOrderMapper;
        this.tourDepartureDateMapper = tourDepartureDateMapper;
        this.sysUserMapper = sysUserMapper;
        this.merchantShopMapper = merchantShopMapper;
        this.tourOrderTravelerMapper = tourOrderTravelerMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            backfillRouteTemplateBindings();
            backfillContractContent();
        } catch (Exception ex) {
            log.warn("Failed to backfill route-contract linkage", ex);
        }
    }

    private void backfillRouteTemplateBindings() {
        List<TourRoute> routes = tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>().orderByAsc(TourRoute::getId));
        if (routes.isEmpty()) {
            return;
        }

        Map<Long, TourCategory> categoryMap = tourCategoryMapper.selectList(new LambdaQueryWrapper<TourCategory>())
                .stream()
                .collect(Collectors.toMap(TourCategory::getId, item -> item, (a, b) -> a));

        List<ContractTemplate> templates = contractTemplateMapper.selectList(new LambdaQueryWrapper<ContractTemplate>()
                .eq(ContractTemplate::getStatus, 1)
                .orderByDesc(ContractTemplate::getUpdateTime)
                .orderByDesc(ContractTemplate::getId));

        Map<String, ContractTemplate> preferredTemplateMap = new HashMap<>();
        for (ContractTemplate template : templates) {
            String key = buildTemplateKey(template.getApplyScope(), template.getTemplateType());
            preferredTemplateMap.putIfAbsent(key, template);
        }

        int updatedCount = 0;
        for (TourRoute route : routes) {
            TourCategory category = categoryMap.get(route.getCategoryId());
            String scope = category == null ? null : category.getCategoryName();
            boolean changed = false;

            if (route.getStandardTemplateId() == null) {
                ContractTemplate standard = preferredTemplateMap.get(buildTemplateKey(scope, "STANDARD"));
                if (standard != null) {
                    route.setStandardTemplateId(standard.getId());
                    changed = true;
                }
            }
            if (route.getRouteTemplateId() == null) {
                ContractTemplate appendix = preferredTemplateMap.get(buildTemplateKey(scope, "ROUTE"));
                if (appendix != null) {
                    route.setRouteTemplateId(appendix.getId());
                    changed = true;
                }
            }

            if (changed) {
                tourRouteMapper.updateById(route);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            log.info("Backfilled contract template bindings for {} routes", updatedCount);
        }
    }

    private void backfillContractContent() {
        List<TourContract> contracts = tourContractMapper.selectList(new LambdaQueryWrapper<TourContract>()
                .orderByAsc(TourContract::getId));
        if (contracts.isEmpty()) {
            return;
        }

        Map<Long, TourOrder> orderMap = listToMap(tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()), TourOrder::getId);
        Map<Long, TourRoute> routeMap = listToMap(tourRouteMapper.selectList(new LambdaQueryWrapper<TourRoute>()), TourRoute::getId);
        Map<Long, TourDepartureDate> departureDateMap = listToMap(
                tourDepartureDateMapper.selectList(new LambdaQueryWrapper<TourDepartureDate>()),
                TourDepartureDate::getId
        );
        Map<Long, SysUser> userMap = listToMap(sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()), SysUser::getId);
        Map<Long, MerchantShop> merchantMap = listToMap(
                merchantShopMapper.selectList(new LambdaQueryWrapper<MerchantShop>()),
                MerchantShop::getId
        );
        Map<Long, ContractTemplate> templateMap = listToMap(
                contractTemplateMapper.selectList(new LambdaQueryWrapper<ContractTemplate>()),
                ContractTemplate::getId
        );
        Map<Long, List<TourOrderTraveler>> travelerMap = tourOrderTravelerMapper.selectList(new LambdaQueryWrapper<TourOrderTraveler>()
                        .orderByAsc(TourOrderTraveler::getCreateTime)
                        .orderByAsc(TourOrderTraveler::getId))
                .stream()
                .filter(item -> item.getOrderId() != null)
                .collect(Collectors.groupingBy(TourOrderTraveler::getOrderId));

        int updatedCount = 0;
        for (TourContract contract : contracts) {
            TourOrder order = orderMap.get(contract.getOrderId());
            if (order == null) {
                continue;
            }
            TourRoute route = routeMap.get(order.getRouteId());
            if (route == null || route.getStandardTemplateId() == null || route.getRouteTemplateId() == null) {
                continue;
            }

            ContractTemplate standardTemplate = templateMap.get(route.getStandardTemplateId());
            ContractTemplate routeTemplate = templateMap.get(route.getRouteTemplateId());
            if (standardTemplate == null || routeTemplate == null) {
                continue;
            }

            String newContent = buildContractContent(
                    contract,
                    order,
                    route,
                    departureDateMap.get(order.getDepartDateId()),
                    userMap.get(order.getUserId()),
                    merchantMap.get(order.getMerchantId()),
                    travelerMap.getOrDefault(order.getId(), new ArrayList<>()),
                    standardTemplate,
                    routeTemplate
            );

            boolean changed = false;
            if (!Objects.equals(contract.getTemplateId(), standardTemplate.getId())) {
                contract.setTemplateId(standardTemplate.getId());
                changed = true;
            }
            if (!Objects.equals(contract.getContractContent(), newContent)) {
                contract.setContractContent(newContent);
                changed = true;
            }
            if (order.getOrderNo() != null) {
                String expectedTitle = "豫途旅游服务合同-" + order.getOrderNo();
                if (!Objects.equals(contract.getContractTitle(), expectedTitle)) {
                    contract.setContractTitle(expectedTitle);
                    changed = true;
                }
            }
            if (changed) {
                tourContractMapper.updateById(contract);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            log.info("Backfilled contract content for {} contracts", updatedCount);
        }
    }

    private String buildContractContent(TourContract contract,
                                        TourOrder order,
                                        TourRoute route,
                                        TourDepartureDate departureDate,
                                        SysUser user,
                                        MerchantShop merchantShop,
                                        List<TourOrderTraveler> travelers,
                                        ContractTemplate standardTemplate,
                                        ContractTemplate routeTemplate) {
        List<String> sections = new ArrayList<>();
        sections.add(ContractContentRenderer.render(
                standardTemplate.getTemplateContent(),
                contract,
                order,
                route,
                departureDate,
                user,
                merchantShop,
                travelers
        ));
        sections.add(ContractContentRenderer.render(
                routeTemplate.getTemplateContent(),
                contract,
                order,
                route,
                departureDate,
                user,
                merchantShop,
                travelers
        ));
        return sections.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.joining("\n\n------------------------------\n\n"));
    }

    private String buildTemplateKey(String scope, String type) {
        return (scope == null ? "" : scope.trim()) + "#" + (type == null ? "" : type.trim());
    }

    private <T, K> Map<K, T> listToMap(List<T> list, java.util.function.Function<T, K> keyExtractor) {
        return list.stream()
                .filter(Objects::nonNull)
                .filter(item -> keyExtractor.apply(item) != null)
                .collect(Collectors.toMap(
                        keyExtractor,
                        item -> item,
                        (left, right) -> left,
                        HashMap::new
                ));
    }
}
