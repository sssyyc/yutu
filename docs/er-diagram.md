# Yutu Travel 项目 E-R 图

生成时间：2026-03-21  
数据来源：

- `yutu-admin/src/main/resources/sql/init.sql`
- `yutu-admin/src/main/resources/sql/upgrade_route_audit.sql`
- `yutu-admin/src/main/resources/sql/upgrade_merchant_apply.sql`
- `yutu-admin/src/main/resources/sql/upgrade_contract_template_stats.sql`
- `ContractSignatureSchemaRunner`
- `TourDepartureDateAuditSchemaRunner`
- 当前数据库 `yutu_travel`

说明：

- 这个项目的大部分关联是“逻辑外键”，数据库本身没有显式声明 `FOREIGN KEY`，下面的关系按表字段语义和后端代码推导。
- `tour_route.merchant_id`、`tour_order.merchant_id`、`tour_contract.merchant_id`、`complaint_order.merchant_id` 指向的是 `merchant_shop.id`，不是 `sys_user.id`。
- `user_favorite.target_id` 当前项目实际用于收藏路线，`target_type` 默认是 `ROUTE`。
- `tour_order_traveler` 是下单时的出行人快照表，和 `user_traveler` 没有物理外键。

## 1. 用户、商家、路线

```mermaid
erDiagram
    sys_user {
        BIGINT id PK
        VARCHAR username
        VARCHAR nickname
        VARCHAR phone
        TINYINT role_type
        TINYINT status
    }

    merchant_shop {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR shop_name
        VARCHAR contact_name
        VARCHAR contact_phone
        TINYINT audit_status
        TINYINT status
    }

    tour_category {
        BIGINT id PK
        BIGINT parent_id
        VARCHAR category_name
        INT sort_num
        TINYINT status
    }

    tour_tag {
        BIGINT id PK
        VARCHAR tag_name
        VARCHAR tag_type
        TINYINT status
    }

    tour_route {
        BIGINT id PK
        BIGINT merchant_id FK
        BIGINT category_id FK
        VARCHAR route_name
        DECIMAL price
        INT stock
        DECIMAL score
        TINYINT audit_status
        TINYINT publish_status
    }

    tour_route_tag {
        BIGINT id PK
        BIGINT route_id FK
        BIGINT tag_id FK
    }

    tour_route_schedule {
        BIGINT id PK
        BIGINT route_id FK
        INT day_no
        VARCHAR title
    }

    tour_departure_date {
        BIGINT id PK
        BIGINT route_id FK
        DATE depart_date
        DECIMAL sale_price
        INT remain_count
        TINYINT audit_status
        TINYINT status
    }

    user_favorite {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT target_id FK
        VARCHAR target_type
    }

    user_traveler {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR traveler_name
        VARCHAR id_card
        VARCHAR phone
    }

    sys_user ||--o| merchant_shop : "user_id"
    merchant_shop ||--o{ tour_route : "merchant_id"
    tour_category ||--o{ tour_route : "category_id"
    tour_category o|--o{ tour_category : "parent_id"
    tour_route ||--o{ tour_route_schedule : "route_id"
    tour_route ||--o{ tour_departure_date : "route_id"
    tour_route ||--o{ tour_route_tag : "route_id"
    tour_tag ||--o{ tour_route_tag : "tag_id"
    sys_user ||--o{ user_favorite : "user_id"
    tour_route ||--o{ user_favorite : "target_id"
    sys_user ||--o{ user_traveler : "user_id"
```

## 2. 订单、支付、合同、评价、投诉

