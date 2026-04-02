package com.yutu.modules.complaint.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yutu.common.context.UserContext;
import com.yutu.common.exception.BizException;
import com.yutu.modules.complaint.dto.ComplaintActionRequest;
import com.yutu.modules.complaint.dto.ComplaintCreateRequest;
import com.yutu.modules.complaint.vo.ComplaintRouteOptionVO;
import com.yutu.modules.model.entity.ComplaintFlow;
import com.yutu.modules.model.entity.ComplaintOrder;
import com.yutu.modules.model.entity.MerchantShop;
import com.yutu.modules.model.entity.TourContract;
import com.yutu.modules.model.entity.TourOrder;
import com.yutu.modules.model.entity.TourRoute;
import com.yutu.modules.model.mapper.ComplaintFlowMapper;
import com.yutu.modules.model.mapper.ComplaintOrderMapper;
import com.yutu.modules.model.mapper.MerchantShopMapper;
import com.yutu.modules.model.mapper.TourContractMapper;
import com.yutu.modules.model.mapper.TourOrderMapper;
import com.yutu.modules.model.mapper.TourRouteMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ComplaintService {
    private final ComplaintOrderMapper complaintOrderMapper;
    private final ComplaintFlowMapper complaintFlowMapper;
    private final TourOrderMapper tourOrderMapper;
    private final TourContractMapper tourContractMapper;
    private final MerchantShopMapper merchantShopMapper;
    private final TourRouteMapper tourRouteMapper;

    public ComplaintService(ComplaintOrderMapper complaintOrderMapper,
                            ComplaintFlowMapper complaintFlowMapper,
                            TourOrderMapper tourOrderMapper,
                            TourContractMapper tourContractMapper,
                            MerchantShopMapper merchantShopMapper,
                            TourRouteMapper tourRouteMapper) {
        this.complaintOrderMapper = complaintOrderMapper;
        this.complaintFlowMapper = complaintFlowMapper;
        this.tourOrderMapper = tourOrderMapper;
        this.tourContractMapper = tourContractMapper;
        this.merchantShopMapper = merchantShopMapper;
        this.tourRouteMapper = tourRouteMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(ComplaintCreateRequest request) {
        Long userId = currentUserId();
        TourOrder order = tourOrderMapper.selectById(request.getOrderId());
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new BizException(404, "订单不存在");
        }
        if (!"COMPLETED".equals(order.getOrderStatus())) {
            throw new BizException(400, "只有完成出行后的路线才可以投诉");
        }

        TourContract contract = tourContractMapper.selectOne(new LambdaQueryWrapper<TourContract>()
                .eq(TourContract::getOrderId, order.getId())
                .last("limit 1"));

        ComplaintOrder complaint = new ComplaintOrder();
        complaint.setComplaintNo(generateNo("CMP"));
        complaint.setOrderId(order.getId());
        complaint.setContractId(contract == null ? null : contract.getId());
        complaint.setUserId(userId);
        complaint.setMerchantId(order.getMerchantId());
        complaint.setComplaintType(request.getComplaintType());
        complaint.setTitle(request.getTitle());
        complaint.setContent(request.getContent());
        complaint.setStatus("PENDING_ACCEPT");
        complaint.setDeleted(0);
        complaintOrderMapper.insert(complaint);

        addFlow(complaint.getId(), userId, "USER", "CREATE", request.getContent());
        return complaint.getId();
    }

    public List<ComplaintRouteOptionVO> userComplaintRouteOptions() {
        List<TourOrder> orders = tourOrderMapper.selectList(new LambdaQueryWrapper<TourOrder>()
                .eq(TourOrder::getUserId, currentUserId())
                .eq(TourOrder::getOrderStatus, "COMPLETED")
                .orderByDesc(TourOrder::getUpdateTime)
                .orderByDesc(TourOrder::getId));
        return buildRouteOptions(orders);
    }

    public List<ComplaintOrder> userList() {
        List<ComplaintOrder> complaints = complaintOrderMapper.selectList(new LambdaQueryWrapper<ComplaintOrder>()
                .eq(ComplaintOrder::getUserId, currentUserId())
                .orderByDesc(ComplaintOrder::getCreateTime));
        return decorateComplaints(complaints);
    }

    public Map<String, Object> userDetail(Long id) {
        ComplaintOrder complaint = complaintOrderMapper.selectById(id);
        if (complaint == null || !Objects.equals(complaint.getUserId(), currentUserId())) {
            throw new BizException(404, "投诉不存在");
        }
        decorateComplaint(complaint);
        return complaintMap(complaint);
    }

    public List<ComplaintOrder> merchantList(String keyword) {
        Long shopId = currentMerchantShopId();
        List<ComplaintOrder> complaints = complaintOrderMapper.selectList(new LambdaQueryWrapper<ComplaintOrder>()
                .eq(ComplaintOrder::getMerchantId, shopId)
                .orderByDesc(ComplaintOrder::getCreateTime));
        List<ComplaintOrder> decoratedComplaints = decorateComplaints(complaints);
        if (!StringUtils.hasText(keyword)) {
            return decoratedComplaints;
        }
        String trimmedKeyword = keyword.trim();
        List<ComplaintOrder> filteredComplaints = new ArrayList<>();
        for (ComplaintOrder complaint : decoratedComplaints) {
            if (complaint == null) {
                continue;
            }
            if (containsKeyword(complaint.getComplaintNo(), trimmedKeyword)
                    || containsKeyword(complaint.getRouteName(), trimmedKeyword)) {
                filteredComplaints.add(complaint);
            }
        }
        return filteredComplaints;
    }

    public Map<String, Object> merchantDetail(Long id) {
        ComplaintOrder complaint = complaintOrderMapper.selectById(id);
        if (complaint == null || !Objects.equals(complaint.getMerchantId(), currentMerchantShopId())) {
            throw new BizException(404, "投诉不存在");
        }
        decorateComplaint(complaint);
        return complaintMap(complaint);
    }

    @Transactional(rollbackFor = Exception.class)
    public void merchantReply(Long id, ComplaintActionRequest request) {
        ComplaintOrder complaint = complaintOrderMapper.selectById(id);
        Long shopId = currentMerchantShopId();
        if (complaint == null || !Objects.equals(complaint.getMerchantId(), shopId)) {
            throw new BizException(404, "投诉不存在");
        }
        if (!"ACCEPTED".equals(complaint.getStatus()) && !"ASSIGNED".equals(complaint.getStatus())) {
            throw new BizException(400, "当前状态不可回复");
        }
        complaint.setStatus("MERCHANT_REPLIED");
        complaintOrderMapper.updateById(complaint);
        addFlow(id, currentUserId(), "MERCHANT", "REPLY", request.getContent());
    }

    public List<ComplaintOrder> adminList(String keyword) {
        List<ComplaintOrder> complaints = complaintOrderMapper.selectList(new LambdaQueryWrapper<ComplaintOrder>()
                .orderByDesc(ComplaintOrder::getCreateTime));
        List<ComplaintOrder> decoratedComplaints = decorateComplaints(complaints);
        if (!StringUtils.hasText(keyword)) {
            return decoratedComplaints;
        }
        String trimmedKeyword = keyword.trim();
        List<ComplaintOrder> filteredComplaints = new ArrayList<>();
        for (ComplaintOrder complaint : decoratedComplaints) {
            if (complaint == null) {
                continue;
            }
            if (containsKeyword(complaint.getComplaintNo(), trimmedKeyword)
                    || containsKeyword(complaint.getRouteName(), trimmedKeyword)) {
                filteredComplaints.add(complaint);
            }
        }
        return filteredComplaints;
    }

    public Map<String, Object> adminDetail(Long id) {
        ComplaintOrder complaint = complaintOrderMapper.selectById(id);
        if (complaint == null) {
            throw new BizException(404, "投诉不存在");
        }
        decorateComplaint(complaint);
        return complaintMap(complaint);
    }

    @Transactional(rollbackFor = Exception.class)
    public void adminAccept(Long id, ComplaintActionRequest request) {
        ComplaintOrder complaint = getComplaint(id);
        if (!"PENDING_ACCEPT".equals(complaint.getStatus())) {
            throw new BizException(400, "当前状态不可受理");
        }
        complaint.setStatus("ACCEPTED");
        complaintOrderMapper.updateById(complaint);
        addFlow(id, currentUserId(), "ADMIN", "ACCEPT", request.getContent());
    }

    @Transactional(rollbackFor = Exception.class)
    public void adminAssign(Long id, ComplaintActionRequest request) {
        ComplaintOrder complaint = getComplaint(id);
        if (!"ACCEPTED".equals(complaint.getStatus())) {
            throw new BizException(400, "当前状态不可分派");
        }
        complaint.setStatus("ASSIGNED");
        complaintOrderMapper.updateById(complaint);
        addFlow(id, currentUserId(), "ADMIN", "ASSIGN", request.getContent());
    }

    @Transactional(rollbackFor = Exception.class)
    public void adminJudge(Long id, ComplaintActionRequest request) {
        ComplaintOrder complaint = getComplaint(id);
        if (!"MERCHANT_REPLIED".equals(complaint.getStatus())) {
            throw new BizException(400, "商家未回复，不可判定");
        }
        complaint.setStatus("JUDGED");
        complaint.setResultType("ADMIN_JUDGE");
        complaint.setResultContent(request.getContent());
        complaintOrderMapper.updateById(complaint);
        addFlow(id, currentUserId(), "ADMIN", "JUDGE", request.getContent());
    }

    @Transactional(rollbackFor = Exception.class)
    public void adminFinish(Long id, ComplaintActionRequest request) {
        ComplaintOrder complaint = getComplaint(id);
        if (!"JUDGED".equals(complaint.getStatus())) {
            throw new BizException(400, "当前状态不可完成");
        }
        complaint.setStatus("FINISHED");
        complaintOrderMapper.updateById(complaint);
        addFlow(id, currentUserId(), "ADMIN", "FINISH", request.getContent());
    }

    private ComplaintOrder getComplaint(Long id) {
        ComplaintOrder complaint = complaintOrderMapper.selectById(id);
        if (complaint == null) {
            throw new BizException(404, "投诉不存在");
        }
        return complaint;
    }

    private void addFlow(Long complaintId, Long operatorId, String role, String action, String content) {
        ComplaintFlow flow = new ComplaintFlow();
        flow.setComplaintId(complaintId);
        flow.setOperatorId(operatorId);
        flow.setOperatorRole(role);
        flow.setActionType(action);
        flow.setActionContent(content);
        complaintFlowMapper.insert(flow);
    }

    private Map<String, Object> complaintMap(ComplaintOrder complaint) {
        List<ComplaintFlow> flows = complaintFlowMapper.selectList(new LambdaQueryWrapper<ComplaintFlow>()
                .eq(ComplaintFlow::getComplaintId, complaint.getId())
                .orderByAsc(ComplaintFlow::getCreateTime));
        Map<String, Object> map = new HashMap<>();
        map.put("complaint", complaint);
        map.put("flows", flows);
        return map;
    }

    private List<ComplaintRouteOptionVO> buildRouteOptions(List<TourOrder> orders) {
        List<TourOrder> safeOrders = orders == null ? new ArrayList<>() : orders;
        Map<Long, TourRoute> routeById = loadRoutesForOrders(safeOrders);
        List<ComplaintRouteOptionVO> options = new ArrayList<>();
        for (TourOrder order : safeOrders) {
            if (order == null || order.getId() == null) {
                continue;
            }
            ComplaintRouteOptionVO option = new ComplaintRouteOptionVO();
            option.setOrderId(order.getId());
            option.setOrderNo(order.getOrderNo());
            option.setRouteId(order.getRouteId());
            TourRoute route = routeById.get(order.getRouteId());
            option.setRouteName(resolveRouteName(order, route));
            option.setCoverImage(route == null ? null : route.getCoverImage());
            option.setCompletedTime(order.getUpdateTime() == null ? order.getCreateTime() : order.getUpdateTime());
            options.add(option);
        }
        return options;
    }

    private List<ComplaintOrder> decorateComplaints(List<ComplaintOrder> complaints) {
        List<ComplaintOrder> safeComplaints = complaints == null ? new ArrayList<>() : complaints;
        Map<Long, TourOrder> orderById = loadOrdersForComplaints(safeComplaints);
        Map<Long, TourRoute> routeById = loadRoutesForOrders(orderById.values());
        for (ComplaintOrder complaint : safeComplaints) {
            TourOrder order = complaint == null ? null : orderById.get(complaint.getOrderId());
            applyComplaintRelationFields(complaint, order, routeById);
        }
        return safeComplaints;
    }

    private void decorateComplaint(ComplaintOrder complaint) {
        if (complaint == null) {
            return;
        }
        List<ComplaintOrder> list = new ArrayList<>();
        list.add(complaint);
        decorateComplaints(list);
    }

    private boolean containsKeyword(String source, String keyword) {
        return StringUtils.hasText(source) && source.contains(keyword);
    }

    private Map<Long, TourOrder> loadOrdersForComplaints(List<ComplaintOrder> complaints) {
        Map<Long, TourOrder> orderById = new LinkedHashMap<>();
        if (complaints == null || complaints.isEmpty()) {
            return orderById;
        }
        for (ComplaintOrder complaint : complaints) {
            if (complaint == null || complaint.getOrderId() == null || orderById.containsKey(complaint.getOrderId())) {
                continue;
            }
            TourOrder order = tourOrderMapper.selectById(complaint.getOrderId());
            if (order != null) {
                orderById.put(order.getId(), order);
            }
        }
        return orderById;
    }

    private Map<Long, TourRoute> loadRoutesForOrders(Iterable<TourOrder> orders) {
        Map<Long, TourRoute> routeById = new LinkedHashMap<>();
        if (orders == null) {
            return routeById;
        }
        for (TourOrder order : orders) {
            if (order == null || order.getRouteId() == null || routeById.containsKey(order.getRouteId())) {
                continue;
            }
            TourRoute route = tourRouteMapper.selectById(order.getRouteId());
            if (route != null) {
                routeById.put(route.getId(), route);
            }
        }
        return routeById;
    }

    private void applyComplaintRelationFields(ComplaintOrder complaint, TourOrder order, Map<Long, TourRoute> routeById) {
        if (complaint == null) {
            return;
        }
        if (order == null) {
            complaint.setOrderNo(null);
            complaint.setRouteId(null);
            complaint.setRouteName(null);
            return;
        }
        complaint.setOrderNo(order.getOrderNo());
        complaint.setRouteId(order.getRouteId());
        complaint.setRouteName(resolveRouteName(order, routeById == null ? null : routeById.get(order.getRouteId())));
    }

    private String resolveRouteName(TourOrder order, TourRoute route) {
        if (route != null && route.getRouteName() != null && !route.getRouteName().trim().isEmpty()) {
            return route.getRouteName();
        }
        if (order != null && order.getRouteId() != null) {
            return "路线 #" + order.getRouteId();
        }
        return "未知路线";
    }

    private String generateNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ((int) (Math.random() * 9000) + 1000);
    }

    private Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException(401, "未登录");
        }
        return userId;
    }

    private Long currentMerchantShopId() {
        MerchantShop shop = merchantShopMapper.selectOne(new LambdaQueryWrapper<MerchantShop>()
                .eq(MerchantShop::getUserId, currentUserId())
                .eq(MerchantShop::getStatus, 1)
                .last("limit 1"));
        if (shop == null) {
            throw new BizException(400, "商家店铺不存在");
        }
        return shop.getId();
    }
}