```mermaid
erDiagram
    sys_user {
        BIGINT id PK
        VARCHAR username
        TINYINT role_type
    }

    merchant_shop {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR shop_name
    }

    tour_route {
        BIGINT id PK
        BIGINT merchant_id FK
        VARCHAR route_name
    }

    tour_departure_date {
        BIGINT id PK
        BIGINT route_id FK
        DATE depart_date
        DECIMAL sale_price
        INT remain_count
    }

    tour_order {
        BIGINT id PK
        VARCHAR order_no
        BIGINT user_id FK
        BIGINT merchant_id FK
        BIGINT route_id FK
        BIGINT depart_date_id FK
        DECIMAL total_amount
        DECIMAL pay_amount
        INT traveler_count
        VARCHAR order_status
        VARCHAR pay_status
        VARCHAR contract_status
    }

    tour_order_traveler {
        BIGINT id PK
        BIGINT order_id FK
        VARCHAR traveler_name
        VARCHAR id_card
        VARCHAR phone
    }

    pay_record {
        BIGINT id PK
        BIGINT order_id FK
        VARCHAR order_no
        VARCHAR pay_no
        VARCHAR pay_type
        DECIMAL pay_amount
        VARCHAR pay_status
    }

    contract_template {
        BIGINT id PK
        VARCHAR template_name
        VARCHAR template_code
        VARCHAR version_no
        BIGINT download_count
        BIGINT use_count
        TINYINT status
    }

    tour_contract {
        BIGINT id PK
        VARCHAR contract_no
        BIGINT order_id FK
        BIGINT template_id FK
        BIGINT user_id FK
        BIGINT merchant_id FK
        VARCHAR sign_status
        DATETIME sign_time
    }

    tour_contract_signature {
        BIGINT id PK
        BIGINT contract_id FK
        BIGINT user_id FK
        VARCHAR signer_name
        DATETIME sign_time
    }

    tour_contract_appendix {
        BIGINT id PK
        BIGINT contract_id FK
        VARCHAR appendix_title
    }

    tour_review {
        BIGINT id PK
        BIGINT route_id FK
        BIGINT order_id FK
        BIGINT user_id FK
        INT score
        VARCHAR content
        TINYINT status
    }

    complaint_order {
        BIGINT id PK
        VARCHAR complaint_no
        BIGINT order_id FK
        BIGINT contract_id FK
        BIGINT user_id FK
        BIGINT merchant_id FK
        VARCHAR complaint_type
        VARCHAR status
        VARCHAR result_type
    }

    complaint_flow {
        BIGINT id PK
        BIGINT complaint_id FK
        BIGINT operator_id FK
        VARCHAR operator_role
        VARCHAR action_type
    }

    sys_user ||--o{ tour_order : "user_id"
    merchant_shop ||--o{ tour_order : "merchant_id"
    tour_route ||--o{ tour_order : "route_id"
    tour_departure_date ||--o{ tour_order : "depart_date_id"
    tour_order ||--o{ tour_order_traveler : "order_id"
    tour_order ||--o{ pay_record : "order_id"
    tour_order ||--o| tour_contract : "order_id"
    contract_template ||--o{ tour_contract : "template_id"
    sys_user ||--o{ tour_contract : "user_id"
    merchant_shop ||--o{ tour_contract : "merchant_id"
    tour_contract ||--o| tour_contract_signature : "contract_id"
    sys_user ||--o{ tour_contract_signature : "user_id"
    tour_contract ||--o{ tour_contract_appendix : "contract_id"
    tour_order ||--o| tour_review : "order_id"
    tour_route ||--o{ tour_review : "route_id"
    sys_user ||--o{ tour_review : "user_id"
    tour_order ||--o| complaint_order : "order_id"
    tour_contract o|--o{ complaint_order : "contract_id"
    sys_user ||--o{ complaint_order : "user_id"
    merchant_shop ||--o{ complaint_order : "merchant_id"
    complaint_order ||--o{ complaint_flow : "complaint_id"
    sys_user ||--o{ complaint_flow : "operator_id"
```

## 3. 权限、菜单、运营内容

```mermaid
erDiagram
    sys_role {
        BIGINT id PK
        VARCHAR role_name
        VARCHAR role_code
        TINYINT status
    }

    sys_menu {
        BIGINT id PK
        BIGINT parent_id
        VARCHAR menu_name
        VARCHAR path
        VARCHAR perms
        TINYINT menu_type
        INT sort_num
        TINYINT status
    }

    sys_role_menu {
        BIGINT id PK
        BIGINT role_id FK
        BIGINT menu_id FK
    }

    sys_banner {
        BIGINT id PK
        VARCHAR title
        VARCHAR image_url
        VARCHAR link_url
        INT sort_num
        TINYINT status
    }

    sys_notice {
        BIGINT id PK
        VARCHAR title
        TEXT content
        TINYINT status
    }

    sys_dict_data {
        BIGINT id PK
        VARCHAR dict_type
        VARCHAR dict_label
        VARCHAR dict_value
        INT sort_num
        TINYINT status
    }

    sys_role ||--o{ sys_role_menu : "role_id"
    sys_menu ||--o{ sys_role_menu : "menu_id"
    sys_menu o|--o{ sys_menu : "parent_id"
```

补充说明：

- `sys_user.role_type` 在业务上对应 `USER / MERCHANT / ADMIN`，但库里不是通过 `role_id` 外键关联 `sys_role`。
- `sys_banner`、`sys_notice`、`sys_dict_data` 是独立运营配置表，没有直接外键关系。

## 4. 业务主链路

如果只看这个项目最核心的业务链路，可以概括为：

`sys_user` -> `merchant_shop` -> `tour_route` -> `tour_departure_date` -> `tour_order` -> `pay_record` / `tour_contract` -> `tour_review` / `complaint_order`

